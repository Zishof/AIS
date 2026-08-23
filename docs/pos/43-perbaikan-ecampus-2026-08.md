# 43 — Perbaikan ECAMPUS, Agustus 2026

Dua belas laporan galat dari produksi ECAMPUS ditangani sekaligus. Setelah
ditelusuri sampai akarnya — dari jejak tumpukan, bukan dari baris terakhirnya —
keduabelasnya bermuara pada **enam sebab**. Menambal per laporan akan
menghasilkan enam tambalan yang sama ditulis enam kali.

Seluruh perbaikan bergaya **Java 1.7/1.6**: tanpa lambda, try-with-resources,
diamond operator, maupun Stream API.

## 1. `lock_version` kosong menghentikan sinkronisasi repositori

`RepositorySyncService` — baris `repo_item` lama punya `lock_version` NULL.
Hibernate `@Version` membaca **snapshot yang dimuat**
(`DefaultFlushEntityEventListener.getNextVersion` memakai `entry.getVersion()`),
**bukan** getter-nya. Karena itu getter yang menangkal null tidak menolong sama
sekali — datanya yang harus dibetulkan.

`perbaikiLockVersionKosong(session)` dipanggil di awal setiap siklus:

```sql
update repo_item set lock_version = 0 where lock_version is null
```

## 2. Bentrok kunci pada `konfigurasi` membanjiri log

`KonfigurasiManager` — dua benang membuat konfigurasi bernama sama secara
bersamaan. Sekarang `ConstraintViolationException` ditangkap, barisnya dibaca
ulang **berdasarkan nama** (yang memang sudah ada), dan hanya bila itu pun gagal
barulah dicatat — sekali saja per nama, lewat `BENTROK_ID_TERCATAT`:

```java
Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>())
```

> **Tindakan operasional yang masih tertunggak:** urutan (sequence) id tabel
> `konfigurasi` tertinggal di belakang isi tabelnya. Perbaikan di atas membuat
> sistem tidak lagi gagal, tetapi penyebabnya baru hilang setelah `setval`
> dijalankan pada urutan itu. Ini pekerjaan pemilik basis data.

## 3. Refleksi yang wajar dianggap galat

`Document.java` — pencarian metode lewat refleksi mencatat
`NoSuchMethodException` sebagai galat, padahal itu jalur cadangan yang memang
diharapkan meleset. Sekarang `Tbmuser` ditangani lewat jalur bertipe
(`hakAkses().getRoleId()`), dan `NoSuchMethodException` pada jalur cadangan
diperlakukan sebagai keadaan normal.

## 4. Pemeriksaan ganda biodata mati ketika tanggal lahir kosong

`BiodataCalonMahasiswaAction` — pendengar pemeriksaan duplikat membaca
`tanggalLahir.getValue()` saat medannya belum terisi. Nilainya kini dibaca di
dalam `try/catch` ke `tglLahirCek` lebih dulu, sehingga medan kosong tidak
mematikan pemeriksaannya.

## 5. Kegagalan otentikasi SMTP membanjiri log

`MailSender` — setiap surel yang gagal mencatat `AuthenticationFailedException`
penuh. Kini dibatasi sekali per 15 menit (`JEDA_LAPOR_AUTH_MS` +
`AUTH_GAGAL_TERAKHIR` bertipe `AtomicLong`).

> **Tindakan operasional yang masih tertunggak:** kredensial SMTP-nya sendiri
> memang salah. Pembatasan ini hanya menghentikan banjir lognya, bukan
> memperbaiki pengiriman surel.

## 6. Admin sistem ditolak layar Hak Akses

`KantinHelper` — tiga penjaga (`ebisnisRoleList`, `ebisnisRoleMenuAmbil`,
`ebisnisRoleMenuSimpan`) mensyaratkan pengguna punya `pedagang`, sehingga admin
sistem yang sungguhan — yang memang tidak terikat pedagang mana pun — ikut
tertolak dari layarnya sendiri:

```java
if (tbmuser != null && tbmuser.getPedagang() != null
        && !Common.getApakahAdminLain(tbmuser)) {
```

## Aturan yang dipegang saat menambal

- Tidak ada fungsi lama yang dibuang; yang salah diperbaiki, yang benar
  dibiarkan.
- Sesi yang dibuka `openSession()`/`currentNativeSession()` ditutup di `finally`
  dengan urutan clear → disconnect → close. `currentSession()` tidak pernah
  ditutup manual.
- Berkas ZUL/JSP/CSS diletakkan di folder webapp yang benar.
- Disunting langsung di repositori — bukan dikirim sebagai arsip ZIP.
