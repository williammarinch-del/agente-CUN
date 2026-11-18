package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class CajaCunDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: CajaCunDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN timeText TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE transactions ADD COLUMN unitPriceCop INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN discountCop INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN expenseKind TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE transactions SET paymentMethod = 'TRANSFERENCIA' WHERE paymentMethod = 'NEQUI_DAVIPLATA'")
                db.execSQL("UPDATE transactions SET unitPriceCop = amountCop WHERE type = 'VENTA' AND unitPriceCop = 0")
                db.execSQL("UPDATE transactions SET expenseKind = 'VARIABLE' WHERE type = 'GASTO' AND expenseKind = ''")
            }
        }

        fun getInstance(context: Context): CajaCunDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CajaCunDatabase::class.java,
                    "caja_cun_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
