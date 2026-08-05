<%@page import="ais.common.Common"%>
<%
String rndK = Common.getGeneratedBarCode(6);
%>

<div class="container-fluid px-0">
    <h5 class="fw-bold mb-3"><i class="fas fa-truck-loading me-2 text-primary"></i><%=Common.getBahasaConfig("Kulakan & Pengadaan")%></h5>

    <ul class="nav nav-pills bg-white p-2 rounded-4 shadow-sm border mb-3 flex-wrap gap-1" id="kulakanTabs<%=rndK%>" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link active rounded-pill fw-bold" id="tab-harga<%=rndK%>" data-bs-toggle="pill" data-bs-target="#pane-harga<%=rndK%>" type="button" role="tab">
                <i class="fas fa-tags me-2"></i><%=Common.getBahasaConfig("Harga Beli")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link rounded-pill fw-bold" id="tab-pr<%=rndK%>" data-bs-toggle="pill" data-bs-target="#pane-pr<%=rndK%>" type="button" role="tab">
                <i class="fas fa-file-signature me-2"></i><%=Common.getBahasaConfig("PR - Permintaan")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link rounded-pill fw-bold" id="tab-po<%=rndK%>" data-bs-toggle="pill" data-bs-target="#pane-po<%=rndK%>" type="button" role="tab">
                <i class="fas fa-file-invoice me-2"></i><%=Common.getBahasaConfig("PO - Pemesanan")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link rounded-pill fw-bold" id="tab-bast<%=rndK%>" data-bs-toggle="pill" data-bs-target="#pane-bast<%=rndK%>" type="button" role="tab">
                <i class="fas fa-dolly me-2"></i><%=Common.getBahasaConfig("BAST - Penerimaan")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link rounded-pill fw-bold" id="tab-draftinv<%=rndK%>" data-bs-toggle="pill" data-bs-target="#pane-draftinv<%=rndK%>" type="button" role="tab">
                <i class="fas fa-clipboard-check me-2"></i><%=Common.getBahasaConfig("Draft Inventaris")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link rounded-pill fw-bold" id="tab-tagihan<%=rndK%>" data-bs-toggle="pill" data-bs-target="#pane-tagihan<%=rndK%>" type="button" role="tab">
                <i class="fas fa-file-invoice-dollar me-2"></i><%=Common.getBahasaConfig("Terima Tagihan")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link rounded-pill fw-bold" id="tab-pks<%=rndK%>" data-bs-toggle="pill" data-bs-target="#pane-pks<%=rndK%>" type="button" role="tab">
                <i class="fas fa-handshake me-2"></i><%=Common.getBahasaConfig("PKS - Kerjasama")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link rounded-pill fw-bold" id="tab-retur<%=rndK%>" data-bs-toggle="pill" data-bs-target="#pane-retur<%=rndK%>" type="button" role="tab">
                <i class="fas fa-rotate-left me-2"></i><%=Common.getBahasaConfig("Retur Barang")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link rounded-pill fw-bold" id="tab-pakai<%=rndK%>" data-bs-toggle="pill" data-bs-target="#pane-pakai<%=rndK%>" type="button" role="tab">
                <i class="fas fa-blender me-2"></i><%=Common.getBahasaConfig("Pemakaian Bahan Baku")%>
            </button>
        </li>
    </ul>

    <div class="tab-content" id="kulakanTabsContent<%=rndK%>">
        <div class="tab-pane fade show active" id="pane-harga<%=rndK%>" role="tabpanel" aria-labelledby="tab-harga<%=rndK%>">
            <jsp:include page="/WEB-INF/baru/modul/kantin/pengadaan/index.jsp" />
        </div>
        <div class="tab-pane fade" id="pane-pr<%=rndK%>" role="tabpanel" aria-labelledby="tab-pr<%=rndK%>">
            <jsp:include page="/WEB-INF/baru/modul/kantin/kulakan/permintaan_asset.jsp" />
        </div>
        <div class="tab-pane fade" id="pane-po<%=rndK%>" role="tabpanel" aria-labelledby="tab-po<%=rndK%>">
            <jsp:include page="/WEB-INF/baru/modul/kantin/kulakan/pemesanan_asset.jsp" />
        </div>
        <div class="tab-pane fade" id="pane-bast<%=rndK%>" role="tabpanel" aria-labelledby="tab-bast<%=rndK%>">
            <jsp:include page="/WEB-INF/baru/modul/kantin/kulakan/penerimaan_asset.jsp" />
        </div>
        <div class="tab-pane fade" id="pane-draftinv<%=rndK%>" role="tabpanel" aria-labelledby="tab-draftinv<%=rndK%>">
            <jsp:include page="/WEB-INF/baru/modul/kantin/kulakan/draft_inventaris.jsp" />
        </div>
        <div class="tab-pane fade" id="pane-tagihan<%=rndK%>" role="tabpanel" aria-labelledby="tab-tagihan<%=rndK%>">
            <jsp:include page="/WEB-INF/baru/modul/kantin/kulakan/saldo_awal_asset.jsp" />
        </div>
        <div class="tab-pane fade" id="pane-pks<%=rndK%>" role="tabpanel" aria-labelledby="tab-pks<%=rndK%>">
            <jsp:include page="/WEB-INF/baru/modul/kantin/kulakan/kerjasama_asset.jsp" />
        </div>
        <div class="tab-pane fade" id="pane-retur<%=rndK%>" role="tabpanel" aria-labelledby="tab-retur<%=rndK%>">
            <jsp:include page="/WEB-INF/baru/modul/kantin/kulakan/retur_barang.jsp" />
        </div>
        <div class="tab-pane fade" id="pane-pakai<%=rndK%>" role="tabpanel" aria-labelledby="tab-pakai<%=rndK%>">
            <jsp:include page="/WEB-INF/baru/modul/kantin/kulakan/pemakaian_bahan_baku.jsp" />
        </div>
    </div>
</div>

<style>
    .kulakan-frame-wrap-<%=rndK%> { position: relative; min-height: 78vh; }
    .kulakan-frame-<%=rndK%> { width: 100%; height: 78vh; border: 0; border-radius: 12px; background: #fff; box-shadow: 0 .25rem .75rem rgba(0,0,0,.06); }
    .kulakan-loading-<%=rndK%> { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; }
</style>

<script>
    (function(){
        // Muat halaman ZK (PR/PO/BAST/Terima Tagihan) secara lazy saat tab pertama kali dibuka.
        document.querySelectorAll('#kulakanTabs<%=rndK%> button[data-zul]').forEach(function(btn){
            btn.addEventListener('shown.bs.tab', function(){
                var pane = document.querySelector(btn.getAttribute('data-bs-target'));
                if(!pane){ return; }
                var frame = pane.querySelector('iframe');
                if(frame && frame.getAttribute('data-loaded') === '0'){
                    frame.setAttribute('data-loaded','1');
                    var loadingEl = pane.querySelector('.kulakan-loading-<%=rndK%>');
                    frame.addEventListener('load', function(){ if(loadingEl){ loadingEl.style.display = 'none'; } });
                    frame.src = btn.getAttribute('data-zul');
                }
            });
        });
    })();
</script>
