package com.armin7270.snispoof.state

import android.content.Context
import com.armin7270.snispoof.core.engine.ProfileCodec
import com.armin7270.snispoof.core.engine.SpoofProfile
import com.armin7270.snispoof.core.engine.dedupe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Persists the SNI spoof profiles (patterniha config.json equivalents). */
class ProfileStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("profiles", Context.MODE_PRIVATE)

    private val _profiles = MutableStateFlow(load())
    val profiles: StateFlow<List<SpoofProfile>> = _profiles

    private val _selectedId = MutableStateFlow(prefs.getString(KEY_SELECTED, null))
    val selectedId: StateFlow<String?> = _selectedId

    fun selected(): SpoofProfile? = _profiles.value.firstOrNull { it.id == _selectedId.value }

    fun save(profile: SpoofProfile) {
        val list = _profiles.value.toMutableList()
        val idx = list.indexOfFirst { it.id == profile.id }
        if (idx >= 0) list[idx] = profile else list.add(profile)
        replaceAll(list)
        if (_selectedId.value == null) select(profile.id)
    }

    fun delete(id: String) {
        replaceAll(_profiles.value.filterNot { it.id == id })
        if (_selectedId.value == id) select(_profiles.value.firstOrNull()?.id)
    }

    fun select(id: String?) {
        _selectedId.value = id
        prefs.edit().putString(KEY_SELECTED, id).apply()
    }

    fun importAll(list: List<SpoofProfile>) {
        if (list.isEmpty()) return
        replaceAll(dedupe(_profiles.value + list))
    }

    fun resetToDefaults() {
        replaceAll(SpoofProfile.defaultProfiles())
        select(_profiles.value.firstOrNull()?.id)
    }

    private fun replaceAll(list: List<SpoofProfile>) {
        _profiles.value = list
        prefs.edit().putString(KEY_PROFILES, ProfileCodec.encode(list)).apply()
    }

    private fun load(): List<SpoofProfile> {
        val raw = prefs.getString(KEY_PROFILES, null)
        if (raw.isNullOrBlank()) return SpoofProfile.defaultProfiles()
        val parsed = ProfileCodec.decode(raw)
        return parsed.ifEmpty { SpoofProfile.defaultProfiles() }
    }

    companion object {
        private const val KEY_PROFILES = "profiles_json"
        private const val KEY_SELECTED = "selected_id"

        @Volatile private var instance: ProfileStore? = null
        fun get(context: Context): ProfileStore =
            instance ?: synchronized(this) {
                instance ?: ProfileStore(context.applicationContext).also { instance = it }
            }
    }
}
