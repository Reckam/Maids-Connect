package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.MarketplaceRepository
import com.example.ui.MarketplaceViewModel
import com.example.ui.MarketplaceViewModelFactory
import com.example.ui.portals.AdminPortal
import com.example.ui.portals.AuthPortal
import com.example.ui.portals.ClientPortal
import com.example.ui.portals.FaqAndSupportPortal
import com.example.ui.portals.HousekeeperPortal
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize Room Database inside the safe Compose context
                val context = LocalContext.current.applicationContext as Application
                val db = remember { AppDatabase.getDatabase(context) }
                val repository = remember {
                    MarketplaceRepository(
                        db.housekeeperDao(),
                        db.bookingDao(),
                        db.reviewDao(),
                        db.supportTicketDao(),
                        db.userDao()
                    )
                }
                
                // Retrieve ViewModel using custom Factory
                val viewModel: MarketplaceViewModel = viewModel(
                    factory = MarketplaceViewModelFactory(context, repository)
                )

                MainScaffoldFrame(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffoldFrame(viewModel: MarketplaceViewModel) {
    val activePortal by viewModel.activePortal.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isCloudMode by viewModel.isCloudMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Land user on their appropriate landing portal on role detection
    LaunchedEffect(currentUser) {
        val role = currentUser?.role ?: "Client"
        when (role) {
            "Housekeeper" -> {
                if (activePortal != "Housekeeper" && activePortal != "FAQs") {
                    viewModel.selectPortal("Housekeeper")
                }
            }
            "Admin" -> {
                if (activePortal != "Admin" && activePortal != "FAQs") {
                    viewModel.selectPortal("Admin")
                }
            }
            else -> {
                if (activePortal != "Client" && activePortal != "FAQs") {
                    viewModel.selectPortal("Client")
                }
            }
        }
    }

    if (currentUser == null) {
        // Safe, beautiful gateway container for login and register
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CarbonBg)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            AuthPortal(
                viewModel = viewModel,
                onAuthSuccess = {}
            )
        }
        return
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // High contrast branding chip
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(UgandaGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = "Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "MaidsConnect",
                                    color = TextWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ugandan Staffing Ecosystem",
                                    color = UgandaGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick sign-out button
                        IconButton(
                            onClick = { viewModel.logoutUser() },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("btn_logout")
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = "Sign Out Session",
                                tint = TextWhite
                            )
                        }

                        // Small cloud connectivity badge
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .background(
                                    if (isCloudMode) SavannahGreen.copy(alpha = 0.2f) else BorderSlate,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isCloudMode) SavannahGreenLight else TextMuted,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isCloudMode) SavannahGreenLight else TextGray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCloudMode) "CLOUD" else "LOCAL",
                                    color = if (isCloudMode) SavannahGreenLight else TextGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CarbonBg
                    )
                )

                // Top syncing loader strip
                AnimatedVisibility(
                    visible = isSyncing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        color = UgandaGold,
                        trackColor = CarbonBg,
                        modifier = Modifier.fillMaxWidth().height(2.dp)
                    )
                }
            }
        },
        bottomBar = {
            val userRole = currentUser?.role ?: "Client"
            NavigationBar(
                containerColor = CarbonCard,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Client Workspace
                if (userRole == "Client") {
                    NavigationBarItem(
                        selected = activePortal == "Client",
                        onClick = { viewModel.selectPortal("Client") },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Client Portal") },
                        label = { Text("Client", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = UgandaGold,
                            selectedTextColor = UgandaGold,
                            indicatorColor = UgandaGoldLight,
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextGray
                        )
                    )
                }
 
                // Housekeeper Workspace
                if (userRole == "Housekeeper") {
                    NavigationBarItem(
                        selected = activePortal == "Housekeeper",
                        onClick = { viewModel.selectPortal("Housekeeper") },
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Maid Account") },
                        label = { Text("Helper Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = UgandaGold,
                            selectedTextColor = UgandaGold,
                            indicatorColor = UgandaGoldLight,
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextGray
                        )
                    )
                }
 
                // Support Desk
                NavigationBarItem(
                    selected = activePortal == "FAQs",
                    onClick = { viewModel.selectPortal("FAQs") },
                    icon = { Icon(Icons.Default.Info, contentDescription = "FAQ Desk") },
                    label = { Text("Support", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = UgandaGold,
                        selectedTextColor = UgandaGold,
                        indicatorColor = UgandaGoldLight,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )
 
                // Admin Console
                if (userRole == "Admin") {
                    NavigationBarItem(
                        selected = activePortal == "Admin",
                        onClick = { viewModel.selectPortal("Admin") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Admin Dashboard") },
                        label = { Text("Admin Desk", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = UgandaGold,
                            selectedTextColor = UgandaGold,
                            indicatorColor = UgandaGoldLight,
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextGray
                        )
                    )
                }
            }
        },
        containerColor = CarbonBg,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Portal views transition animations
            AnimatedContent(
                targetState = activePortal,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "PortalAnimator"
            ) { targetPortal ->
                when (targetPortal) {
                    "Client" -> ClientPortal(viewModel = viewModel)
                    "Housekeeper" -> HousekeeperPortal(viewModel = viewModel)
                    "Admin" -> AdminPortal(viewModel = viewModel)
                    else -> FaqAndSupportPortal(viewModel = viewModel)
                }
            }
        }
    }
}
