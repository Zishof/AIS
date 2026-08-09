# Analisis Gap Instrumen AMI 2026 dan Modul SPMI AIS

Tanggal analisis awal: 8 Agustus 2026

Verifikasi ulang terhadap berkas Ver08 terbaru: 9 Agustus 2026

Sumber: [INSTRUMEN AMI TAHUN 2026 Ver08](https://docs.google.com/spreadsheets/d/1UEO3cbEo4dHyH5tLXStp6rLnzkxPfV05/edit)

## Ringkasan instrumen

Workbook Ver08 terbaru berisi 16 worksheet: `COVER`, 11 worksheet kelompok indikator,
`Ringkasan Hasil Audit`, `Rekap Kesiapan Bukti`, `Laporan Tidak Memenuhi`, dan satu worksheet
tersembunyi `_DataIndikator`. Instrumen memuat 74 indikator inti dan indikator tambahan yang aktif
sesuai lembaga akreditasi program studi:

| Kelompok | Jumlah indikator |
|---|---:|
| Kompetensi Lulusan | 10 |
| Proses Pendidikan | 24 |
| Masukan Pendidikan | 22 |
| Penelitian | 10 |
| Pengabdian kepada Masyarakat | 8 |
| LAMDIK | 6 |
| BAN-PT | 6 |
| LAMEMBA | 8 |
| LAMINFOKOM | 42 |
| LAMSPAK | 5 |
| LAMWISATA | 7 |

Total data master dalam workbook adalah 148 indikator. Ruang lingkup satu audit adalah 74 indikator
inti ditambah satu kelompok akreditasi yang dipilih, sehingga jumlah indikator aktif berkisar 79 sampai 116.

Setiap indikator memakai data: indikator, bukti yang diharapkan, skor 1/0, catatan auditor, rekomendasi, bukti/link auditee, status kesiapan bukti, dan catatan auditee. Instrumen juga menyediakan ringkasan hasil, rekap kesiapan bukti, dan daftar otomatis indikator yang tidak memenuhi.

## Gap terhadap modul existing

| Area | Instrumen AMI 2026 | Modul SPMI existing | Kondisi setelah penyesuaian |
|---|---|---|---|
| Struktur master | Standar dan indikator per kelompok | Jenis > Standar > Butir > Indikator > Skenario | Sudah kompatibel; skenario dipakai sebagai bukti/daftar tilik |
| Identitas audit | Unit/prodi, jenjang, skema LAM, tim auditor/auditee | PT, fakultas, prodi, TA, semester, auditor dan auditee | Sebagian kompatibel; skema LAM dapat dibuat sebagai Jenis SPMI terpisah, jenjang berasal dari prodi |
| Bukti per indikator | Bukti/link auditee | Hanya lampiran global audit | **Ditambahkan** bukti/link per indikator |
| Kesiapan bukti | Tersedia/Sebagian/Belum Tersedia | Belum ada | **Ditambahkan** status kesiapan per indikator |
| Catatan auditee | Per indikator | Belum ada | **Ditambahkan** catatan auditee per indikator |
| Temuan auditor | Catatan auditor | Temuan dan catatan khusus | Sudah tersedia dan dipertahankan |
| Rekomendasi auditor | Per indikator | Tercampur dengan catatan/tindak lanjut | **Ditambahkan** rekomendasi auditor per indikator |
| Penilaian | Biner 1/0 | O, KTS Mayor, KTS Minor, Sesuai, Melebihi | **Ditambahkan** pemetaan otomatis: S/LS=1 dan O/KTS=0 |
| Rekap hasil | Jumlah memenuhi dan persentase | Rekap status temuan | **Ditambahkan** capaian skor AMI tanpa menghapus rekap status lama |
| Rekap kesiapan | Jumlah per status dan persentase | Belum ada | **Ditambahkan** perhitungan langsung dari data indikator |
| Daftar tidak memenuhi | Otomatis dari skor 0 | Dashboard KTS dan tindak lanjut | Sudah lebih kaya melalui KTS, dashboard, PIC, target, progres, dan PPEPP |
| Konten indikator 2026 | 74 inti + satu skema akreditasi | Master bertingkat dan berversi | Kompatibel; ekspor mengambil seluruh indikator aktif pada Jenis SPMI yang dipilih tanpa menimpa instrumen historis |
| Integritas satu file | Identitas dan seluruh indikator berada dalam satu workbook | Metadata audit, ID skenario, ID temuan, dan jumlah indikator disimpan tersembunyi | Diperketat: setiap upload memeriksa sheet wajib, audit/jenis, jumlah, pasangan ID–teks indikator–bukti, duplikasi, skor, dan kesiapan bukti |

## Kelebihan instrumen spreadsheet

- Struktur indikator 2026 jelas dan siap dipakai sebagai checklist audit.
- Pemilihan kelompok LAM membuat ruang lingkup audit adaptif.
- Isian auditee dan auditor dipisahkan dengan baik.
- Rekap hasil, kesiapan bukti, dan ketidakpatuhan mudah dibaca.
- Skor biner sederhana untuk konsolidasi lintas program studi.

## Kekurangan instrumen spreadsheet

- Kolaborasi, kontrol akses, histori perubahan, dan jejak persetujuan lebih lemah daripada sistem.
- Skor biner tidak membedakan observasi, minor, mayor, sesuai, dan melampaui standar.
- Tidak memiliki pengelolaan PIC, tenggat, progres, verifikasi, serta siklus PPEPP terintegrasi.
- Pemilihan LAM dan rumus bergantung pada referensi antarsheet sehingga rawan rusak saat struktur diubah.
- Berkas Ver08 terbaru menyimpan formula persentase kesiapan pada seluruh baris standar; pemindaian
  tidak menemukan `#REF!`, `#DIV/0!`, `#VALUE!`, `#NAME?`, atau `#N/A`.

## Kelebihan modul SPMI AIS

- Memiliki status temuan yang lebih kaya dan tetap dapat dipetakan ke skor biner.
- Sudah terhubung dengan pengajuan, persetujuan, SOP, audit trail, dashboard, dan PPEPP.
- Tindak lanjut mendukung PIC, target tanggal, progres, status, catatan, serta monitoring keterlambatan.
- Master bertingkat dapat menampung instrumen internal maupun referensi eksternal.
- Data terpusat lebih aman untuk rekap lintas periode dan analisis tren.

## Kekurangan modul sebelum penyesuaian

- Lampiran hanya tersedia pada tingkat pengajuan, bukan per indikator.
- Tidak ada status kesiapan bukti dan catatan auditee per indikator.
- Rekomendasi auditor belum menjadi data tersendiri.
- Belum ada tampilan capaian biner yang sama dengan instrumen 2026.
- Konten master lama tidak otomatis mengikuti versi instrumen AMI 2026 dan pilihan skema LAM.

## Rekomendasi lanjutan

1. Kelola enam template `Jenis SPMI` berversi: AMI 2026–LAMDIK, BAN-PT, LAMEMBA, LAMINFOKOM, LAMSPAK, dan LAMWISATA. Masing-masing berisi 74 indikator inti ditambah kelompok lembaganya.
2. Jangan mengubah master audit periode lama; lakukan versioning agar laporan historis tetap konsisten.
3. Tambahkan import master terkontrol dan validasi duplikasi nomor/indikator sebelum template 2026 diaktifkan.
4. Perluas laporan PDF agar bukti auditee, kesiapan bukti, catatan auditee, rekomendasi, dan skor AMI ikut tercetak.
5. Setelah deploy, lakukan smoke test pada satu prodi untuk masing-masing skema sebelum digunakan serentak.

## Perubahan kode pada tahap ini

- Menambah bukti/link auditee per indikator.
- Menambah status kesiapan bukti dan catatan auditee.
- Menambah rekomendasi auditor per indikator.
- Menambah skor AMI otomatis 1/0 tanpa menghilangkan klasifikasi temuan existing.
- Menambah rekap capaian dan kesiapan bukti pada lembar kerja audit.
- Menyimpan kolom baru sebagai data nullable agar audit lama tetap dapat dibuka.
- Menyediakan unduh seluruh lembar AMI dan upload kembali dalam satu file XLSX per pengajuan.
- Memvalidasi sheet wajib, identitas audit/Jenis SPMI, jumlah indikator, pasangan ID dengan teks
  indikator dan bukti dokumen, ID temuan, duplikasi, skor, serta kesiapan bukti sebelum transaksi.
- Menutup resource workbook setelah ekspor/impor dan menjaga rollback penuh jika satu baris gagal.
- Menyelaraskan COVER dengan istilah Ver08: Daftar Tilik AMI, skema/lembaga akreditasi, dan jenjang program.
