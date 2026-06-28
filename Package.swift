// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "Whereabouts",
    platforms: [.macOS(.v13)],
    targets: [
        .executableTarget(
            name: "Whereabouts",
            path: "Sources/Whereabouts"
        )
    ]
)
