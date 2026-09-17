package app.projectzero.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawerScreen(modifier: Modifier = Modifier) {
    var searchQuery by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxSize().padding(16.dp).semantics { testTag = "app_drawer" }) {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().semantics { testTag = "app_search_input" },
            label = { Text("Search Apps") }
        )
        Spacer(Modifier.height(16.dp))
        Text("App List Here", modifier = Modifier.semantics { testTag = "app_list" })
    }
}
