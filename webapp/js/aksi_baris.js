/*
 * aksi_baris.js -- tombol "..." berisi daftar aksi baris untuk tabel CRUD.
 *
 * MENGAPA ADA.
 * Baris tabel CRUD dahulu memuat tiga sampai tujuh tombol ikon berjajar. Tiga
 * masalahnya nyata: kolom Aksi memakan lebar yang seharusnya milik data, ikon
 * tanpa label hanya dapat ditebak artinya (atribut title tidak muncul di layar
 * sentuh), dan target sentuhnya terlalu rapat sehingga salah tekan mudah
 * terjadi -- berbahaya ketika salah satunya Hapus.
 *
 * MENGAPA TIDAK MEMAKAI DROPDOWN BOOTSTRAP.
 * Halaman modul dipakai bersama oleh beberapa kerangka (index.jsp, ebisnis.jsp,
 * erp.jsp, dashboard.jsp) yang TIDAK memuat berkas pendukung yang sama --
 * ebisnis.jsp dan erp.jsp tidak menyertakan include/foot.jsp. Menggantungkan
 * diri pada Bootstrap berarti menu ini hidup di sebagian kerangka dan mati di
 * sebagian lain, tanpa gejala yang kelihatan sampai ada yang mengklik. Berkas
 * ini karena itu berdiri sendiri: tanpa Bootstrap, tanpa jQuery, tanpa Popper.
 *
 * Menunya juga DIPINDAHKAN ke <body> saat dibuka. Tabel CRUD di sini banyak
 * yang berada di dalam kotak bergulir (table-responsive), dan menu yang tetap
 * tinggal di dalam <td> akan terpotong oleh overflow kotak itu.
 *
 * PEMAKAIAN, di dalam perangkaian string baris tabel:
 *
 *   '<td class="text-center">' + aksiBarisMenu([
 *       { ikon: 'fa-edit',       label: 'Ubah data',  onclick: 'edit(' + row.id + ')' },
 *       { ikon: 'fa-trash-alt',  label: 'Hapus data', onclick: 'hapus(' + row.id + ')',
 *         merusak: true },
 *       { ikon: 'fa-check',      label: 'Setujui',    onclick: 'setuju(' + row.id + ')',
 *         nonaktif: row.status !== 'DRAFT' }
 *   ]) + '</td>'
 *
 * Aksi yang sedang tidak berlaku diberi `nonaktif: true`, BUKAN dihilangkan
 * dari daftar. Menu yang isinya berubah-ubah mengikuti status membuat pengguna
 * tidak dapat menghafal letak, dan aksi yang hilang menimbulkan kesan
 * kemampuannya memang tidak ada.
 */
(function () {
    'use strict';

    if (window.aksiBarisMenu) { return; } // sudah dimuat kerangka lain

    var GAYA = '' +
        '.aksi-baris-tombol{border:1px solid rgba(0,0,0,.15);background:#fff;border-radius:.375rem;' +
        'width:32px;height:28px;line-height:1;padding:0;cursor:pointer;color:#495057;font-weight:700;}' +
        '.aksi-baris-tombol:hover{background:#f1f3f5;}' +
        '.aksi-baris-panel{position:fixed;z-index:2000;min-width:210px;background:#fff;' +
        'border:1px solid rgba(0,0,0,.15);border-radius:.5rem;box-shadow:0 .5rem 1.25rem rgba(0,0,0,.18);' +
        'padding:.25rem 0;font-size:.875rem;}' +
        '.aksi-baris-item{display:flex;align-items:center;gap:.6rem;width:100%;border:0;background:none;' +
        'text-align:left;padding:.45rem .9rem;color:#212529;cursor:pointer;white-space:nowrap;}' +
        '.aksi-baris-item:hover{background:#f1f3f5;}' +
        '.aksi-baris-item[disabled]{color:#adb5bd;cursor:default;background:none;}' +
        '.aksi-baris-item.merusak{color:#dc3545;}' +
        '.aksi-baris-item.merusak[disabled]{color:#e9a2a8;}' +
        '.aksi-baris-item i{width:1rem;text-align:center;}' +
        '.aksi-baris-pisah{height:1px;background:rgba(0,0,0,.12);margin:.25rem 0;}';

    var gayaTerpasang = false;
    function pasangGaya() {
        if (gayaTerpasang) { return; }
        var el = document.createElement('style');
        el.type = 'text/css';
        el.appendChild(document.createTextNode(GAYA));
        document.getElementsByTagName('head')[0].appendChild(el);
        gayaTerpasang = true;
    }

    function amanAtribut(teks) {
        return String(teks === null || teks === undefined ? '' : teks)
            .replace(/&/g, '&amp;').replace(/"/g, '&quot;')
            .replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function amanTeks(teks) {
        return String(teks === null || teks === undefined ? '' : teks)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    /* Sebagian halaman memakai gaya "regular" (far) atau "brands" (fab), bukan
     * "solid" (fas). Nama yang sudah memuat awalannya dipakai apa adanya; yang
     * hanya berupa "fa-xxx" diberi awalan fas supaya pemanggil lama tetap ringkas. */
    function kelasIkon(ikon) {
        var n = String(ikon || 'fa-circle').trim();
        return n.indexOf(' ') >= 0 ? n : ('fas ' + n);
    }

    /* Menghasilkan potongan HTML, bukan simpul DOM, karena halaman JSP di sini
     * merangkai barisnya sebagai string lalu memasangnya lewat innerHTML. */
    window.aksiBarisMenu = function (daftar, opsi) {
        pasangGaya();
        opsi = opsi || {};
        daftar = daftar || [];

        var biasa = [], merusak = [], adaAktif = false, i, a;
        for (i = 0; i < daftar.length; i++) {
            a = daftar[i];
            if (!a) { continue; }
            if (!a.nonaktif) { adaAktif = true; }
            (a.merusak ? merusak : biasa).push(a);
        }
        /* Tidak satu pun aksi dapat dipakai -- tombolnya tidak ditampilkan sama
         * sekali daripada membuka menu yang seluruh isinya mati. */
        if (!adaAktif) { return ''; }

        function butir(a) {
            return '<button type="button" class="aksi-baris-item' +
                (a.merusak ? ' merusak' : '') + '"' +
                (a.nonaktif ? ' disabled' : ' onclick="aksiBarisPilih(this);' +
                    amanAtribut(a.onclick) + '"') +
                '><i class="' + amanAtribut(kelasIkon(a.ikon)) + '"></i>' +
                '<span>' + amanTeks(a.label) + '</span></button>';
        }

        var isi = '', j;
        for (j = 0; j < biasa.length; j++) { isi += butir(biasa[j]); }
        if (biasa.length && merusak.length) { isi += '<div class="aksi-baris-pisah"></div>'; }
        for (j = 0; j < merusak.length; j++) { isi += butir(merusak[j]); }

        return '<span class="aksi-baris">' +
            '<button type="button" class="aksi-baris-tombol" title="' +
            amanAtribut(opsi.judul || 'Aksi lain') + '" onclick="aksiBarisBuka(this)">' +
            '&#8943;</button>' +
            '<span class="aksi-baris-panel" style="display:none">' + isi + '</span>' +
            '</span>';
    };

    var panelTerbuka = null;   // panel yang sedang tampil di <body>
    var indukPanel = null;     // tempat asalnya, supaya dapat dikembalikan

    function tutup() {
        if (!panelTerbuka) { return; }
        panelTerbuka.style.display = 'none';
        if (indukPanel) { indukPanel.appendChild(panelTerbuka); }
        panelTerbuka = null;
        indukPanel = null;
    }
    window.aksiBarisTutup = tutup;

    window.aksiBarisBuka = function (tombol) {
        var panel = tombol.parentNode.querySelector('.aksi-baris-panel');
        var samaDenganYangTerbuka = (panel === panelTerbuka);
        tutup();
        if (samaDenganYangTerbuka) { return; } // klik kedua = menutup

        indukPanel = panel.parentNode;
        document.body.appendChild(panel);
        panel.style.display = 'block';
        panel.style.visibility = 'hidden';

        var r = tombol.getBoundingClientRect();
        var lebar = panel.offsetWidth, tinggi = panel.offsetHeight;
        /* Menu dibuka ke kiri-bawah tombol, tetapi dibalik ke atas atau digeser
         * ke dalam bila layarnya tidak cukup -- kolom Aksi hampir selalu berada
         * di tepi kanan tabel, dan baris terakhir berada di dasar layar. */
        var kiri = Math.min(r.right - lebar, window.innerWidth - lebar - 8);
        var atas = (r.bottom + tinggi > window.innerHeight - 8 && r.top - tinggi > 8)
            ? r.top - tinggi : r.bottom + 2;
        panel.style.left = Math.max(8, kiri) + 'px';
        panel.style.top = Math.max(8, atas) + 'px';
        panel.style.visibility = 'visible';
        panelTerbuka = panel;
    };

    /* Dipanggil lebih dulu oleh setiap butir, supaya menunya menutup sebelum
     * aksi aslinya berjalan -- aksi itu kerap membuka modal atau memuat ulang
     * tabel, yang akan meninggalkan panel menggantung di <body>. */
    window.aksiBarisPilih = function () { tutup(); };

    document.addEventListener('click', function (e) {
        if (!panelTerbuka) { return; }
        if (e.target.closest && (e.target.closest('.aksi-baris-panel') ||
            e.target.closest('.aksi-baris-tombol'))) { return; }
        tutup();
    }, true);

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' || e.keyCode === 27) { tutup(); }
    });

    /* Panel memakai position:fixed terhadap tombol yang ikut bergulir, jadi
     * posisinya tidak lagi benar begitu halaman digulir. Menutupnya lebih jujur
     * daripada membiarkannya melayang di tempat yang salah. */
    window.addEventListener('scroll', tutup, true);
    window.addEventListener('resize', tutup);
})();
