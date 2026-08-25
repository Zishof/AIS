# Handoff lanjutan modul Jurnal AIS

Tanggal snapshot: **24 Agustus 2026, 23:52 WIB (Asia/Jakarta)**.

Dokumen ini adalah titik masuk terbaru untuk AI/pengembang berikutnya. Dokumen lama tetap dipertahankan sebagai audit historis, tetapi bila ada perbedaan angka build, status izin, atau konfigurasi runtime, gunakan dokumen ini dan verifikasi ulang terhadap checkout aktual.

## 1. Lokasi dan aturan kerja

- Root aplikasi: `C:\opt\AIS\ais`.
- Source canonical: `src/main/src`.
- Source mirror legacy: `src/main/java`.
- JSP jurnal: `src/main/webapp/WEB-INF/baru/modul/jurnal`.
- Dokumen jurnal: `src/main/docs/jurnal`.
- Folder ini bukan working copy Git/SVN; perubahan tidak dapat direkonstruksi dari commit. Jangan menghapus atau menimpa perubahan paralel yang tidak berkaitan.
- Perubahan kelas jurnal harus disinkronkan pada tree canonical dan mirror bila file memang tersedia pada keduanya.
- Password database dan secret provider wajib berasal dari environment/secret manager; jangan menuliskannya ke source, XML, log, atau Markdown.

Urutan baca yang disarankan:

1. dokumen ini;
2. `10-IMPLEMENTATION-EVIDENCE.md`;
3. `07-TEST-AND-ACCEPTANCE-MATRIX.md`;
4. `08-IMPLEMENTATION-PHASES-AND-RELEASE-GATES.md`;
5. `12-REVIEW-ULANG-DAN-GAP-ANALYSIS-JURNAL-AIS.md`;
6. `13-SPESIFIKASI-UIUX-JURNAL-MODERN.md`;
7. `14-PERINTAH-MASTER-CODEX-CLAUDE-JURNAL-AIS-OJS-3505-V2.md`.

## 2. Ringkasan besar pekerjaan yang sudah dilakukan

### 2.1 Analisis, arsitektur, dan fase implementasi

- Audit baseline AIS dan pemetaan existing-first sudah ditulis.
- Inventaris OJS 3.5.0-5, mapping 134 tabel/905 field, matriks plugin, arsitektur, migrasi, RBAC, acceptance matrix, traceability, gap register, dan release gate Fase 0–19 tersedia dalam dokumen `00`–`14`.
- Domain jurnal menggunakan Hibernate utama AIS dan schema `penelitiandanpengabdian`; BLOB jurnal menggunakan database streaming melalui `LampiranJurnal`.
- Migration, verifier, seed menu, rollback, importer OJS, workflow submission-to-publication, file lifecycle, identifier/export, OAI, galley, subscription/payment, email, usage/report, dan public discovery sudah mempunyai implementasi serta bukti lokal dengan tingkat kelulusan berbeda. Detail authoritative ada di ledger evidence.

### 2.2 Portal publik dan login jurnal

Landing jurnal sekarang mempunyai login AIS seperti modul pustaka:

- form login server-rendered pada halaman `/jurnal`;
- POST `/jurnal/login` dengan CSRF;
- pesan gagal generik dan password tidak direfleksikan;
- lockout scoped login;
- setelah login tampil nama akun, tombol ruang kerja, logout, dan kontrol admin;
- layout desktop serta viewport mobile 390 px pernah diuji tanpa overflow.

File utama:

- `src/main/src/ais/common/newui/PortalLoginApi.java`;
- `src/main/src/ais/common/newui/LibraryLoginApi.java`;
- `src/main/src/ais/action/servlet/Jurnal.java`;
- `src/main/webapp/WEB-INF/baru/modul/jurnal/landing_page.jsp`;
- `src/main/src/ais/action/master/jurnal/test/JurnalPortalLoginSelfTest.java`.

`LibraryLoginApi` didelegasikan ke adapter portal bersama. Hasil `SecurityFilter.doAutoLogin` diperiksa; kegagalan tidak dianggap sukses.

### 2.3 Kontrol administrator pada landing jurnal

Saat `Common.getApakahAdmin()` bernilai benar, landing menampilkan:

- `Ruang kerja jurnal`;
- `Import dari OJS` menuju `/jurnal/admin/import-ojs`;
- `Masukkan data sample minimal 50 jurnal dan tiap jurnal masing-masing minimal 100 artikel`;
- `Hapus Jurnal Sample`.

Render tombol bukan satu-satunya kontrol keamanan. Endpoint tetap memeriksa login, administrator aktif, CSRF, capability jurnal, konfirmasi eksplisit, dan environment gate.

### 2.4 Generator dan penghapusan data sample

`JurnalDemoDataService` mendukung dua skenario:

- dataset demo lama: 500 jurnal × 100 artikel (`demo-500x100`);
- dataset sample landing: tepat 50 jurnal × 100 artikel (`sample-50x100`).

Karakteristik:

- idempotent berdasarkan namespace/source class;
- 100 artikel per jurnal dengan status `DRAFT`, `SUBMITTED`, `SCREENING`, `REVIEW`, `COPYEDITING`, `PRODUCTION`, `PROOF`, `SCHEDULED`, dan `PUBLISHED`;
- penulis default meminta pegawai/dosen ID 245; bila tidak tersedia menggunakan nama fallback `Prof. Dr. ASROFI RIDHO S.AG., M.SI., M.H, M.Pd, M.Psi`;
- create memerlukan capability `journals.create` dan administrator;
- delete memerlukan capability `journals.delete` dan administrator;
- delete hanya menerima key berawalan `sample-`;
- usage event yang tepat terkait item sample dihapus;
- dependensi bisnis lain diperiksa fail-closed;
- dataset demo 500×100 dan jurnal riil tidak boleh ikut terhapus.

File utama:

- `src/main/src/ais/action/master/jurnal/JurnalDemoDataService.java`;
- `src/main/src/ais/action/master/jurnal/test/JurnalDemoDataSelfTest.java`;
- route `demoSample` dalam `src/main/src/ais/action/servlet/Jurnal.java`.

Self-test terakhir yang menjalankan lifecycle penuh pernah membuktikan:

- 50 jurnal, 5.000 artikel, dan 5.000 contributor berhasil dibuat;
- seluruh sample self-test berhasil dibersihkan;
- hasil verifikasi akhir `0|0|50000`: nol item sample, nol collection sample, dan 50.000 item dataset demo tetap ada.

Generator otomatis hanya aktif bila `AIS_JURNAL_DB_NAME` mengandung `_sit`, `_uat`, `demo`, atau `fixture`. Deployment demo dengan nama database lain wajib menyetel `AIS_JURNAL_DEMO_GENERATOR_ENABLED=true`.

### 2.5 Performance lokal

Pengujian lokal pernah menggunakan dataset production-shaped:

- 100.000 artikel;
- 1.000.000 metadata file;
- 10.000.000 usage row;
- 10.000 pengguna;
- concurrent load 8 thread selama 300 detik.

Hasil terakhir yang dicatat: 1.608.291 operasi, warm p95 2,23 ms, analytic p95 1.652,22 ms, load p95 7,18 ms, throughput 5.344,88 ops/s, error 0. Schema performance sudah di-drop setelah evidence. Ini `PASS_LOCAL_SIT_V1`, bukan SLA produksi. Rincian ada di `PERFORMANCE-LOAD-SOAK-RESULT-20260823.md`.

## 3. Database dan keselamatan data

Baseline yang tidak boleh dimutasi tanpa persetujuan eksplisit:

- `ais`;
- `streaming_ais`.

Clone pengujian:

- `ais_jurnal_sit`;
- `ais_jurnal_uat`;
- `streaming_ais_jurnal_sit`;
- `streaming_ais_jurnal_uat`.

PostgreSQL lokal berada di `localhost:5432`, user `root`; password tersedia di luar dokumen. Jangan memasukkan credential tersebut ke handoff berikutnya.

## 4. Masalah terbuka terbaru: admin mencentang izin tetapi sample tetap ditolak

Gejala pada deployment demo:

```text
Data sample gagal: Hak akses jurnal tidak tersedia.
```

Ini bukan kegagalan login dan bukan environment gate. Tombol tampil karena `Common.getApakahAdmin()` benar, tetapi generator kemudian menjalankan:

```text
requireCrud(actor, "journals", "create")
requireAdministrator(actor)
```

`JurnalAuthorizationService.canCrud` mewajibkan **dua sumber izin sekaligus**:

1. role mempunyai salah satu menu jurnal fisik pada `Tbmrole.menus`/`job_has_menu` dengan ID `2000460500`–`2000460528`;
2. `Tbmrole.jurnalAksesJson` valid schema v2 dan secara eksplisit mengizinkan `menu.journals=true` serta `crud.journals.create=true`.

Panel **Hak Akses Pengelolaan Jurnal** dalam `TbmroleAction` membangun dan menyimpan JSON, tetapi checkbox `Buka` pada panel tersebut tidak otomatis menyinkronkan `Tbmrole.menus`. Assignment menu berada pada tab **Dashboard & Menu** yang berbeda. Akibatnya, tampilan dapat terlihat sudah dicentang tetapi authorization masih default-deny.

Pada pemeriksaan database localhost tanggal 24 Agustus 2026, akun demo lokal ditemukan sebagai:

```text
userid=demo
display=Maikogobai
active_role=am
```

State lokal saat pemeriksaan:

| Database | Panjang `jurnal_akses_json` | `journals.create` | Jumlah menu jurnal role `am` |
|---|---:|---:|---:|
| `ais` | 0 | false | 0 |
| `ais_jurnal_sit` | 0 | false | 29 |

Deployment `https://demo.ecampus.id/ecampus/jurnal` dapat memakai database berbeda; angka lokal di atas tidak membuktikan state database deployment. Namun code path dan penyebab default-deny sama.

Langkah operasional sementara:

1. tekan **Simpan** pada editor role;
2. pada tab **Dashboard & Menu**, assign menu jurnal yang diperlukan;
3. pastikan JSON role menyimpan `journals.create`/`journals.delete`;
4. logout dan login kembali bila deployment belum memuat mekanisme refresh cache role terbaru.

Untuk Import OJS, role juga memerlukan menu/capability `integrations` serta workflow `manageImport`.

Perbaikan produk yang disarankan untuk AI berikutnya:

1. sinkronkan checkbox `Buka` panel jurnal dengan menu fisik role secara atomik saat save, atau hilangkan dual-source gate dengan desain migration yang eksplisit;
2. jangan memberi administrator bypass diam-diam tanpa keputusan security; pertahankan least privilege;
3. tambahkan self-test save role → refresh active-role cache → authorization `journals.create`;
4. tampilkan alasan penolakan terdiagnosis untuk admin (`MISSING_JOURNAL_MENU`, `MISSING_CAPABILITY`, `DEMO_GENERATOR_DISABLED`) tanpa membocorkan detail kepada pengguna biasa;
5. uji pada database yang benar-benar dipakai deployment demo.

File yang perlu dibaca untuk perbaikan tersebut:

- `src/main/src/ais/action/maintenance/TbmroleAction.java`, terutama `bangunPanelHakAksesJurnal`, `buildJurnalAksesJson`, dan save role;
- `src/main/src/ais/action/master/jurnal/JurnalAuthorizationService.java`;
- `src/main/src/ais/common/JurnalAksesKatalog.java`;
- `src/main/src/ais/database/model/Tbmuser.java`, cache `getUserRoleYgDipakai` dan `refreshHakAksesUntukRole`;
- `src/main/webapp/sql/seed_menu_jurnal_terpadu_v1.sql`.

## 5. Status build current checkout

Checkout terus berubah akibat pekerjaan paralel. Jangan menggunakan hash lama sebagai bukti current artifact.

Observasi 24 Agustus 2026:

- source canonical: 7.130 file Java;
- source mirror: 7.130 file Java;
- helper `FeederExporter.ambilNilaiData(JSONObject,String)` yang sempat hilang sudah tersedia kembali;
- `mvn -DskipTests compile` pada 23:52 WIB berhasil, mengompilasi 17 source incremental, exit code 0;
- `JurnalPortalLoginSelfTest`: OK;
- `JurnalCatalogSelfTest`: OK;
- `JurnalAksesKatalogSelfTest`: OK;
- `JurnalRouteAuthorizationSelfTest`: OK, 28 positive dan 28 negative.

WAR yang ditemukan saat snapshot—bukan klaim release candidate baru—adalah:

```text
build/maven/ais.war
size    = 738228983 byte
SHA-256 = 3A885D81A4C36D21855B5144C406EA8BDFEA79881DC56C1BEF3F8100458140F5
modified= 24 Agustus 2026 21:13 WIB
```

WAR tersebut sudah ada sebelum compile snapshot 23:52 dan belum dibangun ulang atau diuji cold-container dalam pekerjaan dokumentasi ini. AI berikutnya wajib menjalankan clean/package, memeriksa ZIP, secret scan, dan cold-container smoke sebelum menyebutnya artifact final.

## 6. Perbedaan konfigurasi penting yang wajib diaudit

Dokumen arsitektur lama menyatakan `hibernate.hbm2ddl.auto=none`, tetapi checkout aktual pada 24 Agustus 2026 berisi:

```xml
<property name="hbm2ddl.auto">update</property>
```

Nilai tersebut ada pada kedua file:

- `src/main/src/hibernate.cfg.xml`;
- `src/main/java/hibernate.cfg.xml`.

Saat self-test database dijalankan pada 23 Agustus, mode ini menyebabkan scan/update schema yang panjang dan percobaan membuat beberapa constraint yang sudah ada. Test akhirnya exit 0 pada clone SIT, tetapi konfigurasi ini merupakan perubahan paralel dan belum diputuskan dalam scope jurnal. Jangan diam-diam mengubahnya; audit owner perubahan, risiko Envers/audit schema, dan target deployment terlebih dahulu.

## 7. Status yang belum boleh diklaim selesai

- Perbaikan sinkronisasi izin jurnal/menu role belum diimplementasikan.
- Tombol data sample pada deployment demo masih gagal berdasarkan screenshot terakhir.
- Import OJS positif pada sesi admin deployment demo belum dibuktikan setelah pemberian izin.
- WAR current belum dibangun ulang dan cold-tested setelah seluruh perubahan paralel 24 Agustus.
- Deployment produksi tidak dilakukan dalam pekerjaan ini.
- Formal SIT/UAT dengan pengguna nyata, accessibility matrix penuh, sandbox SMTP/payment/identifier deposit, dependency Severity 1/2 remediation, long soak 4–24 jam, dan sign-off/cutover tetap terbuka.
- Folder bukan VCS checkout; provenance/revision masih `UNVERIFIED_ON_CURRENT_CHECKOUT`.

## 8. Checklist aman untuk AI berikutnya

1. Baca dokumen ini dan ledger evidence; jangan menganggap seluruh Fase 0–19 selesai.
2. Identifikasi database deployment demo tanpa menampilkan credential.
3. Query read-only role aktif admin: `roleid`, panjang/validitas `jurnal_akses_json`, capability `journals.create/delete`, `integrations`, `manageImport`, dan jumlah `job_has_menu` jurnal.
4. Buat regression test yang mereproduksi save role dari `TbmroleAction` dan authorization generator.
5. Implementasikan sinkronisasi menu/JSON secara atomik serta refresh cache setelah commit.
6. Uji negative role non-admin dan role admin tanpa capability agar least privilege tidak rusak.
7. Uji generate/delete `sample-50x100` hanya pada clone disposable.
8. Pastikan deployment demo mempunyai `AIS_JURNAL_DEMO_GENERATOR_ENABLED=true` bila nama DB tidak mengandung penanda demo/SIT/UAT.
9. Jalankan compile, package, JSP compilation, secret scan, dan cold-container test.
10. Perbarui `10-IMPLEMENTATION-EVIDENCE.md`, manifest, hash WAR, serta status SIT/UAT berdasarkan hasil aktual—bukan berdasarkan asumsi.

## 9. Catatan keamanan

- Jangan menjalankan generator atau migration pada baseline `ais`/`streaming_ais`.
- Jangan menonaktifkan CSRF, administrator gate, capability gate, environment gate, atau namespace guard hanya untuk membuat demo berhasil.
- Jangan menyimpan password database dalam file konfigurasi atau dokumentasi.
- Penghapusan sample harus tetap scoped ke `AIS_JOURNAL_DEMO:sample-*` dan gagal bila dependensi bisnis tidak aman.
- Semua perubahan pada role/menu harus diaudit dan tidak boleh memberikan capability lebih tinggi kepada editor non-admin.

## 10. Status pengganti per 25 Agustus 2026

Bagian 7 dan checklist lama di atas merekam keadaan 24 Agustus. Sinkronisasi role/menu, schema-mode hardening, dependency remediation, secret scan, SIT, dan technical UAT telah dilanjutkan dan diverifikasi. Gunakan `16-HANDOFF-PRIORITAS-UTAMA-20260825.md` sebagai handoff terbaru. WAR/deploy tetap belum dilakukan atas instruksi pemilik sistem; formal UAT dan gate eksternal tetap terbuka.
