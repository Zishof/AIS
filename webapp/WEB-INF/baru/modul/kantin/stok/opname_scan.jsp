<%--
  Stok Opname By Scan HP / PDT — versi JSP (tampilan).
  Memanggil endpoint opname_scan_service.jsp (aksi cariBarcode/simpan/ringkasan) yang memakai
  backend bersama StokOpnameScanUtil. UI responsif (HP & desktop), ringkasan visual HTML/CSS.
--%>
<%@page import="java.util.*"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
String rndOs = Common.getGeneratedBarCode(7);
String svcOs = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fstok&s=opname_scan_service";
Tbmuser tbmuser = Common.getCurrentUser(request);
Toko scopeToko = (tbmuser != null && tbmuser.getPedagang() != null) ? tbmuser.getPedagang().getToko() : null;
boolean lockToko = (scopeToko != null);
List tokos = new ArrayList();
if (!lockToko) {
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    try { tokos = HibernateUtil.currentNativeSession().createCriteria(Toko.class).addOrder(Order.asc("nama")).list(); }
    catch (Exception e) { try { tokos = HibernateUtil.currentNativeSession().createCriteria(Toko.class).list(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/stok/opname_scan.jsp:23");} }
}
%>
<style>
.so-scan{--c-lebih:#16a34a;--c-kurang:#dc2626;--c-net:#64748b;}
.so-scan .so-head h6{margin:0;font-weight:800;}
.so-scan .so-hint{font-size:.82rem;color:var(--c-net);}
.so-scan .so-scanbar{position:sticky;top:0;z-index:6;background:rgba(255,255,255,.96);backdrop-filter:blur(4px);
  border:1px solid #e9eef5;border-radius:14px;padding:12px;box-shadow:0 2px 10px rgba(16,24,40,.05);}
.so-scan .so-scan-input{font-size:1.05rem;}
.so-scan .so-stat{flex:1 1 120px;min-width:108px;background:#fff;border:1px solid #e9eef5;border-radius:12px;
  padding:8px 12px;box-shadow:0 1px 2px rgba(16,24,40,.04);}
.so-scan .so-stat .lbl{font-size:.7rem;color:var(--c-net);text-transform:uppercase;letter-spacing:.02em;}
.so-scan .so-stat .val{font-size:1.35rem;font-weight:800;line-height:1.1;}
.so-scan .so-bar{display:flex;height:10px;border-radius:6px;overflow:hidden;background:#eef2f7;}
.so-scan .so-bar>span{display:block;}
.so-scan table td,.so-scan table th{vertical-align:middle;}
.so-scan .so-step{max-width:150px;margin:auto;}
.so-scan .so-step input{text-align:center;}
@media (max-width:640px){
  .so-scan .so-save-wrap{position:sticky;bottom:0;z-index:6;background:rgba(255,255,255,.96);
    padding:8px 0;border-top:1px solid #eef2f7;}
  .so-scan .so-stat .val{font-size:1.15rem;}
}
</style>

<div class="container-fluid px-0 so-scan" id="so<%=rndOs%>">
  <div class="so-head d-flex align-items-center justify-content-between flex-wrap gap-2 mb-2">
    <div>
      <h6><i class="fas fa-barcode me-2 text-primary"></i><%=Common.getBahasaConfig("Stok Opname By Scan HP / PDT")%></h6>
      <div class="so-hint"><%=Common.getBahasaConfig("Hitung ulang stok dengan menembak barcode. Isi jumlah aslinya, lalu Simpan — stok otomatis dibetulkan.")%></div>
    </div>
  </div>

  <!-- Ringkasan hari ini -->
  <div class="so-hint mb-1"><%=Common.getBahasaConfig("Hasil opname hari ini di toko ini.")%></div>
  <div class="d-flex gap-2 flex-wrap mb-3">
    <div class="so-stat"><div class="lbl"><%=Common.getBahasaConfig("Produk diopname")%></div><div class="val" id="osTodProduk<%=rndOs%>">-</div></div>
    <div class="so-stat"><div class="lbl"><%=Common.getBahasaConfig("Total Lebih")%></div><div class="val" style="color:var(--c-lebih)" id="osTodLebih<%=rndOs%>">-</div></div>
    <div class="so-stat"><div class="lbl"><%=Common.getBahasaConfig("Total Kurang")%></div><div class="val" style="color:var(--c-kurang)" id="osTodKurang<%=rndOs%>">-</div></div>
    <div class="so-stat"><div class="lbl"><%=Common.getBahasaConfig("Selisih Bersih")%></div><div class="val" id="osTodNet<%=rndOs%>">-</div></div>
  </div>

  <!-- Kendali scan (menempel saat di-scroll) -->
  <div class="so-scanbar mb-3">
    <div class="row g-2 align-items-end">
      <div class="col-12 col-md-4">
        <label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Toko / Kios")%></label>
        <% if (lockToko) { %>
          <input type="hidden" id="osToko<%=rndOs%>" value="<%=scopeToko.getId()%>">
          <input type="text" class="form-control form-control-sm" value="<%=scopeToko.getNama()==null?"":scopeToko.getNama()%>" disabled>
        <% } else { %>
          <select id="osToko<%=rndOs%>" class="form-select form-select-sm" onchange="osLoadRingkasan<%=rndOs%>()">
            <option value="">-- <%=Common.getBahasaConfig("pilih toko")%> --</option>
            <% for (int i=0;i<tokos.size();i++){ Toko t=(Toko)tokos.get(i); %>
            <option value="<%=t.getId()%>"><%=t.getNama()==null?("Toko #"+t.getId()):t.getNama()%></option>
            <% } %>
          </select>
        <% } %>
      </div>
      <div class="col-12 col-md-5">
        <label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Barcode (scan PDT / ketik manual)")%></label>
        <div class="input-group">
          <span class="input-group-text"><i class="fas fa-keyboard"></i></span>
          <input type="text" id="osScan<%=rndOs%>" class="form-control so-scan-input" autocomplete="off" inputmode="none"
                 placeholder="<%=Common.getBahasaConfig("fokuskan di sini lalu scan / ketik kode + Enter")%>">
          <button class="btn btn-primary" type="button" onclick="osAdd<%=rndOs%>()"><i class="fas fa-plus"></i></button>
        </div>
      </div>
      <div class="col-6 col-md-2 d-grid">
        <button class="btn btn-outline-dark btn-sm" type="button" id="osCamBtn<%=rndOs%>" onclick="osToggleCam<%=rndOs%>()"><i class="fas fa-camera me-1"></i><%=Common.getBahasaConfig("Kamera")%></button>
      </div>
      <div class="col-6 col-md-1 d-grid">
        <button class="btn btn-outline-secondary btn-sm" type="button" onclick="osClear<%=rndOs%>()" title="<%=Common.getBahasaConfig("Bersihkan")%>"><i class="fas fa-eraser"></i></button>
      </div>
    </div>
    <div id="osReaderWrap<%=rndOs%>" style="display:none" class="mt-2">
      <div id="osReader<%=rndOs%>" style="width:100%;max-width:420px;margin:auto;border:1px solid #dee2e6;border-radius:10px;overflow:hidden"></div>
      <div class="text-center so-hint mt-1"><%=Common.getBahasaConfig("Arahkan kamera ke barcode produk.")%></div>
    </div>
  </div>

  <!-- Ringkasan sesi berjalan -->
  <div class="so-hint mb-1"><%=Common.getBahasaConfig("Barang yang sedang Anda hitung sekarang.")%></div>
  <div class="d-flex gap-2 flex-wrap mb-2">
    <div class="so-stat"><div class="lbl"><%=Common.getBahasaConfig("Item discan")%></div><div class="val" id="osSesItem<%=rndOs%>">0</div></div>
    <div class="so-stat"><div class="lbl"><%=Common.getBahasaConfig("Lebih (+)")%></div><div class="val" style="color:var(--c-lebih)" id="osSesLebih<%=rndOs%>">0</div></div>
    <div class="so-stat"><div class="lbl"><%=Common.getBahasaConfig("Kurang (-)")%></div><div class="val" style="color:var(--c-kurang)" id="osSesKurang<%=rndOs%>">0</div></div>
    <div class="so-stat"><div class="lbl"><%=Common.getBahasaConfig("Selisih bersih")%></div><div class="val" id="osSesNet<%=rndOs%>">0</div></div>
  </div>
  <div class="so-bar mb-3"><span id="osBarLebih<%=rndOs%>" style="width:0%;background:var(--c-lebih)"></span><span id="osBarKurang<%=rndOs%>" style="width:0%;background:var(--c-kurang)"></span></div>

  <!-- Tabel hasil -->
  <div class="table-responsive">
    <table class="table table-sm table-hover align-middle">
      <thead class="table-light"><tr>
        <th style="width:34px">#</th><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Produk")%></th>
        <th class="text-end"><%=Common.getBahasaConfig("Stok Sistem")%></th>
        <th class="text-center" style="width:160px"><%=Common.getBahasaConfig("Stok Fisik")%></th>
        <th class="text-end"><%=Common.getBahasaConfig("Selisih")%></th>
        <th style="width:42px"></th>
      </tr></thead>
      <tbody id="osBody<%=rndOs%>"><tr><td colspan="7" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Belum ada item. Mulai scan barcode.")%></td></tr></tbody>
    </table>
  </div>

  <div class="row g-2 align-items-end mt-1 so-save-wrap">
    <div class="col-12 col-md-8">
      <label class="form-label small fw-semibold mb-1"><%=Common.getBahasaConfig("Keterangan / alasan selisih (opsional)")%></label>
      <input type="text" id="osKet<%=rndOs%>" class="form-control form-control-sm" placeholder="<%=Common.getBahasaConfig("mis. barang basi, hilang, rusak")%>">
    </div>
    <div class="col-12 col-md-4 d-grid">
      <button class="btn btn-success" type="button" id="osSave<%=rndOs%>" onclick="osSimpan<%=rndOs%>()" disabled><i class="fas fa-save me-1"></i><%=Common.getBahasaConfig("Simpan Semua")%> (<span id="osCount<%=rndOs%>">0</span>)</button>
    </div>
  </div>
</div>

<script data-cfasync="false" src="https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js"></script>
<script>
(function(){
  var SVC='<%=svcOs%>', rnd='<%=rndOs%>';
  var items=[], cam=null, lastCode='', lastTime=0;
  function $(id){ return document.getElementById(id+rnd); }
  function esc(s){ return (s==null?'':String(s)).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];}); }
  function fmt(n){ n=Number(n)||0; return (n===Math.floor(n))? String(n): String(n); }
  function toko(){ var el=$('osToko'); return el?el.value:''; }
  async function post(p){ var r=await fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},body:new URLSearchParams(p)}); return JSON.parse((await r.text()).trim()); }
  function beep(ok){ try{ var c=new (window.AudioContext||window.webkitAudioContext)(); var o=c.createOscillator(); o.frequency.value=ok?880:240; o.connect(c.destination); o.start(); setTimeout(function(){o.stop();c.close();},110);}catch(e){} }

  function renderSummary(){
    var lebih=0,kurang=0;
    for(var i=0;i<items.length;i++){ var s=items[i].fisik-items[i].sistem; if(s>0)lebih+=s; else if(s<0)kurang+=-s; }
    var net=lebih-kurang, tot=lebih+kurang;
    $('osSesItem').textContent=items.length; $('osSesLebih').textContent=fmt(lebih);
    $('osSesKurang').textContent=fmt(kurang); $('osSesNet').textContent=(net>0?'+':'')+fmt(net);
    $('osSesNet').style.color = net<0?'var(--c-kurang)':(net>0?'var(--c-lebih)':'var(--c-net)');
    var pL=tot<=0?0:Math.round(lebih*100/tot);
    $('osBarLebih').style.width=pL+'%'; $('osBarKurang').style.width=(tot<=0?0:100-pL)+'%';
    $('osCount').textContent=items.length; $('osSave').disabled=items.length===0;
  }

  function render(){
    var tb=$('osBody');
    if(!items.length){ tb.innerHTML='<tr><td colspan="7" class="text-center text-muted py-4">Belum ada item. Mulai scan barcode.</td></tr>'; renderSummary(); return; }
    var h='';
    for(var i=0;i<items.length;i++){ var it=items[i], sel=it.fisik-it.sistem;
      var cls=sel<0?'text-danger':(sel>0?'text-success':'text-muted');
      h+='<tr><td>'+(i+1)+'</td><td class="fw-semibold">'+esc(it.kode)+'</td><td>'+esc(it.nama)+'</td>'
        +'<td class="text-end">'+fmt(it.sistem)+'</td>'
        +'<td><div class="input-group input-group-sm so-step">'
          +'<button class="btn btn-outline-secondary" type="button" onclick="osStep'+rnd+'('+i+',-1)">-</button>'
          +'<input type="number" step="any" class="form-control" value="'+it.fisik+'" onchange="osSetFisik'+rnd+'('+i+',this.value)">'
          +'<button class="btn btn-outline-secondary" type="button" onclick="osStep'+rnd+'('+i+',1)">+</button>'
        +'</div></td>'
        +'<td class="text-end fw-bold '+cls+'">'+(sel>0?'+':'')+fmt(sel)+'</td>'
        +'<td><button class="btn btn-sm btn-outline-danger" type="button" onclick="osDel'+rnd+'('+i+')"><i class="fas fa-times"></i></button></td></tr>';
    }
    tb.innerHTML=h; renderSummary();
  }
  window['osStep'+rnd]=function(i,d){ items[i].fisik=Math.max(0,(parseFloat(items[i].fisik)||0)+d); render(); };
  window['osSetFisik'+rnd]=function(i,v){ items[i].fisik=Math.max(0,parseFloat(v)||0); render(); };
  window['osDel'+rnd]=function(i){ items.splice(i,1); render(); };
  window['osClear'+rnd]=function(){ items=[]; render(); $('osScan').focus(); };

  async function handle(code){
    code=(code||'').trim(); if(!code) return;
    if(!toko()){ tampilkanPesanGagalFormal("pemindaian stok opname", '<%=Common.getBahasaConfigJS("Toko/kios belum dipilih, padahal wajib dipilih.")%>', ["Pilih toko/kios terlebih dahulu sebelum memindai barang."]); return; }
    for(var i=0;i<items.length;i++){ if(items[i].barcode===code){ items[i].fisik=(parseFloat(items[i].fisik)||0)+1; render(); beep(true); return; } }
    try{
      var r=await post({aksi:'cariBarcode', barcode:code, toko:toko()});
      if(r.status!=='00'){ beep(false); tampilkanPesanGagalFormal("pencarian barcode saat opname stok", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Ulangi pemindaian barcode.", "Bila berulang gagal, ketik kode barcode secara manual."]); return; }
      if(!r.found){ beep(false); tampilkanPesanGagalFormal("pencarian barcode saat opname stok", r.message||('Barcode '+code+' tidak ditemukan di dalam sistem.'), ["Pastikan barcode terdaftar sebagai produk pada toko/kios ini.", "Periksa kembali fisik barcode, atau masukkan kode secara manual."]); return; }
      items.push({barcode:code, produkId:r.produkId, kode:r.kode, nama:r.nama, sistem:Number(r.stokSistem)||0, fisik:1});
      render(); beep(true);
    }catch(e){ beep(false); tampilkanPesanGagalFormal("pencarian barcode saat opname stok", "Rincian teknis: "+e.message, ["Periksa koneksi internet Bapak/Ibu.", "Ulangi pemindaian barcode."]); }
  }

  window['osAdd'+rnd]=function(){ var el=$('osScan'); handle(el.value); el.value=''; el.focus(); };
  (function(){ var el=$('osScan'); el.addEventListener('keydown',function(e){ if(e.key==='Enter'||e.keyCode===13){ e.preventDefault(); handle(el.value); el.value=''; } }); el.focus(); })();

  window['osToggleCam'+rnd]=function(){
    var wrap=$('osReaderWrap'), btn=$('osCamBtn');
    if(cam){ cam.stop().then(function(){cam.clear();cam=null;}).catch(function(){cam=null;}); wrap.style.display='none'; btn.classList.remove('btn-danger'); btn.classList.add('btn-outline-dark'); return; }
    if(typeof Html5Qrcode==='undefined'){ tampilkanPesanGagalFormal("pengaktifan kamera untuk pemindaian barcode", '<%=Common.getBahasaConfigJS("Pustaka kamera (Html5Qrcode) belum berhasil termuat pada peramban Bapak/Ibu.")%>', ["Periksa koneksi internet Bapak/Ibu, lalu muat ulang halaman.", "Sebagai alternatif, gunakan alat PDT atau ketik kode barcode secara manual."]); return; }
    wrap.style.display='block'; btn.classList.add('btn-danger'); btn.classList.remove('btn-outline-dark');
    cam=new Html5Qrcode('osReader'+rnd);
    cam.start({facingMode:'environment'},{fps:10,qrbox:250},function(txt){
      var now=Date.now(); if(txt===lastCode && (now-lastTime)<1500) return; lastCode=txt; lastTime=now; handle(txt);
    },function(){}).catch(function(err){ tampilkanPesanGagalFormal("pengaktifan kamera untuk pemindaian barcode", "Rincian teknis: "+err, ["Pastikan izin akses kamera pada peramban sudah diberikan untuk halaman ini.", "Sebagai alternatif, gunakan alat PDT atau ketik kode barcode secara manual."]); wrap.style.display='none'; cam=null; });
  };

  window['osLoadRingkasan'+rnd]=async function(){
    var ids=['osTodProduk','osTodLebih','osTodKurang','osTodNet'];
    if(!toko()){ for(var k=0;k<ids.length;k++) $(ids[k]).textContent='-'; return; }
    try{ var r=await post({aksi:'ringkasan', toko:toko()});
      if(r.status==='00'){ $('osTodProduk').textContent=r.jumlahProduk||0; $('osTodLebih').textContent=fmt(r.totalLebih||0);
        $('osTodKurang').textContent=fmt(r.totalKurang||0); var n=Number(r.selisihBersih)||0;
        $('osTodNet').textContent=(n>0?'+':'')+fmt(n); $('osTodNet').style.color=n<0?'var(--c-kurang)':(n>0?'var(--c-lebih)':'var(--c-net)'); }
    }catch(e){}
  };

  window['osSimpan'+rnd]=async function(){
    if(!items.length) return;
    if(!toko()){ tampilkanPesanGagalFormal("pemindaian stok opname", '<%=Common.getBahasaConfigJS("Toko/kios belum dipilih, padahal wajib dipilih.")%>', ["Pilih toko/kios terlebih dahulu sebelum memindai barang."]); return; }
    if(!confirm('Simpan '+items.length+' hasil opname? Stok akan disesuaikan.')) return;
    var payload=[]; for(var i=0;i<items.length;i++){ payload.push({produkId:items[i].produkId, fisik:items[i].fisik}); }
    $('osSave').disabled=true;
    try{
      var r=await post({aksi:'simpan', toko:toko(), keterangan:$('osKet').value, items:JSON.stringify(payload)});
      if(r.status!=='00'){ tampilkanPesanGagalFormal("penyimpanan hasil opname stok", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data hasil opname.", "Ulangi proses penyimpanan beberapa saat lagi."]); $('osSave').disabled=false; return; }
      tampilkanPesanSuksesFormal("penyimpanan hasil opname stok", 'Berhasil menyimpan '+r.sukses+' dari '+r.total+' item. Stok telah disesuaikan.');
      items=[]; render(); $('osKet').value=''; $('osScan').focus(); window['osLoadRingkasan'+rnd]();
    }catch(e){ tampilkanPesanGagalFormal("penyimpanan hasil opname stok", "Rincian teknis: "+e.message, ["Periksa koneksi internet Bapak/Ibu.", "Ulangi proses penyimpanan beberapa saat lagi."]); $('osSave').disabled=false; }
  };

  render();
  window['osLoadRingkasan'+rnd]();
})();
</script>
