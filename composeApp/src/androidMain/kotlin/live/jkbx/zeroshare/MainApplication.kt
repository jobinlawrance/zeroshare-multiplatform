package live.jkbx.zeroshare

import android.app.Application
import live.jkbx.zeroshare.di.initKoin
import org.koin.dsl.module

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(appModule = module {

        })
    }
}