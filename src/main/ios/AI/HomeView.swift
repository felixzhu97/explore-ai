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
          .foregroundStyle(AppTheme.primary)
        Text("Home")
          .font(.largeTitle.weight(.semibold))
          .foregroundStyle(AppTheme.ink)
        Text(welcomeLine)
          .font(.body)
          .multilineTextAlignment(.center)
          .foregroundStyle(AppTheme.muted)
          .padding(.horizontal)

        NavigationLink {
          ChatConversationView(accessToken: accessToken)
        } label: {
          Label("Chat", systemImage: "bubble.left.and.bubble.right")
            .font(.body.weight(.semibold))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .foregroundStyle(.white)
            .background(AppTheme.primary, in: Capsule())
        }
        .padding(.horizontal)

        Spacer()
      }
      .frame(maxWidth: .infinity, maxHeight: .infinity)
      .background(AppTheme.canvas)
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
