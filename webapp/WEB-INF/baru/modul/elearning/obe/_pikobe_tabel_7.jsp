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
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Kurikulum"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>

<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
String action = request.getParameter("action");
String idJurusanStr = request.getParameter("idJurusan");
String keyword = request.getParameter("keyword");
String idKurikulumStr = request.getParameter("idKurikulum");

Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) return;

boolean edit = false;
boolean delete = false;
if(tbmuser != null && tbmuser.getUserId() != null){
    edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
    if(Common.getApakahAdminLain(tbmuser)){ edit = true; delete = true; }
}

Long idJurusan = null;
try {
    if(idJurusanStr != null && !idJurusanStr.trim().isEmpty()){
        idJurusan = Long.parseLong(idJurusanStr);
    }
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_7.jsp:42");}

if(idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

// ==========================================
// ACTION 1: LOAD TABEL 7 (SUSUNAN MK)
// ==========================================
if ("loadTabel7".equals(action)) {
    List<KurikulumPunyaMatakuliah> listKpm = new ArrayList<KurikulumPunyaMatakuliah>();
    Kurikulum kurikulumAktif = null;
    int maxSemester = 8; // Default
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        
        // 1. Cari Kurikulum OBE Aktif untuk Jurusan Tersebut
        Criteria critKurikulum = sess.createCriteria(Kurikulum.class);
        critKurikulum.add(Restrictions.eq("jurusan.id", idJurusan));
        critKurikulum.add(Restrictions.eq("obe", true));
        critKurikulum.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        critKurikulum.addOrder(Order.desc("id")); // Ambil yang paling baru jika ada banyak
        
        List<Kurikulum> kList = critKurikulum.list();
        if(!kList.isEmpty()) {
            kurikulumAktif = kList.get(0);
            
            Jurusan jur = kurikulumAktif.getJurusan();
            if(jur != null && jur.getJenjang() != null && jur.getJenjang().getJumlahSemester() != null) {
                maxSemester = jur.getJenjang().getJumlahSemester();
            }

            // 2. Tarik Data KurikulumPunyaMatakuliah
            Criteria critKpm = sess.createCriteria(KurikulumPunyaMatakuliah.class);
            critKpm.createAlias("matakuliah", "mk");
            critKpm.add(Restrictions.eq("kurikulum.id", kurikulumAktif.getId()));
            critKpm.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            critKpm.add(Restrictions.or(Restrictions.isNull("mk.aktif"), Restrictions.eq("mk.aktif", true)));
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                Disjunction orMk = Restrictions.disjunction();
                orMk.add(Restrictions.ilike("mk.kode", keyword, MatchMode.ANYWHERE));
                orMk.add(Restrictions.ilike("mk.nama", keyword, MatchMode.ANYWHERE));
                critKpm.add(orMk);
            }
            
            critKpm.addOrder(Order.asc("semester")).addOrder(Order.asc("mk.kode"));
            listKpm = ConstantValues.simpleList(critKpm, KurikulumPunyaMatakuliah.class);
        }

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_7.jsp:96");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }
%>

    <% if(kurikulumAktif == null) { %>
        <div class="alert alert-warning text-center py-4 border-0 shadow-sm rounded-3">
            <i class="fas fa-exclamation-triangle fa-2x mb-2 d-block text-warning"></i>
            <%=Common.getBahasaConfig("Kurikulum OBE aktif untuk Program Studi ini tidak ditemukan. Silakan atur di Master Kurikulum.")%>
        </div>
    <% } else { %>
        <div class="d-flex justify-content-between align-items-center mb-3 bg-white p-3 rounded shadow-sm border">
            <div>
                <span class="text-muted small fw-bold d-block"><%=Common.getBahasaConfig("Kurikulum Aktif:")%></span>
                <span class="text-primary fw-bold" style="font-size: 1.1rem;"><%=kurikulumAktif.getNama()%></span>
            </div>
            <% if(edit) { %>
            <button class="btn btn-primary btn-sm rounded-pill px-3 fw-bold shadow-sm" onclick="window.bukaModalTambahMK7<%=rnd%>('<%=kurikulumAktif.getId()%>')">
                <i class="fas fa-plus me-1"></i> <%=Common.getBahasaConfig("Ambil Matakuliah Baru")%>
            </button>
            <% } %>
        </div>

        <% if(listKpm.isEmpty()) { %>
            <div class="alert alert-warning text-center py-4 border-0 shadow-sm rounded-3">
                <i class="fas fa-folder-open fa-2x mb-2 d-block text-warning"></i>
                <%=Common.getBahasaConfig("Belum ada Matakuliah yang dimasukkan ke dalam kurikulum ini.")%>
            </div>
        <% } else { %>
            <div class="table-responsive border rounded-3 animate__animated animate__fadeIn" style="max-height: 600px;">
                <table class="table table-bordered table-hover align-middle mb-0 text-center small">
                    <thead class="table-light sticky-top shadow-sm" style="z-index: 10;">
                        <tr>
                            <th rowspan="2" class="align-middle bg-light" style="width: 50px;"><%=Common.getBahasaConfig("No")%></th>
                            <th rowspan="2" class="align-middle bg-light text-start ps-3" style="min-width: 250px;"><%=Common.getBahasaConfig("Matakuliah")%></th>
                            <th colspan="<%=maxSemester%>" class="bg-light"><%=Common.getBahasaConfig("Semester")%></th>
                            <% if(delete) { %><th rowspan="2" class="align-middle bg-light" style="width: 60px;"><%=Common.getBahasaConfig("Aksi")%></th><% } %>
                        </tr>
                        <tr>
                            <% for(int s=1; s<=maxSemester; s++) { %>
                                <th class="fw-bold text-success bg-white" style="width: 40px;"><%=s%></th>
                            <% } %>
                        </tr>
                    </thead>
                    <tbody>
                        <% 
                        int no = 1;
                        for(KurikulumPunyaMatakuliah kpm : listKpm) { 
                            Matakuliah mk = kpm.getMatakuliah();
                            Integer smtMk = kpm.getSemester() != null ? kpm.getSemester() : 0;
                        %>
                            <tr>
                                <td class="text-muted fw-bold bg-white"><%=no++%></td>
                                <td class="text-start ps-3 text-dark bg-white">
                                    <span class="badge bg-secondary me-2"><%=mk.getKode()%></span>
                                    <%=mk.getNama()%>
                                </td>
                                <% for(int s=1; s<=maxSemester; s++) { %>
                                    <td class="bg-white">
                                        <input type="radio" name="radioSmt_<%=kpm.getId()%>" class="form-check-input border-secondary shadow-sm" style="cursor: pointer;"
                                               value="<%=s%>"
                                               <%=smtMk == s ? "checked" : ""%> 
                                               <%=!edit ? "disabled" : ""%>
                                               onchange="window.updateSemesterKPM<%=rnd%>(this, '<%=kpm.getId()%>')">
                                    </td>
                                <% } %>
                                <% if(delete) { %>
                                <td class="bg-white">
                                    <button class="btn btn-outline-danger btn-sm rounded-circle p-1" style="width: 28px; height: 28px;" title="<%=Common.getBahasaConfig("Keluarkan Matakuliah dari Kurikulum")%>" onclick="window.hapusKPM7<%=rnd%>('<%=kpm.getId()%>')">
                                        <i class="fas fa-trash" style="font-size: 11px;"></i>
                                    </button>
                                </td>
                                <% } %>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>
            <div class="mt-3 text-muted small"><i class="fas fa-info-circle me-1 text-primary"></i><%=Common.getBahasaConfig("Pilih radio button untuk menempatkan matakuliah pada semester yang sesuai. Perubahan akan disimpan secara otomatis.")%></div>
        <% } %>

<%
    }
} 
// ==========================================
// ACTION 2: LOAD MODAL DAFTAR MATAKULIAH BARU
// ==========================================
else if ("loadModalTambahMK".equals(action)) {
    List<Matakuliah> listMK = new ArrayList<Matakuliah>();
    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        // Cari semua matakuliah yang belum ada di kurikulum ini
        List<Long> idMkTerpakai = sess.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("matakuliah", "mk").setProjection(Projections.distinct(Projections.property("mk.id"))).add(Restrictions.eq("kurikulum.id", Long.parseLong(idKurikulumStr))).list();
        
        Criteria critMk = sess.createCriteria(Matakuliah.class);
        if(!idMkTerpakai.isEmpty()) { critMk.add(Restrictions.not(Restrictions.in("id", idMkTerpakai))); }
        critMk.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        critMk.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
        
        listMK = ConstantValues.simpleList(critMk, Matakuliah.class);
    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_7.jsp:198"); } finally { if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); } }
%>
    <div class="modal fade" id="modalTambahMK_7_<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
        <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content shadow-lg border-0 rounded-4">
                <div class="modal-header bg-primary text-white border-bottom-0 pb-3">
                    <h6 class="modal-title fw-bold"><i class="fas fa-plus-circle me-2"></i><%=Common.getBahasaConfig("Ambil Matakuliah ke Kurikulum")%></h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4 bg-light">
                    <div class="input-group input-group-sm mb-3 shadow-sm">
                        <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                        <input type="text" id="inpCariMkBaru_7_<%=rnd%>" class="form-control border-start-0 ps-0" placeholder="<%=Common.getBahasaConfig("Cari matakuliah yang tersedia...")%>" onkeyup="window.filterMKBaru_7<%=rnd%>()">
                    </div>

                    <div class="p-3 border rounded bg-white shadow-sm" id="wrapListMKBaru_7_<%=rnd%>" style="max-height: 350px; overflow-y: auto;">
                        <% if(listMK.isEmpty()) { %>
                            <span class="text-muted small fst-italic"><%=Common.getBahasaConfig("Semua matakuliah aktif telah dimasukkan ke dalam kurikulum ini.")%></span>
                        <% } else { 
                            for(Matakuliah mk : listMK) {
                                String searchText = (mk.getKode() + " " + mk.getNama()).toLowerCase().replace("\"", "");
                        %>
                            <div class="form-check mb-2 item-mkbaru-7" data-search="<%=searchText%>">
                                <input class="form-check-input chk-mkbaru-7 border-success" type="checkbox" value="<%=mk.getId()%>" id="chkMkb_7_<%=mk.getId()%>">
                                <label class="form-check-label small text-dark" for="chkMkb_7_<%=mk.getId()%>"><strong><%=mk.getKode()%></strong> - <%=mk.getNama()%></label>
                            </div>
                        <%  } 
                        } %>
                    </div>
                </div>
                <div class="modal-footer bg-white border-top p-3 justify-content-end">
                    <button type="button" class="btn btn-light border fw-bold rounded-pill px-4 shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times me-2 text-danger"></i><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-primary fw-bold rounded-pill px-4 shadow-sm" onclick="window.simpanMatakuliahBaru_7<%=rnd%>('<%=idKurikulumStr%>')"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Masukkan ke Kurikulum")%></button>
                </div>
            </div>
        </div>
    </div>
    <script>
        var modalAddMK7 = new bootstrap.Modal(document.getElementById('modalTambahMK_7_<%=rnd%>'));
        modalAddMK7.show();

        window.filterMKBaru_7<%=rnd%> = function() {
            var input = document.getElementById('inpCariMkBaru_7_<%=rnd%>');
            var filter = input.value.toLowerCase();
            var nodes = document.querySelectorAll('#wrapListMKBaru_7_<%=rnd%> .item-mkbaru-7');
            nodes.forEach(function(node) {
                var txt = node.getAttribute('data-search') || "";
                if (txt.indexOf(filter) > -1) { node.style.display = ""; } else { node.style.display = "none"; }
            });
        };

        document.getElementById('modalTambahMK_7_<%=rnd%>').addEventListener('hidden.bs.modal', function () { this.remove(); });
    </script>
<%
}
%>