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
    /* Tombol khusus ADMINISTRATOR (lihat pasangUbahTeks). Ditambahkan setelah dialog
       terpasang karena status admin ditanyakan ke server secara asinkron: dialognya
       tidak boleh menunggu jawaban itu untuk tampil. */
    box.appendChild(tutup); overlay.appendChild(box); document.body.appendChild(overlay);
    try { pasangUbahTeks(box, pesanMudah); } catch (e) {}
}

/* ============================================================================
 * UBAH TEKS / TERJEMAHAN  (khusus administrator)
 * ============================================================================
 *
 * Padanan EditorLabelBahasa di sisi ZKoss. Kalimat yang janggal atau salah terjemah
 * paling mudah dikenali saat kalimat itu sedang dibaca, jadi tombol perbaikannya
 * diletakkan tepat pada dialognya.
 *
 * Status admin, kunci kamus, dan terjemahan yang sudah ada diambil dari endpoint
 * /label-bahasa. Endpoint itu jugalah yang menegakkan hak akses -- tombol ini hanya
 * lapisan tampilan, dan menyembunyikannya BUKAN pengaman.
 */
function pasangUbahTeks(box, teksAsli) {
    if (!teksAsli || !String(teksAsli).trim()) { return; }
    var ctx = (typeof AIS_CONTEXT_PATH === "string") ? AIS_CONTEXT_PATH : "";
    var url = ctx + "/label-bahasa?aksi=muat&teks=" + encodeURIComponent(teksAsli);
    var x = new XMLHttpRequest();
    x.open("GET", url, true);
    x.onreadystatechange = function () {
        if (x.readyState !== 4) { return; }
        var d;
        try { d = JSON.parse(x.responseText); } catch (e) { return; }
        // Bukan admin: tidak ada tombol, tidak ada pesan apa pun.
        if (!d || d.admin !== true) { return; }
        try { bangunPanelUbahTeks(box, d, ctx); } catch (e) {}
    };
    try { x.send(); } catch (e) {}
}

function bangunPanelUbahTeks(box, d, ctx) {
    var bungkus = document.createElement("div");
    bungkus.style.cssText = "margin-top:12px;border-top:1px solid #ddd;padding-top:10px";

    var tombol = document.createElement("button");
    tombol.type = "button";
    tombol.textContent = "Ubah Teks";
    tombol.style.cssText = "padding:8px 14px;cursor:pointer;font-weight:bold";
    bungkus.appendChild(tombol);

    var form = document.createElement("div");
    form.style.cssText = "margin-top:10px";
    // Disetel eksplisit, bukan lewat cssText: pembacaan status buka/tutup di bawah
    // memakai style.display, dan itu lebih jelas daripada mengandalkan penguraian
    // cssText oleh peramban.
    form.style.display = "none";
    bungkus.appendChild(form);

    var ket = document.createElement("div");
    ket.style.cssText = "font-size:11px;color:#666;margin-bottom:8px";
    ket.textContent = "Kunci kamus: " + (d.kunci || "-") +
        " \u2014 perubahan berlaku untuk seluruh aplikasi, bukan hanya halaman ini.";
    form.appendChild(ket);

    var isian = {};
    var bahasa = [["indonesia", "Bahasa Indonesia"], ["english", "English"],
                  ["arab", "Arabic"], ["mandarin", "Mandarin"]];
    for (var i = 0; i < bahasa.length; i++) {
        var l = document.createElement("div");
        l.textContent = bahasa[i][1];
        l.style.cssText = "font-size:12px;font-weight:bold;margin-top:6px";
        form.appendChild(l);
        var t = document.createElement("textarea");
        t.rows = 2;
        t.value = d[bahasa[i][0]] || "";
        t.style.cssText = "width:100%;box-sizing:border-box;font-size:13px;padding:6px;" +
            "border:1px solid #cbd5e1;border-radius:6px";
        form.appendChild(t);
        isian[bahasa[i][0]] = t;
    }

    var status = document.createElement("div");
    status.style.cssText = "font-size:12px;margin-top:8px";
    form.appendChild(status);

    var baris = document.createElement("div");
    baris.style.cssText = "margin-top:10px";
    form.appendChild(baris);

    var btnTerjemah = document.createElement("button");
    btnTerjemah.type = "button";
    btnTerjemah.textContent = "Terjemahkan Otomatis";
    btnTerjemah.style.cssText = "padding:7px 12px;margin-right:8px;cursor:pointer";
    baris.appendChild(btnTerjemah);

    var btnSimpan = document.createElement("button");
    btnSimpan.type = "button";
    btnSimpan.textContent = "Simpan";
    btnSimpan.style.cssText = "padding:7px 14px;cursor:pointer;font-weight:bold";
    baris.appendChild(btnSimpan);

    tombol.onclick = function () {
        var buka = form.style.display === "none";
        form.style.display = buka ? "block" : "none";
        tombol.textContent = buka ? "Tutup Ubah Teks" : "Ubah Teks";
    };

    btnTerjemah.onclick = function () {
        var sumber = isian.indonesia.value;
        if (!sumber || !sumber.trim()) {
            status.style.color = "#b91c1c";
            status.textContent = "Isi dulu kalimat Bahasa Indonesia-nya.";
            return;
        }
        btnTerjemah.disabled = true;
        btnTerjemah.textContent = "Menerjemahkan...";
        var x = new XMLHttpRequest();
        x.open("GET", ctx + "/label-bahasa?aksi=terjemah&teks=" + encodeURIComponent(sumber), true);
        x.onreadystatechange = function () {
            if (x.readyState !== 4) { return; }
            btnTerjemah.disabled = false;
            btnTerjemah.textContent = "Terjemahkan Otomatis";
            var r;
            try { r = JSON.parse(x.responseText); } catch (e) { r = null; }
            if (!r || r.admin !== true) {
                status.style.color = "#b91c1c";
                status.textContent = "Terjemahan otomatis tidak tersedia saat ini.";
                return;
            }
            /* Hanya menimpa bila penerjemah menghasilkan sesuatu: hasil suntingan
               manusia tidak boleh hilang karena server AI sedang tidak siap. */
            if (r.english) { isian.english.value = r.english; }
            if (r.arab) { isian.arab.value = r.arab; }
            if (r.mandarin) { isian.mandarin.value = r.mandarin; }
            status.style.color = "#166534";
            status.textContent = "Terjemahan diisikan. Periksa dan perbaiki bila perlu, lalu Simpan.";
        };
        try { x.send(); } catch (e) {
            btnTerjemah.disabled = false;
            btnTerjemah.textContent = "Terjemahkan Otomatis";
        }
    };

    btnSimpan.onclick = function () {
        btnSimpan.disabled = true;
        btnSimpan.textContent = "Menyimpan...";
        var badan = "aksi=simpan&kunci=" + encodeURIComponent(d.kunci || "") +
            "&indonesia=" + encodeURIComponent(isian.indonesia.value) +
            "&english=" + encodeURIComponent(isian.english.value) +
            "&arab=" + encodeURIComponent(isian.arab.value) +
            "&mandarin=" + encodeURIComponent(isian.mandarin.value);
        var x = new XMLHttpRequest();
        x.open("POST", ctx + "/label-bahasa", true);
        x.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        // Penyimpanan mengubah data, jadi wajib membawa token CSRF dari endpoint muat.
        if (d.csrfHeader && d.csrfToken) { x.setRequestHeader(d.csrfHeader, d.csrfToken); }
        x.onreadystatechange = function () {
            if (x.readyState !== 4) { return; }
            btnSimpan.disabled = false;
            btnSimpan.textContent = "Simpan";
            var r;
            try { r = JSON.parse(x.responseText); } catch (e) { r = null; }
            var ok = r && r.ok === true;
            status.style.color = ok ? "#166534" : "#b91c1c";
            status.textContent = ok
                ? "Teks berhasil diperbarui. Muat ulang halaman untuk melihat hasilnya."
                : ((r && r.pesan) || "Teks gagal disimpan.");
        };
        try { x.send(badan); } catch (e) {
            btnSimpan.disabled = false;
            btnSimpan.textContent = "Simpan";
        }
    };

    box.appendChild(bungkus);
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
