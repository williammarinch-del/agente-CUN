package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.ExpenseKind
import com.example.data.FinanceTransaction
import com.example.data.PaymentMethod
import com.example.data.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amountCop: Long,
    val concept: String,
    val category: String,
    val paymentMethod: String,
    val dateEpochDay: Long,
    val timeText: String = "",
    val quantity: Int = 1,
    val unitPriceCop: Long = 0L,
    val discountCop: Long = 0L,
    val expenseKind: String = "",
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

fun TransactionEntity.toDomain(): FinanceTransaction {
    val txType = if (type == TransactionType.GASTO.name) TransactionType.GASTO else TransactionType.VENTA
    val parsedKind = if (txType == TransactionType.GASTO) {
        ExpenseKind.fromName(expenseKind)
    } else null
    val resolvedUnitPrice = if (txType == TransactionType.VENTA && unitPriceCop <= 0L) {
        amountCop
    } else {
        unitPriceCop
    }
    return FinanceTransaction(
        id = id,
        type = txType,
        amountCop = amountCop,
        concept = concept,
        category = category,
        paymentMethod = PaymentMethod.fromName(paymentMethod),
        dateEpochDay = dateEpochDay,
        timeText = timeText,
        quantity = quantity.coerceAtLeast(1),
        unitPriceCop = resolvedUnitPrice,
        discountCop = discountCop.coerceAtLeast(0L),
        expenseKind = parsedKind,
        notes = notes,
        createdAtMillis = createdAtMillis
    )
}

fun FinanceTransaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        type = type.name,
        amountCop = amountCop,
        concept = concept,
        category = category,
        paymentMethod = paymentMethod.name,
        dateEpochDay = dateEpochDay,
        timeText = timeText,
        quantity = quantity,
        unitPriceCop = unitPriceCop,
        discountCop = discountCop,
        expenseKind = expenseKind?.name ?: "",
        notes = notes,
        createdAtMillis = createdAtMillis
    )
}
