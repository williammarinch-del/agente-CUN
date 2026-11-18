package com.example.ai

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "FinancialAdvisorRepo"

data class AdvisorMessageTurn(
    val isUser: Boolean,
    val text: String
)

interface FinancialAdvisorRepository {
    fun streamAdvice(
        userQuestion: String,
        financialSnapshot: String,
        history: List<AdvisorMessageTurn>
    ): Flow<String>
}

class FirebaseFinancialAdvisorRepository(
    private val appContext: Context
) : FinancialAdvisorRepository {

    private val systemPrompt = content {
        text(
            """
            Eres el "Asistente Financiero CAJA CUN", un asesor financiero especializado en control de caja, análisis de ingresos, gastos, resultados operativos y administración básica de pequeños negocios y microempresas en Colombia.

            REGLAS ESTRICTAS DE OBLIGATORIO CUMPLIMIENTO:
            1. Responde SIEMPRE en español claro, cercano, profesional y práctico.
            2. Utiliza EXCLUSIVAMENTE los datos reales entregados en el bloque "CONTEXTO FINANCIERO REAL DE CAJA CUN".
            3. NUNCA inventes ventas, gastos, saldos, fechas, categorías ni resultados. Si no hay datos suficientes para responder algo, informa explícitamente al usuario qué datos faltan.
            4. Diferencia SIEMPRE con claridad entre ingresos (ventas), gastos y resultado operativo (ingresos menos gastos).
            5. Expresa todas las cifras en pesos colombianos (COP), por ejemplo: $ 150.000 COP.
            6. Explica los cálculos de forma sencilla (mostrando sumas, restas o porcentajes cuando ayuden al negocio).
            7. NUNCA modifiques, elimines ni registres movimientos por iniciativa propia ni afirmes haber ejecutado acciones financieras; eres un asesor de consulta y análisis.
            8. Basa cada recomendación en los datos reales disponibles. Si detectas inconsistencias, márgenes negativos o incrementos inusuales en los gastos, advierte al usuario con claridad.
            """.trimIndent()
        )
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = systemPrompt
        )
    }

    override fun streamAdvice(
        userQuestion: String,
        financialSnapshot: String,
        history: List<AdvisorMessageTurn>
    ): Flow<String> = flow {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            throw IllegalStateException(
                "Firebase AI Logic no está configurado todavía en este proyecto (falta aprovisionar google-services.json en la cuenta)."
            )
        }

        val chatHistory: List<Content> = history
            .takeLast(8)
            .map { turn ->
                content(role = if (turn.isUser) "user" else "model") {
                    text(turn.text)
                }
            }

        val promptWithFreshContext = """
            $financialSnapshot

            === PREGUNTA DEL USUARIO ===
            $userQuestion
        """.trimIndent()

        try {
            val accumulated = StringBuilder()
            withTimeout(60_000L) {
                val chat = model.startChat(history = chatHistory)
                chat.sendMessageStream(promptWithFreshContext).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        accumulated.append(chunkText)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Gemini devolvió una respuesta vacía. Intenta formular la pregunta nuevamente.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini stream timed out after 60s", e)
            throw IllegalStateException("La consulta tardó más de 60 segundos. Por favor intenta de nuevo.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic error", e)
            throw e
        }
    }
}
