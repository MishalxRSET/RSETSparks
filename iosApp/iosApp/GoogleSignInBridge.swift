import FirebaseCore
import GoogleSignIn
import Shared
import UIKit

/// Google Sign-In for the shared app, limited to rajagiri.edu.in Workspace accounts.
/// The shared Kotlin code exchanges the returned tokens for a Firebase session and re-checks the domain.
final class GoogleSignInBridgeImpl: GoogleSignInBridge {
    func signIn(handler: GoogleSignInResultHandler) {
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            handler.onResult(idToken: nil, accessToken: nil, errorMessage: "Google Sign-In isn't configured yet (missing GoogleService-Info.plist).")
            return
        }
        guard let presenter = Self.topViewController() else {
            handler.onResult(idToken: nil, accessToken: nil, errorMessage: "Couldn't open Google Sign-In.")
            return
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: clientID, serverClientID: nil, hostedDomain: "rajagiri.edu.in", openIDRealm: nil
        )
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let error = error as NSError? {
                let cancelled = error.domain == kGIDSignInErrorDomain && error.code == GIDSignInError.canceled.rawValue
                handler.onResult(idToken: nil, accessToken: nil, errorMessage: cancelled ? nil : error.localizedDescription)
                return
            }
            guard let user = result?.user, let idToken = user.idToken?.tokenString else {
                handler.onResult(idToken: nil, accessToken: nil, errorMessage: "Google didn't return an ID token.")
                return
            }
            handler.onResult(idToken: idToken, accessToken: user.accessToken.tokenString, errorMessage: nil)
        }
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
    }

    private static func topViewController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
        var top = window?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
