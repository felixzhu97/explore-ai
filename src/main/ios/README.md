# AI iOS (SwiftUI)

Native iOS app for Explore AI with **in-app** Explore IAM Sign in
(Authorization Code + PKCE via `ASWebAuthenticationSession`).

This is the same system auth-sheet pattern as Sign in with Google / Apple
OAuth: login stays attached to the app and does **not** hand off to external
Safari.

## Requirements

- Xcode 16+
- iOS 17+ simulator or device
- Local Explore IAM (`http://localhost:9100`) and Explore AI API (`http://localhost:9000`)
  with JWT resource server enabled (`APP_OAUTH_EXPLORE_IAM_RESOURCE_SERVER=true`)

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
5. After the sheet dismisses, the app stores the access token in Keychain and
   calls `GET /api/account/me` with `Authorization: Bearer`.
6. Cancel the sheet — the app stays signed out with no crash.
7. Tap **Sign out**, then sign in again — SSO cookies should reduce re-entry
   (`prefersEphemeralWebBrowserSession = false`).

## Test

```bash
cd src/main/ios
xcodebuild test -scheme AI \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  CODE_SIGNING_ALLOWED=NO
```

Bundle ID: `com.explore.ai`
