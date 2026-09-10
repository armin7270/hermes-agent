# SNI Spoofing — Android

دورزدن DPI با دستکاری TLS ClientHello — پورت هسته [patterniha/SNI-Spoofing](https://github.com/patterniha/SNI-Spoofing) (ویندوز/WinDivert) به اندروید، با UI الهام‌گرفته از [UAC-SNI-Spoofer-Android](https://github.com/armin7270/UAC-SNI-Spoofer-Android).

Bypass DPI with TLS ClientHello manipulation — the Windows patterniha core ported to Android user-space, with a UAC-inspired Compose UI.

[فارسی](#فارسی) · [English](#english)

---

## فارسی

### معرفی
اپلیکیشن VPN متن‌باز اندروید که بدون نیاز به روت، تکنیک‌های دورزدن DPI هسته patterniha را روی همه ترافیک TCP سیستم اعمال می‌کند. چون اندروید (برخلاف ویندوز) تزریق پکت خام نمی‌دهد، استک TCP/IP کامل userspace روی TUN پیاده شده و تکنیک‌ها روی سگمنت‌های خروجی اعمال می‌شوند.

### امکانات
- تونل سراسری با `VpnService` + استک TCP کاربر-فضا (SYN/ACK، retransmit، out-of-order، window)
- **حالت‌های دورزدن DPI:**
  - `split_n` — برش ClientHello از بایت N
  - `split_sni` — برش داخل خود SNI (پیش‌فرض: ۲ بایت داخل hostname)
  - `multi_frag` — قطعه‌بندی چندتکه
  - `delayed` — برش + تاخیر
  - `sni_replace` — بازنویسی SNI با دامنه جعلی (اصلاح همه length های رکورد/هندشیک/اکستنشن)
  - `combined` — ترکیب جایگزینی + قطعه‌بندی
  - `wrong_seq` — تزریق وفادار پترنی‌ها (`seq = syn_seq + 1 - len(fake)`) با هلپر روت (اختیاری)
- ساخت ClientHello جعلی — پورت `ClientHelloMaker` پترنی‌ها
- DNS-over-HTTPS با ساقط‌کردن AAAA (اجبار IPv4 داخل تونل) + کش
- پروفایل‌ها با import مستقیم `config.json` پترنی‌ها (CONNECT_IP / CONNECT_PORT / FAKE_SNI)
- مسیریابی per-app (لیست سفید/سیاه)
- DNS: Cloudflare / Google / AdGuard / Quad9 / OpenDNS / سفارشی
- بلاک QUIC (بازگشت اپ‌ها به TCP)، MTU قابل تنظیم، اتصال خودکار بعد از بوت
- UI کامپوز: دکمه اتصال انیمیشنی، آمار زنده (بسته‌ها، قطعات، ترافیک، uptime)، لاگ زنده
- Quick Settings tile + نوتیفیکیشن با دکمه قطع

### ساخت از سورس
JDK 17 لازم است (SDK خودکار دانلود می‌شود).

```bash
git clone https://github.com/armin7270/hermes-agent.git
cd hermes-agent
./gradlew :app:assembleDebug
# خروجی: app/build/outputs/apk/debug/app-debug.apk
```

یا از تب [Actions](https://github.com/armin7270/hermes-agent/actions) همین ریپو، APK آماده را از artifact دانلود کنید.

### هلپر روت (اختیاری — wrong_seq)
فقط برای حالت `wrong_seq` که دقیقاً رفتار ویندوزی پترنی‌ها را تکرار می‌کند:

```bash
cd app/src/main/cpp
$NDK/toolchains/llvm/prebuilt/*/bin/aarch64-linux-android24-clang spoof_helper.c -o spoofhelper -pthread
adb push spoofhelper /data/local/tmp/ && adb shell chmod 755 /data/local/tmp/spoofhelper
```

### اعتبار
- هسته اصلی: [@patterniha](https://t.me/patterniha) — [SNI-Spoofing](https://github.com/patterniha/SNI-Spoofing)
- ساختار/UI: [UAC-SNI-Spoofer-Android](https://github.com/Floxu1/UAC-SNI-Spoofer-Android)

---

## English

### What it is
Open-source Android VPN app that applies the patterniha DPI-evasion techniques (Windows/WinDivert) to all device TCP traffic — no root required. Android has no raw-packet injection, so a full userspace TCP/IP stack runs on the TUN device and the evasion is applied to outgoing segments.

### Evasion modes
| Mode | Description |
|---|---|
| `split_n` | Cut the ClientHello after N bytes |
| `split_sni` | Cut inside the SNI hostname (default: 2 bytes in) |
| `multi_frag` | Split into many fragments |
| `delayed` | Split + micro-delay |
| `sni_replace` | Rewrite SNI to a fake host, fixing every enclosing length field |
| `combined` | SNI replace + fragmentation |
| `wrong_seq` | Faithful patterniha injection (`seq = syn_seq + 1 - len(fake)`) via optional root helper |

### Build
```bash
./gradlew :app:assembleDebug
```
JDK 17 only — the Android SDK is provisioned automatically. CI builds the APK on every push (see Actions tab).

### Credits
- Core concept: [patterniha/SNI-Spoofing](https://github.com/patterniha/SNI-Spoofing)
- UI/UX reference: [UAC-SNI-Spoofer-Android](https://github.com/Floxu1/UAC-SNI-Spoofer-Android)
