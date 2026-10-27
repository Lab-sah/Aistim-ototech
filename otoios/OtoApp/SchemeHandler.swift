import WebKit

/// Melayani alamat oto://app/<berkas> dari WebStore tanpa jaringan. Dengan skema sendiri, halaman punya asal tetap
/// (localStorage menu pribadi tersimpan di sini) dan tetap bisa memanggil https ke GitHub (CORS GitHub mengizinkan semua asal).
final class SchemeHandler: NSObject, WKURLSchemeHandler {
    static let skema = "oto"
    static let beranda = URL(string: "oto://app/newtab.html")!

    private let store: WebStore

    init(store: WebStore) {
        self.store = store
    }

    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        guard let url = urlSchemeTask.request.url else {
            urlSchemeTask.didFailWithError(URLError(.badURL))
            return
        }
        var path = url.path
        if path.isEmpty || path == "/" { path = "/newtab.html" }
        let rel = String(path.dropFirst())

        let data = store.data(rel)
        let kode = data == nil ? 404 : 200
        let isi = data ?? Data()
        let tipe = data == nil ? "text/plain; charset=utf-8" : WebStore.mime(rel)
        guard let r = HTTPURLResponse(url: url, statusCode: kode, httpVersion: "HTTP/1.1",
                                      headerFields: ["Content-Type": tipe, "Content-Length": String(isi.count), "Cache-Control": "no-cache"]) else {
            urlSchemeTask.didFailWithError(URLError(.unknown))
            return
        }
        urlSchemeTask.didReceive(r)
        urlSchemeTask.didReceive(isi)
        urlSchemeTask.didFinish()
    }

    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {}
}
