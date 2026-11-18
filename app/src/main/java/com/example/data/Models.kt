package com.example.data

enum class TransactionType(val label: String, val pluralLabel: String) {
    VENTA("Venta", "Ventas"),
    GASTO("Gasto", "Gastos")
}

enum class PaymentMethod(val label: String) {
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia"),
    TARJETA("Tarjeta"),
    OTRO("Otro");

    companion object {
        fun fromName(name: String): PaymentMethod {
            return when (name) {
                "NEQUI_DAVIPLATA" -> TRANSFERENCIA
                else -> entries.firstOrNull { it.name == name } ?: EFECTIVO
            }
        }
    }
}

enum class ExpenseKind(val label: String) {
    FIJO("Fijo"),
    VARIABLE("Variable");

    companion object {
        fun fromName(name: String): ExpenseKind {
            return entries.firstOrNull { it.name == name } ?: VARIABLE
        }
    }
}

object TransactionCategories {
    val saleCategories = listOf(
        "Productos",
        "Servicios",
        "Venta de contado",
        "Pedidos / Domicilios",
        "Otros ingresos"
    )

    val expenseCategories = listOf(
        "Arriendo",
        "Servicios públicos",
        "Nómina",
        "Compras",
        "Transporte",
        "Publicidad",
        "Mantenimiento",
        "Otros"
    )

    val allCategories: List<String> = (saleCategories + expenseCategories).distinct()

    fun forType(type: TransactionType): List<String> {
        return when (type) {
            TransactionType.VENTA -> saleCategories
            TransactionType.GASTO -> expenseCategories
        }
    }
}

data class FinanceTransaction(
    val id: Long = 0,
    val type: TransactionType,
    val amountCop: Long,
    val concept: String,
    val category: String,
    val paymentMethod: PaymentMethod,
    val dateEpochDay: Long,
    val timeText: String = "",
    val quantity: Int = 1,
    val unitPriceCop: Long = 0L,
    val discountCop: Long = 0L,
    val expenseKind: ExpenseKind? = null,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class BusinessSettings(
    val businessName: String = "Mi Negocio",
    val businessType: String = "Comercio / Microempresa",
    val ownerName: String = "",
    val dailySalesGoalCop: Long = 0L,
    val currencyCode: String = "COP"
)
