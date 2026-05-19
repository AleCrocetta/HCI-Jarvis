package com.example.calendarapp.ui.addevent

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendarapp.ui.theme.DarkBlue
import com.example.calendarapp.ui.theme.LightBlueBg
import com.example.calendarapp.ui.theme.LightGrayBg
import com.example.calendarapp.ui.theme.TextGray
import com.example.calendarapp.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    selectedDay: Int,
    selectedMonth: String,
    onSaveEvent: (title: String, time: String, chosenDay: Int, link: String, fileNames: List<String>) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var chosenDay by remember { mutableIntStateOf(selectedDay) }
    var date by remember { mutableStateOf("$selectedMonth $selectedDay, 2025") }
    var time by remember { mutableStateOf("10:00 AM - 11:00 AM") }
    var link by remember { mutableStateOf("") }
    var fileNames by remember { mutableStateOf<List<String>>(emptyList()) }
    
    var isTitleError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showMockFilePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Native Multiple Files Storage Picker!
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val names = uris.map { uri -> getFileNameFromUri(context, uri) }
        fileNames = fileNames + names
    }

    // Sync date display when chosenDay changes
    LaunchedEffect(chosenDay, selectedMonth) {
        date = "$selectedMonth $chosenDay, 2025"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Event", color = DarkBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        },
        containerColor = White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        if (it.isNotBlank()) isTitleError = false
                    },
                    label = { Text("Event Title", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isTitleError,
                    supportingText = {
                        if (isTitleError) {
                            Text("Title cannot be empty", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = TextGray,
                        focusedContainerColor = LightGrayBg,
                        unfocusedContainerColor = LightGrayBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Interactive Date Field triggering visual picker dialog!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date", color = TextGray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkBlue,
                            unfocusedBorderColor = TextGray,
                            focusedContainerColor = LightGrayBg,
                            unfocusedContainerColor = LightGrayBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Interactive Time Field triggering list slot picker dialog!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showTimePicker = true }
                ) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Time", color = TextGray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkBlue,
                            unfocusedBorderColor = TextGray,
                            focusedContainerColor = LightGrayBg,
                            unfocusedContainerColor = LightGrayBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Modern URL Link input field
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Attachment Link (Optional)", color = TextGray) },
                    placeholder = { Text("https://example.com", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = TextGray,
                        focusedContainerColor = LightGrayBg,
                        unfocusedContainerColor = LightGrayBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Modern Upload Files Section (Multiple storage selection!)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LightGrayBg)
                        .border(1.dp, TextGray.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ATTACHED FILES", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (fileNames.isEmpty()) "No files attached" else "${fileNames.size} files selected",
                                color = DarkBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { 
                                try {
                                    filePickerLauncher.launch("*/*") 
                                } catch (e: Exception) {
                                    // If emulator has NO file manager app installed, open built-in directory simulator!
                                    showMockFilePicker = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Upload Files", color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (fileNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = TextGray.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        fileNames.forEachIndexed { index, fileName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("📄", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = fileName,
                                        color = DarkBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red.copy(alpha = 0.1f))
                                        .clickable {
                                            fileNames = fileNames.filterIndexed { i, _ -> i != index }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("❌", fontSize = 8.sp, color = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (title.isBlank()) {
                        isTitleError = true
                    } else {
                        onSaveEvent(title, time, chosenDay, link, fileNames)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Event", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Graphical Mock Storage Directory Selector Dialog
    if (showMockFilePicker) {
        AlertDialog(
            onDismissRequest = { showMockFilePicker = false },
            title = { Text("Local Storage Simulator", color = DarkBlue, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Your emulator lacks a default Files application. Pick documents from this simulated directory:", color = TextGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    val mockFiles = listOf(
                        "BoardingPass.pdf",
                        "MeetingAgenda.pdf",
                        "ProjectReport.docx",
                        "SprintBacklog.xlsx",
                        "Presentation.pptx"
                    )
                    mockFiles.forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (fileNames.contains(file)) {
                                        fileNames = fileNames.filter { it != file }
                                    } else {
                                        fileNames = fileNames + file
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(file, color = DarkBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            if (fileNames.contains(file)) {
                                Text("✅", fontSize = 14.sp)
                            }
                        }
                        Divider(color = TextGray.copy(alpha = 0.1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showMockFilePicker = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done", color = White)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Graphical Date Selection Dialog (Calendar Grid style!)
    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("Select Day", color = DarkBlue, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select a day in $selectedMonth 2025:", color = TextGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    val days = (1..28).toList()
                    val chunkedDays = days.chunked(7)
                    Column {
                        chunkedDays.forEach { rowDays ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowDays.forEach { dayNum ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (chosenDay == dayNum) DarkBlue else Color.Transparent)
                                            .clickable {
                                                chosenDay = dayNum
                                                showDatePicker = false
                                            }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            color = if (chosenDay == dayNum) White else DarkBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Time Slot Selection Dialog (Vertical Scrollable list!)
    if (showTimePicker) {
        val timesList = listOf(
            "06:30 AM - 07:00 AM",
            "08:00 AM - 09:20 AM",
            "10:00 AM - 10:30 AM",
            "11:00 AM - 12:00 PM",
            "01:30 PM - 02:30 PM",
            "03:00 PM - 04:30 PM",
            "06:00 PM - 07:00 PM",
            "08:00 PM - 09:00 PM"
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time Slot", color = DarkBlue, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .height(240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    timesList.forEach { timeSlot ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (time == timeSlot) LightBlueBg else LightGrayBg)
                                .clickable {
                                    time = timeSlot
                                    showTimePicker = false
                                }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = timeSlot,
                                color = DarkBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// Extract name from local file Uri
fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    try {
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
    } catch (e: Exception) {
        // Safe query protection
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "unnamed_file"
}
