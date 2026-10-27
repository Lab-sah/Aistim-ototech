"use strict";
// Ototech Tab: layanan latar belakang kecil. Saat Chrome dimulai, jendela yang terbuka dijadikan fullscreen (seperti F11),
// kecuali dimatikan lewat kotak "Fullscreen saat Chrome dibuka" di Atur (kunci penyimpanan fsStart, bawaan aktif).
// Tidak ada jaringan dan tidak ada izin tambahan. Tidak berlaku saat extension baru dipasang atau dimuat ulang, hanya saat
// Chrome dimulai.

const KUNCI = "fsStart";
const JENDELA_BARU_MS = 20000; // jendela pertama kadang baru dibuat sesaat setelah peristiwa onStartup

async function aktif() {
  try {
    const r = await chrome.storage.local.get(KUNCI);
    return r[KUNCI] !== false;
  } catch (e) {
    return true;
  }
}

// Hanya jendela peramban biasa (bukan popup atau jendela aplikasi) yang belum fullscreen
async function penuhi(w) {
  try {
    if (w && w.type === "normal" && w.state !== "fullscreen") await chrome.windows.update(w.id, { state: "fullscreen" });
  } catch (e) {}
}

chrome.runtime.onStartup.addListener(() => {
  // Pendengar didaftarkan serentak (sebelum ada await) supaya jendela yang muncul sesaat kemudian tidak terlewat
  const sampai = Date.now() + JENDELA_BARU_MS;
  let nyala = true;
  aktif().then((ya) => { nyala = ya; });
  const dengar = (w) => {
    if (Date.now() > sampai) chrome.windows.onCreated.removeListener(dengar);
    else if (nyala) penuhi(w);
  };
  chrome.windows.onCreated.addListener(dengar);
  aktif().then(async (ya) => {
    if (!ya) return;
    (await chrome.windows.getAll()).forEach(penuhi);
  });
});
