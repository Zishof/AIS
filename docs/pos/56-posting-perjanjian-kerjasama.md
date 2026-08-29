# Posting Massal "Perjanjian Kerjasama" (DP Kerjasama Aset)

Tanggal: 29 Agustus 2026. Kode masuk SVN **r78543** (terbawa commit sapu sesi paralel;
keempat berkas diverifikasi byte-identik di HEAD). Mirror `java/` selaras (r78543).
Lanjutan dari [55-posting-trio-pembayaran-vendor.md](55-posting-trio-pembayaran-vendor.md).

## 1. Apa yang ditambahkan

Baris dasbor baru **"Perjanjian Kerjasama"** (kunci `perjanjian_kerjasama`, kategori
Transaksi Vendor): perjanjian kerjasama aset yang disetujui dan ber-nilai DP dipantau
sampai menjadi jurnal uang muka. Mesin `postingSemua`/`batalkanPostingSemua` di
`PostingPerjanjianKerjasamaAction`, dispatch di `DraftJurnalApiHelper` (kunci izin
`pengadaan_po` — serumpun DP pemesanan: komitmen pada vendor), whitelist
`ENTITAS_CLOSING` mendapat `perjanjianKerjasamaMasterAsset`. Client Flutter tidak
diubah.

Jurnal per dokumen mengikuti tombol layar: **Dr akun DP jenis perjanjian / Cr akun
utang DP jenis perjanjian**, senilai `dp`; `dp` ≤ 0.1 memutar posisi. Dua kolom tanggal
punya peran berbeda dan diikuti setia: **rentang filter memakai `tanggal_pembuatan`**
(kolom filter layar), sedangkan **tanggal jurnalnya `tanggal_persetujuan`**. Konvensi
mesin-mesin sebelumnya berlaku: riwayat `posting=true`, cap hanya bila jurnal benar
tersimpan, riwayat kosong dihapus bila tak satu dokumen pun terjurnal, pembatalan
menghapus baris transaksi dulu baru grupnya (hanya yang belum closing).

## 2. Pengujian

Harness `TesPostingPerjanjian` (scratchpad, DB UAT lokal `ais`), fixture `UATPKS-` pada
rentang **10–20 Mei 2091** (dipastikan kosong):

| Skenario | Hasil |
|---|---|
| K1: disetujui, dp 350k, jenis lengkap | Dr 350k akun DP / Cr 350k akun utang DP; **jurnal bertanggal tanggal persetujuan** |
| K2: dp nol | tidak pernah terpilih |
| K3: belum disetujui | tidak pernah terpilih |
| K4: jenis tanpa akun utang DP | dilewati, tetap draf |
| Hitung dasbor | draf 2 → terposting 1 + draf 1, konsisten mesin |
| Idempoten + batal | posting/batal ulang 0; jurnal terhapus, dokumen kembali draf |

**LULUS 13, GAGAL 0** (lulus pada run pertama — catatan fixture dok 53–55 terpakai
semua). Kompilasi `javac -source 1.7 -target 1.7` bersih (4 berkas).

## 3. Sisa peta modul

Selesai: Jurnal Umum (53), Pengembalian Uang Muka (54), trio pembayaran vendor (55),
Perjanjian Kerjasama (dok ini). Berikutnya: payroll Transaksi Pegawai/Penggajian,
Saldo Awal Kas Kecil (`PostingJenisKasKecilAction`), dan trio kantin (HPP/Penjualan/
Toko — batch per periode, perlu desain endpoint sendiri).
