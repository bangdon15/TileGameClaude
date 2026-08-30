package com.claudetest.matchtiles.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The two sound switches the menu offers, remembered across launches.
 *
 * Small enough that SharedPreferences is the honest choice: two booleans, read once on
 * construction and mirrored into flows the UI can collect.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("match_tiles_settings", Context.MODE_PRIVATE)

    private val _musicOn = MutableStateFlow(prefs.getBoolean(KEY_MUSIC, true))
    /** The looping background music. */
    val musicOn: StateFlow<Boolean> = _musicOn.asStateFlow()

    private val _soundOn = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    /** The interface sounds: taps, matches, misses, fanfares. */
    val soundOn: StateFlow<Boolean> = _soundOn.asStateFlow()

    fun setMusicOn(on: Boolean) = store(KEY_MUSIC, on, _musicOn)

    fun setSoundOn(on: Boolean) = store(KEY_SOUND, on, _soundOn)

    private fun store(key: String, on: Boolean, mirror: MutableStateFlow<Boolean>) {
        prefs.edit().putBoolean(key, on).apply()
        mirror.value = on
    }

    private companion object {
        const val KEY_MUSIC = "background_music"
        const val KEY_SOUND = "ui_sound"
    }
}
