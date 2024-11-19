package live.jkbx.zeroshare

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import live.jkbx.zeroshare.di.initKoin
import org.koin.dsl.module

fun main() = application {
    initKoin(module {  })
    Window(
        onCloseRequest = ::exitApplication,
        title = "ZeroShare",
    ) {
        App()
    }
}