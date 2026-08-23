# Feature traceability register — Jurnal Terpadu

Register ini mengikat seluruh baseline discovery ke implementasi dan evidence. Status hanya berubah menjadi `PASS` setelah test yang disebut benar-benar dijalankan; keberadaan source file saja tidak cukup.

## Namespace requirement dan test

| Namespace | Cakupan canonical | Requirement ID | Planned test ID | Owner | Target release |
|---|---:|---|---|---|---|
| OJS table | 134 tabel pada `02-OJS-3505-TABLE-MAPPING.md` menurut urutan heading | `MAP-001..MAP-134` | ID `MAP-nnn` pada setiap bagian mapping | Backend/Data | Jurnal v1 |
| OJS field | 905 baris field pada tabel mapping; ID deterministik `MAP-nnn-Fmmm` menurut urutan field dalam source table | `MAP-nnn-Fmmm` | `IMP-FIELD-nnn-mmm` dan rekonsiliasi import | Backend/Data | Jurnal v1 |
| Plugin | 45 baris pada `03-OJS-3505-PLUGIN-PARITY.md` | `PLG-001..PLG-045` | `PLG-nnn-NATIVE` atau `PLG-nnn-NA-PROOF` | Backend/QA | Jurnal v1 |
| Email | 73 key berurutan pada `JurnalEmailTemplateCatalog.KEYS` | `EMAIL-001..EMAIL-073` | `EMAIL-nnn-RENDER`, `EMAIL-nnn-DELIVERY` | Backend/QA | Jurnal v1 |
| Menu | child `460501..460528` | `MENU-001..MENU-028` | positive/negative `MENU-nnn-AUTH` dan `MENU-nnn-ROUTE` | Backend/Security | Jurnal v1 |
| Area produk | 12 area pada feature inventory | `AREA-001..AREA-012` | journey `E2E-AREA-nnn` | Product/QA | Jurnal v1 |
| Tabel baru | 12 tabel approved | `DB-NEW-001..DB-NEW-012` | `DB-SCHEMA-12`, upgrade, idempotency, restore | DBA/Backend | Jurnal v1 |

`MAP-nnn-Fmmm` tidak mengubah atau menggandakan 905 mapping: identitas field adalah pasangan ID tabel pada heading dan posisi baris `Source column` pada dokumen generated. Generator mapping tetap menjadi sumber canonical serta wajib gagal bila jumlah berubah.

## Architecture decision records

| ADR | Keputusan | Konsekuensi wajib | Status |
|---|---|---|---|
| `ADR-JRN-001` | Existing-first dan budget tepat 12 tabel jurnal | Tidak membuat master jurnal, role, notification, payment, issue, publication, contributor, discussion, file-content, atau plugin table paralel | ACCEPTED |
| `ADR-JRN-002` | Satu `HibernateUtil`/`hibernate.cfg.xml` | Tidak ada dependency importer baru pada `OjsHibernateUtil` atau `hibernate.ojs.cfg.xml`; source OJS memakai JDBC read-only | ACCEPTED |
| `ADR-JRN-003` | BLOB memakai `LampiranJurnal` streaming | `RepoBitstream.contentRef` adalah scalar reference ke entity mandiri; tidak ada ORM lintas database | ACCEPTED |
| `ADR-JRN-004` | RBAC jurnal pada `Tbmrole.jurnalAksesJson` | Active role + `job_has_menu` + fail-closed capability + object scope; `RolePrivilage` bukan fallback | ACCEPTED |
| `ADR-JRN-005` | Repository adalah source of truth workflow/publikasi | `Artikel`, `FileArtikel`, `AnggotaArtikel` hanya proyeksi kompatibilitas idempoten | ACCEPTED |
| `ADR-JRN-006` | Definition/form/policy versioned dalam profile JSON | Assignment/subscription menyimpan snapshot immutable; validator schema/version/size wajib | ACCEPTED |
| `ADR-JRN-007` | Import provenance multi-source | Key source/table/PK/field, checksum, checkpoint/resume; payload executable tidak pernah dijalankan | ACCEPTED |

## Dependency graph ringkas

`environment/backup → migration → RBAC/routing → journal/identity → submission/files → review/discussion → production → issue/publication → portal/communication/access → integrations/statistics → importer → hardening → SIT → UAT → pilot/cutover`.

Fase boleh overlap hanya setelah kontrak dependensinya lulus. Database baseline `ais` tidak menjadi target mutation pengembangan.

## Definition of done dan severity

Satu requirement selesai hanya bila requirement, implementasi, automated test, negative authorization bila applicable, migration/import reconciliation, dan evidence environment tercatat. Severity 1 berarti kehilangan/korupsi data, bypass authorization lintas tenant/jurnal, atau outage total; Severity 2 berarti workflow utama, publish, file, payment, atau importer reconciliation gagal; Severity 3 adalah fungsi non-kritis dengan workaround; Severity 4 kosmetik/dokumentasi. RC/SIT/UAT tidak boleh keluar dengan Severity 1/2 terbuka.

## Risk register aktif

| Risk | Mitigasi/gate | Owner |
|---|---|---|
| Worktree besar berisi perubahan pengguna | Tidak reset/checkout; diff hanya file jurnal dan extension yang disetujui | Backend |
| Konfigurasi lama dapat menunjuk baseline | Environment jurnal memaksa factory sendiri; bootstrap test menolak nama database `ais` | Backend/DBA |
| Main DB dan streaming DB tidak atomik | State machine, checksum, compensation/retry/orphan reconciliation | Backend |
| JSON rusak/abuse/unknown grant | version/size/type/depth/count allowlist dan fail-closed | Security |
| Anonimitas reviewer bocor | DTO/file/log/email allowlist plus negative tests | Security/QA |
| Import sangat besar/berbahaya | JDBC read-only, identifier catalog, timeout, bounded batch, checkpoint, source checksum | Data/QA |
| External side effect pada test | Email capture serta payment/DOI sandbox; production endpoint forbidden | Release |


