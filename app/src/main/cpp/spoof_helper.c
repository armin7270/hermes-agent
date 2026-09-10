/*
 * spoof_helper.c — root helper for the faithful "wrong_seq" SNI desync.
 *
 * This is the direct Android port of patterniha's SNI-Spoofing core
 * (fake_tcp.py + injecter.py, which use WinDivert on Windows):
 *   - sniff the TCP handshake of the relay's protected socket via AF_PACKET
 *   - record syn_seq / syn_ack_seq
 *   - right after the final handshake ACK, re-send that same ACK as a PSH
 *     segment carrying the fake ClientHello, with
 *         seq = (syn_seq + 1 - len(fake)) & 0xffffffff
 *     so the real server drops it (out-of-window) while the DPI sees an
 *     allowed SNI first
 *   - signal OK when the server's first pure ACK arrives
 *
 * Protocol on stdin/stdout:
 *   -> WATCH <id> <srcIp> <srcPort> <dstIp> <dstPort> <hexFakeData>
 *   <- WATCHING <id>
 *   <- RESULT <id> OK
 *   <- RESULT <id> FAIL <reason>
 *   -> CANCEL <id>
 *   -> PING            <- PONG
 *   -> EXIT
 */
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <pthread.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <netinet/in.h>
#include <netinet/ip.h>
#include <netinet/tcp.h>
#include <netpacket/packet.h>

#define MAX_WATCHERS 64
#define MAX_FAKE 8192

typedef struct Watcher {
    int id;
    int active;
    long deadline_ms;
    uint32_t src_ip, dst_ip;        /* network order */
    uint16_t src_port, dst_port;    /* host order */
    uint8_t fake[MAX_FAKE];
    int fake_len;
    int state; /* 0=want SYN, 1=want SYN-ACK, 2=want final ACK, 3=fake sent, 4=done */
    uint32_t syn_seq, syn_ack_seq;  /* host order */
    uint8_t final_ack[256];
    int final_ack_len;
} Watcher;

static Watcher g_watchers[MAX_WATCHERS];
static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t g_out_lock = PTHREAD_MUTEX_INITIALIZER;
static int g_raw_sock = -1;
static volatile int g_running = 1;

static long now_ms(void) {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (long)tv.tv_sec * 1000L + tv.tv_usec / 1000;
}

static void out_printf(const char *fmt, ...) {
    va_list ap;
    pthread_mutex_lock(&g_out_lock);
    va_start(ap, fmt);
    vfprintf(stdout, fmt, ap);
    va_end(ap);
    fputc('\n', stdout);
    fflush(stdout);
    pthread_mutex_unlock(&g_out_lock);
}

static uint16_t cksum(const uint8_t *b, int len) {
    uint32_t sum = 0;
    int i;
    for (i = 0; i + 1 < len; i += 2)
        sum += ((uint32_t)b[i] << 8) | b[i + 1];
    if (i < len)
        sum += (uint32_t)b[i] << 8;
    while (sum >> 16)
        sum = (sum & 0xffff) + (sum >> 16);
    return (uint16_t)(~sum);
}

static void fail_watcher(Watcher *w, const char *reason) {
    out_printf("RESULT %d FAIL %s", w->id, reason);
    w->state = 4;
    w->active = 0;
}

static void ok_watcher(Watcher *w) {
    out_printf("RESULT %d OK", w->id);
    w->state = 4;
    w->active = 0;
}

static void send_raw_ip(const uint8_t *pkt, int len) {
    struct sockaddr_in dst;
    memset(&dst, 0, sizeof(dst));
    dst.sin_family = AF_INET;
    dst.sin_addr.s_addr = *(const uint32_t *)(pkt + 16); /* ip dst */
    if (sendto(g_raw_sock, pkt, len, 0, (struct sockaddr *)&dst, sizeof(dst)) < 0) {
        out_printf("LOG sendto failed errno=%d", errno);
    }
}

/* Craft and inject the fake PSH segment, cloning the final handshake ACK. */
static void inject_fake(Watcher *w) {
    uint8_t buf[512];
    int ip_ihl = (w->final_ack[0] & 0xf) * 4;
    int tcp_off = ip_ihl;
    int tcp_doff = (w->final_ack[tcp_off + 12] >> 4) * 4;
    int total = ip_ihl + tcp_doff + w->fake_len;

    if (total > (int)sizeof(buf)) { fail_watcher(w, "fake too big"); return; }
    memcpy(buf, w->final_ack, ip_ihl + tcp_doff);

    /* IP: total length + new identification */
    buf[2] = (uint8_t)(total >> 8);
    buf[3] = (uint8_t)(total & 0xff);
    uint16_t ident = ((uint16_t)buf[4] << 8) | buf[5];
    ident = (uint16_t)((ident + 1) & 0xffff);
    buf[4] = (uint8_t)(ident >> 8);
    buf[5] = (uint8_t)(ident & 0xff);

    /* TCP: PSH flag + wrong sequence */
    buf[tcp_off + 13] |= 0x08; /* PSH */
    uint32_t seq = (uint32_t)(((uint64_t)w->syn_seq + 1ULL - (uint64_t)w->fake_len) & 0xffffffffULL);
    buf[tcp_off + 4] = (uint8_t)(seq >> 24);
    buf[tcp_off + 5] = (uint8_t)(seq >> 16);
    buf[tcp_off + 6] = (uint8_t)(seq >> 8);
    buf[tcp_off + 7] = (uint8_t)(seq & 0xff);

    memcpy(buf + ip_ihl + tcp_doff, w->fake, w->fake_len);

    /* TCP checksum (with pseudo header) */
    uint8_t pseudo[12];
    memcpy(pseudo, buf + 12, 4);
    memcpy(pseudo + 4, buf + 16, 4);
    pseudo[8] = 0;
    pseudo[9] = 6;
    pseudo[10] = (uint8_t)((tcp_doff + w->fake_len) >> 8);
    pseudo[11] = (uint8_t)((tcp_doff + w->fake_len) & 0xff);
    buf[tcp_off + 16] = 0;
    buf[tcp_off + 17] = 0;
    {
        uint8_t tmp[512 + 12];
        memcpy(tmp, pseudo, 12);
        memcpy(tmp + 12, buf + tcp_off, tcp_doff + w->fake_len);
        uint16_t c = cksum(tmp, 12 + tcp_doff + w->fake_len);
        buf[tcp_off + 16] = (uint8_t)(c >> 8);
        buf[tcp_off + 17] = (uint8_t)(c & 0xff);
    }

    /* IP checksum */
    buf[10] = 0;
    buf[11] = 0;
    {
        uint16_t c = cksum(buf, ip_ihl);
        buf[10] = (uint8_t)(c >> 8);
        buf[11] = (uint8_t)(c & 0xff);
    }

    send_raw_ip(buf, total);
    out_printf("LOG fake injected seq=%u len=%d", seq, w->fake_len);
}

static Watcher *find_watcher(uint32_t src_ip, uint16_t src_port,
                             uint32_t dst_ip, uint16_t dst_port, int outbound) {
    pthread_mutex_lock(&g_lock);
    Watcher *hit = NULL;
    for (int i = 0; i < MAX_WATCHERS; i++) {
        Watcher *w = &g_watchers[i];
        if (!w->active) continue;
        if (outbound) {
            if (w->src_ip == src_ip && w->src_port == src_port &&
                w->dst_ip == dst_ip && w->dst_port == dst_port) { hit = w; break; }
        } else {
            if (w->dst_ip == src_ip && w->dst_port == src_port &&
                w->src_ip == dst_ip && w->src_port == dst_port) { hit = w; break; }
        }
    }
    if (hit) { pthread_mutex_unlock(&g_lock); return hit; }
    pthread_mutex_unlock(&g_lock);
    return NULL;
}

static void process_packet(uint8_t *b, int len) {
    int ip_off = 0;
    if (((b[0] >> 4) & 0xf) != 4) {
        ip_off = 14; /* ethernet framing (wlan0 etc.) */
        if (len <= ip_off + 20 || ((b[ip_off] >> 4) & 0xf) != 4) return;
    }
    int ip_ihl = (b[ip_off] & 0xf) * 4;
    if (len < ip_off + ip_ihl + 20) return;
    if (b[ip_off + 9] != 6) return; /* TCP only */

    uint32_t src_ip, dst_ip;
    memcpy(&src_ip, b + ip_off + 12, 4);
    memcpy(&dst_ip, b + ip_off + 16, 4);
    const uint8_t *tcp = b + ip_off + ip_ihl;
    int tcp_doff = (tcp[12] >> 4) * 4;
    if (tcp_doff < 20) return;
    int l4_len = len - ip_off - ip_ihl;
    if (l4_len < tcp_doff) return;
    int payload_len = l4_len - tcp_doff;
    uint16_t sport = ((uint16_t)tcp[0] << 8) | tcp[1];
    uint16_t dport = ((uint16_t)tcp[2] << 8) | tcp[3];
    uint32_t seq = ((uint32_t)tcp[4] << 24) | ((uint32_t)tcp[5] << 16) |
                   ((uint32_t)tcp[6] << 8) | tcp[7];
    uint32_t ack = ((uint32_t)tcp[8] << 24) | ((uint32_t)tcp[9] << 16) |
                   ((uint32_t)tcp[10] << 8) | tcp[11];
    uint8_t flags = tcp[13] & 0x3f;
    const uint8_t *payload = tcp + tcp_doff;

    /* quick pre-filter */
    int outbound = 0, inbound = 0;
    pthread_mutex_lock(&g_lock);
    for (int i = 0; i < MAX_WATCHERS && !outbound && !inbound; i++) {
        Watcher *w = &g_watchers[i];
        if (!w->active) continue;
        if (w->src_ip == src_ip && w->src_port == sport &&
            w->dst_ip == dst_ip && w->dst_port == dport) outbound = 1;
        else if (w->dst_ip == src_ip && w->dst_port == sport &&
                 w->src_ip == dst_ip && w->src_port == dport) inbound = 1;
    }
    pthread_mutex_unlock(&g_lock);
    if (!outbound && !inbound) return;

    Watcher *w = find_watcher(src_ip, sport, dst_ip, dport, outbound);
    if (!w) return;
    pthread_mutex_lock(&g_lock);

    if (!w->active) { pthread_mutex_unlock(&g_lock); return; }

    if (outbound) {
        int syn_only = (flags & 0x12) == 0x02 && payload_len == 0 && ack == 0;
        int ack_only = (flags & 0x10) != 0 && (flags & 0x02) == 0 &&
                       (flags & 0x04) == 0 && payload_len == 0;
        if (w->state == 0) {
            if (syn_only) {
                w->syn_seq = seq;
                w->state = 1;
            } else if (flags & 0x04) {
                fail_watcher(w, "rst before synack");
            }
            /* anything else pre-handshake: ignore (rare) */
        } else if (w->state == 1) {
            if (syn_only && seq == w->syn_seq) { /* SYN retransmit: ignore */ }
            else if (flags & 0x04) fail_watcher(w, "rst before synack2");
            /* ignore other stray packets while waiting for SYN-ACK */
        } else if (w->state == 2) {
            if (ack_only && seq == ((w->syn_seq + 1) & 0xffffffffUL) &&
                ack == ((w->syn_ack_seq + 1) & 0xffffffffUL)) {
                int hdr = ip_ihl + tcp_doff;
                if (hdr <= (int)sizeof(w->final_ack)) {
                    memcpy(w->final_ack, b + ip_off, hdr);
                    w->final_ack_len = hdr;
                    w->state = 3;
                    pthread_mutex_unlock(&g_lock);
                    usleep(1000); /* patterniha: 1ms delay */
                    pthread_mutex_lock(&g_lock);
                    if (w->active && w->state == 3) inject_fake(w);
                }
            } else if (flags & 0x04) {
                fail_watcher(w, "rst before final ack");
            }
        } else if (w->state == 3) {
            /* payload sent by the relay before the server ACKed the fake */
            if (payload_len > 0 && w->active) fail_watcher(w, "outbound after fake");
        }
        /* state 4: ignore */
    } else { /* inbound */
        int ack_only = (flags & 0x10) != 0 && (flags & 0x02) == 0 &&
                       (flags & 0x04) == 0 && payload_len == 0;
        int syn_ack = (flags & 0x12) == 0x12 && payload_len == 0;
        if (w->state == 0) {
            fail_watcher(w, "no syn sent");
        } else if (w->state == 1) {
            if (syn_ack) {
                if (ack != ((w->syn_seq + 1) & 0xffffffffUL)) {
                    fail_watcher(w, "ack not matched");
                } else if (w->syn_ack_seq && w->syn_ack_seq != seq) {
                    fail_watcher(w, "seq change");
                } else {
                    w->syn_ack_seq = seq;
                    w->state = 2;
                }
            } else if (flags & 0x04) {
                fail_watcher(w, "rst got");
            } else {
                fail_watcher(w, "unexpected inbound");
            }
        } else if (w->state == 2) {
            if (flags & 0x04) fail_watcher(w, "rst got2");
            /* server is silent until our final ACK; ignore dup SYN-ACKs */
        } else if (w->state == 3) {
            if (ack_only && seq == ((w->syn_ack_seq + 1) & 0xffffffffUL) &&
                ack == ((w->syn_seq + 1) & 0xffffffffUL)) {
                ok_watcher(w); /* fake_data_ack_recv */
            } else if (flags & 0x04) {
                fail_watcher(w, "rst after fake");
            } else if (payload_len > 0 && w->active) {
                fail_watcher(w, "unexpected inbound after fake");
            }
        }
        /* state 4: ignore */
    }
    pthread_mutex_unlock(&g_lock);
}

static void *sniffer_thread(void *arg) {
    (void)arg;
    int s = socket(AF_PACKET, SOCK_RAW, htons(ETH_P_ALL));
    if (s < 0) {
        out_printf("LOG af_packet failed errno=%d", errno);
        g_running = 0;
        return NULL;
    }
    struct timeval tv = {0, 200000};
    setsockopt(s, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));

    uint8_t *buf = malloc(65575);
    while (g_running) {
        /* expire stale watchers */
        long now = now_ms();
        pthread_mutex_lock(&g_lock);
        for (int i = 0; i < MAX_WATCHERS; i++) {
            Watcher *w = &g_watchers[i];
            if (w->active && now > w->deadline_ms) fail_watcher(w, "timeout");
        }
        pthread_mutex_unlock(&g_lock);

        int n = (int)recvfrom(s, buf, 65575, 0, NULL, NULL);
        if (n <= 0) continue;
        process_packet(buf, n);
    }
    free(buf);
    close(s);
    return NULL;
}

static int hexval(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    return -1;
}

int main(void) {
    setvbuf(stdout, NULL, _IOLBF, 0);
    memset(g_watchers, 0, sizeof(g_watchers));

    g_raw_sock = socket(AF_INET, SOCK_RAW, IPPROTO_RAW);
    if (g_raw_sock >= 0) {
        int one = 1;
        setsockopt(g_raw_sock, IPPROTO_IP, IP_HDRINCL, &one, sizeof(one));
    } else {
        out_printf("LOG raw socket failed errno=%d", errno);
    }

    pthread_t tid;
    pthread_create(&tid, NULL, sniffer_thread, NULL);

    char line[65536 + 256];
    while (g_running && fgets(line, sizeof(line), stdin)) {
        size_t l = strlen(line);
        while (l && (line[l - 1] == '\n' || line[l - 1] == '\r')) line[--l] = 0;
        if (!l) continue;

        if (strcmp(line, "PING") == 0) { out_printf("PONG"); continue; }
        if (strcmp(line, "EXIT") == 0) break;
        if (strncmp(line, "CANCEL ", 7) == 0) {
            int id = atoi(line + 7);
            pthread_mutex_lock(&g_lock);
            for (int i = 0; i < MAX_WATCHERS; i++)
                if (g_watchers[i].active && g_watchers[i].id == id) {
                    g_watchers[i].active = 0;
                    g_watchers[i].state = 4;
                }
            pthread_mutex_unlock(&g_lock);
            continue;
        }
        if (strncmp(line, "WATCH ", 6) == 0) {
            /* WATCH id srcip srcport dstip dstport hex */
            char *save = NULL;
            char *tok = strtok_r(line, " ", &save);
            tok = strtok_r(NULL, " ", &save); int id = atoi(tok);
            tok = strtok_r(NULL, " ", &save); uint32_t sip; inet_pton(AF_INET, tok, &sip);
            tok = strtok_r(NULL, " ", &save); int sport = atoi(tok);
            tok = strtok_r(NULL, " ", &save); uint32_t dip; inet_pton(AF_INET, tok, &dip);
            tok = strtok_r(NULL, " ", &save); int dport = atoi(tok);
            tok = strtok_r(NULL, " ", &save); const char *hex = tok;
            int hex_len = (int)strlen(hex);

            if (g_raw_sock < 0) { out_printf("RESULT %d FAIL no raw socket", id); continue; }
            if (hex_len <= 0 || hex_len / 2 > MAX_FAKE) { out_printf("RESULT %d FAIL bad fake", id); continue; }

            pthread_mutex_lock(&g_lock);
            Watcher *slot = NULL;
            for (int i = 0; i < MAX_WATCHERS; i++)
                if (!g_watchers[i].active && g_watchers[i].state == 4) { slot = &g_watchers[i]; break; }
            if (!slot) {
                for (int i = 0; i < MAX_WATCHERS; i++)
                    if (!g_watchers[i].active) { slot = &g_watchers[i]; break; }
            }
            if (!slot) {
                pthread_mutex_unlock(&g_lock);
                out_printf("RESULT %d FAIL busy", id);
                continue;
            }
            memset(slot, 0, sizeof(*slot));
            slot->id = id;
            slot->active = 1;
            slot->deadline_ms = now_ms() + 8000;
            slot->src_ip = sip; slot->dst_ip = dip;
            slot->src_port = (uint16_t)sport; slot->dst_port = (uint16_t)dport;
            for (int i = 0; i < hex_len / 2; i++) {
                int hi = hexval(hex[2 * i]), lo = hexval(hex[2 * i + 1]);
                if (hi < 0 || lo < 0) { slot->active = 0; break; }
                slot->fake[i] = (uint8_t)((hi << 4) | lo);
            }
            slot->fake_len = hex_len / 2;
            slot->state = 0;
            pthread_mutex_unlock(&g_lock);
            out_printf("WATCHING %d", id);
            continue;
        }
    }

    g_running = 0;
    return 0;
}
