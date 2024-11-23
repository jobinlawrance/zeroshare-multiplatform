//
//  PeerScreen.swift
//  iosApp
//
//  Created by Jobin Lawrance on 23/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import SwiftUI
import Shared

struct Member: Identifiable {
    let id: String
    let name: String
    let creationTime: TimeInterval
    let ipAssignments: [String]
    let platform: String
}

struct PeerScreen: View {
    @State private var members: [Member]? = nil
    @State private var isLoading = true
    
    
    var body: some View {
        VStack(alignment: .leading) {
            Text("Peers")
                .font(.largeTitle)
                .fontWeight(.bold)
                .padding(.horizontal)
            
            if isLoading {
                // Show loading indicator
                Spacer()
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                Spacer()
            } else if let members = members {
                // Show members list
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(members) { member in
                            MemberItemView(member: member, isHighlighted: false)
                        }
                    }
                    .padding(.horizontal)
                }
            } else {
                // Show empty state
                Spacer()
                Text("No members available")
                    .foregroundColor(.gray)
                Spacer()
            }
        }
        .background(Color.black.edgesIgnoringSafeArea(.all)) // Dark background
        .onAppear {
            let vm = KotlinDependencies().getZeroTierViewModel()
            Task {
                let peers = try await vm.getZTPeers(networkId: nil)
                members = peers.map {
                    let timeInterval = TimeInterval($0.creationTime) / 1000
                    return Member(id: $0.id, name: $0.name, creationTime: timeInterval, ipAssignments: $0.ipAssignments, platform: $0.platform)
                }
                isLoading = false
            }
        }
    }
    
    
    
    struct MemberItemView: View {
        let member: Member
        let isHighlighted: Bool
        
        var body: some View {
            HStack {
                // Leading avatar
                Image(platformImage)
                    .resizable()
                    .frame(width: 48, height: 48)
                    .padding()
                
                // Member details
                VStack(alignment: .leading, spacing: 4) {
                    Text(member.name)
                        .font(.headline)
                        .foregroundColor(isHighlighted ? .yellow : .white)
                    Text(member.ipAssignments.first ?? "Unknown IP")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                // Timestamp
                Text(formattedDate)
                    .font(.subheadline)
                    .foregroundColor(.gray)
                    .padding(.trailing)
            }
            .padding()
            .background(Color(UIColor.systemGray6))
            .cornerRadius(16)
        }
        
        private var formattedDate: String {
            let date = Date(timeIntervalSince1970: member.creationTime)
            let formatter = DateFormatter()
            formatter.dateFormat = "d MMM, h:mm a"
            return formatter.string(from: date)
        }
        
        private var platformImage: String {
            switch member.platform {
            case let platform where platform.contains("Android"):
                return "android" // Use a system image or replace with custom
            case let platform where platform.contains("iOS"):
                return "apple"
            default:
                return "laptop"
            }
        }
    }
}
