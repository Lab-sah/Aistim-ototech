# Ototech Tab

Extension Chrome yang mengganti halaman **tab baru** dengan tampilan bergaya aplikasi: menu samping (☰), kolom **cari**, kartu **jam dan kata-kata**, lalu kartu **Menu Favorit** berisi grid pintasan berikon bulat dan chip **Terakhir**. Dibuat dari Aistim Board (Aistim Tab), dengan nama dan ID extension sendiri.

## Fitur

- **Jam dan kata-kata** (kartu merah): jam 24 jam, tanggal dalam bahasa Indonesia, dan satu kata-kata per hari. Di atasnya ada sapaan sesuai jam. Semuanya dihitung di browser, tanpa jaringan.
- **Menu Favorit**: grid ikon bulat berwarna dengan nama di bawahnya. Ketuk tile **+** untuk menambah menu: isi **Link** (dan **Nama** bila mau; kosong berarti nama situsnya), lalu Simpan. Link tanpa `https://` dilengkapi otomatis; hanya `http`/`https` yang diterima. Maksimal 24 menu pribadi.
- **Cari**: kolom di atas menyaring semua menu, termasuk submenu grup (nama grup ikut dicocokkan). Enter membuka hasil pertama, panah bawah pindah ke daftar hasil, Escape mengosongkan.
- **Terakhir**: chip di bawah grid (dapat digeser) berisi menu yang terakhir dibuka (maksimal 6), dibuka dari grid, pencarian, atau menu samping.
- **Menu samping** (ikon ☰ di kiri atas): semua menu dalam satu daftar, dengan **Beranda** di atas. Grup dari `main.json` membuka halamannya sendiri; menu pribadi ada di bagian **Menu saya**. Escape atau ketuk bagian gelap untuk menutup.
- **Halaman per grup**: ketuk grup (mis. Logistik) di grid atau menu samping untuk membuka halaman sendiri (`#/grup/<id>`) berisi judul dan grid submenunya, dengan tombol kembali (juga Escape dan tombol Kembali peramban). Beranda hanya memuat menu utama. Pencarian tetap mencari sampai ke submenu.
- **Menu bersama dari `main.json`**: menu juga diambil dari file [`main.json`](main.json) di repo ini. Menu bersama tampil lebih dulu, lalu menu pribadi. Di sisi belakang kartu (**Atur**) menu bersama diberi lencana `main.json` dan ada tombol **Segarkan**.

  ```json
  {
    "menu": [
      { "nama": "Erzap", "link": "https://erzap.com" },
      { "nama": "Trial", "link": "trial.erzap.com", "ikon": "🛒" },
      { "link": "contoh.co.id/halaman", "favorit": false },
      {
        "nama": "Logistik",
        "ikon": "📦",
        "anak": [
          { "nama": "Stok Opname", "link": "https://opname.contoh.com" },
          { "nama": "Pengiriman", "link": "kirim.contoh.com" }
        ]
      }
    ]
  }
  ```

  - **Menu biasa**: `link` wajib; `nama` dan `ikon` opsional. **Grup**: `nama` wajib, `anak` berisi menu biasa (maksimal 24 per grup); grup tampil di grid sebagai tile yang membuka halaman grup itu. `"favorit": false` menyembunyikan menu atau grup dari grid sehingga hanya ada di menu samping. Maksimal 48 menu. Entri yang salah dilewati dan alasannya ditulis di status; file yang bukan JSON sah ditolak seluruhnya dan tab memakai salinan terakhir yang berhasil.
  - **Keamanan**: file ini tidak ditandatangani, jadi siapa pun yang bisa mengubah `main.json` menentukan menu semua pengguna. Isinya divalidasi ketat dan tidak pernah dijalankan sebagai kode. Jaga akses tulis ke repo ini.
- **Ubah dan hapus** ada di sisi belakang kartu: ketuk **Atur**, lalu **Ubah** atau **Hapus** per baris.
- **Ikon otomatis** dari favicon situs lewat Google (hanya domain induk yang dikirim). Bisa diganti emoji atau huruf lewat **Ikon sendiri**.
- **Pembaruan**: extension memeriksa rilis terbaru di GitHub dan menampilkan tombol **Unduh** bila ada versi baru. Tidak memasang apa pun sendiri.
- **Fullscreen saat Chrome dibuka**: layanan latar belakang (`background.js`) menjadikan jendela fullscreen (seperti F11) setiap Chrome dimulai; keluar dengan F11. Tidak berlaku saat extension baru dipasang atau dimuat ulang, hanya pada pembukaan Chrome berikutnya. Matikan lewat kotak **Fullscreen saat Chrome dibuka** di Atur. Tidak butuh izin tambahan.
- **Responsif** dan mengikuti `prefers-reduced-motion`.

> **Catatan repo privat**: menu bersama dan pemeriksaan pembaruan membaca `raw.githubusercontent.com` dan `api.github.com` tanpa token. Selama repo ini **privat**, kedua permintaan itu dijawab 404, jadi `main.json` dianggap belum ada dan tidak ada pembaruan terdeteksi. Menu pribadi, jam, dan kata-kata tetap berfungsi penuh. Jadikan repo publik bila fitur itu dipakai.

## Aplikasi Android (otoapp)

Folder [`otoapp/`](otoapp) berisi kode sumber aplikasi Android (WebView) yang membuka halaman ini dari dalam aplikasi dan memperbarui halamannya dari GitHub. APK dibangun lewat GitHub Actions; lihat [otoapp/README.md](otoapp/README.md).

## ID tetap

ID extension tidak berubah walau dipasang dari folder berbeda, karena `extension/manifest.json` memuat `key` (public key):

```
olmnjboioajdielabpnmpbbndcknjojb
```

`tools/id-extension.py` menghitung ID dari `key` dan gagal bila `key` hilang atau berubah; workflow Release menjalankannya sebelum membuat zip. **Jangan ubah `key`**: ID baru berarti data menu lama tidak terbaca.

## Memasang

1. Unduh `Ototech-Tab-<versi>.zip` dari Releases lalu ekstrak (atau pakai folder `extension/` langsung).
2. Buka `chrome://extensions`, aktifkan **Mode pengembang**, pilih **Load unpacked**, dan tunjuk folder yang berisi `manifest.json`.
3. Buka tab baru.

## Data

Menu pribadi tersimpan di browser (`chrome.storage.local`). Tidak ada akun dan tidak ada server. Lihat [PRIVACY.md](PRIVACY.md).

## Struktur

- `extension/`: `manifest.json`, `newtab.html`, `script.js`, `background.js`, `style.css`, `icons/`
- `main.json`: daftar menu bersama
- `otoapp/`: kode sumber aplikasi Android (WebView)
- `tools/id-extension.py`: pemeriksa ID tetap
- `.github/workflows/release.yml`: setiap push ke `main` membuat atau memperbarui release `v<versi>` dengan `Ototech-Tab-<versi>.zip` (naikkan `version` di `manifest.json` untuk rilis baru)
