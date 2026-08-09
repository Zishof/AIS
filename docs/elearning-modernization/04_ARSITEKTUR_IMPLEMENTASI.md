# Arsitektur Implementasi Modernisasi e-Learning

## Prinsip

Code existing tetap menjadi sumber kebenaran. `TampilanELearningAction`, model Hibernate, JSP service, URL download/upload, modal, dan privilege lama tidak diganti. Lapisan baru hanya mengatur shell, navigasi role-based, konteks mata kuliah, responsivitas, serta penanganan error.

## Struktur runtime

1. `index.jsp` memvalidasi user dan membaca `elearning_workspace_modern_aktif`.
2. Nilai `true` membuka workspace modern; nilai `false` menyertakan `index_classic.jsp`.
3. Sepuluh layar tetap berupa JSP existing dan memakai endpoint existing.
4. Kartu mata kuliah memancarkan event `ais:elearning:course`; shell menyimpan konteks per role di `sessionStorage` dan menerapkannya pada filter halaman tujuan.
5. Semua KPI dashboard dapat dipilih dengan mouse atau keyboard dan membuka tabel rinci existing.
6. Promise rejection dan JavaScript error yang tidak tertangani ditampilkan pada error boundary tanpa menyembunyikan pesan dari pengguna.

## Pemetaan 10 layar

| Layar paket | Implementasi AIS |
|---|---|
| Dashboard dosen/guru | `dasbor.jsp` + hero/aksi cepat role pengajar |
| Dashboard peserta | `dasbor.jsp` + hero/aksi cepat role peserta |
| Daftar mata kuliah/kelas | `ringkasan.jsp` |
| Workspace mata kuliah | context bar di `index.jsp`, katalog, linimasa, dan tab domain |
| Materi/modul/pertemuan | `materi.jsp`, `linimasa.jsp`, `pertemuan.jsp`, media existing |
| Tugas/penilaian | `tugas.jsp`, `tugas_kelompok.jsp`, services existing |
| Ujian/monitoring | `ujian.jsp`, folder `ujian/`, services existing |
| Diskusi/kolaborasi | `diskusi.jsp` dan service diskusi existing |
| Kalender/presensi | `kalender.jsp`, FullCalendar, halaman presensi dan QR existing |
| Gradebook/analitik/OBE | `obe.jsp`, folder `obe/`, laporan dan portofolio existing |

Tidak ada tabel atau migration database baru pada fase ini.
