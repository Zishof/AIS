# Implementasi Prioritas Website Institusi V4

Tanggal: 25 Agustus 2026  
Status: perubahan sumber selesai; validasi statis lulus; **WAR tidak dibangun dan deployment tidak dilakukan**.

## Cakupan yang diimplementasikan

### 1. Isolasi tenant dan agenda perguruan tinggi

- PMB, program, berita, dan detail konten diperiksa berdasarkan tenant.
- `KalenderAkademik` belum memiliki relasi perguruan tinggi. Agenda kampus tanpa scope kini ditutup secara default.
- Agenda global hanya dapat dibuka dengan konfigurasi sadar-risiko:
  - `website_v4_college_agenda_shared = AKTIF`
- Untuk keamanan data, biarkan konfigurasi tersebut `TIDAK_AKTIF` sampai model kalender mempunyai owner perguruan tinggi.

### 2. Halaman publik dan routing

Route yang tersedia:

- `/web`
- `/web/profil`
- `/web/program`
- `/web/program/{id}`
- `/web/penerimaan`
- `/web/berita`
- `/web/berita/{id}`
- `/web/agenda`
- `/web/agenda/{id}`
- `/web/layanan`
- `/web/dokumen`
- `/web/riset`
- `/web/kehidupan-kampus`
- `/web/pembelajaran`
- `/web/orang-tua`
- `/web/perlindungan-anak`
- `/web/ppid`
- `/web/privasi`
- `/web/aksesibilitas`
- `/web/kontak`
- `/web/akreditasi`
- `/web/staf`
- `/web/beasiswa`
- `/web/cari?q=kata-kunci`

Detail program, berita, dan agenda tidak dapat diakses bila ID bukan milik tenant aktif.

### 3. SEO dan discovery

- canonical per halaman;
- public base URL tervalidasi;
- Open Graph dan Twitter Card;
- JSON-LD organisasi, WebPage, NewsArticle, Event, EducationalOccupationalProgram, dan BreadcrumbList;
- `robots.txt` dinamis;
- `sitemap.xml` dinamis dan tenant-scoped;
- `website-feed.xml` RSS berita;
- `website.webmanifest` tenant-aware;
- halaman 404/detail tidak ditemukan diberi `noindex`.

Konfigurasi wajib sebelum deployment produksi:

```text
website_v4_public_base_url = https://domain-resmi-institusi.example
```

Nilai harus berupa origin tanpa trailing slash dan tanpa path.

### 4. Keamanan dan privasi

Filter publik memasang:

- Content-Security-Policy;
- Strict-Transport-Security pada request HTTPS;
- X-Content-Type-Options;
- Referrer-Policy;
- Permissions-Policy;
- X-Frame-Options;
- Cross-Origin-Opener-Policy;
- nonce untuk JSON-LD inline;
- request ID untuk korelasi error.

Inline style dinamis masih membutuhkan `style-src 'unsafe-inline'`. Script tidak mengizinkan `unsafe-inline`.

### 5. Konten kepatuhan dan layanan publik

Sudah tersedia template yang dapat diedit untuk:

- identitas legal, visi-misi, tata kelola;
- jalur, deadline, persyaratan, biaya, bantuan PMB/PPDB;
- pemberitahuan privasi;
- pernyataan aksesibilitas dan kanal pelaporan;
- PPID, Daftar Informasi Publik, permohonan, dan keberatan;
- perlindungan anak serta anti-perundungan;
- akreditasi dan penjaminan mutu.

Teks legal default bersifat baseline. Institusi wajib meminta pemilik proses/hukum memverifikasi isi final sebelum publikasi.

### 6. Paket perguruan tinggi dan sekolah

Perguruan tinggi memperoleh jalur program studi, riset, kehidupan kampus, staf, akreditasi, beasiswa, berita, dan agenda aman.  
Sekolah memperoleh terminologi pembelajaran, pusat orang tua, perlindungan anak, program pendidikan, staf, kegiatan, dan PPDB.

Label `Riset & Inovasi` tidak lagi dipaksakan sebagai default untuk tenant sekolah.

### 7. Observabilitas dan fallback

- Semua request publik memperoleh `X-Request-Id`.
- Fallback V4 ke website lama dicatat melalui `recordVisibleFailure` dengan penanda `[WEBSITE-V4-FALLBACK]`.
- Respons fallback diberi `X-Website-Fallback: legacy` agar dapat dideteksi health check/proxy.
- Bila query section gagal, beranda menampilkan pesan layanan yang ramah dan error teknis tetap masuk audit log.

Rekomendasi alert server:

- alert pada log yang mengandung `[WEBSITE-V4-FALLBACK]`;
- alert pada respons dengan header `X-Website-Fallback: legacy`;
- synthetic request ke `/web`, `/robots.txt`, dan `/sitemap.xml` per domain tenant.

### 8. Aksesibilitas

- skip link;
- fokus terlihat;
- drawer seluler dengan Escape, focus trap, dan pengembalian fokus;
- breadcrumb semantik;
- form search dengan label;
- status kegagalan section;
- reduced motion;
- target interaksi minimum 44 px pada kontrol utama;
- indikasi screen-reader untuk tautan unduhan yang membuka tab baru;
- halaman pernyataan aksesibilitas dan kanal feedback.

Target penerimaan: WCAG 2.2 Level AA. Pengujian manual NVDA/perangkat nyata tetap dilakukan setelah deployment karena tidak dapat disimulasikan secara valid hanya dari sumber.

## Konfigurasi editor baru

- `website_v4_public_base_url`
- `website_v4_college_agenda_shared`
- `website_v4_show_site_search`
- `website_v4_profile_vision_mission`
- `website_v4_profile_legal`
- `website_v4_admission_routes`
- `website_v4_admission_requirements`
- `website_v4_admission_fees`
- `website_v4_privacy_data`
- `website_v4_privacy_retention`
- `website_v4_accessibility_intro`
- `website_v4_accessibility_feedback`
- `website_v4_ppid_intro`
- `website_v4_ppid_request`
- `website_v4_safeguarding_policy`

Konten formal V4 juga memakai konfigurasi website lama sebagai fallback agar naskah yang sudah pernah diisi tidak hilang.

## Validasi tanpa build

Jalankan dari PowerShell:

```powershell
& C:\opt\AIS\ais\docs\validate-website-v4.ps1
```

Validator memeriksa:

- XML `web.xml`;
- mapping route/filter/discovery;
- guard tenant pada query kritis;
- keberadaan security header;
- robots dan sitemap;
- breadcrumb, search, skip link, dan CSP nonce;
- keseimbangan kurung Java dan scriptlet JSP.

Hasil terakhir:

```text
Validasi statis Website Institusi V4: LULUS
Pemeriksaan ini tidak melakukan compile, build WAR, atau deployment.
```

## Checklist deployment mandiri

1. Isi `website_v4_public_base_url` untuk setiap instalasi/domain produksi.
2. Pastikan reverse proxy meneruskan HTTPS dengan benar dan sertifikat valid.
3. Biarkan `website_v4_college_agenda_shared` nonaktif kecuali memang satu kalender global yang disetujui.
4. Verifikasi konten privasi, PPID, penerimaan, biaya, dan perlindungan anak oleh pemilik proses.
5. Setelah deploy, cek:

```text
GET /web
GET /web/profil
GET /web/program
GET /web/berita
GET /web/agenda
GET /web/privasi
GET /web/aksesibilitas
GET /robots.txt
GET /sitemap.xml
GET /website-feed.xml
GET /website.webmanifest
```

6. Pastikan header CSP, HSTS, nosniff, referrer policy, request ID, dan cache tersedia.
7. Uji satu ID program/berita milik tenant serta satu ID milik tenant lain; ID tenant lain harus 404.
8. Uji keyboard, zoom 200%/400%, NVDA, mobile drawer, search, dan PMB/PPDB.
9. Daftarkan sitemap pada Search Console setelah canonical domain diverifikasi.
10. Pantau Error Log dan header fallback selama periode awal deployment.

## File utama yang berubah

- `src/main/java/ais/action/servlet/Web.java`
- `src/main/java/ais/action/servlet/WebsiteDiscovery.java`
- `src/main/java/ais/common/home/HomePortalService.java`
- `src/main/java/ais/common/home/HomePortalContentService.java`
- `src/main/java/ais/common/home/HomePortalViewModel.java`
- `src/main/java/ais/common/home/HomePortalSeoService.java`
- `src/main/java/ais/common/home/WebsitePageService.java`
- `src/main/java/ais/common/home/WebsitePageViewModel.java`
- `src/main/java/ais/common/home/WebsitePublicSecurityFilter.java`
- `src/main/java/ais/action/master/KonfigurasiNewAction.java`
- `src/main/webapp/WEB-INF/baru/website/home.jsp`
- `src/main/webapp/WEB-INF/baru/website/page.jsp`
- `src/main/webapp/css/baru/website-v4.css`
- `src/main/webapp/WEB-INF/web.xml`
- `docs/validate-website-v4.ps1`

Tidak ada file WAR yang dibuat dan tidak ada deployment yang dijalankan.
