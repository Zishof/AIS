<%@page import="java.util.*"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.obe.CapaianPembelajaranLulusan"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%
String rnd = Common.getGeneratedBarCode(7);
Tbmuser tbmuser = Common.getCurrentUser(request);

if (tbmuser == null || tbmuser.getUserId() == null) return;

boolean edit = false;
boolean delete = false;
if (tbmuser != null && tbmuser.getUserId() != null) {
    boolean isNotStudentTeacher = tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
            && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null && tbmuser.getGuru() == null;
            
    if (isNotStudentTeacher) {
        edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
        delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
    }
    
    if (Common.getApakahAdminLain(tbmuser)) {
        edit = true; delete = true;
    }
    if (tbmuser.getDosen() != null) {
        edit = true; delete = true;
    }
}

boolean isMahasiswa = tbmuser.getMahasiswa() != null;
boolean isDosen = tbmuser.getDosen() != null;
boolean disableForm = !edit || isMahasiswa || isDosen;

String idKpmStr = request.getParameter("kurikulumPunyaMatakuliah");
String variable = request.getParameter("var") != null ? request.getParameter("var") : rnd;

if(idKpmStr == null || idKpmStr.trim().isEmpty() || idKpmStr.equals("null")) return;

KurikulumPunyaMatakuliah kpm = null;
Matakuliah mk = null;
Long idJurusanKpm = null;
List<CapaianPembelajaranLulusan> listCpmkLinked = new ArrayList<CapaianPembelajaranLulusan>();

// Menggunakan variabel 'sess' untuk menghindari Duplicate local variable session
Session sess = HibernateUtil.openSession();
try {
    kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpmStr, true);
    if(kpm != null) {
        mk = kpm.getMatakuliah();
        if(kpm.getKurikulum() != null && kpm.getKurikulum().getJurusan() != null) {
            idJurusanKpm = kpm.getKurikulum().getJurusan().getId();
        }
        
        // Membaca relasi CPMK langsung dari Matakuliah (bukan dari KPM)
        if(mk != null && mk.getCapaianPembelajaranLulusan() != null && !mk.getCapaianPembelajaranLulusan().trim().isEmpty()) {
            Set<Long> longs = new HashSet<Long>();
            for(String s : mk.getCapaianPembelajaranLulusan().split(",")) {
                if(!s.trim().isEmpty()) {
                    try { longs.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_capaian_pembelajaran_lulusan.jsp:70");}
                }
            }
            if(!longs.isEmpty()) {
                listCpmkLinked = ais.common.ConstantValues.simpleList(
                    sess.createCriteria(CapaianPembelajaranLulusan.class)
                           .add(Restrictions.in("id", longs))
                           .addOrder(Order.asc("kode")).addOrder(Order.asc("nama")),
                    CapaianPembelajaranLulusan.class
                );
            }
        }
    }
} finally {
    if (sess.isOpen()) { sess.disconnect(); sess.close(); }
    HibernateUtil.closeSessionQuietly(sess);
}

boolean isNilaiCpmk = kpm != null && kpm.getNilaiMenggunakanCpmk() != null && kpm.getNilaiMenggunakanCpmk();
%>

<div class="card border-0 shadow-sm rounded-4 mb-4 animate__animated animate__fadeIn">
    <div class="card-body p-4">
        
        <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3 flex-wrap gap-3">
            <div>
                <h6 class="fw-bold text-primary mb-1"><i class="fas fa-bullseye me-2"></i><%=Common.getBahasaConfig("Capaian Pembelajaran Matakuliah (CPMK)")%></h6>
                <small class="text-muted"><%=Common.getBahasaConfig("Pilih CPMK dan tetapkan komponen Sub-CPMK pembentuknya.")%></small>
            </div>
            <% if(!disableForm) { %>
            <div class="d-flex gap-2 flex-wrap">
                <button type="button" id="btnGenCpmkAi<%=rnd%>" class="btn rounded-pill fw-bold px-4 shadow-sm text-white" style="background-color:#7c3aed;" onclick="window.generateCpmkAi<%=rnd%>()">
                    <i class="fas fa-magic me-2"></i><%=Common.getBahasaConfig("Generate CPMK via AI")%>
                </button>
                <button type="button" class="btn btn-primary rounded-pill fw-bold px-4 shadow-sm" onclick="window.bukaDataPickerCpmk<%=rnd%>()">
                    <i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Tambah CPMK ke Matakuliah")%>
                </button>
            </div>
            <% } %>
        </div>

        <div class="accordion shadow-sm" id="accordionCpmk<%=rnd%>">
            <% if(listCpmkLinked.isEmpty()) { %>
                <div class="text-center py-5 text-muted fst-italic border rounded-3 bg-light"><i class="fas fa-info-circle me-2 d-block fa-2x mb-2 opacity-50"></i><%=Common.getBahasaConfig("Belum ada CPMK yang ditentukan untuk kelas ini.")%></div>
            <% } else { 
                int index = 0;
                for(CapaianPembelajaranLulusan cpmk : listCpmkLinked) {
                    index++;
                    String collapseId = "collapseCpmk_" + cpmk.getId() + "_" + rnd;
                    String headingId = "headingCpmk_" + cpmk.getId() + "_" + rnd;
                    
                    // Parse Sub-CPMK (Formula internal Cpmk)
                    JSONArray formulaArr = new JSONArray();
                    try { if(cpmk.getFormula() != null && !cpmk.getFormula().trim().isEmpty()) formulaArr = new JSONArray(cpmk.getFormula()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_capaian_pembelajaran_lulusan.jsp:118");}
            %>
                <div class="accordion-item border-0 border-bottom mb-2 bg-white rounded-3 overflow-hidden">
                    <h2 class="accordion-header" id="<%=headingId%>">
                        <button type="button" class="accordion-button <%=index != 1 ? "collapsed" : ""%> bg-light fw-bold text-dark" data-bs-toggle="collapse" data-bs-target="#<%=collapseId%>" aria-expanded="<%=index == 1 ? "true" : "false"%>" aria-controls="<%=collapseId%>">
                            <span class="badge bg-primary me-3 px-3 py-2"><%=cpmk.getKode()%></span>
                            <div class="pe-3"><%=cpmk.getNama()%></div>
                        </button>
                    </h2>
                    <div id="<%=collapseId%>" class="accordion-collapse collapse <%=index == 1 ? "show" : ""%>" aria-labelledby="<%=headingId%>" data-bs-parent="#accordionCpmk<%=rnd%>">
                        <div class="accordion-body p-4">
                            
                            <% if(!isNilaiCpmk) { %>
                                <div class="d-flex justify-content-between align-items-center mb-3">
                                    <h6 class="fw-bold text-secondary mb-0"><i class="fas fa-sitemap me-2"></i><%=Common.getBahasaConfig("Komponen Sub-CPMK")%></h6>
                                    <% if(!disableForm) { %>
                                    <div class="d-flex gap-2">
                                        <button type="button" id="btnGenSubCpmkAi_<%=cpmk.getId()%>_<%=rnd%>" class="btn btn-sm fw-bold rounded-pill px-3 shadow-sm text-white" style="background-color:#7c3aed;" onclick="window.generateSubCpmkAi<%=rnd%>('<%=cpmk.getId()%>')">
                                            <i class="fas fa-magic me-1"></i><%=Common.getBahasaConfig("Sub-CPMK via AI")%>
                                        </button>
                                        <button type="button" class="btn btn-sm btn-outline-primary fw-bold rounded-pill px-3 shadow-sm" onclick="window.bukaDataPickerSubCpmk<%=rnd%>('<%=cpmk.getId()%>', '<%=cpmk.getFormula() != null ? URLEncoder.encode(cpmk.getFormula(), "UTF-8") : ""%>')">
                                            <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Atur Sub-CPMK")%>
                                        </button>
                                        <button type="button" class="btn btn-sm btn-outline-danger fw-bold rounded-pill px-3 shadow-sm" onclick="window.hapusCpmkUtama<%=rnd%>('<%=cpmk.getId()%>')">
                                            <i class="fas fa-trash me-1"></i><%=Common.getBahasaConfig("Hapus CPMK Utama")%>
                                        </button>
                                    </div>
                                    <% } %>
                                </div>

                                <div class="table-responsive border rounded-3">
                                    <table class="table table-hover align-middle mb-0 text-center small">
                                        <thead class="table-light">
                                            <tr>
                                                <th style="width: 50px;"><%=Common.getBahasaConfig("No.")%></th>
                                                <th><%=Common.getBahasaConfig("Kode")%></th>
                                                <th class="text-start"><%=Common.getBahasaConfig("Pernyataan Sub-CPMK")%></th>
                                                <% if(!disableForm) { %><th style="width: 60px;"><%=Common.getBahasaConfig("Aksi")%></th><% } %>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <% if(formulaArr.length() == 0) { %>
                                                <tr><td colspan="<%=!disableForm ? "4" : "3"%>" class="text-center py-4 text-muted fst-italic"><%=Common.getBahasaConfig("Belum ada Sub-CPMK yang dipetakan ke CPMK ini.")%></td></tr>
                                            <% } else { 
                                                for(int i = 0; i < formulaArr.length(); i++) {
                                                    JSONObject subObj = formulaArr.getJSONObject(i);
                                                    String subId = subObj.optString("id", "");
                                                    String subKode = subObj.optString("kode", "-");
                                                    String subNama = subObj.optString("nama", "-");
                                            %>
                                                <tr>
                                                    <td class="text-muted"><%=i+1%></td>
                                                    <td class="fw-bold"><span class="badge bg-secondary"><%=subKode%></span></td>
                                                    <td class="text-start"><%=subNama%></td>
                                                    <% if(!disableForm) { %>
                                                    <td>
                                                        <button type="button" class="btn btn-sm btn-outline-danger rounded-circle shadow-sm" onclick="window.hapusSubCpmk<%=rnd%>('<%=cpmk.getId()%>', '<%=subId%>', '<%=URLEncoder.encode(cpmk.getFormula() != null ? cpmk.getFormula() : "", "UTF-8")%>')"><i class="fas fa-times"></i></button>
                                                    </td>
                                                    <% } %>
                                                </tr>
                                            <%  }
                                            } %>
                                        </tbody>
                                    </table>
                                </div>
                            <% } else { %>
                                <div class="alert alert-info border-0 shadow-sm rounded-3 py-3 d-flex justify-content-between align-items-center">
                                    <div><i class="fas fa-info-circle me-2"></i><%=Common.getBahasaConfig("Mata kuliah ini dikonfigurasi untuk dinilai langsung berdasarkan CPMK (Bukan Sub-CPMK).")%></div>
                                    <% if(!disableForm) { %>
                                    <button type="button" class="btn btn-sm btn-danger fw-bold rounded-pill px-3 shadow-sm text-nowrap" onclick="window.hapusCpmkUtama<%=rnd%>('<%=cpmk.getId()%>')">
                                        <i class="fas fa-trash me-1"></i><%=Common.getBahasaConfig("Hapus CPMK")%>
                                    </button>
                                    <% } %>
                                </div>
                            <% } %>
                            
                        </div>
                    </div>
                </div>
            <%  }
            } %>
        </div>

    </div>
</div>

<script>
    var rootUrl_cpmk<%=rnd%> = "<%=Common.ROOT%>";
    var classMk_cpmk<%=rnd%> = "<%=Matakuliah.class.getName()%>";
    var classCpmk_cpmk<%=rnd%> = "<%=CapaianPembelajaranLulusan.class.getName()%>";
    var currentMkId_cpmk<%=rnd%> = "<%=mk != null ? mk.getId() : ""%>";
    var stringCpmkRps<%=rnd%> = "<%=mk != null && mk.getCapaianPembelajaranLulusan() != null ? mk.getCapaianPembelajaranLulusan() : ""%>";

    // -------------------------------------------------------------
    // BAGIAN 1: PENGELOLAAN CPMK UTAMA
    // -------------------------------------------------------------
    window.bukaDataPickerCpmk<%=rnd%> = function() {
        var existingIds = stringCpmkRps<%=rnd%>;
        var jsRnd = Math.random().toString(36).substring(2, 9);
        window['cbCpmk' + jsRnd] = function(selData) { window.simpanRelasiCpmkUtama<%=rnd%>(selData); };
        
        var idJurusan = '<%=idJurusanKpm != null ? idJurusanKpm : ""%>';

        // Menggunakan _data_picker_banyak yang akan me-return Array of Strings (ID)
        var url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=common&s=_data_picker_banyak' +
                  '&class=' + encodeURIComponent(classCpmk_cpmk<%=rnd%>) +
                  '&title=<%=URLEncoder.encode(Common.getBahasaConfig("Pilih CPMK"), "UTF-8")%>' +
                  '&idsTerpilih=' + existingIds +
                  '&callback=cbCpmk' + jsRnd +
                  '&jurusan=' + idJurusan +
                  '&var=' + jsRnd;

        var modalHtml = '<div class="modal fade" id="modalPicker' + jsRnd + '" tabindex="-1" aria-hidden="true">' +
                        '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                        '<div class="modal-content" id="modalContent' + jsRnd + '">' +
                        '<div class="text-center py-5"><div class="spinner-border text-primary" role="status"></div></div>' +
                        '</div></div></div>';
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        var modalEl = document.getElementById('modalPicker' + jsRnd);
        var modalObj = new bootstrap.Modal(modalEl);
        modalObj.show();
        
        fetch(url).then(res => res.text()).then(html => {
            document.getElementById('modalContent' + jsRnd).innerHTML = html;
            var scripts = document.getElementById('modalContent' + jsRnd).querySelectorAll('script');
            scripts.forEach(s => {
                var ns = document.createElement('script');
                if(s.src) ns.src = s.src;
                else ns.textContent = s.textContent;
                document.body.appendChild(ns);
                document.body.removeChild(ns);
            });
        }).catch(e => {
            document.getElementById('modalContent' + jsRnd).innerHTML = '<div class="alert alert-danger m-3"><%=Common.getBahasaConfig("Gagal memuat data picker.")%></div>';
        });

        modalEl.addEventListener('hidden.bs.modal', function () {
            modalEl.remove();
        });
    };

    window.simpanRelasiCpmkUtama<%=rnd%> = async function(selectedData) {
        var p = "";
        // Karena _data_picker_banyak menghasilkan array of strings (ID), kita tinggal gabung
        if(selectedData && selectedData.length > 0) {
            p = "," + selectedData.join(",") + ",";
        }
        
        try {
            var payload = { action: "simpanDataRinci", class: classMk_cpmk<%=rnd%>, id: currentMkId_cpmk<%=rnd%>, data: { capaianPembelajaranLulusan: p }, tanpaLogin: "true" };
            var res = await fetch(rootUrl_cpmk<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            var result = await res.json();
            if (result.status === '00' || result.status === 'success') {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Daftar CPMK Matakuliah berhasil diperbarui.")%>', 'bg-success text-white');
                if (typeof window.reloadTabCpmk<%=variable%> === 'function') window.reloadTabCpmk<%=variable%>();
            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal memperbarui CPMK Matakuliah.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
        }
    };

    window.hapusCpmkUtama<%=rnd%> = function(idCpmk) {
        showConfirmModal('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus CPMK ini beserta konfigurasinya dari Matakuliah?")%>', async function() {
            var p = "," + stringCpmkRps<%=rnd%> + ",";
            p = p.replace("," + idCpmk + ",", ","); p = p.replace(/,,+/g, ",");
            if(p.startsWith(",")) p = p.substring(1);
            if(p.endsWith(",")) p = p.substring(0, p.length - 1);
            if (p === idCpmk.toString()) p = "";

            try {
                var payload = { action: "simpanDataRinci", class: classMk_cpmk<%=rnd%>, id: currentMkId_cpmk<%=rnd%>, data: { capaianPembelajaranLulusan: p }, tanpaLogin: "true" };
                var res = await fetch(rootUrl_cpmk<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
                var result = await res.json();
                if (result.status === '00' || result.status === 'success') {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("CPMK berhasil dihapus dari Matakuliah.")%>', 'bg-success text-white');
                    if (typeof window.reloadTabCpmk<%=variable%> === 'function') window.reloadTabCpmk<%=variable%>();
                } else {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menghapus CPMK.")%>', 'bg-danger text-white');
                }
            } catch(e) {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
            }
        });
    };

    // -------------------------------------------------------------
    // BAGIAN 2: PENGELOLAAN SUB-CPMK (RUMUS/FORMULA JSON)
    // -------------------------------------------------------------
    window.bukaDataPickerSubCpmk<%=rnd%> = function(idCpmkUtama, encodedFormulaStr) {
        var formulaStr = decodeURIComponent(encodedFormulaStr);
        var existingSubIds = "";
        try {
            if (formulaStr && formulaStr.trim().length > 0) {
                var arr = JSON.parse(formulaStr);
                var ids = [];
                for(var i=0; i<arr.length; i++) {
                    if (arr[i] && arr[i].id) ids.push(arr[i].id);
                }
                existingSubIds = ids.join(",");
            }
        } catch(e) { console.error("Parse formula error", e); }

        var jsRnd = Math.random().toString(36).substring(2, 9);
        window['cbSubCpmk' + jsRnd] = function(selData) { window.tambahSubCpmkKeCpmkUtama<%=rnd%>(idCpmkUtama, selData); };
        
        var idJurusan = '<%=idJurusanKpm != null ? idJurusanKpm : ""%>';

        var url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=common&s=_data_picker_banyak' +
                  '&class=' + encodeURIComponent(classCpmk_cpmk<%=rnd%>) +
                  '&title=<%=URLEncoder.encode(Common.getBahasaConfig("Pilih Sub-CPMK"), "UTF-8")%>' +
                  '&idsTerpilih=' + existingSubIds +
                  '&callback=cbSubCpmk' + jsRnd +
                  '&jurusan=' + idJurusan +
                  '&var=' + jsRnd;

        var modalHtml = '<div class="modal fade" id="modalPicker' + jsRnd + '" tabindex="-1" aria-hidden="true">' +
                        '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                        '<div class="modal-content" id="modalContent' + jsRnd + '">' +
                        '<div class="text-center py-5"><div class="spinner-border text-primary" role="status"></div></div>' +
                        '</div></div></div>';
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        var modalEl = document.getElementById('modalPicker' + jsRnd);
        var modalObj = new bootstrap.Modal(modalEl);
        modalObj.show();
        
        fetch(url).then(res => res.text()).then(html => {
            document.getElementById('modalContent' + jsRnd).innerHTML = html;
            var scripts = document.getElementById('modalContent' + jsRnd).querySelectorAll('script');
            scripts.forEach(s => {
                var ns = document.createElement('script');
                if(s.src) ns.src = s.src;
                else ns.textContent = s.textContent;
                document.body.appendChild(ns);
                document.body.removeChild(ns);
            });
        }).catch(e => {
            document.getElementById('modalContent' + jsRnd).innerHTML = '<div class="alert alert-danger m-3"><%=Common.getBahasaConfig("Gagal memuat data picker.")%></div>';
        });

        modalEl.addEventListener('hidden.bs.modal', function () {
            modalEl.remove();
        });
    };

    window.tambahSubCpmkKeCpmkUtama<%=rnd%> = async function(idCpmkUtama, selectedData) {
        if (!selectedData || selectedData.length === 0) {
            try {
                var payloadNull = { action: "simpanDataRinci", class: classCpmk_cpmk<%=rnd%>, id: idCpmkUtama, data: { formula: "" }, tanpaLogin: "true" };
                var resNull = await fetch(rootUrl_cpmk<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payloadNull) });
                var resultNull = await resNull.json();
                if(resultNull.status === '00' || resultNull.status === 'success') {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Pemetaan Sub-CPMK berhasil dikosongkan.")%>', 'bg-success text-white');
                    if (typeof window.reloadTabCpmk<%=variable%> === 'function') window.reloadTabCpmk<%=variable%>();
                }
            } catch(e) {}
            return;
        }

        try {
            // Tarik data asli dari pangkalan data karena _data_picker_banyak hanya memberikan ID (string array)
            var fetchPayload = { action: "ambilSemuaData", log: "true", class: classCpmk_cpmk<%=rnd%>, inList: { prop: "id", data: selectedData }, tanpaLogin: "true" };
            var fetchResp = await fetch(rootUrl_cpmk<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(fetchPayload) });
            var resultList = await fetchResp.json();

            if (resultList && resultList.data) {
                var formulaArray = [];
                for(var i=0; i<resultList.data.length; i++) {
                    var obj = resultList.data[i];
                    formulaArray.push({
                        key: "sub_" + obj.id,
                        id: obj.id,
                        kode: obj.kode || obj.id,
                        nama: obj.nama || "-"
                    });
                }

                var strFormula = JSON.stringify(formulaArray);
                var savePayload = { action: "simpanDataRinci", class: classCpmk_cpmk<%=rnd%>, id: idCpmkUtama, data: { formula: strFormula }, tanpaLogin: "true" };
                
                var resSave = await fetch(rootUrl_cpmk<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(savePayload) });
                var resultSave = await resSave.json();
                
                if (resultSave.status === '00' || resultSave.status === 'success') {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Pemetaan Sub-CPMK berhasil disimpan.")%>', 'bg-success text-white');
                    if (typeof window.reloadTabCpmk<%=variable%> === 'function') window.reloadTabCpmk<%=variable%>();
                } else {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menyimpan pemetaan Sub-CPMK.")%>', 'bg-danger text-white');
                }
            }
        } catch (e) {
            console.error(e);
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
        }
    };

    window.hapusSubCpmk<%=rnd%> = function(idCpmkUtama, subIdHapus, encodedFormulaStr) {
        showConfirmModal('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus Sub-CPMK ini?")%>', async function() {
            var formulaStr = decodeURIComponent(encodedFormulaStr);
            var formulaArray = [];
            if (formulaStr && formulaStr.trim().length > 0) {
                try { formulaArray = JSON.parse(formulaStr); } catch(e){}
            }
            
            formulaArray = formulaArray.filter(function(item) { return String(item.id) !== String(subIdHapus); });
            var strFormula = formulaArray.length > 0 ? JSON.stringify(formulaArray) : "";

            try {
                var savePayload = { action: "simpanDataRinci", class: classCpmk_cpmk<%=rnd%>, id: idCpmkUtama, data: { formula: strFormula }, tanpaLogin: "true" };
                var resSave = await fetch(rootUrl_cpmk<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(savePayload) });
                var resultSave = await resSave.json();
                
                if (resultSave.status === '00' || resultSave.status === 'success') {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Sub-CPMK berhasil dihapus.")%>', 'bg-success text-white');
                    if (typeof window.reloadTabCpmk<%=variable%> === 'function') window.reloadTabCpmk<%=variable%>();
                } else {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menghapus Sub-CPMK.")%>', 'bg-danger text-white');
                }
            } catch (e) {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
            }
        });
    };

    // -------------------------------------------------------------
    // GENERATE via AI (CPMK utama + Sub-CPMK)
    // -------------------------------------------------------------
    window.generateCpmkAi<%=rnd%> = function() {
        var btn = document.getElementById('btnGenCpmkAi<%=rnd%>');
        var asli = btn ? btn.innerHTML : '';
        if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("AI sedang menganalisis...")%>'; }
        var body = new URLSearchParams(); body.append('kurikulumPunyaMatakuliah', '<%=idKpmStr%>');
        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_generate_cpmk_ai', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: body.toString() })
        .then(function(r){ return r.json(); })
        .then(function(d){
            if (!d || d.status !== 'success') throw new Error(d && d.message ? d.message : '<%=Common.getBahasaConfigJS("Gagal generate CPMK.")%>');
            if (typeof tampilkanToast === 'function') tampilkanToast(d.message || '<%=Common.getBahasaConfigJS("CPMK berhasil dibuat via AI.")%>', 'bg-success text-white');
            if (typeof window.reloadTabCpmk<%=variable%> === 'function') window.reloadTabCpmk<%=variable%>();
        })
        .catch(function(err){ console.error(err); if (typeof tampilkanToast === 'function') tampilkanToast('AI: ' + (err && err.message ? err.message : err), 'bg-danger text-white'); })
        .finally(function(){ if (btn) { btn.disabled = false; btn.innerHTML = asli; } });
    };

    window.generateSubCpmkAi<%=rnd%> = function(idCpmk) {
        var btn = document.getElementById('btnGenSubCpmkAi_' + idCpmk + '_<%=rnd%>');
        var asli = btn ? btn.innerHTML : '';
        if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>...'; }
        var body = new URLSearchParams(); body.append('idCpmk', idCpmk);
        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_generate_subcpmk_ai', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: body.toString() })
        .then(function(r){ return r.json(); })
        .then(function(d){
            if (!d || d.status !== 'success') throw new Error(d && d.message ? d.message : '<%=Common.getBahasaConfigJS("Gagal generate Sub-CPMK.")%>');
            if (typeof tampilkanToast === 'function') tampilkanToast(d.message || '<%=Common.getBahasaConfigJS("Sub-CPMK berhasil dibuat via AI.")%>', 'bg-success text-white');
            if (typeof window.reloadTabCpmk<%=variable%> === 'function') window.reloadTabCpmk<%=variable%>();
        })
        .catch(function(err){ console.error(err); if (typeof tampilkanToast === 'function') tampilkanToast('AI: ' + (err && err.message ? err.message : err), 'bg-danger text-white'); if (btn) { btn.disabled = false; btn.innerHTML = asli; } });
    };
</script>