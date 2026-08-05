<%@page import="ais.common.Common"%>
<%
String rndS = Common.getGeneratedBarCode(6);
String svcPKS = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fkulakan&s=kerjasama_asset_service";
%>

<div class="card border-0 shadow-sm rounded-4 mb-3">
  <div class="card-body">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-1">
      <div>
        <h6 class="fw-bold mb-0"><i class="fas fa-handshake me-2 text-primary"></i><%=Common.getBahasaConfig("Perjanjian Kerjasama (PKS)")%></h6>
        <small class="text-muted"><%=Common.getBahasaConfig("Kontrak/kesepakatan dengan pemasok untuk pengadaan barang atau jasa.")%></small>
      </div>
      <button class="btn btn-primary rounded-pill px-3" onclick="pkBaru<%=rndS%>()"><i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Buat PKS")%></button>
    </div>
    <div class="row g-2 align-items-end mt-1">
      <div class="col-12 col-md-3"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Kode")%></label><input type="text" id="pkfKode<%=rndS%>" class="form-control form-control-sm"></div>
      <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Status")%></label>
        <select id="pkfStatus<%=rndS%>" class="form-select form-select-sm">
          <option value="semua"><%=Common.getBahasaConfig("Semua")%></option><option value="pending"><%=Common.getBahasaConfig("Menunggu")%></option><option value="disetujui"><%=Common.getBahasaConfig("Disetujui")%></option>
        </select></div>
      <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Dari")%></label><input type="date" id="pkfTglM<%=rndS%>" class="form-control form-control-sm"></div>
      <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Sampai")%></label><input type="date" id="pkfTglA<%=rndS%>" class="form-control form-control-sm"></div>
      <div class="col-6 col-md-3"><button class="btn btn-outline-primary btn-sm w-100" onclick="pkLoad<%=rndS%>()"><i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Tampilkan")%></button></div>
    </div>
  </div>
</div>

<div class="table-responsive">
  <table class="table table-hover align-middle">
    <thead class="table-light"><tr>
      <th>#</th><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("No. PKS")%></th><th><%=Common.getBahasaConfig("Tanggal")%></th>
      <th><%=Common.getBahasaConfig("Penyedia")%></th><th><%=Common.getBahasaConfig("Toko")%></th><th class="text-center"><%=Common.getBahasaConfig("Item")%></th>
      <th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th><th class="text-center"><%=Common.getBahasaConfig("Status")%></th><th class="text-end"><%=Common.getBahasaConfig("Aksi")%></th>
    </tr></thead>
    <tbody id="pkBody<%=rndS%>"><tr><td colspan="10" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr></tbody>
  </table>
</div>

<div class="modal fade" id="pkModal<%=rndS%>" tabindex="-1" aria-hidden="true">
 <div class="modal-dialog modal-xl modal-dialog-scrollable">
  <div class="modal-content rounded-4">
   <div class="modal-header"><h5 class="modal-title fw-bold" id="pkTitle<%=rndS%>"><%=Common.getBahasaConfig("Perjanjian Kerjasama")%></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
   <div class="modal-body">
    <input type="hidden" id="pkId<%=rndS%>">
    <div class="row g-3">
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Kode")%></label><input type="text" id="pkKode<%=rndS%>" class="form-control" placeholder="otomatis"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("No. Perjanjian")%></label><input type="text" id="pkNomor<%=rndS%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("No. Invoice")%></label><input type="text" id="pkInvoice<%=rndS%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Tanggal")%></label><input type="date" id="pkTgl<%=rndS%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Penyedia")%></label><select id="pkPenyedia<%=rndS%>" class="form-select"></select></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Jenis Kerjasama")%></label><select id="pkJenis<%=rndS%>" class="form-select"></select></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Satuan Kerja")%></label><select id="pkSatker<%=rndS%>" class="form-select"></select></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Toko / Pedagang")%></label><select id="pkToko<%=rndS%>" class="form-select"></select></div>
      <input type="hidden" id="pkDisposisi<%=rndS%>"><%-- SOP dijalankan via menu "Pengajuan SOP", bukan dipilih di CRUD ini --%>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Lokasi")%></label><select id="pkLokasi<%=rndS%>" class="form-select"></select></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Mulai Berlaku")%></label><input type="date" id="pkMulai<%=rndS%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Berakhir / Kirim Plg Lambat")%></label><input type="date" id="pkAkhir<%=rndS%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Uang Muka (DP)")%></label><input type="number" step="any" id="pkDp<%=rndS%>" class="form-control" value="0"></div>
      <div class="col-12 col-md-8"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Keterangan")%></label><input type="text" id="pkKet<%=rndS%>" class="form-control"></div>
      <div class="col-12 col-md-4"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Catatan Kesepakatan")%></label><input type="text" id="pkCatatan<%=rndS%>" class="form-control"></div>
      <div class="col-12 d-flex flex-wrap gap-3 align-items-center">
        <div class="form-check"><input class="form-check-input" type="checkbox" id="pkPpn<%=rndS%>" onchange="pkRecalc<%=rndS%>()"><label class="form-check-label small" for="pkPpn<%=rndS%>"><%=Common.getBahasaConfig("Kena PPN")%></label></div>
        <div class="d-flex align-items-center gap-1"><label class="form-label small mb-0"><%=Common.getBahasaConfig("PPN %")%></label><input type="number" step="any" id="pkPersenPpn<%=rndS%>" class="form-control form-control-sm" style="width:90px" value="0" oninput="pkRecalc<%=rndS%>()"></div>
      </div>
    </div>

    <hr class="my-3">
    <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 gap-2">
      <span class="fw-bold"><i class="fas fa-boxes me-2 text-primary"></i><%=Common.getBahasaConfig("Daftar Barang/Jasa")%></span>
      <button class="btn btn-sm btn-outline-success rounded-pill" onclick="pkBukaImporPr<%=rndS%>()"><i class="fas fa-file-import me-1"></i><%=Common.getBahasaConfig("Ambil dari Permintaan (PR)")%></button>
    </div>
    <div class="position-relative mb-2">
      <input type="text" id="pkCari<%=rndS%>" class="form-control" placeholder="<%=Common.getBahasaConfig("Ketik nama/kode barang lalu pilih...")%>" autocomplete="off" oninput="pkCariBarang<%=rndS%>()">
      <div id="pkCariHasil<%=rndS%>" class="list-group position-absolute w-100 shadow" style="z-index:1090;max-height:240px;overflow:auto;display:none;"></div>
    </div>
    <div class="table-responsive">
      <table class="table table-sm align-middle">
        <thead class="table-light"><tr>
          <th><%=Common.getBahasaConfig("Barang/Jasa")%></th><th style="width:110px" class="text-end"><%=Common.getBahasaConfig("Jumlah")%></th>
          <th style="width:150px" class="text-end"><%=Common.getBahasaConfig("Harga")%></th><th style="width:140px" class="text-end"><%=Common.getBahasaConfig("Potongan")%></th>
          <th style="width:160px" class="text-end"><%=Common.getBahasaConfig("Subtotal")%></th><th style="width:40px"></th>
        </tr></thead>
        <tbody id="pkLines<%=rndS%>"></tbody>
        <tfoot>
          <tr class="fw-semibold"><td colspan="4" class="text-end"><%=Common.getBahasaConfig("Subtotal")%></td><td class="text-end" id="pkSub<%=rndS%>">Rp 0</td><td></td></tr>
          <tr class="table-light fw-bold"><td colspan="4" class="text-end"><%=Common.getBahasaConfig("Total (termasuk PPN)")%></td><td class="text-end" id="pkTotal<%=rndS%>">Rp 0</td><td></td></tr>
        </tfoot>
      </table>
    </div>

    <div id="pkTerminWrap<%=rndS%>" class="mt-2">
      <div class="d-flex justify-content-between align-items-center mb-2">
        <span class="fw-bold"><i class="fas fa-list-ol me-2 text-primary"></i><%=Common.getBahasaConfig("Termin Pembayaran")%> <small class="text-muted">(<%=Common.getBahasaConfig("opsional")%>)</small></span>
        <button class="btn btn-sm btn-outline-primary rounded-pill" onclick="pkTambahTermin<%=rndS%>()"><i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Termin")%></button>
      </div>
      <div class="table-responsive"><table class="table table-sm align-middle">
        <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Nama Termin")%></th><th style="width:160px" class="text-end"><%=Common.getBahasaConfig("Penagihan")%></th><th style="width:150px"><%=Common.getBahasaConfig("Tanggal")%></th><th style="width:40px"></th></tr></thead>
        <tbody id="pkTermin<%=rndS%>"></tbody>
      </table></div>
    </div>
   </div>
   <div class="modal-footer">
     <button type="button" class="btn btn-light rounded-pill" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
     <button type="button" class="btn btn-primary rounded-pill px-4" id="pkBtnSimpan<%=rndS%>" onclick="pkSimpan<%=rndS%>()"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan")%></button>
   </div>
  </div>
 </div>
</div>

<div class="modal fade" id="pkPrModal<%=rndS%>" tabindex="-1" aria-hidden="true">
 <div class="modal-dialog modal-lg modal-dialog-scrollable"><div class="modal-content rounded-4">
   <div class="modal-header"><h6 class="modal-title fw-bold"><%=Common.getBahasaConfig("Pilih Permintaan (PR) yang Disetujui")%></h6><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
   <div class="modal-body"><div class="table-responsive"><table class="table table-hover align-middle">
     <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Satuan Kerja")%></th><th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th><th></th></tr></thead>
     <tbody id="pkPrBody<%=rndS%>"></tbody></table></div></div>
 </div></div>
</div>

<script>
(function(){
  var SVC='<%=svcPKS%>', rnd='<%=rndS%>', comboLoaded=false, lockToko=false, tokoTerkunci='';
  function $(id){ return document.getElementById(id+rnd); }
  function rupiah(n){ n=Number(n)||0; return 'Rp '+n.toLocaleString('id-ID'); }
  function esc(s){ return (s==null?'':String(s)).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];}); }
  function num(v){ return Number(v)||0; }
  async function post(params){ var res=await fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},body:new URLSearchParams(params)}); return JSON.parse((await res.text()).trim()); }
  function opt(list,val){ var h='<option value="">- pilih -</option>'; (list||[]).forEach(function(x){ h+='<option value="'+x.id+'"'+(String(x.id)===String(val)?' selected':'')+'>'+esc(x.nama)+'</option>'; }); return h; }

  window['pkLoad'+rnd]=async function(){
    var tb=$('pkBody'); tb.innerHTML='<tr><td colspan="10" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr>';
    try{ var r=await post({aksi:'list',kode:$('pkfKode').value,status:$('pkfStatus').value,tglMulai:$('pkfTglM').value,tglAkhir:$('pkfTglA').value});
      if(r.status!=='00'){ tb.innerHTML='<tr><td colspan="10" class="text-center text-danger py-4">'+esc(r.message||'Gagal')+'</td></tr>'; return; }
      if(!r.data||!r.data.length){ tb.innerHTML='<tr><td colspan="10" class="text-center text-muted py-4">Belum ada data.</td></tr>'; return; }
      var html='';
      r.data.forEach(function(p,i){
        var badge=p.status==='disetujui'?'<span class="badge bg-success">Disetujui</span>':'<span class="badge bg-warning text-dark">Menunggu</span>';
        var aksi='<button class="btn btn-sm btn-outline-secondary me-1" onclick="pkEdit'+rnd+'('+p.id+')"><i class="fas fa-eye"></i></button>';
        if(p.status==='pending'){ aksi+='<button class="btn btn-sm btn-outline-success me-1" onclick="pkSetujui'+rnd+'('+p.id+')"><i class="fas fa-check"></i></button><button class="btn btn-sm btn-outline-danger" onclick="pkHapus'+rnd+'('+p.id+')"><i class="fas fa-trash"></i></button>'; }
        html+='<tr><td>'+(i+1)+'</td><td class="fw-semibold">'+esc(p.kode)+'</td><td>'+esc(p.nomor)+'</td><td>'+esc(p.tanggal)+'</td><td>'+esc(p.penyedia)+'</td><td>'+esc(p.toko||'')+'</td><td class="text-center">'+p.jumlahItem+'</td><td class="text-end">'+rupiah(p.nilai)+'</td><td class="text-center">'+badge+(p.sop?' <i class="fas fa-route text-info" title="Dijalankan via SOP"></i>':'')+'</td><td class="text-end">'+aksi+'</td></tr>';
      });
      tb.innerHTML=html;
    }catch(e){ tb.innerHTML='<tr><td colspan="10" class="text-center text-danger py-4">Error: '+esc(e.message)+'</td></tr>'; }
  };

  async function loadCombo(){ if(comboLoaded) return; var r=await post({aksi:'combo'}); if(r.status==='00'){
    $('pkSatker').innerHTML=opt(r.satuanKerja); $('pkLokasi').innerHTML=opt(r.lokasi); $('pkPenyedia').innerHTML=opt(r.penyedia);
    $('pkJenis').innerHTML=opt(r.jenisPerjanjian); $('pkToko').innerHTML=opt(r.toko); $('pkDisposisi').innerHTML=opt(r.disposisiSop);
    lockToko=!!r.lockToko; tokoTerkunci=r.tokoTerkunci||''; comboLoaded=true; } }
  function modal(){ return bootstrap.Modal.getOrCreateInstance($('pkModal')); }

  window['pkBaru'+rnd]=async function(){ await loadCombo();
    $('pkId').value=''; $('pkKode').value=''; $('pkNomor').value=''; $('pkInvoice').value=''; $('pkTgl').value=new Date().toISOString().slice(0,10);
    $('pkPenyedia').value=''; $('pkJenis').value=''; $('pkSatker').value=''; $('pkLokasi').value=''; $('pkMulai').value=''; $('pkAkhir').value='';
    $('pkDp').value='0'; $('pkKet').value=''; $('pkCatatan').value=''; $('pkPpn').checked=false; $('pkPersenPpn').value='0';
    $('pkToko').value = lockToko ? tokoTerkunci : ''; $('pkDisposisi').value='';
    $('pkLines').innerHTML=''; $('pkTermin').innerHTML=''; recalc(); enableForm(true);
    document.getElementById('pkTitle'+rnd).textContent='Buat PKS'; modal().show();
  };

  window['pkEdit'+rnd]=async function(id){ await loadCombo();
    var r=await post({aksi:'detail',id:id}); if(r.status!=='00'){ tampilkanPesanGagalFormal("pengelolaan data Perjanjian Kerjasama (PKS) kantin", r.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); return; } var d=r.data;
    $('pkId').value=d.id; $('pkKode').value=d.kode||''; $('pkNomor').value=d.nomor||''; $('pkInvoice').value=d.kodeInvoice||''; $('pkTgl').value=d.tanggal||'';
    $('pkPenyedia').value=d.penyedia||''; $('pkJenis').value=d.jenisPerjanjian||''; $('pkSatker').value=d.satuanKerja||''; $('pkLokasi').value=d.lokasi||'';
    $('pkMulai').value=d.mulai||''; $('pkAkhir').value=d.pengiriman||''; $('pkDp').value=d.dp||0; $('pkKet').value=d.keterangan||''; $('pkCatatan').value=d.catatanKesepakatan||'';
    $('pkPpn').checked=!!d.ppn; $('pkPersenPpn').value=d.persenPpn||0; $('pkToko').value = lockToko ? tokoTerkunci : (d.toko||''); $('pkDisposisi').value=d.disposisiSop||'';
    $('pkLines').innerHTML=''; (d.lines||[]).forEach(function(l){ tambahBaris(l); });
    $('pkTermin').innerHTML=''; try{ JSON.parse(d.formula||'[]').forEach(function(t){ tambahTermin(t); }); }catch(e){}
    recalc();
    var bisa=d.status==='pending'; enableForm(bisa);
    document.getElementById('pkTitle'+rnd).textContent = bisa?'Ubah PKS':'Detail PKS (Disetujui)';
    modal().show();
  };

  function enableForm(on){
    $('pkModal').querySelectorAll('.modal-body input,.modal-body select,.modal-body button').forEach(function(el){ if(el.id!=='pkCari'+rnd) el.disabled=!on; });
    $('pkCari').disabled=!on; if($('pkToko')) $('pkToko').disabled = !on || lockToko; $('pkBtnSimpan').style.display=on?'':'none';
  }

  var cariTimer=null;
  window['pkCariBarang'+rnd]=function(){ clearTimeout(cariTimer); cariTimer=setTimeout(async function(){
    var q=$('pkCari').value.trim(), box=$('pkCariHasil'); if(q.length<2){ box.style.display='none'; return; }
    var r=await post({aksi:'cariMasterAsset',q:q});
    if(r.status!=='00'||!r.data||!r.data.length){ box.innerHTML='<div class="list-group-item text-muted small">Tidak ada barang.</div>'; box.style.display='block'; return; }
    box.innerHTML=r.data.map(function(m){ return '<button type="button" class="list-group-item list-group-item-action" data-id="'+m.id+'" data-nama="'+esc(m.kode+' - '+m.nama)+'" data-harga="'+(m.harga||0)+'"><span class="fw-semibold">'+esc(m.nama)+'</span> <small class="text-muted">'+esc(m.kode)+'</small> <span class="float-end small">'+rupiah(m.harga)+'</span></button>'; }).join('');
    box.style.display='block';
    box.querySelectorAll('button').forEach(function(b){ b.onclick=function(){ tambahBaris({masterAsset:b.getAttribute('data-id'),masterAssetNama:b.getAttribute('data-nama'),jumlah:1,hargaBeli:Number(b.getAttribute('data-harga')),hargaPotongan:0,keterangan:''}); recalc(); box.style.display='none'; $('pkCari').value=''; }; });
  },250); };
  document.addEventListener('click',function(e){ var box=$('pkCariHasil'); if(box&&!box.contains(e.target)&&e.target!==$('pkCari')) box.style.display='none'; });

  function tambahBaris(l){
    var tr=document.createElement('tr');
    tr.setAttribute('data-id', l.masterAsset||''); tr.setAttribute('data-prdetail', l.prDetailId||'');
    tr.innerHTML='<td>'+esc(l.masterAssetNama||'')+'<input type="hidden" class="bb-ket" value="'+esc(l.keterangan||'')+'"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-jml" value="'+num(l.jumlah||1)+'" oninput="pkRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-hrg" value="'+num(l.hargaBeli||0)+'" oninput="pkRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-pot" value="'+num(l.hargaPotongan||0)+'" oninput="pkRecalc'+rnd+'()"></td>'
      +'<td class="text-end bb-sub">Rp 0</td>'
      +'<td class="text-center"><button class="btn btn-sm btn-outline-danger" onclick="this.closest(\'tr\').remove();pkRecalc'+rnd+'()"><i class="fas fa-times"></i></button></td>';
    $('pkLines').appendChild(tr);
  }
  function recalc(){ var sub=0;
    $('pkLines').querySelectorAll('tr').forEach(function(tr){
      var t=num(tr.querySelector('.bb-jml').value)*num(tr.querySelector('.bb-hrg').value)-num(tr.querySelector('.bb-pot').value);
      tr.querySelector('.bb-sub').textContent=rupiah(t); sub+=t; });
    $('pkSub').textContent=rupiah(sub);
    var total=sub; if($('pkPpn').checked) total += sub*(num($('pkPersenPpn').value)/100);
    $('pkTotal').textContent=rupiah(total);
  }
  window['pkRecalc'+rnd]=recalc;

  function tambahTermin(t){ t=t||{};
    var tr=document.createElement('tr');
    tr.innerHTML='<td><input type="text" class="form-control form-control-sm tm-nama" value="'+esc(t.nama||'')+'"></td>'
      +'<td><input type="number" step="any" class="form-control form-control-sm text-end tm-tagih" value="'+num(t.penagihan||0)+'"></td>'
      +'<td><input type="date" class="form-control form-control-sm tm-tgl" value="'+esc(t.tanggalISO||'')+'"></td>'
      +'<td class="text-center"><button class="btn btn-sm btn-outline-danger" onclick="this.closest(\'tr\').remove()"><i class="fas fa-times"></i></button></td>';
    $('pkTermin').appendChild(tr);
  }
  window['pkTambahTermin'+rnd]=function(){ tambahTermin({}); };

  window['pkBukaImporPr'+rnd]=async function(){
    var r=await post({aksi:'listPr'}); var tb=$('pkPrBody');
    if(r.status!=='00'){ tampilkanPesanGagalFormal("pengelolaan data Perjanjian Kerjasama (PKS) kantin", r.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); return; }
    if(!r.data||!r.data.length){ tb.innerHTML='<tr><td colspan="4" class="text-center text-muted py-3">Tidak ada PR disetujui yang tersedia.</td></tr>'; }
    else tb.innerHTML=r.data.map(function(p){ return '<tr><td class="fw-semibold">'+esc(p.kode)+'</td><td>'+esc(p.satuanKerja)+'</td><td class="text-end">'+rupiah(p.nilai)+'</td><td><button class="btn btn-sm btn-primary" onclick="pkImporPr'+rnd+'('+p.id+')">Ambil</button></td></tr>'; }).join('');
    bootstrap.Modal.getOrCreateInstance($('pkPrModal')).show();
  };
  window['pkImporPr'+rnd]=async function(prId){
    var r=await post({aksi:'importPr',id:prId}); if(r.status!=='00'){ tampilkanPesanGagalFormal("pengelolaan data Perjanjian Kerjasama (PKS) kantin", r.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); return; } var d=r.data;
    if(d.satuanKerja) $('pkSatker').value=d.satuanKerja; if(d.lokasi) $('pkLokasi').value=d.lokasi; if(d.keterangan&&!$('pkKet').value) $('pkKet').value=d.keterangan;
    (d.lines||[]).forEach(function(l){ l.hargaPotongan=0; tambahBaris(l); });
    recalc(); bootstrap.Modal.getOrCreateInstance($('pkPrModal')).hide();
  };

  window['pkSimpan'+rnd]=async function(){
    var lines=[];
    $('pkLines').querySelectorAll('tr').forEach(function(tr){ lines.push({ masterAsset:tr.getAttribute('data-id'), prDetailId:tr.getAttribute('data-prdetail')||'', jumlah:num(tr.querySelector('.bb-jml').value), hargaBeli:num(tr.querySelector('.bb-hrg').value), hargaPotongan:num(tr.querySelector('.bb-pot').value), keterangan:tr.querySelector('.bb-ket').value||'' }); });
    if(!lines.length){ tampilkanPesanGagalFormal("penyimpanan Perjanjian Kerjasama (PKS)", '<%=Common.getBahasaConfigJS("Belum ada satu pun barang atau jasa yang ditambahkan pada rincian PKS ini.")%>', ["Klik tombol cari/tambah barang untuk memasukkan minimal satu baris barang atau jasa.", "Setelah rincian terisi, silakan simpan kembali."]); return; }
    var termin=[];
    $('pkTermin').querySelectorAll('tr').forEach(function(tr,i){ termin.push({ key:'t'+(i+1), nama:tr.querySelector('.tm-nama').value, penagihan:num(tr.querySelector('.tm-tagih').value), tanggalISO:tr.querySelector('.tm-tgl').value }); });
    var payload={ id:$('pkId').value, kode:$('pkKode').value, nomor:$('pkNomor').value, kodeInvoice:$('pkInvoice').value, tanggal:$('pkTgl').value, mulai:$('pkMulai').value, pengiriman:$('pkAkhir').value, satuanKerja:$('pkSatker').value, lokasi:$('pkLokasi').value, toko:$('pkToko').value, disposisiSop:$('pkDisposisi').value, penyedia:$('pkPenyedia').value, jenisPerjanjian:$('pkJenis').value, ppn:$('pkPpn').checked, persenPpn:num($('pkPersenPpn').value), dp:num($('pkDp').value), keterangan:$('pkKet').value, catatanKesepakatan:$('pkCatatan').value, formula:JSON.stringify(termin), lines:lines };
    var btn=$('pkBtnSimpan'); btn.disabled=true; var ori=btn.innerHTML; btn.innerHTML='<span class="spinner-border spinner-border-sm me-2"></span>Menyimpan...';
    try{ var r=await post({aksi:'simpan',data:JSON.stringify(payload)}); if(r.status==='00'){ modal().hide(); window['pkLoad'+rnd](); } else tampilkanPesanGagalFormal("pengelolaan data Perjanjian Kerjasama (PKS) kantin", r.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); }
    catch(e){ tampilkanPesanGagalFormal("penyimpanan Perjanjian Kerjasama (PKS)", "Rincian teknis: "+e.message, ["Periksa koneksi internet Bapak/Ibu.", "Ulangi proses penyimpanan beberapa saat lagi."]); } finally{ btn.disabled=false; btn.innerHTML=ori; }
  };
  window['pkSetujui'+rnd]=async function(id){ if(!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menyetujui PKS ini?")%>'))return; var r=await post({aksi:'setujui',id:id}); if(r.status==='00') window['pkLoad'+rnd](); else tampilkanPesanGagalFormal("pengelolaan data Perjanjian Kerjasama (PKS) kantin", r.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); };
  window['pkHapus'+rnd]=async function(id){ if(!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus PKS ini?")%>'))return; var r=await post({aksi:'hapus',id:id}); if(r.status==='00') window['pkLoad'+rnd](); else tampilkanPesanGagalFormal("pengelolaan data Perjanjian Kerjasama (PKS) kantin", r.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); };

  window['pkLoad'+rnd]();
})();
</script>
