# Hybrid Menu V2 — Test Report

Tanggal: 2026-08-08 (Asia/Jakarta)

## Build dan static verification

- Paket Drive: SHA-256 cocok untuk 8 dari 8 file.
- `git diff --check`: lulus.
- Generator invariant: `PASS: operational navigation remains Menu/RBAC-driven`.
- JavaScript: `node --check new-ui.js` lulus.
- Java targeted dari working copy SVN: `javac -source 1.6 -target 1.6` lulus.
- Self-test V1: `PASS New UI RBAC tree/route self-test`.
- Self-test V2: `PASS Hybrid Menu V2 tree/catalog/guard self-test`.
- JSPC Tomcat 7: 12 JSP berubah, 0 error.
- Ant official `ais/ant/build.xml`, target `clean compile`: 6.561 source berhasil dikompilasi.

Catatan build Windows: build script official tidak mempunyai atribut `encoding`; pemanggilan pertama memakai Cp1252 dan berhenti pada karakter Unicode existing di `NilaiObeAction.java`. Build diulang tanpa mengubah source menggunakan `ANT_OPTS=-Dfile.encoding=UTF-8 -Xmx2048m` dan selesai `BUILD SUCCESSFUL`.

## Matriks database lokal (`ais`, read-only)

| Role | Assigned aktif | READ | READ=0 | Branch sidebar | Leaf katalog | Structural-only | Orphan visible |
|---|---:|---:|---:|---:|---:|---:|---:|
| `am` | 562 | 538 | 24 | 81 | 477 | 79 | 10 |
| `Akademik` | 84 | 75 | 9 | 8 | 68 | 8 | 15 |
| `mhs` | 34 | 33 | 1 | 3 | 30 | 3 | 6 |
| `kpsk` | 106 | 46 | 60 | 8 | 46 | 8 | 0 |

Role `kpsk` tersedia sebagai data role sekolah tetapi berstatus nonaktif pada database lokal; service produksi tetap menolak role nonaktif. Query lengkap berada di `webapp/WEB-INF/new/_shared/tests/new-ui-rbac-diagnostic.sql`.

## Acceptance yang dicakup otomatis

- branch/leaf ditentukan setelah filter assignment, READ, scope, dan route;
- leaf tidak masuk tree sidebar;
- parent READ=0 dengan descendant readable menjadi structural-only;
- root leaf/orphan masuk grup virtual `Menu Lainnya`;
- READ=0 dan menu unassigned menghasilkan `FORBIDDEN` pada direct guard;
- katalog direct vs descendant dan sort diuji;
- cycle/orphan/duplicate didiagnosis tanpa recursion tanpa batas;
- Ctrl+K menerima snapshot authorized yang sama;
- `module`/`page` hanya diterima sesudah `menuId` tervalidasi;
- cache key menyertakan user, role, scope, dan global version;
- partial tetap memakai `pageContext.include(target, true)`.

Pengujian tidak melakukan deploy ke server dan tidak menjalankan mutasi database. Uji klik dengan sesi login pada deployment target tetap menjadi smoke test operasional setelah branch dipasang di environment aplikasi.
