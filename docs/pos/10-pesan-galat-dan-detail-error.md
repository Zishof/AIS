# 10 — Pesan galat: alasan penolakan yang tampil, dan Detail Error

Berawal dari satu laporan pengguna: menyimpan **Tambah Topup** gagal dengan kalimat
*"Server belum dapat menyelesaikan proses ini. Tidak ada perubahan parsial yang
dipertahankan."* — kalimat yang tidak memberi tahu apa pun tentang penyebabnya.

Ternyata server **memang menolak dengan alasan yang jelas**, tetapi alasan itu hilang dua kali:
sekali di server, sekali di aplikasi.

| | |
|---|---|
| Server | `ais/action/servlet/PosApi.java` (`normalisasiStatusKantinHelper`, `pesanBisnisAman`) |
| Klien | `apps/ebisnis/lib/api_client.dart` (`GalatTampil`), `lib/widgets/jejak_galat.dart`, `lib/widgets/app_components.dart` (`AppDetailGalat`, `AppDetailGalatOpsional`, `AppInfoBanner.detail`, `AppFormSheet.errorDetail`) |
| Revisi server | r77848 (cabang `PERMINTAAN_DITOLAK`), diperluas kemudian ke status selain "91" |
| Commit klien | `e4616af`, `d81b341`, `a59e7c0`, `18fab35` |

---

## 1. Hilang yang pertama: di server

`PosApi.normalisasiStatusKantinHelper` menyeragamkan balasan bergaya `KantinHelper`
(`status:"00"|"91"|...` + `description`) menjadi kontrak `status:"success"|"error"` + `message`.
Cabangnya lengkap untuk `"00"`, untuk status kosong, dan untuk `checkBayar` + `"01"` — **tetapi
tidak ada cabang untuk `"91"`**, yaitu kode yang dipakai `KantinHelper` untuk **360 penolakan
aturan bisnis**. Semuanya jatuh ke fallback `SERVER_ERROR` dengan kalimat generik itu; alasan
aslinya hanya tersisa di field `teknis`.

Perbaikannya: cabang `PERMINTAAN_DITOLAK` yang meneruskan `description` apa adanya.

Syaratnya sengaja **bukan** "status == 91", melainkan **"deskripsinya layak dibaca"**
(`pesanBisnisAman`), karena `KantinHelper` juga memakai kode lain untuk penolakan yang
kalimatnya sama-sama ditujukan kepada pengguna — mis. `"90"` pada `checkBayar` dan `"01"` di
luar konteks `checkBayar`.

`pesanBisnisAman` menolak teks yang mengandung jejak teknis (`exception`, `caused by`,
`org.hibernate`, `constraint`, `sqlstate`, `select `, `insert into`, baris baru, panjang > 400
karakter, dst). Ini perlu karena beberapa titik di `KantinHelper` menaruh `e.getMessage()` ke
dalam `description` bersama status "91" (mis. `draftBayarRollback`, `editTransaksi`) — dan
aturan "pesan untuk kasir tidak boleh berisi nama class, SQL, atau stack trace" memang sudah
dianut `KantinHelper` sendiri. Penyaringnya kini ditegakkan sekali di pintu keluar API.

Urutan cabang lama **tidak diubah**, sehingga klasifikasi `STOK_TIDAK_CUKUP`,
`PRODUK_KADALUARSA`, `DUPLIKAT_KODE_TRANSAKSI`, `PESANAN_PERLU_DIMUAT_ULANG`,
`TIDAK_DITEMUKAN`, dan `DATA_TIDAK_LENGKAP` tetap persis seperti sebelumnya.

**Cakupan**: 176 aksi PosApi melewati normalisasi ini, termasuk `topup_saldo`, `deposit_ubah`,
dan `deposit_hapus`.

### Hasil uji

Harness reflektif atas `normalisasiStatusKantinHelper` hasil kompilasi terbaru — **16/16
lulus**:

- lima alasan penolakan topup tampil utuh (hak akses, jenis keanggotaan, konfigurasi default,
  member tidak ditemukan, nominal ≤ 0);
- dua deskripsi berjejak teknis tetap tersamar menjadi pesan generik;
- enam klasifikasi lama tidak bergeser;
- `"90"` berkalimat aman → tampil; `"90"` berjejak teknis → tersamar; `"01"` di luar
  `checkBayar` → tampil; `"01"` pada `checkBayar` tetap `TIDAK_DITEMUKAN`;
- `"00"` tetap menjadi `success`.

Ujung ke ujung pada basis UAT: `KantinHelper.topupSaldo` sungguhan dengan peran `am`
menghasilkan *"Jenis keanggotaan member ini tidak diizinkan menerima topup lewat kasir."* —
dan `deposit` 0 baris sebelum maupun sesudah (penolakan tidak menulis apa pun).

### Yang tidak diubah

`"92"` sengaja dibiarkan. Setelah dibaca, itu **sinyal alur** `butuhPilihManual` pada mutasi
antar outlet — PosApi memang sudah memetakannya ke `success` supaya body-nya (daftar kandidat
produk) utuh sampai ke klien.

## 2. Hilang yang kedua: di aplikasi

Kontrak API memisahkan `message` (kalimat untuk pengguna) dari `teknis` (jejak untuk admin),
tetapi `ApiException.toString()` = `pesan + solusi pertama`, dan layar menyimpan `e.toString()`.
Lapis `teknis` terbuang tanpa sisa — persis di kasus Topup, di mana lapis itulah satu-satunya
tempat alasan penolakan berada.

**`GalatTampil.dari(error)`** memisahkan kedua lapis di satu tempat, dan mengembalikan
`detail: null` untuk galat non-API supaya penyingkap tidak muncul saat memang tidak ada lapis
kedua.

**Mixin `JejakGalat`** dipakai `with JejakGalat` pada State mana pun:

```dart
String terapkanGalat(Object e)          // dipanggil DI POSISI `= e.toString()` yang lama
String? detailUntuk(String? pesan)      // detail milik pesan yang sedang tampil
```

`terapkanGalat` dipanggil dari posisi assignment yang lama, sehingga bentuk arrow maupun blok,
ber-`if (mounted)` maupun tidak, semuanya tertangani tanpa satu alur pun ditulis ulang.

**`detailUntuk` memasangkan detail dengan pesannya**, bukan sekadar menyimpan detail terakhir.
Ini menutup kasus detail basi: layar yang gagal di server lalu menampilkan pesan validasi lokal
("Member wajib dipilih") tidak lagi menyodorkan jejak teknis milik kegagalan sebelumnya.

### Permukaan yang menampilkannya

| Permukaan | Cara |
|---|---|
| Form (`AppFormSheet`) | parameter `errorDetail` → penyingkap "Detail Error" |
| Banner (`AppInfoBanner`) | parameter `detail` |
| Panel "gagal memuat" hand-rolled | `AppDetailGalatOpsional` disisipkan di bawah teksnya |
| Placeholder dasbor bersama (`statusMuatDasbor`) | parameter `detail` — 9 tab Ringkasan + Monitor Diskon |
| Tujuh panel `_PanelError*` privat (Inventory Sales) | parameter `detail` |
| Snackbar | `snackbarGalat(context, e)` — pesan ringkas + tombol **Detail** yang membuka panel teknis berikut tombol salin |

Penyingkapnya **tertutup secara bawaan** supaya kasir tidak terganggu, dan isinya dapat
diseleksi serta disalin utuh — itulah yang berguna saat melapor ke admin.

### Cakupan akhir

- **73 dari 78** variabel galat menampilkan penyingkap Detail Error.
- **43 snackbar** berpindah ke `snackbarGalat`.
- 5 sisanya memakai bentuk penampil yang belum berpola (dicatat, tidak dipaksakan).

### Test

| Berkas | Isi |
|---|---|
| `test/jejak_galat_test.dart` | pemasangan detail↔pesan, galat non-API tidak mengarang lapis teknis, snackbar menyediakan jalan ke panel teknis |
| `test/detail_galat_form_test.dart` | penyingkap tertutup sampai diketuk, hilang tanpa detail, banner daftar ikut membawanya |
| `test/detail_galat_kontrak_test.dart` | **setiap `AppFormSheet` dengan `errorText` wajib meneruskan `errorDetail`** — logikanya diuji tidak kosong dengan menjalankannya atas versi berkas sebelum sapuan |

## 3. Yang belum

- 5 variabel galat dengan penampil tak berpola.
- Kasus sempit: pesan validasi lokal yang dipasang **sebelum** titik penihilan, menyusul
  kegagalan server sebelumnya — penyingkap masih memuat detail lama. Cakupannya sempit dan
  tidak menyesatkan isi banner-nya.
- `TransaksiOutboxService.kodePenolakanPermanen` sengaja **tidak** ditambahi
  `PERMINTAAN_DITOLAK`: kode itu menaungi banyak penolakan yang bisa diperbaiki pengguna
  (sesi kas belum dibuka, hak akses baru diaktifkan), dan retry otomatis memakai `kode_unik`
  asli sehingga aman dari transaksi ganda. Menandainya permanen justru memaksa kasir mengirim
  ulang manual.
