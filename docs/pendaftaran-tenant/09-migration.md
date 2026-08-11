# 09 — Migration

## Mekanisme (mengikuti konvensi repo — TANPA DDL manual)

Migration = **additive murni** lewat mekanisme deployment existing `hbm2ddl.auto=update`:
16 entity baru (paket `ais.database.model.tenant`) terdaftar di `src/hibernate.cfg.xml` →
saat startup Tomcat pasca-deploy, Hibernate MEMBUAT 16 tabel `public.*` + 16 tabel audit
`new_audit.*__audit` + unique constraint dari anotasi (`@Column(unique)` /
`@Table(uniqueConstraints)`). TIDAK ada tabel/kolom existing yang diubah/di-drop/di-rename;
TIDAK ada kolom baru pada `public.pendaftar` (sengaja — jebakan kolom-audit Envers, lihat
01-source-audit §9). Idempoten: startup ulang tidak mengubah apa pun.

Daftar tabel: lihat 07-data-dictionary.md. Seed data: `jenis_usaha_tenant` (14) +
`jenis_usaha_tenant_module` — idempoten, hanya insert yang belum ada, editan admin tidak ditimpa.

## Verifikasi pasca-deploy (jalankan di DB dev)

```sql
SELECT table_name FROM information_schema.tables WHERE table_schema='public'
 AND table_name IN ('jenis_usaha_tenant','pendaftar_tenant_profile','pendaftaran_tenant',
 'pendaftaran_tenant_jenis_usaha','schema_name_reservation','tenant_registry','tenant_domain',
 'tenant_module_entitlement','pendaftaran_email_verification','pendaftaran_consent',
 'registration_credential_delivery','provisioning_job','provisioning_step','tenant_membership',
 'jenis_usaha_tenant_module','pendaftaran_audit_event');           -- harus 16 baris
SELECT COUNT(*) FROM new_audit.pendaftaran_tenant__audit LIMIT 1;   -- tabel audit ada
SELECT code FROM public.jenis_usaha_tenant ORDER BY display_order;  -- 14 kode seed
```

## Backfill Pendaftar existing (§16.4) — BELUM DIJALANKAN (butuh akses DB deployment)

Aturan yang disepakati (dieksekusi saat UAT/rilis, hasil ke `migration-exceptions.csv`):
- TIDAK mengarang jenis usaha; profile HANYA utk akun self-service terbukti
  (`password_hash IS NOT NULL`).
- `jenis_bisnis` teks lama yang cocok kode katalog → dipetakan; tak dikenal → LAINNYA + raw value.
- Default tenant registry utk Pendaftar existing HANYA setelah rule disetujui pemilik produk.
- Duplikat email/domain → baris exception report, bukan dipaksa unique.
- Password existing tidak disentuh.

## Rollback

Perubahan kode: `git revert` commit fase terkait (additive, tidak mengubah jalur lama kecuali
bridge `aksi=daftar` + tombol daftar landing — dua titik itu kembali otomatis saat revert).
Tabel baru yang telanjur tercipta boleh dibiarkan (tidak dibaca siapa pun setelah revert) —
konsisten prinsip "tanpa DDL destruktif".
