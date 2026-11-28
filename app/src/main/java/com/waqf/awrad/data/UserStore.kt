package com.waqf.awrad.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "awrad_user")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val fontScale: Float = 1f,
    val haptics: Boolean = true,
    val autoAdvance: Boolean = true,
)

/** Today's progress on the user's awrad, plus the history used for the streak. */
data class DailyState(
    val awrad: List<Wird> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val doneDays: Set<String> = emptySet(),
) {
    fun countOf(w: Wird) = (counts[w.id] ?: 0).coerceAtMost(w.target)
    val totalTarget get() = awrad.sumOf { it.target }
    val totalDone get() = awrad.sumOf { countOf(it) }
    val completed get() = awrad.count { countOf(it) >= it.target }
    val fraction get() = if (totalTarget == 0) 0f else totalDone.toFloat() / totalTarget

    /** Consecutive days (ending today, or yesterday if today is not finished yet) with every wird completed. */
    val streak: Int
        get() {
            var day = LocalDate.now()
            if (day.toString() !in doneDays) day = day.minusDays(1)
            var n = 0
            while (day.toString() in doneDays) {
                n++; day = day.minusDays(1)
            }
            return n
        }
}

class UserStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val wirdList = ListSerializer(Wird.serializer())
    private val countMap = MapSerializer(String.serializer(), Int.serializer())

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val fontScale = floatPreferencesKey("font_scale")
        val haptics = booleanPreferencesKey("haptics")
        val autoAdvance = booleanPreferencesKey("auto_advance")
        val favorites = stringSetPreferencesKey("favorites")
        val awrad = stringPreferencesKey("awrad")
        val progressDate = stringPreferencesKey("progress_date")
        val progress = stringPreferencesKey("progress")
        val doneDays = stringSetPreferencesKey("done_days")
    }

    private fun today() = LocalDate.now().toString()

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            theme = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            fontScale = p[Keys.fontScale] ?: 1f,
            haptics = p[Keys.haptics] ?: true,
            autoAdvance = p[Keys.autoAdvance] ?: true,
        )
    }

    val favorites: Flow<Set<String>> = context.dataStore.data.map { it[Keys.favorites] ?: emptySet() }

    val daily: Flow<DailyState> = context.dataStore.data.map { p ->
        DailyState(
            awrad = readAwrad(p),
            counts = if (p[Keys.progressDate] == today()) readCounts(p) else emptyMap(),
            doneDays = p[Keys.doneDays] ?: emptySet(),
        )
    }

    private fun readAwrad(p: Preferences): List<Wird> =
        p[Keys.awrad]?.let { runCatching { json.decodeFromString(wirdList, it) }.getOrNull() } ?: emptyList()

    private fun readCounts(p: Preferences): Map<String, Int> =
        p[Keys.progress]?.let { runCatching { json.decodeFromString(countMap, it) }.getOrNull() } ?: emptyMap()

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.name }
    suspend fun setFontScale(v: Float) = context.dataStore.edit { it[Keys.fontScale] = v }
    suspend fun setHaptics(v: Boolean) = context.dataStore.edit { it[Keys.haptics] = v }
    suspend fun setAutoAdvance(v: Boolean) = context.dataStore.edit { it[Keys.autoAdvance] = v }

    suspend fun toggleFavorite(itemId: String) = context.dataStore.edit { p ->
        val cur = p[Keys.favorites] ?: emptySet()
        p[Keys.favorites] = if (itemId in cur) cur - itemId else cur + itemId
    }

    suspend fun saveWird(text: String, target: Int, itemId: String?, id: String? = null) =
        context.dataStore.edit { p ->
            val list = readAwrad(p).toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) list[idx] = list[idx].copy(text = text, target = target, itemId = itemId)
            else list += Wird(UUID.randomUUID().toString(), text, target, itemId)
            p[Keys.awrad] = json.encodeToString(wirdList, list)
            updateDone(p)
        }

    suspend fun deleteWird(id: String) = context.dataStore.edit { p ->
        p[Keys.awrad] = json.encodeToString(wirdList, readAwrad(p).filterNot { it.id == id })
        updateDone(p)
    }

    suspend fun moveWird(id: String, delta: Int) = context.dataStore.edit { p ->
        val list = readAwrad(p).toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i >= 0 && j in list.indices) {
            val w = list.removeAt(i); list.add(j, w)
            p[Keys.awrad] = json.encodeToString(wirdList, list)
        }
    }

    /** Adds [delta] (may be negative) to today's count of a wird; clamps to 0..target. */
    suspend fun addCount(id: String, delta: Int) = context.dataStore.edit { p ->
        val counts = (if (p[Keys.progressDate] == today()) readCounts(p) else emptyMap()).toMutableMap()
        val target = readAwrad(p).firstOrNull { it.id == id }?.target ?: return@edit
        counts[id] = ((counts[id] ?: 0) + delta).coerceIn(0, target)
        p[Keys.progressDate] = today()
        p[Keys.progress] = json.encodeToString(countMap, counts)
        updateDone(p)
    }

    suspend fun resetCount(id: String) = context.dataStore.edit { p ->
        val counts = (if (p[Keys.progressDate] == today()) readCounts(p) else emptyMap()).toMutableMap()
        counts.remove(id)
        p[Keys.progressDate] = today()
        p[Keys.progress] = json.encodeToString(countMap, counts)
        updateDone(p)
    }

    private fun updateDone(p: androidx.datastore.preferences.core.MutablePreferences) {
        val awrad = readAwrad(p)
        val counts = if (p[Keys.progressDate] == today()) readCounts(p) else emptyMap()
        val done = p[Keys.doneDays] ?: emptySet()
        val allDone = awrad.isNotEmpty() && awrad.all { (counts[it.id] ?: 0) >= it.target }
        p[Keys.doneDays] = if (allDone) done + today() else done - today()
    }
}
