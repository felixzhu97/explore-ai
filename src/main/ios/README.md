# AI iOS (SwiftUI)

Native iOS hello-world app for this repo (Xcode SwiftUI App layout).

## Requirements

- Xcode 16+
- iOS 17+ simulator or device

## Open

```bash
cd src/main/ios
open AI.xcodeproj
```

## Test

```bash
cd src/main/ios
xcodebuild test -scheme AI \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  CODE_SIGNING_ALLOWED=NO
```

Bundle ID: `com.explore.ai`
