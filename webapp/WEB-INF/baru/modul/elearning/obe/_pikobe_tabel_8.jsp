<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.obe.CapaianLulusan"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Kurikulum"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>

<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
String action = request.getParameter("action");
String idJurusanStr = request.getParameter("idJurusan");
String keyword = request.getParameter("keyword");

Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) return;

Long idJurusan = null;
try { if(idJurusanStr != null && !idJurusanStr.trim().isEmpty()) idJurusan = Long.parseLong(idJurusanStr); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_8.jsp:28");}

if(idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

if ("loadTabel8".equals(action)) {
    List<CapaianLulusan> listCpl = new ArrayList<CapaianLulusan>();
    List<KurikulumPunyaMatakuliah> listKpm = new ArrayList<KurikulumPunyaMatakuliah>();
    Kurikulum kurikulumAktif = null;
    int maxSemester = 8;
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        
        Criteria critKurikulum = sess.createCriteria(Kurikulum.class).add(Restrictions.eq("jurusan.id", idJurusan)).add(Restrictions.eq("obe", true)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("id")); 
        List<Kurikulum> kList = critKurikulum.list();
        if(!kList.isEmpty()) {
            kurikulumAktif = kList.get(0);
            if(kurikulumAktif.getJurusan() != null && kurikulumAktif.getJurusan().getJenjang() != null && kurikulumAktif.getJurusan().getJenjang().getJumlahSemester() != null) {
                maxSemester = kurikulumAktif.getJurusan().getJenjang().getJumlahSemester();
            }
            listKpm = ConstantValues.simpleList(sess.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("matakuliah", "mk").add(Restrictions.eq("kurikulum.id", kurikulumAktif.getId())).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.or(Restrictions.isNull("mk.aktif"), Restrictions.eq("mk.aktif", true))).addOrder(Order.asc("semester")).addOrder(Order.asc("mk.kode")), KurikulumPunyaMatakuliah.class);
            
            Criteria critCpl = sess.createCriteria(CapaianLulusan.class).add(Restrictions.eq("jurusan.id", idJurusan)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            if (keyword != null && !keyword.trim().isEmpty()) {
                critCpl.add(Restrictions.or(Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE), Restrictions.ilike("nama", keyword, MatchMode.ANYWHERE)));
            }
            listCpl = ConstantValues.simpleList(critCpl.addOrder(Order.asc("kode")), CapaianLulusan.class);
        }
    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_8.jsp:60"); } finally { if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); } }
%>
    <% if(kurikulumAktif == null) { %>
        <div class="alert alert-warning text-center"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Kurikulum OBE aktif untuk Program Studi ini tidak ditemukan.")%></div>
    <% } else if(listCpl.isEmpty()) { %>
        <div class="alert alert-warning text-center"><i class="fas fa-folder-open me-2"></i><%=Common.getBahasaConfig("Belum ada data Capaian Lulusan (CPL) yang direkam.")%></div>
    <% } else { %>
        <div class="mb-3 bg-white p-3 rounded shadow-sm border text-center">
            <span class="text-muted small fw-bold d-block"><%=Common.getBahasaConfig("Kurikulum Aktif yang Dipetakan:")%></span>
            <span class="text-primary fw-bold" style="font-size: 1.1rem;"><%=kurikulumAktif.getNama()%></span>
        </div>

        <div class="table-responsive border rounded-3 animate__animated animate__fadeIn" style="max-height: 700px; overflow-y: auto;">
            <table class="table table-bordered table-hover align-middle mb-0 text-center small">
                <thead class="table-light sticky-top shadow-sm" style="z-index: 10;">
                    <tr>
                        <th rowspan="2" class="align-middle bg-light text-start ps-3" style="min-width: 150px;"><%=Common.getBahasaConfig("Capaian Lulusan (CPL)")%></th>
                        <th colspan="<%=maxSemester%>" class="bg-light"><%=Common.getBahasaConfig("Sebaran Matakuliah Tiap Semester")%></th>
                    </tr>
                    <tr>
                        <% for(int s=1; s<=maxSemester; s++) { %><th class="fw-bold text-success bg-white" style="min-width: 120px;"><%=Common.getBahasaConfig("Smt")%> <%=s%></th><% } %>
                    </tr>
                </thead>
                <tbody>
                    <% for(CapaianLulusan cpl : listCpl) { String idCplStr = "," + cpl.getId() + ","; %>
                        <tr>
                            <td class="text-start ps-3 text-dark bg-white" style="vertical-align: top;">
                                <span class="badge bg-primary mb-1 shadow-sm"><%=cpl.getKode()%></span><br>
                                <span class="text-muted small"><%=cpl.getNama()%></span>
                            </td>
                            <% for(int s=1; s<=maxSemester; s++) { %>
                                <td class="bg-white p-2" style="vertical-align: top;">
                                    <div class="d-flex flex-column gap-1">
                                    <% 
                                    boolean hasMk = false;
                                    for(KurikulumPunyaMatakuliah kpm : listKpm) {
                                        if(kpm.getSemester() != null && kpm.getSemester() == s) {
                                            Matakuliah mk = kpm.getMatakuliah();
                                            if(mk.getCapaianLulusan() != null && mk.getCapaianLulusan().contains(idCplStr)) {
                                                hasMk = true;
                                    %>
                                                <span class="badge bg-info text-dark text-start text-wrap shadow-sm border border-light p-2" title="<%=mk.getNama()%>"><%=mk.getKode()%></span>
                                    <%      }
                                        }
                                    } 
                                    if(!hasMk) out.print("<span class='text-muted' style='opacity: 0.3;'>-</span>");
                                    %>
                                    </div>
                                </td>
                            <% } %>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
        <div class="mt-3 text-muted small"><i class="fas fa-info-circle me-1 text-primary"></i><%=Common.getBahasaConfig("Tabel ini bersifat Laporan (Read-Only). Untuk memetakan Matakuliah ke CPL/CPMK, silakan gunakan Tabel 9.")%></div>
    <% } %>
<% } %>