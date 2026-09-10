import Foundation

/// SSE client for `POST /api/text/chat/stream`.
enum ChatStreamClient {
  static func streamReply(
    accessToken: String,
    userText: String,
    sessionId: String?,
    config: Config = .local
  ) async throws -> (sessionId: String?, reply: String) {
    var request = URLRequest(url: config.apiBaseURL.appendingPathComponent("api/text/chat/stream"))
    request.httpMethod = "POST"
    request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
    request.setValue("text/event-stream", forHTTPHeaderField: "Accept")

    var body: [String: Any] = [
      "messages": [["role": "user", "content": userText]],
    ]
    if let sessionId, !sessionId.isEmpty {
      body["sessionId"] = sessionId
    }
    request.httpBody = try JSONSerialization.data(withJSONObject: body)

    let (bytes, response) = try await URLSession.shared.bytes(for: request)
    guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
      throw URLError(.badServerResponse)
    }

    var reply = ""
    var resolvedSession = sessionId
    for try await line in bytes.lines {
      guard line.hasPrefix("data:") else { continue }
      let payload = line.dropFirst(5).trimmingCharacters(in: .whitespaces)
      if payload == "[DONE]" { break }
      if let data = payload.data(using: .utf8),
         let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
      {
        if let sid = obj["sessionId"] as? String ?? obj["session_id"] as? String {
          resolvedSession = sid
        }
        if let content = obj["content"] as? String {
          reply += content
          continue
        }
        if let delta = obj["delta"] as? String {
          reply += delta
          continue
        }
      }
      // Plain token lines from Flux<String>
      if !payload.hasPrefix("{") {
        reply += payload
      }
    }
    return (resolvedSession, reply)
  }
}
