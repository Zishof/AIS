<%--
  Fragmen BERSAMA transaksi stok untuk tab Kulakan: Pemakaian Bahan Baku & Retur Barang.
  Halaman pemanggil men-set request attribute "jenisTrx" = "pemakaian" | "retur" lalu include
  fragmen ini. Backend: transaksi_stok_service.jsp. Toko dikunci utk pedagang.
--%>
<%@page import="java.util.*"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
String jenisTrx = (String) request.getAttribute("jenisTrx");
if (jenisTrx == null) jenisTrx = "pemakaian";
boolean isRetur = "retur".equals(jenisTrx);
String rndT = Common.getGeneratedBarCode(7);
String svcT = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fkulakan&s=transaksi_stok_service";
Tbmuser uT = Common.getCurrentUser(request);
Toko scopeT = (uT != null && uT.getPedagang() != null) ? uT.getPedagang().getToko() : null;
boolean lockT = (scopeT != null);
List tokosT = new ArrayList();
if (!lockT) {
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    try { tokosT = HibernateUtil.currentNativeSession().createCriteria(Toko.class).addOrder(Order.asc("nama")).list(); }
    catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/_transaksi_stok.jsp:26");}
}
String judulT = isRetur ? "Retur Barang" : "Pemakaian Bahan Baku";
String descT = isRetur
    ? "Catat barang yang diretur (rusak/kadaluarsa/dikembalikan). Tersimpan untuk laporan Retur Barang."
    : "Catat pemakaian bahan baku (konsumsi stok untuk produksi). Stok produk otomatis berkurang.";
%>
<div id="tx<%=rndT%>">
  <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-2">
    <div>
      <h6 class="fw-bold mb-0"><i class="fas <%=isRetur?"fa-rotate-left":"fa-blender"%> me-2 text-primary"></i><%=Common.getBahasaConfig(judulT)%></h6>
      <div class="text-muted small"><%=Common.getBahasaConfig(descT)%></div>
    </div>
  </div>

  <div class="row g-3">
    <div class="col-lg-5">
      <div class="card border-0 shadow-sm rounded-4">
        <div class="card-body">
          <div class="fw-bold mb-2"><i class="fas fa-plus-circle text-success me-1"></i><%=Common.getBahasaConfig("Catat "+judulT)%></div>
          <% if (lockT) { %>
            <input type="hidden" id="tToko<%=rndT%>" value="<%=scopeT.getId()%>">
            <div class="mb-2"><label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Toko")%></label>
              <input type="text" class="form-control form-control-sm" value="<%=scopeT.getNama()==null?"":scopeT.getNama()%>" disabled></div>
          <% } else { %>
            <div class="mb-2"><label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Toko")%></label>
              <select id="tToko<%=rndT%>" class="form-select form-select-sm" onchange="txInit<%=rndT%>()">
                <option value="">-- <%=Common.getBahasaConfig("pilih toko")%> --</option>
                <% for (int i=0;i<tokosT.size();i++){ Toko t=(Toko)tokosT.get(i); %>
                <option value="<%=t.getId()%>"><%=t.getNama()==null?("Toko #"+t.getId()):t.getNama()%></option>
                <% } %>
              </select></div>
          <% } %>
          <div class="mb-2"><label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Produk")%></label>
            <select id="tProduk<%=rndT%>" class="form-select form-select-sm" onchange="txOnProduk<%=rndT%>()"><option value="">-- <%=Common.getBahasaConfig("memuat...")%> --</option></select>
            <div class="form-text small" id="tStok<%=rndT%>"></div>
          </div>
          <div class="row g-2">
            <div class="<%=isRetur?"col-6":"col-12"%>"><label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Jumlah (Qty)")%></label>
              <input type="number" step="any" min="0" id="tQty<%=rndT%>" class="form-control form-control-sm" value="1"></div>
            <% if (isRetur) { %>
            <div class="col-6"><label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Harga Satuan")%></label>
              <input type="number" step="any" min="0" id="tHarga<%=rndT%>" class="form-control form-control-sm" value="0"></div>
            <% } %>
          </div>
          <div class="mb-2 mt-2"><label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Tanggal")%></label>
            <input type="date" id="tTgl<%=rndT%>" class="form-control form-control-sm"></div>
          <div class="mb-2"><label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Keterangan")%></label>
            <input type="text" id="tKet<%=rndT%>" class="form-control form-control-sm" placeholder="<%=Common.getBahasaConfig("alasan / catatan")%>"></div>
          <button class="btn btn-success w-100 fw-bold rounded-pill" onclick="txSimpan<%=rndT%>()"><i class="fas fa-save me-1"></i><%=Common.getBahasaConfig("Simpan Transaksi")%></button>
          <div class="small mt-2" id="tMsg<%=rndT%>"></div>
        </div>
      </div>
    </div>
    <div class="col-lg-7">
      <div class="card border-0 shadow-sm rounded-4 h-100">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-center mb-2">
            <div class="fw-bold"><i class="fas fa-clock-rotate-left text-secondary me-1"></i><%=Common.getBahasaConfig("Riwayat Terakhir")%></div>
            <button class="btn btn-sm btn-outline-secondary rounded-pill" onclick="txList<%=rndT%>()"><i class="fas fa-sync-alt"></i></button>
          </div>
          <div class="table-responsive" style="max-height:60vh;overflow:auto;">
            <table class="table table-sm table-hover align-middle">
              <thead class="table-light"><tr>
                <th><%=Common.getBahasaConfig("Waktu")%></th><th><%=Common.getBahasaConfig("Produk")%></th>
                <th class="text-end"><%=Common.getBahasaConfig("Qty")%></th>
                <% if (isRetur) { %><th class="text-end"><%=Common.getBahasaConfig("Harga")%></th><th class="text-end"><%=Common.getBahasaConfig("Total")%></th><% } %>
                <th><%=Common.getBahasaConfig("Keterangan")%></th><th></th>
              </tr></thead>
              <tbody id="tBody<%=rndT%>"><tr><td colspan="<%=isRetur?7:5%>" class="text-center text-muted py-3">-</td></tr></tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

<script>
(function(){
  var RND="<%=rndT%>", SVC="<%=svcT%>", JENIS="<%=jenisTrx%>", RETUR=<%=isRetur%>, LOCK=<%=lockT%>;
  function el(id){ return document.getElementById(id+RND); }
  function esc(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
  function fmt(v){ try{ return new Intl.NumberFormat('id-ID',{maximumFractionDigits:2}).format(v||0);}catch(e){return v;} }
  function tokoId(){ var t=el("tToko"); return t? (t.value||"") : ""; }
  function tnggl(){ var d=new Date(); var m=(''+(d.getMonth()+1)).padStart(2,'0'); var dd=(''+d.getDate()).padStart(2,'0'); return d.getFullYear()+'-'+m+'-'+dd; }

  window["txOnProduk"+RND]=function(){
    var s=el("tProduk"), o=s.options[s.selectedIndex];
    if(o && o.getAttribute('data-stok')!=null){ el("tStok").textContent='Stok saat ini: '+fmt(parseFloat(o.getAttribute('data-stok'))); }
    else el("tStok").textContent='';
    if(RETUR && o && o.getAttribute('data-hb')!=null){ var h=el("tHarga"); if(h && (!h.value||h.value==='0')) h.value=o.getAttribute('data-hb'); }
  };

  window["txProduk"+RND]=function(){
    var s=el("tProduk"); s.innerHTML='<option value="">-- memuat... --</option>';
    var p=new URLSearchParams(); p.append("aksi","produk"); p.append("jenis",JENIS); p.append("tokoId",tokoId());
    fetch(SVC,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:p.toString()})
      .then(function(r){return r.json();}).then(function(d){
        if(d.status!=="00"){ s.innerHTML='<option value="">'+esc(d.message||"gagal")+'</option>'; return; }
        var h='<option value="">-- pilih produk --</option>';
        (d.data||[]).forEach(function(x){ h+='<option value="'+x.id+'" data-stok="'+x.stok+'" data-hb="'+x.hargabeli+'">'+esc((x.kode?x.kode+' - ':'')+x.nama)+'</option>'; });
        s.innerHTML=h;
      }).catch(function(){ s.innerHTML='<option value="">koneksi gagal</option>'; });
  };

  window["txList"+RND]=function(){
    var b=el("tBody"); var cols=RETUR?7:5;
    var p=new URLSearchParams(); p.append("aksi","list"); p.append("jenis",JENIS); p.append("tokoId",tokoId());
    fetch(SVC,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:p.toString()})
      .then(function(r){return r.json();}).then(function(d){
        if(d.status!=="00"||!(d.data||[]).length){ b.innerHTML='<tr><td colspan="'+cols+'" class="text-center text-muted py-3">Belum ada data.</td></tr>'; return; }
        b.innerHTML=d.data.map(function(x){
          var s='<tr><td class="small">'+esc(x.waktu)+'</td><td>'+esc((x.kode?x.kode+' - ':'')+x.nama)+'</td><td class="text-end">'+fmt(x.qty)+'</td>';
          if(RETUR){ s+='<td class="text-end">'+fmt(x.harga)+'</td><td class="text-end">'+fmt(x.total)+'</td>'; }
          s+='<td class="small">'+esc(x.keterangan||'')+'</td>'
            +'<td class="text-end"><button class="btn btn-sm btn-outline-danger" onclick="txHapus'+RND+'('+x.id+')"><i class="fas fa-trash"></i></button></td></tr>';
          return s;
        }).join('');
      }).catch(function(){ b.innerHTML='<tr><td colspan="'+cols+'" class="text-center text-danger py-3">Koneksi gagal.</td></tr>'; });
  };

  window["txSimpan"+RND]=function(){
    var msg=el("tMsg"); msg.innerHTML='';
    if(!LOCK && !tokoId()){ msg.innerHTML='<span class="text-danger">Pilih toko dahulu.</span>'; return; }
    if(!el("tProduk").value){ msg.innerHTML='<span class="text-danger">Pilih produk.</span>'; return; }
    var qty=parseFloat(el("tQty").value); if(!(qty>0)){ msg.innerHTML='<span class="text-danger">Qty harus > 0.</span>'; return; }
    var p=new URLSearchParams(); p.append("aksi","simpan"); p.append("jenis",JENIS); p.append("tokoId",tokoId());
    p.append("produkId",el("tProduk").value); p.append("qty",el("tQty").value);
    p.append("tanggal",el("tTgl").value||""); p.append("keterangan",el("tKet").value||"");
    if(RETUR) p.append("harga", el("tHarga")?el("tHarga").value:"0");
    fetch(SVC,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:p.toString()})
      .then(function(r){return r.json();}).then(function(d){
        if(d.status==="00"){ msg.innerHTML='<span class="text-success">Tersimpan.</span>'; el("tQty").value='1'; el("tKet").value=''; window["txList"+RND](); window["txProduk"+RND](); }
        else msg.innerHTML='<span class="text-danger">'+esc(d.message||"gagal")+'</span>';
      }).catch(function(){ msg.innerHTML='<span class="text-danger">Koneksi gagal.</span>'; });
  };

  window["txHapus"+RND]=function(id){
    if(!confirm("Hapus data ini?")) return;
    var p=new URLSearchParams(); p.append("aksi","hapus"); p.append("jenis",JENIS); p.append("tokoId",tokoId()); p.append("id",id);
    fetch(SVC,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:p.toString()})
      .then(function(r){return r.json();}).then(function(){ window["txList"+RND](); window["txProduk"+RND](); }).catch(function(){});
  };

  window["txInit"+RND]=function(){ window["txProduk"+RND](); window["txList"+RND](); };

  el("tTgl").value=tnggl();
  txInit<%=rndT%>();
})();
</script>
