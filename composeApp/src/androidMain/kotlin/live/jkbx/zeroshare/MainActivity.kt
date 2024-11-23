package live.jkbx.zeroshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mmk.kmpnotifier.permission.permissionUtil
import io.github.vinceglb.filekit.core.FileKit
import org.koin.core.component.KoinComponent

class MainActivity : ComponentActivity(), KoinComponent {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
        val permissionUtil by permissionUtil()
        permissionUtil.askNotificationPermission()
        FileKit.init(this)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
//    App(log)
}