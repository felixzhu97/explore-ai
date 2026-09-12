import Foundation

/// SSE client for `POST /api/text/chat/stream`.
enum ChatStreamClient {
  static func streamReply(
    accessToken: String,
    userText: String,
    sessionId: String?,
    config: Config = .local,
    voiceMode: Bool = false,
    onToken: ((String) -> Void)? = nil
  ) async throws -> (sessionId: String?, reply: String) {
    var request = URLRequest(url: config.apiBaseURL.appendingPathComponent("api/text/chat/stream"))
    request.httpMethod = "POST"
    request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
    request.setValue("text/event-stream", forHTTPHeaderField: "Accept")

    var messages: [[String: String]] = []
    if voiceMode {
      messages.append([
        "role": "system",
        "content":
          "You are in a spoken voice conversation. Reply in at most two short sentences. No lists, headings, or long explanations.",
      ])
    }
    messages.append(["role": "user", "content": userText])
    var body: [String: Any] = ["messages": messages]
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
      if let fragment = Self.tokenFragment(from: payload) {
        reply += fragment
        onToken?(fragment)
      }
      if let sid = Self.sessionId(from: payload) {
        resolvedSession = sid
      }
    }
    return (resolvedSession, reply)
  }

  /// Parses one SSE `data:` payload into a text fragment.
  /// Product stream emits `{"type":"message","token":"..."}` (same as web).
  static func tokenFragment(from payload: String) -> String? {
    if payload.isEmpty { return nil }
    if let data = payload.data(using: .utf8),
       let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
    {
      if let token = obj["token"] as? String {
        return token
      }
      if let content = obj["content"] as? String {
        return content
      }
      if let delta = obj["delta"] as? String {
        return delta
      }
      return nil
    }
    if !payload.hasPrefix("{") {
      return payload
    }
    return nil
  }

  static func sessionId(from payload: String) -> String? {
    guard let data = payload.data(using: .utf8),
          let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
    else { return nil }
    return (obj["sessionId"] as? String) ?? (obj["session_id"] as? String)
  }
}
