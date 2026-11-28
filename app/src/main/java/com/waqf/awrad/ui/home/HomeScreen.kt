package com.waqf.awrad.ui.home

import android.icu.text.DateFormat
import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waqf.awrad.data.Category
import com.waqf.awrad.data.DailyState
import com.waqf.awrad.data.Library
import com.waqf.awrad.data.ar
import com.waqf.awrad.data.normalizeArabic
import com.waqf.awrad.ui.components.Emblem
import com.waqf.awrad.ui.components.PatternBackground
import com.waqf.awrad.ui.components.ProgressRing
import com.waqf.awrad.ui.components.SectionTitle
import com.waqf.awrad.ui.theme.AwradTheme
import com.waqf.awrad.ui.theme.Brand
import java.time.LocalTime
import java.util.Date

private data class Featured(val key: String, val label: String, val glyph: String)

private val featuredOrder = listOf(
    Featured("morning_evening", "الصباح والمساء", "☀"),
    Featured("sleep", "النوم", "☾"),
    Featured("wake", "الاستيقاظ", "✧"),
    Featured("after_prayer", "بعد الصلاة", "۞"),
    Featured("prayer", "الصلاة", "◈"),
    Featured("masjid", "المسجد", "⌂"),
    Featured("wudu", "الوضوء", "◌"),
    Featured("travel", "السفر", "➶"),
    Featured("food", "الطعام", "❀"),
    Featured("distress", "الكرب والهم", "❖"),
)

@Composable
fun HomeScreen(
    library: Library?,
    daily: DailyState,
    favoritesCount: Int,
    onOpenCategory: (String) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenAwrad: () -> Unit,
) {
    val c = AwradTheme.colors
    var filter by rememberSaveable { mutableStateOf("") }
    val cats = library?.categories.orEmpty()
    val shown = remember(filter, cats) {
        val q = filter.normalizeArabic().trim()
        if (q.isEmpty()) cats else cats.filter { it.title.normalizeArabic().contains(q) || it.bookTitle.normalizeArabic().contains(q) }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(c.background),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { Header(cats, daily, onOpenCategory, onOpenAwrad) }

        val featured = featuredOrder.mapNotNull { f -> cats.firstOrNull { it.featured == f.key }?.let { f to it } }
        if (featured.isNotEmpty() || favoritesCount > 0) {
            item { SectionTitle("وصول سريع", Modifier.padding(top = 12.dp)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (favoritesCount > 0) {
                        item { QuickTile("المفضلة", "♥", favoritesCount, onOpenFavorites) }
                    }
                    items(featured, key = { it.second.id }) { (f, cat) ->
                        QuickTile(f.label, f.glyph, cat.items.size) { onOpenCategory(cat.id) }
                    }
                }
            }
        }

        item {
            SectionTitle("أبواب الأذكار", Modifier.padding(top = 16.dp)) {
                Text("${cats.size.ar()} بابًا", color = c.mute, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("ابحث في الأبواب…") },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = c.mute) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = c.surface,
                    unfocusedContainerColor = c.surface,
                    focusedBorderColor = c.gold,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )
        }
        if (library == null) {
            item {
                Text(
                    "جارٍ التحميل…",
                    Modifier.padding(24.dp),
                    color = c.mute,
                )
            }
        }
        itemsIndexed(shown, key = { _, it -> it.id }) { _, cat ->
            CategoryRow(cats.indexOf(cat) + 1, cat) { onOpenCategory(cat.id) }
        }
    }
}

@Composable
private fun Header(
    cats: List<Category>,
    daily: DailyState,
    onOpenCategory: (String) -> Unit,
    onOpenAwrad: () -> Unit,
) {
    val hour = LocalTime.now().hour
    val isMorning = hour in 3..11
    val hijri = remember {
        runCatching {
            DateFormat.getDateInstance(DateFormat.LONG, ULocale("ar@calendar=islamic-umalqura")).format(Date())
        }.getOrDefault("")
    }
    val morningEvening = cats.firstOrNull { it.featured == "morning_evening" }

    PatternBackground(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(hijri, color = Color(0xFFB9CCC6), style = MaterialTheme.typography.bodySmall)
                    Text("أوراد", color = Brand.GoldLight, style = MaterialTheme.typography.displaySmall)
                    Text("أذكار اليوم والليلة من الصحيح المسند", color = Color(0xFFC8DAD4), style = MaterialTheme.typography.bodySmall)
                }
                Emblem(size = 52.dp)
            }
            Spacer(Modifier.height(18.dp))

            // Time-aware hero: morning or evening adhkar
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .clickable(enabled = morningEvening != null) { morningEvening?.let { onOpenCategory(it.id) } }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brand.Gold.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (isMorning) "☀" else "☾", color = Brand.GoldLight, style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isMorning) "أذكار الصباح" else "أذكار المساء",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        if (isMorning) "ابدأ يومك بذكر الله" else "اختم يومك بذكر الله",
                        color = Color(0xFFC8DAD4),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Brand.GoldLight)
            }

            Spacer(Modifier.height(10.dp))

            // Today's awrad summary
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .clickable(onClick = onOpenAwrad)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProgressRing(daily.fraction, Modifier.size(48.dp), stroke = 5.dp) {
                    Text(
                        "${(daily.fraction * 100).toInt().ar()}٪",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("أورادي اليومية", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (daily.awrad.isEmpty()) "أنشئ وردك اليومي وحدد عدده"
                        else "${daily.completed.ar()} من ${daily.awrad.size.ar()} مكتملة" +
                            if (daily.streak > 0) " · ${daily.streak.ar()} يوم متتالٍ" else "",
                        color = Color(0xFFC8DAD4),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Brand.GoldLight)
            }
        }
    }
}

@Composable
private fun QuickTile(label: String, glyph: String, count: Int, onClick: () -> Unit) {
    val c = AwradTheme.colors
    Column(
        Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(c.surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (glyph == "♥") Icon(Icons.Rounded.Favorite, null, tint = c.gold, modifier = Modifier.size(20.dp))
            else Text(glyph, color = c.accent, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = c.ink, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${count.ar()} ذكر", color = c.mute, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CategoryRow(index: Int, cat: Category, onClick: () -> Unit) {
    val c = AwradTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(c.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.goldSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(index.ar(), color = c.goldInk, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(cat.title, color = c.ink, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${cat.items.size.ar()} · ص${cat.page.ar()}", color = c.mute, style = MaterialTheme.typography.labelSmall)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.mute)
    }
}
