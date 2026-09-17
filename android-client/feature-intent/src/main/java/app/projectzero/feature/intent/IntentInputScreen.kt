package app.projectzero.feature.intent

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

@Composable
fun IntentInputScreen(modifier: Modifier = Modifier) {
    var intentQuery by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxSize().padding(16.dp).semantics { testTag = "intent_input_screen" }) {
        TextField(
            value = intentQuery,
            onValueChange = { intentQuery = it },
            modifier = Modifier.fillMaxWidth().semantics { testTag = "intent_input" },
            label = { Text("What do you want to do?") }
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { }, modifier = Modifier.semantics { testTag = "intent_submit" }) {
            Text("Submit")
        }
        Text("Preview / Clarification area", modifier = Modifier.semantics { testTag = "intent_preview" })
    }
}
