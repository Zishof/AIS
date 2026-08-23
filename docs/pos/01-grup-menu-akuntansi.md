# 01 — Grup menu "Akuntansi" dan CRUD Kode Akun

## 1. Yang berubah pada menu

Menu **"Laporan Keuangan"** diganti namanya menjadi **"Akuntansi"**, dan tab-tab yang
sebelumnya berada di dalam satu halaman dinaikkan menjadi **submenu** di bawahnya —
bentuknya disamakan dengan grup menu "Kulakan".

Enam submenu Akuntansi: Kode Akun, Grup Akun, Jenis Transaksi, Bank & Akun, Jurnal
Umum, dan Siklus Akuntansi.

Grup menu di Flutter didefinisikan di `apps/ebisnis/lib/widgets/app_shell.dart`
(`_grupMenu` berisi `_GrupMenuShell(label, items)`), dan cerminnya untuk Android ada di
`app_drawer.dart`. Keduanya harus disunting bersama — ada uji yang memastikan setiap
menu muncul di kedua platform.

## 2. Hak akses per menu dan per aksi

Seluruh menu baru didaftarkan di `ais/common/EbisnisMenuKatalog.java` sehingga muncul di
layar pengaturan peran (`TbmroleAction`) dan dapat ditampilkan/disembunyikan per peran.

Struktur yang dipakai di katalog:

| Konstanta | Guna |
|---|---|
| `DAFTAR` | katalog menu yang dirender `TbmroleAction` |
| `KUNCI_DEFAULT_NONAKTIF` | menu yang **fail-closed**: tersembunyi kecuali kuncinya diberikan |
| `KUNCI_AKUNTANSI` | menu yang tunduk pada peran akuntansi |
| `KUNCI_CRUD` | menu yang hak aksinya dipecah: `create`, `update`, `delete`, `approve`, `reject` |

Bentuk JSON hak akses yang disimpan pada peran:

```json
{ "crud": { "kode_akun": { "create": true, "update": true, "delete": false } } }
```

Pemeriksaannya: `EbisnisMenuKatalog.bolehAksi(roleJson, kunci, aksi)`.

Di sisi Flutter ada dua gerbang yang **berbeda dan disengaja**:

- `Sesi.bolehMenu(k)` — *default-allow*, dipakai menu lama supaya tidak ada yang
  mendadak hilang setelah pembaruan.
- `Sesi.bolehMenuVarianBaru(k)` — *fail-closed*, dipakai seluruh menu baru: selama
  kuncinya belum diberikan ke peran, menunya tidak muncul.

## 3. CRUD Kode Akun

Layar Kode Akun sebelumnya hanya bisa membaca. Sekarang tersedia **Tambah, Ubah, Hapus,
Salin, dan Tambah Anak** pada kelima tabnya, mengikuti layar ZK.

Sisi server: `ais/action/servlet/api/KodeAkunApiHelper.java`

| Aksi | Guna |
|---|---|
| `kode_akun_simpan` / `kode_akun_hapus` | akun |
| `kode_akun_grup_simpan` / `kode_akun_grup_hapus` | grup akun |
| `kode_akun_jenis_transaksi_simpan` / `..._hapus` | jenis transaksi |
| `kode_akun_bank_simpan` / `kode_akun_bank_hapus` | bank & akun |

Hal yang perlu diketahui:

- **Panjang kode anak** tidak dikarang: diambil dari system property `akun_lenght`
  (bawaan `2`) lewat `panjangKodeAnak()`, sama seperti ZK. Kode anak berikutnya dihitung
  `indexBerikutnya` dari kode saudara yang sudah ada.
- Penghapusan akun menolak bila akun masih punya keturunan (`keturunanDari`).
- Respons daftar ikut membawa objek `hak` (hasil `hakAksesJson`) dan `panjangKodeAnak`,
  supaya layar hanya menampilkan tombol yang memang boleh dipakai — bukan menampilkan
  tombol yang ujungnya ditolak server.

## 4. Yang belum dikerjakan

- Field ZK **"Khusus Untuk Satuan Kerja"** pada formulir Akun belum ditampilkan di
  Desktop/Android.
- Belum ada padanan JSP untuk layar Kode Akun.
