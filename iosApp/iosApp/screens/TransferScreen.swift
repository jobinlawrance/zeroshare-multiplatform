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

    var body: some View {
        VStack {
            Text("File Transfer")
                .font(.title)
                .padding()

            Button(action: {
                isExporting = true
                
            }) {
                Text("Send File")
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }.fileImporter(isPresented: $isExporting, allowedContentTypes: [.item] ) { result in
                switch result {
                case .success(let file):
                    print(file.absoluteString)
                    Task {
                        let fileWrapper = try FileWrapper(url: file)
                        fileTransferManager.sendFile(file: fileWrapper)
                    }
                case .failure(let error):
                    print(error.localizedDescription)
                }
            }
        }
        .onAppear {
//            fileTransferManager.startServer()
        }
    }
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
