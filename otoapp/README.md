# OtoApp

Aplikasi Android (WebView) yang membuka halaman **Ototech Tab** dari dalam aplikasi, tanpa Chrome. Halaman web-nya adalah folder [`../extension`](../extension) di repo ini, jadi extension dan aplikasi memakai satu sumber.

## Cara kerja

- **Isi bawaan**: saat build, folder `extension/` disalin ke `assets/web` (tugas Gradle `syncWeb`), jadi aplikasi langsung jalan, juga saat offline.
- **Pembaruan dari GitHub**: saat aplikasi dibuka (paling sering sekali tiap 10 menit), `WebUpdater` membaca versi di `extension/manifest.json` (cabang `main`). Bila lebih baru daripada yang dipakai, aplikasi mengunduh repo sebagai zip, mengambil hanya isi `extension/`, memeriksanya, lalu menukar salinannya di penyimpanan aplikasi dan memuat ulang halaman. Gagal di tengah jalan tidak merusak salinan yang sedang dipakai. Menu pribadi tidak hilang (tersimpan di `localStorage`).
- **Naikkan `version` di `extension/manifest.json`** agar pembaruan sampai ke aplikasi (sama seperti untuk extension).
- **Link keluar**: tile menu dan link ke situs lain dibuka di peramban bawaan HP; halaman Ototech tetap terbuka di aplikasi.
- **Tombol Kembali**: menutup menu samping, dialog, atau sisi Atur dulu; lalu mundur satu halaman (mis. dari halaman grup); terakhir keluar dari aplikasi.
- Mode aplikasi dikenali halaman dari user agent (`OtoApp/<versi aplikasi> Web/<versi halaman>`): blok pembaruan zip dan pilihan fullscreen Chrome disembunyikan.

## Membangun APK

Lewat GitHub Actions saja: workflow [`otoapp.yml`](../.github/workflows/otoapp.yml) berjalan otomatis setiap push ke `main` yang menyentuh `otoapp/`, atau manual di **Actions > OtoApp APK > Run workflow**. Hasilnya `OtoApp-<versi>.apk` di Releases pada tag `otoapp-v<versi>` (prarilis, bukan "latest", supaya pemeriksaan pembaruan extension tidak terganggu).

Pasang: unduh APK di HP, izinkan pemasangan dari sumber itu, lalu buka. Untuk menaikkan nomor aplikasi, ubah `versionName` di `app/build.gradle` (`versionCode` naik sendiri dari nomor run).

Membangun di komputer sendiri (opsional): JDK 17, Android SDK 34, Gradle 8.7+, lalu `gradle -p otoapp assembleRelease`.

## Struktur

- `app/src/main/java/com/ototech/app/`: `MainActivity` (WebView), `WebStore` (menyajikan salinan unduhan atau isi bawaan), `WebUpdater` (pembaruan dari GitHub), `Versi`
- `app/build.gradle`: pengaturan aplikasi dan tugas `syncWeb`
- `ototech.keystore`: kunci tanda tangan tetap
- `../.github/workflows/otoapp.yml`: pembangun APK

## Keamanan

- Halaman berjalan di WebView **tanpa jembatan ke kode Java** dan tanpa akses berkas; hanya alamat `appassets.androidplatform.net` yang dimuat di dalam aplikasi.
- Pembaruan hanya dari alamat `https` tetap (`raw.githubusercontent.com` dan `codeload.github.com`, repo `Lab-sah/Aistim-ototech`), dengan batas ukuran dan jumlah berkas serta penolakan jalur `..`. Karena isinya tidak ditandatangani, siapa pun yang bisa mengubah cabang `main` menentukan isi halaman semua pengguna aplikasi: jaga akses tulis ke repo.
- **Kunci tanda tangan ada di repo publik** (`ototech.keystore`, sandi `ototech`). Ini membuat APK dari build berbeda bisa saling menimpa saat dipasang, cukup untuk pemakaian pribadi. Jangan dipakai untuk distribusi luas: siapa pun bisa membuat APK yang dianggap sebagai "pembaruan" oleh perangkat yang sudah memasangnya. Untuk distribusi luas, buat kunci sendiri dan simpan di GitHub Secrets.
