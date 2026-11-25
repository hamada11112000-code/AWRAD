package com.waqf.awrad.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Reads the bundled content once and answers lookups and searches over it. */
class ContentRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: Library? = null

    suspend fun library(): Library = cached ?: withContext(Dispatchers.IO) {
        cached ?: context.assets.open(ASSET).use { input ->
            json.decodeFromString(Library.serializer(), input.readBytes().decodeToString())
        }.also { cached = it }
    }

    data class SearchHit(val category: Category, val index: Int, val item: Item)

    fun search(library: Library, query: String, limit: Int = 80): List<SearchHit> {
        val q = query.normalizeArabic().trim()
        if (q.length < 2) return emptyList()
        val terms = q.split(' ').filter { it.isNotBlank() }
        val hits = ArrayList<SearchHit>()
        for (cat in library.categories) {
            val titleMatch = terms.all { cat.title.normalizeArabic().contains(it) }
            cat.items.forEachIndexed { i, item ->
                val hay = buildString {
                    append(item.zikr); append(' '); append(item.matn); append(' ')
                    append(item.text); append(' '); append(item.fadl); append(' '); append(item.subheading)
                }.normalizeArabic()
                if (titleMatch && i == 0 || terms.all { hay.contains(it) }) {
                    hits += SearchHit(cat, i, item)
                    if (hits.size >= limit) return hits
                }
            }
        }
        return hits
    }

    companion object {
        const val ASSET = "azkar.json"
    }
}
