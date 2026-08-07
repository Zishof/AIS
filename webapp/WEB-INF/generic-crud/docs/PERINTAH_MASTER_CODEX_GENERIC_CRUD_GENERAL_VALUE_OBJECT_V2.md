# PERINTAH MASTER CODEX / CLAUDE CODE
## Implementasi Master CRUD Generik untuk seluruh turunan `GeneralValueObject` pada AIS
### V2.1 — termasuk Audit Trail, Restore, Hapus Data Aktif oleh Super Admin, dan Complex Form Override

> **Dokumen ini menggantikan versi sebelumnya. Requirement 13 dan 14 bersifat wajib, bukan fitur opsional.**

> Jalankan perintah ini dari **checkout Git terbaru** repository AIS. Jangan menganggap manifest snapshot dalam paket ini sebagai sumber kebenaran terakhir. Source, Hibernate mapping, role/menu, dan helper pada checkout aktif adalah sumber kebenaran.

---

## A. Tujuan akhir

Bangun satu **Generic CRUD Engine enterprise, metadata-driven, deny-by-default, role-aware, scope-aware, server-side paginated**, yang dapat dipakai oleh seluruh class konkret yang merupakan turunan langsung maupun tidak langsung dari:

```java
ais.database.model.GeneralValueObject
```

Nama class aktual pada source adalah `GeneralValueObject`, bukan `GenericValueObject`.

Engine harus melayani UI JSP modern di:

```text
webapp/WEB-INF/new/{MODUL}/uiux/*.jsp
webapp/WEB-INF/new/{MODUL}/services/*.jsp
```

serta dapat dipakai sebagai backend/adaptor untuk ZUL lama selama masa migrasi.

Jangan membuat 1.500 implementasi CRUD yang menyalin logic. Buat satu engine bersama, metadata per entitas, alias JSP per halaman, dan adapter khusus untuk business rule yang tidak generik.

---

## B. Ketentuan lingkungan yang tidak boleh dilanggar

1. Pertahankan kompatibilitas Java 1.7 dan gaya source 1.6.
2. Jangan memakai lambda, stream, record, Optional, try-with-resources apabila source target belum mengizinkannya, pattern matching, text block, atau library Java modern tanpa persetujuan.
3. Gunakan Hibernate 3.6 dan pola session existing:
   - session yang dibuka dengan `openSession()` harus ditutup di `finally`;
   - session managed/current tidak boleh ditutup manual;
   - transaksi mutasi harus commit/rollback eksplisit.
4. Pertahankan Spring 3.1 dan Ant build existing.
5. JSP harus kompatibel dengan Jasper legacy:
   - jangan memakai `#{...}` dalam template text;
   - JSP service hanya adapter tipis;
   - business logic, Hibernate query, privilege, audit, import/export, dan validasi berada di Java.
6. Jangan mengubah mapping Hibernate, tabel bisnis, atau Action/ZUL lama secara massal tanpa parity test.
7. Jangan menggunakan CDN untuk komponen inti New UI.
8. Jangan menyimpan password/token/secret dalam log, export, audit payload, atau response browser.

---

## C. Langkah awal wajib

### C.1 Perbarui Git dan buat branch fitur

```bash
git checkout master
git pull --ff-only origin master
git checkout -b feat/generic-crud-general-value-object
```

Jangan bekerja langsung di `master`.

### C.2 Inventaris source terbaru

Jalankan scanner yang disediakan:

```bash
python tools/scan_general_value_objects.py \
  --project-root . \
  --output-dir build/generic-crud-manifest
```

Kemudian verifikasi manual minimal class berikut:

```text
src/ais/database/model/GeneralValueObject.java
src/ais/action/master/generic/GenericCrudAction.java
src/ais/action/master/AgamaAction.java
src/ais/action/master/helper/GenericRevisiHelper.java
src/ais/action/master/helper/RevisiHelper.java
src/ais/action/master/RevisiAction.java
src/ais/common/CommonPrivilages.java
src/ais/database/model/RolePrivilage.java
src/ais/action/master/MahasiswaAction.java
src/ais/action/master/SiswaAction.java
src/ais/action/master/DosenAction.java
src/ais/action/master/GuruAction.java
src/ais/action/master/PegawaiAction.java
src/ais/action/maintenance/TbmuserAction.java
src/ais/action/maintenance/TbmroleAction.java
```

Cari lokasi terbaru apabila package berubah.

### C.3 Buat laporan sebelum coding

Tambahkan ke `docs/generic-crud/00-source-audit.md`:

- jumlah subclass `GeneralValueObject`;
- jumlah concrete `@Entity`;
- class abstract / mapped superclass;
- entity yang mempunyai Action existing;
- entity yang mempunyai foto;
- entity dengan custom action;
- entity transaksi/log/integrasi yang tidak layak full CRUD;
- mapping menu dan privilege existing;
- helper download/upload/report/foto/audit existing;
- perbedaan source terbaru dengan manifest paket.

Tidak boleh mengaktifkan CRUD otomatis hanya berdasarkan `extends GeneralValueObject`.

---

## D. Arsitektur target

Implementasikan package berikut, sesuaikan bila source terbaru memiliki konvensi helper yang lebih tepat:

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
├── GenericCrudAuditRevisionService.java
├── GenericCrudRestoreService.java
├── GenericCrudRestoreJobService.java
├── GenericCrudPermanentDeleteService.java
├── GenericCrudFormService.java
├── GenericCrudPhotoService.java
├── GenericCrudCustomActionService.java
├── GenericCrudJobService.java
├── GenericCrudCsrf.java
├── GenericCrudJson.java
└── adapter/
    ├── GenericCrudEntityAdapter.java
    ├── AbstractGenericCrudEntityAdapter.java
    ├── GenericCrudScopeAdapter.java
    ├── GenericCrudPhotoAdapter.java
    ├── GenericCrudApprovalAdapter.java
    ├── GenericCrudImportAdapter.java
    ├── GenericCrudExportAdapter.java
    ├── GenericCrudCustomActionProvider.java
    ├── GenericCrudAuditRevisionAdapter.java
    ├── GenericCrudRestorePolicy.java
    ├── GenericCrudPermanentDeletePolicy.java
    ├── GenericCrudFormOverrideProvider.java
    ├── GenericCrudFormDefinition.java
    ├── AgamaGenericCrudAdapter.java
    ├── MahasiswaGenericCrudAdapter.java
    ├── SiswaGenericCrudAdapter.java
    ├── DosenGenericCrudAdapter.java
    ├── GuruGenericCrudAdapter.java
    ├── PegawaiGenericCrudAdapter.java
    └── TbmuserGenericCrudAdapter.java
```

JSP shared:

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
│   ├── saved_view_dialog.jsp
│   ├── audit_drawer.jsp
│   ├── audit_global_page.jsp
│   ├── revision_compare.jsp
│   ├── restore_form.jsp
│   ├── restore_job_dialog.jsp
│   ├── admin_delete_dialog.jsp
│   ├── complex_form_shell.jsp
│   ├── complex_form_tabs.jsp
│   ├── complex_form_section.jsp
│   ├── approval_dialog.jsp
│   └── error_summary.jsp
├── services/
│   └── dispatcher.jsp
└── assets/
    ├── generic-crud.css
    └── generic-crud.js
```

Alias per entity:

```text
webapp/WEB-INF/new/{module}/uiux/{entity_slug}.jsp
webapp/WEB-INF/new/{module}/services/{entity_slug}_service.jsp
```

Alias UI hanya mengatur `entityKey`, `moduleKey`, `pageKey`, dan binding context, kemudian meng-include shared renderer dengan `pageContext.include(..., true)`. Alias service harus `forward()` ke dispatcher. Jangan menggandakan query/HTML inti. Satu entity boleh memiliki beberapa `generic_crud_page_binding` apabila dipakai oleh beberapa menu/action; privilege harus memakai `menuId` binding aktif, bukan satu menu global yang ditebak.

---

## E. Registry dan metadata: sumber kebenaran

### E.1 Urutan sumber metadata

1. Hibernate runtime metadata (`SessionFactory.getAllClassMetadata()` / `ClassMetadata`).
2. Tabel konfigurasi Generic CRUD.
3. Adapter Java per entity.
4. Reflection/anotasi source hanya untuk inventaris awal.

### E.2 Default deny

Setiap entity baru mempunyai status awal:

```text
DISABLED
REVIEW_REQUIRED
READ_ONLY
FULL_CRUD
```

Default adalah `DISABLED` atau `REVIEW_REQUIRED`. Aktivasi hanya dilakukan setelah:

- Hibernate class metadata terverifikasi;
- identifier property diketahui;
- mapped fields terverifikasi;
- menu dan privilege terhubung;
- scope data terdefinisi;
- field sensitif dikeluarkan;
- validation/adaptor tersedia;
- parity test selesai bila ada Action lama.

### E.3 Field yang tidak boleh otomatis diekspos

- collection (`Set`, `List`, `Map`, one-to-many);
- blob/byte array/file content;
- password, PIN, token, secret, session key, API key;
- audit internal dan technical fields yang tidak relevan;
- transient getter yang tidak mapped;
- formula yang tidak mempunyai display adapter;
- relationship tanpa scope dan lookup definition;
- field yang tidak ada di Hibernate metadata.

Gunakan allow-list field, bukan block-list saja.

---

## F. Hak akses dan data scope

### F.1 Mapping privilege

Ikuti nilai dan implementasi existing pada `CommonPrivilages` dan `RolePrivilage`:

```text
READ     -> list, detail, lookup, download/export, print
CREATE   -> add, create import row
UPDATE   -> edit, photo update, update import row
DELETE   -> delete, mass delete, delete=true import row
APPROVE  -> approve apabila entity mendukung approval
REJECT   -> reject apabila entity mendukung approval
```

Tombol upload/import XLSX hanya ditampilkan ketika role aktif mempunyai:

```text
CREATE && UPDATE && DELETE
```

Akan tetapi setiap row import wajib diperiksa ulang berdasarkan operasi aktual.

### F.2 Role aktif

Gunakan mekanisme role aktif existing (`Tbmuser.hakAkses()` atau padanan terbaru). Jangan menggabungkan hak akses seluruh role tambahan.

### F.3 Pemeriksaan pada setiap request

Privilege harus dicek server-side untuk:

- route halaman;
- daftar/detail;
- lookup relation;
- add/edit/delete;
- import/export/report;
- foto;
- approval/reject;
- custom action;
- job status/download hasil job;
- deep-link langsung.

Tidak cukup menyembunyikan tombol di browser.

### F.4 Row-level scope

Buat adapter:

```java
public interface GenericCrudScopeAdapter {
    void applyReadScope(Criteria criteria, GenericCrudRequestContext context);
    void applyCountScope(Criteria criteria, GenericCrudRequestContext context);
    void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context);
}
```

Scope dapat meliputi tenant, perguruan tinggi, yayasan, sekolah, fakultas, program studi, satuan kerja, toko, pemilik data, atau rule domain lain. Bila scope tidak dapat ditentukan, deny.

---

## G. List, filter, sort, paging

### G.1 Server-side paging

Default 10 data. Pilihan yang diperbolehkan:

```text
5, 10, 25, 50, 100, 500, 1000
```

Gunakan query database:

```java
criteria.setFirstResult(offset);
criteria.setMaxResults(pageSize);
```

Buat count query terpisah dengan filter dan scope identik. Dilarang load seluruh data ke `List` lalu dipaging di memori.

Untuk halaman sangat dalam, sediakan mode hybrid/keyset berdasarkan identifier dan sort stabil tanpa mengubah UX nomor halaman untuk pemakaian normal.

### G.2 Sort database

Semua kolom kecuali `Foto` dan `Aksi` harus sortable apabila:

- mapped scalar property tervalidasi; atau
- relation display path mempunyai alias dan sort expression allow-listed; atau
- adapter menyediakan safe sort expression.

Request hanya mengirim `fieldKey`, bukan nama SQL/HQL mentah. Tambahkan tie-breaker identifier agar hasil stabil:

```text
ORDER BY selectedField ASC|DESC, id ASC
```

### G.3 Quick filter

- Tampilkan 3–6 field common paling relevan.
- Search universal hanya mencari field yang didefinisikan sebagai searchable.
- Gunakan debounce 300–500 ms.
- Terapkan tombol Cari untuk query mahal.
- Pertahankan nilai filter setelah paging, sort, export, dan kembali dari detail.

### G.4 Filter lanjutan

Jika entity mempunyai lebih dari 6 field filterable, tampilkan tombol **Filter Lanjutan** yang membuka dialog/popup. Setelah diterapkan:

- dialog boleh ditutup;
- seluruh filter aktif tetap terlihat sebagai chip/label;
- chip dapat dihapus satu per satu;
- tersedia Reset Semua;
- filter dapat disimpan sebagai Saved View;
- state dapat diserialisasi ke URL secara aman tanpa data sensitif.

### G.5 Operator filter

Minimal:

```text
String     : contains, startsWith, equals, notEquals, empty, notEmpty
Number     : equals, notEquals, greaterThan, greaterOrEqual, lessThan, lessOrEqual, between
Date       : on, before, after, between, empty, notEmpty
Boolean    : all, true, false
Enum/status: in, notIn
Relation   : equals id, in ids, empty, notEmpty
```

Semua operator dan field harus allow-listed.

---

## H. Relasi: combo vs searchable bandbox

### H.1 Aturan threshold

Sebelum form/filter dirender, hitung jumlah kandidat relation setelah scope:

```text
<= 20 data -> combobox/select biasa
> 20 data  -> searchable bandbox/lookup dialog
```

Threshold default 20 dan dapat dioverride per field.

### H.2 Search field relation

Gunakan field common yang **benar-benar ada di Hibernate metadata**, misalnya:

```text
kode, nama, namaLengkap, nim, nis, nip, nik,
noRegistrasi, nomorRegistrasi, nomorUjian, status, aktif
```

Jangan membuat Criteria terhadap property hanya karena namanya umum. Verifikasi dahulu.

### H.3 Bandbox server-side

Lookup harus mempunyai:

- query database paging default 10;
- quick search;
- advanced filters bila field >6;
- sort database;
- kolom yang relevan;
- selected value preview;
- clear selection;
- scope dan READ permission pada entity relation;
- request cancellation/debounce;
- loading, empty, dan error state.

### H.4 Nested relation

Jika filter di lookup relation kembali mempunyai relation >20, terapkan aturan sama tetapi:

- maksimum kedalaman default 2;
- deteksi siklus A → B → A;
- tampilkan breadcrumb lookup;
- jangan membuka dialog tak terbatas;
- adapter dapat menyederhanakan relation kompleks.

---

## I. Form tambah/edit modern

### I.1 Layout

- Desktop: drawer kanan lebar 720–960 px atau modal besar; form 2–3 kolom sesuai lebar.
- Tablet: 2 kolom.
- Mobile: 1 kolom, full-screen sheet.
- Field dikelompokkan ke section: Identitas, Organisasi, Akademik, Status, Kontak, Dokumen, Audit, dan section domain lain.
- Sticky footer: Batal, Simpan Draft bila ada, Simpan, Simpan & Tambah Lagi, Kirim Approval bila ada.
- Required marker dan help text jelas.
- Error summary di atas, error inline di field, fokus ke error pertama.
- Nilai yang sudah valid tidak hilang ketika validasi gagal.
- Peringatan unsaved changes.
- Optimistic concurrency melalui version/lastUpdate property bila tersedia.

### I.2 Binding aman

Jangan melakukan mass binding request langsung ke entity. Buat DTO/command allow-list berdasarkan definition. Setiap field diperiksa:

- privilege field;
- create/update editability;
- type conversion;
- length/range;
- domain validator;
- scope relation;
- uniqueness;
- optimistic lock.

### I.3 Validation

Gabungkan:

1. metadata constraints;
2. validator adapter;
3. business validation existing dari Action/helper lama;
4. database uniqueness check;
5. relation scope validation.

Jangan menghapus validasi existing saat memigrasikan entity yang sudah mempunyai Action.


### I.4 Custom/override form untuk entity kompleks

Form generik wajib dapat dioverride per entity atau per `generic_crud_page_binding`. Sediakan:

```text
GenericCrudFormOverrideProvider
GenericCrudFormDefinition
GenericCrudFormService
```

Mode minimum:

```text
GENERIC_DRAWER
GENERIC_MODAL
TABBED_DRAWER
FULL_PAGE_TABS
WIZARD
CUSTOM_COMPONENT
LEGACY_BRIDGE
```

Ketentuan:

- provider dipilih dari registry server-side; browser tidak boleh menentukan class/JSP arbitrary;
- tab/section/field mempunyai order, visibility rule, privilege, lazy-load, validation group, dan save strategy;
- tab dapat `requiresPersistedEntity` dan `saveBeforeEnter`;
- conditional tab harus mengikuti business state;
- error summary lintas tab harus memindahkan fokus ke tab dan field yang error;
- seluruh aturan relation combo/bandbox, foto, scope, field allow-list, audit, dan optimistic lock tetap berlaku;
- form kompleks boleh reuse helper/service existing selama migrasi, tetapi JSP tidak boleh menampung business logic/Hibernate.

Buat `MahasiswaGenericCrudAdapter` sebagai golden complex-form pilot. Minimal pertahankan padanan tab source terbaru:

```text
Data Mahasiswa
Keterangan Mahasiswa Pindahan
Keterangan Mahasiswa Alih Prodi
Biodata Lengkap
Beasiswa
Cuti
Informasi Kelulusan
Informasi Alumni
Login Orang Tua
Audit & Riwayat
```

Pindahan/Alih Prodi bersifat conditional. Biodata/Beasiswa/Cuti/Alumni memakai lazy load dan save-before-enter bila root belum mempunyai ID. Jangan aktifkan form Mahasiswa sebelum parity matrix seluruh tab, field, action, validasi, dan efek bisnis lulus.

---

## J. Foto

Buat `GenericCrudPhotoAdapter` untuk entity yang benar-benar mempunyai mekanisme foto. Minimal adaptor khusus:

```text
Mahasiswa, Dosen, Siswa, Guru, Pegawai, Tbmuser
```

Tambahkan entity lain hanya setelah sumber foto/storage existing terverifikasi.

UI:

- preview;
- upload/drag-drop;
- kamera pada perangkat yang mendukung;
- crop rasio yang sesuai;
- rotate;
- reset/hapus bila diizinkan;
- ukuran dan tipe file;
- fallback avatar.

Backend:

- reuse helper/storage existing dari `MahasiswaAction`, `SiswaAction`, dan class terkait;
- validasi magic bytes/signature, bukan extension saja;
- batas ukuran;
- nama file generated;
- tidak menaruh upload di lokasi executable;
- privilege UPDATE;
- audit before/after metadata, bukan binary foto;
- hapus file sementara di `finally`.

---

## K. Excel XLSX download/upload

### K.1 Download template/data

Download harus mengikuti:

- filter aktif;
- scope aktif;
- sort aktif;
- kolom yang dipilih user bila opsi tersebut dipilih;
- field exportable dan tidak sensitif;
- locale/tanggal/number format konsisten;
- sheet Metadata/Instructions;
- reference sheet untuk relation kecil bila relevan;
- request ID dan timestamp.

Untuk dataset besar, gunakan streaming workbook existing yang kompatibel atau mekanisme POI streaming yang tersedia. Jangan menahan seluruh workbook/data besar di memori.

### K.2 Import wizard

Tahapan wajib:

1. Download Template.
2. Pilih file XLSX.
3. Upload ke temporary storage aman.
4. Validasi workbook, sheet, header, tipe, ukuran, dan formula.
5. Dry-run tanpa mutation.
6. Preview hasil per row: CREATE, UPDATE, DELETE, SKIP, ERROR.
7. Ringkasan jumlah dan warning.
8. Konfirmasi eksplisit.
9. Eksekusi chunked transaction.
10. Laporan hasil dan error workbook.

### K.3 Identifikasi row

Urutan kunci:

1. identifier aman bila template internal;
2. natural key yang didefinisikan adapter;
3. kombinasi unique fields yang eksplisit.

Dilarang menebak berdasarkan field non-unique.

### K.4 `delete=true`

Baris hanya boleh menghapus bila:

- upload user mempunyai DELETE;
- entity mengizinkan import delete;
- identifier/natural key unambiguous;
- row berada dalam data scope;
- dependency check lulus;
- dry-run memperlihatkan objek yang akan dihapus;
- user melakukan konfirmasi kedua;
- audit tersimpan;
- soft delete dipakai bila domain mendukung.

### K.5 Operasi campuran dan privilege

Walaupun tombol Upload mensyaratkan CREATE+UPDATE+DELETE, engine tetap memeriksa setiap row. Bila privilege berubah saat job berjalan, hentikan dan tandai job gagal/partial secara aman.

### K.6 Idempotensi

Simpan hash file + entity + user + role + mode. Cegah file yang sama dieksekusi dua kali tanpa override eksplisit.

---

## L. Export PDF, DOCX, PPTX

Sediakan dialog **Cetak / Export Dokumen** dengan:

```text
PDF
Word (DOCX)
Presentasi (PPTX)
Excel (XLSX)
```

Semua format harus mengikuti filter, scope, dan sort aktif.

### L.1 PDF

Gunakan JasperReports/report helper existing. Tampilkan:

- judul entitas;
- institusi;
- tanggal cetak;
- nama user/role;
- ringkasan filter;
- tabel;
- nomor halaman;
- total record;
- request ID.

### L.2 DOCX

Gunakan exporter Jasper DOCX atau helper XWPF existing. Pastikan tabel dapat dibaca dan orientasi landscape bila kolom banyak.

### L.3 PPTX

Gunakan exporter Jasper PPTX atau XSLF existing. Jangan memindahkan 1.000 row ke satu slide. Gunakan:

- cover;
- ringkasan filter/KPI;
- beberapa slide tabel dengan batas row;
- chart/ringkasan bila adapter menyediakan;
- lampiran atau referensi export XLSX untuk data penuh.

### L.4 Job asinkron

Export besar harus dijalankan sebagai job dengan status:

```text
QUEUED, RUNNING, COMPLETED, FAILED, EXPIRED, CANCELED
```

User hanya boleh mengunduh hasil job miliknya atau sesuai privilege admin.

---

## M. Column chooser dan preferensi user

### M.1 Kolom default

Pilih 6–8 kolom common paling relevan berdasarkan metadata dan ranking konfigurasi. Foto dan Aksi tidak sortable.

### M.2 Column chooser

User dapat:

- show/hide;
- reorder drag-drop;
- resize;
- pin kiri/kanan;
- reset default;
- memilih density compact/comfortable;
- menyimpan page size;
- menyimpan default sort.

### M.3 Persistensi

Simpan per:

```text
userId + activeRoleId + entityKey + viewName
```

Jangan hanya localStorage. Konfigurasi minimal:

```text
visibleColumns, columnOrder, columnWidths, pinnedColumns,
pageSize, density, sort, savedFilters, version
```

Sediakan migration/versioning saat field berubah. Abaikan field yang tidak lagi ada tanpa error.

---

## N. Custom/override action

Jangan mengekspos semua method `on*` melalui reflection. Buat provider explicit allow-list:

```java
public interface GenericCrudCustomActionProvider {
    List getActions(GenericCrudDefinition definition,
                    GenericCrudRequestContext context);
    GenericCrudResult execute(String actionKey,
                              List selectedIds,
                              Map parameters,
                              GenericCrudRequestContext context);
}
```

Setiap action mendefinisikan:

```text
actionKey, label, icon, placement(toolbar/row/detail/bulk),
requiredPrivilege, selectionMode(none/single/multiple/all-filtered),
confirmation, danger, async, parameter schema, handler key
```

### N.1 Mahasiswa parity

`MahasiswaGenericCrudAdapter` harus memetakan secara eksplisit action existing yang masih relevan, misalnya:

- Download Password;
- Upload Password;
- Upload/Download RFID;
- Kirim/sinkron ke Feeder;
- Ambil nilai/data dari Feeder;
- Download/Upload Foto Massal;
- Kartu Mahasiswa;
- KHS/Transkrip/Prestasi;
- Surat Mahasiswa;
- laporan/rekap existing.

Nama dan business logic aktual harus diambil dari source terbaru. Jangan membuat implementasi dummy.

### N.2 All-filtered selection

Untuk aksi massal “semua hasil filter”, jangan mengirim semua ID ke browser. Buat selection token server-side yang mengikat:

```text
entity, user, role, filter hash, scope hash, expiry, excluded IDs
```

---

## O. Approval dan reject

Jangan menganggap semua entity mempunyai field `status`. Gunakan `GenericCrudApprovalAdapter` per domain:

```java
boolean supportsApproval();
List getAvailableTransitions(...);
void submit(...);
void approve(...);
void reject(...);
```

Ketentuan:

- APPROVE/REJECT hanya muncul bila adapter mendukung;
- alasan reject wajib bila rule domain mengharuskan;
- state transition divalidasi server-side;
- approval tidak boleh melompati workflow existing;
- audit actor, role, waktu, status sebelum/sesudah, dan komentar;
- notification menggunakan helper existing bila tersedia.

---


## O.1 Audit Trail, Restore, dan Hapus Data Aktif oleh Super Admin — WAJIB

Implementasikan requirement 13 secara penuh dengan menjadikan `GenericRevisiHelper.java` dan `RevisiHelper.java` terbaru sebagai baseline paritas.

### O.1.1 Tampilan audit

Setiap Generic CRUD yang `auditEnabled` mempunyai:

1. **Audit Seluruh Data** — seluruh ID dalam class/entity, tetap mengikuti READ dan row-level scope;
2. **Riwayat ID Ini** — menu row/detail untuk satu record;
3. dashboard ringkas revisi;
4. filter tanggal, actor, role, revision type ADD/MOD/DEL, sumber, request ID, dan kolom berubah;
5. compare before/after;
6. paging/sort/filter langsung ke audit query/database.

### O.1.2 Restore

Sediakan operasi explicit:

```text
restoreField
manualCorrectField
restoreRevision
restoreDeepRevision
restoreDeletedRecord
previewRestoreLatestFromDate
restoreLatestFromDate
```

Historical audit row **tidak boleh diedit**. “Edit per kolom” berarti menerapkan nilai historis atau koreksi manual ke row aktif sehingga tercipta revisi baru. Sediakan form **Ubah & Restore** yang memuat field aman dari snapshot revisi, menampilkan diff, dan menyimpan perubahan sebagai revisi baru.

Privilege default:

```text
Lihat audit                 READ
Restore field/revisi aktif  READ + UPDATE
Restore record terhapus     READ + CREATE + UPDATE
Restore massal              READ + CREATE + UPDATE + Common.getApakahAdmin() + entity policy
```

Adapter dapat menambahkan APPROVE/REJECT atau pembatasan domain.

Restore terbaru mulai tanggal wajib mempunyai dry-run, preview target/dependency/conflict, confirmation, background progress, cancel, log, ringkasan, serta transaksi aman per item. Deep restore memakai Hibernate metadata, cycle guard, depth limit, deferred relation, dan rollback jika target gagal.

### O.1.3 Hapus Data Ini

Tindakan ini menghapus row aktif, **bukan** mem-purge riwayat audit. Default semua entity:

```text
adminDeleteEnabled = false
```

Tombol hanya tampil dan service hanya menjalankan apabila:

```text
Common.getApakahAdmin() == true
AND DELETE privilege == true
AND entity policy enabled
AND row berada dalam scope
AND typed confirmation benar
AND alasan wajib terisi
AND optimistic/FK/domain preflight lulus
```

Jangan menentukan Super Admin dari label role. Gunakan `Common.getApakahAdmin()` terbaru dan privilege binding aktif. Service wajib memeriksa ulang semua guard. Audit before/after, request ID, reason, actor, role, policy version, dan hasil operasi. Apabila FK/domain rule menolak, rollback.

Transaksi posted/ledger/pembayaran/dokumen final tidak boleh dihapus generik; gunakan cancellation/reversal melalui adapter domain.

### O.1.4 Referensi spesifikasi

Implementasikan seluruh detail pada:

```text
AUDIT_RESTORE_SUPERADMIN_DELETE_SPEC.md
sql/003_generic_crud_audit_restore_form_override.sql
```

---

## P. Fitur tambahan enterprise yang wajib dipertimbangkan

Implementasikan atau sediakan extension point untuk:

1. Saved View pribadi dan shared view sesuai privilege.
2. Bulk edit field yang diizinkan, dengan preview.
3. Soft delete, restore, dan recycle bin bila entity mendukung.
4. Audit trail serta compare before/after.
5. Optimistic concurrency dan pesan data sudah berubah.
6. Duplicate detection dan merge melalui adapter.
7. Data quality score/kelengkapan field.
8. Async import/export dan notifikasi selesai.
9. Shareable filter URL tanpa data sensitif.
10. Keyboard shortcut dan command palette.
11. Field-level masking dan export policy.
12. Query timeout, maximum export row, dan cost guard.
13. Stale data indicator / last refresh.
14. Retry aman untuk job idempotent.
15. Favorite/pin records bila relevan.
16. Context help, glossary field, dan link panduan.

Fitur berisiko harus default off sampai adapter/configuration menyatakan aman.

---

## Q. UI/UX dan responsivitas

Pertahankan shell New UI yang sudah ada, tetapi tambah design system Generic CRUD.

### Desktop

- Sidebar tetap.
- Page header + breadcrumb + title + description.
- Toolbar utama ringkas.
- Filter bar sticky ketika scroll.
- Tabel data utama.
- Detail drawer opsional.
- Pagination dan page-size di atas atau bawah sesuai ruang.

### Tablet

- Sidebar menjadi drawer.
- Kolom penting tetap tampil.
- Action dipindah ke overflow.
- Form 2 kolom.

### Mobile

- Tabel berubah menjadi card/list view, bukan horizontal-scroll sebagai satu-satunya solusi.
- Search dan filter menjadi sticky.
- Bottom action sheet untuk row action.
- Form full-screen satu kolom.
- Target sentuh minimal nyaman.
- Active filter chips tetap terlihat.
- Pagination menggunakan Sebelumnya/Berikutnya + ringkasan record.

### Accessibility

- Label eksplisit untuk semua input.
- Focus indicator jelas.
- Keyboard navigation.
- Dialog mempunyai focus trap dan Escape.
- Error summary mengarah ke field.
- Status tidak dibedakan hanya dengan warna.
- `aria-sort` pada header tabel.
- Kontras teks dan komponen memadai.
- Layout tidak kehilangan informasi pada lebar kecil.

---

## R. Endpoint contract

Gunakan satu dispatcher berdasarkan entityKey. Minimal:

```text
GET  action=meta
GET  action=list
GET  action=detail&id=...
GET  action=lookup&field=...
GET  action=column-preference
GET  action=saved-views
POST action=create
POST action=update
POST action=delete
POST action=bulk-delete
POST action=photo
POST action=approve
POST action=reject
POST action=custom
POST action=save-column-preference
POST action=save-view
POST action=import-upload
POST action=import-dry-run
POST action=import-confirm
POST action=export
GET  action=job-status&id=...
GET  action=job-download&id=...
GET  action=audit-summary
GET  action=audit-list&scope=GLOBAL|ROW
GET  action=audit-detail&revision=...
GET  action=audit-compare&leftRevision=...&rightRevision=...
GET  action=form-meta&id=...
GET  action=form-load-tab&id=...&tab=...
POST action=restore-field
POST action=manual-correct-field
POST action=restore-revision
POST action=restore-preview
POST action=restore-from-date
POST action=admin-delete-preview
POST action=admin-delete-active-row
POST action=form-save-root
POST action=form-save-tab
POST action=form-validate-tab
```

Mutasi wajib POST, CSRF, privilege, scope, validation, audit, dan request ID. Gunakan HTTP status tepat: 400, 401, 403, 404, 409, 413, 422, 429, 500.

Jangan meninggalkan endpoint produksi dengan `501 ADAPTER_NOT_IMPLEMENTED` sebagai tanda selesai.

---

## S. Database configuration

Terapkan SQL dalam paket setelah disesuaikan dengan convention project. Minimal tabel:

```text
generic_crud_entity_config
generic_crud_page_binding
generic_crud_field_config
generic_crud_user_view
generic_crud_saved_view
generic_crud_custom_action_config
generic_crud_import_job
generic_crud_import_row_error
generic_crud_export_job
generic_crud_idempotency
generic_crud_audit_event
generic_crud_restore_job
generic_crud_restore_job_item
generic_crud_form_definition
generic_crud_form_tab_config
generic_crud_form_section_config
```

Gunakan `text` untuk JSON agar kompatibel dengan mapping legacy bila diperlukan. Jangan membuat FK ke tabel existing sebelum identifier dan schema aktual dipastikan.

---

## T. Tahapan implementasi wajib

### Tahap 1 — Foundation

- runtime metadata registry;
- definition/field model;
- operation enum/constants;
- request context;
- privilege guard;
- scope guard;
- JSON/result/error contract;
- CSRF;
- config schema.

**Gate:** unit/component test registry, deny-by-default, invalid field rejection.

### Tahap 2 — Read-only list

- list/detail;
- DB paging/count;
- sort allow-list;
- quick/advanced filter;
- relation aliases;
- column chooser read-only;
- responsive table/mobile cards.

**Pilot:** `Agama`.

**Gate:** hasil list/filter/sort sesuai Action existing; no full-list paging.

### Tahap 3 — Create/update/delete

- safe DTO binding;
- validation;
- transaction;
- audit;
- optimistic concurrency;
- soft/hard delete policy;
- add/edit UI.

**Gate:** permission matrix dan rollback tests lulus.

### Tahap 4 — Relation lookup

- count threshold;
- combo/bandbox;
- nested lookup depth/cycle guard;
- scope;
- relation caching yang aman.

### Tahap 5 — Column preferences dan saved views

- user+active role persistensi;
- version migration;
- reset default;
- shared view policy.

### Tahap 6 — Excel import/export

- template;
- filtered export;
- dry-run;
- preview;
- create/update/delete;
- chunking;
- error XLSX;
- idempotency;
- job status.

### Tahap 7 — PDF/DOCX/PPTX

- filtered export;
- report metadata;
- async job;
- download authorization;
- resource cleanup.

### Tahap 8 — Photo

- pilot Mahasiswa dan Siswa;
- adapter per storage type;
- preview/crop/upload;
- audit.

### Tahap 9 — Custom action dan approval

- explicit provider;
- single/multi/all-filtered selection;
- parameter dialog;
- async action;
- approval/reject adapter.


### Tahap 9A — Audit/revision dan restore

- integrasi Envers/GenericRevisiHelper;
- audit global dan per row;
- compare before/after;
- restore field dan manual correction;
- restore satu revisi/deep restore;
- restore deleted record;
- dry-run dan background restore terbaru mulai tanggal;
- immutable/masked audit.

**Gate:** audit scope/permission, restore transaction, dependency, masking, dan parity tests lulus.

### Tahap 9B — Hapus Data Ini oleh Super Admin

- explicit permanent-delete policy per entity;
- `Common.getApakahAdmin()` + DELETE + scope + policy;
- typed confirmation dan alasan;
- FK/domain/optimistic preflight;
- hapus row aktif tetapi pertahankan audit;
- uji restore kembali pada pilot aman.

**Gate:** default disabled, non-admin 403, admin tanpa DELETE 403, audit retained, rollback tests lulus.

### Tahap 9C — Complex form override

- form provider registry;
- full-page/tabbed/wizard modes;
- tab/section/field metadata;
- conditional/lazy tabs;
- save-before-enter;
- cross-tab validation/error summary;
- Mahasiswa parity pilot.

**Gate:** form generik tetap menjadi fallback; Mahasiswa belum enabled sebelum full parity.


### Tahap 10 — Alias generator dan routing

- generate alias JSP/service;
- update New UI catalog/menu berdasarkan READ;
- deep-link guard;
- command palette hanya menampilkan halaman authorized;
- generator idempotent dan tidak menimpa file override manual.

### Tahap 11 — Migrasi bergelombang

Urutan disarankan:

1. referensi sederhana: Agama, jenis/kategori/status kecil;
2. master organisasi: fakultas, jurusan/prodi, satuan kerja;
3. akademik sederhana;
4. inventory/asset references;
5. master berelasi kompleks;
6. photo entities;
7. Mahasiswa/Siswa/Dosen/Guru/Pegawai;
8. Tbmuser/Tbmrole;
9. entity transaksi/integrasi hanya setelah review khusus.

Jangan menonaktifkan Action/ZUL lama sebelum parity dan rollback tersedia.

---

## U. Pengujian wajib

### U.1 Privilege matrix

Uji setiap kombinasi READ/CREATE/UPDATE/DELETE/APPROVE/REJECT, termasuk perubahan role saat tab masih terbuka.

### U.2 Scope

- tenant A tidak dapat membaca/lookup/export/mengubah tenant B;
- IDOR/deep-link ditolak;
- import row di luar scope ditolak;
- job download tidak dapat diambil user lain.

### U.3 Query

- filter cepat/lanjutan;
- sort asc/desc setiap kolom aman;
- deterministic ordering;
- page size seluruh pilihan;
- page kosong/halaman di luar batas;
- large dataset;
- relation >20 dan <=20;
- nested relation cycle.

### U.4 Mutasi

- required/format/unique;
- relation invalid/out of scope;
- optimistic conflict;
- rollback;
- soft delete/restore;
- audit before/after.

### U.5 Import/export

- template valid/invalid;
- duplicate header;
- hidden/formula cells;
- corrupt XLSX;
- oversized file;
- mixed operations;
- delete=true;
- idempotency;
- row errors;
- filter/scope parity semua format;
- permission revoked mid-job.

### U.6 UI

Uji minimal desktop 1920, laptop 1366, tablet 768, mobile 360/390, keyboard-only, zoom/reflow, dialog focus, long labels, empty/loading/error states.


### U.6A Audit dan restore

- audit global dan per row hanya untuk READ/scope;
- filter actor/tanggal/type/field dan database paging;
- compare dua revisi;
- field restore dan manual correction membuat revisi baru;
- historical revision tidak berubah;
- restore revision shallow/deep;
- restore deleted record membutuhkan CREATE+UPDATE;
- cycle/depth/dependency failure;
- mass restore dry-run/progress/cancel/log;
- secret/PII masking.

### U.6B Hapus Data Ini

- default policy disabled;
- non-admin 403;
- Super Admin tanpa DELETE 403;
- typed confirmation salah ditolak;
- alasan kosong ditolak;
- FK/domain block rollback;
- audit row tetap tersedia setelah active row dihapus;
- restore setelah delete pada pilot aman;
- transaksi sensitif tidak bisa dihapus generik.

### U.6C Form kompleks

- generic fallback;
- provider allow-list dan path traversal/class injection ditolak;
- full-page/tabbed/wizard;
- conditional/lazy tabs;
- save-before-enter idempotent;
- cross-tab validation/focus;
- field/tab privilege;
- unsaved-change guard;
- mobile/keyboard/accessibility;
- parity Mahasiswa.


### U.7 Build/regression

```bash
ant clean
ant
```

Lanjutkan test existing. Periksa:

```bash
git diff --check
```

Tidak boleh ada compile error, JSP syntax error, session leak, lazy loading di view, query tanpa scope, atau perubahan ZUL lama yang tidak diperlukan.

---

## V. Deliverables Codex

Codex harus menghasilkan:

1. seluruh Java framework;
2. shared JSP/CSS/JS;
3. database migration;
4. scanner + alias/config generator;
5. pilot Agama full working;
6. minimal beberapa entity reference tambahan sebagai proof;
7. adapter skeleton explicit untuk Mahasiswa/Siswa/Dosen/Guru/Pegawai/Tbmuser;
8. parity matrix per entity migrated;
9. test dan hasil build;
10. dokumentasi operator/import/export;
11. daftar entity yang tetap DISABLED/REVIEW_REQUIRED beserta alasan;
12. migration dan rollback guide;
13. implementasi audit global/per-row, compare, restore field/revision/deep/massal;
14. implementasi Hapus Data Ini dengan Super Admin policy dan audit retained;
15. complex form provider dan pilot Mahasiswa full-page tabs;
16. commit yang terpisah dan mudah direview.

Contoh commit:

```text
feat(generic-crud): add runtime metadata registry and privilege guard
feat(generic-crud): add server-side query filter sort paging
feat(generic-crud): add responsive JSP CRUD renderer
feat(generic-crud): add safe xlsx import export jobs
feat(generic-crud): add document export and user views
feat(generic-crud): migrate Agama with parity tests
```

Jangan push ke `master`; push branch:

```bash
git push -u origin feat/generic-crud-general-value-object
```

---

## W. Definition of Done

Pekerjaan baru dinyatakan selesai apabila:

- Agama dan pilot lain bekerja end-to-end tanpa `501`;
- UI hanya tampil untuk READ;
- semua tombol mengikuti privilege;
- server menolak operasi tanpa privilege/scope;
- paging/sort/filter query langsung ke database;
- relation selector mengikuti threshold 20;
- XLSX import mempunyai dry-run dan `delete=true` aman;
- XLSX/PDF/DOCX/PPTX mengikuti filter dan scope;
- column preference tersimpan per user+role;
- custom action dapat dioverride tanpa memodifikasi engine;
- photo adapter bekerja pada pilot;
- audit global/per row, compare, restore field/revision/deep/massal berjalan;
- Hapus Data Ini hanya dapat dijalankan oleh `Common.getApakahAdmin()` + DELETE + entity policy, dan audit tetap tersedia;
- audit historis immutable; koreksi/restore membuat revisi baru;
- form Add/Edit dapat dioverride menjadi tabbed/full-page/wizard tanpa menghilangkan generic fallback;
- pilot Mahasiswa mempertahankan parity tab/field/action/validation;
- audit dan optimistic locking berjalan;
- desktop/tablet/mobile lulus test;
- Ant build dan regression test lulus;
- Action/ZUL existing tetap tersedia selama migrasi;
- seluruh entity belum diverifikasi tetap default disabled.
