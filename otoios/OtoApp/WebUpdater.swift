import Foundation

/// Memperbarui halaman web aplikasi dari GitHub. Membaca versi di extension/manifest.json (cabang main); bila lebih baru
/// daripada yang dipakai, mengambil daftar berkas folder extension/ (GitHub API) dan mengunduh tiap berkasnya ke folder
/// sementara, memeriksanya, lalu menukar folder web secara menyeluruh. Gagal di tengah jalan tidak merusak salinan yang
/// sedang dipakai.
///
/// Keamanan: hanya https ke dua host GitHub tetap, batas ukuran dan jumlah berkas, jalur berkas yang mencurigakan ditolak,
/// dan halaman berjalan di WKWebView tanpa jembatan ke kode Swift. Siapa pun yang bisa mengubah cabang main di repo ini
/// menentukan isi halaman semua pengguna aplikasi, jadi jaga akses tulis ke repo.
struct WebUpdater {
    static let pemilik = "Lab-sah"
    static let repo = "Aistim-ototech"
    static let cabang = "main"
    private static let hostDiizinkan: Set<String> = ["raw.githubusercontent.com", "api.github.com"]
    private static let maksBerkas = 300
    private static let maksPerBerkas = 5 * 1024 * 1024
    private static let maksTotal = 20 * 1024 * 1024

    enum Gagal: Error { case pesan(String) }

    let store: WebStore

    /// Mengembalikan versi baru yang sudah terpasang, atau nil bila tidak ada pembaruan atau gagal (gagal = diam).
    func periksa() async -> String? {
        do {
            return try await jalan()
        } catch {
            NSLog("OtoApp: pemeriksaan pembaruan gagal: \(error)")
            return nil
        }
    }

    private func rawURL(_ path: String) throws -> URL {
        let enc = path.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? path
        let t = Int(Date().timeIntervalSince1970)
        guard let u = URL(string: "https://raw.githubusercontent.com/\(Self.pemilik)/\(Self.repo)/\(Self.cabang)/\(enc)?t=\(t)") else {
            throw Gagal.pesan("alamat tidak sah")
        }
        return u
    }

    private func jalan() async throws -> String? {
        let m = try await ambil(try rawURL("extension/manifest.json"), maks: 64 * 1024)
        guard let remote = WebStore.bacaVersi(m), Versi.lebihBaru(remote, store.versi()) else { return nil }

        // Daftar berkas di folder extension/ (satu panggilan API)
        guard let apiURL = URL(string: "https://api.github.com/repos/\(Self.pemilik)/\(Self.repo)/git/trees/\(Self.cabang)?recursive=1") else {
            throw Gagal.pesan("alamat API tidak sah")
        }
        let pohon = try await ambil(apiURL, maks: 4 * 1024 * 1024)
        guard let j = try JSONSerialization.jsonObject(with: pohon) as? [String: Any],
              let entri = j["tree"] as? [[String: Any]] else { throw Gagal.pesan("daftar berkas tidak terbaca") }
        var berkas: [String] = []
        for e in entri {
            guard let p = e["path"] as? String, (e["type"] as? String) == "blob", p.hasPrefix("extension/") else { continue }
            let rel = String(p.dropFirst("extension/".count))
            if rel.isEmpty || rel.contains("..") || rel.contains("\\") || rel.hasPrefix("/") { continue }
            if rel == "background.js" { continue } // hanya dipakai extension Chrome
            berkas.append(rel)
        }
        if berkas.isEmpty || berkas.count > Self.maksBerkas { throw Gagal.pesan("jumlah berkas tidak masuk akal") }

        let fm = FileManager.default
        let baru = store.dir.deletingLastPathComponent().appendingPathComponent("web_new", isDirectory: true)
        try? fm.removeItem(at: baru)
        try fm.createDirectory(at: baru, withIntermediateDirectories: true)
        defer { try? fm.removeItem(at: baru) } // sudah tidak ada bila berhasil dipindahkan

        var total = 0
        for rel in berkas {
            let d = try await ambil(try rawURL("extension/" + rel), maks: Self.maksPerBerkas)
            total += d.count
            if total > Self.maksTotal { throw Gagal.pesan("ukuran melebihi batas") }
            let tujuan = baru.appendingPathComponent(rel)
            try fm.createDirectory(at: tujuan.deletingLastPathComponent(), withIntermediateDirectories: true)
            try d.write(to: tujuan)
        }

        // Lengkap, dan benar-benar lebih baru daripada yang dipakai
        for wajib in ["newtab.html", "script.js", "style.css", "manifest.json"] {
            if !fm.fileExists(atPath: baru.appendingPathComponent(wajib).path) { throw Gagal.pesan("berkas wajib tidak ada: \(wajib)") }
        }
        let v = WebStore.bacaVersi(try Data(contentsOf: baru.appendingPathComponent("manifest.json")))
        guard let versiBaru = v, Versi.lebihBaru(versiBaru, store.versi()) else { throw Gagal.pesan("versi unduhan tidak lebih baru") }

        if fm.fileExists(atPath: store.dir.path) {
            _ = try fm.replaceItemAt(store.dir, withItemAt: baru)
        } else {
            try fm.moveItem(at: baru, to: store.dir)
        }
        return versiBaru
    }

    private func ambil(_ url: URL, maks: Int) async throws -> Data {
        guard url.scheme == "https", let host = url.host, Self.hostDiizinkan.contains(host) else {
            throw Gagal.pesan("alamat tidak diizinkan")
        }
        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 30)
        req.setValue("OtoApp", forHTTPHeaderField: "User-Agent")
        let (d, r) = try await URLSession.shared.data(for: req)
        guard let h = r as? HTTPURLResponse, h.statusCode == 200 else { throw Gagal.pesan("balasan bukan 200") }
        if d.count > maks { throw Gagal.pesan("balasan terlalu besar") }
        return d
    }
}
