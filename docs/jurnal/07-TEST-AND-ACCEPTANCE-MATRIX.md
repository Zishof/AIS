# Test and acceptance matrix

Keputusan test persistence bersifat **existing-first**. Entitas AIS yang dipakai ulang tetap diuji pada schema existing-nya melalui `HibernateUtil`/`hibernate.cfg.xml`; hanya 12 tabel domain/import yang benar-benar baru ditempatkan pada schema `penelitiandanpengabdian`. Fixture OJS dibaca melalui JDBC read-only dan test harus membuktikan tidak ada dependency baru terhadap `OjsHibernateUtil`/`hibernate.ojs.cfg.xml`. Metadata file berada di `RepoBitstream`, sedangkan content BLOB memakai `LampiranJurnal` melalui `StreamingHibernateUtil` dengan scalar reference—dibuat tepat satu tabel `lampiran_jurnal` pada streaming.

## Phase gates

| Gate | Required evidence | Current state |
|---|---|---|
| Source integrity | exact OJS and pkp-lib commits | PASS |
| Table coverage | generator reports 134/134, no unresolved marker | PASS |
| Field coverage | generator reports 905/905 source fields with explicit target/provenance decision | PASS |
| Plugin inventory | 45/45 bundled plugins inventoried; none falsely claimed complete | PASS |
| Email inventory | 73/73 unique `registry/emailTemplates.xml` keys at pinned commit | PASS |
| Admin menu inventory | 28 canonical submenu codes `460501..460528`, with separate `Menu.id` | PASS |
| Existing-first decision | 37 ALTER_EXISTING + 69 MERGED + 4 DERIVED + 11 NEW_MODEL + 13 NOT_APPLICABLE | PASS |
| New-table budget | 6 domain workflow + 2 support + 4 import-control = 12; hard review above 15 | PASS (design) |
| Fresh/legacy classification | 132 fresh/plugin + 2 legacy-source-only | PASS |
| AIS compatibility audit | existing→decision table and dirty-worktree risk | PASS |
| Baseline compile | `mvn -DskipTests compile`, 6,949 sources, target Java 8 | PASS in 3:49 on JDK 25.0.3; warnings only |
| Repository physical schema | all mapped `Repo*` tables/columns/indexes exist in the deployment DB; migration version recorded | PASS pada clone SIT; empat prerequisite repository direkonsiliasi dan migration idempoten |
| Model/schema | empty DB, legacy upgrade clone, Envers reconciliation, exactly 12 approved new tables | PASS pada clone upgrade SIT: 3.277 baseline + 4 prerequisite repository + tepat 12 tabel jurnal |
| Workflow/RBAC | state and permission unit/integration tests | PARTIAL_PASS; submit-to-publish, assignment scope, 28 capability/menu, payment, file dan negative gates utama lulus; full IDOR/UI matrix tetap wajib |
| Portal/admin | canonical `WEB-INF/baru/modul/jurnal/*` routes, `TbmroleAction` journal tab, UI/security tests | PARTIAL_PASS; cold WAR final membuktikan home/browse/search/feed/OAI, galley 404/422, consent 405/403 dan protected admin redirect; authenticated 28-menu DOM/CSRF suite belum lengkap |
| Import | legacy and OJS 3.5 fixtures, dry-run/resume/reconcile | PASS core reconciliation: 134/134, 905/905, cancel/resume/checkpoint dan file reconciliation; long-tail semantic collision UI tetap wajib |
| Performance | documented dataset and p95/heap/query counts | PASS_LOCAL_SIT_V1: full 100 jurnal/100k artikel/1m file/10m usage/10k user; 8 thread × 300 detik; warm p95 2,23 ms, analytics p95 1.652,22 ms, load p95 7,18 ms, 5.344,88 ops/s, error 0, heap 203.459.480 byte. SLA production/HTTP/WAL/long soak owner tetap pending |
| Landing login/admin controls | anonymous, invalid CSRF/password, authenticated admin/non-admin rendering | PASS targeted: login AIS POST+CSRF dan generic failure diuji pada Tomcat/browser lokal; password tidak direfleksikan, mobile 390 px tanpa overflow. Tombol Import OJS serta create/delete sample dibatasi server-side `Common.getApakahAdmin()` dan service authorization; positive login memakai sesi admin pada deployment, sedangkan credential AIS riil tidak dicatat dalam test evidence |
| Sample dataset lifecycle | admin create 50×100, idempotency, scoped cleanup | PASS SIT: namespace self-test membuat 50 jurnal/5.000 artikel/5.000 contributor lalu cleanup menghasilkan 50/5.000/5.000 removed dan 0 row tersisa; cleanup menolak dataset yang sudah memiliki dependency/activity |

## SIT/UAT environment contract

Approved PostgreSQL endpoint is `localhost:5432`, with main baseline `ais` and streaming baseline `streaming_ais`, user `root`; password is injected as an external secret and must not be committed or printed. Before any schema/destructive test, Phase 1 in `08-IMPLEMENTATION-PHASES-AND-RELEASE-GATES.md` requires verified backup/restore and separate clones (`ais_jurnal_sit`, `ais_jurnal_uat`, `streaming_ais_jurnal_sit`, `streaming_ais_jurnal_uat`). Email, payment and identifier deposits use capture/sandbox/test endpoints. Direct testing against either baseline requires an explicit restore point, maintenance window and cutover approval.

## Required test suites

### Unit/service

- valid/invalid transitions and immutable published version;
- DOI/URN normalization and uniqueness;
- active-role + `job_has_menu` + `jurnalAksesJson` + scoped assignment matrix;
- `JurnalAksesKatalog` parse/normalize/version tests: null, empty, malformed, duplicate, oversized, unknown key/version and non-boolean values all deny;
- `TbmroleAction` tab load/save round-trip, **Semua** per-row behavior, unknown-key preservation without grant, optimistic conflict, and explicit permission-change audit;
- conflict of interest and reviewer anonymity;
- template variable allowlist and message idempotency;
- versioned `accessPolicyJson` catalog validation, stable policy keys and immutable `LanggananJurnal.policySnapshotJson` after activation/payment;
- versioned `workflowProfileJson` review-form validation: stable keys, locale, element type/options, sequence, required/included flags, append-only published versions and size/depth/count limits;
- reviewer `responseJson` type validation, form-version match, checksum, immutability after submit and audited replacement/version behavior;
- review round start/close event ordering and consistency of `roundNumber` across assignment/file/event;
- `RepoCollection` hierarchy/type/profile/access-policy validation and journal isolation;
- `RepoItem.documentType` isolation, lifecycle/version/DOI/OAI/tombstone behavior, and compatibility with non-journal repository items;
- `RepoItemMetadata`, contributor/authority and relation ordering/category behavior;
- `RepoBitstream` MIME/path/checksum/bundle/access/version state and storage-backend dispatch;
- import transforms, dialect signatures and collision decisions;
- metric aggregation and retention.

### Database/integration

- Hibernate bootstrap from empty DB;
- upgrade clone containing legacy article/journal/file/member and `new_audit` data;
- explicit mapping registration, physical-table existence, FK/unique/index introspection and migration-version evidence for every reused `Repo*` entity;
- existing models remain in their existing schemas; exactly 12 approved new tables are created in `penelitiandanpengabdian` and all non-streaming ORM relations resolve in one main SessionFactory;
- schema diff fails on an unapproved thirteenth table unless the architecture document and source-to-target mapping contain clone-schema/constraint/analytics evidence and approval;
- no `TipeLanggananJurnal`, `PutaranReviewJurnal`, `FormReviewJurnal`, `ElemenFormReviewJurnal`, or `JawabanReviewJurnal` table is created;
- imported subscription types reconcile to versioned access-policy entries and every subscription keeps an immutable source-equivalent snapshot;
- imported review forms/elements reconcile to immutable workflow-profile versions; responses reconcile to assignment snapshots without loss of locale/type/order/required/value semantics;
- OJS editorial queries reuse extended `Diskusi`/`DiskusiKomentar`; only `PesertaDiskusiJurnal` is created and `(diskusi,user)` membership is unique/indexed;
- `JurnalPenelitian` is the canonical journal master; `Artikel`, `FileArtikel`, and `AnggotaArtikel` are compatibility projections and never become a competing workflow source of truth;
- `RepoCollection`/`RepoItem` rows belonging to repository documents outside the journal module are unchanged by journal migrations, imports, deletes and authorization queries;
- cascade/lazy behavior and optimistic locking;
- rollback for publish/import/payment/deposit failures;
- main-vs-streaming failure compensation and orphan reconciliation;
- large-file bounded streaming through `LampiranJurnal(repoBitstreamId=RepoBitstream.id)` without an ORM cross-database relation;
- file retrieval supports the selected backend explicitly; a BLOB-backed `pathSistem` is never treated unconditionally as a local `java.io.File` path;
- batch resume after failure;
- source JDBC benar-benar read-only, query berparameter, timeout/cancellation bekerja, dan importer baru tetap berfungsi ketika konfigurasi OJS Hibernate legacy tidak tersedia;
- two source instances with colliding IDs, usernames, slugs and identifiers;
- generated 134-table/905-field mapping and per-column reconciliation;
- all 13 `NOT_APPLICABLE_WITH_RATIONALE` runtime/staging tables remain counted in source coverage but create no AIS table; PHP job/filter/session/token/temporary payloads are never deserialized or executed;
- OJS raw/provenance data needed for unmappable source values is retained in `ImportMappingOjs`, not by cloning source tables;
- existing `Notifikasi`/`NotifikasiDibaca`, payment, institution and announcement models pass journal-use regression tests without duplicate journal-specific equivalents.

### Servlet/UI/security

- anonymous public routes and protected redirect/403 behavior;
- every journal public/management view resolves only under `WEB-INF/baru/modul/jurnal/*`; any retained legacy alias redirects and does not host a second authorization path;
- journal management tab appears after Hak Akses Pedagang and persists only `Tbmrole.jurnalAksesJson` through the central catalog;
- `job_has_menu` without JSON capability denies; JSON capability without module entry denies; `RolePrivilage` cannot act as journal CRUD fallback;
- null/invalid/missing capability is fail-closed in both UI and Java service; hidden buttons are backed by server-side negative tests;
- object-level cross-journal/cross-tenant negative cases;
- a role with editor capability for Journal A cannot read or mutate Journal B without scoped assignment; multiple stored roles are not unioned;
- permission editor cannot grant capabilities it lacks, except explicit audited site-admin override;
- CSRF, XSS, upload, traversal, SSRF and blinded metadata tests;
- keyboard/mobile/WCAG smoke tests;
- canonical/SEO/OAI/feed/sitemap validators.
- repository services, OAI servlet and existing non-journal portal routes pass regression tests after journal registration.

### End-to-end

1. internal lecturer + student + external co-author submission;
2. editor assignment and double-anonymous multi-reviewer round;
3. reviewer response/files/recommendation;
4. revision, second round, accept, copyedit, produce and proof;
5. issue schedule, DOI, publish and reader notification;
6. public search/article/citation/galley access;
7. idempotent link to Penelitian dan Pengabdian profile/report;
8. institutional subscription/IP allow and deny;
9. OJS 3.5 preflight, dry run, execute, resume and reconciliation;
10. withdraw/retract/new version with identifier history and OAI tombstone.
11. import OJS role groups in dry-run, require administrator mapping, create scoped assignments/provenance, and prove no duplicate journal role/group table or silent capability grant.

## Performance acceptance dataset

100 journals, 100,000 submissions/articles, 1,000,000 file metadata/revision references, 10,000,000 usage rows in a dedicated test, and 10,000 multi-role users. Record environment, warm/cold state, p50/p95, SQL count, heap peak and throughput. Targets are not marked passed without measurements.

## Commands introduced in discovery

```powershell
& docs/jurnal/Generate-Ojs3505TableMapping.ps1
```

Expected result: `Coverage: 134/134; fresh/plugin=132; legacy-source-only=2`, with 905 field rows and the decision distribution `37/69/4/11/13`. Future phases must add build/unit/integration commands and real outputs here after each gate.

Latest release-candidate evidence (2026-08-23): full package compiled 7,043 sources; WAR size 739,616,271 byte, SHA-256 `BC8466E47832D4A645A43B903E030DB30A8BC94E76FC9E32186B28A746ABF4C6`; 28/28 journal self-tests passed on isolated clones/fixtures, including Crossref 5.3.1, DataCite 4.5, DOAJ 1.3, and PubMed DTD 3.0 official validation; cold Tomcat startup completed in 366,228 ms with no new `SEVERE`. OWASP Dependency-Check terhadap 184 JAR fisik menemukan 72 komponen dengan 302 catatan CVE (53 critical/127 high), terutama dependency legacy AIS; laporan `build/maven/dependency-check-report.json` ber-SHA-256 `465E84CB378FA1C790A183BBB592D133ACCFCF236F066AF1F837DEAE88C33716`. Karena triage/remediasi Severity 1/2 belum selesai, ini belum merupakan RC yang dapat dirilis dan tidak menutup sandbox deposit, performance, accessibility, formal SIT, atau UAT gates.

Addendum komputer lanjutan (03:08 WIB): sesudah upgrade jsoup dan hardening credential, clean offline package mengompilasi 7.047 source/46 resource dengan Java 8 dan menghasilkan WAR 737.796.422 byte/72.084 entry, SHA-256 `419AF20A47981ECFC293C0685F4095C68AE1008149398A21842BD377E9EC13A9`. Seluruh 28 self-test awal kembali lulus, termasuk viewer jsoup, OJS 134/905, legacy 7/37, file reconciliation, two-database lifecycle, dan health; technical UAT smoke juga lulus. Laporan Dependency-Check lama tidak ikut tersalin, sehingga angka inventory lama dipertahankan sebagai blocker historis yang belum ditriage, bukan diklaim sebagai scan ulang artifact terbaru.

Targeted test tambahan `JurnalDemoDataSelfTest` menambah inventory menjadi 29 class self-test dan lulus pada clone SIT: tepat 500 journal/50.000 article/50.000 contributor, sembilan status workflow, fallback authority saat pegawai 245 tidak ada, serta rerun idempoten tanpa row baru. Ini adalah evidence generator/load fixture, bukan pengganti load/soak threshold Fase 16.

Addendum current checkout (05:48 WIB): inventory menjadi 35 self-test. Seluruh kelas telah dijalankan pada clone/fixture yang sesuai dan lulus setelah perbaikan fixture reviewer ke key canonical `review-assignments`, retry manifest file berstatus `FAILED`, dan byte fixture PDF valid. Import OJS 3.5 membuktikan 134/905, cancel/resume dan final file reconciliation ke `LampiranJurnal`; legacy membuktikan 7/37. `JurnalPerformanceSmokeSelfTest` merekam characterization di atas tanpa mengklaim threshold PASS.

Baseline compile executed before runtime journal changes:

```powershell
cd C:\opt\AIS\ais
mvn -DskipTests compile
```

Result: `BUILD SUCCESS`; warnings concerned existing Java 8 bootstrap/deprecation/value-identity usage and did not originate in `docs/jurnal`.

