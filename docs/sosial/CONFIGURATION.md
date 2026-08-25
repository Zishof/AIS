# Konfigurasi Modul Sosial AIS

## Feature flags

Nilai aktif mengikuti nilai `Konfigurasi.AKTIF`. Semua default tidak aktif.

- `sosial_portal_enabled`
- `sosial_public_collection_enabled`
- `sosial_zakat_calculator_enabled`
- `sosial_smartlink_enabled`
- `sosial_receipt_enabled`
- `sosial_general_registration_enabled`
- `sosial_allow_guest_donation`
- `sosial_public_transparency_enabled`
- `sosial_accounting_integration_enabled`

Flag juga dapat disimpan dalam JSON `SocialTenantSetting.featureFlags`; nilai tenant mengungguli konfigurasi global.

## Master SosialChannel dan Smartlink

Credential tidak diisi pada donasi. Administrator mengisi satu record `SosialChannel`, kemudian setiap `JenisDanaSosial`—misalnya Zakat, Infaq, atau Shodaqoh—mereferensikan channel tersebut melalui `sosial_channel_id`. Transaksi dan payment attempt menyimpan relasi channel yang dipakai sebagai snapshot audit.

Kolom utama `public.sosial_channel`:

- tenant, kode, nama, keterangan, status/aktif, mode sandbox/production;
- akun Kas/Bank, Yayasan, dan Sekolah;
- provider dan flag aktivasi Smartlink;
- URL, username, password terenkripsi, daftar channel pembayaran, dan biaya admin;
- callback secret terenkripsi dan IP allow-list.

Password serta callback secret hanya ditulis melalui `SosialChannelAdminService`; nilai kosong ketika mengubah data berarti mempertahankan secret lama. Browser tidak pernah mengirim credential saat membuat donasi. Callback mencari payment berdasarkan `order_id`, mengambil `SosialChannel` milik payment tersebut, lalu menggunakan secret dan allow-list channel itu.

Channel yang sudah dipakai bersifat immutable untuk credential kritis. Untuk rotasi username, password, endpoint, atau callback secret, buat record channel baru (contoh `SMARTLINK_AMIL_V2`), pindahkan mapping jenis dana, dan pertahankan channel lama sampai seluruh order serta settlement selesai.

## RBAC

Role ID dipisah koma:

- `sosial_roles_view`
- `sosial_roles_operate`
- `sosial_roles_approve`
- `sosial_roles_finance`
- `sosial_roles_audit`
- `sosial_roles_admin`

Admin platform tetap dapat mengakses workspace. Guard diterapkan server-side; penyembunyian menu bukan kontrol keamanan. Posting penyaluran dan settlement memerlukan `FINANCE`, refund/reversal memakai maker `FINANCE` dan checker `APPROVE`, sedangkan backfill/channel memerlukan `ADMIN`.

## Scope yang dipaksa nonaktif

- Accounting berstatus `STUB_NOOP`; adapter selalu fail-closed meskipun flag accounting salah diaktifkan.
- Registrasi umum dan donasi tamu berstatus `OUT_OF_SCOPE_V1`; kedua flag wajib tetap off.
- Callback secret baru minimal 32 karakter, URL Smartlink wajib HTTPS, dan allowed IP wajib eksplisit.

## Tenant setting wajib sebelum collection

- `tenantKey` sesuai resolver.
- `operationMode` bukan `SANDBOX_ONLY`.
- Nomor/masa berlaku izin atau SK bila dipersyaratkan.
- Partner, kontak publik, versi privacy, dan versi terms.
- `publicCollectionEnabled`, `gatewayEnabled`, dan `receiptEnabled` setelah sign-off.
- Feature flags tenant.

## Policy zakat

Policy harus `APPROVED`, berada dalam rentang efektif, memiliki formula key allow-list, versi, rate, nisab, price source/reference, scale, rounding mode, sumber keputusan, reviewer, serta timestamp approval. Hindari policy approved yang overlap.

Formula key V1:

- `INCOME_MONTHLY`
- `INCOME_ANNUAL`
- `GOLD`
- `CASH_SAVINGS`
- `TRADE_BUSINESS`
- `MAAL_GENERIC`
- `FITRAH`
