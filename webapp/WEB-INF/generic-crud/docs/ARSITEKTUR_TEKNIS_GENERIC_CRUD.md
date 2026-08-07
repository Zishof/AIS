# Arsitektur Teknis Master CRUD Generik untuk AIS

## 1. Tujuan

Menyediakan satu kerangka CRUD enterprise yang dapat dipakai oleh seluruh entitas Hibernate konkret turunan `GeneralValueObject`, tetap kompatibel dengan Java 1.7/gaya source 1.6, Hibernate 3.6, Spring 3.1, PostgreSQL, JasperReports, ZK existing, dan shell JSP modern di `WEB-INF/new`.

Kerangka ini **bukan** reflection CRUD yang membebaskan semua kolom. Kerangka harus metadata-driven, deny-by-default, tenant-aware, privilege-aware, dan bisa dioverride untuk business rule khusus.

## 2. Temuan source yang menjadi landasan

Source AIS sudah memiliki:

- `ais.database.model.GeneralValueObject` sebagai superclass domain umum.
- `ais.action.master.generic.GenericCrudAction<T extends GeneralValueObject>` sebagai base CRUD ZK.
- `AgamaAction` sebagai contoh penggunaan base CRUD, filter Criteria, download/upload XLSX, form modern, renderer, dan validasi.
- `CommonPrivilages` dengan kode `READ`, `CREATE`, `UPDATE`, `DELETE`, `APPROVE`, `REJECT`.
- `CommonDownloadUpload` dan `Common.appendDownloadUploadButtons(...)` untuk alur Excel existing.
- `Report`/JasperReports yang sudah memiliki exporter PDF, DOCX, dan PPTX.
- `WordExporter` berbasis XWPF.
- mekanisme foto existing melalui `FileFoto`, `FotoAdmin`, dan helper foto pada Mahasiswa/Siswa/Pegawai.

`GenericCrudAction` existing tetap berguna untuk ZUL lama, tetapi belum cukup untuk tujuan baru karena setiap subclass masih harus menulis Criteria, renderer, dan form sendiri. Framework baru harus memperluasnya menjadi engine metadata yang juga dapat dipakai oleh JSP modern.

## 3. Arsitektur berlapis

```text
JSP Modern UI / ZUL Adapter
        │
        ▼
GenericCrudEndpoint / Thin JSP Service Adapter
        │
        ▼
GenericCrudFacade
 ├─ GenericCrudPrivilegeGuard
 ├─ GenericCrudDefinitionRegistry
 ├─ GenericCrudQueryService
 ├─ GenericCrudMutationService
 ├─ GenericCrudLookupService
 ├─ GenericCrudImportService
 ├─ GenericCrudExportService
 ├─ GenericCrudDocumentExportService
 ├─ GenericCrudColumnPreferenceService
 ├─ GenericCrudApprovalService
 ├─ GenericCrudAuditService
 └─ GenericCrudCustomActionService
        │
        ▼
GenericCrudEntityAdapter<T>
        │
        ▼
Hibernate 3.6 / Existing domain helpers / Existing Action business rules
```

## 4. Struktur file target

### 4.1 Java

```text
src/ais/action/master/generic/v2/
├── GenericCrudFacade.java
├── GenericCrudOperation.java
├── GenericCrudException.java
├── GenericCrudRequestContext.java
├── GenericCrudResult.java
├── GenericCrudPage.java
├── GenericCrudFilter.java
├── GenericCrudFilterGroup.java
├── GenericCrudSort.java
├── GenericCrudDefinition.java
├── GenericCrudFieldDefinition.java
├── GenericCrudDefinitionRegistry.java
├── GenericCrudRuntimeMetadataVerifier.java
├── GenericCrudQueryService.java
├── GenericCrudMutationService.java
├── GenericCrudLookupService.java
├── GenericCrudImportService.java
├── GenericCrudExportService.java
├── GenericCrudDocumentExportService.java
├── GenericCrudColumnPreferenceService.java
├── GenericCrudSavedViewService.java
├── GenericCrudPrivilegeGuard.java
├── GenericCrudScopeGuard.java
├── GenericCrudValidationService.java
├── GenericCrudApprovalService.java
├── GenericCrudAuditService.java
├── GenericCrudPhotoService.java
├── GenericCrudCustomActionService.java
├── GenericCrudCsrf.java
├── GenericCrudJson.java
└── adapter/
    ├── GenericCrudEntityAdapter.java
    ├── AbstractGenericCrudEntityAdapter.java
    ├── GenericCrudScopeAdapter.java
    ├── GenericCrudPhotoAdapter.java
    ├── GenericCrudApprovalAdapter.java
    ├── GenericCrudExportAdapter.java
    ├── GenericCrudImportAdapter.java
    ├── GenericCrudCustomActionProvider.java
    ├── AgamaGenericCrudAdapter.java
    ├── MahasiswaGenericCrudAdapter.java
    ├── SiswaGenericCrudAdapter.java
    ├── DosenGenericCrudAdapter.java
    ├── GuruGenericCrudAdapter.java
    ├── PegawaiGenericCrudAdapter.java
    └── TbmuserGenericCrudAdapter.java
```

### 4.2 JSP modern

```text
webapp/WEB-INF/new/_shared/generic-crud/
├── ui/
│   ├── crud_page.jsp
│   ├── table.jsp
│   ├── mobile_cards.jsp
│   ├── quick_filter.jsp
│   ├── advanced_filter_dialog.jsp
│   ├── active_filter_chips.jsp
│   ├── form_drawer.jsp
│   ├── relation_lookup_dialog.jsp
│   ├── import_wizard.jsp
│   ├── export_dialog.jsp
│   ├── column_chooser.jsp
│   ├── audit_drawer.jsp
│   ├── approval_dialog.jsp
│   └── error_summary.jsp
├── services/
│   └── dispatcher.jsp
└── assets/
    ├── generic-crud.css
    └── generic-crud.js
```

Alias per entitas mengikuti pola modul sebelumnya:

```text
webapp/WEB-INF/new/root/uiux/agama.jsp
webapp/WEB-INF/new/root/services/agama_service.jsp

webapp/WEB-INF/new/akunting/uiux/akun.jsp
webapp/WEB-INF/new/akunting/services/akun_service.jsp

webapp/WEB-INF/new/sekolah/uiux/siswa.jsp
webapp/WEB-INF/new/sekolah/services/siswa_service.jsp
```

Alias UI hanya menetapkan `entityKey` dan meng-include renderer bersama. Alias service hanya melakukan `forward` ke dispatcher. Jangan menggandakan logic.

## 4.3 Multi-page/menu binding

Satu entity dapat dipakai oleh lebih dari satu Action/menu/module. Jangan memaksa satu `entityKey` hanya mempunyai satu menu. Gunakan `generic_crud_page_binding` untuk memetakan:

```text
entity_config_id + module_key + page_key + menu_id + adapter/scope override
```

`generic_crud_entity_config` menyimpan definisi domain canonical, sedangkan page binding menyimpan konteks menu/route. Privilege selalu diperiksa terhadap `menu_id` binding aktif. Ini mencegah entity yang sama pada modul berbeda memakai privilege menu yang keliru.

## 5. Sumber kebenaran metadata

Urutan sumber metadata wajib:

1. **Hibernate runtime metadata** (`SessionFactory.getAllClassMetadata()` / `ClassMetadata`) untuk memastikan class dan property benar-benar mapped.
2. Konfigurasi database `generic_crud_entity_config` dan `generic_crud_field_config` untuk izin dan tampilan.
3. Adapter Java untuk override.
4. Anotasi/reflection Java hanya sebagai kandidat awal, bukan bukti bahwa kolom tersedia.

Konsekuensi:

- Getter yang tidak ada di `ClassMetadata.getPropertyNames()` tidak boleh otomatis dipakai dalam filter, sort, import, atau update.
- Collection (`Set`, `List`, `Map`, one-to-many) tidak ditampilkan sebagai kolom CRUD standar.
- Blob, byte array, password, token, session, secret, dan properti sensitif default disembunyikan.
- Formula/transient boleh tampil read-only hanya jika adapter mendefinisikan display extractor dan, jika perlu, sort expression yang aman.
- Properti relasi hanya boleh memakai alias Criteria yang dibuat dari metadata dan allow-list.

## 6. GenericCrudDefinition

Minimal properti:

```java
public final class GenericCrudDefinition {
    private String entityKey;
    private Class entityClass;
    private String moduleKey;
    private Long menuId;
    private String title;
    private boolean enabled;
    private boolean createEnabled;
    private boolean updateEnabled;
    private boolean deleteEnabled;
    private boolean approveEnabled;
    private boolean rejectEnabled;
    private boolean importEnabled;
    private boolean exportEnabled;
    private boolean photoEnabled;
    private int lookupThreshold;
    private int defaultPageSize;
    private int maxPageSize;
    private String defaultSortProperty;
    private boolean defaultSortAscending;
    private String versionProperty;
    private String softDeleteProperty;
    private GenericCrudEntityAdapter adapter;
    private List fields;
}
```

Jangan memakai Java 8 lambda, stream, record, Optional, atau syntax modern.

## 7. GenericCrudFieldDefinition

Minimal properti:

```text
propertyPath
label
mappedType
javaType
association
relationEntityKey
displayProperty
searchProperties
visibleInTable
visibleInQuickFilter
visibleInAdvancedFilter
visibleInForm
editableOnCreate
editableOnUpdate
required
sortable
importable
exportable
sensitive
maskingMode
editorType
filterType
width
position
section
helpText
validationRuleKey
```

### Default common-field ranking

Urutan prioritas untuk kolom default:

1. foto (jika ada; tidak sortable)
2. kode / kode* yang valid pada metadata
3. nama / namaLengkap
4. nim / nis / nip / nik
5. noRegistrasi / nomorRegistrasi / nomorUjian
6. jenis / kategori / status
7. relasi induk utama (program studi, fakultas, sekolah, yayasan, satuan kerja)
8. aktif
9. tanggal utama / tanggal dibuat / tanggal diubah
10. id sebagai fallback, biasanya disembunyikan tetapi tersedia untuk admin

Default maksimum 6–8 kolom desktop. Kolom lain tersedia melalui Column Chooser.

## 8. Privilege dan data scope

### 8.1 Privilege operasi

Setiap request—termasuk AJAX, deep-link, export, import, foto, dan custom action—harus memeriksa server-side:

```text
LIST, DETAIL, LOOKUP, EXPORT, PRINT -> READ
CREATE                              -> CREATE
UPDATE, PHOTO UPDATE                -> UPDATE
DELETE, MASS DELETE                 -> DELETE
APPROVE                             -> APPROVE
REJECT                              -> REJECT
```

Tombol Upload Excel hanya terlihat apabila:

```text
CREATE && UPDATE && DELETE
```

Namun setiap baris import tetap diperiksa lagi sesuai operasi aktualnya.

### 8.2 Role aktif

Gunakan `Common.getCurrentUser().hakAkses()` dan menu aktif existing. Jangan menggabungkan privilege `userRole2..userRole5`. Pergantian role mengikuti mekanisme role switcher yang sudah direncanakan.

### 8.3 Row-level scope

`READ` pada menu belum berarti pengguna boleh membaca seluruh baris. Setiap definition wajib mempunyai scope adapter:

```java
void applyReadScope(Criteria criteria, GenericCrudRequestContext context);
void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context);
```

Scope dapat membatasi perguruan tinggi, yayasan, sekolah, fakultas, program studi, satuan kerja, toko, tenant, pemilik data, atau rule domain lain. Bila scope tidak dapat ditentukan, akses ditolak.

## 9. Query, filter, sort, dan paging

### 9.1 Paging database

Gunakan dua query:

```text
COUNT query  -> total rows berdasarkan filter dan scope
DATA query   -> setFirstResult(page * pageSize) + setMaxResults(pageSize)
```

Pilihan page size:

```text
5, 10, 25, 50, 100, 500, 1000
```

Default 10. Nilai request di luar allow-list ditolak atau dinormalisasi ke 10.

### 9.2 Deterministic ordering

Selalu tambahkan tie-breaker identifier:

```text
ORDER BY selected_property ASC|DESC, id ASC
```

Untuk `Tbmuser`/`Tbmrole` gunakan identifier property aktual dari Hibernate metadata.

### 9.3 Sort

Semua kolom tabel selain Foto dan Aksi harus sortable **jika** mempunyai mapped scalar property atau adapter sort expression yang tervalidasi. Kolom relation display di-sort melalui alias (`relation.nama`) yang sudah allow-listed. Jangan memasukkan nama kolom dari request langsung ke HQL/SQL.

### 9.4 Filter cepat dan lanjutan

- Quick filter menampilkan maksimal 3–6 field common.
- Jika field searchable lebih dari 6, tampilkan tombol **Filter Lanjutan**.
- Filter lanjutan berupa dialog/popup, tetapi kondisi aktif selalu ditampilkan kembali sebagai chips/label di halaman utama.
- Setiap chip dapat dihapus sendiri; tersedia Reset Semua.
- Operator berdasarkan tipe:
  - String: contains, equals, startsWith, endsWith, empty/not empty.
  - Number: equals, greater/less, between.
  - Date: exact day, before/after, between.
  - Boolean: semua/ya/tidak.
  - Enum/status: multi-select.
  - Relation: equals relation ID melalui Combo/Lookup.
- Simpan state filter ke URL/query state dan Saved View; jangan simpan entity/proxy di session.

## 10. Relation lookup: Combo vs Bandbox

Aturan wajib:

1. Jalankan `COUNT` pada relation target dengan scope dan filter aktif.
2. Jika total `<= 20`, gunakan Combobox dan muat maksimal 20.
3. Jika total `> 20`, gunakan Bandbox/Lookup Dialog dengan server paging.
4. Lookup menampilkan field common yang benar-benar mapped: `kode`, `nama`, `nim`, `nis`, `nip`, `nik`, `noRegistrasi`, `nomorUjian`, dan fallback ID.
5. Sebelum membuat Criteria, cek field melalui runtime metadata. Field yang tidak ada dilewati, bukan dipaksakan.
6. Nested relation filter mengikuti aturan sama, tetapi maksimal kedalaman 2 dan memakai cycle guard agar tidak terjadi rekursi tanpa batas.
7. Lookup wajib menerapkan READ + row scope target entity.
8. Hanya ID/natural key yang dikirim ke form; entity tidak diserialisasi penuh.

## 11. Form Tambah/Ubah

- Desktop: drawer lebar atau modal 70–85% viewport; form 2–3 kolom sesuai lebar.
- Tablet: 2 kolom.
- Mobile: 1 kolom full-screen sheet.
- Field dikelompokkan dalam section: Identitas, Klasifikasi, Relasi, Status, Dokumen/Foto, Audit.
- Sticky footer: Batal, Simpan Draft (jika didukung), Simpan, Kirim Approval.
- Tampilkan summary error di atas dan error inline di field.
- Jangan menghapus nilai input ketika validasi gagal.
- Peringatan unsaved changes saat pengguna menutup form.
- Preview perubahan untuk field sensitif/operasi besar.
- Gunakan DTO/allow-list field; jangan bind request langsung ke entity.
- Untuk edit, load ulang entity di transaction aktif dan hanya set property yang diizinkan.
- Terapkan optimistic locking bila entity punya `@Version`; jika tidak, gunakan `tanggal_dirubah`/hash snapshot sebagai conflict guard bila adapter mengizinkan.

## 12. Foto

Foto tidak boleh diperlakukan sebagai byte[] generik. Gunakan `GenericCrudPhotoAdapter` yang menghubungkan mekanisme existing.

Entitas wajib adapter awal:

```text
Mahasiswa, Dosen, Siswa, Guru, Pegawai, Tbmuser
```

Tambahkan entitas lain berdasarkan metadata/config.

Fitur UI:

- preview foto lama dan baru
- drag/drop atau pilih file
- validasi JPG/JPEG/PNG/WebP sesuai dukungan server
- ukuran maksimum terkonfigurasi
- cek signature file, bukan hanya ekstensi/content-type
- crop rasio pas foto, rotate, dan kompres
- tombol hapus/kembalikan foto jika business rule mengizinkan
- perubahan foto masuk audit

## 13. Excel XLSX

### 13.1 Download

- Menggunakan filter, sort, scope, dan pilihan kolom yang sama dengan layar.
- Mengunduh semua baris yang cocok, bukan hanya current page.
- Header dua baris: label pengguna + property key stabil.
- Sheet `Data`, `Petunjuk`, `Referensi`, dan `Metadata`.
- Data validation/dropdown untuk relation kecil.
- Relation besar memakai ID/kode stabil dan sheet referensi terbatas.
- Export besar diproses streaming/job agar tidak menghabiskan heap.

### 13.2 Upload

Wizard:

1. Unduh template.
2. Pilih file XLSX.
3. Validasi file dan header.
4. Dry-run dan preview Create/Update/Delete/Skip/Error.
5. Konfirmasi.
6. Eksekusi chunk transaction.
7. Tampilkan ringkasan dan file error XLSX.

Kolom operasi:

```text
id | natural_key | delete | ...editable fields...
```

Aturan `delete=true`:

- wajib DELETE privilege
- wajib identifier yang tidak ambigu
- tidak boleh delete hanya berdasarkan nama bebas
- adapter dapat menolak hard delete dan mengubah menjadi soft delete
- tampilkan daftar relasi yang menghalangi delete
- audit setiap baris

Import button hanya tampil apabila pengguna mempunyai CREATE + UPDATE + DELETE. Meskipun demikian, dry-run mengklasifikasikan row dan menolak file jika privilege operasi aktual tidak cukup.

Keamanan XLSX:

- hanya `.xlsx`
- batas ukuran, jumlah sheet, jumlah row, jumlah cell, panjang string, formula, dan compression ratio
- formula tidak dievaluasi sebagai instruksi; nilai formula diperlakukan sesuai kebijakan aman
- cegah ZIP bomb/XXE sesuai kemampuan versi POI
- file disimpan dengan nama server-generated di temp non-public
- hapus file temp pada finally/job cleanup

## 14. PDF, DOCX, PPTX

Semua format memakai satu `GenericCrudReportData` agar hasil konsisten.

- PDF: JasperReports landscape/portrait otomatis berdasarkan jumlah kolom.
- DOCX: Jasper `JRDocxExporter` atau `WordExporter` existing setelah diverifikasi dependency.
- PPTX: Jasper `JRPptxExporter`; gunakan halaman 16:9 dan paginate agar teks tidak keluar slide.
- Judul laporan, institusi, pengguna pencetak, timestamp, filter aktif, jumlah data, dan klasifikasi data wajib tampil.
- Kolom sensitif dimasking sesuai privilege/metadata.
- Ekspor mengikuti filter yang sedang aktif, bukan current page.
- Untuk data sangat besar, jadwalkan export job dan beri notifikasi saat selesai.

## 15. Column chooser dan preferensi pengguna

Fitur:

- show/hide kolom
- drag reorder
- resize width
- pin left/right bila UI mendukung
- reset ke default sistem
- simpan sebagai view pribadi
- pilih default view
- page size, density, sort, dan filter tersimpan

Kunci preferensi:

```text
userId + activeRoleId + entityKey + viewName
```

Jangan hanya menggunakan userId karena role aktif dapat mempunyai kebutuhan dan privilege field berbeda.

## 16. Custom/override actions

Interface:

```java
public interface GenericCrudCustomActionProvider {
    List getToolbarActions(GenericCrudRequestContext context);
    List getRowActions(GenericCrudRequestContext context, GeneralValueObject row);
    List getBulkActions(GenericCrudRequestContext context, List selectedIds);
    GenericCrudResult execute(String actionKey, GenericCrudRequestContext context,
                              List selectedIds, Map parameters) throws Exception;
}
```

Setiap action mendefinisikan:

```text
key, label, icon, placement, requiredPrivilege, selectionMode,
confirmationMode, asynchronous, visibilityPredicate, parameterSchema
```

Aksi existing Mahasiswa seperti Download/Upload Password, kirim/ambil Feeder, dan aksi domain lain harus didaftarkan eksplisit pada `MahasiswaGenericCrudAdapter`. Jangan otomatis mengekspos semua method `on*` melalui reflection.

## 17. Approval/Reject

Approval hanya aktif jika:

- entity config menyatakan approval supported; dan
- adapter menyediakan state mapping serta transition; dan
- pengguna memiliki APPROVE/REJECT.

Adapter minimal:

```java
String getState(T entity);
boolean canSubmit(T entity, context);
boolean canApprove(T entity, context);
boolean canReject(T entity, context);
void submit(T entity, context, String note);
void approve(T entity, context, String note);
void reject(T entity, context, String note);
```

Jangan mengasumsikan nama field `status`, `disetujui`, atau `ditolak` sama untuk semua entity.

## 18. Audit dan revisi

Setiap operasi mencatat:

- request ID
- pengguna dan role aktif
- menu dan privilege
- entity key dan object ID
- operasi
- before/after yang dimasking
- sumber: UI, import, custom action, API
- IP/user-agent bila mekanisme existing menyediakannya
- waktu dan hasil

Gunakan `CommonPrivilages.saveActivity`, `AuditTrailHelper`, `RevisiHelper`, dan `ErrorAuditUtil` existing; jangan membuat audit paralel tanpa kebutuhan.

## 19. Transaction dan session Hibernate

- Query list/detail memakai session read-only bila memungkinkan.
- Mutation mempunyai transaction eksplisit.
- Import memakai chunk transaction terkonfigurasi; kegagalan row ditangani sesuai mode atomic/all-or-nothing atau partial dengan laporan jelas.
- `openSession/currentNativeSession` ditutup di `finally`; `currentSession()` tidak ditutup bila dikelola framework existing.
- Jangan menyimpan proxy/entity Hibernate ke HTTP session.
- Hindari lazy access setelah session ditutup; gunakan projection/DTO untuk list dan export.

## 20. Endpoint contract

Contoh:

```text
GET  ..._service.jsp?action=metadata
GET  ..._service.jsp?action=list&page=0&pageSize=10&sort=nama&direction=asc
GET  ..._service.jsp?action=detail&id=123
GET  ..._service.jsp?action=lookup&field=jurusan&q=informatika&page=0
GET  ..._service.jsp?action=export&format=xlsx&viewToken=...
POST ..._service.jsp?action=create
POST ..._service.jsp?action=update&id=123
POST ..._service.jsp?action=delete&id=123
POST ..._service.jsp?action=approve&id=123
POST ..._service.jsp?action=reject&id=123
POST ..._service.jsp?action=importDryRun
POST ..._service.jsp?action=importCommit&jobId=...
POST ..._service.jsp?action=custom&customAction=sendToFeeder
```

Mutation wajib POST, CSRF, privilege, scope, allow-list field, validation, transaction, dan audit.

## 21. Default lifecycle entitas

Karena seluruh entitas tidak sama aman untuk CRUD, gunakan status:

```text
EXCLUDED_ABSTRACT
REVIEW_NOT_ENTITY
REVIEW_REQUIRED
ELIGIBLE_METADATA_FIRST
ELIGIBLE_PARITY_FIRST
ENABLED_READ_ONLY
ENABLED_FULL
DISABLED
```

Class dengan nama/jenis log, request/response bank, file content, cache, temporary, report, audit, atau integration default `REVIEW_REQUIRED` atau read-only. Tidak boleh otomatis full CRUD.




# Amandemen Arsitektur V2.1 — Audit/Restore dan Form Kompleks

## 22. AuditRevision layer

Tambahkan layer:

```text
GenericCrudAuditRevisionService
 ├── GenericCrudAuditRevisionAdapter
 ├── GenericCrudRestorePolicy
 ├── Hibernate Envers / GenericRevisiHelper bridge
 ├── GenericCrudAuditMaskingService
 └── GenericCrudRestoreJobService
```

Audit list global/per-row wajib memakai server-side paging dan scope. Historical record bersifat immutable. Restore/koreksi selalu mengubah data aktif dan menghasilkan revisi baru.

## 23. Restore transaction model

- restore field/revision tunggal: satu transaksi atomik;
- deep restore: satu transaksi per target dengan cycle/depth guard;
- mass restore: dry-run, kemudian job; transaksi per item, progress dan error per item;
- restore deleted record: CREATE+UPDATE, identifier/dependency validation;
- rollback pada target yang gagal;
- adapter hook `afterRestoreInTransaction` atau equivalent untuk domain correction.

## 24. PermanentDelete layer

```text
GenericCrudPermanentDeleteService
 └── GenericCrudPermanentDeletePolicy (explicit per entity)
```

Guard:

```text
Common.getApakahAdmin()
+ DELETE privilege
+ binding/menu authorization
+ row scope
+ entity policy enabled
+ reason/typed confirmation
+ FK/domain/optimistic preflight
```

Operasi hanya menghapus row aktif. Envers dan audit history tidak dipurge.

## 25. Complex Form layer

```text
GenericCrudFormService
 ├── GenericCrudFormOverrideProvider
 ├── GenericCrudFormDefinitionRegistry
 ├── GenericCrudFormDefinition
 ├── GenericCrudFormTabDefinition
 ├── GenericCrudFormSectionDefinition
 └── GenericCrudValidationCoordinator
```

Mode: generic drawer/modal, tabbed drawer, full-page tabs, wizard, custom component, atau legacy bridge. Provider dipilih server-side melalui allow-list. Mahasiswa memakai `ROOT_FIRST` dan conditional/lazy/save-before-enter tabs sesuai Action existing.

## 26. Data/config tambahan

Jalankan dan review:

```text
sql/003_generic_crud_audit_restore_form_override.sql
```

Tabel/kolom baru mengatur audit/restore policy, form mode/provider, form tabs/sections, restore jobs/items, serta revision metadata. Semua restore/admin-delete default disabled sampai review entity selesai.
