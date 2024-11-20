package live.jkbx.zeroshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.touchlab.kermit.Logger
import live.jkbx.zeroshare.di.injectLogger
import org.koin.core.component.KoinComponent

class MainActivity : ComponentActivity(), KoinComponent {
    private val log: Logger by injectLogger("MainActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App(log)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
//    App(log)
}