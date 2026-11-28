package com.waqf.awrad.ui.awrad

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waqf.awrad.data.Category
import com.waqf.awrad.data.DailyState
import com.waqf.awrad.data.Item
import com.waqf.awrad.data.Wird
import com.waqf.awrad.data.ar
import com.waqf.awrad.data.timesWord
import com.waqf.awrad.ui.components.PatternBackground
import com.waqf.awrad.ui.components.ProgressBar
import com.waqf.awrad.ui.components.ProgressRing
import com.waqf.awrad.ui.components.SectionTitle
import com.waqf.awrad.ui.theme.Amiri
import com.waqf.awrad.ui.theme.AwradTheme
import com.waqf.awrad.ui.theme.Brand

@Composable
fun AwradScreen(
    daily: DailyState,
    recitable: () -> List<Pair<Category, Item>>,
    onOpenWird: (String) -> Unit,
    onSave: (text: String, target: Int, itemId: String?, id: String?) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, Int) -> Unit,
) {
    val c = AwradTheme.colors
    var editing by remember { mutableStateOf<Wird?>(null) }
    var adding by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
            item { Hero(daily) }
            item {
                SectionTitle("أورادك", Modifier.padding(top = 14.dp)) {
                    if (daily.awrad.isNotEmpty()) {
                        Text("اضغط مطولًا للتعديل", color = c.mute, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (daily.awrad.isEmpty()) {
                item { EmptyState { adding = true } }
            }
            items(daily.awrad, key = { it.id }) { w ->
                WirdRow(
                    wird = w,
                    count = daily.countOf(w),
                    onClick = { onOpenWird(w.id) },
                    onLongClick = { editing = w },
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            containerColor = Brand.Gold,
            contentColor = Brand.Deep,
            shape = RoundedCornerShape(18.dp),
            icon = { Icon(Icons.Rounded.Add, null) },
            text = { Text("ورد جديد", style = MaterialTheme.typography.titleMedium) },
        )
    }

    if (adding || editing != null) {
        WirdEditorSheet(
            initial = editing,
            recitable = recitable,
            onDismiss = { adding = false; editing = null },
            onSave = { text, target, itemId ->
                onSave(text, target, itemId, editing?.id)
                adding = false; editing = null
            },
            onDelete = editing?.let { w -> { onDelete(w.id); editing = null } },
            onMove = editing?.let { w -> { d: Int -> onMove(w.id, d) } },
        )
    }
}

@Composable
private fun Hero(daily: DailyState) {
    PatternBackground(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            Text("أورادي اليومية", color = Brand.GoldLight, style = MaterialTheme.typography.displaySmall)
            Text("وردٌ تلزم به نفسك كل يوم، يتجدد عدّه مع كل صباح", color = Color(0xFFC8DAD4), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(daily.fraction, Modifier.size(84.dp), stroke = 7.dp) {
                    Text(
                        "${(daily.fraction * 100).toInt().ar()}٪",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Spacer(Modifier.width(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Stat(daily.totalDone.ar(), "ذكرًا اليوم")
                    Stat("${daily.completed.ar()} / ${daily.awrad.size.ar()}", "أوراد")
                    Stat(daily.streak.ar(), "يوم متتالٍ")
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge)
        Text(label, color = Color(0xFFC8DAD4), style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WirdRow(wird: Wird, count: Int, onClick: () -> Unit, onLongClick: () -> Unit) {
    val c = AwradTheme.colors
    val done = count >= wird.target
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                wird.text,
                fontFamily = Amiri,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = c.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            ProgressBar(count.toFloat() / wird.target)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.width(66.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (done) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.goldSoft),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, null, tint = c.goldInk, modifier = Modifier.size(18.dp)) }
                Text("اكتمل · ${wird.target.ar()}", color = c.goldInk, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            } else {
                Text("${count.ar()} / ${wird.target.ar()}", color = c.accent, style = MaterialTheme.typography.titleSmall)
                Text(timesWord(wird.target), color = c.mute, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val c = AwradTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(c.surface)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("لم تضف وردًا بعد", color = c.ink, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "اختر ذكرًا مثل «لا حول ولا قوة إلا بالله» وحدد عدده، مثلًا مائة مرة، ليظهر هنا كل يوم.",
            color = c.mute,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "＋ أضف وردك الأول",
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(c.accent)
                .clickable(onClick = onAdd)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
