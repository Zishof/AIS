# Posting Massal "Jurnal Umum" dari Dasbor Draft Jurnal POS

Tanggal: 29 Agustus 2026. Kode masuk SVN r78520, dokumen ini r78525.

## 1. Apa yang dilengkapi

Baris **"Jurnal Umum"** — baris PERTAMA dasbor Draft Jurnal — selama ini satu-satunya
baris akuntansi inti yang masih `bisaPosting=false`: angkanya tampil, tetapi tombol
posting massal tidak pernah ditawarkan klien karena `DraftJurnalApiHelper.modulPosting`
belum memetakannya ke mesin mana pun. Sekarang:

- `PostingTransaksiHarianAction` (ZK legacy layar "Posting Jurnal Harian") mendapat dua
  metode statis jalur API mengikuti pola mesin massal lain (`postingSemua(mulai, sampai,
  oleh, tglPosting)` dan `batalkanPostingSemua(mulai, sampai)`).
- `DraftJurnalApiHelper` memetakan `"Jurnal Umum" -> kunci menu "jurnal_umum"` (sudah
  terdaftar di `EbisnisMenuKatalog` dan masuk kelompok "create = boleh memposting") dan
  mendispatch kedua arah ke mesin baru.
- Client Flutter TIDAK diubah: layar Draft Jurnal digerakkan bendera `bisaPosting` dari
  server, jadi tombolnya menyala sendiri begitu backend ini terpasang.

## 2. Kenapa bentuk mesinnya berbeda dari modul lain

Dokumen jurnal umum BERBEDA dari kas kecil/vendor/siswa: `GrupTransaksi` + baris
`Transaksi`-nya sudah ada sejak diketik manual. Memposting hanya **mencap** dokumen
dengan `PostingHistory`; membatalkan hanya **melepas cap** — tidak ada jurnal yang
dibuat atau dihapus. Konsekuensinya:

1. **Kriteria pemilihan disamakan persis dengan dasbor** (`DraftJurnalRingkasanUtil.
   kriteriaJurnalUmum`): `jenis_jurnal='Umum'`, `date(tanggal_transaksi)` pada rentang,
   `closing IS NULL`. Jumlah yang diproses tidak mungkin berselisih dengan angka yang
   barusan dilihat pengguna.
2. **Kontrak per dokumen mengikuti `NewUiJournalService.post`** (jalur posting per-dokumen
   New UI): baris harus ada dan balance (toleransi 0.005), dan bila konfigurasi
   `file_bukti_transaksi_jurnal_wajib_diupload` aktif, dokumen tanpa lampiran dilewati.
   Dokumen yang dilewati tercatat di angka `dilewati` respons dasbor.
3. **`posting=true` disetel pada riwayat.** Dasbor menghitung "terposting" lewat
   `PostingJurnalHelper.terapkanStatusPostingHistory` yang mensyaratkan
   `posting_history.posting=true`. Tombol "Posting Semua" ZK lama LUPA menyetel bendera
   ini, sehingga hasil posting massalnya justru tetap terhitung draf di dasbor — mesin
   baru tidak mewarisi cacat itu.
4. **Dokumen dengan riwayat `posting=false` TIDAK disentuh** dua arah. Itu keadaan
   "posting dinonaktifkan" yang disetel eksplisit dari layar Jurnal Umum
   (`setPostingActive`); menyalakannya kembali bukan efek samping posting massal.

## 3. Cacat riwayat-bersama yang sengaja dihindari

`NewUiJournalService.unpost(id)` menghapus `PostingHistory` tanpa memeriksa apakah
riwayat itu dirujuk dokumen lain. Posting massal (tombol ZK lama maupun jalur baru ini)
memakai SATU riwayat untuk BANYAK dokumen — `unpost` per dokumen pada riwayat bersama
akan gagal FK (dan karena satu transaksi, seluruh pembatalan dokumen itu ikut batal),
macet sampai tinggal satu perujuk. `batalkanPostingSemua` karena itu tidak mendelegasi
ke `unpost`: ia melepas cap dulu, lalu menghapus riwayat **hanya bila** tidak ada lagi
`grup_transaksi` maupun `transaksi` yang merujuknya. Diuji eksplisit (skenario E/F di
harness: dua dokumen berbagi riwayat, hanya satu dibatalkan → riwayat bertahan).

Catatan tambahan ditemukan saat menulis mesin ini, belum diperbaiki karena di luar
lingkup: jalur `unpost` New UI per-dokumen tetap membawa cacat itu bila dipakai pada
dokumen hasil posting massal.

## 4. Keamanan transaksi

- Sesi dibuka sendiri (`HibernateUtil.currentNativeSession()` + begin/commit per
  dokumen), pola yang sama dengan `PostingKasKecilAction` jalur API: dipanggil dari API
  tidak ada kerangka ZK yang meng-commit sesi berjalan, dan kegagalan satu dokumen tidak
  membatalkan dokumen lain yang sudah sah.
- Di dalam tiap transaksi, dokumen diperiksa ulang (`postingHistory`/`closing`) —
  sesi lain dapat memposting/meng-closing dokumen yang sama di sela pemilihan id.
- Bila tidak ada satu dokumen pun berhasil dicap, riwayat batch yang terlanjur dibuat
  DIHAPUS supaya riwayat posting tidak dipenuhi baris kosong.

## 5. Pengujian

Harness `TesPostingJurnalUmum` (scratchpad, DB UAT lokal `ais` — bukan tenant mana pun),
fixture berprefiks `UATPJU-` pada rentang 10–20 Maret 2001 yang dipastikan kosong dulu:

| Skenario | Hasil |
|---|---|
| A: balance, dalam rentang | terposting; riwayat `posting=true`; baris tercap status selesai; total debet/kredit terisi |
| B: berat sebelah (100k vs 50k) | dilewati, tetap draf |
| D: balance, luar rentang | tidak disentuh |
| E/F: dua dokumen berbagi satu riwayat, hanya E dalam rentang | batalkan: E lepas cap, riwayat bertahan (F masih merujuk), F tetap terposting |
| Posting ulang setelah semua tercap | n=0, riwayat kosong run kedua ikut terhapus |
| Batal ulang setelah bersih | n=0 |
| Kebersihan fixture | seluruh data uji terhapus |

**LULUS 19, GAGAL 0.** Kompilasi `javac -source 1.7 -target 1.7` bersih.

Catatan harness (untuk sesi berikutnya): menjalankan mesin yang MENYIMPAN entitas di
luar Tomcat butuh `el-api.jar` + `jasper-el.jar` Tomcat di classpath (Hibernate
Validator menagih `javax.el` saat save; harness baca-saja seperti TesSesiPadaWaktu tidak
kena), dan `System.exit` eksplisit di akhir karena pool c3p0 bukan thread daemon.

## 6. Peta sisa modul yang belum punya mesin — TUNTAS (29 Agustus 2026)

Inventarisasi awal 29 Agustus 2026 (saat itu terpasang 31 baris, termasuk Jurnal Umum
ini). Seluruh butir peta selesai pada hari yang sama:

- **PJ Pengembalian** — SELESAI r78531 (mesin) + r78539 (perbaikan tombol layar ZK),
  dok [54](54-posting-pengembalian-uang-muka.md).
- **Trio pembayaran aset** (`PostingPembayaran/Dp/TerminAction`) — SELESAI
  r78536+r78540, dok [57 vendor](57-posting-pembayaran-vendor.md) (audit tombol ZK:
  bersih).
- **Perjanjian Kerjasama** — SELESAI, dok [56](56-posting-perjanjian-kerjasama.md).
  **Payroll Pegawai/Penggajian** — SELESAI r78548 (mesin) + r78551/r78552 (perbaikan
  empat cacat tombol layar ZK), dok
  [57 payroll](57-posting-payroll-pegawai-penggajian.md).
  **Saldo Awal Kas Kecil** — SELESAI r78554–r78557, dok
  [58](58-posting-saldo-awal-kas-kecil.md).
- **Kantin: HPP, Penjualan, Toko** — SELESAI r78560: lima baris dasbor + ruting batch
  per periode (HPP/Penjualan, batal mundur-saja) dan per dokumen (4 posting toko,
  batal diarahkan ke dokumen pembalik), dok [59](59-posting-kantin-dasbor.md).
- Bukan mesin (jangan dibuatkan): `PostingJurnalAction` (kontainer multi-tab),
  `PostingHistoryAction` (penampil riwayat), `PostingPenyusutanTabsAction` (pembungkus
  sub-tab).

Dengan ini setiap baris dasbor draft jurnal yang punya mesin juga punya tombol yang
benar-benar bekerja — atau penolakan yang menjelaskan dirinya.

## 7. Revisi

- Kode: `PostingTransaksiHarianAction.java` + `DraftJurnalApiHelper.java` masuk di
  **r78520** (terbawa commit sapu sesi paralel; konten diverifikasi byte-identik dengan
  hasil kerja sesi ini di HEAD). Mirror `java/` selaras pada r78526.
- Dokumen ini masuk di **r78525** (juga terbawa commit sesi paralel, isi utuh).
