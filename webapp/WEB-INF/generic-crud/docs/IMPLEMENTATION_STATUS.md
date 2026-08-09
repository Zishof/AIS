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
- 49 rute custom masih memerlukan adapter domain khusus;
- 21 keluaran generator menunjuk nama inner/helper class yang tidak dapat
  di-resolve sebagai Action top-level dan tidak diaktifkan oleh runtime;
- 2.999 halaman bukan CRUD dan tidak boleh dihitung sebagai CRUD yang tertinggal.

CREATE otomatis aktif bila entity dapat dibuat dan Action mempunyai lifecycle
native yang cocok; ini mencakup `GenericCrudAction` serta Action
`DataInitDefault` yang memakai container `Window` atau `Component`. UPDATE aktif
bila entity dan `init(entity)` cocok.
DELETE otomatis tetap fail-closed karena renderer/selection/konfirmasi ZK lama
tidak mempunyai kontrak entity tunggal; hanya adapter eksplisit yang boleh
mengaktifkannya. Jadi angka 844 adalah cakupan jembatan lifecycle, bukan klaim
bahwa seluruh operasi khusus pada 49 Action custom sudah selesai.

Override native pertama untuk kelompok custom sudah tersedia pada
`JenjangProgramStudi`: CREATE/READ/UPDATE/DELETE memakai form metadata New UI,
validasi Jurusan/Jenjang mengikuti `JenjangProgramStudiAction`, dan transaksi
tidak lagi dibuka/ditutup dari composer ZK. Workflow header-detail seperti
transaksi SIRS tetap diklasifikasikan adapter domain khusus; workflow tersebut
tidak diturunkan menjadi CRUD satu tabel karena dapat menghilangkan detail,
stok, jadwal, cetak, atau efek bisnis lain.

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
