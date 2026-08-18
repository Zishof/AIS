<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.obe.CapaianLulusan"%>
<%@page import="ais.database.model.obe.ProfilLulusan"%>

<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
String action = request.getParameter("action");
String idJurusanStr = request.getParameter("idJurusan");
String keywordCpl = request.getParameter("keywordCpl");
String keywordPl = request.getParameter("keywordPl");

Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) return;

boolean edit = false;
if(tbmuser != null && tbmuser.getUserId() != null){
    edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    if(Common.getApakahAdminLain(tbmuser)){ edit = true; }
}

Long idJurusan = null;
try {
    if(idJurusanStr != null && !idJurusanStr.trim().isEmpty()){
        idJurusan = Long.parseLong(idJurusanStr);
    }
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_3.jsp:37");}

if(idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

// ==========================================
// ACTION: LOAD MATRIKS TABEL 3
// ==========================================
if ("loadTabel3".equals(action)) {
    List<ProfilLulusan> listPL = new ArrayList<ProfilLulusan>();
    List<CapaianLulusan> listCPL = new ArrayList<CapaianLulusan>();
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        
        // 1. Tarik Data Kolom (Profil Lulusan)
        Criteria critPL = sess.createCriteria(ProfilLulusan.class);
        critPL.add(Restrictions.eq("jurusan.id", idJurusan));
        critPL.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        
        if (keywordPl != null && !keywordPl.trim().isEmpty()) {
            Disjunction orPL = Restrictions.disjunction();
            orPL.add(Restrictions.ilike("kode", keywordPl, MatchMode.ANYWHERE));
            orPL.add(Restrictions.ilike("nama", keywordPl, MatchMode.ANYWHERE));
            critPL.add(orPL);
        }
        critPL.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
        listPL = ConstantValues.simpleList(critPL, ProfilLulusan.class);
        
        // 2. Tarik Data Baris (Capaian Lulusan)
        Criteria critCPL = sess.createCriteria(CapaianLulusan.class);
        critCPL.add(Restrictions.eq("jurusan.id", idJurusan));
        critCPL.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        
        if (keywordCpl != null && !keywordCpl.trim().isEmpty()) {
            Disjunction orCPL = Restrictions.disjunction();
            orCPL.add(Restrictions.ilike("kode", keywordCpl, MatchMode.ANYWHERE));
            orCPL.add(Restrictions.ilike("nama", keywordCpl, MatchMode.ANYWHERE));
            critCPL.add(orCPL);
        }
        critCPL.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
        listCPL = ConstantValues.simpleList(critCPL, CapaianLulusan.class);

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_3.jsp:84");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }
%>

    <% if(listPL.isEmpty()) { %>
        <div class="alert alert-warning text-center py-4 border-0 shadow-sm rounded-3">
            <i class="fas fa-exclamation-triangle fa-2x mb-2 d-block text-warning"></i>
            <%=Common.getBahasaConfig("Data Profil Lulusan (Kolom) tidak ditemukan. Silakan isi terlebih dahulu di Tabel 1.")%>
        </div>
    <% } else if(listCPL.isEmpty()) { %>
        <div class="alert alert-warning text-center py-4 border-0 shadow-sm rounded-3">
            <i class="fas fa-exclamation-triangle fa-2x mb-2 d-block text-warning"></i>
            <%=Common.getBahasaConfig("Data Capaian Pembelajaran Lulusan (Baris) tidak ditemukan. Silakan isi terlebih dahulu di Tabel 2.")%>
        </div>
    <% } else { %>
        <div class="table-responsive border rounded-3 animate__animated animate__fadeIn" style="max-height: 600px;">
            <table class="table table-bordered table-hover align-middle mb-0 text-center small">
                <thead class="table-light sticky-top shadow-sm" style="z-index: 10;">
                    <tr>
                        <th rowspan="2" class="align-middle bg-light text-start ps-3" style="min-width: 300px;"><%=Common.getBahasaConfig("Capaian Lulusan (CPL)")%></th>
                        <th colspan="<%=listPL.size()%>" class="bg-light"><%=Common.getBahasaConfig("Profil Lulusan (PL)")%></th>
                    </tr>
                    <tr>
                        <% for(ProfilLulusan pl : listPL) { %>
                            <th class="fw-bold text-primary bg-white" title="<%=pl.getNama()%>" style="width: 80px;"><%=pl.getKode()%></th>
                        <% } %>
                    </tr>
                </thead>
                <tbody>
                    <% 
                    int no = 1;
                    for(CapaianLulusan cpl : listCPL) { 
                        String pMapped = cpl.getProfil() != null ? cpl.getProfil() : "";
                    %>
                        <tr>
                            <td class="text-start ps-3 text-dark">
                                <span class="badge bg-secondary me-2"><%=cpl.getKode()%></span>
                                <%=cpl.getNama()%>
                            </td>
                            <% for(ProfilLulusan pl : listPL) { 
                                boolean isChecked = pMapped.contains("," + pl.getId() + ",");
                            %>
                                <td>
                                    <input type="checkbox" class="form-check-input border-secondary shadow-sm" style="width: 1.2rem; height: 1.2rem; cursor: pointer;"
                                           <%=isChecked ? "checked" : ""%> 
                                           <%=!edit ? "disabled" : ""%>
                                           onchange="window.updateKorelasiCPLPL<%=rnd%>(this, '<%=cpl.getId()%>', '<%=pl.getId()%>')">
                                </td>
                            <% } %>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
        <div class="mt-3 text-muted small"><i class="fas fa-info-circle me-1 text-primary"></i><%=Common.getBahasaConfig("Arahkan kursor ke Kode PL (Header) untuk melihat nama profesinya. Centang kotak untuk mengaitkan CPL dengan PL.")%></div>
    <% } %>

<%
}
%>