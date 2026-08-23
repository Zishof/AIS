# Transfer, Transitori, dan Realisasi pada Pembayaran Vendor

Tiap baris tagihan pada Pembayaran Vendor dapat ditandai **Transfer** atau **Transitori**.
Pilihan itu bukan penanda tampilan: ia menentukan **akun kredit jurnalnya**.

## Mekanismenya di ZKoss

| Hal | Tempatnya |
|---|---|
| Flag per baris | `DaftarPengajuanTransfer.transitori` (Boolean) |
| Record pasangannya | `DaftarPengajuanTransfer.transitoriData` → `Transitori` |
| Yang menerbitkannya | `ProsesTransferAction` — saat kotak "Transitori" dicentang |
| Batch realisasi | `ProsesTransitori`, disetujui lewat `ProsesTransitoriAction` |
| Jurnalnya | `PostingProsesTransitoriAction` — **layar terpisah** |

### Akun kreditnya

`PostingPembayaranAction` memilih:

- **Transfer** → `CaraPembayaranTransfer.akun`
- **Transitori** → `CaraPembayaranTransfer.akunTransitori`

Keduanya sama-sama mengurangi kewajiban kepada penyedia, tetapi pada transfer uangnya sudah
keluar dari kas/bank sedangkan pada transitori belum — ia menunggu di akun perantara.

## Yang harus dibangun di POS

**POS tidak pernah membuat `DaftarPengajuanTransfer` sama sekali.** `bayarSimpan` hanya
membuat `PembayaranTerminMasterAssetDetail`. Jadi ini bukan menampilkan pilihan yang sudah
ada; mekanismenya harus dibangun dulu.

### Baris pengajuan dibuat di dalam transaksi yang sama

Dibuat langsung di `bayarSimpan`, **bukan** lewat
`DaftarPengajuanTransfer.simpanPembayaranTerminMasterAssetDetail` — pabrik itu membuka dan
menutup transaksinya sendiri, sedangkan seluruh penyimpanan berjalan dalam satu transaksi.

### Record `Transitori` berpasangan wajib ikut

Menandai transitori **tidak cukup dengan menyetel flag-nya**. `ProsesTransferAction` juga
menerbitkan satu record `Transitori`, ditaut dua arah, dan menghapusnya kembali ketika
tandanya dilepas. **Record itulah yang muncul pada daftar realisasi.** Tanpa dia, baris yang
ditandai transitori tidak akan pernah dapat direalisasikan dan uangnya menggantung di akun
perantara selamanya.

> Ini sempat terlewat pada penerapan pertama dan baru ketahuan saat menelusuri dari mana
> entitas `Transitori` lahir.

### Menyunting dokumen yang barisnya sudah masuk batch

`bayarSimpan` menghapus seluruh baris detail lalu membuatnya ulang. Baris pengajuan miliknya
ikut dibuang — kalau tidak, ia menggantung menunjuk detail yang sudah tidak ada.

Aman dilakukan karena dokumen yang sudah disetujui ditolak lebih awal, sehingga yang sampai
ke sini pasti masih DRAF. Tetap dipasang penjagaan tambahan: pengajuan yang sudah masuk
proses transfer atau sudah diposting tidak disentuh.

Dan bila salah satu barisnya **sudah masuk batch realisasi**, penyuntingannya **ditolak di
muka** dengan pesan yang jelas — bukan dilewati diam-diam. Melewatinya akan menyisakan baris
pengajuan yang menunjuk detail terhapus.

## Realisasi

Aksi `pengadaan_transitori_daftar` dan `pengadaan_transitori_realisasi`.

Realisasi = mengumpulkan beberapa `Transitori` ke dalam satu `ProsesTransitori`, menjumlahkan
nilainya, lalu menyetujui batch itu (`disetujuiOleh` + `tanggalPersetujuan`). Persis yang
dikerjakan `ProsesTransitoriAction`.

### REALISASI BUKAN POSTING JURNAL

Di ZKoss keduanya **dua langkah terpisah**. Jurnalnya diterbitkan lewat layar **Posting
Proses Transitori** yang membaca batch yang sudah disetujui, dan pembatalan posting
dikerjakan di sana pula.

POS **sengaja tidak menerbitkan jurnal sendiri**. Menerbitkannya akan melahirkan jurnal kedua
yang lambat laun berbeda dari versi ZKoss tanpa ada yang menyadarinya, sekaligus melewati
layar pembatalan posting yang sudah ada. **Jangan tambahkan posting jurnal di sana.**

Dialog konfirmasinya menyebutkan hal ini apa adanya kepada pengguna, bukan disembunyikan.

### Penjagaan

**Pembayaran yang belum disetujui tidak dapat direalisasikan.** Mencairkan dokumen draf
berarti mencairkan sesuatu yang belum sah — dan draf masih dapat disunting, sehingga
barisnya bisa berubah setelah dicairkan. Barisnya diredupkan di layar dengan alasannya
tertulis; server menolaknya juga.

**Hanya transitori milik pembayaran pengadaan POS yang ditampilkan.** Transitori dari modul
lain (uang muka, kas kecil, reimbursement) berbagi tabel yang sama tetapi alur
persetujuannya berbeda.

## Cetak dari tab Transitori

Yang dicetak adalah **dokumen pembayarannya** (`tahap: 'dpc'`), bukan transitorinya sendiri.
Transitori bukan dokumen berdiri sendiri melainkan satu baris pada pembayaran vendor.

Lembar "Bukti Realisasi Transitori" tersendiri belum dibuat; itu memerlukan templat Jasper baru.

## Bantuan pengguna

Menu **Bantuan** pada Pembayaran Vendor (`bantuan_kontekstual.dart`, kunci `pengadaanDpc`)
menjelaskan keduanya sampai ke jurnalnya, berikut tujuh tanya jawab. Tiga hal ditegaskan
eksplisit karena paling mudah disalahpahami dan paling mahal bila salah:

1. Pilihan itu **per baris**, bukan per dokumen.
2. Baris transitori **tidak selesai sendiri**.
3. **Realisasi bukan posting jurnal.**
