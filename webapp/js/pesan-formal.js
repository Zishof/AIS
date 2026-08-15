/**
 * Menampilkan pesan formal (sukses/gagal) yang konsisten di seluruh halaman JSP aplikasi ini.
 * Reusable -- pola yang sama dipakai di semua halaman JSP/JS, sejalan dengan ais.common.PesanFormalHelper
 * (versi Java untuk layar ZK).
 *
 * Pola pesan gagal WAJIB mengandung 4 bagian:
 *   1. Sapaan formal & permintaan maaf.
 *   2. Penjelasan penyebab (sedetail mungkin).
 *   3. Langkah tindak lanjut yang bisa dicoba pengguna.
 *   4. Eskalasi ke Administrator Sistem / Pengembang Sistem + WAJIB lampirkan screenshot.
 *
 * @param {string} aktivitas     deskripsi singkat aktivitas yang gagal, mis. "penyimpanan data transaksi POS"
 * @param {string} penyebab      penjelasan rinci penyebab kegagalan (boleh "" bila tidak diketahui)
 * @param {string[]} [langkahSolusi] daftar langkah tindak lanjut (array string, boleh kosong/undefined)
 */
function tampilkanPesanGagalFormal(aktivitas, penyebab, langkahSolusi, detailTeknis) {
    var data = (penyebab && typeof penyebab === "object") ? penyebab : {};
    var pesanMudah = data.message || data.pesan ||
        "Proses belum dapat diselesaikan. Data belum berubah; silakan periksa kembali lalu coba sekali lagi.";
    var judul = data.judul || "Ada kendala";
    var solusi = data.solusi || langkahSolusi || [
        "Muat ulang halaman dan periksa kembali data yang diisi.",
        "Coba sekali lagi setelah beberapa saat.",
        "Jika kendala berulang, buka Detail Error lalu salin informasinya untuk admin/developer."
    ];
    var referensi = data.referensi || data.traceId || ("WEB-" + Date.now().toString(36).toUpperCase());
    var teknis = data.teknis || data.technical || detailTeknis ||
        (typeof penyebab === "string" ? penyebab : JSON.stringify(data));
    var teknisLower = String(teknis || "").toLowerCase();
    if (!data.message && teknisLower.indexOf("rincian pesanan") >= 0 && teknisLower.indexOf("keranjang") >= 0) {
        judul = "Pesanan perlu dimuat ulang";
        pesanMudah = "Isi pesanan di server berbeda dengan keranjang yang sedang tampil. Pembayaran dihentikan agar barang atau jumlah yang salah tidak tersimpan.";
        solusi = ["Tutup jendela pembayaran, lalu muat ulang daftar pesanan.", "Buka kembali pesanan dan periksa nama produk serta jumlahnya.", "Jika masih berbeda, salin Detail Error dan hubungi supervisor/admin."];
    }
    var salinan = "Kode referensi: " + referensi + "\nAktivitas: " +
        (aktivitas || "proses aplikasi") + "\n" + teknis;

    var lama = document.getElementById("ais-dialog-error-global");
    if (lama && lama.parentNode) lama.parentNode.removeChild(lama);
    var overlay = document.createElement("div");
    overlay.id = "ais-dialog-error-global";
    overlay.style.cssText = "position:fixed;inset:0;z-index:2147483647;background:rgba(0,0,0,.48);display:flex;align-items:center;justify-content:center;padding:20px";
    var box = document.createElement("div");
    box.style.cssText = "width:min(620px,100%);max-height:90vh;overflow:auto;background:#fff;border-radius:14px;padding:22px;font:14px/1.5 Arial,sans-serif;color:#263238;box-shadow:0 20px 60px rgba(0,0,0,.3)";
    var h = document.createElement("h3"); h.textContent = judul; h.style.margin = "0 0 8px"; box.appendChild(h);
    var p = document.createElement("p"); p.textContent = pesanMudah; box.appendChild(p);
    var label = document.createElement("strong"); label.textContent = "Yang dapat Anda lakukan:"; box.appendChild(label);
    var ul = document.createElement("ol");
    for (var i = 0; i < solusi.length; i++) { var li = document.createElement("li"); li.textContent = solusi[i]; ul.appendChild(li); }
    box.appendChild(ul);
    var details = document.createElement("details"); details.style.cssText = "margin-top:14px;border-top:1px solid #ddd;padding-top:10px";
    var summary = document.createElement("summary"); summary.textContent = "Detail Error"; summary.style.cssText = "cursor:pointer;font-weight:bold"; details.appendChild(summary);
    var pre = document.createElement("pre"); pre.textContent = salinan; pre.style.cssText = "white-space:pre-wrap;word-break:break-word;background:#f5f5f5;padding:10px;border-radius:8px;max-height:260px;overflow:auto"; details.appendChild(pre);
    var copy = document.createElement("button"); copy.type = "button"; copy.textContent = "Copy Error"; copy.style.cssText = "padding:8px 14px;margin-right:8px;cursor:pointer";
    copy.onclick = function () {
        var selesai = function () { copy.textContent = "Error sudah disalin"; };
        if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(salinan).then(selesai);
        else { var ta = document.createElement("textarea"); ta.value = salinan; document.body.appendChild(ta); ta.select(); document.execCommand("copy"); document.body.removeChild(ta); selesai(); }
    };
    details.appendChild(copy); box.appendChild(details);
    var tutup = document.createElement("button"); tutup.type = "button"; tutup.textContent = "Tutup"; tutup.style.cssText = "float:right;padding:8px 18px;margin-top:16px;cursor:pointer";
    tutup.onclick = function () { if (overlay.parentNode) overlay.parentNode.removeChild(overlay); };
    box.appendChild(tutup); overlay.appendChild(box); document.body.appendChild(overlay);
}

/**
 * Menampilkan pesan sukses formal yang konsisten.
 * @param {string} aktivitas  aktivitas yang berhasil diselesaikan
 * @param {string} [detail]   rincian tambahan (opsional)
 */
function tampilkanPesanSuksesFormal(aktivitas, detail) {
    var pesan = "Yang terhormat Bapak/Ibu Pengguna,\n\n";
    pesan += "Dengan ini kami sampaikan bahwa proses " + (aktivitas || "yang Bapak/Ibu jalankan") +
        " telah BERHASIL diselesaikan dengan baik.";
    if (detail && detail.length > 0) {
        pesan += "\n\nRincian: " + detail;
    }
    pesan += "\n\nTerima kasih atas kesabaran Bapak/Ibu.";
    alert(pesan);
}
