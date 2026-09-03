# Layar Kelompok Aset natif — satu `.zul` lagi lepas

Lanjutan doc 104. Layar Flutter-nya sudah ada, jadi "Sesuaikan Akun Posting" tidak lagi
melempar pengguna ke browser untuk Kelompok Aset.

- Server: `KelompokAsetApiHelper` (AIS r83889, r83890, r83899)
- Klien: `apps/ebisnis/lib/screens/kelompok_aset_screen.dart` (zishof-platform `6850455`)

---

## 1. Ruang lingkup sengaja disempitkan

Layar ini **hanya** menyunting pemetaan akun, bukan CRUD kelompok aset. Itulah yang dibutuhkan
saat memperbaiki akun posting, dan mempersempitnya berarti aplikasi tidak menumbuhkan salinan
kedua master aset yang harus dirawat sejajar dengan versi web.

Tiap bidang disimpan **sendiri-sendiri**. Server menerima seluruh daftar satu bidang sekaligus,
jadi menyimpan per bidang membuat kiriman ulang antrean menghasilkan keadaan yang sama, dan
kegagalan satu bidang tidak menyeret bidang lain.

## 2. Konvensi yang nyaris terlewat

Saya menyalin cara `jenis_produk_screen.dart` memuat daftar akun:

```dart
daftarDenganCache('akun_list', {'limit': 2000}, 'master:akun_list')
```

Kontrak test punya aturan lain yang tidak saya lihat sampai membaca namanya:
**`akun_list` dibaca lewat cache BERSAMA `master:akun`** — dipakai sembilan layar akuntansi,
dengan `limit: 5000`. Layar yang saya tiru justru bukan anggota daftar itu.

Akibat kalau dibiarkan: bagan akun tersimpan **dua kali** di cache lokal dengan kunci berbeda,
dan keduanya bisa berbeda isi setelah salah satu disegarkan. Bukan galat, bukan crash — cuma dua
kebenaran.

Layar ini sekarang memakai kunci bersama, dan **didaftarkan ke `layarAkun`** pada kontrak test
supaya aturan itu berlaku untuknya juga. Menumpang aturan tanpa ikut dijaga aturan itu adalah
cara paling mudah untuk kembali menyimpang diam-diam.

## 3. Satu berkas yang tidak saya commit

`posting_akun_perbaikan.dart` — tempat tombol "Kelompok Aset" dipasang — ternyata **belum
pernah masuk git**, padahal dua berkas terlacak (`laporan_screen.dart`,
`posting_toko_dialog.dart`) sudah mengimpornya. Itu pekerjaan sesi lain yang sedang berjalan.

Suntingan saya di dalamnya (impor, satu `case`, dan pembuangan `webPath`) sengaja **tidak**
ikut commit. Menyertakannya berarti menyapu berkas milik sesi lain ke dalam commit saya,
lengkap dengan keadaan setengah jadinya. Lima baris itu akan ikut ketika pemiliknya commit.

Konsekuensinya jujur: di repositori, layar Kelompok Aset sudah ada tetapi **belum tersambung**
ke tombolnya sampai berkas itu masuk. Di working copy ini keduanya sudah tersambung.

## 4. Keadaan `.zul` sekarang

| Titik keluar | Keadaan |
|---|---|
| 10 laporan katalog | natif (doc 101, 102) |
| `lk_dashakun` | masih ZK — isi dasbornya tidak bisa dipastikan dari sumber (doc 102) |
| `kelompok_asset.zul` | **natif** |
| `master_asset.zul` | masih ZK — menunggu keputusan pemilik (doc 103) |

Diperiksa: `flutter analyze` bersih; `master_offline_kontrak_test` 24/24 lulus; kompilasi Java
kode keluar 0, nol galat.
