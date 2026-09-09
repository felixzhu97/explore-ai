import Foundation

struct Config: Sendable {
  let apiBaseURL: URL
  let iamIssuerURL: URL
  let iamClientId: String
  let iamRedirectURI: String
  let iamCallbackScheme: String

  static let local = Config(
    apiBaseURL: URL(string: "http://localhost:9000")!,
    iamIssuerURL: URL(string: "http://localhost:9100")!,
    iamClientId: "explore-ai-ios",
    iamRedirectURI: "com.explore.ai://oauth/callback",
    iamCallbackScheme: "com.explore.ai"
  )
}
