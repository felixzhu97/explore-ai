# AI iOS (SwiftUI)

Native iOS app for Explore AI with **in-app** Explore IAM Sign in
(Authorization Code + PKCE via `ASWebAuthenticationSession`) and **Voice
Conversation** (mic → streaming ASR → chat → TTS).

This is the same system auth-sheet pattern as Sign in with Google / Apple
OAuth: login stays attached to the app and does **not** hand off to external
Safari.

## Requirements

- Xcode 16+
- iOS 17+ simulator or device
- Local Explore IAM (`http://localhost:9100`) and Explore AI API (`http://localhost:9000`)
  with JWT resource server enabled (`APP_OAUTH_EXPLORE_IAM_RESOURCE_SERVER=true`)
- explore-ml **media-gen** on `:8003` (Qwen3-ASR streaming WS) for voice

## Open

```bash
cd src/main/ios
open AI.xcodeproj
```

## Sign in (self-test)

1. Start IAM (`:9100`) and AI (`:9000`).
2. Run the AI scheme on the Simulator.
3. Tap **Sign in with IAM** — a **system sign-in sheet** opens over the app
   (client `explore-ai-ios`, redirect `com.explore.ai://oauth/callback`).
4. Demo user: `demo` / `demo-password`.
5. After the sheet dismisses, the app stores the access token, loads
   `/api/account/me`, and **navigates to Home** (welcome + Sign out).
6. Cancel the sheet — the app stays on the login screen with no crash.
7. Sign out returns to login; sign in again may reuse IAM SSO cookies
   (`prefersEphemeralWebBrowserSession = false`).

## Voice conversation (self-test)

1. Start media-gen (`:8003`), AI (`:9000` with `ASR_PROVIDER=media-gen`), IAM.
2. Sign in, then tap **Voice conversation**.
3. Allow microphone access.
4. Tap the mic → speak → tap again to commit the turn.
5. Wait for assistant text + TTS playback.
6. While speaking, tap the mic again to barge-in.

## Test

```bash
cd src/main/ios
xcodebuild test -scheme AI \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  CODE_SIGNING_ALLOWED=NO
```

Bundle ID: `com.explore.ai`
