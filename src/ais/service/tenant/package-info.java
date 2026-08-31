/**
 * Inti proyek migrasi SaaS multi-tenant AIS: konteks request, penentuan kewenangan, schema
 * PostgreSQL per-tenant, provisioning, audit, dan rekonsiliasi data — seluruhnya mengacu
 * berulang kali ke satu "dokumen master" (bagian §6, §7, §9, §10, §11, §12, §14, §16, §24, §25
 * yang disitir di javadoc kelas masing-masing) yang menjadi sumber kebenaran tunggal rancangan
 * fase P1 sampai P9. Paket ini BUKAN paket biasa di antara paket lain AIS — ia adalah paket
 * yang menentukan apakah seluruh platform boleh melayani lebih dari satu pelanggan pada satu
 * basis data yang sama tanpa data pelanggan satu bocor ke pelanggan lain.
 *
 * <h2>Fase proyek (P1–P9)</h2>
 * <p>
 * Nomor fase muncul di banyak javadoc kelas sebagai penanda "kelas ini lahir di fase mana":
 * P1 membangun konteks request dan kewenangan dasar ({@link ais.service.tenant.TenantContext},
 * {@link ais.service.tenant.TenantContextResolver}, {@link ais.service.tenant.TenantMembershipResolver},
 * {@link ais.service.tenant.TenantRequestExecutor}, {@link ais.service.tenant.TenantSchemaLocator},
 * {@link ais.service.tenant.TenantAccessException}); P3 menurunkan seluruh katalog migrasi
 * schema Inventory &amp; Sales dari arsip FoxPro legacy (bundel v2–v8); P4 menambah audit
 * gaya-Envers ber-schema tenant ({@link ais.service.tenant.TenantAuditWriter}) dan tabel POS
 * eBisnis yang tidak ada di arsip lama (v9, prasyarat); P5 menopang impor legacy bertahap
 * (tabel operasional v8: {@code legacy_import_run}/{@code legacy_import_file}/
 * {@code legacy_import_row}); P7 menjalankan migrasi kanonik per-tenant
 * ({@link ais.service.tenant.TenantSchemaService}); P8 adalah cutover data-plane strangler-fig
 * ({@link ais.service.tenant.TenantDataPlaneService}) sekaligus RBAC peran tenant
 * ({@link ais.service.tenant.TenantRbac}, {@link ais.service.tenant.TenantRoleSeeder}); dan P9
 * adalah gerbang keamanan sebelum pemindahan produksi ke {@code TENANT_ONLY}
 * ({@link ais.service.tenant.TenantDataReconciliationService}). Fase-fase ini TIDAK berurutan
 * rapi secara kronologis satu-satu — sebagian tumpang tindih dan sebagian (P2, P6) tidak
 * bermanifestasi sebagai kelas terpisah di paket ini.
 * </p>
 *
 * <h2>Mode platform: LEGACY, HYBRID, TENANT_ONLY</h2>
 * <p>
 * Satu saklar tunggal ({@code pendaftaran_tenant_mode}, dibaca langsung lewat SQL oleh
 * {@link ais.service.tenant.TenantDataPlaneService#modePlatform}, bukan lewat cache
 * konfigurasi in-JVM, supaya perubahan mode oleh admin berlaku seketika) menentukan seberapa
 * jauh satu tenant sudah berpindah dari tabel {@code public}/{@code koperasi} bersama ke schema
 * miliknya sendiri:
 * </p>
 * <ul>
 * <li><b>LEGACY</b> (default deployment) — tenant mendapat registry, entitlement, membership,
 *     dan trial, tetapi data operasional tetap berada di scope per-Pendaftar yang sudah ada.
 *     Langkah pembuatan schema pada provisioning ditandai SKIPPED secara jujur, bukan
 *     dipalsukan seolah selesai.</li>
 * <li><b>HYBRID</b> — schema tenant ({@code <slug>} dan {@code <slug>__audit>}) sudah dibuat
 *     dan diverifikasi, dan data-plane baru mulai DUAL-WRITE: setiap tulis ke tabel shared
 *     lama tetap berjalan (supaya runtime POS existing tetap kompatibel) SEKALIGUS menulis
 *     baris mirror ber-id-sama ke schema tenant plus baris audit, dalam satu transaksi.
 *     Pembacaan daftar sudah beralih ke schema tenant, dengan {@code sinkronDariShared}
 *     melakukan backfill inkremental atas baris pra-cutover.</li>
 * <li><b>TENANT_ONLY</b> — target akhir migrasi: tenant program tanpa schema valid diblokir
 *     menjalankan data-plane baru (§3.3), sementara akun legacy pra-program tetap fail-open di
 *     jalur lama. Perpindahan produksi ke mode ini WAJIB didahului laporan rekonsiliasi bersih
 *     dari {@link ais.service.tenant.TenantDataReconciliationService}.</li>
 * </ul>
 *
 * <h2>Alur satu request tenant</h2>
 * <p>
 * Urutan baku, dipaksakan oleh {@link ais.service.tenant.TenantRequestExecutor#jalankan}: buka
 * satu Session &rarr; mulai satu transaksi &rarr; bentuk
 * {@link ais.service.tenant.TenantContext} lewat {@link ais.service.tenant.TenantContextResolver}
 * (yang di baliknya menentukan keanggotaan lewat
 * {@link ais.service.tenant.TenantMembershipResolver} dan nama schema lewat
 * {@link ais.service.tenant.TenantSchemaLocator}) &rarr; jalankan pekerjaan pemanggil &rarr;
 * commit, dengan Session selalu ditutup di {@code finally}. Konteks dibentuk DI DALAM transaksi
 * yang sama dengan pekerjaannya secara sengaja — bila dibentuk lewat Session terpisah,
 * keanggotaan tenant dapat dicabut tepat di antara pemeriksaan dan pemakaian. Sepanjang
 * request, {@link ais.service.tenant.TenantContext} adalah objek nilai immutable yang tidak
 * pernah disimpan di variabel statis atau {@code ThreadLocal} (supaya tidak bocor antar
 * request pada kontainer yang memakai ulang thread), dan {@code schemaName}/
 * {@code auditSchemaName} miliknya sengaja dihilangkan dari {@code toJsonKlien()} — nama
 * schema tidak boleh sampai ke klien. Kueri yang menyentuh schema tenant memakai
 * {@link ais.service.tenant.TenantSqlExecutor} untuk mensubstitusi penanda {@code {t}}/
 * {@code {a}} dengan nama schema yang divalidasi ULANG saat itu juga, bukan {@code SET
 * search_path} (yang rawan bocor lewat koneksi c3p0 yang dipakai ulang).
 * </p>
 *
 * <h2>Migrasi schema per-tenant</h2>
 * <p>
 * {@link ais.service.tenant.TenantSchemaMigrations} adalah katalog kanonik: setiap versi
 * (v1 di kelas ini, v2 sampai v9 di kelas
 * {@code TenantSchemaMigrationsV2}..{@code TenantSchemaMigrationsV9} agar berkas induk tetap
 * terbaca) adalah larik DDL ber-checksum SHA-256. {@link ais.service.tenant.TenantSchemaService#terapkanMigrasi}
 * menjalankannya secara idempoten dengan riwayat per-schema: versi tercatat dengan checksum
 * sama dilewati, checksum berbeda gagal keras (definisi kanonik yang sudah dirilis tidak boleh
 * berubah diam-diam — satu-satunya jalan menambah bentuk baru adalah menambah versi BARU di
 * akhir katalog). Ringkasan isi tiap bundel: v2 master organisasi/akses/mitra plus audit
 * generik pengganti cermin-per-tabel v1; v3 produk/stok/harga berversi; v4 pembelian/hutang
 * dagang; v5 penjualan/piutang dagang; v6 sales keliling/SPJ/nota; v7 akuntansi tenant
 * (bagan akun, jurnal, posting/pembalikan); v8 idempotensi, cetak, dan staging impor legacy
 * berlapis run&rarr;file&rarr;row; v9 sepuluh tabel POS eBisnis yang tidak ada di arsip FoxPro.
 * Seluruh bundel konsisten bergaya PostgreSQL 9.3 (tanpa {@code jsonb}, {@code ON CONFLICT},
 * atau {@code IF NOT EXISTS} pada indeks/kolom) walau produksi sungguhan mungkin lebih baru,
 * dan dijaga otomatis oleh {@code TenantSchemaMigrasiSelfTest} di subpaket {@code test}.
 * </p>
 *
 * <h2>RBAC di dalam tenant</h2>
 * <p>
 * {@link ais.service.tenant.TenantRbac} adalah lapisan TAMBAHAN (§16), bukan pengganti,
 * di atas keanggotaan/status/entitlement/tipe-aktor/menu/CRUD/lingkup yang sudah ada — satu
 * gagal berarti ditolak. Delapan peran bawaan ({@code OWNER}, {@code PEMILIK_SALES_INVENTORY},
 * {@code ADMIN_TENANT}, {@code GUDANG}, {@code PEMBELIAN}, {@code SALES_KELILING},
 * {@code KEUANGAN}, {@code AUDITOR}) diperiksa terhadap area dan sifat (baca/tulis/setuju) yang
 * DITURUNKAN dari nama aksi {@code si_*}, bukan didaftar satu per satu — aksi baru otomatis
 * tercakup, dan aksi yang namanya di luar kesepakatan fail-closed (tertolak untuk semua peran,
 * bukan diam-diam terbuka). Pengguna tanpa tenant ({@code tbmuser.pendaftar == null}, keadaan
 * mayoritas hari ini) sama sekali tidak tersentuh lapisan ini.
 * {@link ais.service.tenant.TenantRoleSeeder} menyemai kedelapan peran itu secara idempoten ke
 * tabel {@code role_tenant} setiap tenant, dengan teks keterangannya wajib mengikuti matriks
 * kewenangan sungguhan di {@link ais.service.tenant.TenantRbac}.
 * </p>
 *
 * <h2>Provisioning, audit, dan rekonsiliasi</h2>
 * <p>
 * {@link ais.service.tenant.TenantProvisioningService} menjalankan job provisioning sebagai
 * step machine idempoten (§10) — setiap langkah bertransaksi sendiri dan tercatat, sehingga
 * retry tidak pernah membuat schema/owner/seed ganda; kegagalan menandai job FAILED dan
 * memicu auto-retry terbatas. {@link ais.service.tenant.TenantProvisioningWorker} adalah
 * daemon latar bergaya {@code DepositoAroScheduler} yang meng-klaim job antre lewat
 * pembaruan {@code locked_by}/{@code locked_at} beroptimistic-lock, sengaja tanpa
 * {@code FOR UPDATE SKIP LOCKED} karena deployment PostgreSQL bisa 9.3.
 * {@link ais.service.tenant.TenantOnboardingService} dan
 * {@link ais.service.tenant.TenantEntitlementService} menentukan gerbang READY dan modul mana
 * yang tersedia (status {@code ACTIVE} vs {@code PLANNED} — entitlement bukan permission,
 * §6/§10.3/invariant #13). {@link ais.service.tenant.TenantAuditWriter} menulis jejak
 * gaya-Envers tulis-tangan (satu {@code revinfo} per aksi, banyak {@code audit_baris}) ke
 * {@code <schema>__audit} karena {@code org.hibernate.envers.default_schema} bawaan bersifat
 * statis per {@code SessionFactory} dan tidak dapat dipakai untuk isolasi per-tenant.
 * {@link ais.service.tenant.TenantDataReconciliationService} adalah gerbang terakhir sebelum
 * {@code TENANT_ONLY} produksi: membandingkan baris shared terhadap mirror tenant
 * ({@code hilangDiTenant}, {@code bedaField}, {@code yatimDiTenant}) dan dapat memperbaiki
 * dua kategori pertama secara idempoten — baris yatim hanya dilaporkan, tidak pernah dihapus
 * otomatis.
 * </p>
 *
 * <h2>Subpaket {@code ais.service.tenant.test}</h2>
 * <p>
 * Kumpulan harness verifikasi manual (bukan JUnit): {@code TenantSchemaDdlDump} mencetak
 * katalog migrasi sebagai skrip SQL siap jalan tanpa Hibernate;
 * {@code TenantKonteksSelfTest} menjaga konteks/substitusi SQL; {@code TenantSchemaMigrasiSelfTest}
 * mematok checksum bundel yang sudah dirilis dan memeriksa struktur seluruh katalog sekaligus;
 * {@code TenantRbacSelfTest} memastikan setiap aksi {@code si_*} terpetakan ke area RBAC dan
 * memeriksa pemisahan kewenangan antar peran. Semuanya dijalankan lewat {@code main()} dan
 * melempar {@link java.lang.IllegalStateException} bila ada pemeriksaan yang gagal.
 * </p>
 *
 * @see ais.service.tenant.TenantContext
 * @see ais.service.tenant.TenantContextResolver
 * @see ais.service.tenant.TenantSchemaService
 * @see ais.service.tenant.TenantSchemaMigrations
 * @see ais.service.tenant.TenantRbac
 * @see ais.service.tenant.TenantProvisioningService
 * @see ais.service.tenant.TenantAuditWriter
 * @see ais.service.tenant.TenantDataReconciliationService
 */
package ais.service.tenant;
