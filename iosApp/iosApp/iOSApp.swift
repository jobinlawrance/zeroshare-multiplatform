import SwiftUI
import GoogleSignIn

enum NavigationPath {
    case login
    case main
}

// Create a navigation state manager
class NavigationStateManager: ObservableObject {
    @Published var currentPath: NavigationPath = .login
}

// Modified App struct
@main
struct iOSApp: App {
    @StateObject private var navigationManager = NavigationStateManager()
    
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
                                GIDSignIn.sharedInstance.handle(url)
                            }
                    case .main:
                        PeerScreen()
                    }
                }
            }
            .environmentObject(navigationManager)
        }
    }
}

func application(
  _ app: UIApplication,
  open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]
) -> Bool {
  var handled: Bool

  handled = GIDSignIn.sharedInstance.handle(url)
  if handled {
    return true
  }

  // Handle other custom URL types.

  // If not handled by this app, return false.
  return false
}
