# Jebakan UAT: `flutter build windows` mewarisi `--dart-define` basi dari build sebelumnya

Ditemukan saat memulai UAT modul keuangan/akuntansi POS Desktop (varian eBisnis polos).
Dicatat karena `windows/variant.cmake` **sudah** memuat penjaga untuk gejala persis ini, dan
penjaga itu tidak menutup akar masalahnya.

---

## 1. Gejala

Perintah yang dijalankan:

```
flutter build windows --release -t lib/main.dart
```

Tanpa `--dart-define` apa pun — resep yang sama dengan `tool/build_all_non_albahjah.ps1` untuk
varian `ebisnis` (`Define=''`). Build **berhasil**, tapi executable yang dihasilkan menampilkan
judul jendela **"Al-Bahjah POS"**, bukan "eBisnis", dan layarnya kosong putih (bukan layar
Kasir/Login yang seharusnya).

## 2. Akar masalah

```
$ grep DART_DEFINES windows/flutter/ephemeral/generated_config.cmake
  "DART_DEFINES=RUJJU05JU19WQVJJQU5UPWFsYmFoamFo"
```

Base64 itu terurai menjadi `EBISNIS_VARIANT=albahjah` — dart-define dari build **Al-Bahjah**
yang pernah dijalankan di mesin ini sebelumnya, bukan dari perintah yang baru saja dijalankan.
Mesin ini dipakai bersama beberapa sesi/proyek; `.dart_tool/flutter_build/` menyimpan cache
fingerprint per-target, dan untuk target `-t lib/main.dart` yang sama (dipakai baik oleh varian
`ebisnis` maupun — pada rilis lain — kombinasi target+define yang berbeda), Flutter tidak selalu
menghitung ulang fingerprint saat hanya `--dart-define` yang berubah antar sesi kerja berbeda.

`windows/variant.cmake` sendiri **sudah menyebut gejala ini secara eksplisit**:

> DART_DEFINES berubah setiap kali varian dibangun. Tanpa dependency ini, CMake dapat
> mempertahankan define/resource ProductName dari build sebelumnya (mis. Al-Bahjah) walaupun
> app.so sudah dibangun sebagai eBisnis.

Penjaganya menambahkan `CMAKE_CONFIGURE_DEPENDS` pada `generated_config.cmake` supaya CMake
mengonfigurasi ulang saat berkas itu berubah — tapi berkas itu sendiri **sudah** salah isi
sebelum CMake sempat membacanya, karena Flutter tools yang menuliskannya kembali memakai
dart-define basi. Jadi penjaga ini menutup lapisan CMake, bukan lapisan Flutter tools
di atasnya.

## 3. Cara memulihkan

```
flutter clean
flutter build windows --release -t lib/main.dart
```

`flutter clean` membuang `build/`, `.dart_tool/`, dan `ephemeral/` — termasuk fingerprint cache
yang menyimpan dart-define basi. Build bersih menghasilkan `generated_config.cmake` yang benar
dan executable berjudul "eBisnis" dengan layar Login yang semestinya.

**Biayanya nyata**: build bersih Windows release memakan waktu jauh lebih lama daripada build
inkremental (compile C++ penuh, bukan hanya kernel Dart) — di mesin bersama ini terukur berjalan
belasan menit sebelum selesai, dibanding puluhan detik untuk build inkremental yang lolos gejala
ini.

## 4. Yang perlu diperiksa sebelum UAT varian mana pun

Sebelum menganggap sebuah build merepresentasikan varian yang dimaksud:

1. Jalankan aplikasinya dan **baca judul jendela** — jangan asumsikan dari nama perintah build.
2. Bila mesin ini baru saja dipakai membangun varian lain (Al-Bahjah, Nahl, Petra, dst.), curigai
   `windows/flutter/ephemeral/generated_config.cmake` lebih dulu:
   `grep DART_DEFINES windows/flutter/ephemeral/generated_config.cmake` — bandingkan hasil
   base64-nya dengan varian yang sedang dibangun sebelum menghabiskan waktu menjalankan build
   yang keliru.
3. Kalau ragu, `flutter clean` sebelum build varian apa pun adalah jaminan paling murah
   dibanding menemukan salah varian setelah build "berhasil".

Ditemukan dan dipulihkan dengan cara ini pada sesi yang sama; UAT dilanjutkan sesudahnya.
