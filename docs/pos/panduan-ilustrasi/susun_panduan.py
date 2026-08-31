# -*- coding: utf-8 -*-
"""Isi panduan: tujuh kebutuhan keuangan, tiap laporan digambar maketnya."""
from ilustrasi import Buku

b = Buku()

b.sampul(
    'Panduan Laporan Keuangan',
    'TokoQu Al-Bahjah An Nahl - POS Desktop varian An-Nahl',
    [
        'Server: https://an-nahl.santri.info/nahl/',
        'Aplikasi: TokoQu Al-Bahjah An Nahl, versi 1.34.16 (build 178).',
        'Panduan ini memakai ILUSTRASI tata letak layar, bukan tangkapan layar. Nama menu, '
        'nama laporan, dan NAMA KOLOM pada tiap tabel diambil apa adanya dari aplikasi, '
        'sehingga yang Anda lihat di layar akan sama; hanya angkanya yang contoh.',
        'Berlaku pula untuk POS Android, versi JSP, dan aplikasi web ZK - keempatnya membaca '
        'satu katalog laporan yang sama di server.',
        'Disusun 1 September 2026.',
    ])

# ----------------------------------------------------------------- pengantar
b.judul_bagian('', 'Dua pintu menu, satu cara pakai')
b.teks('Hampir semua laporan keuangan berada di balik satu layar katalog. Yang membedakan '
       'hanya pintu masuknya.', ukuran=10, warna=REDUP if False else (0.357, 0.420, 0.373))
b.teks('PINTU A - menu Akuntansi > Laporan-Laporan Keuangan. Berisi sembilan kategori '
       'akuntansi: Keuangan, Buku Besar, Kas & Bank, Piutang, Pengadaan, Pajak PPN, '
       'Anggaran RAB, Gaji, dan Rekonsiliasi Bank. Inilah pintu untuk neraca, laba rugi, '
       'arus kas, buku besar, kas dan bank, piutang, serta utang.')
b.teks('PINTU B - menu Transaksi & Laporan > Laporan-Laporan. Katalog penuh sekitar 180 '
       'laporan dalam 32 kategori: seluruh isi Pintu A ditambah Penjualan, Pembelian, '
       'Persediaan, Gudang, Produksi, Stock Opname, dan Margin.')
b.langkah([
    'Buka menunya, lalu pilih Kategori pada kotak pilihan - atau ketik nama laporan di kotak cari.',
    'Klik judul laporannya.',
    'Isi Tgl Mulai dan Tgl Sampai.',
    'Bila muncul pilihan Unit / Satuan Kerja, pilih satu unit (SMP, FF Mart, Katering, Laundry) '
    'atau "Semua Unit (Konsolidasi)" untuk gabungan.',
    'Tekan Tampilkan. Hasilnya bisa dicetak PDF atau diunduh Excel dari layar yang sama.',
])
b.kotak('Bentuk umum layar laporan',
        'Ilustrasi di halaman-halaman berikut memakai kerangka yang sama: bilah menu di kiri, '
        'jalur menu dan nama laporan di atas, panel filter (tanggal, unit, tombol Tampilkan), '
        'lalu tabel hasilnya. Yang berbeda antar laporan hanya kategori, nama, dan kolomnya.')

# ------------------------------------------------------------------- butir 1
b.judul_bagian('1.', 'Pembukuan kas masuk dan keluar')
b.teks('Pintu A > kategori Kas & Bank (Akuntansi). Dua laporan intinya: Buku Kas Umum untuk '
       'rincian tiap mutasi, dan Posisi Dana untuk ikhtisar saldo tiap rekening.')
b.maket('Kas & Bank (Akuntansi)', 'Buku Kas Umum (Mutasi Kas & Bank)',
        ['Akun Kas/Bank', 'Tanggal', 'No. Jurnal', 'Uraian', 'Akun Lawan',
         'Penerimaan', 'Pengeluaran', 'Saldo Berjalan'],
        [['1-1110 Kas Tunai', '03/06/26', 'JU-0612', 'Setoran SPP', '4-1100 Pendapatan',
          '12.500.000', '', '18.700.000'],
         ['1-1110 Kas Tunai', '07/06/26', 'JU-0631', 'Bayar listrik', '5-2200 Beban Listrik',
          '', '1.850.000', '16.850.000'],
         ['1-1210 Bank BSI', '11/06/26', 'JU-0644', 'Transfer daftar ulang', '4-1200 Pendapatan DU',
          '43.200.000', '', '60.050.000']])
b.teks('Kolom Saldo Berjalan sudah memperhitungkan saldo sebelum Tgl Mulai, sehingga baris '
       'terakhir tiap rekening sama dengan saldo akhir pada Posisi Dana. Inilah padanan lembar '
       '"Mutasi Kas & Bank" pada laporan yayasan.', ukuran=9.2, warna=(0.357, 0.420, 0.373))
b.maket('Kas & Bank (Akuntansi)', 'Posisi Dana (Saldo Kas & Bank per Rekening)',
        ['Kode', 'Akun Kas/Bank', 'Saldo Awal', 'Mutasi Debet', 'Mutasi Kredit', 'Saldo Akhir'],
        [['1-1110', 'Kas Tunai', '6.200.000', '55.700.000', '45.050.000', '16.850.000'],
         ['1-1210', 'Bank BSI - SPP', '23.928.889', '198.646.266', '218.648.373', '3.926.782'],
         ['1-1220', 'Bank BSI - Operasional', '5.086.866', '132.977.254', '122.406.472', '15.657.648'],
         ['~TOTAL', '~', '~35.215.755', '~387.323.520', '~386.104.845', '~36.434.430']])
b.kotak('Di mana mencatatnya',
        'Pencatatan hariannya ada di menu KEUANGAN: Kas Besar, Kas Kecil, Uang Muka, '
        'Pertanggungjawaban, Penggantian Kas Kecil, Dana Talangan, Reimbursement Pegawai, dan '
        'Proses Transfer. Yang di bagian ini adalah laporannya.')

# ------------------------------------------------------------------- butir 2
b.judul_bagian('2.', 'Pencatatan penjualan dan pembelian')
b.teks('Pintu B (katalog penuh) > kategori Penjualan dan Pembelian. Kategori ini sengaja tidak '
       'ikut di Pintu A yang khusus akuntansi.')
b.maket('Penjualan', 'Daftar Faktur Penjualan',
        ['Tanggal', 'No. Faktur', 'Toko', 'Pelanggan', 'Total', 'Dibayar', 'Kembali'],
        [['02/06/26', 'INV-26060021', 'FF Mart', 'Umum', '187.500', '200.000', '12.500'],
         ['02/06/26', 'INV-26060022', 'FF Mart', 'Ahmad Fauzi', '95.000', '95.000', '0'],
         ['03/06/26', 'INV-26060035', 'Kantin', 'Siti Aminah', '43.000', '50.000', '7.000']],
        filter_unit=False, sorot='TRANSAKSI')
b.maket('Pembelian', 'Faktur Pembelian',
        ['No. Faktur', 'Tanggal', 'Pemasok', 'Jml Item', 'Total Faktur'],
        [['FB-2606-011', '04/06/26', 'CV Sumber Rejeki', '24', '12.480.000'],
         ['FB-2606-012', '09/06/26', 'PT Nabati Distribusi', '11', '6.775.000'],
         ['FB-2606-018', '18/06/26', 'Toko Grosir Amanah', '37', '9.120.500']],
        filter_unit=False, sorot='TRANSAKSI')
b.teks('Kategori lain yang serumpun: Accurate POS (penjualan per kasir per hari) dan Pengadaan '
       '(PR, PO, BAST, tagihan vendor). Untuk pemantauan harian tersedia pula menu Laporan '
       'Transaksi dan Riwayat Penjualan.', ukuran=9.2, warna=(0.357, 0.420, 0.373))

# ------------------------------------------------------------------- butir 3
b.judul_bagian('3.', 'Pencatatan utang piutang')
b.teks('Piutang ada di Pintu A > kategori Piutang; utang ada di Pintu A > kategori Pengadaan '
       '(Vendor / PO / BAST). Keduanya menyediakan laporan umur (aging).')
b.maket('Piutang', 'Umur Piutang (Aging)',
        ['Pelanggan', '0-30 hr', '31-60 hr', '61-90 hr', '>90 hr', 'Total Piutang'],
        [['Koperasi Guru', '4.500.000', '1.200.000', '0', '0', '5.700.000'],
         ['Kantin Putra', '2.100.000', '850.000', '400.000', '0', '3.350.000'],
         ['Warung Bu Hasan', '0', '0', '1.750.000', '2.300.000', '4.050.000'],
         ['~TOTAL', '~6.600.000', '~2.050.000', '~2.150.000', '~2.300.000', '~13.100.000']])
b.maket('Pengadaan (Vendor / PO / BAST)', 'Umur Hutang Supplier (Aging)',
        ['Pemasok', 'No. Faktur', 'Tgl Faktur', 'Jatuh Tempo', 'Sisa Hutang', 'Umur'],
        [['CV Sumber Rejeki', 'FB-2606-011', '04/06/26', '04/07/26', '12.480.000', '12 hr'],
         ['PT Nabati Distribusi', 'FB-2605-093', '22/05/26', '21/06/26', '3.150.000', '40 hr'],
         ['Toko Grosir Amanah', 'FB-2604-077', '15/04/26', '15/05/26', '1.980.000', '77 hr']])
b.kotak('Layar kerjanya',
        'Selain laporan, ada layar khusus untuk mengelolanya: Piutang Customer (AR) dengan 6 tab '
        '(termasuk Aging Customer dan Aging per Sales) dan Hutang Supplier (AP) dengan 5 tab '
        '(termasuk Aging). Keduanya di bilah menu, pada varian Inventory & Sales.')

# ------------------------------------------------------------------- butir 4
b.judul_bagian('4.', 'Pelaporan cash flow tiap bulan')
b.teks('Pintu A > kategori Keuangan. Bentuknya mengikuti lembar "Laporan Penerimaan dan '
       'Pengeluaran Cash" pada laporan yayasan.')
b.maket('Keuangan', 'Arus Kas per Aktivitas (Operasional/Investasi/Pendanaan)',
        ['Keterangan', 'Penerimaan', 'Pengeluaran', 'Bersih'],
        [['~SALDO AWAL KAS & BANK', '~', '~', '~35.215.755'],
         ['A. OPERASIONAL', '', '', ''],
         ['     4-1100 Pendapatan SPP', '198.646.266', '', '198.646.266'],
         ['     5-2200 Beban Listrik', '', '4.850.000', '-4.850.000'],
         ['~     Arus Kas Bersih - Operasional', '~198.646.266', '~4.850.000', '~193.796.266'],
         ['B. INVESTASI', '', '', ''],
         ['     1-3100 Peralatan Lab', '', '66.334.500', '-66.334.500'],
         ['~     Arus Kas Bersih - Investasi', '~0', '~66.334.500', '~-66.334.500'],
         ['~KENAIKAN (PENURUNAN) KAS & BANK', '~387.323.520', '~386.104.845', '~1.218.675'],
         ['~SALDO AKHIR KAS & BANK', '~', '~', '~36.434.430'],
         ['~SELISIH (harus 0)', '~', '~', '~0']])
b.kotak('Kalau semua nilai menumpuk di "(Belum dipetakan)"',
        'Pengelompokan Operasional / Investasi / Pendanaan mengikuti pemetaan akun. Jalankan '
        'laporan "Diagnosa Pemetaan Aktivitas Arus Kas" (kategori Buku Besar) - laporan itu '
        'menampilkan persis akun mana yang belum dipetakan, diurut dari penyumbang terbesar, '
        'lalu petakan lewat Akuntansi > Setup Laporan jenis Arus Kas.')

# ------------------------------------------------------------------- butir 5
b.judul_bagian('5.', 'Pelaporan neraca')
b.teks('Pintu A > kategori Keuangan untuk neracanya; neraca percobaan ada di kategori '
       'Buku Besar (Akuntansi).')
b.maket('Keuangan', 'Neraca (Berbasis Jurnal Akuntansi)',
        ['Keterangan', 'Nilai'],
        [['~ASET LANCAR', '~'],
         ['     1-1110 Kas Tunai', '16.850.000'],
         ['     1-1210 Bank BSI - SPP', '3.926.782'],
         ['     1-1300 Piutang Kegiatan', '13.100.000'],
         ['~Subtotal Aset Lancar', '~33.876.782'],
         ['~KEWAJIBAN DAN ASET BERSIH', '~'],
         ['     2-1100 Hutang Usaha', '17.610.000'],
         ['     3-1000 Aset Bersih', '118.193.135'],
         ['     Laba (rugi) berjalan', '1.218.675'],
         ['~Subtotal Kewajiban & Aset Bersih', '~137.021.810']])
b.maket('Buku Besar (Akuntansi)', 'Neraca Percobaan Lengkap (Awal - Mutasi - Akhir)',
        ['Kode / Nama Akun', 'Awal D', 'Awal K', 'Kas D', 'Kas K', 'Non Kas D',
         'Non Kas K', 'Akhir D', 'Akhir K'],
        [['1-1110 Kas Tunai', '6.200.000', '0', '55.700.000', '45.050.000', '0', '0',
          '16.850.000', '0'],
         ['4-1100 Pendapatan SPP', '0', '0', '0', '198.646.266', '0', '0', '0', '198.646.266'],
         ['5-2200 Beban Listrik', '0', '0', '4.850.000', '0', '0', '0', '4.850.000', '0'],
         ['~TOTAL', '~35.215.755', '~35.215.755', '~387.323.520', '~387.323.520',
          '~2.400.000', '~2.400.000', '~421.116.900', '~421.116.900']])
b.teks('Kolom Mutasi Kas dan Mutasi Non Kas dipisah persis seperti kertas kerja yayasan: sebuah '
       'baris tergolong mutasi kas bila jurnalnya menyentuh akun kas atau bank, selain itu '
       'tergolong non kas (memorial atau penyesuaian). Tiap pasang kolom harus seimbang.',
       ukuran=9.2, warna=(0.357, 0.420, 0.373))

# ------------------------------------------------------------------- butir 6
b.judul_bagian('6.', 'Pelaporan laba rugi')
b.teks('Pintu A > kategori Keuangan. Untuk lembaga nirlaba tersedia bentuk Laporan Aktivitas '
       'yang sudah memuat marginnya.')
b.maket('Keuangan', 'Laporan Aktivitas (Surplus/Defisit)',
        ['Keterangan', 'Nilai'],
        [['~A. PENDAPATAN', '~'],
         ['     4-1100 Pendapatan SPP', '198.646.266'],
         ['     4-1300 Pendapatan FF Mart', '29.714.057'],
         ['~JUMLAH PENDAPATAN', '~228.360.323'],
         ['~B. BIAYA', '~'],
         ['  1. HARGA POKOK PENJUALAN (HPP)', '35.634.642'],
         ['~  2. LABA (RUGI) KOTOR', '~192.725.681'],
         ['       Contribution Margin (%)', '84,4'],
         ['  3. BIAYA TETAP', '52.400.954'],
         ['~  4. SURPLUS (DEFISIT)', '~140.324.727'],
         ['       Profit Margin (%)', '61,4']])
b.teks('Tersedia pula Laba Rugi (Berbasis Jurnal Akuntansi), Laba Rugi 2 Periode, dan Laba Rugi '
       '12 Bulan untuk pembandingan antar periode.', ukuran=9.2, warna=(0.357, 0.420, 0.373))

# ------------------------------------------------------------------- butir 7
b.judul_bagian('7.', 'Budgeting')
b.teks('Menyusun anggarannya di menu Akuntansi > Anggaran (RAB Bulanan) yang punya tiga tab: '
       'Rencana Bulanan, Realisasi, dan Penggunaan Anggaran. Laporannya di Pintu A > kategori '
       'Anggaran (RAB).')
b.maket('Anggaran (RAB)', 'Anggaran vs Realisasi (RAB)',
        ['Kode', 'Uraian', 'Anggaran', 'Realisasi', 'Sisa', '% Serap'],
        [['5.01', 'Belanja Barang', '240.000.000', '227.829.012', '12.170.988', '94,9'],
         ['5.02', 'Biaya Pegawai', '96.000.000', '49.310.954', '46.689.046', '51,4'],
         ['5.03', 'Pemeliharaan', '18.000.000', '13.129.441', '4.870.559', '72,9'],
         ['~TOTAL', '~', '~354.000.000', '~290.269.407', '~63.730.593', '~82,0']])

# -------------------------------------------------------------------- akhir
b.judul_bagian('', 'Kalau laporannya kosong atau angkanya kurang')
b.langkah([
    'Transaksinya belum diposting ke jurnal. Buka Akuntansi > Draft Jurnal, pilih rentang '
    'tanggal, lihat baris mana yang masih berangka di kolom draf, lalu posting. Laporan '
    'berbasis jurnal hanya membaca yang sudah terposting.',
    'Akunnya belum dipetakan ke Kelompok Laporan. Jalankan laporan Diagnosa Pemetaan Akun - '
    'akun yang muncul di situ tidak akan tampil di Neraca maupun Laba Rugi.',
    'Unit yang dipilih salah. Jurnal lama yang satuan kerjanya kosong hanya terlihat pada '
    'pilihan "Semua Unit (Konsolidasi)".',
])
b.kotak('Menunya tidak kelihatan?',
        'Menu keuangan bersifat tertutup secara bawaan dan hanya muncul bila peran penggunanya '
        'diberi kunci aksesnya: "laporankeuangan" untuk Pintu A dan "laporan" untuk Pintu B. '
        'Bila seorang staf tidak melihat menunya, itu soal hak akses peran - bukan pertanda '
        'laporannya belum ada. Atur lewat pengaturan peran, lalu keluar dan masuk lagi.',
        warna=(0.608, 0.173, 0.173))
b.kotak('Satu katalog, empat platform',
        'Daftar laporan hidup di satu tempat pada server dan dibaca oleh POS Desktop, POS '
        'Android, versi JSP, serta aplikasi web ZK. Nama laporan, kategori, dan kolomnya identik '
        'di keempatnya, termasuk pemilih Unit / Satuan Kerja - jadi panduan ini berlaku untuk '
        'semua platform.')

print('PDF tersimpan:', b.simpan())
