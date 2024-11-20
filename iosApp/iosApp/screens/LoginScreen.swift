//
//  LoginScreen.swift
//  iosApp
//
//  Created by Jobin Lawrance on 20/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

struct LoginScreen: View {
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
                    // TODO: Handle Google login
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
                }
                
                Spacer()
            }
            
            // Footer
            VStack {
                Spacer()
                
                HStack {
                    Image("zerotier") // Replace with your asset name
                        .resizable()
                        .frame(width: 24, height: 24)
                    
                    Spacer().frame(width: 8)
                    
                    Text("Powered by ZeroTier")
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
