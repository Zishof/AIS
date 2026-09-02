# 03 — Migrasi schema tenant (FASE P3, ditambah v9)

Delapan bundel baru di atas baseline `v1-core-pos`. **74 tabel ERP, 5 tabel audit,
217 indeks.** Mesin migrasinya (`TenantSchemaService`) **tidak diubah sama sekali** —
`terapkanMigrasi` dan `verifikasiLengkap` sudah mengikuti isi katalog dengan sendirinya.

| Bundel | Isi | Pernyataan |
|---|---|---|
| `v2-inventory-master-erp` | gudang, lokasi stok, akses tenant, sales, supplier, customer | 31 |
| `v2-inventory-master-audit` | `revinfo` diperluas + `audit_baris` generik | 21 |
| `v3-inventory-stock-erp` | satuan, kategori, produk, batch, mutasi, saldo, opname, harga | 52 |
| `v4-inventory-purchase-ap-erp` | pembelian + hutang dagang | 30 |
| `v5-inventory-sales-ar-erp` | penjualan + piutang dagang | 42 |
| `v6-inventory-trip-erp` | surat perintah, trip, nota, hasil, biaya, setoran, rekonsiliasi | 34 |
| `v7-inventory-accounting-erp` | akun, periode, jurnal, posting/reversal log | 26 |
| `v8-inventory-import-erp` | idempotensi, print log, staging impor legacy | 27 |
| `v9-pos-ebisnis-erp` | tabel POS eBisnis yang tidak ada di arsip FoxPro | 41 |

`VERSI_TERKINI` → `v9-pos-ebisnis`.

### Mengapa v9 ada

v2–v8 diturunkan dari **§11.3 dokumen master**, yang menggambarkan domain Inventory & Sales
dari arsip DBF. Permukaan `si_*` juga menyentuh tabel POS eBisnis yang sudah berjalan — sesi
kas kasir, retur, keranjang draft, foto produk — dan semuanya tidak ada di dunia FoxPro itu.
Sepuluh tabel v9 menutup selisihnya, ditambah dua perubahan yang menutup jebakan pemetaan:

- `ALTER kategori_produk ADD maksimal_harian` — tanpa ini, pemetaan `jenis_produk` kehilangan
  batas pembelian harian (**J-2**);
- `customer_anggota_profile` — menampung keanggotaan yang tidak muat di `customer` (**J-3**).

Rinciannya di [05-pemetaan-tabel.md](05-pemetaan-tabel.md).

**Kata sandi tidak ikut.** `anggota_koperasi.pass` tidak punya kolom penampung di mana pun
pada v9, dan itu disengaja (§24 butir 13).

**v9 tetap bergaya 9.3** walau produksi terbukti 9.5+. Konsisten dengan v2–v8, tetap sah, dan
tidak menuntut penjaga dilonggarkan. Longgarkan hanya bersamaan dengan bundel yang benar-benar
memerlukan sintaks baru.

## Keputusan yang paling menentukan

### Audit generik, bukan enam puluh cermin kolom

v1 memakai **cermin per-tabel** gaya Envers: satu tabel audit untuk satu tabel data,
kolomnya disalin satu per satu. Pola itu **tidak diteruskan**.

Alasannya dua. Setiap penambahan kolom bisnis akan menuntut ALTER kembar di sisi audit,
dan yang lupa tidak menimbulkan galat — hanya kolom yang diam-diam tidak pernah terekam.
Dan daftar medan wajib §11.6 sendiri memuat `before/after`, bentuk yang menunjuk catatan
baris generik.

Yang dibangun: `revinfo` diperluas dengan konteks §11.6 (`tenant_id`, `membership_id`,
`user_id`, `role`, `actor_type`, `device_id`, `request_id`, `correlation_id`,
`idempotency_key`, `action`, `reason`, `waktu`), ditambah `audit_baris` generik
(`entity`, `entity_id`, `revtype`, `sebelum`, `sesudah`).

Cermin v1 **dibiarkan berdiri** — `TenantDataPlaneService` masih menulis ke sana, dan
menghapusnya melanggar append-only.

**Yang hilang dari cermin kolom tidak menyakitkan di sini**, karena riwayat harga tidak
bergantung audit: `harga_beli_supplier` dan `harga_jual_customer` berversi di schema ERP,
jadi "berapa harga barang ini pada tanggal X" dijawab tabel bisnis.

### Bundel audit kosong adalah jebakan, bukan slot cadangan

§11.2 menamai pasangan `-erp` dan `-audit` untuk setiap versi. Untuk v3–v8 tidak ada DDL
audit yang perlu dijalankan, dan saya **hampir** mendaftarkan bundel kosong demi kesetiaan
pada penamaan itu.

Itu keliru. Bundel kosong yang terdaftar akan **tercatat checksum-nya pada setiap tenant**;
menambahkan DDL ke slot itu kelak mengubah checksum dan membuat migrasi gagal keras di
seluruh tenant. Bundel audit v3–v8 karena itu **tidak ada**. Bila kelak perlu, tambahkan
versi baru di akhir katalog.

Penjaga di `TenantSchemaMigrasiSelfTest` menolak bundel kosong secara eksplisit.

### Unique constraint pada konteks natural, bukan pada nomor

§11.4 menegaskan nomor faktur tidak unik global, dan arsip legacy membuktikannya. Unique-nya:

- pembelian → `(supplier_id, nomor_faktur, tanggal)`
- faktur penjualan → `(customer_id, nomor_faktur, tanggal)`

Unique global akan menggagalkan impor pada data yang sebenarnya sah.

### Tidak ada unique pada pasangan harga

MASTERBL memuat sepuluh pasangan supplier–produk berulang (satu rangkap tiga), MASTERJL
tiga puluh tujuh pasangan customer–produk berulang. Yang dipakai adalah rentang berlaku,
dan duplikatnya ditandai `kandidat_duplikat` untuk antrean pembersihan. Memasang unique
di sana akan menggagalkan impor pada baris yang justru perlu diperiksa manusia.

### Mutasi append-only, saldo turunan

`mutasi_stok` tidak pernah di-UPDATE — koreksi memakai baris pembalik lewat
`pembalik_dari_id`. `saldo_stok` adalah ringkasan yang boleh dihitung ulang. Menyimpan
saldo saja membuat selisih tidak dapat ditelusuri; menyimpan mutasi saja membuat setiap
layar stok memindai jutaan baris.

### Baris terhapus legacy bukan penghapusan

TRAN_HUT menyimpan 8.793 baris bertanda hapus terhadap 70 aktif; TRAN_PIUT 35.178
terhadap 832. Rasio itu memberitahu bahwa tandanya dipakai sebagai penanda **lunas/riwayat**,
bukan pembatalan. Kolom `legacy_deleted` dan `legacy_tafsir` menyimpannya; dibuang buta
akan menghapus seluruh riwayat pelunasan (§25.4).

### `JUA-rusakL.DBF` tidak diimpor

Atas keputusan pengguna, dan pemeriksaan mendukungnya: 88.553 dari 88.592 barisnya identik
dengan JUAL, sedangkan JUAL memuat 5.490 baris lebih baru. Dua belas baris yang hanya ada
di sana berasal dari tiga faktur tanpa jejak apa pun di TRAN_PIUT — keadaan sebelum koreksi.

### Kata sandi legacy tidak punya tempat

`USERS.DBF` memuat kolom `PSW`. Tidak ada satu pun kolom pada v8 yang menampungnya, dan itu
disengaja (§24 butir 13). Kredensial disetel ulang, bukan dipindahkan.

## Catatan PostgreSQL

`CREATE INDEX IF NOT EXISTS` (9.5) dan `ADD COLUMN IF NOT EXISTS` (9.6) **tidak dipakai**.
Idempotensi datang dari riwayat versi migrasi, dan DDL PostgreSQL transaksional — migrasi
yang gagal di tengah membatalkan seluruhnya termasuk baris riwayatnya. `jsonb` (9.4) juga
tidak dipakai; muatan `sebelum`/`sesudah` bertipe `text`.

Uang `numeric(18,2)`, kuantitas `numeric(18,4)` — tidak pernah float. Kode legacy `varchar`
supaya angka nol di depan bertahan.

## Penjaga: `TenantSchemaMigrasiSelfTest`

`java ais.service.tenant.test.TenantSchemaMigrasiSelfTest`

**Checksum sebelas bundel dipatok.** DDL kanonik masuk ke checksum, dan checksum yang
berubah membuat `terapkanMigrasi` gagal keras di setiap tenant yang sudah memasangnya.
Bahayanya halus: menyunting satu konstanta bersama seperti `JEJAK` mengubah belasan
pernyataan sekaligus tanpa terlihat pada diff yang sempit.

Sensitivitasnya dibuktikan, bukan diasumsikan — mengubah `varchar(64)` menjadi
`varchar(65)` memindahkan checksum v2 dari `71da30d6bde0` ke `35da622c6433`, dan menambah
**satu spasi** memindahkannya ke `00bd3aebbc65`.

Penjaga ini juga memeriksa hal yang hanya terlihat bila katalog dibaca sekaligus: FK yang
menunjuk tabel yang belum dibuat, nama tabel/indeks ganda, kurung tidak seimbang,
placeholder tersisa, sintaks di luar 9.3, bundel kosong, dan `TABEL_WAJIB_*` yang menyebut
tabel yang tidak pernah dibuat.

> Mengubah patokan hanya boleh bila versinya **belum terpasang pada tenant mana pun**.
> Sesudah terpasang, satu-satunya jalan benar adalah menambah versi baru di akhir katalog.

## Dijalankan pada PostgreSQL sungguhan

Katalog ini **sudah dieksekusi utuh**, bukan hanya diperiksa secara statis.

| | |
|---|---|
| Mesin | PostgreSQL 16.4 |
| Pernyataan dijalankan | **313** (311 DDL katalog + 2 `CREATE SCHEMA`) |
| Galat | **0** |
| Tabel ERP | **74** dari bundel + 1 `tenant_schema_migration` = **75**, cocok `TABEL_WAJIB_ERP` |
| Tabel audit | **5**, cocok `TABEL_WAJIB_AUDIT` tanpa selisih |
| Indeks | 323 |
| Kolom | 1.245 |

Ke-11 checksum bundel cocok dengan nilai yang dipatok di `TenantSchemaMigrasiSelfTest`, jadi
yang dijalankan memang katalog yang sama dengan yang dijaga uji statis.

### Cara mengulanginya

Verifikasi ini **tidak** memerlukan basis data yang sudah ada, dan sengaja begitu: ia
dijalankan pada klaster sekali-pakai supaya tidak ada kemungkinan menyentuh data sungguhan.

```
initdb -D <dir-sementara> -U uji --auth=trust
pg_ctl -D <dir-sementara> -o "-p 55432" start

javac -sourcepath src/main/java -d out       src/main/java/ais/service/tenant/test/TenantSchemaDdlDump.java
java -cp out ais.service.tenant.test.TenantSchemaDdlDump uji_tenant uji_tenant__audit > katalog.sql
psql -h 127.0.0.1 -p 55432 -U uji -d postgres -v ON_ERROR_STOP=1 -f katalog.sql
```

`ON_ERROR_STOP=1` penting: tanpanya psql melanjutkan sesudah pernyataan yang gagal dan
kode keluarnya tetap nol, sehingga katalog rusak tampak lulus.

### Satu selisih yang BUKAN cacat

Skrip menghasilkan 74 tabel sedangkan `TABEL_WAJIB_ERP` berisi 75. Yang tidak ikut adalah
`tenant_schema_migration` — tabel riwayat yang dibuat `TenantSchemaService.terapkanMigrasi`
sendiri, bukan oleh bundel DDL. Selisih satu ini **wajar dan harus tetap ada**; kalau suatu
saat skrip menghasilkan 75, berarti ada bundel yang keliru ikut membuat tabel riwayat.

## Bundel v10 — tiga celah yang menghadang P4

Bundel pertama yang **tidak menambah tabel**. Ia menutup tiga celah yang masing-masing
menghentikan satu bagian pemindahan `si_*`, dan ketiganya ditemukan dengan cara yang sama:
mencocokkan kueri legacy terhadap schema tenant sungguhan, bukan membaca dokumen.

| | Celah | Yang tadinya terhalang |
|---|---|---|
| J-10.1 | `harga_jual_customer.customer_id` `NOT NULL` | harga umum pada helper Harga |
| J-10.2 | `sales_trip_biaya` tanpa idempotensi | seluruh kelompok biaya trip |
| J-10.3 | `penerimaan_piutang` tanpa kaitan trip | `tripDetail` |

### Mengapa bundel baru, bukan menyunting v1–v9

Katalog bersifat **append-only ber-checksum**. Menyunting DDL bundel terrilis membuat
`terapkanMigrasi` gagal keras pada **setiap** tenant yang sudah di-provision. Perubahan schema
karena itu selalu berupa bundel baru berisi `ALTER`.

Swauji membuktikan tidak ada yang tersentuh: ia memeriksa checksum v1–v9 terhadap patokan, dan
lulus.

### J-10.1 — harga umum

Legacy menyatakan harga umum sebagai baris `harga_jual_customer` dengan
`anggota_koperasi IS NULL`. Kolom tenant semula `NOT NULL`, sehingga baris semacam itu
mustahil ada.

Alternatif yang ditolak: memakai `produk.harga_jual_standar`. Keduanya berbeda maksud —
harga jual standar adalah **atribut produk**, sedangkan harga umum adalah **versi
berharga-berlaku** yang punya tanggal mulai, tanggal akhir, dan riwayat. Menyamakannya
menghilangkan riwayat harga umum.

Indeks uniknya **parsial** (`WHERE customer_id IS NULL`): satu harga umum per produk per
tanggal berlaku, tanpa mengusik aturan harga khusus pelanggan.

### J-10.2 — idempotensi biaya trip

Legacy **mewajibkan** `kode_unik` saat mencatat biaya trip; itu bukan pilihan. Tanpa kolom
penampungnya, satu permintaan yang diulang membukukan biaya **dua kali** dan langsung merusak
total biaya trip pada rekonsiliasi.

Indeksnya parsial (`WHERE idempotency_key IS NOT NULL`) supaya baris lama tanpa kunci tetap
sah dan tidak saling bentrok sebagai NULL ganda.

### J-10.3 — penerimaan piutang per trip

Tanpa kaitan ini, jumlah penagihan selama satu trip hanya dapat ditempuh lewat rantai
`sales_trip_nota` → `faktur_penjualan` → `piutang_customer` → alokasi. Rantai itu **tidak
setara**: ia hanya menemukan penagihan atas faktur yang terbit pada trip yang sama, sedangkan
sales juga menagih **piutang lama** saat berkeliling — dan justru itulah yang biasanya jadi
alasan perjalanannya.

Kolomnya `NULL`-able: penerimaan di kantor memang tidak punya trip.

### Diverifikasi pada PostgreSQL 16.4

**Dua jalur**, keduanya nol galat:

1. Schema baru dengan v1–v10 sekaligus — 319 pernyataan
2. **Jalur naik-versi**: schema dibangun sampai v9 saja (seperti tenant yang sudah
   di-provision), lalu keenam pernyataan v10 menyusul di atasnya

Jalur kedua yang menentukan. Bundel yang hanya diuji pada schema kosong tidak membuktikan
apa pun tentang tenant yang sudah berjalan.

**Perilakunya diuji dua arah**, bukan hanya "berhasil":

| Yang diuji | Hasil |
|---|---|
| Sisip harga umum (`customer_id` NULL) | diterima |
| Harga umum ganda pada tanggal sama | **ditolak** |
| Biaya trip berkunci baru | diterima |
| Permintaan diulang dengan kunci sama | **ditolak** |
| Dua biaya lama tanpa kunci | diterima (indeks parsial) |
| Penerimaan bertrip, dan penerimaan kantor tanpa trip | keduanya diterima |
| Penerimaan menunjuk trip yang tidak ada | **ditolak** |

### Yang TIDAK dilakukan bundel ini

Tidak ada pengisian data. Baris yang sudah ada tetap `NULL` pada ketiga kolom baru, dan itu
benar: menebak trip mana yang menagih suatu penerimaan lama, atau kunci idempotensi mana yang
dulu dipakai, hanya melahirkan data yang tampak sahih tanpa dasar.

## Bundel v11 — kunci idempotensi yang selama ini tidak mengikat apa pun

Bundel ini tidak menambah tabel dan tidak menambah kolom. Ia menambahkan **indeks unik parsial**
pada sebelas kolom `idempotency_key`.

### Bagaimana celah ini ditemukan

Saat menyiapkan pemindahan `payablePaymentReverse`, jalur tenantnya perlu jaminan bahwa satu
perintah balik tidak menghasilkan dua dokumen pembalik. Jalur legacy memperolehnya dari batasan
basis data — kodenya bahkan menangkap `ConstraintViolationException` dan memperlakukannya sebagai
pengulangan yang sah.

Pemeriksaan katalog menunjukkan batasan itu **tidak ada** pada schema tenant. Pemeriksaan
lanjutan terhadap seluruh katalog menunjukkan celahnya bukan satu, melainkan sebelas:

| | jumlah |
|---|---|
| Tabel ber-`idempotency_key` | 12 |
| Di antaranya berindeks **unik** sebelum v11 | 1 (`idempotency_record`, `UNIQUE (idempotency_key, aksi)`) |
| Berindeks biasa atau tanpa indeks | **11** |

Sebelas tabel itu: `pembelian`, `pembayaran_hutang`, `sales_order`, `faktur_penjualan`,
`penerimaan_piutang`, `sales_trip`, `sales_trip_nota`, `sales_trip_setoran`, `mutasi_stok`,
`jurnal`, `posting_log`.

Perlu dicatat jujur: indeks unik untuk `sales_trip_biaya` yang dipasang **v10** menutup satu
anggota dari kelas celah yang sama tanpa saya sadari kelasnya. v11 menutup sisanya.

### Seberapa berbahaya — dan seberapa tidak

Jalur tulis tenant yang sudah berjalan (misalnya `payablePaymentCreate`) **memeriksa kuncinya
lebih dulu** dan mengembalikan `idempotentReplay` bila sudah ada. Pemeriksaan itu menyelamatkan
kasus yang lazim: klik ganda, atau klien mengulang permintaan setelah waktu habis. Keduanya
berurutan, dan keduanya tertangani.

Yang tidak terlindungi adalah dua permintaan yang **benar-benar bersamaan**: keduanya lolos
pemeriksaan sebelum salah satunya sempat menyisipkan, lalu keduanya menyisipkan. Hasilnya satu
supplier dibayar dua kali dari satu perintah bayar.

Pola "periksa lalu sisipkan" tanpa batasan basis data memang tidak pernah cukup — satu baris
selalu dapat menyelinap di antara keduanya. Yang menutupnya hanya indeks unik.

Jadi: bukan cacat yang menghantam pemakaian sehari-hari, tetapi jaminan yang jalur legacy punya
dan jalur tenant tidak. Selisih jaminan itulah yang ditutup.

### Mengapa parsial

Semua kolomnya boleh `NULL`, dan baris hasil impor legacy umumnya memang tidak berkunci.
`WHERE idempotency_key IS NOT NULL` membuat baris-baris itu tetap sah dan tidak saling bentrok
sebagai NULL berulang. Bentuknya sama persis dengan indeks yang sudah dipasang v10.

### Bundel ini DAPAT gagal, dan itu disengaja

Bila suatu tenant terlanjur menyimpan dua baris berkunci sama pada tabel yang sama, pembuatan
indeksnya ditolak dan migrasi berhenti dengan galat.

Itu perilaku yang diinginkan. Baris kembar semacam itu **adalah** dokumen ganda yang hendak
dicegah kolom ini; menemukannya saat migrasi jauh lebih baik daripada membiarkannya diam di dalam
data. Penanganannya: periksa pasangan baris itu dan batalkan yang bukan asli — bukan melonggarkan
indeksnya.

### Diverifikasi pada PostgreSQL 16

Katalog v1–v11 dijalankan utuh pada klaster sekali-pakai (`initdb --auth=trust`, tanpa kredensial,
tanpa menyentuh data mana pun):

| pemeriksaan | hasil |
|---|---|
| 79 tabel + seluruh ALTER v10 dan v11 | jalan bersih, `psql -v ON_ERROR_STOP=1` keluar 0 |
| indeks unik parsial `idempotency_key` terbentuk | 12 (11 dari v11 + 1 dari v10) |
| `TenantSchemaMigrasiSelfTest` | LULUS — 13 migrasi, versi terkini `v11-idempotensi` |
| checksum bundel v11 | `78ef67a0241d`, dipatok di `PATOKAN` |

Bukti bahwa indeksnya benar-benar menggigit ada pada blok 3 `uji-kesetaraan-reversal.sql`:
indeksnya dilepas dulu, baris kembar dibuktikan **lolos** (keadaan pra-v11), lalu indeksnya
dipasang lagi dan baris kembar yang sama **ditolak**. Tanpa langkah pertama, blok itu tidak
membuktikan indeksnya yang menolak.

### Yang TIDAK dilakukan bundel ini

Tidak ada penghapusan atau penggabungan baris kembar secara otomatis. Menebak mana di antara dua
dokumen uang yang "asli" bukan pekerjaan migrasi.

## Bundel v12 — buku kas trip (celah C-11)

Bundel ini menambah satu tabel, satu kolom sumber, dan tiga kolom pada tabel biaya. Ia menutup
celah C-11: model tenant tidak punya tempat untuk mencatat uang tunai yang berpindah selama satu
perjalanan sales.

### Mengapa ketiadaannya berakibat, bukan sekadar kurang lengkap

Jalur legacy menyimpan satu buku kas per sesi, `nota_sales_kas`: baris bertanda dengan sembilan
jenis, dan saldo kas adalah **penjumlahan bertandanya**. Rumus itu dipakai daftar trip, layar
rinci, dan penutupan sesi.

Tanpa tabel itu, kueri tenant yang **sudah terkirim** menghitung saldo kas sebagai
`Σ nota.tunai − Σ setoran`. Untuk trip dengan panjar 500.000, penjualan tunai 300.000, penagihan
tunai 200.000, biaya 100.000, dan setoran 400.000:

| | hasil |
|---|---|
| buku kas legacy | **500.000** |
| rumus tanpa buku kas | **−100.000** |

Bukan selisih kecil — beda tanda. Angka itu diverifikasi, bukan diperkirakan: blok 2
`uji-kesetaraan-kas-trip.sql` menjalankan kedua rumus atas data yang sama dan mensyaratkan
selisihnya muncul.

### Yang TIDAK ditambahkan: kolom saldo kas awal

Entitas legacy punya `NotaSalesSession.saldoKasAwal`, disalin dari `spj.uangMukaOperasional` saat
trip dimulai. Godaannya adalah menirunya sebagai kolom `sales_trip.saldo_kas_awal`.

Sengaja tidak. Pada jalur legacy uang muka itu **juga** dibukukan sebagai baris `OPENING_ADVANCE`,
dan saldo kasnya menjumlahkan baris — sehingga kolomnya hanyalah salinan yang kebetulan sama. Dua
sumber untuk satu angka adalah bentuk cacat yang sudah berkali-kali ditemukan pada pemindahan ini.
Di sini uang muka awal **diturunkan** dari bukunya sendiri, dan tidak ada yang bisa berselisih.

Yang memang perlu ditambahkan adalah **sumbernya**: `surat_perintah_sales.uang_muka_operasional`.
Tanpa itu tidak ada angka yang bisa dibukukan sebagai baris pembuka.

### Nominal BERTANDA, menyimpang dari pola `mutasi_stok`

Katalog memakai pola "kuantitas selalu positif + kolom arah" pada `mutasi_stok`. Buku kas sengaja
tidak mengikutinya.

Seluruh makna buku kas legacy adalah penjumlahan bertanda, dan setiap pembacanya menjumlahkan
langsung. Menyimpan besaran positif berikut arah terpisah memaksa **setiap** pembaca menyusun ulang
tandanya, dan satu pembaca yang lupa menghasilkan angka uang yang salah tanpa gagal. Kekeliruan
persis jenis itu sudah pernah terkirim sekali pada rumus saldo kas; tandanya disimpan sekali di
sisi penulis supaya tidak ada yang perlu menyusunnya ulang.

Kontrak jenis dan tandanya hidup pada `TenantKasTrip`, bukan sebagai batasan `CHECK`. Batasan itu
akan menolak baris yang jalur legacy terima saat impor, dan menjadikan penambahan jenis kesepuluh
sebagai migrasi tersendiri.

### Biaya trip: tiga kolom supaya kas dan pembalikannya utuh

`sales_trip_biaya` tidak punya cara membedakan biaya tunai dari non-tunai, sehingga tidak ada dasar
memutuskan apakah suatu biaya menyentuh kas — `cara_bayar` menutup itu. Ia juga satu-satunya tabel
dokumen tanpa `pembalik_dari_id` dan tanpa `status`; keduanya ditambahkan, sehingga baris pembalik
dapat dipasangkan kembali dengan aslinya.

### Koreksi klaim: bundel ini TIDAK membuka tujuh aksi

Catatan sebelumnya menyebut bundel kas "membuka tujuh aksi". Setelah ditelusuri satu per satu,
angka itu **terlalu optimistis** — beberapa aksi menunggu lebih dari satu hal:

| | aksi |
|---|---|
| **terbuka oleh v12** | `tripCashSale`, `expenseReverse`, penuntasan `saldoKas` pada `tripList` |
| masih menunggu tabel nota bawaan | `collectionCreate`, `collectionReverse`, `tripClose`, `spjNotaAssign`, `tripNotaResult` |
| masih menunggu master kategori biaya | `expenseCreate` |
| masih menunggu tabel pembelian trip | `tripPurchaseLink`, sisi pembelian `tripDetail` |

Ketiga penghalang sisa itu bundel tersendiri dan tidak dicampurkan ke sini: masing-masing
menyangkut konsep berbeda, dan menggabungkannya membuat satu bundel yang gagal seluruhnya bila satu
bagiannya keliru.

### Diverifikasi pada PostgreSQL 16

| pemeriksaan | hasil |
|---|---|
| katalog v1–v12 dijalankan utuh | bersih, `psql -v ON_ERROR_STOP=1` keluar 0 |
| tabel terbentuk | 80 (74 sebelumnya + `sales_trip_kas`, berikut tabel schema audit) |
| kolom `sales_trip_kas` | 19 |
| kolom baru `sales_trip_biaya` | `cara_bayar`, `status`, `pembalik_dari_id` (+`idempotency_key` dari v10) |
| `surat_perintah_sales.uang_muka_operasional` | ada |
| indeks unik parsial idempotensi | 14 |
| `TenantSchemaMigrasiSelfTest` | LULUS — 14 migrasi, versi terkini `v12-kas-trip` |
| checksum bundel v12 | `01b490764581`, dipatok di `PATOKAN` |

### Yang TIDAK dilakukan bundel ini

Tidak ada pengisian data. Trip yang sudah berjalan tidak memperoleh baris pembuka secara surut, dan
biaya lama tetap `NULL` pada `cara_bayar`. Menebak berapa uang muka yang dulu dibawa, atau biaya
mana yang dulu tunai, hanya melahirkan angka uang yang tampak sahih tanpa dasar.

## Yang BELUM dikerjakan

- **Belum ada satu pun kueri yang memakai tabel ini.** Menyambungkan `si_*` ke schema
  tenant adalah P4.
- **Penulis audit belum ada.** Tabel `audit_baris` sudah berdiri, tetapi yang mengisinya —
  perluasan pola `TenantDataPlaneService` ke tabel tenant — belum dibuat.
- `gudang_id`/`lokasi_stok_id` sudah ada pada mutasi dan saldo, tetapi kebijakan
  penempatannya (satu gudang bawaan per toko, atau wajib dipilih) belum diputuskan.
