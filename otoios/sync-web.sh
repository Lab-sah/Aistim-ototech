#!/bin/sh
# Menyalin halaman web dari ../extension ke OtoApp/web (isi bawaan aplikasi). Jalankan dari folder otoios sebelum xcodegen.
set -e
cd "$(dirname "$0")"
rm -rf OtoApp/web
mkdir -p OtoApp/web
cp -R ../extension/. OtoApp/web/
rm -f OtoApp/web/background.js # hanya dipakai extension Chrome
ls OtoApp/web
