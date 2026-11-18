package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class FinancialColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val warning: Color,
    val warningContainer: Color
)

val LocalFinancialColors = staticCompositionLocalOf {
    FinancialColors(
        income = IncomeGreen,
        incomeContainer = IncomeGreenContainer,
        expense = ExpenseRed,
        expenseContainer = ExpenseRedContainer,
        warning = WarningAmber,
        warningContainer = WarningAmberContainer
    )
}

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmeraldPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = OnEmeraldPrimaryContainer,
    secondary = SlateGreenSecondary,
    onSecondary = OnSlateGreenSecondary,
    secondaryContainer = SlateGreenSecondaryContainer,
    onSecondaryContainer = OnSlateGreenSecondaryContainer,
    tertiary = MintTertiary,
    onTertiary = OnMintTertiary,
    tertiaryContainer = MintTertiaryContainer,
    onTertiaryContainer = OnMintTertiaryContainer,
    background = NeutralBackground,
    onBackground = OnNeutralBackground,
    surface = PureWhiteSurface,
    onSurface = OnPureWhiteSurface,
    surfaceVariant = GraySurfaceVariant,
    onSurfaceVariant = OnGraySurfaceVariant,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    outline = GrayOutline,
    outlineVariant = GrayOutlineVariant,
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = ExpenseRedContainer,
    onErrorContainer = ExpenseRed
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkOnEmeraldPrimary,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkOnEmeraldContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkEmeraldPrimary,
    onTertiary = DarkOnEmeraldPrimary,
    tertiaryContainer = DarkIncomeContainer,
    onTertiaryContainer = DarkIncomeGreen,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = Color(0xFF2B3832),
    outline = DarkOutline,
    outlineVariant = Color(0xFF2E3A35),
    error = DarkExpenseRed,
    onError = Color(0xFF410E0B),
    errorContainer = DarkExpenseContainer,
    onErrorContainer = DarkExpenseRed
)

private val CajaCunShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun CajaCunTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val financialColors = if (darkTheme) {
        FinancialColors(
            income = DarkIncomeGreen,
            incomeContainer = DarkIncomeContainer,
            expense = DarkExpenseRed,
            expenseContainer = DarkExpenseContainer,
            warning = Color(0xFFFFCA7A),
            warningContainer = Color(0xFF3E2B0C)
        )
    } else {
        FinancialColors(
            income = IncomeGreen,
            incomeContainer = IncomeGreenContainer,
            expense = ExpenseRed,
            expenseContainer = ExpenseRedContainer,
            warning = WarningAmber,
            warningContainer = WarningAmberContainer
        )
    }

    CompositionLocalProvider(LocalFinancialColors provides financialColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CajaCunTypography,
            shapes = CajaCunShapes,
            content = content
        )
    }
}
