# Spesifikasi UI/UX modern modul Jurnal eCampus

Dokumen ini adalah kontrak UI/UX untuk servlet `Jurnal.java`, public portal, workspace author/reviewer/editor, dan 28 halaman administrasi. Implementasi berada di `C:\opt\AIS\ais\src\main\webapp\WEB-INF\baru\modul\jurnal\*` dan mengikuti shell Pustaka hanya pada header/footer/theme yang aman.

## Sasaran pengalaman

- Terlihat sebagai produk jurnal ilmiah modern, bukan CRUD hasil generator.
- Satu navigasi konsisten untuk publik, author, reviewer, editor, dan admin.
- Tugas paling penting terlihat dalam satu layar: apa yang menunggu, tenggat, risiko, dan aksi berikutnya.
- Tabel besar tetap cepat, dapat dicari, difilter, dipaging, dan digunakan tanpa horizontal scroll pada mobile.
- Seluruh tombol yang disembunyikan oleh UI tetap dilindungi authorization di service.
- Bahasa Indonesia sebagai default, bahasa Inggris setara, dan metadata multibahasa jelas.

## Design system

### Token

- Primary: indigo/blue ilmiah; accent: teal; gunakan warna tenant hanya melalui validated theme token.
- Status: draft abu-biru, menunggu amber, review ungu, diterima hijau, ditolak merah, published teal, overdue merah tua.
- Background bertingkat: page, surface, elevated, subtle; semua memiliki rasio kontras minimum WCAG 2.2 AA.
- Tipografi: system font stack yang tersedia lokal; skala 12/14/16/20/24/32; body 16 px publik dan minimal 14 px admin.
- Spacing berbasis 4 px; radius 8/12/16; focus ring 2 px yang tidak hanya mengandalkan warna.
- Touch target minimum 44x44 px; icon wajib mempunyai label/tooltip/accessible name.

### Komponen wajib

- app shell, journal switcher, global search, command palette, breadcrumbs;
- KPI card, alert/SLA card, activity timeline, kanban queue;
- responsive data table dengan column priority dan mobile card mode;
- filter drawer, saved view, bulk-action bar, pagination server-side;
- stepper/wizard, autosave indicator, revision comparison, file version list;
- user/avatar chip, role/stage badge, anonymity-safe identity placeholder;
- modal hanya untuk keputusan pendek; drawer untuk edit cepat; halaman penuh untuk workflow kompleks;
- toast untuk hasil singkat, inline error untuk field, error summary di atas form;
- skeleton loading, empty state dengan CTA, no-access state, retry state, partial-failure state;
- destructive confirmation dengan nama objek dan alasan wajib untuk reject/retract/delete.

Jangan menambah framework front-end besar tanpa audit dependency. Gunakan HTML semantik, CSS variables, progressive enhancement JavaScript, dan komponen existing AIS yang benar-benar reusable. Jangan menyalin CSS/JS/aset OJS.

## Arsitektur informasi

### Portal publik

1. Landing multi-journal.
2. Beranda jurnal.
3. Edisi terkini.
4. Arsip edisi.
5. Daftar artikel dan pencarian lanjutan.
6. Detail artikel dengan metadata, contributor, citation, metrics, galley, related content.
7. Detail edisi dan table of contents.
8. Tentang jurnal: scope, editorial board, policy, author guideline, ethics, fees.
9. Pengumuman dan halaman statis.
10. Login/daftar/invitation acceptance.
11. Subscription/access/payment status.
12. OAI/feed/sitemap/citation/export endpoint yang tampil konsisten saat diakses manusia.

### Workspace pengguna

- Author: ringkasan submission, draft, revision request, message, file, timeline, publication result.
- Reviewer: invitation, conflict declaration, guideline, blind file viewer, review form, recommendation, deadline.
- Editor/Section Editor: triage, assignment, reviewer search, review rounds, decision, discussion, SLA.
- Copyeditor/Production/Proofreader: task queue, revision compare, query, proof approval, galley readiness.
- Journal Manager/Admin: konfigurasi, users/roles, templates, access, integrations, import, audit, health.

## Katalog 28 submenu admin canonical

| Child | Label | Route canonical | Capability utama |
|---:|---|---|---|
| 460501 | Dashboard Jurnal | `/jurnal/admin/dashboard` | `menu.dashboard.read` |
| 460502 | Identitas Jurnal | `/jurnal/admin/journals` | `journal.manage` |
| 460503 | Kebijakan & Alur Kerja | `/jurnal/admin/workflow-settings` | `workflow.configure` |
| 460504 | Bagian, Kategori & Kosakata | `/jurnal/admin/taxonomy` | `taxonomy.manage` |
| 460505 | Pengguna, Peran & Undangan | `/jurnal/admin/people` | `people.manage` |
| 460506 | Penugasan Editor | `/jurnal/admin/editor-assignments` | `editor.assign` |
| 460507 | Semua Naskah | `/jurnal/admin/submissions` | `submission.read` |
| 460508 | Pemeriksaan Awal | `/jurnal/admin/screening` | `submission.screen` |
| 460509 | Reviewer & Minat Keahlian | `/jurnal/admin/reviewers` | `reviewer.manage` |
| 460510 | Penugasan & Putaran Review | `/jurnal/admin/review-assignments` | `review.assign` |
| 460511 | Formulir Review | `/jurnal/admin/review-forms` | `review.form.manage` |
| 460512 | Keputusan Editorial | `/jurnal/admin/decisions` | `decision.manage` |
| 460513 | Diskusi Editorial | `/jurnal/admin/discussions` | `discussion.manage` |
| 460514 | Copyediting | `/jurnal/admin/copyediting` | `copyedit.manage` |
| 460515 | Produksi & Proof | `/jurnal/admin/production` | `production.manage` |
| 460516 | Edisi & Daftar Isi | `/jurnal/admin/issues` | `issue.manage` |
| 460517 | Publikasi, Galley & Versi | `/jurnal/admin/publications` | `publication.manage` |
| 460518 | DOI, URN & Deposit | `/jurnal/admin/identifiers` | `identifier.manage` |
| 460519 | Portal, Navigasi & Halaman | `/jurnal/admin/portal` | `portal.manage` |
| 460520 | Pengumuman & Sorotan | `/jurnal/admin/announcements` | `announcement.manage` |
| 460521 | Template Email & Notifikasi | `/jurnal/admin/communications` | `communication.manage` |
| 460522 | Langganan, Institusi & IP | `/jurnal/admin/subscriptions` | `subscription.manage` |
| 460523 | Pembayaran & Rekonsiliasi | `/jurnal/admin/payments` | `payment.manage` |
| 460524 | Statistik & COUNTER | `/jurnal/admin/statistics` | `statistics.read` |
| 460525 | Laporan & Ekspor | `/jurnal/admin/reports` | `report.export` |
| 460526 | Integrasi & Paritas Plugin | `/jurnal/admin/integrations` | `integration.manage` |
| 460527 | Import OJS | `/jurnal/admin/import-ojs` | `import.manage` |
| 460528 | Audit, Job & Kesehatan Sistem | `/jurnal/admin/operations` | `operations.read` |

`Menu.id` tetap primary key terpisah. Label dapat dilokalkan, tetapi route dan capability key tidak boleh bergantung pada label.

## Spesifikasi layar utama

### Dashboard per peran

- Header: journal switcher, periode, saved view, last refresh.
- KPI hanya yang relevan: draft, screening, awaiting reviewer, overdue, revision, ready to publish.
- “Pekerjaan Saya” sebagai queue prioritas, bukan angka dekoratif.
- Timeline aktivitas dan SLA risk panel.
- Editor melihat workload reviewer dan bottleneck; author hanya melihat submission miliknya; reviewer tidak melihat identitas yang dibutakan.

### Submission wizard

Langkah: mulai → checklist/consent → metadata → contributor/affiliation/CRediT → files → references → review & submit.

- Autosave debounced dan indikator “tersimpan/waiting/error”.
- Draft dapat dilanjutkan lintas perangkat.
- Validasi per langkah serta error summary.
- Upload resumable bila infrastruktur memungkinkan; selalu tampilkan progress, checksum state, MIME, ukuran, dan versi.
- Metadata multibahasa memakai tab locale, dengan indikator kelengkapan.
- Review akhir menampilkan preview persis data yang akan diserahkan.

### Editorial workspace

- Desktop: tiga panel—queue, detail naskah, action/timeline.
- Tablet/mobile: panel menjadi route/tab berurutan, tidak dipaksa tiga kolom.
- Stage rail: Submission → Review → Copyediting → Production → Publication.
- Keputusan selalu meminta reason, template preview, recipient preview, dan confirmation.
- Multi-round review ditampilkan sebagai timeline terkelompok; file harus jelas berasal dari round/stage/version mana.

### Reviewer workspace

- Sebelum accept: title/abstract yang diizinkan, deadline, conflict declaration, policy.
- Setelah accept: blind file viewer, structured review form, comment for author/editor, recommendation.
- Autosave dan countdown deadline; warning sebelum submit final.
- Semua label file, metadata DOM, URL, download filename, log client, dan notification harus lolos blindness policy.

### Issue builder

- Daftar artikel eligible, search/filter, drag handle hanya sebagai enhancement; tombol move up/down tetap tersedia untuk keyboard.
- Preview cover, volume/number/year/title, publication date, sections, pagination, galley.
- Readiness checklist DOI, metadata, file, license, contributor, and proof.

### Import OJS wizard

Langkah: pilih connection reference → preflight → scope → mapping identity/role → file scan → dry-run → konflik → execute → reconciliation.

- Browser tidak menerima password database atau `files_dir` arbitrary.
- Progress menampilkan table/row/file, rate, ETA, checkpoint, warning, dan error sample.
- Konflik memiliki pilihan link/merge/external/skip dengan preview before-after.
- Cancel menunjukkan apakah hanya menghentikan batch atau melakukan compensation.
- Final report dapat diunduh dan memiliki checksum/run ID.

## Responsive behavior

| Lebar | Perilaku |
|---|---|
| 1440+ | sidebar expanded, split view, data density comfortable/compact |
| 1024–1439 | sidebar collapsible, dua panel, filter drawer |
| 768–1023 | navigation rail, satu panel utama, secondary content tab/drawer |
| 390–767 | bottom navigation untuk tugas utama, cards mengganti tabel, sticky primary action |
| <390 | tetap usable tanpa horizontal page scroll; metadata panjang wrap/truncate dengan expand |

Tabel boleh scroll horizontal hanya di dalam container terkontrol untuk data teknis yang tidak dapat diubah menjadi card; halaman tidak boleh ikut melebar.

## Accessibility dan usability acceptance

- WCAG 2.2 AA; keyboard-only seluruh journey; visible focus; skip link.
- Heading hierarchy, landmarks, label/help/error association, `aria-live` untuk autosave/job progress.
- Modal/drawer mengunci dan mengembalikan focus dengan benar.
- Status tidak dibedakan hanya dengan warna; badge memiliki text/icon.
- Grafik mempunyai summary dan tabel data alternatif.
- Bahasa halaman dan perubahan locale diumumkan.
- Uji zoom 200%, reflow 320 CSS px, reduced motion, high contrast.
- Usability test minimal 2 orang per peran utama: author, reviewer, editor, admin. Catat completion rate, time-on-task, error, dan komentar.

## Performance UX

- Semua list server-side pagination; default 20, pilihan 20/50/100 dengan batas.
- Search debounce 300–500 ms dan dapat dibatalkan; query lama tidak boleh menimpa hasil baru.
- Initial shell cepat; lazy-load chart, viewer, dan panel berat.
- Skeleton hanya untuk layout yang diketahui; spinner global tidak boleh menutupi seluruh layar untuk aksi kecil.
- Job panjang selalu async, resumable, dan dapat dipantau ulang setelah refresh.

## Visual acceptance matrix

Setiap layar inti wajib memiliki screenshot atau visual regression pada:

- 1440x900 desktop;
- 1024x768 tablet landscape;
- 768x1024 tablet portrait;
- 390x844 mobile;
- tema default tenant dan satu tema warna ekstrem;
- data normal, empty, loading, validation error, server error, no-access, dan long-content state.

Tidak boleh ada clipped text, overlapping controls, horizontal page scroll, invisible focus, layout shift besar, atau tombol primary ganda yang membingungkan.

## Definition of Done UI/UX

UI tidak selesai hanya karena JSP berhasil dirender. Selesai bila:

1. route dan permission negative test lulus;
2. seluruh state responsif dan accessibility lulus;
3. role journey selesai tanpa masuk halaman CRUD generik;
4. visual regression disetujui;
5. tidak ada business query/mutation di JSP;
6. error tidak membocorkan stack trace, SQL, path, token, atau reviewer identity;
7. p95 dan query count memenuhi target yang dicatat;
8. pengguna UAT dapat menyelesaikan tugas tanpa bantuan pengembang.
