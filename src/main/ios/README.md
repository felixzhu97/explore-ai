# AI iOS (SwiftUI)

Native iOS app for Explore AI with Explore IAM Sign in (Authorization Code + PKCE).

## Requirements

- Xcode 16+
- iOS 17+ simulator or device
- Local Explore IAM (`http://localhost:9100`) and Explore AI API (`http://localhost:9000`)

## Open

```bash
cd src/main/ios
open AI.xcodeproj
```

## Sign in

1. Start IAM and AI backends.
2. Tap **Sign in with IAM** (client `explore-ai-ios`, redirect `com.explore.ai://oauth/callback`).
3. Demo user: `demo` / `demo-password` (IAM).
4. The app stores the access token in Keychain and calls `GET /api/account/me` with `Authorization: Bearer`.

## Test

```bash
cd src/main/ios
xcodebuild test -scheme AI \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  CODE_SIGNING_ALLOWED=NO
```

Bundle ID: `com.explore.ai`
