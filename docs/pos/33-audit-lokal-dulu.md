# Audit lokal-dulu — apa yang dikonversi, apa yang tidak, dan mengapa

> Mekanismenya sendiri dijelaskan di **[12-lokal-dulu-dan-hapus-lunak.md](12-lokal-dulu-dan-hapus-lunak.md)**. Dokumen ini melengkapi dengan hasil
> audit menyeluruh atas `lib/`: aksi mana yang dikonversi, mana yang **tetap
> wajib online**, dan jebakan yang ditemui saat mengerjakannya.

Seluruh mutasi POS ditulis ke perangkat **lebih dahulu**, baru dikirim ke server. Hapus di
sisi perangkat bersifat **lunak** sehingga masih dapat dipulihkan.

## Infrastrukturnya sudah benar sejak awal

`prosesSimpanMaster` memang lokal-dulu: `MasterOffline.antreLokal(...)` menulis ke antrean
perangkat **sebelum** `kirimSatuAntrean(...)` menyentuh jaringan. Yang menjadi masalah adalah
puluhan layar yang tidak memakainya sama sekali dan menembak `ApiClient.aksi` langsung.

Audit menyeluruh atas `lib/` menemukan 48 aksi tulis langsung di 30 berkas. **Tidak semuanya
boleh dikonversi** — lihat bagian "Yang tetap wajib online".

## Hapus lunak

`MasterOffline.terapkanLokal` tidak lagi membuang barisnya dari snapshot, melainkan
menandainya:

```dart
{ '_dihapus': true, '_dihapusPada': '<iso8601>' }
```

**Mengapa.** Penghapusan yang dikerjakan saat offline baru sampai ke server belakangan, dan
sampai saat itu **satu-satunya salinan datanya ada di perangkat**. Membuangnya seketika
berarti salah tekan tidak dapat dipulihkan sama sekali. Sisi server tidak memerlukan ini —
di sana sudah ada AuditTrails.

### Penyaringan terpusat

Baris bertanda disaring di **`MasterOffline._hasilDaftar`**, satu tempat untuk seluruh layar.
Menyaringnya di tiap layar berarti setiap layar baru harus ingat melakukannya, dan yang lupa
akan menampilkan baris yang menurut penggunanya sudah dihapus.

Pengguna melihatnya seolah benar-benar terhapus. Tandanya bersih sendiri ketika daftarnya
dimuat ulang dari server, yang memang tidak lagi mengirimkan baris itu.

### Pemulihan mengerjakan DUA hal

`MasterOffline.pulihkanLokal(cacheKey, id, {kunci})`:

1. melepas tanda hapus pada snapshot;
2. **membuang perintah hapus yang masih mengantre** (`CoreDb.outboxMasterHapusKunci`).

Melewatkan yang kedua berarti barisnya kembali tampil di layar tetapi **tetap terhapus di
server** begitu jaringan tersambung — kegagalan yang paling membingungkan, karena tampak
berhasil lebih dulu.

## Dua pola yang berulang saat mengonversi

### 1. Status offline bukan penolakan

Layar yang memeriksa `hasil['status'] != '00'` akan melaporkan **"ditolak server"** untuk
penyimpanan yang sebenarnya sah, sebab hasil offline berbentuk
`{'status':'success', 'offline':true}`. Ditemukan pada `penyesuaian_saldo_simpan`.

### 2. "Antre" bukan "berhasil"

Pengiriman massal yang menghitung baris terantre sebagai berhasil membuat petugas mengira
seluruh berkas sudah sampai server. Dibedakan pada Stok Opname (`'antre'` vs `'ok'`) dan
unggah massal Mutasi Hutang (penghitung `antre` tersendiri).

## Kunci antrean harus unik bila aksinya MENAMBAH

Antrean membuang baris PENDING berkunci sama saat kunci baru masuk. Untuk aksi yang menimpa
(ubah, hapus) itu benar. Untuk aksi yang **menambah**, itu bencana.

Folio hotel karenanya memakai kunci berstempel waktu:

```dart
kunci: 'folio_transaksi:$folioId:${DateTime.now().microsecondsSinceEpoch}'
```

Tanpa itu, penambahan kedua pada folio yang sama akan **membuang yang pertama** dari antrean
dan uangnya hilang dari catatan.

## Yang tetap wajib online — dan alasannya

Dikunci oleh `test/master_offline_kontrak_test.dart`.

| Aksi | Alasan |
|---|---|
| `ebisnis_role_menu_simpan` | hak akses harus berlaku seketika, bukan menyusul |
| `pedagang_ubah` (dgn password) | kredensial tidak boleh mengendap di antrean |
| `produk_simpan` (kulakan massal) | butuh id server seketika |
| `hotel_tamu_simpan` | idem |
| `si_coa_save` | akun yang muncul belakangan membuat jurnal mengacu akun tak dikenal |
| `batalkan_transaksi` | pembalikan transaksi (*reversal*), spec 13.3 |
| seluruh posting akuntansi | mengantre lalu memposting dapat masuk **periode yang salah**, atau setelah buku ditutup |
| `saldo_awal_simpan` / `_hapus` | mengantre sebagian baris neraca awal lalu memposting menghasilkan neraca tidak seimbang |
| `sinkron_referensi` | pemicu sinkron itu sendiri |
| `layar_pelanggan_kirim` | siaran layar pelanggan waktu-nyata |
| `produk_duplikat_hapus` | pembersihan sisi server; tidak ada baris lokal untuk dicerminkan |

### Kelas pengecualian kedua: alur layar bergantung jawaban server

`mutasi_stok_simpan` membalas `butuhPilihManual` ketika produk tujuan tidak dapat dicocokkan
otomatis — petugas harus memilih sendiri. Diantrekan saat offline, jawaban itu tidak pernah
muncul: mutasinya tampak berhasil lalu diam-diam gagal berjam-jam kemudian tanpa ada yang
menunggu untuk memilih. Lebih buruk daripada menolak di muka.

Stok Opname bergejala serupa (`selisih` datang dari server) tetapi dapat diselamatkan: stok
sistem memang sudah ada di layar, jadi selisihnya dihitung sendiri saat offline lalu
ditegaskan ulang begitu tersinkron.

### Kelas ketiga: alur persetujuan banyak pihak

Aksi SOP (`sop_batalkan_langkah`, `sop_batalkan_pengajuan`, `sop_ubah`) **tidak** dikonversi.
Membatalkan sebuah langkah saat offline lalu memutarnya berjam-jam kemudian, ketika penyetuju
lain sudah bertindak, dapat membatalkan sesuatu yang sudah berlanjut. Berbeda dengan Pesanan
dan Folio yang pemiliknya tunggal.

Ini keputusan proses, bukan teknis.

## Layar yang dikonversi

Retur Penjualan · Retur Pembelian · Stok Opname · Saldo Voucher · Mutasi Hutang ·
Pesanan · Nota Sales · Folio Hotel · Kulakan · Mutasi Antar Outlet · Apotik ·
Hutang Supplier · Realisasi Transitori · seluruh modul Keuangan
