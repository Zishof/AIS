# Empat sapuan keuangan yang bersih, dan angka panduan yang diverifikasi

Batch lanjutan sesudah doc 97, tetap di modul keuangan dan akuntansi.

Isinya empat pemeriksaan yang **tidak menemukan cacat**. Dicatat justru karena itu: tanpa
catatan, keempatnya akan disapu ulang oleh siapa pun yang mencari di tempat yang sama.

---

## 1. Laporan terimplementasi tetapi tidak terdaftar — bukan yatim

Kebalikan dari doc 97: bukan entri katalog tanpa pelaksana, melainkan cabang pelaksana tanpa
entri katalog. Dari 169 cabang `if ("kunci".equals(r))` di `LaporanKantinUtil`, dua tidak ada
di katalog:

```
fin_laba_rugi_rincian_hpp
fin_laba_rugi_rincian_penjualan
```

Keduanya laporan keuangan, dan sekilas tampak seperti kode yang tidak dapat dijangkau
pengguna. **Bukan.** Keduanya dirujuk `WEB-INF/baru/modul/kantin/laporan_laporan.jsp` —
sasaran rincian yang dibuka dari layar Laba Rugi, bukan entri daftar. Memang tidak perlu
terdaftar.

## 2. Penjagaan `closing`: 24 dari 24 mesin posting aman

Sudah dicatat di doc 97 §1, diulang di sini karena termasuk sapuan batch ini: pengukuran
pertama tampak menemukan dua mesin tanpa penjaga, dan itu cacat cara mengukurnya — bukan
cacat kodenya.

## 3. Dua penjaga rute New UI: perbandingan himpunan tidak menjawab apa pun

Catatan lama menyebut aksi baru wajib terdaftar di `NewUiRouteGuard` **dan**
`NewUiHybridMenuRouteGuard`. Himpunan keduanya dibandingkan:

| | |
|---|---|
| hanya di `NewUiRouteGuard` | `index`, `menu`, `native_menu`, `nui_native_module`, `nui_native_page`, `ringkasan` |
| hanya di `HybridMenuRouteGuard` | `download_lampiran`, `generate`, `history`, `import_data`, `move`, `photo_upload`, `sync`, `toggle`, … |
| di keduanya | 34 |

Selisihnya besar, tetapi **tidak berarti ada yang terlewat**: kedua penjaga punya kosakata
berbeda. Yang satu menjaga rute halaman, yang lain operasi data. Menyimpulkan "14 aksi tidak
terdaftar" dari selisih ini akan salah.

Pemeriksaan yang benar menuntut mengetahui aksi mana yang termasuk kategori yang wajib ada di
keduanya — dan itu tidak dapat dijawab dengan membandingkan literal string. **Dicatat sebagai
tidak terjawab, bukan sebagai bersih.**

## 4. Angka di panduan staf: cocok

Panduan keuangan yang sudah diserahkan memuat dua angka yang dapat diperiksa terhadap
katalog:

| Klaim di panduan | Kenyataan | |
|---|---|---|
| "sekitar 180 laporan dalam 32 kategori" | 184 entri, 32 kategori | cocok |
| sembilan kategori akuntansi di Pintu A: Keuangan, Buku Besar, Kas & Bank, Piutang, Pengadaan, Pajak PPN, Anggaran RAB, Gaji, Rekonsiliasi Bank | kesembilannya ada dengan nama itu | cocok |

Satu hal yang **tidak** disebut panduan: katalog Pintu B memuat tiga kategori akuntansi
tambahan yang lebih baru — "Laporan Keuangan Resmi — Komparatif (Akuntansi)", "Buku Besar
Resmi (Akuntansi)", dan "Arus Kas & Analisa Keuangan (Akuntansi)". Yang pertama justru berisi
sebelas entri placeholder dari doc 97.

Panduan itu menjelaskan Pintu A, jadi tidak keliru. Tetapi bila suatu saat diperbarui, ketiga
kategori itu layak disebut — terutama supaya pembacanya tidak mencari "Neraca Lajur" di
tempat yang belum menyediakannya.

## 5. Catatan operasional: working copy terkunci

Sepanjang batch ini `docs/pos` berstatus `L` (locked) dengan **tiga proses `svn` aktif**, satu
di antaranya berjalan sejak satu setengah jam sebelumnya. `svn cleanup` **tidak** dijalankan:
kunci itu milik operasi yang sedang berjalan, dan membersihkannya akan merusak pekerjaan sesi
lain.
