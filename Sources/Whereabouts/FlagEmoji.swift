import Foundation

enum FlagEmoji {
    static func flag(forCountryCode code: String) -> String {
        let upper = code.uppercased()
        guard upper.count == 2 else { return "🏳️" }

        let base: UInt32 = 127397
        var scalars = String.UnicodeScalarView()
        for scalar in upper.unicodeScalars {
            guard let flagScalar = Unicode.Scalar(base + scalar.value) else { return "🏳️" }
            scalars.append(flagScalar)
        }
        return String(scalars)
    }
}
