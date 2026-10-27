package com.ototech.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

/**
 * Satu layar berisi WebView yang memuat halaman Ototech Tab dari dalam aplikasi (https://appassets.androidplatform.net/web/,
 * dilayani WebStore tanpa jaringan). Link ke situs lain dibuka di peramban bawaan HP. Pembaruan halaman diambil dari GitHub
 * oleh WebUpdater saat aplikasi dibuka (paling sering sekali tiap 10 menit).
 */
public class MainActivity extends Activity {
    static final String HOST = "appassets.androidplatform.net";
    static final String HOME = "https://" + HOST + "/web/newtab.html";
    private static final long JEDA_CEK_MS = 10 * 60 * 1000L;

    private WebView web;
    private WebStore store;
    private WebUpdater updater;
    private String versiApp = "";
    private String uaDasar = "";
    private long cekTerakhir = 0; // SystemClock.elapsedRealtime() pemeriksaan terakhir; 0 = belum pernah

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            versiApp = pi.versionName == null ? "" : pi.versionName;
        } catch (Exception ignored) {
        }

        store = new WebStore(this);
        store.rapikan();
        updater = new WebUpdater(getApplicationContext(), store);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true); // localStorage: menu pribadi tersimpan di sini
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSupportMultipleWindows(false);
        uaDasar = s.getUserAgentString();
        aturUserAgent();

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/web/", store.pathHandler())
                .build();
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (HOST.equals(u.getHost())) return false; // halaman sendiri
                bukaLuar(u); // link menu (target=_blank) dan lainnya: peramban bawaan HP
                return true;
            }
        });

        if (saved == null || web.restoreState(saved) == null) web.loadUrl(HOME);
    }

    /** Menandai versi aplikasi dan versi halaman web di user agent; halaman membacanya untuk label versi dan mode aplikasi. */
    private void aturUserAgent() {
        web.getSettings().setUserAgentString(uaDasar + " OtoApp/" + versiApp + " Web/" + store.versi());
    }

    private void bukaLuar(Uri u) {
        String sk = u.getScheme();
        if (sk == null || !(sk.equals("http") || sk.equals("https") || sk.equals("mailto") || sk.equals("tel"))) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        } catch (ActivityNotFoundException ignored) {
        }
    }

    private void cekPembaruan() {
        long sekarang = SystemClock.elapsedRealtime();
        if (cekTerakhir != 0 && sekarang - cekTerakhir < JEDA_CEK_MS) return;
        cekTerakhir = sekarang;
        updater.periksa(new Handler(Looper.getMainLooper()), new WebUpdater.Hasil() {
            @Override
            public void diperbarui(String versi) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(MainActivity.this, "Diperbarui ke v" + versi, Toast.LENGTH_SHORT).show();
                aturUserAgent();
                web.clearCache(true);
                web.reload();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        cekPembaruan();
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    /** Kembali: halaman menutup menu samping, dialog, atau sisi Atur dulu; lalu riwayat WebView; terakhir keluar. */
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.__otoBack && window.__otoBack()) === true", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String v) {
                if ("true".equals(v)) return;
                if (web.canGoBack()) web.goBack();
                else finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }
}
