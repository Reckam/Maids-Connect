package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HousekeeperDao {
    @Query("SELECT * FROM housekeepers ORDER BY rating DESC, experienceYears DESC")
    fun getAllHousekeepers(): Flow<List<HousekeeperEntity>>

    @Query("SELECT * FROM housekeepers WHERE id = :id LIMIT 1")
    suspend fun getHousekeeperById(id: Int): HousekeeperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHousekeeper(housekeeper: HousekeeperEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHousekeepers(housekeepers: List<HousekeeperEntity>)

    @Update
    suspend fun updateHousekeeper(housekeeper: HousekeeperEntity)

    @Delete
    suspend fun deleteHousekeeper(housekeeper: HousekeeperEntity)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY timestamp DESC")
    fun getAllBookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE housekeeperId = :hkId ORDER BY timestamp DESC")
    fun getBookingsForHousekeeper(hkId: Int): Flow<List<BookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity): Long

    @Update
    suspend fun updateBooking(booking: BookingEntity)

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun deleteBookingById(id: Int)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews ORDER BY timestamp DESC")
    fun getAllReviews(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE housekeeperId = :hkId ORDER BY timestamp DESC")
    fun getReviewsForHousekeeper(hkId: Int): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Update
    suspend fun updateReview(review: ReviewEntity)

    @Delete
    suspend fun deleteReview(review: ReviewEntity)
}

@Dao
interface SupportTicketDao {
    @Query("SELECT * FROM support_tickets ORDER BY timestamp DESC")
    fun getAllTickets(): Flow<List<SupportTicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicketEntity)

    @Update
    suspend fun updateTicket(ticket: SupportTicketEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

