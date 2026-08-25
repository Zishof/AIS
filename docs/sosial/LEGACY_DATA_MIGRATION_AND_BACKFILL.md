# Legacy Migration dan Backfill

`SocialLegacyMigrationService` menyediakan preview dan backfill idempoten dengan tenant eksplisit. Service tidak pernah menebak tenant dari record legacy.

Mapping: `Donatur -> SocialDonorIdentity`, `ProgramDonatur -> SocialProgramExtension`. Program baru tetap DRAFT, memakai default `JenisDanaSosial` yang dipilih admin, dan tidak otomatis dipublikasikan. `ProgramDonatur.donaturs` hanya dilaporkan oleh preview; CSV tidak dinormalisasi otomatis karena maknanya perlu keputusan data owner. Penyaluran legacy juga tidak dikonversi menjadi nilai finansial tanpa nominal/source allocation yang dapat direkonsiliasi.

Prosedur: jalankan `006_preview_legacy_backfill.sql` read-only; tentukan tenant/default fund; selesaikan nama kosong/ambigu; backup; panggil service dengan `execute=true`; simpan rejection dan mapping report; jalankan ulang untuk membuktikan idempotency; rekonsiliasi count/total. Gunakan forward-fix, bukan delete rollback.
