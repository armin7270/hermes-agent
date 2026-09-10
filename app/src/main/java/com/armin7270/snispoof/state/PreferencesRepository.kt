package com.armin7270.snispoof.state

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spoof_settings")

enum class DnsProvider(val id: String, val label: String, val primary: String) {
    CLOUDFLARE("cloudflare", "Cloudflare", "1.1.1.1"),
    GOOGLE("google", "Google", "8.8.8.8"),
    ADGUARD("adguard", "AdGuard", "94.140.14.14"),
    QUAD9("quad9", "Quad9", "9.9.9.9"),
    OPENDNS("opendns", "OpenDNS", "208.67.222.222"),
    CUSTOM("custom", "Custom", "1.1.1.1");

    companion object {
        fun fromId(id: String): DnsProvider = entries.firstOrNull { it.id == id } ?: CLOUDFLARE
    }
}

enum class PerAppMode(val id: String) {
    ALL("all"),          // every app goes through the tunnel
    WHITELIST("whitelist"), // only selected apps
    BLACKLIST("blacklist"); // selected apps bypass

    companion object { fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: ALL }
}

data class AppSettings(
    val autoStartBoot: Boolean = false,
    val blockQuic: Boolean = true,
    val dnsProvider: DnsProvider = DnsProvider.CLOUDFLARE,
    val customDns: String = "1.1.1.1",
    val mtu: Int = 1280,
    val rootMode: Boolean = false,
    val perAppMode: PerAppMode = PerAppMode.ALL,
    val perAppPackages: Set<String> = emptySet(),
    val language: String = "fa",
) {
    val dnsIp: String get() = if (dnsProvider == DnsProvider.CUSTOM) customDns.trim().ifEmpty { "1.1.1.1" } else dnsProvider.primary
}

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val AUTO_BOOT = booleanPreferencesKey("auto_start_boot")
        val BLOCK_QUIC = booleanPreferencesKey("block_quic")
        val DNS_PROVIDER = stringPreferencesKey("dns_provider")
        val CUSTOM_DNS = stringPreferencesKey("custom_dns")
        val MTU = intPreferencesKey("tun_mtu")
        val ROOT_MODE = booleanPreferencesKey("root_mode")
        val PER_APP_MODE = stringPreferencesKey("per_app_mode")
        val PER_APP_PACKAGES = stringSetPreferencesKey("per_app_packages")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            autoStartBoot = p[Keys.AUTO_BOOT] ?: false,
            blockQuic = p[Keys.BLOCK_QUIC] ?: true,
            dnsProvider = DnsProvider.fromId(p[Keys.DNS_PROVIDER] ?: "cloudflare"),
            customDns = p[Keys.CUSTOM_DNS] ?: "1.1.1.1",
            mtu = (p[Keys.MTU] ?: 1280).coerceIn(576, 9000),
            rootMode = p[Keys.ROOT_MODE] ?: false,
            perAppMode = PerAppMode.fromId(p[Keys.PER_APP_MODE] ?: "all"),
            perAppPackages = p[Keys.PER_APP_PACKAGES] ?: emptySet(),
            language = p[Keys.LANGUAGE] ?: "fa",
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setAutoStartBoot(v: Boolean) = edit { it[Keys.AUTO_BOOT] = v }
    suspend fun setBlockQuic(v: Boolean) = edit { it[Keys.BLOCK_QUIC] = v }
    suspend fun setDnsProvider(v: DnsProvider) = edit { it[Keys.DNS_PROVIDER] = v.id }
    suspend fun setCustomDns(v: String) = edit { it[Keys.CUSTOM_DNS] = v }
    suspend fun setMtu(v: Int) = edit { it[Keys.MTU] = v.coerceIn(576, 9000) }
    suspend fun setRootMode(v: Boolean) = edit { it[Keys.ROOT_MODE] = v }
    suspend fun setPerAppMode(v: PerAppMode) = edit { it[Keys.PER_APP_MODE] = v.id }
    suspend fun setLanguage(v: String) = edit { it[Keys.LANGUAGE] = v }

    suspend fun togglePerAppPackage(pkg: String) = edit {
        val cur = it[Keys.PER_APP_PACKAGES] ?: emptySet()
        it[Keys.PER_APP_PACKAGES] = if (pkg in cur) cur - pkg else cur + pkg
    }

    suspend fun resetDefaults() = edit { it.clear() }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
