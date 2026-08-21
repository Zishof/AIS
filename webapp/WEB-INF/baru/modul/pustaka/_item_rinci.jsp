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
    /* Penataan latar belakang modal agar bersih dan tidak tumpang tindih */
    .modal-bg-custom-<%=modalId%> {
        position: relative;
        background-color: #f8f9fa; 
    }
    .modal-bg-custom-<%=modalId%>::before {
        content: "";
        position: absolute;
        top: 0; left: 0; right: 0; bottom: 0;
        background-image: url('<%=Common.ROOT%>/img/buku.jpeg');
        background-size: cover;
        background-position: center;
        opacity: 0.10;
        z-index: 0;
        pointer-events: none; 
    }
    .modal-bg-custom-<%=modalId%> .modal-header,
    .modal-bg-custom-<%=modalId%> .modal-body {
        position: relative;
        z-index: 1;
    }
</style>

<article class="modal fade" id="<%=modalId%>" tabindex="-1" aria-labelledby="<%=modalId%>Label" aria-hidden="true">
    <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden modal-bg-custom-<%=modalId%>">
            
            <header class="modal-header border-0 pb-0 pt-4 px-4 px-md-5 d-flex align-items-center">
                <div class="modal-title fw-bold text-primary mb-0 fs-5" id="<%=modalId%>Label">
                    <i class="fas fa-book-open me-2"></i><%=Common.getBahasaConfig("Informasi Lengkap Pustaka")%>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<%=Common.getBahasaConfig("Tutup")%>"></button>
            </header>
            
            <div class="modal-body p-4 p-md-5 pt-4">
                <div class="row g-4 g-lg-5">
                    
                    <div class="col-md-5 col-lg-4 text-center">
                        <div class="p-3 rounded-4 shadow-sm mb-4 border" style="background-color: #ffffff;">
                            <% if (punyaCoverAsli) { %><img src="<%=coverImg%>" class="img-fluid rounded-3" style="max-height: 380px; object-fit: contain; width: 100%;" alt="<%=Common.getBahasaConfig("Sampul Buku")%>: <%=judul%>" onerror="this.hidden=true;this.nextElementSibling.hidden=false">
                            <% } %><div <%=punyaCoverAsli ? "hidden" : ""%> role="img" aria-label="<%=Common.getBahasaConfig("Sampul dibuat otomatis untuk")%> <%=judul%>" style="width:min(100%,260px);height:360px;margin:auto;padding:28px 22px;border-radius:16px;text-align:left;font-weight:800;background:linear-gradient(150deg,hsl(<%=coverHue%>,72%,88%),hsl(<%=coverHue%>,55%,58%));color:hsl(<%=coverHue%>,70%,22%);box-shadow:0 14px 30px rgba(30,64,175,.16);"><div style="font-size:1.35rem;line-height:1.25"><%=coverMark%></div><div style="margin-top:220px;font-size:.9rem"><%=tahun%></div></div>
                        </div>
                        
                        <% if (urlLampiran != null && !urlLampiran.trim().isEmpty()) { %>
                            <a href="<%=urlLampiran%>" target="_blank" rel="noopener noreferrer" class="btn btn-primary w-100 rounded-pill fw-bold shadow-sm py-2 py-lg-3 mb-2">
                                <i class="fas fa-file-download me-2"></i><%=Common.getBahasaConfig("Unduh / Baca Lampiran Buku")%>
                            </a>
                        <% } else if (bolehAksesDigital && item.getEbookUrl() != null) { %>
                            <a href="<%=item.getEbookUrl()%>" target="_blank" rel="noopener noreferrer" class="btn btn-primary w-100 rounded-pill fw-bold shadow-sm py-2 py-lg-3 mb-2">
                                <i class="fas fa-external-link-alt me-2"></i><%=Common.getBahasaConfig("Buka Tautan E-Book Eksternal")%>
                            </a>
                        <% } else { %>
                            <div class="alert border-0 rounded-4 p-3 d-flex align-items-center justify-content-center mb-0" style="background-color: #e9ecef; box-shadow: inset 0 1px 2px rgba(0,0,0,0.05);">
                                <i class="fas fa-lock me-3 text-dark"></i>
                                <span class="small fw-medium text-dark"><%=Common.getBahasaConfig("Hak akses dokumen digital dibatasi.")%></span>
                            </div>
                        <% } %>
                        <button type="button" class="btn btn-outline-primary w-100 rounded-pill fw-bold mt-2" onclick="toggleFavoriteDetail<%=modalId%>(this)">
                            <i class="fas fa-heart me-2"></i><%=Common.getBahasaConfig("Tambah / hapus favorit")%>
                        </button>
                        <div id="detailActionStatus<%=modalId%>" class="small mt-2" role="status" aria-live="polite"></div>
                    </div>
                    
                    <div class="col-md-7 col-lg-8">
                        <h1 class="h3 fw-bold text-dark mb-4 lh-base"><%=judul%></h1>

                        <div class="p-4 rounded-4 mb-4 shadow-sm border border-light" style="background-color: rgba(248, 249, 252, 0.9);">
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
                                                <i class="fas fa-phone-alt text-primary mb-1" style="font-size: 1.1rem;"></i>
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
                                    
                                    <tr>
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
                                                            </div>
                                                            <% if (ipb.getLibraryId() != null) { %><button type="button" class="btn btn-sm btn-outline-primary ms-2" onclick="holdDetail<%=modalId%>(<%=ipb.getLibraryId()%>,this)"><%=Common.getBahasaConfig("Reservasi")%></button><% } %>
                                                        </div>
                                                        <% } %>
                                                    <% } %>
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                    
                                </tbody>
                            </table>
                        </div>

                        <div class="mt-4 pt-2 rounded-4" style="background-color: rgba(248, 249, 252, 0.9);">
                            <h6 class="fw-bold text-dark border-bottom pb-3 mb-3">
                                <i class="fas fa-info-circle text-primary me-2"></i><%=Common.getBahasaConfig("Deskripsi Singkat / Abstrak")%>
                            </h6>
                            <div class="text-secondary fw-medium lh-lg text-justify" style=" overflow-y: auto; padding-right: 15px; font-size: 0.95rem;">
                                <%=deskripsi%>
                            </div>
                        </div>

                    </div>
                </div>
                <section class="mt-5" aria-labelledby="detailRelatedTitle<%=modalId%>">
                    <h2 class="h5 fw-bold" id="detailRelatedTitle<%=modalId%>"><i class="fas fa-lightbulb text-primary me-2"></i><%=Common.getBahasaConfig("Rekomendasi koleksi terkait")%></h2>
                    <div class="row g-3" id="detailRecommendations<%=modalId%>"><div class="text-secondary"><%=Common.getBahasaConfig("Menyiapkan rekomendasi...")%></div></div>
                </section>
                <section class="mt-5" aria-labelledby="detailReviewTitle<%=modalId%>">
                    <h2 class="h5 fw-bold" id="detailReviewTitle<%=modalId%>"><i class="fas fa-star text-warning me-2"></i><%=Common.getBahasaConfig("Rating dan ulasan anggota")%></h2>
                    <div id="detailRatingSummary<%=modalId%>" class="text-secondary mb-3" aria-live="polite"></div>
                    <div id="detailReviews<%=modalId%>" class="d-grid gap-2"></div>
                    <% if (bolehAksesDigital) { %><form class="mt-3 p-3 border rounded-4 bg-white" id="detailReviewForm<%=modalId%>"><label class="form-label fw-bold" for="detailRating<%=modalId%>"><%=Common.getBahasaConfig("Rating")%></label><select class="form-select mb-2" id="detailRating<%=modalId%>" required><option value="5">5 — Sangat baik</option><option value="4">4 — Baik</option><option value="3">3 — Cukup</option><option value="2">2 — Kurang</option><option value="1">1 — Buruk</option></select><label class="form-label fw-bold" for="detailComment<%=modalId%>"><%=Common.getBahasaConfig("Ulasan")%></label><textarea class="form-control" id="detailComment<%=modalId%>" maxlength="240" rows="3" required></textarea><button class="btn btn-outline-primary rounded-pill mt-2" type="submit"><%=Common.getBahasaConfig("Kirim untuk moderasi")%></button></form><% } else { %><a class="btn btn-outline-primary rounded-pill mt-2" href="<%=Common.ROOT%>/pustaka?s=login_pustaka"><%=Common.getBahasaConfig("Masuk untuk menulis ulasan")%></a><% } %>
                </section>
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
