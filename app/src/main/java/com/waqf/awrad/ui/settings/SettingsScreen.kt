package com.waqf.awrad.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waqf.awrad.data.Library
import com.waqf.awrad.data.Settings
import com.waqf.awrad.data.ThemeMode
import com.waqf.awrad.ui.components.Emblem
import com.waqf.awrad.ui.components.WaqfSealBackground
import com.waqf.awrad.ui.theme.Amiri
import com.waqf.awrad.ui.theme.AwradTheme

@Composable
fun SettingsScreen(
    settings: Settings,
    library: Library?,
    versionName: String,
    onTheme: (ThemeMode) -> Unit,
    onFontScale: (Float) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onAutoAdvance: (Boolean) -> Unit,
) {
    val c = AwradTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("الإعدادات", style = MaterialTheme.typography.displaySmall, color = c.ink, modifier = Modifier.padding(horizontal = 4.dp))

        Card {
            Text("المظهر", style = MaterialTheme.typography.titleSmall, color = c.ink)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ThemeMode.SYSTEM to "تلقائي", ThemeMode.LIGHT to "فاتح", ThemeMode.DARK to "ليلي").forEach { (m, label) ->
                    val sel = settings.theme == m
                    Text(
                        label,
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) c.accent else c.card)
                            .clickable { onTheme(m) }
                            .padding(vertical = 10.dp),
                        color = if (sel) Color.White else c.ink,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        Card {
            Text("حجم خط الأذكار", style = MaterialTheme.typography.titleSmall, color = c.ink)
            Text(
                "سبحان الله وبحمده",
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                fontFamily = Amiri,
                fontSize = (24 * settings.fontScale).sp,
                color = c.ink,
                textAlign = TextAlign.Center,
            )
            Slider(
                value = settings.fontScale,
                onValueChange = onFontScale,
                valueRange = 0.8f..1.6f,
                steps = 7,
                colors = SliderDefaults.colors(thumbColor = c.gold, activeTrackColor = c.accent),
            )
        }

        Card {
            ToggleRow("الاهتزاز عند العدّ", settings.haptics, onHaptics)
            Spacer(Modifier.height(6.dp))
            ToggleRow("الانتقال تلقائيًا للذكر التالي عند اكتمال العدد", settings.autoAdvance, onAutoAdvance)
        }

        Card {
            WaqfSealBackground {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Emblem(size = 48.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("أوراد", style = MaterialTheme.typography.headlineSmall, color = c.ink)
                            Text("الإصدار $versionName", style = MaterialTheme.typography.labelSmall, color = c.mute)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "وقفٌ لله تعالى، لا يُباع. نسأل الله أن يتقبله وأن ينفع به.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.inkSoft,
                    )
                    Spacer(Modifier.height(8.dp))
                    val book = library?.book
                    Text(
                        "المحتوى منقول من كتاب «${book?.title?.ifBlank { null } ?: "الصحيح المسند من أذكار اليوم والليلة"}» " +
                            "للشيخ ${book?.author?.ifBlank { null } ?: "مصطفى بن العدوي"}، بأسانيده وتخريجه وأحكامه، " +
                            "مع ذكر رقم الصفحة في الكتاب عند كل ذكر للرجوع إليه.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.mute,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "الخطوط: أميري (Amiri) وتجوال (Tajawal) برخصة SIL Open Font License.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.mute,
                    )
                    // مساحة يظهر فيها الختم تحت آخر سطر: ١٢٠dp بنصّ دليل الهوية
                    Spacer(Modifier.height(120.dp))
                }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val c = AwradTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = AwradTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = c.ink)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = Color.White),
        )
    }
}
