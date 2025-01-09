//
//  TransferScreen.swift
//  iosApp
//
//  Created by Jobin Lawrance on 28/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//


import SwiftUI
import Foundation
import Shared
import UniformTypeIdentifiers


struct TransferScreen: View {
    @State private var isExporting = false
    
    @State private var showDialog = false
    @State private var showIncomingDialog = false
    @State private var selectedOption: DropdownItem?
    @State private var selectedFileMeta: FileTransferMetadata?
    @State private var incomingFile: FileTransferMetadata?
    @State private var incomingDevice: Shared.Device?
    @State private var dialogItems: [DropdownItem] = []
    @State private var defaultDevice: Shared.Device?
    @State private var selectedDevice: Device? = nil
    @State private var devices: [Shared.Device] = []
    
    private let socketStream :SocketStream = SocketStream()
    private let backendApi = BackendApi()
    
    var body: some View {
        ZStack {
            Color.black
                .edgesIgnoringSafeArea(.all)
            
            VStack() {
                // Title
                Text("File Sharing")
                    .font(.custom("AvenirNext-Bold", size: 36))
                    .foregroundColor(.white)
                
                
                Spacer(minLength: 16)
                var action:(FileWrapper) -> Void = { file in
                    selectedFileMeta = file.toFileMetaData()
                }
                
                Button(action: {}) {
                    if selectedFileMeta == nil {
                        FilePicker {
                            isExporting = true
                        }
                        .frame(width: 560, height: 80)
                        .ignoresSafeArea(.keyboard)
                    } else {
                        FileDetails(meta: selectedFileMeta!) {
                            isExporting = true
                        }
                        .frame(width: 560, height: 180)
                        .ignoresSafeArea(.keyboard)
                    }
                }
                .fileImporter(isPresented: $isExporting, allowedContentTypes: [.item] ) { result in
                    switch result {
                    case .success(let url):
                        do {
                            if url.startAccessingSecurityScopedResource(){
                                let fileWrapper = try FileWrapper(url: url)
                                url.stopAccessingSecurityScopedResource()
                                action(fileWrapper)
                            }
                            
                        } catch {
                            print("Error reading file: \(error.localizedDescription)")
                        }
                    case .failure(let error):
                        print("File selection failed: \(error.localizedDescription)")
                    }
                }
                
                Spacer(minLength: 10)
                
                // Device selection title
                Text("Select device to Send")
                    .font(.custom("AvenirNext-Regular", size: 24))
                    .foregroundColor(.white)
                
                if (selectedOption != nil) {
                    SelectDevice(item: selectedOption!, isSelected: true, showTick: false, onClick: {showDialog = true})
                        .frame(width: 560, height: 80)
                }
                
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
                    .background(selectedFileMeta != nil && selectedOption != nil ? Color.gray.opacity(0.2) : Color.gray.opacity(0.1))
                    .cornerRadius(12)
                }
                .disabled(selectedFileMeta == nil || selectedOption == nil)
                
                Spacer()
            }
            .padding()
            
            if (incomingFile != nil) {
                IncomingFilePopup(
                    isShowing: $showIncomingDialog,
                    meta: incomingFile!,
                    onDismiss: {
                        showIncomingDialog = false
                    },
                    onClick: { accept in
                        socketStream.sendAcknowledgement(
                            accept: accept,
                            incomingDevice: incomingDevice!,
                            uniqueDeviceId: uniqueDeviceId()
                        )
                    }
                )
            }
            
            if(selectedOption != nil) {
                DeviceSelectorPopup(
                    isShowing: $showDialog,
                    selectedOption: selectedOption!,
                    dialogItems: dialogItems,
                    onSelected: { item in
                        selectedOption = item
                    },
                    onDismiss: {
                        showDialog = false
                    }
                )
            }
        }.onAppear {
            Task {
                devices = try await backendApi.getDevices()
                defaultDevice = devices.first { uniqueDeviceId() != $0.deviceId }
                selectedOption = DropdownItem(
                    name: defaultDevice!.machineName,
                    disabled: false,
                    id: defaultDevice!.iD,
                    platform: defaultDevice!.platform,
                    ipAddress: defaultDevice!.ipAddress
                )
                dialogItems = devices.map { device in
                    DropdownItem(
                        name: device.machineName,
                        disabled: uniqueDeviceId() == device.deviceId,
                        id: device.iD,
                        platform: device.platform,
                        ipAddress: device.ipAddress
                    )
                }
                let myDevice = devices.first {uniqueDeviceId() == $0.deviceId}
                socketStream.startListening(device: myDevice!) { response in
                    print(response)
                    showIncomingDialog = true
                    incomingFile = response.data
                    incomingDevice = response.device
                } onAcknowledgement: { response in
                    let accept = response.data
                    if (accept!.boolValue) {
                        //TODO: need to implement an upload logic
                    }
                } onDownloadComplete: { response in
                    print(response)
                } onDownloadResponse: { response in
                    print(response)
                    let downloadResponse = response.data!
                    
                    let downloader = FileDownloader(fileName: downloadResponse.fileName)
                    downloader.download(from: URL(string: downloadResponse.downloadUrl)!) {
                        print("Download completed!")
                        socketStream.sendDownloadComplete(uniqueDeviceId: uniqueDeviceId(), incomingDevice: incomingDevice!)
                    }
                }
            }
        }
        
    }
}

struct DeviceSelectorPopup: View {
    @Binding var isShowing: Bool
    let selectedOption: DropdownItem
    let dialogItems: [DropdownItem]
    let onSelected: (DropdownItem) -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        if isShowing {
            ZStack {
                DeviceSelector(
                    selectedOption: selectedOption,
                    dialogItems: dialogItems,
                    onSelected: onSelected,
                    onDismiss: onDismiss
                )
            }
        }
    }
}

struct DeviceSelector: UIViewControllerRepresentable {
    let selectedOption: DropdownItem
    let dialogItems: [DropdownItem]
    let onSelected: (DropdownItem) -> Void
    let onDismiss: () -> Void
    
    func makeUIViewController(context: Context) -> UIViewController {
        return DeviceSelectorController(
            selectedOption: selectedOption,
            dialogItems: dialogItems,
            onSelected: onSelected,
            onDismiss: onDismiss
        )
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

struct IncomingFileDialog: UIViewControllerRepresentable {
    let meta: FileTransferMetadata
    let onDismiss: () -> Void
    let onClick: (Bool) -> Void
    
    func makeUIViewController(context: Context) -> UIViewController {
        return IncomingFileDialogController(
            meta: meta,
            onDismiss:
                onDismiss,
            onClick: { accept in
                onClick(accept.boolValue)
            }
        )
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

struct IncomingFilePopup: View {
    // State to control the visibility of the popup
    @Binding var isShowing: Bool
    let meta: FileTransferMetadata
    let onDismiss: () -> Void
    let onClick: (Bool) -> Void
    
    var body: some View {
        if isShowing {
            ZStack {
                IncomingFileDialog(meta: meta, onDismiss: onDismiss, onClick: onClick)
            }
        }
    }
}

struct FilePicker: UIViewControllerRepresentable {
    let onClick: () -> Void
    func makeUIViewController(context: Context) -> UIViewController {
        return DefaultFilePickerController(onClick: onClick)
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}


struct FileDetails: UIViewControllerRepresentable {
    let meta: FileTransferMetadata
    let onClick: () -> Void
    
    func makeUIViewController(context: Context) -> UIViewController {
        return FileDetailsController(meta: meta, onClick: onClick)
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

struct SelectDevice: UIViewControllerRepresentable {
    let item: DropdownItem
    let isSelected: Bool
    let showTick: Bool
    let onClick: () -> Void
    
    func makeUIViewController(context: Context) -> UIViewController {
        return SelectedDeviceController(item: item, isSelected: isSelected, onClick: onClick, showTick: showTick)
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

extension FileWrapper {
    func toFileMetaData() -> FileTransferMetadata? {
        // Ensure the file wrapper represents a regular file
        guard isRegularFile, let fileName = preferredFilename else {
            print("FileWrapper does not represent a regular file or has no name.")
            return nil
        }
        
        // Retrieve file contents
        guard let regularFileContents = regularFileContents else {
            print("Failed to retrieve file contents.")
            return nil
        }
        
        
        do {
            // Get file attributes for size
            // Determine file size from the contents
            let fileSize = UInt64(regularFileContents.count)
            
            // Determine MIME type using the file extension
            let fileExtension = (fileName as NSString).pathExtension
            let mimeType = UTType(filenameExtension: fileExtension)?.preferredMIMEType ?? "application/octet-stream"
            
            // Use the provided fileURL if available for further metadata
            
            // Construct metadata
            return FileTransferMetadata(
                fileName: fileName,
                fileSize: Int64(fileSize),
                mimeType: mimeType,
                extension: fileExtension
            )
        } catch {
            print("Error retrieving file attributes: \(error.localizedDescription)")
            return nil
        }
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
