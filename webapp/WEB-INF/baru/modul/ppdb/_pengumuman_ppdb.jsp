<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.jsoup.Jsoup"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.PengumumanAkademis"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String keyword = request.getParameter("keyword");
    String rnd = request.getParameter("rnd"); 
    String pageStr = request.getParameter("page");
    
    if (rnd == null) rnd = "";
    int currentPage = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
    
    // Batasan jumlah data per halaman
    int limitPerPage = 5;

    Session sess = null;
    List<PengumumanAkademis> rawList = new ArrayList<PengumumanAkademis>();

    // Deteksi Bahasa Aktif
    String currentLang = Tbmuser.INDONESIA;
    Object langAttr = session.getAttribute("current_lang");
    if (langAttr != null) {
        currentLang = (String) langAttr;
    }
    boolean isIndo = currentLang.equals(Tbmuser.INDONESIA);

    try {
        sess = HibernateUtil.openSession();
        Criteria criteria = sess.createCriteria(PengumumanAkademis.class);
        
        // Filter Data Aktif dan Khusus Calon Siswa (PPDB)
        criteria.add(Restrictions.eq("aktif", true));
        criteria.add(Restrictions.ilike("diperuntukkan", PengumumanAkademis.UNTUK_CALON_SISWA, MatchMode.ANYWHERE));
        
        // Pencarian Berdasarkan Kata Kunci dan Bahasa
        if (keyword != null && !keyword.trim().isEmpty()) {
            if (isIndo) {
                criteria.add(Restrictions.ilike("judul", keyword, MatchMode.ANYWHERE));
            } else {
                criteria.add(Restrictions.ilike("judulEn", keyword, MatchMode.ANYWHERE));
            }
        }
        
        // Pengurutan dari yang paling baru
        criteria.addOrder(Order.desc("tanggal"));
        
        // Pengambilan data menggunakan ConstantValues
        rawList = ConstantValues.simpleList(criteria, PengumumanAkademis.class);
        
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/ppdb/_pengumuman_ppdb.jsp:63");
    } finally {
        // PENUTUPAN SESI YANG AMAN
        if (sess != null) {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_pengumuman_ppdb.jsp:67");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_pengumuman_ppdb.jsp:68");}
        }
        HibernateUtil.closeSessionQuietly(sess);
    }
%>

<style>
    .hover-bg-light<%=rnd%>:hover { background-color: #f8f9fa !important; transform: translateX(3px); }
    .transition-base<%=rnd%> { transition: all 0.3s ease-in-out; }
</style>

<div class="list-group list-group-flush rounded-bottom-4 shadow-sm border bg-white">
    <% 
    if (rawList == null || rawList.isEmpty()) { 
    %>
        <div class="p-5 text-center text-muted">
            <i class="far fa-bell-slash fa-3x mb-3 text-secondary" style="opacity: 0.4;"></i><br>
            <span class="fw-bold"><%= Common.getBahasaConfig("Belum Terdapat Pengumuman") %></span>
            <p class="small text-muted mb-0"><%= Common.getBahasaConfig("Saat ini tidak terdapat informasi terbaru untuk ditampilkan.") %></p>
        </div>
    <% 
    } else { 
        int totalData = rawList.size();
        int totalPages = (int) Math.ceil((double) totalData / limitPerPage);
        
        if (currentPage < 1) currentPage = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        int startIdx = (currentPage - 1) * limitPerPage;
        int endIdx = Math.min(startIdx + limitPerPage, totalData);
        List<PengumumanAkademis> pagedList = rawList.subList(startIdx, endIdx);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new java.util.Locale("id", "ID"));
        
        for (PengumumanAkademis p : pagedList) {
            String judulDisplay = isIndo ? p.getJudul() : ais.common.Common.terjemahDinamis(p.getJudul());
            if (judulDisplay == null || judulDisplay.trim().isEmpty()) {
                judulDisplay = p.getJudul();
            }

            String isiDisplay = isIndo ? p.getCatatan() : ais.common.Common.terjemahDinamis(p.getCatatan());
            if (isiDisplay == null || isiDisplay.trim().isEmpty()) {
                isiDisplay = p.getCatatan();
            }
            
            if (isiDisplay != null) {
                isiDisplay = Jsoup.parse(isiDisplay).text();
                if (isiDisplay.length() > 120) {
                    isiDisplay = isiDisplay.substring(0, 120) + "...";
                }
            } else {
                isiDisplay = "-";
            }

            String tglDisplay = p.getTanggal() != null ? sdf.format(p.getTanggal()) : "-";
    %>
        <a href="javascript:void(0)" onclick="bukaPengumumanRinciPPDB<%=rnd%>('<%=p.getId()%>')" 
           class="list-group-item list-group-item-action p-4 border-start border-4 border-primary border-bottom hover-bg-light<%=rnd%> transition-base<%=rnd%> text-decoration-none animate__animated animate__fadeIn">
            <div class="d-flex w-100 justify-content-between align-items-start mb-2">
                <h6 class="mb-0 text-primary fw-bold" style="line-height: 1.5; max-width: 80%;">
                    <i class="fas fa-info-circle text-warning me-2"></i><%= judulDisplay %>
                </h6>
                <small class="text-muted bg-light px-2 py-1 rounded-pill border text-nowrap ms-2" style="font-size: 0.75rem;">
                    <i class="far fa-clock me-1"></i><%= tglDisplay %>
                </small>
            </div>
            <p class="mb-2 text-secondary small" style="line-height: 1.6;">
                <%= isiDisplay %>
            </p>
            <div class="mt-2 text-end">
                <span class="text-primary fw-bold small"><%= Common.getBahasaConfig("Baca Selengkapnya") %> <i class="fas fa-arrow-right ms-1"></i></span>
            </div>
        </a>
    <% 
        } 

        // RENDER PAGINASI
        if (totalPages > 1) {
    %>
        <div class="p-3 border-top d-flex justify-content-center bg-light rounded-bottom-4">
            <nav aria-label="Navigasi Halaman Pengumuman">
                <ul class="pagination pagination-sm shadow-sm rounded-pill mb-0">
                    <li class="page-item <%= (currentPage == 1) ? "disabled" : "" %>">
                        <button class="page-link rounded-start-pill text-primary fw-bold px-3" onclick="loadPengumumanPPDB<%=rnd%>(<%= currentPage - 1 %>)">
                            <i class="fas fa-chevron-left"></i>
                        </button>
                    </li>
                    <% for (int i = 1; i <= totalPages; i++) { 
                        if (i == 1 || i == totalPages || (i >= currentPage - 1 && i <= currentPage + 1)) {
                    %>
                        <li class="page-item <%= (currentPage == i) ? "active" : "" %>">
                            <button class="page-link <%= (currentPage == i) ? "bg-primary text-white border-primary fw-bold" : "text-secondary" %>" onclick="loadPengumumanPPDB<%=rnd%>(<%= i %>)"><%= i %></button>
                        </li>
                    <%  } else if (i == currentPage - 2 || i == currentPage + 2) { %>
                        <li class="page-item disabled"><span class="page-link text-secondary">...</span></li>
                    <%  }
                    } %>
                    <li class="page-item <%= (currentPage == totalPages) ? "disabled" : "" %>">
                        <button class="page-link rounded-end-pill text-primary fw-bold px-3" onclick="loadPengumumanPPDB<%=rnd%>(<%= currentPage + 1 %>)">
                            <i class="fas fa-chevron-right"></i>
                        </button>
                    </li>
                </ul>
            </nav>
        </div>
    <%
        }
    } 
    %>
</div>