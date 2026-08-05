<%@page import="ais.common.Common"%>
<%
String rndD = Common.getGeneratedBarCode(6);
String svcDI = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fkulakan&s=draft_inventaris_service";
%>

<div class="card border-0 shadow-sm rounded-4 mb-3">
  <div class="card-body">
    <div class="mb-1">
      <h6 class="fw-bold mb-0"><i class="fas fa-clipboard-check me-2 text-primary"></i><%=Common.getBahasaConfig("Draft Inventaris")%></h6>
      <small class="text-muted"><%=Common.getBahasaConfig("Barang yang sudah diterima (BAST disetujui). Jadikan Inventaris (aset/sarpras) atau Persediaan — persediaan otomatis masuk ke data kulakan.")%></small>
    </div>
    <div class="row g-2 align-items-end mt-1">
      <div class="col-12 col-md-5"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Cari Barang")%></label><input type="text" id="difQ<%=rndD%>" class="form-control form-control-sm" placeholder="nama/kode barang..."></div>
      <div class="col-6 col-md-3"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Status")%></label>
        <select id="difStatus<%=rndD%>" class="form-select form-select-sm">
          <option value="semua"><%=Common.getBahasaConfig("Semua")%></option>
          <option value="draft" selected><%=Common.getBahasaConfig("Belum Diproses")%></option>
          <option value="selesai"><%=Common.getBahasaConfig("Sudah Diproses")%></option>
        </select></div>
      <div class="col-6 col-md-3"><button class="btn btn-outline-primary btn-sm w-100" onclick="diLoad<%=rndD%>()"><i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Tampilkan")%></button></div>
    </div>
  </div>
</div>

<div class="table-responsive">
  <table class="table table-hover align-middle">
    <thead class="table-light"><tr>
      <th>#</th><th><%=Common.getBahasaConfig("BAST")%></th><th><%=Common.getBahasaConfig("Barang")%></th>
      <th class="text-end"><%=Common.getBahasaConfig("Jumlah")%></th><th class="text-center"><%=Common.getBahasaConfig("Kategori")%></th>
      <th class="text-center"><%=Common.getBahasaConfig("Status")%></th><th class="text-end"><%=Common.getBahasaConfig("Aksi")%></th>
    </tr></thead>
    <tbody id="diBody<%=rndD%>"><tr><td colspan="7" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr></tbody>
  </table>
</div>

<script>
(function(){
  var SVC='<%=svcDI%>', rnd='<%=rndD%>';
  function $(id){ return document.getElementById(id+rnd); }
  function esc(s){ return (s==null?'':String(s)).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];}); }
  function num(n){ n=Number(n)||0; return n.toLocaleString('id-ID'); }
  async function post(params){ var res=await fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},body:new URLSearchParams(params)}); return JSON.parse((await res.text()).trim()); }

  window['diLoad'+rnd]=async function(){
    var tb=$('diBody'); tb.innerHTML='<tr><td colspan="7" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr>';
    try{ var r=await post({aksi:'list', q:$('difQ').value, status:$('difStatus').value});
      if(r.status!=='00'){ tb.innerHTML='<tr><td colspan="7" class="text-center text-danger py-4">'+esc(r.message||'Gagal')+'</td></tr>'; return; }
      if(!r.data||!r.data.length){ tb.innerHTML='<tr><td colspan="7" class="text-center text-muted py-4">Tidak ada data.</td></tr>'; return; }
      var html='';
      r.data.forEach(function(p,i){
        var kat = p.kategori==='inventaris'
          ? '<span class="badge bg-primary"><i class="fas fa-cube me-1"></i>Inventaris</span>'
          : '<span class="badge bg-info text-dark"><i class="fas fa-boxes me-1"></i>Persediaan</span>';
        var stat, aksi;
        if(p.selesai){
          stat = '<span class="badge bg-success">Sudah Diproses</span>'; aksi='-';
        } else {
          stat = '<span class="badge bg-warning text-dark">Belum Diproses</span>';
          if(p.kategori==='inventaris'){
            aksi='<button class="btn btn-sm btn-primary" onclick="diInventaris'+rnd+'('+p.id+')"><i class="fas fa-cube me-1"></i>Jadikan Inventaris</button>';
          } else {
            aksi='<button class="btn btn-sm btn-info text-dark" onclick="diPersediaan'+rnd+'('+p.id+')"><i class="fas fa-dolly-flatbed me-1"></i>Jadikan Persediaan</button>';
          }
        }
        html+='<tr><td>'+(i+1)+'</td><td class="fw-semibold">'+esc(p.bast)+'</td><td>'+esc(p.barang)+'</td><td class="text-end">'+num(p.qty)+'</td><td class="text-center">'+kat+'</td><td class="text-center">'+stat+'</td><td class="text-end">'+aksi+'</td></tr>';
      });
      tb.innerHTML=html;
    }catch(e){ tb.innerHTML='<tr><td colspan="7" class="text-center text-danger py-4">Error: '+esc(e.message)+'</td></tr>'; }
  };

  window['diInventaris'+rnd]=async function(id){
    if(!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menjadikan barang ini sebagai Inventaris/Sarpras? Sistem akan membuat data Aset.")%>')) return;
    var r=await post({aksi:'jadikanInventaris', id:id});
    if(r.status==='00'){ alert(r.message||'<%=Common.getBahasaConfigJS("Proses berhasil dilakukan.")%>'); window['diLoad'+rnd](); } else alert(r.message||'<%=Common.getBahasaConfigJS("Proses gagal dilakukan. Silakan coba lagi.")%>');
  };
  window['diPersediaan'+rnd]=async function(id){
    if(!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menjadikan barang ini sebagai Persediaan? Barang akan masuk ke data kulakan.")%>')) return;
    var r=await post({aksi:'jadikanPersediaan', id:id});
    if(r.status==='00'){ alert(r.message||'<%=Common.getBahasaConfigJS("Proses berhasil dilakukan.")%>'); window['diLoad'+rnd](); } else alert(r.message||'<%=Common.getBahasaConfigJS("Proses gagal dilakukan. Silakan coba lagi.")%>');
  };

  window['diLoad'+rnd]();
})();
</script>
