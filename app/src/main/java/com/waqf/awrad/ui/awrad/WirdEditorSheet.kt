package com.waqf.awrad.ui.awrad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waqf.awrad.data.Category
import com.waqf.awrad.data.Item
import com.waqf.awrad.data.Wird
import com.waqf.awrad.data.WirdPresets
import com.waqf.awrad.data.ar
import com.waqf.awrad.data.normalizeArabic
import com.waqf.awrad.ui.theme.Amiri
import com.waqf.awrad.ui.theme.AwradTheme
import com.waqf.awrad.ui.theme.Brand

/** Bottom sheet to create or edit a wird: pick a phrase (common, from the book, or your own) and a count. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WirdEditorSheet(
    initial: Wird?,
    recitable: () -> List<Pair<Category, Item>>,
    onDismiss: () -> Unit,
    onSave: (text: String, target: Int, itemId: String?) -> Unit,
    onDelete: (() -> Unit)?,
    onMove: ((Int) -> Unit)?,
) {
    val c = AwradTheme.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var itemId by remember { mutableStateOf(initial?.itemId) }
    var target by remember { mutableIntStateOf(initial?.target ?: 100) }
    var targetText by remember { mutableStateOf(target.toString()) }
    var tab by remember { mutableIntStateOf(0) }
    var bookQuery by remember { mutableStateOf("") }
    val book = remember { recitable() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = c.background,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (initial == null) "ورد جديد" else "تعديل الورد",
                    style = MaterialTheme.typography.headlineMedium,
                    color = c.ink,
                    modifier = Modifier.weight(1f),
                )
                if (onMove != null) {
                    IconButton({ onMove(-1) }) { Icon(Icons.Rounded.ArrowUpward, "تقديم", tint = c.mute) }
                    IconButton({ onMove(1) }) { Icon(Icons.Rounded.ArrowDownward, "تأخير", tint = c.mute) }
                }
                if (onDelete != null) {
                    IconButton(onDelete) { Icon(Icons.Rounded.Delete, "حذف", tint = Brand.Gold) }
                }
            }
            Spacer(Modifier.height(10.dp))

            // Chosen text preview
            Text(
                text.ifBlank { "اختر ذكرًا من الأسفل أو اكتب وردك" },
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.card)
                    .border(1.dp, c.cardBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                fontFamily = Amiri,
                fontSize = 20.sp,
                lineHeight = 34.sp,
                color = if (text.isBlank()) c.mute else c.ink,
            )

            Spacer(Modifier.height(16.dp))
            Text("العدد اليومي", style = MaterialTheme.typography.titleSmall, color = c.ink)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WirdPresets.quickCounts.forEach { n ->
                    Choice(n.ar(), selected = target == n) { target = n; targetText = n.toString() }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = targetText,
                onValueChange = { v ->
                    val digits = v.filter { it.isDigit() }.take(5)
                    targetText = digits
                    digits.toIntOrNull()?.let { if (it > 0) target = it }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("عدد مخصص") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors(),
            )

            Spacer(Modifier.height(16.dp))
            TabRow(
                selectedTabIndex = tab,
                containerColor = c.background,
                contentColor = c.accent,
            ) {
                listOf("أذكار شائعة", "من الكتاب", "اكتب وردك").forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, style = MaterialTheme.typography.labelLarge) })
                }
            }
            Spacer(Modifier.height(12.dp))
            when (tab) {
                0 -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WirdPresets.phrases.forEach { p ->
                        Choice(p, selected = text == p && itemId == null) { text = p; itemId = null }
                    }
                }
                1 -> {
                    OutlinedTextField(
                        value = bookQuery,
                        onValueChange = { bookQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ابحث في أذكار الكتاب") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                    )
                    Spacer(Modifier.height(8.dp))
                    val q = bookQuery.normalizeArabic().trim()
                    val list = remember(q, book) {
                        if (q.isEmpty()) book else book.filter { (cat, it) ->
                            it.zikr.normalizeArabic().contains(q) || cat.title.normalizeArabic().contains(q)
                        }
                    }
                    LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(list, key = { it.second.id }) { (cat, item) ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (itemId == item.id) c.accentSoft else c.surface)
                                    .clickable {
                                        text = item.zikr; itemId = item.id
                                        if (item.count > 0) { target = item.count; targetText = item.count.toString() }
                                    }
                                    .padding(12.dp)
                            ) {
                                Text(cat.title, color = c.goldInk, style = MaterialTheme.typography.labelSmall)
                                Text(item.zikr, fontFamily = Amiri, fontSize = 16.sp, color = c.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                else -> OutlinedTextField(
                    value = if (itemId == null) text else "",
                    onValueChange = { text = it; itemId = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("مثال: اللهم صل على محمد") },
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors(),
                )
            }

            Spacer(Modifier.height(20.dp))
            val valid = text.isNotBlank() && target > 0
            Text(
                "حفظ الورد",
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (valid) Brand.Gold else c.track)
                    .clickable(enabled = valid) { onSave(text, target, itemId) }
                    .padding(vertical = 14.dp),
                color = Brand.Deep,
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = AwradTheme.colors
    Text(
        label,
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.accent else c.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        color = if (selected) androidx.compose.ui.graphics.Color.White else c.ink,
        fontFamily = Amiri,
        fontSize = 16.sp,
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AwradTheme.colors.surface,
    unfocusedContainerColor = AwradTheme.colors.surface,
    focusedBorderColor = AwradTheme.colors.gold,
    unfocusedBorderColor = AwradTheme.colors.cardBorder,
)
