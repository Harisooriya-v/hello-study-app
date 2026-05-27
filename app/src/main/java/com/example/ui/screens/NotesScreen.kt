package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.NoteItem
import com.example.ui.viewmodel.StudyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NotesScreen(viewModel: StudyViewModel) {
    val notes by viewModel.notes.collectAsState()
    var selectedCategoryFilter by remember { mutableStateOf("ALL") } // ALL, TEXT, PICTURE, AUDIO, VIDEO

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedNoteType by remember { mutableStateOf("TEXT") } // TEXT, PICTURE, AUDIO, VIDEO

    // Fields for adding notes
    var titleInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }
    var customMediaUri by remember { mutableStateOf("") }

    // Preloaded royalty free high resolution images for Picture Note Presets
    val picturePresets = listOf(
        "Study Laptop" to "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?q=80&w=600&auto=format&fit=crop",
        "Library Stacks" to "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?q=80&w=600&auto=format&fit=crop",
        "Math Formulas" to "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?q=80&w=600&auto=format&fit=crop",
        "Cozy Workspace" to "https://images.unsplash.com/photo-1507842217343-583bb7270b66?q=80&w=600&auto=format&fit=crop"
    )

    // Preloaded educational/study videos
    val videoPresets = listOf(
        "MIT Physics Seminar" to "https://www.w3schools.com/html/mov_bbb.mp4",
        "Biology 101 Lecture" to "https://www.w3schools.com/html/movie.mp4",
        "Linear Algebra Course" to "https://media.w3.org/2010/05/sintel/trailer_hd.mp4"
    )

    // Audio tape recording status
    var isSimulatingRecording by remember { mutableStateOf(false) }
    var recordedTimeSec by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isSimulatingRecording) {
        if (isSimulatingRecording) {
            recordedTimeSec = 0
            while (isSimulatingRecording) {
                delay(1000)
                recordedTimeSec++
            }
        }
    }

    val filteredNotes = remember(notes, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL") notes
        else notes.filter { it.noteType == selectedCategoryFilter }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notes_screen_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category scroll row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "TEXT", "PICTURE", "AUDIO", "VIDEO").forEach { category ->
                    val isActive = selectedCategoryFilter == category
                    val icon = when (category) {
                        "TEXT" -> Icons.Default.Description
                        "PICTURE" -> Icons.Default.Image
                        "AUDIO" -> Icons.Default.Mic
                        "VIDEO" -> Icons.Default.Videocam
                        else -> Icons.Default.FolderOpen
                    }

                    FilterChip(
                        selected = isActive,
                        onClick = { selectedCategoryFilter = category },
                        label = { Text(category, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        leadingIcon = { Icon(icon, contentDescription = category, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("notes_filter_chip_$category")
                    )
                }
            }

            // Notes Dynamic Grid Layout
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NoteAdd,
                            contentDescription = "No study notes",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No study notes found in this folder!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Tap the floating button below to capture ideas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            onDelete = { viewModel.deleteNote(note.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                titleInput = ""
                contentInput = ""
                customMediaUri = ""
                selectedNoteType = "TEXT"
                showAddDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_note_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add New Study Note")
        }

        // Note Creation Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { 
                    isSimulatingRecording = false
                    showAddDialog = false 
                },
                confirmButton = {
                    Button(
                        onClick = {
                            var uriToSave: String? = customMediaUri.trim()
                            if (uriToSave.isNullOrBlank()) {
                                uriToSave = when (selectedNoteType) {
                                    "PICTURE" -> picturePresets.first().second
                                    "VIDEO" -> videoPresets.first().second
                                    "AUDIO" -> "simulated_audio_memo_35s"
                                    else -> null
                                }
                            }
                            viewModel.addNote(titleInput, contentInput, selectedNoteType, uriToSave)
                            isSimulatingRecording = false
                            showAddDialog = false
                        },
                        modifier = Modifier.testTag("save_note_submit")
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        isSimulatingRecording = false
                        showAddDialog = false 
                    }) {
                        Text("Cancel")
                    }
                },
                title = {
                    Text(
                        "Create Learning Note",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Media Type Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("TEXT", "PICTURE", "AUDIO", "VIDEO").forEach { type ->
                                val active = selectedNoteType == type
                                OutlinedButton(
                                    onClick = { 
                                        selectedNoteType = type
                                        isSimulatingRecording = false
                                        customMediaUri = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                                ) {
                                    Text(type, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Title Textfield
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Note Title") },
                            placeholder = { Text("e.g. Chapter 1 Bio vocab") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Description Textfield
                        OutlinedTextField(
                            value = contentInput,
                            onValueChange = { contentInput = it },
                            label = { Text("Note Contents") },
                            placeholder = { Text("Write down key ideas or formulas...") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        )

                        // Special Multimedia Context Sections
                        when (selectedNoteType) {
                            "PICTURE" -> {
                                Text(
                                    "Tap to snap study mockup photo:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    picturePresets.forEach { preset ->
                                        val name = preset.first
                                        val url = preset.second
                                        val isChosen = customMediaUri == url
                                        Card(
                                            modifier = Modifier
                                                .width(130.dp)
                                                .height(90.dp)
                                                .border(
                                                    width = if (isChosen) 2.5.dp else 0.dp,
                                                    color = if (isChosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { customMediaUri = url },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                AsyncImage(
                                                    model = url,
                                                    contentDescription = name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.Black.copy(alpha = 0.5f))
                                                        .align(Alignment.BottomCenter)
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        name,
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "VIDEO" -> {
                                Text(
                                    "Select interactive educational seminar:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    videoPresets.forEach { (name, url) ->
                                        val isChosen = customMediaUri == url
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                )
                                                .border(
                                                    width = if (isChosen) 1.5.dp else 0.dp,
                                                    color = if (isChosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { customMediaUri = url }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.PlayCircle, 
                                                contentDescription = name, 
                                                tint = if (isChosen) MaterialTheme.colorScheme.primary else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            "AUDIO" -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (isSimulatingRecording) "Recording Voice Memorandum..." else "Vocal Recorder",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Tape recorder wave animations
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(30.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSimulatingRecording) {
                                                // Live jumping bars simulation
                                                repeat(12) { index ->
                                                    val animT = remember { androidx.compose.animation.core.Animatable(5f) }
                                                    LaunchedEffect(isSimulatingRecording) {
                                                        while (isSimulatingRecording) {
                                                            animT.animateTo(
                                                                targetValue = (12..30).random().toFloat(),
                                                                animationSpec = androidx.compose.animation.core.tween(
                                                                    durationMillis = (100..200).random()
                                                                )
                                                            )
                                                        }
                                                    }
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier
                                                            .padding(horizontal = 2.dp)
                                                            .width(3.dp)
                                                            .height(animT.value.dp)
                                                            .clip(CircleShape)
                                                    ) {}
                                                }
                                            } else {
                                                Text("Tape Deck Offline (Press Record)", fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        OutlinedButton(
                                            onClick = { isSimulatingRecording = !isSimulatingRecording },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isSimulatingRecording) Color.Red.copy(alpha = 0.15f) else Color.Transparent,
                                                contentColor = if (isSimulatingRecording) Color.Red else MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = if (isSimulatingRecording) Icons.Default.Square else Icons.Default.Mic,
                                                contentDescription = "Mic"
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isSimulatingRecording) "Stop (${recordedTimeSec}s)" else "Simulate Record",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun NoteCardItem(
    note: NoteItem,
    onDelete: () -> Unit
) {
    var isAudioPlaying by remember { mutableStateOf(false) }
    var currentSeekPercent by remember { mutableStateOf(0.0f) }
    val scope = rememberCoroutineScope()

    // Sound bar progress incrementor
    LaunchedEffect(isAudioPlaying) {
        if (isAudioPlaying) {
            while (currentSeekPercent < 1.0f && isAudioPlaying) {
                delay(100)
                currentSeekPercent += 0.02f
            }
            if (currentSeekPercent >= 1.0f) {
                isAudioPlaying = false
                currentSeekPercent = 0.0f
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Media Banner based on noteType
            when (note.noteType) {
                "PICTURE" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        AsyncImage(
                            model = note.mediaUri,
                            contentDescription = "Note Picture Content",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Image, contentDescription = "Pic", tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PIC", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                "VIDEO" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color.Black)
                    ) {
                        Icon(
                            Icons.Default.PlayCircleFilled,
                            contentDescription = "Video Play",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Red.copy(alpha = 0.85f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Videocam, contentDescription = "Video", tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LECTURE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                "AUDIO" -> {
                    // Audio Deck simulator banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { isAudioPlaying = !isAudioPlaying },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play memo"
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Memo Voice_001.amr", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                LinearProgressIndicator(
                                    progress = { currentSeekPercent },
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // Note Text Info
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("delete_note_button_${note.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete Note", 
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Timestamp ticker
                val timestampForm = remember(note.timestamp) {
                    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(note.timestamp))
                }
                Text(
                    text = "Saved $timestampForm",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
