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

<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
String action = request.getParameter("action");
String idJurusanStr = request.getParameter("idJurusan");
String keyword = request.getParameter("keyword");
String idDataStr = request.getParameter("id");

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
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_1.jsp:43");}

if(idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

// ==========================================
// ACTION 1: LOAD TABEL 1 (PROFIL LULUSAN)
// ==========================================
if ("loadTabel".equals(action)) {
    List<ProfilLulusan> listProfil = new ArrayList<ProfilLulusan>();
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        Criteria crit = sess.createCriteria(ProfilLulusan.class);
        crit.add(Restrictions.eq("jurusan.id", idJurusan));
        
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
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_1.jsp:74");
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
                        if(pl.getProfesiLulusan() != null) {
                            namaProfesi = pl.getProfesiLulusan().getNama() != null ? pl.getProfesiLulusan().getNama() : "-";
                        }
                %>
                    <tr>
                        <td class="text-center text-muted fw-bold"><%=no++%></td>
                        <td class="fw-bold text-dark"><span class="badge bg-primary px-3 py-2 shadow-sm"><%=kodePL%></span></td>
                        <td class="text-dark fw-medium"><%=namaPL%></td>
                        <td class="text-secondary"><%=namaProfesi.replaceAll("\n", "<br>")%></td>
                        
                        <% if(edit || delete) { %>
                        <td class="text-center text-nowrap" data-aksi-baris>
                            <% if(edit) { %>
                            <button type="button" class="btn btn-sm btn-outline-primary rounded-circle shadow-sm me-1" 
                                    onclick="window.bukaModalPL<%=rnd%>('<%=pl.getId()%>')" 
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
// ACTION 2: LOAD FORM MODAL (TAMBAH/EDIT)
// ==========================================
else if ("loadForm".equals(action)) {
    ProfilLulusan pl = new ProfilLulusan();
    List<ProfesiLulusan> listProfesi = new ArrayList<ProfesiLulusan>();
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        if(idDataStr != null && !idDataStr.trim().isEmpty()) {
            pl = (ProfilLulusan) sess.get(ProfilLulusan.class, Long.parseLong(idDataStr));
        }
        if(pl == null) pl = new ProfilLulusan();
        
        listProfesi = ConstantValues.simpleList(sess.createCriteria(ProfesiLulusan.class).add(Restrictions.eq("jurusan.id", idJurusan)).addOrder(Order.asc("nama")), ProfesiLulusan.class);
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_1.jsp:159");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }

    String judulForm = (pl.getId() == null) ? Common.getBahasaConfig("Tambah Profil Lulusan Baru") : Common.getBahasaConfig("Ubah Data Profil Lulusan");
    String idProfesiTerpilih = (pl.getProfesiLulusan() != null) ? pl.getProfesiLulusan().getId().toString() : "";
%>
    <div class="modal fade" id="modalPL_<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content shadow-lg border-0 rounded-4">
                <div class="modal-header bg-primary text-white border-bottom-0 pb-3">
                    <h6 class="modal-title fw-bold"><i class="fas fa-user-plus me-2"></i><%=judulForm%></h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4 bg-light">
                    <input type="hidden" id="inpIdPL_<%=rnd%>" value="<%=pl.getId() != null ? pl.getId() : ""%>">
                    
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Kode Profil Lulusan")%> *</label>
                        <input type="text" class="form-control border-primary" id="inpKodePL_<%=rnd%>" value="<%=pl.getKode() != null ? pl.getKode().replace("\"", "&quot;") : ""%>" placeholder="<%=Common.getBahasaConfig("Contoh: PL01")%>">
                    </div>
                    
                    <div class="mb-3">
                        <label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Deskripsi Profil Lulusan")%> *</label>
                        <textarea class="form-control border-primary" id="inpNamaPL_<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Contoh: Lulusan memiliki kemampuan...")%>"><%=pl.getNama() != null ? pl.getNama() : ""%></textarea>
                    </div>

                    <div class="mb-3">
                        <label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Profesi Terkait")%></label>
                        <select class="form-select border-primary" id="inpProfesiPL_<%=rnd%>">
                            <option value="">-- <%=Common.getBahasaConfig("Pilih Profesi Lulusan")%> --</option>
                            <% for(ProfesiLulusan prf : listProfesi) { %>
                                <option value="<%=prf.getId()%>" <%=prf.getId().toString().equals(idProfesiTerpilih) ? "selected" : ""%>><%=prf.getKode()%> - <%=prf.getNama()%></option>
                            <% } %>
                        </select>
                        <small class="text-muted mt-2 d-block"><i class="fas fa-info-circle me-1 text-info"></i><%=Common.getBahasaConfig("Jika profesi belum tersedia, tambahkan terlebih dahulu melalui tab Profesi Lulusan.")%></small>
                    </div>

                </div>
                <div class="modal-footer bg-white border-top p-3 justify-content-end">
                    <button type="button" class="btn btn-light border fw-bold rounded-pill px-4 shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times me-2 text-danger"></i><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-primary fw-bold rounded-pill px-4 shadow-sm" onclick="window.simpanPL<%=rnd%>()"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%></button>
                </div>
            </div>
        </div>
    </div>

    <script>
        var myModalPl = new bootstrap.Modal(document.getElementById('modalPL_<%=rnd%>'));
        myModalPl.show();

        window.simpanPL<%=rnd%> = async function() {
            var idPL = document.getElementById('inpIdPL_<%=rnd%>') ? document.getElementById('inpIdPL_<%=rnd%>').value : "";
            var profVal = document.getElementById('inpProfesiPL_<%=rnd%>') ? document.getElementById('inpProfesiPL_<%=rnd%>').value : null;

            var dataObj = {
                kode: document.getElementById('inpKodePL_<%=rnd%>') ? document.getElementById('inpKodePL_<%=rnd%>').value.trim() : null,
                nama: document.getElementById('inpNamaPL_<%=rnd%>') ? document.getElementById('inpNamaPL_<%=rnd%>').value.trim() : null,
                profesiLulusan: (profVal && profVal !== "") ? profVal : null,
                jurusan: document.getElementById('filterJurusan_<%=rnd%>') ? document.getElementById('filterJurusan_<%=rnd%>').value : null
            };

            if(!dataObj.kode || !dataObj.nama) {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Kode dan Deskripsi Profil Lulusan wajib diisi.")%>', 'bg-warning text-dark');
                return;
            }

            var payload = { action: "simpanDataRinci", log: "true", class: "ais.database.model.obe.ProfilLulusan", data: dataObj, tanpaLogin: "true" };
            if(idPL !== "") payload.id = idPL;

            try {
                var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
                var result = await res.json();
                
                if (result.status === '00' || result.status === 'success') {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Data Profil Lulusan berhasil disimpan.")%>', 'bg-success text-white');
                    myModalPl.hide();
                    setTimeout(function(){ window.loadTabel1<%=rnd%>(); window.loadTabel3<%=rnd%>(); }, 350);
                } else {
                    if(typeof tampilkanToast === 'function') tampilkanToast(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data.")%>', 'bg-danger text-white');
                }
            } catch (e) {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
            }
        };

        document.getElementById('modalPL_<%=rnd%>').addEventListener('hidden.bs.modal', function () { this.remove(); });
    </script>
<%
}
%>