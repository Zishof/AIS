# Cetak Aktifitas PAKEM Mahasiswa

## Tujuan dan ruang lingkup

Fitur **Cetak Aktifitas** menyediakan dokumen resmi PAKEM berdasarkan format `FORM PEDOMAN AKTIFITAS KEGIATAN MAHASISWA.docx`. Tombol tersedia pada toolbar tab **Kegiatan Kemahasiswaan** ketika layar sedang menampilkan satu mahasiswa. Kondisi ini mencakup login mahasiswa, karena helper layar telah menerima objek mahasiswa milik pengguna yang sedang login, dan juga layar pengelola ketika mahasiswa tertentu dipilih. Satu kali klik menghasilkan satu PDF yang berisi dua bagian: **Form A PAKEM Nilai Semester** dan **Form B PAKEM Kumulatif**. Kedua bagian dibuat oleh template Jasper yang berbeda, lalu digabung supaya urutan halaman, pemeliharaan query, dan struktur masing-masing formulir tetap jelas.

Implementasi tidak mengubah tabel, tidak menambah kolom, dan tidak memindahkan data. Seluruh proses bersifat baca-saja. Data laporan tetap berasal dari struktur kegiatan kemahasiswaan yang telah digunakan oleh laporan Angka Kredit dan Rekap Angka Kredit sebelumnya. Dengan demikian fitur baru tidak membuat sumber kebenaran kedua dan tidak memiliki risiko mengubah persetujuan maupun nilai aktivitas mahasiswa.

## Titik masuk aplikasi

Tombol dibuat oleh `MahasiswaPunyaKegiatanKemahasiswaanHelper.display(...)`. Tombol hanya ditambahkan apabila parameter `mahasiswa` tidak null. Event klik meneruskan objek tersebut ke `CommonReportHelper.onCetakAktifitasMahasiswa(...)`. Method report helper melakukan validasi bahwa mahasiswa tidak null dan sudah memiliki ID database. Setelah itu helper menentukan semester berjalan, membangun parameter identitas, membuat Form A, membuat Form B, menggabungkan dua PDF dengan `PDFMergerUtility`, dan menampilkan hasil melalui mekanisme standar `Report.tampil(...)`.

Parameter laporan menggunakan generator yang sama dengan laporan kegiatan kemahasiswaan yang sudah ada. Parameter utamanya adalah ID mahasiswa, nama, NIM, program studi, tahun angkatan, semester awal saat masuk ke kampus, jenis semester masuk, semester aktif, dan batas semester kumulatif. Cara ini penting karena mahasiswa pindahan dapat mulai dari semester selain semester satu. Semester kumulatif minimal dicetak sampai semester delapan. Apabila semester mahasiswa lebih besar dari delapan, laporan diperluas sampai semester berjalan agar aktivitas lanjutan tidak hilang.

## Algoritma penentuan semester

Nomor semester suatu aktivitas tidak diambil dari urutan baris, tanggal input, atau semester global yang sedang aktif. Nomor semester dihitung dari periode akademik aktivitas relatif terhadap periode masuk mahasiswa. Rumus konseptualnya adalah:

`semester awal mahasiswa + indeks periode aktivitas - indeks periode masuk mahasiswa`

Setiap tahun akademik mempunyai dua indeks periode. Ganjil menggunakan indeks tahun dikali dua, sedangkan Genap menambahkan satu. Contoh: mahasiswa angkatan 2025 yang masuk Ganjil dan memiliki semester awal 1 akan memperoleh semester 1 untuk kegiatan 2025/Ganjil, semester 2 untuk 2025/Genap, dan semester 3 untuk 2026/Ganjil. Jika mahasiswa pindahan tercatat masuk pada semester 3, perhitungan dimulai dari 3 tanpa menganggap mahasiswa tersebut sebagai semester 1. Rumus ini sama dengan pola yang digunakan laporan kegiatan sebelumnya, sehingga Form A dan Form B tidak memiliki definisi semester yang saling bertentangan.

Baris dengan `tahunakademik` yang tidak diawali empat digit tidak diproses karena tidak dapat dipetakan secara aman menjadi semester. Data seperti itu perlu diperbaiki pada master kegiatan, bukan ditebak oleh laporan. Jenis semester kosong diperlakukan sebagai Ganjil untuk mempertahankan kompatibilitas dengan data lama, sesuai perilaku laporan angka kredit yang telah digunakan sebelumnya.

## Syarat aktivitas masuk laporan

Aktivitas hanya masuk apabila semua kondisi berikut terpenuhi:

1. Peserta kegiatan menunjuk ke mahasiswa yang sedang dicetak.
2. Status kegiatan adalah `Disetujui`.
3. Persetujuan peserta bernilai benar.
4. Kelompok kegiatan masih aktif.
5. Detail kelompok kegiatan masih aktif.
6. Tahun akademik dapat dipetakan menjadi nomor semester.
7. Untuk Form A, hasil nomor semester harus sama dengan semester aktif mahasiswa.

Pemisahan status kegiatan dan persetujuan peserta disengaja. Status kegiatan menyatakan bahwa kegiatan induk sah, sedangkan persetujuan peserta menyatakan bahwa keikutsertaan mahasiswa tersebut telah diverifikasi. Memasukkan salah satunya saja dapat menampilkan pengajuan yang belum disetujui atau kegiatan yang belum final. Form B membuat daftar semester terlebih dahulu dengan `generate_series`, kemudian memasangkan aktivitas yang lolos. Akibatnya semester tanpa aktivitas tetap muncul dengan total nol dan bukan menghilang dari rekap.

## Makna nilai dan kolom PAKEM

Nilai resmi diambil dari `nilai_kegiatan_kemahasiswaan.nilai` berdasarkan kombinasi detail kelompok kegiatan, skala kegiatan, dan jabatan peserta. Nilai tersebut adalah nilai akhir rubrik yang juga digunakan oleh laporan angka kredit. Apabila kombinasi nilai belum ditemukan, laporan menggunakan nol sehingga kekurangan konfigurasi terlihat dan tidak menghasilkan nilai null yang membingungkan.

Dokumen Word menyediakan kolom IW, IS, IPr, IOr, dan Jumlah. Namun model database saat implementasi ini tidak menyimpan empat komponen tersebut sebagai empat angka terpisah. Model hanya menyimpan nilai akhir untuk satu kombinasi rubrik. Oleh sebab itu sistem tidak membagi nilai akhir secara arbitrer ke IW, IS, IPr, atau IOr. Form A mempertahankan empat kolom itu sebagai ruang verifikasi/manual dan menempatkan nilai resmi pada kolom **Jumlah**. Catatan pada laporan menjelaskan keputusan ini. Aturan ini wajib dipertahankan sampai tersedia perubahan model data yang secara eksplisit merekam setiap komponen. Menyalin nilai akhir ke semua kolom, membagi rata, atau menyimpulkan komponen dari nama skala dilarang karena akan menciptakan data resmi yang tidak mempunyai bukti sumber.

## Isi kedua formulir

Form A menampilkan identitas mahasiswa, tingkat/semester berjalan, daftar aktivitas yang telah disetujui pada semester tersebut, nilai per aktivitas, jumlah angka kredit, arti singkatan komponen, dan ruang tanda tangan Ketua Prodi serta Kemahasiswaan Prodi. Urutan aktivitas menggunakan tanggal mulai, nama kegiatan, lalu ID agar stabil ketika beberapa kegiatan memiliki tanggal yang sama.

Form B menampilkan identitas yang sama dan rekap semester I sampai sekurang-kurangnya VIII. Di dalam setiap semester, aktivitas diberi nomor ulang mulai dari satu. Setiap kelompok semester mempunyai total sendiri, dan bagian akhir mempunyai total kumulatif seluruh semester. Form B juga menyediakan ruang tanda tangan Wakil Rektor Mutu Pendidikan, Kemahasiswaan dan Prestasi serta Kepala Biro Kemahasiswaan.

## Penanganan kegagalan dan konsistensi file

Form A dan Form B dibuat ke file sementara melalui `Report.generateFileReport(...)`. Parameter untuk masing-masing report disalin ke map baru agar perubahan internal saat satu report diproses tidak mengotori parameter report berikutnya. Penggabungan baru dilakukan setelah kedua file berhasil dibuat. Stream keluaran selalu ditutup pada blok `finally`, termasuk ketika PDFBox melempar exception. File gabungan kemudian dibuka memakai penampil laporan standar aplikasi, sehingga perilaku preview, unduh, dan keamanan sesi mengikuti laporan lain.

Jika pembuatan salah satu formulir gagal, file gabungan tidak ditampilkan. Exception diteruskan ke mekanisme penanganan error layar yang sudah ada. Jangan mengganti kegagalan ini dengan PDF kosong karena pengguna dapat menyangka data PAKEM memang kosong, padahal laporan gagal dibentuk.

## Panduan SIT dan UAT

Pengujian minimal harus menggunakan mahasiswa dengan variasi berikut: mahasiswa semester aktif yang mempunyai aktivitas disetujui; mahasiswa tanpa aktivitas; mahasiswa dengan pengajuan belum disetujui; mahasiswa yang mempunyai aktivitas pada beberapa semester; mahasiswa pindahan dengan semester awal lebih dari satu; serta mahasiswa yang rubrik nilainya belum lengkap. Pastikan tombol terlihat di akun mahasiswa dan layar pengelola ketika objek mahasiswa tersedia.

Pada Form A, cocokkan hanya aktivitas semester berjalan dan jumlah nilainya dengan layar Angka Kredit. Pada Form B, cocokkan aktivitas per semester, total per semester, dan total kumulatif. Pastikan aktivitas yang belum disetujui tidak muncul. Untuk mahasiswa tanpa aktivitas, semua semester tetap tersedia dan totalnya nol. Untuk mahasiswa pindahan, pastikan semester tidak bergeser ke semester satu. Uji juga kegiatan dengan nama panjang agar baris bertambah tinggi tanpa menimpa baris berikutnya, serta data yang cukup banyak agar pergantian halaman tidak memotong header tabel secara salah.

Validasi teknis yang telah dilakukan saat implementasi adalah kompilasi penuh 7.505 source Java dengan Maven dan kompilasi langsung kedua JRXML memakai `JasperCompileManager`. UAT tetap diperlukan dengan koneksi database kampus karena validitas isi ditentukan oleh data referensi, status persetujuan, dan konfigurasi rubrik milik institusi.

## Pengembangan berikutnya

Apabila kampus menghendaki IW, IS, IPr, dan IOr terisi otomatis, pekerjaan berikutnya bukan sekadar perubahan JRXML. Harus disepakati definisi setiap indeks, ditambahkan struktur penyimpanan yang dapat diaudit, dibuat form input dan validasi, disiapkan migrasi Hibernate, serta ditentukan bagaimana data lama diperlakukan. Setelah keempat komponen tersimpan secara resmi, Form A dapat membaca kolom tersebut dan memvalidasi bahwa jumlah komponen sama dengan nilai akhir. Sampai perubahan domain tersebut selesai, perilaku sekarang adalah pilihan yang paling akurat dan aman.
