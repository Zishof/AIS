<%--
  Cetak Price Tag / POP otomatis (A2 / A4 / A5) — versi JSP.
  Pilih produk + ukuran + tata letak, lalu cetak. Cetak memakai @page size + window.print()
  (HTML/CSS murni, tanpa library PDF). Barcode via JsBarcode (CDN). Toko terkunci utk pedagang.
--%>
<%@page import="java.util.*"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
String rndPt = Common.getGeneratedBarCode(7);
String svcPt = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fbarang&s=pricetag_service";
Tbmuser tbmuserPt = Common.getCurrentUser(request);
Toko scopeTokoPt = (tbmuserPt != null && tbmuserPt.getPedagang() != null) ? tbmuserPt.getPedagang().getToko() : null;
boolean lockTokoPt = (scopeTokoPt != null);
String namaTokoPt = lockTokoPt ? (scopeTokoPt.getNama()==null?("Toko #"+scopeTokoPt.getId()):scopeTokoPt.getNama()) : "";
List tokosPt = new ArrayList();
if (!lockTokoPt) {
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    try { tokosPt = HibernateUtil.currentNativeSession().createCriteria(Toko.class).addOrder(Order.asc("nama")).list(); }
    catch (Exception e) { try { tokosPt = HibernateUtil.currentNativeSession().createCriteria(Toko.class).list(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/barang/pricetag.jsp:24");} }
}
%>
<style>
.pt-wrap{--pt-mut:#64748b;}
.pt-prod{max-height:340px;overflow:auto;border:1px solid #eef2f7;border-radius:12px;}
.pt-prod .row-item{display:flex;align-items:center;gap:10px;padding:7px 12px;border-bottom:1px solid #f3f6fa;}
.pt-prod .row-item:hover{background:#f8fafc;}
.pt-opt-card{border:1px solid #eef2f7;border-radius:14px;background:#fff;}
.pt-size{display:flex;gap:8px;flex-wrap:wrap;}
.pt-size label{flex:1 1 90px;border:1px solid #e5e9f0;border-radius:10px;padding:8px 10px;text-align:center;cursor:pointer;font-weight:700;}
.pt-size input{display:none;}
.pt-size input:checked+span{color:#1e40af;}
.pt-size label:has(input:checked){border-color:#1e40af;background:#eff6ff;}
</style>

<div class="container-fluid px-0 pt-wrap" id="pt<%=rndPt%>">
  <div class="mb-2">
    <h6 class="fw-bold mb-0"><i class="fas fa-tags me-2 text-primary"></i><%=Common.getBahasaConfig("Cetak Price Tag / POP")%></h6>
    <div class="text-muted small"><%=Common.getBahasaConfig("Pilih produk dan ukuran kertas (A2/A4/A5), lalu cetak label harga untuk standing POP, shelving, atau gondola.")%></div>
  </div>

  <div class="row g-3">
    <!-- Pilih produk -->
    <div class="col-12 col-lg-7">
      <div class="card border-0 shadow-sm rounded-4 h-100"><div class="card-body">
        <div class="row g-2 align-items-end mb-2">
          <div class="col-12 col-md-5">
            <label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Toko / Kios")%></label>
            <% if (lockTokoPt) { %>
              <input type="hidden" id="ptToko<%=rndPt%>" value="<%=scopeTokoPt.getId()%>">
              <input type="text" class="form-control form-control-sm" value="<%=namaTokoPt%>" disabled>
            <% } else { %>
              <select id="ptToko<%=rndPt%>" class="form-select form-select-sm" onchange="ptLoad<%=rndPt%>()">
                <option value="">-- <%=Common.getBahasaConfig("pilih toko")%> --</option>
                <% for (int i=0;i<tokosPt.size();i++){ Toko t=(Toko)tokosPt.get(i); %>
                <option value="<%=t.getId()%>"><%=t.getNama()==null?("Toko #"+t.getId()):t.getNama()%></option>
                <% } %>
              </select>
            <% } %>
          </div>
          <div class="col-12 col-md-7">
            <label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Cari produk")%></label>
            <div class="input-group input-group-sm"><span class="input-group-text"><i class="fas fa-search"></i></span>
              <input type="text" id="ptCari<%=rndPt%>" class="form-control" placeholder="<%=Common.getBahasaConfig("nama / kode produk...")%>" oninput="ptFilter<%=rndPt%>()"></div>
          </div>
        </div>
        <div class="d-flex justify-content-between align-items-center mb-1">
          <div class="form-check"><input class="form-check-input" type="checkbox" id="ptAll<%=rndPt%>" onchange="ptToggleAll<%=rndPt%>(this.checked)"><label class="form-check-label small" for="ptAll<%=rndPt%>"><%=Common.getBahasaConfig("Pilih semua (tampil)")%></label></div>
          <span class="badge bg-primary" id="ptCount<%=rndPt%>">0 dipilih</span>
        </div>
        <div class="pt-prod" id="ptProdBody<%=rndPt%>"><div class="text-center text-muted py-4"><%=Common.getBahasaConfig("Pilih toko untuk memuat produk.")%></div></div>
      </div></div>
    </div>

    <!-- Pengaturan cetak -->
    <div class="col-12 col-lg-5">
      <div class="pt-opt-card p-3 h-100">
        <div class="fw-bold mb-2"><%=Common.getBahasaConfig("Pengaturan Cetak")%></div>
        <label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Jenis cetak")%></label>
        <select id="ptTpl<%=rndPt%>" class="form-select form-select-sm mb-3" onchange="ptTplChange<%=rndPt%>()">
          <option value="pop"><%=Common.getBahasaConfig("POP Besar (A2/A4/A5)")%></option>
          <option value="sticker"><%=Common.getBahasaConfig("Stiker Label Warna A4 (40/lembar, 5x8)")%></option>
          <option value="textlabel"><%=Common.getBahasaConfig("Label Teks Sederhana (2 kolom)")%></option>
        </select>
        <div id="ptPopOpt<%=rndPt%>">
        <label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Ukuran kertas")%></label>
        <div class="pt-size mb-3">
          <label><input type="radio" name="ptSize<%=rndPt%>" value="A2"><span>A2<br><small class="text-muted">Banner besar</small></span></label>
          <label><input type="radio" name="ptSize<%=rndPt%>" value="A4" checked><span>A4<br><small class="text-muted">Standar POP</small></span></label>
          <label><input type="radio" name="ptSize<%=rndPt%>" value="A5"><span>A5<br><small class="text-muted">Shelving</small></span></label>
        </div>
        <label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Label per halaman")%></label>
        <select id="ptPer<%=rndPt%>" class="form-select form-select-sm mb-3">
          <option value="1">1 (penuh)</option><option value="2">2</option><option value="4">4</option>
        </select>
        </div><!-- /ptPopOpt: ukuran & per-halaman hanya utk POP Besar -->
        <div class="row g-2 mb-2">
          <div class="col-6"><label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Salinan / produk")%></label>
            <input type="number" id="ptCopies<%=rndPt%>" class="form-control form-control-sm" value="1" min="1" max="50"></div>
          <div class="col-6"><label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Label promo (opsional)")%></label>
            <input type="text" id="ptPromo<%=rndPt%>" class="form-control form-control-sm" placeholder="mis. PROMO"></div>
        </div>
        <div class="form-check"><input class="form-check-input" type="checkbox" id="ptBc<%=rndPt%>" checked><label class="form-check-label small" for="ptBc<%=rndPt%>"><%=Common.getBahasaConfig("Tampilkan barcode")%></label></div>
        <div class="form-check"><input class="form-check-input" type="checkbox" id="ptKode<%=rndPt%>" checked><label class="form-check-label small" for="ptKode<%=rndPt%>"><%=Common.getBahasaConfig("Tampilkan kode produk")%></label></div>
        <div class="form-check mb-3"><input class="form-check-input" type="checkbox" id="ptNamaToko<%=rndPt%>" checked><label class="form-check-label small" for="ptNamaToko<%=rndPt%>"><%=Common.getBahasaConfig("Tampilkan nama toko")%></label></div>
        <div class="d-grid">
          <button class="btn btn-primary" type="button" onclick="ptCetak<%=rndPt%>()"><i class="fas fa-print me-1"></i><%=Common.getBahasaConfig("Pratinjau & Cetak")%></button>
        </div>
        <div class="text-muted small mt-2"><%=Common.getBahasaConfig("Akan terbuka jendela cetak. Pada dialog printer, pastikan ukuran kertas sama dan skala 100%.")%></div>
      </div>
    </div>
  </div>
</div>

<script src="<%=Common.ROOT%>/js/ais_pricetag_print.js"></script>
<script>
(function(){
  var SVC='<%=svcPt%>', rnd='<%=rndPt%>';
  var NAMA_TOKO='<%=namaTokoPt.replace("\\","\\\\").replace("'","\\'")%>';
  var produk=[], dipilih={};
  function $(id){ return document.getElementById(id+rnd); }
  function esc(s){ return (s==null?'':String(s)).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];}); }
  function rupiah(n){ n=Number(n)||0; try{ return 'Rp '+n.toLocaleString('id-ID'); }catch(e){ return 'Rp '+n; } }
  function toko(){ var el=$('ptToko'); return el?el.value:''; }
  async function post(p){ var r=await fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},body:new URLSearchParams(p)}); return JSON.parse((await r.text()).trim()); }

  function render(list){
    var b=$('ptProdBody');
    if(!list.length){ b.innerHTML='<div class="text-center text-muted py-4">Tidak ada produk.</div>'; return; }
    b.innerHTML=list.map(function(p){
      var ck=dipilih[p.id]?'checked':'';
      return '<div class="row-item"><input class="form-check-input mt-0" type="checkbox" '+ck+' onchange="ptPick'+rnd+'('+p.id+',this.checked)">'
        +'<div class="flex-grow-1"><div class="fw-semibold">'+esc(p.nama)+'</div><div class="small text-muted">'+esc(p.kode)+'</div></div>'
        +'<div class="fw-bold text-primary">'+rupiah(p.hargaJual)+'</div></div>';
    }).join('');
  }
  window['ptPick'+rnd]=function(id,on){ if(on)dipilih[id]=true; else delete dipilih[id]; updCount(); };
  function updCount(){ var n=Object.keys(dipilih).length; $('ptCount').textContent=n+' dipilih'; }
  window['ptFilter'+rnd]=function(){ var q=$('ptCari').value.toLowerCase(); render(produk.filter(function(p){return (p.nama+' '+p.kode).toLowerCase().indexOf(q)>=0;})); };
  window['ptToggleAll'+rnd]=function(on){ var q=$('ptCari').value.toLowerCase(); produk.filter(function(p){return (p.nama+' '+p.kode).toLowerCase().indexOf(q)>=0;}).forEach(function(p){ if(on)dipilih[p.id]=true; else delete dipilih[p.id]; }); updCount(); window['ptFilter'+rnd](); };

  window['ptLoad'+rnd]=async function(){
    if(!toko()){ $('ptProdBody').innerHTML='<div class="text-center text-muted py-4">Pilih toko dulu.</div>'; return; }
    $('ptProdBody').innerHTML='<div class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</div>';
    try{ var r=await post({aksi:'listProduk', toko:toko()});
      if(r.status!=='00'){ $('ptProdBody').innerHTML='<div class="text-danger small py-3">'+esc(r.message||'Gagal')+'</div>'; return; }
      produk=r.data||[]; dipilih={}; updCount(); render(produk);
    }catch(e){ $('ptProdBody').innerHTML='<div class="text-danger small py-3">Error: '+esc(e.message)+'</div>'; }
  };

  function sizeVal(){ var r=document.querySelector('input[name=ptSize'+rnd+']:checked'); return r?r.value:'A4'; }
  // Sembunyikan opsi Ukuran & Per-halaman bila bukan "POP Besar".
  window['ptTplChange'+rnd]=function(){ var t=$('ptTpl').value; var w=$('ptPopOpt'); if(w) w.style.display=(t==='pop')?'':'none'; };

  window['ptCetak'+rnd]=function(){
    var sel=produk.filter(function(p){return dipilih[p.id];});
    if(!sel.length){ alert('<%=Common.getBahasaConfigJS("Silakan pilih minimal satu produk terlebih dahulu.")%>'); return; }
    // Logika cetak dipusatkan di ais_pricetag_print.js (dipakai bersama JSP & ZK).
    var tags=sel.map(function(p){ return {nama:p.nama, harga:p.hargaJual, kode:(p.kode||(''+p.id))}; });
    aisCetakPriceTag({
      tags:tags, template:$('ptTpl').value, copies:$('ptCopies').value, size:sizeVal(), per:$('ptPer').value,
      showBc:$('ptBc').checked, showKode:$('ptKode').checked, showToko:$('ptNamaToko').checked,
      namaToko:NAMA_TOKO, promo:$('ptPromo').value
    });
  };

  // auto-load bila toko terkunci (pedagang)
  if(toko()){ window['ptLoad'+rnd](); }
})();
</script>
