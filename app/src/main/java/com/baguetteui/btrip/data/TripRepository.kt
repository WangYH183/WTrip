package com.baguetteui.btrip.data

import android.content.Context
import com.baguetteui.btrip.Expense
import com.baguetteui.btrip.MainType
import com.baguetteui.btrip.PaymentMethod
import com.baguetteui.btrip.Trip
import com.baguetteui.btrip.db.AppDatabase
import com.baguetteui.btrip.db.ExpenseEntity
import com.baguetteui.btrip.db.TripEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TripRepository(context: Context) {

    private val db = AppDatabase.get(context)
    private val tripDao = db.tripDao()
    private val expenseDao = db.expenseDao()

    fun observeTrips(): Flow<List<Trip>> =
        tripDao.observeAllTrips()
            .map { list -> list.map { Trip(it.id, it.title, it.days) } }

    suspend fun getTripsOnce(): List<Trip> =
        tripDao.getAllTrips().map { Trip(it.id, it.title, it.days) }

    suspend fun getTrip(tripId: Long): Trip? =
        tripDao.getTripById(tripId)?.let { Trip(it.id, it.title, it.days) }

    suspend fun createTrip(title: String, days: Int): Long {
        return tripDao.insertTrip(TripEntity(title = title, days = days))
    }

    suspend fun upsertTrip(trip: Trip) {
        tripDao.upsertTrip(TripEntity(id = trip.id, title = trip.title, days = trip.days))
    }

    suspend fun deleteTrip(tripId: Long) {
        db.deleteTripAndExpenses(tripId)
    }

    fun observeExpenses(tripId: Long): Flow<List<Expense>> =
        expenseDao.observeExpensesByTripId(tripId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun createExpense(
        tripId: Long,
        title: String,
        amountCny: Int,
        mainType: MainType,
        paymentMethod: PaymentMethod,
        dateEpochDay: Long = LocalDate.now().toEpochDay(),

        // 可选字段
        reservationPlatform: String? = null,
        shortReview: String? = null,
        photoUri: String? = null,

        // 时间字段
        eventTimeMillis: Long? = null,
        startTimeMillis: Long? = null,
        endTimeMillis: Long? = null,

        // 子类别/交通字段
        subType: String? = null,
        route: String? = null,
        durationHHmm: String? = null
    ): Long {
        return expenseDao.insertExpense(
            ExpenseEntity(
                tripId = tripId,
                title = title,
                amountCny = amountCny,
                mainType = mainType.name,
                paymentMethod = paymentMethod.name,
                reservationPlatform = reservationPlatform,
                shortReview = shortReview,
                photoUri = photoUri,
                eventTimeMillis = eventTimeMillis,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                subType = subType,
                route = route,
                durationHHmm = durationHHmm,
                dateEpochDay = dateEpochDay
            )
        )
    }

    suspend fun upsertExpense(expense: Expense) {
        expenseDao.upsertExpense(expense.toEntity())
    }

    suspend fun deleteExpense(expenseId: Long) {
        expenseDao.deleteById(expenseId)
    }

    fun observeTripSummaries(): Flow<Map<Long, Pair<Int, Int?>>> =
        expenseDao.observeTripSummaries()
            .map { rows -> rows.associate { it.tripId to (it.itemCount to it.totalAmount) } }

    // -------- 新增：统计（你需求的唯一统计）--------

    /**
     * 返回：
     * - mainTypeTotals: Map<MainType, Int>
     * - total: Int
     */
    fun observeMainTypeTotals(tripId: Long): Flow<Pair<Map<MainType, Int>, Int>> {
        val mainTypeFlow = expenseDao.observeSumByMainType(tripId).map { rows ->
            val base = MainType.entries.associateWith { 0 }.toMutableMap()
            for (r in rows) {
                val key = runCatching { MainType.valueOf(r.mainType) }.getOrNull() ?: continue
                base[key] = r.totalAmountCny ?: 0
            }
            base.toMap()
        }

        val totalFlow = expenseDao.observeTotalAmount(tripId).map { it ?: 0 }

        return combine(mainTypeFlow, totalFlow) { m, t -> m to t }
    }

    /**
     * 支付方式统计（用于饼图）
     */
    fun observePaymentMethodTotals(tripId: Long): Flow<Map<PaymentMethod, Int>> {
        return expenseDao.observeSumByPaymentMethod(tripId).map { rows ->
            val base = PaymentMethod.entries.associateWith { 0 }.toMutableMap()
            for (r in rows) {
                val key = runCatching { PaymentMethod.valueOf(r.paymentMethod) }.getOrNull() ?: continue
                base[key] = r.totalAmountCny ?: 0
            }
            base.toMap()
        }
    }

    // ---------- mapping ----------

    private fun ExpenseEntity.toDomain(): Expense {
        val mainTypeEnum = runCatching { MainType.valueOf(mainType) }.getOrDefault(MainType.EXPERIENCE)
        val paymentEnum = runCatching { PaymentMethod.valueOf(paymentMethod) }.getOrDefault(PaymentMethod.CASH)

        return Expense(
            id = id,
            tripId = tripId,
            title = title,
            amountCny = amountCny,
            mainType = mainTypeEnum,
            paymentMethod = paymentEnum,
            reservationPlatform = reservationPlatform,
            shortReview = shortReview,
            photoUri = photoUri,
            eventTimeMillis = eventTimeMillis,
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            subType = subType,
            route = route,
            durationHHmm = durationHHmm,
            dateEpochDay = dateEpochDay
        )
    }

    private fun Expense.toEntity(): ExpenseEntity {
        return ExpenseEntity(
            id = id,
            tripId = tripId,
            title = title,
            amountCny = amountCny,
            mainType = mainType.name,
            paymentMethod = paymentMethod.name,
            reservationPlatform = reservationPlatform,
            shortReview = shortReview,
            photoUri = photoUri,
            eventTimeMillis = eventTimeMillis,
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            subType = subType,
            route = route,
            durationHHmm = durationHHmm,
            dateEpochDay = dateEpochDay
        )
    }
}