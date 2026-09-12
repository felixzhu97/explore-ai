import AVFoundation
import Foundation

/// Captures mic PCM (16 kHz mono) and plays TTS audio.
@MainActor
final class AudioSessionManager: NSObject {
  var onPCMChunk: ((Data) -> Void)?

  private let engine = AVAudioEngine()
  private var player: AVAudioPlayer?
  private var converter: AVAudioConverter?
  private let targetSampleRate: Double = 16_000

  func requestPermission() async -> Bool {
    await withCheckedContinuation { continuation in
      AVAudioApplication.requestRecordPermission { allowed in
        continuation.resume(returning: allowed)
      }
    }
  }

  func startCapture() throws {
    let session = AVAudioSession.sharedInstance()
    try session.setCategory(.playAndRecord, mode: .voiceChat, options: [.defaultToSpeaker, .allowBluetooth])
    try session.setActive(true)

    let input = engine.inputNode
    let inputFormat = input.outputFormat(forBus: 0)
    guard let pcmFormat = AVAudioFormat(
      commonFormat: .pcmFormatInt16,
      sampleRate: targetSampleRate,
      channels: 1,
      interleaved: true
    ) else {
      throw URLError(.cannotCreateFile)
    }
    converter = AVAudioConverter(from: inputFormat, to: pcmFormat)

    input.removeTap(onBus: 0)
    input.installTap(onBus: 0, bufferSize: 2048, format: inputFormat) { [weak self] buffer, _ in
      guard let self else { return }
      self.convertAndEmit(buffer: buffer, to: pcmFormat)
    }
    try engine.start()
  }

  func stopCapture() {
    engine.inputNode.removeTap(onBus: 0)
    engine.stop()
  }

  func stopPlayback() {
    player?.stop()
    player = nil
  }

  func play(data: Data) throws {
    stopPlayback()
    player = try AVAudioPlayer(data: data)
    player?.prepareToPlay()
    player?.play()
  }

  private func convertAndEmit(buffer: AVAudioPCMBuffer, to format: AVAudioFormat) {
    guard let converter else { return }
    let ratio = format.sampleRate / buffer.format.sampleRate
    let capacity = AVAudioFrameCount(Double(buffer.frameLength) * ratio) + 32
    guard let out = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: capacity) else { return }
    var error: NSError?
    let inputBlock: AVAudioConverterInputBlock = { _, outStatus in
      outStatus.pointee = .haveData
      return buffer
    }
    converter.convert(to: out, error: &error, withInputFrom: inputBlock)
    guard error == nil, let channel = out.int16ChannelData?[0] else { return }
    let byteCount = Int(out.frameLength) * MemoryLayout<Int16>.size
    let data = Data(bytes: channel, count: byteCount)
    Task { @MainActor in
      self.onPCMChunk?(data)
    }
  }
}
