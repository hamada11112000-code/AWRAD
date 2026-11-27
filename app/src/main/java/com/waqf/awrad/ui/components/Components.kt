package com.waqf.awrad.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.waqf.awrad.R
import com.waqf.awrad.ui.theme.AwradTheme
import com.waqf.awrad.ui.theme.Brand
import kotlin.math.cos
import kotlin.math.sin

/** Deep green backdrop with a faint eight-pointed-star lattice, used behind headers and the reader. */
@Composable
fun PatternBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    Box(
        modifier
            .background(Brush.verticalGradient(listOf(Brand.Green, Brand.Deep)))
    ) {
        Canvas(Modifier.fillMaxSize()) { drawStarLattice(Brand.GoldLight.copy(alpha = 0.07f), 64.dp.toPx()) }
        content()
    }
}

fun DrawScope.drawStarLattice(color: Color, cell: Float) {
    val stroke = Stroke(width = 1.dp.toPx())
    var y = 0f
    var row = 0
    while (y < size.height + cell) {
        var x = if (row % 2 == 0) 0f else cell / 2
        while (x < size.width + cell) {
            drawStar(Offset(x, y), cell * 0.36f, color, stroke)
            x += cell
        }
        y += cell * 0.87f
        row++
    }
}

private fun DrawScope.drawStar(c: Offset, r: Float, color: Color, stroke: Stroke) {
    for (rot in listOf(0f, 45f)) {
        val p = Path()
        for (i in 0 until 4) {
            val a = Math.toRadians((rot + 90 * i).toDouble())
            val pt = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
            if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
        }
        p.close()
        drawPath(p, color, style = stroke)
    }
    drawCircle(color, r * 0.42f, c, style = stroke)
}

/** The app emblem (placeholder until the official Waqf logo is supplied). */
@Composable
fun Emblem(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        val c = center
        drawCircle(Brand.Deep, r, c)
        drawCircle(Brand.GoldLight, r * 0.86f, c, style = Stroke(1.2.dp.toPx()))
        for (rot in listOf(0f, 45f)) {
            rotate(rot, c) {
                drawRect(
                    Brand.Gold,
                    topLeft = Offset(c.x - r * 0.45f, c.y - r * 0.45f),
                    size = Size(r * 0.9f, r * 0.9f),
                )
            }
        }
        drawCircle(Brand.Deep, r * 0.3f, c)
        drawCircle(Brand.GoldLight, r * 0.2f, c, style = Stroke(1.dp.toPx()))
    }
}

/**
 * ختم «وَقْف» الخافت المائل خلف محتوى «عن التطبيق».
 *
 * الهوية البصرية الثابتة لكل مشروع يصدر عن «وَقْف»، منقولة من مشروع «قيراطان»
 * (`design/brand/WAQF_SEAL_IDENTITY.md`). الختم زخرفة لا يقرؤها قارئ الشاشة،
 * و**ليس شعارًا في الواجهة** بنصّ دليل الهوية، فلا يوضع في الترويسات.
 */
@Composable
fun WaqfSealBackground(content: @Composable BoxScope.() -> Unit) {
    val c = AwradTheme.colors
    Box {
        Image(
            painterResource(R.drawable.waqf_seal),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(176.dp)
                .graphicsLayer { rotationZ = -12f },
            alpha = if (c.isDark) 0.10f else 0.14f,
            colorFilter = if (c.isDark) ColorFilter.tint(c.ink) else null,
        )
        content()
    }
}

/** Circular progress ring. */
@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 6.dp,
    track: Color = Color.White.copy(alpha = 0.14f),
    color: Color = Brand.GoldLight,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(450), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val d = this.size.minDimension - w
            val tl = Offset((this.size.width - d) / 2, (this.size.height - d) / 2)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(w))
            if (anim > 0f) {
                drawArc(color, -90f, 360f * anim, false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

/** Rounded linear bar with a gold→green fill. */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val c = AwradTheme.colors
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(400), label = "bar")
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(c.track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(anim)
                .height(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.horizontalGradient(listOf(Brand.TealBright, Brand.Gold)))
        )
    }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = fg,
        style = MaterialTheme.typography.labelMedium,
    )
}

/** ✦ ❖ ✦ divider. */
@Composable
fun Ornament(modifier: Modifier = Modifier, color: Color = AwradTheme.colors.gold) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(36.dp).height(1.dp).background(color.copy(alpha = 0.5f)))
        Spacer(Modifier.width(8.dp))
        Text("✦ ❖ ✦", color = color, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.width(36.dp).height(1.dp).background(color.copy(alpha = 0.5f)))
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = AwradTheme.colors.ink, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}
