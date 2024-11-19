package live.jkbx.zeroshare.di

import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.core.scope.get
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<Settings> { PropertiesSettings(get()) }
}