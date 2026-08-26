# Handoff Website Institusi, Home, dan Rumah Sakit

Tanggal audit lanjutan: 25 Agustus 2026

## Ruang lingkup

Dokumen ini merangkum implementasi halaman utama publik, Website Institusi V4,
resolusi identitas multi-tenant, serta profil dan CRUD fasilitas kesehatan.
Source aktif berada di `C:\opt\AIS\ais`.

Instruksi terakhir pemilik sistem adalah tidak menjalankan compile atau build WAR
untuk pekerjaan ini. Karena itu status verifikasi dalam dokumen ini dibatasi pada
pemeriksaan statis source.

## Routing halaman publik

Servlet `ais.action.servlet.Index` mempertahankan urutan berikut:

1. `paksa_halaman_utama_menggunakan_skin=AKTIF` memakai skin
   `/WEB-INF/j/index.jsp` dan menolak request dengan HTTP 503 bila skin wajib tetapi
   file tidak tersedia.
2. `default_login_ke_ebisnis=AKTIF` membuka `/WEB-INF/baru/ebisnis.jsp`.
3. `default_login_ke_erp=AKTIF` membuka `/WEB-INF/baru/erp.jsp`.
4. `default_home_versi_baru=AKTIF` membuka `/WEB-INF/baru/home.jsp`.
5. `default_home_login_versi_baru=AKTIF` membuka `/WEB-INF/baru/login2.jsp`.
6. Jika tidak ada konfigurasi pemaksa dan skin tersedia, skin didahulukan.
7. Jika skin tidak tersedia, fallback adalah `/WEB-INF/baru/home.jsp`.

Halaman website resmi institusi tetap terpisah pada route `/web`, ditangani oleh
`ais.action.servlet.Web`. Website V4 aktif secara default melalui
`website_ui_v4=AKTIF`; tampilan lama hanya dipakai jika flag dinonaktifkan atau V4
gagal dibangun.

## Prioritas identitas dan tema tenant

`HomePortalInstitutionResolver` menerapkan prioritas:

1. Rumah Sakit/fasilitas kesehatan dengan ID valid.
2. Sekolah dengan ID valid.
3. Yayasan dengan ID valid ketika mode sekolah/yayasan aktif.
4. Perguruan Tinggi dengan ID valid.
5. Fallback identitas institusi.

Resolver mengisi nama, motto, alamat, telepon, email, logo, background, CSS tema,
warna utama, dan kategori institusi. Objek kosong tanpa ID tidak dianggap tenant.

`/WEB-INF/baru/home.jsp` kini memakai resolver yang sama. Karena itu home tidak lagi
selalu mengambil profil Perguruan Tinggi. Judul produk menjadi eMedic, eSchool,
Yayasan/Pesantren, atau eCampus sesuai tenant. Modul eMedic juga memiliki grup
khusus dan modul pendidikan tidak ditampilkan pada tenant kesehatan.

## Website Institusi V4

File utama:

- `src/main/java/ais/action/servlet/Web.java`
- `src/main/java/ais/common/home/HomePortalService.java`
- `src/main/java/ais/common/home/HomePortalContentService.java`
- `src/main/webapp/WEB-INF/baru/website/home.jsp`
- `src/main/webapp/css/baru/website-v4.css`
- `src/main/webapp/js/baru/website-v4.js`

Fitur yang tersedia:

- Identitas dan terminologi kondisional untuk kampus, sekolah/yayasan, dan fasilitas
  kesehatan.
- Hero satu kolom; panel visual kanan yang berantakan sudah tidak dirender.
- Headline responsif dengan batas ukuran lebih kecil.
- Program, penerimaan, berita, agenda, dampak, layanan digital, dan kontak.
- Navigasi mobile dengan fokus, Escape, overlay, dan focus trap.
- SEO, Open Graph, canonical, JSON-LD, skip link, reduced motion, serta target/rel
  aman untuk tautan eksternal.
- Tautan Google Play, App Store, dan versi Desktop ditampilkan jika URL tersedia.
- URL aplikasi pendidikan dan eMedic dipisahkan agar tenant kesehatan tidak
  mengiklankan aplikasi eCampus secara tidak sengaja.

Perbaikan audit 25 Agustus 2026:

- Query gelombang penerimaan Perguruan Tinggi sekarang wajib memakai ID tenant.
  Sebelumnya gelombang aktif milik kampus lain dapat terpilih.
- Atribut target dan rel dilengkapi pada pintasan persona serta tombol portal.
- Blok aplikasi resmi ditambahkan ke Website V4 dan dibuat responsif.
- Konfigurasi URL Google Play, App Store, dan Desktop ditambahkan untuk Website V4
  dan Home V3, termasuk key eMedic tersendiri.
- Selector mobile untuk visual hero yang sudah dihapus tidak lagi digunakan.

## Home modern dan aplikasi

`/WEB-INF/baru/home.jsp` sekarang:

- memakai profil/tema hasil resolver multi-tenant;
- menampilkan grup modul eCampus, eSchool/Yayasan, atau eMedic secara kondisional;
- tidak menampilkan eKantin, kursus, karier, pustaka, dan portal rekanan pada tenant
  kesehatan;
- memakai Font Awesome dan JavaScript Bootstrap lokal;
- memperbaiki posisi include pemilih bahasa yang sebelumnya masuk ke dalam atribut
  `style` elemen `body`;
- menampilkan label yang benar: Google Play, App Store, dan Versi Desktop;
- memakai tautan rilis Desktop
  `https://github.com/Zishof/ecampus-eschool-releases/releases/latest`;
- tidak menampilkan tautan aplikasi pendidikan pada tenant kesehatan.

Home V3 tetap tersedia sebagai paket source pada:

- `src/main/webapp/WEB-INF/baru/home-v3.jsp`
- `src/main/webapp/WEB-INF/baru/home/`
- `src/main/webapp/css/baru/home-v3.css`
- `src/main/webapp/js/baru/home-v3.js`

Routing aktual tetap mengikuti ketentuan pemilik sistem di atas dan tidak diubah
diam-diam ke Home V3.

## Model dan CRUD Rumah Sakit

File utama:

- `src/main/java/ais/database/model/sirs/RumahSakit.java`
- `src/main/java/ais/action/master/sirs/RumahSakitAction.java`
- `src/main/java/ais/action/master/sirs/util/RumahSakitUtil.java`
- `src/main/webapp/WEB-INF/z/x/y/pages/master/sirs/rumah_sakit.zul`
- `src/main/java/hibernate.cfg.xml`
- `src/main/java/ais/common/MenuSnapshotData.java`

Model mencakup Rumah Sakit, Puskesmas, Posyandu, Klinik, praktik mandiri,
laboratorium, apotek, dan fasilitas kesehatan lainnya. Properti
`labelJenisFasilitas` berstatus `@Transient`, sehingga Hibernate tidak mencari
setter dan error `PropertyNotFoundException` tidak berulang.

CRUD menyediakan pencarian, tambah, ubah, hapus, validasi nama/domain, jenis
fasilitas, profil, CSS, warna, status, dan pilihan tampilan. Menu snapshot
`Profil Fasilitas Kesehatan` mengarah ke
`/pages/master/sirs/rumah_sakit.zul`.

Perbaikan domain pada audit lanjutan:

- cache session divalidasi terhadap host request;
- cache tenant lama dibuang bila host tidak lagi cocok;
- pencocokan hanya menerima domain tepat atau subdomain, bukan `contains`;
- domain bertitik di akhir dan awalan `www.` dinormalisasi;
- beberapa domain dapat dipisahkan koma, titik koma, atau spasi;
- format domain divalidasi saat simpan;
- domain yang tumpang tindih dengan fasilitas lain ditolak.

## Hibernate

Mapping `ais.database.model.sirs.RumahSakit` tersedia satu kali pada kedua source
mirror `src/main/java/hibernate.cfg.xml` dan `src/main/src/hibernate.cfg.xml`.
Tidak ada script `CREATE TABLE` atau `ALTER TABLE` khusus Rumah Sakit. Pembuatan
dan penyesuaian tabel diserahkan kepada konfigurasi schema update Hibernate saat
aplikasi dijalankan.

Source mirror `src/main/java` dan `src/main/src` untuk file Java yang disentuh
telah disinkronkan byte-for-byte.

## Yang belum dapat diselesaikan tanpa lingkungan deployment

Pekerjaan berikut membutuhkan target dan data nyata, bukan perubahan source lokal:

- menjalankan Hibernate terhadap database staging untuk memastikan tabel utama dan
  tabel audit Envers terbentuk;
- UAT domain nyata untuk Rumah Sakit, Sekolah, Yayasan, dan Perguruan Tinggi;
- memastikan file logo, background, dan CSS tenant benar-benar tersedia pada media
  deployment;
- menguji skin ZIP nyata beserta seluruh kombinasi flag routing;
- smoke test screen reader, zoom 200%, dan widget bantuan;
- Lighthouse serta screenshot aktual dari server yang menjalankan revisi terbaru;
- deployment WAR ke staging atau production.

Tidak ada alamat server, kredensial, prosedur deploy, atau database staging yang
diberikan dalam sesi ini. Deployment tidak boleh diasumsikan.

## Checklist UAT yang disarankan

1. Buat empat tenant dengan domain berbeda: Rumah Sakit, Sekolah, Yayasan, dan PT.
2. Akses `/`, `/web`, `/login`, dan `/new` pada tiap domain.
3. Pastikan profil, warna, logo, background, dan istilah tidak tertukar.
4. Pastikan penerimaan kampus hanya menampilkan gelombang milik PT tersebut.
5. Uji domain tepat, `www`, subdomain, domain tidak dikenal, serta domain ganda.
6. Uji CRUD tambah, ubah, hapus, domain duplikat, CSS tidak valid, dan warna tidak
   valid.
7. Uji routing skin dengan seluruh flag aktif/nonaktif sesuai urutan routing.
8. Periksa seluruh tautan internal, eksternal, Google Play, App Store, dan Desktop.
9. Jalankan pemeriksaan aksesibilitas, responsive viewport, dan Lighthouse.
10. Setelah lolos UAT, baru lakukan build WAR dan deployment sesuai prosedur resmi.

