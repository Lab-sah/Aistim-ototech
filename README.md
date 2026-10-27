# Ototech Tab

Extension Chrome yang mengganti halaman **tab baru** dengan dua kartu: **jam dan kata-kata** di atas, lalu kartu **Menu** berisi pintasan ke situs yang sering dibuka, dengan tile **+** untuk menambah menu. Dibuat mengikuti Aistim Board (Aistim Tab), dengan nama dan ID extension sendiri.

## Fitur

- **Jam dan kata-kata** (kartu atas): jam 24 jam, tanggal dalam bahasa Indonesia, dan satu kata-kata per hari. Semuanya dihitung di browser, tanpa jaringan.
- **Tambah**: ketuk tile **+**, isi **Link** (dan **Nama** bila mau; kosong berarti nama situsnya), lalu Simpan. Link tanpa `https://` dilengkapi otomatis; hanya `http`/`https` yang diterima. Maksimal 24 menu.
- **Menu bersama dari `main.json`**: menu juga diambil dari file [`main.json`](main.json) di repo ini. Menu bersama tampil lebih dulu, lalu menu pribadi. Di sisi belakang kartu (**Atur**) menu bersama diberi lencana `main.json` dan tombol **Segarkan**.

  ```json
  {
    "menu": [
      { "nama": "Erzap", "link": "https://erzap.com" },
      { "nama": "Trial", "link": "trial.erzap.com", "ikon": "🛒" },
      { "link": "contoh.co.id/halaman" }
    ]
  }
  ```

  - `link` wajib; `nama` dan `ikon` opsional. Maksimal 48 menu. Entri yang salah dilewati dan alasannya ditulis di status; file yang bukan JSON sah ditolak seluruhnya dan tab memakai salinan terakhir yang berhasil.
  - **Keamanan**: file ini tidak ditandatangani, jadi siapa pun yang bisa mengubah `main.json` menentukan menu semua pengguna. Isinya divalidasi ketat dan tidak pernah dijalankan sebagai kode. Jaga akses tulis ke repo ini.
- **Ubah dan hapus** ada di sisi belakang kartu: ketuk **Atur**, lalu **Ubah** atau **Hapus** per baris.
- **Ikon otomatis** dari favicon situs lewat Google (hanya domain induk yang dikirim). Bisa diganti emoji atau huruf lewat **Ikon sendiri**.
- **Pembaruan**: extension memeriksa rilis terbaru di GitHub dan menampilkan tombol **Unduh** bila ada versi baru. Tidak memasang apa pun sendiri.
- **Responsif** dan mengikuti `prefers-reduced-motion`.

> **Catatan repo privat**: menu bersama dan pemeriksaan pembaruan membaca `raw.githubusercontent.com` dan `api.github.com` tanpa token. Selama repo ini **privat**, kedua permintaan itu dijawab 404, jadi `main.json` dianggap belum ada dan tidak ada pembaruan terdeteksi. Menu pribadi, jam, dan kata-kata tetap berfungsi penuh. Jadikan repo publik bila fitur itu dipakai.

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

- `extension/`: `manifest.json`, `newtab.html`, `script.js`, `style.css`, `icons/`
- `main.json`: daftar menu bersama
- `tools/id-extension.py`: pemeriksa ID tetap
- `.github/workflows/release.yml`: setiap push ke `main` membuat atau memperbarui release `v<versi>` dengan `Ototech-Tab-<versi>.zip` (naikkan `version` di `manifest.json` untuk rilis baru)
