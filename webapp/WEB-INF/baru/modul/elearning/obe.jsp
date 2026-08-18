<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String rnd = Common.getGeneratedBarCode(7);
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) { response.setStatus(401); return; }

    boolean isSivitas = tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null
                     || tbmuser.getSiswa() != null || tbmuser.ambilGuru() != null;
    boolean isMhs = tbmuser.getMahasiswa() != null;
    boolean isDsn = tbmuser.ambilDosen() != null;
    boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
    boolean apakahPerguruanTinggi = ptYa[0];

    String defTa  = Common.getCurrentTahunAkademik();
    String defSmt = Common.isNowSemensterGanjil()
                    ? String.valueOf(Perkuliahan.GANJIL)
                    : String.valueOf(Perkuliahan.GENAP);

    List<Fakultas> listFakultas = new ArrayList<Fakultas>();
    List<Jurusan>  listJurusan  = new ArrayList<Jurusan>();
    Session sessFilter = null;
    try {
        sessFilter = HibernateUtil.openSession();
        if (apakahPerguruanTinggi && !isSivitas) {
            listFakultas = ConstantValues.simpleList(
                sessFilter.createCriteria(Fakultas.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("nama")), Fakultas.class);
            listJurusan = ConstantValues.simpleList(
                sessFilter.createCriteria(Jurusan.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("nama")), Jurusan.class);
        }
    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe.jsp:47"); }
    finally {
        if (sessFilter != null && sessFilter.isOpen()) {
            try { sessFilter.clear(); sessFilter.disconnect(); sessFilter.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe.jsp:50");}
        }
        HibernateUtil.closeSessionQuietly(sessFilter);
    }
%>
<style>
/* ═══ OBE PANEL <%=rnd%> ═══════════════════════════════════════════════════════════ */
.o-<%=rnd%> { --c1:#198754; --c2:#146c43; --ch:#e8f5ee; --cr:#dc3545; --cw:#fd7e14; --ci:#0d6efd; }

/* Filter bar */
.o-filter-<%=rnd%> { background:#fff; border-radius:16px; border:1px solid #e9ecef;
    box-shadow:0 2px 12px rgba(0,0,0,.05); transition:.3s; }
.o-select-<%=rnd%> { border-radius:10px; border:1.5px solid #dee2e6; font-size:.875rem;
    transition:.2s; background:#fafafa; font-weight:600; }
.o-select-<%=rnd%>:focus { border-color:var(--c1); box-shadow:0 0 0 .2rem rgba(25,135,84,.15); background:#fff; }

/* Hero donut */
.o-donut-<%=rnd%> { position:relative; width:130px; height:130px; border-radius:50%;
    background:conic-gradient(var(--c1) 0% var(--pct,0%), #e9ecef var(--pct,0%) 100%);
    display:flex; align-items:center; justify-content:center;
    box-shadow:0 4px 20px rgba(25,135,84,.2); flex-shrink:0; }
.o-donut-<%=rnd%>::before { content:''; position:absolute; width:94px; height:94px;
    background:#fff; border-radius:50%; }
.o-donut-inner-<%=rnd%> { position:relative; z-index:1; text-align:center; line-height:1.2; }
.o-donut-pct-<%=rnd%> { font-size:1.6rem; font-weight:800; color:var(--c1); display:block; }
.o-donut-lbl-<%=rnd%> { font-size:.62rem; font-weight:700; color:#6c757d;
    text-transform:uppercase; letter-spacing:.5px; }

/* Stat cards */
.o-card-<%=rnd%> { border-radius:16px; border:1px solid #f1f3f5; background:#fff;
    overflow:hidden; position:relative; transition:.25s; }
.o-card-<%=rnd%>:hover { transform:translateY(-3px); box-shadow:0 8px 24px rgba(0,0,0,.08); }
.o-card-<%=rnd%> .o-accent { position:absolute; top:0; left:0; width:4px; height:100%;
    border-radius:16px 0 0 16px; }
.o-icon-<%=rnd%> { width:44px; height:44px; border-radius:11px; display:flex;
    align-items:center; justify-content:center; font-size:1.1rem; flex-shrink:0; }
.o-progress-<%=rnd%> { height:5px; border-radius:99px; background:#e9ecef; overflow:hidden; }
.o-progress-bar-<%=rnd%> { height:100%; border-radius:99px; transition:width 1s ease; }

/* Sub-nav pills */
.o-nav-<%=rnd%> { background:#f1f3f5; border-radius:99px; padding:.3rem; display:inline-flex; gap:.25rem; }
.o-nav-<%=rnd%> .nav-link { border-radius:99px; border:none; color:#6c757d; font-weight:700;
    font-size:.83rem; padding:8px 18px; transition:.2s; }
.o-nav-<%=rnd%> .nav-link:hover { background:#e2e6ea; color:#343a40; }
.o-nav-<%=rnd%> .nav-link.active { background:linear-gradient(135deg,var(--c1),var(--c2));
    color:#fff; box-shadow:0 3px 10px rgba(25,135,84,.3); }

/* Table */
.o-tbl-<%=rnd%> { font-size:.84rem; }
.o-tbl-<%=rnd%> thead th { background:#f8f9fa; font-size:.7rem; font-weight:800;
    text-transform:uppercase; letter-spacing:.6px; color:#495057;
    border-bottom:2px solid #dee2e6; padding:.85rem .75rem; white-space:nowrap;
    cursor:pointer; user-select:none; }
.o-tbl-<%=rnd%> thead th:hover { background:#eef0f2; }
.o-tbl-<%=rnd%> thead th .sort-icon { opacity:.4; margin-left:4px; font-size:.65rem; }
.o-tbl-<%=rnd%> thead th.sorted .sort-icon { opacity:1; }
.o-tbl-<%=rnd%> td { padding:.85rem .75rem; vertical-align:middle; border-bottom:1px solid #f4f5f7; }
.o-tbl-<%=rnd%> tbody tr:hover { background:#f8fdf9; }

/* Badges */
.ob-ok   { background:rgba(25,135,84,.1); color:#146c43; border:1px solid rgba(25,135,84,.2); }
.ob-err  { background:rgba(220,53,69,.1); color:#b02a37; border:1px solid rgba(220,53,69,.2); }
.ob-warn { background:rgba(253,126,20,.1); color:#c05b00; border:1px solid rgba(253,126,20,.2); }
.ob-info { background:rgba(13,110,253,.1); color:#0a58ca; border:1px solid rgba(13,110,253,.2); }

/* Skeleton */
.o-skel-<%=rnd%> { border-radius:8px; background:linear-gradient(90deg,#f0f0f0 25%,#e0e0e0 50%,#f0f0f0 75%);
    background-size:200% 100%; animation:oSkelAnim<%=rnd%> 1.4s infinite; }
@keyframes oSkelAnim<%=rnd%> { 0%{background-position:200% 0} 100%{background-position:-200% 0} }

/* Search box in card-header */
.o-search-<%=rnd%> { border-radius:99px; border:1.5px solid #dee2e6; font-size:.82rem;
    padding:.35rem .9rem; transition:.2s; max-width:220px; }
.o-search-<%=rnd%>:focus { border-color:var(--c1); box-shadow:0 0 0 .2rem rgba(25,135,84,.12); outline:none; }

/* Mobile card view for table rows */
@media (max-width:767.98px) {
    .o-responsive-table-<%=rnd%> thead { display:none; }
    .o-responsive-table-<%=rnd%> tr { display:block; border:1px solid #e9ecef;
        border-radius:12px; margin-bottom:.75rem; padding:.5rem; background:#fff; }
    .o-responsive-table-<%=rnd%> td { display:flex; justify-content:space-between;
        align-items:center; border:none; padding:.4rem .5rem; font-size:.82rem; }
    .o-responsive-table-<%=rnd%> td::before { content:attr(data-label);
        font-weight:700; font-size:.72rem; color:#6c757d; text-transform:uppercase;
        letter-spacing:.4px; flex-shrink:0; margin-right:.5rem; }
    .o-nav-<%=rnd%> { width:100%; overflow-x:auto; -webkit-overflow-scrolling:touch; scrollbar-width:none; }
    .o-nav-<%=rnd%>::-webkit-scrollbar { display:none; }
    .o-donut-<%=rnd%> { width:110px; height:110px; }
    .o-donut-<%=rnd%>::before { width:80px; height:80px; }
    .o-filter-<%=rnd%> .row > [class*="col-"] { padding-left:.4rem; padding-right:.4rem; }
    .o-select-<%=rnd%>, .form-select.o-select-<%=rnd%> { font-size: 16px; }
    #obe-reload-<%=rnd%>, button[onclick*="obeLoad"] { min-height:38px; }
}

/* Empty & error state */
.o-empty-<%=rnd%> { text-align:center; padding:3.5rem 1rem; color:#adb5bd; }
.o-empty-<%=rnd%> i { font-size:2.8rem; opacity:.25; margin-bottom:1rem; display:block; }
</style>

<div class="o-<%=rnd%> container-fluid py-3 py-md-4 px-3 px-xl-4">

    <!-- ─── HEADER ──────────────────────────────────────────────────────────── -->
    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
        <div class="d-flex align-items-center gap-3">
            <div class="rounded-4 d-flex align-items-center justify-content-center shadow-sm flex-shrink-0"
                 style="width:50px;height:50px;background:linear-gradient(135deg,#198754,#146c43);">
                <i class="fas fa-graduation-cap text-white fs-5"></i>
            </div>
            <div>
                <div class="small fw-bold text-success text-uppercase" style="letter-spacing:1px;">Outcome Based Education</div>
                <h5 class="fw-bold text-dark mb-0"><%=Common.getBahasaConfig("Dasbor OBE per Semester")%></h5>
            </div>
        </div>
        <button class="btn btn-sm btn-success rounded-pill fw-bold px-4 shadow-sm border-0"
                onclick="obeLoad<%=rnd%>(true)">
            <i class="fas fa-sync-alt me-2" id="obe-ricon-<%=rnd%>"></i><%=Common.getBahasaConfig("Segarkan")%>
        </button>
    </div>

    <!-- ─── FILTER BAR ──────────────────────────────────────────────────────── -->
    <div class="o-filter-<%=rnd%> p-3 mb-3">
        <div class="row g-2 align-items-end">
            <div class="col-6 col-md-3 col-xl-2">
                <label class="form-label small fw-bold text-muted mb-1">
                    <i class="far fa-calendar-alt me-1 text-success"></i><%=Common.getBahasaConfig("Tahun Akademik")%>
                    <span class="badge bg-danger ms-1" style="font-size:.6rem">WAJIB</span>
                </label>
                <select id="obe-ta-<%=rnd%>" class="form-select o-select-<%=rnd%>"
                        onchange="obeLoad<%=rnd%>(false)">
                    <option value="">— Pilih —</option>
                    <% if (Common.tahunAngkatans != null) { for (String ta : Common.tahunAngkatans) {
                        String s = (ta != null && ta.equals(defTa)) ? "selected" : ""; %>
                    <option value="<%=ta%>" <%=s%>><%=ta%></option>
                    <% }} %>
                </select>
            </div>
            <div class="col-6 col-md-3 col-xl-2">
                <label class="form-label small fw-bold text-muted mb-1">
                    <i class="fas fa-hourglass-half me-1 text-success"></i><%=Common.getBahasaConfig("Semester")%>
                    <span class="badge bg-danger ms-1" style="font-size:.6rem">WAJIB</span>
                </label>
                <select id="obe-smt-<%=rnd%>" class="form-select o-select-<%=rnd%>"
                        onchange="obeLoad<%=rnd%>(false)">
                    <option value="">— Pilih —</option>
                    <option value="<%=Perkuliahan.GANJIL%>" <%=Perkuliahan.GANJIL.equals(defSmt)?"selected":""%>><%=Common.getBahasaConfig("Ganjil")%></option>
                    <option value="<%=Perkuliahan.GENAP%>" <%=Perkuliahan.GENAP.equals(defSmt)?"selected":""%>><%=Common.getBahasaConfig("Genap")%></option>
                    <option value="<%=Perkuliahan.SP%>"><%=Common.getBahasaConfig("Semester Pendek")%></option>
                </select>
            </div>
            <% if (apakahPerguruanTinggi && !isSivitas && !listFakultas.isEmpty()) { %>
            <div class="col-md-4 col-xl-3">
                <label class="form-label small fw-bold text-muted mb-1">
                    <i class="fas fa-university me-1 text-info"></i><%=Common.getBahasaConfig("Fakultas")%>
                </label>
                <select id="obe-fak-<%=rnd%>" class="form-select o-select-<%=rnd%>"
                        onchange="obeUbahFak<%=rnd%>()">
                    <option value=""><%=Common.getBahasaConfig("Semua Fakultas")%></option>
                    <% for (Fakultas f : listFakultas) { %>
                    <option value="<%=f.getId()%>"><%=f.getNama()%></option>
                    <% } %>
                </select>
            </div>
            <div class="col-md-4 col-xl-3">
                <label class="form-label small fw-bold text-muted mb-1">
                    <i class="fas fa-layer-group me-1 text-info"></i><%=Common.getBahasaConfig("Program Studi")%>
                </label>
                <select id="obe-jur-<%=rnd%>" class="form-select o-select-<%=rnd%>"
                        onchange="obeLoad<%=rnd%>(false)">
                    <option value=""><%=Common.getBahasaConfig("Semua Prodi")%></option>
                    <% for (Jurusan j : listJurusan) { %>
                    <option value="<%=j.getId()%>"><%=j.getNama()%></option>
                    <% } %>
                </select>
            </div>
            <% } %>
            <div class="col-auto ms-auto d-flex gap-2">
                <button class="btn btn-outline-secondary rounded-pill btn-sm px-3 fw-bold"
                        onclick="obeReset<%=rnd%>()" title="Reset">
                    <i class="fas fa-undo"></i>
                </button>
                <button class="btn btn-success rounded-pill btn-sm px-4 fw-bold shadow-sm"
                        onclick="obeLoad<%=rnd%>(false)">
                    <i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Tampilkan")%>
                </button>
            </div>
        </div>
    </div>

    <!-- ─── KONTEN AREA ─────────────────────────────────────────────────────── -->
    <div id="obe-area-<%=rnd%>">
        <div class="o-empty-<%=rnd%>">
            <i class="fas fa-filter"></i>
            <h6 class="fw-bold"><%=Common.getBahasaConfig("Pilih Tahun Akademik dan Semester")%></h6>
            <p class="small mb-0"><%=Common.getBahasaConfig("Pilih periode di atas untuk menampilkan data OBE.")%></p>
        </div>
    </div>

</div>

<script>
// ─── Data cascade jurusan ──────────────────────────────────────────────────────
var obeJurusan<%=rnd%> = [
    <% for (Jurusan j : listJurusan) { Long fId = j.getFakultas()!=null?j.getFakultas().getId():0L; %>
    {id:"<%=j.getId()%>",nama:"<%=j.getNama()%>",fId:"<%=fId%>"},
    <% } %>
];
var obeData<%=rnd%>   = {};
var obeAdmin<%=rnd%>  = <%=(!isSivitas)%>;
var obeMhs<%=rnd%>    = <%=isMhs%>;
var obeDsn<%=rnd%>    = <%=isDsn%>;

// ─── Cascade ──────────────────────────────────────────────────────────────────
function obeUbahFak<%=rnd%>() {
    var fEl = document.getElementById('obe-fak-<%=rnd%>');
    var jEl = document.getElementById('obe-jur-<%=rnd%>');
    if (!fEl || !jEl) return;
    var fId = fEl.value;
    jEl.innerHTML = '<option value=""><%=Common.getBahasaConfig("Semua Prodi")%></option>';
    obeJurusan<%=rnd%>.forEach(function(j) {
        if (!fId || j.fId === fId) {
            var o = document.createElement('option');
            o.value = j.id; o.text = j.nama; jEl.add(o);
        }
    });
    obeLoad<%=rnd%>(false);
}

function obeReset<%=rnd%>() {
    ['obe-ta-<%=rnd%>','obe-smt-<%=rnd%>','obe-fak-<%=rnd%>','obe-jur-<%=rnd%>'].forEach(function(id) {
        var el = document.getElementById(id); if(el) el.value = '';
    });
    document.getElementById('obe-area-<%=rnd%>').innerHTML =
        '<div class="o-empty-<%=rnd%>"><i class="fas fa-filter"></i>' +
        '<h6 class="fw-bold"><%=Common.getBahasaConfig("Pilih Tahun Akademik dan Semester")%></h6>' +
        '<p class="small mb-0"><%=Common.getBahasaConfig("Pilih periode di atas untuk menampilkan data OBE.")%></p></div>';
}

// ─── Fetch utama ──────────────────────────────────────────────────────────────
function obeLoad<%=rnd%>(isRefresh) {
    var ta  = document.getElementById('obe-ta-<%=rnd%>');
    var smt = document.getElementById('obe-smt-<%=rnd%>');
    if (!ta||!smt||!ta.value||!smt.value) { obeReset<%=rnd%>(); return; }

    var icon = document.getElementById('obe-ricon-<%=rnd%>');
    if (icon) icon.classList.add('fa-spin');
    obeShowSkel<%=rnd%>();

    var pl = new URLSearchParams();
    pl.append('q_ta', ta.value); pl.append('q_smt', smt.value);
    var fk = document.getElementById('obe-fak-<%=rnd%>');
    var jr = document.getElementById('obe-jur-<%=rnd%>');
    if (fk) pl.append('q_fakultas', fk.value);
    if (jr) pl.append('q_jurusan',  jr.value);
    if (isRefresh) pl.append('refresh','true');

    fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_obe_service', {
        method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'},
        body: pl.toString()
    })
    .then(function(r){ if(!r.ok) throw new Error('Network error'); return r.json(); })
    .then(function(res){
        if (res.status === 'ok') {
            obeData<%=rnd%> = res.rekap || {};
            obeRender<%=rnd%>(res.stats || {});
        } else {
            obeShowErr<%=rnd%>(res.msg || '<%=Common.getBahasaConfigJS("Terjadi kesalahan.")%>');
        }
    })
    .catch(function(e){ obeShowErr<%=rnd%>(e.message); })
    .finally(function(){ if(icon) icon.classList.remove('fa-spin'); });
}

// ─── Skeleton loading ─────────────────────────────────────────────────────────
function obeShowSkel<%=rnd%>() {
    var s = function(w,h){ return '<div class="o-skel-<%=rnd%> d-inline-block" style="width:'+w+';height:'+h+'px;border-radius:8px;"></div>'; };
    var cards = '';
    for(var i=0;i<4;i++) cards += '<div class="col">' +
        '<div class="o-card-<%=rnd%> p-3 shadow-sm">' +
        '<div class="o-accent" style="background:#e9ecef"></div>' +
        '<div class="d-flex justify-content-between mb-3">' + s('44px',44) + s('60px',32) + '</div>' +
        s('80%','12') + '<div class="mt-2"></div>' + s('50%','9') + '</div></div>';
    document.getElementById('obe-area-<%=rnd%>').innerHTML =
        '<div class="row row-cols-2 row-cols-xl-4 g-3 mb-3">' + cards + '</div>' +
        '<div class="o-card-<%=rnd%> shadow-sm p-4 mb-3 d-flex align-items-center gap-4">' +
        '<div class="o-skel-<%=rnd%> flex-shrink-0" style="width:130px;height:130px;border-radius:50%;"></div>' +
        '<div class="flex-grow-1">' + s('50%','14') + '<div class="mt-3"></div>' +
        s('100%','10') + '<div class="mt-2"></div>' + s('70%','10') + '</div></div>';
}

// ─── Error state ──────────────────────────────────────────────────────────────
function obeShowErr<%=rnd%>(msg) {
    document.getElementById('obe-area-<%=rnd%>').innerHTML =
        '<div class="alert alert-danger rounded-4 shadow-sm d-flex align-items-center gap-3">' +
        '<i class="fas fa-exclamation-triangle fa-lg"></i><span>' + msg + '</span></div>';
}

// ─── Heatmap CPL ─────────────────────────────────────────────────────────────
var obeCplCovData<%=rnd%> = [];

function obeCplHeatmapHtml<%=rnd%>() {
    if (!obeCplCovData<%=rnd%> || obeCplCovData<%=rnd%>.length === 0) {
        return '<div class="o-card-<%=rnd%> shadow-sm p-4 text-center text-muted"><i class="fas fa-info-circle me-1"></i>' +
               'Data CPL Bobot belum diisi. Isi field <b>CPL Bobot</b> di masing-masing RPS OBE.</div>';
    }
    var maxCount = 0;
    obeCplCovData<%=rnd%>.forEach(function(d){ if(d.count > maxCount) maxCount = d.count; });
    var rows = '';
    obeCplCovData<%=rnd%>.forEach(function(d) {
        var pct = maxCount > 0 ? Math.round(d.count / maxCount * 100) : 0;
        var col = pct >= 80 ? '#198754' : (pct >= 50 ? '#0d6efd' : (pct >= 25 ? '#fd7e14' : '#dc3545'));
        rows += '<tr>' +
            '<td style="width:90px;font-weight:600;font-size:.85rem">' + d.cpl + '</td>' +
            '<td><div style="background:#e9ecef;border-radius:4px;height:20px;position:relative">' +
            '<div style="background:' + col + ';width:' + pct + '%;height:100%;border-radius:4px;transition:width .5s"></div>' +
            '</div></td>' +
            '<td style="width:50px;text-align:center;font-weight:700;color:' + col + '">' + d.count + '</td>' +
            '<td style="width:80px;text-align:center;color:#6c757d;font-size:.8rem">' +
            (d.bobotTotal > 0 ? '&Sigma;' + d.bobotTotal.toFixed(0) + '%' : '-') + '</td>' +
            '</tr>';
    });
    return '<div class="o-card-<%=rnd%> shadow-sm overflow-hidden">' +
        '<div class="card-header bg-white border-bottom py-3 px-3">' +
        '<h6 class="mb-0 fw-bold"><i class="fas fa-th text-primary me-2"></i>' +
        '<%=Common.getBahasaConfigJS("Cakupan CPL di Seluruh Mata Kuliah")%></h6>' +
        '<div class="text-muted mt-1" style="font-size:.75rem">Berapa MK yang membebankan tiap CPL (dari data CPL Bobot yang sudah diisi)</div>' +
        '</div>' +
        '<div class="p-3"><table class="table table-sm table-borderless" style="table-layout:fixed;width:100%">' +
        '<thead><tr><th style="width:90px">CPL</th><th><%=Common.getBahasaConfig("Cakupan MK")%></th>' +
        '<th style="width:50px;text-align:center">MK</th>' +
        '<th style="width:80px;text-align:center">&Sigma; Bobot</th></tr></thead>' +
        '<tbody>' + rows + '</tbody></table></div></div>';
}

// ─── Render konten utama ──────────────────────────────────────────────────────
function obeRender<%=rnd%>(stats) {
    var total    = stats.totalMk    || 0;
    var sudah    = stats.sudahRps   || 0;
    var belum    = stats.belumRps   || 0;
    var punyaCpl = stats.mkPunyaCpl || 0;
    obeCplCovData<%=rnd%> = stats.cplCoverage || [];
    var pct      = total > 0 ? Math.round((sudah/total)*100) : 0;
    var colPct   = pct >= 75 ? '#198754' : (pct >= 40 ? '#fd7e14' : '#dc3545');

    // 4 stat cards
    var cardData = [
        {acc:'#198754',  bg:'rgba(25,135,84,.12)',  ic:'fas fa-layer-group', lc:'text-success', label:'Total MK OBE',       val:total,    sub:'Mata kuliah kurikulum OBE aktif', pct:100},
        {acc:'#0d6efd',  bg:'rgba(13,110,253,.12)', ic:'fas fa-file-alt',    lc:'text-primary', label:'RPS OBE Terisi',      val:sudah,    sub:'Sudah memiliki RPS OBE lengkap', pct:total>0?Math.round(sudah/total*100):0},
        {acc:'#dc3545',  bg:'rgba(220,53,69,.12)',  ic:'fas fa-exclamation', lc:'text-danger',  label:'RPS Belum Ada',       val:belum,    sub:'Belum mengisi RPS OBE', pct:total>0?Math.round(belum/total*100):0},
        {acc:'#6f42c1',  bg:'rgba(111,66,193,.12)', ic:'fas fa-bullseye',    lc:'text-purple',  label:'Punya CPL',           val:punyaCpl, sub:'MK sudah punya Capaian Lulusan', pct:total>0?Math.round(punyaCpl/total*100):0},
    ];
    var cardsHtml = '<div class="row row-cols-2 row-cols-xl-4 g-3 mb-3">';
    cardData.forEach(function(c) {
        cardsHtml += '<div class="col"><div class="o-card-<%=rnd%> shadow-sm h-100 p-3">' +
            '<div class="o-accent" style="background:'+c.acc+'"></div>' +
            '<div class="d-flex justify-content-between align-items-start mb-2">' +
            '<div class="o-icon-<%=rnd%>" style="background:'+c.bg+'"><i class="'+c.ic+' '+c.lc+'"></i></div>' +
            '<span class="fw-bold '+c.lc+'" style="font-size:1.6rem">'+c.val+'</span></div>' +
            '<div class="fw-bold text-dark" style="font-size:.78rem;text-transform:uppercase;letter-spacing:.4px">'+c.label+'</div>' +
            '<div class="text-muted mb-2" style="font-size:.73rem">'+c.sub+'</div>' +
            '<div class="o-progress-<%=rnd%>"><div class="o-progress-bar-<%=rnd%>" style="width:'+c.pct+'%;background:'+c.acc+'"></div></div>' +
            '<div class="text-end mt-1" style="font-size:.68rem;color:#6c757d">'+c.pct+'%</div>' +
            '</div></div>';
    });
    cardsHtml += '</div>';

    // Hero: donut + konteks
    var heroHtml =
        '<div class="o-card-<%=rnd%> shadow-sm p-4 mb-3">' +
        '<div class="d-flex align-items-center flex-wrap gap-4">' +
        '<div class="o-donut-<%=rnd%>" style="--pct:'+pct+'%">' +
        '<div class="o-donut-inner-<%=rnd%>">' +
        '<span class="o-donut-pct-<%=rnd%>" style="color:'+colPct+'">'+pct+'%</span>' +
        '<span class="o-donut-lbl-<%=rnd%>">RPS OBE</span></div></div>' +
        '<div class="flex-grow-1">' +
        '<h6 class="fw-bold mb-1"><i class="fas fa-tasks me-2 text-success"></i><%=Common.getBahasaConfig("Progres Ketercapaian RPS OBE")%></h6>' +
        '<p class="text-muted small mb-3">'+sudah+' dari '+total+' mata kuliah OBE sudah memiliki RPS.</p>' +
        '<div class="d-flex flex-wrap gap-3">' +
        obeKpi<%=rnd%>('#198754','fas fa-check-circle','Terisi',sudah) +
        obeKpi<%=rnd%>('#dc3545','fas fa-times-circle','Belum',belum) +
        obeKpi<%=rnd%>('#6f42c1','fas fa-bullseye','Punya CPL',punyaCpl) +
        '</div></div></div></div>';

    // Sub-nav + tab-content
    var navHtml =
        '<div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2 mb-3">' +
        '<ul class="nav o-nav-<%=rnd%>" role="tablist">' +
        '<li class="nav-item"><a class="nav-link active" data-bs-toggle="pill" href="#obe-p-rps-<%=rnd%>" role="tab">' +
        '<i class="fas fa-file-alt me-1"></i><%=Common.getBahasaConfig("Status RPS OBE")%></a></li>' +
        '<li class="nav-item"><a class="nav-link" data-bs-toggle="pill" href="#obe-p-cpl-<%=rnd%>" role="tab">' +
        '<i class="fas fa-bullseye me-1"></i><%=Common.getBahasaConfig("CPL & CPMK per MK")%></a></li>' +
        '<li class="nav-item"><a class="nav-link" data-bs-toggle="pill" href="#obe-p-heatmap-<%=rnd%>" role="tab">' +
        '<i class="fas fa-th me-1"></i><%=Common.getBahasaConfig("Heatmap CPL")%></a></li>' +
        '</ul>' +
        '<div class="d-flex align-items-center gap-2">' +
        '<input type="search" id="obe-srch-<%=rnd%>" class="form-control o-search-<%=rnd%>"' +
        ' placeholder="Cari kode / nama..." oninput="obeSearch<%=rnd%>()" />' +
        '</div></div>';

    var tabHtml = '<div class="tab-content">' +
        '<div class="tab-pane fade show active" id="obe-p-rps-<%=rnd%>">' + obeTableRpsHtml<%=rnd%>() + '</div>' +
        '<div class="tab-pane fade" id="obe-p-cpl-<%=rnd%>">' + obeTableCplHtml<%=rnd%>() + '</div>' +
        '<div class="tab-pane fade" id="obe-p-heatmap-<%=rnd%>">' + obeCplHeatmapHtml<%=rnd%>() + '</div>' +
        '</div>';

    document.getElementById('obe-area-<%=rnd%>').innerHTML = cardsHtml + heroHtml + navHtml + tabHtml;

    // Animasi progress bar setelah render
    setTimeout(function() {
        document.querySelectorAll('.o-progress-bar-<%=rnd%>').forEach(function(b){ b.style.width = b.style.width; });
    }, 60);

    obeRenderRps<%=rnd%>(1);

    document.addEventListener('shown.bs.tab', function(e) {
        if (!e||!e.target) return;
        var h = e.target.getAttribute('href');
        if (h === '#obe-p-rps-<%=rnd%>') obeRenderRps<%=rnd%>(obePageRps<%=rnd%>);
        if (h === '#obe-p-cpl-<%=rnd%>') obeRenderCpl<%=rnd%>(1);
        if (h === '#obe-p-heatmap-<%=rnd%>') {
            var el = document.getElementById('obe-p-heatmap-<%=rnd%>');
            if (el && el.dataset.rendered !== '1') {
                el.innerHTML = obeCplHeatmapHtml<%=rnd%>();
                el.dataset.rendered = '1';
            }
        }
    });
}

function obeKpi<%=rnd%>(color, icon, label, val) {
    return '<div class="d-flex align-items-center gap-2 rounded-3 px-3 py-2" style="background:'+
        color+'18;border:1px solid '+color+'28">' +
        '<i class="'+icon+'" style="color:'+color+'"></i>' +
        '<div><div class="fw-bold text-dark" style="font-size:.9rem">'+val+'</div>' +
        '<div class="text-muted" style="font-size:.68rem;text-transform:uppercase;letter-spacing:.4px">'+label+'</div>' +
        '</div></div>';
}

// ─── Tabel RPS ───────────────────────────────────────────────────────────────
var obePageRps<%=rnd%> = 1;
var obeSortRps<%=rnd%> = {col:'', asc:true};

function obeTableRpsHtml<%=rnd%>() {
    var adm = obeAdmin<%=rnd%>;
    return '<div class="o-card-<%=rnd%> shadow-sm overflow-hidden">' +
        '<div class="card-header bg-white border-bottom py-3 px-3 d-flex justify-content-between align-items-center flex-wrap gap-2">' +
        '<h6 class="mb-0 fw-bold"><i class="fas fa-list-alt text-success me-2"></i><%=Common.getBahasaConfig("Daftar Mata Kuliah OBE")%></h6>' +
        '<button class="btn btn-sm btn-outline-success rounded-pill fw-bold px-3 border-2" onclick="obeExport<%=rnd%>()">' +
        '<i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Excel")%></button></div>' +
        '<div class="table-responsive">' +
        '<table class="table table-hover o-tbl-<%=rnd%> o-responsive-table-<%=rnd%> mb-0">' +
        '<thead><tr>' +
        '<th class="text-center" style="width:46px">No</th>' +
        '<th onclick="obeSort<%=rnd%>(\'rps\',\'nama\')"><%=Common.getBahasaConfig("Mata Kuliah")%> <i class="fas fa-sort sort-icon"></i></th>' +
        (adm?'<th><%=Common.getBahasaConfig("Program Studi")%></th>':'') +
        (adm?'<th><%=Common.getBahasaConfig("Dosen")%></th>':'') +
        '<th class="text-center" style="width:60px">SKS</th>' +
        '<th class="text-center" style="width:130px"><%=Common.getBahasaConfig("Status RPS")%></th>' +
        '<th class="text-center" style="width:100px"><%=Common.getBahasaConfig("Tgl Susun")%></th>' +
        '<th class="text-center" style="width:58px">CPL</th>' +
        '<th class="text-center" style="width:58px">CPMK</th>' +
        '<th class="text-center" style="width:58px">BK</th>' +
        '<th class="text-center" style="width:58px">Kunci</th>' +
        '<th class="text-center" style="width:70px">CQI</th>' +
        '</tr></thead>' +
        '<tbody id="obe-tbody-rps-<%=rnd%>"></tbody></table></div>' +
        '<div class="card-footer bg-white border-top-0 py-2" id="obe-pag-rps-<%=rnd%>"></div></div>';
}

function obeRenderRps<%=rnd%>(page) {
    obePageRps<%=rnd%> = page;
    var raw  = obeData<%=rnd%>.rps || [];
    var srch = (document.getElementById('obe-srch-<%=rnd%>')||{}).value||'';
    var data = srch ? raw.filter(function(r){ var k=(r.kode+' '+r.nama+' '+(r.prodi||'')+(r.dosen||'')).toLowerCase(); return k.includes(srch.toLowerCase()); }) : raw;
    var lim = 10; var start = (page-1)*lim; var paged = data.slice(start, start+lim);
    var adm = obeAdmin<%=rnd%>;
    var html = '';
    if (paged.length === 0) {
        var cols = 9 + (adm?2:0);
        html = '<tr><td colspan="'+cols+'" class="o-empty-<%=rnd%> py-5">' +
               '<i class="fas fa-folder-open"></i><b><%=Common.getBahasaConfig("Tidak ada data ditemukan.")%></b></td></tr>';
    } else {
        paged.forEach(function(d, i) {
            var no = start+i+1;
            var badge = d.punyaRps
                ? '<span class="badge rounded-pill ob-ok px-3 py-2 fw-bold"><i class="fas fa-check-circle me-1"></i>Sudah</span>'
                : '<span class="badge rounded-pill ob-err px-3 py-2 fw-bold"><i class="fas fa-times-circle me-1"></i>Belum</span>';
            var kunci = d.dikunci
                ? '<span class="badge ob-warn rounded-pill px-2"><i class="fas fa-lock"></i></span>'
                : '<span class="badge rounded-pill px-2" style="background:#f0f0f0;color:#aaa"><i class="fas fa-lock-open"></i></span>';
            html += '<tr>' +
                '<td class="text-center fw-bold text-muted" data-label="No">'+no+'</td>' +
                '<td data-label="MK"><div class="fw-bold text-dark">'+obeSafe<%=rnd%>(d.kode)+'</div>' +
                '<div class="text-muted" style="font-size:.78rem">'+obeSafe<%=rnd%>(d.nama)+'</div></td>' +
                (adm?'<td data-label="Prodi"><span class="text-muted" style="font-size:.8rem">'+obeSafe<%=rnd%>(d.prodi)+'</span></td>':'') +
                (adm?'<td data-label="Dosen"><span class="text-muted" style="font-size:.8rem">'+obeSafe<%=rnd%>(d.dosen)+'</span></td>':'') +
                '<td class="text-center fw-bold" data-label="SKS">'+(d.sks||0)+'</td>' +
                '<td class="text-center" data-label="Status RPS">'+badge+'</td>' +
                '<td class="text-center text-muted" style="font-size:.78rem" data-label="Tgl">'+obeSafe<%=rnd%>(d.tglRps)+'</td>' +
                '<td class="text-center" data-label="CPL"><span class="badge ob-info rounded-pill">'+(d.jmlCpl||0)+'</span></td>' +
                '<td class="text-center" data-label="CPMK"><span class="badge ob-info rounded-pill">'+(d.jmlCpmk||0)+'</span></td>' +
                '<td class="text-center" data-label="BK"><span class="badge ob-info rounded-pill">'+(d.jmlBk||0)+'</span></td>' +
                '<td class="text-center" data-label="Kunci">'+kunci+'</td>' +
                '<td class="text-center" data-label="CQI"><button class="btn btn-sm btn-outline-primary rounded-pill px-2 border-2" onclick="obeBukaCqi<%=rnd%>(\''+d.id+'\')" title="<%=Common.getBahasaConfig("Analisis CQI per CPMK")%>"><i class="fas fa-clipboard-check"></i></button></td>' +
                '</tr>';
        });
    }
    var el = document.getElementById('obe-tbody-rps-<%=rnd%>');
    if (el) el.innerHTML = html;
    obePaging<%=rnd%>('rps', page, Math.ceil(data.length/lim), 'obeRenderRps<%=rnd%>');
}

// ─── CQI: buka layar Analisis CQI per CPMK (per-perkuliahan) ──────────────────
window.obeBukaCqi<%=rnd%> = function(idPerk) {
    var url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_cqi&perkuliahan=' + idPerk;
    if (typeof loadModalContentCustomSimpan === 'function') {
        var cm = 'cm_cqi_' + idPerk;
        loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Analisis CQI per CPMK")%>', url + '&contentModal=' + cm, '90%', 'hidden', '', cm);
    } else {
        window.open(url, '_blank');
    }
};

// ─── Tabel CPL ───────────────────────────────────────────────────────────────
var obePageCpl<%=rnd%> = 1;

function obeTableCplHtml<%=rnd%>() {
    var adm = obeAdmin<%=rnd%>;
    return '<div class="o-card-<%=rnd%> shadow-sm overflow-hidden">' +
        '<div class="card-header bg-white border-bottom py-3 px-3">' +
        '<h6 class="mb-0 fw-bold"><i class="fas fa-bullseye text-primary me-2"></i><%=Common.getBahasaConfig("CPL & CPMK per Mata Kuliah OBE")%></h6></div>' +
        '<div class="table-responsive">' +
        '<table class="table table-hover o-tbl-<%=rnd%> o-responsive-table-<%=rnd%> mb-0"><thead><tr>' +
        '<th class="text-center" style="width:46px">No</th>' +
        '<th><%=Common.getBahasaConfig("Mata Kuliah")%></th>' +
        (adm?'<th><%=Common.getBahasaConfig("Program Studi")%></th>':'') +
        '<th class="text-center" style="width:65px">CPL</th>' +
        '<th class="text-center" style="width:65px">CPMK</th>' +
        '<th class="text-center" style="width:65px">BK</th>' +
        '<th class="text-center" style="width:110px"><%=Common.getBahasaConfig("Min. Ketercapaian")%></th>' +
        '<th class="text-center" style="width:100px">Status RPS</th>' +
        '</tr></thead><tbody id="obe-tbody-cpl-<%=rnd%>"></tbody></table></div>' +
        '<div class="card-footer bg-white border-top-0 py-2" id="obe-pag-cpl-<%=rnd%>"></div></div>';
}

function obeRenderCpl<%=rnd%>(page) {
    obePageCpl<%=rnd%> = page;
    var raw  = obeData<%=rnd%>.cpl || [];
    var srch = (document.getElementById('obe-srch-<%=rnd%>')||{}).value||'';
    var data = srch ? raw.filter(function(r){ return (r.mk+' '+(r.prodi||'')).toLowerCase().includes(srch.toLowerCase()); }) : raw;
    var lim = 10; var start = (page-1)*lim; var paged = data.slice(start, start+lim);
    var adm = obeAdmin<%=rnd%>;
    var html = '';
    if (paged.length === 0) {
        html = '<tr><td colspan="'+(6+(adm?1:0))+'" class="o-empty-<%=rnd%> py-5">' +
               '<i class="fas fa-folder-open"></i><b><%=Common.getBahasaConfig("Tidak ada data.")%></b></td></tr>';
    } else {
        paged.forEach(function(d, i) {
            var no = start+i+1;
            var minV = parseFloat(d.minKetercapaian)||0;
            var minC = minV>=70?'text-success fw-bold':(minV>0?'text-warning fw-bold':'text-muted');
            var badge = d.punyaRps
                ? '<span class="badge rounded-pill ob-ok px-2 fw-bold"><i class="fas fa-check me-1"></i>Ada</span>'
                : '<span class="badge rounded-pill ob-err px-2 fw-bold"><i class="fas fa-times me-1"></i>Belum</span>';
            html += '<tr>' +
                '<td class="text-center fw-bold text-muted" data-label="No">'+no+'</td>' +
                '<td class="fw-bold text-dark" data-label="MK">'+obeSafe<%=rnd%>(d.mk)+'</td>' +
                (adm?'<td style="font-size:.8rem;color:#6c757d" data-label="Prodi">'+obeSafe<%=rnd%>(d.prodi)+'</td>':'') +
                '<td class="text-center" data-label="CPL"><span class="badge ob-info rounded-pill">'+(d.jmlCpl||0)+'</span></td>' +
                '<td class="text-center" data-label="CPMK"><span class="badge ob-info rounded-pill">'+(d.jmlCpmk||0)+'</span></td>' +
                '<td class="text-center" data-label="BK"><span class="badge ob-info rounded-pill">'+(d.jmlBk||0)+'</span></td>' +
                '<td class="text-center '+minC+'" data-label="Min Ketercapaian">'+(minV>0?minV+'':'–')+'</td>' +
                '<td class="text-center" data-label="Status RPS">'+badge+'</td>' +
                '</tr>';
        });
    }
    var el = document.getElementById('obe-tbody-cpl-<%=rnd%>');
    if (el) el.innerHTML = html;
    obePaging<%=rnd%>('cpl', page, Math.ceil(data.length/lim), 'obeRenderCpl<%=rnd%>');
}

// ─── Search real-time ─────────────────────────────────────────────────────────
var obeSearchTimer<%=rnd%>;
function obeSearch<%=rnd%>() {
    clearTimeout(obeSearchTimer<%=rnd%>);
    obeSearchTimer<%=rnd%> = setTimeout(function(){
        obeRenderRps<%=rnd%>(1); obeRenderCpl<%=rnd%>(1);
    }, 220);
}

// ─── Sort ─────────────────────────────────────────────────────────────────────
function obeSort<%=rnd%>(tbl, col) {
    var s = obeSortRps<%=rnd%>;
    if (s.col === col) s.asc = !s.asc; else { s.col = col; s.asc = true; }
    var d = obeData<%=rnd%>[tbl] || [];
    d.sort(function(a,b){
        var va = (a[col]||'').toString().toLowerCase();
        var vb = (b[col]||'').toString().toLowerCase();
        return s.asc ? va.localeCompare(vb) : vb.localeCompare(va);
    });
    obeData<%=rnd%>[tbl] = d;
    if (tbl==='rps') obeRenderRps<%=rnd%>(1);
    else obeRenderCpl<%=rnd%>(1);
}

// ─── Paginasi ─────────────────────────────────────────────────────────────────
function obePaging<%=rnd%>(key, cur, total, fn) {
    var el = document.getElementById('obe-pag-'+key+'-<%=rnd%>');
    if (!el) return;
    if (total <= 1) { el.innerHTML = ''; return; }
    var h = '<div class="d-flex align-items-center justify-content-between px-1">' +
        '<small class="text-muted">Hal '+cur+' / '+total+'</small>' +
        '<ul class="pagination pagination-sm mb-0 gap-1">';
    h += '<li class="page-item'+(cur===1?' disabled':'')+'"><a class="page-link rounded-3 border-0 shadow-sm" href="javascript:void(0)" onclick="'+fn+'('+(cur-1)+')"><i class="fas fa-chevron-left"></i></a></li>';
    var s=Math.max(1,cur-2), e=Math.min(total,cur+2);
    if(s>1) h+='<li class="page-item"><a class="page-link rounded-3 border-0" href="javascript:void(0)" onclick="'+fn+'(1)">1</a></li>';
    if(s>2) h+='<li class="page-item disabled"><span class="page-link border-0">…</span></li>';
    for(var i=s;i<=e;i++){
        h+='<li class="page-item'+(i===cur?' active':'')+'"><a class="page-link rounded-3 border-0'+(i===cur?' bg-success text-white border-0':'')+'" href="javascript:void(0)" onclick="'+fn+'('+i+')">'+i+'</a></li>';
    }
    if(e<total-1) h+='<li class="page-item disabled"><span class="page-link border-0">…</span></li>';
    if(e<total)   h+='<li class="page-item"><a class="page-link rounded-3 border-0" href="javascript:void(0)" onclick="'+fn+'('+total+')">'+total+'</a></li>';
    h += '<li class="page-item'+(cur===total?' disabled':'')+'"><a class="page-link rounded-3 border-0 shadow-sm" href="javascript:void(0)" onclick="'+fn+'('+(cur+1)+')"><i class="fas fa-chevron-right"></i></a></li>';
    h += '</ul></div>';
    el.innerHTML = h;
}

// ─── Export Excel ─────────────────────────────────────────────────────────────
function obeExport<%=rnd%>() {
    var data = obeData<%=rnd%>.rps||[];
    if (!data.length) { if(typeof tampilkanToast==='function') tampilkanToast('Tidak ada data.','bg-warning text-dark'); return; }
    var h = '<table border="1"><thead><tr><th>No</th><th>Kode</th><th><%=Common.getBahasaConfig("Nama MK")%></th><th>Prodi</th>' +
            '<th>Dosen</th><th>SKS</th><th><%=Common.getBahasaConfig("Status RPS")%></th><th><%=Common.getBahasaConfig("Tgl Susun")%></th>' +
            '<th>CPL</th><th>CPMK</th><th>BK</th><th>Dikunci</th></tr></thead><tbody>';
    data.forEach(function(d,i){
        h += '<tr><td>'+(i+1)+'</td><td>'+(d.kode||'')+'</td><td>'+(d.nama||'')+'</td>' +
             '<td>'+(d.prodi||'')+'</td><td>'+(d.dosen||'')+'</td><td>'+(d.sks||0)+'</td>' +
             '<td>'+(d.punyaRps?'Sudah Ada':'Belum Ada')+'</td><td>'+(d.tglRps||'-')+'</td>' +
             '<td>'+(d.jmlCpl||0)+'</td><td>'+(d.jmlCpmk||0)+'</td><td>'+(d.jmlBk||0)+'</td>' +
             '<td>'+(d.dikunci?'Terkunci':'Terbuka')+'</td></tr>';
    });
    h += '</tbody></table>';
    var a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([h],{type:'application/vnd.ms-excel'}));
    a.download = 'Status_RPS_OBE.xls'; a.click(); URL.revokeObjectURL(a.href);
}

// ─── Helper ───────────────────────────────────────────────────────────────────
function obeSafe<%=rnd%>(v) { return v ? String(v).replace(/</g,'&lt;').replace(/>/g,'&gt;') : '-'; }

// ─── Auto-load jika default tersedia ─────────────────────────────────────────
(function(){
    var ta  = document.getElementById('obe-ta-<%=rnd%>');
    var smt = document.getElementById('obe-smt-<%=rnd%>');
    if (ta && smt && ta.value && smt.value) obeLoad<%=rnd%>(false);
})();
</script>
