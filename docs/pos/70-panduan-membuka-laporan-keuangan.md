# Panduan Membuka Laporan Keuangan (POS Desktop, POS Android, JSP, ZK)

Tanggal: 31 Agustus 2026, keadaan pada r78695. Menjawab "pastikan laporan-laporan di atas sudah
semua, dan jelaskan di mana cara membuka menu laporan-laporan tersebut". Daftar apa yang sudah ada
dan celah yang sudah ditutup dibahas di dok [66](66-laporan-keuangan-standar-yayasan.md),
[67](67-laporan-aktivitas-dan-pemilih-unit.md), dan [69](69-penutup-peta-posting.md); dokumen ini
khusus soal **jalan masuknya**.

## 1. Dua pintu, jangan tertukar

Hampir semua laporan keuangan ada di balik SATU layar katalog. Yang membedakan hanya pintunya:

| Pintu | Menu | Isinya |
|---|---|---|
| **A** | **Akuntansi → Laporan-Laporan Keuangan** | Subset keuangan: 9 kategori akuntansi saja, tanpa laporan operasional. Inilah pintu utama untuk neraca, laba rugi, arus kas, buku besar, kas & bank, piutang, utang, pajak, anggaran, gaji, rekonsiliasi bank. |
| **B** | **Laporan-Laporan** (menu tingkat atas) | Katalog PENUH ± 180 laporan / 32 kategori — semua isi pintu A **ditambah** Penjualan, Pembelian, Persediaan, Gudang, Produksi, Opname, Margin, dan lainnya. |

Cara memakainya sama: buka menunya → pilih kategori di combo **Kategori** (atau ketik di kotak
cari) → klik judul laporannya → isi **Tgl Mulai / Tgl Sampai** (dan **Unit / Satuan Kerja** bila
muncul) → tekan **Tampilkan**. Hasilnya bisa dicetak **PDF** atau diunduh **Excel** dari layar yang
sama.

> **Pemilih Unit / Satuan Kerja** muncul otomatis pada laporan berbasis jurnal. Pilih satu unit
> (SMP, FF Mart, Katering, Laundry) untuk paket per unit, atau **"Semua Unit (Konsolidasi)"** untuk
> gabungan. Laporan operasional (penjualan, stok) tidak menampilkannya karena tidak bergantung
> satuan kerja.

## 2. Tujuh kebutuhan → jalannya

| # | Kebutuhan | Buka lewat | Nama laporan / layar |
|---|---|---|---|
| 1 | **Pembukuan kas masuk & keluar** | Pintu A → kategori **Kas & Bank (Akuntansi)** | **Buku Kas Umum (Mutasi Kas & Bank)** — buku kas per rekening dengan akun lawan & saldo berjalan; **Posisi Dana (Saldo Kas & Bank per Rekening)** — saldo awal, mutasi D/K, saldo akhir; **Rekening Koran**, **Ringkasan Daftar Penerimaan**, **Ringkasan Daftar Pembayaran** |
| | *input & persetujuannya* | Menu **Keuangan** | Kas Besar, Kas Kecil, Uang Muka, Pertanggungjawaban, Penggantian Kas Kecil, Dana Talangan, Reimbursement Pegawai, Proses Transfer |
| 2 | **Penjualan & pembelian** | **Pintu B** (katalog penuh) | Kategori **Penjualan** (13 laporan), **Pembelian** (5), **Accurate POS** (5), **Pengadaan (Vendor/PO/BAST)** (15). Ditambah menu **Laporan Transaksi** (6 tab: order, sesi, per kasir, payment) dan **Riwayat Penjualan** |
| 3 | **Utang piutang** | Pintu A → kategori **Piutang** dan **Pengadaan** | Piutang: Daftar Saldo, Faktur Belum Lunas, **Umur Piutang (Aging)**, Buku Besar Pembantu Piutang, Histori Piutang, Limit & Sisa Kredit. Utang: Saldo Hutang per Supplier, **Umur Hutang (Aging)**, Buku Besar Pembantu Utang, Riwayat Pembayaran, Utang Vendor Belum Lunas |
| | *layar kerjanya (varian Inventory & Sales)* | Menu tingkat atas | **Piutang Customer (AR)** (6 tab, termasuk Aging Customer & Aging per Sales), **Hutang Supplier (AP)** (5 tab, termasuk Aging) |
| 4 | **Cash flow tiap bulan** | Pintu A → kategori **Keuangan** | **Arus Kas per Aktivitas (Operasional/Investasi/Pendanaan)** — bentuk yang sama dengan lembar yayasan, ditutup SELISIH yang harus nol; **Arus Kas (Berbasis Jurnal Akuntansi)** — versi menurut akun lawan |
| 5 | **Neraca** | Pintu A → kategori **Keuangan** | **Neraca (Berbasis Jurnal Akuntansi)**, **Neraca — 2 Tanggal**, **Neraca Lajur (Kertas Kerja)**. Neraca percobaan ada di kategori **Buku Besar (Akuntansi)**: **Neraca Percobaan Lengkap (Awal - Mutasi - Akhir)** dan Neraca Percobaan (Neraca Saldo) |
| 6 | **Laba rugi** | Pintu A → kategori **Keuangan** | **Laporan Aktivitas (Surplus/Defisit)** — bentuk nirlaba beserta Contribution Margin & Profit Margin; **Laba Rugi (Berbasis Jurnal Akuntansi)**, **Laba Rugi — 2 Periode**, **Laba Rugi — 12 Bulan** |
| 7 | **Budgeting** | Menu **Akuntansi → Anggaran (RAB Bulanan)** untuk menyusunnya (3 tab: Rencana Bulanan, Realisasi, Penggunaan Anggaran); Pintu A → kategori **Anggaran (RAB)** untuk laporannya | **Anggaran vs Realisasi (RAB)** — anggaran, realisasi, sisa, % serap |

Tambahan yang sering dicari bersamanya, semuanya di Pintu A kategori **Buku Besar (Akuntansi)**:
Keseluruhan Jurnal (Jurnal Umum), Rincian Buku Besar per Akun, **Histori Buku Besar (Saldo
Berjalan)**, Ringkasan Buku Besar, Daftar Akun Perkiraan, Daftar Aset Tetap (Nilai Buku).

## 3. Kalau menunya tidak kelihatan

Menu keuangan **fail-closed**: hanya muncul bila peran pengguna diberi kuncinya. Kunci induk
Pintu A adalah `laporankeuangan`; kunci Pintu B adalah `laporan`. Bila seorang staf tidak melihat
menunya, itu soal hak akses peran — bukan laporannya belum ada. Atur lewat pengaturan peran/menu,
lalu keluar-masuk aplikasi.

## 4. Kalau laporannya kosong atau angkanya kurang

Tiga sebab yang paling sering, berurutan dari yang paling umum:

1. **Transaksinya belum diposting ke jurnal.** Buka **Akuntansi → Draft Jurnal**, pilih rentang
   tanggal, lihat baris mana yang masih berangka di kolom draf, lalu posting. Laporan berbasis
   jurnal HANYA membaca yang sudah terposting.
2. **Akunnya belum dipetakan ke Kelompok Laporan.** Jalankan **Diagnosa Pemetaan Akun** (kategori
   Buku Besar) — akun yang muncul di situ tidak akan tampil di Neraca/Laba Rugi. Khusus Arus Kas
   per Aktivitas, jalankan **Diagnosa Pemetaan Aktivitas Arus Kas**.
3. **Unit yang dipilih salah.** Jurnal lama yang satuan kerjanya kosong hanya terlihat pada
   "Semua Unit (Konsolidasi)".

## 5. Empat platform, satu katalog

Katalog laporan hidup di SATU tempat pada server (`LaporanKatalogData`), dan keempat penyaji
membacanya dari sana — jadi laporan baru muncul di semuanya tanpa didaftarkan ulang:

| Platform | Layar | Sumber katalog |
|---|---|---|
| POS Desktop & Android | Laporan-Laporan Keuangan / Laporan-Laporan | aksi API `laporan_keuangan_katalog` / `laporan_katalog` |
| JSP | `laporan_keuangan.jsp` / `laporan_laporan.jsp` | `katalogKeuangan()` / `katalog()` |
| ZK | panel laporan pada dasbor kantin | `LaporanKantinZkPanel` memanggil katalog yang sama |

**Yang diperbaiki r78694/r78695**: `laporan_laporan.jsp` dahulu menyalin katalognya sebagai array
JavaScript sendiri, dan salinan itu **tertinggal 19 laporan** — termasuk enam laporan baru serta
Umur Hutang Supplier, Buku Besar Pembantu Piutang/Utang, Daftar Aset Tetap, Laba Rugi 2 Periode &
12 Bulan, dan Neraca Lajur. Salinan itu dihapus dan diganti katalog server, sehingga tidak ada
lagi jalan bagi JSP untuk diam-diam basi. Diperiksa lebih dulu bahwa tidak ada satu pun laporan
yang HANYA ada di salinan JSP, jadi penggantian ini murni menambah.

**Pemilih Unit / Satuan Kerja** kini juga ada di JSP dan ZK, bukan hanya di POS — memakai daftar
unit dan unit bawaan dari server yang sama.

## 6. Versi web lama (ZK)

Laporan resmi berformat JRXML masih tersedia di aplikasi web: **Buku Besar**, **Jurnal Harian**,
**Laporan Akun**, **Laporan Keuangan** (Neraca/Laba Rugi/Arus Kas lewat combo Jenis Laporan),
**Laporan Komparasi Bulanan/Tahunan**, **Laporan Arus Kas**, **Laporan Arus Harian**, **Laporan
Trial Balance**, serta 18 laporan RAB. Semuanya juga dapat dibuka dari Pintu B lewat kategori
"Laporan Keuangan Resmi — Komparatif (Akuntansi)", "Buku Besar Resmi (Akuntansi)", dan "Arus Kas &
Analisa Keuangan (Akuntansi)" — item pada tiga kategori itu membuka halaman web di peramban, jadi
sengaja TIDAK ikut di Pintu A yang seluruhnya berjalan native.
