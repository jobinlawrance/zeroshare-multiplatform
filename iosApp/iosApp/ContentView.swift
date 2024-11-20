import SwiftUI
import Shared
import zt

enum ZeroTierError: Error {
    case nodeStartFailure
    case nodeOffline
    case networkJoinFailure
    case networkTimeout
}

struct ContentView: View {
    @State private var zeroController = ZeroController()
    @State private var networkID: String
    @State private var isConnecting = false
    @State private var navigateToNewView = false
    @State private var titleFromAsync = ""
    @State private var errorMessage: String? = nil
    
    
    lazy var log = koin.loggerWithTag(tag: "ContentView")
    
    init() {
        _networkID = State(initialValue: ZeroController().getNetworkId())
    }
    
    var body: some View {
        NavigationStack {
            VStack {
                Image(systemName: "wifi") // Replace with your image
                    .resizable()
                    .scaledToFit()
                    .frame(width: 100, height: 100)
                    .padding(.top, 50)
                
                TextField("Enter network ID here", text: $networkID)
                    .padding()
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(8)
                    .foregroundColor(.white)
                    .padding([.leading, .trailing], 20)
                
                Button(action: {
                    Task {
                        await connectToNetworkAndNavigate()
                    }
                }) {
                    Text("Connect")
                        .padding()
                        .background(isConnecting ? Color.gray : Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                        .disabled(isConnecting)
                }
                .padding(.top, 20)
                
                if let errorMessage = errorMessage {
                    Text(errorMessage)
                        .foregroundColor(.red)
                        .padding()
                }
                
                Spacer()
            }
            .navigationTitle("Network Connect")
            .background(Color.black.edgesIgnoringSafeArea(.all))
            .foregroundColor(.white)
            .navigationDestination(isPresented: $navigateToNewView) {
                NewView(title: titleFromAsync)
            }
        }
    }

    func connectToNetworkAndNavigate() async {
        guard !networkID.isEmpty else {
            errorMessage = "Please enter a network ID"
            return
        }
        
        do {
            zeroController.saveNetworkId(networkId: networkID)
            isConnecting = true
            errorMessage = nil
            
            // Call the async connectToNetwork function
            let title = try await connectToNetwork(networkID)
            
            // Update the title and navigate
            titleFromAsync = title
            isConnecting = false
            navigateToNewView = true
        } catch {
            isConnecting = false
            errorMessage = "Error: \(error.localizedDescription)"
            NSLog(errorMessage!)
        }
    }
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

func connectToNetwork(_ networkId: String) async throws -> String {
    
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

struct NewView: View {
    var title: String
    
    var body: some View {
        VStack {
            Text(title)
                .font(.largeTitle)
                .padding()
            Spacer()
        }
        .navigationTitle("Connection Status")
        .background(Color.black.edgesIgnoringSafeArea(.all))
        .foregroundColor(.white)
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
