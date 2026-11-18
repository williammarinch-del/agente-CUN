package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.FirebaseFinancialAdvisorRepository
import com.example.ai.configureAppCheck
import com.example.data.FinanceRepository
import com.example.data.local.CajaCunDatabase
import com.example.data.local.SettingsDataStore
import com.example.ui.CajaCunApp
import com.example.ui.CajaCunViewModel
import com.example.ui.theme.CajaCunTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val database = CajaCunDatabase.getInstance(applicationContext)
        val settingsDataStore = SettingsDataStore(applicationContext)
        val repository = FinanceRepository(
            transactionDao = database.transactionDao(),
            settingsDataStore = settingsDataStore
        )
        val advisorRepository = FirebaseFinancialAdvisorRepository(applicationContext)

        setContent {
            CajaCunTheme {
                val viewModel: CajaCunViewModel = viewModel(
                    factory = CajaCunViewModel.provideFactory(
                        repository = repository,
                        advisorRepository = advisorRepository
                    )
                )
                CajaCunApp(viewModel = viewModel)
            }
        }
    }
}
