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
import zt

struct LoginScreen: View {
    
    lazy var log = koin.loggerWithTag(tag: "LoginScreen")
    @State var descriptionText = ""
    @State var isDescriptionVisible = false
    
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
                    let zeroTierViewModel = KotlinDependencies().getZeroTierViewModel()
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
                                        let result = try await zeroTierViewModel.verifyGoogleToken(token: idToken!.tokenString)
                                        isDescriptionVisible = true
                                        descriptionText = "Conntecting to ZeroTier network - \(result.networkId)"
                                        try await connectToZTNetwork(result.networkId, onNodeCreated: { nodeId in
                                            Task {
                                                try await zeroTierViewModel.setNodeId(nodeId: nodeId, machineName: getMachineName(), networkId: result.networkId)
                                                descriptionText = "Connected to \(result.networkId)"
                                            }
                                        })
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

enum ZeroTierError: Error {
    case nodeStartFailure
    case nodeOffline
    case networkJoinFailure
    case networkTimeout
}

var isConnected = false

func eventHandler(msgPtr: UnsafeMutableRawPointer?) {
    guard let msg = msgPtr?.bindMemory(to: zts_event_msg_t.self, capacity: 1) else { return }
    let eventCode = zts_event_t(rawValue: UInt32(msg.pointee.event_code))
    
    if eventCode == ZTS_EVENT_ADDR_ADDED_IP4 {
        // Mark connection as successful
        isConnected = true
    }
}

func connectToZTNetwork(_ networkId: String, onNodeCreated: (_ nodeId: String) -> Void) async throws -> String {
    
    lazy var log = koin.loggerWithTag(tag: "ZeroTier")
    // Convert network ID from string to UInt64
    guard let nwid = UInt64(networkId.replacingOccurrences(of: ":", with: ""), radix: 16) else {
        throw ZeroTierError.networkJoinFailure
    }

     // This can be a global variable to track the connection status
    

    
    // Set up event handler
    zts_init_set_event_handler(eventHandler)  // Pass the C-style function pointer

    var storagePath: String

    if let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
        // Create a ZeroTier subdirectory
        let ztPath = documentsPath.appendingPathComponent("zerotier")
        
        // Create directory if it doesn't exist
        try? FileManager.default.createDirectory(at: ztPath,
                                                 withIntermediateDirectories: true,
                                                 attributes: nil)
        
        storagePath = ztPath.path
    } else {
        // Fallback to temporary directory if documents directory is not available
        log.w(message: {"Falling back to temporary directory"})
        storagePath = NSTemporaryDirectory().appending("zerotier")
    }
    
    // Initialize and start node
    zts_init_set_port(9993)
    zts_init_from_storage(storagePath)
    let startResult = zts_node_start()
    if startResult != 0 {
        throw ZeroTierError.nodeStartFailure
    }
    
    // Wait for node to come online
    var waitTime = 0
    while zts_node_is_online() != 1 {
        if waitTime >= 30 {
            throw ZeroTierError.nodeOffline
        }
        try await Task.sleep(nanoseconds: 1 * 1_000_000_000) // Wait 1 second asynchronously
        waitTime += 1
    }
    
    onNodeCreated(String(format: "%llX", zts_node_get_id()))
    
    // Join network
    let joinResult = zts_net_join(nwid)
    if joinResult != 0 {
        throw ZeroTierError.networkJoinFailure
    }
    
    // Wait for successful network connection
    waitTime = 0
    while !isConnected {
        if waitTime >= 30 {
            throw ZeroTierError.networkTimeout
        }
        try await Task.sleep(nanoseconds: 1 * 1_000_000_000) // Wait 1 second asynchronously
        waitTime += 1
    }
    
    // Return the node ID as a hexadecimal string
    return String(format: "%llX", zts_node_get_id())
}
