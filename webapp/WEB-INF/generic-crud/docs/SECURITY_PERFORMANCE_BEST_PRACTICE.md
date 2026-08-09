# Security, Performance, dan Operability Master CRUD Generik

## 1. Security posture

Generic CRUD adalah komponen berisiko tinggi karena satu kesalahan dapat membuka banyak entity sekaligus. Prinsip dasarnya:

```text
Deny by default
Allow-list every field/operator/action
Check authorization on every request
Validate object scope after loading
Audit every mutation and privileged export
```

Entity tidak boleh aktif hanya karena class ditemukan oleh reflection.

## 2. Authentication dan session

- Gunakan mekanisme login/session existing.
- Endpoint AJAX tidak boleh menerima user/role dari request sebagai sumber kebenaran.
- Role aktif berasal dari session dan diverifikasi masih dimiliki user.
- Session timeout menghasilkan 401/redirect yang dipahami UI.
- Mutasi yang selesai setelah session/role berubah harus dibatalkan atau divalidasi ulang.
- Jangan menaruh session Hibernate/entity lazy di JSP/session HTTP.

## 3. Authorization matrix

### 3.1 Operation mapping

| Operasi | Privilege minimum |
|---|---|
| Meta/list/detail/lookup | READ |
| XLSX/PDF/DOCX/PPTX export | READ |
| Create | CREATE |
| Update/photo update/bulk edit | UPDATE |
| Delete/mass delete/import delete | DELETE |
| Approve | APPROVE |
| Reject | REJECT |
| Import button | CREATE + UPDATE + DELETE |
| Custom action | Explicit per action |

### 3.2 Defense in depth

Privilege diperiksa pada:

1. menu rendering;
2. page route;
3. service dispatcher;
4. facade/service method;
5. object/row scope;
6. job execution dan result download.

UI visibility bukan security boundary.

### 3.3 Field-level policy

Field dapat mempunyai policy terpisah:

```text
readable
filterable
sortable
createable
updateable
importable
exportable
masking mode
```

Contoh: NIK dapat terlihat masked pada table, penuh pada detail role tertentu, tidak exportable untuk role lain.

## 4. Object-level authorization / data scope

Setelah menerima ID:

1. parse/validate type;
2. load melalui query yang sudah diberi scope, atau load lalu `validateObjectScope`;
3. jika tidak ditemukan dalam scope, response 404/403 sesuai policy konsisten;
4. jangan hanya memeriksa menu READ.

Relation yang dipilih pada form juga harus berada dalam scope user. Jangan percaya relation ID dari browser.

## 5. Safe metadata and dynamic query

### 5.1 Field allow-list

Request hanya boleh memakai key yang ada pada `GenericCrudDefinition`. Jangan menerima:

- property path arbitrary;
- HQL fragment;
- SQL column;
- order direction selain ASC/DESC;
- operator di luar enum;
- arbitrary join/association.

### 5.2 Hibernate runtime verification

Pada startup/first use:

- pastikan entity ada di `SessionFactory.getAllClassMetadata()`;
- identifier dan type tersedia;
- setiap field definition ada di mapped property;
- relation target mapped;
- sortable/filterable type didukung;
- invalid definition dinonaktifkan dan dilaporkan.

### 5.3 Alias management

Gunakan alias map deterministic per relation path. Cegah alias injection dan duplicate association path. Maksimum join depth dikonfigurasi.

### 5.4 Parameter binding

Semua nilai filter memakai Hibernate parameters/Criteria restrictions. Jangan concatenation nilai ke HQL/SQL.

## 6. Mass assignment prevention

Jangan melakukan generic bean population dari request ke entity. Alur aman:

```text
request JSON/form
  -> parsed command map
  -> allow-listed field definitions
  -> type converter
  -> validation
  -> adapter setter/update method
  -> entity
```

Fields seperti ID, tenant, createdBy, password hash, audit, version, approval state, dan internal flags tidak boleh dapat diubah kecuali adapter eksplisit.

## 7. CSRF dan HTTP method

- GET hanya untuk read/idempotent operation.
- Semua mutation harus POST (atau method sesuai infrastructure jika tersedia).
- Token CSRF terikat session dan diverifikasi server-side.
- Jangan menaruh token mutasi pada URL.
- Response mutation mengirim no-store.
- SameSite cookie digunakan bila stack/container mendukung tanpa merusak login existing.

## 8. Input validation

Validasi dilakukan sebelum business operation:

- null/required;
- length;
- numeric range/scale;
- date range;
- enum allow-list;
- format kode/email/telepon sesuai domain;
- Unicode normalization bila relevan;
- uniqueness;
- relation existence dan scope;
- cross-field rules;
- state transition;
- maximum filter count dan nesting depth.

Jangan hanya mengandalkan client-side validation.

## 9. Output encoding dan XSS

- Escape semua value saat dirender ke HTML.
- JSON serializer harus escape karakter berbahaya.
- Jangan memasukkan value user ke `innerHTML`; gunakan `textContent` atau template aman.
- Sanitasi rich text hanya bila entity memang membutuhkannya.
- File name/download header disanitasi.
- Spreadsheet formula injection dicegah: value yang dimulai `=`, `+`, `-`, `@` diperlakukan aman sesuai policy export.

## 10. File upload security

### 10.1 XLSX

- allow-list `.xlsx` dan signature ZIP/OOXML;
- tolak macro-enabled format bila tidak didukung;
- ukuran file maksimum;
- jumlah sheet, row, column, shared string, formula, dan decompression ratio dibatasi;
- simpan temporary di luar web root;
- generated file name;
- scan antivirus/hook jika infrastructure tersedia;
- parse formula sebagai value/text sesuai policy; jangan mengeksekusi external link;
- bersihkan temporary file di `finally`/job cleanup.

### 10.2 Foto

- validasi magic bytes;
- decode image untuk memastikan valid;
- batasi dimensi dan pixel count;
- re-encode server-side bila sesuai helper existing;
- strip metadata sensitif jika kebijakan memungkinkan;
- generated name;
- tidak executable;
- hindari path traversal.

## 11. Import safety

- dry-run wajib;
- user melihat create/update/delete/error sebelum confirm;
- operation privilege per row;
- scope per row;
- natural key explicit;
- duplicate row handling explicit;
- maximum row per job;
- chunk size dikonfigurasi;
- idempotency hash;
- job owner/role snapshot;
- privilege dan scope divalidasi ulang saat execute;
- delete=true mempunyai second confirmation;
- partial failure menghasilkan report, tidak diam-diam sukses.

Transaction policy:

- default chunk transaction, misalnya 100–500 row;
- mode all-or-nothing hanya untuk dataset kecil dan explicit;
- setiap chunk rollback penuh bila error fatal;
- error row tidak boleh meninggalkan object setengah tersimpan;
- adapter dapat memilih policy lebih ketat.

## 12. Export safety

- export selalu mengikuti current scope dan filter;
- field sensitif excluded/masked;
- row limit dan timeout;
- large export async;
- output file mempunyai expiry;
- download memeriksa owner/role/privilege;
- signed/opaque job ID bila infrastructure memungkinkan;
- audit siapa mengekspor entity, filter hash, jumlah row, format;
- jangan mencatat seluruh data export di log.

## 13. Audit dan logging

Audit mutation minimal:

```text
requestId
entityKey
objectId / natural key
operation
actor userId
activeRoleId
menuId
tenant/scope summary
timestamp
before snapshot (masked)
after snapshot (masked)
changed fields
result
reason/comment
source UI/import/custom action
jobId bila ada
```

Jangan audit:

- password/plain or hash;
- token/secret;
- full binary/photo;
- session/cookie;
- full payment card/bank secrets;
- data sensitif yang tidak diperlukan.

Application log menggunakan request ID. Stack trace hanya di server log, response user mendapat pesan aman + request ID.

## 14. Optimistic concurrency

Jika entity mempunyai `@Version` atau last-update property yang dapat dipercaya:

- kirim version token pada edit;
- update dengan version check;
- conflict menghasilkan HTTP 409;
- UI menampilkan data terbaru dan perbedaan;
- import update juga mematuhi version policy bila template membawa version.

Jika tidak ada version property, adapter dapat memakai timestamp/field hash. Jangan silent overwrite untuk entity kritis.

## 15. Query performance

### 15.1 Paging

- count dan data query terpisah;
- `setFirstResult`/`setMaxResults`;
- deterministic sort + identifier tie-breaker;
- page size allow-list;
- default 10;
- page-size 500/1000 hanya untuk role/use case yang diizinkan dan query yang aman.

### 15.2 Deep pagination

OFFSET besar dapat mahal. Untuk page dalam:

- gunakan keyset/cursor bila sort mendukung;
- atau batasi page jump berdasarkan policy;
- pertahankan nomor halaman untuk rentang normal;
- tampilkan warning/alternatif export untuk dataset sangat besar.

### 15.3 Count query

- jangan fetch join collection;
- gunakan count identifier/distinct hanya bila join memerlukan;
- filter dan scope harus identik dengan data query;
- adapter dapat menyediakan optimized count;
- cache count hanya jika invalidasi jelas.

### 15.4 Index governance

Jangan otomatis membuat indeks untuk setiap field filterable. Gunakan:

- slow query log/`pg_stat_statements` bila tersedia;
- `EXPLAIN (ANALYZE, BUFFERS)` pada staging;
- index existing review;
- cardinality dan write cost;
- migration terpisah dan rollback.

### 15.5 Relation lookup

- count terlebih dahulu dengan scope;
- <=20 load combobox;
- >20 server-side lookup;
- debounce/cancel stale requests;
- minimal selected fields;
- caching hanya untuk reference data yang aman dan keyed by tenant/role/scope.

## 16. Memory management

- jangan load seluruh result untuk export/import;
- streaming XLSX untuk dataset besar;
- close workbook/input/output stream di finally;
- temporary files cleaned;
- Jasper virtualizer atau batching bila existing mendukung;
- jangan simpan entity list besar di HTTP session;
- DTO projection untuk table/export apabila memungkinkan;
- hindari eager loading association besar.

## 17. Hibernate session discipline

- service boundary membuka/menutup session secara konsisten;
- query dan mapping ke DTO selesai sebelum session ditutup;
- view tidak memicu lazy load;
- mutation transaction atomic;
- rollback pada exception;
- clear/flush per chunk import untuk menahan memory;
- jangan share Session antar thread/job;
- async job membuat session sendiri.

## 18. Background jobs

Job record menyimpan:

```text
jobId, type, entityKey, ownerUserId, activeRoleId,
status, progress, totalRows, successRows, errorRows,
request/filter/config snapshot, result path/token,
createdAt, startedAt, finishedAt, expiresAt, error summary
```

Ketentuan:

- worker idempotent;
- lock/claim job aman;
- heartbeat/stale job recovery;
- retry hanya untuk error yang aman;
- cancel cooperative;
- file expiry cleanup;
- privilege/scope recheck;
- user notification;
- admin monitor dengan masking.

## 19. Rate limiting dan abuse guard

Minimal guard:

- maximum concurrent export/import per user;
- maximum filter conditions;
- maximum nested relation depth;
- maximum page size;
- maximum export rows;
- maximum lookup requests/debounce;
- expensive custom action throttling;
- request timeout;
- job queue limit.

Response 429 dengan retry guidance bila limit tercapai.

## 20. Error contract

Contoh response:

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Periksa kembali data yang diisi.",
  "requestId": "...",
  "fieldErrors": {
    "kode": "Kode sudah digunakan."
  }
}
```

Kode minimal:

```text
AUTH_REQUIRED
FORBIDDEN
OBJECT_OUT_OF_SCOPE
ENTITY_DISABLED
FIELD_NOT_ALLOWED
INVALID_FILTER
VALIDATION_ERROR
CONFLICT
FILE_TOO_LARGE
INVALID_XLSX
IMPORT_CONFIRMATION_REQUIRED
JOB_NOT_FOUND
RATE_LIMITED
INTERNAL_ERROR
```

Jangan mengirim HQL/SQL, path server, stack trace, password, atau entity internal dump.

## 21. Monitoring

Metrics/log summary yang relevan:

- request count/latency per entity+operation;
- slow query count;
- error rate;
- import/export jobs dan duration;
- row processed;
- permission denied count;
- conflict rate;
- temporary storage usage;
- queue depth;
- cache hit/miss metadata/lookup;
- file cleanup failures.

Gunakan label dengan cardinality terbatas; jangan label metric dengan object ID/user ID.

## 22. Deployment dan rollback

- feature flag global dan per entity;
- default disabled;
- enable pilot Agama terlebih dahulu;
- old UI link tetap tersedia;
- DB migration backward-compatible;
- generator tidak menghapus file manual;
- clear JSP work cache setelah deploy bila perlu;
- rollback dapat mematikan route generic tanpa menghapus data config;
- export/import job dari versi lama diselesaikan/dibatalkan dengan policy jelas.

## 23. Security acceptance gates

Sebelum entity `FULL_CRUD`:

- [ ] READ denied without role privilege;
- [ ] direct ID outside scope denied;
- [ ] create/update relation outside scope denied;
- [ ] property/operator/sort injection rejected;
- [ ] CSRF mutation rejected;
- [ ] mass assignment fields ignored/rejected;
- [ ] XLSX corrupt/oversize/zip bomb guard tested;
- [ ] formula injection handled;
- [ ] delete=true preview + DELETE recheck;
- [ ] export masks sensitive fields;
- [ ] job download isolated;
- [ ] audit contains no secrets;
- [ ] session/transaction resource leak test passed;
- [ ] old role/tenant data cannot leak through cache;
- [ ] error response contains request ID but no internals.

## 24. Performance acceptance gates

- [ ] no full dataset list paging;
- [ ] query count and data use identical filters/scope;
- [ ] default list first response within agreed target on representative data;
- [ ] sort/filter fields reviewed with query plan where necessary;
- [ ] relation >20 does not preload all records;
- [ ] page 100+ behavior documented/tested;
- [ ] export/import large dataset uses streaming/job;
- [ ] import flush/clear prevents unbounded memory;
- [ ] temporary files cleaned;
- [ ] concurrent jobs limited;
- [ ] UI cancels/ignores stale search response;
- [ ] no lazy loading from JSP.



# Amandemen Security/Performance V2.1

## 25. Audit integrity

- audit/revision historis append-only pada level aplikasi;
- tidak ada endpoint generic untuk update/delete audit row;
- restore/koreksi membuat revisi baru;
- mask password, PIN, token, secret, binary, dan PII sesuai policy;
- log actor, active role, binding menu, request ID, source, reason, before/after termasking, dan result;
- audit query tetap mengikuti READ dan row-level scope;
- audit export mempunyai authorization dan masking yang sama.

## 26. Restore safety

- property allow-list dan Hibernate runtime metadata;
- UPDATE untuk data aktif; CREATE+UPDATE untuk data terhapus;
- dry-run mass restore;
- transaction per target;
- cycle/depth guard untuk deep restore;
- dependency/FK validation;
- optimistic token;
- idempotency key;
- background job rate/size limits;
- downloadable error log tanpa secret.

## 27. Super Admin active-row delete

Jangan percaya role label. Gunakan `Common.getApakahAdmin()` dan DELETE privilege binding aktif. Entity policy default disabled. Wajib typed confirmation, reason, CSRF, POST, scope, optimistic/FK/domain preflight, audit, dan rollback.

Generic delete tidak boleh mem-purge audit table. Transaksi final/posted menggunakan cancellation atau reversal.

## 28. Complex form safety

- provider/JSP/class hanya dari server allow-list;
- larang path traversal dan arbitrary `Class.forName` dari request;
- field tidak readable tidak dikirim ke browser;
- per-tab save tetap menjalankan privilege/scope/validation/audit;
- lazy endpoint tidak boleh membuka association di luar scope;
- save-before-enter idempotent;
- batasi jumlah tab/field payload dan lakukan lazy load untuk aggregate besar.
