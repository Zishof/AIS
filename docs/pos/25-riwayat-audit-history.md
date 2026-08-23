# 25 — Tombol "History": jelajah tabel audit & restore

Dilatarbelakangi insiden nyata: pesanan bulan Agustus hilang dari halaman **Pesanan** di
versi JSP, dan dibutuhkan untuk penagihan.

## Mengapa tombol jam per baris tidak menolong

Riwayat per baris sudah lama ada — `RevisiApiHelper` (aksi `revisi_daftar`/`revisi_detail`/
`revisi_pulihkan`) dengan dialog `lib/widgets/riwayat_data_dialog.dart`, terpasang di
sekitar 25 layar master.

Tapi tombol itu **menuntut barisnya masih ada untuk diklik**. Baris yang sudah terhapus
tidak muncul di layar mana pun, jadi tidak ada yang bisa diklik — padahal justru baris
itulah yang dicari. Yang kurang adalah padanan tab **"Semua"** milik `GenericRevisiHelper`
(ZK): menyapu tabel audit menurut rentang tanggal, tanpa perlu tahu id-nya lebih dulu.

## Fondasi Envers

`hibernate.cfg.xml`:

```xml
<property name="org.hibernate.envers.store_data_at_delete">true</property>
<property name="org.hibernate.envers.audit_table_suffix">__audit</property>
<property name="org.hibernate.envers.default_schema">new_audit</property>
```

`store_data_at_delete=true` berarti revisi HAPUS **menyimpan isi barisnya**, sehingga data
yang terhapus masih bisa dibaca dan dipulihkan.

Entitas yang relevan dengan halaman Pesanan (semuanya `@Audited`):

| Kode entitas | Kelas | Tabel |
|---|---|---|
| `pesanan` | `koperasi.DraftPembelianAnggotaKoperasi` | `koperasi.draft_pembelian_anggota_koperasi` |
| `pesanan_item` | `inventory.DraftPembelian` | `koperasi.draft_pembelian` |
| `pembelian` | `inventory.Pembelian` | `koperasi.pembelian` |
| `transaksi` | `koperasi.PembelianAnggotaKoperasi` | `koperasi.pembelian_anggota_koperasi` |

Halaman Pesanan JSP membaca `draft_pembelian_anggota_koperasi` (header) dan
`draft_pembelian` (baris) — diverifikasi langsung dari JSP-nya, bukan diduga.

## Aksi server baru

| Aksi | Guna | Revisi |
|---|---|---|
| `revisi_entitas` | daftar kode entitas untuk mengisi combo | r77873 |
| `revisi_jelajah` | sapu audit lintas baris menurut saringan | r77873 |
| `revisi_pulihkan_massal` | restore satuan **dan** massal | r77877 |

### Saringan

Rentang tanggal **wajib**, mengikuti versi ZK: tabel audit menyimpan seluruh sejarah, dan
menyapunya tanpa batas akan menarik jutaan baris. Rentang tanggal diterjemahkan lebih dulu
ke rentang **nomor revisi** (`getRevisionNumberForDate`) karena nomor revisi terindeks;
hasilnya disaring ulang per tanggal di Java karena batas bawah dari terjemahan itu sedikit
longgar.

Saringan lain: tipe perubahan (`SEMUA`/`TAMBAH`/`UBAH`/`HAPUS`), toko, kata kunci, kolom
tertentu, dan id tunggal.

> **Aturan yang disalin dari ZK dan wajib dipertahankan.** `LIKE` hanya boleh menyentuh
> kolom **teks**. Envers tetap bersedia menyusun `... LIKE ?` untuk kolom Integer/Long,
> lalu Hibernate mem-binding parameternya memakai tipe kolom asli dan meledak dengan
> ClassCastException — **bukan saat query disusun, melainkan jauh kemudian saat
> dieksekusi**. Komentar di `GenericRevisiHelper.buildKeywordCriterion` menyebut ini sudah
> pernah menimpa mereka.

Nilai yang tidak cocok dengan tipe kolom (`jumlah = "lima"`, `tanggal = "31-08-2026"`)
ditolak di tempat, bukan diubah jadi query yang pasti gagal saat dijalankan.

### Aturan revisi mana yang dipulihkan

Disalin persis dari `GenericRevisiHelper.restoreLatestFromDate`:

1. revisi diurutkan dari yang **terbaru**;
2. revisi bertipe **HAPUS dilewati**;
3. revisi pertama yang tersisa untuk tiap id dipakai.

Artinya yang kembali adalah keadaan terakhir **sebelum** baris dihapus — bukan cuplikan
penghapusannya.

Restore satuan dan massal memakai aksi yang **sama**; yang satuan hanya menambah saringan
id. Kalau jalurnya dipisah, "Pulihkan baris ini" dan "Pulihkan semua" akan menyimpang
pelan-pelan sampai artinya berbeda bagi orang yang menekannya. `salinKeLive()` adalah
satu-satunya jalur penyalinan, dipakai bersama oleh `pulihkan()` dan `pulihkanMassal()`.

### Pengaman restore massal

Restore massal tidak punya tombol batal, jadi:

- **dihitung dulu tanpa menulis apa pun** (`simulasi=true`); hasil hitungan ditampilkan di
  dialog konfirmasi sebelum satu baris pun tersentuh;
- bawaannya **hanya menghidupkan baris yang benar-benar sudah tidak ada**. Ini **berbeda
  dari versi ZK**, yang menimpa semuanya. Untuk sapuan lintas ratusan baris lewat API,
  menimpa data yang masih dipakai orang adalah kerugian yang tidak bisa dibatalkan,
  sedangkan menghidupkan yang hilang tidak merusak apa pun. Menimpa tetap bisa lewat
  `timpaYangMasihAda=true`, dan **angka di dialog ikut berubah saat dicentang** supaya yang
  tertulis di layar memang yang akan terjadi;
- **batas jumlah baris per panggilan** (bawaan 200, maksimum 500); bila terpotong,
  laporannya mengatakannya alih-alih diam-diam berhenti;
- satu transaksi **per baris**; satu baris gagal tidak menyeret sisanya.

Aksi ini dibatasi **ADMINISTRATOR** karena keluarannya memuat data terhapus dari seluruh
toko — melewati pembatasan toko/pendaftar yang berlaku di layar biasa. Riwayat per baris
(`revisi_daftar`) tetap terbuka untuk semua pengguna.

## Layar

`lib/screens/riwayat_audit_screen.dart`, dijangkau dari tombol **History** di layar
**Pesanan** dan **Riwayat Penjualan** (hanya untuk admin, sama dengan gerbang server supaya
tidak menjanjikan yang akan ditolak). Dibuka dengan saringan **Terhapus**, karena itulah
pertanyaan yang membawa orang ke sana. Mengklik satu baris membuka dialog riwayat per baris
yang sudah ada.

Nilai ditampilkan **mentah** (hanya diberi pemisah ribuan). Ini layar forensik; angka yang
sudah dipercantik mengaburkan apa yang benar-benar tersimpan.

Ketika hasilnya kosong, layar menyebut batasnya secara eksplisit: **audit hanya merekam
perubahan yang lewat aplikasi**; baris yang dihapus langsung di basis data tidak
meninggalkan jejak, jadi hasil kosong **bukan** berarti "tidak pernah dihapus".

## Batas yang disengaja

Restore ini **dangkal** — hanya entitas yang dipilih. Versi ZK menelusuri dependensi secara
rekursif (`restoreDependenciesRecursively`, `applyDeferredRelations`, `saveOrReplicate`,
sekitar 900 baris yang terikat erat ke ZK); itu tidak ditiru, karena menulis ulang mesin
sebesar itu tanpa bisa mengujinya di basis data nyata adalah risiko yang tidak sebanding.

Konsekuensi praktis untuk pesanan: **pulihkan induknya (`pesanan`) dulu, baru jalankan
sapuan kedua untuk itemnya (`pesanan_item`)**. Relasi anak mencari baris hidup ber-id sama,
jadi urutan itu berhasil; kalau dibalik, item-nya kehilangan relasi ke induk dan dilewati.

## Uji & yang belum terbukti

30 uji tanpa basis data: 11 untuk batas rentang tanggal, 19 untuk saringan (memakai
`ClassMetadata` tiruan lewat `java.lang.reflect.Proxy`).

Yang paling dikejar: batas atas rentang harus 23:59:59.999. Kalau berhenti di tengah
malam, permintaan "sampai 31 Agustus" kehilangan seisi hari terakhir — alat pencari data
hilang justru ikut kehilangan data, tanpa tanda apa pun bahwa ada yang salah.

**Belum terbukti:** perilakunya terhadap data audit sungguhan — pemilihan revisi, dedupe
per id, dan `replicate` untuk baris yang dihidupkan kembali. Semua itu butuh basis data.
Setelah deploy, disarankan mencoba dengan **rentang satu hari dan satu toko** lebih dulu,
melihat hasil hitungannya, baru melebarkan.

## Kueri langsung (selagi menunggu deploy)

Read-only, membaca tabel audit yang sama:

```sql
SELECT a.id, r.rev, to_timestamp(r.revtstmp/1000) AS waktu, a.revtype,
       a.kode, a.toko, a.total_biaya
FROM   new_audit.draft_pembelian_anggota_koperasi__audit a
JOIN   new_audit.revinfo r ON r.rev = a.rev
WHERE  a.revtype = 2                                  -- 2 = DEL
  AND  to_timestamp(r.revtstmp/1000) >= '2026-08-01'
  AND  to_timestamp(r.revtstmp/1000) <  '2026-09-01'
ORDER  BY r.rev DESC
LIMIT  200;
```

Bila mengembalikan baris, datanya masih utuh dan bisa dipulihkan. Bila kosong,
penghapusannya tidak lewat aplikasi dan penelusurannya harus pindah ke log basis data.
