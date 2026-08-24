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

## Smartlink

- `sosial_smartlink_create_order_url`
- `sosial_smartlink_username`
- `sosial_smartlink_password`
- `sosial_smartlink_channels`, contoh `VA_CIMB,VA_BRI`
- `sosial_smartlink_callback_secret`, minimal 16 karakter
- `sosial_smartlink_callback_allowed_ips`, daftar IP dipisah koma
- `online_smartlink_biaya_administrasi`

Username, password, dan callback secret tidak boleh memiliki default di source, JSP, JavaScript, atau dokumentasi operasional publik.

## RBAC

Role ID dipisah koma:

- `sosial_roles_view`
- `sosial_roles_operate`
- `sosial_roles_approve`
- `sosial_roles_finance`
- `sosial_roles_audit`
- `sosial_roles_admin`

Admin platform tetap dapat mengakses workspace. Guard diterapkan server-side; penyembunyian menu bukan kontrol keamanan.

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

