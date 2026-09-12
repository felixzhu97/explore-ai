import Foundation

/// Product ASR WebSocket client (`/ws/audio/transcribe`).
final class AsrWebSocketClient: NSObject, URLSessionWebSocketDelegate {
  var onPartial: ((String) -> Void)?
  var onFinal: ((String) -> Void)?
  var onError: ((String) -> Void)?

  private var task: URLSessionWebSocketTask?
  private var session: URLSession?
  private let accessToken: String
  private let config: Config

  init(accessToken: String, config: Config = .local) {
    self.accessToken = accessToken
    self.config = config
  }

  func connect() {
    disconnect()
    var components = URLComponents(url: config.apiBaseURL, resolvingAgainstBaseURL: false)!
    components.scheme = (components.scheme == "https") ? "wss" : "ws"
    components.path = "/ws/audio/transcribe"
    guard let url = components.url else { return }
    var request = URLRequest(url: url)
    request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
    let session = URLSession(configuration: .default, delegate: self, delegateQueue: nil)
    self.session = session
    task = session.webSocketTask(with: request)
    task?.resume()
    receiveLoop()
  }

  func sendAudioBase64(_ data: String, sampleRate: Int = 16_000) {
    send(json: ["type": "audio", "data": data, "sample_rate": sampleRate])
  }

  func commit() {
    send(json: ["type": "commit"])
  }

  func stop() {
    send(json: ["type": "stop"])
  }

  func disconnect() {
    task?.cancel(with: .goingAway, reason: nil)
    task = nil
    session?.invalidateAndCancel()
    session = nil
  }

  private func send(json: [String: Any]) {
    guard let data = try? JSONSerialization.data(withJSONObject: json),
          let text = String(data: data, encoding: .utf8)
    else { return }
    task?.send(.string(text)) { [weak self] error in
      if let error {
        self?.onError?(error.localizedDescription)
      }
    }
  }

  private func receiveLoop() {
    task?.receive { [weak self] result in
      guard let self else { return }
      switch result {
      case .failure(let error):
        self.onError?(error.localizedDescription)
      case .success(let message):
        if case .string(let text) = message {
          self.handle(text)
        }
        self.receiveLoop()
      }
    }
  }

  private func handle(_ text: String) {
    guard let data = text.data(using: .utf8),
          let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
          let type = obj["type"] as? String
    else { return }
    let payload = (obj["text"] as? String) ?? ""
    switch type {
    case "partial":
      onPartial?(payload)
    case "final":
      onFinal?(payload)
    case "error":
      onError?(payload)
    default:
      break
    }
  }
}
