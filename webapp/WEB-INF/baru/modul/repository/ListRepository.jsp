<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.*" %>
<%@ page import="ais.action.master.repository.RepositoryPublicService.*" %>
<%!
private String rh(String value) {
    if (value == null) return "";
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
}
private String ru(String value) {
    try { return URLEncoder.encode(value == null ? "" : value, "UTF-8"); }
    catch (Exception e) { return ""; }
}
private String rlabel(String value) {
    if ("OPEN_ACCESS".equals(value)) return "Open Access";
    if ("METADATA_ONLY".equals(value)) return "Metadata saja";
    if ("EMBARGOED".equals(value)) return "Embargo";
    if ("INSTITUTION_ONLY".equals(value)) return "Khusus institusi";
    if ("AUTHENTICATED".equals(value)) return "Pengguna terautentikasi";
    if ("RESTRICTED".equals(value)) return "Terbatas";
    return value == null || value.length() == 0 ? "Status belum ditetapkan" : value;
}
private String rsize(Long bytes) {
    if (bytes == null) return "Ukuran tidak tersedia";
    double value = bytes.doubleValue();
    if (value >= 1073741824d) return String.format(java.util.Locale.US, "%.1f GB", value / 1073741824d);
    if (value >= 1048576d) return String.format(java.util.Locale.US, "%.1f MB", value / 1048576d);
    if (value >= 1024d) return String.format(java.util.Locale.US, "%.1f KB", value / 1024d);
    return bytes + " B";
}
%>
<%
String root = request.getContextPath();
String view = request.getAttribute("repoView") == null ? "home" : String.valueOf(request.getAttribute("repoView"));
Summary summary = (Summary) request.getAttribute("repoSummary");
List<CollectionView> collections = (List<CollectionView>) request.getAttribute("repoCollections");
if (collections == null) collections = Collections.emptyList();
String institution = "Institusi Pendidikan";
String logo = "";
try {
    ais.database.model.PerguruanTinggi ptPublicRepo = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
    if (ptPublicRepo != null && ptPublicRepo.getNama() != null) institution = ptPublicRepo.getNama();
    logo = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
} catch (Exception ignored) {
    ais.common.ErrorAuditUtil.record(ignored, "repository public institution body");
}
NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("id", "ID"));
%>
<div class="repo-modern">
  <a class="repo-skip" href="#repo-content">Lewati ke konten utama</a>
  <header class="repo-nav">
    <div class="repo-wrap repo-nav-inner">
      <a class="repo-brand" href="<%=root%>/repository" aria-label="Beranda repositori">
        <span class="repo-brand-mark"><%=logo != null && logo.trim().length() > 0 ? "<img src='" + rh(logo) + "' alt='' width='30' height='30'>" : "▤"%></span>
        <span><strong>Repositori Institusi</strong><small><%=rh(institution)%></small></span>
      </a>
      <nav class="repo-links" aria-label="Navigasi repositori">
        <a href="<%=root%>/repository" <%="home".equals(view) ? "aria-current='page'" : ""%>>Beranda</a>
        <a href="<%=root%>/repository?view=search" <%="search".equals(view) || "browse".equals(view) ? "aria-current='page'" : ""%>>Jelajah</a>
        <a href="<%=root%>/repository?view=policies" <%="policies".equals(view) ? "aria-current='page'" : ""%>>Kebijakan</a>
        <a href="<%=root%>/repository?view=help" <%="help".equals(view) ? "aria-current='page'" : ""%>>Bantuan</a>
      </nav>
      <div class="repo-nav-actions"><a class="repo-btn repo-btn-primary" href="<%=root%>/login2">Masuk</a></div>
    </div>
  </header>

  <main id="repo-content">
  <% if ("home".equals(view)) {
      List<ItemCard> latest = (List<ItemCard>) request.getAttribute("repoLatest");
      if (latest == null) latest = Collections.emptyList(); %>
    <section class="repo-wrap repo-hero" aria-labelledby="repo-hero-title">
      <div class="repo-hero-main">
        <span class="repo-eyebrow">Repositori terbuka dan terpercaya</span>
        <h1 id="repo-hero-title">Temukan karya ilmiah dan pengetahuan institusi dalam satu pencarian</h1>
        <p>Jelajahi skripsi, tesis, artikel, buku, bahan ajar, data penelitian, dan koleksi digital yang tersedia sesuai kebijakan akses.</p>
        <form class="repo-search" action="<%=root%>/repository" method="get" role="search">
          <input type="hidden" name="view" value="search">
          <label class="visually-hidden" for="repo-home-q">Kata kunci</label>
          <input id="repo-home-q" name="q" maxlength="200" placeholder="Cari judul, penulis, abstrak, subjek, atau identifier">
          <button class="repo-btn repo-btn-primary" type="submit">Cari Repositori</button>
        </form>
      </div>
      <aside class="repo-summary" aria-label="Statistik repositori">
        <h2>Ringkasan repositori</h2>
        <div class="repo-stat-grid">
          <div class="repo-stat"><span>Karya terbit</span><b><%=summary == null ? "0" : nf.format(summary.totalItems)%></b></div>
          <div class="repo-stat"><span>Koleksi</span><b><%=summary == null ? "0" : nf.format(summary.totalCollections)%></b></div>
          <div class="repo-stat"><span>Open access</span><b><%=summary == null ? "0" : nf.format(summary.openAccess)%></b></div>
          <div class="repo-stat"><span>Metadata saja</span><b><%=summary == null ? "0" : nf.format(summary.metadataOnly)%></b></div>
        </div>
        <p>Angka hanya menghitung record aktif dan berstatus publik.</p>
      </aside>
    </section>

    <section class="repo-wrap repo-section" aria-labelledby="browse-title">
      <div class="repo-section-head"><div><h2 id="browse-title">Jelajah repositori</h2><p>Masuk melalui jalur yang paling sesuai.</p></div><a class="repo-link" href="<%=root%>/repository?view=search">Lihat semua →</a></div>
      <div class="repo-browse-grid">
        <a class="repo-browse-card" href="<%=root%>/repository?view=search"><span class="repo-browse-icon">▦</span><strong>Koleksi</strong><small><%=collections.size()%> koleksi aktif</small></a>
        <a class="repo-browse-card" href="<%=root%>/repository?view=search&sort=title"><span class="repo-browse-icon">◎</span><strong>Pengarang</strong><small>Urutkan dan cari nama</small></a>
        <a class="repo-browse-card" href="<%=root%>/repository?view=search&sort=newest"><span class="repo-browse-icon">◷</span><strong>Tahun terbit</strong><small>Terbaru lebih dahulu</small></a>
        <a class="repo-browse-card" href="<%=root%>/repository?view=search&q=subjek"><span class="repo-browse-icon">◇</span><strong>Subjek</strong><small>Topik dan kata kunci</small></a>
        <a class="repo-browse-card" href="<%=root%>/repository?view=search&type=Thesis"><span class="repo-browse-icon">▧</span><strong>Jenis dokumen</strong><small>Skripsi, artikel, buku</small></a>
        <a class="repo-browse-card" href="<%=root%>/repository?view=search&access=OPEN_ACCESS"><span class="repo-browse-icon">○</span><strong>Open access</strong><small>Berkas terbuka</small></a>
      </div>
    </section>

    <section class="repo-wrap repo-section repo-home-grid">
      <div class="repo-card repo-card-pad">
        <div class="repo-section-head"><h2>Publikasi terbaru</h2><a class="repo-link" href="<%=root%>/repository?view=search">Lihat semua</a></div>
        <div class="repo-list">
        <% if (latest.isEmpty()) { %><div class="repo-empty"><h3>Belum ada publikasi</h3><p>Record publik akan muncul setelah proses persetujuan dan sinkronisasi selesai.</p></div><% }
           for (ItemCard item : latest) { %>
          <article class="repo-item-card">
            <span class="repo-doc"><%=rh(item.documentType.length() > 3 ? item.documentType.substring(0, 3).toUpperCase() : item.documentType.toUpperCase())%></span>
            <div><h3><a href="<%=root%>/repository?view=item&id=<%=item.id%>"><%=rh(item.title)%></a></h3>
              <div class="repo-meta"><span><%=rh(item.authors)%></span><span>•</span><span><%=rh(item.year)%></span><span>•</span><span><%=rh(item.collectionName)%></span></div>
              <div class="repo-chips"><span class="repo-chip <%="OPEN_ACCESS".equals(item.accessPolicy) ? "repo-chip-open" : "repo-chip-warning"%>"><%=rh(rlabel(item.accessPolicy))%></span><span class="repo-chip"><%=rh(item.documentType)%></span></div>
            </div>
            <a class="repo-btn repo-btn-soft" href="<%=root%>/repository?view=item&id=<%=item.id%>">Detail</a>
          </article>
        <% } %>
        </div>
      </div>
      <aside class="repo-card repo-card-pad">
        <div class="repo-section-head"><h2>Koleksi</h2></div>
        <div class="repo-collection-list">
          <% for (CollectionView collection : collections) { %>
          <a class="repo-collection" href="<%=root%>/repository?view=search&collection=<%=collection.id%>"><span><strong><%=rh(collection.nama)%></strong><small><%=rh(collection.tipe)%></small></span><b><%=nf.format(collection.itemCount)%></b></a>
          <% } %>
        </div>
      </aside>
    </section>

  <% } else if ("search".equals(view) || "browse".equals(view)) {
      SearchResult search = (SearchResult) request.getAttribute("repoSearch");
      Query query = search.query;
      String baseSearch = root + "/repository?view=search&q=" + ru(query.keyword)
              + "&collection=" + (query.collectionId == null ? "" : query.collectionId)
              + "&type=" + ru(query.documentType) + "&access=" + ru(query.accessPolicy)
              + "&year=" + (query.year == null ? "" : query.year) + "&sort=" + ru(query.sort) + "&size=" + query.pageSize;
  %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Hasil pencarian</div><h1>Hasil pencarian</h1><p><%=query.keyword.length() == 0 ? "Jelajahi seluruh publikasi yang tersedia." : "Menampilkan hasil untuk “" + rh(query.keyword) + "”."%></p></section>
    <section class="repo-wrap repo-card repo-search-panel">
      <form class="repo-search" action="<%=root%>/repository" method="get" role="search">
        <input type="hidden" name="view" value="search"><input type="hidden" name="type" value="<%=rh(query.documentType)%>"><input type="hidden" name="access" value="<%=rh(query.accessPolicy)%>">
        <label class="visually-hidden" for="repo-search-q">Kata kunci</label><input id="repo-search-q" name="q" maxlength="200" value="<%=rh(query.keyword)%>" placeholder="Cari judul, penulis, abstrak, subjek, atau identifier"><button class="repo-btn repo-btn-primary" type="submit">Cari</button>
      </form>
    </section>
    <div class="repo-wrap repo-search-layout">
      <aside class="repo-card repo-facets" data-repo-facets aria-label="Filter hasil">
        <h2>Saring hasil</h2><a class="repo-link" href="<%=root%>/repository?view=search&q=<%=ru(query.keyword)%>">Reset filter</a>
        <div class="repo-facet"><h3>Hak akses</h3>
          <% for (Map.Entry<String, Long> facet : search.accessFacets.entrySet()) { %><a href="<%=root%>/repository?view=search&q=<%=ru(query.keyword)%>&access=<%=ru(facet.getKey())%>&type=<%=ru(query.documentType)%>"><span><%=rh(rlabel(facet.getKey()))%></span><b><%=nf.format(facet.getValue())%></b></a><% } %>
        </div>
        <div class="repo-facet"><h3>Jenis dokumen</h3>
          <% for (Map.Entry<String, Long> facet : search.typeFacets.entrySet()) { %><a href="<%=root%>/repository?view=search&q=<%=ru(query.keyword)%>&type=<%=ru(facet.getKey())%>&access=<%=ru(query.accessPolicy)%>"><span><%=rh(facet.getKey())%></span><b><%=nf.format(facet.getValue())%></b></a><% } %>
        </div>
        <div class="repo-facet"><h3>Koleksi</h3>
          <% for (CollectionView collection : search.collections) { if (collection.itemCount > 0) { %><a href="<%=root%>/repository?view=search&q=<%=ru(query.keyword)%>&collection=<%=collection.id%>"><span><%=rh(collection.nama)%></span><b><%=nf.format(collection.itemCount)%></b></a><% }} %>
        </div>
      </aside>
      <section aria-live="polite">
        <div class="repo-results-head"><div><strong><%=nf.format(search.total)%> hasil</strong><div class="repo-chips"><% if (query.documentType.length() > 0) { %><span class="repo-chip"><%=rh(query.documentType)%></span><% } if (query.accessPolicy.length() > 0) { %><span class="repo-chip repo-chip-open"><%=rh(rlabel(query.accessPolicy))%></span><% } %></div></div>
          <div><button class="repo-btn repo-mobile-filter" type="button" data-repo-filter-toggle aria-expanded="false">Filter</button>
          <form method="get" action="<%=root%>/repository" style="display:inline"><input type="hidden" name="view" value="search"><input type="hidden" name="q" value="<%=rh(query.keyword)%>"><input type="hidden" name="type" value="<%=rh(query.documentType)%>"><input type="hidden" name="access" value="<%=rh(query.accessPolicy)%>"><select class="repo-btn" name="sort" onchange="this.form.submit()"><option value="newest" <%="newest".equals(query.sort) ? "selected" : ""%>>Terbaru</option><option value="oldest" <%="oldest".equals(query.sort) ? "selected" : ""%>>Terlama</option><option value="title" <%="title".equals(query.sort) ? "selected" : ""%>>Judul A–Z</option></select></form></div>
        </div>
        <div class="repo-results">
          <% if (search.items.isEmpty()) { %><div class="repo-card repo-empty"><h2>Tidak ada hasil</h2><p>Coba gunakan kata kunci lebih singkat atau hapus sebagian filter.</p></div><% }
             for (ItemCard item : search.items) { %>
          <article class="repo-card repo-result-card"><h2><a href="<%=root%>/repository?view=item&id=<%=item.id%>"><%=rh(item.title)%></a></h2><div class="repo-authors"><%=rh(item.authors)%></div><div class="repo-meta"><span><%=rh(item.documentType)%></span><span>•</span><span><%=rh(item.year)%></span><span>•</span><span><%=rh(item.collectionName)%></span></div><p class="repo-abstract"><%=rh(item.abstractText)%></p><div class="repo-chips"><span class="repo-chip <%="OPEN_ACCESS".equals(item.accessPolicy) ? "repo-chip-open" : "repo-chip-warning"%>"><%=rh(rlabel(item.accessPolicy))%></span><% if (item.subjects.length() > 0) { %><span class="repo-chip"><%=rh(item.subjects)%></span><% } %></div><div class="repo-result-actions"><span class="repo-meta"><%=rh(item.oaiIdentifier)%></span><a class="repo-btn repo-btn-soft" href="<%=root%>/repository?view=item&id=<%=item.id%>">Lihat detail</a></div></article>
          <% } %>
        </div>
        <% if (search.totalPages > 1) { %><nav class="repo-pagination" aria-label="Halaman hasil"><% int first = Math.max(1, query.page - 2), last = Math.min(search.totalPages, query.page + 2); if (query.page > 1) { %><a href="<%=baseSearch%>&page=<%=query.page-1%>" aria-label="Halaman sebelumnya">‹</a><% } for (int pi = first; pi <= last; pi++) { if (pi == query.page) { %><span aria-current="page"><%=pi%></span><% } else { %><a href="<%=baseSearch%>&page=<%=pi%>"><%=pi%></a><% }} if (query.page < search.totalPages) { %><a href="<%=baseSearch%>&page=<%=query.page+1%>" aria-label="Halaman berikutnya">›</a><% } %></nav><% } %>
      </section>
    </div>

  <% } else if ("item".equals(view)) {
      ItemDetail item = (ItemDetail) request.getAttribute("repoItem"); %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / <a href="<%=root%>/repository?view=search">Publikasi</a> / <%=rh(item.documentType)%></div></section>
    <div class="repo-wrap repo-detail">
      <section>
        <header class="repo-card repo-paper-head"><div class="repo-chips"><span class="repo-chip"><%=rh(item.documentType)%></span><span class="repo-chip <%="OPEN_ACCESS".equals(item.accessPolicy) ? "repo-chip-open" : "repo-chip-warning"%>"><%=rh(rlabel(item.accessPolicy))%></span><% if (item.year.length() > 0) { %><span class="repo-chip">Terbit <%=rh(item.year)%></span><% } %></div><h1><%=rh(item.title)%></h1><div class="repo-authors"><%=rh(item.authors)%></div><div class="repo-meta"><span><%=rh(item.collectionName)%></span><% if (item.publisher.length() > 0) { %><span>•</span><span><%=rh(item.publisher)%></span><% } %></div><div class="repo-paper-actions"><a class="repo-btn repo-btn-primary" href="<%=root%>/repository?action=citation&format=ris&id=<%=item.id%>">Ekspor RIS</a><a class="repo-btn" href="<%=root%>/repository?action=citation&format=bibtex&id=<%=item.id%>">BibTeX</a><button class="repo-btn" type="button" data-repo-copy="<%=rh(request.getRequestURL().toString() + "?view=item&id=" + item.id)%>">Salin tautan</button></div></header>
        <article class="repo-card repo-article"><h2>Abstrak</h2><% if (item.abstractText.length() > 0) { %><p><%=rh(item.abstractText)%></p><% } else { %><p>Abstrak belum tersedia pada metadata publik.</p><% } %><% if (item.subjects.length() > 0) { %><h2>Kata kunci</h2><div class="repo-chips"><span class="repo-chip"><%=rh(item.subjects)%></span></div><% } %></article>
        <section class="repo-card repo-article"><h2>Informasi bibliografis</h2><table class="repo-metadata"><tbody><tr><th>Identifier OAI</th><td><%=rh(item.oaiIdentifier)%></td></tr><% if (item.dspaceHandle.length() > 0) { %><tr><th>Handle</th><td><%=rh(item.dspaceHandle)%></td></tr><% } %><tr><th>Jenis dokumen</th><td><%=rh(item.documentType)%></td></tr><tr><th>Bahasa</th><td><%=rh(item.language)%></td></tr><tr><th>Koleksi</th><td><%=rh(item.collectionName)%></td></tr><% for (Map.Entry<String, List<String>> entry : item.metadata.entrySet()) { if ("dc.title".equals(entry.getKey()) || "dc.contributor.author".equals(entry.getKey()) || "dc.description.abstract".equals(entry.getKey())) continue; %><tr><th><%=rh(entry.getKey())%></th><td><% for (String value : entry.getValue()) { %><div><%=rh(value)%></div><% } %></td></tr><% } %></tbody></table></section>
      </section>
      <aside class="repo-card repo-file-panel"><div class="repo-file-preview">PDF</div><h2>Berkas publik</h2><% if (item.files.isEmpty()) { %><p>Metadata tersedia; naskah penuh belum tersedia untuk akses publik.</p><% } for (BitstreamView file : item.files) { %><div class="repo-file"><strong><%=rh(file.namaFile)%></strong><small><%=rh(file.mimeType)%> · <%=rh(rsize(file.ukuranByte))%><% if (file.checksum.length() > 0) { %> · checksum tersedia<% } %></small><a class="repo-btn repo-btn-primary" href="<%=root%>/repository?action=download&id=<%=file.id%>">Unduh berkas</a></div><% } %><div class="repo-chips"><span class="repo-chip <%="OPEN_ACCESS".equals(item.accessPolicy) ? "repo-chip-open" : "repo-chip-warning"%>"><%=rh(rlabel(item.accessPolicy))%></span></div></aside>
    </div>

  <% } else if ("policies".equals(view)) { %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Kebijakan</div><h1>Kebijakan repositori</h1><p>Prinsip akses, hak cipta, embargo, dan penarikan konten.</p></section><section class="repo-wrap repo-info-page"><div class="repo-card"><h2>Kebijakan akses</h2><p>Metadata publik dapat dijelajahi tanpa login. Akses berkas mengikuti status Open Access, khusus institusi, terautentikasi, terbatas, atau embargo yang ditetapkan pengelola.</p><h2>Hak cipta dan lisensi</h2><p>Hak cipta tetap berada pada pemegang hak. Pengguna wajib mengikuti lisensi yang tercantum pada setiap record dan tidak boleh menganggap ketersediaan metadata sebagai izin menggandakan naskah.</p><h2>Embargo</h2><p>Berkas dengan embargo tidak disediakan sebelum periode pembatasan berakhir. Metadata dapat tetap terlihat bila kebijakan institusi mengizinkan.</p><h2>Penarikan dan koreksi</h2><p>Record terbit tidak dihapus diam-diam. Permintaan koreksi atau penarikan ditangani pengelola dan menyisakan jejak audit sesuai kebijakan institusi.</p></div></section>
  <% } else { %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Bantuan</div><h1>Pusat bantuan repositori</h1><p>Panduan pencarian, akses berkas, dan pengajuan koreksi.</p></section><section class="repo-wrap repo-info-page"><div class="repo-card"><h2>Mencari publikasi</h2><p>Gunakan judul, nama penulis, kata kunci, abstrak, atau identifier. Hasil dapat disaring berdasarkan jenis dokumen, koleksi, dan hak akses.</p><h2>Mengunduh berkas</h2><p>Tombol unduh hanya muncul untuk berkas yang diizinkan bagi publik. Masuk melalui akun institusi bila record menyatakan akses terautentikasi.</p><h2>Sitasi</h2><p>Pada halaman detail tersedia ekspor RIS dan BibTeX yang dihasilkan dari metadata record.</p><h2>Melaporkan masalah</h2><p>Hubungi pengelola perpustakaan atau administrator institusi dengan menyertakan identifier OAI dan deskripsi masalah.</p></div></section>
  <% } %>
  </main>

  <footer class="repo-footer"><div class="repo-wrap"><div class="repo-footer-grid"><div><h2>Repositori Institusi</h2><p>Pusat pelestarian dan diseminasi karya ilmiah, data penelitian, bahan ajar, publikasi, dan dokumen akademik <%=rh(institution)%>.</p></div><div><h3>Jelajah</h3><div class="repo-footer-links"><a href="<%=root%>/repository?view=search">Koleksi dan publikasi</a><a href="<%=root%>/repository?view=search&sort=newest">Terbitan terbaru</a><a href="<%=root%>/repository?view=search&access=OPEN_ACCESS">Open access</a></div></div><div><h3>Informasi</h3><div class="repo-footer-links"><a href="<%=root%>/repository?view=policies">Kebijakan akses</a><a href="<%=root%>/repository?view=help">Pusat bantuan</a><a href="<%=root%>/oai?verb=Identify">OAI-PMH</a></div></div></div><div class="repo-footer-bottom">© <%=Calendar.getInstance().get(Calendar.YEAR)%> <%=rh(institution)%> · Aksesibilitas · Privasi</div></div></footer>
</div>
