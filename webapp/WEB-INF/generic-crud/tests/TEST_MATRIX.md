# Test Matrix Master CRUD Generik AIS

Gunakan ID test berikut pada automated test/report dan parity matrix. Semua test harus dijalankan terhadap role, tenant/scope, dan data representatif.

## 1. Metadata dan registry

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| META-001 | Class bukan turunan GeneralValueObject | Registry menolak |
| META-002 | Class abstract/mapped superclass | Tidak tersedia sebagai CRUD page |
| META-003 | Entity tidak ada di Hibernate runtime metadata | Disabled + diagnostic |
| META-004 | Field reflection tidak mapped | Tidak boleh filter/sort/form/import/export |
| META-005 | Collection property | Excluded dari field standar |
| META-006 | Blob/password/token field | Hidden/sensitive policy, default excluded |
| META-007 | Duplicate entityKey/module-page | Startup/config validation gagal aman |
| META-008 | Definition berubah dan user preference punya field lama | Field lama diabaikan, page tetap terbuka |
| META-009 | Entity default configuration baru | REVIEW_REQUIRED/disabled |
| META-010 | Invalid adapter class | Entity disabled, error diagnostic tanpa mematikan aplikasi lain |

## 2. Authorization

Buat role uji per privilege dan role kombinasi.

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| AUTH-001 | Tanpa READ membuka menu | Menu tidak tampil |
| AUTH-002 | Tanpa READ deep-link page | 403/authorized fallback |
| AUTH-003 | Tanpa READ call list/detail/lookup/export | 403 |
| AUTH-004 | READ-only role | List/detail/export tampil; mutation hidden dan ditolak |
| AUTH-005 | Tanpa CREATE POST create | 403 |
| AUTH-006 | Tanpa UPDATE POST update/photo/bulk-edit | 403 |
| AUTH-007 | Tanpa DELETE delete/import-delete | 403 |
| AUTH-008 | Tanpa APPROVE/REJECT | Transition hidden dan service menolak |
| AUTH-009 | Entity tidak mendukung approval | APPROVE/REJECT tidak tersedia walau role punya privilege |
| AUTH-010 | Upload visibility kurang satu privilege | Upload tidak tampil |
| AUTH-011 | Role berubah saat page terbuka | Request berikutnya memakai role baru, cache invalidated |
| AUTH-012 | Custom action required privilege | Hidden/403 sesuai role |
| AUTH-013 | Job result milik user lain | 403/404 |
| AUTH-014 | Saved view role lain/private | Tidak terlihat/tidak dapat dibuka |
| AUTH-015 | Column sensitive tanpa field policy | Tidak tersedia |

## 3. Scope / IDOR

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| SCOPE-001 | Tenant A list | Tidak ada row tenant B |
| SCOPE-002 | Direct ID tenant B | Denied |
| SCOPE-003 | Update/delete tenant B | Denied + audit denied |
| SCOPE-004 | Relation lookup | Hanya option dalam scope |
| SCOPE-005 | Form submit relation luar scope | Validation/403 |
| SCOPE-006 | Export filter | Tidak ada row di luar scope |
| SCOPE-007 | Import create/update/delete luar scope | Row error, tidak dimutasi |
| SCOPE-008 | All-filtered selection token setelah scope berubah | Token invalid/denied |
| SCOPE-009 | Cache relation dari tenant A dipakai tenant B | Tidak terjadi leakage |
| SCOPE-010 | Scope adapter tidak terdaftar | Default deny |

## 4. List, filter, sort, paging

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| LIST-001 | First load | Default 10, default sort + ID tie-breaker |
| LIST-002 | Page size 5 | Query max 5 |
| LIST-003 | Page size 10/25/50/100/500/1000 | Query sesuai allow-list |
| LIST-004 | Page size 17/negative/string | Rejected/normalized to 10 |
| LIST-005 | Page 2 | DB offset/max, bukan subList |
| LIST-006 | Filter changes on page >1 | Kembali page 1 |
| LIST-007 | Delete last row current page | Pindah ke valid page |
| LIST-008 | String contains/starts/equal | Hasil benar, parameterized |
| LIST-009 | Number/date between | Hasil benar termasuk boundary policy |
| LIST-010 | Boolean/status in/notIn | Hasil benar |
| LIST-011 | Empty/notEmpty | Hasil benar |
| LIST-012 | Invalid operator | 400 FIELD/OPERATOR_NOT_ALLOWED |
| LIST-013 | Invalid property path/HQL text | 400, tidak dieksekusi |
| LIST-014 | Sort each safe field ASC/DESC | DB order benar |
| LIST-015 | Foto/Aksi sort attempt | Ditolak/ignored, no query injection |
| LIST-016 | Relation sort | Alias aman, no duplicate/unknown path |
| LIST-017 | Duplicate sort values | Urutan stabil dengan ID |
| LIST-018 | Deep page | Policy keyset/hybrid/limit berjalan |
| LIST-019 | Count vs data scope/filter | Total konsisten |
| LIST-020 | Concurrent search responses | Response lama tidak menimpa baru |

## 5. Advanced filter dan saved views

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| FILT-001 | >6 filterable fields | Filter Lanjutan tampil |
| FILT-002 | Dialog apply | Chips tetap terlihat |
| FILT-003 | Remove chip | Query berubah dan page reset |
| FILT-004 | Reset all | Default filter kembali |
| FILT-005 | Save private view | Tersimpan user+role |
| FILT-006 | Set default view | Terbuka otomatis berikutnya |
| FILT-007 | Shared role view unauthorized | Tidak terlihat |
| FILT-008 | URL state | Reload mempertahankan safe filter |
| FILT-009 | Sensitive filter in share URL | Excluded/opaque sesuai policy |
| FILT-010 | Definition field removed | Saved view dimigrasi/field ignored |

## 6. Relation combo/bandbox

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| REL-001 | Relation count 0 | Empty state |
| REL-002 | Relation count 1–20 | Combo |
| REL-003 | Relation count 21+ | Bandbox |
| REL-004 | Count after scope <=20 | Combo berdasarkan scoped count |
| REL-005 | Search common property mapped | Query berhasil |
| REL-006 | Common property tidak mapped | Tidak digunakan/tidak error |
| REL-007 | Bandbox paging | DB paging 10 |
| REL-008 | Bandbox sort/filter | Server-side |
| REL-009 | Nested lookup depth 2 | Berjalan dengan breadcrumb |
| REL-010 | Nested depth >limit | Ditolak/disederhanakan |
| REL-011 | Cycle A→B→A | Guard menghentikan recursion |
| REL-012 | Selected relation becomes inactive/out-of-scope | Warning + validation policy |

## 7. Form dan mutation

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| MUT-001 | Create valid | Commit, audit, row appears |
| MUT-002 | Required missing | 422 + field error + value retained |
| MUT-003 | Type/length/range invalid | 422 |
| MUT-004 | Unique duplicate | 422 friendly message |
| MUT-005 | Internal field submitted | Rejected/ignored per strict policy |
| MUT-006 | ID/tenant/createdBy submitted | Cannot override server values |
| MUT-007 | Relation nonexistent | 422 |
| MUT-008 | Relation out-of-scope | Denied |
| MUT-009 | Update valid | Commit + before/after audit |
| MUT-010 | Optimistic version conflict | 409, no overwrite |
| MUT-011 | Delete valid | Soft/hard policy applied + audit |
| MUT-012 | Delete dependency blocked | Friendly failure, no partial delete |
| MUT-013 | Transaction exception | Rollback all changes |
| MUT-014 | Session opened manually | Closed in finally |
| MUT-015 | Lazy field in JSP | No lazy load/session error |
| MUT-016 | Unsaved changes close | Confirmation shown |
| MUT-017 | Save & Add Again | Previous commit, form reset correctly |
| MUT-018 | Bulk edit preview | Only allowed fields/rows shown |
| MUT-019 | Bulk edit partial invalid | Policy explicit, result report |

## 8. Foto

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| PHOTO-001 | Entity no photo adapter | Photo UI absent |
| PHOTO-002 | User no UPDATE | Photo action absent/403 |
| PHOTO-003 | Valid JPG/PNG | Preview, process, save |
| PHOTO-004 | Extension image but invalid magic bytes | Rejected |
| PHOTO-005 | Oversize/pixel bomb | Rejected |
| PHOTO-006 | Crop/rotate | Result correct |
| PHOTO-007 | Storage failure | Old photo retained, temp cleaned |
| PHOTO-008 | Audit | Metadata change only, no binary |
| PHOTO-009 | Path traversal filename | Generated safe filename |
| PHOTO-010 | Mobile camera input | Works/fallback file picker |

## 9. XLSX export/import

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| XLSX-001 | Filtered export | Row parity with list filter/scope |
| XLSX-002 | Visible/custom columns | Only allowed selected fields |
| XLSX-003 | Sensitive field requested | Masked/excluded |
| XLSX-004 | Large export | Streaming/async, bounded memory |
| XLSX-005 | Valid template dry-run | No DB mutation, preview counts |
| XLSX-006 | Corrupt/non-XLSX/macro | Rejected |
| XLSX-007 | Oversize/too many rows/columns | Rejected with limit message |
| XLSX-008 | Header missing/duplicate | Row/file errors |
| XLSX-009 | Create rows | Only with CREATE |
| XLSX-010 | Update rows | Only with UPDATE |
| XLSX-011 | delete=true rows | DELETE + second confirm + audit |
| XLSX-012 | Ambiguous key | Error, no mutation |
| XLSX-013 | Duplicate key rows | Deterministic policy/report |
| XLSX-014 | Relation invalid/out-of-scope | Row error |
| XLSX-015 | Mixed create/update/delete/error | Correct result summary |
| XLSX-016 | Chunk fatal error | Current chunk rollback |
| XLSX-017 | Re-upload same file | Idempotency warning/block |
| XLSX-018 | Privilege revoked before execute | Job denied/failed safely |
| XLSX-019 | Formula injection on export | Safe cell handling |
| XLSX-020 | Formula/external link on import | Not executed; policy applied |
| XLSX-021 | Temp cleanup | Files expired/deleted |
| XLSX-022 | Error workbook | Contains row and friendly errors |

## 10. PDF/DOCX/PPTX export

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| DOC-001 | PDF small | Valid PDF, filters/scope summary |
| DOC-002 | DOCX small | Valid Word, readable table |
| DOC-003 | PPTX small | Valid slides, pagination/layout |
| DOC-004 | Large dataset | Async job |
| DOC-005 | Wide columns | Landscape/custom columns handled |
| DOC-006 | Empty result | Valid “no data” document |
| DOC-007 | Unicode/Indonesian text | Rendered correctly |
| DOC-008 | Job ownership | Unauthorized download denied |
| DOC-009 | Result expired | Denied/expired message |
| DOC-010 | Generator exception | Failed status + cleanup + request ID |

## 11. Column chooser/preferences

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| COL-001 | Default open | Relevant common columns shown |
| COL-002 | Show/hide | Table updates |
| COL-003 | Reorder/resize/pin | State applies |
| COL-004 | Reload | State persisted |
| COL-005 | Different active role | Separate preference |
| COL-006 | Reset | Defaults restored |
| COL-007 | Sensitive/non-readable column | Cannot select |
| COL-008 | Field deleted/renamed | No error; migration/ignore |
| COL-009 | Page size saved | Restored next open |
| COL-010 | Concurrent preference update | Last/version policy deterministic |

## 12. Custom action/approval

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| CUST-001 | Registered toolbar action | Visible in correct placement |
| CUST-002 | Unregistered on* method | Not exposed |
| CUST-003 | Wrong selection count | Action disabled/rejected |
| CUST-004 | Required params missing | 422 |
| CUST-005 | Danger confirmation | Required before execute |
| CUST-006 | Async action | Job/status/result |
| CUST-007 | All-filtered token expired/tampered | Rejected |
| CUST-008 | Business helper exception | Rollback/error/audit |
| APPR-001 | Entity no approval adapter | No approve/reject |
| APPR-002 | Valid submit/approve/reject | Correct state transition/audit |
| APPR-003 | Invalid transition | 409/422 |
| APPR-004 | Reject reason required | Validation |
| APPR-005 | Concurrent approval | One succeeds; other conflict |

## 13. UI/responsive/accessibility

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| UI-001 | 1920x1080 | Full layout balanced |
| UI-002 | 1366x768 | No default horizontal overflow |
| UI-003 | 1024/tablet landscape | Adaptive columns/actions |
| UI-004 | 768/tablet portrait | Sidebar drawer, form 2/1 columns |
| UI-005 | 390/360 mobile | Card list, full-screen dialogs/forms |
| UI-006 | Keyboard-only | All main actions reachable |
| UI-007 | Focus trap | Dialog focus contained/restored |
| UI-008 | 200–400% zoom/reflow | Critical content/function retained |
| UI-009 | Long labels/data | Truncate/wrap without overlap |
| UI-010 | Loading/empty/error/conflict | Dedicated state visible |
| UI-011 | Status color | Text/icon also present |
| UI-012 | Screen reader semantics | Labels, aria-sort, accessible buttons |
| UI-013 | Active filters after dialog | Always visible |
| UI-014 | Content include | Content inside `.nui-content`, not sidebar |
| UI-015 | Mobile destructive action | Bottom sheet + confirmation |

## 14. Performance/concurrency/operations

| ID | Skenario | Hasil yang diharapkan |
|---|---|---|
| PERF-001 | 100k+ rows list | First page bounded query/memory |
| PERF-002 | Relation 100k | No preload; paged lookup |
| PERF-003 | Export 100k | Background/streaming |
| PERF-004 | Import 100k | Chunk/flush/clear, bounded memory |
| PERF-005 | Concurrent searches | No stale overwrite |
| PERF-006 | Concurrent jobs limit | 429/queue policy |
| PERF-007 | Slow query timeout | Friendly error/request ID |
| PERF-008 | Count heavy joins | Optimized count adapter/no collection fetch |
| PERF-009 | Session leak | Connection/session count stable |
| PERF-010 | Temp storage | Expiry cleanup works |
| OPS-001 | Worker crash | Stale job detected/recovered/failed |
| OPS-002 | Deploy while job running | Defined policy, no corrupt result |
| OPS-003 | Feature flag off | Route falls back/disabled safely |
| OPS-004 | Rollback WAR | Legacy UI works; additive tables harmless |
| OPS-005 | Audit/log | No password/token/full sensitive payload |

## 15. Golden parity: Agama

| ID | Legacy behavior | New behavior expected |
|---|---|---|
| AGAMA-001 | Default filter aktif | Equivalent semantics |
| AGAMA-002 | Search nama ilike any | Equivalent, parameterized |
| AGAMA-003 | Default order nama ASC | Equivalent + ID tie-breaker |
| AGAMA-004 | Fields id,kode,nama,keterangan,aktif,feeder export/import | Equivalent plus safe delete column |
| AGAMA-005 | Nama required | Equivalent friendly validation |
| AGAMA-006 | Nama duplicate check | Equivalent case/trim policy based on source |
| AGAMA-007 | Aktif toggle/update | Privilege UPDATE + audit |
| AGAMA-008 | Feeder code/help | Preserved |
| AGAMA-009 | Revisi/audit helper | Preserved/equivalent verified |
| AGAMA-010 | CRUD privileges | Same or stricter server-side enforcement |


## 16. Audit trail dan revision query

- [ ] Audit global membutuhkan READ dan scope.
- [ ] Audit per row membutuhkan READ dan scope object.
- [ ] ADD/MOD/DEL filter benar.
- [ ] Filter tanggal, actor, role, source, request ID, dan changed field benar.
- [ ] Audit paging/count/sort dilakukan server-side.
- [ ] Compare revisi vs aktif dan dua revisi akurat.
- [ ] Secret/password/token termasking/tidak tersimpan.
- [ ] Audit history tidak mempunyai endpoint edit/delete generic.

## 17. Restore

- [ ] Restore satu field membutuhkan UPDATE.
- [ ] Manual correction memakai typed editor/validator.
- [ ] Restore membuat revisi baru, historical row tidak berubah.
- [ ] Restore revision shallow berhasil.
- [ ] Deep restore menangani dependency, cycle, depth, deferred relation.
- [ ] Restore deleted row membutuhkan CREATE+UPDATE.
- [ ] Invalid/out-of-scope relation ditolak.
- [ ] Optimistic conflict menghasilkan 409.
- [ ] Mass restore mempunyai dry-run, confirmation, progress, cancel, heartbeat, dan log.
- [ ] Satu item gagal tidak membuat item lain half-commit.
- [ ] Permission revoked mid-job menghentikan/menolak proses sesuai policy.

## 18. Hapus Data Ini oleh Super Admin

- [ ] Default entity policy disabled.
- [ ] Non-admin tidak melihat tombol dan service 403.
- [ ] `Common.getApakahAdmin()==true` tanpa DELETE tetap 403.
- [ ] Admin dengan DELETE tetapi di luar scope tetap 404/403 sesuai contract.
- [ ] Typed confirmation salah ditolak.
- [ ] Reason kosong ditolak.
- [ ] FK/domain/posted-status preflight menolak dan rollback.
- [ ] Active row terhapus pada pilot aman.
- [ ] Audit/Envers history tetap tersedia.
- [ ] Restore setelah admin delete berhasil pada pilot.
- [ ] Tidak ada endpoint generic purge audit.

## 19. Complex form override

- [ ] Entity tanpa provider memakai generic fallback.
- [ ] Provider hanya dari registry allow-list.
- [ ] Class/JSP/path traversal dari request ditolak.
- [ ] Drawer/modal/tabbed/full-page/wizard modes dapat dirender.
- [ ] Conditional tab visibility benar.
- [ ] Lazy tab tidak query sebelum dibuka.
- [ ] save-before-enter idempotent dan tidak duplicate root.
- [ ] Cross-tab error summary membuka tab dan fokus field.
- [ ] Tab/field/action privilege diperiksa server-side.
- [ ] Unsaved-change guard bekerja.
- [ ] Mobile dan keyboard tab interaction lulus.
- [ ] Mahasiswa parity: seluruh tab, field, custom action, validasi, dan business effect tercatat.

## 20. Exit report template


Untuk setiap build/release lampirkan:

```text
Git commit/branch:
Database migration version:
Entity enabled:
Entity disabled/review-required:
Test total/pass/fail/skip:
Ant build result:
Representative data size:
List p50/p95:
Export/import max tested rows:
Known issues:
Rollback tested:
Approver:
```
