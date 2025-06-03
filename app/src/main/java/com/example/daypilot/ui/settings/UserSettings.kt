package com.example.daypilot.ui.settings

import androidx.appcompat.app.AppCompatDelegate

class UserSettings (
    var darkModeOn: Boolean = false,
    var notificationsOn: Boolean = false,
    var receiptsOn: Boolean = false
)

fun applyDarkMode(enabled: Boolean) {
    if (!enabled) {
        AppCompatDelegate.setDefaultNightMode((AppCompatDelegate.MODE_NIGHT_NO))
    }
    else {
        AppCompatDelegate.setDefaultNightMode((AppCompatDelegate.MODE_NIGHT_YES))
    }
}