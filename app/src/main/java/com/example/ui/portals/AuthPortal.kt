package com.example.ui.portals

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserEntity
import com.example.ui.MarketplaceViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthPortal(
    viewModel: MarketplaceViewModel,
    onAuthSuccess: () -> Unit
) {
    var isLoginTab by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // --- Login Form State ---
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Login Errors
    var errLoginUsername by remember { mutableStateOf<String?>(null) }
    var errLoginPassword by remember { mutableStateOf<String?>(null) }

    // --- Registration Form State ---
    var regUsername by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regFullName by remember { mutableStateOf("") }
    var regNIN by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf("Client") } // "Client", "Maid"
    var regSkills by remember { mutableStateOf(setOf<String>("Deep Cleaning")) }
    var regDistrict by remember { mutableStateOf("Kampala") }
    var regSubcounty by remember { mutableStateOf("Kampala Central") }
    var regVillage by remember { mutableStateOf("Kololo") }

    // Registration Errors
    var errRegFullName by remember { mutableStateOf<String?>(null) }
    var errRegUsername by remember { mutableStateOf<String?>(null) }
    var errRegPassword by remember { mutableStateOf<String?>(null) }
    var errRegNIN by remember { mutableStateOf<String?>(null) }
    var errRegPhone by remember { mutableStateOf<String?>(null) }
    var errRegEmail by remember { mutableStateOf<String?>(null) }
    var errRegDistrict by remember { mutableStateOf<String?>(null) }
    var errRegSubcounty by remember { mutableStateOf<String?>(null) }
    var errRegVillage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // --- Ugandan NIN Verification Checklist Rules ---
    val cleanNIN = regNIN.trim().uppercase()
    val isLength14 = cleanNIN.length == 14
    val hasValidPrefix = cleanNIN.startsWith("CM") || 
                         cleanNIN.startsWith("CF") || 
                         cleanNIN.startsWith("RM") || 
                         cleanNIN.startsWith("RF") || 
                         cleanNIN.startsWith("PM") || 
                         cleanNIN.startsWith("PF")
    val hasValidSuffix = cleanNIN.length >= 3 && cleanNIN.takeLast(3).all { it.isLetter() }
    val isNINValid = isLength14 && hasValidPrefix && hasValidSuffix

    // --- Login Form Live & On-Submit Validator ---
    fun validateLoginForm(): Boolean {
        var isValid = true
        if (loginUsername.trim().isEmpty()) {
            errLoginUsername = "Username is required."
            isValid = false
        } else {
            errLoginUsername = null
        }

        if (loginPassword.isEmpty()) {
            errLoginPassword = "Password is required."
            isValid = false
        } else {
            errLoginPassword = null
        }
        return isValid
    }

    // --- Registration Form Live & On-Submit Validator ---
    fun validateRegistrationForm(): Boolean {
        var isValid = true

        // Full legal name: min 3 chars and contains at least two words
        val nameWords = regFullName.trim().split("\\s+".toRegex())
        if (regFullName.trim().isEmpty()) {
            errRegFullName = "Full Legal Name is required."
            isValid = false
        } else if (regFullName.trim().length < 3) {
            errRegFullName = "Legal Name must be at least 3 characters."
            isValid = false
        } else if (nameWords.size < 2) {
            errRegFullName = "Please enter both First and Last Name."
            isValid = false
        } else {
            errRegFullName = null
        }

        // Username check
        val usernamePattern = "^[a-zA-Z0-9_]+$".toRegex()
        if (regUsername.trim().isEmpty()) {
            errRegUsername = "Username is required."
            isValid = false
        } else if (regUsername.trim().length < 3) {
            errRegUsername = "Username must be at least 3 characters."
            isValid = false
        } else if (!usernamePattern.matches(regUsername.trim())) {
            errRegUsername = "Username can only contain alphanumeric & underscores."
            isValid = false
        } else {
            errRegUsername = null
        }

        // Password check
        if (regPassword.isEmpty()) {
            errRegPassword = "Password is required."
            isValid = false
        } else if (regPassword.length < 5) {
            errRegPassword = "Password must be at least 5 characters."
            isValid = false
        } else {
            errRegPassword = null
        }

        // Ugandan NIN Check
        if (regNIN.trim().isEmpty()) {
            errRegNIN = "NIN is required."
            isValid = false
        } else if (!isNINValid) {
            errRegNIN = "Invalid Ugandan NIN format. Follow specifications below."
            isValid = false
        } else {
            errRegNIN = null
        }

        // Phone number checking
        val phonePattern = "^(07[0-9]{8}|\\+256[0-9]{9})$".toRegex()
        val plainPhone = regPhone.trim().replace(" ", "")
        if (regPhone.trim().isEmpty()) {
            errRegPhone = "Phone number is required."
            isValid = false
        } else if (!phonePattern.matches(plainPhone)) {
            errRegPhone = "Enter format +2567xxxxxxxx or 07xxxxxxxx."
            isValid = false
        } else {
            errRegPhone = null
        }

        // Email address check
        val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}$".toRegex()
        if (regEmail.trim().isNotEmpty() && !emailPattern.matches(regEmail.trim())) {
            errRegEmail = "Please enter a valid email address."
            isValid = false
        } else {
            errRegEmail = null
        }

        // Regional locations
        if (regDistrict.trim().isEmpty()) {
            errRegDistrict = "District is required."
            isValid = false
        } else {
            errRegDistrict = null
        }

        if (regSubcounty.trim().isEmpty()) {
            errRegSubcounty = "Sub-county is required."
            isValid = false
        } else {
            errRegSubcounty = null
        }

        if (regVillage.trim().isEmpty()) {
            errRegVillage = "Village is required."
            isValid = false
        } else {
            errRegVillage = null
        }

        return isValid
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBg)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("auth_portal_root"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(UgandaGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "MaidsConnect Security Hub",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "MaidsConnect",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Uganda's Verified Staffing Directory & Trust Ledger Network",
                color = TextGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // --- Elegant Unified Tab Segment ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonCard, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderSlate, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        isLoginTab = true
                        errorMessage = null
                        successMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLoginTab) UgandaGold else Color.Transparent,
                        contentColor = if (isLoginTab) Color.White else TextWhite
                    ),
                    elevation = null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_login"),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Secure Login", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        isLoginTab = false
                        errorMessage = null
                        successMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isLoginTab) UgandaGold else Color.Transparent,
                        contentColor = if (!isLoginTab) Color.White else TextWhite
                    ),
                    elevation = null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_register"),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = "User icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Register Account", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Global Alerts Box
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CrimsonA.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CrimsonA.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Verification Guard", tint = CrimsonA, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(errorMessage!!, color = CrimsonA, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                }
            }

            if (successMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SavannahGreen.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SavannahGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success tick", tint = SavannahGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(successMessage!!, color = SavannahGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                }
            }

            AnimatedContent(
                targetState = isLoginTab,
                label = "FormTypeCrossfade"
            ) { targetLoginTab ->
                if (targetLoginTab) {
                    // --- LOGIN VIEW ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarbonCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderSlate),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "Account Verification Gateway",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Username Input
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    value = loginUsername,
                                    onValueChange = {
                                        loginUsername = it
                                        errLoginUsername = null
                                    },
                                    label = { Text("Username") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = UgandaGold,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedLabelColor = UgandaGold,
                                        unfocusedLabelColor = TextGray,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        errorBorderColor = CrimsonA
                                    ),
                                    isError = errLoginUsername != null,
                                    placeholder = { Text("Type registered username...", color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_login_username")
                                )
                                if (errLoginUsername != null) {
                                    Text(errLoginUsername!!, color = CrimsonA, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                                }
                            }

                            // Password Input
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = {
                                        loginPassword = it
                                        errLoginPassword = null
                                    },
                                    label = { Text("Security Code / Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted) },
                                    visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        TextButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                            Text(
                                                text = if (showLoginPassword) "Hide" else "Show",
                                                color = UgandaGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = UgandaGold,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedLabelColor = UgandaGold,
                                        unfocusedLabelColor = TextGray,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        errorBorderColor = CrimsonA
                                    ),
                                    isError = errLoginPassword != null,
                                    placeholder = { Text("••••••", color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_login_password")
                                )
                                if (errLoginPassword != null) {
                                    Text(errLoginPassword!!, color = CrimsonA, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    if (validateLoginForm()) {
                                        errorMessage = null
                                        viewModel.loginCheck(loginUsername.trim(), loginPassword) { result ->
                                            when (result) {
                                                "SUCCESS" -> {
                                                    successMessage = "Session Authorized!"
                                                    onAuthSuccess()
                                                }
                                                "SUSPENDED" -> {
                                                    errorMessage = "ACCESS BLOCKED: This account has been Suspended by the administration."
                                                }
                                                "PENDING_APPROVAL" -> {
                                                    errorMessage = "PENDING APPROVAL: Your account is currently pending administrative approval. MaidsConnect will contact you within 24 hours."
                                                }
                                                else -> {
                                                    errorMessage = "Verification failed: Check Username and Password."
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = UgandaGold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_auth_submit")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Submit Access Permission", tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verify & Access Portal", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    // --- REGISTRATION VIEW ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarbonCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderSlate),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Ministry Legal Ledger Register",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // 1. Legal Name
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    value = regFullName,
                                    onValueChange = {
                                        regFullName = it
                                        errRegFullName = null
                                    },
                                    label = { Text("Full Legal Name") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = UgandaGold,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedLabelColor = UgandaGold,
                                        unfocusedLabelColor = TextGray,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        errorBorderColor = CrimsonA
                                    ),
                                    isError = errRegFullName != null,
                                    placeholder = { Text("e.g. Namubiru Justine", color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reg_fullname")
                                )
                                if (errRegFullName != null) {
                                    Text(errRegFullName!!, color = CrimsonA, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                                }
                            }

                            // 2. Row of Username and Password
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(
                                        value = regUsername,
                                        onValueChange = {
                                            regUsername = it
                                            errRegUsername = null
                                        },
                                        label = { Text("Username") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = UgandaGold,
                                            unfocusedBorderColor = BorderSlate,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            errorBorderColor = CrimsonA
                                        ),
                                        isError = errRegUsername != null,
                                        placeholder = { Text("justine_n", color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.testTag("reg_username")
                                    )
                                    if (errRegUsername != null) {
                                        Text(errRegUsername!!, color = CrimsonA, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
                                    }
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(
                                        value = regPassword,
                                        onValueChange = {
                                            regPassword = it
                                            errRegPassword = null
                                        },
                                        label = { Text("Password") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = UgandaGold,
                                            unfocusedBorderColor = BorderSlate,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            errorBorderColor = CrimsonA
                                        ),
                                        isError = errRegPassword != null,
                                        placeholder = { Text("•••••", color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.testTag("reg_password")
                                    )
                                    if (errRegPassword != null) {
                                        Text(errRegPassword!!, color = CrimsonA, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
                                    }
                                }
                            }

                            // 3. Ugandan NIN Field with Interactive Live Form Verifier Checklist
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = regNIN,
                                    onValueChange = {
                                        if (it.length <= 14) {
                                            regNIN = it
                                            errRegNIN = null
                                        }
                                    },
                                    label = { Text("National Identification Number (NIN)") },
                                    leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = "ID Card Verification", tint = TextMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = UgandaGold,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedLabelColor = UgandaGold,
                                        unfocusedLabelColor = TextGray,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        errorBorderColor = CrimsonA
                                    ),
                                    isError = errRegNIN != null,
                                    placeholder = { Text("CM902847192UGA (14 chars)", color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reg_nin")
                                )
                                if (errRegNIN != null) {
                                    Text(errRegNIN!!, color = CrimsonA, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                                }

                                // Interactive NIN Rules panel representing Ministry authenticity factors
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CarbonBg),
                                    border = BorderStroke(1.dp, BorderSlate),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "NIN NIRA Format Requirements",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = UgandaGold
                                        )

                                        // Requirement 1: Exactly 14 signs
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isLength14) Icons.Default.CheckCircle else Icons.Default.Info,
                                                contentDescription = null,
                                                tint = if (isLength14) UgandaGold else TextMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                "Exactly 14 alphanumeric characters (${regNIN.length}/14)",
                                                fontSize = 11.sp,
                                                color = if (isLength14) TextWhite else TextGray
                                            )
                                        }

                                        // Requirement 2: Valid Citizens/Refugee code
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (hasValidPrefix) Icons.Default.CheckCircle else Icons.Default.Info,
                                                contentDescription = null,
                                                tint = if (hasValidPrefix) UgandaGold else TextMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                "Starts with lawful prefixes (CM, CF, RM, RF, PM, PF)",
                                                fontSize = 11.sp,
                                                color = if (hasValidPrefix) TextWhite else TextGray
                                            )
                                        }

                                        // Requirement 3: Country identification suffix
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (hasValidSuffix) Icons.Default.CheckCircle else Icons.Default.Info,
                                                contentDescription = null,
                                                tint = if (hasValidSuffix) UgandaGold else TextMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                "Ends with a 3-letter country suffix (e.g. UGA)",
                                                fontSize = 11.sp,
                                                color = if (hasValidSuffix) TextWhite else TextGray
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Contact Row (Phone + Email)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(
                                        value = regPhone,
                                        onValueChange = {
                                            regPhone = it
                                            errRegPhone = null
                                        },
                                        label = { Text("Ugandan Phone") },
                                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TextMuted) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = UgandaGold,
                                            unfocusedBorderColor = BorderSlate,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            errorBorderColor = CrimsonA
                                        ),
                                        isError = errRegPhone != null,
                                        placeholder = { Text("0772123456", color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.testTag("reg_phone")
                                    )
                                    if (errRegPhone != null) {
                                        Text(errRegPhone!!, color = CrimsonA, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
                                    }
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(
                                        value = regEmail,
                                        onValueChange = {
                                            regEmail = it
                                            errRegEmail = null
                                        },
                                        label = { Text("Email (Opt)") },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = UgandaGold,
                                            unfocusedBorderColor = BorderSlate,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            errorBorderColor = CrimsonA
                                        ),
                                        isError = errRegEmail != null,
                                        placeholder = { Text("sarah@gmail.com", color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.testTag("reg_email")
                                    )
                                    if (errRegEmail != null) {
                                        Text(errRegEmail!!, color = CrimsonA, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
                                    }
                                }
                            }

                            // 5. Unified Role Selector Card (Requirement)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "System Access Role selection",
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val roles = listOf("Client", "Maid")
                                    roles.forEach { role ->
                                        val isSelected = regRole == role
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) UgandaGold else CarbonCardPressed)
                                                .border(1.dp, if (isSelected) UgandaGold else BorderSlate, RoundedCornerShape(10.dp))
                                                .clickable { regRole = role }
                                                .padding(vertical = 12.dp)
                                                .testTag("role_select_$role"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = when (role) {
                                                        "Client" -> Icons.Default.Face
                                                        else -> Icons.Default.Star
                                                    },
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color.White else TextWhite,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = role,
                                                    color = if (isSelected) Color.White else TextWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                if (regRole == "Maid") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(CarbonCard, RoundedCornerShape(12.dp))
                                             .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "What can you do? (Select skills)",
                                            color = UgandaGold,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Select the professional skills or details you offer to clients in Uganda:",
                                            color = TextGray,
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp
                                        )

                                        val availableSkills = listOf(
                                            "Infant Nursing",
                                            "Deep Cleaning",
                                            "Laundry",
                                            "Culinary Arts",
                                            "Pet Care",
                                            "Elder Care"
                                        )

                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            availableSkills.forEach { s ->
                                                val checked = regSkills.contains(s)
                                                FilterChip(
                                                    selected = checked,
                                                    onClick = {
                                                        regSkills = if (checked) regSkills - s else regSkills + s
                                                    },
                                                    label = { Text(s, fontSize = 11.sp) },
                                                    leadingIcon = {
                                                        if (checked) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = UgandaGold,
                                                        selectedLabelColor = Color.Black,
                                                        containerColor = CarbonBg,
                                                        labelColor = TextWhite
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 6. Geographical Coordinates info
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Primary Local Territory (Uganda)",
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        OutlinedTextField(
                                            value = regDistrict,
                                            onValueChange = {
                                                regDistrict = it
                                                errRegDistrict = null
                                            },
                                            label = { Text("District") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = UgandaGold,
                                                unfocusedBorderColor = BorderSlate,
                                                focusedTextColor = TextWhite,
                                                unfocusedTextColor = TextWhite
                                            ),
                                            isError = errRegDistrict != null,
                                            singleLine = true
                                        )
                                        if (errRegDistrict != null) {
                                            Text(errRegDistrict!!, color = CrimsonA, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        OutlinedTextField(
                                            value = regSubcounty,
                                            onValueChange = {
                                                regSubcounty = it
                                                errRegSubcounty = null
                                            },
                                            label = { Text("Subcounty") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = UgandaGold,
                                                unfocusedBorderColor = BorderSlate,
                                                focusedTextColor = TextWhite,
                                                unfocusedTextColor = TextWhite
                                            ),
                                            isError = errRegSubcounty != null,
                                            singleLine = true
                                        )
                                        if (errRegSubcounty != null) {
                                            Text(errRegSubcounty!!, color = CrimsonA, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        OutlinedTextField(
                                            value = regVillage,
                                            onValueChange = {
                                                regVillage = it
                                                errRegVillage = null
                                            },
                                            label = { Text("Village") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = UgandaGold,
                                                unfocusedBorderColor = BorderSlate,
                                                focusedTextColor = TextWhite,
                                                unfocusedTextColor = TextWhite
                                            ),
                                            isError = errRegVillage != null,
                                            singleLine = true
                                        )
                                        if (errRegVillage != null) {
                                            Text(errRegVillage!!, color = CrimsonA, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            Text(
                                "By registering, you verify that your NIN profile is accurate and registered in NIRA's database according to the laws of Uganda.",
                                color = TextGray,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )

                            Button(
                                onClick = {
                                    if (validateRegistrationForm()) {
                                        errorMessage = null
                                        val roleMapped = if (regRole == "Maid") "Housekeeper" else regRole
                                        viewModel.registerNewUser(
                                            username = regUsername.trim(),
                                            pwhash = regPassword,
                                            fullName = regFullName.trim(),
                                            nin = cleanNIN,
                                            phone = regPhone.trim(),
                                            email = regEmail.trim(),
                                            role = roleMapped,
                                            district = regDistrict.trim(),
                                            subcounty = regSubcounty.trim(),
                                            village = regVillage.trim(),
                                            maidSkills = regSkills.joinToString(",")
                                        ) { isOk ->
                                            if (isOk) {
                                                successMessage = "Registration Completed! Your account is pending admin approval."
                                                isLoginTab = true
                                            } else {
                                                errorMessage = "Registration Rejected: Username is already registered."
                                            }
                                        }
                                    } else {
                                        errorMessage = "Please resolve the validation errors shown below."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = UgandaGold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_register_submit")
                            ) {
                                Text("Complete Verification & Registration", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
