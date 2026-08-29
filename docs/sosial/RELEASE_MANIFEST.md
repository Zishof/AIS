# Release Manifest Modul Sosial

| Field | Nilai saat pembaruan source |
|---|---|
| Baseline sebelum perubahan | SVN r78256 |
| Tanggal | 25 Agustus 2026 |
| Source root | `src/main/src` |
| Web root | `src/main/webapp` |
| Database | PostgreSQL 16.4 lokal |
| Social schema | NOT_CREATED (0 tabel terdeteksi sebelum deployment) |
| WAR | NOT_BUILT_BY_REQUEST |
| Runtime | NOT_DEPLOYED_BY_REQUEST |
| Smartlink sandbox | BLOCKED_EXTERNAL_CREDENTIAL_AND_CONTRACT |
| Accounting | STUB_NOOP / forced fail-closed |
| General registration | OUT_OF_SCOPE_V1 |
| Production ready | NO |

## Pembaruan 29 Agustus 2026

- Baseline working copy source saat audit lanjutan: SVN r78523.
- Perubahan lanjutan masih berupa working-copy change dan belum di-commit: koreksi/refund terhadap alokasi, rekonsiliasi idempoten, validasi sumber dana penyaluran, dan tambahan test invariant refund.
- Targeted Java compilation dan tiga self-test sosial lulus.
- Tidak ada WAR baru yang dibuat dan tidak ada deployment pada pembaruan ini.
- Status produksi tetap **NO** sampai schema/migration, runtime ZK/JSP/API, tenant/RBAC, callback paralel, sandbox Smartlink, dan rollback diuji pada server tujuan.

Revision final, dirty-state, WAR SHA-256, schema fingerprint, Tomcat/JDK, enabled flags, tenant pilot, Smartlink contract version, zakat policy version, evidence version, dan rollback artifact harus diisi oleh release owner setelah commit/build/deployment.
