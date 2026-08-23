# 16 — Satu pintu untuk pencairan: Proses Transfer

Permintaan pemilik produk (dari layar POS Desktop): *"menu ini kalau bisa dijadikan
satu saja, karena fungsinya sama … untuk menu transitori juga pindahkan ke Proses
Transfer."*

---

## 1. Keadaan sebelumnya

Grup **Keuangan** memuat tiga menu yang mengerjakan rangkaian yang sama atas dokumen
yang sama:

| Menu | Layar | Isi |
|---|---|---|
| Proses Transfer | `ProsesTransferScreen` | pencairan baris DPC |
| Pembayaran Vendor | `PengadaanBayarScreen` | bayar tagihan penyedia + tab Transitori |
| Proses Transitori | `ProsesTransitoriScreen` | realisasi dana yang mampir di rekening perantara |

Ketiganya berdiri sendiri-sendiri, sehingga pengguna harus menebak menu mana yang
memuat pekerjaan yang dicarinya — padahal semuanya satu urutan: dana dicairkan,
tagihan penyedianya dibayar, dan yang singgah di rekening perantara direalisasikan.

---

## 2. Bentuk sesudahnya

Satu menu **Proses Transfer** dengan lima tab:

| Tab | Isi | Asal |
|---|---|---|
| Dasbor | dasbor per modul, dipilih lewat satu dropdown | menggabungkan **tiga** dasbor yang tadinya terpisah |
| Proses Transfer | daftar pencairan DPC | tetap |
| Pembayaran Vendor | `PengadaanBayarScreen(tersemat: true)` | menu yang diserap |
| Transitori Menunggu | `PengadaanTransitoriTab` | tadinya sub-tab di Pembayaran Vendor |
| Proses Transitori | `ProsesTransitoriScreen(tersemat: true)` | menu yang diserap |

**Tidak ada satu pun tampilan yang dibuang.** Dua menu yang diserap dipakai ULANG apa
adanya lewat bendera baru `tersemat`, yang hanya melepas kerangka `AppShell` dan deret
tab miliknya sendiri — kerangka dan tab itu kini disediakan tuan rumahnya. Isinya
(penyaring, tabel, seluruh aksi, dan tombol "Bayar Vendor" yang tetap mengambang)
tidak berubah sama sekali.

**Ketiga dasbor juga tidak dibuang.** Sebelum penggabungan, tiap menu membawa
dasbornya sendiri dengan modul berbeda (`proses_transfer`, `dpc`,
`proses_transitori`). Menyatukan menu tidak boleh menghapus dua di antaranya, jadi
ketiganya tetap ada dan dipilih lewat satu dropdown di tab Dasbor.

**Layar yang diserap tetap dapat berdiri sendiri.** `tersemat` bawaannya `false`,
sehingga jalur navigasi lama (tautan dari layar lain, deep link) tidak putus.

---

## 3. Hak akses TIDAK ikut digabung

Ini bagian yang paling mudah salah: menyatukan menu tidak boleh diam-diam membuka
modul yang memang tidak boleh dilihat sebuah peran.

* Kunci menu `pengadaan_dpc` dan `proses_transitori` **tidak dihapus** dari katalog.
* Kunci itu kini menentukan muncul-tidaknya **TAB** di dalam Proses Transfer
  (`Sesi.instance.bolehMenu(...)`), bukan muncul-tidaknya baris menu.
* Peran yang hanya boleh Proses Transfer melihat dua tab saja; tabnya **tidak
  dibuat**, bukan sekadar disembunyikan.
* Gerbang sesungguhnya tetap di server pada tiap aksi — ini murni lapisan tampilan.

---

## 4. Cacat lama yang ikut ketahuan dan diperbaiki

Uji render mendapati `RenderFlex overflowed by 60 pixels on the right` pada tab
**Transitori Menunggu**. Sebabnya `DropdownButtonFormField` "Status" di
`pengadaan_transitori_tab.dart` berada di dalam `SizedBox(width: 190)` tanpa
`isExpanded: true`, sehingga label terpanjang ("Direalisasikan") memaksa lebar
alaminya 250 px dan pengguna melihat garis kuning-hitam.

Cacat ini **sudah ada sebelum penggabungan** — tab itu memang sudah dirender di dalam
Pembayaran Vendor. Ia baru ketahuan karena tab-nya kini diuji secara render. Dropdown
sejenis pada layar Keuangan lain sudah memakai `isExpanded`, jadi perbaikannya
menyamakan saja.

---

## 5. Hasil uji

| Uji | Hasil |
|---|---|
| `test/proses_transfer_gabungan_test.dart` (baru, 6 uji) | **LULUS** |
| — kontrak: kelima tab ada, ketiga layar dipakai ulang | ketiga modul masih dirender |
| — kontrak: hak akses per tab | jumlah penjaga tab = jumlah penjaga halaman (kalau timpang, `TabBarView` melempar saat dibuka) |
| — kontrak: dua menu lama tidak lagi di grup Keuangan | terkunci |
| — kontrak: layar yang diserap tetap punya `AppShell` sendiri | terkunci |
| — render: lima tab tampil dan dapat dibuka bergantian | tanpa galat tata letak |
| — render: peran tanpa hak hanya melihat dua tab | tab lain tidak dibuat |
| `test/sidebar_keuangan_test.dart` | disesuaikan: 'Pembayaran Vendor' & 'Proses Transitori' kini **findsNothing** di sidebar, dan gerbangnya diperiksa di berkas layar gabungan |
| Seluruh suite `apps/ebisnis` | **LULUS**, 279 uji |
| `flutter analyze` | 51 temuan, sama persis dengan garis dasar sebelum perubahan; tidak ada `error` |

> **Catatan cara menguji.** Uji render sempat gagal oleh invarian
> *"A Timer is still pending"* — dasbor menyalakan flush periodik `MasterOffline`.
> Itu artefak harness, bukan cacat produk: diselesaikan dengan
> `MasterOffline.hentikanTimer()` plus satu `pump` panjang agar timer sekali-jalan
> tuntas, pola yang sama dengan `drawer_akuntansi_halaman_test.dart`.

---

## 6. Berkas yang disentuh

| Berkas | Perubahan |
|---|---|
| `lib/screens/proses_transfer_screen.dart` | tuan rumah lima tab + `_DasborPencairan` (pemilih modul dasbor) |
| `lib/screens/pengadaan_bayar_screen.dart` | bendera `tersemat`, isi layar dipisah ke `_isi()` |
| `lib/screens/proses_transitori_screen.dart` | bendera `tersemat`, isi layar dipisah ke `_isi()` |
| `lib/screens/pengadaan_transitori_tab.dart` | `isExpanded: true` — perbaikan luapan 60 px |
| `lib/widgets/app_shell.dart` | dua menu keluar dari grup Keuangan |
| `lib/widgets/app_drawer.dart` | dua entri menu dibuang, impor yang menganggur ikut dibuang |
| `test/proses_transfer_gabungan_test.dart` | **baru** |
| `test/sidebar_keuangan_test.dart` | dua uji disesuaikan dengan bentuk baru |

> `app_shell.dart` saat itu juga sedang disunting sesi lain (penataan ulang grup
> Akuntansi). Hunk milik sesi lain **tidak** ikut dipentaskan: patch-nya disaring per
> hunk lebih dulu, lalu dipentaskan lewat `git apply --cached`.
