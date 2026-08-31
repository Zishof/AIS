# 06 — Kewenangan peran di dalam tenant (FASE P8)

Delapan peran §16, dan gerbang yang menegakkannya pada setiap aksi `si_*`.

> **Lapisan ini menambah, tidak menggantikan.** Gerbang menu di `PosApi` dan gerbang aktor di
> `SalesInventoryApiDispatcher` tetap berlaku. Ketiganya harus lolos; satu pun gagal berarti
> ditolak.

## Peran dan wilayahnya

| Peran | Wilayah |
|---|---|
| `OWNER` | seluruhnya |
| `PEMILIK_SALES_INVENTORY` | seluruhnya |
| `ADMIN_TENANT` | seluruhnya **kecuali impor legacy** |
| `GUDANG` | stok penuh; produk tulis; beli/jual/mitra baca |
| `PEMBELIAN` | beli penuh; hutang, mitra, harga tulis; produk/stok/laporan baca |
| `SALES_KELILING` | trip, jual, piutang tulis; mitra/produk/harga/stok **baca saja** |
| `KEUANGAN` | keuangan, hutang, piutang penuh; laporan setuju; sisanya baca |
| `AUDITOR` | **membaca saja**, seluruh wilayah |

### Tiga pembatasan yang paling menentukan

**`AUDITOR` tidak dapat menulis apa pun.** Bukan sekadar dibatasi — nol tulis, nol setuju, di
seluruh wilayah. Auditor yang dapat mengubah kehilangan artinya sebagai auditor.

**`SALES_KELILING` tidak dapat mengubah harga.** Ia menulis trip, nota, dan piutang, tetapi
harga hanya dibacanya. Kewenangan mengubah harga di tangan orang lapangan adalah celah yang
paling mudah disalahgunakan.

**`ADMIN_TENANT` tidak dapat mengimpor legacy.** Impor menulis puluhan ribu baris sekaligus
dan tidak dapat dibatalkan sebagian; ia milik pemilik usaha, bukan administrator hariannya.

## Diturunkan dari nama aksi, bukan didaftar

Ada **86 aksi** `si_*` dan jumlahnya bertambah. Mendaftarnya satu per satu berarti setiap
aksi baru harus diingat seseorang — dan yang lupa **tidak menimbulkan galat**, hanya lubang
izin yang diam.

Karena itu **area** diturunkan dari awalan nama dan **sifat** dari akhirannya:

| Aksi | Area | Sifat |
|---|---|---|
| `si_customer_list` | `mitra` | `baca` |
| `si_customer_create` | `mitra` | `tulis` |
| `si_collection_reverse` | `piutang` | `setuju` |
| `si_import_legacy` | `impor` | `tulis` |
| `si_receivable_aging_sales` | `piutang` | `baca` |

Awalan yang lebih spesifik didahulukan — `si_supplier_price_` sebelum `si_supplier_` — persis
seperti gerbang menu di `PosApi`.

Sifat bawaannya `baca`. Aksi baca memakai akhiran yang beragam (`_aging_customer`,
`_balance`, `_params`), sedangkan aksi tulis memakai kata kerja yang sedikit dan jelas
(`_create`, `_update`, `_save`, `_delete`, `_deactivate`) dan aksi berisiko lebih jelas lagi
(`_reverse`, `_approve`, `_post`, `_reprint`, `_export`, `_void`).

## Fail-closed, dan itu terbukti bukan sekadar niat

Aksi yang namanya di luar kesepakatan **tidak terpetakan**, dan §12.4 menolaknya:

```
area(si_fitur_baru_list) = null
OWNER boleh?              false
alasan                    "Aksi ini belum tersedia pada usaha ber-tenant."
```

**Bahkan OWNER ditolak.** Itu disengaja — lebih baik satu aksi baru berhenti terang-terangan
daripada diam-diam terbuka untuk semua peran.

Hal yang sama berlaku bagi anggota tenant **tanpa** `roleCode`, dan bagi peran yang tidak
dikenal. Invariannya: **setiap `TenantMembership` wajib punya `roleCode`.**

## Pengguna tanpa tenant tidak tersentuh

`TenantRbac.boleh(null, ...)` selalu `true`. Itu keadaan **seluruh pengguna hari ini**
(`tbmuser.pendaftar == null`), dan izin mereka sepenuhnya ditentukan lapisan lama. Menjadikan
lapisan ini wajib untuk semua orang akan memutus setiap pengguna yang ada.

## Pesan penolakan tidak menyebut peran mana yang boleh

`TenantRbac.alasan` mengembalikan *"Peran Anda tidak berwenang melakukan tindakan ini."* —
bukan *"hanya KEUANGAN yang boleh"*. Menyebutkannya memberi tahu penyerang bentuk kewenangan
di dalam tenant.

Kode galatnya `TENANT_ACCESS_DENIED`, dari sembilan kode baku §7.2.

## Penjaga: `TenantRbacSelfTest`

`java ais.service.tenant.test.TenantRbacSelfTest` — dijalankan dari `src/main`.

**Daftar aksinya dipindai dari sumber, bukan disalin ke dalam uji.** Daftar salinan akan basi
diam-diam begitu seseorang menambah aksi baru — dan itu persis kegagalan yang hendak dicegah.
Uji ini membaca literal `"si_..."` dari seluruh berkas dispatcher, lalu memastikan setiap aksi
utuh punya area. Awalan penyaring (berakhir garis bawah, mis. `"si_audit_"`) dilewati; itu
pola pencocokan menu, bukan nama aksi.

Uji juga menolak berjalan bila sumbernya tidak ditemukan — **tidak dilewati diam-diam**.

Selain itu ia mengunci: AUDITOR nol tulis dan nol setuju, `SALES_KELILING` tidak dapat
menyimpan harga, `GUDANG` tidak dapat membayar hutang, `PEMBELIAN` tidak dapat menagih
piutang, `ADMIN_TENANT` tidak dapat mengimpor, konteks `null` selalu lewat, dan peran kosong
maupun karangan ditolak.

## Yang BELUM dikerjakan

> **Sudah dikerjakan sejak dokumen ini ditulis:** delapan peran kini disemai ke
> `<slug>.role_tenant` oleh `TenantRoleSeeder`, dipanggil dari langkah provisioning
> `STEP_SEED_ROLES`. Lihat bagian di bawah.

- **Lingkup toko/gudang/sales belum ditegakkan** (§16 baris 8). `ActorContext` sudah membawa
  `tokoId` dan `salesId`, tetapi penyaringannya per kueri — bagian dari migrasi kueri P4.
- **Keadaan dokumen dan periode akuntansi** (§16 baris 9–10) menunggu tabel v7 dipakai.
- **Audit tindakan sensitif** (§16: perubahan rekening, harga di bawah biaya, posting,
  reversal, pembukaan periode, reprint, export) — `TenantAuditWriter` siap menerimanya,
  pemanggilnya lahir bersama migrasi kueri.

## Penyemaian peran: `TenantRoleSeeder`

`TenantRbac` menentukan **apa** yang boleh dilakukan tiap peran, tetapi matriksnya hidup di
dalam kode. Tabel `<slug>.role_tenant` adalah sisi yang terlihat: dari sanalah layar
pengelolaan pengguna mengambil pilihan peran, dan ke sanalah `user_role_tenant` menunjuk.

Tanpa penyemaian, tabel itu berdiri **kosong** pada tenant yang baru di-provision — sehingga
tidak ada satu peran pun yang dapat diberikan kepada pengguna, dan RBAC yang lengkap di kode
menjadi tidak terpakai.

### Idempoten, dan tidak menimpa

```sql
INSERT INTO {S}.role_tenant (kode, nama, keterangan, bawaan, aktif, dibuat_pada, oleh)
VALUES (:kode, :nama, :ket, true, true, now(), :oleh)
ON CONFLICT (kode) DO NOTHING
```

`DO NOTHING`, **bukan** `DO UPDATE`. Nama dan keterangan yang sudah disunting pemilik tenant
harus bertahan; menimpanya berarti membatalkan penyesuaian mereka setiap kali provisioning
diulang atau pemulihan dijalankan.

Diverifikasi pada PostgreSQL 16.4: jalan pertama menyisipkan 8 baris, jalan kedua tetap 8,
dan nama yang sudah diubah tidak tertimpa.

### Keterangan mengikuti matriks yang sebenarnya

Teks keterangan tiap peran diturunkan dari `TenantRbac` baris 105–166 — misalnya Sales
Keliling disebut hanya boleh **membaca** harga, sesuai `beri(SALES_KELILING, AREA_HARGA, BACA)`.
Bila matriksnya berubah, teksnya wajib ikut berubah: keterangan yang berbohong tentang
kewenangan lebih berbahaya daripada tidak ada keterangan sama sekali.

### Mode LEGACY tidak disemai

Pada mode LEGACY tidak ada schema per-tenant, sehingga tidak ada `role_tenant` untuk diisi;
peran owner tetap datang dari `tenant_membership.role_code`. `Tbmrole` global **tidak**
disentuh sama sekali — peran tenant tidak boleh mengubah super admin platform (§10.4).
