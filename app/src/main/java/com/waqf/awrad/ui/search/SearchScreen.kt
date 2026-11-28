package com.waqf.awrad.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waqf.awrad.data.ContentRepository
import com.waqf.awrad.data.Library
import com.waqf.awrad.data.ar
import com.waqf.awrad.ui.theme.Amiri
import com.waqf.awrad.ui.theme.AwradTheme

@Composable
fun SearchScreen(
    library: Library?,
    repo: ContentRepository,
    onOpen: (categoryId: String, index: Int) -> Unit,
) {
    val c = AwradTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val hits = remember(query, library) { library?.let { repo.search(it, query) }.orEmpty() }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
    ) {
        Text(
            "البحث",
            Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
            style = MaterialTheme.typography.displaySmall,
            color = c.ink,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("اكتب كلمة من الذكر، مثل: أصبحنا") },
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
        if (query.isNotBlank() && hits.isEmpty()) {
            Text(
                "لا نتائج",
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                color = c.mute,
                textAlign = TextAlign.Center,
            )
        } else if (query.isNotBlank()) {
            Text(
                "${hits.size.ar()} نتيجة",
                Modifier.padding(horizontal = 20.dp),
                color = c.mute,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(hits, key = { it.item.id }) { hit ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.surface)
                        .clickable { onOpen(hit.category.id, hit.index) }
                        .padding(14.dp)
                ) {
                    Text(hit.category.title, color = c.goldInk, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        hit.item.displayText,
                        fontFamily = Amiri,
                        fontSize = 17.sp,
                        lineHeight = 28.sp,
                        color = c.ink,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
