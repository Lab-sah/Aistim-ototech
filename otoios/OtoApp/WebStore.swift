import Foundation

/// Menyajikan isi halaman web aplikasi. Urutannya: salinan unduhan di Application Support/web (hasil pembaruan dari GitHub)
/// bila ada dan lebih baru, jika tidak isi bawaan aplikasi (folder "web" di bundle, disalin dari folder extension saat build).
final class WebStore {
    /// Folder salinan unduhan: Application Support/web
    let dir: URL
    private let bundleDir: URL?
    private let fm = FileManager.default

    init() {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: base, withIntermediateDirectories: true)
        dir = base.appendingPathComponent("web", isDirectory: true)
        bundleDir = Bundle.main.url(forResource: "web", withExtension: nil)
    }

    // MARK: Versi

    /// Membaca "version" dari isi manifest.json; nil bila tidak sah.
    static func bacaVersi(_ data: Data) -> String? {
        guard let j = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let v = j["version"] as? String, Versi.valid(v) else { return nil }
        return v
    }

    func versiBawaan() -> String {
        guard let b = bundleDir, let d = try? Data(contentsOf: b.appendingPathComponent("manifest.json")),
              let v = WebStore.bacaVersi(d) else { return "0.0.0" }
        return v
    }

    /// Versi salinan unduhan, atau nil bila tidak ada.
    func versiUnduhan() -> String? {
        guard let d = try? Data(contentsOf: dir.appendingPathComponent("manifest.json")) else { return nil }
        return WebStore.bacaVersi(d)
    }

    /// Versi yang sedang dipakai.
    func versi() -> String { versiUnduhan() ?? versiBawaan() }

    /// Dipanggil saat aplikasi mulai: salinan unduhan yang rusak, tidak lengkap, atau tidak lebih baru daripada isi bawaan
    /// aplikasi dibuang (mis. setelah aplikasi diperbarui ke versi yang membawa halaman lebih baru).
    func rapikan() {
        guard fm.fileExists(atPath: dir.path) else { return }
        let lengkap = ["newtab.html", "script.js", "style.css"].allSatisfy {
            fm.fileExists(atPath: dir.appendingPathComponent($0).path)
        }
        if !lengkap || !Versi.lebihBaru(versiUnduhan(), versiBawaan()) {
            try? fm.removeItem(at: dir)
        }
    }

    // MARK: Berkas

    /// Isi berkas (path relatif seperti "newtab.html"), atau nil. Jalur yang mencurigakan ditolak.
    func data(_ rel: String) -> Data? {
        if rel.isEmpty || rel.contains("..") || rel.contains("\\") { return nil }
        let akar = dir.standardizedFileURL.path + "/"
        let f = dir.appendingPathComponent(rel).standardizedFileURL
        if f.path.hasPrefix(akar), let d = try? Data(contentsOf: f) { return d }
        if let b = bundleDir {
            let g = b.appendingPathComponent(rel).standardizedFileURL
            if g.path.hasPrefix(b.standardizedFileURL.path + "/"), let d = try? Data(contentsOf: g) { return d }
        }
        return nil
    }

    static func mime(_ p: String) -> String {
        let s = p.lowercased()
        if s.hasSuffix(".html") { return "text/html; charset=utf-8" }
        if s.hasSuffix(".js") { return "text/javascript; charset=utf-8" }
        if s.hasSuffix(".css") { return "text/css; charset=utf-8" }
        if s.hasSuffix(".json") { return "application/json; charset=utf-8" }
        if s.hasSuffix(".png") { return "image/png" }
        if s.hasSuffix(".svg") { return "image/svg+xml" }
        return "application/octet-stream"
    }
}
