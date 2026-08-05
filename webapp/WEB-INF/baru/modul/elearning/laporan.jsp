<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.CriteriaSpecification"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String rnd = Common.getGeneratedBarCode(7);
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) { response.setStatus(401); return; }

    boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
    boolean apakahPT = ptYa[0];
    boolean isSivitas = tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null
                     || tbmuser.getSiswa() != null || tbmuser.ambilGuru() != null;
    boolean isDosen  = tbmuser.ambilDosen() != null;

    String defTa  = Common.getCurrentTahunAkademik();
    String defSmt = Common.isNowSemensterGanjil()
        ? String.valueOf(Perkuliahan.GANJIL) : String.valueOf(Perkuliahan.GENAP);

    // Daftar perkuliahan yang bisa dicetak nilainya
    List<Perkuliahan> listPerkuliahan = new ArrayList<Perkuliahan>();
    List<Fakultas>    listFakultas    = new ArrayList<Fakultas>();
    List<Jurusan>     listJurusan     = new ArrayList<Jurusan>();

    Session sessL = null;
    try {
        sessL = HibernateUtil.openSession();
        if (apakahPT && !isSivitas) {
            listFakultas = ConstantValues.simpleList(sessL.createCriteria(Fakultas.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("nama")), Fakultas.class);
            listJurusan = ConstantValues.simpleList(sessL.createCriteria(Jurusan.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("nama")), Jurusan.class);
        }
        if (isDosen) {
            Dosen dosen = tbmuser.ambilDosen();
            listPerkuliahan = sessL.createCriteria(Perkuliahan.class)
                .add(Restrictions.eq("ta", defTa))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.like("dosens", "%," + dosen.getId() + ",%"))
                .addOrder(Order.asc("semester"))
                .list();
        }
    } catch(Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/laporan.jsp:62"); }
    finally {
        try { if (sessL != null) { sessL.disconnect(); sessL.close(); } } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/laporan.jsp:64");}
        HibernateUtil.closeSessionQuietly(sessL);
    }
%>
<style>
/* ── LAPORAN TAB <%=rnd%> ───────────────────────────────────────────── */
.l-card-<%=rnd%> { border-radius:14px; border:1px solid #e9ecef; background:#fff;
    overflow:hidden; transition:.2s; }
.l-card-<%=rnd%>:hover { box-shadow:0 4px 18px rgba(0,0,0,.09); transform:translateY(-2px); }
.l-icon-<%=rnd%> { width:48px; height:48px; border-radius:12px; display:flex;
    align-items:center; justify-content:center; font-size:1.2rem; flex-shrink:0; }
.l-select-<%=rnd%> { border-radius:10px; border:1.5px solid #dee2e6; font-size:.875rem;
    transition:.2s; background:#fafafa; }
.l-select-<%=rnd%>:focus { border-color:#198754; box-shadow:0 0 0 .2rem rgba(25,135,84,.12); background:#fff; }
@media (max-width:767.98px) {
    .l-select-<%=rnd%>, select.l-select-<%=rnd%>, input.l-select-<%=rnd%> { font-size:16px !important; }
    #wrapper-laporan-<%=rnd%> .btn { min-height:38px; }
}
</style>

<div id="wrapper-laporan-<%=rnd%>" class="container-fluid py-3 px-3 px-xl-4">

    <!-- HEADER -->
    <div class="d-flex align-items-center gap-3 mb-4">
        <div class="rounded-4 d-flex align-items-center justify-content-center shadow-sm flex-shrink-0"
             style="width:50px;height:50px;background:linear-gradient(135deg,#198754,#146c43);">
            <i class="fas fa-file-alt text-white fs-5"></i>
        </div>
        <div>
            <div class="small fw-bold text-success text-uppercase" style="letter-spacing:1px;"><%=Common.getBahasaConfig("Cetak & Ekspor")%></div>
            <h5 class="fw-bold text-dark mb-0"><%=Common.getBahasaConfig("Laporan E-Learning")%></h5>
        </div>
    </div>

    <%-- ─── KELOMPOK 1: Nilai & Ujian ──────────────────────────────────── --%>
    <div class="mb-4">
        <h6 class="fw-bold text-success border-bottom pb-2 mb-3">
            <i class="fas fa-clipboard-list me-2"></i><%=Common.getBahasaConfig("Nilai & Ujian")%>
        </h6>
        <div class="row g-3">

            <%-- Cetak Daftar Nilai --%>
            <div class="col-lg-6">
                <div class="l-card-<%=rnd%> p-3">
                    <div class="d-flex align-items-start gap-3 mb-3">
                        <div class="l-icon-<%=rnd%>" style="background:rgba(220,53,69,.1);color:#dc3545;">
                            <i class="fas fa-file-pdf"></i>
                        </div>
                        <div>
                            <div class="fw-bold text-dark"><%=Common.getBahasaConfig("Cetak Daftar Nilai")%></div>
                            <div class="text-muted" style="font-size:.8rem;"><%=Common.getBahasaConfig("PDF daftar nilai ujian per matakuliah")%></div>
                        </div>
                    </div>
                    <div class="row g-2 mb-2">
                        <div class="col-5">
                            <select id="l-nilai-ta-<%=rnd%>" class="form-select l-select-<%=rnd%>" onchange="lapMuatPerkuliahan<%=rnd%>()">
                                <% if (Common.tahunAngkatans != null) {
                                    for (String ta : Common.tahunAngkatans) {
                                        String s = (ta != null && ta.equals(defTa)) ? "selected" : ""; %>
                                <option value="<%=ta%>" <%=s%>><%=ta%></option>
                                <% }} %>
                            </select>
                        </div>
                        <div class="col-4">
                            <select id="l-nilai-smt-<%=rnd%>" class="form-select l-select-<%=rnd%>" onchange="lapMuatPerkuliahan<%=rnd%>()">
                                <option value="<%=Perkuliahan.GANJIL%>" <%=Perkuliahan.GANJIL.equals(defSmt)?"selected":""%>><%=Common.getBahasaConfig("Ganjil")%></option>
                                <option value="<%=Perkuliahan.GENAP%>"  <%=Perkuliahan.GENAP.equals(defSmt)?"selected":""%>><%=Common.getBahasaConfig("Genap")%></option>
                                <option value="<%=Perkuliahan.SP%>"><%=Common.getBahasaConfig("SP")%></option>
                            </select>
                        </div>
                    </div>
                    <div class="mb-2">
                        <select id="l-nilai-prk-<%=rnd%>" class="form-select l-select-<%=rnd%>">
                            <option value="">— <%=Common.getBahasaConfig("Pilih Matakuliah")%> —</option>
                            <% for(Perkuliahan p : listPerkuliahan) {
                                String mk = (p.getMatakuliah()!=null) ? p.getMatakuliah().getNama() : "";
                                String kls = (p.getKelas()!=null) ? " ["+p.getKelas()+"]" : ""; %>
                            <option value="<%=p.getId()%>"><%=mk%><%=kls%></option>
                            <% } %>
                        </select>
                    </div>
                    <button class="btn btn-sm btn-danger fw-bold rounded-pill px-4 w-100"
                            onclick="lapCetakNilai<%=rnd%>()">
                        <i class="fas fa-print me-2"></i><%=Common.getBahasaConfig("Cetak PDF Nilai")%>
                    </button>
                </div>
            </div>

            <%-- Export Ujian ke Excel --%>
            <div class="col-lg-6">
                <div class="l-card-<%=rnd%> p-3">
                    <div class="d-flex align-items-start gap-3 mb-3">
                        <div class="l-icon-<%=rnd%>" style="background:rgba(25,135,84,.1);color:#198754;">
                            <i class="fas fa-file-excel"></i>
                        </div>
                        <div>
                            <div class="fw-bold text-dark"><%=Common.getBahasaConfig("Export Nilai Ujian ke Excel")%></div>
                            <div class="text-muted" style="font-size:.8rem;"><%=Common.getBahasaConfig("Ekspor jawaban & nilai ujian dalam format Excel")%></div>
                        </div>
                    </div>
                    <div class="mb-2">
                        <select id="l-ujian-prk-<%=rnd%>" class="form-select l-select-<%=rnd%>">
                            <option value="">— <%=Common.getBahasaConfig("Pilih Matakuliah")%> —</option>
                            <% for(Perkuliahan p : listPerkuliahan) {
                                String mk = (p.getMatakuliah()!=null) ? p.getMatakuliah().getNama() : "";
                                String kls = (p.getKelas()!=null) ? " ["+p.getKelas()+"]" : ""; %>
                            <option value="<%=p.getId()%>"><%=mk%><%=kls%></option>
                            <% } %>
                        </select>
                    </div>
                    <button class="btn btn-sm btn-success fw-bold rounded-pill px-4 w-100"
                            onclick="lapExportUjian<%=rnd%>()">
                        <i class="fas fa-file-download me-2"></i><%=Common.getBahasaConfig("Download Excel Ujian")%>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <%-- ─── KELOMPOK 2: Kehadiran & Pertemuan ─────────────────────────── --%>
    <div class="mb-4">
        <h6 class="fw-bold text-primary border-bottom pb-2 mb-3">
            <i class="fas fa-user-check me-2"></i><%=Common.getBahasaConfig("Kehadiran & Pertemuan")%>
        </h6>
        <div class="row g-3">

            <%-- Daftar Peserta & Absensi --%>
            <div class="col-lg-6">
                <div class="l-card-<%=rnd%> p-3">
                    <div class="d-flex align-items-start gap-3 mb-3">
                        <div class="l-icon-<%=rnd%>" style="background:rgba(13,110,253,.1);color:#0d6efd;">
                            <i class="fas fa-users"></i>
                        </div>
                        <div>
                            <div class="fw-bold text-dark"><%=Common.getBahasaConfig("Rekap Kehadiran Mahasiswa")%></div>
                            <div class="text-muted" style="font-size:.8rem;"><%=Common.getBahasaConfig("Daftar peserta dan rekap absensi per perkuliahan")%></div>
                        </div>
                    </div>
                    <div class="mb-2">
                        <select id="l-abs-prk-<%=rnd%>" class="form-select l-select-<%=rnd%>">
                            <option value="">— <%=Common.getBahasaConfig("Pilih Matakuliah")%> —</option>
                            <% for(Perkuliahan p : listPerkuliahan) {
                                String mk = (p.getMatakuliah()!=null) ? p.getMatakuliah().getNama() : "";
                                String kls = (p.getKelas()!=null) ? " ["+p.getKelas()+"]" : ""; %>
                            <option value="<%=p.getId()%>"><%=mk%><%=kls%></option>
                            <% } %>
                        </select>
                    </div>
                    <div class="d-flex gap-2">
                        <button class="btn btn-sm btn-primary fw-bold rounded-pill px-3 flex-fill"
                                onclick="lapBukaAbsensi<%=rnd%>()">
                            <i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Lihat Rekap")%>
                        </button>
                    </div>
                </div>
            </div>

            <%-- Analisis Butir Soal --%>
            <div class="col-lg-6">
                <div class="l-card-<%=rnd%> p-3">
                    <div class="d-flex align-items-start gap-3 mb-3">
                        <div class="l-icon-<%=rnd%>" style="background:rgba(253,126,20,.1);color:#fd7e14;">
                            <i class="fas fa-chart-bar"></i>
                        </div>
                        <div>
                            <div class="fw-bold text-dark"><%=Common.getBahasaConfig("Analisis Butir Soal")%></div>
                            <div class="text-muted" style="font-size:.8rem;"><%=Common.getBahasaConfig("Statistik daya beda, tingkat kesulitan, dan validitas soal ujian")%></div>
                        </div>
                    </div>
                    <div class="mb-2">
                        <input type="text" id="l-analisis-idujian-<%=rnd%>" class="form-control l-select-<%=rnd%>"
                            placeholder="<%=Common.getBahasaConfig("ID Ujian (dari tab Ujian)")%>">
                    </div>
                    <button class="btn btn-sm btn-warning fw-bold rounded-pill px-4 w-100"
                            onclick="lapAnalisisSoal<%=rnd%>()">
                        <i class="fas fa-search-plus me-2"></i><%=Common.getBahasaConfig("Analisis Soal")%>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <%-- ─── KELOMPOK 3: OBE & RPS ──────────────────────────────────────── --%>
    <div class="mb-4">
        <h6 class="fw-bold text-success border-bottom pb-2 mb-3">
            <i class="fas fa-graduation-cap me-2"></i><%=Common.getBahasaConfig("OBE & RPS")%>
        </h6>
        <div class="row g-3">
            <div class="col-lg-6">
                <div class="l-card-<%=rnd%> p-3">
                    <div class="d-flex align-items-start gap-3 mb-3">
                        <div class="l-icon-<%=rnd%>" style="background:rgba(25,135,84,.1);color:#198754;">
                            <i class="fas fa-award"></i>
                        </div>
                        <div>
                            <div class="fw-bold text-dark"><%=Common.getBahasaConfig("Rekap Nilai OBE (CPMK)")%></div>
                            <div class="text-muted" style="font-size:.8rem;"><%=Common.getBahasaConfig("Capaian Pembelajaran Mata Kuliah per mahasiswa")%></div>
                        </div>
                    </div>
                    <div class="mb-2">
                        <select id="l-obe-prk-<%=rnd%>" class="form-select l-select-<%=rnd%>">
                            <option value="">— <%=Common.getBahasaConfig("Pilih Matakuliah")%> —</option>
                            <% for(Perkuliahan p : listPerkuliahan) {
                                String mk = (p.getMatakuliah()!=null) ? p.getMatakuliah().getNama() : "";
                                String kls = (p.getKelas()!=null) ? " ["+p.getKelas()+"]" : ""; %>
                            <option value="<%=p.getId()%>"><%=mk%><%=kls%></option>
                            <% } %>
                        </select>
                    </div>
                    <button class="btn btn-sm btn-success fw-bold rounded-pill px-4 w-100"
                            onclick="lapBukaObeRekap<%=rnd%>()">
                        <i class="fas fa-chart-line me-2"></i><%=Common.getBahasaConfig("Lihat Rekap OBE")%>
                    </button>
                </div>
            </div>
            <div class="col-lg-6">
                <div class="l-card-<%=rnd%> p-3">
                    <div class="d-flex align-items-start gap-3 mb-3">
                        <div class="l-icon-<%=rnd%>" style="background:rgba(13,110,253,.1);color:#0d6efd;">
                            <i class="fas fa-book-open"></i>
                        </div>
                        <div>
                            <div class="fw-bold text-dark"><%=Common.getBahasaConfig("Cetak RPS / Bahan Ajar")%></div>
                            <div class="text-muted" style="font-size:.8rem;"><%=Common.getBahasaConfig("Rencana Pembelajaran Semester versi cetak")%></div>
                        </div>
                    </div>
                    <div class="mb-2">
                        <select id="l-rps-prk-<%=rnd%>" class="form-select l-select-<%=rnd%>">
                            <option value="">— <%=Common.getBahasaConfig("Pilih Matakuliah")%> —</option>
                            <% for(Perkuliahan p : listPerkuliahan) {
                                String mk = (p.getMatakuliah()!=null) ? p.getMatakuliah().getNama() : "";
                                String kls = (p.getKelas()!=null) ? " ["+p.getKelas()+"]" : ""; %>
                            <option value="<%=p.getId()%>"><%=mk%><%=kls%></option>
                            <% } %>
                        </select>
                    </div>
                    <button class="btn btn-sm btn-primary fw-bold rounded-pill px-4 w-100"
                            onclick="lapBukaRps<%=rnd%>()">
                        <i class="fas fa-print me-2"></i><%=Common.getBahasaConfig("Buka RPS")%>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <%-- Pesan jika bukan dosen --%>
    <% if (!isDosen) { %>
    <div class="alert alert-info rounded-4 mt-3">
        <i class="fas fa-info-circle me-2"></i>
        <%=Common.getBahasaConfig("Pilih matakuliah yang Anda ampu melalui filter di atas. Laporan cetak nilai memerlukan hak akses Dosen.")%>
    </div>
    <% } %>
</div>

<script>
(function() {
    var ROOT_<%=rnd%> = "<%=Common.ROOT%>";

    window.lapMuatPerkuliahan<%=rnd%> = function() {
        var ta  = document.getElementById("l-nilai-ta-<%=rnd%>").value;
        var smt = document.getElementById("l-nilai-smt-<%=rnd%>").value;
        var url = ROOT_<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_ujian_service&aksi=listPerkuliahan&q_ta=" + encodeURIComponent(ta) + "&q_smt=" + encodeURIComponent(smt);
        fetch(url).then(function(r) { return r.json(); }).then(function(resp) {
            var sels = ["l-nilai-prk-<%=rnd%>","l-ujian-prk-<%=rnd%>","l-abs-prk-<%=rnd%>","l-obe-prk-<%=rnd%>","l-rps-prk-<%=rnd%>"];
            var blankOpt = '<option value="">— <%=Common.getBahasaConfig("Pilih Matakuliah")%> —</option>';
            var opts = blankOpt;
            if (resp.data) {
                resp.data.forEach(function(p) {
                    opts += '<option value="' + p.id + '">' + (p.namaMk || '') + (p.kelas ? ' ['+p.kelas+']' : '') + '</option>';
                });
            }
            sels.forEach(function(id) {
                var el = document.getElementById(id);
                if (el) el.innerHTML = opts;
            });
        }).catch(function(e) { console.error(e); });
    };

    window.lapCetakNilai<%=rnd%> = function() {
        var idPrk = document.getElementById("l-nilai-prk-<%=rnd%>").value;
        if (!idPrk) { if(typeof tampilkanToast==='function') tampilkanToast('<%=Common.getBahasaConfigJS("Pilih matakuliah terlebih dahulu.")%>','bg-warning'); else alert('<%=Common.getBahasaConfigJS("Pilih matakuliah terlebih dahulu.")%>'); return; }
        var url = ROOT_<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_cetak_laporan_nilai&perkuliahanId=" + idPrk;
        fetch(url).then(function(r) { return r.json(); }).then(function(resp) {
            if ((resp.status === "ok" || resp.status === "success") && resp.url) { window.open(resp.url, "_blank"); }
            else { if(typeof tampilkanToast==='function') tampilkanToast(resp.message || '<%=Common.getBahasaConfigJS("Gagal mencetak laporan.")%>','bg-danger'); else alert(resp.message || '<%=Common.getBahasaConfigJS("Gagal mencetak laporan.")%>'); }
        }).catch(function(e){ console.error(e); });
    };

    window.lapExportUjian<%=rnd%> = function() {
        var idPrk = document.getElementById("l-ujian-prk-<%=rnd%>").value;
        if (!idPrk) { if(typeof tampilkanToast==='function') tampilkanToast('<%=Common.getBahasaConfigJS("Pilih matakuliah terlebih dahulu.")%>','bg-warning'); else alert('<%=Common.getBahasaConfigJS("Pilih matakuliah terlebih dahulu.")%>'); return; }
        window.open(ROOT_<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_export_ujian_ke_excel_services&perkuliahanId=" + idPrk, "_blank");
    };

    window.lapBukaAbsensi<%=rnd%> = function() {
        var idPrk = document.getElementById("l-abs-prk-<%=rnd%>").value;
        if (!idPrk) { if(typeof tampilkanToast==='function') tampilkanToast('<%=Common.getBahasaConfigJS("Pilih matakuliah terlebih dahulu.")%>','bg-warning'); else alert('<%=Common.getBahasaConfigJS("Pilih matakuliah terlebih dahulu.")%>'); return; }
        var url = ROOT_<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning&s=daftar_peserta_absen&perkuliahanId=" + idPrk;
        loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Rekap Kehadiran")%>', url, '90%', 'hidden', '', 'cm_' + Date.now());
    };

    window.lapAnalisisSoal<%=rnd%> = function() {
        var idUjian = document.getElementById("l-analisis-idujian-<%=rnd%>").value.trim();
        if (!idUjian) { if(typeof tampilkanToast==='function') tampilkanToast('<%=Common.getBahasaConfigJS("Masukkan ID Ujian.")%>','bg-warning'); else alert('<%=Common.getBahasaConfigJS("Masukkan ID Ujian.")%>'); return; }
        var url = ROOT_<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_analisis_butir_soal_services&ujianId=" + encodeURIComponent(idUjian);
        loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Analisis Butir Soal")%>', url, '90%', 'hidden', '', 'cm_' + Date.now());
    };

    window.lapBukaObeRekap<%=rnd%> = function() {
        var idPrk = document.getElementById("l-obe-prk-<%=rnd%>").value;
        if (!idPrk) { if(typeof tampilkanToast==='function') tampilkanToast('<%=Common.getBahasaConfigJS("Pilih matakuliah terlebih dahulu.")%>','bg-warning'); else alert('<%=Common.getBahasaConfigJS("Pilih matakuliah terlebih dahulu.")%>'); return; }
        var url = ROOT_<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_obe_service&aksi=rekap&perkuliahanId=" + idPrk;
        loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Rekap OBE")%>', url, '90%', 'hidden', '', 'cm_' + Date.now());
    };

    window.lapBukaRps<%=rnd%> = function() {
        var idPrk = document.getElementById("l-rps-prk-<%=rnd%>").value;
        if (!idPrk) { if(typeof tampilkanToast==='function') tampilkanToast('<%=Common.getBahasaConfigJS("Pilih matakuliah terlebih dahulu.")%>','bg-warning'); else alert('<%=Common.getBahasaConfigJS("Pilih matakuliah terlebih dahulu.")%>'); return; }
        var url = ROOT_<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning&s=buat_pertemuan&perkuliahanId=" + idPrk;
        loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("RPS - Rencana Pembelajaran Semester")%>', url, '95%', 'auto', '', 'cm_' + Date.now());
    };
})();
</script>
