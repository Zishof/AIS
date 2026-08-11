# 00 — Baseline P0 Pendaftaran Tenant

Tanggal: 2026-08-12

## Workspace (diverifikasi perintah nyata)

```
cd C:\opt\AIS\ais\src\main
git status --short        -> M src/ais/action/servlet/PosApi.java  (milik sesi paralel — TIDAK disentuh/di-commit pekerjaan ini)
git branch --show-current -> feat/new-ui-rbac-role-user
git remote -v             -> origin https://github.com/Zishof/AIS.git
git rev-parse HEAD        -> aaf825b5987b1ddfced5ac8554c638afb5549fa6
git log -1 --oneline      -> aaf825b5 inventory_sales server: aksi si_import_legacy -- impor master DBF legacy (FoxPro)
```

### Keputusan branch (deviasi sadar dari dokumen master §1.1)

Dokumen menyarankan branch kerja terpisah `feat/pendaftaran-tenant-multi-jenis-usaha`. TIDAK dilakukan, alasan:
1. Working tree ini DIPAKAI BERSAMA sesi agent lain yang aktif (ada perubahan uncommitted `PosApi.java` milik mereka) — `git checkout -b` membawa/mengganggu perubahan itu.
2. Seluruh program multi-fase repo ini (inventory_sales P0-P3, apotik, dll.) sudah berjalan di `feat/new-ui-rbac-role-user` sebagai branch delivery tunggal, ter-mirror otomatis ke SVN near-real-time.
3. Mitigasi keamanan perubahan: SETIAP commit memakai pathspec eksplisit (`git commit -F <msg> -- <file...>`), tidak pernah `-A`; tidak ada reset/force-push.

## Baseline build

- **Tidak ada `build.xml` Ant** di `C:\opt\AIS\ais` maupun `src\main` (instruksi `ant clean; ant compile` pada dokumen tidak berlaku di workspace ini).
- Build kanonik: **`mvn -o compile`** dari `C:\opt\AIS\ais` (pom.xml di root ais, BUKAN src\main).
- Hasil baseline (sebelum perubahan apa pun): **EXIT=0** — hanya warning JVM Maven (`sun.misc.Unsafe` dari guice bawaan Maven 3.9.11), bukan warning proyek.
- Java: target kompatibilitas 1.8 dengan gaya source 1.7 (tanpa lambda/stream/Optional — konvensi repo).
- Test otomatis existing: tidak ada test-suite JUnit yang jalan via mvn di modul ini (verifikasi: tidak ada konfigurasi surefire aktif utk paket terkait); pengujian fase-fase ini memakai kelas verifikasi mandiri + UAT (pola program inventory_sales sebelumnya).

## Baseline runtime (menunggu UAT server)

- Startup Tomcat + halaman `ebisnis.jsp` + daftar/login existing + dashboard Brand/Toko/Mesin POS/Investor/Manajemen: TIDAK dapat diverifikasi dari workspace build ini (butuh deploy dev). Ditandai **UAT_REQUIRED** — dicatat di handover; restart/deploy Tomcat adalah wewenang operator (konvensi program ini).
- Audit duplikasi data `public.pendaftar` (email/domain ganda) butuh akses DB deployment → dimasukkan ke langkah backfill P4/P6 (`migration-exceptions.csv`), bukan diklaim sekarang.

## Mode kompatibilitas awal

Konfigurasi `LEGACY` dijadikan default deployment (provisioning schema OFF; pendaftaran tetap menghasilkan permohonan+registry+scope Pendaftar) — perilaku existing tidak berubah diam-diam, sesuai §3.3 dokumen master.
