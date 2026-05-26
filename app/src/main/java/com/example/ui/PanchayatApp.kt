package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.PanchayatGreen
import com.example.ui.theme.PanchayatGreenLight
import com.example.ui.theme.PanchayatOrange
import com.example.ui.theme.PanchayatOrangeLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.viewmodel.Booking
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.Notice
import com.example.viewmodel.PanchayatViewModel
import com.example.viewmodel.Ticket
import kotlinx.coroutines.delay

enum class PanchayatTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Insight", Icons.Default.Dashboard, "dashboard_tab"),
    VOICE_TICKET("AI Voice", Icons.Default.Mic, "voice_tab"),
    SERVICES("Market", Icons.Default.Store, "services_tab"),
    RULEBOOK("Rulebook", Icons.Default.AutoStories, "rulebook_tab")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanchayatApp(
    viewModel: PanchayatViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(PanchayatTab.DASHBOARD) }
    var isAdminMode by remember { mutableStateOf(false) }

    // Observe flows
    val tickets by viewModel.tickets.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val notices by viewModel.notices.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val voiceIssueStatus by viewModel.voiceIssueStatus.collectAsState()
    val isProcessingAI by viewModel.isProcessingAI.collectAsState()

    // Screen State variables
    var showQuickIssueDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!isAdminMode) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimaryDark,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    PanchayatTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (currentTab == tab) PanchayatOrange else TextSecondaryDark
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    color = if (currentTab == tab) PanchayatOrange else TextSecondaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = DarkSurfaceElevated
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = isAdminMode,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState) width else -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> if (targetState) -width else width } + fadeOut()
                },
                label = "AdminResidentTransition"
            ) { adminViewActive ->
                if (adminViewActive) {
                    AdminDashboardScreen(
                        tickets = tickets,
                        onUpdateStatus = { id, status -> viewModel.updateTicketStatus(id, status) },
                        onExitAdmin = { isAdminMode = false }
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Brand Top Bar
                        PanchayatTopHeader(
                            isAdmin = false,
                            onToggleAdmin = { isAdminMode = true }
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            when (currentTab) {
                                PanchayatTab.DASHBOARD -> DashboardScreen(
                                    notices = notices,
                                    tickets = tickets,
                                    bookings = bookings,
                                    onNavigateToVoice = { currentTab = PanchayatTab.VOICE_TICKET },
                                    onQuickManualIssue = { showQuickIssueDialog = true }
                                )

                                PanchayatTab.VOICE_TICKET -> VoiceTicketScreen(
                                    isRecording = isRecording,
                                    voiceIssueStatus = voiceIssueStatus,
                                    isProcessingAI = isProcessingAI,
                                    onStartRecord = { viewModel.startRecording() },
                                    onStopRecord = { apt, overrideText -> 
                                        viewModel.stopRecordingAndProcess(apt, overrideText) 
                                    },
                                    onManualAnalyze = { text, apt -> 
                                        viewModel.processVoiceTicketWithAI(text, apt) 
                                    }
                                )

                                PanchayatTab.SERVICES -> ServicesMarketplaceScreen(
                                    bookings = bookings,
                                    onBookService = { servName, apt, dSlot, tSlot, inst ->
                                        viewModel.bookMarketplaceService(servName, apt, dSlot, tSlot, inst)
                                    }
                                )

                                PanchayatTab.RULEBOOK -> RulebookChatScreen(
                                    chatMessages = chatMessages,
                                    isProcessingAI = isProcessingAI,
                                    onSendMessage = { text -> viewModel.sendChatMessageToAI(text) }
                                )
                            }
                        }
                    }
                }
            }

            // Quick Manual Ticket Creation Popup Dialog
            if (showQuickIssueDialog) {
                ManualTicketDialog(
                    onDismiss = { showQuickIssueDialog = false },
                    onSubmit = { title, desc, cat, priority, apt ->
                        viewModel.addManualTicket(title, desc, cat, priority, apt)
                        showQuickIssueDialog = false
                    }
                )
            }
        }
    }
}

// --- Custom Top Header Bar ---
@Composable
fun PanchayatTopHeader(
    isAdmin: Boolean,
    onToggleAdmin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(PanchayatGreenLight)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Panchayat Premium Society",
                        fontSize = 12.sp,
                        color = PanchayatGreenLight,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isAdmin) "Admin Control Core" else "Digital Resident Portal",
                    fontSize = 20.sp,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Quick Admin Toggle pill
            Surface(
                onClick = onToggleAdmin,
                shape = RoundedCornerShape(12.dp),
                color = if (isAdmin) PanchayatOrange else DarkSurfaceElevated,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("admin_mode_toggle"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAdmin) Icons.Default.SwitchAccount else Icons.Default.AdminPanelSettings,
                        contentDescription = "Switch Console",
                        tint = if (isAdmin) Color.White else PanchayatOrangeLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAdmin) "Resident View" else "Admin Board",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAdmin) Color.White else PanchayatOrangeLight
                    )
                }
            }
        }
    }
}

// --- Screen 1: Resident Dashboard Screen ---
@Composable
fun DashboardScreen(
    notices: List<Notice>,
    tickets: List<Ticket>,
    bookings: List<Booking>,
    onNavigateToVoice: () -> Unit,
    onQuickManualIssue: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Stats Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Good Day Resident!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your maintenance dues are completely paid till June 2026 ✅",
                        fontSize = 12.sp,
                        color = PanchayatGreenLight
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    Divider(color = DarkSurfaceElevated, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickMetricItem(
                            title = "Active Issues",
                            value = tickets.count { it.status != "Resolved" }.toString(),
                            icon = Icons.Default.Warning,
                            iconColor = PanchayatOrangeLight
                        )
                        QuickMetricItem(
                            title = "Open Bookings",
                            value = bookings.count { it.status != "Completed" }.toString(),
                            icon = Icons.Default.CalendarToday,
                            iconColor = PanchayatGreenLight
                        )
                        QuickMetricItem(
                            title = "Flat Assigned",
                            value = "A - 304",
                            icon = Icons.Default.Home,
                            iconColor = Color.Magenta
                        )
                    }
                }
            }
        }

        // Action Quick Access Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToVoice,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("on_navigate_to_voice"),
                    colors = ButtonDefaults.buttonColors(containerColor = PanchayatOrange),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Voice Ticket", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = onQuickManualIssue,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("on_quick_manual_issue"),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = "Manual Ticket", tint = Color.LightGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Issue", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.LightGray)
                }
            }
        }

        // Active Panchayat Notices Segment
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Campaign, contentDescription = "Speaker Notice", tint = PanchayatOrangeLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Announcements & Notices",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        items(notices) { notice ->
            NoticeItemRow(notice = notice)
        }

        // Recent Private Tickets Segment
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = "My Tickets", tint = TextSecondaryDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Logged Complaints",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (tickets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "No tickets reported. Your flat is peaceful and clean!",
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(tickets) { ticket ->
                TicketItemCard(ticket = ticket, isAdmin = false)
            }
        }
    }
}

@Composable
fun QuickMetricItem(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text(text = title, fontSize = 10.sp, color = TextSecondaryDark)
    }
}

@Composable
fun NoticeItemRow(notice: Notice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notice.isUrgent) Color(0xFF451A03) else DarkSurface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notice.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (notice.isUrgent) PanchayatOrangeLight else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (notice.isUrgent) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PanchayatOrange)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CRITICAL",
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notice.description,
                fontSize = 12.sp,
                color = if (notice.isUrgent) Color(0xFFFFD8A8) else TextSecondaryDark,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = notice.date,
                    fontSize = 10.sp,
                    color = TextSecondaryDark
                )
            }
        }
    }
}

// --- Screen 2: AI Voice-to-Ticket Screen ---
@Composable
fun VoiceTicketScreen(
    isRecording: Boolean,
    voiceIssueStatus: String?,
    isProcessingAI: Boolean,
    onStartRecord: () -> Unit,
    onStopRecord: (apartment: String, overrideText: String?) -> Unit,
    onManualAnalyze: (text: String, apartment: String) -> Unit
) {
    var apartmentText by remember { mutableStateOf("A-304") }
    var userManualExplainText by remember { mutableStateOf("") }
    var activeTimerSec by remember { mutableStateOf(0) }

    // Simulated Timer for Recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            activeTimerSec = 0
            while (isRecording) {
                delay(1000)
                activeTimerSec++
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Voice Ticket Processor",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hold down the microphone to register visual or physical maintenance failures. Or type manual complaints to analyze using Gemini AI.",
                fontSize = 12.sp,
                color = TextSecondaryDark,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Text Field for Flat number
            OutlinedTextField(
                value = apartmentText,
                onValueChange = { apartmentText = it },
                label = { Text("Your flat number", color = TextSecondaryDark) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray,
                    focusedBorderColor = PanchayatOrange,
                    unfocusedBorderColor = DarkSurfaceElevated
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Live Audio Pulsating / Recording Center Core
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isRecording) {
                                listOf(PanchayatOrange, Color.Transparent)
                            } else {
                                listOf(DarkSurfaceElevated, Color.Transparent)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = {
                        if (isRecording) {
                            onStopRecord(apartmentText, null)
                        } else {
                            onStartRecord()
                        }
                    },
                    shape = CircleShape,
                    color = if (isRecording) PanchayatOrange else PanchayatOrange.copy(alpha = 0.82f),
                    modifier = Modifier
                        .size(90.dp)
                        .testTag("mic_record_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop Record" else "Start Record",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isRecording) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RECORDING: ${activeTimerSec / 60}:${String.format("%02d", activeTimerSec % 60)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
                Text(
                    text = "Release mic or tap red square to process",
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "Tap circle to record vocal voice",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Alternative input form / Output progress bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isProcessingAI) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = PanchayatOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Panchayat Gemini AI is working...",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI helper",
                            tint = PanchayatOrangeLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Or type issue description manually:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PanchayatOrangeLight
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = userManualExplainText,
                        onValueChange = { userManualExplainText = it },
                        placeholder = { Text("E.g. Broken sewage outlet in basement block B slot 14", color = TextSecondaryDark, fontSize = 12.sp) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PanchayatOrange,
                            unfocusedBorderColor = DarkSurfaceElevated
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (userManualExplainText.isNotBlank()) {
                                onManualAnalyze(userManualExplainText, apartmentText)
                                userManualExplainText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = userManualExplainText.isNotBlank()
                    ) {
                        Text("Extract & Categorize with Gemini AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Show status updates if any
                voiceIssueStatus?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = DarkSurfaceElevated)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        color = PanchayatGreenLight,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// --- Screen 3: AI Service Marketplace ---
data class LocalService(
    val title: String,
    val description: String,
    val basePrice: String,
    val icon: ImageVector,
    val bgGradient: List<Color>
)

@Composable
fun ServicesMarketplaceScreen(
    bookings: List<Booking>,
    onBookService: (serviceName: String, apartment: String, dateSlot: String, timeSlot: String, instructions: String) -> Unit
) {
    val itemsList = listOf(
        LocalService(
            "Plumbing Services",
            "Leak repair, faucet replace, water pipes flush",
            "₹250 Base Rate",
            Icons.Default.WaterDrop,
            listOf(Color(0xFF0284C7), Color(0xFF0369A1))
        ),
        LocalService(
            "Electrician Services",
            "Smart switch install, wiring checks, fan repair",
            "₹199 Base Rate",
            Icons.Default.FlashOn,
            listOf(Color(0xFFD97706), Color(0xFFB45309))
        ),
        LocalService(
            "Water Tanker Booking",
            "Emergency clean drinking water tanker supply",
            "₹1500 / Tanker",
            Icons.Default.LocalShipping,
            listOf(Color(0xFF059669), Color(0xFF047857))
        ),
        LocalService(
            "Waste / Debris Pickup",
            "Heavy packing boxes, furniture waste discarding",
            "₹300 Per Trip",
            Icons.Default.DeleteSweep,
            listOf(Color(0xFF4B5563), Color(0xFF374151))
        ),
        LocalService(
            "Pest Control Sweep",
            "Organic eco-friendly herbal pest/bug spray",
            "₹799 Complete Flat",
            Icons.Default.BugReport,
            listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))
        ),
        LocalService(
            "Security Escort",
            "Vocal gate escort, personal guest security setup",
            "₹400 / Escort",
            Icons.Default.Shield,
            listOf(Color(0xFFDC2626), Color(0xFFB91C1C))
        )
    )

    var selectedServiceForBooking by remember { mutableStateOf<LocalService?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Society Professional On-Demand Marketplace",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Official certified service specialists dispatched directly by society developers within 30 minutes.",
            fontSize = 11.sp,
            color = TextSecondaryDark,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Grid List of services
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(itemsList) { service ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable { selectedServiceForBooking = service },
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(service.bgGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = service.icon,
                                contentDescription = service.title,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = service.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = service.description,
                                fontSize = 10.sp,
                                color = TextSecondaryDark,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 12.sp
                            )
                        }

                        Text(
                            text = service.basePrice,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PanchayatOrangeLight
                        )
                    }
                }
            }
        }

        // Show horizontal scroll of prior bookings active states
        if (bookings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = DarkSurfaceElevated)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "My Scheduled Bookings",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bookings) { booking ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = booking.serviceName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Slot: ${booking.dateSlot} · ${booking.timeSlot}",
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PanchayatGreen.copy(alpha = 0.3f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${booking.status}: ${booking.assignedAgent}",
                                    fontSize = 9.sp,
                                    color = PanchayatGreenLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Booking Dialogue
    selectedServiceForBooking?.let { s ->
        BookingFormDialog(
            serviceName = s.title,
            onDismiss = { selectedServiceForBooking = null },
            onSubmit = { apt, dSlot, tSlot, inst ->
                onBookService(s.title, apt, dSlot, tSlot, inst)
                selectedServiceForBooking = null
            }
        )
    }
}

@Composable
fun BookingFormDialog(
    serviceName: String,
    onDismiss: () -> Unit,
    onSubmit: (apartment: String, dateSlot: String, timeSlot: String, instructions: String) -> Unit
) {
    var apartment by remember { mutableStateOf("A-304") }
    var dateSlot by remember { mutableStateOf("Tomorrow (May 27)") }
    var timeSlot by remember { mutableStateOf("10 AM - 12 PM") }
    var instructions by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Request $serviceName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = apartment,
                    onValueChange = { apartment = it },
                    label = { Text("Flat & Block", color = TextSecondaryDark) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PanchayatOrange,
                        unfocusedBorderColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateSlot,
                    onValueChange = { dateSlot = it },
                    label = { Text("Preffered Date", color = TextSecondaryDark) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PanchayatOrange,
                        unfocusedBorderColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = timeSlot,
                    onValueChange = { timeSlot = it },
                    label = { Text("Preferred Time Slot", color = TextSecondaryDark) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PanchayatOrange,
                        unfocusedBorderColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Specific instructions (Optional)", color = TextSecondaryDark) },
                    placeholder = { Text("E.g. Call before coming", color = TextSecondaryDark, fontSize = 12.sp) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PanchayatOrange,
                        unfocusedBorderColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            onSubmit(apartment, dateSlot, timeSlot, instructions)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PanchayatOrange),
                        modifier = Modifier.weight(1f).testTag("book_service_button")
                    ) {
                        Text("Book Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- Screen 4: AI Society Rulebook (RAG Chat) ---
@Composable
fun RulebookChatScreen(
    chatMessages: List<ChatMessage>,
    isProcessingAI: Boolean,
    onSendMessage: (String) -> Unit
) {
    var typedMessageText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val suggestionChips = listOf(
        "🤫 Quiet Hours?",
        "🐕 Pet Policies?",
        "♻️ Waste Disposal?",
        "🚗 Guest Parking?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Chat Header Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(PanchayatGreenLight)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Panchayat Guard AI Assistant",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Armed with Panchayat official rulebook context",
                        fontSize = 10.sp,
                        color = TextSecondaryDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Message Thread Scroller
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages) { message ->
                val isAI = message.sender == "AI"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isAI) Arrangement.Start else Arrangement.End
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAI) DarkSurface else PanchayatOrange
                        ),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isAI) 2.dp else 16.dp,
                            bottomEnd = if (isAI) 16.dp else 2.dp
                        ),
                        modifier = Modifier.widthIn(max = 290.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = message.text,
                                fontSize = 13.sp,
                                color = Color.White,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Pulsing loader of active text generation
            if (isProcessingAI) {
                item {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = PanchayatOrange,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Panchayat Guard is consulting the code...",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chips for pre-defined inquiries
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestionChips.forEach { chip ->
                Card(
                    modifier = Modifier
                        .clickable {
                            onSendMessage(chip.removeRange(0, 2).trim())
                        },
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        text = chip,
                        fontSize = 11.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat bottom input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = typedMessageText,
                onValueChange = { typedMessageText = it },
                placeholder = { Text("Ask about rules, pets, penalties, parking...", color = TextSecondaryDark, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PanchayatOrange,
                    unfocusedBorderColor = DarkSurfaceElevated
                ),
                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (typedMessageText.isNotBlank()) {
                        onSendMessage(typedMessageText)
                        typedMessageText = ""
                        focusManager.clearFocus()
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (typedMessageText.isNotBlank()) {
                        onSendMessage(typedMessageText)
                        typedMessageText = ""
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PanchayatOrange)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send text",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// --- Screen 5: Admin & Management Dashboard (Separate View) ---
@Composable
fun AdminDashboardScreen(
    tickets: List<Ticket>,
    onUpdateStatus: (String, String) -> Unit,
    onExitAdmin: () -> Unit
) {
    var filterCategory by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Core Top Header exit bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "Console",
                    tint = PanchayatOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Admin Dashboard",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            TextButton(
                onClick = onExitAdmin,
                colors = ButtonDefaults.textButtonColors(contentColor = PanchayatOrangeLight)
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Exit admin", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Exit Console", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live stats highlight cards in admin view
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("PENDING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark)
                    Text(
                        text = tickets.count { it.status == "Pending" }.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PanchayatOrangeLight
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ACTIVE DISPATCH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark)
                    Text(
                        text = tickets.count { it.status == "In Progress" }.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("RESOLVED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark)
                    Text(
                        text = tickets.count { it.status == "Resolved" }.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PanchayatGreenLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter chips bar
        Text(
            text = "Segment tickets by type:",
            fontSize = 12.sp,
            color = TextSecondaryDark,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Plumbing", "Electrical", "Security", "General").forEach { cat ->
                val isSelected = filterCategory == cat
                Card(
                    modifier = Modifier.clickable { filterCategory = cat },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PanchayatOrange else DarkSurface
                    )
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main live tickets scroller
        val filteredTickets = if (filterCategory == "All") tickets else tickets.filter { it.category == filterCategory }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredTickets.isEmpty()) {
                item {
                    Text(
                        text = "No tickets listed in this category.",
                        fontSize = 13.sp,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(filteredTickets) { ticket ->
                    TicketItemCard(
                        ticket = ticket,
                        isAdmin = true,
                        onUpdateAction = { id, nextStat -> onUpdateStatus(id, nextStat) }
                    )
                }
            }
        }
    }
}

// --- Specific Ticket Display card template ---
@Composable
fun TicketItemCard(
    ticket: Ticket,
    isAdmin: Boolean,
    onUpdateAction: ((String, String) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category & Priority Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (ticket.category) {
                            "Plumbing" -> Icons.Default.WaterDrop
                            "Electrical" -> Icons.Default.FlashOn
                            "Security" -> Icons.Default.Shield
                            else -> Icons.Default.Build
                        },
                        contentDescription = ticket.category,
                        tint = PanchayatOrangeLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ticket.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PanchayatOrangeLight
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Priority Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (ticket.priority) {
                                    "High" -> Color(0xFF7F1D1D)
                                    "Medium" -> Color(0xFF78350F)
                                    else -> Color(0xFF14532D)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = ticket.priority.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (ticket.priority) {
                                "High" -> Color(0xFFFECACA)
                                "Medium" -> Color(0xFFFEF3C7)
                                else -> Color(0xFFD1FAE5)
                            }
                        )
                    }

                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (ticket.status) {
                                    "Resolved" -> Color(0xFF14532D)
                                    "In Progress" -> Color(0xFF1E3A8A)
                                    else -> Color(0xFF334155)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = ticket.status.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (ticket.status) {
                                "Resolved" -> Color(0xFFD1FAE5)
                                "In Progress" -> Color(0xFFDBEAFE)
                                else -> Color(0xFFF1F5F9)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Title & description
            Text(
                text = ticket.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ticket.description,
                fontSize = 12.sp,
                color = TextSecondaryDark,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = DarkSurfaceElevated)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Apt: ${ticket.apartment} · ${ticket.date}",
                    fontSize = 10.sp,
                    color = TextSecondaryDark
                )

                if (ticket.isVoice) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsVoice,
                            contentDescription = "Voice prompt",
                            tint = PanchayatGreenLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI Voice Extracted",
                            fontSize = 9.sp,
                            color = PanchayatGreenLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Admin Command options row below the card
            if (isAdmin && onUpdateAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (ticket.status != "In Progress") {
                        Button(
                            onClick = { onUpdateAction(ticket.id, "In Progress") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            modifier = Modifier.weight(1f).height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Mark Dispatch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (ticket.status != "Resolved") {
                        Button(
                            onClick = { onUpdateAction(ticket.id, "Resolved") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF166534)),
                            modifier = Modifier.weight(1f).height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Mark Resolved", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- Popup Dialog to record/input issues ---
@Composable
fun ManualTicketDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, desc: String, category: String, priority: String, apartment: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Plumbing") }
    var priority by remember { mutableStateOf("Medium") }
    var apartment by remember { mutableStateOf("A-304") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Log Maintenance Complaint",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Short Title", color = TextSecondaryDark) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PanchayatOrange,
                        unfocusedBorderColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Fully describe the problem", color = TextSecondaryDark) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PanchayatOrange,
                        unfocusedBorderColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                OutlinedTextField(
                    value = apartment,
                    onValueChange = { apartment = it },
                    label = { Text("Flat & Block", color = TextSecondaryDark) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PanchayatOrange,
                        unfocusedBorderColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Quick Row Choice for Category
                Column {
                    Text("Category:", fontSize = 11.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Plumbing", "Electrical", "Security", "General").forEach { cat ->
                            Card(
                                modifier = Modifier.clickable { category = cat },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (category == cat) PanchayatOrange else DarkSurfaceElevated
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Quick Row Choice for Priority
                Column {
                    Text("Priority Level:", fontSize = 11.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Low", "Medium", "High").forEach { pr ->
                            Card(
                                modifier = Modifier.clickable { priority = pr },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (priority == pr) PanchayatOrange else DarkSurfaceElevated
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = pr,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank() && desc.isNotBlank()) {
                                onSubmit(title, desc, category, priority, apartment)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PanchayatOrange),
                        modifier = Modifier.weight(1f).testTag("submit_manual_ticket_button"),
                        enabled = title.isNotBlank() && desc.isNotBlank()
                    ) {
                        Text("Submit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
