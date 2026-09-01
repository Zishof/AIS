# Progres Javadoc Menyeluruh

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

  Sisa **61 dari 83** subclass `AmbilData*Banbox` (di `ais/action/master/helper/` dan package
  modul lain seperti `akunting/helper`, `asset/helper`, `sekolah/helper`, dst.) masih `[belum]`
  disentuh — kandidat lanjutan sesi berikutnya, baca dulu Javadoc lengkap
  `ais/ui/util/GetEventListener.java` sebelum mulai.

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
