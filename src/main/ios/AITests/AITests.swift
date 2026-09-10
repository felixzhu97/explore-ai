import XCTest
@testable import AI

final class AITests: XCTestCase {
  func testContentViewExists() {
    XCTAssertNotNil(ContentView())
  }

  func testShouldMapLocalApiBaseToAsrWebSocketURL() {
    let url = Config.local.asrWebSocketURL
    XCTAssertEqual(url.scheme, "ws")
    XCTAssertEqual(url.host, "localhost")
    XCTAssertEqual(url.port, 9000)
    XCTAssertEqual(url.path, "/ws/audio/transcribe")
  }

  @MainActor
  func testShouldStartVoiceConversationInIdlePhase() {
    let model = VoiceConversationViewModel(accessToken: "test")
    XCTAssertEqual(model.phase, .idle)
  }
}
