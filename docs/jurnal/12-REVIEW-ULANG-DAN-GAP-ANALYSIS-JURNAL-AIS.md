# Review ulang dokumen dan kode modul Jurnal AIS

Tanggal review: 23 Agustus 2026  
Baseline GitHub: `Zishof/AIS`, branch `master`, commit `2f12a5ea3932e3510dce35dee38cd9b9ba861d2e`  
Baseline OJS: tag `3_5_0-5`, commit `372e3b84740344db6c2f85193c3eb3e4f539cb00`, pkp-lib `8b5f0fdc8d5664000b8652002781a14bd406bf21`

## Kesimpulan utama

Paket dokumen sudah jauh lebih matang daripada kode yang terlihat di GitHub. Cakupan 134 tabel, 905 field, 45 plugin, 73 template email, 28 submenu, migrasi, RBAC, dan release gate merupakan fondasi yang baik. Namun paket ini belum siap dieksekusi tanpa revisi karena:

1. kode dan evidence terbaru hanya ada di SVN/workspace lokal, tidak dapat diverifikasi dari GitHub;
2. beberapa klaim build, jumlah tabel, ukuran WAR, dan checksum saling bertentangan;
3. instruksi terbaru pengguna mewajibkan `LampiranJurnal`, sementara ADR lama melarangnya;
4. kebijakan perubahan schema “melalui Hibernate” bertentangan dengan beberapa bagian yang mewajibkan migration SQL eksplisit;
5. pemetaan 905 field masih banyak berupa target generik, belum field-to-field konkret;
6. 28 submenu belum memiliki katalog nama, route, capability, dan tujuan layar yang canonical;
7. UI/UX baru sebatas persyaratan umum dan belum memiliki design system, inventaris layar, pola interaksi, serta visual acceptance matrix;
8. evidence implementasi tidak boleh diwariskan sebagai `PASS` pada komputer/checkout baru tanpa re-run.

Status yang aman: **dokumen adalah design baseline dan historical evidence, bukan bukti bahwa implementasi GitHub sudah selesai**.

## Sumber yang benar-benar berhasil diverifikasi

- Repository `Zishof/AIS` publik dan default branch `master` berada pada commit `2f12a5e` tanggal 17 Agustus 2026.
- `docs/jurnal` tidak ada pada `master` maupun branch GitHub `feat/new-ui-rbac-role-user`; dokumen yang diunggah lebih baru daripada GitHub.
- AIS sudah mempunyai `Artikel`, `JurnalPenelitian`, `FileArtikel`, `AnggotaArtikel`, enam model OJS legacy, `OjsHibernateUtil`, `JurnalPenelitianAction`, Repository AIS, servlet OAI, pola Pustaka, dan New UI registry parsial.
- OJS 3.5.0-5 resmi berlisensi GPL. Paritas harus clean-room: tiru perilaku dan kontrak data, jangan menyalin PHP, template, CSS, JavaScript, ikon, terjemahan, atau aset OJS.

## Review per dokumen

| Dokumen | Yang sudah baik | Kekurangan yang harus diperbaiki |
|---|---|---|
| `00-AUDIT-BASELINE-AIS.md` | Existing-first, dirty-worktree safety, importer legacy, schema boundary | Klaim branch/HEAD/worktree lokal tidak dapat diverifikasi; Java 8 vs kompatibilitas legacy perlu dipastikan ulang; keputusan “tidak ada LampiranJurnal” bertentangan dengan instruksi terbaru |
| `01-OJS-3505-FEATURE-INVENTORY.md` | 12 area produk, role, workflow, email, API | Terlalu ringkas untuk klaim “semua fitur”; perlu requirement ID per capability, business rule, layar, API, role, state, dan acceptance test |
| `02-OJS-3505-TABLE-MAPPING.md` | 134/134 tabel, 905 kolom, keputusan per tabel | Banyak field diarahkan ke nama generik seperti `Target.fieldCamelCase` atau raw payload; belum menunjukkan field target fisik, tipe target, nullability, converter, FK, uniqueness, query, dan owner transform yang nyata |
| `03-OJS-3505-PLUGIN-PARITY.md` | 45/45 plugin dan clean-room boundary | Status discovery dan evidence bercampur; belum ada konfigurasi UI, route, error/retry UX, observability, sandbox fixture, dan fallback per plugin |
| `04-ARSITEKTUR-MODUL-JURNAL.md` | Source of truth dan konsolidasi entity | Perlu diagram deployment/main-streaming DB, transaction compensation, cache/search strategy, concurrency/locking, multi-tenant discriminator, dan keputusan final `LampiranJurnal` |
| `05-RENCANA-MIGRASI-OJS.md` | Dry-run, checkpoint, resume, provenance | Perlu wizard resolusi konflik, preview sampel, ETA/progress, cancel semantics, chain-of-custody file, quarantine, rollback unit, serta strategi delta/cutover setelah initial import |
| `06-SECURITY-AND-RBAC-MATRIX.md` | Fail-closed, capability JSON, scope object | Perlu matriks route-by-route 28 menu, field-level redaction, session fixation, CSP per viewer, rate limit, lockout, audit retention, break-glass, dan test reviewer anonymity berbasis browser |
| `07-TEST-AND-ACCEPTANCE-MATRIX.md` | Unit, database, security, E2E, dataset performa | Evidence lama bercampur dengan target; visual regression, device/browser matrix, usability test, Lighthouse/axe, load/soak, recovery-time, dan real sandbox belum selesai |
| `08-IMPLEMENTATION-PHASES-AND-RELEASE-GATES.md` | Urutan 0–19 dan gate | Belum ada estimasi, dependency owner nyata, change budget per fase, rollback command, demo checkpoint, dan aturan kapan UI boleh dianggap selesai |
| `09-FEATURE-TRACEABILITY-REGISTER.md` | Namespace requirement dan ADR | Register hanya mendefinisikan rentang ID; belum memiliki satu baris per requirement yang mengikat source → target → route → permission → test → evidence |
| `10-IMPLEMENTATION-EVIDENCE.md` | Jujur membedakan partial/unverified | Memuat baseline berbeda: 3.277/3.293, 3.303/3.323, WAR 739.616.271 dan 737.777.432 byte; harus dinormalisasi menjadi run ledger per timestamp, commit, DB fingerprint, dan artifact hash |
| `11-HANDOFF-KOMPUTER-BARU.md` | Checklist pemindahan dan blocker | Bagian “belum terverifikasi setelah jsoup” bertabrakan dengan addendum “sudah lulus”; handoff harus menunjuk satu run ID authoritative dan menandai record lama superseded |
| `Generate-Ojs3505TableMapping.ps1` | Pin commit dan fail-fast 134 tabel | Parser regex migrasi PHP rapuh; tidak menangkap semua perubahan upgrade/rename/drop/constraint secara semantik; perlu manifest snapshot, golden test, deterministic diff, dan field target catalog nyata |

## Temuan kode GitHub yang paling penting

### Model existing

- `Artikel.article_id` unik global dan getter membuat ID negatif acak bila null. Ini tidak aman sebagai identitas multi-source OJS dan getter tidak boleh mempunyai side effect persistence.
- Sejumlah getter `Artikel` mengubah state dari Sinta/Scholar atau memberi default dinamis. Mapping/import harus memakai DTO/service, bukan mengandalkan getter tersebut untuk rekonsiliasi.
- `JurnalPenelitian.path` unik global; desain multi-tenant harus menentukan apakah slug unik global atau unik per tenant/journal host.
- `RepoItem` dan `RepoBitstream` pada GitHub masih sederhana; field workflow/version/publication/storage backend yang diklaim dokumen belum tampak pada baseline GitHub.
- `RepoBitstream.pathSistem` non-null dan diasumsikan path; pemakaian BLOB streaming memerlukan storage strategy yang eksplisit.

### Importer legacy

- `JurnalPenelitianAction` memakai `OjsHibernateUtil`, query settings berbasis string concatenation, query per-row, dan banyak `System.out`.
- Importer hanya menyalin journal serta artikel published dari struktur OJS lama; tidak menangani workflow, review, file, version, galley, user group, email, payment, metrics, atau provenance lengkap.
- Matching `Artikel` hanya memakai `articleId`; dua sumber OJS dapat bertabrakan.
- Proses tidak memiliki dry-run, checkpoint, resume, cancel, manifest file, checksum, reconciliation, dan resolusi konflik interaktif.

### UI existing

- `webapp/WEB-INF/new/penelitiandanpengabdian/uiux/jurnal_penelitian.jsp` adalah scaffold metadata generik dan salah diklasifikasikan sebagai `finance`.
- `pagesmasterpenelitiandanpengabdianjurnalpenelitianzul/index.jsp` hanya memanggil `DynamicJspCrudGenerator`; ini bukan workspace editorial modern.
- `NewUiRouteRegistry` pada GitHub hanya mempunyai alias minimal `tbmrole` dan `tbmuser`; belum ada 28 route jurnal.
- Pola Pustaka dapat dipakai untuk shell/header/footer, tetapi `p` dan `s` dinamis tidak boleh disalin tanpa route allowlist agar tidak membuka arbitrary JSP include.

## Konflik keputusan yang wajib diselesaikan

### 1. `LampiranJurnal`

Instruksi terbaru pengguna mewajibkan `ais.database.model.file.LampiranJurnal` dan `hibernate.streaming.cfg.xml`. Karena instruksi terbaru lebih authoritative, paket baru menetapkan:

- buat `LampiranJurnal` sebagai entity BLOB khusus jurnal pada streaming SessionFactory;
- ikuti pola aman `LampiranLain`, tetapi jangan subclass entity;
- gunakan scalar `repoBitstreamId`, `jenis`, checksum, ukuran, MIME, storage state, audit, dan unique constraint;
- jangan membuat relasi ORM lintas SessionFactory;
- total target menjadi **12 tabel main + 1 tabel streaming**, bukan lagi “tepat 12 tabel keseluruhan”.

Semua ADR, mapping, test, dan evidence lama yang mengatakan “tidak ada LampiranJurnal” harus direvisi.

### 2. Perubahan schema melalui Hibernate

Permintaan pengguna: alter/modifikasi diserahkan ke Hibernate. Implementasi aman:

- annotation/entity mapping adalah sumber canonical;
- Hibernate schema update hanya dijalankan pada clone dev/SIT yang sudah dibackup;
- tangkap dan review SQL yang dihasilkan;
- production tidak boleh menjalankan update schema otomatis saat startup tanpa change window, backup, dry-run, dan approval;
- jangan menaruh `ALTER TABLE` ad-hoc dalam servlet/action.

### 3. Kompatibilitas Java

Dokumen lokal menyebut Java 8, sedangkan sebagian source/pola legacy masih menyatakan Java 1.6/1.7-compatible. Perintah implementasi harus membaca POM/Ant aktual dan memakai level compiler paling rendah yang benar-benar berlaku; jangan memakai record, lambda, stream API, var, text block, atau API JDK baru sebelum baseline disepakati.

## Kekurangan UI/UX yang paling besar

Dokumen lama belum menentukan:

- design token warna, tipografi, spacing, radius, elevasi, focus ring, status dan dark/high-contrast behavior;
- informasi arsitektur untuk publik, author, reviewer, editor, production, manager, dan administrator;
- 28 nama submenu beserta route/capability;
- dashboard per role, editorial queue, kanban/timeline, wizard submission, compare revision, blind review workspace, issue builder, proofing, importer wizard, dan analytics;
- responsif 390/768/1024/1440+, touch target, keyboard navigation, skip link, focus management, aria-live, dan screen-reader semantics;
- loading/skeleton, empty, no-access, validation, conflict, offline/retry, partial failure, success, destructive confirmation, and long-running job states;
- visual regression acceptance dan screenshot matrix.

Spesifikasi lengkap ditambahkan pada `13-SPESIFIKASI-UIUX-JURNAL-MODERN.md`.

## Prioritas perbaikan

1. Bekukan checkout SVN lokal dan buat manifest file/hash tanpa reset/clean/pull.
2. Tandai semua evidence lama `UNVERIFIED_ON_CURRENT_CHECKOUT`, lalu re-run dari nol.
3. Reconcile keputusan `LampiranJurnal`, budget tabel, Hibernate DDL, Java target, dan source of truth.
4. Ubah mapping 905 field dari generik menjadi executable field mapping catalog.
5. Tambahkan katalog 28 menu, route, capability, dan layar.
6. Implementasikan design system dan vertical slice: public portal → submission → editor/reviewer → publish.
7. Baru lanjutkan plugin/integration long tail, migrasi penuh, performance, security, SIT, dan UAT.

## Kriteria dokumen dinyatakan konsisten

- satu checkout ID: Git commit/SVN revision + dirty manifest;
- satu environment fingerprint dan satu run ID per evidence;
- tidak ada dua artifact yang sama-sama disebut “terbaru”;
- semua angka tabel/row/hash memiliki command dan timestamp;
- requirement terbaru `LampiranJurnal` tercermin di seluruh dokumen;
- 134 tabel, 905 field, 45 plugin, 73 email, 28 menu, seluruh route, dan seluruh layar mempunyai ID serta status yang tidak ambigu;
- tidak ada `PASS` tanpa test output yang dapat direproduksi;
- UI/UX mempunyai acceptance visual, accessibility, responsif, dan usability yang terukur.
