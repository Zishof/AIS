# Audit baseline AIS untuk modul Jurnal

Tanggal audit: 22 Agustus 2026. Audit ini read-only terhadap runtime dan schema; perubahan hanya dibuat di `docs/jurnal/`.

## Keputusan arsitektur target

Seluruh entity metadata, domain, workflow, authorization scope, statistik, job, audit, dan provenance modul Jurnal memakai `ais.database.hibernate.HibernateUtil` existing dan mapping `src/hibernate.cfg.xml`. Entity existing tetap pada schema existing-nya (`public`, `penelitiandanpengabdian`, dan lainnya); hanya tabel journal-specific yang benar-benar belum terwakili yang dibuat pada schema `penelitiandanpengabdian`. Karena tetap satu main SessionFactory, relasi/ID reference ke `Artikel`, `JurnalPenelitian`, `Tbmuser`, `RepoItem`, `RepoCollection`, dan entity AIS lain tidak memerlukan persistence unit baru.

`OjsHibernateUtil.java` dan `hibernate.ojs.cfg.xml` tidak digunakan oleh importer baru. Keduanya hanya dipertahankan sementara sebagai jalur legacy sampai kedua tombol import lama dialihkan dan compatibility test lulus. Database OJS sumber dibaca melalui koneksi JDBC read-only yang lifecycle-nya dimiliki job importer, dengan konfigurasi eksternal, parameter binding, pagination, timeout, dan tanpa schema mutation.

Isi BLOB besar memakai entity mandiri `LampiranJurnal` pada streaming SessionFactory dan tepat satu tabel `lampiran_jurnal`. Metadata/revision/galley tetap memakai `RepoBitstream` pada main SessionFactory dengan kontrak scalar `repoBitstreamId=RepoBitstream.id`. Tidak ada ORM lintas SessionFactory.

Hak akses global jurnal tidak membuat tabel grup/peran baru. `Tbmrole` existing diperluas dengan `jurnalAksesJson` pada kolom `jurnal_akses_json text`, dan `TbmroleAction` memperoleh tab keempat **Hak Akses Pengelolaan Jurnal** setelah tab Hak Akses Pedagang. Bentuk JSON mengikuti pola capability `ebisnisMenu` (`menu`, `crud`, dan operasi workflow), bukan pola `tokoAksesJson` yang hanya menyimpan daftar ID toko. Seluruh parsing, normalisasi, default-deny, dan katalog key dipusatkan pada `JurnalAksesKatalog` agar JSP/service tidak menafsirkan JSON sendiri.

## Checkout dan keselamatan kerja

- Repository Git aktual berada di `C:\opt\AIS\ais\src\main`, bukan `C:\opt\AIS\ais`.
- Branch: `feat/new-ui-rbac-role-user`.
- HEAD: `2d7a3c2ed5c195bdec11abc5c743e0304c2514e3`.
- Baseline publik pembanding tersedia di object database lokal: `2f12a5ea3932e3510dce35dee38cd9b9ba861d2e`.
- Branch lokal tercatat 58 commit di depan dan 37 commit di belakang remote tracking pada saat audit.
- Worktree memiliki 4.993 perubahan: 3.362 modified, 91 deleted, dan 1.540 untracked. Tidak dilakukan reset, checkout, pull, rebase, clean, atau overwrite.
- File relevan yang sudah modified sebelum implementasi jurnal meliputi `JurnalPenelitianAction.java`, `OjsHibernateUtil.java`, `LampiranJurnal.java`, `FileFotoLain.java`, ketiga konfigurasi Hibernate, `MenuInitializer.java`, `Pustaka.java`, `web.xml`, dan view Pustaka. Perubahan runtime harus diawali diff tiga arah per file.

## Build dan runtime aktual

- Source aktual: `C:\opt\AIS\ais\src\main\src`.
- Web root aktual: `C:\opt\AIS\ais\src\main\webapp`.
- Maven aggregator berada di `C:\opt\AIS\ais\pom.xml` dan menunjuk `src/main/src`.
- Default compiler level adalah Java 8; profile Maven tertentu menaikkan ke Java 11. Kode jurnal wajib tetap Java 8-compatible sampai deployment profile disepakati.
- UI memakai ZK 9.6.0.2 pada POM lokal; servlet/JSP publik dan New UI hidup berdampingan.
- Hibernate memakai mapping class eksplisit. Main dan streaming menggunakan SessionFactory berbeda. SessionFactory OJS ketiga adalah legacy dan bukan bagian arsitektur importer baru.

## Existing → keputusan

| Existing/path | Bukti aktual | Keputusan |
|---|---|---|
| `src/ais/database/model/penelitiandanpengabdian/Artikel.java` | `@Audited`, tabel `penelitiandanpengabdian.artikel`; `article_id` unik/non-null; relasi user, mahasiswa, jurnal lama, indeks dan SOP | **ENHANCE VIA SIDE TABLE**. Tetap canonical article; jangan gunakan `articleId` sebagai identity lintas sumber dan jangan menambah state workflow masif ke tabel audited. |
| `JurnalPenelitian.java` | `@Audited`, tabel `jurnal_penelitian`; `path` unik global dan `journalId` legacy | **CANONICAL JOURNAL MASTER**. Jangan buat entity/tabel `Jurnal` kedua. Tambah source/config link secara nullable dan gunakan `RepoCollection` bertipe JOURNAL/SECTION/CATEGORY/VOCAB sebagai hierarchy/projection. |
| `FileArtikel.java` | Metadata path/MIME/nama/tanggal dan relasi `Artikel`; bukan content/version graph | **REUSE AS PUBLISHED PROJECTION**. Workflow/file graph memakai `RepoBitstream`; BLOB memakai `LampiranJurnal`. Gunakan `LampiranJurnal` khusus streaming. |
| `AnggotaArtikel.java` | Hanya `Tbmuser` atau `Mahasiswa` plus keterangan | **REUSE AS PUBLISHED PROJECTION**. Authority dan ordered contributor memakai `RepoAuthorAuthority`/`RepoItemContributor`, bukan model kontributor jurnal baru. |
| `ArtikelTerindeks.java` | Klasifikasi indeks artikel eCampus | **REUSE AS-IS**; jangan dicampur dengan search index internal jurnal. |
| `src/ais/database/model/ojs/{Users,Roles,Journals,Articles,Issues,PublishedArticles}.java` | Memetakan tabel OJS legacy termasuk `articles`, `roles`, `published_articles` | **DEPRECATE ONLY AFTER TEST**. Tetap menjadi adapter legacy; bukan model domain dan bukan adapter OJS 3.5. |
| `JurnalPenelitianAction.java` | Query `journal_settings`/`article_settings` melalui SQL concatenation, query per row, `System.out` metadata, dan enam entity legacy | **ENHANCE/REDIRECT** ke wizard/job tunggal. Jalur lama tetap diuji dan tidak boleh menjadi writer kedua. |
| `OjsHibernateUtil.java`, `hibernate.ojs.cfg.xml` | SessionFactory sumber legacy tersendiri, `hbm2ddl.auto=none` | **DEPRECATE AFTER COMPATIBILITY TEST**. Importer baru memakai JDBC read-only eksternal; jangan tambahkan adapter/entity OJS 3.5 ke konfigurasi ini. |
| `HibernateUtil.java`, `hibernate.cfg.xml` | Persistence unit utama AIS dan mapping eksplisit lintas schema | **ONLY MAIN PERSISTENCE UNIT**. Reuse entity pada schema existing; tabel baru minimum masuk `penelitiandanpengabdian`. |
| `RepoCollection`, `RepoItem`, `RepoItemMetadata`, `RepoBitstream` | Aggregate Repository committed; hierarchy/profiles, workflow/version/DOI/OAI, repeatable localized metadata, checksum/file-version/access sudah ada | **REUSE + ALTER EXISTING** sebagai publication foundation. Hindari `NaskahJurnal`, `VersiPublikasiJurnal`, `EdisiJurnal`, `BagianJurnal`, `KategoriJurnal`, static page, highlight, DOI, tombstone, dan search tables baru. |
| `RepoWorkflowEvent`, `RepoItemRelation`, `RepoUsageEvent`, `RepoNotification` | Entity lokal sudah mapped; workflow event, generic relation, usage event, dan notification tersedia | **REUSE + VERIFY PHYSICAL DB**. Extend payload/stage/round/sequence/collection link secara kompatibel; jangan buat event/relation/usage/notification jurnal paralel. |
| `RepoAuthorAuthority`, `RepoItemContributor`, `RepoIntegrationEvent`, `RepoUserPreference` | Entity lokal mapped tetapi masih untracked pada audit | **REUSE AFTER OWNERSHIP/DEPLOYMENT CONFIRMATION**. Cocok untuk ORCID/ROR/affiliation/contributor, deposit audit, dan preference. Verifikasi tabel fisik sebelum schema design dikunci. |
| `Diskusi`, `DiskusiKomentar` | Thread generic pada schema `public`, topic/reply serta actor internal sudah mapped di main SessionFactory | **ALTER EXISTING FOR EDITORIAL THREADS**. Tambah nullable typed reference, journal stage, sequence dan closed state; jangan buat `DiskusiJurnal`. Membership terindeks tetap memakai satu link baru `PesertaDiskusiJurnal`. |
| `TemplateSurat`, `FormatTemplateSurat`, `TemplateSuratParameter` | Master/format/parameter untuk surat cetak/JRXML; consumer existing mengambil semua `TemplateSurat` tanpa discriminator channel | **DO NOT REUSE FOR EMAIL DEFINITIONS**. Satu `TemplateEmailJurnal` tetap baru agar key event, subject/body HTML, locale, version dan variable allowlist tidak bocor ke jalur surat/pembayaran existing. Sent snapshot tetap memakai `Notifikasi`. |
| `LampiranJurnal.java` | Entity mandiri, scalar `repoBitstreamId`, PostgreSQL Large Object, bounded `Blob`/`InputStream`, `StreamingHibernateUtil` | **DEDICATED STREAMING CONTENT**; bukan subclass `LampiranLain`, tidak memiliki relasi ORM ke main DB. |
| `hibernate.cfg.xml` | Mapping domain eksplisit dan schema update existing | **ENHANCE CAREFULLY** dengan model utama satu per satu; validasi schema dan Envers pada clone. |
| `hibernate.streaming.cfg.xml` | Pool/connection streaming dan `hibernate.jdbc.use_streams_for_binary=true` | **REUSE AS-IS** untuk `LampiranJurnal`; jangan mendaftarkan `RepoBitstream` atau entity main-domain ke SessionFactory streaming. |
| `Pustaka.java`, `baru/pustaka.jsp`, `baru/modul/pustaka/*` | Pola servlet publik → wrapper → modular JSP | **REUSE PATTERN SELECTIVELY**. File-file ini modified lokal, sehingga modul jurnal dibuat terpisah. |
| `webapp/WEB-INF/new/penelitiandanpengabdian/*` | Scaffold uiux/services; metadata `jurnal_penelitian` masih mengandung klasifikasi `finance` | **DO NOT EXTEND FOR JOURNAL**. Seluruh UI publik dan pengelolaan jurnal baru berada di `webapp/WEB-INF/baru/modul/jurnal/*`; URL lama boleh menjadi redirect/compatibility alias tanpa view jurnal kedua. |
| `MenuInitializer.java` | Memisahkan `Menu.id` dari `root`/`child`; file sudah modified lokal | **EXTEND WITH RECONCILER** setelah collision query database. Kode 4605/460501–460528 bukan PK otomatis. |
| `Tbmrole.java`, `TbmroleAction.java`, `EbisnisMenuKatalog.java` | Role aktif, tab pengaturan, serta pola JSON `menu`/`crud` sudah tersedia. `tokoAksesJson` sendiri hanya array ID; grid CRUD pada screenshot berasal dari `ebisnisMenu`. | **REUSE + EXTEND**. Tambah `Tbmrole.jurnalAksesJson`, tab indeks 3, dan `JurnalAksesKatalog`; key hilang/rusak/asing tidak pernah memberi akses. |
| `Menu`, `job_has_menu`, `RolePrivilage` | Menu/route gate existing dan privilege menu generik | **REUSE WITHOUT DUPLICATION**. `job_has_menu` membuka modul; capability bisnis jurnal berasal dari `jurnalAksesJson`. Jangan salin CRUD jurnal ke `RolePrivilage`. |
| `Notifikasi`, `NotifikasiDibaca`, `NotifikasiWa` | Delivery platform, status baca per penerima, dan jalur WA existing | **REUSE DELIVERY/READ STATE**. Tambah hanya preference/event linkage/provenance jurnal yang tidak dapat direpresentasikan tanpa mengubah semantik tabel generik. |
| `JenisPembayaran`, `LogPembayaran`, provider pembayaran existing | Abstraction tipe, settlement/log dan konektor payment platform | **REUSE PAYMENT RAILS**. Tabel baru dibatasi pada order/policy/subscription linkage jurnal dan idempotency yang belum dimiliki tabel existing. |
| `PengumumanPenelitian`, `KategoriPengumuman`, `LampiranPengumumanPenelitian` | Pengumuman penelitian, kategori bilingual, komentar/broadcast dan lampiran sudah hidup | **ALTER EXISTING** dengan nullable journal scope/localized JSON; jangan buat Pengumuman/JenisPengumuman jurnal baru. |
| `PerguruanTinggi`, `PerguruanTinggiLain` | Master institusi pendidikan existing | **LINK BEFORE CREATE**. Institusi subscription yang cocok memakai master existing; unmatched identity memakai `PerguruanTinggiLain`, policy berada pada `LanggananJurnal`, dan hanya IP range yang baru. |
| OJS `user_groups`, `user_group_settings`, `user_group_stage`, `user_user_groups` | Role/group sumber bersifat context/stage scoped | **MERGE** ke pemetaan `Tbmrole.jurnalAksesJson`, `PenugasanTahapJurnal`, dan `ImportMappingOjs`; jangan buat `GrupPeranJurnal` atau `PenugasanGrupPeranJurnal`. |

## Hasil deep-audit existing-first

`134/134` dan `905` adalah jumlah source table/column yang wajib dipertanggungjawabkan, bukan jumlah target table/column. Setelah audit Repository, pengumuman, notifikasi, institusi, payment, file streaming, dan ephemeral runtime:

| Decision per source table | Sebelum deep-audit | Setelah deep-audit |
|---|---:|---:|
| `NEW_MODEL` | 112 | **11** |
| `ALTER_EXISTING` | 2 | **37** |
| `MERGED` | 18 | **69** |
| `DERIVED` | 1 | **4** |
| `NOT_APPLICABLE_WITH_RATIONALE` | 1 | **13** |

Sebelas source table `NEW_MODEL` terkonsolidasi menjadi enam tabel domain: `TemplateEmailJurnal`, `LanggananJurnal`, `UndanganPeranJurnal`, `PesertaDiskusiJurnal`, `PenugasanTahapJurnal`, dan `PenugasanReviewerJurnal`.

Konsolidasi final: subscription type memakai versioned `RepoCollection.accessPolicyJson` dan immutable snapshot pada `LanggananJurnal`; review round memakai `RepoWorkflowEvent` plus `roundNumber`; definisi form/elemen memakai append-only version di `RepoCollection.workflowProfileJson`; respons form memakai immutable `responseJson` pada `PenugasanReviewerJurnal`. OJS `queries` memakai `Diskusi`/`DiskusiKomentar` existing.

Tabel baru pendukung yang masih sah karena belum ada equivalent aman: satu `AgregatPenggunaanJurnal`, satu `RentangIpLanggananJurnal`, serta empat tabel import (`ImportSumberOjs`, `ImportJobOjs` termasuk report/reconciliation JSON, `ImportCheckpointOjs`, `ImportMappingOjs`). Target final adalah **12 tabel baru**, dengan hard ceiling 15 hanya bila clone-schema/constraint/analytics test membuktikan satu aggregate harus dipisah. Setiap penambahan di luar budget membutuhkan bukti invariant/lifecycle/query yang tidak dapat ditampung existing atau JSON berversi.

Tiga belas source runtime tidak menjadi tabel bisnis: sessions; jobs/job_batches/failed_jobs; filter_groups/filters/filter_settings; oai_resumption_tokens; temporary_files; dan empat usage temporary staging. Raw evidence/count tetap direkonsiliasi, tetapi PHP payload/token/temporary rows tidak disalin.

Status repository dibedakan tegas: empat entity core (`RepoCollection`, `RepoItem`, `RepoItemMetadata`, `RepoBitstream`) tracked; delapan entity tambahan masih local/untracked atau berada pada file modified walau sudah didaftarkan di `hibernate.cfg.xml`. Keberadaan class bukan bukti tabel production. Jalankan `webapp/sql/verifikasi_repository_modern_v2.sql` dan query `to_regclass` tambahan untuk authority/contributor/integration/preference sebelum implementasi.

## Kontrak hak akses jurnal existing-first

- `Tbmrole.jurnalAksesJson` adalah profil capability global milik role aktif, bukan daftar objek jurnal dan bukan pengganti penugasan workflow.
- Field direkomendasikan `@NotAudited` dan `@Column(name="jurnal_akses_json", columnDefinition="text")` agar tidak memaksa perubahan tabel Envers existing. Karena itu penyimpanan tab wajib menulis audit event eksplisit berisi actor, role, before/after checksum, waktu, dan correlation ID tanpa menyalin data sensitif ke log.
- Struktur minimum berversi adalah `{"version":1,"menu":{...},"crud":{...},"workflow":{...}}`. Nilai selain boolean `true`, key yang tidak dikenal, JSON invalid, dan field yang hilang diperlakukan `false` untuk authorization.
- Editor UI boleh mempertahankan key asing saat round-trip untuk rolling deployment, tetapi evaluator tidak boleh memberinya hak. Checkbox **Semua** hanya mengubah capability yang terlihat pada baris tersebut.
- Hak efektif adalah irisan `job_has_menu`, `jurnalAksesJson`, tenant/journal ownership, penugasan object/stage, state workflow, anonymity, dan conflict-of-interest. Site administrator override harus eksplisit dan diaudit.
- `RolePrivilage` tetap menangani privilege menu generik existing. Ia tidak menjadi writer kedua atau fallback allow untuk capability jurnal.

## Gap importer yang dikonfirmasi

`JurnalPenelitianAction.singkronkanArtikel` membaca seluruh `PublishedArticles` melalui SessionFactory OJS legacy, lalu melakukan lookup `Articles`, `Issues`, `Users` dan settings per row. Query settings dibangun dengan concatenation ID. Kode mencetak ukuran, ID, objek, judul, abstrak, lisensi, ISSN, dan username ke stdout. Tidak ada source instance UUID, schema signature, dry-run, checkpoint, resume, file manifest, checksum, collision policy, atau reconciliation report.

Kesimpulan: importer existing hanya boleh dipertahankan sebagai jalur legacy di belakang orchestration baru. Orchestration baru tidak memanggil `OjsHibernateUtil`; ia membaca source melalui JDBC read-only, mendeteksi OJS 3.5 melalui `versions` plus signature `submissions/publications/submission_files`, lalu menulis target melalui `HibernateUtil`. Preflight harus gagal sebelum write bila signature tidak cocok.

## Baseline OJS resmi yang diverifikasi

- OJS tag commit: `372e3b84740344db6c2f85193c3eb3e4f539cb00`.
- Gitlink `lib/pkp`: `8b5f0fdc8d5664000b8652002781a14bd406bf21`.
- Source audit ditempatkan di `%TEMP%\codex-ojs-3505-audit`, di luar repository AIS.
- Fresh install migrations membuat 130 tabel core/PKP; bundled `generic/staticPages` menambah dua. Dua nama lain dalam matriks 134 adalah sumber legacy: `institutional_subscription_ip` dan `submission_tombstones`.

## Gerbang menuju model

Belum ada entity/runtime baru yang dibuat pada fase ini. Model hanya boleh dimulai setelah:

1. `02-OJS-3505-TABLE-MAPPING.md` tetap lulus generator 134/134 dan review field mapping;
2. collision query menu dijalankan pada clone database target;
3. diff lokal pada file integrasi diselesaikan tanpa kehilangan perubahan pengguna;
4. backup/restore clone serta strategi schema/Envers disetujui;
5. query read-only repository verification membuktikan tabel/kolom yang akan direuse benar-benar ada pada clone target;
6. model fase pertama mematuhi budget 12 tabel dan tidak memperkenalkan master journal/submission/version/issue/section/category/contributor/file/DOI/tombstone/search/discussion/institution-profile/subscription-type/review-round/review-form/review-answer baru.

