<%@page import="java.util.*"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.obe.CapaianLulusan"%>
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
boolean disableForm = !edit || isMahasiswa;

String idKpmStr = request.getParameter("kurikulumPunyaMatakuliah");
String variable = request.getParameter("var") != null ? request.getParameter("var") : rnd;

if(idKpmStr == null || idKpmStr.trim().isEmpty() || idKpmStr.equals("null")) return;

KurikulumPunyaMatakuliah kpm = null;
Matakuliah mk = null;
Long idJurusanKpm = null;

List<CapaianLulusan> listCplLinked = new ArrayList<CapaianLulusan>();

Session sess = HibernateUtil.openSession();
try {
    kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpmStr, true);
    if(kpm != null) {
        mk = kpm.getMatakuliah();
        if(kpm.getKurikulum() != null && kpm.getKurikulum().getJurusan() != null) {
            idJurusanKpm = kpm.getKurikulum().getJurusan().getId();
        }
        if(mk != null && mk.getCapaianLulusan() != null && !mk.getCapaianLulusan().trim().isEmpty()) {
            Set<Long> longs = new HashSet<Long>();
            for(String s : mk.getCapaianLulusan().split(",")) {
                if(!s.trim().isEmpty()) {
                    try { longs.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_capaian_lulusan.jsp:65");}
                }
            }
            if(!longs.isEmpty()) {
                listCplLinked = ais.common.ConstantValues.simpleList(
                    sess.createCriteria(CapaianLulusan.class)
                           .add(Restrictions.in("id", longs))
                           .addOrder(Order.asc("kode")).addOrder(Order.asc("nama")),
                    CapaianLulusan.class
                );
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
                <h6 class="fw-bold text-primary mb-1"><i class="fas fa-graduation-cap me-2"></i><%=Common.getBahasaConfig("Capaian Pembelajaran Lulusan (CPL)")%></h6>
                <small class="text-muted"><%=Common.getBahasaConfig("Daftar CPL Program Studi yang dibebankan pada matakuliah ini.")%></small>
            </div>
            <% if(!disableForm) { %>
            <div class="d-flex gap-2 flex-wrap">
                <button type="button" id="btnGenCplAi<%=rnd%>" class="btn rounded-pill fw-bold px-4 shadow-sm text-white" style="background-color:#7c3aed;" onclick="window.generateCplAi<%=rnd%>()">
                    <i class="fas fa-magic me-2"></i><%=Common.getBahasaConfig("Generate CPL via AI")%>
                </button>
                <button type="button" class="btn btn-primary rounded-pill fw-bold px-4 shadow-sm" onclick="window.bukaDataPickerCpl<%=rnd%>()">
                    <i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Tambah CPL ke MK")%>
                </button>
            </div>
            <% } %>
        </div>

        <div class="table-responsive border rounded-3">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th class="text-center" style="width: 60px;"><%=Common.getBahasaConfig("No.")%></th>
                        <th style="width: 15%;"><%=Common.getBahasaConfig("Kode")%></th>
                        <th><%=Common.getBahasaConfig("Pernyataan CPL")%></th>
                        <% if(!disableForm) { %>
                        <th class="text-center" style="width: 100px;"><%=Common.getBahasaConfig("Aksi")%></th>
                        <% } %>
                    </tr>
                </thead>
                <tbody>
                    <% if(listCplLinked.isEmpty()) { %>
                        <tr><td colspan="<%=!disableForm ? "4" : "3"%>" class="text-center py-5 text-muted fst-italic"><i class="fas fa-info-circle me-2 d-block fa-2x mb-2 opacity-50"></i><%=Common.getBahasaConfig("Belum ada CPL yang dibebankan pada matakuliah ini.")%></td></tr>
                    <% } else { 
                        int no = 1;
                        for(CapaianLulusan cpl : listCplLinked) {
                    %>
                        <tr>
                            <td class="text-center text-muted"><%=no++%></td>
                            <td class="fw-bold text-dark"><span class="badge bg-primary px-3 py-2"><%=cpl.getKode()%></span></td>
                            <td><%=cpl.getNama()%></td>
                            <% if(!disableForm) { %>
                            <td class="text-center">
                                <button type="button" class="btn btn-sm btn-outline-danger rounded-circle shadow-sm" onclick="window.hapusCplDariMk<%=rnd%>('<%=cpl.getId()%>')" title="<%=Common.getBahasaConfig("Hapus CPL")%>">
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

    </div>
</div>

<script>
    var rootUrl_cpl<%=rnd%> = "<%=Common.ROOT%>";
    var classMk_cpl<%=rnd%> = "<%=Matakuliah.class.getName()%>";
    var currentMkId_cpl<%=rnd%> = "<%=mk != null ? mk.getId() : ""%>";
    var stringCplMK<%=rnd%> = "<%=mk != null && mk.getCapaianLulusan() != null ? mk.getCapaianLulusan() : ""%>";

    window.bukaDataPickerCpl<%=rnd%> = function() {
        var existingIds = stringCplMK<%=rnd%>;
        var jsRnd = Math.random().toString(36).substring(2, 9);
        window['cbCpl' + jsRnd] = function(selIds) { window.simpanRelasiCpl<%=rnd%>(selIds); };
        
        var idJurusan = '<%=idJurusanKpm != null ? idJurusanKpm : ""%>';

        var url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=common&s=_data_picker_banyak' +
                  '&class=<%=CapaianLulusan.class.getName()%>' +
                  '&title=<%=URLEncoder.encode(Common.getBahasaConfig("Pilih Capaian Lulusan (CPL)"), "UTF-8")%>' +
                  '&idsTerpilih=' + existingIds +
                  '&callback=cbCpl' + jsRnd +
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

    window.simpanRelasiCpl<%=rnd%> = async function(selectedIds) {
        var p = "";
        if(selectedIds && selectedIds.length > 0) {
            p = "," + selectedIds.join(",") + ",";
        }
        
        try {
            var payload = { action: "simpanDataRinci", class: classMk_cpl<%=rnd%>, id: currentMkId_cpl<%=rnd%>, data: { capaianLulusan: p }, tanpaLogin: "true" };
            var res = await fetch(rootUrl_cpl<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            var result = await res.json();
            if (result.status === '00' || result.status === 'success') {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Daftar CPL berhasil diperbarui.")%>', 'bg-success');
                if (typeof window.reloadTabCpl<%=variable%> === 'function') window.reloadTabCpl<%=variable%>();
            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal memperbarui daftar CPL.")%>', 'bg-danger');
            }
        } catch (e) {
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger');
        }
    };

    window.hapusCplDariMk<%=rnd%> = function(idCpl) {
        showConfirmModal('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus CPL ini dari daftar relasi matakuliah?")%>', async function() {
            var p = "," + stringCplMK<%=rnd%> + ",";
            p = p.replace("," + idCpl + ",", ","); p = p.replace(/,,+/g, ",");
            if(p.startsWith(",")) p = p.substring(1);
            if(p.endsWith(",")) p = p.substring(0, p.length - 1);
            if (p === idCpl.toString()) p = "";

            try {
                var payload = { action: "simpanDataRinci", class: classMk_cpl<%=rnd%>, id: currentMkId_cpl<%=rnd%>, data: { capaianLulusan: p }, tanpaLogin: "true" };
                var res = await fetch(rootUrl_cpl<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
                var result = await res.json();
                if (result.status === '00' || result.status === 'success') {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("CPL berhasil dihapus.")%>', 'bg-success');
                    if (typeof window.reloadTabCpl<%=variable%> === 'function') window.reloadTabCpl<%=variable%>();
                } else {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menghapus CPL.")%>', 'bg-danger');
                }
            } catch(e) {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger');
            }
        });
    };

    // GENERATE CPL via AI
    window.generateCplAi<%=rnd%> = function() {
        var btn = document.getElementById('btnGenCplAi<%=rnd%>');
        var asli = btn ? btn.innerHTML : '';
        if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("AI sedang menganalisis...")%>'; }
        var body = new URLSearchParams(); body.append('kurikulumPunyaMatakuliah', '<%=idKpmStr%>');
        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_generate_cpl_ai', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: body.toString() })
        .then(function(r){ return r.json(); })
        .then(function(d){
            if (!d || d.status !== 'success') throw new Error(d && d.message ? d.message : '<%=Common.getBahasaConfigJS("Gagal generate CPL.")%>');
            if (typeof tampilkanToast === 'function') tampilkanToast(d.message || '<%=Common.getBahasaConfigJS("CPL berhasil dibuat via AI.")%>', 'bg-success text-white');
            if (typeof window.reloadTabCpl<%=variable%> === 'function') window.reloadTabCpl<%=variable%>();
        })
        .catch(function(err){ console.error(err); if (typeof tampilkanToast === 'function') tampilkanToast('AI: ' + (err && err.message ? err.message : err), 'bg-danger text-white'); })
        .finally(function(){ if (btn) { btn.disabled = false; btn.innerHTML = asli; } });
    };
</script>