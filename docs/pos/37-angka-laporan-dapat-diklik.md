# Setiap angka di Laporan-Laporan dapat diklik

Permintaan 22-08-2026: seluruh angka pada menu **Laporan-Laporan** dapat diklik dan
memunculkan popup asal-usulnya, di **keempat kanal** — ZKoss, JSP, POS Desktop, dan
Android.

## Keadaan sebelum pekerjaan ini

Sel data sudah dapat diklik di ketiga kanal, memakai satu sumber query bersama
(`LaporanRincianTransaksiUtil`, aksi `laporan_rincian_transaksi`). Yang tertinggal adalah
angka **agregat**:

| Kanal | sel data | subtotal grup | grand total |
|---|:--:|:--:|:--:|
| JSP (`laporan_laporan.jsp`) | ✅ | ✅ `data-grup` | ✅ `data-total` |
| ZKoss (`LaporanKantinZkPanel`) | ✅ | ❌ | ❌ |
| Desktop & Android (`laporan_detail_screen.dart`) | ✅ | ❌ | ❌ |

Justru angka agregat itulah yang paling sering dipertanyakan — "dari mana total 20 juta
ini?" — sehingga celahnya terasa persis di tempat yang paling penting.

## Yang dikerjakan

**Desktop & Android.** Baris subtotal dan grand total kini memakai sel yang sama dengan
sel data (`_SelAngkaRincian`), membuka `_bukaPenyusun(...)`: sebuah popup berisi
baris-baris yang dijumlahkan menjadi angka itu, seluruh kolomnya, dengan kolom yang sedang
ditelusuri ditebalkan, ditutup baris `TOTAL (<nama kolom>)` dan keterangan "N baris
penyusun".

**ZKoss.** `barisSubtotal(...)` menerima daftar baris penyusunnya dan merender tautan
`a.lkzk-num` untuk tiap sel angka. Isinya memakai format pasangan `"Label: Nilai"` yang
**sudah dipahami skrip popup ZK** — jadi tidak perlu mesin render kedua: tiap baris
penyusun menjadi satu pasangan, dan totalnya masuk ke bagian catatan setelah `"  ||  "`.
Baris penyusun dibatasi 200 baris per popup supaya atribut HTML-nya tidak membengkak;
bila terpotong, keterangannya menyebutkan berapa yang ditampilkan.

**JSP.** Tidak diubah — sudah benar sejak awal, dan justru menjadi acuan bentuk popup di
dua kanal lainnya.

## Kenapa masih ada kolom angka yang tidak dapat diklik

Klik-untuk-popup digerbangi **tipe kolom dari peladen**: hanya sel bertipe `num` yang
menjadi tautan, di semua kanal. Penyisiran seluruh 3.395 baris `LaporanKantinUtil`
(721 deklarasi kolom) menemukan hanya tiga kolom berlabel "angka" yang bertipe `text`,
dan ketiganya memang seharusnya begitu:

- **Lembar Penghitungan Stok** — kolom *Hitungan Fisik* dan *Selisih* memang dipilih
  kosong oleh SQL-nya (`select ..., '' , ''`). Lembar itu untuk dicetak lalu diisi tangan;
  tidak ada angka yang bisa ditelusuri.
- **Serapan Anggaran** — kolom *% Serap* dikembalikan SQL sebagai TEKS berakhiran `%`
  (atau `-` bila pagunya nol). Menjadikannya `num` akan merusak tampilannya sekaligus
  membuat subtotal/grand total **menjumlahkan persen**, yang tidak berarti apa-apa.
  Penelusuran baris itu tetap tersedia lewat kolom Anggaran/Realisasi/Sisa di baris yang
  sama, yang memang bertipe `num`.

Artinya tidak ada kolom angka yang tertinggal karena kelalaian. Bila kelak persentase
benar-benar perlu dapat diklik, jalan yang benar adalah menambah tipe kolom baru — "angka,
boleh diklik, JANGAN dijumlahkan" — bukan menyalahartikannya sebagai `num`.

## Catatan hasil memeriksa ulang

**Ukuran popup TIDAK bermasalah di ponsel — dugaan yang dibatalkan.** Sempat disimpulkan
bahwa lebar tetap 860 dan tinggi 460 akan meluber di Android, lalu ukurannya dibatasi
terhadap ukuran layar. Dugaan itu **salah** dan sudah dibatalkan kembali setelah diukur:
pada layar 360x640, `AlertDialog` berisi `SizedBox(width: 860, height: 460)` sebenarnya
dirender **232x392** dengan **nol galat render** — `SizedBox` menegakkan ukurannya
terhadap batasan induk (`constraints.enforce`), jadi Flutter memang sudah memotongnya
sendiri.

Pelajarannya: ukuran tetap di dalam dialog itu **dipotong, bukan meluber**. Menambah
pembatas `MediaQuery` di 219 titik yang memakai pola ini hanya akan menambah kode tanpa
memperbaiki apa pun. Bila kelak benar-benar ada dialog yang meluber, penyebabnya bukan
angka tetapnya melainkan induk tanpa batasan (mis. berada di dalam scroll mendatar) --
dan itu yang harus diperiksa, bukan angkanya.

**Label baris tidak boleh membelah popup ZK.** Skrip popup ZKoss memecah isinya dengan
`" | "` (antar pasangan) dan `": "` (label vs nilai). Untuk sel data, isinya adalah label
KOLOM sehingga aman. Untuk baris penyusun, isinya label BARIS — nama produk atau uraian
milik pengguna, yang boleh saja memuat kedua pemisah itu; satu nama bisa terbelah menjadi
dua baris popup atau nilainya tampak berpindah kolom. Teksnya kini dibersihkan lebih dulu
(`tanpaPemisah`).

## Sapuan yang tidak menemukan apa-apa

Dicatat supaya tidak diulang orang berikutnya: seluruh **15 layar** yang menerima
`hak` dari peladen memakai **setiap** kunci yang dikirimkan helper-nya — termasuk
`approve` dan `reject` pada dua belas modul Keuangan. Tidak ada tombol persetujuan yang
lolos gerbang.

Catatan metode: audit pertama melaporkan "hanya satu layar memakai hak" — itu artefak
pengukuran, bukan temuan. Helper `_boleh()` ada dalam dua bentuk (satu argumen dan dua
argumen) dan pola pencarian pertama hanya mengenali salah satunya.

## Penjaga

`test/laporan_angka_diklik_test.dart` pada repositori Flutter mengunci: sel data membuka
`_bukaRincianBaris`, subtotal dan grand total membuka `_bukaPenyusun`, popup-nya menyebut
jumlah baris penyusun dan totalnya, serta ZKoss memakai `penyusunRingkas` yang sama. Uji
paritas ZK dilewati dengan sopan bila working copy AIS tidak tersedia (mis. di CI Flutter
yang berdiri sendiri).
