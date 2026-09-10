import Foundation
import Combine
import UIKit

enum ChatConversationPhase: Equatable {
  case idle
  case dictating
  case voiceListening
  case recognizing
  case thinking
  case speaking
}

/// ChatGPT-style session: text composer + Qwen ASR/TTS voice mode.
@MainActor
final class ChatConversationViewModel: ObservableObject {
  @Published private(set) var turns: [ChatTurn] = []
  @Published private(set) var phase: ChatConversationPhase = .idle
  @Published var draft = ""
  @Published private(set) var statusHint = ""
  @Published private(set) var errorMessage: String?
  @Published private(set) var title = "New chat"

  private let accessToken: String
  private let config: Config
  private let audio = AudioSessionManager()
  private var asr: AsrWebSocketClient?
  private var sessionId: String?
  private var chunkBuffer = Data()
  private var lastSend = Date.distantPast
  private var finalTranscriptReceived = false
  private var partialTranscript = ""
  private var userUtterance = ""
  private var voiceModeForNextReply = false

  init(accessToken: String, config: Config = .local) {
    self.accessToken = accessToken
    self.config = config
  }

  var canSend: Bool {
    !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
      && (phase == .idle || phase == .speaking)
  }

  func startNewChat() {
    stopAll()
    turns = []
    sessionId = nil
    draft = ""
    title = "New chat"
    errorMessage = nil
    statusHint = ""
  }

  func sendDraft() {
    let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !text.isEmpty else { return }
    draft = ""
    voiceModeForNextReply = false
    Task { await askAssistant(userText: text, speakReply: false) }
  }

  func copyAssistant(_ text: String) {
    UIPasteboard.general.string = text
  }

  func toggleDictation() {
    switch phase {
    case .idle, .speaking:
      Task { await startListening(mode: .dictating) }
    case .dictating:
      Task { await finishListening(mode: .dictating) }
    default:
      break
    }
  }

  func toggleVoiceMode() {
    switch phase {
    case .idle:
      Task { await startListening(mode: .voiceListening) }
    case .speaking:
      bargeIn()
    case .voiceListening:
      Task { await finishListening(mode: .voiceListening) }
    default:
      break
    }
  }

  func bargeIn() {
    guard phase == .speaking else { return }
    audio.stopPlayback()
    Task { await startListening(mode: .voiceListening) }
  }

  func stopAll() {
    asr?.stop()
    asr?.disconnect()
    asr = nil
    audio.stopCapture()
    audio.stopPlayback()
    phase = .idle
    statusHint = ""
  }

  private func startListening(mode: ChatConversationPhase) async {
    errorMessage = nil
    audio.stopPlayback()
    let allowed = await audio.requestPermission()
    guard allowed else {
      errorMessage = "Microphone permission is required"
      return
    }
    partialTranscript = ""
    userUtterance = ""
    finalTranscriptReceived = false
    chunkBuffer.removeAll(keepingCapacity: true)

    let client = AsrWebSocketClient(accessToken: accessToken, config: config)
    client.onPartial = { [weak self] text in
      Task { @MainActor in
        self?.partialTranscript = text
        if mode == .dictating {
          self?.draft = text
        }
      }
    }
    client.onFinal = { [weak self] text in
      Task { @MainActor in
        self?.userUtterance = text
        self?.finalTranscriptReceived = true
        if mode == .dictating {
          self?.draft = text
        }
      }
    }
    client.onError = { [weak self] message in
      Task { @MainActor in
        self?.errorMessage = message
        self?.audio.stopCapture()
        self?.asr?.disconnect()
        self?.asr = nil
        self?.phase = .idle
        self?.statusHint = ""
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
      phase = mode
      statusHint = mode == .dictating ? "Dictating… tap mic when done" : "Listening… tap again when done"
    } catch {
      errorMessage = error.localizedDescription
      phase = .idle
    }
  }

  private func enqueuePCM(_ data: Data) {
    guard phase == .dictating || phase == .voiceListening else { return }
    chunkBuffer.append(data)
    let now = Date()
    guard now.timeIntervalSince(lastSend) >= 0.4, chunkBuffer.count >= 3200 else { return }
    let chunk = chunkBuffer
    chunkBuffer.removeAll(keepingCapacity: true)
    lastSend = now
    asr?.sendAudioBase64(chunk.base64EncodedString())
  }

  private func finishListening(mode: ChatConversationPhase) async {
    phase = .recognizing
    statusHint = "Recognizing…"
    audio.stopCapture()
    if !chunkBuffer.isEmpty {
      asr?.sendAudioBase64(chunkBuffer.base64EncodedString())
      chunkBuffer.removeAll()
    }
    asr?.commit()
    let text = await waitForFinalTranscript(timeoutSeconds: 60)
    asr?.stop()
    asr?.disconnect()
    asr = nil
    let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else {
      phase = .idle
      statusHint = ""
      errorMessage = "No speech detected"
      return
    }
    if mode == .dictating {
      draft = trimmed
      phase = .idle
      statusHint = ""
      return
    }
    voiceModeForNextReply = true
    await askAssistant(userText: trimmed, speakReply: true)
  }

  private func waitForFinalTranscript(timeoutSeconds: Double) async -> String {
    let deadline = Date().addingTimeInterval(timeoutSeconds)
    while Date() < deadline {
      if finalTranscriptReceived { return userUtterance }
      try? await Task.sleep(nanoseconds: 200_000_000)
    }
    return userUtterance.isEmpty ? partialTranscript : userUtterance
  }

  private func askAssistant(userText: String, speakReply: Bool) async {
    turns.append(ChatTurn(role: .user, text: userText))
    let assistantId = UUID()
    turns.append(ChatTurn(id: assistantId, role: .assistant, text: "", isStreaming: true))
    phase = .thinking
    statusHint = "Thinking…"
    errorMessage = nil
    if title == "New chat" {
      title = String(userText.prefix(28))
    }

    do {
      if sessionId == nil {
        sessionId = UUID().uuidString
      }
      let result = try await ChatStreamClient.streamReply(
        accessToken: accessToken,
        userText: userText,
        sessionId: sessionId,
        config: config,
        voiceMode: speakReply || voiceModeForNextReply,
        onToken: { [weak self] token in
          Task { @MainActor in
            guard let self,
                  let idx = self.turns.firstIndex(where: { $0.id == assistantId })
            else { return }
            self.turns[idx].text += token
          }
        }
      )
      sessionId = result.sessionId ?? sessionId
      if let idx = turns.firstIndex(where: { $0.id == assistantId }) {
        turns[idx].text = result.reply
        turns[idx].isStreaming = false
      }
      guard !result.reply.isEmpty else {
        phase = .idle
        statusHint = ""
        errorMessage = "Empty reply"
        return
      }
      if speakReply {
        phase = .speaking
        statusHint = "Speaking… tap waveform to interrupt"
        let speakText = Self.speakableExcerpt(result.reply)
        let audioData = try await TtsClient.speak(
          accessToken: accessToken,
          text: speakText,
          config: config
        )
        try audio.play(data: audioData)
      }
      phase = .idle
      statusHint = ""
      voiceModeForNextReply = false
    } catch {
      if let idx = turns.firstIndex(where: { $0.id == assistantId }) {
        turns[idx].isStreaming = false
        if turns[idx].text.isEmpty {
          turns.remove(at: idx)
        }
      }
      errorMessage = error.localizedDescription
      phase = .idle
      statusHint = ""
      voiceModeForNextReply = false
    }
  }

  static func speakableExcerpt(_ text: String, maxChars: Int = 160) -> String {
    let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
    guard trimmed.count > maxChars else { return trimmed }
    let enders: [Character] = [".", "!", "?", "。", "！", "？", "\n"]
    if let idx = trimmed.firstIndex(where: { enders.contains($0) }) {
      let sentence = String(trimmed[...idx]).trimmingCharacters(in: .whitespacesAndNewlines)
      if sentence.count >= 12, sentence.count <= maxChars * 2 {
        return sentence
      }
    }
    let end = trimmed.index(trimmed.startIndex, offsetBy: maxChars)
    return String(trimmed[..<end]) + "…"
  }
}
