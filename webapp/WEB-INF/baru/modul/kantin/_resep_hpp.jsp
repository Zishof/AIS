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
String sqlFilterTokoP = (toko != null) ? (" AND p.toko = " + toko.getId() + " ") : "";
String sqlFilterTokoPb = (toko != null) ? (" AND pb.toko = " + toko.getId() + " ") : "";
%>

<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<style>
    /* Tabel HPP/Resep & Pemakaian Bahan Baku: di ponsel, baris tabel dilipat jadi kartu
       (teknik "responsive table -> card" standar) drpd scroll horizontal sempit. */
    @media (max-width: 767.98px) {
        table.hpp-responsive<%=rnd%> thead { display: none; }
        table.hpp-responsive<%=rnd%>, table.hpp-responsive<%=rnd%> tbody,
        table.hpp-responsive<%=rnd%> tr, table.hpp-responsive<%=rnd%> td { display: block; width: 100%; }
        table.hpp-responsive<%=rnd%> tr { margin-bottom: 10px; border: 1px solid #eef0f3; border-radius: 12px; padding: 4px 10px; background: #fff; }
        table.hpp-responsive<%=rnd%> td { text-align: right !important; padding: 7px 2px; border: none !important; border-bottom: 1px dashed #f1f3f5 !important; font-size: 13.5px; }
        table.hpp-responsive<%=rnd%> td:last-child { border-bottom: none !important; }
        table.hpp-responsive<%=rnd%> td::before { content: attr(data-label); float: left; font-weight: 700; color: #868e96; font-size: 11.5px; text-transform: uppercase; }
    }
    .hpp-badge<%=rnd%> { display: inline-block; padding: .28em .65em; border-radius: 999px; font-size: 11px; font-weight: 800; }
    .hpp-badge-sehat<%=rnd%> { background: rgba(25,135,84,.12); color: #146c43; }
    .hpp-badge-tipis<%=rnd%> { background: rgba(255,193,7,.18); color: #997404; }
    .hpp-badge-rugi<%=rnd%> { background: rgba(220,53,69,.12); color: #b02a37; }
</style>

<div class="card border-0 shadow-sm rounded-4 border-top border-success border-4 mb-4 animate__animated animate__fadeInUp">
    <div class="card-header bg-white border-0 pt-4 pb-3 px-4 border-bottom border-light">
        <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
            <div>
                <h5 class="fw-bold text-dark mb-1"><i class="fas fa-mortar-pestle text-success me-2"></i><%=Common.getBahasaConfig("Resep, HPP & Margin")%></h5>
                <small class="text-muted"><%=Common.getBahasaConfig("Menghitung modal tiap menu langsung dari resep bahan bakunya, supaya kelihatan menu mana yang untungnya sehat dan mana yang nyaris tidak untung.")%></small>
            </div>
            <select class="form-select form-select-sm rounded-3 shadow-sm border" id="periodeResep<%=rnd%>" style="width: 170px; cursor: pointer;">
                <option value="7"><%=Common.getBahasaConfig("7 Hari Terakhir")%></option>
                <option value="30" selected><%=Common.getBahasaConfig("30 Hari Terakhir")%></option>
                <option value="90"><%=Common.getBahasaConfig("3 Bulan Terakhir")%></option>
            </select>
        </div>
    </div>
    <div class="card-body p-4 bg-light bg-opacity-50">

        <div class="row g-3 mb-4">
            <div class="col-md-3 col-6"><div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3"><span class="text-muted small fw-bold text-uppercase d-block mb-1"><%=Common.getBahasaConfig("Menu Ber-Resep")%></span><h4 class="fw-bold text-primary mb-0" id="rsMenu<%=rnd%>">0</h4></div></div></div>
            <div class="col-md-3 col-6"><div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3"><span class="text-muted small fw-bold text-uppercase d-block mb-1"><%=Common.getBahasaConfig("Rata-rata Margin")%></span><h4 class="fw-bold text-success mb-0" id="rsAvg<%=rnd%>">0%</h4></div></div></div>
            <div class="col-md-3 col-6"><div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3"><span class="text-muted small fw-bold text-uppercase d-block mb-1"><%=Common.getBahasaConfig("Margin Tertipis")%></span><h4 class="fw-bold text-danger mb-0" id="rsMin<%=rnd%>">0%</h4><small class="text-muted" id="rsMinNama<%=rnd%>">-</small></div></div></div>
            <div class="col-md-3 col-6"><div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3"><span class="text-muted small fw-bold text-uppercase d-block mb-1"><%=Common.getBahasaConfig("Nilai Bahan Baku Terpakai")%></span><h4 class="fw-bold text-warning mb-0" id="rsNilai<%=rnd%>">Rp 0</h4></div></div></div>
        </div>

        <div class="row g-3 mb-3">
            <div class="col-lg-6">
                <div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3">
                    <h6 class="fw-bold text-secondary mb-1"><i class="fas fa-chart-bar me-2"></i><%=Common.getBahasaConfig("Untung (Margin) Tiap Menu")%></h6>
                    <small class="text-muted d-block mb-2"><%=Common.getBahasaConfig("Menu mana yang paling untung, dan mana yang nyaris tidak untung.")%></small>
                    <div style="position: relative; height: 280px;"><canvas id="chartMargin<%=rnd%>"></canvas></div>
                </div></div>
            </div>
            <div class="col-lg-6">
                <div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3">
                    <h6 class="fw-bold text-secondary mb-1"><i class="fas fa-blender me-2"></i><%=Common.getBahasaConfig("Bahan Baku Paling Banyak Terpakai")%></h6>
                    <small class="text-muted d-block mb-2"><%=Common.getBahasaConfig("Bahan yang paling cepat habis — pastikan stoknya selalu siap.")%></small>
                    <div style="position: relative; height: 280px;"><canvas id="chartPakai<%=rnd%>"></canvas></div>
                </div></div>
            </div>
        </div>

        <div class="card border-0 shadow-sm rounded-3 mb-3"><div class="card-body p-3">
            <div class="d-flex justify-content-between align-items-center mb-2">
                <h6 class="fw-bold text-secondary mb-0"><i class="fas fa-list me-2"></i><%=Common.getBahasaConfig("Rekap Resep, HPP & Margin per Menu")%></h6>
                <button class="btn btn-sm btn-outline-success rounded-pill fw-bold" onclick="unduhResepTbl<%=rnd%>()"><i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh Excel")%></button>
            </div>
            <small class="text-muted d-block mb-2"><%=Common.getBahasaConfig("Modal (HPP) dihitung otomatis: harga tiap bahan baku dikalikan jumlah yang dipakai di resep, dijumlahkan jadi modal per menu — bukan angka yang diisi manual.")%></small>
            <div class="table-responsive"><table class="table table-sm table-hover align-middle hpp-responsive<%=rnd%>">
                <thead><tr class="small text-muted text-uppercase">
                    <th><%=Common.getBahasaConfig("Tenant")%></th><th><%=Common.getBahasaConfig("Menu")%></th>
                    <th class="text-end"><%=Common.getBahasaConfig("Bahan")%></th>
                    <th class="text-end"><%=Common.getBahasaConfig("HPP (Modal)")%></th><th class="text-end"><%=Common.getBahasaConfig("Harga Jual")%></th>
                    <th class="text-end"><%=Common.getBahasaConfig("Untung")%></th><th class="text-end"><%=Common.getBahasaConfig("Margin")%></th>
                </tr></thead>
                <tbody id="tblResep<%=rnd%>"></tbody>
            </table></div>
        </div></div>

        <div class="card border-0 shadow-sm rounded-3"><div class="card-body p-3">
            <div class="d-flex justify-content-between align-items-center mb-2">
                <h6 class="fw-bold text-secondary mb-0"><i class="fas fa-receipt me-2"></i><%=Common.getBahasaConfig("Rekap Pemakaian Bahan Baku")%></h6>
                <button class="btn btn-sm btn-outline-success rounded-pill fw-bold" onclick="unduhPakaiTbl<%=rnd%>()"><i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh Excel")%></button>
            </div>
            <small class="text-muted d-block mb-2"><%=Common.getBahasaConfig("Berapa banyak tiap bahan baku terpakai beserta nilainya.")%></small>
            <div class="table-responsive"><table class="table table-sm table-hover align-middle hpp-responsive<%=rnd%>">
                <thead><tr class="small text-muted text-uppercase">
                    <th><%=Common.getBahasaConfig("Bahan Baku")%></th><th class="text-end"><%=Common.getBahasaConfig("Qty Terpakai")%></th><th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th>
                </tr></thead>
                <tbody id="tblPakai<%=rnd%>"></tbody>
            </table></div>
        </div></div>

    </div>
</div>

<script>
    var chartMarginObj<%=rnd%> = null, chartPakaiObj<%=rnd%> = null;
    var dataResep<%=rnd%> = [], dataPakai<%=rnd%> = [];

    const rpResep<%=rnd%> = (a) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(a || 0);

    const sqlResep<%=rnd%> = async (sql) => {
        try {
            const r = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sql: sql, action: "sql" }) });
            const j = await r.json();
            return j.data || [];
        } catch (e) { return []; }
    };

    const unduhExcel<%=rnd%> = (headers, rows, filename) => {
        try {
            const ws = XLSX.utils.aoa_to_sheet([headers].concat(rows));
            const wb = XLSX.utils.book_new();
            XLSX.utils.book_append_sheet(wb, ws, 'Data');
            XLSX.writeFile(wb, filename + '.xlsx');
        } catch (e) {
            const csv = [headers].concat(rows).map(r => r.map(c => '"' + String(c).replace(/"/g, '""') + '"').join(',')).join('\n');
            const a = document.createElement('a');
            a.href = URL.createObjectURL(new Blob(["﻿" + csv], { type: 'text/csv;charset=utf-8' }));
            a.download = filename + '.csv';
            a.click();
        }
    };

    const unduhResepTbl<%=rnd%> = () => unduhExcel<%=rnd%>(
        ["Tenant", "Menu", "Jumlah Bahan", "HPP (Modal)", "Harga Jual", "Untung", "Margin %"],
        dataResep<%=rnd%>.map(d => [d.tenant, d.menu, d.jmlBahan, d.hpp, d.jual, d.untung, Math.round(d.margin)]), "Resep_HPP_Margin");
    const unduhPakaiTbl<%=rnd%> = () => unduhExcel<%=rnd%>(
        ["Bahan Baku", "Qty Terpakai", "Nilai"], dataPakai<%=rnd%>.map(d => [d.nama, d.qty, d.nilai]), "Pemakaian_Bahan_Baku");

    const labelMargin<%=rnd%> = (margin) => {
        if (margin < 0) return '<span class="hpp-badge<%=rnd%> hpp-badge-rugi<%=rnd%>"><%=Common.getBahasaConfig("Rugi")%></span>';
        if (margin < 15) return '<span class="hpp-badge<%=rnd%> hpp-badge-tipis<%=rnd%>"><%=Common.getBahasaConfig("Tipis")%></span>';
        return '<span class="hpp-badge<%=rnd%> hpp-badge-sehat<%=rnd%>"><%=Common.getBahasaConfig("Sehat")%></span>';
    };

    // Ambil harga bahan baku sekaligus utk sekumpulan id produk (satu query, bukan tanya satu-satu per resep)
    const ambilPetaHargaBahan<%=rnd%> = async (idSet) => {
        const peta = {};
        if (idSet.size === 0) return peta;
        const ids = Array.from(idSet).filter(x => Number.isFinite(x)).join(',');
        if (!ids) return peta;
        const rows = await sqlResep<%=rnd%>("SELECT id, COALESCE(hargabeli,0) AS hargabeli FROM koperasi.produk WHERE id IN (" + ids + ")");
        rows.forEach(r => { peta[parseInt(r.id)] = parseFloat(r.hargabeli) || 0; });
        return peta;
    };

    const loadResepHpp<%=rnd%> = async () => {
        const periode = document.getElementById('periodeResep<%=rnd%>').value;

        // 1) Produk ber-resep -- tarik RESEPNYA (bahanbaku, berisi [{produk,qty}, ...]), BUKAN hargabeli
        //    produk itu sendiri. HPP menu = jumlah (qty resep x harga beli tiap bahan baku), persis pola
        //    parsing JSON yg sudah dipakai BahanBakuUtil.konsumsiBahanBaku (key "produk" & "qty").
        const resepMentah = await sqlResep<%=rnd%>("SELECT p.id, COALESCE(t.nama,'-') AS tenant, p.nama AS menu, "
            + "p.bahanbaku AS resep, COALESCE(p.hargajual,0) AS jual "
            + "FROM koperasi.produk p LEFT JOIN koperasi.toko t ON t.id = p.toko "
            + "WHERE p.bahanbaku IS NOT NULL AND TRIM(p.bahanbaku) NOT IN ('','[]')<%=sqlFilterTokoP%> ORDER BY p.nama");

        const resepTerurai = resepMentah.map(r => {
            let items = [];
            try { items = JSON.parse(r.resep) || []; } catch (e) { items = []; }
            if (!Array.isArray(items)) items = [];
            return { tenant: r.tenant || '-', menu: r.menu || '-', jual: parseFloat(r.jual) || 0, items: items };
        });

        // Kumpulkan SEMUA id bahan baku yg dipakai di seluruh resep, tarik harganya sekali jalan
        const idBahan = new Set();
        resepTerurai.forEach(r => r.items.forEach(it => { const pid = parseInt(it.produk); if (pid) idBahan.add(pid); }));
        const petaHarga = await ambilPetaHargaBahan<%=rnd%>(idBahan);

        dataResep<%=rnd%> = resepTerurai.map(r => {
            let hpp = 0;
            r.items.forEach(it => {
                const qty = parseFloat(it.qty) || 0;
                const harga = petaHarga[parseInt(it.produk)] || 0;
                hpp += qty * harga;
            });
            const untung = r.jual - hpp;
            return { tenant: r.tenant, menu: r.menu, hpp: hpp, jual: r.jual, untung: untung,
                margin: r.jual > 0 ? (untung / r.jual * 100) : 0, jmlBahan: r.items.length };
        });

        // Kartu margin
        let totalM = 0, nM = 0, minM = null, minN = '-';
        dataResep<%=rnd%>.forEach(d => { if (d.jual > 0) { totalM += d.margin; nM++; if (minM === null || d.margin < minM) { minM = d.margin; minN = d.menu; } } });
        document.getElementById('rsMenu<%=rnd%>').innerText = dataResep<%=rnd%>.length;
        document.getElementById('rsAvg<%=rnd%>').innerText = Math.round(nM > 0 ? totalM / nM : 0) + '%';
        document.getElementById('rsMin<%=rnd%>').innerText = Math.round(minM === null ? 0 : minM) + '%';
        document.getElementById('rsMinNama<%=rnd%>').innerText = minN;

        // Tabel resep
        let htmlR = '';
        if (dataResep<%=rnd%>.length === 0) {
            htmlR = '<tr><td colspan="7" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Belum ada menu ber-resep. Tambahkan bahan baku di Pengaturan Barang.")%></td></tr>';
        } else {
            dataResep<%=rnd%>.forEach(d => {
                const mc = d.margin < 0 ? 'text-danger' : (d.margin < 15 ? 'text-warning' : 'text-success');
                htmlR += '<tr>'
                    + '<td data-label="<%=Common.getBahasaConfig("Tenant")%>" class="fw-semibold">' + d.tenant + '</td>'
                    + '<td data-label="<%=Common.getBahasaConfig("Menu")%>">' + d.menu + '</td>'
                    + '<td data-label="<%=Common.getBahasaConfig("Bahan")%>" class="text-end text-muted">' + d.jmlBahan + '</td>'
                    + '<td data-label="<%=Common.getBahasaConfig("HPP (Modal)")%>" class="text-end text-muted">' + rpResep<%=rnd%>(d.hpp) + '</td>'
                    + '<td data-label="<%=Common.getBahasaConfig("Harga Jual")%>" class="text-end">' + rpResep<%=rnd%>(d.jual) + '</td>'
                    + '<td data-label="<%=Common.getBahasaConfig("Untung")%>" class="text-end fw-bold ' + mc + '">' + rpResep<%=rnd%>(d.untung) + '</td>'
                    + '<td data-label="<%=Common.getBahasaConfig("Margin")%>" class="text-end fw-bolder ' + mc + '">' + Math.round(d.margin) + '% ' + labelMargin<%=rnd%>(d.margin) + '</td>'
                    + '</tr>';
            });
        }
        document.getElementById('tblResep<%=rnd%>').innerHTML = htmlR;

        // Chart margin (top 12 by margin desc)
        const sortM = dataResep<%=rnd%>.slice().sort((a, b) => b.margin - a.margin).slice(0, 12);
        renderChartMargin<%=rnd%>(sortM.map(d => d.menu), sortM.map(d => Math.round(d.margin)));

        // 2) Pemakaian bahan baku (periode)
        const pakai = await sqlResep<%=rnd%>("SELECT COALESCE(pr.nama,'-') AS nama, COALESCE(SUM(pb.qty),0) AS qty, "
            + "COALESCE(SUM(pb.qty*COALESCE(pr.hargabeli,0)),0) AS nilai "
            + "FROM koperasi.pemakaian_bahan_baku pb LEFT JOIN koperasi.produk pr ON pr.id = pb.produk "
            + "WHERE pb.waktu >= CURRENT_DATE - INTERVAL '" + periode + " days'<%=sqlFilterTokoPb%> GROUP BY pr.nama ORDER BY qty DESC");
        dataPakai<%=rnd%> = pakai.map(r => ({ nama: r.nama || '-', qty: parseFloat(r.qty) || 0, nilai: parseFloat(r.nilai) || 0 }));

        let nilaiTot = 0;
        dataPakai<%=rnd%>.forEach(d => nilaiTot += d.nilai);
        document.getElementById('rsNilai<%=rnd%>').innerText = rpResep<%=rnd%>(nilaiTot);

        let htmlP = '';
        if (dataPakai<%=rnd%>.length === 0) {
            htmlP = '<tr><td colspan="3" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Belum ada pemakaian bahan baku pada periode ini.")%></td></tr>';
        } else {
            dataPakai<%=rnd%>.forEach(d => {
                htmlP += '<tr>'
                    + '<td data-label="<%=Common.getBahasaConfig("Bahan Baku")%>" class="fw-semibold">' + d.nama + '</td>'
                    + '<td data-label="<%=Common.getBahasaConfig("Qty Terpakai")%>" class="text-end">' + (Math.round(d.qty * 100) / 100) + '</td>'
                    + '<td data-label="<%=Common.getBahasaConfig("Nilai")%>" class="text-end fw-bold text-success">' + rpResep<%=rnd%>(d.nilai) + '</td>'
                    + '</tr>';
            });
        }
        document.getElementById('tblPakai<%=rnd%>').innerHTML = htmlP;

        const topP = dataPakai<%=rnd%>.slice(0, 12);
        renderChartPakai<%=rnd%>(topP.map(d => d.nama), topP.map(d => Math.round(d.qty * 100) / 100));
    };

    const renderChartMargin<%=rnd%> = (labels, data) => {
        const ctx = document.getElementById('chartMargin<%=rnd%>').getContext('2d');
        if (chartMarginObj<%=rnd%>) chartMarginObj<%=rnd%>.destroy();
        const colors = data.map(v => v < 0 ? 'rgba(220,53,69,0.85)' : (v < 15 ? 'rgba(255,193,7,0.85)' : 'rgba(25,135,84,0.85)'));
        chartMarginObj<%=rnd%> = new Chart(ctx, {
            type: 'bar',
            data: { labels: labels, datasets: [{ label: 'Margin %', data: data, backgroundColor: colors, borderRadius: 6, barPercentage: 0.7 }] },
            options: { indexAxis: 'y', responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false }, tooltip: { callbacks: { label: c => c.raw + '%' } } }, scales: { x: { ticks: { callback: v => v + '%' }, grid: { color: '#eef2f7' } }, y: { grid: { display: false } } } }
        });
    };

    const renderChartPakai<%=rnd%> = (labels, data) => {
        const ctx = document.getElementById('chartPakai<%=rnd%>').getContext('2d');
        if (chartPakaiObj<%=rnd%>) chartPakaiObj<%=rnd%>.destroy();
        chartPakaiObj<%=rnd%> = new Chart(ctx, {
            type: 'bar',
            data: { labels: labels, datasets: [{ label: '<%=Common.getBahasaConfigJS("Qty Terpakai")%>', data: data, backgroundColor: 'rgba(13,110,253,0.8)', borderRadius: 6, barPercentage: 0.7 }] },
            options: { indexAxis: 'y', responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { x: { grid: { color: '#eef2f7' } }, y: { grid: { display: false } } } }
        });
    };

    document.addEventListener("DOMContentLoaded", () => {
        const sel = document.getElementById('periodeResep<%=rnd%>');
        if (sel) sel.addEventListener("change", () => loadResepHpp<%=rnd%>());
        loadResepHpp<%=rnd%>();
    });
</script>
