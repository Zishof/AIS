# UAT, Security, Accessibility, dan Financial Controls

## Compatibility

- [ ] Donatur, ProgramDonatur, PenyaluranDonasi legacy berfungsi.
- [ ] SOP, revision audit, upload, cetak, dan `/new?module=sosial` berfungsi.
- [ ] Existing data tidak berubah.

## Domain dan tenant

- [ ] Hibernate memetakan seluruh tabel baru.
- [ ] Query program, policy, donor, transaksi, payment, receipt, dan statistik tenant-scoped.
- [ ] Manipulasi ID lintas tenant ditolak.
- [ ] Identity resolver tidak membuat duplikat.

## Calculator

- [ ] Golden case tujuh formula minimum.
- [ ] Below/at/above nisab.
- [ ] Monthly/annual, rounding, upper bound, negative input.
- [ ] Policy version dan snapshot lama stabil.
- [ ] Tidak ada nisab/harga fitrah current yang hard-coded.

## Checkout dan Smartlink

- [ ] CSRF, rate limit, ownership, idempotency double-click/refresh.
- [ ] Fee dan kontribusi terpisah dari gross donation.
- [ ] Payment request tersimpan sebelum external call.
- [ ] Callback HMAC/source/amount/currency/tenant valid.
- [ ] Duplicate callback hanya satu allocation/receipt.
- [ ] Redirect tidak mengubah status menjadi PAID.
- [ ] Mismatch masuk reconciliation exception.
- [ ] Secret dan payload sensitif tidak masuk log.

## Distribution dan reconciliation

- [ ] Total allocation sama dengan gross donation.
- [ ] Restricted compatibility diterapkan.
- [ ] Concurrent posting tidak membuat saldo negatif.
- [ ] Posted distribution immutable.
- [ ] Refund/reversal/correction memerlukan alasan dan approval.
- [ ] Expected payment = settlement = bank = accounting dapat direkonsiliasi.

## Privacy dan security

- [ ] No donor/beneficiary PII pada publik.
- [ ] No IDOR riwayat/receipt/payment.
- [ ] XSS, mass assignment, path traversal, upload spoofing, replay diuji.
- [ ] Private page dan API memakai `no-store`.
- [ ] Prayer tidak auto-publish.

## Accessibility dan responsive

- [ ] Keyboard, visible focus, skip link, live result/status.
- [ ] Screen reader kalkulator/payment.
- [ ] Zoom 200%, reflow 360 px, target sentuh minimal 44 px.
- [ ] 1920, 1440, 1366, 1024, 768, 390, 360 px.
- [ ] Reduced motion dan tidak ada status berbasis warna saja.

## Release gate

- [ ] WAR build dan Tomcat start lulus.
- [ ] Callback deployment test evidence tersedia.
- [ ] Backup/restore dan rollback rehearsal lulus.
- [ ] Tidak ada defect P0 atau selisih finansial tanpa penjelasan.
- [ ] Legal/syariah/finance/security sign-off tersedia.

