<%@page import="ais.database.model.GelombangPendaftaran"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.jsoup.Jsoup"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="ais.common.Common"%>
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
    int limitPerPage = 5;

    Session sess = HibernateUtil.openSession();
    List<PengumumanAkademis> rawList = new ArrayList<PengumumanAkademis>();

    String currentLang = Tbmuser.INDONESIA;
    Object langAttr = session.getAttribute("current_lang");
    if (langAttr != null) {
        currentLang = (String) langAttr;
    }
    boolean isIndo = currentLang.equals(Tbmuser.INDONESIA);

    try {
        Criteria criteria = sess.createCriteria(PengumumanAkademis.class);
        criteria.add(Restrictions.eq("aktif", true));
        
        // FILTER KHUSUS UNTUK ALUMNI
        criteria.add(
            Restrictions.ilike("diperuntukkan", PengumumanAkademis.UNTUK_ALUMNI, MatchMode.ANYWHERE)
        );
        
        // PENCARIAN
        if (keyword != null && !keyword.trim().isEmpty()) {
            if (isIndo) {
                criteria.add(Restrictions.ilike("judul", keyword, MatchMode.ANYWHERE));
            } else {
                criteria.add(Restrictions.ilike("judulEn", keyword, MatchMode.ANYWHERE));
            }
        }
        
        // PENGURUTAN
        criteria.addOrder(Order.desc("tanggal"));
        rawList = ConstantValues.simpleList(criteria, PengumumanAkademis.class);
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/alumni/_pengumuman_alumni.jsp:58");
    } finally {
        try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_pengumuman_alumni.jsp:60");}
        try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_pengumuman_alumni.jsp:61");}
        HibernateUtil.closeSessionQuietly(sess);
    }
%>

<div class="list-group list-group-flush rounded-bottom-4">
    <% 
    if (rawList == null || rawList.isEmpty()) { 
    %>
        <div class="p-4 text-center text-muted">
            <i class="fas fa-inbox fa-2x mb-2 opacity-50 d-block"></i>
            <span class="small"><%= Common.getBahasaConfig("Saat ini tidak terdapat informasi terbaru untuk alumni.") %></span>
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

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy");
        for(PengumumanAkademis p : pagedList) {
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
                if (isiDisplay.length() > 90) {
                    isiDisplay = isiDisplay.substring(0, 90) + "...";
                }
            } else {
                isiDisplay = "";
            }

            String tglDisplay = p.getTanggal() != null ? sdf.format(p.getTanggal()) : "";
    %>
        <a href="javascript:void(0)" onclick="detailPengumuman<%=rnd%>('<%=p.getId()%>')" 
           class="list-group-item list-group-item-action p-3 border-start border-4 border-primary border-0 mb-1 hover-bg-light transition-base text-decoration-none animate__animated animate__fadeIn">
            <div class="d-flex w-100 justify-content-between align-items-start mb-1">
                <h6 class="mb-0 text-primary fw-bold" style="line-height: 1.4; max-width: 85%;">
                    <%=judulDisplay%>
                </h6>
                <small class="text-muted text-nowrap ms-2" style="font-size: 0.75rem;">
                    <i class="far fa-calendar-alt me-1"></i><%=tglDisplay%>
                </small>
            </div>
           
            <p class="mb-1 text-muted small" style="line-height: 1.5;">
                <%=isiDisplay%>
            </p>
            <div class="mt-2 text-end">
                <span class="badge bg-light text-primary border small"><%= Common.getBahasaConfig("Baca Selengkapnya") %> <i class="fas fa-arrow-right ms-1"></i></span>
            </div>
        </a>
  
    <% 
        } // end loop

        // RENDER PAGINASI
        if (totalPages > 1) {
    %>
        <div class="p-3 border-top d-flex justify-content-center bg-white rounded-bottom-4 mt-2">
            <nav>
                <ul class="pagination pagination-sm shadow-sm rounded-pill mb-0">
            
                    <li class="page-item <%= (currentPage == 1) ? "disabled" : "" %>">
                        <button class="page-link rounded-start-pill text-primary fw-bold" onclick="loadPengumuman<%=rnd%>(<%= (currentPage - 1) %>)"><i class="fas fa-angle-left"></i></button>
                    </li>
                    <% for (int i = 1; i <= totalPages; i++) { 
           
                        if (i == 1 || i == totalPages || (i >= currentPage - 1 && i <= currentPage + 1)) {
                    %>
                        <li class="page-item <%= (currentPage == i) ? "active" : "" %>">
                            <button class="page-link <%= (currentPage == i) ? "bg-primary text-white border-primary fw-bold" : "text-secondary" %>" onclick="loadPengumuman<%=rnd%>(<%=i%>)"><%=i%></button>
                        </li>
                    <%  } else if (i == currentPage - 2 || i == currentPage + 2) { %>
                        <li class="page-item disabled"><span class="page-link text-secondary">...</span></li>
                    <%  }
                    } %>
                    <li class="page-item <%= (currentPage == totalPages) ? "disabled" : "" %>">
                        <button class="page-link rounded-end-pill text-primary fw-bold" onclick="loadPengumuman<%=rnd%>(<%= (currentPage + 1) %>)"><i class="fas fa-angle-right"></i></button>
                    </li>
                </ul>
            </nav>
        </div>
    <%
        }
    } 
    %>
</div>

<style>
    .hover-bg-light:hover { background-color: #f8f9fa !important; }
    .transition-base { transition: all 0.3s ease-in-out; }
</style>