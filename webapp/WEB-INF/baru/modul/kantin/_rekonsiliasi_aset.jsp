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
%>

<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<div class="card border-0 shadow-sm rounded-4 border-top border-info border-4 mb-4 animate__animated animate__fadeInUp">
    <div class="card-header bg-white border-0 pt-4 pb-3 px-4 border-bottom border-light">
        <div>
            <h5 class="fw-bold text-dark mb-1"><i class="fas fa-balance-scale text-info me-2"></i><%=Common.getBahasaConfig("Rekonsiliasi Stok Persediaan (Aset ↔ Kantin)")%></h5>
            <small class="text-muted"><%=Common.getBahasaConfig("Membandingkan stok versi kantin dengan stok persediaan di modul Aset, untuk menemukan selisih.")%></small>
        </div>
    </div>
    <div class="card-body p-4 bg-light bg-opacity-50">

        <div class="row g-3 mb-3">
            <div class="col-md-4 col-6"><div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3"><span class="text-muted small fw-bold text-uppercase d-block mb-1"><%=Common.getBahasaConfig("Produk Tertaut")%></span><h4 class="fw-bold text-primary mb-0" id="rkCount<%=rnd%>">0</h4></div></div></div>
            <div class="col-md-4 col-6"><div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3"><span class="text-muted small fw-bold text-uppercase d-block mb-1"><%=Common.getBahasaConfig("Stok Cocok")%></span><h4 class="fw-bold text-success mb-0" id="rkCocok<%=rnd%>">0</h4></div></div></div>
            <div class="col-md-4 col-12"><div class="card border-0 shadow-sm rounded-3 h-100"><div class="card-body p-3"><span class="text-muted small fw-bold text-uppercase d-block mb-1"><%=Common.getBahasaConfig("Perlu Dicek")%></span><h4 class="fw-bold text-danger mb-0" id="rkBeda<%=rnd%>">0</h4></div></div></div>
        </div>

        <div class="card border-0 shadow-sm rounded-3"><div class="card-body p-3">
            <div class="d-flex justify-content-between align-items-center mb-2">
                <h6 class="fw-bold text-secondary mb-0"><i class="fas fa-list me-2"></i><%=Common.getBahasaConfig("Daftar Rekonsiliasi")%></h6>
                <button class="btn btn-sm btn-outline-info rounded-pill fw-bold" onclick="unduhRekonTbl<%=rnd%>()"><i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh Excel")%></button>
            </div>
            <div class="table-responsive"><table class="table table-sm table-hover align-middle">
                <thead><tr class="small text-muted text-uppercase">
                    <th><%=Common.getBahasaConfig("Tenant")%></th><th><%=Common.getBahasaConfig("Produk")%></th>
                    <th><%=Common.getBahasaConfig("Barang Persediaan (Aset)")%></th>
                    <th class="text-end"><%=Common.getBahasaConfig("Stok Kantin")%></th><th class="text-end"><%=Common.getBahasaConfig("Stok Aset")%></th><th class="text-end"><%=Common.getBahasaConfig("Selisih")%></th>
                </tr></thead>
                <tbody id="tblRekon<%=rnd%>"></tbody>
            </table></div>
        </div></div>

    </div>
</div>

<script>
    var dataRekon<%=rnd%> = [];
    const sqlRekon<%=rnd%> = async (sql) => {
        try {
            const r = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sql: sql, action: "sql" }) });
            const j = await r.json();
            return j.data || [];
        } catch (e) { return []; }
    };

    const unduhRekonTbl<%=rnd%> = () => {
        const headers = ["Tenant", "Produk", "Barang Persediaan (Aset)", "Stok Kantin", "Stok Aset", "Selisih"];
        const rows = dataRekon<%=rnd%>.map(d => [d.tenant, d.produk, d.aset, d.stokKantin, d.stokAset, d.selisih]);
        try {
            const ws = XLSX.utils.aoa_to_sheet([headers].concat(rows));
            const wb = XLSX.utils.book_new();
            XLSX.utils.book_append_sheet(wb, ws, 'Data');
            XLSX.writeFile(wb, "Rekonsiliasi_Stok_Aset_Kantin.xlsx");
        } catch (e) {
            const csv = [headers].concat(rows).map(r => r.map(c => '"' + String(c).replace(/"/g, '""') + '"').join(',')).join('\n');
            const a = document.createElement('a');
            a.href = URL.createObjectURL(new Blob(["﻿" + csv], { type: 'text/csv;charset=utf-8' }));
            a.download = "Rekonsiliasi_Stok_Aset_Kantin.csv";
            a.click();
        }
    };

    const loadRekon<%=rnd%> = async () => {
        const rows = await sqlRekon<%=rnd%>("SELECT COALESCE(t.nama,'-') AS tenant, p.nama AS produk, "
            + "COALESCE(ma.kode,'') || ' - ' || COALESCE(ma.nama,'') AS aset, COALESCE(p.stok,0) AS stok_kantin, "
            + "COALESCE((SELECT SUM((a.qty+a.qtybonus)*b.jenis) FROM asset.detail_transaksi_asset a "
            + "INNER JOIN library.kode_transaksi b ON a.kode_transaksi = b.id WHERE a.master_asset = p.master_asset),0) AS stok_aset "
            + "FROM koperasi.produk p INNER JOIN asset.master_asset ma ON ma.id = p.master_asset "
            + "LEFT JOIN koperasi.toko t ON t.id = p.toko WHERE p.master_asset IS NOT NULL<%=sqlFilterTokoP%> ORDER BY p.nama");

        dataRekon<%=rnd%> = rows.map(r => {
            const sk = parseFloat(r.stok_kantin) || 0, sa = parseFloat(r.stok_aset) || 0;
            return { tenant: r.tenant || '-', produk: r.produk || '-', aset: r.aset || '-', stokKantin: sk, stokAset: sa, selisih: Math.round((sk - sa) * 100) / 100 };
        });

        let cocok = 0, beda = 0;
        dataRekon<%=rnd%>.forEach(d => { if (Math.abs(d.selisih) < 0.001) cocok++; else beda++; });
        document.getElementById('rkCount<%=rnd%>').innerText = dataRekon<%=rnd%>.length;
        document.getElementById('rkCocok<%=rnd%>').innerText = cocok;
        document.getElementById('rkBeda<%=rnd%>').innerText = beda;

        let html = '';
        if (dataRekon<%=rnd%>.length === 0) {
            html = '<tr><td colspan="6" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Belum ada produk kantin yang ditautkan ke barang persediaan (Aset). Tautkan di Pengaturan Barang.")%></td></tr>';
        } else {
            dataRekon<%=rnd%>.forEach(d => {
                const cls = Math.abs(d.selisih) > 0.001 ? 'text-danger fw-bolder' : 'text-success fw-bold';
                html += '<tr><td class="fw-semibold">' + d.tenant + '</td><td>' + d.produk + '</td><td class="small text-muted">' + d.aset + '</td><td class="text-end">' + d.stokKantin + '</td><td class="text-end">' + d.stokAset + '</td><td class="text-end ' + cls + '">' + d.selisih + '</td></tr>';
            });
        }
        document.getElementById('tblRekon<%=rnd%>').innerHTML = html;
    };

    document.addEventListener("DOMContentLoaded", () => { loadRekon<%=rnd%>(); });
</script>
