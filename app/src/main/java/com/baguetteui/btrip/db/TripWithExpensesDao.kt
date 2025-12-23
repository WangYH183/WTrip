package com.baguetteui.btrip.db

import androidx.room.Dao
import androidx.room.Transaction

@Dao
interface TripWithExpensesDao {

    fun tripDao(): TripDao
    fun expenseDao(): ExpenseDao

    @Transaction
    suspend fun deleteTripAndExpenses(tripId: Long) {
        expenseDao().deleteByTripId(tripId)
        tripDao().deleteTripById(tripId)
    }
}