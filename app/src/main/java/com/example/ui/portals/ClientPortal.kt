package com.example.ui.portals

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import com.example.data.*
import com.example.ui.MarketplaceViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalUriHandler
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ClientPortal(
    viewModel: MarketplaceViewModel,
    modifier: Modifier = Modifier
) {
    val hks by viewModel.filteredHousekeepers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSkill by viewModel.selectedSkill.collectAsState()
    val selectedDist by viewModel.selectedDistrict.collectAsState()
    val selectedSub by viewModel.selectedSubcounty.collectAsState()
    val selectedVil by viewModel.selectedVillage.collectAsState()
    val rateTypeFilter by viewModel.rateTypeFilter.collectAsState()
    val maxBudgetFilter by viewModel.maxBudgetFilter.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showFilterDrawer by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("FIND_MAIDS") } // "FIND_MAIDS", "MY_BOOKINGS"
    val geoMap = GeographicHierarchy()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        TabRow(
            selectedTabIndex = if (activeTab == "FIND_MAIDS") 0 else 1,
            containerColor = Color.Transparent,
            contentColor = UgandaGold,
            divider = { Divider(color = BorderSlate, thickness = 1.dp) }
        ) {
            Tab(
                selected = activeTab == "FIND_MAIDS",
                onClick = { activeTab = "FIND_MAIDS" },
                text = { Text("Find Housekeepers", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (activeTab == "FIND_MAIDS") UgandaGold else TextGray) }
            )
            Tab(
                selected = activeTab == "MY_BOOKINGS",
                onClick = { activeTab = "MY_BOOKINGS" },
                text = { Text("My Bookings", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (activeTab == "MY_BOOKINGS") UgandaGold else TextGray) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeTab == "FIND_MAIDS") {
            // --- Search bar & Filter trigger ---
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by helper name or bio...", color = TextGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = UgandaGold) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextGray)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UgandaGold,
                    unfocusedBorderColor = BorderSlate,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = CarbonCard,
                    unfocusedContainerColor = CarbonCard
                ),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Filter Badge Indicator count
            val filterCount = (if (selectedSkill != "All") 1 else 0) +
                    (if (selectedDist != "All") 1 else 0) +
                    (if (selectedSub != "All") 1 else 0) +
                    (if (selectedVil != "All") 1 else 0) +
                    (if (rateTypeFilter != "All") 1 else 0)

            Box {
                Button(
                    onClick = { showFilterDrawer = !showFilterDrawer },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filterCount > 0) UgandaGold else CarbonCard,
                        contentColor = if (filterCount > 0) Color.Black else TextWhite
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Filter")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Filters", fontWeight = FontWeight.Bold)
                    if (filterCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.Black, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = filterCount.toString(),
                                color = UgandaGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- Collapsing Filter Drawer ---
        AnimatedVisibility(
            visible = showFilterDrawer,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = CarbonCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Target Helpers by local criteria",
                        color = UgandaGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // District dropdown
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("District:", color = TextWhite, modifier = Modifier.width(80.dp), fontWeight = FontWeight.Medium)
                        Box(modifier = Modifier.weight(1f)) {
                            var expandedDist by remember { mutableStateOf(false) }
                            Text(
                                text = selectedDist,
                                color = TextWhite,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CarbonBg, RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                                    .clickable { expandedDist = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                            DropdownMenu(
                                expanded = expandedDist,
                                onDismissRequest = { expandedDist = false },
                                modifier = Modifier.background(CarbonCard).border(1.dp, BorderSlate)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Districts", color = TextWhite) },
                                    onClick = { viewModel.setDistrict("All"); expandedDist = false }
                                )
                                geoMap.districts.forEach { dist ->
                                    DropdownMenuItem(
                                        text = { Text(dist, color = TextWhite) },
                                        onClick = { viewModel.setDistrict(dist); expandedDist = false }
                                    )
                                }
                            }
                        }
                    }

                    // County/Subcounty Dropdown (Only enabled if District selected)
                    if (selectedDist != "All") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Subcounty:", color = TextWhite, modifier = Modifier.width(80.dp), fontWeight = FontWeight.Medium)
                            Box(modifier = Modifier.weight(1f)) {
                                var expandedSub by remember { mutableStateOf(false) }
                                Text(
                                    text = selectedSub,
                                    color = TextWhite,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CarbonBg, RoundedCornerShape(8.dp))
                                        .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                                        .clickable { expandedSub = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                                DropdownMenu(
                                    expanded = expandedSub,
                                    onDismissRequest = { expandedSub = false },
                                    modifier = Modifier.background(CarbonCard).border(1.dp, BorderSlate)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Subcounties", color = TextWhite) },
                                        onClick = { viewModel.setSubcounty("All"); expandedSub = false }
                                    )
                                    val subList = geoMap.subcounties[selectedDist] ?: emptyList()
                                    subList.forEach { sub ->
                                        DropdownMenuItem(
                                            text = { Text(sub, color = TextWhite) },
                                            onClick = { viewModel.setSubcounty(sub); expandedSub = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Village Dropdown (Only enabled if Subcounty selected)
                    if (selectedSub != "All") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Village:", color = TextWhite, modifier = Modifier.width(80.dp), fontWeight = FontWeight.Medium)
                            Box(modifier = Modifier.weight(1f)) {
                                var expandedVil by remember { mutableStateOf(false) }
                                Text(
                                    text = selectedVil,
                                    color = TextWhite,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CarbonBg, RoundedCornerShape(8.dp))
                                        .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                                        .clickable { expandedVil = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                                DropdownMenu(
                                    expanded = expandedVil,
                                    onDismissRequest = { expandedVil = false },
                                    modifier = Modifier.background(CarbonCard).border(1.dp, BorderSlate)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Villages", color = TextWhite) },
                                        onClick = { viewModel.setVillage("All"); expandedVil = false }
                                    )
                                    val vilList = geoMap.villages[selectedSub] ?: emptyList()
                                    vilList.forEach { vil ->
                                        DropdownMenuItem(
                                            text = { Text(vil, color = TextWhite) },
                                            onClick = { viewModel.setVillage(vil); expandedVil = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Specialized skill filter
                    Text("Specialized Helper Skill:", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Infant Nursing", "Deep Cleaning", "Laundry", "Culinary Arts", "Pet Care", "Elder Care").forEach { skill ->
                            FilterChip(
                                selected = selectedSkill == skill,
                                onClick = { viewModel.setSkill(skill) },
                                label = { Text(skill) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = UgandaGold,
                                    selectedLabelColor = Color.Black,
                                    containerColor = CarbonBg,
                                    labelColor = TextWhite
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rate type filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rate Format:", color = TextWhite, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("All", "Daily", "Monthly").forEach { type ->
                                Button(
                                    onClick = { viewModel.setRateType(type) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (rateTypeFilter == type) UgandaGold else CarbonBg,
                                        contentColor = if (rateTypeFilter == type) Color.Black else TextWhite
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(type, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Budget Slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Max Rate: ", color = TextWhite, modifier = Modifier.weight(1f))
                        Text(
                            text = "${String.format("%,d", maxBudgetFilter)} UGX",
                            color = UgandaGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = maxBudgetFilter.toFloat(),
                        onValueChange = { viewModel.setMaxBudget(it.toInt()) },
                        valueRange = if (rateTypeFilter == "Daily") 10000f..50000f else 100000f..800000f,
                        colors = SliderDefaults.colors(
                            thumbColor = UgandaGold,
                            activeTrackColor = UgandaGold,
                            inactiveTrackColor = BorderSlate
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reset buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { viewModel.clearFilters() }) {
                            Text("Reset Filters", color = CrimsonA)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showFilterDrawer = false },
                            colors = ButtonDefaults.buttonColors(containerColor = UgandaGold, contentColor = Color.Black)
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Housekeeper Roster Grid / List ---
        if (hks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Empty",
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No housekeepers match current coordinates", color = TextGray, fontWeight = FontWeight.SemiBold)
                    Text("Try clearing some geographical or skill filters", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(hks, key = { it.id }) { hk ->
                    HousekeeperBentoCard(hk = hk, onHireClicked = { viewModel.startBookingFlow(hk) })
                }
            }
        }
    } else {
        // --- My Bookings Sub-Portal for Client ---
        val myBookings = bookings.filter {
            currentUser != null && (
                it.employerNIN.trim().uppercase() == currentUser!!.nin.trim().uppercase() ||
                it.employerPhone.trim() == currentUser!!.phoneNumber.trim() ||
                it.employerName.trim().equals(currentUser!!.fullName.trim(), ignoreCase = true)
            )
        }

        if (myBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "No Bookings",
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No active matches or maid bookings", color = TextGray, fontWeight = FontWeight.SemiBold)
                    Text("Hire a professional keeper from the catalog tab", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(myBookings) { booking ->
                    val statusColor = when (booking.status) {
                        "Confirmed" -> SavannahGreenLight
                        "Completed" -> SavannahGreenLight
                        "Declined" -> CrimsonA
                        else -> UgandaGold
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarbonCard),
                        border = BorderStroke(1.dp, BorderSlate),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(UgandaGold.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("👩🏾", fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(booking.housekeeperName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Start Date: ${booking.startDate}", color = TextGray, fontSize = 11.sp)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .border(1.dp, statusColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(booking.status.uppercase(), color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text("CONTRACT SERVICE", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("${booking.durationDays} Days Duration", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("TOTAL ESCROW FEE", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("${booking.totalCostUGX} UGX", color = UgandaGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (booking.status == "Confirmed" || booking.status == "Completed") {
                                Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            // Optional rate action
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CarbonBg),
                                        border = BorderStroke(1.dp, UgandaGold),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = UgandaGold, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Rate Maid", color = TextWhite, fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.selectPortal("FAQs")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CarbonBg),
                                        border = BorderStroke(1.dp, CrimsonA),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonA, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("File Dispute", color = TextWhite, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }

    // --- Booking Gated Matchmaker Escrow Dialog ---
    val bookingHK by viewModel.selectedHKForBooking.collectAsState()
    if (bookingHK != null) {
        Dialog(onDismissRequest = { viewModel.cancelOrCloseBooking() }) {
            Surface(
                color = CarbonCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                BookingWorkflowWizard(viewModel = viewModel, hk = bookingHK!!)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HousekeeperBentoCard(
    hk: HousekeeperEntity,
    onHireClicked: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CarbonCard),
        border = BorderStroke(1.dp, BorderSlate),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Avatar, Name, Location, Average Rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CarbonBg, CircleShape)
                        .border(1.dp, BorderSlate, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (hk.profileImageUri != null) {
                        if (hk.profileImageUri == "preset_1") {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.img_maid_profile_one),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (hk.profileImageUri == "preset_2") {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.img_maid_profile_two),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = rememberAsyncImagePainter(model = hk.profileImageUri),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Text(hk.avatarEmoji, fontSize = 28.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = hk.name,
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Vetted state tag
                        if (hk.vettingStatus == "Approved") {
                            Box(
                                modifier = Modifier
                                    .background(SavannahGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .border(1.dp, SavannahGreen, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Vetted", tint = SavannahGreenLight, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("APPROVED", color = SavannahGreenLight, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Text(
                        text = "📍 ${hk.district} • ${hk.subcounty} • ${hk.village}",
                        color = UgandaGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "Rating", tint = UgandaGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", hk.rating),
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${hk.reviewCount} compliance reviews",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bio
            Text(
                text = hk.bio,
                color = TextGray,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Custom Badges - Spoken Languages & Skills
            Text("LANGUAGES SPOKEN:", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                hk.languages.split(",").forEach { lang ->
                    Box(
                        modifier = Modifier
                            .background(CarbonBg, RoundedCornerShape(6.dp))
                            .border(1.dp, BorderSlate, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(lang.trim(), color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text("SPECIALIZATION SKILLS:", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                hk.skills.split(",").forEach { skill ->
                    Box(
                        modifier = Modifier
                            .background(UgandaGold.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .border(1.dp, UgandaGold.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(skill.trim(), color = UgandaGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

            // Footer info: rate price and "Initiate Match"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("REQUESTED RATE:", color = TextGray, fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${String.format("%,d", hk.rateUGX)} UGX",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/" + hk.rateType.lowercase(),
                            color = TextGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                        )
                    }
                    Text(
                        text = "• ${hk.experienceYears} Years Verified Exp",
                        color = SavannahGreenLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onHireClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = UgandaGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Hiring Match", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Initiate placement", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BookingWorkflowWizard(
    viewModel: MarketplaceViewModel,
    hk: HousekeeperEntity
) {
    val step by viewModel.bookingStep.collectAsState()
    val payRef by viewModel.placementReference.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    // Input States
    val empName by viewModel.employerName.collectAsState()
    val empPhone by viewModel.employerPhone.collectAsState()
    val empNIN by viewModel.employerNIN.collectAsState()
    val bStartDate by viewModel.bookingStartDate.collectAsState()
    val bDurationDays by viewModel.bookingDurationDays.collectAsState()

    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var showInAppWebView by remember { mutableStateOf(false) }
    var livepayMethod by remember { mutableStateOf("MOMO") } // "MOMO", "CARD", "LINK"
    var livepayPhone by remember { mutableStateOf("") }
    var livepayProvider by remember { mutableStateOf("MTN") } // "MTN", "Airtel"
    var livepayCardNo by remember { mutableStateOf("") }
    var livepayExpiry by remember { mutableStateOf("") }
    var livepayCVV by remember { mutableStateOf("") }
    var livepayStatus by remember { mutableStateOf("IDLE") } // "IDLE", "PROCESSING", "SUCCESS"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step progress header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Match Wizard: ${hk.name}",
                color = UgandaGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.cancelOrCloseBooking() }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray)
            }
        }

        // Custom Step Progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("1. VETTING GATEWAY", "2. SIMULATE ESCROW", "3. SUCCESS").forEachIndexed { index, title ->
                val active = step == (index + 1)
                val completed = step > (index + 1)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = if (active || completed) UgandaGold else CarbonBg,
                                shape = CircleShape
                            )
                            .border(
                                1.dp,
                                if (active || completed) UgandaGold else BorderSlate,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (completed) {
                            Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.Black, modifier = Modifier.size(14.dp))
                        } else {
                            Text(
                                (index + 1).toString(),
                                color = if (active) Color.Black else TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        title,
                        fontSize = 8.sp,
                        color = if (active) UgandaGold else TextGray,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(bottom = 16.dp))

        if (isSyncing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = UgandaGold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Escrow gateway verifying documents...", color = TextGray, fontSize = 12.sp)
                }
            }
        } else {
            when (step) {
                1 -> {
                    // STEP 1: Employer Name, Phone, and VETTING NIN
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Employer KYC Vetting (Required for safety)",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "To protect domestic assistants' physical safety before connecting, we require verified National ID credentials from all matching householders.",
                            color = TextGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        OutlinedTextField(
                            value = empName,
                            onValueChange = { viewModel.employerName.value = it },
                            label = { Text("Your Complete Legal Name", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = UgandaGold,
                                unfocusedBorderColor = BorderSlate,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = empPhone,
                            onValueChange = { viewModel.employerPhone.value = it },
                            label = { Text("Your Contact Mobile (+256...)", color = TextGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = UgandaGold,
                                unfocusedBorderColor = BorderSlate,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = empNIN,
                            onValueChange = { viewModel.employerNIN.value = it.uppercase() },
                            label = { Text("National ID Number (NIN - 14 Chars)", color = TextGray) },
                            placeholder = { Text("e.g. CM920384729UGA", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = UgandaGold,
                                unfocusedBorderColor = BorderSlate,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            supportingText = {
                                Text(
                                    "Must be exactly 14 characters. Entered: ${empNIN.length}/14",
                                    color = if (empNIN.length == 14) SavannahGreenLight else TextGray
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = bStartDate,
                                onValueChange = { viewModel.bookingStartDate.value = it },
                                label = { Text("Start Date", color = TextGray) },
                                placeholder = { Text("YYYY-MM-DD", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = UgandaGold,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = bDurationDays,
                                onValueChange = { viewModel.bookingDurationDays.value = it },
                                label = { Text("Duration (Days)", color = TextGray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = UgandaGold,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.proceedToPayment() },
                            enabled = empName.isNotBlank() && empPhone.isNotBlank() && empNIN.length == 14,
                            colors = ButtonDefaults.buttonColors(containerColor = UgandaGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit KYC & Proceed to Placement", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                2 -> {
                    // STEP 2: SUBSCRIPTION PLACEMENT SERVICE FEE (INTEGRATED LIVEPAY SECURE PAYMENT)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // LivePay Header Brand Ribbon
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(UgandaGold.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .border(1.dp, UgandaGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(UgandaGold, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Send,
                                            contentDescription = "LivePay Logo",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "livepay",
                                            color = TextWhite,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "SECURE BILLING GATEWAY",
                                            color = TextGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(SavannahGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Secure",
                                            tint = SavannahGreenLight,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("ESCROW PCI-DSS", color = SavannahGreenLight, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (showInAppWebView) {
                            var webViewLoading by remember { mutableStateOf(true) }
                            var hasLoadError by remember { mutableStateOf(false) }

                            // Render Native securely embedded WebView!
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CarbonCard, RoundedCornerShape(12.dp))
                                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "SSL Encrypted WebSession",
                                            tint = SavannahGreenLight,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "https://livepay.me/pay...",
                                            color = TextGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "External Browser ↗",
                                            color = UgandaGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                uriHandler.openUri("https://livepay.me/pay?c=9d0129a8f6b618fac57b1d95a2d5182d")
                                            }
                                        )
                                    }
                                    IconButton(
                                        onClick = { showInAppWebView = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close WebView",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(420.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AndroidView(
                                        factory = { context ->
                                            WebView(context).apply {
                                                layoutParams = android.view.ViewGroup.LayoutParams(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                                webViewClient = object : WebViewClient() {
                                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                                        super.onPageStarted(view, url, favicon)
                                                        webViewLoading = true
                                                    }
                                                    override fun onPageFinished(view: WebView?, url: String?) {
                                                        super.onPageFinished(view, url)
                                                        webViewLoading = false
                                                    }
                                                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                                        super.onReceivedError(view, errorCode, description, failingUrl)
                                                        hasLoadError = true
                                                        webViewLoading = false
                                                    }
                                                }
                                                settings.javaScriptEnabled = true
                                                settings.domStorageEnabled = true
                                                settings.useWideViewPort = true
                                                settings.loadWithOverviewMode = true
                                                loadUrl("https://livepay.me/pay?c=9d0129a8f6b618fac57b1d95a2d5182d")
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    if (webViewLoading) {
                                        CircularProgressIndicator(
                                            color = UgandaGold,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    if (hasLoadError) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(CarbonBg)
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                "Payment Gateway Offline",
                                                color = TextWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "The security checkout failed to load inline.",
                                                color = TextGray,
                                                fontSize = 11.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    uriHandler.openUri("https://livepay.me/pay?c=9d0129a8f6b618fac57b1d95a2d5182d")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = UgandaGold, contentColor = Color.Black),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Open LivePay in Chrome/Safari ↗", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            livepayStatus = "PROCESSING"
                                            showInAppWebView = false
                                            kotlinx.coroutines.delay(2000)
                                            livepayStatus = "SUCCESS"
                                            kotlinx.coroutines.delay(1000)
                                            viewModel.completeBookingAndHiring()
                                            livepayStatus = "IDLE"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SavannahGreen, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("I Completed My Payment ✅", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        } else if (livepayStatus == "PROCESSING") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = UgandaGold,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Processing secure checkout via livepay...",
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Please check your device for a 3D-secure pin or mobile payment prompt.",
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (livepayStatus == "SUCCESS") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(SavannahGreen.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Success",
                                        tint = SavannahGreenLight,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "livepay Authorized!",
                                    color = SavannahGreenLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Match successfully secured. Escrow transaction ID recorded.",
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // IDLE landing screen: Just one button that opens that link inside the app itself!
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CarbonCard, RoundedCornerShape(12.dp))
                                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Escrow Placement Fee",
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )

                                Text(
                                    text = "To secure matching coordinators and unlock direct phone numbers of candidate, a one-time 20,000 UGX safety escrow is required.",
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp
                                 )

                                 Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CarbonBg, RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Matching Fee:", color = TextGray, fontSize = 11.sp)
                                    Text("20,000 UGX", color = UgandaGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { showInAppWebView = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = UgandaGold, contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Pay Subscription Fee", fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }

                                TextButton(onClick = { viewModel.startBookingFlow(hk) }) {
                                    Text("Back to KYC Info", color = TextGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // STEP 3: MATCH SUCCESS & COORDINATES UNLOCKED
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(SavannahGreen.copy(alpha = 0.2f), CircleShape)
                                .border(2.dp, SavannahGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Unlocked", tint = SavannahGreenLight, modifier = Modifier.size(36.dp))
                        }

                        Text(
                            "Placement Escrow Unlocked!",
                            color = SavannahGreenLight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Match successfully simulated! The placement request has been pushed live to the housekeeper/maid's contracts inbox. You can call them directly now.",
                            color = TextGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        // Locked Coordinate details
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CarbonBg),
                            border = BorderStroke(1.dp, BorderSlate),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Direct Helper Connection",
                                    color = UgandaGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    hk.name,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "📞 Phone: ${hk.phone}",
                                    color = TextWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.background(BorderSlate, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Location: ${hk.village}, ${hk.subcounty}",
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // REVIEW DEMO: Write a review generator
                        var ratingInput by remember { mutableStateOf(5) }
                        var commentInput by remember { mutableStateOf("") }
                        var reviewSubmitted by remember { mutableStateOf(false) }

                        if (!reviewSubmitted) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CarbonCardPressed),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Simulate review submission:",
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        (1..5).forEach { star ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "$star Stars",
                                                tint = if (star <= ratingInput) UgandaGold else TextGray,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clickable { ratingInput = star }
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = commentInput,
                                        onValueChange = { commentInput = it },
                                        placeholder = { Text("E.g. Juliet was excellent with the infant coaching!", color = TextGray, fontSize = 11.sp) },
                                        textStyle = TextStyle(fontSize = 12.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = UgandaGold,
                                            unfocusedBorderColor = BorderSlate,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(60.dp)
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.submitEmployerReview(
                                                hkId = hk.id,
                                                bookingId = 200, // Simulated mock booking ID
                                                author = empName,
                                                rating = ratingInput,
                                                comment = commentInput.ifBlank { "Excellent matched placement service." }
                                            )
                                            reviewSubmitted = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = UgandaGold, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) {
                                        Text("Submit Star Review", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Text(
                                "✅ Star reviews uploaded dynamically to Room! Check operator console.",
                                color = SavannahGreenLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Button(
                            onClick = { viewModel.cancelOrCloseBooking() },
                            colors = ButtonDefaults.buttonColors(containerColor = UgandaGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dismiss & Return to Browser", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
