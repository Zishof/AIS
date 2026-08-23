# 21 — Hak akses ubah harga

Sebelum ini, siapa pun yang bisa membuka layar Produk bisa mengubah harga jual. Yang
dibangun: izin ubah harga yang bisa dibatasi **per peran** dan **per pengguna**, ditegakkan
di semua jalan masuk — bukan hanya di layar.

## Model data

`ais.database.model.inventory.Toko` (tiga kolom, dibuat Hibernate):

| Kolom | Arti |
|---|---|
| `semuaBolehUbahHarga` | `TRUE` (bawaan bila `null`) = tidak ada pembatasan sama sekali |
| `pedagangBolehUbahHarga` | daftar id pengguna, dipisah koma |
| `roleBolehUbahHarga` | daftar id peran (`Tbmrole`), dipisah koma |

Bawaan `null` sengaja dibaca sebagai **boleh**: toko lama yang belum pernah disentuh tidak
boleh tiba-tiba kehilangan kemampuan yang selama ini dimilikinya.

## Aturan

`ais.action.master.inventory.HargaAksesUtil`:

```java
public static boolean bolehUbahHarga(Toko toko, Tbmuser tbmuser)
public static boolean bolehUbahHarga(Session session, Long tokoId, Tbmuser tbmuser)
public static String  pesanDitolak()
public static boolean berubah(Double lama, double baru)
public static String  normalkan(String csv)
```

Semantiknya **ATAU**, bukan DAN: pengguna boleh bila id-nya terdaftar di daftar pengguna
**atau** perannya terdaftar di daftar peran. Kalau dibuat DAN, memberi izin ke sebuah peran
tidak akan berpengaruh apa-apa sampai tiap anggotanya juga didaftarkan satu per satu —
kebalikan dari gunanya izin per peran.

Pencocokan CSV dilakukan **per elemen setelah dipangkas**, bukan `contains()` pada string
utuh. Tanpa itu, peran `SPV` akan cocok dengan `SPV2`, dan izin bocor ke peran yang tidak
pernah diberi izin. `normalkan()` merapikan CSV (buang spasi dan elemen kosong) supaya
data yang tersimpan tidak bergantung pada cara admin mengetiknya. Pencocokan tidak
membedakan huruf besar-kecil.

`berubah(lama, baru)` memisahkan "menyimpan produk dengan harga yang sama" dari "mengubah
harga". Tanpa pemisahan itu, pengguna tanpa izin harga tidak bisa menyunting nama produk
sekalipun, karena harga ikut terkirim di payload yang sama.

## Semua jalan masuk ditutup

Gerbang dipanggil di enam berkas, karena harga bisa diubah lewat lebih dari satu pintu:

| Berkas | Pintu |
|---|---|
| `ProdukAction`, `GrupProdukAction` | layar ZK |
| `PosApi`, `KantinHelper` | API POS Desktop/Android (`produk_simpan`) |
| `GrupProdukApiHelper` | API grup produk |
| `KantinHelper` (impor Excel) | **impor Excel produk** |

Pintu impor Excel yang paling mudah terlewat: memblokir kolom harga di layar tidak ada
gunanya bila harga yang sama bisa masuk lewat unggahan berkas.

## Sisi klien: label, bukan input yang dimatikan

Widget `AppHargaTerkunci` (`lib/widgets/app_components.dart`). Ketika pengguna tidak
berizin, harga dirender sebagai **teks biasa**, bukan `TextField` ber-`enabled: false`.

Alasannya bukan estetika. Input yang dimatikan tetap sebuah kotak isian: terlihat seperti
sesuatu yang seharusnya bisa diisi, mengundang percobaan, dan menyisakan pertanyaan
"kenapa tidak bisa diketik?". Label tidak menjanjikan apa pun. Penolakan disertai kalimat
`HargaAksesUtil.pesanDitolak()`:

> Anda tidak boleh mengubah harga karena tidak diberikan akses. …

sehingga pengguna tahu ini keputusan admin, bukan aplikasi yang rusak.

## Yang mudah salah

- **Gerbang di klien bukan keamanan.** Klien hanya menyembunyikan; penegakan sebenarnya
  ada di server. Setiap aksi yang menyentuh harga harus memanggil gerbang sendiri —
  termasuk aksi baru yang ditambahkan nanti.
- **`contains()` pada CSV.** Sudah dijelaskan di atas; jangan pernah kembali ke sana.
- **Kasus "tidak berubah".** Menyamakan `Double` dengan `==` pada nilai pecahan bisa
  menganggap harga berubah padahal tidak; `berubah()` yang menentukan, bukan pembandingan
  langsung di tempat pemanggilan.

## Uji

21 dari 21 kombinasi lulus: matriks izin (peran saja, pengguna saja, keduanya, tidak
sama sekali), substring `SPV2` vs `SPV`, perbedaan huruf besar-kecil, CSV berspasi, dan
kasus harga tidak berubah. Sisi klien diuji terpisah bahwa harga benar-benar dirender
sebagai teks — bukan `TextField` yang dimatikan (lihat [28](28-harness-uji-dan-temuan.md)).
