# Inventaris fitur OJS 3.5.0-5

Sumber: tag OJS `372e3b8`, pkp-lib `8b5f0fd`, `registry/userGroups.xml`, `registry/emailTemplates.xml`, install migrations, dan plugin tree. Inventaris ini adalah baseline perilaku clean-room; tidak ada PHP, template, CSS, JS, terjemahan, ikon, atau aset GPL yang disalin ke AIS.

Keputusan persistence existing-first: semua fitur memakai main `HibernateUtil`/`hibernate.cfg.xml`; entity existing tetap pada schema existing dan hanya 12 tabel journal-specific/import yang direncanakan pada `penelitiandanpengabdian`. `JurnalPenelitian` adalah journal master, Repository AIS adalah publication foundation, `Artikel`/`FileArtikel`/`AnggotaArtikel` adalah published compatibility projections, dan `LampiranJurnal` menyimpan BLOB streaming dengan scalar `RepoBitstream.id`. OJS dibaca JDBC read-only; `OjsHibernateUtil` bukan dependency baru.

## Scope produk

| Area | Kemampuan yang harus tersedia native | Kondisi AIS saat audit |
|---|---|---|
| Multi-journal | tenant/journal scope, slug, locale, identity, contact, policies, appearance | PARTIAL_EXISTING; `JurnalPenelitian` master + `RepoCollection` profiles/hierarchy perlu extension |
| Author submission | resumable wizard, checklist/consent, files, metadata, contributors, references | PARTIAL_EXISTING; `RepoItem`, `RepoItemMetadata`, `RepoBitstream`, authority/contributor sudah mencakup content foundation |
| Editorial | queues, scoped participants, deadlines, discussions, immutable decision history | GAP_CONFIRMED |
| Review | anonymous modes, rounds, reviewer pool/conflict, forms, reminders, recommendations | GAP_CONFIRMED |
| Copyediting/production | stage assignment, consultation, revisions, proof and approval | GAP_CONFIRMED |
| Publication | immutable versions, issue scheduling, galleys, publish/unpublish/retract/correction | PARTIAL_EXISTING; `RepoItem` version/DOI/OAI/access/withdrawal + `RepoBitstream`; `Artikel` published projection |
| Public portal | journal/current/archive/issue/article/search/about/announcement | GAP_CONFIRMED for journal; Pustaka gives a shell pattern only |
| Identity/communication | AIS account linking, external profile, invitations, notification preferences | PARTIAL_PLATFORM_CAPABILITY; journal scope is absent |
| Access/payment | open/subscription/embargo, individual/institution IP, payment reconciliation | PARTIAL_PLATFORM_CAPABILITY; journal policy/state is absent |
| Discovery | DOI/URN, OAI-PMH, Crossref/DataCite/DOAJ/PubMed, ORCID, CRediT, JATS, feeds/sitemap | PARTIAL_EXISTING; `RepoItem`, `RepoAuthorAuthority`, `RepoIntegrationEvent`, dan servlet OAI sudah ada |
| Statistics | usage events, privacy aggregation, COUNTER and reports | PARTIAL_EXISTING; raw `RepoUsageEvent`/item counters reuse, hanya satu aggregate table baru |
| Import | legacy six-table importer berbasis SessionFactory OJS | PARTIAL_LEGACY_ONLY; diganti orchestration JDBC read-only dan target `HibernateUtil` |

## Roles and scope

Baseline groups include Site Administrator, Journal Manager, Editor, Section Editor, Guest Editor, Production Editor, Copyeditor, Designer, Funding Coordinator, Indexer, Layout Editor, Marketing and Sales Coordinator, Proofreader, Author, Translator, External Reviewer, Reader, Subscription Manager, and Editorial Board Member.

AIS implementation uses one account and three intersecting authorization layers, all based on existing platform ownership:

1. active AIS role (`Tbmuser.hakAkses`) plus `job_has_menu` controls entry to the journal module;
2. `Tbmrole.jurnalAksesJson` controls the role's journal menu, CRUD, and workflow capabilities through a central fail-closed `JurnalAksesKatalog`;
3. journal assignments control tenant → journal → section → submission → stage → operation.

Effective access is the intersection of all three layers plus ownership, state, anonymity and conflict rules. Multiple stored AIS roles are never unioned silently.

OJS role-group metadata is import input, not a reason to duplicate the AIS role model. `user_groups`, `user_group_settings`, and `user_group_stage` map to an administrator-reviewed capability profile/provenance for existing `Tbmrole`; `user_user_groups`, `stage_assignments`, and subeditor mappings become scoped `PenugasanTahapJurnal` records. Import preflight must report unmapped OJS groups and must not silently broaden an existing role's `jurnalAksesJson`.

## Workflow/state inventory

Primary states: draft, submission/screening, external review, copyediting, production, scheduled, published, and terminal declined/archived/withdrawn variants.

Required editorial events: send to review; accept/skip review; accept; request revisions; resubmit for review; decline; initial decline; recommendation outcomes; new round; revert decline; send to production; return from production/copyediting; cancel round. Each event is append-only with actor, before/after state, round, reason, recipients, template snapshot, attachment and correlation ID.

## Email inventory

`registry/emailTemplates.xml` contains exactly 73 unique keys at the verified tag. The required list in the implementation specification matches those 73 when `PASSWORD_RESET_CONFIRM` and `USER_REGISTER` at the start of the block are included. Implementation must provide Indonesian and English content, a variable allowlist, immutable send snapshot, idempotency key and delivery status. Content must be written originally for AIS.

## API/public surface inventory

- context/journal, settings, sections/categories, issues/TOC;
- submissions, publication versions, contributors and files;
- users, roles, invitations, reviewer forms/rounds/decisions;
- announcement/highlight/static page/navigation;
- institutions/ROR/vocabulary;
- DOI/URN/deposit state and export;
- email template/mailable, upload, jobs/failures, statistics/reports;
- OAI-PMH verbs, metadata formats, feeds, sitemap and public galley access.

All lists require pagination; all mutations require object authorization, CSRF where cookie-authenticated, validation, audit and safe error responses.

## Consolidation rules

- `RepoCollection` bertipe `JOURNAL`, `SECTION`, `CATEGORY`, atau `VOCABULARY` menampung hierarchy, order, metadata/workflow/access profile JSON. Tidak dibuat tabel section/category/vocabulary terpisah.
- `RepoItem.documentType` membedakan `JOURNAL_SUBMISSION`, `JOURNAL_ISSUE`, `STATIC_PAGE`, dan publication version. Issue metadata memakai `RepoItemMetadata`; TOC/version/category memakai `RepoItemRelation` yang di-extend secara kompatibel.
- `RepoBitstream` menampung file/galley/revision metadata. `LampiranJurnal` menampung bytes. Temporary OJS file tidak diimport kecuali menjadi final referenced file.
- `RepoWorkflowEvent` menampung state transition, editorial decision, note, imported comment, serta `ROUND_STARTED`/`ROUND_CLOSED`. `roundNumber` berada pada assignment/file; tidak dibuat tabel review round.
- `RepoCollection.workflowProfileJson` menampung append-only versioned review-form definitions. `PenugasanReviewerJurnal` menyimpan `formVersionKey`, immutable typed `responseJson`, dan checksum; tidak dibuat tabel form/element/answer.
- `RepoCollection.accessPolicyJson` menampung versioned subscription-type catalog. `LanggananJurnal` menyimpan immutable policy snapshot agar perubahan harga/masa berlaku tidak mengubah histori.
- OJS editorial `queries` memakai `Diskusi`/`DiskusiKomentar` existing dengan typed journal reference; hanya participant membership yang memerlukan link table baru.
- Institusi memakai `PerguruanTinggi`/`PerguruanTinggiLain`, subscription policy memakai `LanggananJurnal`, dan IP memakai satu `RentangIpLanggananJurnal`; tidak dibuat profile/master institusi jurnal paralel.
- Delivery/preference memakai `Notifikasi`, `NotifikasiDibaca`, `RepoNotification`, dan `RepoUserPreference`; hanya template email versioned yang baru.
- OJS search tables, resumption token, PHP jobs/filters, sessions, dan temporary usage staging tidak mempunyai target business table; semuanya derived atau excluded dengan reconciliation evidence.

