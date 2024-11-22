import SwiftUI
import GoogleSignIn

@main
struct iOSApp: App {
    init() {
        startKoin()
    }
    var body: some Scene {
        WindowGroup {
            LoginScreen()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
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
