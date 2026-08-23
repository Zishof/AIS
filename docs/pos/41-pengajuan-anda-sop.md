# 41 — "Pengajuan Anda" (Workflow / SOP) di POS Desktop & Android

Pemindahan layar SOP dari ZKoss (`DasboardSop.java`, `TampilanAlurSopAction.java`)
ke POS Desktop/Android, **sepenuhnya berbasis API** — tanpa iframe maupun WebView.

## Bentuk sisi server

Mesin aturannya sudah ada: `ais.action.servlet.api.SopService` (2.748 baris).
Yang ditambahkan hanyalah jalur masuk dari POS.

`PosApi` meneruskan setiap aksi ber-awalan `sop_` ke `prosesSop`, yang mencari
rutenya di daftar tetap:

```java
private static final Map<String, ais.action.servlet.api.ApiRoute> RUTE_SOP =
        ais.action.servlet.api.ApiRouteRegistry.createDefaultRoutes();
```

Dua belas aksi: `sop_daftar`, `sop_jenis`, `sop_cari`, `sop_cari_entitas`,
`sop_detail`, `sop_alur`, `sop_proses`, `sop_ubah_info`, `sop_ubah`,
`sop_dasbor`, `sop_ajukan`, `sop_lampiran`.

### Jembatan identitas

`SopService` dan seluruh kode SOP lama membaca pengguna dari atribut sesi
`mytbmuser`, sementara POS mengenali pengguna dari token. `prosesSop` memasang
atribut itu sebelum memanggil rute dan **memulihkannya di `finally`** — kalau
tidak, permintaan POS akan mencemari sesi web yang mungkin dipakai pengguna yang
sama di peramban.

### Penyelarasan amplop — sumber satu bug produksi

Ini pantas ditulis panjang karena sudah membuat seluruh modul tidak berfungsi.

AIS memakai konvensi `status:"00"` untuk berhasil. POS memakai
`status:"success"`; `ApiClient.aksi` (`api_client.dart:338`) melempar
`ApiException` untuk apa pun selain itu. Saya sempat menyimpulkan "amplopnya
sudah cocok" dan melewati penyelarasan — akibatnya balasan sukses berbunyi "OK"
muncul di layar pengguna sebagai **pesan galat**.

Ada satu seluk-beluk lagi: untuk aksi yang mengembalikan **daftar**, `status:"99"`
berarti "tidak ada data", bukan galat. Daftar kosong harus sampai ke klien
sebagai keberhasilan dengan nol baris, bukan sebagai kegagalan.

```java
private static void selaraskanAmplopSop(String action, JSONObject hasil) throws Exception {
    String asli = hasil.optString("status", "");
    hasil.put("statusAsli", asli);          // status asli tetap dikirim
    if ("00".equals(asli)) { hasil.put("status", "success"); return; }
    if ("99".equals(asli) && aksiDaftarSop(action)) { hasil.put("status", "success"); return; }
    hasil.put("status", "error");
}
```

`statusAsli` sengaja tetap dikirim supaya klien masih bisa membedakan "berhasil"
dari "kosong" bila suatu saat perlu.

### Hak akses

Tombol dan aksinya dijaga `Tbmrole.workflow`:

```java
if (action.startsWith("sop_")) {
    // Diperiksa SEBELUM jalan pintas supervisor
    return Boolean.TRUE.equals(role.getWorkflow());
}
```

Urutannya penting: pemeriksaan ini harus **mendahului** jalan pintas supervisor,
kalau tidak supervisor tanpa hak workflow ikut lolos.

Menu dikirim lewat `aksesMenu.put("pengajuan_anda", ...)`; Flutter membacanya
lewat `Sesi.instance.bolehMenu('pengajuan_anda')`, sehingga tombolnya benar-benar
hilang — bukan sekadar dinonaktifkan — bagi yang tidak berhak.

## Sisi Flutter

| Berkas | Isi |
|---|---|
| `screens/pengajuan_anda_screen.dart` | Tab **Dasbor** (6 kartu KPI) + tab **Pengajuan** (7 cip penyaring, pemilih Satker) |
| `screens/pengajuan_anda_detail_screen.dart` | Header, riwayat, tahap tertunda, formulir, alur; `_DialogProsesTahap` dengan `modeUbah` |
| `screens/pengajuan_baru_screen.dart` | Wisaya 3 langkah; `DialogCariEntitas` publik |
| `services/unggah_lampiran_sop.dart` | Unggah multipart ke `DoUpload` (medan `token`, parameter `ref`) |

Enam kartu KPI dan tujuh cip penyaringnya **sengaja sama persis** dengan versi
ZKoss, termasuk cip ketujuh `kCariSemua`. Perbedaan sekecil apa pun di sini
membuat dua layar melaporkan angka berbeda untuk data yang sama, dan pengguna
akan menganggap salah satunya rusak.

Tombolnya dipasang di `widgets/app_shell.dart` — cip di desktop, `IconButton` di
ponsel.

`screens/toko_kelola_screen.dart` diubah dari `Scaffold` menjadi `AppShell`
karena sebelumnya layar itu terbuka sebagai **jendela baru** alih-alih tetap di
bingkai konten.

## Cacat yang ditemukan di ZKoss dan ikut diperbaiki

Acuannya adalah ZKoss, tetapi acuan itu sendiri menyimpan empat cacat nyata.
Semuanya diperbaiki di ketiga versi (ZKoss, JSP, POS), bukan hanya di POS:

1. **`criteriaSudahDisposisi`** tidak menyaring `disposisiSop.aktif`, sehingga
   disposisi yang sudah dicabut tetap terhitung.
2. **`criteriaMenungguSaya`** tidak menyaring `selesai`, sehingga dokumen yang
   sudah tuntas masih muncul sebagai menunggu tindakan.
3. **`criteriaDipantau`** menghitung ganda ketika satu dokumen punya lebih dari
   satu disposisi. Diperbaiki dengan `hitungDipantauTepat` memakai
   `countDistinct("disposisiSop.id")`.
4. **`countDataPengajuanAnda`** sudah mati (tidak dipanggil siapa pun) tetapi
   masih dirawat; dihapus.

Ditambahkan pula: `perBulan` (lewat `kunciBulanTahun()` + `deretWaktu()`),
serta `satkerAlur`/`satkerSop`.

## Fitur "Ubah"

Aturannya **tidak dikarang** — diambil apa adanya dari konfigurasi tahap milik
ZKoss, lima bendera: `bolehDiisiCatatan`, `tanggalBolehDiubah`,
`bekukanDokumen`, `kembaliKePengaju`, `aktorLabel`.

Sisi server: `ubahInfo()` melaporkan apa yang boleh diubah, `ubah()`
melaksanakannya, `tolakBilaTidakBolehUbah()` menjaganya, dan `langkahMilik()`
memastikan seseorang hanya bisa mengubah langkah miliknya sendiri. `proses()`
menerima `waktu` opsional supaya waktu tahap dapat dikoreksi bila tahapnya
mengizinkan.

Sisi klien: `_DialogProsesTahap` dipakai ulang dengan `modeUbah`, sehingga
formulir ubah dan formulir proses tidak bisa menyimpang satu sama lain.

## Catatan

SOP **tetap daring** — tidak masuk mekanisme lokal-dulu. Alasannya di
[42](42-lokal-dulu-dan-hapus-lunak.md).
