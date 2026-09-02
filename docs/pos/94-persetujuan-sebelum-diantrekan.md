# 94 — Persetujuan diminta sebelum apa pun diantrekan

Tanggal: 2026-09-02

Utang jalur terakhir dari dok. 86 dan 91 dilunasi: entri massal Kulakan kini
menawarkan persetujuan harga modal tinggi yang selama ini dijanjikan pesan
server tetapi tidak pernah dapat diberikan.

## 1. Ternyata lebih buruk daripada yang tercatat

Dok. 86 paragraf 5 menulis bahwa layar itu "masih menampilkan penolakan tanpa
menawarkan persetujuan". Membaca kodenya menunjukkan sesuatu yang lain:

```dart
} on ApiException catch (e) {
  if (!e.offline) rethrow; // penolakan bisnis: posting berhenti.
  produkId = idLokal;
}
```

Penolakan bisnis **melempar keluar dari loop**. Satu baris bermasalah dari tiga
puluh membunuh **seluruh posting faktur** — bukan sekadar tidak menawarkan jalan
keluar, melainkan membatalkan pekerjaan yang sudah diketik seluruhnya.

Untuk barang promo rugi atau klaim garansi, entri massal Kulakan sama sekali
tidak dapat diselesaikan.

## 2. Bentuknya berbeda dari layar Produk, dan itu disengaja

Di `produk_screen.dart` (dok. 86) perbaikannya berupa tangkap-lalu-ulang:
tangkap `ApiException`, tawarkan persetujuan, kirim ulang dengan penanda.

Bentuk itu **tidak boleh** dipakai di sini. Penolakannya melempar dari dalam
loop yang sudah:

* menaruh baris di antrean offline (`MasterOffline.antreLokal`),
* memakai id sementara negatif yang nanti ditukar dengan id server,
* dan menahan flush faktur yang masih menunjuk produk yang belum terkirim.

Mencoba ulang di tengah keadaan itu berarti menyentuh penukaran id dan urutan
flush — risiko merusak outbox yang jauh lebih besar daripada gerbang yang
sedang diperbaiki, di mesin yang bahkan tidak dapat menjalankan `flutter test`.

Jadi persetujuannya diminta **pra-kirim**:

```dart
if (!await _konfirmasiPosting(validasi)) return;
final hargaModalTinggi = _barisHargaModalTinggi;
if (hargaModalTinggi.isNotEmpty &&
    !await _konfirmasiHargaModalTinggi(hargaModalTinggi)) {
  return;
}
```

Saat dialog itu muncul, **belum satu baris pun diantrekan**. Kalau pengguna
memilih "Periksa lagi", tidak ada apa pun yang perlu dibatalkan.

Bentuk pra-kirim juga menjawab pertanyaan yang dua kali saya tunda — "satu
persetujuan untuk seluruh batch, atau per baris?". Pertanyaan itu hanya sulit
selama perbaikannya dibayangkan sebagai coba-ulang. Pra-kirim membuatnya
gugur: satu dialog, tetapi menyebut **setiap baris** beserta angkanya, sehingga
yang disetujui adalah daftar yang benar-benar dilihat — bukan izin buta atas
batch.

## 3. Aturan yang digandakan, dan mengapa itu aman di sini

Syaratnya mencerminkan server persis:

```dart
row.produkId == null &&
row.hargaJualNilai > 0 &&
row.hppUnit > row.hargaJualNilai * 10
```

Menggandakan aturan bisnis ke klien biasanya buruk. Di sini akibat terburuk dari
drift sudah diukur: **kembali ke perilaku hari ini.** Kalau syarat klien meleset,
server tetap menolak seperti sekarang — server tidak pernah berhenti menjadi
pemegang keputusan. Yang ditambahkan hanyalah kesempatan bagi pengguna untuk
melihatnya lebih dulu.

`row.produkId == null` ikut disyaratkan karena hanya produk baru yang melewati
`produk_simpan`; baris yang produknya sudah ada tidak pernah menyentuh gerbang
itu.

## 4. Penjaga yang melaporkan utangnya sendiri lunas

Dok. 91 memasang `UTANG_JALUR` dengan satu entri: layar ini. Sesudah perbaikan,
alatnya sendiri yang memberi tahu:

```
Entri daftar yang sudah tidak berlaku:
* kulakan_bulk_entry_screen.dart / izin_harga_modal_tinggi  <- keluarkan dari daftarnya
```

Entrinya dikeluarkan. Aturan "daftar utang hanya boleh menyusut" bekerja tanpa
seorang pun perlu mengingat isinya — dan itu justru gunanya, karena yang menulis
entri itu dan yang melunasinya bisa saja terpisah berbulan-bulan.

## 5. Ujinya dibuat agar bisa merah

`test/harga_modal_persetujuan_test.dart` mendapat satu grup baru. Satu ujinya
mengikat pada **urutan**, bukan sekadar keberadaan:

```dart
expect(iKonfirmasi, lessThan(iAntre),
    reason: 'persetujuan harus diminta sebelum antreLokal dipanggil');
```

Kalau kelak seseorang memindahkan persetujuannya ke dalam loop — bentuk yang
tampak lebih rapi dan justru berbahaya — uji itu merah. Menegaskan keduanya ada
tidak akan menangkapnya.

## 6. Yang belum diuji

Mesin ini tidak punya toolchain Dart/Flutter; `dart analyze` dan `flutter test`
**tidak dijalankan**. Yang diperiksa: keseimbangan kurung berkas (234/234
kurawal, 1180/1180 kurung, 143/143 siku), `_postingFaktur` tertutup rapi, dan
ketujuh pola yang ditegaskan uji baru benar-benar cocok dengan sumber hasil
suntingan. Itu bukan pengganti menjalankan ujinya.

## 7. Yang dipelajari

**Pertanyaan yang tampak butuh keputusan produk kadang hanya butuh bentuk yang
berbeda.** "Per batch atau per baris?" saya tunda dua kali sebagai keputusan
pengguna. Ia lenyap begitu perbaikannya dipindahkan ke sebelum pengiriman —
bukan dijawab, melainkan tidak lagi perlu ditanyakan.

**Menunda dua kali membuat saya melihat kodenya lebih teliti.** Baru pada
penundaan ketiga saya membaca `if (!e.offline) rethrow;` dan menyadari seluruh
posting mati. Catatan dok. 86 tentang layar ini terlalu ringan selama dua batch.

## 8. Koreksi atas paragraf 6: ujinya sudah dijalankan

Paragraf 6 menyatakan `dart analyze` dan `flutter test` tidak dapat dijalankan
karena mesin ini tidak punya toolchain-nya. **Itu salah** — lihat docs/pos/98.

Sesudah dijalankan: analisis bersih, dan kedelapan uji di
`harga_modal_persetujuan_test.dart` lulus, termasuk uji urutan yang menuntut
konfirmasi mendahului `MasterOffline.antreLokal`.

Ujinya juga dibuktikan **dapat merah**: baris penanda dicabut dari sumber,
suntingannya diverifikasi lebih dulu (`pola tersisa = 0`), hasilnya `+7 -1`
dengan pesan yang menyebut pola yang hilang. Dipulihkan, `git status` kosong,
kembali `+8 All tests passed`.
