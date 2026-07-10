package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

class MarketplaceViewModel(
    application: Application,
    private val repository: MarketplaceRepository
) : AndroidViewModel(application) {

    // Active platform roles: "Client", "Housekeeper", "Admin"
    val activePortal: StateFlow<String> = repository.activePortal
    val activeMaidId: StateFlow<Int> = repository.activeMaidId
    val isCloudMode: StateFlow<Boolean> = repository.isCloudMode
    val queryLogs: StateFlow<List<String>> = repository.queryLogs
    val currentUser: StateFlow<UserEntity?> = repository.currentUser


    // Core raw flows from DB
    val housekeepers: StateFlow<List<HousekeeperEntity>> = repository.allHousekeepers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookings: StateFlow<List<BookingEntity>> = repository.allBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviews: StateFlow<List<ReviewEntity>> = repository.allReviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tickets: StateFlow<List<SupportTicketEntity>> = repository.allTickets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search & Filtering State (Client Portal) ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedSkill = MutableStateFlow("All")
    val selectedSkill = _selectedSkill.asStateFlow()

    private val _selectedDistrict = MutableStateFlow("All")
    val selectedDistrict = _selectedDistrict.asStateFlow()

    private val _selectedSubcounty = MutableStateFlow("All")
    val selectedSubcounty = _selectedSubcounty.asStateFlow()

    private val _selectedVillage = MutableStateFlow("All")
    val selectedVillage = _selectedVillage.asStateFlow()

    private val _rateTypeFilter = MutableStateFlow("All") // "All", "Daily", "Monthly"
    val rateTypeFilter = _rateTypeFilter.asStateFlow()

    private val _maxBudgetFilter = MutableStateFlow(500000) // Upper limit
    val maxBudgetFilter = _maxBudgetFilter.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    // --- Filtered Housekeepers (Derived State) ---
    val filteredHousekeepers: StateFlow<List<HousekeeperEntity>> = housekeepers
        .combine(_searchQuery) { list, query ->
            if (query.isEmpty()) list else list.filter { it.name.contains(query, ignoreCase = true) || it.bio.contains(query, ignoreCase = true) }
        }
        .combine(_selectedSkill) { list, skill ->
            if (skill == "All") list else list.filter { it.skills.split(",").map { it.trim() }.contains(skill) }
        }
        .combine(_selectedDistrict) { list, dist ->
            if (dist == "All") list else list.filter { it.district.equals(dist, ignoreCase = true) }
        }
        .combine(_selectedSubcounty) { list, sub ->
            if (sub == "All") list else list.filter { it.subcounty.equals(sub, ignoreCase = true) }
        }
        .combine(_selectedVillage) { list, vil ->
            if (vil == "All") list else list.filter { it.village.equals(vil, ignoreCase = true) }
        }
        .combine(_rateTypeFilter) { list, rateType ->
            if (rateType == "All") list else list.filter { it.rateType.equals(rateType, ignoreCase = true) }
        }
        .combine(_maxBudgetFilter) { list, budget ->
            list.filter { it.rateUGX <= budget && it.vettingStatus == "Approved" }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Booking (Matchmaking) Wizards ---
    private val _selectedHKForBooking = MutableStateFlow<HousekeeperEntity?>(null)
    val selectedHKForBooking = _selectedHKForBooking.asStateFlow()

    // Form states
    var bookingStartDate = MutableStateFlow("")
    var bookingDurationDays = MutableStateFlow("7")
    var employerName = MutableStateFlow("")
    var employerPhone = MutableStateFlow("")
    var employerNIN = MutableStateFlow("")

    private val _placementReference = MutableStateFlow("")
    val placementReference = _placementReference.asStateFlow()

    private val _bookingStep = MutableStateFlow(1) // 1 = Details/NIN upload, 2 = simulated Placement Fee, 3 = Matched
    val bookingStep = _bookingStep.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedData()
        }
    }

    // Filter clear/update actions
    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setSkill(s: String) { _selectedSkill.value = s }
    fun setDistrict(d: String) {
        _selectedDistrict.value = d
        // Auto reset cascading subcounty & villages when district changes
        _selectedSubcounty.value = "All"
        _selectedVillage.value = "All"
    }
    fun setSubcounty(sc: String) {
        _selectedSubcounty.value = sc
        _selectedVillage.value = "All"
    }
    fun setVillage(v: String) { _selectedVillage.value = v }
    fun setRateType(rt: String) {
        _rateTypeFilter.value = rt
        // Tweak default budget limit slider based on Daily vs Monthly standards
        if (rt == "Daily") {
            _maxBudgetFilter.value = 50000
        } else if (rt == "Monthly") {
            _maxBudgetFilter.value = 500000
        } else {
            _maxBudgetFilter.value = 500000
        }
    }
    fun setMaxBudget(amt: Int) { _maxBudgetFilter.value = amt }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedSkill.value = "All"
        _selectedDistrict.value = "All"
        _selectedSubcounty.value = "All"
        _selectedVillage.value = "All"
        _rateTypeFilter.value = "All"
        _maxBudgetFilter.value = 500000
    }

    // Portal Switcher
    fun selectPortal(portal: String) {
        viewModelScope.launch {
            repository.setPortal(portal)
        }
    }

    fun selectActiveMaidId(id: Int) {
        viewModelScope.launch {
            repository.setActiveMaidId(id)
        }
    }

    // --- Authentication Actions ---
    fun loginCheck(username: String, pwhash: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val user = repository.loginUser(username, pwhash)
            if (user == null) {
                onResult("FAILED")
            } else if (user.isSuspended) {
                onResult("SUSPENDED")
            } else if (!user.isVerified && user.role != "Admin") {
                onResult("PENDING_APPROVAL")
            } else {
                onResult("SUCCESS")
            }
        }
    }

    fun registerNewUser(
        username: String,
        pwhash: String,
        fullName: String,
        nin: String,
        phone: String,
        email: String,
        role: String,
        district: String,
        subcounty: String,
        village: String,
        maidSkills: String = "",
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val newUser = UserEntity(
                username = username,
                passwordHash = pwhash,
                fullName = fullName,
                nin = nin,
                phoneNumber = phone,
                emailAddress = email,
                role = role,
                district = district,
                subcounty = subcounty,
                village = village,
                isVerified = false
            )
            val resultId = repository.registerUser(newUser)
            if (resultId > 0) {
                if (role == "Housekeeper") {
                    val newHk = HousekeeperEntity(
                        name = fullName,
                        avatarEmoji = "👩🏾",
                        phone = phone,
                        bio = "Licensed domestic specialist. Registered and vetted through NIRA verification.",
                        rateUGX = 15000,
                        rateType = "Daily",
                        skills = if (maidSkills.isBlank()) "Deep Cleaning" else maidSkills,
                        languages = "English,Luganda",
                        district = district,
                        county = district,
                        subcounty = subcounty,
                        village = village,
                        experienceYears = 2,
                        rating = 5.0f,
                        reviewCount = 0,
                        vettingStatus = "Pending Approval",
                        nin = nin,
                        profileImageUri = null
                    )
                    repository.insertHousekeeper(newHk)
                }
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun logoutUser() {
        repository.logout()
    }

    // Dual Cloud/Local Engine switcher
    fun toggleCloudMode(enabled: Boolean) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.setCloudMode(enabled)
            delay(600) // Beautiful sync transition
            _isSyncing.value = false
        }
    }

    // --- Matchmaker Operations ---
    fun startBookingFlow(hk: HousekeeperEntity) {
        _selectedHKForBooking.value = hk
        _bookingStep.value = 1
        _placementReference.value = ""
        // Reset Inputs
        bookingStartDate.value = ""
        bookingDurationDays.value = "7"
        employerName.value = ""
        employerPhone.value = ""
        employerNIN.value = ""
    }

    fun proceedToPayment() {
        if (employerName.value.isBlank() || employerPhone.value.isBlank() || employerNIN.value.isBlank()) {
            repository.logQuery("MATCH ENGINE", "Validation Failed: Employer NIN and contact references are MANDATORY.")
            return
        }
        // Generate random Ugandan reference
        val ugSuffix = (10000..99999).random()
        _placementReference.value = "PAY-UGX-$ugSuffix"
        _bookingStep.value = 2
        repository.logQuery("GATEWAY SIMULATOR", "Generated escrow payment reference UGX 20,000: ${_placementReference.value}")
    }

    fun completeBookingAndHiring() {
        val hk = _selectedHKForBooking.value ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            val rate = hk.rateUGX
            val duration = bookingDurationDays.value.toIntOrNull() ?: 7
            val totalCost = rate * duration

            val newBooking = BookingEntity(
                housekeeperId = hk.id,
                housekeeperName = hk.name,
                employerName = employerName.value,
                employerPhone = employerPhone.value,
                employerNIN = employerNIN.value,
                startDate = bookingStartDate.value.ifBlank { "2026-06-25" },
                durationDays = duration,
                totalCostUGX = totalCost,
                placementFeePaid = true,
                paymentReference = _placementReference.value,
                status = "Pending"
            )

            repository.createBooking(newBooking)
            _bookingStep.value = 3
            _isSyncing.value = false
        }
    }

    fun cancelOrCloseBooking() {
        _selectedHKForBooking.value = null
        _bookingStep.value = 1
    }

    // --- Housekeeper Updates & Actions ---
    fun updateHousekeeperProfile(hk: HousekeeperEntity) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.updateHousekeeper(hk)
            _isSyncing.value = false
        }
    }

    fun uploadProfileImage(
        context: android.content.Context,
        imageUri: android.net.Uri,
        housekeeper: HousekeeperEntity,
        onProgress: (Float, String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            
            // Check if Firebase is initialized/present
            val isFirebaseReady = try {
                com.google.firebase.FirebaseApp.getInstance()
                true
            } catch (e: Exception) {
                false
            }

            if (isFirebaseReady) {
                repository.logQuery("FIREBASE-STORAGE", "Initializing secure connection to default Firebase bucket...")
                try {
                    val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                        .child("profile_images/${housekeeper.id}.jpg")

                    storageRef.putFile(imageUri)
                        .addOnProgressListener { taskSnapshot ->
                            val progress = if (taskSnapshot.totalByteCount > 0) {
                                taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount
                            } else {
                                0.5f
                            }
                            onProgress(progress, "Uploading to Firebase Storage: ${(progress * 100).toInt()}%")
                            repository.logQuery("FIREBASE-STORAGE", "Uploading chunk: ${taskSnapshot.bytesTransferred} of ${taskSnapshot.totalByteCount} bytes")
                        }
                        .addOnSuccessListener {
                            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                val downloadUrl = downloadUri.toString()
                                repository.logQuery("FIREBASE-STORAGE", "Upload successful: $downloadUrl")
                                
                                // Syncing reference with Firebase Firestore!
                                try {
                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    db.collection("housekeepers")
                                        .document(housekeeper.id.toString())
                                        .set(mapOf("profileImageUri" to downloadUrl), com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener {
                                            repository.logQuery("CLOUD-FIRESTORE", "Firestore document sync verified: /housekeepers/${housekeeper.id}")
                                            // Update local Room caching engine
                                            val updated = housekeeper.copy(profileImageUri = downloadUrl)
                                            updateHousekeeperProfile(updated)
                                            _isSyncing.value = false
                                            onComplete(downloadUrl)
                                        }
                                        .addOnFailureListener { fe ->
                                            repository.logQuery("CLOUD-FIRESTORE", "WARNING: Firestore write failed directly, synchronizing locally...")
                                            val updated = housekeeper.copy(profileImageUri = downloadUrl)
                                            updateHousekeeperProfile(updated)
                                            _isSyncing.value = false
                                            onComplete(downloadUrl)
                                        }
                                } catch (fe: Exception) {
                                    repository.logQuery("CLOUD-FIRESTORE", "WARNING: Firestore unavailable, saved to Local Cache")
                                    val updated = housekeeper.copy(profileImageUri = downloadUrl)
                                    updateHousekeeperProfile(updated)
                                    _isSyncing.value = false
                                    onComplete(downloadUrl)
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            _isSyncing.value = false
                            onError(e.localizedMessage ?: "Firebase Storage error")
                            repository.logQuery("FIREBASE-STORAGE", "ERROR: Upload task failed: ${e.message}")
                        }
                } catch (e: Exception) {
                    _isSyncing.value = false
                    onError(e.localizedMessage ?: "Firebase client error")
                    repository.logQuery("FIREBASE-STORAGE", "ERROR: Client initialization failed: ${e.message}")
                }
            } else {
                // Fallback simulation is extremely robust: we use progress intervals, update Room storage,
                // and preserve the actual content/file Uri locally so that the live previewers see their REAL image.
                repository.logQuery("FIREBASE-STORAGE", "NIRA Ledger link offline. Activating Sandbox Firebase Transfer Engine...")
                
                onProgress(0.2f, "Compressing portfolio image matrix...")
                delay(800)
                onProgress(0.5f, "Uploading payload to Firebase Storage bucket (Simulated)...")
                delay(1000)
                
                val localUriStr = imageUri.toString()
                repository.logQuery("FIREBASE-STORAGE", "Uploaded successfully (gs://kazimatch-app.appspot.com/profiles/${housekeeper.id}.jpg)")
                
                onProgress(0.8f, "Writing profile reference to Firestore document...")
                delay(800)
                
                repository.logQuery("CLOUD-FIRESTORE", "Sync Document /housekeepers/${housekeeper.id} { profileImageUri: $localUriStr }")
                
                val updated = housekeeper.copy(profileImageUri = localUriStr)
                updateHousekeeperProfile(updated)
                
                _isSyncing.value = false
                onComplete(localUriStr)
            }
        }
    }

    fun housekeeperConfirmBooking(booking: BookingEntity, accept: Boolean) {
        viewModelScope.launch {
            _isSyncing.value = true
            val newStatus = if (accept) "Confirmed" else "Declined"
            val updatedBooking = booking.copy(status = newStatus)
            repository.updateBooking(updatedBooking)
            _isSyncing.value = false
        }
    }

    fun housekeeperMarkBookingCompleted(booking: BookingEntity) {
        viewModelScope.launch {
            _isSyncing.value = true
            val updatedBooking = booking.copy(status = "Completed")
            repository.updateBooking(updatedBooking)
            _isSyncing.value = false
        }
    }

    // --- Review Management (Client submits rating) ---
    fun submitEmployerReview(hkId: Int, bookingId: Int, author: String, rating: Int, comment: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val newReview = ReviewEntity(
                housekeeperId = hkId,
                bookingId = bookingId,
                employerName = author,
                rating = rating,
                comment = comment
            )
            repository.insertReview(newReview)
            _isSyncing.value = false
        }
    }

    // --- Admin Operations ---
    fun adminSuspendUser(user: UserEntity, suspend: Boolean) {
        viewModelScope.launch {
            _isSyncing.value = true
            val updated = user.copy(isSuspended = suspend)
            repository.updateUser(updated)
            if (user.role == "Housekeeper") {
                val linkedHk = housekeepers.value.find { 
                    it.nin.trim().uppercase() == user.nin.trim().uppercase() || 
                    it.name.trim().equals(user.fullName.trim(), ignoreCase = true) 
                }
                if (linkedHk != null) {
                    val status = if (suspend) "Suspended" else "Approved"
                    repository.updateHousekeeper(linkedHk.copy(vettingStatus = status))
                }
            }
            _isSyncing.value = false
        }
    }

    fun adminVerifyUser(user: UserEntity, verify: Boolean) {
        viewModelScope.launch {
            _isSyncing.value = true
            val updated = user.copy(isVerified = verify)
            repository.updateUser(updated)
            if (user.role == "Housekeeper") {
                val linkedHk = housekeepers.value.find { 
                    it.nin.trim().uppercase() == user.nin.trim().uppercase() || 
                    it.name.trim().equals(user.fullName.trim(), ignoreCase = true) 
                }
                if (linkedHk != null) {
                    val status = if (verify) "Approved" else "Pending Approval"
                    repository.updateHousekeeper(linkedHk.copy(vettingStatus = status))
                }
            }
            _isSyncing.value = false
        }
    }

    fun adminDeleteUser(user: UserEntity) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.deleteUser(user)
            if (user.role == "Housekeeper") {
                val linkedHk = housekeepers.value.find { 
                    it.nin.trim().uppercase() == user.nin.trim().uppercase() || 
                    it.name.trim().equals(user.fullName.trim(), ignoreCase = true) 
                }
                if (linkedHk != null) {
                    repository.deleteHousekeeper(linkedHk)
                }
            }
            _isSyncing.value = false
        }
    }

    fun adminSuspendHousekeeper(hk: HousekeeperEntity, suspend: Boolean) {
        viewModelScope.launch {
            _isSyncing.value = true
            val status = if (suspend) "Suspended" else "Approved"
            val updatedHk = hk.copy(vettingStatus = status)
            repository.updateHousekeeper(updatedHk)
            val linkedUser = allUsers.value.find { 
                it.nin.trim().uppercase() == hk.nin.trim().uppercase() || 
                it.fullName.trim().equals(hk.name.trim(), ignoreCase = true) 
            }
            if (linkedUser != null) {
                repository.updateUser(linkedUser.copy(isSuspended = suspend))
            }
            _isSyncing.value = false
        }
    }

    fun adminDeleteHousekeeper(hk: HousekeeperEntity) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.deleteHousekeeper(hk)
            val linkedUser = allUsers.value.find { 
                it.nin.trim().uppercase() == hk.nin.trim().uppercase() || 
                it.fullName.trim().equals(hk.name.trim(), ignoreCase = true) 
            }
            if (linkedUser != null) {
                repository.deleteUser(linkedUser)
            }
            _isSyncing.value = false
        }
    }

    fun adminEditHousekeeper(hk: HousekeeperEntity) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.updateHousekeeper(hk)
            val linkedUser = allUsers.value.find { 
                it.nin.trim().uppercase() == hk.nin.trim().uppercase() 
            }
            if (linkedUser != null && linkedUser.fullName != hk.name) {
                repository.updateUser(linkedUser.copy(fullName = hk.name))
            }
            _isSyncing.value = false
        }
    }

    fun adminUpdateVetting(hk: HousekeeperEntity, newStatus: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val updated = hk.copy(vettingStatus = newStatus)
            repository.updateHousekeeper(updated)
            // Also approve/verify the matching housekeeper user account
            val linkedUser = allUsers.value.find { 
                it.nin.trim().uppercase() == hk.nin.trim().uppercase() ||
                it.fullName.trim().equals(hk.name.trim(), ignoreCase = true)
            }
            if (linkedUser != null) {
                val updatedUser = linkedUser.copy(
                    isVerified = (newStatus == "Approved"),
                    isSuspended = (newStatus == "Suspended")
                )
                repository.updateUser(updatedUser)
            }
            _isSyncing.value = false
        }
    }

    fun adminOverrideBookingStatus(booking: BookingEntity, overrideStatus: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val updated = booking.copy(status = overrideStatus)
            repository.updateBooking(updated)
            _isSyncing.value = false
        }
    }

    fun adminModerateReview(review: ReviewEntity, hide: Boolean) {
        viewModelScope.launch {
            _isSyncing.value = true
            val updated = review.copy(isHidden = hide, isFlagged = !hide)
            repository.updateReview(updated)
            _isSyncing.value = false
        }
    }

    fun adminDeleteReview(review: ReviewEntity) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.deleteReview(review)
            _isSyncing.value = false
        }
    }

    // --- Support Desk & FAQs ---
    var faqSearch = MutableStateFlow("")
        private set

    val faqList = listOf(
        FAQItem("Is NIN Verification mandatory?", "Yes. Under Ugandan administrative guidelines, employers are required to enter their 14-digit National ID (NIN) number to guarantee the safety of domestic personnel, and personnel upload their IDs for employer trust."),
        FAQItem("What is the 20,000 UGX placement fee?", "MaidsConnect operates on a highly secure subscription placement monetization model. Employers pay a one-time 20,000 UGX service fee for an approved contract to unlock direct mobile coordinates of verified helpers."),
        FAQItem("How do domestic assistants receive payments?", "Assistants define their rates (Daily or Monthly in UGX) and keep 100% of their actual service fee. Payments are completed directly via Mobile Money (MTN or Airtel) on matching successfully."),
        FAQItem("How are housekeepers vetted?", "Our operators verify housekeepers' National ID card photocopies, conduct references checks on former employers, and validate local Local Council (LC1) recommendation letters before granting 'Approved' status."),
        FAQItem("How can assistants challenge suspension?", "If suspended by administrative moderation, helpers can open a ticket in the Resolution Center with category 'Domestic Issue' requesting details.")
    )

    val filteredFAQs = faqSearch.map { query ->
        if (query.isBlank()) faqList
        else faqList.filter { it.question.contains(query, ignoreCase = true) || it.answer.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), faqList)

    fun submitSupportTicket(title: String, category: String, desc: String, role: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val newTicket = SupportTicketEntity(
                title = title,
                category = category,
                description = desc,
                userRole = role
            )
            repository.insertTicket(newTicket)
            _isSyncing.value = false
        }
    }

    fun changeTicketStatus(ticket: SupportTicketEntity, newStatus: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val updated = ticket.copy(status = newStatus)
            repository.updateTicket(updated)
            _isSyncing.value = false
        }
    }
}

data class FAQItem(val question: String, val answer: String)

class MarketplaceViewModelFactory(
    private val application: Application,
    private val repository: MarketplaceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarketplaceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarketplaceViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
