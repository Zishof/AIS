# Lepas dari ZUL: empat laporan akuntansi jadi berbasis API

Permintaan: **semua berbasis API, tidak ada pemanggilan `*.zul` di POS Desktop atau POS
Android.** Dokumen ini mencatat inventaris lengkap titik keluar ZK, empat yang sudah ditutup,
dan sisa yang belum.

---

## 1. Inventaris: 13 titik keluar, bukan 11

Doc 100 menghitung 11 entri katalog yang berupa tautan ZK. Angka itu benar untuk katalog,
tetapi bukan seluruh cerita — klien punya dua tempat yang membuka URL, dan hanya satu di
antaranya katalog:

| Tempat | Jumlah | Tujuan |
|---|---|---|
| `LaporanKatalogData.launchZk(...)` | 10 | `pages/master/kantin/laporan_keuangan.zul?lap=<kode>` |
| `LaporanKatalogData.dashAkun()` | 1 | `common/display.zul?p=akuntansi` |
| `posting_akun_perbaikan.dart` `webPath` | 2 | `master_asset.zul`, `kelompok_asset.zul` |

Keduanya memakai `launchUrl(..., LaunchMode.externalApplication)`. Jadi bukan "halaman web di
dalam aplikasi" — pengguna **keluar dari POS ke browser sistem**, dan harus masuk lagi ke
aplikasi web dengan sesi yang berbeda.

Rujukan `.zul` lain di kode Dart (5 berkas) semuanya **komentar dokumentasi** yang menyebut
layar ZK sebagai rujukan bentuk data; bukan pemanggilan. Itu tidak perlu diapa-apakan.

## 2. Yang menentukan bentuk pekerjaan

Dua temuan membuat pekerjaan ini jauh lebih kecil daripada kelihatannya.

**Pertama, klien sudah punya cabang natif.** `laporan_screen.dart`:

```dart
Future<void> _bukaItem(Map<String, dynamic> item) async {
  final url = item['url'] as String?;
  if (url != null && url.isNotEmpty) { ... launchUrl(...) ; return; }
  await Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => LaporanDetailScreen(item: item, ...)));
}
```

Entri **tanpa** `url` langsung dirender natif. Jadi seluruh pekerjaan ada di sisi server:
laksanakan kuncinya, buang URL-nya, dan kliennya natif **tanpa satu baris Dart pun berubah**.

**Kedua, kembarannya sudah ada.** Katalog ternyata sudah memuat 23 laporan akuntansi
berbasis API berawalan `akn_`, semuanya membaca jurnal terposting yang sama lewat
`klausaLedger()` (`a1.posting_history is not null` + penyaring Satuan Kerja kantin). Empat di
antaranya setara persis dengan entri ZK:

| Entri ZK | Kembaran natif yang sudah ada |
|---|---|
| `lk_jurnal` Jurnal Harian | `akn_jurnal` |
| `lk_bukubesar` Buku Besar | `akn_buku_besar` (subtotal per akun) |
| `lk_trial` Trial Balance | `akn_neraca_saldo` |

Artinya katalog selama ini membawa **entri rangkap**: satu versi natif dan satu versi ZK dari
laporan yang sama, dan yang ZK itulah yang melempar pengguna ke browser.

## 3. Yang dikerjakan

Tiga kunci ZK dialiaskan ke pelaksana natif yang sudah terbukti — bukan disalin, supaya tidak
ada SQL kembar yang bisa menyimpang satu sama lain:

```java
} else if ("akn_jurnal".equals(r)     || "lk_jurnal".equals(r))     {
} else if ("akn_buku_besar".equals(r) || "lk_bukubesar".equals(r))  {
} else if ("akn_neraca_saldo".equals(r) || "lk_trial".equals(r))    {
```

Satu kunci tidak punya kembaran dan dibuatkan cabang sendiri: `lk_bukubesartgl` — buku besar
dikelompokkan **per tanggal**, bukan per akun, dengan subtotal tiap tanggal. Sumber, penyaring,
dan klausa ledger-nya sama persis dengan Buku Besar.

URL pada keempat entri katalog dibuang, dan keterangannya dijujurkan. Keterangan lama
menyebut "resmi ... format JRXML" — itu tidak lagi benar untuk versi natif, dan membiarkannya
berarti menjanjikan tata letak cetak yang tidak diberikan. Angkanya sama; formatnya tabel,
bukan Jasper.

Hasil pemeriksa: entri tautan ZK **11 → 7**, kunci tanpa pelaksana tetap **0**.
Kompilasi: kode keluar 0, nol galat, 19.241 kelas — ketiga syarat gerbang terpenuhi.

## 4. Sisa yang belum, dengan alasannya

Enam entri katalog masih menunjuk ZK karena memang **belum ada padanan natifnya**, bukan
karena terlewat:

| Entri | Yang belum ada |
|---|---|
| `lk_keu2`, `lk_keu12`, `lk_keu2th` | Neraca/Laba Rugi/Arus Kas versi **kolom banyak periode** (2 periode, 12 bulan, 2 tahun). Versi satu periode sudah ada: `akn_neraca`, `akn_laba_rugi`, `akn_arus_kas`. |
| `lk_aruskas12`, `lk_aruskas31` | Arus kas format **kolom** (12 bulan / 31 hari). |
| `lk_neracalajur` | Neraca Lajur (kertas kerja) — tidak punya kembaran sama sekali. |

Ditambah `dashAkun()` (dasbor akuntansi ZK) dan dua formulir aset di
`posting_akun_perbaikan.dart` (`master_asset.zul`, `kelompok_asset.zul`) yang butuh layar
Flutter baru, bukan sekadar kunci laporan.

Klasifikasi Neraca/Laba Rugi/Arus Kas-nya sudah tersedia di basis data lewat
`akunting.kelompok_laporan_punya_akun` → `kelompok_laporan` → `jenis_laporan` (dipakai
`akn_daftar_akun` dan `DasboardAkuntansi`), jadi bahan untuk versi kolom banyak periode sudah
ada; yang belum hanya penyusunannya.

## 5. Catatan urutan yang tidak boleh dibalik

Membuang `url` sebelum kuncinya terlaksana akan mengubah laporan yang **bekerja** menjadi
pesan "sedang disiapkan" — persis kerusakan yang doc 100 peringatkan. Karena itu urutannya
selalu: laksanakan kuncinya, kompilasi, jalankan pemeriksa, **baru** buang URL-nya. Enam entri
yang tersisa sengaja dibiarkan menunjuk ZK sampai penggantinya benar-benar ada.
