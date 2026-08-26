# Gap Analysis Website Perguruan Tinggi dan Sekolah

Tanggal analisis: 25 Agustus 2026  
Ruang lingkup: Website Institusi V4 (`/web`) dan arsitektur home portal bersama di AIS  
Metode: audit statis kode, pemetaan pengalaman pengguna, tata kelola konten, SEO, aksesibilitas, keamanan, kinerja, serta perbandingan dengan standar resmi. Tidak dilakukan kompilasi, build WAR, deployment, atau pengujian produksi.

## 1. Ringkasan eksekutif

Website V4 sudah memiliki fondasi visual dan tenant awareness yang layak: identitas institusi dinamis, terminologi perguruan tinggi/sekolah/fasilitas kesehatan, CTA penerimaan, program, berita, agenda, layanan digital, kontak, SEO dasar, navigasi seluler, skip link, dan fokus keyboard.

Namun, bentuk saat ini masih lebih dekat ke **landing page institusi** daripada **website resmi perguruan tinggi/sekolah yang lengkap**. Jurang terbesar bukan pada estetika, melainkan pada:

1. arsitektur informasi dan halaman detail;
2. kedalaman informasi akademik dan penerimaan;
3. keterbukaan informasi, legal, privasi, serta kebijakan;
4. pencarian, bahasa, direktori, dan layanan berbasis persona;
5. tata kelola CMS dan siklus hidup konten;
6. SEO teknis dan data terstruktur per entitas;
7. keamanan header, observabilitas, caching, dan pengujian kualitas otomatis.

Estimasi kematangan berdasarkan audit kode (bukan skor produksi): **1,8 dari 5 / sekitar 36%**. Website cukup untuk memperkenalkan institusi dan memberi jalan masuk ke aplikasi, tetapi belum cukup sebagai sumber informasi resmi, pusat layanan publik, alat rekrutmen, dan kanal reputasi institusi.

### Keputusan strategis yang disarankan

- Pertahankan V4 sebagai beranda, tetapi ubah arsitektur menjadi **beranda + kumpulan halaman entitas dan hub layanan**, bukan menambah semua isi ke satu halaman.
- Gunakan satu platform multi-tenant, tetapi pisahkan paket konten dan terminologi untuk perguruan tinggi, sekolah, yayasan, dan fasilitas kesehatan.
- Jadikan data akademik AIS sebagai sumber data terstruktur; tambahkan CMS untuk narasi, media, halaman kebijakan, serta workflow editorial.
- Targetkan WCAG 2.2 AA, Core Web Vitals “good”, SEO teknis lengkap, dan security baseline di tingkat filter/middleware.

## 2. Kondisi saat ini yang sudah baik

| Area | Kemampuan saat ini | Nilai |
|---|---|---|
| Multi-tenant | Identitas rumah sakit, sekolah, yayasan, atau perguruan tinggi dipilih menurut tenant/domain | Fondasi kuat |
| Adaptasi istilah | Mahasiswa/dosen/PMB, siswa-guru/PPDB, atau pasien-tenaga kesehatan | Baik, masih perlu diperluas ke seluruh copy |
| Konten dinamis | Program, penerimaan, berita, agenda, statistik, dan layanan digital dapat berasal dari data/config | Awal yang baik |
| Penerimaan | Gelombang aktif dibaca sesuai tenant perguruan tinggi/sekolah | Berguna, belum menjadi funnel lengkap |
| Navigasi seluler | Drawer, overlay, Escape, dan pengelolaan fokus tersedia | Baik |
| Aksesibilitas dasar | Skip link, focus-visible, reduced motion, struktur section | Positif, belum teruji penuh |
| SEO dasar | title, description, canonical, Open Graph dasar, JSON-LD organisasi | Minimum viable |
| Ketahanan | Kegagalan query menyembunyikan section dan V4 dapat fallback ke website lama | Menjaga availability, tetapi berisiko menyamarkan regresi |
| Ekosistem layanan | Tautan eCampus/eSchool, PMB/PPDB, pustaka, repository, alumni, karier, dokumen, kantin, dan lain-lain | Diferensiasi yang kuat |

## 3. Skor kematangan per kapabilitas

Skala: 1 = belum tersedia, 2 = dasar, 3 = memadai, 4 = matang, 5 = unggul. Skor ini berasal dari artefak kode yang tersedia, bukan audit terhadap URL produksi dan bukan pengganti uji pengguna.

| Kapabilitas | Skor | Alasan utama |
|---|---:|---|
| Identitas tenant dan branding | 3,0 | Dinamis dan cukup konsisten; belum ada design token/brand governance per tenant |
| Arsitektur informasi | 1,5 | Hampir seluruh pengalaman masih satu halaman dan anchor |
| Informasi program/kurikulum | 2,0 | Ada kartu program/kelas, tidak ada halaman detail dan struktur kurikulum |
| Penerimaan PMB/PPDB | 2,0 | Ada CTA dan gelombang aktif, belum ada persyaratan, biaya, beasiswa, FAQ, deadline, tracking funnel |
| Berita dan agenda | 2,0 | Daftar ringkas tersedia; belum ada detail, kategori, arsip, penulis, berbagi, Event schema |
| Layanan berbasis persona | 2,5 | Persona dan aplikasi digital cukup kaya; kebutuhan informasi persona belum lengkap |
| Pencarian dan navigasi | 1,0 | Flag tersedia, UI/fungsi pencarian belum dirender; tidak ada breadcrumb/mega menu |
| Multibahasa | 1,0 | `lang` dan `dir` ada, tetapi copy Indonesia hard-coded dan tidak ada URL lokal/hreflang |
| Aksesibilitas | 2,5 | Fondasi keyboard baik; belum ada audit WCAG AA, statement, caption, atau regression suite |
| SEO teknis | 2,0 | Metadata dasar ada; sitemap, robots, detail metadata, dan schema entitas belum ada |
| Kinerja | 2,0 | Aset relatif ringan; beberapa query DB per request, tanpa cache halaman/data dan strategi gambar lengkap |
| Keamanan dan privasi publik | 1,5 | Sanitasi link/escape HTML ada; header publik, privacy center, dan consent analytics belum lengkap |
| CMS dan editorial governance | 1,0 | Config dan data operasional dipakai langsung; belum ada workflow editorial lengkap |
| Analytics, observabilitas, QA | 1,0 | Error dicatat, tetapi belum tampak KPI publik, RUM, synthetic monitoring, broken-link/SEO/a11y CI |

## 4. Gap analysis mendalam

### A. Arsitektur informasi dan navigasi

**Kondisi sekarang**

- Navigasi utama mengarah ke anchor seperti program, penerimaan, layanan, informasi, dan kontak.
- Kartu program, berita, dan agenda sering kembali ke anchor yang sama karena URL detail default belum tersedia.
- Persona sudah dikenali, tetapi hanya menjadi jalan pintas ke section/aplikasi.
- `showSearch`, `showLanguage`, dan `showPartners` diisi oleh service, tetapi belum dirender pada JSP V4.

**Gap dan dampak**

- Pengunjung tidak dapat melakukan eksplorasi mendalam atau membagikan URL spesifik.
- Mesin pencari hanya menerima sedikit halaman yang dapat diindeks.
- Informasi penting terkubur dan sulit ditemukan oleh calon mahasiswa/siswa, orang tua, dosen/guru, alumni, mitra, media, dan publik.
- Tidak ada breadcrumb, indeks A–Z, sitemap HTML, halaman 404 berorientasi tugas, atau navigasi sekunder.

**Rekomendasi**

1. Bangun route detail dan listing: `/program`, `/program/{slug}`, `/berita`, `/berita/{slug}`, `/agenda`, `/agenda/{slug}`, `/penerimaan`, `/unit/{slug}`, `/staf/{slug}`, dan `/dokumen`.
2. Gunakan mega menu ringkas berbasis tugas dan persona, bukan daftar organisasi yang terlalu dalam.
3. Implementasikan site search dengan indeks tenant-scoped, filter tipe konten, typo tolerance, highlight, dan halaman no-result yang membantu.
4. Tambahkan breadcrumb pada semua halaman level dua atau lebih.
5. Sediakan quick links yang dapat dikelola editor dan berbeda per tenant/persona.

**Prioritas:** P0 untuk routing/IA; P1 untuk search dan breadcrumb.

### B. Profil, kepercayaan, dan reputasi institusi

**Belum tercakup secara memadai**

- profil, sejarah, visi-misi, nilai, pimpinan, struktur organisasi, yayasan/badan penyelenggara;
- status dan sertifikat akreditasi institusi/program;
- nomor identitas resmi seperti kode perguruan tinggi, NPSN, izin operasional;
- laporan tahunan, rencana strategis, indikator mutu, capaian, dan data alumni/lulusan;
- alamat kampus/unit yang lebih dari satu, peta, transportasi, akses disabilitas, dan kontak unit;
- penghargaan, mitra, testimoni terverifikasi, dan penggunaan logo mitra yang terkelola.

**Rekomendasi model konten**

- `InstitutionProfile`, `Leadership`, `OrganizationUnit`, `Accreditation`, `CampusLocation`, `AnnualReport`, `Achievement`, dan `Partner`.
- Setiap fakta penting memiliki sumber, tanggal berlaku, pemilik konten, dan tanggal review berikutnya.
- Tampilkan tautan verifikasi ke sumber resmi yang relevan, bukan sekadar badge gambar.

**Prioritas:** P1. Informasi identitas/legal minimum masuk P0.

### C. Perguruan tinggi: akademik, riset, dan kehidupan kampus

**Gap akademik**

- Tidak ada hierarki fakultas → program studi → kurikulum → mata kuliah.
- Halaman program belum memuat jenjang, gelar, profil lulusan, capaian pembelajaran, kurikulum, durasi, mode kuliah, akreditasi, biaya, jalur masuk, fasilitas, dosen, dan kontak prodi.
- Belum ada direktori dosen/tenaga kependidikan, profil keahlian, publikasi, ORCID/Google Scholar/SINTA, dan jadwal konsultasi.
- Kalender akademik hanya menjadi agenda ringkas dan belum berupa halaman/unduhan yang jelas.

**Gap riset dan pengabdian**

- “Riset & Inovasi” baru berupa kartu generik.
- Belum ada pusat riset, proyek, hibah, publikasi, laboratorium, kekayaan intelektual, kerja sama, pengabdian masyarakat, dan dampak berbasis bukti.

**Gap pengalaman kampus**

- Belum ada kemahasiswaan, organisasi, UKM, beasiswa, asrama, layanan kesehatan/konseling, fasilitas, perpustakaan, aksesibilitas kampus, virtual tour, internasional, alumni, karier, dan hasil tracer study yang dipublikasikan.

**Rekomendasi utama**

- Jadikan program studi sebagai entitas pusat dan hubungkan ke fakultas, staf, kurikulum, penerimaan, berita, agenda, fasilitas, serta outcome.
- Pisahkan konten institusional dan data transaksional; sinkronkan data inti dari AIS, tetapi izinkan editor prodi menambah narasi dan media.
- Sediakan direktori yang dapat difilter berdasarkan unit, bidang keahlian, dan peran.

**Prioritas:** P1 untuk program/fakultas/direktori; P2 untuk riset, kehidupan kampus, internasional, alumni, dan outcome.

### D. Sekolah/pesantren: pembelajaran, orang tua, dan perlindungan anak

**Belum tercakup**

- jenjang/kelas, kurikulum, pendekatan pembelajaran, kalender pendidikan, jadwal, hasil belajar agregat, dan profil lulusan;
- direktori pimpinan, guru, wali kelas, BK, tenaga kependidikan, dan kanal kontak resmi;
- PPDB lengkap: kuota, zonasi/jalur, usia, syarat, biaya, potongan, beasiswa, jadwal, seleksi, daftar ulang, FAQ, open house;
- fasilitas, ekstrakurikuler, prestasi, galeri, kegiatan, transportasi, seragam, makan/asrama untuk tenant yang relevan;
- pusat orang tua: kalender, kebijakan, formulir, panduan, pembayaran, komunikasi, dan bantuan;
- kebijakan perlindungan anak, anti-perundungan, keamanan digital, penanganan keluhan, keadaan darurat, kesehatan, dan safeguarding;
- komite sekolah, penggunaan BOS/transparansi bila berlaku, dan dokumen publik.

**Masalah terminologi saat ini**

- Section “Riset & Dampak” dan kartu “Riset & Inovasi” aktif secara default juga untuk sekolah, sehingga kurang sesuai konteks.
- Program sekolah dibaca dari `KelasSiswa`, yang lebih tepat sebagai rombongan/kelas operasional daripada katalog program pendidikan publik.

**Rekomendasi**

- Tambahkan preset tenant sekolah: `Pembelajaran & Prestasi`, bukan `Riset & Dampak`.
- Bentuk entitas publik `EducationProgram`/`GradeLevel` terpisah dari kelas operasional siswa.
- Sediakan `Parent Resource Center` dan `Safeguarding & Student Wellbeing` sebagai hub wajib.

**Prioritas:** P0 untuk koreksi terminologi; P1 untuk PPDB/program/staf; P2 untuk parent center, safeguarding, ekstrakurikuler, dan fasilitas.

### E. Penerimaan PMB/PPDB sebagai funnel

**Kondisi sekarang**

- Website menampilkan satu gelombang aktif dan CTA ke PMB/PPDB.
- Alur visual tiga langkah bersifat generik.

**Gap**

- Tidak ada halaman jalur penerimaan, persyaratan, biaya, beasiswa, kuota, tanggal penting, dokumen unduhan, FAQ, kontak konselor, visit/open house, dan status layanan.
- Tidak ada pemetaan CTA per program/jalur/persona atau deep link ke formulir yang tepat.
- Tidak ada pengukuran funnel: view program → mulai daftar → submit → verifikasi → daftar ulang.
- Bila query gagal, section bisa hilang tanpa pesan operasional yang jelas.

**Rekomendasi**

- Buat hub penerimaan dengan deadline terstruktur dan status buka/tutup.
- Hubungkan tiap program ke jalur, biaya, beasiswa, dan CTA yang tepat.
- Tambahkan FAQ terstruktur, checklist dokumen, estimasi proses, kontak bantuan konsisten, dan notifikasi perubahan.
- Ukur konversi secara consent-aware dan tanpa menyimpan data sensitif di layer analytics.

**Prioritas:** P0 untuk informasi minimum dan deadline; P1 untuk funnel lengkap.

### F. Berita, agenda, dokumen, dan editorial CMS

**Kondisi sekarang**

- Berita mengambil maksimal tiga pengumuman umum; agenda maksimal empat entri; program maksimal enam.
- Data ditampilkan ringkas tanpa halaman detail yang nyata.
- Yayasan dapat memiliki berita, tetapi program yayasan tidak diambil dinamis.
- Agenda perguruan tinggi memakai kondisi `sekolah is null and yayasan is null`; karena entitas kalender tidak tampak memiliki relasi perguruan tinggi, agenda berisiko tercampur antar tenant kampus.

**Gap CMS**

- draft, review, approval, scheduled publish/unpublish, revision history, rollback;
- penulis, editor, unit pemilik, sumber, tanggal review, masa berlaku;
- slug, redirect saat slug berubah, preview, related content, taxonomy;
- media library, crop/focal point, lisensi/kredit, alt text wajib;
- arsip, kategori, tag, featured/pinned, dan kalender editorial;
- deteksi broken link, konten kedaluwarsa, dan orphan page.

**Rekomendasi**

- Tambahkan modul CMS tenant-scoped dengan role `Author`, `Reviewer`, `Publisher`, dan `Administrator`.
- Pisahkan pengumuman akademik internal dari artikel publik.
- Tambahkan tenant perguruan tinggi eksplisit pada agenda atau sumber kalender alternatif yang scoped.
- Terapkan content freshness SLA: penerimaan harian saat masa aktif; berita 90 hari; profil/program per semester; kebijakan minimal tahunan.

**Prioritas:** P0 untuk perbaikan scope agenda; P1 untuk detail pages/workflow; P2 untuk governance automation.

### G. Multibahasa

**Kondisi sekarang**

- `language` dan `direction` tersedia, tetapi sebagian besar label/copy di JSP masih hard-coded dalam bahasa Indonesia.
- Flag pemilih bahasa default aktif, tetapi kontrolnya tidak dirender.

**Gap**

- Tidak ada URL per bahasa, `hreflang`, `x-default`, localized slug, status terjemahan, atau fallback editorial.
- Risiko halaman beratribut bahasa tertentu tetapi isi utama berbeda bahasa.

**Rekomendasi**

- Gunakan URL eksplisit, misalnya `/id/program/...` dan `/en/program/...`.
- Simpan versi terjemahan per entitas, bukan hanya menerjemahkan template.
- Render switcher yang mempertahankan halaman ekuivalen dan jangan memaksa redirect berbasis browser.
- Emit self-referential canonical dan reciprocal `hreflang` untuk setiap versi yang benar-benar tersedia.

**Prioritas:** P1. Jangan menampilkan switcher sebelum konten kedua siap.

### H. SEO dan discoverability

**Kondisi sekarang**

- Ada title, description, canonical, OG title/description/image/url, dan schema organisasi.
- Canonical berasal langsung dari `request.getRequestURL()`.

**Gap teknis**

- tidak ada `robots.txt` dan sitemap institusi dinamis;
- manifest yang tersedia adalah milik POS Kantin, bukan website institusi;
- tidak ada metadata detail per program/berita/agenda karena detail pages belum ada;
- belum ada `BreadcrumbList`, `Article`/`NewsArticle`, `Event`, `Course`/program, FAQ yang sesuai kebijakan, dan relasi organisasi/unit;
- tidak ada OG locale/site_name dan social card lengkap;
- canonical bergantung pada host/scheme request, sehingga perlu kehati-hatian di reverse proxy dan terhadap host header;
- tidak ada redirect map, monitoring 404, Search Console workflow, atau RSS/Atom untuk publikasi.

**Rekomendasi**

- Tambahkan konfigurasi `public_base_url` tervalidasi per tenant sebagai sumber canonical, sitemap, dan schema URL.
- Generate sitemap index tenant-scoped dari halaman yang published dan canonical.
- Tambahkan robots yang merujuk sitemap; gunakan `noindex` atau autentikasi untuk halaman nonpublik, bukan robots sebagai kontrol akses.
- Terapkan schema per halaman dan validasi dengan Rich Results Test/Schema validator.
- Sediakan redirect permanen saat URL berubah dan dashboard 404/broken link.

**Prioritas:** P0 untuk canonical/sitemap/robots; P1 untuk schema detail.

### I. Aksesibilitas dan inklusi

**Kekuatan saat ini**

- skip link, focus-visible, navigasi drawer dengan Escape/focus trap, semantic sections, serta reduced motion.

**Gap yang perlu audit manual**

- target WCAG belum dinyatakan dan belum ada accessibility statement;
- belum ada pengujian keyboard penuh, screen reader, zoom 200–400%, reflow 320 CSS px, forced colors, dan perangkat sentuh;
- gambar berita memakai alt kosong; perlu dibedakan gambar dekoratif dan informatif;
- tautan membuka tab baru belum memberi indikasi yang mudah dipahami;
- belum ada proses caption, transcript, audio description, serta quality gate untuk media;
- belum ada validasi contrast seluruh state, target size, focus not obscured, link purpose, heading hierarchy, dan language of parts;
- form penerimaan/login berada di aplikasi lain dan perlu diuji sebagai proses lengkap, bukan hanya halaman beranda.

**Rekomendasi**

- Tetapkan WCAG 2.2 Level AA sebagai acceptance criterion.
- Tambahkan accessibility statement, kanal pelaporan hambatan, pemilik tindak lanjut, dan tanggal review.
- Gunakan kombinasi axe/Lighthouse otomatis dan pengujian manual NVDA + keyboard + zoom.
- Jadikan alt text, caption, heading, dan label sebagai validasi CMS, bukan hanya panduan editor.

**Prioritas:** P0 untuk audit dan perbaikan kritis; P1 untuk pipeline regresi.

### J. Keamanan, privasi, dan kepatuhan

**Kondisi sekarang**

- Output teks di JSP umumnya di-escape dan link melewati resolver.
- Endpoint `/web` belum memasang baseline header keamanan yang terlihat pada servlet.
- JSON-LD dan sebagian style inline akan memengaruhi desain CSP.

**Gap**

- belum konsisten: CSP, HSTS pada deployment HTTPS, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`, frame protection, serta penghapusan header versi server;
- canonical dan URL absolut perlu mengambil base URL tepercaya, bukan host request mentah;
- belum ada pusat privasi, kebijakan data, syarat penggunaan, retensi, hak subjek data, kontak pengendali, dan pencatatan consent bila analytics/cookie non-esensial dipakai;
- belum ada kebijakan publikasi foto/video siswa dan pengelolaan data anak;
- fallback diam-diam dapat menutupi masalah keamanan/ketepatan tenant jika hanya dipantau melalui tampilan pengguna.

**Rekomendasi**

- Terapkan header melalui filter global agar konsisten; mulai CSP dalam mode report-only, hilangkan inline style/script atau gunakan nonce/hash.
- Validasi trusted proxy dan host allowlist per tenant.
- Bentuk Privacy Center dan inventory pemrosesan data; hindari cookie banner bila memang tidak ada cookie non-esensial, tetapi gunakan consent yang benar bila analytics/marketing diaktifkan.
- Lakukan threat modeling untuk route publik, upload media, pencarian, preview CMS, redirect, dan integrasi formulir.

**Catatan regulasi:** kewajiban spesifik perlu dipastikan oleh penanggung jawab hukum institusi. UU 27/2022 tentang Pelindungan Data Pribadi relevan bagi pemrosesan data pribadi. Untuk institusi yang termasuk Badan Publik, UU 14/2008 dan PerKI 1/2021 menjadi acuan layanan informasi publik.

**Prioritas:** P0.

### K. Keterbukaan informasi dan PPID

**Gap**

- Tidak ada hub PPID/keterbukaan informasi yang terstruktur.
- Tidak ada klasifikasi informasi berkala, serta-merta, tersedia setiap saat, dan informasi dikecualikan.
- Belum ada profil PPID, Daftar Informasi Publik, SOP, maklumat layanan, formulir permohonan, keberatan, register, waktu/biaya layanan, laporan layanan, pengadaan, laporan keuangan, serta kontak resmi.

**Rekomendasi**

- Aktifkan paket PPID hanya bagi tenant yang termasuk/ingin mengikuti standar Badan Publik, tetapi sediakan modulnya di platform.
- Buat content type `PublicInformationDocument` dengan klasifikasi, tahun, unit pemilik, versi, status, format aksesibel, dan tanggal pembaruan.
- Sediakan formulir dan tracking permohonan pada sistem yang aman; jangan mengumumkan data pemohon.

**Prioritas:** P0 untuk scope legal dan halaman dasar; P1 untuk workflow penuh.

### L. Kinerja dan skalabilitas

**Kondisi sekarang**

- Setiap render dapat membuka session/query terpisah untuk program, berita, penerimaan, dan agenda.
- Tidak terlihat cache hasil publik atau response caching.
- Gambar berita lazy-loaded, tetapi belum ada strategi dimensi/responsive image yang menyeluruh.
- `heroUrl` tetap dimuat ke model meski visual hero V4 saat ini tidak menggunakannya.

**Gap**

- TTFB dapat tumbuh seiring beban dan latensi DB.
- Gambar tanpa ukuran intrinsik konsisten dapat menaikkan CLS.
- Belum ada WebP/AVIF, `srcset`/`sizes`, image proxy/CDN, caching aset/HTML, ETag, atau stale-while-revalidate yang terukur.
- Belum ada RUM untuk LCP, INP, CLS dan performance budget CI.

**Rekomendasi**

- Agregasikan query dalam satu read transaction/session dan cache view model publik per tenant dengan invalidasi saat publish/config berubah.
- Terapkan cache asset immutable; untuk HTML publik gunakan TTL pendek + stale-while-revalidate bila arsitektur mendukung.
- Bangun image pipeline dengan ukuran eksplisit, varian responsif, format modern, focal point, dan fallback.
- Target lapangan persentil ke-75: LCP ≤2,5 detik, INP ≤200 ms, CLS ≤0,1, terpisah mobile/desktop.

**Prioritas:** P1 untuk cache/dimensi gambar; P2 untuk CDN/RUM lanjutan.

### M. Reliability, analytics, dan operasi

**Gap**

- Tidak ada synthetic check tenant/domain, monitoring status section, atau alert saat data publik kosong/tidak segar.
- Fallback V4 → website lama menjaga halaman hidup, tetapi dapat membuat regresi tidak terlihat.
- Belum ada dashboard konten, SEO, search terms, broken link, 404, konversi penerimaan, atau kepuasan tugas pengguna.
- Belum ada suite visual regression, contract test tenant scoping, accessibility CI, dan schema validation.

**Rekomendasi**

- Tambahkan request/correlation ID dan metrik per tenant/section/query, tanpa mengekspos data pribadi.
- Alert bila fallback terjadi, bukan hanya merekam error.
- Synthetic test minimal untuk tenant perguruan tinggi, sekolah, yayasan, dan kesehatan.
- Definisikan analytics event dictionary dan governance; hormati consent, DNT/GPC bila relevan, serta minimalkan data.

**Prioritas:** P0 untuk alert fallback dan tenant-scoping tests; P1 untuk dashboard/QA pipeline.

## 5. Arsitektur informasi target

### Struktur bersama

```text
Beranda
├── Tentang
│   ├── Profil, sejarah, visi-misi
│   ├── Pimpinan dan organisasi
│   ├── Akreditasi/izin/identitas resmi
│   └── Lokasi, fasilitas, kontak
├── Akademik / Pembelajaran
│   ├── Daftar program
│   ├── Detail program
│   ├── Kalender
│   └── Direktori staf
├── Penerimaan
│   ├── Jalur dan jadwal
│   ├── Persyaratan dan biaya
│   ├── Beasiswa/bantuan
│   ├── FAQ dan bantuan
│   └── Daftar / cek status
├── Kehidupan Institusi
├── Berita
├── Agenda
├── Layanan Digital
├── Dokumen & Informasi Publik / PPID
├── Pencarian
└── Privasi, aksesibilitas, kebijakan, kontak
```

### Ekstensi perguruan tinggi

- Fakultas dan program studi
- Riset, pusat studi, publikasi, laboratorium, pengabdian
- Kemahasiswaan, internasional, alumni, karier, tracer study
- Repository, jurnal, perpustakaan, kerja sama

### Ekstensi sekolah/pesantren

- Jenjang/kurikulum/pembelajaran
- Kesiswaan, ekstrakurikuler, prestasi
- Pusat orang tua/wali
- Safeguarding, kesehatan, anti-perundungan, keadaan darurat
- Komite sekolah dan transparansi yang relevan

## 6. Model konten minimum

| Entitas | Field penting |
|---|---|
| Page | title, slug, summary, body, owner, status, locale, review date, SEO, related content |
| Program | tenant, unit, jenjang, gelar, durasi, mode, akreditasi, profil lulusan, kurikulum, biaya, jalur, CTA |
| Organization Unit | nama, tipe, pimpinan, deskripsi, kontak, lokasi, parent unit |
| Person | nama, peran, unit, keahlian, bio, kontak publik, identifier eksternal opsional |
| Admission Route | periode, program, syarat, kuota, biaya, beasiswa, deadline, status, CTA |
| Article | judul, slug, ringkasan, isi, kategori, penulis, unit, publish/updated, image+alt+credit |
| Event | judul, start/end, timezone, lokasi/online URL, organizer, status, registration URL |
| Document | judul, kategori, nomor, tahun, versi, file, format, accessibility status, owner |
| Policy | judul, scope, effective date, review date, owner, version, supersedes |
| Location | alamat terstruktur, koordinat, jam layanan, akses transportasi/disabilitas, kontak |

Semua entitas harus tenant-scoped, memiliki audit trail, slug unik per tenant+locale, dan status publish yang eksplisit.

## 7. Roadmap implementasi

### P0 — fondasi kritis (0–2 minggu)

1. Definisikan sitemap/IA dan route detail yang akan dipakai.
2. Perbaiki tenant scoping agenda perguruan tinggi dan tambahkan regression test lintas tenant.
3. Tambahkan `public_base_url` tervalidasi, self-canonical, robots, sitemap dinamis.
4. Pasang security headers melalui filter; CSP report-only lebih dulu.
5. Buat halaman minimum: profil/legal identity, penerimaan lengkap, privasi, aksesibilitas, kontak, dan PPID bila berlaku.
6. Koreksi preset terminologi sekolah dan sembunyikan feature flags yang belum memiliki UI.
7. Alert setiap fallback V4 dan kegagalan section publik.
8. Audit WCAG 2.2 AA pada beranda serta alur PMB/PPDB/login utama.

### P1 — website informasional lengkap (2–6 minggu)

1. Listing/detail program, berita, agenda, unit, staf, dokumen.
2. CMS tenant-scoped dengan workflow author-review-publish dan media governance.
3. Search, breadcrumb, mega menu, 404, redirect manager.
4. Hub PMB/PPDB dan pengukuran funnel consent-aware.
5. Schema per entitas, metadata sosial, RSS, Search Console process.
6. Cache view model publik, image pipeline, dimensi responsif, performance budget.
7. Automated tests: tenant isolation, links, schema, Lighthouse, axe, visual regression.

### P2 — diferensiasi pendidikan (6–12 minggu)

**Perguruan tinggi:** fakultas/kurikulum, riset/pengabdian, kemahasiswaan, internasional, alumni/karier/outcome.  
**Sekolah:** parent center, safeguarding, ekstrakurikuler, fasilitas, komite/transparansi.  
**Bersama:** multibahasa berbasis URL, personalization ringan berbasis pilihan pengguna, analytics/RUM, dashboard kualitas konten.

### P3 — operasi berkelanjutan

- governance board konten dan design system;
- quarterly accessibility and security review;
- content freshness SLA dan archive policy;
- evaluasi search log dan top tasks;
- riset pengguna per persona dan eksperimen funnel yang etis;
- disaster recovery, backup/restore CMS, serta export arsip publik.

## 8. Backlog prioritas teratas

| No. | Item | Prioritas | Dampak | Estimasi relatif |
|---:|---|---|---|---|
| 1 | Route dan template detail program/berita/agenda | P0 | Sangat tinggi | L |
| 2 | Perbaikan tenant scope agenda kampus + test | P0 | Sangat tinggi | S–M |
| 3 | Public base URL, canonical, robots, sitemap | P0 | Tinggi | M |
| 4 | Security header filter + CSP report-only | P0 | Sangat tinggi | M |
| 5 | Privacy, accessibility, legal identity, PPID scope | P0 | Sangat tinggi | M |
| 6 | Hub PMB/PPDB lengkap | P0/P1 | Sangat tinggi | L |
| 7 | CMS workflow dan content ownership | P1 | Sangat tinggi | XL |
| 8 | Program/fakultas/jenjang model publik | P1 | Sangat tinggi | L–XL |
| 9 | Search, breadcrumb, navigation redesign | P1 | Tinggi | L |
| 10 | Staf/dosen/guru directory | P1 | Tinggi | L |
| 11 | Accessibility QA dan acceptance criteria | P0/P1 | Tinggi | M |
| 12 | Cache, responsive image, Web Vitals RUM | P1/P2 | Tinggi | L |
| 13 | Multibahasa URL + workflow terjemahan | P2 | Sedang–tinggi | XL |
| 14 | Research/campus life atau parent/safeguarding hubs | P2 | Tinggi | XL |
| 15 | Analytics, content quality, 404/broken-link dashboard | P1/P2 | Tinggi | L |

S = beberapa hari, M = sekitar satu sprint, L = beberapa sprint, XL = program lintas modul. Estimasi harus dikalibrasi setelah pemetaan model database, UI admin, dan deployment.

## 9. Definition of Done dan KPI

### Definition of Done untuk setiap halaman publik

- tenant isolation teruji;
- URL stabil, canonical benar, masuk sitemap bila published;
- title/description/social metadata dan schema sesuai tipe;
- responsive, keyboard accessible, zoom/reflow lolos;
- alt/caption/credit media lengkap;
- owner, approver, publish date, updated date, review date tercatat;
- link checker dan HTML/schema validation lolos;
- tidak memuat data pribadi yang tidak perlu;
- performance budget dan error monitoring aktif;
- redirect tersedia bila mengganti URL lama.

### KPI 90 hari setelah rilis

- ≥95% top tasks berhasil dalam uji pengguna;
- zero cross-tenant content leak;
- ≥90% halaman published memiliki owner dan review date valid;
- broken internal links <0,5%;
- LCP ≤2,5 s, INP ≤200 ms, CLS ≤0,1 pada p75 mobile dan desktop;
- WCAG 2.2 AA: tidak ada blocker/critical pada otomatisasi dan semua skenario manual prioritas lulus;
- penurunan no-result search dan 404 dari bulan ke bulan;
- peningkatan klik program → mulai PMB/PPDB tanpa peningkatan pengumpulan data pribadi;
- seluruh fallback V4 terdeteksi dan ditindaklanjuti melalui alert.

## 10. Risiko implementasi

1. **Mencampur CMS dengan data operasional.** Jangan gunakan tabel kelas operasional sebagai katalog publik tanpa lapisan domain yang tepat.
2. **Kebocoran lintas tenant.** Semua query, cache key, sitemap, search index, slug, media, dan preview wajib membawa tenant ID.
3. **Membangun semua sebagai satu halaman.** Ini memperburuk pencarian, SEO, aksesibilitas, dan pemeliharaan.
4. **Mengaktifkan multibahasa hanya pada menu.** Bahasa harus mencakup isi utama dan URL ekuivalen.
5. **Analytics sebelum privacy design.** Tentukan tujuan, minimasi data, consent, retensi, dan akses sebelum pemasangan tracker.
6. **CSP dipasang sekaligus tanpa inventaris aset.** Gunakan report-only dan migrasi inline code secara bertahap.
7. **Fallback menutupi bug.** Pertahankan fallback hanya dengan telemetry dan alert yang jelas.
8. **Konten tanpa pemilik.** Fitur teknis akan cepat usang jika tidak ada workflow dan SLA review.

## 11. Acuan resmi

- W3C, WCAG 2.2 Recommendation: https://www.w3.org/TR/WCAG22/
- Google Search Central, sitemap: https://developers.google.com/search/docs/crawling-indexing/sitemaps/build-sitemap
- Google Search Central, canonical: https://developers.google.com/search/docs/crawling-indexing/consolidate-duplicate-urls
- Google Search Central, multilingual/multiregional: https://developers.google.com/search/docs/advanced/crawling/managing-multi-regional-sites
- Google Search Central, Event structured data: https://developers.google.com/search/docs/appearance/structured-data/event
- Google Search Central, Course list structured data: https://developers.google.com/search/docs/appearance/structured-data/course
- web.dev, Web Vitals: https://web.dev/articles/vitals
- OWASP, HTTP Headers Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html
- UU No. 14 Tahun 2008 tentang Keterbukaan Informasi Publik: https://peraturan.bpk.go.id/Details/39047/uu-no-14-tahun-2008
- PerKI No. 1 Tahun 2021 tentang Standar Layanan Informasi Publik: https://eppid.komisiinformasi.go.id/uploads/lampiran/22797PerKINo1Tahun2021.pdf
- UU No. 27 Tahun 2022 tentang Pelindungan Data Pribadi: https://peraturan.bpk.go.id/Details/229798/uu-no-27-tahun-2022

## 12. Batasan audit

- Tidak ada pengujian terhadap URL produksi, data produksi, browser matrix, perangkat nyata, analytics, Search Console, atau infrastruktur reverse proxy/CDN.
- Skor accessibility, security, dan Core Web Vitals belum merupakan hasil sertifikasi/pentest/field data.
- Penentuan apakah tenant tertentu termasuk Badan Publik dan kewajiban hukum detail harus dikonfirmasi oleh pejabat/konsultan hukum institusi.
- Audit ini tidak melakukan build atau mengubah implementasi website; dokumen ini adalah baseline backlog dan arsitektur pengembangan berikutnya.
