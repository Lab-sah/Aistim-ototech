package com.ototech.app;

import android.content.Context;
import android.webkit.WebResourceResponse;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Menyajikan isi halaman web aplikasi. Urutannya: salinan unduhan di penyimpanan aplikasi (files/web, hasil pembaruan dari
 * GitHub) bila ada dan lebih baru, jika tidak isi bawaan APK (assets/web, disalin dari folder extension saat build).
 */
final class WebStore {
    private final Context ctx;
    /** Folder salinan unduhan: files/web */
    final File dir;

    WebStore(Context context) {
        ctx = context.getApplicationContext();
        dir = new File(ctx.getFilesDir(), "web");
    }

    // ---------- Versi ----------

    /** Membaca "version" dari manifest.json; null bila tidak sah. Aliran ditutup. */
    static String bacaVersi(InputStream in) {
        try {
            JSONObject j = new JSONObject(new String(bacaSemua(in), StandardCharsets.UTF_8));
            String v = j.optString("version", "");
            return Versi.valid(v) ? v : null;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    static byte[] bacaSemua(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }

    String versiBawaan() {
        try {
            String v = bacaVersi(ctx.getAssets().open("web/manifest.json"));
            return v != null ? v : "0.0.0";
        } catch (IOException e) {
            return "0.0.0";
        }
    }

    /** Versi salinan unduhan, atau null bila tidak ada. */
    String versiUnduhan() {
        File m = new File(dir, "manifest.json");
        if (!m.isFile()) return null;
        try {
            return bacaVersi(new FileInputStream(m));
        } catch (IOException e) {
            return null;
        }
    }

    /** Versi yang sedang dipakai. */
    String versi() {
        String u = versiUnduhan();
        return u != null ? u : versiBawaan();
    }

    /**
     * Dipanggil saat aplikasi mulai: salinan unduhan yang rusak, tidak lengkap, atau tidak lebih baru daripada isi bawaan
     * APK dibuang (mis. setelah APK diperbarui ke versi yang membawa halaman lebih baru).
     */
    void rapikan() {
        if (!dir.exists()) return;
        String u = versiUnduhan();
        boolean lengkap = new File(dir, "newtab.html").isFile()
                && new File(dir, "script.js").isFile()
                && new File(dir, "style.css").isFile();
        if (!lengkap || u == null || !Versi.lebihBaru(u, versiBawaan())) hapus(dir);
    }

    static void hapus(File f) {
        File[] anak = f.listFiles();
        if (anak != null) {
            for (File a : anak) hapus(a);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    // ---------- Penyaji untuk WebViewAssetLoader (alamat /web/...) ----------

    WebViewAssetLoader.PathHandler pathHandler() {
        return this::buka;
    }

    private WebResourceResponse buka(String path) {
        if (path == null || path.isEmpty() || path.contains("..") || path.contains("\\")) return null;
        try {
            InputStream in;
            File f = new File(dir, path);
            if (f.isFile() && f.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator)) {
                in = new FileInputStream(f);
            } else {
                in = ctx.getAssets().open("web/" + path);
            }
            String mime = mime(path);
            WebResourceResponse r = new WebResourceResponse(mime, mime.startsWith("text/") || mime.endsWith("json") ? "utf-8" : null, in);
            Map<String, String> h = new HashMap<>();
            h.put("Cache-Control", "no-cache"); // pembaruan dari GitHub langsung terlihat
            r.setResponseHeaders(h);
            return r;
        } catch (IOException e) {
            return null;
        }
    }

    private static String mime(String p) {
        String s = p.toLowerCase(Locale.ROOT);
        if (s.endsWith(".html")) return "text/html";
        if (s.endsWith(".js")) return "text/javascript";
        if (s.endsWith(".css")) return "text/css";
        if (s.endsWith(".json")) return "application/json";
        if (s.endsWith(".png")) return "image/png";
        if (s.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
