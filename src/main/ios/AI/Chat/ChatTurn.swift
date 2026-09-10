import Foundation

enum ChatRole: String, Equatable {
  case user
  case assistant
}

struct ChatTurn: Identifiable, Equatable {
  let id: UUID
  var role: ChatRole
  var text: String
  var isStreaming: Bool

  init(id: UUID = UUID(), role: ChatRole, text: String, isStreaming: Bool = false) {
    self.id = id
    self.role = role
    self.text = text
    self.isStreaming = isStreaming
  }
}
