import SwiftUI

struct ContentView: View {
  @State private var accessToken = KeychainStore.accessToken
  @State private var account: AccountMe?
  @State private var status = "Sign in with Explore IAM. A system sheet opens in-app — same pattern as Sign in with Google."
  @State private var isLoading = false
  @State private var errorMessage: String?

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
      if let errorMessage {
        Text(errorMessage)
          .font(.footnote)
          .foregroundStyle(.red)
          .multilineTextAlignment(.center)
      }

      if accessToken == nil {
        Button {
          Task { await signIn() }
        } label: {
          HStack(spacing: 8) {
            if isLoading {
              ProgressView()
            } else {
              Image(systemName: "person.badge.key.fill")
              Text("Sign in with IAM")
                .fontWeight(.semibold)
            }
          }
          .frame(maxWidth: .infinity)
          .frame(height: 44)
        }
        .buttonStyle(.bordered)
        .disabled(isLoading)
        .accessibilityHint("Opens an in-app sign-in sheet for Explore IAM")
      } else {
        Button {
          Task { await refreshMe() }
        } label: {
          HStack {
            if isLoading { ProgressView() }
            Text(isLoading ? "Loading…" : "Refresh /api/account/me")
              .fontWeight(.semibold)
          }
          .frame(maxWidth: .infinity)
          .frame(height: 44)
        }
        .buttonStyle(.borderedProminent)
        .disabled(isLoading)

        Button("Sign out", role: .destructive) {
          KeychainStore.accessToken = nil
          accessToken = nil
          account = nil
          errorMessage = nil
          status = "Signed out. Sign in again with the in-app IAM sheet."
        }
        .disabled(isLoading)
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
    errorMessage = nil
    defer { isLoading = false }
    do {
      let tokens = try await AuthService().signIn()
      KeychainStore.accessToken = tokens.accessToken
      accessToken = tokens.accessToken
      status = "Signed in. Calling AI…"
      await refreshMe()
    } catch {
      if case AuthService.AuthError.cancelled = error { return }
      errorMessage = error.localizedDescription
      status = "Sign in failed. You can try again."
    }
  }

  private func refreshMe() async {
    guard let token = accessToken else { return }
    isLoading = true
    errorMessage = nil
    defer { isLoading = false }
    do {
      let me = try await APIClient.accountMe(accessToken: token)
      account = me
      status = "Authenticated as \(me.mode)"
    } catch {
      errorMessage = error.localizedDescription
      status = "Could not load /api/account/me"
    }
  }
}

#Preview {
  ContentView()
}
