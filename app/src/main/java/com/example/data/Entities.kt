package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "housekeepers")
data class HousekeeperEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val avatarEmoji: String,
    val phone: String,
    val bio: String,
    val rateUGX: Int,
    val rateType: String, // "Daily" or "Monthly"
    val skills: String, // Comma-separated: e.g. "Infant Nursing,Deep Cleaning"
    val languages: String, // Comma-separated: e.g. "English,Luganda,Swahili"
    val district: String,
    val county: String,
    val subcounty: String,
    val village: String,
    val experienceYears: Int,
    val rating: Float,
    val reviewCount: Int,
    val vettingStatus: String, // "Approved", "Pending Approval", "Suspended", "Deactivated"
    val isAvailable: Boolean = true,
    val nin: String, // Ugandan National ID (NIN)
    val profileImageUri: String? = null
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val housekeeperId: Int,
    val housekeeperName: String,
    val employerName: String,
    val employerPhone: String,
    val employerNIN: String,
    val startDate: String,
    val durationDays: Int,
    val totalCostUGX: Int,
    val placementFeePaid: Boolean = false,
    val paymentReference: String = "",
    val status: String = "Pending", // "Pending", "Confirmed", "Declined", "Completed"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val housekeeperId: Int,
    val bookingId: Int,
    val employerName: String,
    val rating: Int, // 1 to 5
    val comment: String,
    val isFlagged: Boolean = false,
    val isHidden: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // "Booking", "Payment", "Domestic Issue"
    val description: String,
    val status: String = "Open", // "Open", "InProgress", "Resolved"
    val userRole: String, // "Client" or "Housekeeper"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val nin: String,
    val phoneNumber: String,
    val emailAddress: String,
    val role: String, // "Client", "Housekeeper", "Admin"
    val district: String,
    val subcounty: String,
    val village: String,
    val isVerified: Boolean = true,
    val isSuspended: Boolean = false
)

