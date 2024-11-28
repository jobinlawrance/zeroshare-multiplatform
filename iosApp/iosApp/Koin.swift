//
//  Koin.swift
//  iosApp
//
//  Created by Jobin Lawrance on 19/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Shared
import CryptoKit


func startKoin() {
    let userDefaults = UserDefaults(suiteName: "ZeroShare_Settings")!
    let doOnStartup = { NSLog("Hello from iOS/Swift!") }
    
    let koinApplication = Koin_nativeKt.doInitKoinIos(
            userDefaults: userDefaults,
            doOnStartup: doOnStartup,
            kmpHashing: KmpHashingIOSImpl()
        )
    _koin = koinApplication.koin
}

private var _koin: Koin_coreKoin?
var koin: Koin_coreKoin {
    return _koin!
}

class KmpHashingIOSImpl: KmpHashing {
    func getSha256Hash(bytesArray: KotlinByteArray) -> String {
        let fileSaver = KotlinDependencies().getFileSaver()
        let nsData = fileSaver.getNSData(bytesArray: bytesArray)
        let digest = SHA256.hash(data: nsData)
            let hashString = digest
                .compactMap { String(format: "%02x", $0) }
                .joined()
            return hashString
    }
}
