# Tagihan mahasiswa mengikuti Item Biaya pada Setting Biaya

Tanggal: 4 September 2026

## Masalah

Item yang sudah dilepas dari **Setting Biaya** masih dapat muncul pada layar pembayaran mahasiswa. Contoh yang dilaporkan adalah `SPP Bulan ke 1` yang tetap terlihat, walaupun Setting Biaya hanya memuat `Dana Asrama` dan `Dana Kuliah`.

## Akar masalah

Layar pada laporan bukan modul tagihan sekolah. Jalur yang benar adalah:

- `DaftarUlangMahasiswaLamaAction` untuk mahasiswa lama;
- `DaftarUlangMahasiswaBaruAction` untuk mahasiswa baru/calon mahasiswa;
- `PembayaranUtilHelper` dan `SetingBiayaHelper` sebagai pembentuk tagihan mahasiswa.

Ada tiga penyebab item lama dapat hidup kembali:

1. Kedua halaman masih boleh mengambil cache tagihan yang dibuat sebelum Setting Biaya diubah.
2. Ketika `DetailSettingBiaya` sudah dipakai oleh `DetailBiaya`, proses simpan sebelumnya menolak menghapus relasi tersebut. Akibatnya pilihan yang dilepas oleh user masih tersimpan di database.
3. Jika hasil query kosong, halaman membentuk kembali tagihan aktif dari riwayat cicilan. Perilaku ini bertentangan dengan ketentuan bahwa item yang tidak lagi diatur tidak boleh tampil sebagai tagihan.

## Perbaikan

- Perubahan yang sebelumnya ditempatkan di `sekolah/helper/TagihanUtil` dan `TagihanUtilCalonSiswa` dibatalkan karena kedua class tersebut adalah jalur tagihan siswa sekolah.
- Kedua halaman mahasiswa selalu meminta data terbaru dari database ketika membentuk daftar tagihan.
- Fallback dari riwayat cicilan tidak lagi dipakai untuk membentuk tagihan aktif pada kedua halaman.
- Saat item dilepas dari Setting Biaya, referensi nullable dari `DetailBiaya` diputus terlebih dahulu, kemudian `DetailSettingBiaya` dihapus. Nominal dan histori transaksi tetap disimpan; yang hilang hanya status item tersebut sebagai pilihan tagihan aktif.

## Dampak operasional

- Tidak ada penghapusan histori pembayaran.
- Tidak diperlukan perubahan struktur database.
- Setelah versi baru dideploy, buka Setting Biaya terkait, pastikan hanya item yang diinginkan tercentang, lalu simpan sekali lagi agar relasi lama yang sebelumnya gagal dilepas dibersihkan.
- Server AIS perlu dibangun dan dideploy ulang; POS Desktop tidak perlu dibangun ulang.

## Verifikasi minimum

1. Pada Setting Biaya, lepas satu Item Biaya yang sudah pernah menghasilkan tagihan dan simpan.
2. Buka Pembayaran Mahasiswa Lama dan pastikan item tersebut tidak muncul.
3. Ulangi pada Pembayaran Mahasiswa Baru.
4. Pastikan histori cicilan lama masih tersedia pada daftar/riwayat pembayaran.
5. Centang kembali item tersebut, simpan, dan pastikan tagihan kembali muncul sesuai konfigurasi terbaru.
