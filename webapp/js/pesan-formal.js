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
function tampilkanPesanGagalFormal(aktivitas, penyebab, langkahSolusi, informasiTeknis) {
    var referensi = Date.now().toString(36).toUpperCase();
    var teknis = informasiTeknis || penyebab || "Tidak ada rincian tambahan.";
    var penyebabRamah = penyebab || "Sistem belum dapat menyelesaikan permintaan. Kendala ini mungkin bersifat sementara.";
    if (/rincian teknis|exception|stack|sql|http\s*\d/i.test(penyebabRamah)) {
        penyebabRamah = "Sistem menerima kendala saat memproses permintaan. Rincian untuk administrator tersedia pada Informasi Teknis.";
    }
    var solusi = langkahSolusi && langkahSolusi.length ? langkahSolusi : [
        "Periksa kembali data yang diisi serta koneksi internet Bapak/Ibu.",
        "Muat ulang halaman, kemudian ulangi proses beberapa saat lagi.",
        "Jika tetap gagal, hubungi administrator dan berikan kode referensi serta Informasi Teknis."
    ];

    var lama = document.getElementById("aisPesanFormalOverlay");
    if (lama) lama.parentNode.removeChild(lama);
    var overlay = document.createElement("div");
    overlay.id = "aisPesanFormalOverlay";
    overlay.style.cssText = "position:fixed;inset:0;z-index:2147483647;background:rgba(15,23,42,.58);display:flex;align-items:center;justify-content:center;padding:20px";
    var itemSolusi = "";
    for (var i = 0; i < solusi.length; i++) itemSolusi += "<li style='margin:5px 0'>" + aisPesanEscape(solusi[i]) + "</li>";
    overlay.innerHTML = "<div role='alertdialog' aria-modal='true' style='background:#fff;color:#1f2937;border-radius:14px;max-width:650px;width:100%;max-height:88vh;overflow:auto;padding:24px;box-shadow:0 24px 70px rgba(0,0,0,.3)'>" +
        "<div style='font-size:21px;font-weight:700;color:#b91c1c;margin-bottom:10px'>Proses belum berhasil</div>" +
        "<p>Mohon maaf, proses <b>" + aisPesanEscape(aktivitas || "yang Bapak/Ibu jalankan") + "</b> belum dapat diselesaikan.</p>" +
        "<p>" + aisPesanEscape(penyebabRamah) + "</p>" +
        "<div style='font-weight:600;margin-top:14px'>Yang dapat Bapak/Ibu lakukan:</div><ol>" + itemSolusi + "</ol>" +
        "<details style='margin-top:14px;border-top:1px solid #ddd;padding-top:10px'><summary style='cursor:pointer;color:#166534;font-weight:600'>Informasi Teknis</summary>" +
        "<pre style='white-space:pre-wrap;background:#f3f4f6;padding:10px;border-radius:7px;font-size:12px'>Kode referensi: " + aisPesanEscape(referensi) + "\n" + aisPesanEscape(teknis) + "</pre></details>" +
        "<p style='font-size:13px;color:#6b7280'>Jika langkah di atas belum berhasil, hubungi administrator dan sertakan kode referensi, Informasi Teknis, serta tangkapan layar.</p>" +
        "<div style='text-align:right'><button type='button' style='border:0;border-radius:8px;background:#166534;color:#fff;padding:9px 22px;cursor:pointer'>Tutup</button></div></div>";
    overlay.querySelector("button").onclick = function () { overlay.parentNode.removeChild(overlay); };
    document.body.appendChild(overlay);
    aisCatatErrorClient(aktivitas, penyebabRamah, teknis, referensi);
}

function aisPesanEscape(value) {
    var d = document.createElement("div");
    d.textContent = value == null ? "" : String(value);
    return d.innerHTML;
}

function aisCatatErrorClient(aktivitas, pesan, detail, referensi) {
    try {
        var bagian = location.pathname.split("/");
        var konteks = bagian.length > 1 && bagian[1] ? "/" + bagian[1] : "";
        fetch(konteks + "/Api_eBisnis", {
            method: "POST", headers: {"Content-Type": "application/json"},
            body: JSON.stringify({action:"client_error_log", sumber:"JSP:" + (aktivitas || "unknown"),
                pesan:pesan || "", detail:detail || "", referensi:referensi || ""})
        }).catch(function () {});
    } catch (ignored) {}
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
