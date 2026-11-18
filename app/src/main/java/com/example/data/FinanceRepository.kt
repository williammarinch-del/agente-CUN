package com.example.data

import com.example.data.local.SettingsDataStore
import com.example.data.local.TransactionDao
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val settingsDataStore: SettingsDataStore
) {
    val transactions: Flow<List<FinanceTransaction>> =
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    val settings: Flow<BusinessSettings> = settingsDataStore.settingsFlow

    suspend fun addTransaction(transaction: FinanceTransaction): Long {
        return transactionDao.insert(transaction.toEntity())
    }

    suspend fun updateTransaction(transaction: FinanceTransaction) {
        transactionDao.update(transaction.toEntity())
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun updateSettings(settings: BusinessSettings) {
        settingsDataStore.updateSettings(settings)
    }

    suspend fun clearAllLocalData() {
        transactionDao.deleteAll()
        settingsDataStore.resetSettings()
    }
}
