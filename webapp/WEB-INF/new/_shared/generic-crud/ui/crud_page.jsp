<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.net.URLEncoder" %>
<%
String gcModule = String.valueOf(request.getAttribute("genericCrudModuleKey"));
String gcPage = String.valueOf(request.getAttribute("genericCrudPageKey"));
String gcEndpoint = request.getContextPath() + "/new?service=1&module="
        + URLEncoder.encode(gcModule, "UTF-8") + "&page=" + URLEncoder.encode(gcPage, "UTF-8")
        + "&entity=" + URLEncoder.encode(String.valueOf(request.getAttribute("genericCrudEntityKey")), "UTF-8");
Object gcMenuId = request.getAttribute("nui_current_menu_id");
if (gcMenuId != null) gcEndpoint += "&menuId=" + URLEncoder.encode(String.valueOf(gcMenuId), "UTF-8");
%>
<style><%@ include file="/WEB-INF/new/_shared/generic-crud/assets/generic-crud.css" %></style>
<section class="gc" data-gc-root data-endpoint="<%=gcEndpoint%>">
  <header class="gc-head">
    <div><div class="gc-breadcrumb">Beranda / Master Data</div><h1 data-gc-title>Memuat…</h1><p>CRUD berbasis metadata dengan audit, scope, dan hak akses role aktif.</p></div>
    <div class="gc-actions">
      <select class="gc-export-format" data-gc-export-format aria-label="Format ekspor"><option value="xlsx">XLSX</option><option value="pdf">PDF</option><option value="docx">DOCX</option><option value="pptx">PPTX</option></select>
      <button type="button" class="gc-btn" data-gc-export>Ekspor</button>
      <button type="button" class="gc-btn" data-gc-template hidden>Template Import</button>
      <label class="gc-btn gc-file" data-gc-import-label hidden>Import XLSX<input type="file" accept=".xlsx" data-gc-import></label>
      <button type="button" class="gc-btn gc-primary" data-gc-add hidden>+ Tambah Data</button>
    </div>
  </header>
  <div class="gc-alert" data-gc-alert hidden role="alert"></div>
  <section class="gc-parity" data-gc-parity hidden>
    <header>
      <div><strong>Fungsi dari modul ZKOSS</strong><span data-gc-parity-summary></span></div>
      <button type="button" class="gc-btn" data-gc-parity-toggle aria-expanded="false">Tampilkan semua fungsi</button>
    </header>
    <div class="gc-parity-actions" data-gc-parity-actions hidden></div>
  </section>
  <section class="gc-card">
    <div class="gc-toolbar">
      <label class="gc-search"><span class="gc-sr">Cari</span><input type="search" data-gc-search placeholder="Cari data…" autocomplete="off"></label>
      <button type="button" class="gc-btn" data-gc-filter-toggle>Filter Lanjutan</button>
      <button type="button" class="gc-btn" data-gc-columns-toggle>Kolom</button>
      <select data-gc-page-size aria-label="Jumlah baris per halaman"><option>5</option><option selected>10</option><option>25</option><option>50</option><option>100</option></select>
      <span class="gc-status" data-gc-status>Memuat…</span>
    </div>
    <div class="gc-config-panel" data-gc-filter-panel hidden>
      <select data-gc-filter-field aria-label="Field filter"></select>
      <select data-gc-filter-operator aria-label="Operator filter"><option value="EQ">Sama dengan</option><option value="NE">Tidak sama</option><option value="CONTAINS">Mengandung</option><option value="STARTS_WITH">Diawali</option><option value="GT">Lebih besar</option><option value="GTE">Lebih besar/sama</option><option value="LT">Lebih kecil</option><option value="LTE">Lebih kecil/sama</option><option value="IS_NULL">Kosong</option><option value="IS_NOT_NULL">Tidak kosong</option></select>
      <input data-gc-filter-value placeholder="Nilai filter"><button type="button" class="gc-btn" data-gc-filter-add>Tambah Filter</button><div class="gc-chips" data-gc-filter-chips></div>
    </div>
    <div class="gc-config-panel gc-columns" data-gc-columns-panel hidden><div data-gc-columns></div><button type="button" class="gc-btn" data-gc-columns-reset>Reset</button></div>
    <div class="gc-table-wrap"><table class="gc-table"><thead data-gc-head></thead><tbody data-gc-body><tr><td>Memuat data…</td></tr></tbody></table></div>
    <footer class="gc-pager"><button class="gc-btn" data-gc-prev>← Sebelumnya</button><span data-gc-page-info>Halaman 1</span><button class="gc-btn" data-gc-next>Berikutnya →</button></footer>
  </section>

  <div class="gc-overlay" data-gc-overlay hidden></div>
  <aside class="gc-drawer" data-gc-drawer hidden aria-modal="true" role="dialog" aria-labelledby="gc-form-title">
    <header><div><small data-gc-form-mode>Form</small><h2 id="gc-form-title" data-gc-form-title>Data</h2></div><button type="button" class="gc-icon-btn" data-gc-close aria-label="Tutup">×</button></header>
    <form data-gc-form novalidate><div class="gc-form" data-gc-fields></div><div class="gc-form-error" data-gc-form-error hidden></div>
      <footer><button type="button" class="gc-btn" data-gc-cancel>Batal</button><button type="submit" class="gc-btn gc-primary">Simpan</button></footer>
    </form>
  </aside>
  <aside class="gc-drawer gc-audit" data-gc-audit hidden aria-modal="true" role="dialog" aria-labelledby="gc-audit-title">
    <header><div><small>Histori immutable</small><h2 id="gc-audit-title">Riwayat Perubahan</h2></div><button type="button" class="gc-icon-btn" data-gc-audit-close aria-label="Tutup">×</button></header>
    <div class="gc-audit-list" data-gc-audit-list>Memuat…</div>
  </aside>
  <aside class="gc-drawer" data-gc-import-drawer hidden aria-modal="true" role="dialog" aria-labelledby="gc-import-title"><header><div><small>Dry-run wajib</small><h2 id="gc-import-title">Preview Import</h2></div><button type="button" class="gc-icon-btn" data-gc-import-close aria-label="Tutup">×</button></header><div class="gc-audit-list" data-gc-import-summary></div><footer><button type="button" class="gc-btn" data-gc-import-cancel>Batal</button><button type="button" class="gc-btn gc-primary" data-gc-import-confirm>Konfirmasi Import</button></footer></aside>
  <aside class="gc-legacy" data-gc-legacy hidden aria-modal="true" role="dialog" aria-labelledby="gc-legacy-title">
    <header><div><small>Compatibility bridge — business flow asli dipertahankan</small><h2 id="gc-legacy-title" data-gc-legacy-title>Modul ZKOSS</h2></div><button type="button" class="gc-icon-btn" data-gc-legacy-close aria-label="Tutup">×</button></header>
    <iframe data-gc-legacy-frame title="Modul ZKOSS Mahasiswa" loading="lazy"></iframe>
  </aside>
</section>
<script><%@ include file="/WEB-INF/new/_shared/generic-crud/assets/generic-crud.js" %></script>
