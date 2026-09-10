import Foundation
import Combine

enum VoiceConversationPhase: Equatable {
  case idle
  case listening
  case finalizing
  case thinking
  case speaking
}

/// Orchestrates mic → ASR → chat → TTS with barge-in.
@MainActor
final class VoiceConversationViewModel: ObservableObject {
  @Published private(set) var phase: VoiceConversationPhase = .idle
  @Published private(set) var partialTranscript = ""
  @Published private(set) var userUtterance = ""
  @Published private(set) var assistantReply = ""
  @Published private(set) var statusMessage = "Tap the mic to talk"
  @Published private(set) var errorMessage: String?

  private let accessToken: String
  private let config: Config
  private let audio = AudioSessionManager()
  private var asr: AsrWebSocketClient?
  private var sessionId: String?
  private var chunkBuffer = Data()
  private var lastSend = Date.distantPast

  init(accessToken: String, config: Config = .local) {
    self.accessToken = accessToken
    self.config = config
  }

  func toggleListening() {
    switch phase {
    case .idle, .speaking:
      Task { await startListening() }
    case .listening:
      Task { await finishListening() }
    case .finalizing, .thinking:
      break
    }
  }

  func bargeIn() {
    guard phase == .speaking else { return }
    audio.stopPlayback()
    Task { await startListening() }
  }

  func stopAll() {
    asr?.stop()
    asr?.disconnect()
    asr = nil
    audio.stopCapture()
    audio.stopPlayback()
    phase = .idle
    statusMessage = "Tap the mic to talk"
  }

  private func startListening() async {
    errorMessage = nil
    audio.stopPlayback()
    let allowed = await audio.requestPermission()
    guard allowed else {
      errorMessage = "Microphone permission is required"
      return
    }
    partialTranscript = ""
    userUtterance = ""
    let client = AsrWebSocketClient(accessToken: accessToken, config: config)
    client.onPartial = { [weak self] text in
      Task { @MainActor in
        self?.partialTranscript = text
      }
    }
    client.onFinal = { [weak self] text in
      Task { @MainActor in
        self?.userUtterance = text
      }
    }
    client.onError = { [weak self] message in
      Task { @MainActor in
        self?.errorMessage = message
      }
    }
    asr = client
    client.connect()
    audio.onPCMChunk = { [weak self] data in
      Task { @MainActor in
        self?.enqueuePCM(data)
      }
    }
    do {
      try audio.startCapture()
      phase = .listening
      statusMessage = "Listening… tap again when done"
    } catch {
      errorMessage = error.localizedDescription
      phase = .idle
    }
  }

  private func enqueuePCM(_ data: Data) {
    guard phase == .listening else { return }
    chunkBuffer.append(data)
    let now = Date()
    guard now.timeIntervalSince(lastSend) >= 0.4, chunkBuffer.count >= 3200 else { return }
    let chunk = chunkBuffer
    chunkBuffer.removeAll(keepingCapacity: true)
    lastSend = now
    asr?.sendAudioBase64(chunk.base64EncodedString())
  }

  private func finishListening() async {
    phase = .finalizing
    statusMessage = "Recognizing…"
    audio.stopCapture()
    if !chunkBuffer.isEmpty {
      asr?.sendAudioBase64(chunkBuffer.base64EncodedString())
      chunkBuffer.removeAll()
    }
    asr?.commit()
    try? await Task.sleep(nanoseconds: 800_000_000)
    let text = userUtterance.isEmpty ? partialTranscript : userUtterance
    asr?.stop()
    asr?.disconnect()
    asr = nil
    guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
      phase = .idle
      statusMessage = "No speech detected"
      return
    }
    userUtterance = text
    await askAssistant(userText: text)
  }

  private func askAssistant(userText: String) async {
    phase = .thinking
    statusMessage = "Thinking…"
    assistantReply = ""
    do {
      let result = try await ChatStreamClient.streamReply(
        accessToken: accessToken,
        userText: userText,
        sessionId: sessionId,
        config: config
      )
      sessionId = result.sessionId ?? sessionId
      assistantReply = result.reply
      guard !assistantReply.isEmpty else {
        phase = .idle
        statusMessage = "Empty reply"
        return
      }
      phase = .speaking
      statusMessage = "Speaking… tap mic to interrupt"
      let audioData = try await TtsClient.speak(
        accessToken: accessToken,
        text: assistantReply,
        config: config
      )
      try audio.play(data: audioData)
    } catch {
      errorMessage = error.localizedDescription
      phase = .idle
      statusMessage = "Tap the mic to talk"
    }
  }
}
