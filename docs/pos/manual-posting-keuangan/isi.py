# -*- coding: utf-8 -*-
"""Bagian 3-12 -- disambung dari susun.py lewat exec (lihat rakit.py)."""

# ============================================================ 3. KODE AKUN & BAGAN AKUN
b.judul_bagian('3', 'Kode Akun & Bagan Akun')
b.teks(
    'Menu di grup AKUNTANSI ini mengelola master akun yang dipakai seluruh jurnal di '
    'manual ini. Sumber: kode_akun_screen.dart -- empat tab dibaca lewat cache lokal '
    'terpisah (baris 99-127): akun (aksi kode_akun_daftar), Bank (kode_akun_bank), Grup '
    'Akun (kode_akun_grup), dan Jenis Transaksi (kode_akun_jenis_transaksi).')
b.poin([
    'Kode Akun -- daftar akun perkiraan lengkap (kode, nama, tipe, grup, debet/kredit normal).',
    'Grup Akun -- pengelompokan akun (mis. Aset Lancar, Kewajiban Jangka Pendek).',
    'Jenis Transaksi -- kategori jurnal yang menentukan penomoran otomatis (dipakai medan '
    '"Jenis Transaksi" pada Jurnal Umum, lihat Bagian 4).',
    'Bank -- daftar rekening bank yang dipetakan ke akun Kas/Bank.',
])
b.kotak(
    'Peringatan dari kode sumber',
    'kode_akun_screen.dart baris 297: aksi kode_akun_bersihkan dijalankan dengan '
    'praTinjau:true lebih dulu -- artinya ada fitur "bersihkan akun" yang MENGHAPUS data. '
    'Selalu periksa hasil pratinjaunya sebelum menyetujui pembersihan; akun yang sudah '
    'dipakai jurnal terposting tidak semestinya bisa dibersihkan, tetapi periksa daftarnya '
    'satu per satu bila ragu.',
    warna=(0.702, 0.180, 0.180), bg=(1.0, 0.969, 0.925))

# ============================================================ 4. JURNAL UMUM
b.judul_bagian('4', 'Jurnal Umum')
b.teks(
    'Menu Akuntansi > Jurnal Umum. Sumber: jurnal_umum_screen.dart. Layar ini mencatat '
    'jurnal manual apa pun yang tidak berasal dari transaksi kasir/pembelian otomatis -- '
    'misalnya setoran modal, koreksi, atau jurnal penyesuaian satuan.')
b.sub_judul('4.1  Daftar Jurnal Umum')
b.teks('Kolom daftar (AppTableColumn, baris 318-506): Kode, Tanggal, Keterangan, Debet, '
       'Kredit, Status, Aksi. Filter tersedia: rentang tanggal (Mulai/Sampai), Status '
       '(Semua status / Draf saja / Terposting saja), dan kata kunci (kode/keterangan).')
b.tabel('AKUNTANSI', 'Jurnal Umum',
    ['Kode', 'Tanggal', 'Keterangan', 'Debet', 'Kredit', 'Status', 'Aksi'],
    [(['JU-0001', '01/06/2026', 'Setoran modal awal kas', '5.000.000', '5.000.000',
       'Terposting', 'Lihat'], 'tebal'),
     (['JU-0002', '03/06/2026', 'Koreksi selisih kas kecil', '150.000', '150.000',
       'Draf', 'Ubah'], None),
     (['JU-0003', '05/06/2026', 'Penyesuaian penyusutan bulanan', '2.400.000', '2.400.000',
       'Draf', 'Ubah'], None)],
    catatan='Baris "Draf" bisa diubah/dihapus/diposting; baris "Terposting" hanya bisa dilihat atau dibatalkan.')
b.sub_judul('4.2  Membuat / mengubah jurnal')
b.teks(
    'Tombol "Jurnal Baru" membuka editor (_EditorJurnal, baris 506-905). Medan kepala: '
    'Tanggal, Jenis Transaksi (penomoran) -- opsional, dan Keterangan jurnal -- wajib. '
    'Editor dimulai dengan MINIMAL 2 baris kosong (initState, baris 561-563); baris tidak '
    'bisa dikurangi di bawah 2 (_hapusBaris, baris 587: "if (_baris.length <= 2) return").')
b.formulir('AKUNTANSI', 'Jurnal Umum > Jurnal Baru',
    [('Tanggal', '01/06/2026', True),
     ('Jenis Transaksi (penomoran)', 'Jurnal Umum', False),
     ('Keterangan jurnal', 'Setoran modal awal kas', True)],
    [('Simpan', 'utama'), ('Batal', 'biasa')],
    catatan='Baris debet/kredit (Akun, Debet, Kredit, Keterangan baris) ada di bawah tiga medan ini -- lihat maket berikutnya.')
b.sub_judul('4.3  Baris debet/kredit')
b.teks('Tiap baris: pemilih Akun, medan Debet, medan Kredit, dan Keterangan baris '
       '(labelText, baris 863-904). Aturan pengisiannya sudah dijelaskan di Bagian 2.2 -- '
       'satu baris hanya boleh berisi salah satu dari Debet atau Kredit.')
b.tabel('AKUNTANSI', 'Jurnal Umum > Jurnal Baru',
    ['#', 'Akun', 'Debet', 'Kredit', 'Keterangan baris'],
    [['1', '1101 - Kas', '5.000.000', '', 'Setoran tunai'],
     ['2', '3101 - Modal Pemilik', '', '5.000.000', 'Setoran modal awal'],
     ['+ Tambah Baris', '', '', '', '']],
    bobot=[0.6, 2.2, 1.2, 1.2, 2.0])
b.sub_judul('4.4  Posting, batal posting, dan hapus')
b.teks(
    'Tiga aksi baris (event listener, baris 217-230, tombol baris 474-493):')
b.poin([
    'Posting ke buku besar -- aksi jurnal_umum_posting. Mengubah status Draf menjadi Terposting; jurnal mulai muncul di laporan resmi.',
    'Batalkan posting -- aksi jurnal_umum_batal_posting. Mengembalikan status ke Draf; tersedia hanya untuk jurnal yang sudah terposting.',
    'Hapus -- aksi jurnal_umum_hapus. Kode baris 230: "if (terposting && aksi == \'jurnal_umum_hapus\') return" -- jurnal TERPOSTING tidak bisa dihapus sama sekali, hanya bisa dibatalkan postingnya dulu.',
])
b.kotak('Posting ganda dari daftar', 'Tombol "Posting Semua Draf (N)" pada kepala '
    'halaman (baris 62) memposting SELURUH jurnal berstatus Draf pada rentang tanggal '
    'yang sedang difilter sekaligus -- pastikan filter tanggal dan Status sudah benar '
    'sebelum menekannya, karena tindakan ini menjurnal permanen semua draf yang tampil.')

# ============================================================ 5. POSTING KULAKAN
b.judul_bagian('5', 'Posting Kulakan (Pembelian)')
b.teks(
    'Menu Akuntansi > Posting Kulakan. Sumber: posting_toko_dialog.dart -- kelas '
    'PostingTokoDialog dipakai jenis:"kulakan". Dokblok kelas ini (baris 10-16) menjelaskan '
    'polanya: "draf jurnal ditampilkan PER DOKUMEN lebih dulu lengkap dengan akun '
    'debet/kreditnya, baris yang belum siap tetap terlihat beserta alasannya, dan posting '
    'bisa dilakukan per baris atau sekaligus untuk yang sudah siap."')
b.langkah([
    'Pilih rentang tanggal lewat tombol "Mulai .../Sampai ...".',
    'Tekan "Muat ulang" untuk memuat draf periode itu (aksi posting_kulakan_draft) -- ini HANYA menghitung, belum menjurnal apa pun.',
    'Periksa tabel draf: baris berstatus "Siap diposting" boleh langsung diposting; baris lain menampilkan alasannya (mis. akun belum dipetakan).',
    'Untuk baris yang belum siap: tekan "Sesuaikan Akun Debet"/"Sesuaikan Akun Kredit" -- membuka posting_akun_perbaikan.dart untuk memperbaiki pemetaan akun sumbernya (Cara Pembayaran, Jenis Produk, Supplier, Toko, atau Kelompok Aset).',
    'Setelah setting diperbaiki, tekan "Muat ulang" lagi -- baris yang tadi belum siap akan pindah status bila akunnya sudah lengkap.',
    'Posting satu dokumen lewat tombol "Posting" pada barisnya, atau semua yang siap sekaligus lewat "Posting semua yang siap (N)".',
])
b.tabel('AKUNTANSI', 'Posting Kulakan',
    ['Tanggal', 'Referensi', 'Nilai', 'Debet', 'Kredit', 'Status', 'Aksi'],
    [(['01/06/2026', 'KLK-0021', '1.200.000', '5101 Persediaan Barang Dagang',
       '2101 Utang Dagang', 'Siap diposting', 'Posting'], 'siap'),
     (['02/06/2026', 'KLK-0022', '850.000', '-', '-',
       'Setting akun belum lengkap', 'Sesuaikan Akun'], 'belum')],
    catatan='Kolom Debet/Kredit pada baris "belum siap" kosong karena akun sumbernya belum dipetakan -- itulah yang ditunjuk tombol Sesuaikan Akun.')
b.kotak(
    'Dialog konfirmasi APA ADANYA dari aplikasi (posting_toko_dialog.dart baris 116-129)',
    'Posting satu dokumen: "Dokumen ini akan dijurnal tersendiri. Dokumen lain yang '
    'akunnya belum lengkap tidak ikut terhalang."\n\n'
    'Posting semua yang siap: "Semua dokumen berstatus SIAP pada periode ini akan '
    'dijurnal. Dokumen yang belum siap dilewati."',
    warna=(0.086, 0.588, 0.353), bg=(0.94, 0.98, 0.96))
b.teks('Aksi server: posting_kulakan_terapkan, dikirim dengan posting_ids berisi satu id '
       '(satu dokumen) atau tanpa posting_ids sama sekali (semua yang siap).')

# ============================================================ 6-7 BAYAR HUTANG / TERIMA PIUTANG
b.judul_bagian('6-7', 'Posting Bayar Hutang & Posting Terima Piutang')
b.teks(
    'Dua menu ini (Akuntansi > Posting Bayar Hutang, Akuntansi > Posting Terima Piutang) '
    'memakai kelas PostingTokoDialog yang SAMA dengan Posting Kulakan, hanya jenisnya '
    'berbeda: bayar_hutang dan terima_piutang (laporan_screen.dart baris 258-260, peta '
    'petaPostingToko). Seluruh langkah, tata letak tabel draf, dan dialog konfirmasi di '
    'Bagian 5 berlaku sama persis -- yang berbeda hanya sumber dokumennya (pembayaran '
    'hutang ke supplier vs penerimaan piutang dari pelanggan) dan aksi servernya: '
    'posting_bayar_hutang_draft/_terapkan dan posting_terima_piutang_draft/_terapkan.')
b.kotak('Kenapa tidak diulang maketnya', 'Tata letaknya identik dengan Bagian 5 -- '
    'kolom Tanggal | Referensi | Nilai | Debet | Kredit | Status | Aksi, tombol Sesuaikan '
    'Akun pada baris yang belum siap, dan dialog konfirmasi yang sama. Mengulang maket '
    'yang sama hanya mengganti judul tidak menambah informasi.')

# ============================================================ 8-9 HPP / PENJUALAN
b.judul_bagian('8', 'Posting HPP')
b.teks(
    'Menu Akuntansi > Posting HPP. Sumber: laporan_screen.dart, kelas '
    '_PostingKeuanganDialog dipakai jenis:"hpp". Berbeda dari Posting Kulakan/Bayar '
    'Hutang/Terima Piutang: layar ini memposting HARGA POKOK PENJUALAN periode berjalan '
    'sebagai satu ringkasan, bukan per dokumen transaksi.')
b.langkah([
    'Pilih rentang tanggal (Mulai/Sampai).',
    'Layar otomatis memuat pratinjau saat dibuka (initState, addPostFrameCallback -> _proses(false)) -- aksi laporan_keuangan_pendukung dengan posting:false.',
    'Periksa angka pratinjau HPP periode itu.',
    'Tekan tombol Posting untuk menjurnal permanen -- dialog konfirmasi tampil lebih dulu.',
])
b.kotak('Dialog konfirmasi APA ADANYA (laporan_screen.dart baris 745-751)',
    '"Posting Posting HPP?" -- "Jurnal akan disimpan permanen menggunakan hasil '
    'pratinjau periode ini."', warna=(0.086, 0.588, 0.353), bg=(0.94, 0.98, 0.96))
b.formulir('AKUNTANSI', 'Posting HPP',
    [('Mulai', '01/06/2026', True), ('Sampai', '30/06/2026', True)],
    [('Posting', 'utama')],
    catatan='Setelah pratinjau tampil, ringkasan HPP periode itu muncul di bawah kedua medan tanggal ini.')
b.teks('Satu transaksi juga bisa diposting tersendiri lewat _postingSatu (baris '
       '797-847) -- dialognya menegaskan: "Tindakan ini tidak dapat dibatalkan dari '
       'halaman ini." Aksi server sama: laporan_keuangan_pendukung, dengan '
       'posting_ids berisi satu id.')

b.judul_bagian('9', 'Posting Penjualan')
b.teks(
    'Menu Akuntansi > Posting Penjualan. Kelas dan alur SAMA PERSIS dengan Posting HPP '
    '(Bagian 8) -- hanya jenis:"penjualan", memposting jurnal PENDAPATAN PENJUALAN '
    'periode berjalan. Seluruh langkah, dialog konfirmasi, dan aksi server '
    '(laporan_keuangan_pendukung) identik; yang berbeda hanya angka yang dijurnal.')

# ============================================================ 10. SALDO AWAL, JURNAL PENYESUAIAN, TUTUP BUKU
b.judul_bagian('10', 'Saldo Awal, Jurnal Penyesuaian & Tutup Buku')
b.sub_judul('10.1  Saldo Awal (Neraca Awal)')
b.teks('Menu Akuntansi > Saldo Awal (Neraca Awal). Dipakai SEKALI di awal pemakaian modul '
       'akuntansi untuk mencatat saldo pembukaan tiap akun sebelum transaksi pertama '
       'dijurnal lewat aplikasi ini.')
b.sub_judul('10.2  Jurnal Penyesuaian Berkala')
b.teks('Menu Akuntansi > Jurnal Penyesuaian Berkala. Untuk jurnal penyesuaian rutin '
       '(penyusutan, akrual, dsb.) yang berulang tiap periode.')
b.sub_judul('10.3  Tutup Buku (Laba Ditahan)')
b.teks('Menu Akuntansi > Tutup Buku (Laba Ditahan). Menutup periode akuntansi: saldo akun '
       'Pendapatan dan Beban dipindahkan ke Laba Ditahan, dan Tanggal Closing (Bagian 2.3) '
       'maju ke tanggal penutupan.')
b.kotak('PERINGATAN', 'Tutup Buku bersifat PERIODIK dan berdampak luas -- setelah '
    'sebuah tanggal ditutup, jurnal baru maupun koreksi tidak bisa lagi menyentuh periode '
    'itu (Bagian 2.3). Pastikan seluruh posting periode berjalan (Bagian 4-9) sudah '
    'selesai dan Neraca Saldo (Bagian 11.2) sudah seimbang SEBELUM menutup buku.',
    warna=(0.702, 0.180, 0.180), bg=(1.0, 0.969, 0.925))

# ============================================================ 11. KATALOG LAPORAN
b.judul_bagian('11', 'Katalog Laporan Keuangan')
b.teks(
    'Menu Akuntansi > Katalog Laporan, atau lewat kartu di Beranda. Sumber definisi: '
    'ais/action/master/koperasi/helper/LaporanKatalogData.java (server AIS), tiga '
    'kategori relevan modul ini, dikutip apa adanya beserta keterangan resminya.')
b.sub_judul('11.1  Laporan Keuangan Resmi - Komparatif (Akuntansi)')
b.teks('Empat laporan berformat KOLOM (banyak periode berdampingan):')
b.poin([
    'Neraca & Laba Rugi - 2 Periode -- "Dua kolom periode. Akun Neraca berisi SALDO per akhir periode; akun Laba Rugi berisi MUTASI dalam periode itu."',
    'Neraca & Laba Rugi - 12 Bulan (Kolom) -- "Dua belas kolom bulan pada satu tahun."',
    'Neraca & Laba Rugi - 2 Tahun -- "Perbandingan dua tahun."',
    'Neraca Lajur (Kertas Kerja) -- "Neraca Saldo, Penyesuaian, NSD, lalu dipisah ke kolom Laba Rugi / Neraca menurut Kelompok Laporan."',
])
b.tabel('AKUNTANSI', 'Katalog Laporan > Neraca & Laba Rugi - 12 Bulan',
    ['Kode', 'Nama Akun', 'Jenis', 'Jan', 'Feb', 'Mar', '...', 'Des'],
    [['1101', 'Kas', 'Neraca', '5.000.000', '5.850.000', '5.850.000', '...', '7.200.000'],
     ['4101', 'Penjualan', 'Laba Rugi', '-', '3.400.000', '2.900.000', '...', '4.100.000'],
     ['5101', 'Beban Gaji', 'Laba Rugi', '-', '1.200.000', '1.200.000', '...', '1.350.000']],
    bobot=[0.8, 1.6, 0.9] + [0.8] * 5,
    catatan='Kolom akun Neraca (Kas) SALDO KUMULATIF tiap bulan; kolom akun Laba Rugi (Penjualan, Beban Gaji) MUTASI bulan itu saja.')
b.sub_judul('11.2  Buku Besar Resmi (Akuntansi)')
b.poin([
    'Neraca Saldo / Trial Balance -- "Saldo tiap akun dipisah kolom Debet/Kredit dari jurnal TERPOSTING. Total Debet harus sama dengan total Kredit."',
    'Buku Besar -- "Mutasi tiap akun dari jurnal TERPOSTING, dengan subtotal Debet/Kredit per akun."',
    'Buku Besar per Tanggal -- "Mutasi jurnal TERPOSTING dikelompokkan per tanggal, dengan subtotal Debet/Kredit tiap tanggal."',
    'Jurnal Harian -- "Seluruh baris jurnal TERPOSTING urut tanggal."',
])
b.tabel('AKUNTANSI', 'Katalog Laporan > Buku Besar',
    ['Akun', 'Tanggal', 'No. Jurnal', 'Keterangan', 'Debet', 'Kredit'],
    [(['1101 - Kas', '', '', '', '', ''], 'tebal'),
     ['', '01/06/2026', 'JU-0001', 'Setoran modal awal kas', '5.000.000', ''],
     ['', '15/06/2026', 'KLK-0021', 'Pembayaran kulakan KLK-0021', '', '1.200.000'],
     (['', '', '', 'Subtotal 1101 - Kas', '5.000.000', '1.200.000'], 'tebal')])
b.sub_judul('11.3  Arus Kas & Analisa Keuangan (Akuntansi)')
b.poin([
    'Arus Kas - 12 Bulan (Kolom) -- "Mutasi bersih tiap akun Kas/Bank per bulan (positif = kas masuk bersih) dari jurnal TERPOSTING."',
    'Arus Kas - Harian (Kolom) -- "Mutasi bersih tiap akun Kas/Bank per hari dalam satu bulan."',
    'Rasio, Grafik, Laba Ditahan & Proyeksi Kas -- "Buka Dasbor Akuntansi penuh: rasio keuangan, grafik, laba ditahan, perubahan ekuitas, proyeksi kas."',
])
b.kotak('Satu-satunya laporan yang masih membuka layar web',
    '"Rasio, Grafik, Laba Ditahan & Proyeksi Kas" adalah SATU-SATUNYA entri di seluruh '
    'katalog laporan (177 entri per pemeriksaan terakhir) yang masih membuka layar web '
    'lewat browser sistem, bukan layar natif aplikasi -- dasbornya menyusun isinya secara '
    'dinamis dari basis data per instalasi, sehingga tidak bisa dipastikan bentuknya dari '
    'kode sumber semata (docs/pos/102). Sepuluh laporan lain di kategori ini SUDAH natif.',
    warna=(0.145, 0.388, 0.922))

# ============================================================ 12. LAMPIRAN
b.judul_bagian('12', 'Lampiran')
b.sub_judul('Lampiran A -- Pemetaan Menu ke Aksi API')
b.tabel('AKUNTANSI', 'Lampiran A',
    ['Menu', 'Aksi Draf / Pratinjau', 'Aksi Posting'],
    [['Jurnal Umum', 'jurnal_umum_list', 'jurnal_umum_posting'],
     ['Posting Kulakan', 'posting_kulakan_draft', 'posting_kulakan_terapkan'],
     ['Posting Bayar Hutang', 'posting_bayar_hutang_draft', 'posting_bayar_hutang_terapkan'],
     ['Posting Terima Piutang', 'posting_terima_piutang_draft', 'posting_terima_piutang_terapkan'],
     ['Posting HPP', 'laporan_keuangan_pendukung (posting:false)', 'laporan_keuangan_pendukung (posting:true)'],
     ['Posting Penjualan', 'laporan_keuangan_pendukung (posting:false)', 'laporan_keuangan_pendukung (posting:true)']],
    bobot=[1.6, 2.4, 2.4])
b.sub_judul('Lampiran B -- Pertanyaan Umum')
b.kotak('"Menunya tidak muncul di aplikasi saya"',
    'Menu AKUNTANSI/KEUANGAN bersifat fail-closed (Bagian 1.2) -- kemungkinan besar peran '
    'Anda belum diberi izin menu itu. Hubungi admin untuk memeriksa hak akses peran.')
b.kotak('"Tombol Posting tidak aktif (abu-abu)"',
    'Pada Posting Kulakan/Bayar Hutang/Terima Piutang, tombol "Posting semua yang siap" '
    'nonaktif bila tidak ada dokumen berstatus "Siap diposting" pada periode yang '
    'dipilih -- periksa filter tanggal, atau perbaiki setting akun lewat tombol '
    '"Sesuaikan Akun" pada baris yang belum siap.')
b.kotak('"Jurnal tidak bisa disimpan, tombol Simpan abu-abu"',
    'Periksa pesan yang tampil di bawah formulir -- editor Jurnal Umum memeriksa aturan '
    'keseimbangan (Bagian 2.2) SEBELUM mengizinkan simpan, dan menampilkan alasannya '
    'persis seperti yang akan ditolak server.')
b.sub_judul('Lampiran C -- Tentang Ilustrasi di Manual Ini')
b.teks(
    'Sesi penyusunan manual ini mencoba tiga teknik berbeda untuk mengambil tangkapan '
    'layar interaktif (mengklik lalu menangkap tiap layar) dan ketiganya gagal dengan '
    'bukti yang jelas -- bukan dugaan:')
b.poin([
    'PostMessage ke jendela Flutter -- diuji pada tombol biasa, tidak diterima sama sekali.',
    'SendInput/mouse_event + AttachThreadInput + SetForegroundWindow (termasuk trik "tekan Alt") -- GetForegroundWindow() selalu mengembalikan 0: sesi ini tidak punya jendela foreground sama sekali.',
    'UI Automation (pohon aksesibilitas Windows) -- pohon semantik Flutter tidak pernah aktif, hanya 1 elemen (jendela itu sendiri) yang selalu terlihat.',
])
b.teks(
    'Build web juga dicoba sebagai jalan pintas (supaya bisa dikendalikan lewat Browser '
    'pane yang benar-benar bisa mengklik) tetapi gagal KOMPILASI: paket core_hw (akses '
    'laci kas/printer) mengimpor dart:ffi tanpa syarat, dan pustaka itu tidak ada di web.')
b.teks(
    'Karena itu manual ini memakai ILUSTRASI TATA LETAK untuk sebagian besar layar -- '
    'setiap label, nama kolom, dan pesan yang tertulis di dalamnya dikutip apa adanya '
    'dari kode sumber (sitasi berkas:baris ada di teks pendamping tiap maket), warna '
    'diambil sampel dari tangkapan layar NYATA yang berhasil diperoleh untuk beranda '
    'aplikasi (Lampiran D), dan setiap maket ditandai jelas "ilustrasi tata letak" di '
    'sudut kanan-bawah. Rincian lengkap dan cara memperbaikinya untuk sesi berikutnya: '
    'docs/pos/110-uat-otomasi-tidak-bisa-jalan-di-lingkungan-ini.md.')
