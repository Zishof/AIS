# Audit source aktual AIS untuk Generic CRUD V2

Tanggal pemindaian: 8 Agustus 2026. Source yang dipakai adalah
`C:\opt\AIS\ais\src\main\java` dan `C:\opt\AIS\ais\src\main\webapp`.
Folder `C:\opt\AIS\ais\src\main\src` tidak ada; folder Java aktual adalah
`java`.

Scanner paket dijalankan ulang terhadap checkout aktual, bukan hanya memakai
snapshot paket. Hasil lengkap ada di folder `manifests`:

- subclass `GeneralValueObject`: 1.506;
- concrete entity: 1.495;
- abstract: 11;
- kandidat foto: 68;
- kandidat custom action: 913;
- `REVIEW_REQUIRED`: 1.132;
- `ELIGIBLE_METADATA_FIRST`: 350;
- `ELIGIBLE_PARITY_FIRST`: 13.

Snapshot paket berisi 1.501 subclass dan 1.490 entity, sehingga ada lima entity
baru pada checkout aktual. Tidak ada aktivasi massal. Semua hasil scanner tetap
kandidat disabled sampai metadata Hibernate, natural key, menu, privilege,
scope, relasi, dan aturan bisnisnya direview.

Pilot yang diaktifkan hanya `ais.database.model.Agama` pada binding
`root/agama`. `Mahasiswa` hanya memiliki metadata parity form
`FULL_PAGE_TABS` dan tetap `REVIEW_REQUIRED`/disabled.

Kode existing yang menjadi sumber integrasi:

- `GenericCrudAction` dan `AgamaAction` untuk perilaku CRUD lama;
- `CommonPrivilages`/`RolePrivilage` untuk RBAC;
- `HibernateUtil` dan metadata `SessionFactory` untuk session dan source of truth;
- Envers, `RevisiHelper`, `AuditTrailHelper`, dan interceptor audit existing;
- `Common.getApakahAdmin()` untuk lapisan wajib permanent active-row delete;
- route/service New UI existing untuk binding JSP.

Migrasi SQL disertakan tetapi tidak dijalankan otomatis terhadap database.
Semua fitur restore, mass restore, import, dan permanent delete memiliki default
disabled sampai review policy/adapters dan dry-run selesai.
