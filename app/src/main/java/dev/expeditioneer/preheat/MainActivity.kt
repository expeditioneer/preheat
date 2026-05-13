package dev.expeditioneer.preheat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HeaterRcTheme {
                PreheatApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreheatApp() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.tab_home_name)) })
        },
        modifier = Modifier.fillMaxSize(),
    ) { inner ->
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) { _ ->
            StandHeizungScreen()
        }
    }
}
