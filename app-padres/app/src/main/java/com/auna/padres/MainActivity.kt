package com.auna.padres

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auna.padres.ui.AunaApp
import com.auna.padres.ui.theme.AunaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AunaTheme {
                val vm: AunaViewModel = viewModel()
                AunaApp(vm)
            }
        }
    }
}
