import SwiftUI

/// Post-login home for Explore AI.
struct HomeView: View {
  let account: AccountMe?
  let accessToken: String
  var onSignOut: () -> Void

  var body: some View {
    NavigationStack {
      VStack(spacing: 20) {
        Spacer(minLength: 40)
        Image(systemName: "sparkles")
          .font(.system(size: 44))
          .foregroundStyle(.tint)
        Text("Home")
          .font(.largeTitle.weight(.semibold))
        Text(welcomeLine)
          .font(.body)
          .multilineTextAlignment(.center)
          .foregroundStyle(.secondary)
          .padding(.horizontal)

        NavigationLink {
          VoiceConversationView(accessToken: accessToken)
        } label: {
          Label("Voice conversation", systemImage: "waveform")
            .font(.body.weight(.semibold))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .foregroundStyle(.white)
            .background(Color(red: 0, green: 0.4, blue: 0.8), in: Capsule())
        }
        .padding(.horizontal)

        Spacer()
      }
      .frame(maxWidth: .infinity, maxHeight: .infinity)
      .padding()
      .navigationTitle("AI")
      .navigationBarTitleDisplayMode(.inline)
      .toolbar {
        ToolbarItem(placement: .topBarTrailing) {
          Button("Sign out", role: .destructive, action: onSignOut)
        }
      }
    }
  }

  private var welcomeLine: String {
    if let email = account?.email, !email.isEmpty {
      return "Welcome, \(email)"
    }
    if let userId = account?.userId {
      return "Welcome. Account \(userId.prefix(8))…"
    }
    return "Welcome to Explore AI."
  }
}

#Preview {
  HomeView(
    account: AccountMe(mode: "authenticated", userId: "u1", email: "demo@explore-iam.local", plan: "free"),
    accessToken: "preview"
  ) {}
}
