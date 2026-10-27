import UIKit
import WebKit

/// Satu layar berisi WKWebView yang memuat halaman Ototech Tab dari dalam aplikasi (oto://app/newtab.html, dilayani
/// SchemeHandler tanpa jaringan). Link ke situs lain dibuka di Safari. Pembaruan halaman diambil dari GitHub oleh
/// WebUpdater saat aplikasi dibuka (paling sering sekali tiap 10 menit).
final class ViewController: UIViewController, WKNavigationDelegate, WKUIDelegate {
    private let store = WebStore()
    private var web: WKWebView!
    private var cekTerakhir: Date?
    private let jedaCek: TimeInterval = 10 * 60

    override func viewDidLoad() {
        super.viewDidLoad()
        overrideUserInterfaceStyle = .light // halaman hanya punya tema terang
        view.backgroundColor = .white
        store.rapikan()

        let cfg = WKWebViewConfiguration()
        let versiApp = (Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String) ?? ""
        // Halaman membaca "OtoApp/<versi aplikasi> Web/<versi halaman>" dari user agent untuk mode aplikasi dan label versi
        cfg.applicationNameForUserAgent = "OtoApp/\(versiApp) Web/\(store.versi())"
        cfg.setURLSchemeHandler(SchemeHandler(store: store), forURLScheme: SchemeHandler.skema)

        web = WKWebView(frame: .zero, configuration: cfg)
        web.translatesAutoresizingMaskIntoConstraints = false
        web.navigationDelegate = self
        web.uiDelegate = self
        web.allowsBackForwardNavigationGestures = true // geser dari tepi untuk kembali (mis. dari halaman grup)
        web.isOpaque = false
        web.backgroundColor = UIColor(red: 0.957, green: 0.961, blue: 0.969, alpha: 1)
        web.scrollView.contentInsetAdjustmentBehavior = .never
        view.addSubview(web)
        let g = view.safeAreaLayoutGuide
        NSLayoutConstraint.activate([
            web.topAnchor.constraint(equalTo: g.topAnchor),
            web.bottomAnchor.constraint(equalTo: g.bottomAnchor),
            web.leadingAnchor.constraint(equalTo: g.leadingAnchor),
            web.trailingAnchor.constraint(equalTo: g.trailingAnchor),
        ])

        web.load(URLRequest(url: SchemeHandler.beranda))
        NotificationCenter.default.addObserver(self, selector: #selector(cekPembaruan),
                                               name: UIApplication.didBecomeActiveNotification, object: nil)
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    // MARK: Pembaruan

    @objc private func cekPembaruan() {
        if let t = cekTerakhir, Date().timeIntervalSince(t) < jedaCek { return }
        cekTerakhir = Date()
        let updater = WebUpdater(store: store)
        Task { [weak self] in
            guard let v = await updater.periksa() else { return }
            await MainActor.run {
                guard let self = self else { return }
                self.web.reloadFromOrigin()
                self.toast("Diperbarui ke v\(v)")
            }
        }
    }

    private func toast(_ teks: String) {
        let l = UILabel()
        l.text = "  " + teks + "  "
        l.textColor = .white
        l.backgroundColor = UIColor(white: 0.1, alpha: 0.9)
        l.font = .systemFont(ofSize: 14)
        l.layer.cornerRadius = 16
        l.clipsToBounds = true
        l.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(l)
        NSLayoutConstraint.activate([
            l.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            l.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -24),
            l.heightAnchor.constraint(equalToConstant: 32),
        ])
        UIView.animate(withDuration: 0.3, delay: 2.2, options: [], animations: { l.alpha = 0 }) { _ in l.removeFromSuperview() }
    }

    // MARK: Navigasi

    private func bukaLuar(_ url: URL) {
        guard let s = url.scheme?.lowercased(), ["http", "https", "mailto", "tel"].contains(s) else { return }
        UIApplication.shared.open(url)
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        guard let url = navigationAction.request.url else {
            decisionHandler(.cancel)
            return
        }
        if url.scheme == SchemeHandler.skema || url.scheme == "about" {
            decisionHandler(.allow) // halaman sendiri (termasuk pindah #/grup/...)
        } else {
            bukaLuar(url) // situs lain: Safari
            decisionHandler(.cancel)
        }
    }

    /// Link target=_blank (tile menu): buka di Safari, halaman ini tetap terbuka.
    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                 for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        if let url = navigationAction.request.url { bukaLuar(url) }
        return nil
    }
}
