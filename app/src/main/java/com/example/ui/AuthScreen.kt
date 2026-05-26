package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import com.example.viewmodel.PanchayatViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: PanchayatViewModel,
    modifier: Modifier = Modifier
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Role state
    var selectedRole by remember { mutableStateOf("Resident") } // Resident, Secretary, Service Provider

    // Joint and Society Fields
    var joinCodeInput by remember { mutableStateOf("PANC-999") }
    var apartmentInput by remember { mutableStateOf("") } // A-304
    
    // Service Provider Fields
    var selectedServiceType by remember { mutableStateOf("Plumber") }
    val serviceTypes = listOf("Plumber", "Carpenter", "Electrician", "Painter", "Pest Control", "Security Guard", "Gardener")

    // Secretary fields for creating a new society
    var societyNameInput by remember { mutableStateOf("") }
    var societyLocationInput by remember { mutableStateOf("") }
    var societyWingsInput by remember { mutableStateOf("") } // e.g. A, B, C

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessRegDialog by remember { mutableStateOf<String?>(null) } // holds generated code

    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Logo Icon & Text
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(PanchayatOrange, PanchayatOrangeLight))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MapsHomeWork,
                    contentDescription = "Panchayat Core Logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Panchayat",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Text(
                text = "AI-Driven Smart Society Management Engine",
                fontSize = 12.sp,
                color = PanchayatGreenLight,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Professional Login & Signup Panel Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tab Selector Tab Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceElevated),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!isSignUpMode) PanchayatOrange else Color.Transparent)
                                .clickable { 
                                    isSignUpMode = false
                                    errorMessage = null
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Login",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSignUpMode) PanchayatOrange else Color.Transparent)
                                .clickable { 
                                    isSignUpMode = true
                                    errorMessage = null
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign Up",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error icon",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // --- Form inputs ---

                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
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
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = PanchayatOrange,
                            unfocusedBorderColor = DarkSurfaceElevated
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = PanchayatOrange,
                            unfocusedBorderColor = DarkSurfaceElevated
                        ),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // --- Dynamic Role & Society Registration Selection (For Sign Up) ---
                    if (isSignUpMode) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = DarkSurfaceElevated)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Select Application Role:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PanchayatOrangeLight,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Role Selection Row Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Resident", "Secretary", "Provider").forEach { role ->
                                val isSelected = when (role) {
                                    "Provider" -> selectedRole == "Service Provider"
                                    else -> selectedRole == role
                                }
                                val actualRole = if (role == "Provider") "Service Provider" else role

                                Surface(
                                    onClick = { selectedRole = actualRole },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PanchayatOrange else DarkSurfaceElevated,
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = role,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual form changes depending on your Selected Role
                        AnimatedVisibility(
                            visible = selectedRole == "Secretary",
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurfaceElevated.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "🏢 Register a New Society",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PanchayatGreenLight
                                )

                                OutlinedTextField(
                                    value = societyNameInput,
                                    onValueChange = { societyNameInput = it },
                                    label = { Text("Society Name") },
                                    placeholder = { Text("e.g. Gokuldham Co-op Society") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.LightGray,
                                        focusedBorderColor = PanchayatGreenLight,
                                        unfocusedBorderColor = DarkSurfaceElevated
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = societyLocationInput,
                                    onValueChange = { societyLocationInput = it },
                                    label = { Text("Location City / PIN") },
                                    placeholder = { Text("e.g. Powai, Mumbai - 400076") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.LightGray,
                                        focusedBorderColor = PanchayatGreenLight,
                                        unfocusedBorderColor = DarkSurfaceElevated
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = societyWingsInput,
                                    onValueChange = { societyWingsInput = it },
                                    label = { Text("Wings/Blocks (Comma Separated)") },
                                    placeholder = { Text("e.g. A, B, C, D") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.LightGray,
                                        focusedBorderColor = PanchayatGreenLight,
                                        unfocusedBorderColor = DarkSurfaceElevated
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = selectedRole == "Resident",
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurfaceElevated.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "🔑 Join your Society",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PanchayatOrangeLight
                                )

                                OutlinedTextField(
                                    value = joinCodeInput,
                                    onValueChange = { joinCodeInput = it },
                                    label = { Text("Society Join Code") },
                                    placeholder = { Text("e.g. PANC-999") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.LightGray,
                                        focusedBorderColor = PanchayatOrange,
                                        unfocusedBorderColor = DarkSurfaceElevated
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = apartmentInput,
                                    onValueChange = { apartmentInput = it },
                                    label = { Text("Flat & Wing Number") },
                                    placeholder = { Text("e.g. Block B, Flat 402") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.LightGray,
                                        focusedBorderColor = PanchayatOrange,
                                        unfocusedBorderColor = DarkSurfaceElevated
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = selectedRole == "Service Provider",
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurfaceElevated.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "🛠️ Specialist Field",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Yellow
                                )

                                OutlinedTextField(
                                    value = joinCodeInput,
                                    onValueChange = { joinCodeInput = it },
                                    label = { Text("Society Join Code") },
                                    placeholder = { Text("e.g. PANC-999") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.LightGray,
                                        focusedBorderColor = PanchayatOrange,
                                        unfocusedBorderColor = DarkSurfaceElevated
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Text(
                                    text = "Select your Professional Field:",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                // Simple Flow Row / Grid of Specialist choices
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    serviceTypes.forEach { type ->
                                        val isTypeSelected = selectedServiceType == type
                                        Surface(
                                            onClick = { selectedServiceType = type },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isTypeSelected) PanchayatGreen else DarkSurfaceElevated,
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    text = type,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary submit button
                    Button(
                        onClick = {
                            errorMessage = null
                            if (emailInput.isBlank() || passwordInput.isBlank()) {
                                errorMessage = "Email and Password cannot be blank."
                                return@Button
                            }

                            if (isSignUpMode) {
                                if (nameInput.isBlank()) {
                                    errorMessage = "Please enter your name."
                                    return@Button
                                }

                                when (selectedRole) {
                                    "Secretary" -> {
                                        if (societyNameInput.isBlank() || societyLocationInput.isBlank() || societyWingsInput.isBlank()) {
                                            errorMessage = "Please complete all society details (Name, Location, Wings)."
                                            return@Button
                                        }

                                        val code = viewModel.registerSocietyAndLogin(
                                            fullName = nameInput,
                                            email = emailInput,
                                            societyName = societyNameInput,
                                            location = societyLocationInput,
                                            wings = societyWingsInput
                                        )
                                        // Open join code generation success dialog
                                        showSuccessRegDialog = code
                                    }
                                    
                                    "Resident" -> {
                                        if (joinCodeInput.isBlank() || apartmentInput.isBlank()) {
                                            errorMessage = "Please provide both the Society Join Code and your Apartment Number."
                                            return@Button
                                        }

                                        val isJoined = viewModel.joinSocietyAndLogin(
                                            fullName = nameInput,
                                            email = emailInput,
                                            role = "Resident",
                                            serviceType = null,
                                            joinCode = joinCodeInput,
                                            apartmentOrDetail = apartmentInput
                                        )

                                        if (!isJoined) {
                                            errorMessage = "Society Join Code '$joinCodeInput' is invalid. Try PANC-999 or ask your Secretary."
                                        }
                                    }

                                    "Service Provider" -> {
                                        if (joinCodeInput.isBlank()) {
                                            errorMessage = "Please insert Society Join Code to register with."
                                            return@Button
                                        }

                                        val isJoined = viewModel.joinSocietyAndLogin(
                                            fullName = nameInput,
                                            email = emailInput,
                                            role = "Service Provider",
                                            serviceType = selectedServiceType,
                                            joinCode = joinCodeInput,
                                            apartmentOrDetail = selectedServiceType
                                        )

                                        if (!isJoined) {
                                            errorMessage = "Society Join Code '$joinCodeInput' is invalid. Try PANC-999 or ask your Secretary."
                                        }
                                    }
                                }
                            } else {
                                // Simple Login Flow matching existing
                                val success = viewModel.loginExistingUser(emailInput)
                                if (!success) {
                                    errorMessage = "Error initializing session. Please try again."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PanchayatOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_auth")
                    ) {
                        Text(
                            text = if (isSignUpMode) "Generate & Setup Core Session" else "Log In to Portal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "— OR CONTINUING WITH GOOGLE —",
                        color = TextSecondaryDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Simulated Google OAuth button
                    OutlinedButton(
                        onClick = {
                            viewModel.loginExistingUser("godejatin@gmail.com", isGoogle = true)
                        },
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(PanchayatGreenLight, PanchayatOrange))
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("google_auth_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AlternateEmail,
                                contentDescription = "Google Icon",
                                tint = PanchayatOrangeLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Continue with Google Smart Login",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Handy hint footer
            Spacer(modifier = Modifier.height(18.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "💡 Quick Tip: Use PANC-999 as Society Code to test joining immediately, or register a new one as a Secretary to share!",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Secretary Join Code share dialog popup
    showSuccessRegDialog?.let { joinCode ->
        Dialog(onDismissRequest = { showSuccessRegDialog = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = PanchayatGreenLight,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Society Registered Successfully!",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Share this Join Code with other residents or service providers so they can connect directly with $societyNameInput.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Core Highlight Code
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceElevated)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = joinCode,
                            color = PanchayatOrangeLight,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showSuccessRegDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = PanchayatOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Launch Panchayat Board", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
