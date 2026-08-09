
# Changelog V2 — Audit, Restore, Hapus Data Aktif oleh Super Admin, dan Form Kompleks

## Ringkasan

Versi ini memperluas paket Generic CRUD `GeneralValueObject` dengan dua requirement wajib yang sebelumnya belum cukup eksplisit:

1. **Requirement 13:** pusat audit trail/revisi global dan per row, perbandingan before/after, koreksi atau restore per field, restore satu revisi, deep restore, restore terbaru mulai tanggal tertentu, serta tindakan **Hapus Data Ini** yang hanya tersedia bagi Super Admin dan tetap mempertahankan riwayat revisi.
2. **Requirement 14:** form Add/Edit yang dapat dioverride menjadi drawer, modal, full page, wizard, atau form bertab yang kompleks seperti pola `MahasiswaAction.java`.

## Landasan source AIS

Paket V2 mewajibkan agent mempelajari source terbaru, terutama:

```text
src/ais/action/master/helper/GenericRevisiHelper.java
src/ais/action/master/helper/RevisiHelper.java
src/ais/action/master/MahasiswaAction.java
src/ais/action/master/AgamaAction.java
```

`GenericRevisiHelper` existing sudah menjadi referensi penting karena menyediakan:

- Dasbor revisi data aktif;
- Riwayat revisi satu ID;
- Seluruh data revisi dalam class yang sama;
- mode Tambah, Ubah, dan Hapus;
- perbandingan nilai revisi dengan nilai aktif;
- penggunaan nilai historis untuk satu field;
- edit manual nilai field aktif;
- formulir **Ubah & Restore**;
- restore satu revisi, termasuk deep restore relasi;
- restore massal revisi terbaru mulai suatu tanggal;
- progress, ringkasan, dan log restore;
- tombol **Hapus Data Ini** untuk admin, dengan riwayat revisi tetap dipertahankan agar data bisa direstore kembali.

`MahasiswaAction` menjadi referensi form kompleks karena memisahkan Data Mahasiswa, Pindahan, Alih Prodi, Biodata Lengkap, Beasiswa, Cuti, Informasi Kelulusan, Informasi Alumni, dan Login Orang Tua ke tab berbeda, termasuk conditional visibility, lazy loading, dan save-before-enter untuk tab yang membutuhkan ID.

## Keputusan keselamatan

### Audit record tidak diedit secara langsung

Permintaan “edit per kolom” diterapkan sebagai:

- pilih nilai dari revisi tertentu lalu terapkan ke data aktif; atau
- edit nilai data aktif berdasarkan konteks revisi;
- simpan sebagai mutasi baru sehingga menghasilkan revisi/audit baru.

Historical audit row tidak boleh diubah atau dihapus melalui Generic CRUD. Dengan demikian jejak perubahan tetap dapat dipertanggungjawabkan.

### Hapus permanen yang dimaksud

Generic CRUD membedakan:

1. **Nonaktif/soft delete** — pilihan utama untuk master data;
2. **Hapus data aktif oleh Super Admin** — menghapus row aktif dari tabel bisnis, tetapi audit/revisi tetap ada dan dapat digunakan untuk restore;
3. **Purge audit history** — tidak disediakan secara generik dan tidak boleh disamakan dengan poin 2.

Tombol Hapus Data Ini hanya muncul apabila semua syarat terpenuhi:

```text
Common.getApakahAdmin() == true
AND privilege DELETE pada menu/binding aktif
AND data masih berada dalam scope pengguna
AND entity policy adminDeleteEnabled == true
AND record tidak diblokir oleh rule domain/status/FK
```

## File baru

```text
AUDIT_RESTORE_SUPERADMIN_DELETE_SPEC.md
COMPLEX_FORM_OVERRIDE_SPEC.md
sql/003_generic_crud_audit_restore_form_override.sql
prototype/generic_crud_audit_restore_complex_form.html
templates/java/GenericCrudAuditRevisionAdapter.java.template
templates/java/GenericCrudRestorePolicy.java.template
templates/java/GenericCrudPermanentDeletePolicy.java.template
templates/java/GenericCrudFormOverrideProvider.java.template
templates/java/GenericCrudFormDefinition.java.template
```

## File yang diperbarui

- prompt master Codex/Claude Code;
- arsitektur teknis;
- spesifikasi UI/UX;
- matriks requirement dan acceptance;
- security/performance;
- migration plan;
- test matrix;
- contoh konfigurasi Agama dan Mahasiswa;
- validation script, validation report, dan checksum.
