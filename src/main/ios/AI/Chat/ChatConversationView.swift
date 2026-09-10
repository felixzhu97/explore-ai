import SwiftUI

/// ChatGPT-style conversation: ghost assistant text, user bubbles, pill composer.
struct ChatConversationView: View {
  @StateObject private var model: ChatConversationViewModel
  @FocusState private var composerFocused: Bool
  var onSignOut: () -> Void

  init(accessToken: String, onSignOut: @escaping () -> Void = {}) {
    _model = StateObject(wrappedValue: ChatConversationViewModel(accessToken: accessToken))
    self.onSignOut = onSignOut
  }

  var body: some View {
    VStack(spacing: 0) {
      ScrollViewReader { proxy in
        ScrollView {
          LazyVStack(alignment: .leading, spacing: 20) {
            if model.turns.isEmpty {
              emptyState
            }
            ForEach(model.turns) { turn in
              messageRow(turn)
                .id(turn.id)
            }
          }
          .padding(.horizontal, 16)
          .padding(.vertical, 12)
        }
        .onChange(of: model.turns.last?.text) { _, _ in
          if let id = model.turns.last?.id {
            withAnimation(.easeOut(duration: 0.2)) {
              proxy.scrollTo(id, anchor: .bottom)
            }
          }
        }
      }

      if !model.statusHint.isEmpty {
        Text(model.statusHint)
          .font(.footnote)
          .foregroundStyle(AppTheme.muted)
          .frame(maxWidth: .infinity)
          .padding(.bottom, 4)
      }

      if let error = model.errorMessage {
        Text(error)
          .font(.footnote)
          .foregroundStyle(.red)
          .multilineTextAlignment(.center)
          .padding(.horizontal)
          .padding(.bottom, 4)
      }

      composer
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(AppTheme.canvas)
    }
    .background(AppTheme.canvas.ignoresSafeArea())
    .navigationTitle(model.title)
    .navigationBarTitleDisplayMode(.inline)
    .toolbar {
      ToolbarItem(placement: .topBarTrailing) {
        Button {
          model.startNewChat()
        } label: {
          Image(systemName: "square.and.pencil")
        }
        .accessibilityLabel("New chat")
        .disabled(model.phase != .idle && model.phase != .speaking)
      }
      ToolbarItem(placement: .topBarTrailing) {
        Menu {
          Button("New chat", systemImage: "square.and.pencil") {
            model.startNewChat()
          }
          Button("Sign out", systemImage: "rectangle.portrait.and.arrow.right", role: .destructive) {
            onSignOut()
          }
        } label: {
          Image(systemName: "ellipsis")
        }
      }
    }
    .onDisappear { model.stopAll() }
  }

  private var emptyState: some View {
    VStack(spacing: 8) {
      Spacer(minLength: 80)
      Text("Ask Explore AI")
        .font(.title2.weight(.semibold))
        .foregroundStyle(AppTheme.ink)
      Text("Type a message, or use the mic / voice mode.")
        .font(.body)
        .foregroundStyle(AppTheme.muted)
        .multilineTextAlignment(.center)
      Spacer(minLength: 40)
    }
    .frame(maxWidth: .infinity)
  }

  @ViewBuilder
  private func messageRow(_ turn: ChatTurn) -> some View {
    switch turn.role {
    case .user:
      HStack {
        Spacer(minLength: 48)
        Text(turn.text)
          .font(.body)
          .foregroundStyle(AppTheme.ink)
          .padding(.horizontal, 14)
          .padding(.vertical, 10)
          .background(AppTheme.parchment, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
      }
    case .assistant:
      VStack(alignment: .leading, spacing: 10) {
        Text(turn.text.isEmpty && turn.isStreaming ? "…" : turn.text)
          .font(.body)
          .foregroundStyle(AppTheme.ink)
          .frame(maxWidth: .infinity, alignment: .leading)
          .textSelection(.enabled)

        if !turn.isStreaming, !turn.text.isEmpty {
          HStack(spacing: 16) {
            Button {
              model.copyAssistant(turn.text)
            } label: {
              Image(systemName: "square.on.square")
            }
            Image(systemName: "hand.thumbsup")
            Image(systemName: "hand.thumbsdown")
            Image(systemName: "arrow.clockwise")
            Image(systemName: "ellipsis")
          }
          .font(.system(size: 15, weight: .regular))
          .foregroundStyle(AppTheme.muted)
          .buttonStyle(.plain)
        }
      }
    }
  }

  private var composer: some View {
    HStack(spacing: 10) {
      Button {
        // Placeholder for attachments / tools
      } label: {
        Image(systemName: "plus")
          .font(.system(size: 17, weight: .semibold))
          .foregroundStyle(AppTheme.ink)
          .frame(width: 36, height: 36)
          .background(AppTheme.parchment, in: Circle())
      }
      .accessibilityLabel("Add")

      HStack(spacing: 8) {
        TextField("Ask Explore AI", text: $model.draft, axis: .vertical)
          .lineLimit(1...5)
          .focused($composerFocused)
          .submitLabel(.send)
          .onSubmit { model.sendDraft() }

        Button {
          model.toggleDictation()
        } label: {
          Image(systemName: model.phase == .dictating ? "stop.fill" : "mic")
            .foregroundStyle(model.phase == .dictating ? .red : AppTheme.muted)
        }
        .accessibilityLabel(model.phase == .dictating ? "Stop dictation" : "Dictate")

        Button {
          model.toggleVoiceMode()
        } label: {
          Image(systemName: model.phase == .voiceListening || model.phase == .speaking ? "waveform" : "waveform")
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(.white)
            .frame(width: 32, height: 32)
            .background(
              model.phase == .voiceListening ? Color.red : AppTheme.primary,
              in: Circle()
            )
        }
        .accessibilityLabel("Voice mode")
      }
      .padding(.horizontal, 12)
      .padding(.vertical, 8)
      .background(AppTheme.parchment, in: Capsule())

      if model.canSend {
        Button {
          model.sendDraft()
        } label: {
          Image(systemName: "arrow.up")
            .font(.system(size: 15, weight: .bold))
            .foregroundStyle(.white)
            .frame(width: 32, height: 32)
            .background(AppTheme.primary, in: Circle())
        }
        .accessibilityLabel("Send")
        .transition(.scale.combined(with: .opacity))
      }
    }
    .animation(.easeOut(duration: 0.15), value: model.canSend)
  }
}
