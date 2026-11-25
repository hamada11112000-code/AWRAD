package com.waqf.awrad.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waqf.awrad.data.Item
import com.waqf.awrad.data.ar
import com.waqf.awrad.data.countLabel
import com.waqf.awrad.ui.components.Ornament
import com.waqf.awrad.ui.components.Pill
import com.waqf.awrad.ui.theme.Amiri
import com.waqf.awrad.ui.theme.AwradTheme

/**
 * One entry of the book: the zikr in large script, its virtue (الفضل) beneath,
 * and the chain of narration, source and takhrij folded away behind a toggle.
 */
@Composable
fun ZikrCard(
    item: Item,
    fontScale: Float,
    expanded: Boolean,
    onToggleDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AwradTheme.colors
    val shape = RoundedCornerShape(28.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = c.card,
        shadowElevation = 10.dp,
    ) {
        Column(
            Modifier
                .padding(7.dp)
                .border(BorderStroke(1.dp, c.cardBorder), RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.grade.isNotBlank()) Pill(item.grade, c.chipGreenBg, c.accent)
                if (item.isAyah) Pill("آية", c.chipGreenBg, c.accent)
                Spacer(Modifier.weight(1f))
                if (item.count > 0) Pill(countLabel(item.count), c.goldSoft, c.goldInk)
            }
            if (item.subheading.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    item.subheading,
                    Modifier.fillMaxWidth(),
                    color = c.goldInk,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(14.dp))

            val big = item.hasZikr || item.isAyah
            val size = (if (big) 25f else 20f) * fontScale
            Text(
                item.displayText,
                Modifier.fillMaxWidth(),
                fontFamily = Amiri,
                fontWeight = if (big) FontWeight.Bold else FontWeight.Normal,
                fontSize = size.sp,
                lineHeight = (size * 1.85f).sp,
                color = c.ink,
                textAlign = if (big) TextAlign.Center else TextAlign.Justify,
            )
            if (item.isAyah && item.ref.isNotBlank()) {
                Text(
                    "[${item.ref}]",
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = c.mute,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(12.dp))
            Ornament()

            if (item.fadl.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.fadlBg)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text("الفضل", color = c.goldInk, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.fadl,
                        fontFamily = Amiri,
                        fontSize = (17f * fontScale).sp,
                        lineHeight = (17f * fontScale * 1.7f).sp,
                        color = c.inkSoft,
                    )
                }
            }

            if (item.hasHiddenDetails) {
                Spacer(Modifier.height(12.dp))
                DetailsToggle(expanded, onToggleDetails)
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    HiddenDetails(item, fontScale)
                }
            }
        }
    }
}

@Composable
private fun DetailsToggle(expanded: Boolean, onClick: () -> Unit) {
    val c = AwradTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = c.gold,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = c.goldInk,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (expanded) "إخفاء الإسناد والتخريج" else "إظهار الإسناد والتخريج",
                color = c.goldInk,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun HiddenDetails(item: Item, fontScale: Float) {
    val c = AwradTheme.colors
    Column(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val narration = listOf(item.isnad, item.matn.takeIf { item.hasZikr }.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(" ")
        if (item.source.isNotBlank()) DetailBlock("المصدر", item.source, fontScale)
        if (narration.isNotBlank()) DetailBlock(if (item.hasZikr) "الإسناد والمتن" else "الإسناد", narration, fontScale)
        if (item.extra.isNotBlank()) DetailBlock("زيادة", item.extra, fontScale)
        if (item.grade.isNotBlank()) DetailBlock("الحكم", item.grade, fontScale)
        if (item.takhrij.isNotBlank()) DetailBlock("التخريج", item.takhrij, fontScale)
        item.notes.forEach { DetailBlock("حاشية", it, fontScale) }
        val ref = buildString {
            item.num?.let { append("رقم ${it.ar()} · ") }
            append("الصحيح المسند ص${item.page.ar()}")
        }
        Text(ref, color = c.mute, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DetailBlock(label: String, body: String, fontScale: Float) {
    val c = AwradTheme.colors
    Column {
        Text(label, color = c.accent, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(2.dp))
        Text(
            body,
            fontFamily = Amiri,
            fontSize = (15.5f * fontScale).sp,
            lineHeight = (15.5f * fontScale * 1.75f).sp,
            color = c.inkSoft,
            textAlign = TextAlign.Justify,
        )
    }
}
