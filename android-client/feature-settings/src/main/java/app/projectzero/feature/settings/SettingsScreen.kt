package app.projectzero.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var cloudOptIn by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize().padding(16.dp).semantics { testTag = "settings_screen" }) {
        Text("Settings & Privacy", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Row {
            Text("Cloud Reasoning Opt-In")
            Switch(
                checked = cloudOptIn,
                onCheckedChange = { cloudOptIn = it },
                modifier = Modifier.semantics { testTag = "cloud_opt_in_switch" }
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { }, modifier = Modifier.semantics { testTag = "delete_data_button" }) {
            Text("Delete All Data")
        }
    }
}
