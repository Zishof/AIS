# Gap Analysis Laporan Keuangan: Paket Standar Yayasan vs AIS/POS

Tanggal: 31 Agustus 2026, pada HEAD r78640. Menjawab "apakah sudah semua? tolong pastikan
laporan-laporan seperti ini juga tersedia" atas lima berkas laporan keuangan Konsorsium Al
Bahjah (SMP An Nahl, FF Mart, Katering, Laundry) beserta tujuh butir kebutuhan keuangan.
Kode penutup celah masuk **r78641**.

## 1. Isi paket laporan yang dipakai yayasan

Kelima berkas memakai kerangka yang sama, hanya beda unit:

| Lembar | Isi | Peran |
|---|---|---|
| Mutasi Kas & Bank per rekening | tanggal, uraian, debet, kredit, **saldo berjalan**, + 21 kolom distribusi klasifikasi (piutang usaha, persediaan, aktiva tetap, hutang usaha, modal, pendapatan usaha, HPP, biaya pegawai, listrik, internet, penyusutan, adm bank, dst.) | buku sumber |
| Posisi Saldo Bank / Posisi Dana | per rekening: saldo awal, mutasi debet, mutasi kredit, saldo akhir + TOTAL | ikhtisar kas |
| Neraca Percobaan | per akun: saldo awal D/K, **mutasi kas** D/K, **mutasi non kas** D/K, saldo akhir D/K | kertas kerja |
| Laporan Cash Flow | Penerimaan; Pengeluaran Operasional; Surplus Operasional; Investasi; Pendanaan; Saldo awal & akhir kas; **SELISIH harus 0** | arus kas |
| Laporan Aktivitas | Pendapatan; HPP (saldo awal barang + pembelian − persediaan akhir); Laba Kotor + Contribution Margin; Biaya Tetap; Laba Usaha + Profit Margin | laba rugi |

Ciri khas paket ini: semuanya **bertumpu pada buku kas** dan setiap lembar diakhiri
angka rekonsiliasi yang harus nol.

## 2. Peta tujuh butir kebutuhan terhadap yang SUDAH ada

| # | Kebutuhan | Keadaan sebelum r78641 |
|---|---|---|
| 1 | Pembukuan kas masuk & keluar | ADA — layar Kas Besar, Kas Kecil, Uang Muka, Penggantian, Dana Talangan, Proses Transfer (POS native); laporan `akn_rekening_koran`, `akn_penerimaan`, `akn_pembayaran`, `akn_histori_bb` (saldo berjalan), rekonsiliasi bank |
| 2 | Penjualan & pembelian | ADA berlimpah — 13 laporan penjualan + 5 pembelian + 15 pengadaan/vendor, ditambah layar Laporan Transaksi 6 tab dan Riwayat Penjualan |
| 3 | Utang piutang | ADA lengkap — AR: saldo, faktur belum lunas, **aging**, kartu/buku besar pembantu piutang, histori; AP: saldo per supplier, **aging**, buku besar pembantu utang, riwayat pembayaran |
| 4 | Cash flow bulanan | ADA sebagian — `akn_arus_kas` (menurut akun lawan) native, dan versi resmi 12 bulan/31 hari hanya di ZK (dibuka di peramban) |
| 5 | Neraca | ADA — `akn_neraca`, `akn_neraca_2tanggal`, `akn_neraca_lajur` (berbasis Kelompok Laporan), `akn_neraca_saldo` |
| 6 | Laba rugi | ADA — `akn_laba_rugi`, `akn_lr_2periode`, `akn_lr_12bulan`, plus 13 laporan margin |
| 7 | Budgeting | ADA paling lengkap — layar Anggaran (RAB Bulanan) 3 tab di POS, 18 laporan cetak RAB di ZK, `akn_anggaran` (Anggaran vs Realisasi) |

Kesimpulan: tidak ada butir yang kosong. Yang belum ada adalah **tiga BENTUK laporan**
di atas yang menjadi tulang punggung paket yayasan.

## 3. Tiga celah yang ditutup (r78641)

Ketiganya laporan native pada mesin `LaporanKantinUtil` + terdaftar di
`LaporanKatalogData`, pada kategori yang ikut `katalogKeuangan()` sehingga muncul di menu
**Laporan Keuangan** dan berjalan native di Desktop/Android/JSP/ZK — bukan membuka peramban.

### 3.1 `akn_posisi_dana` — Posisi Dana (Saldo Kas & Bank per Rekening)
Satu baris per akun kas/bank: saldo awal (seluruh jurnal terposting SEBELUM Tgl Mulai),
mutasi debet & kredit periode, saldo akhir. Sebelumnya saldo awal/akhir per rekening tidak
pernah tampil dalam satu tabel: `akn_rekening_koran` memberi baris transaksi,
`akn_penerimaan`/`akn_pembayaran` memberi total terpisah.

### 3.2 `akn_neraca_percobaan` — Neraca Percobaan Lengkap
Sepuluh kolom: saldo awal D/K, **mutasi kas** D/K, **mutasi non kas** D/K, saldo akhir D/K.
Pemisah kas vs non kas memakai definisi yang dipakai akuntan yayasan: sebuah baris jurnal
tergolong *mutasi kas* bila **grup transaksinya memuat minimal satu baris berakun kas/bank**
(`exists` ke `akunting.transaksi` saudaranya), selain itu *non kas* (memorial/penyesuaian).
`akn_neraca_saldo` yang lama hanya memberi saldo akhir D/K.

### 3.3 `akn_arus_kas_aktivitas` — Arus Kas per Aktivitas
Kerangka yatim keempat yang akhirnya dipakai. Mutasi kas diuraikan proporsional ke akun
lawan (rumus sama persis dengan `akn_arus_kas` sehingga totalnya tetap sama dengan mutasi
kas sesungguhnya), lalu dikelompokkan ke **Operasional / Investasi / Pendanaan** memakai:

1. **Kelompok Laporan jenis "Arus Kas"** → `master_grup_laporan.nama` (jenis laporan ke-3
   ini sudah ada sejak 2022 dengan enam kelompok, tetapi **nol akun terpetakan** dan tidak
   ada satu laporan pun yang membacanya);
2. cadangan: kolom **`akun.aktifitas`** ("Aktifitas (Arus Kas)" di master Kode Akun) yang
   dirawat layar master tetapi tidak dibaca laporan mana pun;
3. sisanya masuk keranjang **"(Belum dipetakan ke Aktivitas Arus Kas)"** — sengaja terlihat,
   bukan diam-diam dimasukkan ke Operasional.

Kolom: Keterangan | Penerimaan | Pengeluaran | Bersih; ditutup KENAIKAN (PENURUNAN) KAS,
SALDO AKHIR, dan **SELISIH (harus 0)** persis seperti lembar cash flow yayasan.

## 4. Pengujian

Harness `TesLaporanYayasan` (scratchpad, DB UAT), fixture `UATLPY-` rentang
**1–31 Desember 2091**, dua rekening kas/bank + empat akun lawan, tujuh jurnal (dua saldo
awal, empat jurnal kas, satu memorial tanpa kas):

| Skenario | Hasil |
|---|---|
| Posisi Dana | hanya akun kas/bank; Kas 10jt→13jt, Bank 5jt→2,3jt; awal + D − K = akhir |
| Neraca Percobaan | saldo awal/akhir D/K benar; beban terpisah 1,2jt kas dan 0,5jt non kas; **tiap pasang kolom seimbang** |
| Arus Kas Aktivitas | Operasional menggabung jalur pemetaan (3jt masuk) dan jalur cadangan `aktifitas` (1,2jt keluar) → bersih 1,8jt; Investasi −4jt; keranjang belum dipetakan 2,5jt |
| Tie-out | kenaikan kas 0,3jt, **SELISIH 0**; jurnal memorial tidak ikut terhitung |
| Katalog | ketiganya terdaftar di `katalogKeuangan()` |

**LULUS 20, GAGAL 0.** Kompilasi `javac -source 1.7 -target 1.7` bersih.

## 5. Yang masih perlu keputusan / pekerjaan admin

1. **Pemetaan Aktivitas Arus Kas belum diisi** — kini ada alat bantunya: laporan
   **Diagnosa Pemetaan Aktivitas Arus Kas** (r78658) menampilkan persis akun penggerak kas mana
   yang belum dipetakan, diurut dari penyumbang terbesar. Selama akun belum dipetakan (Akuntansi >
   Setup Laporan, jenis "Arus Kas") atau kolom Aktifitas pada Kode Akun belum diisi, seluruh
   nilai jatuh ke keranjang "Belum dipetakan". Perhatikan: kelompok jenis Arus Kas yang ada
   baru bernaung di grup **Operasional** dan **Investasi** — grup **Pendanaan** perlu
   ditambahkan bila laporan hendak persis mengikuti format yayasan.
2. **Satu satuan kerja per instalasi — SELESAI r78643** (dok
   [67](67-laporan-aktivitas-dan-pemilih-unit.md)). Dahulu semua laporan berbasis jurnal terkunci
   pada konfigurasi `satuan_kerja_kantin`, sehingga satu instalasi hanya bisa menampilkan satu
   unit; parameter `lintasSatker` ada di mesin tetapi tidak ada klien yang mengirimnya. Kini ada
   pemilih Unit/Satuan Kerja pada layar Laporan (termasuk pilihan "Semua Unit (Konsolidasi)").
3. **Kolom distribusi buku kas** (21 klasifikasi pada lembar Mutasi) tidak direplikasi:
   di AIS peran itu dipegang akun jurnal itu sendiri, dan rekapnya keluar lewat Buku Besar /
   Neraca Percobaan. Tidak dianggap celah.
4. **"Laporan Aktivitas" — SELESAI r78643** (dok
   [67](67-laporan-aktivitas-dan-pemilih-unit.md)). Semula tidak dibuat karena angkanya identik
   dengan `akn_laba_rugi`; atas permintaan, laporan tersendiri `akn_laporan_aktivitas` kini ada
   dengan susunan nirlaba lengkap beserta Contribution Margin dan Profit Margin. Rincian HPP
   bergaya persediaan (saldo awal barang + pembelian − persediaan akhir) tetap TIDAK ada di versi
   berbasis jurnal — di buku besar HPP adalah satu akun beban; versi berbasis stok tersedia di
   kategori "Margin, Laba & Analisa".
5. **Kelas yatim yang ditemukan sepanjang audit.** `LaporanBukuKasUmum` — yang paling penting
   karena satu-satunya BKU — sudah **digantikan laporan native `akn_buku_kas_umum` (r78658)** yang
   justru lebih dekat ke lembar yayasan (saldo berjalan + akun lawan) dan berjalan di semua
   platform. Sisanya belum tersentuh, dicatat saja:
   `LaporanNeracaLajur` (padanan native `akn_neraca_lajur` sudah ada),
   `LaporanBukuBesarPerTanggal`, `LaporanRiwayatTransaksi`, `LaporanJurnalHarianSimple` —
   berkas JRXML-nya ada, menunya tidak.

## 6. Sisi operasional (server)

Tidak ada kolom/tabel baru pada r78641 — hanya kode laporan; cukup bangun ulang WAR dan
mulai ulang Tomcat. Setelah itu tiga laporan baru langsung muncul di menu Laporan Keuangan
tanpa perubahan klien apa pun (katalog dikirim server).
