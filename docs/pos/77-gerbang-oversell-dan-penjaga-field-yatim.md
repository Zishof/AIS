# 77 — Gerbang yang tidak ada di kode, dan penjaga field yatim

Batch lanjutan sesudah [73](73-stok-minus-tiga-nilai-dan-pemulihan-member.md),
[75](75-halaman-pesanan-tiga-celah-sunyi.md), dan
[76](76-peringatan-stok-terbaca-dan-nota-terparkir.md). Dua pekerjaan:

- **D** — sakelar "Cegah Oversell Kasir" yang didokumentasikan tetapi tidak pernah dibaca,
  dan defaultnya yang bertengkar di dua tempat.
- **E** — penjaga otomatis untuk cacat yang sudah berulang **empat kali**: field yang
  dikirim satu sisi dan tidak dibaca sisi mana pun. Celah ini ditulis sebagai "yang masih
  terbuka" pada [76](76-peringatan-stok-terbaca-dan-nota-terparkir.md) bagian 6.

---

## 1. D — sakelar yang ternyata sudah punya keputusan

Kemarin ini saya tunda sebagai "keputusan produk". Ternyata keputusannya **sudah ada**,
tertulis pada label layar Konfigurasi sendiri (`KonfigurasiNewAction`:983):

> "Cegah Oversell Kasir — blokir penambahan item melebihi stok pada POS **(default MATI**;
> aktifkan setelah toko rutin mencatat stok masuk lewat Pengadaan/Stok Opname, **jika belum
> akan memblokir seluruh penjualan produk yang stoknya belum pernah tercatat**)"

Label itu bahkan **memperingatkan persis insiden yang terjadi** — "jika belum akan
memblokir seluruh penjualan produk yang stoknya belum pernah tercatat" adalah uraian
kejadian Nasi Koloke, ditulis jauh sebelum kejadiannya.

Jadi yang perlu diputuskan bukan kebijakannya, melainkan menyelaraskan kode dengan
kebijakan yang sudah tertulis.

### 1.1 Dua cacat

| | Cacat | Akibat |
|---|---|---|
| D1 | `PosKantinAction` memakai default **AKTIF** | Satu sakelar, dua arti. Instalasi yang belum pernah menyimpan barisnya memblokir di POS ZK tetapi meloloskan di JSP/Desktop/Android |
| D2 | JavaDoc `validasiStokCukupDenganLock` menjanjikan gerbang konfigurasi yang **badan methodnya tidak pernah baca** | Menyalakan/mematikan "Cegah Oversell Kasir" tidak berpengaruh apa pun pada jalur API |

D2 adalah kebalikan dari cacat-cacat sebelumnya: bukan kode yang mengabaikan dokumentasi,
melainkan **dokumentasi yang menjanjikan lebih daripada kodenya**. Sama berbahayanya —
orang membaca JavaDoc, percaya gerbangnya ada, dan mencari sebab masalah di tempat lain.

### 1.2 Bentuk gerbangnya

```java
static boolean wajibDiblokirKarenaStok(Boolean izinkanJualMinusStok,
        boolean cegahOversellAktif) {
    if (Boolean.FALSE.equals(izinkanJualMinusStok)) return true;   // dikunci admin
    if (Boolean.TRUE.equals(izinkanJualMinusStok)) return false;   // selalu boleh
    return cegahOversellAktif;                                     // "Ikut Pengaturan"
}
```

Override per-produk **menang atas sakelar pada kedua arah**:

| Nilai produk | Sakelar MATI | Sakelar AKTIF |
|---|---|---|
| `FALSE` "Wajib Diblokir" | blokir | blokir |
| `TRUE` "Selalu Boleh" | lolos | lolos |
| `null` "Ikut Pengaturan" | lolos | **blokir** |

Itu penting: barang mahal/gampang basi yang sengaja dikunci admin tidak boleh ikut terbuka
hanya karena kebijakan umum toko dilonggarkan — dan sebaliknya, jasa/produk tanpa
pelacakan stok tidak boleh ikut terblokir saat kebijakan umum diketatkan.

### 1.3 Hasil yang tidak diduga: perilaku lama = "sakelar AKTIF"

Rumus r77493 yang keliru (`!Boolean.TRUE.equals(...)`) memblokir `null` **dan** `FALSE`.
Itu persis arti "cegah oversell menyala".

Jadi perilaku lama tidak hilang ke mana-mana — **ia sekarang punya sakelar**, dan
sakelarnya MATI secara bawaan sesuai keputusan 20-07-2026. Toko yang memang menginginkan
blokir keras tinggal menyalakannya, sadar dan sengaja.

---

## 2. E — penjaga field yatim lintas kanal

Cacat yang sama sudah muncul **empat kali** dengan wajah berbeda:

| Dok. | Yang mengirim | Yang tidak membaca |
|---|---|---|
| [45](45-penyaring-dasbor-dan-layani-semua.md) | klien (`metodeBayar`) | server |
| [46](46-angka-tanpa-rincian.md) | server (`bisaRincian`) | klien Flutter |
| [75](75-halaman-pesanan-tiga-celah-sunyi.md) | server (`peringatanStok`) | JSP |
| [76](76-peringatan-stok-terbaca-dan-nota-terparkir.md) | server (`peringatanStok`) | klien Flutter |

Yang terakhir terjadi **sehari** setelah pelajarannya ditulis, oleh tangan yang menulisnya.
Pada titik itu jelas bahwa mengandalkan kewaspadaan tidak cukup.

`TesFieldTanpaPembaca` menyapu `hasil.put("...")` pada helper API, lalu mencari nama itu di
**seluruh** sumber kanal klien (semua modul JSP, semua aplikasi Flutter, semua paket) DAN di
lapisan servlet server — rantainya bisa server → server → klien, lihat bagian 2.2.

### 2.1 Dua daftar, dan bedanya penting

| Daftar | Isi | Sifat |
|---|---|---|
| `DIIZINKAN` | field yang memang WAJAR tidak dibaca per nama — `status`, `description`, `teknis`, dsb., yang ditangani lapisan umum | tetap |
| `UTANG_BELUM_DITELUSURI` | **16** field yatim yang sudah ada sebelum penjaga ini dipasang | **hanya boleh mengecil** |

Namanya sengaja *utang*, bukan "diizinkan". Tujuan penjaga ini bukan membereskan 16 field
sekaligus — melainkan **mencegah yang ke-17**.

Ada pemeriksaan tersendiri yang menolak nama pada daftar utang yang ternyata **sudah**
punya pembaca. Jadi begitu satu utang dilunasi, namanya wajib dikeluarkan dan daftarnya
tidak bisa membusuk diam-diam menjadi daftar yang tidak lagi benar.

### 2.2 Koreksi: temuan pertama penjaga ini ternyata salah

Versi pertama dokumen ini menyebut tiga field persetujuan limit member
(`memerlukanPersetujuanLimit`, `pengajuanLimitId`, `peringatanPengajuanLimit`) sebagai
"paling patut ditelusuri", dengan dugaan bahwa server menuntut persetujuan limit dan tidak
ada layar yang menyampaikannya.

**Dugaan itu keliru.** Penelusurannya menemukan rantai yang utuh:

```
KantinHelper.bayar        status 92 + memerlukanPersetujuanLimit
        ↓
PosApi:3075               membacanya -> kode galat PENGAJUAN_LIMIT_MENUNGGU
        ↓
keranjang_screen.dart:1761  menangani kode itu
```

lengkap dengan dua uji kontrak tersendiri (`pengajuan_limit_member_kontrak_test.dart`,
`member_pin_limit_uat_test.dart`). Fiturnya bekerja.

Yang salah adalah penjaganya: ia hanya memindai kanal **klien**, padahal rantainya bisa
**server → server → klien**. Sesudah lapisan servlet ikut dihitung sebagai pembaca, utang
turun **37 → 16**; 22 dari 37 "yatim" itu positif palsu.

Cakupan pembaca sisi server sengaja dibatasi pada lapisan `ais/action/servlet` saja, bukan
seluruh pohon `ais/`. Memindai semuanya (105 MB) membuat nama field bertabrakan dengan
variabel lokal mana pun yang kebetulan bernama sama. Selisihnya kecil dan terukur: seluruh
pohon menghasilkan 15, lapisan servlet 16 — hanya `totalGrup` yang berbeda.

### 2.3 Apa isi 16 yang tersisa

| Kelompok | Field | Sifat |
|---|---|---|
| Peringatan pasca-transaksi | `peringatanPengajuanLimit` | **satu-satunya yang berbentuk sama dengan `peringatanStok` sebelum dok. 76** |
| Sesi kas | `sesi_kas_tidak_dikenal`, `sesi_kas_direkonsiliasi`, `sesi_kas_sudah_tutup`, `sesi_kas_asal`, `idSesiKas` | penanda diagnostik rekonsiliasi |
| Id/angka pendamping | `pengajuanLimitId`, `satuanDasarId`, `waktuHargaBeliTerakhir`, `versiStok` | klien memakai jalur lain |
| Ringkasan proses massal | `draftDiperbarui`, `ringkasanBerjalan`, `siapDisimpan`, `totalGrup`, `totalTerjualBersih`, `statusRincian` | angka hasil batch |

`versiStok` sempat dicurigai penjaga *lost-update* karena namanya. Diperiksa: isinya hanya
`so.getId()` — id baris stok opname yang juga sudah dikirim sebagai `id`. Nama yang
menyesatkan, bukan bahaya.

**`peringatanPengajuanLimit` sengaja DIBIARKAN sebagai utang, tidak dipindah ke daftar
izin.** Ia disetel ketika penandaan "persetujuan limit sudah dipakai" gagal sesudah
transaksi final — persis bentuk `peringatanStok` sebelum dok. 76. Alasan belum dibayar:
kejadiannya jarang, isinya sudah tercatat permanen di Error Log server lewat
`ErrorAuditUtil` berikut jejak tumpukannya, dan yang perlu menindaklanjuti adalah
administrator/keuangan, bukan kasir di depan layar.

Itu keputusan yang bisa saja salah. Karena itu ia tetap terdaftar sebagai utang yang
terlihat — memindahkannya ke daftar izin hanya supaya penjaganya hijau adalah persis cara
sebuah baseline berubah menjadi tempat sampah.

## 3. Berkas yang berubah

| Berkas | Perubahan |
|---|---|
| `ais/action/servlet/api/KantinHelper.java` | `wajibDiblokirKarenaStok(Boolean, boolean)`; gerbang `KANTIN_POS_CEGAH_OVERSELL` dibaca; JavaDoc jujur |
| `ais/action/master/koperasi/PosKantinAction.java` | default disamakan jadi MATI |

---

## 4. Hasil uji

### 4.1 `TesAturanStokMinus` — 13 dari 13 lulus (tanpa basis data)

Bagian 5-nya yang paling berguna: membuktikan **sakelar AKTIF = persis rumus r77493**
untuk ketiga nilai. Jadi klaim "perilaku lama tidak hilang, hanya diberi sakelar" bukan
kalimat penenang — ia diperiksa.

Bagian 4 tetap membandingkan rumus lama vs baru berdampingan dan menuntut **tepat satu**
nilai berubah pada default.

### 4.2 `TesFieldTanpaPembaca` — 12 dari 12 lulus

```
sumber klien terbaca      : 37.732.318 karakter
sumber server (pembaca)   :  7.374.106 karakter
field yang dikirim server : 274
Utang lama (dibekukan)    : 16 field
Yatim BARU                : (tidak ada) -- inilah yang dijaga uji ini
```

**Penjaganya dibuktikan menyala dengan kontrol negatif sungguhan**, bukan sekadar nama
karangan: sebuah `hasil.put("uatSengajaTanpaPembaca", true)` benar-benar disisipkan ke
`DraftJurnalApiHelper`, uji dijalankan, dan hasilnya

```
   - uatSengajaTanpaPembaca
  GAGAL tidak ada field baru yang dikirim server tanpa pembaca (1 ditemukan)
```

lalu berkasnya dikembalikan dan uji kembali hijau. Penjaga yang belum pernah terbukti
bisa gagal tidak layak dipakai sebagai bukti.

### 4.3 Cakupan pindaian sempat salah — dan begitu ketahuannya

Versi pertama memakai akar sempit (`modul/kantin` + `apps/ebisnis/lib`) dan melaporkan 39
yatim. Pemeriksaan silang dengan `grep` shell menemukan `memerlukanPersetujuanLimit` di
6 berkas `apps/`, yang tampak seperti tuduhan palsu.

Ternyata **grep shell itu** yang keliru: keenam berkas ada di direktori `build/`
(artefak build), yang `ripgrep` lewati karena menghormati `.gitignore`. Jadi field itu
memang yatim.

Cakupannya tetap diperluas ke seluruh kanal — 9,5 juta → 37,7 juta karakter — dan
perluasan itu memang menghapus satu tuduhan palsu yang nyata (`id_server`). Direktori
`build`, `.dart_tool`, dan `test` dilewati: berkas uji yang menyebut nama field tidak
membuat field itu sampai ke mata pengguna mana pun.

### 4.4 Yang BELUM diuji

- **Gerbang oversell dengan sakelar benar-benar AKTIF di basis data.** Yang diuji adalah
  aturannya sebagai fungsi murni; pembacaan konfigurasinya sendiri belum dijalankan
  (kredensial UAT masih ditolak, lihat [73](73-stok-minus-tiga-nilai-dan-pemulihan-member.md)
  bagian 4.2).
- **16 utang field yatim** belum ditelusuri satu per satu; yang sudah diperiksa hanya
  `peringatanPengajuanLimit` dan `versiStok` (bagian 2.3).

---

## 5. Yang perlu diperiksa lain kali

Empat kali berturut-turut cacat yang sama, dan yang keempat terjadi sehari setelah
pelajarannya ditulis. Itu bukan soal kurang teliti — itu tanda bahwa **kewaspadaan bukan
mekanisme**. Selama satu-satunya penjaga adalah ingatan orang yang sedang menyunting,
kanal ketiga akan terus terlewat.

Bentuk penjagaan yang terbukti bekerja di sini semuanya punya sifat yang sama: **menghitung
sesuatu, bukan mendaftar sesuatu.**

- `jumlah id_member == jumlah action:"bayar"` ([75](75-halaman-pesanan-tiga-celah-sunyi.md))
  menangkap titik panggil keenam yang belum ada hari ini.
- `setiap hasil.put wajib punya pembaca` menangkap field ke-38 yang belum ditulis siapa pun.

Daftar yang ditulis tangan hanya menangkap yang sudah diketahui — dan yang sudah diketahui
bukan itu yang membuat kita rugi.
