<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// SECURITY CHECK
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali.") + "\"}");
	return;
}
Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = Common.getGeneratedBarCode(7);
String sqlFilterTokoPb = (toko != null) ? (" AND pb.toko = " + toko.getId() + " ") : "";
%>

<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<div class="card border-0 shadow-sm rounded-4 border-top border-warning border-4 mb-4 animate__animated animate__fadeInUp">
    <div class="card-header bg-white border-0 pt-4 pb-3 px-4 border-bottom border-light">
        <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
            <div>
                <h5 class="fw-bold text-dark mb-1"><i class="fas fa-hourglass-half text-warning me-2"></i><%=Common.getBahasaConfig("Bahan Baku & Estimasi Habis")%></h5>
                <small class="text-muted"><%=Common.getBahasaConfig("Perkiraan kapan tiap bahan baku habis, supaya bisa belanja sebelum kehabisan.")%></small>
            </div>
            <select class="form-select form-select-sm rounded-3 shadow-sm border" id="periodeBb<%=rnd%>" style="width: 170px; cursor: pointer;">
                <option value="7"><%=Common.getBahasaConfig("Laju 7 Hari Terakhir")%></option>
                <option value="30" selected><%=Common.getBahasaConfig("Laju 30 Hari Terakhir")%></option>
                <option value="90"><%=Common.getBahasaConfig("Laju 3 Bulan Terakhir")%></option>
            </select>
        </div>
    </div>
    <div class="card-body p-4 bg-light bg-opacity-50">

        <div class="row g-3 mb-4">
            <div class="col-md-3 col-6"><div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3"><span class="text-muted small fw-bold text-uppercase d-block mb-1"><%=Common.getBahasaConfig("Bahan Baku Dipantau")%></span><h4 class="fw-bold text-primary mb-0" id="bbCount<%=rnd%>">0</h4></div></div></div>
            <div class="col-md-3 col-6"><div class="card border-0 shadow-sm rounded-3 h-100 bg-danger bg-gradient text-white"><div class="card-body p-3"><span class="text-white-50 small fw-bold text-uppercase d-block mb-1"><%=Common.getBahasaConfig("Segera Habis (< 7 hari)")%></span><h4 class="fw-bold text-white mb-0" id="bbUrgent<%=rnd%>">0</h4></div></div></div>
            <div class="col-md-3 col-6"><div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3"><span class="text-muted small fw-bold text-uppercase d-block mb-1"><%=Common.getBahasaConfig("Paling Mendesak")%></span><h4 class="fw-bold text-warning mb-0" id="bbTop<%=rnd%>">-</h4><small class="text-muted" id="bbTopNama<%=rnd%>">-</small></div></div></div>
            <div class="col-md-3 col-6"><div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3"><span class="text-muted small fw-bold text-uppercase d-block mb-1"><%=Common.getBahasaConfig("Nilai Stok Bahan Baku")%></span><h4 class="fw-bold text-success mb-0" id="bbNilai<%=rnd%>">Rp 0</h4></div></div></div>
        </div>

        <div class="row g-3 mb-3">
            <div class="col-lg-6"><div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3">
                <h6 class="fw-bold text-secondary mb-1"><i class="fas fa-tachometer-alt me-2"></i><%=Common.getBahasaConfig("Laju Pemakaian per Hari")%></h6>
                <small class="text-muted d-block mb-2"><%=Common.getBahasaConfig("Bahan yang paling cepat berkurang tiap harinya.")%></small>
                <div style="position: relative; height: 280px;"><canvas id="chartLaju<%=rnd%>"></canvas></div>
            </div></div></div>
            <div class="col-lg-6"><div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3">
                <h6 class="fw-bold text-secondary mb-1"><i class="fas fa-calendar-day me-2"></i><%=Common.getBahasaConfig("Perkiraan Hari Stok Tersisa")%></h6>
                <small class="text-muted d-block mb-2"><%=Common.getBahasaConfig("Makin kecil makin mendesak untuk dibeli lagi.")%></small>
                <div style="position: relative; height: 280px;"><canvas id="chartHari<%=rnd%>"></canvas></div>
            </div></div></div>
        </div>

        <div class="card border-0 shadow-sm rounded-3"><div class="card-body p-3">
            <div class="d-flex justify-content-between align-items-center mb-2">
                <h6 class="fw-bold text-secondary mb-0"><i class="fas fa-list me-2"></i><%=Common.getBahasaConfig("Estimasi Bahan Baku Habis")%></h6>
                <button class="btn btn-sm btn-outline-warning text-dark rounded-pill fw-bold" onclick="unduhBbTbl<%=rnd%>()"><i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh Excel")%></button>
            </div>
            <small class="text-muted d-block mb-2"><%=Common.getBahasaConfig("Merah = segera habis, oranye = mulai menipis.")%></small>
            <div class="table-responsive"><table class="table table-sm table-hover align-middle">
                <thead><tr class="small text-muted text-uppercase">
                    <th><%=Common.getBahasaConfig("Bahan Baku")%></th><th class="text-end"><%=Common.getBahasaConfig("Sisa Stok")%></th>
                    <th class="text-end"><%=Common.getBahasaConfig("Terpakai")%></th><th class="text-end"><%=Common.getBahasaConfig("Estimasi Habis")%></th>
                </tr></thead>
                <tbody id="tblBb<%=rnd%>"></tbody>
            </table></div>
        </div></div>

    </div>
</div>

<script>
    var chartLajuObj<%=rnd%> = null, chartHariObj<%=rnd%> = null;
    var dataBb<%=rnd%> = [];

    const rpBb<%=rnd%> = (a) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(a || 0);
    const sqlBb<%=rnd%> = async (sql) => {
        try {
            const r = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sql: sql, action: "sql" }) });
            const j = await r.json();
            return j.data || [];
        } catch (e) { return []; }
    };

    const unduhBbTbl<%=rnd%> = () => {
        const headers = ["Bahan Baku", "Sisa Stok", "Terpakai", "Estimasi Habis (hari)"];
        const rows = dataBb<%=rnd%>.map(d => [d.nama, d.sisa, Math.round(d.terpakai * 100) / 100, (d.hari === Infinity ? '-' : Math.round(d.hari))]);
        try {
            const ws = XLSX.utils.aoa_to_sheet([headers].concat(rows));
            const wb = XLSX.utils.book_new();
            XLSX.utils.book_append_sheet(wb, ws, 'Data');
            XLSX.writeFile(wb, "Estimasi_Bahan_Baku_Habis.xlsx");
        } catch (e) {
            const csv = [headers].concat(rows).map(r => r.map(c => '"' + String(c).replace(/"/g, '""') + '"').join(',')).join('\n');
            const a = document.createElement('a');
            a.href = URL.createObjectURL(new Blob(["﻿" + csv], { type: 'text/csv;charset=utf-8' }));
            a.download = "Estimasi_Bahan_Baku_Habis.csv";
            a.click();
        }
    };

    const loadBahanBaku<%=rnd%> = async () => {
        const hari = parseInt(document.getElementById('periodeBb<%=rnd%>').value) || 30;
        const rows = await sqlBb<%=rnd%>("SELECT COALESCE(pr.nama,'-') AS nama, COALESCE(pr.stok,0) AS sisa, "
            + "COALESCE(SUM(pb.qty),0) AS terpakai, COALESCE(pr.hargabeli,0) AS hb "
            + "FROM koperasi.pemakaian_bahan_baku pb LEFT JOIN koperasi.produk pr ON pr.id = pb.produk "
            + "WHERE pb.waktu >= CURRENT_DATE - INTERVAL '" + hari + " days'<%=sqlFilterTokoPb%> "
            + "GROUP BY pr.id, pr.nama, pr.stok, pr.hargabeli ORDER BY terpakai DESC");

        dataBb<%=rnd%> = rows.map(r => {
            const sisa = parseFloat(r.sisa) || 0, terpakai = parseFloat(r.terpakai) || 0, hb = parseFloat(r.hb) || 0;
            const perHari = terpakai / Math.max(1, hari);
            return { nama: r.nama || '-', sisa: sisa, terpakai: terpakai, hb: hb, perHari: perHari, hari: perHari > 0 ? sisa / perHari : Infinity };
        });

        // Kartu
        let urgent = 0, nilai = 0, topNama = '-', topHari = Infinity;
        dataBb<%=rnd%>.forEach(d => { nilai += d.sisa * d.hb; if (d.hari < 7) urgent++; if (d.hari < topHari) { topHari = d.hari; topNama = d.nama; } });
        document.getElementById('bbCount<%=rnd%>').innerText = dataBb<%=rnd%>.length;
        document.getElementById('bbUrgent<%=rnd%>').innerText = urgent;
        document.getElementById('bbTop<%=rnd%>').innerText = (topHari === Infinity ? '<%=Common.getBahasaConfigJS("Aman")%>' : (Math.round(topHari) + ' <%=Common.getBahasaConfig("hari")%>'));
        document.getElementById('bbTopNama<%=rnd%>').innerText = topNama;
        document.getElementById('bbNilai<%=rnd%>').innerText = rpBb<%=rnd%>(nilai);

        // Tabel (urut paling mendesak)
        const sorted = dataBb<%=rnd%>.slice().sort((a, b) => a.hari - b.hari);
        let html = '';
        if (sorted.length === 0) {
            html = '<tr><td colspan="4" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Belum ada pemakaian bahan baku. Bagian ini terisi otomatis setelah produk ber-resep terjual.")%></td></tr>';
        } else {
            sorted.forEach(d => {
                const cls = d.hari <= 3 ? 'text-danger fw-bolder' : (d.hari <= 7 ? 'text-warning fw-bold' : 'text-dark');
                const hariTxt = d.hari === Infinity ? '<%=Common.getBahasaConfigJS("Aman")%>' : (Math.round(d.hari) + ' <%=Common.getBahasaConfig("hari")%>');
                html += '<tr><td class="fw-semibold">' + d.nama + '</td><td class="text-end ' + cls + '">' + (Math.round(d.sisa * 100) / 100) + '</td><td class="text-end">' + (Math.round(d.terpakai * 100) / 100) + '</td><td class="text-end ' + cls + '">' + hariTxt + '</td></tr>';
            });
        }
        document.getElementById('tblBb<%=rnd%>').innerHTML = html;

        // Chart laju per hari (top 12)
        const topLaju = dataBb<%=rnd%>.slice().sort((a, b) => b.perHari - a.perHari).slice(0, 12);
        renderChartLaju<%=rnd%>(topLaju.map(d => d.nama), topLaju.map(d => Math.round(d.perHari * 100) / 100));

        // Chart hari tersisa (paling mendesak, top 12, abaikan tak terhingga)
        const topHariList = dataBb<%=rnd%>.filter(d => d.hari !== Infinity).sort((a, b) => a.hari - b.hari).slice(0, 12);
        renderChartHari<%=rnd%>(topHariList.map(d => d.nama), topHariList.map(d => Math.round(d.hari)));
    };

    const renderChartLaju<%=rnd%> = (labels, data) => {
        const ctx = document.getElementById('chartLaju<%=rnd%>').getContext('2d');
        if (chartLajuObj<%=rnd%>) chartLajuObj<%=rnd%>.destroy();
        chartLajuObj<%=rnd%> = new Chart(ctx, {
            type: 'bar',
            data: { labels: labels, datasets: [{ label: '<%=Common.getBahasaConfigJS("Per Hari")%>', data: data, backgroundColor: 'rgba(13,110,253,0.8)', borderRadius: 6, barPercentage: 0.7 }] },
            options: { indexAxis: 'y', responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { x: { grid: { color: '#eef2f7' } }, y: { grid: { display: false } } } }
        });
    };

    const renderChartHari<%=rnd%> = (labels, data) => {
        const ctx = document.getElementById('chartHari<%=rnd%>').getContext('2d');
        if (chartHariObj<%=rnd%>) chartHariObj<%=rnd%>.destroy();
        const colors = data.map(v => v <= 3 ? 'rgba(220,53,69,0.85)' : (v <= 7 ? 'rgba(255,193,7,0.85)' : 'rgba(25,135,84,0.85)'));
        chartHariObj<%=rnd%> = new Chart(ctx, {
            type: 'bar',
            data: { labels: labels, datasets: [{ label: '<%=Common.getBahasaConfigJS("Hari")%>', data: data, backgroundColor: colors, borderRadius: 6, barPercentage: 0.7 }] },
            options: { indexAxis: 'y', responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false }, tooltip: { callbacks: { label: c => c.raw + ' <%=Common.getBahasaConfig("hari")%>' } } }, scales: { x: { grid: { color: '#eef2f7' } }, y: { grid: { display: false } } } }
        });
    };

    document.addEventListener("DOMContentLoaded", () => {
        const sel = document.getElementById('periodeBb<%=rnd%>');
        if (sel) sel.addEventListener("change", () => loadBahanBaku<%=rnd%>());
        loadBahanBaku<%=rnd%>();
    });
</script>
