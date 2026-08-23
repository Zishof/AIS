# 18 — Seluruh mesin posting dasbor Draft Jurnal

Kelanjutan [14](14-mesin-posting-per-modul.md). Dokumen itu mencatat pola dan enam modul
pertama; dokumen ini mencatat penyelesaiannya sampai **32 dari 35 baris** dasbor punya mesin,
dan menjelaskan tiga baris terakhir yang **sengaja tidak** dibuatkan mesin.

---

## 1. Keadaan akhir

| Kelompok | Baris | Status |
|---|---|---|
| Keuangan | Kas Kecil, Kas Besar, Uang Muka, PJ Uang Muka, PJ Kas Besar, Penggantian Kas Kecil, Dana Talangan | ✔ |
| Pencairan | Jurnal Pengajuan Transfer, Transitori | ✔ |
| Vendor/Aset | Penerimaan Tagihan Vendor, Pekerjaan Vendor, DP Vendor, DP Pekerjaan Vendor, Jurnal Balik DP Pekerjaan, Pajak, Fix Aset (BAST), Aset dalam Pekerjaan (BAST), Jurnal Penyusutan | ✔ |
| Payroll | Gaji | ✔ |
| Mahasiswa | Piutang Tagihan, Pembayaran, Dibayar Dimuka, Tabungan/Deposit, Pengeluaran/Refund, Biaya Administrasi, Biaya Payment Gateway | ✔ |
| Siswa | Piutang Tagihan, Pembayaran, Dibayar Dimuka, Deposit, Piutang Denda, Utang Diskon | ✔ |
| **Tanpa mesin** | Jurnal Umum, Closing, Posting HPP | **sengaja** — bagian 4 |

---

## 2. Pola baru: pakai ulang, jangan salin

Modul-modul awal menyalin logika jurnal ZK ke dalam blok statis, dijaga komentar
"PEMELIHARAAN: harus identik". Untuk gelombang ini polanya diperbaiki: **bila layar ZK sudah
memisahkan logikanya ke sebuah method yang hanya memakai parameternya, method itu dijadikan
`static` dan dipakai bersama** — satu implementasi jurnal, dua pintu masuk, tidak mungkin
menyimpang.

| Kelas | Method yang kini dipakai bersama | Isi |
|---|---|---|
| `PostingCicilanSiswaAction` | `eksekusiPosting` | pemecahan nilai tabungan per baris detail, pemindahan ke akun "dibayar di muka", pemisahan denda |
| `PostingBiayaAdministrasiPembayaranMahasiswaAction` | `populateAkun` | pemilihan akun per kanal (Faspay/BNI/Jatelindo) lewat konfigurasi |
| `PostingBiayaPaymentGatewayPembayaranMahasiswaAction` | `populateAkun` | idem |
| `PostingDetailKegiatanAction` | `ambilTanggalPostingPiutang` | tanggal jurnal = cicilan pembayaran PERTAMA, bukan tanggal tagihan |

Sisanya tetap disalin karena logikanya menyatu di dalam badan tombol ZK; masing-masing
membawa komentar PEMELIHARAAN di tempatnya.

---

## 3. Hal-hal yang mudah salah, dan bagaimana dijaga

### 3.1 Satu dokumen memikul banyak jurnal

Beberapa entitas menerbitkan **lebih dari satu** jurnal, jadi pembatalan wajib menyaring — bila
tidak, membatalkan satu jenis akan menghapus jurnal saudaranya:

| Entitas | Pembeda | Baris yang berbagi |
|---|---|---|
| `tagihan` (siswa) | kolom `jenis` | Piutang Tagihan, Dibayar Dimuka, Piutang Denda, Utang Diskon |
| `cicilan_pembayaran` (mahasiswa) | kolom `ref` (`'dimuka'`) | Pembayaran, Dibayar Dimuka |
| `saldo_awal_master_asset` | kolom `ref` (`'DP_PEKERJAAN'`) | Pekerjaan Vendor, DP Pekerjaan Vendor |

### 3.2 Penanda posting berbeda-beda kolom

Bukan semua baris memakai `postingHistory`. Yang keliru di sini membuat dokumen tampak
"belum diposting" selamanya, atau sebaliknya hilang dari daftar draft tanpa jurnal:

| Baris | Kolom penanda |
|---|---|
| Siswa - Dibayar Dimuka | `postingHistoryUangMuka` |
| Siswa - Piutang Denda | `postingHistoryDenda` |
| Siswa - Utang Diskon | `postingHistoryDiskon` |
| Mahasiswa - Dibayar Dimuka | `postingHistoryDimuka` |
| Mahasiswa - Biaya Payment Gateway | `postingHistoryPaymentGateway` |
| selebihnya | `postingHistory` |

### 3.3 Satuan kerja: layar punya penyaring, API tidak

Beberapa layar mengambil satuan kerja dari komponen penyaring di halamannya
(`satuanKerja.getAttribute(...)`). Dari API komponen itu tidak ada, jadi urutannya dibuat
eksplisit dan ditulis di Javadoc masing-masing: satuan kerja **entitasnya** (fakultas
mahasiswa, sekolah siswa, satuan kerja dokumen gaji), lalu satuan kerja **pengguna yang
memposting** sebagai cadangan. Menulis jurnal tanpa satuan kerja membuat laporan per unit
kehilangan barisnya.

### 3.4 Gaji: bentuk ringkas, bukan rinci

Layar Gaji punya centang "rinci" yang memilih satu baris jurnal per item gaji per pegawai,
atau satu baris per AKUN dengan nilai yang sudah dijumlahkan. Dari API tidak ada centang itu;
yang dipakai adalah bentuk **ringkas**. Satu pembayaran gaji sebulan bisa memuat ribuan item,
dan jurnal sepanjang itu tidak terbaca di buku besar. Total nilainya sama persis.

### 3.5 Satu transaksi per dokumen

`PostingCicilanSiswaAction` di layar membungkus SELURUH batch dalam satu transaksi: satu
dokumen bermasalah membatalkan ribuan dokumen lain yang sudah benar. Jalur API memberi tiap
dokumen transaksinya sendiri — yang gagal hanya dirinya sendiri, tercatat di Error Log, dan
sisanya tetap diproses.

---

## 4. Tiga baris yang SENGAJA tidak dibuatkan mesin

Ketiganya bukan posting per-dokumen; memaksakan `postingSemua(mulai, sampai, …)` untuk mereka
berarti mengarang perilaku yang tidak ada di layar ZK.

| Baris | Sebabnya |
|---|---|
| **Posting HPP** | Diposting **per periode**, bukan per dokumen. `DraftJurnalRingkasanUtil` sendiri menolak merinci dokumennya (`bisaRincian = false`) karena tidak ada daftar yang jujur bisa ditampilkan. |
| **Closing** | Proses tutup buku itu sendiri, bukan penjurnalan dokumen. Barisnya menghitung grup transaksi yang sudah terkunci. |
| **Jurnal Umum** | "Posting"-nya adalah penanda pada `GrupTransaksi` jurnal manual, bukan dokumen sumber yang dijurnalkan. |

Bila ketiganya tetap diinginkan di dasbor, bentuk tombolnya harus berbeda — mis. Posting HPP
menjadi "posting periode ini" dengan parameter periode, bukan rentang tanggal dokumen. Itu
pekerjaan tersendiri, bukan penyeragaman mesin yang ada.

---

## 5. Hasil uji

### 5.1 Seluruh mesin tersambung dan tidak meledak (`TesSemuaMesinPosting`)

Harness baru menyapu **setiap** baris dasbor yang menyalakan bendera `bisaPosting`, memanggil
posting DAN batal posting pada rentang tahun 1900 (tidak ada dokumen yang cocok, jadi tidak
ada jurnal yang ditulis):

| Yang diperiksa | Hasil |
|---|---|
| Modul bermesin | **31** (Dana Talangan tidak muncul pada rentang bawaan dasbor) |
| Panggilan | **62** (31 × posting + batal) |
| Ditolak sopan dengan status 91 | **62** |
| Salah sambung (jatuh ke "belum terpasang") | **0** |
| Meledak (exception) | **0** |

Uji ini bukan formalitas: kriteria tiap mesin baru benar-benar dijalankan Hibernate di sini,
sehingga **nama properti yang salah ketik pasti ketahuan** — itu kelas kesalahan yang paling
mungkin terjadi saat memasang belasan mesin sekaligus.

### 5.2 Dasbor tetap utuh (`TesDraftJurnal`)

34 baris terhitung **tanpa satu pun exception**; 31 baris membawa tombol posting.

### 5.3 Yang BELUM diuji

**Bentuk jurnal masing-masing modul baru.** Basis UAT tidak punya dokumen yang memenuhi syarat
untuk modul Mahasiswa, Siswa, Gaji, maupun Penyusutan, jadi yang terbukti adalah: kriterianya
jalan, mesinnya tersambung, penjaganya bekerja, dan kompilasinya bersih. Nilai debet/kredit
yang benar-benar tertulis baru terverifikasi pada dua modul vendor (lihat
[14](14-mesin-posting-per-modul.md) bagian 5).

Karena itu penjaga kedua di API ("ada dokumen tetapi nol terproses → penolakan") tetap menjadi
jaring pengaman yang membuat kegagalan diam-diam terlihat oleh pengguna.

---

## 6. Kunci hak akses

Modul yang belum punya kunci menu di katalog POS diberi kunci deskriptif
(`siswa_pembayaran`, `mahasiswa_piutang`, `gaji`, `penyusutan_aset`, dst.). Kunci yang tidak
dikenal katalog ditegakkan **fail-closed** lewat `EbisnisMenuKatalog.aksesAkuntansi` — hanya
peran akuntansi bawaan yang terbuka — sama seperti perlakuan `pengajuan_transfer` dan
`transitori` sebelumnya. Modul yang sudah punya kunci memakai kunci aslinya
(`pengadaan_tagihan`, `pengadaan_po`, `pengadaan_pajak`) supaya hak yang sudah diatur admin
tetap berlaku.

---

## 7. Catatan kerja: tabrakan dengan sesi lain

Empat modul (DP Vendor, DP Pekerjaan Vendor, Pajak, Jurnal Balik DP Pekerjaan) ternyata sudah
dikerjakan sesi lain dan hanya belum terdaftar; dua baris BAST menyusul saat pekerjaan ini
berjalan. Sempat terjadi pendaftaran **ganda** untuk tiga nama. Yang dibuang adalah
pendaftaran sesi ini, bukan milik mereka: daftar mereka lebih lengkap dan pemetaan
`DP Pekerjaan Vendor → pengadaan_tagihan` lebih tepat daripada `pengadaan_po`, karena DP
pekerjaan memang melekat pada tagihannya.

Selama pekerjaan ini berlangsung, sampai **6 JVM** sesi lain berjalan bersamaan dan menghabiskan
kolam koneksi PostgreSQL (`max_connections=100`, ~50 per JVM) — harness sempat mati di tengah
boot Hibernate tiga kali berturut-turut. Ini alasan praktis aturan "satu JVM uji pada satu
waktu" yang sudah dicatat di [08](08-harness-uji.md).
