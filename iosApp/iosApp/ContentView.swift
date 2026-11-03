import Shared
import SwiftUI
import UIKit

/// Hosts the shared Compose app (shared/src/iosMain/kotlin/com/example/MainViewController.kt).
struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? ""
        return MainViewControllerKt.MainViewController(googleSignIn: GoogleSignInBridgeImpl(), versionName: version)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
