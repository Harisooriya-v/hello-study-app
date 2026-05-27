package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScribbleItem
import com.example.ui.viewmodel.DrawPoint
import com.example.ui.viewmodel.DrawStroke
import com.example.ui.viewmodel.StudyViewModel

// Safe helper to convert hex string safely to Compose Color
fun safeParseColor(colorHex: String, fallback: Color = Color.Cyan): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        fallback
    }
}

@Composable
fun ScribbleScreen(viewModel: StudyViewModel) {
    val strokes by viewModel.activeStrokes.collectAsState()
    val savedScribbles by viewModel.scribbles.collectAsState()
    val currentBrushColorHex by viewModel.currentBrushColorHex.collectAsState()
    val currentBrushWidth by viewModel.currentBrushWidth.collectAsState()
    val isDarkTheme by viewModel.isDarkMode.collectAsState()

    var sketchTitle by remember { mutableStateOf("Physics Diagram") }
    var eraserMode by remember { mutableStateOf(false) }

    // Palette setup
    val neonDarkPalette = listOf(
        "#FF00E5FF", // Neon Cyan
        "#FF00E676", // Neon Green
        "#FFFFAB00", // Bright Amber
        "#FFFF007F", // Neon Hot Pink
        "#FF9D4EDD", // Royal Purple
        "#FFFFFFFF"  // White
    )

    val softLightPalette = listOf(
        "#FF00838F", // Dark Teal
        "#FF2E7D32", // Forest Green
        "#FFF57C00", // Dark Orange
        "#FFC2185B", // Deep Pink
        "#FF4A148C", // Royal Purple
        "#00000000"  // Black (resolved below)
    )

    val currentPalette = if (isDarkTheme) neonDarkPalette else softLightPalette.map { 
        if (it == "00000000") "#FF263238" else it 
    }

    // Background color of canvas
    val canvasBg = if (isDarkTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else Color.White
    val canvasBorderColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("scribble_screen_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title and Save panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = sketchTitle,
                onValueChange = { sketchTitle = it },
                label = { Text("Sketch Title") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("sketch_title_input"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 1,
                trailingIcon = {
                    if (sketchTitle.isNotEmpty()) {
                        IconButton(onClick = { sketchTitle = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Title")
                        }
                    }
                }
            )

            Button(
                onClick = {
                    viewModel.saveScribble(sketchTitle)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.testTag("save_sketch_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }

        // Saved Sketches Row (History)
        if (savedScribbles.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Saved Sketches (${savedScribbles.size})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedScribbles, key = { it.id }) { item ->
                        InputChip(
                            selected = false,
                            onClick = {
                                viewModel.loadScribble(item)
                                sketchTitle = item.title
                            },
                            label = { 
                                Text(
                                    item.title, 
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Delete",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.deleteScribble(item.id) },
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("saved_sketch_chip_${item.id}")
                        )
                    }
                }
            }
        }

        // Draw Canvas Board
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(canvasBg)
                .border(2.dp, canvasBorderColor, RoundedCornerShape(20.dp))
                .pointerInput(currentBrushColorHex, currentBrushWidth, eraserMode) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val colorToUse = if (eraserMode) {
                                if (isDarkTheme) "#FF131A30" else "#FFFFFFFF" // matches surface bg exactly
                            } else {
                                currentBrushColorHex
                            }
                            val widthToUse = if (eraserMode) 32f else currentBrushWidth

                            val stroke = DrawStroke(
                                points = listOf(DrawPoint(startOffset.x, startOffset.y)),
                                colorHex = colorToUse,
                                width = widthToUse
                            )
                            viewModel.activeStrokes.value += stroke
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val active = viewModel.activeStrokes.value
                            if (active.isNotEmpty()) {
                                val last = active.last()
                                val updatedPoints = last.points + DrawPoint(change.position.x, change.position.y)
                                val updatedLast = last.copy(points = updatedPoints)
                                viewModel.activeStrokes.value = active.dropLast(1) + updatedLast
                            }
                        }
                    )
                }
                .testTag("scribble_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                strokes.forEach { stroke ->
                    if (stroke.points.isNotEmpty()) {
                        val path = Path()
                        path.moveTo(stroke.points.first().x, stroke.points.first().y)
                        for (i in 1 until stroke.points.size) {
                            val pt = stroke.points[i]
                            path.lineTo(pt.x, pt.y)
                        }
                        drawPath(
                            path = path,
                            color = safeParseColor(stroke.colorHex),
                            style = Stroke(
                                width = stroke.width,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            // Small prompt overlay when blank
            if (strokes.isEmpty()) {
                Text(
                    text = "Scribble down your thoughts here with touch...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }

        // Drawing Controls panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color selection list
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        currentPalette.forEach { hex ->
                            val selected = currentBrushColorHex == hex && !eraserMode
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(safeParseColor(hex))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        eraserMode = false
                                        viewModel.currentBrushColorHex.value = hex
                                    }
                            )
                        }
                    }

                    // Brush / Eraser triggers
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drawing Pen
                        IconButton(
                            onClick = { eraserMode = false },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (!eraserMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (!eraserMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Gesture, contentDescription = "Pen Mode")
                        }

                        // Eraser
                        IconButton(
                            onClick = { eraserMode = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (eraserMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (eraserMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.size(36.dp).testTag("eraser_button")
                        ) {
                            Icon(Icons.Default.FormatPaint, contentDescription = "Eraser Mode")
                        }

                        // Clear board
                        IconButton(
                            onClick = { viewModel.clearScribbleCanvas() },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.size(36.dp).testTag("clear_canvas_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Canvas")
                        }
                    }
                }

                // Width slide controller
                if (!eraserMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LineWeight, 
                            contentDescription = "Brush stroke width", 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = currentBrushWidth,
                            onValueChange = { viewModel.currentBrushWidth.value = it },
                            valueRange = 2f..32f,
                            modifier = Modifier.weight(1f).testTag("brush_width_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${currentBrushWidth.toInt()}px",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(32.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FormatPaint, 
                            contentDescription = "Eraser size", 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Eraser Mode Active (Fixed wide 32px point)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
