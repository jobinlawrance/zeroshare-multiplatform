import SwiftUI
import GoogleSignIn
import Shared
import UniformTypeIdentifiers

enum NavigationPath {
    case login
    case main
}

// Create a navigation state manager
class NavigationStateManager: ObservableObject {
    @Published var currentPath: NavigationPath = .login
}

class FileImportViewModel: ObservableObject {
    @Published var importedFileURL: URL?
    @Published var importedFileContent: String = ""
    
    func importFile(url: URL) {
        do {
            // Verify file extension
            guard url.pathExtension.lowercased() == "pub" else {
                print("Invalid file type")
                return
            }
            
            // Read file content
            importedFileContent = try String(contentsOf: url, encoding: .utf8)
            importedFileURL = url
            
            // Log detailed information
            print("File imported successfully:")
            print("File URL: \(url)")
            print("File Content: \(importedFileContent)")
            
            // Process the public file
            processPublicFile()
        } catch {
            print("Error importing file: \(error.localizedDescription)")
            print("Full error: \(error)")
        }
    }
    
    private func processPublicFile() {
        // Add your specific .pub file processing logic here
        print("Processing imported .pub file")
    }
}

// Modify AppDelegate to improve file handling
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        print("AppDelegate: Received URL \(url)")
        
        // Handle Google Sign-In first
        if GIDSignIn.sharedInstance.handle(url) {
            return true
        }
        
        // Check if it's a .pub file
        guard url.pathExtension.lowercased() == "pub" else {
            print("Not a .pub file")
            return false
        }
        
        // Try to handle the file
        return handleFile(at: url)
    }
    
    // Scene-based URL handling (iOS 13+)
    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        let config = UISceneConfiguration(name: nil, sessionRole: connectingSceneSession.role)
        return config
    }
}

// Modify the file handling function to be more robust
func handleFile(at url: URL) -> Bool {
    print("Handling file at: \(url)")
    
    // Ensure it's a .pub file
    guard url.pathExtension.lowercased() == "pub" else {
        print("Invalid file type")
        return false
    }
    
    do {
        // Try to read the file content
        let fileContent = try String(contentsOf: url, encoding: .utf8)
        print("File content: \(fileContent)")
        
        // Add your specific file processing logic here
        return true
    } catch {
        print("Error reading file: \(error.localizedDescription)")
        return false
    }
}

// Modify App struct to ensure proper file import handling
@main
struct iOSApp: App {
    @StateObject private var navigationManager = NavigationStateManager()
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject private var viewModel = FileImportViewModel()
    @State private var showImportSheet = false
    
    init() {
        startKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            NavigationStack {
                Group {
                    switch navigationManager.currentPath {
                    case .login:
                        LoginScreen()
                            .onOpenURL { url in
                                print("NavigationStack: Received URL \(url)")
                                handleShareFile(url)
                            }
                            .fileImporter(
                                isPresented: $showImportSheet,
                                allowedContentTypes: [.pubFile],
                                allowsMultipleSelection: false
                            ) { result in
                                handleFileImporterResult(result)
                            }
                    case .main:
                        PeerScreen()
                    }
                }
            }
            .environmentObject(navigationManager)
        }
    }
    
    private func handleShareFile(_ url: URL) {
        print("Handling shared file: \(url)")
        
        // Additional handling for shared files
        if url.pathExtension.lowercased() == "pub" {
            viewModel.importFile(url: url)
        }
    }
    
    private func handleFileImporterResult(_ result: Result<[URL], Error>) {
        do {
            let fileURL = try result.get().first!
            
            guard fileURL.startAccessingSecurityScopedResource() else {
                print("Could not access file")
                return
            }
            
            viewModel.importFile(url: fileURL)
            
            fileURL.stopAccessingSecurityScopedResource()
        } catch {
            print("File import error: \(error.localizedDescription)")
        }
    }
}

// Ensure the custom UTType is defined
extension UTType {
    static var pubFile: UTType {
        UTType(exportedAs: "live.jkbx.zeroshare.ZeroShare.pub")
    }
}
