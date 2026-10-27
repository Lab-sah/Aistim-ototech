#!/usr/bin/env python3
"""Menghitung ID extension dari "key" di extension/manifest.json dan memastikan sama dengan ID tetap.

Chrome menurunkan ID dari public key: 32 hex pertama SHA-256 dari DER, tiap digit hex 0-f dipetakan ke huruf a-p.
Skrip ini gagal (exit 1) bila "key" hilang atau berubah, supaya ID extension tidak ikut berubah tanpa sengaja.
Pakai: python3 tools/id-extension.py [lokasi manifest.json]
"""
import base64
import hashlib
import json
import sys

ID_TETAP = "olmnjboioajdielabpnmpbbndcknjojb"


def id_dari_key(key_b64):
    der = base64.b64decode(key_b64, validate=True)
    h = hashlib.sha256(der).hexdigest()[:32]
    return "".join(chr(ord("a") + int(c, 16)) for c in h)


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "extension/manifest.json"
    with open(path, encoding="utf-8") as f:
        m = json.load(f)
    key = m.get("key")
    if not key:
        sys.exit("GAGAL: manifest.json tidak punya \"key\", ID extension akan berubah-ubah.")
    try:
        eid = id_dari_key(key)
    except Exception as e:
        sys.exit("GAGAL: \"key\" bukan base64 yang sah (%s)." % e)
    print(eid)
    if eid != ID_TETAP:
        sys.exit("GAGAL: ID dari key (%s) tidak sama dengan ID tetap (%s)." % (eid, ID_TETAP))


if __name__ == "__main__":
    main()
