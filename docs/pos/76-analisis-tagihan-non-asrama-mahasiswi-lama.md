# 76. Analisis Tagihan Non-Asrama Mahasiswi Lama

Tanggal: 2 September 2026

## Laporan

Mutiara Rahma Humairah (NIM `2022010066`) dan sejumlah mahasiswi lama yang
tidak memilih asrama diminta mempunyai tagihan SPP bulanan Rp1.500.000 tanpa
tagihan asrama. Tangkapan layar justru memperlihatkan item **Asrama** sebesar
Rp2.800.000 untuk September, Oktober, dan November 2026.

Lampiran kedua berisi 18 nama dan tidak mencantumkan Mutiara Rahma. Karena
pemilihan setting di kode memakai NIM, bukan nama, cakupan belum boleh dianggap
18 orang: kemungkinan ada 19 orang (Mutiara + 18 nama) dan seluruh nama harus
dipetakan ke NIM sebelum data produksi diubah.

## Hasil penelusuran kode

### 1. Nominal berasal dari setting, bukan status historis mahasiswa

`PembayaranUtil.getDetailBiayaMahasiswadariDatabase(...)` menyusun profil
mahasiswa dari angkatan, jenjang, semester, jenis pembayaran, status, prodi,
program, dan atribut lain. Profil itu diteruskan ke
`SetingBiayaAction.getSettingBiayaTerpilih(...)`, lalu sistem mengambil
`DetailBiaya` dan `PengaturanPembayaranBulanan` milik setting yang terpilih.

Kode tidak mempunyai aturan yang menyimpulkan bahwa item bernama "Asrama"
harus hilang hanya karena mahasiswa disebut "mahasiswi lama" atau secara
operasional pernah memilih tidak tinggal di asrama. Tanpa setting khusus atau
pengecualian NIM, mahasiswa tetap memakai setting cohort umum yang cocok.

### 2. Filter tempat tinggal bukan pengganti routing non-asrama

Filter `jenisTinggalMahasiswa` hanya dipakai bila konfigurasi
`tampilkan_filter_jenis_tempat_tinggal_mahasiswa_pada_billing_pembayaran`
aktif. Bila aktif, sumbernya adalah biodata terakhir
`BiodataMahasiswa.jenisTinggalMahasiswa` dan pencocokannya harus sama persis
dengan `DetailBiaya.jenisTinggalMahasiswa`.

Filter tersebut tidak membaca pilihan asrama historis, tidak membaca nama
item biaya, dan tidak otomatis menganggap nilai kosong sebagai "non-asrama".
Karena itu solusi yang aman adalah routing Setting Biaya per NIM, bukan
menebak dari nama item atau status lama.

### 3. Kode terbaru sudah menyediakan routing yang diperlukan

`SettingBiaya` menyediakan:

- **Batasi hanya untuk mahasiswa yang dipilih (tetap tagihan bulanan)**;
- **Pengecualian Mahasiswa (NIM)**;
- **Prioritas**, dengan angka lebih kecil dipilih lebih dahulu.

`SettingBiayaMahasiswaSelector` memastikan bahwa bila NIM terdaftar pada
setting terbatas, setting cohort umum tidak dicampurkan kembali. Jalur UI,
tagihan bulanan, dan host-to-host memakai satu setting induk yang sama melalui
`getSettingBiayaTerpilih(...)`.

Fitur tersebut mulai ditambahkan pada SVN `r78617`/`r78637`. Konsistensi relasi
dan seleksi per NIM diperbaiki lagi pada `r82931` dan `r82956`. Server harus
menggunakan build tersebut dan migrasi
`migrasi_relasi_prioritas_setting_biaya_20260902.sql`; bila belum, setting
khusus dapat kalah atau tercampur dengan setting umum.

### 4. Cache dapat mempertahankan tampilan lama

Tagihan disimpan dengan kunci `tagihan_mhs_<id>_<jenisKegiatan>_<semester>`.
Sesudah setting diubah, layar harus memakai Refresh/reload agar daftar tidak
diambil kembali dari cache sebelum perubahan.

## Kesimpulan akar masalah

Ini bukan kesalahan aritmetika Rp1.500.000 menjadi Rp2.800.000. Baris yang
tampil memang bersumber dari setting/item **Asrama Rp2.800.000**. Akar masalah
adalah mahasiswa non-asrama belum dirutekan secara eksklusif ke Setting Biaya
non-asrama dan/atau masih mewarisi setting asrama umum. Build/migrasi lama dan
cache dapat mempertahankan gejalanya setelah konfigurasi dikoreksi.

## Tindakan aman yang disarankan

1. Dapatkan NIM untuk seluruh 18 nama dan pastikan apakah Mutiara merupakan
   orang ke-19 atau termasuk salah satu nama dengan penulisan berbeda.
2. Pada Setting Biaya **Pembayaran Semester Non Asrama**, aktifkan pembatasan
   mahasiswa, pilih seluruh NIM tersebut, dan gunakan prioritas lebih tinggi
   (angka lebih kecil) daripada setting cohort umum.
3. Pastikan item bulanannya hanya **SPP Rp1.500.000**; jangan sertakan item
   Asrama.
4. Tambahkan NIM yang sama pada **Pengecualian Mahasiswa (NIM)** di setting
   Asrama sebagai pertahanan kedua.
5. Deploy build dan migrasi terbaru, lalu Refresh tagihan setiap NIM. Verifikasi
   minimal tiga bulan dan inquiry host-to-host/VA.
6. Bila sudah ada cicilan terposting atau VA aktif, jangan menghapus langsung.
   Rekonsiliasi dahulu agar pembayaran dan histori akuntansi tidak terputus.

Karena baru NIM Mutiara yang terlihat pada lampiran, perubahan massal produksi
belum aman dilakukan dari daftar nama saja.

## Draf balasan WhatsApp

> Baik Ibu, sudah kami cek sampai ke alur pembentukan tagihannya. Tagihan
> Rp2.800.000 yang terlihat bukan hasil salah penjumlahan SPP, tetapi berasal
> dari setting item Asrama yang saat ini masih terpilih untuk mahasiswa
> tersebut. Sistem belum otomatis mengetahui bahwa mahasiswi lama tidak memilih
> asrama apabila NIM-nya belum dimasukkan ke kelompok tagihan Non Asrama.
>
> Akan kami arahkan mahasiswa yang dimaksud ke tagihan **SPP bulanan
> Rp1.500.000** dan mengecualikannya dari tagihan Asrama. Agar tidak salah orang,
> mohon kirimkan NIM untuk 18 nama pada daftar. Mutiara Rahma Humairah sudah kami
> identifikasi dengan NIM `2022010066`, tetapi namanya tidak terlihat pada daftar
> 18 orang, jadi mohon dikonfirmasi apakah Mutiara merupakan orang tambahan atau
> termasuk salah satu nama pada daftar.
>
> Setelah NIM lengkap dan pengaturannya diterapkan, kami akan muat ulang serta
> memeriksa tagihan satu per satu. Tagihan yang diharapkan adalah SPP
> Rp1.500.000 per bulan tanpa item Asrama. Bila ada pembayaran atau VA yang sudah
> terlanjur aktif, akan kami rekonsiliasi terlebih dahulu agar tidak mengganggu
> histori pembayaran.
