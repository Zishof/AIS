# 25 — Jurnal saat BAST: satu mesin, dua baris dasbor

Dua baris terakhir rantai pengadaan: **Fix Aset (Jurnal Saat BAST)** dan **Aset dalam
Pekerjaan (Jurnal Saat BAST)**. Keduanya berbagi satu dokumen
(`PenerimaanPengadaanMasterAsset`) dan satu mesin ZK — layar memposting keduanya sekaligus.
Dasbor Draft Jurnal memecahnya menjadi dua baris memakai kelompok asetnya.

Dengan ini rantai pengadaan lengkap dari ujung ke ujung di POS:
**PR → PO → DP → BAST → Tagihan → Pekerjaan/Termin → Jurnal Balik.**

Lima belas dari 35 baris Draft Jurnal punya mesin posting sebelum ini; sekarang **tujuh belas**.

---

## 1. Kalau kelompoknya tidak dipisah, angkanya berbohong

Mesin ZK tidak mengenal pemisahan itu. Kalau jalur API ikut begitu, menekan tombol pada baris
**Fix Aset** akan memposting dokumen milik baris **Aset dalam Pekerjaan** juga — dan sebaliknya.
Angka yang ditampilkan tidak akan pernah cocok dengan yang dikerjakan.

Karena itu `postingSemua` di sini menerima parameter kelompok:

```java
public static int postingSemua(String kelompok, Date mulai, Date sampai, Tbmuser oleh, Date tglPosting)
```

Penyaringnya `SQL_KELOMPOK_CIP`, disalin apa adanya dari `DraftJurnalRingkasanUtil` supaya
kedua tempat tidak dapat berbeda diam-diam. `KELOMPOK_PEKERJAAN` memakai penyaring itu,
`KELOMPOK_FIX_ASSET` memakai negasinya.

Jurnalnya **bersisi banyak**: satu baris debet per detail penerimaan, masing-masing pada
akunnya sendiri sebesar `getHargaTotal()`, dilawan **satu** baris kredit ke
`jenisPenerimaanBarang.akun` sebesar totalnya.

Akun debet tiap detail dicari berurutan, dari yang paling khusus ke yang paling umum:

1. akun transaksi master asetnya;
2. akun pada permintaan pengadaannya;
3. akun biaya penyusutan — **hanya** untuk aset yang bukan aset tetap.

---

## 2. `ambilDataAkun` menutup sesi pemanggilnya

Ini yang paling memakan waktu, dan gejalanya menyesatkan.

`AssetUtil.ambilDataAkun` **membuka dan menutup sesi Hibernate sendiri**. Sesi yang dipegang
pemanggil menjadi basi, dan transaksi berikutnya gagal:

```
org.hibernate.SessionException: Session is closed!
	at ...PostingSaldoAwalMasterAssetDetailAction.postingSemua
```

> **Dikoreksi 24 Agustus 2026.** Yang menutup sesi adalah `AssetUtil.ambilDataAkun`
> **sendiri**, bukan `ConstantValues.ambil` yang dipanggilnya. Dibuktikan langsung: setelah
> `ambilDataAkun`, `session.isOpen()` berubah dari `true` menjadi `false`; setelah
> `ConstantValues.ambil` sesi tetap terbuka. Perbedaannya penting — memperlakukan keduanya
> sama menghasilkan temuan palsu, dan itu sempat terjadi pada satu pemindaian di sini.

Percobaan pertama membuat satu dokumen berjurnal dan satu lagi tidak, tanpa pesan apa pun di
layar. Kalau ini lolos ke produksi, gejalanya adalah "kadang jalan, kadang tidak" — jenis
laporan bug yang paling mahal ditelusuri.

Perbaikannya memisahkan tegas dua fase:

| Fase | Yang boleh dilakukan |
|---|---|
| **1 — mengumpulkan bahan** | menelusuri entitas, memanggil `ambilDataAkun`. Yang dibawa keluar **hanya id dan angka**, tidak satu pun referensi entitas. |
| **2 — menulis jurnal** | sesi diambil ULANG, seluruh entitas dimuat kembali di dalamnya (dokumen, riwayat posting, akun, satuan kerja), baru transaksinya dibuka. |

Memakai objek fase 1 di fase 2 berakhir `Session is closed!` atau — lebih buruk —
`NonUniqueObjectException` karena entitas yang sama masuk ke sesi dua kali. Keduanya sempat
muncul di `error_log` selama pengerjaan ini.

> **Pelajaran yang berlaku umum:** di basis kode ini, **memanggil utilitas berarti sesi Anda
> mungkin sudah mati sesudahnya.** Sesi yang dipegang melintasi pemanggilan utilitas tidak
> dapat dipercaya. Pola dua fase ini layak dipakai ulang di mesin posting mana pun yang
> memanggil `AssetUtil.ambilDataAkun`. Seluruh 51 kelas `Posting*` dipindai pada 24 Agustus
> 2026 dan **tidak ada satu pun** yang memegang sesi melintasi pemanggilan itu.

---

## 3. Satu hal yang sengaja BERBEDA dari layar

Layar memasukkan akun debet ke dalam larik **meski nilainya null**, lalu hanya memeriksa larik
itu tidak kosong sebelum menjurnal. Di sini dokumen yang salah satu detailnya belum berakun
**dilewati seluruhnya**.

Melewati barisnya saja tidak dapat dibenarkan: nilai kreditnya adalah jumlah **seluruh** detail,
jadi menghilangkan satu debet menghasilkan jurnal yang tidak seimbang — kesalahan yang jauh
lebih mahal daripada dokumen yang tertunda.

Berlaku juga tiga perbedaan yang sama dengan dokumen 23 dan 24: riwayat posting dibuat setelah
dipastikan ada yang perlu dikerjakan, penanda ditulis lewat SQL lalu entitasnya di-`evict`, dan
baris `akunting.transaksi` dihapus lebih dulu saat pembatalan. Jurnal yang sudah closing tidak
pernah ikut terhapus.

Pembatalan **tidak** menyaring `ref`, dan memang tidak boleh: modul inilah satu-satunya yang
menulis jurnal bertaut `penerimaan_pengadaan_master_asset`, dan jurnalnya ber-`ref` null.

---

## 4. Kriteria yang sempat lebih longgar daripada dasbor

Versi pertama `kriteriaPostingStatic` saya lupa satu baris:

```java
.createAlias("pemesananPengadaanMasterAsset", "pemesananPengadaanMasterAsset")
```

Itu **INNER JOIN**, bukan hiasan. Layar ZK dan dasbor keduanya memakainya, sehingga BAST tanpa
pemesanan tidak pernah ikut dihitung. Tanpa alias itu jalur API akan memposting dokumen yang
tidak pernah muncul di angka mana pun — persis kebalikan dari cacat "terhitung tetapi mustahil"
pada dokumen 24, dan sama tidak dapat diterimanya.

Kunci menunya `pengadaan_bast` untuk kedua baris, mengikuti dokumennya. Kunci itu ada di
`KUNCI_CRUD`, jadi hak `create`-nya dapat dibatasi admin per peran.

---

## 5. Uji

`TesPostingBast` — **21 lulus, 0 gagal**.

UAT sudah punya **empat BAST sungguhan** yang belum diposting (21 Agustus 2026). Data itu bukan
milik harness, jadi seluruh dokumen uji bertanggal **2013-07-07** — tanggal yang diverifikasi
kosong lebih dulu — dan tiap pemanggilan memakai rentang sehari itu saja. Salah satu kasus uji
memeriksa langsung bahwa keempat dokumen itu **tetap belum diposting** sesudahnya.

Yang diuji selain jalur bahagianya:

- memposting **Fix Aset** tidak menyentuh dokumen **CIP**, baik jurnalnya maupun penandanya —
  inilah alasan parameter kelompok itu ada;
- membatalkan salah satu tidak menghapus jurnal satunya;
- debet mendarat di akun transaksi master asetnya, kredit di akun jenis penerimaan barang;
- tidak ada baris `akunting.transaksi` yang tertinggal yatim;
- rentang tanpa dokumen tidak meninggalkan riwayat posting kosong.

---

## 6. Yang masih terbuka

Tujuh belas dari 35 baris kini punya mesin posting; tiga baris lain memang bukan posting massal
(Jurnal Umum dan Posting HPP punya layarnya sendiri, Closing adalah penguncian). Sisa **15 baris**:

| Baris | Jumlah |
|---|---|
| Mahasiswa | 7 |
| Siswa | 6 |
| Jurnal Penyusutan | 1 |
| Gaji | 1 |

Dan seperti dua dokumen sebelumnya: **jurnal yang dihasilkan belum pernah diperiksa akuntan.**
Yang terbukti adalah akunnya sesuai rantai yang disalin dari layar dan angkanya dihitung dengan
rumus yang sama — bukan bahwa perlakuan akuntansinya tepat.
