# OtoApp untuk iOS

Aplikasi iOS sederhana: satu `WKWebView` yang memuat halaman Ototech Tab (folder `extension/`) dari dalam aplikasi, dan
memperbarui halaman itu sendiri dari GitHub (cabang `main`) saat aplikasi dibuka. Sama seperti `otoapp/` untuk Android.

## Unduh dan pasang

- IPA terbaru: https://github.com/Lab-sah/Aistim-ototech/raw/otoios/OtoApp.ipa
- Atau lewat Releases (`otoios-v<versi>`).

**IPA ini tidak bertanda tangan Apple.** iOS hanya memasang aplikasi yang bertanda tangan, jadi pasanglah lewat
[AltStore](https://altstore.io) atau [Sideloadly](https://sideloadly.io), yang menandatangani ulang dengan Apple ID Anda.
Dengan Apple ID gratis, tanda tangan berlaku 7 hari (AltStore bisa memperbaruinya otomatis); dengan akun Apple Developer
berbayar berlaku setahun. Mengirim ke App Store/TestFlight butuh akun Developer dan tidak disiapkan di sini.

## Cara kerja

- Halaman disajikan lewat skema `oto://app/...` (`SchemeHandler`), tanpa server dan tanpa jaringan.
- Isi bawaan = salinan folder `extension/` saat build (`sync-web.sh`, tanpa `background.js`).
- Saat aplikasi aktif (paling sering tiap 10 menit), `WebUpdater` membaca versi di `extension/manifest.json` di GitHub.
  Bila lebih baru, semua berkas `extension/` diunduh ke folder sementara, diperiksa, lalu menggantikan salinan lama.
  Gagal di tengah jalan tidak merusak salinan yang sedang dipakai. Halaman dimuat ulang dan muncul keterangan versi.
- Link ke situs lain dibuka di Safari. Geser dari tepi layar untuk kembali (halaman grup).
- Daftar menu bersama (`main.json`) diambil halaman langsung dari GitHub seperti di extension.

## Build

Hanya lewat GitHub Actions (`.github/workflows/otoios.yml`, runner macOS): XcodeGen membuat proyek dari `project.yml`,
`xcodebuild` membangun tanpa tanda tangan, hasilnya dibungkus menjadi IPA. Naikkan `MARKETING_VERSION` di `project.yml`
untuk rilis bernomor baru. Perubahan di `extension/` tidak butuh build baru.

## Catatan keamanan

Isi halaman semua pemakai ditentukan oleh cabang `main` repo ini. Siapa pun yang bisa menulis ke `main` bisa mengubah
tampilan dan perilaku halaman di aplikasi; jaga akses tulis ke repo.
