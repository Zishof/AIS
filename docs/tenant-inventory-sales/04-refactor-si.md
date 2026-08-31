# 04 — Fondasi tenant untuk `si_*` (FASE P4, sebagian)

> **Baca ini lebih dulu.** P4 menuntut refactor **7.512 baris** di sebelas helper agar seluruh
> 112 aksi `si_*` memakai repository ber-schema tenant. Yang selesai pada tahap ini adalah
> **fondasinya**, bukan migrasi kuerinya. Rinciannya di bagian terakhir.

## Yang selesai

| | |
|---|---|
| Pemblokir P0 ditutup | `mutasi_idempoten` kini berskema eksplisit |
| §12.3 dilengkapi | `TenantSqlExecutor` mendapat pagination dan pembatas limit |
| Opsi (a) terwujud | `TenantAuditWriter` — penulis audit per-tenant |
| Gerbang tenant | `SalesInventoryApiDispatcher` membentuk `TenantContext` per request |
| Bug ditemukan & ditutup | validasi nama schema audit |
| Penjaga | `TenantKonteksSelfTest` |

## Bug yang ditemukan pengujian: nama schema audit yang panjang

`TenantSchemaService.pastikanAman` memakai pola `^[a-z][a-z0-9_]{2,30}$` — maksimum
**31 karakter**. Nama schema audit adalah nama data ditambah `__audit`, **tujuh karakter
lebih panjang**.

Akibatnya: slug sepanjang **25 karakter ke atas** lolos provisioning, `buatSchema`
benar-benar membuat `<slug>__audit`, tetapi nama turunannya **ditolak** bila divalidasi ulang
dengan pola yang sama. Setiap kueri audit tenant itu gagal padahal schema-nya ada.

Contoh pada dokumen master §4.3, `caruban_medika_nusantara` (24 karakter), berada **satu
karakter** di bawah tebing. `apotek_sumber_sehat_sentosa` (27) sudah patah.

Saya sendiri yang memasang cacat itu di P1 (`TenantSqlExecutor.kutip` memvalidasi nama audit
dengan pola data), dan hampir mengulanginya di `TenantAuditWriter`. Ketahuan karena diuji,
bukan karena dibaca.

**Perbaikannya**: `TenantSchemaLocator.pastikanAmanAudit` memvalidasi **basisnya** lalu
memastikan akhirannya, bukan menjalankan pola data atas nama turunan. `TenantSqlExecutor`
kini memilih validator sesuai penanda — `{t}` memakai validator data, `{a}` memakai validator
audit.

## Penulis audit per-tenant — opsi (a)

`TenantAuditWriter` menulis ke `<schema-tenant>__audit`, bukan ke `new_audit` global.

**Satu revisi, banyak baris.** Satu aksi menghasilkan satu baris `revinfo` yang membawa
konteksnya (siapa, peran apa, dari perangkat mana, permintaan yang mana, alasannya apa), lalu
beberapa baris `audit_baris` — satu per baris data yang tersentuh. Menyimpan faktur berisi
lima puluh item tetap satu revisi, bukan lima puluh salinan konteks yang sama.

**Selalu pada transaksi pemanggil.** Tidak pernah membuka Session sendiri. Audit yang commit
terpisah dapat bertahan padahal perubahannya dibatalkan, atau hilang padahal perubahannya jadi.

Muatan `sebelum`/`sesudah` disusun pemanggil — kelas ini tidak dapat menebak medan mana yang
rahasia, dan §11.6 melarang audit memuat kata sandi atau token.

## Gerbang tenant: fail-closed, tetapi tidak memutus pengguna hari ini

`SalesInventoryApiDispatcher` kini membentuk `TenantContext` sebelum aksi dijalankan, lalu
menempelkannya ke `ActorContext`.

Kuncinya ada pada perbedaan dua keadaan:

- **Aktor tanpa keanggotaan tenant sama sekali** — keadaan seluruh pengguna existing — tetap
  lewat jalur lama **tanpa perubahan apa pun**. Gerbang mengembalikan `null`, bukan galat.
- **Aktor yang sudah ber-tenant** ditegakkan penuh: tenant yang bukan miliknya, tenant
  suspended, schema belum siap, modul tidak aktif, atau punya banyak tenant tanpa memilih —
  semuanya ditolak dengan kode baku §7.2.

Menjadikan tenant wajib sekarang akan memutus setiap pengguna yang ada hari ini. Itulah
rollout HYBRID yang §12.5 maksud.

Schema diverifikasi benar-benar ada sebelum kueri pertama menyentuhnya — kalau tidak, galat
SQL mentahlah yang muncul, lengkap dengan nama schema di dalamnya.

### Aturan routing: `tbmuser.pendaftar == null` → schema existing

Penentu jalurnya adalah **satu medan yang sudah lama ada**: `Tbmuser.pendaftar`. Javadoc-nya
sendiri menjelaskan semantiknya — pengguna yang terikat pendaftar hanya melihat toko milik
pendaftar itu, sedangkan yang `null` adalah **admin pusat** yang melihat seluruh toko.

Justru karena itulah `null` **tidak boleh** masuk ke schema tenant. Membiarkan admin pusat
membaca schema tenant sama dengan menjadikan `null` sebagai kunci lintas tenant — persis yang
§24 butir 5 larang. Mengembalikannya ke jalur lama menutup celah itu **sekaligus** menjaga
perilaku existing tidak berubah sedikit pun.

Jadi:

| `tbmuser.pendaftar` | Jalur |
|---|---|
| `null` (admin pusat, akun legacy) | **schema existing**, tanpa perubahan apa pun |
| terisi | konteks tenant dibentuk, ditegakkan penuh |

**Satu kueri.** Penerapan pertama saya memakai dua `COUNT` untuk menebak dari data; ini
membacanya langsung dari medan yang memang dirancang untuk itu. Gerbang ini berjalan pada
setiap request `si_*`, jadi selisih satu kueri ditanggung setiap transaksi kasir.

> Ditanyakan lewat kueri, **bukan** `tbmuser.getPendaftar()`. Relasinya `LAZY` dan objek
> `Tbmuser` datang dari Session autentikasi yang sudah ditutup — penelusuran malas di sana
> melempar `LazyInitializationException`, dan hanya kadang-kadang, tergantung apakah
> proxy-nya sempat terinisialisasi. Kegagalan yang muncul kadang-kadang jauh lebih mahal
> daripada satu kueri.

**Batas yang perlu dijaga:** pengguna dengan `pendaftar == null` tetapi punya
`TenantMembership` akan dirutekan ke jalur lama. Itu sisi aman — ia melihat data bersama yang
memang sudah boleh dilihatnya, bukan data tenant lain. Tetapi artinya ada invarian yang harus
dipelihara: **setiap Tbmuser yang diberi keanggotaan tenant wajib juga diisi
`pendaftar`-nya.**

### Konsekuensi rollout: pengguna dengan dua tenant butuh klien baru dulu

§7.1 butir 4 menuntut aktor yang punya lebih dari satu tenant **memilih** salah satunya.
Klien Flutter dan JSP hari ini belum mengirim `X-Tenant-Id` maupun `tenantId` — header itu
baru lahir di P2.

Akibatnya, begitu ini di-deploy:

- aktor **tanpa** tenant → tidak berubah sama sekali;
- aktor dengan **satu** tenant → dipilih otomatis, tidak berubah bagi penggunanya;
- aktor dengan **dua tenant atau lebih** → seluruh aksi `si_*` ditolak dengan
  `TENANT_SELECTION_REQUIRED` sampai kliennya diperbarui.

Kelompok ketiga saat ini kosong, karena belum ada tenant Inventory & Sales yang
diprovisioning. Tetapi urutan deploy-nya penting: **klien yang mengirim tenant harus rilis
sebelum tenant kedua dibuat untuk seorang pengguna**, bukan sesudahnya.

### `ActorContext.tenant` bersifat `transient`, dan dijaga uji

P2 sengaja hanya **menyalin** empat medan publik ke `ActorContext` agar nama schema tidak
ikut terbawa. P4 membutuhkan nama schema di sisi server, jadi konteks penuhnya kini
ditempelkan — sebagai medan `transient` yang **tidak pernah ikut `toJson()`**.

Janji itu tidak dibiarkan sebagai niat: `TenantKonteksSelfTest` memeriksa **teks JSON**-nya,
bukan daftar medannya, sehingga medan baru yang kelak ditambahkan pun tetap tertangkap.

## `mutasi_idempoten` — pemblokir P0 ditutup

Satu-satunya tabel di jalur `si_*` yang bergantung `search_path`. Kini
`@Table(schema = "public", ...)` — **tidak dipindah**, hanya dieksplisitkan: ini tabel legacy
lintas-tenant. Idempotensi jalur tenant memakai `<schema-tenant>.idempotency_record` dari v8.

## Temuan yang mengubah rencana P4: schema tenant BUKAN cermin legacy

Diuji langsung pada PostgreSQL 16.4 dengan schema tenant hasil katalog v9, dan hasilnya
membatalkan asumsi yang paling menggoda tentang P4.

### Asumsi yang gugur

Helper Sales/Inventory menulis nama schema **harfiah** di dalam SQL:

```java
"COALESCE((SELECT SUM(" + kolomQty + ") FROM koperasi." + tabel + " x WHERE ..."
```

Ada **111 rujukan `koperasi.` semacam itu di tujuh helper**, menyentuh 37 tabel. Dari
bentuknya, P4 tampak seperti pekerjaan mekanis: satu titik penentuan schema, lalu ganti
seluruh literal. Rintisan seperti itu sudah dicoba pada `SalesInventoryStokHelper` dan
**berhasil dikompilasi tanpa satu pun galat**.

Ia tetap salah.

### Yang sebenarnya terjadi

Dari sepuluh tabel yang dipakai helper itu, **lima tidak ada** di schema tenant:

| Legacy | Schema tenant |
|---|---|
| `satuan_produk` | `satuan` |
| `mutasi_stok_toko` | — (lebur ke `mutasi_stok`) |
| `mutasi_stok_produksi` | — (lebur ke `mutasi_stok`) |
| `pengadaan_produk` | — (lebur ke `mutasi_stok`) |
| `retur_pembelian` | — (lebur ke `mutasi_stok`) |

Kolomnya pun berbeda. Dari lima kolom `produk` yang dipakai helper, **empat berganti**:

| Legacy | Schema tenant |
|---|---|
| `p.satuan` | `p.satuan_id` |
| `p.hargabeli` | `p.harga_beli_terakhir` |
| `p.hargajual` | `p.harga_jual_standar` |
| `p.toko` | **hilang** — digantikan model `gudang`/`lokasi_stok` |
| `p.stok_minimum` | sama |

### Sebabnya disengaja, bukan kelalaian

Schema tenant memakai **satu buku besar append-only** `mutasi_stok` — berkolom `jenis`,
`arah`, `dokumen_tipe`, `dokumen_id`, `pembalik_dari_id` — dengan `saldo_stok` sebagai
turunan. Legacy menghitung stok dengan menjumlahkan **delapan tabel terpisah**.

Itu keputusan desain yang sudah tercatat di [03](03-migrasi-schema.md) ("Mutasi append-only,
saldo turunan"), dan memang lebih baik. Konsekuensinya yang belum tercatat: **kueri legacy
tidak dapat dipindahkan, hanya dapat ditulis ulang.**

### Karena itu rintisannya dibatalkan

Perubahan pada `SalesInventoryStokHelper` di-`revert`, dan kelas pemilih schema yang menyertainya
dibuang. Menyimpannya berarti menaruh kode yang merutekan kueri ke tabel dan kolom yang tidak
ada — gagal saat dijalankan, bukan saat dikompilasi. Kode yang lulus kompilasi tetapi pasti
gagal di runtime lebih berbahaya daripada tidak ada kode sama sekali, sebab ia tampak selesai.

### Yang harus dikerjakan P4, kalau begitu

1. **Pemetaan kueri per aksi**, bukan per tabel. Untuk tiap aksi `si_*`, tentukan bentuk
   kueri setara pada model `mutasi_stok`/`saldo_stok`.
2. **Dua jalur berdampingan**, dipilih dari `ActorContext.tenant`: jalur legacy tetap persis
   seperti sekarang, jalur tenant memakai kueri baru. Bukan satu kueri ber-prefiks.
3. **Uji kesetaraan**: data yang sama dimasukkan lewat kedua model harus menghasilkan
   Awal/Masuk/Keluar/Akhir yang sama. Tanpa uji ini, perbedaan pembulatan atau tanda pada
   `arah` tidak akan ketahuan sampai ada selisih stok di toko sungguhan.

Perkiraan sebelumnya — "111 literal, pekerjaan mekanis" — **terlalu rendah**. Yang benar:
sebelas helper, masing-masing perlu kueri baru dan uji kesetaraan sendiri.

## Yang BELUM dikerjakan — dan ini bagian terbesar P4

**Sebelas helper, 7.512 baris, belum satu pun kuerinya dipindah ke schema tenant.**

| Helper | Baris |
|---|---|
| `SalesInventoryTripHelper` | 1.500 |
| `SalesInventoryReceivableHelper` | 1.232 |
| `SalesInventoryMasterHelper` | 952 |
| `SalesInventoryPayableHelper` | 700 |
| `SalesInventoryFinanceHelper` | 608 |
| `SalesInventoryReversalHelper` | 524 |
| `SalesInventoryDbfImportHelper` | 488 |
| `SalesInventoryHargaHelper` | 454 |
| `SalesInventoryStokHelper` | 293 |
| `SalesInventoryHelper` | 278 |
| `SalesInventoryApiDispatcher` | 235 |

Semuanya masih membaca `library.penyedia`, `koperasi.supplier_inventory_profile`,
`koperasi.customer_inventory_profile`, dan kerabatnya — persis yang §12.1 larang menjadi
kanonik bagi data tenant.

**Mengapa tidak sekalian dikerjakan.** Tabel tenant hasil P3 **belum pernah dibuat pada
PostgreSQL mana pun**. Memindahkan 7.512 baris SQL ke tabel yang belum pernah ada, tanpa
satu pun cara menjalankannya, akan menghasilkan perubahan besar yang tidak dapat diverifikasi
siapa pun — termasuk saya. Urutan yang benar: jalankan migrasi pada basis data uji, baru
pindahkan kueri per helper dengan hasilnya dapat dibandingkan.

**Juga belum:**

- **Satu Session per request (§9.4).** Setiap helper masih membuka Session sendiri, termasuk
  gerbang tenant ini. Penyatuannya menuntut perubahan tanda tangan seluruh helper —
  pekerjaan yang wajar dilakukan bersamaan dengan migrasi kuerinya, bukan terpisah.
- **`TenantAuditWriter` belum dipanggil dari mana pun.** Ia siap; pemanggilnya lahir bersama
  helper yang dipindahkan.
- **Uji lintas-tenant dan uji pool A → B → A (§25.5)** memerlukan basis data.
