<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.net.URLEncoder" %>
<%
String gcModule = String.valueOf(request.getAttribute("genericCrudModuleKey"));
String gcPage = String.valueOf(request.getAttribute("genericCrudPageKey"));
String gcEndpoint = request.getContextPath() + "/new?service=1&module="
        + URLEncoder.encode(gcModule, "UTF-8") + "&page=" + URLEncoder.encode(gcPage, "UTF-8");
%>
<style><jsp:include page="/WEB-INF/new/_shared/generic-crud/assets/generic-crud.css" /></style>
<section class="gc" data-gc-root data-endpoint="<%=gcEndpoint%>">
  <header class="gc-head">
    <div><div class="gc-breadcrumb">Beranda / Master Data</div><h1 data-gc-title>Memuat…</h1><p>CRUD berbasis metadata dengan audit, scope, dan hak akses role aktif.</p></div>
    <div class="gc-actions">
      <button type="button" class="gc-btn" data-gc-export>Ekspor XLSX</button>
      <button type="button" class="gc-btn gc-primary" data-gc-add hidden>+ Tambah Data</button>
    </div>
  </header>
  <div class="gc-alert" data-gc-alert hidden role="alert"></div>
  <section class="gc-card">
    <div class="gc-toolbar">
      <label class="gc-search"><span class="gc-sr">Cari</span><input type="search" data-gc-search placeholder="Cari data…" autocomplete="off"></label>
      <select data-gc-page-size aria-label="Jumlah baris per halaman"><option>5</option><option selected>10</option><option>25</option><option>50</option><option>100</option></select>
      <span class="gc-status" data-gc-status>Memuat…</span>
    </div>
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
</section>
<script><jsp:include page="/WEB-INF/new/_shared/generic-crud/assets/generic-crud.js" /></script>
