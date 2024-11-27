package live.jkbx.zeroshare.utils

import com.russhwolf.settings.Settings
import live.jkbx.zeroshare.di.tokenKey
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsUtil : KoinComponent {
    val settings by inject<Settings>()

    fun hasAuthKey(): Boolean {
        return settings.getStringOrNull(tokenKey) != null
    }
}