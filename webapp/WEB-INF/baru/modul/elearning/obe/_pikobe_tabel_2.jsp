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
<%@page import="ais.database.model.obe.BahanKajian"%>
<%@page import="ais.database.model.GeneralValueObject"%>

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
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_2.jsp:45");}

if(idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

// ==========================================
// ACTION 1: LOAD TABEL 2 (CAPAIAN LULUSAN)
// ==========================================
if ("loadTabel".equals(action)) {
    List<CapaianLulusan> listCpl = new ArrayList<CapaianLulusan>();
    Map<String, String> mapProfil = new HashMap<String, String>();
    Map<String, String> mapBahan = new HashMap<String, String>();
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        
        List<ProfilLulusan> allProfil = ConstantValues.simpleList(sess.createCriteria(ProfilLulusan.class).add(Restrictions.eq("jurusan.id", idJurusan)), ProfilLulusan.class);
        for(ProfilLulusan p : allProfil) { mapProfil.put(p.getId().toString(), p.getKode() + " " + p.getNama()); }
        
        List<BahanKajian> allBahan = ConstantValues.simpleList(sess.createCriteria(BahanKajian.class).add(Restrictions.eq("jurusan.id", idJurusan)), BahanKajian.class);
        for(BahanKajian b : allBahan) { mapBahan.put(b.getId().toString(), b.getKode() + " " + b.getNama()); }

        Criteria crit = sess.createCriteria(CapaianLulusan.class);
        crit.add(Restrictions.eq("jurusan.id", idJurusan));
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            Disjunction or = Restrictions.disjunction();
            or.add(Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("nama", keyword, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE));
            crit.add(or);
        }
        
        crit.addOrder(Order.asc("kode"));
        listCpl = ConstantValues.simpleList(crit, CapaianLulusan.class);
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_2.jsp:85");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }
%>
    <div class="table-responsive border rounded-3 animate__animated animate__fadeIn">
        <table class="table table-hover align-middle mb-0 text-start small">
            <thead class="table-light border-bottom">
                <tr>
                    <th class="text-center" style="width: 50px;"><%=Common.getBahasaConfig("No")%></th>
                    <th style="width: 15%;"><%=Common.getBahasaConfig("Kode CPL")%></th>
                    <th style="width: 30%;"><%=Common.getBahasaConfig("Deskripsi CPL")%></th>
                    <th style="width: 25%;"><%=Common.getBahasaConfig("Profil Lulusan Terkait")%></th>
                    <th style="width: 20%;"><%=Common.getBahasaConfig("Keterangan")%></th>
                    <% if(edit || delete) { %> <th class="text-center" style="width: 100px;"><%=Common.getBahasaConfig("Aksi")%></th><% } %>
                </tr>
            </thead>
            <tbody>
                <% if(listCpl.isEmpty()) { %>
                    <tr>
                        <td colspan="<%= (edit||delete) ? 6 : 5 %>" class="text-center py-5 text-muted fst-italic">
                            <i class="fas fa-folder-open d-block fa-3x mb-3 text-secondary opacity-50"></i>
                            <span class="fw-bold"><%=Common.getBahasaConfig("Data Kosong")%></span><br>
                            <%=Common.getBahasaConfig("Belum ada data Capaian Lulusan (CPL) yang direkam.")%>
                        </td>
                    </tr>
                <% } else { 
                    int no = 1;
                    for(CapaianLulusan cpl : listCpl) {
                        String kodeCPL = cpl.getKode() != null ? cpl.getKode() : "-";
                        String namaCPL = cpl.getNama() != null ? cpl.getNama() : "-";
                        String ket = cpl.getKeterangan() != null ? cpl.getKeterangan() : "-";
                        
                        StringBuilder profilStr = new StringBuilder();
                        if(cpl.getProfil() != null && !cpl.getProfil().isEmpty()) {
                            for(String id : cpl.getProfil().split(",")) {
                                if(!id.trim().isEmpty() && mapProfil.containsKey(id.trim())) {
                                    profilStr.append("<span class='badge bg-info text-dark mb-1 me-1 text-wrap text-start shadow-sm'>").append(mapProfil.get(id.trim())).append("</span><br>");
                                }
                            }
                        }
                %>
                    <tr>
                        <td class="text-center text-muted fw-bold"><%=no++%></td>
                        <td class="fw-bold text-dark"><span class="badge bg-primary px-3 py-2 shadow-sm"><%=kodeCPL%></span></td>
                        <td class="text-dark fw-medium"><%=namaCPL%></td>
                        <td><%=profilStr.length() > 0 ? profilStr.toString() : "-"%></td>
                        <td class="text-secondary"><%=ket%></td>
                        
                        <% if(edit || delete) { %>
                        <td class="text-center text-nowrap" data-aksi-baris>
                            <% if(edit) { %>
                            <button type="button" class="btn btn-sm btn-outline-primary rounded-circle shadow-sm me-1" 
                                    onclick="window.bukaModalCPL<%=rnd%>('<%=cpl.getId()%>')" 
                                    title="<%=Common.getBahasaConfig("Ubah Data")%>">
                                <i class="fas fa-edit"></i>
                            </button>
                            <% } %>
                            <% if(delete) { %>
                            <button type="button" class="btn btn-sm btn-outline-danger rounded-circle shadow-sm" 
                                    onclick="window.hapusCPL<%=rnd%>('<%=cpl.getId()%>')" 
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
    CapaianLulusan cpl = new CapaianLulusan();
    List<ProfilLulusan> listProfil = new ArrayList<ProfilLulusan>();
    List<BahanKajian> listBahan = new ArrayList<BahanKajian>();
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        if(idDataStr != null && !idDataStr.trim().isEmpty()) {
            cpl = (CapaianLulusan) sess.get(CapaianLulusan.class, Long.parseLong(idDataStr));
        }
        if(cpl == null) cpl = new CapaianLulusan();
        
        listProfil = ConstantValues.simpleList(sess.createCriteria(ProfilLulusan.class).add(Restrictions.eq("jurusan.id", idJurusan)).addOrder(Order.asc("kode")).addOrder(Order.asc("nama")), ProfilLulusan.class);
        listBahan = ConstantValues.simpleList(sess.createCriteria(BahanKajian.class).add(Restrictions.eq("jurusan.id", idJurusan)).addOrder(Order.asc("kode")).addOrder(Order.asc("nama")), BahanKajian.class);
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_2.jsp:179");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }

    String judulForm = (cpl.getId() == null) ? Common.getBahasaConfig("Tambah Capaian Lulusan Baru") : Common.getBahasaConfig("Ubah Capaian Lulusan");
    String profilTerpilih = cpl.getProfil() != null ? cpl.getProfil() : "";
    String bahanTerpilih = cpl.getBahanKajian() != null ? cpl.getBahanKajian() : "";
%>
    <div class="modal fade" id="modalCPL_<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
        <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content shadow-lg border-0 rounded-4">
                <div class="modal-header bg-primary text-white border-bottom-0 pb-3">
                    <h6 class="modal-title fw-bold"><i class="fas fa-graduation-cap me-2"></i><%=judulForm%></h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4 bg-light">
                    <input type="hidden" id="inpIdCPL_<%=rnd%>" value="<%=cpl.getId() != null ? cpl.getId() : ""%>">
                    
                    <div class="row g-4">
                        <div class="col-md-5">
                            <div class="card border-0 shadow-sm rounded-4 h-100">
                                <div class="card-body">
                                    <h6 class="fw-bold text-secondary mb-3 border-bottom pb-2"><%=Common.getBahasaConfig("Informasi Dasar CPL")%></h6>
                                    <div class="mb-3">
                                        <label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Kode Capaian Lulusan (CPL)")%> *</label>
                                        <input type="text" class="form-control border-primary" id="inpKodeCPL_<%=rnd%>" value="<%=cpl.getKode() != null ? cpl.getKode().replace("\"", "&quot;") : ""%>">
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Deskripsi / Nama Capaian Lulusan")%> *</label>
                                        <textarea class="form-control border-primary" id="inpNamaCPL_<%=rnd%>" rows="4"><%=cpl.getNama() != null ? cpl.getNama() : ""%></textarea>
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                                        <textarea class="form-control border-primary" id="inpKetCPL_<%=rnd%>" rows="3"><%=cpl.getKeterangan() != null ? cpl.getKeterangan() : ""%></textarea>
                                    </div>
                                    <div class="form-check form-switch mt-4">
                                        <input class="form-check-input" type="checkbox" id="inpAktifCPL_<%=rnd%>" <%=cpl.getAktif() == null || cpl.getAktif() ? "checked" : ""%>>
                                        <label class="form-check-label fw-bold small text-dark" for="inpAktifCPL_<%=rnd%>"><%=Common.getBahasaConfig("Status Aktif")%></label>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-md-7">
                            <div class="card border-0 shadow-sm rounded-4 h-100">
                                <div class="card-body">
                                    <h6 class="fw-bold text-secondary mb-3 border-bottom pb-2"><%=Common.getBahasaConfig("Pemetaan Relasi")%></h6>
                                    
                                    <div class="mb-4">
                                        <label class="form-label fw-bold small text-primary"><%=Common.getBahasaConfig("Pilih Profil Lulusan Terkait")%></label>
                                        <div class="p-3 border rounded bg-white shadow-sm" style="max-height: 180px; overflow-y: auto;">
                                            <% if(listProfil.isEmpty()) { %>
                                                <span class="text-muted small fst-italic"><%=Common.getBahasaConfig("Data master Profil Lulusan kosong.")%></span>
                                            <% } else { 
                                                for(ProfilLulusan pl : listProfil) {
                                                    boolean isChecked = profilTerpilih.contains("," + pl.getId() + ",");
                                            %>
                                                <div class="form-check mb-2">
                                                    <input class="form-check-input chk-profil-cpl" type="checkbox" value="<%=pl.getId()%>" id="chkPL_<%=pl.getId()%>" <%=isChecked ? "checked" : ""%>>
                                                    <label class="form-check-label small text-dark" for="chkPL_<%=pl.getId()%>"><strong><%=pl.getKode()%></strong> - <%=pl.getNama()%></label>
                                                </div>
                                            <%  } 
                                            } %>
                                        </div>
                                    </div>

                                    <div class="mb-3">
                                        <label class="form-label fw-bold small text-success"><%=Common.getBahasaConfig("Pilih Bahan Kajian Terkait")%></label>
                                        <div class="p-3 border rounded bg-white shadow-sm" style="max-height: 180px; overflow-y: auto;">
                                            <% if(listBahan.isEmpty()) { %>
                                                <span class="text-muted small fst-italic"><%=Common.getBahasaConfig("Data master Bahan Kajian kosong.")%></span>
                                            <% } else { 
                                                for(BahanKajian bk : listBahan) {
                                                    boolean isChecked = bahanTerpilih.contains("," + bk.getId() + ",");
                                            %>
                                                <div class="form-check mb-2">
                                                    <input class="form-check-input chk-bahan-cpl border-success" type="checkbox" value="<%=bk.getId()%>" id="chkBK_<%=bk.getId()%>" <%=isChecked ? "checked" : ""%>>
                                                    <label class="form-check-label small text-dark" for="chkBK_<%=bk.getId()%>"><strong><%=bk.getKode()%></strong> - <%=bk.getNama()%></label>
                                                </div>
                                            <%  } 
                                            } %>
                                        </div>
                                    </div>

                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer bg-white border-top p-3 justify-content-end">
                    <button type="button" class="btn btn-light border fw-bold rounded-pill px-4 shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times me-2 text-danger"></i><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-primary fw-bold rounded-pill px-4 shadow-sm" onclick="window.simpanDataCPL<%=rnd%>()"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%></button>
                </div>
            </div>
        </div>
    </div>

    <script>
        var myModalCpl = new bootstrap.Modal(document.getElementById('modalCPL_<%=rnd%>'));
        myModalCpl.show();

        // UPDATE DENGAN OBJECT PAYLOAD BUILDER DINAMIS TERNARY DAN ACTION SIMPANDATARINCI
        window.simpanDataCPL<%=rnd%> = async function() {
            var idCpl = document.getElementById('inpIdCPL_<%=rnd%>') ? document.getElementById('inpIdCPL_<%=rnd%>').value : "";
            
            var getCheckedIds = function(className) {
                var ids = [];
                document.querySelectorAll('.' + className + ':checked').forEach(function(el) { ids.push(el.value); });
                return ids.length > 0 ? "," + ids.join(",") + "," : null;
            };

            var dataObj = {
                kode: document.getElementById('inpKodeCPL_<%=rnd%>') ? document.getElementById('inpKodeCPL_<%=rnd%>').value.trim() : null,
                nama: document.getElementById('inpNamaCPL_<%=rnd%>') ? document.getElementById('inpNamaCPL_<%=rnd%>').value.trim() : null,
                keterangan: document.getElementById('inpKetCPL_<%=rnd%>') ? document.getElementById('inpKetCPL_<%=rnd%>').value.trim() : null,
                aktif: document.getElementById('inpAktifCPL_<%=rnd%>') ? document.getElementById('inpAktifCPL_<%=rnd%>').checked : false,
                profil: getCheckedIds('chk-profil-cpl'),
                bahanKajian: getCheckedIds('chk-bahan-cpl'),
                jurusan: document.getElementById('filterJurusan_<%=rnd%>') ? document.getElementById('filterJurusan_<%=rnd%>').value : null
            };

            if(!dataObj.kode || !dataObj.nama) {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Kode dan Deskripsi CPL wajib diisi.")%>', 'bg-warning text-dark');
                return;
            }

            var payload = { action: "simpanDataRinci", log: "true", class: "ais.database.model.obe.CapaianLulusan", data: dataObj, tanpaLogin: "true" };
            if(idCpl !== "") payload.id = idCpl;

            try {
                var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
                var result = await res.json();
                
                if (result.status === '00' || result.status === 'success') {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Data Capaian Lulusan berhasil disimpan.")%>', 'bg-success text-white');
                    myModalCpl.hide();
                    setTimeout(function(){ window.loadTabel2<%=rnd%>(); }, 350);
                } else {
                    if(typeof tampilkanToast === 'function') tampilkanToast(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data.")%>', 'bg-danger text-white');
                }
            } catch (e) {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
            }
        };

        document.getElementById('modalCPL_<%=rnd%>').addEventListener('hidden.bs.modal', function () { this.remove(); });
    </script>
<%
}
%>