# 80 — Satu saluran peringatan pasca-transaksi

Rekomendasi penutup [79](79-enam-belas-utang-ditelusuri.md), dikerjakan.

Penelusuran itu menemukan **lima peringatan yang tidak sampai ke siapa pun**, semuanya
berbentuk sama: *transaksi DITERIMA, tetapi ada sesuatu yang perlu direkonsiliasi
belakangan.* Salah satunya membawa niat yang tertulis di komentar kodenya sendiri —
"dikembalikan ke klien supaya bagian keuangan punya jejaknya" — dan niat itu tidak pernah
terwujud.

---

## 1. Kenapa memperbaikinya satu per satu justru salah

Sebabnya struktural, bukan kelalaian. Sebelum ini tiap peringatan adalah **field lepas**,
dan tiap field baru menuntut **lima titik klien** disentuh:

| Kanal | Titik |
|---|---|
| Flutter | bayar massal, bayar tunggal, penyimpanan outbox, layar struk |
| JSP | bayar manual, 2× "Bayar Semua", 2× Verifikasi Otomatis |

Menyambungkan lima field berarti mengulang pekerjaan itu lima kali — dan peringatan
**ketujuh** akan terlupa dengan cara yang sama persis. Yang perlu diperbaiki bukan kelima
peringatannya, melainkan bentuk yang membuat mereka mudah terlupa.

---

## 2. Bentuknya

### 2.1 Server: satu method, satu daftar

```java
static void tambahPeringatanTransaksi(JSONObject hasil, String kode, String pesan)
```

Menitipkan `{kode, pesan}` ke satu larik `peringatanTransaksi`. Lima pemanggil:

| Kode | Keadaan |
|---|---|
| `STOK_KURANG` | saldo stok tidak mencukupi, transaksi tetap dicatat |
| `SESI_KAS_SUDAH_TUTUP` | transaksi masuk ke sesi kas yang sudah ditutup |
| `SESI_KAS_TIDAK_DIKENAL` | kode sesi dari perangkat tidak dikenal server |
| `SESI_KAS_DIREKONSILIASI` | transaksi diikat ke sesi yang benar-benar terbuka saat itu |
| `PERSETUJUAN_LIMIT_PERLU_REKONSILIASI` | penandaan pemakaian persetujuan gagal |

**Pesannya wajib lengkap sendiri** — memuat kode sesi/nama produk di dalam kalimatnya —
supaya klien tidak perlu merakit ulang apa pun. Itulah sebabnya `sesi_kas_asal` tidak perlu
lagi dikirim terpisah: kode sesinya kini ada di dalam kalimat.

### 2.2 Klien: satu pembaca per kanal

| Kanal | Pembaca |
|---|---|
| Flutter | `lib/services/peringatan_transaksi.dart` — `PeringatanTransaksi.dari()` |
| JSP | `window.peringatanTransaksi<rnd>()` |

Empat titik Flutter dan lima titik JSP memanggil pembaca itu. **Peringatan berikutnya tidak
menuntut satu baris pun diubah di kesembilan titik itu** — cukup satu pemanggilan di server.

### 2.3 Satu field lama sengaja dipertahankan

`peringatanStok` **tetap dikirim**, berbeda dari lima yang lain yang dipindahkan seluruhnya.
Alasannya membentuk garis yang jelas:

> **Yang terbukti tidak punya pembaca boleh diganti; yang punya pembaca di lapangan tetap
> dipertahankan.**

`peringatanStok` sudah punya pembaca yang terpasang di perangkat kasir sejak
[76](76-peringatan-stok-terbaca-dan-nota-terparkir.md). Menghapusnya membuat aplikasi yang
belum diperbarui kehilangan peringatannya. Kelima yang lain tidak punya pembaca sama sekali
— menghapusnya tidak mungkin merugikan siapa pun, **secara konstruksi**.

Nilainya dirakit **sekali** lalu dipakai keduanya, jadi saluran baru dan field lama tidak
mungkin berbunyi berbeda — pelajaran yang sama dengan `kondisiMetodeBayar` di
[45](45-penyaring-dasbor-dan-layani-semua.md) dan `alasanTanpaRincian` di
[46](46-angka-tanpa-rincian.md).

Kedua pembaca klien mendahulukan saluran baru dan hanya jatuh ke field lama bila salurannya
kosong — kalau tidak, peringatan stok akan tampil **dua kali**.

---

## 3. Berkas yang berubah

| Berkas | Perubahan |
|---|---|
| `ais/action/servlet/api/KantinHelper.java` | `tambahPeringatanTransaksi()` + 5 konstanta; lima pemanggil menggantikan enam `hasil.put` lepas |
| `modul/kantin/pesanan/_draft_pesanan_anggota.jsp` | `peringatanTransaksi<rnd>()` + 5 titik memanggilnya |
| `apps/ebisnis/lib/services/peringatan_transaksi.dart` | **baru** — pembaca tunggal Flutter |
| `apps/ebisnis/lib/screens/pesanan_screen.dart` | 2 titik memakai pembaca bersama |
| `apps/ebisnis/lib/screens/struk_screen.dart` | membaca daftar yang sudah diratakan outbox |
| `apps/ebisnis/lib/services/transaksi_outbox_service.dart` | menyimpan daftar kalimat, bukan satu string |
| `apps/ebisnis/test/peringatan_transaksi_test.dart` | **baru** |

---

## 4. Hasil uji

### 4.1 Penjaganya sendiri yang membuktikan utangnya lunas

Ini bagian yang paling meyakinkan, karena bukan saya yang menyatakannya.
[`alat/field-tanpa-pembaca.py`](alat/field-tanpa-pembaca.py) menolak hijau dan **menyebut
kelima nama itu satu per satu**:

```
== Utang yang sudah LUNAS tetapi masih terdaftar ==
   - peringatanPengajuanLimit  <- keluarkan dari daftar UTANG
   - sesi_kas_asal             <- keluarkan dari daftar UTANG
   - sesi_kas_direkonsiliasi   <- keluarkan dari daftar UTANG
   - sesi_kas_sudah_tutup      <- keluarkan dari daftar UTANG
   - sesi_kas_tidak_dikenal    <- keluarkan dari daftar UTANG

PERIKSA LAGI: 0 yatim baru, 5 utang basi
```

Pemeriksaan "utang hanya boleh mengecil" yang dipasang di
[77](77-gerbang-oversell-dan-penjaga-field-yatim.md) — yang saat itu terasa seperti
kehati-hatian berlebihan — justru inilah gunanya: **ia yang memverifikasi pelunasannya,
bukan pernyataan saya.**

Sesudah daftarnya dibersihkan: **field terkirim 274 → 270, utang 16 → 11, BERSIH.**

### 4.2 Uji lain

| Uji | Hasil |
|---|---|
| `peringatan_transaksi_test.dart` | **10/10** — baru |
| Regresi Flutter penuh | **645 lulus** |
| `PesananPayloadKontrakUat` | **16/16** (sesudah disesuaikan, lihat 4.3) |
| `alat/payload-tanpa-pembaca.py` | BERSIH |
| Sintaks JS JSP (`node --check`) | BERSIH, 1981 baris |

Uji Flutter yang baru sengaja menguji hal-hal yang **tidak mungkin** dilakukan bentuk lama:
dua peringatan berbeda tampil bersama (satu field per jenis tidak pernah bisa), saluran baru
menang atas field lama supaya tidak ganda, dan bentuk respons yang rusak tidak melempar —
transaksinya sendiri sudah tersimpan, strukanya tidak boleh gagal tampil karenanya.

### 4.3 Uji kontrak lama ikut menangkap perubahan ini

`PesananPayloadKontrakUat` sempat **2 GAGAL**: ia menuntut `peringatanStok` dibaca di lima titik
JSP, padahal kini dibaca sekali saja sebagai cadangan di dalam pembaca bersama.

Ujinya diperbarui, dan yang dijaga **bergeser** — dari "berapa titik membaca field itu"
menjadi "apakah ada TEPAT SATU pembaca bersama, dan apakah tiap jalur memanggilnya".
Bentuk baru itu justru lebih tepat: ia menolak logika yang diulang per jalur, yaitu keadaan
yang membuat lima peringatan terlupa sejak awal.

### 4.4 Yang BELUM diuji

- **Peringatan sesi kas dengan data sungguhan.** Yang diuji adalah pembacanya dan bentuk
  kodenya; memicu "sesi kas sudah ditutup" menuntut transaksi terlambat pada sesi tertutup
  di basis data, dan kredensial UAT masih ditolak
  ([73](73-stok-minus-tiga-nilai-dan-pemulihan-member.md) bagian 4.2).
- **Tampilannya di layar** belum dilihat mata.

---

## 5. Yang perlu diperiksa lain kali

Enam field lepas menjadi satu saluran, dan yang berubah bukan hanya jumlah barisnya:
**biaya menambah peringatan berikutnya turun dari sembilan titik menjadi satu.**

Itu ukuran yang lebih berguna daripada "sudah diperbaiki". Cacat berulang lima kali di
dokumen 45–79 semuanya punya bentuk yang sama — informasi yang menyeberang antar kanal
lewat jalur yang harus dirakit ulang setiap kali. Selama merakitnya mahal, ia akan terlupa
lagi; ketika merakitnya gratis, tidak ada yang perlu diingat.

Pertanyaannya untuk berikutnya bukan *"apakah field ini sudah dibaca?"* — dua penjaga sudah
menjawabnya otomatis — melainkan **"berapa titik yang harus disentuh untuk menambah satu
lagi?"** Jika jawabannya lebih dari satu, bentuknya yang perlu diperbaiki, bukan isinya.
