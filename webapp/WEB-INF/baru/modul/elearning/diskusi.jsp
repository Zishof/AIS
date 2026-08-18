<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
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

    List<Fakultas> listFakultas = new ArrayList<Fakultas>();
    List<Jurusan>  listJurusan  = new ArrayList<Jurusan>();
    Session sessF = null;
    try {
        sessF = HibernateUtil.openSession();
        if (apakahPT && !isSivitas) {
            listFakultas = ConstantValues.simpleList(sessF.createCriteria(Fakultas.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("nama")), Fakultas.class);
            listJurusan = ConstantValues.simpleList(sessF.createCriteria(Jurusan.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("nama")), Jurusan.class);
        }
    } catch(Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/diskusi.jsp:39"); }
    finally {
        try { sessF.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/diskusi.jsp:41");}
        ais.common.ElearningSessionUtil.closeQuietly(sessF);
        HibernateUtil.closeSessionQuietly(sessF);
    }

    String defTa  = Common.getCurrentTahunAkademik();
    String defSmt = Common.isNowSemensterGanjil()
        ? String.valueOf(Perkuliahan.GANJIL) : String.valueOf(Perkuliahan.GENAP);
%>
<style>
/* ── DISKUSI TAB <%=rnd%> ─────────────────────────────────────────── */
.d-card-<%=rnd%> { border-radius:14px; border:1px solid #e9ecef; background:#fff;
    transition:.2s; overflow:hidden; }
.d-card-<%=rnd%>:hover { box-shadow:0 4px 18px rgba(0,0,0,.08); transform:translateY(-2px); }
.d-badge-<%=rnd%> { min-width:32px; height:32px; border-radius:99px;
    background:linear-gradient(135deg,#0d6efd,#0a58ca);
    color:#fff; font-weight:700; font-size:.8rem;
    display:inline-flex; align-items:center; justify-content:center; }
.d-badge-tutup-<%=rnd%> { background:linear-gradient(135deg,#6c757d,#495057); }
.d-input-<%=rnd%> { border-radius:10px; border:1.5px solid #dee2e6; font-size:.875rem;
    transition:.2s; background:#fafafa; }
.d-input-<%=rnd%>:focus { border-color:#0d6efd; box-shadow:0 0 0 .2rem rgba(13,110,253,.12); background:#fff; }
.d-skel-<%=rnd%> { border-radius:8px;
    background:linear-gradient(90deg,#f0f0f0 25%,#e0e0e0 50%,#f0f0f0 75%);
    background-size:200% 100%; animation:dSkelAnim<%=rnd%> 1.4s infinite; }
@keyframes dSkelAnim<%=rnd%> { 0%{background-position:200% 0} 100%{background-position:-200% 0} }
@media (max-width:767.98px) {
    #wrapper-diskusi-<%=rnd%> .row.g-2 > [class*="col-"] { padding:.2rem; }
    #wrapper-diskusi-<%=rnd%> .d-flex.justify-content-end { justify-content:stretch !important; }
    #wrapper-diskusi-<%=rnd%> .d-flex.justify-content-end .btn { flex:1 1 0; }
    .d-input-<%=rnd%>, select.d-input-<%=rnd%> { font-size:16px !important; }
}
</style>

<div id="wrapper-diskusi-<%=rnd%>" class="container-fluid py-3 px-3 px-xl-4">

    <!-- HEADER -->
    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
        <div class="d-flex align-items-center gap-3">
            <div class="rounded-4 d-flex align-items-center justify-content-center shadow-sm flex-shrink-0"
                 style="width:50px;height:50px;background:linear-gradient(135deg,#0d6efd,#0a58ca);">
                <i class="fas fa-comments text-white fs-5"></i>
            </div>
            <div>
                <div class="small fw-bold text-primary text-uppercase" style="letter-spacing:1px;"><%=Common.getBahasaConfig("Forum")%></div>
                <h5 class="fw-bold text-dark mb-0"><%=Common.getBahasaConfig("Diskusi Kelas — Semua Pertemuan")%></h5>
            </div>
        </div>
        <button class="btn btn-sm btn-primary rounded-pill fw-bold px-4 shadow-sm"
                onclick="diskusiLoad<%=rnd%>(true)">
            <i class="fas fa-sync-alt me-2" id="d-ricon-<%=rnd%>"></i><%=Common.getBahasaConfig("Segarkan")%>
        </button>
    </div>

    <!-- FILTER -->
    <div class="card border-0 shadow-sm rounded-4 p-3 mb-3">
        <div class="row g-2 align-items-end">
            <div class="col-6 col-md-2">
                <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Tahun Akademik")%></label>
                <select id="d-ta-<%=rnd%>" class="form-select d-input-<%=rnd%>" onchange="diskusiLoad<%=rnd%>(true)">
                    <option value="">— <%=Common.getBahasaConfig("Semua")%> —</option>
                    <% if (Common.tahunAngkatans != null) {
                        for (String ta : Common.tahunAngkatans) {
                            String s = (ta != null && ta.equals(defTa)) ? "selected" : ""; %>
                    <option value="<%=ta%>" <%=s%>><%=ta%></option>
                    <% }} %>
                </select>
            </div>
            <div class="col-6 col-md-2">
                <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Semester")%></label>
                <select id="d-smt-<%=rnd%>" class="form-select d-input-<%=rnd%>" onchange="diskusiLoad<%=rnd%>(true)">
                    <option value="">— <%=Common.getBahasaConfig("Semua")%> —</option>
                    <option value="<%=Perkuliahan.GANJIL%>" <%=Perkuliahan.GANJIL.equals(defSmt)?"selected":""%>><%=Common.getBahasaConfig("Ganjil")%></option>
                    <option value="<%=Perkuliahan.GENAP%>"  <%=Perkuliahan.GENAP.equals(defSmt)?"selected":""%>><%=Common.getBahasaConfig("Genap")%></option>
                    <option value="<%=Perkuliahan.SP%>"><%=Common.getBahasaConfig("Semester Pendek")%></option>
                </select>
            </div>
            <% if (apakahPT && !isSivitas) { %>
            <div class="col-md-3">
                <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Fakultas")%></label>
                <select id="d-fak-<%=rnd%>" class="form-select d-input-<%=rnd%>" onchange="diskusiUbahFak<%=rnd%>()">
                    <option value="">— <%=Common.getBahasaConfig("Semua")%> —</option>
                    <% for(Fakultas f : listFakultas) { %><option value="<%=f.getId()%>"><%=f.getNama()%></option><% } %>
                </select>
            </div>
            <div class="col-md-3">
                <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Jurusan")%></label>
                <select id="d-jur-<%=rnd%>" class="form-select d-input-<%=rnd%>" onchange="diskusiLoad<%=rnd%>(true)">
                    <option value="">— <%=Common.getBahasaConfig("Semua")%> —</option>
                    <% for(Jurusan j : listJurusan) { %><option value="<%=j.getId()%>"><%=j.getNama()%></option><% } %>
                </select>
            </div>
            <% } %>
            <div class="col-md-2">
                <label class="form-label small fw-bold text-muted mb-1"><%=Common.getBahasaConfig("Matakuliah/Pelajaran")%></label>
                <input type="text" id="d-mk-<%=rnd%>" class="form-control d-input-<%=rnd%>"
                    placeholder="<%=Common.getBahasaConfig("Cari...")%>"
                    oninput="clearTimeout(window.dTimeout<%=rnd%>); window.dTimeout<%=rnd%>=setTimeout(function(){diskusiLoad<%=rnd%>(true);},600);">
            </div>
        </div>
    </div>

    <!-- CONTAINER DISKUSI -->
    <div id="d-container-<%=rnd%>">
        <div class="text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
            <div class="mt-2 small fw-bold text-muted"><%=Common.getBahasaConfig("Sedang memuat daftar diskusi...")%></div>
        </div>
    </div>

    <!-- LOAD MORE -->
    <div class="text-center mt-3 mb-5" id="d-loadmore-<%=rnd%>" style="display:none;">
        <button class="btn btn-outline-primary rounded-pill px-5 fw-bold shadow-sm"
                onclick="diskusiLoadMore<%=rnd%>()">
            <i class="fas fa-chevron-down me-2"></i><%=Common.getBahasaConfig("Muat Lebih Banyak")%>
            <span class="badge bg-primary ms-2" id="d-sisa-<%=rnd%>">0</span>
        </button>
    </div>
</div>

<script>
(function() {
    var ROOT_<%=rnd%>   = "<%=Common.ROOT%>";
    var offset<%=rnd%>  = 0;
    var LIMIT_<%=rnd%>  = 20;

    var gv<%=rnd%> = function(id) {
        var el = document.getElementById(id); return el ? el.value.trim() : "";
    };

    var buildQuery<%=rnd%> = function() {
        var q = "";
        q += "&q_ta="         + encodeURIComponent(gv<%=rnd%>("d-ta-<%=rnd%>"));
        q += "&q_smt="        + encodeURIComponent(gv<%=rnd%>("d-smt-<%=rnd%>"));
        q += "&q_matakuliah=" + encodeURIComponent(gv<%=rnd%>("d-mk-<%=rnd%>"));
        <% if (apakahPT && !isSivitas) { %>
        q += "&q_fakultas=" + encodeURIComponent(gv<%=rnd%>("d-fak-<%=rnd%>"));
        q += "&q_jurusan="  + encodeURIComponent(gv<%=rnd%>("d-jur-<%=rnd%>"));
        <% } %>
        return q;
    };

    window.diskusiUbahFak<%=rnd%> = function() {
        var fId = gv<%=rnd%>("d-fak-<%=rnd%>");
        var jSel = document.getElementById("d-jur-<%=rnd%>");
        if (!jSel) return;
        var jurData = [<% for(Jurusan j : listJurusan) { Long fId2 = (j.getFakultas()!=null)?j.getFakultas().getId():0L; %>
            {id:"<%=j.getId()%>",nama:<%=new org.json.JSONObject().put("v",j.getNama()).toString().replace("{\"v\":","").replace("}","")%>,fak:"<%=fId2%>"},
        <% } %>];
        jSel.innerHTML = '<option value="">— <%=Common.getBahasaConfig("Semua")%> —</option>';
        jurData.forEach(function(j) {
            if (!fId || j.fak === fId) {
                var o = document.createElement("option"); o.value = j.id; o.text = j.nama; jSel.add(o);
            }
        });
        diskusiLoad<%=rnd%>(true);
    };

    function renderSkeleton<%=rnd%>() {
        var html = "";
        for (var i = 0; i < 4; i++) {
            html += '<div class="d-card-<%=rnd%> p-3 mb-3"><div class="d-flex align-items-center gap-3">'
                  + '<div class="d-skel-<%=rnd%>" style="width:36px;height:36px;border-radius:50%;flex-shrink:0"></div>'
                  + '<div class="flex-grow-1"><div class="d-skel-<%=rnd%> mb-2" style="height:14px;width:60%"></div>'
                  + '<div class="d-skel-<%=rnd%>" style="height:12px;width:40%"></div></div></div></div>';
        }
        return html;
    }

    function renderKartu<%=rnd%>(item) {
        var ikoBadge = item.komentarDitutup
            ? '<span class="badge bg-danger ms-2" title="Diskusi Ditutup"><i class="fas fa-lock"></i></span>'
            : '<span class="badge bg-success ms-2"><i class="fas fa-lock-open"></i></span>';
        var linkDiskusi = ROOT_<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning&s=_diskusi&idPertemuan=" + item.id;
        var html = '<div class="d-card-<%=rnd%> p-3 mb-3 fade-in">'
            + '<div class="d-flex align-items-start justify-content-between gap-2 flex-wrap">'
            + '<div class="d-flex align-items-center gap-3 flex-grow-1">'
            + '<div class="d-badge-<%=rnd%> ' + (item.komentarDitutup ? 'd-badge-tutup-<%=rnd%>' : '') + ' flex-shrink-0">'
            + '<i class="fas fa-comments"></i></div>'
            + '<div class="overflow-hidden">'
            + '<div class="fw-bold text-dark" style="font-size:.92rem">'
            + (item.namaKelas ? '<span class="text-primary me-2 me-1">' + item.namaKelas + '</span> ' : '')
            + (item.topik || '<%=Common.getBahasaConfigJS("Pertemuan ke-")%>' + item.ke)
            + ikoBadge + '</div>'
            + '<div class="text-muted" style="font-size:.78rem">'
            + '<%=Common.getBahasaConfigJS("Pertemuan ke-")%> ' + item.ke
            + (item.tgl ? ' &bull; ' + item.tgl : '')
            + (item.pengajar ? ' &bull; ' + item.pengajar : '')
            + '</div></div></div>'
            + '<div class="d-flex align-items-center gap-2 flex-shrink-0">'
            + '<span class="badge bg-primary rounded-pill px-3 py-2" style="font-size:.8rem">'
            + '<i class="fas fa-comment-dots me-1"></i>' + item.jumlahDiskusi + '</span>'
            + '<button class="btn btn-sm btn-outline-primary rounded-pill fw-bold" '
            + 'onclick="loadModalContentCustomSimpan(\'<%=Common.getBahasaConfigJS("Forum Diskusi")%>\',\'' + linkDiskusi + '\',\'85%\',\'hidden\',\'\',\'cm_\'+Date.now())">'
            + '<i class="far fa-comments me-1"></i><%=Common.getBahasaConfig("Buka Diskusi")%></button>'
            + '</div></div></div>';
        return html;
    }

    window.diskusiLoad<%=rnd%> = function(isReset) {
        if (isReset) offset<%=rnd%> = 0;
        var cont = document.getElementById("d-container-<%=rnd%>");
        var btn  = document.getElementById("d-loadmore-<%=rnd%>");
        if (isReset && cont) cont.innerHTML = renderSkeleton<%=rnd%>();
        if (btn) btn.style.display = "none";
        var icon = document.getElementById("d-ricon-<%=rnd%>");
        if (icon) { icon.classList.add("fa-spin"); }

        var url = ROOT_<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_diskusi_lintas_service"
                + "&mulai=" + offset<%=rnd%> + "&banyak=" + LIMIT_<%=rnd%> + buildQuery<%=rnd%>();

        fetch(url).then(function(r) { return r.json(); }).then(function(resp) {
            if (icon) icon.classList.remove("fa-spin");
            if (!cont) return;
            if (resp.status !== "ok") {
                cont.innerHTML = '<div class="text-center py-5 text-danger"><i class="fas fa-exclamation-triangle fa-2x mb-3"></i><br><%=Common.getBahasaConfig("Gagal memuat data diskusi.")%></div>';
                return;
            }
            var items = resp.data;
            if (isReset) cont.innerHTML = "";
            if (!items || items.length === 0) {
                if (isReset) cont.innerHTML = '<div class="text-center py-5 text-muted"><i class="fas fa-comments fa-3x mb-3 opacity-25"></i><br><strong><%=Common.getBahasaConfig("Belum ada diskusi yang tersedia")%></strong><br><small><%=Common.getBahasaConfig("Diskusi muncul setelah ada komentar di salah satu pertemuan.")%></small></div>';
                return;
            }
            var html = "";
            for (var i = 0; i < items.length; i++) html += renderKartu<%=rnd%>(items[i]);
            cont.insertAdjacentHTML("beforeend", html);
            offset<%=rnd%> += items.length;

            var sisa = resp.sisaTampil || 0;
            if (sisa > 0 && btn) {
                document.getElementById("d-sisa-<%=rnd%>").textContent = sisa;
                btn.style.display = "";
            }
        }).catch(function(err) {
            if (icon) icon.classList.remove("fa-spin");
            console.error(err);
        });
    };

    window.diskusiLoadMore<%=rnd%> = function() { diskusiLoad<%=rnd%>(false); };

    // Auto-load saat tab aktif pertama kali
    diskusiLoad<%=rnd%>(true);
})();
</script>
