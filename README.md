# AIS — Academic Information System

**Sistem informasi & ERP Pendidikan (Enterprise Education) versi Java** yang melayani tiga produk sekaligus di atas satu basis kode:

- 🎓 **eCAMPUS** — manajemen perguruan tinggi (universitas, institut, akademi, sekolah tinggi)
- 🏫 **eSCHOOL** — manajemen sekolah & madrasah
- 🕌 **ePESANTREN** — manajemen pondok pesantren (kesantrian, asrama, diniyah, tahfiz)

AIS bukan tiga aplikasi terpisah, melainkan **satu platform terpadu** dengan fondasi bersama (identitas, hak akses, akuntansi, SDM, aset, tata kelola) yang modul-modulnya diaktifkan sesuai jenis lembaga. Inilah yang dimaksud dengan **Enterprise Education**: seluruh urusan pendidikan — akademik, keuangan, kepegawaian, mutu, penelitian, sampai unit usaha — disatukan menjadi modul-modul ERP yang saling terhubung dan berbagi data.

> Prinsip inti: **satu data dimasukkan sekali, dimanfaatkan di seluruh modul** — dari penerimaan peserta didik hingga pelaporan nasional dan akreditasi.

---

## Daftar Isi

1. [Tiga Produk dalam Satu Platform](#1-tiga-produk-dalam-satu-platform)
2. [Arsitektur & Teknologi](#2-arsitektur--teknologi)
3. [Struktur Repositori](#3-struktur-repositori)
4. [Peta Modul](#4-peta-modul)
5. [Integrasi Nasional & Pembayaran](#5-integrasi-nasional--pembayaran)
6. [Cara Membangun & Menjalankan](#6-cara-membangun--menjalankan)
7. [Konfigurasi](#7-konfigurasi)
8. [Keamanan](#8-keamanan)
9. [Kepemilikan](#9-kepemilikan)

---

## 1. Tiga Produk dalam Satu Platform

| Produk | Sasaran | Ciri khas modul |
|---|---|---|
| **eCAMPUS** | Perguruan tinggi | PMB, KRS/KHS, Kurikulum OBE, BKD, Penelitian & Pengabdian, SPMI, SPI, Akreditasi (LKPS/LAMDIK/SAPTO), Feeder/PDDikti, SISTER, Wisuda/Alumni |
| **eSCHOOL** | Sekolah & madrasah | PSB, kurikulum & jadwal, presensi, nilai & rapor, kesiswaan (pelanggaran/prestasi), antar-jemput, akreditasi sekolah |
| **ePESANTREN** | Pondok pesantren | Kesantrian, asrama, diniyah/tahfiz, perizinan, uang saku/kantin — di atas fondasi yang sama |

Ketiganya **berbagi fondasi tunggal**: identitas & keamanan (Spring Security), tata kelola (surat, SOP, tiket, dashboard), keuangan & akuntansi, SDM & penggajian, aset & persediaan, serta unit usaha (koperasi/kantin). Modul yang tampil bagi sebuah lembaga ditentukan oleh konfigurasi & hak akses, sehingga satu sistem dapat melayani sekolah, kampus, maupun pesantren — bahkan dalam satu yayasan multi-jenjang.

---

## 2. Arsitektur & Teknologi

Aplikasi web monolitik berbasis Java dengan lapisan berikut:

| Lapisan | Teknologi |
|---|---|
| **Bahasa** | Java |
| **Antarmuka (desktop web)** | **ZK / ZKoss** (`zk.jar`, `zkex.jar`, `zkplus.jar`, `zkfontawesome`) |
| **Antarmuka (mobile/publik)** | JSP + shell mobile |
| **Application layer** | **Spring 3.1** (Core, Context, AOP, Expression) |
| **Keamanan** | **Spring Security 3.1** (ACL, OpenID/Google login, taglibs, web) |
| **ORM / persistensi** | **Hibernate** (+ ehcache) |
| **Basis data** | **PostgreSQL** (utama), plus basis data pendukung (streaming/BLOB, radius, OJS, chat) |
| **Pelaporan** | **JasperReports** (berkas `.jrxml` di `webapp/report`) |
| **Runtime** | Servlet container (mis. Apache Tomcat); kelas hasil kompilasi diletakkan di `webapp/WEB-INF/classes` |

Karakteristik penting:

- **Multi-basis-data** — modul tertentu (streaming file/BLOB, radius, OJS, chat/Openfire) memakai koneksi Hibernate terpisah.
- **Multi-tenant/konfigurasi** — perilaku & modul disesuaikan per lembaga melalui tabel konfigurasi dan hak akses peran.
- **Cache in-JVM** — lapisan cache memori untuk data yang sering diakses (mis. ringkasan kampus/dashboard).
- **Berbasis peran (RBAC)** — menu, tombol, dan data dibatasi menurut peran pengguna.

---

## 3. Struktur Repositori

```
.
├── src/                       # Kode sumber Java
│   └── ais/
│       ├── action/            # Lapisan UI & alur (ZK controllers, helper, servlet, ws, mobile, report)
│       │   ├── master/        # Inti seluruh modul fungsional (lihat Peta Modul)
│       │   ├── maintenance/   # Login, dashboard utama, integrasi (Google/sosial), pemeliharaan
│       │   ├── mobile/        # Antarmuka mobile
│       │   ├── report/        # Pembuatan laporan (Jasper)
│       │   ├── servlet/       # Endpoint servlet
│       │   ├── ws/            # Web service
│       │   └── iso8583/       # Switching pembayaran (protokol ISO 8583)
│       ├── database/model/    # Entitas domain (Hibernate) per bidang
│       ├── hibernate*.cfg.xml # Konfigurasi koneksi (utama & pendukung)
│       └── applicationContext-business.xml
├── webapp/                    # Aplikasi web
│   ├── WEB-INF/               # web.xml, Spring context, pustaka (lib), kelas, halaman ZUL, bantuan
│   ├── component/             # Aset UI (assets, uiux, adminlte, dsb.)
│   ├── report/                # Definisi laporan JasperReports (.jrxml)
│   ├── img/  css/  js/  help/ # Sumber daya statis & bantuan per halaman
│   └── *.jsp                  # Halaman JSP (login, mobile, dsb.)
└── .gitignore
```

---

## 4. Peta Modul

Modul nyata berada di `src/ais/action/master/**` dengan entitas di `src/ais/database/model/**`. Dikelompokkan menurut fungsi:

### a. Akademik & Pembelajaran
- **pmb** — Penerimaan Mahasiswa Baru (eCampus)
- **psb** — Penerimaan Siswa Baru (eSchool)
- **kalender** — Kalender akademik
- **sekolah** — Fungsi khusus jenjang sekolah
- **kkn**, **pkl** — KKN & Praktik Kerja Lapangan
- **kursus** — Kursus & pelatihan
- **library** — Perpustakaan (katalog, keanggotaan, sirkulasi)

### b. Mutu, Kurikulum OBE & Akreditasi
- **obe** — Kurikulum berbasis capaian (PL → CPL → CPMK → Sub-CPMK, RPS, ketercapaian)
- **spmi** — Sistem Penjaminan Mutu Internal (siklus PPEPP, Audit Mutu Internal)
- **spi** — Satuan Pengawasan Internal (audit, temuan, tindak lanjut)
- **akreditasi**, **lkp**, **sapto** — Penyusunan borang/LKPS, LAMDIK, & integrasi akreditasi daring
- **kpi** — Indikator kinerja

### c. Penelitian, Pengabdian & Publikasi
- **penelitiandanpengabdian** — Pengelolaan penelitian & pengabdian
- **repository** — Repositori karya ilmiah
- **bkd** — Beban Kinerja Dosen (terhubung aktivitas tridharma)

### d. Keuangan & Akuntansi
- **akunting** — Akuntansi (jurnal, buku besar, laporan keuangan, penyusutan, kas)
- **payroll** — Penggajian
- **rab** — RAB & manajemen proyek/anggaran
- Tagihan (UKT/SPP) & pembayaran multi-kanal (lihat bagian Integrasi Pembayaran)

### e. SDM & Kepegawaian
- **employ** — Kepegawaian
- **recruitment** — Rekrutmen
- **kpi**, **bkd** — Penilaian kinerja

### f. Aset, Persediaan & Pengadaan
- **asset** — Aset & inventaris (BMN/BMD), pengadaan, pemeliharaan, penyusutan
- **inventory** — Persediaan barang

### g. Unit Usaha
- **koperasi** — Koperasi
- POS/Kantin (transaksi, pembayaran nontunai)

### h. Kesiswaan / Kemahasiswaan
- **pelanggaran** — Pelanggaran & kedisiplinan
- **prestasi**, **apresiasi** — Prestasi & penghargaan
- **beasiswa** — Beasiswa & keringanan biaya
- **antarjemput** — Layanan antar-jemput
- **alumni** — Data alumni & penelusuran lulusan

### i. Tata Kelola & Kolaborasi
- **surat**, **sirkulasisurat** — Tata kelola & sirkulasi surat (disposisi, arsip)
- **sop** — Prosedur operasi standar / alur kerja
- **ticket** — Ticketing/dukungan
- **dashboard** — Dasbor pimpinan (akademik, keuangan, SDM, mutu)
- **message**, **chat** — Notifikasi & percakapan
- **catatan**, **monitor** — Catatan & pemantauan

### j. Kanal & Integrasi Lain
- **mobile** — Aplikasi mobile
- **sosial** — Integrasi media sosial (Google/Facebook/Twitter/LinkedIn)
- **sirs** (+ model `kedokteran`) — Sistem Informasi Rumah Sakit / fakultas kedokteran
- **sisdes** — Sistem informasi desa (modul pendukung)
- **ojs** — Open Journal System (jurnal)

---

## 5. Integrasi Nasional & Pembayaran

**Pelaporan & integrasi pendidikan:**
- **feeder** — Feeder PDDikti (pelaporan PT)
- **epsbed** — EPSBED (pelaporan legacy)
- **sister** — SISTER (sumber daya dosen)
- **sapto** — Sistem Akreditasi Perguruan Tinggi Online

**Gerbang pembayaran (multi-channel):**
- Bank: **BNI, BRI, BSI, CIMB**
- Agregator: **Faspay, Finpay, iPaymu, Jatelindo, Doku**
- **iso8583** — switching pembayaran tingkat protokol (Virtual Account, QRIS, dsb.)

Integrasi ini memungkinkan tagihan (UKT/SPP) dibayar daring dengan konfirmasi otomatis, serta data akademik dilaporkan ke sistem nasional dari sumber data yang sama.

---

## 6. Cara Membangun & Menjalankan

> Ini aplikasi web Java klasik (ZK + Spring + Hibernate) yang dijalankan di servlet container.

Garis besar:

1. **Basis data** — siapkan PostgreSQL dan buat skema `ais` (serta basis data pendukung bila modul streaming/radius/ojs digunakan).
2. **Konfigurasi koneksi** — atur `src/hibernate.cfg.xml` (dan turunannya) melalui variabel yang telah dieksternalisasi (`${url}`, `${username}`, `${password}`).
3. **Kompilasi** — kompilasi kode `src/**` dengan classpath dari `webapp/WEB-INF/lib`, keluarkan `.class` ke `webapp/WEB-INF/classes`.
4. **Deploy** — jalankan direktori `webapp/` pada servlet container (mis. Apache Tomcat). Akses melalui peramban; antarmuka utama berbasis ZK, halaman mobile/publik berbasis JSP.

> Catatan: pustaka runtime disertakan di `webapp/WEB-INF/lib`. Sesuaikan versi JDK/servlet container dengan pustaka ZK/Spring/Hibernate yang dipakai.

---

## 7. Konfigurasi

- **`src/hibernate.cfg.xml`** — koneksi basis data utama (URL/kredensial dieksternalisasi via placeholder).
- **`src/hibernate.*.cfg.xml`** — koneksi basis data pendukung (streaming/BLOB, radius, OJS, openfire/chat).
- **`webapp/WEB-INF/applicationContext-security.xml`** — aturan Spring Security (autentikasi, otorisasi, OpenID/Google login).
- **`src/applicationContext-business.xml`** — wiring layanan bisnis.
- Konfigurasi fungsional lain (mis. modul aktif, tampilan, tarif) dikelola melalui tabel konfigurasi di basis data.

---

## 8. Keamanan

- Otentikasi & otorisasi berbasis **Spring Security** dengan peran berjenjang; menu/tombol/data dibatasi per peran.
- **Repositori bersifat privat.** Jangan menaruh kredensial produksi nyata di berkas yang dilacak Git.
- Beberapa berkas konfigurasi masih memuat nilai koneksi literal (mis. `webapp/WEB-INF/classes/hibernate.cfg.xml` dan blok koneksi lama yang dikomentari di `src/hibernate.cfg.xml`). **Disarankan menggantinya menjadi placeholder** dan memutar (rotate) kredensial bila pernah terekspos.
- Aktivitas penting dicatat untuk audit; data sensitif (mis. rekam medis pada modul kedokteran) berada pada lingkup akses terbatas.

---

## 9. Kepemilikan

Perangkat lunak internal milik **Zishof**. Seluruh hak cipta dilindungi. Penggunaan, penggandaan, atau distribusi memerlukan izin pemilik.

---

_README ini merangkum arsitektur dan modul berdasarkan struktur kode aktual pada branch `master` (paket `src/ais/action/master/**` dan `src/ais/database/model/**`)._
