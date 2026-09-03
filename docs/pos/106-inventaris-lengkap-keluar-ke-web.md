# Inventaris lengkap titik keluar ke aplikasi web — dua aplikasi, bukan satu

Doc 101 menyusun inventaris titik keluar ZK dari POS dan menyebut angka 13. Angka itu benar
untuk `apps/ebisnis` — dan **hanya** untuk `apps/ebisnis`. Repositori klien berisi **dua**
aplikasi; `apps/ecanteen` tidak ikut disurvei.

Dokumen ini menutup lubang itu dengan sapuan menyeluruh, dan mencatat satu hipotesis saya yang
ternyata salah.

---

## 1. Sapuan menyeluruh: sepuluh titik `launchUrl`, dua aplikasi

Seluruh `apps/` (di luar `.dart_tool`) memuat sepuluh pemanggilan `launchUrl`. Tujuh di
antaranya **bukan** aplikasi web AIS:

| Titik | Membuka | |
|---|---|---|
| `ebisnis/bootstrap.dart:323` | unduhan APK/EXE pembaruan | sah |
| `ebisnis/main.dart:352` | unduhan APK/EXE pembaruan | sah |
| `ebisnis/kasir_screen.dart:1677` | unduhan APK/EXE pembaruan | sah |
| `ebisnis/log_error_screen.dart:204` | isu GitHub baru | sah |
| `ebisnis/anggota/tab_topup.dart:1220` | tautan pembayaran dari server | sah |
| `ecanteen/topup_screen.dart:196` | tautan pembayaran dari server | sah |
| `ebisnis/pengadaan_cetak_util.dart:84` | dokumen cetak — **jalan mundur** | lihat bawah |

`pengadaan_cetak_util` layak dicatat karena jalur utamanya **sudah** berbasis API: dokumennya
dikirim sebagai bytes dan ditampilkan di dalam aplikasi lewat dialog pratinjau. URL hanya
dipakai ketika dokumennya terlalu besar untuk ikut dikirim. Itu bukan layar web, melainkan
berkas cetak.

Sisanya tiga, dan hanya inilah yang benar-benar membuka aplikasi web:

| Titik | Membuka | Keadaan |
|---|---|---|
| `ebisnis/laporan_screen.dart:143` | `common/display.zul?p=akuntansi` lewat entri `lk_dashakun` | tersisa (doc 102) |
| `ebisnis/posting_akun_perbaikan.dart:201` | `master_asset.zul` | tersisa, menunggu pemilik (doc 103) |
| `ecanteen/beranda_screen.dart:177` | `mobile_auth.jsp?tujuan=notifikasi` | **baru terdata** |

Jadi rujukan `.zul` yang benar-benar dipanggil di seluruh repositori klien tinggal **satu**:
`master_asset.zul`. Rujukan `.zul` lain di kode Dart semuanya komentar dokumentasi atau nama
test.

## 2. Hipotesis yang salah, dan kenapa memeriksanya penting

Komentar di `ecanteen/beranda_screen.dart` berbunyi: *"Notifikasi belum punya aksi API
tersendiri, jadi dibuka lewat jembatan sesi web."*

Sekilas itu tampak usang. Server **punya** `notifikasi_list` dan `notifikasi_hapus`, keduanya
tersalur di `PosApi` dan lolos gerbang izin, dan ebisnis sudah memakainya secara natif di
`anggota/tab_notifikasi.dart`. Kesimpulan yang menggoda: tinggal disambungkan.

Kesimpulan itu salah. `KantinHelper.notifikasiList` dimulai begini:

```java
boolean isAdminNl = tbmuser != null && tbmuser.getPedagang() == null;
if (!isAdminNl) {
    hasil.put("status", "91");
    hasil.put("description", "Hanya admin yang dapat melihat log Notifikasi.");
    return;
}
```

Ia **admin-only**, dan isinya **log seluruh notifikasi** untuk keperluan audit — bukan kotak
masuk milik satu anggota. eCanteen dipakai anggota/wali murid, jadi aksi itu memang bukan yang
dibutuhkannya. Komentar tadi benar apa adanya.

**Bahaya yang perlu ditulis.** Pembaca berikutnya yang melihat kecocokan nama lalu menyambungkan
`notifikasi_list` di eCanteen akan mendapat status `91` — dan langkah lanjutan yang wajar dari
situ adalah **melonggarkan penjaganya**. Itu akan membuka log notifikasi seluruh anggota kepada
setiap pengguna eCanteen. Nama yang cocok bukan kemampuan yang cocok; ini bentuk kekeliruan yang
sama dengan doc 100, hanya akibatnya kebocoran, bukan pekerjaan mubazir.

Versi natifnya butuh aksi **baru** yang mengembalikan notifikasi milik pengguna yang sedang
masuk. Itu fitur baru, bukan penyambungan, dan tidak dikerjakan di sini karena permintaannya
menyangkut `*.zul`.

## 3. Pelajaran inventarisnya

Doc 101 menghitung dengan teliti di dalam batas yang tidak pernah saya periksa. Sapuannya benar,
angkanya benar, dan tetap tidak lengkap — karena pertanyaannya "berapa di ebisnis", sementara
yang dijawabkan "berapa seluruhnya".

Sebelum menyebut sebuah inventaris lengkap: periksa dulu berapa banyak pohon yang ada.
