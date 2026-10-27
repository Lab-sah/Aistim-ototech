package com.ototech.app;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONObject;

/**
 * Memperbarui halaman web aplikasi dari GitHub. Membaca versi di extension/manifest.json (cabang main); bila lebih baru
 * daripada yang dipakai sekarang, mengunduh repo sebagai zip, mengambil hanya isi folder extension/, memeriksanya, lalu
 * menukar folder files/web secara menyeluruh. Gagal di tengah jalan tidak merusak salinan yang sedang dipakai.
 *
 * Keamanan: hanya alamat https tetap ke GitHub, batas ukuran dan jumlah berkas, jalur berkas yang mencurigakan ditolak, dan
 * halaman berjalan di WebView tanpa jembatan ke kode Java. Siapa pun yang bisa mengubah cabang main di repo ini menentukan
 * isi halaman semua pengguna aplikasi, jadi jaga akses tulis ke repo.
 */
final class WebUpdater {
    interface Hasil {
        void diperbarui(String versi);
    }

    static final String PEMILIK = "Lab-sah";
    static final String REPO = "Aistim-ototech";
    static final String CABANG = "main";
    static final String URL_MANIFEST =
            "https://raw.githubusercontent.com/" + PEMILIK + "/" + REPO + "/" + CABANG + "/extension/manifest.json";
    static final String URL_ZIP = "https://codeload.github.com/" + PEMILIK + "/" + REPO + "/zip/refs/heads/" + CABANG;

    private static final Pattern DALAM_ZIP = Pattern.compile("^[^/]+/extension/(.+)$");
    private static final int MAKS_BERKAS = 300;
    private static final long MAKS_PER_BERKAS = 5L * 1024 * 1024;
    private static final long MAKS_TOTAL = 20L * 1024 * 1024;

    private final Context ctx;
    private final WebStore store;
    private final AtomicBoolean sibuk = new AtomicBoolean(false);

    WebUpdater(Context context, WebStore store) {
        this.ctx = context.getApplicationContext();
        this.store = store;
    }

    /** Memeriksa di latar belakang; hasil dikirim lewat ui (utas utama) hanya bila ada pembaruan terpasang. Gagal = diam. */
    void periksa(final Handler ui, final Hasil hasil) {
        if (!sibuk.compareAndSet(false, true)) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String v = jalan();
                    if (v != null) {
                        ui.post(new Runnable() {
                            @Override
                            public void run() {
                                hasil.diperbarui(v);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.w("OtoApp", "Pemeriksaan pembaruan gagal: " + e);
                } finally {
                    sibuk.set(false);
                }
            }
        }, "oto-update").start();
    }

    /** Mengembalikan versi baru yang sudah terpasang, atau null bila tidak ada pembaruan. */
    private String jalan() throws IOException {
        String teks = unduhTeks(URL_MANIFEST + "?t=" + System.currentTimeMillis(), 64 * 1024);
        String remote = null;
        try {
            remote = new JSONObject(teks).optString("version", "");
        } catch (Exception ignored) {
        }
        if (!Versi.valid(remote) || !Versi.lebihBaru(remote, store.versi())) return null;

        File files = ctx.getFilesDir();
        File baru = new File(files, "web_new");
        File lama = new File(files, "web_old");
        WebStore.hapus(baru);
        WebStore.hapus(lama);
        try {
            if (!baru.mkdirs()) throw new IOException("tidak bisa membuat folder sementara");
            ekstrak(URL_ZIP, baru);

            // Isi zip yang jadi patokan: lengkap, dan benar-benar lebih baru daripada yang dipakai
            for (String wajib : new String[] {"newtab.html", "script.js", "style.css", "manifest.json"}) {
                if (!new File(baru, wajib).isFile()) throw new IOException("berkas wajib tidak ada: " + wajib);
            }
            String v = WebStore.bacaVersi(new FileInputStream(new File(baru, "manifest.json")));
            if (v == null || !Versi.lebihBaru(v, store.versi())) throw new IOException("versi di zip tidak lebih baru");

            File sekarang = store.dir;
            if (sekarang.exists() && !sekarang.renameTo(lama)) throw new IOException("tidak bisa memindahkan salinan lama");
            if (!baru.renameTo(sekarang)) {
                if (lama.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    lama.renameTo(sekarang);
                }
                throw new IOException("tidak bisa memasang salinan baru");
            }
            WebStore.hapus(lama);
            return v;
        } finally {
            WebStore.hapus(baru); // sudah tidak ada bila berhasil dipindahkan
        }
    }

    // ---------- Jaringan ----------

    private static HttpURLConnection buka(String alamat) throws IOException {
        if (!alamat.startsWith("https://")) throw new IOException("hanya https yang diizinkan");
        HttpURLConnection c = (HttpURLConnection) new URL(alamat).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setUseCaches(false);
        c.setRequestProperty("User-Agent", "OtoApp");
        int kode = c.getResponseCode();
        if (kode != HttpURLConnection.HTTP_OK) {
            c.disconnect();
            throw new IOException("HTTP " + kode);
        }
        return c;
    }

    private static String unduhTeks(String alamat, int maks) throws IOException {
        HttpURLConnection c = buka(alamat);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                if (out.size() + n > maks) throw new IOException("balasan terlalu besar");
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            c.disconnect();
        }
    }

    /** Mengunduh zip repo dan mengekstrak hanya isi folder extension/ ke folder tujuan (aman dari jalur "..", dengan batas ukuran). */
    private static void ekstrak(String alamat, File tujuan) throws IOException {
        HttpURLConnection c = buka(alamat);
        String akar = tujuan.getCanonicalPath() + File.separator;
        int jumlah = 0;
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(c.getInputStream()))) {
            ZipEntry e;
            byte[] buf = new byte[16384];
            while ((e = zip.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                Matcher m = DALAM_ZIP.matcher(e.getName());
                if (!m.matches()) continue;
                String rel = m.group(1);
                if (rel.contains("..") || rel.contains("\\") || rel.startsWith("/")) continue;
                if (++jumlah > MAKS_BERKAS) throw new IOException("terlalu banyak berkas");
                File f = new File(tujuan, rel);
                if (!f.getCanonicalPath().startsWith(akar)) continue;
                File induk = f.getParentFile();
                if (induk != null && !induk.isDirectory() && !induk.mkdirs()) throw new IOException("gagal membuat folder");
                long ukuran = 0;
                try (OutputStream out = new FileOutputStream(f)) {
                    int n;
                    while ((n = zip.read(buf)) != -1) {
                        ukuran += n;
                        total += n;
                        if (ukuran > MAKS_PER_BERKAS || total > MAKS_TOTAL) throw new IOException("ukuran melebihi batas");
                        out.write(buf, 0, n);
                    }
                }
            }
        } finally {
            c.disconnect();
        }
    }
}
