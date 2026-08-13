# Audit Parity Toolbar `index.zul` ke New UI

Dokumen ini mencatat kontrak migrasi toolbar utama. Sumber kebenaran tetap
`webapp/WEB-INF/z/x/y/pages/main/index.zul`, `MainAction`,
`MainDashboardEventHelper`, dan class dashboard/action yang dipanggil olehnya.
New UI tidak menjalankan atau menyertakan `.zul`; class existing dipakai untuk
memahami aturan, konteks pengguna, sumber data, dan target operasionalnya.

## Hasil inventarisasi

- Seluruh 29 `toolbarbutton` terdaftar: 24 modul dan 5 utility shell.
- 135 fungsi dashboard/operasional mempunyai key stabil dan deskripsi native.
- 81 class action/dashboard existing dirujuk sebagai sumber logika; seluruh
  file sumbernya telah diverifikasi ada.
- Fungsi disaring menurut konteks PT, sekolah, personal, atau admin.
- Target operasional hanya dipilih dari snapshot menu yang telah lolos
  `job_has_menu`, scope institusi, status aktif, dan privilege `READ`.
- Parameter `dashboard` atau `dashboardView` yang tidak terotorisasi ditolak;
  parameter tersebut bukan sumber pemberian hak akses.
- Utility `usersOnline`, `eMenuButton`, `menuService`, `customerService`, dan
  `back_to_top` tersedia di shell New UI pada semua halaman. Daftar pengguna
  online dan customer service tetap memakai sumber data existing.
- Angka dashboard memakai pembatas institusi/pemilik yang sama dengan Generic
  CRUD. Admin dapat melihat agregat global; role lain hanya dihitung dari relasi
  yayasan, sekolah, program, fakultas, jurusan, satuan kerja, atau profilnya
  yang benar-benar tersedia pada metadata entity.
- Entity yang mempunyai flag `aktif` menampilkan rincian aktif/nonaktif dengan
  scope yang sama, bukan hanya angka total global.
- Dashboard koperasi mengekspos seluruh delapan tab Action existing. Laporan
  keuangan koperasi sudah memakai service headless native dengan perhitungan
  neraca, hasil usaha, arus kas, PPAP, CALK, dan ekspor XLSX yang sama dengan
  `LaporanKeuanganKoperasiAction`.
- Halaman ARO deposito sudah native: daftar/ringkasan jatuh tempo, filter lokal,
  toggle ARO dengan privilege `UPDATE` dan CSRF, serta pemicu manual scheduler
  existing. Tidak ada include atau redirect ke ZUL.
- Fungsi Pembagian SHU sudah mempunyai workflow native: parameter hasil RAT,
  validasi tujuh pos alokasi harus tepat 100%, perhitungan proporsional jasa
  modal dan jasa usaha, penyimpanan header+rincian dalam satu transaksi,
  scope koperasi aktif, RBAC `UPDATE`, CSRF, pencarian anggota, dan ringkasan
  nominal. Rumus dan basis transaksi mengikuti `PembagianShuAction` existing.
- CRUD Anggaran Kas dan Modal Penyertaan memakai adapter hasil review, bukan
  lifecycle metadata generik saja. Adapter menegakkan default dan validasi
  `onSave` existing: tahun valid/unik per koperasi, nama penyerta wajib,
  nominal positif, tanggal masuk, jenis penyerta, serta status bawaan.
- Halaman fungsi membaca referensi entity secara rekursif dari bytecode Action,
  superclass, dan inner/delegated dashboard class existing. Audit saat ini
  menemukan 73 dari 81 class sumber memakai entity Hibernate langsung (868
  referensi). Delapan sumber lain sudah mempunyai adapter native: empat
  dashboard SIRS berbasis raw SQL, dashboard piutang sekolah berbasis raw SQL,
  serta tiga container navigasi untuk prestasi dosen, prestasi siswa, dan
  koperasi. Query SQL terparameterisasi telah diuji terhadap PostgreSQL lokal;
  seluruh 4 + 4 + 8 tab container hanya diselesaikan ke menu New UI yang lolos
  RBAC. Dengan demikian audit source dashboard tidak lagi menyisakan class
  tanpa resolver native.

## Modul yang dicakup

1. eMedic
2. e-Learning
3. Prestasi
4. Pustaka
5. Pengajuan Anda / Workflow
6. Repository
7. Antar Jemput
8. SPMI
9. Toko
10. Koperasi
11. Akademik
12. Administrasi
13. Pengadaan
14. Pembayaran
15. Keuangan
16. Akuntansi
17. Kepegawaian
18. Gaji
19. Kinerja
20. Presensi
21. Kalender Akademik
22. Info Kegiatan
23. Neo Feeder
24. SISTER

## Aturan implementasi

Setiap kartu modul membuka dashboard native di `WEB-INF/new`. Dashboard memuat
metric dari entity Hibernate existing, daftar fungsi hasil audit action ZK, dan
seluruh child/sub-child yang benar-benar dapat diakses role aktif. Fungsi yang
mempunyai menu operasional membuka route New UI terotorisasi. Fungsi ringkasan
yang tidak mempunyai menu terpisah tetap mempunyai halaman ringkasan native dan
tidak melakukan fallback ke ZK/ZUL.

## Pemeriksaan otomatis

`NewUiModuleFunctionServiceSelfTest` memastikan jumlah modul sama dengan toolbar
existing, setiap modul mempunyai minimal empat fungsi, key fungsi tidak
duplikat, dan total inventarisasi tidak kurang dari batas audit. Java dikompilasi
dengan `-source 1.7 -target 1.7`; JSP baru diparse dan dikompilasi dengan Jasper.
`NewUiIndexToolbarParitySelfTest` memastikan seluruh 29 kontrol—termasuk utility
shell—mempunyai handler existing dan implementasi native.
