<%@page import="ais.common.Common"%>
<%@page import="ais.action.master.library.modern.LibraryItemDetailService"%>
<%@page import="ais.common.newui.NewUiCsrfUtil"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>

<%
    // Mengambil dan memvalidasi parameter dari HTTP Request
    String idStr = request.getParameter("id");
    String modalId = request.getParameter("modalId");
    
    if (modalId == null || modalId.trim().isEmpty()) {
        modalId = "modalDetail";
    }
    modalId = modalId.replaceAll("[^A-Za-z0-9_]", "");
    if (modalId.length() == 0) modalId = "modalDetail";

    LibraryItemDetailService.Detail item = null;
    try {
        if (idStr != null && !idStr.trim().isEmpty()) {
            item = LibraryItemDetailService.find(Long.valueOf(idStr));
        }
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pustaka/_item_rinci.jsp:27");
        // Kesalahan parsing diabaikan agar sistem tidak crash, objek item tetap null
    }

    // Penanganan jika data pustaka tidak ditemukan, telah dihapus, atau dinonaktifkan
    if (item == null) {
        out.print("<div class='modal fade' id='" + modalId + "' tabindex='-1' aria-hidden='true'><div class='modal-dialog modal-dialog-centered'><div class='modal-content border-0 shadow rounded-4 p-4 text-center'><h5 class='text-danger mb-0'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Data Pustaka tidak ditemukan.") + "</h5></div></div></div>");
        return;
    }

    // Persiapan variabel data dengan validasi Null (Fallback ke strip "-")
    boolean punyaCoverAsli = item.getImageUrl() != null && item.getImageUrl().trim().startsWith("http");
    String coverImg = punyaCoverAsli
        ? item.getImageUrl() 
        : Common.getRequestHostWithProtocol() + "/library/item-cover?id=" + item.getId();
    
    String judul = item.getTitle() != null && !item.getTitle().trim().isEmpty() ? item.getTitle() : Common.getBahasaConfig("Tanpa Judul");
    String pengarang = item.getAuthors() != null && !item.getAuthors().trim().isEmpty() ? item.getAuthors() : "-";
    String penerbit = item.getPublisher() != null ? item.getPublisher() : "-";
    String kategori = item.getCategory() != null && !item.getCategory().trim().isEmpty() ? item.getCategory() : "-";
    String klasifikasi = item.getClassification() != null && !item.getClassification().trim().isEmpty() ? item.getClassification() : "-";
    String tema = item.getTheme() != null && !item.getTheme().trim().isEmpty() ? item.getTheme() : "-";
    String isbn = item.getIsbn() != null && !item.getIsbn().trim().isEmpty() ? item.getIsbn() : "-";
    String issn = item.getIssn() != null && !item.getIssn().trim().isEmpty() ? item.getIssn() : "-";
    String edisi = item.getEdition() != null && !item.getEdition().trim().isEmpty() ? item.getEdition() : "-";
    String tahun = item.getYear() != null ? item.getYear().toString() : "-";
    String bahasa = item.getLanguage() != null && !item.getLanguage().trim().isEmpty() ? item.getLanguage() : "-";
    String callNumber = item.getCallNumber() != null && !item.getCallNumber().trim().isEmpty() ? item.getCallNumber() : "-";
    String deskripsi = item.getSummary() != null && !item.getSummary().trim().isEmpty() ? item.getSummary() : Common.getBahasaConfig("Tidak ada deskripsi yang tersedia.");
    String[] kataCover = judul.trim().split("\\s+");
    StringBuilder coverMarkBuilder = new StringBuilder();
    for (int coverIndex = 0; coverIndex < kataCover.length && coverIndex < 3; coverIndex++) {
        if (coverIndex > 0) coverMarkBuilder.append("<br>");
        String kata = kataCover[coverIndex].toUpperCase();
        coverMarkBuilder.append(StringEscapeUtils.escapeHtml(kata.length() > 10 ? kata.substring(0, 10) : kata));
    }
    String coverMark = coverMarkBuilder.length() == 0 ? "KOLEKSI" : coverMarkBuilder.toString();
    int coverHue = (int) ((item.getId() == null ? 1L : item.getId().longValue()) * 47L % 360L);

    // Sanitasi data teks untuk menghindari error pada format JSON-LD (Kutip ganda & Baris baru)
    String jsonJudul = judul.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    String jsonDeskripsi = deskripsi.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    String jsonPengarang = pengarang.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    String jsonPenerbit = penerbit.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");

    judul = StringEscapeUtils.escapeHtml(judul);
    pengarang = StringEscapeUtils.escapeHtml(pengarang);
    penerbit = StringEscapeUtils.escapeHtml(penerbit);
    kategori = StringEscapeUtils.escapeHtml(kategori);
    klasifikasi = StringEscapeUtils.escapeHtml(klasifikasi);
    tema = StringEscapeUtils.escapeHtml(tema);
    isbn = StringEscapeUtils.escapeHtml(isbn);
    issn = StringEscapeUtils.escapeHtml(issn);
    edisi = StringEscapeUtils.escapeHtml(edisi);
    tahun = StringEscapeUtils.escapeHtml(tahun);
    bahasa = StringEscapeUtils.escapeHtml(bahasa);
    callNumber = StringEscapeUtils.escapeHtml(callNumber);
    deskripsi = StringEscapeUtils.escapeHtml(deskripsi);

    // Evaluasi Hak Akses Dokumen Digital (Unduhan Lampiran / Ebook)
    boolean bolehAksesDigital = Common.getCurrentUser(request) != null;
    String urlLampiran = bolehAksesDigital && item.getDigitalUrl() != null ? item.getDigitalUrl() : "";

    // Persistence access lives in a typed Java read service, not in this renderer.
    List<LibraryItemDetailService.Holding> listKetersediaan = item.getHoldings();
    int jumlahEksemplar = listKetersediaan == null ? 0 : listKetersediaan.size();
    int jumlahTersedia = 0;
    if (listKetersediaan != null) {
        for (LibraryItemDetailService.Holding holding : listKetersediaan) if (holding.isAvailable()) jumlahTersedia++;
    }
    String detailCsrf = NewUiCsrfUtil.getToken(request.getSession());
%>

<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "Book",
  "name": "<%=jsonJudul%>",
  "author": {
    "@type": "Person",
    "name": "<%=jsonPengarang%>"
  },
  "publisher": {
    "@type": "Organization",
    "name": "<%=jsonPenerbit%>"
  },
  "isbn": "<%=isbn%>",
  "datePublished": "<%=tahun%>",
  "image": "<%=coverImg%>",
  "description": "<%=jsonDeskripsi%>",
  "inLanguage": "<%=bahasa%>"
}
</script>

<style>
    #<%=modalId%>.library-detail-modal { --detail-primary:var(--theme-primary,#173b72);--detail-dark:var(--theme-primary-dark,#102b55);--detail-light:var(--theme-light,#eef4ff);--detail-on:var(--theme-text-on-primary,#fff);color:#14243a; }
    #<%=modalId%>.library-detail-modal .modal-dialog { max-width:1180px; }
    #<%=modalId%>.library-detail-modal .modal-content { border:0;border-radius:24px!important;background:#f5f8fc;box-shadow:0 30px 90px rgba(7,20,43,.32)!important; }
    #<%=modalId%> .library-detail-header { min-height:84px;padding:18px 24px 18px 28px;border:0;background:var(--theme-gradient,linear-gradient(135deg,var(--detail-dark),var(--detail-primary)))!important;color:var(--detail-on)!important; }
    #<%=modalId%> .library-detail-header-icon { display:grid;place-items:center;flex:0 0 46px;width:46px;height:46px;border-radius:14px;background:rgba(255,255,255,.15);font-size:1.2rem; }
    #<%=modalId%> .library-detail-header small { display:block;margin-bottom:2px;color:rgba(255,255,255,.78);font-size:.68rem;font-weight:800;letter-spacing:.1em;text-transform:uppercase; }
    #<%=modalId%> .library-detail-header h2 { margin:0;color:var(--detail-on)!important;font-size:clamp(1.15rem,2vw,1.55rem);font-weight:850; }
    #<%=modalId%> .library-detail-header .btn-close { padding:12px;border-radius:12px;background-color:rgba(255,255,255,.92);opacity:1; }
    #<%=modalId%> .modal-body { padding:22px 28px 30px; }
    #<%=modalId%> .library-detail-tabs { position:sticky;top:0;z-index:8;margin:0 0 22px;padding:7px;border-color:#dbe5f0;box-shadow:0 8px 22px rgba(30,55,90,.08); }
    #<%=modalId%> .library-detail-tabs a { display:inline-flex;align-items:center;gap:7px;min-height:40px;padding:8px 12px; }
    #<%=modalId%> .library-detail-tabs a:first-child { background:var(--detail-light);color:var(--detail-dark); }
    #<%=modalId%> .library-detail-tabs a.is-active { background:var(--detail-light);color:var(--detail-dark);box-shadow:inset 0 0 0 1px color-mix(in srgb,var(--detail-primary) 22%,transparent); }
    #<%=modalId%> .library-detail-hero { display:grid;grid-template-columns:260px minmax(0,1fr);gap:28px;align-items:start; }
    #<%=modalId%> .library-detail-cover-card { padding:16px;border:1px solid #dbe5f0;border-radius:20px;background:#fff;box-shadow:0 14px 36px rgba(22,45,78,.1); }
    #<%=modalId%> .library-detail-cover-card img { display:block;width:100%;height:330px;object-fit:contain;border-radius:14px;background:#f8fafc; }
    #<%=modalId%> .library-detail-generated-cover { display:flex;flex-direction:column;justify-content:space-between;width:100%;height:330px;padding:28px 24px;border-radius:14px;text-align:left;font-weight:850;box-shadow:inset 0 0 0 1px rgba(255,255,255,.3); }
    #<%=modalId%> .library-detail-generated-cover strong { font-size:1.35rem;line-height:1.25; }
    #<%=modalId%> .library-detail-generated-cover small { font-size:.85rem;font-weight:850; }
    #<%=modalId%> .library-detail-actions { display:grid;gap:9px;margin-top:14px; }
    #<%=modalId%> .library-detail-actions .btn { min-height:46px;border-radius:12px!important;font-size:.85rem; }
    #<%=modalId%> .library-detail-actions .btn-primary { border-color:var(--detail-primary);background:var(--theme-gradient,var(--detail-primary)); }
    #<%=modalId%> .library-detail-access { display:flex;align-items:center;gap:10px;padding:12px 14px;border:1px solid #dbe5f0;border-radius:13px;background:#fff;color:#52647b;text-align:left;font-size:.78rem;font-weight:750; }
    #<%=modalId%> .library-detail-access i { color:var(--detail-primary); }
    #<%=modalId%> .library-detail-kicker { display:flex;flex-wrap:wrap;gap:7px;margin-bottom:12px; }
    #<%=modalId%> .library-detail-kicker span { padding:5px 9px;border-radius:999px;background:var(--detail-light);color:var(--detail-dark);font-size:.68rem;font-weight:850;letter-spacing:.03em;text-transform:uppercase; }
    #<%=modalId%> .library-detail-main h1 { max-width:850px;margin:0 0 10px;color:#12233a!important;font-size:clamp(1.65rem,3vw,2.35rem);line-height:1.18;font-weight:880;letter-spacing:-.025em;overflow-wrap:anywhere; }
    #<%=modalId%> .library-detail-byline { margin:0 0 18px;color:#65758b;font-size:.95rem; }
    #<%=modalId%> .library-detail-byline strong { color:#24364e; }
    #<%=modalId%> .library-detail-stats { display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;margin-bottom:18px; }
    #<%=modalId%> .library-detail-stat { padding:12px 14px;border:1px solid #dbe5f0;border-radius:14px;background:#fff; }
    #<%=modalId%> .library-detail-stat small { display:block;color:#718198;font-size:.68rem;font-weight:750; }
    #<%=modalId%> .library-detail-stat strong { display:block;margin-top:3px;color:#172a43;font-size:1rem; }
    #<%=modalId%> .library-detail-stat.is-ready strong { color:#047857; }
    #<%=modalId%> .library-detail-section { margin-top:18px;padding:20px;border:1px solid #dbe5f0;border-radius:18px;background:#fff;box-shadow:0 6px 20px rgba(22,45,78,.045); }
    #<%=modalId%> .library-detail-section-title { display:flex;align-items:center;gap:10px;margin:0 0 15px;color:#172a43!important;font-size:1rem;font-weight:850; }
    #<%=modalId%> .library-detail-section-title i { display:grid;place-items:center;width:34px;height:34px;border-radius:10px;background:var(--detail-light);color:var(--detail-primary); }
    #<%=modalId%> .library-detail-description { margin:0;color:#52647b;font-size:.92rem;line-height:1.75;white-space:pre-line; }
    #<%=modalId%> .library-detail-meta-grid { display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1px;border:1px solid #e2e9f2;border-radius:14px;overflow:hidden;background:#e2e9f2; }
    #<%=modalId%> .library-detail-meta { display:grid;grid-template-columns:34px minmax(0,1fr);gap:10px;align-items:center;min-height:66px;padding:11px 13px;background:#fff; }
    #<%=modalId%> .library-detail-meta>i { display:grid;place-items:center;width:32px;height:32px;border-radius:9px;background:var(--detail-light);color:var(--detail-primary); }
    #<%=modalId%> .library-detail-meta small { display:block;color:#7a899d;font-size:.67rem;font-weight:750; }
    #<%=modalId%> .library-detail-meta strong { display:block;margin-top:2px;color:#253750;font-size:.84rem;overflow-wrap:anywhere; }
    #<%=modalId%> #detailMetadata<%=modalId%> table { margin:0!important; }
    #<%=modalId%> #detailMetadata<%=modalId%> tbody { display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1px;border:1px solid #e2e9f2;border-radius:14px;overflow:hidden;background:#e2e9f2; }
    #<%=modalId%> #detailMetadata<%=modalId%> tr { display:grid;grid-template-columns:46% minmax(0,1fr);min-height:72px;background:#fff; }
    #<%=modalId%> #detailMetadata<%=modalId%> tr[hidden] { display:none; }
    #<%=modalId%> #detailMetadata<%=modalId%> td { display:block;width:auto!important;padding:12px!important;background:#fff!important;border:0!important; }
    #<%=modalId%> #detailMetadata<%=modalId%> td:first-child>div { flex-direction:row!important;align-items:center!important;gap:8px;color:#74849a; }
    #<%=modalId%> #detailMetadata<%=modalId%> td:first-child i { display:grid;place-items:center;flex:0 0 30px;width:30px;height:30px;margin:0!important;border-radius:9px;background:var(--detail-light);color:var(--detail-primary)!important; }
    #<%=modalId%> #detailMetadata<%=modalId%> td:nth-child(2) { display:flex;align-items:center;color:#253750!important;font-size:.84rem; }
    #<%=modalId%> #detailMetadata<%=modalId%> td:nth-child(2)>div { width:100%; }
    #<%=modalId%> #detailMetadata<%=modalId%> td:nth-child(2)>div>.me-2:first-child { display:none; }
    #<%=modalId%> #detailAvailability<%=modalId%> { grid-column:1/-1;grid-template-columns:150px minmax(0,1fr);min-height:90px; }
    #<%=modalId%> #detailAvailability<%=modalId%> td:nth-child(2)>.d-flex>div { width:100%;margin-left:0!important; }
    #<%=modalId%> #detailAvailability<%=modalId%> td:nth-child(2)>.d-flex>div>.d-flex { display:grid!important;grid-template-columns:minmax(70px,.45fr) minmax(220px,1.5fr) auto;gap:8px;align-items:center!important;padding:9px 0;margin:0!important;border-bottom:1px solid #edf1f6; }
    #<%=modalId%> #detailAvailability<%=modalId%> td:nth-child(2)>.d-flex>div>.d-flex:last-child { border-bottom:0; }
    #<%=modalId%> #detailAvailability<%=modalId%> .rounded-1 { box-shadow:none!important;border-radius:9px!important;line-height:1.45; }
    #<%=modalId%> #detailAvailability<%=modalId%> .btn { border-radius:9px;white-space:nowrap; }
    #<%=modalId%> .library-holding-grid { display:grid;gap:10px; }
    #<%=modalId%> .library-holding-detail { display:grid;grid-template-columns:minmax(110px,.65fr) minmax(180px,1.4fr) auto;gap:12px;align-items:center;padding:13px 14px;border:1px solid #dfe7f0;border-radius:14px;background:#fbfdff; }
    #<%=modalId%> .library-holding-detail small { display:block;color:#7a899d;font-size:.67rem; }
    #<%=modalId%> .library-holding-detail strong { display:block;color:#253750;font-size:.82rem; }
    #<%=modalId%> .library-holding-status { display:inline-flex;align-items:center;gap:6px;margin-top:4px;font-size:.76rem;font-weight:850; }
    #<%=modalId%> .library-holding-status.is-available { color:#047857; }
    #<%=modalId%> .library-holding-status.is-loaned { color:#b45309; }
    #<%=modalId%> .library-detail-empty { padding:18px;border:1px dashed #ccd8e6;border-radius:14px;background:#f8fafc;color:#68798f;text-align:center; }
    #<%=modalId%> .library-detail-lower { display:grid;grid-template-columns:1fr 1fr;gap:18px;align-items:start; }
    #<%=modalId%> .library-detail-lower .library-detail-section { height:100%; }
    @media (max-width:900px){#<%=modalId%>.library-detail-modal .modal-dialog{max-width:calc(100vw - 20px);margin:10px auto}#<%=modalId%> .modal-body{padding:18px}#<%=modalId%> .library-detail-hero{grid-template-columns:210px minmax(0,1fr);gap:20px}#<%=modalId%> .library-detail-cover-card img,#<%=modalId%> .library-detail-generated-cover{height:285px}#<%=modalId%> .library-detail-stats{grid-template-columns:repeat(2,minmax(0,1fr))}#<%=modalId%> .library-detail-lower{grid-template-columns:1fr}#<%=modalId%> #detailMetadata<%=modalId%> tbody{grid-template-columns:1fr}#<%=modalId%> #detailAvailability<%=modalId%>{grid-column:auto}}
    @media (max-width:620px){#<%=modalId%> .library-detail-header{min-height:72px;padding:14px 16px}#<%=modalId%> .library-detail-header-icon{width:40px;height:40px;flex-basis:40px}#<%=modalId%> .library-detail-hero{grid-template-columns:1fr}#<%=modalId%> .library-detail-cover-column{max-width:300px;width:100%;margin:auto}#<%=modalId%> .library-detail-main h1{font-size:1.55rem}#<%=modalId%> .library-detail-stats{grid-template-columns:1fr 1fr}#<%=modalId%> #detailMetadata<%=modalId%> tr,#<%=modalId%> #detailAvailability<%=modalId%>{grid-template-columns:1fr}#<%=modalId%> #detailMetadata<%=modalId%> td:nth-child(2){padding-top:0!important}#<%=modalId%> #detailAvailability<%=modalId%> td:nth-child(2)>.d-flex>div>.d-flex{grid-template-columns:1fr}#<%=modalId%> #detailAvailability<%=modalId%> .btn{width:100%;margin-left:0!important}#<%=modalId%> .library-holding-detail{grid-template-columns:1fr}#<%=modalId%> .library-holding-detail .btn{width:100%}#<%=modalId%> .library-detail-tabs{margin-left:-4px;margin-right:-4px}}
</style>

<article class="modal fade library-detail-modal" id="<%=modalId%>" tabindex="-1" aria-labelledby="<%=modalId%>Label" aria-hidden="true">
    <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content overflow-hidden">
            
            <header class="modal-header library-detail-header d-flex align-items-center">
                <div class="library-detail-header-icon me-3" aria-hidden="true"><i class="fas fa-book-open"></i></div>
                <div class="modal-title" id="<%=modalId%>Label">
                    <small><%=Common.getBahasaConfig("Katalog Perpustakaan Digital")%></small>
                    <h2><%=Common.getBahasaConfig("Detail Koleksi")%></h2>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<%=Common.getBahasaConfig("Tutup")%>"></button>
            </header>
            
            <div class="modal-body">
                <nav class="library-detail-tabs" aria-label="<%=Common.getBahasaConfig("Bagian detail koleksi")%>"><a href="#detailSummary<%=modalId%>"><i class="fas fa-align-left" aria-hidden="true"></i><%=Common.getBahasaConfig("Ringkasan")%></a><a href="#detailAvailability<%=modalId%>"><i class="fas fa-map-marker-alt" aria-hidden="true"></i><%=Common.getBahasaConfig("Ketersediaan")%></a><a href="#detailFiles<%=modalId%>"><i class="fas fa-file-alt" aria-hidden="true"></i><%=Common.getBahasaConfig("Isi & Lampiran")%></a><a href="#detailMetadata<%=modalId%>"><i class="fas fa-list" aria-hidden="true"></i><%=Common.getBahasaConfig("Metadata")%></a><a href="#detailReview<%=modalId%>"><i class="fas fa-star" aria-hidden="true"></i><%=Common.getBahasaConfig("Ulasan")%></a><a href="#detailRelated<%=modalId%>"><i class="fas fa-th-large" aria-hidden="true"></i><%=Common.getBahasaConfig("Terkait")%></a></nav>
                <div class="library-detail-hero" id="detailSummary<%=modalId%>">
                    
                    <aside class="library-detail-cover-column text-center" id="detailFiles<%=modalId%>">
                        <div class="library-detail-cover-card">
                            <% if (punyaCoverAsli) { %><img src="<%=coverImg%>" alt="<%=Common.getBahasaConfig("Sampul Buku")%>: <%=judul%>" onerror="this.hidden=true;this.nextElementSibling.hidden=false">
                            <% } %><div class="library-detail-generated-cover" <%=punyaCoverAsli ? "hidden" : ""%> role="img" aria-label="<%=Common.getBahasaConfig("Sampul dibuat otomatis untuk")%> <%=judul%>" style="background:linear-gradient(150deg,hsl(<%=coverHue%>,72%,88%),hsl(<%=coverHue%>,55%,58%));color:hsl(<%=coverHue%>,70%,22%);"><strong><%=coverMark%></strong><small><%=tahun%></small></div>
                        </div>
                        <div class="library-detail-actions">
                        <% if (urlLampiran != null && !urlLampiran.trim().isEmpty()) { %>
                            <a href="<%=Common.ROOT%>/pustaka?s=reader&id=<%=item.getId()%>" class="btn btn-primary w-100 fw-bold">
                                <i class="fas fa-book-reader me-2"></i><%=Common.getBahasaConfig("Baca koleksi digital")%>
                            </a>
                        <% } else if (bolehAksesDigital && item.getEbookUrl() != null) { %>
                            <a href="<%=Common.ROOT%>/pustaka?s=reader&id=<%=item.getId()%>" class="btn btn-primary w-100 fw-bold">
                                <i class="fas fa-external-link-alt me-2"></i><%=Common.getBahasaConfig("Buka e-book")%>
                            </a>
                        <% } else { %>
                            <div class="library-detail-access">
                                <i class="fas fa-lock" aria-hidden="true"></i>
                                <span><%=Common.getBahasaConfig("Akses digital belum tersedia atau dibatasi.")%></span>
                            </div>
                        <% } %>
                        <button type="button" class="btn btn-outline-primary w-100 fw-bold" onclick="toggleFavoriteDetail<%=modalId%>(this)">
                            <i class="far fa-heart me-2"></i><%=Common.getBahasaConfig("Simpan ke daftar bacaan")%>
                        </button>
                        </div>
                        <div id="detailActionStatus<%=modalId%>" class="small mt-2" role="status" aria-live="polite"></div>
                    </aside>
                    
                    <main class="library-detail-main">
                        <div class="library-detail-kicker"><% if (!kategori.equals("-")) { %><span><%=kategori%></span><% } %><% if (!bahasa.equals("-")) { %><span><%=bahasa%></span><% } %><% if (!tahun.equals("-")) { %><span><%=tahun%></span><% } %></div>
                        <h1><%=judul%></h1>
                        <p class="library-detail-byline"><%=Common.getBahasaConfig("oleh")%> <strong><%=pengarang%></strong><% if (!penerbit.equals("-")) { %> · <%=penerbit%><% } %></p>
                        <div class="library-detail-stats">
                            <div class="library-detail-stat <%=jumlahTersedia>0?"is-ready":""%>"><small><%=Common.getBahasaConfig("Ketersediaan")%></small><strong><%=jumlahTersedia%> <%=Common.getBahasaConfig("tersedia")%></strong></div>
                            <div class="library-detail-stat"><small><%=Common.getBahasaConfig("Eksemplar")%></small><strong><%=jumlahEksemplar%> <%=Common.getBahasaConfig("tercatat")%></strong></div>
                            <div class="library-detail-stat"><small><%=Common.getBahasaConfig("Nomor panggil")%></small><strong><%=callNumber%></strong></div>
                        </div>

                        <section class="library-detail-section" id="detailMetadata<%=modalId%>">
                            <h2 class="library-detail-section-title"><i class="fas fa-info-circle" aria-hidden="true"></i><%=Common.getBahasaConfig("Informasi bibliografi")%></h2>
                            <table class="table table-borderless table-sm mb-0 align-middle" style="background-color: transparent; font-size: 0.95rem;">
                                <tbody>
                                    <tr>
                                        <td class="text-secondary pb-3" style="width: 140px; vertical-align: top;">
                                            <div class="d-flex flex-column align-items-start">
                                                <i class="fas fa-user-edit text-primary mb-1" style="font-size: 1.1rem;"></i>
                                                <span class="small fw-medium"><%=Common.getBahasaConfig("Pengarang")%></span>
                                            </div>
                                        </td>
                                        <td class="fw-bold text-dark pb-3" style="vertical-align: top;">
                                            <div class="d-flex">
                                                <span class="me-2">:</span> 
                                                <span><%=pengarang%></span>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="text-secondary pb-3" style="vertical-align: top;">
                                            <div class="d-flex flex-column align-items-start">
                                                <i class="far fa-building text-primary mb-1" style="font-size: 1.1rem;"></i>
                                                <span class="small fw-medium"><%=Common.getBahasaConfig("Penerbit")%></span>
                                            </div>
                                        </td>
                                        <td class="fw-bold text-dark pb-3" style="vertical-align: top;">
                                            <div class="d-flex">
                                                <span class="me-2">:</span> 
                                                <span><%=penerbit%></span>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="text-secondary pb-3" style="vertical-align: top;">
                                            <div class="d-flex flex-column align-items-start">
                                                <i class="fas fa-tag text-primary mb-1" style="font-size: 1.1rem;"></i>
                                                <span class="small fw-medium"><%=Common.getBahasaConfig("Tema")%></span>
                                            </div>
                                        </td>
                                        <td class="fw-bold text-dark pb-3" style="vertical-align: top;">
                                            <div class="d-flex">
                                                <span class="me-2">:</span> 
                                                <span><%=tema%></span>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="text-secondary pb-3" style="vertical-align: top;">
                                            <div class="d-flex flex-column align-items-start">
                                                <i class="fas fa-layer-group text-primary mb-1" style="font-size: 1.1rem;"></i>
                                                <span class="small fw-medium"><%=Common.getBahasaConfig("Kategori")%></span>
                                            </div>
                                        </td>
                                        <td class="fw-bold text-dark pb-3" style="vertical-align: top;">
                                            <div class="d-flex">
                                                <span class="me-2">:</span> 
                                                <span><%=kategori%></span>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="text-secondary pb-3" style="vertical-align: top;">
                                            <div class="d-flex flex-column align-items-start">
                                                <i class="fas fa-sitemap text-primary mb-1" style="font-size: 1.1rem;"></i>
                                                <span class="small fw-medium"><%=Common.getBahasaConfig("Klasifikasi")%></span>
                                            </div>
                                        </td>
                                        <td class="fw-bold text-dark pb-3" style="vertical-align: top;">
                                            <div class="d-flex">
                                                <span class="me-2">:</span> 
                                                <span><%=klasifikasi%></span>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="text-secondary pb-3" style="vertical-align: top;">
                                            <div class="d-flex flex-column align-items-start">
                                                <i class="fas fa-sort-numeric-down text-primary mb-1" style="font-size: 1.1rem;"></i>
                                                <span class="small fw-medium"><%=Common.getBahasaConfig("Call Number")%></span>
                                            </div>
                                        </td>
                                        <td class="fw-bold text-dark pb-3" style="vertical-align: top;">
                                            <div class="d-flex">
                                                <span class="me-2">:</span> 
                                                <span><%=callNumber%></span>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="text-secondary pb-3" style="vertical-align: top;">
                                            <div class="d-flex flex-column align-items-start">
                                                <i class="fas fa-language text-primary mb-1" style="font-size: 1.1rem;"></i>
                                                <span class="small fw-medium"><%=Common.getBahasaConfig("Bahasa")%></span>
                                            </div>
                                        </td>
                                        <td class="fw-bold text-dark pb-3" style="vertical-align: top;">
                                            <div class="d-flex">
                                                <span class="me-2">:</span> 
                                                <span><%=bahasa%></span>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="text-secondary pb-3" style="vertical-align: top;">
                                            <div class="d-flex flex-column align-items-start">
                                                <i class="fas fa-copy text-primary mb-1" style="font-size: 1.1rem;"></i>
                                                <span class="small fw-medium"><%=Common.getBahasaConfig("Edisi")%></span>
                                            </div>
                                        </td>
                                        <td class="fw-bold text-dark pb-3" style="vertical-align: top;">
                                            <div class="d-flex">
                                                <span class="me-2">:</span> 
                                                <span><%=edisi%></span>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="text-secondary pb-3" style="vertical-align: top;">
                                            <div class="d-flex flex-column align-items-start">
                                                <i class="far fa-calendar-alt text-primary mb-1" style="font-size: 1.1rem;"></i>
                                                <span class="small fw-medium"><%=Common.getBahasaConfig("Tahun")%></span>
                                            </div>
                                        </td>
                                        <td class="fw-bold text-dark pb-3" style="vertical-align: top;">
                                            <div class="d-flex">
                                                <span class="me-2">:</span> 
                                                <span><%=tahun%></span>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="text-secondary pb-3" style="vertical-align: top;">
                                            <div class="d-flex flex-column align-items-start">
                                                <i class="fas fa-barcode text-primary mb-1" style="font-size: 1.1rem;"></i>
                                                <span class="small fw-medium"><%=Common.getBahasaConfig("ISBN / ISSN")%></span>
                                            </div>
                                        </td>
                                        <td class="fw-bold text-dark pb-3" style="vertical-align: top;">
                                            <div class="d-flex align-items-center">
                                                <span class="me-2">:</span> 
                                                <span class="badge bg-white text-dark border px-2 py-1 shadow-sm"><%=isbn%></span> 
                                                <%=!issn.equals("-") ? "<span class='mx-2 text-muted'>|</span> <span class='badge bg-white text-dark border px-2 py-1 shadow-sm'>" + issn + "</span>" : ""%>
                                            </div>
                                        </td>
                                    </tr>
                                    
                                    <tr id="detailAvailability<%=modalId%>">
                                        <td class="text-secondary pb-1" style="vertical-align: top;">
                                            <div class="d-flex flex-column align-items-start mt-2">
                                                <i class="fas fa-boxes text-primary mb-1" style="font-size: 1.1rem;"></i>
                                                <span class="small fw-medium"><%=Common.getBahasaConfig("Ketersediaan")%></span>
                                            </div>
                                        </td>
                                        <td class="pb-1" style="vertical-align: top;">
                                            <div class="d-flex flex-column gap-2 mt-2">
                                                <span class="me-2 text-dark fw-bold position-absolute" style="margin-top: 1px;">:</span> 
                                                <div style="margin-left: 15px;">
                                                    <% if (listKetersediaan.isEmpty()) { %>
                                                        <span class="text-muted fw-bold"><%=Common.getBahasaConfig("Belum ada data ketersediaan fisik perpustakaan.")%></span>
                                                    <% } else { %>
                                                        <% for (LibraryItemDetailService.Holding ipb : listKetersediaan) {
                                                            String barcodeStr = StringEscapeUtils.escapeHtml(ipb.getBarcode());
                                                            String perpusName = StringEscapeUtils.escapeHtml(ipb.getLibraryName());
                                                        %>
                                                        <div class="d-flex align-items-center mb-2">
                                                            <span class="me-3 text-dark fw-bold text-center" style="min-width: 65px;"><%=barcodeStr%></span>
                                                            <div class="px-3 py-1 shadow-sm rounded-1" style="background-color: <%=ipb.isAvailable() ? "#ecfdf5" : "#fff7ed"%>; color: <%=ipb.isAvailable() ? "#047857" : "#b45309"%>; font-size: 0.9rem; font-weight: bold;">
                                                                <%=ipb.isAvailable() ? Common.getBahasaConfig("Tersedia di") : Common.getBahasaConfig("Sedang dipinjam di")%> <%=perpusName.toUpperCase()%>
                                                                <%=!ipb.isAvailable() && !ipb.getDueDate().isEmpty() ? " · " + Common.getBahasaConfig("Jatuh tempo") + " " + ipb.getDueDate() : ""%>
                                                                <%=!ipb.getShelf().isEmpty() ? " · " + Common.getBahasaConfig("Rak") + " " + StringEscapeUtils.escapeHtml(ipb.getShelf()) : ""%>
                                                                <%=ipb.getQueue()>0 ? " · " + Common.getBahasaConfig("Antrean") + " " + ipb.getQueue() : ""%>
                                                            </div>
                                                            <% if (ipb.getLibraryId() != null) { %><button type="button" class="btn btn-sm btn-outline-primary ms-2" onclick="holdDetail<%=modalId%>(<%=ipb.getLibraryId()%>,this)"><%=Common.getBahasaConfig(ipb.isAvailable()?"Reservasi":"Masuk Antrean")%></button><% } %>
                                                        </div>
                                                        <% } %>
                                                    <% } %>
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                    
                                </tbody>
                            </table>
                        </section>

                        <section class="library-detail-section">
                            <h2 class="library-detail-section-title"><i class="fas fa-align-left" aria-hidden="true"></i><%=Common.getBahasaConfig("Ringkasan / Abstrak")%></h2>
                            <p class="library-detail-description"><%=deskripsi%></p>
                        </section>

                    </main>
                </div>
                <div class="library-detail-lower">
                <section class="library-detail-section" id="detailRelated<%=modalId%>" aria-labelledby="detailRelatedTitle<%=modalId%>">
                    <h2 class="library-detail-section-title" id="detailRelatedTitle<%=modalId%>"><i class="fas fa-lightbulb" aria-hidden="true"></i><%=Common.getBahasaConfig("Koleksi terkait")%></h2>
                    <div class="row g-3" id="detailRecommendations<%=modalId%>"><div class="text-secondary"><%=Common.getBahasaConfig("Menyiapkan rekomendasi...")%></div></div>
                </section>
                <section class="library-detail-section" id="detailReview<%=modalId%>" aria-labelledby="detailReviewTitle<%=modalId%>">
                    <h2 class="library-detail-section-title" id="detailReviewTitle<%=modalId%>"><i class="fas fa-star" aria-hidden="true"></i><%=Common.getBahasaConfig("Rating dan ulasan")%></h2>
                    <div id="detailRatingSummary<%=modalId%>" class="text-secondary mb-3" aria-live="polite"></div>
                    <div id="detailReviews<%=modalId%>" class="d-grid gap-2"></div>
                    <% if (bolehAksesDigital) { %><form class="mt-3 p-3 border rounded-4 bg-white" id="detailReviewForm<%=modalId%>"><label class="form-label fw-bold" for="detailRating<%=modalId%>"><%=Common.getBahasaConfig("Rating")%></label><select class="form-select mb-2" id="detailRating<%=modalId%>" required><option value="5">5 — Sangat baik</option><option value="4">4 — Baik</option><option value="3">3 — Cukup</option><option value="2">2 — Kurang</option><option value="1">1 — Buruk</option></select><label class="form-label fw-bold" for="detailComment<%=modalId%>"><%=Common.getBahasaConfig("Ulasan")%></label><textarea class="form-control" id="detailComment<%=modalId%>" maxlength="240" rows="3" required></textarea><button class="btn btn-outline-primary rounded-pill mt-2" type="submit"><%=Common.getBahasaConfig("Kirim untuk moderasi")%></button></form><% } else { %><a class="btn btn-outline-primary rounded-pill mt-2" href="<%=Common.ROOT%>/pustaka?s=login_pustaka"><%=Common.getBahasaConfig("Masuk untuk menulis ulasan")%></a><% } %>
                </section>
                </div>
            </div>
        </div>
    </div>
</article>

<script>
    var detailApi<%=modalId%> = '<%=Common.ROOT%>/pustaka?hanya_tampil_jsp=true&p=pustaka&s=_beranda_anggota_service';
    var detailCatalogApi<%=modalId%> = '<%=Common.ROOT%>/pustaka?hanya_tampil_jsp=true&p=pustaka&s=_catalog_api';
    var detailOperationsApi<%=modalId%> = '<%=Common.ROOT%>/pustaka?hanya_tampil_jsp=true&p=pustaka&s=_operations_api';
    var detailCsrf<%=modalId%> = '<%=detailCsrf%>';
    function detailMutation<%=modalId%>(action, data, button) {
        var status = document.getElementById('detailActionStatus<%=modalId%>');
        var form = new URLSearchParams(Object.assign({action: action, nui_csrf: detailCsrf<%=modalId%>}, data));
        button.disabled = true; status.className = 'small mt-2 text-secondary'; status.textContent = '<%=Common.getBahasaConfigJS("Memproses...")%>';
        fetch(detailApi<%=modalId%>, {method:'POST', credentials:'same-origin', headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8','X-NUI-CSRF':detailCsrf<%=modalId%>,'Accept':'application/json'}, body:form.toString()})
          .then(function(r){return r.json();}).then(function(r){
            detailCsrf<%=modalId%> = r.csrf || detailCsrf<%=modalId%>;
            if (!r.ok) throw new Error(r.error || r.message);
            status.className = 'small mt-2 text-success'; status.textContent = r.message; button.disabled = false;
          }).catch(function(e){ status.className = 'small mt-2 text-danger'; status.textContent = e.message; button.disabled = false; });
    }
    function toggleFavoriteDetail<%=modalId%>(button) { detailMutation<%=modalId%>('favorite_toggle', {itemId:'<%=item.getId()%>'}, button); }
    function holdDetail<%=modalId%>(libraryId, button) { detailMutation<%=modalId%>('hold', {itemId:'<%=item.getId()%>', libraryId:String(libraryId)}, button); }
    function detailEscape<%=modalId%>(value) { var node=document.createElement('span');node.textContent=value==null?'':String(value);return node.innerHTML; }
    (function prepareDetailView<%=modalId%>() {
        var metadata=document.querySelector('#<%=modalId%> #detailMetadata<%=modalId%> tbody');
        if(metadata) metadata.querySelectorAll('tr:not(#detailAvailability<%=modalId%>)').forEach(function(row){var cells=row.querySelectorAll('td');if(cells.length>1&&cells[1].textContent.replace(':','').trim()==='-')row.hidden=true;});
        document.querySelectorAll('#<%=modalId%> .library-detail-tabs a').forEach(function(link){link.addEventListener('click',function(){document.querySelectorAll('#<%=modalId%> .library-detail-tabs a').forEach(function(x){x.classList.remove('is-active');});link.classList.add('is-active');});});
    }());
    function loadRecommendations<%=modalId%>() {
        fetch(detailCatalogApi<%=modalId%>+'&action=recommendations&itemId=<%=item.getId()%>',{credentials:'same-origin',headers:{'Accept':'application/json'}}).then(function(r){return r.json();}).then(function(data){var box=document.getElementById('detailRecommendations<%=modalId%>'),rows=data.items||[];box.innerHTML=rows.length?rows.map(function(x){return '<div class="col-md-6 col-xl-4"><a class="d-block h-100 p-3 border rounded-4 bg-white text-decoration-none text-dark" href="<%=Common.ROOT%>/pustaka?id='+encodeURIComponent(x.id)+'"><small class="text-primary fw-bold">'+detailEscape<%=modalId%>(x.jenisKoleksi||'Koleksi')+'</small><strong class="d-block mt-1">'+detailEscape<%=modalId%>(x.nama)+'</strong><span class="small text-secondary">'+detailEscape<%=modalId%>(x.pengarangs||'-')+' · '+detailEscape<%=modalId%>(x.tahun||'-')+'</span></a></div>';}).join(''):'<div class="text-secondary"><%=Common.getBahasaConfigJS("Belum ada rekomendasi terkait.")%></div>';}).catch(function(){document.getElementById('detailRecommendations<%=modalId%>').innerHTML='<div class="text-secondary"><%=Common.getBahasaConfigJS("Rekomendasi belum dapat dimuat.")%></div>';});
    }
    function loadReviews<%=modalId%>() {
        fetch(detailOperationsApi<%=modalId%>+'&action=comment_list&itemId=<%=item.getId()%>',{credentials:'same-origin',headers:{'Accept':'application/json'}}).then(function(r){return r.json();}).then(function(data){detailCsrf<%=modalId%>=data.csrf||detailCsrf<%=modalId%>;var rating=data.rating||{},rows=data.data||[];document.getElementById('detailRatingSummary<%=modalId%>').textContent=rating.count?(rating.average+' / 5 · '+rating.count+' ulasan terbit'):'<%=Common.getBahasaConfigJS("Belum ada ulasan terbit.")%>';document.getElementById('detailReviews<%=modalId%>').innerHTML=rows.map(function(x){return '<article class="p-3 border rounded-4 bg-white"><div class="text-warning" aria-label="Rating '+x.rating+' dari 5">'+('★★★★★'.slice(0,x.rating))+'<span class="text-secondary">'+('★★★★★'.slice(x.rating))+'</span></div><p class="mb-1">'+detailEscape<%=modalId%>(x.comment)+'</p><small class="text-secondary">'+detailEscape<%=modalId%>(x.author)+'</small></article>';}).join('');});
    }
    <% if (bolehAksesDigital) { %>document.getElementById('detailReviewForm<%=modalId%>').addEventListener('submit',function(event){event.preventDefault();var button=this.querySelector('button');button.disabled=true;var form=new URLSearchParams({action:'comment_add',itemId:'<%=item.getId()%>',rating:document.getElementById('detailRating<%=modalId%>').value,comment:document.getElementById('detailComment<%=modalId%>').value,nui_csrf:detailCsrf<%=modalId%>});fetch(detailOperationsApi<%=modalId%>,{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8','X-NUI-CSRF':detailCsrf<%=modalId%>,'Accept':'application/json'},body:form.toString()}).then(function(r){return r.json();}).then(function(data){detailCsrf<%=modalId%>=data.csrf||detailCsrf<%=modalId%>;if(!data.ok)throw new Error(data.error||data.message);document.getElementById('detailComment<%=modalId%>').value='';document.getElementById('detailActionStatus<%=modalId%>').textContent=data.message;button.disabled=false;}).catch(function(error){document.getElementById('detailActionStatus<%=modalId%>').textContent=error.message;button.disabled=false;});});<% } %>
    loadRecommendations<%=modalId%>();loadReviews<%=modalId%>();

    var currentUrl = window.location.href;
    if (currentUrl.indexOf('pustaka?id=') > -1 || currentUrl.indexOf('/main/item/') > -1) {
        setTimeout(function() {
            var modalElement = document.getElementById('<%=modalId%>');
            if (modalElement) {
                modalElement.classList.remove('fade'); 
                modalElement.style.display = 'block'; 
                modalElement.style.backgroundColor = 'rgba(0, 0, 0, 0.7)'; 
                modalElement.style.overflowY = 'auto'; 
                
                document.body.style.backgroundColor = '#f8f9fa';
                
                // 1. Temukan tombol close, biarkan TETAP TAMPIL, tapi ubah fungsinya
                var btnClose = modalElement.querySelector('.btn-close');
                if (btnClose) {
                    // btnClose.style.display = 'none'; // (BARIS INI KITA HAPUS)
                    
                    btnClose.addEventListener('click', function(e) {
                        e.preventDefault();
                        e.stopPropagation();
                        // Arahkan kembali ke halaman katalog utama
                        window.location.href = '<%=Common.ROOT%>/pustaka'; 
                    });
                }

                // 2. Jika pengunjung mengeklik area luar popup (background gelap), arahkan juga ke katalog
                modalElement.addEventListener('click', function(e) {
                    if (e.target === modalElement) {
                        e.preventDefault();
                        window.location.href = '<%=Common.ROOT%>/pustaka';
                    }
                });
            }
        }, 100);
    }
</script>
