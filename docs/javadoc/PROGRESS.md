# Progres Javadoc Menyeluruh

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
