# 42 — Lokal-dulu dan hapus lunak

Aturan: **setiap mutasi ditulis ke penyimpanan lokal lebih dulu, baru dikirim ke
server.** Berlaku untuk tambah, ubah, dan hapus. Tujuannya agar pekerjaan kasir
dan admin tidak hilang ketika jaringan putus di tengah.

## Satu pintu

Seluruh layar memakai satu fungsi, bukan menyalin polanya masing-masing:

```dart
prosesSimpanMaster(
  context,
  aksi: ...,        // aksi API
  body: ...,        // muatan
  kunci: ...,       // kunci baris untuk pencocokan
  cacheKey: ...,    // kunci daftar di singgahan
  rowLokal: ...,    // baris yang ditulis lokal lebih dulu
  hapusLokal: ...,  // true untuk hapus lunak
);
```

Pendukungnya di `MasterOffline`: `daftarCacheDulu` (khusus muatan daftar),
`daftarTerhapusLokal`, dan `pulihkanLokal`.

Nama metodenya `daftarTerhapusLokal`, **bukan** `daftarTerhapus` — kesalahan
nama ini sudah sekali memakan waktu.

## Hapus lunak — dan mengapa hanya di sisi lokal

Keputusan pemilik sistem, dicatat apa adanya:

> Satukan ke mekanisme hapus lunak jika di sisi lokal; namun di sisi server
> serahkan ke mekanisme audit Hibernate, jadi hapusnya seperti hapus pada
> umumnya di sisi server. Jadi jika ingin mengembalikan data, harus mencari data
> tersebut lewat mekanisme restore dari tabel audit.

Maka bentuknya:

- **Lokal** — baris ditandai `_dihapus` + `_dihapusPada`. Barisnya tetap ada,
  sehingga pemulihan cukup lewat `pulihkanLokal` tanpa perlu jaringan.
- **Server** — hapus biasa. Server sudah punya jejak audit Hibernate sendiri;
  menambah kolom "dihapus" di sana hanya akan membuat dua mekanisme pemulihan
  yang saling bersaing dan bisa berbeda isi.

Penandanya diseragamkan menjadi `_dihapus` (sebelumnya `_dihapusLokal` di
sebagian layar) supaya satu penyaring berlaku untuk seluruh layar.

## Yang TETAP daring — dan alasannya

Tidak semua mutasi pantas dibuat lokal-dulu. Daftar berikut tercatat di
`test/master_offline_kontrak_test.dart` (spesifikasi 13.3) sebagai kontrak yang
diuji, bukan sekadar catatan:

| Golongan | Alasan |
|---|---|
| Kredensial dan kontrol akses | Menyimpan keputusan hak akses secara lokal berarti perangkat bisa memberi akses yang sudah dicabut server. |
| Alur yang butuh id server segera | Id sementara buatan lokal akan menjadi rujukan yang salah pada dokumen turunannya. |
| Uang | Saldo dan pembayaran harus tunggal dan berwenang di server; dua perangkat luring dapat membelanjakan saldo yang sama. |
| Pembalikan (reversal) | Membatalkan transaksi yang belum tentu sampai ke server dapat membalik sesuatu yang tidak pernah terjadi. |

**SOP/Workflow masuk golongan ini dan tetap daring** — modul itu memutuskan
siapa boleh menyetujui apa, dan keputusannya milik server.

Modul yang sudah punya kotak-keluar (outbox) sendiri dibiarkan memakai
mekanismenya masing-masing; tidak dipaksa pindah.

## Layar yang diubah

`kas_besar_screen.dart`, `kas_kecil_screen.dart`, `pj_kas_besar_screen.dart`,
`jurnal_umum_screen.dart`, `kulakan_bulk_entry_screen.dart`, dan lainnya —
seluruhnya lewat `prosesSimpanMaster`.

Pengujiannya di `test/keuangan_lokal_dulu_test.dart` dan
`test/master_offline_kontrak_test.dart`.

## Jebakan saat menyunting layar-layar ini

Pembersihan `_payloadDari` yang yatim sempat **memakan `_hapusBaris`**: metode
`_payloadDari` ditulis sebagai ekspresi tunggal yang berakhir dengan titik koma,
bukan blok kurung kurawal, sehingga pencocokan kurung menelan metode
berikutnya. Bila menyunting berkas-berkas ini secara otomatis, hitung kurung
kurawal dengan benar dan pasang pemeriksaan bahwa metode tetangganya masih ada.
