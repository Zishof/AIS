# 40 — Modul Pengadaan di POS Desktop & Android

Alur PR → PO → BAST → Terima Tagihan → Bayar Vendor → Bayar Pajak, dipindahkan
dari ZKoss ke POS Desktop/Android (Flutter) dan JSP.

## Prinsip yang menentukan bentuknya

**Tabel dipakai bersama.** POS memakai tabel `asset.*` yang SAMA dengan ZKoss,
dibedakan kolom `toko`. Tidak ada tabel tiruan untuk POS.

**Aturan bisnis hanya satu salinan.** Seluruhnya di
`ais.action.servlet.api.PengadaanPosApiHelper`; `PosApi` dan `Data` meneruskan
aksi ber-awalan `pengadaan_`. Pabrik milik ZKoss dipanggil langsung — bukan
ditiru — sehingga keduanya tidak bisa menyimpang:

- `Pajak.buatDariTermin` untuk pembentukan pajak,
- `PenggunaanAnggaran.prosesSimpan` untuk pemotongan anggaran,
- pembangun `parameter(...)` milik aksi ZKoss untuk cetak.

**Kolom baru selalu nullable dan dibuat Hibernate.** Tujuh kolom ditambahkan
lewat `hbm2ddl` saat boot, tanpa DDL tulisan tangan:

| Entitas | Kolom |
|---|---|
| `PemesananPengadaanMasterAsset` | `tutup`, `alasanTutup`, `poInduk` (FK ke dirinya sendiri) |
| `PembayaranTerminMasterAsset` | `caraPembayaranTransfer`, `judul`, `tanggalRealisasi` |
| `PenerimaanPengadaanMasterAssetDetail` | `pajak` |

## Back Order

Ketika kiriman tidak memenuhi pesanan, sisa PO **ditutup pendek** dan pesanan
susulan diterbitkan untuk kekurangannya. Nilai PO awal ikut dipangkas menjadi
sebatas yang benar-benar diterima.

**Status PO awal TETAP `DISETUJUI`, bukan `DITUTUP`.** Label "ditutup" keliru
menggambarkan keadaannya: dokumennya sah, sudah disetujui, dan barangnya
sebagian sudah diterima — yang berhenti hanya SISA kiriman. Label itu juga tidak
pernah ada pada penyaring status di layar mana pun, sehingga PO yang
menyandangnya justru tidak dapat ditemukan lewat penyaring. Keadaan "ditutup"
tetap dikirim terpisah lewat medan `tutup` dan `alasanTutup`.

`poInduk` adalah referensi-diri. Konsekuensinya: **apa pun yang menghapus PO
harus mengosongkan `po_induk` lebih dulu**, kalau tidak PO induk terkunci kunci
asing. Ini sudah menggigit dua harness uji (lihat [44](44-uji-regresi.md)).

## Perhitungan yang mudah salah

`terpakaiPembayaranPo/Termin(..., hanyaDisetujui)` punya dua arti, dan memilih
yang salah menghasilkan angka yang kelihatan masuk akal tetapi keliru:

- `true` — hanya yang **sudah dibayar** (disetujui). Dipakai untuk status lunas.
- `false` — termasuk yang **baru diajukan** (draf). Dipakai untuk daftar tagihan
  terbuka, supaya satu tagihan tidak dapat diajukan dua kali.

PO yang sudah ditutup dihitung berdasarkan yang **diterima**, bukan yang
dipesan:

```java
if (Boolean.TRUE.equals(induk.getTutup())) {
    jml += jumlahSudahDiterima(session, d.getId(), null);
    continue;
}
```

## Lampiran tagihan

Slot lampiran didefinisikan di `SLOT_LAMPIRAN_TAGIHAN`: Invoice (wajib, harus
gambar), Faktur Pajak, Surat Jalan, Kwitansi, Dokumen Lain. Batas 5 MB.

**Format gambar diperiksa dari angka ajaib (magic number), bukan ekstensi
berkas** — `jenisBerkas(byte[])`. Ekstensi mudah dipalsukan.

Lampiran disimpan sebagai PostgreSQL **Large Object** di basis data streaming.
Itu membawa satu aturan yang mudah terlewat dan sudah pernah terlewat:

> Large Object **hanya boleh dibaca di dalam transaksi**. Memanggil
> `getFoto().getBinaryStream()` begitu saja selalu gagal dengan *"Large Objects
> may not be used in auto-commit mode"*.

Aturan itu sudah dipecahkan di `FileFotoLain`. Pakai
`FileFotoLain.ambilIsiBlob(berkas)` — jangan menyalin aturannya ke pemanggil
baru, karena salinan akan menyimpang.

## Pajak

PPN dan PPh yang diketik di BAST masuk ke daftar pajak terutang. `pajakTerutang`
punya DUA sumber: `PEMBAYARAN` dan `BAST`. Pemindaian BAST dibatasi
(`Restrictions.gt` pada persen + `isNull("pajak")` + `setMaxResults(3000)`) agar
tidak memindai seluruh tabel.

## Cetak

`pengadaan_cetak` memakai ulang templat dan pembangun parameter milik ZKoss.
Balasannya membawa `url` dan `fileBase64` (≤8 MB) supaya Desktop/Android bisa
menampilkan **pratinjau** lebih dulu.

Pratinjau memakai `PdfPreview` di dalam aplikasi, **bukan** `Printing.layoutPdf`
langsung: di Windows yang terakhir itu melompat ke dialog printer sistem tanpa
memberi pengguna kesempatan melihat dokumennya.

Templat baru: `webapp/report/asset/bukti_setor_pajak.jrxml` — tahap Bayar Pajak
tidak punya padanan cetak per-baris di ZKoss, jadi pembangun parameternya
(`parameterCetakPajak`) dibuat `public static` dan dipakai bersama oleh tombol
cetak versi ZKoss agar hasilnya benar-benar sama.

## Yang perlu diketahui sebelum mengubah

- `Report` mencari templat di direktori yang SAMA dengan tempat ia menulis
  keluaran (`Common.ambilREAL_PATH_REPORT()`). Di luar Tomcat jalur itu kosong
  dan berkas dicoba dibuat di akar drive.
- Cetak memerlukan basis data **streaming** karena gambar kop surat disimpan di
  sana (`SuratUtil.initDefaultKop` → `LampiranLain.ambil`). Bila basis data itu
  tak terjangkau, cetak menggantung lalu gagal — bukan mencetak tanpa logo.
