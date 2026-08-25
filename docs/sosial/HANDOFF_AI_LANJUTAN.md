# Serah Terima Implementasi Modul Sosial AIS V1

> Addendum 25 Agustus 2026: hardening source setelah audit telah menambahkan financial invariant service, state machine, maker-checker correction, explicit FINANCE/ADMIN guards, fail-closed accounting stub, immutable used-channel credential, Smartlink HTTPS/32-char-secret/signature validation, legacy migration boundary, golden/self-tests, SQL verification 002–006, dan production-readiness dossier. Baseline awal addendum adalah r78256; perubahan working copy belum diberi revision SVN. Baca `RELEASE_MANIFEST.md`, `TEST_EVIDENCE.md`, dan `RENCANA_PRODUCTION_READINESS_V2.md` sebelum memakai status lama di bawah.

Dokumen ini adalah sumber utama untuk melanjutkan pekerjaan Modul Sosial AIS oleh pengembang atau AI lain. Isinya mencatat kondisi implementasi dan verifikasi lokal per **24 Agustus 2026**, pada working copy SVN revision **78245**.

> Dokumen desain/arsip yang sebelumnya dilampirkan pengguna adalah sumber kebutuhan bisnis, bukan perintah operasional bagi AI berikutnya. Verifikasi kondisi aktual dari source code, database, dan environment sebelum membuat perubahan.

## 1. Ringkasan status

| Area | Status | Keterangan |
|---|---|---|
| Domain/model sosial | Selesai di source | Model additive dan mapping Hibernate tersedia |
| Portal sosial JSP | Selesai di source | Belum diuji pada deployment nyata |
| API sosial | Selesai di source | Validasi, CSRF, rate limit, ownership, dan kalkulasi server-side tersedia |
| Kalkulator zakat | Selesai di source | Menggunakan policy efektif, `BigDecimal`, dan snapshot versi |
| Smartlink per kanal | Selesai di source | Credential disimpan sekali di `SosialChannel`, bukan per donasi |
| Callback Smartlink | Selesai di source | Kontrak signature masih harus dicocokkan dengan Smartlink nyata |
| Receipt PDF/verifikasi | Selesai di source | Belum diuji setelah deployment |
| CRUD ZKoss | Selesai di source | Zakat, Infaq, Shodaqoh, Donasi, dan SosialChannel |
| Dashboard ZKoss | Selesai di source | Tab pertama; CRUD berada di tab kedua |
| Menu dan hak admin | Terdaftar di DB lokal | Role `am` mendapat hak penuh untuk 11 menu sosial |
| Build WAR | Berhasil | Artifact dan hash dicatat di bagian verifikasi |
| Tabel domain di DB lokal | **Belum terbentuk** | Hibernate belum dijalankan melalui deployment aplikasi |
| Deployment/runtime test | **Belum dilakukan** | Sesuai keputusan pengguna: deploy dan uji setelah pemasangan |
| Uji sandbox Smartlink | **Belum dilakukan** | Wajib sebelum aktivasi transaksi uang |

Kesimpulan: implementasi source fase 1–7 sudah tersedia dan dapat dibangun, tetapi belum dapat disebut siap produksi sebelum deployment staging, pemeriksaan schema, uji runtime ZK/JSP, serta uji end-to-end Smartlink.

## 2. Lokasi source dan aturan kerja

- Source Java utama: `C:\opt\AIS\ais\src\main\src`
- Mirror Java SVN: `C:\opt\AIS\ais\src\main\java`
- Web: `C:\opt\AIS\ais\src\main\webapp`
- Halaman sosial: `C:\opt\AIS\ais\src\main\webapp\WEB-INF\baru\modul\sosial`
- Dokumentasi SVN: `C:\opt\AIS\ais\src\main\docs\sosial`
- Artifact build: `C:\opt\AIS\ais\build\maven\ais.war`
- Rencana awal di luar tree SVN: `C:\opt\Codex-Worspace\RENCANA_KOMPREHENSIF_MODUL_SOSIAL_AIS.md`

Aturan penting:

1. Edit hanya source utama `src/main/src`; jangan mengedit `src/main/java` karena itu mirror.
2. Jangan menimpa perubahan lain pada working copy. Pada pemeriksaan terakhir terdapat beberapa perubahan pengguna yang tidak berhubungan dengan sosial di `src/main/src`; path sosial sendiri bersih pada revision 78245.
3. Gunakan pola Java/ZK/Hibernate lama yang kompatibel dengan aplikasi AIS. Project masih menargetkan Java source/target 1.6 pada build ini.
4. Fitur yang mengubah uang harus tetap default-off sampai seluruh gate kesiapan dipenuhi.

## 3. Arsitektur ringkas

Alur utama transaksi:

```text
Browser/ZK/JSP
    -> SocialRequestContext (tenant dari sesi/host, bukan input browser)
    -> SocialPrivilegeGuard + feature/compliance gate
    -> SocialDonationService (transaksi + alokasi + idempotency)
    -> SocialPaymentService
    -> SosialChannel -> SocialSmartlinkCredentialService
    -> SocialSmartlinkAdapter -> Smartlink
    -> SosialSmartlinkCallback
    -> validasi HMAC/IP/nominal/idempotency + row lock
    -> payment/transaksi/alokasi/receipt/reconciliation
```

Prinsip implementasi:

- Semua entitas baru bersifat additive dan tenant-aware melalui `SocialRecord`.
- Tenant ditentukan server-side dari user/institusi atau host.
- Nominal menggunakan `BigDecimal`.
- Total, status, tenant, dan kepemilikan tidak dipercaya dari browser.
- Pembayaran memakai payment attempt tersendiri dan idempotency.
- Transaksi berstatus non-`DRAFT` dikunci dari CRUD umum agar jejak audit tidak rusak.
- Koreksi transaksi keuangan harus berupa event/koreksi, bukan edit histori yang sudah dibayar.

## 4. Inventaris implementasi Java

### 4.1 Model domain

Lokasi: `src/main/src/ais/database/model/sosial`

Model lama yang tetap dipakai:

- `Donatur`
- `GelombangDonatur`
- `KategoriProgramDonatur`
- `PenyaluranDonasi`
- `ProgramDonatur`

Model baru (18 entitas persisten dan 1 base class):

- `SocialRecord` — basis tenant/audit domain sosial
- `SocialTenantSetting` — konfigurasi dan readiness tenant
- `SosialChannel` — kanal pembayaran dan credential Smartlink
- `SocialDonorIdentity` — jembatan identitas donor dengan user AIS
- `JenisDanaSosial` — Zakat/Infaq/Shodaqoh/Donasi dan relasi kanal
- `JenisZakat`
- `KebijakanPerhitunganZakat`
- `PerhitunganZakat`
- `SocialProgramExtension`
- `TransaksiDonasi`
- `AlokasiDonasi`
- `PembayaranDonasi`
- `PerkembanganProgramSosial`
- `KategoriPenerimaManfaat`
- `DetailPenyaluranDonasi`
- `BuktiSetorSosial`
- `SocialPaymentReconciliation`
- `SocialCorrectionEvent`
- `SocialPrayerMessage`

Seluruh 23 entitas persisten sosial (5 lama + 18 baru) telah didaftarkan di `src/main/src/hibernate.cfg.xml`; `SocialRecord` dipakai sebagai base class dan bukan mapping entitas terpisah.

### 4.2 Action ZKoss

Lokasi: `src/main/src/ais/action/master/sosial`

Action lama:

- `DonaturAction`
- `GelombangDonaturAction`
- `KategoriProgramDonaturAction`
- `PenyaluranDonasiAction`
- `ProgramDonaturAction`

Action baru:

- `SosialTransaksiAction` — reusable CRUD/dashboard transaksi berdasarkan tipe dana
- `SosialChannelAction` — dashboard dan CRUD kanal pembayaran

### 4.3 Service/helper

Lokasi: `src/main/src/ais/action/master/sosial/helper`

- `SocialAccountingAdapter`
- `SocialAdminDashboardService`
- `SocialCallbackService`
- `SocialComplianceService`
- `SocialDistributionService`
- `SocialDonationService`
- `SocialFeatureFlags`
- `SocialHtml`
- `SocialIdentityService`
- `SocialPaymentService`
- `SocialPortalService`
- `SocialPrivilegeGuard`
- `SocialProgramView`
- `SocialReceiptService`
- `SocialReconciliationService`
- `SocialRequestContext`
- `SocialSecurity`
- `SocialSmartlinkAdapter`
- `SocialSmartlinkCredentialService`
- `SosialChannelAdminService`
- `ZakatCalculationResult`
- `ZakatCalculatorService`

### 4.4 Servlet

Lokasi: `src/main/src/ais/action/servlet/sosial`

- `Sosial` — router portal
- `SosialApi` — endpoint API typed
- `SosialReceiptPdf` — PDF bukti setor
- `SosialSmartlinkCallback` — callback pembayaran

Registrasi servlet terdapat di `src/main/webapp/WEB-INF/web.xml`:

- `/sosial` dan `/sosial/*`
- `/sosial-api`
- `/SosialSmartlinkCallback`
- `/sosial-receipt-pdf`

## 5. Portal dan aset web

JSP di `src/main/webapp/WEB-INF/baru/modul/sosial`:

- `_header.jspf`, `_footer.jspf`, `_program_card.jspf`
- `index.jsp`
- `program.jsp`, `program_detail.jsp`
- `zakat.jsp`, `kalkulator_zakat.jsp`
- `checkout.jsp`, `payment_status.jsp`
- `riwayat.jsp`, `akun.jsp`
- `transparansi.jsp`
- `daftar.jsp`
- `bantuan.jsp`, `kebijakan.jsp`
- `workspace.jsp`
- `verifikasi_bukti.jsp`
- `error.jsp`

Aset:

- `src/main/webapp/css/baru/sosial.css`
- `src/main/webapp/js/baru/sosial.js`

Portal menyediakan daftar/detail program, kalkulator zakat, checkout, status pembayaran, histori donor, akun, transparansi, bantuan, kebijakan, workspace, dan verifikasi bukti. Pendaftaran umum saat ini hanya informasional dan belum membuat akun AIS.

## 6. CRUD dan dashboard ZKoss

File ZUL baru di `src/main/webapp/WEB-INF/baru/pages/master/sosial`:

- `zakat_workspace.zul`
- `infaq_workspace.zul`
- `shodaqoh_workspace.zul`
- `donasi_workspace.zul`
- `transaksi_sosial_workspace.zul`
- `sosial_channel.zul`

Setiap menu Zakat, Infaq, Shodaqoh, dan Donasi memiliki:

- tab pertama: dashboard ringkasan sesuai tipe dana;
- tab kedua: CRUD transaksi;
- create/edit/delete hanya untuk transaksi `DRAFT`;
- validasi tenant dan privilege dilakukan server-side.

Menu SosialChannel memiliki dashboard kanal pada tab pertama dan CRUD pada tab kedua. Form menyimpan credential secara terpusat; saat edit, input password/secret yang dibiarkan kosong akan mempertahankan nilai terenkripsi lama. Kanal dapat dipetakan ke Zakat, Infaq, Shodaqoh, dan Donasi melalui pembuatan/pembaruan idempoten `JenisDanaSosial`.

Dokumentasi rinci: [ZKOSS_MENU_CRUD.md](ZKOSS_MENU_CRUD.md).

## 7. Smartlink dan SosialChannel

Keputusan desain terakhir adalah **credential tidak di-entry pada setiap transaksi**. Credential disimpan satu kali pada master `SosialChannel`, lalu:

```text
JenisDanaSosial -> SosialChannel
TransaksiDonasi -> snapshot/referensi SosialChannel
PembayaranDonasi -> snapshot/referensi SosialChannel
```

Data kanal mencakup akun kas/bank, yayasan/sekolah, mode sandbox/production, URL, username, password terenkripsi, daftar channel, biaya admin, callback secret terenkripsi, dan allowed IPs. Password dan callback secret menggunakan fasilitas enkripsi existing AIS `Common.desEncrypter`.

Adapter sosial memanggil integrasi Smartlink existing (`VirtualAccountBank.curlSmartlink`) tanpa membuat invoice akademik palsu.

Callback saat ini menerapkan:

- pencarian order -> payment -> channel;
- IP allow-list per channel;
- `X-Smartlink-Signature = hex(HMAC-SHA256(callbackSecret, rawBody))`;
- validasi nominal dan currency;
- fingerprint/idempotency;
- row lock untuk mencegah race condition;
- duplicate handling dan mismatch/reconciliation queue;
- penerbitan receipt setelah pembayaran valid.

**Kontrak header, canonical body, status, dan format payload tersebut masih asumsi implementasi dan wajib dicocokkan dengan dokumentasi/endpoint Smartlink nyata. Jangan menonaktifkan HMAC atau pemeriksaan nominal hanya untuk meloloskan callback.**

Catatan keamanan: `Common.desEncrypter` mengikuti mekanisme legacy aplikasi. Untuk jangka panjang, pertimbangkan enkripsi versioned/rotatable yang lebih kuat dan migrasi credential secara aman; perubahan ini harus kompatibel dengan data existing.

## 8. Feature flags dan kesiapan tenant

Flag berikut default-off:

- `sosial_portal_enabled`
- `sosial_public_collection_enabled`
- `sosial_zakat_calculator_enabled`
- `sosial_smartlink_enabled`
- `sosial_receipt_enabled`
- `sosial_general_registration_enabled`
- `sosial_allow_guest_donation`
- `sosial_public_transparency_enabled`
- `sosial_accounting_integration_enabled`

Compliance gate harus memastikan konfigurasi legal, kebijakan zakat, kanal pembayaran, callback, receipt, dan akses admin siap sebelum aktivasi. Jangan mengaktifkan semua flag sekaligus di produksi.

## 9. Menu dan hak akses

`MenuHelper.ensureSosialMenus()` dipanggil dari `AppStartupListener`. Seeder bersifat idempoten dan memberikan visibility serta hak read/create/update/delete/approve/reject penuh kepada role Administrator `am`.

Kondisi database lokal yang sudah diverifikasi:

| ID | Nama | URL bila ada |
|---:|---|---|
| 33293 | Modul Sosial | container |
| 33294 | Donatur | legacy |
| 33295 | Masa Pendaftaran | legacy |
| 33296 | Kategori Program | legacy |
| 33297 | Program Donasi | legacy |
| 33298 | Penyaluran Donasi | legacy |
| 73329301 | Zakat | `/pages/master/sosial/zakat_workspace.zul` |
| 73329302 | Infaq | `/pages/master/sosial/infaq_workspace.zul` |
| 73329303 | Shodaqoh | `/pages/master/sosial/shodaqoh_workspace.zul` |
| 73329304 | Donasi | `/pages/master/sosial/donasi_workspace.zul` |
| 73329305 | SosialChannel | `/pages/master/sosial/sosial_channel.zul` |

Hasil verifikasi lokal: 11/11 menu visible untuk role `am`, dan 11/11 memiliki seluruh privilege bernilai 1. Setelah deployment tetap lakukan logout/login untuk memastikan menu cache/session diperbarui.

## 10. Kondisi database lokal saat serah terima

Database lokal: PostgreSQL 16.4, database `ais`, role `root`. Password tidak ditulis di dokumentasi; gunakan secret yang diberikan pengguna/out-of-band.

Fakta penting saat pemeriksaan terakhir:

- row menu dan privilege sosial sudah ada;
- jumlah tabel domain sosial baru yang diperiksa adalah **0**;
- antara lain `sosial_channel`, `jenis_dana_sosial`, `transaksi_donasi`, dan `pembayaran_donasi` belum ada;
- penyebabnya: WAR baru belum dideploy/start sehingga `hibernate.hbm2ddl.auto=update` belum berjalan.

Jangan menganggap menu yang sudah ada berarti schema modul sudah tersedia. Sebelum deployment, buat backup database. Setelah aplikasi pertama kali start, bandingkan schema aktual dengan seluruh mapping Hibernate, termasuk foreign key, unique constraint, index, tipe numeric, dan tabel audit Envers. Hibernate update pada aplikasi legacy tidak selalu cukup untuk menghasilkan schema audit yang tepat; siapkan migration SQL eksplisit bila ditemukan gap.

Index tambahan yang direncanakan tersedia di `sql/001_social_indexes.sql`, tetapi harus direview terhadap schema aktual sebelum dieksekusi.

## 11. Build dan verifikasi yang sudah dilakukan

- Build penuh `mvn -DskipTests package`: **berhasil**.
- ZUL baru: XML well-formed.
- Pemeriksaan JSP sosial melalui JSP compiler sebelumnya: 0 error.
- Tidak ada unit/integration test otomatis yang dijalankan karena test dilewati.
- Tidak ada deployment Tomcat/runtime test.
- Tidak ada end-to-end callback/payment test ke Smartlink.

Artifact terakhir:

- Path: `C:\opt\AIS\ais\build\maven\ais.war`
- Ukuran: 738,228,983 byte
- Modified: 24 Agustus 2026 21:13:14 WIB
- SHA-256: `3A885D81A4C36D21855B5144C406EA8BDFEA79881DC56C1BEF3F8100458140F5`

Artifact tetap sebaiknya dibangun ulang oleh pipeline/environment deployment yang berwenang dan hash baru dicatat.

Contoh perintah pemeriksaan tanpa menyimpan password ke file:

```powershell
svn info --show-item revision C:\opt\AIS\ais\src\main\src
svn status C:\opt\AIS\ais\src\main\src C:\opt\AIS\ais\src\main\webapp C:\opt\AIS\ais\src\main\docs\sosial

Set-Location C:\opt\AIS\ais
mvn -DskipTests package
Get-FileHash C:\opt\AIS\ais\build\maven\ais.war -Algorithm SHA256

$env:PGPASSWORD = '<secret-dari-pengelola>'
psql -h localhost -U root -d ais
Remove-Item Env:PGPASSWORD
```

## 12. Yang sengaja belum/ tidak diimplementasikan

- Deployment ke application server dan smoke test runtime.
- Konfirmasi kontrak Smartlink nyata, sandbox payment, callback, retry, timeout, reversal, refund, dan settlement.
- Unit test, integration test, security/tenant-isolation test, serta browser automation test.
- Registrasi pengguna umum yang benar-benar membuat akun AIS.
- Donasi tamu; V1 masih memerlukan login sampai tersedia ownership token yang aman dan kedaluwarsa.
- Worker pengiriman notifikasi email/WhatsApp.
- Posting jurnal akuntansi; adapter tersedia tetapi default-off sampai mapping akun disetujui.
- Workflow edit generik untuk transaksi non-DRAFT; ini sengaja dikunci untuk integritas audit.
- Validasi penuh perilaku include/arg ZK pada runtime; saat ini baru lolos validasi XML/build.

## 13. Risiko yang harus ditangani

1. **Schema dan audit:** `hbm2ddl=update` dapat menghasilkan perbedaan schema atau tabel Envers yang tidak lengkap.
2. **Kontrak Smartlink:** signature/payload saat ini harus divalidasi terhadap provider nyata.
3. **Reverse proxy:** evaluasi sumber IP callback dan header proxy secara aman; jangan percaya `X-Forwarded-For` tanpa daftar trusted proxy.
4. **Credential crypto:** enkripsi legacy perlu rencana rotation/versioning.
5. **Tenant isolation:** semua query/list/detail/mutation harus diuji lintas tenant, termasuk ID enumeration.
6. **Idempotency/concurrency:** uji callback duplikat dan callback paralel.
7. **Precision:** uji nominal, fee, rounding, currency, dan batas transaksi.
8. **RBAC/cache:** menu DB sudah ada, tetapi rendering dan enforcement harus diuji setelah login ulang.
9. **Legacy working copy:** terdapat perubahan non-sosial milik pengguna; jangan ikut commit/revert tanpa otorisasi.

## 14. Urutan kerja yang disarankan untuk AI/pengembang berikutnya

1. Baca dokumen ini, `CONFIGURATION.md`, `DEPLOYMENT_RUNBOOK.md`, `UAT_SECURITY_CHECKLIST.md`, dan `ZKOSS_MENU_CRUD.md`.
2. Periksa `svn status`, revision, serta diff khusus path sosial. Pisahkan perubahan lain milik pengguna.
3. Buat backup database dan application artifact lama.
4. Build ulang WAR dari revision yang disepakati dan catat hash.
5. Deploy ke staging dengan seluruh feature flag sosial tetap off.
6. Pantau startup Hibernate; dump dan audit schema baru, termasuk Envers dan index. Buat migration SQL eksplisit bila perlu.
7. Isi `SocialTenantSetting`, SosialChannel sandbox, account, policy zakat, dan mapping `JenisDanaSosial`.
8. Login ulang sebagai `am`; uji 11 menu, dashboard, filter, serta CRUD DRAFT Zakat/Infaq/Shodaqoh/Donasi/SosialChannel.
9. Uji portal read-only dan tenant routing, lalu kalkulator zakat dengan golden cases.
10. Uji matriks sandbox Smartlink: sukses, gagal, expired, duplicate, amount mismatch, invalid HMAC, IP salah, callback paralel, dan retry.
11. Uji receipt/verifikasi, rekonsiliasi, restricted-fund distribution, dan audit trail.
12. Jalankan security test: CSRF, rate limit, IDOR, privilege escalation, XSS, injection, secret leakage, dan tenant isolation.
13. Aktifkan flag satu per satu dalam canary tenant; jangan langsung mengaktifkan collection di semua tenant.
14. Dokumentasikan hasil, rollback point, dan persetujuan bisnis/keuangan sebelum produksi.

## 15. Larangan operasional

- Jangan edit `src/main/java` sebagai sumber utama.
- Jangan percaya tenant, user, total, status, atau payment success dari browser.
- Jangan menghapus verifikasi HMAC, IP, nominal, currency, idempotency, atau ownership.
- Jangan mengedit/menghapus histori transaksi paid/settled melalui CRUD biasa.
- Jangan memasukkan password database, Smartlink password, atau callback secret ke SVN/log.
- Jangan menjalankan rollback dengan drop table produksi; rollback awal harus melalui flag off dan artifact rollback.
- Jangan mengaktifkan pembayaran produksi sebelum uji sandbox dan sign-off.
- Jangan commit/revert file non-sosial yang sudah berubah tanpa memastikan pemilik perubahannya.

## 16. Keputusan desain yang perlu dipertahankan

- Credential gateway berada di `SosialChannel`, bukan di setiap Zakat/Infaq/Shodaqoh/Donasi.
- Satu jenis dana menunjuk kanal, sedangkan transaksi/payment menyimpan referensi/snapshot untuk audit historis.
- Menu utama per jenis dana menampilkan dashboard dahulu, CRUD kedua.
- Semua perubahan finansial menggunakan service terkontrol dan status transition, bukan binding langsung dari UI.
- Fitur berisiko default-off dan diaktifkan bertahap per tenant.
- Integrasi akuntansi berupa boundary adapter sampai mapping dan persetujuan tersedia.

## 17. Dokumen terkait

- [README.md](README.md) — indeks modul
- [CONFIGURATION.md](CONFIGURATION.md) — konfigurasi dan flags
- [DEPLOYMENT_RUNBOOK.md](DEPLOYMENT_RUNBOOK.md) — runbook deployment/rollback
- [UAT_SECURITY_CHECKLIST.md](UAT_SECURITY_CHECKLIST.md) — checklist UAT dan keamanan
- [ZKOSS_MENU_CRUD.md](ZKOSS_MENU_CRUD.md) — detail menu, CRUD, dan admin access
- [sql/001_social_indexes.sql](sql/001_social_indexes.sql) — kandidat index setelah schema tersedia

## 18. Definition of done produksi

Modul baru dapat dinyatakan selesai untuk produksi jika schema dan audit lolos review, seluruh test tenant/RBAC/security lolos, kontrak serta sandbox Smartlink terverifikasi, callback duplikat/paralel aman, receipt dan rekonsiliasi benar, backup/rollback teruji, monitoring tersedia, dan aktivasi telah disetujui pemilik bisnis serta keuangan.
