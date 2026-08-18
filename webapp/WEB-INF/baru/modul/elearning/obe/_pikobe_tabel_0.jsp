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
<%@page import="ais.database.model.obe.ProfesiLulusan"%>

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
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_0.jsp:41");}

if(idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

// ==========================================
// ACTION 1: LOAD TABEL 0 (PROFESI LULUSAN)
// ==========================================
if ("loadTabel".equals(action)) {
    List<ProfesiLulusan> listProfesi = new ArrayList<ProfesiLulusan>();
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        Criteria crit = sess.createCriteria(ProfesiLulusan.class);
        crit.add(Restrictions.eq("jurusan.id", idJurusan));
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            Disjunction or = Restrictions.disjunction();
            or.add(Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("nama", keyword, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE));
            crit.add(or);
        }
        
        crit.addOrder(Order.asc("kode"));
        listProfesi = ConstantValues.simpleList(crit, ProfesiLulusan.class);
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_0.jsp:72");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }
%>
    <div class="table-responsive border rounded-3 animate__animated animate__fadeIn">
        <table class="table table-hover align-middle mb-0 text-start small">
            <thead class="table-light border-bottom">
                <tr>
                    <th class="text-center" style="width: 50px;"><%=Common.getBahasaConfig("No")%></th>
                    <th style="width: 15%;"><%=Common.getBahasaConfig("Kode Profesi")%></th>
                    <th style="width: 30%;"><%=Common.getBahasaConfig("Nama Profesi Lulusan")%></th>
                    <th style="width: 30%;"><%=Common.getBahasaConfig("Keterangan")%></th>
                    <th class="text-center" style="width: 10%;"><%=Common.getBahasaConfig("Aktif")%></th>
                    <% if(edit || delete) { %><th class="text-center" style="width: 10%;"><%=Common.getBahasaConfig("Aksi")%></th><% } %>
                </tr>
            </thead>
            <tbody>
                <% if(listProfesi.isEmpty()) { %>
                    <tr>
                        <td colspan="<%= (edit||delete) ? 6 : 5 %>" class="text-center py-5 text-muted fst-italic">
                            <i class="fas fa-briefcase d-block fa-3x mb-3 text-secondary opacity-50"></i>
                            <span class="fw-bold"><%=Common.getBahasaConfig("Data Kosong")%></span><br>
                            <%=Common.getBahasaConfig("Belum ada data Profesi Lulusan yang sesuai untuk program studi ini.")%>
                        </td>
                    </tr>
                <% } else { 
                    int no = 1;
                    for(ProfesiLulusan prof : listProfesi) {
                        String kode = prof.getKode() != null ? prof.getKode() : "-";
                        String nama = prof.getNama() != null ? prof.getNama() : "-";
                        String ket = prof.getKeterangan() != null ? prof.getKeterangan() : "-";
                        boolean aktif = prof.getAktif() == null ? true : prof.getAktif();
                %>
                    <tr>
                        <td class="text-center text-muted fw-bold"><%=no++%></td>
                        <td class="fw-bold text-dark"><span class="badge bg-primary px-3 py-2 shadow-sm"><%=kode%></span></td>
                        <td class="text-dark fw-medium"><%=nama%></td>
                        <td class="text-secondary"><%=ket%></td>
                        <td class="text-center">
                            <% if(aktif) { %>
                                <span class="badge bg-success"><i class="fas fa-check"></i> <%=Common.getBahasaConfig("Ya")%></span>
                            <% } else { %>
                                <span class="badge bg-danger"><i class="fas fa-times"></i> <%=Common.getBahasaConfig("Tidak")%></span>
                            <% } %>
                        </td>
                        
                        <% if(edit || delete) { %>
                        <td class="text-center text-nowrap">
                            <% if(edit) { %>
                            <button type="button" class="btn btn-sm btn-outline-primary rounded-circle shadow-sm me-1" 
                                    onclick="window.bukaModalProfesi<%=rnd%>('<%=prof.getId()%>', '<%=kode.replace("'", "\\'")%>', '<%=nama.replace("'", "\\'").replace("\n", "\\n")%>', '<%=ket.replace("'", "\\'").replace("\n", "\\n")%>', <%=aktif%>)" 
                                    title="<%=Common.getBahasaConfig("Ubah Data")%>">
                                <i class="fas fa-edit"></i>
                            </button>
                            <% } %>
                            <% if(delete) { %>
                            <button type="button" class="btn btn-sm btn-outline-danger rounded-circle shadow-sm" 
                                    onclick="window.hapusProfesi<%=rnd%>('<%=prof.getId()%>')" 
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
%>