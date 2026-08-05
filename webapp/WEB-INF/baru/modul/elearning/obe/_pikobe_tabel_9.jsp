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
<%@page import="ais.database.model.obe.CapaianLulusan"%>
<%@page import="ais.database.model.obe.CapaianPembelajaranLulusan"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Kurikulum"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>

<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
String action = request.getParameter("action");
String idJurusanStr = request.getParameter("idJurusan");
String keywordCpl = request.getParameter("keywordCpl");

Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) return;

boolean edit = false;
if(tbmuser != null && tbmuser.getUserId() != null){
    edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    if(Common.getApakahAdminLain(tbmuser)){ edit = true; }
}

Long idJurusan = null;
try { if(idJurusanStr != null && !idJurusanStr.trim().isEmpty()) idJurusan = Long.parseLong(idJurusanStr); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_9.jsp:37");}
if(idJurusan == null) return;

// ==========================================
// ACTION 1: LOAD MATRIKS TABEL 9
// ==========================================
if ("loadTabel9".equals(action)) {
    List<CapaianLulusan> listCPL = new ArrayList<CapaianLulusan>();
    Map<Long, List<CapaianPembelajaranLulusan>> mapCPMK = new HashMap<Long, List<CapaianPembelajaranLulusan>>();
    List<KurikulumPunyaMatakuliah> listKpm = new ArrayList<KurikulumPunyaMatakuliah>();
    Kurikulum kurikulumAktif = null;
    int maxSemester = 8;
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        kurikulumAktif = (Kurikulum) sess.createCriteria(Kurikulum.class).add(Restrictions.eq("jurusan.id", idJurusan)).add(Restrictions.eq("obe", true)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
        
        if(kurikulumAktif != null) {
            if (kurikulumAktif.getJurusan() != null && kurikulumAktif.getJurusan().getJenjang() != null && kurikulumAktif.getJurusan().getJenjang().getJumlahSemester() != null) {
                maxSemester = kurikulumAktif.getJurusan().getJenjang().getJumlahSemester();
            }
            
            Criteria cplCrit = sess.createCriteria(CapaianLulusan.class).add(Restrictions.eq("jurusan.id", idJurusan)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            if (keywordCpl != null && !keywordCpl.trim().isEmpty()) {
                cplCrit.add(Restrictions.or(Restrictions.ilike("kode", keywordCpl, MatchMode.ANYWHERE), Restrictions.ilike("nama", keywordCpl, MatchMode.ANYWHERE)));
            }
            listCPL = ConstantValues.simpleList(cplCrit.addOrder(Order.asc("kode")), CapaianLulusan.class);

            List<CapaianPembelajaranLulusan> allCPMK = ConstantValues.simpleList(sess.createCriteria(CapaianPembelajaranLulusan.class).add(Restrictions.eq("jurusan.id", idJurusan)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("kode")), CapaianPembelajaranLulusan.class);
            
            for(CapaianLulusan cpl : listCPL) {
                List<CapaianPembelajaranLulusan> subList = new ArrayList<CapaianPembelajaranLulusan>();
                String pMapped = cpl.getCapaianPembelajaranLulusan() != null ? cpl.getCapaianPembelajaranLulusan() : "";
                for(CapaianPembelajaranLulusan cp : allCPMK) {
                    if(pMapped.contains("," + cp.getId() + ",")) subList.add(cp);
                }
                mapCPMK.put(cpl.getId(), subList);
            }

            listKpm = ConstantValues.simpleList(sess.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("matakuliah", "mk").add(Restrictions.eq("kurikulum.id", kurikulumAktif.getId())).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("mk.kode")), KurikulumPunyaMatakuliah.class);
        }
    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_9.jsp:79"); } finally { if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); } }
%>
    <% if(kurikulumAktif == null) { %>
        <div class="alert alert-warning text-center"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Kurikulum OBE tidak ditemukan.")%></div>
    <% } else { %>
        <div class="table-responsive border rounded-3 animate__animated animate__fadeIn" style="max-height: 700px; overflow-y: auto;">
            <table class="table table-bordered align-middle mb-0 text-center small">
                <thead class="table-light sticky-top shadow-sm" style="z-index: 10;">
                    <tr>
                        <th rowspan="2" style="min-width: 150px;" class="align-middle bg-light"><%=Common.getBahasaConfig("Capaian Lulusan (CPL)")%></th>
                        <th rowspan="2" style="min-width: 120px;" class="align-middle bg-light"><%=Common.getBahasaConfig("Kode CPMK")%></th>
                        <th rowspan="2" style="min-width: 250px;" class="align-middle bg-light"><%=Common.getBahasaConfig("Deskripsi CPMK / Sub-CPMK")%></th>
                        <th colspan="<%=maxSemester%>" class="bg-light"><%=Common.getBahasaConfig("Matakuliah per Semester")%></th>
                    </tr>
                    <tr>
                        <% for(int s=1; s<=maxSemester; s++) { %><th class="fw-bold text-success bg-white" style="min-width: 130px;"><%=Common.getBahasaConfig("Smt")%> <%=s%></th><% } %>
                    </tr>
                </thead>
                <tbody>
                    <% for(CapaianLulusan cpl : listCPL) { 
                        List<CapaianPembelajaranLulusan> subCpmk = mapCPMK.get(cpl.getId());
                        int rowSpan = Math.max(1, subCpmk.size());
                        boolean firstCpl = true;
                    %>
                        <% if(subCpmk.isEmpty()) { %>
                            <tr>
                                <td class="fw-bold bg-light text-primary align-middle">
                                    <%=cpl.getKode()%><br>
                                    <% if(edit) { %><button class="btn btn-outline-primary btn-sm mt-2 shadow-sm rounded-pill" onclick="window.bukaModalKelolaCPMK9<%=rnd%>('<%=cpl.getId()%>', '<%=cpl.getKode()%>', '')"><i class="fas fa-link me-1"></i> <%=Common.getBahasaConfig("Tautkan CPMK")%></button><% } %>
                                </td>
                                <td colspan="<%=maxSemester + 2%>" class="text-muted fst-italic text-start ps-3 bg-white"><%=Common.getBahasaConfig("Belum ada CPMK yang dikaitkan ke CPL ini.")%></td>
                            </tr>
                        <% } else { 
                            for(CapaianPembelajaranLulusan cp : subCpmk) {
                        %>
                            <tr>
                                <% if(firstCpl) { %>
                                    <td rowspan="<%=rowSpan%>" class="fw-bold bg-light align-top pt-3 text-primary">
                                        <span class="d-block mb-2"><%=cpl.getKode()%></span>
                                        <% if(edit) { %><button class="btn btn-primary btn-sm shadow-sm rounded-pill" onclick="window.bukaModalKelolaCPMK9<%=rnd%>('<%=cpl.getId()%>', '<%=cpl.getKode()%>', '')"><i class="fas fa-cog me-1"></i> <%=Common.getBahasaConfig("Kelola CPMK")%></button><% } %>
                                    </td>
                                <% firstCpl = false; } %>
                                
                                <td class="fw-bold text-dark bg-white"><%=cp.getKode()%></td>
                                <td class="text-start text-muted bg-white"><%=cp.getNama()%></td>
                                
                                <% for(int s=1; s<=maxSemester; s++) { %>
                                    <td class="bg-white position-relative p-2" style="vertical-align: top;">
                                        <div class="d-flex flex-wrap gap-1 justify-content-center mb-4">
                                        <% 
                                            for(KurikulumPunyaMatakuliah kpm : listKpm) {
                                                if(kpm.getSemester() != null && kpm.getSemester() == s) {
                                                    Matakuliah mk = kpm.getMatakuliah();
                                                    String mkCpl = mk.getCapaianLulusan() != null ? mk.getCapaianLulusan() : "";
                                                    String mkCpmk = mk.getCapaianPembelajaranLulusan() != null ? mk.getCapaianPembelajaranLulusan() : "";
                                                    
                                                    if(mkCpl.contains("," + cpl.getId() + ",") && mkCpmk.contains("," + cp.getId() + ",")) {
                                                        String badgeColor = (kpm.getNilaiMenggunakanCpmk() != null && kpm.getNilaiMenggunakanCpmk()) ? "bg-success" : "bg-info text-dark";
                                        %>
                                                        <span class="badge <%=badgeColor%> rounded-pill border shadow-sm d-flex align-items-center" title="<%=mk.getNama()%>">
                                                            <%=mk.getKode()%>
                                                            <% if(edit) { %><i class="fas fa-times ms-2 text-white" style="cursor: pointer; opacity: 0.8;" onclick="window.hapusKorelasiMK_9<%=rnd%>('<%=mk.getId()%>', '<%=cpl.getId()%>|<%=cp.getId()%>')"></i><% } %>
                                                        </span>
                                        <%          }
                                                }
                                            }
                                        %>
                                        </div>
                                        <% if(edit) { %>
                                            <div class="position-absolute bottom-0 start-50 translate-middle-x mb-1">
                                                <button class="btn btn-outline-primary btn-sm rounded-circle shadow-sm" style="width: 25px; height: 25px; padding: 0;" title="<%=Common.getBahasaConfig("Tambahkan Matakuliah")%>" onclick="window.bukaModalPilihMK_9<%=rnd%>('<%=cpl.getId()%>', '<%=cp.getId()%>', '<%=cpl.getKode()%> - <%=cp.getKode()%>', '<%=s%>', '<%=kurikulumAktif.getId()%>')"><i class="fas fa-plus" style="font-size: 10px;"></i></button>
                                            </div>
                                        <% } %>
                                    </td>
                                <% } %>
                            </tr>
                        <% } } %>
                    <% } %>
                </tbody>
            </table>
        </div>
        <div class="mt-3 text-muted small">
            <span class="badge bg-success me-1">MK</span> <%=Common.getBahasaConfig("Matakuliah menggunakan basis nilai CPMK")%> &nbsp;|&nbsp; 
            <span class="badge bg-info text-dark me-1">MK</span> <%=Common.getBahasaConfig("Matakuliah menggunakan basis nilai Sub-CPMK")%>
        </div>
    <% } %>

<%
} 
// ==========================================
// ACTION 2: LOAD MODAL TAUTKAN CPMK KE CPL
// ==========================================
else if ("loadModalKelolaCpmk".equals(action)) {
    String idCpl = request.getParameter("idCpl");
    String kodeCpl = request.getParameter("kodeCpl");
    String idMk = request.getParameter("idMk"); // TAMBAHAN PENERIMAAN ID MK
    
    CapaianLulusan cpl = new CapaianLulusan();
    List<CapaianPembelajaranLulusan> allCPMK = new ArrayList<CapaianPembelajaranLulusan>();
    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        cpl = (CapaianLulusan) sess.get(CapaianLulusan.class, Long.parseLong(idCpl));
        allCPMK = ConstantValues.simpleList(sess.createCriteria(CapaianPembelajaranLulusan.class).add(Restrictions.eq("jurusan.id", idJurusan)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("kode")), CapaianPembelajaranLulusan.class);
    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_9.jsp:183"); } finally { if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); } }
    
    String cpmkMapped = cpl.getCapaianPembelajaranLulusan() != null ? cpl.getCapaianPembelajaranLulusan() : "";
%>
    <div class="modal fade" id="modalKelolaCpmk_9_<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
        <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content shadow-lg border-0 rounded-4">
                <div class="modal-header bg-primary text-white border-bottom-0 pb-3">
                    <h6 class="modal-title fw-bold"><i class="fas fa-link me-2"></i><%=Common.getBahasaConfig("Tautkan CPMK ke")%> <%=kodeCpl%></h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4 bg-light">
                    <div class="p-3 border rounded bg-white shadow-sm" style="max-height: 400px; overflow-y: auto;">
                        <% if(allCPMK.isEmpty()) { %>
                            <span class="text-muted small"><%=Common.getBahasaConfig("Tidak ada data master CPMK.")%></span>
                        <% } else { 
                            for(CapaianPembelajaranLulusan cpmk : allCPMK) {
                                boolean isChecked = cpmkMapped.contains("," + cpmk.getId() + ",");
                        %>
                            <div class="form-check mb-2">
                                <input class="form-check-input chk-cpmk-9 border-primary" type="checkbox" value="<%=cpmk.getId()%>" id="chkCpmk_9_<%=cpmk.getId()%>" <%=isChecked ? "checked" : ""%>>
                                <label class="form-check-label small" for="chkCpmk_9_<%=cpmk.getId()%>"><strong><%=cpmk.getKode()%></strong> - <%=cpmk.getNama()%></label>
                            </div>
                        <% } } %>
                    </div>
                </div>
                <div class="modal-footer bg-white border-top p-3 justify-content-end">
                    <button type="button" class="btn btn-light border fw-bold rounded-pill px-4 shadow-sm" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                    <!-- MENGIRIM idMk SEBAGAI PARAMETER -->
                    <button type="button" class="btn btn-primary fw-bold rounded-pill px-4 shadow-sm" onclick="window.simpanKorelasiCPL_CPMK9<%=rnd%>('<%=idCpl%>', '<%=idMk != null ? idMk : ""%>')"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Tautan")%></button>
                </div>
            </div>
        </div>
    </div>
    <script>
        var modalKelola9 = new bootstrap.Modal(document.getElementById('modalKelolaCpmk_9_<%=rnd%>')); modalKelola9.show();
        document.getElementById('modalKelolaCpmk_9_<%=rnd%>').addEventListener('hidden.bs.modal', function () { this.remove(); });
    </script>
<%
}
// ==========================================
// ACTION 3: LOAD MODAL MK UNTUK CPL-CPMK-SMT
// ==========================================
else if ("loadModalMK".equals(action)) {
    String idCpl = request.getParameter("idCpl");
    String idCpmk = request.getParameter("idCpmk");
    String labelRelasi = request.getParameter("labelRelasi");
    String semester = request.getParameter("semester");
    String idKurikulum = request.getParameter("idKurikulum");
    
    List<KurikulumPunyaMatakuliah> listKpm = new ArrayList<KurikulumPunyaMatakuliah>();
    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        listKpm = ConstantValues.simpleList(sess.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("matakuliah", "mk").add(Restrictions.eq("kurikulum.id", Long.parseLong(idKurikulum))).add(Restrictions.eq("semester", Integer.parseInt(semester))).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.or(Restrictions.isNull("mk.aktif"), Restrictions.eq("mk.aktif", true))).addOrder(Order.asc("mk.kode")), KurikulumPunyaMatakuliah.class);
    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_9.jsp:238"); } finally { if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); } }
%>
    <div class="modal fade" id="modalPilihMK_9_<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
        <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content shadow-lg border-0 rounded-4">
                <div class="modal-header bg-primary text-white border-bottom-0 pb-3">
                    <h6 class="modal-title fw-bold"><i class="fas fa-plus-circle me-2"></i><%=Common.getBahasaConfig("Pilih Matakuliah")%> (Smt <%=semester%>)</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4 bg-light">
                    <div class="alert alert-primary py-2 px-3 mb-3 border-0 small shadow-sm rounded-3">
                        <i class="fas fa-info-circle me-1"></i> <%=Common.getBahasaConfig("Memasukkan MK untuk irisan:")%> <strong><%=labelRelasi%></strong>
                    </div>
                    
                    <div class="p-3 border rounded bg-white shadow-sm" style="max-height: 350px; overflow-y: auto;">
                        <% if(listKpm.isEmpty()) { %>
                            <span class="text-muted small"><%=Common.getBahasaConfig("Tidak ada Matakuliah yang dijadwalkan pada semester ini dalam kurikulum aktif.")%></span>
                        <% } else { 
                            for(KurikulumPunyaMatakuliah kpm : listKpm) {
                                Matakuliah mk = kpm.getMatakuliah();
                                boolean isChecked = (mk.getCapaianLulusan() != null && mk.getCapaianLulusan().contains("," + idCpl + ",")) && (mk.getCapaianPembelajaranLulusan() != null && mk.getCapaianPembelajaranLulusan().contains("," + idCpmk + ","));
                        %>
                            <div class="form-check mb-2">
                                <input class="form-check-input chk-mk-9 border-success" type="checkbox" value="<%=mk.getId()%>" id="chkMk9_<%=mk.getId()%>" <%=isChecked ? "checked" : ""%>>
                                <label class="form-check-label small" for="chkMk9_<%=mk.getId()%>"><strong><%=mk.getKode()%></strong> - <%=mk.getNama()%></label>
                            </div>
                        <% } } %>
                    </div>
                </div>
                <div class="modal-footer bg-white border-top p-3 justify-content-end">
                    <button type="button" class="btn btn-light border fw-bold rounded-pill px-4 shadow-sm" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-primary fw-bold rounded-pill px-4 shadow-sm" onclick="window.simpanKorelasiMK_9<%=rnd%>('<%=idCpl%>', '<%=idCpmk%>')"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Terapkan Pilihan")%></button>
                </div>
            </div>
        </div>
    </div>
    <script>
        var modalMK9 = new bootstrap.Modal(document.getElementById('modalPilihMK_9_<%=rnd%>')); modalMK9.show();
        document.getElementById('modalPilihMK_9_<%=rnd%>').addEventListener('hidden.bs.modal', function () { this.remove(); });
    </script>
<%
}
%>