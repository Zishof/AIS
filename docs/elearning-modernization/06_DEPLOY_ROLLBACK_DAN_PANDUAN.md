# Deploy, Rollback, dan Panduan Pengguna

## Deploy

1. Backup aplikasi dan konfigurasi sebelum deploy.
2. Deploy Java/classes existing dan webapp hasil revision yang sama.
3. Pastikan `base-elearning.css` termuat setelah CSS tema.
4. Biarkan konfigurasi `elearning_workspace_modern_aktif=true` untuk workspace modern.
5. Bersihkan cache JSP/Tomcat sesuai prosedur server, lalu restart terkontrol.
6. Jalankan smoke test login dan matriks role pada `05_CHECKLIST_UJI_DAN_PARITY.md`.

## Rollback UI cepat

Set konfigurasi `elearning_workspace_modern_aktif=false`. Request berikutnya memakai `index_classic.jsp`, sementara endpoint, model, dan data tetap sama. Rollback penuh dapat dilakukan dengan revision SVN/Git sebelumnya setelah mengikuti prosedur backup operasional.

## Penggunaan

- Gunakan sidebar desktop atau pilihan `Buka bagian` pada tablet/mobile.
- Aksi cepat berubah menurut role: pengajar, peserta, atau pengelola.
- Dari katalog mata kuliah pilih `Buka Workspace`; nama kelas akan tetap aktif saat berpindah ke Pertemuan, Materi, Tugas, Ujian, Diskusi, Presensi, atau Nilai/OBE.
- Klik kartu KPI dashboard untuk berpindah langsung ke tabel rinci; Enter dan Space juga didukung.
- Bila terjadi error JavaScript yang tidak tertangani, pesan tampil di bagian atas konten dan tidak lagi diam tanpa informasi.

## Risiko tersisa

- UAT dengan data dan role produksi tetap wajib karena checkout lokal tidak memiliki deployment AIS aktif.
- Peringatan pemindaian JAR `WEB-INF/lib/lib/*` berasal dari manifest dependency lama; kompilasi JSP tetap selesai dengan nol error.
- Library eksternal seperti FullCalendar dan QR harus tersedia sesuai kebijakan jaringan server.
