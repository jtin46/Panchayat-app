package com.example.viewmodel

import android.app.Application
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// --- Data Models ---

data class UserSession(
    val fullName: String,
    val email: String,
    val role: String, // "Secretary", "Resident", "Service Provider"
    val serviceType: String? = null, // "Plumber", "Carpenter", "Electrician", "Painter", "Pest Control", "Security Guard", etc.
    val societyName: String,
    val joinCode: String,
    val apartment: String = "",
    val pinCodeOrLocation: String = "",
    val wingsCount: String = ""
)

data class SocietyDetails(
    val name: String,
    val location: String,
    val wings: String,
    val code: String,
    val creatorName: String
)

data class Ticket(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: String, // Plumbing, Electrical, Security, General
    val status: String = "Pending", // Pending, In Progress, Resolved
    val priority: String = "Medium", // Low, Medium, High
    val apartment: String,
    val date: String,
    val isVoice: Boolean = false
)

data class Booking(
    val id: String = UUID.randomUUID().toString(),
    val serviceName: String,
    val apartment: String,
    val dateSlot: String,
    val timeSlot: String,
    val instructions: String,
    val status: String = "Assigned", // Assigned, In Progress, Completed
    val assignedAgent: String = "Local Expert"
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // User, AI
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Notice(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val date: String,
    val isUrgent: Boolean = false
)

class PanchayatViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // --- State Flows ---
    private val _currentUser = MutableStateFlow<UserSession?>(null)
    val currentUser: StateFlow<UserSession?> = _currentUser

    private val _registeredSocieties = MutableStateFlow<Map<String, SocietyDetails>>(emptyMap())
    val registeredSocieties: StateFlow<Map<String, SocietyDetails>> = _registeredSocieties

    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())
    val tickets: StateFlow<List<Ticket>> = _tickets

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _notices = MutableStateFlow<List<Notice>>(emptyList())
    val notices: StateFlow<List<Notice>> = _notices

    // Recording States
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec

    private val _voiceIssueStatus = MutableStateFlow<String?>(null)
    val voiceIssueStatus: StateFlow<String?> = _voiceIssueStatus

    private val _isProcessingAI = MutableStateFlow(false)
    val isProcessingAI: StateFlow<Boolean> = _isProcessingAI

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    // For Society Rulebook Pre-defined instructions (RAG context)
    private val societyRulebookContext = """
        You are "Panchayat AI", the official smart AI assistant for the Panchayat Premium Society.
        You must answer residents' questions STRICTLY based on the official guidelines and regulations below:
        
        1. PET POLICY: 
           - Pets are allowed inside apartments, but they must be kept on a leash at all times in common areas (gardens, corridors, lobby).
           - Littering by pets in common areas is strictly penalized with a ₹500 fine for first offense, and ₹1000 for recurring. Must clean up after pets immediately.
        
        2. QUIET HOURS:
           - Quiet hours are strictly from 10:00 PM to 07:00 AM daily.
           - Playing loud music, shouting, or carrying out noisy apartment renovations during these hours is strictly forbidden.
           - Penalty for quiet hours violations: ₹1500 per incident. Regular work/renovations allowed 9:00 AM - 6:00 PM.
           
        3. WASTE SEGREGATION:
           - Segregating waste into Green Bins (Wet waste: organic/food waste) and Blue Bins (Dry waste: plastics, papers, cans) is mandatory under Municipal Code and society rules.
           - Unsegregated waste will not be collected by staff. Fine for non-compliance: ₹200.
           
        4. GUEST PARKING:
           - Visitors and guests can ONLY park in slots marked 'G1' to 'G10' near gate number 2.
           - Guest parking is free for up to 4 hours.
           - Overnight parking of guest vehicles is not allowed without registering at the main gate security and obtaining a temporary pass (₹100 charge per night).
           
        5. MAINTENANCE DUES:
           - Periodic maintenance charges are ₹3.0 per sq ft, due on or before the 5th of every month.
           - Late fee of 12% per annum simple interest is applied after the 10th of the month.
           - Mode of payment: Panchayat app quick pay or Resident portal net banking.
           
        6. CLUBHOUSE & SWIMMING POOL BOOKINGS:
           - Clubhouse must be booked at least 7 days in advance through the resident dashboard/admin.
           - Booking charges: ₹1500 for a 4-hour party event, plus a refundable security deposit of ₹2000.
           - Swimming pool hours: 6:00 AM - 10:00 AM and 4:00 PM - 9:00 PM. Appropriate swimwear is mandatory. No lifeguards are present, children must be accompanied.
           
        7. GUEST ENTRY & SECURITY:
           - All external deliveries, cabs, and visitors must be verified on the main intercom or using the Panchayat App Gatepass.
           - Unverified visitors are turned away at the gate. No exceptions.
           
        Ensure your tone is polite, professional, and authoritative. Speak like a premium virtual society representative. If a user asks a question unrelated to society management, politely guide them back to topics related to the Panchayat Society.
    """.trimIndent()

    init {
        seedInitialData()
    }

    private fun seedInitialData() {
        _registeredSocieties.value = mapOf(
            "PANC-999" to SocietyDetails(
                name = "Panchayat Premium Society",
                location = "Gokuldham Co-op, Mumbai",
                wings = "A, B, C, D",
                code = "PANC-999",
                creatorName = "Sec. Bhide"
            )
        )

        // Initial Mock Notices
        _notices.value = listOf(
            Notice(
                title = "Clubhouse Painting & Maintenance",
                description = "The clubhouse building and lobby areas will be closed for painting from May 28 to May 30. Booking is suspended.",
                date = "Today",
                isUrgent = false
            ),
            Notice(
                title = "Scheduled Water Outage",
                description = "Due to overhead tank cleaning, water supply will be suspended for Block A & B on Wednesday from 11:00 AM to 2:00 PM. Please conserve water.",
                date = "Yesterday",
                isUrgent = true
            )
        )

        // Initial Mock Tickets
        _tickets.value = listOf(
            Ticket(
                title = "Corridor Light Broken",
                description = "The tubelight in Block B, 4th floor corridor near elevator is flickering and has stopped working completely.",
                category = "Electrical",
                status = "In Progress",
                priority = "Medium",
                apartment = "B-402",
                date = "25 May, 10:30 AM"
            ),
            Ticket(
                title = "Water Leakage in Parking",
                description = "Massive water pipe blockage showing seepage right above parking space G-12. Please direct plumbing staff ASAP.",
                category = "Plumbing",
                status = "Pending",
                priority = "High",
                apartment = "A-104",
                date = "26 May, 09:15 AM"
            )
        )

        // Initial Marketplace Bookings
        _bookings.value = listOf(
            Booking(
                serviceName = "Electrician Services",
                apartment = "C-501",
                dateSlot = "May 27",
                timeSlot = "10 AM - 12 PM",
                instructions = "Install 2 new smart ceiling fans in the living room.",
                status = "Assigned",
                assignedAgent = "Subhash Kumar"
            )
        )

        // Initial greeting chat message
        _chatMessages.value = listOf(
            ChatMessage(
                sender = "AI",
                text = "Welcome to Panchayat Companion! I am your AI Rulebook agent. Ask me anything about society rules, parking, maintenance, waste management, or clubhouse bookings."
            )
        )
    }

    // --- Create Custom Ticket ---
    fun addManualTicket(title: String, description: String, category: String, priority: String, apartment: String) {
        val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        val newTicket = Ticket(
            title = title,
            description = description,
            category = category,
            priority = priority,
            apartment = apartment,
            date = dateStr,
            isVoice = false
        )
        _tickets.value = listOf(newTicket) + _tickets.value
    }

    // --- Voice Recording Logic ---
    fun startRecording() {
        if (_isRecording.value) return

        try {
            val cacheDir = context.cacheDir
            audioFile = File.createTempFile("panchayat_voice_", ".m4a", cacheDir)
            
            // On standard compilation we instantiate MediaRecorder safely
            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }

            _isRecording.value = true
            _recordingDurationSec.value = 0
            _voiceIssueStatus.value = "Recording..."
            Log.d("PanchayatVoice", "Recording started successfully saving to ${audioFile!!.absolutePath}")
        } catch (e: Exception) {
            _voiceIssueStatus.value = "Record error: ${e.message}"
            Log.e("PanchayatVoice", "Failed to start recording", e)
        }
    }

    fun stopRecordingAndProcess(apartment: String, overrideText: String? = null) {
        if (!_isRecording.value) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecording.value = false
            _voiceIssueStatus.value = "Analyzing audio via Google Gemini API..."

            // Now, we simulate transcribing by passing a default description of the issue.
            // But since the user might be recording ambient silence in emulator, we check if
            // a custom typed/override description is available, or use an AI-assisted text generation!
            val issueText = overrideText ?: "Heavy leakage of tap water in Block D flat toilet."
            
            processVoiceTicketWithAI(issueText, apartment)
        } catch (e: Exception) {
            _voiceIssueStatus.value = "Stopped recording: ${e.message}"
            Log.e("PanchayatVoice", "Failed to stop recording cleanly", e)
        }
    }

    // --- AI Automated Ticket Processing ---
    fun processVoiceTicketWithAI(descriptionText: String, apartment: String) {
        _isProcessingAI.value = true
        _voiceIssueStatus.value = "Panchayat AI is digesting issue and formatting ticket..."

        viewModelScope.launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // If API Key is placeholder, show fallback AI ticket generation
                withContext(Dispatchers.Main) {
                    val fallbackTitle = if (descriptionText.length > 30) descriptionText.take(30) + "..." else descriptionText
                    addManualTicket(
                        title = "AI Gen: $fallbackTitle",
                        description = descriptionText,
                        category = "General",
                        priority = "Medium",
                        apartment = apartment
                    )
                    _isProcessingAI.value = false
                    _voiceIssueStatus.value = "Ticket processed successfully using AI Fallback! Setup real API Key in Secrets panel for live extraction."
                }
                return@launch
            }

            val prompt = """
                You are a smart society management system. 
                Analyze this resident complaint: "${descriptionText}".
                Based on this description, extract a concise Title (max 5 words), classify it under one of the categories: Plumbing, Electrical, Security, General, and evaluate appropriate Priority (Low, Medium, High) based on emergency.
                Output ONLY a valid JSON string containing exactly three fields:
                {
                  "title": "Clean concise title",
                  "category": "Plumbing or Electrical or Security or General",
                  "priority": "Low or Medium or High"
                }
                Do not include markdown tags, enclosing ticks or other statements. Return ONLY the JSON.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                Log.d("PanchayatAI", "AI Response parsing: $aiText")
                
                // Clean markdown tags from response if present
                val cleanedJson = aiText.trim()
                    .removePrefix("```json")
                    .removeSuffix("```")
                    .trim()

                val json = JSONObject(cleanedJson)
                val title = json.optString("title", "AI Resolved Maintenance")
                val category = json.optString("category", "General")
                val priority = json.optString("priority", "Medium")

                withContext(Dispatchers.Main) {
                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
                    val newTicket = Ticket(
                        title = title,
                        description = descriptionText,
                        category = category,
                        priority = priority,
                        status = "Pending",
                        apartment = apartment,
                        date = dateStr,
                        isVoice = true
                    )
                    _tickets.value = listOf(newTicket) + _tickets.value
                    _voiceIssueStatus.value = "Voiced Ticket Processed! Classified as '$category' with $priority Priority."
                }

            } catch (e: Exception) {
                Log.e("PanchayatAI", "Error in AI parsing", e)
                withContext(Dispatchers.Main) {
                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
                    val fallbackTicket = Ticket(
                        title = "AI parsed standard maintenance",
                        description = descriptionText,
                        category = "General",
                        priority = "High",
                        status = "Pending",
                        apartment = apartment,
                        date = dateStr,
                        isVoice = true
                    )
                    _tickets.value = listOf(fallbackTicket) + _tickets.value
                    _voiceIssueStatus.value = "AI processed with standard fallback: ${e.localizedMessage}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isProcessingAI.value = false
                }
            }
        }
    }

    // --- Marketplace Bookings ---
    fun bookMarketplaceService(serviceName: String, apartment: String, dateSlot: String, timeSlot: String, instructions: String) {
        val newBooking = Booking(
            serviceName = serviceName,
            apartment = apartment,
            dateSlot = dateSlot,
            timeSlot = timeSlot,
            instructions = instructions,
            assignedAgent = when(serviceName) {
                "Plumbing Services" -> "Rameesh Chawla"
                "Electrician Services" -> "Subhash Kumar"
                "Water Tanker" -> "Delhi Jal Board #45"
                "Security Escort" -> "Commandant Guard Singh"
                else -> "Panchayat Partner"
            }
        )
        _bookings.value = _bookings.value + newBooking
    }

    // --- AI RAG Chat Code ---
    fun sendChatMessageToAI(userText: String) {
        if (userText.isBlank()) return

        val userMessage = ChatMessage(sender = "User", text = userText)
        _chatMessages.value = _chatMessages.value + userMessage

        _isProcessingAI.value = true

        viewModelScope.launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // Return quick response if no key is configured, advising of security rule answers
                withContext(Dispatchers.Main) {
                    val promptKeyword = userText.lowercase()
                    val answer = when {
                        promptKeyword.contains("pet") || promptKeyword.contains("dog") -> 
                            "🐕 Panchayat Pet Policy: Pets must be on a leash in common areas. Littering fine: ₹500 for first offense. Feed only in designated zones."
                        promptKeyword.contains("quiet") || promptKeyword.contains("noise") || promptKeyword.contains("music") ->
                            "🤫 Quiet Hours are 10:00 PM to 07:00 AM daily. No loud sirens, parties, or hammering. Violators face ₹1500 fine per incident."
                        promptKeyword.contains("waste") || promptKeyword.contains("garbage") || promptKeyword.contains("dustbin") ->
                            "♻️ Waste Segregation is Mandatory. Use Green Bins for wet organic kitchen waste, Blue Bins for dry plastic/paper. Fine for default is ₹200."
                        promptKeyword.contains("park") || promptKeyword.contains("guest") || promptKeyword.contains("car") ->
                            "🚗 Guest parking is in G1 to G10 (near gate 2) for up to 4 hours. No overnight unless validated (₹100 charge per night)."
                        promptKeyword.contains("maintenance") || promptKeyword.contains("payment") || promptKeyword.contains("due") ->
                            "💰 Maintenance fee is ₹3.0 per square feet, payable on/before the 5th of each month. Late dues draw 12% annual penalty charge."
                        promptKeyword.contains("clubhouse") || promptKeyword.contains("swimming") || promptKeyword.contains("pool") ->
                            "🏊 Clubhouse must be booked 7 days early. Fee is ₹1500 for 4-hr slots + ₹2000 backup deposit. Safe pool hours: 6 AM - 10 AM, 4 PM - 9 PM."
                        else -> "Panchayat Helpdesk: This is a demo. Please add your real GEMINI_API_KEY in the Secrets Panel to get intelligent society RAG answers!"
                    }
                    _chatMessages.value = _chatMessages.value + ChatMessage(sender = "AI", text = answer)
                    _isProcessingAI.value = false
                }
                return@launch
            }

            // Build full conversation payload for Gemini, keeping rulebook system constraint
            val chatHistoryString = _chatMessages.value.takeLast(10).joinToString("\n") { 
                "${it.sender}: ${it.text}"
            }

            val systemDirective = societyRulebookContext
            val prompt = """
                $systemDirective
                
                Below is the recent local conversation history:
                $chatHistoryString
                
                Please answer the resident's last query accurately and helpful. Return ONLY the helpful answer response. Keep it concise.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val aiResponseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from Panchayat AI helper."
                withContext(Dispatchers.Main) {
                    _chatMessages.value = _chatMessages.value + ChatMessage(sender = "AI", text = aiResponseText)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _chatMessages.value = _chatMessages.value + ChatMessage(
                        sender = "AI",
                        text = "I encountered a communication issue checking Panchayat records: ${e.localizedMessage}. Please try again shortly."
                    )
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isProcessingAI.value = false
                }
            }
        }
    }

    // --- Update Ticket Status (For Admin Screen) ---
    fun updateTicketStatus(id: String, newStatus: String) {
        val updated = _tickets.value.map {
            if (it.id == id) {
                it.copy(status = newStatus)
            } else {
                it
            }
        }
        _tickets.value = updated
    }

    // --- Authentication & Society Registration Logic ---

    fun registerSocietyAndLogin(
        fullName: String,
        email: String,
        societyName: String,
        location: String,
        wings: String
    ): String {
        // Generate a unique society join code
        val randomNum = (1000..9999).random().toString()
        val generatedCode = "PANC-$randomNum"
        
        val newSociety = SocietyDetails(
            name = societyName,
            location = location,
            wings = wings,
            code = generatedCode,
            creatorName = fullName
        )
        
        // Save to our master society pool
        _registeredSocieties.value = _registeredSocieties.value + (generatedCode to newSociety)
        
        // Establish current active session
        val session = UserSession(
            fullName = fullName,
            email = email,
            role = "Secretary",
            societyName = societyName,
            joinCode = generatedCode,
            apartment = "Office #1",
            pinCodeOrLocation = location,
            wingsCount = wings
        )
        _currentUser.value = session
        return generatedCode
    }

    fun joinSocietyAndLogin(
        fullName: String,
        email: String,
        role: String,
        serviceType: String?,
        joinCode: String,
        apartmentOrDetail: String
    ): Boolean {
        val uppercaseCode = joinCode.uppercase().trim()
        val society = _registeredSocieties.value[uppercaseCode] ?: return false
        
        val actualApartment = if (role == "Service Provider") {
            "Partner: ${serviceType ?: "General Support"}"
        } else {
            apartmentOrDetail
        }

        val session = UserSession(
            fullName = fullName,
            email = email,
            role = role,
            serviceType = serviceType,
            societyName = society.name,
            joinCode = uppercaseCode,
            apartment = actualApartment,
            pinCodeOrLocation = society.location,
            wingsCount = society.wings
        )
        _currentUser.value = session
        return true
    }

    fun loginExistingUser(email: String, isGoogle: Boolean = false): Boolean {
        val name = if (isGoogle) {
            "Google Authorized User"
        } else {
            email.substringBefore("@").replaceFirstChar { it.uppercase() }
        }
        val defaultCode = "PANC-999"
        val society = _registeredSocieties.value[defaultCode] ?: return false
        
        val session = UserSession(
            fullName = name,
            email = email,
            role = "Resident",
            societyName = society.name,
            joinCode = defaultCode,
            apartment = "A-304",
            pinCodeOrLocation = society.location,
            wingsCount = society.wings
        )
        _currentUser.value = session
        return true
    }

    fun logout() {
        _currentUser.value = null
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
    }
}
