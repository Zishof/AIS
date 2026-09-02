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

**Pembuatan akun baru — DIBUKA pada §17.** `akun` model tenant mewajibkan kolom `tipe` yang
tidak pernah dibawa permintaan legacy, dan mengarangnya berarti menebak klasifikasi akun. Yang
tidak tercatat di sini waktu itu adalah akibatnya: `coaSave` satu-satunya penulis tabel itu dan
tidak ada penyemai bagan akun, sehingga penolakan ini membuat `jurnal_detail` — yang
`akun_id`-nya `NOT NULL REFERENCES akun(id)` — tidak akan pernah bisa ditulis. Sejak §17,
kelasnya diminta dari permintaan atau **diwarisi** dari akun induk; menebaknya dari awalan kode
tetap tidak dilakukan.

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

### Kelompok trip non-uang: 7 dari 19 aksi

`tripStart` dan `tripBarangUpdate` menyusul. Dua aksi lain dalam kelompok ini **tetap
ditolak**, dan alasannya bukan kekurangan waktu:

**`tripNotaResult` beroperasi pada konsep yang tidak ada.** Ia memperbarui hasil kunjungan
pada `spj_sales_nota` — nota kunjungan per SPJ. Sebagaimana dicatat di kelompok SPJ, model
tenant tidak mengenalnya.

**`tripDetail` menjumlahkan dua hal tanpa padanan.** Biaya dan setoran punya padanan, tetapi
pembelian selama trip (`nota_sales_pembelian`) tidak punya tabel, dan penerimaan piutang
tidak punya kaitan ke trip. Menyajikan sebagiannya sebagai total akan memberi angka yang
lebih kecil dari sebenarnya — tanpa tanda apa pun bahwa ada yang hilang.

### Uang muka operasional: perbedaan rancangan, bukan medan yang hilang

Jalur legacy memulai trip dengan `saldoKasAwal` yang disalin dari `spj.uangMukaOperasional`
— kas mengambang yang diperhitungkan saat rekonsiliasi.

Model tenant tidak punya keduanya, dan rekonsiliasinya **memang tidak memakainya**:
`sales_trip_rekonsiliasi` menimbang nilai barang bawa, barang kembali, penjualan, biaya, dan
setoran. Jadi `tripStart` pada jalur tenant memulai trip tanpa saldo kas awal, dan itu benar
untuk model ini.

### Rencana dan hasil dipisah, dan itu lebih baik

Legacy menyimpan rencana **dan** hasil pada satu baris `spj_sales_barang`: `qty_rencana`,
`qty_dimuat`, lalu `qty_terjual`/`qty_kembali`/`qty_rusak`/`qty_hilang` menimpanya seiring
trip berjalan.

Model tenant memisahkannya: `sales_trip_barang` (yang dibawa) dan `sales_trip_hasil` (yang
terjadi). Berapa yang dibawa tetap terbaca sesudah trip ditutup.

`qty_hilang` dipetakan ke `selisih` — keduanya menyatakan kuantitas yang tidak kembali dan
tidak terjual.

### Uji kesetaraan: `uji-kesetaraan-trip-nonuang.sql`

| Blok | Hasil |
|---|---|
| Barang yang dibawa (salinan rencana SPJ) | **SETARA** |
| Hasil barang: terjual / kembali / rusak / hilang→selisih | **SETARA** |
| **Keseimbangan: bawa = terjual + kembali + rusak + hilang** | **SETARA & SEIMBANG** |

Blok 3 yang terpenting: ia membuktikan pemisahan menjadi dua tabel **tidak merusak
keseimbangan kuantitas**. Kesalahan pemetaan `hilang`→`selisih` akan terlihat di sini
sebagai ketidakseimbangan, bukan sekadar angka berbeda.

### CACAT YANG SUDAH TERKIRIM, dan koreksinya

Jalur tenant `tripList` yang dikirim pada r83047 menghitung saldo kas sesi sebagai
`SUM(sales_trip_setoran.nilai)` — yakni **total setoran**.

Itu salah. Legacy menghitungnya dari `nota_sales_kas`, dan pada buku kas itu penjualan tunai
bertanda **positif** sedangkan setoran ke pemilik **dinegatifkan** oleh `catatKas()`. Yang
dimaksud legacy adalah **kas yang masih dipegang sales**, bukan yang sudah disetor.

Untuk trip bertunai 1.000.000 dan setoran 800.000: legacy **200.000**, versi keliru
**800.000**. Empat kali lipat, pada kolom yang dibaca sebagai uang di tangan.

**Koreksinya**: model tenant memisahkan bagian tunai tiap nota (`sales_trip_nota.tunai`) dari
setorannya (`sales_trip_setoran.nilai`, seluruhnya positif karena tabelnya khusus setoran).
Saldo kas = tunai − setoran, sebagaimana penjumlahan bertanda pada jalur legacy.

**Mengapa lolos**: uji kesetaraan untuk kelompok SPJ dan trip non-uang tidak memuat blok kas
sama sekali. Kuerinya sah, kolomnya cocok, dan compiler tidak punya cara mengetahui bahwa dua
angka bertipe sama punya arti berbeda.

`uji-kesetaraan-trip-kas.sql` ada supaya tidak terulang. Blok 1-nya memuat **penjaga**: ia
memeriksa bahwa contoh ujinya memang membedakan rumus benar dari rumus keliru — sebab contoh
yang membuat keduanya kebetulan sama akan lulus tanpa membuktikan apa pun.

| Blok | Hasil |
|---|---|
| Saldo kas sesi (tunai − setoran) | **SETARA** 200.000 = 200.000 |
| Penjaga: versi keliru memang berbeda | **BERBEDA** (800.000) |
| Total setoran | **SETARA** |
| Penjualan tunai | **SETARA** |
| Perpindahan status ACTIVE → RETURNED | **SETARA** |

### Kelompok biaya: TERHALANG, bukan tertunda

`expenseCategoryList`, `expenseCategorySave`, dan `expenseCreate` tidak dipindahkan karena
dua celah katalog:

**Tidak ada tabel master kategori biaya.** Legacy mengelolanya sebagai master ber-kode,
ber-nama, dan ber-status aktif. Model tenant menaruh kategorinya sebagai kolom
`varchar(64)` pada `sales_trip_biaya`. `expenseCategorySave` karena itu tidak punya sasaran
tulis sama sekali.

**Tidak ada kolom idempotensi pada `sales_trip_biaya`**, sedangkan legacy **mewajibkan**
`kode_unik` untuk `expenseCreate` — aksi yang menulis uang. Tanpa kolom itu, satu permintaan
yang diulang (gangguan jaringan, klien mencoba lagi) akan **membukukan biaya dua kali**, dan
langsung merusak total biaya trip pada rekonsiliasi.

Yang kedua bukan sesuatu yang pantas dilewati dengan catatan. Perbaikannya migrasi **v10**
yang menambahkan `idempotency_key` pada `sales_trip_biaya` — keputusan Anda, bukan efek
samping pemindahan kueri.

### BELUM: dua belas aksi sisanya

Termasuk keenam aksi bermuatan uang, dan dua aksi kelompok ini yang tertahan pada celah
model. Ujinya wajib ditulis **bersamaan** dengan pemindahannya, bukan sesudahnya.

## Helper ketujuh: Piutang &amp; Sales Order — 6 dari 12 aksi

Dibuka oleh migrasi **v10**. Enam aksi yang murni membaca dipindahkan; enam yang menulis
lewat entitas — termasuk `collectionCreate` yang mencatat penerimaan uang — **ditolak**
sampai ditulis beserta uji kesetaraannya.

### Piutang tenant tidak menyimpan toko maupun order

`piutang_customer` menautkan dirinya ke `faktur_penjualan`, dan **fakturnya** yang menyimpan
`toko_id` serta `sales_order_id`. Saringan lingkup toko dan penyaringan per order karena itu
ditempuh lewat faktur.

Itu **wajib** ditegakkan, bukan dilewati: saringan lingkup yang hilang berarti satu toko
melihat piutang toko lain.

### Uang muka: kolom menjadi dokumen

Legacy menyimpan uang muka pada kolom `dibayar_awal` yang tidak punya dokumen pembayaran.
Model tenant mencatatnya sebagai **penerimaan beralokasi** seperti pembayaran lain, sehingga
uang muka pun punya jejak dokumennya sendiri.

Rumus sisa karena itu berbeda bentuk tetapi sama hasilnya: legacy `total − dibayar_awal −
Σalokasi`, tenant `nilai − Σalokasi` — sebab uang mukanya sudah termasuk alokasi.

### Kesalahan kolom, ketiga kalinya

`selectPenagihan` rancangan pertama punya **12 kolom, legacy 14** — `status_dok` dan
`status_bg` terlewat.

Ini pola yang sama dengan Master (`akun_utang` di kolom ke-9 padahal ke-17) dan Trip
(`selectSpj` 9 padahal 12). **Tiga kali berturut-turut**, dan compiler tidak menangkap satu
pun. Pencocokan jumlah kolom kini saya lakukan sebelum menulis, bukan sesudah.

`status_bg` sendiri tidak punya padanan; dikembalikan `NULL`, bukan disamakan dengan status
dokumen — menyamakannya membuat giro yang belum cair tampak sudah beres.

### Uji kesetaraan: `uji-kesetaraan-piutang.sql`

| Blok | Hasil |
|---|---|
| Sisa piutang per dokumen | **SETARA** 450.000 dan 500.000 |
| Umur piutang | **SETARA** |
| **Lingkup toko lewat faktur** | **SETARA** — tiap toko melihat miliknya saja |
| Total tertagih (uang muka + pembayaran) | **SETARA** 350.000 |

Blok 3 memuat **penjaga** yang memeriksa contoh ujinya memang menyaring: bila seluruh piutang
kebetulan milik satu toko, uji lingkup akan lulus tanpa membuktikan apa pun.

Blok 1 dan 4 bersama-sama membuktikan pemetaan uang muka: sisa 450.000 hanya benar bila
uang muka 100.000 <b>dan</b> pembayaran 250.000 keduanya terhitung, dan totalnya 350.000.

## Helper kedelapan: Reversal &amp; Log Cetak — 3 dari 7 aksi

`SalesInventoryReversalHelper` (524 baris) berisi tujuh aksi. Tiga dipindahkan; **empat
ditolak**, dan keempatnya karena model tenant tidak menyediakan tempatnya.

| aksi | jalur tenant |
|---|---|
| `payablePaymentReverse` | **dipindahkan** |
| `printLogCreate` | **dipindahkan** |
| `printLogList` | **dipindahkan** |
| `collectionReverse` | ditolak — tidak ada buku kas trip |
| `expenseReverse` | ditolak — `sales_trip_biaya` tanpa `pembalik_dari_id` |
| `payableBgStatus` | ditolak — status giro tidak disimpan |
| `collectionBgStatus` | ditolak — status giro tidak disimpan |

### Sisi pembalikan justru lebih matang di tenant

Berbeda dengan kebanyakan celah sebelumnya, di sini model tenant **melebihi** legacy:
`pembalik_dari_id` tersedia pada enam tabel dokumen, dan ada `reversal_log` yang mencatat dokumen
asal, jurnal asal, jurnal pembalik, alasan, serta pelakunya. Legacy hanya punya `reversalDari`
pada dokumennya sendiri. Jejaknya bertambah, bukan berkurang.

### Yang mengembalikan sisa hutang adalah alokasi negatif

Legacy membalik dengan dokumen cermin bernilai negatif, lalu mencerminkan tiap alokasinya juga
negatif. Bentuk itu dipertahankan persis, dan bukan demi kemiripan: rumus sisa yang dipakai
helper Payable adalah `nilai − Σalokasi`. Alokasi negatiflah yang mengembalikan sisa hutang;
dokumen pembaliknya sendiri tidak menyentuh angka itu sama sekali.

Blok 2 `uji-kesetaraan-reversal.sql` membuktikan hal itu dari sisi negatifnya: dengan alokasi
negatif diabaikan, sisanya tetap 600.000 dan tidak kembali ke 1.000.000. Tanpa blok itu, blok 1
tidak membuktikan apa pun — angka yang benar bisa saja benar karena sebab lain.

### Satu tambahan yang dipaksa model

`pembayaran_hutang.nomor_dokumen` berstatus `NOT NULL`, sedangkan legacy tidak memberi nomor pada
dokumen pembalik AP. Nomornya diturunkan sebagai `"REV-" + nomor asal`, mengikuti pola yang sudah
dipakai legacy pada sisi piutang.

### Idempotensi: celah yang ditemukan sambil jalan, lalu ditutup v11

Pemindahan ini menuntut jaminan bahwa satu perintah balik tidak melahirkan dua dokumen pembalik.
Ternyata jaminan itu tidak ada — bukan hanya di sini, melainkan pada sebelas tabel. Rinciannya di
`03-migrasi-schema.md`, bundel v11.

Jalur tenant kini berpenjaga dua lapis, sama seperti legacy: pemeriksaan kunci di muka untuk
percobaan berurutan, dan indeks unik untuk permintaan yang bersamaan (`SQLState` kelas `23`
diperlakukan sebagai pengulangan yang sah, persis seperti legacy memperlakukan
`ConstraintViolationException`).

### `collectionReverse`: ditolak utuh, bukan dipindahkan separuh

Legacy mengerjakan lima hal. Model tenant mendukung tiga:

| yang dikerjakan legacy | ada di tenant? |
|---|---|
| dokumen cermin negatif + penunjuk asal | ya |
| alokasi negatif | ya |
| sales order LUNAS mundur ke SIAP_TAGIH | ya (lewat piutang → faktur → order) |
| `SpjSalesNota.nilaiTertagih` dimundurkan | **tidak** — tidak ada tabelnya |
| baris kas sesi negatif (`NotaSalesKas`) | **tidak** — tidak ada `sales_trip_kas` |

Tabel tenant yang namanya mirip, `sales_trip_nota`, adalah nota **penjualan yang diterbitkan**
dalam trip (`faktur_penjualan_id`, `tunai`, `kredit`) — konsep berbeda, tanpa kolom nilai
tertagih.

Tiga bagian yang bisa dikerjakan sengaja **tidak** dikerjakan. Memindahkan sebagian berarti
membalik piutangnya tanpa membalik kasnya, sehingga sales tetap tampak memegang uang yang tidak
pernah diterimanya. Itu kelas cacat uang yang paling sulit ditemukan belakangan, dan sudah sekali
terkirim pada helper ini (lihat "CACAT YANG SUDAH TERKIRIM" di atas). Tidak diulang.

### Celah C-11: model tenant tidak punya buku kas trip

Ketiadaan `sales_trip_kas` bukan hanya menghalangi `collectionReverse`. Ia juga berarti **angka
`saldoKas` pada `tripList` jalur tenant belum setara**, dan ini menyangkut kode yang sudah
terkirim (r83075).

Legacy menghitungnya sebagai penjumlahan bertanda seluruh buku kas sesi, yang mencakup sembilan
jenis baris: `OPENING_ADVANCE`, `COLLECTION_CASH`, `CASH_SALE`, `EXPENSE_CASH`,
`PURCHASE_PAYMENT`, `OWNER_DEPOSIT`, `REFUND`, `ADJUSTMENT`, `REVERSAL`.

Rumus tenant saat ini hanya `Σ nota.tunai − Σ setoran.nilai` — mencakup `CASH_SALE` dan setoran,
tetapi tidak uang muka operasional, tidak penagihan tunai piutang lama, dan tidak biaya tunai.
Untuk trip dengan panjar 500.000, penjualan tunai 300.000, penagihan tunai 200.000, biaya 100.000,
dan setoran 400.000: legacy menghasilkan 500.000, rumus sekarang menghasilkan −100.000.

Perbaikan sebelumnya (r83075) memang memperbaiki cacat yang lebih besar — waktu itu rumusnya
menjumlahkan setoran saja — tetapi **belum menuntaskannya**, dan saat itu saya menyatakannya
selesai. Yang tersisa tidak dapat ditutup dengan menulis ulang kuerinya: `sales_trip` tidak punya
kolom panjar, dan tidak ada tabel tempat menaruh baris kas.

Bundel yang menutupnya perlu sekurang-kurangnya `sales_trip.saldo_kas_awal` dan satu tabel buku
kas trip. Bundel itu sekaligus membuka `collectionReverse` dan `expenseReverse`. Belum dibuat —
menambah tabel baru adalah keputusan rancangan, bukan tambalan, dan pantas diputuskan terpisah.

### `expenseReverse` dan siklus giro

`sales_trip_biaya` adalah satu-satunya tabel dokumen tanpa `pembalik_dari_id`, tanpa `status`, dan
tanpa kolom metode pembayaran. Baris pembalik bernilai negatif tanpa penunjuk asal menghasilkan
dua baris yang tidak dapat dipasangkan kembali: totalnya benar, tetapi tidak ada yang tahu baris
mana membalik baris mana.

Untuk giro, model tenant menyimpan `nomor_bg` dan `tanggal_bg` tetapi **tidak statusnya** —
sehingga giro yang sudah cair tidak dapat dibedakan dari yang ditolak, dan itu justru inti kedua
aksi `*BgStatus`.

### Log cetak: enam medan, dan satu kolom yang namanya tidak cocok

`printLogList` legacy mengembalikan enam medan (id, jenisDokumen, referensi, userId, perangkat,
waktu) dan jalur tenant mengembalikan enam yang sama. Rancangan pertama saya menulis delapan
kolom — pencacah cetakan dan id dokumen ikut terbawa — dan itu akan mengubah bentuk JSON-nya.
Tertangkap saat mencocokkan ke kode legacy sebelum menulis, bukan sesudah.

`LogCetak.parameterJson` tidak punya kolom sendiri pada `print_log`; isinya disimpan pada `alasan`,
satu-satunya kolom teks bebas. Namanya tidak cocok dan itu dicatat apa adanya di javadoc —
membuang isian yang pada jalur legacy tersimpan jelas lebih buruk daripada menaruhnya di kolom
yang namanya kurang tepat.

`cetakan_ke` justru sebaliknya: kolom milik tenant yang tidak punya padanan legacy. Diisi
`MAX+1` karena membiarkannya bernilai bawaan membuat setiap baris mengaku cetakan pertama. Dua
permintaan cetak yang bersamaan dapat memperoleh angka sama; itu diterima, sebab registernya
catatan dan tidak ada keputusan uang yang bergantung padanya.

### Uji kesetaraan: `uji-kesetaraan-reversal.sql`

Enam blok, seluruhnya LULUS pada klaster v1–v11:

| blok | yang dibuktikan |
|---|---|
| 1 | sisa hutang setelah pembalikan setara legacy (1.000.000) |
| 2 | **penjaga** — tanpa alokasi negatif sisanya tetap 600.000, jadi blok 1 memang membedakan |
| 3 | **penjaga** — kembar lolos tanpa indeks v11, ditolak dengan indeks v11 |
| 4 | baris berkunci `NULL` tetap boleh banyak (indeksnya parsial) |
| 5 | dokumen asal ditandai DIBATALKAN, `reversal_log` terisi satu baris |
| 6 | `printLogList` mengembalikan enam medan yang sama, parameter tetap tersimpan |

Berkasnya dapat dijalankan berulang: data jalan uji sebelumnya dibersihkan lebih dulu menurut
urutan kunci asing.

## Helper ketujuh, lanjutan: Piutang &amp; Sales Order — 9 dari 12 aksi

Batch ini menambah tiga aksi: `salesOrderDetail`, `salesOrderStatus`, `collectionReceipt`.
Tersisa tiga, dan ketiganya menulis dokumen.

| aksi | jalur tenant |
|---|---|
| `salesOrderDetail` | **dipindahkan** |
| `salesOrderStatus` | **dipindahkan** |
| `collectionReceipt` | **dipindahkan** |
| `salesOrderSimpan` | belum ditulis |
| `salesOrderInvoice` | belum ditulis |
| `collectionCreate` | **terhalang C-11** — bukan sekadar belum ditulis |

### Kosakata status: katalog menyingkat DRAFT, jalur `si_*` tidak

`SalesOrderLapangan.STATUS_DRAFT` bernilai `"DRAFT"`. Bawaan kolom `sales_order.status` pada
katalog tenant adalah **`'DRAF'`** — begitu pula dua belas tabel lain. Itu kosakata katalog,
bukan salah ketik satu tempat.

Akibatnya bukan kosmetik. Penjaga transisi membandingkan `"DRAFT".equals(lama)`, sehingga setiap
order yang statusnya berasal dari bawaan kolom akan **menolak seluruh transisi keluar dari
draf**: tidak bisa dikonfirmasi, tidak bisa dibatalkan. Ordernya macet tanpa pesan yang
menjelaskan sebabnya.

Diterjemahkan di batas, bukan dilawan: basis data tetap berbicara kosakata katalog, API tetap
berbicara kosakata legacy (`statusOrder()`). Nilai lain — `PESAN`, `SIAP_KIRIM`, `TERKIRIM`,
`BATAL` — sudah sama persis di kedua sisi, jadi hanya draf yang perlu diterjemahkan.

**Ini juga memperbaiki jalur yang sudah terkirim.** `salesOrderList` mengembalikan kolom status
apa adanya, sehingga klien tenant menerima `DRAF` di tempat klien legacy menerima `DRAFT`.
Bukan cacat uang, tetapi tetap dua klien yang melihat kata berbeda untuk keadaan yang sama, dan
setiap penyaringan sisi klien atas `"DRAFT"` akan meleset. Sekarang keduanya dinormalkan lewat
satu fungsi.

### MTO tidak dilewati — memang tidak ada yang bisa dipicu

Jalur legacy menjalankan `terapkanMto` pada transisi DRAFT → PESAN: baris ber-produk rute
`MTO_PRODUKSI` menerbitkan draf Work Order, rute `MTO_BELI` menerbitkan pengajuan pembelian
gudang. Komentarnya tegas — SO yang mengaku terkonfirmasi tetapi pemicunya gagal adalah
kebohongan data.

Katalog tenant **tidak punya kolom `produk.rute`**, dan tidak punya tabel work order maupun
pengajuan pembelian. Tidak ada produk tenant yang dapat berute MTO, sehingga pemicunya kosong
menurut definisi. Konfirmasi order karena itu setara, bukan disederhanakan.

Penjaganya (`mtoMungkin()`) tetap dipasang dan mengembalikan `false`. Kalau suatu bundel kelak
menambahkan `produk.rute`, penjaga itu diubah menjadi `true` dan konfirmasi order akan
**berhenti berisik** alih-alih diam-diam melewatkan pemicunya.

### Deep-link piutang: legacy menunjuk order, tenant lewat faktur

Legacy menyimpan `PiutangCustomerDoc.salesOrder`. Model tenant tidak punya kolom itu; kaitannya
melewati `piutang_customer` → `faktur_penjualan` → `sales_order`.

Kedua subkueri (id dan nomor) memakai `ORDER BY d.id LIMIT 1` yang sama supaya keduanya pasti
berasal dari baris yang sama. Jalur legacy memakai `setMaxResults(1)` tanpa urutan — hasilnya
sewenang-wenang bila satu order punya lebih dari satu dokumen piutang. Di sini hasilnya menjadi
tentu; itu perbedaan yang memperbaiki, dan dicatat sebagai perbedaan.

Blok 5 ujinya menguji sisi negatifnya: faktur milik order lain tidak boleh ikut tertaut.

### Nama produk: salinan beku lawan nama sekarang

Baris order legacy menyimpan salinan `namaProduk` yang membeku saat order dibuat; baris order
tenant tidak punya kolom itu, sehingga namanya ditarik lewat join ke `produk`.

Produk yang berganti nama karena itu tampil dengan nama lama pada jalur legacy dan nama sekarang
pada jalur tenant. Blok 4 ujinya sengaja memakai produk yang sudah berganti nama, dan
**mensyaratkan** perbedaan itu muncul — kalau namanya kebetulan sama, ujinya tidak membuktikan
apa pun tentang perilaku ini.

### `collectionCreate`: pesan penolakannya dijujurkan

Sebelumnya ketiga aksi tersisa memakai pesan yang sama, "belum tersedia... sampai jalur tenantnya
ditulis". Untuk `collectionCreate` itu menyesatkan: aksinya membukukan penerimaan tunai ke kas
sesi (`NotaSalesKas`) dan memundurkan nota bawaan (`SpjSalesNota`), dan model tenant tidak punya
keduanya. Ia **terhalang C-11**, bukan menunggu giliran ditulis. Pesannya kini mengatakan itu.

### Penempatan cabang tenant: sesudah hak akses, bukan sebelumnya

Penjaga tenant yang lama berdiri di **baris pertama** tiap aksi, sebelum `ctx.bolehMenu(...)`.
Selama penjaganya menolak, urutan itu tidak berakibat. Begitu penjaganya berubah menjadi
pengalihan ke jalur tenant, urutan itu menjadi lubang: pemakai tenant akan melewati pemeriksaan
hak menu sama sekali.

Cabangnya karena itu dipindah ke **sesudah** seluruh pemeriksaan hak dan validasi argumen —
sama seperti yang sudah dilakukan pada helper Reversal. Penjaga lingkup sales (order/kwitansi ini
milik sales yang mana) ditegakkan di dalam metode tenantnya, memakai `salesperson_id`, bukan
nama.

### Uji kesetaraan: `uji-kesetaraan-order-kwitansi.sql`

Tujuh blok, seluruhnya LULUS pada klaster v1–v11:

| blok | yang dibuktikan |
|---|---|
| 1 | **penjaga** — katalog memang menyimpan `DRAF`, jadi ada yang perlu dinormalkan |
| 2 | setelah dinormalkan, status tenant setara legacy |
| 3 | header rincian order setara |
| 4 | angka baris order setara **dan** perbedaan nama produk benar-benar terjadi |
| 5 | deep-link piutang menemukan tepat satu, dan bukan milik order lain |
| 6 | kwitansi setara |
| 7 | rincian alokasi kwitansi setara |

Selain itu SQL yang **benar-benar dikeluarkan Java** dijalankan apa adanya ke basis data lewat
alat sekali-pakai, bukan hanya salinan tangannya di berkas uji. Itu yang memastikan jumlah dan
urutan kolomnya benar pada string yang dipakai program — bukan pada versi yang saya tulis ulang.

## Helper ketujuh, tuntas kecuali satu: 11 dari 12 aksi

Batch ini menambah `salesOrderSimpan` dan `salesOrderInvoice`. Yang tersisa hanya
`collectionCreate`, dan ia terhalang C-11 — bukan menunggu giliran ditulis.

### Penyaring status: bug yang lebih tajam daripada yang ditutup batch sebelumnya

Batch sebelumnya menormalkan status yang **ditampilkan**. Yang belum tertutup adalah status yang
**disaring**:

```java
if (!status.isEmpty()) where.append(" AND o.status = ?");
```

Klausa itu dipakai kedua jalur. Klien mengirim `DRAFT` (kosakata legacy), kolom tenant berisi
`DRAF`, sehingga saringan "hanya draf" mengembalikan **kosong**. Bukan salah tampil — order yang
hilang sama sekali dari daftar, tanpa pesan apa pun. Sekarang jalur tenant memakai ekspresi
ternormalkan yang sama dengan sisi pembacaannya.

Blok 1 ujinya membuktikan keduanya sekaligus: saringan mentah menemukan 0, saringan ternormalkan
menemukan 1, atas baris yang sama.

### `salesOrderSimpan`: tiga hal legacy tanpa rumah di tenant

**1. Satuan jual — DITOLAK, bukan diabaikan.** Legacy menurunkan jumlah dasar dari
`qty_input × faktor` lewat `KantinHelper.faktorUomInputKeDasar`, dan menyatakan tegas bahwa jumlah
kiriman klien hanya pratinjau. Katalog tenant tidak punya tabel `satuan_produk` maupun kolom
penampung `satuan_jual`/`qty_input`/`faktor_ke_dasar`.

Menerima permintaan ber-`satuan_jual_id` berarti membiarkan angka pratinjau klien menjadi angka
resmi — persis yang dijaga jalur legacy. Karena itu barisnya ditolak dengan pesan yang menyuruh
mengirim jumlah dalam satuan dasar, bukan dikonversi diam-diam.

**2. Salinan nama produk.** Sudah dicatat pada batch sebelumnya: baris order tenant menariknya
lewat join, sehingga produk yang berganti nama tampil dengan nama sekarang.

**3. Snapshot HPP per baris — celah C-12.** Legacy membekukan harga beli pada tiap baris order
untuk perhitungan margin. Model tenant menaruh biaya pada `faktur_penjualan.hpp`: di tingkat
faktur, bukan baris, dan pada saat pemfakturan, bukan pemesanan.

Itu perbedaan rancangan yang koheren, tetapi akibatnya nyata — margin per baris order tidak dapat
dihitung mundur pada tenant. Tidak ada kueri tenant yang membacanya saat ini, jadi tidak ada yang
rusak hari ini; dicatat supaya tidak ditemukan sebagai kejutan saat laporan margin dipindahkan.

**Nomor dokumen.** `sales_order.nomor_dokumen` berstatus `NOT NULL`, jadi barisnya disisipkan
dengan nomor kosong lalu ditimpa setelah id-nya diketahui — sama persis dengan legacy yang
menyimpan dulu baru memanggil `setNomor`. Aman karena indeks nomor pada `sales_order` tidak unik.

**Tanggal.** Kolom tenant `NOT NULL` sedangkan legacy membolehkannya kosong; bila permintaan tidak
menyertakannya, dipakai tanggal server hari ini.

### `salesOrderInvoice`: satu dokumen legacy menjadi DUA dokumen tenant

Legacy menerbitkan satu `PiutangCustomerDoc` yang merangkap faktur dan piutang. Model tenant
memisahkannya:

| peran | tabel tenant | memegang |
|---|---|---|
| dokumen penjualan | `faktur_penjualan` | `toko_id`, `sales_order_id`, nomor, total |
| tagihan | `piutang_customer` | jatuh tempo, nilai, sisa |

Keduanya lahir dalam **satu transaksi** bersama pemutakhiran status ordernya. Faktur tanpa piutang
berarti barang terjual yang tak pernah ditagih; piutang tanpa faktur memutus seluruh penelusuran
ke ordernya — termasuk saringan lingkup toko, yang pada model tenant memang ditempuh lewat faktur.

**`dibayar_awal` ditolak.** Legacy menyimpannya sebagai kolom pada dokumen piutang. Pada model
tenant uang muka bukan kolom melainkan **alokasi penerimaan**, sehingga menghormatinya berarti
ikut menerbitkan dokumen penerimaan uang — jauh melampaui "menerbitkan faktur", dan menyentuh kas
yang celahnya masih terbuka. Ditolak daripada dibuang diam-diam.

**Nomor sementara harus unik.** `faktur_penjualan` dibatasi `UNIQUE (customer_id, nomor_faktur,
tanggal)`. Menyisipkan nomor kosong seperti pada `sales_order` akan membuat dua faktur untuk
customer yang sama pada hari yang sama bertabrakan. Nomor sementaranya karena itu memakai kunci
idempotensinya (`SO-INV-<orderId>`), yang sudah unik per order.

**Termin.** Diambil dari `customer_profile.syarat_bayar_hari`. Legacy menyaring profil yang
`aktif`; profil tenant tidak punya kolom itu dan dibatasi `UNIQUE (customer_id)` — satu customer
tepat satu profil, jadi tidak ada yang perlu disaring.

### Idempotensi keduanya kini benar-benar mengikat

`sales_order` dan `faktur_penjualan` termasuk sebelas tabel yang memperoleh indeks unik parsial
dari v11. Kedua aksi ini memakai penjaga dua lapis yang sama dengan legacy: pemeriksaan kunci di
muka untuk percobaan berurutan, dan pelanggaran batasan (`SQLState` kelas `23`) diperlakukan
sebagai pengulangan yang sah. Blok 6 ujinya membuktikan kembar ditolak pada kedua tabel.

### Koreksi metode: pola pencarian tabel saya punya titik buta

Daftar tabel yang saya pakai untuk menyatakan sesuatu "tidak ada pada katalog tenant" dibangun
dari pola `CREATE TABLE "<skema>".<nama>`. Bundel v1 membuat sebagian tabelnya dengan
`CREATE TABLE IF NOT EXISTS`, dan tiga tabel — `brand`, `pedagang`, `toko` — karena itu **tidak
pernah muncul** di daftar saya.

Ditemukan saat `sales_order.toko_id` perlu divalidasi: `toko` di-referensi kunci asing tetapi
tidak ada di daftar. Seluruh klaim "tidak ada" yang sudah terlanjur ditulis diperiksa ulang dengan
pola yang benar: `sales_trip_kas`, `nota_sales_kas`, `work_order`, `pengajuan_pembelian_gudang`,
`satuan_produk`, `spj_sales_nota`, dan kolom `produk.rute` memang **tidak ada**. C-11 dan
penalaran MTO tetap berdiri.

Jumlah tabel schema ERP yang sebenarnya: **74**.

### Uji kesetaraan: `uji-kesetaraan-order-simpan-faktur.sql`

Enam blok, seluruhnya LULUS pada klaster v1–v11:

| blok | yang dibuktikan |
|---|---|
| 1 | **penjaga** — saringan mentah menemukan 0, ternormalkan menemukan 1, atas baris yang sama |
| 2 | total header setara jumlah barisnya |
| 3 | pengubahan MENGGANTI baris (bukan menambah), dan tanggal tidak hilang saat tidak dikirim |
| 4 | faktur + piutang lahir bersama; jatuh tempo = tanggal + termin; order maju ke SIAP_TAGIH |
| 5 | deep-link `salesOrderDetail` menemukan faktur yang baru terbit |
| 6 | kunci idempotensi kembar ditolak pada `sales_order` **dan** `faktur_penjualan` |

Seluruh SQL tulis yang **benar-benar dikeluarkan Java** juga dijalankan berurutan ke basis data —
sisip order, sisip baris, finalisasi nomor, baca untuk ubah, hapus-dan-sisip-ulang, perbarui,
baca untuk faktur, sisip faktur, finalisasi, sisip piutang, tandai siap tagih — dan bolak-baliknya
terbukti benar: `sisipOrder` menulis `DRAF`, `orderUntukUbah` membacanya kembali sebagai `DRAFT`.

## Helper keenam, lanjutan: Trip — 10 dari 19 aksi

Batch ini menambah tiga aksi: `tripDeposit`, `tripReturn`, `tripReconcile`. Sembilan sisanya
tetap tertutup, dan sekarang alasannya dapat dipisahkan dengan tegas.

| aksi | jalur tenant | sebab |
|---|---|---|
| `tripDeposit` | **dipindahkan** | setoran punya rumah: `sales_trip_setoran` |
| `tripReturn` | **dipindahkan** | murni transisi status |
| `tripReconcile` | **dipindahkan** | penjaga barangnya dapat dinyatakan ulang |
| `tripCashSale` | ditolak | tidak ada buku kas, dan nota bukan padanannya |
| `spjNotaAssign`, `tripNotaResult` | ditolak | tidak ada tabel nota bawaan bernilai tertagih |
| `tripDetail`, `tripClose` | ditolak | keduanya membaca/menutup buku kas |
| `expenseCategoryList`, `expenseCategorySave` | ditolak | tidak ada master kategori biaya |
| `expenseCreate` | ditolak | sisi kasnya tidak punya rumah |
| `tripPurchaseLink` | ditolak | tidak ada tabel pembelian dalam trip |

### Setoran bisa, penjualan tunai tidak — dan bedanya bukan kebetulan

Jalur legacy mencatat keduanya sebagai baris buku kas sesi: `CASH_SALE` positif dan
`OWNER_DEPOSIT` negatif, lewat satu fungsi `catatKasSederhana`. Sekilas keduanya sama-sama
terhalang C-11.

Ternyata tidak. Model tenant memang tidak punya buku kas, tetapi **punya rumah untuk setoran** —
`sales_trip_setoran`, dokumen tersendiri — dan rumus `saldoKas` yang sudah dipakai `tripList`
memang mengurangkan setoran. Jadi arah dan besarannya setara: legacy menjumlahkan baris bertanda,
tenant mengurangkan dokumen.

Penjualan tunai tidak punya padanan itu. Satu-satunya kandidat, `sales_trip_nota`, adalah **nota
penjualan** berkop nomor dan tanggal, sedangkan aksinya hanya menerima nominal. Menyisipkan nota
sintetis akan menambah **jumlah nota** yang dilaporkan layar SPJ dan rekonsiliasi — angka yang
tidak bertambah pada jalur legacy, sebab di sana yang bertambah hanya baris buku kas. Ditolak.

### `keterangan` setoran tidak punya kolom — dan itu dikatakan, bukan disembunyikan

`sales_trip_setoran` menyediakan `nomor_bukti` tetapi tidak `keterangan`. Dua pilihan yang
sama-sama buruk: menolak setoran (uang tidak tercatat gara-gara catatan opsional) atau membuang
isiannya diam-diam.

Yang dipilih ketiga: setorannya dicatat, dan bila `keterangan` memang diisi, faktanya
dikembalikan pada medan `peringatan`. Medan itu aditif — klien lama mengabaikannya, sama seperti
pola `aktorInventorySales`. Uangnya tercatat benar dan kehilangannya terlihat. Dicatat sebagai
celah C-13; kolomnya sekali tambah bila diputuskan perlu.

### Penjaga masuk RECONCILING: satu `LEFT JOIN` yang menentukan

Invarian legacy: `dimuat = terjual + kembali + rusak + hilang`, dan sisanya harus nol. Legacy
membacanya dari satu baris `spj_sales_barang` yang memuat rencana sekaligus hasil.

Model tenant memisahkannya: `sales_trip_barang` (yang dibawa) dan `sales_trip_hasil` (yang
terjadi). Pemeriksaannya karena itu menjadi join — dan **join-nya wajib `LEFT`**. Produk yang
dibawa tetapi belum punya baris hasil sama sekali adalah justru kasus terpenting: nol dari sepuluh
teralokasi. `INNER JOIN` akan melewatkannya, sehingga trip bisa masuk rekonsiliasi dengan seluruh
barangnya belum dipertanggungjawabkan.

Blok 3 dan 4 ujinya adalah pasangan: blok 3 membuktikan `LEFT JOIN` menangkap kedua produk, blok
4 membuktikan `INNER JOIN` menangkap **nol** — jadi perbedaannya nyata, bukan teoretis.

`qty_hilang` legacy dipetakan ke `selisih` tenant, meneruskan pemetaan yang sudah dipakai
`tripBarangUpdate`. Blok 5 membuktikannya: Kopi 5 dibawa, 2 terjual, 3 selisih — alokasi baru
habis bila selisih ikut dihitung.

### `tanggal_kembali` diisi sekali, tidak tertimpa

`ubahStatusTrip` memakai `COALESCE(?, tanggal_kembali)`: transisi ke RETURNED mengisinya, transisi
berikutnya mengirim NULL dan kolomnya dibiarkan. Tanpa itu, masuk RECONCILING akan menghapus
tanggal kembali yang baru saja dicatat.

Jalur legacy juga menyetel `spj.tanggalKembaliAktual`. Kolom itu **tidak ada** pada
`surat_perintah_sales` tenant — tanggal kembali sesungguhnya melekat pada tripnya, dan satu SPJ
hanya punya satu trip, sehingga angkanya tetap dapat ditelusuri. Perbedaan tempat penyimpanan,
bukan informasi yang hilang.

Status SPJ ikut dimajukan dalam transaksi yang sama: trip yang mengaku sudah kembali sementara
SPJ-nya masih berjalan adalah dua catatan yang saling membantah.

### Penjaga `tripCashSale` dipindah supaya pesannya jujur

Penjaga lama berdiri di baris pertama `tripCashSale` dan memberi pesan generik "belum tersedia".
Karena `catatKasSederhana` kini punya cabang tenant yang menolak penjualan tunai dengan alasan
sebenarnya, penjaga lama itu membuat cabang tersebut **tidak terjangkau**. Dilepas: sekarang hak
akses diperiksa dulu, lalu penolakannya menyebut sebabnya.

### Uji kesetaraan: `uji-kesetaraan-trip-setoran-status.sql`

Enam blok, seluruhnya LULUS pada klaster v1–v11:

| blok | yang dibuktikan |
|---|---|
| 1 | saldo kas sesudah setoran setara legacy (600.000 dari 1.000.000 tunai − 400.000 setoran) |
| 2 | **penjaga** — tanpa mengurangkan setoran angkanya 1.000.000, jadi contohnya membedakan |
| 3 | kedua produk tertangkap belum teralokasi sebelum hasilnya diisi |
| 4 | **penjaga** — `INNER JOIN` menangkap nol, jadi `LEFT JOIN` memang menentukan |
| 5 | alokasi habis di kedua jalur; `qty_hilang` legacy = `selisih` tenant |
| 6 | status maju, `tanggal_kembali` bertahan, status SPJ ikut |

SQL yang **benar-benar dikeluarkan Java** juga dijalankan berurutan ke basis data: baca status
trip, periksa barang belum habis, sisip setoran, hitung saldo kas, ubah status trip dan SPJ, isi
hasil, periksa ulang, lalu transisi kedua — dan `tanggal_kembali` terbukti bertahan.

## Audit penjaga tenant: satu lubang gagal-terbuka dari 75 aksi

Batch ini tidak memindahkan aksi. Ia memeriksa apakah ada aksi yang **terlewat sama sekali** —
dan menemukan satu.

### `si_import_legacy` berjalan tanpa penjaga apa pun

`SalesInventoryDbfImportHelper.importLegacy` dirutekan dispatcher pada baris 130, dan tidak punya
penjaga maupun cabang tenant di mana pun. Seluruh penyimpanannya memakai entitas Hibernate —
`Produk`, `AnggotaKoperasi`, `Penyedia`, `SalesInventory`, `HargaJualCustomer`,
`HargaBeliSupplier`, `StokOpname`, `Pembelian`, `PengadaanProduk`, `SatuanProduk`, dan ketiga
profilnya — dan entitas menyematkan schema-nya pada anotasi.

Dijalankan oleh pemilik usaha pada tenant berschema, akibatnya dua-duanya buruk sekaligus:

| | akibat |
|---|---|
| ke luar | master miliknya mendarat di schema **bersama**, terlihat instalasi lain |
| ke dalam | schema tenantnya sendiri tetap **kosong** |

Yang kedua itu yang paling menipu: impornya melapor sukses, jumlah barisnya benar, lalu seluruh
layar tenant tidak menampilkan apa-apa. Tidak ada galat yang bisa ditelusuri.

Ditutup dengan gagal-tertutup. Memindahkannya menuntut jalur tulis SQL tersendiri untuk ketiga
belas jenis itu berikut pemetaan ulang ke model tenant — stok sebagai turunan mutasi, pembelian
ber-header/detail, harga umum tanpa customer. Itu pekerjaan P5 utuh, bukan tambalan.

### Mengapa lubang semacam ini tidak pernah terlihat sendiri

Aksi yang lupa diberi penjaga **tidak gagal dan tidak berisik**. Kompilator tidak melihatnya, uji
fungsional satu-tenant tidak melihatnya, dan responsnya `status=00`. Satu-satunya cara menemukannya
adalah memeriksa daftar aksi terhadap daftar penjaga — dan itu pemeriksaan yang harus diulang tiap
kali aksi baru ditambahkan.

Karena itu auditnya dijadikan artefak: `audit-penjaga-tenant.py`, di folder yang sama dengan
berkas uji kesetaraan. Ia membaca dispatcher, menelusuri tiap aksi ke metode helpernya, dan keluar
dengan kode 1 bila ada yang tanpa penjaga.

Bahwa ia benar-benar menggigit sudah dibuktikan, bukan diasumsikan: penjaga yang baru dipasang
dilepas sementara, audit melaporkan lubangnya dan keluar 1; penjaga dipulihkan, audit lulus dan
keluar 0.

### Versi pertama audit ini salah, dan itu dicatat di dalamnya

Versi pertama tidak menelusuri delegasi sama sekali dan melaporkan **tujuh positif palsu** —
`tripDeposit`, `tripReturn`, `tripCashSale`, `tripReconcile`, kedua aksi status giro, dan
`si_actor_context` — sebab penjaganya berada di metode privat yang dipanggil, bukan di badan
aksinya sendiri.

Batas itu ditulis di kepala berkasnya, berikut dua batas lain yang masih ada: pemeriksaannya
tekstual belaka, dan ia hanya memastikan penjaga **ada**, bukan bahwa letaknya benar. Penjaga yang
berdiri sebelum pemeriksaan hak akses — persis yang harus dipindahkan pada dua batch sebelumnya —
tetap lolos di sini.

`si_actor_context` dikecualikan dengan sengaja: ia hanya mengembalikan konteks aktor pemanggil dan
tidak menyentuh schema mana pun.

### Sisa P4: tidak ada lagi aksi yang bebas hambatan

Penyisiran seluruh helper menunjukkan setiap aksi yang masih tertutup menunggu sesuatu yang nyata,
bukan menunggu ditulis:

| penghalang | aksi yang menunggu |
|---|---|
| buku kas trip (C-11) | `tripCashSale`, `tripDetail`, `tripClose`, `expenseCreate`, `collectionCreate`, `collectionReverse`, `expenseReverse` |
| nota bawaan bernilai tertagih | `spjNotaAssign`, `tripNotaResult` |
| master kategori biaya | `expenseCategoryList`, `expenseCategorySave` |
| tabel pembelian dalam trip | `tripPurchaseLink` |
| `payable_faktur_info` | `purchaseTermsSave` |
| status giro | `payableBgStatus`, `collectionBgStatus` |
| Envers ber-schema statis | `auditHistory` |
| §16 lingkup toko&rarr;gudang | saringan toko pada `priceAnalysis` dan `inventoryBalance` |
| P5 | `importLegacy` |

Empat penghalang teratas bermuara pada keputusan katalog; sisanya pada pekerjaan yang memang belum
dijadwalkan. Tidak ada satu pun yang tinggal ditulis.
## Memakai buku kas v12: saldo yang benar, penjualan tunai, dan pembalikan biaya

Bundel v12 hanya menyediakan tempatnya. Batch ini yang mengisinya — dan yang paling penting,
**memindahkan sumber kebenaran saldo kas** dari tambalan lama ke bukunya.

### `saldoKas` kini dibaca dari bukunya

Rumus lama, `Σ nota.tunai − Σ setoran`, diganti penjumlahan bertanda `sales_trip_kas`. Ini
menuntaskan koreksi yang dua kali sebelumnya hanya separuh: r83075 memperbaiki cacat yang lebih
besar (waktu itu rumusnya menjumlahkan setoran saja), tetapi tetap mengabaikan uang muka
operasional, penagihan tunai piutang lama, dan biaya tunai.

Bentuk barunya satu baris, tanpa `CASE` dan tanpa daftar jenis yang harus diingat pembaca. Itu
disengaja: setiap `CASE` yang ditambahkan di sana adalah kesempatan baru untuk melupakan satu
jenis, dan justru kelupaan semacam itu yang sudah dua kali menghasilkan angka uang salah.

### Setiap aksi kas kini membukukan barisnya

| aksi | baris yang dibukukan |
|---|---|
| `tripStart` | `OPENING_ADVANCE` sebesar uang muka operasional SPJ |
| `tripCashSale` | `CASH_SALE` positif |
| `tripDeposit` | `OWNER_DEPOSIT` negatif, **plus** dokumen `sales_trip_setoran` |
| `expenseReverse` | `REVERSAL` positif bila biayanya tunai — kas kembali |

`uang_muka_operasional` juga mulai ditulis dan dibaca `spjSimpan`; sebelum v12 kolomnya tidak ada,
sehingga trip yang dimulai berangkat dari kas nol padahal sales membawa uang.

### Risiko baru batch ini: setoran tercatat di dua tempat

Setoran kini menulis **dua** baris — dokumen `sales_trip_setoran` yang memegang nomor buktinya, dan
baris buku kas `OWNER_DEPOSIT`. Kalau saldo kas ikut membaca dokumennya, setorannya terhitung dua
kali.

Aturannya tegas: **hanya bukunya yang dihitung.** Dokumen setoran menyimpan nomor bukti dan status,
tidak ikut aritmetika. Blok 6 dan 7 uji kesetaraannya sepasang: blok 6 membuktikan saldonya 500.000
dengan satu dokumen dan satu baris kas; blok 7 membuktikan rumus yang keliru — yang ikut
mengurangkan dokumennya — menghasilkan 100.000, sehingga penggandaan itu memang terdeteksi contoh
ini, bukan hanya mungkin secara teori.

### `tripCashSale` tidak lagi ditolak, dan alasan penolakannya gugur dengan benar

Penolakan sebelumnya bukan karena malas: menyisipkan nota sintetis akan menambah **jumlah nota**
yang tidak bertambah pada jalur legacy. Alasan itu gugur karena v12 memberi rumah yang tepat —
baris buku kas, bukan nota. Jumlah nota tidak tersentuh.

### `expenseReverse` dipindahkan

Bentuknya sama dengan jalur legacy: dokumen cermin bernilai negatif berstatus REVERSAL, menunjuk
asalnya lewat `pembalik_dari_id`, asalnya ditandai DIBATALKAN, dan bila biayanya tunai satu baris
`REVERSAL` positif mengembalikan kasnya. Sesi yang sudah ditutup tetap ditolak — snapshot penutupan
tidak boleh berubah diam-diam.

Dua medan legacy tidak punya kolom: `penerima` memang tidak ada pada `sales_trip_biaya`, sedangkan
`alasanReversal` ditampung `reversal_log` — yang justru menyimpan lebih banyak daripada legacy,
sebab pelaku dan waktunya ikut tercatat.

Kuerinya sengaja diletakkan pada `SalesInventoryTripTenant`, bukan disalin ke
`SalesInventoryReversalTenant`: biaya trip milik ranah trip, dan dua definisi untuk satu tabel
adalah awal dari dua definisi yang berselisih.

### Verifikasi

Delapan blok `uji-kesetaraan-kas-trip.sql` LULUS, termasuk tiga penjaga:

| blok | yang dibuktikan |
|---|---|
| 2 | rumus lama menghasilkan **−100.000** di tempat yang benar 500.000 — beda tanda |
| 7 | rumus yang menggandakan setoran menghasilkan 100.000, jadi penggandaan terdeteksi |
| 4 | kunci idempotensi kembar pada buku kas ditolak |

Seluruh SQL kas yang **benar-benar dikeluarkan Java** juga dijalankan berurutan ke basis data: baca
SPJ (lima kolom, uang muka di kolom kelima), bukukan baris pembuka, penjualan tunai, penagihan
tunai, setoran berikut dokumennya, biaya tunai, lalu pembalikan biaya lengkap dengan kas
kembalinya. Hasil akhirnya saldo **600.000**, uang muka awal **500.000** diturunkan dari buku, dan
setoran tercatat satu dokumen + satu baris kas — tidak tergandakan.

### Sisa Trip: 12 dari 19 aksi

Naik dari 10. Yang masih tertutup dan penghalangnya:

| penghalang | aksi |
|---|---|
| tabel nota bawaan bernilai tertagih | `spjNotaAssign`, `tripNotaResult`, `tripClose` |
| master kategori biaya | `expenseCategoryList`, `expenseCategorySave`, `expenseCreate` |
| tabel pembelian dalam trip | `tripPurchaseLink`, sisi pembelian `tripDetail` |

`collectionReverse` di helper Reversal kini tinggal menunggu **satu** hal saja — nota bawaan —
sebab sisi kasnya sudah dibuka v12.

## Memakai nota bawaan v13: `spjNotaAssign` dan `tripNotaResult`

Trip naik ke **13 dari 19 aksi**. Dua aksi yang murni tentang tabel baru ini dipindahkan; tiga
aksi uang yang juga memakainya (`tripClose`, `collectionCreate`, `collectionReverse`) menyusul.

### `spjNotaAssign`: penggantian menyeluruh, dan potret yang harus benar

Bentuknya sama dengan legacy — daftar nota ditulis ulang utuh, dan hanya boleh selama SPJ masih
DRAFT/SUBMITTED/APPROVED. Sesudah berangkat, daftar bawaannya beku.

Satu hal yang layak disebut: `saldo_saat_assign` dihitung dari alokasi, **bukan** dibaca dari kolom
ringkasan `piutang_customer.sisa`. Kolom ringkasan itu bisa basi, dan potret yang salah akan terus
salah selamanya sebab tidak pernah dihitung ulang. Justru karena angkanya dibekukan, angka yang
dibekukan harus benar.

### `tripNotaResult`: mencatat hasil kunjungan, bukan uang

Status, hasil kunjungan, janji bayar, dan alasan gagal. Nota yang sudah direkonsiliasi tetap
ditolak — hasil kunjungan yang berubah sesudah penutupan membuat rekap yang sudah disetujui tidak
lagi cocok dengan rinciannya.

Aksi ini tidak menyentuh nilai tertagih, sama seperti legacy. Bedanya, di sini angka itu memang
tidak ada untuk disentuh.

### Verifikasi

SQL yang **benar-benar dikeluarkan Java** dijalankan berurutan ke basis data v1–v13: baca SPJ →
hitung sisa piutang → hapus daftar lama → sisipkan nota → baca untuk hasil kunjungan → simpan
hasilnya → baca nilai tertagih turunan.

Hasilnya: sisa piutang **800.000** (1.000.000 dikurangi 200.000 yang sudah dibayar), nota tersimpan
berstatus ASSIGNED, hasil kunjungan tersimpan sebagai PROMISE_TO_PAY dengan janji bayar
2026-07-20, `saldo_saat_assign` bertahan 800.000, dan **nilai tertagih turunan 0** — benar, sebab
penagihan 300.000 di skenario itu sudah dibalik. Tidak ada kolom yang perlu diingat untuk
diturunkan.

### Sisa Trip: 6 dari 19 aksi masih tertutup

| penghalang | aksi |
|---|---|
| master kategori biaya | `expenseCategoryList`, `expenseCategorySave`, `expenseCreate` |
| tabel pembelian dalam trip | `tripPurchaseLink`, dan `tripDetail` yang ikut membaca pembelian trip |
| (v13 tersedia, tinggal ditulis) | `tripClose` |

Enam aksi, bukan lima: `tripDetail` tertutup **utuh**, bukan sebagian, sebab ia merangkum biaya,
kas, dan pembelian sekaligus — dan pembeliannya belum punya tabel.

Dan pada helper Piutang, `collectionCreate` serta `collectionReverse` kini **tidak lagi terhalang
apa pun** — keduanya tinggal ditulis.
## Penagihan piutang dan pembalikannya: Piutang TUNTAS 12 dari 12

`collectionCreate` dan `collectionReverse` adalah dua aksi yang paling lama tertahan pada P4 —
keduanya menunggu **dua bundel sekaligus**. Sisi kasnya menunggu v12: penagihan tunai di lapangan
menaikkan uang yang dipegang sales, dan tanpa buku kas tidak ada tempat mencatatnya. Sisi notanya
menunggu v13: penagihan memutakhirkan status nota bawaan yang ditugaskan pada SPJ.

Memindahkan salah satu saja berarti mencatat uang masuk yang tidak terlihat di kas, atau nota yang
tidak pernah berubah status. Itu sebabnya keduanya ditolak utuh selama ini, bukan dipindahkan
separuh.

**Helper Piutang kini tuntas: 12 dari 12 aksi.** Tidak ada lagi aksi Piutang yang gagal-tertutup.
Helper Reversal naik ke 5 dari 7 — sisanya hanya kedua aksi status giro.

### Satu langkah legacy yang HILANG, dan itu perbaikan

Jalur legacy menaikkan `SpjSalesNota.nilaiTertagih` saat penagihan dicatat, lalu menurunkannya saat
dibalik — dan harus menjepitnya ke nol supaya tidak menjadi negatif. Dua penulis untuk satu angka,
dengan penjepit yang menyembunyikan kelupaan.

Model tenant tidak menyimpan angka itu; ia diturunkan dari alokasi (bundel v13). Alokasi pembalik
memang bernilai negatif, jadi **jumlahnya turun sendiri**. Kedua jalur tenant karena itu tidak
menulis apa pun ke sana — yang tersisa hanyalah memutakhirkan **statusnya**, dan status memang
bukan turunan: PAID/PARTIAL ditentukan sisa tagihan, tetapi PROMISE_TO_PAY atau DISPUTED datang
dari kunjungan.

### Kosakata status piutang, sekali lagi

Legacy mengunci faktur dengan syarat `status = 'AKTIF'`; katalog tenant memakai `'TERBUKA'`.
Alih-alih memilih salah satu, penjaganya dibalik menjadi **bukan** dokumen yang dibatalkan
(`NOT IN ('BATAL','DIBATALKAN')`) — itu yang sebenarnya dimaksud, dan tahan terhadap kosakata mana
pun yang dipakai baris hasil impor.

`FOR UPDATE` disalin apa adanya dari legacy dan bukan hiasan: tanpa kunci baris, dua penagihan
bersamaan atas faktur yang sama sama-sama membaca sisa yang masih penuh, lalu sama-sama lolos — dan
faktur tertagih melebihi nilainya.

### Verifikasi

Seluruh SQL yang **benar-benar dikeluarkan Java** dijalankan berurutan ke basis data v1–v13, untuk
kedua alur. Hasilnya:

| | sesudah penagihan | sesudah pembalikan |
|---|---|---|
| sisa piutang | 600.000 | 1.000.000 |
| nilai tertagih (turunan) | 400.000 | **0** |
| saldo kas trip | 400.000 | 0 |
| status dokumen asal | AKTIF | DIBATALKAN |
| jejak `reversal_log` | — | 1 |

Angka tertagih kembali ke nol **tanpa satu pun pengurang ditulis**.

Jalan SQL itu meninggalkan satu jalur tidak teruji — `UPDATE 0` pada pemunduran order, sebab di
skenario itu ordernya memang belum lunas. `uji-kesetaraan-penagihan.sql` menutupnya dengan skenario
pelunasan penuh:

| blok | yang dibuktikan |
|---|---|
| 1 | sisa nol, kas naik, nota PAID |
| 2 | pelunasan penuh memajukan order ke LUNAS |
| 3 | pembalikan memulihkan sisa, kas, dan status asal |
| 4 | order **mundur** dari LUNAS ke SIAP_TAGIH — jalur yang tadi terlewat |
| 5 | **inti** — turunan memperbaiki diri sendiri; kolom legacy yang terlupa tetap 1.000.000 |
| 6 | **penjaga** — turunannya terikat TRIP: pembayaran kantor tidak diakui hasil lapangan |

Blok 6 penting: tanpa itu, rumus turunan bisa saja benar karena kebetulan menghitung *semua*
pembayaran. Pembayaran kantor 250.000 menurunkan sisa piutang menjadi 750.000 tetapi tertagih
lapangan tetap 0 — terikat trip, sebagaimana seharusnya.

### Sisa P4

| helper | keadaan |
|---|---|
| Piutang & Sales Order | **12 dari 12 — tuntas** |
| Reversal & Log Cetak | 5 dari 7 — sisa kedua aksi status giro |
| Trip | 13 dari 19 |

Penghalang katalog yang tersisa tinggal tiga, semuanya kecil: master kategori biaya (3 aksi Trip),
tabel pembelian dalam trip (`tripPurchaseLink` dan `tripDetail`), serta kolom status giro (2 aksi
Reversal). `tripClose` tidak terhalang apa pun lagi — tinggal ditulis.
## `tripClose`: penutupan sesi — Trip 14 dari 19

Seluruh angka penutupan dibaca dari sumbernya masing-masing pada saat penutupan, lalu dibekukan
menjadi satu baris `sales_trip_rekonsiliasi`. Buku kas tetap sumber kebenaran "kas seharusnya",
sama seperti jalur legacy membaca `nota_sales_kas`.

### Dua angka legacy yang tidak dibaca dari kolom

**Pemilahan penerimaan tunai/non-tunai diturunkan** dari `penerimaan_piutang` yang menunjuk trip
ini, dipilah `cara_bayar`. Legacy menyimpannya sebagai dua kolom; di sini tidak perlu.

**Pembayaran pembelian bernilai nol menurut definisi.** Model tenant belum punya pembelian dalam
trip, sehingga tidak ada yang bisa terlewat — angkanya nol karena memang tidak ada, bukan karena
hilang. Itu perbedaan penting: bila kelak tabelnya ditambahkan, angka ini harus mulai dibaca, dan
catatan ini yang mengingatkannya.

### `ON CONFLICT` alih-alih gagal

`sales_trip_rekonsiliasi` dijaga `UNIQUE (sales_trip_id)` — satu trip satu rekonsiliasi.
Penyimpanannya memakai `ON CONFLICT ... DO UPDATE` sehingga penutupan yang diulang memperbarui
barisnya alih-alih gagal. Karena penutupan hanya sah dari status RECONCILING, pengulangan itu
hanya mungkin bila transaksinya sempat gagal di tengah — dan justru di situlah gagal keras akan
menyulitkan.

### Barang dibawa tidak ditandai satu per satu

Legacy menyetel status RECONCILED pada tiap baris barang. Model tenant tidak: kefinalannya sudah
dinyatakan status tripnya yang menjadi CLOSED. Penanda kedua hanya akan berselisih dengan
induknya.

### Verifikasi

SQL yang **benar-benar dikeluarkan Java** dijalankan berurutan ke basis data v1–v14, atas
skenario: panjar 500.000, jual tunai 300.000, tagih tunai 200.000, biaya 100.000, setoran 400.000,
barang dibawa 10 @15.000 dengan 3 kembali, kas fisik dihitung 480.000.

| angka | hasil |
|---|---|
| total biaya | 100.000 |
| nilai penjualan | 300.000 |
| tertagih / tunai | 200.000 / 200.000 |
| barang dibawa / kembali | 150.000 / 45.000 |
| kas fisik aktual | 480.000 |
| selisih | **−20.000** |
| status trip / SPJ / nota | CLOSED / CLOSED / RECONCILED |

`simpanRekonsiliasi` dijalankan **dua kali** berturut-turut; barisnya tetap satu — `ON CONFLICT`
memperbarui, tidak menggandakan.

### Sisa Trip: 5 dari 19

| penghalang | aksi |
|---|---|
| master kategori biaya | `expenseCategoryList`, `expenseCategorySave`, `expenseCreate` |
| tabel pembelian dalam trip | `tripPurchaseLink`, `tripDetail` |

## Tiga aksi biaya trip — Trip 17 dari 19

`expenseCategoryList`, `expenseCategorySave`, dan `expenseCreate` dipindahkan bersama v15. Sisa
Trip tinggal dua: `tripDetail` dan `tripPurchaseLink`, keduanya menunggu tabel pembelian dalam
trip.

### `aktif` dan `akunId` hanya disentuh bila disebut

Jalur legacy menyetel keduanya hanya bila kuncinya ada pada permintaan. Itu bukan detail: menyetel
tanpa syarat akan **menyalakan kembali kategori yang sengaja dimatikan**, dan **mengosongkan akun
beban** yang sudah disetel — akun beban yang hilang membuat mesin posting tidak tahu ke mana biaya
dibukukan.

`aktif` ditangani `COALESCE(?, aktif)`; `akunId` ditangani pernyataan terpisah yang dijalankan
hanya bila kuncinya ada. Menggabungkannya ke satu `UPDATE` akan menghilangkan perbedaan
"tidak disebut" dan "disebut kosong".

### Kolom teks kategori sengaja dikosongkan untuk baris baru

Penunjuk `kategori_biaya_id` yang berwenang. Menyalin kodenya ke kolom teks hanya melahirkan
salinan yang membeku saat kategori berganti nama — pola yang sudah berkali-kali ditolak sepanjang
pemindahan ini.

Jalan SQL membuktikannya: sesudah kategori BBM diganti nama menjadi "BBM (revisi)", biaya yang
menunjuknya **ikut berubah**. Kalau kodenya disalin, ia akan tetap menampilkan nama lama.

### Satu perbaikan menyusul: pembalikan biaya kehilangan kategorinya

`expenseReverse` dipindahkan pada batch v12, ketika kategori masih teks bebas. Setelah v15
memindahkan kewenangan ke penunjuk, baris pembalik yang hanya menyalin kolom teks akan lahir
**tanpa kategori** — dan akun bebannya tidak lagi dapat ditelusuri.

`biayaUntukBalik` kini ikut membaca `kategori_biaya_id`, dan `sisipBiayaPembalik` menulisnya.
Ditemukan saat merancang v15, bukan setelah terkirim.

### Verifikasi

SQL yang benar-benar dikeluarkan Java dijalankan berurutan ke basis data v1–v15:

| langkah | hasil |
|---|---|
| daftar kategori aktif | 9 bawaan |
| ubah nama dengan `aktif` NULL | nama berubah, aktif bertahan |
| sisip kategori baru | tersimpan |
| catat biaya 100.000 tunai | tertaut kategori; penerima dan nomor bukti tersimpan |
| buku kas | **−100.000** |
| **penjaga** — kategori diganti nama | biaya ikut berubah, bukan beku |

## `tripPurchaseLink` — Trip 18 dari 19

Pembelian dalam perjalanan kini dapat dicatat. Bagian yang dibayar dari kas yang dipegang sales
ikut membukukan satu baris `PURCHASE_PAYMENT` bertanda negatif — itu sebabnya aksi ini menunggu
**dua** bundel: v12 untuk buku kasnya dan v16 untuk dokumen pembeliannya.

Kaitan faktur pengadaan dan pemasok divalidasi bila disebut, dan dibiarkan kosong bila tidak —
mengikuti legacy.

### Verifikasi

SQL yang benar-benar dikeluarkan Java dijalankan berurutan ke basis data v1–v16: faktur 500.000
dengan 200.000 dibayar dari kas.

| | hasil |
|---|---|
| daftar pembelian | supplier terisi, total 500.000, dibayar 200.000, **sisa 300.000 (turunan)** |
| saldo kas trip | **−200.000** |
| **penjaga** — `dibayar_trip` dinaikkan ke 500.000 | sisa hutang turun sendiri ke **0** |

Penjaga itu semula **lulus tanpa membuktikan apa pun**: ia menargetkan `id = 1` sementara id
serialnya sudah bergeser, sehingga `UPDATE` mengenai nol baris dan angkanya tampak konsisten
karena memang tidak berubah. Sekarang ia menargetkan barisnya lewat kunci idempotensi, dan
`UPDATE 1` membuktikan ia benar-benar menyentuh datanya.

### Sisa Trip: satu aksi

`tripDetail` sengaja tidak dikerjakan pada batch ini. Ia bukan lagi terhalang — seluruh bagiannya
kini ada — tetapi ia aksi baca terbesar pada helper ini: header, blok SPJ berikut barangnya, tiga
larik (biaya, pembelian, kas), dan lima belas medan rumus. Menggabungkannya ke batch ini berarti
banyak kode dengan verifikasi tipis, dan pencocokan jumlah kolom adalah justru tempat kesalahan
paling sering terjadi pada pemindahan ini.

## `tripDetail`: Trip TUNTAS 19 dari 19

Aksi terakhir helper Trip, dan yang paling banyak menghimpun: satu layar yang membaca hasil
**enam bundel sekaligus** — buku kas (v12), nota bawaan (v13), kas fisik penutupan (v14),
kategori biaya (v15), dan pembelian trip (v16) — di samping tabel yang sudah ada sejak awal.
Itu sebabnya ia dikerjakan terakhir.

Keluarannya: header sembilan medan, blok SPJ empat belas medan berikut barangnya, tiga larik
(biaya, pembelian, buku kas), dan lima belas medan rumus.

### Rumus dihitung satu lintasan, bukan agregat terpisah

Jalur legacy melintasi baris buku kas sekali dan memilah per jenis. Jalur tenant melakukan hal
yang sama, dan itu disengaja: menghitungnya lewat beberapa SQL agregat terpisah menggoda untuk
melupakan satu jenis — dan kelupaan persis itu sudah **dua kali** menghasilkan angka uang salah
pada pemindahan ini.

### Satu `LEFT JOIN` yang menentukan, lagi

Legacy menyimpan rencana dan hasil pada satu baris; model tenant memecahnya menjadi tiga tabel:
rencana di `surat_perintah_sales_detail`, yang dibawa di `sales_trip_barang`, hasilnya di
`sales_trip_hasil`. Blok barang menyatukan ketiganya kembali.

Basisnya **rencana**, dengan `LEFT JOIN` ke dua sisanya. Barang yang direncanakan tetapi tidak
jadi dimuat harus tetap muncul dengan qtyDimuat nol — kalau ia menghilang, layar rincian akan
menyatakan sales membawa sesuatu yang tidak pernah direncanakan hilang. Blok 4 dan 5 ujinya
sepasang: `LEFT JOIN` menemukan dua baris (satu belum dimuat), `INNER JOIN` hanya satu.

### Dua medan dikembalikan kosong, dan itu dicatat

`rute` tidak ada pada model tenant — `wilayah` bukan padanannya, sebab rute adalah urutan
kunjungan sedangkan wilayah adalah cakupan. `disetujuiOleh` juga tidak ada: SPJ tenant mencatat
statusnya berubah menjadi APPROVED tetapi tidak menyimpan siapa yang menyetujui.

Keduanya dikembalikan kosong, bukan diisi tebakan.

### Dua titik usang pada `spjDetail` ikut diperbaiki

`detailSpjTenant` dipindahkan jauh sebelum v12 dan v13 ada, dan sejak itu menyimpan dua
pernyataan yang **berhenti benar** ketika bundel-bundel itu mendarat:

| medan | dahulu | sekarang |
|---|---|---|
| `uangMukaOperasional` | selalu `null` | dibaca dari kolomnya (v12) |
| `nota` | selalu larik kosong | diisi dari `surat_perintah_sales_nota` (v13) |

Komentar lamanya bahkan menyatakan "konsep penugasan piutang ke SPJ tidak ada pada model tenant"
— pernyataan yang benar saat ditulis dan salah sejak v13. Ini jenis kebusukan yang tidak pernah
gagal, hanya diam-diam berhenti melaporkan data yang sudah ada.

### Verifikasi

Seluruh kueri yang **benar-benar dikeluarkan Java** dijalankan ke basis data v1–v16 atas skenario
lengkap. Jumlah kolomnya dihitung langsung dari keluaran: 10, 14, 12, 8, 7, 6, 8 — sesuai
rancangan.

| angka | hasil |
|---|---|
| saldo kas | **300.000** (500+300+200−100−200−400) |
| uang muka awal (turunan) | 500.000 |
| nilai tertagih nota (turunan) | 200.000 |
| sisa hutang pembelian (turunan) | 300.000 |
| barang | Gula dimuat 10, **Kopi muncul dengan dimuat 0** |

`uji-kesetaraan-rincian-trip.sql` — enam blok LULUS, dua di antaranya penjaga: rumus tanpa panjar
menghasilkan −200.000 (jadi contohnya membedakan), dan `INNER JOIN` menghilangkan barang yang
belum dimuat.

### Helper Trip tuntas

Sembilan belas dari sembilan belas aksi berjalan pada schema tenant. Tidak ada lagi aksi Trip yang
gagal-tertutup.

## Siklus status giro — dan `statusBg` yang berhenti `NULL`

`payableBgStatus` dan `collectionBgStatus` dipindahkan bersama v17. Keduanya melalui satu metode
`bgStatus`, sehingga satu cabang tenant melayani dua aksi.

### Pembalikan dipanggil, bukan disalin

Giro yang ditolak menerbitkan dokumen pembalik. Jalur tenant **memanggil aksi publiknya sendiri** —
`payablePaymentReverse` atau `collectionReverse` — yang keduanya sudah mengenali jalur tenant dan
sudah idempoten lewat kunci `REV-PHS-<id>` / `REV-KWT-<id>`.

Menyalin isinya akan melahirkan jalan kedua yang bisa berselisih dengan yang pertama; memanggilnya
kembali membuat keduanya mustahil berbeda.

Pembalikannya dijalankan **sesudah** status giro tersimpan, sama seperti legacy. Bila pembalikannya
gagal, statusnya sudah tercatat dan pembalikannya dapat diulang tanpa menggandakan —
kebalikannya akan menyembunyikan penolakan gironya sama sekali.

### Dua daftar berhenti mengembalikan `NULL`

`selectRiwayat` (Payable) dan `selectPenagihan` (Piutang) sama-sama mengembalikan `NULL` pada kolom
`statusBg`, dengan javadoc yang menyatakan model tenant tidak menyimpannya. Benar saat ditulis,
salah sejak v17.

Keduanya kini membaca kolomnya. Ini pola ketiga kalinya pada pemindahan ini: pernyataan yang benar
saat ditulis lalu berhenti benar ketika sebuah bundel mendarat — tidak pernah gagal, hanya
diam-diam berhenti melaporkan data yang sudah ada.

### Verifikasi

Seluruh kueri yang benar-benar dikeluarkan Java dijalankan ke basis data v1–v17. `giroUntukStatus`
membaca `NULL` (DITERIMA), `ubahStatusGiro` memindahkannya ke CAIR, pembacaan berikutnya
mengembalikan CAIR — final. Sisi piutang berpindah ke TOLAK. Kedua daftar kini menampilkan
statusnya: `CAIR` pada riwayat pembayaran, `TOLAK` pada riwayat penagihan.

`uji-kesetaraan-status-giro.sql` — enam blok LULUS, satu di antaranya penjaga: sebelum pembalikan
sisa piutang masih 300.000, sesudahnya pulih ke 600.000. Tanpa blok penjaga itu, blok berikutnya
bisa lulus hanya karena sisanya kebetulan sudah penuh sejak awal.

Blok terakhir memeriksa hal yang mudah terlewat: dokumen asal menjadi DIBATALKAN **tetapi**
`status_bg` tetap TOLAK. Sebab pembatalannya harus tetap terbaca; dokumen yang dibatalkan tanpa
keterangan mengapa adalah catatan yang setengah.

## `purchaseTermsSave` — helper Payable TUNTAS 7 dari 7

Pada jalur tenant `faktur_id` berarti id `hutang_supplier`, sama seperti pada pencatatan
pembayaran yang juga mengunci hutangnya lewat id itu.

### `dibayar_awal` ditolak, dan itu bukan kekurangan

Menerimanya berarti memperkenalkan pengurang kedua atas sisa hutang yang tidak berasal dari
dokumen mana pun. Pada model tenant uang muka adalah pembayaran, dan penolakannya menunjuk ke aksi
pembayaran hutang.

Untuk jenis CASH, jalur legacy diam-diam menyetel uang muka sebesar total fakturnya — artinya
faktur dianggap lunas seketika. Di sini pelunasannya juga harus berupa dokumen, sehingga terminnya
disimpan dan pemanggil diberi tahu lewat medan aditif `peringatan` bahwa pembayarannya masih perlu
dicatat. Jujur, dan tidak memindahkan uang diam-diam.

### Verifikasi

SQL yang benar-benar dikeluarkan Java dijalankan ke basis data v1–v18: hutang bertanggal
2026-12-01 senilai 1.000.000, disimpan CREDIT dengan termin 30 hari.

| | hasil |
|---|---|
| jenis / termin | CREDIT / 30 |
| jatuh tempo | **2026-12-31** (tanggal + termin) |
| sisa hutang | **1.000.000 — tidak tersentuh** |

Baris terakhir itu yang paling penting: menyimpan termin tidak menyentuh rumus sisa, yang tetap
bersumber tunggal pada alokasi.

### Catatan: pohon kerja sedang tidak dapat dikompilasi utuh

Kompilasi seluruh pohon saat ini gagal dengan empat galat pada `OnlineBmtUtil.java` dan
`KantinHelper.java` — keduanya bukan berkas pemindahan ini, dan keduanya tidak tersunting pada
working copy, yang berarti keadaan itu datang dari pekerjaan sesi lain yang sedang berjalan.

Bahwa galat itu bukan milik perubahan ini dibuktikan dua kali: `SalesInventoryTripHelper`, yang
dikompilasi bersih pada batch sebelumnya dan tidak disentuh batch ini, kini menghasilkan **empat
galat yang sama persis**; dan sebuah galat sengaja yang disisipkan sementara ke
`SalesInventoryPayableTenant` **dilaporkan javac**, membuktikan berkas batch ini memang ikut
diperiksa dan bersih. Berkas itu dipulihkan utuh sesudahnya.

## §16 — lingkup toko pada model tenant adalah lingkup GUDANG

`priceAnalysis` dan `inventoryBalance` sama-sama menolak saringan toko sejak awal pemindahan.
Keduanya kini menegakkannya, dan cara menegakkannya adalah keputusan rancangan yang layak
dijelaskan.

### Mengapa tidak bisa disalin apa adanya

Jalur legacy menyaring `produk.toko`: di sana produk **milik** satu toko. Model tenant tidak
begitu — produk berlaku se-tenant, dan yang menjadi milik satu toko adalah **gudangnya**
(`gudang.toko_id`). Tidak ada kolom yang bisa disalin.

### Dua pembatas, dan keduanya perlu

| pembatas | apa yang dijaga |
|---|---|
| daftar barisnya | produk yang punya baris `saldo_stok` pada gudang toko itu |
| angkanya | mutasi dihitung hanya dari gudang toko itu |

Membatasi daftarnya saja akan menampilkan produk toko ini **dengan stok se-tenant** — angka yang
lebih besar dari kenyataan di raknya. Blok 3 ujinya membuktikan itu: Gula yang hanya 30 di rak
Toko 1 tampil **530** kalau angkanya tidak ikut dibatasi.

Membatasi angkanya saja akan menampilkan seluruh produk tenant, kebanyakan bernilai nol.

### Mengapa `saldo_stok`, bukan `mutasi_stok`

Daftarnya dibatasi lewat `saldo_stok` justru supaya produk yang **bersaldo nol tetap muncul**.
Kalau dasarnya mutasi bersaldo positif, produk yang habis akan menghilang dari layar — padahal
layar persediaan justru dipakai untuk melihat apa yang habis. Blok 4 dan 5 ujinya sepasang: Gula
dikosongkan di Toko 1, tetap terlihat lewat `saldo_stok` (1 baris), sedangkan dasar yang keliru
menemukan **nol**.

### Satu perbedaan hasil, dan itu disengaja

Produk yang *ditugaskan* ke suatu toko tetapi belum pernah distok di sana **tidak muncul**,
sedangkan jalur legacy menampilkannya — sebab di sana penugasannya atribut produk, bukan akibat
adanya stok.

Model tenant tidak punya penugasan semacam itu. Satu-satunya pernyataan bahwa suatu toko menangani
suatu produk adalah adanya stok produk itu di gudangnya. Menambahkan kolom penugasan hanya demi
menyamai daftar legacy berarti menambah sumber kebenaran kedua tentang "toko ini menjual apa",
yang lalu bisa berselisih dengan stoknya sendiri.

### Saringan `stok_ada` / `stok_nol` ikut dibatasi

Pada `priceAnalysis`, kedua saringan itu membaca stok turunan. Keduanya kini memakai stok yang
sudah dibatasi gudang: menyaring "ada stok" atas angka se-tenant akan meloloskan produk yang
justru habis di toko yang sedang dilihat.

### `tokoId` disambung sebagai literal

Ia `Long` yang sudah tervalidasi pemanggil, dan ekspresinya muncul di dalam `SELECT`. Memakai `?`
di sana akan menyisipkan parameter **sebelum** parameter `where`, mengacaukan urutan pengikatan
yang sudah ada — pola yang sama sudah dipakai untuk literal tanggal tervalidasi.

### Verifikasi

SQL yang benar-benar dikeluarkan Java dijalankan atas dua toko: Gula distok di keduanya (30 di T1,
500 di T2), Kopi hanya di T2, Teh tidak di mana pun.

| kueri | hasil |
|---|---|
| tanpa lingkup | 3 produk; Gula 530 |
| lingkup Toko 1 | **1 produk** (Gula), masuk **30** |
| lingkup Toko 2 | 2 produk (Gula 500, Kopi 70) |
| stok turunan Harga | se-tenant 530 lawan Toko 1 **30** |

`uji-kesetaraan-lingkup-toko.sql` — lima blok LULUS, dua di antaranya penjaga.

## §17 — bagan akun tenant: yang tertutup bukan satu layar, melainkan seluruh pembukuan

`coaSave` menolak pembuatan akun sejak awal pemindahan, dengan alasan yang benar sejauh
alasannya berjalan: `akun.tipe` bersifat `NOT NULL` sedangkan permintaan legacy tidak pernah
membawanya, dan mengarangnya berarti menebak klasifikasi akun.

Yang tidak tercatat waktu itu adalah **akibatnya**. Tidak ada penyemai bagan akun di katalog
migrasi — v7 membuat tabelnya, tidak ada satu pun `INSERT` yang mengisinya — dan `coaSave`
adalah **satu-satunya penulis** `akun` pada schema tenant. Selama pembuatan ditolak, tabel itu
kosong selamanya. Dan `jurnal_detail.akun_id` bersifat `NOT NULL REFERENCES akun(id)`.

Artinya: pada tenant berschema, **satu baris jurnal pun tidak akan pernah bisa ditulis**. Yang
tertutup bukan satu layar master, melainkan seluruh pembukuan tenant.

### Kelas akun: diminta, atau diwarisi — tidak ditebak

Ada dua jalan keluar dan keduanya bukan tebakan.

| jalan | dari mana kelasnya |
|---|---|
| permintaan menyebut `tipe` | pemakainya sendiri yang menentukan |
| permintaan menyebut `parent_id` | **diwarisi** dari akun induk |

Pewarisan bukan dugaan: sub-akun dari sebuah akun aset **adalah** aset. Itu definisi. Bila
permintaan tidak menyebut kelas maupun induk, barulah ditolak — dan penolakannya menyebutkan
kedua jalannya, bukan hanya berkata "belum tersedia".

Yang tetap **tidak** dilakukan: menyimpulkan kelas dari awalan kode akun. Konvensi
"1 = aset, 2 = kewajiban" memang lazim, tetapi ia konvensi satu bagan akun, bukan aturan. Salah
menebaknya menaruh akun pada sisi neraca yang keliru, dan salahnya baru terlihat saat laporan
disusun.

Kosakata `tipe` adalah **kelas akuntansi** — `ASET`, `KEWAJIBAN`, `EKUITAS`, `PENDAPATAN`,
`BEBAN` — bukan sandi tipe vendor. Bagan akun yang diunggah dari Accurate memakai sandi lain
(`BANK`, `AREC`, `OEXP` …) yang disimpan jalur legacy pada kolomnya sendiri. Menerima kedua
kosakata pada satu kolom akan membuat `idx_akun_tipe` memuat campuran, dan laporan mana pun yang
mengelompokkan menurut kelas akun tidak lagi punya dasar. Nilai di luar daftar ditolak, dan
daftarnya disebutkan pada pesannya.

### `saldo_normal` disimpan justru karena kelas akun TIDAK menentukannya

Aturan "turunkan, jangan simpan" berlaku sepanjang nilainya memang turunan. Yang ini bukan.

Akun lawan seperti **Akumulasi Penyusutan** berkelas `ASET` tetapi bersaldo normal **KREDIT**.
Repositori ini sudah memperlakukannya begitu: `KodeAkunApiHelper.posisiDariTipeAccurate`
menempatkan `DEPR` di sisi kredit bersama kewajiban dan ekuitas. Menurunkan saldo normal dari
kelasnya akan **membalik tanda setiap akun lawan** pada laporan keuangan.

Karena itu kelasnya hanya memberi **bawaan** — `ASET`/`BEBAN` → `D`, sisanya → `K` — yang boleh
ditimpa `debet_credit` dari permintaan. Blok 2 uji kesetaraan penjaganya: ia menuntut nilai
tersimpan berbeda dari nilai turunan, sehingga contoh yang tidak membedakan apa pun akan GAGAL.

Sisi kredit menerima `-1` maupun `2` pada masukan. Keduanya benar-benar beredar pada kolom
legacy yang sama: `Akun.CREDIT` bernilai `-1`, sedangkan `KodeAkunApiHelper` menulis `2`.
Perselisihan itu milik schema bersama; di sini keduanya diterima dan disimpan sebagai satu huruf.

`debet_credit = 0` berarti **"tidak disebut"**, bukan sandi yang salah, dan jatuh ke bawaan
kelasnya. Jalur legacy menyimpan nol itu apa adanya; menirunya akan merusak, sebab laporan
keuangan **mengalikan** saldo dengan sandi ini — akun bersandi nol selalu tampil nol. Menolaknya
juga salah: klien yang selalu mengirim `debet_credit` dengan bawaan `0` akan kehilangan
kemampuan membuat akun sama sekali. Nilai lain yang tidak dikenali tetap ditolak.

### `level` sengaja tidak diisi

`level` adalah kedalaman pada pohon `induk_id` — turunan penuh, tanpa kekecualian, tidak seperti
`saldo_normal`. Menyimpannya berarti menanggung kebenarannya selamanya: memindahkan satu akun ke
induk lain membuat level **seluruh keturunannya** salah, dan salahnya tidak kelihatan sampai ada
yang menggambar pohonnya. Kolomnya dibiarkan `NULL`; pembaca yang memerlukan kedalaman
menghitungnya dari `induk_id`. Level yang `NULL` tidak bisa berbohong; level yang basi bisa.

### Dua cacat lain yang ikut ketahuan, dan keduanya harus diperbaiki bersamaan

**1. `coaList` melempar pada setiap baris.** Jalur tenant memilih
`COALESCE(a.saldo_normal,'')` untuk kolom kelima, sedangkan pembacanya memanggil
`rs.getInt(5)`. `saldo_normal` bertipe `varchar`. Dibuktikan lewat JDBC sungguhan atas ketiga
kemungkinan isinya:

```
kode=1000 saldo_normal='D' -> getInt(5) = LEMPAR PSQLException: Bad value for type int : D
kode=1900 saldo_normal=''  -> getInt(5) = LEMPAR PSQLException: Bad value for type int :
kode=2000 saldo_normal='K' -> getInt(5) = LEMPAR PSQLException: Bad value for type int : K
```

Kolomnya yang kosong pun melempar, jadi tidak ada isi yang aman.

**Kedua cacat ini terkunci satu sama lain, dan itu sebabnya keduanya satu batch.** Selama
pembuatan akun ditolak, tabelnya kosong, `while (rs.next())` tidak pernah berjalan, dan
`coaList` diam-diam mengembalikan daftar kosong tanpa melempar apa pun. Membuka pembuatan
tanpa memperbaiki pembacaan akan mengubah layar yang diam-diam kosong menjadi layar yang
**jatuh** — persis pada hari pertama tenant memakai bagan akunnya.

Pemetaannya karena itu dipindah ke SQL, bukan diserahkan ke driver, dengan sandi legacy
`Akun.DEBET = 1` / `Akun.CREDIT = -1` — pasangan yang dipakai laporan keuangan untuk
**mengalikan** saldo dengan saldo normalnya. Akun tanpa saldo normal memberi `0`, persis yang
dikembalikan `getInt` atas kolom legacy yang `NULL`.

**2. Pembaruan diam-diam mengabaikan kode.** `ubahAkun` lama hanya menyentuh `nama`, sementara
jalur legacy juga menyimpan `kode` dan `parent`. Mengganti kode akun pada tenant karena itu
melaporkan sukses dan tidak mengubah apa pun. Sekarang keduanya ikut disimpan, dengan pemeriksaan
kode ganda yang mengecualikan dirinya sendiri, dan dengan penjaga lingkaran saat induk diganti
(`WITH RECURSIVE`, kedalaman dibatasi 64 supaya penelusuran tetap berhenti walau tabelnya sudah
terlanjur berlingkar karena sebab lain).

`induk_id` dan `saldo_normal` memakai pola **"ganti bila diberi"** — `COALESCE(?, kolom)` —
sebab permintaan yang hanya mengganti nama mengirim keduanya sebagai `NULL`. Tanpa itu, mengganti
nama akan menghapus induk dan saldo normal akun. Blok 5 uji penjaganya: ia menjalankan pola polos
`induk_id = ?` atas baris salinan dan menuntut hasilnya memang kosong.

`tipe` **tidak** ikut diperbarui. Kelas akun menentukan letak sebuah akun pada laporan keuangan;
mengubahnya sesudah ada jurnal yang menunjuknya memindahkan angka yang sudah dilaporkan tanpa
jejak apa pun. Pemindahan kelas adalah pekerjaan penataan bagan akun, bukan penyuntingan satu
baris.

### `keterangan` dijawab, bukan didiamkan

Model tenant tidak punya kolomnya. Permintaan yang mengirimnya tidak ditolak — itu akan
mematahkan klien yang selalu mengirim medan itu — melainkan dijawab dengan medan `peringatan`,
pola yang sama dengan `purchaseTermsSave` pada §15. Pemanggilnya jadi tahu keterangannya tidak
tersimpan, alih-alih mengiranya tersimpan.

### Verifikasi

SQL yang benar-benar dikeluarkan Java dijalankan atas klaster sekali-pakai berisi katalog v1–v18:

| yang diuji | hasil |
|---|---|
| buat `1000 Kas` (`tipe` disebut) | tersimpan, `saldo_normal = D` |
| buat `1100 Kas Kecil` (hanya `parent_id`) | `tipe` **diwarisi** `ASET` |
| buat `1900 Akm. Penyusutan` (`debet_credit = -1`) | `ASET` bersaldo normal **`K`** |
| buat `2000 Hutang` (tanpa `debet_credit`) | bawaan `K` dari `KEWAJIBAN` |
| `coaList` → `rs.getInt(5)` | `1, 1, -1, -1` — tidak melempar |
| kode ganda | ketemu saat menyisip; bebas saat mengubah dirinya sendiri |
| penjaga lingkaran | anak sebagai induk terdeteksi; bukan-keturunan lolos |
| ganti kode | benar-benar berubah (dulu diabaikan) |
| ganti nama saja | induk dan saldo normal **bertahan** |
| `level` | `NULL` pada keempat baris |

`uji-kesetaraan-bagan-akun.sql` — enam blok, **sepuluh LULUS, nol GAGAL**, tiga di antaranya
penjaga.

Tidak ada bundel migrasi baru: seluruh kolom yang dipakai sudah ada sejak v7, dan
`uq_..._akun_kode` tetap jaminan sesungguhnya atas kode ganda — pemeriksaan di Java hanya untuk
pesan yang enak dibaca, dan dua permintaan serentak tetap berakhir pada pelanggaran batasan,
bukan pada dua akun berkode sama.

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
