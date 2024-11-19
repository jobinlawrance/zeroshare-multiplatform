//
//  Koin.swift
//  iosApp
//
//  Created by Jobin Lawrance on 19/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Shared

func startKoin() {
    let userDefaults = UserDefaults(suiteName: "ZeroShare_Settings")!
    let doOnStartup = { NSLog("Hello from iOS/Swift!") }
    
    let koinApplication = Koin_nativeKt.doInitKoinIos(
            userDefaults: userDefaults,
            doOnStartup: doOnStartup
        )
    _koin = koinApplication.koin
}

private var _koin: Koin_coreKoin?
var koin: Koin_coreKoin {
    return _koin!
}
