import SwiftUI

struct VoiceConversationView: View {
  @StateObject private var model: VoiceConversationViewModel

  init(accessToken: String) {
    _model = StateObject(wrappedValue: VoiceConversationViewModel(accessToken: accessToken))
  }

  var body: some View {
    VStack(spacing: 24) {
      VStack(alignment: .leading, spacing: 12) {
        Text("You")
          .font(.subheadline.weight(.semibold))
          .foregroundStyle(.secondary)
        Text(model.userUtterance.isEmpty ? model.partialTranscript : model.userUtterance)
          .font(.body)
          .frame(maxWidth: .infinity, alignment: .leading)
          .padding()
          .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))

        Text("Assistant")
          .font(.subheadline.weight(.semibold))
          .foregroundStyle(.secondary)
        Text(model.assistantReply.isEmpty ? "—" : model.assistantReply)
          .font(.body)
          .frame(maxWidth: .infinity, alignment: .leading)
          .padding()
          .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
      }

      Spacer()

      Text(model.statusMessage)
        .font(.footnote)
        .foregroundStyle(.secondary)

      if let error = model.errorMessage {
        Text(error)
          .font(.footnote)
          .foregroundStyle(.red)
          .multilineTextAlignment(.center)
      }

      Button {
        if model.phase == .speaking {
          model.bargeIn()
        } else {
          model.toggleListening()
        }
      } label: {
        Image(systemName: micSymbol)
          .font(.system(size: 36, weight: .medium))
          .foregroundStyle(.white)
          .frame(width: 84, height: 84)
          .background(micColor, in: Circle())
      }
      .accessibilityLabel(micLabel)
      .padding(.bottom, 24)
    }
    .padding()
    .navigationTitle("Voice")
    .navigationBarTitleDisplayMode(.inline)
    .onDisappear { model.stopAll() }
  }

  private var micSymbol: String {
    switch model.phase {
    case .listening: return "stop.fill"
    case .speaking: return "mic.fill"
    default: return "mic.fill"
    }
  }

  private var micColor: Color {
    switch model.phase {
    case .listening: return .red
    case .speaking: return Color(red: 0, green: 0.4, blue: 0.8)
    default: return Color(red: 0, green: 0.4, blue: 0.8)
    }
  }

  private var micLabel: String {
    switch model.phase {
    case .listening: return "Stop listening"
    case .speaking: return "Interrupt and speak"
    default: return "Start listening"
    }
  }
}
