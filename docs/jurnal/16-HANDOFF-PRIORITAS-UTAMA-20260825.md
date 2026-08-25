# Handoff prioritas utama jurnal AIS — 25 Agustus 2026

Dokumen ini adalah addendum paling mutakhir untuk checkout `C:\opt\AIS\ais`. Jika status di dokumen 10 atau 15 bertentangan dengan dokumen ini, gunakan hasil bertanggal 25 Agustus 2026 di bawah. Pekerjaan ini sengaja tidak membuat WAR dan tidak melakukan deployment sesuai instruksi pemilik sistem.

## Ringkasan hasil

| Area | Status | Evidence aktual |
|---|---|---|
| Sinkronisasi izin role | `PASS_LOCAL` | Penyimpanan `jurnal_akses_json` kini menyinkronkan `job_has_menu` secara atomik dalam transaksi yang sama. Regression test membuktikan grant, revoke, negative role, cache refresh, dan rollback. |
| Policy schema Hibernate jurnal | `PASS_LOCAL` | Main dan streaming dipaksa `hbm2ddl.auto=none`; SQL comments dipaksa `false`. Migrasi SQL menjadi satu-satunya jalur perubahan schema jurnal. |
| Compile source final | `PASS` | Ant mengompilasi 7.162 source dari `src/main/src`; `BUILD SUCCESSFUL`, 1 menit 23 detik. Tidak ada target package/WAR yang dijalankan. |
| Dependency remediation | `PASS_WITH_RESIDUAL` | Dependency prioritas berisiko diperbarui dan versi lama dikeluarkan dari classpath compile/package Maven serta Ant. Full compile dengan classpath efektif baru lulus. JAR lama masih fisik karena dikunci proses Java milik pekerjaan lain, tetapi tidak ikut packaging. |
| SBOM/SCA/VEX | `PASS_WITH_RESIDUAL` | CycloneDX memuat 185 komponen. Scan raw menyisakan dua temuan Hibernate 3; VEX tervalidasi menurunkan hasil efektif menjadi 0 karena jalur Criteria API tidak ada dan SQL comments dipaksa mati. Review legal lisensi tetap wajib. |
| Secret scan | `PASS` | Credential SharePoint yang tertanam di source dihapus; konfigurasi wajib melalui environment. Trivy secret scan ulang menghasilkan 0 temuan. Secret lama wajib dirotasi karena pernah berada di source. |
| SIT teknis | `PASS_LOCAL` | Contract, RBAC, database, workflow, email, payment, access/IP, usage, file, health, schema fingerprint, importer OJS 3.5/legacy, demo lifecycle, dan performance smoke lulus pada clone/fixture. |
| UAT teknis | `PASS_LOCAL_TECHNICAL` | Bootstrap, menu 29/29, role sync, notification, workflow, streaming, file end-to-end, health, dan schema fingerprint lulus pada clone UAT. Ini bukan sign-off pengguna. |
| Full-scale performance | `PASS_LOCAL_SIT_V1` | Evidence 23 Agustus tetap berlaku: 100 ribu artikel, 1 juta metadata file, 10 juta usage row, 10 ribu pengguna; 8 thread x 300 detik; 0 error. Tidak diulang pada 25 Agustus karena dataset/gate tersebut sudah mempunyai evidence. |
| WAR dan deployment | `NOT_RUN_BY_REQUEST` | Pemilik sistem akan build WAR dan deploy sendiri. |

## Implementasi utama

### RBAC dan menu fisik

- `JurnalRoleMenuSynchronizer` memetakan izin canonical jurnal ke menu fisik.
- `TbmroleAction` menyimpan JSON role dan assignment menu pada transaksi yang sama; kegagalan membatalkan seluruh perubahan.
- Test `JurnalRolePermissionSyncSelfTest` dan `JurnalAksesKatalogSelfTest` mengunci least privilege dan sinkronisasi dua arah.
- UAT setelah reconcile: `inserts=0 updates=0 unchanged=29 collision=0`.

### Database dan migrasi eksplisit

- `migrasi_prasyarat_repository_jurnal_v1.sql` dilengkapi lima kolom preference notification yang sebelumnya hilang.
- `migrasi_streaming_jurnal_v1.sql` membuat `lampiran_jurnal` secara idempoten: 19 kolom, tipe file `oid`, dan dua unique constraint.
- Verifier main dan streaming bersifat read-only dan fail-fast.
- Migrasi streaming dan verifier lulus pada `streaming_ais_jurnal_sit`, `streaming_ais_jurnal_uat`, dan `streaming_ais_jurnal_import_fixture`.
- Fingerprint UAT: main 12 tabel jurnal; streaming 1 tabel jurnal, 19 kolom, 2 unique constraint.

### Dependency dan keamanan

Dependency yang diperbarui mencakup Commons FileUpload/IO/Collections/BeanUtils/Lang3, reload4j, PostgreSQL JDBC, Jackson Core, Gson, Google OAuth Client, Protobuf, Jersey 1.19.1, PDFBox 1.8.17, Guava, dan Apache HttpClient/HttpCore. `pom.xml` dan `ant/build.xml` mengecualikan nama JAR lama dari compile/package sehingga file terkunci tidak dapat masuk ke artifact.

Artifact evidence:

| File | SHA-256 |
|---|---|
| `build/security/journal-jar-sbom.cdx.json` | `5E36CBE2E18BDDDCBFC67B5C8DF1FAB32F6D4FF5CA42082182E386C7FB68D768` |
| `build/security/journal-hibernate-vex-20260825.cdx.json` | `2B08094BA5910B2B22CB1BF340A877C6F89B40A71FC75E22DABF9DD525297A3F` |
| `build/security/trivy-sbom-20260825.json` | `B39B711A8E3CFE2439BE54B76FC92EC5C154917C4E035544EDFC80B12546047F` |
| `build/security/trivy-sbom-vex-20260825.json` | `736CEE5450AD335CC877A6559953D611A4A8E4614584DE52FE8BC3C4D8775CF3` |
| `build/security/trivy-secrets-20260825.json` | `5618B01ADCD93932CDF8943F367E302D80A96C883E9C42D0F585C679B9D58ACD` |

Raw SCA menemukan `CVE-2020-25638` dan `CVE-2019-14900` pada Hibernate 3.6.10. VEX menyatakan keduanya `not_affected` berdasarkan kontrol yang diuji, bukan karena dependency telah di-upgrade. Migrasi Hibernate/Spring/Axis lintas-modul tetap merupakan pekerjaan arsitektur terpisah.

Scan lisensi menghasilkan 23 finding. Dua theme ZK berlisensi LGPL-3.0 ditandai high oleh kebijakan scanner dan `jsr311-api` CDDL ditandai medium; sebagian besar JAR legacy belum memiliki metadata lisensi terstruktur. Ini memerlukan penerimaan legal, bukan perubahan kode otomatis.

### SIT/UAT dan importer

- Pure contract suite: 13/13 lulus.
- Main/streaming database suite dan rollback hygiene lulus.
- Demo lifecycle lulus dengan 500 jurnal/50.000 artikel, termasuk minimal 5.500 published dan 6.000 draft; dataset `sample-50x100` dapat dibuat idempoten dan dihapus aman.
- OJS 3.5: 134 tabel/905 field, preflight read-only, dry-run, execute, cancel/resume, transform, checksum/file reconciliation, dan idempotency lulus.
- OJS legacy: 7 tabel/37 field lulus.
- Workflow/export lulus secara lokal. Validasi remote DOAJ pada run 25 Agustus mendapat HTTP 403 dari endpoint eksternal; journey yang sama lulus tanpa remote XSD dan evidence validator resmi sebelumnya tetap tercatat. HTTP 403 tidak diklaim sebagai kelulusan remote gate baru.
- Performance smoke: 50.000 row, 180 sample, p50 38,86 ms, p95 85,26 ms, 21,19 ops/detik, heap 1,868 GB. Angka ini characterization, bukan threshold resmi.

## Yang masih terbuka

1. Rotasi segera secret Azure/SharePoint lama di tenant penyedia. Penghapusan dari source tidak membatalkan credential yang mungkin telah terekspos.
2. Review legal/lisensi atas theme LGPL, CDDL, dan komponen tanpa metadata lisensi yang memadai.
3. Sandbox riil SMTP, payment callback, DOI/Crossref/DataCite/DOAJ/PubMed/OAI provider dan rekonsiliasi eksternal.
4. Browser journey authenticated untuk 28 menu, matrix visual 390/768/1024/1440, WCAG/accessibility, dan active-content galley.
5. Soak produksi-like 4–24 jam, HTTP/WAL monitoring, threshold/SLA yang disetujui owner, serta capacity sign-off.
6. UAT manusia, training/support, backup-restore drill, pilot, cutover, dan go/no-go formal.
7. Upgrade besar Hibernate 3/Spring 3/Axis dan dependency legacy lain sebagai proyek kompatibilitas lintas-modul.
8. Setelah proses Java eksternal yang mengunci JAR lama berhenti, pindahkan JAR obsolete ke backup. Jangan menghentikan proses tersebut hanya untuk cleanup; packaging exclusion sudah menjadi kontrol efektif.

## Instruksi build/deploy untuk pemilik sistem

Sebelum build WAR, pastikan migrasi main dan streaming telah direview dan diterapkan pada target yang benar, environment database/SharePoint tersedia melalui secret manager, dan daftar exclude JAR lama pada Maven/Ant tidak dihapus. Setelah build, catat hash WAR, periksa isi `WEB-INF/lib` tidak mengandung nama JAR obsolete, lalu jalankan cold-container smoke pada clone sebelum deploy server.

Jangan memasukkan password, token, atau connection string ber-secret ke Markdown, XML, source, command history yang dibagikan, atau artifact WAR.
