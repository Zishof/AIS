# Arsitektur modul Jurnal eCampus

## Prinsip existing-first

- Hanya main `HibernateUtil` dan mapping `hibernate.cfg.xml` dipakai untuk metadata/domain/workflow/provenance. Entity existing tetap pada schema existing; tabel journal-specific baru berada di `penelitiandanpengabdian`.
- `OjsHibernateUtil`/`hibernate.ojs.cfg.xml` adalah legacy-only. Source OJS dibaca JDBC read-only menjadi DTO, bukan entity source pada persistence unit AIS.
- `JurnalPenelitian` adalah canonical journal master. Tidak dibuat tabel/entity `Jurnal` kedua.
- Repository AIS adalah publication foundation: `RepoCollection`, `RepoItem`, `RepoItemMetadata`, `RepoBitstream`, `RepoItemRelation`, authority/contributor, workflow/integration/usage event, notification, dan preference direuse/di-extend.
- `Artikel`, `FileArtikel`, dan `AnggotaArtikel` tetap canonical compatibility projections untuk Penelitian dan Pengabdian, BKD, akreditasi, serta laporan existing; mereka bukan writer workflow kedua.
- BLOB memakai `LampiranJurnal` existing pada streaming SessionFactory dengan scalar `repoBitstreamId=RepoBitstream.id`. `LampiranJurnal` adalah entity streaming mandiri; tidak ada ORM lintas database.
- Global capability memakai `Tbmrole.jurnalAksesJson`; tidak ada global journal role/group table.

## Canonical graph

```text
JurnalPenelitian (canonical journal master)
  -> RepoCollection(type=JOURNAL, sourceClass/sourceId -> JurnalPenelitian)
     -> RepoCollection(type=SECTION/CATEGORY/VOCABULARY)
     -> RepoItem(documentType=JOURNAL_SUBMISSION)
        -> RepoItem version chain (publication versions)
        -> RepoItem(documentType=JOURNAL_ISSUE) via RepoItemRelation/TOC sequence
        -> RepoItem(documentType=STATIC_PAGE) where applicable
        -> RepoItemMetadata (localized/repeatable metadata and citations)
        -> RepoAuthorAuthority + RepoItemContributor
        -> RepoBitstream (file/revision/galley metadata)
           -> LampiranJurnal streaming BLOB, scalar ref only
        -> RepoWorkflowEvent / RepoIntegrationEvent / RepoUsageEvent
        -> Artikel + FileArtikel + AnggotaArtikel published projections
```

Required compatible extensions:

- `RepoCollection`: `sourceClass/sourceId` or equivalent verified link to `JurnalPenelitian`; typed profile schema/version.
- `RepoItem`: journal/root-submission/current-stage scope and explicit `documentType` rules; keep version chain and optimistic lock.
- `RepoItemRelation`: related collection and sequence support for category/TOC, or equivalent validated relation without a new master.
- `RepoBitstream`: storage backend/opaque locator, OJS stage/genre/revision and optional galley DOI; filesystem services must branch safely for streaming backend.
- `RepoWorkflowEvent`: versioned structured payload plus optional stage/review-round reference; append-only after insert.

## What is not a new table

| OJS concern | Existing target |
|---|---|
| journal/site/settings | `JurnalPenelitian`, `PerguruanTinggi`, linked `RepoCollection` profiles |
| submission/publication/issue/static page/highlight | typed `RepoItem` |
| localized settings/citation | `RepoItemMetadata` or versioned profile JSON |
| section/category/vocabulary/navigation/plugin config | typed `RepoCollection` hierarchy/profiles |
| contributor/affiliation/ORCID/ROR/interest | `RepoAuthorAuthority`, `RepoItemContributor`, `RepoUserPreference` |
| file/revision/galley | `RepoBitstream`; content `LampiranJurnal` |
| DOI/OAI/tombstone/deposit | `RepoItem`/`RepoBitstream`, `RepoIntegrationEvent`, existing OAI servlet |
| transition/decision/note/legacy comment | structured `RepoWorkflowEvent` |
| review round | `RepoWorkflowEvent` round lifecycle; `roundNumber` on assignment/file metadata |
| review form/element | append-only versions in `RepoCollection.workflowProfileJson` |
| review response | immutable typed snapshot on `PenugasanReviewerJurnal` |
| subscription type/catalog | versioned `RepoCollection.accessPolicyJson`; immutable snapshot on `LanggananJurnal` |
| editorial discussion/topic/reply | existing `Diskusi`/`DiskusiKomentar`; only participant membership is new |
| notification/email delivery log/read state/preference | `Notifikasi`, `NotifikasiDibaca`, `RepoNotification`, `RepoUserPreference`, `MailSender` |
| payment settlement | `JenisPembayaran`, `LogPembayaran`, existing providers |
| institution identity/access policy | `PerguruanTinggi`/`PerguruanTinggiLain`; policy on `LanggananJurnal`, only IP range is new |
| raw usage/search | `RepoUsageEvent`/`RepoItem`; search rebuilt from repository metadata |

## Minimal new tables

Final implementation budget is 12 tables:

1. `TemplateEmailJurnal`;
2. `LanggananJurnal` (`policyKey` plus immutable policy/pricing snapshot JSON);
3. `UndanganPeranJurnal`;
4. `PesertaDiskusiJurnal` (membership for existing `Diskusi`);
5. `PenugasanTahapJurnal`;
6. `PenugasanReviewerJurnal` (round, invitation lifecycle, anonymity, recommendation, form version and immutable response JSON);
7. `AgregatPenggunaanJurnal` (one dimensioned daily/monthly/COUNTER table);
8. `RentangIpLanggananJurnal`;
9. `ImportSumberOjs`;
10. `ImportJobOjs` (status, dry-run/final reconciliation report JSON);
11. `ImportCheckpointOjs`;
12. `ImportMappingOjs` (multi-source provenance/raw payload/decision).

Hard ceiling 15 is allowed only after clone-schema/constraint/analytics evidence proves a listed aggregate must be split. A new table must document the missing existing invariant, ownership, retention, cardinality, query/index need, and why a JSON/profile or compatible extension is insufficient.

## Review workflow boundary

Repository workflow is deliberately coarse and currently has a single assigned reviewer. Item state/events stay in Repository, while journal-specific reviewer invariants are owned by `PenugasanReviewerJurnal`:

- multiple reviewers per round;
- double/single/open anonymity;
- invitation/accept/decline deadlines;
- recommendation versus final editorial decision;
- `roundNumber`, form-version key and immutable typed response snapshot;
- conflict-of-interest and author-reviewer exclusion;
- section/submission/stage scoped assignment.

Services perform one logical command through the main SessionFactory: validate capability/scope/state, mutate `RepoItem` plus assignment, append `RepoWorkflowEvent`, create notification, and update compatibility projections only when publish rules require it.

## Versioned JSON contracts

- `RepoCollection.accessPolicyJson` has `schemaVersion`, stable `policyKey`, localized label, currency, price, duration, format, institutional/membership flags and activation state. Existing entries are never mutated after a subscription references them; `LanggananJurnal.policySnapshotJson` is authoritative for history.
- `RepoCollection.workflowProfileJson` has `schemaVersion` and append-only review-form versions. Every element has a stable key, sequence, type, required/included flags, localized prompt/description and allowlisted options. Published versions are immutable.
- `PenugasanReviewerJurnal.responseJson` stores typed answers keyed by stable element key, plus `formVersionKey`, SHA-256 checksum, submitted time and recommendation. It is immutable after submission except through an explicit audited replacement/version operation.
- JSON is parsed through typed DTOs, size/depth/count limits and fail-closed validation; raw OJS payload remains in `ImportMappingOjs`, not inside operational profiles.
- A normalized form/element/answer table may be proposed later only when measured cross-journal per-question SQL analytics cannot be served by a derived projection.

## File consistency

There is no XA transaction between main and streaming SessionFactories:

```text
RepoBitstream PENDING_CONTENT
  -> LampiranJurnal stream/write + SHA-256/byte verification
  -> RepoBitstream CONTENT_VERIFIED
  -> LINKED
  -> FAILED (retryable)
```

`LampiranJurnal.repoBitstreamId` adalah scalar `RepoBitstream.id`; `idempotencyKey` unik mengikat satu content. Retry memakai provenance/checksum. Authorization terjadi sebelum streaming session dibuka. Request tidak pernah memasok path fisik atau class name.

## Authorization

```text
active Tbmrole + job_has_menu module gate
  ∩ Tbmrole.jurnalAksesJson capability
  ∩ tenant and JurnalPenelitian/RepoCollection scope
  ∩ PenugasanTahapJurnal/reviewer assignment or ownership
  ∩ RepoItem workflow-state command
  ∩ anonymity and conflict rule
```

`JurnalAksesKatalog` is the single fail-closed evaluator. `RolePrivilage` remains generic menu behavior and is not a duplicate journal capability store. Site-admin override is explicit and audited using an existing generic integration/audit event path where the object type is supported.

## Web boundary

- All journal public and management views reside under `C:\opt\AIS\ais\src\main\webapp\WEB-INF\baru\modul\jurnal\*`.
- `/jurnal` and protected management routes invoke Java controller/services; a retained old alias only redirects.
- `TbmroleAction` owns the fourth **Hak Akses Pengelolaan Jurnal** tab.
- JSP is presentation-only; no Hibernate query/session/mutation and no direct JSON authorization logic.
- File download accepts an opaque bitstream ID, checks object policy, resolves the storage backend server-side, and streams bounded content.

## Compatibility and deployment evidence

- Four repository core entities are tracked; eight auxiliary entities are local/mapped but some remain untracked. Class presence is not proof of production table presence.
- Run `webapp/sql/verifikasi_repository_modern_v2.sql` plus `to_regclass`/column checks for authority, contributor, relation, workflow, usage, notification, preference, and integration tables on a restored clone.
- Existing repository services must receive document-type guards so journal items do not enter repository-only queues or public results incorrectly.
- Existing filesystem-only `RepoBitstream.pathSistem` callers must be inventoried before enabling streaming backend.
- Keep legacy OJS entity/action paths through compatibility tests, then redirect both old import buttons to the JDBC importer.

