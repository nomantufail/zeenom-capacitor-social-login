import Foundation
import Capacitor
import FirebaseCore
import FirebaseAuth
import GoogleSignIn

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
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
        })

    }

    override public func load() {
        print("plugin loaded")
    }

    public func initializeFirebase(with jsonConfig: String, completion: @escaping (Bool, String?) -> Void) {
        guard let data = jsonConfig.data(using: .utf8) else {
            print("Error: Invalid JSON string")
            completion(false, "Invalid JSON string")
            return
        }

        do {
            if let configDict = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] {
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
