# Repository UI V2 — rollout dan rollback

## Feature flag

V2 aktif secara default. Atur JVM system property berikut sebelum Tomcat dimulai:

```text
-Dais.repository.uiV2=false
```

Nilai `false` mengaktifkan halaman kompatibilitas `ListRepositoryLegacy.jsp`. Hapus property atau ubah menjadi `true` untuk kembali ke V2. Flag hanya mengganti presentasi; endpoint publik, OAI-PMH, metadata, workflow, dan tabel tetap sama.

## Scope tenant

`RepositoryTenantScope` membentuk kunci `SEKOLAH:<id>`, `PT:<id>`, atau fallback `AIS:DEFAULT` dari domain/request aktif. `RepoItem`, `RepoCollection`, statistik, facet, OAI/detail publik, workspace, dan preferensi pengguna difilter menggunakan kunci tersebut. Record legacy tanpa scope diberi tenant satu per satu ketika sumber terkait disinkronkan; tidak ada bulk-claim lintas tenant.

## Hak akses

- Anonymous: membaca record publik.
- Member dengan `bacaRepository=true`: deposit ke koleksi yang mengizinkan deposit.
- Role dengan `dasborRepository=true`: review.
- Role `am`: administrasi repository.

Create, save, submit, resubmit, dan upload memeriksa izin kembali di server serta menggunakan CSRF untuk operasi browser.

## Data baru

Hibernate mengelola seluruh kolom tambahan pada item, bitstream, koleksi, dan usage event,
serta tabel workflow, relation, notification, preference, author authority, contributor, dan
integration audit. Tidak diperlukan migrasi, `ALTER TABLE`, `CREATE TABLE`, atau rollback DDL
manual. Skrip kompatibilitas migrasi/rollback tidak melakukan mutasi database.

## Keunikan OAI-PMH

`oai_identifier` tetap unik secara global agar endpoint OAI-PMH tidak pernah menerbitkan dua
record dengan identifier yang sama. Sinkronisasi memeriksa bentrok lintas tenant. Identifier
lama yang sudah terpublikasi dipertahankan; record tenant lain menggunakan sufiks tenant dan,
bila masih diperlukan, kelas sumber serta nomor urut. Kegagalan sinkronisasi terdahulu dapat
langsung dicoba ulang setelah deployment tanpa mengubah constraint atau membersihkan tabel.
