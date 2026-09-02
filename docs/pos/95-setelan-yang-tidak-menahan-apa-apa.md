# 95 — Setelan yang tidak menahan apa-apa

Tanggal: 2026-09-02

Dok. 93 membuat aplikasi `ecanteen` terlihat oleh alat untuk pertama kalinya —
aplikasi kedua di repositori POS, yang sebelumnya berada di luar korpus setiap
pemindai. Memeriksanya menemukan sesuatu yang tidak dicari.

## 1. Janji di layar admin

Layar **Jenis Anggota Koperasi** punya satu isian:

> **Minimal Saldo Mengendap**
> *Batas saldo minimum yang tertahan dan tidak dapat digunakan untuk transaksi.*

Kalimat itu adalah janji kepada administrator koperasi. Nilainya:

* dapat disetel di **dua** antarmuka — JSP `jenis_anggota_koperasi.jsp` dan
  Flutter `tab_jenis_member.dart`;
* disimpan di `koperasi.jenis_anggota_koperasi.minimal_saldo`;
* dikirim ke klien lewat `KantinMemberApi` (`minimal_saldo`) dan `KantinHelper`
  (`minSaldo`).

Dan tidak ditegakkan di mana pun di server.

`getMinimalSaldo()` dipakai **tiga kali** di seluruh lapisan servlet: dua kali
menaruhnya ke respons, sekali lagi membacanya dari basis data. Nol kali
dibandingkan dengan apa pun.

## 2. Satu-satunya yang menegakkannya adalah layar yang paling tidak penting

Pencarian di kedua repositori klien menemukan tepat satu penegak:

```dart
// apps/ecanteen/lib/screens/bayar_qr_screen.dart
if ((saldo - nominal) < Sesi.instance.minimalSaldo) {
  throw ApiException(
      'Sisa saldo setelah pembayaran kurang dari batas saldo mengendap '
      'yang diizinkan.');
}
```

Yaitu layar bayar-QR **milik anggota sendiri**. Aplikasi kasir `ebisnis` memuat
`minimalSaldo` hanya di form admin untuk menyetelnya — bukan saat membayar.
`KantinHelper.bayar` (jalur kasir) tidak memeriksanya. `TopupHelper.bayarOnline`
memeriksa "Saldo tidak mencukupi", tetapi tidak batas mengendapnya.

Jadi lantainya berlaku persis di satu tempat: ketika anggota membayar sendiri
lewat QR di aplikasinya sendiri. Di kasir — kanal utama — ia diabaikan
seluruhnya, dan aplikasi ecanteen versi lama pun melewatinya.

Bentuknya sama dengan dok. 89, dibalik: di sana gerbang yang tidak pernah
menutup; di sini setelan yang tidak pernah menahan. Keduanya **tampak** bekerja.

## 3. Saudara-saudaranya

Dua belas kolom berbau batas diperiksa pada model koperasi. Hasilnya bercampur,
dan campurannya penting:

| Kolom | Keadaan |
|---|---|
| `maksimal_transaksi_harian` / `mingguan` / `bulanan` | **ditegakkan** — masuk `double[] batas` lalu dibandingkan di loop `berjalan + transaksiIni > batas[i]` |
| `maksimal_boleh_utang` | dipakai di jalur checkout |
| `minimal_saldo` | **hanya dikirim** |
| `limit_kredit` | **hanya dikirim** (`j.put`) |
| `limit_penagihan`, `maksimal_pelanggaran`, `maksimal_potongan`, `target_bulanan`, `target_frekuensi_belanja` | **tidak dipakai sama sekali** — nol pemakaian getter-nya |

Dua di antara yang terakhir punya isian sendiri di layar admin:
`targetFrekuensiBelanja` (input "Target Belanja") dan `maksimalPelanggaran`
(input "Maks SP"). Administrator mengisinya, nilainya tersimpan, dan tidak ada
satu baris kode pun yang pernah membacanya lagi.

## 4. Pengukuran pertama saya salah, dan ini kali kedua

Percobaan pertama memakai heuristik: getter yang muncul dalam ~120 karakter dari
sebuah operator relasional dianggap "dibandingkan". Hasilnya **12 dari 12
dinyatakan tidak ditegakkan** — termasuk `maksimal_transaksi_harian`, yang
ternyata ditegakkan dengan rapi:

```java
double[] batas = new double[] { tipe.getMaksimalTransaksiHarian(), ... };
...
if (berjalan + transaksiIni > batas[i] + 0.5) { ... }
```

Nilainya disalin ke array lebih dulu, sehingga perbandingannya tidak pernah
dekat dengan getter-nya. Ini persis kesalahan dok. 88: **kedekatan bukan
sebab-akibat.** Angka 12/12 itu tidak pernah dilaporkan ke mana pun karena
diperiksa tangan lebih dulu — tetapi ia sudah siap menjadi tuduhan massal atas
mekanisme yang bekerja dengan benar.

Karena itu tidak ada penjaga baru yang dibuat di sini. Bentuk "batas yang
ditegakkan" terlalu beragam untuk dijaring dengan andal: array, variabel
perantara, perbandingan di method lain. Yang tertinggal adalah tabel di atas,
hasil pemeriksaan tangan atas dua belas kolom.

## 5. Keputusan yang bukan milik saya

Menegakkan `minimal_saldo` di server berarti **mulai menolak pembayaran yang
hari ini berhasil**. Kalau ada jenis anggota dengan lantai bukan nol dan
anggotanya rutin berbelanja di bawahnya, menyalakannya bisa menghentikan
penjualan di kasir pada hari pertama.

Menilai risiko itu menuntut melihat data produksi — berapa jenis anggota yang
lantainya bukan nol, dan berapa sering saldo turun di bawahnya. Basis data UAT
tidak dapat diakses dari sini (kredensial ditolak, dok. 82), jadi saya tidak
menyalakannya sendiri.

Tempatnya kalau diputuskan menyalakan: `TopupHelper.bayarOnline` di samping
pemeriksaan "Saldo tidak mencukupi" yang sudah ada, dan `KantinHelper.bayar`
untuk jalur kasir. Keduanya perlu, karena melindungi satu jalur saja persis
keadaan hari ini.

Pilihan yang lain sama sahnya: **hapus isiannya dari layar admin**. Setelan yang
tidak menahan apa-apa lebih buruk daripada tidak ada setelan sama sekali, karena
administrator mengira lantainya berlaku.

## 6. Yang dipelajari

**Deskripsi setelan adalah janji, sama seperti pesan galat.** Dok. 86
menemukannya pada pesan penolakan; di sini pada teks bantuan di bawah sebuah
isian. Keduanya kalimat yang dibaca manusia, dan keduanya tidak dikompilasi,
tidak diuji, tidak dijaga.

**Penegakan sebagian lebih menyesatkan daripada tidak ada penegakan.** Kalau
lantai itu tidak pernah bekerja di mana pun, seseorang akan cepat mengeluh. Yang
terjadi justru ia bekerja di satu layar — cukup untuk membuat siapa pun yang
mengujinya di sana menyimpulkan bahwa setelannya berfungsi.
