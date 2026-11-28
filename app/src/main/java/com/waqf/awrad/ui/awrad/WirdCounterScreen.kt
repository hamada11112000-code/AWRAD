package com.waqf.awrad.ui.awrad

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waqf.awrad.data.Wird
import com.waqf.awrad.data.ar
import com.waqf.awrad.ui.components.PatternBackground
import com.waqf.awrad.ui.components.ProgressRing
import com.waqf.awrad.ui.theme.Amiri
import com.waqf.awrad.ui.theme.Brand

/** Full-screen tasbeeh counter for one wird: tap anywhere to count. */
@Composable
fun WirdCounterScreen(
    wird: Wird,
    count: Int,
    haptics: Boolean,
    onIncrement: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    val view = LocalView.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.5f), label = "press")
    val done = count >= wird.target

    PatternBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .clickable(interactionSource = source, indication = null) {
                    if (!done) {
                        if (haptics) {
                            view.performHapticFeedback(
                                if (count + 1 >= wird.target) HapticFeedbackConstants.LONG_PRESS
                                else HapticFeedbackConstants.KEYBOARD_TAP
                            )
                        }
                        onIncrement()
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع", tint = Brand.GoldLight) }
                Spacer(Modifier.weight(1f))
                IconButton(onUndo, enabled = count > 0) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, "تراجع", tint = Brand.GoldLight.copy(alpha = if (count > 0) 1f else 0.3f))
                }
                IconButton(onReset, enabled = count > 0) {
                    Icon(Icons.Rounded.Refresh, "إعادة", tint = Brand.GoldLight.copy(alpha = if (count > 0) 1f else 0.3f))
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    wird.text,
                    fontFamily = Amiri,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 50.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(36.dp))
                ProgressRing(
                    fraction = count.toFloat() / wird.target,
                    modifier = Modifier
                        .size(230.dp)
                        .scale(scale),
                    stroke = 9.dp,
                    color = Brand.Gold,
                ) {
                    Box(
                        Modifier
                            .size(186.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (done) "✓" else count.ar(),
                                color = if (done) Brand.Gold else Color.White,
                                fontSize = 60.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                if (done) "تم الورد بحمد الله" else "من ${wird.target.ar()}",
                                color = Brand.GoldLight,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
            }
            Text(
                if (done) "تقبّل الله منك" else "اضغط في أي مكان للعدّ",
                Modifier.padding(bottom = 28.dp),
                color = Color(0xFFB9CCC6),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
