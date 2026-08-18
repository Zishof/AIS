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
<%@page import="ais.database.model.obe.ProfilLulusan"%>
<%@page import="ais.database.model.obe.ProfesiLulusan"%>
<%@page import="ais.database.model.Kurikulum"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>

<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
String action = request.getParameter("action");
String idJurusanStr = request.getParameter("idJurusan");
String keyword = request.getParameter("keyword");

Tbmuser tbmuser = Common.getCurrentUser(request);

if (tbmuser == null || tbmuser.getUserId() == null) return;

// --- PENERAPAN HAK AKSES ---
boolean edit = false;
boolean delete = false;

if(tbmuser != null && tbmuser.getUserId() != null){
    edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
    if(Common.getApakahAdminLain(tbmuser)){
        edit = true; delete = true; 
    }
}

Long idJurusan = null;
try {
    if(idJurusanStr != null && !idJurusanStr.trim().isEmpty()){
        idJurusan = Long.parseLong(idJurusanStr);
    }
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_service.jsp:45");}

if(idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

// ==========================================
// ACTION 1: LOAD TABEL 1 (PROFIL LULUSAN)
// ==========================================
if ("loadTabel1".equals(action)) {
    List<ProfilLulusan> listProfil = new ArrayList<ProfilLulusan>();
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        Criteria crit = sess.createCriteria(ProfilLulusan.class);
        crit.add(Restrictions.eq("jurusan.id", idJurusan));
        
        // PENCARIAN BERDASARKAN KEYWORD (KODE, NAMA, KETERANGAN)
        if (keyword != null && !keyword.trim().isEmpty()) {
            Disjunction or = Restrictions.disjunction();
            or.add(Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("nama", keyword, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE));
            crit.add(or);
        }
        
        crit.addOrder(Order.asc("kode"));
        listProfil = ConstantValues.simpleList(crit, ProfilLulusan.class);
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_service.jsp:77");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }
%>
    <div class="table-responsive border rounded-3 animate__animated animate__fadeIn">
        <table class="table table-hover align-middle mb-0 text-start small">
            <thead class="table-light border-bottom">
                <tr>
                    <th class="text-center" style="width: 50px;"><%=Common.getBahasaConfig("No")%></th>
                    <th style="width: 15%;"><%=Common.getBahasaConfig("Kode PL")%></th>
                    <th style="width: 40%;"><%=Common.getBahasaConfig("Deskripsi Profil Lulusan (PL)")%></th>
                    <th style="width: 30%;"><%=Common.getBahasaConfig("Profesi Lulusan")%></th>
                    <% if(edit || delete) { %><th class="text-center" style="width: 100px;"><%=Common.getBahasaConfig("Aksi")%></th><% } %>
                </tr>
            </thead>
            <tbody>
                <% if(listProfil.isEmpty()) { %>
                    <tr>
                        <td colspan="<%= (edit||delete) ? 5 : 4 %>" class="text-center py-5 text-muted fst-italic">
                            <i class="fas fa-folder-open d-block fa-3x mb-3 text-secondary opacity-50"></i>
                            <span class="fw-bold"><%=Common.getBahasaConfig("Data Kosong")%></span><br>
                            <%=Common.getBahasaConfig("Belum ada data Profil Lulusan yang sesuai untuk program studi ini.")%>
                        </td>
                    </tr>
                <% } else { 
                    int no = 1;
                    for(ProfilLulusan pl : listProfil) {
                        String kodePL = pl.getKode() != null ? pl.getKode() : "-";
                        String namaPL = pl.getNama() != null ? pl.getNama() : "-";
                        
                        String namaProfesi = "-";
                        String idProfesi = "";
                        if(pl.getProfesiLulusan() != null) {
                            namaProfesi = pl.getProfesiLulusan().getNama() != null ? pl.getProfesiLulusan().getNama() : "-";
                            idProfesi = pl.getProfesiLulusan().getId().toString();
                        }
                %>
                    <tr>
                        <td class="text-center text-muted fw-bold"><%=no++%></td>
                        <td class="fw-bold text-dark"><span class="badge bg-primary px-3 py-2 shadow-sm"><%=kodePL%></span></td>
                        <td class="text-dark fw-medium"><%=namaPL%></td>
                        <td class="text-secondary"><%=namaProfesi.replaceAll("\n", "<br>")%></td>
                        
                        <% if(edit || delete) { %>
                        <td class="text-center text-nowrap">
                            <% if(edit) { %>
                            <button type="button" class="btn btn-sm btn-outline-primary rounded-circle shadow-sm me-1" 
                                    onclick="window.bukaModalPL<%=rnd%>('<%=pl.getId()%>', '<%=kodePL.replace("'", "\\'")%>', '<%=namaPL.replace("'", "\\'").replace("\n", "\\n")%>', '<%=idProfesi%>')" 
                                    title="<%=Common.getBahasaConfig("Ubah Data")%>">
                                <i class="fas fa-edit"></i>
                            </button>
                            <% } %>
                            <% if(delete) { %>
                            <button type="button" class="btn btn-sm btn-outline-danger rounded-circle shadow-sm" 
                                    onclick="window.hapusPL<%=rnd%>('<%=pl.getId()%>')" 
                                    title="<%=Common.getBahasaConfig("Hapus Data")%>">
                                <i class="fas fa-trash"></i>
                            </button>
                            <% } %>
                        </td>
                        <% } %>
                    </tr>
                <%  }
                } %>
            </tbody>
        </table>
    </div>
<%
} 
// ==========================================
// ACTION 2: LOAD OPSI PROFESI (DROPDOWN MODAL)
// ==========================================
else if ("loadOpsiProfesi".equals(action)) {
    List<ProfesiLulusan> listProfesi = new ArrayList<ProfesiLulusan>();
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        listProfesi = ConstantValues.simpleList(
            sess.createCriteria(ProfesiLulusan.class)
                .add(Restrictions.eq("jurusan.id", idJurusan))
                .addOrder(Order.asc("nama")), 
            ProfesiLulusan.class
        );
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_service.jsp:163");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }
%>
    <option value="">-- <%=Common.getBahasaConfig("Pilih Profesi Lulusan")%> --</option>
    <% for(ProfesiLulusan prof : listProfesi) { %>
        <option value="<%=prof.getId()%>"><%=prof.getNama()%></option>
    <% } %>
<%
}
// ==========================================
// ACTION 3: LOAD OPSI MATA KULIAH / KPM (DROPDOWN TAB ASESMEN)
// ==========================================
else if ("loadOpsiKpm".equals(action)) {
    List<KurikulumPunyaMatakuliah> listKpm = new ArrayList<KurikulumPunyaMatakuliah>();
    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        Kurikulum kurAktif = (Kurikulum) sess.createCriteria(Kurikulum.class)
            .add(Restrictions.eq("jurusan.id", idJurusan))
            .add(Restrictions.eq("obe", true))
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
            .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
        if (kurAktif != null) {
            listKpm = ConstantValues.simpleList(
                sess.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("matakuliah", "mk")
                    .add(Restrictions.eq("kurikulum.id", kurAktif.getId()))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("mk.kode")),
                KurikulumPunyaMatakuliah.class);
        }
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_service.jsp:196");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }
%>
    <option value="">-- <%=Common.getBahasaConfig("Pilih Mata Kuliah")%> --</option>
    <% for(KurikulumPunyaMatakuliah kpm : listKpm) {
         String kodeMk = (kpm.getMatakuliah() != null && kpm.getMatakuliah().getKode() != null) ? kpm.getMatakuliah().getKode() : "";
         String namaMk = (kpm.getMatakuliah() != null && kpm.getMatakuliah().getNama() != null) ? kpm.getMatakuliah().getNama() : "-";
    %>
        <option value="<%=kpm.getId()%>"><%=kodeMk%> - <%=namaMk%></option>
    <% } %>
<%
}
%>