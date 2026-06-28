import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    guard
                        let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                        let token = components.queryItems?.first(where: { $0.name == "token" })?.value
                    else { return }
                    if url.host == "account" {
                        MainViewControllerKt.handleRecoveryEmailVerificationToken(token: token)
                    } else {
                        MainViewControllerKt.handleMagicLinkToken(token: token)
                    }
                }
        }
    }
}