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

## Helper keempat: Kas/Jurnal &amp; Laba-Rugi

`SalesInventoryFinanceHelper` — delapan aksi. Helper pertama yang menyentuh **schema ketiga**.

### `akunting` — schema yang luput dari survei awal

Tiga helper sebelumnya hanya merujuk `koperasi` dan `library`. Helper ini juga memakai
`akunting` (bagan akun dan jurnal). Survei awal saya hanya mencari dua schema pertama, jadi
ini sempat tidak terlihat.

**Diperiksa ulang untuk seluruh helper**, dan hasilnya melegakan: Stok, Harga, dan Payable
yang sudah dipindahkan **hanya** merujuk `koperasi` dan `library` — tidak ada rujukan
`akunting` yang terlewat di sana. Yang memakainya hanya Finance dan Master.

### Pemetaan yang paling banyak berubah bentuk

| Legacy | Tenant |
|---|---|
| `akunting.akun` | `akun` |
| `akunting.transaksi` (satu baris = satu baris jurnal) | `jurnal` (kepala) + `jurnal_detail` (baris) |
| `nota_sales_biaya` + `kategori_biaya_sales` | `sales_trip_biaya` saja |
| `sales_order_lapangan(_item)` untuk HPP | `faktur_penjualan(_detail)` |
| `nota_sales_kas` ber-`jenis='CASH_SALE'` | faktur tanpa baris piutang |

### Tiga keputusan pemetaan yang menentukan angka

**Kategori biaya: join yang lenyap, bukan hilang.** Legacy menautkan `kategori_biaya_sales`;
model tenant menaruh kategorinya langsung pada `sales_trip_biaya.kategori` bertipe
`varchar(64)`. Bukan tabel yang hilang, melainkan normalisasi yang memang ditiadakan.

**HPP ada di FAKTUR, bukan di order.** `sales_order_detail` model tenant **tidak punya kolom
harga pokok sama sekali**; yang punya adalah `faktur_penjualan_detail.harga_beli`. Menghitung
laba kotor dari order akan menghasilkan **nol harga pokok pada seluruh baris** — laba kotor
persis sama dengan omzet, dan terlihat wajar sampai ada yang memeriksanya.

**Penjualan tunai = faktur tanpa piutang.** Model tenant tidak punya penanda `CASH_SALE`;
ketiadaan baris `piutang_customer` yang menjadi penandanya. Polanya sama persis dengan sisi
pembelian pada helper ketiga — pembelian tunai adalah `pembelian` tanpa `hutang_supplier`.

### Yang ditolak, bukan dijalankan sebagian

**Riwayat audit.** `auditHistory` membaca lewat Envers, yang menaruh seluruh barisnya pada
satu schema yang ditetapkan **statis per SessionFactory** (`default_schema=new_audit`).
Membiarkannya berjalan berarti menyajikan riwayat perubahan **seluruh instalasi** kepada satu
tenant — kebocoran, bukan sekadar hasil yang salah.

**Pembuatan akun baru.** `akun` model tenant mewajibkan kolom `tipe` yang tidak pernah dibawa
permintaan legacy. Mengarangnya berarti menebak klasifikasi akun — kesalahan yang baru
terlihat saat laporan keuangan disusun. Pembaruan nama tetap dilayani.

### Saringan lingkup toko tetap ditegakkan

`piutang_customer` model tenant tidak punya kolom toko, sedangkan jalur legacy menyaringnya
langsung. Fakturnya di-`LEFT JOIN` supaya saringan itu tetap berlaku — membuangnya berarti
menyajikan piutang seluruh toko kepada pengguna yang lingkupnya satu toko.

### Perbedaan yang dicatat

Nama produk pada laporan: legacy menyimpan salinannya di baris order (membeku saat
transaksi), tenant menariknya dari master. Bila produk berganti nama, laporan legacy
menampilkan nama lama dan laporan tenant menampilkan nama sekarang.

### Verifikasi — dan apa yang BELUM diverifikasi

Sepuluh pernyataan SQL yang **benar-benar dihasilkan kodenya** dijalankan pada schema tenant
PostgreSQL 16.4: **seluruhnya berjalan tanpa galat**. Itu membuktikan tidak ada tabel atau
kolom yang keliru — kesalahan yang tidak akan ditangkap compiler.

### Uji kesetaraan: `uji-kesetaraan-finance.sql`

Enam blok, seluruhnya **SETARA** pada PostgreSQL 16.4:

| Blok | Legacy = Tenant |
|---|---|
| Bagan akun (nama + induk) | **SETARA** |
| Kas/Jurnal | 2 baris, debet 500.000, kredit 500.000 — **SETARA** |
| Laba kotor per produk | qty 13 dan 5; omzet 52.000 dan 5.000; HPP 32.500 dan 6.000 — **SETARA** |
| Laba-Rugi | kredit 45.000, tunai 12.000, HPP 38.500, beban 225.000 — **SETARA** |
| Beban per kategori | BBM 150.000, Makan 75.000 — **SETARA** |
| Rincian per baris | laba 15.000 dan **−1.000**, sisa piutang 25.000 — **SETARA** |

Yang dibuktikan tiap blok, bukan sekadar "angkanya cocok":

- **Blok 2** membuktikan pemisahan kepala/baris jurnal tersusun kembali dengan benar —
  dua baris legacy menjadi satu `jurnal` berisi dua `jurnal_detail`, dan totalnya utuh.
- **Blok 3** membuktikan HPP dari baris **faktur** menghasilkan angka yang sama dengan
  snapshot pada baris **order**. Inilah pemetaan yang paling mudah menghasilkan nol
  harga pokok tanpa ketahuan.
- **Blok 4** membuktikan penanda penjualan tunai — faktur tanpa baris piutang — menghasilkan
  omzet tunai yang sama dengan ledger kas ber-`CASH_SALE`.
- **Blok 6** sengaja memuat satu baris **rugi** (harga jual 1.000 di bawah harga pokok 1.200).
  Tanda negatifnya bertahan di kedua model; pemetaan yang membalik tanda akan tertangkap di
  sini.

## Helper kelima: Master Supplier / Customer / Sales

`SalesInventoryMasterHelper` — **sebelas aksi, seluruhnya dipindahkan.**

### Pemetaan tingkat KOLOM, bukan tingkat tabel

Empat helper sebelumnya sebagian besar berganti nama tabel. Di sini yang berpindah adalah
**letak medannya**:

| Legacy | Tenant |
|---|---|
| `library.penyedia` + `supplier_inventory_profile` | `supplier` + `supplier_profile` + `supplier_bank_account` |
| `anggota_koperasi` + `customer_inventory_profile` | `customer` + `customer_profile` |
| `sales_inventory` | `salesperson` + `sales_assignment` (berjangka waktu) |
| `a.limit_kredit` | `customer_profile.plafon_piutang` |
| `termin_hari` | `syarat_bayar_hari` |
| `cp.sales_owner` | `customer.salesperson_id` |
| `s.nomor_perkiraan` | `salesperson.akun_perkiraan` |
| `s.area` | `sales_assignment.wilayah` |

Rekening bank pemasok yang dulu tiga kolom pada profil kini menjadi tabel tersendiri yang
boleh berisi lebih dari satu rekening; yang bertanda `utama` yang diambil.

### Kesalahan yang tertangkap saat mencocokkan urutan kolom

Rancangan pertama `selectSupplier` menaruh `akun_utang` di kolom **ke-9**, padahal jalur
legacy menaruhnya di **ke-17**. Kalau lolos, nomor rekening akan muncul di kolom nama.
Compiler tidak akan mengeluh sedikit pun — keduanya sama-sama teks.

`selectSales` juga sempat 11 kolom padahal legacy 13; `target_bulanan` dan `limit_penagihan`
terlewat. Keduanya dikembalikan `NULL`, **bukan nol**: nol berarti "targetnya nol" dan akan
membuat laporan pencapaian melaporkan 100% terhadap target kosong.

### Uji kesetaraan: `uji-kesetaraan-master.sql`

| Blok | Hasil |
|---|---|
| Daftar supplier (identitas + profil + bank) | **SETARA** |
| Daftar customer (identitas + profil + pemilik sales) | **SETARA** |
| Daftar sales (identitas + penugasan + jumlah customer) | **SETARA** |
| Saldo hutang 500.000 dan piutang 550.000 | **SETARA** |

Blok 3 membuktikan penugasan berjangka waktu terbaca benar: toko dan wilayah yang dulu kolom
pada sales kini ditarik dari `sales_assignment` aktif terbaru lewat `LEFT JOIN LATERAL`, dan
hasilnya sama.

Blok 4 memakai rumus yang **sama persis** dengan `SalesInventoryPayableTenant` dan sisi
piutangnya — dihitung dari alokasi, bukan dibaca dari kolom ringkasan. Dua layar yang
menghitung hutang dengan cara berbeda adalah cara pasti melahirkan dua angka.

### Yang sengaja dibiarkan kosong

**`wilayah` pemasok dan pelanggan tidak dipetakan ke `kota`.** Keduanya berdekatan tetapi
berbeda: wilayah adalah pembagian penjualan, kota adalah bagian alamat. Memetakannya membuat
saringan "wilayah = Jawa Barat" mencari **kota** bernama demikian dan mengembalikan nol baris
— saringan yang tampak bekerja padahal tidak pernah cocok. Lebih baik kosong dan terlihat
kosong.

Wilayah **sales** berbeda: model tenant memang menyimpannya, dan yang itu dipetakan sungguhan
(terbukti SETARA di blok 3).

Yang lain tanpa padanan: `akun_utang` dan `keterangan` pemasok, rekening bank pelanggan,
`alamat` dan `target_bulanan` sales.

### Alasan nonaktif: kebutuhan pertama bagi `TenantAuditWriter`

Jalur legacy mewajibkan alasan nonaktif **"untuk audit"** dan menaruhnya pada profil. Model
tenant tidak menyediakan kolom untuk itu. Alasannya tetap **diwajibkan dan divalidasi** agar
perilaku antarmuka tidak berubah, tetapi setelah itu tidak ke mana-mana.

Inilah kebutuhan konkret pertama bagi `TenantAuditWriter`, yang sudah berdiri sejak P4 awal
tanpa satu pun pemanggil. Menyambungkannya di sini akan memberi alasan itu tempat yang benar.

## Helper keenam: Sales Lapangan / Trip — SEBAGIAN, dan itu disengaja

`SalesInventoryTripHelper` — **dua dari sembilan belas aksi** dipindahkan. Tujuh belas
sisanya **ditolak** pada jalur tenant. Ini keputusan sadar, bukan pekerjaan yang tertinggal.

### Mengapa Trip berbeda

| Helper | Baris | Operasi entitas |
|---|---|---|
| Stok | 330 | 0 |
| Harga | 700 | 11 |
| Payable | 933 | 8 |
| Finance | 791 | 5 |
| Master | 1519 | 26 |
| **Trip** | **1517** | **65** |

Enam aksinya menyentuh **uang**: penjualan tunai, setoran, biaya, retur, rekonsiliasi, dan
penutupan trip. Menulis enam puluh lima operasi SQL asli sekaligus, tanpa uji kesetaraan per
bagian, berarti menaruh kesalahan hitung uang di tempat yang paling mahal untuk ditemukan.

### Model tenant di sini DIRANCANG ULANG, bukan dinamai ulang

Berbeda dengan lima helper sebelumnya yang sebagian besar berganti nama tabel:

- `sales_trip_nota` — nota per trip, **dengan pisah tunai/kredit** yang legacy tidak punya
- `sales_trip_hasil` — hasil barang per produk: terjual, kembali, rusak, selisih
- `sales_trip_rekonsiliasi` — rekonsiliasi lengkap: nilai barang bawa/kembali, penjualan,
  biaya, setoran, dan selisihnya

Model yang lebih baik, tetapi memetakannya menuntut keputusan **per aksi**.

### Lingkup toko vs gudang — sempat tampak penghalang, ternyata bukan

Seluruh tabel Trip tenant berlingkup `gudang_id`; jalur legacy menyaring dengan `toko`
(lima rujukan `ctx.tokoId`). Sempat terlihat sebagai penghalang struktural.

Ternyata bukan: **`gudang` memuat `toko_id`**, sehingga saringan toko tetap dapat ditegakkan
lewat `EXISTS`. Itu wajib ditegakkan, bukan dilewati — saringan lingkup yang hilang berarti
sales satu toko melihat perjalanan toko lain.

### Yang belum punya padanan

**`penerimaan_piutang` tenant tidak punya kaitan ke trip**, sedangkan legacy menautkannya
lewat `p.sesi`. Akibatnya `tripDetail` — yang menjumlahkan penerimaan piutang selama satu
trip — tidak dapat dipetakan langsung; jalurnya harus lewat
`sales_trip_nota` → `faktur_penjualan` → `piutang_customer` → alokasinya. Keputusan
tersendiri, bukan efek samping.

`rute` SPJ juga tidak ada, dan `wilayah` bukan padanannya: rute adalah urutan kunjungan,
wilayah adalah pembagian penjualan.

### Kesalahan yang tertangkap lagi oleh pencocokan kolom

`selectSpj` rancangan pertama punya **9 kolom, legacy 12** — `uang_muka_operasional`,
`s.id`, dan `s.nama` terlewat. Kalau lolos, nama sales muncul di kolom kendaraan.
`selectTrip` juga 7 versus 9. Compiler tidak akan mengeluh sedikit pun.

Ini kesalahan yang sama bentuknya dengan yang terjadi di Master (`akun_utang` di kolom ke-9
padahal ke-17). **Pencocokan jumlah dan urutan kolom terhadap jalur legacy perlu menjadi
langkah tetap**, bukan pemeriksaan sesekali.

### Yang ditolak, dan mengapa itu benar

Tujuh belas aksi menolak jalur tenant dengan pesan jelas. Alternatifnya — membiarkan jalur
legacy berjalan — berarti membaca dan, pada aksi bermuatan uang, **menulis** ke schema
bersama. Layar yang tidak tersedia jauh lebih baik daripada setoran satu tenant mendarat di
pembukuan tenant lain.

### Kelompok SPJ selesai: 5 dari 19 aksi

`spjSimpan`, `spjDetail`, dan `spjStatus` menyusul `spjList` dan `tripList`. Empat belas aksi
sisanya masih ditolak.

**Tiga celah model yang membentuk jalur SPJ, dan cara menanganinya:**

**Lingkup: permintaan membawa toko, model tenant menuntut gudang.** `gudang_id` boleh kosong,
tetapi mengosongkannya membuat SPJ **tidak terlihat** oleh saringan lingkup — yang justru
menegakkan lewat `gudang.toko_id`. Satu toko boleh punya beberapa gudang, sehingga
memilihkannya sepihak berarti menebak. Jalur tenant **menuntut `gudang_id` eksplisit**, dan
memverifikasi gudang itu memang milik toko aktor.

**Idempotensi tidak punya tempat.** `surat_perintah_sales` tidak punya `idempotency_key`
maupun `correlation_id`. Bila permintaan membawa `kode_unik`, jalur tenant **menolaknya**
alih-alih mengabaikannya: menerima kunci idempotensi lalu tidak menghormatinya lebih buruk
daripada berterus terang, sebab pemanggil akan mengira pengulangan aman.

**Penugasan piutang ke SPJ tidak ada padanannya.** Legacy menautkan dokumen piutang ke SPJ
lewat `spj_sales_nota` — rencana penagihan sebelum berangkat. Model tenant punya
`sales_trip_nota`, tetapi itu menautkan **trip ke faktur penjualan**: nota yang *dihasilkan*
selama perjalanan, bukan piutang yang *direncanakan* untuk ditagih. Berbeda arah waktu dan
berbeda maksud. Daftar nota pada rinci SPJ karena itu selalu kosong, dan `spjNotaAssign`
tetap ditolak.

### Uji kesetaraan: `uji-kesetaraan-spj.sql`

| Blok | Hasil |
|---|---|
| Rinci SPJ (nomor, status, kendaraan, toko, sesi) | **SETARA** |
| Baris barang (termasuk kuantitas pecahan 15,5) | **SETARA** |
| **Saringan lingkup toko** | **SETARA** — toko 1 melihat 1 SPJ, toko 2 melihat 0 |

Blok 3 adalah yang terpenting: ia membuktikan saringan lingkup lewat `gudang.toko_id`
menghasilkan **visibilitas yang sama persis** dengan saringan langsung legacy — termasuk
sisi negatifnya, yakni toko lain tetap tidak melihat apa pun. Saringan lingkup yang bocor
tidak akan terlihat dari blok 1 maupun 2.

Blok 1 juga membuktikan dua turunan bekerja: kendaraan ditarik dari trip (di model tenant ia
melekat pada trip, bukan SPJ), dan toko diturunkan dari gudangnya.

### BELUM: empat belas aksi sisanya

Termasuk keenam aksi bermuatan uang. Ujinya wajib ditulis **bersamaan** dengan
pemindahannya, bukan sesudahnya.

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
