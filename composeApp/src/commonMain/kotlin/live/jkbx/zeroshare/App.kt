package live.jkbx.zeroshare

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import live.jkbx.zeroshare.screens.LoginScreen
import live.jkbx.zeroshare.screens.NebulaScreen
import live.jkbx.zeroshare.screens.TransferScreen
import live.jkbx.zeroshare.ui.theme.darkScheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = darkScheme,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
//            Navigator(LoginScreen())
            Navigator(TransferScreen())
//            Navigator(NebulaScreen())
        }
    }
}