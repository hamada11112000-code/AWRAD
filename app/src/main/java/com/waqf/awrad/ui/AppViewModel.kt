package com.waqf.awrad.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waqf.awrad.data.Category
import com.waqf.awrad.data.ContentRepository
import com.waqf.awrad.data.DailyState
import com.waqf.awrad.data.Item
import com.waqf.awrad.data.Library
import com.waqf.awrad.data.Settings
import com.waqf.awrad.data.ThemeMode
import com.waqf.awrad.data.UserStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val repo = ContentRepository(app)
    private val store = UserStore(app)

    private val _library = MutableStateFlow<Library?>(null)
    val library: StateFlow<Library?> = _library.asStateFlow()

    val settings: StateFlow<Settings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val favorites: StateFlow<Set<String>> =
        store.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val daily: StateFlow<DailyState> =
        store.daily.stateIn(viewModelScope, SharingStarted.Eagerly, DailyState())

    init {
        viewModelScope.launch { _library.value = repo.library() }
    }

    fun category(id: String): Category? {
        val lib = _library.value ?: return null
        if (id == FAVORITES_ID) {
            val fav = favorites.value
            val items = lib.categories.flatMap { it.items }.filter { it.id in fav }
            return Category(FAVORITES_ID, "المفضلة", items = items)
        }
        return lib.categories.firstOrNull { it.id == id }
    }

    fun item(id: String): Pair<Category, Int>? {
        val lib = _library.value ?: return null
        for (c in lib.categories) {
            val i = c.items.indexOfFirst { it.id == id }
            if (i >= 0) return c to i
        }
        return null
    }

    /** Book entries that carry a recitable zikr — offered when building a wird. */
    fun recitable(): List<Pair<Category, Item>> =
        _library.value?.categories.orEmpty().flatMap { c -> c.items.filter { it.hasZikr }.map { c to it } }

    fun toggleFavorite(itemId: String) = viewModelScope.launch { store.toggleFavorite(itemId) }
    fun setTheme(m: ThemeMode) = viewModelScope.launch { store.setTheme(m) }
    fun setFontScale(v: Float) = viewModelScope.launch { store.setFontScale(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { store.setHaptics(v) }
    fun setAutoAdvance(v: Boolean) = viewModelScope.launch { store.setAutoAdvance(v) }

    fun saveWird(text: String, target: Int, itemId: String?, id: String? = null) =
        viewModelScope.launch { store.saveWird(text.trim(), target, itemId, id) }

    fun deleteWird(id: String) = viewModelScope.launch { store.deleteWird(id) }
    fun moveWird(id: String, delta: Int) = viewModelScope.launch { store.moveWird(id, delta) }
    fun addCount(id: String, delta: Int = 1) = viewModelScope.launch { store.addCount(id, delta) }
    fun resetCount(id: String) = viewModelScope.launch { store.resetCount(id) }

    companion object {
        const val FAVORITES_ID = "favorites"
    }
}
