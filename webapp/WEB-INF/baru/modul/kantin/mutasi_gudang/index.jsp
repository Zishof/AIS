<%--
  "Mutasi Gudang" (versi JSP) — catat barang MASUK / KELUAR / TRANSFER antar lokasi / PENYESUAIAN,
  dengan pratinjau stok berjalan + riwayat mutasi. Reuse mutasi_gudang/service.jsp (StokLokasiUtil).
  Gate admin (bukan pedagang/toko). Aman di-include berkali-kali (id ber-akhiran rnd).
--%>
<%@page import="ais.common.Common"%>
<%@page import="ais.action.master.koperasi.helper.LokasiKantinUtil"%>
<%
String rmg = ais.common.Common.getGeneratedBarCode(6);
boolean bolehMG = LokasiKantinUtil.bolehKelola(request);
String svcMG = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fmutasi_gudang&s=service";
%>
<div class="mg-wrap-<%=rmg%>">
  <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
    <div>
      <h5 class="fw-bold mb-0"><i class="fas fa-dolly text-primary me-2"></i><%=Common.getBahasaConfig("Mutasi Gudang")%></h5>
      <div class="text-muted small"><%=Common.getBahasaConfig("Catat barang masuk, keluar, pindah antar gudang, atau koreksi stok. Stok tiap lokasi dihitung otomatis dari catatan ini.")%></div>
    </div>
  </div>

  <% if (!bolehMG) { %>
  <div class="alert alert-light border small py-2"><i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Anda hanya dapat melihat riwayat. Pencatatan hanya untuk admin (bukan pedagang/toko).")%></div>
  <% } %>

  <div class="row g-3">
    <% if (bolehMG) { %>
    <div class="col-lg-5">
      <div class="card border-0 shadow-sm rounded-4 h-100"><div class="card-body">
        <div class="btn-group w-100 mb-3" role="group">
          <input type="radio" class="btn-check" name="mgJenis<%=rmg%>" id="mgM<%=rmg%>" value="MASUK" checked onchange="mgJenis<%=rmg%>()">
          <label class="btn btn-outline-success btn-sm" for="mgM<%=rmg%>"><i class="fas fa-arrow-down me-1"></i>Masuk</label>
          <input type="radio" class="btn-check" name="mgJenis<%=rmg%>" id="mgK<%=rmg%>" value="KELUAR" onchange="mgJenis<%=rmg%>()">
          <label class="btn btn-outline-danger btn-sm" for="mgK<%=rmg%>"><i class="fas fa-arrow-up me-1"></i>Keluar</label>
          <input type="radio" class="btn-check" name="mgJenis<%=rmg%>" id="mgT<%=rmg%>" value="TRANSFER" onchange="mgJenis<%=rmg%>()">
          <label class="btn btn-outline-primary btn-sm" for="mgT<%=rmg%>"><i class="fas fa-exchange-alt me-1"></i>Transfer</label>
          <input type="radio" class="btn-check" name="mgJenis<%=rmg%>" id="mgP<%=rmg%>" value="PENYESUAIAN" onchange="mgJenis<%=rmg%>()">
          <label class="btn btn-outline-secondary btn-sm" for="mgP<%=rmg%>"><i class="fas fa-sliders-h me-1"></i>Opname</label>
        </div>

        <label class="form-label small fw-semibold" id="mgLblLok<%=rmg%>"><%=Common.getBahasaConfig("Lokasi")%></label>
        <select id="mgLok<%=rmg%>" class="form-select mb-2" onchange="mgCekStok<%=rmg%>()"></select>

        <div id="mgTujuanBox<%=rmg%>" style="display:none">
          <label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Lokasi Tujuan")%></label>
          <select id="mgTujuan<%=rmg%>" class="form-select mb-2"></select>
        </div>

        <label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Produk")%></label>
        <select id="mgProduk<%=rmg%>" class="form-select mb-1" onchange="mgCekStok<%=rmg%>()"></select>
        <div class="small text-muted mb-2">Stok di lokasi: <b id="mgStok<%=rmg%>">-</b></div>

        <div class="row g-2">
          <div class="col-6"><label class="form-label small fw-semibold" id="mgLblQty<%=rmg%>"><%=Common.getBahasaConfig("Jumlah")%></label>
            <input type="number" step="any" id="mgQty<%=rmg%>" class="form-control"/></div>
          <div class="col-6"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Harga Satuan")%></label>
            <input type="number" step="any" id="mgHarga<%=rmg%>" class="form-control"/></div>
        </div>
        <div class="row g-2 mt-0">
          <div class="col-6"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Tanggal")%></label>
            <input type="date" id="mgTgl<%=rmg%>" class="form-control"/></div>
          <div class="col-6"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Keterangan")%></label>
            <input id="mgKet<%=rmg%>" class="form-control"/></div>
        </div>
        <button class="btn btn-primary w-100 mt-3" onclick="mgSimpan<%=rmg%>()"><i class="fas fa-save me-1"></i>Simpan Mutasi</button>
      </div></div>
    </div>
    <% } %>

    <div class="col-lg-<%= bolehMG ? "7" : "12" %>">
      <div class="card border-0 shadow-sm rounded-4 h-100"><div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <h6 class="fw-bold mb-0"><%=Common.getBahasaConfig("Riwayat Mutasi Terbaru")%></h6>
          <select id="mgFilter<%=rmg%>" class="form-select form-select-sm" style="max-width:200px" onchange="mgLoadList<%=rmg%>()"><option value="">Semua lokasi</option></select>
        </div>
        <div class="table-responsive" style="max-height:520px;overflow:auto">
          <table class="table table-sm table-hover align-middle">
            <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Tanggal")%></th><th><%=Common.getBahasaConfig("Jenis")%></th><th><%=Common.getBahasaConfig("Lokasi")%></th><th><%=Common.getBahasaConfig("Produk")%></th><th class="text-end"><%=Common.getBahasaConfig("Qty")%></th></tr></thead>
            <tbody id="mgBody<%=rmg%>"><tr><td colspan="5" class="text-center text-muted py-4"><i class="fas fa-spinner fa-spin me-1"></i>Memuat…</td></tr></tbody>
          </table>
        </div>
      </div></div>
    </div>
  </div>
</div>

<script>
(function(){
  var SVC='<%=svcMG%>', BOLEH=<%=bolehMG%>, lokasi=[], produk=[];
  function esc(s){return (s==null?'':String(s)).replace(/[&<>"]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
  function fmt(n){ return (Math.round((n||0)*100)/100).toLocaleString('id-ID'); }
  function jenisSel(){ var r=document.querySelector('input[name="mgJenis<%=rmg%>"]:checked'); return r?r.value:'MASUK'; }
  function fill(el, list, empty){ var h=empty?'<option value="">'+empty+'</option>':''; list.forEach(function(x){ h+='<option value="'+x.id+'">'+esc(x.nama)+(x.jenis?(' ('+esc(x.jenis)+')'):'')+'</option>'; }); el.innerHTML=h; }
  function fillProduk(el){ var h=''; produk.forEach(function(p){ h+='<option value="'+p.id+'" data-h="'+p.hargabeli+'">'+esc(p.kode?(p.kode+' — '):'')+esc(p.nama)+'</option>'; }); el.innerHTML=h; }

  window.mgJenis<%=rmg%>=function(){
    var j=jenisSel();
    document.getElementById('mgTujuanBox<%=rmg%>').style.display=(j==='TRANSFER')?'block':'none';
    document.getElementById('mgLblLok<%=rmg%>').textContent=(j==='TRANSFER')?'Lokasi Asal':'Lokasi';
    document.getElementById('mgLblQty<%=rmg%>').textContent=(j==='PENYESUAIAN')?'Selisih (+/-)':'Jumlah';
  };
  window.mgCekStok<%=rmg%>=function(){
    if(!BOLEH) return;
    var l=document.getElementById('mgLok<%=rmg%>').value, p=document.getElementById('mgProduk<%=rmg%>').value;
    var el=document.getElementById('mgStok<%=rmg%>'); if(!l||!p){ el.textContent='-'; return; }
    fetch(SVC+'&aksi=stok&lokasi='+l+'&produk='+p).then(function(r){return r.json();}).then(function(j){ el.textContent=fmt(j.stok); });
    var opt=document.getElementById('mgProduk<%=rmg%>').selectedOptions[0];
    if(opt && !document.getElementById('mgHarga<%=rmg%>').value){ document.getElementById('mgHarga<%=rmg%>').value=opt.getAttribute('data-h')||''; }
  };
  window.mgLoadList<%=rmg%>=function(){
    var l=document.getElementById('mgFilter<%=rmg%>').value;
    fetch(SVC+'&aksi=list'+(l?('&lokasi='+l):'')).then(function(r){return r.json();}).then(function(j){
      var b=document.getElementById('mgBody<%=rmg%>'), rows=(j&&j.data)||[], h='';
      if(!rows.length){ b.innerHTML='<tr><td colspan="5" class="text-center text-muted py-4">Belum ada mutasi.</td></tr>'; return; }
      rows.forEach(function(d){
        var neg=d.qty<0, badge={MASUK:'success',KELUAR:'danger',TRANSFER:'primary',PENYESUAIAN:'secondary'}[d.jenis]||'secondary';
        h+='<tr><td class="small">'+esc(d.tanggal)+'</td>'
          +'<td><span class="badge bg-'+badge+'-subtle text-'+badge+'">'+esc(d.jenis)+'</span>'+(d.pasangan?'<div class="small text-muted">↔ '+esc(d.pasangan)+'</div>':'')+'</td>'
          +'<td class="small">'+esc(d.lokasi)+'</td>'
          +'<td class="small">'+esc(d.produk)+(d.kode?'<div class="text-muted">'+esc(d.kode)+'</div>':'')+'</td>'
          +'<td class="text-end fw-semibold '+(neg?'text-danger':'text-success')+'">'+fmt(d.qty)+'</td></tr>';
      });
      b.innerHTML=h;
    });
  };
  window.mgSimpan<%=rmg%>=function(){
    var j=jenisSel(), l=document.getElementById('mgLok<%=rmg%>').value, p=document.getElementById('mgProduk<%=rmg%>').value;
    var qty=parseFloat(document.getElementById('mgQty<%=rmg%>').value||'0');
    if(!l){ tampilkanPesanGagalFormal("penyimpanan Mutasi Gudang", '<%=Common.getBahasaConfigJS("Lokasi belum dipilih, padahal wajib dipilih.")%>', ["Pilih Lokasi terlebih dahulu pada formulir ini."]); return; }
    if(!p){ tampilkanPesanGagalFormal("penyimpanan Mutasi Gudang", '<%=Common.getBahasaConfigJS("Produk belum dipilih, padahal wajib dipilih.")%>', ["Pilih Produk terlebih dahulu pada formulir ini."]); return; }
    if(j!=='PENYESUAIAN' && !(qty>0)){ tampilkanPesanGagalFormal("penyimpanan Mutasi Gudang", '<%=Common.getBahasaConfigJS("Jumlah yang dimasukkan harus lebih besar dari 0.")%>', ["Masukkan jumlah yang benar (lebih dari 0), lalu simpan kembali."]); return; }
    if(j==='PENYESUAIAN' && !qty){ tampilkanPesanGagalFormal("penyimpanan Mutasi Gudang (Penyesuaian)", '<%=Common.getBahasaConfigJS("Nilai selisih penyesuaian tidak boleh bernilai 0.")%>', ["Masukkan nilai selisih yang sesuai dengan hasil pemeriksaan stok."]); return; }
    var pr=new URLSearchParams();
    pr.append('aksi','simpan'); pr.append('jenis',j); pr.append('lokasi',l); pr.append('produk',p);
    pr.append('qty',qty); pr.append('harga',document.getElementById('mgHarga<%=rmg%>').value);
    pr.append('tanggal',document.getElementById('mgTgl<%=rmg%>').value); pr.append('keterangan',document.getElementById('mgKet<%=rmg%>').value);
    if(j==='TRANSFER') pr.append('lokasiTujuan',document.getElementById('mgTujuan<%=rmg%>').value);
    fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:pr.toString()})
      .then(function(r){return r.json();}).then(function(res){
        if(res.status==='00'){ document.getElementById('mgQty<%=rmg%>').value=''; document.getElementById('mgKet<%=rmg%>').value=''; window.mgCekStok<%=rmg%>(); window.mgLoadList<%=rmg%>(); }
        else tampilkanPesanGagalFormal("penyimpanan Mutasi Gudang", res.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]);
      });
  };
  fetch(SVC+'&aksi=ref').then(function(r){return r.json();}).then(function(j){
    lokasi=(j&&j.lokasi)||[]; produk=(j&&j.produk)||[];
    if(BOLEH){ fill(document.getElementById('mgLok<%=rmg%>'),lokasi,'— pilih —'); fill(document.getElementById('mgTujuan<%=rmg%>'),lokasi,'— pilih —'); fillProduk(document.getElementById('mgProduk<%=rmg%>')); }
    fill(document.getElementById('mgFilter<%=rmg%>'),lokasi,'Semua lokasi');
    window.mgLoadList<%=rmg%>();
  });
})();
</script>
