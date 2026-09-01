# 02 — API tenant dan aktor (FASE P2)

Empat aksi aditif dan empat medan baru pada `ActorContext`. Tidak ada aksi lama yang
berubah perilakunya.

| Aksi | Hasil |
|---|---|
| `tenant_list` | daftar tenant milik aktor: `tenantId`, `tenantCode`, `nama`, `role`, `status`, `modules` |
| `tenant_context` | konteks lengkap satu tenant, bagian publiknya saja |
| `tenant_validate` | memvalidasi aktor berhak atas tenant tersebut |
| `tenant_select` | alias validasi — **tidak menyimpan apa pun di server** |

## Keputusan yang membentuknya

### Di luar gerbang `si_`, dan itu disengaja

`TenantApiDispatcher` dipanggil di `ApiEBisnis.prosesAksiTambahan` **sebelum**
`SalesInventoryApiDispatcher`, dan nama aksinya tidak berawalan `si_`.

Alasannya melingkar kalau dilanggar: gerbang `si_` menolak aktor yang belum punya profil
Inventory & Sales. Profil itu melekat pada tenant. Kalau `tenant_list` ikut tergerbang,
pengguna baru tidak akan pernah dapat melihat daftar tenantnya — sehingga tidak pernah
dapat memilih tenant yang justru akan memberinya profil tersebut. Dokumen master §10
menyebutkannya eksplisit: *"Aksi `tenant_list` harus dapat dipanggil sebelum actor
Inventory/Sales lengkap."*

### `tenant_select` tidak menyimpan keadaan

Ia hanya alias `tenant_validate`. Klien yang menyimpan pilihannya lalu mengirimkannya
kembali pada tiap request lewat header `X-Tenant-Id`.

Menyimpannya di server melanggar §7.1 dan §24 butir 7, tetapi alasan praktisnya lebih
tajam: dua tab peramban atau dua perangkat milik satu pengguna akan saling mengubah tenant
aktif satu sama lain. Pengguna mengira sedang bekerja pada perusahaan A, padahal tab
sebelah baru memindahkannya ke B.

### Tiga kueri, berapa pun jumlah tenantnya

`tenant_list` mengambil tenant, peran, dan modul masing-masing sekali lalu menggabungkannya
di memori. Membentuk `TenantContext` per tenant akan berarti dua sampai tiga kueri per
baris — dan `TenantContextResolver.resolve` **menolak** tenant yang SUSPENDED atau belum
READY, padahal justru itu yang perlu dilihat pengguna.

### Tenant bermasalah tetap ditampilkan

Tenant SUSPENDED dan yang belum READY ikut masuk daftar, lengkap dengan `status`-nya.
Menyembunyikannya menghasilkan daftar kosong yang tampak seperti kehilangan data;
menampilkannya memberi tahu pengguna bahwa perusahaannya ada tetapi sedang tidak dapat
dipakai — dan `status` itulah yang memberi tahu ia harus menunggu atau menghubungi admin.

### `ActorContext` menyalin, bukan merujuk

Empat medan publik ditambahkan: `tenantId`, `tenantCode`, `tenantName`, `membershipRole`.
Metode `isiTenant(TenantContext)` **menyalin** keempatnya alih-alih menyimpan rujukan ke
`TenantContext` — rujukan membuat `schemaName` ikut terbawa ke mana pun aktor dioper, dan
cepat atau lambat ada yang men-serialisasinya.

`toJson()` karena itu tetap bebas nama schema, sesuai §4.7 dan §7.2.

### Nama `tenant_list` dipakai dua endpoint

Sudah ada `tenant_list` lain di `EbisnisPublicServlet` (`TenantOnboardingService.tenantList`).
**Tidak bertabrakan**: endpointnya berbeda (`/EbisnisPublic` vs `/Api_eBisnis`), kelas
induknya berbeda (`HttpServlet` vs `PosApi`), dan yang lama tidak pernah memanggil
`prosesAksiTambahan`.

Tetapi semantiknya berbeda dan itu perlu diketahui penulis klien: yang lama mengambil
pendaftar dari **sesi** dan melayani dasbor pendaftaran; yang baru mengambil aktor dari
**token** dan melayani POS. Jangan menyamakan responsnya.

Perhatikan pula: aksi lain berawalan `tenant_` memang ada (`tenant_setoran`,
`tenant_bagi_hasil`, `tenant_tunggakan`). Dispatcher ini memeriksa nama **persis** terhadap
empat aksi yang dikenalnya lalu mengembalikan `false` untuk sisanya — bukan menyaring dengan
awalan saja. Menyaring dengan awalan akan menelan ketiga aksi itu diam-diam.

## Kontrak `tenantId` (§7.1)

Header `X-Tenant-Id`, dengan `tenantId` pada body sebagai cadangan kompatibilitas.
`TenantContextResolver.selaraskanTenantId` menerapkan butir 1–2: sama → lanjut; berbeda →
`TENANT_CONTEXT_MISMATCH`.

Bila keduanya kosong, `resolveOtomatis` menerapkan butir 3–4: keanggotaan **tepat satu**
boleh dipilih otomatis; **lebih dari satu wajib dipilih pengguna**
(`TENANT_SELECTION_REQUIRED`). Bedanya dengan "ambil yang pertama" bukan soal gaya: aktor
dengan dua perusahaan yang diam-diam dipilihkan salah satunya akan bekerja pada perusahaan
yang keliru tanpa pernah tahu.

`tenantId` yang bukan angka diperlakukan sama dengan tidak dikirim — validasi kepemilikannya
tetap dikerjakan resolver, jadi tidak ada jalan pintas.

## Bentuk galat

```json
{ "status": "error", "kode": "TENANT_SELECTION_REQUIRED", "message": "Pilih tenant terlebih dahulu." }
```

`kode` dibaca mesin dan berasal dari sembilan kode baku §7.2; `message` dibaca manusia.

Kuncinya **`kode`, bukan `code`** — `ApiClient` Flutter membaca `json['kode']` dan hanya itu.
Mengirim `code` berarti kode galat tidak pernah sampai ke klien.
Keduanya bebas nama schema, SQL, jejak tumpukan, dan kredensial.

## Yang BELUM dikerjakan

- ~~**Aksi `si_*` belum menuntut TenantContext.**~~ **Sudah tidak berlaku.**
  `SalesInventoryApiDispatcher` kini me-resolve tenant (baris 48), memasangnya ke aktor
  lewat `isiTenant` (baris 50), dan menjalankan gerbang RBAC `TenantRbac.boleh` (baris 66)
  sebelum aksi dijalankan. Aktor tanpa tenant lolos tanpa perubahan, sesuai §12.5.
- ~~**`isiTenant` belum dipanggil dari jalur `si_*`**~~ — dipanggil, lihat di atas.
- **Yang benar-benar tersisa: kuerinya, bukan gerbangnya.** Gerbang tenant sudah berdiri,
  tetapi SQL di dalam helper masih menunjuk schema `koperasi`. Itulah P4 — dan bentuknya
  bukan penggantian prefiks; lihat [04-refactor-si.md](04-refactor-si.md).
- Pemblokir **Envers** masih berdiri (menghadang P3, bukan P4) — lihat
  [01-tenant-context.md](01-tenant-context.md). Pemblokir `mutasi_idempoten`
  **sudah ditutup**, lihat [04-refactor-si.md](04-refactor-si.md).
