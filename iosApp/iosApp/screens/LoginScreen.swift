//
//  LoginScreen.swift
//  iosApp
//
//  Created by Jobin Lawrance on 20/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Shared
import GoogleSignIn

struct LoginScreen: View {
    
    lazy var log = koin.loggerWithTag(tag: "LoginScreen")
    @State var descriptionText = ""
    @State var isDescriptionVisible = false
    @State var buttonDisabled = false
    @EnvironmentObject private var navigationManager: NavigationStateManager

    
    var body: some View {
        ZStack {
            // Background Color
            Color.black
                .ignoresSafeArea()
            
            // Main Content
            VStack {
                Spacer()
                
                // Logo
                Image("neural") // Replace with your asset name
                    .resizable()
                    .frame(width: 100, height: 100)
                
                Spacer().frame(height: 16)
                
                // Title
                Text("ZeroShare")
                    .font(.system(size: 48, weight: .bold))
                    .foregroundColor(.white)
                
                Spacer().frame(height: 32)
                
                // Google Login Button
                Button(action: {
                    buttonDisabled = true
                    let loginViewModel = KotlinDependencies().getLoginViewModel()
                    let keyWindow = UIApplication.shared.connectedScenes
                            .filter({$0.activationState == .foregroundActive})
                            .compactMap({$0 as? UIWindowScene})
                            .first?.windows
                            .filter({$0.isKeyWindow}).first

                    guard let rootViewController = keyWindow!.rootViewController else {
                        print("No root view controller")
                            return
                    }
                    GIDSignIn.sharedInstance.signIn(
                        withPresenting: rootViewController) { signInResult, error in
                            guard error == nil else { return }
                                guard let signInResult = signInResult else { return }

                                signInResult.user.refreshTokensIfNeeded { user, error in
                                    guard error == nil else { return }
                                    guard let user = user else { return }

                                    let idToken = user.idToken
                                    // Send ID token to backend (example below).
                                    
                                    Task {
                                        let result = try await loginViewModel.verifyGoogleToken(token: idToken!.tokenString)
                                        let backendApi = KotlinDependencies().getBackendApi()
                                        let deviceResult = try await backendApi.setDeviceDetails(machineName: Utils_nativeKt.getMachineName(), platformName: Utils_nativeKt.getPlatform().name, deviceId: Utils_nativeKt.uniqueDeviceId())
                                        if (deviceResult.boolValue) {
                                            withAnimation {
                                                navigationManager.currentPath = .nebula_setup
                                            }
                                        }
                                    }
                                }
                            
                        }
                    
                }) {
                    HStack {
                        Image("search") // Replace with your asset name
                            .resizable()
                            .frame(width: 24, height: 24)
                        
                        Spacer().frame(width: 8)
                        
                        Text("Login with Google")
                            .foregroundColor(.black)
                    }
                    .padding()
                    .background(Color(hex: "#FFFFB4AC"))
                    .cornerRadius(8)
                    .disabled(buttonDisabled)
                }
                
                Spacer().frame(height: 16)
                
                if(isDescriptionVisible) {
                    Text(descriptionText)
                        .font(.system(size: 14))
                        .foregroundColor(.white)
                }
                
                Spacer()
            }
            
            // Footer
            VStack {
                Spacer()
                
                HStack {
                    Image("nebula") // Replace with your asset name
                        .resizable()
                        .frame(width: 24, height: 24)
                    
                    Spacer().frame(width: 8)
                    
                    Text("Powered by Nebula")
                        .font(.system(size: 14))
                        .foregroundColor(.white)
                }
                .padding(.bottom, 16)
            }
        }
    }
}

struct LoginScreen_Previews: PreviewProvider {
    static var previews: some View {
        LoginScreen()
    }
}


extension Color {
    init(hex: String) {
        let scanner = Scanner(string: hex)
        _ = scanner.scanString("#")
        var hexNumber: UInt64 = 0
        scanner.scanHexInt64(&hexNumber)
        
        let r = Double((hexNumber & 0xFF0000) >> 16) / 255
        let g = Double((hexNumber & 0x00FF00) >> 8) / 255
        let b = Double(hexNumber & 0x0000FF) / 255
        let a = hex.count > 7 ? Double((hexNumber & 0xFF000000) >> 24) / 255 : 1.0
        
        self.init(red: r, green: g, blue: b, opacity: a)
    }
}
