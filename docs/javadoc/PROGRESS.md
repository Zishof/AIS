# Progres Javadoc Menyeluruh

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
