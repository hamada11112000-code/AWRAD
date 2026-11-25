package com.waqf.awrad.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.waqf.awrad.ui.awrad.AwradScreen
import com.waqf.awrad.ui.awrad.WirdCounterScreen
import com.waqf.awrad.ui.home.HomeScreen
import com.waqf.awrad.ui.reader.ReaderScreen
import com.waqf.awrad.ui.search.SearchScreen
import com.waqf.awrad.ui.settings.SettingsScreen
import com.waqf.awrad.ui.theme.AwradTheme

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "الأذكار", Icons.Outlined.MenuBook),
    Tab("awrad", "أورادي", Icons.Outlined.AutoAwesome),
    Tab("search", "بحث", Icons.Outlined.Search),
    Tab("settings", "الإعدادات", Icons.Outlined.Settings),
)

@Composable
fun AwradApp(vm: AppViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val daily by vm.daily.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    // The whole app is Arabic: always lay out right-to-left, whatever the phone language.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AwradTheme(settings.theme) {
            Scaffold(
                containerColor = AwradTheme.colors.background,
                bottomBar = {
                    if (tabs.any { it.route == route }) {
                        BottomBar(route) { r ->
                            nav.navigate(r) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
            ) { pad ->
                Box(Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding())) {
                    NavHost(
                        navController = nav,
                        startDestination = "home",
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() },
                    ) {
                        composable("home") {
                            HomeScreen(
                                library = library,
                                daily = daily,
                                favoritesCount = favorites.size,
                                onOpenCategory = { nav.navigate("reader/$it/0") },
                                onOpenFavorites = { nav.navigate("reader/${AppViewModel.FAVORITES_ID}/0") },
                                onOpenAwrad = { nav.navigate("awrad") { launchSingleTop = true } },
                            )
                        }
                        composable("awrad") {
                            AwradScreen(
                                daily = daily,
                                recitable = vm::recitable,
                                onOpenWird = { nav.navigate("wird/$it") },
                                onSave = { text, target, itemId, id -> vm.saveWird(text, target, itemId, id) },
                                onDelete = { vm.deleteWird(it) },
                                onMove = { id, d -> vm.moveWird(id, d) },
                            )
                        }
                        composable("search") {
                            SearchScreen(library, vm.repo) { cat, index -> nav.navigate("reader/$cat/$index") }
                        }
                        composable("settings") {
                            SettingsScreen(
                                settings = settings,
                                library = library,
                                versionName = versionName,
                                onTheme = { vm.setTheme(it) },
                                onFontScale = { vm.setFontScale(it) },
                                onHaptics = { vm.setHaptics(it) },
                                onAutoAdvance = { vm.setAutoAdvance(it) },
                            )
                        }
                        composable(
                            "reader/{cat}/{index}",
                            arguments = listOf(
                                navArgument("cat") { type = NavType.StringType },
                                navArgument("index") { type = NavType.IntType },
                            ),
                        ) { e ->
                            val catId = e.arguments?.getString("cat").orEmpty()
                            val index = e.arguments?.getInt("index") ?: 0
                            // Re-read when favourites change so the favourites reader stays current.
                            val cat = remember(library, if (catId == AppViewModel.FAVORITES_ID) favorites else null) {
                                vm.category(catId)
                            }
                            if (cat != null) {
                                ReaderScreen(
                                    category = cat,
                                    startIndex = index,
                                    settings = settings,
                                    favorites = favorites,
                                    onToggleFavorite = { vm.toggleFavorite(it) },
                                    onBack = { nav.popBackStack() },
                                )
                            }
                        }
                        composable("wird/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                            val id = e.arguments?.getString("id").orEmpty()
                            val wird = daily.awrad.firstOrNull { it.id == id }
                            if (wird != null) {
                                WirdCounterScreen(
                                    wird = wird,
                                    count = daily.countOf(wird),
                                    haptics = settings.haptics,
                                    onIncrement = { vm.addCount(id, 1) },
                                    onUndo = { vm.addCount(id, -1) },
                                    onReset = { vm.resetCount(id) },
                                    onBack = { nav.popBackStack() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(current: String?, onSelect: (String) -> Unit) {
    val c = AwradTheme.colors
    NavigationBar(containerColor = c.surface, tonalElevation = 0.dp) {
        tabs.forEach { t ->
            NavigationBarItem(
                selected = current == t.route,
                onClick = { onSelect(t.route) },
                icon = { Icon(t.icon, contentDescription = t.label) },
                label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.accent,
                    selectedTextColor = c.accent,
                    indicatorColor = c.accentSoft,
                    unselectedIconColor = c.mute,
                    unselectedTextColor = c.mute,
                ),
            )
        }
    }
}
