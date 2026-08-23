# Perintah master Codex/Claude — Jurnal Terpadu AIS setara OJS 3.5.0-5

## Mandat

Kerjakan pada checkout lokal:

`C:\opt\AIS\ais\src\main`

Bangun/enhance modul Penelitian dan Pengabdian—terutama `penelitiandanpengabdian.artikel`—menjadi aplikasi pengelolaan jurnal ilmiah native eCampus dengan paritas kemampuan OJS 3.5.0-5, terintegrasi penuh dengan identitas, role, mahasiswa/siswa/dosen/guru, penelitian, repository, notifikasi, pembayaran, audit, dan laporan AIS.

Jangan memasang OJS kedua, jangan iframe, dan jangan menyalin source PHP/template/CSS/JS/ikon/terjemahan/aset OJS. Implementasikan perilaku secara clean-room dalam Java/JSP/Hibernate AIS.

## Sumber dan prioritas kebenaran

Urutan authoritative:

1. instruksi pengguna terbaru dalam dokumen ini;
2. source dan schema checkout SVN lokal yang sedang digunakan;
3. dokumen `docs\jurnal\*.md` setelah direkonsiliasi;
4. GitHub `Zishof/AIS` commit `2f12a5e` sebagai pembanding publik;
5. source resmi OJS tag `3_5_0-5` dan pkp-lib yang dipin.

Jika source lokal berbeda dari GitHub, jangan menimpa perubahan lokal. Catat perbedaan dan gunakan source lokal sebagai implementasi aktual. Klaim evidence lama menjadi `UNVERIFIED_ON_CURRENT_CHECKOUT` sampai test diulang.

## Aturan keselamatan kerja

Sebelum mengubah file:

1. jalankan `git status --short`, `git branch --show-current`, `git rev-parse HEAD`, `svn info`, dan `svn status` bila working copy SVN tersedia;
2. buat manifest path, size, mtime, dan SHA-256 seluruh file relevan;
3. jangan `reset`, `clean`, `checkout --`, rebase, pull, revert, atau overwrite perubahan pengguna;
4. lakukan diff tiga arah untuk file yang sudah modified;
5. buat branch/patch checkpoint yang recoverable;
6. hanya ubah file yang diperlukan dan pada akhir pekerjaan tampilkan daftar **hanya file yang berubah**.

## Preflight wajib

Audit aktual, jangan percaya nama class dari dokumen tanpa membuka source:

- POM/Ant, compiler source/target, JDK deploy, Tomcat, ZK, Hibernate, PostgreSQL;
- `Artikel`, `JurnalPenelitian`, `FileArtikel`, `AnggotaArtikel`, `ArtikelTerindeks`;
- seluruh package `ais.database.model.repository`, `file`, `ojs`, payment, notification, institution, discussion;
- `JurnalPenelitianAction`, `OjsHibernateUtil`, `StreamingHibernateUtil`, servlet OAI, Pustaka, security filter;
- `hibernate.cfg.xml`, `hibernate.streaming.cfg.xml`, `hibernate.ojs.cfg.xml`, `web.xml`;
- `Menu`, `MenuInitializer`, `job_has_menu`, `RolePrivilage`, `Tbmrole`, `TbmroleAction`, `NewUiRouteRegistry`;
- semua JSP/ZUL Penelitian dan Pengabdian, Pustaka, Repository, New UI, CSS responsive/theme;
- physical schema/index/FK/Envers pada clone database;
- seluruh `docs\jurnal\*.md` dan script generator.

Hasilkan `CURRENT-CHECKOUT-MANIFEST.md`, `CURRENT-SCHEMA-FINGERPRINT.md`, dan `GAP-REGISTER.md`. Berhenti sebelum mutation bila backup/restore clone, credential isolation, atau collision menu belum aman.

## Keputusan arsitektur final

### Domain utama

- `JurnalPenelitian` adalah master jurnal canonical; jangan membuat master `Jurnal` kedua.
- `Artikel` tetap proyeksi kompatibilitas published untuk Penelitian/Pengabdian, BKD, akreditasi, dan laporan. Jangan menjadikannya satu-satunya penyimpan workflow OJS.
- Gunakan Repository AIS untuk collection/item/metadata/relation/contributor/workflow/publication bila physical schema dan invariant terbukti cocok.
- Jangan memakai `articleId` OJS sebagai identity global. Gunakan provenance `(sourceInstanceUuid, sourceTable, sourcePrimaryKey, sourceRevision)`.
- Hilangkan side effect ID random dari jalur import; jangan memanggil getter domain yang mengubah state untuk melakukan reconciliation.

### File jurnal

Instruksi terbaru mewajibkan:

- buat `ais.database.model.file.LampiranJurnal` mengikuti pola streaming aman `LampiranLain`;
- register hanya pada `hibernate.streaming.cfg.xml`;
- simpan bytes OJS dan upload jurnal pada tabel streaming `lampiran_jurnal`;
- metadata/version/galley tetap berada di main domain, dengan scalar `repoBitstreamId`/reference;
- tidak ada `@ManyToOne` lintas SessionFactory;
- wajib ada checksum SHA-256, declared/actual size, MIME sniff result, original safe filename, stage/version, storage state, created/updated audit, quarantine/scan state, dan unique/idempotency key;
- download wajib authorization, range/stream bound, `nosniff`, disposition aman, CSP yang sesuai, dan blindness redaction.

Target schema: **12 tabel jurnal pada main database + 1 `lampiran_jurnal` pada streaming database**. Revisi semua dokumen lama yang menyatakan tidak ada `LampiranJurnal`.

### Hibernate/schema

- Main metadata/domain hanya melalui `HibernateUtil` dan `hibernate.cfg.xml`.
- `LampiranJurnal` hanya melalui `StreamingHibernateUtil` dan `hibernate.streaming.cfg.xml`.
- OJS source dibaca JDBC read-only; importer baru tidak bergantung pada `OjsHibernateUtil`.
- Entity annotations/mapping adalah sumber schema canonical sesuai permintaan pengguna.
- Jalankan Hibernate schema update hanya pada clone backup/SIT; capture dan review DDL sebelum production.
- Jangan menaruh ALTER ad-hoc di servlet/action dan jangan menjalankan auto-DDL production tanpa change window/approval.

### Transaction boundary

Main DB dan streaming DB tidak atomik. Terapkan state machine `PENDING_CONTENT → CONTENT_STORED → VERIFIED → LINKED → AVAILABLE`, compensation, retry, orphan reconciliation, dan idempotency. Kegagalan tidak boleh menghasilkan metadata published dengan content hilang.

## Servlet, route, dan view

- Buat `ais.action.servlet.Jurnal.java`.
- Map canonical `/jurnal` dan `/jurnal/*` pada `web.xml`; URL yang dirender memakai `Common.ROOT + "/jurnal"`.
- Public dan seluruh role workspace berada di:
  `C:\opt\AIS\ais\src\main\webapp\WEB-INF\baru\modul\jurnal\*`.
- Boleh memakai header/footer/theme Pustaka, tetapi jangan memakai dynamic JSP include berbasis parameter tanpa route allowlist.
- Legacy ZUL/JSP hanya menjadi redirect/compatibility alias; tidak boleh menjadi authorization path kedua.
- Pisahkan JSP presentation dari Java service. Tidak ada HQL/SQL, mutation, secret, atau authorization decision di JSP.

## UI/UX wajib

Implementasikan seluruh kontrak `13-SPESIFIKASI-UIUX-JURNAL-MODERN.md`.

Minimum yang harus ada:

- public portal modern, responsive, SEO-ready;
- dashboard per role;
- resumable submission wizard;
- editorial queue/workspace dengan stage rail, SLA, timeline, bulk action aman;
- blind reviewer workspace dan structured form;
- copyediting/production/proof workspace;
- issue/TOC builder dan publication readiness;
- importer wizard dengan preflight, dry-run, progress, konflik, resume, reconciliation;
- statistics/report dashboard;
- loading, empty, no-access, validation, conflict, partial failure, retry, success, dan destructive states;
- responsive 390/768/1024/1440+, WCAG 2.2 AA, keyboard, screen reader, 200% zoom, reduced motion;
- visual regression screenshot matrix.

Jangan memakai scaffold `finance` atau `DynamicJspCrudGenerator` sebagai hasil akhir.

## Menu admin

Buat parent `root=46, child=4605` bernama **Jurnal**, lalu tepat 28 children `root=4605, child=460501..460528` sesuai tabel canonical di dokumen UI/UX. `Menu.id` adalah PK terpisah.

Sebelum seed, cek collision `id/root/child/url/label`. Seed/reconciler harus idempoten dan tidak mengubah menu pengguna yang tidak terkait.

Admin memperoleh CRUD/configuration/workflow sesuai capability. Author, mahasiswa, siswa, dosen, guru, reviewer, editor, copyeditor, proofreader, production editor, reader, dan subscription manager memakai workspace `/jurnal/*`; mereka tidak otomatis memperoleh 28 menu admin.

## RBAC dan object authorization

- Entry module: active role + `job_has_menu`.
- Capability: versioned `Tbmrole.jurnalAksesJson` melalui satu `JurnalAksesKatalog` fail-closed.
- Scope: tenant → journal → section → submission → stage → operation assignment.
- Effective access adalah intersection, bukan union semua stored role.
- `RolePrivilage` tidak menjadi fallback CRUD jurnal.
- Tambahkan tab **Hak Akses Pengelolaan Jurnal** setelah Hak Akses Pedagang di `TbmroleAction`.
- Test null, malformed, oversized, unknown version/key, non-boolean, stale role, cross-journal, cross-tenant, IDOR, COI, anonymity, and break-glass audit.

## Cakupan fitur OJS wajib

Jangan berhenti pada artikel published. Implementasikan dan trace seluruh kemampuan berikut:

1. multi-journal/site configuration, locale, appearance, policy;
2. registration, login/linking AIS identity, invitation, profile, interest;
3. submission wizard, checklist, consent, metadata, contributor, affiliation, CRediT, references, files;
4. screening, editor/section editor assignment, queue, discussion, deadline/SLA;
5. single/double/open anonymous review, multi-reviewer, multi-round, conflict, form, reminder, recommendation;
6. editorial decisions lengkap: revision, resubmit, accept, decline, revert, cancel/reinstate;
7. copyediting, production, proof, consultation, revisions;
8. issue, section, category, TOC/order, schedule;
9. publication version, galley, publish/unpublish, correction, retraction, withdrawal, tombstone;
10. public portal, browse, search, archive, related/recommendation, citation download;
11. DOI, URN, ORCID, ROR, OAI-PMH, feeds, sitemap, Scholar/DC/OG metadata;
12. Crossref, DataCite, DOAJ, PubMed, JATS, native XML import/export;
13. notification preference, 73 bilingual email keys, digest, immutable send snapshot;
14. open access, embargo, subscription individual/institution, IPv4/IPv6, payment/reconciliation;
15. usage privacy, statistics, COUNTER, article/reviewer/subscription reports;
16. announcements, highlights, static pages, navigation/custom blocks;
17. seluruh 45 plugin bundled: native equivalent, mapped existing, atau N/A dengan bukti;
18. audit, jobs, retry, health, operational observability;
19. import OJS legacy dan 3.5.0-5 termasuk seluruh file.

Setiap capability harus mempunyai requirement ID, target service/entity/route, permission, positive/negative test, dan evidence. “Ada halaman” bukan bukti parity.

## Mapping 134 tabel dan 905 field

Pertahankan coverage 134/134 dan 905/905, tetapi tingkatkan generator/catalog agar setiap field memuat:

- source table/column/type/null/default/constraint/semantic;
- target entity/table/field atau provenance/exclusion yang eksplisit;
- converter/locale/timezone/enum/HTML policy;
- identity/FK/order/cardinality;
- collision and merge rule;
- import phase/batch/query owner;
- test ID, row/relationship reconciliation, and sample evidence.

Mapping generik `Target.camelCase` tidak dianggap selesai. Parser schema harus mempunyai golden snapshot dan fail bila source OJS berubah.

## Importer OJS

Sediakan adapter version-aware:

- legacy AIS-supported schema;
- OJS 3.5.0-5 exact signature;
- preflight fail-closed untuk versi/signature lain.

Pipeline:

1. named connection reference dari environment/server config;
2. JDBC read-only, parameter binding, fetch size, pagination, timeout, cancellation;
3. schema/file manifest dan source instance UUID;
4. dry-run tanpa target mutation;
5. identity/role/journal mapping review;
6. staged batch dengan checkpoint dan resume;
7. file canonical-path validation, symlink/NUL/traversal/archive bomb protection;
8. stream ke `LampiranJurnal`, read-back checksum, metadata link;
9. collision wizard link/merge/external/skip;
10. reconciliation 134 tables/905 fields/rows/FK/files/checksum;
11. final report, run ID, checksum, unresolved item list;
12. optional delta import/cutover with source freeze rules.

Jangan menerima password DB atau arbitrary server path dari browser. Jangan log metadata sensitif, SQL berisi value, token, reviewer identity, atau credentials.

## Keamanan minimum

- server-side authorization pada setiap read/mutation/download/export;
- CSRF untuk cookie-auth mutation; SameSite/HttpOnly/Secure cookie;
- XSS-safe output, HTML sanitizer allowlist, CSP nonce/sandbox;
- upload size/quota/MIME sniff/extension policy/AV hook/quarantine;
- XXE disabled; ZIP bomb/traversal/symlink protection;
- SSRF allowlist dan private-IP/redirect/DNS rebinding defense;
- HMAC/callback constant-time verification, replay window, idempotency;
- rate limiting/abuse protection untuk login, search, download, invitation, OAI/API;
- reviewer blindness pada DTO, DOM, URL, filename, metadata, log, email, audit, export;
- audit append-only dengan correlation ID dan retention;
- no plaintext secret di source/XML/JSP/docs/log.

## Implementasi bertahap

### Fase A — Reconcile dan baseline

Manifest checkout, schema fingerprint, doc contradiction register, backup/restore clone, menu collision, compile baseline.

### Fase B — Foundation vertical slice

Schema/model/RBAC/routes/design system/LampiranJurnal; satu journey author submit → editor assign → reviewer → accept → publish → public article.

### Fase C — Workflow lengkap

Multi-round, copyediting, production, proof, issue, version, correction/retraction, notification.

### Fase D — Portal dan UI lengkap

Seluruh public/role/admin screens, responsive/accessibility/visual regression/usability.

### Fase E — Integrasi dan plugin parity

Identifiers, metadata exports, OAI/feed, payment/subscription, metrics/COUNTER, 45 plugin dispositions.

### Fase F — Import penuh

Legacy + OJS 3.5, file migration ke LampiranJurnal, collision UI, delta/cutover, reconciliation.

### Fase G — Hardening dan release

Dependency triage, SBOM/license, security, performance/load/soak, recovery, SIT, UAT, pilot, cutover.

Setiap fase harus compile, test, document evidence, dan menghasilkan demo yang dapat diperiksa. Jangan mengklaim fase berikut lulus karena source file tersedia.

## Test dan acceptance

Wajib:

- unit/domain/state transition;
- Hibernate empty/upgrade/idempotency/Envers/schema diff;
- main-streaming compensation dan orphan repair;
- importer dry-run/execute/cancel/resume/idempotency/collision/file corruption;
- route/capability/object-scope matrix seluruh 28 menu dan public API;
- complete role E2E, multi-round/anonymity;
- official schema/protocol validators dan provider sandbox;
- browser DOM/CSRF/IDOR/XSS/accessibility/visual regression;
- load/soak dengan 100 jurnal, 100.000 submission, 1.000.000 file metadata, 10.000.000 usage event, 10.000 user;
- recovery-time and backup restore drill;
- regression Penelitian/Pengabdian, Repository, OAI, BKD, akreditasi, notifikasi, payment, dan menu AIS existing.

Catat p50/p95, throughput, query count, heap peak, error rate, environment, warm/cold state. Tidak ada target yang boleh ditandai PASS tanpa pengukuran.

## Evidence dan status

Gunakan run ledger append-only dengan:

`runId, timestamp, Git SHA, SVN revision, dirty manifest hash, DB fingerprint, JDK/Tomcat/PostgreSQL, command, exit code, artifact hash, test totals, blocker`.

Record lama yang digantikan ditandai `SUPERSEDED`, bukan dihapus. Jangan mencampur angka 3.293/3.323 tabel atau dua checksum WAR dalam satu “latest evidence”.

Status canonical: `NOT_STARTED`, `IMPLEMENTED_UNVERIFIED`, `PARTIAL_PASS`, `PASS`, `BLOCKED`, `SUPERSEDED`, `NOT_APPLICABLE_WITH_PROOF`.

## Definition of Done final

Selesai hanya bila:

- seluruh requirement OJS mempunyai traceability dan disposition;
- 134/134 tabel dan 905/905 field direkonsiliasi;
- 45/45 plugin dan 73/73 email ditutup dengan evidence;
- 28 menu, seluruh public/workspace route, RBAC dan IDOR negative test lulus;
- 12 main + 1 streaming table tervalidasi dan tidak ada duplikasi domain;
- semua file OJS yang referenced berhasil ke `LampiranJurnal` atau masuk unresolved report;
- UI modern, responsive, accessible, dan lulus UAT per role;
- performance, security, dependency Severity 1/2, sandbox integration, backup/restore, SIT/UAT/pilot/cutover gate ditutup;
- tidak ada secret, reviewer identity leak, data loss, broken canonical URL, atau writer workflow kedua;
- dokumentasi, run ledger, rollback, dan daftar file berubah lengkap.

## Format laporan setiap sesi

1. checkout/run ID;
2. scope yang dikerjakan;
3. temuan dan keputusan;
4. file yang berubah saja;
5. schema/menu/route yang berubah;
6. test command dan hasil nyata;
7. screenshot/visual evidence bila UI berubah;
8. blocker dan risiko;
9. langkah aman berikutnya.

Jangan menulis “selesai”, “setara OJS”, atau “PASS” bila masih ada evidence yang belum dijalankan.
