import Foundation

struct Config: Sendable {
  let apiBaseURL: URL
  let iamIssuerURL: URL
  let iamClientId: String
  let iamRedirectURI: String
  let iamCallbackScheme: String

  /// Mac LAN IP for physical-device → host loopback services.
  /// Simulator can use localhost; a real iPhone cannot.
  private static let developmentHost: String = {
    #if targetEnvironment(simulator)
    return "localhost"
    #else
    // Override with EXPLORE_DEV_HOST if you change networks.
    return ProcessInfo.processInfo.environment["EXPLORE_DEV_HOST"]
      ?? "192.168.3.100"
    #endif
  }()

  static let local = Config(
    apiBaseURL: URL(string: "http://\(developmentHost):9000")!,
    iamIssuerURL: URL(string: "http://\(developmentHost):9100")!,
    iamClientId: "explore-ai-ios",
    iamRedirectURI: "com.explore.ai://oauth/callback",
    iamCallbackScheme: "com.explore.ai"
  )

  var asrWebSocketURL: URL {
    var components = URLComponents(url: apiBaseURL, resolvingAgainstBaseURL: false)!
    components.scheme = (components.scheme == "https") ? "wss" : "ws"
    components.path = "/ws/audio/transcribe"
    return components.url!
  }
}
