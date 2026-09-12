import Foundation

/// TTS client for `POST /api/audio/speak`.
enum TtsClient {
  static func speak(
    accessToken: String,
    text: String,
    config: Config = .local
  ) async throws -> Data {
    var request = URLRequest(url: config.apiBaseURL.appendingPathComponent("api/audio/speak"))
    request.httpMethod = "POST"
    request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
    request.httpBody = try JSONSerialization.data(withJSONObject: ["text": text])
    let (data, response) = try await URLSession.shared.data(for: request)
    guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
      throw URLError(.badServerResponse)
    }
    return data
  }
}
