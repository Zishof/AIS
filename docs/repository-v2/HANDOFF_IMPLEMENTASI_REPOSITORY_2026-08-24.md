# Handoff Implementasi Repository AIS

Tanggal pembaruan: 24 Agustus 2026  
Status dokumen: baseline implementasi untuk review dan pengembangan berikutnya

## 1. Tujuan dokumen

Dokumen ini merangkum pekerjaan modernisasi Repository AIS yang telah diterapkan pada sesi
pengembangan ini. Dokumen harus digunakan oleh AI atau pengembang berikutnya agar tidak
membangun ulang fitur yang sudah ada dan dapat memusatkan review pada kekurangan yang benar-benar
masih tersisa.

Pernyataan `sudah diterapkan` di dokumen ini berarti source code telah tersedia. Sesuai instruksi
pemilik sistem, WAR tidak dibangun dan aplikasi tidak diuji pada lingkungan lokal. Keberhasilan
runtime, tampilan browser, akses database, integrasi eksternal, dan deployment tetap harus
divalidasi pada server.

## 2. Arsitektur yang dipertahankan

Repository tetap mempunyai dua antarmuka:

1. Konsol internal ZKoss:
   `src/main/src/ais/action/master/repository` beserta halaman `repository.zul`.
2. Portal publik dan workspace modern JSP:
   `src/main/webapp/WEB-INF/baru/modul/repository`.

Keduanya menggunakan service Java dan model Hibernate yang sama. Implementasi tidak mengganti
arsitektur AIS, autentikasi eCampus, Hibernate, tema institusi, atau mekanisme privilege yang
sudah digunakan aplikasi.

Komponen utama:

- `RepositoryPublicService`: discovery, statistik, detail publik, citation, OAI metadata, dan
  data halaman publik.
- `RepositoryWorkflowService`: draft, validasi, deposit, review, publish, withdraw, versi, dan
  metadata deposit.
- `RepositorySyncService`: sinkronisasi sumber akademik/perpustakaan ke `RepoItem`.
- `RepositoryFileService`: penyimpanan dan kontrol berkas, signature, MIME, checksum, serta scan.
- `RepositoryAdminService`: health dashboard, authority, perbaikan metadata, fixity, dan operasi
  administrasi.
- Servlet `Repository`: route portal publik, pencarian, detail, download, citation, feed, dan
  halaman informasi.
- Servlet `RepositoryWorkspace`: workspace depositor, reviewer, dan administrator.
- Servlet OAI-PMH pada URL aplikasi `/oai`.

## 3. Portal publik yang sudah diterapkan

### 3.1 Beranda

- Hero pencarian dan ringkasan record publik.
- Pencarian utama disederhanakan menjadi Subjek, Tahun, dan Program Studi.
- Tautan menuju pencarian lanjutan.
- Topik populer dapat diklik.
- Statistik metadata publik, naskah lengkap, koleksi, dan penulis.
- Statistik enam kelompok karya:
  - Skripsi;
  - Tesis;
  - Disertasi;
  - Karya dosen;
  - Laporan penelitian;
  - Prosiding.
- Kartu jelajah Koleksi, Penulis, Tahun terbit, Subjek, Jenis dokumen, dan Naskah lengkap dibuat
  sebagai tautan utuh sehingga seluruh kartu dapat diklik.
- Publikasi terbaru menampilkan enam record per halaman.
- Pagination server-side Publikasi terbaru tersedia di atas dan bawah daftar.
- Pagination mempunyai halaman aktif, nomor di sekitar halaman aktif, sebelumnya/berikutnya,
  dan anchor `#publikasi-terbaru` agar posisi layar kembali ke daftar setelah pindah halaman.
- Koleksi populer dan topik populer.
- Karya unggulan, karya paling banyak diunduh, dan rekomendasi pengguna bila datanya tersedia.
- Panel kepercayaan untuk identifier permanen, metadata terstruktur, kebijakan akses, dan OAI-PMH.

### 3.2 Pencarian dan discovery

- Pencarian keyword pada metadata serta extracted full text sesuai scope.
- Pencarian spesifik untuk judul, penulis, subjek, tahun, abstrak, program studi, pembimbing,
  full text, dan identifier.
- Pencarian tahun menerapkan rentang tanggal satu tahun dan gagal tertutup untuk input tidak valid.
- Pencarian lanjut AND, OR, frasa tepat, dan NOT.
- Filter penulis, subjek, program studi, identifier, rentang tahun, bahasa, koleksi, jenis dokumen,
  akses, dan ketersediaan naskah.
- Facet, sorting, paging hasil, saran koreksi, synonym expansion, dan empty state.
- Seluruh result card, penulis, subjek, koleksi, tombol detail, dan ekspor sitasi dapat diklik.

### 3.3 Halaman Koleksi

- Route `/repository/collections` ditambahkan.
- Menampilkan koleksi terbaru berdasarkan waktu perubahan.
- Menampilkan jumlah item pada setiap koleksi.
- Menyediakan jelajah berdasarkan Subjek, Tahun, Penulis, dan Program Studi.
- Menampilkan karya terbaru lintas koleksi.
- Halaman detail koleksi tetap menyediakan pencarian dalam koleksi dan daftar publikasi.

### 3.4 Detail karya

- Judul, penulis yang dapat diklik, koleksi, tahun, jenis dokumen, akses, lisensi, embargo, versi,
  view, dan download.
- Abstrak dan kata kunci.
- Informasi bibliografis termasuk OAI identifier, DOI, Handle, bahasa, koleksi, lisensi, dan
  metadata tambahan.
- Versi record dan publikasi terkait.
- Ekspor APA, IEEE, Harvard, Vancouver, RIS, BibTeX, EndNote, dan CSL-JSON.
- Salin tautan dan bookmark untuk pengguna login.
- Panel naskah lengkap tidak lagi menampilkan area kosong berlebihan.
- Pratinjau dan download berkas hanya muncul bila hak akses mengizinkan.
- Untuk pengguna anonim, panel menampilkan CTA Login eCampus bila naskah tersedia tetapi login
  diwajibkan.
- Metadata dan abstrak tetap terlihat oleh pengguna umum.

### 3.5 Profil penulis dan authority

- Profil penulis menampilkan karya, afiliasi, variasi nama, tren tahun, dan topik.
- Dukungan ORCID dan ROR.
- Authority penulis dan contributor dipakai untuk relasi karya-penulis.
- Merge authority menghindari pembuatan relasi contributor yang sama.

### 3.6 Bantuan, Tanya Repository, dan kebijakan

- Menu Bantuan dan Tanya Repository tersedia.
- FAQ membahas unggah, download, berkas tidak muncul, dan koreksi metadata.
- Panduan unggah sembilan langkah tersedia.
- Kontak pengelola mengambil email, telepon, dan alamat dari institusi aktif.
- Kebijakan disesuaikan dengan institusi aktif, bukan menyalin nama perguruan tinggi contoh.
- Kelompok kebijakan yang tersedia:
  - Metadata;
  - Data dan naskah lengkap;
  - Konten;
  - Pengajuan;
  - Preservasi;
  - Hak cipta dan lisensi;
  - Embargo;
  - Withdrawal/takedown;
  - Privasi;
  - Aksesibilitas.

## 4. Workspace deposit dan editorial yang sudah diterapkan

- Workspace terpisah untuk depositor, reviewer, dan administrator.
- Permission diperiksa server-side; UI bukan sumber otorisasi.
- CSRF digunakan untuk operasi perubahan data.
- Deposit wizard sembilan tahap.
- Draft, autosave, optimistic version, submit, resubmit, claim, return, reject, approve, publish,
  withdraw, restore, dan komentar.
- Antrian review, assignment reviewer, riwayat audit, notifikasi, dan catatan keputusan.
- Deteksi kemungkinan duplikat sebelum publikasi.
- Jenis dokumen deposit mencakup Skripsi, Tesis, Disertasi, Thesis/Dissertation legacy, Artikel,
  Buku, Bab Buku, Karya Dosen, Research Report, Proceedings, Dataset, Teaching Material, dan Other.
- Field judul, penulis, ORCID, abstrak, kata kunci, penerbit, bahasa, afiliasi, ROR, program studi,
  fakultas, pembimbing, penguji, pendanaan, pernyataan hak, dan catatan reviewer.
- Field Daftar Pustaka disimpan sebagai `dc.relation.references`.
- Daftar pustaka diwajibkan saat submit untuk Skripsi, Tesis, Disertasi, Thesis, atau Dissertation.
- Upload PDF mewajibkan konfirmasi bahwa naskah final telah diberi watermark institusi.
- Validasi watermark dilakukan server-side pada request upload PDF, bukan hanya checkbox HTML.
- Berkas utama diwajibkan untuk kebijakan akses yang mendistribusikan file.
- Validasi lisensi, embargo, signature, status malware, dan file utama.
- Checksum SHA-256, MIME/signature, fixity, versi file, dan provenance.
- Saran metadata AI bersifat draft dan tetap harus ditinjau manusia.

## 5. Administrasi dan integrasi yang sudah tersedia

- Dashboard jumlah item, item publik, antrian review, kesehatan OAI-PMH, bitstream, scan, checksum,
  sync gagal, dan workflow.
- Retry sync gagal dan bulk metadata repair.
- Collection profile, deposit flag, metadata profile, workflow profile, access policy, dan lisensi
  default.
- Author authority, ORCID OAuth, ROR matching, dan merge authority.
- DataCite DOI, COAR Notify, audit integration event, dan pemeriksaan konfigurasi.
- Export/import administratif dan dry-run XLSX.
- Feed RSS dan Atom.
- SEO metadata, citation meta tags, canonical URL, schema.org JSON-LD, sitemap, dan robots.
- PWA/service worker serta responsive layout yang sudah menjadi bagian portal modern.

Integrasi eksternal bersifat opt-in. Detail property terdapat di `docs/repository-v2/INTEGRATIONS.md`.

## 6. OAI-PMH

Endpoint utama setelah aplikasi dipasang pada context `/ais`:

`https://HOST/ais/oai`

Contoh verb:

- `/ais/oai?verb=Identify`
- `/ais/oai?verb=ListMetadataFormats`
- `/ais/oai?verb=ListSets`
- `/ais/oai?verb=ListIdentifiers&metadataPrefix=oai_dc`
- `/ais/oai?verb=ListRecords&metadataPrefix=oai_dc`
- `/ais/oai?verb=GetRecord&metadataPrefix=oai_dc&identifier=IDENTIFIER`

Implementasi mencakup validasi verb/argument, `oai_dc`, set, date range, resumption token, paging,
tombstone withdrawn record, tenant scope, XML escaping, error OAI, dan cache/response headers.

Keunikan `oai_identifier` tetap dipertahankan secara global. Sinkronisasi diperbaiki agar:

- mencari record yang sudah memiliki identifier sebelum membuat record baru;
- tidak menulis identifier yang telah dimiliki record lain;
- menghasilkan identifier alternatif stabil bila identifier dasar bentrok;
- menangani sumber historis yang memetakan lebih dari satu sumber ke identifier yang sama;
- kegagalan satu record tidak merusak seluruh batch sinkronisasi.

## 7. Perbaikan duplicate key sinkronisasi

### 7.1 Duplicate `repo_item.oai_identifier`

Kasus duplicate seperti `oai:ais:library-item:*` dan `oai:ais:penelitian-pengabdian:*` telah
ditangani pada `RepositorySyncService` melalui lookup global, pemakaian ulang record yang benar,
pemeriksaan pemilik identifier, dan fallback identifier bebas bentrok.

### 7.2 Duplicate contributor

Constraint `(item_id, authority_id, contributor_role)` ditangani secara idempoten:

- relasi contributor dicari sebelum insert;
- relasi yang sama tidak dibuat ulang;
- merge authority menonaktifkan link duplikat bila target sudah mempunyai relasi yang sama;
- sinkronisasi dapat dilanjutkan per record tanpa membatalkan seluruh batch.

Perbaikan ini ditujukan untuk menghilangkan kegagalan berulang yang sebelumnya menyisakan sebagian
record gagal sinkron.

## 8. Multi-tenant, tema, dan schema

- Tenant key berasal dari PerguruanTinggi atau Sekolah aktif:
  `PT:<id>`, `SEKOLAH:<id>`, atau fallback `AIS:DEFAULT`.
- Koleksi, item, search, facet, detail, statistik, workspace, preference, dan OAI menggunakan tenant
  scope.
- Warna, logo, nama, email, telepon, dan alamat mengikuti PerguruanTinggi/Sekolah aktif.
- CSS menggunakan variable tema Repository sehingga tidak mengunci warna satu institusi.
- Tidak ada `ALTER TABLE` runtime di `RepositoryTenantScope`.
- Hibernate bertanggung jawab atas pembuatan/perubahan kolom model Repository.
- Routine tenant hanya melakukan backfill nilai tenant setelah schema tersedia.
- Jangan menambahkan SQL DDL manual atau skrip `ALTER TABLE` sebagai solusi lanjutan.

## 9. Kebijakan akses naskah lengkap

Nilai bawaan:

```text
-Dais.repository.anonymousFullText=false
```

Dengan nilai tersebut:

- pengguna umum dapat membuka metadata dan abstrak;
- pengguna harus login eCampus untuk membaca/mengunduh naskah lengkap;
- setelah login, akses tetap mengikuti status item, status bitstream, lisensi, embargo, dan privilege;
- property dapat diubah menjadi `true` hanya bila kebijakan institusi mengizinkan download anonim
  untuk berkas Open Access.

Jangan menaruh password database atau secret integrasi di dokumen/source. Gunakan konfigurasi
deployment server.

## 10. File penting

### Java/service

- `src/main/src/ais/action/master/repository/RepositoryPublicService.java`
- `src/main/src/ais/action/master/repository/RepositoryWorkflowService.java`
- `src/main/src/ais/action/master/repository/RepositorySyncService.java`
- `src/main/src/ais/action/master/repository/RepositoryFileService.java`
- `src/main/src/ais/action/master/repository/RepositoryAdminService.java`
- `src/main/src/ais/action/master/repository/RepositoryIntegrationService.java`
- `src/main/src/ais/action/master/repository/RepositoryTenantScope.java`
- `src/main/src/ais/action/servlet/Repository.java`
- `src/main/src/ais/action/servlet/RepositoryWorkspace.java`

### JSP/CSS/JavaScript

- `src/main/webapp/WEB-INF/baru/modul/repository/ListRepository.jsp`
- `src/main/webapp/WEB-INF/baru/modul/repository/WorkspaceRepository.jsp`
- `src/main/webapp/WEB-INF/baru/modul/repository/landing_page.jsp`
- `src/main/webapp/css/repository-modern.css`
- `src/main/webapp/js/repository-modern.js`

### Dokumentasi deployment

- `src/main/docs/repository-v2/INTEGRATIONS.md`
- `src/main/docs/repository-v2/ROLLOUT_ROLLBACK.md`
- dokumen handoff ini.

## 11. Pemeriksaan yang sudah dilakukan pada source

- Pemeriksaan `git diff --check` pada file yang diubah.
- Jumlah pembuka/penutup scriptlet JSP seimbang.
- Jumlah tag form dan nav pada JSP yang disentuh seimbang.
- Jumlah kurung kurawal Java pada service/servlet yang disentuh seimbang.
- Referensi parameter Daftar Pustaka dan watermark ditelusuri dari JSP ke servlet dan service.
- Tidak ditemukan `ALTER TABLE` pada `RepositoryTenantScope` setelah perbaikan.

Pemeriksaan tersebut adalah pemeriksaan statis, bukan bukti bahwa aplikasi telah berjalan di
server.

## 12. Yang belum boleh dianggap selesai sebelum validasi server

AI/pengembang berikutnya harus membedakan kekurangan implementasi dengan validasi runtime. Hal
berikut belum diklaim lulus pada sesi ini:

- build penuh aplikasi dan WAR terbaru;
- startup Tomcat dengan source terbaru;
- update schema Hibernate pada database target;
- uji login civitas, depositor, reviewer, administrator, dan pengguna anonim;
- uji upload PDF berwatermark serta penolakan upload tanpa konfirmasi;
- uji submit Skripsi/Tesis/Disertasi dengan dan tanpa daftar pustaka;
- uji semua verb OAI-PMH menggunakan validator eksternal;
- uji ulang sinkronisasi sampai jumlah gagal nol atau setiap kegagalan mempunyai sebab data yang
  sah;
- uji pagination Publikasi terbaru pada halaman awal, tengah, terakhir, dan parameter tidak valid;
- uji tampilan desktop 1920×1080, laptop 1366×768, tablet, serta mobile 360–390 px;
- uji warna/logo/data kontak pada tenant PerguruanTinggi dan Sekolah berbeda;
- accessibility audit WCAG 2.2 dan visual regression;
- sandbox DataCite, ORCID, ROR, COAR Notify, antivirus, dan integrasi eksternal lainnya.

## 13. Batasan untuk pengembangan berikutnya

- Jangan membangun ulang Repository dari nol.
- Jangan mengembalikan SQL browser-side atau endpoint SQL publik.
- Jangan memindahkan privilege ke pemeriksaan UI saja.
- Jangan menghapus CSRF, tenant scope, audit, versioning, atau constraint unik.
- Jangan mengubah identifier OAI record lama tanpa strategi kompatibilitas.
- Jangan membuat `ALTER TABLE` runtime; serahkan schema kepada Hibernate.
- Jangan menyalin nama, alamat, warna, atau kebijakan universitas contoh secara hard-coded.
- Jangan menganggap jumlah statistik sama dengan data valid tanpa memeriksa pemetaan jenis dokumen
  dan koleksi pada server.
- Pertahankan feature flag V1/V2 dan jalur rollback.

## 14. Fokus saran desain berikutnya

Review berikutnya sebaiknya menggunakan screenshot/video runtime terbaru dan memeriksa:

- hierarki visual, density, spacing, alignment, dan area kosong;
- konsistensi ZKoss dengan JSP tanpa memaksakan kedua teknologi menjadi identik;
- hero, statistik, Publikasi terbaru dan pagination atas/bawah;
- search result card, facet, filter mobile, empty/error/loading state;
- detail karya, CTA login/download, restricted, embargo, withdrawn, dan metadata-only;
- halaman Koleksi, profil penulis, FAQ, Bantuan, dan Kebijakan;
- deposit wizard, validasi daftar pustaka/watermark, duplicate warning, dan preview;
- review queue, dashboard administrasi, authority merge, ORCID/ROR, DataCite, dan COAR Notify;
- keyboard navigation, focus, contrast, target size, screen reader label, dan responsive layout.

Setiap saran harus menyebut lokasi, masalah yang terlihat, dampak, solusi konkret, breakpoint atau
ukuran yang disarankan, prioritas P0–P3, dan acceptance criteria. Pisahkan saran desain murni,
frontend, backend/data, integrasi eksternal, dan validasi runtime.

## 15. Prompt siap diberikan kepada AI lain

Salin prompt berikut bersama dokumen ini dan screenshot/video terbaru:

> Pelajari dokumen handoff Implementasi Repository AIS ini sebagai baseline resmi. Jangan
> menyarankan pembangunan ulang dan jangan mengulang fitur yang telah tercatat selesai. Repository
> mempunyai konsol internal ZKoss dan portal/workspace JSP yang menggunakan service Java serta
> Hibernate yang sama. Review screenshot/video runtime terbaru secara evidence-based.
>
> Temukan hanya masalah yang masih terlihat atau perilaku yang memang perlu divalidasi. Untuk setiap
> temuan, tuliskan halaman/komponen, bukti visual atau runtime, dampak pengguna, solusi konkret,
> ukuran/spacing/breakpoint bila relevan, prioritas P0–P3, acceptance criteria, dan klasifikasi:
> desain tanpa backend, frontend, backend/data, integrasi eksternal, atau validasi server.
>
> Pertahankan autentikasi eCampus, privilege server-side, CSRF, tenant scope PerguruanTinggi/Sekolah,
> tema institusi, Hibernate sebagai pengelola schema, OAI-PMH, workflow editorial, authority penulis,
> ORCID/ROR, DataCite, analytics, PWA, feature flag V1/V2, dan seluruh route existing. Pengguna umum
> hanya melihat metadata/abstrak secara default; naskah lengkap membutuhkan login eCampus.
>
> Jangan mengusulkan SQL browser-side, DDL/ALTER TABLE runtime, hard-coded identitas universitas,
> penghapusan constraint unik, atau perubahan identifier OAI lama tanpa rencana kompatibilitas.
> Bedakan secara tegas source yang sudah diimplementasikan dari fitur yang baru dapat dinyatakan
> lulus setelah build, deployment, dan pengujian server.
>
> Fokuskan review pada hierarki visual, spacing, responsivitas, accessibility WCAG 2.2, state
> loading/empty/error/restricted/embargo/withdrawn, pagination Publikasi terbaru, search/facet,
> detail karya, profil penulis, deposit wizard, review queue, dashboard, dan UX integrasi.
> Sertakan daftar komponen yang harus dipertahankan agar tidak dibangun ulang.

## 16. Ketahanan beranda terhadap kegagalan sementara

Pembaruan 25 Agustus 2026 menanggapi HTTP 500 intermiten pada `/repository` yang biasanya hilang
setelah browser di-refresh:

- paging Publikasi terbaru tidak lagi membuka session/koneksi Hibernate kedua di tengah request;
- seluruh query beranda memakai satu native ThreadLocal session yang ditutup terpusat pada akhir
  request;
- lazy tenant backfill tidak lagi dijalankan dari setiap pembacaan portal publik;
- GET beranda mendapat satu retry otomatis dengan session baru bila query inti gagal sebelum
  response dikirim;
- pada retry kedua, kegagalan summary/koleksi/publikasi terbaru memakai empty fallback agar halaman
  tetap dapat dirender;
- widget nonkritis seperti topik populer, karya unggulan, paling banyak diunduh, rekomendasi, dan
  search alert diisolasi sehingga kegagalan satu widget tidak menjatuhkan seluruh halaman;
- pemeriksaan ketersediaan deposit juga gagal tertutup dan tidak membuat portal publik HTTP 500;
- response retry diberi header `X-Repository-Retry: 1`, sedangkan kegagalan komponen tetap dicatat
  dengan nama komponen dan request ID untuk diagnosis server.

Perubahan ini mencegah kegagalan sementara sebuah query berubah menjadi halaman error global.
Kegagalan permanen tetap harus dicari di log server; fallback bukan pengganti perbaikan database,
pool koneksi, atau query yang memang salah.

## 17. Penyelesaian lanjutan 25 Agustus 2026

Audit lanjutan terhadap source aktual menemukan dan memperbaiki beberapa perbedaan antara baseline
dan implementasi:

- `RepositoryTenantScope.ensureSchema()` tidak lagi melakukan `UPDATE` massal terhadap semua item
  atau koleksi yang `tenant_key`-nya kosong. Hook kompatibilitas tersebut sekarang tidak memutasi
  database; penetapan tenant legacy dilakukan per record oleh sinkronisasi saat pemilik sumber
  diketahui. Ini mencegah institusi pertama yang membuka portal mengklaim data tenant lain.
- `ListIdentifiers` dan `ListRecords` OAI-PMH sekarang mencakup record publik serta tombstone
  withdrawn. Datestamp tombstone memakai waktu penarikan bila lebih baru.
- Filter `from`/`until` OAI memperhitungkan penarikan, sinkronisasi, waktu perubahan, publikasi,
  penerbitan, dan submit.
- Argumen OAI divalidasi per verb, argumen berulang/asing ditolak, `resumptionToken` bersifat
  eksklusif dan terikat pada verb, token kedaluwarsa/tidak valid ditolak, serta urutan harvesting
  menggunakan ID stabil.
- Identitas OAI dapat dikonfigurasi melalui `ais.repository.oaiBaseUrl`,
  `ais.repository.oaiRepositoryName`, dan `ais.repository.oaiAdminEmail`.
- Hak reviewer dan administrator dipisahkan kembali. `dasborRepository` hanya memberi akses review;
  pengelolaan koleksi/authority, ekspor, fixity, bulk repair, DataCite, COAR Notify, ORCID/ROR,
  dan fungsi administrasi memerlukan role administrator.
- Reviewer dapat melihat item/antrian yang diizinkan, tetapi tidak dapat menyunting metadata atau
  berkas milik depositor. Penyuntingan hanya untuk pemilik deposit atau administrator.
- Otorisasi upload dilakukan sebelum penyimpanan Repository, pemindaian antivirus, dan ekstraksi
  konten. Penghapusan berkas memulihkan file dari trash bila transaksi database gagal.
- Ditambahkan `validate-repository-server.sh` untuk smoke validation publik read-only setelah
  deployment.

Perubahan ini hanya diperiksa secara statis. Build, WAR, Tomcat, database, dan pengujian lokal tetap
tidak dijalankan sesuai instruksi pemilik sistem.

## 18. Paging konsisten pada daftar publik 25 Agustus 2026

Menindaklanjuti evaluasi visual halaman koleksi, paging server-side kini ditampilkan konsisten di
atas dan bawah seluruh daftar publik utama yang memakai pola publikasi:

- Publikasi terbaru pada beranda;
- hasil pencarian dan jelajah;
- publikasi dalam koleksi; dan
- karya pada profil penulis.

Semua halaman memakai pembentuk kontrol paging bersama dengan tombol sebelumnya/berikutnya,
halaman awal/akhir, elipsis untuk rentang besar, penanda halaman aktif, label aksesibel, serta
anchor yang mengembalikan viewport ke daftar terkait. Parameter pencarian, filter, urutan, dan
ukuran halaman dipertahankan. Profil penulis yang sebelumnya dibatasi satu potongan hasil kini
memakai paging service sesungguhnya; statistik tahun dan topiknya tetap berasal dari facet seluruh
hasil penulis, bukan hanya item pada halaman aktif.

Perubahan ini diperiksa secara statis tanpa build WAR, Tomcat, database, atau pengujian lokal.

## 19. Katalog 300 tanya jawab Repository 25 Agustus 2026

Halaman Tanya Repository tidak lagi menampilkan empat FAQ statis. Ditambahkan katalog tepat 300
tanya jawab publik yang dibentuk dari 20 kategori, masing-masing berisi 15 topik. Jawaban mencakup
konteks, langkah pemeriksaan, tindakan aman, dan jalur eskalasi untuk akun, deposit, metadata,
berkas, review, publikasi, pencarian, koleksi, akses naskah, lisensi, author authority, identifier,
sitasi, versi, notifikasi, kendala teknis, keamanan, aksesibilitas, data penelitian, dan integrasi.

Katalog tidak bergantung pada database. Pencarian teks, filter kategori, jumlah hasil, koreksi nomor
halaman, empty state, dan paging 12 entri per halaman diproses server-side. Kontrol paging tersedia
di atas dan bawah daftar serta mempertahankan pertanyaan sumber, filter FAQ, kategori, dan ukuran
halaman. Tampilan responsif berubah menjadi satu kolom pada layar kecil.

Validasi statis memastikan terdapat 20 blok kategori dengan 15 topik per blok (total 300), seluruh
delimiter JSP/form/details seimbang, dan brace serta parenthesis Java seimbang. Build WAR, Tomcat,
database, dan pengujian lokal tidak dijalankan sesuai instruksi pemilik sistem.

## 20. Manual Pusat Bantuan 5.000+ kata 25 Agustus 2026

Halaman Bantuan yang sebelumnya hanya berisi lima penjelasan singkat dan satu daftar tahap deposit
diganti dengan manual terstruktur berjumlah 5.561 kata. Konten dipisahkan ke partial
`_repository_help.jsp` agar `ListRepository.jsp` tetap dapat dirawat. Manual mempunyai 17 bab dan
54 heading yang mengikuti perjalanan pengguna: orientasi, pencarian, pembacaan record, akses,
akun, persiapan deposit, sembilan tahap unggah, metadata, berkas/lisensi/embargo, review,
publikasi/versi, sitasi/identifier, integrasi, privasi/aksesibilitas, troubleshooting, dukungan,
dan glosarium.

Ditambahkan daftar isi responsif, checklist, tabel keputusan, template laporan, glosarium, enam
diagram HTML/CSS dengan deskripsi `aria-label`, tautan ke katalog 300 tanya jawab, serta stylesheet
cetak. Diagram tidak memakai gambar eksternal sehingga tetap dapat dibaca pembaca layar, mengikuti
tema institusi, dan dapat bergeser horizontal pada layar sempit.

Validasi statis memastikan 5.561 kata, 17 bab, enam diagram, 18 anchor internal yang semuanya
memiliki target, tidak ada ID duplikat, pasangan section/article seimbang, delimiter JSP seimbang,
serta brace CSS seimbang. Build WAR, Tomcat, database, dan pengujian lokal tidak dijalankan sesuai
instruksi pemilik sistem.
