# 01 — Audit Source Existing (P0)

> Tanggal audit: 2026-08-12 · Branch: `feat/new-ui-rbac-role-user` · HEAD awal: `aaf825b5`
> Semua fakta di bawah diverifikasi BACA LANGSUNG source (bukan asumsi dokumen prompt).

## 1. `ais.database.model.Pendaftar` (src/ais/database/model/Pendaftar.java)

- `@Entity @Audited @Table(schema="public", name="pendaftar")`, extends `GeneralValueObject`, dynamicInsert/Update.
- Field existing: `kode, nama, keterangan, domain, email, alamat, telp, kontakperson, telpkontakperson, emailkontakperson, aktif, merupakanSekolah, motto, css, negara, provinsi, kotaKabupaten(@Column kota_kabupaten), kecamatan, jenisBisnis(@Column jenis_bisnis), passwordHash(@Column password_hash), passwordSalt(@Column password_salt), dibuatPada(@Column dibuat_pada), admin(ManyToOne Tbmuser, @JoinColumn admin, nullable)`.
- ⚠️ **`getAktif()` default null→TRUE** (line 148-150) — `aktif=false` WAJIB di-set eksplisit; baris lama dgn NULL dianggap aktif.
- ⚠️ **`getMerupakanSekolah()` default null→TRUE** (line 205-207) — tenant bisnis WAJIB `setMerupakanSekolah(false)` eksplisit (sudah dilakukan `PendaftarPublicHelper.daftar` line 129).
- `getDomain()`: `@Column(unique=true, nullable=false)`; getter mengembalikan null bila blank (line 156-159).
- ⚠️ Kolom TANPA @Column eksplisit (kode, nama via @Column tanpa name tapi single-word, email, alamat, telp, kontakperson, telpkontakperson, emailkontakperson, aktif, motto, css, domain) → nama kolom lowercase gabung (implicit naming deployment ini TIDAK menyisipkan underscore). `kontakperson` dkk memang single-word-lowercase di DB.
- JavaDoc `getPasswordHash()` menyebut "SHA-256(salt+password)" — **BASI/SALAH**: implementasi nyata di `PendaftarPublicHelper.pbkdf2` = **PBKDF2WithHmacSHA256, 120.000 iterasi, key 256-bit, salt 32-byte hex** (helper line 51-53, 230-241).

## 2. `PendaftarPublicHelper` (src/ais/action/servlet/api/PendaftarPublicHelper.java, 259 baris)

- `daftar(...)` (line 76): validasi nama/email/password≥6/konfirmasi → cek email unik HANYA di antara akun self-service (`Restrictions.isNotNull("passwordHash")`, line 98-103 — email legacy duplikat eCampus/eSchool sengaja tidak dihalangi) → `buatSlugUnik` dari nama bisnis → `kode=domain=slug` → **`aktif=true` LANGSUNG** (line 124) → `merupakanSekolah=false` → save+commit → kembalikan entity penuh.
- `login(email, password)` (line 154): filter `passwordHash IS NOT NULL`, cocokkan PBKDF2, tolak bila `!aktif`.
- ⚠️ `cocokkanPassword` memakai `equalsIgnoreCase` (line 227) — BUKAN constant-time (`MessageDigest.isEqual`). Perbaikan di jalur baru; jalur lama biarkan (kompatibilitas).
- ⚠️ `buatSlugUnik` = query-then-insert loop (line 196-212) TANPA penanganan constraint-violation race — aman-praktis krn kolom domain unique (insert kedua akan exception), tapi BUKAN reservasi atomik. Jalur baru pakai `schema_name_reservation` + unique constraint.
- Belum ada: OTP/verifikasi email, consent, idempotency, rate-limit, CSRF, multi-tenant, workflow status. `HasilProses{sukses,pesan,pendaftarId,namaBisnis,pendaftar}`.

## 3. `EbisnisPublicServlet` (src/ais/action/servlet/EbisnisPublicServlet.java, 202 baris)

- Konstanta sesi: `SESSION_PENDAFTAR="pendaftarEbisnisEntity"` (ENTITY Pendaftar penuh detached di HttpSession), `SESSION_FLASH`, `SESSION_FLASH_JENIS`.
- POST `aksi=daftar|login|logout` (+`ajax=1` → JSON `{status:"00"/"91",description,redirect}`); non-AJAX redirect ke `/ebisnis.jsp`.
- POST `s=<subAksi>` = dashboard (ringkasan, brand_list/tambah, toko_list/tambah, mesin_pos_list/tambah, investor_list/tambah, manajemen_list/tambah) → delegasi `PendaftarDashboardHelper`; resolve Pendaftar dari SESI (IDOR-safe), tolak bila sesi kosong.
- GET: sesi ada → forward `/WEB-INF/baru/dashboard_ebisnis.jsp`; tidak → redirect `/ebisnis.jsp`.
- TIDAK ada CSRF token, TIDAK ada rate limit, TIDAK ada verifikasi email di jalur ini.
- **Bridge P2**: `aksi=daftar` akan didelegasikan ke service pendaftaran baru (satu sumber aturan), response lama dipertahankan.

## 4. `PendaftarDashboardHelper` (src/ais/action/servlet/api/PendaftarDashboardHelper.java, 486 baris)

- Pola IDOR-safe: SEMUA query difilter `Restrictions.eq("pendaftar.id", pendaftar.getId())`; `milikPendaftar(toko,pendaftar)` utk aksi per-toko.
- `ringkasan/brandList/brandTambah/tokoList/tokoTambah/mesinPosList/mesinPosTambah/investorList/investorTambah/manajemenList/manajemenTambah` — HANYA list+tambah (belum ada update/nonaktif — gap utk onboarding P5).
- Mesin POS = `inventory.Pedagang` standalone per mesin (tabel fisik `koperasi.pedagang`, lihat `useridDipakai` line 461 raw SQL) — userid/password plaintext auto-generate, ditampilkan SEKALI + `qrData=userid:password`. Investor/AkunManajemen serupa (kolom `pass` plaintext, level kepercayaan akun kasir).
- ⚠️ **TIDAK ADA gerbang status** — begitu login (dan `daftar` lama langsung aktif), Pendaftar bebas membuat Brand/Toko/Mesin POS/Investor/Manajemen. Ini titik pemasangan guard READY/entitlement (P5): tambahkan pemeriksaan status tenant di depan seluruh aksi mutasi TANPA merusak pemakaian existing (mode LEGACY: pendaftar lama tanpa registry tetap boleh — fail-open utk data existing, fail-closed utk tenant baru berstatus belum-READY).

## 5. `PendaftarAction` (src/ais/action/master/PendaftarAction.java, 769 baris)

- Composer ZK admin (GenericAutowireComposer + DataCriteria/DataSearchDefault/DataInitDefault): grid + form CRUD Pendaftar utk staf internal (nama, kode, domain, alamat, telp, kontakperson, merupakanSekolah, email, motto, css, kop/lampiran, relasi PerguruanTinggi/Sekolah/Yayasan, Tbmuser admin, Tbmrole).
- Filter super-admin `abaikanDomain` (line 80). Ini basis backoffice P6 (tab tambahan permohonan/provisioning/dll).

## 6. `GeneralValueObject` (base class semua entity)

- Menyediakan `id/kode/nama/keterangan/nomorUrut/oleh/olehId/tanggal_dirubah` + `check()` (resolusi proxy detached: EntityIdentityMap → cache → initialize → reload openSession) + equals by id.
- Pola entity baru (template terbukti compile: `koperasi/PayableFakturInfo.java`): `@Entity @org.hibernate.annotations.Entity(dynamicInsert,dynamicUpdate) @Audited @Table(schema,name)`, field private + getter ber-`@Column(name=...)` EKSPLISIT SEMUA (landmine implicit naming), `@PreUpdate onUpdate()` → `AuditTimestampInterceptor.ubah(this)`, `@Id @GeneratedValue(IDENTITY)`.

## 7. Route & duplikasi semantik

- `web.xml`: TIDAK ada mapping `/pendaftaran` (grep kosong) — aman dipakai.
- TIDAK ada class existing `JenisUsaha*/TenantRegistry/TenantMembership/ProvisioningJob/PendaftaranTenant` (grep kosong) — tidak ada duplikasi semantik. (Detail entity lain + infra: lihat lampiran hasil audit agent di bagian 8-9, diisi setelah sweep selesai.)

## 8. Entity terkait kepemilikan Pendaftar (Brand/Toko/Pedagang/Investor/AkunManajemen/Tbmuser/Tbmrole)

Hasil sweep baca-penuh (fakta kunci; detail kolom per file ada di source):

- **`GeneralValueObject` BUKAN `@MappedSuperclass`** — field superclass TIDAK dipersist; setiap entity wajib mendeklarasi ulang `id/oleh/olehId/tanggal_dirubah` sendiri (semua entity existing melakukannya; template: `koperasi/PayableFakturInfo.java`). Akses Hibernate = property access (@Id di getter).
- Tidak ada `naming_strategy` di cfg → default Hibernate 3: properti camelCase TANPA @Column → kolom lowercase GABUNG (`merupakanSekolah`→`merupakansekolah`; dikonfirmasi JavaDoc `SetoranTenant.java:44-48`). `hbm2ddl.auto=update` (cfg line 50) menambah kolom/tabel baru otomatis, TAPI TIDAK menambah kolom baru ke tabel audit Envers existing.
- **Brand** (`public.brand`, @Audited): `pendaftar` NOT NULL (line 82), `aktif` default true, `dibuat_pada`. **Investor** (`public.investor`): + `userid` unique NOT NULL, `pass` plaintext NOT NULL, `kepemilikan_json` text. **AkunManajemen** (`public.akun_manajemen`): + `jabatan`, `userid` unique, `pass`. Ketiganya klon struktural ber-`pendaftar` NOT NULL.
- **Toko** (`koperasi.toko`, @Audited): `pendaftar` **nullable** (Toko lama Kantin/Koperasi null — Toko.java:297-305), `brand` nullable, + profil (alamat/kota/kode_pos/telp/email/pic_nama/pic_hp/npwp/jam_operasional/pesan_terima_kasih).
- **Pedagang** (`koperasi.pedagang`, @Audited): `userid` TANPA unique di anotasi; ⚠ **dalam praktik TIDAK unik global** — `PedagangAction.cariPedagangToko/salinAkunPedagang` (line 332-363) SENGAJA membuat beberapa baris Pedagang ber-`userid` SAMA (1 baris/toko utk multi-toko). Cek ketersediaan userid existing (`KantinHelper:2034`, `PendaftarDashboardHelper.useridDipakai:461`) pakai COUNT>0 — jadi cek benturan username tenant vs `pedagang.userid` HARUS COUNT>0 juga (konservatif), bukan asumsi unique.
- **Tbmuser** (`public.tbmuser`, @Audited): PK `userid` unique; TIDAK punya relasi ke Pendaftar (arah sebaliknya: `Pendaftar.admin`). Getter SANGAT berlogika (derive userId/nama/email/aktif lintas 10+ entity; `getUserPassword()` re-encrypt DES) — JANGAN meniru pola getter berat utk entity baru; JANGAN menyalin hash PBKDF2 ke jalur DES.
- **Tbmrole** (`public.tbmrole`, @Audited): PK `roleid`; TIDAK ada relasi Pendaftar (role global). Akses menu POS/eBisnis terkonsolidasi di kolom JSON **`ebisnis_menu`** (@NotAudited, parser `EbisnisMenuKatalog.urai`) — entitlement tenant baru TIDAK boleh berupa kolom boolean baru di Tbmrole; cukup tabel entitlement sendiri + (bila perlu) kunci JSON.
- **Duplikasi semantik**: TIDAK ada entity tenancy/subscription/entitlement/consent/verification-email existing. ⚠ `inventory.SetoranTenant` (koperasi.setoran_tenant) = "tenant" penyewa STAN/KIOS (bagi hasil omzet per Toko) — makna BERBEDA dari multi-tenancy; nama kelas baru tetap `TenantRegistry`/dll (tidak tabrakan nama), perbedaan makna didokumentasikan di sini.
- Entity yang menunjuk Pendaftar (kolom join selalu `pendaftar`): Brand, Investor, AkunManajemen (NOT NULL); Toko (nullable); PerguruanTinggi:921, SatuanKerja:251, Sekolah:1101, Yayasan:364 (unique — 1:1). `Pendaftar.domain` unique de-facto = tenant identifier publik existing.

## 9. Infrastruktur (Common.ROOT, FilterJSP, Spring Security, hibernate.cfg.xml, MailClient, ErrorAuditUtil, scheduler)

### Common.ROOT (Common.java:394)
`public static String ROOT = "/ais";` — mutable, di-overwrite runtime: `AppStartupListener:637` (dari path WEB-INF/classes) dan `FilterJSP:238` (`req.getContextPath()`), juga oleh banyak servlet halaman. Pola link JSP: `<%=Common.ROOT%>/pmb` (erp.jsp:2612). Catatan: `ebisnis.jsp` existing memakai `request.getContextPath()` — nilainya identik dgn Common.ROOT pada runtime; halaman baru WAJIB pakai `Common.ROOT + "/pendaftaran"` sesuai dokumen master.

### Route `/pendaftaran` — kesimpulan intersepsi
1. **ErrorAuditFilter** `/*` → lolos (audit error saja).
2. **springSecurityFilterChain** `/*` → `applicationContext-security.xml:36` catch-all `/** = IS_AUTHENTICATED_ANONYMOUSLY` → publik TANPA perubahan config (juga TANPA proteksi — CSRF/rate-limit ditangani aplikasi).
3. **FilterJSP** `/*` → `/pendaftaran` TIDAK match daftar bypass `isIgnoredPath` (:450-456) → masuk `handleRouting` (set Common.ROOT dst.) → tidak match cabang mana pun → `chain.doFilter` (:294) → servlet-mapping. **FilterJSP TIDAK perlu diubah.** (Akses `pendaftaran_tenant.jsp` langsung akan dipantulkan ke `/` oleh :291-292 — sesuai desain, JSP hanya via forward servlet.)
4. `web.xml`: cukup 1 blok `<servlet>` + `<servlet-mapping>` pola EbisnisPublicServlet (:919-924, :1739-1742; tanpa load-on-startup wajib). TIDAK ada session-config (timeout default kontainer).

### CSRF & random
- TIDAK ada util CSRF di Common. Pola siap-tiru: `ais.common.newui.NewUiCsrfUtil` (session key `nui_csrf`, token UUID, `getToken(HttpSession)`/`isValid(request)`) dan `GenericCrudCsrf` (403 CSRF_INVALID).
- `Common.randLong()` NON-kriptografis (JavaDoc-nya sendiri menyuruh SecureRandom utk keamanan) — token verifikasi WAJIB SecureRandom.

### Email
- `ais.common.MailClient` = kelas DEMO (text/plain, kredensial dari argumen, tak ada pemanggil produksi — hanya JamesConfigTest). **BUKAN jalur produksi.**
- Jalur produksi: **`ais.delivery.email.sender.MailSender.sendMail(JSONArray userIds, String subject, String body, String sender, String recipients, GeneralValueObject dataObject)`** — HTML (`text/html`), SMTP dari tabel Konfigurasi (`default_mailhost`, `default_mail_port` 465, TLS/SSL, `default_email_username/password`), gate `Common.bolehKonfigurasi("aktfikan_pengiriman_email", TIDAK_AKTIF)`. Contoh: ResetPasswordApi:293-303. Implikasi: alur verifikasi WAJIB tahan-gagal-kirim (token tetap tersimpan, bisa resend/manual-verify admin).

### ErrorAuditUtil
`record(Throwable, String info[, HttpServletRequest[, boolean rethrow]])` → `public.error_log` (entity ErrorLog @Audited, cfg:572). Dedup 1×/lokasi per JVM. Dipakai utk audit exception; audit EVENT bisnis pendaftaran perlu tabel event sendiri (bukan error_log).

### hibernate.cfg.xml
- SUMBER: `src/hibernate.cfg.xml` (2331 baris). `webapp/WEB-INF/classes/hibernate.cfg.xml` TIDAK ada di source tree (hanya log4j.properties) — file classes dihasilkan build/deploy dan DITULIS ULANG startup (`AppStartupListener:708,722-729`, placeholder `${url}`:25). `build/classes/hibernate.cfg.xml` = artefak Eclipse STALE (tanpa Brand/Investor/AkunManajemen) — bukan acuan. Instruksi dokumen "kedua hibernate.cfg.xml" terpenuhi dgn mengubah SATU sumber `src/hibernate.cfg.xml` (deploy menyalinnya).
- Blok mapping model publik: `:368-371` (Pendaftar/Brand/Investor/AkunManajemen) — entity baru public didaftarkan berdampingan di sini.
- Envers: `store_data_at_delete=true`, suffix `__audit`, **schema audit `new_audit`**, listener auto-register dari hibernate3.jar + listener kustom :2292-2325. ⚠ Peringatan cfg:41-48: `hbm2ddl.auto=update` TIDAK menambah kolom baru ke tabel `new_audit.*__audit` existing → **desain ini SENGAJA tidak menambah kolom apa pun ke `Pendaftar`** (extension table `pendaftar_tenant_profile`), semua entity baru = tabel baru (main+audit dibuat otomatis saat startup).
- `hbm2ddl.auto=update` (:50), dialect PostgreSQL, c3p0, EhCache L2, `current_session_context_class=thread`.

### Scheduler/worker existing (pola utk ProvisioningWorker P4)
Pola kanonik: `DepositoAroScheduler` — `static volatile ScheduledExecutorService` + `mulai()/hentikan()` (daemon thread factory), `scheduleAtFixedRate`, buka `HibernateUtil.openSession()+beginTransaction` sendiri (thread latar TIDAK lewat FilterJSP), start/stop di `AppStartupListener:237/510`. Contoh lain: StokThresholdScheduler, RepositorySyncScheduler.

### JSP existing
- `ebisnis.jsp` (±3200 baris): landing publik di-forward dari servlet `Index` bila konfigurasi `default_login_ke_ebisnis` aktif; modal Daftar (`aksi=daftar`, field namaBisnis/jenisBisnis[teks]/negara/provinsi/kotaKabupaten/kecamatan/alamat/kontakPerson/telpKontakPerson/telp/email/password/konfirmasiPassword) + Masuk; fetch AJAX `ajax=1`; TANPA CSRF.
- `dashboard_ebisnis.jsp` (±470 baris): guard sesi → redirect; `panggil(s,data)` fetch tunggal; aksi `ringkasan/brand_list/brand_tambah/toko_list/toko_tambah/mesin_pos_list/mesin_pos_tambah/investor_tambah/manajemen_tambah`; kredensial+QR via `api.qrserver.com` (eksternal); TANPA CSRF.

### Grep pemakaian (ringkas)
`SESSION_PENDAFTAR`: EbisnisPublicServlet (5×), ebisnis.jsp:27, dashboard_ebisnis.jsp:14. `jenisBisnis`: hanya Pendaftar model + PendaftarPublicHelper:120 (tulis) — TIDAK ada konsumen lain yang membaca nilainya utk logika → aman dijadikan snapshot kompatibilitas. `merupakanSekolah`: model + PendaftarAction (form admin) + PendaftarPublicHelper:129.
