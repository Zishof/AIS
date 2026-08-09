# Mapping UI Lama ke 10 Layar Modern

Prinsip implementasi: shell dan presentasi diperbarui, sedangkan JSP/service existing menjadi sumber data dan tindakan. Tidak ada tombol mockup yang dianggap selesai sebelum terhubung ke fungsi nyata.

| Mockup | Target UI baru | Sumber fungsi/data existing | Status |
|---|---|---|---|
| 01 Dashboard Dosen/Guru | Ringkasan role pengajar | `dasbor.jsp`, service dasbor, statistik action | Shell terpasang; komponen dashboard dalam migrasi |
| 02 Dashboard Mahasiswa/Siswa | Ringkasan role peserta | `ringkasan.jsp`, linimasa, tugas/ujian | Shell terpasang; komponen dashboard dalam migrasi |
| 03 Daftar Mata Kuliah/Kelas | Mata Kuliah / Kelas | `ringkasan.jsp`, filter pembelajaran | Fungsi existing dipertahankan |
| 04 Workspace Mata Kuliah | detail kelas/pertemuan | `pertemuan.jsp`, `load_ringkasan.jsp` | Perlu layout workspace per kelas |
| 05 Materi & Modul | Materi & Modul | `materi.jsp`, audio/video, buat pertemuan | Fungsi existing dipertahankan |
| 06 Tugas & Penilaian | Tugas | `tugas.jsp`, folder tugas dan service | Fungsi existing dipertahankan |
| 07 Ujian & Monitoring | Ujian & Kuis | `ujian.jsp`, hasil/koreksi/analisis/service | Fungsi existing dipertahankan; monitoring live perlu audit lanjutan |
| 08 Diskusi & Kolaborasi | Diskusi | `diskusi.jsp` dan service diskusi | Fungsi existing dipertahankan |
| 09 Kalender & Presensi | Kalender & Presensi | `kalender.jsp`, presensi per pertemuan | Fungsi existing dipertahankan |
| 10 Gradebook/Analitik/OBE | Nilai & Analitik | `obe.jsp`, laporan, asesmen dan service OBE | Fungsi existing dipertahankan |

## Navigasi V1

Shell sekarang menyediakan: Ringkasan, Mata Kuliah/Kelas, Linimasa, Materi & Modul, Tugas, Ujian & Kuis, Diskusi, Kalender & Presensi, Nilai & Analitik, dan Laporan. Pada desktop berupa rail kiri; pada tablet/ponsel berubah menjadi pemilih bagian. Bagian terakhir disimpan per role di `sessionStorage`, dapat dibuka melalui hash `#el-...`, dan tetap memakai Bootstrap tab existing.

## Aturan parity

1. Setiap tombol lama tetap berada di JSP domainnya sampai padanan modern terbukti berfungsi.
2. Tombol mockup baru hanya boleh diaktifkan bila endpoint dan privilege server-side tersedia.
3. Include JSP, parameter, ID elemen, serta fungsi reload tidak diubah tanpa tes regresi.
4. Role pengajar, peserta, sekolah, dan pengelola harus diuji terpisah.
5. Layar kosong, loading, gagal, forbidden, dan data besar merupakan acceptance state, bukan kasus tambahan.

## Tahap berikut

- Ekstrak adapter/view-model read-only untuk dashboard role tanpa memindahkan logika bisnis ke JSP.
- Pecah detail kelas menjadi workspace dengan tab per course.
- Selaraskan kartu/filter/table domain satu per satu.
- Tambahkan endpoint monitoring ujian hanya jika data existing dan otorisasinya memadai.

