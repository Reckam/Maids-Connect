package com.example.ui.portals

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalUriHandler
import java.net.URLEncoder
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.data.*
import com.example.ui.FAQItem
import com.example.ui.MarketplaceViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FaqAndSupportPortal(
    viewModel: MarketplaceViewModel,
    modifier: Modifier = Modifier
) {
    val faqSearchQuery by viewModel.faqSearch.collectAsState()
    val filteredFAQs by viewModel.filteredFAQs.collectAsState()
    val tickets by viewModel.tickets.collectAsState()
    val currentRole by viewModel.activePortal.collectAsState()
    val uriHandler = LocalUriHandler.current

    var showTicketForm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        // --- 1. Searchable FAQ Header ---
        Text(
            "Ugandan Staffing FAQ & Regulatory Guide",
            color = UgandaGold,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        Text(
            "Self-help directory compiled in accordance with Uganda's Ministry of Gender, Labour and Social Development Guidelines.",
            color = TextGray,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = faqSearchQuery,
            onValueChange = { viewModel.faqSearch.value = it },
            placeholder = { Text("Search help topics...", color = TextGray) },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = "FAQ search", tint = UgandaGold) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = UgandaGold,
                unfocusedBorderColor = BorderSlate,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = CarbonCard,
                unfocusedContainerColor = CarbonCard
            ),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Expanding FAQ list
        filteredFAQs.forEach { item ->
            FAQAccordionItem(item)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))

        // --- 2. Resolution Center (Support ticketing system) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "MaidsConnect Resolution Center",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Submit feedback or dispute active matches",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = { showTicketForm = !showTicketForm },
                colors = ButtonDefaults.buttonColors(containerColor = UgandaGold, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(if (showTicketForm) Icons.Default.Close else Icons.Default.Add, contentDescription = "Add ticket", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (showTicketForm) "Cancel" else "File Escrow Dispute", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Ticket Escalation Input Form
        AnimatedVisibility(
            visible = showTicketForm,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonCard),
                border = BorderStroke(1.dp, BorderSlate),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "File Dispute Ticket",
                        color = UgandaGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    var ticketTitle by remember { mutableStateOf("") }
                    var ticketDesc by remember { mutableStateOf("") }
                    var selectedCategory by remember { mutableStateOf("Booking") } // "Booking", "Payment", "Domestic Issue"

                    OutlinedTextField(
                        value = ticketTitle,
                        onValueChange = { ticketTitle = it },
                        label = { Text("Title", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = UgandaGold, unfocusedBorderColor = BorderSlate),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("CATEGORY:", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Booking", "Payment", "Domestic Issue").forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = UgandaGold,
                                    selectedLabelColor = Color.Black,
                                    containerColor = CarbonBg,
                                    labelColor = TextWhite
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = ticketDesc,
                        onValueChange = { ticketDesc = it },
                        label = { Text("Describe the dispute in detail...", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = UgandaGold, unfocusedBorderColor = BorderSlate),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (ticketTitle.isNotBlank() && ticketDesc.isNotBlank()) {
                                viewModel.submitSupportTicket(
                                    title = ticketTitle,
                                    category = selectedCategory,
                                    desc = ticketDesc,
                                    role = currentRole
                                )
                                try {
                                    val formattedMessage = "MaidsConnect Ticket Escalation:\n" +
                                            "Category: $selectedCategory\n" +
                                            "Title: $ticketTitle\n" +
                                            "Details: $ticketDesc"
                                    val encodedMessage = URLEncoder.encode(formattedMessage, "UTF-8")
                                    val whatsappUrl = "https://api.whatsapp.com/send?phone=256750018455&text=$encodedMessage"
                                    uriHandler.openUri(whatsappUrl)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                showTicketForm = false
                                ticketTitle = ""
                                ticketDesc = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SavannahGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Submit")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Escalate Ticket to Operators", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List of filed tickets in local Room Database
        Text(
            "Filed Support History (${tickets.size})",
            color = TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (tickets.isEmpty()) {
            Text(
                "You have no historic disputes or filed support tickets.",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        } else {
            tickets.forEach { ticket ->
                TicketListItem(ticket = ticket, onResolve = { viewModel.changeTicketStatus(ticket, "Resolved") })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun FAQAccordionItem(item: FAQItem) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = CarbonCard,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderSlate),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.question,
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.0f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = UgandaGold
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))
                    Text(
                        text = item.answer,
                        color = TextGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TicketListItem(
    ticket: SupportTicketEntity,
    onResolve: () -> Unit
) {
    val categoryColor = when (ticket.category) {
        "Payment" -> UgandaGold
        "Booking" -> SavannahGreenLight
        else -> Color(0xFF64B5F6)
    }

    val statusColor = when (ticket.status) {
        "Resolved" -> SavannahGreen
        "InProgress" -> UgandaGold
        else -> CrimsonA
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CarbonCard),
        border = BorderStroke(1.dp, BorderSlate),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(1.dp, categoryColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(ticket.category.uppercase(), color = categoryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Role: ${ticket.userRole}", color = TextMuted, fontSize = 9.sp)
                }

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(ticket.status.uppercase(), color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(ticket.title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(ticket.description, color = TextGray, fontSize = 12.sp, lineHeight = 15.sp)

            // Resolve button if it's not already resolved
            if (ticket.status != "Resolved") {
                Divider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onResolve,
                        colors = ButtonDefaults.buttonColors(containerColor = SavannahGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Simulate Resolve Ticket", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
