import Foundation
import Capacitor
import CryptoKit
import AuthenticationServices
import FirebaseCore
import FirebaseAuth
import GoogleSignIn

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */

private func randomNonceString(length: Int = 32) -> String {
    precondition(length > 0)
    var randomBytes = [UInt8](repeating: 0, count: length)
    let errorCode = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
    if errorCode != errSecSuccess {
        fatalError(
                "Unable to generate nonce. SecRandomCopyBytes failed with OSStatus \(errorCode)"
        )
    }

    let charset: [Character] =
            Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")

    let nonce = randomBytes.map { byte in
        // Pick a random character from the set, wrapping around if needed.
        charset[Int(byte) % charset.count]
    }

    return String(nonce)
}


@available(iOS 13, *)
private func sha256(_ input: String) -> String {
    let inputData = Data(input.utf8)
    let hashedData = SHA256.hash(data: inputData)
    let hashString = hashedData.compactMap {
                String(format: "%02x", $0)
            }
            .joined()

    return hashString
}

// Unhashed nonce.
fileprivate var currentNonce: String?
fileprivate var pluginCall: CAPPluginCall?

@available(iOS 13.0, *)
extension UIViewController: ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {

    public func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        return self.view.window!
    }

    @available(iOS 13, *)
    public func startSignInWithAppleFlow() {
        let nonce = randomNonceString()
        currentNonce = nonce
        let appleIDProvider = ASAuthorizationAppleIDProvider()
        let request = appleIDProvider.createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)

        let authorizationController = ASAuthorizationController(authorizationRequests: [request])
        authorizationController.delegate = self
        authorizationController.presentationContextProvider = self
        authorizationController.performRequests()
    }

    public func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        if let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential {
            guard let nonce = currentNonce else {
                print("Invalid state: A login callback was received, but no login request was sent.")
                pluginCall?.resolve([
                    "value": "Invalid state: A login callback was received, but no login request was sent."
                ])
                return
            }
            guard let appleIDToken = appleIDCredential.identityToken else {
                print("Unable to fetch identity token")
                pluginCall?.resolve([
                    "value": "Unable to fetch identity token"
                ])
                return
            }
            guard let idTokenString = String(data: appleIDToken, encoding: .utf8) else {
                print("Unable to serialize token string from data: \(appleIDToken.debugDescription)")
                pluginCall?.resolve([
                    "value": "Unable to serialize token string from data: \(appleIDToken.debugDescription)"
                ])
                return
            }
            print("Apple ID Token: \(idTokenString)")
            // Initialize a Firebase credential, including the user's full name.
            let credential = OAuthProvider.appleCredential(withIDToken: idTokenString,
                    rawNonce: nonce,
                    fullName: appleIDCredential.fullName)
            // Sign in with Firebase.
            Auth.auth().signIn(with: credential) { (authResult, error) in
                if let error = error {
                    print("Firebase sign-in error:", error.localizedDescription)
                    pluginCall?.resolve([
                        "value": error.localizedDescription
                    ])
                    return
                }

                guard let user = authResult?.user else {
                    pluginCall?.resolve([
                        "value": "No Firebase user"
                    ])
                    return
                }

                // ✅ Get the Firebase ID token
                user.getIDToken(completion: { token, error in
                    if let error = error {
                        pluginCall?.resolve([
                            "value": "Failed to get ID token: \(error.localizedDescription)"
                        ])
                        return
                    }

                    guard let idToken = token else {
                        pluginCall?.resolve([
                            "value": "ID token is nil"
                        ])
                        return
                    }

                    print("✅ Firebase ID Token:", idToken)
                    pluginCall?.resolve([
                        "value": idToken
                    ])
                })
            }
        }
    }

    public func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        // Handle error.
        print("Sign in with Apple errored: \(error)")
        pluginCall?.resolve([
            "value": "Sign in with Apple errored: \(error)"
        ])
    }

}

@objc(LoginPluginPlugin)
public class LoginPluginPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "LoginPluginPlugin"
    public let jsName = "LoginPlugin"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "echo", returnType: CAPPluginReturnPromise)
    ]
    private let implementation = LoginPlugin()

    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        initializeFirebase(with: value, completion: { result, error in

            if (self.provider == "apple") {
                self.signInWithApple(call: call)
            } else {
                self.signInWithGoogle(call: call)
            }
        })

    }

    func signInWithApple(call: CAPPluginCall) {
        pluginCall = call
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            call.resolve([
                "value": "Missing Firebase client ID"
            ])
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)

        // Find the root view controller
        guard let rootViewController = UIApplication.shared.windows.first?.rootViewController else {
            call.resolve([
                "value": "Unable to find root view controller"
            ])
            return
        }

        rootViewController.startSignInWithAppleFlow()
    }

    func signInWithGoogle(call: CAPPluginCall) {
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            call.resolve([
                "value": "Missing Firebase client ID"
            ])
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)

        // Find the root view controller
        guard let rootViewController = UIApplication.shared.windows.first?.rootViewController else {
            call.resolve([
                "value": "Unable to find root view controller"
            ])
            return
        }

        GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController) { result, error in
            if let error = error {
                call.resolve([
                    "value": "Google sign-in failed: \(error.localizedDescription)"
                ])
                return
            }

            guard let idToken = result?.user.idToken?.tokenString else {
                call.resolve([
                    "value": "Missing Google ID token"
                ])
                return
            }

            let credential = GoogleAuthProvider.credential(withIDToken: idToken,
                    accessToken: result?.user.accessToken.tokenString ?? "")

            Auth.auth().signIn(with: credential) { authResult, error in
                if let error = error {
                    call.resolve([
                        "value": "Firebase authentication failed: \(error.localizedDescription)"
                    ])
                    return
                }

                authResult?.user.getIDToken { firebaseToken, error in
                    if let error = error {
                        call.resolve([
                            "value": "Failed to retrieve Firebase token: \(error.localizedDescription)"
                        ])
                        return
                    }

                    call.resolve([
                        "value": firebaseToken ?? "ERROR: Fire base token not found"
                    ])
                }
            }
        }
    }

    override public func load() {
        print("plugin loaded")
    }

    var provider = "google"

    public func initializeFirebase(with jsonConfig: String, completion: @escaping (Bool, String?) -> Void) {
        guard let data = jsonConfig.data(using: .utf8) else {
            print("Error: Invalid JSON string")
            completion(false, "Invalid JSON string")
            return
        }

        do {
            if let configDict = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] {

                print("Provider is: \(configDict["provider"] as! String)")
                if let provider = configDict["provider"] as? String {
                    self.provider = provider
                }
                guard let googleAppID = configDict["appId"] as? String,
                      let gcmSenderID = configDict["messagingSenderId"] as? String
                else {
                    print("Error: Missing required Firebase config values")
                    completion(false, "Missing required Firebase config values")
                    return
                }

                let options = FirebaseOptions(googleAppID: googleAppID, gcmSenderID: gcmSenderID)
                options.apiKey = (configDict["apiKey"] as! String)
                options.clientID = (configDict["clientId"] as! String)
                options.projectID = (configDict["projectId"] as! String)
                options.bundleID = configDict["bundleId"] as! String
                options.storageBucket = configDict["storageBucket"] as? String
                options.googleAppID = googleAppID
                options.gcmSenderID = gcmSenderID

                DispatchQueue.main.async {
                    if FirebaseApp.app() == nil {
                        FirebaseApp.configure(options: options)
                    }
                    print("Firebase initialized successfully")
                    completion(true, nil)
                }
            } else {
                print("Error: Failed to parse JSON")
                completion(false, "Failed to parse JSON")
            }
        } catch {
            print("Error decoding JSON:", error.localizedDescription)
            completion(false, error.localizedDescription)
        }
    }

}
