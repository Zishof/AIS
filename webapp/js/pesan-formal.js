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
    /* Blok Detail disusun dengan bentuk yang SAMA seperti sisi ZK
       (MyMessageboxConfig.susunDetail): keterangan konteks lebih dulu, lalu informasi
       teknis. Sebelumnya hanya kode referensi + aktivitas + teks teknis mentah,
       sehingga laporan pengguna sering tiba tanpa waktu kejadian maupun halaman
       tempat kesalahan terjadi -- dua hal pertama yang ditanyakan pengembang. */
    var barisDetail = [];
    barisDetail.push("Waktu         : " + new Date().toLocaleString());
    barisDetail.push("Kode Referensi: " + referensi);
    barisDetail.push("Aktivitas     : " + (aktivitas || "proses aplikasi"));
    barisDetail.push("Judul         : " + judul);
    try { barisDetail.push("Halaman       : " + window.location.href); } catch (e) {}
    try { barisDetail.push("Peramban      : " + navigator.userAgent); } catch (e) {}
    barisDetail.push("");
    barisDetail.push("Pesan Singkat yang Ditampilkan:");
    barisDetail.push(pesanMudah);
    barisDetail.push("");
    if (solusi && solusi.length) {
        barisDetail.push("Langkah yang Disarankan:");
        for (var s2 = 0; s2 < solusi.length; s2++) {
            barisDetail.push("  " + (s2 + 1) + ". " + solusi[s2]);
        }
        barisDetail.push("");
    }
    barisDetail.push("Informasi Teknis:");
    barisDetail.push(teknis && String(teknis).length ? teknis
        : "Tidak ada informasi teknis yang dikirim ke komponen alert ini.");
    var salinan = barisDetail.join("\n");

    var lama = document.getElementById("ais-dialog-error-global");
    if (lama && lama.parentNode) lama.parentNode.removeChild(lama);
    var overlay = document.createElement("div");
    overlay.id = "ais-dialog-error-global";
    overlay.style.cssText = "position:fixed;inset:0;z-index:2147483647;background:rgba(0,0,0,.48);display:flex;align-items:center;justify-content:center;padding:20px";
    var box = document.createElement("div");
    box.style.cssText = "width:min(620px,100%);max-height:90vh;overflow:auto;background:#fff;border-radius:14px;padding:22px;font:14px/1.5 Arial,sans-serif;color:#263238;box-shadow:0 20px 60px rgba(0,0,0,.3)";
    var h = document.createElement("h3"); h.textContent = judul; h.style.margin = "0 0 8px"; box.appendChild(h);
    var p = document.createElement("p"); p.textContent = pesanMudah;
    p.style.cssText = "white-space:pre-wrap;word-break:break-word;margin:0 0 10px";
    box.appendChild(p);
    if (solusi && solusi.length) {
        var label = document.createElement("strong"); label.textContent = "Yang dapat Anda lakukan:"; box.appendChild(label);
        var ul = document.createElement("ol");
        for (var i = 0; i < solusi.length; i++) { var li = document.createElement("li"); li.textContent = solusi[i]; ul.appendChild(li); }
        box.appendChild(ul);
    }
    var details = document.createElement("details"); details.style.cssText = "margin-top:14px;border-top:1px solid #ddd;padding-top:10px";
    var summary = document.createElement("summary"); summary.textContent = "Detail (informasi teknis)"; summary.style.cssText = "cursor:pointer;font-weight:bold"; details.appendChild(summary);
    var pre = document.createElement("pre"); pre.textContent = salinan; pre.style.cssText = "white-space:pre-wrap;word-break:break-word;background:#f5f5f5;padding:10px;border-radius:8px;max-height:260px;overflow:auto"; details.appendChild(pre);
    var copy = document.createElement("button"); copy.type = "button"; copy.textContent = "Copy Detail"; copy.style.cssText = "padding:8px 14px;margin-right:8px;cursor:pointer";
    copy.onclick = function () {
        var selesai = function () { copy.textContent = "Tersalin"; };
        if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(salinan).then(selesai);
        else { var ta = document.createElement("textarea"); ta.value = salinan; document.body.appendChild(ta); ta.select(); document.execCommand("copy"); document.body.removeChild(ta); selesai(); }
    };
    details.appendChild(copy); box.appendChild(details);
    var tutup = document.createElement("button"); tutup.type = "button"; tutup.textContent = "Tutup"; tutup.style.cssText = "float:right;padding:8px 18px;margin-top:16px;cursor:pointer";
    tutup.onclick = function () {
        if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
        /* alert() bawaan MEMBLOKIR; dialog ini tidak. Pemanggil yang dulu mengandalkan
           jeda itu (mis. memuat ulang halaman setelah pengguna menekan OK) memindahkan
           lanjutannya ke callback ini, sehingga urutannya tetap sama seperti dulu. */
        if (typeof data.onTutup === "function") { try { data.onTutup(); } catch (e) {} }
    };
    box.appendChild(tutup); overlay.appendChild(box); document.body.appendChild(overlay);
}

/**
 * Pengganti DROP-IN untuk `alert()` bawaan peramban.
 *
 * Bentuk pemanggilannya sengaja dibuat identik -- `alertFormal(pesan)` -- supaya
 * pengalihan dari `alert(pesan)` menjadi perubahan satu kata saja, tanpa menyentuh
 * logika halaman. Yang didapat sebagai gantinya: pesan tampil di dalam dialog aplikasi
 * (bukan kotak abu-abu peramban yang bisa diblokir/di-"jangan tampilkan lagi"),
 * lengkap dengan tombol Detail berisi waktu, halaman, peramban, dan informasi teknis
 * yang dapat disalin pengguna untuk dikirim ke pengembang.
 *
 * Berbeda dari `alert()` bawaan, fungsi ini TIDAK memblokir eksekusi. Kode yang
 * mengandalkan jeda `alert()` (mis. menunggu pengguna menekan OK sebelum berpindah
 * halaman) perlu dipindahkan ke dalam callback -- lihat catatan pada migrasi.
 *
 * @param {string} pesan  isi pesan; pergantian baris dipertahankan
 * @param {Object} [opsi] penyesuaian opsional: {judul, solusi, teknis, aktivitas}
 */
function alertFormal(pesan, opsi) {
    opsi = opsi || {};
    var isi = (pesan === null || pesan === undefined) ? "" : String(pesan);
    tampilkanPesanGagalFormal(opsi.aktivitas || "proses pada halaman ini", {
        judul: opsi.judul || "Informasi",
        message: isi,
        // Sengaja kosong: pemanggil alert() biasa tidak punya daftar langkah, dan
        // saran generik yang tidak nyambung justru membingungkan pengguna.
        solusi: opsi.solusi || [],
        teknis: opsi.teknis || ("Pesan dari halaman: " + isi),
        onTutup: opsi.onTutup
    });
}

/**
 * Menampilkan pesan sukses formal yang konsisten.
 *
 * Sebelumnya memakai `alert()` bawaan dan diawali baris sapaan, sehingga pengguna
 * membaca "Yang terhormat Bapak/Ibu Pengguna," lebih dulu sebelum tahu apa yang
 * berhasil. Kini memakai dialog yang sama dengan pesan lain, dan kalimat intinya
 * langsung di muka.
 *
 * @param {string} aktivitas  aktivitas yang berhasil diselesaikan
 * @param {string} [detail]   rincian tambahan (opsional)
 */
function tampilkanPesanSuksesFormal(aktivitas, detail) {
    var pesan = "Proses " + (aktivitas || "yang Anda jalankan") +
        " telah BERHASIL diselesaikan.";
    if (detail && detail.length > 0) {
        pesan += "\n\nRincian: " + detail;
    }
    alertFormal(pesan, {
        judul: "Berhasil",
        aktivitas: aktivitas || "proses pada halaman ini",
        teknis: "Proses berhasil; tidak ada informasi galat."
    });
}


/*
 * ============================================================================
 * PENINGKATAN alert() BAWAAN PERAMBAN
 * ============================================================================
 *
 * Mengapa menimpa window.alert, bukan mengganti nama 285 pemanggilan menjadi
 * alertFormal():
 *
 *   Dari 146 berkas JSP/JS yang memakai alert(), hanya SATU yang memuat
 *   pesan-formal.js secara langsung; sisanya bergantung pada halaman induk yang
 *   tidak dapat dipastikan secara statis. Mengganti nama pemanggilan pada berkas
 *   yang ternyata tidak memuat skrip ini akan melempar "alertFormal is not
 *   defined" -- dan pesannya hilang SAMA SEKALI, jauh lebih buruk daripada kotak
 *   abu-abu bawaan peramban.
 *
 *   Dengan menimpa di sini: halaman yang memuat pesan-formal.js otomatis
 *   mendapat dialog aplikasi lengkap dengan tombol Detail, sementara halaman yang
 *   tidak memuatnya tetap berjalan persis seperti sekarang. Tidak ada satu pun
 *   call site yang perlu disentuh, sehingga tidak ada risiko salah sunting.
 *
 * Perbedaan perilaku yang HARUS diketahui:
 *
 *   alert() bawaan MEMBLOKIR eksekusi sampai pengguna menekan OK; dialog berbasis
 *   DOM tidak bisa. Kode berpola `alert(pesan); location.reload();` akan memuat
 *   ulang halaman sebelum pesannya sempat dibaca. Pemanggil seperti itu harus
 *   memindahkan lanjutannya ke callback:
 *
 *       alertFormal(pesan, { onTutup: function () { location.reload(); } });
 *
 * Jalan keluar: setel window.AIS_ALERT_NATIVE = true pada halaman yang memang
 * memerlukan sifat memblokir, dan alert() bawaan dipakai kembali di halaman itu.
 * alert() aslinya juga tetap tersedia sebagai window.alertAsli.
 */
(function () {
    if (typeof window === "undefined" || typeof window.alert !== "function") { return; }
    if (window.alertAsli) { return; }  // sudah dipasang; jangan bertumpuk
    window.alertAsli = window.alert;
    window.alert = function (pesan) {
        if (window.AIS_ALERT_NATIVE) { return window.alertAsli(pesan); }
        try {
            alertFormal(pesan);
        } catch (e) {
            // Gagal-aman: pesan TIDAK BOLEH hilang hanya karena dialognya bermasalah.
            try { window.alertAsli(pesan); } catch (e2) {}
        }
    };
})();
