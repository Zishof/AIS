<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.EbisnisMenuKatalog"%>
<%@ page import="ais.database.model.Tbmrole"%>
<%@ page import="ais.database.model.Tbmuser"%>
<%@ page import="org.json.JSONObject"%>
<%
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");
    Tbmuser pengguna = Common.getCurrentUser(request);
    if (pengguna == null || Boolean.FALSE.equals(pengguna.getAktif())) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return;
    }
    Tbmrole role = pengguna.hakAkses();
    JSONObject akses = EbisnisMenuKatalog.urai(role == null ? null : role.getEbisnisMenu());
    JSONObject menu = akses.optJSONObject("menu");
    String[] kunciInventory = {"master_supplier","master_customer","master_sales","persediaan","harga",
        "hutang","penjualan_sales","piutang","surat_perintah_sales","nota_sales","biaya_sales",
        "pembelian_sales","rekonsiliasi_sales","kas_jurnal","laba_rugi","laporan_inventory_sales"};
    boolean bolehMasuk = akses.optBoolean("landingInventory", false);
    if (menu != null) for (int i = 0; i < kunciInventory.length; i++) {
        if (menu.optBoolean(kunciInventory[i], false)) { bolehMasuk = true; break; }
    }
    if (!bolehMasuk) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Grup pengguna tidak memiliki akses Inventory & Sales.");
        return;
    }
    String ctx = request.getContextPath();
%>
<jsp:include page="/WEB-INF/baru/include/header.jsp" />
<style><%@ include file="inventory.css" %></style>
<style>@media(max-width:520px){.si-toolbar-actions{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));overflow:visible}.si-toolbar-actions .si-button{width:100%;padding:0 8px}}</style>
<body class="si-body">
<div id="siApp" class="si-app" data-api="<%=ctx%>/Api_eBisnis" data-user="<%=pengguna.getUserId()%>" data-initial="<%=request.getAttribute("inventoryInitialScreen") == null ? "01" : request.getAttribute("inventoryInitialScreen")%>">
    <aside class="si-sidebar" id="siSidebar">
        <div class="si-brand">
            <span class="si-brand-mark"><i class="fas fa-boxes"></i></span>
            <span><strong>eBisnis</strong><small>Inventory &amp; Sales</small></span>
            <button class="si-icon-button si-close-nav" id="siCloseNav" aria-label="Tutup menu"><i class="fas fa-times"></i></button>
        </div>
        <label class="si-nav-search"><i class="fas fa-search"></i><input id="siMenuSearch" placeholder="Cari dari 48 layar" autocomplete="off"></label>
        <nav id="siNavigation" class="si-navigation" aria-label="48 layar Inventory & Sales"></nav>
        <div class="si-sidebar-foot"><i class="fas fa-shield-alt"></i><span>RBAC aktif<br><small>Akses mengikuti grup pengguna</small></span></div>
    </aside>

    <section class="si-main">
        <header class="si-topbar">
            <button class="si-icon-button" id="siOpenNav" aria-label="Buka menu"><i class="fas fa-bars"></i></button>
            <div class="si-breadcrumb"><span>Inventory &amp; Sales</span><i class="fas fa-chevron-right"></i><strong id="siBreadcrumb">Ringkasan</strong></div>
            <div class="si-top-actions">
                <span class="si-online" id="siOnline"><i class="fas fa-circle"></i> Online</span>
                <button class="si-user"><span><%=pengguna.getUserId()%></span><i class="fas fa-user-circle"></i></button>
                <a class="si-icon-button" href="<%=ctx%>/main" title="Kembali ke AIS"><i class="fas fa-sign-out-alt"></i></a>
            </div>
        </header>

        <main class="si-content">
            <section class="si-hero">
                <div><span class="si-eyebrow" id="siNumber">LAYAR 01</span><h1 id="siTitle">Data Supplier</h1><p id="siDescription">Master, transaksi, stok, hutang, piutang, nota sales, kas dan laporan dalam satu workspace.</p></div>
                <div class="si-context">
                    <label>Periode<input type="date" id="siDateFrom"></label>
                    <label>s.d.<input type="date" id="siDateTo"></label>
                    <button class="si-button si-primary" id="siRefresh"><i class="fas fa-sync-alt"></i> Muat ulang</button>
                </div>
            </section>

            <section class="si-kpis" id="siKpis">
                <article><span>Total data</span><strong id="siTotal">—</strong><i class="fas fa-database"></i></article>
                <article><span>Status layar</span><strong id="siStatus">Siap</strong><i class="fas fa-check-circle"></i></article>
                <article><span>Nominal</span><strong id="siNominal">Rp 0</strong><i class="fas fa-wallet"></i></article>
                <article><span>Sinkronisasi</span><strong>Server AIS</strong><i class="fas fa-cloud-upload-alt"></i></article>
            </section>

            <section class="si-panel">
                <div class="si-toolbar">
                    <label class="si-search"><i class="fas fa-search"></i><input id="siKeyword" placeholder="Cari kode, nama, nomor dokumen..."></label>
                    <select id="siFilter"><option value="">Semua status</option><option value="aktif">Aktif</option><option value="nonaktif">Nonaktif</option><option value="open">Belum lunas</option><option value="lunas">Lunas</option></select>
                    <div class="si-toolbar-actions">
                        <button class="si-button" id="siDetail"><i class="fas fa-eye"></i> Detail</button>
                        <button class="si-button" id="siWorkflow"><i class="fas fa-bolt"></i> Aksi</button>
                        <button class="si-button" id="siPrintHistory"><i class="fas fa-history"></i> Riwayat Cetak</button>
                        <button class="si-button" id="siExport"><i class="fas fa-file-excel"></i> Ekspor</button>
                        <button class="si-button" id="siPrint"><i class="fas fa-print"></i> Cetak</button>
                        <button class="si-button si-primary" id="siCreate"><i class="fas fa-plus"></i> Tambah</button>
                    </div>
                </div>
                <div class="si-notice" id="siNotice" hidden></div>
                <div class="si-loading" id="siLoading" hidden><span></span><p>Mengambil data dari AIS…</p></div>
                <div class="si-table-wrap"><table class="si-table"><thead id="siHead"></thead><tbody id="siRows"></tbody></table></div>
                <div class="si-empty" id="siEmpty" hidden><i class="fas fa-inbox"></i><h3>Belum ada data</h3><p>Ubah filter atau tambahkan transaksi sesuai hak akses Anda.</p></div>
                <footer class="si-pagination"><span id="siCount">0 data</span><div><button id="siPrev"><i class="fas fa-chevron-left"></i></button><span id="siPage">1</span><button id="siNext"><i class="fas fa-chevron-right"></i></button></div></footer>
            </section>
        </main>
    </section>

    <div class="si-modal" id="siModal" hidden><div class="si-modal-card" role="dialog" aria-modal="true"><header><div><span class="si-eyebrow">INPUT DATA</span><h2 id="siModalTitle">Tambah data</h2></div><button class="si-icon-button" id="siModalClose"><i class="fas fa-times"></i></button></header><form id="siForm"><div class="si-form-grid" id="siFormFields"></div><footer><button type="button" class="si-button" id="siCancel">Batal</button><button type="submit" class="si-button si-primary"><i class="fas fa-save"></i> Simpan</button></footer></form></div></div>
    <div class="si-toast" id="siToast" hidden></div>
</div>
<script><%@ include file="inventory.js" %></script>
</body>
</html>
