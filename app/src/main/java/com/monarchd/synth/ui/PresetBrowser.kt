package com.monarchd.synth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monarchd.synth.model.Patch
import com.monarchd.synth.model.Preset
import com.monarchd.synth.model.PresetCategory
import com.monarchd.synth.ui.components.EngravedButton
import com.monarchd.synth.ui.components.LedIndicator
import com.monarchd.synth.ui.theme.Amber
import com.monarchd.synth.ui.theme.AmberBright
import com.monarchd.synth.ui.theme.AmberDeep
import com.monarchd.synth.ui.theme.Cream
import com.monarchd.synth.ui.theme.CreamDim
import com.monarchd.synth.ui.theme.CreamFaint
import com.monarchd.synth.ui.theme.Panel
import com.monarchd.synth.ui.theme.PanelEdge
import com.monarchd.synth.ui.theme.PanelFont
import com.monarchd.synth.ui.theme.PanelHigh
import com.monarchd.synth.ui.theme.PanelInset
import com.monarchd.synth.ui.theme.ReadoutFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchLibrarySheet(
    factory: List<Preset>,
    userPresets: List<Preset>,
    current: Patch,
    onLoad: (Preset) -> Unit,
    onSave: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRandomize: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf(current.name) }
    var justSaved by remember { mutableStateOf(false) }

    val all = remember(factory, userPresets) { factory + userPresets }
    val filtered = all.filter { preset ->
        val categoryOk = when (category) {
            null -> true
            "MINE" -> preset.isUser
            else -> preset.category.label == category
        }
        val queryOk = query.isBlank() ||
            preset.name.contains(query, ignoreCase = true) ||
            preset.description.contains(query, ignoreCase = true) ||
            preset.category.label.contains(query, ignoreCase = true)
        categoryOk && queryOk
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Panel,
        contentColor = Cream,
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.62f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = CreamFaint) }
    ) {
        Box(
            Modifier.fillMaxWidth().fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 18.dp)
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "PATCH LIBRARY",
                        color = Cream,
                        fontFamily = PanelFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        letterSpacing = 2.5.sp
                    )
                    Text(
                        text = "${all.size} PATCHES · TAP TO LOAD",
                        color = Amber,
                        fontFamily = PanelFont,
                        fontSize = 7.sp,
                        letterSpacing = 1.3.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = CreamDim,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            SearchField(query = query, onQueryChange = { query = it })
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                FilterChipSmall("ALL", category == null) { category = null }
                FilterChipSmall("MINE", category == "MINE") { category = "MINE" }
                PresetCategory.entries.forEach { cat ->
                    FilterChipSmall(cat.label, category == cat.label) { category = cat.label }
                }
            }
            Spacer(Modifier.height(10.dp))

            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(filtered, key = { preset -> "${preset.name}-${preset.category}-${preset.isUser}" }) { preset ->
                    PatchRow(
                        preset = preset,
                        selected = preset.name.equals(current.name, ignoreCase = true) &&
                            preset.category == current.category,
                        onLoad = { onLoad(preset) },
                        onDelete = if (preset.isUser) {
                            { onDelete(preset.name) }
                        } else {
                            null
                        }
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = "No patches found. Try another search, or save the sound you are playing.",
                            color = CreamFaint,
                            fontFamily = PanelFont,
                            fontSize = 11.sp,
                            letterSpacing = 0.3.sp,
                            modifier = Modifier.padding(vertical = 26.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                EngravedButton(
                    text = "SAVE CURRENT",
                    onClick = {
                        saveName = current.name
                        showSaveDialog = true
                        justSaved = false
                    },
                    modifier = Modifier.weight(1f),
                    accent = true
                )
                EngravedButton(
                    text = "SURPRISE ME",
                    onClick = onRandomize,
                    modifier = Modifier.weight(1f)
                )
            }
            if (justSaved) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "PATCH SAVED TO MY PATCHES",
                    color = AmberBright,
                    fontFamily = PanelFont,
                    fontSize = 7.5.sp,
                    letterSpacing = 1.2.sp
                )
            }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = PanelHigh,
            titleContentColor = Cream,
            textContentColor = CreamDim,
            title = {
                Text(
                    text = "SAVE PATCH",
                    fontFamily = PanelFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = 2.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Name this sound so it is waiting for you next time.",
                        fontFamily = PanelFont,
                        fontSize = 12.sp,
                        color = CreamDim
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it.take(40) },
                        singleLine = true,
                        label = { Text("PATCH NAME", fontFamily = PanelFont, fontSize = 9.sp, letterSpacing = 1.sp) },
                        textStyle = TextStyle(fontFamily = PanelFont, fontSize = 14.sp, color = Cream),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Amber,
                            unfocusedBorderColor = PanelEdge,
                            cursorColor = Amber,
                            focusedLabelColor = Amber,
                            unfocusedLabelColor = CreamFaint
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (saveName.isNotBlank()) {
                            onSave(saveName)
                            showSaveDialog = false
                            justSaved = true
                        }
                    }
                ) {
                    Text("SAVE", color = Amber, fontFamily = PanelFont, fontSize = 10.sp, letterSpacing = 1.5.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL", color = CreamDim, fontFamily = PanelFont, fontSize = 10.sp, letterSpacing = 1.5.sp)
                }
            }
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(
            color = Cream,
            fontFamily = PanelFont,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
        ),
        cursorBrush = SolidColor(Amber),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(PanelInset, RoundedCornerShape(4.dp))
                    .border(1.dp, PanelEdge.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = CreamFaint,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(7.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search patches",
                            color = CreamFaint,
                            fontFamily = PanelFont,
                            fontSize = 12.sp
                        )
                    }
                    inner()
                }
            }
        }
    )
}

@Composable
private fun FilterChipSmall(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(
                if (selected) Amber.copy(alpha = 0.18f) else PanelInset,
                RoundedCornerShape(3.dp)
            )
            .border(
                1.dp,
                if (selected) Amber.copy(alpha = 0.65f) else PanelEdge.copy(alpha = 0.45f),
                RoundedCornerShape(3.dp)
            )
            .clickable(onClick = onClick)
            .height(32.dp)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) AmberBright else CreamDim,
            fontFamily = PanelFont,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 7.5.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PatchRow(
    preset: Preset,
    selected: Boolean,
    onLoad: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) Amber.copy(alpha = 0.09f) else androidx.compose.ui.graphics.Color(0xFF111015),
                RoundedCornerShape(5.dp)
            )
            .border(
                1.dp,
                if (selected) Amber.copy(alpha = 0.55f) else PanelEdge.copy(alpha = 0.35f),
                RoundedCornerShape(5.dp)
            )
            .clickable(onClick = onLoad)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LedIndicator(selected, Modifier.size(5.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = preset.name.uppercase(),
                    color = if (selected) AmberBright else Cream,
                    fontFamily = PanelFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (preset.isUser) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "USER",
                        color = CreamFaint,
                        fontFamily = PanelFont,
                        fontSize = 6.5.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(Modifier.height(1.dp))
            Text(
                text = preset.description,
                color = CreamFaint,
                fontFamily = PanelFont,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .background(PanelInset, RoundedCornerShape(2.dp))
                .border(1.dp, PanelEdge.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = preset.category.label,
                color = AmberDeep,
                fontFamily = ReadoutFont,
                fontSize = 6.5.sp,
                letterSpacing = 0.5.sp
            )
        }
        if (onDelete != null) {
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete ${preset.name}",
                    tint = CreamFaint,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
