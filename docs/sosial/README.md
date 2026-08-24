# Modul Sosial AIS V1

Baseline implementasi: SVN revision 78235, 24 Agustus 2026.

## Lokasi

- Java/model/service: `src/main/src/ais/...`
- Hibernate mapping: `src/main/src/hibernate.cfg.xml`
- JSP: `src/main/webapp/WEB-INF/baru/modul/sosial`
- CSS/JS: `src/main/webapp/css/baru/sosial.css`, `src/main/webapp/js/baru/sosial.js`
- Dokumentasi: `src/main/docs/sosial`

`src/main/java` adalah mirror dan bukan target edit utama.

## Komponen yang diimplementasikan

- Domain additive untuk tenant, donor identity, jenis dana/zakat, policy dan snapshot kalkulasi, program extension, transaksi, alokasi, payment attempt, receipt, penyaluran typed, rekonsiliasi, koreksi, dan doa/pesan.
- `BigDecimal` untuk seluruh nominal baru.
- Tenant context yang ditentukan dari user/institusi atau host, bukan parameter browser.
- Identity resolver idempotent untuk pengguna AIS.
- Compliance gate dan feature flags default-off.
- Portal publik `/sosial`, program, kalkulator, checkout, status, riwayat, transparansi, bantuan, kebijakan, workspace, serta verifikasi bukti.
- API typed `/sosial-api` dengan CSRF, rate limit, validation, ownership, dan server-calculated total.
- Smartlink adapter khusus sosial tanpa membuat tagihan akademik palsu. Credential diisi sekali pada master `SosialChannel`; jenis dana dan transaksi hanya menyimpan referensinya.
- Callback `/SosialSmartlinkCallback` dengan IP allow-list, HMAC-SHA256, fingerprint, lock, amount check, duplicate handling, mismatch queue, allocation posting, dan receipt.
- PDF bukti setor on-demand di `/sosial-receipt-pdf`.
- Service posting penyaluran dengan restricted balance check.
- Service rekonsiliasi settlement dan accounting boundary yang default-off.

## Status aktivasi

Seluruh fitur mutasi uang harus tetap nonaktif sampai konfigurasi tenant, policy, legal, gateway, receipt, RBAC, dan callback tersedia. Tidak ada deployment atau callback end-to-end test pada implementasi ini; keduanya dilakukan setelah artifact dipasang pada environment target.

## Batasan sebelum pilot

- General registration belum membuat akun baru dari portal sosial. Halaman pendaftaran hanya menjelaskan kesiapan dan harus dihubungkan ke lifecycle registrasi AIS yang disetujui; flag tetap off.
- Donasi tamu belum diaktifkan. V1 mewajibkan login AIS sampai token kepemilikan transaksi tamu yang dapat kedaluwarsa tersedia.
- Notification sender/worker belum dihubungkan ke email/WhatsApp existing.
- Accounting adapter tidak memposting jurnal sebelum mapping akun disetujui dan flag diaktifkan.
- Kontrak header callback implementasi saat ini adalah `X-Smartlink-Signature = hex(HMAC-SHA256(secret SosialChannel transaksi, rawBody))`. Kontrak ini wajib dicocokkan dengan Smartlink saat deployment; jangan melonggarkan verifikasi untuk membuat callback lolos.
- CRUD master/workflow rinci tetap memerlukan menu/action Generic CRUD dan assignment role pada data konfigurasi AIS.

## Build lokal

Kelas baru telah dikompilasi terarah menggunakan `-source 1.6 -target 1.6`. Build penuh `mvn -DskipTests package` juga berhasil dan menghasilkan `build/maven/ais.war`. Artifact tetap harus dibangun ulang oleh pipeline AIS yang berwenang sebelum deployment produksi.
