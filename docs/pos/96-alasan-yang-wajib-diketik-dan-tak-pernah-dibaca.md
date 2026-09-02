# 96 — Alasan yang wajib diketik, lalu tidak pernah dibaca

Tanggal: 2026-09-02

Dok. 95 menemukan lima kolom jenis anggota koperasi yang dapat disetel admin dan
tidak dibaca kode mana pun, tetapi menutup tanpa alat: pertanyaan "apakah
nilainya **dibandingkan**?" terbukti tidak dapat dijawab dengan andal.

Pertanyaan yang lebih sempit ternyata bisa.

## 1. Pertanyaan yang mekanis, bukan heuristik

    setter-nya dipanggil dari luar direktori model  (admin dapat menyimpannya)
    getter-nya TIDAK PERNAH dipanggil, di mana pun

Kolom seperti itu tidak mungkin dibaca lewat mekanisme apa pun. Tidak ada
"mungkin nilainya singgah di array dulu" — tidak ada pembacaan sama sekali.

Empat jalur baca wajib dikecualikan, dan yang keempat hampir membuat seluruh
pengukuran ini menjadi tuduhan massal:

| Jalur | Contoh |
|---|---|
| getter di Java lain | `x.getFoo()` |
| nama properti di JSP/ZUL atau Dart | `${item.foo}`, `'foo'` |
| nama properti di HQL/Criteria | `Restrictions.eq("foo", …)` |
| **nama kolom snake_case di SQL native** | `SELECT … sesi_kas_kasir …` |

Basis kode ini banyak memakai SQL native. Tanpa saringan keempat,
`sesiKasKasir` dituduh mati padahal `sesi_kas_kasir` muncul **47 kali** di kueri
native. Saringan itu sendirian menurunkan **41 kandidat menjadi 18**.

## 2. Kluster pertama: justifikasi yang tidak pernah ditampilkan

Tiga entitas menyimpan pasangan `alasanReversal` + `reversalDari`:
`NotaSalesBiaya`, `PembayaranHutangSupplier`, `PenerimaanPiutangCustomer`.

Alasannya **wajib**. Server menolak reversal tanpa alasan:

```java
String alasan = request.optString("alasan", "").trim();
if (id == null || alasan.isEmpty()) {
    tolak(hasil, "pembayaran_id dan alasan wajib diisi.");
    return;
}
```

Dan hanya Pemilik/Admin yang boleh melakukannya. Klien menyediakan kolom
ketiknya:

```dart
// hutang_supplier_screen.dart
'alasan': alasan.text.trim(),
```

Nilainya disimpan lewat `asal.setAlasanReversal(alasan)`. Lalu **tidak ada satu
pun tempat yang membacanya kembali** — bukan Java, bukan JSP, bukan Dart, bukan
SQL native. Hal yang sama berlaku untuk `reversalDari`, tautan ke dokumen yang
dibatalkan.

Jadi: seorang Pemilik diminta berhenti, memikirkan, dan mengetikkan alasan
sebelum membatalkan pembayaran supplier. Sistem menolak melanjutkan tanpa itu.
Kalimat yang diketiknya masuk ke basis data dan tidak pernah muncul di layar
mana pun — dan orang yang melihat sebuah reversal tidak dapat mengetahui
dokumen mana yang dibatalkannya.

Ini bentuk paling tajam dari "biaya tanpa manfaat" yang berulang sejak dok. 45.
Yang terbuang di sini bukan satu field JSON, melainkan **usaha manusia yang
diminta secara paksa**.

## 3. Kluster kedua: ringkasan tutup trip

`SalesInventoryTripHelper` menghitung tiga angka saat menutup sesi trip:

```java
sesi.setTotalPenerimaanTunai(...);
sesi.setTotalPenerimaanNonTunai(...);
sesi.setTotalPembayaranPembelian(...);
```

Ketiganya tidak pernah dibaca kembali. Layar mana pun yang menampilkan angka
serupa menghitungnya ulang.

Ini konsisten dengan temuan dok. 92: mode `--luas` di sana juga menandai
`penerimaanNonTunai` yang dikirim `SalesInventoryTripHelper` sebagai field tanpa
pembaca. Dua alat yang berbeda menunjuk sudut yang sama dari kode yang sama.

## 4. Alatnya melapor, tidak memvonis

`alat/kolom-hanya-ditulis.py` mengeluarkan 18 kandidat dan **selalu** keluar
dengan kode 0. Enam di antaranya (dua kluster di atas) sudah ditelusuri tangan;
dua belas sisanya belum.

Membekukan yang dua belas sebagai "utang" akan mengulangi kesalahan yang
dihindari dok. 92: daftar utang yang tiap barisnya beralasan berubah menjadi
daftar bungkam. Jadi tidak dibekukan, tidak digerbangkan — dilaporkan.

## 5. Keputusannya

Untuk kluster reversal ada dua pilihan, dan keduanya sah:

* **Tampilkan.** Alasan dan tautan dokumen asal muncul di layar riwayat/detail.
  Ini menambah field respons baru, jadi klien harus benar-benar membacanya —
  kalau tidak, `field-tanpa-pembaca.py` akan menandainya, dan memang seharusnya.
* **Berhenti memintanya.** Kalau memang tidak ada yang perlu melihatnya, jangan
  mewajibkan seorang Pemilik mengetiknya.

Yang tidak boleh adalah membiarkannya seperti sekarang: menuntut penjelasan,
lalu membuangnya ke laci yang tidak pernah dibuka.

## 6. Yang dipelajari

**Mempersempit pertanyaan mengubah yang tak terjawab menjadi terjawab.** Dok. 95
gagal pada "apakah dibandingkan?" dan menyimpulkan tidak ada penjaga yang mungkin.
Yang sebenarnya salah bukan gagasannya, melainkan lebar pertanyaannya. "Apakah
pernah dibaca?" jauh lebih miskin — dan justru karena itu dapat dijawab tanpa
menebak.

**Biaya sebuah cacat tidak selalu ditanggung mesin.** Field JSON yatim membuang
byte. Kolom ini membuang perhatian seorang Pemilik, setiap kali sebuah
pembayaran dibatalkan, sambil menjanjikan jejak audit yang tidak ada.
