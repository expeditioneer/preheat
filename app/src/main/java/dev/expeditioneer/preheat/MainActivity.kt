package dev.expeditioneer.preheat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HeaterRcTheme {
                Surface {
                    MainScreen()
                }
            }
        }
    }
}

private data class TabItem(
    val label: String,
    val icon: ImageVector,
    val content: @Composable () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen() {
    val tabs = listOf(
        TabItem(
            label = stringResource(R.string.tab_config_name),
            icon = Icons.Default.Settings,
            content = { ConfigScreen() },
        ),
        TabItem(
            label = stringResource(R.string.tab_home_name),
            icon = Icons.Default.LocalFireDepartment,
            content = { StandHeizungScreen() },
        ),
    )

    var selectedIndex by remember { mutableIntStateOf(1) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(tabs[selectedIndex].label) })
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = selectedIndex == i,
                        onClick = { selectedIndex = i },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            tabs[selectedIndex].content()
        }
    }
}
