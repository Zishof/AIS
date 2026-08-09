# Matriks Requirement dan Acceptance Criteria

Dokumen ini memetakan permintaan pengguna menjadi artefak teknis dan pengujian yang dapat diverifikasi. Nomor requirement mengikuti urutan permintaan.

## R1 — READ, CREATE, UPDATE, DELETE, APPROVE, REJECT

### Implementasi

- `GenericCrudPrivilegeGuard` memakai mekanisme `CommonPrivilages`/`RolePrivilage` existing.
- Menu/page hanya tampil jika READ.
- Semua endpoint memeriksa privilege ulang.
- Approval/reject hanya aktif melalui adapter entity.
- Row-level scope terpisah dari menu privilege.

### Acceptance

- [ ] Tanpa READ, sidebar, command palette, page route, service list/detail/lookup/export ditolak.
- [ ] Tanpa CREATE, tombol add hilang dan direct POST create menghasilkan 403.
- [ ] Tanpa UPDATE, edit/photo/bulk-edit ditolak.
- [ ] Tanpa DELETE, delete row/mass/import delete ditolak.
- [ ] APPROVE/REJECT tidak muncul pada entity tanpa adapter.
- [ ] Perubahan role aktif langsung mengubah menu/action setelah cache invalidation.
- [ ] ID di luar scope tidak dapat dibaca atau dimutasi walaupun user mempunyai privilege menu.

## R2 — Download dan upload XLSX, termasuk `delete=true`

### Implementasi

- `GenericCrudExportService` untuk filtered XLSX.
- `GenericCrudImportService` + import wizard.
- Tombol upload hanya untuk CREATE+UPDATE+DELETE.
- Dry-run dan preview operasi.
- `delete=true` memakai DELETE privilege, key unambiguous, dependency policy, second confirmation, audit.
- Streaming/batch untuk data besar.

### Acceptance

- [ ] Export menghasilkan record yang sama dengan filter/scope aktif.
- [ ] Template berisi header yang valid dan instruksi.
- [ ] Import tidak melakukan mutasi sebelum confirm.
- [ ] Create/update/delete/skip/error terhitung per row.
- [ ] User tanpa salah satu CREATE/UPDATE/DELETE tidak melihat upload dan service menolak.
- [ ] `delete=true` tanpa DELETE ditolak walau job telah di-upload.
- [ ] File corrupt, header salah, tipe salah, oversize, duplicate key, dan row luar scope dilaporkan.
- [ ] File sama tidak dieksekusi ulang tanpa override idempotensi.
- [ ] Error workbook dapat diunduh.

## R3 — PDF, DOCX, PPTX berdasarkan filter

### Implementasi

- `GenericCrudDocumentExportService` memakai JasperReports/exporter existing dan Apache POI helper existing bila sesuai.
- Synchronous untuk kecil, async job untuk besar.
- Filter/scope/sort summary dicetak.

### Acceptance

- [ ] Semua format mengikuti filter/scope/sort.
- [ ] Kolom sensitif tidak ikut tanpa izin.
- [ ] PDF mempunyai header, filter summary, page number, total record.
- [ ] DOCX terbuka valid dan tabel terbaca.
- [ ] PPTX membagi data ke slide, bukan satu slide sangat padat.
- [ ] Large export berjalan sebagai job dan result hanya dapat diunduh owner/authorized admin.

## R4 — Quick filter, advanced filter, relation combo/bandbox

### Implementasi

- Quick filter 3–6 common fields.
- Advanced filter dialog bila field >6.
- Active filter chips selalu terlihat.
- Relation count <=20 combo, >20 bandbox server-side.
- Common relation field diverifikasi dengan Hibernate metadata.
- Nested lookup max depth/cycle guard.

### Acceptance

- [ ] Quick fields sesuai konfigurasi dan benar-benar mapped.
- [ ] Advanced dialog mempertahankan nilai saat ditutup/diterapkan.
- [ ] Chips menunjukkan operator dan nilai aktif, dapat dihapus satu per satu.
- [ ] Relation dengan 20 data menjadi combo; 21 data menjadi bandbox.
- [ ] Bandbox paging/filter/sort dilakukan database.
- [ ] Property common yang tidak ada tidak pernah dipakai dalam query.
- [ ] Relation di luar scope tidak muncul.
- [ ] Nested relation berhenti pada depth limit dan siklus tidak menyebabkan recursion tak terbatas.

## R5 — Popup/fungsi add/edit modern

### Implementasi

- Shared form drawer/modal/full-page responsive.
- Schema berdasarkan field definition + adapter.
- Relation editor mengikuti R4.
- Error summary, inline validation, sticky action footer, unsaved warning, optimistic conflict.

### Acceptance

- [ ] Form desktop rapi 2–3 kolom dan mobile 1 kolom.
- [ ] Section/group dapat dibuka dan error section otomatis terbuka.
- [ ] Nilai valid tidak hilang saat server validation error.
- [ ] Direct mass assignment field internal ditolak.
- [ ] Relation scope/required/unique/cross-field validation berjalan.
- [ ] Conflict update menghasilkan 409 dan UI memberi pilihan aman.

## R6 — Ganti foto

### Implementasi

- `GenericCrudPhotoAdapter` dan adapter khusus Mahasiswa, Dosen, Siswa, Guru, Pegawai, Tbmuser.
- Reuse helper/storage existing.
- Preview/crop/rotate/upload, signature and size validation.

### Acceptance

- [ ] Photo button hanya untuk entity photo-enabled dan UPDATE.
- [ ] Foto lama tampil; fallback avatar jika kosong.
- [ ] File non-image/oversize ditolak.
- [ ] Path filesystem tidak bocor.
- [ ] Photo update ter-audit tanpa menyimpan binary ke audit.
- [ ] Temporary file dibersihkan.
- [ ] Storage/helper existing tidak diduplikasi.

## R7 — Custom/override buttons

### Implementasi

- `GenericCrudCustomActionProvider` explicit allow-list.
- Placement toolbar/row/detail/bulk.
- Required privilege, selection mode, confirmation, params, async.
- Adapters parity untuk Mahasiswa dan entity kompleks.

### Acceptance

- [ ] Engine dapat menambah custom action tanpa edit shared JSP/query engine.
- [ ] Method `on*` tidak otomatis diekspos melalui reflection.
- [ ] Custom action tanpa privilege ditolak server-side.
- [ ] Single/multiple/all-filtered selection tervalidasi.
- [ ] Action parameter memakai schema dan validation.
- [ ] Action existing Mahasiswa yang dipilih dalam parity matrix memanggil business helper yang sama atau ekuivalen teruji.

## R8 — Paging database dan page size

### Implementasi

- Count query + data query.
- `setFirstResult`/`setMaxResults`.
- Allow-list 5,10,25,50,100,500,1000.
- Default 10, preference persisted.
- Hybrid/keyset untuk deep page bila diperlukan.

### Acceptance

- [ ] Tidak ada load-all-then-subList.
- [ ] Total record benar untuk filter/scope.
- [ ] Semua pilihan page size bekerja.
- [ ] Invalid page size dinormalisasi/ditolak.
- [ ] Filter/sort change kembali ke page 1.
- [ ] Delete last row pada page menavigasi ke page valid.
- [ ] Query log menunjukkan LIMIT/OFFSET/setMaxResults sesuai page.

## R9 — Semua kolom sortable ke database

### Implementasi

- Header sort untuk mapped scalar atau safe adapter sort.
- Relation alias allow-list.
- Foto/Aksi non-sortable.
- Tie-breaker ID.

### Acceptance

- [ ] Setiap kolom default selain Foto/Aksi dapat ASC/DESC jika backend mendukung.
- [ ] Sort tidak dilakukan pada list browser/memory.
- [ ] Invalid field/direction ditolak.
- [ ] Duplicate values mempunyai urutan stabil dengan ID tie-breaker.
- [ ] Relation sort tidak menghasilkan duplicate rows/unknown property.

## R10 — Column chooser dan persistensi

### Implementasi

- `GenericCrudColumnPreferenceService`.
- Key userId+activeRoleId+entityKey+viewName.
- Visible/order/width/pinned/pageSize/density/sort/filter versioned.
- Default common relevant fields.

### Acceptance

- [ ] User dapat show/hide/reorder/resize/pin.
- [ ] Setelah login/reopen, preference terakhir kembali.
- [ ] Role berbeda dapat mempunyai preference berbeda.
- [ ] Reset mengembalikan default.
- [ ] Field yang dihapus dari metadata tidak menyebabkan page error.
- [ ] User tidak dapat memilih field sensitif/non-readable.

## R11 — Fitur tambahan relevan

### Implementasi prioritas

- Saved views;
- bulk edit dengan preview;
- soft delete/restore;
- audit compare;
- optimistic concurrency;
- duplicate detection/merge adapters;
- data quality score;
- async job + notification;
- all-filtered selection token;
- field masking;
- shareable filter URL;
- keyboard/accessibility;
- query/file/job limits;
- stale data indicator.

### Acceptance

- [ ] Extension point tersedia tanpa membuat engine monolitik tidak terawat.
- [ ] Fitur berisiko default off.
- [ ] Saved view tidak dapat membocorkan filter/column sensitif ke user lain.
- [ ] All-filtered token terikat user/role/scope/filter dan mempunyai expiry.
- [ ] Audit compare memasking field sensitif.
- [ ] Job mempunyai status, progress, ownership, expiry, cleanup.

## R12 — UI/UX modern, responsive, best practice

### Implementasi

- Reuse New UI shell.
- Desktop table + detail drawer.
- Tablet adaptive.
- Mobile card view dan full-screen form/filter.
- Accessibility dan full state coverage.

### Acceptance

- [ ] 1920/1366/1024/768/390/360 px diuji.
- [ ] Tidak ada content tertanam di sidebar/DOM rusak.
- [ ] Mobile dapat menjalankan list/filter/detail/add/edit/delete/custom action.
- [ ] Focus/keyboard/dialog/error summary bekerja.
- [ ] Status tidak hanya warna.
- [ ] Loading/empty/error/conflict/partial state tersedia.
- [ ] Tidak ada horizontal scroll sebagai satu-satunya cara memakai mobile.


## R13 — Audit Trails, Restore, dan Hapus Data Ini oleh Super Admin

### Implementasi

- Audit global seluruh record dan audit per row/per ID;
- dashboard/filter revision type, actor, role, waktu, source, request ID, dan field berubah;
- compare before/after;
- restore satu field, koreksi manual field aktif, restore full revision, deep restore, restore deleted row;
- restore terbaru mulai tanggal dengan dry-run, progress, cancel, log, dan hasil per item;
- historical audit immutable;
- `Hapus Data Ini` hanya untuk `Common.getApakahAdmin()` + DELETE + scope + entity policy;
- delete hanya menghapus row aktif, bukan audit history;
- typed confirmation, reason, FK/domain/optimistic preflight, transaction, rollback, audit.

### Acceptance

- pengguna tanpa READ menerima 403 untuk audit;
- row di luar scope tidak terlihat;
- field/revision restore membuat audit/revisi baru;
- audit historis tidak berubah;
- restore deleted record memerlukan CREATE+UPDATE;
- deep restore aman terhadap cycle/dependency;
- mass restore selalu preview lebih dahulu;
- non-admin dan admin tanpa DELETE ditolak;
- policy disabled menolak admin delete;
- audit tetap tersedia setelah active row dihapus;
- pilot aman dapat direstore setelah admin delete;
- password/token tidak masuk audit payload.

## R14 — Custom/Override Add/Edit Form Kompleks

### Implementasi

- provider dan registry form server-side;
- mode drawer/modal/tabbed/full-page/wizard/custom/legacy bridge;
- metadata tab, section, field, action, validation group, save strategy;
- conditional/lazy tabs;
- `requiresPersistedEntity` dan idempotent `saveBeforeEnter`;
- error summary lintas tab;
- field/tab/action privilege server-side;
- generic fallback bila tidak ada override;
- pilot Mahasiswa dengan parity seluruh tab/field/action/validation existing.

### Acceptance

- request tidak dapat memilih class/JSP arbitrary;
- generic entity tetap memakai renderer standar;
- conditional dan lazy tab bekerja;
- save-before-enter tidak membuat duplicate root;
- validation memfokuskan tab/field error;
- unsaved-change guard bekerja;
- mobile dan keyboard tidak kehilangan fungsi;
- Mahasiswa tidak enabled sebelum parity matrix lulus.

# Requirement lintas fitur

## Backward compatibility

- [ ] Action/ZUL existing tidak dihapus pada tahap awal.
- [ ] Agama parity matrix disusun sebelum switch.
- [ ] Mahasiswa/Siswa/Tbmuser dimigrasikan belakangan dengan adapter khusus.
- [ ] Feature flag per entity tersedia.
- [ ] Rollback route ke UI lama tersedia.

## Data correctness

- [ ] Runtime Hibernate metadata menjadi sumber kebenaran.
- [ ] Collection/blob/sensitive/transient default excluded.
- [ ] Every mutation transactionally safe.
- [ ] No lazy loading from JSP.
- [ ] Scope filter sama untuk count/data/export/import lookup.

## Build and code quality

- [ ] Java 1.7/source style compatible.
- [ ] Ant clean/build lulus.
- [ ] `git diff --check` bersih.
- [ ] No Java 8 APIs/syntax.
- [ ] No business logic in JSP.
- [ ] No placeholder `501` on migrated pilot.
- [ ] Unit/component/integration/UI test evidence tersimpan.

# Definition per entity status

| Status | Perilaku |
|---|---|
| DISABLED | Tidak ada route/menu/API aktif |
| REVIEW_REQUIRED | Hanya metadata/admin diagnostic, tidak tersedia untuk user umum |
| READ_ONLY | List/detail/filter/sort/export sesuai policy, tanpa mutation |
| FULL_CRUD | Create/update/delete/import/export dan adapter yang lulus test |

Entity transaksi, audit/log, integrasi bank, file content, token, queue/job, dan entity internal tidak boleh otomatis naik ke FULL_CRUD.
