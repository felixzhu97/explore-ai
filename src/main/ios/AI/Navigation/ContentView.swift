import SwiftUI

/// Root: login when signed out; Chat when an IAM token is present.
struct ContentView: View {
  @State private var accessToken = KeychainStore.accessToken
  @State private var isHydrating = true

  var body: some View {
    Group {
      if isHydrating {
        ProgressView("Loading…")
      } else if let token = accessToken {
        NavigationStack {
          ChatConversationView(accessToken: token, onSignOut: signOut)
        }
      } else {
        LoginView(onSignedIn: applySession)
      }
    }
    .task { await hydrate() }
  }

  private func hydrate() async {
    defer { isHydrating = false }
    guard let token = accessToken else { return }
    do {
      _ = try await APIClient.accountMe(accessToken: token)
    } catch {
      KeychainStore.accessToken = nil
      accessToken = nil
    }
  }

  private func applySession(token: String, account: AccountMe) {
    _ = account
    KeychainStore.accessToken = token
    accessToken = token
  }

  private func signOut() {
    KeychainStore.accessToken = nil
    accessToken = nil
  }
}

#Preview {
  ContentView()
}
