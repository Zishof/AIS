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

## Smartlink multi-credential per donasi

Setiap transaksi menyimpan `smartlinkCredentialCode`. Kode dipilih server-side dan disalin ke payment attempt sehingga credential tidak berubah walaupun konfigurasi routing kemudian diperbarui.

- `sosial_smartlink_profiles_<tenant-slug>`: pool kode profil untuk tenant, misalnya `amil_a,amil_b`.
- `sosial_smartlink_profiles`: fallback pool bila konfigurasi tenant tidak tersedia.
- `sosial_smartlink_fund_<jenisDanaId>_profile`: profil tetap untuk suatu jenis dana; mengungguli pool.
- `sosial_smartlink_profile_<code>_url`
- `sosial_smartlink_profile_<code>_username`
- `sosial_smartlink_profile_<code>_password`
- `sosial_smartlink_profile_<code>_channels`, contoh `VA_CIMB,VA_BRI`
- `sosial_smartlink_profile_<code>_fee`
- `sosial_smartlink_profile_<code>_callback_secret`, minimal 16 karakter
- `sosial_smartlink_profile_<code>_callback_allowed_ips`, daftar IP dipisah koma

Tanpa mapping jenis dana, profil dipilih secara deterministik dari hash nomor transaksi. Browser tidak boleh mengirim atau memilih kode profil. Callback mencari payment berdasarkan `order_id`, lalu memverifikasi signature dan IP menggunakan profil yang dibekukan pada payment tersebut.

Username, password, dan callback secret tidak memiliki default di source, JSP, JavaScript, atau dokumentasi operasional publik. Batasi akses tabel konfigurasi dan log; nilai secret tidak boleh ikut response atau audit payload.

Kode profil bersifat versioned dan immutable setelah dipakai membuat order. Untuk rotasi username, password, endpoint, atau callback secret, buat kode baru (contoh `amil_a_v2`), masukkan ke pool, hentikan routing baru ke kode lama, lalu pertahankan konfigurasi lama sampai seluruh order dan settlement-nya selesai.

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
