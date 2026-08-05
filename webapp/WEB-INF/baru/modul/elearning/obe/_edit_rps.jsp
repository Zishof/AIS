<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%
String rnd = Common.getGeneratedBarCode(7);
Tbmuser tbmuser = Common.getCurrentUser(request);

if (tbmuser == null || tbmuser.getUserId() == null) {
    out.print("<div class='alert alert-danger m-4 shadow-sm'>" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "</div>");
    return;
}

boolean isMahasiswa = tbmuser.getMahasiswa() != null;
boolean isDosen = tbmuser.getDosen() != null;

boolean edit = false;
if (tbmuser != null && tbmuser.getUserId() != null) {
    boolean isNotStudentTeacher = !isMahasiswa && !isDosen && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null && tbmuser.getGuru() == null;
    if (isNotStudentTeacher) {
        edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    }
    if (Common.getApakahAdminLain(tbmuser)) {
        edit = true;
    }
}

// PENAMBAHAN FALLBACK PARAMETER AGAR BISA MEMBACA 'id' DARI ringkasan.jsp
String idKpmStr = request.getParameter("kurikulumPunyaMatakuliah");
if (idKpmStr == null || idKpmStr.trim().isEmpty() || idKpmStr.equals("null")) {
    idKpmStr = request.getParameter("id");
}
if (idKpmStr == null || idKpmStr.trim().isEmpty() || idKpmStr.equals("null")) {
    idKpmStr = request.getParameter("kur");
}

String idPerkuliahanStr = request.getParameter("perkuliahan");
String variable = request.getParameter("var") != null ? request.getParameter("var") : rnd;

String urlParams = "&var=" + variable;
if (idPerkuliahanStr != null && !idPerkuliahanStr.trim().isEmpty() && !idPerkuliahanStr.equals("null")) {
    urlParams += "&perkuliahan=" + idPerkuliahanStr;
}

if (idKpmStr == null || idKpmStr.trim().isEmpty() || idKpmStr.equals("null")) {
    out.print("<div class='alert alert-danger m-4 shadow-sm'>" + Common.getBahasaConfig("Pengenal (ID) RPS tidak ditemukan.") + "</div>");
    return;
}

KurikulumPunyaMatakuliah kpm = null;
try {
    kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpmStr, true);
} catch(Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_edit_rps.jsp:59"); }

if (kpm == null || kpm.getMatakuliah() == null || kpm.getKurikulum() == null) {
    out.print("<div class='alert alert-danger m-4 shadow-sm'>" + Common.getBahasaConfig("Data Rencana Pembelajaran Semester tidak ditemukan di pangkalan data.") + "</div>");
    return;
}

Matakuliah mk = kpm.getMatakuliah();
boolean isLocked = kpm.getDikunci() != null;
boolean disableForm = isLocked || !edit;

String tglPenyusunan = kpm.getTanggalPenyusunan() != null ? new SimpleDateFormat("yyyy-MM-dd").format(kpm.getTanggalPenyusunan()) : "";
String mkInfo = mk.getKode() + " - " + mk.getNama() + " (" + mk.getSks() + " SKS)";
String lockName = isLocked ? kpm.getDikunci().getUserNama() : Common.getBahasaConfig("Pengguna");

String kaprodiName = "-";
if(kpm.getKurikulum() != null && kpm.getKurikulum().getJurusan() != null && kpm.getKurikulum().getJurusan().getKaprodi() != null) {
    kaprodiName = kpm.getKurikulum().getJurusan().getKaprodi().getNama();
}
%>

<style>
    .nav-tabs .nav-link.active {
        font-weight: bold; color: #0d6efd !important; border-bottom: 3px solid #0d6efd; background-color: transparent !important;
    }
    .nav-tabs .nav-link { color: #6c757d; border: none; transition: color 0.3s ease-in-out; }
    .nav-tabs .nav-link:hover { color: #0d6efd; border-bottom: 3px solid #e9ecef; }
    /* ROMBAK MOBILE: 11 tab RPS jadi 1 baris geser (bukan menumpuk), padding rapat */
    @media (max-width: 767.98px) {
        #rpsEditContainer<%=rnd%> { padding: 1rem !important; }
        #rpsTabs<%=rnd%> { flex-wrap: nowrap; overflow-x: auto; -webkit-overflow-scrolling: touch; scrollbar-width: none; }
        #rpsTabs<%=rnd%>::-webkit-scrollbar { display: none; }
        #rpsTabs<%=rnd%> .nav-item { flex: 0 0 auto; }
        #rpsTabs<%=rnd%> .nav-link { white-space: nowrap; font-size: .8rem; padding: .45rem .6rem; }
    }
</style>

<div id="rpsEditContainer<%=rnd%>" class="animate__animated animate__fadeIn p-4">
    
    <div class="d-flex justify-content-between align-items-start mb-3 flex-wrap gap-3">
        <div><p class="text-muted mb-0"><%=Common.getBahasaConfig("Matakuliah:")%> <b class="text-primary"><%=mkInfo%></b></p></div>
        <div class="text-end">
            <% if (isLocked) { %>
                <div class="badge bg-warning text-dark p-2 px-3 shadow-sm d-block">
                    <i class="fas fa-lock me-1"></i> <%=Common.getBahasaConfig("Dikunci oleh:")%> <%=lockName%>
                </div>
            <% } %>
        </div>
    </div>

    <ul class="nav nav-tabs border-bottom-0 mt-2" id="rpsTabs<%=rnd%>" role="tablist">
        <li class="nav-item" role="presentation"><button class="nav-link active fw-bold px-3" data-bs-toggle="tab" data-bs-target="#tabIdentitas<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Identitas")%></button></li>
        <li class="nav-item" role="presentation"><button class="nav-link fw-bold px-3" data-bs-toggle="tab" data-bs-target="#tabProfilLulusan<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Profil Lulusan")%></button></li>
        <li class="nav-item" role="presentation"><button class="nav-link fw-bold px-3" data-bs-toggle="tab" data-bs-target="#tabCpl<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("CPL")%></button></li>
        <li class="nav-item" role="presentation"><button class="nav-link fw-bold px-3" data-bs-toggle="tab" data-bs-target="#tabCpmk<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("CPMK")%></button></li>
        <li class="nav-item" role="presentation"><button class="nav-link fw-bold px-3" data-bs-toggle="tab" data-bs-target="#tabDeskripsi<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Pustaka")%></button></li>
        <li class="nav-item" role="presentation"><button class="nav-link fw-bold px-3" data-bs-toggle="tab" data-bs-target="#tabRincian<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Rincian Pertemuan")%></button></li>
        <li class="nav-item" role="presentation"><button class="nav-link fw-bold px-3" data-bs-toggle="tab" data-bs-target="#tabPertemuan<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Pertemuan")%></button></li>
        <li class="nav-item" role="presentation"><button class="nav-link fw-bold px-3 text-info" data-bs-toggle="tab" data-bs-target="#tabRekap<%=rnd%>" type="button" role="tab"><i class="fas fa-clipboard-list me-1"></i><%=Common.getBahasaConfig("Rekap")%></button></li>
        <li class="nav-item" role="presentation"><button class="nav-link fw-bold px-3 text-success" data-bs-toggle="tab" data-bs-target="#tabLaporan<%=rnd%>" type="button" role="tab"><i class="fas fa-chart-bar me-1"></i><%=Common.getBahasaConfig("Laporan")%></button></li>
        <li class="nav-item" role="presentation"><button class="nav-link fw-bold px-3 text-danger" data-bs-toggle="tab" data-bs-target="#tabCetak<%=rnd%>" type="button" role="tab"><i class="fas fa-print me-1"></i><%=Common.getBahasaConfig("Cetak")%></button></li>
        <li class="nav-item" role="presentation"><button class="nav-link fw-bold px-3" data-bs-toggle="tab" data-bs-target="#tabCatatan<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Catatan")%></button></li>
    </ul>

    <form id="formRpsInternal<%=rnd%>" class="mt-3">
        <div class="tab-content" id="rpsTabsContent<%=rnd%>">
            
            <div class="tab-pane fade show active" id="tabIdentitas<%=rnd%>" role="tabpanel">
                <div class="card border-0 shadow-sm rounded-4 mb-4">
                    <div class="card-body p-4">
                        <div class="row g-4">
                            <div class="col-md-6">
                                <h6 class="fw-bold text-secondary mb-3 border-bottom pb-2"><i class="fas fa-book me-2"></i><%=Common.getBahasaConfig("Mata Kuliah")%></h6>
                                <table class="table table-sm table-borderless small mb-3">
                                    <tr><td width="160" class="text-muted"><%=Common.getBahasaConfig("Kode")%></td><td>: <b><%=mk.getKode()%></b></td></tr>
                                    <tr><td class="text-muted"><%=Common.getBahasaConfig("Nama")%></td><td>: <b><%=mk.getNama()%></b></td></tr>
                                    <tr><td class="text-muted"><%=Common.getBahasaConfig("Rumpun MK")%></td><td>: <b><%=mk.getKelompokMatakuliah() != null ? mk.getKelompokMatakuliah().getNama() : "-"%></b></td></tr>
                                    <tr><td class="text-muted"><%=Common.getBahasaConfig("SKS Mata Kuliah")%></td><td>: <b><%=Common.numberFormat.get().format(mk.getSks())%></b></td></tr>
                                    <% if(mk.getSksDiskusi() != null && mk.getSksDiskusi() > 0) { %><tr><td class="text-muted"><%=Common.getBahasaConfig("SKS Tatap Muka")%></td><td>: <b><%=Common.numberFormat.get().format(mk.getSksDiskusi())%></b></td></tr><% } %>
                                    <% if(mk.getSksPraktek() != null && mk.getSksPraktek() > 0) { %><tr><td class="text-muted"><%=Common.getBahasaConfig("SKS Praktikum")%></td><td>: <b><%=Common.numberFormat.get().format(mk.getSksPraktek())%></b></td></tr><% } %>
                                    <% if(mk.getSksPraktekLapangan() != null && mk.getSksPraktekLapangan() > 0) { %><tr><td class="text-muted"><%=Common.getBahasaConfig("SKS Praktikum Lapangan")%></td><td>: <b><%=Common.numberFormat.get().format(mk.getSksPraktekLapangan())%></b></td></tr><% } %>
                                    <% if(mk.getSksSimulasi() != null && mk.getSksSimulasi() > 0) { %><tr><td class="text-muted"><%=Common.getBahasaConfig("SKS Simulasi")%></td><td>: <b><%=Common.numberFormat.get().format(mk.getSksSimulasi())%></b></td></tr><% } %>
                                    <tr><td class="text-muted"><%=Common.getBahasaConfig("Kurikulum")%></td><td>: <b><%=kpm.getKurikulum().getNama()%></b></td></tr>
                                    <tr><td class="text-muted"><%=Common.getBahasaConfig("Program Studi")%></td><td>: <b><%=kpm.getKurikulum().getJurusan() != null ? kpm.getKurikulum().getJurusan().getNama() : "-"%></b></td></tr>
                                    <tr><td class="text-muted"><%=Common.getBahasaConfig("Semester")%></td><td>: <b><%=Common.numberFormat.get().format(kpm.getSemester())%></b></td></tr>
                                </table>

                                <div class="mb-3">
                                    <label class="form-label small fw-bold text-dark"><%=Common.getBahasaConfig("Tanggal Penyusunan")%></label>
                                    <% if (disableForm) { %><div class="form-control form-control-sm border-0 bg-light shadow-none text-secondary"><%=tglPenyusunan.isEmpty() ? "-" : tglPenyusunan%></div><% } else { %><input type="date" class="form-control form-control-sm shadow-sm" id="inpTglPenyusunan<%=rnd%>" value="<%=tglPenyusunan%>"><% } %>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label small fw-bold text-dark"><%=Common.getBahasaConfig("Nilai Minimal Ketercapaian")%></label>
                                    <% if (disableForm) { %><div class="form-control form-control-sm border-0 bg-light shadow-none text-secondary"><%=kpm.getMinimalKetercapaian() != null ? kpm.getMinimalKetercapaian() : 0%></div><% } else { %><input type="number" step="0.01" class="form-control form-control-sm shadow-sm" id="inpMinimal<%=rnd%>" value="<%=kpm.getMinimalKetercapaian() != null ? kpm.getMinimalKetercapaian() : 0%>"><% } %>
                                </div>
                                <div class="mb-3">
                                    <% if (disableForm) { %>
                                        <label class="form-label small fw-bold text-dark"><%=Common.getBahasaConfig("Bobot Penilaian Menggunakan CPMK")%></label>
                                        <div class="form-control form-control-sm border-0 bg-light shadow-none text-secondary"><%=kpm.getNilaiMenggunakanCpmk() != null && kpm.getNilaiMenggunakanCpmk() ? Common.getBahasaConfig("Ya") : Common.getBahasaConfig("Tidak")%></div>
                                    <% } else { %>
                                        <div class="form-check form-switch mt-2">
                                            <input class="form-check-input shadow-sm border-primary" type="checkbox" id="inpNilaiCpmk<%=rnd%>" <%=kpm.getNilaiMenggunakanCpmk() != null && kpm.getNilaiMenggunakanCpmk() ? "checked" : ""%>>
                                            <label class="form-check-label small fw-bold text-dark" for="inpNilaiCpmk<%=rnd%>"><%=Common.getBahasaConfig("Bobot Penilaian Menggunakan CPMK (Tidak ada Sub-CPMK)")%></label>
                                        </div>
                                    <% } %>
                                </div>
                            </div>

                            <div class="col-md-6">
                                <h6 class="fw-bold text-secondary mb-3 border-bottom pb-2"><i class="fas fa-user-edit me-2"></i><%=Common.getBahasaConfig("Otoritas")%></h6>
                                <div class="mb-3">
                                    <label class="form-label small fw-bold text-dark"><%=Common.getBahasaConfig("Pengembang RPS")%></label>
                                    <% if (disableForm) { %><div class="form-control form-control-sm border-0 bg-light shadow-none text-secondary" style="min-height: 55px; white-space: pre-wrap;"><%=kpm.getPengembangRps() != null ? kpm.getPengembangRps() : "-"%></div><% } else { %><textarea class="form-control form-control-sm shadow-sm" id="inpPengembang<%=rnd%>" rows="2"><%=kpm.getPengembangRps() != null ? kpm.getPengembangRps() : ""%></textarea><% } %>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label small fw-bold text-dark"><%=Common.getBahasaConfig("Koordinator")%></label>
                                    <% if (disableForm) { %><div class="form-control form-control-sm border-0 bg-light shadow-none text-secondary" style="min-height: 55px; white-space: pre-wrap;"><%=kpm.getKoordinator() != null ? kpm.getKoordinator() : "-"%></div><% } else { %><textarea class="form-control form-control-sm shadow-sm" id="inpKoordinator<%=rnd%>" rows="2"><%=kpm.getKoordinator() != null ? kpm.getKoordinator() : ""%></textarea><% } %>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label small fw-bold text-dark"><%=Common.getBahasaConfig("Ketua Program Studi")%></label>
                                    <div class="form-control form-control-sm border-0 bg-light shadow-none text-secondary"><%=kaprodiName%></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="tab-pane fade" id="tabProfilLulusan<%=rnd%>" role="tabpanel"><div id="containerPL<%=rnd%>"><div class="text-center py-5"><div class="spinner-border spinner-border-sm text-primary"></div></div></div></div>
            <div class="tab-pane fade" id="tabCpl<%=rnd%>" role="tabpanel"><div id="containerCPL<%=rnd%>"><div class="text-center py-5"><div class="spinner-border spinner-border-sm text-primary"></div></div></div></div>
            <div class="tab-pane fade" id="tabCpmk<%=rnd%>" role="tabpanel"><div id="containerCPMK<%=rnd%>"><div class="text-center py-5"><div class="spinner-border spinner-border-sm text-primary"></div></div></div></div>
            <div class="tab-pane fade" id="tabDeskripsi<%=rnd%>" role="tabpanel"><div id="containerDeskripsi<%=rnd%>"><div class="text-center py-5"><div class="spinner-border spinner-border-sm text-primary"></div></div></div></div>
            <div class="tab-pane fade" id="tabRincian<%=rnd%>" role="tabpanel"><div id="containerRincian<%=rnd%>"><div class="text-center py-5"><div class="spinner-border spinner-border-sm text-primary"></div></div></div></div>
            <div class="tab-pane fade" id="tabPertemuan<%=rnd%>" role="tabpanel"><div id="containerPertemuan<%=rnd%>"><div class="text-center py-5"><div class="spinner-border spinner-border-sm text-primary"></div></div></div></div>
            <div class="tab-pane fade" id="tabRekap<%=rnd%>" role="tabpanel"><div id="containerRekap<%=rnd%>"><div class="text-center py-5"><div class="spinner-border spinner-border-sm text-primary"></div></div></div></div>
            <div class="tab-pane fade" id="tabLaporan<%=rnd%>" role="tabpanel"><div id="containerLaporan<%=rnd%>"><div class="text-center py-5"><div class="spinner-border spinner-border-sm text-primary"></div></div></div></div>
            <div class="tab-pane fade" id="tabCetak<%=rnd%>" role="tabpanel"><div id="containerCetak<%=rnd%>"><div class="text-center py-5"><div class="spinner-border spinner-border-sm text-primary"></div></div></div></div>
            <div class="tab-pane fade" id="tabCatatan<%=rnd%>" role="tabpanel"><div id="containerCatatan<%=rnd%>"><div class="text-center py-5"><div class="spinner-border spinner-border-sm text-primary"></div></div></div></div>

        </div>
    </form>

    <div class="d-flex justify-content-end bg-white p-3 mt-2 rounded-4 shadow-sm border <%=disableForm ? "d-none" : ""%>">
        <button type="button" class="btn btn-success px-4 rounded-pill shadow-sm fw-bold" onclick="window.simpanRpsKeseluruhan<%=variable%>()" id="btnSimpanMainRps<%=rnd%>">
            <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Identitas")%>
        </button>
    </div>
</div>

<script>
    var classModel<%=rnd%> = "<%=KurikulumPunyaMatakuliah.class.getName()%>";
    var rootUrl<%=rnd%> = "<%=Common.ROOT%>";
    var idKpm<%=rnd%> = "<%=kpm.getId()%>";
    var urlParams<%=rnd%> = "<%=urlParams%>";

    window.reloadTabPl<%=variable%> = function() { loadContentIntoContainer(rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_profile_lulusan&kurikulumPunyaMatakuliah=" + idKpm<%=rnd%> + urlParams<%=rnd%>, 'containerPL<%=rnd%>'); };
    window.reloadTabCpl<%=variable%> = function() { loadContentIntoContainer(rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_capaian_lulusan&kurikulumPunyaMatakuliah=" + idKpm<%=rnd%> + urlParams<%=rnd%>, 'containerCPL<%=rnd%>'); };
    window.reloadTabCpmk<%=variable%> = function() { loadContentIntoContainer(rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_capaian_pembelajaran_lulusan&kurikulumPunyaMatakuliah=" + idKpm<%=rnd%> + urlParams<%=rnd%>, 'containerCPMK<%=rnd%>'); };
    window.reloadTabDeskripsi<%=variable%> = function() { loadContentIntoContainer(rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_deskripsi_dan_pustaka&kurikulumPunyaMatakuliah=" + idKpm<%=rnd%> + urlParams<%=rnd%>, 'containerDeskripsi<%=rnd%>'); };
    window.reloadTabCatatan<%=variable%> = function() { loadContentIntoContainer(rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_catatan_obe&kurikulumPunyaMatakuliah=" + idKpm<%=rnd%> + urlParams<%=rnd%>, 'containerCatatan<%=rnd%>'); };
    window.reloadTabRincian<%=variable%> = function() { loadContentIntoContainer(rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_rincian_pertemuan&kurikulumPunyaMatakuliah=" + idKpm<%=rnd%> + urlParams<%=rnd%>, 'containerRincian<%=rnd%>'); };
    window.reloadTabPertemuan<%=variable%> = function() { loadContentIntoContainer(rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_pertemuan&kurikulumPunyaMatakuliah=" + idKpm<%=rnd%> + urlParams<%=rnd%>, 'containerPertemuan<%=rnd%>'); };
    window.reloadTabRekap<%=variable%> = function() { loadContentIntoContainer(rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_rekap&kurikulumPunyaMatakuliah=" + idKpm<%=rnd%> + urlParams<%=rnd%>, 'containerRekap<%=rnd%>'); };
    window.reloadTabLaporan<%=variable%> = function() { loadContentIntoContainer(rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_laporan&kurikulumPunyaMatakuliah=" + idKpm<%=rnd%> + urlParams<%=rnd%>, 'containerLaporan<%=rnd%>'); };
    window.reloadTabCetak<%=variable%> = function() { loadContentIntoContainer(rootUrl<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_cetak&kurikulumPunyaMatakuliah=" + idKpm<%=rnd%> + urlParams<%=rnd%>, 'containerCetak<%=rnd%>'); };

    window['simpanRpsKeseluruhan<%=variable%>'] = async function() {
        var btn = document.getElementById('btnSimpanMainRps<%=rnd%>');
        var oriBtn = btn.innerHTML;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btn.disabled = true;

        var tglVal = document.getElementById('inpTglPenyusunan<%=rnd%>') ? document.getElementById('inpTglPenyusunan<%=rnd%>').value : null;
        var dataObj = {
            pengembangRps: document.getElementById('inpPengembang<%=rnd%>') ? document.getElementById('inpPengembang<%=rnd%>').value.trim() : null,
            koordinator: document.getElementById('inpKoordinator<%=rnd%>') ? document.getElementById('inpKoordinator<%=rnd%>').value.trim() : null,
            minimalKetercapaian: document.getElementById('inpMinimal<%=rnd%>') ? parseFloat(document.getElementById('inpMinimal<%=rnd%>').value) : null,
            nilaiMenggunakanCpmk: document.getElementById('inpNilaiCpmk<%=rnd%>') ? document.getElementById('inpNilaiCpmk<%=rnd%>').checked : false,
            tanggalPenyusunan: tglVal ? tglVal : null
        };

        var payload = { action: "simpanDataRinci", log: "true", class: classModel<%=rnd%>, id: idKpm<%=rnd%>, data: dataObj };
        
        try {
            var response = await fetch(rootUrl<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            var result = await response.json();
            if (result.status === '00' || result.status === 'success' || result.id) {
                tampilkanToast('<%=Common.getBahasaConfigJS("Data Identitas berhasil diperbarui!")%>', 'bg-success text-white');
                var modalEl = btn.closest('.modal');
                if (modalEl) {
                    var modalInstance = bootstrap.Modal.getInstance(modalEl);
                    if (modalInstance) modalInstance.hide(); 
                    else { var newModal = new bootstrap.Modal(modalEl); newModal.hide(); }
                }
                if (typeof window['onSearchDefault'] === 'function') window['onSearchDefault'](null);
                if (typeof window['resetAndLoadData<%=rnd%>'] === 'function') window['resetAndLoadData<%=rnd%>']();
            } else {
                tampilkanToast(result.description || '<%=Common.getBahasaConfigJS("Gagal memproses penyimpanan data.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi jaringan.")%>', 'bg-danger text-white');
        } finally {
            btn.innerHTML = oriBtn;
            btn.disabled = false;
        }
    };

    // Load semua isi tab secara asinkron
    window.reloadTabPl<%=variable%>();
    window.reloadTabCpl<%=variable%>();
    window.reloadTabCpmk<%=variable%>();
    window.reloadTabDeskripsi<%=variable%>();
    window.reloadTabCatatan<%=variable%>();
    window.reloadTabRincian<%=variable%>();
    window.reloadTabPertemuan<%=variable%>();
    window.reloadTabRekap<%=variable%>();
    window.reloadTabLaporan<%=variable%>();
    window.reloadTabCetak<%=variable%>();
</script>