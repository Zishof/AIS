# 01 — Konteks tenant di server (FASE P1)

Tujuh kelas di `ais/service/tenant/`. Semuanya baru; lapisan entitas
(`TenantRegistry`, `TenantMembership`, `TenantModuleEntitlement`) sudah ada sejak
program pendaftaran tenant dan **tidak diubah**.

| Kelas | Tugasnya |
|---|---|
| `TenantContext` | objek nilai immutable, 16 medan, dibentuk sekali per request |
| `TenantContextResolver` | menyatukan penentuan tenant + aktor + schema |
| `TenantMembershipResolver` | menentukan kewenangan aktor atas tenant |
| `TenantSchemaLocator` | satu-satunya penerjemah tenant → nama schema |
| `TenantSqlExecutor` | menjalankan SQL berkualifikasi schema |
| `TenantRequestExecutor` | satu Session + satu transaksi per request |
| `TenantAccessException` | penolakan, dengan kode terbaca mesin |

## Keputusan yang menentukan bentuknya

### Kualifikasi eksplisit, bukan `SET search_path`

Templat memakai penanda `{t}` (schema data) dan `{a}` (schema audit):

```sql
SELECT id, waktu FROM {t}.pembelian WHERE toko = :toko
```

`SET search_path` terlihat lebih ringkas, tetapi c3p0 mengembalikan koneksi ke
kolam **beserta `search_path`-nya**. Satu jalur yang lupa mengembalikan nilainya
membuat request tenant berikutnya membaca schema tenant sebelumnya — tanpa galat,
tanpa gejala, dan tidak terlihat pada uji satu-tenant. Kualifikasi eksplisit tidak
punya keadaan yang bisa tertinggal.

Nama schema tidak pernah berasal dari input pengguna: diambil dari `TenantContext`,
divalidasi `TenantSchemaService.pastikanAman` (pola `^[a-z][a-z0-9_]{2,30}$`),
**lalu divalidasi ulang di `TenantSqlExecutor`** sebelum dikutip ganda. Validasi
ganda itu disengaja — menutup kemungkinan konteks dibentuk lewat jalur lain kelak.
Nilai selain nama schema tetap wajib lewat parameter terikat.

### Keanggotaan, bukan kepemilikan

Kewenangan ditentukan baris `TenantMembership` yang **aktif dan masih berlaku**
(`status`, `validFrom`, `validUntil`) — bukan `TenantRegistry.ownerPendaftar` saja,
dan bukan role global aktor. Role global menjawab "boleh apa di aplikasi ini",
bukan "berhak atas tenant yang mana".

**Kompatibilitas owner lama tanpa admin implicit.** Tenant yang terbit sebelum
tabel keanggotaan terisi hanya punya `ownerPendaftar`. Pemiliknya tetap dilayani,
tetapi hasilnya ditandai `isTurunan()` dengan `membershipId` kosong dan
`sumber = OWNER_REGISTRY` — **tidak ada baris keanggotaan yang dikarang diam-diam**.
Hanya pemilik terdaftar yang mendapat kelonggaran ini.

**Kedaluwarsa dibedakan dari bukan-anggota.** Dua kode berbeda
(`KEANGGOTAAN_KEDALUWARSA` dan `BUKAN_ANGGOTA`) supaya pesannya dapat menuntun
pengguna, bukan sekadar menolak.

### Tenant wajib dinyatakan eksplisit

Tidak ada penafsiran "aktor tanpa pedagang berarti boleh seluruh tenant".
Platform admin pun harus menyebut tenant; `tenantId` kosong ditolak dengan
`TENANT_BELUM_DIPILIH`. Penafsiran diam-diam semacam itu adalah cara paling mudah
membocorkan data antar tenant.

### Konteks dibentuk di dalam transaksi pekerjaannya

`TenantRequestExecutor` membentuk `TenantContext` memakai Session dan transaksi
**yang sama** dengan tugasnya. Bila konteks dibentuk pada Session tersendiri,
keanggotaan dapat dicabut tepat di antara pemeriksaan dan pemakaian — pekerjaan
berjalan atas kewenangan yang sudah tidak ada.

Session ditutup di `finally` lewat `HibernateUtil.closeSessionQuietly`. Kegagalan
rollback sengaja ditelan: ia hanya terjadi ketika koneksinya memang sudah putus,
dan melemparnya dari `finally` akan menutupi galat asli yang sedang naik.

### Nama schema tidak sampai ke klien

`toJsonKlien()` sengaja menghilangkan `schemaName`, `auditSchemaName`, dan
`schemaVersion`. Pesan pada `TenantAccessException` juga tidak pernah memuat nama
schema — termasuk pada cabang galat konfigurasi, yang paling mudah terlewat.

## Catatan penerapan

**`org.json` di sini melempar `JSONException` terperiksa.** `toJsonKlien()`
karena itu mendeklarasikan `throws JSONException`, mengikuti konvensi repo
(173 deklarasi `throws` berbanding 29 `catch`). Konstruktor `JSONArray(Collection)`
tidak dipakai — diganti perulangan eksplisit supaya tidak bergantung versi pustaka.

**Gaya Java 1.7.** Tanpa lambda, diamond, try-with-resources, maupun Stream API.
Pekerjaan diserahkan ke `TenantRequestExecutor` lewat antarmuka `Tugas`.
`TenantContext` memakai Builder karena enam belas medan terlalu banyak untuk satu
konstruktor.

## Yang BELUM dikerjakan P1

- ~~**Belum ada pemanggil.**~~ **Sudah tidak berlaku.** Lima dari tujuh kelas kini
  dipakai di luar paketnya: `TenantAccessException`, `TenantContext`,
  `TenantContextResolver`, `TenantMembershipResolver`, dan `TenantSchemaLocator` —
  terutama oleh `SalesInventoryApiDispatcher` dan `TenantApiDispatcher`. Yang masih
  menunggu P4 hanya **`TenantSqlExecutor`** dan **`TenantRequestExecutor`**, sebab
  keduanya baru terpakai ketika kueri `si_*` benar-benar berpindah ke schema tenant.
- **Envers masih pemblokir.** `org.hibernate.envers.default_schema=new_audit`
  bersifat statis per SessionFactory, sehingga baris audit 1.561 entitas `@Audited`
  dari semua tenant berkumpul di satu schema. `TenantContext.auditSchemaName`
  sudah menyediakan tempatnya, tetapi Envers tidak dapat mengisinya.
  `TenantDataPlaneService` menyiasatinya dengan menulis baris audit gaya Envers
  **dengan tangan** ke `<slug>__audit` — pola itu bekerja, tetapi hanya untuk
  tabel yang ditulis tangan, bukan untuk 1.561 entitas.
- **`mutasi_idempoten` masih tanpa skema** (`@Table` tanpa `schema`) — satu-satunya
  tabel yang bergantung `search_path`. Wajib dikualifikasi sebelum P4.
