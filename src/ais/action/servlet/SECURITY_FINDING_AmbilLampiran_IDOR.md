# Temuan Keamanan (Belum Diperbaiki): IDOR via Parameter Mentah di `/AmbilLampiran` dan `/al`

Status: **TERBUKA — perlu tindak lanjut terpisah.** Bukan bagian dari fitur yang sedang berjalan; dicatat sebagai temuan sampingan hasil audit servlet lampiran (lihat riwayat perbaikan terkait di bawah).

## Ringkasan

`ais.action.servlet.AmbilLampiran` (dipetakan ke `/AmbilLampiran` dan `/al`) menerima parameter `ref`, `clazz`, `jenis`, `usingId`, `jurusan`, `download` **langsung dari query string, tanpa enkripsi/tanda tangan apa pun**, sebagai fallback ketika token `d` tidak ada atau tidak memuat key tersebut:

```java
// src/ais/action/servlet/AmbilLampiran.java (process(), sekitar baris 129-139)
String id = jsonObject != null && !jsonObject.isNull("ref") ? ... : request1.getParameter("ref");
...
String cc  = jsonObject != null && !jsonObject.isNull("clazz") ? ... : request1.getParameter("clazz");
...
String jenis = jsonObject != null && !jsonObject.isNull("jenis") ? ... : request1.getParameter("jenis");
```

Karena servlet **tidak pernah memeriksa apakah user yang login berhak melihat baris `ref` tsb** (tidak ada pengecekan kepemilikan/ACL per entitas — lihat catatan di bawah untuk pengecualian kecil pada `FotoAdmin`), siapa pun yang **sudah login** (peran apa saja — siswa, mahasiswa, pegawai, dst) dapat men-tebak/menaikkan `ref` untuk mengunduh lampiran milik orang lain, selama tahu (atau menebak) `clazz` yang benar.

## Kenapa bukan tambalan satu baris

Menghapus fallback `request1.getParameter(...)` akan mematahkan **23+ lokasi pemanggilan sah** yang sudah beredar di kode (laporan JasperReports, JSP, REST API) yang memang membangun URL `/AmbilLampiran?ref=..&clazz=..&jenis=..` secara langsung, bukan lewat `FileFotoLain.ambilLinkLampiranLain(...)` yang menghasilkan token `d` terenkripsi. Perbaikan yang benar butuh audit per-pemanggil, bukan perubahan global di servlet.

## Prasyarat akses (konteks setelah perbaikan A)

Endpoint `/AmbilLampiran` **dan** `/al` sama-sama sudah digerbangi `IS_AUTHENTICATED_REMEMBERED` di `applicationContext-security.xml` (perbaikan terpisah, sudah diterapkan — lihat bagian "Riwayat terkait" di bawah). Jadi eksploitasi temuan ini **membutuhkan akun yang sudah login** (peran apa saja di sistem), bukan lagi anonim dari luar. Ini menurunkan severity dari "kebocoran data publik tanpa syarat" menjadi "eskalasi horizontal antar-user yang sudah terautentikasi" — tetap serius karena sistem ini multi-tenant (banyak yayasan/sekolah/kampus dalam satu deployment) dan mencakup data sensitif pribadi.

## Pemanggil yang perlu diaudit, dikelompokkan menurut sensitivitas

### Kategori A — Sensitivitas TINGGI (data personal/berkas privat, prioritas audit pertama)

- [src/ais/action/servlet/api/CatatanApi.java:741](src/ais/action/servlet/api/CatatanApi.java:741) — `url = "/AmbilLampiran?download=1&ref=" + c.getId() + "&clazz=" + CLAZZ_CATATAN_SISWA + ...` dengan `CLAZZ_CATATAN_SISWA = CatatanSiswa.class.getName()` (baris 94). Lampiran catatan siswa (kemungkinan berisi catatan perilaku/konseling) — `clazz` sudah diketahui publik dari nama kelas Java, tinggal tebak `ref` (ID baris) untuk baca lampiran siswa lain.
- [src/ais/action/servlet/api/SopService.java:2187](src/ais/action/servlet/api/SopService.java:2187) — pola sama: `"/AmbilLampiran?download=1&ref=" + refId + "&clazz=" + clazzName + "&jenis=" + jenis`. Perlu ditelusuri `clazzName` berasal dari mana (dokumen SOP/disposisi surat kemungkinan berisi dokumen resmi/administratif).
- [webapp/report/form_realisasi_kinerja_dosen.jrxml:716,722](webapp/report/form_realisasi_kinerja_dosen.jrxml:716) dan [webapp/report/Rekaman_Nilai.jrxml:650](webapp/report/Rekaman_Nilai.jrxml:650) — `ref=id_dosen_asli` / `atasanlangsung` / `id_kaprodi`, `jenis=TTD+Dosen`. Ini **gambar tanda tangan dosen** — kalau `clazz` default (`LampiranLain`) cukup untuk mengambilnya via `ref` sembarang, tanda tangan siapa pun bisa diunduh dan berpotensi disalahgunakan untuk memalsukan dokumen resmi (nilai, SK, dll).

### Kategori B — Sensitivitas RENDAH (aset institusi, bukan data pribadi individu)

Dipakai untuk menampilkan **kop surat/letterhead** sekolah atau yayasan (`jenis=KOP+Sekolah` / `KOP+Yayasan`) — logo/kop yang memang dicetak terbuka di laporan resmi, jadi risikonya terbatas pada kebocoran batas multi-tenant (user sekolah A bisa lihat kop sekolah B), bukan data pribadi:

- [src/ais/action/report/format1/sekolah/LaporanTunggakanSiswa.java:460,462](src/ais/action/report/format1/sekolah/LaporanTunggakanSiswa.java:460)
- [src/ais/action/report/format1/sekolah/LaporanSaldoSiswa.java:375,377](src/ais/action/report/format1/sekolah/LaporanSaldoSiswa.java:375)
- [src/ais/action/report/format1/sekolah/LaporanPembelianSiswa.java:412,414](src/ais/action/report/format1/sekolah/LaporanPembelianSiswa.java:412)
- [src/ais/action/report/format1/sekolah/LaporanPembayaranSiswa.java:495,497](src/ais/action/report/format1/sekolah/LaporanPembayaranSiswa.java:495)
- [src/ais/action/report/format1/sekolah/LaporanDepositSiswa.java:416,418](src/ais/action/report/format1/sekolah/LaporanDepositSiswa.java:416)
- [src/ais/action/report/format1/payroll/LaporanTransaksiPegawai.java:225,227](src/ais/action/report/format1/payroll/LaporanTransaksiPegawai.java:225)
- [src/ais/action/master/dashboard/sekolah/DashboardRekapKegiatanSiswaData.java:285,287](src/ais/action/master/dashboard/sekolah/DashboardRekapKegiatanSiswaData.java:285)
- [webapp/report/ValidasiPSB.jrxml:364](webapp/report/ValidasiPSB.jrxml:364), [AlbumPSBHari.jrxml:103](webapp/report/AlbumPSBHari.jrxml:103), [AbsensiPSB_day1.jrxml:241](webapp/report/AbsensiPSB_day1.jrxml:241), [BeritaAcaraUjianPSB.jrxml:151](webapp/report/BeritaAcaraUjianPSB.jrxml:151), [Coverspsbi.jrxml:223](webapp/report/Coverspsbi.jrxml:223), [sekolah/struk_pembayaran.jrxml:155](webapp/report/sekolah/struk_pembayaran.jrxml:155), [sekolah/deposit.jrxml:114](webapp/report/sekolah/deposit.jrxml:114)

### Referensi dokumentasi terkait

- [src/ais/action/servlet/api/DOKUMENTASI_API_CATATAN_AKTIFITAS.md:116](src/ais/action/servlet/api/DOKUMENTASI_API_CATATAN_AKTIFITAS.md:116) — dokumentasi resmi API yang justru **mencantumkan pola URL tak terenkripsi ini sebagai kontrak API**, jadi perlu diperbarui bila format berubah.

## Rekomendasi perbaikan (pilih salah satu, disusun dari yang paling minim-risiko-regresi)

1. **Tambah pengecekan kepemilikan/ACL di `process()`** untuk kombinasi `clazz`+`ref` yang berasal dari parameter mentah (bukan token `d`) — mis. verifikasi user login berhak atas entitas tsb sebelum stream. Tidak mengubah pemanggil yang ada, tapi butuh pemetaan aturan akses per `clazz` (siapa boleh lihat `CatatanSiswa` siswa mana, dst) — kerja paling besar tapi paling tuntas.
2. **Migrasi bertahap pemanggil Kategori A ke `FileFotoLain.ambilLinkLampiranLain(...)`** (token `d` terenkripsi) — mulai dari `CatatanApi.java` dan `SopService.java` karena data personalnya paling sensitif dan jumlah titik pemanggilan sedikit (mudah diuji). Kategori B (kop surat) bisa menyusul belakangan karena risikonya jauh lebih rendah.
3. Sebagai mitigasi sementara berbiaya rendah sebelum #1/#2 selesai: tambah **rate-limiting/audit-log** pada `process()` untuk pola akses `ref` berurutan/cepat (indikasi enumerasi), supaya percobaan IDOR di produksi setidaknya terdeteksi meski belum tertutup total.

## Riwayat terkait (sudah diperbaiki, disebut untuk konteks)

Dalam audit yang sama, dua celah lain pada servlet ini sudah diperbaiki:

- **Gap routing** `/al` tidak digerbangi auth (padahal `/AmbilLampiran` sudah) — ditutup dengan menambah `<intercept-url pattern="/al**" access="IS_AUTHENTICATED_REMEMBERED" />` di `webapp/WEB-INF/applicationContext-security.xml`.
- **Arbitrary file read** via field `file` di token `d` (`new File(filePath)` tanpa validasi direktori) — ditutup dengan `isDalamDirektoriDiizinkan(...)` di `AmbilLampiran.java` yang membatasi ke direktori media/webapp lewat canonical-path check.

Temuan IDOR di dokumen ini adalah bagian **ketiga** dari audit yang sama, sengaja dipisah karena blast radius-nya (23+ pemanggil) butuh keputusan/pekerjaan tersendiri.

## Sub-temuan terkait, SUDAH DIPERBAIKI: tabrakan jenis-namespace pada `lampiran_lain`

Ditemukan dan diperbaiki dalam sesi audit terpisah (2026-09-03, r83920–r83969), independen dari temuan IDOR parameter-mentah di atas tapi dalam keluarga akar-masalah yang sama: tabel fisik `lampiran_lain` (dipetakan oleh `LampiranLain`) dibaca lewat `LampiranLain.ambil(ref, jenis)` / `FileFotoLain.ambil(ref, jenis, LampiranLain.class)`, yang menyaring **hanya** berdasarkan pasangan `(ref, jenis)` — tanpa penanda kelas entitas pemilik apa pun di level query.

**Akar masalah:** seluruh keluarga `ParameterTambahan*Listener` (21 kelas) plus method render setara pada masing-masing kelas entity pemilik (`ais/database/model/*.java`), kelas Action-nya (`ais/action/master/*Action.java`), dan jalur cetak/laporannya (`ais/action/report/**/Laporan*.java`) membangun `jenis` dengan format telanjang `kelompokId + "->" + parameterId` — **tanpa nama kelas pemilik**. Karena tiap tabel kelompok/entity pemilik (CatatanSiswa, CatatanPegawai, Pengaduan, Pertemuan, Mahasiswa, dst. — 20-an entitas) punya sequence `IDENTITY` independen, dua entitas berbeda yang kebetulan punya `(ref, kelompokId, parameterId)` sama akan saling mengambil lampiran satu sama lain. Karena `/al` sudah anonim sejak 2026-08-19 (lihat Javadoc kelas `LampiranLain`), ini membuka jalur enumerasi yang **lebih murah** dari IDOR `usingId=true` yang sudah didokumentasikan di atas: `GET /al?usingId=false&ref=<id kecil>&jenis=<kelompokId>-><parameterId>`, kedua nilai adalah integer kecil yang mudah ditebak, tanpa perlu menebak nama kelas sama sekali (format lama tak pernah menyertakannya).

**Perbaikan:** method statis baru `LampiranLain.resolveJenisParameterTambahan(Class<?> ownerClass, Long ref, String jenisMentah)` ([LampiranLain.java](LampiranLain.java) — lihat direktori `ais/database/model/file/`) mengikuti konvensi aman yang sudah dipakai 70+ pemanggil lain di codebase ini (menyisipkan nama kelas pemilik ke `jenis`). Migrasi dipilih **tanpa penulisan ulang basis data**: dicoba dulu bentuk ber-namespace (`ownerClass.getName() + "#" + jenisMentah`), fallback ke bentuk lama tanpa-namespace bila baris pra-perbaikan masih ada untuk `ref` tsb — lampiran yang sudah terunggah tetap bisa diakses tanpa migrasi/downtime, sementara unggahan baru otomatis aman sejak commit ini.

Diterapkan di seluruh titik yang membangun `jenis` dengan format telanjang tsb: 21 kelas `ParameterTambahan*Listener` + `IsiAngketParameterUmumListener` (cabang `GrupChecklistPenilaianUmum`), 23 method setara pada kelas entity pemilik, 17 kelas Action, dan 14 kelas Laporan/cetak — total ~90 titik panggilan di ~59 berkas. Detail lengkap ada di riwayat commit r83920–r83969 (pesan tiap commit merujuk `task_b82b25d2`).

**Referensi silang:** `task_b82b25d2` (task tracking internal, lihat memory sesi audit `ais-audit-keamanan-temuan`).
