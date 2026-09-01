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

## Helper pertama selesai: Persediaan &amp; Kartu Stok

`SalesInventoryStokHelper` — aksi `si_inventory_balance` dan `si_inventory_ledger` — kini
melayani dua model berdampingan. Ia menjadi contoh bentuk untuk sepuluh helper sisanya.

### Jalur legacy tidak disentuh

Kode lama tetap berada di tempatnya, huruf demi huruf, di dalam cabang `else`. Aktor tanpa
tenant — yaitu seluruh pengguna hari ini — menjalankan SQL yang sama persis seperti sebelum
perubahan ini. Tidak ada satu pun `koperasi.` yang hilang dari jalur itu.

### Hanya SELECT yang berbeda

`SalesInventoryStokTenant` menghasilkan `SELECT` dengan **kolom yang sama dan berurutan
sama**: id, kode, barcode, nama, satuan, harga_beli, harga_jual, stok_minimum, awal, masuk,
keluar, opname. Karena itu pembungkus, penghitungan total, paginasi, dan seluruh perakitan
JSON dipakai bersama — tidak ada satu baris pun yang digandakan.

Sembilan `UNION ALL` pada kartu stok legacy runtuh menjadi **satu pemindaian** di sisi
tenant, sebab seluruh pergerakan sudah berada pada satu buku besar.

### Kontrak `mutasi_stok` akhirnya ditetapkan

Tabelnya berdiri sejak v3, tetapi nilai `jenis` dan `arah` **belum pernah didefinisikan di
mana pun** — tidak di DDL, tidak di dokumen. `TenantMutasiStok` menetapkannya sekarang,
sebelum penulis pertama diam-diam menentukannya untuk semua penulis sesudahnya.

`kuantitas` selalu positif; `arah` (+1/−1) yang menentukan naik-turun. Menyimpan tanda di
kuantitas akan membuat `SUM(kuantitas)` bermakna ganda — pada satu kueri total pergerakan,
pada kueri lain saldo bersih.

**Jebakan yang paling mudah menjatuhkan:** tabel legacy bernama `pembelian` menyimpan
**penjualan**. Memetakannya ke `PEMBELIAN` akan membalik arah seluruh omzet.

### Uji kesetaraan: `uji-kesetaraan-stok.sql`

Bukan uji yang mengulang rumusnya, melainkan yang **membandingkan dua model** atas peristiwa
bisnis yang sama. Dijalankan pada PostgreSQL 16.4:

| Produk | Awal | Masuk | Keluar | Opname | Hasil |
|---|---|---|---|---|---|
| P-001 (delapan jenis pergerakan) | 42 = 42 | 119,5 = 119,5 | 45 = 45 | 7 = 7 | **SETARA** |
| P-002 (tanpa pergerakan) | 0 = 0 | 0 = 0 | 0 = 0 | 0 = 0 | **SETARA** |

Termasuk kasus yang mudah salah: retur penjualan yang **tidak** kembali ke stok tidak
terhitung di kedua model, dan kuantitas pecahan (100,5) bertahan tanpa pembulatan.

### Satu perbedaan yang DISENGAJA, dan sebuah cacat legacy yang tersingkap

Kondisi rentang legacy adalah `BETWEEN dari AND (sampai + INTERVAL '1 day')` atas kolom
`timestamp`. Akibatnya peristiwa tepat pukul **00:00:00 pada H+1 ikut terhitung ke rentang
sebelumnya** — transaksi 1 Maret pukul 00:00 masuk ke laporan Februari.

Uji itu memperagakannya: legacy 1.100,5 versus tenant 100,5 untuk satu baris uji.

`mutasi_stok.tanggal` bertipe `date`, jadi jalur tenant **tidak dapat dan tidak boleh**
meniru cacat itu. Meniru cacat legacy demi angka yang sama berarti mengabadikannya di model
baru. Uji sengaja **melaporkan** selisih ini, bukan menggagalkannya.

### Filter toko: gagal-tertutup

Model tenant tidak punya `produk.toko`. Permintaan bersaring toko pada jalur tenant
**ditolak**, bukan dijalankan tanpa saringan — mengabaikan saringan lingkup berarti
menyajikan data di luar wewenang peminta. Lingkup `gudang`/`lokasi_stok` menyusul bersama
§16.

### Ketidakkonsistenan legacy yang ditemukan, dan sengaja tidak diperbaiki

Kartu saldo legacy menjumlahkan tujuh tabel dan **tidak** memuat `mutasi_stok_produksi`,
padahal kartu stok legacy menampilkannya. Untuk produk yang punya pergerakan produksi,
kedua layar itu **tidak akan pernah cocok**.

Tidak diperbaiki di sini: mengubah angka yang selama ini dilihat pengguna adalah keputusan
Anda, bukan efek samping pekerjaan tenant. Di sisi tenant persoalan ini tidak muncul, sebab
produksi adalah satu `jenis` pada buku besar yang sama.

## Helper kedua: Harga Supplier/Customer &amp; Analisa Harga

`SalesInventoryHargaHelper` — lima aksi — kini juga melayani dua model. Helper ini membawa
dimensi yang tidak ada pada helper pertama: **dua di antaranya menulis**.

### Pemblokir baru yang ditemukan: entitas Hibernate mematok schema

Jalur simpan legacy memakai entitas (`session.saveOrUpdate(new HargaBeliSupplier())`), bukan
SQL. Entitasnya menyatakan schemanya di anotasi:

```java
@Table(schema = "koperasi", name = "harga_beli_supplier")
```

Pemetaan itu **statis per SessionFactory**. Artinya `session.saveOrUpdate()` selalu menulis
ke `koperasi`, berapa pun tenant yang sedang aktif — yaitu persis kebocoran yang seluruh
pekerjaan ini hendak cegah.

**Ini bukan persoalan satu helper.** Di deployment ini **1.551 entitas** memakai
`@Table(schema=...)`, 98 di antaranya `koperasi`. Sembilan dari sebelas helper Sales/Inventory
menulis lewat entitas — 93 panggilan `save`/`update`/`delete` seluruhnya. Setiap jalur tulis
menghadapi tembok yang sama, dan sifatnya sekelas dengan pemblokir Envers.

Jalan keluar yang dipakai di sini: **jalur tenant menulis lewat SQL asli**, tanpa entitas.
Aturan bisnisnya tidak dilonggarkan — tanggal efektif ganda tetap ditolak, dan versi
tersimpan tetap tidak boleh diubah harga/tanggal/pihak/produknya.

### Supplier legacy tidak berada di schema `koperasi`

Ia di `library.penyedia`. Jalur tenant menariknya ke `<schema>.supplier`, sehingga daftar
pemasok satu tenant tidak lagi bercampur dengan tenant lain. Ini salah satu contoh konkret
manfaat pemisahan, bukan sekadar biaya.

### Gap katalog yang tersingkap: harga umum tidak dapat disimpan

Legacy menyatakan harga umum sebagai baris `harga_jual_customer` dengan
`anggota_koperasi IS NULL`. Model tenant **tidak dapat menyatakannya**:

```
customer_id bigint NOT NULL REFERENCES {S}.customer(id)
```

Konsekuensinya di jalur tenant, dan semuanya **gagal-tertutup, bukan diam-diam**:

| Jalur | Perlakuan |
|---|---|
| Saringan "hanya umum" pada daftar | **Ditolak** dengan sebab, bukan daftar kosong yang terbaca "belum ada datanya" |
| Simpan harga tanpa anggota | **Ditolak** lebih awal dengan sebab, bukan dibiarkan gagal di lapisan basis data |
| Kolom harga umum pada Analisa | Memakai `produk.harga_jual_standar` — satu-satunya representasi yang tersedia |

**Kalau yang dimaksud desain sebenarnya adalah `customer_id` boleh kosong, ini gap katalog
dan bukan pilihan pemetaan.** Perbaikannya sebuah migrasi **v10** yang melonggarkan kolom
itu — bukan menyunting DDL yang sudah dirilis, sebab katalognya append-only ber-checksum.
Keputusan itu bukan efek samping pemindahan kueri, jadi ditandai alih-alih diputuskan
diam-diam.

### Uji kesetaraan: `uji-kesetaraan-harga.sql`

| Blok | Hasil |
|---|---|
| Daftar harga beli supplier | 2 baris **SETARA** |
| Daftar harga jual customer | **SETARA**; baris harga umum dilaporkan tanpa padanan |
| Analisa harga — stok &amp; harga beli terbaru | **SETARA** untuk kedua produk |

Stok pada jalur tenant memakai rumus yang **sama persis** dengan helper pertama
(`SUM(arah * kuantitas)`), supaya layar Analisa Harga dan layar Persediaan tidak pernah
menyebut angka stok berbeda untuk produk yang sama.

### Dua hal yang dilaporkan uji, bukan diklaim setara

**Harga umum pada produk tanpa baris harga umum eksplisit.** Legacy mengosongkannya; jalur
tenant memakai harga jual standar yang selalu terisi. Uji melaporkannya apa adanya —
melabelinya "setara" akan menyembunyikan konsekuensi gap di atas.

**Harga kedaluwarsa.** Legacy hanya punya satu tanggal efektif, sehingga harga lama terpilih
selamanya; model tenant punya `berlaku_sampai`, sehingga mundur ke versi yang masih berlaku.
Diperagakan pada uji: legacy 2.500, tenant 2.400. Perbedaan ini dikehendaki.

## Helper ketiga: Hutang Supplier

`SalesInventoryPayableHelper` — tujuh aksi. Perbedaan bentuknya paling dalam sejauh ini.

### Legacy menyimpan FAKTUR, tenant menyimpan HUTANG

Legacy menaruh seluruh faktur kulakan di `pengadaan_faktur`, menempelkan
`payable_faktur_info` 1:1 untuk medan khusus hutang, lalu menyaring mana yang berhutang
lewat `i.jenis_pembayaran IN ('DP','CREDIT')`.

Model tenant memisahkannya: `pembelian` adalah dokumennya, `hutang_supplier` **hanya berisi
yang benar-benar berhutang**. Tabel sisi itu lenyap — medannya menyatu.

Akibatnya saringan jenis pembayaran **tidak perlu** di jalur tenant: keberadaan barisnya
sudah berarti hutang.

### Jebakan yang nyaris menjatuhkan laporan

Laporan pembelian mencakup **pembelian tunai juga**. Menyusunnya dari `hutang_supplier`
akan menghilangkan seluruh pembelian tunai — tanpa satu pun galat muncul. Angka salah yang
terlihat benar.

Rancangan pertama di sini memang keliru begitu, dan tertangkap saat memeriksa kolom yang
sebenarnya dipakai. Laporan kini bertumpu pada `pembelian` dengan `LEFT JOIN hutang_supplier`:
ada berarti kredit, tidak ada berarti tunai.

### Sisa hutang dihitung, bukan dibaca

`hutang_supplier` menyediakan `terbayar` dan `sisa`. Jalur ini **sengaja tidak memakainya**
dan menghitung ulang dari `alokasi_pembayaran_hutang`, persis seperti legacy.

Alasannya sama dengan `saldo_stok`: kolom ringkasan bisa basi, dan angka hutang yang basi
berarti membayar dua kali atau menagih yang sudah lunas.

### Cacat yang ditangkap uji, bukan oleh compiler

`pembayaran_hutang.nomor_dokumen` **NOT NULL**, sedangkan `INSERT` jalur tenant yang pertama
ditulis tidak mengisinya. Kodenya lulus kompilasi dan akan gagal saat pembayaran pertama
disimpan.

Sampai ada skema penomoran per tenant, kunci idempotensi dipakai sekaligus sebagai nomor
dokumen — sudah dijamin unik dan dapat ditelusuri balik. Bukan pengganti penomoran yang
sebenarnya, dan ditandai begitu di kodenya.

### Uji kesetaraan: `uji-kesetaraan-hutang.sql`

| Blok | Hasil |
|---|---|
| Sisa hutang per faktur | **SETARA** (500.000 = 500.000 pada keduanya) |
| Umur hutang, acuan 2026-02-15 | **SETARA** (B1_30 dan B90) |
| Laporan pembelian termasuk tunai | **SETARA**, ketiga baris |
| Jumlah baris laporan | **3 = 3** — tunai tidak hilang |

### Dua keputusan yang ditandai, bukan diambil diam-diam

**Kait ke Daftar Pengajuan Transfer dilewati.** Jalur legacy menautkan tiap pembayaran ke
`akunting.DaftarPengajuanTransfer` supaya muncul di layar Pembayaran Transfer. Modul itu
**bersama**, bukan per-tenant — menautkan pembayaran satu tenant ke sana membocorkan datanya
ke seluruh instalasi. Kaitnya dilewati, dan itu **mengubah perilaku**: pembayaran hutang
tenant tidak muncul di layar transfer bersama. Padanan per-tenantnya belum ada.

**`purchaseTermsSave` ditolak pada tenant.** Jenis pembayaran dan termin per faktur tidak
punya tempat di model tenant. Menyimpan jatuh temponya saja lalu melaporkan sukses akan
membuat pengguna mengira jenis dan termin ikut tersimpan.

### Yang tidak punya padanan

`status_bg` — legacy melacak status giro terpisah dari status dokumen; model tenant hanya
punya `status`. Kolomnya dikembalikan kosong, bukan diisi status dokumen: menyamakan keduanya
akan membuat giro yang belum cair tampak sudah beres.

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
