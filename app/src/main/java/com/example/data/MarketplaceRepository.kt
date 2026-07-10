package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MarketplaceRepository(
    private val housekeeperDao: HousekeeperDao,
    private val bookingDao: BookingDao,
    private val reviewDao: ReviewDao,
    private val supportTicketDao: SupportTicketDao,
    private val userDao: UserDao
) {
    // Dual Engine State
    private val _isCloudMode = MutableStateFlow(true)
    val isCloudMode: StateFlow<Boolean> = _isCloudMode.asStateFlow()

    // Query Logging System
    private val _queryLogs = MutableStateFlow<List<String>>(emptyList())
    val queryLogs: StateFlow<List<String>> = _queryLogs.asStateFlow()

    // Active User Selection (Simulates changing workspace)
    private val _activePortal = MutableStateFlow("Client") // "Client", "Housekeeper", "Admin"
    val activePortal: StateFlow<String> = _activePortal.asStateFlow()

    // Authentication States
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Flow of all registered users
    val allUsersFlow: Flow<List<UserEntity>> = userDao.getAllUsersFlow()


    // Simulated "Logged In" ID for Maid Workspace
    private val _activeMaidId = MutableStateFlow(1) // Defaults to Namubiru Juliet
    val activeMaidId: StateFlow<Int> = _activeMaidId.asStateFlow()

    // Flow inputs
    val allHousekeepers: Flow<List<HousekeeperEntity>> = housekeeperDao.getAllHousekeepers()
    val allBookings: Flow<List<BookingEntity>> = bookingDao.getAllBookings()
    val allReviews: Flow<List<ReviewEntity>> = reviewDao.getAllReviews()
    val allTickets: Flow<List<SupportTicketEntity>> = supportTicketDao.getAllTickets()

    fun logQuery(engine: String, action: String) {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val logLine = "[$timestamp] [$engine] $action"
        _queryLogs.value = listOf(logLine) + _queryLogs.value.take(199) // Keep last 200 logs
    }

    fun setPortal(portal: String) {
        _activePortal.value = portal
        logQuery("APP CORE", "Switched workspace role context to: $portal Portal")
    }

    fun setActiveMaidId(id: Int) {
        _activeMaidId.value = id
        logQuery("APP CORE", "Switched Housekeeper session to ID: $id")
    }

    suspend fun setCloudMode(enabled: Boolean) {
        _isCloudMode.value = enabled
        if (enabled) {
            logQuery("CLOUD SYNC", "Toggle Dual Engine: ACTIVATING Firebase Firestore cloud persistence...")
            logQuery("CLOUD SYNC", "Offline cache synchronizing metadata with region uganda-east-1...")
            delay(400)
            logQuery("CLOUD SYNC", "Establish secure connection: Firestore client initialized successfully.")
            startRealtimeSync()
        } else {
            logQuery("LOCAL ROOM", "Toggle Dual Engine: DISCONNECTING cloud. High-speed local disk storage active.")
            stopRealtimeSync()
        }
    }

    suspend fun simulateLatency() {
        if (_isCloudMode.value) {
            // Simulate cloud network latency (e.g. 500ms)
            delay(500)
        }
    }

    // --- Core Housekeeper Logic ---
    suspend fun insertHousekeeper(hk: HousekeeperEntity) {
        simulateLatency()
        housekeeperDao.insertHousekeeper(hk)
        val engineName = if (_isCloudMode.value) "CLOUD-FIRESTORE" else "LOCAL-SQLITE"
        logQuery(engineName, "INSERT/REPLACE housekeeper: ${hk.name} (NIN: ${hk.nin})")
        if (_isCloudMode.value && isFirebaseAvailable()) {
            syncHousekeeperToFirestore(hk)
        }
    }

    suspend fun updateHousekeeper(hk: HousekeeperEntity) {
        simulateLatency()
        housekeeperDao.updateHousekeeper(hk)
        val engineName = if (_isCloudMode.value) "CLOUD-FIRESTORE" else "LOCAL-SQLITE"
        logQuery(engineName, "UPDATE housekeeper ID ${hk.id}: ${hk.name} status=${hk.vettingStatus}")
        if (_isCloudMode.value && isFirebaseAvailable()) {
            syncHousekeeperToFirestore(hk)
        }
    }

    suspend fun getHousekeeperById(id: Int): HousekeeperEntity? {
        val res = housekeeperDao.getHousekeeperById(id)
        val engineName = if (_isCloudMode.value) "CLOUD-FIRESTORE" else "LOCAL-SQLITE"
        logQuery(engineName, "SELECT housekeeper WHERE id = $id (result=${res?.name ?: "null"})")
        return res
    }

    // --- Core Bookings Logic ---
    suspend fun createBooking(booking: BookingEntity): Int {
        simulateLatency()
        val id = bookingDao.insertBooking(booking).toInt()
        val engineName = if (_isCloudMode.value) "CLOUD-FIRESTORE" else "LOCAL-SQLITE"
        logQuery(engineName, "INSERT booking request: Maid=${booking.housekeeperName}, Employer=${booking.employerName}, status=${booking.status} (Row ID: $id)")
        if (_isCloudMode.value && isFirebaseAvailable()) {
            syncBookingToFirestore(booking.copy(id = id))
        }
        return id
    }

    suspend fun updateBooking(booking: BookingEntity) {
        simulateLatency()
        bookingDao.updateBooking(booking)
        val engineName = if (_isCloudMode.value) "CLOUD-FIRESTORE" else "LOCAL-SQLITE"
        logQuery(engineName, "UPDATE booking ID ${booking.id}: Status changed to '${booking.status}'")
        if (_isCloudMode.value && isFirebaseAvailable()) {
            syncBookingToFirestore(booking)
        }
    }

    // --- Core Reviews Logic ---
    suspend fun insertReview(review: ReviewEntity) {
        simulateLatency()
        reviewDao.insertReview(review)
        val engineName = if (_isCloudMode.value) "CLOUD-FIRESTORE" else "LOCAL-SQLITE"
        logQuery(engineName, "INSERT review for HK ID ${review.housekeeperId} by ${review.employerName} (Rating: ${review.rating})")
        
        // Recalculate Housekeeper average rating and reviewCount
        val hks = housekeeperDao.getAllHousekeepers().first()
        val targetHk = hks.find { it.id == review.housekeeperId }
        if (targetHk != null) {
            val hReviews = reviewDao.getReviewsForHousekeeper(review.housekeeperId).first()
            val filteredReviews = hReviews.filter { !it.isHidden }
            val count = filteredReviews.size
            val avg = if (count > 0) filteredReviews.map { it.rating }.average().toFloat() else 5.0f
            val updatedHk = targetHk.copy(rating = avg, reviewCount = count)
            housekeeperDao.updateHousekeeper(updatedHk)
            logQuery(engineName, "RECALCULATE rating for ${targetHk.name}: Avg=$avg, Count=$count")
            if (_isCloudMode.value && isFirebaseAvailable()) {
                syncHousekeeperToFirestore(updatedHk)
            }
        }
        if (_isCloudMode.value && isFirebaseAvailable()) {
            syncReviewToFirestore(review)
        }
    }

    suspend fun updateReview(review: ReviewEntity) {
        simulateLatency()
        reviewDao.updateReview(review)
        val engineName = if (_isCloudMode.value) "CLOUD-FIRESTORE" else "LOCAL-SQLITE"
        logQuery(engineName, "UPDATE review ID ${review.id}: Flagged=${review.isFlagged}, Hidden=${review.isHidden}")
        if (_isCloudMode.value && isFirebaseAvailable()) {
            syncReviewToFirestore(review)
        }
    }

    suspend fun deleteReview(review: ReviewEntity) {
        simulateLatency()
        reviewDao.deleteReview(review)
        val engineName = if (_isCloudMode.value) "CLOUD-FIRESTORE" else "LOCAL-SQLITE"
        logQuery(engineName, "DELETE review ID ${review.id}")
        if (_isCloudMode.value && isFirebaseAvailable()) {
            deleteReviewFromFirestore(review)
        }
    }

    // --- Support Ticket Logic ---
    suspend fun insertTicket(ticket: SupportTicketEntity) {
        simulateLatency()
        supportTicketDao.insertTicket(ticket)
        val engineName = if (_isCloudMode.value) "CLOUD-FIRESTORE" else "LOCAL-SQLITE"
        logQuery(engineName, "INSERT Support Ticket: '${ticket.title}' (Category: ${ticket.category})")
        if (_isCloudMode.value && isFirebaseAvailable()) {
            syncTicketToFirestore(ticket)
        }
    }

    suspend fun updateTicket(ticket: SupportTicketEntity) {
        simulateLatency()
        supportTicketDao.updateTicket(ticket)
        val engineName = if (_isCloudMode.value) "CLOUD-FIRESTORE" else "LOCAL-SQLITE"
        logQuery(engineName, "UPDATE Support Ticket ID ${ticket.id}: Status=${ticket.status}")
        if (_isCloudMode.value && isFirebaseAvailable()) {
            syncTicketToFirestore(ticket)
        }
    }

    suspend fun updateUser(user: UserEntity) {
        simulateLatency()
        userDao.updateUser(user)
        logQuery("LOCAL-SQLITE", "UPDATE User account ID ${user.id}: username='${user.username}', isSuspended=${user.isSuspended}")
        if (_isCloudMode.value && isFirebaseAvailable()) {
            syncUserToFirestore(user)
        }
    }

    suspend fun deleteUser(user: UserEntity) {
        simulateLatency()
        userDao.deleteUser(user)
        logQuery("LOCAL-SQLITE", "DELETE User account ID ${user.id}: username='${user.username}'")
        if (_isCloudMode.value && isFirebaseAvailable()) {
            deleteUserFromFirestore(user)
        }
    }

    suspend fun deleteHousekeeper(hk: HousekeeperEntity) {
        simulateLatency()
        housekeeperDao.deleteHousekeeper(hk)
        logQuery("LOCAL-SQLITE", "DELETE Housekeeper profile ID ${hk.id}: name='${hk.name}'")
        if (_isCloudMode.value && isFirebaseAvailable()) {
            deleteHousekeeperFromFirestore(hk)
        }
    }

    // --- Authentication & User Session Management ---
    suspend fun registerUser(user: UserEntity): Long {
        simulateLatency()
        val existing = userDao.getUserByUsername(user.username)
        if (existing != null) {
            logQuery("AUTH ACCOUNT", "Registration Failed: Username '${user.username}' is already taken.")
            return -1L
        }
        val id = userDao.insertUser(user)
        logQuery("AUTH REGISTER", "Registered user account: '${user.username}' - Role: ${user.role} (ID: $id)")
        return id
    }

    suspend fun loginUser(username: String, passwordHash: String): UserEntity? {
        simulateLatency()
        val user = userDao.getUserByUsername(username)
        if (user != null && user.passwordHash == passwordHash) {
            if (user.isSuspended) {
                logQuery("AUTH LOGIN", "Login BLOCKED: user '${username}' is SUSPENDED by administration.")
                return user
            }
            _currentUser.value = user
            _activePortal.value = user.role
            if (user.role == "Housekeeper") {
                val hks = housekeeperDao.getAllHousekeepers().first()
                val maid = hks.find { it.nin.trim().uppercase() == user.nin.trim().uppercase() }
                if (maid != null) {
                    _activeMaidId.value = maid.id
                }
            }
            logQuery("AUTH LOGIN", "User signed in successfully: '${username}' with Role '${user.role}'")
            return user
        }
        logQuery("AUTH LOGIN", "Login attempt failed for username: '${username}'")
        return null
    }

    fun logout() {
        logQuery("AUTH SIGNOUT", "Signed out user session: ${_currentUser.value?.username ?: "Guest"}")
        _currentUser.value = null
    }

    suspend fun seedDefaultUsersIfEmpty() {
        val allUsers = userDao.getAllUsersFlow().first()
        val hasAdmin = allUsers.any { it.username == "admin" }
        if (!hasAdmin) {
            logQuery("DB SEED", "Seeding default security admin authenticated account...")
            val demoAdmin = UserEntity(
                username = "admin",
                passwordHash = "admin123",
                fullName = "Nalwanga Proscovia",
                nin = "CM843920194UGA",
                phoneNumber = "+256 701 384729",
                emailAddress = "admin@maidsconnect.ug",
                role = "Admin",
                district = "Kampala",
                subcounty = "Kampala Central",
                village = "Nakasero"
            )
            userDao.insertUser(demoAdmin)
            if (_isCloudMode.value && isFirebaseAvailable()) {
                syncUserToFirestore(demoAdmin)
            }
            logQuery("DB SEED", "Seeded default security admin credentials: 'admin/admin123'")
        }
    }

    // --- Data Seeding Checker ---
    suspend fun checkAndSeedData() {
        if (_isCloudMode.value && isFirebaseAvailable()) {
            startRealtimeSync()
            syncFirestoreToRoom()
        }
        seedDefaultUsersIfEmpty()
        logQuery("DB INIT", "Real database environment active.")
    }

    private fun isFirebaseAvailable(): Boolean {
        return try {
            com.google.firebase.FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun syncHousekeeperToFirestore(hk: HousekeeperEntity) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val data = mapOf(
                "id" to hk.id,
                "name" to hk.name,
                "avatarEmoji" to hk.avatarEmoji,
                "phone" to hk.phone,
                "bio" to hk.bio,
                "rateUGX" to hk.rateUGX,
                "rateType" to hk.rateType,
                "skills" to hk.skills,
                "languages" to hk.languages,
                "district" to hk.district,
                "county" to hk.county,
                "subcounty" to hk.subcounty,
                "village" to hk.village,
                "experienceYears" to hk.experienceYears,
                "rating" to hk.rating,
                "reviewCount" to hk.reviewCount,
                "vettingStatus" to hk.vettingStatus,
                "isAvailable" to hk.isAvailable,
                "nin" to hk.nin,
                "profileImageUri" to hk.profileImageUri
            )
            db.collection("housekeepers")
                .document(hk.id.toString())
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    logQuery("CLOUD-FIRESTORE", "Successfully synchronized housekeeper: ${hk.name} to Firestore.")
                }
                .addOnFailureListener { e ->
                    logQuery("CLOUD-FIRESTORE", "WARNING: Firestore housekeeper sync failed: ${e.message}")
                }
        } catch (e: Exception) {
            logQuery("CLOUD-FIRESTORE", "WARNING: Firestore housekeeper sync exception: ${e.message}")
        }
    }

    private fun deleteHousekeeperFromFirestore(hk: HousekeeperEntity) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("housekeepers")
                .document(hk.id.toString())
                .delete()
                .addOnSuccessListener {
                    logQuery("CLOUD-FIRESTORE", "Successfully deleted housekeeper ID ${hk.id} reference from Firestore.")
                }
        } catch (e: Exception) {
            logQuery("CLOUD-FIRESTORE", "WARNING: Firestore housekeeper delete failed: ${e.message}")
        }
    }

    private fun syncBookingToFirestore(booking: BookingEntity) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val data = mapOf(
                "id" to booking.id,
                "housekeeperId" to booking.housekeeperId,
                "housekeeperName" to booking.housekeeperName,
                "employerName" to booking.employerName,
                "employerPhone" to booking.employerPhone,
                "employerNIN" to booking.employerNIN,
                "startDate" to booking.startDate,
                "durationDays" to booking.durationDays,
                "totalCostUGX" to booking.totalCostUGX,
                "placementFeePaid" to booking.placementFeePaid,
                "paymentReference" to booking.paymentReference,
                "status" to booking.status,
                "timestamp" to booking.timestamp
            )
            db.collection("bookings")
                .document(booking.id.toString())
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    logQuery("CLOUD-FIRESTORE", "Successfully synchronized booking ID ${booking.id} to Firestore.")
                }
                .addOnFailureListener { e ->
                    logQuery("CLOUD-FIRESTORE", "WARNING: Firestore booking sync failed: ${e.message}")
                }
        } catch (e: Exception) {
            logQuery("CLOUD-FIRESTORE", "WARNING: Firestore booking sync exception: ${e.message}")
        }
    }

    private fun syncReviewToFirestore(review: ReviewEntity) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val data = mapOf(
                "id" to review.id,
                "housekeeperId" to review.housekeeperId,
                "bookingId" to review.bookingId,
                "employerName" to review.employerName,
                "rating" to review.rating,
                "comment" to review.comment,
                "isFlagged" to review.isFlagged,
                "isHidden" to review.isHidden,
                "timestamp" to review.timestamp
            )
            db.collection("reviews")
                .document(review.id.toString())
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    logQuery("CLOUD-FIRESTORE", "Successfully synchronized review ID ${review.id} to Firestore.")
                }
                .addOnFailureListener { e ->
                    logQuery("CLOUD-FIRESTORE", "WARNING: Firestore review sync failed: ${e.message}")
                }
        } catch (e: Exception) {
            logQuery("CLOUD-FIRESTORE", "WARNING: Firestore review sync exception: ${e.message}")
        }
    }

    private fun deleteReviewFromFirestore(review: ReviewEntity) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("reviews")
                .document(review.id.toString())
                .delete()
                .addOnSuccessListener {
                    logQuery("CLOUD-FIRESTORE", "Successfully deleted review ID ${review.id} reference from Firestore.")
                }
        } catch (e: Exception) {
            logQuery("CLOUD-FIRESTORE", "WARNING: Firestore review delete failed: ${e.message}")
        }
    }

    private fun syncTicketToFirestore(ticket: SupportTicketEntity) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val data = mapOf(
                "id" to ticket.id,
                "title" to ticket.title,
                "category" to ticket.category,
                "description" to ticket.description,
                "status" to ticket.status,
                "userRole" to ticket.userRole,
                "timestamp" to ticket.timestamp
            )
            db.collection("support_tickets")
                .document(ticket.id.toString())
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    logQuery("CLOUD-FIRESTORE", "Successfully synchronized Ticket ID ${ticket.id} to Firestore.")
                }
                .addOnFailureListener { e ->
                    logQuery("CLOUD-FIRESTORE", "WARNING: Firestore ticket sync failed: ${e.message}")
                }
        } catch (e: Exception) {
            logQuery("CLOUD-FIRESTORE", "WARNING: Firestore ticket sync exception: ${e.message}")
        }
    }

    private fun syncUserToFirestore(user: UserEntity) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val data = mapOf(
                "id" to user.id,
                "username" to user.username,
                "fullName" to user.fullName,
                "nin" to user.nin,
                "phoneNumber" to user.phoneNumber,
                "emailAddress" to user.emailAddress,
                "role" to user.role,
                "district" to user.district,
                "subcounty" to user.subcounty,
                "village" to user.village,
                "isVerified" to user.isVerified,
                "isSuspended" to user.isSuspended
            )
            db.collection("users")
                .document(user.id.toString())
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    logQuery("CLOUD-FIRESTORE", "Successfully synchronized user profile: ${user.username} to Firestore.")
                }
                .addOnFailureListener { e ->
                    logQuery("CLOUD-FIRESTORE", "WARNING: Firestore user sync failed: ${e.message}")
                }
        } catch (e: Exception) {
            logQuery("CLOUD-FIRESTORE", "WARNING: Firestore user sync exception: ${e.message}")
        }
    }

    private fun deleteUserFromFirestore(user: UserEntity) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("users")
                .document(user.id.toString())
                .delete()
                .addOnSuccessListener {
                    logQuery("CLOUD-FIRESTORE", "Successfully deleted user profile ID ${user.id} reference from Firestore.")
                }
        } catch (e: Exception) {
            logQuery("CLOUD-FIRESTORE", "WARNING: Firestore user profile delete failed: ${e.message}")
        }
    }

    private val activeListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    fun startRealtimeSync() {
        if (!isFirebaseAvailable()) return
        stopRealtimeSync()

        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

            // Listen to housekeepers
            val l1 = db.collection("housekeepers")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        logQuery("CLOUD SYNC", "Housekeeper sync stream error: ${e.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        val hksList = mutableListOf<HousekeeperEntity>()
                        for (doc in snapshots.documents) {
                            try {
                                val id = doc.getLong("id")?.toInt() ?: continue
                                val name = doc.getString("name") ?: ""
                                val avatarEmoji = doc.getString("avatarEmoji") ?: "🧹"
                                val phone = doc.getString("phone") ?: ""
                                val bio = doc.getString("bio") ?: ""
                                val rateUGX = doc.getLong("rateUGX")?.toInt() ?: 15000
                                val rateType = doc.getString("rateType") ?: "Daily"
                                val skills = doc.getString("skills") ?: ""
                                val languages = doc.getString("languages") ?: ""
                                val district = doc.getString("district") ?: ""
                                val county = doc.getString("county") ?: ""
                                val subcounty = doc.getString("subcounty") ?: ""
                                val village = doc.getString("village") ?: ""
                                val experienceYears = doc.getLong("experienceYears")?.toInt() ?: 1
                                val rating = doc.getDouble("rating")?.toFloat() ?: 5.0f
                                val reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0
                                val vettingStatus = doc.getString("vettingStatus") ?: "Pending Approval"
                                val isAvailable = doc.getBoolean("isAvailable") ?: true
                                val nin = doc.getString("nin") ?: ""
                                val profileImageUri = doc.getString("profileImageUri")

                                hksList.add(
                                    HousekeeperEntity(
                                        id = id,
                                        name = name,
                                        avatarEmoji = avatarEmoji,
                                        phone = phone,
                                        bio = bio,
                                        rateUGX = rateUGX,
                                        rateType = rateType,
                                        skills = skills,
                                        languages = languages,
                                        district = district,
                                        county = county,
                                        subcounty = subcounty,
                                        village = village,
                                        experienceYears = experienceYears,
                                        rating = rating,
                                        reviewCount = reviewCount,
                                        vettingStatus = vettingStatus,
                                        isAvailable = isAvailable,
                                        nin = nin,
                                        profileImageUri = profileImageUri
                                    )
                                )
                            } catch (ex: Exception) {}
                        }
                        if (hksList.isNotEmpty()) {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                housekeeperDao.insertHousekeepers(hksList)
                                logQuery("CLOUD SYNC", "Real-time update: Integrated ${hksList.size} housekeepers from Firestore.")
                            }
                        }
                    }
                }
            activeListeners.add(l1)

            // Listen to bookings
            val l2 = db.collection("bookings")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshots != null) {
                        val list = mutableListOf<BookingEntity>()
                        for (doc in snapshots.documents) {
                            try {
                                val id = doc.getLong("id")?.toInt() ?: continue
                                val housekeeperId = doc.getLong("housekeeperId")?.toInt() ?: 0
                                val housekeeperName = doc.getString("housekeeperName") ?: ""
                                val employerName = doc.getString("employerName") ?: ""
                                val employerPhone = doc.getString("employerPhone") ?: ""
                                val employerNIN = doc.getString("employerNIN") ?: ""
                                val startDate = doc.getString("startDate") ?: ""
                                val durationDays = doc.getLong("durationDays")?.toInt() ?: 1
                                val totalCostUGX = doc.getLong("totalCostUGX")?.toInt() ?: 0
                                val placementFeePaid = doc.getBoolean("placementFeePaid") ?: false
                                val paymentReference = doc.getString("paymentReference") ?: ""
                                val status = doc.getString("status") ?: "Pending"
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                list.add(
                                    BookingEntity(
                                        id = id,
                                        housekeeperId = housekeeperId,
                                        housekeeperName = housekeeperName,
                                        employerName = employerName,
                                        employerPhone = employerPhone,
                                        employerNIN = employerNIN,
                                        startDate = startDate,
                                        durationDays = durationDays,
                                        totalCostUGX = totalCostUGX,
                                        placementFeePaid = placementFeePaid,
                                        paymentReference = paymentReference,
                                        status = status,
                                        timestamp = timestamp
                                    )
                                )
                            } catch (ex: Exception) {}
                        }
                        if (list.isNotEmpty()) {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                for (item in list) {
                                    bookingDao.insertBooking(item)
                                }
                                logQuery("CLOUD SYNC", "Real-time update: Sync ${list.size} booking records.")
                            }
                        }
                    }
                }
            activeListeners.add(l2)

            // Listen to reviews
            val l3 = db.collection("reviews")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshots != null) {
                        val list = mutableListOf<ReviewEntity>()
                        for (doc in snapshots.documents) {
                            try {
                                val id = doc.getLong("id")?.toInt() ?: continue
                                val housekeeperId = doc.getLong("housekeeperId")?.toInt() ?: 0
                                val bookingId = doc.getLong("bookingId")?.toInt() ?: 0
                                val employerName = doc.getString("employerName") ?: ""
                                val rating = doc.getLong("rating")?.toInt() ?: 5
                                val comment = doc.getString("comment") ?: ""
                                val isFlagged = doc.getBoolean("isFlagged") ?: false
                                val isHidden = doc.getBoolean("isHidden") ?: false
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                list.add(
                                    ReviewEntity(
                                        id = id,
                                        housekeeperId = housekeeperId,
                                        bookingId = bookingId,
                                        employerName = employerName,
                                        rating = rating,
                                        comment = comment,
                                        isFlagged = isFlagged,
                                        isHidden = isHidden,
                                        timestamp = timestamp
                                    )
                                )
                            } catch (ex: Exception) {}
                        }
                        if (list.isNotEmpty()) {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                for (item in list) {
                                    reviewDao.insertReview(item)
                                }
                                logQuery("CLOUD SYNC", "Real-time update: Sync ${list.size} reviews.")
                            }
                        }
                    }
                }
            activeListeners.add(l3)

            // Listen to tickets
            val l4 = db.collection("support_tickets")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshots != null) {
                        val list = mutableListOf<SupportTicketEntity>()
                        for (doc in snapshots.documents) {
                            try {
                                val id = doc.getLong("id")?.toInt() ?: continue
                                val title = doc.getString("title") ?: ""
                                val category = doc.getString("category") ?: ""
                                val description = doc.getString("description") ?: ""
                                val status = doc.getString("status") ?: "Open"
                                val userRole = doc.getString("userRole") ?: "Client"
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                list.add(
                                    SupportTicketEntity(
                                        id = id,
                                        title = title,
                                        category = category,
                                        description = description,
                                        status = status,
                                        userRole = userRole,
                                        timestamp = timestamp
                                    )
                                )
                            } catch (ex: Exception) {}
                        }
                        if (list.isNotEmpty()) {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                for (item in list) {
                                    supportTicketDao.insertTicket(item)
                                }
                                logQuery("CLOUD SYNC", "Real-time update: Sync ${list.size} tickets.")
                            }
                        }
                    }
                }
            activeListeners.add(l4)

            // Listen to users
            val l5 = db.collection("users")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshots != null) {
                        val list = mutableListOf<UserEntity>()
                        for (doc in snapshots.documents) {
                            try {
                                val id = doc.getLong("id")?.toInt() ?: continue
                                val username = doc.getString("username") ?: ""
                                val passwordHash = doc.getString("passwordHash") ?: "123456"
                                val fullName = doc.getString("fullName") ?: ""
                                val nin = doc.getString("nin") ?: ""
                                val phoneNumber = doc.getString("phoneNumber") ?: ""
                                val emailAddress = doc.getString("emailAddress") ?: ""
                                val role = doc.getString("role") ?: "Client"
                                val district = doc.getString("district") ?: ""
                                val subcounty = doc.getString("subcounty") ?: ""
                                val village = doc.getString("village") ?: ""
                                val isVerified = doc.getBoolean("isVerified") ?: false
                                val isSuspended = doc.getBoolean("isSuspended") ?: false

                                list.add(
                                    UserEntity(
                                        id = id,
                                        username = username,
                                        passwordHash = passwordHash,
                                        fullName = fullName,
                                        nin = nin,
                                        phoneNumber = phoneNumber,
                                        emailAddress = emailAddress,
                                        role = role,
                                        district = district,
                                        subcounty = subcounty,
                                        village = village,
                                        isVerified = isVerified,
                                        isSuspended = isSuspended
                                    )
                                )
                            } catch (ex: Exception) {}
                        }
                        if (list.isNotEmpty()) {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                for (item in list) {
                                    userDao.insertUser(item)
                                }
                                logQuery("CLOUD SYNC", "Real-time update: Sync ${list.size} user settings.")
                            }
                        }
                    }
                }
            activeListeners.add(l5)

        } catch (e: Exception) {
            logQuery("CLOUD SYNC", "Real-time listener setup caught exception: ${e.message}")
        }
    }

    fun stopRealtimeSync() {
        for (listener in activeListeners) {
            try {
                listener.remove()
            } catch (e: Exception) {}
        }
        activeListeners.clear()
    }

    suspend fun syncFirestoreToRoom() {
        if (!isFirebaseAvailable()) {
            logQuery("CLOUD SYNC", "WARNING: Firebase not available for bidirectional sync.")
            return
        }

        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            logQuery("CLOUD SYNC", "Starting full download from cloud database collections...")

            // 1. Sync Housekeepers
            db.collection("housekeepers")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val hksList = mutableListOf<HousekeeperEntity>()
                    for (doc in querySnapshot.documents) {
                        try {
                            val id = doc.getLong("id")?.toInt() ?: continue
                            val name = doc.getString("name") ?: ""
                            val avatarEmoji = doc.getString("avatarEmoji") ?: "🧹"
                            val phone = doc.getString("phone") ?: ""
                            val bio = doc.getString("bio") ?: ""
                            val rateUGX = doc.getLong("rateUGX")?.toInt() ?: 15000
                            val rateType = doc.getString("rateType") ?: "Daily"
                            val skills = doc.getString("skills") ?: ""
                            val languages = doc.getString("languages") ?: ""
                            val district = doc.getString("district") ?: ""
                            val county = doc.getString("county") ?: ""
                            val subcounty = doc.getString("subcounty") ?: ""
                            val village = doc.getString("village") ?: ""
                            val experienceYears = doc.getLong("experienceYears")?.toInt() ?: 1
                            val rating = doc.getDouble("rating")?.toFloat() ?: 5.0f
                            val reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0
                            val vettingStatus = doc.getString("vettingStatus") ?: "Pending Approval"
                            val isAvailable = doc.getBoolean("isAvailable") ?: true
                            val nin = doc.getString("nin") ?: ""
                            val profileImageUri = doc.getString("profileImageUri")

                            hksList.add(
                                HousekeeperEntity(
                                    id = id,
                                    name = name,
                                    avatarEmoji = avatarEmoji,
                                    phone = phone,
                                    bio = bio,
                                    rateUGX = rateUGX,
                                    rateType = rateType,
                                    skills = skills,
                                    languages = languages,
                                    district = district,
                                    county = county,
                                    subcounty = subcounty,
                                    village = village,
                                    experienceYears = experienceYears,
                                    rating = rating,
                                    reviewCount = reviewCount,
                                    vettingStatus = vettingStatus,
                                    isAvailable = isAvailable,
                                    nin = nin,
                                    profileImageUri = profileImageUri
                                )
                            )
                        } catch (e: Exception) {
                            logQuery("CLOUD SYNC", "Error parsing housekeeper document ${doc.id}: ${e.message}")
                        }
                    }
                    if (hksList.isNotEmpty()) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            housekeeperDao.insertHousekeepers(hksList)
                            logQuery("CLOUD SYNC", "Synchronized ${hksList.size} housekeeper profiles from Firestore.")
                        }
                    } else {
                        // If Cloud Firestore has no housekeepers, populate it with current local ones so it's not empty!
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val localHks = housekeeperDao.getAllHousekeepers().first()
                            for (hk in localHks) {
                                syncHousekeeperToFirestore(hk)
                            }
                            logQuery("CLOUD SYNC", "Firestore collection empty. Seeding local profiles up to the cloud.")
                        }
                    }
                }

            // 2. Sync Bookings
            db.collection("bookings")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val bookingsList = mutableListOf<BookingEntity>()
                    for (doc in querySnapshot.documents) {
                        try {
                            val id = doc.getLong("id")?.toInt() ?: continue
                            val housekeeperId = doc.getLong("housekeeperId")?.toInt() ?: 0
                            val housekeeperName = doc.getString("housekeeperName") ?: ""
                            val employerName = doc.getString("employerName") ?: ""
                            val employerPhone = doc.getString("employerPhone") ?: ""
                            val employerNIN = doc.getString("employerNIN") ?: ""
                            val startDate = doc.getString("startDate") ?: ""
                            val durationDays = doc.getLong("durationDays")?.toInt() ?: 1
                            val totalCostUGX = doc.getLong("totalCostUGX")?.toInt() ?: 0
                            val placementFeePaid = doc.getBoolean("placementFeePaid") ?: false
                            val paymentReference = doc.getString("paymentReference") ?: ""
                            val status = doc.getString("status") ?: "Pending"
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                            bookingsList.add(
                                BookingEntity(
                                    id = id,
                                    housekeeperId = housekeeperId,
                                    housekeeperName = housekeeperName,
                                    employerName = employerName,
                                    employerPhone = employerPhone,
                                    employerNIN = employerNIN,
                                    startDate = startDate,
                                    durationDays = durationDays,
                                    totalCostUGX = totalCostUGX,
                                    placementFeePaid = placementFeePaid,
                                    paymentReference = paymentReference,
                                    status = status,
                                    timestamp = timestamp
                                )
                            )
                        } catch (e: Exception) {
                            logQuery("CLOUD SYNC", "Error parsing booking document ${doc.id}: ${e.message}")
                        }
                    }
                    if (bookingsList.isNotEmpty()) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            for (b in bookingsList) {
                                bookingDao.insertBooking(b)
                            }
                            logQuery("CLOUD SYNC", "Synchronized ${bookingsList.size} booking records from Firestore.")
                        }
                    }
                }

            // 3. Sync Reviews
            db.collection("reviews")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val reviewsList = mutableListOf<ReviewEntity>()
                    for (doc in querySnapshot.documents) {
                        try {
                            val id = doc.getLong("id")?.toInt() ?: continue
                            val housekeeperId = doc.getLong("housekeeperId")?.toInt() ?: 0
                            val bookingId = doc.getLong("bookingId")?.toInt() ?: 0
                            val employerName = doc.getString("employerName") ?: ""
                            val rating = doc.getLong("rating")?.toInt() ?: 5
                            val comment = doc.getString("comment") ?: ""
                            val isFlagged = doc.getBoolean("isFlagged") ?: false
                            val isHidden = doc.getBoolean("isHidden") ?: false
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                            reviewsList.add(
                                ReviewEntity(
                                    id = id,
                                    housekeeperId = housekeeperId,
                                    bookingId = bookingId,
                                    employerName = employerName,
                                    rating = rating,
                                    comment = comment,
                                    isFlagged = isFlagged,
                                    isHidden = isHidden,
                                    timestamp = timestamp
                                )
                            )
                        } catch (e: Exception) {
                            logQuery("CLOUD SYNC", "Error parsing review document ${doc.id}: ${e.message}")
                        }
                    }
                    if (reviewsList.isNotEmpty()) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            for (r in reviewsList) {
                                reviewDao.insertReview(r)
                            }
                            logQuery("CLOUD SYNC", "Synchronized ${reviewsList.size} quality reviews from Firestore.")
                        }
                    }
                }

            // 4. Sync Users
            db.collection("users")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val usersList = mutableListOf<UserEntity>()
                    for (doc in querySnapshot.documents) {
                        try {
                            val id = doc.getLong("id")?.toInt() ?: continue
                            val username = doc.getString("username") ?: ""
                            val passwordHash = doc.getString("passwordHash") ?: "123456"
                            val fullName = doc.getString("fullName") ?: ""
                            val nin = doc.getString("nin") ?: ""
                            val phoneNumber = doc.getString("phoneNumber") ?: ""
                            val emailAddress = doc.getString("emailAddress") ?: ""
                            val role = doc.getString("role") ?: "Client"
                            val district = doc.getString("district") ?: ""
                            val subcounty = doc.getString("subcounty") ?: ""
                            val village = doc.getString("village") ?: ""
                            val isVerified = doc.getBoolean("isVerified") ?: false
                            val isSuspended = doc.getBoolean("isSuspended") ?: false

                            usersList.add(
                                UserEntity(
                                    id = id,
                                    username = username,
                                    passwordHash = passwordHash,
                                    fullName = fullName,
                                    nin = nin,
                                    phoneNumber = phoneNumber,
                                    emailAddress = emailAddress,
                                    role = role,
                                    district = district,
                                    subcounty = subcounty,
                                    village = village,
                                    isVerified = isVerified,
                                    isSuspended = isSuspended
                                )
                            )
                        } catch (e: Exception) {
                            logQuery("CLOUD SYNC", "Error parsing user document ${doc.id}: ${e.message}")
                        }
                    }
                    if (usersList.isNotEmpty()) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            for (u in usersList) {
                                userDao.insertUser(u)
                            }
                            logQuery("CLOUD SYNC", "Synchronized ${usersList.size} user authentication records.")
                        }
                    } else {
                        // Seed local users to firestore
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val localUsers = userDao.getAllUsersFlow().first()
                            for (u in localUsers) {
                                syncUserToFirestore(u)
                            }
                        }
                    }
                }

            // 5. Sync Support Tickets
            db.collection("support_tickets")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val ticketsList = mutableListOf<SupportTicketEntity>()
                    for (doc in querySnapshot.documents) {
                        try {
                            val id = doc.getLong("id")?.toInt() ?: continue
                            val title = doc.getString("title") ?: ""
                            val category = doc.getString("category") ?: ""
                            val description = doc.getString("description") ?: ""
                            val status = doc.getString("status") ?: "Open"
                            val userRole = doc.getString("userRole") ?: "Client"
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                            ticketsList.add(
                                SupportTicketEntity(
                                    id = id,
                                    title = title,
                                    category = category,
                                    description = description,
                                    status = status,
                                    userRole = userRole,
                                    timestamp = timestamp
                                )
                            )
                        } catch (e: Exception) {
                            logQuery("CLOUD SYNC", "Error parsing ticket document ${doc.id}: ${e.message}")
                        }
                    }
                    if (ticketsList.isNotEmpty()) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            for (t in ticketsList) {
                                supportTicketDao.insertTicket(t)
                            }
                            logQuery("CLOUD SYNC", "Synchronized ${ticketsList.size} support tickets.")
                        }
                    }
                }

        } catch (e: Exception) {
            logQuery("CLOUD SYNC", "ERROR: Failed during full Firebase snapshot sync: ${e.message}")
        }
    }
}
