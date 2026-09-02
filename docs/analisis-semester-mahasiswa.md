# Analisis dan Resolver Semester Mahasiswa

## Tujuan

Dokumen ini menjelaskan mengapa angka semester pada daftar Mahasiswa pernah maju dari semester 2 menjadi semester 4, walaupun operator merasa periode akademik yang masih berlaku adalah 2025/2026 Genap. Dokumen ini juga menjadi kontrak pemeliharaan untuk `SemesterMahasiswaAnalisisHelper`, link semester pada `MahasiswaAction`, dan popup "Analisis Semester Mahasiswa". Pembaca perlu membedakan tiga data yang namanya mirip tetapi fungsinya berbeda: nomor semester tempuh mahasiswa, Rencana Tahun Akademik, dan Kalender Akademik.

Nomor semester tempuh adalah angka relatif terhadap perjalanan studi seorang mahasiswa, misalnya semester 1, 2, 3, dan seterusnya. Rencana Tahun Akademik menentukan periode institusi yang sedang berlaku, misalnya 2025/2026 Genap. Kalender Akademik berisi kegiatan dan rentang waktu untuk membuka atau menutup fungsi seperti KRS, ujian, penginputan nilai, dan aktivitas lain. Centang aktif pada Kalender Akademik tidak otomatis menjadikan baris tersebut sumber periode bagi `Mahasiswa.currentSemester()`. Resolver semester lama membaca Rencana Tahun Akademik; ketika tidak menemukannya, resolver memakai tanggal server.

## Penyebab insiden

Implementasi lama pada `Mahasiswa.currentSemester()` memanggil `Common.isNowSemensterGanjil()` dan `Common.getSemester(...)`. Kedua fungsi periode umum tersebut mencari Rencana Tahun Akademik menggunakan `Common.getCurrentUser()`. Pada halaman administrasi, current user adalah admin yang sedang login, bukan mahasiswa pada baris grid. Jika admin mempunyai scope fakultas, prodi, program, sekolah, atau yayasan yang berbeda, Rencana Tahun Akademik yang terpilih dapat berbeda dari RTA yang seharusnya berlaku bagi mahasiswa. Bahkan bila admin tidak mempunyai scope, baris global atau baris lain dengan skor tertentu dapat menjadi hasil resolusi.

Masalah kedua muncul saat tidak ada RTA yang rentang tanggalnya meliputi tanggal server. Fungsi umum tidak mempertahankan periode akademik terakhir. Ia langsung menebak tahun akademik dan Ganjil/Genap dari bulan. Pada 2 September 2026, fallback menganggap periode 2026/2027 Ganjil. Jika data kampus baru dikonfigurasi sampai 2025/2026 Genap, tanggal server sudah maju sementara konfigurasi operasional kampus belum. Akibatnya mahasiswa angkatan 2025 dapat terlihat naik satu atau lebih semester. Besarnya kenaikan juga dipengaruhi oleh `semesterMulai` dan `pindahKeKampusIniMasukSemester`. Karena itu angka 4 tidak boleh langsung dianggap berasal dari KRS; angka tersebut adalah hasil pemetaan periode dan offset awal studi.

Audit juga menemukan ketidaksesuaian antara dokumentasi dan implementasi skor scope RTA. Baris RTA yang mempunyai scope khusus seharusnya gugur ketika konteks pemanggil tidak mempunyai nilai pembanding. Implementasi lama membiarkannya lolos dengan skor nol. Akibatnya admin global tanpa prodi dapat memilih salah satu RTA khusus milik prodi lain, lalu kandidat dengan tanggal mulai paling baru menang. Pemeriksaan ini telah dikoreksi: nilai scope RTA yang terisi hanya cocok bila konteks pemanggil juga terisi dan identitasnya sama. Admin global harus mempunyai RTA global, sedangkan resolver per mahasiswa dapat memilih RTA khusus yang memang cocok dengan mahasiswa tersebut.

Masalah ketiga berada di renderer daftar mahasiswa. Versi lama memanggil `Common.singkronkanKrsMahasiswa(...)` hanya untuk menampilkan satu baris. Sinkronisasi adalah operasi bisnis yang dapat membuat atau memperbarui kepala KRS. Membuka layar seharusnya tidak melakukan mutasi. Jika semester hasil fallback sudah salah, renderer berpotensi membuat kepala KRS kosong pada semester yang salah. Kepala KRS kosong lalu dapat terlihat seperti jejak akademik, padahal mahasiswa belum pernah mengambil mata kuliah. Implementasi baru menggantinya dengan `Common.ambilKrsMahasiswaTanpaSinkronisasi(...)`.

## Algoritma baru

Resolver baru selalu bekerja dalam konteks mahasiswa yang sedang dirender. Fakultas diambil dari prodi mahasiswa; prodi, program, status awal, dan tahun angkatan diteruskan secara eksplisit ke resolver RTA. Urutan keputusan adalah sebagai berikut.

1. Cari RTA yang rentang tanggalnya meliputi hari ini dan semua scope yang terisi cocok dengan mahasiswa. Jika ditemukan, gunakan nama tahun akademik dan jenis semesternya.
2. Jika tidak ada RTA yang meliputi hari ini, cari RTA lampau terbaru yang masih cocok dengan konteks mahasiswa. Periode ini dipertahankan sampai administrator membuat periode berikutnya. Ini adalah perilaku fail-closed: kekosongan konfigurasi tidak boleh diam-diam menaikkan semester.
3. Jika instalasi sama sekali tidak mempunyai RTA yang cocok, gunakan fallback tanggal server untuk menjaga kompatibilitas instalasi lama. Popup memberikan peringatan tegas bahwa sumber ini lebih lemah dan perlu diganti dengan RTA eksplisit.
4. Hitung nomor semester memakai fungsi domain lama `Common.getSemester(...)`. Rumus dasarnya mengubah tahun akademik dan Ganjil/Genap menjadi urutan setengah-tahunan, menghitung selisih dari periode masuk, lalu menambahkan semester masuk efektif. Nilai pindahan atau alih prodi di atas 1 akan menggeser hasil.
5. Gunakan angka yang sama untuk pencarian KRS baca-saja, status mahasiswa pada baris, tahapan, teks tampilan, dan link popup. Tidak boleh ada satu komponen yang kembali memanggil `currentSemester()` secara terpisah karena hasilnya dapat memakai konteks berbeda.

Pilihan mempertahankan RTA lampau terakhir dibuat untuk mengatasi celah konfigurasi. Misalnya RTA 2025/2026 Genap berakhir pada 31 Agustus dan RTA 2026/2027 Ganjil belum dibuat pada 2 September. Sistem tetap menampilkan semester berdasarkan 2025/2026 Genap. Setelah RTA baru dibuat dengan tanggal dan scope yang benar, pencarian atau refresh daftar akan memakai periode baru. Tidak diperlukan perubahan manual pada nomor semester mahasiswa.

## Isi popup analisis

Angka semester pada kolom `Status/Awal/Smt` sekarang berupa link. Proses render hanya membuat snapshot ringan dari cache RTA. Query riwayat KRS baru dijalankan saat link diklik, sehingga daftar massal tidak menerima query tambahan untuk setiap bagian analisis. Popup menampilkan mahasiswa, prodi, program, status awal, tanggal analisis, tahun angkatan, semester mulai, offset pindahan/alih prodi, periode efektif, sumber periode, RTA menurut konteks mahasiswa, RTA menurut konteks user login, hasil resolver lama, hasil resolver baru, dan bukti KRS.

Riwayat KRS dibedakan menjadi kepala KRS tersimpan dan KRS yang benar-benar mempunyai SKS. Perbedaan ini penting. Kepala KRS 0 SKS dapat berasal dari proses lama atau sinkronisasi administratif dan tidak membuktikan mahasiswa pernah mengambil KRS. Popup juga membandingkan semester tertinggi yang mempunyai SKS dengan semester efektif. Jika KRS aktual lebih tinggi daripada semester efektif, operator diminta memeriksa periode KRS, tahun angkatan, atau data pindahan sebelum mengubah apa pun.

Popup bersifat read-only. Ia tidak menyimpan mahasiswa, tidak membuat KRS, tidak menghitung ulang status, dan tidak mengubah konfigurasi. Tujuannya adalah memberi bukti dan langkah perbaikan yang dapat ditindaklanjuti, bukan melakukan koreksi otomatis yang berisiko mengubah data akademik historis.

## Langkah penanganan operator

Jika popup menyatakan RTA saat ini tidak ditemukan, buka master Rencana Tahun Akademik. Pastikan ada baris yang mencakup tanggal operasional sekarang. Isi Tahun Akademik dan Ganjil/Genap dengan benar. Periksa tanggal mulai dan tanggal selesai. Scope kosong berarti global, sedangkan scope yang diisi harus cocok dengan fakultas, prodi, program, status awal, atau angkatan mahasiswa. Hindari dua RTA tumpang tindih dengan scope yang sama karena hasil akan dipilih berdasarkan skor kekhususan dan tanggal mulai terbaru.

Jika popup menunjukkan RTA konteks admin berbeda dari RTA konteks mahasiswa, koreksi scope RTA. Jangan mengubah tahun angkatan atau semester mahasiswa hanya untuk menyesuaikan tampilan. Jika offset semester masuk pindahan/alih prodi lebih besar dari 1 padahal mahasiswa bukan pindahan, koreksi biodata sumber tersebut. Setelah perbaikan, klik Cari atau Refresh pada daftar mahasiswa.

Kalender Akademik tetap perlu konsisten dengan RTA, tetapi perbaikannya berbeda. Kalender Akademik mengatur jadwal kegiatan. RTA memasok identitas periode bagi resolver. Menambah kegiatan Kalender Akademik tanpa membuat RTA tidak menghilangkan fallback tanggal server. Sebaliknya, membuat RTA tanpa jadwal Kalender Akademik dapat menghasilkan angka semester yang benar tetapi fitur KRS atau nilai masih tertutup.

## Aturan bagi pengembang dan AI berikutnya

Jangan memanggil `singkronkanKrsMahasiswa` dari renderer, getter tampilan, tooltip, popup informasi, atau proses pencarian. Gunakan `ambilKrsMahasiswaTanpaSinkronisasi` untuk kebutuhan baca. Sinkronisasi hanya layak dijalankan oleh perintah eksplisit seperti simpan KRS, hitung ulang, proses tagihan, atau refresh bisnis yang memang terdokumentasi melakukan mutasi.

Jangan memakai `Common.getCurrentUser()` untuk menentukan periode milik mahasiswa lain. Selalu teruskan dimensi mahasiswa secara eksplisit. Jangan menganggap KRS terakhir sebagai sumber tunggal semester berjalan, sebab mahasiswa baru dapat belum mempunyai KRS dan mahasiswa cuti dapat mempunyai jeda KRS. KRS adalah bukti pembanding, bukan jam akademik utama.

Jika algoritma ini hendak dipakai di halaman lain, gunakan `SemesterMahasiswaAnalisisHelper.ringkas(mahasiswa)` dan angka `getSemesterEfektif()`. Gunakan `SemesterMahasiswaAnalisisPopupHelper.pasangLink(...)` untuk UI ZK. Jangan menyalin rumus atau HTML popup ke Action lain. Dengan satu resolver dan satu renderer popup, penjelasan di setiap halaman akan tetap sama ketika aturan akademik berubah.

Terakhir, setiap perubahan pada batas fallback bulan, aturan skor scope RTA, atau rumus `Common.getSemester` harus diuji untuk mahasiswa reguler, mahasiswa mulai Genap, pindahan, alih prodi, mahasiswa tanpa KRS, kepala KRS 0 SKS, KRS aktual, admin scope global, admin scope prodi, periode RTA aktif, jeda antar-RTA, dan instalasi tanpa RTA. Pengujian tersebut mencegah masalah yang sama muncul kembali dalam bentuk angka semester yang tampak benar di satu halaman tetapi berbeda di halaman lain.
