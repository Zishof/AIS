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
<%@page import="ais.database.model.obe.BahanKajian"%>
<%@page import="ais.database.model.obe.ReferensiLulusan"%>

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
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_4.jsp:43");}

if(idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

// ==========================================
// ACTION 1: LOAD TABEL 4 (BAHAN KAJIAN)
// ==========================================
if ("loadTabel".equals(action)) {
    List<BahanKajian> listBK = new ArrayList<BahanKajian>();
    Map<String, String> mapReferensi = new HashMap<String, String>();
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        
        // OPTIMALISASI MEMORI: Tarik data referensi/pustaka ke dalam Map untuk mempercepat perulangan rendering
        List<ReferensiLulusan> allRef = ConstantValues.simpleList(sess.createCriteria(ReferensiLulusan.class).addOrder(Order.asc("nama")), ReferensiLulusan.class);
        for(ReferensiLulusan r : allRef) { mapReferensi.put(r.getId().toString(), r.getNama()); }

        Criteria crit = sess.createCriteria(BahanKajian.class);
        crit.add(Restrictions.eq("jurusan.id", idJurusan));
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            Disjunction or = Restrictions.disjunction();
            or.add(Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("nama", keyword, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE));
            crit.add(or);
        }
        
        crit.addOrder(Order.asc("kode"));
        listBK = ConstantValues.simpleList(crit, BahanKajian.class);
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_4.jsp:80");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }
%>
    <div class="table-responsive border rounded-3 animate__animated animate__fadeIn">
        <table class="table table-hover align-middle mb-0 text-start small">
            <thead class="table-light border-bottom">
                <tr>
                    <th class="text-center" style="width: 50px;"><%=Common.getBahasaConfig("No")%></th>
                    <th style="width: 15%;"><%=Common.getBahasaConfig("Kode BK")%></th>
                    <th style="width: 30%;"><%=Common.getBahasaConfig("Bahan Kajian (Materi)")%></th>
                    <th style="width: 25%;"><%=Common.getBahasaConfig("Referensi / Pustaka")%></th>
                    <th style="width: 15%;"><%=Common.getBahasaConfig("Keterangan")%></th>
                    <% if(edit || delete) { %><th class="text-center" style="width: 100px;"><%=Common.getBahasaConfig("Aksi")%></th><% } %>
                </tr>
            </thead>
            <tbody>
                <% if(listBK.isEmpty()) { %>
                    <tr>
                        <td colspan="<%= (edit||delete) ? 6 : 5 %>" class="text-center py-5 text-muted fst-italic">
                            <i class="fas fa-folder-open d-block fa-3x mb-3 text-secondary opacity-50"></i>
                            <span class="fw-bold"><%=Common.getBahasaConfig("Data Kosong")%></span><br>
                            <%=Common.getBahasaConfig("Belum ada data Bahan Kajian yang direkam.")%>
                        </td>
                    </tr>
                <% } else { 
                    int no = 1;
                    for(BahanKajian bk : listBK) {
                        String kodeBK = bk.getKode() != null ? bk.getKode() : "-";
                        String namaBK = bk.getNama() != null ? bk.getNama() : "-";
                        String ket = bk.getKeterangan() != null ? bk.getKeterangan() : "-";
                        
                        StringBuilder refStr = new StringBuilder();
                        if(bk.getReferensi() != null && !bk.getReferensi().isEmpty()) {
                            for(String id : bk.getReferensi().split(",")) {
                                if(!id.trim().isEmpty() && mapReferensi.containsKey(id.trim())) {
                                    refStr.append("<span class='badge bg-warning text-dark mb-1 me-1 text-wrap text-start shadow-sm'>").append(mapReferensi.get(id.trim())).append("</span><br>");
                                }
                            }
                        }
                %>
                    <tr>
                        <td class="text-center text-muted fw-bold"><%=no++%></td>
                        <td class="fw-bold text-dark"><span class="badge bg-primary px-3 py-2 shadow-sm"><%=kodeBK%></span></td>
                        <td class="text-dark fw-medium"><%=namaBK%></td>
                        <td><%=refStr.length() > 0 ? refStr.toString() : "-"%></td>
                        <td class="text-secondary"><%=ket%></td>
                        
                        <% if(edit || delete) { %>
                        <td class="text-center text-nowrap">
                            <% if(edit) { %>
                            <button type="button" class="btn btn-sm btn-outline-primary rounded-circle shadow-sm me-1" 
                                    onclick="window.bukaModalBK<%=rnd%>('<%=bk.getId()%>')" 
                                    title="<%=Common.getBahasaConfig("Ubah Data")%>">
                                <i class="fas fa-edit"></i>
                            </button>
                            <% } %>
                            <% if(delete) { %>
                            <button type="button" class="btn btn-sm btn-outline-danger rounded-circle shadow-sm" 
                                    onclick="window.hapusBK<%=rnd%>('<%=bk.getId()%>')" 
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
    BahanKajian bk = new BahanKajian();
    List<ReferensiLulusan> listReferensi = new ArrayList<ReferensiLulusan>();
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        if(idDataStr != null && !idDataStr.trim().isEmpty()) {
            bk = (BahanKajian) sess.get(BahanKajian.class, Long.parseLong(idDataStr));
        }
        if(bk == null) bk = new BahanKajian();
        
        listReferensi = ConstantValues.simpleList(sess.createCriteria(ReferensiLulusan.class).addOrder(Order.asc("nama")), ReferensiLulusan.class);
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_4.jsp:172");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }

    String judulForm = (bk.getId() == null) ? Common.getBahasaConfig("Tambah Bahan Kajian Baru") : Common.getBahasaConfig("Ubah Bahan Kajian");
    String referensiTerpilih = bk.getReferensi() != null ? bk.getReferensi() : "";
%>
    <div class="modal fade" id="modalBK_<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
        <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content shadow-lg border-0 rounded-4">
                <div class="modal-header bg-primary text-white border-bottom-0 pb-3">
                    <h6 class="modal-title fw-bold"><i class="fas fa-book-open me-2"></i><%=judulForm%></h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4 bg-light">
                    <input type="hidden" id="inpIdBK_<%=rnd%>" value="<%=bk.getId() != null ? bk.getId() : ""%>">
                    
                    <div class="row g-4">
                        <div class="col-md-5">
                            <div class="card border-0 shadow-sm rounded-4 h-100">
                                <div class="card-body">
                                    <h6 class="fw-bold text-secondary mb-3 border-bottom pb-2"><%=Common.getBahasaConfig("Informasi Bahan Kajian")%></h6>
                                    <div class="mb-3">
                                        <label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Kode Bahan Kajian")%> *</label>
                                        <input type="text" class="form-control border-primary" id="inpKodeBK_<%=rnd%>" value="<%=bk.getKode() != null ? bk.getKode().replace("\"", "&quot;") : ""%>">
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Nama / Materi Kajian")%> *</label>
                                        <textarea class="form-control border-primary" id="inpNamaBK_<%=rnd%>" rows="3"><%=bk.getNama() != null ? bk.getNama() : ""%></textarea>
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                                        <textarea class="form-control border-primary" id="inpKetBK_<%=rnd%>" rows="3"><%=bk.getKeterangan() != null ? bk.getKeterangan() : ""%></textarea>
                                    </div>
                                    <div class="form-check form-switch mt-4">
                                        <input class="form-check-input" type="checkbox" id="inpAktifBK_<%=rnd%>" <%=bk.getAktif() == null || bk.getAktif() ? "checked" : ""%>>
                                        <label class="form-check-label fw-bold small text-dark" for="inpAktifBK_<%=rnd%>"><%=Common.getBahasaConfig("Status Aktif")%></label>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-md-7">
                            <div class="card border-0 shadow-sm rounded-4 h-100">
                                <div class="card-body">
                                    <h6 class="fw-bold text-secondary mb-3 border-bottom pb-2"><%=Common.getBahasaConfig("Pustaka / Referensi Terkait")%></h6>
                                    
                                    <div class="input-group input-group-sm mb-3 shadow-sm">
                                        <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                                        <input type="text" id="inpCariRef_<%=rnd%>" class="form-control border-start-0 ps-0" placeholder="<%=Common.getBahasaConfig("Cari berdasarkan kode atau nama referensi...")%>" onkeyup="window.filterReferensi<%=rnd%>()">
                                    </div>

                                    <div class="p-3 border rounded bg-white shadow-sm" id="wrapListRef_<%=rnd%>" style="max-height: 300px; overflow-y: auto;">
                                        <% if(listReferensi.isEmpty()) { %>
                                            <span class="text-muted small fst-italic"><%=Common.getBahasaConfig("Data master Referensi Pustaka kosong.")%></span>
                                        <% } else { 
                                            for(ReferensiLulusan ref : listReferensi) {
                                                boolean isChecked = referensiTerpilih.contains("," + ref.getId() + ",");
                                                String refKode = ref.getKode() != null ? ref.getKode() : "";
                                                String refNama = ref.getNama() != null ? ref.getNama() : "";
                                                
                                                // Data kombinasi untuk kebutuhan filter (di-lowercase agar case-insensitive)
                                                String searchText = (refKode + " " + refNama).toLowerCase().replace("\"", "");
                                        %>
                                            <div class="form-check mb-2 item-referensi-bk" data-search="<%=searchText%>">
                                                <input class="form-check-input chk-referensi-bk" type="checkbox" value="<%=ref.getId()%>" id="chkRef_<%=ref.getId()%>" <%=isChecked ? "checked" : ""%>>
                                                <label class="form-check-label small text-dark" for="chkRef_<%=ref.getId()%>">
                                                    <% if(!refKode.isEmpty()) { %><strong><%=refKode%></strong> - <% } %><%=refNama%>
                                                </label>
                                            </div>
                                        <%  } 
                                        } %>
                                    </div>
                                    <small class="text-muted mt-2 d-block"><i class="fas fa-info-circle me-1 text-info"></i><%=Common.getBahasaConfig("Centang sumber buku atau pedoman pustaka yang menjadi acuan bahan kajian ini. Gunakan kotak pencarian untuk mencari lebih cepat.")%></small>

                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer bg-white border-top p-3 justify-content-end">
                    <button type="button" class="btn btn-light border fw-bold rounded-pill px-4 shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times me-2 text-danger"></i><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-primary fw-bold rounded-pill px-4 shadow-sm" onclick="window.simpanDataBK<%=rnd%>()"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%></button>
                </div>
            </div>
        </div>
    </div>

    <script>
        var myModalBk = new bootstrap.Modal(document.getElementById('modalBK_<%=rnd%>'));
        myModalBk.show();

        // FUNGSI PENYARINGAN REFERENSI (REAL-TIME CLIENT-SIDE)
        window.filterReferensi<%=rnd%> = function() {
            var input = document.getElementById('inpCariRef_<%=rnd%>');
            var filter = input.value.toLowerCase();
            var nodes = document.querySelectorAll('#wrapListRef_<%=rnd%> .item-referensi-bk');

            nodes.forEach(function(node) {
                var txt = node.getAttribute('data-search') || "";
                if (txt.indexOf(filter) > -1) {
                    node.style.display = "";
                } else {
                    node.style.display = "none";
                }
            });
        };

        // FUNGSI SIMPAN DATA BAHAN KAJIAN
        window.simpanDataBK<%=rnd%> = async function() {
            var idBk = document.getElementById('inpIdBK_<%=rnd%>') ? document.getElementById('inpIdBK_<%=rnd%>').value : "";
            
            var getCheckedIds = function(className) {
                var ids = [];
                document.querySelectorAll('.' + className + ':checked').forEach(function(el) { ids.push(el.value); });
                return ids.length > 0 ? "," + ids.join(",") + "," : null;
            };

            var dataObj = {
                kode: document.getElementById('inpKodeBK_<%=rnd%>') ? document.getElementById('inpKodeBK_<%=rnd%>').value.trim() : null,
                nama: document.getElementById('inpNamaBK_<%=rnd%>') ? document.getElementById('inpNamaBK_<%=rnd%>').value.trim() : null,
                keterangan: document.getElementById('inpKetBK_<%=rnd%>') ? document.getElementById('inpKetBK_<%=rnd%>').value.trim() : null,
                aktif: document.getElementById('inpAktifBK_<%=rnd%>') ? document.getElementById('inpAktifBK_<%=rnd%>').checked : false,
                referensi: getCheckedIds('chk-referensi-bk'),
                jurusan: document.getElementById('filterJurusan_<%=rnd%>') ? document.getElementById('filterJurusan_<%=rnd%>').value : null
            };

            if(!dataObj.kode || !dataObj.nama) {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Kode dan Nama Bahan Kajian wajib diisi.")%>', 'bg-warning text-dark');
                return;
            }

            var payload = { action: "simpanDataRinci", log: "true", class: "ais.database.model.obe.BahanKajian", data: dataObj, tanpaLogin: "true" };
            if(idBk !== "") payload.id = idBk;

            try {
                var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
                var result = await res.json();
                
                if (result.status === '00' || result.status === 'success') {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Data Bahan Kajian berhasil disimpan.")%>', 'bg-success text-white');
                    myModalBk.hide();
                    setTimeout(function(){ window.loadTabel4<%=rnd%>(); }, 350);
                } else {
                    if(typeof tampilkanToast === 'function') tampilkanToast(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data.")%>', 'bg-danger text-white');
                }
            } catch (e) {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
            }
        };

        document.getElementById('modalBK_<%=rnd%>').addEventListener('hidden.bs.modal', function () { this.remove(); });
    </script>
<%
}
%>