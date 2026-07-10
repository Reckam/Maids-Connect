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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.data.*
import com.example.ui.MarketplaceViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminPortal(
    viewModel: MarketplaceViewModel,
    modifier: Modifier = Modifier
) {
    val hks by viewModel.housekeepers.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val queryLogs by viewModel.queryLogs.collectAsState()
    val isDevCloudMode by viewModel.isCloudMode.collectAsState()

    var activeTab by remember { mutableStateOf("VETTING") } // "VETTING", "CLIENTS", "MATCHES", "REVIEWS", "LOGS"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 100.dp)
    ) {
        // --- Dual Engine Switch ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonCardPressed),
            border = BorderStroke(1.dp, BorderSlate),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Hybrid Storage Engine",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isDevCloudMode) "Connected: Firestore Cloud Engine" else "Offline: Local SQLite Disk (Room)",
                        color = if (isDevCloudMode) SavannahGreenLight else TextGray,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = isDevCloudMode,
                    onCheckedChange = { viewModel.toggleCloudMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = UgandaGold,
                        uncheckedThumbColor = TextGray,
                        uncheckedTrackColor = CarbonBg
                    )
                )
            }
        }

        // --- Admin Tab Header ---
        TabRow(
            selectedTabIndex = when (activeTab) {
                "VETTING" -> 0
                "CLIENTS" -> 1
                "MATCHES" -> 2
                "REVIEWS" -> 3
                else -> 0
            },
            containerColor = Color.Transparent,
            divider = { Divider(color = BorderSlate, thickness = 1.dp) }
        ) {
            val maidsCount = hks.size
            Tab(selected = activeTab == "VETTING", onClick = { activeTab = "VETTING" }) {
                Text("Maids ($maidsCount)", modifier = Modifier.padding(vertical = 12.dp), color = if (activeTab == "VETTING") UgandaGold else TextGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            val clientsCount = allUsers.count { it.role == "Client" }
            Tab(selected = activeTab == "CLIENTS", onClick = { activeTab = "CLIENTS" }) {
                Text("Clients ($clientsCount)", modifier = Modifier.padding(vertical = 12.dp), color = if (activeTab == "CLIENTS") UgandaGold else TextGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Tab(selected = activeTab == "MATCHES", onClick = { activeTab = "MATCHES" }) {
                Text("Placements (" + bookings.size + ")", modifier = Modifier.padding(vertical = 12.dp), color = if (activeTab == "MATCHES") UgandaGold else TextGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Tab(selected = activeTab == "REVIEWS", onClick = { activeTab = "REVIEWS" }) {
                Text("Reviews (" + reviews.size + ")", modifier = Modifier.padding(vertical = 12.dp), color = if (activeTab == "REVIEWS") UgandaGold else TextGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Tab Body Switcher ---
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                "VETTING" -> AdminVettingRoster(hks = hks, viewModel = viewModel)
                "CLIENTS" -> AdminClientsRoster(clients = allUsers.filter { it.role == "Client" }, viewModel = viewModel)
                "MATCHES" -> AdminMatchesSupervisor(bookings = bookings, onOverride = { b, state -> viewModel.adminOverrideBookingStatus(b, state) })
                "REVIEWS" -> AdminReviewsIntegrity(reviews = reviews, onModerate = { r, hide -> viewModel.adminModerateReview(r, hide) }, onDelete = { viewModel.adminDeleteReview(it) })
                else -> AdminVettingRoster(hks = hks, viewModel = viewModel)
            }
        }
    }
}

// Sub-Component 1: Helper Vetting
@Composable
fun AdminVettingRoster(
    hks: List<HousekeeperEntity>,
    viewModel: MarketplaceViewModel
) {
    var editingMaid by remember { mutableStateOf<HousekeeperEntity?>(null) }

    if (editingMaid != null) {
        val maid = editingMaid!!
        var nameVal by remember { mutableStateOf(maid.name) }
        var phoneVal by remember { mutableStateOf(maid.phone) }
        var bioVal by remember { mutableStateOf(maid.bio) }
        var rateVal by remember { mutableStateOf(maid.rateUGX.toString()) }
        var expVal by remember { mutableStateOf(maid.experienceYears.toString()) }
        var skillsVal by remember { mutableStateOf(maid.skills) }
        var langsVal by remember { mutableStateOf(maid.languages) }

        AlertDialog(
            onDismissRequest = { editingMaid = null },
            title = { Text("Edit Maid Profile Data", color = TextWhite, fontWeight = FontWeight.Bold) },
            containerColor = CarbonCard,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("FULL NAME & CONTACT", color = UgandaGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = nameVal,
                        onValueChange = { nameVal = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = UgandaGold,
                            unfocusedBorderColor = BorderSlate,
                            focusedContainerColor = CarbonBg,
                            unfocusedContainerColor = CarbonBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneVal,
                        onValueChange = { phoneVal = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = UgandaGold,
                            unfocusedBorderColor = BorderSlate,
                            focusedContainerColor = CarbonBg,
                            unfocusedContainerColor = CarbonBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("PROFILE INSIGHTS", color = UgandaGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = bioVal,
                        onValueChange = { bioVal = it },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = UgandaGold,
                            unfocusedBorderColor = BorderSlate,
                            focusedContainerColor = CarbonBg,
                            unfocusedContainerColor = CarbonBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rateVal,
                            onValueChange = { rateVal = it },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = UgandaGold,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = CarbonBg,
                                unfocusedContainerColor = CarbonBg
                            )
                        )
                        OutlinedTextField(
                            value = expVal,
                            onValueChange = { expVal = it },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = UgandaGold,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = CarbonBg,
                                unfocusedContainerColor = CarbonBg
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("SPECIALTIES & LANGUAGES", color = UgandaGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = skillsVal,
                        onValueChange = { skillsVal = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = UgandaGold,
                            unfocusedBorderColor = BorderSlate,
                            focusedContainerColor = CarbonBg,
                            unfocusedContainerColor = CarbonBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = langsVal,
                        onValueChange = { langsVal = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = UgandaGold,
                            unfocusedBorderColor = BorderSlate,
                            focusedContainerColor = CarbonBg,
                            unfocusedContainerColor = CarbonBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val upd = maid.copy(
                            name = nameVal,
                            phone = phoneVal,
                            bio = bioVal,
                            rateUGX = rateVal.toIntOrNull() ?: maid.rateUGX,
                            experienceYears = expVal.toIntOrNull() ?: maid.experienceYears,
                            skills = skillsVal,
                            languages = langsVal
                        )
                        viewModel.adminEditHousekeeper(upd)
                        editingMaid = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UgandaGold, contentColor = Color.Black)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMaid = null }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(hks) { hk ->
            val colorStatus = when (hk.vettingStatus) {
                "Approved" -> SavannahGreenLight
                "Suspended" -> CrimsonA
                else -> UgandaGold
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonCard),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(40.dp).background(CarbonBg, CircleShape), contentAlignment = Alignment.Center) {
                            Text(hk.avatarEmoji, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(hk.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("NIN: ${hk.nin} • EXP: ${hk.experienceYears} Yrs", color = TextGray, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .background(colorStatus.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .border(1.dp, colorStatus, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(hk.vettingStatus.uppercase(), color = colorStatus, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(hk.bio, color = TextWhite, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Rate: ${hk.rateUGX} UGX / ${hk.rateType}", color = UgandaGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Skills: ${hk.skills}", color = TextGray, fontSize = 10.sp, maxLines = 1)
                    }

                    Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                    Text("ADMIN ACTIONS:", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { editingMaid = hk },
                            colors = ButtonDefaults.buttonColors(containerColor = CarbonBg),
                            border = BorderStroke(1.dp, UgandaGold.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Edit Profiling", fontSize = 10.sp, color = UgandaGold, fontWeight = FontWeight.Bold)
                        }

                        if (hk.vettingStatus == "Pending Approval") {
                            Button(
                                onClick = { 
                                    viewModel.adminUpdateVetting(hk, "Approved") 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SavannahGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Approve Profile", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = { 
                                    val isSus = hk.vettingStatus != "Suspended"
                                    viewModel.adminSuspendHousekeeper(hk, isSus) 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (hk.vettingStatus == "Suspended") SavannahGreen else CrimsonA),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (hk.vettingStatus == "Suspended") "Reactivate" else "Suspend Profile", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { viewModel.adminDeleteHousekeeper(hk) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonA.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, CrimsonA),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text("Delete Account", fontSize = 10.sp, color = CrimsonA, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Sub-Component 2: Global Matches Supervisor
@Composable
fun AdminMatchesSupervisor(
    bookings: List<BookingEntity>,
    onOverride: (BookingEntity, String) -> Unit
) {
    // Computed indicators
    val reconciledMatchFee = bookings.size * 20000

    Column {
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonBg),
            border = BorderStroke(1.dp, BorderSlate),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Matchmaking Escrow Reconciliation", color = UgandaGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Placement Fees Reconciled:", color = TextWhite, fontSize = 14.sp)
                    Text("${String.format("%,d", reconciledMatchFee)} UGX", color = SavannahGreenLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Text("Each match generates a pre-paid 20,000 UGX system escrow placement fee.", color = TextMuted, fontSize = 10.sp)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(bookings) { b ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonCard),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Helper: ${b.housekeeperName}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Employer: ${b.employerName} (${b.employerPhone})", color = TextGray, fontSize = 11.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .background(UgandaGold.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .border(1.dp, UgandaGold, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(b.status.uppercase(), color = UgandaGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("KYC Reference NIN Check: ${b.employerNIN}", color = SavannahGreenLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Escrow Reference: ${b.paymentReference}", color = TextMuted, fontSize = 10.sp)

                        Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CONTRACT OVERRIDES: ", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = { onOverride(b, "Confirmed") },
                                enabled = b.status != "Confirmed",
                                colors = ButtonDefaults.buttonColors(containerColor = SavannahGreen),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Force Confirmed", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onOverride(b, "Completed") },
                                enabled = b.status != "Completed",
                                colors = ButtonDefaults.buttonColors(containerColor = UgandaGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Force Done", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onOverride(b, "Declined") },
                                enabled = b.status != "Declined",
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonA),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Deactivate", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sub-Component 3: Review Moderation Dashboard
@Composable
fun AdminReviewsIntegrity(
    reviews: List<ReviewEntity>,
    onModerate: (ReviewEntity, Boolean) -> Unit,
    onDelete: (ReviewEntity) -> Unit
) {
    if (reviews.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No client comments recorded on database.", color = TextGray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(reviews) { r ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (r.isHidden) CarbonBg else CarbonCard),
                    border = BorderStroke(1.dp, if (r.isHidden) CrimsonA.copy(alpha = 0.5f) else BorderSlate)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Review Author: ${r.employerName}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                                    (1..5).forEach { star ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (star <= r.rating) UgandaGold else TextGray,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            if (r.isHidden) {
                                Box(
                                    modifier = Modifier
                                        .background(CrimsonA.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .border(1.dp, CrimsonA, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("MUTED / HIDDEN", color = CrimsonA, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            "\"${r.comment}\"",
                            color = if (r.isHidden) TextMuted else TextGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("MODERATION DECK: ", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

                            Button(
                                onClick = { onModerate(r, !r.isHidden) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (r.isHidden) SavannahGreen else CarbonBg),
                                shape = RoundedCornerShape(6.dp),
                                border = if (r.isHidden) null else BorderStroke(1.dp, BorderSlate),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(if (r.isHidden) "Restore Comment" else "Hide & Mute", fontSize = 9.sp, color = TextWhite)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { onDelete(r) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(CrimsonA.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Review", tint = CrimsonA, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sub-Component 4: Cyberpunk SQL terminal
@Composable
fun AdminTerminalConsole(
    logs: List<String>
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .border(1.dp, BorderSlate)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color.Red, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color.Yellow, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color.Green, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "KAZIMATCH_DB_SPLIT_CONSOLE.sh",
                color = TextGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .border(1.dp, BorderSlate)
                .padding(10.dp)
        ) {
            if (logs.isEmpty()) {
                Text(
                    "Terminal active. Waiting for Room DB queries or Firebase sync operations to process...",
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            } else {
                LazyColumn {
                    items(logs) { log ->
                        val logColor = when {
                            log.contains("CLOUD-FIRESTORE") || log.contains("CLOUD SYNC") -> Color(0xFF64B5F6)
                            log.contains("LOCAL-SQLITE") || log.contains("ROOM") -> Color(0xFF81C784)
                            log.contains("DB SEED") -> Color(0xFFFFD54F)
                            else -> Color.Green
                        }
                        Text(
                            text = log,
                            color = logColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminClientsRoster(
    clients: List<UserEntity>,
    viewModel: MarketplaceViewModel
) {
    if (clients.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No registered clients found in the database.", color = TextGray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(clients) { client ->
                val clientStatusColor = if (client.isSuspended) CrimsonA else if (!client.isVerified) UgandaGold else SavannahGreenLight
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonCard),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(UgandaGold.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = UgandaGold, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(client.fullName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("NIN Verified: ${client.nin}", color = SavannahGreenLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .background(clientStatusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .border(1.dp, clientStatusColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (client.isSuspended) "SUSPENDED" else if (!client.isVerified) "PENDING APPROVAL" else "ACTIVE", 
                                    color = clientStatusColor, 
                                    fontSize = 8.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("CONTACT DETAILS", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("Phone: ${client.phoneNumber}", color = TextWhite, fontSize = 11.sp)
                                Text("Email: ${client.emailAddress}", color = TextGray, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("LOCATION", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("${client.village}, ${client.subcounty}", color = TextWhite, fontSize = 11.sp)
                                Text(client.district, color = TextGray, fontSize = 11.sp)
                            }
                        }

                        Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                        Text("ADMIN ACTIONS:", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!client.isVerified) {
                                Button(
                                    onClick = { viewModel.adminVerifyUser(client, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SavannahGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("Approve Client", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.adminSuspendUser(client, !client.isSuspended) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (client.isSuspended) SavannahGreen else CrimsonA),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text(if (client.isSuspended) "Unsuspend" else "Suspend Client", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { viewModel.adminDeleteUser(client) },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonA.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, CrimsonA),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Delete Account", fontSize = 10.sp, color = CrimsonA, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

