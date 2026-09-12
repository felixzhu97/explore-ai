import XCTest
@testable import AI

final class AITests: XCTestCase {
  func testContentViewExists() {
    XCTAssertNotNil(ContentView())
  }

  func testShouldMapLocalApiBaseToAsrWebSocketURL() {
    let url = Config.local.asrWebSocketURL
    XCTAssertEqual(url.scheme, "ws")
    #if targetEnvironment(simulator)
    XCTAssertEqual(url.host, "localhost")
    #else
    XCTAssertEqual(url.host, "192.168.3.100")
    #endif
    XCTAssertEqual(url.port, 9000)
    XCTAssertEqual(url.path, "/ws/audio/transcribe")
  }

  @MainActor
  func testShouldStartChatConversationInIdlePhase() {
    let model = ChatConversationViewModel(accessToken: "test")
    XCTAssertEqual(model.phase, .idle)
    XCTAssertTrue(model.turns.isEmpty)
  }

  func testShouldParseMessageTokenFromChatStreamPayload() {
    let fragment = ChatStreamClient.tokenFragment(
      from: #"{"type":"message","token":"Hello!"}"#
    )
    XCTAssertEqual(fragment, "Hello!")
  }

  func testShouldIgnoreNonMessageJsonWithoutToken() {
    let fragment = ChatStreamClient.tokenFragment(
      from: #"{"type":"sources","items":[]}"#
    )
    XCTAssertNil(fragment)
  }

  func testShouldShortenLongAssistantReplyForTts() {
    let long =
      "Hello! How can I help you today? I can look up weather forecasts, search the web for current facts, or query your knowledge base documents."
    let excerpt = ChatConversationViewModel.speakableExcerpt(long)
    XCTAssertTrue(excerpt.count <= 180)
    XCTAssertTrue(excerpt.hasPrefix("Hello!"))
  }

  @MainActor
  func testShouldClearSessionWhenStartingNewChat() {
    let model = ChatConversationViewModel(accessToken: "test")
    model.draft = "hello"
    model.startNewChat()
    XCTAssertEqual(model.draft, "")
    XCTAssertEqual(model.title, "New chat")
    XCTAssertTrue(model.turns.isEmpty)
  }
}
