# 47 — Transaksi terlambat menempel ke sesi kas yang salah

Laporan lapangan, 23–24 Agustus 2026: kasir **Agung** (Toko Al Bahjah) melapor angka penjualan di
dashboard "Laporan Kasir" berbeda jauh dari struk fisik Tutup Kas hari yang sama — 77 transaksi
(Rp 4.498.500) di dashboard, 137 di struk, selisih kas yang dilaporkan struk (-Rp 1.411.000)
lebih dari tiga kali lipat selisih sebenarnya (-Rp 451.000).

Diusut lewat serangkaian query terhadap basis data produksi (`albahjah`), bukan tebakan dari kode
saja — setiap hipotesis diuji dan dua di antaranya **terbukti salah** sebelum yang benar
ditemukan. Ditulis di sini termasuk jalan buntunya, supaya penelusuran serupa berikutnya tidak
mengulang langkah yang sama.

---

## 1. Dua laporan, dua sumber angka

- **Struk Tutup Kas** (`SesiKasUtil.laporanTutupKas`) menghitung dari header
  `koperasi.pembelian_anggota_koperasi`, dicocokkan ke sesi lewat kolom `sesi_kas_kasir` —
  atau, bila kolom itu kosong, lewat identitas kasir + rentang jam sesi.
- **Dashboard "Laporan Kasir"** (`PosApi.prosesLaporanTransaksiPerKasir`) menghitung dari
  `koperasi.pembelian` (baris item), disaring `DATE(a.waktu)` sama dengan tanggal yang dipilih.

Dua jalur berbeda, membaca kolom tanggal yang berbeda pula (`tanggal_pembayaran` pada header vs
`waktu` pada baris item). Keduanya SEHARUSNYA sepakat kalau setiap transaksi tertaut ke sesi yang
benar. Yang terjadi: tidak.

---

## 2. Dua hipotesis yang terbukti salah

**Hipotesis 1 — header tanpa baris item.** Dugaan awal: dashboard mensyaratkan minimal satu baris
`koperasi.pembelian`, dan ada header (mis. transaksi topup non-produk) yang tidak punya baris
sama sekali sehingga tak pernah muncul di dashboard.

```sql
-- hasil: 0 header tanpa item, dari 135 total
```

Nol. Semua header punya baris item. Hipotesis gugur.

**Hipotesis 2 — transaksi dibatalkan tidak disaring.** Dugaan kedua: `cakupanTransaksi()` pada
struk tidak menyaring status batal, sehingga transaksi yang sudah dibatalkan ikut terhitung.
Diperiksa lewat kode `PembatalanTransaksiUtil.batalkan()`: pembatalan di sini **menghapus**
baris header DAN item sekaligus (bukan menandai `aktif=false`), jadi transaksi batal lenyap dari
kedua query secara setara. Tidak mungkin ini penyebabnya — dicoret tanpa perlu query pembuktian.

**Kandidat ketiga yang juga sempat diuji dan gugur:** kolom `aktif` pada baris item (0 header
punya item ber-`aktif` bukan true), dan pergeseran zona waktu pada kolom `waktu` (selisih waktu
antara item dan headernya persis `00:00:00` — bukan offset jam, tapi tanggal yang jauh berbeda).

---

## 3. Akar masalahnya: 58 transaksi 20 Agustus menempel ke sesi 23 Agustus

Query pembanding langsung ke data produksi:

```sql
-- header sesi Agung 23 Agustus, dikelompokkan per tanggal PEMBAYARAN sungguhan
 tanggal    | jumlah_header | total_nilai | total_tunai
------------+---------------+-------------+-------------
 2026-08-20 |            58 |     2506000 |      960000
 2026-08-23 |            77 |     4498500 |     2832500
```

Cocok sampai ke rupiah dengan selisih yang dilaporkan: Rp 960.000 = persis selisih tunai antara
struk dan dashboard. Dashboard (yang menyaring per tanggal `waktu` baris item) benar; struk (yang
mengikuti FK `sesi_kas_kasir` apa adanya) tercemar 58 transaksi tiga hari lebih tua.

Dua kemungkinan diperiksa dan disingkirkan sebelum sampai ke penyebab sebenarnya:

- **Fitur Koreksi Supervisor** (`KantinHelper.java`, dekat baris 15700) mengubah `keterangan`
  header dengan menambahkan teks `"KOREKSI SUPERVISOR ..."`. Kolom `keterangan` pada seluruh 58
  header itu **kosong**. Bukan ini.
- **ID sesi dipakai ulang antar hari.** Diperiksa langsung: sesi 20 Agustus ber-id 222, sesi 23
  Agustus ber-id 265 — dua baris terpisah, masing-masing `kode` unik, keduanya `TUTUP`/`BUKA`
  dengan bersih. Bukan ini juga.

Penyebab sebenarnya: **`KantinHelper.bayar()`**, jalur penanganan `kode_sesi_kas` yang tidak
dikenal server.

---

## 4. Kode yang menyebabkannya

POS ini bekerja **lokal-dulu**: penjualan tersimpan di perangkat lebih dulu, pengirimannya
menyusul — bisa detik, bisa hari, tergantung jaringan. Perbaikan sebelumnya (komentar "KE-FIX",
insiden 61 transaksi tertahan di toko yang sama, 19–21 Agustus 2026) sudah benar mencegah
transaksi **hilang** ketika kode sesi yang dibawa perangkat tidak dikenal server (mis. versi
aplikasi lama yang menyimpan kode karangan):

```java
if (sesiAsal == null) {
    sesiAsalSah = true;                                   // transaksi TETAP diterima -- benar
    hasil.put("sesi_kas_tidak_dikenal", kodeSesiDiminta);
    // (sebelum perbaikan ini) sesiKasTransaksi dibiarkan null di sini
}
```

`sesiKasTransaksi` yang dibiarkan `null` kemudian jatuh ke baris di ujung method:

```java
if (sesiKasTransaksi == null) {
    sesiKasTransaksi = sesiKasAktif;   // sesi yang KEBETULAN sedang terbuka SAAT DATA TIBA
}
```

Itulah celahnya: transaksi diterima (benar), tetapi ditempelkan ke sesi yang sedang berjalan
**saat pengiriman akhirnya berhasil**, bukan sesi tempat penjualannya benar-benar terjadi.
Transaksi 20 Agustus yang baru terkirim di sela sesi 23 Agustus pun ikut tercatat ke sana.

---

## 5. Perbaikannya

Satu helper baru, dipakai di satu titik.

**`SesiKasUtil.sesiPadaWaktu(session, tokoId, kasirNama, kasirUserId, waktu)`** — mencari sesi
(status apa pun, BUKA atau TUTUP) yang **jamnya mencakup** `waktu`. Kriterianya sama persis
dengan yang sudah dipakai fitur Koreksi Supervisor (`waktuBuka <= waktu AND (waktuTutup IS NULL
OR waktuTutup >= waktu)`) — disatukan ke satu tempat supaya kedua jalur tidak dapat berbeda
perilaku diam-diam, alih-alih menulis ulang kriteria yang sama untuk kedua kalinya.

Di `bayar()`, cabang "kode sesi tidak dikenal" kini mencari dulu sesi yang mencakup **waktu
transaksi itu sendiri** (`currentWaktu`, sudah tersedia dari payload), baru jatuh ke `sesiKasAktif`
bila memang tidak ada sesi yang cocok (mis. transaksi dari sebelum fitur sesi kas dipakai —
kasus ini tetap harus diterima, bukan ditolak):

```java
sesiKasTransaksi = SesiKasUtil.sesiPadaWaktu(session, toko.getId(), idKasir[0], idKasir[1], currentWaktu);
```

**Yang TIDAK diubah, sengaja:** gerbang penerimaan transaksi itu sendiri. Transaksi berkode sesi
tak dikenal tetap **diterima** (`sesiAsalSah = true`), persis seperti sebelumnya. Yang berubah
murni soal **ke sesi mana ia dicatat** untuk keperluan laporan — bukan apakah ia diterima.
Mengencangkan gerbang penerimaan adalah persis kesalahan yang menyebabkan insiden 61 transaksi
sebelumnya; catatan besar di kode itu sendiri memperingatkan agar tidak diulang.

---

## 6. Uji

`TesSesiPadaWaktu` — **7 lulus, 0 gagal**. Fixture meniru persis situasi nyata: kasir dengan sesi
20 Agustus (TUTUP) dan sesi 23 Agustus (BUKA, tiga hari kemudian).

| Kasus | Diharapkan |
|---|---|
| Transaksi 20 Agustus 13:37 (kasus nyata yang ditemukan) | diikat ke sesi 20 Agustus, **bukan** sesi aktif |
| Tepat di detik jam tutup sesi lama | masih terhitung milik sesi lama |
| Waktu di ANTARA dua sesi (tidak dicakup siapa pun) | `null` — bukan sembarang jatuh ke sesi terdekat |
| Transaksi dalam jam sesi yang sedang berjalan | diikat ke sesi itu (perilaku normal tak berubah) |
| Waktu sebelum sesi mana pun pernah dibuka | `null` (pemanggil jatuh ke sesi aktif, transaksi tetap diterima) |
| Waktu `null` | `null`, tidak melempar exception |

Uji ini terbatas pada `SesiKasUtil.sesiPadaWaktu` secara langsung, bukan `bayar()` penuh —
skema `koperasi` (toko, anggota, produk, cara bayar, konfigurasi wajib-sesi-kas) belum punya
harness fixture di sesi ini, dan menyusunnya dari nol berisiko lebih besar daripada nilainya
untuk perbaikan sesempit ini. Logika inti (kriteria pencarian sesi) sudah terbukti benar di
seluruh kasus tepi; penyambungannya ke `bayar()` diperiksa lewat pembacaan kode langsung —
`toko`, `idKasir`, dan `currentWaktu` sudah tersedia di lingkup yang sama sebelum perubahan ini.

---

## 7. Data produksi — 58 transaksi Agung dikoreksi, 24 Agustus 2026

Perbaikan kode di atas mencegah kejadian **berikutnya**; ia tidak membetulkan yang sudah
terjadi. Bagian ini awalnya berhenti di situ. Atas persetujuan eksplisit pemilik toko, 58
transaksi 20 Agustus milik kasir Agung yang tercemar ke sesi 23 Agustus (id 265) kemudian
dikoreksi langsung di produksi — dipindah ke sesi yang benar-benar mencakup waktu
transaksinya (id 222, sesi 20 Agustus).

**Bukan lewat aplikasi — SQL langsung di server, karena tidak ada jalur koreksi massal di
aplikasi untuk kasus ini.** Konsekuensinya: perubahan ini **tidak tercatat di jejak audit
Envers** (`new_audit.pembelian_anggota_koperasi__audit`), yang hanya menangkap perubahan
lewat siklus hidup Hibernate, bukan SQL mentah. Catatan di sini adalah satu-satunya jejak
permanennya.

Dikerjakan tiga langkah, masing-masing diverifikasi sebelum lanjut ke langkah berikutnya:

1. **Verifikasi pemetaan** — memastikan seluruh 58 baris (bukan sebagian) memetakan ke TEPAT
   SATU sesi tujuan sebelum menyentuh apa pun:
   ```sql
   SELECT (SELECT sk.id FROM koperasi.sesi_kas_kasir sk WHERE sk.kasir_nama = 'Agung'
             AND sk.toko = h.toko AND sk.waktubuka <= h.tanggal_pembayaran
             AND (sk.waktututup IS NULL OR sk.waktututup >= h.tanggal_pembayaran)
           ORDER BY sk.waktubuka DESC LIMIT 1) AS sesi_seharusnya,
          count(*), coalesce(sum(h.total_biaya),0)
   FROM koperasi.pembelian_anggota_koperasi h
   WHERE h.kasir_login_nama = 'Agung' AND h.sesi_kas_kasir = 265
     AND date(h.tanggal_pembayaran) = '2026-08-20'
   GROUP BY 1;
   -- hasil: sesi_seharusnya=222, count=58, total=2506000 -- persis prediksi bagian 3.
   ```
2. **Koreksi** — kriteria identik dipakai untuk `UPDATE` (sama persis logika
   `SesiKasUtil.sesiPadaWaktu`, supaya koreksi data lama konsisten dengan aturan yang kini
   berlaku untuk data baru):
   ```sql
   UPDATE koperasi.pembelian_anggota_koperasi h
   SET sesi_kas_kasir = (SELECT sk.id FROM koperasi.sesi_kas_kasir sk
                            WHERE sk.kasir_nama = 'Agung' AND sk.toko = h.toko
                              AND sk.waktubuka <= h.tanggal_pembayaran
                              AND (sk.waktututup IS NULL OR sk.waktututup >= h.tanggal_pembayaran)
                          ORDER BY sk.waktubuka DESC LIMIT 1)
   WHERE h.kasir_login_nama = 'Agung' AND h.sesi_kas_kasir = 265
     AND date(h.tanggal_pembayaran) = '2026-08-20';
   -- UPDATE 58
   ```
3. **Verifikasi hasil** — seluruh transaksi 20 Agustus Agung kini di bawah SATU sesi:
   ```sql
   SELECT sesi_kas_kasir, count(*), coalesce(sum(total_biaya),0)
   FROM koperasi.pembelian_anggota_koperasi
   WHERE kasir_login_nama = 'Agung' AND date(tanggal_pembayaran) = '2026-08-20'
   GROUP BY sesi_kas_kasir;
   -- hasil: satu baris -- sesi_kas_kasir=222, count=62, total=3148500.
   -- 62 = 58 yang dikoreksi + 4 yang sejak awal sudah benar tertaut ke sesi ini.
   ```

**Yang TIDAK berubah oleh koreksi ini**, dan penting dipahami:

- **Struk kertas 23 Agustus yang sudah dicetak dan diserahkan ke Agung** — snapshot
  `laporan_tutup_json` pada sesi 265 (dan juga sesi 222, yang sudah lebih dulu ditutup pada
  20 Agustus) adalah arsip beku pada saat masing-masing dicetak. Keduanya TIDAK ditulis
  ulang oleh koreksi ini. Struk 23 Agustus yang beredar secara fisik tetap menunjukkan
  selisih -Rp 1.411.000 seperti tercetak; itulah alasan dokumen 47 bagian 1-6 ada.
- **Dashboard "Laporan Kasir"** sudah benar SEBELUM koreksi ini (ia menyaring per tanggal
  transaksi sungguhan, bukan lewat `sesi_kas_kasir`) — koreksi ini tidak mengubah angka yang
  ditampilkannya untuk kedua tanggal tersebut, hanya merapikan data yang mendasarinya.
- **164 baris milik empat kasir lain** (Jannah, Rizal, Ziko, Rima) yang tercatat di bagian 6
  dengan pola serupa TIDAK ikut dikoreksi di sini — ini murni kasus Agung, atas permintaan
  eksplisit yang menyebut nama kasirnya. Investigasi kasus Jannah sendiri kemudian menemukan
  mekanisme yang BERBEDA (tabrakan id lintas tabel draft/transaksi final lewat fitur "Tahan
  Keranjang" di `keranjang_screen.dart`, bukan kode sesi tak dikenal) — didokumentasikan
  terpisah bila perbaikannya dikerjakan.
