<%@page import="ais.database.model.PenghargaanDosen"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	return;
}
String rnd = Common.getGeneratedBarCode(7);

Dosen currentDosen = tbmuser.ambilDosen();
Long idDosenLogin = (currentDosen != null) ? currentDosen.getId() : null;
Jurusan loginSebagaiJurusan = tbmuser.getJurusan();
Fakultas loginSebagaiFakultas = tbmuser.getFakultas();

String baseWhere = " WHERE p.id IS NOT NULL ";
if (idDosenLogin != null) {
    baseWhere += " AND p.dosen = " + idDosenLogin;
} else if (loginSebagaiJurusan != null) {
    baseWhere += " AND p.jurusan = " + loginSebagaiJurusan.getId();
} else if (loginSebagaiFakultas != null) {
    baseWhere += " AND p.fakultas = " + loginSebagaiFakultas.getId();
}
%>

<div class="animate__animated animate__fadeInUp" id="dashboardKaryaDosen<%=rnd%>">
    <!-- KPI Row -->
    <div class="row g-3 mb-4" id="kpiRow<%=rnd%>">
        <div class="col-12 text-center py-4">
            <div class="spinner-border text-primary"></div>
            <p class="text-muted small mt-2"><%=Common.getBahasaConfig("Memuat ringkasan...")%></p>
        </div>
    </div>

    <!-- Chart: Distribusi Status + Terbaru -->
    <div class="row g-3">
        <div class="col-md-6">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-header bg-white border-0 pt-3 pb-0 px-4">
                    <h6 class="fw-bold text-dark mb-0"><i class="fas fa-chart-pie me-2 text-primary"></i><%=Common.getBahasaConfig("Distribusi Status Persetujuan")%></h6>
                </div>
                <div class="card-body px-4 pb-3">
                    <div id="chartStatus<%=rnd%>" style="min-height:160px;"></div>
                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-header bg-white border-0 pt-3 pb-0 px-4">
                    <h6 class="fw-bold text-dark mb-0"><i class="fas fa-list me-2 text-info"></i><%=Common.getBahasaConfig("Terbaru")%></h6>
                </div>
                <div class="card-body px-4 pb-3">
                    <div id="listTerbaru<%=rnd%>" style="max-height:200px;overflow-y:auto;">
                        <div class="text-center text-muted small py-3"><div class="spinner-border spinner-border-sm text-info me-2"></div><%=Common.getBahasaConfig("Memuat...")%></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
(function(){
    var ROOT = '<%=Common.ROOT%>';
    var baseWhere = '<%=baseWhere.replace("'", "\\'")%>';

    async function fetchSql(sql) {
        try {
            var res = await fetch(ROOT + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: 'sql', sql: sql }) });
            var result = await res.json();
            return result.data || [];
        } catch(e) { return []; }
    }

    function fmtBadge(st) {
        var map = { 'Disetujui': 'bg-success', 'Ditolak': 'bg-danger', 'Sedang diproses': 'bg-info text-dark', 'Belum diproses': 'bg-warning text-dark' };
        return '<span class="badge ' + (map[st] || 'bg-secondary') + '">' + (st || '-') + '</span>';
    }

    function renderKpi(total, disetujui, proses) {
        var pending = total - disetujui - proses;
        var pct = total > 0 ? Math.round(disetujui * 100 / total) : 0;
        document.getElementById('kpiRow<%=rnd%>').innerHTML =
            '<div class="col-6 col-md-3">' +
            '<div class="card border-0 shadow-sm rounded-4 text-center p-3 border-top border-primary border-3">' +
            '<div style="font-size:2rem;font-weight:900;color:#0d6efd;">' + total + '</div>' +
            '<small class="text-muted fw-semibold"><%=Common.getBahasaConfig("Total Karya")%></small>' +
            '</div></div>' +

            '<div class="col-6 col-md-3">' +
            '<div class="card border-0 shadow-sm rounded-4 text-center p-3 border-top border-success border-3">' +
            '<div style="font-size:2rem;font-weight:900;color:#198754;">' + disetujui + '</div>' +
            '<small class="text-muted fw-semibold"><%=Common.getBahasaConfig("Disetujui")%></small>' +
            '</div></div>' +

            '<div class="col-6 col-md-3">' +
            '<div class="card border-0 shadow-sm rounded-4 text-center p-3 border-top border-warning border-3">' +
            '<div style="font-size:2rem;font-weight:900;color:#ffc107;">' + pending + '</div>' +
            '<small class="text-muted fw-semibold"><%=Common.getBahasaConfig("Belum/Proses")%></small>' +
            '</div></div>' +

            '<div class="col-6 col-md-3">' +
            '<div class="card border-0 shadow-sm rounded-4 text-center p-3 border-top border-info border-3">' +
            '<div style="font-size:2rem;font-weight:900;color:#0dcaf0;">' + pct + '%</div>' +
            '<small class="text-muted fw-semibold"><%=Common.getBahasaConfig("Tingkat Persetujuan")%></small>' +
            '</div></div>';
    }

    function renderStatus(rows) {
        var el = document.getElementById('chartStatus<%=rnd%>');
        if (!rows || rows.length === 0) { el.innerHTML = '<p class="text-muted text-center small py-4"><%=Common.getBahasaConfig("Tidak ada data.")%></p>'; return; }
        var total = rows.reduce(function(s, r) { return s + parseInt(r.jml || 0); }, 0);
        var colors = { 'Disetujui': '#198754', 'Ditolak': '#dc3545', 'Sedang diproses': '#0dcaf0', 'Belum diproses': '#ffc107' };
        var html = '<div class="d-flex flex-column gap-2 mt-2">';
        rows.forEach(function(r) {
            var jml = parseInt(r.jml || 0);
            var pct = total > 0 ? Math.round(jml * 100 / total) : 0;
            var st = r.status || 'Belum diproses';
            var clr = colors[st] || '#6c757d';
            html += '<div>' +
                '<div class="d-flex justify-content-between small fw-semibold mb-1"><span>' + st + '</span><span>' + jml + ' (' + pct + '%)</span></div>' +
                '<div class="progress" style="height:10px;border-radius:5px;"><div class="progress-bar" style="width:' + pct + '%;background:' + clr + ';border-radius:5px;"></div></div>' +
                '</div>';
        });
        html += '</div>';
        el.innerHTML = html;
    }

    function renderTerbaru(rows) {
        var el = document.getElementById('listTerbaru<%=rnd%>');
        if (!rows || rows.length === 0) { el.innerHTML = '<p class="text-muted text-center small py-3"><%=Common.getBahasaConfig("Belum ada data.")%></p>'; return; }
        var html = '<ul class="list-group list-group-flush">';
        rows.forEach(function(r) {
            html += '<li class="list-group-item px-0 py-2 border-0 border-bottom">' +
                '<div class="d-flex justify-content-between align-items-start gap-2">' +
                '<div class="fw-semibold text-dark" style="font-size:.85rem;line-height:1.3;">' + (r.namaprestasi || r.nama || '-') + '</div>' +
                fmtBadge(r.status) +
                '</div>' +
                '<small class="text-muted">' + (r.nama_dosen || '') + '</small>' +
                '</li>';
        });
        html += '</ul>';
        el.innerHTML = html;
    }

    async function muatDashboard<%=rnd%>() {
        var sqlTotal = "SELECT COUNT(*) as total, SUM(CASE WHEN p.status='Disetujui' THEN 1 ELSE 0 END) as disetujui, SUM(CASE WHEN p.status='Sedang diproses' THEN 1 ELSE 0 END) as proses FROM public.penghargaan_dosen p " + baseWhere;
        var sqlStatus = "SELECT p.status, COUNT(*) as jml FROM public.penghargaan_dosen p " + baseWhere + " GROUP BY p.status ORDER BY jml DESC";
        var sqlTerbaru = "SELECT p.namaprestasi, p.nama, p.status, d.nama as nama_dosen FROM public.penghargaan_dosen p LEFT JOIN public.dosen d ON p.dosen = d.id " + baseWhere + " ORDER BY p.id DESC LIMIT 10";

        var totals = await fetchSql(sqlTotal);
        if (totals.length > 0) {
            var t = totals[0];
            renderKpi(parseInt(t.total || 0), parseInt(t.disetujui || 0), parseInt(t.proses || 0));
        }
        renderStatus(await fetchSql(sqlStatus));
        renderTerbaru(await fetchSql(sqlTerbaru));
    }

    document.addEventListener('DOMContentLoaded', function() {
        muatDashboard<%=rnd%>();
    });
})();
</script>
