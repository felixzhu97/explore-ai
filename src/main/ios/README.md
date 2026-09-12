# AI iOS (SwiftUI)

Native iOS app for Explore AI with **in-app** Explore IAM Sign in
(Authorization Code + PKCE via `ASWebAuthenticationSession`) and a
**ChatGPT-style chat** surface (text + Qwen ASR/TTS via Speech).

Layout mirrors Apple’s [Food Truck](https://github.com/apple/sample-food-truck)
sample: domain folders under `AI/` (like `App/`), plus `Navigation/`,
`General/`, and `Brand/`. Files inside each folder stay flat. Entry is
`AI/AIApp.swift`; keep `Assets.xcassets` as a bundle.

After sign-in the root shows **Chat** (no Home).

## Requirements

- Xcode 16+ / [XcodeGen](https://github.com/yonaskolb/XcodeGen)
- iOS 17+ simulator or device
- Local Explore IAM (`http://localhost:9100`) and Explore AI API (`http://localhost:9000`)
  with JWT resource server enabled (`APP_OAUTH_EXPLORE_IAM_RESOURCE_SERVER=true`)
- Voice / dictation: explore-ml **Speech** on `:8004` (Qwen3-ASR + Qwen3-TTS)
- Physical device uses Mac LAN host `192.168.3.100` (not `localhost`) for API + IAM

## Open

```bash
cd src/main/ios
xcodegen generate
open AI.xcodeproj
```

## Sign in → Chat (self-test)

1. Start IAM (`:9100`) and AI (`:9000`).
2. Run the AI scheme on the Simulator.
3. Tap **Sign in with IAM** — system sheet (client `explore-ai-ios`).
4. Demo user: `demo` / `demo`.
5. Land on **Chat** (pill composer).

## Chat (self-test)

1. Start Speech (`:8004`), AI (`:9000`), IAM.
2. Type in the pill composer and send (↑), or:
   - **Mic** — dictate into the draft (Qwen ASR)
   - **Waveform** — full voice turn (ASR → chat → TTS)
3. Toolbar **New chat** clears the session; menu **Sign out** returns to login.

## Physical device

1. Unlock iPhone, trust this Mac, Connected in Xcode.
2. Same Wi‑Fi as Mac; allow Local Network.
3. If Mac IP ≠ `192.168.3.100`, edit `Config.developmentHost`.

## Test

```bash
cd src/main/ios
xcodebuild test -scheme AI \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  CODE_SIGNING_ALLOWED=NO
```
