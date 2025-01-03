//
//  TransferScreen.swift
//  iosApp
//
//  Created by Jobin Lawrance on 28/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//


import SwiftUI
import Foundation

struct TransferScreen: View {
    @StateObject private var fileTransferManager = FileTransferManager()
    @State private var isExporting = false
    
    @State private var showDialog = false
    @State private var showIncomingDialog = false
    @State private var selectedOption: DropdownItem?
    @State private var selectedFileMeta: FileTransferMetadata?
    @State private var selectedFile: PlatformFile?
    @State private var incomingFile: FileTransferMetadata?
    @State private var incomingDevice: Device?
    @State private var dialogItems: [DropdownItem] = []
    @State private var defaultDevice: Device?
    @State private var selectedDevice: Device? = nil

//    var body: some View {
//        VStack {
//            Text("File Transfer")
//                .font(.title)
//                .padding()
//
//            Button(action: {
//                isExporting = true
//                
//            }) {
//                Text("Send File")
//                    .padding()
//                    .background(Color.blue)
//                    .foregroundColor(.white)
//                    .cornerRadius(8)
//            }.fileImporter(isPresented: $isExporting, allowedContentTypes: [.item] ) { result in
//                switch result {
//                case .success(let file):
//                    print(file.absoluteString)
//                    Task {
//                        let fileWrapper = try FileWrapper(url: file)
//                        fileTransferManager.sendFile(file: fileWrapper)
//                    }
//                case .failure(let error):
//                    print(error.localizedDescription)
//                }
//            }
//        }
//        .onAppear {
////            fileTransferManager.startServer()
//        }
//    }
    
    
       var body: some View {
           ZStack {
               Color.black
                   .edgesIgnoringSafeArea(.all)
               
               VStack(spacing: 20) {
                   // Title
                   Text("File Sharing")
                       .font(.custom("AvenirNext-Bold", size: 36))
                       .foregroundColor(.white)
                   
                   Spacer()
                   
                   // File selection area
                   Button(action: {
                       showFilePicker()
                   }) {
                       HStack {
                           Image("flames")
                               .resizable()
                               .frame(width: 48, height: 48)
                               
                           Text("Click to select a file")
                               .padding()
                               .font(.custom("AvenirNext-Regular", size: 24))
                               .foregroundColor(.white)
                            
                       }
                       .frame(width: 560) // Similar to width(560.dp)
                       .background(Color.gray.opacity(0.2))
                       .cornerRadius(12)
                   }
                   .padding(.horizontal, 16)
                   
                   
                   Spacer()
                   
                   // Device selection title
                   Text("Select device to Send")
                       .font(.custom("AvenirNext-Regular", size: 24))
                       .foregroundColor(.white)
                   
                   // Selected device
                   Button(action: {
                       // Handle device selection
//                       selectedDevice = Device(name: "Jobin's iPhone 16 Simulator", ipAddress: "69.69.0.2")
                   }) {
                       HStack {
                           Image(systemName: "applelogo")
                               .foregroundColor(.red)
                               .font(.largeTitle)
                           VStack(alignment: .leading) {
//                               Text(selectedDevice?.name ?? "Select a device")
//                                   .foregroundColor(.white)
//                                   .font(.title3)
                               if let ipAddress = selectedDevice?.ipAddress {
                                   Text(ipAddress)
                                       .foregroundColor(.gray)
                                       .font(.subheadline)
                               }
                           }
                           Spacer()
                       }
                       .frame(width: 560)
                       .background(Color.gray.opacity(0.2))
                       .cornerRadius(12)
                   }
                   .padding(.horizontal, 16)
                   
                   Spacer()
                   
                   // Send button
                   Button(action: {
                       // Handle send action
                       print("Send tapped")
                   }) {
                       HStack {
                           Text("Send")
                               .foregroundColor(.white)
                               .font(.title3)
                           Image(systemName: "paperplane.fill")
                               .foregroundColor(.purple)
                       }
                       .padding()
                       .frame(maxWidth: 200)
                       .background(selectedFile != nil && selectedDevice != nil ? Color.gray.opacity(0.2) : Color.gray.opacity(0.1))
                       .cornerRadius(12)
                   }
                   .disabled(selectedFile == nil || selectedDevice == nil)
                   
                   Spacer()
               }
               .padding()
           }
       }
    
    
    
    private func showFilePicker(){
        
    }
    
     
     private func sendFile() {
         guard let fileMeta = selectedFileMeta, let option = selectedOption else { return }
         // Send file logic here
     }
     
     private func sendAcknowledgement(accept: Bool) {
         // Handle acknowledgement logic
     }
     
     private func loadDialogItems() {
         // Load dialog items and default device
     }
}

struct DefaultFilePicker: View {
    var onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            Text("Pick a File")
        }
    }
}

struct FileDetails: View {
    var meta: FileTransferMetadata
    var onClick: () -> Void
    
    var body: some View {
        VStack {
            Text("File: \(meta.fileName)")
                .font(.headline)
            Button(action: onClick) {
                Text("Change File")
            }
        }
    }
}

struct DialogListItem: View {
    var item: DropdownItem
    var isSelected: Bool
    var onClick: () -> Void
    
    var body: some View {
        HStack {
            Image(systemName: "desktopcomputer") // Replace with dynamic image logic
            Text(item.name)
            Spacer()
            if isSelected {
                Image(systemName: "checkmark")
            }
        }
        .padding()
        .background(isSelected ? Color.gray : Color.clear)
        .cornerRadius(12)
        .onTapGesture {
            onClick()
        }
    }
}

struct DeviceSelectionDialog: View {
    var items: [DropdownItem]
    @Binding var selectedOption: DropdownItem?
    
    var body: some View {
        VStack {
            Text("Select a Device")
                .font(.headline)
                .padding()
            
            List(items, id: \.id) { item in
                DialogListItem(item: item, isSelected: selectedOption?.id == item.id) {
                    if !item.disabled {
                        selectedOption = item
                    }
                }
            }
        }
        .padding()
    }
}

// Supporting Data Models
struct DropdownItem {
    var name: String
    var disabled: Bool
    var id: String
    var platform: String
    var ipAddress: String
}

struct PlatformFile {
    // Add relevant properties
}

struct Device {
    var iD: String
    var machineName: String
    var deviceId: String
    var platform: String
    var ipAddress: String
}

class FileTransferManager: ObservableObject {
    private let fileTransfer = SwiftSocketFileTransfer()
    private let publicDirectoryURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        .appendingPathComponent("ReceivedFiles")

    init() {
        createPublicDirectory()
    }

    func startServer() {
        fileTransfer.startServer { metadata, fileData in
            self.saveFile(metadata: metadata, fileData: fileData)
        }
    }

    func sendFile(file: FileWrapper) {
        // Replace `destinationIp` and `fileWrapper` with your implementation
        let destinationIp = "69.69.73.46" // Replace with actual IP

        fileTransfer.sendFile(destinationIp: destinationIp, fileWrapper: file, listener: FileTransferListenerImpl())
    }

    private func saveFile(metadata: FileTransferMetadata, fileData: Data) {
        let fileURL = publicDirectoryURL.appendingPathComponent(metadata.fileName)
        do {
            try fileData.write(to: fileURL)
            print("File saved at \(fileURL)")
        } catch {
            print("Failed to save file: \(error.localizedDescription)")
        }
    }

    private func createPublicDirectory() {
        do {
            try FileManager.default.createDirectory(at: publicDirectoryURL, withIntermediateDirectories: true)
            print("Public directory created at \(publicDirectoryURL)")
        } catch {
            print("Failed to create public directory: \(error.localizedDescription)")
        }
    }
}

class FileTransferListenerImpl: FileTransferListener {
    func onTransferProgress(_ progress: Float) {
        print("Progress: \(progress)%")
    }

    func onSpeedUpdate(_ speed: String) {
        print("Speed: \(speed)")
    }

    func onTransferComplete() {
        print("Transfer complete")
    }

    func onError(_ error: Error) {
        print("Error during transfer: \(error.localizedDescription)")
    }
}
