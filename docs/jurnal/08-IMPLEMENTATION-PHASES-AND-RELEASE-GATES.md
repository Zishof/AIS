# Fase implementasi sistem Jurnal Terpadu eCampus

Dokumen ini adalah urutan implementasi resmi. Tidak ada fase yang boleh dinyatakan selesai hanya karena tabel, halaman, atau endpoint sudah tersedia. Setiap item wajib mempunyai requirement, implementasi, automated test, evidence SIT/UAT bila relevan, dan status rekonsiliasi.

## Ledger kelengkapan wajib

Satu feature-traceability register dibuat pada Fase 0 dengan kolom minimum:

`requirementId`, sumber, area, source table/field, plugin/email/menu key bila ada, target service/entity/route, permission key, test ID, migration/import evidence, owner, status, blocker, dan release version.

Register harus mempertanggungjawabkan seluruh baseline berikut:

| Baseline | Jumlah | Sumber canonical | Syarat selesai |
|---|---:|---|---|
| OJS/PKP source tables | 134 | `02-OJS-3505-TABLE-MAPPING.md` | 134 decision + transform + reconciliation test |
| OJS/PKP source fields | 905 | field rows pada mapping generated | 905 explicit target/provenance outcomes; tidak ada unmapped |
| Bundled plugins | 45 | `03-OJS-3505-PLUGIN-PARITY.md` | 45 disposition + native behavior test/proof N/A |
| Email template keys | 73 | pinned `registry/emailTemplates.xml` | 73 original AIS ID/EN definitions, variable policy dan delivery test |
| Admin submenus | 28 | `root=4605`, `child=460501..460528` | route, menu gate, capability, negative authorization dan UI test |
| Product areas | 12 | `01-OJS-3505-FEATURE-INVENTORY.md` | capability and end-to-end evidence |
| New physical tables | 12 | `04-ARSITEKTUR-MODUL-JURNAL.md` | schema diff tepat 12; tidak ada tabel redundan |

Jika requirement baru ditemukan, ia ditambahkan ke register tanpa mengurangi atau menomori ulang evidence baseline. Perubahan scope membutuhkan architecture decision record, dampak security/data, dan approval sebelum implementasi.

## Kontrak environment SIT dan UAT

Instance PostgreSQL yang disediakan berada pada `localhost:5432`, dengan baseline main `ais` dan baseline BLOB `streaming_ais`, user `root`. Keduanya diperlakukan read-only selama implementasi. Password adalah secret eksternal dan **tidak boleh** ditulis pada Markdown, source code, XML Hibernate, log, screenshot, command history, atau test report.

Konfigurasi aplikasi/test memakai secret injection dengan nama semantik berikut:

```text
AIS_JURNAL_DB_HOST=localhost
AIS_JURNAL_DB_PORT=5432
AIS_JURNAL_DB_NAME=<environment database>
AIS_JURNAL_DB_USER=root
AIS_JURNAL_DB_PASSWORD=<external secret>
AIS_JURNAL_PUBLIC_BASE_URL=https://journal.example.ac.id/ais
AIS_JURNAL_STREAMING_DB_HOST=localhost
AIS_JURNAL_STREAMING_DB_PORT=5432
AIS_JURNAL_STREAMING_DB_NAME=<environment streaming database>
AIS_JURNAL_STREAMING_DB_USER=root
AIS_JURNAL_STREAMING_DB_PASSWORD=<external secret>
AIS_JURNAL_CROSSREF_XSD=https://www.crossref.org/schemas/crossref5.3.1.xsd
AIS_JURNAL_DATACITE_XSD=https://schema.datacite.org/meta/kernel-4.5/metadata.xsd
AIS_JURNAL_DOAJ_XSD=https://doaj.org/static/doaj/doajArticles.xsd
AIS_JURNAL_PUBMED_DTD=https://dtd.nlm.nih.gov/ncbi/pubmed/in/PubMed.dtd
```

Sebelum mutation pertama:

1. lakukan fingerprint read-only terhadap server/database/schema/version/extension/table counts;
2. buat backup format custom dan verifikasi checksum;
3. lakukan restore drill dan buktikan aplikasi dapat bootstrap dari hasil restore;
4. gunakan clone terpisah pada instance yang sama: `ais_jurnal_sit`/`ais_jurnal_uat` dan `streaming_ais_jurnal_sit`/`streaming_ais_jurnal_uat`;
5. database `ais` dan `streaming_ais` tidak menjadi tempat destructive/schema/performance test kecuali ada backup teruji, maintenance window, dan persetujuan cutover eksplisit;
6. gunakan tenant/journal/user/file-root/email/payment/DOI endpoint terpisah antara SIT dan UAT;
7. email memakai sandbox/capture, payment memakai sandbox, DOI/deposit memakai test endpoint; tidak boleh mengirim efek eksternal riil.

SIT boleh di-refresh berulang. UAT hanya di-refresh melalui change record agar evidence dan sign-off tetap dapat direproduksi. Data sensitif hasil clone harus dibatasi, dimasking bila diperlukan, serta mengikuti retention policy.

Saat environment jurnal aktif, bootstrap Hibernate wajib memakai `hibernate.hbm2ddl.auto=none`; perubahan schema hanya melalui migration SQL yang direview, kemudian divalidasi oleh gate main/streaming yang eksplisit. Validasi global SessionFactory legacy tidak dipakai karena factory tersebut juga memetakan modul non-jurnal yang berada di luar scope. Konfigurasi deployment legacy di luar mode jurnal tidak diubah oleh kontrak ini.

## Fase 0 — Governance, scope lock, dan traceability

Tujuan: mengubah seluruh dokumen discovery menjadi backlog yang tidak kehilangan fitur.

Deliverable:

- feature-traceability register untuk 134/905, 45 plugin, 73 email, 28 submenu, RBAC operations, public/API surfaces, workflows dan migration cases;
- glossary canonical untuk journal, collection, item, publication version, issue, stage, round, assignment, galley dan projection;
- architecture decision records untuk existing-first, 12-table budget, JSON contracts, streaming BLOB, authorization dan importer;
- dependency graph, risk register, owner teknis/fungsional, definition of ready/done, defect severity dan change-control rule.

Exit gate: seluruh baseline mempunyai requirement ID dan planned test ID; tidak ada `TBD`, `UNKNOWN`, atau owner kosong untuk fitur release.

## Fase 1 — Environment, backup, dan physical-schema verification

Tujuan: memastikan implementation target dan recovery benar-benar tersedia.

Deliverable:

- konektivitas read-only ke baseline `ais` tanpa mencetak credential;
- inventory PostgreSQL: database/schema/table/column/index/FK/sequence/extension/size dan migration state;
- verifikasi fisik seluruh `Repo*`, `Diskusi`, `LampiranJurnal`, `Tbmrole`, `Menu`, `job_has_menu`, notification, payment, institution dan publication projection;
- backup, checksum, restore drill, clone SIT/UAT, isolated file root dan external-integration sandbox;
- baseline compile/test evidence dan snapshot performance sebelum perubahan.

Exit gate: restore drill lulus, clone bootstrap lulus, setiap entity reuse mempunyai bukti tabel/kolom fisik, dan tidak ada migration yang dijalankan pada baseline tanpa rollback plan.

## Fase 2 — Schema contract dan migration foundation

Tujuan: menyediakan perubahan kompatibel dan tepat 12 tabel baru.

Deliverable:

- compatible nullable/versioned extensions untuk Repository, `Diskusi`, `Tbmrole.jurnalAksesJson`, notification/audit dan projection yang telah disetujui;
- enam tabel domain: `TemplateEmailJurnal`, `LanggananJurnal`, `UndanganPeranJurnal`, `PesertaDiskusiJurnal`, `PenugasanTahapJurnal`, `PenugasanReviewerJurnal`;
- dua tabel support: `AgregatPenggunaanJurnal`, `RentangIpLanggananJurnal`;
- empat tabel importer: `ImportSumberOjs`, `ImportJobOjs`, `ImportCheckpointOjs`, `ImportMappingOjs`;
- index/unique/FK/check constraints, Envers strategy, migration version, forward/rollback SQL dan schema-diff test;
- typed/versioned schema untuk `accessPolicyJson`, `workflowProfileJson`, assignment response/settings dan import reports.

Exit gate: empty-db dan upgraded-clone migration lulus; schema diff tepat 12 tabel; rollback/restore lulus; tidak terbentuk `TipeLanggananJurnal`, tabel round/form/element/answer, atau master paralel lainnya.

## Fase 3 — Shared kernel, routing, menu, dan authorization

Tujuan: membuat satu jalur keamanan untuk semua fitur berikutnya.

Deliverable:

- `JurnalAksesKatalog`, parser/normalizer versioned `jurnalAksesJson`, fail-closed evaluator dan permission audit;
- tab Hak Akses Jurnal pada `TbmroleAction`, unknown-key round-trip tanpa grant, optimistic conflict dan **Semua** per-row;
- reconciler hierarchy `46/4605/460501..460528`, `job_has_menu`, `NewUiRouteRegistry`, canonical `WEB-INF/baru/modul/jurnal/*` dan redirect alias;
- object policy yang mengiris active role, module gate, capability, tenant/journal, ownership/assignment, workflow state, anonymity dan conflict-of-interest;
- CSRF/error/validation/audit/idempotency primitives yang dipakai semua command.

Exit gate: 28 menu mempunyai positive/negative route and authorization test; `RolePrivilage` tidak menjadi fallback CRUD; cross-journal/cross-tenant IDOR suite lulus.

## Fase 4 — Journal administration dan configuration

Tujuan: menyediakan master journal tanpa membuat master kedua.

Deliverable:

- CRUD/configuration `JurnalPenelitian` dan linked `RepoCollection(type=JOURNAL)`;
- locale, identity/contact, slug/domain, policies, appearance slots, sections/categories/vocabulary/navigation/static pages/highlights;
- versioned metadata/workflow/access profiles dengan validator, limits, checksum dan audit;
- submission checklist, author guidelines, review policy, privacy/copyright/license dan publication schedule;
- journal isolation, clone/config export-import dan preview.

Exit gate: multi-journal configuration dan isolation test lulus; profile version rollback/preview lulus; tidak ada tabel section/category/vocabulary/navigation/plugin paralel.

## Fase 5 — Identity, roles, contributors, dan invitations

Tujuan: menyatukan actor internal dan eksternal tanpa menduplikasi account/role global.

Deliverable:

- safe link ke `Tbmuser`, external authority/profile, verified-email/provenance collision flow;
- ORCID/ROR/affiliation/CRediT, ordered/corresponding contributors dan published `AnggotaArtikel` projection;
- `UndanganPeranJurnal`: hashed one-time token, expiry, resend, revoke, accept/decline, idempotency dan audit;
- scoped `PenugasanTahapJurnal` untuk journal/section/submission/stage dengan start/end dan provenance;
- reviewer pool, expertise/interests, availability, conflict declaration dan admin-reviewed OJS group mapping.

Exit gate: internal/external identity collision tests, invitation security tests, expired/revoked scope tests dan no-global-role-duplication tests lulus.

## Fase 6 — Author submission dan pre-review workflow

Tujuan: menyediakan submission lengkap dan resumable.

Deliverable:

- wizard draft/resume untuk checklist, consent, locale, metadata, contributors, references dan files;
- `RepoItem(JOURNAL_SUBMISSION)` root/version chain, autosave, optimistic locking, validation dan submit confirmation;
- file stage/genre/revision metadata pada `RepoBitstream`, streaming bytes pada `LampiranJurnal`, checksum/MIME/quota/AV hook;
- author dashboard, status/timeline, withdrawal request, incomplete submission cleanup dan notification;
- author/editor preview serta safe metadata projection.

Exit gate: happy-path, interrupted resume, concurrent edit, invalid file, external co-author, duplicate identifier dan cross-journal negative tests lulus.

## Fase 7 — Editorial, review, discussion, dan decision workflow

Tujuan: menyediakan single/double/open review dan editorial control lengkap.

Deliverable:

- editor queues, filters, deadline/SLA, scoped assignment dan `Diskusi`/`DiskusiKomentar` plus indexed participants;
- `PenugasanReviewerJurnal`: suggested/invited/accepted/declined/overdue/completed/cancelled/reinstated, `roundNumber`, anonymity, due dates, recommendation dan response snapshot;
- round lifecycle sebagai append-only `RepoWorkflowEvent`; round consistency pada assignment/files;
- append-only versioned review forms di `RepoCollection.workflowProfileJson`, typed answers/checksum pada assignment;
- reviewer files/comments, author response/revision, reminder/escalation, conflict-of-interest, blinded DTO/file/log/email;
- editorial decisions: decline, resubmit, revision, accept, cancel/reinstate, with immutable history.

Exit gate: multi-reviewer/multi-round, all anonymity modes, conflict, overdue/reminder, cancellation/reinstatement, form-version and immutable-response suites lulus. Jika analytics per-element diperlukan, buktikan dahulu bahwa derived projection tidak cukup sebelum mengusulkan tabel baru.

## Fase 8 — Copyediting, production, proof, dan file lifecycle

Tujuan: melanjutkan accepted submission sampai publication-ready artifacts.

Deliverable:

- copyeditor/production/proofreader assignments, consultation/discussion dan deadlines;
- copyedit revisions, author approval, production-ready files, galley metadata dan proof approval;
- PDF/HTML/JATS/XML/supplementary galley handling, safe HTML sanitization dan accessible viewer;
- storage state machine `PENDING_CONTENT -> CONTENT_VERIFIED -> LINKED/FAILED`, compensation, retry dan orphan reconciliation;
- version/file immutability, replacement audit, download authorization dan watermark/disposition policy bila diperlukan.

Exit gate: large-file bounded streaming, checksum corruption, retry/orphan, unauthorized/blinded file, proof approval dan accessibility tests lulus.

## Fase 9 — Issue management, publication, correction, dan retraction

Tujuan: membuat publication lifecycle lengkap tanpa writer kedua.

Deliverable:

- `RepoItem(JOURNAL_ISSUE)`, volume/number/year/title/cover metadata dan TOC ordering melalui `RepoItemRelation`;
- scheduling, publish/unpublish rules, immutable publication version, article/issue DOI readiness;
- `Artikel`, `FileArtikel`, `AnggotaArtikel` published compatibility projection yang idempotent;
- correction/new version, withdrawal/retraction, reason/history, OAI tombstone dan identifier continuity;
- issue/article access policy, embargo dan notification hooks.

Exit gate: schedule/publish, rollback-before-publication, projection idempotency, correction/retraction, DOI/OAI continuity dan non-journal Repository regression tests lulus.

## Fase 10 — Public portal, discovery, accessibility, dan API

Tujuan: menyediakan seluruh reader-facing surface.

Deliverable:

- journal home/current/archive/issue/article/galley/about/contact/editorial team/policies/announcements/static pages;
- browse, filter, search, pagination, citation download, recommendations dan submission CTA;
- canonical URL, metadata tags, OpenGraph, Scholar/DC, robots, sitemap, RSS/Atom dan accessible markup/viewer;
- OAI-PMH verbs, sets, resumption token service dan required metadata formats;
- public/protected API with pagination, validation, rate/abuse controls and consistent error contract;
- responsive keyboard/mobile/WCAG behavior.

Exit gate: route crawl has no broken/duplicate canonical route; SEO/feed/OAI validators, accessibility smoke and anonymous/protected negative tests lulus.

## Fase 11 — Email, notification, preference, dan announcements

Tujuan: menutup seluruh komunikasi workflow.

Deliverable:

- 73 original AIS Indonesian/English template definitions, stable keys, variable allowlist, subject/body sanitization dan preview;
- journal-scoped `TemplateEmailJurnal`, immutable sent snapshot pada `Notifikasi`, recipient/CC/BCC/result/idempotency/correlation evidence;
- in-app/read state, email/WA routing bila diizinkan, preference/unsubscribe dan digest/reminder scheduler;
- existing `PengumumanPenelitian` path dengan journal scope, locale, category, attachment, publish window dan feeds;
- sandbox/capture configuration pada SIT/UAT.

Exit gate: 73/73 template coverage, missing/unknown variable rejection, duplicate-send prevention, preference/unsubscribe, announcement/feed dan no-real-recipient SIT tests lulus.

## Fase 12 — Access, subscription, institution, IP, dan payment

Tujuan: mendukung open, embargo, individual dan institutional access.

Deliverable:

- versioned subscription catalog pada `RepoCollection.accessPolicyJson`;
- `LanggananJurnal` dengan policy key/snapshot, user/institution, period/status/reference, renewal/expiry and payment linkage;
- existing `PerguruanTinggi`/`PerguruanTinggiLain`, domain dan `RentangIpLanggananJurnal` untuk IPv4/IPv6 normalized range;
- access evaluator untuk open/subscribed/embargo/institution IP/admin override dengan audit-safe reason;
- manual/provider payment initiation, callback/idempotency/signature, settlement/reconciliation/refund/cancellation pada AIS payment rails;
- entitlement cache invalidation dan expiry reminders.

Exit gate: allow/deny boundary, overlapping/malformed IP, expired/cancelled access, policy snapshot immutability, duplicate callback dan sandbox reconciliation tests lulus.

## Fase 13 — Identifier, metadata, integrations, dan 45-plugin parity

Tujuan: menyediakan perilaku native untuk seluruh plugin bawaan yang in-scope.

Deliverable:

- DOI/URN assignment, validate, resolve, deposit/retry/audit; Crossref/DataCite test endpoints;
- DOAJ/PubMed/native XML/user import-export, DC/MARC/MARCXML/JATS/RFC1807/OAI formats;
- ORCID/ROR/CRediT, citation styles, JATS generator/reader, Scholar/DC metadata;
- safe HTML/PDF viewer, feeds, recommendations, configurable blocks, consent-aware analytics, integrity facts;
- report and payment/theme mappings serta documented proof untuk N/A plugin;
- integration attempts pada `RepoIntegrationEvent`, permission keys dan operational retry UI.

Exit gate: semua 45 baris plugin memiliki implementation/test/proof evidence; test deposits tidak mencapai production; export round-trip dan retry/idempotency tests lulus.

## Fase 14 — Search, statistics, COUNTER, dan reports

Tujuan: menyediakan discovery internal dan pelaporan operasional/strategis.

Deliverable:

- search projection rebuild dari Repository metadata, locale-aware filters/sort dan authorization-safe indexing;
- normalized `RepoUsageEvent`, bot/privacy/IP retention controls dan one dimensioned `AgregatPenggunaanJurnal`;
- daily/monthly/context/geo/COUNTER aggregation, late-event handling dan reprocessing;
- article, review performance, subscription, payment, editorial SLA dan reconciliation reports;
- paged/streaming export with capability checks and formula-injection protection.

Exit gate: source-to-aggregate reconciliation, privacy retention, COUNTER fixtures, search rebuild/no-leak and large report bounded-memory tests lulus.

## Fase 15 — Unified importer legacy dan OJS 3.5.0-5

Tujuan: mengganti importer lama dengan jalur version-aware dan dapat dipulihkan.

Deliverable:

- external JDBC read-only source configuration, dialect/schema signature and preflight;
- legacy and OJS 3.5 readers, dependency-ordered transforms untuk seluruh 134/905 mapping;
- dry-run, collision/manual-decision workflow, multi-source provenance, batch/checkpoint/resume/cancel;
- secure file manifest/path/archive handling dan streaming reconciliation;
- role mapping approval, 73 template key mapping, plugin/config allowlist dan 13 N/A runtime/staging proof;
- final report: source/accepted/linked/created/merged/skipped/failed totals, relationship/file/checksum evidence dan blocker list;
- compatibility redirect dari importer lama tanpa writer kedua.

Exit gate: legacy and OJS 3.5 fixtures lulus dry-run/execute/resume/re-run; 134/905 reconciliation lengkap; collision fixtures lulus; zero unexplained data loss dan zero final blocker.

## Fase 16 — Hardening, operations, dan release candidate

Tujuan: menghasilkan build yang dapat dioperasikan dan diuji end-to-end.

Deliverable:

- unit/service/database/UI/API/end-to-end regression suites;
- CSRF/XSS/SQL injection/IDOR/SSRF/upload/archive/token/blinding/JSON abuse/security tests;
- dependency/license/SBOM/secrets review dan clean-room evidence;
- performance/load/soak dataset, p50/p95/throughput/heap/query measurements dan agreed thresholds;
- scheduler/job locking, retry/dead-letter/alerting, metrics/logging/correlation, backup/restore, file reconciliation dan runbooks;
- release notes, migration/rollback package dan immutable RC artifact.

Exit gate: zero open Severity 1/2, accepted Severity 3 plan, security blockers zero, restore/rollback drill lulus, performance thresholds disetujui dan RC reproducible.

Status evidence 2026-08-23: gate belum lulus. OWASP Dependency-Check 13.0.0 dengan NVD 2.0/CISA KEV menginventarisasi 184 JAR fisik menjadi 1.762 dependency/komponen tertanam dan menemukan 302 catatan CVE pada 72 komponen (53 critical, 127 high, 116 medium, 4 low). Mayoritas temuan awal berada pada pustaka legacy/bundled AIS; laporan JSON wajib ditriage untuk reachability, false positive, ownership, compatible upgrade dan retest sebelum label RC. Laporan berada di `build/maven/dependency-check-report.json`, SHA-256 `465E84CB378FA1C790A183BBB592D133ACCFCF236F066AF1F837DEAE88C33716`.

Addendum komputer lanjutan: clean compile/package serta 28/28 self-test setelah upgrade jsoup lulus; hanya JAR jsoup 1.23.1 yang memiliki class `org/jsoup/*`. Namun laporan Dependency-Check tersebut tidak ikut tersalin ke komputer ini dan belum ada retest/triage seluruh 184 JAR. Karena zero-open Severity 1/2, SBOM/license, performance/load/soak, accessibility, dan security regression penuh belum dibuktikan, status Fase 16 tetap **BLOCKED**.

Addendum current checkout 05:48 WIB: performance tidak lagi `NOT_RUN`, tetapi masih **PARTIAL**. Characterization read-only pada 50.000 artikel/180 operasi mencatat 3 query per loop, p50 31,30 ms, p95 78,44 ms, 22,42 operasi/detik, dan heap used 1.509.496.960 byte. Dataset acceptance 100k submission/1m file/10m usage/10k user, soak, peak heap, serta threshold owner belum tersedia; karena itu exit gate tetap tidak lulus. Reconciler menu juga lulus pada clone SIT (29 canonical/0 collision/idempoten), tetapi production preflight tetap memerlukan approval terpisah.

Addendum full-scale 2026-08-23: `PASS_LOCAL_SIT_V1` pada schema dedicated clone membuktikan 100 jurnal/100k artikel/1m metadata file/10m usage/10k user. Soak 8 thread × 300 detik menghasilkan 1.608.291 operasi, warm p95 2,23 ms, analytic p95 1.652,22 ms, load p95 7,18 ms, throughput 5.344,88 ops/s, error 0, heap peak 203.459.480 byte; seluruh threshold lokal v1 lulus. Gate Fase 16 tetap BLOCKED karena dependency/security/accessibility serta production-like HTTP/WAL, long soak, dan SLA owner belum ditutup.

## Fase 17 — SIT

SIT dijalankan pada clone `ais_jurnal_sit` atau environment setara di `localhost:5432`, bukan langsung pada baseline tanpa restore point.

Minimum scenario:

1. multi-journal administration, locale, menu/RBAC dan cross-tenant negatives;
2. lecturer/student/external author submission, resume, contributors and files;
3. editor assignment, double-anonymous multi-reviewer, multiple rounds, reminders and decisions;
4. copyedit/production/proof, issue schedule, DOI, publish, projection and notifications;
5. public portal/search/citation/galley/feed/OAI/accessibility;
6. individual/institution subscription, IP allow/deny and payment sandbox reconciliation;
7. correction/retraction/new version/OAI tombstone;
8. 45 plugin parity suites and 73 email coverage;
9. legacy and OJS 3.5 import dry-run/execute/resume/re-run/reconcile;
10. backup/restore, job retry, integration outage, file orphan/corruption and rollback drills;
11. security negative and performance/load/soak suites;
12. regression seluruh modul AIS existing yang disentuh.

Exit gate: planned SIT cases 100% executed; zero Severity 1/2; zero data-reconciliation blocker; performance/security/restore gates pass; signed SIT report links every failed/fixed/retested defect.

Status komputer lanjutan: **TECHNICAL_PARTIAL_PASS**. Migration/verifier dan reconciler menu idempoten, 35 self-test, OJS 134/905+legacy 7/37, retry file ke `LampiranJurnal`, BLOB/file lifecycle, health, demo 500×100, serta performance characterization awal lulus pada clone/fixture. External sandbox, browser/accessibility, full target load/soak, full security/regression, 100% execution matrix, dan signed SIT report belum terpenuhi.

## Fase 18 — UAT

UAT memakai clone/stable database `ais_jurnal_uat`, build RC yang sama dengan SIT, sanitized representative data dan external sandboxes.

Actor minimum: site admin, journal manager, editor, section editor, reviewer, author internal, student author, external co-author, copyeditor, production/proofreader, librarian/subscription officer, finance officer dan anonymous reader.

Business journey minimum:

- create/configure journal and access role;
- submit-to-publish full journey including exception/revision paths;
- reviewer anonymity/conflict/deadline/reminder experience;
- issue/public portal/discovery/accessibility experience;
- subscription/payment/institution access and reports;
- correction/retraction and audit history;
- import preview/approval/reconciliation;
- operational monitoring, retry, backup and restore procedures.

Exit gate: business owner signs each journey and 12-area coverage; zero Severity 1/2; accepted residual risk and training/support material; formal go/no-go approval.

Status komputer lanjutan: **TECHNICAL_SMOKE_PASS / FORMAL_NOT_RUN**. Streaming rollback, file end-to-end+cleanup, serta health/readiness lulus pada clone UAT dengan output clean build yang sama. Actor journey, owner sign-off, residual-risk acceptance, training/support, dan go/no-go formal tetap memerlukan manusia berwenang.

## Fase 19 — Pilot, cutover, dan stabilization

Tujuan: merilis secara recoverable tanpa mematikan compatibility terlalu dini.

Deliverable:

- final backup/restore verification, migration preflight, file manifest and source freeze/read-only window;
- canary/pilot journal, feature flags, smoke tests and reconciliation before wider enablement;
- cutover runbook with owner/timestamp/command/evidence, rollback triggers and maximum rollback window;
- monitoring dashboard, alert routing, support triage, user training and incident communication;
- post-cutover reconciliation, performance/security observation and daily defect review;
- decommission `OjsHibernateUtil`/legacy importer only after compatibility period and explicit evidence that no caller remains.

Exit gate: pilot and full cutover reconciled, no Severity 1/2 during stabilization window, rollback window closed by approval, operational handover signed, and traceability register shows 100% released or explicitly deferred with approved rationale.

Status komputer lanjutan: **NOT_RUN / NOT_AUTHORIZED**. Entry gate Fase 16–18 belum lulus dan belum tersedia endpoint pilot, owner, freeze/read-only window, alert routing, rollback approval, maupun otorisasi cutover.

## Aturan tidak boleh terlewat

- Fase berikutnya boleh dikerjakan paralel hanya jika dependency dan entry gate-nya sudah terpenuhi; exit gate tidak boleh dihapus.
- Setiap PR/change harus menyebut requirement IDs, permission keys, migration impact dan test IDs.
- Status plugin/email/menu/table/field tidak boleh dihitung dari jumlah file; harus berdasarkan passing evidence.
- Tidak ada external side effect pada SIT/UAT selain sandbox/capture.
- Tidak ada password atau token pada repository/log/report.
- Tidak ada schema auto-update tanpa reviewed migration dan schema diff.
- Tidak ada direct DAO/route yang melewati object authorization service.
- Tidak ada source OJS PHP/job/filter/session payload yang dieksekusi.
- Tidak ada data import dianggap selesai sebelum reconciliation dan file checksum selesai.
- Tidak ada go-live sebelum backup restore dan rollback drill berhasil.

