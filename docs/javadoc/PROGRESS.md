# Progres Javadoc Menyeluruh

## Batch 59 — SELESAI 100% (3 Sep 2026) — SQL injection kini 3 instance pola dashboard, bug gerbang `getMahasiswa()` terkonfirmasi template salin-tempel, TreeSet penciutan aktif 2x lagi

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika. Fokus batch ini: domain kegiatan
kesiswaan/organisasi siswa, menguji apakah kerentanan `OrganisasiSiswa`
(b46) menular ke tetangga domainnya:

- **`ais/database/model/sekolah/JabatanKegiatanKesiswaan.java`**
  (r83719) — 134→543 baris, 100% (24 anggota). BUKAN jabatan
  pengurus organisasi — satu kolom campur peran/capaian juara/format
  lomba (Peserta/Panitia/Juara I-III/dst). Verifikasi SQLi
  `OrganisasiSiswaAction` NEGATIF di Action-nya sendiri, **TAPI SQL
  injection NYATA ditemukan lewat jalur lain**: `DashboardRekapKegiatanKesiswaan`
  menyisipkan nama baris katalog mentah ke alias kolom native SQL
  (instance ke-2 pola batch 54). Bug TreeSet penciutan (b55)
  terkonfirmasi AKTIF di `NilaiKegiatanKesiswaanAction` (2 lokasi).
  `Intbox` nomor urut tanpa gerbang.
- **`ais/database/model/sekolah/MatapelajaranPunyaBukuBahanAjar.java`**
  (r83720) — 120→466 baris, 100% (22 anggota). BUKAN terkait paket
  perpustakaan (dugaan ditolak) — katalog global buku ajar (`public.buku_bahan_ajar`,
  dibagi dengan modul PT/BKD/DSpace). **Bug gerbang `getMahasiswa()`
  bukan `getSiswa()` (b58) TERKONFIRMASI SEBAGAI TEMPLATE SALIN-TEMPEL**,
  instance LEBIH PARAH: seluruh toolbar (bukan cuma tombol Hapus)
  salah gerbang — siswa bisa Tambah/Ambil/Hapus buku ajar global DAN
  memicu email spam "Pengumuman Resmi Sekolah" ke guru+seluruh siswa
  kelas dengan judul buku bebas pilihan siswa.
- **`ais/database/model/sekolah/SkalaKegiatanKesiswaan.java`**
  (r83721) — 133→466 baris, 100% (23 anggota). Katalog teks bebas
  campur skala/durasi/flag tampilan/peran (klon seed PT). SQL
  injection instance ke-3 pola dashboard yang sama (`DashboardRekapKegiatanKesiswaan`).
  Bug TreeSet penciutan terkonfirmasi dengan mekanisme KONKRET: rubrik
  nilai kehilangan kolom skala karena semua baris ber-nomorUrut NULL.
  `Intbox` tanpa gerbang lagi (ironisnya satu-satunya cara perbaikan).
- **`ais/database/model/sekolah/UploadTransaksiPembelianSiswa.java`**
  (r83722) — 119→539 baris, 100% (24 anggota). Log/header unggahan
  batch transaksi pembelian siswa — **fitur TIDAK PERNAH
  diimplementasikan** (nol Action/ZUL/JSP, hanya deklarasi relasi).
  Risiko LATEN dicatat untuk masa depan: tanpa kolom tenant sama
  sekali, tanpa proteksi duplikasi transaksi finansial.
- **`ais/database/model/sekolah/JenisKelompokKegiatanKesiswaan.java`**
  (r83723) — 115→520 baris, 100% (22 anggota). Tingkat 1 hierarki
  3-tingkat (Utama/Penunjang, BUKAN Akademik/Olahraga/dst). Verifikasi
  SQLi NEGATIF kedua berturut-turut (setelah `PembinaSiswa` b58) —
  kerentanan `OrganisasiSiswa` makin jelas TERLOKALISASI, bukan pola
  domain. Bug seed: query pencarian "Kelompok Penunjang" salah ketik
  jadi "Kelompok Utama" — baris kedua tak pernah tercipta pada
  instalasi baru. Contoh POSITIF gerbang tombol unggah massal
  (kontras bug `AsramaSiswa` b58).

**Pola SQL injection lewat nama katalog master → alias kolom native
SQL kini 3 instance terkonfirmasi** (`DashboardRekapPrestasiSiswa` b54,
`DashboardRekapKegiatanKesiswaan` ×2 b59) — cukup luas untuk dianggap
pola arsitektur `Common.getBahasaConfig()` yang tidak meng-escape,
bukan kebetulan lokal. Memperkuat `task_493423ef`. Tidak ada task baru
dibuat.

Kumulatif sesi ini: **472+ file** (125 batch 34-59) + 343 (sesi
sebelumnya) dari 7.401 total (~11,9%).

## Batch 58 — SELESAI 100% (3 Sep 2026) — broken access control baru (referensi guru, asrama), verifikasi negatif OrganisasiSiswa, pewarisan hak menu kini 15 instance

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika:

- **`ais/database/model/sekolah/JadwalPelajaranPunyaItem.java`**
  (r83713) — 134→435 baris, 100% (24 anggota). Dugaan "model jadwal
  alternatif" DITOLAK — "Item" merujuk paket perpustakaan
  (`library.Item`), entity ini daftar buku referensi per jadwal.
  **Broken access control baru**: tombol Hapus di
  `JadwalPelajaranPunyaItemHelper` hanya cek `getMahasiswa()==null`
  (bukan siswa) sementara kontrol lain di layar sama cek siswa —
  siswa yang buka tab "Buku" dari kalender mingguannya bisa menghapus
  daftar rujukan milik gurunya. Bug tak berbahaya: tombol Revisi
  memanggil `RevisiHelper` dengan kelas AUDIT SALAH (`SaldoAwalDetail`,
  bukan entity ini) — riwayat revisi yang tampil salah tempel.
- **`ais/database/model/sekolah/PendidikanOrangTuaSiswa.java`**
  (r83714) — 125→495 baris, 100% (28 anggota). Katalog jenjang
  pendidikan ortu, auto-seed dari PT (tapi sumbernya juga tak pernah
  di-seed kode → tabel awalnya kosong). Pewarisan hak menu — pintu
  SAMA dengan 4 kembarannya (tab "Konfigurasi Tampilan Siswa").
  Observasi mentah `aktif` dilaporkan tanpa kesimpulan (sesuai
  instruksi kalibrasi b57).
- **`ais/database/model/sekolah/AsramaSiswa.java`** (r83715) —
  126→676 baris, 100% (27 anggota). Katalog LABEL murni (bukan
  gedung/lantai/kamar). **Broken access control baru**: tombol
  "Singkronkan" — `Common.appendKeToolbar` tidak menyalin
  `isVisible()` dari tombol jangkarnya, hak BACA saja cukup memicu
  mutasi massal `Siswa.asrama` LINTAS SELURUH INSTALASI tanpa filter
  tenant. Fail-open tenant untuk pengguna terkait `Dosen` (yayasan
  null). Pewarisan hak menu instance ke-15. **Mendukung mekanisme
  "true tertulis saat INSERT"** untuk pola aktif (bukan penjelasan
  `JenisTinggalSiswa` b57).
- **`ais/database/model/sekolah/PembinaSiswa.java`** (r83716) —
  128→499 baris, 100% (27 anggota). Kolom `pembina` bertipe `Tbmuser`
  (akun), BUKAN `Guru` — picker tanpa batas sekolah/yayasan.
  **Verifikasi NEGATIF pola `OrganisasiSiswa`** (SQLi & bug schema
  TIDAK ada di sini — menenangkan). Bug baru: cache preload
  `ConstantValues` punya batas 100 baris tanpa fallback DB — isi
  otomatis pembina berhenti bekerja secara acak begitu instalasi
  melewati ambang itu. Filter "Nama Pembina" rusak (alias salah,
  QueryException berisik — beda dari pola `OrganisasiSiswa` yang
  gagal diam-diam).
- **`ais/database/model/sekolah/PekerjaanOrtuSiswa.java`** (r83717)
  — 125→583 baris, 100% (28 anggota). Bug auto-seed: kode Feeder
  HILANG TOTAL (`Pekerjaan.getKode()` yang dipanggil bukan
  `getFeeder()`, dan `kode` tidak dipetakan di kelas sumber → selalu
  null). Bug promosi PPDB→siswa: `CommonPSB` menyalin properti
  reflektif berbasis NAMA, tipe `Pekerjaan`(PT)→`PekerjaanOrtuSiswa`
  tidak cocok → exception ditelan, data pekerjaan ortu tak pernah
  terbawa dari pendaftaran ke biodata siswa. Observasi mentah `aktif`
  dilaporkan tanpa kesimpulan.

Tidak ada task baru dibuat — broken access control baru memperkuat
`task_5e93a600`. Kumulatif sesi ini: **467+ file** (120 batch 34-58)
+ 343 (sesi sebelumnya) dari 7.401 total (~11,7%).

## Batch 57 — SELESAI 100% (3 Sep 2026) — instance ke-6 keluarga PSB, broken access control finansial baru, KALIBRASI ULANG diperlukan untuk pola "aktif tak pernah ditulis"

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika:

- **`ais/database/model/sekolah/StatusKeluarSiswa.java`** (r83707) —
  129→521 baris, 100% (29 anggota). Arah relasi ke `KelompokStatusKeluarSiswa`
  TERBALIK dari dugaan (kelompok→katalog, bukan sebaliknya). Auto-seed
  menyalin seluruh baris `StatusKeluar` (PT). `getKeterangan()` tidak
  dipetakan (pola b56) TIDAK berlaku di sini (properti dipetakan
  benar). **Menandai kemungkinan kontradiksi dalam klaim "aktif tak
  pernah ditulis" batch 52-56** — lihat catatan kalibrasi di bawah.
  Pewarisan hak menu instance ke-14.
- **`ais/database/model/sekolah/GelombangPendaftaranPsbPunyaMatapelajaran.java`**
  (r83708) — 118→496 baris, 100% (22 anggota). BUKAN mapel ujian
  masuk — daftar mapel yang nilai rapornya wajib diverifikasi per
  gelombang PSB. **Instance ke-6 keluarga PSB nol-privilese**
  (`edit`/`delete` HARDCODE `true`, bukan sekadar nol `checkPrevilages`)
  — DAN ditemukan bahwa `CommonPrivilages.doCheckPrevilagesRead()`
  whitelist `MUST_CHECKED` (12 URL) HANYA berisi modul PT, seluruh
  modul sekolah tidak tercakup sama sekali (memperkuat `task_9b7ff647`).
  Verifikasi NEGATIF menenangkan: nol keterlibatan di jalur `/ppdb`
  pra-otentikasi.
- **`ais/database/model/sekolah/DiskonSiswaItemBiaya.java`** (r83709)
  — 120→529 baris, 100% (17 anggota). Sisi kanan relasi adalah
  `ItemBiayaSekolah` (bukan `PengaturanBiayaItemBiaya`). **Broken
  access control finansial BARU**: `DiskonSiswaAction` "Singkronkan
  Tagihan" DAN 5 tombol di `DiskonSiswaPunyaSiswaHelper` (Ambil
  Siswa/Ambil Calon Siswa/Singkronkan×2/**Kirimkan Diskon Ke
  Pembayaran**) nol cek `edit` — hak BACA saja bisa memberi diskon
  massal + mendorongnya ke alur pembayaran. Instance kedua persis
  pola batch 55 (`DetailTagihanSiswaHelper`), kali ini di sisi
  pengurangan tagihan.
- **`ais/database/model/sekolah/AlatTransportasiSiswa.java`** (r83710)
  — 126→505 baris, 100% (28 anggota). Katalog PDDikti 13 nilai,
  disalin dari `AlatTransportasiMahasiswa` via auto-seed. Pewarisan
  hak menu instance ke-14 varian PALING MURNI (satu-satunya pintu,
  tanpa menu sendiri sama sekali). **Koreksi mekanisme penting**: getter
  `null→true` + Hibernate property access berarti nilai coalesced
  YANG DITULIS ke INSERT — risiko nyata pola "aktif tak pernah
  ditulis" hanya untuk baris yang masuk LEWAT SQL MENTAH/migrasi, bukan
  lewat `onSave()` normal.
- **`ais/database/model/sekolah/JenisTinggalSiswa.java`** (r83711) —
  125→593 baris, 100% (24 anggota). Katalog PDDikti 6 nilai. Pewarisan
  hak menu varian BARU: rantai TIGA TINGKAT (Siswa→konfigurasi_siswa→
  Jenis Tinggal, tanpa menu di kedua tingkat tengah). **Bug "aktif tak
  pernah ditulis" instance ke-7, DIKLAIM TERKONFIRMASI** termasuk pada
  baris hasil auto-seed — **BERTENTANGAN** dengan penjelasan mekanisme
  `AlatTransportasiSiswa` di atas (agen berbeda, kesimpulan berbeda
  untuk pola getter yang serupa).

**⚠ CATATAN KALIBRASI PENTING — perlu verifikasi empiris**: batch ini
menghasilkan 2 kesimpulan BERTENTANGAN soal mekanisme sebenarnya di
balik pola "kolom `aktif` tak pernah ditulis `onSave()`" yang sudah
diklaim 6-7 instance sejak batch 45. `StatusKeluarSiswa`/`AlatTransportasiSiswa`
berargumen bahwa Hibernate property-access + getter `null→true`
berarti nilai coalesced ITU YANG DITULIS ke kolom saat INSERT (risiko
nyata cuma untuk baris masuk lewat SQL mentah), sementara
`JenisTinggalSiswa` mengklaim baris auto-seed tetap `NULL` di DB dan
gagal cocok filter ketat. Sebelum instance-instance lama pola ini
dipakai sebagai dasar perbaikan kode, **VERIFIKASI EMPIRIS lewat
harness DB UAT** (lihat [[ais-mesin-posting-pattern]]) sangat
disarankan — jangan asumsikan salah satu penjelasan benar tanpa
mengecek nilai kolom sungguhan di database.

Tidak ada task baru dibuat — semua temuan keamanan memperkuat
`task_5e93a600`/`task_9b7ff647` yang sudah ada.

Kumulatif sesi ini: **462+ file** (115 batch 34-57) + 343 (sesi
sebelumnya) dari 7.401 total (~11,5%).

## Batch 56 — SELESAI 100% (3 Sep 2026) — IDOR PRA-OTENTIKASI KRITIS: unduh dokumen PPDB anak tanpa login (memperkuat `task_1f9c66d3`/`task_4ca32776`), instance ke-5 keluarga PSB nol-privilese

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika:

- **`ais/database/model/sekolah/CalonSiswaPunyaVerifikasiBerkas.java`**
  (r83705) — 129→629 baris, 100% (18 anggota). Entity BERBEDA dari
  `CalonSiswaPunyaVerifikasiParameter` (b49) — jangkar transaksi
  kepemilikan berkas wajib PPDB (Akte/KK/Ijazah) via `LampiranLain`.
  **TEMUAN PALING KRITIS SESI INI**: `/ppdb?hanya_tampil_jsp=true&p=ppdb&s=_sukses_login&id=<N>`
  fully pra-otentikasi — parameter URL `id` MENANG atas sesi login,
  merender link unduh dokumen PPDB (akte kelahiran, KK, KTP orang
  tua, ijazah, surat keterangan RS anak berkebutuhan khusus) untuk
  CALON SISWA MANAPUN, nol cek kepemilikan. `createLinkUri()` bahkan
  MENYALIN berkas ke direktori publik statis. Permintaan anonim juga
  meng-INSERT baris (tulis pra-otentikasi). Endpoint pra-otentikasi
  konkret ke-2 di `/ppdb` (setelah `_wawancara_service` b50) — kali
  ini eksfiltrasi dokumen PII anak di bawah umur. **Instance ke-5
  keluarga PSB nol-`checkPrevilages`**: `VerifikasiPSBHelper` (hak
  READ saja cukup mencentang/membatalkan status verifikasi — gerbang
  bisnis nyata untuk cetak kartu ujian/ikut ujian/wawancara).
- **`ais/database/model/sekolah/CabangPrestasiGuru.java`** (r83701) —
  127→493 baris, 100% (17 anggota). Bidang lomba guru, kembar
  `CabangPrestasiSiswa`. **Konfirmasi PENUH** relevansi temuan
  fail-open personalia guru b55 (`_prestasi_guru.jsp` hardcode null)
  — dipakai di layar yang sama. Temuan baru: filter "Cabang" di layar
  daftar menyebut kolom yang tidak ada (`_id` salah tempel) → SQL
  error, bukan filter mati fungsional. `PrestasiGuruAction` TIDAK ADA
  sama sekali di repo — tabel selalu kosong pada instalasi baru.
- **`ais/database/model/sekolah/StatusAwalSiswa.java`** (r83702) —
  137→626 baris, 100% (28 anggota). Katalog jalur masuk siswa
  (Baru/Beasiswa/Pindahan), klon jenjang sekolah dari
  `StatusAwalMahasiswa`. Bug "aktif tak pernah ditulis" instance
  ke-6. `getKeterangan()` TIDAK DIPETAKAN SAMA SEKALI (`GeneralValueObject`
  bukan `@MappedSuperclass`) — isian keterangan hilang tiap request,
  sudah pernah timbulkan NPE reflektif (ada komentar "FIX NPE"
  eksplisit di 2 tempat pemanggil). Pewarisan hak menu instance ke-13.
- **`ais/database/model/sekolah/PaketPsbPunyaGelombangPendaftaranPsb.java`**
  (r83704) — 118→474 baris, 100% (18 anggota). BUKAN instance ke-5
  broken access control PSB (layar bergerbang benar — instance ke-5
  sesungguhnya ditemukan di `CalonSiswaPunyaVerifikasiBerkas` di atas).
  Siklus tulis hapus-total-lalu-sisip-ulang tanpa transaksi eksplisit
  — beberapa bom waktu integritas (Envers bolong untuk DELETE native
  SQL, risiko no-op bila session-per-request berubah).
- **`ais/database/model/sekolah/KurikulumPunyaJenisNilai.java`**
  (r83703) — 123→529 baris, 100% (24 anggota). **Klaim batch 55
  (relasi yatim) TERKONFIRMASI, bahkan lebih ekstrem**: satu-satunya
  rujukan kode (`JenisPenilaian.hitungNilaiBerdasarkanDetailGrupPenilaian`)
  mati 3 lapis (nol pemanggil + bug query properti salah nama + badan
  tombol dikomentari 100%). Bom waktu terkait: bila fitur pernah
  dihidupkan, exception di tengah alur render meninggalkan `Timer`
  ZK 200ms tak pernah dilepas.

Kumulatif sesi ini: **457+ file** (110 batch 34-56) + 343 (sesi
sebelumnya) dari 7.401 total (~11,4%).

## Batch 55 — SELESAI 100% (3 Sep 2026) — akar bug penciutan TreeSet b50 ditemukan, fail-open personalia guru, broken access control finansial baru

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika:

- **`ais/database/model/sekolah/DetailJenisPenilaian.java`** (r83695)
  — 137→669 baris, 100% (28 anggota). Simpul TERTINGGI rantai
  penilaian (`JenisPenilaian→DetailJenisPenilaian→GrupPenilaian→...`).
  Varian timing bug bom-waktu `aktif` (b51/54) TERKONFIRMASI di sini,
  paling parah karena simpul teratas — Simpan sebelum timer 50ms
  selesai = seluruh rapor mapel pemakai jenis penilaian itu lenyap.
  **Pewarisan hak menu varian baru**: layar ini menyisipkan 7 tab
  TERMASUK `/pages/master/konstanta.zul` — hak ubah katalog penilaian
  sekolah dengan sendirinya memberi hak CRUD konstanta GLOBAL
  instalasi (eskalasi menuju layar konfigurasi sistem).
- **`ais/database/model/sekolah/KategoriPrestasiGuru.java`** (r83696)
  — 134→503 baris, 100% (17 anggota). Master TINGKAT kejuaraan guru
  (kembar `KategoriPrestasiSiswa`/`CabangPrestasiSiswa`, kini 5
  kembaran total termasuk versi mahasiswa/dosen/pegawai).
  **Fail-open cakupan PERSONALIA GURU**: `_prestasi_guru.jsp` dan
  `_dashboard_prestasi_guru.jsp` meng-hardcode `Yayasan/Sekolah/Guru
  loginSebagai... = null` dengan panggilan asli DIKOMENTARI — akun
  guru biasa melihat+mengekspor prestasi SELURUH guru lintas
  sekolah/yayasan; regresi khusus sisi guru (sisi siswa hidup normal).
- **`ais/database/model/sekolah/ParameterVerifikasiCalonSiswa.java`**
  (r83697) — 133→533 baris, 100% (27 anggota). BUKAN instance ke-5
  broken access control PSB (layar bergerbang benar — rantai nol-checkPrevilages
  berhenti di 4). **TEMUAN KUNCI: akar penyebab bug penciutan TreeSet
  batch 50 ditemukan** — `getNomorUrut()` override tak pernah `null`
  (default 1) membuat `compareTo()` induk selalu 0 untuk baris
  ber-nomorUrut sama → `GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getParameterVerifikasiCalonSiswas()`
  hanya menyimpan 1 tingkat walau banyak dicentang. Ironi: satu-satunya
  kontrol UI tanpa gerbang di layar ini (`Intbox` nomor urut) adalah
  satu-satunya cara memperbaikinya.
- **`ais/database/model/sekolah/AbsenPiketPeserta.java`** (r83698) —
  138→581 baris, 100% (27 anggota). **VERIFIKASI NEGATIF** (menenangkan):
  TIDAK tersentuh jalur pra-otentikasi `/welsis` (`task_acfae1fb`)
  maupun IDOR `simpanAbsenPiket` (`task_493423ef`) — nol referensi di
  kedua jalur. Tapi tabel praktis SELALU KOSONG akibat 2 bug menulis
  bertumpuk (syarat terbalik + objek salah yang di-`session.save`);
  panel detail sisi PT (`DetailAbsenPiketMahasiswaHelper`) nol
  `checkPrevilages` — bom waktu bila bug penulisan pernah diperbaiki.
- **`ais/database/model/sekolah/PengaturanBiayaPunyaSiswa.java`**
  (r83699) — 131→534 baris, 100% (22 anggota). BUKAN penetapan tarif
  individual — whitelist peserta pengaturan biaya `khususBuatSiswaTertentu`.
  **Broken access control BARU**: 4 tombol toolbar (`Ambil Siswa`/
  `Sinkronkan`/`Recovery`/`Upload`) di `DetailTagihanSiswaHelper` nol
  cek `edit` — hak BACA saja bisa menciptakan kewajiban finansial atas
  nama siswa. Fail-open tenant dua lapis. **Verifikasi NEGATIF untuk
  `task_493423ef`**: REST `TagihanSiswa`/`PsbCalonApi` di sini justru
  contoh POSITIF (menolak token tanpa kepemilikan). Bug integritas:
  "tagihan hantu" via 2 dari 8 jalur yang melewati penegakan whitelist.

Tidak ada task baru dibuat — semua temuan memperkuat
`task_5e93a600`/`task_493423ef`/`task_acfae1fb` yang sudah ada; 2
temuan (AbsenPiketPeserta, TagihanSiswa API) justru NEGATIF/menenangkan.

Kumulatif sesi ini: **452+ file** (105 batch 34-55) + 343 (sesi
sebelumnya) dari 7.401 total (~11,2%).

## Batch 54 — SELESAI 100% (3 Sep 2026) — SQL injection baru via nama katalog master (memperkuat `task_493423ef`), komentar generator "JenisSekolah" sumber asli ditemukan, pewarisan hak menu tumbuh lagi

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika:

- **`ais/database/model/sekolah/JenisSekolah.java`** (r83691) —
  132→638 baris, 100% (24 anggota). Master jenis/bentuk satuan
  pendidikan (TK/SD/SMP/SMA/SMK). **Komentar generator "JenisSekolah
  generated by hbm2java" TERKONFIRMASI SUMBER ASLI** yang dicari sejak
  batch 51 (satu-satunya kemunculan tersisa di repo, dipertahankan +
  diberi catatan). Contoh POSITIF gerbang hak akses & tanpa pewarisan
  menu (tetap 9 instance). Bug bootstrap: combo wajib "Jenjang" di
  layar ini menyaring `aktif=true` ketat, padahal SELURUH penulis
  `Jenjang` tak pernah mengisi `aktif` — berpotensi menutup total
  pendirian sekolah baru.
- **`ais/database/model/sekolah/CabangPrestasiSiswa.java`** (r83689)
  — 135→545 baris, 100% (25 anggota). Bidang lomba (Seni/Olah
  Raga/Kejuaraan Ilmiah, kode PDDikti Feeder) — pasangan `KategoriPrestasiSiswa`
  b53 terverifikasi penuh. **SQL injection BARU**: nama baris katalog
  master disisipkan mentah ke alias kolom berkutip ganda di native SQL
  `DashboardRekapPrestasiSiswa` (dipakai juga oleh `KategoriPrestasiSiswa`)
  — `Common.getBahasaConfig()` tidak meng-escape (kontras varian JS/JSQ
  di file sama yang meng-escape). Memperkuat `task_493423ef` (pola SQLi
  string-concat tersebar luas) — tidak dibuat task baru.
- **`ais/database/model/sekolah/KelompokGelombang.java`** (r83690) —
  141→560 baris, 100% (33 anggota). BUKAN instance ke-5 broken access
  control keluarga PSB (layar bergerbang benar) — tapi pewarisan hak
  menu **varian baru: kebocoran LINTAS MODUL** (dipakai dari menu PSB
  sekolah DAN menu Gelombang Pendaftaran PMB PT sekaligus, tab di
  kedua modul). Bug "aktif tak pernah ditulis" instance ke-5. Dua
  bucket koleksi in-memory global tak pernah dikosongkan → kebocoran
  lintas tenant dalam cache aplikasi.
- **`ais/database/model/sekolah/DetailGrupPenilaian.java`** (r83693)
  dan **`DetailGrupKategoriItemPenilaianSiswa.java`** (r83692) —
  139→582 dan 138→622 baris, 100% masing-masing (18 dan 24 anggota).
  Dua simpul rantai penilaian `JenisPenilaian→...→JenisItemPenilaianSiswa`.
  **Verifikasi lengkap bug bom-waktu `aktif`** dari batch 51 dari sisi
  kedua entity ini — mekanismenya persis: `onSave()` Grup mematikan
  SEMUA baris detail lalu menghidupkan hanya yang tercentang; peta
  pilihan diisi via timer async, tertekan Simpan sebelum timer jalan =
  seluruh pemetaan lenyap sekaligus. **Temuan baru**: duplikasi baris
  detail tanpa unique constraint saat kategori dinonaktif-aktifkan
  ulang; `GrupPenilaianUtil.hitung()` satu-satunya dari 10 titik baca
  yang MENGABAIKAN `aktif` — butir yang "dilepas" tetap tersubstitusi
  ke formula total (rapor sembunyikan butir tapi total tetap
  menghitungnya).

**Pola "pewarisan hak lewat menu induk" terus tumbuh** (varian lintas
modul baru ditemukan) — kandidat sangat mungkin muncul lagi di setiap
file "katalog kelompok jenis" bertab. Tidak ada task baru dibuat —
seluruh temuan memperkuat `task_493423ef`/`task_5e93a600`/pola
audit-luas yang sudah ada.

Kumulatif sesi ini: **447+ file** (100 batch 34-54) + 343 (sesi
sebelumnya) dari 7.401 total (~11,1%).

## Batch 53 — SELESAI 100% (3 Sep 2026) — entity klon yatim berbagi tabel (bom waktu skema), pewarisan hak menu kini 9 instance, fail-open `ambilAnakSiswa` instance baru

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika:

- **`ais/database/model/sekolah/KategoriPrestasiSiswa.java`** (r83683)
  — 142→484 baris, 100% (25 anggota). Master TINGKAT kejuaraan
  (Internasional/Nasional/.../Kecamatan, kode PDDikti Feeder — bukan
  Akademik/Non-Akademik). **Fail-open `ambilAnakSiswa()` instance
  baru** di `PrestasiSiswaAction`; **gerbang hak akses DIKOMENTARI
  TOTAL** di Action yang sama (tombol Ubah/Hapus tampil untuk siapa
  pun bisa buka menu); `DasbordPrestasi`/`DashboardRekapPrestasiSiswa`
  nol filter tenant + amplifier cache L3; bug schema salah-salin di
  `catatan.jsp` (cabang siswa menembak `public.*`, seharusnya
  `sekolah.*`).
- **`ais/database/model/sekolah/GrupChecklistPenilaianGuru.java`**
  (r83684) — 145→597 baris, 100% (27 anggota). BUKAN supervisi kepsek
  — angket umpan balik SISWA atas guru. **Renderer grid menulis balik
  FK angket ke baris SEMBARANG tanpa filter tenant** (varian baru
  "write-back destruktif" di level renderer, bukan getter) —
  menyentuh instrumen penilaian kinerja guru (personalia), severity
  dinaikkan. Pewarisan hak menu **instance ke-9**, mekanisme baru:
  layar ini sendiri PEMBERI hak ke 3 tab tanpa entri menu sendiri.
- **`ais/database/model/sekolah/KompetensiDasarMatapelajaran.java`**
  (r83685) — 156→578 baris, 100% (33 anggota). **TEMUAN STRUKTURAL
  BESAR**: nama & Javadoc lama menyesatkan total — file ini adalah
  KLON YATIM `JenisJadwalPelajaran` (`serialVersionUID` identik),
  memetakan **tabel fisik yang sama** (`sekolah.jenis_jadwal_pelajaran`)
  lewat 2 `@Entity`+`@Audited` terpisah. Kolom hantu `kode` dipaksa DDL
  ke tabel utama tanpa pembaca. **Terdaftar di manifest generic-CRUD**
  sebagai "Kompetensi Dasar Matapelajaran" — bila dibangkitkan jadi
  layar, pengguna mengira mengelola KD kurikulum padahal
  menghapus/mengubah master Jenis Jam Pelajaran yang dipakai FK wajib
  `JamPelajaran`. "Bom waktu" skema murni, bukan kerentanan akses.
- **`ais/database/model/sekolah/JenisNilaiHuruf.java`** (r83686) —
  149→562 baris, 100% (31 anggota). BUKAN skala konversi nilai (itu
  `NilaiHurufSekolah`) — label dimensi yang memungkinkan sekolah punya
  beberapa skala huruf paralel. Bug "aktif tak pernah ditulis"
  instance ke-4 (skala baru tak pernah muncul di kombo manapun sampai
  checkbox ditekan 2x). Pewarisan hak menu instance ke-9 (kembar pola
  b51-52).
- **`ais/database/model/sekolah/JenisJadwalPelajaran.java`** (r83687)
  — 146→603 baris, 100% (31 anggota). Sisi HIDUP dari pasangan klon di
  atas — FK wajib `JamPelajaran.jenis_jadwal_pelajaran_id`. Dijadikan
  **contoh pembanding POSITIF** untuk bug "aktif tak pernah ditulis"
  (di sini getter+SQL konsisten toleran-NULL, tidak bermasalah).
  Konfirmasi independen ke-2 atas temuan klon `KompetensiDasarMatapelajaran`.
  `TimetableJadwalPelajaranWindow.jenisDefault()` fail-open lintas
  sekolah (kembar pola `task_5e93a600`).

**Pola "pewarisan hak lewat menu induk" kini 9 instance kumulatif.**
Tidak ada task baru dibuat — semua temuan memperkuat
`task_5e93a600`/pola audit-luas yang sudah ada; 2 temuan "bom waktu"
skema (`KompetensiDasarMatapelajaran`) dicatat untuk perbaikan masa
depan, bukan kerentanan akses.

Kumulatif sesi ini: **442+ file** (95 batch 34-53) + 343 (sesi
sebelumnya) dari 7.401 total (~10,9%).

## Batch 52 — SELESAI 100% (3 Sep 2026) — amplifier JasperReports instance ke-4 & ke-5, pewarisan hak menu kini 8 instance, Intbox nomor urut tanpa gerbang

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika. Kelima file adalah katalog master
"kelompok jenis" domain sekolah, semuanya keluarga template
JasperReports/label default, ditemukan lewat scan cakupan Javadoc
rendah:

- **`ais/database/model/sekolah/JenisLaporanJadwalSekolah.java`**
  (r83677) — 158→544 baris, 100% (34 anggota). Profil template cetak
  `.jrxml` untuk Laporan Jadwal Pelajaran (berkas hidup di
  `LampiranLain`, entity ini hanya label+jangkar id). **Amplifier
  JasperReports instance ke-4** (kembar `JenisCatatanGuru` b45,
  `JenisNilaiSiswa` b48): fail-open tenant + pewarisan hak menu induk
  memungkinkan pemegang UPDATE mengganti template sekolah lain, lalu
  dikompilasi+dieksekusi server (`task_b82b25d2`). Bug "aktif tak
  pernah ditulis" instance ke-3 — jenis laporan baru tak pernah bisa
  dipakai cetak sampai checkbox ditekan 2x.
- **`ais/database/model/sekolah/JenisMateriHarianDefault.java`**
  (r83678) — 164→610 baris, 100% (34 anggota). Katalog nama materi
  yang mengisi-awal grid MATERI formulir Aktivitas Harian Siswa
  (relasi ke transaksi = salinan teks JSON, bukan FK). Bug "aktif tak
  pernah ditulis": baris baru admin tak pernah muncul di formulir.
  Bug operasional: menyunting baris seed global lewat UI memaksanya
  jadi milik 1 sekolah dan lenyap dari sekolah lain.
- **`ais/database/model/sekolah/KelompokJamPelajaran.java`** (r83679)
  — 165→691 baris, 100% (35 anggota). Label pengelompok `JamPelajaran`
  (FK nullable, relasi satu arah, tanpa koleksi). 2 bug baru ditemukan
  di `LaporanJadwalPelajaran` (bukan file ini): slot jam ke-11/12 tak
  pernah diseleksi kriteria pencarian; panen ganda saat satu baris
  jadwal menunjuk 2 kelompok berbeda.
- **`ais/database/model/sekolah/JenisSKGuru.java`** (r83680) —
  167→702 baris, 100% (37 anggota). BUKAN status kepegawaian (koreksi
  brief) — profil template cetak SK Guru (`.jrxml` via `LampiranLain`).
  **Amplifier JasperReports instance ke-5**, paling langsung terhubung
  RCE (`compileReportToFile` eksplisit atas file unggahan). Bug
  "aktif tak pernah ditulis" instance ke-3. Flag `glondongan`
  mengalihkan seluruh alur cetak (per-guru vs borongan) — bukan
  kosmetik.
- **`ais/database/model/sekolah/JenisAktiftasHarianDefault.java`**
  (r83681) — 164→650 baris, 100% (25 anggota). Pasangan
  `JenisMateriHarianDefault` (grid AKTIVITAS, bukan MATERI).
  **Broken access control BARU**: `Intbox` nomor urut di grid master
  TANPA `setDisabled`/gerbang sama sekali (checkbox Aktif di
  sebelahnya benar digerbangi) — pengguna hak BACA saja bisa mengubah
  urutan baris global lintas sekolah, langsung tersimpan. Digabung
  pewarisan hak menu induk: siapa pun yang mengisi jurnal harian
  otomatis beroleh CRUD penuh + unggah massal katalog global.

**Pola "pewarisan hak lewat menu induk" kini 8 instance kumulatif**
(`PaketPsb` b50; `KategoriItemPenilaianSiswa`, `SubMatapelajaran` b51;
`JenisLaporanJadwalSekolah`, `JenisMateriHarianDefault`,
`KelompokJamPelajaran`, `JenisSKGuru`, `JenisAktiftasHarianDefault`
b52) — layar `.zul` disisipkan sebagai tab di menu lain, `checkPrevilages()`
menguji hak menu induk bukan layar sesungguhnya. Semua 5 komentar
generator palsu "JenisGuru/Bank/JenisPenilaian generated by hbm2java"
diperbaiki. Tidak ada task baru dibuat — seluruh temuan memperkuat
`task_b82b25d2`/`task_5e93a600`/pola audit-luas yang sudah ada.

Kumulatif sesi ini: **437+ file** (90 batch 34-52) + 343 (sesi
sebelumnya) dari 7.401 total (~10,8%).

## Batch 51 — SELESAI 100% (3 Sep 2026) — dump PII guru tanpa login (memperkuat `task_493423ef`), pola pewarisan hak menu instance ke-2/3

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika:

- **`ais/database/model/sekolah/PenjurusanSekolah.java`** (r83671) —
  160→652 baris, 100% (40 anggota). Katalog GLOBAL jurusan/peminatan
  (IPA/IPS/Bahasa/kompetensi keahlian SMK), dimensi tarif keuangan via
  `PengaturanBiaya`. Konfigurasi `nilai_umur_calon_siswa_dibatasi`
  praktis mati di 2 tempat sekaligus (kembar temuan batch 48); gerbang
  batas umur jurusan bisa terlewat total di PPDB bila combobox tidak
  pernah dirender; 4 mesin tagihan punya semantik "tanpa jurusan"
  berbeda-beda (nominal tagihan siswa sama bisa beda antar layar).
  Layar master salah satu yang bergerbang paling benar sejauh ini.
- **`ais/database/model/sekolah/KategoriItemPenilaianSiswa.java`**
  (r83673) — 158→645 baris, 100% (34 anggota). Simpul dalam rantai
  `JenisPenilaian → DetailJenisPenilaian → GrupPenilaian →
  DetailGrupPenilaian → GrupKategoriItemPenilaianSiswa →
  DetailGrupKategoriItemPenilaianSiswa → kelas ini →
  JenisItemPenilaianSiswa`, terpisah total dari `JenisNilaiSiswa`.
  Bug bom-waktu integritas data: saklar `aktif` kategori sendiri tak
  pernah dibaca runtime, tapi menyimpan ulang Grup Kategori mematikan
  permanen seluruh pemetaan butir nilainya dari rapor secara senyap.
  "Kategori hantu" lintas sekolah bisa tetap aktif tanpa cara
  dilepas dari layar mana pun. Pewarisan hak lewat menu induk
  (instance ke-2, kembar `PaketPsb` b50).
- **`ais/database/model/sekolah/SubMatapelajaran.java`** (r83674) —
  157→659 baris, 100% (34 anggota). Label penjadwalan murni (25 slot
  `GuruMengajar`, 12 slot `JadwalPelajaran`), bukan unit penilaian.
  Bug fungsional signifikan: kombo pilihan sub-mapel TIDAK PERNAH
  di-`selectComboItem` saat render ulang formulir jadwal/penugasan —
  setiap simpan ulang baris (walau hanya ubah jam/guru) menulis
  seluruh kolom sub-mapel jadi NULL secara senyap; nol laporan/API
  membaca kolom ini sehingga bug tak pernah terlihat. Pewarisan hak
  lewat menu induk instance ke-3.
- **`ais/database/model/sekolah/JenisGuru.java`** (r83675) — 157→559
  baris, 100% (31 anggota). VERIFIKASI KOMENTAR GENERATOR: berkas ini
  adalah SUMBER ASLI string "generated by hbm2java" yang salah-salin
  ke 15 berkas lain + 25+ berkas `.zul` tak berhubungan — dikonfirmasi
  lewat jejak `title="Tambah Jenis Guru"`. Domain dikoreksi dari brief:
  taksonomi bebas (bukan status kepegawaian), tak pernah dipakai untuk
  keputusan bisnis. **Temuan keamanan menonjol**: `_statistik_guru.jsp`
  mengirim `SELECT g.*` mentah dari `sekolah.guru` (NIK, NPWP, rekening
  bank, koordinat rumah) ke servlet `/Data` dengan `tanpaLogin=true`,
  melewati cek login untuk seluruh aksi baca — kembaran sisi-guru dari
  `task_4ca32776`, memperkuat `task_493423ef` yang sudah ada (tidak
  dibuat task baru). Layar master `JenisGuruAction` sendiri contoh
  POSITIF gerbang hak akses yang benar.
- **`ais/database/model/sekolah/RuangGelombangPendaftaranPsbPSB.java`**
  (r83672) — 148→549 baris (dinormalkan LF→CRLF, outlier), 100% (25
  anggota). Nama kelas menyesatkan — bukan penghubung 3 tabel, hanya
  pasangan `(RuangPSB, CalonSiswa)` unik per-instalasi (bukan
  many-to-many). **Broken access control instance ke-4 keluarga
  PSB**: `RuangPsbCalonSiswaDetailAction` nol `checkPrevilages`,
  dengan hak BACA saja pengguna dapat tombol "Ambil Data Calon Siswa
  Manual" (tulis massal lintas ruang/gelombang/sekolah/yayasan),
  Hapus, dan Cetak (ekspor >120 kolom PII CalonSiswa termasuk NIK,
  KK, riwayat penyakit, GPS rumah). Tombol mutasi massal ke-3 tanpa
  gerbang di `RuangPSBAction` ("Perbaiki Urutan Nomor Ujian").
  Memperkuat `task_5e93a600` dan `task_4ca32776` (tidak dibuat task
  baru). Bug tak berbahaya: tombol Hapus menyasar tabel modul PMB
  yang salah (`ruang_paket_pmb`) sehingga selalu gagal dan tak pernah
  menghapus apa pun — "bom waktu" bila kolomnya "diperbaiki" tanpa
  memperbaiki nama tabelnya. Jalur `_ikut_ujian_online_service.jsp`
  pra-otentikasi baca-saja ditemukan, memperkuat `task_1f9c66d3`.

Kumulatif sesi ini: **432+ file** (85 batch 34-50 + 5 batch 51) +
343 (sesi sebelumnya) dari 7.401 total (~10.6%).

## Batch 50 — SELESAI 100% (3 Sep 2026) — `/ppdb` PRA-OTENTIKASI DIKONFIRMASI, MEMPERKUAT `task_1f9c66d3`

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika:

- **`ais/database/model/sekolah/PengaturanBiayaItemBiaya.java`**
  (r83665) — 158→622 baris, 100% (34 anggota). TERKONFIRMASI HIDUP
  PENUH sebagai inti mesin billing sekolah (rantai `PengaturanBiaya →
  ...  → Tagihan → PembayaranSiswa`). Bug baru: fitur "salin dari
  pengaturan biaya lain" MATI TOTAL (ternary logika salah membuat
  sumber salinan selalu null).
- **`ais/database/model/sekolah/GelombangPendaftaranPsbPunyaParameter
  VerifikasiCalonSiswa.java`** (r83667) — 152→696 baris, 100% (31
  anggota). Melengkapi temuan batch 49: KEDUA sisi rantai verifikasi
  PSB kini terkonfirmasi TANPA privilese sama sekali. Bug penciutan
  TreeSet berdampak khusus: penyimpanan PERTAMA kehilangan pilihan
  checkbox secara senyap, edit-simpan kedua terlihat normal — sangat
  mudah disalahkan ke kesalahan pengguna.
- **`ais/database/model/sekolah/InterviewPunyaCalonSiswa.java`**
  (r83668) — 147→607 baris, 100% (34 anggota). **TEMUAN PALING KRITIS
  batch ini**: endpoint `/ppdb?hanya_tampil_jsp=true&p=ppdb&s=
  _wawancara_service` SEPENUHNYA PRA-OTENTIKASI — `action=get_data`
  membocorkan foto+NOMOR HP pewawancara dan LINK VIDEO CONFERENCE
  wawancara anak orang lain; `action=submit_siap` bisa MENULIS (timpa
  catatan, set status "siap") pada baris calon siswa MANA PUN lintas
  instalasi TANPA LOGIN. Ini konfirmasi langsung `/ppdb` yang sudah
  dicurigai di daftar `task_1f9c66d3` (dispatcher `hanya_tampil_jsp`)
  memang rentan — MEMPERKUAT task itu signifikan, bukan task baru.
  Layar pengelola juga nol privilese (instance ke-3 pola PSB).
- **`ais/database/model/sekolah/KurikulumPunyaMatapelajaran.java`**
  (r83666) — 143→665 baris, 100% (31 anggota). Premis awal keliru
  (tidak ada field `jenisPenilaian`). Bug batch 48 TERKONFIRMASI dengan
  koreksi penting (penjaga null menjaga hal yang tidak pernah terjadi).
  Bug baru "matapelajaran hantu": divergensi checkbox vs SQL `aktif`
  membuat mapel yang tak pernah disentuh admin tetap terhitung di
  rapor/rekap/API meski tampak tak tercentang.
- **`ais/database/model/sekolah/PaketPsb.java`** (r83669) — 158→639
  baris, 100% (34 anggota). Domain terverifikasi 6 sumber (katalog
  jalur/paket PPDB, bukan bundel biaya). Broken access control via
  PEWARISAN MENU (hak CREATE/UPDATE/DELETE master paket sesungguhnya
  hak menu Gelombang Pendaftaran PSB — mekanisme baru, bukan fail-open
  biasa). Bug fungsional: nama paket wajib unik GLOBAL (bukan per
  sekolah) — instalasi multi-sekolah tak bisa punya 2 paket "Reguler".

**`task_1f9c66d3` (dispatcher JSP anonim) DIPERKUAT SIGNIFIKAN** —
`/ppdb` kini terkonfirmasi rentan dengan mekanisme baca DAN tulis
pra-otentikasi konkret (bukan cuma dugaan dari daftar 17 halaman).

Total akumulasi 50 sesi: **427 file + 1 Action dasbor**.

## Batch 49 — SELESAI 100% (3 Sep 2026) — DATA DISABILITAS ANAK TERHAPUS SENYAP, BROKEN ACCESS CONTROL VERIFIKASI PSB

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika:

- **`ais/database/model/sekolah/Penghargaan.java`** (r83659) — 157→709
  baris, 100% (34 anggota). Bug `totalPointPenghargaan` TERKONFIRMASI —
  bahkan lebih parah dari dugaan: kode penjumlahan salah-alamat itu
  SEPENUHNYA MATI (tidak bocor ke total manapun karena `Double`
  immutable + urutan penulisan parameter). Penciutan TreeSet
  terverifikasi NYATA menghasilkan selisih hitung layar vs laporan.
- **`ais/database/model/sekolah/AsramaSiswaPunyaSiswa.java`** (r83660)
  — 157→703 baris, 100% (33 anggota). Bug `syncAsrama` TIDAK menyeberang
  dari PT (mekanisme beda), TAPI bug "penambahan lupa filter asrama"
  TERKONFIRMASI ADA di 2 lokasi (memindahkan siswa diam-diam antar
  asrama, berdampak keuangan via `PengaturanBiaya`). Bug baru: tombol
  "Bersihkan" membatalkan dirinya sendiri (renderer langsung menulis
  ulang data yang baru dibersihkan).
- **`ais/database/model/sekolah/CalonSiswaPunyaVerifikasiParameter.java`**
  (r83661) — 155→647 baris, 100% (27 anggota). Domain lebih kompleks
  dari dugaan (N entri per kategori). **Broken access control
  signifikan**: layar verifikasi PSB nol `checkPrevilages` sama sekali
  — hak READ saja bisa mengubah status verifikasi berkas penerimaan
  calon siswa MANA PUN lintas instalasi (keputusan lolos/tidak seleksi).
- **`ais/database/model/sekolah/NilaiKegiatanKesiswaan.java`** (r83662)
  — 151→659 baris, 100% (31 anggota). Struktur kunci gabungan
  TERKONFIRMASI RAPI (mencerminkan versi mahasiswa PT yang baik).
  Temuan penting: SELURUH lapis nilai kegiatan kesiswaan YATIM
  FUNGSIONAL — diisi tapi tak pernah dibaca untuk perhitungan kredit
  apa pun (porting dari PT berhenti di layar master). Broken access
  control ada tapi "bom waktu" (severity rendah sekarang).
- **`ais/database/model/sekolah/KebutuhanKhususSiswa.java`** (r83663)
  — 149→588 baris, 100% (19 anggota). Premis awal keliru — entity ini
  katalog GENERIK 17 kategori (bukan data pribadi), data disabilitas
  asli ada di kolom teks bebas `Siswa.kebutuhanKhusus` TANPA FK ke
  entity ini. **BUG PALING SIGNIFIKAN batch ini**: penulis form PPDB
  menyimpan LABEL kategori, pembaca mencocokkan berdasarkan ID — hampir
  tidak pernah cocok → checkbox SELALU tampak kosong saat form dibuka
  ulang → menyimpan ulang tanpa mencentang lagi MENGHAPUS DATA
  DISABILITAS ANAK SECARA SENYAP, sekaligus melewati validasi wajib
  unggah surat keterangan dari rumah sakit. "Bom waktu" ganda: memperbaiki
  1 sisi saja akan membuat data historis format-lama jadi tak terbaca.

**Bug fungsional paling berdampak sesi ini** (bukan kerentanan keamanan,
integritas data anak berkebutuhan khusus): kehilangan data senyap di
`KebutuhanKhususSiswa`/`CalonSiswa.kebutuhanKhusus` — dicatat lengkap
di Javadoc `CalonSiswa.kebutuhanKhusus(Box)` untuk perbaikan masa depan,
TIDAK dieskalasi sebagai task keamanan (murni bug data, bukan akses).

Total akumulasi 49 sesi: **422 file + 1 Action dasbor**.

## Batch 48 — SELESAI 100% (3 Sep 2026) — "SURAT SAKTI" TERKONFIRMASI DI SEKOLAH, DoS FUNGSIONAL PSB, BUG PERUSAK DATA

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika:

- **`ais/database/model/sekolah/RuangPSB.java`** (r83653) — 201→676
  baris, 100% (38 anggota). Domain terverifikasi: unit kuota alokasi
  ujian PSB, bukan katalog ruangan. Nol filter tenant (bukan fail-open
  — memang tidak ada), DAN 2 tombol mutasi massal tanpa gerbang sama
  sekali — salah satunya (centang "Penuh") berpotensi **DoS fungsional**:
  hak READ saja bisa memblokir SELURUH pendaftaran satu gelombang PSB.
- **`ais/database/model/sekolah/KelompokStatusKeluarSiswa.java`**
  (r83654) — 171→523 baris, 100% (28 anggota). Pola "surat sakti"
  `task_1214dd58` TERKONFIRMASI PENUH menyeberang ke modul sekolah
  (`KelompokStatusKeluarSiswaDetailAction` — nol `checkPrevilages` sama
  sekali, assign/hapus massal, edit tanggal lulus langsung tersimpan).
  Mekanisme baru `task_5e93a600` (`AmbilDataSiswaBanyak`). Kalibrasi
  penting: dampak SAAT INI lebih rendah dari dugaan (label tak pernah
  benar-benar diturunkan ke siswa) — tapi "bom waktu": begitu fitur
  "dilengkapi" sesuai niat desain asli, panel jadi mesin ubah-status
  massal sungguhan.
- **`ais/database/model/sekolah/KelasSiswaPSB.java`** (r83655) —
  173→680 baris, 100% (34 anggota). Dugaan awal keliru (master kuota
  ruang PSB, bukan penempatan siswa). **Bug perusak data signifikan**:
  `getNama()` destruktif TANPA syarat menimpa nama jadi KOSONG untuk
  setiap ruang berbasis kelas reguler — kolom nama grid selalu kosong,
  filter pencarian tak pernah cocok, revisi Envers palsu tiap render.
  Kuota praktis tak ditegakkan (`==` bukan `>=`, hanya dicek renderer
  admin). Nol filter tenant lagi (varian sama seperti `SiswaAction`).
- **`ais/database/model/sekolah/JenisNilaiSiswa.java`** (r83656) —
  178→712 baris, 100% (34 anggota). Premis awal keliru (bukan kategori
  nilai rapor — profil template cetak JasperReports). Bug "kolom aktif
  tak pernah ditulis layar master" — **instance ke-2** (kembar
  `JenisCatatanSiswa` b45). Amplifier "unggah ulang template .jrxml
  lintas sekolah" — **instance ke-3** (kembar `JenisCatatanGuru` b45).
- **`ais/database/model/sekolah/KurikulumSekolah.java`** (r83657) —
  167→779 baris, 100% (24 anggota). Bug NULL/SQL "self-healing" — baris
  `aktif=NULL` hilang senyap dari dropdown lalu SEMBUH SENDIRI pada
  flush berikutnya (sangat sulit direproduksi). Nol auto-seed —
  instalasi baru mulai TANPA kurikulum sama sekali, membuat jalur
  "kurikulum null" jadi jalur paling mungkin ditempuh. Severity
  keamanan rendah (metadata katalog, bukan PII).

**Pola "amplifier unggah-ulang-template-JasperReports-lintas-tenant"
kini 3 instance** (`JenisCatatanGuru` b45, `JenisNilaiSiswa` b48, dan
disebut juga di `OrganisasiSiswa`/keluarga terkait) — pola ini spesifik
berbahaya karena JasperReports mengeksekusi ekspresi Java saat render,
jadi ini juga terhubung ke kekhawatiran RCE lama dari `task_b82b25d2`
(upload `.jrxml` sebagai lampiran).

**Task lama diperkuat, tidak ada task baru batch ini**: `task_1214dd58`
(surat sakti — instance baru di sekolah), `task_5e93a600` (2 mekanisme
baru: `AmbilDataSiswaBanyak`, nol-filter `RuangPSB`/`KelasSiswaPSB`).

Total akumulasi 48 sesi: **417 file + 1 Action dasbor**.

## Batch 47 — SELESAI 100% (3 Sep 2026) — EKSPOR 116-KOLOM PII TANPA PRIVILESE DITEMUKAN, TASK ESKALASI BARU

5 file selesai didokumentasikan penuh (4 entity model + 1 Action dasbor),
semua dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi
`cmp` byte-identik, nol perubahan logika:

- **`ais/database/model/sekolah/ChecklistPenilaianGuru.java`** (r83650)
  — 201→659 baris, 100% (39 anggota). Javadoc dangkal lama (r78724)
  DIPERKAYA bukan diganti. TERKONFIRMASI HIDUP PENUH (kontras entity
  transaksinya yang yatim, b45). Bug fungsional serius: asimetri filter
  gerbang-wajib-isi vs formulir-tampilan bisa membuat status "angket
  belum lengkap" TIDAK PERNAH bisa dituntaskan siswa mana pun di
  instalasi multi-sekolah (blokir permanen). Modul disalin dari versi
  dosen TAPI tidak lengkap: `pilihan` (label opsi JSON) tak pernah
  diisi → label radio selalu angka telanjang, tak pernah "Sangat Baik".
- **`ais/database/model/sekolah/Apresiasi.java`** (r83647) — 165→604
  baris, 100% (34 anggota). Master butir apresiasi (`kredit`).
  Konfirmasi pola dasbor fail-open (kini dijangkau dari TIGA layar
  berbeda, bukan cuma satu). Bug kembar temuan batch 42:
  `totalPointPenghargaan` di rapor SELALU 0.0 — bug tunggal batch 42
  ternyata SEPASANG (sisi disiplin & apresiasi, file sama).
- **`ais/database/model/sekolah/BlokirSiswa.java`** (r83648) — 164→624
  baris, 100% (35 anggota). Investigasi penegakan blokir: HANYA 1 dari
  3 saklar (`login`) benar-benar ditegakkan, dan HANYA di jalur portal
  ZK — jalur REST/mobile (token login, API) BYPASS TOTAL. `krs`/`nilai`
  nol pembaca sama sekali. Kontrol keamanan semu parsial — cocok pola
  "6 pola berulang" yang sudah tercatat.
- **`ais/database/model/sekolah/PenghasilanOrangTuaSiswa.java`**
  (r83649) — 162→491 baris, 100% (32 anggota). Premis awal keliru
  (kamus rentang global, bukan data pribadi langsung). TAPI investigasi
  menemukan **`SiswaAction.initCriteria()` TANPA FILTER TENANT SAMA
  SEKALI** (bukan fail-open — memang tidak ada filter) — siapa pun
  hak READ menu Siswa melihat penghasilan ayah/ibu/wali, NIK, nomor HP
  orang tua SELURUH siswa lintas sekolah/yayasan. Kelas keparahan lebih
  tinggi dari 5 instance fail-open sebelumnya.
- **`ais/action/master/dashboard/sekolah/DasboardSiswa.java`** (r83651)
  — 626→879 baris, Javadoc lengkap. **TEMUAN PALING SEVERE batch ini**:
  dua ternary TERBALIK ARAH melumpuhkan filter tenant tab "Data", DAN
  tautan drill-down mengekspor **116 kolom PII** (NIK, data kesehatan,
  rekening bank, KOORDINAT GPS RUMAH) hingga 1 juta+ baris, TANPA
  privilese apa pun — cukup hak BACA menu Siswa 1 sekolah untuk
  mengunduh data SELURUH instalasi. Mekanisme BEDA dari `task_5e93a600`
  (bukan `OrangTua.ambilAnakSiswa()`). **Task eskalasi baru:
  `task_4ca32776`.**

**Total kumulatif fail-open `task_5e93a600` kini termasuk 6 mekanisme**
(5 varian `OrangTua.ambilAnakSiswa()` + 1 varian ternary terbalik BARU
di `DasboardSiswa`) — pola arsitektur "dasbor sekolah tanpa scoping
tenant" terbukti MULTI-MEKANISME, bukan 1 root cause tunggal.

Total akumulasi 47 sesi: **412 file + 1 Action dasbor**.

## Batch 46 — SELESAI 100% (3 Sep 2026) — POLA DASBOR FAIL-OPEN KINI 4 INSTANCE, SQL INJECTION "BOM WAKTU" DITEMUKAN

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika:

- **`ais/database/model/sekolah/KelompokMatapelajaran.java`** (r83644)
  — 203→766 baris, 100% (47 anggota). Domain terverifikasi dari 4
  sumber independen (rumpun mapel berjenjang: umum/kejuruan/mulok).
  Bug siklus rekursi TANPA penjaga kunjungan pada relasi self-FK
  `induk` — berpotensi `StackOverflowError` saat cetak rapor bila admin
  membuat rantai melingkar. 2 NPE nyata di `LaporanRaporSiswa`/
  `LaporanRekapTotalNilai` (cek null ada di satu titik, hilang di
  titik lain file yang sama).
- **`ais/database/model/sekolah/DiskonSiswaPunyaSiswa.java`** (r83643)
  — 181→686 baris, 100% (28 anggota). Dugaan nama KELIRU (bukan
  relasi antar-siswa — aturan-diskon-punya-penerima). TERKONFIRMASI
  HIDUP (kontras `ItemBiayaPunyaDiskon` b37 yang yatim) dengan bug
  finansial nyata: DUA mesin diskon berbeda (`hitungDiskon()` otomatis
  vs `sinkronkan()` manual) memproses jumlah aturan BERBEDA untuk
  siswa yang sama. Fail-open orang tua ditemukan lagi (akar sama
  `OrangTua.ambilAnakSiswa()`) — **instance ke-5** pola `task_5e93a600`.
- **`ais/database/model/sekolah/ApresiasiDanPenghargaan.java`** (r83641)
  — 180→663 baris, 100% (38 anggota). Struktur "paket master"
  TERKONFIRMASI PENUH (cerminan persis `PelanggaranDanHukuman` b43).
  **Konfirmasi independen KEEMPAT** pola `task_5e93a600` (fail-open
  orang tua + `DasbordApresiasi` tanpa filter + amplifier cache L3
  app-wide) — di domain APRESIASI, bukan cuma pelanggaran. Pola
  "Dasbor*+*SiswaAction fail-open" kini terbukti template arsitektur
  lintas-modul, bukan bug 1 fitur.
- **`ais/database/model/sekolah/KelompokKegiatanKesiswaan.java`**
  (r83642/83643) — 178→718 baris, 100% (40 anggota). Bug kolom FK
  salin-tempel terkonfirmasi LAGI — **instance ke-3** lintas modul
  dosen/PT/sekolah. Bug seed nyata: nama literal salah membuat
  "Kelompok Penunjang" TIDAK PERNAH tersemai untuk modul kesiswaan.
  Kontras menarik: versi sekolah justru MEMPERBAIKI jebakan bug flush
  PT lain (batch 36) — bukti bug tidak selalu menyeberang simetris.
- **`ais/database/model/sekolah/OrganisasiSiswa.java`** (r83645) —
  169→787 baris, 100% (33 anggota). SQL injection TERKONFIRMASI tapi
  **saat ini "tertutup" karena SQL sekitarnya kebetulan rusak** (find/
  replace skema salah mengenai nama kolom) — filter mati total secara
  fungsional, TAPI celah SQLi langsung hidup penuh begitu ada yang
  "memperbaiki" bug fungsionalnya tanpa sadar. Inversi hak akses LEBIH
  PARAH dari versi PT (bahkan komentar jejak niat kode `edit`/`delete`
  dihapus total, bukan cuma dikomentari). Tombol "Bersihkan" juga rusak
  total (salah tabel/kolom) — "bom waktu" kedua di file yang sama.

**TEMUAN METODOLOGIS PENTING**: pola "Dasbor*+*SiswaAction fail-open
orang tua + amplifier cache L3" (akar `task_5e93a600`) kini terverifikasi
independen di **2 domain berbeda** (Pelanggaran b42-43, Apresiasi b46)
dengan mekanisme structural IDENTIK — ini bukan bug spesifik 1 fitur,
melainkan TEMPLATE arsitektur yang kemungkinan besar terulang di modul
dasbor sekolah lain manapun (Kunjungan, Kegiatan, dll — belum diperiksa
semua). Total kumulatif instance fail-open `task_5e93a600`: 5.

**Konsep baru "bom waktu" ditemukan 2x batch ini**: kerentanan yang saat
ini TIDAK dapat dieksploitasi karena kebetulan terhalang bug fungsional
lain di sekitarnya, tapi akan langsung aktif penuh begitu bug tersebut
"diperbaiki" tanpa menyadari implikasi keamanannya. Pola ini layak
diwaspadai khusus saat ada permintaan perbaikan bug fungsional di masa
depan — cek dulu apakah perbaikan itu membuka celah keamanan tersembunyi.

Total akumulasi 46 sesi: **408 file**.

## Batch 45 — SELESAI 100% (3 Sep 2026) — IDOR CRUD PENUH (TERMASUK HAPUS) DITEMUKAN DI REST API CATATAN SISWA

5 entity selesai didokumentasikan penuh (100% method/field), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika:

- **`ais/database/model/sekolah/JenisCatatanSiswa.java`** (r83639) —
  191→679 baris, 100% (37 anggota). Tidak ada jenis catatan bawaan seed
  (semua diketik admin). **TEMUAN KRITIS BARU**: rute REST
  `catatan_siswa_*` (`CatatanApi.java`) — CRUD PENUH TERMASUK HAPUS
  hanya butuh token valid APA PUN (termasuk akun siswa/orang tua),
  NOL cek kepemilikan/tenant. Pemegang token bisa baca, ubah, DAN
  MENGHAPUS catatan disiplin/konseling siswa mana pun lintas sekolah/
  yayasan. Id `IDENTITY` berurutan → enumerasi trivial. Memperkuat
  `task_493423ef` secara signifikan. Bug data nyata: kolom `aktif`
  tidak pernah diisi layar master → jenis catatan BARU tidak pernah
  muncul di combobox web (hanya API mobile yang selamat, filter beda).
- **`ais/database/model/sekolah/JenisCatatanKelasSiswa.java`** (r83635)
  — 191→677 baris, 100% (23 anggota). Koreksi catatan batch 41:
  `LaporanRaporSiswa` TERNYATA pakai `ArrayList` bukan `TreeSet` — jalur
  rapor TIDAK terdampak bug penciutan. Bug baru: kelompok bawaan tidak
  pernah bisa dicentang di konteks sekolah aktif.
- **`ais/database/model/sekolah/JenisCatatanGuru.java`** (r83637/83638)
  — 191→809 baris, 100% (37 anggota). Nuansa TreeSet: penciutan HANYA
  terjadi di jalur formulir pengisian (bukan layar master/rapor). Fail-
  open cakupan tenant lagi, dengan amplifier: pengguna UPDATE 1 sekolah
  bisa unggah ulang template JasperReports (.jrxml, ekspresi Java
  server-side) milik sekolah LAIN.
- **`ais/database/model/sekolah/ChecklistPenilaianGuruOlehSiswa.java`**
  (r83638) — 201→525 baris, 100% (37 anggota). Entity transaksi angket
  guru, YATIM/skema lama (jalur aktif pakai `ChecklistBaruPenilaian
  GuruOlehSiswa`). **Investigasi anonimitas menemukan instance BARU
  `task_72336ffe` DI FILE YANG SAMA** (`LaporanAngketDosenPerDosenWindow
  .java` — tab "Angket Guru" kehilangan guard peran yang sama persis
  dengan tab dosen yang sudah tercatat). Plus temuan terpisah: IDOR
  impersonasi siswa lintas sekolah/yayasan via `?siswa=<id>` di layar
  pengisian angket (kandidat `task_493423ef`).
- **`ais/database/model/sekolah/Kantin.java`** (r83636) — 170→565
  baris, 100% (34 anggota). TERKONFIRMASI yatim total, konsisten
  `PembelianSiswa` (b43) — bahkan satu-satunya pembaca SQL (laporan
  saldo) sudah dimatikan (dikomentari). Risiko keamanan NIHIL saat ini.

**`task_493423ef` diperkuat SANGAT SIGNIFIKAN batch ini** — 2 instance
IDOR baru sekaligus, satu di antaranya (CRUD penuh + HAPUS via
`CatatanApi`) adalah salah satu temuan paling severe dari kategori IDOR
sepanjang inisiatif ini karena mencakup operasi destruktif (bukan cuma
baca) pada data disiplin anak di bawah umur.

**`task_72336ffe` diperkuat** dengan instance baru persis di file yang
sama dengan temuan aslinya — pola "tab tanpa guard peran" kini
terkonfirmasi berlaku untuk 3 tab (dosen, data-dosen, data-umum) DAN
tab guru sekolah.

Total akumulasi 45 sesi: **403 file**.

## Batch 44 — SELESAI 100% (3 Sep 2026) — BATCH KEAMANAN PALING SIGNIFIKAN: CACAT DISPATCHER JSP LINTAS 17 HALAMAN, IDOR API BARU

5 file selesai didokumentasikan penuh (4 servlet + 1 entity), semua
dikompilasi `-implicit:none` bersih, mirror `java/` diverifikasi `cmp`
byte-identik, nol perubahan logika. **Batch investigasi keamanan murni**
menyusul temuan `/welsis` batch 43 — 4 servlet kiosk sejenis (turunan
template generator "CheckISBN" yang sama) diinvestigasi satu per satu:

- **`ais/action/servlet/Welpus.java`** (r83629) — 68→248 baris. Domain:
  kiosk buku tamu PERPUSTAKAAN (`KunjunganAnggota`), BUKAN siswa. Jalur
  BARU (`_welpus_service.jsp`) TERNYATA SUDAH diperkeras dengan baik
  (masking nama/kode/alamat, staff gate, CSRF, scoping per perpustakaan)
  — TAPI parameter `?versilama=true` melewati SEMUA mitigasi itu: dump
  PII massal TERMASUK FOTO, lintas perpustakaan/sekolah/yayasan, plus
  scan-oracle identitas. Pola "diperbaiki tapi jalur lama lupa
  dimatikan".
- **`ais/action/servlet/Anjungan.java`** (r83630) — 59→247 baris. Domain:
  kios layanan mandiri akademik (cetak KRS/KHS/transkrip). Halaman
  penuh AMAN (cek sesi benar). **TEMUAN YANG MEMPERLUAS DRASTIS
  cakupan**: parameter `hanya_tampil_jsp=true&p=X&s=Y` menjadikan
  servlet ini proksi ANONIM ke SELURUH JSP di `WEB-INF/baru/modul/`
  tanpa daftar putih — termasuk 65+ file `_service.jsp` backend AJAX
  yang TIDAK punya cek sesi sendiri (mengandalkan gerbang normal yang
  di-bypass). Pola dispatcher SAMA dikonfirmasi ada di `anjungan.jsp`,
  `welsis.jsp`, `tamu.jsp` — cacat STRUKTURAL, bukan spesifik 1 servlet.
- **`ais/action/servlet/Hadir.java`** (r83631) — 59→263 baris. Domain:
  papan kehadiran dosen/guru lobi (BUKAN siswa) — disengaja publik
  (terkonfirmasi via whitelist eksplisit `FilterJSP`). Cabang PT bocor
  data dosen (NIDN, foto, jadwal) lintas fakultas; cabang sekolah
  "aman" HANYA karena bug NPE yang mengancam bocor serupa bila
  "diperbaiki" tanpa sadar. Kelas keparahan LEBIH RINGAN dari
  `/welsis` (baca-saja, bukan data anak di bawah umur) — direkomendasikan
  TIDAK digabung mentah ke `task_acfae1fb`.
- **`ais/action/servlet/Tamu.java`** (r83632) — 67→245 baris. Domain:
  buku tamu institusi (`KunjunganTamu`, BUKAN `KunjunganSiswa` — dugaan
  awal keliru). Aksi `guest` publik BY DESIGN (terdaftar `HomePortalService`).
  Tapi `action=list` bocor SELURUH riwayat sejak awal instalasi (bukan
  cuma "hari ini" seperti label UI). **Konfirmasi independen KEDUA**
  pola dispatcher `hanya_tampil_jsp` di 17 halaman root JSP publik,
  termasuk modul keuangan/kepegawaian/akuntansi TANPA cek sesi sama
  sekali. Plus XSS TERSIMPAN pra-otentikasi terpisah (isian anonim
  dirender ke `innerHTML` tanpa escaping).
- **`ais/database/model/sekolah/AbsenPiket.java`** (r83633) — 362→1072
  baris, 100% (66 anggota). Header absensi harian per kelas. **4 kanal
  penulis diidentifikasi, hanya 1 bergerbang benar** (`AbsenPiketAction`,
  tapi `initCriteria()` fail-open serupa `task_5e93a600` DAN
  `DetailAbsenPiketHelper` — 503 baris — NOL `checkPrevilages` sama
  sekali, tombol massal "Semua hadir"/"Reset" tanpa syarat). Kanal kiosk
  `/welsis` dikonfirmasi ULANG end-to-end, PLUS detail baru: penyerang
  anonim menciptakan header `AbsenPiket` BARU dan menunjuk guru pembina
  kelas sebagai guru piket palsu — atribusi palsu ter-audit permanen
  (`@Audited`). **TEMUAN BARU: IDOR terautentikasi PENUH** di REST
  `ElearningApiUtil.simpanAbsenPiket` — token valid APA PUN (termasuk
  siswa/orang tua) bisa mengubah status kehadiran siswa MANA PUN lintas
  sekolah/yayasan, id `IDENTITY` berurutan → enumerasi trivial (masuk
  `task_493423ef`). Bug data nyata: `CommonPayroll` header absensi
  "membajak" kelas lain akibat kondisi filter asimetris antar 2 cabang
  kode.

**TIGA TASK ESKALASI dari batch ini**:
- `task_acfae1fb` (sudah ada) — diperkuat signifikan: path traversal
  konkret di `welsis.jsp` (parameter `p`/`s` mentah ke `jsp:include`),
  jalur legacy `/welpus?versilama=true`, atribusi palsu guru piket.
- **`task_1f9c66d3` (BARU)** — cacat dispatcher `hanya_tampil_jsp`
  lintas 17 halaman JSP publik, akses anonim ke backend keuangan/
  kepegawaian/akuntansi, PLUS XSS tersimpan Tamu. Kategori BEDA dari
  `task_acfae1fb` (struktural lintas-modul, bukan aksi 1 servlet).
- `task_493423ef` (sudah ada) — diperkuat dengan IDOR baru `simpanAbsenPiket`.

**Kalibrasi penting**: batch ini JUGA menghasilkan 1 kontra-contoh
kalibrasi (`Hadir.java` — publik BY DESIGN, bukan kerentanan) dan 1
contoh POSITIF parsial (`AbsenPiketAction` layar utama bergerbang benar,
cacatnya di helper detail) — mengingatkan untuk tidak menggeneralisasi
semua servlet publik sebagai otomatis rentan.

Total akumulasi 44 sesi: **398 file** (394 model + 4 servlet, dihitung
gabungan) dari basis 7.401 file model (~5,3%; servlet dihitung terpisah
dari basis utama).

## Batch 43 — SELESAI 100% (3 Sep 2026) — ENDPOINT PRA-OTENTIKASI DITEMUKAN, `task_5e93a600` DIKONFIRMASI 3X INDEPENDEN

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/sekolah/PelanggaranDanHukuman.java`** (r83624)
  — 206→815 baris, 100% (35 anggota). Master paket pemetaan
  pelanggaran↔hukuman (bukan transaksi). Klaim "nol filter sekolah"
  TERKONFIRMASI simetris untuk KEDUA sisi. Bug nyata: tombol "Batal"
  tidak membatalkan apa pun (alias langsung ke `PersistentSet` entity
  terkelola, bukan salinan — perubahan centang tetap ter-flush).
  Instance baru bug TreeSet penciutan senyap. Memperkuat `task_5e93a600`
  dengan celah tambahan: daftar pilihan paket tanpa filter apa pun.
- **`ais/database/model/sekolah/PelanggaranSiswa.java`** (r83626) —
  248→1026 baris, 100% (55 anggota). **VERIFIKASI INDEPENDEN KETIGA
  `task_5e93a600` — TERKONFIRMASI PENUH**, dengan amplifier BARU:
  kebocoran ke akun orang tua ditulis ke cache L3 APP-WIDE (persisten
  lintas-sesi, bukan sekali per permintaan). Akar fail-open dilacak ke
  `OrangTua.ambilAnakSiswa()` — TIGA kondisi berbeda menghasilkan
  koleksi kosong (bukan satu). Bug nyata: kolom `waktu` tidak pernah
  diisi formulir (readonly, disetel ke waktu SIMPAN, bukan waktu
  kejadian) — merusak urutan grid/laporan/dasbor.
- **`ais/database/model/sekolah/ParameterTambahanKegiatanSiswa.java`**
  (r83623) — 149→525 baris, 100% (26 anggota). Struktur BEDA dari
  keluarga lain: 3 lapis (bukan 4, tidak ada `KelompokParameterTambahan
  KegiatanSiswa` — perannya diambil `KelompokKegiatanSiswa`), dan
  atribut BENAR-BENAR hidup runtime (bukan kode mati seperti mayoritas
  keluarga). Broken access control ADA — **hit rate `task_58f74860`
  kini 15/15, MENUNTASKAN seluruh daftar kandidat eksplisit**.
- **`ais/database/model/sekolah/KunjunganSiswa.java`** (r83625) —
  206→642 baris, 100% (43 anggota). Domain terverifikasi: log absensi
  kiosk scan-kartu (BUKAN konseling/BK). **TEMUAN PALING KRITIS SEJAK
  AWAL INISIATIF**: servlet `/welsis` SEPENUHNYA PRA-OTENTIKASI —
  `action=list` dump PII massal (nama, NIS, kelas, alamat) lintas
  sekolah/yayasan TANPA LOGIN; `action=scan` memalsukan absensi siswa
  MANA PUN tanpa kredensial, sekaligus jadi oracle identitas untuk
  brute-force NIS/NISN. **Task eskalasi baru: `task_acfae1fb`.**
- **`ais/database/model/sekolah/PembelianSiswa.java`** (r83627) —
  202→749 baris, 100% (42 anggota). Domain terverifikasi: nota belanja
  kantin siswa (sisi pengeluaran deposit). **Entity YATIM TOTAL** (nol
  referensi di luar berkasnya sendiri) — fitur "Belanja Siswa" yang
  HIDUP ternyata memakai entity BERBEDA (`inventory.Pembelian`),
  jebakan penamaan berbahaya bagi pembaca kode masa depan. Getter
  destruktif berantai DUA tingkat (varian terparah yang pernah
  ditemukan). Risiko keamanan saat ini NIHIL (tabel tak pernah diisi).

**DUA TASK ESKALASI PENTING batch ini**: `task_5e93a600` (data disiplin
siswa) dikonfirmasi INDEPENDEN 3 KALI dari 3 sudut entity berbeda
(Pelanggaran b42, Hukuman b42, PelanggaranSiswa b43) — keyakinan sangat
tinggi. `task_acfae1fb` (BARU) — endpoint `/welsis` pra-otentikasi,
kategori kerentanan PALING PARAH sejauh ini karena TIDAK BUTUH LOGIN
SAMA SEKALI (beda dari mayoritas temuan proyek yang butuh minimal 1
akun sah).

**`task_58f74860` TUNTAS 100% (15/15)** — seluruh kandidat eksplisit
keluarga `ParameterTambahan*Action` (10 PT + 5 sekolah) sudah
diverifikasi, tanpa satu pun pengecualian.

**Pola "getKeterangan() membalik kontrak"**: 0 instance baru batch ini.
Tetap **23 instance** total.

Total akumulasi 43 sesi: **393 file** dari 7.401 (~5,3%).

## Batch 42 — SELESAI 100% (3 Sep 2026) — KEBOCORAN DATA DISIPLIN SISWA DITEMUKAN, TASK ESKALASI BARU

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/sekolah/ParameterTambahanCatatanKelasSiswa.java`**
  (r83617) — 160→623 baris, 100% (21 anggota). Serialisasi 7/4 ruas
  (identik pola CatatanSiswa). Jangkauan pembaca TERLUAS di keluarga:
  5 jalur termasuk rapor. Broken access control ADA.
- **`ais/database/model/sekolah/ParameterTambahanGelombangPendaftaranPsb.java`**
  (r83618) — 159→522 baris, 100% (33 anggota). SQL migrasi mentah
  TERKONFIRMASI (pola sama versi PT, setiap layar dibuka, lewati
  Envers). Broken access control ADA — hit rate **14/14**. Varian bug
  salin-tempel baru: daftar properti ekspor/impor Excel menyebut 2
  kolom fiktif dari entity lain (`ParameterTambahanPaket`).
- **`ais/database/model/sekolah/KelompokParameterTambahanCalonSiswa.java`**
  (r83619) — 177→597 baris, 100% (22 anggota). 4/5 pola ADA (TreeSet
  TIDAK — konsumen pakai `List`+`Collections.sort`). Kuirk: versi
  sekolah KEHILANGAN sifat "aman secara bawaan" versi PT — kategori
  baru langsung tampil di form termasuk SEBELUM login.
- **`ais/database/model/sekolah/Pelanggaran.java`** (r83621) — 183→570
  baris, 100% (35 anggota). Master jenis pelanggaran (`kredit`).
  **TEMUAN KRITIS**: `PelanggaranSiswa` (data disiplin anak di bawah
  umur) bisa di-dump ANONIM lewat `/Data` dengan `tanpaLogin=true`
  (amplifier `task_493423ef`). TEMUAN TERPISAH: `DasbordPelanggaran`
  tidak memfilter `orangTua` sama sekali (beda dari grid utama yang
  sudah benar) — orang tua/guru melihat hingga 600 baris pelanggaran
  siswa LAIN lintas sekolah/yayasan.
- **`ais/database/model/sekolah/Hukuman.java`** (r83620) — 183→616
  baris, 100% (33 anggota). Master jenis sanksi (`poin`). Rantai
  TERNYATA 4 lapis, `PelanggaranDanHukuman` BUKAN entity transaksi
  (masih master — `PelanggaranSiswa` yang transaksi). **Konfirmasi
  independen temuan Pelanggaran**: `PelanggaranSiswaAction.initCriteria()`
  tidak memfilter siswa/guru sama sekali, filter orang tua FAIL-OPEN
  (kosong = lihat semua). `HukumanAction` sendiri CONTOH POSITIF
  (guard lengkap, langka di keluarga sekolah). Bug nyata: total poin
  hukuman di rapor SELALU 0.0 (salah nama variabel akumulator,
  ditambah ke akumulator yang sudah ditulis sebelumnya).

**TASK ESKALASI BARU: `task_5e93a600`** — broken access control pada
riwayat pelanggaran/hukuman siswa, dikonfirmasi INDEPENDEN oleh 2 agen
dari sudut entity berbeda (Pelanggaran & Hukuman). Cukup spesifik &
severe (data disiplin anak di bawah umur, fail-open, cross-tenant leak)
untuk eskalasi tersendiri di luar `task_493423ef` yang sudah ada.

**Hit rate `task_58f74860` kini 14/14** (10 PT + 4 sekolah, tanpa
pengecualian) — kandidat sekolah tersisa: `ParameterTambahanKegiatanSiswaAction`.
**Pengecualian pertama yang menyegarkan** di keluarga Action sekolah:
`HukumanAction`/`PelanggaranAction`/`PelanggaranDanHukumanAction`
SEMUA punya guard lengkap — cacatnya bergeser ke lapis dasbor/endpoint,
bukan Action master itu sendiri.

**Pola "getKeterangan() membalik kontrak"**: 1 instance baru batch ini
(`KelompokParameterTambahanCalonSiswa`; entity penghubung tetap tidak
punya field `keterangan`). Total kumulatif: 22+1 = **23 instance**.

Total akumulasi 42 sesi: **388 file** dari 7.401 (~5,2%).

## Batch 41 — SELESAI 100% (3 Sep 2026) — MODUL `sekolah/` DIBUKA, POLA BROKEN ACCESS CONTROL TERBUKTI MENYEBERANG SKEMA

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika. **File PERTAMA yang digarap dari modul `sekolah/`** (155
file, belum tersentuh sama sekali sebelum batch ini):

- **`ais/database/model/sekolah/KelompokParameterTambahanCatatanKelasSiswa.java`**
  (r83611) — 205→621 baris, 100% (30 anggota). Domain TERVERIFIKASI:
  catatan tingkat KELAS (rombel), bukan per-siswa — entity pemilik
  `CatatanKelasSiswa` punya relasi ke `KelasSiswa`, bukan `Siswa`.
  Bug TreeSet di sini DAMPAK PALING LUAS sejauh ini: kelompok yang
  tabrakan nomor urut lenyap dari formulir DAN dari rapor siswa
  (`LaporanRaporSiswa`). Broken access control ADA di kedua lapis
  (Intbox guard bolong + `ParameterTambahanCatatanKelasSiswaAction`
  hardcoded).
- **`ais/database/model/sekolah/KelompokParameterTambahanCatatanSiswa.java`**
  (r83612) — 205→718 baris, 100% (26 anggota). Padanan `CatatanMahasiswa`
  versi PT. Varian BARU pola salin-tempel SOP: kali ini nama TABEL
  itu sendiri yang salah (`kelompok_parameter_tambahan_alur_sop`),
  bukan cuma nama kolom FK seperti temuan batch 38-40. Kuirk cakupan
  auto-seed: baris bawaan lahir `sekolah`/`yayasan=null`, tapi filter
  centang kategori di `JenisCatatanSiswaAction` mensyaratkan NON-null
  — kategori bawaan tidak pernah bisa dipakai sampai admin mengisi
  cakupan manual. Broken access control ADA di kedua lapis.
- **`ais/database/model/sekolah/KelompokParameterTambahanCatatanGuru.java`**
  (r83613) — 205→807 baris, 100% (38 anggota). Padanan
  `CatatanPegawai` versi PT — identik kata-per-kata kecuali relasi
  `satuanKerja` (yatim di versi PT) diganti `yayasan`+`sekolah` yang
  BENAR-BENAR terpakai. Kuirk baru: `getYayasan()` destruktif (menimpa
  dari `getSekolah().getYayasan()` saat baris dibaca). Broken access
  control ADA di kedua lapis.
- **`ais/database/model/sekolah/ParameterTambahanCatatanSiswa.java`**
  (r83614) — 160→580 baris, 100% (30 anggota). Serialisasi 7 ruas
  (identik pola CatatanMahasiswa versi PT). Broken access control
  `ParameterTambahanCatatanSiswaAction` ADA — konfirmasi lintas skema.
- **`ais/database/model/sekolah/ParameterTambahanCatatanGuru.java`**
  (r83615) — 160→650 baris, 100% (29 anggota). Serialisasi PALING
  RAMPING di seluruh keluarga: 6 ruas berlabel/3 ruas ber-ID, TANPA
  ruas keterangan sama sekali. Bug parser "keterangan" di sini LEBIH
  PARAH dari versi Pegawai (b40): salah 100% waktu (bukan kasus tepi)
  karena ruas keterangan memang tidak ada. Kolom berlabel TERKONFIRMASI
  write-only independen (nol pemanggil parser). Broken access control
  `ParameterTambahanCatatanGuruAction` ADA.

**KESIMPULAN PALING PENTING — `task_58f74860` TERBUKTI MENYEBERANG
SKEMA/MODUL.** 3 file Action BARU di `ais/action/master/sekolah/`
dikonfirmasi cacat identik (edit/delete hardcoded, nol checkPrevilages):
`ParameterTambahanCatatanKelasSiswaAction`, `ParameterTambahanCatatanSiswaAction`,
`ParameterTambahanCatatanGuruAction`. **Total kumulatif terverifikasi
kini 13 file** (10 versi PT/`public` akhir batch 40 + 3 versi `sekolah`
batch ini) — TANPA SATU PUN pengecualian di kedua skema. Kandidat
tersisa yang BELUM diverifikasi tapi sangat mungkin cacat sama:
`ParameterTambahanCalonMahasiswaAction`(?)/`ParameterTambahanCatatanPegawaiAction`(sudah
b40)/dst versi PT sisa, dan `ParameterTambahanGelombangPendaftaranPsbAction`/
`ParameterTambahanKegiatanSiswaAction` versi sekolah.

**Pola "guard Intbox nomor urut bolong" JUGA terbukti menyeberang ke
`sekolah/`** — 3 instance baru (`KelompokParameterTambahanCatatanKelasSiswaAction`,
`...CatatanSiswaAction`, `...CatatanGuruAction`), semuanya berpasangan
dengan bug TreeSet yang sama.

**Varian bug salin-tempel SOP baru ditemukan**: bukan cuma kolom FK
(6/6 sub-keluarga "Catatan*" versi PT+sekolah cacat), tapi juga NAMA
TABEL itu sendiri untuk `sekolah.ParameterTambahanCatatanSiswa`.

**Pola "getKeterangan() membalik kontrak"**: 0 instance baru batch ini
(entity penghubung `ParameterTambahan*` tidak punya field `keterangan`,
konsisten dengan pola versi PT). Tetap **22 instance** total.

Total akumulasi 41 sesi: **383 file** dari 7.401 (~5,2%).

## Batch 40 — SELESAI 100% (3 Sep 2026) — BROKEN ACCESS CONTROL SISTEMIK 10/10, KELUARGA `ParameterTambahan*` TUNTAS

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/ParameterTambahanCatatanMahasiswa.java`**
  (r83605) — 124→526 baris, 100% (21 anggota). Entity pemilik
  `CatatanMahasiswa` (bukan `BiodataMahasiswa`), serialisasi berlabel
  TERNYATA 7 ruas (bukan 8 seperti Alumni/Mahasiswa — tanpa `indexKe`).
  `nomorUrut` entity ini kode mati untuk pengurutan (semua pembaca
  runtime pakai `groupProperty` yang mengembalikan `ParameterTambahan`,
  bukan entity ini). Broken access control ADA (konfirmasi ulang b38).
- **`ais/database/model/ParameterTambahanCatatanAdministrasi.java`**
  (r83606/83607) — 132→533 baris, 100% (16 anggota). Serialisasi 7 ruas
  juga (bukan 8). Kunci KETIGA ditemukan: `LaporanCatatanAdministrasi`
  pakai format garis-bawah `idKelompok_idParameter` terpisah dari 2
  kunci lain yang harus dijaga konsisten. Nama tabel/kolom salah
  salin-tempel dari modul SOP (`kelompok_parameter_tambahan_alur_sop`).
  Broken access control ADA — hit rate **7/7**.
- **`ais/database/model/ParameterTambahanPaket.java`** (r83607) —
  164→600 baris, 100% (22 anggota). Klaim SQL migrasi mentah batch 34
  TERKONFIRMASI AKURAT (setiap layar dibuka, tanpa syarat, lewati
  Envers). Nilai isian PMB TERKONFIRMASI di `BiodataCalonMahasiswa`.
  Premis brief soal `@ManyToMany` ke `GelombangPendaftaran` KELIRU —
  cakupan gelombang didenormalisasi ke kolom text terpisah. Bug
  fungsional nyata: ganti tahun akademik lalu centang 1 gelombang
  MENGHAPUS semua pilihan gelombang tahun lain tanpa peringatan. Broken
  access control ADA — hit rate **8/8**.
- **`ais/database/model/ParameterTambahanPengajuan.java`** (r83609) —
  124→589 baris, 100% (23 anggota). Mekanisme dua entity pemilik
  (Mahasiswa+Siswa) TERKONFIRMASI: pembedaan di lapis pemilik data (2
  pasang kolom di 2 tabel terpisah), BUKAN di entity ini yang justru
  paling ramping di keluarga. Ditemukan tabrakan ruang-nama lampiran
  lintas jenjang — `LampiranLain` tidak membedakan pemilik `Mahasiswa`
  vs `Siswa` (kedua urutan IDENTITY mulai dari 1, kunci sama = lampiran
  bisa tertukar bila kedua modul aktif sekaligus). Broken access control
  ADA — hit rate **9/9**.
- **`ais/database/model/ParameterTambahanCatatanPegawai.java`** (r83608)
  — 138→551 baris, 100% (26 anggota). Serialisasi 7 ruas (konsisten pola
  sub-keluarga "Catatan*"). Kolom berlabel TERKONFIRMASI write-only —
  parser pembacanya kode mati (nol pemanggil), kontras 6 pemanggil di
  padanan Alumni/Mahasiswa. Bug parser nyata: `split` Java membuang ruas
  kosong ekor → keterangan kosong membuat field "keterangan" yang
  terbaca justru berisi URL lampiran atau nilai isian. Broken access
  control ADA — hit rate **10/10 (SEMPURNA)**.

**KESIMPULAN — `task_58f74860` (broken access control `ParameterTambahan*Action`)
kini 10 dari 10 file yang diperiksa, TANPA SATU PUN PENGECUALIAN.**
Ini pola template sistemik yang pasti berlaku di SEMUA anggota keluarga
`ParameterTambahan*Action` yang tersisa (jika ada lagi) — tidak perlu
verifikasi eksplisit lagi kecuali untuk kelengkapan dokumentasi, cukup
asumsikan ADA sampai terbukti sebaliknya.

**Keluarga `KelompokParameterTambahan*` (9/9) dan lapis penghubung
`ParameterTambahan*` (7/7 yang relevan: Alumni/Mahasiswa/CatatanPegawai/
CatatanAdministrasi/CatatanMahasiswa/Pengajuan/Paket) kini TUNTAS.**
Total 2 keluarga besar (16 file model + puluhan file Action terkait)
selesai diaudit menyeluruh dalam 4 batch berturut-turut (37-40).

**Pola "getKeterangan() membalik kontrak"**: 0 instance baru batch ini
(seluruh 5 entity penghubung tidak punya field `keterangan`). Tetap
**22 instance** total.

Total akumulasi 40 sesi: **378 file** dari 7.401 (~5,1%).

## Batch 39 — SELESAI 100% (3 Sep 2026) — BROKEN ACCESS CONTROL SISTEMIK 6/6, TASK ESKALASI BARU

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/KelompokParameterTambahanPengaduan.java`** (r83599)
  — 168→552 baris, 100% (28 anggota). Rantai 4 lapis (per `JenisPengaduan`).
  Ketiga pola keluarga terkonfirmasi ADA. **Broken access control TOTAL**
  di `ParameterTambahanPengaduanAction` (edit/delete hardcoded, nol
  `checkPrevilages`) — modul WHISTLEBLOWER, amplifier langsung
  `task_18d52b8b`.
- **`ais/database/model/KelompokParameterTambahanPengajuanPegawai.java`**
  (r83600) — 168→618 baris, 100% (25 anggota). Skema `payroll` (satu-
  satunya anggota keluarga di skema ini). Broken access control identik
  di `ParameterTambahanPengajuanPegawaiAction`.
- **`ais/database/model/KelompokParameterTambahanPengajuan.java`**
  (r83601) — 168→687 baris, 100% (33 anggota). Domain "Pengajuan"
  TERVERIFIKASI = peserta didik (mahasiswa+siswa), BUKAN pegawai. Broken
  access control identik di `ParameterTambahanPengajuanAction`. Bug bonus:
  alias Criteria rusak (`kelompokParameterTambahanPengajuan` vs
  `...Mahasiswa`/`...Siswa`) berpotensi `QueryException` tak tertangkap
  di jalur tampil, dikonfirmasi kembar di modul Mahasiswa DAN Siswa.
- **`ais/database/model/ParameterTambahanMahasiswa.java`** (r83602) —
  195→642 baris, 100% (20 anggota). Lapis PENGHUBUNG (bukan kategori) —
  mekanisme penyimpanan nilai isian mahasiswa TERKONFIRMASI PENUH:
  2 kolom text `BiodataMahasiswa`, format 8-ruas (berlabel)/4-ruas
  (ber-ID) dipisah `\n`/`<=>`, kunci gabungan `idKelompok-&gt;idParameter`
  juga dipakai sebagai `jenis` di `LampiranLain.ambil(idBiodata, jenis)`.
  4 kolom cakupan akademik (fakultas/jurusan/program/jenjang)
  TERKONFIRMASI write-only/fiktif — nol pembaca runtime memakainya.
  Broken access control identik di `ParameterTambahanMahasiswaAction`.
- **`ais/database/model/ParameterTambahanAlumni.java`** (r83603) —
  209→728 baris, 100% (30 anggota). Filter tahun angkatan
  TERKONFIRMASI (format `";thn;;thn;"` tanpa pemisah tambahan, dibaca
  4 query identik `Restrictions.ilike ANYWHERE`). Bug nyata: combobox
  `program` tidak pernah di-`appendChild` ke dialog Ubah tapi tetap
  dibaca `onSave()` → membuka lalu menyimpan baris apa pun MENGOSONGKAN
  `program` yang sudah terisi. Broken access control identik di
  `ParameterTambahanAlumniAction`.

**TEMUAN PALING SIGNIFIKAN — pola broken access control kini 6/6 (HIT
RATE 100%)** di seluruh file `ParameterTambahan*Action` yang sudah
diperiksa lintas batch 38-39 (CatatanMahasiswa, Pengaduan,
PengajuanPegawai, Pengajuan, Mahasiswa, Alumni) — `edit`/`delete`
di-hardcode `true`, NOL pemanggilan `checkPrevilages` di setiap file,
tanpa kecuali. Bukan kebetulan — cacat TEMPLATE sistemik di seluruh
keluarga kelas Action ini (kontras dengan `KelompokParameterTambahan*Action`
yang pada umumnya PUNYA guard benar). **Task eskalasi baru dibuat:
`task_58f74860`** — cukup spesifik & actionable (daftar file konkret +
pola perbaikan referensi) untuk ditangani terpisah dari task audit-luas
umum.

**Pola "guard Intbox nomor urut bolong"**: 3 instance baru batch ini
(Pengaduan/PengajuanPegawai/Pengajuan) — total kumulatif keluarga
`KelompokParameterTambahan*` kini 7 dari 9 varian yang sudah digarap.

**Pola "getKeterangan() membalik kontrak"**: 3 dari 5 file batch ini
(kedua entity `ParameterTambahan*` penghubung TIDAK punya field
`keterangan`). Total kumulatif: 19 (akhir b38) + 3 = **22 instance**.

Total akumulasi 39 sesi: **373 file** dari 7.401 (~5,0%).

## Batch 38 — SELESAI 100% (3 Sep 2026) — POLA "GUARD KOMPONEN BOLONG" TERKONFIRMASI SISTEMIK

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/KelompokParameterTambahanCatatanPegawai.java`**
  (r83595) — 186→698 baris, 100% (35 anggota). Rantai TERNYATA 4 lapis
  (bukan 3): kategori harus dicentang per `JenisCatatanPegawai` sebelum
  muncul di form. `compareTo()` dipangkas (tanpa fallback `getNim`).
  Broken access control kembar Mahasiswa (Intbox nomor urut tanpa
  guard), DAMPAK LEBIH BERAT: berpasangan dengan bug TreeSet →
  pengguna READ-saja bisa MELENYAPKAN satu seksi form dari tampilan
  SEMUA orang. Field `satuanKerja` terkonfirmasi yatim total (nol
  pemanggil di luar entity sendiri).
- **`ais/database/model/KelompokParameterTambahanCatatanAdministrasi.java`**
  (r83593) — 176→539 baris, 100% (30 anggota). `compareTo()` versi
  pendek (tanpa fallback). Broken access control kembar persis (Intbox
  nomor urut). Bug TreeSet penciutan senyap dikonfirmasi ADA (bukan
  kasus langka — kondisi DEFAULT karena `nomorUrut` tak pernah diisi
  form Tambah/Ubah).
- **`ais/database/model/KelompokParameterTambahanCatatanMahasiswa.java`**
  (r83596) — 170→644 baris, 100% (33 anggota). Rantai 4 lapis (per
  `JenisCatatanMahasiswa`). **TEMUAN PALING SERIUS batch ini**:
  `ParameterTambahanCatatanMahasiswaAction` — `edit=true`/`delete=true`
  DI-HARDCODE, NOL pemanggilan `checkPrevilages` di SELURUH file (bukan
  cuma dikomentari — tidak pernah ada sama sekali) + nol gerbang READ
  di `doAfterCompose`. Siapa pun yang bisa buka layar bisa
  ubah+hapus pemetaan parameter tanpa hak apa pun. PLUS broken access
  control kembar Intbox nomor urut (instance kedua di file yang sama).
- **`ais/database/model/ItemBiayaPunyaDibayarDimuka.java`** (r83594) —
  160→564 baris, 100% (18 anggota). Entity TERKONFIRMASI HIDUP (beda
  dari Diskon) — `ItemBiaya.ambilDibayarDimuka()` ada & dipanggil 8+
  titik. Mekanisme fallback rantai `PIUTANG→DIMUKA→PENDAPATAN` di
  `GrupTransaksi` dikonfirmasi dari kode: kegagalan cari akun piutang
  DIAM-DIAM beralih ke akun dibayar-dimuka tanpa peringatan apa pun.
  `getFakultas()` write-back kembar persis Piutang/Diskon. Broken
  access control kembar (cek UPDATE dikomentari mati).
- **`ais/database/model/ItemBiayaPunyaPendapatanDenda.java`** (r83597)
  — 160→576 baris, 100% (31 anggota). Entity TERKONFIRMASI HIDUP dengan
  GERBANG KERAS (posting diblokir total bila akun denda tak ditemukan,
  beda dari fallback senyap Dibayar Dimuka). Bug tabrakan nama BARU:
  `ItemBiayaAction` query kelas HELPER `ItemBiayaPunyaDenda` (bukan
  entity `ItemBiayaPunyaPendapatanDenda`) → `MappingException` tertelan
  2 lapis catch → ringkasan "Akun denda" di layar daftar PERMANEN
  KOSONG tanpa jejak error. Broken access control kembar (instance ke-4
  keluarga `ItemBiayaPunya*`).

**Pola "guard komponen bolong" (Intbox nomor urut tanpa guard) kini
TERKONFIRMASI 5x independen** dalam satu batch saja (3 di keluarga
`KelompokParameterTambahan*`, sudah pernah juga di varian Mahasiswa b37
= total 4 kali di keluarga itu) — bukan kebetulan, TEMPLATE hbm2java/
Action generator yang konsisten menghilangkan satu guard spesifik.
**Pola "cek UPDATE dikomentari mati" di keluarga `ItemBiayaPunya*` kini
4 instance** (Piutang b37, Diskon b37, DibayarDimuka b38, PendapatanDenda
b38) — SEMUA anggota keluarga yang sudah digarap punya bug ini, pola
TEMPLATE bukan kebetulan.

**Pola "getKeterangan() membalik kontrak"**: 3 dari 5 file batch ini
(kedua entity `ItemBiayaPunya*` tetap tidak punya field `keterangan`).
Total kumulatif: 16 (akhir b37) + 3 = **19 instance**.

Total akumulasi 38 sesi: **368 file** dari 7.401 (~5,0%).

## Batch 37 — SELESAI 100% (3 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/KelompokParameterTambahanAlumni.java`** (r83587)
  — 204→636 baris, 100% (37 anggota). Bug fungsional nyata: DUA mekanisme
  auto-seed (`checkCreateDefault()` vs `KelompokParameterTambahanAlumniAction
  .doAfterCompose`) dengan syarat pembuatan BERBEDA dan tidak sadar satu
  sama lain — instalasi kosong bisa berakhir dengan 2 kategori bawaan
  berbeda tergantung urutan klik admin. `getNim()` di `compareTo()`
  terkonfirmasi kode mati aman (didefinisikan di `GeneralValueObject`
  sendiri, selalu null karena base bukan `@MappedSuperclass`).
- **`ais/database/model/KelompokParameterTambahanMahasiswa.java`**
  (r83588) — 187→522 baris, 100% (24 anggota). Struktur IDENTIK
  `KelompokParameterTambahanAlumni` (dikonfirmasi kata-per-kata). Temuan
  broken access control: komponen nomor urut TANPA guard sama sekali di
  renderer grid (checkbox Aktif & tombol lain dijaga, Intbox nomor urut
  tidak) — pengguna READ-saja bisa mengubah&simpan urutan seksi
  formulir biodata.
- **`ais/database/model/ItemBiayaPunyaPiutang.java`** (r83589) —
  160→511 baris, 100% (semua anggota). `getFakultas()` write-back
  TERKONFIRMASI PENUH: cukup buka tab "Akun Piutang" tanpa edit apa pun
  untuk memicu `UPDATE`+revisi Envers palsu. Broken access control:
  cek `CommonPrivilages.UPDATE` dikomentari mati di helper — siapa pun
  yang bisa buka layar Item Biaya bisa mengubah pemetaan akun piutang
  (mempengaruhi jurnal akuntansi) tanpa hak UPDATE.
- **`ais/database/model/ItemBiayaPunyaDiskon.java`** (r83590) —
  160→449 baris, 100% (31 anggota). Struktur IDENTIK KATA-PER-KATA
  dengan `ItemBiayaPunyaPiutang` (diverifikasi via `sed`+`diff` otomatis
  terhadap versi pristine). **TEMUAN PALING SIGNIFIKAN BATCH INI**:
  `ItemBiaya.ambilDiskon()` TIDAK ADA (beda dari 3 saudaranya yang semua
  punya resolver 8-tahap) — seluruh data pemetaan diskon di modul ini
  YATIM/WRITE-ONLY, tidak pernah dibaca mesin akuntansi sama sekali.
  Diskon sesungguhnya dibukukan lewat jalur `ItemBiaya.DIKALI_NILAI_MINUS`
  terpisah yang tetap memakai `ItemBiayaPunyaAkun`.
- **`ais/database/model/KelompokParameterTambahanCalonMahasiswa.java`**
  (r83591) — 186→703 baris, 100% (38 anggota). Struktur IDENTIK
  `KelompokParameterTambahanAlumni`. Kuirk: kelompok default lahir
  dengan `tampilDiFormPendaftaran=false` — field tambahan yatim yang
  diadopsi ke situ TIDAK MUNCUL di form publik sampai admin mencentang
  manual (asimetri "aman secara bawaan" yang tampak disengaja). SQL
  migrasi mentah di `ParameterTambahanPaketAction` dijalankan ULANG
  setiap layar dibuka, melewati Envers. **Contoh keamanan POSITIF**
  (guard lengkap READ/CREATE/UPDATE/DELETE, nol SQL injection).

**Pola "getKeterangan() membalik kontrak base class"**: 3 dari 5 file
batch ini (kedua entity `ItemBiayaPunya*` TIDAK punya field
`keterangan` sama sekali). Total kumulatif: 13 (akhir b36) + 3 =
**16 instance**.

Total akumulasi 37 sesi: **363 file** dari 7.401 (~4,9%).

## Batch 36 — SELESAI 100% (3 Sep 2026) — RANTAI MASTER KEGIATAN DOSEN/MAHASISWA TERPETAKAN LENGKAP

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/BerkasHasilAkreditasiPunyaNama.java`** (r83580) —
  186→662 baris, 100% (37 anggota). Kedua dugaan sesi 35 TERKONFIRMASI:
  metadata bibliografi + lampiran fisik di `LampiranLain`, pola
  `DspaceInformation.linksForClass` hanya mendaftarkan entity anak.
  Bug fungsional konkret: ekspor Excel — kolom hyperlink lampiran SELALU
  ketimpa nomor urut karena urutan penulisan sel yang salah (perbaikan
  1 karakter, di luar cakupan).
- **`ais/database/model/KelompokKegiatanKedosenan.java`** (r83582) —
  193→667 baris, 100% (41 anggota). Puncak rantai master Kegiatan Dosen.
  Bug write-back BARU: `getJenis()` menurunkan bidang Tridharma dari 12
  nama harfiah lalu MENYIMPAN tebakan itu ke DB (+ revisi Envers palsu)
  saat baris kebetulan dibaca dalam sesi aktif. Nama kolom FK salah
  salin-tempel: `jenisKelompokKegiatanKedosenan` dipetakan ke kolom
  `skala_kegiatan_kedosenan`.
- **`ais/database/model/DetailKelompokKegiatanKedosenan.java`** (r83583)
  — 178→561 baris, 100% (36 anggota). Premis brief soal bobot SEBAGIAN
  KELIRU: bobot TIDAK ada di entity manapun dalam rantai, melainkan di
  `ParameterUmum` (tabel key-value) lewat kunci string rakitan dari 4 id.
  **BUG SERIUS ditemukan**: layar "Konfigurasi BKD" menulis ke
  `Konfigurasi` dengan urutan segmen kunci TERTUKAR (jabatan/skala
  terbalik), sedangkan pembaca sesungguhnya (`BkdKegiatanDosenHelper`)
  membaca dari `ParameterUmum` — DUA TABEL BERBEDA SAMA SEKALI. Input
  admin di layar itu tidak pernah terbaca; satu-satunya jalur yang
  berfungsi adalah layar "Nilai Kegiatan Kedosenan" terpisah. Plus bug
  `TreeSet` penciutan senyap yang membuat sebagian sel matriks bobot
  MUSTAHIL diisi via UI (selalu 0.0).
- **`ais/database/model/KelompokKegiatanKemahasiswaan.java`** (r83584) —
  169→640 baris, 100% (40 anggota). Padanan sisi mahasiswa, hierarki
  TERNYATA 3 tingkat (bukan 2 seperti dugaan awal). Bug kolom FK salah
  salin-tempel YANG SAMA PERSIS dengan versi dosen: kolom
  `skala_kegiatan_kemahasiswaan` dipakai untuk relasi ke
  `JenisKelompokKegiatanKemahasiswaan` — bug kembar lintas modul
  terverifikasi 2x independen di batch yang sama.
- **`ais/database/model/DetailKelompokKegiatanKemahasiswaan.java`**
  (r83585) — 184→672 baris, 100% (36 anggota). **Asimetri struktural
  penting vs sisi dosen**: bobot mahasiswa TIDAK di `ParameterUmum`,
  melainkan di entity KHUSUS `NilaiKegiatanKemahasiswaan` (sudah selesai
  batch sebelumnya) dengan kunci gabungan bertanda `unique=true` — jauh
  lebih rapi dari mekanisme sisi dosen. `getBisaDipilihMahasiswa()`
  destruktif (kembaran `getBisaDipilihDosen()` file saudara di batch
  ini) — satu arah, tidak self-healing, DAN non-deterministik (bergantung
  urutan pemanggilan getter lain akibat guard `Hibernate.isInitialized`).

**Pola "getKeterangan() membalik kontrak base class"**: 3 dari 5 file
batch ini punya pola ini (`BerkasHasilAkreditasiPunyaNama`,
`KelompokKegiatanKedosenan`, `KelompokKegiatanKemahasiswaan` — TIDAK ada
di kedua entity "Detail*"). Sesuai perbaikan proses dari batch 35, agent
hanya melaporkan ada/tidak (bukan nomor urut) — total kumulatif
sebenarnya: 10 (akhir b35) + 3 = **13 instance**.

**Keluarga getter destruktif via property access terus bertambah**:
`getJenis()` (KelompokKegiatanKedosenan), `getBisaDipilihDosen()`
(DetailKelompokKegiatanKedosenan), `getBisaDipilihMahasiswa()`
(DetailKelompokKegiatanKemahasiswaan) — semuanya varian pola yang sudah
tercakup `task_15f5001e`, memperkuat kesimpulan bahwa pola ini SANGAT
tersebar luas di seluruh `GeneralValueObject` subclass manapun yang
extends dengan property-access + `dynamicUpdate=true`.

Total akumulasi 36 sesi: **358 file** dari 7.401 (~4,8%).

## Batch 35 — SELESAI 100% (3 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/OrganisasiIntraKampusPunyaMahasiswa.java`** (r83571)
  — 204→839 baris, 100% (46 anggota). **Kedua dugaan kuirk sesi 34
  TERKONFIRMASI PENUH**: `getTbmuser()` destruktif (menulis `null` ke DB
  saat pengaju adalah mahasiswa — kasus NORMAL di entity ini, jadi
  dampaknya lebih luas dari kembaran dosennya) DAN `getTahun()` mutatif
  (tapi TERNYATA justru satu-satunya penulis kolom itu, bersifat
  self-healing kecuali `mulai` di-null-kan). **Temuan amplifier baru**:
  jalur `DataUtil.CLASS_IZINKAN` + `BeanUtilsBean.copyProperties` merusak
  **instance kanonik bersama lintas-JVM** (bukan cuma nilai kembalian) —
  detail penting baru untuk `task_15f5001e`.
- **`ais/database/model/DokumenAkreditasi.java`** (r83569) — 203→594
  baris, 100% (39 anggota). Premis awal KELIRU (bukan sumber angka
  borang, melainkan pohon arsip dokumen). Ditemukan pola dual-jalur
  unduh identik `LampiranLain` (servlet `/document` bergerbang 401,
  tapi `document.zul` merender tautan `/al` tanpa gerbang) — memperkuat
  `task_b82b25d2`.
- **`ais/database/model/BerkasHasilAkreditasi.java`** (r83568) — 176→666
  baris, 100% (39 anggota). Premis awal juga KELIRU (bukan entity
  sertifikat/nilai, melainkan wadah/kategori borang tanpa kolom hasil).
  Bug fungsional nyata: impor Excel tidak memetakan relasi pemilik →
  baris hasil impor yatim, hilang permanen dari SEMUA tab UI (nol
  filter yang cocok).
- **`ais/database/model/SkripsiPunyaKomponenPenilaianSkripsi.java`**
  (r83570) — 199→689 baris, 100% (23 anggota). Premis awal KELIRU (bukan
  sisi transaksi nilai — murni tabel penghubung master, nilai
  sesungguhnya CSV di `Skripsi.detail_nilai`). Bug slot-swap dosen/nilai
  TIDAK menyeberang ke sini (kosakata nama menular tapi kode mati total).
  Bug integritas audit baru: `simpan()` hapus via SQL MENTAH melewati
  Envers, id baris berganti total tiap kali format disimpan.
- **`ais/database/model/KegiatanKedosenanPunyaDosen.java`** (r83572-73)
  — 210→743 baris, 100% (46 anggota). Pola "banyak nama Kegiatan* tak
  berkerabat" TERKONFIRMASI berlaku (nol relasi ke `Kegiatan`/
  `DetailKegiatan` billing), TAPI nama class ini SENDIRI **tidak
  menyesatkan** (cocok label UI). `getTbmuser()` destruktif — KEMBARAN
  KATA-PER-KATA bug `OrganisasiDosenPunyaDosen` (b30). `getPersetujuan()`
  JUGA destruktif: menarik persetujuan kegiatan induk mengosongkan
  persetujuan SETIAP peserta satu-per-satu saat baris kebetulan dibaca.
  NPE dijamin di ekspor DSpace untuk baris tanpa keterangan.

**KOREKSI PENTING — tabrakan penghitungan paralel**: brief batch ini
memberi baseline "pola getKeterangan() 6 instance" ke SEMUA 5 agent
sekaligus (tanpa tahu sesama agent paralel). 4 dari 5 file batch ini
TERNYATA punya pola yang sama (`BerkasHasilAkreditasi`,
`SkripsiPunyaKomponenPenilaianSkripsi`, `OrganisasiIntraKampusPunyaMahasiswa`,
`KegiatanKedosenanPunyaDosen` — HANYA `DokumenAkreditasi` yang TIDAK),
dan masing-masing MELAPORKAN DIRI SENDIRI sebagai "instance ke-7" secara
independen tanpa sadar 3 lainnya juga demikian. **Total sebenarnya: 6 + 4
= 10 instance.** Pelajaran proses: saat memberi angka baseline count ke
banyak agent PARALEL di brief yang sama, ingatkan bahwa file lain di
batch yang sama BISA JADI juga menambah — jangan biarkan tiap agent
melaporkan nomor urut mutlak, cukup laporkan "ada/tidak" dan biarkan
orkestrator yang menjumlahkan di akhir.

Total akumulasi 35 sesi: **353 file** dari 7.401 (~4,8%).

## Batch 34 — SELESAI 100% (3 Sep 2026) — ENUMERASI TANPA AUTENTIKASI DITEMUKAN

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/ScholarArticle.java`** (r83546) — 170→811 baris,
  100% (36 anggota). Konfirmasi arah relasi `@ManyToMany` dengan
  `ScholarAuthor`: UNIDIRECTIONAL, satu-satunya arah baca artikel→penulis.
  Dugaan sesi 31 terkonfirmasi: `getNama()` menulis balik hasil pembersihan
  `[PDF]`/`[BUKU]` ke field — rantai write-back berlapis lintas tabel
  (memicu UPDATE di `scholar_article` DAN `penelitiandanpengabdian.artikel`
  sekaligus). Pengecualian KELIMA kontrak base class (`getKeterangan()`).
  Koreksi status sesi 33: tombol pencarian kata kunci Scholar TERNYATA
  masih hidup (toolbar kedua non-mahasiswa tidak `setVisible(false)`).
- **`ais/database/model/DiskusiKomentar.java`** (r83548) — 196→848 baris,
  100% (43 anggota). **TEMUAN TERBESAR BATCH INI**: anonimitas peer-review
  dikonfirmasi ulang fiktif (nol kolom anonimitas), DAN ditemukan jalur baca
  KEDUA yang lebih parah dari IDOR sesi 30 — `POST /Data` dengan
  `tanpaLogin=true` + `action=daftar` mengeluarkan badan pesan peer-review
  rahasia TANPA LOGIN SAMA SEKALI (aksi baca tidak diblokir seperti aksi
  tulis), plus `where1..10` diteruskan mentah ke `sqlRestriction` di jalur
  yang sama. Menaikkan severity `task_493423ef` dari "IDOR terautentikasi"
  ke "enumerasi tanpa autentikasi". Importer OJS ternyata tidak pernah
  memindahkan isi komentar historis (tabel `notes` tidak diimpor) — seluruh
  utas hasil migrasi adalah cangkang kosong.
- **`ais/database/model/MetaReport.java`** (r83547) — 196→642 baris,
  100% (42 anggota). Premis awal (definisi laporan Jasper) KELIRU —
  ternyata jejak cetak dokumen akademik untuk verifikasi keaslian, dan
  fiturnya MATI TOTAL: nol baris pernah dibuat di codebase manapun, layar
  verifikasi dijamin NPE seandainya ada baris (id komponen ZK hilang dari
  `.zul`), dan modul "baru" merujuk nama kelas yang tidak ada. Nol query
  SQL mentah — tidak menambah bukti `task_493423ef`.
- **`ais/database/model/RekapAngketUntukDosen.java`** (r83549) — 198→587
  baris, 100% (48 anggota). Entity sendiri BERSIH (contoh positif, agregat
  murni tanpa kolom pemilih). Tapi investigasi anonimitas angket
  menemukan **pelanggaran konkret di sekitarnya**: satu guard `setVisible`
  yang hilang di `LaporanAngketDosenPerDosenWindow.java` (tab "Data Angket
  Dosen"/"Data Angket Umum") membuat dosen bisa melihat matriks NIM+nama
  mahasiswa × nilai angket individual yang mereka berikan. **Task baru
  dibuat: `task_72336ffe`.** Tabel kembar `rekap_angket_dosen`/
  `RekapAngketUntukDosen` punya query jrxml identik kata-per-kata kecuali
  nama tabel — laporan versi dosen kemungkinan selalu kosong.
- **`ais/database/model/OrganisasiIntraKampus.java`** (r83550) — 183→880
  baris, 100% (43 anggota). Perbandingan dengan `OrganisasiDosen` (b32):
  TIDAK ada field level (bug pelaporan A-4.5.5 tidak punya padanan), tapi
  `minimal*` (IPK/SKS/SKKM) BENAR-BENAR ditegakkan (kebalikan pola
  write-only). SQL injection BARU (`initCriteria`, kembaran sesi 32).
  Inversi hak akses dengan BUKTI KUAT: kelas saudara persis
  (`JabatanOrganisasiIntraKampusAction`, tab layar yang sama) memasang
  guard CREATE/UPDATE/DELETE lengkap, kelas ini nol — anomali terisolasi,
  bukan gaya arsitektur. Impor Excel bypass total kontrol bisnis
  (persetujuan borongan langsung dari sel berkas).

**Pola "getKeterangan() membalik kontrak base class" kini 6 instance**
(`Bank` b29, `SintaArticle` b31, `PendaftaranSidang` b33, `ScholarAuthor`
b33, `ScholarArticle` b34, `DiskusiKomentar`/`Diskusi` b34) — pola
arsitektural mapan, konsisten di seluruh entity `hbm2java`-turunan.

**Task keamanan baru batch ini**: `task_72336ffe` (guard anonimitas angket
dosen hilang, perbaikan spesifik dan bisa langsung dieksekusi).

Total akumulasi 34 sesi: **348 file** dari 7.401 (~4,7%).

## Batch 33 — SELESAI 100% (3 Sep 2026) — AKAR PENYEBAB POLA FAIL-OPEN DITEMUKAN

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/SatuanKerjaPegawai.java`** (r83540) — 149→457
  baris, 100% (29 anggota). **TEMUAN TERBESAR BATCH INI**: investigasi akar
  penyebab pola fail-open (`satuanKerjas.size()==0 → "1=1"`) menemukan
  sumber sesungguhnya di `SekolahUtil.ambilSatuanKerjas()` (BUKAN entity
  ini) — 4 kondisi presisi penyebab himpunan kosong, termasuk temuan
  mengejutkan: admin yang SALAH KETIK kode satuan kerja saat MEMBATASI
  role justru MEMPERLUAS akses ke seluruh data lintas unit (inversi).
  Skala jauh lebih besar dari dugaan: **153 file** mengandung pola ini,
  bukan segelintir Action. Bahkan layar master entity ini sendiri juga
  fail-open. Memperkuat `task_1214dd58`/`task_9b7ff647` secara drastis.
- **`ais/database/model/AsramaPunyaMahasiswa.java`** (r83539) — 150→521
  baris, 100% (30 anggota). Bug `KelasPunyaMahasiswaTemporary` (b28)
  TIDAK terulang di sini. Tapi ditemukan bug data nyata: `syncAsrama`
  cuma memangkas (anggota manual lenyap senyap), dan `save()` di
  helper penambah lupa filter asrama — menambahkan mahasiswa ke asrama
  B bisa diam-diam MEMINDAHKANNYA dari asrama A.
- **`ais/database/model/PendaftaranSidang.java`** (r83542) — 145→535
  baris, 100% (30 anggota). Fitur PRAKTIS MATI TOTAL — bahkan wiring
  ZUL rusak (klik tombol dijamin NPE, class Action yang dirujuk tidak
  ada di codebase). Alur sidang sungguhan lewat `Skripsi`/
  `GelombangPendaftaranSidangTugasAkhir`. Pengecualian KETIGA kontrak
  base class (`getKeterangan()` bisa `null`). **Saran agent untuk batch
  berikutnya: `PendaftaranWisuda.java`** (modul aktif, banyak bendera
  persetujuan finansial — permukaan audit menarik).
- **`ais/database/model/BeasiswaPunyaItemBiayaTambahan.java`** (r83541)
  — 143→573 baris, 100% (19 anggota). Seluruh jalur UI+keuangan DORMAN
  (dead code) — tidak menambah risiko `task_51f767ec`.
- **`ais/database/model/ScholarAuthor.java`** (r83544) — 155→644 baris,
  100% (29 anggota). Pengecualian KEEMPAT kontrak base class. Ditemukan
  potensi JS injection + path traversal terbatas di jalur crawler
  (severity rendah-menengah, mayoritas jalur dorman).

**Pola "getKeterangan() membalik kontrak base class" kini 4 instance**
(`Bank` b29, `SintaArticle` b31, `PendaftaranSidang` b33, `ScholarAuthor`
b33) — cukup sering untuk dicatat sebagai variasi arsitektural yang
dikenal, bukan anomali terisolasi.

Total akumulasi 33 sesi: **343 file** dari 7.401 (~4,6%).

## Batch 32 — SELESAI 100% (3 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/OrganisasiDosen.java`** (r83533) — 171→637 baris,
  100% (37 anggota). Konfirmasi PENUH bug pelaporan sesi 30: borang
  akreditasi BAN-PT A-4.5.5 salah baca level organisasi dari nama JABATAN
  (bukan `levelOrganisasiDosen`) — SETIAP baris terlapor "Lokal". Field
  `levelOrganisasiDosen` sendiri wajib-isi tapi praktis write-only (hanya
  dibaca 2 tempat, keduanya di Action-nya sendiri). Instance SQL injection
  BARU (`initCriteria`, parameter pencarian nama/NIDN mentah) — masuk
  `task_493423ef`. Pola inversi hak akses lagi (gerbang UPDATE/DELETE
  sengaja dikomentari).
- **`ais/database/model/DendaPembayaranNominal.java`** (r83536) — 159→745
  baris, 100% (34 anggota). Kembaran `DendaPembayaran` (b25) — JUGA fitur
  mati, tapi dengan 3 perbedaan perilaku halus dari kembarannya (guard
  null asimetris, entity ini "menang" saat kedua blok berjalan). Contoh
  positif keamanan.
- **`ais/database/model/AsesorPenunjangKinerjaDosen.java`** (r83536) —
  158→589 baris, 100% (35 anggota). Premis salah — ini master PERAN
  asesor (Asesor I/II/III), bukan kategori "penunjang". Jalur ZK sendiri
  contoh positif. Detail baru untuk `PenilaianAsesor` (b28): parameter URL
  4 dimensi (`pegawai`/`ta`/`smt`/`asesor`) semua dikendalikan klien. Bug
  fungsional: menonaktifkan 1 peran mengubah persentase kinerja SEMUA
  dosen secara surut, bisa hasilkan `NaN`/`Infinity` di laporan tanpa
  exception.
- **`ais/database/model/Staff.java`** (r83536) — 164→644 baris, 100% (31
  anggota). Klarifikasi: BUKAN modul ebisnis.id — master pejabat penanda
  tangan dokumen akademik (dekan/rektor/kaprodi). Konfirmasi independen:
  TIDAK ADA kredensial sama sekali. Bug nyata: pencarian kunci `staff`
  sebagian besar `eq` case-sensitive huruf kecil vs data yang ditulis
  berkapital ("Dekan") — banyak jenis laporan gagal SENYAP, blok tanda
  tangan tercetak kosong tanpa error.
- **`ais/database/model/Asrama.java`** (r83536) — 156→558 baris, 100%
  (34 anggota). Premis dikoreksi — asrama di AIS hanya label bercakupan
  (fakultas/jurusan/angkatan), TANPA kapasitas/kamar/alamat sama sekali.
  Sinkronisasi penghuni otomatis bersifat replace-all destruktif.

Total akumulasi 32 sesi: **338 file** dari 7.401 (~4,6%).

## Batch 31 — SELESAI 100% (3 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/Kota.java`** (r83524) — 192→587 baris, 100% (33
  anggota). Beda dari `Propinsi`: `getNama()` di sini MENULIS BALIK ke
  field. Kolom `kode` praktis mati lewat UI tapi merambat jadi bug data di
  hierarki `Wilayah` di bawahnya. Koreksi sesi 27: `PropinsiAction`
  sebenarnya PUNYA gerbang CREATE/UPDATE/DELETE (diwarisi
  `GenericCrudAction`) — yang nihil hanya gerbang READ (whitelist
  `MUST_CHECKED`).
- **`ais/database/model/Wilayah.java`** (r83525) — 189→717 baris, 100%
  (43 anggota). Dualitas hierarki wilayah AIS kini terjelaskan LENGKAP:
  hierarki klasik (`Propinsi→Kota`, berhenti di kota) vs `Wilayah` (satu
  tabel self-reference, satu-satunya yang punya kecamatan) — keduanya
  saling menumbuhkan isi lewat jembatan otomatis tanpa ada yang membuka
  layar master. Bug nyata: impor JSON Feeder salah memetakan `induk` ke
  id barisnya sendiri (self-reference); menyunting 1 kecamatan bisa
  memindahkan seluruh kota/kabupaten induknya ke provinsi lain. **2
  instance SQL injection baru** (`FeederImporter.wilayah()`,
  `WilayahKecamatanAction.exportKeFeeder()`) — masuk `task_493423ef`.
- **`ais/database/model/DiskusiPengumumanPerkuliahan.java`** (r83525) —
  190→671 baris, 100% (33 anggota). Konfirmasi: ini entity diskusi
  akademik yang BENAR (beda total dari `Diskusi.java` modul jurnal, b30).
  Pola "dua jalur, satu bergerbang satu tidak" terulang (mirip
  `Komentar`/`PenilaianAsesor`) — helper detail nol gerbang kepemilikan,
  bisa hapus/ubah komentar siapa pun. Temuan privasi baru: email
  notifikasi menyiarkan SELURUH transkrip percakapan ke semua peserta
  setiap ada komentar baru.
- **`ais/database/model/SintaArticle.java`** (r83523) — 188→584 baris,
  100% (46 anggota). Data publikasi ilmiah publik, risiko rendah. Fitur
  sinkronisasi SINTA saat ini DORMAN di UI (tombol tidak pernah
  ditambahkan ke toolbar).
- **`ais/database/model/ParameterUmum.java`** (r83529) — 183→697 baris,
  100% (44 anggota). Ternyata entity KEMBAR `Konfigurasi` (bukan
  survei/kuesioner seperti dugaan). Kuirk arsitektural mencolok: tabel
  pengaturan global dipakai sebagai "buku catatan" per-mahasiswa oleh
  `CommonReportHelper` (kunci berisi NIM+tahun+semester) — sekadar
  merender surat tagihan yang belum dicetak menyisipkan baris baru,
  tumbuh sebanding jumlah mahasiswa×tahun×semester. Instance lain pola
  inversi hak akses (gerbang READ ada, gerbang UPDATE nihil di seluruh
  `ParameterUmumAction`).

Total akumulasi 31 sesi: **333 file** dari 7.401 (~4,5%).

## Batch 30 — SELESAI 100% (3 Sep 2026) — TEMUAN KEAMANAN PALING KRITIS SELURUH INISIATIF

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika. Batch ini memprioritaskan `LampiranLain`/`Pesan` — inti
mekanisme 2 kerentanan IDOR terbesar proyek:

- **`ais/database/model/file/LampiranLain.java`** (r83517/83519/83520,
  666→2020 baris, 3 commit bertahap) — **TEMUAN PALING KRITIS SELURUH
  INISIATIF, memperkuat `task_b82b25d2` secara drastis**. Mekanisme
  `usingId=true` MEMATIKAN filter `jenis` SEKALIGUS mencocokkan langsung
  ke primary key — penyerang TIDAK PERLU tahu `jenis`/`clazz` sama sekali,
  cukup enumerasi id berurutan (`IDENTITY`). Penghapusan lampiran ternyata
  cuma SOFT DELETE (`ref` diubah ke sentinel `-111111119`) — dokumen
  KTP/KK/ijazah yang "dihapus" pengguna TETAP terjangkau penuh via
  `usingId=true` (mode itu mengabaikan `ref` sama sekali). Dikonfirmasi
  `/AmbilLampiran` (butuh login) dan `/al` (publik anonim) memetakan ke
  SERVLET SAMA — gerbang di path pertama tidak berarti apa-apa. URL statis
  bypass servlet dikonfirmasi (nama file deterministik `DES(id+kelas)`,
  bukan nonce). Bonus: upload `.jrxml` sebagai lampiran berpotensi RCE
  (ekspresi Java dieksekusi JasperReports) — belum diaudit lebih lanjut.
- **`ais/database/model/Pesan.java`** (r83516) — 259→1008 baris, 100%
  (59 anggota). **Bukti terkuat untuk `task_493423ef`** — rantai
  eksploitasi IDOR baca chat pribadi dikonfirmasi persis. Pemberat baru:
  chat "efemeral" yang dihapus dari tabel utama TERSALIN PERMANEN ke
  tabel audit (`store_data_at_delete=true`) — jendela eksploitasi jauh
  lebih besar dari dugaan. Jalur ZK chat asli BERSIH (scoping benar).
  Bonus temuan: modul perpustakaan menumpang tabel ini sebagai antrean
  tiket dengan filter kepemilikan di MEMORI, bukan SQL.
- **`ais/database/model/Diskusi.java`** (r83518) — 197→947 baris, 100%
  (52 anggota). Premis salah — ini modul editorial JURNAL ILMIAH
  (peer-review), bukan diskusi akademik. Kebijakan `visibility`/
  `anonymity_mode` TIDAK PERNAH DITEGAKKAN (`DOUBLE_ANONYMOUS` tidak
  menyembunyikan identitas siapa pun). Satu-satunya cara baca korespondensi
  editorial rahasia = IDOR `/Api dataRinci` yang sama.
- **`ais/database/model/KelasPmb.java`** (r83512) — 209→677 baris, 100%
  (43 anggota). Instance baru inversi hak akses (checkbox "Penuh" tanpa
  gerbang) + tombol "Ambil Calon Mahasiswa"/"Bersihkan" massal tanpa
  gerbang sama sekali.
- **`ais/database/model/OrganisasiDosenPunyaDosen.java`** (r83514) —
  203→701 baris, 100% (36 anggota). Bug kembar persis `Komentar.
  getTbmuser()` (b27) — identitas pengaju dihapus permanen oleh getter
  destruktif. Pola inversi hak akses lagi (tombol "Bersihkan" massal).

**Strategi "tindak lanjuti entity kunci yang disebut temuan sebelumnya"
kini TERBUKTI PALING PRODUKTIF sejauh ini** — `LampiranLain` sendirian
menghasilkan detail teknis yang mengubah total pemahaman severity
`task_b82b25d2` dari "IDOR yang perlu menebak jenis/clazz" menjadi
"enumerasi id sekuensial tanpa syarat apa pun, termasuk data yang sudah
di-soft-delete".

Total akumulasi 30 sesi: **328 file** dari 7.401 (~4,4%).

## Batch 29 — SELESAI 100% (3 Sep 2026) — BATCH TINDAK LANJUT KEAMANAN LANGSUNG

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika. Batch ini SENGAJA menindaklanjuti temuan keamanan batch
23/28 secara langsung — hasilnya SANGAT produktif:

- **`ais/database/model/Bank.java`** (r83503) — 150→516 baris, 100% (31
  anggota). Konfirmasi: komentar generator "Bank generated by hbm2java"
  DI FILE INI memang benar — sumber asli yang dibajak puluhan entity lain
  sepanjang proyek. Tidak menyimpan kredensial sama sekali. Kuirk:
  `getKeterangan()` di sini MEMBALIK kontrak base class (bisa `null`,
  padahal `GeneralValueObject` menjamin non-null) — pengecualian pertama
  atas klaim Javadoc base class yang ditemukan.
- **`ais/database/model/AkunManajemen.java`** (r83507) — 147→540 baris,
  100% (31 anggota). **KONFIRMASI INDEPENDEN ULANG password plaintext**
  (modul ebisnis.id): dicetak acak, disimpan tanpa hash apa pun, bahkan
  dikirim balik di response JSON (`qrData`). Tersalin PERMANEN ke tabel
  audit Envers meski baris dihapus. Terjangkau via `/Api dataRinci` dengan
  token AIS biasa (mahasiswa manapun) — `ManajemenProperty` dikonfirmasi
  TIDAK punya daftar-hitam properti sensitif sama sekali. Menaikkan
  severity `task_493423ef` signifikan (kredensial siap-pakai, bukan cuma
  ciphertext).
- **`ais/database/model/BlacklistIp.java`** (r83508) — 144→556 baris,
  100% (27 anggota). **KONFIRMASI DEFINITIF untuk `task_78a5b1ab`**:
  mekanisme blokir IP bisa dilewati dengan SATU HEADER (`X-Real-IP`/
  `X-Forwarded-For` dipercaya tanpa validasi proxy tepercaya). Pemeriksaan
  terjadi SETELAH password diverifikasi (nol proteksi brute force + bocor
  info kredensial benar). **Fail-open baru**: cache in-memory diam-diam
  jadi KOSONG TOTAL begitu tabel `blacklist_ip` melebihi 100 baris
  (`preload_maks_baris_kecil` default) — makin rajin diblokir, makin besar
  peluang penegakan mati. Bug tambahan: wildcard `"*"` tunggal → kunci
  seluruh sistem tanpa validasi.
- **`ais/database/model/Program.java`** (r83505) — 143→553 baris, 100%
  (30 anggota). Konfirmasi keanggotaan mahasiswa↔program berbasis TEKS
  (pola sama `Kelas`) meski ada kolom FK `program_baru` yang cuma cache
  turunan. **Instance KELIMA** pola "panel detail nol-otorisasi, assign
  massal" (`ProgramMahasiswaDetailAction`, sampai 5000 mahasiswa/klik).
- **`ais/database/model/BankHost.java`** (r83506) — 149→578 baris, 100%
  (25 anggota). Field `username`/`password` dorman (komentar di Action,
  tapi terpetakan Hibernate & terjangkau endpoint reflektif). **TEMUAN
  SANGAT SERIUS**: otentikasi mitra H2H payment gateway 100% berbasis IP
  header yang dispoofable, konfigurasi default MEMBUAT baris `BankHost`
  OTOMATIS untuk IP tak dikenal dengan status AKTIF, DAN ada baris
  wildcard `"0.0.0.0"` yang menerima IP manapun sebagai mitra sah.

**REKAP dampak batch 29**: 3 dari 5 file menghasilkan konfirmasi/perkuatan
LANGSUNG terhadap task eksisting dengan detail teknis baru yang signifikan
(`task_493423ef` naik severity, `task_78a5b1ab` dapat bukti definitif,
`task_9b7ff647` dapat instance H2H gateway). Strategi "tindak lanjuti
temuan sebelumnya secara langsung" (bukan cuma sweep umum) terbukti sangat
efisien untuk memperdalam investigasi keamanan yang sudah dimulai.

Total akumulasi 29 sesi: **323 file** dari 7.401 (~4,4%).

## Batch 28 — SELESAI 100% (3 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/JenisPengaduan.java`** (r83498) — 175→704 baris,
  100% (35 anggota). **TEMUAN SANGAT SIGNIFIKAN untuk `task_18d52b8b`**:
  entity ini sendiri TIDAK berkontribusi pada bug routing whistleblower
  (tidak punya field penerima sama sekali — rute 100% dari rantai atasan
  pelapor). TAPI ditemukan jalur KEDUA kebocoran identitas pelapor yang
  TERPISAH: layar `LaporanPengaduan` (dipicu `PengaduanAction#onLaporan`)
  NOL pemeriksaan hak akses DAN nol scoping atasan-bawahan — siapa pun
  bisa menarik keluar SELURUH aduan suatu jenis + identitas pelapor lengkap
  cukup dengan memilih combo. Direkomendasikan perluas `task_18d52b8b`
  mencakup `LaporanPengaduan`.
- **`ais/database/model/Penghasilan.java`** (r83494) — 175→522 baris, 100%
  (35 anggota). Contoh positif keamanan. Bug: `getNama()` selalu
  membangkitkan ulang label dari rentang angka dan menimpa isian manual
  — render daftar saja memicu UPDATE. Dashboard turunannya
  (`DashboardMahasiswaPenghasilanOrtu`) mengekspos PII lengkap (nama+
  alamat+RT/RW+penghasilan 3 ortu) tanpa gerbang sendiri — masuk
  `task_44ea51dd`.
- **`ais/database/model/Investor.java`** (r83495) — 173→554 baris, 100%
  (37 anggota). **PENEMUAN BARU PENTING**: entity ini bagian dari lini
  produk TERPISAH "ebisnis.id" (SaaS POS/ERP multi-tenant) yang menumpang
  codebase & session factory AIS yang sama — bukan modul akademik.
  Jalur dashboard-nya sendiri CONTOH POSITIF terbaik (scoping via session
  server, bukan parameter klien). Tapi tabel `investor`/`akun_manajemen`
  menyimpan PASSWORD PLAINTEXT yang terjangkau via `/Api dataRinci`
  (`task_493423ef`) — menaikkan severity task itu (password langsung
  pakai, bukan ciphertext).
- **`ais/database/model/PenilaianAsesor.java`** (r83497) — 171→529 baris,
  100% (46 anggota). Broken access control: parameter URL `?pegawai=<id>`
  tanpa cek kepemilikan + gerbang edit TAUTOLOGIS (membandingkan objek
  dengan dirinya sendiri, selalu true) — siapa pun bisa menulis ulang
  rekomendasi hasil asesmen BKD (dampak ke tunjangan/pelaporan) dosen
  manapun. Dua jalur ke data sama: satu benar (Helper), satu salah
  (Action) — pola berulang.
- **`ais/database/model/KelasPunyaMahasiswaTemporary.java`** (r83496) —
  161→469 baris, 100% (34 anggota). **BUG DATA KORUPSI NYATA**: proses
  batch penempatan kelas (`JamPerkuliahanSyncrhonizerProcessor.
  procesKelas()`) justru MENGOSONGKAN `Mahasiswa.kelas` karena
  `getNama()` mengembalikan `null` (properti warisan `GeneralValueObject`
  tak terpetakan) — konsekuensi NYATA dari kuirk "keharusan teknis" yang
  selama ini cuma dicatat sebagai catatan arsitektural. Bug kembar di
  `procesDosenPa()` (pakai PK baris antrean sebagai id dosen).

**Pola baru penting**: `KelasPunyaMahasiswaTemporary` adalah kasus PERTAMA
di mana kuirk `GeneralValueObject` bukan `@MappedSuperclass` (dicatat
ratusan kali sebagai "keharusan teknis, bukan bug") benar-benar
MENYEBABKAN bug data korupsi nyata di production — karena entity ini,
tidak seperti kebanyakan entity lain, TIDAK mendeklarasikan ulang properti
warisan yang justru dipakai kode pemanggil (`getNama()`). **Pelajaran**:
saat entity TIDAK mendeklarasikan ulang field warisan yang biasanya
di-override (nama/keterangan/kode), cek apakah ada kode pemanggil yang
mengasumsikan properti itu ada — potensi bug tersembunyi.

Total akumulasi 28 sesi: **318 file** dari 7.401 (~4,3%).

## Batch 27 — SELESAI 100% (3 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/Kelas.java`** (r83487) — 180→586 baris, 100% (21
  anggota). BUKAN jalur sekolah (dugaan awal salah) — kelas paralel
  akademik PT. Contoh POSITIF keamanan. Bug: penautan mahasiswa pakai
  NAMA bukan FK (`Mahasiswa.kelas` String, `ilike EXACT`); flag "update
  Dosen PA sekarang" tersimpan PERMANEN → tiap penyimpanan berikutnya
  (bahkan yang tak berniat mengubah Dosen PA) menimpa ulang Dosen PA
  seluruh mahasiswa dalam cakupan.
- **`ais/database/model/Komentar.java`** (r83487) — 203→678 baris, 100%
  (46 anggota). Entity AKTIF (beda dari `ChatMessage` b24 yang yatim) —
  komentar bimbingan KRS. **Temuan keamanan**: mahasiswa bisa MENGHAPUS
  komentar dosen pembimbing akademiknya sendiri (nol gerbang di
  `KomentarRenderer`) + stored XSS terbatas (sanitasi HTML cuma ganti
  substring "script"). Getter destruktif `getTbmuser()` menghapus
  identitas penulis permanen → nama yang tampil sebenarnya field audit
  `oleh` yang bisa berubah tiap update.
- **`ais/database/model/ProgramMahasiswa.java`** (r83486) — 188→603
  baris, 100% (48 anggota). Nama menyesatkan: BUKAN tabel penghubung,
  master aturan rentang semester (3 slot). **Instance KEEMPAT** pola
  "panel detail nol-otorisasi, assign massal sampai 5000 mahasiswa"
  (`ProgramDataMahasiswaDetailAction`) — kembaran persis 3 temuan
  sebelumnya (`KelompokMahasiswa`/`KelompokStatusKeluarMahasiswa`/
  `KelompokStatusMahasiswa`).
- **`ais/database/model/PembagianKuotaPerkuliahanBerdasarkantahunAngkatan.java`**
  (r83487) — 206→653 baris, 100% (41 anggota). Nama menyesatkan: BUKAN
  penjatahan kursi per angkatan, tapi "batas atas berbeda per angkatan"
  atas total peserta semua angkatan. **Temuan keamanan**: parameter URL
  `?perkuliahan=<id>` melewati gerbang login/READ SEPENUHNYA — menguatkan
  `task_9b7ff647` + `task_b82b25d2`.
- **`ais/database/model/Propinsi.java`** (r83487) — 192→533 baris, 100%
  (35 anggota). Layar masternya NOL pemeriksaan `checkPrevilages` sama
  sekali (bukan cuma whitelist tak lengkap). Jalur unggah Excel: nama
  berkas dari klien dipakai mentah untuk path penulisan file + impor bisa
  menimpa baris manapun berdasarkan id dari sheet. Kuirk: layar daftar
  MENULIS ke DB saat sekadar di-render (`simpanWilayah()` dipanggil per
  baris grid).

**REKAP pola "panel detail nol-otorisasi, assign massal"** — sekarang **4
instance terkonfirmasi**: `KelompokStatusKeluarMahasiswaDetailAction`(b24),
`KelompokStatusMahasiswaDetailAction`(b26), `ProgramDataMahasiswaDetail
Action`(b27), dan kemungkinan besar `KelompokMahasiswaDetailAction`(belum
diverifikasi eksplisit — b22 hanya mendokumentasikan efek UPDATE massal
`KelompokMahasiswa` sendiri, belum action detailnya). Pola ini sekarang
CUKUP KUAT untuk disimpulkan sebagai TEMPLATE arsitektural yang disalin
lintas modul "Kelompok*"/"Program*" — bukan kebetulan per-file.

Total akumulasi 27 sesi: **313 file** dari 7.401 (~4,2%).

## Batch 26 — SELESAI 100% (3 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/KelompokStatusMahasiswa.java`** (r83476) — 151→633
  baris, 100% (34 anggota). Melengkapi trilogi "Kelompok*" (menimpa status
  PER SEMESTER selama studi, beda dari status AWAL `KelompokMahasiswa` b22
  dan status KELUAR `KelompokStatusKeluarMahasiswa` b24). **Kembaran
  PERSIS temuan b24**: `KelompokStatusMahasiswaDetailAction` nol
  pemeriksaan hak akses, bisa memaksa status sampai 5.000 mahasiswa
  sekaligus, melewati gerbang pembayaran & batas studi.
- **`ais/database/model/StatusAwalMahasiswa.java`** (r83479) — 155→661
  baris, 100% (35 anggota, ~1.400 rujukan/194 file). Contoh POSITIF
  keamanan (checkbox grid ikut `setDisabled(!edit)`). Bug fungsional:
  kolom "Keterangan" di layar master TIDAK PERNAH tersimpan (bukan
  properti terpetakan Hibernate — warisan dari `GeneralValueObject` yang
  bukan `@MappedSuperclass`). Checkbox Pindahan/Alih Prodi tak bisa
  dimatikan untuk baris bernama mengandung kata kunci itu (write-back
  otomatis menimpa kembali).
- **`ais/database/model/MahasiswaJadiAsisten.java`** (r83480) — 152→616
  baris, 100% (35 anggota). Default tri-state berpihak "boleh": mahasiswa
  baru dicentang asisten LANGSUNG berwenang ubah presensi tanpa siapa pun
  mencentang izin eksplisit. **2 instance SQL injection BARU** ditemukan
  di `PenilaianAction.java` dan `CommonReportHelper.java` (parameter
  pencarian diinterpolasi mentah ke `sqlRestriction`) — masuk
  `task_493423ef`. Inversi hak akses lagi di helper pemberi wewenang
  nilai/presensi.
- **`ais/database/model/NilaiKegiatanKemahasiswaan.java`** (r83477) —
  151→607 baris, 100% (31 anggota). Master rubrik angka kredit (bukan
  nilai per mahasiswa seperti dugaan). Bug perhitungan nyata: predikat
  join di `Common.java:5430` salah ketik (`a.jabatan` seharusnya
  `h.jabatan`) — batasan jabatan hilang untuk peserta tanpa jabatan,
  angka kredit menggelembung. Instance baru inversi hak akses.
- **`ais/database/model/KeadaanKeluargaPengajuanBeasiswa.java`** (r83478)
  — 151→575 baris, 100% (34 anggota). **TEMUAN SANGAT SERIUS — memperkuat
  `task_b82b25d2` dan `task_51f767ec`**: dikonfirmasi jalur `/Data` action
  `daftar`/`load`/`cari` TIDAK termasuk daftar blokir `aksiSqlTulis`,
  sehingga flag `tanpaLogin` dari klien memungkinkan enumerasi SELURUH
  data sosial-ekonomi pemohon beasiswa (alamat, penghasilan ortu, kondisi
  rumah, narasi alasan memohon bantuan) TANPA akun sama sekali. Entity
  yatim (belum pernah dipakai lewat UI apa pun) tapi TERJANGKAU penuh
  lewat endpoint reflektif — mengaitkan `task_51f767ec` (broken access
  PengajuanBeasiswaAction) + `task_b1e610b6` (`/Data` generik) +
  `task_b82b25d2` (kebocoran PII tanpa autentikasi) jadi satu gambar besar.

**Pola sekarang: banyak entity "yatim"/belum-pernah-dipakai TETAP berisiko
tinggi** karena endpoint reflektif generik (`/Data`, `/Api`) menjangkau
SEMUA entity Hibernate terpetakan tanpa peduli apakah entity itu punya
layar UI aktif atau tidak — "belum pernah dipakai" bukan berarti "aman".

Total akumulasi 26 sesi: **308 file** dari 7.401 (~4,2%).

## Batch 25 — SELESAI 100% (3 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/BlokirMahasiswa.java`** (r83474) — 160→698 baris,
  100% (35 anggota). Mekanisme blokir SENDIRI fail-open: baris tanpa
  `keterangan` terisi tidak memblokir apa pun meski semua flag aktif
  (setiap query penegakan mensyaratkan `keterangan` non-kosong, tapi hanya
  UI yang memvalidasinya, jalur non-ZK bisa lolos). Blokir login pakai
  cache in-memory yang tidak pernah diinvalidasi Action-nya — baru
  berlaku setelah restart aplikasi. Blokir KRS cuma menjaga dialog
  pembuka, bukan jalur penulisan KRS sesungguhnya. Semua cocok task yang
  ada, tidak ada task baru.
- **`ais/database/model/ItemBiayaPunyaAkun.java`** (r83470) — 161→415
  baris, 100% (21 anggota). CONTOH POSITIF keamanan. Bug finansial nyata:
  `getFakultas()` menimpa field otomatis dari `jurusan` → baris yang
  dimaksudkan khusus 1 jurusan malah jadi fallback se-fakultas, salah
  atribusi akun pendapatan. Juga 2 mekanisme resolusi akun paralel yang
  bisa berbeda hasil (cascade 8-tahap vs SQL native `ORDER BY id DESC
  LIMIT 1`).
- **`ais/database/model/UploadBiodataCalonMahasiswa.java`** (r83473) —
  166→604 baris, 100% (36 anggota). **TEMUAN SANGAT SIGNIFIKAN, perluas
  `task_b82b25d2`**: analisis statis `applicationContext-security.xml`
  menunjukkan catch-all `/**` = `IS_AUTHENTICATED_ANONYMOUSLY` (baris 62)
  — berpotensi SELURUH `/pages/**` (bukan cuma 1 layar) reachable tanpa
  login sama sekali, karena `FilterJSP` juga tak memaksa cek login untuk
  `.zul` generik. Repo SENDIRI sudah punya komentar internal mengakui
  risiko serupa untuk `/al` (dibuka publik 19-08-2026, mitigasi belum
  dikerjakan). Langkah verifikasi konkret diusulkan: curl anonim ke layar
  ini, cek HTML ZK vs redirect `/login`. Data-loss bug terpisah: `getNama()`
  fallback nama gelombang ikut TERSIMPAN permanen saat batch lama disunting
  ulang tanpa upload baru.
- **`ais/database/model/DendaPembayaran.java`** (r83472) — 157→624 baris,
  100% (25 anggota). Fitur MATI: persentase denda yang diisi di layar ini
  TIDAK PERNAH dipakai menghitung tagihan (parameter diteruskan tapi tak
  pernah dibaca isinya) — tapi keberadaan barisnya tetap mempengaruhi
  pemilihan `JadwalPembayaran`, efek tak terduga. Bug validasi rentang
  tumpang-tindih sekelas `PesanRuangan` (b22). Contoh positif keamanan.
- **`ais/database/model/SettingBiayaDetail.java`** (r83471) — 161→537
  baris, 100% (34 anggota). Klarifikasi penting: BUKAN `DetailBiaya` —
  4 entity berbeda dalam keluarga `SettingBiaya`. Contoh positif (halaman
  induk ADA di whitelist `MUST_CHECKED`), tapi layar detail turunannya
  sendiri nol pemeriksaan hak akses (menempel sepenuhnya ke induk).

**Pola menarik batch ini**: 3 dari 5 file (`ItemBiayaPunyaAkun`,
`DendaPembayaran`, `SettingBiayaDetail`) adalah CONTOH POSITIF keamanan —
menyeimbangkan rentetan temuan negatif batch 20-24. Menunjukkan proteksi
di AIS TIDAK seragam buruk — tergantung Action mana yang menuliskannya.

Total akumulasi 25 sesi: **303 file** dari 7.401 (~4,1%).

## Batch 24 — SELESAI 100% (3 Sep 2026) — BATCH DENGAN TEMUAN KEAMANAN TERPADAT

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika. Batch ini menghasilkan **4 task eskalasi keamanan baru
sekaligus**:

- **`ais/database/model/JenisTabungan.java`** (r83465) — 180→646 baris,
  100% (36 anggota). Contoh POSITIF: layar tidak ada di whitelist
  `MUST_CHECKED` tapi Action-nya sendiri punya pemeriksaan eksplisit.
  Kehalusan: layar ini tak punya menu sendiri (dimuat sebagai tab di
  `DepositAction`), jadi hak aksesnya menempel ke menu Deposit induk.
- **`ais/database/model/JenisPengeluaranMahasiswa.java`** (r83464) —
  172→538 baris, 100% (29 anggota). Inversi hak akses terkonfirmasi lagi:
  checkbox Aktif/Default di grid tanpa gerbang `CommonPrivilages.UPDATE`
  sama sekali (langsung `refreshSaveOrUpdate`), sementara tombol Ubah/Hapus
  di baris sama dijaga penuh. Instance baru `task_9b7ff647` juga
  dikonfirmasi (halaman tak ada di `MUST_CHECKED`).
- **`ais/database/model/KelompokStatusKeluarMahasiswa.java`** (r83466) —
  172→648 baris, 100% (29 anggota). **TEMUAN AKADEMIK BERDAMPAK
  TERTINGGI**: `KelompokStatusKeluarMahasiswaDetailAction` (mengubah status
  "Lulus"/"Drop Out" sampai 5.000 mahasiswa sekaligus per klik) TIDAK
  PUNYA satu pun pemeriksaan hak akses di seluruh 1.062 barisnya — nol
  panggilan `checkPrevilages`/`doCheckSecurity`. Perbandingan dengan
  `KelompokMahasiswa` (b22): entity ini menimpa SAAT BACA (reversibel,
  tanpa jejak per-mahasiswa) vs `KelompokMahasiswa` yang UPDATE massal
  permanen.
- **`ais/database/model/VerifikasiKelengkapanCalonMahasiswa.java`**
  (r83468) — 171→773 baris, 100% (43 anggota). **TEMUAN PALING KRITIS
  SELURUH INISIATIF — dieskalasi sebagai `task_b82b25d2`**: dokumen
  keamanan `SECURITY_FINDING_AmbilLampiran_IDOR.md` yang SUDAH ADA di
  repo ternyata USANG — klaim gerbang `/al` butuh login TIDAK LAGI AKURAT,
  konfigurasi SEKARANG `IS_AUTHENTICATED_ANONYMOUSLY` (publik sejak
  19-08-2026). Plus 2 jalur baru yang lolos dari mitigasi manapun di level
  servlet: (a) URL statis langsung ke berkas ter-cache di webapp (melewati
  servlet sepenuhnya), (b) JSP PMB (`_tampilkan_berkas_di_sukses_login.jsp`)
  yang memuat data dari `getParameter("id")` TANPA cek login sama sekali,
  reachable anonim. Data berisiko: KTP, KK, ijazah, foto rumah pendaftar.
  Klarifikasi relasi dengan `Berkas` (b22): berdampingan sejak lahir (hbm2java
  sama persis, Apr 2010), bukan penerus — `Berkas` cabang mati, ini cabang
  yang tumbuh.
- **`ais/database/model/ChatMessage.java`** (r83467) — 171→591 baris,
  100% (33 anggota). Entity YATIM TOTAL (pola sama `MenuMobile`/`Berkas`)
  — chat sungguhan pakai entity `Pesan`, bukan ini. **TEMUAN BESAR —
  dieskalasi sebagai `task_493423ef`**: IDOR baca-apa-saja lewat endpoint
  `/Api` action `dataRinci` — hanya butuh token login APA SAJA (mahasiswa/
  siswa/penduduk), lalu `Class.forName(class_dari_klien)` + `id` sembarang
  mengembalikan graf data ENTITY APAPUN termasuk `Pesan` (chat pribadi
  orang lain) dan `Tbmuser` (ciphertext password). Plus SQL injection
  terpisah di `DaftarDataService` (parameter `where1..10` masuk mentah ke
  `Restrictions.sqlRestriction`). Jalur ZK chat asli (`ChatUsers`/`Chatter`)
  sendiri BERSIH — scoping kepemilikan ditegakkan benar di SQL.

**REKAP task eskalasi setelah batch 24 — total 9 task keamanan aktif**:
`task_1214dd58`, `task_5b47d41b`, `task_7b77e368` (b20-21, otorisasi
finansial/akademik spesifik-file), `task_b1e610b6` (b22, endpoint `/Data`
TULIS tanpa otorisasi), `task_9b7ff647` (b23, whitelist `MUST_CHECKED`
tidak lengkap), `task_44ea51dd` (b20, semantik RolePrivilage sistemik),
`task_493423ef` (b24, endpoint `/Api` BACA + SQLi `DaftarDataService`),
`task_b82b25d2` (b24, dokumen IDOR usang + kebocoran tanpa autentikasi —
**PRIORITAS TERTINGGI, data identitas pribadi bocor tanpa akun**).

Total akumulasi 24 sesi: **298 file** dari 7.401 (~4,0%).

## Batch 23 — SELESAI 100% (3 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/TunggakanMahasiswa.java`** (r83458) — 197→854
  baris, 100% (43 anggota). Fail-open instance ke-5 (gerbang H2H bank,
  `dianggapLunas` tri-state + `OR isNull` meloloskan baris ber-tunggakan
  besar). Inversi hak akses lagi: tombol hapus baris tunggakan SENGAJA
  dikomentari cek DELETE-nya (`// delete = CommonPrivilages...`) sementara
  checkbox "Lunas" tetap dijaga — kebalikan dari yang seharusnya
  diproteksi lebih ketat. Konfirmasi `task_b1e610b6` mencakup entity ini.
  Bug TRUNCATE CASCADE yang gagal ter-commit (kondisi ganda salah ketik).
- **`ais/database/model/MatakuliahBerbayar.java`** (r83453) — 194→629
  baris, 100% (45 anggota). Nama menyesatkan total: TIDAK ADA relasi ke
  `Matakuliah` maupun kolom nominal biaya — bahkan Javadoc `Matakuliah.java`
  sendiri keliru mendaftarkannya. Fitur yatim, salin-tempel dari
  `KalenderAkademik` yang tak dituntaskan. Bug data-corruption di hulu:
  kotak "Deskripsi" diisi dari `getNama()`, bukan `getDeskripsi()`.
- **`ais/database/model/MenuMobile.java`** (r83454) — 193→626 baris, 100%
  (44 anggota). Entity YATIM TOTAL — nol pembaca/penulis di seluruh
  codebase, sisa desain 2009. Menu mobile Flutter sebenarnya dari
  konfigurasi JSON kunci `menu_mobile` di tabel `konfigurasi` — cuma
  tabrakan nama. Dikonfirmasi masuk radius `task_b1e610b6` sebagai contoh
  "entity terpetakan tanpa pemilik" (risiko laten, bukan aktif).
- **`ais/database/model/LogUserActifity.java`** (r83461) — 187→662 baris,
  100% (24 method + 12 field). **Kluster kebocoran kredensial `LogLogin`
  (sesi 10) TERKONFIRMASI BERULANG**: `CommonPrivilages.buildKeterangan`
  menyalin SELURUH properti Hibernate entity sumber ke kolom log tanpa
  daftar-hitam — untuk `Tbmuser`, ciphertext DES password (bisa dibalik,
  kunci statis aplikasi) ikut tersalin setiap akun disimpan/diubah. Uji
  eksploitasi bug substring-URL sesi 18 (contoh `log_user_actifity.zul`)
  hasilnya NEGATIF — teruji langsung atas 3.857 nilai `nuiPage`, nol
  kecocokan. Ditemukan layar dashboard log tanpa `checkPrevilages()` sama
  sekali (hanya mengandalkan lapis menu) — masuk `task_44ea51dd`.
- **`ais/database/model/RekonsiliasiHostToHost.java`** (r83456) — 186→716
  baris, 100% (36 anggota). Entity yang mendasari layar rekonsiliasi H2H
  sesi 18. **TEMUAN SISTEMIK BESAR — dieskalasi sebagai task BARU
  `task_9b7ff647`**: `CommonPrivilages.doCheckSecurity()` yang dipanggil
  di RATUSAN layar sebagai "pemeriksaan keamanan" ternyata hanya benar-
  benar menegakkan pemeriksaan untuk 12 halaman hardcoded di whitelist
  `MUST_CHECKED` — semua layar lain lolos tanpa diperiksa sama sekali,
  meski tampak terjaga. Contoh konkret: layar ini bisa memicu DELETE SQL
  native finansial tapi tidak ada di whitelist. Juga: jalur tanpa
  autentikasi sama sekali via `ReconsilePembayaranHostToHost
  SyncrhonizerProcessor` (proses terjadwal tiap 6 jam, cukup taruh CSV di
  direktori konfigurasi) + `Class.forName` tanpa allow-list dari kolom
  master (`namaKelas`) — rantai ke `task_b1e610b6`.

**7 task eskalasi sekarang aktif** — bertambah `task_9b7ff647` (whitelist
`MUST_CHECKED` tidak lengkap, temuan sistemik terbesar setelah
`task_b1e610b6`). Kedua task ini saling melengkapi gambaran besar "kontrol
akses AIS jauh lebih longgar dari yang terlihat" tapi akar penyebab beda
(whitelist tak lengkap vs endpoint generik tanpa gerbang per-kelas) —
JANGAN digabung.

Total akumulasi 23 sesi: **293 file** dari 7.401 (~4,0%).

## Batch 22 — SELESAI 100% (3 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/PengajuanIzinTidakMasukPerkuliahan.java`** (r83442)
  — 162→586 baris, 100% (22 method + 10 field). **TEMUAN TERBESAR seluruh
  inisiatif sejauh ini — dieskalasi sebagai `task_b1e610b6` (task BARU,
  sengaja, bukan perluasan task lain)**: endpoint CRUD reflektif generik
  `/Data` (`ElearningApiUtil.prosesSimpan`/`simpanData`/`simpanProperty`)
  TIDAK PUNYA otorisasi per-kelas — hanya 2 kelas master e-Kantin yang
  dikunci hardcoded, SEMUA kelas lain (termasuk entity finansial/akademik)
  bebas diubah lewat POST langsung. LEBIH PARAH: flag `tanpaLogin` dibaca
  dari JSON permintaan KLIEN dan melewati pemeriksaan login sepenuhnya
  untuk beberapa aksi tulis (`simpanDataRinci`, `simpanBatchDataRinci`,
  `simpanBatchProduk`, `hapusDataRinci`) yang TIDAK masuk daftar blokir
  `aksiSqlTulis`. Radius ledakan berpotensi mencakup SEMUA entity yang
  lewat `DynamicFormGenerator`, bukan cuma 1 file — perlu enumerasi
  terpisah. Prioritas TERTINGGI untuk audit lanjutan.
- **`ais/database/model/KelompokMahasiswa.java`** (r83443) — 212→710
  baris, 100% (52 anggota). "Kelompok kebijakan" — menimpa status awal
  mahasiswa (3 slot) DAN sumber diskon biaya prioritas pertama. Efek
  samping besar: menyimpan mahasiswa ke kelompok ini bisa memicu UPDATE
  SQL massal yang memaksa SELURUH riwayat status mahasiswa jadi AKTIF.
  Unggah massal UI-only lagi (masuk lingkup `task_1214dd58`, dampak
  finansial+akademik sekaligus, retroaktif).
- **`ais/database/model/PesanRuangan.java`** (r83443) — 208→768 baris,
  100% (46 anggota). Bug double-booking NYATA: predikat `BETWEEN` di
  `checkPemakaian()` tidak menangkap kasus pemesanan lama yang
  MEMBUNGKUS pemesanan baru — bisa menimpa slot terisi. Fail-open varian
  baru: kombo TA/semester kosong → deteksi bentrok jadwal kuliah
  dimatikan total (`1!=1`). Dua jalur tulis (ZK vs API perpustakaan)
  dengan aturan validasi berbeda untuk tabel sama — cocok `task_5b47d41b`,
  direkomendasikan diperluas cakupannya. `toString()` membocorkan
  userId/nama dosen/tujuan pemesanan orang lain via dialog peringatan
  bentrok.
- **`ais/database/model/Berkas.java`** (r83443) — 202→612 baris, 100%
  (24 method + 13 field). Investigasi IDOR HASIL NEGATIF — bersih, entity
  ini terpisah total dari `LampiranLain`, tanpa jalur unduh. Ditemukan
  fitur yatim (tak dikonsumsi modul manapun) dengan layar CRUD-nya sendiri
  kemungkinan rusak total (NPE saat load + QueryException laten + grid
  misalignment) — sisa refactor tak lengkap.
- **`ais/database/model/PaketPerkuliahan.java`** (r83443) — 207→709
  baris, 100% (45 anggota). CONTOH POSITIF — gerbang `CommonPrivilages`
  lengkap di layar utama. Bug fungsional: kolom `statusSemesterPendek`
  dibaca tapi tak pernah ditulis (`setStatusSemesterPendek` nol pemanggil)
  → fitur paket semester pendek efektif MATI total.

**REKAP status task eskalasi setelah batch 22**: sekarang **7 task
keamanan aktif** kategori otorisasi (bertambah `task_b1e610b6` — endpoint
`/Data` generik, radius ledakan terluas & prioritas tertinggi sejauh ini).
`task_5b47d41b` (penjagaan tak merata antar Action/Helper) kini punya 2
kandidat perluasan: `PaketPerkuliahanHelper`/`MatakuliahKurikulumHelper`
dan `PesanRuanganAction`/`LibraryEngagementApi`.

Total akumulasi 22 sesi: **288 file** dari 7.401 (~3,9%).

## Batch 21 — SELESAI 100% (3 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika. Batch ini menyapu SELURUH keluarga entity "Pengecualian"
(dispensasi per-mahasiswa/dosen) untuk memeriksa apakah pola bypass tanpa
kontrol dari `BaypassPembayaranMahasiswa` (batch 20, `task_1214dd58`)
terulang — hasilnya: **YA, berulang kali, dengan variasi baru tiap
instance**:

- **`ais/database/model/PengecualianJadwalPengisianKRSMahasiswa.java`**
  (r83425) — 154→458 baris, 100% (34 anggota). **TEMUAN PALING SERIUS
  batch ini**: entity ini SECARA FUNGSIONAL adalah bypass syarat
  pembayaran (dipakai persis sama seperti `BaypassPembayaranMahasiswa` di
  `CommonPaymentHelper.checkStatusPembayaranMahasiswa`), tapi MENYAMAR di
  bawah nama administratif "pengecualian jadwal". Tanpa approval, tanpa
  scope, unggah massal tanpa gerbang server-side — pola `task_1214dd58`
  penuh, DIPERBERAT oleh penyamaran nama. Bukti tim tahu cara membangun
  approval (lihat poin berikutnya) tapi tidak menerapkannya di sini.
  Direkomendasikan masuk lingkup `task_1214dd58`.
- **`ais/database/model/PengecualianJadwalPenilaianDosen.java`** (r83428)
  — 315→929 baris, 100% (48 anggota). KONTRAS: entity ini justru PUNYA
  alur persetujuan matang (state machine PENGAJUAN→DISETUJU/DITOLAK +
  integrasi SOP + scope per-dosen). Tapi ditemukan pola BARU: **penjagaan
  tidak merata antar 3 Action/Helper berbeda** yang mengelola tabel sama —
  helper py CommonPrivilages+anti-self-approval lengkap, tapi 2 Action lain
  cuma `Common.getApakahAdmin()` tanpa re-cek tanggal/status di server,
  sehingga rentang izin yang SUDAH disetujui bisa diperpanjang tanpa
  persetujuan ulang. Dieskalasi sebagai **`task_5b47d41b`** (dibuat agent).
- **`ais/database/model/PengecualianKknMahasiswa.java`** (r83429) —
  125→546 baris, 100% (25 anggota). "Versi akademik" `Baypass
  PembayaranMahasiswa` — pola penuh terulang (tanpa approval/scope/gerbang
  massal). Diklarifikasi: bug syarat SKS/IPK `Kkn.java` (sesi 14) dan
  mekanisme dispensasi resmi ini adalah 2 MEKANISME TERPISAH yang
  kebetulan menghasilkan gejala sama ("mahasiswa tak memenuhi syarat tapi
  lolos") — audit insiden wajib periksa keduanya.
- **`ais/database/model/PengecualianPklMahasiswa.java`** (r83430) —
  125→604 baris, 100% (13 anggota). Pola sama + temuan BARU: **inversi hak
  akses** — pada layar YANG SAMA, checkbox terima/tolak pendaftar dijaga
  `CommonPrivilages.APPROVE`, tapi tombol "Pengecualian" (yang membebaskan
  DARI SELURUH syarat sekaligus, dampak lebih besar) TIDAK dijaga apa pun.
  Plus jejak audit kosong (tanpa `@PrePersist`, pembuat dispensasi tak
  tercatat). Dieskalasi sebagai **`task_7b77e368`** (dibuat agent).
- **`ais/database/model/PembatasanNilaiIPKUntukPengambilanKRS.java`**
  (r83431) — 211→772 baris, 100% (32 method + 14 field). BUKAN anggota
  keluarga "Pengecualian" tapi berelasi: aturan UMUM (bukan dispensasi
  individu) pembatasan SKS berdasar IP. Temuan struktural: baris
  per-mahasiswa di sini MENGGANTIKAN seluruh kebijakan umum bagi orang itu
  (bukan menambah), bisa MELONGGARKAN batas SKS tak sengaja. Nama kolom
  `batasMaksimumIPKYangBolehDiambil` menyesatkan — sebenarnya batas SKS,
  bukan IPK. Broken access control: tanpa scope fakultas/prodi + unggah
  massal UI-only + fail-open filter kosong. Direkomendasikan masuk lingkup
  `task_1214dd58` sebagai instance akademik non-finansial pertama.

**REKAP keluarga "surat sakti tanpa approval+scope" setelah batch 20-21**:
sekarang **5 entity** menunjukkan pola penuh atau sebagian (`Baypass
PembayaranMahasiswa`, `PengecualianJadwalPengisianKRSMahasiswa`,
`PengecualianKknMahasiswa`, `PengecualianPklMahasiswa`,
`PembatasanNilaiIPKUntukPengambilanKRS`), plus 1 entity yang justru jadi
CONTOH POSITIF sebagian (`PengecualianJadwalPenilaianDosen` — punya
approval tapi penjagaan tak merata). **3 task eskalasi baru** dari
investigasi ini: `task_1214dd58` (diperluas cakupannya secara konseptual),
`task_5b47d41b` (penjagaan tak merata), `task_7b77e368` (inversi hak
akses PKL). Total kini **6 task keamanan aktif** kategori broken-access-
control/otorisasi finansial-akademik dari inisiatif ini.

Total akumulasi 21 sesi: **283 file** dari 7.401 (~3,8%).

## Batch 20 — SELESAI 100% (2 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika. Batch ini SENGAJA menargetkan file bertema
keamanan/finansial untuk melanjutkan investigasi pola berulang dari batch
17-19 — strategi ini menghasilkan 3 temuan keamanan besar:

- **`ais/database/model/Gedung.java`** (r83416) — 218→840 baris, 100%
  (51 anggota, 34 file merujuk). **Konfirmasi mendalam pola "kontrol
  keamanan semu"**: fitur pembatasan kehadiran-berbasis-IP
  (`Perkuliahan.kehadiranDosenHarusDiinputDiIpYangDitentukan` dkk) punya
  checkbox UI + tersimpan ke DB, tapi TIDAK ADA satu pun jalur presensi
  yang benar-benar membandingkan IP klien — fiturnya tidak pernah
  diimplementasikan di manapun. Juga: mengosongkan IP gedung berpotensi
  menghapus permanen IP semua ruangan di bawahnya (`Ruang.getIp()` menyalin
  `""` bukan `null`).
- **`ais/database/model/BaypassPembayaranMahasiswa.java`** (r83417) —
  202→761 baris, 100% (36 anggota). **TEMUAN FINANSIAL SIGNIFIKAN —
  dieskalasi terpisah sebagai `task_1214dd58`**: mekanisme bypass syarat
  pembayaran TANPA alur persetujuan sama sekali, TANPA batasan lingkup
  operator (siapa pun ber-hak CREATE bisa membebaskan mahasiswa manapun di
  SELURUH institusi), fitur unggah massal bisa membuat ribuan baris bypass
  tanpa gerbang otorisasi server-side (hanya `setVisible()` UI), dan
  fail-open tambahan di `CommonPaymentHelper.checkBaypassStatusPembayaran
  Mahasiswa` (rentang semester salah konfigurasi → bypass otomatis untuk
  SEMUA mahasiswa).
- **`ais/database/model/UserAccess.java`** (r83419) — 220→808 baris, 100%
  (49 anggota). Temuan: tabel `_user_access` HANYA DITULIS, tidak pernah
  dibaca untuk autentikasi/otorisasi nyata (Spring Security pakai
  `UserDetailsServiceImpl` yang tak menyentuh tabel ini) — gudang
  kredensial bayangan (MD5 tanpa salt, `@Audited` permanen) tanpa fungsi,
  murni risiko laten. Flag `enabled`/`accountLocked`/dst = kontrol semu
  instance ke-3.
- **`ais/database/model/RolePrivilage.java`** (r83418) — 202→674 baris,
  100% (41 anggota). **TEMUAN SISTEMIK — dieskalasi terpisah sebagai
  `task_44ea51dd`**: jantung sistem hak akses dibaca oleh 5 jalur berbeda
  (`CommonPrivilages`, `NewUiPermission`, `GenericCrudRoutePrivilegeResolver`,
  `HakAksesApi`, `GrupPenggunaAksesApi`) dengan semantik BERBEDA-BEDA untuk
  baris kosong/duplikat/flag `Menu.aktif`. `HakAksesApi` fail-open (baris
  kosong → baca diizinkan, instance ke-4 pola fail-open, PERTAMA di API
  hak akses langsung). Perluasan bug substring-URL sesi 18: hasil resolver
  ternyata MENIMPA (bukan meng-AND) nilai dasar — bisa MENAIKKAN hak akses,
  bukan cuma bocor baca.
- **`ais/database/model/KegiatanKemahasiswaanPunyaMahasiswa.java`**
  (r83420) — 210→916 baris, 100% (20 method + 12 field). Entity penghubung
  `KegiatanKemahasiswaan`×`Mahasiswa`. `getPersetujuan()` destruktif:
  status kegiatan non-DISETUJUI menimpa `persetujuan=false` PERMANEN,
  tidak pulih meski status kegiatan dikembalikan. Persetujuan peserta
  tanpa gerbang hak akses server-side (varian kontrol-semu lagi).

**Pola keamanan — REKAP setelah 4 batch (17-20), 2 task sistemik baru
dibuat**:
1. Fail-open: sekarang **4 instance** (+`HakAksesApi.privilegeJson`,
   PERTAMA di jalur API langsung, bukan cuma filter pencarian UI).
2. Kontrol keamanan semu: sekarang **5-6 instance** (+`Gedung`/IP presensi
   dikonfirmasi tak pernah diimplementasikan, +`UserAccess` flag akun,
   +`KegiatanKemahasiswaanPunyaMahasiswa` persetujuan peserta).
3. Penjagaan terbalik: masih 1 instance (`AmbilDataMasaPerkuliahanBanbox`,
   batch 19) — belum ada instance kedua.
4. IDOR `AmbilLampiran`: relevan lagi (lampiran kegiatan kemahasiswaan).
5. **BARU — "surat sakti" finansial tanpa approval+scope**: baru 1
   instance (`BaypassPembayaranMahasiswa`) tapi dampak finansial tertinggi
   sejauh ini di seluruh inisiatif — `task_1214dd58`.

Total 2 task eskalasi BARU dari batch ini: `task_1214dd58` (bypass
pembayaran) dan `task_44ea51dd` (semantik RolePrivilage sistemik).

Total akumulasi 20 sesi: **278 file** dari 7.401 (~3,8%).

## Batch 19 — SELESAI 100% (2 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/MasaPerkuliahan.java`** (r83405) — 218→830 baris,
  100% (28 accessor + 14 field). Master rentang tanggal KBM. Komentar
  generator hbm2java DIKONFIRMASI salah salin-tempel ("JamPerkuliahan").
  **TEMUAN KEAMANAN PENTING — varian pola BARU "penjagaan terbalik"**: di
  `AmbilDataMasaPerkuliahanBanbox.java` negasi hilang pada kondisi admin
  (`&& Common.getApakahAdmin()` seharusnya `&& !...`), sehingga saat
  konfigurasi pengetatan admin-only DIAKTIFKAN, justru ADMIN yang dibatasi
  hanya-baca dan PENGGUNA BIASA yang mendapat akses tulis. Juga checkbox
  "Default" tanpa gerbang hak akses sama sekali (siapapun bisa memindahkan
  default sistem).
- **`ais/database/model/GelombangPendaftaranSidangTugasAkhir.java`**
  (r83403) — 214→756 baris, 100% (22 accessor + 14 field). Komentar
  generator DIKONFIRMASI salah ("JamPerkuliahan"). **Dikonfirmasi BEDA
  konsep** dari `JadwalSidangTugasAkhir` (pendaftaran+kuota vs
  pelaksanaan+ruang) — `Skripsi` punya 2 FK terpisah ke keduanya, bukan
  duplikat. `serialVersionUID` sama di 3 entity kembar salin-tempel
  (`JadwalSidangTugasAkhir`/`JadwalSeminarTugasAkhir`/file ini).
- **`ais/database/model/PertemuanPunyaGrupPertemuan.java`** (r83406) —
  232→695 baris, 100% (21 method + 12 field). **Mengonfirmasi PENUH**
  kesimpulan sesi 15 (`GrupPertemuan.java`): inilah entity penghubung
  sesungguhnya (`grupPertemuan`+`mahasiswa`+`pertemuan`), dengan kunci unik
  alami `kodeUnik` yang tak disebut sesi 15. `getKodeUnik()` NPE nyata bila
  `grupPertemuan` kosong. `getPertemuan()` memutasi entity `Pertemuan` lain
  saat dibaca.
- **`ais/database/model/BuktiPembayaran.java`** (r83407) — 238→862 baris,
  100% (22 method + 15 field). **Koreksi temuan sesi 18**: data
  `BuktiPembayaran` TIDAK hilang saat round-trip cicilan gagal↔sukses —
  hanya PENUNJUK dari sisi cicilan yang putus, bukti bayar jadi "yatim"
  tapi tetap ada di tabel ini. **TEMUAN KEAMANAN SERIUS**: IDOR di servlet
  `ais/action/servlet/AmbilLampiran.java` — token enkripsi bisa dilewati
  total via parameter mentah `ref`/`clazz`/`usingId`, id berurutan
  memungkinkan unduh SELURUH isi `lampiran_lain` lintas pengguna. Repo
  sudah punya `SECURITY_FINDING_AmbilLampiran_IDOR.md` (status TERBUKA)
  tapi belum mencakup jalur bukti pembayaran. Juga upload tanpa autentikasi
  di `DoUpload.java` (`tanpaLogin=true` tanpa validasi kepemilikan).
- **`ais/database/model/Jenjang.java`** (r83404) — 244→813 baris, 100%
  (34 method + 17 field, ~353 file merujuk). Temuan: satu tabel dipakai
  untuk 2 master berbeda (jenjang akademik via flag `aktif` + pendidikan
  orang tua via flag `aktifDipilih`) yang saling BOCOR karena kedua Action
  tidak konsisten menyetel flag pembedanya — baris baru dari satu layar
  otomatis muncul di combobox layar lainnya.

**Pola keamanan berulang — REKAP setelah 3 batch (17-19)**:
1. **Fail-open** (himpunan/filter kosong → `1=1`): 3+ instance
   (`PrestasiPegawaiAction`, `JenisPembayaranAction`, `GrupPertemuanAction`)
   — terkonsentrasi di modul **akunting** menurut temuan `BuktiPembayaran`.
2. **Kontrol keamanan semu** (field terlihat seperti pembatas tapi tak
   ditegakkan): 3 instance (`Ruang.ip`, `Menu.aktif`,
   `BuktiPembayaranAction` tanpa predikat kepemilikan).
3. **Penjagaan terbalik** (negasi hilang, kebalikan dari maksud) — BARU
   ditemukan batch 19, `AmbilDataMasaPerkuliahanBanbox`. Baru 1 instance,
   perlu diverifikasi apakah ini pola tersebar atau kasus tunggal.
4. **IDOR pada servlet unduh berkas** (`AmbilLampiran`) — sudah ada
   dokumen `SECURITY_FINDING_AmbilLampiran_IDOR.md` terpisah di repo,
   TERBUKA, kini terbukti relevan juga untuk modul pembayaran.

Total akumulasi 19 sesi: **273 file** dari 7.401 (~3,7%).

## Batch 18 — SELESAI 100% (2 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik, nol
perubahan logika:

- **`ais/database/model/CicilanPembayaranGagal.java`** (r83393) — 274→755
  baris, 100% (2 konstruktor + 36 method + 18 field). Baris status TIDAK
  disimpan sebagai flag — dipindah fisik antar tabel `cicilan_pembayaran`
  ↔ `cicilan_pembayaran_gagal`. Temuan serius: `getTanggal()` DESTRUKTIF
  (menge-null-kan tanggal bila nilai ≤0.01, beda dari kembarannya
  `CicilanPembayaran`), dan round-trip gagal→sukses **menghapus permanen**
  `buktiPembayaran`/`idLampiran` (bukti bayar mahasiswa hilang) plus 13
  properti lain yang tak ada di skema tabel gagal. Penghapusan baris pakai
  SQL native → lolos dari Envers (`@Audited` tapi riwayat hapus tak
  tercatat). Keamanan: tombol admin-only ("Diyatakan Gagal"/"Tidak Gagal")
  hanya `setVisible()` di UI, tanpa pengecekan peran ulang di `onClick`
  atau `CommonPrivilages` — masuk cakupan `task_c27d18e4`/`task_4180ddb8`.
- **`ais/database/model/Menu.java`** (r83394) — 267→785 baris, 100% (54
  anggota). Entity MASTER menu/navigasi + unit hak akses granular (~78 file
  merujuk). Hierarki BUKAN id/parentId biasa — `child`=kode node sendiri,
  `root`=kode induk, tanpa FK formal. **3 temuan keamanan signifikan**
  (masuk cakupan task audit-luas): (a) `GenericCrudRoutePrivilegeResolver`
  mencocokkan hak akses lewat SUBSTRING URL menu — bisa bocor lintas route;
  (b) flag `aktif=false` HANYA menyembunyikan item di UI, TIDAK mencabut
  akses di resolver hak akses — kontrol keamanan semu (pola sama dengan
  `Ruang.ip` batch 17); (c) `bukaHalamanBaru` + `setUrl()` tanpa validasi →
  potensi open-redirect yang membocorkan token SSO terenkripsi user ke host
  eksternal manapun bila operator master menu disusupi/lalai.
- **`ais/database/model/JamPerkuliahan.java`** (r83398) — 255→805 baris,
  100% (43 anggota). Entity MASTER slot jam kuliah (~30 file). Waktu
  tersimpan GANDA (kolom `TIME` + teks `"HH.mm"`) yang bisa desinkron
  permanen (`ParseException` ditelan diam-diam). `getFakultas()` destruktif
  (selalu ditimpa dari `jurusan`). Ditemukan 2 file TAMBAHAN dengan komentar
  generator hbm2java salah salin-tempel: `MasaPerkuliahan.java`,
  `GelombangPendaftaranSidangTugasAkhir.java` (kandidat baik utk batch
  selanjutnya, melengkapi `JadwalSidangTugasAkhir`/`JadwalSeminarTugasAkhir`
  dari batch 17).
- **`ais/database/model/JenisPembayaran.java`** (r83397) — 257→798 baris,
  100% (31 anggota + 12 field). Entity MASTER cara/jenis pembayaran (141
  file), jembatan ke akunting (tiap baris → 1 `Akun`). Bug: `reloadDefault()`
  bisa membuat baris "Tunai" KEDUA tanpa `akun` karena seed awal tak
  menandai `defaultPembayaran=true`. Keamanan: pola FAIL-OPEN yang SAMA
  dengan `PrestasiPegawaiAction` (batch 17) ditemukan lagi di
  `JenisPembayaranAction.initCriteria` — sekarang 2 instance pola
  "himpunan satuan kerja kosong → filter diganti `1=1`" — kemungkinan besar
  ini TEMPLATE yang disalin ke banyak Action master lain, layak jadi fokus
  utama audit `task_c27d18e4`/`task_4180ddb8`.
- **`ais/database/model/JadwalPembayaran.java`** (r83400) — 257→933 baris,
  100% (30 accessor + 17 field). Entity jendela waktu+sasaran pembayaran —
  **sumbu berbeda** dari `PengaturanPembayaranBulanan` (KAPAN/SIAPA vs
  BERAPA), tidak tumpang tindih, tanpa FK dua arah. Bug: `formatNim` tidak
  membuang tab/newline dari textbox multibaris → NIM per baris gagal cocok
  pencarian. Bug terpisah di `JadwalPembayaranAction.onSave` (dicatat di
  Javadoc kelas): cabang null-check deteksi jadwal duplikat tertukar
  (jenjang↔tahunAkademik).

**Pola FAIL-OPEN kini instance ke-2** (`PrestasiPegawaiAction` batch 17 +
`JenisPembayaranAction` batch 18) — dicurigai kuat sebagai template yang
disalin ke banyak Action master data lain. **Pola "kontrol keamanan semu"
kini instance ke-2** (`Ruang.ip` batch 17 + `Menu.aktif` batch 18). Kedua
pola berulang ini sebaiknya jadi prioritas pencarian eksplisit saat audit
`task_c27d18e4`/`task_4180ddb8` dijalankan.

Total akumulasi 18 sesi: **268 file** dari 7.401 (~3,6%).

## Batch 17 — SELESAI 100% (2 Sep 2026)

5 entity selesai didokumentasikan penuh (100% method/field), semua dikompilasi
`-implicit:none` bersih, mirror `java/` diverifikasi `cmp` byte-identik,
nol perubahan logika (dibuktikan pembandingan sumber tanpa komentar/spasi
terhadap HEAD sebelum commit):

- **`ais/database/model/NilaiHuruf.java`** (r83385) — 313→994 baris, 100%
  (1 konstruktor + 34 getter/setter + `toString()` + kait + 20 field).
  Entity MASTER konversi nilai angka↔huruf↔IPK, dipakai ±150 file lewat cache
  statis `ConstantValues.nilaiHurufs` (bukan query per pemakaian). Koreksi
  penting: `PenghargaanMahasiswa`/`PenghargaanDosen` **TIDAK** memakai
  `ConstantValues.lulusDariNilaiHuruf` seperti diduga sesi 15 — pemakai
  sebenarnya adalah `Detailperkuliahan`, `MahasiswaDapatKelompokKkn/Pkl`,
  `MahasiswaRequestTugasAkhir`, `WarnaStatusLulusUtil`.
- **`ais/database/model/Ruang.java`** (r83381) — 276→888 baris, 100%
  (40 anggota). Entity MASTER ruangan, dipakai ~33 entity/199 file. Temuan:
  field `ip`/"IP Gedung" terlihat seperti kontrol akses tapi **tidak pernah
  ditegakkan di mana pun** — kontrol keamanan semu, dicatat untuk audit-luas.
- **`ais/database/model/JadwalSidangTugasAkhir.java`** (r83382) — 284→754
  baris, 100% (23 anggota). Nama menyesatkan: bukan jadwal 1 mahasiswa,
  melainkan 1 gelombang/periode sidang (nol properti Dosen/Mahasiswa — bug
  slot-swap dosen TIDAK BERLAKU, dikonfirmasi tak ada slotnya sama sekali).
  Bug kehilangan data: agenda rinci berformat teks `||`/`<>` diurai dengan
  `StringUtils.split` (himpunan-karakter, bukan pemisah) → baris ber-nama
  kosong hilang permanen saat disimpan ulang.
- **`ais/database/model/JadwalSeminarTugasAkhir.java`** (r83384) — 281→860
  baris, 100% (34 anggota). Kembar salin-tempel `JadwalSidangTugasAkhir`
  (field/anotasi/`serialVersionUID` sama persis) tapi **pemakaiannya sangat
  asimetris**: sisi sidang dikonsumsi luas oleh `Skripsi` (tanggal+ruang
  disalin), sisi seminar nyaris tidak dikonsumsi (`MahasiswaRequestTugasAkhir`
  tak pernah baca `getMulai()`-nya). Bug format `jadwal_rinci` sama dengan
  saudaranya. Catatan proses: pesan commit r83384 sempat salah tertulis
  ("...NilaiHuruf.java") karena file pesan sementara `msg.txt` tertimpa sesi
  paralel yang berbagi scratchpad — isi diff diverifikasi benar via
  `svn log -c 83384 -v`. Pelajaran: pakai nama file pesan unik per sesi/file.
- **`ais/database/model/PrestasiPegawai.java`** (r83383) — 306→1004 baris,
  100% (81 anggota). Konsisten konsep "ajang/kejuaraan" dengan
  `PrestasiMahasiswa`/`PrestasiDosen` (bukan `Penghargaan*`/karya). Himpunan
  properti = subset sejati dari kedua saudaranya; satu-satunya beda PERILAKU
  nyata: `getTahun()` di sini murni dari jam server, tak pernah menimpa nilai
  ada, tak pernah lihat `tahunAkademik` (beda dari Dosen/Mahasiswa). Temuan
  keamanan (masuk cakupan `task_c27d18e4`/`task_4180ddb8`, tidak dibuatkan
  task baru): `PrestasiPegawaiAction` fail-open (himpunan satuan kerja kosong
  → filter diganti `1=1`, membuka semua data lintas unit) dan
  `DashboardRekapPrestasiPegawai` merakit SQL dengan konkatenasi nama master
  data (potensi SQL injection via data yang bisa disunting operator).

Total akumulasi 17 sesi: **263 file** dari 7.401 (~3,6%).

## `ais/database/model/KegiatanKemahasiswaan.java` — SELESAI 100% (2 Sep 2026)

Entity **master kegiatan kemahasiswaan** (tabel `public.kegiatan_kemahasiswaan`,
`@Audited`, `dynamicInsert/dynamicUpdate`, turunan langsung `GeneralValueObject`).
**65 anggota** (1 konstruktor + 62 getter/setter + `toString()` + kait
`@PreUpdate`) + 31 field + 4 konstanta status terdokumentasi (100%),
411 → 1447 baris. Revisi **r83369**, mirror `java/` verifikasi `cmp` identik
byte. Hanya Javadoc/komentar; **nol perubahan logika** (dibuktikan dengan
membandingkan sumber tanpa komentar/spasi terhadap HEAD r77530 — identik
persis, 9.418 byte).

**Kali ini nama class TIDAK menyesatkan** (kebalikan dari `PenghargaanDosen`/
`PenghargaanMahasiswa`/`BukuBahanAjar`). Menu `NewUiLayarLainnyaController`
baris 79 (tab `TAB_KEMAHASISWAAN`) berlabel "Kegiatan Mahasiswa" →
`/pages/master/kegiatan_kemahasiswaan.zul`; judul jendela tambah/ubah
"Tambah/Ubah Kegiatan Kemahasiswaan" (`KegiatanKemahasiswaanAction:1145`).
Yang **menyesatkan adalah nama PROPERTI vs label layar**:
`kelompokKegiatanKemahasiswaan` = **"Aspek Kegiatan"**,
`detailKelompokKegiatanKemahasiswaan` = **"Rincian Aspek Kegiatan"** / kolom
grid "Aspek Rinci". Tidak ada kata "kelompok" di layar mana pun.

**Jawaban pertanyaan sesi ini (diverifikasi dari kode, bukan tebakan):**

- vs **`Kegiatan`/`DetailKegiatan`** → **TIDAK BERHUBUNGAN SAMA SEKALI**, hanya
  berbagi kata "kegiatan". Keduanya entity **billing**: `Kegiatan` (2.125
  baris) = wadah tagihan per mahasiswa per semester (`amount`, `denda`,
  `pengurangan`, `lunas`, `amountTerhutang`, `jadwalPembayaran`,
  `JenisKegiatan`); `DetailKegiatan` (2.096 baris) = baris tagihan di dalamnya
  (`biaya`, `diskon`, `detailBiaya`, `itemBiaya`, `postingHistory`). Class ini
  (411 baris) **tidak punya satu pun properti nominal/denda/cicilan/posting**,
  tidak ada `@JoinColumn` ke `kegiatan`/`detail_kegiatan`, dan tidak ada
  properti bertipe class ini di kedua entity billing itu. Master pendukungnya
  pun berbeda total.
- **Kerabat sebenarnya** = **`KegiatanKedosenan`** (tabel `kegiatan_kedosenan`,
  338 baris) dan **`sekolah/KegiatanKesiswaan`** — tiga varian sebentuk untuk
  mahasiswa/dosen/siswa. Beda: `diajukanOleh` di sini bertipe `Mahasiswa` (di
  sisi kedosenan `Dosen`), dan hanya versi mahasiswa yang punya dua dosen
  pembina, `jenisAktfitasMahasiswa`, `feeder`, `noSk`/`tglSk`, dan `tempat`.
- **Peserta kegiatan** ada di **`KegiatanKemahasiswaanPunyaMahasiswa`**
  (`nullable=false`), bukan di `diajukanOleh` (itu hanya pengusul, dan
  opsional). Hanya DUA entity di `ais.database.model` yang menunjuk class ini:
  entity kepesertaan tsb + `FormulirKegiatan` (`nullable=true`).
  **`NilaiKegiatanKemahasiswaan` TIDAK menunjuk class ini** — ia tabel rubrik
  `DetailKelompok × Jabatan × Skala → nilai`.

**Verifikasi pola berulang (menyeluruh atas 65 anggota):**

- **Getter yang menulis field TERPETAKAN: 4** — `getKode()` (kode 5 digit
  dari id), `getTahun()` (**selalu** menimpa dari potongan pertama tahun
  akademik), `getTahunAkademik()` (isi periode berjalan bila null),
  `getJenisSemester()` (idem). Dengan `dynamicUpdate=true`, membaca keempatnya
  pada instance managed bisa meng-`flush` perubahan ke DB tanpa aksi simpan.
- **Getter relasi yang menulis balik referensi (`check()`): 11** — semua
  relasi tanpa kecuali.
- **Getter yang menutup sesi Hibernate: 0** — file tidak menyentuh `Session`/
  `HibernateUtil`/`Criteria` sama sekali, dan **tidak punya satu pun method
  query statis**. Jalur tak langsung: `check()` (buka+tutup sesi sendiri) dan
  `Common` (cache tahun akademik).
- **Getter destruktif (mengosongkan data): 0.**
- **Getter yang mengembalikan default TANPA menulis balik: 5** —
  `getStatus()` (`BELUM_DIPROSES`), `getBolehDipilih()` (`true`),
  `getJenisAktfitasMahasiswa()` (konstanta global), `getNama()` (trim),
  `getFeeder()` (kosong→null). Karena `@Id` di getter (property access),
  Hibernate tetap membaca nilai hasil getter saat dirty-check → default itu
  bisa tetap tertulis ke DB.

**Temuan/kuirk (dicatat, TIDAK diperbaiki):**

1. **`feeder` bukan satu id, melainkan dokumen JSON** peta
   `idJurusan → id_aktivitas` Feeder (`FeederExporter.aktivitasKegiatanMahasiswa`,
   `:1305-1337`) — satu kegiatan bisa berpadanan BANYAK aktivitas Neo Feeder
   karena Feeder mencatat per program studi. Menimpanya dengan skalar akan
   menghapus jejak sinkronisasi seluruh jurusan lain.
2. **Kolom FK dosen pembina salah eja di skema DB**: `dosen_pmbina1` /
   `dosen_pmbina2` ("pmbina", bukan "pembina").
3. **`nama` unik GLOBAL** (`@Column(unique=true)`, tidak per tahun akademik)
   berpadu dengan **pemotongan diam-diam 255 karakter** di `setNama()`
   (KE-FIX `DataException`) → dua nama panjang berbeda bisa bertabrakan
   constraint unik, dan kegiatan tahunan berulang hanya boleh muncul sekali.
4. **`getKode()` berputar setelah id > 99999** (ambil 5 karakter terakhir dari
   id yang di-prefiks nol) — id 7 dan 100007 sama-sama `"00007"`. Bukan
   pengenal unik.
5. **`getJenisAktfitasMahasiswa()` bisa mengembalikan object yang tidak ada di
   field** (`ConstantValues.KEGIATAN_KEMAHASISWAAN`, diisi `InitDataHelper`
   dari referensi Feeder bernama persis "Aktivitas kemahasiswaan"). Karena
   property access + `cascade=PERSIST,MERGE`, FK bawaan itu bisa ikut
   tersimpan walau pengguna tak pernah memilihnya.
6. **Kopling salah class**: `KegiatanKemahasiswaanAction` baris 891/894/898/911
   membandingkan status kegiatan dengan **`PrestasiMahasiswa.DISETUJUI`**,
   bukan `KegiatanKemahasiswaan.DISETUJUI`. Nilai stringnya kebetulan sama
   sehingga tak pernah ketahuan.
7. **Nomor baris usang** di penanda `auto-audit(empty-catch)` `getTahun()`
   (menyebut `:307`, posisi sebenarnya `:313` sebelum sesi ini).
8. **Titik buta urutan panggil**: `getTahun()` memeriksa **field**
   `tahunAkademik`, bukan `getTahunAkademik()` — memanggil getter tahun
   akademik lebih dulu memberi hasil berbeda.
9. `serialVersionUID` `2463821577548439808L` dipakai bersama **315 class** di
   paket ini (salin-tempel; tidak berbahaya, tapi bukan petunjuk kekerabatan).
10. Empat syarat tersembunyi agar kegiatan muncul di daftar pilih mahasiswa:
    `bolehDipilih ≠ false` **DAN** `kelompok.bisaDipilihMahasiswa` **DAN**
    `kelompok.aktif` **DAN** `status = Disetujui`
    (`AmbilDataKegiatanForKegiatanKemahasiswaanHelper:381`).

**Keamanan:** tidak ditemukan kerentanan baru. Kontrol akses tampak wajar:
combobox ubah status hanya dirender untuk non-mahasiswa, pejabat fakultas/prodi
dibuat baca-saja untuk kegiatan lintas unit. Catatan ringan (bukan kerentanan
di class ini): `getUrl()` tidak memvalidasi/membersihkan skema URL — penyaringan
harus dilakukan lapisan tampilan.


## `ais/database/model/PengajuanPegawai.java` — SELESAI 100% (2 Sep 2026)

Entity **pengajuan pegawai** (tabel `public.pengajuan_pegawai`, `@Audited`,
`dynamicInsert/dynamicUpdate`, turunan `DataSop` → `GeneralValueObject`).
**49 anggota** (1 konstruktor + 44 getter/setter + `toString()` + kait
`@PreUpdate` + 2 method bisnis) + 26 field + konstanta `DEFAULT_FORMULA`
terdokumentasi (100%), 481 → 1267 baris. Revisi **r83365**, mirror `java/`
verifikasi `cmp` identik byte. Hanya Javadoc/komentar; **nol perubahan logika**
(sumber tanpa komentar/spasi dibandingkan dengan HEAD r75894 — identik persis,
12.872 byte).

**Nama class menyesatkan lagi.** Menu aplikasi menyebut modul ini **"Pengajuan
Lembur & Masuk Hari Libur Pegawai"** (`MenuSnapshotData.java:361` →
`/pages/master/pengajuan_pegawai.zul`), di bawah grup "Pengajuan", bersebelahan
dengan "Pengajuan Izin & Cuti Pegawai" yang ditangani modul **berbeda**
(`CutiDanIzinAction`). Jadi izin/cuti BUKAN urusan class ini. Nama lama masih
tersisa di judul jendela ("Tambah/Ubah Pengajuan Pegawai") dan `istilah()`.

**Temuan struktural utama — dua mode persetujuan saling eksklusif:**
ada/tidaknya `disposisiSop` menentukan jalurnya. Tanpa disposisi → centang
manual atasan (`setujui`/`setujuiTanggal`/`disetujuiOleh`). Dengan disposisi →
`setujui` dan `aktif` **berhenti jadi kolom dan menjadi nilai turunan**
disposisi, dan centang manual tidak dirender. Semua modul payroll hilir
(absensi, lembur, konsumsi, rekap kehadiran) menyaring lewat SQL
`setujui = true`, jadi mereka bergantung pada kolom yang baru sinkron setelah
getter dipanggil pada instance managed.

**Verifikasi pola berulang (menyeluruh atas 49 anggota):**

- **Getter yang menulis field TERPETAKAN: 15** — `getNama()` (salin nama jenis
  bila kosong), `getTahun()`/`getBulan()` (isi periode berjalan), `getSatuanKerja()`
  (**selalu** menimpa dari `pegawai.getSatuanKerja()`), `getSatuanKerjaPengaju()`,
  `getJumlahHari()` (**selalu** hitung ulang), `getSetujui()`, `getAktif()`
  (menulis `aktif` **dan** `disposisiSop`), `getParameterTambahan()` dan
  `getParameterTambahanInds()` (normalisasi null → ""), plus 6 getter relasi
  ber-`check()`.
- **Getter relasi yang menulis balik referensi (`check()`): 6** —
  `jenisPengajuanPegawai`, `pegawai`, `satuanKerja` (cabang else),
  `disposisiSop`, `diajukanOleh`, `disetujuiOleh`. **`getSatuanKerjaPengaju()`
  TIDAK memakai `check()`** — nilai tersimpan dikembalikan apa adanya, bisa
  masih berupa proxy lazy.
- **Getter yang TIDAK menulis: 11** — `getKode()`, `getKeterangan()`,
  `getWaktu()`, `getWaktuSampai()`, `getKeteranganBanyak()`, `getIndex()`,
  `getSetujuiTanggal()`, `getId()`, dan trio jejak audit. Nilai default hanya
  dikembalikan, tidak disimpan.
- **Getter destruktif: 0.** **Getter yang menutup sesi Hibernate: 0** — file
  tidak menyentuh `Session`/`Criteria`/`HibernateUtil`. Jalur tak langsung:
  `check()` dan `LampiranLain.ambil(...)` di `populateParameterTambahan`.
- **Flag `aktif` SATU ARAH** (hanya pernah turun ke `false`; `null` dibaca
  `true`, itulah sebabnya filter layar harus `isNull OR eq(true)`) vs
  **`setujui` DUA ARAH** (menyalin status disposisi apa adanya).
- **Setter yang menolak pengosongan: 3** — `setOleh`, `setOlehId`,
  `setDisposisiSop` (sekali tertaut tidak bisa dilepas).

**Kuirk/bug yang dicatat, tidak diperbaiki:**

1. **Ternary mati di `setDisposisiSop`** — penjaga di baris pertama sudah
   memastikan argumen non-null dan ber-ID, sehingga syarat ternary selalu
   `false`; penugasan selalu memakai argumen baru.
2. **Pengurutan leksikografis di `ambilDataParameterTambahan()`** — karena
   `name5` **diisi** (nama kelompok), `CommonVO.compareTo` mengambil cabang
   perbandingan string `"namaKelompok nomorUrut"`, bukan cabang numerik yang
   dipakai `BiodataMahasiswa` (yang membiarkan `name5` kosong). Akibatnya
   nomor urut 10 muncul sebelum 2.
3. **Asimetri 7 ruas ditulis vs 5 ruas dibaca** — `populateParameterTambahan`
   menulis `idKelompok` dan `keterangan` yang tidak pernah dibaca kembali oleh
   `ambilDataParameterTambahan`.
4. **`getJumlahHari()` menghitung atas stempel waktu, bukan tanggal kalender** —
   dua stempel di hari sama beda jam sudah menghasilkan 2. Karena
   `getWaktuSampai()` bawaannya "besok", pengajuan baru yang belum disunting pun
   sudah terbaca 2 hari.
5. **Ejaan field `disetujiOleh`** (kurang huruf "u") vs properti/method
   `disetujuiOleh` vs kolom `disetuji_oleh` — tiga ejaan berbeda, sengaja
   dibiarkan.
6. **`toString()` memakai field `id`/`nama` langsung, bukan getter** — bagian
   nama bisa tampil `null` selama `getNama()` belum pernah dipanggil.
7. **`DEFAULT_FORMULA` `public static` tanpa `final`** (pola sama seperti
   `KasBesar`/`KasKecil`, tapi di sini objek `{}` bukan array `[]`).
8. **`serialVersionUID` identik dengan `JenisPengajuanPegawai`** — sisa
   penggandaan berkas.
9. **Renderer daftar menulis ke DB** — backfill `kode` lewat `Common.refreshUpdate`
   dan `NomorSurat.tambahIndexNomorSurat`, jadi sekadar membuka layar daftar
   dapat menaikkan penghitung nomor surat (di `PengajuanPegawaiAction`, bukan
   di entity).

**Catatan otorisasi (bukan IDOR klasik, tapi perlu ditindaklanjuti terpisah):**
`PengajuanPegawaiAction.java:221-223` membaca `execution.getParameter("jenis")`
mentah dari query-string tanpa mencocokkannya ke `jenisPengajuanPegawai
.getJenisPengguna()`/`getUsernamePengguna()`, padahal pengecekan hak-akses per
jenis itu ADA dan dipakai untuk menyusun filter listing (`:1284-1297`). Di jalur
tulis, `form(...)` memaksa jenis tersebut (`:790-792`, `:952-954`), kombonya
di-disable (`:966`), dan `onSave` menerimanya (`:1143-1145`). Sisi persetujuan
justru terjaga (guard atasan di `:531-552`), dan field pegawai dikunci ke
pegawai pengguna login (`:700-706`), jadi tidak ada eskalasi ke data orang lain.
Terkait: flag `Tbmrole.getMengajukanPengajuanPegawaiLain()` dihormati
`CutiDanIzinAction:819` tapi **tidak pernah dibaca** `PengajuanPegawaiAction`.

## `ais/database/model/FormatNilai.java` — SELESAI 100% (2 Sep 2026)

Entity **komponen (butir) penilaian perkuliahan reguler** (tabel
`public.formatnilai`, `@Audited`, `dynamicInsert/dynamicUpdate`, turunan langsung
`GeneralValueObject`). **38 anggota** (2 konstruktor + 36 method) + 16 field
terdokumentasi (100%), 446 → 1121 baris. Revisi **r83362** (tersapu ke revisi
gabungan sesi paralel, pesan kosong, 4 berkas — isi diverifikasi lewat
`svn diff -c 83362`), mirror `java/` verifikasi `cmp` identik byte. Hanya
Javadoc/komentar; **nol perubahan logika** (dibuktikan: seluruh 678 baris
tambahan lolos saringan "hanya baris komentar", nol baris kode ditambah/diubah;
hanya 2 baris lama dihapus, keduanya isi Javadoc kosong/generik yang digantikan).

**Peran:** kalau `PembombotanNilai` adalah *cetakan* ("Absensi 10%, Tugas 20%,
UTS 30%, UAS 40%"), kelas ini adalah *hasil cetakannya* — baris nyata per
`Perkuliahan`. Isinya pada dasarnya empat hal: milik kelas mana, komponen apa
(`StatusPertemuan`), berapa persen bobotnya, dan apa namanya di layar.

**Dua mode:** *konvensional* (id `StatusPertemuan` **dipatok keras**: 1 Absen,
2 Form/Tugas, 3 UTS, 4 UAS, 21–25 Tugas 1–5, 31–35 Quiz 1–5; nama diambil dari
label institusi di `PembombotanNilai`) vs *OBE* (penanda: `capaianPembelajaranLulusan`
terisi **atau** `kodeSubCpmk` tidak kosong; nama diturunkan dari formula JSON CPL,
ambang lulus lewat `ambilMinimal()`).

**JAWABAN VERIFIKASI BUG SLOT-SWAP DOSEN 1/2 — TIDAK ADA di berkas ini**, dan
buktinya struktural, bukan sekadar "kelihatannya tidak ada":

1. **Nol kemunculan** kata `dosen`/`penguji`/`pembimbing`/`ketua`/`prosentasi`
   di seluruh 446 baris asli, termasuk komentar (`grep -i` exit 1). Tidak ada
   slot dosen untuk ditukar.
2. Bobot di sini disimpan sebagai **satu skalar `persen` per baris**, bukan
   deretan kolom `nilai_ketua_sidang`/`nilai_pembimbing`/`nilai_pengujiN` yang
   sejajar deretan kolom label peran. Bentuk "N kolom bobot sejajar N kolom
   label" — satu-satunya bentuk yang bisa tergeser — sama sekali tidak ada.
3. Di hulu pun tidak ada jalurnya: `PembombotanNilai.setDefaultPembobotan(...)`
   **tidak pernah** menerbitkan komponen `dosen1..dosen5` miliknya menjadi
   `FormatNilai` (sudah tercatat sebagai "kesenjangan" di Javadoc method itu).
   Bobot per dosen penguji dibaca langsung dari entity pembobotan oleh modul TA/
   KKN/PKL.

Jadi pola bug `FormatNilaiSkripsi` **tidak** menyeberang ke sisi perkuliahan
reguler — sejalan dengan `FormatNilaiProposalSkripsi` yang juga bersih.

**Verifikasi pola berulang (menyeluruh atas 38 anggota):**

- **Getter yang menulis field TERPETAKAN: 4** — `getPersen()` (`null` → `0.0`,
  perlu karena kolomnya `NOT NULL`), `getNama()` (menghitung ulang & menimpa nama
  tiap kali dibaca, kecuali nama OBE buatan pengguna), `getNomorUrut()` (menimpa
  dari JSON `PembombotanNilai.getNomorUrutFormat()`, **hanya untuk kelas
  non-OBE**), `getJenisEvaluasi()` (menebak dari teks nama, lalu jatuh ke
  `ConstantValues.Tugas`). Dengan `dynamicUpdate=true`, membaca keempatnya pada
  instance managed bisa meng-`flush` `UPDATE` tanpa aksi simpan pengguna.
  `getJenisEvaluasi()` paling berbahaya: ia memanggil `getNama()`, jadi satu
  pembacaan bisa memutakhirkan **dua** kolom sekaligus.
- **Getter relasi yang menulis balik referensi (`check()`): 5** — semua relasi
  (`perkuliahan`, `statusPertemuan`, `kunci`, `jenisEvaluasi`,
  `capaianPembelajaranLulusan`).
- **Getter yang menutup sesi Hibernate: 0** — berkas tidak menyentuh `Session`/
  `HibernateUtil`/`Criteria` sama sekali (`grep` exit 1). Jalur tak langsung
  hanya lewat `check()`.
- **Getter destruktif: 0.** Yang mendekati adalah `getNama()`, tapi ia hanya
  menimpa nama *turunan*; nama OBE ketikan pengguna sengaja dipertahankan
  (dijaga oleh `hanyaAngka(String)`).

**Kuirk & cacat yang dicatat apa adanya (tidak diperbaiki):**

- **Ketidakkonsistenan atribut JSON di dalam satu berkas.** `ambilMinimal()`
  mencocokkan `kodeSubCpmk` ke atribut **`kode`** (baris 419/435), sedangkan
  `ambilNamaObeDariFormula()` mencocokkan `kodeSubCpmk` ke atribut **`key`**
  (baris 238–239). Kunci yang sama dipetakan ke dua atribut JSON berbeda.
- **`ambilMinimal()` bisa memakai nilai elemen terakhir.** Variabel `minimal`
  **dinolkan lalu diisi ulang di setiap iterasi**; bila tidak ada elemen yang
  cocok, yang tersisa adalah `minimal` milik elemen **terakhir** array — bukan
  nilai dasar dari `KurikulumPunyaMatakuliah.getMinimalKetercapaian()`.
- **`ambilNama(...)` memanggil `perkuliahan.getPembombotanNilai().getXxxLabel()`
  tanpa cek null.** Praktis aman (getter itu punya nilai baku berlapis) tapi
  pemanggil langsung dari luar (`EksporNilaiFeeder`) tetap menanggung risiko NPE.
- **`compareTo()` mengembalikan `0` saat gagal** — pengurutan bermasalah tampak
  sebagai "semua sama", tanpa pesan ke pengguna.
- **Pemotongan 255 karakter diam-diam** pada `nama` dan `kodeSubCpmk`.
- **`getJenisEvaluasi()` memakai `"quis"` (tanpa spasi) dan `"kuis "` (dengan
  spasi di belakang)** — nama komponen yang persis `"Kuis"` justru tidak
  tertangkap.
- **`ambilMinimal()` memakai `perkuliahan` tanpa cek null** → komponen yatim
  melempar NPE.
- **Akhiran baris CAMPURAN di HEAD.** Berkas ini bukan CRLF murni: 90 dari 446
  baris (121–124, 178–179, 213–279, 284–286, 381–394 — semuanya sisa suntingan
  fitur OBE) berakhiran **LF saja**. Sudah begitu di HEAD sebelum sesi ini;
  sengaja **dipertahankan byte-identik** agar diff tetap bersih, seluruh baris
  baru ditulis CRLF. Perlu penyapuan normalisasi tersendiri kalau mau dirapikan.
- **Di luar berkas ini** (dicatat untuk penelusuran lain, bukan diperbaiki):
  `ais/action/master/feeder/integrator/ekspor/EksporNilaiFeeder.java` sekitar
  baris 200–210 mengurai `detailNilai` menjadi variabel bernama `idFormatNilai`,
  lalu memakai nilai itu untuk mencari **`StatusPertemuan`** berdasarkan id yang
  sama. Kalau `detailNilai` benar-benar menyimpan id `FormatNilai`, ini
  pencampuran ruang-id antar dua tabel. Perlu diverifikasi terpisah.

## `ais/database/model/PengaturanPembayaranBulanan.java` — SELESAI 100% (2 Sep 2026)

Entity **satu baris rencana pembayaran bulanan** (tabel
`public.pengaturan_pembayaran_bulanan`, `@Audited` dengan tabel bayangan
`new_audit.pengaturan_pembayaran_bulanan__audit`, `dynamicInsert/dynamicUpdate`,
turunan langsung `GeneralValueObject`). **71 anggota** (1 konstruktor + 50
method + 20 field) terdokumentasi 100%, 431 → 1341 baris. Revisi **r83362**,
mirror `java/` verifikasi `cmp` identik byte. Hanya Javadoc/komentar; **nol
perubahan logika** (sumber tanpa komentar/spasi identik persis dengan HEAD
r82931, 9.816 byte).

**Judul Javadoc lama salah total**: tertulis *"Bank generated by hbm2java"* —
sisa salin-tempel generator, tidak ada hubungannya dengan entity `Bank`.

**Kedudukan**: `SettingBiaya` → `DetailBiaya` → **`PengaturanPembayaranBulanan`**
→ `DetailKegiatan`/`CicilanPembayaran`. `DetailBiaya` menyatakan total satu item
biaya; class ini memecahnya jadi N baris bulanan (kunci logis: `detailBiaya` +
`bulan`), lalu tiap baris menurunkan satu baris tagihan `DetailKegiatan` dan
dilunasi `CicilanPembayaran`.

**Empat penomoran bulan yang mudah tertukar** (didokumentasikan eksplisit):
`bulan` (angsuran ke-N dalam semester, yang disimpan pengguna), `realBulan`
(bulan kalender 1..12 hasil `hitungRealBulan`), `realBulanTahun` (`YYYYMM`),
`namaBulan` (label; "Januari" *atau* "Angsuran n" tergantung
`ItemBiaya.getMenggunakanIstilahBayarAngsuran()`, dengan asumsi keras 6 angsuran
per semester).

**Temuan terpenting — kolom turunan ternyata KOLOM SUNGGUHAN.** Entity ini
memakai *property access* (`@Id` di getter), jadi setiap getter tanpa
`@Transient` adalah properti persisten walau tanpa `@Column`. Diverifikasi dari
daftar kolom nyata pada INSERT pemulihan di
`ais/common/CicilanPembayaranRecoveryHelper.java` (~baris 1041): tabel benar-benar
punya kolom `nama`, `namabulan`, `realbulan`, `realbulantahun`. Padahal keempat
getternya menghitung ulang **dan menugaskan kembali ke field** setiap dipanggil —
termasuk saat Hibernate sendiri memanggilnya untuk dirty-check. Hasilnya
`UPDATE` + revisi Envers palsu dari operasi yang secara logika cuma membaca.
Penawarnya sudah ada di repo dan kini ditautkan dari Javadoc:
`KegiatanHelper.tandaiPengaturanBulananReadOnly(...)` /
`lindungiKonfigurasiBulananSaatHitungUlang(...)` memanggil
`session.setReadOnly(ppb, true)`.

**Verifikasi pola berulang (menyeluruh atas 51 anggota, bukan asumsi dari file
lain):**

- **Getter menulis balik SELALU ke kolom terpetakan: 4** — `getNama()`,
  `getNamaBulan()`, `getRealBulan()`, `getRealBulanTahun()`.
- **Getter menulis balik hanya saat field `null`: 5** — `getPersentase()` (0.0),
  `getNominal()` (0.0), `getBulan()` (1), `getAktif()` (**true**),
  `getDikalikanDenganKondisiKhusus()` (false).
- **Getter yang sengaja TIDAK menulis balik: 2** — `getTetapDitampilkanWalaupunNol()`
  (bawaan **false**) dan `getTanggalTagihanSelaluDibuatAwalBulan()` (bawaan
  **true**, kebalikan tetangganya). Asimetri ini menguntungkan: pembacaan tidak
  mengotori dirty-check. Jangan diseragamkan.
- **Getter menulis balik ke field `@Transient`: 1** — `getInfoDenda()` (aman
  terhadap DB; tapi salinan lama tidak pernah dibersihkan).
- **Getter yang membuka/menutup `Session`: 0.** File tidak menyentuh `Session`/
  `HibernateUtil`/`Criteria`/`Query` sama sekali. Jalur DB tak langsung hanya
  `hitungTahap(...)` (via `Common.poulateTahapan`) dan `ambilNominalModifikasi(...)`
  (via `PembayaranNominalModifikasiHelper`).
- **Getter destruktif: 0.**
- **Getter relasi memakai `check()`: 0** — `getDetailBiaya()` mengembalikan field
  mentah. Aman di sini karena `@ManyToOne` tanpa `fetch=LAZY` (bawaan JPA EAGER)
  + `@Fetch(FetchMode.SELECT)`, jadi tidak ada proxy lazy. **Berbeda dari
  mayoritas entity AIS** — jangan menambah `check()` di sini.

**Flag `aktif`: DUA ARAH** (diverifikasi dari kode file ini + pemanggilnya).
`setAktif(Boolean)` polos tanpa penjaga apa pun; keempat pemanggilnya di
`NewDetailBiayaExcelAction` meneruskan `aktifRencana.isChecked()` langsung dari
checkbox, jadi bebas bolak-balik. Satu-satunya asimetri ada di getter: `null`
dibaca sebagai **true** (baris warisan dianggap aktif). Ini variasi lagi
dibanding modul akunting sesi 12 — asumsi lintas-file memang tidak boleh dipakai.

**Kuirk/bug dicatat, sengaja TIDAK diperbaiki:**

- `getNama()` menimpa kolom `nama` dengan `toString()`, dan `toString()` membaca
  **field mentah** `realBulan` (bukan getter). Isi kolom `nama` karena itu
  bergantung urutan pemanggilan getter dan bisa tersimpan `"12-null-500000.0-..."`.
  `toString()` juga memanggil `getDetailBiaya()` tanpa memakai hasilnya (sisa pola
  resolusi proxy yang tak berlaku di sini) dan merangkai `DetailBiaya.toString()`
  yang mahal.
- `hitungPersentase()`/`hitungNominal()` bernama seperti fungsi murni padahal
  **mengubah state** (menugaskan ke field terpetakan); pemanggil di
  `NewDetailBiayaExcelAction` malah memakai pola `setPersentase(hitungPersentase())`.
  `hitungPersentase()` masih menyisakan `System.out.println` debug di jalur produksi.
- `getRealBulan()` melipat hasil >12 dengan `% 12`, sehingga kelipatan 12
  (mis. 24) menjadi **0**, bukan 12 → `getNamaBulan()` mengembalikan string
  kosong. Tak tercapai pada konfigurasi bawaan (`pembayaranSemesterGanjilMulaiDiBulan=7`,
  `pembayaranSemesterGenapMulaiDiBulan=1`, maks 6 angsuran/semester).
- Kolom `denda` dan `filelocation` **ada di tabel tetapi tidak dipetakan** class
  ini — kolom yatim sisa versi lama.
- `compareTo()` hanya membandingkan `bulan`, mengembalikan `0` untuk tipe lain
  (tidak konsisten dengan `equals()` berbasis `id`), dan masih menyimpan komentar
  `// TODO Auto-generated method stub` bawaan Eclipse.
- `setOleh()`/`setOlehId()` **mengabaikan `null`/kosong diam-diam** → jejak audit
  tidak bisa dikosongkan kembali.
- `getKeterangan()` me-`trim()` nilai kembalian tanpa menulis balik, sehingga pola
  baca-ubah-simpan diam-diam memangkas spasi tepi.
- `ambilNominalModifikasi()` terlihat murni, tetapi helper yang dipanggilnya
  **menulis `setKeterangan(...)`** (kolom terpetakan) pada jalur berbasis SKS →
  satu lagi sumber `UPDATE` tak terduga.

**Keamanan/privasi/akses:** tidak ditemukan kerentanan pada file ini (nol SQL
mentah, nol kredensial, nol pemeriksaan hak akses yang bisa dilewati).


## `ais/database/model/TugasPertemuan.java` — SELESAI 100% (2 Sep 2026)

Entity **tugas mandiri/perorangan** pada satu pertemuan e-Learning (tabel
`public.tugas_pertemuan`, `@Audited`, `dynamicInsert/dynamicUpdate`). **56 anggota**
(1 konstruktor + 55 getter/setter/kait/utilitas) + 25 field + konstanta
`DEFAULT_FORMULA` terdokumentasi (100%), 438 → 1246 baris. Revisi **r83361**, mirror
`java/` verifikasi `cmp` identik byte. Hanya Javadoc/komentar; **nol perubahan
logika** (sumber tanpa komentar/spasi dibandingkan terhadap HEAD — identik persis,
9.434 byte). Berkas sekalian dinormalkan ke CRLF murni (sebelumnya 6 baris ber-EOL
LF saja: 211–215 dan 246).

**Bukan turunan langsung `GeneralValueObject`** — `extends Tugas`, kelas abstrak
"sesuatu yang bisa dikumpulkan berkas oleh peserta" yang memiliki seluruh mesin
berkas jawaban (`TugasFileContent`, indeks JSON `tugas_file_content_<id>`). Turunan
konkret `Tugas` hanya TIGA: `TugasPertemuan` (individu), `TugasKelompok`
(berkelompok, sudah didokumentasikan sesi lain — **nyaris kembar**, banyak method
salinan baris-demi-baris), dan `Pertemuan` sendiri.

**Jawaban pertanyaan sesi ini (relasi langsung atau tidak langsung?) — KEDUANYA:**

- Arah **`TugasPertemuan` → `Pertemuan` LANGSUNG**, kolom `pertemuan` `NOT NULL`,
  tapi diakses lewat **tiga jalur** atas satu kolom yang sama: `getPertemuan()`
  (`Long` mentah, satu-satunya jalur TULIS), `getPertemuanData()` (`@ManyToOne`
  `insertable=false, updatable=false` + `@NotFound(IGNORE)`, baca-saja — setternya
  hanya mengisi cache memori dan **tidak pernah tersimpan**), dan `ambilPertemuan()`
  (bukan properti persistence; lewat `GeneralValueObject.ambilData`, bisa membuka
  session sendiri). Blok `@ManyToOne getPertemuan()` versi lama masih dibiarkan
  dikomentari di file.
- Arah **balik `Pertemuan` → daftar tugas TIDAK LANGSUNG** — tidak ada
  `@OneToMany`. Daftarnya dibangun dari **indeks JSON berbasis berkas**
  (`Pertemuan.reInitTugasPertemuan(Session)` / `ambilTugasPertemuanTotal()`), jadi
  menambah/menghapus baris tanpa menyegarkan indeks membuat layar basi. Ini sejajar
  temuan `GrupPertemuan` (r83344) meski mekanismenya berbeda (di sana lewat entity
  penghubung, di sini lewat indeks berkas).

**Verifikasi pola berulang (menyeluruh atas 56 anggota, dari kode file ini sendiri):**

- **Getter yang menulis field TERPETAKAN: 3** — `getMhsYgTidakIkut()`,
  `getMhsBolehUploadUlang()` (normalisasi CSV berbungkus koma, ditulis balik ke
  field), dan `getAktif()` (`aktif = !getJudultugas().isEmpty()`). Dengan
  `dynamicUpdate=true`, **sekadar membaca** ketiganya pada instance managed bisa
  memicu `UPDATE` saat flush tanpa aksi simpan pengguna.
- **Getter relasi yang menulis balik referensi (`check()`): 4 dari 5** —
  `getSyaratMengumpulkanTugas()`, `getJenisItemPenilaianSiswa()`,
  `getGrupKategoriItemPenilaianSiswa()`, `getGrupPenilaian()`. **`getFormatNilai()`
  satu-satunya relasi yang TIDAK memakai `check()`** (anomali; dicatat, tidak
  diperbaiki).
- **Getter yang menutup sesi Hibernate: 0** — file tidak menyentuh
  `Session`/`HibernateUtil`/`Criteria` sama sekali. Jalur tak langsung hanya
  `check()` dan `ambilData()` (dipakai `ambilPertemuan()`), keduanya mengurus
  session sendiri di `finally`.
- **Getter destruktif: 0.**

**Temuan/kuirk lain (dicatat apa adanya, TIDAK diperbaiki):**

- **Enam kolom `text` berisi JSON** yang mudah tertukar; bentuk kuncinya
  diverifikasi dari pemanggil, bukan ditebak: `formatNilais` (kunci = id
  `FormatNilai`, sekaligus **saklar mode OBE** lewat perbandingan dengan
  `Tugas.JSON`), `keterangan_nilai_baru`/`keterangannilai` (kunci
  `<id>_mhs|_siswa|_cal_mhs|_cal_siswa` + akhiran `_nilai`, `_nilai_<idFormatNilai>`,
  `_ket`), `nilai_manual_json` (bersarang: `{"<idMhs>":{"fn_<idFN>":n,
  "fn_<idFN>_ket":s,"paksa":b}}`), `sub_cpmk_per_peserta`
  (`{"<idMhs>":["<idFN>",…]}`, id sebagai STRING), dan `syaratakses`.
- **Nilai per peserta tidak bisa di-query SQL** dan dua penilai yang menyimpan
  bersamaan saling menimpa nilai SELURUH peserta. `TugasMandiriHelper` memanggil
  `session.refresh(tp)` tepat sebelum tiap penulisan JSON — mitigasi, bukan
  penyelesaian.
- **Kontrak `null` berbeda dari entity kembarannya**: `getNilaiManualJson()` di sini
  menormalkan kosong menjadi `"{}"`, sedangkan `PertemuanPunyaUjian` yang bernama
  sama mengembalikan nilai mentah (bisa `null`). Kode yang menangani keduanya
  bersama harus tetap memeriksa `null`.
- `ambilSubCpmkPeserta()` **menelan seluruh kegagalan parsing menjadi `null`**,
  sehingga JSON rusak tidak bisa dibedakan dari "peserta mengerjakan semua Sub-CPMK".
- Normalisasi CSV memakai `replaceAll(",,", ",")` **tiga kali berurutan**, jadi
  runtutan koma yang sangat panjang bisa tidak habis; pemeriksaan `== null` pada
  baris `return` kedua getter CSV sudah mati (field dipastikan non-null di atasnya).
- Nilai bawaan tersembunyi di getter: `getProsentase()` → `100.0` bila kosong (bukan
  0), `getJudultugas()` di-`trim` sementara `toString()` membaca field mentah.
- `DEFAULT_FORMULA` **`public static` tanpa `final`** (pola yang sama seperti
  `Tugas.JSON`) — konstanta pembanding secara teknis bisa diubah saat runtime.
- `setOleh()`/`setOlehId()` **menolak nilai kosong diam-diam** — jejak audit tidak
  pernah bisa dibersihkan lewat setter.
- Hanya ada kait `@PreUpdate`; tidak ada `@PrePersist`, jadi pengisian audit saat
  `INSERT` bergantung pada pemanggil.
- Import `ais.ui.util.WaktuUtil` tidak terpakai (kode memakai nama berkualifikasi
  penuh). Dibiarkan.

**Tidak ditemukan kerentanan keamanan/privasi/broken access control** di file ini —
entity murni state, tanpa SQL, tanpa I/O, tanpa pemeriksaan otorisasi.

## `ais/database/model/PenghargaanMahasiswa.java` — SELESAI 100% (2 Sep 2026)

Entity **karya mahasiswa** (tabel `public.penghargaan_mahasiswa`, `@Audited`,
`dynamicInsert/dynamicUpdate`, turunan langsung `GeneralValueObject`).
**57 anggota** (1 konstruktor + 56 getter/setter/kait) + 26 field + 4 konstanta
status terdokumentasi (100%), 352 → 1414 baris. Revisi **r83350**, mirror `java/`
verifikasi `cmp` identik byte. Hanya Javadoc/komentar; **nol perubahan logika**
(dibuktikan dengan membandingkan sumber tanpa komentar/spasi terhadap HEAD
r75894 — identik persis, 7.283 byte).

**Nama class menyesatkan lagi** — pola KETIGA setelah `PenghargaanDosen` dan
`BukuBahanAjar`. Modul UI-nya bernama **"Karya Mahasiswa"**, bukan "Penghargaan
Mahasiswa". Bukti berlapis: menu `NewUiLayarLainnyaController` baris 82/101
(entri "Karya Mahasiswa"/"Karya" → `/pages/master/penghargaan_mahasiswa.zul`),
label form `PenghargaanMahasiswaAction` ("Nama Karya *", "Tanggal Pendaftaran
Karya *", "Nomor Sertifikat Karya *", "Bentuk Karya *"), koleksi DSpace bernama
"Karya Mahasiswa", judul blok indeks `InitIndex`
(`--- Karya/Penghargaan Mahasiswa ---`), tab ZUL "Karya Mahasiswa",
`ProfileUiHelper` kartu "Karya", modul JSP baru "Manajemen Karya". Sisa nama
lama: judul jendela tambah/ubah masih "Tambah/Ubah Penghargaan Mahasiswa", tab
master masih "Bentuk Penghargaan", dan `ProfileMahasiswa` memakai **dua judul
berbeda untuk layar yang sama**.

**Jawaban pertanyaan sesi ini (diverifikasi dari kode, bukan tebakan):**

- vs **`PrestasiMahasiswa`** → **KONSEP BERBEDA, bukan duplikat.** Karya/HKI vs
  ajang/kejuaraan. Tabel, `.zul`, action, dan entri menu semuanya terpisah
  (`NewUiLayarLainnyaController` baris 81 "Prestasi Mahasiswa" vs 82 "Karya
  Mahasiswa"). Himpunan propertinya rapi: **57 anggota di sini ≈ 73 anggota
  `PrestasiMahasiswa` dikurangi properti khas ajang** (`tempat`,
  `penyelenggara`, `juara`, `peringkat`, `jumlahPeserta`, `prestasiLuarKampus`,
  `cabangPrestasiMahasiswa`, `kategoriPrestasiMahasiswa`, `feederPrestasi`)
  **ditambah** `kategoriPenghargaan`. Sisanya (`noSk`, `tglSk`, `alamat`, dua
  dosen pembina, `feeder`, trio periode) **ada di kedua class dengan bentuk
  sama persis** — tumpang tindih bentuk itu nyata dan dalam, tapi bukan
  duplikasi konsep.
- vs **`PenghargaanDosen`** → **KONSEP SAMA, beda pemilik.** Class ini adalah
  sisi mahasiswa dari modul "Karya". Bukti terkuat: **master
  `KategoriPenghargaan` dipakai bersama**, dan `PenghargaanMahasiswaAction`
  maupun `PenghargaanDosenAction` **sama-sama menyemai baris bawaan identik**
  ("Paten", "HaKI", "Nasional / Internasional") bila tabel master kosong —
  layar mana pun yang dibuka lebih dulu, itulah yang menyemai. Beda: class ini
  lebih kaya (57 vs 43) dengan dosen pembina I/II, `jenisAktfitasMahasiswa`,
  dan **integrasi Neo Feeder yang tidak dimiliki `PenghargaanDosen` sama
  sekali**; juga kebalikan pola `check()` — 7 getter relasi di sini memakai
  `check()`, di `PenghargaanDosen` tidak satu pun.

**Verifikasi pola berulang (menyeluruh atas 57 anggota):**

- **Getter yang menulis field TERPETAKAN: 3** — `getTahunAkademik()` (isi
  periode berjalan bila null), `getJenisSemester()` (idem), `getTahun()`
  (**selalu** menimpa dari potongan pertama tahun akademik). Dengan
  `dynamicUpdate=true`, membaca ketiganya pada instance managed bisa
  meng-`flush` perubahan ke DB tanpa aksi simpan pengguna.
- **Getter relasi yang menulis balik referensi (`check()`): 7** — semua relasi.
- **Getter yang menutup sesi Hibernate: 0** — file tidak menyentuh `Session`/
  `HibernateUtil`/`Criteria` sama sekali. Jalur tak langsung: `check()` (buka+
  tutup sesi sendiri) dan `Common` (cache tahun akademik).
- **Getter penormal tanpa tulis balik: 6** — `getNama()` (trim), `getCapaian()`/
  `getUrl()` (null→""), `getStatus()` (null→BELUM_DIPROSES), `getFeeder()`
  (kosong→null), `getJenisAktfitasMahasiswa()` (fallback konstanta).
- **Setter yang mengabaikan input diam-diam: 2** — `setOleh`/`setOlehId`.

**Kuirk/bug yang dicatat (tidak diperbaiki):**

1. **Kolom `tahun` tidak pernah ditulis form.** Isian "Tahun *" `readonly`, dan
   `onSave(...)` tidak pernah memanggil `setTahun(...)`. Satu-satunya penulis
   kolom itu adalah `getTahun()` sendiri.
2. **Fallback `getJenisAktfitasMahasiswa()` bisa menempel permanen.** Form ubah
   memanggil `selectComboItem(..., getJenisAktfitasMahasiswa())` sehingga baris
   ber-kolom `NULL` tampil sudah terpilih "Program kreativitas mahasiswa" (PKM);
   sekali disimpan, `NULL` berubah jadi PKM walau pengguna tak menyentuhnya.
   Catatan: `PrestasiMahasiswa` memakai fallback `KOMPETENSI` ("Kompetisi") —
   dua modul kembar, dua jenis aktivitas PDDikti berbeda.
3. **Pesan validasi salah kata**: seluruh pesan gagal-simpan berbunyi
   "Kejuaraan" ("Nomor sertifikat kejuaraan", "Kategori Kejuaraan", "Capaian
   kejuaraan") padahal label formnya "Karya". Gejala sama persis dengan
   `PenghargaanDosenAction`.
4. **Isian filter mati**: `.zul` masih punya `searchpenyelenggara`
   ("Penyelenggara") padahal entity tak punya kolom itu dan `initCriteria(...)`
   tak pernah membacanya.
5. **Konstanta status kembar-tapi-terpisah** dengan `PrestasiMahasiswa` (string
   identik, konstanta berbeda). `DasboardAktivitasMahasiswa` sengaja memakai
   `PrestasiMahasiswa.BELUM_DIPROSES` untuk menghitung **semua** tipe termasuk
   karya — kesamaan teks itu jadi kontrak tak tertulis.
6. **UI baru menembak SQL langsung** dengan status hardcode
   (`p.status='Disetujui'`), jadi mengubah konstanta di entity tak ikut
   mengubah JSP.
7. `serialVersionUID` `2463821577548439808L` dipakai bersama
   `KategoriPenghargaan`, `PenghargaanDosen`, `PrestasiMahasiswa`,
   `PrestasiDosen`.
8. **Marker `auto-audit(empty-catch)` di `getTahun()` menyebut baris `:264`**
   yang kini bergeser karena penambahan Javadoc — dibiarkan apa adanya (string
   literal = kode; mengubahnya bukan perubahan Javadoc).
9. `Mahasiswa.removePenghargaanMahasiswa` hanya mengosongkan *nilai* kunci
   (bukan menghapus kuncinya) → berkas indeks JSON membesar monoton.

**Paparan keluar yang perlu disadari:** baris `DISETUJUI` bisa terbaca **tanpa
login** — `WEB-INF/baru/website.jsp` memanggil
`listApproved(hs, "ais.database.model.PenghargaanMahasiswa", 6)` untuk feed
publik, dan ekspor DSpace mempublikasikan judul + capaian + sertifikat ke
repositori. Tombol "Setujui Semua"
(`aktifkan_tombol_setujui_semua_karya_mahasiswa`) menyetujui borongan semua
baris hasil filter yang bukan `DITOLAK`.

**ESKALASI KEAMANAN — broken access control KEEMPAT** (pola sama dengan
`PenghargaanDosenAction` dan `BukuBahanAjarAction`, masuk cakupan audit
`task_c27d18e4`). Di `ais/action/master/PenghargaanMahasiswaAction.java`:

- baris 261-267: parameter URL `mahasiswa=<id>` **menimpa**
  `tbmuser.getMahasiswa()` tanpa membandingkan id-nya. `mhs` inilah satu-satunya
  penyempit data (baris 297-301 → `initCriteria()` 1609-1610), jadi mahasiswa A
  yang membuka `penghargaan_mahasiswa.zul?mahasiswa=<idB>` melihat **seluruh
  karya mahasiswa B**. Bentuk URL-nya mudah ditebak karena memang dibangun sah
  oleh `ProfileMahasiswa:2080` dan `DashboardKegiatanKemahasiswaan:166`.
- baris 947-948: tombol Ubah/Hapus hanya dijaga status + login
  (`!DISETUJUI && tbmuser != null`), **bukan kepemilikan** → penyamar tetap
  dapat mengubah/menghapus baris yang belum disetujui. Ini **lebih longgar**
  dari `PenghargaanDosenAction`.
- baris 269-272 + 1652-1658: parameter `penghargaan=<id>` dimuat langsung
  (`GeneralValueObject.ambilData`) tanpa cek pemilik, lalu **disisipkan paksa ke
  awal daftar di luar `Criteria`** → IDOR baca murni: id apa pun dirender
  lengkap (nama karya, capaian, no. sertifikat, nama+NIM pemilik, pembina,
  lampiran) walau `mahasiswa=` diisi benar.
- Kontras: UI JSP baru justru benar — `_karya_mahasiswa.jsp:903` memaksa
  `filters.push("mahasiswa = <idMhsLogin>")` dari sesi server. Celahnya
  spesifik pada jalur ZK lama.

## `ais/database/model/BukuBahanAjar.java` — SELESAI 100% (2 Sep 2026)

Entity **buku/diktat/bahan ajar karya dosen** (tabel `public.buku_bahan_ajar`,
`@Audited`, `dynamicInsert/dynamicUpdate`, turunan langsung
`GeneralValueObject`). **60 method + konstruktor + 26 field**
terdokumentasi (100%), 389 → 1161 baris. Revisi **r83342**, mirror
`java/` verifikasi `cmp` identik. Hanya Javadoc/komentar; nol perubahan
logika (dibuktikan dengan membandingkan sumber tanpa komentar/spasi
terhadap HEAD r73618 — identik persis).

**Struktur:** kepengarangan datar **5 slot dosen** (`dosenPengarang1` =
ketua/penulis utama, 2–5 = anggota) + **3 slot nama penulis luar** berupa
teks bebas (`pengarang1..3`) + `editorDanKontributor` (teks). Dua
klasifikasi lookup: `TahapanPenyusunanBuku` dan `JenisPeredaranBuku` —
**keduanya wajib tidak null** agar buku ikut dinilai BKD. Nol koleksi:
semua relasi ditarik dari sisi anak (`MatakuliahPunyaBukuBahanAjar`,
`MatapelajaranPunyaBukuBahanAjar`, `DataPunyaBukuBahanAjar`,
`FileBukuBahanAjar`, `AsesemenPenilaian`, `DspaceInformation`).
Perhatikan `FileBukuBahanAjar` menautkan lewat kolom `Long bukuBahanAjar`
berisi id mentah, **bukan** relasi Hibernate — hapus buku tidak
meng-cascade ke berkasnya.

**Delapan pintu masuk** terverifikasi: menu "Buku Bahan Ajar"
(`BukuBahanAjarAction`), menu "Cari Buku Ajar"
(`BacaBukuBahanAjarAction`), BKD bidang Pendidikan komponen "Penulis
Buku" (`AsesementAction` + `BkdPenulisHelper`), profil/biodata dosen
(`?dosen=<id>`), matakuliah & matapelajaran sekolah, berkas lampiran,
ekspor DSpace tipe `Book` + sitasi CSL, dan `DataPunyaBukuBahanAjar`.

**Hubungan dengan `PenghargaanDosen` (r83317):** mirip bentuk, **beda
konsep**. `PenghargaanDosen` = karya ber-HKI/paten, menu "Karya Dosen",
sumber SAPTO `LaporanKaryaDosen_A_7_1_5`, kepemilikan satu dosen.
`BukuBahanAjar` = penerbitan buku/diktat dengan 5 penulis berperingkat,
siklus penyusunan + jangkauan peredaran sendiri, dipakai ganda sebagai
materi ajar matakuliah. Kesamaan struktural nyata: keduanya menempel ke
pipeline BKD (`AsesemenPenilaian`/`PenilaianAsesor`) dan bisa diekspor
ke DSpace. `serialVersionUID` keduanya sama persis
(`2463821577548439808L`) karena tersalin dari template hbm2java — bukan
penanda kekerabatan.

**Verifikasi pola berulang (3 kategori getter tidak-murni):**

1. **Resolusi proxy lazy via `check()`** — kelima `getDosenPengarangN()`
   menjalankan `dosenPengarangN = check(dosenPengarangN)`. Pola standar;
   bisa membuka+menutup Session baru diam-diam dari dalam `check()`.
2. **Getter yang MENULIS BALIK dan ikut ter-UPDATE ke DB** —
   `getSemester()`, `getTahunAkademik()`, `getPengarangAdalahDosen()`.
   Ketiganya kolom terpetakan + property access ⇒ dirty-check menulisnya
   ke database pada flush, dan Envers mencatat revisi baru, **tanpa ada
   yang menyunting buku**. Sekadar menampilkan daftar buku / ekspor
   DSpace / pengisian BKD massal sudah cukup memicunya.
3. **Default sementara TANPA tulis balik** — `getTahun()`,
   `getTanggal()`, `getMasaPenugasan()`, `getPengarang1..3()`,
   `getAbstrak()`, `getLink()`, `getEditorDanKontributor()`,
   `getNama()`.

**Tidak ada** getter penghapus data di file ini (berbeda dari
`JadwalUjianPMB.getRuanganYgIkut()`), dan **tidak ada** method yang
membuka/menutup Session sendiri — penutupan session hanya terjadi di
dalam `check()`.

**Kuirk/bug yang dicatat (tidak diperbaiki):**
- `getSemester()`/`getTahunAkademik()` bergantung pada kalender
  **pengguna yang sedang login** (`Common.getCurrentUser()`), dan bila
  `tanggal` juga null acuannya jadi **hari pembacaan**. Dua pengguna
  berbeda yang membuka baris lama yang sama bisa menuliskan nilai
  berbeda, permanen (tidak pernah dihitung ulang).
- `getPengarangAdalahDosen()` **searah**: memaksa `true` selama field
  `dosenPengarang1` terisi, jadi `setPengarangAdalahDosen(false)` selalu
  ditimpa; mengosongkan slot dosen kemudian hari tidak mengembalikannya
  ke `false`.
- `toString()` membaca **field** `nama` mentah — bisa `null`, tidak
  di-`trim()`, berbeda hasil dari `getNama()`. Muncul di bilah progres
  BKD sebagai teks `"null"`.
- `getKeterangan()` di sini mengembalikan `null`, sedangkan versi induk
  `GeneralValueObject.getKeterangan()` menjamin tidak pernah `null` —
  jebakan saat kode berpindah dari tipe induk ke tipe konkret.
- `getTahapanPenyusunanBuku()`/`getJenisPeredaranBuku()` dianotasi
  `@Fetch(FetchMode.SELECT)` **tanpa** `fetch = LAZY` dan **tanpa**
  `check()` ⇒ eager, satu SELECT tambahan per baris (N+1) di layar
  daftar.
- Batas keras 5 slot dosen merembet: `Dosen.reInitBukuBahanAjar`,
  `BukuBahanAjarAction.initCriteria`, `BacaBukuBahanAjarAction` semuanya
  menyusun 5 `Restrictions.or(...)` bertingkat.
- `setNama()` tidak `trim()` dan tidak memvalidasi panjang padahal
  kolomnya `NOT NULL length 255`.
- `populateDosen()`/`populateDosenAnggota()` memakai kunci Map
  `"<idBuku>-<idDosen>"`; untuk buku yang belum tersimpan kuncinya jadi
  `"null-<idDosen>"` sehingga dua buku baru bertabrakan bila Map-nya
  digabung.

**TEMUAN KEAMANAN (instance KETIGA pola broken access control):**
`ais/action/master/BukuBahanAjarAction.java` baris 419–426 membaca
`execution.getParameter("dosen")` lalu `Restrictions.idEq(...)` **tanpa
cek kepemilikan sama sekali** — persis pola `PenghargaanDosenAction`
(`task_c27d18e4`) dan beasiswa (`task_51f767ec`). `setDisabled(true)`
pada kotak pencarian hanya menyembunyikan kontrol di UI, bukan
otorisasi. Dampak lebih ringan dari dua kasus sebelumnya (data buku
umumnya publik), tapi mengonfirmasi pola sistemik. Bonus: baris 432
`System.out.println("dosen => " + ...)` membocorkan parameter ke log.
Masuk cakupan audit `task_c27d18e4` ("audit pola serupa di action
lain") — tidak dibuatkan task baru.

## `ais/database/model/MahasiswaDapatKelompokPkl.java` — SELESAI 100% (2 Sep 2026)

Entity **keanggotaan mahasiswa dalam kelompok PKL** (tabel
`public.mahasiswa_dapat_kelompok_kelompok_pkl` — kata `kelompok` memang
tertulis DUA KALI, bukan salah ketik), `@Audited`, `dynamicInsert/
dynamicUpdate`, turunan langsung `GeneralValueObject`, implementasi
`VOPesertaPembelajaran`. **42 method + konstruktor** terdokumentasi (100%),
534 → 1378 baris. Revisi **r83345**, mirror `java/` verifikasi `cmp`
identik. Hanya Javadoc/komentar; nol perubahan logika (dibuktikan dengan
membandingkan sumber tanpa komentar/spasi terhadap r76904 — identik persis).

**Alur:** hilir modul PKL — `Pkl` (program) → `KelompokPkl` (tempat magang +
pembimbing) → **kelas ini** (anggota). TIDAK ada FK langsung ke `pkl`; kode
selalu dua hop lewat `createAlias("kelompokPkl",…)` +
`Restrictions.eq("kelompokPkl.pkl", pkl)`. Mahasiswa memilih kelompok di
`PklUntukMahasiswaAction` (baris dibuat, `diterima` belum true) → panitia
menyetujui lewat checkbox di `KelompokPklHelper` (DUA ARAH, bisa dicabut) →
penilaian di `PenilaianPklHelper` → nilai disalin ke `Detailperkuliahan`
matakuliah PKL agar masuk KHS/IPK → sertifikat + ekspor Feeder.

**Format `detailNilai`** (kolom `text`, dipakai juga oleh `Detailperkuliahan`,
`Skripsi`, `MahasiswaDapatKelompokKkn`): ruas dipisah `;`, tiap ruas lima
medan `idKomponen,nilai,0,bobot,verifikasi`. Medan 3 selalu literal `0` dan
tidak pernah dibaca. Medan 4 adalah **bobot** komponen (disalin saat tulis),
walau variabel penampungnya di `hitungTotalNilai` bernama `persen`. Bobot
tidak wajib berjumlah 100 — `hitungTotalNilai` menormalkan sendiri.

**Temuan / cacat (dicatat, TIDAK diperbaiki):**
- `reloadPklPunyaKomponenPenilaianPkl(Session)` menyaring properti `parent`,
  `persen`, `statusPertemuan` yang **tidak ada** pada
  `PklPunyaKomponenPenilaianPkl` → `QueryException` bila dijalankan. **Nol
  pemanggil** di seluruh pohon sumber = kode mati. Cacat **identik** ada di
  `MahasiswaDapatKelompokKkn.reloadKknPunyaKomponenPenilaianKkn` — pola
  salin-tempel lintas 2 modul, sama seperti bug syarat SKS/IPK Pkl/Kkn.
- `bersihkanNilaiKeDefault(List)` memanggil `Long.parseLong` **tanpa**
  try/catch (berbeda dari method sejenis di kelas yang sama) — satu ruas
  rusak menggagalkan seluruh alur penilaian.
- `getLulus()` memaksa `lulus=false` bila `nilaiHuruf` null, **menimpa**
  nilai yang sudah diset eksplisit lewat `setLulus`.
- `hitungTotalNilai` menampung ruas dalam `Map` berkunci id (ruas ganda
  menang-yang-terakhir) TAPI bobotnya sudah terlanjur dijumlahkan ke pembagi
  → rincian dengan ruas ganda menghasilkan nilai lebih rendah dari semestinya.
- **Tidak ada unique constraint** pada (`mahasiswa`, `kelompok_pkl`); semua
  pemanggil melindungi diri manual dengan `setMaxResults(1)`/hitung-dulu.

**Verifikasi pola berulang:** getter yang menulis balik ke field ADA
(`getKelompokPkl`/`getMahasiswa` via `check()` — tidak mengubah data;
`getLulus` mengoreksi `lulus` dari master NilaiHuruf; `getNamaDosen`
**menimpa kolom terpetakan** `namaDosen` tiap kali dibaca; `toString()`).
Getter yang **menutup session Hibernate: TIDAK ADA**. Getter **penghapus
data: TIDAK ADA** (beda dari `JadwalUjianPMB.getRuanganYgIkut`).
`refreshNilaiKeDefault`/`bersihkanNilaiKeDefault` memakai
`HibernateUtil.currentSession()` dan sengaja tidak menutupnya; keduanya
membaca field `kelompokPkl` LANGSUNG (bukan getter) → NPE bila belum terisi.
Flag `diterima`: **dua arah** (bukan satu arah); blok lama yang meng-auto-
approve kelompok non-pilihan-mahasiswa sudah dinonaktifkan (tinggal komentar).

**Perbandingan dengan `MahasiswaDapatKelompokKkn`** (567 baris, dikerjakan
sesi paralel): struktur **sama persis** — field, urutan method, format
`detailNilai`, bahkan cacat `reload*` yang sama. Beda hanya penamaan
(`kelompokKkn`/`KknPunyaKomponenPenilaianKkn`) dan nama tabel. Kembaran
ketiga: `SiswaDapatKelompokPkl` (jalur sekolah).

**Catatan lingkungan:** saat kompilasi verifikasi, `ais/common/OnlineBmtUtil.java`
(r83339, sesi paralel, 2 Sep 22:08) **gagal kompilasi** — 3 × `unreported
exception JSONException` di baris 76-78. Bukan akibat pekerjaan ini:
`Pkl.java` yang tidak disentuh mereproduksi galat identik. HEAD sedang rusak.

## `ais/database/model/GrupPertemuan.java` — SELESAI 100% (2 Sep 2026)

Entity **sesi konsultasi terjadwal** (tabel `public.grup_pertemuan`, `@Audited`,
`dynamicInsert/dynamicUpdate`). **62 method + konstruktor + 4 konstanta + 26
field** terdokumentasi (100%), 395 → 1102 baris. Revisi **r83344**, mirror
`java/` verifikasi `cmp` identik. Hanya Javadoc/komentar; nol perubahan logika
(dibuktikan dengan membandingkan sumber tanpa komentar/spasi terhadap HEAD —
identik persis).

**Koreksi hierarki:** bukan turunan langsung `GeneralValueObject`, melainkan
`GrupPertemuan` → `VOPembelajaran` → `VoKunci` → `sop.DataSop` →
`GeneralValueObject`.

**Struktur relasi (mengejutkan, diverifikasi dari kode):** meski namanya "grup
pertemuan", entity ini **BUKAN koleksi `Pertemuan`** dan **tidak punya satu pun
field koleksi**. Yang dikelompokkan adalah *peserta*, lewat entity penghubung
`PertemuanPunyaGrupPertemuan` yang menautkan `grupPertemuan` + `mahasiswa` +
`pertemuan`. Arah relasi **terbalik**: `Pertemuan.getPertemuanPunyaGrupPertemuan()`
menunjuk ke penghubung, bukan ke `GrupPertemuan`. Satu sesi dengan 20 peserta
menghasilkan **20 baris `Pertemuan` terpisah**, masing-masing menempel pada
`KrsMahasiswa`/`MahasiswaRequestTugasAkhir`/`Skripsi` milik mahasiswa itu
sendiri; `GrupPertemuan` hanya mengikatnya secara logis (nomor pertemuan, jam,
ruang, catatan, presensi, file/audio/video).

**Empat jenis konsultasi** (konstanta `jenis`, menentukan populasi calon
peserta di `GrupPertemuanAction#loadMahasiswa`/`#saveDetail`): `KRS_MAHASISWA`
(dosen PA → `Mahasiswa.dosen`, pertemuan digantung ke `KrsMahasiswa`),
`BIMBINGAN` (`MahasiswaRequestTugasAkhir.dosen1..dosen5`), `SIDANG`
(`Skripsi` pembimbing/ketua sidang/penguji1..4), `LAINNYA` (semua mahasiswa
aktif).

**Kuirk arsitektural utama — mesin pertemuan warisan yang inert:** dari
`VOPembelajaran`, class ini mewarisi seluruh mesin pertemuan
(`ambilPertemuan()`, `populatePertemuan()`, `reInitPertemuan/Tugas/Ujian`),
tetapi **semuanya mati**: rantai `instanceof` di `VOPembelajaran` hanya
mengenali 14 subtipe dan **tidak ada cabang `GrupPertemuan`** — kueri jatuh ke
`Restrictions.sqlRestriction("false")` dan selalu kosong. Yang terdaftar sebagai
subtipe sah justru `PertemuanPunyaGrupPertemuan`. Penelusuran seluruh pohon
sumber tidak menemukan satu pun pemanggilan mesin itu atas instance
`GrupPertemuan`, jadi tidak ada kerusakan nyata — tapi jangan diandalkan.

**Verifikasi pola berulang (getter tidak murni):** `getFakultas()` menimpa field
`jurusan` **dan** `fakultas` (sehingga `setFakultas()` efektif diabaikan setiap
kali jurusan terisi); `getTahunAkademik()`, `getJenisSemester()`, dan
`getTahun()` menulis balik ke field; `getDikunci()`/`getJurusan()`/`getRuang()`/
`getDosen()`/`getJenisLayananKepadaMahasiswa()` menugaskan hasil `check()`
kembali ke field. **Tidak ada** getter di file ini yang membuka/menutup
`Session` Hibernate sendiri, dan **tidak ada** getter penghapus data.

**Kuirk lain yang dicatat (tidak diperbaiki):**
- `getPertemuanKe()` default `0` padahal field diinisialisasi `1` — beda hanya
  terlihat pada baris migrasi lama yang kolomnya `NULL`.
- `getTahun()` membaca **field** `tahunAkademik` langsung (bukan getter-nya),
  jadi hasilnya bergantung urutan pemanggilan: tanpa `getTahunAkademik()` lebih
  dulu, hasilnya `null` walau tahun akademik punya default berjalan.
- `getJenis()` tanpa default meski kolomnya `NOT NULL`; beberapa pemanggil di
  `GrupPertemuanAction` dan `AmbilDataMahasiswaForGrupPertemuanDosenPaHelper`
  memanggil `getJenis().equals(...)` tanpa cek null.
- `getTahunAngkatan()` menghasilkan default dinamis "6 tahun terakhir" yang
  tidak ditulis balik — daftar bergeser sendiri tiap pergantian tahun.
- `dosenPengganti` disimpan sebagai `Long` telanjang, **bukan** relasi
  `@ManyToOne` seperti `dosen` — tanpa jaminan integritas referensial; pemanggil
  memuat manual dengan `Restrictions.idEq(...)`.

**Temuan build (di luar cakupan, tidak disentuh):** `ais/common/OnlineBmtUtil.java`
baris 76-78 **gagal kompilasi** — `JSONObject.put(String, Object)` melempar
checked `JSONException` pada versi org.json di classpath, tapi tidak
ditangkap/dideklarasikan. File itu tidak dimodifikasi lokal (sudah tercommit
sesi lain) dan tidak berkaitan dengan `GrupPertemuan`; ia hanya ikut tertarik
lewat `-sourcepath`. `GrupPertemuan.class` sendiri tetap terbentuk.

## `ais/database/model/MahasiswaDapatKelompokKkn.java` — SELESAI 100% (2 Sep 2026)

Entity **keanggotaan mahasiswa pada satu kelompok KKN** sekaligus kartu
nilainya (tabel `public.mahasiswa_dapat_kelompok_kelompok_kkn` — kata
"kelompok" memang dobel, jangan "dirapikan"; `@Audited`,
`dynamicInsert/dynamicUpdate`, turunan `GeneralValueObject`,
pengimplementasi `VOPesertaPembelajaran`). **46 method + konstruktor +
seluruh field** terdokumentasi (100%), 567 → 1297 baris. Revisi
**r83343**, mirror `java/` verifikasi `cmp` identik byte. Hanya
Javadoc/komentar; sumber tanpa komentar/spasi identik persis dengan HEAD
sebelum commit (r77693).

**Alur:** lapis ketiga modul KKN — `Kkn` (gelaran) → `MahasiswaDaftarKkn`
(seleksi, harus `DITERIMA`) → `KelompokKkn` → **entity ini** → penilaian
→ konversi ke `Detailperkuliahan` (KRS/IPK) → `Sertifikat`. **Tidak ada
FK langsung ke `kkn`**: gelaran hanya dicapai dua hop lewat
`kelompokKkn.kkn`, sehingga semua query memakai
`createAlias("kelompokKkn","kelompokKkn")`. Nol koleksi. Dua jalur
pembuatan baris: mahasiswa memilih sendiri (`KknUntukMahasiswaAction`)
atau operator menempatkan (`AmbilDataMahasiswaKelompokKknHelper`,
`KelompokKknHelper`). Flag `diterima` **dua arah** (operator bisa
mencentang & membatalkan).

**Nilai disimpan sebagai string**, bukan baris tabel: kolom `detailNilai`
bertipe `text` berformat `idKomponen,nilai,0,bobot,terverifikasi;…`
(kolom indeks 2 selalu literal `0`, slot warisan yang tak pernah dibaca;
indeks 0 adalah id `KomponenPenilaianKkn`, BUKAN
`KknPunyaKomponenPenilaianKkn` walau variabel lokalnya terlanjur dinamai
begitu). `hitungTotalNilai` = rata-rata terbobot ternormalisasi.

**Temuan (dicatat, TIDAK diperbaiki):**
- `reloadKknPunyaKomponenPenilaianKkn(Session)` **rusak & dead code** —
  menyaring properti `parent`/`persen`/`statusPertemuan` yang TIDAK ADA
  pada `KknPunyaKomponenPenilaianKkn` (entity itu cuma punya id, nama,
  keterangan, kkn, komponenPenilaianKkn + field audit) → `QueryException`
  begitu dieksekusi, dan kriteria dibangun **di luar** blok `try` jadi
  merambat ke pemanggil. Bentuknya identik penyaringan sah di modul
  perkuliahan (`PertemuanPunyaFormatNilai`) → sisa salin-tempel.
- Getter penulis-balik (pola berulang, terverifikasi dari kode):
  `getKelompokKkn`/`getMahasiswa` (`check()`), `getLulus` (menimpa `lulus`
  dari master Nilai Huruf + menormalkan `nilaiHuruf`), `getNamaDosen`
  (menghitung ulang nama pembimbing ke kolom persisten), `toString`, dan
  `refreshNilaiKeDefault` yang dipanggil dari hampir semua method nilai —
  termasuk `retreiveDetailNilai` yang "baca saja". Membaca bisa memicu
  `UPDATE` lewat dirty-checking.
- **Tidak ada method yang membuka/menutup session** — semua memakai
  `HibernateUtil.currentSession()` milik thread pemanggil.
- `getDiterima()` menormalkan `null`→`false`, tetapi query menyaring
  langsung ke kolom (`Restrictions.eq("diterima", false)`) sehingga baris
  ber-`NULL` tak terjaring filter "belum diterima".
- Duplikat id pada `detailNilai` menambah pembagi `totalPersen` tanpa
  menambah nilai (Map menimpa) → nilai akhir mengecil; jalur
  `hitungTotalNilai(true)` aman karena dibersihkan dulu.
- `getNamaDosen()` tanpa `@Transient` → kolom persisten berisi data
  turunan yang baru ikut berubah kalau barisnya kebetulan dibaca lagi.

## Batch "5 entity Kkn/Pkl/PMB/karya/billing" — SELESAI 100% (2 Sep 2026, dikonsolidasi orkestrator)

Semua 5 file TUNTAS 100% method, dikompilasi, dikommit, di-mirror ke `java/`:
- `Kkn.java` — 52/52+konstruktor. 337→1193 baris. r83321. AKAR modul KKN,
  nol koleksi (semua relasi ditarik dari sisi anak). **Bug fungsional sama
  persis dengan `Pkl.java`**: default syarat SKS/IPK alternatif (0/0.0)
  tidak cocok dengan nilai awal field (110/2.0) — mengaktifkan "Syarat
  Lain" tanpa isi angka justru MELOLOSKAN semua pendaftar. Konfirmasi bug
  copy-paste lintas 2 modul identik.
- `Pkl.java` — 53/53+konstruktor. 334→1122 baris. r83316/83319. Struktur
  sejajar `Kkn.java`; bug default syarat SKS/IPK yang sama (lihat atas).
- `JadwalUjianPMB.java` — 40/40+konstruktor. 273→858 baris. r83312/83314/83315.
  Koreksi hierarki: bukan langsung `GeneralValueObject`, tapi lewat
  `VOPembelajaran`. Getter penghapus data lagi (`getRuanganYgIkut`).
- `PenghargaanDosen.java` — 43/43. 277→1009 baris. r83317/83320. Nama
  class menyesatkan (modul UI-nya "Karya Dosen"/paten-HKI, BUKAN sekadar
  "penghargaan" generik) — konsep BEDA dari `PrestasiDosen` (dikonfirmasi
  terpisah di dashboard+cache index). **ESKALASI KEAMANAN BARU**: broken
  access control di `PenghargaanDosenAction.java` (parameter `dosen=<id>`
  tanpa cek kepemilikan). Task `task_c27d18e4` (sekaligus audit pola
  serupa di action lain — pola KEDUA setelah beasiswa `task_51f767ec`).
- `PembayaranMahasiswa.java` — 46/46+konstruktor. 288→790 baris. r83311/83313.
  Pemetaan JPA KEDUA atas tabel SAMA dengan `Kegiatan.java` (dual-mapping
  antipattern) — bug nyata: perbaikan `kodeunik` yang sudah diterapkan di
  `Kegiatan` tidak pernah disalin ke sini, potensi tabrakan `unique`.

**6 task eskalasi keamanan/privasi aktif sekarang** (2 kategori BARU sesi
ini menambah "broken access control" jadi pola berulang — 2 instance
ditemukan dalam 1 batch): `task_15f5001e` (arsitektur getter destruktif),
`task_b0a90191` (command injection VA), `task_78a5b1ab` (kebocoran
kredensial log login), `task_51f767ec` (akses lintas-mahasiswa beasiswa),
`task_18d52b8b` (kebocoran identitas pelapor pengaduan), `task_c27d18e4`
(broken access control karya dosen + audit pola serupa — BARU).

**Total akumulasi 14 sesi kerja**: 243 (sesi 1-13) + 5 = **248 file** dari
7.401 (~3,4%).

## `ais/database/model/Kkn.java` — SELESAI 100% (2 Sep 2026)

Entity **gelaran/periode KKN** (tabel `public.kkn`, `@Audited`,
`dynamicInsert/dynamicUpdate`, turunan langsung `GeneralValueObject`).
**52 method + konstruktor + 26 field** terdokumentasi (100%), 337 → 1193
baris. Revisi **r83321**, mirror `java/` verifikasi `cmp` identik. Hanya
Javadoc/komentar; nol perubahan logika (dibuktikan dengan membandingkan
sumber tanpa komentar/spasi terhadap HEAD r73618 — identik persis).

**Alur:** `Kkn` adalah AKAR seluruh modul KKN. Tujuh entity ber-FK ke
`kkn`: `KelompokKkn` (nullable) → `MahasiswaDapatKelompokKkn` (dua hop,
tidak ada FK langsung), `MahasiswaDaftarKkn` (pendaftar + `memenuhiSyarat`
+ `totalSkor`), `MahasiswaDapatKkn` (peserta diterima),
`KknPunyaPersyaratan` → `PersyaratanKkn` → `MahasiswaKknPersyaratan`,
`KknPunyaKomponenPenilaianKkn` → `KomponenPenilaianKkn`, dan
`PengecualianKknMahasiswa`. **Tidak ada satu pun koleksi di sisi `Kkn`** —
semua relasi ditarik dari sisi anak dengan `Restrictions.eq("kkn", kkn)`,
jadi menghapus baris `kkn` tidak meng-cascade apa pun (data anak jadi
yatim).

**Gerbang kelayakan** ada di `Common.checkSyaratKkn(Mahasiswa, Kkn)`
(`Common.java:13080`), dipanggil dari `KknUntukMahasiswaAction`,
`AmbilDataMahasiswaKknHelper`, dan `AmbilDataMahasiswaSeleksiKknHelper`:
pengecualian → jurusan → fakultas → `(sks≥S1 ∧ ipk≥I1) ∨ (aktifkanSyaratLain
∧ sks≥S2 ∧ ipk≥I2)`. AND di dalam pasangan, OR antar-pasangan.

**Verifikasi pola berulang:**
- Pemetaan berbasis properti (`@Id` di `getId()`, nol `@Transient`) →
  seluruh getter adalah kolom.
- **14 getter menulis balik ke field**; 3 netral (`check()`:
  `getJurusan`, `getFakultas`, `getJenisAktfitasMahasiswa`), **11
  benar-benar mengubah data** (`getNama`, empat getter ambang SKS/IPK,
  `getAktifkanSyaratLain`, `getHarusBayar`, `getKodeItemBiaya`,
  `getSemester`, `getTahunAkademik`, `getNimMhsTanpaBiaya`).
- Getter tanpa penulisan balik tapi tetap mengubah nilai tersimpan:
  `getProgram`, `getMahasiswaBolehMerubahAgenda`,
  `getDosenBolehMerubahAgenda`, fallback `getJenisAktfitasMahasiswa`.
- **TIDAK ADA field `aktif` sama sekali** (diverifikasi dari kode).
  Visibilitas gelaran ditentukan kecocokan `jurusan`/`fakultas`/`program`
  di `KknUntukMahasiswaAction.initCriteria`, bukan flag — dan bukan pula
  `tanggal_selesai` (tanggal tidak ikut menyaring; pendaftaran tidak
  tertutup sendiri setelah gelaran berakhir).
- **TIDAK ADA getter yang membuka/menutup sesi Hibernate**; satu-satunya
  sentuhan persistensi adalah `check()` pada tiga getter relasi.
- `setOleh`/`setOlehId` satu arah (masukan kosong/`null` diabaikan).

**Kuirk/bug yang dicatat (tidak diperbaiki):**
- **Jebakan konfigurasi paling serius**: `getMinimalSksBolehIkutKkn2()` dan
  `getMinimalIpkBolehIkutKkn2()` mengganti `null` dengan **0 / 0.0**,
  padahal nilai awal field-nya 110 / 2.0. Pada gelaran lama yang kolom
  syarat-2-nya masih `null`, mencentang "Aktifkan Syarat Lain" tanpa
  mengisi angkanya membuat cabang alternatif berbunyi `sks≥0 ∧ ipk≥0.0` —
  yaitu **meloloskan SEMUA pendaftar**, kebalikan dari maksud operator.
- **Ambang bulan tidak selaras**: fallback kalender `getSemester()` mulai
  Ganjil pada `MONTH >= 5` (Juni), sedangkan `getTahunAkademik()` maju ke
  `YYYY/YYYY+1` baru pada `MONTH > 5` (Juli). Gelaran yang `tanggal_mulai`
  di bulan Juni + kedua kolom `null` terisi otomatis dengan pasangan
  mustahil (Ganjil tapi TA periode sebelumnya), dan nilai itu langsung
  tersimpan.
- Nilai turunan `getSemester()`/`getTahunAkademik()` **bergantung pada
  pengguna yang sedang login** (`Common.getCurrentUser()` di dalam
  `CommonCurrentSessionHelper`) — proses latar bisa menghasilkan nilai lain.
- `program` **hanya** menyaring daftar yang terlihat mahasiswa; tidak
  divalidasi ulang di `checkSyaratKkn` (beda dari `jurusan`/`fakultas`),
  sehingga penambahan peserta massal oleh operator bisa menembus batas
  program.
- `ConstantValues.KKN` bukan konstanta melainkan field statis **mutable
  non-final**; penugasannya di `InitDataHelper` bergantung pada nama baris
  master yang persis `"Kuliah kerja nyata"` (lookup-nya `feeder = 5`).
  Nama yang pernah diedit operator → konstanta tetap `null` →
  `KknAction` yang merangkai
  `kkn.getJenisAktfitasMahasiswa().getKampusMerderka()` melempar NPE.
- Field `nama_kelompok` menyimpan **nama gelaran**, bukan nama kelompok
  (nama kelompok yang sebenarnya ada di `kkn/KelompokKkn.java`).
  `toString()` dan `getNama()` keduanya memakainya; `setNama(String)`
  praktis tidak berguna karena selalu ditimpa `getNama()`.
- `getNimMhsTanpaBiaya()` menormalkan nilai ke bentuk `",nim1,nim2,"`
  dengan `replaceAll(",,", ",")` **tiga kali berturut-turut** (perlu karena
  `replaceAll` tak menangani pencocokan tumpang tindih); cukup untuk
  maksimal 8 koma beruntun, lebih dari itu masih menyisakan koma ganda.
  NIM di daftar ini melewati **kedua** gerbang biaya sekaligus.
- Kode di `kodeItemBiaya` yang tidak ditemukan di master `ItemBiaya`
  **diabaikan diam-diam** — salah ketik kode = syarat hilang tanpa
  peringatan.
- `KelompokKkn.kkn` nullable; kelompok yatim membuat
  `MahasiswaDapatKelompokKkn` kehilangan komponen nilai dan
  `AktifitasKknHelper` kehilangan sakelar izin agenda.
- Kembaran hampir persis: `ais/database/model/Pkl.java` +
  `Common.checkSyaratPkl` (disebut eksplisit sebagai "copy semantis").

**Tidak ada kerentanan keamanan/privasi baru** yang ditemukan di file ini.

## `ais/database/model/PenghargaanDosen.java` — SELESAI 100% (2 Sep 2026)

Entity **karya dosen** (tabel `public.penghargaan_dosen`, `@Audited`,
`dynamicInsert/dynamicUpdate`, turunan langsung `GeneralValueObject`).
**43 anggota** (konstruktor + 42 getter/setter/kait) + 4 konstanta + 22 field
terdokumentasi (100%), 277 → 1009 baris. Revisi **r83317**, mirror `java/`
verifikasi `cmp` identik byte. Hanya Javadoc/komentar; nol perubahan logika
(dibuktikan dengan membandingkan sumber tanpa komentar/spasi terhadap HEAD —
identik persis).

**Nama class menyesatkan**: seluruh lapisan UI menyebut modul ini **"Karya
Dosen"**, bukan "Penghargaan Dosen" — menu `NewUiLayarLainnyaController`
("Karya Dosen"), label form ("Nama Karya", "Bentuk Penghargaan"), koleksi
DSpace "Karya Dosen", `DasborPerguruanTinggiTerpadu.createKaryaDosenCriteria()`,
dan laporan SAPTO `LaporanKaryaDosen_A_7_1_5` (HKI/Paten).

**Perbandingan dengan `PrestasiDosen` (r83288)**: **dua konsep BERBEDA**, bukan
duplikat — dibuktikan dari kode: dasbor terpadu menghitung keduanya berdampingan
(`createKaryaDosenCriteria` vs `createPrestasiDosenCriteria`), `ProfileDosen`
menampilkan dua grup terpisah, dan `Dosen` memelihara dua berkas indeks JSON
terpisah (`penghargaanDosen_<id>` vs `prestasiDosen_<id>`). Isi berbeda:
`PrestasiDosen` = ajang/kejuaraan (cabang+kategori tingkat, juara, peringkat,
jumlahPeserta, tempat, penyelenggara, prestasiLuarKampus); `PenghargaanDosen` =
karya/HKI (satu sumbu klasifikasi `kategoriPenghargaan` = bentuk karya:
Paten / HaKI / Nasional-Internasional). **Bentuknya** tumpang tindih berat
(serialVersionUID identik, blok audit, 4 konstanta status, nama/namaEn,
tanggal/tanggalSelesai, nomorSertifikat, capaian, url, trio periode, DSpace,
LampiranLain, cache indeks) — jangan simpulkan salah satunya redundan.

**Verifikasi pola berulang**: 3 getter menulis balik ke field
(`getTahun`/`getTahunAkademik`/`getJenisSemester` — risiko flush diam-diam
karena `dynamicUpdate`); **0** getter memakai `check()` pada relasi (BEDA dari
`PrestasiDosen` yang punya 3 — keempat relasi di sini eager +
`FetchMode.SELECT` mentah); **0** getter menutup sesi Hibernate (file tidak
menyentuh `Session` sama sekali; jalur tak langsung lewat `Common` memakai
`openSession()` sendiri).

**Kuirk dicatat, tidak diperbaiki**:
- `KategoriPenghargaan.getKode()` memetakan nama **tingkat prestasi**
  ("Internasional"/"Nasional"/"Regional"/...) hasil salin dari
  `KategoriPrestasiDosen` — tidak satu pun cocok dengan master yang di-seed
  otomatis ("Paten", "HaKI", "Nasional / Internasional"), jadi selalu
  mengembalikan kolom `kode` apa adanya (umumnya null). Klasifikasi nyata
  dilakukan `LaporanKaryaDosen_A_7_1_5` lewat `contains("paten")`/
  `contains("haki")` — ganti nama master = ubah hasil laporan akreditasi
  tanpa peringatan.
- Semua pesan validasi `PenghargaanDosenAction.onSave()` berbunyi
  **"Kejuaraan"** padahal label form "Karya"/"Bentuk Penghargaan".
- `setTahun()` praktis tidak berpengaruh (ditimpa `getTahun()`).
- `getStatus()` mengembalikan default `BELUM_DIPROSES` tanpa menulis balik,
  sehingga baris ber-`NULL` **tidak** terjaring penyaring status SQL.
- Kolom `fakultas`/`jurusan` entity ini tidak dipakai penyaring mana pun —
  semua filter memakai homebase dosen (`dosen.fakultas`/`dosen.jurusan`).
- `nomorSertifikat` diekspor sebagai `dc.identifier.issn` (bukan ISSN).
- `Dosen.removePenghargaanDosen()` hanya mengosongkan nilai kunci JSON
  (kunci tetap ada) → berkas indeks membesar monoton sampai di-`reInit`.

**Temuan akses (bukan di entity, di `PenghargaanDosenAction`)**:
`doAfterCompose` menerima parameter URL `dosen=<id>` dan memuat **Dosen mana
pun** — menimpa `tbmuser.ambilDosen()` milik pengguna yang masuk. Tombol
ubah/hapus baris hanya dijaga `!status.equals(DISETUJUI) && tbmuser != null`,
**tanpa pemeriksaan kepemilikan**. Jadi pengguna ber-role dosen yang membuka
`/pages/master/penghargaan_dosen.zul?dosen=<id dosen lain>` dapat melihat dan
**mengubah/menghapus** karya dosen lain yang belum disetujui. (Ubah status
tetap tertutup karena combobox status hanya muncul saat `mhs == null`.)

## `ais/database/model/Pkl.java` — SELESAI 100% (2 Sep 2026)

Entity **program PKL** (Praktik Kerja Lapangan / kerja praktek; tabel
`public.pkl`, `@Audited`, `dynamicInsert/dynamicUpdate`, turunan langsung
`GeneralValueObject`). **53 method + konstruktor + 22 field**
terdokumentasi (100%), 334 → 1122 baris. Revisi **r83316**, mirror `java/`
diverifikasi `cmp` identik byte. Hanya Javadoc/komentar; nol perubahan
logika (dibuktikan dengan membandingkan sumber tanpa komentar/spasi
terhadap HEAD — identik persis, 7290 byte di kedua sisi).

**Lapisan yang benar:** `Pkl` adalah lapisan **program/periode**, BUKAN
penempatan. Tempat magang (alamat, `Lokasi`, `KerjasamaAntarInstansi`),
kuota, sampai 10 dosen pembimbing, sertifikat, dan flag `aktif` semuanya
ada di `ais.database.model.pkl.KelompokPkl`. Jangan mencari `aktif`/
`kuota` di `Pkl.java` — memang tidak ada.

**Alur:** `PklAction` (admin) → `PersyaratanPkl`/`KomponenPenilaianPkl`
dikaitkan lewat `PklPunyaPersyaratan`/`PklPunyaKomponenPenilaianPkl` →
`PklUntukMahasiswaAction` (mahasiswa mendaftar) → `MahasiswaDaftarPkl`
(`terima` = BELUM_DIPROSES/DITERIMA/DITOLAK) + `MahasiswaPklPersyaratan`
→ `SeleksiPenerimaPklAction`/`PendaftarPklHelper` → `KelompokPkl` +
`MahasiswaDapatKelompokPkl`/`SiswaDapatKelompokPkl` → `AktifitasPklHelper`
(agenda `Pertemuan`) → `PenilaianPklHelper`. Jalur samping:
`MahasiswaDapatPkl` (mahasiswa ↔ program tanpa kelompok) dan
`PengecualianPklMahasiswa` (bebas syarat akademis). Ekspor Feeder lewat
`EksporAktifitasPklFeeder`/`EksporPeserta{Dosen,Mahasiswa}PklFeeder`.

**Verifikasi pola berulang (diperiksa dari kode, bukan diasumsikan):**
- **Getter menulis balik saat `null`** (9): `getMinimalSksBolehIkutPkl`
  (100), `getMinimalIpkBolehIkutPkl` (3.0), `getMinimalSksBolehIkutPkl2`
  (0), `getMinimalIpkBolehIkutPkl2` (0.0), `getAktifkanSyaratLain`
  (false), `getHarusBayar` (false), `getKodeItemBiaya` (""),
  `getSemester`, `getTahunAkademik`.
- **Getter SELALU menimpa field**: `getNama()` (menyalin
  `nama_kelompok`) dan `getNimMhsTanpaBiaya()` (menormalkan jadi
  `,nim1,nim2,`) — membaca saja mengubah kolom pada flush berikutnya.
- **Getter resolusi proxy `x = check(x)`** (3): `getJurusan`,
  `getFakultas`, `getJenisAktfitasMahasiswa`.
- **Getter yang TIDAK menulis balik**: `getTanggal_mulai` (fallback hari
  ini), `getProgram` (kosong → `null`), cabang fallback
  `getJenisAktfitasMahasiswa` → `ConstantValues.PKL`. Karena pemetaan
  **property access**, nilai kembalian getter tetap yang dilihat Hibernate
  saat dirty-check, jadi normalisasi tanpa write-back pun bisa mengubah
  kolom.
- **Sesi Hibernate**: NOL. Kelas ini tidak meng-import `HibernateUtil`;
  satu-satunya akses DB implisit lewat `GeneralValueObject.check()`.
- **Flag `aktif`**: tidak ada di entity ini.

**Kuirk/temuan (dicatat, tidak diperbaiki):**
- **Default syarat alternatif terbalik arah.** Field diinisialisasi
  `minimalSksBolehIkutPkl2 = 110` / `minimalIpkBolehIkutPkl2 = 2.0`
  (hanya untuk object baru), tapi getter mendefault kolom `NULL` menjadi
  **0 SKS / IPK 0.0**. Baris lama ber-`aktifkanSyaratLain = true` karena
  itu punya jalur alternatif `sks >= 0 && ipk >= 0.0` yang SELALU benar →
  seluruh penyaringan SKS/IPK program itu efektif mati. Kontras dengan
  syarat utama yang mendefault ketat (100 SKS / IPK 3.0).
- **Kolom `nama` adalah salinan bayangan.** Nama program sesungguhnya di
  `nama_kelompok`; `getNama()` selalu menimpa `nama` dengan salinannya.
  Karena `hbm2ddl.auto=update` + `MyNamingStrategy` (turunan
  `DefaultNamingStrategy`), kolom `nama` benar-benar ada dan ikut
  ter-UPDATE. `setNama()` praktis tak berguna.
- **`toString()` memakai field mentah** `nama_kelompok` (bukan getter) →
  bisa mengembalikan `null` pada komponen ZK yang memanggilnya implisit.
- **Kedua flag `*BolehMerubahAgenda` fail-open** (`null` → `true`),
  berbeda arah dengan semua flag boolean lain di kelas ini yang default
  `false`. Program lama otomatis mengizinkan mahasiswa & dosen menyunting
  agenda.
- **`ConstantValues.PKL` bisa `null`** — hanya terisi bila sinkronisasi
  Neo Feeder (`InitDataHelper`) pernah menemukan jenis aktivitas "Kerja
  praktek/PKL". Fallback `getJenisAktfitasMahasiswa()` karena itu tidak
  dijamin non-null.
- **Kode item biaya salah ketik diabaikan diam-diam** — pemanggil mencari
  `ItemBiaya` per kode dan melewatkan yang tidak ketemu tanpa peringatan,
  sehingga syarat pembayaran hilang tanpa jejak.
- **Kode defensif tak terjangkau** di `getNimMhsTanpaBiaya()`: tiga cabang
  `if` (`","`, `",,"`, `",,,"`) dan cek `null` pada baris `return` tidak
  bisa tercapai setelah tiga kali `replaceAll(",,", ",")`.
- **Pesan galat salah modul (di `Common.java`, bukan file ini):**
  `Common.checkSyaratPkl` menampilkan "tidak bisa mendaftar di **KKN**"
  pada dua pesan penolakan jurusan/fakultas — sisa salin-tempel dari
  `checkSyaratKkn`. Membingungkan pengguna, tidak berbahaya.

**Tidak ada kerentanan keamanan/privasi baru** pada file ini. Daftar putih
`nimMhsTanpaBiaya` melewati seluruh pemeriksaan biaya (`kodeItemBiaya`
DAN `harusBayar`) tetapi hanya bisa disunting dari layar admin
`PklAction`, dan pemeriksaan syarat akademis tetap berjalan — perilaku
yang disengaja, bukan cacat.

## `ais/database/model/JadwalUjianPMB.java` — SELESAI 100% (2 Sep 2026, sesi 14)

Entity **sesi ujian PMB daring** (tabel `public.jadwal_ujian_pmb`, `@Audited`,
`dynamicInsert/dynamicUpdate`). **40 method + konstruktor + 19 field**
terdokumentasi (100%), 273 → 858 baris. Revisi **r83312**, mirror `java/`
verifikasi `cmp` identik byte. Hanya Javadoc/komentar; nol perubahan logika
(dibuktikan membandingkan sumber tanpa komentar/spasi terhadap HEAD — identik).

**Koreksi penting terhadap asumsi awal**: file ini **BUKAN** turunan langsung
`GeneralValueObject`. Silsilahnya `JadwalUjianPMB → VOPembelajaran → VoKunci →
DataSop → GeneralValueObject`. Artinya ia anggota keluarga **unit pembelajaran**
dan mewarisi cuma-cuma seluruh mesin pertemuan/e-learning/Google Classroom —
itulah sebabnya "jadwal ujian PMB" bisa diperlakukan persis seperti `Perkuliahan`
oleh layar e-learning, absensi, dan dasbor timeline. Deklarasi ulang field audit
(`id`/`oleh`/`olehId`/`tanggal_dirubah`) tetap keharusan teknis seperti biasa.

**Verifikasi rantai relasi yang diklaim `UjianPMB.java` (r83292)**: rantai
`UjianPMB → JadwalUjianPMB → Pertemuan → PertemuanPunyaUjian → Ujian → BankSoal`
**benar, tetapi hanya mata rantai pertamanya relasi Hibernate biasa**
(`@ManyToOne` kolom `ujian_pmb`, `nullable = false`; tak ada `@OneToMany` balik).
Mata rantai `JadwalUjianPMB → Pertemuan` **tidak ada sebagai properti sama
sekali** — arahnya terbalik (`Pertemuan.jadwalUjianPMB`), dan penelusuran maju
lewat `VOPembelajaran.ambilPertemuanList()` yang **membaca berkas JSON pendamping
di disk** (`Common.getFileLocation(this, "pertemuan_" + id)`), bukan query
langsung. Bila berkas itu hilang/tak sinkron, daftar pertemuan tampak kosong
padahal barisnya ada di DB — di situlah `RecoveryPertemuanHelper` bermain.

**Penentuan peserta** (tak ada relasi ke `BiodataCalonMahasiswa`, dihitung ulang
tiap kali oleh `AbsensiHelper`/`HasilUjianMahasiswaHelper`), berurutan:
1. `pesertaUjianHarusTelahUjian` → dari `HasilUjianMahasiswa` pertemuan itu;
2. `ruanganYgIkut` tak kosong → penghuni `RuangPaketPMB` ruang terdaftar;
3. selain itu → seluruh calon aktif pada gelombang induk, disaring `paket`.
`pesertaUjianHarusPunyaNomorUjian` filter tambahan di semua cabang.

**Kuirk (dicatat, tidak diperbaiki)**:
- `getRuanganYgIkut()` getter berefek samping yang **menghapus data**: bila
  `getBerlakuUntukSemuaRuangan()` true — **termasuk saat kolomnya masih `null`,
  karena getter itu default `true`** — daftar ruang dikosongkan permanen di
  field. Baris lama tanpa flag itu tak pernah bisa menahan daftar ruang sampai
  kolomnya ditulis `false` eksplisit. Keluarga sama dengan getter destruktif
  `UjianPMB.getTanggalUjian2..10` (r83292).
- `getWaktuMulai()`/`getWaktuSampai()` mengisi field dengan waktu server bila
  `null` (kolom `nullable = false`) — baris lama "menjadi hari ini" saat dibaca.
- Cabang mati di `getRuanganYgIkut()`: `equals(",,")`/`equals(",,,")` tak
  terjangkau setelah rangkaian `replaceAll(",,", ",")`; cek `null` pada `return`
  juga mustahil.
- `ambilJumlahDetailperkuliahanLangsung()` stub `return 0` → semua UI generik
  (`TampilanELearningAction`, `CommonUiFactoryHelper`, `ElearningApiUtil`)
  menampilkan **0 peserta** untuk kartu jadwal ujian PMB.
- Properti `dikunci` **dorman**: hanya memenuhi kontrak `VoKunci`, tak satu pun
  layar/helper PMB pernah menulis atau membacanya.
- `getCourse()` tak pernah `null`/kosong (kembalikan `"{}"`) — itulah yang
  menyelamatkan `ClassRoomUtil` dari NPE pada sinkronisasi pertama.
- `serialVersionUID` identik dengan `Ujian` dan `UjianPMB` (salin-tempel).
- **Injeksi tingkat-dua (risiko rendah, tidak dieskalasi)**: nilai
  `getRuanganYgIkut()` disambung mentah ke
  `Restrictions.sqlRestriction("ruang_pmb in (-1" + … + "-1)")` di
  `AbsensiHelper`, `HasilUjianMahasiswaHelper`, `DashboardTimelinePertemuan`,
  dan `JadwalUjianPMBAction`; getter hanya merapikan koma, **tidak memvalidasi
  isinya numerik**. Butuh akses tulis DB lebih dulu.

**Catatan alat**: kali ini `grep -cU` dan cross-check `perl`/`od -c` **sepakat**
(273/273 sebelum, 858/858 sesudah) — tidak ada anomali CRLF pada file ini.

## `ais/database/model/PembayaranMahasiswa.java` — SELESAI 100% (2 Sep 2026, sesi 14)

**46 method + konstruktor + 19 field** terdokumentasi (100%), 288 → 790 baris.
Revisi **r83311**, mirror `java/` verifikasi `cmp` identik. Hanya
Javadoc/komentar; nol perubahan logika (sumber tanpa komentar/spasi identik
persis dengan HEAD r73618). Lulus `javac 1.7 -implicit:none`.

**TEMUAN STRUKTURAL BESAR**: file ini **BUKAN entity tabel tersendiri**.
Anotasinya `@Table(schema = "public", name = "kegiatan")` — SAMA PERSIS dengan
`Kegiatan.java`. Jadi `PembayaranMahasiswa` adalah **pemetaan JPA KEDUA yang
ramping atas tabel tagihan `public.kegiatan`** (±19 kolom inti vs ±2.100 baris
`Kegiatan.java`). Bukti copy-paste: `serialVersionUID` identik
(`2413822577548439808L`), komentar generator & tanggal hbm2java sama, salah
ketik properti `semster` ikut terbawa. Jawaban pertanyaan brief: ia **BAGIAN
rantai billing utama** (`ItemBiaya→DetailBiaya→…`, `Kegiatan ≡
PembayaranMahasiswa → DetailKegiatan/CicilanPembayaran`), **bukan** snapshot
pelaporan seperti kasus sesi 12.

**Pemakai tipe yang sesungguhnya cuma 3** (dari 99 berkas yang menyebut string
"PembayaranMahasiswa", mayoritas hanya substring nama lain seperti
`PembayaranMahasiswaAction`/`checkStatusPembayaranMahasiswa`):
1. REST mobile `MahasiswaResource#lihatPembayaran` / `#lihatDetailPembayaran`
   lewat `PembayaranUtil#checkPembayaranMahasiswa` (Criteria
   `mahasiswa+jenisKegiatan+semster`, `HibernateUtil.closeSession()` dipanggil
   SEBELUM objek dikembalikan → objek diterima *detached*; aman selama semua
   relasi `@ManyToOne`).
2. Dasbor CRUD generik v2 (`StudentPaymentWorkflowGenericCrudAdapter`, natural
   key `kodeunik`, kolom urut `tanggal`) — dan **masuk daftar
   `AUTO_CREATE_BLOCKED_CLASSES`** di `GenericCrudAutoDefinitionFactory`
   sehingga baris baru tak bisa dibuat dari dasbor.
3. Menu `NewUiModuleDashboardService` (dipasang 2×: "Pembayaran Mahasiswa" dan
   "Pembayaran").

**Kuirk/bug yang dicatat (tidak diperbaiki)**:
- `getKodeunik()` = **getter berefek samping** (menulis ke field SENDIRI, bukan
  ke objek lain) yang **meng-`null`-kan kolom `nullable=false`** bila
  `mahasiswa`/`jenisKegiatan`/`semster` tak lengkap. Karena akses Hibernate
  bertipe *property* (`@Id` di getter), Hibernate memanggilnya sendiri saat
  dirty-check/INSERT/UPDATE → `null value in column kodeunik violates not-null
  constraint`. **Fallback `Common.getGeneratedBarCode()` yang SUDAH diterapkan
  & berkomentar panjang di `Kegiatan.getKodeunik()` tidak pernah disalin ke
  sini** — perbaikan hanya menyentuh satu dari dua entity tabel yang sama.
- **Format `kodeunik` beda untuk baris yang sama**: di sini
  `""+idMhs+idJenis+smt` (tanpa awalan/pemisah) vs
  `Kegiatan.generateKodeUnik()` `"MHS_<idMhs>-<idJenis>-<smt>"`. Risiko (a)
  tabrakan pada kolom `unique` (mhs 1/jenis 23/smt 4 dan mhs 12/jenis 3/smt 4
  sama-sama `"1234"`), (b) pencarian `Restrictions.eq("kodeunik", …)` di
  `KegiatanHelper` (baris 840, 965) dan `VOMahasiswa` (744) meleset. Baru
  menggigit pada operasi TULIS; jalur baca REST aman.
- `getAmountTerhutang()` di sini membaca kolom mentah, sedangkan
  `Kegiatan.getAmountTerhutang()` menghitung ulang `tagihan-dibayar` tiap
  dibaca → nilai REST bisa basi.
- `toString()` mengembalikan `refNumber` mentah → **dapat `null`** (melanggar
  kontrak `Object.toString()`); dipakai sebagai label baris dasbor generik.
- Kolom `calon_mahasiswa` tidak dipetakan → tagihan PMB terbaca dengan
  `mahasiswa == null` tanpa cara tahu pemiliknya.
- **Envers ganda**: dua entity `@Audited` menulis revisi untuk tabel audit yang
  sama; riwayat satu baris bisa campuran dua himpunan kolom.
- Tidak ada `equals`/`hashCode` → baris sama yang dibaca sebagai `Kegiatan` dan
  sebagai `PembayaranMahasiswa` tak pernah `equals`.

**Verifikasi pola berulang**: TIDAK ditemukan getter yang menulis ke objek LAIN,
dan TIDAK ada getter yang menutup sesi Hibernate di dalam entity (penutupan sesi
ada di pemanggil `PembayaranUtil`, bukan di entity). Satu-satunya getter
berefek samping adalah `getKodeunik()` (menulis ke field sendiri).

**Catatan alat**: `grep -cU $'\r$'` kali ini AKUR dengan cross-check
`perl`/`od -c` (288/288 lalu 790/790 CRLF).

## Batch "5 entity beasiswa/prasyarat/pengaduan/PMB" — SELESAI 100% (2 Sep 2026, dikonsolidasi orkestrator)

Semua 5 file TUNTAS 100% method, dikompilasi, dikommit, di-mirror ke `java/`:
- `PengajuanBeasiswa.java` — 59/59. 330→1032 baris. r83287/83293.
  **ESKALASI KEAMANAN**: `PengajuanBeasiswaAction.initCriteria()` tanpa
  filter kepemilikan sama sekali — mahasiswa mana pun bisa melihat (dan
  berpotensi edit/hapus) data pribadi keluarga pengajuan beasiswa mahasiswa
  lain. Task `task_51f767ec`.
- `MatakuliahPrasyarat.java` — 63/63. 281→818 baris. r83286/83289. Menjawab
  pertanyaan terbuka sesi 10: DI SINI-lah prasyarat MK sungguhan disimpan
  (bukan di `Matakuliah.java`). Jebakan data-entry: slot 1 kosong mematikan
  pengecekan slot 2-10 sepenuhnya.
- `Pengaduan.java` — 57/57. 460→1219 baris. r83290/83296.
  **ESKALASI PRIVASI SERIUS**: aduan pegawai TENTANG atasan langsungnya
  sendiri otomatis dirutekan ke meja persetujuan atasan yang diadukan itu
  sendiri (risiko pembalasan ke whistleblower) — tidak ada field "pihak
  diadukan" sama sekali. Plus nomor WhatsApp pelapor tersimpan mentah &
  ikut ekspor massal. Task `task_18d52b8b`.
- `PrestasiDosen.java` — 57/57. 345→1131 baris. r83288/83291. Sama persis
  pola tertukar `cabang`/`kategori` dengan `PrestasiMahasiswa`; SQL
  injection tingkat-dua serupa (risiko rendah, tidak dieskalasi) ditemukan
  lagi di dashboard rekap dosen.
- `UjianPMB.java` — 55/55+konstruktor. 362→1103 baris. r83292/83297. Getter
  penghapus data (`getTanggalUjian2..10`) — membaca saja bisa menghapus
  tanggal ujian permanen dari DB.

**3 task eskalasi keamanan/privasi BARU ditambahkan sesi ini** (total kini
5 task aktif): `task_51f767ec` (kebocoran data beasiswa antar-mahasiswa),
`task_18d52b8b` (kebocoran identitas pelapor pengaduan ke terlapor) — kedua
ini BEDA KATEGORI dari `task_15f5001e`/`task_b0a90191`/`task_78a5b1ab`
sebelumnya: BUKAN soal getter-berefek-samping atau injection, melainkan
**broken access control / kebocoran privasi lintas-pengguna**. Kategori
kerentanan yang ditemukan inisiatif ini sekarang mencakup: arsitektur
getter destruktif, command injection, kebocoran kredensial, DAN broken
access control.

**Catatan alat**: `grep -cU $'\r$'` menyesatkan LAGI di `MatakuliahPrasyarat.java`
(melaporkan CRLF pada file LF murni) — ini sudah kejadian ke-3+ dalam
inisiatif ini. Brief agent sekarang SELALU minta cross-check `perl`/`od -c`,
terbukti efektif menangkapnya tiap kali.

**Total akumulasi 13 sesi kerja**: 238 (sesi 1-12) + 5 = **243 file** dari
7.401 (~3,3%).

## `ais/database/model/UjianPMB.java` — SELESAI 100% (2 Sep 2026)

Entity **gelaran ujian seleksi PMB** (tabel `public.ujian_pmb`, `@Audited`,
`dynamicInsert/dynamicUpdate`, turunan langsung `GeneralValueObject`).
**55 method + konstruktor + 22 field** terdokumentasi (100%), 362 → 1103
baris. Revisi **r83292**, mirror `java/` verifikasi `cmp` identik. Hanya
Javadoc/komentar; nol perubahan logika (dibuktikan dengan membandingkan
sumber tanpa komentar/spasi terhadap HEAD r83275 — identik persis).

**Alur:** `GelombangPendaftaran` → `UjianPMB` (kapan & di mana ujian digelar)
→ `RuangPMB` → `RuangPaketPMB` → `BiodataCalonMahasiswa`. Cabang kedua:
`UjianPMB` → `JadwalUjianPMB` → `Pertemuan` → `PertemuanPunyaUjian` →
`Ujian` → `BankSoal` (ujian PMB daring). **Tidak ada relasi langsung ke
`BiodataCalonMahasiswa`** — selalu lewat penempatan ruang.

**Beda tegas dengan `Ujian.java`** (sesi 10, r83178): `Ujian` = master
soal/kuis daring untuk mahasiswa aktif; `UjianPMB` = penyelenggaraan ujian
saringan masuk, **tanpa satu pun soal, bobot, durasi, atau skor kelulusan**.
Javadoc class lama berbunyi `Bank generated by hbm2java` (salin-tempel
keliru dari entity `Bank`) — diganti dokumentasi sebenarnya.

**Verifikasi pola berulang:**
- Pemetaan **berbasis properti** (`@Id` di `getId()`, **nol `@Transient`**)
  → seluruh getter adalah kolom terpetakan.
- **15 getter menulis balik ke field**: `getKeterangan`,
  `getJumlahHariUjian`, `getTanggalUjian2..10` (9 buah), `getTahunAkademik`,
  `getTahun`, `getGelombangPendaftaran` (`check()`, netral),
  `getTampilkanJadwalUjianDiKartuUjian`, `getKeteranganSetelahBayar`,
  `getKeteranganHeader`, `getKeteranganSetelahBayarHeader`.
- **`getTanggalUjian2..10` MENGHAPUS data**: menurunkan `jumlahHariUjian`
  lalu sekadar *membaca* daftar sudah cukup untuk meng-`UPDATE` tanggal
  hari-hari di atasnya menjadi `NULL` secara permanen. Ini getter paling
  merusak yang ditemukan sejauh ini dalam inisiatif.
- `getNama` (trim), `getLokasi` (`"Belum ditentukan"`), `getAktif`
  (`null`→`true`) **tidak** menulis balik ke field, tetapi nilai
  penggantinya **tetap tersimpan** lewat akses properti.
- **Tidak ada getter yang membuka/menutup sesi Hibernate** — kelas ini tidak
  mengimpor `Session`/`HibernateUtil` dan tidak menjalankan query apa pun.
- **Bukan pola "flag aktif satu-arah"**: `aktif` murni dikendalikan operator
  lewat kotak centang; tidak ada logika bisnis yang menonaktifkan sendiri.

**Kuirk/temuan (dicatat, tidak diperbaiki):**
- `getTahun()` membaca **field** `tahunAkademik` langsung, bukan getter-nya
  → hasilnya bergantung urutan pemanggilan; berpotensi
  `NumberFormatException` bila isinya bukan `"NNNN/..."`; **tanpa satu pun
  pemanggil** di seluruh pohon sumber.
- `keteranganHeader` & `keteranganSetelahBayarHeader`: **tanpa pemakai sama
  sekali** (Java/ZUL/laporan), tetapi tetap kolom aktif yang nilai bawaannya
  ikut ditulis ke DB pada flush pertama. Teks bawaan keduanya sama persis.
- `toString()` memakai field `nama` **mentah** (tidak di-trim) dan **dapat
  mengembalikan `null`** — dipakai ZK combobox & `RevisiHelper`.
- `serialVersionUID` sama persis dengan `Ujian` dan `JadwalUjianPMB`.
- Auto-seed: `GelombangPendaftaran.chekKuotaPendaftar()` membuat sendiri
  `UjianPMB` bawaan `"Online"` + `RuangPMB` kapasitas 10.000 bila gelombang
  belum punya — baris bisa muncul tanpa pernah diinput operator.
- Salah ketik lama pada teks bawaan dipertahankan apa adanya:
  `"Tuilis"`, `"malakukan"`, `"2  Bukti"` (tanpa titik), dan
  `"membayar senilai Rp. ."` (nominal dibiarkan kosong).
- Kembaran hampir persis: `ais.database.model.sekolah.UjianPSB` dan
  `ais.database.model.recruitment.UjianPegawai`.

**Tidak ada kerentanan keamanan**: kelas ini tidak menjalankan query maupun
merangkai SQL; teks keterangan dirender sebagai `Label` ZK (ter-escape),
bukan `MyHtml`.

## `ais/database/model/Pengaduan.java` — SELESAI 100% (2 Sep 2026)

Entity **pengaduan/keluhan** (tabel `public.pengaduan`, `@Audited`,
`dynamicInsert/dynamicUpdate`, turunan `DataSop` → `GeneralValueObject`).
**57/57 anggota** terdokumentasi (100%), 460 → 1219 baris. Revisi **r83290**,
mirror `java/` verifikasi `cmp` identik. Hanya Javadoc/komentar; nol perubahan
logika (sumber tanpa komentar+spasi dibandingkan byte demi byte terhadap HEAD —
identik persis).

**Tiga pintu masuk:** layar ZK `PengaduanAction` (`/pages/master/pengaduan.zul`,
menu "Pengaduan-Pengaduan"/"Pengaduan Mahasiswa"/"Pengaduan Siswa") → API native
`PengaduanMahasiswaApi` (list/simpan/hapus milik mahasiswa sendiri) → webhook
WhatsApp `Wa#simpanPesan` (membuat `Tbmuser` role `pengadu` on the fly, mengisi
`req`/`res`/`tanggapan`). Penanganan lewat `DisposisiSop`; rekap di
`LaporanPengaduan`.

**Verifikasi pola berulang:**
- *Getter menulis balik ke kolom terpetakan* (ikut `UPDATE` permanen saat flush;
  **tidak ada satu pun `@Transient`**): `getNama` (judul diisi dari
  `JenisPengaduan`), `getTahun`/`getBulan` (jam sistem), `getParameterTambahan`
  + `getParameterTambahanInds` (`null` → `""`), `getSetujui` (diturunkan dari
  `DisposisiSop`, menimpa nilai manual dua arah), `getAktif` (paksa `false`
  **satu arah**), `getPegawai` (menimpa `pegawai` **dan** menulis ulang
  `diajukan`), plus 5 getter relasi ber-`check()`.
- *Getter memberi nilai pengganti TANPA menulis balik* (beda halus, dicatat
  eksplisit): `getWaktu`, `getKeterangan`, `getTanggapan`, `getKode`.
- *Getter yang menutup sesi Hibernate*: **TIDAK ADA**. Semua sesi diurus
  `GeneralValueObject.check()`. File ini juga **tidak punya satu pun method
  utilitas/query statis** — seluruh query ada di `PengaduanAction`.

**Kuirk (dicatat, tidak diperbaiki):**
- `getIndex` — `PengaduanAction.getindex()` sudah mengembalikan `rowCount+1`
  lalu renderer masih `setIndex(++currentIndex)`; kolom `index` bisa berselisih
  satu terhadap nomor yang tercetak di `kode`.
- `ambilDataParameterTambahan` — pada kolom kosong tetap mengembalikan **satu
  `CommonVO` kosong** (akibat `"".split`), tidak membaca potongan ke-5/ke-6,
  dan **tidak dipanggil dari mana pun** untuk `Pengaduan`.
- `populateParameterTambahan` — pada pengaduan yang belum tersimpan `getId()`
  masih `null` sehingga URL lampiran tersimpan kosong.
- `setDisposisiSop` — ternary setelah penjagaan awal tidak pernah lagi bercabang.

**Pengecekan privasi (khusus, hasilnya masuk Javadoc class):**
- **Tidak ada mekanisme anonimasi apa pun** — tidak ada field anonim/rahasia,
  identitas pelapor selalu tersimpan dan selalu dirender.
- **Tidak ada field "pihak yang diadukan"**, jadi tidak ada relasi langsung yang
  membocorkan pelapor ke terlapor.
- **Namun** `getPegawai()` memaksa kolom `pegawai` berisi pegawai **pelapor**,
  dan `PengaduanAction` menampilkan kendali persetujuan kepada
  `atasanlangsung`/`2`/`3` pegawai itu → aduan pegawai otomatis mendarat di meja
  atasan langsungnya, lengkap dengan identitas pelapor.
- `req` menyimpan **payload webhook WhatsApp mentah** (nomor telepon + nama
  profil pelapor tanpa penyamaran); `req`+`res` ikut daftar kolom **ekspor/impor
  massal** `PengaduanAction:243-249` (tombol Cetak/Upload).
- Isi aduan tidak pernah benar-benar hilang: `@Audited` + hapus = `aktif=false`.

## `ais/database/model/PengajuanBeasiswa.java` — SELESAI 100% (2 Sep 2026)

Entity **formulir pengajuan beasiswa oleh mahasiswa** (tabel
`public.pengajuan_beasiswa`, `@Audited`, `dynamicInsert/dynamicUpdate`, turunan
`GeneralValueObject`). **59/59 anggota** terdokumentasi (100%) plus seluruh field,
330 → 1032 baris. Revisi **r83287**, mirror `java/` verifikasi `cmp` identik.
Hanya Javadoc/komentar; nol perubahan logika (dibuktikan dengan membandingkan
sumber tanpa komentar/spasi terhadap HEAD — identik persis; baris
`@PreUpdate onUpdate()` + `tanggal_dirubah` dibiarkan menyatu seperti aslinya).

**Alur:** master `Beasiswa` bercabang **dua jalur yang tidak saling tersambung
di kode**: (1) jalur seleksi sungguhan `MahasiswaDaftarBeasiswa` (`terima`,
`totalSkor`) + `MahasiswaBeasiswaPersyaratan` → `MahasiswaDapatBeasiswa`;
(2) jalur formulir sosial-ekonomi ini (`pengajuan_beasiswa.zul` +
`PengajuanBeasiswaAction`), murni bahan pertimbangan manual panitia. Anaknya
`KeadaanKeluargaPengajuanBeasiswa` — relasi searah dari sisi anak, dan entity
anak itu **yatim di sisi kode** (tanpa DAO/Action/ZUL).

**Verifikasi pola "getter menulis balik" — DUA DITEMUKAN:**
- `getNama()` **SELALU** menimpa field dengan `mahasiswa + "-" + beasiswa`
  (bukan hanya saat `null`). Karena pemetaan property access, Hibernate memanggil
  getter ini saat `INSERT` dan saat dirty-checking → kolom `nama` selalu konvergen
  ke hasil hitungan, dan `setNama()` praktis tak berguna (nol pemanggil).
- `getTanggalPengajuan()` mengisi "sekarang" hanya saat `null`.

Keduanya kolom terpetakan sungguhan (bukan `@Transient`), jadi hasilnya tersimpan
permanen saat flush. `toString()` memanggil `getNama()` → mencetak object pun
memicu tulis balik (kontras dengan `Beasiswa.toString()` yang sengaja baca field
langsung).

**Verifikasi pola "flag `aktif` satu arah" — TIDAK BERLAKU.** Entity ini sama
sekali **tidak punya field/kolom `aktif`**; penghapusan berkas dilakukan fisik
lewat `Common.refreshDelete(...)` dari tombol Hapus di renderer. Jadi entity ini
bukan anggota kelompok "murni satu-arah" maupun "dua-arah bersyarat".

**Sesi Hibernate:** tidak ada method di kelas ini yang menyentuh `Session` —
berbeda dari `Beasiswa`, ketiga getter relasi di sini **tidak** memakai
`GeneralValueObject.check(...)`, jadi tidak ada resolusi proxy sama sekali.

**Kuirk yang dicatat (kode dibiarkan apa adanya):**
- `setMahasiswaDapatBeasiswa()` **tidak dipanggil dari mana pun** di seluruh
  pohon sumber, padahal renderer memakai relasi itu untuk kolom "Status"
  (`null` → "Belum mensetujui"). Praktisnya kolom Status **selalu** berbunyi
  "Belum mensetujui" kecuali diisi langsung lewat SQL.
- Kolom `nama` hasil hitungan justru yang membuat satu kotak pencarian
  `ilike("nama", ANYWHERE)` bisa menemukan lewat NIM / nama mahasiswa / nama
  program / nama instansi sekaligus. Efek lanjutan: nama mahasiswa berubah atau
  tanggal program disunting → sekadar membuka daftar memicu `UPDATE` + revisi
  Envers baru tanpa ada yang menekan Simpan.
- Risiko `length = 255`: gabungan `Mahasiswa.toString()` ("{id}-{nim} - {nama}")
  dan `Beasiswa.toString()` ("{nama}-{tglBuka}-{tglTutup}-{instansi}") bisa
  melewati 255 karakter → `INSERT`/`UPDATE` gagal di tingkat DB.
- Cabang penjaga `this.nama == null ? null : ...` **kode mati**: rangkaian
  `mahasiswa + "-" + beasiswa` selalu `String` (relasi kosong → `"null-null"`).
- `Order.asc("nama")` sebenarnya mengurutkan menurut **id mahasiswa sebagai
  teks** ("10-" mendahului "2-"), dan kolom grid yang memakainya berlabel
  "Beasiswa" — menyesatkan.
- `penghasilan` adalah **String kelas rentang** (5 pilihan hardcoded di Action,
  tanpa rentang di atas Rp. 5.000.000), bukan angka — tidak sebanding dengan
  `Beasiswa.penghasilanOrangTua` (`Long`). Tiga combobox lain (`rumahTinggal`,
  `peneranganRumah`, `sumberAirBersih`) juga hardcoded, bukan master data.
- Seluruh `setConstraint("no empty")` di `PengajuanBeasiswaAction.init()`
  **dikomentari** → hampir semua properti bisa tersimpan kosong; hanya
  `mahasiswa` dan `beasiswa` yang benar-benar divalidasi.
- `keterangan` tidak pernah diisi UI (warisan template generator).
- Satuan `jarakKotaKecamatan`, `jarakKampus`, `luasBangunanRumah` tidak
  didefinisikan di mana pun (label layar pun tanpa satuan).
- `DashboardStatistikPengajuanBeasiswaPerJurusan` **tidak membaca entity ini
  sama sekali** — isinya salinan dasbor KKN yang mengagregasi
  `MahasiswaDapatKelompokKkn`. Nama kelas/judulnya menyesatkan.
- `serialVersionUID` identik dengan milik `KeadaanKeluargaPengajuanBeasiswa`
  (sama-sama dari template generator).

**Temuan kerahasiaan data (BUKAN di file ini — di
`ais/action/master/PengajuanBeasiswaAction.java`, TIDAK diperbaiki):**
`initCriteria()` hanya memfilter `ilike("nama", searchnama, ANYWHERE)` **tanpa
batasan pemilik**, sementara `init()` menunjukkan layar ini memang ditujukan juga
untuk pengguna mahasiswa (`Common.getCurrentUser().getMahasiswa() != null` →
pemohon dipaksa jadi diri sendiri dan komponennya dikunci). Akibatnya setiap
pengguna dengan hak READ pada menu ini — termasuk mahasiswa — dapat melihat
seluruh berkas pengajuan mahasiswa lain beserta data pribadi keluarganya (nama
dan pekerjaan orang tua, alamat lengkap, kelas penghasilan, kondisi rumah), dan
dengan hak UPDATE/DELETE dapat menyuntingnya lewat tombol per baris. Perlu
eskalasi terpisah.

## `ais/database/model/PrestasiDosen.java` — SELESAI 100% (2 Sep 2026)

Entity **prestasi/penghargaan dosen** (tabel `public.prestasi_dosen`, `@Audited`,
`dynamicInsert/dynamicUpdate`, turunan langsung `GeneralValueObject`).
**57/57 anggota** terdokumentasi (100%), 345 → 1131 baris. Revisi **r83288**,
mirror `java/` verifikasi `cmp` identik byte. Hanya Javadoc/komentar; nol
perubahan logika (dibuktikan dengan membandingkan sumber tanpa komentar/spasi
terhadap HEAD — identik persis; baris `@PreUpdate onUpdate()` + `tanggal_dirubah`
dibiarkan menyatu seperti aslinya, Javadoc field disisipkan inline).

**Verifikasi pola berulang (lengkap):**
- **A. Getter yang memanggil `check()`** — hanya **3**: `getDosen()`,
  `getFakultas()`, `getJurusan()`. `getCabangPrestasiDosen()` dan
  `getKategoriPrestasiDosen()` **TIDAK** ikut pola ini (beda dari
  `PrestasiMahasiswa`).
- **B. Getter yang menulis balik ke kolom TERPETAKAN** — **3**:
  `getTahunAkademik()` (isi bila `null`), `getJenisSemester()` (isi bila `null`),
  `getTahun()` (**selalu** menimpa dari potongan pertama `tahunAkademik`).
  Identik dengan `PrestasiMahasiswa`.
- **C. Getter penormal yang TIDAK menulis balik** — 7: `getNama()` (trim),
  `getStatus()`, `getPrestasiLuarKampus()`, `getPeringkat()`,
  `getJumlahPeserta()`, `getCapaian()`, `getUrl()`.
- **D. Getter yang menutup session Hibernate** — **TIDAK ADA**.

**Perbandingan dengan `PrestasiMahasiswa` (sesi 11, r83225):**
- *Sama*: `serialVersionUID` identik, 4 konstanta status identik, blok jejak
  audit, `toString()` `"<id>-<nama>"`, bendera `prestasiLuarKampus` +
  `fakultas`/`jurusan`, trio periode, istilah **cabang→jenis / kategori→tingkat
  yang tertukar** (dikonfirmasi lewat `getKode()` di kedua master).
- *Beda*: 57 lawan 73 anggota; **tidak ada properti Neo Feeder** (versi dosen
  memakai integrasi **repositori DSpace** yang seluruhnya di
  `PrestasiDosenAction`, bukan properti entity); tidak ada dosen pembina,
  `jenisAktfitas`, `alamat`, no/tanggal SK; `setStatus(String)` **tanpa validasi**
  (versi mahasiswa punya whitelist 4 konstanta); cabang/kategori **eager +
  `@Fetch(FetchMode.SELECT)` tanpa `check()`** (mahasiswa: LAZY + `check()`);
  ada cache indeks JSON `prestasiDosen_<id>` di `Dosen` yang dipelihara
  `AuditListener`.

**Kuirk dicatat (tidak diperbaiki):** `getPeringkat()` tak pernah `null` sehingga
cek `== null ? "" : ...` di `PrestasiDosenAction:751` mati (layar menampilkan
"Peringkat: 0"); `nomorSertifikat` dipetakan ke `dc.identifier.issn` di DSpace;
`jumlahPeserta` bertipe `String`; dasbor rekap hanya menghitung status
`"Disetujui"`.

**Keamanan:** pola SQL injection tingkat-dua yang sama seperti temuan
`PrestasiMahasiswa` **ADA juga** di jalur dosen —
`ais/action/master/dashboard/helper/DashboardRekapPrestasiDosen.java` (±br. 221-250)
merangkai SQL mentah dan menyisipkan `generalValueObject.getNama()` (nama master
`CabangPrestasiDosen`/`KategoriPrestasiDosen`, lewat `Common.getBahasaConfig`)
sebagai alias kolom berkutip ganda, lalu dieksekusi `Common.ambilSql(sql)`.
Risiko rendah (butuh hak tulis data master), di luar entity ini; dilaporkan untuk
eskalasi terpisah, tidak diubah pada sesi ini.

## `ais/database/model/MatakuliahPrasyarat.java` — SELESAI 100% (2 Sep 2026)

Entity **aturan prasyarat mata kuliah** (tabel `public.matakuliah_prasyarat`,
`@Entity`, `@Audited`, `dynamicInsert/dynamicUpdate`, turunan
`GeneralValueObject`). **63/63 anggota** terdokumentasi (100%: 45
method/konstruktor + 18 field), 281 → 818 baris. Revisi **r83286**, mirror
`java/` verifikasi `cmp` identik. Hanya Javadoc/komentar; nol perubahan logika
(dibuktikan dengan membandingkan sumber tanpa komentar/spasi terhadap HEAD —
identik, kecuali pemisahan baris gabungan `@PreUpdate onUpdate()` +
`tanggal_dirubah` menjadi dua baris, murni whitespace, mengikuti gaya
`Matakuliah.java`).

### Pertanyaan terbuka sesi 10 TERJAWAB

Sesi 10 (`Matakuliah.java`, r83072-83087) menemukan bahwa relasi prasyarat
TIDAK tersimpan sebagai field di `Matakuliah`. **Terkonfirmasi: file inilah
pemiliknya.** Tidak ada sisi terbalik (`inverse`) yang dipetakan sama sekali —
`Matakuliah` tidak punya `getMatakuliahPrasyarats()` dan memang tidak pernah
punya. Satu-satunya jalan menemukan prasyarat sebuah mata kuliah adalah query
Criteria eksplisit `Restrictions.eq("matakuliah", matakuliah)` yang digabung
`Restrictions.or(isNull("aktif"), eq("aktif", true))`.

### Struktur aturan: OR di dalam baris, AND antar baris

- Satu baris = satu "kelompok syarat" untuk satu `matakuliah` (sisi kiri,
  `nullable = false`).
- Dalam satu baris ada **10 slot** `matakuliahPrasyarat`..`matakuliahPrasyarat10`
  yang bersifat **alternatif (OR)** — baris terpenuhi bila salah satu slot lulus
  (pesan validasi memakai kata "atau" untuk slot 2 ke atas).
- Satu mata kuliah boleh punya **banyak baris**, dan semuanya harus terpenuhi →
  **AND antar baris**.
- Tiga jenis syarat bisa dicampur dalam satu baris: (1) kelulusan MK slot +
  `minimalNilaiLulus` vs `Detailperkuliahan.getTotalNilai()` (hanya yang
  `persetujuan == DISETUJUI`, `matakuliahKonversi` diutamakan di atas
  `perkuliahan.matakuliah`); (2) `minimalSks` vs `KrsMahasiswa.getSksk()`;
  (3) `minimalIpk` vs `KrsMahasiswa.getIpk()`.
- `hanyaBerdasarkanKode` (default **true**) memilih pencocokan riwayat kuliah
  by **kode** MK (semua baris `Matakuliah` berkode sama diakui — penting karena
  revisi kurikulum melahirkan baris MK baru berkode sama) vs by **id** (ketat,
  bisa menahan mahasiswa angkatan lama).

### Mesin validasi tunggal

Semua aturan dievaluasi **satu** method:
`CommonAcademicSyncHelper.checkMatakuliahPrasyarat(Matakuliah, Mahasiswa, Integer)`
lewat fasad `Common.checkMatakuliahPrasyarat(...)`. **Menampilkan messagebox ZK
saat gagal**, jadi hanya boleh dipanggil dari event thread UI. Tujuh pemanggil,
semuanya jalur pengisian KRS: `AmbilDataPerkuliahanHelper`,
`AmbilDataPerkuliahanNonPaketHelper`, `AmbilDataMahasiswaHelper`,
`AmbilDataMahasiswaForPaketPerkuliahanHelper`, `AmbilDataPaketPerkuliahanHelper`,
`AmbilDataKurikulumPerkuliahanHelper`, `GenerateKRSPaketMahasiswaOtomatisWindow`.

### Verifikasi pola berulang

- **Getter menulis balik ke field**: 11 getter relasi (`getMatakuliah` +
  `getMatakuliahPrasyarat`..`10`) melakukan `x = check(x)` — resolusi proxy lazy
  dengan tulis balik. `getAktif()` menulis balik `true` saat field `null`;
  `aktif` adalah properti terpetakan sungguhan (**bukan** `@Transient`) dan
  pemetaan memakai *property access*, jadi hasil logika getter itulah yang
  tertulis ke DB. Justru karena baris warisan di DB masih bisa `NULL`, query
  mesin validasi sengaja memakai `isNull("aktif") OR aktif = true`. Konsisten
  dengan kesimpulan pola "flag aktif" batch akunting.
- `getMinimalNilaiLulus`/`getMinimalSks`/`getMinimalIpk`/`getHanyaBerdasarkanKode`
  menormalkan `null` **hanya pada nilai kembalian**, tidak menugaskan balik ke
  field (beda dengan `getAktif`).
- `toString()` **bukan operasi baca murni** — memanggil dua getter relasi lalu
  menugaskan hasilnya ke field.
- **Getter yang menutup Session Hibernate: TIDAK ADA** di kelas ini (beda dengan
  `Matakuliah.reInitEkivalen()`/`ambilEkivalen()`). Satu-satunya jalur yang bisa
  membuka session sendiri adalah `check()` milik kelas induk, dan itu ditutupnya
  sendiri.
- Field `id`/`oleh`/`olehId`/`tanggal_dirubah` yang dideklarasikan ULANG
  didokumentasikan sebagai **keharusan teknis** (`GeneralValueObject` bukan
  `@Entity`/`@MappedSuperclass`), bukan duplikasi yang lupa dibersihkan.
- Kolom tanpa `@Column` (`minimalNilaiLulus`, `minimalSks`, `minimalIpk`,
  `hanyaBerdasarkanKode`, `aktif`, `keterangan`, `tanggal_dirubah`) jatuh ke
  `MyNamingStrategy` (turunan `DefaultNamingStrategy`: nama kolom = nama
  properti apa adanya, camelCase tidak dikonversi).

### Kuirk dicatat apa adanya (TIDAK diperbaiki)

1. **Slot 1 null mematikan slot 2..10.** Mesin validasi `continue` bila
   `getMatakuliahPrasyarat() == null`, jadi baris yang slot 1-nya kosong tetapi
   slot 2..10 terisi **tidak diperiksa sama sekali**. Perangkap data-entry nyata.
2. **Ambang penyaring IPK `> 0.01`, bukan `> 0`.** Syarat `minimalIpk` bernilai
   0.01 ke bawah diabaikan diam-diam.
3. **Urutan deklarasi**: pasangan getter/setter slot **5 berada sebelum slot 4**
   di berkas sumber. Tidak berpengaruh pada pemetaan, tapi membingungkan.
4. `toString()` hanya menampilkan slot 1; slot 2..10 tak pernah muncul. Bila
   slot 1 kosong, hasilnya berakhir dengan literal `"null"`.
5. Pencocokan by kode **tidak menyaring prodi/kurikulum** — kode yang kebetulan
   sama antar prodi saling diakui.
6. Batas 10 alternatif bersifat **keras** (slot = kolom, bukan koleksi).

**Tidak ada temuan keamanan** — seluruh akses DB lewat Criteria API
terparameterisasi.

## Batch "kluster akunting — persetujuan dana" — SELESAI 100% (2 Sep 2026, dikonsolidasi orkestrator, sempat kena rate limit & dilanjutkan)

Semua 5 file TUNTAS 100% method, dikompilasi, dikommit, di-mirror ke `java/`.
**Catatan proses**: 3 agent (KasBesar/PenggantianKasKecil/KasKecil) sempat
terkena rate limit API di tengah jalan TAPI sudah sempat commit sebelum
terputus (tidak ada kerja hilang); 2 agent lain (DanaTalangan,
DaftarPengajuanTransfer) dilanjutkan lewat `SendMessage` resume setelah
limit reset dan berhasil tuntas.

- `KasKecil.java` — 64/64. 504→1348 baris. r83243.
- `KasBesar.java` — 63/63. 521→1403 baris. r83248.
- `DanaTalangan.java` — 53/53. 428→1362 baris. r83251.
- `PenggantianKasKecil.java` — 55/55. 442→1250 baris. r83244.
- `DaftarPengajuanTransfer.java` — 103/103. 2161→3647 baris. 9 commit
  bertahap (r83242-r83271).

### Kesimpulan LENGKAP pola "flag aktif satu-arah" (5 file + `PengajuanMahasiswa` sesi 11)

**Terbelah dua kelompok, bukan satu pola seragam seperti dugaan awal:**
- **Murni satu-arah** (`false` permanen, TIDAK PERNAH ada jalur pemulih ke
  `true` di dalam class): `KasKecil`, `KasBesar`, `DaftarPengajuanTransfer`
  (+ `PengajuanMahasiswa` dari sesi 11). `DaftarPengajuanTransfer` punya SATU
  pengecualian sempit dari LUAR class (`BreakdownTagihanVendorHelper`, hanya
  untuk baris tipe `Pajak`).
- **Dua-arah bersyarat** (ADA blok pemulih `aktif=true` saat status mencapai
  "Disetujui", TAPI sebelum status itu tercapai tetap satu-arah seperti
  kelompok pertama): `DanaTalangan`, `PenggantianKasKecil`. Konsekuensi nyata
  untuk `DanaTalangan`: penonaktifan manual operator DIBATALKAN DIAM-DIAM
  begitu dokumen disetujui.

Semua varian sepakat pada SATU hal: kolom `aktif` adalah properti terpetakan
sungguhan (bukan `@Transient`), jadi nilai hasil logika getter SELALU
ikut ter-`UPDATE` permanen ke DB saat entity di-flush — baik versi
satu-arah maupun dua-arah bersyarat.

**Tidak ada eskalasi keamanan baru** dari batch ini (dicek eksplisit tiap
file, semua akses DB lewat Criteria API terparameterisasi).

**Total akumulasi 12 sesi kerja**: 233 (sesi 1-11) + 5 = **238 file** dari
7.401 (~3,2%).

## `ais/database/model/akunting/KasKecil.java` — SELESAI 100% (2 Sep 2026)

Entity **pengajuan kas kecil** (tabel `akunting.kas_kecil`, `@Audited`,
`dynamicInsert/dynamicUpdate`, turunan `DataSop` → `GeneralValueObject`).
**64/64 anggota** terdokumentasi (100%), 504 → 1348 baris. Revisi **r83243**,
mirror `java/` verifikasi `cmp` identik. Hanya Javadoc/komentar; nol perubahan
logika (dibuktikan dengan membandingkan sumber tanpa komentar/spasi terhadap
HEAD — identik persis, baris `@PreUpdate onUpdate()` + `tanggal_dirubah`
dibiarkan menyatu seperti aslinya).

**Alur:** master `JenisKasKecil` (dompet: saldo awal, akun kas kecil, akun
penutup) → pengajuan `KasKecil` (nilai, keperluan, `formula` JSON berisi
banyak baris debet) → disposisi/persetujuan SOP → `PenggantianKasKecil`
(pengisian ulang) / `KasBesar` (sumber dana) → posting jurnal
`PostingKasKecilAction` → `PostingHistory`.

**Verifikasi pola "flag `aktif` satu arah" — IDENTIK** dengan
`PengajuanMahasiswa` (r83227), baris demi baris. `getAktif()` memaksa
`aktif = false` bila disposisi nonaktif atau alur berhenti di simpul
penolakan, dan tidak pernah mengembalikannya ke `true`; `aktif` properti
terpetakan sehingga nilai itu ikut ter-flush ke DB saat membaca daftar.
**Konsekuensi khusus kas kecil (lebih tajam daripada di
`PengajuanMahasiswa`):** `JenisKasKecilAction.hitungSaldo(...)` hanya
menjumlahkan `nilai` dari baris `aktif IS NULL OR aktif = true`, jadi sekali
pengajuan dipaksa nonaktif, nilainya **berhenti mengurangi saldo dompet secara
permanen** meski penolakannya kemudian dicabut.

**Pola berulang lain:** 14 getter menulis balik ke properti terpetakan
(`getAktif`, `getJenisKasKecil`, `getDibuatOleh`, `getDisetujuiOleh`,
`getTanggalPersetujuan`, `getStatus`, `getSatuanKerja`, `getKodeUnik`,
`getDisposisiSop`, `getTahun`, `getBulan`, `getNomorSuratAlurKeuangan`,
`getTanggalTransaksi`, `getSisa`) — tiga di antaranya menulis **tanpa syarat**
tiap pembacaan (`getKodeUnik`, `getTanggalTransaksi`, `getSisa`). **Tidak ada**
getter yang membuka/menutup session Hibernate langsung di file ini; biaya itu
hanya muncul lewat `check(...)` milik `GeneralValueObject`.
`getTanggalPersetujuan()` menelan `LazyInitializationException` lewat
`try/catch` bertanda `auto-audit(empty-catch)`.

**Kuirk/bug dicatat (tidak diperbaiki):**
- `getDisetujuiOleh()`/`getTanggalPersetujuan()` diakhiri blok yang memaksa
  `null` bila disposisi ada tetapi belum punya langkah "setuju" — **menimpa**
  hasil pencarian `kasBesar`/`penggantianKasKecil` di atasnya, sehingga cabang
  itu efektif hanya berlaku bagi pengajuan tanpa disposisi.
- `setDisposisiSop()` mengandung ternary **kode mati** (kondisinya mustahil
  benar setelah guard di barisnya sendiri) → efektif penugasan biasa.
- `getKodeUnik()` derivatif tapi dipetakan `@Column(unique = true)`; bila
  `kode` masih `null` hasilnya string harfiah `"null_<id>"`, dan dua baris
  tanpa kode maupun id bisa sama-sama menghasilkan `"null_null"` →
  berpotensi menabrak batasan unik.
- `toString()` membaca field `nama` mentah (bukan `getNama()`) → bisa `null`.
- `DEFAULT_FORMULA` `public static` tapi **tidak** `final`.
- `getSisa()` = snapshot `saldo` − `nilai`; karena `saldo` hanya snapshot saat
  penyimpanan, `sisa` ikut basi bila saldo dompet berubah setelahnya — padahal
  nilai ini dipakai sebagai nominal debet akun penutup saat posting.

Tidak ditemukan kerentanan keamanan di file ini.

## Batch "5 entity prestasi/beasiswa/pengajuan/kalender" — SELESAI 100% (2 Sep 2026, dikonsolidasi orkestrator)

Semua 5 file TUNTAS 100% method, dikompilasi, dikommit, di-mirror ke `java/`:
- `PrestasiMahasiswa.java` — 73/73. 457→1306 baris. r83225/83226. SQL
  injection tingkat-dua berisiko RENDAH ditemukan di
  `DashboardRekapPrestasiMahasiswa.java` (butuh akses tulis data master
  dulu) — TIDAK dieskalasi terpisah (severity di bawah ambang task baru).
- `Beasiswa.java` — 56/56. 354→1136 baris. r83229. Nama field `bolehGanda`
  maknanya TERBALIK (true = tidak boleh ganda). Kemungkinan bug arah
  perbandingan syarat penghasilan ortu (kandidat &gt;batas malah lolos).
- `PengajuanMahasiswa.java` — 60/60. 499→1340 baris. r83227. Entity GENERIK
  lintas-jenis (bukan spesifik beasiswa). Pola "flag `aktif` satu-arah"
  dikonfirmasi berulang di ≥5 class sejenis (`KasKecil`/`KasBesar`/
  `DanaTalangan`/`PenggantianKasKecil`/`DaftarPengajuanTransfer` — kandidat
  leverage utk sesi mendatang, semua kemungkinan share pola sama).
- `KalenderAkademik.java` — 53/53. 379→1258 baris. r83228. Bug crash:
  `getHari()` index array pakai `Calendar.WEEK_OF_MONTH` bukan
  `DAY_OF_WEEK` → `ArrayIndexOutOfBoundsException` bisa terjadi. Melengkapi
  pemahaman 2 jalur gerbang waktu `Konfigurasi` (relasi langsung +
  `KonfigurasiKalenderAkademikProcessor`).
- `TunggakanMahasiswaDetail.java` — 43/43. 286→912 baris. r83221/83224.
  BUKAN bagian rantai transaksi billing (snapshot pelaporan terpisah,
  fotokopi `DetailBiaya`). Bug logika lucu: `compareTo()` panggil `.trim()`
  SEBELUM cek null-nya sendiri → NPE yang harusnya dicegah malah terpicu.

**Total akumulasi 11 sesi kerja**: 228 (sesi 1-10) + 5 = **233 file** dari
7.401 (~3,1%). 3 task eskalasi aktif tetap: `task_15f5001e`, `task_b0a90191`,
`task_78a5b1ab`.

## `ais/database/model/KalenderAkademik.java` — SELESAI 100% (2 Sep 2026)

Entity **kalender akademik** (tabel `public.kalender_akademik`, `@Audited`,
`dynamicInsert/dynamicUpdate`). **53/53 anggota** terdokumentasi (100%),
379 → 1258 baris. Revisi **r83228**, mirror `java/` verifikasi `cmp` identik.
Hanya Javadoc/komentar; nol perubahan logika (dibuktikan dengan membandingkan
sumber tanpa komentar/spasi — satu-satunya beda adalah pemecahan baris
deklarasi `tanggal_dirubah` dari baris `onUpdate()`, murni tata letak).

**Peran ganda entity (yang kedua sering terlewat):** (1) kalender informatif
untuk grid/dasbor/laporan, dan (2) **gerbang waktu untuk `Konfigurasi`**. Ada
DUA jalur gerbang yang keduanya masih hidup:
- **Jalur A** — relasi langsung `Konfigurasi.kalenderAkademik` (`@ManyToOne`).
  Langkah 3 `Konfigurasi.getNilai()` **menimpa** nilai jadi `AKTIF`/`TIDAK_AKTIF`
  murni dari rentang tanggal, lalu menyalin `tahunAjaran` → `tahunAkademik` dan
  `ganjilGenap` → `info1`. Kolom `nilai` jadi tak relevan begitu kalender ditaut.
- **Jalur B** — tabel penghubung `KonfigurasiKalenderAkademik` +
  `KonfigurasiManager.fetchKonfigurasiKalender` (dibungkus
  `Common.checkKonfigurasiDenganKalenderAkademik[Aktif]`), dipakai `KrsHelper`,
  `KrsPaketHelper`, `KrsNonPaketHelper`, `KrsKurikulumHelper`, `AngketUtil`,
  `ChecklistPenilaianDosenOlehMhsAction`/`GuruOlehMhsAction`. Plus penjadwal
  latar `KonfigurasiKalenderAkademikProcessor` (`TimerTask`).

Semantik ruang lingkup: **`NULL` = "berlaku untuk semua"** (UI: "Semua"). Dua
mode saling eksklusif: fakultas/jurusan/jenjang/program (PT) vs yayasan/sekolah.

**Verifikasi pola "getter yang menulis balik" — 8 getter, mayoritas bukan getter
polos:** `getTanggalMulai`, `getTanggalSelesai` (isi hari ini bila `null`),
`getTahunAjaran`, `getFakultas`, `getJenjang`, `getHari`, `getStatus`,
`getWarna`, `getJumlahHari`. Tidak ada getter yang membuka/menutup sesi
Hibernate sendiri di kelas ini — penutupan sesi tersembunyi ada di
`GeneralValueObject.check()` (tahap 3 reload), yang dipanggil 5 getter relasi
(`jenisKegiatan`, `jurusan`, `fakultas`, `jenjang`, `yayasan`, `sekolah`).
Getter yang **tidak** menulis balik (pengecualian menarik): `getGanjilGenap`,
`getProgram`, `getMasukDiSmt`, `getAktif` — normalisasi/default baca-saja.

**Temuan (dicatat, TIDAK diperbaiki):**
1. **`getHari()` salah tulis konstanta** — mengindeks `Common.haris` (larik nama
   HARI) dengan `Calendar.WEEK_OF_MONTH`, bukan `DAY_OF_WEEK`. Nilainya selalu
   salah, `"Sabtu"` **tak pernah** bisa muncul (WEEK_OF_MONTH maks 6 → indeks 5),
   dan bila `WEEK_OF_MONTH == 0` indeksnya `-1` →
   **`ArrayIndexOutOfBoundsException`**. `DasbordKalenderAkademik` membungkus
   pembacaan turunan dengan `try/catch`, pemanggil lain tidak.
2. **Penyempitan ruang lingkup diam-diam** — `getFakultas()` dan `getJenjang()`
   menimpa nilai tersimpan dengan turunan dari `getJurusan()`. Baris ber-lingkup
   "Semua fakultas"/"Semua jenjang" berubah jadi spesifik **hanya dengan
   dibaca**, lalu ikut ter-`UPDATE` pada `flush` berikutnya — padahal query
   gerbang menyaring `fakultas IS NULL OR = ?` / `jenjang IS NULL OR = ?`.
   Pola sekeluarga dengan `JenjangProgramStudi.getNama()` (sesi batch 5 entity).
3. **`getTahunAjaran()` mematikan opsi "Semua tahun ajaran"** — mengisi
   `Common.getCurrentTahunAkademik()` ke baris yang kosong dan menulisnya balik.
   Akibat lanjutan: cabang `"Semua"` di `KalenderAkademikAction` (uji
   `getTahunAjaran()` kosong) praktis **dead code**.
4. **Empat properti turunan ternyata PERSISTEN** — `hari`, `status`, `warna`,
   `jumlahHari` tidak diberi `@Transient`; dengan property-access +
   `hbm2ddl.auto=update`, kolomnya nyata dan ikut diaudit Envers. Kolom `warna`
   benar-benar menyimpan teks CSS `"background-color: rgba(252, 214, 202,0.4)"`.
   Nilai tersimpan di keempat kolom itu hanyalah sisa render terakhir.
5. **Centang "Aktif" tidak menggerbang apa pun** — `getAktif()` hanya dibaca
   satu tempat (checkbox grid `KalenderAkademikAction:457`); tidak ada
   `Restrictions` atas kolom ini di `KonfigurasiManager`, dan
   `Konfigurasi.getNilai()` pun tak melihatnya. **Melepas centang tidak
   menonaktifkan kalender** — satu-satunya cara adalah mengubah tanggal atau
   memutus tautan konfigurasi.
6. **Tiga implementasi "hari ini di dalam rentang"** dengan perlakuan batas hari
   berbeda: `getStatus()`/`getWarna()` (geser −1 hari pada dua acuan sekaligus,
   efektif inklusif), `Konfigurasi.getNilai()` (banding string `dateFormat1`),
   `KonfigurasiManager` (awal/akhir hari di SQL). Sumber ketidaksinkronan label
   layar vs perilaku fitur di sekitar tengah malam.
7. Kolom salah eja `descripsi_kegiatan_akademik`; `toString()` membaca **field**
   langsung (bukan getter) sehingga bisa `"null_null"` dan tidak layak tampil;
   `ditetapkanOleh` kolomnya `nullable = false` tetapi UI tidak mewajibkannya;
   `getProgram()` merapikan spasi hanya saat dibaca sehingga nilai `" "`
   tersimpan gagal cocok di query gerbang meski terlihat "Semua" di layar;
   `getMasukDiSmt()` memakai `isEmpty()` (bukan `trim().isEmpty()`) sehingga
   `" "` tidak dinormalkan. `setYayasan`/`setSekolah` membuang object tanpa ID.

**Tidak ditemukan kerentanan keamanan** di berkas ini (tidak ada SQL mentah,
tidak ada kredensial, tidak ada I/O). Temuan di atas bersifat korupsi/kehilangan
data dan salah-gerbang fungsional, bukan keamanan.

## `ais/database/model/PrestasiMahasiswa.java` — SELESAI 100% (2 Sep 2026)

Entity **prestasi/kejuaraan mahasiswa** (tabel `public.prestasi_mahasiswa`,
`@Audited`, `dynamicInsert/dynamicUpdate`). **73/73 anggota** terdokumentasi
(100%), 457 → 1306 baris. Revisi **r83225**, mirror `java/` verifikasi `cmp`
identik byte. Hanya Javadoc/komentar; nol perubahan logika (diverifikasi lewat
diff atas berkas yang sudah dibuang seluruh komentarnya).

**Struktur:** tidak ada satu pun method statis/query/helper — 73 anggota semuanya
instance (8 pasang getter/setter relasi `@ManyToOne` lazy, sisanya properti
skalar + jejak audit + `toString()` + constructor). Pengambilan data dikerjakan
`PrestasiMahasiswaAction`, dasbor `Dashboard*Prestasi*`, laporan akreditasi/SAPTO,
dan `FeederExporter`.

**Verifikasi pola "getter yang menulis" — 11 getter, tiga kelompok berbeda:**
- **Menulis ke field relasi (`field = check(field)`)**: 8 getter (`mahasiswa`,
  `dosenPembina1/2`, `fakultas`, `jurusan`, `kategoriPrestasiMahasiswa`,
  `cabangPrestasiMahasiswa`, `jenisAktfitasMahasiswa`) — pola baku, hanya
  mengganti referensi object.
- **Menulis ke field TERPETAKAN (berbahaya)**: `getTahunAkademik()` dan
  `getJenisSemester()` mengisi field bila `null` (nilainya mengikuti tahun
  akademik/semester **saat dibaca**, bukan saat prestasi diraih);
  `getTahun()` **selalu** menimpa `tahun` dari `tahunAkademik.split("/")[0]`
  setiap kali dipanggil. Pada entity *managed*, membaca saja bisa memicu
  `UPDATE` pada `flush` berikutnya tanpa aksi simpan pengguna.
- **Menormalkan TANPA menulis balik**: `getNama()` (trim), `getStatus()`
  (default `BELUM_DIPROSES`), `getPrestasiLuarKampus()` (default `true`),
  `getPeringkat()` (default `0`), `getJumlahPeserta()`/`getCapaian()`/
  `getUrl()` (default `""`), `getFeeder()` (trim + `""`→`null`). Akibatnya
  tampilan layar bisa berbeda dari isi kolom database.
- **Getter yang menutup session Hibernate: TIDAK ADA** — file ini tidak
  menyentuh `HibernateUtil`/transaksi sama sekali; risiko session hanya
  tidak langsung lewat tahap penyelamat `check()` di induk.

**Kuirk/temuan (dicatat, tidak diperbaiki):**
- Javadoc class lama berbunyi *"Bank generated by hbm2java"* — salah salin dari
  entity `Bank`, tidak pernah menggambarkan class ini. Dimutakhirkan.
- `setStatus()` "validasi ketat" tapi **senyap**: nilai apa pun di luar 4
  konstanta (termasuk `null`, beda kapitalisasi, status hasil impor) dipaksa
  menjadi `BELUM_DIPROSES` tanpa exception — bisa **memundurkan** prestasi yang
  sudah `DISETUJUI`. Komentar di dalam badan method mencatat alternatif
  `IllegalArgumentException` yang sengaja tidak dipakai.
- `getStatus()` menormalkan `null`→`BELUM_DIPROSES` **tanpa** menulis balik, jadi
  kueri SQL langsung `status = 'Belum diproses'` melewatkan baris lama ber-`null`.
- `getPeringkat()` tidak pernah `null`, tetapi `PrestasiMahasiswaAction:1042`
  memeriksa `getPeringkat() == null` — cabang yang **tidak pernah benar**,
  peringkat kosong tetap tampil `0`. Nilai `0` juga terkirim mentah ke feeder.
- `getJenisAktfitasMahasiswa()` fallback ke `ConstantValues.KOMPETENSI`, yang
  ternyata diisi `InitDataHelper` dari master bernama **"Kompetisi"** — nama
  konstanta ≠ data yang diwakilinya. Konstanta itu bisa masih `null` pada
  instalasi yang belum pernah menyinkronkan feeder.
- Properti `feeder` di entity ini adalah **string JSON peta** `{"<idJurusan>":
  "<id_aktivitas>"}` (satu prestasi bisa dilaporkan ke >1 prodi), **berbeda
  bentuk** dari `feeder` pada entity master (`CabangPrestasiMahasiswa`,
  `KategoriPrestasiMahasiswa`) yang berisi satu id polos. `feederPrestasi`
  barulah id polos (`id_prestasi`).
- Nama kolom DB salah ketik: `dosen_pmbina1`/`dosen_pmbina2` (bukan "pembina").
- `getKeterangan()` di sini **tidak** meniru induk (induk mengubah `null`→`""`),
  jadi perilaku berbeda tergantung tipe statis variabel pemanggil.
- `toString()` memakai field `nama`/`id` langsung (tanpa `trim`), dan pada baris
  belum tersimpan menghasilkan awalan `"null-"`.
- `nomorSertifikat` tanpa cek keunikan; tidak ada validasi silang apa pun di
  entity (`tanggalSelesai` boleh mendahului `tanggal`; `fakultas`/`jurusan`
  boleh tetap terisi walau ditandai prestasi luar kampus — form hanya
  menyembunyikan barisnya, tidak mengosongkan nilainya). Semua validasi ada di
  `PrestasiMahasiswaAction.onSave(...)`, sehingga jalur impor feeder/batch
  melewatinya seluruhnya.
- Status `DISETUJUI` bukan sekadar label: ia menyembunyikan tombol ubah/hapus
  baris **dan** memunculkan tombol kirim ke Neo Feeder.
- Catatan lintas berkas (bukan file ini):
  `ais/action/master/dashboard/helper/DashboardRekapPrestasiMahasiswa.java`
  menyusun SQL dengan merangkai `getNama()` master cabang/kategori ke dalam
  alias kolom berkutip ganda. Nilai berasal dari data master (butuh akses tulis
  master untuk dieksploitasi), jadi risikonya rendah, tetapi ini SQL injeksi
  tingkat kedua yang nyata bila master data bisa diisi pengguna non-tepercaya.

## `ais/database/model/TunggakanMahasiswaDetail.java` — SELESAI 100% (2 Sep 2026)

Baris **rincian tunggakan** milik satu `TunggakanMahasiswa` (tabel
`public.tunggakan_mahasiswa_detail`, `@Audited`, `dynamicInsert/dynamicUpdate`).
**43/43 anggota** terdokumentasi (2 konstruktor + 41 method) + seluruh field.
286 → 912 baris. Revisi **r83221**, mirror `java/` verifikasi `cmp` identik.
Hanya Javadoc/komentar; nol perubahan logika.

**Posisi dalam rantai billing (diverifikasi dari kode pemanggil):** entity ini
**BUKAN** mata rantai `ItemBiaya → SettingBiaya/DetailSettingBiaya → DetailBiaya
→ DetailKegiatan → CicilanPembayaran/BuktiPembayaran`, melainkan **snapshot
pelaporan** di sampingnya. Satu baris = satu `DetailBiaya` yang **difoto**
(didenormalisasi) saat penyapuan tunggakan, supaya laporan tetap bisa
menampilkan rincian walau master biaya berubah. Judul Javadoc generate aslinya
memang berbunyi *"DetailBiaya generated by hbm2java"* — class ini hasil
salin-tempel `DetailBiaya`, bahkan `serialVersionUID`-nya identik.

- **Tidak punya relasi ke `Mahasiswa`** sama sekali; identitas mahasiswa hanya
  lewat induk `TunggakanMahasiswa.getMahasiswa()`.
- **Tidak punya** jatuh tempo, tanggal tagihan, status lunas, maupun nilai
  terbayar. Semua itu di `DetailBiaya` (`defaultTanggalTagihan`/
  `defaultTanggalDeadline`) dan di induk (`dianggapLunas`/`jumlahTunggakan`).
- Penulis satu-satunya: `TunggakanMahasiswaBaruProcessor` dan
  `TunggakanMahasiswaDaftarUlangProcessor` — dua salinan kode kembar
  `insertTunggakanMahasiswaDetail(...)`.
- Pembaca: drill-down `TunggakanMahasiswaAction` ("Informasi Detail Biaya
  Tagihan") dan `NewUiStudentArrearsService#details(Long)` (daftar `charges`).

**Kuirk/temuan (dicatat, TIDAK diperbaiki):**

1. **Nominal rincian ≠ nominal induk.** Detail menyimpan
   `detailBiaya.getNilaiBiaya()` **bruto apa adanya**, sedangkan
   `TunggakanMahasiswa.jumlahTunggakan` dihitung dengan
   `Kegiatan.ambilJumlahTagihan(...)` yang menghormati diskon, `nilaiBiayaBaru`
   (hasil perkalian SKS/matakuliah), `tunggakanLalu`, dan bendera
   `bukanTagihan`. Item `bukanTagihan` tetap tertulis di rincian dengan
   nominalnya padahal kontribusinya ke total = 0. Jejaknya masih ada di UI:
   `TunggakanMahasiswaAction` menjumlahkan `getNilaiBiaya()` ke `totalBiaya`,
   tapi baris `tunggakanMahasiswa.setJumlahTunggakan(totalBiaya)` sudah
   **dikomentari** — kemungkinan justru karena penjumlahan bruto pernah merusak
   angka tunggakan.
2. **Siklus hidup hapus-total-tulis-ulang lewat SQL native**
   (`delete from tunggakan_mahasiswa_detail where tunggakan_mahasiswa = <id>`):
   id baris **tidak stabil**, cache Hibernate L1/L2 jadi basi, dan penghapusan
   **tidak tercatat Envers** walau class `@Audited` → jejak audit berlubang.
   Penulisan ulang hanya terjadi bila flag `bersihkanDuluDetail` menyala pada
   jalur "tunggakan sudah ada" → induk bisa mutakhir sementara rincian basi.
3. **Ketidakcocokan panjang kolom `nama`:** di sini `length = 50`
   `nullable = false`, sumbernya `DetailBiaya.nama` `length = 255` dan dirakit
   dinamis (`"<nama item biaya> ke-<n>"`). Nama item panjang → penyisipan gagal
   di level DB, ditelan `catch` prosesor, gejalanya rincian diam-diam kosong.
4. **`compareTo()` cacat rangkap tiga:** (a) urutan **TERBALIK**
   (`o.kode.compareTo(this.kode)`); (b) dua pemeriksaan
   `...getKode().trim() != null` **selalu true** (`trim()` tak pernah `null`) —
   `.trim()` dipanggil SEBELUM cek, jadi `getKode()` bernilai `null` justru
   menimbulkan **NPE tepat di baris pemeriksaan itu**, bukan tersaring;
   (c) `return 0` untuk kasus tak terbandingkan → di `TreeSet`/`TreeMap` baris
   saling menggusur. Praktis tak berpengaruh ke tampilan karena kedua pembaca
   mengurutkan berdasarkan `nama` (`Order.asc("nama")`), bukan kode.
5. `toString()` mengembalikan field `nama` mentah → **bisa `null`**.
6. Konstruktor `TunggakanMahasiswaDetail(String tahunAkademik)` **tidak dipakai
   di mana pun** dan menghasilkan obyek tak sah disimpan (`nama` NOT NULL).
7. Belasan field `DetailBiaya` **tidak ikut difoto** (`nilaiBiayaBaru`,
   `tunggakanLalu`, `paket`, `gelombangPendaftaran`, `bayarKe`, `kelas`,
   `keterangan`, `defaultTanggalTagihan`/`Deadline`, `aktif`, dst.) → rincian
   **tidak bisa merekonstruksi** angka tagihan riil. `jenisBiaya` masih tersisa
   sebagai blok kode dikomentari di tengah file.
8. Relasi `tunggakanMahasiswa` `nullable = true` tanpa constraint → baris yatim
   tak akan pernah tersapu (penghapusan native memfilter per id induk).

**Verifikasi pola "getter yang menulis" — class ini BERSIH.** Dari 20 getter
hanya `getJenjang()` yang menulis, dan **hanya ke field miliknya sendiri**
(`jenjang = check(jenjang)`, resolusi proxy lazy baku — satu-satunya relasi
`FetchType.LAZY` di file ini). **TIDAK ADA** getter yang menulis ke object LAIN
(kontras dengan `CicilanPembayaran`/`DetailKegiatan`), **tidak ada** getter yang
menjalankan query, **tidak ada** getter yang membuka/menutup session Hibernate.
Efek samping justru ada di *setter*: `setOleh()`/`setOlehId()` mengabaikan nilai
`null`/kosong diam-diam (pola seragam entity AIS).

**Tidak ditemukan kerentanan keamanan** pada entity ini. Catatan: kedua prosesor
pemanggil merakit SQL native lewat konkatenasi `tunggakanMahasiswa.getId()` —
`Long` hasil generate DB, bukan input pengguna, jadi bukan SQL injection.


## Batch "5 entity kurikulum/wisuda/billing/keamanan" — SELESAI 100% (2 Sep 2026, dikonsolidasi orkestrator)

Semua 5 file TUNTAS 100% method, dikompilasi, dikommit, di-mirror ke `java/`:
- `Kurikulum.java` — 54/54 method. 392→1093 baris. r83191 (+r83204 perbaikan
  EOL susulan, CRLF sempat tak sengaja jadi LF, sudah dibetulkan).
- `DetailKegiatan.java` — 68/68 method. 987→2096 baris. r83201/83205.
  Konfirmasi lagi pola "getter menulis ke object LAIN": `getBiaya()` bisa
  mengubah baris master `DetailBiaya` yang dipakai BANYAK mahasiswa
  sekaligus. Bug kehilangan data: `getDiskonMahasiswaData()`/`2`/`3` bisa
  menghapus tautan diskon permanen hanya dengan dibaca.
- `PendaftaranWisuda.java` — 59/59 anggota. 346→1000 baris. r83192/83194/83196.
  Konfirmasi+perluas temuan sesi 6 (no. registrasi=no. kursi dari `getId()`)
  — plus risiko NPE dan nomor tak pernah reset per gelombang wisuda.
- `JenjangProgramStudi.java` — 84/84 method. 548→1346 baris. r83193. Nama
  kelas menyesatkan (bukan master jenjang, tapi profil "Tentang Prodi" per
  Jurusan). Bug korupsi data: `getNama()` menimpa nama jadi `"-"` setiap kali
  relasi jenjang ada (logika kebalikan dari `Jenjang.java`).
- `LogLogin.java` — 60/60 method. 440→1174 baris. r83197/83202.
  **KLUSTER TEMUAN KEAMANAN NYATA** — DIESKALASI ke task terpisah
  `task_78a5b1ab`: (1) password mentah dari percobaan gagal tersimpan
  permanen & tampil di layar admin (`FilterLoginAis.java`), (2) header HTTP
  di-dump tanpa filter termasuk Cookie/Authorization (`MServet.java`), (3)
  sessionid tersimpan tanpa hash, (4) header IP dipercaya tanpa validasi
  trusted-proxy → **blacklist IP bisa di-bypass**. Akar masalah di kode
  PEMANGGIL, bukan di entity — perbaikan BUTUH KEHATI-HATIAN EKSTRA
  (menyentuh alur login produksi, terutama bagian validasi IP).

**Total akumulasi 10 sesi kerja**: 223 (sesi 1-9) + 5 = **228 file** dari
7.401 (~3,1%). **3 task eskalasi aktif**: `task_15f5001e` (arsitektur getter),
`task_b0a90191` (command injection VA), `task_78a5b1ab` (kebocoran
kredensial log login — BARU).

## `ais/database/model/DetailKegiatan.java` — SELESAI 100% (2 Sep 2026)

Entity **baris tagihan** di dalam satu `Kegiatan` (tabel `public.detail_kegiatan`,
`@Audited`, `dynamicInsert/dynamicUpdate`). **68/68 method** terdokumentasi
(100%), 987 → 2096 baris. Revisi **r83201**, mirror `java/` verifikasi `cmp`
identik. Hanya Javadoc/komentar; nol perubahan logika.

**Posisi dalam rantai billing (diverifikasi dari kode, bukan asumsi):**
`ItemBiaya` → `SettingBiaya`/`DetailSettingBiaya` → `DetailBiaya` →
`DetailKegiatan` → `CicilanPembayaran` → `BuktiPembayaran`. **TIDAK ADA foreign
key `DetailKegiatan` ↔ `CicilanPembayaran`** — `CicilanPembayaran` tidak punya
properti `detailKegiatan` sama sekali; keduanya bertemu karena sama-sama menunjuk
`Kegiatan` + `DetailBiaya`/`ItemBiaya`/`PengaturanPembayaranBulanan` yang sama,
dan dicocokkan lewat kunci string `DetailKegiatan.kodeUnik(...)` (lihat
`KegiatanPersistenceHelper`). "Sisa tagihan"/"status lunas" TIDAK dihitung di
entity ini, melainkan di lapisan helper. Induknya pun bukan koleksi Hibernate:
`Kegiatan.detailKegiatans` adalah **string CSV** `",<id>:true,<id>:false,"`;
penghapusan = menandai `:false`, bukan `DELETE`.

**Verifikasi pola "getter yang menulis" — 12 getter, hampir semua getter di kelas
ini bukan getter polos:** `getBiaya`, `getBiayaTemporary`, `getDetailBiaya`,
`getItemBiaya`, `getKeterangan`, `getTanggal`, `getDiskon`, `getDendaCustom`,
`getKodeUnik`, `getKunci`, `getDiskonMahasiswaData`/`2`/`3`.

**Pola "getter menulis ke OBJECT LAIN" (sejenis temuan sesi 8 pada
`CicilanPembayaran.getKegiatan()`) — KONFIRMASI ADA:**
`getBiaya()` memanggil `detailBiaya.updateKeterangan(mahasiswa, semester)` →
`PembayaranNominalModifikasiHelper.updateKeterangan` yang menjalankan query
Hibernate dan men-`set` `nilaiBiayaBaru` pada `DetailBiaya`, yaitu baris **master
yang dipakai bersama banyak mahasiswa**. Jadi membaca nominal SATU baris tagihan
dapat mengubah baris master yang dilihat mahasiswa lain. Selain itu
`setKegiatan()` menulis ke `Kegiatan` induk (`appendDetailKegiatan`), dan
`appendDetailKegiatan` **mengabaikan diam-diam** objek yang `id`-nya masih `null`.

**Kuirk/bug dicatat apa adanya, TIDAK diperbaiki:**
1. **`getDiskonMahasiswaData()`/`2`/`3` bisa MENGHAPUS data.** Penyaringan rentang
   semester dan penolakan duplikat jenis diskon dikerjakan dengan menulis `null`
   ke field yang dipetakan ke kolom `diskon_mahasiswa_data*`. Pada instance
   *attached*, sekadar **membaca** baris tagihan di luar rentang semester dapat
   menghapus tautan diskonnya **permanen** (kerusakan satu arah — kembali masuk
   rentang tidak memulihkan). Membaca slot 3 memanggil slot 2 dan 1 lebih dulu,
   jadi satu pembacaan berpotensi mengosongkan ketiga kolom sekaligus.
2. **`getDendaCustom()` menolkan `dendaCustom`** bila saklar
   `menggunakanDendaCustom` mati — mematikan saklar lalu membaca sekali sudah
   cukup untuk menghapus angka denda yang diketik petugas.
3. **`getKeterangan()` menghasilkan awalan literal `"null"`.** Penjaganya
   mengizinkan `keterangan == null` tetapi penggabungannya `+=`, jadi hasilnya
   `"null, Diskon : ..."`. Ini asal-usul teks itu di kartu tagihan. Getter ini
   juga hanya menyebut slot diskon ke-1 — diskon kelompok/jenis seleksi/promo
   global/slot 2-3 memotong nominal tanpa pernah muncul di keterangan.
4. **`getDiskon()` memakai FIELD `biaya` mentah, bukan `getBiaya()`** → diskon
   persen dihitung di atas nominal basi bila `getBiaya()` belum dipanggil.
   Ketergantungan urutan panggilan yang tidak dijamin apa pun.
5. **`getKodeUnik()` memanggil `detailBiaya.getBayarKe()` tanpa penjagaan null dan
   tanpa `try/catch`** — satu-satunya getter di kelas ini yang membiarkan
   exception (NPE) lolos keluar.
6. `getTanggal()` cabang 4 memanggil `getItemBiaya().getTanggal...()` tanpa
   penjagaan null; NPE-nya ditelan `catch(Exception)` bertanda
   `auto-audit(empty-catch)` sehingga perhitungan tanggal berhenti diam-diam dan
   nilai balik jatuh ke `WaktuUtil.getDate()` (gejala "jatuh tempo jadi hari ini").
7. `hitungDiskon()` asimetris: cabang kelompok mahasiswa memakai `=` (satu diskon),
   cabang slot per-orang memakai `+=` (tiga slot menumpuk). Hanya promo global yang
   diplafon tidak melebihi nominal; kombinasi tiga slot bisa melampaui tagihan.
8. `adaDiskon()` memakai `check()` langsung, bukan getter penyaringnya, sehingga
   diskon yang sudah lewat semester masih bisa memblokir diskon kolektif tanpa
   memberi diskon per-orang.
9. `cariJenisDiskonMahasiswa()` dan `hitungDiskon()` menyalin rantai kondisi raksasa
   yang sama persis (label vs nominal) — perubahan aturan harus di kedua tempat.
10. Komentar kepala asli *"Bank generated by hbm2java"* salah salin dari entity
    `Bank` (hbm2java, Apr 2010); dipertahankan penjelasannya di Javadoc class.

**Tidak ada kerentanan keamanan baru** yang ditemukan di file ini.

## `ais/database/model/LogLogin.java` — SELESAI 100% (2 Sep 2026)

Entity **jejak audit login** (tabel `public.log_login`, `dynamicInsert/dynamicUpdate`,
**tidak** `@Audited`). 440 → 1174 baris, **60/60 method** terdokumentasi, r83197
(pesan utuh, tidak tersapu), mirror `java/` identik byte.

Struktur: satu baris = satu percobaan login (sukses MAUPUN gagal, pembeda
`success_status`). Dibuat HANYA di `ais/common/FilterLoginAis.java:172`, disimpan
best-effort oleh `saveLoginAndCheckBlacklist(...)`, lalu dipakai sebagai identitas
sesi (atribut session `"login"`). Hirarki log: `LogLogin` → `DetailLogLogin`
(ditulis `AutoStarter`) → `LogUserActifity`.

**Retensi ADA**: `LogCleanerProcessor` menghapus baris > 3 bulan, dijadwalkan
`ScheduledTimerTask` (delay & period 864.000.000 ms ≈ 10 hari) di
`applicationContext-business.xml`, gerbang konfigurasi `log_cleaner_processor`.
Kuirk: ambang memakai `tanggal_dirubah` (ter-reset tiap `UPDATE`), penghapusan
`detail_log_login` dikomentari padahal induknya dihapus (risiko gagal FK →
seluruh transaksi pembersihan batal), dan jadwal pertama baru jalan setelah
aplikasi hidup 10 hari.

**Pola berulang terverifikasi**: 9 getter menulis balik ke field (`getDosen`,
`getGuru`, `getTbmuser`, `getJurusan`, `getFakultas`, `getSekolah`, `getYayasan`,
`getSatuanKerja`, `getIp`) — akses properti, jadi pada instance *attached* nilai
turunan ikut ter-*flush*: menampilkan log bisa MENGUBAH log. TIDAK ada getter
yang membuka/menutup session Hibernate sendiri (semua lewat `check()`).

**Kuirk menonjol**: `getTbmuser()` MENIHILKAN field `tbmuser` bila pelaku
mahasiswa/siswa → tautan akun bisa terhapus di DB hanya karena baris dibaca.

### TEMUAN KEAMANAN (perlu eskalasi terpisah — bukan di entity, di pemanggilnya)

1. **Password plaintext masuk kolom `description`** — `FilterLoginAis` baris
   215/256/276/318: `"... menggunakan password " + password + ", namun gagal login"`
   pada 4 jalur (Dosen/Admin, Siswa, Penduduk, Mahasiswa). Tampil di layar
   `log_login.zul`.
2. **Dump header HTTP tanpa penyaringan** — `MServet.getHeadersInfo()` menyalin
   SEMUA header (termasuk `Cookie`/`JSESSIONID` dan `Authorization`) ke kolom
   `header` bertipe `text`. Bandingkan `ApiMobileLogger` yang memanggil
   `redactSensitive(...)`; penyaringan itu tidak ada di jalur ini.
3. **`sessionid` disimpan utuh tanpa hash**.
4. **IP bisa dipalsukan** — `getIp()` percaya `Cf-Connecting-Ip`/`X-Forwarded-For`/
   `X-Real-IP` tanpa cek proxy tepercaya → jejak audit bisa dipalsukan DAN gerbang
   `BlacklistIp.chek(login.getIp())` bisa dilewati dengan satu header.
5. Entity **tidak `@Audited`** padahal `LogLoginAction` tetap memasang tombol
   `RevisiHelper.createNewRevisi(...)` (riwayat akan kosong).
6. Pengguna **tidak bisa menghapus** log miliknya lewat jalur normal (layar
   dilindungi `CommonPrivilages`, tanpa aksi hapus); penghapusan hanya lewat
   retensi atau kaskade `delete from log_login where dosen/pegawai in (...)` oleh
   operator.

## `ais/database/model/Kurikulum.java` — SELESAI 100% (2 Sep 2026, sesi 10)

Entity **kurikulum** (tabel `public.kurikulum`, `@Audited`,
`dynamicInsert/dynamicUpdate`) — mis. "Kurikulum 2018", "Kurikulum 2023".
**55/55 method** (termasuk konstruktor) + class-level Javadoc + Javadoc
`serialVersionUID`. 392 → 1093 baris. **r83191** (pesan utuh, tidak tersapu),
mirror `java/` sudah diverifikasi byte-identik. Kompilasi `-implicit:none`
lulus; kode terbukti tidak berubah (perbandingan baris non-Javadoc identik).

### Struktur

Tujuh kelompok method: jejak audit (`oleh`/`olehId`/`tanggal_dirubah` + hook
`@PreUpdate`), identitas & relasi, masa berlaku, aturan pengambilan per
angkatan, aturan kelulusan (SKS wajib/pilihan/lulus), ambang OBE, integrasi
Feeder/PDDikti. Dua method bisnis nyata: `bolehAmbil(Mahasiswa)` dan
`apakahObe(String, String)`. **Tidak ada method utilitas/query statis sama
sekali** di kelas ini — seluruh query kurikulum ada di pemanggil
(`KurikulumAction`, `ImporKrsFeeder`, dst).

Relasi: `Jurusan` (LAZY, lewat `check()`), `Program` (`FetchMode.SELECT`), dan
— penting — **`KurikulumPunyaMatakuliah` TIDAK dipetakan sebagai koleksi
`@OneToMany`** di sini; selalu diquery dari sisi join table (satu kurikulum
bisa ratusan baris berisi RPS besar).

### Konfirmasi temuan sesi 8

`serialVersionUID = 2461822577548439808L` **memang identik** dengan
`KurikulumPunyaMatakuliah` — dan cakupannya ternyata lebih luas:
`BeasiswaPunyaItemBiayaTambahan` dan `UjianPunyaSoal` memakai angka yang sama
juga (**total 4 kelas**). Tanpa dampak fungsional (dicocokkan per kelas).

### Verifikasi pola berulang

- **Getter menulis balik ke field/DB: ADA, dominan.** Sembilan getter:
  `getTahun()` (→ tahun berjalan!), `getNama()`, `getTahunAkademik()`,
  `getJenisSemester()`, tiga `getJumlahAturanSks*()`, `getFeeders()`,
  `getNamaAsli()`, `getJurusan()`, dan `getTaObe()`.
- **Getter MURNI (tidak menulis balik):** `getAktif()`, `getObe()`,
  `getNonAktifkanYgTerlanjur...()`, `getFeeder()`, seluruh getter
  `tahunAngkatan*`/`*Obe` selain `getTaObe()`.
- **Getter menutup sesi Hibernate: TIDAK ADA.** Kelas ini bahkan tidak
  meng-import `HibernateUtil`; satu-satunya akses DB implisit lewat
  `GeneralValueObject.check()` di `getJurusan()`.

### Kuirk/bug (dicatat, TIDAK diperbaiki)

1. **`getNama()` memotong bagian yang salah.** Bila rakitan >60 karakter, yang
   disimpan `substring(nama.length() - 59)` — **59 karakter TERAKHIR**,
   sehingga nama program + jurusan di depan justru terbuang, menyisakan
   "...knik Informatika thn 2023 - ID: 412". Ambang 60 juga tidak sejalan
   dengan `@Column(length = 255)` pada kolom yang sama.
2. **`getNama()` membaca field `jurusan` langsung, bukan `getJurusan()`**, jadi
   melewati `check()` — `jurusan.getNama()` berpotensi
   `LazyInitializationException` pada instance detached. Ironisnya
   `getProgram()` di baris yang sama dipanggil lewat getter.
3. **`getProgram()` tidak memanggil `check()`** sedangkan `getJurusan()`
   memanggil — inkonsistensi pola antar dua relasi di kelas yang sama.
4. **`setTaObe()` praktis tidak berguna.** `getTaObe()` **selalu** menghitung
   ulang dari `tahunAkademikObe`+`semesterObe` dan **selalu** menimpa field
   (bukan hanya saat `null`) — kolom `taObe` adalah nilai turunan yang redundan.
   Karena `apakahObe()` memanggilnya tiap evaluasi, dan `apakahObe()` dipanggil
   di dalam perulangan dasbor/penilaian, entity kurikulum yang attached bisa
   berulang kali ditandai kotor.
5. **Asimetri gagal-terbuka/gagal-tertutup di jalur OBE.** Ambang `getTaObe()`
   yang gagal diurai jatuh ke `0` ⇒ OBE berlaku sejak kapan pun
   (gagal-TERBUKA); sedangkan semester yang dinilai gagal diurai juga jatuh ke
   `0` ⇒ OBE tidak berlaku (gagal-TERTUTUP). Dua arah berlawanan dari formula
   yang identik.
6. **Digit semester bersifat "sisanya Ganjil"** — apa pun selain
   "Genap"/"Semester Pendek" (termasuk salah ketik) dipetakan ke `"1"`. Tanpa
   validasi.
7. **`getAktif()` membaca `null` sebagai `true` tanpa menulis balik**, jadi
   setiap query wajib `or(isNull("aktif"), eq("aktif", true))`. Menulis
   `eq("aktif", true)` saja akan menyembunyikan seluruh kurikulum lama.
   Default-nya juga **berlawanan arah** dengan
   `getNonAktifkanYgTerlanjur...()` yang default `false`.
8. **`getFeeders()` menumpuk tanpa deduplikasi/batas.** `FeederJSONImport`
   menambah dengan `setFeeders(getFeeder() + ";" + existing.getFeeders())`;
   impor berulang membuat kolom `text` itu membengkak berisi id kembar.
9. **`getNonAktifkanYgTerlanjurMengambilTidakSesuaiTahunAngkatan()` berdampak
   retroaktif dan destruktif.** Di `Detailperkuliahan`, kombinasi flag ini +
   `!bolehAmbil()` menurunkan status KRS menjadi `BELUM_DISETUJUI` — jadi
   mencentangnya di layar bisa **membatalkan KRS yang sudah disetujui secara
   massal**.

**Tidak ditemukan kerentanan keamanan** di file ini (tidak ada SQL dirakit
manual, tidak ada I/O berkas, tidak ada keluaran HTML mentah).

## `ais/database/model/PendaftaranWisuda.java` — SELESAI 100% (2 Sep 2026)

Entity **pendaftaran wisuda mahasiswa** (tabel `public.pendaftaran_wisuda`,
`@Audited`, `dynamicInsert/dynamicUpdate`). 59/59 anggota terdokumentasi
(100%) — 57 getter/setter + 1 konstruktor + 1 kait `@PreUpdate` —
346 → 1000 baris. Commit **r83192** (pesan utuh, tidak tersapu),
mirror `java/` diverifikasi byte-identik. Kompilasi javac 1.7
`-implicit:none` lulus; sumber tanpa komentar dibandingkan dengan HEAD →
identik, jadi **nol perubahan kode**.

Struktur: (1) jejak audit re-deklarasi `id`/`oleh`/`olehId`/
`tanggal_dirubah` + kait `@PreUpdate onUpdate()`, (2) 3 relasi ManyToOne
LAZY `Mahasiswa`/`Skripsi`/`Wisuda`, (3) 5 kolom persetujuan berkas,
(4) kolom JSON `status_pendaftaran`, (5) 9 kolom ceklis lama, (6) nomor
registrasi + nomor kursi, (7) atribut lain (`tanggalDaftarWisuda`,
`ukuranToga`, `persetujuanWisuda`, `keterangan`).

Temuan:
- **No. Registrasi Wisuda == No. Kursi** (konfirmasi dari sisi entity atas
  temuan sesi 6): keduanya diisi `pendaftaranWisuda.getId().toString()`
  di-pad nol 8 digit di `GenerateNoKursiDanNoRegistrasiWindow` — bukan
  sequence terpisah, bukan nomor urut per acara. Untuk satu mahasiswa
  kedua kolom selalu bernilai sama; nomor kursi tak pernah mulai dari 1
  per acara dan berlompatan mengikuti id global.
- **9 kolom ceklis lama mati**: `statusFotoCopy*` (6 buah),
  `statusBiayaWisuda`, `statusTandaLulusTOAFLTOEFL`, `statusPasPhoto`
  tidak dibaca/ditulis dari mana pun di luar entity (penelusuran seluruh
  pohon sumber). Digantikan kolom JSON `status_pendaftaran`. Jangan
  dijadikan sumber kebenaran; dipertahankan demi baris lama + Envers.
- **Kunci JSON terikat nama kelas Java**: kunci dirakit sebagai
  `namaKelasAction.toLowerCase() + "_" + namaItem.toLowerCase()
  .replaceAll(" ", "_")`; daftar item dari konfigurasi (mis.
  `wisuda_administrasi`). Ganti nama kelas Action atau ubah label item →
  centang lama jadi yatim. Kelima meja menulis ulang dokumen JSON secara
  utuh → dua meja menyetujui bersamaan bisa saling timpa.
- **`persetujuanWisuda` disetel MANUAL** (checkbox di
  `MahasiswaRegistrasiWisudaAction`), BUKAN turunan otomatis dari kelima
  status berkas — padahal ia gerbang generate nomor & daftar hadir. Bisa
  `true` walau tahap pengecekan masih 0.
- Tidak ada unique constraint (mahasiswa, wisuda) di DB; pencegahan
  pendaftaran ganda hanya di kode (`Criteria ... setMaxResults(1)`).

Verifikasi pola berulang:
- **Getter menulis balik ke field**: `getTanggalDaftarWisuda()` mengisi
  `WaktuUtil.getDate()` bila null → pada instance *attached* nilai itu
  bisa ikut tersimpan + terekam Envers, sehingga baris lama ber-`NULL`
  bisa "mendapat" tanggal = waktu pertama dibuka di layar.
  `getMahasiswa()`/`getSkripsi()`/`getWisuda()` menulis balik hasil
  `check()`.
- **Getter yang bisa membuka/menutup sesi Hibernate**: ketiga getter
  relasi lewat `check()` (tahap 3 `reloadDetachedObject` membuka
  `openSession()` dan menutup di `finally`). `toString()` ikut terkena
  karena memanggil `getMahasiswa()` + `getSkripsi()`.
- **Getter yang TIDAK menulis balik**: `getStatusPersetujuan*` (null→0)
  dan `getStatusPendaftaran()` (null→`"{}"`) hanya menormalkan nilai
  kembalian; kolom di DB tetap `NULL`.
- Ketidakkonsistenan: `getUkuranToga()` satu-satunya getter `Integer`
  yang TIDAK menormalkan null. Peta kode toga 1=S, 2=M, 3=L, selain
  itu=XL — nilai awal field 0 ("belum memilih") ikut jatuh ke label "XL".
- `getStatusPendaftaran()` memanggil `.toString()` pada nilai yang sudah
  `String` (mubazir, sisa versi lama saat field bertipe `JSONObject`).

Tidak ditemukan kerentanan keamanan baru pada file ini.

## Batch "5 entity ujian/organisasi/konfigurasi" — SELESAI 100% (2 Sep 2026, dikonsolidasi orkestrator)

Semua 5 file TUNTAS 100% method, dikompilasi, dikommit, di-mirror ke `java/`:
- `Ujian.java` — 66/66 method. 607→1506 baris. r83178/83181. Memperjelas 3
  lapis: `Ujian`=cetakan/master, `PertemuanPunyaUjian`=jadwal+durasi,
  `HasilUjianMahasiswa`=hasil (brief awal keliru taruh jadwal/durasi di
  `Ujian`, sudah dikoreksi). XSS tersimpan berhak-istimewa (dosen→mahasiswa)
  di teks tata tertib — akar penyebab SAMA dengan filter lemah yang sudah
  terdokumentasi di `GeneralValueObject`, bukan kerentanan baru terpisah.
- `HasilUjianMahasiswa.java` — 77/77 method. r83184 (pesan lengkap, tidak
  tersapu). 4 relasi peserta saling eksklusif (Mahasiswa/BiodataCalon
  Mahasiswa/Siswa/CalonSiswa).
- `Fakultas.java` — 57/57 method. 394→1192 baris. r83177/83180. Dirujuk ~97
  entity, setara level `Jurusan`. **Risiko integritas multi-tenant**:
  `getPerguruanTinggi()` menebak tenant dari konteks HTTP request — di jalur
  batch/non-web (tanpa request) jatuh ke default statis, berpotensi salah
  tenant.
- `Konfigurasi.java` — 41/41 method. 408→1200 baris. r83183/83185.
  **Dokumentasi mekanisme auto-seed PALING LENGKAP sejauh ini** (lihat
  bagian khusus di bawah — juga sudah dipindahkan ke memory lintas-sesi).
- `HistoryStatusMahasiswa.java` — 41/41 method. 525→1106 baris. r83176/83179.
  `Mahasiswa` TERNYATA tidak punya properti status sendiri — file ini
  SATU-SATUNYA sumber kebenaran status per semester, bukan sekadar "riwayat".

### Mekanisme auto-seed `Common.getKonfigurasi` — referensi lengkap

Logika ada di `ais/common/KonfigurasiManager.java:67`, entity `Konfigurasi`
hanya wadahnya. 7 langkah: (1) kunci kosong → singleton `konfigurasiKosong`
dibagi seluruh JVM, (2) cache MapDB dengan `catch(Throwable)` khusus store
tertutup, (3) baca DB terisolasi `order by id desc limit 1` (kolom `nama`
SENGAJA tidak unik — baris terbaru menang), (4) **seed**: kunci belum ada →
buat baris baru berisi `defaultValue` hardcode dari kode PEMANGGIL, commit
permanen ke produksi, (5) penanganan balapan lewat tabrakan PRIMARY KEY
(bukan `nama`) + perbaikan sequence via `lock table`, (6) `finally` tutup
sesi, (7) isi cache. **Race window TIDAK terkunci** antara baca(3) dan
tulis(4) — baris duplikat senama masih mungkin terbentuk (tidak merusak,
karena pembaca selalu ambil id terbesar). Kredensial default hardcode YANG
SUDAH TER-SEED bertahan permanen di DB + Envers + backup; mengubah default
di kode TIDAK mengubah baris lama. Cek keberadaan kunci TANPA memicu seed:
`cariKonfigurasi(nama)`/`kumpulanNamaKonfigurasi()`. Getter `Konfigurasi`
menulis balik ke field HANYA berlaku untuk instance *attached*
(dari session Hibernate aktif) — instance hasil deserialisasi cache MapDB
adalah *detached*, jadi gejala "kadang tersimpan kadang tidak" pada
penulisan getter adalah hal yang diharapkan, bukan bug acak.

**Total akumulasi 9 sesi kerja**: 218 (sesi 1-8) + 5 = **223 file** dari
7.401 (~3,0%).

**CATATAN UKURAN FILE**: tracker ini sudah >2400 baris. Pertimbangkan minta
orkestrator/pengguna memecahnya jadi beberapa file per-topik (mis.
`PROGRESS-action-helper.md`, `PROGRESS-database-model.md`,
`PROGRESS-catatan-sesi.md`) di sesi mendatang bila makin sulit dikelola —
belum dilakukan sesi ini, hanya dicatat sebagai pertimbangan.

## `ais/database/model/HasilUjianMahasiswa.java` — SELESAI 100% (2 Sep 2026)

Entity **hasil ujian per peserta** (tabel `public.hasil_ujian_mahasiswa`,
`@Audited`, `dynamicInsert/dynamicUpdate`) — induk dari
`HasilUjianMahasiswaDetail` (satu baris per jawaban soal). 1184 → 2455 baris,
**77/77 method + 2 konstruktor + seluruh field ber-Javadoc (100%)**. Revisi
**r83184**, mirror `java/` diverifikasi byte-identik (`cmp`). Kompilasi
`javac 1.7 -implicit:none` lulus; kode tidak diubah sama sekali.

**Struktur**: `extends GeneralValueObject` langsung. Empat relasi peserta yang
saling eksklusif dalam satu tabel — `Mahasiswa` / `BiodataCalonMahasiswa` (PMB)
/ `Siswa` / `CalonSiswa` (PPDB) — tanpa constraint DB yang memaksa "tepat
satu"; itu murni konvensi kode, dan urutan pemeriksaannya (mhs → cal_mhs →
siswa → cal_siswa) menentukan di `genKey`. Relasi ke `PertemuanPunyaUjian`
wajib dan menjadi sumber hampir semua parameter perhitungan. Relasi ke
`HasilUjianMahasiswaDetail` **sengaja tidak dipetakan** sebagai koleksi
Hibernate — diambil manual lewat `ambilDataAsli()` supaya bisa di-cache.

**Method berlogika nyata** (bukan sekadar getter/setter):
`getNilai()` (koreksi otomatis pilihan ganda, 2 penyebut berjenjang + clamp
100), `getLulus()`, `getJumlahSoal()`, `getLamaPengerjaan()`,
`getSisaWaktuPengerjaan()`, `getSelesaiPada()`, `getKeyhasil()`, `reset()`,
`ambilDataAsli()` (mesin cache statik + session terdedikasi),
`ambilHasilUjianMahasiswaDetail(boolean,int,Label,MyArrayList)` (deduplikasi
jawaban ganda — method paling berlogika di file),
`ambilUjianPunyaSoals()` (menyusun ulang lembar ujian untuk "lanjutkan"),
`genKey()`, `ambilByKey()` (get-or-create), `tampilkanUjianKembali()`.

**Verifikasi pola berulang (sesuai instruksi)**
- **Getter yang menulis balik ke field/DB — ADA, banyak.** `getNilai`,
  `getLulus`, `getJumlahSoal`, `getLamaPengerjaan`, `getSisaWaktuPengerjaan`,
  `getSelesaiPada`, `getKeyhasil`, `getJawabanBenar`, plus 5 getter relasi
  (lewat `check()`). Karena kelas ini memakai **akses properti** (anotasi di
  getter), Hibernate memanggilnya saat dirty-check/flush → hasil hitungan ikut
  **tersimpan ke DB** walau tak ada setter yang dipanggil aplikasi. Mengubah
  rumus di getter = mengubah data tersimpan.
- **Getter yang menutup sesi Hibernate — TIDAK ADA di getter.** Yang menutup
  sesi adalah `ambilDataAsli()` (private; sengaja `openSession()` terdedikasi
  lalu `clear`→`disconnect`→`close` di `finally`, justru supaya TIDAK menutup
  sesi pemanggil) dan `ambilByKey()` (statik; menutup `currentNativeSession()`
  + `HibernateUtil.closeSession()` — pemanggil `HasilUjianMahasiswaHelper`
  memang sengaja membuang sesinya dulu sebelum memanggil).

**Temuan/kuirk (dicatat, TIDAK diperbaiki)**
1. **`ambilByKey` menulis ke DB meski namanya "ambil"** — get-or-create: buat
   + `save` baris baru, atau `update` untuk mengisi `keyhasil` baris warisan.
   Transaksi di-`commit` tanpa `rollback` di jalur gagal; `masukkanDataLangsung`
   dipanggil di luar `try` sehingga bisa menerima argumen `null` saat exception.
2. **`getKeyhasil()` menghitung ulang & menimpa kolom `unique`** — mengubah
   relasi peserta/sesi pada baris tersimpan mengubah kunci uniknya diam-diam;
   bila bentrok, `UPDATE` gagal di tempat yang tampak tak berhubungan. Bila
   keempat peserta kosong, `genKey` mengembalikan `null` → kunci tersimpan ikut
   terhapus.
3. **`getSisaWaktuPengerjaan()` menimpa kolom DB dengan isi cache berkas**
   (`retreive()`), akurat saat ujian berjalan tapi basi setelahnya. Ini akar
   dua insiden lama ("waktu selesai di masa depan", "lama pengerjaan ngawur")
   yang sudah dijinakkan penjaga `if (selesaiPada != null)` /
   `if (lamaPengerjaan != null)` — penjaga itu **jangan dilepas**. Normalisasi
   jam > 22 → `00:00:01` menangani pengurangan waktu yang membalik lewat
   tengah malam.
4. **Kode mati** (dipastikan lewat pencarian seluruh repo): `ambilJumlahTerjawab`,
   `ambilHasilUjianMahasiswaDetail(Session, Collection, BankSoalDetail)`, dan
   `ambilHasilUjianMahasiswaDetail(int, MyArrayList, BankSoal)`.
5. **Parameter tak terpakai**: `session` pada overload `(Session, Collection,
   BankSoalDetail)`; `label` pada `ambilHasilUjianMahasiswaDetail(boolean, int,
   Label, MyArrayList)` (pemanggil tetap mengoper `new Label()` sebagai pengisi).
6. **`ambilUjianPunyaSoals` lintasan pertama tidak cek duplikat** (hanya
   lintasan kedua yang cek) → id `UjianPunyaSoal` bisa masuk dua kali bila
   peserta punya >1 baris jawaban untuk soal yang sama. Persentase progres
   memakai penyebut jumlah *detail*, bukan `maxSize`, jadi bisa tak sampai 100%.
7. **`setMulaiPada` sekali-tulis** — argumen diabaikan bila field sudah terisi
   (melindungi waktu mulai asli saat peserta keluar-masuk). Akibatnya waktu
   mulai tidak bisa dikoreksi/dikosongkan lewat setter; hanya `reset()` yang
   bisa, karena menyentuh field langsung.
8. **`getJumlahSoal()` membuat kolomnya jadi bayangan** — selalu ditimpa
   `pertemuanPunyaUjian.getJmlDitampilkan()`, jadi mengubah konfigurasi jumlah
   soal ikut mengubah angka historis peserta lama.
9. **`ambilBankSoalIdTerjawabDinilai` memakai ambang `> 0.01`** → soal esai yang
   sudah dikoreksi tapi bernilai **nol** tampak "belum dinilai" di filter
   `KoreksiHasilUjian`. Method ini juga satu-satunya yang memanggil
   `getBankSoal().getId()` tanpa cek null (aman dalam praktik karena
   `ambilDataAsli` sudah menyaring `isNotNull("bankSoal")`).
10. **`reset()` tidak menghapus jawaban** (`HasilUjianMahasiswaDetail`) — itu
    tanggung jawab pemanggil; ia hanya mengosongkan field + menulis
    `put("1","index")` (posisi soal kembali ke awal).
11. **Idiom "buang referensi" yang tak berefek**: `terjawab = null`,
    `hasilUjianMahasiswaDetailsa = null` atas *parameter* — muncul berulang,
    tidak berpengaruh bagi pemanggil.
12. `toString()` mahal: memicu resolusi 3 relasi lazy + menulis balik ke field,
    dan menampilkan field `nilai` mentah (bukan `getNilai()`).

**Keamanan**: tidak ada kerentanan yang bisa dieksploitasi hari ini.
`Restrictions.sqlRestriction("true")` adalah literal konstan (bukan gabungan
input) → **bukan** SQL injection. Satu catatan pengerasan defensif:
`tampilkanUjianKembali(String)` **tidak memverifikasi kepemilikan** — id apa
pun yang dioper akan dimuat & ditampilkan. Aman sekarang karena satu-satunya
pemanggil (`MainAction`/`MainAction2`) mengambil id dari cache berkas milik
pengguna yang sedang login, bukan dari input HTTP; tapi bila kelak ada
pemanggil yang mengambil id dari parameter permintaan, ini menjadi IDOR.
Sudah dicatat di Javadoc method tersebut.

**Konfirmasi ulang fakta arsitektur**: field `id`/`oleh`/`olehId`/
`tanggal_dirubah` dideklarasikan ulang di file ini karena `GeneralValueObject`
bukan `@Entity`/`@MappedSuperclass` — **keharusan teknis**, bukan duplikasi.

## `ais/database/model/Konfigurasi.java` — SELESAI 100% (2 Sep 2026)

Entity **key-value konfigurasi sistem** (tabel `public.konfigurasi`, `@Audited`,
`dynamicInsert/dynamicUpdate`), dirujuk **>1.000 berkas** — salah satu entity
paling menyebar di codebase. 408 → 1200 baris, **41/41 method ber-Javadoc
(100%)** plus seluruh konstanta kunci dan 15 field. Revisi **r83183**, mirror
`java/` diverifikasi byte-identik (`cmp`). **Kode terbukti tidak berubah**:
`javap -c -p` identik dengan HEAD sebelum edit. Javadoc kelas lama berjudul
salah salin *"Bank generated by hbm2java"* (sisa hbm2java dari entity `Bank`).

**MEKANISME AUTO-SEED — didokumentasikan lengkap di Javadoc kelas.** Logikanya
TIDAK ada di entity ini melainkan di `ais/common/KonfigurasiManager.java`
(dibungkus `Common.getKonfigurasi`/`bolehKonfigurasi`). Kontrak intinya:
**membaca kunci yang belum ada AKAN MEMBUAT barisnya di DB, terisi
`defaultValue` yang di-hardcode di kode pemanggil.** Tujuh langkah:
(1) kunci kosong → singleton `konfigurasiKosong` (`new Konfigurasi("","")`,
tanpa id, **dibagi seluruh JVM**); (2) cache MapDB `MemoryDbUtil.getKonfigurasi()`
— ditangkap `Throwable` bukan `Exception` karena store tertutup melempar
`java.lang.Error`; (3) `openSession()` terisolasi + Criteria `order by id desc
limit 1` (**kolom `nama` sengaja TIDAK unik**, beda dari `ParameterUmum`);
(4) seed baris baru dalam transaksi; (5) penanganan `ConstraintViolationException`
— yang bentrok **primary key**, bukan `nama`: rollback → `session.clear()` →
baca ulang hasil thread lain; bila nihil → `perbaikiSequenceDanSimpan()` yang
`lock table ... in share row exclusive mode`, cek ulang, `setval(pg_get_serial_sequence,
max(id)+1)`, ulang simpan SEKALI; gagal total → dicatat **sekali per nama**
(`BENTROK_ID_TERCATAT`) dan pemanggil dapat `konfigurasiKosong`, bukan exception;
(6) `clear/disconnect/close` di `finally`; (7) isi cache.

**Sisa risiko balapan (dicatat jujur):** jendela baca→insert TIDAK terkunci dan
tidak ada indeks unik pada `nama`, jadi **baris duplikat masih mungkin
terbentuk**. Tidak merusak (pembacaan pilih id terbesar) — itulah alasan query
tidak memakai `uniqueResult()`.

**Implikasi keamanan (bukan kerentanan baru, penegasan pola yang sudah ada di
memory):** default rahasia yang ditulis sebagai argumen `defaultValue`
ter-seed diam-diam ke DB produksi pada akses pertama, bertahan, ikut terekam
Envers (`@Audited`), ikut masuk cadangan. **Mengubah default di kode TIDAK
mengubah baris yang sudah ter-seed.** Cek keberadaan kunci tanpa memicu seed:
`KonfigurasiManager.cariKonfigurasi(nama)` atau `kumpulanNamaKonfigurasi()`.

**Getter-yang-menulis diverifikasi satu per satu** (pola wajib audit):
- `getNilai()` — penulis paling agresif, **5 penugasan** ke field `nilai`:
  substitusi 5 kunci `ISTILAH_*` dari `ConstantUtil`; normalisasi `null` →
  `"aktif"` (**default implisit seluruh konfigurasi adalah MENYALA**); evaluasi
  rentang `KalenderAkademik` (juga menulis `tahunAkademik` + `info1`);
  substitusi `nilaiDikunci` saat terkunci; pemaksaan `recapcha_home` → `"login"`.
- `getInfo1()` — normalisasi null + pengisian ganjil/genap otomatis.
- `getNilaiDikunci()` — menyalin nilai berjalan selama belum dikunci; penyalinan
  itulah yang membuat tombol Kunci berfungsi.
- `getDikunci()`/`getKalenderAkademik()` — `check(...)` standar (tulis balik proxy).

Semua itu properti terpetakan (**property access**: `@Id` di getter → getter
tanpa `@Transient` ikut dipetakan, termasuk `info1..5`, `oleh`, `olehId`,
`nilaiDikunci` yang tanpa `@Column`) pada entity `dynamicUpdate`, jadi
penulisannya **tersimpan ke DB pada flush** untuk object *attached* — tetapi
TIDAK untuk instance hasil deserialisasi cache MapDB (*detached*). Inilah asal
gejala klasik "kadang tersimpan, kadang tidak". **Tidak ada getter di kelas ini
yang membuka/menutup session sendiri** (session hanya dibuka `KonfigurasiManager`).

**Kuirk lain (dicatat, TIDAK diperbaiki):** `setTahunAkademik` menolak menimpa
nilai terisi (tahun ajaran tak bisa diganti lewat setter); `getInfo4`/`getInfo5`
tidak menormalkan null seperti `getInfo1..3`; `info1AndCheckWajibDanTidakWajib`
membaca field mentah → bisa mengawali hasil dengan teks `"null"`, dan NPE bila
`getNilai()` null; nama method `niliaInteger` salah eja (sama di `ParameterUmum`);
`WAJIB_DIISI` berisi literal identik `AKTIF` sehingga tak terbedakan; `getNama()`
trim hanya di getter sedangkan query membandingkan kolom mentah; `getNilai()`
membandingkan `nama` lewat field (tanpa trim) untuk `ISTILAH_*` tapi dengan
`trim()` untuk `recapcha_home`; cabang `info1 == null` di `getInfo1` adalah kode
mati; **penguncian bersifat destruktif** — flush pertama sesudah dikunci menimpa
kolom `nilai` dengan salinan beku, membuka kunci tidak mengembalikan nilai asli.

**Temuan minor (bukan eskalasi):** `recapcha_home` dipaksa `"login"` di
`getNilai()` sehingga baris konfigurasi yang tetap disediakan di
`KonfigurasiNewAction` praktis tak berpengaruh lewat jalur itu. Pemaksaan ini
justru pagar terhadap open-redirect (`ConstantValues.recapchaHome` mengalir ke
`sendRedirect`), TAPI listener di `KonfigurasiNewAction:7898` menyetel
`ConstantValues.recapchaHome` **langsung di memori**, melewati `getNilai()` —
admin dapat mengarahkannya ke alamat sembarang sampai restart. Admin-only,
severity rendah.

## `ais/database/model/Ujian.java` — SELESAI 100% (2 Sep 2026)

Entity **master ujian** (tabel `public.ujian`, `@Audited`, `dynamicInsert/
dynamicUpdate`), `extends GeneralValueObject`. 607 → 1506 baris,
**66/66 method ber-Javadoc (100%)** plus 22 field didokumentasi. Revisi
**r83178**, mirror `java/` diverifikasi byte-identik (`cmp`). Kode terbukti
tidak berubah: perbandingan baris non-komentar terhadap HEAD sebelum edit
identik.

**Kedudukan dalam model ujian online** (sering tertukar saat membaca kode):
`Ujian` = *cetakan*/master (nama, kode, jenis, nilai lulus, level, tata
tertib, sertifikat, syarat) → `PertemuanPunyaUjian` = *penjadwalan* pada satu
`Pertemuan` (mulai/selesai, durasi, random, jumlah soal ditampilkan, format
nilai, seluruh setelan anti-curang) → `HasilUjianMahasiswa` = pengerjaan satu
peserta. **Jadwal dan durasi TIDAK ada di `Ujian`.**

**Relasi soal tidak dipetakan sebagai koleksi.** Many-to-many ke `BankSoal`
lewat `UjianPunyaSoal`, tapi tidak ada `Set<UjianPunyaSoal>` yang dipetakan.
Sebagai gantinya ada **indeks JSON berbasis berkas** per-ujian
(`Common.getFileLocation(this, "ujian_punya_soal_<id>")`) yang isinya hanya
ID (`"<id>": "<id>"`); isi sebenarnya selalu diambil ulang lewat
`ambilData(UjianPunyaSoal.class, id)`. Sinkronisasi otomatis oleh
`AuditListener` (`populateUjianPunyaSoal` saat insert, `removeUjianPunyaSoal`
saat delete); pembangunan ulang total oleh `reInitUjianPunyaSoal(Session)`
yang dipicu pertama kali lewat penanda test-and-set `udah("ujian")`.

**Dua domain akademik** dalam satu tabel: fakultas/jurusan/dosen/matakuliah
(PT) berdampingan dengan yayasan/sekolah/guru/matapelajaran (sekolah).

**Verifikasi pola berulang** (bentuk spesifik di file ini):
- *Getter yang menulis balik ke field* → 4: `getJenis()`, `getJenisKoreksi()`,
  `getLevel()`, `getTatatertibUjian()`. Karena `@Id` di getter, pemetaan
  Hibernate berbasis properti, jadi keempat kolom itu **ikut tersimpan ke DB**
  pada flush berikutnya walau pemanggil cuma "membaca".
- *Getter yang TIDAK menulis balik* (didokumentasikan sebagai pembanding):
  `getNilaiLulus()`, `getAktif()`, `getNama()`, `getSyaratAkses()`,
  `getTampilanHurufDiPilihanJawaban()`.
- *Getter relasi lazy yang memanggil `check()`* → 6: jurusan, fakultas, dosen,
  syaratUjian, yayasan, sekolah. Relasi eager (matakuliah, sertifikat, guru,
  matapelajaran) sengaja TIDAK memakai `check()`.
- *Penutupan sesi Hibernate* → hanya SATU titik:
  `closeNativeSessionQuietly(Session)` dipanggil dari blok `finally`
  `ambilUjianPunyaSoal(String, PertemuanPunyaUjian, boolean)` pada jalur
  pembangunan ulang indeks. **Tidak ada getter properti yang menutup sesi.**

**Kuirk dicatat, tidak diperbaiki:**
1. `setJenis()` praktis mati — `getJenis()` SELALU menurunkan `jenis` dari
   `getJenisKoreksi()`, jadi nilai yang di-set akan tertimpa pada pembacaan
   berikutnya. Pintu masuk yang benar adalah `setJenisKoreksi()`.
2. Baris `return jenis == null ? PILIHAN_GANDA : jenis` di `getJenis()` tak
   pernah tercapai cabang bawaannya (percabangan di atasnya menjamin non-null).
3. Parameter `tulisUlang` pada `populateUjianPunyaSoal(UjianPunyaSoal, boolean)`
   **tidak dipakai sama sekali**; kedua pemanggil kebetulan selalu kirim `true`.
4. `removeUjianPunyaSoal()` menulis `"<id>": ""` (nisan) alih-alih membuang
   kunci → berkas indeks **tumbuh terus** dan tidak pernah menyusut sampai
   di-`reInit`.
5. `reInitUjianPunyaSoal()` menulis `"{}"` dulu baru mengisi ulang → ada
   **jendela indeks kosong** tanpa penguncian; pembaca bersamaan melihat ujian
   seolah tanpa soal.
6. Dua cabang besar `ambilUjianPunyaSoal(String, PertemuanPunyaUjian, boolean)`
   praktis duplikat; beda nyatanya hanya `Collections.sort` pada cabang tidak-acak.
7. Paginasi `ambilUjianPunyaSoal(boolean, ..., mulai, banyak)` memotong di
   memori, bukan `LIMIT/OFFSET` — halaman kecil tidak menghemat biaya baca.
8. `serialVersionUID` identik dengan milik `PertemuanPunyaUjian` (sisa
   salin-tempel; tidak berdampak).
9. Javadoc kelas lama berbunyi `"Bank generated by hbm2java"` — nama salah,
   sudah digantikan.

**KERUSAKAN DATA + PERMUKAAN XSS TERSIMPAN** — `getTatatertibUjian()`
menjalankan `filterTidakBolehSederhana()` lalu **menugaskan hasilnya kembali
ke field**. Filter itu mengganti setiap kemunculan kata `script`
(case-insensitive, termasuk yang menyatu dengan kata lain) menjadi `__S__`.
Akibatnya tata tertib sah yang memuat kata tersebut **rusak permanen** pada
flush berikutnya, dan kerusakan menumpuk tiap simpan ulang. Getter yang sama
juga menulis balik teks bawaan ke DB hanya karena halaman ujian dibuka. Di
sisi lain teks ini dirender **mentah sebagai HTML** ke peserta
(`ProsesUjianHelper` via `MyHtml`, dua titik: pembuka ujian dan footer),
sementara filternya hanya menyaring kata `script` — atribut event seperti
`onerror=` lolos. Penulisnya dosen/admin, jadi ini permukaan XSS tersimpan
berhak-istimewa (dosen → sesi mahasiswa/admin), bukan dari input mahasiswa.
Dicatat, tidak diubah.

Field audit bayangan (`id`/`oleh`/`olehId`/`tanggal_dirubah`) hadir seperti
biasa dan didokumentasikan sebagai **keharusan teknis**, bukan bug.

## `ais/database/model/Fakultas.java` — SELESAI 100% (2 Sep 2026)

Entity master **fakultas** (tabel `public.fakultas`, `@Audited`,
`dynamicInsert/dynamicUpdate`) — tingkat tengah hierarki
`PerguruanTinggi` → `Fakultas` → `Jurusan`. 394 → 1192 baris,
**57/57 method ber-Javadoc (100%)** plus semua field didokumentasi. Revisi
**r83177**, mirror `java/` diverifikasi byte-identik (`cmp`). **Kode terbukti
tidak berubah**: bytecode `javap -c -p` identik dengan HEAD sebelum edit.

**Struktur**: `extends GeneralValueObject` langsung; 55 accessor + 2
konstruktor + `putFile(Map)` (satu-satunya method non-accessor, mengisi
parameter kop/stempel JasperReports — sama persis pola `Jurusan.putFile`).
**Tidak ada koleksi `List<Jurusan>`** — relasi fakultas–jurusan dipetakan
satu arah dari sisi `Jurusan` saja, jadi tidak ada navigasi ke bawah dan
tidak ada cascade ke prodi. Dirujuk sangat luas: ~97 entity di
`ais.database.model` punya field `private Fakultas`, ~932 berkas menyebut
nama kelasnya (setara level `Jurusan`).

**Verifikasi pola berulang** (dibandingkan dengan `Jurusan.java`):
- *Getter menulis balik ke field*: **ADA**, 4 buah — `getNama()` (→ `""`),
  `getDeskripsi()` (→ `""`), `getWarna()` (→ default `"#3300ff"`),
  `getRgb()` (selalu menimpa dengan hasil `Common.hex2Rgb`). Karena
  pemetaan property-access, nilai sulihan ikut tertulis ke DB.
- *Getter membuka session Hibernate*: **ADA**, 8 getter relasi memanggil
  `check()` (`dekan`, `pudek1..3`, `perguruanTinggi`, `satuanKerja`,
  `pegawai1..3`).
- *Getter yang men-`save`/`insert` ke tabel master lain*: **TIDAK ADA**
  (sama seperti `Jurusan`).
- *Field audit shadow* (`id`/`oleh`/`olehId`/`tanggal_dirubah`): ADA —
  keharusan teknis, bukan bug (`GeneralValueObject` bukan `@MappedSuperclass`).

**Temuan/kuirk (dicatat, TIDAK diperbaiki):**
1. `getPerguruanTinggi()` **mengisi dirinya sendiri dari konteks request**
   (`PerguruanTinggiUtil.getPerguruanTinggi()` → tebak dari user login /
   HttpSession / nama domain → default statis) lalu menugaskan hasilnya ke
   field. Tidak ada padanannya di `Jurusan`. Untuk fakultas **baru**, induk
   hasil tebakan itu ikut tersimpan. Di jalur non-web (batch/penjadwal/impor
   Feeder) yang terpilih adalah `perguruanTinggiDefault` statis — berpotensi
   salah tenant pada pemasangan multi-tenant.
2. `getRgb()` **bisa melempar exception**: `Common.hex2Rgb` mengiris
   `substring(1,3/5/7)` + `Integer.valueOf(...,16)`, sedangkan `setWarna`
   dan layar `FakultasAction:637` menyimpan isi Textbox apa adanya tanpa
   validasi. Warna `"merah"`/`"#fff"` → `StringIndexOutOfBoundsException` /
   `NumberFormatException`, dan karena property `rgb` dipetakan (tanpa
   `@Transient`) kegagalannya bisa muncul saat Hibernate menyimpan, jauh
   dari layar pengisinya.
3. `getNama()` punya **cek mati**: `return this.nama == null ? null : ...`
   tidak pernah bernilai benar karena baris sebelumnya sudah menjamin
   non-null.
4. `toString()` membaca **field** `nama` langsung (bukan `getNama()`) →
   bisa mengembalikan `null`. Menyimpang dari `GeneralValueObject`
   (`"kode - nama"`) dan dari `Jurusan` (`"id-nama"`).
5. `getKode()` **tanpa normalisasi** sama sekali (beda dengan
   `Jurusan.getKode()` yang menyulih `"--"`).
6. `setRgb()` praktis tidak berguna (selalu tertimpa `getRgb()`).
7. Kunci polos `KOP_FAKULTAS`/`STEMPEL_FAKULTAS` di `putFile` saling
   menimpa bila dipanggil untuk beberapa fakultas pada map yang sama.

**Tidak ada temuan keamanan** (dicek eksplisit: tidak ada SQL string
concat, tidak ada kredensial, tidak ada I/O path dari input pengguna).

## `ais/database/model/HistoryStatusMahasiswa.java` — SELESAI 100% (2 Sep 2026)

Entity **riwayat status kemahasiswaan per semester** (tabel
`public.history_status_mahasiswa`, `@Audited`, `dynamicInsert/dynamicUpdate`).
525 → 1106 baris, **41/41 method ber-Javadoc (100%)** plus 15 field
didokumentasi. Revisi **r83176**, mirror `java/` diverifikasi byte-identik
(`cmp`). **Kode terbukti tidak berubah**: sumber yang dilucuti seluruh
komentar identik baris-per-baris dengan HEAD sebelum edit (410 baris kode
di kedua sisi); lulus `javac 1.7 -implicit:none`.

**Temuan arsitektur — ini SATU-SATUNYA tempat status per semester.**
`Mahasiswa` sama sekali TIDAK punya properti `statusMahasiswa`. Yang ada
di sana hanya `statusKeluar` (status terminal Lulus/Keluar/DO — nasib
akhir, bukan per semester), `kelompokStatusMahasiswa` (override rentang
`smtMulai..smtSampai` → satu status), dan `paksaAktifSemester`. Jadi nama
"History…" menyesatkan: ini bukan tabel arsip pendamping kolom "status
sekarang" — "status terkini" hanyalah baris riwayat pada semester berjalan
(`HistoryStatusMahasiswaUtil.currentStatus(...)`).

**Verifikasi pola berulang (getter tidak pasif).** `@Id` ada di
*getter* (`getId()`) → Hibernate memakai **property access**, sehingga
getter-getter inilah yang dibaca saat dirty-check/flush. Tujuh di antaranya
menulis balik ke field: `getMahasiswa()` (`check()`), `getStatusMahasiswa()`
(memicu SELURUH mesin aturan `ambilStatusMahasiswa`), `getSemester()`
(hitung lazy dari tahun akademik+angkatan, gagal → `0` bukan `null`),
`getGanjilGenap()`, `getStatusAwalMahasiswa()`, `getProgram()`, dan
`toString()`. Akibatnya **membaca bisa menulis ke DB** — satu `UPDATE`
plus satu revisi audit Envers — hanya karena baris riwayat ditampilkan di
layar. `getSks()` menormalkan `null`→`0`, jadi kolom `NULL` ikut tertulis
ulang jadi `0` saat flush. **Tidak ada getter yang menutup sesi Hibernate
sendiri** di file ini (resolusi session sepenuhnya didelegasikan ke
`GeneralValueObject.check()`).

**Setter dengan efek samping ke luar**: `setStatusAwalMahasiswa()` menulis
id status awal ke penyimpanan kunci-nilai milik `Mahasiswa` dengan kunci
`"sts_<semester>"` (`mahasiswa.put(nilai, kunci)` — urutan argumen
nilai-dulu), yang dibaca kembali oleh `ambilStatusAwal()`.
`setOleh()`/`setOlehId()` mengabaikan `null`/kosong (pola audit repo).

**Kuirk & bug tercatat (TIDAK diperbaiki, sesuai instruksi):**
1. `getGanjilGenap()` memanggil `getMahasiswa()` di baris pertama tetapi
   **hasilnya tidak dipakai sama sekali** — biaya `check()` sia-sia.
2. `getStatusAwalMahasiswa()` mengirim **field** `semester` mentah ke
   `ambilStatusAwal`, bukan `getSemester()`. Bila `semester` masih `null`
   semua aturan rentang semester terlewat. Beda perilaku dengan
   `getStatusMahasiswa()` yang memaksa `getSemester()` dulu.
3. `getProgram()` jalur "selalu ikut data utama" memanggil `getMahasiswa()`
   **tiga kali** dan mengembalikan program mahasiswa TANPA memperbarui
   field — nilai tampil bisa beda dari nilai kolom. Langkah berikutnya
   memakai field `mahasiswa`/`semester` mentah, bukan getter.
4. `ambilStatusAwal()`: ekspresi `(statusAwalMahasiswa == null ||
   mahasiswa.getStatusAwalSelaluIkutDataUtama()) && mahasiswa != null`
   men-dereference `mahasiswa` SEBELUM cek null-nya (aman hanya karena ada
   early-return `mahasiswa == null` di atas — cek `mahasiswa != null` itu
   dead code).
5. Pemetaan status membingungkan di `ambilStatusMahasiswa()`: nama status
   keluar yang mengandung `"keluar"` dipetakan ke **DROP_OUT**, sedangkan
   `ConstantValues.KELUAR` baru dipakai di cabang dalam untuk status keluar
   bernama lain.
6. Aturan tumpang tindih dengan `HistoryStatusMahasiswaUtil.kalkulasi
   StatusLogikaLanjutan` (Lulus/Keluar/DO retroaktif dievaluasi di KEDUA
   lapis) → mengubah salah satu saja menghasilkan status berbeda tergantung
   jalur pemanggilan (cache Util vs getter entity).
7. `retreive("checkStatusPembayaranMahasiswa")` bukan kolom DB, melainkan
   penyimpanan kunci-nilai per-instance `GeneralValueObject`; satu-satunya
   penulisnya `BaypassPembayaranMahasiswaAction`. Di luar jalur itu flag
   selalu kosong dan dua cabang aturan pembayaran tidak pernah aktif.
8. Beberapa `statusMahasiswa.getId().equals(...)` tanpa jaga `getId()!=null`
   — hanya selamat karena dibungkus `try/catch` terluar.

**Tidak ada temuan keamanan** (dicek eksplisit: tidak ada SQL string,
tidak ada kredensial, tidak ada I/O berkas berbasis input pengguna).
Field audit shadow (`id`/`oleh`/`olehId`/`tanggal_dirubah`) dideklarasi
ulang — sesuai arsitektur (`GeneralValueObject` bukan `@MappedSuperclass`).

## Batch "5 entity formulir/soal/pengumuman" — SELESAI 100% (2 Sep 2026, dikonsolidasi orkestrator)

Semua 5 file TUNTAS 100% method, dikompilasi, dikommit, di-mirror ke `java/`:
- `PengumumanAkademis.java` — 127/127 method. 1084→2552 baris. r83151.
  Kanal informasi serba-guna (bukan cuma "akademik") dengan 2 mekanisme
  distribusi (tarik via query + dorong via broadcast email 8 flag).
- `FormulirKegiatan.java` — 157/157 method. 932→2317 baris. r83145/83150.
- `TugasKelompok.java` — 68/68 method. 476→1427 baris. r83144/83146. Nama
  kelas menyesatkan (ini "penugasan"-nya, bukan kelompoknya — hierarki
  sebenarnya `TugasKelompok`→`NamaTugasKelompok`→`...PunyaMahasiswa`).
- `BankSoal.java` — 80/80 method. 656→1862 baris. r83151/83153.
- `SyaratUjian.java` — 90/90 method. 684→1986 baris. r83155/83159.
  **TEMUAN ARSITEKTUR PENTING** (menjawab pertanyaan yang menggantung sejak
  entity pertama): `GeneralValueObject` BUKAN `@Entity` atau
  `@MappedSuperclass` — POJO abstrak biasa. Hibernate TIDAK memetakan
  properti induknya sama sekali. Jadi field audit shadow (`oleh`/`olehId`/
  `tanggal_dirubah`/`id`/dst di semua 18 entity yang sudah digarap) BUKAN
  kelalaian/bug berulang — itu KEHARUSAN TEKNIS. Update pemahaman: jangan
  lagi sebut ini sebagai "pola mencurigakan", itu memang cara kerja yang
  benar untuk arsitektur ini.

**Kehilangan data destruktif ditemukan lagi** (pola sudah sangat familiar,
tidak dieskalasi tersendiri — cukup tercakup `task_15f5001e`):
`PengumumanAkademis.getHanyaUntuk()` (mengosongkan daftar NIM saat audiens
diubah), `FormulirKegiatan.getKodeItemBiaya()`, `SyaratUjian.getKodeMatakuliah()`
(sama persis pola `FormatNilaiSkripsi`/`FormatNilaiProposalSkripsi` sesi 7).
**Tidak ada temuan keamanan baru** di batch ini (dicek eksplisit tiap file).

**Total akumulasi 8 sesi kerja**: 213 (sesi 1-7) + 5 = **218 file** dari
7.401 (~2,9%). Field audit shadow: 100% di **18 entity** — SEKARANG
DIPAHAMI SEBAGAI ARSITEKTUR YANG BENAR, bukan pola mencurigakan.

## `ais/database/model/SyaratUjian.java` — SELESAI 100% (2 Sep 2026)

Entity **syarat kelayakan** (tabel `public.syarat_ujian`, `@Audited`,
`dynamicInsert/dynamicUpdate`) — satu baris = satu aturan yang menentukan
boleh-tidaknya mahasiswa melewati gerbang akademik: ujian online, pengumpulan
tugas, ambil/cetak KRS, cetak kartu UTS/UAS, cetak KHS. 684 → 1986 baris,
**90/90 method ber-Javadoc (100%)** plus 34 field didokumentasi. Revisi
**r83155**, mirror `java/` diverifikasi byte-identik (`cmp`). **Kode terbukti
tidak berubah**: bytecode `javap -c -p` identik dengan HEAD sebelum edit.

**Struktur**: `extends GeneralValueObject` langsung. Berbeda dari
`VirtualAccountBank`/`ItemBiaya`, entity ini **hampir seluruhnya pasif** —
murni data konfigurasi. Seluruh logika evaluasi ada di SATU method di luar:
`ais.action.master.SyaratUjianAction.checkSyaratSyaratUjian(SyaratUjian,
VOPembelajaran, Mahasiswa, Integer semester, String namaSyarat, KrsMahasiswa,
List<String> warnings)` (`SyaratUjianAction.java:1132`), `true` = boleh lewat.
Method di entity ini hanya getter/setter + pengelola string terpadat
`syaratPembayaran` (`populateJadwal`/`hapusJadwal`/`daftarJadwal`) + pengurai
angka defensif `parseAngkaAman`.

**Penting dicatat — jebakan penamaan**: `UtsDanUasCheckerHelper`,
`CommonValidationHelper`, dan `CommonPaymentHelper` **TIDAK** mengevaluasi
entity ini. Dua terakhir hanya menyimpan `import` mati; yang pertama adalah
gerbang pembayaran TERPISAH yang dikendalikan `Konfigurasi` dan bekerja atas
`JenisKegiatan` (cache `CommonHelperClass.jenisKegiatansUntukSyaratUjian`).
Jangan tertukar — kemiripan namanya menyesatkan.

**Dua lapis kerja**: (1) pemanggil menyaring baris mana yang relevan lewat
Criteria memakai bendera gerbang `krs`/`uts`/`uas`/`nilai`/
`berlakuUntukSemuaUjian`/`berlakuUntukSemuaTugas`/`statusPertemuan` — bendera
ini TIDAK PERNAH dibaca mesin penilaian; (2) tiap baris dinilai berurutan
(SKS → angka kredit → IPK → kehadiran → MK ATAU → MK DAN → jadwal pembayaran →
item biaya DAN → item biaya ATAU), berhenti pada kegagalan pertama.
**Lapis lingkup** (`minimalSmt`/`maksimalSmt`, `minimalAngkatan`/
`maksimalAngkatan`, `fakultas`/`jurusan`/`jenjang`/`program`,
`statusAwalMahasiswa`, `ta`) bila TIDAK cocok membuat aturan **dilewati dan
mahasiswa dianggap LOLOS** — bukan diblokir.

**Field audit shadow: KONFIRMASI KE-18 BERTURUT-TURUT.** 6 field induk
dideklarasikan ulang (`id`, `nama`, `keterangan`, `oleh`, `olehId`,
`tanggal_dirubah`). **Sebab akarnya akhirnya terdokumentasi**:
`GeneralValueObject` adalah POJO abstrak biasa — **BUKAN `@Entity` maupun
`@MappedSuperclass`** — sehingga Hibernate tidak memetakan properti induk sama
sekali; deklarasi ulang di tiap subclass adalah **keharusan teknis**, bukan
kelalaian. Ini menjelaskan pola di 17 entity sebelumnya sekaligus.

**Temuan lain (dicatat jujur, TIDAK diperbaiki)**:
- `getKeterangan()` di sini **melanggar jaminan non-null induk** (induk
  mengembalikan `""`, override ini bisa `null`) — memengaruhi cabang
  `keterangan` pada `GeneralValueObject.compareTo`.
- **4 getter berefek samping** menulis balik ke field: `getKodeMatakuliah()`,
  `getKodeMatakuliahDan()`, `getFakultas()`, `getTa()`. Terparah: membaca
  `getKodeMatakuliah()` saat `tidakWajibMengambilMkTertentu=true`
  **MENGHAPUS PERMANEN** daftar mata kuliah dari DB — membatalkan centangnya
  kemudian tidak mengembalikannya.
- `getFakultas()` menimpa nilai tersimpan dengan `getJurusan().getFakultas()`,
  sehingga penyaring Criteria `fakultas IS NULL OR fakultas = ?` bisa memberi
  hasil berbeda sebelum vs sesudah baris pernah dibaca.
- `getTa()` selalu hitung ulang ⇒ `setTa()` praktis tidak berguna; bila parse
  gagal, field mempertahankan nilai **LAMA (basi)**, tidak direset.
- `ambilPersen()`/`ambilSemester()`/`ambilBulan()` = **DEAD API** (nol
  pemanggil di seluruh pohon sumber) dan ketiganya menangani record rusak
  dengan **tiga cara berbeda** (return default / return default / lanjut).
- `daftarJadwal()`: catch kosong **membuang record rusak dari hasil**, jadi
  syarat pembayaran itu diam-diam tidak lagi diberlakukan.
- `populateJadwal()`: parameter `keterangan` **sia-sia** (selalu ditimpa
  kalimat bangkitan); penjaga item kosong tidak pernah benar sehingga memilih
  "Semua" menghasilkan teks `"item biaya 0"`.
- `StringUtils.split(s,"||")` memakai **himpunan KARAKTER**, bukan string
  pemisah utuh — `||` bekerja secara kebetulan, tapi `|`/`<`/`>` tunggal tetap
  merusak record.
- Ruas `item` (indeks 5) pada `syaratPembayaran` **tersimpan & ditampilkan tapi
  tidak pernah dinilai** mesin (combobox-nya memang `setVisible(false)`).
- `kodeItemBiaya` (label layar **"DAN"**) hanya memblokir bila **belum bayar
  sama sekali** (`jumlah < 0.01`), bukan bila belum lunas.
- `kodeItemBiayaOr` (label layar **"ATAU"**): kombinasi "kode A lunas penuh,
  kode B belum dibayar" **tetap MEMBLOKIR** — melanggar semantik ATAU.
- Field `nilai` berlabel layar **"KHS"** (cetak Kartu Hasil Studi), bukan
  gerbang input nilai. Jangan diganti nama tanpa migrasi:
  `Restrictions.eq("nilai", true)`.
- `berlakuUntukSemuaTugas` hanya diperluas gerbang unggah berkas; jalur tugas
  mandiri/kelompok tidak memakainya — cakupannya lebih sempit dari namanya.
- `hanyaBolehDiubahOlehAdmin` murni penguncian UI, **bukan** kontrol otorisasi.

**Tidak ada kerentanan keamanan baru** yang ditemukan di file ini.

## `ais/database/model/BankSoal.java` — SELESAI 100% (2 Sep 2026)

Entity **bank soal ujian** (tabel `public.bank_soal`, `@Audited`) — satu baris = satu
butir soal yang bisa dipakai ulang lintas ujian/kuis/grup soal. 656 → 1862 baris,
**80/80 method ber-Javadoc (100%)** (diaudit skrip, 0 tersisa) plus 9 konstanta jenis
soal dan seluruh field entity. Revisi **r83151** (tersapu ke revisi gabungan sesi
paralel, isi diverifikasi `svn diff -c 83151`). Mirror `java/` byte-identik (`cmp`).
**Kode terbukti tidak berubah**: bytecode `javap -c -p` identik dengan versi HEAD
sebelum edit.

**Struktur**: `extends GeneralValueObject`. Rantai: `BankSoal` → `BankSoalDetail`
(opsi/kunci jawaban) ; `BankSoal` ← `UjianPunyaSoal` → `Ujian` ; jawaban peserta di
`HasilUjianMahasiswaDetail`/`JawabanPercobaanKuisKursus`. Kepemilikan lewat dua rumpun
relasi paralel: jalur PT (`Fakultas`/`Jurusan`/`Dosen`/`Matakuliah`) dan jalur sekolah
(`Yayasan`/`Sekolah`/`Guru`/`Matapelajaran`), plus `SatuanKerja`, `KategoriBankSoal`,
`PenjelasanBankSoal`.

**Temuan utama:**

- **Dua sumbu “jenis soal”.** `jenis`/`jenisKoreksi` cuma punya 2 nilai efektif
  (`ESAY`/`PILIHAN_GANDA`) dan saling menormalkan; yang benar-benar dipakai semua layar
  ujian adalah `jenisPilihanGanda` dengan 7 nilai. Konstanta `MENGURUTKAN`,
  `MENJODOHKAN`, `BENAR_SALAH`, `JAWABAN_SINGKAT`, `RUMPANG` **tidak pernah** menjadi
  nilai `getJenis()` walau namanya sekelompok. `setJenis()` praktis tak berguna.
- **Daftar `BankSoalDetail` bukan `@OneToMany`** melainkan cache berkas JSON per soal,
  dipelihara otomatis oleh `AuditListener` pada save/delete detail.
- `ambilBankSoalDetail(true)` **menutup sesi Hibernate thread-local**
  (`HibernateUtil.closeSession()`) — pola berulang, bisa membuat entity pemanggil
  mendadak detached.
- **Kehilangan data senyap**: untuk `PILIHAN_GANDA` daftar opsi dibangun lewat
  `TreeMap` berkunci `getHuruf()`, jadi dua opsi berhuruf sama saling menimpa dan satu
  hilang tanpa error; urutannya juga leksikografis.
- `ambilBankSoalDetailBenar()` **mati** (nol pemanggil) **dan salah nama**: menyaring
  `!getBetul()` sehingga mengumpulkan opsi yang SALAH.
- `ambilSatuBankSoalDetailEssay()` mengembalikan kecocokan **terakhir** (tanpa
  `break`), padahal urutan sumbernya tidak stabil untuk jenis non pilihan-ganda.
- `removeBankSoalDetail()` menulis nisan `""` alih-alih menghapus kunci → berkas cache
  tumbuh terus. `populateBankSoalDetail()` punya parameter `tulisUlang` yang tidak
  dipakai sama sekali.
- `getYayasan()` menimpa field dari sekolah **termasuk bila hasilnya `null`** (asimetris
  dengan `getSatuanKerja()` yang menjaga nilai lama bila induk kosong).
- `BankSoal.jsonObject` adalah `public static` `JSONArray` **mutable** yang dipakai
  bersama semua instance sebagai default `getOpsiSoal()`.
- Salah eja load-bearing: `PenjelasanBankSoal.KOREKSI_OTOMATIS` = `"Hasil dikoreksi
  otomtais"` (tersimpan di produksi, jangan diperbaiki tanpa migrasi);
  `MULTIPLE_COICE` salah eja pada nama konstantanya saja.

**Field audit shadow: KONFIRMASI ke-18 berturut-turut** (`oleh`/`olehId`/
`tanggal_dirubah` + `@PreUpdate onUpdate()` berdesakan di satu baris, setter
`oleh`/`olehId` menolak null/kosong diam-diam). Getter berefek samping: HADIR
(9 method). Getter menutup sesi Hibernate: HADIR.

## `ais/database/model/FormulirKegiatan.java` — SELESAI 100% (2 Sep 2026)

Entity **formulir pendaftaran kegiatan** (tabel `public.formulir_kegiatan`,
`@Audited`) — satu baris = satu kegiatan yang dibuka pendaftarannya (seminar,
workshop, kegiatan kemahasiswaan/kesiswaan/kedosenan) lengkap dengan panitia,
pembicara 1..3, jadwal, kuota, gerbang syarat pembayaran, dan template tanda
tangan formulir cetak. 932 -> 2317 baris, **157/157 method ber-Javadoc (100%)**
(diaudit skrip, 0 tersisa) plus 60+ field entity didokumentasi. Revisi
**r83145**, mirror `java/` diverifikasi byte-identik (`cmp`). Kode terbukti
tidak berubah: diff versi tanpa komentar terhadap HEAD lama identik.

**Struktur**: `extends VOPembelajaran extends VoKunci extends DataSop ...
extends GeneralValueObject`, sekaligus `implements VOPesertaPembelajaran`
(kuirk: kelas pembelajaran mengimplementasikan antarmuka *peserta*;
`ambilVOPembelajaran()` hanya mengembalikan `this`). Peserta disimpan terpisah
di `FormulirKegiatanPeserta` (mahasiswa/dosen/siswa/guru/pegawai). Hanya 3
method berlogika nyata (`ambilDataPerkuliahans`, `ambilDataDosens`,
`ambilJumlahDetailperkuliahanLangsung`), sisanya accessor — tapi banyak
accessor yang TIDAK murni.

**Temuan penting**:
- `getKodeItemBiaya()` **mengosongkan kolomnya sendiri secara permanen**
  begitu `syaratUjian` terisi (`kodeItemBiaya = ""` di dalam getter). Nilai
  lama hilang dan ikut tersimpan bila objeknya managed; melepas syarat ujian
  tidak mengembalikannya. Kasus getter-merusak-data terparah di entity ini.
- Default `getTahunAkademik()`/`getSemester()` dihitung **saat baca**
  (tahun akademik/semester berjalan), bukan saat simpan — baris berkolom
  kosong "ikut berpindah" tahun; berpengaruh langsung ke `id_semester` yang
  dikirim ke feeder PDDikti dan ke perhitungan semester tagihan.
- Getter berefek samping lain: `getAktif()` (tulis `true`),
  `getHanyaUntukAngkatan()` (tulis hasil normalisasi CSV),
  `getWaktumulai()`/`getWaktusampai()` (bikin default 08:00/17:00 dan
  menuliskannya; milidetik tidak di-reset), `getJenisAktfitasMahasiswa()`,
  serta seluruh getter relasi ber-`check()`.
- `getPesertaGuru()` default **false** sementara tiga saklar peserta lain
  default **true** — kegiatan lama otomatis tertutup bagi guru.
- `perkuliahans` adalah CSV id `Perkuliahan` tanpa foreign key; id yatim
  ditelan diam-diam oleh `ambilDataPerkuliahans()`.
- `feeder` (kolom `feederdata`) bukan satu id melainkan peta JSON
  `{idJurusan: id_aktivitas}` — satu aktivitas PDDikti per jurusan peserta.
- Javadoc class lama berbunyi "Bank generated by hbm2java" (salin-tempel
  generator, tidak ada hubungannya dengan entity `Bank`) — diganti.
- `serialVersionUID` sama persis dengan `FormulirKegiatanPeserta`,
  `JenisFormulirKegiatan`, dan `GrupFormulirKegiatan`.

**Field audit shadow: KONFIRMASI ke-18 berturut-turut** (`oleh`/`olehId`/
`tanggal_dirubah` + `@PreUpdate onUpdate` -> `AuditTimestampInterceptor.ubah`),
termasuk varian setter yang mengabaikan nilai kosong diam-diam. Tidak ada
getter yang menutup sesi Hibernate di file ini
(`ambilJumlahDetailperkuliahanLangsung()` memakai `currentSession()` dan
membiarkannya terbuka). Tidak ditemukan kerentanan keamanan baru.

## `ais/database/model/TugasKelompok.java` — SELESAI 100% (2 Sep 2026)

Entity **penugasan kelompok** (tabel `public.tugas_kelompok`, `@Audited`).
476 → 1427 baris, **68/68 method ber-Javadoc (100%)** + seluruh field entity.
Revisi **r83144**. Mirror `java/` diverifikasi byte-identik (`cmp`). **Kode
terbukti tidak berubah**: `javap -c -p` identik dengan versi HEAD sebelum edit.

**Peringatan penamaan (temuan utama)**: `TugasKelompok` BUKAN sebuah kelompok
mahasiswa — ia adalah *penugasannya*. Hierarki sebenarnya tiga tingkat:
`TugasKelompok` (tugas) → `NamaTugasKelompok` (kelompok, kolom
`nama_tugas_kelompok.tugas_kelompok`) → `NamaTugasKelompokPunyaMahasiswa`
(keanggotaan). "Berapa kelompok di tugas ini" dijawab dari `NamaTugasKelompok`.

**Struktur**: `extends Tugas` (abstract) → `extends GeneralValueObject`.
Saudara kembarnya `TugasPertemuan` (tugas mandiri) — banyak method di sini
salinan persis; cacat di satu sisi hampir pasti ada di sisi lain. Seluruh mesin
berkas jawaban (indeks JSON `tugas_file_content_<id>`) ada di `Tugas`, bukan di
sini.

**Tiga kolom JSON yang gampang tertukar** (didokumentasikan sebagai tabel di
Javadoc kelas):
- `formatNilais` (kolom `format_nilais`) — kunci = id `FormatNilai`, isi bobot.
  Sekaligus **saklar mode OBE**: selama isinya masih literal `Tugas.JSON`
  (`"{}"`), tugas dianggap penilaian standar.
- `keteranganNilai` (kolom `keterangan_nilai_baru`) — nilai OBE per mahasiswa
  per komponen, kunci `"<idMahasiswa>_mhs_nilai_<idFormatNilai>"` → double.
- `keteranganNilaiLama` (kolom `keterangannilai`) — versi lama, hanya cadangan;
  `getKeteranganNilai()` jatuh ke sini bila kolom baru kosong.

Konsekuensi yang dicatat: nilai OBE tidak bisa di-query SQL (tak ada JOIN/
agregasi), dua penyimpan bersamaan saling menimpa nilai SEMUA mahasiswa
(last-write-wins tingkat baris), dan beberapa pembaca memanggil
`.replace('\0',' ')` sebelum parse — bukti byte NUL pernah menyusup ke kolom
teks ini di produksi.

**Getter tidak murni (menulis balik ke field terpetakan)** — 7 buah:
`getPerkuliahan()`, `getKelompokKkn()`, `getKelompokPkl()`,
`getJadwalPelajaran()` (empat pertama ber-`cascade PERSIST/MERGE`, menurunkan
ulang nilai dari `Pertemuan` induk sehingga selalu menang atas setter),
`getMhsYgTidakIkut()`, `getMhsBolehUploadUlang()` (normalisasi CSV
koma-terbungkus, ditulis balik), dan `getAktif()` (nilai turunan
`!getJudultugas().isEmpty()` ditimpakan ke field yang TIDAK `@Transient` —
`setAktif()` praktis tak berguna).

**Getter yang memukul cache/DB**: `getJadwalPelajaran()` memanggil
`ambilPertemuan()` → `DataUtil.ambilData(Class,String)`. Inkonsisten dengan
tiga saudaranya yang memakai `getPertemuanData()` di memori — dicatat, tidak
diperbaiki.

**Pemetaan ganda kolom** (satu sisi baca-saja): `pertemuan` →
`getPertemuan()` (Long, writable) + `getPertemuanData()` (`@ManyToOne`,
`insertable/updatable=false`, `@NotFound(IGNORE)` sehingga baris yatim jadi
`null`); `judul` → `getJudul()` + `getJudultugas()`; `nama` → `getNama()` +
`getIsitugas()`.

**Kuirk lain**: `setOleh()`/`setOlehId()` menolak nilai kosong diam-diam (audit
tak pernah bisa dikosongkan lewat setter); `toString()` membaca field `nama`
langsung sehingga bisa `null` padahal `getNama()` menormalkannya; normalisasi
koma ganda hanya 3 iterasi, bukan sampai konvergen; `DEFAULT_FORMULA` dan
`Tugas.JSON` `public static` tanpa `final`.

**Field audit shadow: KONFIRMASI ke-18 berturut-turut.** `oleh`, `olehId`,
`tanggal_dirubah`, dan hook `onUpdate()` dideklarasikan ulang identik dengan
milik `GeneralValueObject` (baris 360–440). Pola ini PASTI ADA.

Tidak ada temuan keamanan baru pada file ini (tanpa SQL/command dinamis).

## Batch "5 entity pembayaran/kurikulum/keluarga" — SELESAI 100% (2 Sep 2026, dikonsolidasi orkestrator)

Semua 5 file TUNTAS 100% method, dikompilasi, dikommit, di-mirror ke `java/`
(verifikasi `cmp` byte-identik):
- `VirtualAccountBank.java` — 135/135 method. 2376→3949 baris. r83134/83136.
  Bukan sekadar entity — MESIN pembayaran VA dipanggil 17 servlet callback
  bank berbeda. **Potensi command injection ditemukan** di `curlSmartlink()`
  (payload JSON disisipkan mentah ke command line `curl`/`ssh`) — DIESKALASI
  ke task keamanan terpisah `task_b0a90191` (kategori BEDA dari audit
  arsitektur getter, severity lebih tinggi, TIDAK diperbaiki di sesi ini).
- `ItemBiaya.java` — 70/70 method. 946→2263 baris. r83133/83135. Peringatan
  penting: konstanta salah eja (`"suatau matakuliah tertentu"`) adalah
  LOAD-BEARING di data produksi — JANGAN diperbaiki ejaannya tanpa migrasi
  data, akan mematikan mode itu di baris lama.
- `CicilanPembayaran.java` — 73/73 method. 620→1666 baris. r83129/83132.
  Kasus "getter menulis ke object lain" terparah sejauh ini: `getKegiatan()`
  bisa mengubah+menyimpan rentang tanggal bayar `Kegiatan` INDUK hanya
  dengan dibaca (cascade MERGE/PERSIST).
- `KurikulumPunyaMatakuliah.java` — 79/79 method. 749→1670 baris.
  r83128/83131. Konfirmasi: RPS milik KURIKULUM bukan mata kuliah (bisa beda
  antar tahun kurikulum untuk MK yang sama).
- `OrangTua.java` — 90/90 method. 777→1774 baris. r83130. **Konfirmasi**:
  data orang tua di `OrangTua` vs `BiodataMahasiswa` TUMPANG TINDIH TAPI
  TIDAK PERNAH disinkronkan, bahkan pakai tabel acuan master berbeda. Bug
  fungsional serius: 3 catch kosong di method penentu akses portal wali bisa
  membuat wali "kehilangan" semua data anaknya secara diam-diam.

**Field audit shadow: 100% konsisten di SEMUA 17 entity yang sudah digarap**
(termasuk batch ini, entity ke-13 s/d ke-17 berturut-turut) — pola ini bukan
lagi "kemungkinan", anggap PASTI ADA di entity model manapun yang belum
disentuh.

**Dua task eskalasi terpisah aktif** (di luar inisiatif dokumentasi ini):
`task_15f5001e` (audit arsitektur getter-berefek-samping, kluster besar) dan
`task_b0a90191` (command injection `curlSmartlink`, BARU sesi ini — kategori
keamanan berbeda, jangan digabung dengan yang pertama).

**Total akumulasi 7 sesi kerja**: 208 (sesi 1-6) + 5 = **213 file** dari
7.401 (~2,9%).

## `ais/database/model/VirtualAccountBank.java` — SELESAI 100% (2 Sep 2026)

Entity **tagihan Virtual Account** (tabel `public.virtual_account_bank`, `@Audited`)
— satu baris = satu permintaan pembayaran yang diterbitkan AIS untuk dibayar lewat
bank/payment gateway. 2376 → 3949 baris, **135/135 method ber-Javadoc (100%)**
(diaudit skrip, 0 tersisa) plus 45 field entity didokumentasi. Revisi **r83134**.
Mirror `java/` diverifikasi byte-identik (`cmp`). **Kode terbukti tidak berubah**:
bytecode `javap -c -p` (kelas utama + anonymous `VirtualAccountBank$1`) identik
dengan versi HEAD sebelum edit.

**Struktur**: `extends GeneralValueObject` langsung. Bukan sekadar entity — di
dalamnya tinggal **mesin pembayaran VA** berupa method static yang dipanggil
langsung oleh 17 servlet callback bank (`ais/action/servlet/Briva`, `BSI`,
`Bankaltimtara`, `Bjb`, `BCA`, `Mandiri`, `Nagari`, `OcbcNisp`, `MncBank`,
`Finpay`, `Flip`, `Esmartlink`, `Otto`, `Maja`, `Jaring`, `BMS`, `Va`) dan oleh
`ais/action/master/helper/virtualaccount/*`. Dua jalur posting terpisah:
`bayarVa()` (mahasiswa/calon mahasiswa → `Kegiatan` + `CicilanPembayaran`) dan
`bayarSiswa()` (siswa/calon siswa → `PembayaranSiswa` + `PembayaranSiswaDetail`),
plus `bayarTopup()` untuk saldo `Deposit`. Enam alternatif pemilik tagihan
(mahasiswa, calon mahasiswa, siswa, calon siswa, anggota koperasi, peserta
kursus). Rincian biaya disimpan sebagai **token teks** di kolom `cicilan`, dan
**format token itu berbeda antara jalur mahasiswa** (`Bulanan-`/`Item-`/
`Keranjang-`) **dan jalur siswa** (`<apa saja>-<idTagihan>[-<nilai>]`).

**Field audit shadow TERKONFIRMASI lagi** (entity ke-12 berturut-turut, 100%):
- `getWaktuBayar()` **mengosongkan** `waktuBayar` bila 3 penanda posting kosong;
- `getAkunPembayaranSiswa()` **mengosongkan** field bila akun tabungan/manual →
  risiko FK ternull saat flush;
- `getKegiatan()` membaca **cache berkas samping** (`retreive("k")`/`"hapus"`) dan
  bisa menyetel `kegiatan = null`; `setKegiatan()` menulis berkas itu;
- menimpa field tanpa mengosongkan: `getKode()`, `getNama()`, `getBank()`,
  `getSemester()`, `getTahunAkademik()`, `getBiayaAdmin()`, `getAmount()`,
  `getKadaluarsa()`, `getKadaluarsaWaktu()`, `getPt()`, `getKanalPembayaran()`,
  `getKelas()`, `getChannel()`, bahkan `toString()`.
- Getter yang **membuka & menutup session Hibernate sendiri**: `ambilVa()` (3
  overload), `ambilLink()`, `ambilByNisAja()`, `updateVa()`, `updateTotal()` —
  `ambilVa`/`ambilLink` bahkan **memperbaiki baris di DB** (mengisi `waktuBayar`,
  `bankHost`, membuat `Va`) saat sekadar dicari.

**Temuan lain (dicatat, TIDAK diperbaiki)**:
1. `buatAtauChekTagihan()` memakai `Restrictions.eq("refTagihan", tag)` padahal
   entity **tidak punya properti `refTagihan`** (grep seluruh pohon: hanya muncul
   di baris ini). Query dieksekusi di luar `try` → `QueryException` merambat ke
   `ambilByNisAja()` yang menelannya, jadi **jalur pembuatan VA otomatis dari NIS
   praktis mati total**. Memperbaikinya justru MENGAKTIFKAN fitur yang selama ini
   tidak jalan — perlu pengujian, bukan patch buta.
2. `getAmount()` cabang e-Smartlink non-SUCCESS menghitung `amount = total + total`
   padahal syarat cabangnya memeriksa `biayaAdmin != null` (mestinya
   `total + biayaAdmin`, seperti cabang terakhir).
3. `getTahunAkademik()` argumen fakultas:
   `mahasiswa == null || mahasiswa.getJurusan() != null ? null : mahasiswa.getJurusan().getFakultas()`
   — cabang else hanya tercapai saat jurusan `null`, jadi pasti NPE. Kondisi
   tampak terbalik; method tidak menangkap exception.
4. `updateTotal()` menulis kolom `total` lewat **thread terpisah** yang `sleep(500)`
   dulu, SQL native, session sendiri → method kembali sebelum DB berubah, kegagalan
   tidak pernah sampai ke pemanggil.
5. Cache statis `sukses` (`Set<Long>`) tidak pernah dikosongkan massal → tumbuh
   selama JVM hidup dan tidak dibagi antar node. Sudah didampingi pengecekan
   permanen ke DB, jadi bukan bug korektnes, tapi catat untuk deployment multi-node.
6. `getTotalAman()` redundan — `getTotal()` sudah menormalkan `null` ke `0.0`.
7. `updateVirtualAccountSiswaMinimal()` sengaja memakai bulk update HQL +
   `FlushMode.MANUAL` justru **untuk menghindari getter-getter perusak di atas**.
   Ini bukti tim sudah menyadari masalahnya dan menambal per kasus.

**KEAMANAN — perlu diwaspadai (dicatat di Javadoc, nilai TIDAK disentuh)**:
`curlSmartlink()` / `curlSmartlinkGet()` mengubah username+password e-Smartlink
menjadi header `Authorization: Basic ...` lalu menaruhnya sebagai **argumen baris
perintah `curl`**; bila konfigurasi `curl_e_smartlink_via_server_lain` aktif,
header itu menjadi bagian string perintah yang dieksekusi shell di host lain lewat
`ssh` (terbaca lewat `ps`/riwayat shell). Payload `postData.toString()` disisipkan
di antara kutip tunggal `--data-raw '...'` → **berpotensi command injection** bila
JSON memuat kutip tunggal. Default relay ter-hardcode di kode: IP `38.47.178.46`,
port `22031`, user `zishof` — dan `Common.getKonfigurasi` **menulis default ke DB**
bila barisnya belum ada, sehingga default ini bisa menjadi nilai produksi. Tidak
ada password/API key literal di file ini (username/password diterima sebagai
parameter dari pemanggil).

## `ais/database/model/ItemBiaya.java` — SELESAI 100% (2 Sep 2026)

Entity master **item biaya** (tabel `item_biaya`) — katalog komponen tagihan (SPP,
uang gedung, uang praktikum, denda, diskon). 946 → 2263 baris, **70/70 method
ber-Javadoc (100%)** (diaudit skrip) + seluruh 37 konstanta mode penghitungan,
`PENGHITUNGAN_MAP`, blok inisialisasi statis, dan semua field privat.
Revisi **r83133**. Mirror `java/` diverifikasi byte-identik dengan `cmp`. Kode
diverifikasi TIDAK berubah sama sekali (perbandingan sumber lama vs baru setelah
seluruh komentar dilucuti → `IDENTICAL`).

**Struktur**: `extends GeneralValueObject` langsung. **TIDAK menyimpan nominal
sama sekali** — nominal ada di `DetailSettingBiaya.defaultBiaya` (template) dan
`DetailBiaya.nilaiBiaya`/`nilaiBiayaBaru` (baris tagihan nyata). Rantai:
`ItemBiaya` → `SettingBiaya`/`DetailSettingBiaya` → `DetailBiaya` →
`DetailKegiatan` → `CicilanPembayaran`/`BuktiPembayaran`/`DendaPembayaran`.
Kelompok anggota: (1) katalog mode penghitungan, (2) jejak audit, (3) identitas
item, (4) aturan penghitungan nominal, (5) aturan denda, (6) aturan cicilan &
tampilan, (7) batas semester/tanggal, (8) **8 method resolver akun** (`ambilAkun`,
`ambilPiutang`, `ambilDibayarDimuka`, `ambilPendapatanDenda`, masing-masing 2
bentuk) — satu-satunya kelompok yang menyentuh DB.

**Resolusi akun**: 5 tabel jembatan (`ItemBiayaPunyaAkun`/`Piutang`/
`DibayarDimuka`/`PendapatanDenda`/`Diskon`; yang terakhir TIDAK punya resolver di
entity). Algoritma pencarian berjenjang **8 tahap identik** di keempat resolver,
didokumentasikan lengkap sekali di `ambilAkun(Fakultas,Jurusan,String,String)`,
tiga lainnya `@see` ke sana (strategi reference-class+link).

**Pola field audit shadow — TERKONFIRMASI (entity ke-12 berturut-turut)**:
- `getDeskripsi()` menulis `getNama()` ke field bila kosong → **kolom `deskripsi`
  permanen berisi salinan nama** setelah sekali dibaca dalam session ter-flush.
- `getAktif()`→`true`, `getPenghitungan()`→`TIDAK_ADA_PENGHITUNGAN`,
  `getMaxSmt()`→`30`, `getAutoCreate()`/`getNilaiBisaDiubah()`/
  `getTerhubungKeNilaiTambahan()`→`false` semuanya menulis balik ke field.
- `getParameterTambahan()`/`getJenisPembayaran()` menulis hasil `check()` (pola
  standar, disengaja).
- Sisanya memakai bentuk aman `x == null ? default : x`. **Tidak ada** getter yang
  membuka/menutup session Hibernate di file ini.

**Temuan lain (dicatat, TIDAK diperbaiki)**:
- `tidakDitagihDiSmtGanjil`/`Genap` **tidak pernah ditegakkan** — hanya dibaca &
  ditulis `ItemBiayaAction`. Operator bisa salah kira item tak ditagih.
- `minSmt`/`maxSmt` hanya memicu teks peringatan di `DaftarUlangMahasiswa*Action`,
  bukan penyaring tagihan.
- `setAutoCreate(true)` tidak pernah dipanggil kode Java mana pun.
- `DIAMBIL_DARI_DENDA_PERPUSTAKAAN` ("456") bisa dipilih operator tapi **tanpa
  penangan** di mana pun.
- `DIKALI_JUMLAH_SKS_UAS_REMDIAL` ("5592") **kehilangan cabang** di jalur
  `DetailBiaya` biasa `PembayaranNominalModifikasiHelper` — tempatnya terisi blok
  **kembar persis** `DIKALI_JUMLAH_SKS_UTS_REMEDIAL` (baris ~1098 dan ~1131, cabang
  kedua tak terjangkau). Penangannya hanya ada di jalur tagihan bulanan.
- 7 konstanta blok "BARU" (`DIKALI_JUMLAH_PERTEMUAN` dkk) tidak ada di
  `PENGHITUNGAN_MAP` dan tidak dirujuk di mana pun.
- **Salah eja load-bearing**: teks `DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU` berbunyi
  "suatau" dan itulah yang tersimpan di DB — memperbaikinya akan mematikan mode ini
  pada data lama. Nama kolom `dendaAkanBerlipatTerlambaHari` ikut salah eja karena
  `MyNamingStrategy` (turunan `DefaultNamingStrategy`) memakai nama properti apa
  adanya.
- `ambilAkun(...)` TIDAK dibungkus `try/catch` sementara tiga saudaranya dibungkus
  → kegagalan query merambat ke pemanggil pada akun pendapatan saja.
- Denda: bila `JenisKegiatan.getDendaJikaTerlambat()` true, **seluruh** parameter
  denda diambil dari `JenisKegiatan` dan pengaturan di `ItemBiaya` diabaikan
  (`DetailBiaya.checkDenda`).
- Di luar file ini: `ItemBiayaAction` memanggil
  `session.createCriteria(ItemBiayaPunyaDenda.class)` padahal
  `ais.action.master.akunting.helper.ItemBiayaPunyaDenda` adalah **helper UI ZK,
  bukan entity** (entity-nya `ItemBiayaPunyaPendapatanDenda`). Perlu diperiksa
  sesi lain.

## `ais/database/model/CicilanPembayaran.java` — SELESAI 100% (2 Sep 2026)

Entity **transaksi cicilan/angsuran pembayaran** mahasiswa & calon mahasiswa
(tabel `cicilan_pembayaran`, `@Audited`). 620 → 1666 baris, **73/73
method+konstruktor ber-Javadoc (100%)**. Revisi: **r83129** (pesan utuh, tidak
tersapu). Mirror `java/` diverifikasi byte-identik dengan `cmp`. Kode
diverifikasi TIDAK berubah (bandingkan sumber lama vs baru setelah seluruh blok
javadoc dilucuti).

**Struktur**: `extends GeneralValueObject` langsung. Rantai domain:
`ItemBiaya → DetailBiaya → PengaturanPembayaranBulanan` (konfigurasi) dan
`Kegiatan → CicilanPembayaran` (transaksi). Relasi induk-anak **tidak** memakai
koleksi Hibernate: `Kegiatan` menyimpan string penanda `cicilans`
(`",<id>:true,<id>:false,"`) yang dirawat lewat `Kegiatan.appendCicilan` dan
`KegiatanPersistenceHelper`. Cicilan lahir dari 4 jalur: loket/daftar ulang,
unggah massal, virtual account/host-to-host (belasan servlet gateway), dan
payment gateway lama (Doku).

**Verifikasi pola berulang — KEDUANYA TERKONFIRMASI (entity ke-12 berturut-turut)**:

1. **Field audit bayangan**: `id`, `keterangan`, `oleh`, `olehId`,
   `tanggal_dirubah` dideklarasikan ulang, membayangi field `private` induk.
   Di sini konsisten (semua accessor ikut di-override) sehingga `equals()`
   induk yang memanggil `getId()` virtual tetap benar — **bukan** varian bug
   id anak-vs-induk seperti `Skripsi`/`MahasiswaRequestTugasAkhir`.
2. **Getter menulis balik**: 13 getter tidak murni — `getKegiatan`, `getNilai`,
   `getItemBiaya`, `getDetailBiaya`, `getKeterangan`, `getValidator`,
   `getTahap`, `getIdLampiran`, `getBayarKe`, `getTanggalTagihan`,
   `getNilaiAsli`, `getJenisPembayaran`, `getJenisTabungan`.
   `getKegiatan()` adalah kasus terparah yang ditemui sejauh ini: getter
   menulis ke **object lain** (`Kegiatan.setTanggalBayarAwal/Terakhir`) pada
   relasi ber-`CascadeType.MERGE`, sehingga sekadar membaca cicilan bisa
   mengubah + menyimpan rentang tanggal bayar kegiatan induk (dan rentang itu
   hanya pernah melebar, tak pernah menyempit). Tidak ada getter yang menutup
   sesi Hibernate di file ini.

**Temuan lain (dicatat, TIDAK diperbaiki)**:

- `Kegiatan.getValidator()` mengembalikan `"-"` (bukan string kosong) bila
  kosong ⇒ cek `getValidator().trim().isEmpty()` di `getKeterangan()` praktis
  selalu `false` ⇒ deskripsi kwitansi hampir selalu memuat `", validator : -"`.
- `getTanggalTagihan()`: tanpa penjagaan `null`/format pada
  `kegiatan.getSemster()` (auto-unbox) dan `getTahunAkademik().split("/")`,
  dan **tanpa try/catch** — satu-satunya getter di file ini yang bisa melempar.
  Cabang `else`-nya juga menghasilkan nilai identik dengan cabang pertama.
- `getNilaiAsli()`/`getNilai()`: syarat "belum diisi" memakai
  `intValue() == 0`, jadi nominal 0 < x < 1 selalu dianggap kosong.
- `getKeterangan()`: syarat `getNilai() != null` mati (getNilai tak pernah
  null); teks cabang bulanan menghasilkan spasi ganda `"bulan Januari , nominal"`.
- `setKegiatan()` memanggil `Kegiatan.appendCicilan(this)` yang langsung
  keluar bila `getId() == null` — untuk cicilan baru (belum di-save)
  pendaftaran itu **tidak pernah terjadi**; sinkronisasi nyata mengandalkan
  `populatePembayaran(...)`.
- Format `ref` VA yang diparsing `getNilai()` dikonfirmasi dari sisi produsen
  (`Bankaltimtara`/`BCA`/`BSI`/dll + `PembayaranGatewayKatalog`):
  `ntt-<idKegiatan>-Bulanan-<idPPB>-<nominal>-<idVA>` (tepat 6 potongan).
  Prefiks `"ntt"` sisa historis Bank NTT, kini dipakai belasan gateway lain.
  Kolom `ref` `unique` — itulah mekanisme idempotensi anti-bayar-ganda.
- Komentar generator usang `"Bank generated by hbm2java"` (salah entity,
  copy-paste hbm2java 2010) digantikan dokumentasi sebenarnya.

## `ais/database/model/KurikulumPunyaMatakuliah.java` — SELESAI 100% (2 Sep 2026)

Entity **join-table kurikulum &harr; matakuliah** (tabel
`kurikulum_punya_matakuliah`): satu baris = penempatan satu `Matakuliah` pada
satu `Kurikulum`, lengkap dengan semester, tahap, dan **seluruh berkas RPS/OBE**
mata kuliah itu khusus untuk kurikulum tersebut. 749 → 1670 baris,
**79/79 method ber-Javadoc (100%)** (diaudit skrip, 0 tersisa). Revisi
**r83128**. Mirror `java/` diverifikasi byte-identik dengan `cmp`. Kode
diverifikasi TIDAK berubah sama sekali (bandingkan sumber lama vs baru setelah
seluruh komentar dilucuti → IDENTIK).

**Struktur**: `extends GeneralValueObject` langsung. Bukan join table polos —
membawa puluhan atribut sendiri: penempatan akademik (`semester`, `tahap`,
`inti`, `institusional`, `aktif`, `terdapatTugas`), berkas RPS/OBE (`rincian`
JSON agenda mingguan, `cplBobot`, `komponenPenilaian`, `teknikPerCpmk`,
`rubrikPenilaian`, `pemetaanSoalUts`/`Uas`, `minimalKetercapaian`,
`nilaiMenggunakanCpmk`), dan administratif (`tanggalPenyusunan`,
`pengembangRps`, `koordinator`, `dosen`, `mitraPengembang`, `pustaka`,
`pustakaPendukung`, `mkPrasyarat`, `catatan`, `dikunci`). Konsekuensi desain:
**RPS milik kurikulum, bukan milik mata kuliah** — MK yang sama di kurikulum
2018 dan 2023 punya dua baris dengan dua RPS berbeda.

**Konfirmasi lokasi relasi prasyarat**: `mkPrasyarat` di file INI adalah tempat
relasi prasyarat mata kuliah disimpan (CSV id `Matakuliah`, bukan id baris
kelas ini) — melengkapi temuan sesi 6 bahwa `Matakuliah.java` tidak menyimpan
prasyarat sebagai field. Prasyarat karena itu bisa berbeda antar kurikulum.

**Field audit shadow: ADA** (`oleh`, `olehId`, `tanggal_dirubah` + hook
`@PreUpdate onUpdate()` ke `AuditTimestampInterceptor.ubah`). Ini **entity ke-12
berturut-turut** — pola tetap 100% konsisten. Varian di sini:
`setOleh`/`setOlehId` **mengabaikan diam-diam** null/kosong (kolom tak bisa
dikosongkan lewat setter), dan **tidak ada `@PrePersist`** — pada INSERT
`tanggal_dirubah` hanya berisi nilai inisialisasi field, `oleh`/`olehId` bisa
tetap kosong.

**Getter yang menulis balik ke field (pola shadow-write) — 8 tempat**:
- `getFeeder()` — menimpa kolom dengan `kurikulum.feeder + "-" +
  matakuliah.feeder`; nilai DB selalu diabaikan. Bisa menghasilkan
  `"null-null"` bila kedua induk belum punya id Feeder.
- `getDeskripsiPembelajaran()` dan `getCapaianPembelajaranProdi()` —
  **pewarisan yang dimaterialisasi**: bila kolom kosong, isi disalin dari
  `Matakuliah` ke field baris ini. Sejak itu perubahan di `Matakuliah` tidak
  lagi ikut terbawa. Sekadar membuka layar RPS sudah "membekukan" warisan.
- `getJumlahPertemuanPerkuliahanDefault()` — mengisi sendiri kolomnya dari
  konfigurasi `jumlah_pertemuan_perkuliahan_default`; dan `getKonfigurasi`
  sendiri menulis default ke tabel konfigurasi bila kunci belum ada
  (bahaya auto-seed yang sudah tercatat di memori repo).
- `getPustaka()`, `getPustakaPendukung()`, `getDosen()`, `getMkPrasyarat()` —
  normalisasi CSV berpembungkus koma persis pola
  `Matakuliah.getCapaianPembelajaranLulusan()`: **urutan hilang** (dedup pakai
  `HashSet`), **isi field ≠ nilai kembali** (field tanpa pembungkus, return
  dibungkus koma → `Restrictions.like` tidak andal), daftar kosong = `",,"`.
  Banyak cabang di dalamnya **dead code** (cek `",,"`/`",,,"`/`",,,,"` setelah
  koma ganda sudah dirapatkan; cek `null` setelah field pasti terisi).
- `toString()` — menulis balik `kurikulum` dan `matakuliah`; artinya mencatat
  object ini ke log saja bisa memicu query/buka session.

**Getter/method yang membuka & menutup session Hibernate: ADA** —
`populateRinci(JSONObject)` membuka `openSession()` sendiri dan SELALU
menutupnya di `finally` (`clear` → `disconnect` → `close`, masing-masing
dibungkus try). Komentar alasannya sudah ada di kode dari sesi lain: dipanggil
juga dari background thread dasbor e-learning tanpa transaksi aktif.

**Temuan/kuirk (TIDAK diperbaiki, hanya dicatat)**:
1. `populateRinci` mengunci map hasil dengan `mulaiMingguKe`, sehingga **dua
   baris RPS yang mulai pada minggu sama saling menimpa** — hanya yang terakhir
   diproses bertahan, padahal pengurutan tahap 1 sudah membedakan keduanya.
2. `populateRinci` memanggil `getMatakuliah().getCapaianPembelajaranLulusan()`
   **di luar semua `try`** dan tanpa cek null, padahal kolom `matakuliah`
   dipetakan `nullable = true` → potensi `NullPointerException` nyata.
3. Kegagalan query CPL di `populateRinci` ditelan: daftar CPL kosong membuat
   semua baris ber-sub-CPMK gagal dicocokkan dan **hilang dari tampilan** tanpa
   pesan kesalahan — RPS tampak kosong.
4. `ambilRinci` menjalankan ulang seluruh `populateRinci` (termasuk buka session
   + query) tiap pemanggilan; `DashboardTimelinePertemuan` memanggilnya per
   minggu → satu query per minggu.
5. `getMinimalKetercapaian()` sudah menjamin non-null (bawaan `50`), tapi
   pemanggil masih memasang bawaan sendiri yang **berbeda-beda**: `60.0`, `75.0`,
   `0.0` di `NilaiObeAction`/`RekapHasilTugasPerTugasDanUjianObe`. Kode bawaan
   itu praktis mati tapi menyesatkan.
6. Konstanta `ARRAY` berisi JSON **object** (`"{}"`), bukan array — penamaan
   menyesatkan.
7. `serialVersionUID` file ini **identik** dengan milik `Kurikulum.java`
   (`2461822577548439808L`) — jejak salin-tempel, tidak berdampak fungsional.
8. Bawaan `getInti()` = `false` sementara `getInstitusional()` = `true`: baris
   baru yang belum diisi terbaca sebagai institusional dan bukan inti.
9. Kolom OBE `cplBobot`/`komponenPenilaian`/`teknikPerCpmk`/`rubrikPenilaian`/
   `pemetaanSoal*` adalah **teks berformat konvensi tanpa validasi apa pun** di
   lapisan entity; `komponenPenilaian` bersifat dokumentatif saja dan bisa tidak
   sinkron dengan bobot sebenarnya di `FormatNilai`/`PembombotanNilai`.
10. `indukMatakuliah` adalah FK ke tabel ini sendiri tanpa penjagaan siklus.

## Batch "5 entity penilaian/skripsi" — SELESAI 100% (2 Sep 2026, dikonsolidasi orkestrator)

Semua 5 file TUNTAS 100% method, dikompilasi, dikommit bertahap, di-mirror ke
`java/` (verifikasi `cmp` byte-identik):
- `FormatNilaiSkripsi.java` — 245/245 method. 1249→3519 baris. r83105/83110/83113.
- `FormatNilaiProposalSkripsi.java` — 243/243 method. 1247→3765 baris.
  r83110/83114/83119/83120.
- `PembombotanNilai.java` — 101/101 method. 1295→2642 baris. r83104/83106.
- `BiodataDosen.java` — 125/125 method. 729→1828 baris. r83101/83103.
- `MahasiswaRequestTugasAkhir.java` — 145/145 method. 1472→3150 baris.
  r83110/83112.

**AUDIT TERPISAH DIBUAT** (`task_15f5001e`, di luar inisiatif dokumentasi ini)
untuk kluster temuan arsitektur yang sudah cukup banyak & serius setelah 11+
entity digarap: getter Hibernate "cerdas" yang diam-diam menulis/MENGHAPUS
data saat dibaca (beberapa kasus kehilangan data nyata di `PembombotanNilai`,
`FormatNilaiSkripsi`/`FormatNilaiProposalSkripsi`, `MahasiswaRequestTugasAkhir`,
`BiodataDosen`), bug penamaan kolom dosen 1/2 di `FormatNilaiSkripsi.java`
(terkonfirmasi sisi MASTER dari bug yang sama di `Skripsi.java` sesi
sebelumnya) plus SATU inkonsistensi arah pemetaan yang mungkin bug fungsional
nyata (`VOPembelajaran` vs method lain di file yang sama), dan bug id
anak-vs-induk yang muncul independen di 2 tempat (`Skripsi`+
`MahasiswaRequestTugasAkhir`, pola kemungkinan copy-paste). **PENTING**:
bug label/kolom tertukar itu TIDAK ada di `FormatNilaiProposalSkripsi.java`
(penamaannya konsisten bersih) — jangan generalisasi berlebihan, itu spesifik
`FormatNilaiSkripsi.java` saja.

**Total akumulasi 6 sesi kerja**: 203 (sesi 1-5) + 5 = **208 file** dari 7.401
(~2,8%). Field audit shadow sekarang terkonfirmasi 100% konsisten di
**11 entity berturut-turut** — anggap SELALU ADA di entity manapun yang
belum digarap.

## `ais/database/model/FormatNilaiProposalSkripsi.java` — SELESAI 100% (2 Sep 2026)

Entity master **format penilaian seminar/pengajuan proposal skripsi** (tabel
`format_nilai_proposal_skripsi`). 1247 → 3765 baris, **243/243 method
ber-Javadoc (100%)** (diaudit skrip, 0 tersisa). Revisi: r83110 (bagian awal
tersapu ke revisi gabungan sesi paralel, isi diverifikasi lewat
`svn diff -c 83110`), r83114, r83119. Mirror `java/` diverifikasi byte-identik
dengan `cmp`. Kode diverifikasi TIDAK berubah sama sekali dengan membandingkan
sumber lama vs baru setelah seluruh komentar dilucuti.

**Struktur**: `extends GeneralValueObject` langsung (bukan lewat
`VOPembelajaran` seperti `Skripsi`). Ini **template/konfigurasi**, bukan data
transaksi — transaksinya `MahasiswaRequestTugasAkhir`. Satu baris = satu
"Jenis Pengajuan" (mis. Pengajuan Judul, Seminar Proposal). Anaknya:
`MahasiswaRequestTugasAkhir`, `ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi`
(komponen penilaian), `TemplateFormatBimbingan`.

**Perbandingan dengan `FormatNilaiSkripsi.java`** (dikerjakan sesi paralel —
struktur MIRIP tapi TIDAK identik, sudah dibaca langsung untuk konfirmasi):
- Proposal punya **6 slot dosen** (`dosen1`..`dosen6`, label default
  Pembimbing I/II/III + Penguji I/II/III); sidang akhir punya **8**
  (`dosen1`, `dosen2`, `dosen21`, `dosen3`..`dosen7`).
- **Pemetaan slot→bobot di entity proposal KONSISTEN** (dosen1..3 →
  `prosentasiNilaiPembimbing1..3`, dosen4..6 → `prosentasiNilaiPenguji1..3`).
  **Cacat penamaan kolom slot 1/2 yang terkenal itu HANYA ada di
  `FormatNilaiSkripsi`** (yang memakai `prosentasi_nilai_ketua_sidang` untuk
  slot pertama); entity proposal tidak punya kolom itu sama sekali.
- `getDosen6Aktif()` membaca `getProsentasiNilaiPenguji3()` di sini, tapi
  `getProsentasiNilaiPenguji4()` di `FormatNilaiSkripsi` — jangan menyalin
  logika antar keduanya tanpa menyesuaikan pemetaan.
- Hanya entity proposal yang punya `alurSebelumnya` + `ambilSebelumnya()`
  (rantai prasyarat antar tahapan) dan `formatNilaiSkripsi` (jembatan ke alur
  sidang akhir).
- Sisanya (lampiran 1..20, tipeItem 1..20, prasyarat SKS/IPK/angka kredit,
  biaya, flag perilaku) berbagi bentuk yang sama — hasil copy-paste.

**Verifikasi pola berulang lintas entity**:
- Field audit shadow (`id`/`oleh`/`olehId`/`tanggal_dirubah` dideklarasikan
  ulang padahal `GeneralValueObject` sudah punya) — **ADA, dikonfirmasi**.
  Entity ke-11 berturut-turut; pola ini sekarang bisa dianggap universal.
- Getter berefek samping ke DB — **ADA**, dalam tiga varian sekaligus:
  (a) `check(...)` + penugasan ulang field pada semua getter relasi;
  (b) `getKodeMatakuliah()`/`getKodeMatakuliahDan()` menormalkan lalu menulis
  balik; (c) `getJenisKegiatanMahasiswa()` melakukan *backfill* relasi dari
  kolom teks warisan `jenis`. Karena entity memakai *property access*
  (`@Id` di getter) + `dynamicUpdate`, ketiganya terlihat oleh dirty checking.
- Getter yang menutup sesi Hibernate — **TIDAK ADA** di file ini.
  `ambilSebelumnya()` memang query DB dari dalam model, tapi memakai
  `HibernateUtil.currentSession()` dan tidak menutupnya.

**Temuan/kuirk (dicatat, TIDAK diperbaiki)**:
1. **`getKodeMatakuliah()`/`getKodeMatakuliahDan()` bisa MENGHAPUS DATA.**
   Bila `getTidakWajibMengambilMkTertentu()` bernilai `true`, kedua getter
   menugaskan string kosong ke field-nya; lewat property access pengosongan
   itu dapat ikut ter-`UPDATE` ke DB. Mencentang opsi "tidak wajib mengambil
   MK tertentu" lalu membaca format itu berpotensi menghapus permanen daftar
   prasyarat (hanya bisa dilihat lagi lewat riwayat Envers). Mengembalikan
   flag ke `false` tidak memulihkan daftarnya.
2. **`getProsentasiNilaiPenguji1..4()` tidak memberi default** — satu-satunya
   kelompok getter numerik di file ini yang mengembalikan `null` apa adanya.
   Inisialisasi field `= 0.0` tertimpa saat Hibernate memuat baris berkolom
   NULL, sehingga rumus pembobotan (`nilai * persen / 100.0`) dan
   `getDosenNAktif()` (`persen > 0.1`) bisa melempar NPE unboxing.
   `FormatNilaiSkripsi` memakai pola `== null ? 0.0 : ...` yang aman.
3. **Kolom `prosentasi_nilai_penguji_4` MATI** di alur proposal: tidak ada slot
   dosen ke-7, dan satu-satunya baris UI penulisnya di
   `FormatNilaiProposalSkripsiAction` (~baris 1571) sudah dikomentari.
4. **`toString()` cacat salin-tempel**: `prosentasiNilaiPembimbing3` tidak
   pernah dicetak, `prosentasiNilaiPenguji1` dicetak dua kali, dan satu
   pemisah `"-"` hilang sehingga dua angka menempel. Hanya dipakai untuk log.
5. **Pencocokan slot berbasis STRING label**, bukan indeks
   (`jenis.equals(fmt.getDosen1())` di `dataDosen()`, `cariNilaiDariDosen()`,
   `PenilaianProposalSkripsiHelper`). Mengganti label `dosenN` pada format yang
   sudah dipakai transaksi memutus pencocokan ke nilai tersimpan; dua slot
   berlabel sama membuat pencocokan selalu jatuh ke nomor terkecil.
6. **`kode1..6` = kode kategori kegiatan Feeder per slot.** Bila kosong,
   `EksporPesertaDosenBimbinganFeeder` **menebak dari label**: mengandung kata
   "penguji" → `110500`, selain itu → `110400`. Format yang memakai istilah
   lokal ("Reviewer", "Pembahas") akan salah kategori di ekspor PDDikti, dan
   mengganti label slot dapat mengubah hasil ekspor secara diam-diam.
7. **`tahunAngkatan` dicocokkan dengan `contains()`**, bukan pemisahan per
   koma — rawan cocok sebagian (isian `"2021"` vs angkatan `"202"`).
8. **Default getter tidak seragam arahnya**: `aktif`, `adaProposal`,
   `hanyaBisaDilakukanSekali`, `terdapatSidangSetelahSelesai`,
   `mahasiswaBolehMengubahAgendaAtauJadwalBimbingan` default `true`;
   sisanya `false`. `getBobot()` default `100.0` (format lama mendominasi
   perhitungan `GradingHelper`, bukan diabaikan). Membaca kolom lewat SQL
   langsung memberi jawaban berbeda dari membaca lewat entity.
9. Kode mati di `getKodeMatakuliah()`/`getKodeMatakuliahDan()`: cabang
   `equals(",,")`/`equals(",,,")` dan pemeriksaan `== null` tidak akan pernah
   tercapai setelah tiga kali `replaceAll(",,", ",")`.
10. `alurSebelumnya` dan `tipeItem1..20` disimpan sebagai `Long` mentah tanpa
    relasi/foreign key — id bisa menggantung; rantai `alurSebelumnya` tidak
    punya proteksi melingkar.
11. Javadoc class lama tertulis *"FormatNilaiSkripsi generated by hbm2java"* —
    salah salin nama class, bukti file ini disalin dari `FormatNilaiSkripsi`.
    Sudah dimutakhirkan (fakta salin-tempelnya tetap dicatat).

## `ais/database/model/FormatNilaiSkripsi.java` — SELESAI 100% (2 Sep 2026)

Master "format penilaian sidang tugas akhir/skripsi". 1249 → 3519 baris,
**245/245 method ber-Javadoc (100%)** (diaudit skrip, 0 tersisa). Revisi:
r83105, r83110 (bagian 2 sebagian tersapu ke r83108, revisi gabungan sesi
paralel; isi diverifikasi dengan `svn diff -r HEAD` bersih). Mirror `java/`
diverifikasi byte-identik dengan `cmp`.

**Struktur**: `extends GeneralValueObject`. Kepadatan method sangat tinggi
relatif ukurannya karena hampir seluruhnya getter/setter dari beberapa keluarga
berulang: 8 slot dosen × 4 atribut (label `dosenN`, kode `kodeN`, bendera
`dosenNAktif`, bobot `prosentasiNilai*`), 20 slot lampiran × 3 atribut (judul
`uploadLampiranN`, bendera `uploadLampiranNWajib`, penautan pustaka
`tipeItemN`), plus penargetan format, syarat pendaftaran, dan pelaporan.
Keluarga berulang didokumentasikan dengan pola "method pertama detail + sisanya
ringkas ber-`@see`".

**Perannya**: satu baris = satu "jenis pengajuan sidang" yang menentukan (a)
siapa saja penilainya dan berapa bobotnya, (b) gerbang syarat pendaftaran
(SKS/IPK/angka kredit/matkul prasyarat lulus/pelunasan biaya/bebas pustaka),
(c) 20 slot lampiran wajib, (d) skala nilai huruf, (e) bobot format terhadap
nilai mata kuliah tugas akhir di KHS, (f) kode aktivitas untuk ekspor Feeder.
Data nilainya sendiri ada di `Skripsi`.

**Keterkaitan dengan bug slot dosen 1/2 di `Skripsi.java` — TERKONFIRMASI**:
file ini adalah *sisi master* dari bug tersebut dan mengidap pergeseran nama
yang sama. Slot `dosen1` berlabel default "Pembimbing I" tetapi bobotnya di
kolom `prosentasi_nilai_ketua_sidang`; slot `dosen2` ("Pembimbing II") bobotnya
di `prosentasi_nilai_pembimbing`. Ini sejalan sempurna dengan
`skripsi.nilai_ketua_sidang` (nilai slot 1) dan `skripsi.nilai_pembimbing`
(nilai slot 2) — jadi keduanya salah dengan cara yang sama, bukan dua kesalahan
yang saling meniadakan. Bukti terkuatnya `getDosen1Aktif()` yang menyimpulkan
keaktifan slot 1 dari `getProsentasiNilaiKetuaSidang()`. Konsisten di seluruh
aplikasi → tampilan benar, hanya namanya yang salah. Entity sejenis
`FormatNilaiProposalSkripsi` justru memakai penamaan bersih
(`prosentasiNilaiPembimbing1/2/3`), jadi asumsi penamaan TIDAK boleh disalin
antar entity.

**Temuan lain (dicatat, tidak diperbaiki)**:
- Peran dosen dicocokkan berbasis **teks label** di seluruh aplikasi → dua slot
  berlabel sama akan bertabrakan (cabang `if` pertama menang).
- `PenilaianSkripsiHelper.populateKomponen(String)` tidak punya cabang untuk
  slot `dosen21` (Pembimbing III) → jatuh ke default `"dosen1"`.
- `VOPembelajaran` memetakan slot 1/2 **terbalik** (`getKetuaSidang()` →
  `getDosen1()`), berlawanan dengan `Skripsi.dataDosen(boolean)` dan dengan
  method lain di file yang sama.
- `getTidakWajibMengambilMkTertentu()` bersifat destruktif: getter
  `getKodeMatakuliah()`/`getKodeMatakuliahDan()` **mengosongkan field** saat
  opsi itu true → daftar matkul hilang dari DB saat flush.
- `toString()` menyebut `prosentasiNilaiPenguji1` dua kali, kehilangan satu
  pemisah `-`, dan tidak pernah menyebut tiga bobot lainnya.
- Penanda `auto-audit` pada dua blok catch menyebut nomor baris versi lama.

**Verifikasi pola berulang**: field audit shadow (`id`, `oleh`, `olehId`,
`tanggal_dirubah`) — **ADA**, konsisten dengan entity-entity sebelumnya. Getter
menulis balik ke field/DB — **ADA**, varian lokalnya
`getKodeMatakuliah()`/`getKodeMatakuliahDan()` (normalisasi koma + pengosongan
paksa) dan `getJenisKegiatanMahasiswa()` (migrasi on-the-fly dari kolom teks
warisan `jenis`). Getter yang membuka session Hibernate — **ADA** lewat
`check(...)` pada 5 getter relasi.

## `ais/database/model/MahasiswaRequestTugasAkhir.java` — SELESAI 100% (2 Sep 2026)

Entity **pengajuan judul & pembimbing tugas akhir** — tahap SEBELUM `Skripsi`
resmi terbentuk. 1472 → 3150 baris, **145/145 method ber-Javadoc (100%)**
(diaudit skrip, 0 tersisa). Revisi: **r83110** (tersapu ke revisi gabungan sesi
paralel berpesan kosong; isi diverifikasi lewat penanda teks di HEAD). Mirror
`java/` diverifikasi byte-identik dengan `cmp`.

**Struktur**: `extends VOPembelajaran extends VoKunci extends sop.DataSop extends
GeneralValueObject`, sekaligus `implements VOPesertaPembelajaran` (mengembalikan
dirinya sendiri sebagai objek pembelajaran — pola sama dengan `Skripsi`). Alur:
`Pengajuan` → `Disetujui` → `Seminar/Proses Bimbingan` → `Sidang`, dengan cabang
`Mengulang`/`Ditolak`. `getStatus()` adalah mesin status yang menaikkan DAN
menurunkan status sendiri berdasarkan `JadwalSeminarTugasAkhir`, tanggal
bimbingan, dan `Skripsi.getTelahSidang()`.

**Mekanisme `skr`/`setSkr` (temuan utama)**: arah pengajuan → `Skripsi` BUKAN
foreign key. Id skripsi disimpan sebagai TEKS pada properti `skr` yang dipetakan
ke kolom bernama **`filelocation`** (nama peninggalan; isinya id skripsi).
Pengisinya adalah getter di sisi seberang — `Skripsi.getMahasiswaRequestTugasAkhir()`
memanggil `setSkr(getId())` tiap kali dipanggil, jadi membuka layar skripsi
MENULIS ke entity pengajuan. `setSkr` menulis dua tempat: berkas cache lewat
`put(nilai,"skr")` **hanya bila nilainya angka**, dan field/kolom **selalu**;
`getSkr()` membaca berkas lebih dulu dan isinya MENIMPA field. Asimetri ini
berarti tautan skripsi **tidak bisa dilepas** lewat setter selama berkas
cache-nya masih ada. `ambilSkripsi()` memuat entity lewat `DataUtil.ambilData()`
(cache → DB), bukan join — berpotensi N+1 karena `getStatus()` memanggilnya.

**Temuan lain (dicatat, TIDAK diperbaiki)**:
- `getDosen1()`..`getDosen6()` menyetel field dosen jadi `null` bila slotnya
  dinonaktifkan di `FormatNilaiProposalSkripsi`; `null` itu ikut tersimpan ke
  kolom `dosenN` saat flush → mengubah master format nilai dapat **menghapus**
  penetapan dosen pengajuan lama hanya dengan membacanya.
- `retreiveDetailVerifikasiNilai` membandingkan id
  `ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi` dengan kolom pertama
  `detailNilai` yang selalu berisi id `KomponenPenilaianProposalSkripsi` — **bug
  id anak vs id induk**, identik dengan yang sudah dicatat di `Skripsi.java` dan
  sepola dengan `Pertemuan.java`. Dampak terbatas: satu-satunya pemanggil sudah
  tidak dipakai.
- `getTotalNilai()`: penjaga masuk hanya memeriksa slot 1..5, **slot 6 terlewat**
  → nilai yang hanya berasal dari slot 6 tak pernah memicu hitung ulang. Nama
  variabel lokal bergeser satu langkah dari makna slot, TAPI pemetaan slot ke
  kolom prosentase **konsisten** dengan master — tidak ada pertukaran slot
  seperti pada `Skripsi.java`.
- `reloadProposalSkripsiPunya...` menimpa `detailNilai` seluruhnya → entri dosen
  lain hilang. `bersihkanNilaiKeDefault(List)` tak membungkus parsernya dengan
  try/catch.
- **Field audit di-shadow**: `id`, `oleh`, `olehId`, `tanggal_dirubah`, `nama`,
  `keterangan` — pola 100% konsisten, kini 10 entity berturut-turut. Bonus:
  `disposisiSop` + getter/setter-nya **duplikat verbatim** dari `VOPembelajaran`
  (termasuk cabang ternary yang mati).
- Getter menulis balik ke field: `semester`, `tahunAkademik`, `status`, `judul`,
  `tanggalAwal/AkhirBimbingan`, `waktuSeminar`, `waktuSampaiSeminar`,
  `catatanSeminar` (migrasi teks default lama → baru), `totalNilai`,
  `nilaiHuruf`, `lulus`, `skr`.
- Kuintet "peta lokasi berkas JSON" (`ambilLokasi/tulisLokasi/...`) **TIDAK ada**
  di file ini; yang ada hanya pemakaian `put`/`retreive` untuk `skr`.

## `ais/database/model/PembombotanNilai.java` — SELESAI 100% (2 Sep 2026)

Master **format/skema pembobotan nilai** perkuliahan. 1295 → 2642 baris,
**101/101 method ber-Javadoc (100%)** (diaudit skrip). Revisi: **r83104**.
Mirror `java/` diverifikasi byte-identik (`cmp`). `javac -implicit:none` lulus;
`javadoc -Xdoclint:reference` bersih untuk berkas ini.

**Koreksi asumsi awal**: entity ini BUKAN milik satu `Perkuliahan`. Ia baris
MASTER yang dipakai ulang banyak kelas lewat `Perkuliahan.pembombotanNilai`,
di-cache global (`ConstantValues.ambilBerdasarClass`), punya pemilik
(`dimilikiOleh` → `Dosen`), penanda `aktif`/`defaultPembobotan`, dan mode
"wajib di tahun akademik + semester tertentu" yang mengalahkan pilihan
per-kelas. `ConstantValues.DEFAULT_PEMBOBOTAN_NILAI` (Tugas 20/UTS 30/UAS 50)
dibuat `InitDataHelper` bila belum ada.

**Typo historis dikonfirmasi & dicatat di Javadoc kelas** (TIDAK diganti):
"PembombotanNilai" sudah merambat ke nama tabel `pembombotan_nilai`, kolom FK
di `Perkuliahan`, properti Hibernate, dan tabel audit Envers. Asimetri: layar
pengelolanya `ais.action.master.PembobotanNilaiAction` dieja BENAR. Typo lain
di berkas yang sama: method `getTahunAkadmeik()` dan kunci konfigurasi
`default_prentasi_uts`/`default_prentasi_uas`.

**Temuan (didokumentasikan, tidak diperbaiki)**:
- `getNama()` getter-yang-menulis: membangun ulang nama dari komponen, membuang
  hasil `setNama()`, dan karena property-access ikut tersimpan ke kolom `nama`.
- `getUts()`/`getUas()` memanggil `Common.getKonfigurasi(...)` yang AUTO-SEED
  baris konfigurasi ke DB → membaca bobot bisa menerbitkan INSERT.
- `getTahunAkadmeik()`/`getSemester()` MENGOSONGKAN field-nya sendiri saat
  bendera "wajib" padam → periode tersimpan hilang hanya karena dibaca.
- `compareTo()` induk memanggil `getNama()` → MENGURUTKAN koleksi entity ini
  memicu semua efek samping di atas per elemen.
- Nama `setDefaultPembobotan` dipakai untuk dua hal berbeda jauh: setter flag
  vs method statis pencetak baris `FormatNilai`.
- `dosen1..dosen5` ikut divalidasi ke total 100% oleh UI tapi TIDAK PERNAH
  diterbitkan sebagai `FormatNilai` dan tidak muncul di `getNama()`.
- Ambang komponen tidak konsisten: `>= 0,01` (getNama) vs `> 0,1`
  (setDefaultPembobotan) → bobot 0,01–0,1 tampil di nama tapi tak jadi komponen.
- Validasi "total 100%" hanya di UI (`PembobotanNilaiAction`), bukan di entity.
- `getKeterangan()` mempersempit kontrak induk (induk jamin non-null, override
  bisa null) → memengaruhi cabang terakhir `compareTo`.
- `tampilkanFormat()` bernama "tampilkan" tapi BISA MENGUBAH DATA (pemulihan
  otomatis format OBE lama) dan mengelola sesi Hibernate sendiri.
- `keterhubungan` = `public static final Map` yang isinya mutable, tanpa
  `unmodifiableMap`; pemanggil `get()` tanpa cek null.

**Verifikasi pola berulang**: field audit shadow **ADA** (`oleh`, `olehId`,
`tanggal_dirubah`) — dan lebih jauh dari entity sebelumnya: `id`, `nama`,
`keterangan` pun ikut di-shadow. Getter-menulis-balik-ke-DB **ADA** (varian
`Common.getKonfigurasi` auto-seed + `getNama()` property-access). Getter yang
menutup sesi Hibernate **ADA** tapi hanya pada method statis
`tampilkanFormat()`, bukan pada getter properti.

**Kerapian berkas**: EOL berkas ini sebelumnya CAMPURAN (1204 CRLF dari 1295
baris); dinormalkan ke CRLF murni dalam commit yang sama.

## `ais/database/model/BiodataDosen.java` — SELESAI 100% (2 Sep 2026)

Entity biodata pribadi dosen (tabel `public.biodata_dosen`), pasangan
`BiodataPegawai`/`BiodataMahasiswa` yang sudah selesai sesi lalu. 729 → 1828 baris,
**125/125 method ber-Javadoc (100%)** (diaudit skrip, 0 tersisa). Revisi: **r83101**
(pesan tersapu ke revisi gabungan sesi paralel — isi diverifikasi lewat
`svn diff -c 83101`, 1102 baris ditambahkan). Mirror `java/` diverifikasi
byte-identik dengan `cmp`. Kompilasi `-implicit:none` lulus.

**Struktur**: `BiodataDosen extends GeneralValueObject`. Seluruh isinya pasangan
getter/setter properti Hibernate — tidak ada method bisnis, query statis, maupun
helper UI. Satu relasi wajib (`dosen`, `nullable=false`, TIDAK unique) dan delapan
relasi `@ManyToOne` opsional (Agama, Negara, Wilayah kecamatan, Kota, Propinsi, dan
tiga `PekerjaanOrangTua` untuk ayah/ibu/pasangan).

**Jalur pembuatan instance**: `Dosen.ambilBiodata()` — cache → Criteria kolom
`dosen` (ID terbesar) → **membuat + commit baris `biodata_dosen` baru** bila belum
ada, lalu `HibernateUtil.closeSession()`. Dipanggil luas, termasuk dari SELURUH
getter `BiodataPegawai` lewat `Pegawai.getDosen().ambilBiodata()`, jadi membuka
layar biodata pegawai bisa menambah baris `biodata_dosen`.

**Verifikasi pola berulang (jawaban eksplisit)**:
- *Getter yang menulis baris master baru ke DB* (`findOrCreatePropinsi`/
  `findOrCreateKota` + Levenshtein seperti di `BiodataMahasiswa`): **TIDAK ADA**.
  `getKecamatan()`/`getKota()` murni `check()`; `getPropinsi()` hanya menurunkan
  propinsi dari kota **di memori**. Ini perbedaan nyata dari `BiodataMahasiswa`.
- *Getter yang menutup session Hibernate pemanggil*: **TIDAK ADA**. Tidak satu pun
  method di file ini membuka `Session`, memulai transaksi, atau memanggil
  `HibernateUtil.closeSession()`. Efek itu seluruhnya datang dari
  `Dosen.ambilBiodata()` di luar file ini.
- *Field audit yang di-shadow ulang*: **ADA** — `id`, `oleh`, `olehId`,
  `tanggal_dirubah` dideklarasikan ulang menutupi `GeneralValueObject`. Pola ini
  kini 100% konsisten di seluruh entity yang sudah digarap.
- *Getter berefek samping ke DB secara umum*: **ADA**, dalam bentuk lain — 11 getter
  menulis balik ke field-nya sendiri saat dibaca (5 menyalin dari `Dosen`:
  alamat/noKtp/noIdentitas/teleponRumah/hp; 6 menulis nilai default:
  tinggiBadan/beratBadan/statusNikah=0, kewarganegaraan=WNI, kelurahan="-",
  kewarganegaraanFeeder="ID"). Karena `dynamicUpdate=true`, membaca = menulis saat
  flush. Menguatkan kesimpulan lintas batch: polanya universal, bentuknya beda-beda.

**Temuan/kuirk (didokumentasikan, TIDAK diperbaiki)**:
- `getAlamat()` dan `getNoKtp()` menimpa dari `Dosen` **tanpa penjagaan null** —
  membaca getter dapat MENGOSONGKAN data yang sudah tersimpan. `getNoIdentitas()`,
  `getTeleponRumah()`, `getHp()` yang membaca sumber sama justru dijaga.
- **Dua validasi wajib yang tidak pernah bisa gagal**: `checkBiodataDosen()`
  mewajibkan 10 properti terisi via `Common.checkIsNull`, yang membaca lewat
  `ClassMetadata.getPropertyValue` — yaitu lewat getter. Karena `getKelurahan()`
  selalu minimal `"-"` dan `getStatusNikah()` selalu minimal `0`, pemeriksaan
  "kelurahan" dan "statusNikah" mati. Efek lanjutan: ekspor Feeder `ds_kel` bisa
  berisi `"-"`, bukan nama kelurahan sebenarnya.
- `@Column(name="alamat_asal_s2")` terpasang di **setter**, bukan getter. Pemetaan
  property-access mengabaikannya → kolom jatuh ke default `alamatAsalS2`
  (camelCase), padahal saudaranya `alamat_asal_s1`/`alamat_asal_s3`. **Bug kembar
  persis dengan `BiodataPegawai`.**
- **10 properti tanpa `@Column`** (rt, rw, kodepos, kelurahan, noIdentitas, dusun,
  namaSuamiIstri, nipSuamiIstri, kewarganegaraanFeeder, alamatAsalS2). Karena
  `MyNamingStrategy extends DefaultNamingStrategy` (nama kolom = nama properti apa
  adanya, TIDAK konversi snake_case), gaya kolom tabel ini bercampur: `no_ktp`
  bersebelahan dengan `noIdentitas`. Wajib diperhatikan pada SQL ad-hoc/laporan.
- `toString()` membaca field `dosen` langsung (bukan `getDosen()`) → tidak lewat
  `check()` → bisa melempar `LazyInitializationException`, termasuk dari logging
  atau debugger.
- `noKtp` dan `noIdentitas` duplikat isi (dua kolom, satu sumber `Dosen.getKtp()`);
  yang dipakai formulir & validasi adalah `noIdentitas`.
- `getNegara()` satu-satunya getter berdefault yang TIDAK menulis field (fallback
  `ConstantValues.INDONESIA` hanya dikembalikan) — dan bisa tetap `null` sebelum
  inisialisasi data aplikasi berjalan.
- `getAsalSma/Smp/Sd()` membuang tanda kutip tunggal dan ganda pada nilai yang
  dikembalikan tanpa menulis balik → getter asimetris terhadap setter; isyarat masih
  ada perangkaian SQL/CSV berbasis penyambungan string di lapisan laporan.
- Salah eja properti/kolom `keahliah1..keahliah5` (properti slot 1 `keahliah1`,
  slot 2-5 `keahlian2..5`; kolom konsisten salah eja semua).
- `serialVersionUID` identik dengan `BiodataPegawai` (salin-tempel).
- `setOleh()`/`setOlehId()` tidak bisa mengosongkan nilai (langsung `return` bila
  null/kosong).

## Batch "5 entity moderat" — SELESAI 100% (2 Sep 2026, dikonsolidasi orkestrator)

Lanjutan langsung batch 4-entity-besar sebelumnya. Semua 5 file TUNTAS 100%
method, dikompilasi, dikommit bertahap, di-mirror ke `java/` (verifikasi `cmp`
byte-identik setelah `svn update` menyeluruh):
- `Matakuliah.java` — 120/120 method. 976→2284 baris. r83072/83085/83087.
- `KrsMahasiswa.java` — 87/87 method. 748→1695 baris. r83076/83080/83082.
  **Koreksi asumsi**: bukan "pendaftaran ke 1 Perkuliahan" tapi rekap KRS per
  semester + catatan bimbingan dosen PA (approval sebenarnya di
  `Detailperkuliahan`, entity terpisah belum digarap).
- `Jurusan.java` — 97/97 method. 619→1668 baris. r83076/83077. Dirujuk 130
  entity lain — paling sentral dari batch ini.
- `BiodataPegawai.java` — 94/94 method. 707→1633 baris. r83069/83073.
- `Skripsi.java` — 187/187 method. 1803→3703 baris. r83076/83080/83088/83089.

**Bug penamaan kolom SERIUS ditemukan** (`Skripsi.java`, TIDAK diperbaiki,
hanya didokumentasikan detail di Javadoc method terkait): slot dosen 1/2
tertukar antara kolom "orang" (`pembimbing`) dan kolom "nilai"
(`nilai_ketua_sidang`) — konsisten dipakai di SELURUH aplikasi jadi tampilan
UI tetap benar, TAPI query SQL langsung/laporan ad-hoc atas tabel `skripsi`
akan salah baca kalau mengasumsikan nama kolom sesuai isinya.

**Verifikasi pola berulang lintas batch ini** (lihat memory untuk detail):
field audit shadow — ADA di SEMUA 5 file (100% konsisten sejauh ini di semua
entity yang sudah digarap). Pola getter-menulis-master-DB — variannya
BERBEDA-BEDA per file (BiodataPegawai lewat `Dosen.ambilBiodata()`,
KrsMahasiswa lewat `singkronkanKrsMahasiswa`, Skripsi lewat
`MahasiswaRequestTugasAkhir.setSkr()`, Jurusan/Matakuliah/KrsMahasiswa TIDAK
punya varian `findOrCreateX`+Levenshtein seperti entity biodata mahasiswa) —
KESIMPULAN: pola "getter berefek samping ke DB" itu sendiri UNIVERSAL di
codebase ini (ditemukan di semua entity sejauh ini dalam SATU bentuk atau
lain), tapi bentuk konkretnya harus diverifikasi per file, jangan asumsikan
sama persis.

**Total akumulasi 5 sesi kerja**: 198 (sesi 1-4) + 5 = **203 file** dari 7.401
(~2,7%), mencakup hampir semua entity model paling sentral dalam sistem
akademik (Mahasiswa, Dosen, Pegawai, Perkuliahan, Pertemuan, KrsMahasiswa,
Matakuliah, Jurusan, Skripsi, + biodata masing-masing).

## `ais/database/model/Skripsi.java` — SELESAI 100% (2 Sep 2026)

Entity tugas akhir/skripsi satu mahasiswa. 1803 → 3703 baris, **187/187 method
ber-Javadoc (100%)** (diaudit skrip, 0 tersisa). Revisi: r83076, r83080 (bagian 2
tersapu ke revisi gabungan sesi paralel setelah WC sempat ter-lock; isi
diverifikasi lewat penanda teks di HEAD), r83088. Mirror `java/` diverifikasi
byte-identik dengan `cmp`.

**Struktur**: `Skripsi extends VOPembelajaran extends VoKunci extends
sop.DataSop extends GeneralValueObject`, sekaligus `implements
VOPesertaPembelajaran` (mengembalikan dirinya sendiri sebagai objek
pembelajaran). Alur hidup: pengajuan judul (`MahasiswaRequestTugasAkhir`) →
penetapan pembimbing → bimbingan (baris `pertemuan` berkolom `skripsi`) →
seminar proposal → gelombang pendaftaran + `JadwalSidangTugasAkhir` → sidang &
penilaian per komponen → nilai huruf/kelulusan → konversi ke `Detailperkuliahan`
(KHS) dan ekspor Feeder.

**Temuan struktural terpenting — penamaan kolom slot 1/2 tertukar**:
`FormatNilaiSkripsi` menyediakan 8 slot dosen (`dosen1`, `dosen2`, `dosen21`,
`dosen3`..`dosen7`). Untuk dua slot pertama, kolom ORANG dan kolom NILAI
tertukar namanya: orang slot 1 di kolom `pembimbing` tetapi nilainya di kolom
`nilai_ketua_sidang`; orang slot 2 di kolom `ketua_sidang` dengan nilai di
`nilai_pembimbing`. Silang ini KONSISTEN di seluruh aplikasi (`dataDosen`/
`simpanDosen` memakai kolom orang, `cariNilaiDariDosen`/`getTotalNilai` memakai
kolom nilai, dan `dosen1Aktif` pun default dari `prosentasiNilaiKetuaSidang`),
jadi tampilan aplikasi benar — yang salah hanya NAMANYA. Query SQL langsung dan
laporan ad-hoc atas tabel `skripsi` akan salah baca. Tabel pemetaan lengkap ada
di Javadoc class.

**Pola berulang yang DIKONFIRMASI ada di sini** (dicek dari kode, bukan
diasumsikan):
- *Field audit di-shadow ulang*: ADA — `oleh`/`olehId`/`tanggal_dirubah`
  dideklarasikan lagi padahal `GeneralValueObject` sudah punya.
- *Getter yang menulis ke DB*: ADA dalam bentuk lintas-entity —
  `getMahasiswaRequestTugasAkhir()` memanggil `setSkr(id)` pada
  `MahasiswaRequestTugasAkhir` (menyunting kolom JSON-nya), sehingga membaca
  relasi memicu UPDATE tabel lain saat flush. Tidak ditemukan pola
  `findOrCreate*` gaya entity biodata.
- *Getter yang menutup sesi Hibernate pemanggil*: TIDAK ditemukan langsung;
  yang ada efek `check()` warisan `GeneralValueObject` (bisa membuka session
  sendiri untuk object detached).
- *Peta lokasi berkas JSON + kuintet `ambilLokasi/tulisLokasi/...`*: TIDAK ADA
  di entity ini.

**Temuan/kuirk lain (dicatat, TIDAK diperbaiki)**:
- `retreiveDetailVerifikasiNilai(SkripsiPunyaKomponenPenilaianSkripsi, Dosen)`
  membandingkan token pertama `detailNilai` (yang berisi id
  `KomponenPenilaianSkripsi`) dengan id `SkripsiPunyaKomponenPenilaianSkripsi` —
  DUA RUANG ID BERBEDA, sehingga bendera verifikasi praktis selalu `false`.
  Ini pola yang sama dengan bug id anak vs id utama di `Pertemuan.java`. Dampak
  saat ini terbatas: satu-satunya pemanggilnya,
  `reloadSkripsiPunyaKomponenPenilaianSkripsi`, sudah tidak dipanggil dari mana
  pun (kode mati).
- `getLulus()` menyimpulkan LULUS untuk skripsi yang BELUM DINILAI, karena
  `getNilaiHuruf()` mengembalikan `"-"` (tidak kosong, tidak memuat D/E/T).
  Cabang `if (nilaiHuruf == null) lulus = false;` karenanya tidak pernah
  tercapai.
- `checkMaksSksDosen` tidak menghitung slot `penguji5`; `populateDosenPenguji()`
  juga melewatkan penguji V. Pemeriksaan `dosen == null` di dalamnya kode mati.
- `getTotalNilai()` selalu membagi 100, bukan jumlah persentase slot aktif —
  total mengecil bila konfigurasi bobot tidak berjumlah 100.
- `refreshNilaiKeDefault(dosen)` membangkitkan rincian nilai atas nama dosen yang
  kebetulan membuka layar; dosen berikutnya melihat nilai nol karena rincian
  sudah tidak kosong.
- Parameter/variabel bernama `skripsiPunyaKomponenPenilaianSkripsis` sebenarnya
  berisi id `KomponenPenilaianSkripsi`.
- Getter yang mengisi field saat dibaca: `getWaktuSidang()`/
  `getWaktuSampaiSidang()` mengisi jam server saat ini, `getTahunAkademik()`
  mengisi periode berjalan, `getCatatanPenting()` mengisi teks baku 14 hari
  kerja, `getSmt()`/`getTahun()`/`getSemester()`/`getTelahSidang()`/
  `getSetujuiSidang()`/`getLulus()`/`getNilaiHuruf()`/`getTotalNilai()` menulis
  state.
- Getter slot dosen dapat MENG-NULL-KAN relasi di memori bila slot dinonaktifkan
  di `FormatNilaiSkripsi` — bila entity lalu di-flush, relasi hilang dari DB.
- `setDisposisiSop(null)` diabaikan diam-diam: tautan SOP tidak bisa dilepas
  lewat setter.
- Import `ais.ui.util.MyMessageboxConfig` tidak terpakai.

## `ais/database/model/Matakuliah.java` — SELESAI 100% (2 Sep 2026)

Entity **definisi mata kuliah** di kurikulum (kode/nama/SKS, tanpa keterikatan waktu).
976 → 2284 baris, **120/120 method ber-Javadoc (100%)** (diaudit skrip, 0 tersisa).
Revisi: **r83072** (class-level + field + blok audit + konstruktor + identitas) dan
**r83085** (sisanya, tuntas). Potongan sempat tersapu revisi gabungan sesi paralel
r83076/r83077 (pesan kosong) — normal, isi terverifikasi. Sudah di-mirror ke `java/`
(`cmp` byte-identik setelah `svn update`).

**Struktur**: identitas (kode/nama/namaEn/singkatan/prefix/jurusan) → bobot SKS total +
4 rincian bentuk pembelajaran → penggolongan (status/jenis/kelompok/tingkat kesulitan/
aktif/modul/pra-perkuliahan) → kurikulum-OBE (deskripsi + 4 kolom CSV id) → kelengkapan
perangkat ajar + UTS/UAS → Feeder → 6 method flag store ekivalensi. Relasi kurikulum,
prasyarat, dan ekivalensi TIDAK ada sebagai field — dipegang entity seberang
(`KurikulumPunyaMatakuliah`, `MatakuliahPrasyarat`, `MatakuliahEkivalen`), sebab itu
"semester ke berapa" bukan kolom di sini.

**Pola berulang yang DITEMUKAN di sini**:
- *Getter menutup sesi Hibernate pemanggil*: **ADA**. `reInitEkivalen()` dan
  `ambilEkivalen(String)` memanggil `session.close()` + `HibernateUtil.closeSession()`
  atas sesi thread-local pemanggil.
- *Field audit `oleh`/`olehId`/`tanggal_dirubah` di-shadow ulang*: **ADA**, sama seperti
  `Dosen`/`Pegawai`.
- *Getter `findOrCreateX` + Levenshtein yang menulis baris master baru*: **TIDAK ADA**
  di file ini (diverifikasi grep: tidak ada `findOrCreate`/`save`/`persist`).

**Temuan/kuirk (didokumentasikan, TIDAK diperbaiki)**:
1. `getMilikUniversitas()` **selalu mengembalikan `true`** — pemeriksaan `if (x == null)`
   dikomentari sehingga penugasan berjalan tanpa syarat; karena property-access, nilai
   `true` itu ikut di-flush dan menimpa kolom. Dampak terbatas: tidak ada satu pun
   pemanggil `matakuliah.getMilikUniversitas()` di pohon sumber.
2. **Banyak getter tidak bebas efek samping** dan bisa memicu `UPDATE` + baris Envers baru
   hanya karena dibaca: `getKode()` (menghapus spasi/tanda hubung permanen bila konfigurasi
   `matakuliah_tanpa_spasi` aktif — berisiko memutus pencocokan kode Feeder), `getStatus()`,
   `getSks()`, `getSksDiskusi()`, `getFeeders()`, seluruh `getTerdapat*`/`getMerupakanMk*`,
   dan 4 getter CSV OBE.
3. `getSksDiskusi()` **mengisi dirinya sendiri** dengan `getSks()` bila keempat rincian SKS
   nol — nilai bentukan ini tersimpan ke DB, jadi mata kuliah yang belum dirinci
   otomatis tercatat 100% tatap muka (disengaja demi ekspor Feeder).
4. Bendera `terdapatPraktek`/`terdapatDiskusi`/`terdapatSimulasi`/`terdapatPraktekLapangan`/
   `merupakanMkPraktek`/`merupakanMkTeori` adalah **turunan yang menimpa kolomnya**;
   setter-nya tidak berpengaruh dan kolomnya tidak boleh dipakai di `Restrictions`/HQL.
5. Empat getter CSV OBE (`bahanKajian`, `capaianLulusan`, `capaianPembelajaranLulusan`,
   `profilLulusan`) adalah salinan algoritma yang sama: dedup lewat `HashSet` sehingga
   **urutan hilang**, daftar kosong dikembalikan sebagai `",,"` (bukan `""`), dan
   pemeriksaan `== null` pada baris `return` adalah **kode mati**.
6. **Asimetri ekivalensi**: `reInitEkivalen()` mengumpulkan baris dua arah (sengaja, agar
   `EkivalenNilaiUtil` bisa mengelompokkan nilai ganda), sedangkan `ambilEkivalen()` hanya
   meresolusi arah sumber→target. Jalur pemulihan `ambilEkivalen()` (saat JSON rusak)
   mengembalikan hasil `reInitEkivalen()` — dua arah, tanpa filter NIM — jadi bentuk
   hasilnya berbeda dari jalur normal.
7. `removeEkivalen()` hanya mengosongkan nilai kunci, tidak menghapusnya — peta JSON hanya
   bisa tumbuh sampai `reInitEkivalen()` menimpanya.
8. `ambilLokasiEkivalen()`/`tulisLokasiEkivalen()` memanggil `getId().toString()` di luar
   blok `try` → **NPE** pada entity yang belum tersimpan.
9. Field `public transient String descKurikulum` **tidak dipakai sama sekali** (nihil di
   seluruh pohon sumber, termasuk `.zul`).
10. `status` dan `jenisMatakuliah` disimpan sebagai **teks bebas**, bukan FK: `status`
    didenormalisasi dari nama `StatusMatakuliah` (pemetaan balik di `FeederExporterGenerator`
    memakai perbandingan nama), dan label `jenisMatakuliah` diambil dari berkas bahasa
    sehingga isinya ikut bahasa antarmuka saat penyimpanan.

## `ais/database/model/KrsMahasiswa.java` — SELESAI 100% (2 Sep 2026)

748 → 1695 baris, **87/87 method ber-Javadoc (100%)** (diaudit skrip, 0 tersisa).
Kompilasi `-implicit:none` lulus; kode diverifikasi tidak berubah (diff setelah
seluruh komentar dilucuti = identik). Commit tersapu ke revisi gabungan sesi
paralel **r83076** (pesan kosong — normal di WC ini), isi diverifikasi lewat
`svn diff -c 83076` (992 baris tambahan) dan penanda teks di HEAD. Mirror
`java/` sudah `svn update` + `cmp` byte-identik (r83080).

**Koreksi asumsi penugasan** (penting untuk sesi berikutnya): `KrsMahasiswa`
BUKAN "pendaftaran mahasiswa ke satu `Perkuliahan`". Satu baris =
1 mahasiswa × 1 semester × 1 tahapan × 1 penanda semester pendek — yaitu
**kepala/rekap KRS per semester** (kunci alami `kodeUnik` =
`idMahasiswa-semester-tahapan-semesterPendek`, kolom `unique`). Pengambilan mata
kuliah per baris DAN status persetujuan dosen PA (`getPersetujuan()`,
`BELUM_DISETUJUI`/`DISETUJUI`) tinggal di `Detailperkuliahan` — entity ini tidak
punya field approve/reject sama sekali. Yang ada hanya `dikunci` (Tbmuser
pengunci) dan `aktif`. Isinya angka rekap (SKS diambil/kumulatif/lulus, SKS
konversi vs bukan konversi, IPS, IPK, jumlah komentar, dosen PA, kelas) yang
dihitung ulang oleh `KrsDanSkripsiHelper.singkronkanKrsMahasiswa(...)`.

**Peran kedua**: `extends VOPembelajaran implements VOPesertaPembelajaran`
bukan karena KRS adalah mata kuliah, melainkan karena kepala KRS adalah salah
satu dari 16 induk tabel `pertemuan` — yakni **bimbingan/konsultasi dosen PA**
(`Pertemuan.getKrsMahasiswa()`). Field `jenis`, `tanggalAwalBimbingan`,
`lewatiTanggalMerahNasional`, `course`, `urutkanotomatis`, `noSk`, `tglSk`
melayani peran ini (dibaca `PenjadwalanHelper`), bukan peran rekap nilai.

**Pola berulang lintas entity — hasil verifikasi di file ini:**
- *Getter berefek samping menulis balik properti ter-map*: **ADA, banyak** —
  `getNama`, `getKodeUnik`, `getTahunAkademik`, `getNoUts`, `getNoUas`,
  `getSksYangDiambil`, `getIpk`, `getDosenPa`, `getKelas`. Merender layar bisa
  memicu `UPDATE` saat flush. `getNoUts`/`getNoUas` bahkan **membangkitkan**
  nomor ujian (yyMM + 6 digit acak) pada pembacaan pertama — nomor "terbit"
  tanpa aksi pengguna, kolomnya tidak `unique` sehingga tabrakan mungkin.
- *Getter menulis baris master baru ke DB (pola `findOrCreate*`)*: **TIDAK ADA**
  di file ini. Tapi `getKelas()` mendekati: ia menulis ke entity LAIN
  (`mahasiswa.setKelas(...)`) dan menelusuri mundur semester demi semester
  memanggil `Common.singkronkanKrsMahasiswa(..., jikaTidakAdaKembali=true)` —
  tidak membuat baris baru, tapi bisa membuka session/transaksi sendiri.
- *Getter menutup sesi Hibernate pemanggil*: **TIDAK ADA** langsung di file ini
  (penutupan sesi terjadi di `KrsDanSkripsiHelper`, bukan di entity).
- *Field audit di-shadow ulang*: **ADA** — `id`, `nama`, `keterangan`, `oleh`,
  `olehId`, `tanggal_dirubah` dideklarasikan ulang padahal `GeneralValueObject`
  sudah punya; field induk jadi mati untuk entity ini.

**Temuan/kuirk lain (dicatat, TIDAK diperbaiki):**
- `getKelas()` memanggil `mahasiswa.getKelas()` di baris pertama padahal cek
  `mahasiswa != null` baru ada di baris ke-4 blok — NPE bila relasi kosong.
- `getDosenPa()` menelan exception tanpa audit (`e.printStackTrace()` sengaja
  dikomentari) dan membandingkan `jumlah_semester < semester` memakai **field**
  `semester` mentah → NPE senyap bila semester null.
- `getNama()`/`getKodeUnik()` memakai field `mahasiswa` langsung (tanpa
  `check()`), sedangkan `getTahunAkademik()` memakai `getMahasiswa()` — tidak
  konsisten.
- Kolom warisan menyesatkan: `sksKonversi` → kolom `mkbelumdiniali` (salah ketik
  aslinya), `sksBukanKonversi` → `mkkbelumdinilai`. Nama kolom tidak lagi
  mencerminkan isi.
- `sksYangDiambilS`/`skskS` = CSV id `Detailperkuliahan` yang disisipkan apa
  adanya ke `Restrictions.sqlRestriction("id in (" + sks + ")")` di
  `PenilaianUtil.downloadSemuaKRS` — aman selama tetap mesin yang mengisi.
- `parameterData(...)` mengisi `ip_round_i`/`ip_round` dengan `Math.floor(...)`,
  bukan `Math.round(...)` — beda dengan pasangan `ipk_round` di atasnya.
- `public transient boolean berubah` tidak pernah dibaca/ditulis di mana pun.
- `VOPembelajaran.ambilSemester()`/`ambilJenisSemester()` sudah punya cabang
  `instanceof KrsMahasiswa`, jadi override di sini redundan (dibiarkan).

## `ais/database/model/BiodataPegawai.java` — SELESAI 100% (2 Sep 2026)

707 → 1633 baris, **94/94 method ber-Javadoc (100%)**. Revisi **r83069**. Dikompilasi
(`-implicit:none`, KantinHelper.java yang sedang `M` oleh sesi lain dioverlay dari
HEAD), CRLF utuh, mirror `java/` byte-identik.

**Jawaban atas hipotesis "pola biodata berulang"** (dicek langsung, bukan asumsi):

- Getter yang diam-diam menulis baris master baru ke DB — **ADA, tapi BENTUKNYA
  BEDA**. Tidak ada field wilayah/propinsi/kota sama sekali di entity ini, jadi
  `findOrCreatePropinsi()`/pencocokan Levenshtein ala `BiodataMahasiswa`/
  `BiodataCalonMahasiswa` **TIDAK ADA**. Yang ada: helper privat
  `ambilBiodataDosen()` → `Pegawai.getDosen().ambilBiodata()` yang sama dengan
  `ambilBiodata(true)` → bila dosen belum punya biodata, method itu membuka
  transaksi sendiri dan **INSERT baris `biodata_dosen` baru**. Karena
  `ambilBiodataDosen()` dipanggil di awal hampir SEMUA getter, membaca
  `getHobi()` saja bisa menulis ke DB.
- Getter yang menutup session Hibernate pemanggil — **ADA (transitif)**.
  `Dosen.ambilBiodata()` memanggil `HibernateUtil.closeSession()` di cabang
  pencarian maupun cabang penyimpanan.
- Field audit di-shadow padahal sudah ada di `GeneralValueObject` — **ADA, empat
  properti**: `id`, `oleh`, `olehId`, `tanggal_dirubah` dideklarasikan ulang
  lengkap dengan getter/setter-nya.

**Temuan khas file ini** (dicatat, TIDAK diperbaiki):

- Hampir semua getter **menimpa field lokalnya** dari `BiodataDosen` bila pegawai
  merangkap dosen. Dengan `dynamicUpdate = true`, membaca biodata bisa memicu
  `UPDATE biodata_pegawai` saat flush, dan nilai yang baru diset lewat setter bisa
  "hilang" pada pembacaan berikutnya.
- **Bug salah tempel**: `getSuratIzinMengemudi()` menimpa SIM dengan
  `ambilBiodataDosen().getHp()` — nomor HP, bukan SIM.
- **Bug pemetaan**: `@Column(name = "alamat_asal_s2")` terpasang di **setter**
  `setAlamatAsalS2`, bukan getter. Entity ini pakai property access (`@Id` di
  `getId()`), jadi anotasi di setter diabaikan Hibernate; nama kolom jatuh ke
  strategi bawaan dan — karena `hbm2ddl.auto=update` — kolom duplikat kemungkinan
  dibuat otomatis, sehingga nilai tidak mendarat di `alamat_asal_s2`.
- `toString()` membaca field `pegawai` mentah tanpa `check()`, bisa menghasilkan
  teks `"null"` untuk entity detached.
- Salah eja skema permanen: kolom `keahliah1`..`keahliah5` (hanya slot ke-1 yang
  nama method-nya ikut salah eja: `getKeahliah1`).
- Komentar generator lama keliru menyebut "BiodataMahasiswa" — sudah diganti.

## Batch "4 entity besar tambahan" — SELESAI 100% (2 Sep 2026, dikonsolidasi orkestrator)

Lanjutan batch `GeneralValueObject`+`Mahasiswa`+`Dosen`+`Perkuliahan`. Semua 4
file berikut TUNTAS 100% method, dikompilasi, dikommit bertahap, di-mirror ke
`java/` (verifikasi `cmp` byte-identik setelah `svn update` menyeluruh):
- `BiodataMahasiswa.java` — 229/229 method. 2000→4000 baris. r83000-83012.
- `Pegawai.java` — 361/361 method. 3711→6756 baris. r82998-83040. Field
  bayangan dikonfirmasi (16 field dari `Karyawan`, mirip `Dosen.java`) + bug
  perbandingan ID lintas-entity di `createDataPegawaiDariDosen`.
- `BiodataCalonMahasiswa.java` — 388/388 method. 3799→6792 baris. r82997-83043.
- `Pertemuan.java` — 395/395 method, entity TERBESAR (6529→11100 baris,
  bahkan lebih besar dari `Mahasiswa.java`). r83006-83051. **Koreksi penting**:
  bukan "1 sesi dari 1 Perkuliahan" seperti diasumsikan brief awal — tabel
  `pertemuan` dipakai bersama **16 jenis entity induk** (perkuliahan, KKN,
  PKL, skripsi, wisuda, dst.), rantai if/else di ~24 tempat per method.

**Pola berulang lintas 3 entity biodata** (BiodataMahasiswa/BiodataCalonMahasiswa
sesi ini + gejala serupa di Mahasiswa.java sesi lalu): getter yang DIAM-DIAM
menulis baris master baru ke DB (`findOrCreatePropinsi` dkk, pencocokan
Levenshtein), getter yang menutup sesi Hibernate pemanggil, field audit
(`oleh`/`olehId`/`tanggal_dirubah`) di-shadow ulang padahal sudah ada di
`GeneralValueObject`. Pola ini kemungkinan ADA JUGA di entity biodata lain
yang belum digarap (`BiodataPegawai.java`?) — cek saat menggarapnya.

**Bug data-correctness paling serius ditemukan sesi ini** (di `Pertemuan.java`,
BUKAN diperbaiki, hanya didokumentasikan): `removePertemuanFileContent`/
`removeVideoPertemuan`/`removeAudioPertemuan` memakai id ANAK sebagai id
PERTEMUAN saat baca/tulis peta lokasi berkas JSON → menyentuh peta yang salah.
`ambilTugasTotalSemua()` menggabung 2 tabel dengan `putAll` berkunci id
lintas-tabel → tabrakan id bisa menghilangkan tugas diam-diam. Lihat Javadoc
method masing-masing untuk detail lengkap — TIDAK dieskalasi sebagai task
terpisah (beda dengan hashCode/password) karena orkestrator menilai severity
belum jelas cukup tinggi untuk mengganggu pengguna lagi setelah 2 eskalasi
sebelumnya; pengguna bisa minta perbaikan langsung bila diinginkan.

**Total akumulasi 4 sesi kerja**: 190 (sesi 1-3) + 4 (`GeneralValueObject`+
`Mahasiswa`+`Dosen`+`Perkuliahan`) + 4 (`BiodataMahasiswa`+`Pegawai`+
`BiodataCalonMahasiswa`+`Pertemuan`) = **198 file** dari 7.401 (~2,7%), tapi
mencakup SEMUA entity paling sentral (>2000 method individual di file-file
besar ini saja).

## `ais/database/model/Pertemuan.java` — SELESAI 100% (2 Sep 2026)

Entity **terbesar** di `ais.database.model` setelah `Mahasiswa.java`: 6529 → 11100
baris, **395/395 method ber-Javadoc (100%)** (diaudit dengan skrip, 0 tersisa).
Revisi: r83006, r83015, r83024, r83026, r83045, ditambah potongan yang tersapu ke
revisi gabungan sesi paralel r83029/r83033 (chunk 5) dan r83046/r83049 (chunk 7) —
pesan kosong, normal di WC ini, isi sudah diverifikasi lewat `svn diff -c` dan
pemeriksaan penanda teks di HEAD.

**Temuan struktural terpenting** (mengoreksi asumsi awal penugasan): `Pertemuan`
BUKAN sekadar "satu tatap muka dari satu `Perkuliahan`". Tabel `public.pertemuan`
dipakai bersama oleh **16 jenis induk** (perkuliahan, jadwal pelajaran, kelas les,
bimbingan TA, skripsi, KRS/konsultasi PA, kelompok KKN, kelompok PKL, ujian PMB,
ujian PSB, pertemuan PSB, ujian pegawai, formulir kegiatan, grup pertemuan, produk
kursus, wisuda) — tepat satu kolom relasi terisi per baris. Akibatnya hampir setiap
method non-sepele berbentuk rantai `if/else if` panjang yang harus ditambah di ~24
tempat sekaligus bila ada jenis induk baru. Dua titik pusatnya: `untuk()` (nama
jenis induk) dan `ambilVOPembelajaran()` (induk sebagai `VOPembelajaran`).
Selain itu kelas ini `extends Tugas` (bukan langsung `GeneralValueObject`), jadi
satu Pertemuan sekaligus dapat berperan sebagai tugas.

**Dua pola yang didokumentasikan sebagai rujukan** (ditautkan dari method sejenis,
jangan diulang penjelasannya):

1. **Kolom teks 9 slot** — seluruh daftar hadir satu pertemuan ada di SATU kolom
   `absensi` (baris dipisah `;`, slot dipisah `,`). Tiga kolom sepupu memakai tata
   letak sama tetapi slot 4 berubah arti menjadi `dosen.id`: `keteranganKonfirmasi`,
   `keteranganSesuaiDenganRps`, `keteranganSesuaiOlehAkademik`. Rujukan:
   `populate(...)` dan `retreiveAbsensiId(...)`.
2. **"Peta lokasi" berkas JSON** — koleksi anak TIDAK dipetakan `@OneToMany`;
   tiap jenis anak punya berkas indeks JSON di disk plus kuintet
   `ambilLokasiXxx/tulisLokasiXxx/bersihkanLokasiXxx/reInitXxx/populateXxx/removeXxx`
   dan pembaca `ambilXxxTotal/ambilJumlahXxx/ambilXxx(map,mulai,banyak)`. Penanda
   "sudah dibangun" adalah `udah(nama)`/`belum(nama)` dari `GeneralValueObject`, dan
   NAMA-nya tidak selalu sama dengan nama method (mis. `"pertemuan_tugas"`,
   `"kelompok_tugas"`, `"pertemuan_punya_Ujian"` dengan U besar). Rujukan: kuintet
   `PengajuanIzinTidakMasukPerkuliahan`.

**Bug/kuirk yang DICATAT di Javadoc, sengaja TIDAK diperbaiki** (perlu keputusan
terpisah karena menyentuh perilaku produksi):

- `removePertemuanFileContent`, `removeVideoPertemuan`, `removeAudioPertemuan`
  memakai id **anak** sebagai id **pertemuan** saat membaca/menulis peta lokasi →
  menyentuh peta yang salah. (`populateXxx` padanannya benar.)
- `retreivePengajuanIzinIdKonfirmasi/...KonfirmasiRps/...OlehAkademik` bernama
  salah: slot 4 di kolom-kolom itu berisi `dosen.id`, bukan id pengajuan izin, dan
  slot itu pula yang dicocokkan → hasilnya selalu `dosen.getId()` atau `-1L`.
- `ambilTugasTotalSemua()` menggabungkan `TugasPertemuan` dan `TugasKelompok`
  dengan `putAll` berkunci id dari DUA tabel berbeda → tabrakan id membuat tugas
  perorangan hilang diam-diam. **Risiko nyata, bukan teoretis.**
- `hitungStatus()` menggabungkan dengan `putAll` (menimpa, bukan menjumlah).
- `reInitTugasKelompok` menyaring properti `"judul"` sedangkan pembacanya menyaring
  `getJudultugas()` — nama properti tidak selaras.
- `getOnlineMenggunakan()` menebak `JITSI` sebagai bawaan (bukan `TIDAK_AKTIF`).
- `generateJitsiLink`: cabang `jadwalUjianPSB` ganda (`I_`/`L_`, yang kedua mati);
  cabang perkuliahan tanpa awalan konteks → dua tenant bisa berbagi ruang Jitsi.
- Getter tautan daring MENIMPA teks undangan yang ditempel pengguna dengan URL
  pertama hasil pungutan; teks aslinya hilang pada penyimpanan berikutnya.
- `getMahasiswas()` punya penjaga anti-rekursi (autoflush Hibernate memanggil ulang
  getter di tengah query → StackOverflow) tetapi `getGurus()`/`getSiswas()` yang
  sama-sama menjalankan query TIDAK punya penjaga serupa.
- `getKurikulumPunyaMatakuliahDetail()` adalah getter yang dapat MEMBUAT baris
  template format bimbingan baru di basis data.
- `getIndikator/getWaktupembelajaran/getPengalamanBelajar/getTugasDanPenilaian`
  membaca `Common.getKonfigurasi(...)` yang menulis default ke DB bila kunci belum
  ada (lihat catatan auto-seed konfigurasi di memori proyek).
- `populateParameterTambahan` menyisakan `System.out.println("ket => "...)` yang
  jalan untuk setiap baris pada setiap penyimpanan.
- `getJurusan` vs `getJurusanId` dan `getSekolah` vs `getSekolahId` menempuh rantai
  induk BERBEDA → hasilnya dapat tidak selaras dalam satu laporan.
- `getYayasanId()` tidak menjaga proxy (dapat melempar `LazyInitializationException`)
  padahal `getSekolahId()` yang bersebelahan menjaganya.
- Parameter `tulisUlang` pada beberapa `populateXxx` sama sekali tidak dipakai.
- `removeXxx` tidak membuang entri melainkan mengosongkan nilainya → berkas peta
  terus membesar sampai `reInit` berikutnya.

Catatan EOL: berkas ini sebelumnya bercampur (93 baris LF di tengah berkas CRLF);
sekalian dinormalkan menjadi CRLF penuh pada commit pertama. Diff dengan
`--ignore-eol-style` hanya berisi penambahan Javadoc.

## `ais/database/model/Pegawai.java` — SELESAI 100% (2 Sep 2026)

Saudara `Dosen.java` (keduanya `extends Karyawan extends GeneralValueObject`).
**361/361 method ber-Javadoc (100%)**, 3711 → 6756 baris. Revisi:
r82998, r83007, r83018, r83023, r83027, lalu potongan terakhir tersapu ke
revisi gabungan sesi paralel r83029/r83033/r83037 (pesan kosong — normal di WC
ini, isi sudah diverifikasi lewat `svn diff -c`).

Poin penting untuk sesi berikutnya:

- **Pegawai bukan "non-dosen".** Ia kartu induk *kepegawaian*; `Dosen`/`Guru`
  adalah peran *akademik*. Dwifungsi normal: dosen yang login otomatis
  di-provisioning satu baris `Pegawai` lewat `createDataPegawaiDariDosen`.
  `getTipePegawai()` menyimpulkan DOSEN/GURU/STAF dari ada tidaknya relasi.
- **Pola getter "cermin" (read-through)** ke `Dosen`/`Guru` dipakai ~30 getter:
  nilai kolom lokal ditimpa dari sumber, jadi kolom di DB praktis diabaikan dan
  setter umumnya "hilang" pada pembacaan berikutnya (kecuali `setAlamat` dan
  `setKtp` yang menulis balik).
- **Field bayangan ADA**, sama seperti `Dosen.java`: `code`, `mycode`, `nama`,
  `alamat`, `email`, `telp`, `kelamin`, `tempatlahir`, `pangkat`, `golongan`,
  `jabatan`, `spesialisasi1..3`, `tanggallahir`, `tetap`, `idfinger`.
  **Bedanya dengan `Dosen`**: `jurusan`/`fakultas` milik `Karyawan` TIDAK
  dideklarasikan ulang dan getternya tidak di-override, jadi
  `pegawai.getJurusan()`/`getFakultas()` selalu null — padanan yang dipakai
  adalah `getTendikJurusan()`/`getTendikFakultas()`.
- **Empat rentang masa kerja paralel** (pengalaman kerja, honorer, semi tetap,
  tetap) yang getternya disaring `getTipeMasaKerja()`.
- **Penggajian dihitung ulang**, tidak disimpan: `ambilGajiPokok/Insentif/
  Makan/Transport(Date)` berbasis `KenaikanPangkat` dari cache `ConstantValues`.

## Batch "4 file besar" — SELESAI 100% (2 Sep 2026)

Semua 4 file rampung TUNTAS 100% method, dikompilasi, dikommit bertahap,
di-mirror ke `java/` (verifikasi `cmp` byte-identik oleh orkestrator setelah
`svn update` menyeluruh):

- `ais/database/model/GeneralValueObject.java` — base class utk **1.456
  subclass** (leverage tertinggi yang pernah ditemukan). 1807→3412 baris,
  SEMUA method ber-Javadoc. r82933 (+ r82936 tracker). Detail mekanisme
  `check()`/cache/thread-safety di entri `ais/database/model/` di bawah.
  **Bug arsitektur ditemukan**: `equals()` di-override berbasis `id` tapi
  `hashCode()` TIDAK di-override (di class ini maupun induk `DataUtil`) —
  dieskalasi ke task terpisah `task_9d2ca4da` (investigasi dampak, BUKAN
  bagian inisiatif dokumentasi ini, blast radius 1456 subclass jadi butuh
  kajian risiko dulu sebelum diperbaiki).
- `ais/database/model/Dosen.java` — 249/249 method (100%). 3742→5744 baris.
  r82927/82929/82935/82939/82946/82951/82953 (+ r82957 tracker).
- `ais/database/model/Perkuliahan.java` — 300/300 method (100%).
  3537→6346 baris. r82926/82932/82941/82945 (+ r82949 tracker).
- `ais/database/model/Mahasiswa.java` — 445/445 method (100%, file
  TERBESAR). 6403→10087 baris. r82928/82934/82939/82950/82952/82958/82963
  (+ r82966 tracker).

**Temuan penting lintas-file**: DAO layer (`ais/database/dao/GenericHibernateDao.java`
+ 286 `*DaoImpl`) TERNYATA SUDAH terdokumentasi baik dari sesi/inisiatif
SEBELUM proyek dokumentasi ini dimulai — jangan dianggap "belum digarap",
cek dulu kualitas Javadoc yang ADA sebelum menugaskan agent ke suatu paket
(bisa hemat banyak waktu, seperti kasus ini).

**Total akumulasi seluruh sesi (3 sesi kerja)**: 49 (`Revisi*Helper`) + 84
(`GetEventListener`+`AmbilData*Banbox`) + 53 (mandiri) + 4 (file besar,
tapi mewakili ~1450+ method individual di 4 file MASSIVE) = **190 file
langsung disentuh**, dari 7.401 total (~2,6%). Namun dampak *kualitas*
jauh lebih besar dari angka file: `GeneralValueObject.java` sendiri adalah
fondasi 1.456 entity lain, jadi nilai referensinya menyebar luas begitu
subclass-nya mulai ditautkan di sesi mendatang.

Format tiap baris: `- [status] path/File.java — catatan singkat (revisi svn, tanggal)`

Status: `[referensi]` = class induk/pola sudah didokumentasikan sangat detail (jadi
target link dari class lain), `[tautan]` = subclass/pemanggil tipis sudah ditautkan
ke referensi, `[lengkap]` = file berdiri sendiri sudah didokumentasikan penuh tanpa
perlu referensi eksternal, `[sebagian]` = baru sebagian method, `[belum]` = belum

Format tiap baris: `- [status] path/File.java — catatan singkat (revisi svn, tanggal)`

Status: `[referensi]` = class induk/pola sudah didokumentasikan sangat detail (jadi
target link dari class lain), `[tautan]` = subclass/pemanggil tipis sudah ditautkan
ke referensi, `[lengkap]` = file berdiri sendiri sudah didokumentasikan penuh tanpa
perlu referensi eksternal, `[sebagian]` = baru sebagian method, `[belum]` = belum
disentuh (default untuk semua file yang tidak disebut di sini).

## ais/ui/util/ (pola lintas-modul)

- [referensi] `GetEventListener.java` — interface kecil (2 method) diperkaya jadi
  referensi arsitektur pola "Bandbox picker" AIS: kerangka constructor/`display()`/
  `onSearchDefault()`/renderer-batin/callback yang identik di 83 file
  `AmbilData*Banbox` tersebar di banyak `ais.action.master.<modul>.helper`. r82818,
  dikompilasi & di-mirror (verifikasi `cmp` byte-identik).
- **Temuan penting (jangan diulang di sesi berikutnya)**: banyak file `AmbilData*Banbox`
  SUDAH punya Javadoc, tapi berupa TEMPLATE GENERIK hasil pass otomatis sebelumnya
  (ciri: frasa "Kelas ini memberi nama dan batas tanggung jawab yang eksplisit...",
  "Batas tanggung jawab:...", "Efek samping: nama operasi di atas menunjukkan...").
  Minimal 57 file di seluruh codebase mengandung frasa ini (`grep -rl "memberi nama
  dan batas tanggung jawab yang eksplisit"`), dan 1946 file mengandung marker
  `auto-audit(empty-catch)` (pass otomatis lain, urusan exception handling, mungkin
  turut menambah Javadoc generik). ~7405/7401 file sudah punya MINIMAL SATU blok
  `/**` — jadi skala kerja sebenarnya bukan "tulis dari nol" tapi mayoritas
  "tingkatkan Javadoc generik/template jadi spesifik & akurat", sesuai instruksi
  pengguna (jangan kurangi, kembangkan). Prioritaskan grep frasa template di atas
  untuk menemukan kandidat "sudah ada tapi dangkal" secara cepat.
- Batch penautan 83 file `AmbilData*Banbox` ke referensi ini sedang berjalan (4 agent
  paralel @ ~20-22 file) — hasil akan dicatat sesi berikutnya begitu selesai.

## ais/action/master/helper/

- [referensi] `GenericRevisiHelper.java` — Javadoc class-level ~900 kata (arsitektur
  3 tab, alur Envers/session, restore satu vs massal, hook
  `afterRestoreInTransaction`, extension point `QueryCustomizer`) + 33 method
  public/protected lengkap. r82750 (sebagian tersapu commit sesi lain tanpa pesan —
  lihat catatan) + r82752 (sisanya, pesan lengkap). Ini class REFERENSI untuk pola
  "window ZK riwayat revisi Envers" — 50 subclass di bawah ini menaut ke sini,
  JANGAN duplikasi penjelasan arsitekturnya di subclass.
- [tautan] 48 dari 50 subclass `Revisi*Helper` sudah ditautkan ke referensi di atas
  (Javadoc class-level diperkaya + `{@link GenericRevisiHelper}`, constructor diberi
  `@param`, override `afterRestoreInTransaction`/method lain didokumentasikan +
  `@see` balik ke hook induk). Semua sudah dikompilasi (javac 1.7, lulus) dan
  dikommit PER FILE dengan pesan sendiri, lalu di-mirror ke `java/` (r82808,
  verifikasi `cmp` byte-identik semua 49 file termasuk `GenericRevisiHelper.java`).

  Daftar file + revisi commit (WC src):
  - `RevisiBiodataCalonMahasiswaHelper.java` r82755
  - `RevisiBniRequestHelper.java` r82758
  - `RevisiBsiRequestHelper.java` r82760
  - `RevisiCicilanPembayaranHelper.java` r82761 (override `afterRestoreInTransaction`
    didokumentasikan detail — perbaikan `pengaturanPembayaranBulanan`)
  - `RevisiCicilanPembayaranTemporaryHelper.java` r82762
  - `RevisiDetailPembayaranSiswaHelper.java` r82763
  - `RevisiDetailPerkuliahanDariMahasiswaHelper.java` r82764
  - `RevisiDetailPerkuliahanHelper.java` r82765
  - `RevisiDiskusiHelper.java` r82766
  - `RevisiGeneralValueObject.java` r82767 — **legacy/dead**: arsitektur PRA-
    GenericRevisiHelper, dikonfirmasi TIDAK ADA subclass aktif lagi (grep). Jangan
    dicontoh untuk kode baru; didokumentasikan sebagai compatibility shim historis.
  - `RevisiGrupTransaksiHelper.java` r82768
  - `RevisiHasilUjianMahasiswaHelper.java` r82769
  - `RevisiHelper.java` r82770 — BUKAN subclass tipis biasa: berisi logika UI/utilitas
    nyata (`createNewRevisi`, `bolehLihatRevisi` via konfigurasi `boleh_lihat_revisi`,
    `updatePropertyAndSave`) dipakai luas dari banyak Action lain — didokumentasikan
    lebih lengkap (>200 kata).
  - `RevisiHistoryDetailPerkuliahanHelper.java` r82771
  - `RevisiHistoryKRSDetailPerkuliahanHelper.java` r82772
  - `RevisiHistoryPertemuanHelper.java` r82773
  - `RevisiItemHelper.java` r82774
  - `RevisiKegiatanHelper.java` r82775
  - `RevisiKegiatanTemporaryHelper.java` r82776
  - `RevisiMahasiswaHelper.java` r82777
  - `RevisiMatakuliahHelper.java` r82778
  - `RevisiNotifikasiHelper.java` r82780
  - `RevisiParameterTambahanHelper.java` r82781
  - `RevisiPembobotanNilaiHelper.java` r82782 — dicatat: entity class-nya bernama
    `PembombotanNilai` (typo historis di kode asli, BUKAN typo dokumentasi baru).
  - `RevisiPerkuliahanHelper.java` r82784 — dicatat: filter `perkuliahan` SELALU
    dipasang meski `null` (beda dari pola subclass lain yang skip filter bila null).
  - `RevisiPertemuanHelper.java` r82785
  - `ais/action/master/akunting/helper/RevisiUangMukaHelper.java` r82757
  - `ais/action/master/asset/helper/RevisiPemesananPengadaanMasterAssetDetailHelper.java` r82787
  - `ais/action/master/asset/helper/RevisiPemesananPengadaanMasterAssetHelper.java` r82788
  - `ais/action/master/asset/helper/RevisiPenerimaanPengadaanMasterAssetDetailHelper.java` r82789
  - `ais/action/master/asset/helper/RevisiPenerimaanPengadaanMasterAssetHelper.java` r82790
  - `ais/action/master/asset/helper/RevisiPermintaanPengadaanMasterAssetDetailHelper.java` r82791
  - `ais/action/master/asset/helper/RevisiPermintaanPengadaanMasterAssetHelper.java` r82792
  - `ais/action/master/asset/helper/RevisiSaldoAwalMasterAssetDetailHelper.java` r82793
  - `ais/action/master/asset/helper/RevisiSaldoAwalMasterAssetHelper.java` r82794
  - `RevisiPertemuanKknHelper.java` r82795
  - `RevisiPertemuanPklHelper.java` r82796
  - `RevisiSiswaHelper.java` r82797
  - `RevisiSkripsiHelper.java` r82798
  - `RevisiTagihanHelper.java` r82799
  - `RevisiUjianHelper.java` r82800 (override `afterRestoreInTransaction`
    didokumentasikan detail — restore master-detail atomik `UjianPunyaSoal`)
  - `RevisiVirtualAccountBankHelper.java` r82801
  - `RevisiWorkspaceHelper.java` r82802
  - `ais/action/master/sekolah/helper/RevisiCalonSiswaHelper.java` r82803
  - `ais/action/master/sekolah/helper/RevisiPembayaranSiswaDetailHelper.java` r82804
    (inner `MilikSiswaFilter` diperkaya, ganti template auto-audit generik dengan
    penjelasan nyata workaround scoping dua-tingkat)
  - `ais/action/master/sekolah/helper/RevisiPembayaranSiswaHelper.java` r82805
  - `ais/action/master/sop/helper/RevisiDisposisiAlurSopHelper.java` r82806
  - `ais/action/master/sop/helper/RevisiDisposisiSopHelper.java` r82807

- [belum] **2 file TERTUNDA** — terdeteksi sesi paralel lain sedang aktif mengedit
  area terkait (fitur e-learning "recovery aktivitas pembelajaran", commit r82783 +
  turunannya, masih berlangsung per 1 Sep 2026 malam: lihat `RekapitulasiTugasHelper`,
  `RekapitulasiUjianHelper`, `TugasKelompokHelper`, `RecoveryAktivitasPembelajaranHelper`
  berstatus M/baru saat pengecekan terakhir). JANGAN disentuh sampai sesi itu selesai
  (cek `svn log -l 3` dulu):
  - `ais/action/master/helper/RevisiPertemuanPunyaUjianHelper.java`
  - `ais/action/master/helper/RevisiTugasHelper.java`

- [lengkap] **4 file window ZK modul wisuda** (`ais/action/master/helper/`) — berdiri sendiri,
  tanpa pola referensi bersama. Semua sudah punya Javadoc class-level TEMPLATE GENERIK dari pass
  otomatis 1 Sep 2026 (r79992-r80004, pesan "docs: jelaskan tanggung jawab dan kelompok operasi
  class menengah") yang diganti dengan penjelasan konkret domain wisuda; semua method public
  (constructor, `init()`, handler tombol) didokumentasikan lengkap. Dikompilasi (javac 1.7,
  `-implicit:none`, lulus) dan dikommit per file (2 dari 4 tersapu commit sesi paralel lain tanpa
  pesan — isi diverifikasi benar via `svn diff -c <rev>` sebelum lanjut).
  - `GenerateNoKursiDanNoRegistrasiWindow.java` r82865 (*) — satu-satunya varian yang men-generate
    No. Registrasi DAN No. Kursi sekaligus dalam satu window.
  - `GenerateNoKursiWindow.java` r82867 — kuirk: handler tombol "Generate" bernama
    `onGenerateLaporanRegistrasiWisuda()` tapi isinya men-generate No. KURSI (sisa copy-paste, nama
    tidak diperbarui); ada blok kode mati skema penomoran lama berbasis `Projections.rowCount()`
    yang sudah ditinggalkan.
  - `GenerateUndanganWisudaWindow.java` r82881 — satu-satunya window yang murni cetak (tidak
    men-generate nomor apa pun); validasi 3 prasyarat sebelum cetak (nama ayah di
    `BiodataMahasiswa`, No. Registrasi, No. Kursi); tombol Batal men-detach `Tabpanel` induk.
  - `LaporanRegistrasiWisudaWindow.java` r82889 — padanan No. Registrasi saja dari
    `GenerateNoKursiWindow`; di sini nama method `onGenerateLaporanRegistrasiWisuda()` SESUAI
    isinya (kontras dengan kuirk penamaan di `GenerateNoKursiWindow`); cetak selalu pakai literal
    `"pdf"` (bukan konstanta `Report.PDF` atau pilihan Combobox).

  **Temuan lintas-file penting**: baik "No. Registrasi Wisuda" maupun "No. Kursi Wisuda" di
  keempat window ini SAMA-SAMA dihasilkan dari `pendaftaranWisuda.getId().toString()` (primary key
  baris `PendaftaranWisuda`) yang di-pad nol jadi 8 digit — BUKAN dari counter/sequence terpisah.
  Akibatnya untuk mahasiswa yang sama, kedua nomor akan bernilai string identik. Kemungkinan besar
  bukan maksud bisnisnya, tapi tidak diubah (tugas ini hanya Javadoc). Validasi 5 status persetujuan
  wisuda (Administrasi/Administrasi Fakultas/Keuangan/Perpustakaan/Perpustakaan Fakultas)
  diduplikasi copy-paste identik di keempat file, tidak ada helper validasi bersama — kandidat
  refactor/dedup di masa depan (di luar cakupan tugas dokumentasi ini).

- [referensi] `ais/ui/util/GetEventListener.java` — Javadoc interface diperkaya jadi referensi
  arsitektur lengkap untuk pola "Bandbox picker" AIS (subclass `AmbilData*Banbox`, 83 file
  tersebar di banyak package `ais.action.master.<modul>.helper`): kerangka
  constructor/`display()`/`onSearchDefault(Event)`/renderer batin/callback
  `getEventListener()`/`setEventListener()` yang identik di semua subclass. Dikerjakan sesi
  terpisah (bukan sesi ini) — lihat isi filenya untuk detail lengkap. Semua subclass di bawah ini
  menaut ke sini.
- [tautan] 22 dari 83 subclass `AmbilData*Banbox` (`ais/action/master/helper/`) sudah ditautkan ke
  referensi di atas: Javadoc class-level diganti dari template generik/tanpa Javadoc jadi
  penjelasan konkret (entity spesifik, field & logika pencarian, checkbox/radio, constructor
  dengan parameter tambahan), constructor/`display()`/`onSearchDefault()`/renderer/getter-setter
  diberi Javadoc method. Semua dikompilasi (javac 1.7, lulus) dan dikommit — SEBAGIAN besar
  ter-*sweep* oleh commit sesi paralel lain tanpa pesan/tanpa scope path eksplisit (pola yang sama
  seperti insiden `GenericRevisiHelper` di atas); isi diverifikasi benar via `svn diff -c <rev>`
  sebelum dilanjutkan, tidak ada kerja yang hilang.

  Daftar file + revisi commit (WC src) — file bertanda (*) landasan di revisi commit sesi paralel
  lain (sweep), file lain dikommit langsung oleh sesi ini:
  - `AmbilDataItemBiayaBanbox.java` r82825
  - `AmbilDataJamPerkuliahanBanbox.java` r82827 — constructor kedua dengan filter `Jurusan` induk,
    aksi CRUD inline admin (tambah/ubah/hapus) di renderer.
  - `AmbilDataJurnalPenelitianBanbox.java` r82832 (*) — paging server-side
    `AmbilDataPagingHelper` + `BanboxFilterToggle`, tombol tambah jurnal inline.
  - `AmbilDataJurusanBanbox.java` r82832 (*) — dicatat: `pagingHelper` dideklarasikan tapi tidak
    dipakai (masih paging client-side lama).
  - `AmbilDataKecamatanBanbox.java` r82834 (*) — entity `Wilayah` generik multi-level (bukan
    cuma kecamatan): constructor kedua dengan `level` eksplisit, integrasi PMB Arkatama, method
    statis `tambah()` untuk buat negara/propinsi/kota/kecamatan baru berjenjang dari popup.
  - `AmbilDataKelasBanbox.java` r82838 (*) — filter tahun angkatan dengan parsing input bebas
    (`ambilTahunAngkatanFilter`), paging server-side dengan reset halaman per perubahan filter.
  - `AmbilDataKelasPertemuanBanbox.java` r82838 (*) — field dosen berupa Bandbox nested
    (`AmbilDataDosenBanbox`) dicocokkan ke 10 kolom `dosen1`..`dosen10`.
  - `AmbilDataKelompokKknBanbox.java` r82838 (*) — hasil di-scope otomatis ke fakultas/jurusan
    pengguna login, radio dinonaktifkan bila kuota kelompok penuh.
  - `AmbilDataKelompokPklBanbox.java` r82838 (*) — padanan PKL dari `AmbilDataKelompokKknBanbox`,
    struktur identik.
  - `AmbilDataKonfigurasiBanbox.java` r82842 (*) — constructor memanggil `display()` langsung
    (bukan lazy `onOpen`), field `info` dicocokkan ke 5 kolom info1..info5 sekaligus (OR).
  - `AmbilDataKotaKabupatenBanbox.java` r82843 — entity `Wilayah` level "2" (versi sederhana dari
    `AmbilDataKecamatanBanbox`, level dikunci hardcode, tanpa fitur tambah cepat).
  - `AmbilDataKurikulumBanbox.java` r82848 — baris grid punya `MyDetail` yang bisa diperluas,
    memuat `DetailSemesterKurikulumHelper` inline.
  - `AmbilDataMahasiswaBanbox.java` r82850 — subclass paling kaya fitur (4 constructor overload,
    scoping `PerguruanTinggi` multi-tenant, sub-query status berjalan via
    `history_status_mahasiswa`, field nested `AmbilDataKelasBanbox`/`AmbilDataDosenBanbox`).
    Dicatat kuirk: constructor `(Boolean hanyaYangAktif)` mengabaikan parameternya sendiri
    (selalu memanggil `this(false, false)`).
  - `AmbilDataMahasiswaDaftarSidangBanbox.java` r82853 — root criteria `PendaftaranSidang`
    (BUKAN `Mahasiswa` langsung) dengan sub-criteria berjenjang `skripsi.mahasiswa`.
  - `AmbilDataMahasiswaKonversiBanbox.java` r82854 — filter tetap `statusKonversi == 1`.
  - `AmbilDataMahasiswaSkripsiBanbox.java` r82855 (*) — Combobox fakultas/prodi dikunci sesuai
    kewenangan pengguna login, try-catch khusus untuk lock timeout PostgreSQL 55P03.
  - `AmbilDataMahasiswaTanpaDosenPa.java` r82855 (*) — dicatat: nama kelas menyiratkan filter
    "tanpa dosen PA" tapi kode saat ini TIDAK memuat filter terkait `dosenPa` sama sekali;
    didokumentasikan apa adanya, bukan diasumsikan.
  - `AmbilDataMasaPerkuliahanBanbox.java` r82855 (*) — mem-preselect `MasaPerkuliahan` dengan
    `defaultData == true` langsung dari constructor, aksi CRUD inline dua lapis (config +
    jenis pengguna), toggle "Default" meng-unset default baris lain via SQL langsung.
  - `AmbilDataMatakuliahBanbox.java` r82857 (*) — DUA MODE: master biasa vs "mata kuliah milik
    mahasiswa" (constructor dengan `Mahasiswa`, query riwayat KRS `Detailperkuliahan` bukan
    tabel `Matakuliah` langsung, gabung hasil asli + konversi).
  - `AmbilDataMenuBanbox.java` r82857 (*) — MENYIMPANG dari kerangka: popup berupa `Tree`
    hierarkis (bukan form+grid), tanpa field pencarian, checkbox pakai event `onClick` bukan
    `onCheck`.
  - `AmbilDataNamaSekolahBanbox.java` r82858 (*) — field tunggal dicocokkan ke kode ATAU nama
    (OR), tautan "Buat baru disini" inline, memanggil `NamaSekolahAsalAction.initdata()`.
  - `AmbilDataNegaraBanbox.java` r82858 (*) — paling sederhana: satu field, tanpa penanganan
    kosong khusus pada filter ilike.

  Semua 22 file di atas tadinya SUDAH punya Javadoc class-level (bukan kosong), tapi berupa
  template generik hasil pass otomatis sebelumnya (ciri: "Kelas ini memberi nama dan batas
  tanggung jawab...", "Batas tanggung jawab:...", "Efek samping: nama operasi di atas
  menunjukkan..." — kalimat abstrak tanpa detail spesifik file). Javadoc method-level (constructor,
  renderer, `display()`, `onSearchDefault()`) sebagian besar TIDAK ada sama sekali sebelumnya,
  ditambahkan penuh di sesi ini. Tidak ada file yang di-skip.

- [tautan] **Sisa 61 dari 83** subclass `AmbilData*Banbox` — SELESAI juga (9 agent tambahan,
  sebagian paralel bercabang dari 2 agent orkestrator lain). Total **83/83 file rampung**,
  dikompilasi (javac 1.7), dikommit, di-mirror ke `java/`, dan diverifikasi `cmp` byte-identik
  semua 83 file (termasuk `GetEventListener.java`). Sama seperti 22 file di atas: mayoritas
  tadinya template generik, diganti penjelasan spesifik + link wajib ke referensi; beberapa
  commit ter-*sweep* sesi paralel lain (pola sama, isi selalu diverifikasi benar).

  Daftar file + revisi (dikelompokkan per agent yang mengerjakan; lihat pesan commit svn per
  file untuk detail argumentasi lengkap):

  *akunting/asset/employ/helper (batch 1, 21 file)*:
  `akunting/helper/AmbilDataAkunBanbox.java` r82832 (sudah bagus, hanya ditaut) ·
  `akunting/helper/AmbilDataKasBesarBanbox.java` r82832 ·
  `akunting/helper/AmbilDataPegawaiBanbox.java` r82832 ·
  `akunting/helper/AmbilDataReimbursementBanbox.java` r82832 (sudah bagus) ·
  `akunting/helper/AmbilDataUangMukaBanbox.java` r82832 ·
  `asset/helper/AmbilDataAssetDetailBanbox.java` r82832 ·
  `asset/helper/AmbilDataMasterAssetBanbox.java` r82834 ·
  `employ/helper/AmbilDataJenisPelatihanBanbox.java` r82834 ·
  `helper/AmbilDataAfiliasiCalonMahasiswaBanbox.java` r82838 ·
  `helper/AmbilDataBerkasBanbox.java` r82841 ·
  `helper/AmbilDataCalonMahasiswaBanbox.java` r82841 ·
  `helper/AmbilDataCalonMahasiswaCekKesehatanBanbox.java` r82843 (constructor panggil
  `display()` langsung, menyimpang pola `onOpen` lazy) ·
  `helper/AmbilDataCalonMahasiswaDaftarUlangBaruBanbox.java` r82843 ·
  `helper/AmbilDataCalonMahasiswaGenerateNimBanbox.java` r82851 (sama, `display()` langsung) ·
  `helper/AmbilDataAsramaBanbox.java` r82855 ·
  `helper/AmbilDataDetailPerkuliahanBanbox.java` r82855 ·
  `helper/AmbilDataDosenBanbox.java` r82855 (2 dari 4 constructor overload mengabaikan
  parameternya sendiri) ·
  `helper/AmbilDataDosenSkripsiBanbox.java` r82855 ·
  `helper/AmbilDataFormSopBanbox.java` r82855 (sumber data `ConstantValues.treeMapFormSop`,
  BUKAN entity Hibernate) ·
  `helper/AmbilDataGelombangPendaftaranBanbox.java` r82855 ·
  `helper/AmbilDataGolonganBanbox.java` r82855.

  *rab/helper (8 file, 2 agent)*:
  `AmbilDataKppnBanbox.java` r82832 ·
  `AmbilDataMitraBanbox.java` r82846 (scoping satker hierarkis) ·
  `AmbilDataPejabatBanbox.java` r82849 (auto-pilih & kunci dari user login) ·
  `AmbilDataProyekBanbox.java` r82852 (filter exact-match) ·
  `AmbilDataSasaranBanbox.java` r82823 (scoping satuan kerja + anak-cucu via
  `SatuanKerjaTreeModel`) ·
  `AmbilDataSatuanKerjaBanbox.java` r82832 (picker berbasis Tree, domain-lock, 3 constructor,
  2 tab popup — non-standar) ·
  `AmbilDataSumberDanaBanbox.java` r82832 ·
  `AmbilDataWorkspaceBanbox.java` r82834 (Tree-based, 4 constructor termasuk mode terkunci).

  *sekolah/helper (7 file, 2 agent)*:
  `AmbilDataCalonSiswaBanbox.java` r82830 (sudah bagus, hanya ditaut) ·
  `AmbilDataGuruBanbox.java` r82832 (filter kode/NUPTK, status kepegawaian, penugasan
  4-sekolah, kecuali guru "milik universitas") ·
  `AmbilDataKelasLesSiswaBanbox.java` r82832 (kelas les bukan reguler, filter guru pembina
  bercabang 2 flag constructor) ·
  `AmbilDataKelasSiswaBanbox.java` r82832 (kelas reguler, filter tahun akademik + cross-check
  `JadwalPelajaran` 12 slot guru) ·
  `AmbilDataKelasSiswaSemuaBanbox.java` r82832 ·
  `AmbilDataMatapelajaranBanbox.java` r82838 (pencarian hanya lewat tombol Cari, tanpa
  onOK/onChange otomatis) ·
  `AmbilDataSiswaBanbox.java` r82839 (sudah detail — mode alumni dll — hanya ditaut +
  Javadoc getter/setter listener yang sebelumnya kosong).

  *library/lkp/surat/helper (5 file)*:
  `library/helper/AmbilDataKategoriItemBanbox.java` r82832 (Tree hierarkis, tab "Sering
  Dipakai", parameter `chooseAll`) ·
  `library/helper/AmbilDataUdcItemBanbox.java` r82861 (constructor non-standar: Bandpopup/
  Radiogroup eager + flag `hasDisplayed`, bukan lazy `getChildren().isEmpty()`) ·
  `lkp/helper/AmbilDataKegiatanTugasJabatanTreeBanbox.java` r82837 (scoping satuan
  kerja/role/periode/aktif, kuirk per-overload constructor) ·
  `surat/helper/AmbilDataAlurPersetujuanSuratKeluarBanbox.java` r82840 (approval workflow
  hierarkis, scoping multi-dimensi satker/fakultas-jurusan/yayasan-sekolah/tipe) ·
  `surat/helper/AmbilDataAlurPersetujuanSuratMasukBanbox.java` r82844 (padanan disposisi surat
  masuk, event `onClick` bukan `onCheck`, tanpa guard null di renderer).

  *helper (Batch A, 5 file)*:
  `AmbilDataParameterTambahanBanbox.java` r82822 ·
  `AmbilDataPenjelasanBankSoalBanbox.java` r82831 ·
  `AmbilDataPerguruanTinggiLainBanbox.java` r82832 ·
  `AmbilDataPertemuanBerdasarKelasPertemuanBanbox.java` r82834 ·
  `AmbilDataPropinsiBanbox.java` r82838.

  *helper (Batch B, 5 file)*:
  `AmbilDataRuangBanbox.java` r82821 ·
  `AmbilDataTbmuserBanbox.java` r82832 (constructor non-standar flag `hasDisplayed`) ·
  `AmbilDataUploadLogBanbox.java` r82832 ·
  `AmbilJadwalSeminarTugasAkhirBanbox.java` r82834 (CRUD inline di dalam picker, gated hak
  akses) ·
  `AmbilJadwalSidangTugasAkhirBanbox.java` r82841 (padanan sidang).

  *obe/koperasi/kpi/kursus/helper (Batch C, 5 file)*:
  `helper/obe/AmbilDataCapaianLulusanBanbox.java` r82824 ·
  `helper/obe/AmbilDataCapaianPembelajaranLulusanBanbox.java` r82832 ·
  `koperasi/helper/AmbilDataAnggotaKoperasiBanbox.java` r82834 ·
  `kpi/helper/AmbilDataPegawaiFormatKPIBanbox.java` r82838 (root query `FormatKpiDetail`
  diproyeksikan ke `Pegawai`, filter otorisasi `usernamePenggunaRealisasi`, scoping satuan
  kerja via `SekolahUtil` + nested picker `AmbilDataSatuanKerjaBanbox`) ·
  `kursus/helper/AmbilDataKomponenDataProdukKursusBanbox.java` r82841.

  *kursus/library/helper (Batch D, 5 file)*:
  `kursus/helper/AmbilDataKomponenProdukKursusBanbox.java` r82826 ·
  `kursus/helper/AmbilDataPesertaKursusBanbox.java` r82829 ·
  `kursus/helper/AmbilDataProdukKursusBanbox.java` r82832 (renderer hitung ulang total harga
  dari JSON `hargaKomponens`, auto-save bila beda) ·
  `library/helper/AmbilDataDdcItemBanbox.java` r82834 (Tree `DdcItemTreeModel` lazy-load,
  parameter `chooseAll`, tab kedua "Sering Dipakai") ·
  `library/helper/AmbilDataDdcItemBanboxCampuran.java` r82836 (**DUPLIKAT fungsional 100%**
  dari `AmbilDataDdcItemBanbox` — hanya beda nama kelas; Javadoc menaut ke file itu, bukan
  mengulang).

  Beberapa file (`AmbilDataJurusanBanbox`, `AmbilDataKelasPertemuanBanbox`,
  `AmbilDataKonfigurasiBanbox`, `AmbilDataKotaKabupatenBanbox`, `AmbilDataNegaraBanbox`)
  mendeklarasikan field `AmbilDataPagingHelper pagingHelper` yang TIDAK dipakai (masih paging
  client-side lama) — dicatat eksplisit di Javadoc, bukan bug baru.

## Catatan sesi

### 1 Sep 2026 (sesi awal inisiatif)

- Diukur skala penuh codebase (lihat README.md).
- Dibuat tracker ini + memory `ais-inisiatif-javadoc-menyeluruh`.
- Target pertama dipilih: `ais/action/master/helper/GenericRevisiHelper.java`
  (6.319 baris, dasar 50 subclass `Revisi*Helper`) — leverage tertinggi yang
  ditemukan sejauh ini untuk pola referensi+link.
- **Insiden tersapu (contoh nyata risiko yang sudah dicatat di memory
  `ais-svn-workflow`)**: saat agent mengerjakan `GenericRevisiHelper.java`, sesi
  paralel lain melakukan `svn commit` TANPA pesan dan TANPA scope path atas seluruh
  WC-nya sendiri (r82750), yang menyapu ikut sebagian besar edit
  `GenericRevisiHelper.java` yang saat itu belum sempat dicommit, bercampur dengan
  5 file lain milik sesi itu (`KonfigurasiNewAction.java`,
  `GenericCrudDefinitionRegistry.java`, `Wa.java`, `AIGenerator.java`,
  `TestGemini.java` — BUKAN bagian dari inisiatif dokumentasi ini). Isi yang tersapu
  diverifikasi UTUH dan BENAR (dibaca ulang + kompilasi javac 1.7 lulus) — tidak ada
  kerja yang hilang, hanya pesan commit untuk porsi itu jadi kosong/tidak deskriptif.
  Sisa edit (7 method terakhir) dicommit terpisah dengan pesan lengkap di r82752.
  Pelajaran: commit per-file secepat mungkin tetap tidak 100% menghindari sapuan
  bila sesi lain commit seluruh WC-nya di tengah proses edit panjang (satu file
  6000+ baris makan waktu ~9 menit); tidak ada mitigasi lain selain tetap disiplin
  commit path-spesifik di sisi kita sendiri dan verifikasi isi (bukan cuma status M
  hilang/tidak) setelah tiap commit orang lain yang terdeteksi.
- Dua agent paralel dikerahkan untuk menautkan 49 subclass `Revisi*Helper` ke
  referensi (batch A: 26 file `ais.action.master.helper`; batch B: 23 file
  `akunting/helper`+`asset/helper`+`sekolah/helper`+`sop/helper`+sisa
  `ais.action.master.helper`).
- **Hasil akhir sesi**: 49/50 file rampung (1 induk + 48 subclass), 2 subclass
  tertunda (lihat daftar `[belum]` di atas) karena bentrok sesi paralel — ditangani
  benar (agent revert, tidak ada kerja yang tercampur/hilang). Semua sudah
  dikompilasi ulang, di-mirror ke `java/`, dan diverifikasi byte-identik (r82808).
- **Pengingat skala** (jangan lupa di sesi berikutnya): pencapaian sesi ini adalah
  49 file dari total 7.401 file (~127rb method, ~11% terdokumentasi saat awal sesi).
  Paket `ais.action.master.helper` sendiri masih punya ~380 file lain yang belum
  disentuh. Kandidat leverage tinggi berikutnya (belum digarap, urutan sembarang):
  cari pola serupa "class generic + banyak subclass tipis" lain (grep
  `extends Generic`/`extends Abstract` di seluruh source), atau lanjutkan menyapu
  sisa file `ais.action.master.helper/*.java` satu-satu (409 file, sebagian besar
  belum ada Javadoc method sama sekali). Baca bagian atas file ini dulu sebelum
  memilih target agar tidak duplikasi kerja.

### 2 Sep 2026 (sesi kedua)

- Ditemukan pola leverage tinggi kedua: `ais.ui.util.GetEventListener` (interface 2
  method) = kontrak wajib 83 file `AmbilData*Banbox` (pola "Bandbox picker" ZK).
  Diperkaya jadi referensi (r82818), lalu 4 agent orkestrator diluncurkan paralel
  (masing-masing ~20-22 file); beberapa dari mereka mendelegasikan lagi ke sub-agent
  sendiri (pola rekursif tak terduga tapi berhasil — total ~13 agent/sub-agent aktif
  pada puncaknya). **Hasil akhir: 83/83 file rampung**, dikompilasi, dikommit per
  file, di-mirror ke `java/`, dan diverifikasi `cmp` byte-identik semua file (lihat
  daftar lengkap per file di atas).
- **Temuan penting soal skala nyata proyek** (jangan diulang audit-nya): HAMPIR
  SEMUA file (~7405/7401) sudah punya minimal satu blok `/**`, tapi banyak berupa
  TEMPLATE GENERIK dari pass otomatis SEBELUM inisiatif ini (minimal 57 file
  persis, 1946 file dengan marker `auto-audit(empty-catch)`). Pekerjaan nyata BUKAN
  "tulis dari nol" tapi mayoritas "deteksi & ganti template dangkal dengan
  penjelasan spesifik" — dari 83 file batch ini, HANYA 3 (`AmbilDataAkunBanbox`,
  `AmbilDataReimbursementBanbox`, `AmbilDataCalonSiswaBanbox`, `AmbilDataSiswaBanbox`
  — 4 sebenarnya) yang sudah punya Javadoc spesifik baik sebelumnya; sisanya semua
  template generik atau (jarang) benar-benar kosong di level method.
- **Insiden konkurensi berulang** (pola sama seperti sesi pertama, BUKAN kejadian
  baru — WC ini memang dipakai bersama beberapa sesi paralel aktif terus-menerus):
  puluhan commit sweep tanpa pesan/tanpa scope eksplisit dari sesi lain (r82832,
  r82834, r82838, r82841, r82843, r82855, r82857, r82858, dst.) menyapu banyak file
  batch ini bersama file-file TIDAK TERKAIT milik sesi lain (mis. r82832 menyapu 17
  file termasuk `GenericCrudAkademikOverrides.java`, `NewUiResolverProbe.java`).
  Setiap kali terdeteksi, isi diverifikasi benar (`svn diff -c <rev>` / `grep`
  marker) sebelum lanjut — TIDAK ADA kerja yang hilang atau rusak sepanjang sesi
  ini, hanya atribusi pesan commit yang kadang kosong/generik untuk porsi kita.
  Orkestrator juga sempat menemukan 1 file (`AmbilDataUdcItemBanbox.java`) yang
  tampak "berbeda" antara WC src dan mirror karena source WC lokal sempat tertinggal
  1 commit (r82861) di belakang HEAD — bukan korupsi, selesai dengan `svn update`
  biasa. Pelajaran: setelah "selesai" menurut semua agent, tetap jalankan `svn
  update` menyeluruh + `cmp` sebelum menganggap mirror final.
- **Masalah tooling ditemukan & diperbaiki di tengah jalan**: kompilasi verifikasi
  dengan `-d <scratch-kosong>` + `-sourcepath .` memicu javac meng-cascade
  kompilasi ULANG SELURUH codebase (6000+ file tak terkait) tiap kali dipanggil dari
  direktori scratch kosong — sangat boros waktu bila diulang per-agent. Perbaikan:
  tambahkan flag `-implicit:none` (javac tetap resolve tipe lewat sourcepath untuk
  validasi, tapi tidak ikut menulis .class untuk file selain target eksplisit).
  **Pakai `-implicit:none` sejak awal di sesi berikutnya**, jangan menunggu sampai
  agent tersendat dulu baru diperbaiki.
- **Total pencapaian sesi ini**: 84 file (`GetEventListener.java` + 83 subclass).
  Akumulasi 2 sesi: 49 (Revisi*Helper) + 84 (AmbilData*Banbox) = **133 file** dari
  total 7.401 (~1,8%). Skala tersisa masih sangat besar.
- **Kandidat leverage tinggi berikutnya** (belum digarap): cari pola generic+banyak-
  pemakai lain lewat grep (`extends Bandbox` tanpa `GetEventListener` — sisanya ~70
  file yang mungkin pola berbeda; `implements` interface lain yang dipakai luas;
  `extends Generic`/`extends Abstract` lain); atau lanjutkan sisa ~380 file di
  `ais.action.master.helper/*.java` yang tak masuk kategori manapun di atas
  (banyak berisi template generik siap-diperkaya, cek dulu dengan grep frasa
  template sebelum menganggap "belum ada Javadoc").

### 2 Sep 2026 (sesi ketiga, batch mandiri 4 file)

- 4 file berdiri sendiri di `ais/action/master/helper/` (tidak masuk pola
  referensi+link manapun di atas) diperkaya dari template generik ke Javadoc
  konkret spesifik-domain, method public lengkap didokumentasikan, dikompilasi
  (javac 1.7 `-implicit:none`, lulus), dan dikommit per file (tidak ada yang
  tersapu commit sesi lain kali ini):
  - `ProgramMahasiswaDetailAction.java` r82879 — window ZK kelola anggota
    `Program` (kelompok/program mahasiswa, BUKAN prodi/jurusan); dicatat kuirk:
    keanggotaan `Mahasiswa.program` adalah kolom teks berisi NAMA program
    (String), bukan foreign key ke entity `Program`, sehingga dua `Program`
    dengan nama sama akan tercampur anggotanya di query.
  - `ProsesKehadiranDosen.java` r82892 — window rekap kehadiran & beban SKS
    dosen dari data `Pertemuan`; dicatat kuirk: tombol "cetak laporan" pada
    tiap tab BUKAN sekadar cetak, melainkan juga menulis/menimpa entri JSON ke
    kolom `Dosen.formula` (dipakai modul lain, kemungkinan insentif/honor
    dosen) dan meng-upsert `KehadiranDosenBulanan` — efek samping DB
    tersembunyi di balik tombol yang namanya terkesan read-only.
  - `RecoveryPertemuanHelper.java` r82904 — window generik pemulihan
    `Pertemuan` dari histori audit Envers untuk 14+ jenis induk
    `VOPembelajaran` (Perkuliahan/JadwalPelajaran/Skripsi/dll); DIKONFIRMASI
    file bersih (bukan `M`) sebelum dikerjakan sesuai instruksi kewaspadaan —
    ternyata BUKAN bagian fitur e-learning `RecoveryAktivitasPembelajaranHelper`
    (paket terpisah, sesi lain) walau namanya sangat mirip; dicatat eksplisit
    di Javadoc kelas agar sesi mendatang tidak menyatukan keduanya.
  - `ResetPasswordDosenMahasiswaHelper.java` r82909 — composer ZK admin reset
    password akun dosen (`Tbmuser`) atau mahasiswa via satu input User ID/NIM;
    dicatat kuirk mencolok: password baru yang di-set SELALU SAMA PERSIS
    dengan nilai User ID/NIM yang dicari (bukan password acak terpisah), dan
    nilai itu bahkan dicetak balik ke `System.out` sebagai "konfirmasi".
- Ditemukan & didokumentasikan **penyebab kegagalan `Edit` string-match
  berulang** saat menyunting blok Javadoc panjang: tool `Read` kadang
  menampilkan wrapping baris yang TIDAK identik dengan newline fisik asli file
  (terlihat dari posisi word-wrap yang bergeser antar pemanggilan `Read` pada
  konten yang sama, tanpa file berubah). Mengandalkan `old_string` hasil
  salin-tempel dari tampilan `Read` untuk blok multi-baris panjang bisa gagal
  match. **Mitigasi yang terbukti berhasil**: sebelum `Edit` pada blok
  multi-baris, ambil teks acuan lewat `sed -n '<start>,<end>p' <file>`
  (Bash) yang mencerminkan newline fisik sebenarnya, baru pakai itu sebagai
  `old_string`. Tidak ditemukan masalah serupa untuk `old_string` satu baris.

### 2 Sep 2026 (batch 4 file `Kelompok*DetailAction`/`GrupKuosionerUmumDetailAction`)

- [lengkap] `GrupKuosionerUmumDetailAction.java` r82866 (tersapu bersama 2 file tak
  terkait sesi lain, `FormatPenilaianHelper.java` + `HistoryStatusMahasiswaUtil.java`,
  di commit tanpa pesan — isi diverifikasi benar via `svn diff -c 82866`) — baris
  detail ZK untuk grid `GrupKuesionerUmum`, tombol upload Excel-nya memakai
  `Common.uploadData` generik (BUKAN method kustom `uploadDataMahasiswa` seperti 3
  file sekeluarga lain di batch ini).
- [lengkap] `KelompokMahasiswaDetailAction.java` r82877.
- [lengkap] `KelompokStatusKeluarMahasiswaDetailAction.java` r82890 — file paling
  kompleks di batch: 9 kolom dokumen kelulusan/keluar berbagi 1 `onChange` listener
  (simpan gabungan bukan per-field), toolbar cetak massal ijazah/transkrip PDF
  (gabung via PDFBox `PDFMergerUtility`), `uploadDataMahasiswa` reload entity lewat
  `session.get()` sebelum simpan (hindari entity detached lintas-thread).
- [lengkap] `KelompokStatusMahasiswaDetailAction.java` r82902 — struktur identik
  `KelompokMahasiswaDetailAction` (nama field/method/kolom grid sama persis, beda
  entity saja); dicatat kuirk `uploadDataMahasiswa`-nya memakai
  `HibernateUtil.currentNativeSession()` (bukan `openSession()` eksplisit seperti 2
  file lain) + try/finally berlapis dengan `closeSession()` dipanggil dua kali —
  redundan tapi tidak berbahaya, dibiarkan apa adanya.
- **Konfirmasi pola kekerabatan**: keempat file SEMUANYA `extends MyDetail` (baris
  detail ZK lazy-load, `org.zkoss.zul.Detail`) dan mengikuti kerangka identik
  (constructor+listener `onOpen`, inner `*Renderer extends MyRowRenderer`,
  `loadData(Object)`, `display()`, `initCriteria(boolean)` via `DataCriteria` —
  kecuali `GrupKuosionerUmumDetailAction` yang implements `DataSearchDefault` bukan
  `DataCriteria`). Bukan pewarisan lewat superclass generic (semua langsung `extends
  MyDetail`), jadi pola leverage-nya bukan "1 referensi + link" seperti
  `GenericRevisiHelper`/`GetEventListener`, melainkan kemiripan struktural antar
  subclass sejenis — didokumentasikan penuh di tiap file (bukan ditaut ke satu
  referensi) karena `MyDetail` sendiri terlalu tipis (cuma alias `Detail` ZK) untuk
  jadi target referensi bermakna.
- Semua 4 file sudah punya Javadoc SEBELUMNYA (hasil pass 1 Sep 2026, revisi
  r81708/r81715/r81716/r81717 + r79995/r80000/r80001/r80002) tapi berupa template
  generik ("Kelas ini memberi nama dan batas tanggung jawab...", dst) — diperkaya
  sesuai instruksi (tidak dihapus, diganti jadi penjelasan domain konkret + Javadoc
  method lengkap untuk semua method yang sebelumnya belum punya). Tidak ada file
  yang di-skip; tidak ada insiden konkurensi selain sweep r82866 di atas (isi
  terverifikasi utuh).
- Semua 4 file dikompilasi (`javac -source 1.7 -target 1.7 -implicit:none`, lulus
  tanpa error) sebelum commit; CRLF diverifikasi (`grep -cU`/`wc -l` sama) tiap
  file. Belum di-mirror ke `java/` sesi ini (mirror menyusul).

### 2 Sep 2026 (batch 5 file berdiri sendiri di `ais/action/master/helper/`)

- [lengkap] `HistoryStatusMahasiswaUtil.java` r82869 — utilitas status
  kemahasiswaan per semester (entity `HistoryStatusMahasiswa`): caching dua
  lapis (RAM + JSON temporary), mesin aturan status (lambat bayar, syarat-aktif
  kegiatan dua arah, status terminal retroaktif, paksa aktif admin). Kuirk
  dicatat: parameter `tx` tak terpakai di `updateViaTransactionQuietly`/
  `prosesNonAktifkanStatusSingkronisasi` (tiap panggilan selalu buka transaksi
  baru sendiri, bukan bug berdampak, hanya kode membingungkan).
- [lengkap] `KegiatanHelper.java` r82895 (3677 baris) — inti domain `Kegiatan`
  (tagihan mahasiswa/calon mahasiswa): dua sisi Mahasiswa/BiodataCalonMahasiswa,
  lapisan ketahanan transaksi PostgreSQL (`saveEntitySafe`/`updateEntitySafe`/
  `executeUpdateSafe` menangani lock timeout 55P03/57014, deadlock 40P01,
  transaksi ter-abort 25P02, constraint violation 23505/23503, retry+backoff+
  jitter di sesi terisolasi), ekspor/impor Excel tagihan massal. Kuirk dicatat:
  field publik `prosestagihan` — SEMUA titik pakainya di codebase
  (`KegiatanAction`, `KegiatanProsesHeper`, `TagihanProcessor`) sudah
  dikomentari, efektif dead code.
- [lengkap] `KegiatanKemahasiswaanPunyaMahasiswaHelper.java` r82906 (file
  LF-only, bukan CRLF — dikonfirmasi sudah begitu di pristine sebelum
  disentuh via `svn cat`, bukan hasil edit sesi ini) — layar peserta
  organisasi/UKM kemahasiswaan, alur persetujuan peserta, integrasi ekspor
  repository DSpace/OJS berhierarki community(jurusan) → collection(kegiatan)
  → item(peserta).
- [lengkap] `KegiatanProsesHeper.java` r82910 (2593 baris; nama file "Heper"
  bukan "Helper" — TYPO HISTORIS pada nama class publik, dikonfirmasi BUKAN
  salah ketik dokumentasi sesi ini, dipertahankan apa adanya) — fitur "Proses
  Tagihan"/"Proses Surat Tagihan"/"Sinkronisasi Data Cicilan" massal, paralel
  via `WORKER_EXECUTOR` bersama (pool kecil, daemon) untuk mencegah ledakan
  thread saat banyak admin memicu proses berat bersamaan.
- [lengkap] `NamaTugasKelompokHelper.java` r82914 — layar daftar kelompok
  tugas kuliah (perkuliahan/KKN/PKL), alur gabung-kelompok mandiri oleh
  mahasiswa, dua mode ekspor/impor Excel (standar vs OBE — nilai per
  `FormatNilai` disimpan sebagai JSON di `TugasKelompok.keteranganNilai`,
  bukan baris terpisah). Bug historis yang sudah diperbaiki dicatat di
  Javadoc: importer OBE lama tidak pernah membuat baris keanggotaan kelompok
  (jumlah peserta tampil 0 walau upload "berhasil").

  Kelima file SUDAH punya Javadoc dari pass otomatis sebelumnya (template
  generik "Kelas ini memberi nama dan batas tanggung jawab..." atau serupa)
  — diperkaya jadi penjelasan konkret per instruksi pengguna, bukan ditulis
  dari nol. Semua dikompilasi (javac 1.7, `-implicit:none`, lulus) dan
  dikommit PER FILE segera setelah lulus kompilasi; tidak ada insiden sweep
  sesi paralel terdeteksi pada batch ini, tidak ada file yang di-skip.

### 2 Sep 2026 — konsolidasi akhir batch "53 file berdiri sendiri"

**Semua 53/53 file rampung** (rincian per file ada di 6+ sub-bagian di atas,
ditulis langsung oleh masing-masing agent pelaksana): file-file ini TIDAK
punya pola referensi+link seperti 2 batch sebelumnya — masing-masing action/
helper/window/panel berdiri sendiri, jadi didokumentasikan penuh mandiri.
Orkestrator sudah menjalankan `svn update` menyeluruh, verifikasi 53/53 file
bersih, **mirror ke `java/` (r82916, verifikasi `cmp` byte-identik semua
53 file)**.

**Total akumulasi 3 batch sesi ini + sesi lalu: 49 (`Revisi*Helper`) + 84
(`GetEventListener`+`AmbilData*Banbox`) + 53 (mandiri) = 186 file dari 7.401
(~2,5%).**

**Pelajaran proses tambahan (penting untuk sesi berikutnya):**
- **Edit tool bisa diam-diam mengonversi SELURUH file dari CRLF ke LF** saat
  edit pertama pada file itu (bukan cuma baris yang diubah) — ditemukan di
  `KrsDanSkripsiHelper.java`, `MainHelper.java`, dan 59 baris di
  `PertemuanPunyaUjianHelper.java`. Perbaikan: `perl -pi -e 's/\r?\n/\r\n/g'
  <file>` SEBELUM commit, dan JANGAN asumsikan "pasti CRLF" dari kebiasaan —
  selalu cek ulang `grep -cU $'\r$' <file>` vs `wc -l <file>` SETELAH edit,
  bukan cuma sebelum.
- **Sebagian file di repo ini pure-LF secara sah dari awal** (bukan bug edit):
  `DaftarUlangPembayaranHelper.java`, `DetailPAHelper.java`,
  `KegiatanKemahasiswaanPunyaMahasiswaHelper.java` dikonfirmasi lewat
  `svn cat` pristine sudah LF sebelum disentuh — jangan paksa jadi CRLF.
- **Rekursi delegasi antar-agent bisa 3-4 tingkat** (orkestrator → agent
  batch → sub-agent per beberapa file → kadang sub-sub-agent per 1 file) —
  normal dan biasanya tetap tuntas, tapi verifikasi progres nyata SELALU
  lewat `svn log`/`svn status` per file, bukan cuma percaya laporan teks.
- **2 temuan kerentanan keamanan nyata** (didokumentasikan di Javadoc,
  DIESKALASI ke task terpisah `task_99c9a86a` untuk perbaikan, BUKAN
  bagian dari inisiatif dokumentasi ini): `ResetPasswordGuruSiswaHelper.java`
  (r82900) dan `ResetPasswordDosenMahasiswaHelper.java` (r82909)
  sama-sama mengeset password baru = User ID/NIS/NIM pengguna itu sendiri
  (predictable), ditampilkan/dicetak plaintext ke admin.
- **Interface framework AIS yang sangat luas dipakai** (ditemukan tapi BELUM
  digarap — kandidat sesi berikutnya, TAPI beda karakter dari
  `GenericRevisiHelper`/`GetEventListener`): `ais.ui.util.DataCriteria`
  (602 implementer), `DataSearchDefault` (494), `DataInitDefault` (327),
  `ais.common.listener.DataLoader` (125) — SEMUA sudah punya Javadoc
  interface yang BAIK (bukan template generik, tidak perlu disentuh lagi).
  BEDA dari pola sebelumnya: implementer-nya adalah Action/screen class BESAR
  dengan logika substantif unik per file (bukan subclass tipis) — jadi
  "link ke referensi" saja TIDAK cukup, tetap perlu dokumentasi penuh per
  file. Nilai dari fakta ini: saat mendokumentasikan Action manapun yang
  implements salah satu interface ini, boleh `@see`/`{@link}` balik ke
  interface itu untuk kontrak umum method `initCriteria`/`onSearchDefault`/
  `init`, TAPI logika query/bisnis spesifiknya tetap wajib dijelaskan detail
  sendiri.

**Kandidat lanjutan sesi berikutnya**: lanjutkan menyapu sisa file
`ais.action.master.helper/*.java` yang belum kategori manapun (masih ada
banyak di luar 53 file batch ini — total paket ini ~409 file), ATAU mulai
modul lain sepenuhnya (`ais.database.model/*` — 483 file POJO Hibernate,
kemungkinan besar banyak template generik & getter/setter, cek dulu skala
kerja per file sebelum estimasi), ATAU jelajahi package `ais.action.master.*`
lain yang belum tersentuh sama sekali.

## ais/database/model/

- [lengkap] `BiodataCalonMahasiswa.java` — entity PENDAFTAR PMB (sebelum jadi
  `Mahasiswa` resmi). **388/388 method (100%)**, 3799 → 6792 baris.
  r82997/83005/83011/83021/83025/83028 + sisa chunk terakhir tersapu ke revisi
  gabungan sesi paralel r83033/83035/83037/83041 (pesan kosong — verifikasi via
  `svn diff -c`, `svn diff -r HEAD` bersih). Kompilasi Java 7 `-implicit:none`
  lulus; EOL dinormalkan ke CRLF murni (52 baris LF sisa sesi sebelumnya ikut
  dirapikan).

  Yang didokumentasikan (rangkuman untuk sesi berikutnya):
  - **Alur PMB 6 tahap** dikonfirmasi dari kode, bukan asumsi: pendaftaran
    (`FormBiodataCalonMahasiswaAction`/`UploadBiodataCalonMahasiswa`) → bayar
    registrasi (`Kegiatan` jenis `ConstantUtil.PENDAFTARAN_CALON_MAHASISWA`) →
    seleksi (jawaban DISERIALISASI ke kolom teks `parameterTambahan`, skor
    dihitung ulang tiap dibaca) → penetapan kelulusan (`prodiLulus` +
    `statusLulus`) → generate NIM (`ais.action.master.pmb.nim.NimGenerator`
    per institusi) + konversi lewat `CommonPMB.saveMahasiswa` → daftar ulang.
  - **Relasi ke `Mahasiswa`: DISALIN *dan* DITAUTKAN.** `CommonPMB.saveMahasiswa`
    menyalin nilai ke `Mahasiswa` + `BiodataMahasiswa` (perubahan biodata calon
    SETELAH konversi tidak merambat), sekaligus menautkan dua arah
    (`calon.mahasiswa` ↔ `Mahasiswa.biodata_calon_mahasiswa_long`, UNIQUE).
    Sesudah tertaut, `getNim()`/`getNama()`/`getProdiLulus()`/`getTanggalMasuk()`
    berbalik jadi CERMIN mahasiswa resmi.
  - **Getter di kelas ini TIDAK murni** — memutasi field, menarik nilai dari
    entity lain, bahkan MEMBUKA session Hibernate & MENULIS baris master baru
    (`getPropinsiCalon` → `findOrCreatePropinsi`). Karena sekaligus properti
    Hibernate, hasil turunannya IKUT DITULIS ke kolom saat flush. Session khusus
    (`openSession`, bukan thread-local) wajib di situ karena getter dieksekusi di
    tengah INSERT entity lain.
  - **Pola "fallback alumni"**: puluhan getter biodata mengisi field kosong dari
    `mahasiswaAlumni.ambilBiodata()`. Bedakan tiga relasi `Mahasiswa` di kelas ini:
    `mahasiswa` (diri sendiri setelah konversi), `mahasiswaAlumni` (diri sendiri
    sebagai alumni kampus yang sama), `afiliasiMahasiswa` (ORANG LAIN dasar afiliasi).
  - **Pola "nilai saat diterima mengalahkan nilai saat mendaftar"** pada tiga
    pasangan: `gelombangPendaftaran`/`gelombangPendaftaranDiterima`,
    `jenisSeleksi`/`jenisSeleksiDipilih`, `statusAwalMahasiswa`/`statusAwalDiterima`.
  - **Format serialisasi `parameterTambahan`** (baris `\n`, ruas `<=>`:
    label/nilai/url/nomorUrut/idParameter/idKelompok/keterangan; label =
    `namaKelompok->labelInputan`), plus kembarannya `parameterTambahanInds`
    berbasis id agar tahan ganti nama.

  Temuan (DICATAT di Javadoc, TIDAK diperbaiki — perlu task terpisah):
  - **BUG `getKelurahanCalon()`**: cabang fallback alumni MENGUJI
    `ambilBiodata().getKelurahan()` tetapi MENGAMBIL `getNoIdentitas()`, sehingga
    kolom kelurahan bisa terisi NOMOR IDENTITAS dan nilai salah itu ikut tersimpan
    saat flush. Butuh pembersihan data lama juga.
  - `getRincianSkor()` tidak memakai `ekstrakSkorDariTeks` untuk nilai non-angka
    (langsung 0) sedangkan `getTotalSkor()` memakainya → jumlah rincian bisa
    BERBEDA dari total (mis. jawaban "1. 450 Watt": total 1, rincian 0).
  - `getStatusLulus()` memaksa 0 saat `mundur`/`ditolak` TANPA menulis balik ke
    kolom → kolom mentah bisa tetap 1. Jangan baca kolom langsung.
  - Javadoc `ekstrakSkorDariTeks` sebelumnya salah tempat (di depan konstanta
    `POLA_ANGKA`) → dipindahkan agar melekat ke method; isi tidak diubah.
  - Ternary pada `setDisposisiSop` efektif mati (kondisinya sudah dijamin tidak
    mungkin oleh guard di baris sebelumnya).

- [referensi] `GeneralValueObject.java` — **target leverage TERTINGGI sejauh ini**:
  kelas dasar **1.456 file** (`grep -rl "extends GeneralValueObject"`), mencakup
  hampir seluruh entity Hibernate AIS (Mahasiswa, Dosen, Perkuliahan, Tagihan,
  Siswa, Guru, Tbmuser, Tbmrole) plus beberapa value object non-tabel. Javadoc
  class-level r81331 (gaya template "Batas tanggung jawab/Efek samping")
  DIPERKAYA, bukan diganti; SELURUH method public/protected/private kini punya
  Javadoc sendiri, termasuk 4 override `onEvent` pada listener anonim di
  `tampilKunci`. r82933, kompilasi Java 7 `-implicit:none` lulus, di-mirror ke
  `java/` (verifikasi `cmp` byte-identik). File 1807 → 3412 baris.

  Mekanisme yang didokumentasikan (rangkuman untuk sesi berikutnya):
  - **`check(T)` / `chek(T)` / `resolveLazy(T)`** — method paling kritis, dipanggil
    dari ribuan getter relasi (827 file di `ais.database.model` saja memuat
    `check(`). Pola getter standar: `jurusan = check(jurusan); return this.jurusan;`
    — hasilnya DITUGASKAN KEMBALI ke field karena bisa berupa instance LAIN.
    Urutan resolusi: jalan pintas flag `initData` → (0) `EntityIdentityMap`
    canonical per-JVM → (1) cache `ConstantValues.ambil(kelas, id, false)` tanpa
    fallback DB → (2) `Hibernate.initialize` via session aktif → (3)
    `reloadDetachedObject` yang membuka `openSession()` sendiri & menutupnya di
    `finally`. Tidak pernah melempar exception; kegagalan SENYAP (mengembalikan
    argumen apa adanya). `chek` = ejaan historis, `resolveLazy` = alias
    deskriptif — ketiganya identik.
  - **`hashCode()` TIDAK di-override** (baik di sini maupun di `ais.common.DataUtil`)
    padahal `equals()` berbasis `id` → entity TIDAK aman jadi elemen `HashSet`
    atau kunci `HashMap`. Pola benar di codebase ini: `Map<Long, Entity>`
    berkunci `id`. Temuan baru sesi ini; catat bila menemui bug deduplikasi.
  - `equals` juga tidak memeriksa kelas → `Mahasiswa#5`.equals(`Dosen#5`) = true.
  - `compareTo` berjenjang: `nomorUrut` → `nim` → `nama` → `keterangan`; cabang
    `keterangan` praktis SELALU terpakai karena `getKeterangan()` mengembalikan
    `""`, bukan `null`. Tidak konsisten dengan `equals` → hindari `TreeSet`.
  - **Cache JSON/berkas sementara** (`read`/`write`/`delete`/`put`/`retreive`/
    `udah`/`belum`/`putBaru`/`tulisPutBaru`/`retreiveAll`, kelompok
    `*ChecklistHasilPenilaianUmum*` & `*IsiAngketParameterUmum*`) — SEMUANYA
    berkas temp, BUKAN tabel. `delete()` menghapus BERKAS, bukan baris DB.
    Kunci berkas: `Tbmuser` → `userId`, `Tbmrole` → `roleId`, lainnya → `getId()`.
    `udah(String)` bergaya test-and-set: `false` = "belum, dan sekarang ditandai".
  - **Thread-safety `datatemporary`**: komentar blok hasil debugging nyata
    (ConcurrentModificationException + berkas JSON terpotong "Unterminated
    string") DIPERTAHANKAN UTUH, dipindah jadi Javadoc field tersebut.
    `ConcurrentHashMap` + `synchronized(key.intern())`; penanda "sudah ditulis"
    pakai `remove(key)` karena CHM tolak value null.
  - **Penjaga startup** `AppStartupListener.isStartupInProgress()` di `put()` dan
    `retreive()`: Hibernate memanggil setter ter-map saat hidrasi
    (`TwoPhaseLoad.initializeEntity`) → memuat ribuan entity = ribuan I/O berkas
    = startup macet di thread "main". Komentar aslinya tetap di tempatnya.

  Ketidakpastian yang dicatat jujur di Javadoc (jangan diklaim pasti tanpa
  verifikasi ulang): cabang `Guru` di `masukkanData(String, Tbmuser)` memeriksa
  `tbmuser.getSiswa()` (tampak keliru, cabang tak pernah tercapai);
  `retreiveBaru()` & `bersihkanPutBaru()` tanpa pemanggil aktif di pohon sumber;
  `retreiveCacheMap` `transient` tapi tidak `volatile` pada double-checked
  locking `retreiveCache()`.

- [belum] **Batch penautan 1.456 subclass ke referensi di atas SENGAJA TIDAK
  dikerjakan** — di luar scope sesi ini, jadikan proyek terpisah. Saat
  mendokumentasikan entity turunan mana pun, cukup `{@link GeneralValueObject}`
  untuk kontrak umum (`check`, cache berkas, `equals`/`compareTo`) dan fokuskan
  tulisan pada relasi & logika domain khas entity itu.

- [lengkap] `Perkuliahan.java` — entity **kelas kuliah** (satu penawaran konkret
  sebuah mata kuliah pada satu tahun akademik + semester + kelas), unit inti
  penjadwalan/KRS/presensi/penilaian/Feeder. 3537 → 6346 baris; **SELURUH 300
  method** (296 satu baris + 4 tanda tangan multi-baris) kini ber-Javadoc, dari 4
  blok sebelumnya. Javadoc class-level placeholder "generated by hbm2java"
  diganti uraian domain penuh. r82926, r82932, r82941, r82945; kompilasi Java 7
  `-implicit:none` lulus tiap commit; di-mirror ke `java/` (verifikasi `cmp`
  byte-identik). Tidak ada perubahan logika.

  Temuan yang didokumentasikan (rangkuman untuk sesi berikutnya):
  - **Perkuliahan vs Matakuliah** (dikonfirmasi dari kode, bukan asumsi):
    `Matakuliah` = definisi generik kurikulum (kode/nama/SKS, tak terikat waktu);
    `Perkuliahan` = instansiasinya per periode. SKS TIDAK disimpan di
    `Perkuliahan`, selalu dibaca `getMatakuliah().getSks()`.
  - **Pola 10 slot dosen TERKONFIRMASI**: `dosen1`..`dosen10` sebagai 10 kolom
    `@ManyToOne` terpisah + 10 kolom pendamping `feeder1`..`feeder10`.
    `getJumlahDosen()` BUKAN cacah dosen melainkan panjang rantai slot BERURUTAN
    (dosen1 kosong + dosen2 terisi → hasil 0), dan getter slot ke-n menolkan
    dirinya bila jumlah < n. Konsekuensi query: `checkMaksSksDosen` menyusun `OR`
    atas kesepuluh kolom karena tak ada tabel penghubung untuk di-join. Iterasi
    yang benar lewat `populateDosen()`/`populateDosenBuNama()`/`populateDosenBuId()`
    di `VOPembelajaran`, bukan 10 cabang `if` baru.
  - **Kelas paralel** (`perkuliahan_paralel`, rujuk-diri): begitu terisi, hampir
    semua pembacaan peserta/format nilai/lokasi berkas di-short-circuit ke induk.
    Efek sampingnya kelas paralel tak pernah menyimpan `jumlah_mahasiswa` lewat
    jalur biasa — langkah (5) `singkronkan()` menambal ini secara eksplisit.
  - **Flag store JSON** 4 macam: `detail_perkuliahan_<id>` (peserta),
    `MahasiswaJadiAsisten_<id>`, `perkuliahan_punya_format_nilai_<id>`,
    `paralel_<id>`. Hanya flag store peserta yang dilindungi lock ber-strip 257
    objek; jalur asisten/format nilai/paralel tidak. Penghapusan entri =
    mengosongkan nilainya, bukan menghapus kunci.
  - **Kunci flag salah ketik `"detailperkulaiahan"`** — HARUS dipertahankan
    persis, sudah tertulis pada data yang ada.
  - **`FlushMode.MANUAL`** pada query peserta bukan optimasi melainkan pencegah
    rekursi: method dapat terpanggil dari getter property Hibernate saat flush →
    autoFlush → getter lagi → StackOverflow.
  - **Penyembuhan otomatis**: `ambilDetailperkuliahan(...)` memanggil dirinya
    SEKALI dengan `refresh=true, simpanJmlMhs=true` bila hasil kosong padahal
    kolom `jumlah_mahasiswa` > 0; `ambilFormatNilai(...)` mencoba ulang sekali
    bila total bobot < 99.0 (parameter `coba` yang menghentikan rekursi).
  - **Banyak getter BUKAN pembaca murni** — menulis balik ke field, membuka
    session, membaca konfigurasi (yang bisa menulis default ke DB), bahkan
    menyimpan ke DB (`ambilKurikulumPunyaMatakuliah()`). `getKurikulumPunyaMatakuliah()`
    diam-diam menambah id dosen ke baris kurikulum. `getDikunci()` membatalkan
    kunci bila penguncinya bukan lagi pengampu.
  - **Setter yang menolak nilai kosong** (`setOleh`, `setOlehId`,
    `setPembombotanNilai`) mengabaikan argumen null/kosong diam-diam.
  - Kuirk yang dicatat jujur: parameter `Dosen` pada `ambilParalel(Dosen)` TIDAK
    berpengaruh (kedua cabang if/else menambahkan hal sama); parameter
    `tulisUlang` pada `populateFormatNilai` tidak dipakai;
    `ambilKurikulumPunyaMatakuliah()` menulis blok pencarian ke-3 dan ke-4 secara
    identik; `getDosenBolehVerifikasiNilaiSendiri()` membandingkan konfigurasi
    dengan `TIDAK_AKTIF` sehingga hasilnya berkebalikan dari pembacaan naif;
    `semesterPerkuliahan` kolom mati; `getTerdapatKegiatanPraktek()` memeriksa
    FIELD `matakuliah` bukan getter, jadi pemaksaan `true` bisa terlewat.
  - EOL file diseragamkan ke CRLF pada commit pertama (89 baris sebelumnya
    LF-only di tengah file murni-CRLF).

- [lengkap] `Dosen.java` — entity **dosen/tenaga pendidik** (tabel `public.dosen`,
  subclass konkret `Karyawan` → `GeneralValueObject`, mengimplementasikan
  `VOMahasiswaDosen`), titik masuk hampir semua fitur "sisi dosen": pengampuan
  perkuliahan, pertemuan, bimbingan TA, KKN/PKL, perwalian KRS, kegiatan
  kedosenan, penelitian & pengabdian, artikel, buku bahan ajar, prestasi,
  penghargaan. 3742 → 5744 baris; **SELURUH 249 deklarasi method** kini
  ber-Javadoc (dari 6 blok sebelumnya), ditambah konstanta/field statis. Javadoc
  class-level placeholder "generated by hbm2java" diganti uraian domain penuh.
  r82927, r82929, r82935 (tersapu), r82939 (tersapu), r82946, r82951 (tersapu),
  r82953; kompilasi Java 7 `-implicit:none` lulus tiap commit; di-mirror ke
  `java/` (verifikasi `cmp` byte-identik). Tidak ada perubahan logika.

  Temuan yang didokumentasikan (rangkuman untuk sesi berikutnya):
  - **Pola indeks berkas JSON per dosen** adalah struktur dominan file ini.
    Relasi "1 dosen → banyak X" TIDAK dipetakan sebagai koleksi Hibernate;
    tiap domain punya berkas `<nama>_<idDosen>` berisi peta `"id" -> "id"`
    (nilai kosong = dihapus) dan enam method senama:
    `ambilLokasiX` / `tulisLokasiX` / `populateX` / `removeX` / `reInitX` /
    `ambilX`, dengan penanda sekali-jalan `udah("X")` yang memicu `reInitX`.
    Dipakai 9 kali: perkuliahan, pertemuan, kegiatan kedosenan, organisasi
    dosen, prestasi, penghargaan, pengajuan penelitian & pengabdian, artikel,
    buku bahan ajar. Menambah/menghapus relasi di tempat lain WAJIB disertai
    `populateX`/`removeX` (lihat `AuditListener`) atau indeks jadi basi.
  - **Perbedaan halus antar blok indeks** yang mudah terlewat: `reInitArtikel`
    dan `reInitPengajuanPenelitianDanPengabdian` menyaring lewat alias
    `tbmuser.dosen` (data melekat pada akun, bukan baris dosen);
    `reInitBukuBahanAjar` memakai rangkaian `OR` atas 5 slot `dosenPengarang`;
    `reInitPerkuliahan` atas 10 slot `dosen1..dosen10`; hanya `ambilArtikel`
    yang membungkus pembangunan ulang dengan `try/catch/finally` dan menutup
    session tuntas. Setiap `populate*` juga memanggil `write()` pada objek
    terkait (menulis berkas cache objek, bukan hanya indeks).
  - **Kunci sinkronisasi `KUNCI_PERTEMUAN_DOSEN`** — `ConcurrentHashMap` statis
    ber-kunci ID dosen, karena satu dosen dapat punya BANYAK instance Hibernate
    sehingga `synchronized(this)` tidak cukup. Hanya jalur pertemuan yang
    dilindungi; jalur perkuliahan dan portofolio lain TIDAK.
  - **Pemulihan JSON terpotong**: `ambilLokasiPertemuanJsonAman()` memindai
    berkas rusak dengan regex pasangan `"<angka>": "<angka>"`, menyelamatkan
    pasangan utuh, dan menulis balik hasil pemulihan. `removePerkuliahan` punya
    penyembuhan serupa (reset ke objek kosong, dulu NPE di `c.put`).
  - **Anti-rekursi `ThreadLocal AMBIL_BIODATA_AKTIF`** pada `ambilBiodata`:
    `getAgama()` → `ambilBiodata()` → query → auto-flush Hibernate →
    `getAgama()` lagi → StackOverflowError. Selain itu `ambilBiodata(true)`
    (dan `ambilBiodata()` tanpa argumen) diam-diam **MENYIMPAN baris
    `BiodataDosen` baru** — operasi tulis di balik nama "ambil"; pemanggil yang
    hanya membaca wajib memakai `ambilBiodata(false)`.
  - **Banyak getter BUKAN pembaca murni** — menormalkan lalu menulis balik ke
    field (`getNama`, `getEmail`, `getKelamin`), menurunkan nilai dari relasi
    lain (`getFakultas` dari jurusan, `getTetap` dari `ikatanKerjaDosen`,
    `getGolongan` dari `golonganPns`/`golonganPegawai`), menjatuhkan ke master
    `ConstantValues` (`getStatusPegawai` → `AKTIF_PEGAWAI`, `getIkatanKerjaDosen`
    → `DOSEN_TETAP`/`DOSEN_HONORER`, `getStatusKewajibanBebanDosen` →
    `DOSEN_BIASA`), atau menelusuri master untuk mencocokkan data warisan
    (`getGolonganPns` mencocokkan kode dengan nama `golonganPegawai`).
    `getPerguruanTinggi()` punya rantai fallback 4 langkah dan bisa ditimpa oleh
    perguruan tinggi milik fakultas.
  - **Field bayangan `Karyawan`**: `code`, `mycode`, `nama`, `alamat`, `email`,
    `telp`, `kelamin`, `tempatlahir`, `jurusan`, `fakultas`, `tetap`, `idfinger`
    dideklarasikan ULANG di `Dosen` meski sudah ada di induk. Yang dipetakan
    Hibernate adalah accessor `Dosen`. Efek nyata: `Karyawan.getNama()`
    mengembalikan `code + "-" + nama`, versi `Dosen` tidak.
  - **Dua mesin besar** yang benar-benar memuat logika:
    `ambilPerkuliahanDanParalel(...)` (4 overload, 1 implementasi; bercabang per
    `TampilanELearningAction.*` — SKRIPSI/BIMBINGAN/KKN/PKL/KRS/KEGIATAN/
    KONSULTASI/PERKULIAHAN — lalu saringan seragam + potong halaman manual;
    mengembalikan `Object[]{halaman, jumlah, semuaData}`) dan
    `ambilPertemuan(TreeMap, ...)` (3 overload; penyaringan berlapis ~15 tingkat
    lalu memperbarui state komponen ZK `paging` dan tombol `back` — jadi terikat
    UI, bukan pengolah data murni).
  - **Kuirk yang dicatat jujur**: parameter `tulisUlang` pada
    `populatePerkuliahan`/`populatePertemuan` TIDAK dipakai; parameter `tanggal`
    pada `ambilPertemuan(TreeMap, ...)` tidak ikut menyaring; overload
    `reInitPertemuan(Session, Label, Calendar sampai, Calendar mulai)` menaruh
    `sampai` SEBELUM `mulai` pada tanda tangannya; `setGoogleScholar` tetap
    menimpa field dengan nilai kosong sedangkan `setIdfinger`/`setNama`/
    `setOleh`/`setOlehId` mengabaikannya; komentar pemisah `// KARYA DOSEN`
    sebenarnya menaungi blok penghargaan; `ttdQr()` men-cache PNG per ID dosen
    sehingga QR TIDAK ikut berubah bila nama/jurusan berubah; `atasanlangsung`
    disimpan sebagai `Long` mentah (bukan relasi) dan pemanggil harus memuatnya
    sendiri lewat `ConstantValues.ambil(...)`.
  - Kolom `formula` = larik JSON rekap kehadiran/SKS per periode (`key`,
    `sks`, `hdr`, `hdr_hr`) yang ditulis `ProsesKehadiranDosen`, bukan diisi
    lewat layar biodata. Kolom `aLisensiKepsek`/`jmlSekolahBinaan`/`aDiklatAwas`/
    `aktaIjinAjar` adalah warisan domain sekolah-pengawas yang menumpang.
  - EOL file diseragamkan ke CRLF pada commit pertama (154 baris sebelumnya
    LF-only di tengah file murni-CRLF).

- [lengkap] `Mahasiswa.java` — **entity paling sentral di domain akademik AIS**
  (tabel `public.mahasiswa`). Sebelum sesi ini: 6.403 baris, 445 deklarasi
  method/konstruktor, hanya 14 blok Javadoc. Sesudah: 10.087 baris, **445/445
  (100%) method punya Javadoc** + Javadoc class-level penuh menggantikan stub
  "Mahasiswa generated by hbm2java". Dikerjakan 7 commit bertahap: r82928,
  r82934, r82939 (tersapu ke revisi gabungan sesi lain — isi terverifikasi lewat
  `svn diff -c`), r82950, r82952, r82958, r82963. Kompilasi Java 7
  `-implicit:none` lulus tiap batch, di-mirror ke `java/` (`cmp` byte-identik).

  Struktur yang ditemukan (8 kelompok, dipakai sebagai kerangka Javadoc kelas):
  identitas/audit; biodata & kontak; status akademik & masa studi; KRS &
  perkuliahan; perhitungan nilai/IPK/SKS; cache berkas JSON; linimasa
  e-learning; laporan & berkas.

  **Mekanisme penting yang didokumentasikan** (rangkuman untuk sesi berikutnya):
  - **Cache berkas JSON pengganti `@OneToMany`** — relasi "satu mahasiswa punya
    banyak X" TIDAK dipetakan Hibernate. Daftar id disimpan sebagai berkas JSON
    per mahasiswa (`Common.getFileLocation`), diakses lewat pola berulang
    `ambilLokasi*` / `tulisLokasi*` / `bersihkanLokasi*` / `populate*` /
    `remove*` / `reInit*` untuk detailperkuliahan, pertemuan, checklist penilaian
    dosen, kegiatan kemahasiswaan, organisasi, prestasi, penghargaan. `remove*`
    MENGOSONGKAN nilai kunci, bukan membuang kuncinya.
  - **`dipanggilOlehHibernateFlush()`** — membaca stack trace untuk mengenali
    apakah getter sedang dipanggil mesin flush Hibernate; bila ya, pemuatan lazy
    `BiodataMahasiswa` dilewati agar tidak terjadi flush bersarang. Rapuh
    terhadap kenaikan versi Hibernate (daftar nama kelas internal di-hardcode).
  - **Perhitungan IPK sepenuhnya disetir Konfigurasi**: `nilai_0_tidak_masuk_
    dalam_perhitungan_ipk`, `nilai_minimal_tidak_masuk_dalam_perhitungan_ipk`,
    `nilai_huruf_yg_tidak_masuk_perhitungan_ip`, `nilai_belum_verifikasi_tidak_
    masuk_dalam_perhitungan_ipk`, `aktifkan_ekivalen`, `aktifkan_kesamaan_nama`,
    `aktifkan_kesamaan_kode`, `saring_nilai_ipk_juga_berdasarkan_nama`.
    `saringBerdasarNilaiDan0` (4 tahap) adalah penyaring baku; `saringBerdasarNilaiOk`
    (dipakai lewat `saringBerdasarNilai`) TIDAK membaca konfigurasi sama sekali
    sehingga hasilnya lebih longgar — dan justru itulah yang dipakai `prosesHitungSks`.
  - **Nilai kelompok menimpa nilai perorangan**: `KelompokStatusKeluarMahasiswa`
    menimpa `getStatusKeluar`, `getTanggalLulus`, `getTahunLulus`, `getSemesterLulus`
    — TAPI sengaja TIDAK menimpa `getTanggalSkRektor` dan `getNoAkta2` (kode
    lamanya masih ada sebagai komentar).

  **Kuirk yang dicatat jujur**:
  - Sangat banyak getter TIDAK MURNI: `getPass()` membangkitkan sandi awal,
    `ambilBiodata(true)` MENULIS baris `BiodataMahasiswa` baru ke DB,
    `getTanggallahir()`/`getTanggalKegiatanBelajarMengajar()` mengisi tebakan ke
    field, `getKelamin()`/`getSemesterMulai()` menormalkan teks.
  - Pembacaan ambang konfigurasi TIDAK seragam: `prosesHitungMutu` dan
    `prosesHitungIpk` sudah toleran koma (`Common.parseAngkaKonfigurasi`),
    `saringBerdasarNilaiDan0`/`alasanTidakValidDetail`/`prosesHitungSks`
    menormalkan koma→titik manual, sedangkan `prosesHitungRataRata`,
    `prosesHitungNilai`, `prosesHitungTotalIP`, `prosesHitungNilaiIp` dan
    `prosesHitungMk` masih `Double.parseDouble` polos sehingga nilai "0,1" gagal
    parse dan diam-diam jatuh ke default 0.1.
  - `setGcpToken` perilakunya MENAMBAH token, bukan menimpa. `setOleh`/`setOlehId`/
    `setIdfinger`/`setDisposisiSop` mengabaikan nilai kosong (penjaga jejak audit).
  - Parameter `tulisUlang` pada `populateDetailperkuliahan`/`populatePertemuan`
    tidak dipakai; `reInitPertemuan(Session, Label, Calendar sampai, Calendar mulai)`
    menaruh `sampai` SEBELUM `mulai` (sama seperti kuirk di `Dosen.java`).
  - `getKarpeg()` isinya daftar id `LampiranLain` dipisah koma, BUKAN nomor kartu
    pegawai. `ttdQr()` men-cache PNG per id mahasiswa sehingga QR tidak ikut
    berubah bila nama/prodi berubah (kuirk sama dengan `Dosen.java`).
  - Sandi nilai "kosong" pada data lama: `"1000"` untuk `batasStudi`/
    `paksaAktifSemester`, `"00000"` untuk KTP, `"0000000000"`/
    `"08100000000000000000"`/`"00000000000000000000"`/`"000000000"` untuk telepon,
    koma ganda pada kolom email & id media sosial, `tahunWisuda == 0`.
  - `hitungIpk()` tanpa argumen sudah TIDAK punya pemanggil di pohon sumber.
  - `ambilDetailperkuliahan(Integer)` dan `reInit()` MENUTUP session thread-local
    (`HibernateUtil.closeSession()`) setelah selesai — pemanggil harus mengambil
    session ulang sesudahnya.
  - EOL: 39 baris LF-only (peninggalan sesi lain) dikembalikan ke CRLF pada
    commit pertama (r82928).

  **Temuan di luar berkas ini** (belum ditangani, bukan cakupan sesi Javadoc):
  `ais/action/master/helper/PembayaranUtilHelper.java` dan
  `ais/action/ws/util/PembayaranUtil.java` sudah ter-commit dengan
  `import org.hibernate.CriteriaSpecification` yang TIDAK ADA di versi Hibernate
  repo ini → 6 error kompilasi saat javac menarik keduanya lewat `-sourcepath`.
