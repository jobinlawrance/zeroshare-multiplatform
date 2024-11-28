//
//  SwiftSocketFileTransfer.swift
//  iosApp
//
//  Created by Jobin Lawrance on 28/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Network
import Shared
import CryptoKit

protocol FileTransferListener {
    func onTransferProgress(_ progress: Float)
    func onSpeedUpdate(_ speed: String)
    func onTransferComplete()
    func onError(_ error: Error)
}

struct FileTransferMetadata: Codable {
    let fileName: String
    let fileSize: Int64
    let fileHash: String
    let transferType: String
}

class SwiftSocketFileTransfer {
    private let host: String
    private let port: UInt16
    private let queue = DispatchQueue(label: "FileTransferQueue", qos: .background)
    
    init(host: String = "0.0.0.0", port: UInt16 = 6969) {
        self.host = host
        self.port = port
    }
    
    func startServer(onFileReceived: @escaping (FileTransferMetadata, Data) -> Void) {
        queue.async {
            let listener = try? NWListener(using: .tcp, on: NWEndpoint.Port(rawValue: self.port)!)
            
            listener?.newConnectionHandler = { connection in
                connection.start(queue: self.queue)
                
                self.receiveFile(from: connection) { metadata, data in
                    onFileReceived(metadata, data)
                }
            }
            
            listener?.stateUpdateHandler = { state in
                switch state {
                case .ready:
                    print("Server ready on \(self.host):\(self.port)")
                case .failed(let error):
                    print("Server failed: \(error)")
                default:
                    break
                }
            }
            
            listener?.start(queue: self.queue)
        }
    }
    
    func sendFile(destinationIp: String, fileWrapper: FileWrapperNative, listener: FileTransferListener?) {
        queue.async {
            let connection = NWConnection(host: NWEndpoint.Host(destinationIp), port: NWEndpoint.Port(rawValue: self.port)!, using: .tcp)
            
            connection.start(queue: self.queue)
            
            connection.stateUpdateHandler = { state in
                switch state {
                case .ready:
                    self.transferFile(connection: connection, fileWrapper: fileWrapper, listener: listener)
                case .failed(let error):
                    listener?.onError(error)
                default:
                    break
                }
            }
        }
    }
    
    private func receiveFile(from connection: NWConnection, completion: @escaping (FileTransferMetadata, Data) -> Void) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 65536) { content, context, isComplete, error in
            guard let data = content else {
                print("Error receiving file: \(error?.localizedDescription ?? "Unknown error")")
                return
            }
            
            do {
                let decoder = JSONDecoder()
                let metadata = try decoder.decode(FileTransferMetadata.self, from: data)
                
                // Wait for the file content
                self.receiveFileData(connection: connection, expectedSize: metadata.fileSize) { fileData in
                    completion(metadata, fileData)
                }
            } catch {
                print("Error decoding metadata: \(error.localizedDescription)")
            }
        }
    }
    
    private func receiveFileData(connection: NWConnection, expectedSize: Int64, completion: @escaping (Data) -> Void) {
        var receivedData = Data()
        
        connection.receive(minimumIncompleteLength: 1, maximumLength: 65536) { content, context, isComplete, error in
            guard let data = content else {
                print("Error receiving file data: \(error?.localizedDescription ?? "Unknown error")")
                return
            }
            
            receivedData.append(data)
            
            if receivedData.count >= expectedSize {
                completion(receivedData)
            } else {
                self.receiveFileData(connection: connection, expectedSize: expectedSize, completion: completion)
            }
        }
    }
    
    private func transferFile(connection: NWConnection, fileWrapper: FileWrapperNative, listener: FileTransferListener?) {
        do {
            let fileData = fileWrapper.toData()
            let metadata = FileTransferMetadata(
                fileName: fileWrapper.getName(),
                fileSize: Int64(fileData.count),
                fileHash: calculateFileHash(fileData),
                transferType: "UPLOAD"
            )
            
            let encoder = JSONEncoder()
            let metadataData = try encoder.encode(metadata)
            
            connection.send(content: metadataData, completion: .contentProcessed { error in
                if let error = error {
                    listener?.onError(error)
                    return
                }
                
                self.sendFileData(connection: connection, fileData: fileData, listener: listener)
            })
        } catch {
            listener?.onError(error)
        }
    }
    
    private func sendFileData(connection: NWConnection, fileData: Data, listener: FileTransferListener?) {
        let totalSize = fileData.count
        var sentBytes = 0
        
        let chunkSize = 65536
        var offset = 0
        
        while offset < totalSize {
            let end = min(offset + chunkSize, totalSize)
            let chunk = fileData.subdata(in: offset..<end)
            
            connection.send(content: chunk, completion: .contentProcessed { error in
                if let error = error {
                    listener?.onError(error)
                    return
                }
                
                sentBytes += chunk.count
                offset += chunk.count
                
                let progress = Float(sentBytes) / Float(totalSize) * 100
                listener?.onTransferProgress(progress)
                
                let speed = self.calculateTransferSpeed(sentBytes)
                listener?.onSpeedUpdate(speed)
                
                if sentBytes == totalSize {
                    listener?.onTransferComplete()
                }
            })
        }
    }
    
    private func calculateTransferSpeed(_ bytesTransferred: Int) -> String {
        let kbps = Float(bytesTransferred) / 1024.0
        return String(format: "%.2f KB/s", kbps)
    }
    
    private func calculateFileHash(_ data: Data) -> String {
        let digest = SHA256.hash(data: data)
        let hashString = digest
            .compactMap { String(format: "%02x", $0) }
            .joined()
        return hashString
    }
}
