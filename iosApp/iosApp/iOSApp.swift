import FirebaseCore
import GoogleSignIn
import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Without GoogleService-Info.plist the shared app shows its "Firebase isn't configured" screen.
        if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Compose draws edge to edge and applies the safe-area / keyboard insets itself.
                .ignoresSafeArea()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
