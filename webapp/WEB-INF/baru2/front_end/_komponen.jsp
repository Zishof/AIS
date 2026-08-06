<%--
================================================================================
 _komponen.jsp — Pustaka Komponen UI Reusable Sistem Baru2
================================================================================

 TUJUAN
 -------
 File ini adalah sumber kebenaran tunggal (single source of truth) untuk seluruh
 komponen UI/UX yang digunakan di semua halaman front_end/*/. Setiap halaman
 modul cukup menyertakan file ini dengan:

     <%@ include file="../_komponen.jsp" %>

 Filosofi desain:
 - Zero-dependency selain Bootstrap 5, Font Awesome 6, Alpine.js 3, dan NProgress
   (sudah tersedia via _libs.jsp).
 - Setiap komponen adalah fungsi JavaScript murni yang menerima opsi konfigurasi,
   sehingga mudah dipakai ulang di berbagai konteks.
 - Komponen tidak menyimpan state global — state ada di Alpine.js x-data per
   instance, sehingga beberapa tabel dapat hidup berdampingan di satu halaman.
 - CSS menggunakan CSS Custom Properties (variabel) yang sudah didefinisikan
   di _libs.jsp / index.jsp sehingga perubahan tema cukup di satu tempat.
 - Responsif by default: mobile-first, dengan breakpoint Bootstrap standard
   (sm:576px, md:768px, lg:992px, xl:1200px).

 DAFTAR KOMPONEN
 ---------------
 1. AISTable        – Tabel data dengan AJAX, paginasi, sort, filter, aksi baris
 2. AISModal        – Modal dialog untuk form tambah/ubah/lihat detail
 3. AISFilter       – Panel filter collapsible (advanced search)
 4. AISToast        – Notifikasi toast (sukses, gagal, peringatan, info)
 5. AISConfirm      – Dialog konfirmasi sebelum tindakan destruktif
 6. AISBadge        – Badge / chip status berwarna
 7. AISForm         – Helper validasi dan serialisasi form
 8. AISUpload       – Komponen upload file dengan preview dan progress
 9. AISExport       – Tombol ekspor ke Excel / PDF dengan NProgress
 10. AISPageHeader  – Header halaman dengan breadcrumb, judul, tombol aksi

 KONVENSI PENAMAAN
 -----------------
 - Semua fungsi/class publik menggunakan prefix "AIS" untuk menghindari konflik
   dengan library lain.
 - Nama opsi menggunakan camelCase.
 - Nama CSS class menggunakan prefix "ais2-" untuk membedakan dari class ZKoss lama.

 CARA MENAMBAH KOMPONEN BARU
 ---------------------------
 1. Definisikan fungsi atau class JavaScript dengan JavaDoc minimal 10 baris.
 2. Tambahkan CSS-nya di blok <style> komponen ini (gunakan prefix ais2-).
 3. Tambahkan ke daftar di atas.
 4. Tidak perlu recompile Java — cukup simpan file ini dan Tomcat hot-reload JSP.

 VERSI   : 1.0.0
 TANGGAL : 2026-07-22
 PENULIS : Sistem Baru2 eCampus
================================================================================
--%>

<style>
/* ===================================================================
   CSS KOMPONEN BARU2
   =================================================================== */

/* ── Variabel tema (override Bootstrap defaults) ─────────────────── */
:root {
  --ais2-pri: #123d77;
  --ais2-pri2: #2563eb;
  --ais2-acc: #06b6d4;
  --ais2-navy: #082f5f;
  --ais2-navy-2: #0d447f;
  --ais2-grad: linear-gradient(135deg, #0b3d75 0%, #12669a 58%, #0f8fa7 100%);
  --ais2-ok: #10b981;
  --ais2-warn: #f59e0b;
  --ais2-bad: #ef4444;
  --ais2-info: #0ea5e9;
  --ais2-ink: #10243e;
  --ais2-ink2: #5b6b80;
  --ais2-ink3: #91a0b3;
  --ais2-line: #dfe7f1;
  --ais2-bg: #f2f6fc;
  --ais2-card: #ffffff;
  /* Alias kompatibilitas untuk JSP lama yang memakai nama token generasi awal. */
  --ais2-card-bg: var(--ais2-card);
  --ais2-border: var(--ais2-line);
  --ais2-text: var(--ais2-ink);
  --ais2-text-sec: var(--ais2-ink2);
  --ais2-radius: 14px;
  --ais2-radius-sm: 9px;
  --ais2-shadow: 0 1px 2px rgba(15,39,71,.04), 0 8px 24px rgba(15,39,71,.055);
  --ais2-shadow-md: 0 10px 28px rgba(15,39,71,.10);
  --ais2-shadow-lg: 0 20px 50px rgba(15,39,71,.16);
  --ais2-font: "Plus Jakarta Sans", "Segoe UI", system-ui, -apple-system, sans-serif;
  --ais2-transition: .18s cubic-bezier(.4,0,.2,1);
}

/* ── Reset ─────────────────────────────────────────────────────────── */
.ais2-page * { box-sizing: border-box; }
.ais2-page {
  font-family: var(--ais2-font); color: var(--ais2-ink);
  background: var(--ais2-bg); min-height: 100vh; line-height: 1.55;
  -webkit-font-smoothing: antialiased; text-rendering: optimizeLegibility;
}
.ais2-page :where(a,button,input,select,textarea,[tabindex]):focus-visible {
  outline: 3px solid rgba(37,99,235,.32); outline-offset: 2px;
}
.ais2-page :where(button,.ais2-btn,.ais2-tab,.ais2-page-btn) { min-height: 40px; }
.ais2-page ::selection { background: #bfdbfe; color: #0b2f5b; }

/* ── Page Header ────────────────────────────────────────────────────── */
.ais2-page-header {
  background: var(--ais2-card);
  border: 1px solid var(--ais2-line);
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 18px;
  border-radius: var(--ais2-radius);
  box-shadow: var(--ais2-shadow);
}
.ais2-page-header .ais2-ph-icon {
  width: 44px; height: 44px; border-radius: 12px;
  background: var(--ais2-grad); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 19px; flex-shrink: 0;
}
.ais2-page-header .ais2-ph-title {
  font-size: 18px; font-weight: 800; color: var(--ais2-ink); line-height: 1.2;
}
.ais2-page-header .ais2-ph-sub {
  font-size: 12px; color: var(--ais2-ink2); margin-top: 2px;
}
.ais2-page-header .ais2-ph-actions {
  margin-left: auto; display: flex; gap: 8px; flex-wrap: wrap;
}
.ais2-breadcrumb { font-size: 12px; color: var(--ais2-ink3); display: flex; gap: 5px; align-items: center; }
.ais2-breadcrumb a { color: var(--ais2-pri2); text-decoration: none; }
.ais2-breadcrumb a:hover { text-decoration: underline; }
.ais2-breadcrumb .sep { color: var(--ais2-ink3); }

/* ── Tombol Standar ─────────────────────────────────────────────────── */
.ais2-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 9px 16px; border-radius: var(--ais2-radius-sm);
  font-size: 13px; font-weight: 700; font-family: var(--ais2-font);
  cursor: pointer; border: none; transition: var(--ais2-transition);
  white-space: nowrap; line-height: 1;
}
.ais2-btn-primary { background: #2563eb; color: #fff; box-shadow: 0 4px 12px rgba(37,99,235,.18); }
.ais2-btn-primary:hover { opacity: .9; transform: translateY(-1px); box-shadow: var(--ais2-shadow-md); }
.ais2-btn-outline { background: #fff; color: var(--ais2-pri2); border: 1.5px solid var(--ais2-pri2); }
.ais2-btn-outline:hover { background: #f0f4ff; }
.ais2-btn-danger { background: #fee2e2; color: var(--ais2-bad); }
.ais2-btn-danger:hover { background: var(--ais2-bad); color: #fff; }
.ais2-btn-success { background: #dcfce7; color: #16a34a; }
.ais2-btn-success:hover { background: #16a34a; color: #fff; }
.ais2-btn-sm { padding: 5px 10px; font-size: 11.5px; }
.ais2-btn-icon { width: 32px; height: 32px; padding: 0; justify-content: center; }
.ais2-btn:disabled { opacity: .5; cursor: not-allowed; transform: none !important; }

/* ── Filter Card ────────────────────────────────────────────────────── */
.ais2-filter-card {
  background: var(--ais2-card); border: 1px solid var(--ais2-line);
  border-radius: var(--ais2-radius); padding: 14px 18px;
  margin-bottom: 16px; box-shadow: var(--ais2-shadow);
}
.ais2-filter-head {
  display: flex; align-items: center; gap: 10px; margin-bottom: 12px;
}
.ais2-filter-title { font-size: 13px; font-weight: 800; color: var(--ais2-ink2); flex: 1; }
.ais2-filter-row { display: flex; flex-wrap: wrap; gap: 10px; align-items: flex-end; }
.ais2-filter-field { display: flex; flex-direction: column; gap: 4px; min-width: 160px; flex: 1; }
.ais2-filter-field label { font-size: 11.5px; font-weight: 700; color: var(--ais2-ink2); }
.ais2-filter-field input, .ais2-filter-field select {
  padding: 8px 11px; border: 1.5px solid var(--ais2-line);
  border-radius: var(--ais2-radius-sm); font-size: 13px;
  font-family: var(--ais2-font); color: var(--ais2-ink);
  background: var(--ais2-bg); transition: var(--ais2-transition); outline: none;
}
.ais2-filter-field input:focus, .ais2-filter-field select:focus {
  border-color: var(--ais2-pri2); background: #fff;
  box-shadow: 0 0 0 3px rgba(79,70,229,.1);
}
.ais2-filter-advanced { display: none; margin-top: 12px; padding-top: 12px; border-top: 1px dashed var(--ais2-line); }
.ais2-filter-advanced.show { display: block; }
.ais2-filter-checks { display: flex; flex-wrap: wrap; gap: 8px 16px; }
.ais2-filter-check { display: flex; align-items: center; gap: 6px; font-size: 12.5px; cursor: pointer; }
.ais2-filter-check input[type=checkbox] { width: 15px; height: 15px; accent-color: var(--ais2-pri2); cursor: pointer; }

/* ── Data Table ─────────────────────────────────────────────────────── */
.ais2-table-card {
  background: var(--ais2-card); border: 1px solid var(--ais2-line);
  border-radius: var(--ais2-radius); box-shadow: var(--ais2-shadow); overflow: hidden;
}
.ais2-table-toolbar {
  display: flex; align-items: center; gap: 10px; padding: 12px 16px;
  border-bottom: 1px solid var(--ais2-line); flex-wrap: wrap;
}
.ais2-table-count { font-size: 12.5px; color: var(--ais2-ink2); margin-left: auto; }
.ais2-table-wrap { overflow-x: auto; }
.ais2-tbl {
  width: 100%; border-collapse: collapse; font-size: 13px;
}
.ais2-tbl thead tr { background: var(--ais2-bg); }
.ais2-tbl thead th {
  padding: 10px 12px; font-size: 12px; font-weight: 700;
  color: var(--ais2-ink2); text-align: left; border-bottom: 2px solid var(--ais2-line);
  white-space: nowrap; user-select: none;
}
.ais2-tbl thead th.sortable { cursor: pointer; }
.ais2-tbl thead th.sortable:hover { color: var(--ais2-pri2); }
.ais2-tbl thead th .sort-ic { font-size: 9px; margin-left: 4px; opacity: .4; }
.ais2-tbl thead th.asc .sort-ic, .ais2-tbl thead th.desc .sort-ic { opacity: 1; color: var(--ais2-pri2); }
.ais2-tbl tbody tr {
  border-bottom: 1px solid var(--ais2-line); transition: background var(--ais2-transition);
}
.ais2-tbl tbody tr:last-child { border-bottom: none; }
.ais2-tbl tbody tr:hover { background: #f8f9ff; }
.ais2-tbl td { padding: 10px 12px; color: var(--ais2-ink); vertical-align: middle; }
.ais2-tbl td.nowrap { white-space: nowrap; }
.ais2-tbl .td-actions { display: flex; gap: 5px; justify-content: flex-start; flex-wrap: wrap; }
.ais2-tbl .no-data {
  text-align: center; padding: 48px 24px; color: var(--ais2-ink3);
}
.ais2-tbl .no-data i { font-size: 32px; margin-bottom: 10px; display: block; }
/* Responsive: sembunyikan kolom opsional di mobile */
@media (max-width: 767px) {
  .ais2-tbl .d-none-sm { display: none !important; }
  .ais2-page { font-size: 15px; }
  .ais2-page-header { align-items: flex-start; padding: 14px; }
  .ais2-page-header .ais2-ph-actions { width: 100%; margin-left: 0; }
  .ais2-page-header .ais2-ph-actions .ais2-btn { flex: 1; justify-content: center; }
  .ais2-filter-card { padding: 13px; }
  .ais2-filter-field { min-width: 100%; }
  .ais2-table-wrap { border-radius: 0; }
}

@media (prefers-reduced-motion: reduce) {
  .ais2-page *, .ais2-page *::before, .ais2-page *::after {
    scroll-behavior: auto !important; animation-duration: .01ms !important;
    animation-iteration-count: 1 !important; transition-duration: .01ms !important;
  }
}

/* ── Paginasi ──────────────────────────────────────────────────────── */
.ais2-pagination {
  display: flex; align-items: center; gap: 8px; padding: 12px 16px;
  border-top: 1px solid var(--ais2-line); flex-wrap: wrap;
}
.ais2-pagination .ais2-page-info { font-size: 12.5px; color: var(--ais2-ink2); }
.ais2-pagination .ais2-page-btns { display: flex; gap: 4px; margin-left: auto; flex-wrap: wrap; }
.ais2-page-btn {
  min-width: 32px; height: 32px; padding: 0 8px; border-radius: 7px;
  font-size: 12.5px; font-weight: 600; cursor: pointer;
  border: 1.5px solid var(--ais2-line); background: #fff; color: var(--ais2-ink2);
  display: flex; align-items: center; justify-content: center;
  transition: var(--ais2-transition);
}
.ais2-page-btn:hover { border-color: var(--ais2-pri2); color: var(--ais2-pri2); }
.ais2-page-btn.active { background: var(--ais2-pri2); color: #fff; border-color: var(--ais2-pri2); }
.ais2-page-btn:disabled { opacity: .4; cursor: not-allowed; }
.ais2-per-page { padding: 5px 8px; border: 1.5px solid var(--ais2-line); border-radius: 7px; font-size: 12.5px; }

/* ── Modal ─────────────────────────────────────────────────────────── */
.ais2-modal-backdrop {
  position: fixed; inset: 0; background: rgba(15,23,42,.45);
  z-index: 1050; display: flex; align-items: flex-start;
  justify-content: center; padding: 24px 16px; overflow-y: auto;
  backdrop-filter: blur(2px);
}
.ais2-modal {
  background: var(--ais2-card); border-radius: var(--ais2-radius);
  box-shadow: var(--ais2-shadow-lg); width: 100%; max-width: 720px;
  overflow: hidden; animation: ais2-modalIn .2s ease;
  margin: auto;
}
.ais2-modal-lg { max-width: 980px; }
.ais2-modal-sm { max-width: 460px; }
@keyframes ais2-modalIn {
  from { opacity: 0; transform: translateY(-20px) scale(.97); }
  to   { opacity: 1; transform: none; }
}
.ais2-modal-header {
  background: var(--ais2-grad); padding: 16px 20px;
  display: flex; align-items: center; gap: 12px;
}
.ais2-modal-header h5 { color: #fff; font-size: 15px; font-weight: 800; margin: 0; flex: 1; }
.ais2-modal-close {
  width: 30px; height: 30px; border-radius: 8px; background: rgba(255,255,255,.15);
  color: #fff; border: none; cursor: pointer; display: flex; align-items: center;
  justify-content: center; font-size: 14px; transition: var(--ais2-transition);
}
.ais2-modal-close:hover { background: rgba(255,255,255,.25); }
.ais2-modal-body { padding: 22px 24px; max-height: 75vh; overflow-y: auto; }
.ais2-modal-footer {
  padding: 14px 24px; border-top: 1px solid var(--ais2-line);
  display: flex; justify-content: flex-end; gap: 10px; flex-wrap: wrap;
}

/* ── Form Fields ─────────────────────────────────────────────────────── */
.ais2-form-section { margin-bottom: 24px; }
.ais2-form-section-title {
  font-size: 12px; font-weight: 800; text-transform: uppercase;
  letter-spacing: .7px; color: var(--ais2-pri2); margin-bottom: 14px;
  padding-bottom: 8px; border-bottom: 2px solid #e0e7ff;
}
.ais2-form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.ais2-form-grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.ais2-form-full { grid-column: 1 / -1; }
.ais2-field { display: flex; flex-direction: column; gap: 5px; }
.ais2-field label { font-size: 12.5px; font-weight: 700; color: var(--ais2-ink); }
.ais2-field label .req { color: var(--ais2-bad); margin-left: 2px; }
.ais2-field .ais2-input,
.ais2-field select,
.ais2-field textarea {
  padding: 9px 12px; border: 1.5px solid var(--ais2-line);
  border-radius: var(--ais2-radius-sm); font-size: 13.5px;
  font-family: var(--ais2-font); color: var(--ais2-ink);
  background: var(--ais2-bg); transition: var(--ais2-transition); outline: none; width: 100%;
}
.ais2-field .ais2-input:focus,
.ais2-field select:focus,
.ais2-field textarea:focus {
  border-color: var(--ais2-pri2); background: #fff;
  box-shadow: 0 0 0 3px rgba(79,70,229,.1);
}
.ais2-field .ais2-input.is-invalid, .ais2-field select.is-invalid, .ais2-field textarea.is-invalid {
  border-color: var(--ais2-bad);
}
.ais2-field .ais2-err { font-size: 11.5px; color: var(--ais2-bad); margin-top: 2px; }
.ais2-field .ais2-hint { font-size: 11.5px; color: var(--ais2-ink3); margin-top: 2px; }
.ais2-field textarea { resize: vertical; min-height: 80px; }
.ais2-field-check { flex-direction: row; align-items: center; gap: 8px; }
.ais2-field-check input[type=checkbox] { width: 16px; height: 16px; accent-color: var(--ais2-pri2); }
.ais2-input-group { display: flex; }
.ais2-input-group .ais2-input { border-radius: var(--ais2-radius-sm) 0 0 var(--ais2-radius-sm); flex: 1; }
.ais2-input-group .ais2-btn-addon {
  padding: 0 12px; border: 1.5px solid var(--ais2-pri2); border-left: none;
  border-radius: 0 var(--ais2-radius-sm) var(--ais2-radius-sm) 0;
  background: var(--ais2-pri2); color: #fff; cursor: pointer; font-size: 13px;
}
@media (max-width: 767px) {
  .ais2-form-grid, .ais2-form-grid-3 { grid-template-columns: 1fr; }
}

/* ── Badge / Status ─────────────────────────────────────────────────── */
.ais2-badge {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 3px 9px; border-radius: 20px; font-size: 11.5px; font-weight: 700;
}
.ais2-badge-green { background: #dcfce7; color: #16a34a; }
.ais2-badge-red   { background: #fee2e2; color: #dc2626; }
.ais2-badge-yellow { background: #fef9c3; color: #854d0e; }
.ais2-badge-blue  { background: #dbeafe; color: #1d4ed8; }
.ais2-badge-gray  { background: #f1f5f9; color: #64748b; }
.ais2-badge-purple { background: #ede9fe; color: #7c3aed; }
.ais2-badge-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }

/* ── Toast ──────────────────────────────────────────────────────────── */
.ais2-toast-container {
  position: fixed; bottom: 20px; right: 20px; z-index: 9999;
  display: flex; flex-direction: column; gap: 10px; pointer-events: none;
}
.ais2-toast {
  display: flex; align-items: flex-start; gap: 10px;
  background: var(--ais2-card); border-radius: var(--ais2-radius);
  box-shadow: var(--ais2-shadow-lg); padding: 13px 16px;
  min-width: 280px; max-width: 360px; pointer-events: all;
  border-left: 4px solid var(--ais2-ok); animation: ais2-toastIn .25s ease;
  font-size: 13.5px;
}
.ais2-toast.error { border-left-color: var(--ais2-bad); }
.ais2-toast.warn  { border-left-color: var(--ais2-warn); }
.ais2-toast.info  { border-left-color: var(--ais2-info); }
.ais2-toast-ic { font-size: 16px; flex-shrink: 0; margin-top: 1px; }
.ais2-toast.error .ais2-toast-ic { color: var(--ais2-bad); }
.ais2-toast.warn  .ais2-toast-ic { color: var(--ais2-warn); }
.ais2-toast.info  .ais2-toast-ic { color: var(--ais2-info); }
.ais2-toast.success .ais2-toast-ic { color: var(--ais2-ok); }
@keyframes ais2-toastIn {
  from { opacity: 0; transform: translateX(20px); }
  to   { opacity: 1; transform: none; }
}

/* ── Confirm Dialog ─────────────────────────────────────────────────── */
.ais2-confirm { text-align: center; padding: 28px 20px 16px; }
.ais2-confirm .ais2-confirm-ic { font-size: 48px; margin-bottom: 12px; }
.ais2-confirm h5 { font-size: 16px; font-weight: 800; margin-bottom: 8px; }
.ais2-confirm p { font-size: 13.5px; color: var(--ais2-ink2); margin-bottom: 0; }

/* ── Loading Skeleton ─────────────────────────────────────────────── */
.ais2-skeleton {
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%; animation: ais2-shimmer 1.5s infinite;
  border-radius: 6px;
}
@keyframes ais2-shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.ais2-sk-row { height: 16px; margin: 8px 0; }
.ais2-sk-row.short { width: 60%; }

/* ── Avatar ─────────────────────────────────────────────────────────── */
.ais2-avatar {
  width: 38px; height: 38px; border-radius: 50%; display: flex;
  align-items: center; justify-content: center; font-weight: 800;
  font-size: 13px; flex-shrink: 0; color: #fff;
  background: var(--ais2-grad);
}

/* ── Tabs ─────────────────────────────────────────────────────────── */
.ais2-tabs { display: flex; gap: 0; border-bottom: 2px solid var(--ais2-line); margin-bottom: 18px; overflow-x: auto; }
.ais2-tab {
  padding: 10px 18px; font-size: 13px; font-weight: 700; cursor: pointer;
  color: var(--ais2-ink2); border-bottom: 3px solid transparent;
  white-space: nowrap; transition: var(--ais2-transition); margin-bottom: -2px;
}
.ais2-tab:hover { color: var(--ais2-pri2); }
.ais2-tab.active { color: var(--ais2-pri2); border-bottom-color: var(--ais2-pri2); }
.ais2-tab-pane { display: none; }
.ais2-tab-pane.active { display: block; }

/* ── Upload Zone ─────────────────────────────────────────────────── */
.ais2-upload-zone {
  border: 2px dashed var(--ais2-line); border-radius: var(--ais2-radius);
  padding: 28px; text-align: center; cursor: pointer;
  transition: var(--ais2-transition); background: var(--ais2-bg);
}
.ais2-upload-zone:hover, .ais2-upload-zone.dragover {
  border-color: var(--ais2-pri2); background: #f0f4ff;
}
.ais2-upload-zone i { font-size: 32px; color: var(--ais2-ink3); margin-bottom: 10px; display: block; }
.ais2-upload-zone p { font-size: 13px; color: var(--ais2-ink2); margin: 0; }
.ais2-upload-zone .ais2-btn { margin-top: 12px; }

/* ── Foto / Avatar Picker ─────────────────────────────────────────── */
.ais2-foto-picker {
  display: flex; align-items: center; gap: 16px;
}
.ais2-foto-preview {
  width: 80px; height: 80px; border-radius: 50%; object-fit: cover;
  border: 3px solid var(--ais2-line); background: var(--ais2-bg);
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; color: var(--ais2-ink3); overflow: hidden;
}
.ais2-foto-preview img { width: 100%; height: 100%; object-fit: cover; }

/* ── Empty State ─────────────────────────────────────────────────── */
.ais2-empty {
  text-align: center; padding: 56px 24px; color: var(--ais2-ink3);
}
.ais2-empty i { font-size: 48px; margin-bottom: 14px; display: block; opacity: .5; }
.ais2-empty h4 { font-size: 15px; font-weight: 700; color: var(--ais2-ink2); margin-bottom: 6px; }
.ais2-empty p { font-size: 13px; }
</style>

<script>
/**
 * AIS2_KOMPONEN — Pustaka Komponen UI Reusable Sistem Baru2 eCampus
 *
 * @namespace AIS2
 * @version 1.0.0
 *
 * GAMBARAN UMUM
 * =============
 * Namespace ini mengelompokkan seluruh komponen UI interaktif yang digunakan
 * di semua halaman modul Baru2. Setiap komponen dirancang sebagai fungsi
 * mandiri (self-contained function) yang:
 *   - Menerima satu objek opsi (options object) sebagai parameter
 *   - Mengelola state internalnya sendiri via closure
 *   - Berkomunikasi dengan server melalui Fetch API ke service JSP baru2
 *   - Tidak bergantung pada state global (kecuali ROOT yang sudah di-set
 *     oleh index.jsp halaman shell)
 *
 * INTEGRASI ALPINE.JS
 * ===================
 * Komponen yang memerlukan reaktifitas dua arah (misalnya AIS2.Table)
 * menggunakan Alpine.js untuk mengelola state tabel (loading, data, paginasi).
 * Komponen lainnya menggunakan vanilla JS biasa agar ringan.
 *
 * POLA KOMUNIKASI SERVER
 * ======================
 * Semua request data menggunakan endpoint standar:
 *   GET  /baru2?modul={modul}&svc={nama_service}&{params}  → JSON list/detail
 *   POST /baru2?modul={modul}&svc={nama_service}            → JSON {ok, msg, id}
 *
 * Service JSP mengembalikan JSON dengan format standar:
 *   List:   { "total": N, "data": [...] }
 *   Action: { "ok": true/false, "msg": "...", "id": optional }
 *   Error:  { "ok": false, "msg": "Pesan kesalahan" }
 *
 * PENGGUNAAN DASAR
 * ================
 * 1. Sertakan _libs.jsp dan _komponen.jsp di head halaman.
 * 2. Buat elemen container: <div id="tabelMahasiswa"></div>
 * 3. Panggil komponen setelah DOM siap:
 *
 *    AIS2.Table.init({
 *      container: '#tabelMahasiswa',
 *      modul: 'pendataan',
 *      svc: 'mahasiswa_list',
 *      kolom: [
 *        { field: 'nim', label: 'NIM', sort: true },
 *        { field: 'nama', label: 'Nama' },
 *        { field: 'status', label: 'Status', render: AIS2.Badge.aktif }
 *      ],
 *      aksi: ['ubah', 'hapus']
 *    });
 */
var AIS2 = window.AIS2 || {};

/** Root context path, diset oleh halaman shell (index.jsp) */
AIS2.ROOT = window.ROOT || '';

/* ───────────────────────────────────────────────────────────────────────────
 * AIS2.L — Fungsi terjemahan multi-bahasa dari tabel label_bahasa
 *
 * @description Mengembalikan teks terjemahan untuk kunci (nama) yang diberikan,
 * berdasarkan bahasa aktif pengguna yang sudah dimuat via _lang.jsp ke
 * window.AIS2_LANG. Jika kunci tidak ditemukan, mengembalikan nilai fallback
 * atau kunci itu sendiri.
 *
 * Pola penggunaan:
 *   AIS2.L('Mahasiswa')              → "Mahasiswa" / "Student" / "الطالب"
 *   AIS2.L('NIM','NIM')              → "NIM" atau terjemahannya
 *   AIS2.L('TambahData','Tambah')    → terjemahan atau "Tambah" sebagai default
 *
 * Label dapat didaftarkan / diedit melalui menu Pengaturan → LabelBahasa.
 * ─────────────────────────────────────────────────────────────────────────── */
AIS2.L = function(kunci, fallback) {
    var m = window.AIS2_LANG;
    if (m && m[kunci] !== undefined && m[kunci] !== '') return m[kunci];
    return fallback !== undefined ? fallback : kunci;
};

/* Shorthand: lapisan helper untuk label umum yang sering dipakai */
AIS2.L.btn = {
    tambah : function() { return AIS2.L('TambahData','Tambah'); },
    simpan  : function() { return AIS2.L('Simpan','Simpan'); },
    tutup   : function() { return AIS2.L('Tutup','Tutup'); },
    hapus   : function() { return AIS2.L('Hapus','Hapus'); },
    cari    : function() { return AIS2.L('Cari','Cari'); },
    ekspor  : function() { return AIS2.L('Ekspor','Ekspor'); },
    ubah    : function() { return AIS2.L('Ubah','Ubah'); },
    batal   : function() { return AIS2.L('Batal','Batal'); }
};

/* ───────────────────────────────────────────────────────────────────────────
 * AIS2.Toast — Notifikasi ringan di pojok layar
 *
 * @description Menampilkan pesan notifikasi bertipe success, error, warn, atau
 * info. Pesan hilang otomatis setelah durasi yang dikonfigurasi. Mendukung
 * antrian beberapa pesan secara bersamaan.
 *
 * @example
 *   AIS2.Toast.sukses('Data berhasil disimpan');
 *   AIS2.Toast.error('Gagal menyimpan: NIM sudah digunakan');
 *   AIS2.Toast.peringatan('Kolom Nama wajib diisi');
 *   AIS2.Toast.info('Memuat data, mohon tunggu...');
 * ─────────────────────────────────────────────────────────────────────────── */
AIS2.Toast = (function() {
    var _container = null;

    function _getContainer() {
        if (!_container) {
            _container = document.getElementById('ais2-toast-container');
            if (!_container) {
                _container = document.createElement('div');
                _container.id = 'ais2-toast-container';
                _container.className = 'ais2-toast-container';
                document.body.appendChild(_container);
            }
        }
        return _container;
    }

    function _tampil(pesan, tipe, durasi) {
        tipe = tipe || 'success';
        durasi = durasi || 3500;
        var ikon = { success: 'fa-circle-check', error: 'fa-circle-xmark', warn: 'fa-triangle-exclamation', info: 'fa-circle-info' };
        var el = document.createElement('div');
        el.className = 'ais2-toast ' + tipe;
        el.innerHTML = '<i class="fa-solid ' + (ikon[tipe] || 'fa-circle-info') + ' ais2-toast-ic"></i>'
            + '<span>' + _escHtml(pesan) + '</span>';
        _getContainer().appendChild(el);
        setTimeout(function() {
            el.style.opacity = '0';
            el.style.transition = 'opacity .3s';
            setTimeout(function() { if (el.parentNode) el.parentNode.removeChild(el); }, 350);
        }, durasi);
    }

    return {
        sukses:    function(p, d) { _tampil(p, 'success', d); },
        error:     function(p, d) { _tampil(p, 'error', d); },
        peringatan: function(p, d) { _tampil(p, 'warn', d); },
        info:      function(p, d) { _tampil(p, 'info', d); }
    };
})();

/* ───────────────────────────────────────────────────────────────────────────
 * AIS2.Confirm — Dialog konfirmasi sebelum tindakan destruktif
 *
 * @param {Object} opsi
 * @param {string} opsi.judul       Judul dialog
 * @param {string} opsi.pesan       Pesan konfirmasi (HTML diperbolehkan)
 * @param {string} [opsi.labelOk]   Label tombol konfirmasi (default: "Ya, Lanjutkan")
 * @param {string} [opsi.tipe]      "danger" | "warning" | "info" (default: "danger")
 * @param {Function} opsi.onOk      Callback jika pengguna mengkonfirmasi
 * @param {Function} [opsi.onBatal] Callback jika pengguna membatalkan
 *
 * @example
 *   AIS2.Confirm.tampil({
 *     judul: 'Hapus Data',
 *     pesan: 'Data mahasiswa <b>AHMAD</b> akan dihapus permanen.',
 *     onOk: function() { hapusMahasiswa(id); }
 *   });
 * ─────────────────────────────────────────────────────────────────────────── */
AIS2.Confirm = (function() {
    function tampil(opsi) {
        var tipe = opsi.tipe || 'danger';
        var ikonWarna = { danger: '#ef4444', warning: '#f59e0b', info: '#0ea5e9' };
        var ikonClass = { danger: 'fa-circle-exclamation', warning: 'fa-triangle-exclamation', info: 'fa-circle-question' };
        var backdrop = document.createElement('div');
        backdrop.className = 'ais2-modal-backdrop';
        backdrop.style.zIndex = '1060';
        backdrop.innerHTML =
            '<div class="ais2-modal ais2-modal-sm">'
            + '<div class="ais2-modal-body ais2-confirm">'
            + '<i class="fa-solid ' + ikonClass[tipe] + '" style="font-size:48px;color:' + ikonWarna[tipe] + ';margin-bottom:12px;display:block"></i>'
            + '<h5>' + _escHtml(opsi.judul || 'Konfirmasi') + '</h5>'
            + '<p>' + (opsi.pesan || '') + '</p>'
            + '</div>'
            + '<div class="ais2-modal-footer">'
            + '<button class="ais2-btn ais2-btn-outline" id="ais2-conf-batal">Batal</button>'
            + '<button class="ais2-btn ' + (tipe === 'danger' ? 'ais2-btn-danger' : 'ais2-btn-primary') + '" id="ais2-conf-ok">'
            + (opsi.labelOk || 'Ya, Lanjutkan') + '</button>'
            + '</div></div>';
        document.body.appendChild(backdrop);
        backdrop.querySelector('#ais2-conf-batal').onclick = function() {
            document.body.removeChild(backdrop);
            if (opsi.onBatal) opsi.onBatal();
        };
        backdrop.querySelector('#ais2-conf-ok').onclick = function() {
            document.body.removeChild(backdrop);
            if (opsi.onOk) opsi.onOk();
        };
        backdrop.onclick = function(e) { if (e.target === backdrop) backdrop.querySelector('#ais2-conf-batal').click(); };
    }
    return { tampil: tampil };
})();

/* ───────────────────────────────────────────────────────────────────────────
 * AIS2.Modal — Modal dialog reusable untuk form dan detail
 *
 * @description Membuka modal dialog dengan header bergradien, body scrollable,
 * dan footer dengan tombol aksi. Mendukung ukuran sm, md (default), lg.
 * Menutup dengan klik backdrop atau tekan Escape.
 *
 * @param {Object} opsi
 * @param {string} opsi.judul       Judul modal
 * @param {string} opsi.konten      HTML konten body modal
 * @param {string} [opsi.ukuran]    "sm" | "" | "lg" (default: "")
 * @param {Array}  [opsi.tombol]    Array {label, kelas, onClick}
 * @returns {Object} { tutup: Function }
 *
 * @example
 *   var modal = AIS2.Modal.buka({
 *     judul: 'Tambah Mahasiswa',
 *     ukuran: 'lg',
 *     konten: '<form id="formMahasiswa">...</form>',
 *     tombol: [
 *       { label: 'Simpan', kelas: 'ais2-btn-primary', onClick: function() { simpan(); } },
 *       { label: 'Batal',  kelas: 'ais2-btn-outline', onClick: function() { modal.tutup(); } }
 *     ]
 *   });
 * ─────────────────────────────────────────────────────────────────────────── */
AIS2.Modal = (function() {
    var _aktif = null;
    var _pemicu = null;

    function buka(opsi) {
        if (_aktif) tutup();
        _pemicu = document.activeElement;
        var ukuran = opsi.ukuran ? ' ais2-modal-' + opsi.ukuran : '';
        var tombolHtml = '';
        if (opsi.tombol) {
            opsi.tombol.forEach(function(t) {
                tombolHtml += '<button class="ais2-btn ' + (t.kelas || '') + '" data-role="' + (t.role || '') + '">' + _escHtml(t.label) + '</button>';
            });
        }
        var backdrop = document.createElement('div');
        backdrop.className = 'ais2-modal-backdrop';
        backdrop.innerHTML =
            '<div class="ais2-modal' + ukuran + '" role="dialog" aria-modal="true" aria-labelledby="ais2-modal-title">'
            + '<div class="ais2-modal-header">'
            + '<h5 id="ais2-modal-title"><i class="fa-solid ' + (opsi.ikon || 'fa-pen-to-square') + '" style="margin-right:8px"></i>' + _escHtml(opsi.judul || '') + '</h5>'
            + '<button class="ais2-modal-close" id="ais2-modal-x" aria-label="Tutup dialog"><i class="fa-solid fa-xmark"></i></button>'
            + '</div>'
            + '<div class="ais2-modal-body" id="ais2-modal-body">' + (opsi.konten || '') + '</div>'
            + (tombolHtml ? '<div class="ais2-modal-footer">' + tombolHtml + '</div>' : '')
            + '</div>';
        document.body.appendChild(backdrop);
        backdrop.querySelector('#ais2-modal-x').onclick = tutup;
        backdrop.onclick = function(e) { if (e.target === backdrop) tutup(); };
        if (opsi.tombol) {
            var btns = backdrop.querySelectorAll('.ais2-modal-footer .ais2-btn');
            btns.forEach(function(btn, i) {
                if (opsi.tombol[i] && opsi.tombol[i].onClick) {
                    btn.onclick = opsi.tombol[i].onClick;
                }
            });
        }
        document.addEventListener('keydown', _escListener);
        _aktif = backdrop;
        setTimeout(function() {
            var fokusAwal = backdrop.querySelector('input:not([disabled]),select:not([disabled]),textarea:not([disabled]),button:not([disabled]),[tabindex]:not([tabindex="-1"])');
            if (fokusAwal) fokusAwal.focus();
        }, 0);
        if (opsi.onBuka) setTimeout(opsi.onBuka, 50);
        return { tutup: tutup, body: function() { return backdrop.querySelector('#ais2-modal-body'); } };
    }

    function tutup() {
        if (_aktif) { document.body.removeChild(_aktif); _aktif = null; }
        document.removeEventListener('keydown', _escListener);
        if (_pemicu && document.documentElement.contains(_pemicu) && _pemicu.focus) _pemicu.focus();
        _pemicu = null;
    }

    function _escListener(e) {
        if (e.key === 'Escape') { tutup(); return; }
        if (e.key !== 'Tab' || !_aktif) return;
        var fokus = _aktif.querySelectorAll('a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])');
        if (!fokus.length) { e.preventDefault(); return; }
        var pertama = fokus[0], terakhir = fokus[fokus.length - 1];
        if (e.shiftKey && document.activeElement === pertama) { e.preventDefault(); terakhir.focus(); }
        else if (!e.shiftKey && document.activeElement === terakhir) { e.preventDefault(); pertama.focus(); }
    }

    return { buka: buka, tutup: tutup };
})();

/* ───────────────────────────────────────────────────────────────────────────
 * AIS2.Api — Helper untuk AJAX ke endpoint service JSP baru2
 *
 * @description Membungkus Fetch API dengan penanganan error terpusat,
 * NProgress loading bar, dan konversi respons JSON otomatis.
 * Mendukung GET (untuk daftar/detail) dan POST (untuk aksi CRUD).
 *
 * @example
 *   // GET data list
 *   AIS2.Api.get('pendataan', 'mahasiswa_list', { page: 1, size: 20, cari: 'Ahmad' })
 *     .then(function(data) { renderTabel(data); });
 *
 *   // POST simpan form
 *   AIS2.Api.post('pendataan', 'mahasiswa_simpan', formData)
 *     .then(function(res) {
 *       if (res.ok) AIS2.Toast.sukses(res.msg);
 *       else AIS2.Toast.error(res.msg);
 *     });
 * ─────────────────────────────────────────────────────────────────────────── */
AIS2.Api = (function() {
    function _root() {
        return window.ROOT !== undefined && window.ROOT !== null ? window.ROOT : (AIS2.ROOT || '');
    }
    function _url(modul, svc, params) {
        var u = _root() + '/baru2?modul=' + encodeURIComponent(modul) + '&svc=' + encodeURIComponent(svc);
        if (params) {
            Object.keys(params).forEach(function(k) {
                if (params[k] !== null && params[k] !== undefined && params[k] !== '') {
                    u += '&' + encodeURIComponent(k) + '=' + encodeURIComponent(params[k]);
                }
            });
        }
        return u;
    }

    function get(modul, svc, params) {
        if (typeof NProgress !== 'undefined') NProgress.start();
        return fetch(_url(modul, svc, params), { credentials: 'same-origin' })
            .then(function(r) { return r.json(); })
            .catch(function(e) { AIS2.Toast.error('Gagal memuat data: ' + e.message); return null; })
            .finally(function() { if (typeof NProgress !== 'undefined') NProgress.done(); });
    }

    function post(modul, svc, dataObj) {
        if (typeof NProgress !== 'undefined') NProgress.start();
        var form = new FormData();
        if (dataObj) {
            Object.keys(dataObj).forEach(function(k) {
                if (dataObj[k] !== null && dataObj[k] !== undefined) form.append(k, dataObj[k]);
            });
        }
        return fetch(_root() + '/baru2?modul=' + encodeURIComponent(modul) + '&svc=' + encodeURIComponent(svc), {
            method: 'POST', body: form, credentials: 'same-origin'
        })
            .then(function(r) { return r.json(); })
            .catch(function(e) { AIS2.Toast.error('Gagal: ' + e.message); return { ok: false, msg: e.message }; })
            .finally(function() { if (typeof NProgress !== 'undefined') NProgress.done(); });
    }

    return { get: get, post: post };
})();

/* ───────────────────────────────────────────────────────────────────────────
 * AIS2.Badge — Renderer badge status untuk kolom tabel
 * ─────────────────────────────────────────────────────────────────────────── */
AIS2.Badge = {
    aktif: function(v) {
        return v ? '<span class="ais2-badge ais2-badge-green"><span class="ais2-badge-dot"></span> Aktif</span>'
                 : '<span class="ais2-badge ais2-badge-gray"><span class="ais2-badge-dot"></span> Nonaktif</span>';
    },
    yaTidak: function(v) {
        return v ? '<span class="ais2-badge ais2-badge-blue">Ya</span>'
                 : '<span class="ais2-badge ais2-badge-gray">Tidak</span>';
    },
    kustom: function(v, peta) {
        var item = peta[v] || peta['_default'] || { label: v, kelas: 'ais2-badge-gray' };
        return '<span class="ais2-badge ' + item.kelas + '">' + _escHtml(item.label) + '</span>';
    }
};

/* ───────────────────────────────────────────────────────────────────────────
 * AIS2.Form — Helper validasi dan serialisasi form
 * ─────────────────────────────────────────────────────────────────────────── */
AIS2.Form = {
    /**
     * Mengambil semua nilai field dari sebuah form element menjadi plain object.
     * @param {HTMLFormElement|string} form - Element atau selector CSS form
     * @returns {Object} Data form sebagai key-value pairs
     */
    ambilData: function(form) {
        if (typeof form === 'string') form = document.querySelector(form);
        var data = {};
        if (!form) return data;
        new FormData(form).forEach(function(v, k) { data[k] = v; });
        // Juga ambil checkbox yang tidak tercentang
        form.querySelectorAll('input[type=checkbox]').forEach(function(cb) {
            data[cb.name] = cb.checked ? 'true' : 'false';
        });
        return data;
    },

    /**
     * Mengisi nilai field form dari sebuah object data.
     * @param {HTMLFormElement|string} form - Element atau selector CSS form
     * @param {Object} data - Data untuk diisi ke form
     */
    isiData: function(form, data) {
        if (typeof form === 'string') form = document.querySelector(form);
        if (!form || !data) return;
        Object.keys(data).forEach(function(k) {
            var el = form.querySelector('[name="' + k + '"]');
            if (!el) return;
            if (el.type === 'checkbox') { el.checked = (data[k] === true || data[k] === 'true' || data[k] === '1'); }
            else { el.value = data[k] === null ? '' : data[k]; }
        });
    },

    /**
     * Validasi sederhana: cek field required tidak kosong.
     * Menambahkan class is-invalid dan mengembalikan false jika gagal.
     * @param {HTMLFormElement|string} form
     * @returns {boolean}
     */
    validasi: function(form) {
        if (typeof form === 'string') form = document.querySelector(form);
        var ok = true;
        form.querySelectorAll('[required]').forEach(function(el) {
            el.classList.remove('is-invalid');
            if (!el.value.trim()) { el.classList.add('is-invalid'); ok = false; }
        });
        return ok;
    },

    reset: function(form) {
        if (typeof form === 'string') form = document.querySelector(form);
        if (form) {
            form.reset();
            form.querySelectorAll('.is-invalid').forEach(function(el) { el.classList.remove('is-invalid'); });
        }
    }
};

/* ───────────────────────────────────────────────────────────────────────────
 * AIS2.Table — Tabel data dengan AJAX, paginasi, sort, filter
 *
 * @description Komponen inti untuk semua halaman pendataan. Memuat data dari
 * service JSP via AJAX, menampilkan dalam tabel responsif, mendukung
 * paginasi sisi server, sorting per kolom, dan aksi baris (ubah/hapus/dll).
 *
 * Pola halaman yang menggunakan AIS2.Table:
 *   1. Definisikan kolom dan opsi sekali (biasanya di awal skrip halaman)
 *   2. Table memuat data otomatis saat init
 *   3. Filter/sort/paginasi me-reload data via API tanpa full page refresh
 *
 * @param {Object} opsi
 * @param {string|HTMLElement} opsi.container   Selector atau elemen wrapper
 * @param {string} opsi.modul                   Nama modul baru2 (mis: "pendataan")
 * @param {string} opsi.svc                     Nama service (mis: "mahasiswa_list")
 * @param {Array}  opsi.kolom                   Definisi kolom tabel
 * @param {Object} [opsi.filter]                State filter awal
 * @param {number} [opsi.ukuranHalaman]         Baris per halaman (default: 20)
 * @param {boolean} [opsi.bisa.tambah]          Tampilkan tombol Tambah
 * @param {boolean} [opsi.bisa.hapus]           Aktifkan aksi Hapus
 * @param {boolean} [opsi.bisa.ubah]            Aktifkan aksi Ubah
 * @param {boolean} [opsi.bisa.ekspor]          Tampilkan tombol Ekspor
 * @param {Function} [opsi.onTambah]            Callback tombol Tambah
 * @param {Function} [opsi.onUbah]              Callback tombol Ubah (baris)
 * @param {Function} [opsi.onHapus]             Callback tombol Hapus (baris)
 * @param {Function} [opsi.renderAksi]          Custom render sel aksi baris
 * ─────────────────────────────────────────────────────────────────────────── */
AIS2.Table = {
    /**
     * Inisialisasi tabel AJAX.
     * @param {Object} opsi - Konfigurasi tabel (lihat deskripsi di atas)
     * @returns {Object} API instance { muat, refresh, getFilter, setFilter }
     */
    init: function(opsi) {
        var container = typeof opsi.container === 'string'
            ? document.querySelector(opsi.container) : opsi.container;
        if (!container) { console.error('AIS2.Table: container tidak ditemukan'); return null; }

        var state = {
            halaman: 1,
            ukuranHalaman: opsi.ukuranHalaman || 20,
            total: 0,
            sortField: opsi.sortDefault || '',
            sortDir: 'asc',
            filter: opsi.filter || {},
            loading: false
        };

        // Render struktur awal
        container.innerHTML = _renderStrukturTabel(opsi);
        container.classList.add('ais2-table-card');

        var tabelEl = container.querySelector('.ais2-tbl');
        var tbodyEl = container.querySelector('tbody');
        var paginasiEl = container.querySelector('.ais2-pagination');
        var countEl = container.querySelector('.ais2-table-count');

        // Bind header sort
        container.querySelectorAll('th.sortable').forEach(function(th) {
            th.onclick = function() {
                var f = th.dataset.field;
                if (state.sortField === f) {
                    state.sortDir = state.sortDir === 'asc' ? 'desc' : 'asc';
                } else {
                    state.sortField = f; state.sortDir = 'asc';
                }
                container.querySelectorAll('th').forEach(function(h) { h.classList.remove('asc','desc'); });
                th.classList.add(state.sortDir);
                state.halaman = 1;
                muat();
            };
        });

        function muat() {
            if (state.loading) return;
            state.loading = true;
            tbodyEl.innerHTML = _renderSkeleton(opsi.kolom ? opsi.kolom.length : 5);
            var params = Object.assign({}, state.filter, {
                page: state.halaman,
                size: state.ukuranHalaman,
                sortField: state.sortField,
                sortDir: state.sortDir
            });
            AIS2.Api.get(opsi.modul, opsi.svc, params).then(function(res) {
                state.loading = false;
                if (!res) { tbodyEl.innerHTML = _noData('Gagal memuat data'); return; }
                state.total = res.total || 0;
                var rows = res.data || [];
                if (rows.length === 0) {
                    tbodyEl.innerHTML = _noData('Tidak ada data ditemukan');
                } else {
                    tbodyEl.innerHTML = rows.map(function(row, idx) {
                        return _renderBaris(row, idx, opsi, state);
                    }).join('');
                    // Bind aksi baris
                    _bindAksi(tbodyEl, opsi, function() { muat(); });
                }
                _renderPaginasi(paginasiEl, state, muat);
                if (countEl) {
                    var dari = (state.halaman - 1) * state.ukuranHalaman + 1;
                    var sampai = Math.min(state.halaman * state.ukuranHalaman, state.total);
                    countEl.textContent = state.total > 0
                        ? 'Menampilkan ' + dari + '–' + sampai + ' dari ' + state.total + ' data'
                        : 'Tidak ada data';
                }
            });
        }

        // Muat pertama kali
        muat();

        // Bind tombol Tambah
        var btnTambah = container.querySelector('.ais2-btn-tambah');
        if (btnTambah && opsi.onTambah) btnTambah.onclick = opsi.onTambah;

        return {
            muat: muat,
            refresh: muat,
            getFilter: function() { return state.filter; },
            setFilter: function(f) { state.filter = Object.assign({}, f); state.halaman = 1; muat(); },
            getState: function() { return state; }
        };
    }
};

/* -- Fungsi internal (private) ------------------------------------------- */

function _renderStrukturTabel(opsi) {
    var bisa = opsi.bisa || {};
    var tombolBar = '';
    if (bisa.tambah !== false) {
        tombolBar += '<button class="ais2-btn ais2-btn-primary ais2-btn-tambah"><i class="fa-solid fa-plus"></i> Tambah</button>';
    }
    if (bisa.ekspor !== false) {
        tombolBar += '<button class="ais2-btn ais2-btn-outline ais2-btn-ekspor"><i class="fa-solid fa-file-excel"></i> Ekspor</button>';
    }

    var kolomHtml = (opsi.kolom || []).map(function(k) {
        var sortAttr = k.sort ? ' class="sortable" data-field="' + k.field + '"' : ' class=""';
        var sortIc = k.sort ? ' <i class="fa-solid fa-sort sort-ic"></i>' : '';
        var hideClass = k.sembunyiMobile ? ' d-none-sm' : '';
        return '<th' + sortAttr + ' style="' + (k.lebar ? 'width:' + k.lebar : '') + '">' + k.label + sortIc + '</th>';
    }).join('');
    var adaAksi = opsi.bisa && (opsi.bisa.ubah !== false || opsi.bisa.hapus !== false);
    if (adaAksi || opsi.renderAksi) kolomHtml += '<th style="width:120px">Aksi</th>';

    return '<div class="ais2-table-toolbar">'
        + '<div style="display:flex;gap:8px;flex-wrap:wrap">' + tombolBar + '</div>'
        + '<div class="ais2-table-count"></div>'
        + '</div>'
        + '<div class="ais2-table-wrap"><table class="ais2-tbl"><thead><tr>' + kolomHtml + '</tr></thead><tbody></tbody></table></div>'
        + '<div class="ais2-pagination"></div>';
}

function _renderBaris(row, idx, opsi, state) {
    var bisa = opsi.bisa || {};
    var sel = (opsi.kolom || []).map(function(k) {
        var val = row[k.field];
        var rendered = k.render ? k.render(val, row) : _escHtml(val === null || val === undefined ? '' : String(val));
        var hideClass = k.sembunyiMobile ? ' class="d-none-sm"' : '';
        return '<td' + hideClass + '>' + rendered + '</td>';
    }).join('');

    var aksiHtml = '';
    if (opsi.renderAksi) {
        aksiHtml = opsi.renderAksi(row);
    } else if (bisa.ubah !== false || bisa.hapus !== false) {
        aksiHtml = '<div class="td-actions">';
        if (bisa.ubah !== false) {
            aksiHtml += '<button class="ais2-btn ais2-btn-outline ais2-btn-sm ais2-aksi-ubah" data-id="' + _escAttr(row.id) + '" data-json="' + _escAttr(JSON.stringify(row)) + '" title="Ubah"><i class="fa-solid fa-pen"></i></button>';
        }
        if (bisa.hapus !== false) {
            aksiHtml += '<button class="ais2-btn ais2-btn-danger ais2-btn-sm ais2-aksi-hapus" data-id="' + _escAttr(row.id) + '" data-nama="' + _escAttr(row.nama || row.id) + '" title="Hapus"><i class="fa-solid fa-trash"></i></button>';
        }
        aksiHtml += '</div>';
    }
    if (aksiHtml) sel += '<td class="nowrap">' + aksiHtml + '</td>';

    return '<tr>' + sel + '</tr>';
}

function _bindAksi(tbody, opsi, onRefresh) {
    tbody.querySelectorAll('.ais2-aksi-ubah').forEach(function(btn) {
        btn.onclick = function() {
            var data = JSON.parse(btn.dataset.json || '{}');
            if (opsi.onUbah) opsi.onUbah(data);
        };
    });
    tbody.querySelectorAll('.ais2-aksi-hapus').forEach(function(btn) {
        btn.onclick = function() {
            var id = btn.dataset.id;
            var nama = btn.dataset.nama || id;
            AIS2.Confirm.tampil({
                judul: 'Konfirmasi Hapus',
                pesan: 'Data <b>' + _escHtml(nama) + '</b> akan dihapus permanen.<br>Tindakan ini tidak dapat dibatalkan.',
                onOk: function() {
                    if (opsi.onHapus) {
                        opsi.onHapus(id, function(ok, msg) {
                            if (ok) { AIS2.Toast.sukses(msg || 'Data berhasil dihapus'); onRefresh(); }
                            else AIS2.Toast.error(msg || 'Gagal menghapus data');
                        });
                    }
                }
            });
        };
    });
}

function _renderPaginasi(el, state, onGanti) {
    if (!el) return;
    var totalHalaman = Math.ceil(state.total / state.ukuranHalaman);
    if (totalHalaman <= 1 && state.total <= state.ukuranHalaman) { el.innerHTML = ''; return; }
    var hlm = state.halaman;
    var pages = [];
    pages.push(1);
    if (hlm > 3) pages.push('...');
    for (var i = Math.max(2, hlm - 1); i <= Math.min(totalHalaman - 1, hlm + 1); i++) pages.push(i);
    if (hlm < totalHalaman - 2) pages.push('...');
    if (totalHalaman > 1) pages.push(totalHalaman);

    var html = '<span class="ais2-page-info">Halaman ' + hlm + ' dari ' + totalHalaman + '</span>'
        + '<div class="ais2-page-btns">'
        + '<button class="ais2-page-btn" ' + (hlm <= 1 ? 'disabled' : '') + ' data-hlm="' + (hlm - 1) + '"><i class="fa-solid fa-chevron-left"></i></button>';
    pages.forEach(function(p) {
        if (p === '...') { html += '<span class="ais2-page-btn" style="pointer-events:none">…</span>'; }
        else { html += '<button class="ais2-page-btn ' + (p === hlm ? 'active' : '') + '" data-hlm="' + p + '">' + p + '</button>'; }
    });
    html += '<button class="ais2-page-btn" ' + (hlm >= totalHalaman ? 'disabled' : '') + ' data-hlm="' + (hlm + 1) + '"><i class="fa-solid fa-chevron-right"></i></button>'
        + '</div>';
    el.innerHTML = html;
    el.querySelectorAll('[data-hlm]').forEach(function(btn) {
        btn.onclick = function() {
            if (btn.disabled) return;
            state.halaman = parseInt(btn.dataset.hlm);
            onGanti();
        };
    });
}

function _renderSkeleton(n) {
    var s = '';
    for (var r = 0; r < 8; r++) {
        s += '<tr>';
        for (var c = 0; c < n + 1; c++) {
            s += '<td><div class="ais2-skeleton ais2-sk-row ' + (c % 3 === 0 ? 'short' : '') + '"></div></td>';
        }
        s += '</tr>';
    }
    return s;
}

function _noData(msg) {
    return '<tr><td colspan="99" class="no-data"><i class="fa-solid fa-inbox"></i>' + msg + '</td></tr>';
}

function _escHtml(s) {
    return String(s || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function _escAttr(s) {
    return String(s || '').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}
</script>
