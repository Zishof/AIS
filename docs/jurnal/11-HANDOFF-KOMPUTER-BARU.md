# Handoff implementasi jurnal terpadu AIS

Tanggal snapshot informasi: **23 Agustus 2026 (Asia/Jakarta)**.

Dokumen ini adalah titik masuk untuk melanjutkan pekerjaan pada komputer lain. Path project harus tetap:

```text
C:\opt\AIS\ais
```

Kode Java berada di `C:\opt\AIS\ais\src\main\src`, web di `C:\opt\AIS\ais\src\main\webapp`, dan dokumentasi jurnal di `C:\opt\AIS\ais\src\main\docs\jurnal`.

> Jangan menganggap seluruh Fase 0–19 selesai. Implementasi inti dan banyak gate lokal sudah tersedia, tetapi Fase 16 masih mempunyai blocker dependency; SIT/UAT formal, sandbox provider, performance, accessibility, dan cutover belum ditutup.

> **Addendum komputer lanjutan (23 Agustus 2026, 03:08 WIB):** clean offline package dan seluruh 28 self-test awal lulus setelah upgrade jsoup; targeted generator test ke-29 juga lulus. WAR terbaru berukuran 737.796.422 byte dengan SHA-256 `419AF20A47981ECFC293C0685F4095C68AE1008149398A21842BD377E9EC13A9`. Migration/verifier lulus idempoten pada clone SIT dan UAT. Technical SIT serta technical UAT smoke dua-database lulus. Fase 16 tetap blocked dan sign-off formal/cutover belum dilakukan; rincian authoritative terbaru ada pada addendum `10-IMPLEMENTATION-EVIDENCE.md`.

`META-INF/context.xml` pada komputer lanjutan tidak lagi menyimpan password plaintext dan mengharuskan Tomcat menerima system property `AIS_DB_PASSWORD`. Validasi cold-container/JNDI dengan property eksternal wajib dilakukan sebelum artefak disebut RC; jangan mengganti placeholder dengan secret di repository.

Generator data demo tersedia pada menu admin **Master Jurnal → Tindakan → Generate 500 jurnal demo × 100+ artikel**. Ia hanya menerima admin+CSRF, konfirmasi `GENERATE-DEMO-500X100`, jumlah jurnal tepat 500, artikel 100–200 per jurnal, dan idempotency key 8–40 karakter. Pada SIT/UAT/fixture ia otomatis diizinkan; deployment demo lain wajib menyetel `AIS_JURNAL_DEMO_GENERATOR_ENABLED=true`. ID dosen default 245; jika record ada, nama aktual dipakai, jika tidak maka dibuat authority penulis demo dari nama fallback. Run `demo-500x100` sudah mengisi `ais_jurnal_sit` dengan 500 jurnal/50.000 artikel dan rerun terbukti tidak menduplikasi data.

## 1. Yang wajib dipindahkan

Pindahkan **seluruh** folder `C:\opt\AIS\ais`, bukan hanya `src\main`. Folder root juga memuat `pom.xml`, laporan build, dump baseline, dependency report, dan fixture/test artifact yang diperlukan untuk verifikasi.

Minimal yang harus ikut:

- `C:\opt\AIS\ais\src\main\src`
- `C:\opt\AIS\ais\src\main\webapp`
- `C:\opt\AIS\ais\src\main\docs\jurnal`
- `C:\opt\AIS\ais\pom.xml`
- `C:\opt\AIS\ais\tmp\jurnal-baseline`
- `C:\opt\AIS\ais\build\maven\dependency-check-report.json`
- seluruh JAR di `C:\opt\AIS\ais\src\main\webapp\WEB-INF\lib`

Sebelum menyalin, tutup task Codex/Maven yang masih berjalan agar `build\maven` tidak berubah saat proses copy. Setelah copy, verifikasi checksum penting pada bagian 8.

## 2. Aturan database yang tidak boleh berubah

PostgreSQL lokal snapshot 2026-08-23 menggunakan host `localhost`, port `5432`, user `root`. Password harus dimasukkan melalui environment/secret lokal dan **tidak ditulis ke source, XML, log, atau Markdown**.

Baseline read-only selama development:

- main AIS: `ais`
- BLOB/streaming: `streaming_ais`

Target mutation pengujian:

- main SIT: `ais_jurnal_sit`
- main UAT: `ais_jurnal_uat`
- streaming SIT: `streaming_ais_jurnal_sit`
- streaming UAT: `streaming_ais_jurnal_uat`

Jangan mengarahkan migration, Hibernate test, importer, atau SIT/UAT ke `ais` maupun `streaming_ais`.

Environment utama:

```text
AIS_JURNAL_DB_HOST=localhost
AIS_JURNAL_DB_PORT=5432
AIS_JURNAL_DB_NAME=ais_jurnal_sit
AIS_JURNAL_DB_USER=root
AIS_JURNAL_DB_PASSWORD=<external secret>

AIS_JURNAL_STREAMING_DB_HOST=localhost
AIS_JURNAL_STREAMING_DB_PORT=5432
AIS_JURNAL_STREAMING_DB_NAME=streaming_ais_jurnal_sit
AIS_JURNAL_STREAMING_DB_USER=root
AIS_JURNAL_STREAMING_DB_PASSWORD=<external secret>
```

Untuk UAT, ganti hanya nama database menjadi clone UAT. Detail environment integrasi publik/provider ada di `08-IMPLEMENTATION-PHASES-AND-RELEASE-GATES.md`.

## 3. Dokumen Markdown yang sudah dibuat/diperbarui

| File | Isi dan status |
|---|---|
| `00-AUDIT-BASELINE-AIS.md` | Audit kode/database existing dan keputusan existing-first. |
| `01-OJS-3505-FEATURE-INVENTORY.md` | Inventaris fitur OJS 3.5.0-5. |
| `02-OJS-3505-TABLE-MAPPING.md` | Mapping lengkap **134/134 tabel dan 905/905 field** beserta keputusan MERGED/ALTER/NEW_MODEL/DERIVED/N/A. |
| `03-OJS-3505-PLUGIN-PARITY.md` | Matriks 45 plugin, implementasi native, dan validator resmi. |
| `04-ARSITEKTUR-MODUL-JURNAL.md` | Arsitektur existing-first, schema `penelitiandanpengabdian`, main/streaming boundary, serta keputusan tanpa ORM lintas database. |
| `05-RENCANA-MIGRASI-OJS.md` | Strategi migration/import version-aware, dry-run, checkpoint, resume, provenance, dan rekonsiliasi file. |
| `06-SECURITY-AND-RBAC-MATRIX.md` | RBAC memakai `Tbmrole.jurnalAksesJson`, capability/action matrix, active role, dan object-level authorization. |
| `07-TEST-AND-ACCEPTANCE-MATRIX.md` | Test matrix dan release-gate aktual, termasuk dependency blocker. |
| `08-IMPLEMENTATION-PHASES-AND-RELEASE-GATES.md` | Rencana lengkap Fase 0–19 dan exit gate yang tidak boleh dihapus. |
| `09-FEATURE-TRACEABILITY-REGISTER.md` | Traceability requirement ke service/entity/route/test/evidence. |
| `10-IMPLEMENTATION-EVIDENCE.md` | Ledger bukti aktual: migration, tests, importer, schema validator, WAR, cold smoke, dan temuan security. |
| `11-HANDOFF-KOMPUTER-BARU.md` | Dokumen handoff ini. |

Generator mapping juga tersedia sebagai `Generate-Ojs3505TableMapping.ps1` di folder yang sama.

## 4. Keputusan arsitektur final yang telah diterapkan

- Modul jurnal memakai `HibernateUtil.java` dan `hibernate.cfg.xml` existing pada schema `penelitiandanpengabdian`; bukan SessionFactory OJS tersendiri.
- `OjsHibernateUtil`/`hibernate.ojs.cfg.xml` tidak dipakai importer jurnal baru. Reader OJS memakai JDBC eksternal read-only dan named connection reference.
- BLOB file memakai `streaming_ais` melalui `StreamingHibernateUtil` existing dan `LampiranJurnal`; main database hanya menyimpan scalar reference/checksum/metadata. Tabel `lampiran_jurnal` berada hanya di streaming dan tidak ada ORM lintas database.
- Existing entities/tables yang dipakai ulang antara lain `Artikel`, `JurnalPenelitian`, `FileArtikel`, `AnggotaArtikel`, repository domain, `Diskusi`, `DiskusiKomentar`, `LogPembayaran`, serta data perguruan tinggi.
- RBAC jurnal disimpan pada kolom JSON existing-extension `Tbmrole.jurnalAksesJson`. Pengaturan berada pada tab **Hak Akses Pengelolaan Jurnal** di flow `TbmroleAction`; enforcement tidak hanya di UI, tetapi juga di service/object scope.
- Menu memakai hierarchy `root=46`, child `4605`, lalu submenu `460501–460528`; `Menu.id` tetap primary key terpisah.
- UI canonical berada di `webapp\WEB-INF\baru\modul\jurnal\*` dan route didaftarkan melalui `NewUiRouteRegistry`/menu existing.
- Migration SQL idempoten; Hibernate tetap `hbm2ddl.auto=none`.

## 5. Implementasi kode yang sudah tersedia

### Service/domain

Folder utama: `src\main\src\ais\action\master\jurnal`.

Sudah tersedia service untuk administration, authorization, workflow, stage assignment, contributor, reviewer/invitation, discussion, file, galley, publication, identifier, public discovery/citation, access/subscription/IP, email/template/preference, payment callback, usage/report, plugin parity/integration, health, rate limiting, dan admin workspace.

Importer berada di `src\main\src\ais\action\master\jurnal\importer`, meliputi source catalog OJS 3.5/legacy, preflight, transform catalog, execution, checkpoint/cancel/resume, reconciliation, domain transform, dan file streaming.

### Model

- 12 tabel domain/import jurnal berada pada package `ais.database.model.jurnal`.
- Repository model existing/extension berada pada package `ais.database.model.repository`.
- `Tbmrole` telah mempunyai `jurnalAksesJson`.
- Mapping jurnal dan repository dimasukkan ke `hibernate.cfg.xml` existing.

### Servlet dan UI

Servlet utama:

- `Jurnal.java`
- `JurnalAdminApi.java`
- `JurnalFile.java`
- `JurnalGalley.java`
- `JurnalPaymentCallback.java`
- `JurnalReport.java`
- `Oai.java`

JSP canonical:

- `webapp\WEB-INF\baru\modul\jurnal\landing_page.jsp`
- `webapp\WEB-INF\baru\modul\jurnal\admin.jsp`

Mapping servlet/filter berada di `webapp\WEB-INF\web.xml`.

### SQL migration/verification

Folder `src\main\webapp\sql`:

- `migrasi_prasyarat_repository_jurnal_v1.sql`
- `migrasi_jurnal_terpadu_v1.sql`
- `seed_menu_jurnal_terpadu_v1.sql`
- `verifikasi_prasyarat_repository_jurnal_v1.sql`
- `verifikasi_jurnal_terpadu_v1.sql`
- `verifikasi_menu_jurnal_terpadu_v1.sql`
- `rollback_jurnal_terpadu_v1.sql`

## 6. Evidence terakhir yang sudah lulus

Evidence ini berlaku pada source/dependency **sebelum** perubahan jsoup terakhir di bagian 7 dan harus diregresi ulang setelah pindah komputer.

- Baseline main: 3.277 tabel.
- Fingerprint current SIT: 3.322 tabel main, termasuk tepat 12 tabel jurnal.
- Fingerprint current streaming SIT: 83 tabel, termasuk tepat satu `lampiran_jurnal`; lifecycle test meninggalkan 0 baris.
- Backup/restore drill main dan streaming: lulus.
- Menu: parent + 28 submenu, seed ulang idempoten.
- RBAC route contract: 28/28 positive read dan 28/28 default-deny negative.
- Aggregated journal self-tests: **28/28 lulus** pada clone/fixture terisolasi.
- Importer OJS 3.5: preflight tepat `3.5.0-5`, **134 tabel/905 field**, dry-run, execute, rerun, cancel/resume, provenance, file reconciliation dan checksum lulus.
- Importer legacy: 7 tabel/37 field, execute dan rerun idempoten lulus.
- Export resmi: Crossref 5.3.1, DataCite 4.5, DOAJ Articles XSD 1.3, dan PubMed DTD 3.0 lulus.
- Cold Tomcat smoke: public home/browse/search/feed/OAI merespons; protected route, CSRF/consent, galley 404/422, CSP dan `nosniff` berperilaku sesuai.
- Setelah test, fixture journal/import/assignment tidak tersisa pada clone dan baseline tidak dimutasi.

## 7. Perubahan dependency terakhir—belum menjadi build terverifikasi

OWASP Dependency-Check 13.0.0 dengan NVD 2.0/CISA KEV menemukan 302 catatan CVE pada 72 komponen dari 184 JAR fisik/1.762 komponen tertanam. Temuan legacy utama meliputi Spring 3.1.4, Log4j lama, PostgreSQL 42.2.10, Commons FileUpload, Axis, Hibernate 3, XStream/Jackson tertanam, dan jsoup lama. Ini membuat exit gate Fase 16 masih **BLOCKED** sampai triage/remediasi Severity 1/2 selesai.

Triage menemukan jsoup langsung reachable dari sanitizer galley. Perubahan terakhir yang sudah ada di worktree:

- `JurnalGalleyViewerService.java` menggunakan `org.jsoup.safety.Safelist`.
- `jsoup-1.13.1.jar` diganti dengan `jsoup-1.23.1.jar`.
- SHA-256 `jsoup-1.23.1.jar`: `8B15E2B28EEB1E0A88A9B7DAB4DC0C23524491C56959785DEA22F7846897B668`.
- Fat JAR `docear-metadata-lib-0.0.1.jar` sebelumnya membawa 235 entry `org/jsoup/*` versi lama. Entry duplikat itu sudah dihapus dari JAR aktif agar classpath tidak mencampur `Jsoup` lama dan `Safelist` baru.
- SHA-256 Docear aktif/unshaded: `DAE3F60C755022B7D7FE916B9E633D33C1428BEF9E71645584ACE78CD396F288`.
- Salinan Docear asli tersedia sebagai `docear-metadata-lib-0.0.1.jar.original-backup`, SHA-256 `23037CF55E792DDF51CAC3AC9B1C8DAA1A7D93BBC42A1E08C2CE24BB1DAA02B0`.

Compile yang dimulai setelah perubahan tersebut sempat menghasilkan ulang `JurnalGalleyViewerService.class`, tetapi lifecycle Maven belum mempunyai hasil `BUILD SUCCESS` yang terdokumentasi ketika handoff ini dibuat. Oleh karena itu:

- **jangan** menganggap upgrade dependency sudah lulus;
- **jangan** memakai WAR lama sebagai hasil dari upgrade;
- lakukan full clean compile/package dan seluruh regression lebih dahulu;
- uji juga flow Google Scholar/Docear, karena Docear sekarang memakai jsoup 1.23.1 eksternal.

## 8. Artefak dan checksum

### Artefak terakhir yang terverifikasi sebelum upgrade jsoup

- WAR: `build\maven\ais.war`
- Size: `739.616.271` byte
- SHA-256: `BC8466E47832D4A645A43B903E030DB30A8BC94E76FC9E32186B28A746ABF4C6`
- Waktu build: 23 Agustus 2026 00:10 WIB

WAR ini adalah baseline regression lama, **bukan** kandidat setelah perubahan dependency terbaru.

### Dependency report

- File: `build\maven\dependency-check-report.json`
- Size: `6.567.037` byte
- SHA-256: `465E84CB378FA1C790A183BBB592D133ACCFCF236F066AF1F837DEAE88C33716`

### Dump baseline

- `tmp\jurnal-baseline\ais-before-jurnal-fase-0-19.dump`
  - SHA-256 `99E31D3117B587ADB5DBB2AB483E2AC22F2BCF3CF2F12B6F66BF90CE4CECE95F`
- `tmp\jurnal-baseline\streaming-ais-before-jurnal-fase-0-19.dump`
  - size `3.410.318` byte
  - SHA-256 `162B558542B20A487CB443C3639B94A7D89BAC088D24A939E85FFC02BE973263`

Sesudah copy, jalankan `Get-FileHash -Algorithm SHA256` terhadap keempat artifact tersebut. Jika checksum berbeda, jangan lanjut migration/test sebelum sumber perbedaan ditemukan.

## 9. Urutan melanjutkan pada komputer baru

1. Pasang/validasi JDK, Maven, PostgreSQL client 17, dan Tomcat 9 yang kompatibel.
2. Pastikan project berada tepat di `C:\opt\AIS\ais`.
3. Verifikasi checksum dump, dependency report, jsoup, Docear, dan WAR baseline.
4. Restore/refresh clone SIT/UAT dari baseline dump; jangan restore di atas `ais` atau `streaming_ais`.
5. Set environment main/streaming ke clone dan secret secara lokal.
6. Jalankan migration prerequisite, migration jurnal, menu seed, lalu ketiga verification SQL.
7. Jalankan full clean compile/package. Build harus memberi `BUILD SUCCESS`; hitung size/SHA-256 WAR baru.
8. Jalankan regression sanitizer galley, Docear/Google Scholar, 28 journal self-tests, importer OJS 3.5 + legacy, official XML validators, dan clone hygiene.
9. Jalankan ulang OWASP Dependency-Check. Pastikan tidak ada lagi standalone/shaded jsoup lama pada classpath/report. Triage seluruh critical/high berdasarkan reachability dan ownership; jangan melakukan suppression tanpa alasan/evidence.
10. Ulangi cold Tomcat smoke dari WAR baru.
11. Lanjutkan gate yang masih terbuka: authenticated 28-menu DOM/CSRF/IDOR, valid HTML/JATS/PDF galley HTTP fixture, OAI seluruh verb validator, accessibility/crawler, performance/load/soak, SMTP/payment/deposit sandbox, dan COUNTER validator.
12. Baru setelah Fase 16 lulus, jalankan SIT formal pada clone SIT, UAT formal pada clone UAT, restore/rollback/cutover drill, sign-off, lalu Fase 19.

Contoh build dari root project:

```powershell
Set-Location 'C:\opt\AIS\ais'
mvn -o -DskipTests clean package
Get-FileHash -LiteralPath 'C:\opt\AIS\ais\build\maven\ais.war' -Algorithm SHA256
```

Jika cache Maven belum ikut dipindahkan atau dependency belum tersedia lokal, jalankan build online terkontrol terlebih dahulu, lalu ulangi build offline untuk evidence reproducibility.

## 10. Status Fase 0–19 saat handoff

| Fase | Status ringkas |
|---|---|
| 0 | PASS: backup, clone, dan restore drill main/streaming tersedia. |
| 1 | PASS: baseline/inventory/mapping/traceability terdokumentasi. |
| 2 | PASS: arsitektur existing-first dan migration idempoten tersedia. |
| 3 | PARTIAL_PASS: RBAC JSON/menu/route contract lulus; authenticated UI matrix formal masih terbuka. |
| 4 | PARTIAL_PASS: administration/workspace tersedia; full HTTP DOM/CSRF suite belum lengkap. |
| 5 | PARTIAL_PASS: submit-to-publish journey lulus; exception/multi-round UI coverage belum penuh. |
| 6 | PARTIAL_PASS: reviewer/invitation/anonymity core tersedia; seluruh matrix review formal belum penuh. |
| 7 | PARTIAL_PASS: copyedit/production/proof/publish journey lulus; full UI suite terbuka. |
| 8 | PARTIAL_PASS: DOI/URN/issue/publication dan empat schema validator lulus; provider sandbox terbuka. |
| 9 | PARTIAL_PASS: portal/discovery/citation/feed tersedia; browser accessibility/crawler terbuka. |
| 10 | PARTIAL_PASS: subscription/access IPv4+IPv6 core lulus; full policy/reminder/cache matrix terbuka. |
| 11 | PARTIAL_PASS: template/preferences/digest/capture lulus; SMTP sandbox terbuka. |
| 12 | PARTIAL_PASS: payment HMAC/idempotency/existing tables lulus; provider sandbox/reconciliation terbuka. |
| 13 | PARTIAL_PASS: native parity/exports/OAI behavior tersedia; external protocol/COUNTER/sandbox gates terbuka. |
| 14 | PARTIAL_PASS: usage capture/aggregate/CSV/COUNTER JSON tersedia; large data/privacy/performance terbuka. |
| 15 | PARTIAL_PASS: 134/905 importer dan legacy core lulus; long-tail collision UI/manual resolution belum penuh. |
| 16 | BLOCKED: dependency Severity 1/2 terbuka; performance/accessibility/security regression belum selesai. |
| 17 | TECHNICAL_PARTIAL_PASS: migration, 28/28 self-test, importer 134/905+legacy, streaming/file dan health lulus; planned SIT formal, external sandbox, performance/security/accessibility dan tanda tangan masih terbuka. |
| 18 | TECHNICAL_SMOKE_PASS / FORMAL_NOT_RUN: smoke main+streaming UAT lulus; actor/business journeys dan sign-off belum dilakukan. |
| 19 | NOT_RUN / NOT_AUTHORIZED: entry gate 16–18, endpoint pilot, owner, freeze window dan persetujuan cutover belum tersedia. |

Status authoritative terperinci tetap berada di `10-IMPLEMENTATION-EVIDENCE.md`. Jika hasil baru berbeda dari dokumen ini, perbarui ledger evidence dan dokumen handoff dengan output/checksum aktual; jangan sekadar mengubah label menjadi PASS.

