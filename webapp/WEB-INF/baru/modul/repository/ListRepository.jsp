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
ais.database.model.Tbmuser publicUser=(ais.database.model.Tbmuser)request.getAttribute("repoPublicUser");
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
      <div class="repo-nav-actions"><%if(publicUser!=null){%><a class="repo-btn repo-btn-primary" href="<%=root%>/repository-workspace">Unggah karya</a><%}else{%><a class="repo-btn repo-btn-primary" href="<%=root%>/login2">Masuk</a><%}%></div>
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
            <div><h3><a href="<%=root%>/repository/item/<%=item.id%>"><%=rh(item.title)%></a></h3>
              <div class="repo-meta"><span><%=rh(item.authors)%></span><span>•</span><span><%=rh(item.year)%></span><span>•</span><span><%=rh(item.collectionName)%></span></div>
              <div class="repo-chips"><span class="repo-chip <%="OPEN_ACCESS".equals(item.accessPolicy) ? "repo-chip-open" : "repo-chip-warning"%>"><%=rh(rlabel(item.accessPolicy))%></span><span class="repo-chip"><%=rh(item.documentType)%></span></div>
            </div>
            <a class="repo-btn repo-btn-soft" href="<%=root%>/repository/item/<%=item.id%>">Detail</a>
          </article>
        <% } %>
        </div>
      </div>
      <aside class="repo-card repo-card-pad">
        <div class="repo-section-head"><h2>Koleksi</h2></div>
        <div class="repo-collection-list">
          <% for (CollectionView collection : collections) { %>
          <a class="repo-collection" href="<%=root%>/repository/collection/<%=collection.id%>"><span><strong><%=rh(collection.nama)%></strong><small><%=rh(collection.tipe)%></small></span><b><%=nf.format(collection.itemCount)%></b></a>
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
              + "&year=" + (query.year == null ? "" : query.year)
              + "&author=" + ru(query.author) + "&subject=" + ru(query.subject)
              + "&language=" + ru(query.language) + "&identifier=" + ru(query.identifier) + "&program=" + ru(query.programStudy)
              + "&sort=" + ru(query.sort) + "&size=" + query.pageSize;
  %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Hasil pencarian</div><h1>Hasil pencarian</h1><p><%=query.keyword.length() == 0 ? "Jelajahi seluruh publikasi yang tersedia." : "Menampilkan hasil untuk “" + rh(query.keyword) + "”."%></p></section>
    <section class="repo-wrap repo-card repo-search-panel">
      <form class="repo-search" action="<%=root%>/repository" method="get" role="search">
        <input type="hidden" name="view" value="search"><input type="hidden" name="type" value="<%=rh(query.documentType)%>"><input type="hidden" name="access" value="<%=rh(query.accessPolicy)%>"><input type="hidden" name="author" value="<%=rh(query.author)%>"><input type="hidden" name="subject" value="<%=rh(query.subject)%>"><input type="hidden" name="language" value="<%=rh(query.language)%>"><input type="hidden" name="identifier" value="<%=rh(query.identifier)%>"><input type="hidden" name="program" value="<%=rh(query.programStudy)%>">
        <label class="visually-hidden" for="repo-search-q">Kata kunci</label><input id="repo-search-q" name="q" maxlength="200" value="<%=rh(query.keyword)%>" placeholder="Cari judul, penulis, abstrak, subjek, atau identifier"><button class="repo-btn repo-btn-primary" type="submit">Cari</button>
      </form>
      <details class="repo-advanced-search" <%=(query.author.length()+query.subject.length()+query.language.length()+query.identifier.length()+query.programStudy.length()>0)?"open":""%>><summary>Pencarian lanjut</summary><form action="<%=root%>/repository/search" method="get" class="repo-form-grid"><label>Kata kunci umum<input name="q" maxlength="200" value="<%=rh(query.keyword)%>"></label><label>Penulis<input name="author" maxlength="200" value="<%=rh(query.author)%>"></label><label>Subjek/kata kunci<input name="subject" maxlength="200" value="<%=rh(query.subject)%>"></label><label>Program studi/unit<input name="program" maxlength="200" value="<%=rh(query.programStudy)%>"></label><label>Identifier/handle<input name="identifier" maxlength="255" value="<%=rh(query.identifier)%>"></label><label>Bahasa<select name="language"><option value="">Semua bahasa</option><option value="id" <%="id".equals(query.language)?"selected":""%>>Indonesia</option><option value="en" <%="en".equals(query.language)?"selected":""%>>English</option></select></label><label>Koleksi<select name="collection"><option value="">Semua koleksi</option><%for(CollectionView c:search.collections){%><option value="<%=c.id%>" <%=c.id.equals(query.collectionId)?"selected":""%>><%=rh(c.nama)%></option><%}%></select></label><div class="repo-field-full"><button class="repo-btn repo-btn-primary" type="submit">Terapkan pencarian lanjut</button> <a class="repo-btn" href="<%=root%>/repository/search">Reset</a></div></form></details>
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
        <div class="repo-facet"><h3>Tahun terbit</h3>
          <% for (Map.Entry<String, Long> facet : search.yearFacets.entrySet()) { %><a href="<%=root%>/repository?view=search&q=<%=ru(query.keyword)%>&year=<%=ru(facet.getKey())%>&type=<%=ru(query.documentType)%>&access=<%=ru(query.accessPolicy)%>"><span><%=rh(facet.getKey())%></span><b><%=nf.format(facet.getValue())%></b></a><% } %>
        </div>
        <div class="repo-facet"><h3>Koleksi</h3>
          <% for (CollectionView collection : search.collections) { if (collection.itemCount > 0) { %><a href="<%=root%>/repository?view=search&q=<%=ru(query.keyword)%>&collection=<%=collection.id%>"><span><%=rh(collection.nama)%></span><b><%=nf.format(collection.itemCount)%></b></a><% }} %>
        </div>
        <details class="repo-facet"><summary>Penulis</summary><%for(Map.Entry<String,Long> f:search.authorFacets.entrySet()){%><a href="<%=root%>/repository/search?author=<%=ru(f.getKey())%>"><span><%=rh(f.getKey())%></span><b><%=f.getValue()%></b></a><%}%></details>
        <details class="repo-facet"><summary>Subjek</summary><%for(Map.Entry<String,Long> f:search.subjectFacets.entrySet()){%><a href="<%=root%>/repository/search?subject=<%=ru(f.getKey())%>"><span><%=rh(f.getKey())%></span><b><%=f.getValue()%></b></a><%}%></details>
        <details class="repo-facet"><summary>Program studi/unit</summary><%for(Map.Entry<String,Long> f:search.programFacets.entrySet()){%><a href="<%=root%>/repository/search?program=<%=ru(f.getKey())%>"><span><%=rh(f.getKey())%></span><b><%=f.getValue()%></b></a><%}%></details>
        <details class="repo-facet"><summary>Bahasa</summary><%for(Map.Entry<String,Long> f:search.languageFacets.entrySet()){%><a href="<%=root%>/repository/search?language=<%=ru(f.getKey())%>"><span><%=rh(f.getKey())%></span><b><%=f.getValue()%></b></a><%}%></details>
        <details class="repo-facet"><summary>Sumber dan lisensi</summary><%for(Map.Entry<String,Long> f:search.sourceFacets.entrySet()){%><span class="repo-facet-static"><%=rh(f.getKey())%> <b><%=f.getValue()%></b></span><%}for(Map.Entry<String,Long> f:search.licenseFacets.entrySet()){if(f.getKey().trim().length()>0){%><span class="repo-facet-static"><%=rh(f.getKey())%> <b><%=f.getValue()%></b></span><%}}%></details>
        <div class="repo-facet"><h3>Ketersediaan naskah</h3><%for(Map.Entry<String,Long> f:search.fullTextFacets.entrySet()){%><span class="repo-facet-static"><%="WITH_FILE".equals(f.getKey())?"Dengan berkas OA":"Metadata saja"%> <b><%=f.getValue()%></b></span><%}%></div>
      </aside>
      <section aria-live="polite">
        <div class="repo-results-head"><div><strong><%=nf.format(search.total)%> hasil</strong><div class="repo-chips"><% if (query.documentType.length() > 0) { %><span class="repo-chip"><%=rh(query.documentType)%></span><% } if (query.accessPolicy.length() > 0) { %><span class="repo-chip repo-chip-open"><%=rh(rlabel(query.accessPolicy))%></span><% } if(query.author.length()>0){%><span class="repo-chip">Penulis: <%=rh(query.author)%></span><%}if(query.subject.length()>0){%><span class="repo-chip">Subjek: <%=rh(query.subject)%></span><%}if(query.programStudy.length()>0){%><span class="repo-chip">Program: <%=rh(query.programStudy)%></span><%}%></div></div>
          <div><button class="repo-btn repo-mobile-filter" type="button" data-repo-filter-toggle aria-expanded="false">Filter</button>
          <form method="get" action="<%=root%>/repository/search" style="display:inline"><input type="hidden" name="q" value="<%=rh(query.keyword)%>"><input type="hidden" name="type" value="<%=rh(query.documentType)%>"><input type="hidden" name="access" value="<%=rh(query.accessPolicy)%>"><input type="hidden" name="author" value="<%=rh(query.author)%>"><input type="hidden" name="subject" value="<%=rh(query.subject)%>"><input type="hidden" name="language" value="<%=rh(query.language)%>"><input type="hidden" name="identifier" value="<%=rh(query.identifier)%>"><input type="hidden" name="program" value="<%=rh(query.programStudy)%>"><select class="repo-btn" name="sort" onchange="this.form.submit()"><option value="newest" <%="newest".equals(query.sort) ? "selected" : ""%>>Terbaru</option><option value="oldest" <%="oldest".equals(query.sort) ? "selected" : ""%>>Terlama</option><option value="title" <%="title".equals(query.sort) ? "selected" : ""%>>Judul A–Z</option><option value="author" <%="author".equals(query.sort) ? "selected" : ""%>>Penulis A–Z</option></select></form></div>
        </div>
        <div class="repo-results">
          <% if (search.items.isEmpty()) { %><div class="repo-card repo-empty"><h2>Tidak ada hasil</h2><p>Coba gunakan kata kunci lebih singkat atau hapus sebagian filter.</p></div><% }
             for (ItemCard item : search.items) { %>
          <article class="repo-card repo-result-card"><h2><a href="<%=root%>/repository/item/<%=item.id%>"><%=rh(item.title)%></a></h2><div class="repo-authors"><%for(String a:item.authors.split(";")){if(a.trim().length()>0){%><a href="<%=root%>/repository/author/<%=ru(a.trim())%>"><%=rh(a.trim())%></a> <%}}%></div><div class="repo-meta"><span><%=rh(item.documentType)%></span><span>•</span><span><%=rh(item.year)%></span><span>•</span><span><%=rh(item.collectionName)%></span></div><p class="repo-abstract"><%=rh(item.abstractText)%></p><div class="repo-chips"><span class="repo-chip <%="OPEN_ACCESS".equals(item.accessPolicy) ? "repo-chip-open" : "repo-chip-warning"%>"><%=rh(rlabel(item.accessPolicy))%></span><% if (item.subjects.length() > 0) { %><span class="repo-chip"><%=rh(item.subjects)%></span><% } %></div><div class="repo-result-actions"><span class="repo-meta"><%=rh(item.oaiIdentifier)%></span><a class="repo-btn repo-btn-soft" href="<%=root%>/repository/item/<%=item.id%>">Lihat detail</a></div></article>
          <% } %>
        </div>
        <% if (search.totalPages > 1) { %><nav class="repo-pagination" aria-label="Halaman hasil"><% int first = Math.max(1, query.page - 2), last = Math.min(search.totalPages, query.page + 2); if (query.page > 1) { %><a href="<%=baseSearch%>&page=<%=query.page-1%>" aria-label="Halaman sebelumnya">‹</a><% } for (int pi = first; pi <= last; pi++) { if (pi == query.page) { %><span aria-current="page"><%=pi%></span><% } else { %><a href="<%=baseSearch%>&page=<%=pi%>"><%=pi%></a><% }} if (query.page < search.totalPages) { %><a href="<%=baseSearch%>&page=<%=query.page+1%>" aria-label="Halaman berikutnya">›</a><% } %></nav><% } %>
      </section>
    </div>

  <% } else if ("collection".equals(view)) { CollectionView c=(CollectionView)request.getAttribute("repoCollection"); SearchResult sr=(SearchResult)request.getAttribute("repoSearch"); %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Koleksi</div><p class="repo-eyebrow"><%=rh(c.tipe)%></p><h1><%=rh(c.nama)%></h1><p><%=rh(c.deskripsi)%></p><div class="repo-chips"><span class="repo-chip"><%=nf.format(c.itemCount)%> item publik</span></div><form class="repo-search" action="<%=root%>/repository/search" method="get"><input type="hidden" name="collection" value="<%=c.id%>"><label class="visually-hidden" for="collection-q">Cari dalam koleksi</label><input id="collection-q" name="q" placeholder="Cari dalam koleksi ini"><button class="repo-btn repo-btn-primary">Cari</button></form></section><section class="repo-wrap repo-section"><div class="repo-section-head"><h2>Publikasi terbaru</h2></div><div class="repo-results"><%for(ItemCard i:sr.items){%><article class="repo-card repo-result-card"><h2><a href="<%=root%>/repository/item/<%=i.id%>"><%=rh(i.title)%></a></h2><div class="repo-authors"><%=rh(i.authors)%></div><div class="repo-meta"><%=rh(i.year)%> · <%=rh(i.documentType)%> · <%=rh(rlabel(i.accessPolicy))%></div><p class="repo-abstract"><%=rh(i.abstractText)%></p></article><%}%></div></section>

  <% } else if ("author".equals(view)) { AuthorProfile author=(AuthorProfile)request.getAttribute("repoAuthor"); %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Penulis</div><p class="repo-eyebrow">Profil penulis</p><h1><%=rh(author.name)%></h1><p><%=nf.format(author.workCount)%> karya publik ditemukan.</p><div class="repo-chips"><%for(Map.Entry<String,Long> y:author.yearTrend.entrySet()){%><span class="repo-chip"><%=rh(y.getKey())%>: <%=y.getValue()%></span><%}%></div></section><section class="repo-wrap repo-section repo-home-grid"><div><div class="repo-results"><%for(ItemCard i:author.works){%><article class="repo-card repo-result-card"><h2><a href="<%=root%>/repository/item/<%=i.id%>"><%=rh(i.title)%></a></h2><div class="repo-meta"><%=rh(i.year)%> · <%=rh(i.documentType)%> · <%=rh(i.collectionName)%></div><p class="repo-abstract"><%=rh(i.abstractText)%></p></article><%}%></div></div><aside class="repo-card repo-card-pad"><h2>Topik</h2><div class="repo-chips"><%for(Map.Entry<String,Long> s:author.subjects.entrySet()){%><a class="repo-chip" href="<%=root%>/repository/search?subject=<%=ru(s.getKey())%>"><%=rh(s.getKey())%> (<%=s.getValue()%>)</a><%}%></div><a class="repo-btn" href="<%=root%>/repository?action=feed&format=rss&author=<%=ru(author.name)%>">RSS karya penulis</a></aside></section>

  <% } else if ("item".equals(view)) {
      ItemDetail item = (ItemDetail) request.getAttribute("repoItem"); %>
    <%if(item.withdrawn){%><div class="repo-wrap repo-alert repo-alert-warning" role="status"><strong>Record ini telah ditarik.</strong> Metadata minimum dipertahankan sebagai tombstone agar identifier tetap stabil.<%if(item.withdrawalReason.length()>0){%> Alasan: <%=rh(item.withdrawalReason)%><%}%></div><%}%>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / <a href="<%=root%>/repository?view=search">Publikasi</a> / <%=rh(item.documentType)%></div></section>
    <div class="repo-wrap repo-detail">
      <section>
        <header class="repo-card repo-paper-head"><div class="repo-chips"><span class="repo-chip"><%=rh(item.documentType)%></span><span class="repo-chip <%="OPEN_ACCESS".equals(item.accessPolicy) ? "repo-chip-open" : "repo-chip-warning"%>"><%=rh(rlabel(item.accessPolicy))%></span><% if (item.year.length() > 0) { %><span class="repo-chip">Terbit <%=rh(item.year)%></span><% } %><span class="repo-chip">Versi <%=item.versionNumber%></span></div><h1><%=rh(item.title)%></h1><div class="repo-authors"><%for(String a:item.authors.split(";")){if(a.trim().length()>0){%><a href="<%=root%>/repository/author/<%=ru(a.trim())%>"><%=rh(a.trim())%></a> <%}}%></div><div class="repo-meta"><a href="<%=root%>/repository/collection/<%=item.collectionId%>"><%=rh(item.collectionName)%></a><% if (item.publisher.length() > 0) { %><span>•</span><span><%=rh(item.publisher)%></span><% } %><span>•</span><span><%=item.viewCount%> dilihat · <%=item.downloadCount%> unduhan</span></div><div class="repo-paper-actions"><a class="repo-btn repo-btn-primary" href="<%=root%>/repository?action=citation&format=ris&id=<%=item.id%>">RIS</a><a class="repo-btn" href="<%=root%>/repository?action=citation&format=bibtex&id=<%=item.id%>">BibTeX</a><a class="repo-btn" href="<%=root%>/repository?action=citation&format=endnote&id=<%=item.id%>">EndNote</a><a class="repo-btn" href="<%=root%>/repository?action=citation&format=csl&id=<%=item.id%>">CSL-JSON</a><button class="repo-btn" type="button" data-repo-copy="<%=root%>/repository/item/<%=item.id%>">Salin tautan</button></div></header>
        <article class="repo-card repo-article"><h2>Abstrak</h2><% if (item.abstractText.length() > 0) { %><p><%=rh(item.abstractText)%></p><% } else { %><p>Abstrak belum tersedia pada metadata publik.</p><% } %><% if (item.subjects.length() > 0) { %><h2>Kata kunci</h2><div class="repo-chips"><span class="repo-chip"><%=rh(item.subjects)%></span></div><% } %></article>
        <section class="repo-card repo-article"><h2>Informasi bibliografis</h2><table class="repo-metadata"><tbody><tr><th>Identifier OAI</th><td><%=rh(item.oaiIdentifier)%></td></tr><% if (item.doi.length() > 0) { %><tr><th>DOI</th><td><%=rh(item.doi)%></td></tr><% } if (item.dspaceHandle.length() > 0) { %><tr><th>Handle</th><td><%=rh(item.dspaceHandle)%></td></tr><% } %><tr><th>Jenis dokumen</th><td><%=rh(item.documentType)%></td></tr><tr><th>Bahasa</th><td><%=rh(item.language)%></td></tr><tr><th>Koleksi</th><td><%=rh(item.collectionName)%></td></tr><%if(item.licenseUri.length()>0){%><tr><th>Lisensi</th><td><a href="<%=rh(item.licenseUri)%>" rel="license"><%=rh(item.licenseUri)%></a></td></tr><%}%><% for (Map.Entry<String, List<String>> entry : item.metadata.entrySet()) { if ("dc.title".equals(entry.getKey()) || "dc.contributor.author".equals(entry.getKey()) || "dc.description.abstract".equals(entry.getKey())) continue; %><tr><th><%=rh(entry.getKey())%></th><td><% for (String value : entry.getValue()) { %><div><%=rh(value)%></div><% } %></td></tr><% } %></tbody></table></section><%if(!item.versions.isEmpty()){%><section class="repo-card repo-article"><h2>Versi dan riwayat publik</h2><ol class="repo-timeline"><%for(ItemCard v:item.versions){%><li><strong>Versi <%=v.versionNumber%></strong><a href="<%=root%>/repository/item/<%=v.id%>"><%=rh(v.title)%></a><small><%=rh(v.year)%> · <%=rh(v.oaiIdentifier)%></small></li><%}%></ol></section><%}%><%if(!item.relatedItems.isEmpty()){%><section class="repo-card repo-article repo-related-section"><h2>Publikasi terkait</h2><div class="repo-list repo-related-list"><%for(ItemCard related:item.relatedItems){%><article class="repo-item-card"><div><h3><a href="<%=root%>/repository/item/<%=related.id%>"><%=rh(related.title)%></a></h3><div class="repo-meta"><span><%=rh(related.authors)%></span><span>•</span><span><%=rh(related.year)%></span></div></div></article><%}%></div></section><%}%>
      </section>
      <aside class="repo-card repo-file-panel"><div class="repo-file-preview">PDF</div><h2>Berkas publik</h2><% if (item.files.isEmpty()) { %><p>Metadata tersedia; naskah penuh belum tersedia untuk akses publik.</p><% } for (BitstreamView file : item.files) { %><div class="repo-file"><strong><%=rh(file.namaFile)%></strong><small><%=rh(file.mimeType)%> · <%=rh(rsize(file.ukuranByte))%><% if (file.checksum.length() > 0) { %> · checksum tersedia<% } %></small><%if("application/pdf".equalsIgnoreCase(file.mimeType)){%><a class="repo-btn" target="_blank" rel="noopener" href="<%=root%>/repository?action=download&inline=true&id=<%=file.id%>">Pratinjau aman</a><%}%><a class="repo-btn repo-btn-primary" href="<%=root%>/repository?action=download&id=<%=file.id%>">Unduh berkas</a></div><% } %><div class="repo-chips"><span class="repo-chip <%="OPEN_ACCESS".equals(item.accessPolicy) ? "repo-chip-open" : "repo-chip-warning"%>"><%=rh(rlabel(item.accessPolicy))%></span></div></aside>
    </div>

  <% } else if ("policies".equals(view)) { %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Kebijakan</div><h1>Kebijakan repositori</h1><p>Ketentuan pengiriman, metadata, akses, preservasi, privasi, dan penarikan konten.</p></section><section class="repo-wrap repo-info-page"><div class="repo-card"><h2 id="submission">Kebijakan pengiriman</h2><p>Pengiriman dilakukan oleh pengguna terautentikasi dan mengikuti review koleksi. Depositor bertanggung jawab atas keakuratan metadata serta kewenangan mendistribusikan berkas.</p><h2 id="metadata">Kebijakan metadata</h2><p>Metadata record publik dapat dipanen dan digunakan kembali dengan tetap menyebut sumber serta identifier permanen. Data akun, catatan internal, dan komentar review tidak dipublikasikan.</p><h2 id="content">Kebijakan konten</h2><p>Repositori menerima karya akademik sesuai profil koleksi. Pengelola dapat mengembalikan, menolak, atau menarik konten yang melanggar kebijakan institusi.</p><h2 id="access">Kebijakan akses terbuka</h2><p>Metadata publik dapat dijelajahi tanpa login. Akses berkas mengikuti status Open Access, khusus institusi, terautentikasi, terbatas, atau embargo.</p><h2 id="copyright">Hak cipta dan lisensi</h2><p>Hak cipta tetap berada pada pemegang hak. Pengguna wajib mengikuti lisensi yang tercantum pada record.</p><h2 id="embargo">Embargo</h2><p>Berkas berembargo tidak disediakan sebelum tanggal berakhir. Metadata dapat tetap terlihat bila kebijakan institusi mengizinkan.</p><h2 id="withdrawal">Penarikan dan takedown</h2><p>Record terbit tidak dihapus diam-diam. Record yang ditarik menyisakan tombstone dan jejak audit; laporan pelanggaran ditangani pengelola.</p><h2 id="privacy">Privasi</h2><p>Statistik publik menyimpan pseudonim hash, tipe perangkat, dan waktu kejadian; alamat jaringan mentah tidak ditampilkan sebagai statistik publik.</p><h2 id="accessibility">Aksesibilitas</h2><p>Antarmuka dirancang dapat digunakan dengan keyboard, fokus terlihat, struktur heading, label form, dan tampilan responsif. Laporkan hambatan kepada pengelola.</p><h2 id="preservation">Preservasi</h2><p>Checksum SHA-256, riwayat versi, pemeriksaan fixity, dan provenance digunakan untuk menjaga integritas aset digital.</p><h2 id="contact">Kontak dan umpan balik</h2><p>Hubungi perpustakaan atau administrator institusi dengan menyertakan URL permanen atau identifier OAI record yang dilaporkan.</p></div></section>
  <% } else { %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Bantuan</div><h1>Pusat bantuan repositori</h1><p>Panduan pencarian, akses berkas, dan pengajuan koreksi.</p></section><section class="repo-wrap repo-info-page"><div class="repo-card"><h2>Mencari publikasi</h2><p>Gunakan judul, nama penulis, kata kunci, abstrak, atau identifier. Hasil dapat disaring berdasarkan jenis dokumen, koleksi, dan hak akses.</p><h2>Mengunduh berkas</h2><p>Tombol unduh hanya muncul untuk berkas yang diizinkan bagi publik. Masuk melalui akun institusi bila record menyatakan akses terautentikasi.</p><h2>Sitasi</h2><p>Pada halaman detail tersedia ekspor RIS dan BibTeX yang dihasilkan dari metadata record.</p><h2>Melaporkan masalah</h2><p>Hubungi pengelola perpustakaan atau administrator institusi dengan menyertakan identifier OAI dan deskripsi masalah.</p></div></section>
  <% } %>
  </main>

  <footer class="repo-footer"><div class="repo-wrap"><div class="repo-footer-grid"><div><h2>Repositori Institusi</h2><p>Pusat pelestarian dan diseminasi karya ilmiah, data penelitian, bahan ajar, publikasi, dan dokumen akademik <%=rh(institution)%>.</p></div><div><h3>Jelajah</h3><div class="repo-footer-links"><a href="<%=root%>/repository/search">Koleksi dan publikasi</a><a href="<%=root%>/repository/search?sort=newest">Terbitan terbaru</a><a href="<%=root%>/repository/search?access=OPEN_ACCESS">Open access</a><a href="<%=root%>/repository/rss/recent">RSS</a><a href="<%=root%>/repository?action=feed&format=atom">Atom</a></div></div><div><h3>Informasi</h3><div class="repo-footer-links"><a href="<%=root%>/repository/policies">Kebijakan</a><a href="<%=root%>/repository/help">Pusat bantuan</a><a href="<%=root%>/oai?verb=Identify">OAI-PMH</a><a href="<%=root%>/repository/policies#accessibility">Aksesibilitas</a><a href="<%=root%>/repository/policies#privacy">Privasi</a></div></div></div><div class="repo-footer-bottom">© <%=Calendar.getInstance().get(Calendar.YEAR)%> <%=rh(institution)%></div></div></footer>
</div>
