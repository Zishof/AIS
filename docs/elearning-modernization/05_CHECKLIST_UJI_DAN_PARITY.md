# Checklist Uji dan Parity e-Learning

## Otomatis

- Jalankan `powershell -ExecutionPolicy Bypass -File tools/verify-elearning-modernization.ps1`.
- Kompilasi JSP dengan Jasper/Tomcat menggunakan Java source/target 1.7.
- Jalankan `git diff --check`.
- Pastikan `git ls-files` tidak memuat `.svn`.

## Parity fungsi existing

- Filter TA, semester, fakultas/jurusan atau yayasan/sekolah.
- Linimasa, ringkasan, materi, tugas individu/kelompok, ujian, diskusi, kalender, dashboard, OBE, dan laporan.
- Detail peserta/pertemuan/tugas/ujian dan paging.
- Tambah/edit/hapus pertemuan sesuai privilege.
- Upload/download materi, audio, video, lampiran, dan hasil ujian sesuai privilege.
- Cetak PDF, ekspor Excel, agenda, refresh, serta kalender.
- Presensi mahasiswa/dosen, izin/sakit, koreksi status, dan QR jika library tersedia.
- RPS/OBE, CPMK/CPL, portofolio, serta bukti akreditasi sesuai hak akses.

## Matriks UAT wajib di server data aktual

| Dimensi | Nilai |
|---|---|
| Role | admin, operator, dosen/guru, mahasiswa/siswa/santri, pimpinan, wali jika tersedia |
| Viewport | 1920×1080, 1366×768, 1024×768, 768×1024, 390×844, 360×800 |
| Browser | Chrome, Edge, Firefox, Safari/mobile WebView yang didukung |
| Kondisi | data penuh, tanpa data, koneksi lambat, HTTP error, session habis, file terlalu besar |

Untuk setiap kombinasi, validasi tombol aktif, data sesuai scope, tidak ada data lintas role, paging benar, modal dapat ditutup/Escape, fokus keyboard terlihat, dan tidak ada horizontal overflow pada viewport normal.
