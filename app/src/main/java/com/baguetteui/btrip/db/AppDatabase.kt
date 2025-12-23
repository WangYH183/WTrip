package com.baguetteui.btrip.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

@Database(
    entities = [TripEntity::class, ExpenseEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun expenseDao(): ExpenseDao

    @Transaction
    open suspend fun deleteTripAndExpenses(tripId: Long) {
        expenseDao().deleteByTripId(tripId)
        tripDao().deleteTripById(tripId)
    }

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trips_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        days INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO trips_new (id, title, days) SELECT id, title, days FROM trips")
                db.execSQL("DROP TABLE trips")
                db.execSQL("ALTER TABLE trips_new RENAME TO trips")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expenses_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tripId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        amount INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO expenses_new (id, tripId, title, amount) SELECT id, tripId, title, amount FROM expenses")
                db.execSQL("DROP TABLE expenses")
                db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expenses_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tripId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        FOREIGN KEY(tripId) REFERENCES trips(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO expenses_new (id, tripId, title, amount) SELECT id, tripId, title, amount FROM expenses")
                db.execSQL("DROP TABLE expenses")
                db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_tripId ON expenses(tripId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expenses_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tripId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        category TEXT NOT NULL DEFAULT '其他',
                        FOREIGN KEY(tripId) REFERENCES trips(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO expenses_new (id, tripId, title, amount, category)
                    SELECT id, tripId, title, amount, '其他' FROM expenses
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE expenses")
                db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_tripId ON expenses(tripId)")
            }
        }

        // 4->5: 增加 dateEpochDay（默认今天），并保留外键/索引
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val todayEpochDay = LocalDate.now().toEpochDay()

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expenses_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tripId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        category TEXT NOT NULL DEFAULT '其他',
                        dateEpochDay INTEGER NOT NULL DEFAULT $todayEpochDay,
                        FOREIGN KEY(tripId) REFERENCES trips(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO expenses_new (id, tripId, title, amount, category, dateEpochDay)
                    SELECT id, tripId, title, amount, category, $todayEpochDay FROM expenses
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE expenses")
                db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_tripId ON expenses(tripId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_tripId_dateEpochDay ON expenses(tripId, dateEpochDay)")
            }
        }

        // 5->6: 重构 expenses 表以支持四大类/支付方式/时间/单图等
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 旧表字段：id, tripId, title, amount, category, dateEpochDay
                // 新表字段：id, tripId, title, amountCny, mainType, paymentMethod, ...
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expenses_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tripId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        amountCny INTEGER NOT NULL,
                        mainType TEXT NOT NULL,
                        paymentMethod TEXT NOT NULL,
                        reservationPlatform TEXT,
                        shortReview TEXT,
                        photoUri TEXT,
                        eventTimeMillis INTEGER,
                        startTimeMillis INTEGER,
                        endTimeMillis INTEGER,
                        subType TEXT,
                        route TEXT,
                        durationHHmm TEXT,
                        dateEpochDay INTEGER NOT NULL,
                        FOREIGN KEY(tripId) REFERENCES trips(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // 迁移策略：
                // - amount -> amountCny
                // - mainType 默认 EXPERIENCE（因为旧的 category 更像“类别/备注”，先当作 subType 保存）
                // - paymentMethod 无法推断，默认 CASH（你若想默认微信/支付宝，告诉我）
                // - subType = 旧 category
                // - 其它字段置空
                db.execSQL(
                    """
                    INSERT INTO expenses_new (
                        id, tripId, title, amountCny, mainType, paymentMethod,
                        reservationPlatform, shortReview, photoUri,
                        eventTimeMillis, startTimeMillis, endTimeMillis,
                        subType, route, durationHHmm,
                        dateEpochDay
                    )
                    SELECT
                        id,
                        tripId,
                        title,
                        amount AS amountCny,
                        'EXPERIENCE' AS mainType,
                        'CASH' AS paymentMethod,
                        NULL AS reservationPlatform,
                        NULL AS shortReview,
                        NULL AS photoUri,
                        NULL AS eventTimeMillis,
                        NULL AS startTimeMillis,
                        NULL AS endTimeMillis,
                        category AS subType,
                        NULL AS route,
                        NULL AS durationHHmm,
                        dateEpochDay
                    FROM expenses
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE expenses")
                db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_tripId ON expenses(tripId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_tripId_dateEpochDay ON expenses(tripId, dateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_tripId_mainType ON expenses(tripId, mainType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_tripId_paymentMethod ON expenses(tripId, paymentMethod)")
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "btrip.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}