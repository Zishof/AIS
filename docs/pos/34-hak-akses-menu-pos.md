# Hak akses menu POS di Tbmrole

Dua perbaikan terpisah: **kotak centangnya harus ada**, dan **server harus benar-benar
menegakkannya**.

## Prinsip

> Kotak centang tanpa penegakan di server **lebih berbahaya daripada tidak ada**, sebab admin
> akan mengira sudah membatasi sesuatu yang sebenarnya tetap terbuka.

Karena itu setiap kunci yang ditambahkan ke `EbisnisMenuKatalog.KUNCI_CRUD` dikerjakan
bersama penegakannya di endpoint yang bersangkutan.

## Bagian 1 — lima menu yang haknya tidak pernah dapat dibatasi

| Menu | Kunci | Penegakannya |
|---|---|---|
| Kasir | `kasir` | `create` = buka sesi kas, `delete` = tutup sesi kas (`PosApi`) |
| Konfigurasi | `konfigurasi` | `update` = `pedagang_ubah`, termasuk ganti kata sandi |
| Jurnal Umum | `jurnal_umum` | `create` simpan, `delete` hapus, `approve` **posting**, `reject` batal posting |
| Posting HPP | `posting_hpp` | `approve` di `onPosting()` **dan** `postingBarisIni()` |
| Posting Penjualan | `posting_penjualan` | idem |

`posting_hpp` dan `posting_penjualan` sifatnya **sama persis** dengan `posting_kulakan`,
`posting_bayar_hutang`, `posting_terima_piutang`, dan `posting_penyesuaian` yang sudah lama
terdaftar. Ketertinggalannya tidak disengaja.

### Jalan pintas yang hampir terlewat

Pada layar posting ZK, tombol "Posting" **per baris** memanggil `postingBarisIni()`
**langsung**, tidak lewat `onPosting()`. Menjaga `onPosting()` saja menyisakan pintu yang
terbuka lebar. Keduanya dijaga.

### Jalur POS-nya juga

POS memposting HPP/Penjualan lewat `laporan_keuangan_pendukung` dengan `posting=true` (atau
`posting_ids` untuk posting per transaksi) — aksi yang sebelumnya tidak memeriksa hak sama
sekali. Tanpa ini, layar ZK terjaga sementara POS tetap terbuka untuk aksi yang sama.

**Melihat** lampiran pendukung tetap bebas; yang digerbang hanya penulisan jurnalnya. Jenis
lampiran selain hpp dan penjualan tidak berubah perilakunya.

Memakai varian akuntansi (`bolehAksiAkuntansi`), bukan `bolehAksi` biasa, supaya aturannya
sama persis dengan layar ZK-nya dan dengan `KodeAkunApiHelper`.

### Pemisahan kewenangan

- **Memposting dipisah dari menyimpan.** Menulis buku besar lazim dipegang orang lain
  daripada yang menyusun drafnya.
- **Membuka sesi kas dipisah dari menutupnya.** Di banyak toko itu memang bukan orang yang
  sama.

### Aman secara mundur

`bolehAksi` dan `bolehAksiAkuntansi` mengembalikan `true` untuk kunci yang **belum pernah
disimpan**, jadi peran lama tidak kehilangan akses. Admin tinggal mencentang yang perlu
dibatasi.

## Bagian 2 — tab "Hak Akses Pedagang" hanya memuat 28 dari 99 menu

`TbmroleAction`

Tab ini **berbeda** dari tab "Dashboard & Menu". Barisnya ditulis **satu per satu dengan
tangan**, dan hanya memuat menu e-Kantin generasi pertama. Setiap modul yang lahir sesudahnya
tidak pernah ditambahkan: Pengadaan, Akuntansi, Keuangan, Inventory & Sales, Apotik, eMedik,
MitraInap. Semuanya sudah punya menu di POS, tetapi haknya tidak dapat diatur sama sekali.

### Dibangkitkan, bukan ditulis

Barisnya kini dibangkitkan dari `EbisnisMenuKatalog.DAFTAR`. Menuliskannya satu per satu
berarti masalah yang sama **terulang pada modul berikutnya** — seseorang harus ingat
menyunting berkas ini, dan yang lupa tidak akan ketahuan sampai ada yang mencari kotak
centangnya. Sekarang menu baru muncul dengan sendirinya.

- Menu pada `KUNCI_CRUD` → grid Read/Create/Update/Delete/Approve/Reject penuh.
- Sisanya → satu kotak Read, sebab memang tidak ada aksi mutasi yang berarti (laporan,
  dasbor, monitor).

28 baris tulis tangan dipertahankan apa adanya: labelnya sudah dikenal pengguna lama
("Barang" untuk `produk`, "Vendor" untuk `penyedia`) dan urutannya tidak diubah.

### Hasilnya: 8 kelompok, 71 baris tambahan

| Kelompok | Baris |
|---|---|
| Pengadaan | 8 |
| Akuntansi | 16 |
| Keuangan | 6 |
| Inventory & Sales | 16 |
| Apotik / eMedik / MitraInap | 10 / 6 / 8 |
| (POS lain-lain) | 1 |

### Jebakan: judul kelompok muncul dua kali

Sub-judul diambil dari awalan label sebelum titik dua (`"Pengadaan: ..."`,
`"Akuntansi: ..."`). Mengelompokkan **sambil berjalan** — "cetak judul bila berbeda dari
baris sebelumnya" — membuat "Akuntansi" muncul dua kali, sebab urutan deklarasi katalog
tidak selalu menyatukan satu kelompok: `posting_penyesuaian` dan `anggaran` dideklarasikan
**setelah** blok Keuangan.

Pengelompokan karena itu dikerjakan **lebih dahulu** lalu dicetak.
