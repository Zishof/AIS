<%@page import="ais.common.Common"%>
<%
String rndT = Common.getGeneratedBarCode(6);
String svcTG = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fkulakan&s=saldo_awal_asset_service";
String postingUrl = Common.ROOT + "/pages/master/asset/posting_saldo_awal_asset.zul";
%>

<div class="card border-0 shadow-sm rounded-4 mb-3">
  <div class="card-body">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-1">
      <div>
        <h6 class="fw-bold mb-0"><i class="fas fa-file-invoice-dollar me-2 text-primary"></i><%=Common.getBahasaConfig("Terima Tagihan (Ubah ke Inventaris)")%></h6>
        <small class="text-muted"><%=Common.getBahasaConfig("Catat tagihan dari barang yang sudah diterima, lalu posting ke jurnal akuntansi.")%></small>
      </div>
      <button class="btn btn-primary rounded-pill px-3" onclick="tgBaru<%=rndT%>()"><i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Buat Tagihan")%></button>
    </div>
    <div class="row g-2 align-items-end mt-1">
      <div class="col-12 col-md-3"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Kode")%></label><input type="text" id="tgfKode<%=rndT%>" class="form-control form-control-sm"></div>
      <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Status")%></label>
        <select id="tgfStatus<%=rndT%>" class="form-select form-select-sm">
          <option value="semua"><%=Common.getBahasaConfig("Semua")%></option><option value="pending"><%=Common.getBahasaConfig("Menunggu")%></option><option value="disetujui"><%=Common.getBahasaConfig("Disetujui (belum posting)")%></option><option value="posting"><%=Common.getBahasaConfig("Terposting")%></option>
        </select></div>
      <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Dari")%></label><input type="date" id="tgfTglM<%=rndT%>" class="form-control form-control-sm"></div>
      <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Sampai")%></label><input type="date" id="tgfTglA<%=rndT%>" class="form-control form-control-sm"></div>
      <div class="col-6 col-md-3"><button class="btn btn-outline-primary btn-sm w-100" onclick="tgLoad<%=rndT%>()"><i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Tampilkan")%></button></div>
    </div>
  </div>
</div>

<div class="table-responsive">
  <table class="table table-hover align-middle">
    <thead class="table-light"><tr>
      <th>#</th><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Kode Tagihan")%></th><th><%=Common.getBahasaConfig("Tanggal")%></th>
      <th><%=Common.getBahasaConfig("Penyedia")%></th><th><%=Common.getBahasaConfig("Toko")%></th><th class="text-center"><%=Common.getBahasaConfig("Item")%></th>
      <th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th><th class="text-center"><%=Common.getBahasaConfig("Status")%></th><th class="text-end"><%=Common.getBahasaConfig("Aksi")%></th>
    </tr></thead>
    <tbody id="tgBody<%=rndT%>"><tr><td colspan="10" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr></tbody>
  </table>
</div>

<div class="modal fade" id="tgModal<%=rndT%>" tabindex="-1" aria-hidden="true">
 <div class="modal-dialog modal-xl modal-dialog-scrollable">
  <div class="modal-content rounded-4">
   <div class="modal-header"><h5 class="modal-title fw-bold" id="tgTitle<%=rndT%>"><%=Common.getBahasaConfig("Terima Tagihan")%></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
   <div class="modal-body">
    <input type="hidden" id="tgId<%=rndT%>"><input type="hidden" id="tgBastId<%=rndT%>">
    <div class="row g-3">
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Kode")%></label><input type="text" id="tgKode<%=rndT%>" class="form-control" placeholder="otomatis"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Kode Tagihan / Faktur")%></label><input type="text" id="tgKodeTagihan<%=rndT%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Tanggal")%></label><input type="date" id="tgTgl<%=rndT%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Tanggal Tagihan")%></label><input type="date" id="tgTglTagihan<%=rndT%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Penyedia")%></label><select id="tgPenyedia<%=rndT%>" class="form-select"></select></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Satuan Kerja")%></label><select id="tgSatker<%=rndT%>" class="form-select"></select></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Toko / Pedagang")%></label><select id="tgToko<%=rndT%>" class="form-select"></select></div>
      <input type="hidden" id="tgDisposisi<%=rndT%>"><%-- SOP dijalankan via menu "Pengajuan SOP", bukan dipilih di CRUD ini --%>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Pemilik Aset")%></label><select id="tgPemilik<%=rndT%>" class="form-select"></select></div>
      <div class="col-12 col-md-3 position-relative"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Ruang")%></label>
        <input type="hidden" id="tgRuangId<%=rndT%>"><input type="text" id="tgRuang<%=rndT%>" class="form-control" placeholder="<%=Common.getBahasaConfig("cari ruang (opsional)...")%>" autocomplete="off" oninput="tgCariRuang<%=rndT%>()">
        <div id="tgRuangHasil<%=rndT%>" class="list-group position-absolute w-100 shadow" style="z-index:1095;max-height:220px;overflow:auto;display:none;"></div>
      </div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Lokasi")%></label><select id="tgLokasi<%=rndT%>" class="form-select"></select></div>
      <div class="col-12"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Keterangan")%></label><input type="text" id="tgKet<%=rndT%>" class="form-control"></div>
    </div>

    <hr class="my-3">
    <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 gap-2">
      <span class="fw-bold"><i class="fas fa-boxes me-2 text-primary"></i><%=Common.getBahasaConfig("Rincian Tagihan")%></span>
      <button class="btn btn-sm btn-outline-success rounded-pill" onclick="tgBukaImporBast<%=rndT%>()"><i class="fas fa-file-import me-1"></i><%=Common.getBahasaConfig("Ambil dari Penerimaan (BAST)")%></button>
    </div>
    <div class="position-relative mb-2">
      <input type="text" id="tgCari<%=rndT%>" class="form-control" placeholder="<%=Common.getBahasaConfig("Ketik nama/kode barang lalu pilih...")%>" autocomplete="off" oninput="tgCariBarang<%=rndT%>()">
      <div id="tgCariHasil<%=rndT%>" class="list-group position-absolute w-100 shadow" style="z-index:1090;max-height:240px;overflow:auto;display:none;"></div>
    </div>
    <div class="table-responsive">
      <table class="table table-sm align-middle">
        <thead class="table-light"><tr>
          <th><%=Common.getBahasaConfig("Barang")%></th><th style="width:90px" class="text-end"><%=Common.getBahasaConfig("Jumlah")%></th>
          <th style="width:120px" class="text-end"><%=Common.getBahasaConfig("Harga")%></th><th style="width:100px" class="text-end"><%=Common.getBahasaConfig("Potongan")%></th>
          <th style="width:40px" class="text-center" title="Potongan dalam %">%</th><th style="width:70px" class="text-end"><%=Common.getBahasaConfig("PPN%")%></th>
          <th style="width:70px" class="text-end"><%=Common.getBahasaConfig("PPh%")%></th><th style="width:140px" class="text-end"><%=Common.getBahasaConfig("Subtotal")%></th><th style="width:36px"></th>
        </tr></thead>
        <tbody id="tgLines<%=rndT%>"></tbody>
        <tfoot><tr class="table-light fw-bold"><td colspan="7" class="text-end"><%=Common.getBahasaConfig("Total Nilai")%></td><td class="text-end" id="tgTotal<%=rndT%>">Rp 0</td><td></td></tr></tfoot>
      </table>
    </div>
   </div>
   <div class="modal-footer">
     <button type="button" class="btn btn-light rounded-pill" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
     <button type="button" class="btn btn-primary rounded-pill px-4" id="tgBtnSimpan<%=rndT%>" onclick="tgSimpan<%=rndT%>()"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan")%></button>
   </div>
  </div>
 </div>
</div>

<div class="modal fade" id="tgBastModal<%=rndT%>" tabindex="-1" aria-hidden="true">
 <div class="modal-dialog modal-lg modal-dialog-scrollable"><div class="modal-content rounded-4">
   <div class="modal-header"><h6 class="modal-title fw-bold"><%=Common.getBahasaConfig("Pilih Penerimaan (BAST) yang Disetujui")%></h6><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
   <div class="modal-body"><div class="table-responsive"><table class="table table-hover align-middle">
     <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Penyedia")%></th><th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th><th></th></tr></thead>
     <tbody id="tgBastBody<%=rndT%>"></tbody></table></div></div>
 </div></div>
</div>

<script>
(function(){
  var SVC='<%=svcTG%>', POSTING_URL='<%=postingUrl%>', rnd='<%=rndT%>', comboLoaded=false, lockToko=false, tokoTerkunci='';
  function $(id){ return document.getElementById(id+rnd); }
  function rupiah(n){ n=Number(n)||0; return 'Rp '+n.toLocaleString('id-ID'); }
  function esc(s){ return (s==null?'':String(s)).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];}); }
  function num(v){ return Number(v)||0; }
  async function post(params){ var res=await fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},body:new URLSearchParams(params)}); return JSON.parse((await res.text()).trim()); }
  function opt(list,val){ var h='<option value="">- pilih -</option>'; (list||[]).forEach(function(x){ h+='<option value="'+x.id+'"'+(String(x.id)===String(val)?' selected':'')+'>'+esc(x.nama)+'</option>'; }); return h; }
  function lineTotal(jml,h,pot,pct,ppn,pph){ var dpp=jml*h; var potongan=pct?((pot/100)*dpp):pot; dpp=dpp-potongan; var vppn=(ppn/100)*dpp; var vpph=(pph/100)*dpp; return Math.round((dpp+vppn)-vpph); }

  window['tgLoad'+rnd]=async function(){
    var tb=$('tgBody'); tb.innerHTML='<tr><td colspan="10" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr>';
    try{ var r=await post({aksi:'list',kode:$('tgfKode').value,status:$('tgfStatus').value,tglMulai:$('tgfTglM').value,tglAkhir:$('tgfTglA').value});
      if(r.status!=='00'){ tb.innerHTML='<tr><td colspan="10" class="text-center text-danger py-4">'+esc(r.message||'Gagal')+'</td></tr>'; return; }
      if(!r.data||!r.data.length){ tb.innerHTML='<tr><td colspan="10" class="text-center text-muted py-4">Belum ada data.</td></tr>'; return; }
      var html='';
      r.data.forEach(function(p,i){
        var badge=p.status==='posting'?'<span class="badge bg-success">Terposting</span>':(p.status==='disetujui'?'<span class="badge bg-info text-dark">Disetujui</span>':'<span class="badge bg-warning text-dark">Menunggu</span>');
        var aksi='<button class="btn btn-sm btn-outline-secondary me-1" onclick="tgEdit'+rnd+'('+p.id+')"><i class="fas fa-eye"></i></button>';
        if(p.status==='pending'){ aksi+='<button class="btn btn-sm btn-outline-success me-1" title="Setujui" onclick="tgSetujui'+rnd+'('+p.id+')"><i class="fas fa-check"></i></button><button class="btn btn-sm btn-outline-danger" onclick="tgHapus'+rnd+'('+p.id+')"><i class="fas fa-trash"></i></button>'; }
        else if(p.status==='disetujui'){ aksi+='<button class="btn btn-sm btn-success" title="Posting ke Jurnal" onclick="tgPosting'+rnd+'()"><i class="fas fa-book me-1"></i>Posting</button>'; }
        html+='<tr><td>'+(i+1)+'</td><td class="fw-semibold">'+esc(p.kode)+'</td><td>'+esc(p.kodeTagihan)+'</td><td>'+esc(p.tanggal)+'</td><td>'+esc(p.penyedia)+'</td><td>'+esc(p.toko||'')+'</td><td class="text-center">'+p.jumlahItem+'</td><td class="text-end">'+rupiah(p.nilai)+'</td><td class="text-center">'+badge+(p.sop?' <i class="fas fa-route text-info" title="Dijalankan via SOP"></i>':'')+'</td><td class="text-end">'+aksi+'</td></tr>';
      });
      tb.innerHTML=html;
    }catch(e){ tb.innerHTML='<tr><td colspan="10" class="text-center text-danger py-4">Error: '+esc(e.message)+'</td></tr>'; }
  };

  async function loadCombo(){ if(comboLoaded) return; var r=await post({aksi:'combo'}); if(r.status==='00'){
    $('tgSatker').innerHTML=opt(r.satuanKerja); $('tgLokasi').innerHTML=opt(r.lokasi); $('tgPenyedia').innerHTML=opt(r.penyedia); $('tgToko').innerHTML=opt(r.toko); $('tgDisposisi').innerHTML=opt(r.disposisiSop); $('tgPemilik').innerHTML=opt(r.pemilikAsset);
    lockToko=!!r.lockToko; tokoTerkunci=r.tokoTerkunci||''; comboLoaded=true; } }
  function modal(){ return bootstrap.Modal.getOrCreateInstance($('tgModal')); }

  window['tgBaru'+rnd]=async function(){ await loadCombo();
    $('tgId').value=''; $('tgBastId').value=''; $('tgKode').value=''; $('tgKodeTagihan').value=''; $('tgTgl').value=new Date().toISOString().slice(0,10); $('tgTglTagihan').value='';
    $('tgPenyedia').value=''; $('tgSatker').value=''; $('tgLokasi').value=''; $('tgKet').value=''; $('tgToko').value = lockToko ? tokoTerkunci : ''; $('tgDisposisi').value=''; $('tgPemilik').value=''; $('tgRuang').value=''; $('tgRuangId').value='';
    $('tgLines').innerHTML=''; recalc(); enableForm(true);
    document.getElementById('tgTitle'+rnd).textContent='Buat Tagihan'; modal().show();
  };

  window['tgEdit'+rnd]=async function(id){ await loadCombo();
    var r=await post({aksi:'detail',id:id}); if(r.status!=='00'){ tampilkanPesanGagalFormal("pengelolaan Saldo Awal / Tagihan aset kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); return; } var d=r.data;
    $('tgId').value=d.id; $('tgBastId').value=d.bastId||''; $('tgKode').value=d.kode||''; $('tgKodeTagihan').value=d.kodeTagihan||''; $('tgTgl').value=d.tanggal||''; $('tgTglTagihan').value=d.tanggalTagihan||'';
    $('tgPenyedia').value=d.penyedia||''; $('tgSatker').value=d.satuanKerja||''; $('tgLokasi').value=d.lokasi||''; $('tgKet').value=d.keterangan||''; $('tgToko').value = lockToko ? tokoTerkunci : (d.toko||''); $('tgDisposisi').value=d.disposisiSop||''; $('tgPemilik').value=d.pemilikAsset||''; $('tgRuangId').value=d.ruang||''; $('tgRuang').value=d.ruangNama||'';
    $('tgLines').innerHTML=''; (d.lines||[]).forEach(function(l){ tambahBaris(l); }); recalc();
    var bisa = d.status==='pending'; enableForm(bisa);
    document.getElementById('tgTitle'+rnd).textContent = bisa?'Ubah Tagihan':(d.status==='posting'?'Detail Tagihan (Terposting)':'Detail Tagihan (Disetujui)');
    modal().show();
  };

  function enableForm(on){
    $('tgModal').querySelectorAll('.modal-body input,.modal-body select,.modal-body button').forEach(function(el){ if(el.id!=='tgCari'+rnd) el.disabled=!on; });
    $('tgCari').disabled=!on; if($('tgToko')) $('tgToko').disabled = !on || lockToko; $('tgBtnSimpan').style.display=on?'':'none';
  }

  var cariTimer=null;
  window['tgCariBarang'+rnd]=function(){ clearTimeout(cariTimer); cariTimer=setTimeout(async function(){
    var q=$('tgCari').value.trim(), box=$('tgCariHasil'); if(q.length<2){ box.style.display='none'; return; }
    var r=await post({aksi:'cariMasterAsset',q:q});
    if(r.status!=='00'||!r.data||!r.data.length){ box.innerHTML='<div class="list-group-item text-muted small">Tidak ada barang.</div>'; box.style.display='block'; return; }
    box.innerHTML=r.data.map(function(m){ return '<button type="button" class="list-group-item list-group-item-action" data-id="'+m.id+'" data-nama="'+esc(m.kode+' - '+m.nama)+'" data-harga="'+(m.harga||0)+'"><span class="fw-semibold">'+esc(m.nama)+'</span> <small class="text-muted">'+esc(m.kode)+'</small> <span class="float-end small">'+rupiah(m.harga)+'</span></button>'; }).join('');
    box.style.display='block';
    box.querySelectorAll('button').forEach(function(b){ b.onclick=function(){ tambahBaris({masterAsset:b.getAttribute('data-id'),masterAssetNama:b.getAttribute('data-nama'),jumlah:1,harga:Number(b.getAttribute('data-harga')),hargaPotongan:0,diskonPersen:false,persenPpn:0,persenPph:0,keterangan:''}); recalc(); box.style.display='none'; $('tgCari').value=''; }; });
  },250); };
  document.addEventListener('click',function(e){ var box=$('tgCariHasil'); if(box&&!box.contains(e.target)&&e.target!==$('tgCari')) box.style.display='none'; });

  // Pemilih cari Ruang ala banbox ZK
  function pickerCari(inKey, hasilKey, hiddenKey, aksi){
    clearTimeout(cariTimer);
    cariTimer = setTimeout(async function(){
      $(hiddenKey).value='';
      var q=$(inKey).value.trim(), box=$(hasilKey);
      if(q.length<2){ box.style.display='none'; return; }
      var r=await post({aksi:aksi, q:q});
      if(r.status!=='00'||!r.data||!r.data.length){ box.innerHTML='<div class="list-group-item text-muted small">Tidak ada data.</div>'; box.style.display='block'; return; }
      box.innerHTML=r.data.map(function(x){ return '<button type="button" class="list-group-item list-group-item-action" data-id="'+x.id+'" data-nama="'+esc(x.nama)+'">'+esc(x.nama)+'</button>'; }).join('');
      box.style.display='block';
      box.querySelectorAll('button').forEach(function(b){ b.onclick=function(){ $(hiddenKey).value=b.getAttribute('data-id'); $(inKey).value=b.getAttribute('data-nama'); box.style.display='none'; }; });
    },250);
  }
  window['tgCariRuang'+rnd]=function(){ pickerCari('tgRuang','tgRuangHasil','tgRuangId','cariRuang'); };
  document.addEventListener('click',function(e){ var box=$('tgRuangHasil'); if(box&&!box.contains(e.target)&&e.target!==$('tgRuang')) box.style.display='none'; });

  function tambahBaris(l){
    var tr=document.createElement('tr');
    tr.setAttribute('data-id', l.masterAsset||''); tr.setAttribute('data-bastdetail', l.bastDetailId||'');
    tr.innerHTML='<td>'+esc(l.masterAssetNama||'')+'<input type="hidden" class="bb-ket" value="'+esc(l.keterangan||'')+'"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-jml" value="'+num(l.jumlah||1)+'" oninput="tgRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-hrg" value="'+num(l.harga||0)+'" oninput="tgRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-pot" value="'+num(l.hargaPotongan||0)+'" oninput="tgRecalc'+rnd+'()"></td>'
      +'<td class="text-center"><input type="checkbox" class="form-check-input bb-pct" '+(l.diskonPersen?'checked':'')+' onchange="tgRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-ppn" value="'+num(l.persenPpn||0)+'" oninput="tgRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-pph" value="'+num(l.persenPph||0)+'" oninput="tgRecalc'+rnd+'()"></td>'
      +'<td class="text-end bb-sub">Rp 0</td>'
      +'<td class="text-center"><button class="btn btn-sm btn-outline-danger" onclick="this.closest(\'tr\').remove();tgRecalc'+rnd+'()"><i class="fas fa-times"></i></button></td>';
    $('tgLines').appendChild(tr);
  }
  function recalc(){ var total=0;
    $('tgLines').querySelectorAll('tr').forEach(function(tr){
      var t=lineTotal(num(tr.querySelector('.bb-jml').value),num(tr.querySelector('.bb-hrg').value),num(tr.querySelector('.bb-pot').value),tr.querySelector('.bb-pct').checked,num(tr.querySelector('.bb-ppn').value),num(tr.querySelector('.bb-pph').value));
      tr.querySelector('.bb-sub').textContent=rupiah(t); total+=t; });
    $('tgTotal').textContent=rupiah(total);
  }
  window['tgRecalc'+rnd]=recalc;

  window['tgBukaImporBast'+rnd]=async function(){
    var r=await post({aksi:'listBast'}); var tb=$('tgBastBody');
    if(r.status!=='00'){ tampilkanPesanGagalFormal("pengelolaan Saldo Awal / Tagihan aset kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); return; }
    if(!r.data||!r.data.length){ tb.innerHTML='<tr><td colspan="4" class="text-center text-muted py-3">Tidak ada BAST disetujui yang belum ditagihkan.</td></tr>'; }
    else tb.innerHTML=r.data.map(function(p){ return '<tr><td class="fw-semibold">'+esc(p.kode)+'</td><td>'+esc(p.penyedia)+'</td><td class="text-end">'+rupiah(p.nilai)+'</td><td><button class="btn btn-sm btn-primary" onclick="tgImporBast'+rnd+'('+p.id+')">Ambil</button></td></tr>'; }).join('');
    bootstrap.Modal.getOrCreateInstance($('tgBastModal')).show();
  };
  window['tgImporBast'+rnd]=async function(bastId){
    var r=await post({aksi:'importBast',id:bastId}); if(r.status!=='00'){ tampilkanPesanGagalFormal("pengelolaan Saldo Awal / Tagihan aset kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); return; } var d=r.data;
    $('tgBastId').value=d.bastId||''; if(d.penyedia) $('tgPenyedia').value=d.penyedia; if(d.satuanKerja) $('tgSatker').value=d.satuanKerja; if(d.lokasi) $('tgLokasi').value=d.lokasi;
    if(!lockToko && d.toko) $('tgToko').value=d.toko; if(d.kodeTagihan&&!$('tgKodeTagihan').value) $('tgKodeTagihan').value=d.kodeTagihan; if(d.keterangan&&!$('tgKet').value) $('tgKet').value=d.keterangan;
    (d.lines||[]).forEach(function(l){ tambahBaris(l); }); recalc();
    bootstrap.Modal.getOrCreateInstance($('tgBastModal')).hide();
  };

  window['tgSimpan'+rnd]=async function(){
    var lines=[];
    $('tgLines').querySelectorAll('tr').forEach(function(tr){ lines.push({ masterAsset:tr.getAttribute('data-id'), bastDetailId:tr.getAttribute('data-bastdetail')||'', jumlah:num(tr.querySelector('.bb-jml').value), harga:num(tr.querySelector('.bb-hrg').value), hargaPotongan:num(tr.querySelector('.bb-pot').value), diskonPersen:tr.querySelector('.bb-pct').checked, persenPpn:num(tr.querySelector('.bb-ppn').value), persenPph:num(tr.querySelector('.bb-pph').value), keterangan:tr.querySelector('.bb-ket').value||'' }); });
    if(!lines.length){ tampilkanPesanGagalFormal("penyimpanan Saldo Awal / Tagihan aset", '<%=Common.getBahasaConfigJS("Belum ada satu pun barang yang ditambahkan pada rincian ini.")%>', ["Klik tombol cari/tambah barang untuk memasukkan minimal satu baris barang.", "Setelah rincian terisi, silakan simpan kembali."]); return; }
    var payload={ id:$('tgId').value, bastId:$('tgBastId').value, kode:$('tgKode').value, kodeTagihan:$('tgKodeTagihan').value, tanggal:$('tgTgl').value, tanggalTagihan:$('tgTglTagihan').value, satuanKerja:$('tgSatker').value, lokasi:$('tgLokasi').value, toko:$('tgToko').value, disposisiSop:$('tgDisposisi').value, pemilikAsset:$('tgPemilik').value, ruang:$('tgRuangId').value, penyedia:$('tgPenyedia').value, keterangan:$('tgKet').value, lines:lines };
    var btn=$('tgBtnSimpan'); btn.disabled=true; var ori=btn.innerHTML; btn.innerHTML='<span class="spinner-border spinner-border-sm me-2"></span>Menyimpan...';
    try{ var r=await post({aksi:'simpan',data:JSON.stringify(payload)}); if(r.status==='00'){ modal().hide(); window['tgLoad'+rnd](); } else tampilkanPesanGagalFormal("pengelolaan Saldo Awal / Tagihan aset kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); }
    catch(e){ tampilkanPesanGagalFormal("penyimpanan Saldo Awal / Tagihan aset", "Rincian teknis: "+e.message, ["Periksa koneksi internet Bapak/Ibu.", "Ulangi proses penyimpanan beberapa saat lagi."]); } finally{ btn.disabled=false; btn.innerHTML=ori; }
  };
  window['tgSetujui'+rnd]=async function(id){ if(!confirm('<%= Common.getBahasaConfigJS("Apakah Bapak/Ibu yakin ingin menyetujui tagihan ini?") %>'))return; var r=await post({aksi:'setujui',id:id}); if(r.status==='00'){ tampilkanPesanSuksesFormal("persetujuan tagihan", r.message||''); window['tgLoad'+rnd](); } else tampilkanPesanGagalFormal("pengelolaan Saldo Awal / Tagihan aset kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); };
  window['tgHapus'+rnd]=async function(id){ if(!confirm('<%= Common.getBahasaConfigJS("Apakah Bapak/Ibu yakin ingin menghapus tagihan ini?") %>'))return; var r=await post({aksi:'hapus',id:id}); if(r.status==='00') window['tgLoad'+rnd](); else tampilkanPesanGagalFormal("pengelolaan Saldo Awal / Tagihan aset kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); };
  window['tgPosting'+rnd]=function(){ window.open(POSTING_URL, '_blank'); };

  window['tgLoad'+rnd]();
})();
</script>
