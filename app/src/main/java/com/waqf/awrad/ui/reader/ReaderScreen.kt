package com.waqf.awrad.ui.reader

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waqf.awrad.data.Category
import com.waqf.awrad.data.Settings
import com.waqf.awrad.data.ar
import com.waqf.awrad.ui.components.PatternBackground
import com.waqf.awrad.ui.components.ProgressRing
import com.waqf.awrad.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    category: Category,
    startIndex: Int,
    settings: Settings,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onBack: () -> Unit,
) {
    val items = category.items
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))) { items.size }
    val counts = remember(category.id) { mutableStateMapOf<Int, Int>() }
    val expanded = remember(category.id) { mutableStateMapOf<Int, Boolean>() }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val clipboard = LocalClipboardManager.current

    PatternBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderButton(onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع", tint = Brand.GoldLight)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        category.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Brand.GoldLight,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (items.isNotEmpty()) {
                        Text(
                            "${(pager.currentPage + 1).ar()} من ${items.size.ar()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB9CCC6),
                        )
                    }
                }
                val current = items.getOrNull(pager.currentPage)
                val fav = current != null && current.id in favorites
                HeaderButton({ current?.let { onToggleFavorite(it.id) } }) {
                    Icon(
                        if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "المفضلة",
                        tint = if (fav) Brand.Gold else Brand.GoldLight,
                    )
                }
            }

            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد عناصر هنا بعد", color = Brand.GoldLight, style = MaterialTheme.typography.titleMedium)
                }
                return@Column
            }

            PageIndicator(items.size, pager.currentPage)

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
            ) { page ->
                val item = items[page]
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    ZikrCard(
                        item = item,
                        fontScale = settings.fontScale,
                        expanded = expanded[page] == true,
                        onToggleDetails = { expanded[page] = expanded[page] != true },
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        IconButton({ clipboard.setText(AnnotatedString(item.displayText)) }) {
                            Icon(Icons.Rounded.ContentCopy, "نسخ", tint = Brand.GoldLight.copy(alpha = 0.8f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Counter / next
            val page = pager.currentPage
            val item = items[page]
            val target = item.count
            val done = counts[page] ?: 0
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (target > 0) {
                    CounterButton(
                        done = done,
                        target = target,
                        onTap = {
                            if (done < target) {
                                val n = done + 1
                                counts[page] = n
                                if (settings.haptics) {
                                    view.performHapticFeedback(
                                        if (n == target) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP
                                    )
                                }
                                if (n == target && settings.autoAdvance && page < items.lastIndex) {
                                    scope.launch {
                                        delay(380)
                                        pager.animateScrollToPage(page + 1)
                                    }
                                }
                            } else if (page < items.lastIndex) {
                                scope.launch { pager.animateScrollToPage(page + 1) }
                            }
                        },
                        onReset = { counts[page] = 0 },
                    )
                } else {
                    NextButton(
                        label = if (page < items.lastIndex) "التالي" else "تم بحمد الله",
                        onClick = {
                            if (page < items.lastIndex) scope.launch { pager.animateScrollToPage(page + 1) } else onBack()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun PageIndicator(total: Int, current: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (total <= 24) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (i <= current) Brand.Gold else Color.White.copy(alpha = 0.15f))
                )
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((current + 1f) / total)
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brand.Gold)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CounterButton(done: Int, target: Int, onTap: () -> Unit, onReset: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, spring(dampingRatio = 0.5f), label = "press")
    val complete = done >= target
    Box(contentAlignment = Alignment.Center) {
        ProgressRing(
            fraction = done.toFloat() / target,
            modifier = Modifier.size(150.dp),
            stroke = 6.dp,
            color = Brand.Gold,
        ) {
            Box(
                Modifier
                    .size(118.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            if (complete) listOf(Brand.Gold, Color(0xFF9C7A3A))
                            else listOf(Brand.TealBright, Color(0xFF0E4A41))
                        )
                    )
                    .combinedClickable(
                        interactionSource = source,
                        indication = null,
                        onClick = onTap,
                        onLongClick = onReset,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (complete) "✓" else done.ar(),
                        color = if (complete) Brand.Deep else Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        if (complete) "اكتمل" else "من ${target.ar()}",
                        color = if (complete) Brand.Deep else Brand.GoldLight,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        if (done > 0) {
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 0.dp)) {
                IconButton(onReset) {
                    Icon(Icons.Rounded.Refresh, "إعادة العد", tint = Brand.GoldLight.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun NextButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brand.Gold)
            .clickable(onClick = onClick)
            .padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Brand.Deep, style = MaterialTheme.typography.titleMedium)
    }
}
