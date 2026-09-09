import SwiftUI

struct ContentView: View {
  @State private var accessToken = KeychainStore.accessToken
  @State private var account: AccountMe?
  @State private var status = "Sign in with Explore IAM to call the AI API."
  @State private var isLoading = false

  var body: some View {
    VStack(spacing: 20) {
      Image(systemName: "sparkles")
        .imageScale(.large)
        .foregroundStyle(.tint)
      Text("AI")
        .font(.largeTitle.weight(.semibold))
      Text(status)
        .font(.body)
        .multilineTextAlignment(.center)
        .foregroundStyle(.secondary)
      if let account {
        Text(account.email ?? account.userId ?? account.mode)
          .font(.headline)
      }
      if accessToken == nil {
        Button {
          Task { await signIn() }
        } label: {
          Text(isLoading ? "Signing in…" : "Sign in with IAM")
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .disabled(isLoading)
      } else {
        Button {
          Task { await refreshMe() }
        } label: {
          Text(isLoading ? "Loading…" : "Refresh /api/account/me")
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .disabled(isLoading)
        Button("Sign out", role: .destructive) {
          KeychainStore.accessToken = nil
          accessToken = nil
          account = nil
          status = "Signed out."
        }
      }
    }
    .padding()
    .task {
      if accessToken != nil {
        await refreshMe()
      }
    }
  }

  private func signIn() async {
    isLoading = true
    defer { isLoading = false }
    do {
      let tokens = try await AuthService().signIn()
      KeychainStore.accessToken = tokens.accessToken
      accessToken = tokens.accessToken
      status = "Signed in. Calling AI…"
      await refreshMe()
    } catch {
      if case AuthService.AuthError.cancelled = error { return }
      status = error.localizedDescription
    }
  }

  private func refreshMe() async {
    guard let token = accessToken else { return }
    isLoading = true
    defer { isLoading = false }
    do {
      let me = try await APIClient.accountMe(accessToken: token)
      account = me
      status = "Authenticated as \(me.mode)"
    } catch {
      status = error.localizedDescription
    }
  }
}

#Preview {
  ContentView()
}
