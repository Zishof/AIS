# Status Implementasi Paket

## Status runtime checkout AIS — 9 Agustus 2026

Engine New UI sekarang memuat class `Action` dari `nuiSourcePackage` +
`nuiSourceClass`, memverifikasi pasangan entity/action, dan menjalankan
`init(entity)` serta `boolean onSave(Event)` existing secara headless di dalam
transaksi Hibernate yang sama. Pesan validasi ZK ditangkap sebagai error JSON;
dialog/window/background ZK tidak dirender.

Hasil `tools/audit_native_crud_source_truth.py` pada checkout ini:

- 3.892 halaman New UI mempunyai referensi source Java;
- 893 di antaranya merupakan kandidat CRUD dengan `onSave(Event)`;
- 844 rute mempunyai kontrak lifecycle existing yang dapat diverifikasi statis,
  termasuk kontrak yang diwarisi dari superclass: 75 `GenericCrudAction`,
  341 `DataInitDefault`, dan 428 pola `init(Entity)`;
- 2 rute memakai override native eksplisit, sehingga total 846 rute CRUD sudah
  mempunyai binding native;
- 47 rute custom masih memerlukan adapter domain khusus;
- 21 keluaran generator menunjuk nama inner/helper class yang tidak dapat
  di-resolve sebagai Action top-level dan tidak diaktifkan oleh runtime;
- 2.999 halaman bukan CRUD dan tidak boleh dihitung sebagai CRUD yang tertinggal.

CREATE otomatis aktif bila entity dapat dibuat dan Action mempunyai lifecycle
native yang cocok; ini mencakup `GenericCrudAction` serta Action
`DataInitDefault` yang memakai container `Window` atau `Component`. UPDATE aktif
bila entity dan `init(entity)` cocok.
DELETE otomatis tetap fail-closed karena renderer/selection/konfirmasi ZK lama
tidak mempunyai kontrak entity tunggal; hanya adapter eksplisit yang boleh
mengaktifkannya. Jadi angka 846 adalah cakupan binding CRUD native, bukan klaim
bahwa seluruh operasi khusus pada 47 Action custom sudah selesai.

Override native pertama untuk kelompok custom sudah tersedia pada
`JenjangProgramStudi`: CREATE/READ/UPDATE/DELETE memakai form metadata New UI,
validasi Jurusan/Jenjang mengikuti `JenjangProgramStudiAction`, dan transaksi
tidak lagi dibuka/ditutup dari composer ZK. Workflow header-detail seperti
transaksi SIRS tetap diklasifikasikan adapter domain khusus; workflow tersebut
tidak diturunkan menjadi CRUD satu tabel karena dapat menghilangkan detail,
stok, jadwal, cetak, atau efek bisnis lain.

Override native kedua tersedia pada `PenilaianSiswaAction`. Route
`sekolah/penilaian_siswa` sekarang dipaksa ke entity `KelasSiswa`, bukan
`KelasSiswaPunyaSiswa` yang sebelumnya salah terpilih oleh urutan kandidat.
Form New UI mempertahankan nama kelas, yayasan, sekolah, ruang, tingkat, tahun
ajaran, kurikulum, keterangan, status aktif, serta scope yayasan/sekolah.

## Yang sudah tersedia dalam paket

- Spesifikasi arsitektur lengkap Generic CRUD metadata-driven.
- Prompt eksekusi Codex/Claude Code bertahap.
- Source scanner untuk seluruh subclass `GeneralValueObject`.
- Manifest snapshot 1.501 subclass dan 1.490 concrete entity.
- Generator alias JSP UI/service yang idempotent dan tidak menimpa file manual.
- 1.543 page binding alias yang seluruhnya **disabled**.
- SQL konfigurasi, page binding, preference, saved view, custom action, import/export job, idempotency, selection token, dan audit.
- Contoh definisi Agama dan override Mahasiswa.
- Prototype UI responsif.
- Matriks test/security/performance/migration.

## Yang sengaja belum diklaim selesai

Paket ini **bukan WAR/drop-in framework yang sudah terkompilasi**. Java Generic CRUD engine, dispatcher produksi, query/mutation services, import/export workers, report templates, photo adapters, dan parity integration harus diterapkan oleh Codex/Claude Code pada Git terbaru lalu dibuild dan diuji terhadap konfigurasi AIS aktual.

Alasannya:

- snapshot tidak menjamin mapping/session/helper sama dengan Git terbaru;
- menuId, privilege, dan tenant/scope tidak dapat ditebak aman;
- 1.490 entity mempunyai risiko dan business rule yang berbeda;
- banyak entity mempunyai Action/custom button existing;
- activation otomatis dapat membuka data internal/transaksi/integrasi.

## Prinsip aktivasi

Semua seed dan alias berada pada status disabled/review-required. Target awal yang harus benar-benar diimplementasikan end-to-end adalah `Agama`, kemudian reference master lain secara bertahap. Mahasiswa, Siswa, Dosen, Guru, Pegawai, dan Tbmuser memerlukan adapter khusus.



## Tambahan V2.1

Sudah tersedia dalam paket sebagai spesifikasi/template/config/prototype:

- Audit Trail global/per-row;
- restore field, manual correction, revision/deep/mass restore;
- Super Admin active-row delete policy;
- complex form override provider;
- Mahasiswa full-page tab definition;
- SQL migration 003;
- expanded test matrix.

Belum boleh diklaim production-ready sebelum Java runtime, Envers bridge, domain adapters, DB migration staging, Ant build, parity tests, dan security tests benar-benar dijalankan pada checkout Git terbaru.

## Blocklist domain medis SIRS ditutup — 6 September 2026

Audit inisiatif Javadoc menyeluruh AIS pada klaster `ais.database.model.sirs` (Pasien,
AlergiPasien, KepesertaanPasien, dan 121 entity lain) menemukan bahwa seluruh paket ini
**tidak punya sumbu tenant apa pun** di level entity (tidak ada `satuanKerja`/`yayasan`/dst.),
sehingga `GenericCrudAutoEntityAdapter.scopeBindings()` selalu kosong untuknya — beda dari
domain lain yang setidaknya punya pembatas tenant parsial.

`BLOCKED_CLASS_TOKENS` di `GenericCrudAutoDefinitionFactory.java` sama sekali tidak punya
token domain medis, jadi `isBlockedClass()` meloloskan seluruh entity sirs sebagai `FULL_CRUD`.
Ini relevan karena `model_crud_service.jsp` (browser model admin generik) menerima `entityKey`
**mentah** dari `request.getParameter("entity")` dan hanya digerbangi `Common.getApakahAdmin()`
— cek "admin apa pun" yang kasar, bukan hak akses per menu. Temuan tambahan: `applyScope()`,
`scopeBindings()`, dan `validateObjectScope()` pada adapter yang sama semuanya langsung
`return` kosong ketika `Common.getApakahAdmin()==true`, jadi untuk jalur browser admin ini
`BLOCKED_CLASS_TOKENS` adalah satu-satunya pagar untuk SEMUA domain sensitif, bukan cuma sirs.

Jalur kedua (`pasien_service.jsp` → `dispatcher.jsp` → `tryAutoRegister`) digerbangi
sesi + `NewUiRouteGuard` per menu `sirs`/`pasien` — risikonya lebih rendah karena populasinya
sama dengan yang memang sudah berwenang membuka menu Pasien, tapi tetap membypass validasi
bisnis `PasienAction` dan sama sekali tanpa scoping tenant.

**Perbaikan:** menambahkan blok per-paket (bukan per-token nama kelas) di `isBlockedClass()`:

```java
private static final String SIRS_BLOCKED_PACKAGE_PREFIX = "ais.database.model.sirs.";
...
if (type != null && type.getName().startsWith(SIRS_BLOCKED_PACKAGE_PREFIX)) return true;
```

Seluruh entity `ais.database.model.sirs.*` — termasuk entity baru di masa depan — otomatis ikut
terlindungi tanpa bergantung pada kata kunci dalam nama kelasnya. Efeknya mendowngrade
`FULL_CRUD` menjadi `READ_ONLY` (bukan blokir baca total — itu perilaku bawaan mekanisme
blocklist ini, sama seperti kelas "bank"/"audit" yang sudah ada). Landed di r85063 (tersapu ke
commit sesi paralel lain berpesan kosong, diverifikasi `svn diff -c 85063` = persis perubahan
yang dimaksud, tidak tercampur apa pun di luar itu untuk berkas ini).

Payroll punya gap serupa (nol token blocklist, `FULL_CRUD` via `tryAutoRegister`) dan sudah
ter-track terpisah — lihat javadoc `PembayaranGaji.java`. Koperasi, asset, dan employ punya
paket sendiri dan juga nol token blocklist spesifik domain — audit granularnya ada di bagian
berikutnya, yang berujung pada perbaikan akar masalah, bukan blok per-paket seperti sirs.

Audit data historis (siapa saja yang mungkin sudah mengakses/mengubah data sirs lewat jalur
CRUD generik ini) belum dijalankan — butuh kredensial database dan log akses yang tidak
tersedia saat audit ini dilakukan.

## Audit granular koperasi/asset/employ, dan perbaikan akar masalah — 6 September 2026

Audit lanjutan menghitung token blocklist terhadap 180 entity di tiga paket ini: **56 dari 61**
entity koperasi, **61 dari 63** entity asset, dan **56 dari 56** entity employ lolos
`BLOCKED_CLASS_TOKENS` tanpa satu pun token cocok. Akar sebabnya sama seperti sirs — token
berbahasa Inggris ("payment", "bank") tidak menangkap padanan Indonesia (`Pembayaran*`,
`Piutang`, `Hutang`, `Gaji`, `Hukuman`, `Pelanggaran`, `Keluarga`) yang maknanya identik.

Beda dari sirs: penelusuran JSP scaffold (`generate_new_jsp_scaffold.py`, 6 Agu 2026, pola
sama persis dengan `pasien_service.jsp`) membuktikan entity paling sensitif di ketiga domain
ini **sudah live** lewat jalur menu (`tryAutoRegister`, digerbangi `NewUiRouteGuard`) — bukan
sekadar termapping Hibernate:

- **employ**: `hukuman_pegawai_service.jsp`, `pelanggaran_dan_hukuman_pegawai_service.jsp`
  (catatan disiplin pegawai), `keluarga_service.jsp` (data keluarga/tanggungan),
  `riwayat_kartu_identitas_pegawai_service.jsp`, `riwayat_keluar_negeri_pegawai_service.jsp`,
  `gaji_pokok_service.jsp`, `kenaikan_gaji_berkala_service.jsp` (gaji).
- **koperasi**: `anggota_koperasi_service.jsp` (identitas anggota), `pembayaran_anggota_koperasi_service.jsp`,
  `pencairan_diskon_service.jsp`.
- **asset**: `master_asset_service.jsp`, `penyedia_asset_service.jsp`, dan tiga helper
  `pembayaran_{dp,pengadaan,termin}_master_asset_helper_service.jsp` (pembayaran pengadaan/vendor).

Karena `isBlockedClass()` (dan sebelumnya `SIRS_BLOCKED_PACKAGE_PREFIX`) dipakai bersama oleh
DUA jalur — `tryAutoRegister` (menu-gated, `scopeBindings` parsial aktif utk staf biasa) dan
`buildAdministrative`/`model_crud_service.jsp` (admin-flag kasar, scope selalu bypass) —
menambah token/blok paket di sana akan menurunkan `FULL_CRUD`→`READ_ONLY` di **kedua jalur
sekaligus**, termasuk layar-layar live di atas yang sedang dipakai staf HR/koperasi/aset
sungguhan untuk create/update. Blok blanket ala sirs ditolak di sini karena (tidak seperti
sirs) domain-domain ini punya lalu lintas `scopeBindings`-terproteksi yang nyata di jalur
menu — menurunkannya adalah regresi fungsional, bukan sekadar pengetatan keamanan.

**Perbaikan akar masalah, dipilih setelah user memilih opsi ini secara eksplisit:** pisahkan
gerbang `buildAdministrative`/`listAdministrativeModels` dari `isBlockedClass()`. Jalur admin
browser sekarang default **DENY**, digerbangi daftar-terima baru `ADMINISTRATIVE_BROWSING_ALLOWLIST`
(kosong-hampir-kosong, seed satu-satunya: `ais.database.model.Agama`) — bukan lagi default-allow
kecuali diblokir token. Jalur menu (`tryAutoRegister`) sama sekali tidak disentuh, jadi seluruh
CRUD live yang terdaftar di atas tetap berfungsi seperti biasa. Ini juga menutup gap yang sama
untuk payroll (`PembayaranGaji`, `CaraPembayaranGaji`, `PengajuanPeminjaman` — sudah
didokumentasikan terpisah di javadoc masing-masing) dan modul lain manapun (`TenantRegistry`,
`SocialTenantSetting`, dst.) yang sebelumnya lolos `BLOCKED_CLASS_TOKENS` di jalur admin browser
ini, sekaligus — bukan tambal token satu per satu.

Sesuai prinsip "default deny / default disabled" di README paket ini: entity baru HARUS
ditinjau lalu ditambahkan satu per satu ke `ADMINISTRATIVE_BROWSING_ALLOWLIST` sebelum bisa
dijelajah lewat `model_crud_service.jsp` — daftar kosong-hampir-kosong ini adalah keadaan yang
disengaja, bukan kekurangan yang perlu buru-buru diisi.
