package com.ruvo.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Ruvo's bottom sheet: rounded, canvas-coloured, with a title and a close button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuvoSheet(
    title: String,
    onDismiss: () -> Unit,
    eyebrow: String? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val c = Ruvo.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        // The top of the sky, so the strip behind the handle meets the glowing sky below without a seam.
        containerColor = c.canvasTop,
        scrimColor = c.scrim,
        // The content pads itself for the navigation bar, so the glowing sky reaches the very bottom.
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) },
        shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
        dragHandle = {
            Box(Modifier.padding(top = Space.sm).size(width = 40.dp, height = 5.dp).clip(RoundedCornerShape(Radius.chip)).background(c.ink.copy(alpha = 0.2f)))
        },
    ) {
        val scroll = rememberScrollState()
        Column(
            Modifier.fillMaxWidth()
                // The same glowing sky as the screens, held still, so glass cards look the same in a sheet.
                .drawBehind { drawSky(c, 0.15f, 0.55f) }
                .let { if (scrollable) it.verticalScroll(scroll) else it }
                .padding(horizontal = Space.gutter)
                .navigationBarsPadding()
                .padding(bottom = Space.lg),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = Space.sm), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (eyebrow != null) Eyebrow(eyebrow)
                    Text(title, Modifier.semantics { heading() }, style = Ruvo.type.headline, color = c.ink)
                }
                IconCircle(RuvoIcons.Close, "Close", onDismiss)
            }
            Spacer(Modifier.height(Space.md))
            content()
        }
    }
}

/** A text field in Ruvo's style. */
@Composable
fun RuvoField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    number: Boolean = false,
    prefix: String? = null,
    /** A mobile number: digits only, at most 10, with the phone keypad. */
    phone: Boolean = false,
    error: String? = null,
) {
    val c = Ruvo.colors
    OutlinedTextField(
        value = value,
        onValueChange = { v ->
            onValueChange(
                when {
                    phone -> v.filter { it.isDigit() }.take(10)
                    number -> v.filter { it.isDigit() || it == '.' }.take(9)
                    else -> v
                },
            )
        },
        isError = error != null,
        supportingText = error?.let { e -> { Text(e, style = Ruvo.type.caption, color = c.warning) } },
        label = { Text(label, style = Ruvo.type.label) },
        prefix = if (prefix != null) { { Text(prefix, style = Ruvo.type.bodyStrong, color = c.ink) } } else null,
        singleLine = true,
        textStyle = if (number) Ruvo.type.title else Ruvo.type.body,
        keyboardOptions = KeyboardOptions(keyboardType = when { phone -> KeyboardType.Phone; number -> KeyboardType.Number; else -> KeyboardType.Text }),
        shape = RoundedCornerShape(Radius.inner),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.ink, unfocusedBorderColor = c.hairline, focusedLabelColor = c.ink,
            unfocusedLabelColor = c.inkSoft, cursorColor = c.ink, focusedTextColor = c.ink, unfocusedTextColor = c.ink,
            focusedContainerColor = c.surface.copy(alpha = if (c.isDark) 0.6f else 0.9f), unfocusedContainerColor = c.glass,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Row of buttons with equal widths. */
@Composable
fun ButtonRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm), content = content)
}

/** Parses a typed rupee amount ("1,250.50") into paise. */
fun parseRupees(text: String): Long? {
    val v = text.replace(",", "").trim().toDoubleOrNull() ?: return null
    if (v < 0) return null
    return Math.round(v * 100)
}
