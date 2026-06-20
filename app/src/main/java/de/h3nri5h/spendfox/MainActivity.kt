package de.h3nri5h.spendfox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.h3nri5h.spendfox.ui.SpendFoxApp
import de.h3nri5h.spendfox.ui.theme.SpendFoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpendFoxTheme {
                SpendFoxApp()
            }
        }
    }
}
