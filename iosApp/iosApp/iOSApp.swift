import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    guard
                        let token = URLComponents(url: url, resolvingAgainstBaseURL: false)?
                            .queryItems?.first(where: { $0.name == "token" })?.value
                    else { return }
                    MainViewControllerKt.handleMagicLinkToken(token: token)
                }
        }
    }
}