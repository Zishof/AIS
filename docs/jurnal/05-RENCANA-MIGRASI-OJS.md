# Rencana migrasi OJS

## Keputusan koneksi dan persistence

- Source OJS: JDBC read-only dengan konfigurasi eksternal per `ImportSumberOjs`; connection/pool ditutup oleh job dan tidak terdaftar pada Hibernate AIS.
- Target AIS: main `HibernateUtil`/`hibernate.cfg.xml`; entity existing tetap pada schema existing dan hanya 12 tabel journal-specific/import final berada pada `penelitiandanpengabdian`.
- `OjsHibernateUtil.java`/`hibernate.ojs.cfg.xml` tidak dipakai implementasi baru dan dipensiunkan setelah jalur lama lulus compatibility/redirect test.
- BLOB besar: `RepoBitstream` menyimpan metadata/revision/galley pada main DB; `LampiranJurnal` existing menyimpan bytes melalui `StreamingHibernateUtil` dengan scalar `repoBitstreamId=RepoBitstream.id`. Gunakan `LampiranJurnal` khusus streaming.

## Source modes

- Read-only JDBC connection to a live/replica database.
- Database dump restored into an isolated temporary database.
- File root or validated archive supplied separately from database metadata.

Credentials are externalized and redacted. JDBC source dipaksa read-only bila driver mendukung, memakai query parameter, fetch size/keyset pagination, timeout, dan transaksi source read-only. OJS PHP code is never executed in AIS and source schema is never mutated.

## Dialect detection

Detection combines `versions`, schema/table/column signatures and application metadata:

- **LEGACY** requires `articles`, `article_settings`, `published_articles` and the matching legacy columns; reader-nya tetap JDBC, bukan enam entity OJS legacy.
- **OJS_3505** requires modern `submissions`, `publications`, `publication_settings`, `submission_files`, review/workflow tables and compatible version metadata.
- Any mixed or unsupported signature fails preflight before business writes.

Enam entity Hibernate OJS existing hanya dipertahankan untuk mencegah kerusakan kode lama selama masa transisi. Importer terpadu memakai JDBC projection/readers berparameter untuk LEGACY maupun OJS_3505; adapter OJS_3505 tidak pernah menjalankan query `articles/article_settings`.

## Job lifecycle

```text
CREATED -> PREFLIGHTED -> DRY_RUN_COMPLETE -> RUNNING
        -> PAUSED/FAILED_RESUMABLE -> RECONCILING -> COMPLETED
```

Each checkpoint stores source instance UUID, table/entity, keyset cursor, batch number, counts, checksum state and last committed target provenance. Cancellation occurs only at transaction/checkpoint boundaries.

## Dependency order

1. verify physical Repository AIS tables/columns on target clone; site/tenant → `PerguruanTinggi`; journal → canonical `JurnalPenelitian` plus linked `RepoCollection`;
2. identities → `Tbmuser`/`RepoAuthorAuthority`; institutions → existing `PerguruanTinggi`/`PerguruanTinggiLain`, policy → `LanggananJurnal`, IP → `RentangIpLanggananJurnal`; ROR IDs are normalized, not mirrored into a new master/profile;
3. OJS role-group definitions → administrator-reviewed `Tbmrole.jurnalAksesJson` mapping/provenance; memberships/stage rows → scoped `PenugasanTahapJurnal`;
4. allowlisted settings/navigation/plugin profiles → `RepoCollection`/journal JSON; subscription-type catalog → `accessPolicyJson`; versioned review-form definitions → `workflowProfileJson`; sections/categories/vocabularies → typed collection hierarchy; email template → one new table;
5. issues/static pages/highlights → typed `RepoItem`; issue settings → `RepoItemMetadata`; TOC/order → `RepoItemRelation`;
6. submissions/publication versions → `RepoItem`; contributors/affiliations → repository authority/link; citations → repeatable metadata; `Artikel` projection only after publish;
7. file metadata/revision/galley → `RepoBitstream`; accepted bytes → `LampiranJurnal`; temporary files are not imported;
8. review round lifecycle → `RepoWorkflowEvent`; reviewer assignments/round number/form response snapshot → `PenugasanReviewerJurnal`; discussions → existing `Diskusi`/`DiskusiKomentar` plus participant link; decisions/notes/legacy comments → structured append-only events;
9. DOI/OAI/withdrawal → `RepoItem`/`RepoBitstream`; deposit attempts → `RepoIntegrationEvent`;
10. subscriptions/payments;
11. announcements → existing `PengumumanPenelitian` path; static/highlight already handled as repository items;
12. raw metrics → `RepoUsageEvent`, one aggregate table; logs/tombstones → repository/platform events;
13. rebuild search indexes/caches/reports; do not import OJS search rows, sessions, resumption tokens, PHP jobs/filters, or temporary usage staging.

## Identity and collision policy

Unique provenance is `(sourceInstanceUuid, sourceTable, sourcePrimaryKey, sourceRevision)`. A numeric OJS ID is never globally unique. People are linked by existing provenance first, then verified email, otherwise manual/external profile; names alone are prohibited. DOI, URN, ORCID, ISSN, username, email and slug are normalized and checked before execute.

Allowed decisions are link, non-destructive metadata merge, create external profile, skip with reason, or manual resolution. Every decision is included in the immutable report.

## Migrasi role tanpa tabel redundan

- `user_groups`, `user_group_settings`, dan `user_group_stage` tidak membuat master role jurnal baru. Dry-run mengusulkan pemetaan ke `Tbmrole` existing dan capability stabil `JurnalAksesKatalog`.
- Importer tidak boleh mengubah `jurnalAksesJson` role existing secara diam-diam. Perlu persetujuan administrator per source group/context; before/after checksum dan keputusan pemetaan masuk audit/provenance.
- `user_user_groups`, `stage_assignments`, dan `subeditor_submission_group` menghasilkan assignment journal/section/submission/stage setelah identity serta journal scope ter-resolve. Mereka tidak menambah role global atau `job_has_menu` otomatis.
- Group yang tidak dikenal menjadi blocker/manual mapping, bukan default manager/editor. Key OJS mentah tetap disimpan pada `ImportMappingOjs.rawPayload` untuk rekonsiliasi.
- Re-run bersifat idempotent berdasarkan provenance. Pengurangan capability atau berakhirnya `date_end` tidak menghapus hak existing tanpa kebijakan dan approval eksplisit.

## Reuse platform dan batas redundansi

- Account tetap `Tbmuser`; active role tetap `Tbmrole`; module entry tetap `Menu`/`job_has_menu`.
- Notifikasi journal memakai `Notifikasi`, `NotifikasiDibaca`, `MailSender`, dan preference existing. Hanya satu `TemplateEmailJurnal` baru; immutable sent subject/body/recipient/result tetap pada structured Notifikasi payload/provenance.
- Payment journal memakai abstraction dan log pembayaran AIS existing untuk settlement; hanya policy/order/subscription linkage journal-specific yang ditambah.
- Subscription-type source rows menjadi versioned `RepoCollection.accessPolicyJson`; setiap `LanggananJurnal` menyimpan immutable policy snapshot, sehingga tidak ada `TipeLanggananJurnal`.
- Review form/element source rows menjadi append-only version pada `RepoCollection.workflowProfileJson`; response menjadi checksummed immutable JSON pada reviewer assignment, sehingga tidak ada tabel round/form/element/answer paralel.
- `JurnalPenelitian` adalah canonical journal master. `RepoItem`/repository aggregate adalah workflow/publication model. `Artikel`, `FileArtikel`, dan `AnggotaArtikel` hanya published compatibility projections.
- `RepoCollection`, `RepoItem`, `RepoItemMetadata`, `RepoBitstream`, relation/authority/contributor/workflow/integration/usage/preference direuse; class lokal yang belum tracked/deployed wajib lolos ownership dan physical-schema verification.
- Model baru hanya sah bila ada scope, lifecycle, invariant, atau provenance OJS yang tidak dapat disimpan pada tabel existing tanpa mengubah semantik existing.

## File migration

- Reconstruct paths using the detected version/configuration.
- Reject traversal, NUL, absolute escape, symlink escape and archive bomb.
- Sniff MIME and compare to extension/source metadata.
- Stream through bounded buffers, enforce quota/size, compute SHA-256 and verify bytes.
- Record source table/ID/file/revision/path/checksum and target scalar IDs.
- Create/update `RepoBitstream` metadata first with `PENDING_CONTENT`, stream into `LampiranJurnal(repoBitstreamId=RepoBitstream.id)`, verify checksum/bytes, then mark `CONTENT_VERIFIED/LINKED`.
- Never feed an opaque streaming locator to existing `new File(pathSistem)` paths; services must branch on validated storage backend.
- Missing/corrupt published files are reconciliation blockers; non-public optional files are explicit warnings.

## Reconciliation

For every mapped table/entity: source rows, accepted, linked, created, merged, skipped, failed and target totals. For files: metadata count, present/missing, bytes and SHA-256. For relationships: contributor, issue/TOC, review, decision, discussion, identifier and access links. Finalization is allowed only with zero blockers.

`ImportJobOjs` holds the immutable dry-run/final reconciliation JSON, so no separate reconciliation table is introduced. PHP job/filter payload, OAI tokens, temporary files and temporary usage rows are counted as deliberately excluded, with rationale and source checksum.

## Rollback/recovery

No destructive mass rollback is exposed initially. Recovery uses database backup/restore clone, idempotent retry and provenance-scoped cleanup designed and tested before production authorization. A failed content stage remains `FAILED` and recoverable without pretending the business entity is linked.

