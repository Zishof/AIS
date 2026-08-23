# 09 — Dasbor Draft Jurnal di POS Desktop/Android

Padanan natif `draft_jurnal.zul` untuk aplikasi POS: berapa dokumen per jenis jurnal yang
masih draft, sudah terposting, dan sudah terkunci closing — lengkap dengan daftar dokumen
di balik tiap angka, dan tombol posting massal untuk modul yang mesinnya sudah ada.

**Tanpa webview dan tanpa iframe.** Seluruh isinya JSON yang dirender sebagai widget, jadi
ikut tema aplikasi dan bekerja sama di Desktop maupun Android. Test kontrak menguji hal itu
secara eksplisit (`WebView`/`InAppWebView`/`url_launcher` harus tidak ada di sumbernya).

| | |
|---|---|
| Server | `ais/action/master/akunting/util/DraftJurnalRingkasanUtil.java`, `ais/action/servlet/api/DraftJurnalApiHelper.java` |
| Aksi API | `draft_jurnal_ringkasan`, `draft_jurnal_rincian`, `draft_jurnal_posting`, `draft_jurnal_batal_posting` |
| Klien | `apps/ebisnis/lib/screens/draft_jurnal_screen.dart` |
| Menu | `MenuEBisnis.draftJurnal`, kunci hak akses `draft_jurnal` (fail-closed, bawaan hanya peran `keu` dan `am`) |
| Revisi | r77997 (ringkasan), r78000 (rincian), r78009 (posting), r78013 (mesin Kas Besar) |

---

## 1. Kenapa penghitungnya dipindah keluar dari composer ZK

Angka dasbor semula hanya hidup di dalam `DrafJurnalAction` (composer ZK, 1.254 baris) dan
menulis langsung ke `Rows` ZK. Kanal lain tidak punya cara memakainya selain menyalin
kriterianya — dan dua salinan kriteria untuk pertanyaan *"berapa jurnal yang belum
diposting"* adalah cara tercepat membuat dua kanal menjawab beda pada hal yang paling tidak
boleh berbeda.

`DraftJurnalRingkasanUtil` memuat penghitung itu **bebas ZK**: masukannya `Session` + rentang
tanggal, keluarannya data.

- `hitungModul(session, kunci, mulai, sampai)` — satu modul, boleh menghasilkan >1 baris.
- `hitungSemua(session, mulai, sampai)` — seluruh baris, berurutan.
- `hitungStatus(session, namaBaris, status, mulai, sampai)` — jumlah dokumen satu baris pada
  satu status; dipakai penjaga sebelum mesin posting dijalankan.
- `rincian(session, namaBaris, status, mulai, sampai, batas)` — daftar dokumennya.

`hitungModul` sengaja dipisah dari `hitungSemua` supaya layar ZK tetap dapat menjalankan tiap
modul di thread sendiri (dasbor ini memang lambat bila berurutan) tanpa menduplikasi kriteria.

> **STATUS PERALIHAN.** Isi util ini adalah **port persis** dari penghitung `DrafJurnalAction`
> — kriteria, urutan, dan kalimat uraiannya sama. Layar ZK **masih menjalankan salinannya
> sendiri**; pengalihan layar itu belum dikerjakan. Selama masa peralihan, **perubahan kriteria
> wajib dilakukan di kedua tempat**. Catatan yang sama ada di JavaDoc kelasnya.

## 2. Delapan belas modul → 31 baris

Urutannya mengikuti layar ZK (`DraftJurnalRingkasanUtil.KUNCI`):

`jurnal_umum`, `uang_muka`, `kas_kecil`, `kas_besar`, `pajak`, `tagihan_vendor`,
`pekerjaan_vendor`, `dp_vendor`, `dp_pekerjaan_vendor`, `jurnal_balik_dp_pekerjaan`, `gaji`,
`mahasiswa` (7 baris), `siswa` (6 baris), `penyusutan` (3 baris), `pengajuan_transfer`,
`transitori`, `closing`, `posting_hpp`.

Baris `posting_hpp` hanya muncul bila admin mengaktifkan tab "Posting HPP" di Konfigurasi >
Posting Jurnal (`Konfigurasi.POSTING_JURNAL_TAB_PREFIX + "posting_hpp"`).

**Kegagalan satu modul tidak menjatuhkan dasbor.** Tiap penghitung menangkap exception-nya
sendiri dan melaporkan 0, persis perilaku layar ZK: dasbor dengan satu baris bernilai 0 masih
berguna, dasbor yang gagal total tidak.

## 3. Satu kriteria untuk angka DAN daftar

`kriteriaDokumen(session, namaBaris, mulai, sampai)` mengembalikan kriteria dokumen **tanpa**
proyeksi hitung dan **tanpa** saringan status. Dua pemakainya:

- **Angka**: tambahkan `Projections.rowCount()` + `terapkanStatus(...)`.
- **Daftar**: tambahkan `terapkanStatus(...)` + `setMaxResults(...)`, lalu `list()`.

Kalau keduanya dibangun terpisah, cepat atau lambat pengguna mengetuk angka 7 dan menerima
daftar berisi 5 baris — dan yang salah menjadi mustahil ditentukan.

Dua mekanisme status dipertahankan apa adanya dari kode ZK, karena tidak seragam:

| Mekanisme | Dipakai baris |
|---|---|
| JOIN `postingHistory` (`PostingJurnalHelper.terapkanStatusPostingHistory`) | Jurnal Umum, Uang Muka, Kas Kecil, Kas Besar, Pajak, empat modul vendor, Jurnal Balik DP, Gaji |
| Restriksi properti (`restriksiPosting(field, ...)`) | seluruh baris Mahasiswa, Siswa, Penyusutan, Pengajuan Transfer, Transitori — dengan properti berbeda-beda (`postingHistoryDimuka`, `postingHistoryUangMuka`, `postingHistoryDenda`, `postingHistoryDiskon`, `postingHistoryPaymentGateway`) |
| Kolom `closing` pada GrupTransaksi | Jurnal Umum dan Closing |

`propertiPosting(namaBaris)` memegang pemetaan properti itu di satu tempat.

## 4. Pemeta dokumen berbasis refleksi

`petaDokumen` mengubah entity apa pun menjadi `{id, tanggal, uraian, nilai}` dengan mencari
getter yang sudah baku di model AIS:

- tanggal: `getTanggal`, `getTanggalPersetujuan`, `getTanggalTransaksi`, `getTanggalTagihan`,
  `getTanggalBayar`, `getWaktu`, `getWaktubayar`, `getPertanggal`, `getTanggalRealisasikan`
- nilai: `getNilai`, `getNominal`, `getBiaya`, `getDp`, `getNilaiPenyusutan`, `getDenda`,
  `getDiskonTidakLangsung`, `getBiayaAdministrasi`, `getBiayaPaymentGateway`, `getTotal`
- uraian: `getKeterangan`, `getUraian`, `getKode`, `getNama`, `getNomor`, `getNoBukti`,
  `getDeskripsi`

Alasannya: dasbor ini menyentuh 20-an entity dari empat modul berbeda. Pemeta khusus per
entity berarti 20 tempat yang harus diingat setiap kali sebuah entity berubah, padahal daftar
rincian hanya perlu tiga hal — kapan, apa, berapa.

**Konsekuensi yang perlu diketahui**: entity tanpa getter uraian di antara kandidat (mis.
`SaldoAwalMasterAsset`) tampil dengan "(tanpa keterangan)". Kalau uraiannya penting, tambahkan
nama getter ke daftar kandidat.

`id` dikirim sebagai **String**, bukan angka — beberapa id di basis ini (mis. `rab.workspace`)
berada di luar jangkauan aman angka JavaScript.

## 5. Mesin posting

`modulPosting(namaBaris)` mengembalikan kunci hak akses modul yang mesinnya **sudah ada**, atau
null. Ringkasan membawa bendera `bisaPosting` dan `bisaRincian` per baris, dan klien hanya
menawarkan tombol yang benar-benar ada mesinnya — tombol yang ujungnya menolak sama saja dengan
menjanjikan sesuatu yang tidak ada.

| Baris | Mesin (`postingSemua` / `batalkanPostingSemua`) | Kunci hak akses |
|---|---|---|
| Kas Kecil | `PostingKasKecilAction` (sudah ada sebelumnya) | `kas_kecil` |
| Kas Besar | `PostingKasBesarAction` | `kas_besar` |
| Uang Muka | `PostingUangMukaAction` | `uang_muka` |
| Pertanggungjawaban Uang Muka | `PostingPertangungjawabanAction` | `pj_uang_muka` |
| Pertanggungjawaban Kas Besar | `PostingPertangungjawabanKasBesarAction` | `pj_kas_besar` |
| Penggantian Kas Kecil | `PostingPenggantianKasKecilAction` | `penggantian_kas_kecil` |
| Jurnal Pengajuan Transfer | `PostingProsesTransferAction` | `pengajuan_transfer` |
| Transitori | `PostingProsesTransitoriAction` | `transitori` |
| Penerimaan Tagihan Vendor | `PostingPengadaanAction` | `pengadaan_tagihan` |
| Pekerjaan Vendor | `PostingPemesananPekerjaanAction` | `pengadaan_tagihan` |
| selain itu | belum ada | — |

Daftar ini bertambah satu per satu; aturan jurnal tiap mesin, dua penyimpangan sadar dari kode
ZK, dan hasil ujinya ada di [14](14-mesin-posting-per-modul.md) dan
[06](06-posting-jurnal-keuangan.md).

Gerbangnya memakai hak **`create` pada kunci MODUL-nya sendiri**, bukan kunci dasbor: peran
yang boleh membaca dasbor tidak otomatis boleh memposting isi modul yang bukan wewenangnya.

Dua penjaga yang lahir dari pengujian:

1. **Rentang kosong → mesin tidak dipanggil sama sekali.** `PostingKasKecilAction.postingSemua`
   menyimpan satu baris `PostingHistory` **sebelum** memeriksa ada-tidaknya dokumen, sehingga
   menekan tombol pada angka nol meninggalkan riwayat posting kosong di basis data.
2. **Ada dokumen tetapi nol terproses → dilaporkan sebagai PENOLAKAN**, bukan "berhasil, 0
   dokumen". Mesin posting lama menelan kegagalan per dokumen (`Common.tampilErrorJikaAdmin`),
   dan kalimat sukses justru menyesatkan persis ketika ada yang perlu diperiksa.

## 6. Kontrak API

```
draft_jurnal_ringkasan   { mulai?, sampai? }
  -> { status:"00", mulai, sampai, draft, posting, closing, total,
       data:[ { kunci, nama, keterangan, draft, posting, closing,
                bisaRincian, bisaPosting } ] }

draft_jurnal_rincian     { nama, status:"draft"|"posting"|"closing", mulai?, sampai?, limit? }
  -> { status:"00", nama, statusRincian, mulai, sampai, jumlah,
       data:[ { id, tanggal, uraian, nilai } ] }

draft_jurnal_posting        { nama, mulai?, sampai? }  -> { status:"00", nama, jumlah, description }
draft_jurnal_batal_posting  { nama, mulai?, sampai? }  -> idem
```

Tanggal memakai `yyyy-MM-dd`. Rentang bawaan menyalin layar ZK: **enam bulan ke belakang sampai
besok** — dan rentang yang benar-benar dipakai selalu ikut dikembalikan, sehingga tanggal yang
tidak terbaca tidak menjatuhkan seluruh dasbor.

## 7. Hasil pengujian (basis data UAT lokal)

Harness: `TesDraftJurnal.java` (lihat [08](08-harness-uji.md)).

| Yang diuji | Hasil |
|---|---|
| 18 modul dihitung | **tanpa satu pun exception**, 31 baris |
| Angka berasal dari query, bukan nol semua | Penerimaan Tagihan Vendor draft=1, Fix Aset BAST draft=4 |
| Periode bawaan | 2026-02-22 s.d. 2026-08-23 — sama persis dengan layar ZK |
| Angka tidak berubah setelah restrukturisasi kriteria | 31 baris, draft=4 — identik dengan sebelum perubahan |
| Jumlah baris rincian = angkanya | Penerimaan Tagihan Vendor 1=1, Fix Aset BAST 4=4 |
| Bendera kemampuan | 2 baris berposting (Kas Kecil, Kas Besar), 30 dari 31 baris berrincian |
| Aksi/nama tak dikenal, modul tanpa mesin, rentang kosong | seluruhnya ditolak dengan kalimat penjelas |
| Sesi thread-local mesin lama di luar ZK | `currentSession` dan `currentNativeSession` terbuka, query jalan, `begin`+`rollback` berhasil |

**Yang BELUM terverifikasi:** penulisan jurnal yang sebenarnya. Basis UAT tidak punya satu pun
dokumen `kas_kecil`/`kas_besar`/`pajak`, dan tabel master yang dibutuhkan untuk menyemai
dokumen uji (`jenis_kas_besar`, `satuan_kerja`) tidak ada di basis ini. Untuk menutup celah itu
dibutuhkan basis data yang berisi dokumen nyata.

## 8. Yang belum dikerjakan

1. Mengalihkan `DrafJurnalAction` (ZK) memakai `DraftJurnalRingkasanUtil` — menghapus
   duplikasi kriteria. Perlu memindahkan 31 pemetaan URL per baris, pantas jadi langkah
   tersendiri yang bisa diuji sendiri.
2. Mesin posting untuk 16 modul sisanya, satu per satu.
3. Uji tulis-jurnal pada basis data yang berisi dokumen.
