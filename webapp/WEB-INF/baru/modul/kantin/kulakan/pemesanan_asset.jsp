<%@page import="ais.common.Common"%>
<%
String rndO = Common.getGeneratedBarCode(6);
String svcPO = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fkulakan&s=pemesanan_asset_service";
%>

<div class="card border-0 shadow-sm rounded-4 mb-3">
  <div class="card-body">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-1">
      <div>
        <h6 class="fw-bold mb-0"><i class="fas fa-file-invoice me-2 text-primary"></i><%=Common.getBahasaConfig("Pemesanan / Pembelian (PO)")%></h6>
        <small class="text-muted"><%=Common.getBahasaConfig("Buat order ke pemasok dari permintaan yang sudah disetujui, lengkap dengan harga, diskon, dan pajak.")%></small>
      </div>
      <button class="btn btn-primary rounded-pill px-3" onclick="poBaru<%=rndO%>()"><i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Buat Pemesanan")%></button>
    </div>
    <div class="row g-2 align-items-end mt-1">
      <div class="col-12 col-md-3"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Kode")%></label><input type="text" id="pofKode<%=rndO%>" class="form-control form-control-sm"></div>
      <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Status")%></label>
        <select id="pofStatus<%=rndO%>" class="form-select form-select-sm">
          <option value="semua"><%=Common.getBahasaConfig("Semua")%></option><option value="pending"><%=Common.getBahasaConfig("Menunggu")%></option>
          <option value="disetujui"><%=Common.getBahasaConfig("Disetujui")%></option><option value="ditolak"><%=Common.getBahasaConfig("Ditolak")%></option>
        </select></div>
      <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Dari")%></label><input type="date" id="pofTglM<%=rndO%>" class="form-control form-control-sm"></div>
      <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Sampai")%></label><input type="date" id="pofTglA<%=rndO%>" class="form-control form-control-sm"></div>
      <div class="col-6 col-md-3"><button class="btn btn-outline-primary btn-sm w-100" onclick="poLoad<%=rndO%>()"><i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Tampilkan")%></button></div>
    </div>
  </div>
</div>

<div class="table-responsive">
  <table class="table table-hover align-middle">
    <thead class="table-light"><tr>
      <th>#</th><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Invoice")%></th><th><%=Common.getBahasaConfig("Tanggal")%></th>
      <th><%=Common.getBahasaConfig("Satuan Kerja")%></th><th><%=Common.getBahasaConfig("Toko")%></th><th class="text-center"><%=Common.getBahasaConfig("Item")%></th>
      <th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th><th class="text-center"><%=Common.getBahasaConfig("Status")%></th><th class="text-end"><%=Common.getBahasaConfig("Aksi")%></th>
    </tr></thead>
    <tbody id="poBody<%=rndO%>"><tr><td colspan="10" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr></tbody>
  </table>
</div>

<div class="modal fade" id="poModal<%=rndO%>" tabindex="-1" aria-hidden="true">
 <div class="modal-dialog modal-xl modal-dialog-scrollable">
  <div class="modal-content rounded-4">
   <div class="modal-header"><h5 class="modal-title fw-bold" id="poTitle<%=rndO%>"><%=Common.getBahasaConfig("Pemesanan")%></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
   <div class="modal-body">
    <input type="hidden" id="poId<%=rndO%>">
    <div class="row g-3">
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Kode")%></label><input type="text" id="poKode<%=rndO%>" class="form-control" placeholder="otomatis"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("No. Invoice")%></label><input type="text" id="poInvoice<%=rndO%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Tanggal")%></label><input type="date" id="poTgl<%=rndO%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Pengiriman Plg Lambat")%></label><input type="date" id="poKirim<%=rndO%>" class="form-control"></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Satuan Kerja")%></label><select id="poSatker<%=rndO%>" class="form-select"></select></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Lokasi")%></label><select id="poLokasi<%=rndO%>" class="form-select"></select></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Toko / Pedagang")%></label><select id="poToko<%=rndO%>" class="form-select"></select></div>
      <input type="hidden" id="poDisposisi<%=rndO%>"><%-- SOP dijalankan via menu "Pengajuan SOP", bukan dipilih di CRUD ini --%>
      <div class="col-12 col-md-6 position-relative" id="poWorkspaceWrap<%=rndO%>"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Anggaran")%></label>
        <input type="hidden" id="poWorkspaceId<%=rndO%>"><input type="text" id="poWorkspace<%=rndO%>" class="form-control" placeholder="<%=Common.getBahasaConfig("cari anggaran (opsional)...")%>" autocomplete="off" oninput="poCariWorkspace<%=rndO%>()">
        <div id="poWorkspaceHasil<%=rndO%>" class="list-group position-absolute w-100 shadow" style="z-index:1095;max-height:220px;overflow:auto;display:none;"></div>
      </div>
      <div class="col-12" id="poAnggaranInfo<%=rndO%>" style="display:none">
        <div class="border rounded-3 p-2 bg-light small"><div class="row g-2">
          <div class="col-6 col-md-3"><span class="text-muted d-block"><%=Common.getBahasaConfig("Unit")%></span><span class="fw-semibold" id="poAiUnit<%=rndO%>">-</span></div>
          <div class="col-6 col-md-3"><span class="text-muted d-block"><%=Common.getBahasaConfig("Nilai Anggaran")%></span><span class="fw-semibold" id="poAiNilai<%=rndO%>">-</span></div>
          <div class="col-6 col-md-3"><span class="text-muted d-block"><%=Common.getBahasaConfig("Dalam Proses")%></span><span class="fw-semibold text-warning" id="poAiProses<%=rndO%>">-</span></div>
          <div class="col-6 col-md-3"><span class="text-muted d-block"><%=Common.getBahasaConfig("Sisa Anggaran")%></span><span class="fw-bold text-success" id="poAiSisa<%=rndO%>">-</span></div>
          <div class="col-12 col-md-6"><span class="text-muted d-block"><%=Common.getBahasaConfig("Periode")%></span><span id="poAiPeriode<%=rndO%>">-</span></div>
          <div class="col-12 col-md-6"><span class="text-muted d-block"><%=Common.getBahasaConfig("Akun Anggaran")%></span><span id="poAiAkun<%=rndO%>">-</span></div>
        </div></div>
      </div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Penyedia / Perjanjian")%></label><select id="poPerjanjian<%=rndO%>" class="form-select"></select></div>
      <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Jenis Pemesanan")%></label><select id="poJenis<%=rndO%>" class="form-select"></select></div>
      <div class="col-12 col-md-8"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Keterangan")%></label><input type="text" id="poKet<%=rndO%>" class="form-control"></div>
      <div class="col-12 col-md-4"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Catatan Kesepakatan")%></label><input type="text" id="poCatatan<%=rndO%>" class="form-control"></div>
    </div>

    <div class="row g-3 mt-1 p-2 bg-light rounded-3">
      <div class="col-6 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Uang Muka (DP)")%></label><input type="number" step="any" id="poDp<%=rndO%>" class="form-control" value="0"></div>
      <div class="col-6 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("PPN atas DP")%></label><select id="poPpnDp<%=rndO%>" class="form-select"></select></div>
      <div class="col-6 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Mulai")%></label><input type="date" id="poMulai<%=rndO%>" class="form-control"></div>
      <div class="col-6 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Sampai")%></label><input type="date" id="poSampai<%=rndO%>" class="form-control"></div>
      <div class="col-12 d-flex flex-wrap gap-3">
        <div class="form-check"><input class="form-check-input" type="checkbox" id="poByTermin<%=rndO%>" onchange="poToggleTermin<%=rndO%>()"><label class="form-check-label small" for="poByTermin<%=rndO%>"><%=Common.getBahasaConfig("Pembayaran Bertahap (Termin)")%></label></div>
        <div class="form-check"><input class="form-check-input" type="checkbox" id="poTanpaAnggaran<%=rndO%>" onchange="poToggleAnggaran<%=rndO%>()"><label class="form-check-label small" for="poTanpaAnggaran<%=rndO%>"><%=Common.getBahasaConfig("Tanpa Anggaran")%></label></div>
        <div class="form-check"><input class="form-check-input" type="checkbox" id="poLangsung<%=rndO%>"><label class="form-check-label small" for="poLangsung<%=rndO%>"><%=Common.getBahasaConfig("Pembelian Langsung")%></label></div>
        <div class="form-check"><input class="form-check-input" type="checkbox" id="poTampaPermintaan<%=rndO%>"><label class="form-check-label small" for="poTampaPermintaan<%=rndO%>"><%=Common.getBahasaConfig("Tanpa Permintaan (PR)")%></label></div>
      </div>
    </div>

    <hr class="my-3">
    <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 gap-2">
      <span class="fw-bold"><i class="fas fa-boxes me-2 text-primary"></i><%=Common.getBahasaConfig("Daftar Barang")%></span>
      <button class="btn btn-sm btn-outline-success rounded-pill" onclick="poBukaImporPr<%=rndO%>()"><i class="fas fa-file-import me-1"></i><%=Common.getBahasaConfig("Ambil dari Permintaan (PR)")%></button>
    </div>
    <div class="position-relative mb-2">
      <input type="text" id="poCari<%=rndO%>" class="form-control" placeholder="<%=Common.getBahasaConfig("Ketik nama/kode barang lalu pilih...")%>" autocomplete="off" oninput="poCariBarang<%=rndO%>()">
      <div id="poCariHasil<%=rndO%>" class="list-group position-absolute w-100 shadow" style="z-index:1090;max-height:240px;overflow:auto;display:none;"></div>
    </div>
    <div class="table-responsive">
      <table class="table table-sm align-middle">
        <thead class="table-light"><tr>
          <th><%=Common.getBahasaConfig("Barang")%></th><th style="width:90px" class="text-end"><%=Common.getBahasaConfig("Jumlah")%></th>
          <th style="width:120px" class="text-end"><%=Common.getBahasaConfig("Harga")%></th><th style="width:110px" class="text-end"><%=Common.getBahasaConfig("Potongan")%></th>
          <th style="width:48px" class="text-center" title="Potongan dalam %">%</th><th style="width:80px" class="text-end"><%=Common.getBahasaConfig("PPN%")%></th>
          <th style="width:80px" class="text-end"><%=Common.getBahasaConfig("PPh%")%></th><th style="width:140px" class="text-end"><%=Common.getBahasaConfig("Subtotal")%></th><th style="width:40px"></th>
        </tr></thead>
        <tbody id="poLines<%=rndO%>"></tbody>
        <tfoot><tr class="table-light fw-bold"><td colspan="7" class="text-end"><%=Common.getBahasaConfig("Total Nilai")%></td><td class="text-end" id="poTotal<%=rndO%>">Rp 0</td><td></td></tr></tfoot>
      </table>
    </div>

    <div id="poTerminWrap<%=rndO%>" style="display:none">
      <hr class="my-3">
      <div class="d-flex justify-content-between align-items-center mb-2">
        <span class="fw-bold"><i class="fas fa-list-ol me-2 text-primary"></i><%=Common.getBahasaConfig("Termin Pembayaran")%></span>
        <button class="btn btn-sm btn-outline-primary rounded-pill" onclick="poTambahTermin<%=rndO%>()"><i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Termin")%></button>
      </div>
      <div class="table-responsive"><table class="table table-sm align-middle">
        <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Nama Termin")%></th><th style="width:160px" class="text-end"><%=Common.getBahasaConfig("Penagihan")%></th><th style="width:150px"><%=Common.getBahasaConfig("Tanggal")%></th><th style="width:130px" class="text-end"><%=Common.getBahasaConfig("Pinalti")%></th><th style="width:40px"></th></tr></thead>
        <tbody id="poTermin<%=rndO%>"></tbody>
      </table></div>
    </div>
   </div>
   <div class="modal-footer">
     <button type="button" class="btn btn-light rounded-pill" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
     <button type="button" class="btn btn-primary rounded-pill px-4" id="poBtnSimpan<%=rndO%>" onclick="poSimpan<%=rndO%>()"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan")%></button>
   </div>
  </div>
 </div>
</div>

<!-- Modal pilih PR -->
<div class="modal fade" id="poPrModal<%=rndO%>" tabindex="-1" aria-hidden="true">
 <div class="modal-dialog modal-lg modal-dialog-scrollable"><div class="modal-content rounded-4">
   <div class="modal-header"><h6 class="modal-title fw-bold"><%=Common.getBahasaConfig("Pilih Permintaan (PR) yang Disetujui")%></h6><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
   <div class="modal-body"><div class="table-responsive"><table class="table table-hover align-middle">
     <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Satuan Kerja")%></th><th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th><th></th></tr></thead>
     <tbody id="poPrBody<%=rndO%>"></tbody></table></div></div>
 </div></div>
</div>

<script>
(function(){
  var SVC='<%=svcPO%>', rnd='<%=rndO%>', comboLoaded=false, combos={}, lockToko=false, tokoTerkunci='';
  function $(id){ return document.getElementById(id+rnd); }
  function rupiah(n){ n=Number(n)||0; return 'Rp '+n.toLocaleString('id-ID'); }
  function esc(s){ return (s==null?'':String(s)).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];}); }
  async function post(params){ var res=await fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},body:new URLSearchParams(params)}); return JSON.parse((await res.text()).trim()); }
  function opt(list,val){ var h='<option value="">- pilih -</option>'; (list||[]).forEach(function(x){ h+='<option value="'+x.id+'"'+(String(x.id)===String(val)?' selected':'')+'>'+esc(x.nama)+'</option>'; }); return h; }

  window['poLoad'+rnd]=async function(){
    var tb=$('poBody'); tb.innerHTML='<tr><td colspan="10" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr>';
    try{ var r=await post({aksi:'list',kode:$('pofKode').value,status:$('pofStatus').value,tglMulai:$('pofTglM').value,tglAkhir:$('pofTglA').value});
      if(r.status!=='00'){ tb.innerHTML='<tr><td colspan="10" class="text-center text-danger py-4">'+esc(r.message||'Gagal')+'</td></tr>'; return; }
      if(!r.data||!r.data.length){ tb.innerHTML='<tr><td colspan="10" class="text-center text-muted py-4">Belum ada data.</td></tr>'; return; }
      var html='';
      r.data.forEach(function(p,i){
        var badge=p.status==='disetujui'?'<span class="badge bg-success">Disetujui</span>':(p.status==='ditolak'?'<span class="badge bg-danger">Ditolak</span>':'<span class="badge bg-warning text-dark">Menunggu</span>');
        var aksi='<button class="btn btn-sm btn-outline-secondary me-1" onclick="poEdit'+rnd+'('+p.id+')"><i class="fas fa-eye"></i></button>';
        if(p.status==='pending'){ aksi+='<button class="btn btn-sm btn-outline-success me-1" onclick="poSetujui'+rnd+'('+p.id+')"><i class="fas fa-check"></i></button><button class="btn btn-sm btn-outline-danger" onclick="poHapus'+rnd+'('+p.id+')"><i class="fas fa-trash"></i></button>'; }
        html+='<tr><td>'+(i+1)+'</td><td class="fw-semibold">'+esc(p.kode)+'</td><td>'+esc(p.kodeInvoice)+'</td><td>'+esc(p.tanggal)+'</td><td>'+esc(p.satuanKerja)+'</td><td>'+esc(p.toko||'')+'</td><td class="text-center">'+p.jumlahItem+'</td><td class="text-end">'+rupiah(p.nilai)+'</td><td class="text-center">'+badge+(p.sop?' <i class="fas fa-route text-info" title="Dijalankan via SOP"></i>':'')+'</td><td class="text-end">'+aksi+'</td></tr>';
      });
      tb.innerHTML=html;
    }catch(e){ tb.innerHTML='<tr><td colspan="10" class="text-center text-danger py-4">Error: '+esc(e.message)+'</td></tr>'; }
  };

  async function loadCombo(){ if(comboLoaded) return; var r=await post({aksi:'combo'}); if(r.status==='00'){ combos=r;
    $('poSatker').innerHTML=opt(r.satuanKerja); $('poLokasi').innerHTML=opt(r.lokasi); $('poPerjanjian').innerHTML=opt(r.perjanjian);
    $('poJenis').innerHTML=opt(r.jenisPemesanan); $('poPpnDp').innerHTML=opt(r.jenisPajakPpn); $('poToko').innerHTML=opt(r.toko); $('poDisposisi').innerHTML=opt(r.disposisiSop);
    lockToko=!!r.lockToko; tokoTerkunci=r.tokoTerkunci||''; comboLoaded=true; } }
  function modal(){ return bootstrap.Modal.getOrCreateInstance($('poModal')); }

  window['poBaru'+rnd]=async function(){ await loadCombo();
    $('poId').value=''; $('poKode').value=''; $('poInvoice').value=''; $('poTgl').value=new Date().toISOString().slice(0,10);
    $('poKirim').value=''; $('poSatker').value=''; $('poLokasi').value=''; $('poPerjanjian').value=''; $('poJenis').value='';
    $('poKet').value=''; $('poCatatan').value=''; $('poDp').value='0'; $('poPpnDp').value=''; $('poMulai').value=''; $('poSampai').value=''; $('poToko').value = lockToko ? tokoTerkunci : ''; $('poDisposisi').value=''; $('poWorkspace').value=''; $('poWorkspaceId').value=''; poToggleAnggaranFn();
    ['poByTermin','poTanpaAnggaran','poLangsung','poTampaPermintaan'].forEach(function(k){ $(k).checked=false; });
    $('poLines').innerHTML=''; $('poTermin').innerHTML=''; toggleTermin(); recalc(); enableForm(true);
    document.getElementById('poTitle'+rnd).textContent='Buat Pemesanan'; modal().show();
  };

  window['poEdit'+rnd]=async function(id){ await loadCombo();
    var r=await post({aksi:'detail',id:id}); if(r.status!=='00'){ tampilkanPesanGagalFormal("pengelolaan Purchase Order (PO) kantin", r.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); return; } var d=r.data;
    $('poId').value=d.id; $('poKode').value=d.kode||''; $('poInvoice').value=d.kodeInvoice||''; $('poTgl').value=d.tanggal||'';
    $('poKirim').value=d.pengiriman||''; $('poSatker').value=d.satuanKerja||''; $('poLokasi').value=d.lokasi||''; $('poPerjanjian').value=d.perjanjian||'';
    $('poJenis').value=d.jenisPemesanan||''; $('poKet').value=d.keterangan||''; $('poCatatan').value=d.catatanKesepakatan||'';
    $('poDp').value=d.dp||0; $('poPpnDp').value=d.jenisPajakPpnDp||''; $('poMulai').value=d.tglMulai||''; $('poSampai').value=d.tglSampai||''; $('poToko').value = lockToko ? tokoTerkunci : (d.toko||''); $('poDisposisi').value=d.disposisiSop||''; $('poWorkspaceId').value=d.workspace||''; $('poWorkspace').value=d.workspaceNama||''; poToggleAnggaranFn();
    $('poByTermin').checked=!!d.byTermin; $('poTanpaAnggaran').checked=!!d.tanpaAnggaran; $('poLangsung').checked=!!d.pembelianLangsung; $('poTampaPermintaan').checked=!!d.tampaPermintaan;
    $('poLines').innerHTML=''; (d.lines||[]).forEach(function(l){ tambahBaris(l); });
    $('poTermin').innerHTML=''; try{ JSON.parse(d.formula||'[]').forEach(function(t){ tambahTermin(t); }); }catch(e){}
    toggleTermin(); recalc();
    var bisa=d.status==='pending'; enableForm(bisa);
    document.getElementById('poTitle'+rnd).textContent=bisa?'Ubah Pemesanan':'Detail Pemesanan ('+(d.status==='disetujui'?'Disetujui':'Ditolak')+')';
    modal().show();
  };

  function enableForm(on){
    $('poModal').querySelectorAll('.modal-body input,.modal-body select,.modal-body button').forEach(function(el){ if(el.id!=='poCari'+rnd) el.disabled=!on; });
    $('poCari').disabled=!on; if($('poToko')) $('poToko').disabled = !on || lockToko; $('poBtnSimpan').style.display=on?'':'none';
  }

  function toggleTermin(){ $('poTerminWrap').style.display=$('poByTermin').checked?'':'none'; }
  window['poToggleTermin'+rnd]=toggleTermin;

  var cariTimer=null;
  window['poCariBarang'+rnd]=function(){ clearTimeout(cariTimer); cariTimer=setTimeout(async function(){
    var q=$('poCari').value.trim(), box=$('poCariHasil'); if(q.length<2){ box.style.display='none'; return; }
    var r=await post({aksi:'cariMasterAsset',q:q});
    if(r.status!=='00'||!r.data||!r.data.length){ box.innerHTML='<div class="list-group-item text-muted small">Tidak ada barang.</div>'; box.style.display='block'; return; }
    box.innerHTML=r.data.map(function(m){ return '<button type="button" class="list-group-item list-group-item-action" data-id="'+m.id+'" data-nama="'+esc(m.kode+' - '+m.nama)+'" data-harga="'+(m.harga||0)+'"><span class="fw-semibold">'+esc(m.nama)+'</span> <small class="text-muted">'+esc(m.kode)+'</small> <span class="float-end small">'+rupiah(m.harga)+'</span></button>'; }).join('');
    box.style.display='block';
    box.querySelectorAll('button').forEach(function(b){ b.onclick=function(){ tambahBaris({masterAsset:b.getAttribute('data-id'),masterAssetNama:b.getAttribute('data-nama'),jumlah:1,hargaBeli:Number(b.getAttribute('data-harga')),hargaPotongan:0,diskonPersen:false,persenPpn:0,persenPph:0,keterangan:''}); recalc(); box.style.display='none'; $('poCari').value=''; }; });
  },250); };
  document.addEventListener('click',function(e){ var box=$('poCariHasil'); if(box&&!box.contains(e.target)&&e.target!==$('poCari')) box.style.display='none'; });

  // Pemilih cari Anggaran (Workspace) ala banbox ZK
  function pickerCari(inKey, hasilKey, hiddenKey, aksi, onSelect){
    clearTimeout(cariTimer);
    cariTimer = setTimeout(async function(){
      $(hiddenKey).value='';
      var q=$(inKey).value.trim(), box=$(hasilKey);
      if(q.length<2){ box.style.display='none'; return; }
      var r=await post({aksi:aksi, q:q});
      if(r.status!=='00'||!r.data||!r.data.length){ box.innerHTML='<div class="list-group-item text-muted small">Tidak ada data.</div>'; box.style.display='block'; return; }
      box.innerHTML=r.data.map(function(x){ return '<button type="button" class="list-group-item list-group-item-action" data-id="'+x.id+'" data-nama="'+esc(x.nama)+'">'+esc(x.nama)+'</button>'; }).join('');
      box.style.display='block';
      box.querySelectorAll('button').forEach(function(b){ b.onclick=function(){ $(hiddenKey).value=b.getAttribute('data-id'); $(inKey).value=b.getAttribute('data-nama'); box.style.display='none'; if(onSelect) onSelect(); }; });
    },250);
  }
  window['poCariWorkspace'+rnd]=function(){ pickerCari('poWorkspace','poWorkspaceHasil','poWorkspaceId','cariWorkspace', infoAnggaran); };
  async function infoAnggaran(){
    var box=$('poAnggaranInfo'); var wid=$('poWorkspaceId').value;
    if(!wid){ if(box) box.style.display='none'; return; }
    try{
      var r=await post({ aksi:'infoAnggaran', workspace:wid, id:$('poId').value, tanggal:$('poTgl').value });
      if(r.status==='00' && r.ada){
        $('poAiUnit').textContent=r.unit||'-'; $('poAiNilai').textContent=rupiah(r.nilai);
        $('poAiProses').textContent=rupiah(r.dalamProses); $('poAiSisa').textContent=rupiah(r.sisa);
        $('poAiPeriode').textContent=r.periode||'-'; $('poAiAkun').textContent=r.akun||'-';
        box.style.display='';
      } else box.style.display='none';
    }catch(e){ box.style.display='none'; }
  }
  function poToggleAnggaranFn(){
    var sembunyi=$('poTanpaAnggaran').checked;
    if($('poWorkspaceWrap')) $('poWorkspaceWrap').style.display = sembunyi ? 'none' : '';
    if(sembunyi){ if($('poAnggaranInfo')) $('poAnggaranInfo').style.display='none'; }
    else if($('poWorkspaceId').value){ infoAnggaran(); }
  }
  window['poToggleAnggaran'+rnd]=poToggleAnggaranFn;
  document.addEventListener('click',function(e){ var box=$('poWorkspaceHasil'); if(box&&!box.contains(e.target)&&e.target!==$('poWorkspace')) box.style.display='none'; });

  function num(v){ return Number(v)||0; }
  function lineTotal(j,h,pot,pct,ppn,pph){ var dpp=j*h; var potongan=pct?((pot/100)*dpp):pot; dpp=dpp-potongan; var vppn=(ppn/100)*dpp; var vpph=(pph/100)*dpp; return (dpp+vppn)-vpph; }

  function tambahBaris(l){
    var tr=document.createElement('tr');
    tr.setAttribute('data-id', l.masterAsset||'');
    tr.setAttribute('data-prdetail', l.prDetailId||'');
    tr.innerHTML=
      '<td>'+esc(l.masterAssetNama||'')+'<input type="hidden" class="bb-ket" value="'+esc(l.keterangan||'')+'"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-jml" value="'+num(l.jumlah||1)+'" oninput="poRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-hrg" value="'+num(l.hargaBeli||0)+'" oninput="poRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-pot" value="'+num(l.hargaPotongan||0)+'" oninput="poRecalc'+rnd+'()"></td>'
      +'<td class="text-center"><input type="checkbox" class="form-check-input bb-pct" '+(l.diskonPersen?'checked':'')+' onchange="poRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-ppn" value="'+num(l.persenPpn||0)+'" oninput="poRecalc'+rnd+'()"></td>'
      +'<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-pph" value="'+num(l.persenPph||0)+'" oninput="poRecalc'+rnd+'()"></td>'
      +'<td class="text-end bb-sub">Rp 0</td>'
      +'<td class="text-center"><button class="btn btn-sm btn-outline-danger" onclick="this.closest(\'tr\').remove();poRecalc'+rnd+'()"><i class="fas fa-times"></i></button></td>';
    $('poLines').appendChild(tr);
  }
  function recalc(){ var total=0;
    $('poLines').querySelectorAll('tr').forEach(function(tr){
      var t=lineTotal(num(tr.querySelector('.bb-jml').value),num(tr.querySelector('.bb-hrg').value),num(tr.querySelector('.bb-pot').value),tr.querySelector('.bb-pct').checked,num(tr.querySelector('.bb-ppn').value),num(tr.querySelector('.bb-pph').value));
      tr.querySelector('.bb-sub').textContent=rupiah(t); total+=t; });
    $('poTotal').textContent=rupiah(total);
  }
  window['poRecalc'+rnd]=recalc;

  function tambahTermin(t){ t=t||{};
    var tr=document.createElement('tr');
    tr.innerHTML='<td><input type="text" class="form-control form-control-sm tm-nama" value="'+esc(t.nama||'')+'"></td>'
      +'<td><input type="number" step="any" class="form-control form-control-sm text-end tm-tagih" value="'+num(t.penagihan||0)+'"></td>'
      +'<td><input type="date" class="form-control form-control-sm tm-tgl" value="'+esc(t.tanggalISO||'')+'"></td>'
      +'<td><input type="number" step="any" class="form-control form-control-sm text-end tm-pinalti" value="'+num(t.pinalti||0)+'"></td>'
      +'<td class="text-center"><button class="btn btn-sm btn-outline-danger" onclick="this.closest(\'tr\').remove()"><i class="fas fa-times"></i></button></td>';
    $('poTermin').appendChild(tr);
  }
  window['poTambahTermin'+rnd]=function(){ tambahTermin({}); };

  window['poBukaImporPr'+rnd]=async function(){
    var r=await post({aksi:'listPr'}); var tb=$('poPrBody');
    if(r.status!=='00'){ tampilkanPesanGagalFormal("pengelolaan Purchase Order (PO) kantin", r.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); return; }
    if(!r.data||!r.data.length){ tb.innerHTML='<tr><td colspan="4" class="text-center text-muted py-3">Tidak ada PR disetujui yang belum dipesan.</td></tr>'; }
    else tb.innerHTML=r.data.map(function(p){ return '<tr><td class="fw-semibold">'+esc(p.kode)+'</td><td>'+esc(p.satuanKerja)+'</td><td class="text-end">'+rupiah(p.nilai)+'</td><td><button class="btn btn-sm btn-primary" onclick="poImporPr'+rnd+'('+p.id+')">Ambil</button></td></tr>'; }).join('');
    bootstrap.Modal.getOrCreateInstance($('poPrModal')).show();
  };
  window['poImporPr'+rnd]=async function(prId){
    var r=await post({aksi:'importPr',id:prId}); if(r.status!=='00'){ tampilkanPesanGagalFormal("pengelolaan Purchase Order (PO) kantin", r.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); return; } var d=r.data;
    if(d.satuanKerja) $('poSatker').value=d.satuanKerja; if(d.lokasi) $('poLokasi').value=d.lokasi; if(d.keterangan&&!$('poKet').value) $('poKet').value=d.keterangan;
    (d.lines||[]).forEach(function(l){ l.persenPpn=0; l.persenPph=0; l.hargaPotongan=0; l.diskonPersen=false; tambahBaris(l); });
    recalc(); bootstrap.Modal.getOrCreateInstance($('poPrModal')).hide();
  };

  window['poSimpan'+rnd]=async function(){
    var lines=[];
    $('poLines').querySelectorAll('tr').forEach(function(tr){ lines.push({ masterAsset:tr.getAttribute('data-id'), prDetailId:tr.getAttribute('data-prdetail')||'', jumlah:num(tr.querySelector('.bb-jml').value), hargaBeli:num(tr.querySelector('.bb-hrg').value), hargaPotongan:num(tr.querySelector('.bb-pot').value), diskonPersen:tr.querySelector('.bb-pct').checked, persenPpn:num(tr.querySelector('.bb-ppn').value), persenPph:num(tr.querySelector('.bb-pph').value), keterangan:tr.querySelector('.bb-ket').value||'' }); });
    if(!lines.length){ tampilkanPesanGagalFormal("penyimpanan Purchase Order (PO)", '<%=Common.getBahasaConfigJS("Belum ada satu pun barang yang ditambahkan pada rincian PO ini.")%>', ["Klik tombol cari/tambah barang untuk memasukkan minimal satu baris barang.", "Setelah rincian terisi, silakan simpan kembali."]); return; }
    var termin=[];
    $('poTermin').querySelectorAll('tr').forEach(function(tr,i){ termin.push({ key:'t'+(i+1), nama:tr.querySelector('.tm-nama').value, penagihan:num(tr.querySelector('.tm-tagih').value), tanggalISO:tr.querySelector('.tm-tgl').value, pinalti:num(tr.querySelector('.tm-pinalti').value) }); });
    var payload={ id:$('poId').value, kode:$('poKode').value, kodeInvoice:$('poInvoice').value, tanggal:$('poTgl').value, pengiriman:$('poKirim').value, tglMulai:$('poMulai').value, tglSampai:$('poSampai').value, satuanKerja:$('poSatker').value, lokasi:$('poLokasi').value, toko:$('poToko').value, disposisiSop:$('poDisposisi').value, workspace:$('poWorkspaceId').value, perjanjian:$('poPerjanjian').value, jenisPemesanan:$('poJenis').value, jenisPajakPpnDp:$('poPpnDp').value, dp:num($('poDp').value), keterangan:$('poKet').value, catatanKesepakatan:$('poCatatan').value, byTermin:$('poByTermin').checked, tanpaAnggaran:$('poTanpaAnggaran').checked, pembelianLangsung:$('poLangsung').checked, tampaPermintaan:$('poTampaPermintaan').checked, formula:JSON.stringify(termin), lines:lines };
    var btn=$('poBtnSimpan'); btn.disabled=true; var ori=btn.innerHTML; btn.innerHTML='<span class="spinner-border spinner-border-sm me-2"></span>Menyimpan...';
    try{ var r=await post({aksi:'simpan',data:JSON.stringify(payload)}); if(r.status==='00'){ modal().hide(); window['poLoad'+rnd](); } else tampilkanPesanGagalFormal("pengelolaan Purchase Order (PO) kantin", r.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); }
    catch(e){ tampilkanPesanGagalFormal("penyimpanan Purchase Order (PO)", "Rincian teknis: "+e.message, ["Periksa koneksi internet Bapak/Ibu.", "Ulangi proses penyimpanan beberapa saat lagi."]); } finally{ btn.disabled=false; btn.innerHTML=ori; }
  };
  window['poSetujui'+rnd]=async function(id){ if(!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menyetujui pemesanan ini?")%>'))return; var r=await post({aksi:'setujui',id:id}); if(r.status==='00') window['poLoad'+rnd](); else tampilkanPesanGagalFormal("pengelolaan Purchase Order (PO) kantin", r.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); };
  window['poHapus'+rnd]=async function(id){ if(!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus pemesanan ini?")%>'))return; var r=await post({aksi:'hapus',id:id}); if(r.status==='00') window['poLoad'+rnd](); else tampilkanPesanGagalFormal("pengelolaan Purchase Order (PO) kantin", r.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); };

  window['poLoad'+rnd]();
})();
</script>
