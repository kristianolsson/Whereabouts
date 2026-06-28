import Foundation

struct GeoResult {
    let countryCode: String
    let countryName: String?
    let city: String?
    let ip: String?
}

enum GeoLocatorError: Error {
    case decodingFailed
    case allProvidersFailed
}

final class GeoLocator {

    private struct IpInfoResponse: Decodable {
        let ip: String?
        let city: String?
        let country: String?
    }

    private struct IpApiResponse: Decodable {
        let status: String
        let countryCode: String?
        let country: String?
        let city: String?
        let query: String?
    }

    func lookup() async throws -> GeoResult {
        if let result = try? await lookupIpInfo() {
            return result
        }
        if let result = try? await lookupIpApi() {
            return result
        }
        throw GeoLocatorError.allProvidersFailed
    }

    private func lookupIpInfo() async throws -> GeoResult {
        let url = URL(string: "https://ipinfo.io/json")!
        let (data, _) = try await URLSession.shared.data(from: url)
        let decoded = try JSONDecoder().decode(IpInfoResponse.self, from: data)
        guard let country = decoded.country else { throw GeoLocatorError.decodingFailed }
        return GeoResult(countryCode: country, countryName: nil, city: decoded.city, ip: decoded.ip)
    }

    private func lookupIpApi() async throws -> GeoResult {
        let url = URL(string: "http://ip-api.com/json")!
        let (data, _) = try await URLSession.shared.data(from: url)
        let decoded = try JSONDecoder().decode(IpApiResponse.self, from: data)
        guard decoded.status == "success", let code = decoded.countryCode else {
            throw GeoLocatorError.decodingFailed
        }
        return GeoResult(countryCode: code, countryName: decoded.country, city: decoded.city, ip: decoded.query)
    }
}
