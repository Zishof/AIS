# 90 — Angka cakupan yang salah, dan 68 gerbang yang ternyata terjangkau

Tanggal: 2026-09-02

Dok. 89 menutup dengan satu batas yang dinyatakan terus terang:

> **113 pemanggilan berkunci VARIABEL — di luar jangkauan alat ini.**

Angka itu salah, dan salahnya ke arah yang paling tidak enak: ia membuat alatnya
tampak jauh lebih lemah daripada yang sebenarnya, sekaligus menyembunyikan bahwa
mayoritas gerbang memang dapat diperiksa.

## 1. Dari 113 menjadi 30

Menghitung ulang bentuk tiap argumen memberi jawabannya:

| Bentuk | Jumlah | Nasib |
|---|---|---|
| `String kunciMenu` | 17 | **bukan pemanggilan** — itu deklarasi methodnya sendiri |
| `KUNCI_BAST`, `KUNCI_PO`, … | 68 | konstanta kelas, dapat diresolusi |
| `kunci`, `kunciMenu`, `kunciModul…` | 28 | parameter pass-through dari pemanggil di berkas lain |
| `"posting_" + jenis` | 2 | dirakit saat berjalan |

Dua sebabnya:

**Pola pemanggilan juga cocok dengan deklarasi.** `boolean bolehAksi(Tbmuser
tbmuser, String kunciMenu, String aksi)` memuat `bolehAksi(` persis seperti
sebuah panggilan, dan argumen pada posisi kunci menjadi teks `"String
kunciMenu"` — bukan kunci variabel. Rentang deklarasi kini dikecualikan.

**Konstanta kelas tidak pernah dicoba diresolusi.** `PengadaanPosApiHelper`
menaruh kuncinya di delapan konstanta lalu meneruskannya ke gerbang:

```java
private static final String KUNCI_BAST = "pengadaan_bast";
```

Membaca deklarasi konstanta di kelas yang sama menyelesaikan **68 pemanggilan**
sekaligus — dari satu pola regex.

Cakupan sebenarnya: **39 kunci terresolusi, 30 tersisa** — bukan 31 lawan 113.

## 2. Hasilnya bersih

Kedelapan kunci pengadaan (`pengadaan_pr`, `po`, `bast`, `tagihan`, `dpc`,
`bdp`, `sinkron`, `pajak`) semuanya sudah terdaftar di katalog dan di
`KUNCI_CRUD`. Tidak ada cacat baru seperti `returpembelian` di dok. 89.

Itu jawaban yang berharga: 68 gerbang yang kemarin dinyatakan "tak terperiksa"
kini terbukti benar, bukan sekadar tidak diketahui.

## 3. Jalur baru dibuktikan hidup

Menambahkan resolusi konstanta tidak ada gunanya kalau ia tidak benar-benar
menyalurkan nilainya ke pemeriksaan. Kontrol negatifnya menyentuh konstantanya,
bukan katalognya:

```
KUNCI_BAST = "pengadaan_bast"  ->  "pengadaan_bast_uji"
```

```
== 1. Kunci gerbang yang TIDAK ADA di katalog ==
   - pengadaan_bast_uji   (PengadaanPosApiHelper.java, ...)

== 2. Kunci beraksi CRUD yang TIDAK ADA di KUNCI_CRUD ==
   - pengadaan_bast_uji       aksi: approve, delete, update

2 PELANGGARAN
```

Kedua invarian menyala dari satu perubahan konstanta, membuktikan nilainya
memang mengalir. `svn revert`, status kosong, alatnya kembali bersih.

## 4. Yang benar-benar tersisa

**28 parameter pass-through.** `ApotikApiDispatcher` memanggil
`bolehAksiMenu(tbmuser, kunci, "create")` dengan `kunci` datang dari
pemanggilnya. Menelusurinya menuntut analisis antar-method dan antar-berkas —
dan salah telusur di sini menghasilkan tuduhan palsu atas gerbang keamanan,
harga yang jauh lebih mahal daripada satu kunci yang lolos.

**2 kunci rakitan.** `"posting_" + jenis` tidak dapat diselesaikan tanpa
mengetahui nilai `jenis` saat berjalan.

Keduanya tertulis di kepala alatnya, bukan hanya di sini.

## 5. Yang dipelajari

**Batas yang dinyatakan pun perlu diukur.** Dok. 89 menyebut angka 113 sebagai
tanda kejujuran — "ini yang tidak saya jangkau". Tetapi angka yang tidak
diperiksa tetaplah klaim, bahkan ketika ia klaim tentang kelemahan sendiri.
Menyatakan batas dengan angka yang salah bukan kehati-hatian; ia hanya tampak
begitu.

**Kelemahan yang diakui cenderung tidak ditinjau ulang.** Justru karena 113
sudah tertulis sebagai batas yang jujur, ia nyaris tidak diperiksa lagi. Satu
survei bentuk argumen — sepuluh menit — memindahkan 68 pemanggilan dari "tak
terjangkau" ke "terbukti benar".
