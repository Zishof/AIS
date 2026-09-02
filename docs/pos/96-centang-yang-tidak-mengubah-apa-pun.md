# Centang yang tidak mengubah apa pun

Audit dua arah atas `EbisnisMenuKatalog.KUNCI_CRUD` — daftar kunci yang
memunculkan baris create/update/delete/approve/reject di grid peran
(`TbmroleAction`).

## Arah pertama: kunci gerbang di luar KUNCI_CRUD — BERSIH

Kunci yang **dipakai** gerbang tetapi **tidak terdaftar** di `KUNCI_CRUD`
membuat `bolehAksi()` jatuh ke `aksiLegacy = true`: gerbangnya meloloskan setiap
peran, dan grid tidak pernah menawarkan barisnya sehingga izinnya tak pernah
dapat dicabut. Begitulah `returpembelian` pernah lolos (lihat
[89](89-gerbang-yang-tidak-pernah-menutup.md)).

Ditelusuri dua lapis — 26 pemanggilan langsung `EbisnisMenuKatalog.bolehAksi`,
lalu kunci yang mengalir lewat delapan metode pembungkus yang menerimanya
sebagai parameter. **Semuanya ada di `KUNCI_CRUD`.** Kelas cacat itu sudah
tertutup.

## Arah kedua: kunci ber-CRUD yang tidak pernah ditegakkan — ENAM

Arah ini tidak meninggalkan gejala apa pun. Admin membuka grid peran, mencabut
centang "Hapus", menyimpan, dan percaya sudah membatasi sesuatu. Tidak ada galat,
tidak ada perbedaan perilaku, tidak ada cara mengetahuinya selain mengaudit.

Dari 92 kunci `KUNCI_CRUD`, **86 ditegakkan** dan **6 tidak** — masing-masing
dengan alasan yang, setelah ditelusuri, **bukan cacat**:

| Kunci | Kenyataannya |
|---|---|
| `apotik_narkotika` | register audit; ditulis OTOMATIS, tanpa mutasi pengguna |
| `emedik_kasir` | panel ZK SIRS; digerbangi model `hakAkses` lama |
| `emedik_pendaftaran` | idem |
| `emedik_tagihan` | idem |
| `emedik_deposit` | idem |
| `emedik_penjamin` | idem |

**Obat terkendali.** `sirs.apotik_narkotika_log` bukan tabel yang disunting
pengguna. Barisnya lahir sendiri di `ApotikApiHelper` ketika obat bergolongan
terkendali terjual (`ApotikItemProfile.terkendali(golongan)`), di dalam
transaksi penjualan yang sama. Tidak ada endpoint create/update/delete untuk
register itu; satu-satunya pembaca lain adalah `ApotikLaporanHelper`. Jadi tidak
ada apa pun yang bisa digerbangi — dan itulah yang membuat centangnya
menyesatkan: admin yang mencabut "Hapus" pada menu ini mengira telah mengunci
register obat terkendali, padahal memang tidak pernah ada jalur hapusnya.

**Lima layar eMedik.** Kelimanya berkas JSP sepanjang 3-23 baris yang hanya
menyisipkan panel ZK SIRS (`pagesmastersirspembayaranzul`,
`pagesmastersirspendaftaranrawatjalanzul`, dan seterusnya). Panel itu dirender
`DynamicJspCrudGenerator`, yang **menegakkan haknya sendiri** — lewat model peran
AIS lama (`u.hakAkses()`), dengan pesan "Anda tidak memiliki hak akses tambah
data" dan seterusnya. Layarnya berizin; izinnya hanya datang dari sistem yang
**lain**.

Ini bentuk yang sama persis dengan divergensi Penyedia/Supplier yang sudah
dicatat di [84](84-sapuan-hak-akses-tombol.md): dua sistem izin untuk satu
produk, dan divergensinya tidak terlihat dari grid.

## Keputusan yang TIDAK diambil di sini

Grid peran kini menawarkan 30 centang (6 kunci x 5 aksi) yang tidak mengubah
apa pun. Ada dua jalan, dan keduanya mengubah permukaan admin yang menyangkut
keamanan:

1. **Keluarkan keenamnya dari `KUNCI_CRUD`** — grid berhenti berbohong.
   Visibilitas menunya tidak terpengaruh: `defaultObj()` menyusun `menu` dari
   `DAFTAR` dan `crud` dari `KUNCI_CRUD` secara terpisah, jadi menunya tetap
   tampil seperti sekarang.
2. **Bangun gerbang eBisnis untuk eMedik** — berarti sistem izin **kedua** untuk
   layar yang sudah punya satu, dengan risiko keduanya lambat laun berbeda.

Untuk `apotik_narkotika` jalan (1) jelas benar — tidak ada mutasi yang mungkin
ada. Untuk eMedik pilihannya bergantung pada arah produk: apakah layar SIRS itu
akan dipindahkan ke model izin eBisnis, atau memang dibiarkan di model lama.
Karena itu **tidak diubah** dalam pekerjaan ini; temuannya dicatat dan
regresinya dikunci.

## Penjaga

`test/kunci_crud_ditegakkan_test.dart` (repositori Flutter), 2 uji, ~2 detik.

Enam kunci di atas menjadi **daftar pengecualian bersama alasannya** — izin
untuk tidak ditegakkan, bukan daftar cacat yang dibiarkan. Menambah kunci baru
ke `KUNCI_CRUD` tanpa gerbangnya akan menjatuhkan uji.

Uji kedua menjaga pengecualiannya sendiri: entri yang kuncinya sudah tidak ada
di `KUNCI_CRUD` harus dihapus. Pengecualian basi lebih berbahaya daripada tidak
ada pengecualian — ia diam-diam memaafkan kunci lain yang kebetulan bernama sama.

Keduanya dibuktikan dengan uji negatif: (1) menambahkan kunci ber-CRUD tanpa
gerbang membuat uji pertama jatuh menyebut kuncinya; (2) menghapus
`emedik_kasir` dari `KUNCI_CRUD` membuat uji kedua jatuh menyuruh membersihkan
pengecualiannya. Berkas dipulihkan dan diverifikasi byte-identik setelah
masing-masing.

## Catatan metode: tiga pengukuran salah sebelum yang benar

Dicatat karena polanya berulang di hampir setiap audit di repositori ini.

| Percobaan | Hasilnya | Sebabnya |
|---|---|---|
| Ambil argumen kunci dari pemanggilan `boleh*` mana pun | 5 "temuan": `cancel`, `submit`, `reverse`, `deactivate`, `edit_draft` | itu nama AKSI, bukan kunci menu — argumennya salah posisi |
| Cari pembungkus berdasarkan NAMA metode di seluruh pohon | 6 "temuan" dari `NewUi*Controller` kampus | tabrakan nama: `private static boolean boleh(String kunci, boolean fallback)` adalah pembaca flag konfigurasi, tak ada hubungannya dengan katalog |
| Anggap `emedik_*` tak terimplementasi karena tak ada di `ais/action/servlet/` | salah | modulnya ada, sebagai lima halaman JSP yang menyisipkan panel ZK |

Yang akhirnya benar: berangkat dari **satu titik masuk yang mengikat**
(`EbisnisMenuKatalog.bolehAksi`), telusuri pemanggilnya, lalu resolusi
konstantanya — bukan mencari nama yang terdengar seperti gerbang.

Diverifikasi pula bahwa pencocokan **literal saja** memberi jawaban yang sama
persis dengan resolusi konstanta (enam kunci yang sama), sehingga penjaganya
dapat memakai logika yang lebih sederhana tanpa kehilangan cakupan.
