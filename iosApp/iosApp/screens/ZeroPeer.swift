//
//  ZeroPeer.swift
//  iosApp
//
//  Created by Jobin Lawrance on 23/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Shared
import zt
import UIKit

class ZeroPeerImpl {
    
    private var port: UInt16
    
    init(port: Int) {
        self.port = UInt16(port)
    }
    
    func sendMessage(remoteAddr: String, port: Int32, message: String) async throws {
        
    }
    
    func startServer() {
            let queue = DispatchQueue(label: "com.zerotier.server", attributes: .concurrent)
            
            queue.async {
//
//                
                // Set up server address
                var serverAddr = zts_sockaddr_in(
                    sin_len: UInt8(MemoryLayout<zts_sockaddr_in>.size),
                    sin_family: UInt8(ZTS_AF_INET),
                    sin_port: UInt16(self.port).bigEndian,
                    sin_addr: zts_in_addr(s_addr: 0),
                    sin_zero: (0, 0, 0, 0, 0, 0, 0, 0)
                )
                
                // Convert string IP to network address
                zts_inet_pton(ZTS_AF_INET, "0.0.0.0", &serverAddr.sin_addr)
                
                // Create socket
                let sockfd = zts_socket(ZTS_AF_INET, ZTS_SOCK_STREAM, 0)
                if(sockfd < 0) {
                    NSLog("Failed to create socket \(sockfd) \(zts_errno)")
                    return
                }
                
                // Bind socket
                let bindResult = zts_bind(sockfd, "0.0.0.0", self.port)
                
                if(bindResult < 0) {
                    NSLog("Failed to bind socket \(bindResult) erro no - \(zts_errno)")
                    zts_close(sockfd)
                    return
                }
                
                // Listen for connections
                if (zts_listen(sockfd, 5) < 0 )  {
                    print("Failed to listen on socket")
                    zts_close(sockfd)
                    return
                }
                
                print("Server listening on port \(self.port)")
                
                while true {
                    var remote_ipstr = [CChar](repeating: 0, count: Int(INET6_ADDRSTRLEN))
                    
                    // Accept connection
                    let clientSock = zts_accept(sockfd, &remote_ipstr, ZTS_INET6_ADDRSTRLEN, &self.port)
                    
                    if (clientSock < 0) {
                        print("Failed to accept connection \(clientSock) \(zts_errno)")
                        continue
                    }
                    
                    // Handle client in separate thread
                    queue.async {
                        self.handleClient(clientSock)
                    }
                }

            }
        }
    
    private func handleClient(_ clientSock: Int32) {
            var buffer = [UInt8](repeating: 0, count: 1024)
            
            while true {
                let bytesRead = zts_read(clientSock, &buffer, buffer.count)
                
                if bytesRead <= 0 {
                    break
                }
                
                if let message = String(bytes: buffer.prefix(Int(bytesRead)), encoding: .utf8) {
                    print("Received: \(message)")
                    
                    // Echo back
                    let response = "Server received: \(message)"
                    let responseData = Array(response.utf8)
                    zts_write(clientSock, responseData, responseData.count)
                }
            }
            
            zts_close(clientSock)
        }
    
    func sendMessage(_ message: String, to remoteAddr: String, port: Int) {
        print("Creating socket...")
        let fd = zts_socket(ZTS_AF_INET, ZTS_SOCK_STREAM, 0)
        guard fd >= 0 else {
            print("Failed to create socket, errno: \(zts_errno)")
            return
        }
        
        print("Connecting to \(remoteAddr):\(port)...")
        let connectionResult = zts_connect(fd, remoteAddr, UInt16(port), 0)
        guard connectionResult >= 0 else {
            print("Failed to connect to \(remoteAddr):\(port), errno: \(zts_errno)")
            zts_close(fd)
            return
        }
        
        print("Connection established, sending message...")
        let messageData = Array(message.utf8)
        let bytesWritten = zts_send(fd, messageData, messageData.count, ZTS_MSG_MORE)
        guard bytesWritten > 0 else {
            print("Failed to send message, errno: \(zts_errno)")
            zts_close(fd)
            return
        }
        
        print("Message sent: \(message)")
//        zts_close(fd)
    }
        
    func stop() {
        zts_node_stop()
    }
    
}
