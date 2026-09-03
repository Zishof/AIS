# Local-first modul keuangan: diperiksa, dan penjaganya ditambal

Batch lanjutan sesudah doc 98.

---

## 1. Pertanyaannya, dan aturan yang menjawabnya

"Pastikan semua sudah local-first" tidak dapat dijawab dengan menyapu kode begitu saja —
sebagian aksi memang **sengaja** tetap daring. Aturannya sudah tertulis di
[33-audit-lokal-dulu.md](33-audit-lokal-dulu.md):

- **Seluruh modul Keuangan** termasuk yang dikonversi menjadi lokal-dulu.
- **Seluruh posting akuntansi tetap daring**, karena mengantre lalu memposting dapat masuk
  **periode yang salah**, atau setelah buku ditutup.
- Sepuluh aksi lain juga dikecualikan: kredensial (`ebisnis_role_menu_simpan`,
  `pedagang_ubah`), yang butuh id server seketika (`produk_simpan`, `hotel_tamu_simpan`),
  `si_coa_save` (akun yang datang belakangan membuat jurnal mengacu akun tak dikenal),
  `batalkan_transaksi`, `saldo_awal_simpan`, `sinkron_referensi`, `layar_pelanggan_kirim`,
  `produk_duplikat_hapus`, dan `mutasi_stok_simpan`.

Jadi yang diperiksa bukan "apakah semuanya lokal-dulu", melainkan **apakah batasnya masih
sesuai aturan itu**.

## 2. Hasil: batasnya benar

Setiap aksi yang seharusnya daring diperiksa di situs panggilnya — apakah ia lewat
`ApiClient.instance.aksi(...)` (langsung) atau `MasterOffline` (antrean):

| Aksi | Hasil |
|---|---|
| `ebisnis_role_menu_simpan`, `produk_simpan`, `hotel_tamu_simpan` | daring langsung |
| `si_coa_save`, `batalkan_transaksi`, `sinkron_referensi` | daring langsung |
| `layar_pelanggan_kirim`, `produk_duplikat_hapus`, `mutasi_stok_simpan` | daring langsung |
| `saldo_awal_simpan` | tidak ada di aplikasi Flutter (jalur ZK/web) |

Sembilan dari sembilan yang ada benar. **Tidak ada kode aplikasi yang perlu diubah.**

## 3. Yang kurang: penjaganya, bukan kodenya

Doc 33 menulis daftar itu "**Dikunci oleh** `test/master_offline_kontrak_test.dart`".

Peta `tetapOnline` di berkas itu hanya memuat **tiga** entri: `hak_akses_screen`,
`kulakan_bulk_entry_screen`, dan `resepsionis_hotel_screen`.

Enam aksi sisanya — termasuk `si_coa_save` yang alasannya justru paling erat dengan
akuntansi, dan `batalkan_transaksi` yang menyentuh uang — **benar dalam praktik tetapi tidak
dijaga tes apa pun**. Mengubah salah satunya menjadi lokal-dulu tidak akan membuat satu tes
pun merah, dan dokumen yang menjanjikan penguncian itu justru membuat orang berhenti
memeriksa.

Tujuh entri ditambahkan (`batalkan_transaksi` muncul di dua layar), masing-masing diperiksa
lebih dulu memenuhi asersi yang sama dengan yang sudah ada — `ApiClient.instance` dalam 200
karakter sebelum nama aksi — supaya tidak ada entri yang ditambahkan lalu gagal.

```
flutter test test/master_offline_kontrak_test.dart
00:00 +8: All tests passed!
```

Kontraknya kini mengunci **sepuluh** aksi, bukan tiga. Commit `602aecb` di
`apotik-uiux`, branch `feature/apotik-modern-uiux`.

## 4. Kenapa ini pola yang layak dicurigai

Ini kali kedua dalam rangkaian batch ini sebuah dokumen menyatakan sesuatu "dikunci" atau
"diverifikasi" lebih kuat daripada kenyataannya. Yang pertama: `MenuSnapshotData` menyebut
dirinya "daftar menu LENGKAP" padahal 155 grup induknya tidak ada di dalamnya (doc 95).

Keduanya sama-sama tidak berbohong tentang kode — kodenya memang benar. Yang dilebihkan
adalah **seberapa terjaganya**. Dan klaim semacam itu lebih berbahaya daripada tidak ada
klaim sama sekali: ia memberi alasan untuk tidak memeriksa.
