<%@page import="ais.common.Common"%><%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%><%
String rnd = Common.getGeneratedBarCode(7);
String root = Common.ROOT;
String requestedSort = request.getParameter("sort");
String defaultSort = ("POPULAR".equals(requestedSort) || "YEAR_DESC".equals(requestedSort)) ? requestedSort : "NEWEST";
%>
<link rel="stylesheet" href="<%=root%>/assets/library-modern/library.css">
<script src="<%=root%>/assets/library-modern/library.js"></script>

<div class="library-modern" id="libraryCatalog<%=rnd%>">
  <section class="library-hero" aria-labelledby="libraryTitle<%=rnd%>">
    <div class="library-container">
      <div class="library-eyebrow" style="color:#99f6e4"><%=Common.getBahasaConfig("Katalog & Discovery")%></div>
      <h1 id="libraryTitle<%=rnd%>"><%=Common.getBahasaConfig("Temukan pengetahuan untuk belajar, meneliti, dan berkarya")%></h1>
      <p><%=Common.getBahasaConfig("Telusuri buku, e-book, jurnal, tugas akhir, multimedia, kitab, dan koleksi khusus dari seluruh unit perpustakaan.")%></p>
      <form class="library-searchbar" id="libraryQuickSearch<%=rnd%>" role="search">
        <label class="visually-hidden" for="libraryQuery<%=rnd%>"><%=Common.getBahasaConfig("Cari koleksi")%></label>
        <input id="libraryQuery<%=rnd%>" name="query" autocomplete="off" maxlength="160"
               placeholder="<%=Common.getBahasaConfig("Cari judul, penulis, ISBN, subjek, kata kunci, atau nomor panggil...")%>">
        <button class="library-button library-button-primary" type="submit"><i class="fa fa-search" aria-hidden="true"></i> <%=Common.getBahasaConfig("Cari")%></button>
      </form>
    </div>
  </section>

  <div class="library-container">
    <div class="library-workspace">
      <aside class="library-card library-facets" id="libraryFacets<%=rnd%>" aria-label="<%=Common.getBahasaConfig("Filter katalog")%>">
        <div class="library-facet">
          <div class="library-eyebrow"><%=Common.getBahasaConfig("Filter")%></div>
          <h2 class="library-section-title"><%=Common.getBahasaConfig("Persempit hasil")%></h2>
        </div>
        <div class="library-facet">
          <label for="libraryTitleFilter<%=rnd%>"><%=Common.getBahasaConfig("Judul")%></label>
          <input class="library-input" id="libraryTitleFilter<%=rnd%>" maxlength="160">
          <label for="libraryAuthor<%=rnd%>" style="margin-top:10px"><%=Common.getBahasaConfig("Penulis / Pengarang")%></label>
          <input class="library-input" id="libraryAuthor<%=rnd%>" maxlength="120">
          <label for="libraryPublisher<%=rnd%>" style="margin-top:10px"><%=Common.getBahasaConfig("Penerbit")%></label>
          <input class="library-input" id="libraryPublisher<%=rnd%>" maxlength="120">
        </div>
        <div class="library-facet">
          <label for="libraryBranch<%=rnd%>"><%=Common.getBahasaConfig("Perpustakaan / Cabang")%></label>
          <select class="library-select" id="libraryBranch<%=rnd%>"><option value=""><%=Common.getBahasaConfig("Semua cabang")%></option></select>
          <label for="libraryItemType<%=rnd%>" style="margin-top:10px"><%=Common.getBahasaConfig("Jenis koleksi")%></label>
          <select class="library-select" id="libraryItemType<%=rnd%>"><option value=""><%=Common.getBahasaConfig("Semua jenis")%></option></select>
          <label for="libraryMaterialType<%=rnd%>" style="margin-top:10px"><%=Common.getBahasaConfig("Format / tipe")%></label>
          <select class="library-select" id="libraryMaterialType<%=rnd%>"><option value=""><%=Common.getBahasaConfig("Semua format")%></option></select>
        </div>
        <div class="library-facet">
          <label><%=Common.getBahasaConfig("Tahun terbit")%></label>
          <div class="library-filter-grid">
            <input class="library-input" id="libraryYearFrom<%=rnd%>" type="number" min="1000" max="2200" placeholder="Dari" aria-label="<%=Common.getBahasaConfig("Tahun mulai")%>">
            <input class="library-input" id="libraryYearTo<%=rnd%>" type="number" min="1000" max="2200" placeholder="Sampai" aria-label="<%=Common.getBahasaConfig("Tahun akhir")%>">
          </div>
        </div>
        <details class="library-facet">
          <summary style="cursor:pointer;font-weight:800"><%=Common.getBahasaConfig("Institusi & unit akademik")%></summary>
          <label for="libraryFoundation<%=rnd%>" style="margin-top:12px"><%=Common.getBahasaConfig("Yayasan")%></label>
          <select class="library-select" id="libraryFoundation<%=rnd%>"><option value=""><%=Common.getBahasaConfig("Semua yayasan")%></option></select>
          <label for="librarySchool<%=rnd%>" style="margin-top:10px"><%=Common.getBahasaConfig("Sekolah")%></label>
          <select class="library-select" id="librarySchool<%=rnd%>"><option value=""><%=Common.getBahasaConfig("Semua sekolah")%></option></select>
          <label for="libraryFaculty<%=rnd%>" style="margin-top:10px"><%=Common.getBahasaConfig("Fakultas")%></label>
          <select class="library-select" id="libraryFaculty<%=rnd%>"><option value=""><%=Common.getBahasaConfig("Semua fakultas")%></option></select>
          <label for="libraryStudyProgram<%=rnd%>" style="margin-top:10px"><%=Common.getBahasaConfig("Program studi / jurusan")%></label>
          <select class="library-select" id="libraryStudyProgram<%=rnd%>"><option value=""><%=Common.getBahasaConfig("Semua program studi")%></option></select>
        </details>
        <div class="library-filter-actions">
          <button class="library-button" id="libraryReset<%=rnd%>" type="button"><%=Common.getBahasaConfig("Atur ulang")%></button>
          <button class="library-button library-button-primary" id="libraryApply<%=rnd%>" type="button"><%=Common.getBahasaConfig("Terapkan")%></button>
        </div>
      </aside>

      <main>
        <div class="library-results-head">
          <div>
            <div class="library-eyebrow"><%=Common.getBahasaConfig("Hasil pencarian")%></div>
            <h2 class="library-section-title" id="libraryResultTitle<%=rnd%>"><%=Common.getBahasaConfig("Seluruh koleksi")%></h2>
            <div class="library-muted" id="libraryResultCount<%=rnd%>" aria-live="polite"><%=Common.getBahasaConfig("Menyiapkan katalog...")%></div>
          </div>
          <div class="library-result-tools">
            <button class="library-button library-filter-toggle" id="libraryFilterToggle<%=rnd%>" type="button" aria-expanded="false"><%=Common.getBahasaConfig("Filter")%></button>
            <button class="library-button" id="libraryListMode<%=rnd%>" type="button" aria-pressed="true" title="<%=Common.getBahasaConfig("Mode daftar")%>">☷</button>
            <button class="library-button" id="libraryGridMode<%=rnd%>" type="button" aria-pressed="false" title="<%=Common.getBahasaConfig("Mode kartu")%>">▦</button>
            <label class="visually-hidden" for="librarySort<%=rnd%>"><%=Common.getBahasaConfig("Urutkan")%></label>
            <select class="library-select" id="librarySort<%=rnd%>">
              <option value="NEWEST"><%=Common.getBahasaConfig("Koleksi terbaru")%></option>
              <option value="YEAR_DESC"><%=Common.getBahasaConfig("Tahun terbaru")%></option>
              <option value="TITLE_ASC"><%=Common.getBahasaConfig("Judul A-Z")%></option>
              <option value="AUTHOR_ASC"><%=Common.getBahasaConfig("Penulis A-Z")%></option>
              <option value="POPULAR"><%=Common.getBahasaConfig("Paling populer")%></option>
            </select>
          </div>
        </div>

        <div class="library-chips" id="libraryActiveFilters<%=rnd%>" aria-label="<%=Common.getBahasaConfig("Filter aktif")%>"></div>
        <div class="library-result-list" id="libraryResults<%=rnd%>" aria-busy="true">
          <div class="library-card library-skeleton"></div><div class="library-card library-skeleton"></div><div class="library-card library-skeleton"></div>
        </div>
        <nav class="library-pagination" aria-label="<%=Common.getBahasaConfig("Navigasi halaman katalog")%>">
          <button class="library-button" id="libraryPrevious<%=rnd%>" type="button"><%=Common.getBahasaConfig("Sebelumnya")%></button>
          <span id="libraryPageInfo<%=rnd%>">1 / 1</span>
          <button class="library-button" id="libraryNext<%=rnd%>" type="button"><%=Common.getBahasaConfig("Selanjutnya")%></button>
        </nav>
      </main>
    </div>
  </div>
</div>

<script>
(function () {
  'use strict';
  var suffix = '<%=rnd%>';
  var root = '<%=root%>';
  var api = root + '/pustaka?hanya_tampil_jsp=true&p=pustaka&s=_catalog_api';
  var page = 1;
  var pageSize = 12;
  var total = 0;
  var requestSerial = 0;
  var filterMap = {
    query: 'libraryQuery', title: 'libraryTitleFilter', author: 'libraryAuthor', publisher: 'libraryPublisher',
    libraryId: 'libraryBranch', itemTypeId: 'libraryItemType', materialTypeId: 'libraryMaterialType',
    foundationId: 'libraryFoundation', schoolId: 'librarySchool', facultyId: 'libraryFaculty',
    studyProgramId: 'libraryStudyProgram', yearFrom: 'libraryYearFrom', yearTo: 'libraryYearTo'
  };
  var filterLabels = {
    query: '<%=Common.getBahasaConfigJS("Kata kunci")%>', title: '<%=Common.getBahasaConfigJS("Judul")%>',
    author: '<%=Common.getBahasaConfigJS("Penulis")%>', publisher: '<%=Common.getBahasaConfigJS("Penerbit")%>',
    libraryId: '<%=Common.getBahasaConfigJS("Cabang")%>', itemTypeId: '<%=Common.getBahasaConfigJS("Jenis")%>',
    materialTypeId: '<%=Common.getBahasaConfigJS("Format")%>', foundationId: '<%=Common.getBahasaConfigJS("Yayasan")%>',
    schoolId: '<%=Common.getBahasaConfigJS("Sekolah")%>', facultyId: '<%=Common.getBahasaConfigJS("Fakultas")%>',
    studyProgramId: '<%=Common.getBahasaConfigJS("Program studi")%>', yearFrom: '<%=Common.getBahasaConfigJS("Tahun mulai")%>',
    yearTo: '<%=Common.getBahasaConfigJS("Tahun akhir")%>'
  };
  function el(base) { return document.getElementById(base + suffix); }
  function value(key) { var node = el(filterMap[key]); return node ? node.value.trim() : ''; }
  function params() {
    var data = {action:'search', page:page, pageSize:pageSize, sort:el('librarySort').value};
    Object.keys(filterMap).forEach(function (key) { if (value(key)) data[key] = value(key); });
    return data;
  }
  function optionLabel(key, raw) {
    var node = el(filterMap[key]);
    if (node && node.tagName === 'SELECT' && node.selectedIndex >= 0) return node.options[node.selectedIndex].text;
    return raw;
  }
  function fillSelect(base, rows) {
    var select = el(base);
    (rows || []).forEach(function (row) { select.appendChild(new Option(row.nama || '-', row.id)); });
  }
  function renderChips() {
    var target = el('libraryActiveFilters');
    var html = '';
    Object.keys(filterMap).forEach(function (key) {
      var raw = value(key);
      if (raw) html += '<span class="library-chip">' + LibraryModern.escapeHtml(filterLabels[key] + ': ' + optionLabel(key, raw))
        + '<button type="button" data-clear="' + key + '" aria-label="Hapus filter">×</button></span>';
    });
    target.innerHTML = html;
    Array.prototype.forEach.call(target.querySelectorAll('[data-clear]'), function (button) {
      button.addEventListener('click', function () { el(filterMap[button.getAttribute('data-clear')]).value = ''; page = 1; load(); });
    });
  }
  function cover(item) {
    var title = LibraryModern.escapeHtml(item.nama || '<%=Common.getBahasaConfigJS("Tanpa judul")%>');
    if (item.imageUrl && /^https?:\/\//i.test(item.imageUrl)) {
      return '<div class="library-cover"><img loading="lazy" src="' + LibraryModern.escapeHtml(item.imageUrl) + '" alt="" onerror="this.parentNode.innerHTML=\'KOLEKSI\'"></div>';
    }
    var media = root + '/library/item-cover?id=' + encodeURIComponent(item.id || '');
    return '<div class="library-cover"><img loading="lazy" src="' + media + '" alt="" onerror="this.parentNode.textContent=\'KOLEKSI\'"></div>';
  }
  function renderItems(items) {
    var target = el('libraryResults');
    if (!items || !items.length) {
      target.innerHTML = '<div class="library-card library-state"><strong><%=Common.getBahasaConfigJS("Koleksi tidak ditemukan")%></strong><span class="library-muted"><%=Common.getBahasaConfigJS("Coba kurangi filter atau gunakan kata kunci yang lebih umum.")%></span></div>';
      return;
    }
    target.innerHTML = items.map(function (item) {
      var title = LibraryModern.escapeHtml(item.nama || '<%=Common.getBahasaConfigJS("Tanpa judul")%>');
      var detail = root + '/pustaka?id=' + encodeURIComponent(item.id);
      var copies = Number(item.jumlahEksemplar || 0);
      return '<article class="library-card library-result">' + cover(item)
        + '<div><div class="library-eyebrow">' + LibraryModern.escapeHtml(item.kategories || '<%=Common.getBahasaConfigJS("Koleksi perpustakaan")%>') + '</div>'
        + '<h3><a href="' + detail + '">' + title + '</a></h3>'
        + '<div class="library-meta"><span>✎ ' + LibraryModern.escapeHtml(item.pengarangs || '-') + '</span><span>▣ ' + LibraryModern.escapeHtml((item.penerbit && item.penerbit.nama) || '-') + '</span><span>◷ ' + LibraryModern.escapeHtml(item.tahun || '-') + '</span><span>' + LibraryModern.escapeHtml(item.bahasa || '-') + '</span></div>'
        + '<p class="library-summary">' + LibraryModern.escapeHtml(item.ringkasan || '<%=Common.getBahasaConfigJS("Ringkasan belum tersedia.")%>') + '</p></div>'
        + '<div class="library-result-action"><span class="library-status">● ' + copies + ' <%=Common.getBahasaConfigJS("eksemplar tercatat")%></span>'
        + '<small>' + LibraryModern.escapeHtml(item.callNumber || item.isbn || item.issn || '<%=Common.getBahasaConfigJS("Nomor panggil belum tersedia")%>') + '</small>'
        + '<a class="library-button library-button-primary" href="' + detail + '"><%=Common.getBahasaConfigJS("Lihat detail")%></a></div></article>';
    }).join('');
  }
  function updatePage() {
    var pages = Math.max(1, Math.ceil(total / pageSize));
    el('libraryPageInfo').textContent = page + ' / ' + pages;
    el('libraryPrevious').disabled = page <= 1;
    el('libraryNext').disabled = page >= pages;
  }
  function load() {
    var serial = ++requestSerial;
    el('libraryResults').setAttribute('aria-busy', 'true');
    el('libraryResults').innerHTML = '<div class="library-card library-skeleton"></div><div class="library-card library-skeleton"></div>';
    renderChips();
    LibraryModern.fetchJson(api, params()).then(function (data) {
      if (serial !== requestSerial) return;
      total = Number(data.total || data.count || 0);
      renderItems(data.items || data.data || []);
      el('libraryResultCount').textContent = total.toLocaleString('id-ID') + ' <%=Common.getBahasaConfigJS("judul ditemukan")%>';
      var keyword = value('query');
      el('libraryResultTitle').textContent = keyword ? '<%=Common.getBahasaConfigJS("Hasil untuk")%> “' + keyword + '”' : '<%=Common.getBahasaConfigJS("Seluruh koleksi")%>';
      updatePage();
    }).catch(function () {
      if (serial !== requestSerial) return;
      el('libraryResults').innerHTML = '<div class="library-card library-state"><strong><%=Common.getBahasaConfigJS("Katalog belum dapat dimuat")%></strong><span class="library-muted"><%=Common.getBahasaConfigJS("Periksa koneksi lalu coba kembali.")%></span><br><button class="library-button library-button-primary" type="button" id="libraryRetry<%=rnd%>"><%=Common.getBahasaConfigJS("Coba lagi")%></button></div>';
      el('libraryRetry').addEventListener('click', load);
    }).then(function () { el('libraryResults').setAttribute('aria-busy', 'false'); });
  }
  LibraryModern.fetchJson(api, {action:'references'}).then(function (data) {
    fillSelect('libraryBranch', data.libraries); fillSelect('libraryItemType', data.itemTypes); fillSelect('libraryMaterialType', data.materialTypes);
    fillSelect('libraryFoundation', data.foundations); fillSelect('librarySchool', data.schools); fillSelect('libraryFaculty', data.faculties); fillSelect('libraryStudyProgram', data.studyPrograms);
  }).catch(function () { /* Search remains usable without optional reference lists. */ });
  el('libraryQuickSearch').addEventListener('submit', function (event) { event.preventDefault(); page = 1; load(); });
  el('libraryApply').addEventListener('click', function () { page = 1; load(); });
  el('libraryReset').addEventListener('click', function () { Object.keys(filterMap).forEach(function (key) { el(filterMap[key]).value = ''; }); page = 1; load(); });
  el('librarySort').addEventListener('change', function () { page = 1; load(); });
  el('libraryPrevious').addEventListener('click', function () { if (page > 1) { page--; load(); } });
  el('libraryNext').addEventListener('click', function () { if (page * pageSize < total) { page++; load(); } });
  el('libraryFilterToggle').addEventListener('click', function () { var facets = el('libraryFacets'); var open = facets.classList.toggle('is-open'); this.setAttribute('aria-expanded', String(open)); });
  el('libraryListMode').addEventListener('click', function () { el('libraryResults').classList.remove('library-grid-mode'); this.setAttribute('aria-pressed','true'); el('libraryGridMode').setAttribute('aria-pressed','false'); });
  el('libraryGridMode').addEventListener('click', function () { el('libraryResults').classList.add('library-grid-mode'); this.setAttribute('aria-pressed','true'); el('libraryListMode').setAttribute('aria-pressed','false'); });
  el('librarySort').value = '<%=defaultSort%>';
  load();
}());
</script>
