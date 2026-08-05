<%@page import="java.util.*"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.obe.ProfilLulusan"%>
<%@page import="ais.database.model.obe.CapaianLulusan"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%!
    // Helper method untuk memecah String berbatas koma menjadi Set<Long>
    private Set<Long> parseIdsToSet(String ids) {
        Set<Long> longs = new HashSet<Long>();
        if (ids != null && !ids.trim().isEmpty()) {
            for (String s : ids.split(",")) {
                if (!s.trim().isEmpty()) {
                    try { longs.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_profile_lulusan.jsp:23");}
                }
            }
        }
        return longs;
    }
%>
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
Matakuliah matakuliah = null;
Long idJurusanKpm = null;

List<ProfilLulusan> listPlLinked = new ArrayList<ProfilLulusan>();
List<CapaianLulusan> listCplLinked = new ArrayList<CapaianLulusan>();

Session sess = HibernateUtil.openSession();
try {
    kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpmStr, true);
    if(kpm != null) {
        matakuliah = kpm.getMatakuliah();
        if(kpm.getKurikulum() != null && kpm.getKurikulum().getJurusan() != null) {
            idJurusanKpm = kpm.getKurikulum().getJurusan().getId();
        }

        if(matakuliah != null) {
            if(matakuliah.getProfilLulusan() != null && !matakuliah.getProfilLulusan().trim().isEmpty()) {
                Set<Long> longsPl = parseIdsToSet(matakuliah.getProfilLulusan());
                if(!longsPl.isEmpty()) {
                    listPlLinked = ConstantValues.simpleList(
                        sess.createCriteria(ProfilLulusan.class)
                               .add(Restrictions.in("id", longsPl))
                               .addOrder(Order.asc("kode")).addOrder(Order.asc("nama")),
                        ProfilLulusan.class
                    );
                }
            }

            if(matakuliah.getCapaianLulusan() != null && !matakuliah.getCapaianLulusan().trim().isEmpty()) {
                Set<Long> longsCpl = parseIdsToSet(matakuliah.getCapaianLulusan());
                if(!longsCpl.isEmpty()) {
                    listCplLinked = ConstantValues.simpleList(
                        sess.createCriteria(CapaianLulusan.class)
                               .add(Restrictions.in("id", longsCpl))
                               .addOrder(Order.asc("kode")).addOrder(Order.asc("nama")),
                        CapaianLulusan.class
                    );
                }
            }
        }
    }
} finally {
    if (sess.isOpen()) { sess.disconnect(); sess.close(); }
    HibernateUtil.closeSessionQuietly(sess);
}
%>

<div class="card border-0 shadow-sm rounded-4 mb-4 animate__animated animate__fadeIn">
    <div class="card-body p-4">
        
        <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3 flex-wrap gap-3">
            <div>
                <h6 class="fw-bold text-primary mb-1"><i class="fas fa-user-tie me-2"></i><%=Common.getBahasaConfig("Profil Lulusan / Profesi Lulusan")%></h6>
                <small class="text-muted"><%=Common.getBahasaConfig("Daftar Profil Lulusan yang dibebankan pada matakuliah ini.")%></small>
            </div>
            <% if(!disableForm) { %>
            <button type="button" class="btn btn-primary rounded-pill fw-bold px-4 shadow-sm" onclick="window.bukaDataPickerProfil<%=rnd%>()">
                <i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Tambah Profil Lulusan")%>
            </button>
            <% } %>
        </div>

        <div class="table-responsive border rounded-3 mb-5">
            <table class="table table-hover align-middle mb-0 text-center small">
                <thead class="table-light">
                    <tr>
                        <th style="width: 60px;"><%=Common.getBahasaConfig("No.")%></th>
                        <th style="width: 15%;"><%=Common.getBahasaConfig("Kode")%></th>
                        <th class="text-start"><%=Common.getBahasaConfig("Nama Profil / Profesi Lulusan")%></th>
                        <% if(!disableForm) { %><th style="width: 100px;"><%=Common.getBahasaConfig("Aksi")%></th><% } %>
                    </tr>
                </thead>
                <tbody>
                    <% if(listPlLinked.isEmpty()) { %>
                        <tr><td colspan="<%=!disableForm ? "4" : "3"%>" class="text-center py-5 text-muted fst-italic"><i class="fas fa-info-circle me-2 d-block fa-2x mb-2 opacity-50"></i><%=Common.getBahasaConfig("Belum ada Profil Lulusan yang dibebankan pada matakuliah ini.")%></td></tr>
                    <% } else { 
                        int no = 1;
                        for(ProfilLulusan pl : listPlLinked) {
                    %>
                        <tr>
                            <td class="text-muted"><%=no++%></td>
                            <td class="fw-bold text-dark"><span class="badge bg-warning text-dark px-3 py-2"><%=pl.getKode()%></span></td>
                            <td class="text-start"><%=pl.getNama()%></td>
                            <% if(!disableForm) { %>
                            <td>
                                <button type="button" class="btn btn-sm btn-outline-danger rounded-circle shadow-sm" onclick="window.hapusProfilLulusan<%=rnd%>('<%=pl.getId()%>')" title="<%=Common.getBahasaConfig("Hapus Profil Lulusan")%>">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </td>
                            <% } %>
                        </tr>
                    <%  }
                    } %>
                </tbody>
            </table>
        </div>

        <h6 class="fw-bold text-secondary mb-3"><i class="fas fa-th me-2"></i><%=Common.getBahasaConfig("Matriks Korelasi CPL terhadap Profil Lulusan")%></h6>
        <div class="alert alert-light border shadow-sm small mb-4">
            <i class="fas fa-info-circle text-primary me-2"></i><%=Common.getBahasaConfig("Centang kotak untuk memetakan CPL ke Profil Lulusan. Pastikan Profil Lulusan dan CPL telah didaftarkan di atas terlebih dahulu.")%>
        </div>
        
        <div class="table-responsive border rounded-3">
            <table class="table table-bordered table-hover align-middle mb-0 text-center">
                <thead class="table-light">
                    <tr>
                        <th rowspan="2" class="align-middle" style="width: 15%;"><%=Common.getBahasaConfig("CPL")%></th>
                        <% if(listPlLinked.size() > 0) { %>
                        <th colspan="<%=listPlLinked.size()%>"><%=Common.getBahasaConfig("Profil Lulusan")%></th>
                        <% } %>
                    </tr>
                    <tr>
                        <% for(ProfilLulusan pl : listPlLinked) { %>
                            <th class="fw-bold text-dark" title="<%=pl.getNama()%>"><%=pl.getKode()%></th>
                        <% } %>
                    </tr>
                </thead>
                <tbody>
                    <% if(listCplLinked.isEmpty()) { %>
                        <tr><td colspan="<%=listPlLinked.size() + 1%>" class="py-4 text-muted fst-italic"><%=Common.getBahasaConfig("CPL belum ditentukan untuk matakuliah ini.")%></td></tr>
                    <% } else if (listPlLinked.isEmpty()) { %>
                        <tr><td colspan="2" class="py-4 text-muted fst-italic"><%=Common.getBahasaConfig("Profil Lulusan belum ditentukan untuk matakuliah ini.")%></td></tr>
                    <% } else {
                        for(CapaianLulusan cpl : listCplLinked) {
                            String profilMapped = cpl.getProfil() != null ? cpl.getProfil() : "";
                    %>
                        <tr>
                            <td class="text-start fw-bold text-primary" title="<%=cpl.getNama()%>"><%=cpl.getKode()%></td>
                            <% for(ProfilLulusan pl : listPlLinked) { 
                                boolean isMapped = profilMapped.contains("," + pl.getId() + ",");
                            %>
                                <td>
                                    <input type="checkbox" class="form-check-input border-secondary shadow-sm" style="cursor:pointer; width:1.2rem; height:1.2rem;" 
                                           <%=isMapped ? "checked" : ""%> 
                                           <%=disableForm ? "disabled" : ""%> 
                                           onchange="window.updatePemetaanCPL_PL<%=rnd%>(this, '<%=cpl.getId()%>', '<%=pl.getId()%>')">
                                </td>
                            <% } %>
                        </tr>
                    <%  }
                    } %>
                </tbody>
            </table>
        </div>

    </div>
</div>

<script>
    var rootUrl_pl<%=rnd%> = "<%=Common.ROOT%>";
    var classMk_pl<%=rnd%> = "<%=Matakuliah.class.getName()%>";
    var classCpl_pl<%=rnd%> = "<%=CapaianLulusan.class.getName()%>";
    var currentMkId_pl<%=rnd%> = "<%=matakuliah != null ? matakuliah.getId() : ""%>";
    var stringPlMK<%=rnd%> = "<%=matakuliah != null && matakuliah.getProfilLulusan() != null ? matakuliah.getProfilLulusan() : ""%>";

    // -------------------------------------------------------------
    // FUNGSI 1: MENGELOLA DAFTAR PROFIL LULUSAN
    // -------------------------------------------------------------
    window.bukaDataPickerProfil<%=rnd%> = function() {
        var existingIds = stringPlMK<%=rnd%>;
        var jsRnd = Math.random().toString(36).substring(2, 9);
        window['cbProfil' + jsRnd] = function(selData) { window.simpanProfilLulusan<%=rnd%>(selData); };
        
        var idJurusan = '<%=idJurusanKpm != null ? idJurusanKpm : ""%>';

        var url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=common&s=_data_picker_banyak' +
                  '&class=<%=ProfilLulusan.class.getName()%>' +
                  '&title=<%=URLEncoder.encode(Common.getBahasaConfig("Pilih Profil Lulusan"), "UTF-8")%>' +
                  '&idsTerpilih=' + encodeURIComponent(existingIds) +
                  '&callback=cbProfil' + jsRnd +
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

    window.simpanProfilLulusan<%=rnd%> = async function(selectedData) {
        if (!selectedData || selectedData.length === 0) return;

        var existingArr = stringPlMK<%=rnd%> ? stringPlMK<%=rnd%>.split(",").filter(function(x) { return x.trim() !== ""; }) : [];
        selectedData.forEach(function(item) {
            var newId = String(item.id || item);
            if(existingArr.indexOf(newId) === -1) {
                existingArr.push(newId);
            }
        });
        
        var p = existingArr.length > 0 ? "," + existingArr.join(",") + "," : "";
        
        try {
            var payload = { action: "simpanDataRinci", class: classMk_pl<%=rnd%>, id: currentMkId_pl<%=rnd%>, data: { profilLulusan: p }, tanpaLogin: "true" };
            var res = await fetch(rootUrl_pl<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            var result = await res.json();
            if (result.status === '00' || result.status === 'success') {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Daftar Profil Lulusan berhasil diperbarui.")%>', 'bg-success text-white');
                if (typeof window.reloadTabPl<%=variable%> === 'function') window.reloadTabPl<%=variable%>();
            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal memperbarui daftar Profil Lulusan.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
        }
    };

    window.hapusProfilLulusan<%=rnd%> = function(idPl) {
        if(typeof showConfirmModal === 'function') {
            showConfirmModal('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus Profil Lulusan ini dari daftar matakuliah?")%>', async function() {
                var existingArr = stringPlMK<%=rnd%> ? stringPlMK<%=rnd%>.split(",").filter(function(x) { return x.trim() !== ""; }) : [];
                existingArr = existingArr.filter(function(x) { return x !== String(idPl); });
                var p = existingArr.length > 0 ? "," + existingArr.join(",") + "," : "";

                try {
                    var payload = { action: "simpanDataRinci", class: classMk_pl<%=rnd%>, id: currentMkId_pl<%=rnd%>, data: { profilLulusan: p }, tanpaLogin: "true" };
                    var res = await fetch(rootUrl_pl<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
                    var result = await res.json();
                    if (result.status === '00' || result.status === 'success') {
                        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Profil Lulusan berhasil dihapus.")%>', 'bg-success text-white');
                        if (typeof window.reloadTabPl<%=variable%> === 'function') window.reloadTabPl<%=variable%>();
                    } else {
                        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menghapus Profil Lulusan.")%>', 'bg-danger text-white');
                    }
                } catch(e) {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
                }
            });
        }
    };

    // -------------------------------------------------------------
    // FUNGSI 2: MENGELOLA KORELASI CPL - PROFIL LULUSAN
    // -------------------------------------------------------------
    window.updatePemetaanCPL_PL<%=rnd%> = async function(checkbox, idCpl, idPl) {
        var isChecked = checkbox.checked;
        checkbox.disabled = true;

        try {
            // Ambil state CPL terbaru
            var fetchPayload = { action: "ambilDataRinci", class: classCpl_pl<%=rnd%>, id: idCpl, tanpaLogin: "true" };
            var resFetch = await fetch(rootUrl_pl<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(fetchPayload) });
            var resultFetch = await resFetch.json();

            if(resultFetch.status === '00' || resultFetch.status === 'success') {
                var cplData = resultFetch.data;
                var currentProfil = cplData.profil || "";
                
                var existingArr = currentProfil.split(",").filter(function(x) { return x.trim() !== ""; });
                if(isChecked) {
                    if(existingArr.indexOf(String(idPl)) === -1) existingArr.push(String(idPl));
                } else {
                    existingArr = existingArr.filter(function(x) { return x !== String(idPl); });
                }
                var p = existingArr.length > 0 ? "," + existingArr.join(",") + "," : "";

                var payload = {
                    action: "simpanDataRinci",
                    class: classCpl_pl<%=rnd%>,
                    id: idCpl,
                    data: { profil: p },
                    tanpaLogin: "true"
                };

                var resSave = await fetch(rootUrl_pl<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
                var resultSave = await resSave.json();
                
                if(resultSave.status === '00' || resultSave.status === 'success') {
                    if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Korelasi matriks berhasil diperbarui.")%>', 'bg-success text-white');
                } else {
                    checkbox.checked = !isChecked; // Revert UI
                    if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal memperbarui pemetaan")%>', 'bg-danger text-white');
                }
            } else {
                checkbox.checked = !isChecked; // Revert UI
                if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Data CPL tidak ditemukan di pangkalan data")%>', 'bg-danger text-white');
            }
        } catch(e) {
            checkbox.checked = !isChecked; // Revert UI
            if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan")%>', 'bg-danger text-white');
        } finally {
            checkbox.disabled = false;
        }
    };
</script>