package com.baguetteui.wtrip.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY id DESC")
    fun observeExpensesByTripId(tripId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY id DESC")
    suspend fun getExpensesByTripId(tripId: Long): List<ExpenseEntity>

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: Long)

    @Query("DELETE FROM expenses WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: Long)

    @Query(
        """
        SELECT tripId AS tripId,
               COUNT(*) AS itemCount,
               SUM(amountCny) AS totalAmount
        FROM expenses
        GROUP BY tripId
        """
    )
    fun observeTripSummaries(): Flow<List<TripSummaryRow>>

    // ---- 新增：统计（你需求的唯一统计）----

    /** 四大类花费汇总（交通/饮食/住宿/体验） */
    @Query(
        """
        SELECT mainType AS mainType,
               SUM(amountCny) AS totalAmountCny
        FROM expenses
        WHERE tripId = :tripId
        GROUP BY mainType
        """
    )
    fun observeSumByMainType(tripId: Long): Flow<List<MainTypeSumRow>>

    /** 支付方式花费汇总（用于饼图） */
    @Query(
        """
        SELECT paymentMethod AS paymentMethod,
               SUM(amountCny) AS totalAmountCny
        FROM expenses
        WHERE tripId = :tripId
        GROUP BY paymentMethod
        """
    )
    fun observeSumByPaymentMethod(tripId: Long): Flow<List<PaymentMethodSumRow>>

    /** 总花费 */
    @Query(
        """
        SELECT SUM(amountCny)
        FROM expenses
        WHERE tripId = :tripId
        """
    )
    fun observeTotalAmount(tripId: Long): Flow<Int?>
}