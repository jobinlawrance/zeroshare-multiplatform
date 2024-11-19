package live.jkbx.zeroshare

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import live.jkbx.zeroshare.di.initKoin
import org.koin.dsl.module

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(appModule = module {
            single<Context> {this@MainApplication}
            single<SharedPreferences> {
                get<Context>().getSharedPreferences(
                    "ZeroShare_SETTINGS",
                    Context.MODE_PRIVATE
                )
            }
            single<Settings> {
                SharedPreferencesSettings(get<SharedPreferences>())
            }


        })
    }
}