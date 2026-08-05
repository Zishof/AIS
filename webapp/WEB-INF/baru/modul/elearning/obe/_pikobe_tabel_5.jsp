<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.obe.BahanKajian"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Kurikulum"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>

<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
String action = request.getParameter("action");
String idJurusanStr = request.getParameter("idJurusan");
String keywordMk = request.getParameter("keywordMk");
String keywordBk = request.getParameter("keywordBk");

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
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_5.jsp:40");}

if(idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

// ==========================================
// ACTION: LOAD MATRIKS TABEL 5
// ==========================================
if ("loadTabel5".equals(action)) {
    List<BahanKajian> listBK = new ArrayList<BahanKajian>();
    List<Matakuliah> listMK = new ArrayList<Matakuliah>();
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        
        // 1. Tarik Data Kolom (Bahan Kajian)
        Criteria critBK = sess.createCriteria(BahanKajian.class);
        critBK.add(Restrictions.eq("jurusan.id", idJurusan));
        critBK.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        
        if (keywordBk != null && !keywordBk.trim().isEmpty()) {
            Disjunction orBK = Restrictions.disjunction();
            orBK.add(Restrictions.ilike("kode", keywordBk, MatchMode.ANYWHERE));
            orBK.add(Restrictions.ilike("nama", keywordBk, MatchMode.ANYWHERE));
            critBK.add(orBK);
        }
        critBK.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
        listBK = ConstantValues.simpleList(critBK, BahanKajian.class);
        
        // 2. Tarik Data Baris (Matakuliah yang terkait Kurikulum OBE Aktif di Jurusan tsb)
        // Ambil ID Kurikulum OBE
        List<Long> idKurikulums = sess.createCriteria(Kurikulum.class)
            .setProjection(Projections.property("id"))
            .add(Restrictions.eq("jurusan.id", idJurusan))
            .add(Restrictions.eq("obe", true))
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
            .list();

        if(!idKurikulums.isEmpty()) {
            // Ambil ID Matakuliah dari KurikulumPunyaMatakuliah
            List<Long> idMks = sess.createCriteria(KurikulumPunyaMatakuliah.class)
                .createAlias("kurikulum", "k")
                .createAlias("matakuliah", "mk")
                .setProjection(Projections.distinct(Projections.property("mk.id")))
                .add(Restrictions.in("k.id", idKurikulums))
                .list();

            if(!idMks.isEmpty()) {
                Criteria critMK = sess.createCriteria(Matakuliah.class);
                critMK.add(Restrictions.in("id", idMks));
                
                if (keywordMk != null && !keywordMk.trim().isEmpty()) {
                    Disjunction orMK = Restrictions.disjunction();
                    orMK.add(Restrictions.ilike("kode", keywordMk, MatchMode.ANYWHERE));
                    orMK.add(Restrictions.ilike("nama", keywordMk, MatchMode.ANYWHERE));
                    critMK.add(orMK);
                }
                critMK.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
                listMK = ConstantValues.simpleList(critMK, Matakuliah.class);
            }
        }

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_5.jsp:106");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }
%>

    <% if(listBK.isEmpty()) { %>
        <div class="alert alert-warning text-center py-4 border-0 shadow-sm rounded-3">
            <i class="fas fa-exclamation-triangle fa-2x mb-2 d-block text-warning"></i>
            <%=Common.getBahasaConfig("Data Bahan Kajian (Kolom) tidak ditemukan. Silakan isi terlebih dahulu di Tabel 4.")%>
        </div>
    <% } else if(listMK.isEmpty()) { %>
        <div class="alert alert-warning text-center py-4 border-0 shadow-sm rounded-3">
            <i class="fas fa-exclamation-triangle fa-2x mb-2 d-block text-warning"></i>
            <%=Common.getBahasaConfig("Data Matakuliah OBE tidak ditemukan. Pastikan Anda telah mengatur Kurikulum (OBE = Ya) dan merelasikannya dengan Matakuliah.")%>
        </div>
    <% } else { %>
        <div class="table-responsive border rounded-3 animate__animated animate__fadeIn" style="max-height: 600px;">
            <table class="table table-bordered table-hover align-middle mb-0 text-center small">
                <thead class="table-light sticky-top shadow-sm" style="z-index: 10;">
                    <tr>
                        <th rowspan="2" class="align-middle bg-light text-start ps-3" style="min-width: 300px;"><%=Common.getBahasaConfig("Matakuliah")%></th>
                        <th colspan="<%=listBK.size()%>" class="bg-light"><%=Common.getBahasaConfig("Bahan Kajian (BK)")%></th>
                    </tr>
                    <tr>
                        <% for(BahanKajian bk : listBK) { %>
                            <th class="fw-bold text-success bg-white" title="<%=bk.getNama()%>" style="width: 50px;"><%=bk.getKode()%></th>
                        <% } %>
                    </tr>
                </thead>
                <tbody>
                    <% 
                    int no = 1;
                    for(Matakuliah mk : listMK) { 
                        String bkMapped = mk.getBahanKajian() != null ? mk.getBahanKajian() : "";
                    %>
                        <tr>
                            <td class="text-start ps-3 text-dark">
                                <span class="badge bg-secondary me-2"><%=mk.getKode()%></span>
                                <%=mk.getNama()%>
                            </td>
                            <% for(BahanKajian bk : listBK) { 
                                boolean isChecked = bkMapped.contains("," + bk.getId() + ",");
                            %>
                                <td>
                                    <input type="checkbox" class="form-check-input border-secondary shadow-sm" style="width: 1.2rem; height: 1.2rem; cursor: pointer;"
                                           <%=isChecked ? "checked" : ""%> 
                                           <%=!edit ? "disabled" : ""%>
                                           onchange="window.updateKorelasiMKBK<%=rnd%>(this, '<%=mk.getId()%>', '<%=bk.getId()%>')">
                                </td>
                            <% } %>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
        <div class="mt-3 text-muted small"><i class="fas fa-info-circle me-1 text-primary"></i><%=Common.getBahasaConfig("Arahkan kursor ke Kode BK (Header) untuk melihat nama bahan kajian. Centang kotak untuk mengaitkan Matakuliah dengan Bahan Kajian.")%></div>
    <% } %>

<%
}
%>