# Spesifikasi UI/UX Master CRUD Generik AIS

## 1. Prinsip desain

UI Generic CRUD harus terasa sebagai satu produk enterprise yang konsisten, bukan kumpulan form hasil reflection. Sasaran utamanya:

- pengguna cepat menemukan data;
- status filter dan scope selalu terlihat;
- operasi berisiko mempunyai konteks, preview, dan konfirmasi;
- tampilan tetap efektif pada layar desktop, laptop, tablet, dan mobile;
- privilege menentukan apa yang terlihat dan apa yang dapat dieksekusi;
- loading, empty, error, conflict, dan partial-success diperlakukan sebagai state utama;
- field dan action khusus domain dapat ditambahkan tanpa merusak layout generik.

Pertahankan shell New UI existing: sidebar, topbar, search global, content, dan footer. Generic CRUD menambahkan komponen terstandardisasi di dalam `.nui-content`.

## 2. Anatomi halaman desktop

```text
┌─────────────────────────────────────────────────────────────────────────┐
│ Breadcrumb                                     Help / Refresh / More    │
│ Judul Master Data                              [Tambah] [Export]         │
│ Deskripsi, scope aktif, last refresh                                    │
├─────────────────────────────────────────────────────────────────────────┤
│ KPI ringan opsional: Total | Aktif | Tidak Aktif | Perlu Tindakan       │
├─────────────────────────────────────────────────────────────────────────┤
│ Search utama | Quick filter 1 | Quick filter 2 | Filter Lanjutan        │
│ Active filter chips                          Saved View | Reset          │
├─────────────────────────────────────────────────────────────────────────┤
│ Selection bar (hanya saat ada pilihan)                                  │
├───────────────────────────────────────────────┬─────────────────────────┤
│ Data table                                    │ Detail drawer opsional  │
│                                               │                         │
├───────────────────────────────────────────────┴─────────────────────────┤
│ 1–10 dari 12.458 | page size | pagination | refresh state              │
└─────────────────────────────────────────────────────────────────────────┘
```

### Page header

Wajib berisi:

- breadcrumb;
- nama entity dalam bahasa pengguna;
- deskripsi singkat;
- scope aktif, misalnya perguruan tinggi/sekolah/toko;
- last refresh dan stale indicator;
- primary action `Tambah Data` hanya jika CREATE;
- grouped export menu hanya jika READ;
- upload XLSX hanya jika CREATE+UPDATE+DELETE;
- help/context menu.

Jangan menaruh terlalu banyak tombol sejajar. Prioritas:

1. primary action;
2. satu secondary action penting;
3. sisanya pada split button/overflow.

## 3. KPI cards

KPI bukan wajib untuk semua entity. Tampilkan hanya jika query count murah atau adapter menyediakan agregat. Maksimal 4–6 card:

- total record sesuai scope;
- aktif/nonaktif;
- incomplete/data quality;
- pending approval;
- custom domain KPI.

KPI harus clickable hanya jika menghasilkan filter yang jelas. Jangan menjalankan agregasi berat setiap keystroke filter.

## 4. Filter bar

### 4.1 Search utama

- Placeholder menyebut field yang benar-benar dicari.
- Debounce 350 ms untuk dataset kecil/terindeks; tombol Cari untuk query mahal.
- `Esc` membersihkan search saat fokus.
- Clear button terlihat ketika ada nilai.
- Search tidak boleh diam-diam mencari seluruh relasi.

### 4.2 Quick filters

Pilih 3–6 field paling sering dipakai berdasarkan konfigurasi, misalnya:

```text
Kode/Nama | Status | Unit/Program | Tahun/Angkatan | Aktif
```

Filter tanggal menggunakan range picker. Boolean menggunakan Semua/Ya/Tidak. Relation mengikuti combo/bandbox threshold.

### 4.3 Advanced filter dialog

Dialog mempunyai:

- header dan jumlah filter aktif;
- kategori/section field;
- search nama field jika sangat banyak;
- operator sesuai tipe;
- reset section;
- Cancel dan Terapkan;
- footer sticky;
- keyboard focus trap;
- responsive full-screen di mobile.

### 4.4 Active filter chips

Setelah dialog ditutup, kondisi tetap terlihat:

```text
Status: Aktif ×   Angkatan: 2024 ×   Program Studi: Gizi ×
```

Untuk range:

```text
Tanggal dibuat: 01 Jan 2026 – 31 Jan 2026 ×
```

Jika nilai sangat panjang, truncate visual tetapi full text tersedia pada tooltip/accessible label.

### 4.5 Saved views

User dapat menyimpan kombinasi:

- filter;
- sort;
- visible columns;
- order/width/pinned columns;
- density;
- page size.

UI mendukung:

- Simpan view baru;
- Perbarui view;
- Jadikan default;
- Duplikasi;
- Hapus;
- Shared view jika mempunyai privilege.

## 5. Data table desktop

### 5.1 Header

- Sticky header ketika halaman panjang.
- `aria-sort` pada kolom sortable.
- indikator ASC/DESC jelas;
- tooltip untuk label panjang;
- resize handle tidak mengganggu click sort;
- column menu: sort, hide, pin, filter by column.

### 5.2 Kolom

Urutan default:

1. selection checkbox jika bulk action tersedia;
2. Foto jika ada;
3. identifier/kode;
4. nama/deskripsi utama;
5. relation/status utama;
6. tanggal/common field;
7. Aksi.

Foto dan Aksi tidak sortable. Semua kolom lain hanya diberi affordance sort bila backend mendukung.

### 5.3 Rows

- Hover subtle;
- selected row jelas;
- row click membuka detail drawer, tetapi link/action tetap dapat digunakan tanpa konflik;
- status memakai badge + teks, bukan warna saja;
- nilai kosong ditampilkan `—`, bukan `null`;
- data sensitif dimasking;
- row yang stale/bermasalah mempunyai indikator dan tooltip.

### 5.4 Action column

Maksimal 1–2 ikon langsung, sisanya overflow. Rekomendasi:

- Edit langsung bila UPDATE;
- More (`⋮`) untuk Detail, Audit, Duplicate, Delete, custom actions.

Action destructive berwarna merah hanya di menu/dialog konfirmasi, bukan memenuhi tabel.

### 5.5 Selection bar

Muncul menggantikan toolbar ketika row dipilih:

```text
3 data dipilih  | Pilih semua 12.458 hasil filter | Edit Massal | Export | Hapus | Lainnya
```

“All filtered” menggunakan server selection token, bukan semua ID di browser.

### 5.6 Pagination

Desktop:

```text
Menampilkan 1–10 dari 12.458 data
[5|10|25|50|100|500|1000 per halaman]
[‹] [1] [2] [3] […] [1.246] [›]
```

- default 10;
- page-size tersimpan per user+role+entity;
- saat filter berubah, kembali ke page 1;
- jika current page tidak lagi valid setelah delete, pindah ke page valid terakhir;
- loading hanya pada table region, tidak memutihkan seluruh aplikasi.

## 6. Mobile list/card view

Pada lebar kecil, jangan memaksakan tabel 10 kolom. Ubah menjadi card:

```text
┌────────────────────────────────────────────┐
│ [Foto] Nama Lengkap                [Aktif] │
│        Kode/NIM: 2412010001                │
│        Program: S1 Gizi                    │
│        Diperbarui: 18 Mei 2026             │
│ [Detail] [Edit] [⋮]                        │
└────────────────────────────────────────────┘
```

Ketentuan:

- hanya 3–5 field utama;
- field tambahan di expand/detail;
- sticky search/filter bar;
- floating/anchored `Tambah` bila CREATE;
- row action melalui bottom sheet;
- selection mode diaktifkan dengan checkbox/long-press yang tetap accessible;
- previous/next pagination, page size pada filter sheet;
- filter chips horizontal scroll yang tidak menyembunyikan nilai.

## 7. Detail drawer

Detail drawer desktop 360–480 px atau proporsional, mobile full-screen. Isi:

- header: photo/icon, title, identifier, status;
- tabs: Ringkasan, Data Lengkap, Relasi, Dokumen, Audit, Riwayat/Approval;
- key-value rows;
- action footer sesuai privilege;
- close button dan keyboard Escape;
- deep-link opsional dengan ID opaque/validated.

Jangan lazy-load association tanpa service; tiap tab memanggil endpoint terkontrol.

## 8. Add/Edit form

### 8.1 Form container

- Desktop: drawer besar 760–960 px untuk form biasa; full-page untuk object sangat kompleks.
- Tablet: 2 kolom.
- Mobile: full-screen 1 kolom.
- Header sticky: Tambah/Edit + entity + status draft.
- Footer sticky: Batal, Simpan, Simpan & Tambah Lagi, Kirim Approval.

### 8.2 Sections

Gunakan section/collapsible group, contoh:

```text
Informasi Utama
Klasifikasi dan Relasi
Alamat/Kontak
Status dan Periode
Foto/Dokumen
Catatan
Informasi Sistem (read-only)
```

Section dengan error otomatis terbuka.

### 8.3 Field layout

- label di atas input;
- required marker konsisten;
- help text singkat;
- unit/suffix jelas;
- width mengikuti tipe, bukan semua full width;
- textarea untuk teks panjang;
- date/time picker sesuai locale;
- toggle hanya untuk boolean langsung, bukan pilihan lebih dari dua.

### 8.4 Validation states

Error summary di atas form:

```text
Terdapat 3 kesalahan. Periksa field berikut:
• Nama wajib diisi
• Kode sudah digunakan
• Program Studi tidak berada dalam scope Anda
```

Klik error memfokuskan field. Nilai valid dipertahankan. Server error tidak hanya ditampilkan sebagai toast.

### 8.5 Unsaved changes dan conflict

- Tutup/batal dengan perubahan meminta konfirmasi.
- Saat versi record berubah, tampilkan conflict dialog:
  - data Anda;
  - data terbaru;
  - field berbeda;
  - Muat Ulang / Simpan sebagai Draft / Override hanya jika diizinkan.

## 9. Relation picker

### 9.1 Combo <=20

- searchable select ringan;
- label + kode/identifier;
- clear bila optional;
- list tidak memuat item di luar scope.

### 9.2 Bandbox >20

Input menampilkan selected label dan tombol buka dialog. Dialog:

```text
Cari kode/nama/NIM...
[Quick filters] [Filter Lanjutan]
Active chips
------------------------------------------------
Kode | Nama | Status | Relasi Utama
------------------------------------------------
Pagination 10 per halaman
[Batal] [Pilih]
```

- single/multi select sesuai field;
- nilai existing ditandai;
- exact match dan fuzzy/contains ditata jelas;
- tidak menampilkan property yang tidak mapped;
- nested lookup mempunyai breadcrumb dan max depth.

## 10. Photo editor

UI foto:

- current photo/fallback avatar;
- Upload File;
- Ambil Foto bila didukung;
- crop rectangle/circle sesuai konteks;
- zoom/rotate;
- preview hasil;
- validasi ukuran/type;
- Restore/Remove bila rule mengizinkan;
- privacy notice singkat.

Upload progress dan error harus jelas. Jangan menampilkan path filesystem.

## 11. Import XLSX wizard

### Step 1 — Persiapan

- penjelasan operasi Create/Update/Delete;
- warning `delete=true`;
- tombol download template;
- versi template dan timestamp;
- pilihan update mode bila adapter mendukung.

### Step 2 — Upload

- drag-drop/file picker;
- hanya XLSX;
- ukuran maksimum;
- progress;
- checksum/idempotency warning.

### Step 3 — Validasi/Dry-run

KPI:

```text
Create 120 | Update 35 | Delete 4 | Skip 8 | Error 12
```

Table preview dapat difilter per status. Tampilkan row Excel, key, operation, message. Error workbook dapat diunduh.

### Step 4 — Konfirmasi

- ringkasan operasi;
- scope/role aktif;
- warning delete;
- checkbox confirmation untuk destructive job;
- estimasi waktu;
- opsi notifikasi selesai.

### Step 5 — Progress/Result

- job status;
- processed/total;
- success/error;
- cancel jika aman;
- download result/error;
- link audit/job detail.

## 12. Export dialog

Pilihan format:

```text
XLSX | PDF | DOCX | PPTX
```

Opsi:

- Semua hasil filter / page saat ini / selected rows;
- Default columns / visible columns / custom columns;
- Include filter summary;
- orientation/size untuk PDF/DOCX;
- template PPTX ringkasan/data;
- file name preview;
- synchronous untuk kecil, background job untuk besar.

Sebelum submit, tampilkan jumlah record yang diperkirakan dan policy limit.

## 13. Column chooser

Dialog dua panel:

```text
Kolom tersedia       Kolom ditampilkan
[search]              [drag reorder]
[checkboxes]           [pin/width]
```

Fitur:

- show/hide;
- reorder;
- pin;
- width;
- reset;
- density;
- save as view;
- perubahan preview langsung tetapi baru dipersist setelah Apply.

Kolom Foto/Aksi mempunyai aturan khusus dan tidak dapat dijadikan sortable.

## 14. Approval/reject

Detail/action bar menampilkan status workflow. Dialog approval:

- objek dan key;
- transition sebelum → sesudah;
- summary perubahan;
- komentar;
- attachment bila adapter mendukung;
- confirm.

Reject:

- alasan wajib bila domain mengharuskan;
- template alasan opsional;
- informasi konsekuensi;
- tidak boleh hanya popup “Yakin?”.

## 15. Custom actions

Custom action ditempatkan berdasarkan scope:

- toolbar: tidak membutuhkan selection;
- selection bar: bulk;
- row overflow: single row;
- detail drawer: object context;
- form footer: sebelum/sesudah save.

Action yang mempunyai parameter membuka schema-driven dialog. Action panjang menjadi job dan tidak memblokir UI.

## 16. Empty, loading, error, offline

### Empty state tanpa filter

- ikon sederhana;
- “Belum ada data”;
- explanation;
- Tambah Data jika CREATE;
- Import bila memenuhi privilege.

### Empty state akibat filter

- “Tidak ada data yang cocok”;
- ringkasan filter;
- Reset Filter.

### Loading

- skeleton row/card;
- loading bar region;
- hindari spinner tanpa konteks;
- tombol submit disabled dan menunjukkan progress.

### Error

- message manusiawi;
- request ID;
- Retry;
- Detail teknis hanya untuk user berwenang;
- state lama dipertahankan bila aman.

### Offline/connection failure

Untuk aplikasi desktop/mobile wrapper, tampilkan connection state. Mutasi generic tidak boleh secara otomatis diulang tanpa idempotency key.

## 17. Visual system

- Font: gunakan stack existing (`Inter`, `Segoe UI`, `Roboto`, Arial) tanpa wajib CDN.
- Base spacing: 4/8 px rhythm.
- Radius: 10–16 px; dialog/card konsisten.
- Shadow ringan, bukan berlebihan.
- Warna primary existing; status mengikuti semantic colors.
- Data density dapat dipilih compact/comfortable.
- Ikon harus memiliki label/tooltip; jangan bergantung pada emoji yang berbeda antar OS.
- Gunakan SVG/icon set lokal existing bila tersedia.

## 18. Accessibility checklist

- Semua input mempunyai label terasosiasi.
- Icon-only button mempunyai accessible name.
- Focus visible.
- Tab order logis.
- Modal focus trap dan restore focus.
- Escape menutup dialog non-destructive.
- Header sort keyboard-operable dan `aria-sort`.
- Table/card tetap terbaca pada zoom 200–400%.
- Status mempunyai teks, tidak hanya warna.
- Error summary dan inline error terhubung dengan `aria-describedby`.
- Toast tidak menjadi satu-satunya cara menyampaikan error penting.
- Touch targets cukup besar dan berjarak.

## 19. Performance UX

- Debounce search.
- Batalkan request lama ketika query baru berjalan.
- Gunakan request sequence agar response lama tidak menimpa yang baru.
- Cache metadata ringan per entity/role, tetapi invalidasi saat role/config berubah.
- Lazy-load detail tabs dan relation lookup.
- Jangan render ribuan option DOM.
- Virtual scrolling hanya jika benar-benar perlu; paging tetap default.
- Export/import besar menjadi job.
- Tampilkan estimasi record dan progress nyata bila tersedia.

## 20. Acceptance visual

Sebuah entity dianggap berhasil dimigrasikan bila:

- tidak ada horizontal overflow pada 1366 px untuk kolom default;
- mobile 360 px tetap dapat mencari, filter, melihat data, add/edit, dan menjalankan action;
- filter aktif selalu terlihat setelah popup ditutup;
- dialog tidak terpotong;
- keyboard-only dapat menjalankan fungsi utama;
- loading/error/empty/conflict state tersedia;
- layout entity sederhana dan kompleks tetap serasi;
- privilege tidak meninggalkan ruang kosong atau tombol mati yang membingungkan;
- custom actions tetap tertata dalam overflow/selection bar.



# Amandemen UI/UX V2.1

## 21. Audit Trail Center

Entry point:

- toolbar `Audit Seluruh Data`;
- row overflow `Riwayat Perubahan`;
- tab `Audit` pada detail drawer.

Audit global menjadi full-page; audit per-row menjadi drawer 520–760 px pada desktop dan full-screen pada mobile. Tampilkan timeline, table revision, before/after compare, active filter chips, request/actor/source, serta restore progress.

Aksi berisiko dipisahkan ke **Danger Zone**. `Hapus Data Ini` tidak boleh menjadi primary action. Dialog wajib menyebut bahwa row aktif akan dihapus, audit tetap ada, typed confirmation, reason, dan hasil FK preflight.

## 22. Ubah & Restore

Gunakan editor sesuai metadata field. Tampilkan:

```text
Nilai pada revisi
Nilai aktif
Nilai yang akan diterapkan
```

Historical revision tidak diedit. Setelah simpan, tampilkan revision baru dan link audit event.

## 23. Complex Add/Edit Form

Mode full-page/tabbed untuk form besar. Header/footer sticky, tab error badge, error summary lintas tab, conditional tabs, lazy content, save-before-enter, completeness indicator, dan audit/approval timeline.

Mahasiswa minimal mempunyai tab Data Mahasiswa, Pindahan, Alih Prodi, Biodata, Beasiswa, Cuti, Kelulusan, Alumni, Login Orang Tua, dan Audit. Pada mobile, tab menjadi horizontal scroll/stepper/select yang tetap accessible; tidak satu pun fungsi boleh hilang.
