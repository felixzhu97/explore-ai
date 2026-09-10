import Foundation

struct AccountMe: Decodable {
  let mode: String
  let userId: String?
  let email: String?
  let plan: String?
}

enum APIClient {
  static func accountMe(config: Config = .local, accessToken: String) async throws -> AccountMe {
    var request = URLRequest(url: config.apiBaseURL.appendingPathComponent("api/account/me"))
    request.httpMethod = "GET"
    request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
    let (data, response) = try await URLSession.shared.data(for: request)
    guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
      let detail = String(data: data, encoding: .utf8) ?? "request failed"
      throw URLError(.badServerResponse, userInfo: [NSLocalizedDescriptionKey: detail])
    }
    return try JSONDecoder().decode(AccountMe.self, from: data)
  }
}
