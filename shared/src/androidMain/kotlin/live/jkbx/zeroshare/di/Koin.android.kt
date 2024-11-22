package live.jkbx.zeroshare.di

import androidx.credentials.CredentialManager
import live.jkbx.zeroshare.controllers.GoogleAuthProvider
import live.jkbx.zeroshare.controllers.GoogleAuthProviderImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf

import org.koin.dsl.bind
import org.koin.dsl.module

private val googleAuthModule = module {
    factory { CredentialManager.create(androidContext()) } bind CredentialManager::class
    factoryOf(::GoogleAuthProviderImpl) bind GoogleAuthProvider::class
}

actual val platformModule: Module = module{
    includes(googleAuthModule)
}