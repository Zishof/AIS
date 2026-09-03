# POS Al-Bahjah 1.34.20 — Koreksi Transaksi dan Laporan Pemasok

## Status akhir

Source perubahan sudah tersimpan di kedua repositori:

- backend AIS: SVN r83902;
- Flutter POS dan runbook UAT: GitHub `main`, commit `91f79cf`;
- kedua mirror backend `src/main/java` dan `src/main/src` sudah identik untuk file yang diubah.

Installer masih merupakan artefak UAT lokal. Source yang sudah di-push tidak berarti backend sudah dideploy atau installer sudah didistribusikan ke komputer toko.

## Cakupan

- Validasi ulang member/PIC, batas transaksi, hutang, dan saldo/deposit pada koreksi transaksi selesai.
- Split-payment lama tetap utuh ketika pengguna hanya mengubah tanggal, kasir, produk, atau kuantitas.
- Perubahan metode pembayaran hanya dikirim bila dropdown memang diubah pengguna.
- Riwayat penjualan menjelaskan audit sebelum/sesudah beserta alasan koreksi.
- Laporan Penjualan Barang Per Pemasok menampilkan rincian produk, UOM, qty UOM, qty dasar, dan nilai penjualan.
- Dialog kesalahan dan layar perbaikan posting akun disiapkan sebagai jalur pemulihan yang dapat ditindaklanjuti pengguna.

## Bukti verifikasi

- targeted Flutter tests: 12/12 lulus;
- Flutter analyzer: tanpa warning/error; tersisa 50 lint level info lama;
- Maven incremental compile: BUILD SUCCESS;
- self-test koreksi transaksi: 9/9 aturan lulus;
- self-test SQL laporan: 18/18 aturan lulus;
- build Windows Al-Bahjah 1.34.20 dan smoke-run executable: berhasil.

Artefak UAT lokal:

- installer: `C:\opt\CodeBaseDesktopDanMobile\apps\ebisnis\release-artifacts\semua-varian\1.34.20\Al-Bahjah-POS-Setup-1.34.20.exe`;
- SHA-256 installer: `04E2FCE109CFEA88212D295414EF249470C50F7715B26B61EDA76407FFF3BC5E`;
- SHA-256 executable: `31E029F14FA24807EBDBCB5E6002358F5562150288FC6B7516E31F9AF37E3F42`.

## Deployment dan rollback

Fitur koreksi penuh memerlukan update backend dan client. Jangan menguji tombol baru terhadap server lama. Jalankan UAT dengan transaksi dummy yang belum diposting dan belum mempunyai retur.

Jika UAT gagal, nonaktifkan kebijakan koreksi global/per toko, kembalikan backend dan client ke versi stabil sebelumnya, lalu periksa kembali transaksi dummy serta stok. Jangan menghapus transaksi langsung dari database; gunakan retur/pembatalan resmi atau pulihkan database UAT dari backup.

Runbook rinci, skenario UAT, batas laporan, dan balasan WA tersimpan di `docs/pos/2026-09-03-koreksi-transaksi-rincian-pemasok-1.34.20.md` pada repository GitHub.
