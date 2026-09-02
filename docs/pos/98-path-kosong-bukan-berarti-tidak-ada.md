# 98 — PATH kosong bukan berarti tidak ada

Tanggal: 2026-09-02

Tiga kali saya menulis kalimat ini, di dokumen dan di pesan commit:

> Mesin ini tidak punya toolchain Dart/Flutter, sehingga `dart analyze` dan
> `flutter test` **tidak dijalankan**.

Kalimat itu salah. Flutter ada di `C:\opt\flutter` — ia hanya tidak berada di
PATH.

## 1. Bagaimana kesimpulannya diambil

Satu perintah:

```bash
for c in dart flutter; do command -v $c; done   # dua-duanya kosong
```

Dari situ langsung disimpulkan "tidak ada", dan kesimpulan itu diulang tanpa
diperiksa lagi di dok. 86, dok. 94, dan dok. 97 B.2 — persis bentuk yang
dikeluhkan dok. 92: **sebuah batas yang dinyatakan, tetapi tidak pernah diukur.**

Yang seharusnya ditanyakan: PATH kosong menjawab "dapatkah dipanggil begitu
saja", bukan "adakah". Satu pencarian berkas menemukannya dalam hitungan detik.

## 2. Akibatnya

Tiga perbaikan sisi klien dikirim ke `main` dan di-push tanpa pernah dianalisis
maupun diuji: dok. 85 (bukti verifikasi biometrik), dok. 86 (persetujuan harga
modal, layar Produk), dok. 94 (persetujuan pra-kirim, entri massal Kulakan).

Sebagai gantinya saya memeriksa keseimbangan kurung dan kecocokan teks assertion
— dan menulis, benar, bahwa itu "bukan pengganti menjalankan ujinya". Itu memang
bukan. Yang tidak saya lakukan adalah memeriksa apakah menjalankannya benar-benar
mustahil.

## 3. Sesudah dijalankan

```
dart analyze produk_screen.dart keranjang_screen.dart kulakan_bulk_entry_screen.dart
   No issues found!

flutter test
   00:02:10 +710: All tests passed!
```

**710 uji lulus**, seluruh suite `apps/ebisnis`, termasuk kedelapan uji yang
ditulis seri dokumen ini. Tidak ada satu pun temuan analisis statis pada ketiga
berkas yang disunting.

Hasilnya kebetulan bersih. Itu tidak membuat cara kerjanya benar — kalau salah
satunya merah, ia sudah berada di `main` selama tiga batch.

## 4. Ujinya dibuktikan dapat merah

Sekarang hal itu benar-benar dapat dibuktikan, bukan diklaim. Baris penanda
dicabut dari `kulakan_bulk_entry_screen.dart`, dan suntingannya diverifikasi
lebih dulu (`pola tersisa = 0` — percobaan pertama gagal menyunting karena jalur
relatif, dan tanpa verifikasi itu "All tests passed" akan terbaca sebagai
kontrol yang lulus padahal tidak ada yang berubah):

```
00:00 +6 -1: entri massal Kulakan penandanya benar-benar ikut terkirim [E]
   Which: does not contain 'if (izinHargaModalTinggi) \'izin_harga_modal_tinggi\': true,'
```

Dipulihkan, `git status` kosong, kembali `+8 All tests passed`.

## 5. Supaya tidak terulang

Letaknya tidak lagi bergantung pada PATH:

* `alat/akar_repo.py` mendapat `flutter_bin()` dan `dart_bin()`, mengikuti pola
  yang sama seperti `repo_pos()` dan `node()`: variabel lingkungan `FLUTTER_ROOT`
  lebih dulu, lalu beberapa letak lazim, lalu menyerah dengan pesan yang
  menyebutkan cara menentukannya.
* `alat/uji-klien.py` menjalankan analisis dan seluruh uji memakai resolusi itu.
  Berkas ini ada khusus supaya tidak ada lagi kesempatan menyimpulkan
  "tidak ada toolchain" dari PATH yang kosong.

Dok. 86, 94, dan 97 dikoreksi di tempatnya, bukan diam-diam diperbaiki.

## 6. Yang dipelajari

**Ketiadaan bukti bukan bukti ketiadaan — dan itu berlaku juga untuk alat.**
Seri dokumen ini berulang kali menemukan penjaga yang hijau karena hanya melihat
sebagian (dok. 90, 92, 93). Kali ini yang melihat sebagian adalah saya, dengan
satu perintah, dan hasilnya diperlakukan sebagai fakta selama tiga batch.

**Batas yang menguntungkan diri sendiri paling jarang diperiksa.** "Tidak ada
toolchain" bukan sekadar kesimpulan yang salah; ia juga kesimpulan yang
membebaskan saya dari menjalankan uji. Justru batas berbentuk itu yang paling
layak dicurigai — bukan karena niatnya, melainkan karena tidak ada gesekan yang
memaksa memeriksanya.
