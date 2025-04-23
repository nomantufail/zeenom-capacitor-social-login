// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "Zeenom",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "Zeenom",
            targets: ["LoginPluginPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0"),
        .package(url: "https://github.com/firebase/firebase-ios-sdk.git",  from: "11.8.0"),
        .package(url: "https://github.com/google/GoogleSignIn-iOS.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "LoginPluginPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm"),
                .product(name: "FirebaseCore", package: "firebase-ios-sdk"),
                .product(name: "FirebaseAuth", package: "firebase-ios-sdk"),
                .product(name: "GoogleSignIn", package: "GoogleSignIn-iOS")
            ],
            path: "ios/Sources/LoginPluginPlugin"),
        .testTarget(
            name: "LoginPluginPluginTests",
            dependencies: ["LoginPluginPlugin"],
            path: "ios/Tests/LoginPluginPluginTests")
    ]
)
