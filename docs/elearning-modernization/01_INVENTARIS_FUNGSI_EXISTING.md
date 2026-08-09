# Inventaris Fungsi Existing e-Learning AIS

Tanggal audit: 9 Agustus 2026. Acuan visual: paket 10 mockup 4K Modernisasi e-Learning V1.

## Entry point dan implementasi aktif

- ZK legacy: `TampilanELearningAction.java` (lebih dari 10.000 baris) dan halaman ZUL Tampilan e-Learning.
- JSP portal: `WEB-INF/o/ux/elearning.jsp` dan `elearning_detail.jsp`.
- JSP modern existing: `WEB-INF/baru/modul/elearning/index.jsp`.
- REST/mobile: `ElearningPertemuanApi`, `ElearningApiUtil`, `ApiBaruElearning`, dan `ELearningResource`.
- Optimasi: `ElearningRingkasanCache`, `ElearningFlagAccessCache`, `ElearningWarmupHelper`, serta indeks khusus di `InitIndex`.

## Matriks fungsi yang wajib dipertahankan

| Domain | Fungsi existing yang ditemukan | Implementasi |
|---|---|---|
| Kelas | filter TA/semester/fakultas/jurusan/sekolah, daftar perkuliahan/pelajaran, peserta, progres | `ringkasan.jsp`, `linimasa.jsp`, service ringkasan/linimasa |
| Pertemuan | tambah satu/massal, ubah, hapus satu/semua, publikasi, akses, statistik, detail | `buat_pertemuan.jsp`, `pertemuan.jsp`, `_pertemuan_service.jsp` |
| Presensi | daftar peserta, hadir/izin/sakit/alpa, pengajuan dan koreksi, rekap | JSP daftar presensi/pengajuan, `ubah_status_kehadiran.jsp` |
| Materi/media | materi, file, referensi, video, audio, unduh, statistik akses | `materi.jsp`, `audio.jsp`, `video.jsp`, loader dan service linimasa |
| Tugas | individu/kelompok, buat/ubah, upload, download semua, format/rubrik nilai | `tugas.jsp`, folder `tugas/`, service tugas |
| Ujian | buat/ubah, bank/daftar soal, AI soal, acak, hasil, koreksi, hitung ulang, ekspor | `ujian.jsp`, folder `ujian/`, service ujian/analisis |
| Diskusi | topik/balasan, diskusi lintas, simpan, muat, hapus | `diskusi.jsp`, `_diskusi.jsp`, service diskusi |
| Kalender | agenda pertemuan/tugas/ujian, filter, detail dan pemuatan event | `kalender.jsp`, `_kalender_service.jsp` |
| OBE | CPL, CPMK, Sub-CPMK, asesmen, RPS, CQI, PIKOBE, laporan, generator AI | `obe.jsp`, folder `obe/`, service OBE |
| Laporan | daftar nilai, rekap absensi, ekspor ujian, analisis soal, laporan OBE/RPS | `laporan.jsp` dan service cetak/ekspor |
| Notifikasi | email/notifikasi tugas dan diskusi, tautan kembali ke e-Learning | `CommonEmail`, `CommonNotifikasi` |
| Multi-instansi | perguruan tinggi, sekolah/yayasan, dosen/guru, mahasiswa/siswa | action, model VO pembelajaran, filter dashboard |

## Hak akses

- Autentikasi wajib memakai `Common.getCurrentUser(request)`.
- Data dibatasi oleh role, pengguna, institusi, kelas, dan relasi peserta/pengajar di query existing.
- Operasi tulis tetap harus melalui service existing yang melakukan validasi server-side.
- UI baru tidak boleh mengandalkan hide/show tombol sebagai satu-satunya otorisasi.

## Kompatibilitas yang tidak boleh rusak

- Java 7 source compatibility, ZKoss 5.5, Hibernate 3.6, PostgreSQL, JSP/Servlet, Ant/Tomcat.
- URL service `/baru?hanya_tampil_jsp=true&p=elearning/...&s=...` dan servlet `/Data`.
- ID DOM dan nama fungsi JavaScript yang dipakai reload lintas include.
- Konfigurasi `elearning_*`, dukungan bahasa, tema perguruan tinggi/sekolah, dan cache existing.

