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

## Yang BELUM dikerjakan

- **Belum ada satu pun kueri yang memakai tabel ini.** Menyambungkan `si_*` ke schema
  tenant adalah P4.
- **Penulis audit belum ada.** Tabel `audit_baris` sudah berdiri, tetapi yang mengisinya —
  perluasan pola `TenantDataPlaneService` ke tabel tenant — belum dibuat.
- `gudang_id`/`lokasi_stok_id` sudah ada pada mutasi dan saldo, tetapi kebijakan
  penempatannya (satu gudang bawaan per toko, atau wajib dipilih) belum diputuskan.
