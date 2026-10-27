package com.ototech.app;

/** Nomor versi "X.Y.Z" (angka saja, seperti di extension/manifest.json). */
final class Versi {
    private Versi() {}

    static boolean valid(String v) {
        return v != null && v.matches("\\d{1,4}\\.\\d{1,4}\\.\\d{1,4}");
    }

    /** true bila a lebih baru daripada b; versi yang tidak sah dianggap tidak lebih baru. */
    static boolean lebihBaru(String a, String b) {
        if (!valid(a) || !valid(b)) return false;
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            int p = Integer.parseInt(x[i]);
            int q = Integer.parseInt(y[i]);
            if (p != q) return p > q;
        }
        return false;
    }
}
