//
//  NebulaSetupView.swift
//  iosApp
//
//  Created by Jobin Lawrance on 27/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import SwiftUI
import Shared


struct NebulaSetupView: View {
    @Binding var publicKey: String
    @State var signedPublicKey = ""
    @State var caCertificate = ""
    @State private var isLoading: Bool = true // Loading state
    
    var body: some View {
        
        VStack(spacing: 20) {
            // Title
            Text("Nebula Setup")
                .font(.largeTitle)
                .foregroundColor(.white)
                .bold()
                .padding(.top)
            
            if isLoading {
                // Loading Screen
                VStack {
                    ProgressView("Loading...")
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .foregroundColor(.white)
                        .padding()
                    Text("Processing your data...")
                        .foregroundColor(.white)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color.black.edgesIgnoringSafeArea(.all))
                            
            } else {
                // Signed Public Key Section
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text("Signed Public Key")
                            .font(.headline)
                            .foregroundColor(.white)
                        Spacer()
                        Button(action: {
                            UIPasteboard.general.string = signedPublicKey
                        }) {
                            Image(systemName: "doc.on.doc")
                                .foregroundColor(.blue)
                                .padding(8)
                                .background(Color.white)
                                .clipShape(Circle())
                        }
                    }
                    ScrollView {
                        Text(signedPublicKey)
                            .font(.body)
                            .foregroundColor(.white)
                            .padding()
                            .background(Color.gray.opacity(0.2))
                            .cornerRadius(8)
                            .textSelection(.enabled) // Enable text selection
                    }
                }
                
                // CA Certificate Section
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text("CA Certificate")
                            .font(.headline)
                            .foregroundColor(.white)
                        Spacer()
                        Button(action: {
                            UIPasteboard.general.string = caCertificate
                        }) {
                            Image(systemName: "doc.on.doc")
                                .foregroundColor(.blue)
                                .padding(8)
                                .background(Color.white)
                                .clipShape(Circle())
                        }
                    }
                    ScrollView {
                        Text(caCertificate)
                            .font(.body)
                            .foregroundColor(.white)
                            .padding()
                            .background(Color.gray.opacity(0.2))
                            .cornerRadius(8)
                            .textSelection(.enabled) // Enable text selection
                    }
                }
                
                Spacer()
            }
        }
        .padding()
        .background(Color.black.edgesIgnoringSafeArea(.all))
        .onChange(of: publicKey) { oldValue, newValue in
            if (oldValue != newValue && newValue != "") {
                Task {
                    let backendApi = KotlinDependencies().getBackendApi()
                    let kp = try await backendApi.signPublicKey(publicKey: newValue, deviceId: Utils_nativeKt.uniqueDeviceId())
                    signedPublicKey = kp.signedKey
                    caCertificate = kp.caCert
                    isLoading = false
                }
            }
        }
    }
}
