package com.example.ui.portals

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.data.*
import com.example.ui.MarketplaceViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HousekeeperPortal(
    viewModel: MarketplaceViewModel,
    modifier: Modifier = Modifier
) {
    val hks by viewModel.housekeepers.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val activeMaidId by viewModel.activeMaidId.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showPhotoPicker by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var uploadStatus by remember { mutableStateOf("") }

    // Find housekeeper linked to current authenticated user
    val housekeeperByKyc = hks.find { it.nin == currentUser?.nin || it.name.equals(currentUser?.fullName, ignoreCase = true) }
    
    // Fallback or Admin selected profile
    val activeHk = if (currentUser?.role == "Housekeeper") housekeeperByKyc else hks.find { it.id == activeMaidId }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null && activeHk != null) {
            coroutineScope.launch {
                viewModel.uploadProfileImage(
                    context = context,
                    imageUri = uri,
                    housekeeper = activeHk,
                    onProgress = { progress, status ->
                        uploadProgress = progress
                        uploadStatus = status
                    },
                    onComplete = { downloadUrl ->
                        uploadStatus = "✓ Photo successfully verified!"
                        uploadProgress = 1.0f
                        coroutineScope.launch {
                            delay(1500)
                            showPhotoPicker = false
                            uploadProgress = 0f
                            uploadStatus = ""
                        }
                    },
                    onError = { errorMsg ->
                        uploadStatus = "Error: $errorMsg"
                        uploadProgress = 0f
                    }
                )
            }
        }
    }

    // Synchronize active maid ID flow for consistency
    if (currentUser?.role == "Housekeeper" && housekeeperByKyc != null && activeMaidId != housekeeperByKyc.id) {
        LaunchedEffect(housekeeperByKyc.id) {
            viewModel.selectActiveMaidId(housekeeperByKyc.id)
        }
    }

    // Top-level tab management state
    var activeTab by remember { mutableStateOf("BOOKINGS") }

    // State parameters for bio, rate, cycle - defined at function-level to remain synchronous
    var bioText by remember(activeHk?.id) { mutableStateOf(activeHk?.bio ?: "") }
    var rateVal by remember(activeHk?.id) { mutableStateOf(activeHk?.rateUGX?.toString() ?: "15000") }
    var rateBasis by remember(activeHk?.id) { mutableStateOf(activeHk?.rateType ?: "Daily") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        // --- 1. Top Session Selector (Admin/Simulated only) ---
        if (currentUser?.role != "Housekeeper") {
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonCardPressed),
                border = BorderStroke(1.dp, UgandaGold.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Simulated Housekeeper Identity:",
                        color = UgandaGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    var expandMaids by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CarbonBg, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                                .clickable { expandMaids = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(activeHk?.avatarEmoji ?: "👩🏾", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    activeHk?.let { "${it.name} (${it.vettingStatus})" } ?: "Select Identity",
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = UgandaGold)
                        }

                        DropdownMenu(
                            expanded = expandMaids,
                            onDismissRequest = { expandMaids = false },
                            modifier = Modifier.background(CarbonCard).border(1.dp, BorderSlate)
                        ) {
                            hks.forEach { hk ->
                                DropdownMenuItem(
                                    leadingIcon = { Text(hk.avatarEmoji) },
                                    text = { Text("${hk.name} [${hk.vettingStatus}]", color = TextWhite) },
                                    onClick = {
                                        viewModel.selectActiveMaidId(hk.id)
                                        expandMaids = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (activeHk == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No housekeeper profile selected.", color = TextGray)
            }
            return
        }

        val maidBookings = bookings.filter { it.housekeeperId == activeHk.id }
        val activeCount = maidBookings.count { it.status == "Confirmed" || it.status == "Pending" }
        val completedCount = maidBookings.count { it.status == "Completed" }

        // --- MODERN TAB BAR FOR HOUSEKEEPER ---
        val tabs = listOf("BOOKINGS", "PROFILE", "REVIEWS")
        TabRow(
            selectedTabIndex = tabs.indexOf(activeTab),
            containerColor = Color.Transparent,
            contentColor = UgandaGold,
            divider = { Divider(color = BorderSlate, thickness = 1.dp) }
        ) {
            tabs.forEach { tab ->
                val selected = activeTab == tab
                Tab(
                    selected = selected,
                    onClick = { activeTab = tab },
                    text = {
                        Text(
                            text = tab,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) UgandaGold else TextGray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TAB CONTENT ---
        if (activeTab == "BOOKINGS") {
            // Analytics Scorecard
            Text(
                text = "Analytics Scorecard",
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stat 1: Placements count
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonCard),
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("HIRE MATCHES", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${maidBookings.size}",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Text("$activeCount active • $completedCount completed", color = UgandaGold, fontSize = 9.sp)
                    }
                }

                // Stat 2: Rating
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonCard),
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TRUST SCORE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                String.format("%.1f", activeHk.rating),
                                color = TextWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = UgandaGold, modifier = Modifier.size(16.dp))
                        }
                        Text("${activeHk.reviewCount} user comments", color = SavannahGreenLight, fontSize = 9.sp)
                    }
                }

                // Stat 3: Vetting Status
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonCard),
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("LICENSE STATUS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        val statusColor = when (activeHk.vettingStatus) {
                            "Approved" -> SavannahGreenLight
                            "Suspended" -> CrimsonA
                            else -> UgandaGold
                        }
                        Text(
                            activeHk.vettingStatus.uppercase(),
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                        Text("NIN Verified", color = TextMuted, fontSize = 9.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Contracts Inbox & Status Controls
            Text(
                text = "Live Placements Inbox (${maidBookings.size})",
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (maidBookings.isEmpty()) {
                Surface(
                    color = CarbonCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Empty", tint = TextMuted, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Your inbox is empty", color = TextWhite, fontWeight = FontWeight.SemiBold)
                        Text("Waiting for client matches. Share your rate card to attract offers!", color = TextGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                maidBookings.forEach { booking ->
                    ContractInboxCard(booking = booking, onAcceptConfirm = { viewModel.housekeeperConfirmBooking(booking, it) }, onMarkCompleted = { viewModel.housekeeperMarkBookingCompleted(booking) })
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

        } else if (activeTab == "PROFILE") {
            // Interactive Profile Builder
            Text(
                text = "Professional Identity Builder & Rate Card",
                color = UgandaGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonCard),
                border = BorderStroke(1.dp, BorderSlate),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Profile Photo selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CarbonBg)
                                .border(2.dp, UgandaGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeHk != null && activeHk.profileImageUri != null) {
                                if (activeHk.profileImageUri == "preset_1") {
                                    Image(
                                        painter = painterResource(id = com.example.R.drawable.img_maid_profile_one),
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (activeHk.profileImageUri == "preset_2") {
                                    Image(
                                        painter = painterResource(id = com.example.R.drawable.img_maid_profile_two),
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = activeHk.profileImageUri),
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else if (activeHk != null) {
                                Text(activeHk.avatarEmoji, fontSize = 32.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeHk.name,
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (activeHk.profileImageUri != null) "✓ Profile verification photo active" else "⚠ No verified photo attached",
                                color = if (activeHk.profileImageUri != null) SavannahGreenLight else UgandaGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Manage Profile Photo",
                                color = UgandaGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showPhotoPicker = true }
                                    .background(UgandaGold.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            if (showPhotoPicker) {
                                AlertDialog(
                                    onDismissRequest = { showPhotoPicker = false },
                                    title = { Text("Update Live Identity Face Proof", color = TextWhite, fontWeight = FontWeight.Bold) },
                                    containerColor = CarbonCard,
                                    text = {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text("Identity Verification Service (NIRA Secure Ledger Link)", color = TextGray, fontSize = 11.sp)

                                            Button(
                                                onClick = {
                                                    imagePickerLauncher.launch("image/*")
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = UgandaGold, contentColor = CarbonBg),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Upload custom photo from Device", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }

                                            Divider(color = BorderSlate.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                                            Text("Or select visual profile template:", color = TextGray, fontSize = 11.sp)
                                            if (uploadStatus.isNotEmpty()) {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Text(uploadStatus, color = UgandaGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    LinearProgressIndicator(progress = uploadProgress, color = UgandaGold, trackColor = CarbonBg, modifier = Modifier.fillMaxWidth())
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            coroutineScope.launch {
                                                                uploadStatus = "Compressing profile portrait..."
                                                                uploadProgress = 0.3f
                                                                delay(1000)
                                                                uploadStatus = "Broadcasting to ledger nodes..."
                                                                uploadProgress = 0.7f
                                                                delay(1000)
                                                                uploadStatus = "NIRA system verification complete!"
                                                                uploadProgress = 1.0f
                                                                delay(500)
                                                                val updated = activeHk.copy(profileImageUri = "preset_1")
                                                                viewModel.updateHousekeeperProfile(updated)
                                                                showPhotoPicker = false
                                                                uploadProgress = 0f
                                                                uploadStatus = ""
                                                            }
                                                        }
                                                        .padding(8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = com.example.R.drawable.img_maid_profile_one),
                                                        contentDescription = "Portrait 1",
                                                        modifier = Modifier.size(56.dp).clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Option A", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            coroutineScope.launch {
                                                                uploadStatus = "Optimizing photo matrix..."
                                                                uploadProgress = 0.3f
                                                                delay(1000)
                                                                uploadStatus = "Broadcasting secure proof hash..."
                                                                uploadProgress = 0.7f
                                                                delay(1000)
                                                                uploadStatus = "MaidsConnect trust validation success!"
                                                                uploadProgress = 1.0f
                                                                delay(500)
                                                                val updated = activeHk.copy(profileImageUri = "preset_2")
                                                                viewModel.updateHousekeeperProfile(updated)
                                                                showPhotoPicker = false
                                                                uploadProgress = 0f
                                                                uploadStatus = ""
                                                            }
                                                        }
                                                        .padding(8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = com.example.R.drawable.img_maid_profile_two),
                                                        contentDescription = "Portrait 2",
                                                        modifier = Modifier.size(56.dp).clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Option B", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showPhotoPicker = false }) {
                                            Text("Cancel", color = UgandaGold)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Divider(color = BorderSlate, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("PERSONAL PROFESSIONAL BIO", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = bioText,
                        onValueChange = { bioText = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = UgandaGold,
                            unfocusedBorderColor = BorderSlate,
                            focusedContainerColor = CarbonBg,
                            unfocusedContainerColor = CarbonBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DAILY/MONTHLY RATE (UGX)", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = rateVal,
                                onValueChange = { rateVal = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = UgandaGold,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedContainerColor = CarbonBg,
                                    unfocusedContainerColor = CarbonBg
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("PAY CYCLE BASIS", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Daily", "Monthly").forEach { basis ->
                                    FilterChip(
                                        selected = rateBasis == basis,
                                        onClick = { rateBasis = basis },
                                        label = { Text(basis) },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Specialized skills checkboxes
                    Text("CHOOSE SPECIALIZATION BADGES", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    val allSkills = listOf("Infant Nursing", "Deep Cleaning", "Laundry", "Culinary Arts", "Pet Care", "Elder Care")
                    val activeSkills = remember(activeHk.id) {
                        mutableStateMapOf<String, Boolean>().apply {
                            val split = activeHk.skills.split(",").map { it.trim() }
                            allSkills.forEach { skill ->
                                this[skill] = split.contains(skill)
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allSkills.forEach { skill ->
                            val checked = activeSkills[skill] ?: false
                            FilterChip(
                                selected = checked,
                                onClick = { activeSkills[skill] = !checked },
                                label = { Text(skill) },
                                leadingIcon = {
                                    if (checked) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
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

                    // Spoken languages selection
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("TOGGLE SPOKEN LANGUAGES IN UGANDA", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    val allLangs = listOf("English", "Luganda", "Swahili", "Runyankole", "Acholi")
                    val activeLangs = remember(activeHk.id) {
                        mutableStateMapOf<String, Boolean>().apply {
                            val split = activeHk.languages.split(",").map { it.trim() }
                            allLangs.forEach { l ->
                                this[l] = split.contains(l)
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allLangs.forEach { lang ->
                            val checked = activeLangs[lang] ?: false
                            FilterChip(
                                selected = checked,
                                onClick = { activeLangs[lang] = !checked },
                                label = { Text(lang) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = UgandaGold,
                                    selectedLabelColor = Color.Black,
                                    containerColor = CarbonBg,
                                    labelColor = TextWhite
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val skillsStr = activeSkills.filter { it.value }.keys.joinToString(",")
                            val langsStr = activeLangs.filter { it.value }.keys.joinToString(",")
                            val updated = activeHk.copy(
                                bio = bioText,
                                rateUGX = rateVal.toIntOrNull() ?: activeHk.rateUGX,
                                rateType = rateBasis,
                                skills = if (skillsStr.isBlank()) "Deep Cleaning" else skillsStr,
                                languages = if (langsStr.isBlank()) "Luganda" else langsStr
                            )
                            viewModel.updateHousekeeperProfile(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UgandaGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save Profile")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Rate Card & Profile Info", fontWeight = FontWeight.Bold)
                    }
                }
            }

        } else if (activeTab == "REVIEWS") {
            // Reviews & Ratings
            Text(
                text = "Your Service Reviews & Ratings",
                color = UgandaGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val maidReviews = reviews.filter { it.housekeeperId == activeHk.id }
            if (maidReviews.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonCard),
                    border = BorderStroke(1.dp, BorderSlate),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No written reviews yet", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Work in progress. Once clients complete placements, your performance reviews will populate here.", color = TextGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonCard),
                    border = BorderStroke(1.dp, BorderSlate),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        maidReviews.forEach { r ->
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(r.employerName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        (1..5).forEach { star ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (star <= r.rating) UgandaGold else TextGray,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "\"${r.comment}\"",
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                                Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }

        } else if (activeTab == "SUPPORT") {
            // Support Shortcut
            Text(
                text = "Instant Support Assistance & Escalation",
                color = UgandaGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonCard),
                border = BorderStroke(1.dp, BorderSlate),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("NEED ESCROW SUPPORT OR HAVE A DISPUTE?", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("As a MaidsConnect partner, your safety and payment protection are fully guaranteed. Contact our dispute desk for immediate assistance.", color = TextGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.selectPortal("FAQs") },
                        colors = ButtonDefaults.buttonColors(containerColor = CarbonBg),
                        border = BorderStroke(1.dp, UgandaGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Build, contentDescription = "Dispute Button", tint = UgandaGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Support Tickets", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonCardPressed),
                border = BorderStroke(1.dp, UgandaGold.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectPortal("FAQs") }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Support Contact", tint = UgandaGold, modifier = Modifier.size(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Call Helpline Immediate Desk Chat", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Tap to visit the Support & Dispute desk or raise helpline tickets.", color = TextMuted, fontSize = 10.sp)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = "Go", tint = UgandaGold)
                }
            }
        }
    }
}

@Composable
fun ContractInboxCard(
    booking: BookingEntity,
    onAcceptConfirm: (Boolean) -> Unit,
    onMarkCompleted: () -> Unit
) {
    val statusColor = when (booking.status) {
        "Confirmed" -> SavannahGreenLight
        "Completed" -> SavannahGreen
        "Declined" -> CrimsonA
        else -> UgandaGold
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CarbonCard),
        border = BorderStroke(1.dp, BorderSlate),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Status and reference info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STATUS: ${booking.status.uppercase()}",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Ref: ${booking.paymentReference.ifBlank { "PENDING PAY" }}",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

            // Body: Employer specifications
            Text("MATCHED CLIENT (EMPLOYER):", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(
                booking.employerName,
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CONTRACT TERM:", color = TextMuted, fontSize = 10.sp)
                    Text("Starts: ${booking.startDate}", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("Duration: ${booking.durationDays} Days", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ESTIMATED PAY (GROSS):", color = TextMuted, fontSize = 10.sp)
                    Text(
                        "${String.format("%,d", booking.totalCostUGX)} UGX",
                        color = UgandaGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Escrow: 100% Guaranteed", color = SavannahGreenLight, fontSize = 9.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("EMPLOYER TRUST CARD: APPROVED", color = SavannahGreenLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("NIN Verified: ${booking.employerNIN}", color = TextGray, fontSize = 10.sp)

            Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

            // Action triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call / Dialer button
                Button(
                    onClick = { /* Simulated Dialer activation */ },
                    colors = ButtonDefaults.buttonColors(containerColor = CarbonBg),
                    border = BorderStroke(1.dp, BorderSlate),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call Client", tint = UgandaGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call ${booking.employerPhone}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.weight(1f))

                when (booking.status) {
                    "Pending" -> {
                        // Accept / Decline buttons
                        Button(
                            onClick = { onAcceptConfirm(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonA),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Decline", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onAcceptConfirm(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = SavannahGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Accept & Confirm", fontSize = 11.sp)
                        }
                    }
                    "Confirmed" -> {
                        // Mark as Completed button
                        Button(
                            onClick = onMarkCompleted,
                            colors = ButtonDefaults.buttonColors(containerColor = UgandaGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Mark Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    "Completed" -> {
                        Box(
                            modifier = Modifier
                                .background(SavannahGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("PLACCEMENT ACTIVE & COMPLETED", color = SavannahGreenLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .background(Color.Black, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Match deactivated", color = TextGray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
