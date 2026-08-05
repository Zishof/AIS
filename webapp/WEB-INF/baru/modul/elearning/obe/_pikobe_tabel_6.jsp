<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.obe.CapaianLulusan"%>
<%@page import="ais.database.model.obe.BahanKajian"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Kurikulum"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>

<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
String action = request.getParameter("action");
String idJurusanStr = request.getParameter("idJurusan");

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
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_6.jsp:37");}

if(idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

// ==========================================
// ACTION 1: LOAD MATRIKS TABEL 6 (CPL vs BK vs MK)
// ==========================================
if ("loadTabel6".equals(action)) {
    List<CapaianLulusan> listCPL = new ArrayList<CapaianLulusan>();
    List<BahanKajian> listBK = new ArrayList<BahanKajian>();
    List<Matakuliah> listMK = new ArrayList<Matakuliah>();
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        
        // 1. Tarik Data Kolom (CPL)
        listCPL = ConstantValues.simpleList(sess.createCriteria(CapaianLulusan.class)
            .add(Restrictions.eq("jurusan.id", idJurusan))
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
            .addOrder(Order.asc("kode")).addOrder(Order.asc("nama")), CapaianLulusan.class);
            
        // 2. Tarik Data Baris (BK)
        listBK = ConstantValues.simpleList(sess.createCriteria(BahanKajian.class)
            .add(Restrictions.eq("jurusan.id", idJurusan))
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
            .addOrder(Order.asc("kode")).addOrder(Order.asc("nama")), BahanKajian.class);

        // 3. Tarik Data MK OBE Aktif
        List<Long> idKurikulums = sess.createCriteria(Kurikulum.class).setProjection(Projections.property("id")).add(Restrictions.eq("jurusan.id", idJurusan)).add(Restrictions.eq("obe", true)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
        if(!idKurikulums.isEmpty()) {
            List<Long> idMks = sess.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("kurikulum", "k").createAlias("matakuliah", "mk").setProjection(Projections.distinct(Projections.property("mk.id"))).add(Restrictions.in("k.id", idKurikulums)).list();
            if(!idMks.isEmpty()) {
                listMK = ConstantValues.simpleList(sess.createCriteria(Matakuliah.class).add(Restrictions.in("id", idMks)).addOrder(Order.asc("kode")).addOrder(Order.asc("nama")), Matakuliah.class);
            }
        }

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_6.jsp:78");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }
%>

    <% if(listBK.isEmpty()) { %>
        <div class="alert alert-warning text-center py-4 border-0 shadow-sm rounded-3"><i class="fas fa-exclamation-triangle fa-2x mb-2 d-block text-warning"></i><%=Common.getBahasaConfig("Data Bahan Kajian (Baris) tidak ditemukan.")%></div>
    <% } else if(listCPL.isEmpty()) { %>
        <div class="alert alert-warning text-center py-4 border-0 shadow-sm rounded-3"><i class="fas fa-exclamation-triangle fa-2x mb-2 d-block text-warning"></i><%=Common.getBahasaConfig("Data CPL (Kolom) tidak ditemukan.")%></div>
    <% } else { %>
        <div class="table-responsive border rounded-3 animate__animated animate__fadeIn" style="max-height: 700px; overflow-y: auto;">
            <table class="table table-bordered align-middle mb-0 text-center small">
                <thead class="table-light sticky-top shadow-sm" style="z-index: 10;">
                    <tr>
                        <th rowspan="2" class="align-middle bg-light text-start ps-3" style="min-width: 250px;"><%=Common.getBahasaConfig("Bahan Kajian (BK)")%></th>
                        <th colspan="<%=listCPL.size()%>" class="bg-light"><%=Common.getBahasaConfig("Capaian Pembelajaran Lulusan (CPL)")%></th>
                    </tr>
                    <tr>
                        <% for(CapaianLulusan cpl : listCPL) { %>
                            <th class="fw-bold text-primary bg-white" title="<%=cpl.getNama()%>" style="min-width: 150px;"><%=cpl.getKode()%></th>
                        <% } %>
                    </tr>
                </thead>
                <tbody>
                    <% 
                    int no = 1;
                    for(BahanKajian bk : listBK) { 
                    %>
                        <tr>
                            <td class="text-start ps-3 text-dark bg-white">
                                <span class="badge bg-secondary me-2"><%=bk.getKode()%></span><br>
                                <span class="text-muted"><%=bk.getNama()%></span>
                            </td>
                            <% for(CapaianLulusan cpl : listCPL) { 
                                String kodeKorelasi = cpl.getId() + "|" + bk.getId();
                            %>
                                <td class="bg-white position-relative p-2" style="vertical-align: top;">
                                    <div class="d-flex flex-wrap gap-1 justify-content-center mb-4">
                                    <% 
                                        for(Matakuliah mk : listMK) {
                                            String mkCpl = mk.getCapaianLulusan() != null ? mk.getCapaianLulusan() : "";
                                            String mkBk = mk.getBahanKajian() != null ? mk.getBahanKajian() : "";
                                            
                                            // Format pemetaan ZK: ",ID_CPL|ID_BK,"
                                            if(mkCpl.contains("," + kodeKorelasi + ",") && mkBk.contains("," + kodeKorelasi + ",")) {
                                    %>
                                                <span class="badge bg-success rounded-pill border shadow-sm" style="font-weight: 500;">
                                                    <%=mk.getKode()%>
                                                    <% if(edit) { %>
                                                        <i class="fas fa-times ms-1 text-white" style="cursor: pointer; opacity: 0.8;" onclick="window.hapusKorelasiMK_6<%=rnd%>('<%=mk.getId()%>', '<%=kodeKorelasi%>')" title="<%=Common.getBahasaConfig("Hapus Matakuliah Ini")%>"></i>
                                                    <% } %>
                                                </span>
                                    <%      } 
                                        } 
                                    %>
                                    </div>
                                    
                                    <% if(edit) { %>
                                    <div class="position-absolute bottom-0 start-50 translate-middle-x mb-2">
                                        <button class="btn btn-outline-primary btn-sm rounded-circle shadow-sm" style="width: 25px; height: 25px; padding: 0;" title="<%=Common.getBahasaConfig("Tambah Matakuliah")%>" onclick="window.bukaModalPilihMK_6<%=rnd%>('<%=cpl.getKode()%> vs <%=bk.getKode()%>', '<%=kodeKorelasi%>')">
                                            <i class="fas fa-plus" style="font-size: 10px;"></i>
                                        </button>
                                    </div>
                                    <% } %>
                                </td>
                            <% } %>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
        <div class="mt-3 text-muted small"><i class="fas fa-info-circle me-1 text-primary"></i><%=Common.getBahasaConfig("Matriks ini mengaitkan Matakuliah ke dalam irisan antara CPL dan Bahan Kajian. Klik tombol plus (+) untuk menambahkan Matakuliah pada irisan terkait.")%></div>
    <% } %>

<%
} 
// ==========================================
// ACTION 2: LOAD MODAL DAFTAR MK (UNTUK DIPILIH)
// ==========================================
else if ("loadModalMK".equals(action)) {
    String kodeKorelasi = request.getParameter("kodeKorelasi");
    String titleRelasi = request.getParameter("titleRelasi");
    
    List<Matakuliah> listMK = new ArrayList<Matakuliah>();
    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        List<Long> idKurikulums = sess.createCriteria(Kurikulum.class).setProjection(Projections.property("id")).add(Restrictions.eq("jurusan.id", idJurusan)).add(Restrictions.eq("obe", true)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
        if(!idKurikulums.isEmpty()) {
            List<Long> idMks = sess.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("kurikulum", "k").createAlias("matakuliah", "mk").setProjection(Projections.distinct(Projections.property("mk.id"))).add(Restrictions.in("k.id", idKurikulums)).list();
            if(!idMks.isEmpty()) {
                listMK = ConstantValues.simpleList(sess.createCriteria(Matakuliah.class).add(Restrictions.in("id", idMks)).addOrder(Order.asc("kode")).addOrder(Order.asc("nama")), Matakuliah.class);
            }
        }
    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_6.jsp:173"); } finally { if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); } }
%>
    <div class="modal fade" id="modalPilihMK_6_<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
        <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content shadow-lg border-0 rounded-4">
                <div class="modal-header bg-primary text-white border-bottom-0 pb-3">
                    <h6 class="modal-title fw-bold"><i class="fas fa-plus-circle me-2"></i><%=Common.getBahasaConfig("Pilih Matakuliah")%></h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4 bg-light">
                    <div class="alert alert-primary py-2 px-3 mb-3 border-0 small shadow-sm rounded-3">
                        <i class="fas fa-link me-1"></i> <%=Common.getBahasaConfig("Irisan Relasi:")%> <strong><%=titleRelasi%></strong>
                    </div>
                    
                    <div class="input-group input-group-sm mb-3 shadow-sm">
                        <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                        <input type="text" id="inpCariMk_6_<%=rnd%>" class="form-control border-start-0 ps-0" placeholder="<%=Common.getBahasaConfig("Cari matakuliah...")%>" onkeyup="window.filterDaftarMK_6<%=rnd%>()">
                    </div>

                    <div class="p-3 border rounded bg-white shadow-sm" id="wrapListMK_6_<%=rnd%>" style="max-height: 350px; overflow-y: auto;">
                        <% if(listMK.isEmpty()) { %>
                            <span class="text-muted small fst-italic"><%=Common.getBahasaConfig("Data Matakuliah OBE kosong.")%></span>
                        <% } else { 
                            for(Matakuliah mk : listMK) {
                                String mkCpl = mk.getCapaianLulusan() != null ? mk.getCapaianLulusan() : "";
                                String mkBk = mk.getBahanKajian() != null ? mk.getBahanKajian() : "";
                                boolean isChecked = mkCpl.contains("," + kodeKorelasi + ",") && mkBk.contains("," + kodeKorelasi + ",");
                                String searchText = (mk.getKode() + " " + mk.getNama()).toLowerCase().replace("\"", "");
                        %>
                            <div class="form-check mb-2 item-mk-6" data-search="<%=searchText%>">
                                <input class="form-check-input chk-mk-6 border-success" type="checkbox" value="<%=mk.getId()%>" id="chkMk_6_<%=mk.getId()%>" <%=isChecked ? "checked" : ""%>>
                                <label class="form-check-label small text-dark" for="chkMk_6_<%=mk.getId()%>"><strong><%=mk.getKode()%></strong> - <%=mk.getNama()%></label>
                            </div>
                        <%  } 
                        } %>
                    </div>
                </div>
                <div class="modal-footer bg-white border-top p-3 justify-content-end">
                    <button type="button" class="btn btn-light border fw-bold rounded-pill px-4 shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times me-2 text-danger"></i><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-primary fw-bold rounded-pill px-4 shadow-sm" onclick="window.simpanKorelasiMK_6<%=rnd%>('<%=kodeKorelasi%>')"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Terapkan Pilihan")%></button>
                </div>
            </div>
        </div>
    </div>
    <script>
        var modalMK6 = new bootstrap.Modal(document.getElementById('modalPilihMK_6_<%=rnd%>'));
        modalMK6.show();

        window.filterDaftarMK_6<%=rnd%> = function() {
            var input = document.getElementById('inpCariMk_6_<%=rnd%>');
            var filter = input.value.toLowerCase();
            var nodes = document.querySelectorAll('#wrapListMK_6_<%=rnd%> .item-mk-6');
            nodes.forEach(function(node) {
                var txt = node.getAttribute('data-search') || "";
                if (txt.indexOf(filter) > -1) { node.style.display = ""; } else { node.style.display = "none"; }
            });
        };

        document.getElementById('modalPilihMK_6_<%=rnd%>').addEventListener('hidden.bs.modal', function () { this.remove(); });
    </script>
<%
}
%>