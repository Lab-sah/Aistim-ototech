import Foundation

/// Nomor versi "X.Y.Z" (angka saja, seperti di extension/manifest.json).
enum Versi {
    static func valid(_ v: String?) -> Bool {
        guard let v = v else { return false }
        return v.range(of: #"^\d{1,4}\.\d{1,4}\.\d{1,4}$"#, options: .regularExpression) != nil
    }

    /// true bila a lebih baru daripada b; versi yang tidak sah dianggap tidak lebih baru.
    static func lebihBaru(_ a: String?, _ b: String?) -> Bool {
        guard valid(a), valid(b), let a = a, let b = b else { return false }
        let x = a.split(separator: ".").compactMap { Int($0) }
        let y = b.split(separator: ".").compactMap { Int($0) }
        guard x.count == 3, y.count == 3 else { return false }
        for i in 0..<3 where x[i] != y[i] {
            return x[i] > y[i]
        }
        return false
    }
}
