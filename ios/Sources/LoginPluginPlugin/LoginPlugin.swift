import Foundation
import FirebaseCore
import FirebaseAuth
import GoogleSignIn

@objc public class LoginPlugin: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value + "from swift"
    }
    
    @objc public func signInWithGoogle(_ callback: @escaping (String?, String?) -> Void) {
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            callback(nil, "Missing Firebase client ID")
            return
        }
        
        let config = GIDConfiguration(clientID: clientID)
        
        // Find the root view controller
        guard let rootViewController = UIApplication.shared.windows.first?.rootViewController else {
            callback(nil, "Unable to find root view controller")
            return
        }
        
        GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController) { result, error in
            if let error = error {
                callback(nil, "Google sign-in failed: \(error.localizedDescription)")
                return
            }
            
            guard let idToken = result?.user.idToken?.tokenString else {
                callback(nil, "Missing Google ID token")
                return
            }
            
            let credential = GoogleAuthProvider.credential(withIDToken: idToken,
                                                           accessToken: result?.user.accessToken.tokenString ?? "")
            
            Auth.auth().signIn(with: credential) { authResult, error in
                if let error = error {
                    callback(nil, "Firebase authentication failed: \(error.localizedDescription)")
                    return
                }
                
                authResult?.user.getIDToken { firebaseToken, error in
                    if let error = error {
                        callback(nil, "Failed to retrieve Firebase token: \(error.localizedDescription)")
                        return
                    }
                    
                    callback(firebaseToken, nil)
                }
            }
        }
    }
}
