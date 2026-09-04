# Kesiapan Deploy Server untuk UAT Akuntansi, Keuangan, dan Pengadaan

Tanggal pemeriksaan: 4 September 2026 (Asia/Jakarta)

## Status

Kode backend sudah berhasil dikompilasi penuh dan paket WAR berhasil dibentuk. Paket belum
dideploy; deployment menunggu instruksi/eksekusi operator server.

Artefak deploy:

- Lokasi: `C:\opt\AIS\ais\build\maven\ais.war`
- Ukuran: 749.678.558 byte
- SHA-256: `4EDEA94BA51DF1D2295CBD3C4300D2A5FFCCFC6135014F4ABA7FB60E3C898B71`
- Waktu build: 2026-09-04 07:30:59 +07:00
- Hasil build: `BUILD SUCCESS`

## Perubahan Backend

1. `KantinHelper.waktuTransaksiDariPayload`
   - Parser tanggal dibuat non-lenient dan wajib mengonsumsi seluruh teks.
   - Mencegah nilai ISO `yyyy-MM-dd HH:mm:ss` salah diterima pola `dd-MM-yyyy` lalu tersimpan
     sebagai tahun 0010.

2. `PosApi` dan mesin Posting Penjualan/HPP Kantin
   - Pengguna terautentikasi dari token API diteruskan eksplisit sebagai pelaku posting.
   - Mesin headless tidak lagi bergantung pada `Common.getCurrentUser()` milik sesi ZK/web.
   - Menghilangkan kegagalan penyimpanan `PostingHistory.nama/tbmuser` pada posting lewat POS API.

3. `PosApi.daftarOrderDenganSesi`
   - Filter toko hanya ditambahkan bila `tokoId` tersedia.
   - Akun yang sudah berhak melihat Semua Toko tidak lagi memperoleh daftar kosong akibat
     perbandingan SQL `a.toko = NULL`.

4. `ReimbursementApiHelper.simpan`
   - Snapshot atasan langsung diisi seperti alur web; bila hierarki atasan belum tersedia,
     pegawai penerima dipakai sebagai fallback sesuai aturan lama layar ZK.
   - Mencegah pelanggaran constraint kolom `atasan` pada instalasi database lama.

5. `PostingPertangungjawabanKasBesarAction`
   - LPJ Kas Besar tanpa workspace/anggaran mengambil akun debit dari rincian biaya LPJ.
   - Perhitungan per baris tetap memperhitungkan PPN dan kebijakan PPh.
   - Dokumen operasional tanpa anggaran tidak lagi dilewati diam-diam oleh posting massal.

6. `DraftJurnalApiHelper`
   - Penerusan identitas pelaku diperbaiki untuk jalur posting Kantin dari Draft Jurnal.

7. `pom.xml`
   - Menambahkan dependency `org.json` khusus test agar 20 source UAT dapat dikompilasi
     oleh build Maven standar.

## Perubahan Klien yang Perlu Ikut Rilis

`lib/widgets/app_shell.dart` memberi `ValueKey` berbeda untuk setiap submenu posting.
Perpindahan Posting Penjualan, HPP, dan Kulakan sekarang membentuk state layar baru sehingga
panel menu sebelumnya tidak tertinggal.

## Verifikasi Build

Build deploy final dijalankan dengan:

```powershell
& '<maven>\bin\mvn.cmd' package
```

Kompilasi source utama dan 20 source UAT berhasil. Maven menyelesaikan packaging WAR dengan
status `BUILD SUCCESS`. Probe parser tanggal juga lulus untuk nilai `2026-09-04 08:00:00`.

## Pemeriksaan Setelah Deploy

Setelah WAR dipasang dan server selesai restart, jalankan ulang pemeriksaan berikut:

1. Login eBisnis dan buka Riwayat Penjualan pada pilihan Semua Toko.
2. Posting sedikitnya 50 transaksi Penjualan Kantin yang siap.
3. Posting HPP setelah akun HPP dan Persediaan di Kelompok/Master Aset lengkap.
4. Posting Kulakan/BAST setelah akun Persediaan dan akun lawan penerimaan/vendor lengkap.
5. Buat, setujui, dan posting satu Reimbursement, lalu lanjutkan data volume.
6. Posting seluruh LPJ Kas Besar tanpa anggaran.
7. Cocokkan Draft Jurnal, Jurnal, Buku Besar, Neraca Saldo, Laba Rugi, Neraca, dan Arus Kas.

## Prasyarat Data Master (Bukan Bug Kode)

Deployment tidak boleh mengarang akun akuntansi. Agar UAT seluruh lini lulus, data sample tetap
harus mempunyai pemetaan eksplisit berikut:

- akun Persediaan/Transaksi dan HPP pada Kelompok Aset atau Master Aset produk Kantin;
- akun penerimaan BAST/utang vendor dan akun cara bayar;
- akun Jenis Reimbursement/Jenis Pengeluaran;
- pemetaan akun ke Kelompok Laporan agar saldo yang sudah diposting muncul di laporan resmi.

Konfigurasi tersebut harus dilengkapi pada data demo setelah deploy, kemudian UAT bergambar
diulang untuk menghasilkan bukti final tanpa baris kosong atau gagal posting.
