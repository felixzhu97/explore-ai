import SwiftUI

/// Sign in with Explore IAM (system auth sheet). On success, parent switches to home.
struct LoginView: View {
  var onSignedIn: (String, AccountMe) -> Void

  @State private var status =
    "Sign in with Explore IAM. A system sheet opens in-app — same pattern as Sign in with Google."
  @State private var isLoading = false
  @State private var errorMessage: String?

  var body: some View {
    VStack(spacing: 20) {
      Spacer(minLength: 24)
      Image(systemName: "sparkles")
        .imageScale(.large)
        .foregroundStyle(.tint)
      Text("AI")
        .font(.largeTitle.weight(.semibold))
      Text(status)
        .font(.body)
        .multilineTextAlignment(.center)
        .foregroundStyle(.secondary)
        .padding(.horizontal)
      if let errorMessage {
        Text(errorMessage)
          .font(.footnote)
          .foregroundStyle(.red)
          .multilineTextAlignment(.center)
          .padding(.horizontal)
      }
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
      .buttonStyle(.borderedProminent)
      .disabled(isLoading)
      .padding(.horizontal, 24)
      .accessibilityHint("Opens an in-app sign-in sheet for Explore IAM")
      Spacer()
    }
    .padding()
  }

  private func signIn() async {
    isLoading = true
    errorMessage = nil
    defer { isLoading = false }
    do {
      let tokens = try await AuthService().signIn()
      let me = try await APIClient.accountMe(accessToken: tokens.accessToken)
      onSignedIn(tokens.accessToken, me)
    } catch {
      if case AuthService.AuthError.cancelled = error { return }
      errorMessage = error.localizedDescription
      status = "Sign in failed. You can try again."
    }
  }
}

#Preview {
  LoginView { _, _ in }
}
