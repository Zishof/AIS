# UAT: otomasi klik tidak bisa jalan di lingkungan sesi ini — dua jalur, dua bukti keras

Permintaan: UAT alur posting (Jurnal Umum, pembelian, penjualan) sampai laporan keuangan,
sambil menangkap layar tiap langkah untuk disusun jadi manual pengguna. Dokumen ini mencatat
kenapa penangkapan **interaktif** (klik-lalu-tangkap) tidak bisa dilakukan di sesi ini, dengan
bukti — bukan dugaan — supaya sesi berikutnya tidak mengulang jalan yang sama.

---

## 1. Build Windows: render bekerja, klik tidak pernah sampai

Setelah memperbaiki jebakan `--dart-define` basi (doc 109), aplikasi varian eBisnis berjalan
benar: masuk otomatis ke `ebisnis.id` produksi, menampilkan seluruh grup menu termasuk
KEUANGAN dan TRANSAKSI & LAPORAN. `PrintWindow` (flag `PW_RENDERFULLCONTENT`) menangkap
tangkapan layar yang sempurna — jendela benar-benar dirender oleh DWM.

Tiga jalur klik dicoba, berurutan, masing-masing diverifikasi gagal dengan diagnostik
langsung (bukan diasumsikan dari tidak-ada-perubahan visual saja):

1. **`PostMessage` (WM_LBUTTONDOWN/UP) ke jendela anak `FLUTTERVIEW`.** Diuji pada tombol
   toolbar biasa ("Tampilan Normal") — bukan hanya `ExpansionTile` sidebar yang mungkin
   punya hit-test rumit. Nihil. Permukaan Flutter Windows tidak memproses pesan tetikus yang
   di-*post* manual.
2. **`SendInput`/`mouse_event` (input perangkat-keras tiruan) + `AttachThreadInput` +
   `SetForegroundWindow`, termasuk trik "tekan Alt sekilas" yang biasa membuka kunci
   foreground.** Diagnostik langsung: `GetForegroundWindow()` mengembalikan **`0`** —
   *tidak ada jendela foreground sama sekali* di sesi tempat proses PowerShell ini berjalan,
   sebelum maupun sesudah setiap percobaan. `AttachThreadInput` ke thread ID `0` otomatis
   gagal (parameter tidak valid), dan itulah sebabnya seluruh rantai berikutnya gagal.
3. **UI Automation (pohon aksesibilitas Windows).** `AutomationElement.FromHandle` +
   `FindAll(Descendants)` hanya mengembalikan **1 elemen** (jendela itu sendiri) meski dicoba
   ulang berkali-kali dengan jeda — pohon semantik Flutter tidak pernah aktif.

Kesimpulan: sesi ini tidak punya kanal interaksi apa pun ke jendela GUI native, meski
kanal *render* (baca) bekerja sempurna. Ini bukan bug skrip — tiga mekanisme independen
gagal dengan pola yang sama.

## 2. Build web: gagal kompilasi, bukan gagal jalan

Sebagai jalan pintas, dicoba `flutter build web -t lib/main.dart` supaya bisa dikendalikan
lewat Browser pane (yang benar-benar bisa mengklik). Gagal di tahap **kompilasi**, bukan saat
dijalankan:

```
../../packages/core_hw/lib/src/buka_laci.dart:1:8:
Error: Dart library 'dart:ffi' is not available on this platform.
import 'dart:ffi';
```

`core_hw` — paket akses perangkat keras (laci kas, printer) lewat `win32`/`ffi` — diimpor
tanpa syarat dari `main.dart`. `dart:ffi` tidak ada di web, jadi seluruh aplikasi gagal
dikompilasi untuk target itu. Ini bukan konfigurasi yang lupa dinyalakan; ini ketidakcocokan
platform yang nyata, dan membetulkannya berarti menyuntikkan impor bersyarat
(`dart.library.io` vs `dart.library.html`) di seluruh jalur yang menyentuh perangkat keras —
pekerjaan tersendiri, bukan langkah UAT.

## 3. Yang tetap terverifikasi meski tanpa klik

- Build Windows varian eBisnis **benar** (setelah perbaikan doc 109): versi 1.34.20 (build
  182), masuk otomatis, seluruh grup menu tampil termasuk yang jadi fokus UAT ini.
- Katalog laporan tetap bersih: `entri tautan ZK dilewati: 1`, `kunci tanpa pelaksana: 0` —
  regresi apa pun sejak doc 102 akan langsung ketahuan dari sini, dan tidak ada.
- Struktur tiap layar (medan, validasi, aksi API, urutan tombol) dipastikan dari **pembacaan
  kode sumber langsung**, bukan tangkapan layar — lebih tahan terhadap perubahan versi
  daripada tangkapan layar satu waktu, dan diverifikasi lewat sitasi baris seperti seluruh
  temuan lain di seri dokumen ini.

## 4. Jalan yang diambil sebagai gantinya

Karena tangkapan layar interaktif tidak bisa diperoleh di sesi ini, manualnya disusun dengan
teknik yang sudah terbukti di seri dokumen ini: **ilustrasi tata letak presisi**, bukan
tangkapan layar sungguhan — nama kolom, label tombol, dan pesan validasi diambil apa adanya
dari kode sumber (Dart untuk layar, Java untuk katalog laporan), digambar sebagai maket layar,
dan ditandai eksplisit sebagai ilustrasi supaya tidak dikira tangkapan layar asli. Pola yang
sama dipakai `docs/pos/70-panduan-laporan-keuangan-an-nahl.pdf`.

## 5. Untuk sesi berikutnya

Bila UAT interaktif sungguhan dibutuhkan: jalur yang mungkin berhasil adalah mengendalikan
mesin ini lewat **vnc.ebisnis.id** (lihat memori `vnc-ebisnis-remote-access`) — itu
tersambung ke sesi desktop yang *benar-benar* interaktif, berbeda dari sesi tempat shell agen
ini berjalan. Klik dari dalam sesi agen, seberapa pun teknik yang dicoba, tidak akan sampai
ke jendela yang dirender di sesi lain.
