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
private String rrelative(Date value) {
    if (value == null) return "Tanggal belum tersedia";
    long days = Math.max(0L, (System.currentTimeMillis() - value.getTime()) / 86400000L);
    if (days == 0L) return "Hari ini";
    if (days == 1L) return "Kemarin";
    if (days < 30L) return days + " hari lalu";
    if (days < 365L) return (days / 30L) + " bulan lalu";
    return new java.text.SimpleDateFormat("dd MMM yyyy", new java.util.Locale("id", "ID")).format(value);
}
private String rdate(Date value){return value==null?"":new java.text.SimpleDateFormat("dd MMM yyyy",new java.util.Locale("id","ID")).format(value);}
private String rpageHref(String base, String pageParam, int page, String anchor) {
    String separator = base.endsWith("?") || base.endsWith("&") ? "" : (base.indexOf('?') >= 0 ? "&" : "?");
    return rh(base + separator + pageParam + "=" + page + (anchor == null ? "" : anchor));
}
private String rpaging(String base, String pageParam, int currentPage, int totalPages,
        String label, String anchor, boolean compact, boolean bottom) {
    if (totalPages <= 1) return "";
    int current = Math.max(1, Math.min(currentPage, totalPages));
    int first = Math.max(1, current - 2);
    int last = Math.min(totalPages, current + 2);
    StringBuilder html = new StringBuilder();
    html.append("<nav class=\"repo-pagination");
    if (compact) html.append(" repo-pagination-compact");
    if (bottom) html.append(" repo-pagination-bottom");
    html.append("\" aria-label=\"").append(rh(label)).append("\">");
    if (current > 1) {
        html.append("<a href=\"").append(rpageHref(base, pageParam, current - 1, anchor))
                .append("\" aria-label=\"Halaman sebelumnya\">‹</a>");
    }
    if (first > 1) {
        html.append("<a href=\"").append(rpageHref(base, pageParam, 1, anchor)).append("\">1</a>");
        if (first > 2) html.append("<b class=\"repo-pagination-gap\" aria-hidden=\"true\">…</b>");
    }
    for (int pageNumber = first; pageNumber <= last; pageNumber++) {
        if (pageNumber == current) {
            html.append("<span aria-current=\"page\">").append(pageNumber).append("</span>");
        } else {
            html.append("<a href=\"").append(rpageHref(base, pageParam, pageNumber, anchor))
                    .append("\">").append(pageNumber).append("</a>");
        }
    }
    if (last < totalPages) {
        if (last < totalPages - 1) html.append("<b class=\"repo-pagination-gap\" aria-hidden=\"true\">…</b>");
        html.append("<a href=\"").append(rpageHref(base, pageParam, totalPages, anchor))
                .append("\">").append(totalPages).append("</a>");
    }
    if (current < totalPages) {
        html.append("<a href=\"").append(rpageHref(base, pageParam, current + 1, anchor))
                .append("\" aria-label=\"Halaman berikutnya\">›</a>");
    }
    return html.append("</nav>").toString();
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
boolean isReviewer=Boolean.TRUE.equals(request.getAttribute("repoIsReviewer"));
boolean isAdmin=Boolean.TRUE.equals(request.getAttribute("repoIsAdmin"));
boolean canDeposit=Boolean.TRUE.equals(request.getAttribute("repoCanDeposit"));
boolean anonymousFullText=Boolean.TRUE.equals(request.getAttribute("repoAnonymousFullText"));
String repoCsrf=request.getAttribute("repoCsrf")==null?"":String.valueOf(request.getAttribute("repoCsrf"));
String logo = "";
String institutionEmail="",institutionPhone="",institutionAddress="";
try {
    ais.database.model.PerguruanTinggi ptPublicRepo = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
    if (ptPublicRepo != null) {if(ptPublicRepo.getNama()!=null)institution=ptPublicRepo.getNama();institutionEmail=ptPublicRepo.getEmail()==null?"":ptPublicRepo.getEmail();institutionPhone=ptPublicRepo.getTelepon()==null?"":ptPublicRepo.getTelepon();institutionAddress=ptPublicRepo.getAlamat1()==null?"":ptPublicRepo.getAlamat1();}
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
      <button class="repo-menu-toggle" type="button" data-repo-menu-toggle aria-controls="repo-primary-nav" aria-expanded="false"><span aria-hidden="true">☰</span><span class="visually-hidden">Buka menu</span></button>
      <nav class="repo-links" id="repo-primary-nav" data-repo-menu aria-label="Navigasi repositori">
        <a href="<%=root%>/repository" <%="home".equals(view) ? "aria-current='page'" : ""%>>Beranda</a>
        <a href="<%=root%>/repository?view=search" <%="search".equals(view) || "browse".equals(view) ? "aria-current='page'" : ""%>>Jelajah</a>
        <a href="<%=root%>/repository/collections" <%="collections".equals(view) ? "aria-current='page'" : ""%>>Koleksi</a>
        <a href="<%=root%>/repository?view=policies" <%="policies".equals(view) ? "aria-current='page'" : ""%>>Kebijakan</a>
        <a href="<%=root%>/repository?view=help" <%="help".equals(view) ? "aria-current='page'" : ""%>>Bantuan</a>
        <a href="<%=root%>/repository/ask" <%="ask".equals(view) ? "aria-current='page'" : ""%>>Tanya Repository</a>
      </nav>
      <div class="repo-nav-actions"><%if(publicUser==null){%><a class="repo-btn repo-btn-quiet" href="<%=root%>/repository/help#upload">Panduan unggah</a><a class="repo-btn repo-btn-primary" href="<%=root%>/login2">Masuk</a><%}else if(isAdmin){%><a class="repo-btn repo-btn-quiet" href="<%=root%>/repository-workspace?view=review">Antrian review</a><a class="repo-btn repo-btn-primary" href="<%=root%>/repository-workspace?view=admin">Administrasi</a><%}else if(isReviewer){%><a class="repo-btn repo-btn-quiet" href="<%=root%>/repository-workspace?view=deposit">Karya saya</a><a class="repo-btn repo-btn-primary" href="<%=root%>/repository-workspace?view=review">Antrian review</a><%}else{%><a class="repo-btn repo-btn-quiet" href="<%=root%>/repository-workspace?view=deposit">Karya saya</a><%if(canDeposit){%><a class="repo-btn repo-btn-primary" href="<%=root%>/repository-workspace?view=deposit">Unggah karya</a><%}%><%}%></div>
    </div>
  </header>

  <main id="repo-content">
  <% if ("state".equals(view)) { Integer stateCode=(Integer)request.getAttribute("repoStateCode"); %>
    <section class="repo-wrap repo-state-page" role="alert"><div class="repo-state-code"><%=stateCode==null?"!":stateCode%></div><p class="repo-eyebrow">Status permintaan</p><h1><%=rh(String.valueOf(request.getAttribute("repoStateTitle")))%></h1><p><%=rh(String.valueOf(request.getAttribute("repoStateMessage")))%></p><div class="repo-paper-actions"><a class="repo-btn repo-btn-primary" href="<%=root%>/repository">Kembali ke repository</a><%if(stateCode!=null&&stateCode.intValue()==401){%><a class="repo-btn" href="<%=root%>/login2">Masuk kembali</a><%}%></div><small>ID permintaan: <%=rh(String.valueOf(request.getAttribute("repoRequestId")))%></small></section>
  <% } else if ("home".equals(view)) {
      List<ItemCard> latest = (List<ItemCard>) request.getAttribute("repoLatest");
      if (latest == null) latest = Collections.emptyList();
      SearchResult latestPageResult=(SearchResult)request.getAttribute("repoLatestPage");
      int latestCurrentPage=latestPageResult==null?1:latestPageResult.query.page;
      int latestTotalPages=latestPageResult==null?0:latestPageResult.totalPages;
      List<CollectionView> popularCollections=(List<CollectionView>)request.getAttribute("repoPopularCollections");
      if(popularCollections==null)popularCollections=Collections.emptyList();
      Map<String,Long> popularTopics=(Map<String,Long>)request.getAttribute("repoPopularTopics");
      if(popularTopics==null)popularTopics=Collections.emptyMap();
      List<ItemCard> recommendations=(List<ItemCard>)request.getAttribute("repoRecommendations");if(recommendations==null)recommendations=Collections.emptyList();
      List<ItemCard> featured=(List<ItemCard>)request.getAttribute("repoFeatured");if(featured==null)featured=Collections.emptyList();
      List<ItemCard> mostDownloaded=(List<ItemCard>)request.getAttribute("repoMostDownloaded");if(mostDownloaded==null)mostDownloaded=Collections.emptyList();
      List<PreferenceView> homeAlerts=(List<PreferenceView>)request.getAttribute("repoSearchAlerts");if(homeAlerts==null)homeAlerts=Collections.emptyList();
      long openFilePercent=summary==null||summary.totalItems==0?0:Math.round(summary.openFileItems*100.0d/summary.totalItems); %>
    <section class="repo-wrap repo-hero-v2" aria-labelledby="repo-hero-title">
      <div class="repo-hero-content">
        <span class="repo-eyebrow">Repositori ilmiah <%=rh(institution)%></span>
        <h1 id="repo-hero-title">Temukan karya ilmiah tepercaya dalam satu pencarian</h1>
        <p>Jelajahi skripsi, tesis, artikel, buku, bahan ajar, data penelitian, dan koleksi digital sesuai kebijakan akses institusi.</p>
        <form class="repo-search repo-search-v2" action="<%=root%>/repository" method="get" role="search" data-repo-suggest-form data-suggest-url="<%=root%>/repository?action=suggest">
          <input type="hidden" name="view" value="search">
          <label class="visually-hidden" for="repo-home-field">Bidang pencarian</label>
          <select id="repo-home-field" name="field" aria-label="Cari berdasarkan"><option value="subject">Subjek</option><option value="year">Tahun terbit</option><option value="program">Program studi</option></select>
          <span class="repo-search-input"><label class="visually-hidden" for="repo-home-q">Kata kunci</label><input id="repo-home-q" name="q" maxlength="200" autocomplete="off" aria-autocomplete="list" aria-controls="repo-home-suggestions" aria-expanded="false" placeholder="Judul, penulis, subjek, DOI, atau identifier"><button class="repo-search-clear" type="button" data-repo-search-clear aria-label="Hapus kata kunci" hidden>×</button><span class="repo-suggestions" id="repo-home-suggestions" role="listbox" hidden></span></span>
          <button class="repo-btn repo-btn-primary" type="submit">Cari</button>
        </form>
<div class="repo-popular-inline"><a href="<%=root%>/repository/search">Pencarian lebih lanjut</a><%if(!popularTopics.isEmpty()){%><span>Topik populer:</span><%int topicIndex=0;for(Map.Entry<String,Long> topic:popularTopics.entrySet()){if(topicIndex++>=4)break;%><a href="<%=root%>/repository/search?subject=<%=ru(topic.getKey())%>"><%=rh(topic.getKey())%></a><%}%><%}%></div>
      </div>
      <aside class="repo-hero-insight" aria-label="Ringkasan repositori publik">
        <div class="repo-insight-head"><span>Data publik terverifikasi</span><small>Record aktif dan berstatus terbit</small></div>
        <%if(summary!=null&&summary.totalItems>0){%><div class="repo-stat-grid">
          <a class="repo-stat" href="<%=root%>/repository/search"><b><%=nf.format(summary.totalItems)%></b><span>Karya terbit</span></a>
<a class="repo-stat" href="<%=root%>/repository/search?fullText=WITH_FILE"><b><%=nf.format(summary.openFileItems)%></b><span><%=anonymousFullText?"Full text terbuka":"Naskah untuk anggota"%></span><small><%=openFilePercent%>% dari karya terbit</small></a>
          <%if(summary.totalCollections>0){%><a class="repo-stat" href="<%=root%>/repository/search"><b><%=nf.format(summary.totalCollections)%></b><span>Koleksi aktif</span></a><%}%>
          <%if(summary.authorCount>0){%><a class="repo-stat" href="<%=root%>/repository/search?sort=author"><b><%=nf.format(summary.authorCount)%></b><span>Penulis</span></a><%}%>
</div><a class="repo-quality repo-access-breakdown repo-click-card" href="<%=root%>/repository/search" aria-label="Lihat seluruh metadata publik"><span><b>Metadata publik</b><strong><%=nf.format(summary.totalItems)%></strong></span><small><%=nf.format(summary.openFileItems)%> dengan naskah lengkap · <%=nf.format(summary.restrictedItems)%> terbatas · <%=nf.format(summary.embargoedItems)%> embargo</small></a><%if(summary.downloads30Days>0){%><a class="repo-recent-downloads repo-click-card" href="<%=root%>/repository/search?fullText=WITH_FILE"><strong><%=nf.format(summary.downloads30Days)%></strong> unduhan dalam 30 hari</a><%}%>
        <%}else{%><div class="repo-insight-empty"><strong>Repositori siap digunakan</strong><p>Statistik akan tampil setelah record pertama berstatus publik.</p></div><%}%>
      </aside>
    </section>

    <%if(summary!=null&&!summary.typeStatistics.isEmpty()){%><section class="repo-wrap repo-section" aria-labelledby="repo-type-stat-title"><div class="repo-section-head"><div><h2 id="repo-type-stat-title">Statistik jenis karya</h2><p>Jumlah record publik berdasarkan jenis atau koleksi karya.</p></div><a class="repo-link" href="<%=root%>/repository/search">Lihat seluruh karya →</a></div><div class="repo-type-stat-grid"><%for(Map.Entry<String,Long> typeStat:summary.typeStatistics.entrySet()){%><a class="repo-card repo-type-stat repo-click-card" href="<%=root%>/repository/search?q=<%=ru(typeStat.getKey())%>"><strong><%=nf.format(typeStat.getValue())%></strong><span><%=rh(typeStat.getKey())%></span></a><%}%></div></section><%}%>

    <%if(!homeAlerts.isEmpty()){%><section class="repo-wrap repo-alert repo-alert-success repo-home-alerts"><strong>Alert pencarian aktif</strong><span>Periksa publikasi terbaru untuk:</span><%for(PreferenceView alert:homeAlerts){%><a href="<%=rh(alert.queryValue)%>"><%=rh(alert.label)%></a><%}%></section><%}%>

    <section class="repo-wrap repo-section" aria-labelledby="browse-title">
      <div class="repo-section-head"><div><h2 id="browse-title">Jelajah repositori</h2><p>Masuk melalui jalur yang paling sesuai.</p></div><a class="repo-link" href="<%=root%>/repository?view=search">Lihat semua →</a></div>
      <div class="repo-browse-grid repo-browse-grid-v2">
        <a class="repo-browse-card" href="<%=root%>/repository?view=search"><span class="repo-browse-icon">▦</span><span><strong>Koleksi</strong><%if(summary!=null&&summary.totalCollections>0){%><b><%=nf.format(summary.totalCollections)%></b><%}%><small><%int ce=0;for(CollectionView pc:popularCollections){if(ce++>0){%> · <%}%><%=rh(pc.nama)%><%if(ce>=3)break;}if(ce==0){%>Kelompok karya yang aktif<%}%></small></span><i>→</i></a>
        <a class="repo-browse-card" href="<%=root%>/repository?view=search&sort=author"><span class="repo-browse-icon">◎</span><span><strong>Penulis</strong><%if(summary!=null&&summary.authorCount>0){%><b><%=nf.format(summary.authorCount)%></b><%}%><small>Telusuri nama kontributor</small></span><i>→</i></a>
        <a class="repo-browse-card" href="<%=root%>/repository?view=search&sort=newest"><span class="repo-browse-icon">◷</span><span><strong>Tahun terbit</strong><%if(summary!=null&&summary.firstYear.length()>0){%><b><%=rh(summary.firstYear+(summary.lastYear.equals(summary.firstYear)?"":"–"+summary.lastYear))%></b><%}%><small>Urutkan dari yang terbaru</small></span><i>→</i></a>
        <a class="repo-browse-card" href="<%=root%>/repository?view=search"><span class="repo-browse-icon">◇</span><span><strong>Subjek</strong><%if(summary!=null&&summary.subjectCount>0){%><b><%=nf.format(summary.subjectCount)%></b><%}%><small><%int se=0;for(Map.Entry<String,Long> pt:popularTopics.entrySet()){if(se++>0){%> · <%}%><%=rh(pt.getKey())%><%if(se>=3)break;}if(se==0){%>Topik dari metadata publik<%}%></small></span><i>→</i></a>
        <a class="repo-browse-card" href="<%=root%>/repository?view=search"><span class="repo-browse-icon">▧</span><span><strong>Jenis dokumen</strong><%if(summary!=null&&summary.documentTypeCount>0){%><b><%=nf.format(summary.documentTypeCount)%></b><%}%><small>Skripsi, artikel, buku, dan lainnya</small></span><i>→</i></a>
<a class="repo-browse-card" href="<%=root%>/repository?view=search&fullText=WITH_FILE"><span class="repo-browse-icon">○</span><span><strong>Naskah lengkap</strong><%if(summary!=null&&summary.openFileItems>0){%><b><%=nf.format(summary.openFileItems)%></b><%}%><small><%=anonymousFullText?"Dapat diunduh tanpa login":"Tersedia setelah login eCampus"%></small></span><i>→</i></a>
      </div>
    </section>

    <section class="repo-wrap repo-section repo-home-grid">
      <div class="repo-card repo-card-pad" id="publikasi-terbaru">
        <div class="repo-section-head"><h2>Publikasi terbaru</h2><a class="repo-link" href="<%=root%>/repository?view=search">Lihat semua</a></div>
        <%=rpaging(root+"/repository","latestPage",latestCurrentPage,latestTotalPages,"Halaman atas publikasi terbaru","#publikasi-terbaru",true,false)%>
        <div class="repo-list">
        <% if (latest.isEmpty()) { %><div class="repo-empty"><h3>Belum ada publikasi</h3><p>Record publik akan muncul setelah proses persetujuan dan sinkronisasi selesai.</p></div><% }
           for (ItemCard item : latest) { %>
          <a class="repo-item-card repo-click-card" href="<%=root%>/repository/item/<%=item.id%>" aria-label="Buka detail: <%=rh(item.title)%>">
            <span class="repo-doc"><%=rh(item.documentType.length() > 3 ? item.documentType.substring(0, 3).toUpperCase() : item.documentType.toUpperCase())%></span>
            <div><h3><%=rh(item.title)%></h3>
              <div class="repo-meta"><span><%=rh(item.authors)%></span><span>•</span><span><%=rh(item.year)%></span><span>•</span><span><%=rh(item.collectionName)%></span></div>
              <%if(item.abstractText.length()>0){%><p class="repo-item-abstract"><%=rh(item.abstractText.length()>190?item.abstractText.substring(0,190)+"…":item.abstractText)%></p><%}%>
              <div class="repo-chips"><span class="repo-chip <%="OPEN_ACCESS".equals(item.accessPolicy) ? "repo-chip-open" : "repo-chip-warning"%>"><%=rh(rlabel(item.accessPolicy))%><%if("EMBARGOED".equals(item.accessPolicy)&&item.embargoUntil!=null){%> sampai <%=rdate(item.embargoUntil)%><%}%></span><span class="repo-chip"><%=item.publicFileCount>0?(item.pdfAvailable?"PDF tersedia":item.publicFileCount+" berkas tersedia"):"Metadata tersedia"%></span><%if(item.superseded){%><span class="repo-chip repo-chip-warning">Digantikan versi baru</span><%}if(item.programStudy.length()>0){%><span class="repo-chip"><%=rh(item.programStudy)%></span><%}%></div>
              <div class="repo-item-foot"><span><%=rrelative(item.issuedAt)%></span><span><%=nf.format(item.viewCount)%> dilihat · <%=nf.format(item.downloadCount)%> unduhan</span></div>
              <%if(item.doi.length()>0||item.oaiIdentifier.length()>0){%><div class="repo-identifier"><%=rh(item.doi.length()>0?"DOI "+item.doi:item.oaiIdentifier)%></div><%}%>
            </div>
            <span class="repo-btn repo-btn-soft">Lihat detail</span>
          </a>
        <% } %>
        </div>
        <%=rpaging(root+"/repository","latestPage",latestCurrentPage,latestTotalPages,"Halaman bawah publikasi terbaru","#publikasi-terbaru",true,true)%>
      </div>
      <aside class="repo-card repo-card-pad">
        <div class="repo-section-head"><h2>Koleksi populer</h2></div>
        <div class="repo-collection-list">
          <% if(popularCollections.isEmpty()){%><div class="repo-empty repo-empty-compact"><p>Koleksi akan tampil setelah memiliki item publik.</p></div><%} for (CollectionView collection : popularCollections) { %>
          <a class="repo-collection" href="<%=root%>/repository/collection/<%=collection.id%>"><span><strong><%=rh(collection.nama)%></strong><small><%=rh(collection.tipe)%></small></span><b><%=nf.format(collection.itemCount)%></b></a>
          <% } %>
        </div>
        <%if(!popularTopics.isEmpty()){%><div class="repo-topic-panel"><h3>Topik populer</h3><div class="repo-chips"><%int pi=0;for(Map.Entry<String,Long> topic:popularTopics.entrySet()){if(pi++>=8)break;%><a class="repo-chip" href="<%=root%>/repository/search?subject=<%=ru(topic.getKey())%>"><%=rh(topic.getKey())%> <small><%=topic.getValue()%></small></a><%}%></div></div><%}%>
      </aside>
    </section>

    <%if(!featured.isEmpty()){%><section class="repo-wrap repo-section"><div class="repo-section-head"><div><h2>Karya unggulan</h2><p>Dipilih oleh editor repository.</p></div></div><div class="repo-recommend-grid"><%for(ItemCard f:featured){%><a class="repo-card repo-result-card repo-click-card" href="<%=root%>/repository/item/<%=f.id%>" aria-label="Buka karya unggulan: <%=rh(f.title)%>"><span class="repo-chip repo-chip-open">Pilihan editor</span><h2><%=rh(f.title)%></h2><div class="repo-meta"><%=rh(f.authors)%> · <%=rh(f.year)%></div><p class="repo-abstract"><%=rh(f.abstractText)%></p></a><%}%></div></section><%}%>
    <%if(!mostDownloaded.isEmpty()){%><section class="repo-wrap repo-section"><div class="repo-section-head"><div><h2>Paling banyak diunduh</h2><p>Hanya unduhan manusia; crawler tidak dihitung.</p></div><div class="repo-chips"><a class="repo-chip" href="<%=root%>/repository?downloadPeriod=30d">30 hari</a><a class="repo-chip" href="<%=root%>/repository?downloadPeriod=year">Tahun ini</a><a class="repo-chip" href="<%=root%>/repository">Sepanjang waktu</a></div></div><div class="repo-recommend-grid"><%for(ItemCard f:mostDownloaded){%><a class="repo-card repo-result-card repo-click-card" href="<%=root%>/repository/item/<%=f.id%>" aria-label="Buka karya: <%=rh(f.title)%>"><h2><%=rh(f.title)%></h2><div class="repo-meta"><%=rh(f.authors)%> · <%=nf.format(f.downloadCount)%> unduhan</div></a><%}%></div></section><%}%>

    <%if(!recommendations.isEmpty()){%><section class="repo-wrap repo-section"><div class="repo-section-head"><div><h2>Rekomendasi untuk Anda</h2><p>Berdasarkan subjek karya yang Anda simpan.</p></div></div><div class="repo-recommend-grid"><%for(ItemCard item:recommendations){%><a class="repo-card repo-result-card repo-click-card" href="<%=root%>/repository/item/<%=item.id%>" aria-label="Buka rekomendasi: <%=rh(item.title)%>"><h2><%=rh(item.title)%></h2><div class="repo-meta"><%=rh(item.authors)%> · <%=rh(item.year)%></div><p class="repo-abstract"><%=rh(item.abstractText)%></p><span class="repo-link">Lihat detail →</span></a><%}%></div></section><%}%>

    <section class="repo-trust" aria-label="Layanan dan standar repositori"><div class="repo-wrap repo-trust-grid"><a href="<%=root%>/repository/policies#preservation"><span>⌁</span><strong>Identifier permanen</strong><small>Tautan stabil untuk setiap record</small></a><a href="<%=root%>/repository/policies#metadata"><span>≡</span><strong>Metadata terstruktur</strong><small>Siap ditemukan dan digunakan kembali</small></a><a href="<%=root%>/repository/policies#access"><span>◉</span><strong>Akses transparan</strong><small>Status berkas dijelaskan pada record</small></a><a href="<%=root%>/oai?verb=Identify"><span>↔</span><strong>OAI-PMH</strong><small>Interoperabilitas untuk pemanen metadata</small></a></div></section>

  <% } else if("collections".equals(view)) { List<CollectionView> latestCollections=(List<CollectionView>)request.getAttribute("repoLatestCollections");if(latestCollections==null)latestCollections=Collections.emptyList();List<ItemCard> collectionLatest=(List<ItemCard>)request.getAttribute("repoLatest");if(collectionLatest==null)collectionLatest=Collections.emptyList();SearchResult collectionFacets=(SearchResult)request.getAttribute("repoCollectionFacets"); %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Koleksi</div><p class="repo-eyebrow">Jelajah koleksi</p><h1>Koleksi Repository</h1><p>Temukan koleksi terbaru atau jelajahi karya berdasarkan subjek, tahun, penulis, dan program studi.</p></section>
    <section class="repo-wrap repo-section"><div class="repo-section-head"><div><h2>Koleksi terbaru</h2><p>Koleksi aktif yang terakhir diperbarui dan telah mempunyai karya publik.</p></div></div><div class="repo-collection-catalog"><%for(CollectionView c:latestCollections){%><a class="repo-card repo-collection-catalog-card repo-click-card" href="<%=root%>/repository/collection/<%=c.id%>"><span class="repo-browse-icon">▦</span><div><strong><%=rh(c.nama)%></strong><small><%=rh(c.deskripsi.length()>150?c.deskripsi.substring(0,150)+"…":c.deskripsi)%></small></div><b><%=nf.format(c.itemCount)%> karya</b></a><%}if(latestCollections.isEmpty()){%><div class="repo-card repo-empty"><p>Belum ada koleksi publik.</p></div><%}%></div></section>
    <%if(collectionFacets!=null){%><section class="repo-wrap repo-section"><div class="repo-section-head"><div><h2>Jelajah berdasarkan kategori</h2><p>Pilih kategori untuk langsung membuka hasil yang sesuai.</p></div></div><div class="repo-category-grid"><article class="repo-card repo-card-pad"><h3>Subjek</h3><div class="repo-chips"><%int ci=0;for(Map.Entry<String,Long> f:collectionFacets.subjectFacets.entrySet()){if(ci++>=12)break;%><a class="repo-chip" href="<%=root%>/repository/search?subject=<%=ru(f.getKey())%>"><%=rh(f.getKey())%> <small><%=f.getValue()%></small></a><%}%></div></article><article class="repo-card repo-card-pad"><h3>Tahun</h3><div class="repo-chips"><%ci=0;for(Map.Entry<String,Long> f:collectionFacets.yearFacets.entrySet()){if(ci++>=12)break;%><a class="repo-chip" href="<%=root%>/repository/search?year=<%=ru(f.getKey())%>"><%=rh(f.getKey())%> <small><%=f.getValue()%></small></a><%}%></div></article><article class="repo-card repo-card-pad"><h3>Penulis</h3><div class="repo-chips"><%ci=0;for(Map.Entry<String,Long> f:collectionFacets.authorFacets.entrySet()){if(ci++>=12)break;%><a class="repo-chip" href="<%=root%>/repository/search?author=<%=ru(f.getKey())%>"><%=rh(f.getKey())%> <small><%=f.getValue()%></small></a><%}%></div></article><article class="repo-card repo-card-pad"><h3>Program studi</h3><div class="repo-chips"><%ci=0;for(Map.Entry<String,Long> f:collectionFacets.programFacets.entrySet()){if(ci++>=12)break;%><a class="repo-chip" href="<%=root%>/repository/search?program=<%=ru(f.getKey())%>"><%=rh(f.getKey())%> <small><%=f.getValue()%></small></a><%}%></div></article></div></section><%}%>
    <section class="repo-wrap repo-section"><div class="repo-section-head"><div><h2>Karya terbaru</h2><p>Publikasi terbaru dari seluruh koleksi.</p></div><a class="repo-link" href="<%=root%>/repository/search?sort=newest">Lihat semua →</a></div><div class="repo-recommend-grid"><%for(ItemCard i:collectionLatest){%><a class="repo-card repo-result-card repo-click-card" href="<%=root%>/repository/item/<%=i.id%>"><span class="repo-chip"><%=rh(i.documentType)%></span><h2><%=rh(i.title)%></h2><div class="repo-meta"><%=rh(i.authors)%> · <%=rh(i.year)%></div></a><%}%></div></section>

  <% } else if("ask".equals(view)) {
      RepositoryAnswer answer=(RepositoryAnswer)request.getAttribute("repoAnswer");
      FaqResult faq=(FaqResult)request.getAttribute("repoFaq");
      String faqBase=root+"/repository/ask?q="+ru(answer==null?"":answer.question)+"&faq="+ru(faq.keyword)+"&faqCategory="+ru(faq.category)+"&faqSize="+faq.pageSize;
  %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Tanya Repository</div><p class="repo-eyebrow">Jawaban berbasis sumber</p><h1>Tanya Repository</h1><p>Jawaban hanya disusun dari metadata karya yang tersedia dan selalu menyertakan tautan sumber.</p><form class="repo-search repo-search-v2" action="<%=root%>/repository/ask" method="get"><label class="visually-hidden" for="repo-ask-q">Pertanyaan</label><input id="repo-ask-q" name="q" maxlength="500" value="<%=answer==null?"":rh(answer.question)%>" placeholder="Apa penelitian terbaru mengenai pendidikan Al-Qur’an?"><button class="repo-btn repo-btn-primary">Cari jawaban</button></form></section>
    <%if(answer!=null&&answer.question.length()>0){%><section class="repo-wrap repo-section repo-answer-layout"><article class="repo-card repo-article"><p class="repo-eyebrow"><%=rh(answer.method)%></p><h2>Ringkasan berbasis bukti</h2><p class="repo-answer-summary"><%=rh(answer.answer)%></p><div class="repo-alert repo-alert-warning"><strong>Wajib diverifikasi.</strong> Ringkasan menggunakan metadata dan abstrak, bukan pengganti pembacaan sumber lengkap.</div></article><aside class="repo-card repo-card-pad"><h2>Sumber terpilih</h2><p class="repo-muted"><%=answer.sources.size()%> record diurutkan menurut kecocokan istilah.</p><ol class="repo-source-list"><%for(ItemCard source:answer.sources){%><li><a href="<%=root%>/repository/item/<%=source.id%>"><strong><%=rh(source.title)%></strong></a><small><%=rh(source.authors)%> · <%=rh(source.year)%></small></li><%}%></ol></aside></section><%if(!answer.evidence.isEmpty()){%><section class="repo-wrap repo-section"><div class="repo-section-head"><div><h2>Bukti dari abstrak</h2><p>Potongan yang paling dekat dengan istilah pertanyaan.</p></div></div><div class="repo-evidence-grid"><%for(AnswerEvidence evidence:answer.evidence){%><article class="repo-card repo-evidence-card"><div class="repo-chips"><span class="repo-chip"><%=rh(evidence.year)%></span><%if(evidence.matchedTerms.length()>0){%><span class="repo-chip">Istilah: <%=rh(evidence.matchedTerms)%></span><%}%></div><h3><a href="<%=root%>/repository/item/<%=evidence.itemId%>"><%=rh(evidence.title)%></a></h3><p><%=evidence.excerpt.length()>0?rh(evidence.excerpt):"Abstrak belum tersedia; buka metadata sumber untuk verifikasi."%></p><a class="repo-link" href="<%=root%>/repository/item/<%=evidence.itemId%>">Buka sumber →</a></article><%}%></div></section><%}%><%}%>
    <section class="repo-wrap repo-section" id="repository-faq" aria-labelledby="repository-faq-title">
      <div class="repo-section-head"><div><h2 id="repository-faq-title">Pusat tanya jawab Repository</h2><p>300 panduan rinci mengenai penggunaan, pengajuan, akses, kebijakan, dan pemecahan masalah.</p></div><a class="repo-link" href="<%=root%>/repository/help">Buka pusat bantuan →</a></div>
      <form class="repo-card repo-faq-controls" action="<%=root%>/repository/ask" method="get" role="search">
        <%if(answer!=null&&answer.question.length()>0){%><input type="hidden" name="q" value="<%=rh(answer.question)%>"><%}%>
        <label for="repo-faq-query">Cari dalam 300 tanya jawab<input id="repo-faq-query" name="faq" maxlength="200" value="<%=rh(faq.keyword)%>" placeholder="Contoh: unggah PDF, ORCID, embargo, metadata"></label>
        <label for="repo-faq-category">Kategori<select id="repo-faq-category" name="faqCategory"><option value="">Semua kategori (300)</option><%for(Map.Entry<String,Long> category:faq.categories.entrySet()){%><option value="<%=rh(category.getKey())%>" <%=category.getKey().equals(faq.category)?"selected":""%>><%=rh(category.getKey())%> (<%=category.getValue()%>)</option><%}%></select></label>
        <input type="hidden" name="faqSize" value="<%=faq.pageSize%>"><button class="repo-btn repo-btn-primary" type="submit">Cari panduan</button><a class="repo-btn" href="<%=root%>/repository/ask<%=answer!=null&&answer.question.length()>0?"?q="+ru(answer.question):""%>#repository-faq">Reset</a>
      </form>
      <div class="repo-faq-status" role="status"><strong><%=nf.format(faq.total)%> tanya jawab ditemukan</strong><%if(faq.totalPages>0){%><span>Halaman <%=faq.page%> dari <%=faq.totalPages%></span><%}%></div>
      <%=rpaging(faqBase,"faqPage",faq.page,faq.totalPages,"Halaman atas pusat tanya jawab","#repository-faq",false,false)%>
      <div class="repo-faq repo-faq-catalog">
        <%if(faq.items.isEmpty()){%><div class="repo-card repo-empty repo-faq-empty"><h3>Panduan tidak ditemukan</h3><p>Coba kata yang lebih singkat, pilih semua kategori, atau buka pusat bantuan.</p></div><%}%>
        <%for(FaqItem faqItem:faq.items){%><details class="repo-card" id="faq-<%=faqItem.id%>"><summary><span><small><%=rh(faqItem.category)%></small><%=rh(faqItem.question)%></span></summary><div class="repo-faq-answer"><p><%=rh(faqItem.answer)%></p><dl class="repo-faq-governance"><div><dt>Penanggung jawab</dt><dd><%=rh(faqItem.reviewedBy)%></dd></div><div><dt>Terakhir ditinjau</dt><dd><%=rh(faqItem.lastReviewed)%></dd></div><div><dt>Rujukan</dt><dd><%=rh(faqItem.policyRef)%></dd></div></dl><a href="#repository-faq" class="repo-link">Kembali ke pencarian FAQ ↑</a></div></details><%}%>
      </div>
      <%=rpaging(faqBase,"faqPage",faq.page,faq.totalPages,"Halaman bawah pusat tanya jawab","#repository-faq",false,true)%>
    </section>

  <% } else if ("search".equals(view) || "browse".equals(view)) {
      SearchResult search = (SearchResult) request.getAttribute("repoSearch");
      Query query = search.query;
      String resultMode="card".equalsIgnoreCase(request.getParameter("layout"))?"card":"list";
      List<PreferenceView> savedSearches=(List<PreferenceView>)request.getAttribute("repoSavedSearches");if(savedSearches==null)savedSearches=Collections.emptyList();
      List<PreferenceView> searchAlerts=(List<PreferenceView>)request.getAttribute("repoSearchAlerts");if(searchAlerts==null)searchAlerts=Collections.emptyList();
      String baseSearch = root + "/repository?view=search&q=" + ru(query.keyword)
              + "&field=" + ru(query.searchField)
              + "&collection=" + (query.collectionId == null ? "" : query.collectionId)
              + "&type=" + ru(query.documentType) + "&access=" + ru(query.accessPolicy)
              + "&year=" + (query.year == null ? "" : query.year)
              + "&author=" + ru(query.author) + "&subject=" + ru(query.subject)
              + "&language=" + ru(query.language) + "&identifier=" + ru(query.identifier) + "&program=" + ru(query.programStudy)
              + "&scope=" + ru(query.searchScope) + "&fullText=" + ru(query.fullText)
              + "&exact=" + ru(query.exactPhrase) + "&any=" + ru(query.anyWords) + "&without=" + ru(query.withoutWords)
              + "&yearFrom=" + (query.yearFrom == null ? "" : query.yearFrom) + "&yearUntil=" + (query.yearUntil == null ? "" : query.yearUntil)
              + "&semantic=" + query.semantic
              + "&sort=" + ru(query.sort) + "&size=" + query.pageSize + "&layout=" + ru(resultMode);
      String modeSearch=baseSearch.substring(0,baseSearch.lastIndexOf("&layout="));
  %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Hasil pencarian</div><h1>Hasil pencarian</h1><p><%=query.keyword.length() == 0 ? "Jelajahi seluruh publikasi yang tersedia." : "Menampilkan hasil untuk “" + rh(query.keyword) + "”."%></p></section>
    <section class="repo-wrap repo-card repo-search-panel">
      <form class="repo-search repo-search-v2" action="<%=root%>/repository" method="get" role="search" data-repo-suggest-form data-suggest-url="<%=root%>/repository?action=suggest">
        <input type="hidden" name="view" value="search"><input type="hidden" name="type" value="<%=rh(query.documentType)%>"><input type="hidden" name="access" value="<%=rh(query.accessPolicy)%>"><input type="hidden" name="author" value="<%=rh(query.author)%>"><input type="hidden" name="subject" value="<%=rh(query.subject)%>"><input type="hidden" name="language" value="<%=rh(query.language)%>"><input type="hidden" name="identifier" value="<%=rh(query.identifier)%>"><input type="hidden" name="program" value="<%=rh(query.programStudy)%>"><input type="hidden" name="fullText" value="<%=rh(query.fullText)%>">
        <label class="visually-hidden" for="repo-search-field">Bidang pencarian</label><select id="repo-search-field" name="field"><option value="all" <%="all".equals(query.searchField)?"selected":""%>>Semua bidang</option><option value="title" <%="title".equals(query.searchField)?"selected":""%>>Judul</option><option value="author" <%="author".equals(query.searchField)?"selected":""%>>Penulis</option><option value="abstract" <%="abstract".equals(query.searchField)?"selected":""%>>Abstrak</option><option value="subject" <%="subject".equals(query.searchField)?"selected":""%>>Kata kunci / subjek</option><option value="program" <%="program".equals(query.searchField)?"selected":""%>>Program studi</option><option value="advisor" <%="advisor".equals(query.searchField)?"selected":""%>>Pembimbing</option><option value="identifier" <%="identifier".equals(query.searchField)?"selected":""%>>DOI / Handle / Identifier</option><option value="fulltext" <%="fulltext".equals(query.searchField)?"selected":""%>>Isi dokumen</option></select><span class="repo-search-input"><label class="visually-hidden" for="repo-search-q">Kata kunci</label><input id="repo-search-q" name="q" maxlength="200" autocomplete="off" aria-autocomplete="list" aria-controls="repo-search-suggestions" aria-expanded="false" value="<%=rh(query.keyword)%>" placeholder="Cari dalam repositori"><button class="repo-search-clear" type="button" data-repo-search-clear aria-label="Hapus kata kunci" <%=query.keyword.length()==0?"hidden":""%>>×</button><span class="repo-suggestions" id="repo-search-suggestions" role="listbox" hidden></span></span><button class="repo-btn repo-btn-primary" type="submit">Cari</button>
        <fieldset class="repo-search-scope"><legend>Cakupan pencarian</legend><label><input type="radio" name="scope" value="metadata" <%="metadata".equals(query.searchScope)?"checked":""%>> Metadata saja</label><label><input type="radio" name="scope" value="all" <%="all".equals(query.searchScope)?"checked":""%>> Metadata dan isi dokumen</label><label><input type="checkbox" name="semantic" value="true" <%=query.semantic?"checked":""%>> Pencarian semantik</label></fieldset>
      </form>
      <details class="repo-advanced-search" <%=(query.author.length()+query.subject.length()+query.language.length()+query.identifier.length()+query.programStudy.length()+query.exactPhrase.length()+query.anyWords.length()+query.withoutWords.length()>0)?"open":""%>><summary>Pencarian lanjut AND / OR / NOT</summary><form action="<%=root%>/repository/search" method="get" class="repo-form-grid"><input type="hidden" name="field" value="all"><label>Semua kata (AND)<input name="q" maxlength="200" value="<%=rh(query.keyword)%>"></label><label>Frasa tepat<input name="exact" maxlength="200" value="<%=rh(query.exactPhrase)%>"></label><label>Salah satu kata (OR)<input name="any" maxlength="200" value="<%=rh(query.anyWords)%>"></label><label>Tanpa kata (NOT)<input name="without" maxlength="200" value="<%=rh(query.withoutWords)%>"></label><label>Penulis<input name="author" maxlength="200" value="<%=rh(query.author)%>"></label><label>Subjek/kata kunci<input name="subject" maxlength="200" value="<%=rh(query.subject)%>"></label><label>Program studi/unit<input name="program" maxlength="200" value="<%=rh(query.programStudy)%>"></label><label>Identifier/handle<input name="identifier" maxlength="255" value="<%=rh(query.identifier)%>"></label><label>Tahun mulai<input type="number" min="1000" max="3000" name="yearFrom" value="<%=query.yearFrom==null?"":query.yearFrom%>"></label><label>Tahun akhir<input type="number" min="1000" max="3000" name="yearUntil" value="<%=query.yearUntil==null?"":query.yearUntil%>"></label><label>Bahasa<select name="language"><option value="">Semua bahasa</option><option value="id" <%="id".equals(query.language)?"selected":""%>>Indonesia</option><option value="en" <%="en".equals(query.language)?"selected":""%>>English</option></select></label><label>Koleksi<select name="collection"><option value="">Semua koleksi</option><%for(CollectionView c:search.collections){%><option value="<%=c.id%>" <%=c.id.equals(query.collectionId)?"selected":""%>><%=rh(c.nama)%></option><%}%></select></label><div class="repo-field-full"><button class="repo-btn repo-btn-primary" type="submit">Terapkan pencarian lanjut</button> <a class="repo-btn" href="<%=root%>/repository/search">Reset</a></div></form></details>
      <%if(publicUser!=null){%><details class="repo-saved-searches"><summary>Pencarian tersimpan dan alert (<%=savedSearches.size()+searchAlerts.size()%>)</summary><div><%for(PreferenceView p:savedSearches){%><span class="repo-saved-entry"><a href="<%=rh(p.queryValue)%>"><strong><%=rh(p.label)%></strong><small>Pencarian tersimpan</small></a><form method="post" action="<%=root%>/repository"><input type="hidden" name="action" value="removePreference"><input type="hidden" name="csrf" value="<%=rh(repoCsrf)%>"><input type="hidden" name="preferenceId" value="<%=p.id%>"><button type="submit" aria-label="Hapus <%=rh(p.label)%>">×</button></form></span><%}for(PreferenceView p:searchAlerts){%><span class="repo-saved-entry"><a href="<%=rh(p.queryValue)%>"><strong><%=rh(p.label)%></strong><small>Alert pencarian aktif</small></a><form method="post" action="<%=root%>/repository"><input type="hidden" name="action" value="removePreference"><input type="hidden" name="csrf" value="<%=rh(repoCsrf)%>"><input type="hidden" name="preferenceId" value="<%=p.id%>"><button type="submit" aria-label="Hapus alert <%=rh(p.label)%>">×</button></form></span><%}if(savedSearches.isEmpty()&&searchAlerts.isEmpty()){%><p>Belum ada pencarian tersimpan.</p><%}%></div></details><%}%>
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
<div class="repo-facet"><h3>Ketersediaan naskah</h3><%for(Map.Entry<String,Long> f:search.fullTextFacets.entrySet()){%><a href="<%=root%>/repository?view=search&q=<%=ru(query.keyword)%>&field=<%=ru(query.searchField)%>&type=<%=ru(query.documentType)%>&access=<%=ru(query.accessPolicy)%>&scope=<%=ru(query.searchScope)%>&fullText=<%=ru(f.getKey())%>"><span><%="WITH_FILE".equals(f.getKey())?"Dengan naskah lengkap":"Metadata saja"%></span><b><%=f.getValue()%></b></a><%}%></div>
      </aside>
      <section id="repo-search-results" aria-live="polite">
        <div class="repo-results-head"><div><strong><%=nf.format(search.total)%> hasil</strong><div class="repo-chips"><% if (query.documentType.length() > 0) { %><span class="repo-chip"><%=rh(query.documentType)%></span><% } if (query.accessPolicy.length() > 0) { %><span class="repo-chip repo-chip-open"><%=rh(rlabel(query.accessPolicy))%></span><% } if(query.author.length()>0){%><span class="repo-chip">Penulis: <%=rh(query.author)%></span><%}if(query.subject.length()>0){%><span class="repo-chip">Subjek: <%=rh(query.subject)%></span><%}if(query.programStudy.length()>0){%><span class="repo-chip">Program: <%=rh(query.programStudy)%></span><%}if(query.fullText.length()>0){%><span class="repo-chip"><%="WITH_FILE".equals(query.fullText)?"Full text tersedia":"Metadata saja"%></span><%}%></div></div>
          <div class="repo-result-tools"><button class="repo-btn repo-mobile-filter" type="button" data-repo-filter-toggle aria-expanded="false">Filter</button><a class="repo-btn <%="list".equals(resultMode)?"is-active":""%>" href="<%=modeSearch%>&layout=list">Daftar</a><a class="repo-btn <%="card".equals(resultMode)?"is-active":""%>" href="<%=modeSearch%>&layout=card">Kartu</a>
          <form method="get" action="<%=root%>/repository/search" style="display:inline"><input type="hidden" name="q" value="<%=rh(query.keyword)%>"><input type="hidden" name="field" value="<%=rh(query.searchField)%>"><input type="hidden" name="type" value="<%=rh(query.documentType)%>"><input type="hidden" name="access" value="<%=rh(query.accessPolicy)%>"><input type="hidden" name="author" value="<%=rh(query.author)%>"><input type="hidden" name="subject" value="<%=rh(query.subject)%>"><input type="hidden" name="language" value="<%=rh(query.language)%>"><input type="hidden" name="identifier" value="<%=rh(query.identifier)%>"><input type="hidden" name="program" value="<%=rh(query.programStudy)%>"><input type="hidden" name="scope" value="<%=rh(query.searchScope)%>"><input type="hidden" name="fullText" value="<%=rh(query.fullText)%>"><select class="repo-btn" name="sort" onchange="this.form.submit()"><option value="newest" <%="newest".equals(query.sort) ? "selected" : ""%>>Terbaru</option><option value="oldest" <%="oldest".equals(query.sort) ? "selected" : ""%>>Terlama</option><option value="title" <%="title".equals(query.sort) ? "selected" : ""%>>Judul A–Z</option><option value="author" <%="author".equals(query.sort) ? "selected" : ""%>>Penulis A–Z</option></select></form></div>
        </div>
        <%if(publicUser!=null){%><form class="repo-save-search" action="<%=root%>/repository" method="post"><input type="hidden" name="action" value="saveSearch"><input type="hidden" name="csrf" value="<%=rh(repoCsrf)%>"><input type="hidden" name="queryValue" value="<%=rh(baseSearch)%>"><input type="hidden" name="returnTo" value="<%=rh(baseSearch)%>"><label>Nama pencarian<input name="label" maxlength="255" value="<%=rh(query.keyword.length()>0?query.keyword:"Semua publikasi")%>"></label><label class="repo-check-inline"><input type="checkbox" name="alert" value="true"> Aktifkan alert</label><button class="repo-btn" type="submit">Simpan pencarian</button></form><%}%>
        <%=rpaging(baseSearch,"page",query.page,search.totalPages,"Halaman atas hasil pencarian","#repo-search-results",false,false)%>
        <div class="repo-results <%="card".equals(resultMode)?"repo-results-card-mode":""%>">
          <div class="repo-search-skeleton" data-repo-search-skeleton hidden aria-hidden="true"><i></i><i></i><i></i></div>
          <% if (search.items.isEmpty()) { %><div class="repo-card repo-empty"><h2>Tidak ada hasil</h2><p>Coba gunakan kata kunci lebih singkat, periksa ejaan, atau hapus sebagian filter.</p><%if(search.didYouMean.length()>0){%><p class="repo-did-you-mean">Apakah yang Anda maksud: <a href="<%=root%>/repository/search?q=<%=ru(search.didYouMean)%>"><strong><%=rh(search.didYouMean)%></strong></a>?</p><%}%><div class="repo-paper-actions"><a class="repo-btn repo-btn-primary" href="<%=root%>/repository/search">Hapus semua filter</a><a class="repo-btn" href="<%=root%>/repository/search?sort=newest">Lihat publikasi terbaru</a><a class="repo-btn" href="<%=root%>/repository/search?fullText=WITH_FILE">Jelajahi full text terbuka</a></div></div><% }
             for (ItemCard item : search.items) { %>
          <article class="repo-card repo-result-card" data-repo-card-href="<%=root%>/repository/item/<%=item.id%>"><div class="repo-chips"><span class="repo-chip"><%=rh(item.documentType)%></span><span class="repo-chip <%="OPEN_ACCESS".equals(item.accessPolicy) ? "repo-chip-open" : "repo-chip-warning"%>"><%=rh(rlabel(item.accessPolicy))%></span><span class="repo-chip"><%=item.publicFileCount>0?(item.pdfAvailable?"PDF tersedia":item.publicFileCount+" berkas tersedia"):"Metadata saja"%></span></div><h2><a href="<%=root%>/repository/item/<%=item.id%>"><%=rh(item.title)%></a></h2><div class="repo-authors"><%for(String a:item.authors.split(";")){if(a.trim().length()>0){%><a href="<%=root%>/repository/author/<%=ru(a.trim())%>"><%=rh(a.trim())%></a> <%}}%></div><div class="repo-meta"><%if(item.programStudy.length()>0){%><span><%=rh(item.programStudy)%></span><span>•</span><%}%><span><%=rh(item.year)%></span><span>•</span><span><%=rh(item.collectionName)%></span></div><p class="repo-abstract"><%=rh(item.abstractText)%></p><div class="repo-chips"><% if (item.subjects.length() > 0) { for(String subject:item.subjects.split("[;,]")){if(subject.trim().length()>0){%><a class="repo-chip" href="<%=root%>/repository/search?subject=<%=ru(subject.trim())%>"><%=rh(subject.trim())%></a><%}} } %></div><div class="repo-result-actions"><span class="repo-meta"><%if(item.doi.length()>0){%>DOI <%=rh(item.doi)%> · <%}%><%=nf.format(item.viewCount)%> dilihat · <%=nf.format(item.downloadCount)%> unduhan</span><a class="repo-btn" href="<%=root%>/repository?action=citation&format=apa&id=<%=item.id%>">Sitasi</a><a class="repo-btn repo-btn-soft" href="<%=root%>/repository/item/<%=item.id%>">Detail</a></div></article>
          <% } %>
        </div>
        <%=rpaging(baseSearch,"page",query.page,search.totalPages,"Halaman bawah hasil pencarian","#repo-search-results",false,true)%>
      </section>
    </div>

  <% } else if ("collection".equals(view)) {
      CollectionView c=(CollectionView)request.getAttribute("repoCollection");
      SearchResult sr=(SearchResult)request.getAttribute("repoSearch");
      Query collectionQuery=sr.query;
      String collectionBase=root+"/repository/collection/"+c.id+"?q="+ru(collectionQuery.keyword)+"&sort="+ru(collectionQuery.sort)+"&size="+collectionQuery.pageSize;
  %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Koleksi</div><p class="repo-eyebrow"><%=rh(c.tipe)%></p><h1><%=rh(c.nama)%></h1><p><%=rh(c.deskripsi)%></p><div class="repo-chips"><span class="repo-chip"><%=nf.format(c.itemCount)%> item publik</span></div><form class="repo-search" action="<%=root%>/repository/search" method="get"><input type="hidden" name="collection" value="<%=c.id%>"><label class="visually-hidden" for="collection-q">Cari dalam koleksi</label><input id="collection-q" name="q" placeholder="Cari dalam koleksi ini"><button class="repo-btn repo-btn-primary">Cari</button></form></section>
    <section class="repo-wrap repo-section" id="collection-publications" aria-live="polite">
      <div class="repo-section-head"><h2>Publikasi</h2><span class="repo-meta">Halaman <%=collectionQuery.page%> dari <%=Math.max(1,sr.totalPages)%></span></div>
      <%=rpaging(collectionBase,"page",collectionQuery.page,sr.totalPages,"Halaman atas publikasi koleksi","#collection-publications",false,false)%>
      <div class="repo-results">
        <%if(sr.items.isEmpty()){%><div class="repo-card repo-empty"><h2>Belum ada publikasi</h2><p>Belum ada item publik yang dapat ditampilkan dalam koleksi ini.</p></div><%}%>
        <%for(ItemCard i:sr.items){%><article class="repo-card repo-result-card" data-repo-card-href="<%=root%>/repository/item/<%=i.id%>"><h2><a href="<%=root%>/repository/item/<%=i.id%>"><%=rh(i.title)%></a></h2><div class="repo-authors"><%=rh(i.authors)%></div><div class="repo-meta"><%=rh(i.year)%> · <%=rh(i.documentType)%> · <%=rh(rlabel(i.accessPolicy))%></div><p class="repo-abstract"><%=rh(i.abstractText)%></p></article><%}%>
      </div>
      <%=rpaging(collectionBase,"page",collectionQuery.page,sr.totalPages,"Halaman bawah publikasi koleksi","#collection-publications",false,true)%>
    </section>

  <% } else if ("author".equals(view)) {
      AuthorProfile author=(AuthorProfile)request.getAttribute("repoAuthor");
      String authorBase=root+"/repository/author/"+ru(author.queryName.length()>0?author.queryName:author.name)+"?size="+author.pageSize;
  %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Penulis</div><p class="repo-eyebrow">Profil penulis <%if(author.verified){%>· Terverifikasi<%}%></p><h1><%=rh(author.name)%></h1><p><%=nf.format(author.workCount)%> karya publik ditemukan.<%if(author.affiliation.length()>0){%> · <%=rh(author.affiliation)%><%}%></p><div class="repo-chips"><%if(author.orcid.length()>0){%><a class="repo-chip repo-chip-open" href="https://orcid.org/<%=rh(author.orcid)%>" rel="me noopener" target="_blank">ORCID <%=rh(author.orcid)%></a><%}if(author.rorId.length()>0){%><a class="repo-chip" href="https://ror.org/<%=rh(author.rorId.replace("https://ror.org/",""))%>" rel="noopener" target="_blank">ROR</a><%}for(Map.Entry<String,Long> y:author.yearTrend.entrySet()){%><span class="repo-chip"><%=rh(y.getKey())%>: <%=y.getValue()%></span><%}%></div></section>
    <section class="repo-wrap repo-section repo-home-grid" id="author-works">
      <div>
        <%=rpaging(authorBase,"page",author.currentPage,author.totalPages,"Halaman atas karya penulis","#author-works",false,false)%>
        <div class="repo-results"><%if(author.works.isEmpty()){%><div class="repo-card repo-empty"><h2>Belum ada karya</h2><p>Belum ada karya publik yang dapat ditampilkan untuk penulis ini.</p></div><%}%><%for(ItemCard i:author.works){%><article class="repo-card repo-result-card" data-repo-card-href="<%=root%>/repository/item/<%=i.id%>"><h2><a href="<%=root%>/repository/item/<%=i.id%>"><%=rh(i.title)%></a></h2><div class="repo-meta"><%=rh(i.year)%> · <%=rh(i.documentType)%> · <%=rh(i.collectionName)%></div><p class="repo-abstract"><%=rh(i.abstractText)%></p></article><%}%></div>
        <%=rpaging(authorBase,"page",author.currentPage,author.totalPages,"Halaman bawah karya penulis","#author-works",false,true)%>
      </div>
      <aside class="repo-card repo-card-pad"><%if(author.nameVariants.length()>0){%><h2>Variasi nama</h2><p><%=rh(author.nameVariants.replace("\n"," · "))%></p><%}%><h2>Topik</h2><div class="repo-chips"><%for(Map.Entry<String,Long> s:author.subjects.entrySet()){%><a class="repo-chip" href="<%=root%>/repository/search?subject=<%=ru(s.getKey())%>"><%=rh(s.getKey())%> (<%=s.getValue()%>)</a><%}%></div><a class="repo-btn" href="<%=root%>/repository?action=feed&format=rss&author=<%=ru(author.name)%>">RSS karya penulis</a></aside>
    </section>

    <%if(isAdmin&&author.authorityId!=null){%><section class="repo-wrap repo-card repo-card-pad repo-authority-admin"><p class="repo-eyebrow">Authority control</p><h2>Verifikasi identitas penulis</h2><div class="repo-paper-actions"><a class="repo-btn repo-btn-primary" href="<%=root%>/repository-workspace?action=orcidStart&authorityId=<%=author.authorityId%>">Autentikasi ORCID</a><a class="repo-btn" href="<%=root%>/repository-workspace?view=admin">Kelola ROR dan authority</a></div></section><%}%>

  <% } else if ("item".equals(view)) {
      ItemDetail item = (ItemDetail) request.getAttribute("repoItem"); boolean bookmarked=Boolean.TRUE.equals(request.getAttribute("repoBookmarked")); %>
    <%if(item.withdrawn){%><div class="repo-wrap repo-alert repo-alert-warning" role="status"><strong>Record ini telah ditarik.</strong> Metadata minimum dipertahankan sebagai tombstone agar identifier tetap stabil.<%if(item.withdrawalReason.length()>0){%> Alasan: <%=rh(item.withdrawalReason)%><%}%></div><%}%>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / <a href="<%=root%>/repository?view=search">Publikasi</a> / <%=rh(item.documentType)%></div></section>
    <div class="repo-wrap repo-detail">
      <section>
        <header class="repo-card repo-paper-head"><div class="repo-chips"><span class="repo-chip"><%=rh(item.documentType)%></span><span class="repo-chip <%="OPEN_ACCESS".equals(item.accessPolicy) ? "repo-chip-open" : "repo-chip-warning"%>"><%=rh(rlabel(item.accessPolicy))%><%if("EMBARGOED".equals(item.accessPolicy)&&item.embargoUntil!=null){%> sampai <%=rdate(item.embargoUntil)%><%}%></span><%if(item.licenseUri.length()>0){%><a class="repo-chip repo-chip-open" href="<%=rh(item.licenseUri)%>" rel="license">Lisensi terbuka</a><%}%><% if (item.year.length() > 0) { %><span class="repo-chip">Terbit <%=rh(item.year)%></span><% } %><span class="repo-chip">Versi <%=item.versionNumber%></span></div><h1><%=rh(item.title)%></h1><div class="repo-authors"><%for(String a:item.authors.split(";")){if(a.trim().length()>0){%><a href="<%=root%>/repository/author/<%=ru(a.trim())%>"><%=rh(a.trim())%></a> <%}}%></div><div class="repo-meta"><a href="<%=root%>/repository/collection/<%=item.collectionId%>"><%=rh(item.collectionName)%></a><% if (item.publisher.length() > 0) { %><span>•</span><span><%=rh(item.publisher)%></span><% } %><span>•</span><span><%=item.viewCount%> dilihat · <%=item.downloadCount%> unduhan</span></div><div class="repo-paper-actions"><details class="repo-citation-menu"><summary class="repo-btn repo-btn-primary">Sitasi</summary><div><a href="<%=root%>/repository?action=citation&format=apa&id=<%=item.id%>">APA</a><a href="<%=root%>/repository?action=citation&format=ieee&id=<%=item.id%>">IEEE</a><a href="<%=root%>/repository?action=citation&format=harvard&id=<%=item.id%>">Harvard</a><a href="<%=root%>/repository?action=citation&format=vancouver&id=<%=item.id%>">Vancouver</a></div></details><a class="repo-btn" href="<%=root%>/repository?action=citation&format=ris&id=<%=item.id%>">RIS</a><a class="repo-btn" href="<%=root%>/repository?action=citation&format=bibtex&id=<%=item.id%>">BibTeX</a><a class="repo-btn" href="<%=root%>/repository?action=citation&format=endnote&id=<%=item.id%>">EndNote</a><a class="repo-btn" href="<%=root%>/repository?action=citation&format=csl&id=<%=item.id%>">CSL-JSON</a><button class="repo-btn" type="button" data-repo-copy="<%=root%>/repository/item/<%=item.id%>">Salin tautan</button><%if(publicUser!=null){%><form method="post" action="<%=root%>/repository"><input type="hidden" name="action" value="bookmark"><input type="hidden" name="csrf" value="<%=rh(repoCsrf)%>"><input type="hidden" name="id" value="<%=item.id%>"><button class="repo-btn" type="submit"><%=bookmarked?"Hapus bookmark":"Simpan bookmark"%></button></form><%}%></div></header>
        <article class="repo-card repo-article"><h2>Abstrak</h2><% if (item.abstractText.length() > 0) { %><p><%=rh(item.abstractText)%></p><% } else { %><p>Abstrak belum tersedia pada metadata publik.</p><% } %><% if (item.subjects.length() > 0) { %><h2>Kata kunci</h2><div class="repo-chips"><span class="repo-chip"><%=rh(item.subjects)%></span></div><% } %></article>
        <section class="repo-card repo-article"><h2>Informasi bibliografis</h2><table class="repo-metadata"><tbody><tr><th>Identifier OAI</th><td><%=rh(item.oaiIdentifier)%></td></tr><% if (item.doi.length() > 0) { %><tr><th>DOI</th><td><%=rh(item.doi)%></td></tr><% } if (item.dspaceHandle.length() > 0) { %><tr><th>Handle</th><td><%=rh(item.dspaceHandle)%></td></tr><% } %><tr><th>Jenis dokumen</th><td><%=rh(item.documentType)%></td></tr><tr><th>Bahasa</th><td><%=rh(item.language)%></td></tr><tr><th>Koleksi</th><td><%=rh(item.collectionName)%></td></tr><%if(item.licenseUri.length()>0){%><tr><th>Lisensi</th><td><a href="<%=rh(item.licenseUri)%>" rel="license"><%=rh(item.licenseUri)%></a></td></tr><%}%><% for (Map.Entry<String, List<String>> entry : item.metadata.entrySet()) { if ("dc.title".equals(entry.getKey()) || "dc.contributor.author".equals(entry.getKey()) || "dc.description.abstract".equals(entry.getKey())) continue; %><tr><th><%=rh(entry.getKey())%></th><td><% for (String value : entry.getValue()) { %><div><%=rh(value)%></div><% } %></td></tr><% } %></tbody></table></section><%if(!item.versions.isEmpty()){%><section class="repo-card repo-article"><h2>Versi dan riwayat publik</h2><ol class="repo-timeline"><%for(ItemCard v:item.versions){%><li><strong>Versi <%=v.versionNumber%></strong><a href="<%=root%>/repository/item/<%=v.id%>"><%=rh(v.title)%></a><small><%=rh(v.year)%> · <%=rh(v.oaiIdentifier)%></small></li><%}%></ol></section><%}%><%if(!item.relatedItems.isEmpty()){%><section class="repo-card repo-article repo-related-section"><h2>Publikasi terkait</h2><div class="repo-list repo-related-list"><%for(ItemCard related:item.relatedItems){%><article class="repo-item-card" data-repo-card-href="<%=root%>/repository/item/<%=related.id%>"><div><h3><a href="<%=root%>/repository/item/<%=related.id%>"><%=rh(related.title)%></a></h3><div class="repo-meta"><span><%=rh(related.authors)%></span><span>•</span><span><%=rh(related.year)%></span></div></div></article><%}%></div></section><%}%>
      </section>
      <aside class="repo-card repo-file-panel"><div class="repo-file-preview">PDF</div><h2>Naskah lengkap</h2><%if(publicUser==null&&!anonymousFullText&&item.publicFileCount>0){%><p>Naskah tersedia untuk anggota. Login menggunakan akun eCampus untuk melihat hak akses berkas.</p><a class="repo-btn repo-btn-primary" href="<%=root%>/login2">Login eCampus</a><%}else if(item.files.isEmpty()){%><p>Metadata tersedia; naskah lengkap belum tersedia atau aksesnya dibatasi.</p><%}for(BitstreamView file:item.files){%><div class="repo-file"><strong><%=rh(file.namaFile)%></strong><small><%=rh(file.mimeType)%> · <%=rh(rsize(file.ukuranByte))%><%if(file.checksum.length()>0){%> · checksum tersedia<%}%></small><%if("application/pdf".equalsIgnoreCase(file.mimeType)){%><a class="repo-btn" target="_blank" rel="noopener" href="<%=root%>/repository?action=download&inline=true&id=<%=file.id%>">Pratinjau aman</a><%}%><a class="repo-btn repo-btn-primary" href="<%=root%>/repository?action=download&id=<%=file.id%>">Unduh berkas</a></div><%}%><div class="repo-chips"><span class="repo-chip <%="OPEN_ACCESS".equals(item.accessPolicy) ? "repo-chip-open" : "repo-chip-warning"%>"><%=rh(rlabel(item.accessPolicy))%></span></div></aside>
    </div>
    <%if(!item.files.isEmpty()){%><div class="repo-mobile-sticky-action"><a class="repo-btn repo-btn-primary" href="<%=root%>/repository?action=download&id=<%=item.files.get(0).id%>">Unduh berkas utama</a></div><%}%>

  <% } else if ("policies".equals(view)) { %>
    <section class="repo-wrap repo-page-head"><div class="repo-breadcrumb"><a href="<%=root%>/repository">Beranda</a> / Kebijakan</div><h1>Kebijakan repositori</h1><p>Ketentuan pengiriman, metadata, akses, preservasi, privasi, dan penarikan konten.</p></section><section class="repo-wrap repo-info-page"><div class="repo-card"><h2 id="submission">Kebijakan pengiriman</h2><p>Pengiriman dilakukan oleh pengguna terautentikasi dan mengikuti review koleksi. Depositor bertanggung jawab atas keakuratan metadata serta kewenangan mendistribusikan berkas.</p><h2 id="metadata">Kebijakan metadata</h2><p>Metadata record publik dapat dipanen dan digunakan kembali dengan tetap menyebut sumber serta identifier permanen. Data akun, catatan internal, dan komentar review tidak dipublikasikan.</p><h2 id="content">Kebijakan konten</h2><p>Repositori menerima karya akademik sesuai profil koleksi. Pengelola dapat mengembalikan, menolak, atau menarik konten yang melanggar kebijakan institusi.</p><h2 id="access">Kebijakan akses terbuka</h2><p>Metadata publik dapat dijelajahi tanpa login. Akses berkas mengikuti status Open Access, khusus institusi, terautentikasi, terbatas, atau embargo.</p><h2 id="copyright">Hak cipta dan lisensi</h2><p>Hak cipta tetap berada pada pemegang hak. Pengguna wajib mengikuti lisensi yang tercantum pada record.</p><h2 id="embargo">Embargo</h2><p>Berkas berembargo tidak disediakan sebelum tanggal berakhir. Metadata dapat tetap terlihat bila kebijakan institusi mengizinkan.</p><h2 id="withdrawal">Penarikan dan takedown</h2><p>Record terbit tidak dihapus diam-diam. Record yang ditarik menyisakan tombstone dan jejak audit; laporan pelanggaran ditangani pengelola.</p><h2 id="privacy">Privasi</h2><p>Statistik publik menyimpan pseudonim hash, tipe perangkat, dan waktu kejadian; alamat jaringan mentah tidak ditampilkan sebagai statistik publik.</p><h2 id="accessibility">Aksesibilitas</h2><p>Antarmuka dirancang dapat digunakan dengan keyboard, fokus terlihat, struktur heading, label form, dan tampilan responsif. Laporkan hambatan kepada pengelola.</p><h2 id="preservation">Preservasi</h2><p>Checksum SHA-256, riwayat versi, pemeriksaan fixity, dan provenance digunakan untuk menjaga integritas aset digital.</p><h2 id="contact">Kontak dan umpan balik</h2><p>Hubungi perpustakaan atau administrator institusi dengan menyertakan URL permanen atau identifier OAI record yang dilaporkan.</p></div></section>
    <section class="repo-wrap repo-section repo-policy-groups"><article class="repo-card repo-article"><h2>Kebijakan Metadata</h2><ol><li>Metadata karya dapat diakses tanpa biaya untuk keperluan penemuan informasi.</li><li>Pemilik dan pengelola metadata adalah <%=rh(institution)%>.</li><li>Metadata dapat digunakan kembali untuk tujuan nonkomersial dengan mencantumkan sumber dan tautan identifier permanen.</li><li>Pengumpulan metadata secara sistematis menggunakan OAI-PMH atau izin resmi pengelola.</li></ol></article><article class="repo-card repo-article"><h2>Kebijakan Data dan Naskah Lengkap</h2><ol><li>Metadata dan abstrak tersedia bagi pengguna umum.</li><li>Naskah lengkap tersedia bagi pengguna eCampus yang telah login dan tetap mengikuti status akses, lisensi, serta embargo setiap karya.</li><li>Penggunaan untuk penelitian, pendidikan, dan tujuan nonkomersial wajib menyebut penulis, judul, serta sumber.</li><li>Naskah tidak boleh dijual atau didistribusikan secara komersial tanpa izin pemegang hak.</li></ol></article><article class="repo-card repo-article"><h2>Kebijakan Konten</h2><ol><li>Repository menyimpan karya akademik dan ilmiah sesuai profil koleksi institusi.</li><li>Konten dapat berupa skripsi, tesis, disertasi, karya dosen, laporan penelitian, prosiding, artikel, buku, bahan ajar, dataset, dan materi pendukung.</li><li>Bahasa utama adalah Bahasa Indonesia dan Bahasa Inggris; bahasa lain mengikuti kebijakan koleksi.</li></ol></article><article class="repo-card repo-article"><h2>Kebijakan Pengajuan</h2><ol><li>Deposit dilakukan oleh civitas akademika yang menggunakan akun eCampus.</li><li>Depositor bertanggung jawab atas keaslian, kelengkapan metadata, hak cipta, dan izin distribusi.</li><li>Pengelola memeriksa format, watermark, metadata, duplikasi, dan kelayakan akses sebelum publikasi.</li><li>Pelanggaran hak cipta ditangani melalui takedown; identifier dan jejak audit dipertahankan sebagai tombstone bila diperlukan.</li></ol></article><article class="repo-card repo-article"><h2>Kebijakan Pelestarian</h2><ol><li>Karya disimpan untuk pelestarian jangka panjang sesuai kebijakan institusi.</li><li>Checksum, fixity, versi file, dan provenance digunakan untuk menjaga integritas.</li><li>Perubahan terhadap karya terbit dilakukan melalui versi baru atau proses penarikan yang tercatat.</li><li>Pengelola dapat melakukan migrasi format untuk mempertahankan keterbacaan tanpa mengubah substansi.</li></ol></article></section>
  <% } else { %>
    <%@ include file="_repository_help.jsp" %>
  <% } %>
  </main>

  <footer class="repo-footer"><div class="repo-wrap"><div class="repo-footer-grid"><div><h2>Repositori Institusi</h2><p>Pusat pelestarian dan diseminasi karya ilmiah, data penelitian, bahan ajar, publikasi, dan dokumen akademik <%=rh(institution)%>.</p></div><div><h3>Jelajah</h3><div class="repo-footer-links"><a href="<%=root%>/repository/search">Koleksi dan publikasi</a><a href="<%=root%>/repository/search?sort=newest">Terbitan terbaru</a><a href="<%=root%>/repository/search?access=OPEN_ACCESS">Open access</a><a href="<%=root%>/repository/rss/recent">RSS</a><a href="<%=root%>/repository?action=feed&format=atom">Atom</a></div></div><div><h3>Informasi</h3><div class="repo-footer-links"><a href="<%=root%>/repository/policies">Kebijakan</a><a href="<%=root%>/repository/help">Pusat bantuan</a><a href="<%=root%>/oai?verb=Identify">OAI-PMH</a><a href="<%=root%>/repository/policies#accessibility">Aksesibilitas</a><a href="<%=root%>/repository/policies#privacy">Privasi</a></div></div></div><div class="repo-footer-bottom">© <%=Calendar.getInstance().get(Calendar.YEAR)%> <%=rh(institution)%></div></div></footer>
</div>
