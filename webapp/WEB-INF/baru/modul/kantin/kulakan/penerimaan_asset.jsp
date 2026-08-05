<%@page import="ais.common.Common"%>
<%
String rndB = Common.getGeneratedBarCode(6);
String svcBAST = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fkulakan&s=penerimaan_asset_service";
%>

<div class="card border-0 shadow-sm rounded-4 mb-3">
  <div class="card-body">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-1">
      <div>
        <h6 class="fw-bold mb-0"><i class="fas fa-dolly me-2 text-primary"></i><%=Common.getBahasaConfig("Penerimaan Barang/Jasa (BAST)")%></h6>
        <small class="text-muted"><%=Common.getBahasaConfig("Catat barang yang diterima dari pesanan. Saat disetujui, stok otomatis bertambah.")%></small>
      </div>
      <button class="btn btn-primary rounded-pill px-3" onclick="baBaru<%=rndB%>()"><i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Buat Penerimaan")%></button>
    </div>
    <div class="row g-2 align-items-end mt-1">
      <div class="col-12 col-md-3"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Kode")%></label><input type="text" id="bafKode<%=rndB%>" class="form-control form-control-sm"></div>
      <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Status")%></label>
        <select id="bafStatus<%=rndB%>" class="form-select form-select-sm">
          <option value="semua"><%=Common.getBahasaConfig("Semua")%></option><option value="pending"><%=Common.getBahasaConfig("Menunggu")%></option><option value="disetujui"><%=Common.getBahasaConfig("Disetujui")%></option>
        </select></div>
      <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Dari")%></label><input type="date" id="bafTglM<%=rndB%>" class="form-control form-control-sm"></div>
      <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Sampai")%></label><input type="date" id="bafTglA<%=rndB%>" class="form-control form-control-sm"></div>
      <div class="col-6 col-md-3"><button class="btn btn-outline-primary btn-sm w-100" onclick="baLoad<%=rndB%>()"><i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Tampilkan")%></button></div>
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
    <tbody id="baBody<%=rndB%>"><tr><td colspan="10" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr></tbody>
  </table>
</div>

<div class="modal fade" id="baModal<%=rndB%>" tabindex="-1" aria-hidden="true">
 <div class="modal-dialog modal-xl modal-dialog-scrollable">
  <div class="modal-content rounded-4">
   <div class="modal-header"><h5 class="modal-title fw-bold" id="baTitle<%=rndB%>"><%=Common.getBahasaConfig("Penerimaan Barang/Jasa")%></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
   <div class="modal-body">
    <input type="hidden" id="baId<%=rndB%>"><input type="hidden" id="baPoId<%=rndB%>">
    <div class="row g-3">
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Kode")%></label><input type="text" id="baKode<%=rndB%>" class="form-control" placeholder="otomatis"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Kode Tagihan")%></label><input type="text" id="baKodeTagihan<%=rndB%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Tanggal Terima")%></label><input type="date" id="baTgl<%=rndB%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Tanggal Tagihan")%></label><input type="date" id="baTglTagihan<%=rndB%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Penyedia")%></label><select id="baPenyedia<%=rndB%>" class="form-select"></select></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Jenis Penerimaan")%></label><select id="baJenis<%=rndB%>" class="form-select"></select></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Satuan Kerja")%></label><select id="baSatker<%=rndB%>" class="form-select"></select></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Toko / Pedagang")%></label><select id="baToko<%=rndB%>" class="form-select"></select></div>
      <input type="hidden" id="baDisposisi<%=rndB%>"><%-- SOP dijalankan via menu "Pengajuan SOP", bukan dipilih di CRUD ini --%>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Pemilik Aset")%></label><select id="baPemilik<%=rndB%>" class="form-select"></select></div>
      <div class="col-12 col-md-3 position-relative"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Ruang")%></label>
        <input type="hidden" id="baRuangId<%=rndB%>"><input type="text" id="baRuang<%=rndB%>" class="form-control" placeholder="<%=Common.getBahasaConfig("cari ruang (opsional)...")%>" autocomplete="off" oninput="baCariRuang<%=rndB%>()">
        <div id="baRuangHasil<%=rndB%>" class="list-group position-absolute w-100 shadow" style="z-index:1095;max-height:220px;overflow:auto;display:none;"></div>
      </div>
      <div class="col-12"><div class="form-check"><input class="form-check-input" type="checkbox" id="baTanpaAnggaran<%=rndB%>"><label class="form-check-label small" for="baTanpaAnggaran<%=rndB%>"><%=Common.getBahasaConfig("Tanpa Anggaran")%></label></div></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Lokasi (Gudang)")%></label><select id="baLokasi<%=rndB%>" class="form-select"></select></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Kurir / Pengirim")%></label><input type="text" id="baKurir<%=rndB%>" class="form-control"></div>
      <div class="col-12 col-md-6"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Keterangan")%></label><input type="text" id="baKet<%=rndB%>" class="form-control"></div>
    </div>

    <hr class="my-3">
    <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 gap-2">
      <span class="fw-bold"><i class="fas fa-boxes me-2 text-primary"></i><%=Common.getBahasaConfig("Barang Diterima")%></span>
      <button class="btn btn-sm btn-outline-success rounded-pill" onclick="baBukaImporPo<%=rndB%>()"><i class="fas fa-file-import me-1"></i><%=Common.getBahasaConfig("Ambil dari Pesanan (PO)")%></button>
    </div>
    <div class="position-relative mb-2">
      <input type="text" id="baCari<%=rndB%>" class="form-control" placeholder="<%=Common.getBahasaConfig("Ketik nama/kode barang lalu pilih...")%>" autocomplete="off" oninput="baCariBarang<%=rndB%>()">
      <div id="baCariHasil<%=rndB%>" class="list-group position-absolute w-100 shadow" style="z-index:1090;max-height:240px;overflow:auto;display:none;"></div>
    </div>
    <div class="table-responsive">
      <table class="table table-sm align-middle">
        <thead class="table-light"><tr>
          <th><%=Common.getBahasaConfig("Barang")%></th><th style="width:80px" class="text-end"><%=Common.getBahasaConfig("Dipesan")%></th>
          <th style="width:90px" class="text-end"><%=Common.getBahasaConfig("Diterima")%></th><th style="width:120px" class="text-end"><%=Common.getBahasaConfig("Harga")%></th>
          <th style="width:100px" class="text-end"><%=Common.getBahasaConfig("Potongan")%></th><th style="width:40px" class="text-center" title="Potongan dalam %">%</th>
          <th style="width:70px" class="text-end"><%=Common.getBahasaConfig("PPN%")%></th><th style="width:70px" class="text-end"><%=Common.getBahasaConfig("PPh%")%></th>
          <th style="width:130px" class="text-end"><%=Common.getBahasaConfig("Subtotal")%></th><th style="width:36px"></th>
        </tr></thead>
        <tbody id="baLines<%=rndB%>"></tbody>
        <tfoot><tr class="table-light fw-bold"><td colspan="8" class="text-end"><%=Common.getBahasaConfig("Total Nilai")%></td><td class="text-end" id="baTotal<%=rndB%>">Rp 0</td><td></td></tr></tfoot>
      </table>
    </div>
   </div>
   <div class="modal-footer">
     <button type="button" class="btn btn-light rounded-pill" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
     <button type="button" class="btn btn-primary rounded-pill px-4" id="baBtnSimpan<%=rndB%>" onclick="baSimpan<%=rndB%>()"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan")%></button>
   </div>
  </div>
 </div>
</div>

<div class="modal fade" id="baPoModal<%=rndB%>" tabindex="-1" aria-hidden="true">
 <div class="modal-dialog modal-lg modal-dialog-scrollable"><div class="modal-content rounded-4">
   <div class="modal-header"><h6 class="modal-title fw-bold"><%=Common.getBahasaConfig("Pilih Pesanan (PO) yang Disetujui")%></h6><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
   <div class="modal-body"><div class="table-responsive"><table class="table table-hover align-middle">
     <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Satuan Kerja")%></th><th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th><th></th></tr></thead>
     <tbody id="baPoBody<%=rndB%>"></tbody></table></div></div>
 </div></div>
</div>

<script>
(function(){
  var SVC='<%=svcBAST%>', rnd='<%=rndB%>', comboLoaded=false, lockToko=false, tokoTerkunci='';
  function $(id){ return document.getElementById(id+rnd); }
  function rupiah(n){ n=Number(n)||0; return 'Rp '+n.toLocaleString('id-ID'); }
  function esc(s){ return (s==null?'':String(s)).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];}); }
  function num(v){ return Number(v)||0; }
  async function post(params){ var res=await fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},body:new URLSearchParams(params)}); return JSON.parse((await res.text()).trim()); }
  function opt(list,val){ var h='<option value="">- pilih -</option>'; (list||[]).forEach(function(x){ h+='<option value="'+x.id+'"'+(String(x.id)===String(val)?' selected':'')+'>'+esc(x.nama)+'</option>'; }); return h; }
  function lineTotal(dit,h,pot,pct,ppn,pph){ var dpp=dit*h; var potongan=pct?((pot/100)*dpp):pot; dpp=dpp-potongan; var vppn=(ppn/100)*dpp; var vpph=(pph/100)*dpp; return (dpp+vppn)-vpph; }

  window['baLoad'+rnd]=async function(){
    var tb=$('baBody'); tb.innerHTML='<tr><td colspan="10" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr>';
    try{ var r=await post({aksi:'list',kode:$('bafKode').value,status:$('bafStatus').value,tglMulai:$('bafTglM').value,tglAkhir:$('bafTglA').value});
      if(r.status!=='00'){ tb.innerHTML='<tr><td colspan="10" class="text-center text-danger py-4">'+esc(r.message||'Gagal')+'</td></tr>'; return; }
      if(!r.data||!r.data.length){ tb.innerHTML='<tr><td colspan="10" class="text-center text-muted py-4">Belum ada data.</td></tr>'; return; }
      var html='';
      r.data.forEach(function(p,i){
        var badge=p.status==='disetujui'?'<span class="badge bg-success">Disetujui</span>':'<span class="badge bg-warning text-dark">Menunggu</span>';
        var aksi='<button class="btn btn-sm btn-outline-secondary me-1" onclick="baEdit'+rnd+'('+p.id+')"><i class="fas fa-eye"></i></button>';
        if(p.status==='pending'){ aksi+='<button class="btn btn-sm btn-outline-success me-1" title="Setujui & catat stok" onclick="baSetujui'+rnd+'('+p.id+')"><i class="fas fa-check"></i></button><button class="btn btn-sm btn-outline-danger" onclick="baHapus'+rnd+'('+p.id+')"><i class="fas fa-trash"></i></button>'; }
        html+='<tr><td>'+(i+1)+'</td><td class="fw-semibold">'+esc(p.kode)+'</td><td>'+esc(p.kodeTagihan)+'</td><td>'+esc(p.tanggal)+'</td><td>'+esc(p.penyedia)+'</td><td>'+esc(p.toko||'')+'</td><td class="text-center">'+p.jumlahItem+'</td><td class="text-end">'+rupiah(p.nilai)+'</td><td class="text-center">'+badge+(p.sop?' <i class="fas fa-route text-info" title="Dijalankan via SOP"></i>':'')+'</td><td class="text-end">'+aksi+'</td></tr>';
      });
      tb.innerHTML=html;
    }catch(e){ tb.innerHTML='<tr><td colspan="10" class="text-center text-danger py-4">Error: '+esc(e.message)+'</td></tr>'; }
  };

  async function loadCombo(){ if(comboLoaded) return; var r=await post({aksi:'combo'}); if(r.status==='00'){
    $('baSatker').innerHTML=opt(r.satuanKerja); $('baLokasi').innerHTML=opt(r.lokasi); $('baPenyedia').innerHTML=opt(r.penyedia);
    $('baJenis').innerHTML=opt(r.jenisPenerimaan); $('baToko').innerHTML=opt(r.toko); $('baDisposisi').innerHTML=opt(r.disposisiSop); $('baPemilik').innerHTML=opt(r.pemilikAsset);
    lockToko=!!r.lockToko; tokoTerkunci=r.tokoTerkunci||''; comboLoaded=true; } }
  function modal(){ return bootstrap.Modal.getOrCreateInstance($('baModal')); }

  window['baBaru'+rnd]=async function(){ await loadCombo();
    $('baId').value=''; $('baPoId').value=''; $('baKode').value=''; $('baKodeTagihan').value=''; $('baTgl').value=new Date().toISOString().slice(0,10); $('baTglTagihan').value='';
    $('baPenyedia').value=''; $('baJenis').value=''; $('baSatker').value=''; $('baLokasi').value=''; $('baKurir').value=''; $('baKet').value='';
    $('baToko').value = lockToko ? tokoTerkunci : ''; $('baDisposisi').value=''; $('baPemilik').value=''; $('baRuang').value=''; $('baRuangId').value=''; $('baTanpaAnggaran').checked=false;
    $('baLines').innerHTML=''; recalc(); enableForm(true);
    document.getElementById('baTitle'+rnd).textContent='Buat Penerimaan'; modal().show();
  };

  window['baEdit'+rnd]=async function(id){ await loadCombo();
    var r=await post({aksi:'detail',id:id}); if(r.status!=='00'){ tampilkanPesanGagalFormal("pengelolaan Berita Acara Serah Terima (BAST) penerimaan barang kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); return; } var d=r.data;
    $('baId').value=d.id; $('baPoId').value=d.poId||''; $('baKode').value=d.kode||''; $('baKodeTagihan').value=d.kodeTagihan||''; $('baTgl').value=d.tanggal||''; $('baTglTagihan').value=d.tanggalTagihan||'';
    $('baPenyedia').value=d.penyedia||''; $('baJenis').value=d.jenisPenerimaan||''; $('baSatker').value=d.satuanKerja||''; $('baLokasi').value=d.lokasi||''; $('baKurir').value=d.kurir||''; $('baKet').value=d.keterangan||'';
    $('baToko').value = lockToko ? tokoTerkunci : (d.toko||''); $('baDisposisi').value=d.disposisiSop||''; $('baPemilik').value=d.pemilikAsset||''; $('baRuangId').value=d.ruang||''; $('baRuang').value=d.ruangNama||''; $('baTanpaAnggaran').checked=!!d.tanpaAnggaran;
    $('baLines').innerHTML=''; (d.lines||[]).forEach(function(l){ tambahBaris(l); }); recalc();
    var bisa=d.status==='pending'; enableForm(bisa);
    document.getElementById('baTitle'+rnd).textContent = bisa?'Ubah Penerimaan':'Detail Penerimaan (Disetujui)';
    modal().show();
  };

  function enableForm(on){
    $('baModal').querySelectorAll('.modal-body input,.modal-body select,.modal-body button').forEach(function(el){ if(el.id!=='baCari'+rnd) el.disabled=!on; });
    $('baCari').disabled=!on; if($('baToko')) $('baToko').disabled = !on || lockToko; $('baBtnSimpan').style.display=on?'':'none';
  }

  var cariTimer=null;
  window['baCariBarang'+rnd]=function(){ clearTimeout(cariTimer); cariTimer=setTimeout(async function(){
    var q=$('baCari').value.trim(), box=$('baCariHasil'); if(q.length<2){ box.style.display='none'; return; }
    var r=await post({aksi:'cariMasterAsset',q:q});
    if(r.status!=='00'||!r.data||!r.data.length){ box.innerHTML='<div class="list-group-item text-muted small">Tidak ada barang.</div>'; box.style.display='block'; return; }
    box.innerHTML=r.data.map(function(m){ return '<button type="button" class="list-group-item list-group-item-action" data-id="'+m.id+'" data-nama="'+esc(m.kode+' - '+m.nama)+'" data-harga="'+(m.harga||0)+'"><span class="fw-semibold">'+esc(m.nama)+'</span> <small class="text-muted">'+esc(m.kode)+'</small> <span class="float-end small">'+rupiah(m.harga)+'</span></button>'; }).join('');
    box.style.display='block';
    box.querySelectorAll('button').forEach(function(b){ b.onclick=function(){ tambahBaris({masterAsset:b.getAttribute('data-id'),masterAssetNama:b.getAttribute('data-nama'),jumlah:1,diterima:1,hargaBeli:Number(b.getAttribute('data-harga')),hargaPotongan:0,diskonPersen:false,persenPpn:0,persenPph:0,keterangan:''}); recalc(); box.style.display='none'; $('baCari').value=''; }; });
  },250); };
  document.addEventListener('click',function(e){ var box=$('baCariHasil'); if(box&&!box.contains(e.target)&&e.target!==$('baCari')) box.style.display='none'; });

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
  window['baCariRuang'+rnd]=function(){ pickerCari('baRuang','baRuangHasil','baRuangId','cariRuang'); };
  document.addEventListener('click',function(e){ var box=$('baRuangHasil'); if(box&&!box.contains(e.target)&&e.target!==$('baRuang')) box.style.display='none'; });

  function tambahBaris(l){
    var tr=document.createElement('tr');
    tr.setAttribute('data-id', l.masterAsset||''); tr.setAttribute('data-podetail', l.poDetailId||'');
    tr.innerHTML='<td>'+esc(l.masterAssetNama||'')+'<input type="hidden" class="bb-ket" value="'+esc(l.keterangan||'')+'"></td>'
      +'<td class="text-end bb-dipesan">'+num(l.jumlah||0)+'<input type="hidden" class="bb-jml" value="'+num(l.jumlah||0)+'"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-dit" value="'+num(l.diterima!=null?l.diterima:(l.jumlah||1))+'" oninput="baRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-hrg" value="'+num(l.hargaBeli||0)+'" oninput="baRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-pot" value="'+num(l.hargaPotongan||0)+'" oninput="baRecalc'+rnd+'()"></td>'
      +'<td class="text-center"><input type="checkbox" class="form-check-input bb-pct" '+(l.diskonPersen?'checked':'')+' onchange="baRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-ppn" value="'+num(l.persenPpn||0)+'" oninput="baRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-pph" value="'+num(l.persenPph||0)+'" oninput="baRecalc'+rnd+'()"></td>'
      +'<td class="text-end bb-sub">Rp 0</td>'
      +'<td class="text-center"><button class="btn btn-sm btn-outline-danger" onclick="this.closest(\'tr\').remove();baRecalc'+rnd+'()"><i class="fas fa-times"></i></button></td>';
    $('baLines').appendChild(tr);
  }
  function recalc(){ var total=0;
    $('baLines').querySelectorAll('tr').forEach(function(tr){
      var t=lineTotal(num(tr.querySelector('.bb-dit').value),num(tr.querySelector('.bb-hrg').value),num(tr.querySelector('.bb-pot').value),tr.querySelector('.bb-pct').checked,num(tr.querySelector('.bb-ppn').value),num(tr.querySelector('.bb-pph').value));
      tr.querySelector('.bb-sub').textContent=rupiah(t); total+=t; });
    $('baTotal').textContent=rupiah(total);
  }
  window['baRecalc'+rnd]=recalc;

  window['baBukaImporPo'+rnd]=async function(){
    var r=await post({aksi:'listPo'}); var tb=$('baPoBody');
    if(r.status!=='00'){ tampilkanPesanGagalFormal("pengelolaan Berita Acara Serah Terima (BAST) penerimaan barang kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); return; }
    if(!r.data||!r.data.length){ tb.innerHTML='<tr><td colspan="4" class="text-center text-muted py-3">Tidak ada PO disetujui.</td></tr>'; }
    else tb.innerHTML=r.data.map(function(p){ return '<tr><td class="fw-semibold">'+esc(p.kode)+'</td><td>'+esc(p.satuanKerja)+'</td><td class="text-end">'+rupiah(p.nilai)+'</td><td><button class="btn btn-sm btn-primary" onclick="baImporPo'+rnd+'('+p.id+')">Terima</button></td></tr>'; }).join('');
    bootstrap.Modal.getOrCreateInstance($('baPoModal')).show();
  };
  window['baImporPo'+rnd]=async function(poId){
    var r=await post({aksi:'importPo',id:poId}); if(r.status!=='00'){ tampilkanPesanGagalFormal("pengelolaan Berita Acara Serah Terima (BAST) penerimaan barang kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); return; } var d=r.data;
    $('baPoId').value=d.poId||''; if(d.satuanKerja) $('baSatker').value=d.satuanKerja; if(d.lokasi) $('baLokasi').value=d.lokasi;
    if(!lockToko && d.toko) $('baToko').value=d.toko; if(d.keterangan&&!$('baKet').value) $('baKet').value=d.keterangan;
    (d.lines||[]).forEach(function(l){ tambahBaris(l); }); recalc();
    bootstrap.Modal.getOrCreateInstance($('baPoModal')).hide();
  };

  window['baSimpan'+rnd]=async function(){
    var lines=[];
    $('baLines').querySelectorAll('tr').forEach(function(tr){ lines.push({ masterAsset:tr.getAttribute('data-id'), poDetailId:tr.getAttribute('data-podetail')||'', jumlah:num(tr.querySelector('.bb-jml').value), diterima:num(tr.querySelector('.bb-dit').value), hargaBeli:num(tr.querySelector('.bb-hrg').value), hargaPotongan:num(tr.querySelector('.bb-pot').value), diskonPersen:tr.querySelector('.bb-pct').checked, persenPpn:num(tr.querySelector('.bb-ppn').value), persenPph:num(tr.querySelector('.bb-pph').value), keterangan:tr.querySelector('.bb-ket').value||'' }); });
    if(!lines.length){ tampilkanPesanGagalFormal("penyimpanan Berita Acara Serah Terima (BAST)", '<%=Common.getBahasaConfigJS("Belum ada satu pun barang yang ditambahkan pada rincian penerimaan ini.")%>', ["Klik tombol cari/tambah barang untuk memasukkan minimal satu baris barang.", "Setelah rincian terisi, silakan simpan kembali."]); return; }
    var payload={ id:$('baId').value, poId:$('baPoId').value, kode:$('baKode').value, kodeTagihan:$('baKodeTagihan').value, tanggal:$('baTgl').value, tanggalTagihan:$('baTglTagihan').value, satuanKerja:$('baSatker').value, lokasi:$('baLokasi').value, toko:$('baToko').value, disposisiSop:$('baDisposisi').value, pemilikAsset:$('baPemilik').value, ruang:$('baRuangId').value, tanpaAnggaran:$('baTanpaAnggaran').checked, penyedia:$('baPenyedia').value, jenisPenerimaan:$('baJenis').value, kurir:$('baKurir').value, keterangan:$('baKet').value, lines:lines };
    var btn=$('baBtnSimpan'); btn.disabled=true; var ori=btn.innerHTML; btn.innerHTML='<span class="spinner-border spinner-border-sm me-2"></span>Menyimpan...';
    try{ var r=await post({aksi:'simpan',data:JSON.stringify(payload)}); if(r.status==='00'){ modal().hide(); window['baLoad'+rnd](); } else tampilkanPesanGagalFormal("pengelolaan Berita Acara Serah Terima (BAST) penerimaan barang kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); }
    catch(e){ tampilkanPesanGagalFormal("penyimpanan Berita Acara Serah Terima (BAST)", "Rincian teknis: "+e.message, ["Periksa koneksi internet Bapak/Ibu.", "Ulangi proses penyimpanan beberapa saat lagi."]); } finally{ btn.disabled=false; btn.innerHTML=ori; }
  };
  window['baSetujui'+rnd]=async function(id){ if(!confirm('<%= Common.getBahasaConfigJS("Apakah Bapak/Ibu yakin ingin menyetujui penerimaan ini? Stok barang akan otomatis bertambah.") %>'))return; var r=await post({aksi:'setujui',id:id}); if(r.status==='00'){ tampilkanPesanSuksesFormal("persetujuan penerimaan barang (BAST)", r.message||''); window['baLoad'+rnd](); } else tampilkanPesanGagalFormal("pengelolaan Berita Acara Serah Terima (BAST) penerimaan barang kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); };
  window['baHapus'+rnd]=async function(id){ if(!confirm('<%= Common.getBahasaConfigJS("Apakah Bapak/Ibu yakin ingin menghapus penerimaan ini?") %>'))return; var r=await post({aksi:'hapus',id:id}); if(r.status==='00') window['baLoad'+rnd](); else tampilkanPesanGagalFormal("pengelolaan Berita Acara Serah Terima (BAST) penerimaan barang kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); };

  window['baLoad'+rnd]();
})();
</script>
