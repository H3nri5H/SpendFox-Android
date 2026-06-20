package de.h3nri5h.spendfox

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.h3nri5h.spendfox.ui.SpendFoxApp
import de.h3nri5h.spendfox.ui.SpendFoxViewModel
import de.h3nri5h.spendfox.ui.theme.SpendFoxTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val spendFoxViewModel: SpendFoxViewModel = viewModel(factory = SpendFoxViewModel.Factory)
            val state = spendFoxViewModel.state.collectAsStateWithLifecycle()
            SpendFoxTheme(themeMode = state.value.themeMode) {
                SpendFoxApp(spendFoxViewModel)
            }
        }
    }
}
