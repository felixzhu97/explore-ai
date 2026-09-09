import SwiftUI

/// Root: login sheet flow when signed out; home when an IAM token is present.
struct ContentView: View {
  @State private var accessToken = KeychainStore.accessToken
  @State private var account: AccountMe?
  @State private var isHydrating = true

  var body: some View {
    Group {
      if isHydrating {
        ProgressView("Loading…")
      } else if accessToken != nil {
        HomeView(account: account, onSignOut: signOut)
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
      account = try await APIClient.accountMe(accessToken: token)
    } catch {
      KeychainStore.accessToken = nil
      accessToken = nil
      account = nil
    }
  }

  private func applySession(token: String, account: AccountMe) {
    KeychainStore.accessToken = token
    accessToken = token
    self.account = account
  }

  private func signOut() {
    KeychainStore.accessToken = nil
    accessToken = nil
    account = nil
  }
}

#Preview {
  ContentView()
}
