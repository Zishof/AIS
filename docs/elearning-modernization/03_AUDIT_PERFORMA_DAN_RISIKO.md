# Audit Performa dan Risiko Modernisasi e-Learning

## Risiko prioritas tinggi

1. **Monolit action ZK.** `TampilanELearningAction` sangat besar dan menggabungkan query, komponen, event, statistik, serta navigasi. Reimplementasi langsung berisiko kehilangan fungsi.
2. **JSP dengan query dan JavaScript inline.** Banyak include saling bergantung pada ID acak, fungsi global, parameter, dan urutan pemuatan.
3. **N+1 dan data besar.** Pertemuan, peserta, akses materi, kehadiran, dan nilai berpotensi menghasilkan ribuan query. Cache/warmup existing tidak boleh dilewati.
4. **Operasi ujian berisiko tinggi.** Autosave, koreksi, hitung ulang, token, acak soal, dan insiden koneksi memerlukan transaksi serta audit yang konsisten.
5. **Hak akses multi-role.** Dosen/guru, mahasiswa/siswa, admin PT/sekolah/yayasan memiliki scope berbeda. UI tidak boleh menjadi security boundary.
6. **Aset CDN.** Summernote, jQuery, Chart.js, animate.css, dan FullCalendar masih bergantung jaringan; kegagalan CDN dapat mematikan sebagian UI.

## Temuan performa existing

- Sudah tersedia cache ringkasan persisten, warmup latar, streaming session, dan indeks database khusus e-Learning.
- Beberapa loader sudah memakai paging/lazy fetch dan pemuatan paralel.
- Beberapa JSP masih membuka session Hibernate langsung; disiplin close/disconnect tidak seragam.
- Dashboard dan OBE memiliki query agregat berat sehingga wajib tetap server-side, terfilter, dan berpaging.

## Guardrail implementasi

- Tidak menyalin data besar ke browser hanya untuk filter/paging.
- Tidak menjalankan query per kartu/baris bila agregat dapat diambil sekaligus.
- Pertahankan `ElearningRingkasanCache` dan `ElearningFlagAccessCache`.
- Semua request tulis harus memvalidasi current user, role, institusi, kepemilikan kelas, dan CSRF sesuai mekanisme aplikasi.
- HTML dinamis harus di-escape; materi rich text hanya melalui sanitizer existing.
- Jangan mengubah schema, credential, atau data produksi tanpa persetujuan.

## Baseline dan target verifikasi

| Area | Baseline yang harus direkam | Target |
|---|---|---|
| Dashboard | TTFB, jumlah query, heap sesudah muat | tidak lebih lambat dari existing; tidak ada query per kartu |
| Daftar kelas | data 10/50/100+ kelas | paging/filter tetap responsif |
| Materi | kelas 16 pertemuan, banyak media | lazy load; tidak memuat media penuh di awal |
| Tugas/nilai | 30/100/500 peserta | paging server-side dan ekspor terpisah |
| Ujian | autosave dan monitoring | tidak kehilangan jawaban; operasi idempotent |
| Diskusi | topik/balasan besar | pagination dan sanitasi |
| Kalender | event satu semester | event dimuat per rentang |
| OBE | CPL/CPMK/Sub-CPMK besar | agregasi server-side, ekspor terkontrol |

## Status baseline saat Fase 1

Build dan tes JSP/runtime harus dilakukan setelah shell masuk. Pengukuran role aktual memerlukan data dan sesi login lokal yang mewakili dosen, mahasiswa, guru, siswa, dan admin; hasilnya tidak boleh diklaim hanya dari mockup statis.

