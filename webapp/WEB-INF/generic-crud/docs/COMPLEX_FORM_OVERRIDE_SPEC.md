
# Spesifikasi Custom/Override Form Add/Edit Kompleks

## 1. Tujuan

Generic CRUD harus mempunyai renderer form standar untuk master sederhana, tetapi tidak boleh memaksa entity kompleks seperti Mahasiswa, Siswa, Dosen, Guru, Pegawai, Tbmuser, pembayaran, workflow, atau SIRS masuk ke satu form datar.

Setiap entity/page binding dapat memilih salah satu mode:

```text
GENERIC_DRAWER
GENERIC_MODAL
TABBED_DRAWER
FULL_PAGE_TABS
WIZARD
CUSTOM_COMPONENT
LEGACY_BRIDGE
```

Apabila tidak ada override, engine memakai `GENERIC_DRAWER`.

## 2. Referensi MahasiswaAction

Form Mahasiswa existing mempunyai pola yang harus dipertahankan secara fungsional:

```text
Data Mahasiswa
Keterangan Mahasiswa Pindahan          — conditional
Keterangan Mahasiswa Alih Prodi        — conditional
Biodata Lengkap                        — lazy load, perlu data tersimpan
Beasiswa                               — lazy load, perlu ID
Cuti                                   — lazy load, perlu ID
Informasi Kelulusan
Informasi Alumni                       — lazy load, perlu ID
Login Orang Tua
```

Implementasi modern boleh mengubah visual, urutan responsif, dan komponen, tetapi tidak boleh menghilangkan field, validasi, business effect, custom action, atau dependensi save-before-enter tanpa parity decision yang disetujui.

## 3. Extension point

```java
public interface GenericCrudFormOverrideProvider {
    String getMode(GenericCrudRequestContext context);
    GenericCrudFormDefinition getDefinition(GenericCrudRequestContext context,
            Object entity, boolean createMode) throws Exception;
    GenericCrudFormLoadResult loadTab(String tabKey,
            GenericCrudRequestContext context, Object entity) throws Exception;
    GenericCrudValidationResult validateTab(String tabKey,
            Map values, GenericCrudRequestContext context, Object entity) throws Exception;
    GenericCrudResult saveTab(String tabKey,
            Map values, GenericCrudRequestContext context, Object entity) throws Exception;
    List getFormActions(GenericCrudRequestContext context, Object entity) throws Exception;
}
```

Gunakan concrete DTO/class sesuai konvensi source. Template di paket adalah kontrak awal, bukan alasan untuk memakai reflection bebas.

## 4. Registry aman

Provider ditentukan dari server-side registry/config yang sudah diverifikasi. Request browser tidak boleh mengirim nama class/JSP lalu membuat engine melakukan `Class.forName()` atau include path arbitrer.

Allow-list:

```text
entityKey + pageBindingId -> providerClass + formDefinitionVersion
```

## 5. GenericCrudFormDefinition

Minimal metadata:

```text
formKey
mode
title
subtitle
width/maxWidth
createMode/updateMode
saveStrategy
optimisticLockProperty
unsavedChangeGuard
autoSavePolicy
tabs[]
sections[]
fields[]
actions[]
validationGroups[]
```

### Tab

```text
tabKey
label
icon
order
visibleExpression/provider
requiredPrivilege
lazyLoad
requiresPersistedEntity
saveBeforeEnter
validationGroup
endpoint/actionKey
badgeProvider
errorCountProvider
mobilePresentation
```

### Section

```text
sectionKey
tabKey
label
description
order
collapsible
initiallyExpanded
columnCountDesktop
columnCountTablet
```

### Field override

```text
propertyPath
editorType
label/helpText
required/readOnly/visible predicates
columnSpan
relation lookup definition
masking
validator
default value
custom renderer/editor key
```

## 6. Save strategy

### 6.1 Simple entity

Satu transaksi menyimpan seluruh form.

### 6.2 Complex aggregate

Gunakan satu dari strategi yang didaftarkan:

```text
ROOT_FIRST
PER_TAB_TRANSACTION
ATOMIC_AGGREGATE
DRAFT_AGGREGATE
LEGACY_ACTION_BRIDGE
```

`ROOT_FIRST` sesuai pola tab dependent Mahasiswa: data inti harus tersimpan terlebih dahulu agar entity mempunyai ID sebelum tab Biodata/Beasiswa/Cuti/Alumni dibuka.

### 6.3 Save-before-enter

Ketika user membuka tab yang membutuhkan ID:

1. validasi tab utama;
2. tampilkan pesan bahwa data inti perlu disimpan;
3. simpan root dalam transaksi;
4. bila gagal, kembali ke tab asal dan fokus ke error;
5. bila berhasil, load tab tujuan;
6. jangan membuat duplicate root jika user klik berulang.

## 7. Conditional dan lazy tabs

Contoh:

```text
Pindahan visible jika merupakanPindahan == true
Alih Prodi visible jika merupakanAlihProdi == true
Alumni visible berdasarkan status/lifecycle yang sesuai
```

Visibility dihitung di server dan boleh diperbarui setelah field trigger berubah. Tab lazy tidak memuat relasi besar sebelum dibuka. Error lazy load harus tampil di dalam tab dengan tombol retry; tidak boleh mengosongkan seluruh form.

## 8. Validation lintas tab

- error per field;
- error count badge per tab;
- error summary global;
- klik error memindahkan tab, scroll, dan fokus ke field;
- cross-tab validator untuk aturan bisnis;
- tab tersembunyi tidak boleh menyembunyikan error wajib tanpa policy;
- nilai valid tidak hilang ketika tab lain gagal;
- optimistic conflict menampilkan diff sebelum reload/overwrite.

## 9. Hak akses pada form

Selain operasi CREATE/UPDATE, provider dapat mengatur privilege tab/action/field. Server harus memeriksa ulang saat load dan save.

Contoh:

```text
Data pribadi                   UPDATE
Password/reset credential      UPDATE + Super Admin policy
Keuangan                       READ/UPDATE khusus menu keuangan
Approval                       APPROVE/REJECT
Audit                          READ
Admin delete                   DELETE + Common.getApakahAdmin()
```

Field yang tidak boleh dibaca tidak dikirim ke browser, bukan sekadar disabled.

## 10. Relation, foto, dokumen, custom action

Seluruh aturan Generic CRUD tetap berlaku di form override:

- relation <=20 combo; >20 bandbox server-side;
- property lookup harus mapped dan scope-safe;
- foto memakai photo adapter;
- dokumen memakai upload policy;
- custom footer/header/tab actions memakai provider explicit;
- audit sebelum/sesudah untuk setiap save;
- import/export/custom action tidak ditulis ulang di JSP.

## 11. UI/UX desktop

`FULL_PAGE_TABS` untuk entity yang sangat kompleks:

```text
Header sticky
Breadcrumb + title + status + identifier
Horizontal/scrollable tabs
Content max-width yang nyaman
Summary panel / completeness
Sticky footer action
Audit/approval timeline
```

Form biasa tetap memakai drawer 760–960 px. Tab harus mempunyai label jelas dan badge error/status. Gunakan progressive disclosure; jangan membuat satu halaman setinggi puluhan ribu pixel.

## 12. UI/UX mobile

- full-screen route/sheet;
- tab banyak menjadi horizontal scroll atau select/stepper yang accessible;
- sticky header dan footer;
- satu kolom;
- section collapsible;
- tab error tetap terlihat;
- keyboard mobile tidak menutupi tombol Simpan;
- kembali meminta konfirmasi jika ada perubahan belum tersimpan.

## 13. Accessibility

- semantic tablist/tab/tabpanel;
- keyboard Left/Right/Home/End untuk tab;
- manual activation bila load tab tidak instan;
- `aria-selected`, `aria-controls`, label, focus indicator;
- dialog/modal mempunyai focus trap dan mengembalikan fokus ke trigger;
- error summary mengarah ke field dan tab yang tepat.

## 14. Coexistence dengan Action/ZUL existing

Strategi migrasi:

1. buat parity matrix form lama;
2. extract reusable validator/helper/service;
3. pakai `LEGACY_BRIDGE` hanya sebagai transisi;
4. jangan menjalankan Hibernate/business logic di JSP;
5. jangan memodifikasi Action lama sampai form baru lulus parity;
6. sediakan feature flag dan rollback.

## 15. Endpoint contract

```text
GET  action=form-meta&id=...
GET  action=form-load-tab&id=...&tab=...
POST action=form-save-root
POST action=form-save-tab
POST action=form-validate-tab
POST action=form-run-action
GET  action=form-conflict-diff
```

Setiap request memakai entity/page binding yang sudah diotorisasi, privilege, scope, CSRF untuk mutasi, request ID, optimistic token, dan field allow-list.

## 16. Acceptance criteria minimum

- entity tanpa override tetap memakai form generik;
- entity override dapat memilih full-page/tabbed/wizard;
- request tidak dapat memilih provider/JSP arbitrer;
- conditional tab benar;
- lazy tab tidak query sebelum dibuka;
- save-before-enter tidak membuat duplicate root;
- validation error berpindah ke tab/field tepat;
- privilege tab/field/action diperiksa di server;
- unsaved-change guard bekerja;
- mobile satu kolom dan fungsi tidak hilang;
- keyboard tab dan modal lulus test;
- Mahasiswa mempunyai parity matrix lengkap sebelum enabled;
- seluruh tab/action/field existing yang masih relevan mempunyai padanan nyata.
