
# Spesifikasi Audit Trail, Restore, dan Hapus Data Aktif oleh Super Admin

## 1. Tujuan

Menyediakan satu mekanisme audit/revisi yang konsisten untuk seluruh Generic CRUD turunan `GeneralValueObject`, tanpa menghilangkan kemampuan khusus yang sudah tersedia di AIS. Implementasi harus mengintegrasikan Hibernate Envers dan helper existing, bukan membangun log dangkal yang hanya menyimpan teks aktivitas.

Fitur wajib:

- audit global seluruh record pada satu entity/class;
- audit per row/per ID;
- filter actor, role, waktu, jenis revisi, request ID, sumber perubahan, dan kolom yang berubah;
- compare before/after;
- restore satu field;
- koreksi manual satu field dari konteks revisi;
- restore seluruh snapshot revisi;
- deep restore relasi bila diizinkan;
- restore record yang sudah dihapus;
- restore terbaru mulai tanggal tertentu, dengan preview, progress, dan hasil per item;
- **Hapus Data Ini** khusus Super Admin;
- audit immutable untuk semua operasi restore dan delete.

## 2. Paritas dengan helper existing

Implementasi baru wajib membuat parity matrix terhadap `GenericRevisiHelper.java` dan `RevisiHelper.java` pada checkout terbaru. Minimal perilaku yang tidak boleh hilang:

```text
Dasbor Data Ini
Riwayat ID Ini
Seluruh Data Revisi
Mode Semua / Tambah / Ubah / Hapus
Filter tanggal dan keyword
Filter kolom yang berubah
Compare nilai revisi vs nilai aktif
Pakai nilai revisi untuk satu field
Edit manual nilai field aktif
Ubah & Restore seluruh kolom
Restore satu revisi
Deep restore relasi
Restore Terbaru mulai tanggal
Progress dan log restore
Hapus Data Ini oleh admin
```

Apabila helper terbaru sudah lebih aman atau lebih lengkap daripada rancangan paket ini, reuse/refactor helper tersebut dan pertahankan perilakunya.

## 3. Model otorisasi

### 3.1 Melihat audit

```text
READ + binding menu aktif + row-level scope
```

Audit global tidak boleh membuka record di luar tenant/perguruan tinggi/sekolah/fakultas/program studi/satuan kerja yang boleh dibaca pengguna.

### 3.2 Restore field atau revisi data aktif

Default:

```text
UPDATE + READ + row-level scope + entity restore policy
```

Untuk menghidupkan kembali record yang sudah terhapus:

```text
CREATE + UPDATE + READ + row-level scope + entity restore policy
```

Adapter boleh menambah syarat `APPROVE`, status tertentu, atau Super Admin untuk entity sensitif.

### 3.3 Restore massal

Default deny. Hanya tersedia apabila:

```text
massRestoreEnabled == true
AND Common.getApakahAdmin() == true
AND UPDATE == true
AND CREATE == true apabila ada record terhapus yang akan dibuat ulang
AND scope target sudah dihitung di server
```

### 3.4 Hapus Data Ini

Wajib memenuhi seluruh kondisi:

```text
Common.getApakahAdmin() == true
DELETE == true
entity.adminDeleteEnabled == true
permanentDeletePolicy.canDelete(...) == true
record berada dalam scope
CSRF valid
request menggunakan POST
konfirmasi eksplisit valid
alasan terisi
```

Pemeriksaan `Common.getApakahAdmin()` harus dilakukan kembali di service Java. Menyembunyikan tombol di UI tidak dianggap sebagai keamanan.

## 4. Konsep operasi

### 4.1 Audit global

Entry point toolbar: **Audit Seluruh Data**.

Tampilan:

- kartu statistik Tambah/Ubah/Hapus;
- tren revisi per hari/bulan;
- pengguna paling aktif;
- field paling sering berubah;
- daftar revisi server-side paginated;
- filter tanggal wajib untuk data besar;
- export audit sesuai policy dan masking.

### 4.2 Audit per row

Entry point:

- menu `⋮` pada row → **Riwayat Perubahan**;
- klik nama/kode apabila konfigurasi existing memang memakai pola tersebut;
- tab **Audit** di detail drawer.

Tampilan utama:

```text
Waktu | Jenis | Oleh | Role | Sumber | Field Berubah | Request ID | Aksi
```

Aksi:

- Lihat Detail;
- Bandingkan dengan data aktif;
- Bandingkan dua revisi;
- Pakai nilai field ini;
- Edit nilai field aktif;
- Ubah & Restore;
- Restore Revisi;
- Deep Restore;
- Hapus Data Ini, hanya jika seluruh guard lulus.

### 4.3 Restore satu field

Alur:

1. pengguna memilih field yang berbeda;
2. sistem menampilkan nilai revisi dan nilai aktif;
3. pengguna menekan **Pakai Nilai Ini**;
4. service memuat record aktif dalam transaksi baru;
5. service memverifikasi privilege, scope, property allow-list, tipe data, business validator, dan optimistic lock;
6. service mengubah satu property;
7. commit menghasilkan revisi baru;
8. UI menampilkan revision/request ID baru.

Tidak boleh mengubah row audit historis.

### 4.4 Koreksi manual per field

Koreksi manual memakai editor sesuai metadata field, bukan selalu textbox bebas. Relation memakai combo/bandbox yang sama dengan Add/Edit; tanggal memakai date/time picker; enum/boolean memakai pilihan terkontrol.

### 4.5 Form Ubah & Restore

Form memuat seluruh field yang dapat dipulihkan dari snapshot revisi. Field:

- identifier: read-only;
- technical/audit/password/token/blob/collection: tidak dapat diedit;
- scalar: editor sesuai type metadata;
- relation: lookup ID yang divalidasi dan berada dalam scope;
- field khusus: adapter override.

Sebelum commit, tampilkan diff final:

```text
Field | Nilai Aktif | Nilai yang Akan Diterapkan
```

### 4.6 Restore seluruh revisi

Mode:

```text
SHALLOW  — data utama saja
DEEP     — data utama dan relasi pendukung yang dapat dipulihkan secara aman
```

Deep restore harus:

- memakai Hibernate metadata, bukan memanggil semua getter;
- mendeteksi siklus;
- mempunyai batas kedalaman;
- memproses deferred relation;
- memberikan error per relasi;
- rollback atomik untuk satu target record;
- tidak membuat relasi di luar scope/policy.

### 4.7 Restore terbaru mulai tanggal

Harus mempunyai dua tahap:

1. **Dry-run/Preview**: jumlah ID, jumlah ADD/MOD/DEL, dependency, konflik, record yang akan dibuat kembali, estimasi durasi;
2. **Commit**: background job dengan progress, cancel request, heartbeat, log, dan hasil per item.

Satu item sebaiknya memakai transaksi terpisah agar satu kegagalan tidak membuat seluruh batch menjadi setengah commit atau terlalu lama mengunci database. Ringkasan akhir wajib menyebut sukses, gagal, dilewati, konflik, dan link download log.

## 5. Hapus Data Ini oleh Super Admin

### 5.1 Definisi

Operasi ini menghapus **row aktif** dari tabel bisnis. Riwayat Envers/audit tidak dipurge. Karena audit tetap ada, record dapat direstore apabila mapping, constraint, dan dependency memungkinkan.

### 5.2 Default

```text
adminDeleteEnabled = false
```

Aktifkan per entity setelah review. Untuk transaksi posted, jurnal, pembayaran, ledger, rekam medis final, dokumen hukum, dan data integrasi kritis, adapter harus menolak delete dan menyediakan cancellation/reversal sesuai rule domain.

### 5.3 Dialog konfirmasi

Dialog harus menampilkan:

- entity dan identifier;
- display label record;
- jumlah referensi/FK yang terdeteksi;
- dampak;
- fakta bahwa data aktif akan hilang tetapi audit tetap ada;
- alasan wajib;
- typed confirmation, misalnya `HAPUS AGAMA:123`;
- checkbox pengakuan dampak;
- optional re-authentication sesuai kebijakan deployment.

### 5.4 Preflight

Sebelum delete:

- reload record dalam session baru;
- verifikasi version/last update;
- hitung relasi yang dapat diketahui;
- jalankan `GenericCrudPermanentDeletePolicy`;
- tolak bila record system/core/default;
- tolak bila masih direferensikan, kecuali adapter mempunyai strategi aman yang eksplisit;
- jangan menggunakan cascade delete generik yang tidak direview.

### 5.5 Transaction dan audit

Dalam transaksi:

1. simpan snapshot termasking untuk audit;
2. delete object aktif;
3. flush;
4. commit;
5. tulis/konfirmasi audit event `ADMIN_DELETE_ACTIVE_ROW` dengan request ID, actor, role, reason, object key, before snapshot, hasil, dan policy version.

Apabila terjadi error, rollback dan catat audit `FAILED`. Jangan menampilkan stack trace kepada pengguna biasa.

## 6. Audit event dan data sensitif

Minimal metadata:

```text
revisionNumber
revisionTimestamp
revisionType
entityKey
objectKey
actorUserKey
activeRoleKey
menuId
sourceType
sourceJobKey
requestId
operation
resultStatus
reason
changedFields
beforeSnapshot
appliedSnapshot/afterSnapshot
scopeSummary
policyVersion
```

Password, PIN, token, secret, private key, session ID, dan binary foto tidak boleh disimpan. NIK, rekening, email, nomor telepon, dan data sensitif lain mengikuti masking policy.

Audit store harus append-only pada level aplikasi. Generic UI tidak menyediakan edit/delete audit event.

## 7. Java extension points

```text
GenericCrudAuditRevisionService
GenericCrudAuditRevisionAdapter
GenericCrudRestoreService
GenericCrudRestorePolicy
GenericCrudPermanentDeletePolicy
GenericCrudRestoreJobService
GenericCrudAuditMaskingService
```

`GenericCrudAuditRevisionAdapter` menghubungkan entity ke Envers/helper existing. `GenericCrudRestorePolicy` menangani batasan domain. `GenericCrudPermanentDeletePolicy` wajib explicit per entity untuk mengaktifkan delete aktif oleh Super Admin.

## 8. Endpoint contract

```text
GET  action=audit-summary
GET  action=audit-list&scope=GLOBAL
GET  action=audit-list&scope=ROW&id=...
GET  action=audit-detail&revision=...
GET  action=audit-compare&leftRevision=...&rightRevision=...
POST action=restore-field
POST action=manual-correct-field
POST action=restore-revision
POST action=restore-preview
POST action=restore-from-date
GET  action=restore-job-status&jobKey=...
GET  action=restore-job-log&jobKey=...
POST action=restore-job-cancel
POST action=admin-delete-preview
POST action=admin-delete-active-row
```

Semua mutasi: POST, CSRF, request ID, idempotency, privilege, scope, allow-list field, optimistic lock, reason bila diwajibkan, dan audit.

## 9. UI/UX

Desktop:

- audit drawer 520–760 px atau full-page untuk audit global;
- timeline, table compare, filter chips, dan sticky action footer;
- destructive action berada dalam danger zone, bukan tombol utama.

Mobile:

- audit full-screen;
- revision item menjadi card;
- before/after dapat ditukar melalui segmented control;
- tindakan restore/delete berada di bottom sheet;
- typed confirmation tetap accessible.

## 10. Acceptance criteria minimum

- non-READ menerima 403 untuk audit list/detail;
- record di luar scope tidak terlihat dan tidak dapat direstore;
- property yang tidak mapped/allow-listed ditolak;
- field restore membuat revisi baru;
- historical row tidak berubah;
- restore deleted row memerlukan CREATE+UPDATE;
- deep restore menangani siklus dan kegagalan dependency;
- mass restore mempunyai dry-run dan progress;
- non-admin tidak pernah melihat/menjalankan Hapus Data Ini;
- admin tanpa DELETE tetap ditolak;
- admin delete disabled policy tetap ditolak;
- FK/preflight failure rollback;
- audit tetap tersedia setelah active row dihapus;
- restore setelah admin delete diuji pada pilot yang aman;
- audit payload tidak mengandung password/token;
- paging/filter/sort audit dilakukan langsung ke database/audit query.
