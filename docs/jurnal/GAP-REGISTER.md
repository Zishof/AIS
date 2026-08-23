# Gap register current checkout

Status mengikuti kontrak dokumen 14. Tidak ada status `PASS` tanpa test aktual.

| ID | Scope | Status | Evidence / gap berikutnya |
|---|---|---|---|
| JRN-A-001 | Manifest dan identitas checkout | `PARTIAL_PASS` | Manifest current checkout diperbarui setelah build final; Git/SVN tidak tersedia sehingga revision tetap unverified |
| JRN-A-002 | Schema 12 main + 1 streaming | `PASS` | Fingerprint SIT 12 + 1; baseline tidak dimutasi |
| JRN-B-001 | Entity streaming `LampiranJurnal` | `PASS` | Mandiri, 19 kolom, dua unique constraint, tidak subclass dan tidak ada cross-DB ORM |
| JRN-B-002 | Lifecycle/compensation file | `PARTIAL_PASS` | Upload, checksum, stream, cleanup, reconcile lulus; AV scanner nyata dan orphan scheduler periodik belum tersedia |
| JRN-B-003 | Canonical 28 route/menu/capability | `PARTIAL_PASS` | Reconciler fail-closed memeriksa id/root/child/url/label; clone SIT direkonsiliasi atomik menjadi parent+28 child dan rerun 29 unchanged; 28 positive + 28 negative lulus. Production tetap menunggu preflight/approval |
| JRN-B-004 | RBAC schema versi 2 | `PARTIAL_PASS` | Canonical key + internal legacy alias fail-closed; migrasi role production memerlukan review/approval |
| JRN-D-001 | Design system dan responsive shell | `IMPLEMENTED_UNVERIFIED` | Token, focus, touch target, reduced motion, mobile card mode masuk; visual matrix 390/768/1024/1440 belum dijalankan |
| JRN-D-002 | 28 layar workflow khusus | `PARTIAL_PASS` | Shell dan operasi service tersedia; beberapa route masih berbagi workspace generik dan belum memenuhi seluruh state visual |
| JRN-F-001 | OJS 134/905 import | `PARTIAL_PASS` | Suite aktual lulus: preflight/dry-run/execute 134/905, domain transform, cancel/resume, retry file FAILED ke `LampiranJurnal`, checksum/anonymous stream/final reconciliation, dan legacy 7/37; long-tail manual collision UI masih terbuka |
| JRN-G-001 | Dependency/security/SBOM | `BLOCKED` | Triage severity 1/2, SBOM/license, pentest, dan sandbox eksternal belum dijalankan |
| JRN-G-002 | Performance/load/soak | `PARTIAL_PASS` | `PASS_LOCAL_SIT_V1`: dataset 100 jurnal/100k artikel/1m metadata file/10m usage/10k user, 8 thread × 300 detik, 1.608.291 operasi, warm p95 2,23 ms, analytic p95 1.652,22 ms, load p95 7,18 ms, 5.344,88 ops/s, error 0, heap peak 203.459.480 byte. Production-like HTTP/WAL, long soak 4–24 jam, dan SLA owner masih terbuka |
| JRN-G-003 | Formal SIT/UAT/pilot/cutover | `BLOCKED` | Technical test bukan sign-off owner; endpoint sandbox, actor UAT, dan approval cutover belum tersedia |
