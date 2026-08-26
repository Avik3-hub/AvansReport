package com.example.avans

import android.content.Context

class LocationHistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("location_history_prefs", Context.MODE_PRIVATE)

    fun getSavedLocations(): List<String> {
        return prefs.getStringSet("saved_locations", emptySet())?.toList()?.sorted() ?: emptyList()
    }

    fun saveLocation(location: String) {
        val trimmed = location.trim()
        if (trimmed.isNotBlank()) {
            val current = prefs.getStringSet("saved_locations", emptySet())?.toMutableSet() ?: mutableSetOf()
            current.add(trimmed)
            prefs.edit().putStringSet("saved_locations", current).apply()
        }
    }
}
