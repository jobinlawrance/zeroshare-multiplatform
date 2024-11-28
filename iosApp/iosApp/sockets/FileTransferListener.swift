//
//  FileTransferListener.swift
//  iosApp
//
//  Created by Jobin Lawrance on 28/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//


//
//  SwiftSocketFileTransfer.swift
//  iosApp
//
//  Created by Jobin Lawrance on 28/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Network
import CryptoKit
import SwiftyJSON


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

enum TransferStatus: String, Codable {
    case success
    case failed
    case hashMismatch
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
            do {
                let listener = try NWListener(using: .tcp, on: NWEndpoint.Port(rawValue: self.port)!)
                listener.newConnectionHandler = { connection in
                    connection.start(queue: self.queue)
                    self.receiveFile(from: connection) { result in
                        switch result {
                        case .success(let (metadata, data)):
                            let fileHash = self.calculateFileHash(data)
                            let status: TransferStatus = (fileHash == metadata.fileHash) ? .success : .hashMismatch
                            self.sendAcknowledgement(connection: connection, status: status)
                            
                            if status == .success {
                                onFileReceived(metadata, data)
                            }
                        case .failure(let error):
                            print("Error receiving file: \(error.localizedDescription)")
                        }
                    }
                }
                listener.start(queue: self.queue)
            } catch {
                print("Failed to start server: \(error.localizedDescription)")
            }
        }
    }
    
    func sendFile(destinationIp: String, fileWrapper: FileWrapper, listener: FileTransferListener?) {
        queue.async {
            guard let fileData = fileWrapper.regularFileContents,
                  let fileName = fileWrapper.filename else {
                listener?.onError(NSError(domain: "FileTransfer", code: -1, userInfo: [NSLocalizedDescriptionKey: "Invalid file wrapper"]))
                return
            }
            
            let connection = NWConnection(host: NWEndpoint.Host(destinationIp), port: NWEndpoint.Port(rawValue: self.port)!, using: .tcp)
            connection.start(queue: self.queue)
            
            connection.stateUpdateHandler = { state in
                switch state {
                case .ready:
                    self.transferFile(connection: connection, fileData: fileData, fileName: fileName, listener: listener)
                case .failed(let error):
                    listener?.onError(error)
                default:
                    break
                }
            }
        }
    }
    
    private func transferFile(connection: NWConnection, fileData: Data, fileName: String, listener: FileTransferListener?) {
        let metadata = FileTransferMetadata(
            fileName: fileName,
            fileSize: Int64(fileData.count),
            fileHash: calculateFileHash(fileData),
            transferType: "UPLOAD"
        )
        
        do {
            let encoder = JSONEncoder()
                let metadataData = try encoder.encode(metadata) // This gives us Data, not JSON
                
                // Convert Data to a UTF-8 string (this will give you a valid JSON string)
                if let metadataString = String(data: metadataData, encoding: .utf8) {
                    print("Sending Metadata - \(metadataString)") // Logs the JSON as string
                    
                    // Calculate the length of the UTF-8 string
                    let metadataLength = UInt16(metadataString.utf8.count)

                    // Send the length and then the actual data
                    var lengthData = Data()

                    // Append the high byte and low byte of metadataLength to the Data object
                    lengthData.append(UInt8(metadataLength >> 8))  // High byte
                    lengthData.append(UInt8(metadataLength & 0xFF))  // Low byte

                    // Append the actual metadata string as UTF-8 bytes
                    lengthData.append(metadataString.data(using: .utf8)!)  // Send the actual data


                    // Send the metadata string over the connection
                    connection.send(content: lengthData, completion: .contentProcessed { error in
                        if let error = error {
                            listener?.onError(error)
                            return
                        }
                    })
                    
                    self.sendFileData(connection: connection, fileData: fileData, listener: listener)
                                
                } else {
                    print("Failed to convert metadata to UTF-8 string")
                }
        } catch {
            listener?.onError(error)
        }
    }
    
    private func sendFileData(connection: NWConnection, fileData: Data, listener: FileTransferListener?) {
        let totalSize = fileData.count
        var sentBytes = 0
        
        let chunkSize = 65536
        var offset = 0
        
        let group = DispatchGroup()
        
        while offset < totalSize {
            let end = min(offset + chunkSize, totalSize)
            let chunk = fileData.subdata(in: offset..<end)
            
            group.enter()
            connection.send(content: chunk, completion: .contentProcessed { error in
                if let error = error {
                    listener?.onError(error)
                    group.leave()
                    return
                }
                
                sentBytes += chunk.count
                let progress = Float(sentBytes) / Float(totalSize)
                listener?.onTransferProgress(progress)
                
                if sentBytes == totalSize {
                    listener?.onTransferComplete()
                }
                group.leave()
            })
            offset += chunkSize
        }
        
        group.wait()
    }
    
    private func receiveFile(from connection: NWConnection, completion: @escaping (Result<(FileTransferMetadata, Data), Error>) -> Void) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 65536) { content, context, isComplete, error in
            guard let data = content else {
                completion(.failure(error ?? NSError(domain: "FileTransfer", code: -1, userInfo: [NSLocalizedDescriptionKey: "Unknown error"])))
                return
            }
            
            do {
                let decoder = JSONDecoder()
                let metadata = try decoder.decode(FileTransferMetadata.self, from: data)
                
                self.receiveFileData(connection: connection, expectedSize: metadata.fileSize) { result in
                    switch result {
                    case .success(let fileData):
                        completion(.success((metadata, fileData)))
                    case .failure(let error):
                        completion(.failure(error))
                    }
                }
            } catch {
                completion(.failure(error))
            }
        }
    }
    
    private func receiveFileData(connection: NWConnection, expectedSize: Int64, completion: @escaping (Result<Data, Error>) -> Void) {
        var receivedData = Data()
        
        func receiveNextChunk() {
            connection.receive(minimumIncompleteLength: 1, maximumLength: 65536) { content, context, isComplete, error in
                guard let data = content else {
                    completion(.failure(error ?? NSError(domain: "FileTransfer", code: -1, userInfo: [NSLocalizedDescriptionKey: "Unknown error"])))
                    return
                }
                
                receivedData.append(data)
                if receivedData.count >= expectedSize {
                    completion(.success(receivedData))
                } else {
                    receiveNextChunk()
                }
            }
        }
        
        receiveNextChunk()
    }
    
    private func sendAcknowledgement(connection: NWConnection, status: TransferStatus) {
        do {
            let encoder = JSONEncoder()
            let ackData = try encoder.encode(status)
            
            connection.send(content: ackData, completion: .contentProcessed { error in
                if let error = error {
                    print("Error sending acknowledgement: \(error.localizedDescription)")
                }
            })
        } catch {
            print("Error encoding acknowledgement: \(error.localizedDescription)")
        }
    }
    
    private func calculateFileHash(_ data: Data) -> String {
        let digest = SHA256.hash(data: data)
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}
