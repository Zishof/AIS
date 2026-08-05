<%--
  "Pengiriman Antar Gudang" (versi JSP) — Gudang Pusat <-> Cabang/Outlet dengan jeda konfirmasi
  terima (BUKAN transfer instan seperti Mutasi Gudang > Transfer). Dua tab: "Kirim Baru" (buat
  dokumen, boleh banyak baris produk) dan "Perlu Diterima" (inbox per lokasi tujuan, konfirmasi
  qty diterima -- boleh sebagian). Reuse pengiriman_gudang/service.jsp (PengirimanGudangUtil).
  Gate admin (bukan pedagang/toko) utk aksi kirim/terima; siapa saja boleh melihat.
--%>
<%@page import="ais.common.Common"%>
<%@page import="ais.action.master.koperasi.helper.LokasiKantinUtil"%>
<%
String rpg = ais.common.Common.getGeneratedBarCode(6);
boolean bolehPG = LokasiKantinUtil.bolehKelola(request);
String svcPG = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fpengiriman_gudang&s=service";
%>
<div class="pg-wrap-<%=rpg%>">
  <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
    <div>
      <h5 class="fw-bold mb-0"><i class="fas fa-truck-fast text-primary me-2"></i><%=Common.getBahasaConfig("Pengiriman Antar Gudang")%></h5>
      <div class="text-muted small"><%=Common.getBahasaConfig("Kirim barang dari Gudang Pusat ke Cabang/Outlet -- stok tujuan baru bertambah SETELAH penerima mengonfirmasi terima (boleh penuh atau sebagian).")%></div>
    </div>
  </div>

  <% if (!bolehPG) { %>
  <div class="alert alert-light border small py-2"><i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Anda hanya dapat melihat riwayat. Kirim/terima hanya untuk admin (bukan pedagang/toko).")%></div>
  <% } %>

  <ul class="nav nav-tabs mb-3" role="tablist">
    <li class="nav-item"><button class="nav-link active" data-bs-toggle="tab" data-bs-target="#pgTabKirim<%=rpg%>" type="button"><i class="fas fa-paper-plane me-1"></i><%=Common.getBahasaConfig("Kirim Baru")%></button></li>
    <li class="nav-item"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#pgTabTerima<%=rpg%>" type="button"><i class="fas fa-inbox me-1"></i><%=Common.getBahasaConfig("Perlu Diterima")%> <span class="badge bg-danger rounded-pill ms-1 d-none" id="pgBadgeInbox<%=rpg%>">0</span></button></li>
  </ul>

  <div class="tab-content">
    <!-- ====== TAB: Kirim Baru ====== -->
    <div class="tab-pane fade show active" id="pgTabKirim<%=rpg%>" role="tabpanel">
      <% if (bolehPG) { %>
      <div class="row g-3 mb-3">
        <div class="col-md-6">
          <label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Lokasi Asal (mis. Gudang Pusat)")%></label>
          <select id="pgAsal<%=rpg%>" class="form-select" onchange="pgMuatRiwayatAsal<%=rpg%>()"></select>
        </div>
        <div class="col-md-6">
          <label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Lokasi Tujuan (mis. Cabang/Outlet)")%></label>
          <select id="pgTujuan<%=rpg%>" class="form-select"></select>
        </div>
      </div>

      <div class="card border-0 shadow-sm rounded-4 mb-3"><div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <h6 class="fw-bold mb-0"><%=Common.getBahasaConfig("Baris Produk")%></h6>
          <button type="button" class="btn btn-sm btn-outline-primary" onclick="pgTambahBaris<%=rpg%>()"><i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Baris")%></button>
        </div>
        <div class="table-responsive">
          <table class="table table-sm align-middle">
            <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Produk")%></th><th style="width:120px"><%=Common.getBahasaConfig("Qty")%></th><th style="width:140px"><%=Common.getBahasaConfig("Harga Satuan")%></th><th style="width:40px"></th></tr></thead>
            <tbody id="pgBarisBody<%=rpg%>"></tbody>
          </table>
        </div>
        <div class="row g-2 mt-1">
          <div class="col-md-4"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Tanggal Kirim")%></label>
            <input type="date" id="pgTgl<%=rpg%>" class="form-control"/></div>
          <div class="col-md-8"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Keterangan")%></label>
            <input id="pgKet<%=rpg%>" class="form-control"/></div>
        </div>
        <button class="btn btn-primary mt-3" onclick="pgKirim<%=rpg%>()"><i class="fas fa-paper-plane me-1"></i><%=Common.getBahasaConfig("Kirim Sekarang")%></button>
      </div></div>
      <% } %>

      <div class="card border-0 shadow-sm rounded-4"><div class="card-body">
        <h6 class="fw-bold mb-2"><%=Common.getBahasaConfig("Riwayat Kirim dari Lokasi Asal Terpilih")%></h6>
        <div class="table-responsive" style="max-height:420px;overflow:auto">
          <table class="table table-sm table-hover align-middle">
            <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Tujuan")%></th><th><%=Common.getBahasaConfig("Tanggal Kirim")%></th><th><%=Common.getBahasaConfig("Status")%></th></tr></thead>
            <tbody id="pgRiwayatAsalBody<%=rpg%>"><tr><td colspan="4" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Pilih lokasi asal terlebih dahulu.")%></td></tr></tbody>
          </table>
        </div>
      </div></div>
    </div>

    <!-- ====== TAB: Perlu Diterima ====== -->
    <div class="tab-pane fade" id="pgTabTerima<%=rpg%>" role="tabpanel">
      <div class="row g-3 mb-3">
        <div class="col-md-6">
          <label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Lokasi Saya (Tujuan)")%></label>
          <select id="pgLokasiSaya<%=rpg%>" class="form-select" onchange="pgMuatInbox<%=rpg%>()"></select>
        </div>
        <div class="col-md-6">
          <label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Filter Status")%></label>
          <select id="pgFilterStatus<%=rpg%>" class="form-select" onchange="pgMuatInbox<%=rpg%>()">
            <option value=""><%=Common.getBahasaConfig("Semua")%></option>
            <option value="DIKIRIM" selected><%=Common.getBahasaConfig("Dikirim (belum diterima)")%></option>
            <option value="DITERIMA_SEBAGIAN"><%=Common.getBahasaConfig("Diterima Sebagian")%></option>
            <option value="DITERIMA"><%=Common.getBahasaConfig("Diterima Penuh")%></option>
          </select>
        </div>
      </div>

      <div class="table-responsive">
        <table class="table table-sm table-hover align-middle">
          <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Dari")%></th><th><%=Common.getBahasaConfig("Tanggal Kirim")%></th><th><%=Common.getBahasaConfig("Status")%></th><th></th></tr></thead>
          <tbody id="pgInboxBody<%=rpg%>"><tr><td colspan="5" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Pilih lokasi tujuan terlebih dahulu.")%></td></tr></tbody>
        </table>
      </div>

      <!-- Panel Terima (muncul saat satu dokumen dipilih) -->
      <div class="card border-0 shadow-sm rounded-4 mt-3 d-none" id="pgPanelTerima<%=rpg%>"><div class="card-body">
        <h6 class="fw-bold mb-2"><i class="fas fa-inbox me-1"></i><%=Common.getBahasaConfig("Konfirmasi Terima")%> — <span id="pgTerimaKode<%=rpg%>"></span></h6>
        <div class="table-responsive">
          <table class="table table-sm align-middle">
            <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Produk")%></th><th class="text-end"><%=Common.getBahasaConfig("Qty Dikirim")%></th><th class="text-end"><%=Common.getBahasaConfig("Sudah Diterima")%></th><th style="width:140px"><%=Common.getBahasaConfig("Terima Sekarang")%></th></tr></thead>
            <tbody id="pgTerimaBarisBody<%=rpg%>"></tbody>
          </table>
        </div>
        <label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Keterangan Penerimaan (opsional -- isi bila ada selisih)")%></label>
        <input id="pgTerimaKet<%=rpg%>" class="form-control mb-2"/>
        <button class="btn btn-success" onclick="pgSubmitTerima<%=rpg%>()"><i class="fas fa-check me-1"></i><%=Common.getBahasaConfig("Simpan Penerimaan")%></button>
        <button class="btn btn-outline-secondary ms-1" onclick="document.getElementById('pgPanelTerima<%=rpg%>').classList.add('d-none')"><%=Common.getBahasaConfig("Batal")%></button>
      </div></div>
    </div>
  </div>
</div>

<script>
(function(){
  var SVC='<%=svcPG%>', BOLEH=<%=bolehPG%>, lokasi=[], produk=[], pengirimanTerimaAktif=null;
  function esc(s){return (s==null?'':String(s)).replace(/[&<>"]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
  function fmt(n){ return (Math.round((n||0)*100)/100).toLocaleString('id-ID'); }
  function fill(el, list, empty){ var h=empty?'<option value="">'+empty+'</option>':''; list.forEach(function(x){ h+='<option value="'+x.id+'">'+esc(x.nama)+(x.jenis?(' ('+esc(x.jenis)+')'):'')+'</option>'; }); el.innerHTML=h; }
  var badgeStatus={DIKIRIM:'warning',DITERIMA_SEBAGIAN:'info',DITERIMA:'success',DIBATALKAN:'secondary'};

  // ===== Baris produk (tab Kirim Baru) =====
  window.pgTambahBaris<%=rpg%>=function(){
    var tb=document.getElementById('pgBarisBody<%=rpg%>');
    var tr=document.createElement('tr');
    var optProduk=''; produk.forEach(function(p){ optProduk+='<option value="'+p.id+'" data-h="'+p.hargabeli+'">'+esc(p.kode?(p.kode+' — '):'')+esc(p.nama)+'</option>'; });
    tr.innerHTML='<td><select class="form-select form-select-sm pg-baris-produk" onchange="pgIsiHargaDefault<%=rpg%>(this)"><option value="">— pilih —</option>'+optProduk+'</select></td>'
      +'<td><input type="number" step="any" class="form-control form-control-sm pg-baris-qty"></td>'
      +'<td><input type="number" step="any" class="form-control form-control-sm pg-baris-harga"></td>'
      +'<td><button type="button" class="btn btn-sm btn-outline-danger" onclick="this.closest(\'tr\').remove()"><i class="fas fa-trash"></i></button></td>';
    tb.appendChild(tr);
  };
  window.pgIsiHargaDefault<%=rpg%>=function(sel){
    var tr=sel.closest('tr'), opt=sel.selectedOptions[0], hargaInput=tr.querySelector('.pg-baris-harga');
    if(opt && !hargaInput.value){ hargaInput.value=opt.getAttribute('data-h')||''; }
  };

  window.pgKirim<%=rpg%>=function(){
    var asal=document.getElementById('pgAsal<%=rpg%>').value, tujuan=document.getElementById('pgTujuan<%=rpg%>').value;
    if(!asal || !tujuan){ tampilkanPesanGagalFormal("pengiriman antar gudang", '<%=Common.getBahasaConfigJS("Lokasi asal dan tujuan wajib dipilih.")%>', ["Pilih Lokasi Asal dan Lokasi Tujuan terlebih dahulu."]); return; }
    if(asal===tujuan){ tampilkanPesanGagalFormal("pengiriman antar gudang", '<%=Common.getBahasaConfigJS("Lokasi asal dan tujuan tidak boleh sama.")%>', ["Pilih dua lokasi yang berbeda."]); return; }
    var rows=document.querySelectorAll('#pgBarisBody<%=rpg%> tr'), baris=[];
    rows.forEach(function(tr){
      var p=tr.querySelector('.pg-baris-produk').value, q=parseFloat(tr.querySelector('.pg-baris-qty').value||'0'), h=tr.querySelector('.pg-baris-harga').value;
      if(p && q>0){ baris.push({produk:parseInt(p,10), qty:q, harga:h?parseFloat(h):null}); }
    });
    if(!baris.length){ tampilkanPesanGagalFormal("pengiriman antar gudang", '<%=Common.getBahasaConfigJS("Tambahkan minimal satu baris produk dengan jumlah lebih dari 0.")%>', ["Klik \"Tambah Baris\", pilih produk, isi jumlah lebih dari 0."]); return; }
    var pr=new URLSearchParams();
    pr.append('aksi','kirim'); pr.append('lokasiAsal',asal); pr.append('lokasiTujuan',tujuan);
    pr.append('tanggal',document.getElementById('pgTgl<%=rpg%>').value); pr.append('keterangan',document.getElementById('pgKet<%=rpg%>').value);
    pr.append('baris', JSON.stringify(baris));
    fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:pr.toString()})
      .then(function(r){return r.json();}).then(function(res){
        if(res.status==='00'){
          document.getElementById('pgBarisBody<%=rpg%>').innerHTML=''; pgTambahBaris<%=rpg%>();
          document.getElementById('pgKet<%=rpg%>').value='';
          pgMuatRiwayatAsal<%=rpg%>();
        } else {
          tampilkanPesanGagalFormal("pengiriman antar gudang", res.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali baris produk pada formulir.", "Ulangi proses beberapa saat lagi."]);
        }
      });
  };

  window.pgMuatRiwayatAsal<%=rpg%>=function(){
    var asal=document.getElementById('pgAsal<%=rpg%>').value, tb=document.getElementById('pgRiwayatAsalBody<%=rpg%>');
    if(!asal){ tb.innerHTML='<tr><td colspan="4" class="text-center text-muted py-3">Pilih lokasi asal terlebih dahulu.</td></tr>'; return; }
    fetch(SVC+'&aksi=daftarAsal&lokasiAsal='+asal).then(function(r){return r.json();}).then(function(j){
      var rows=(j&&j.data)||[];
      if(!rows.length){ tb.innerHTML='<tr><td colspan="4" class="text-center text-muted py-3">Belum ada pengiriman.</td></tr>'; return; }
      var h='';
      rows.forEach(function(d){
        h+='<tr><td class="small fw-semibold">'+esc(d.kode)+'</td><td class="small">'+esc(d.lokasiTujuan)+'</td><td class="small">'+esc(d.tanggalKirim)+'</td>'
          +'<td><span class="badge bg-'+(badgeStatus[d.status]||'secondary')+'-subtle text-'+(badgeStatus[d.status]||'secondary')+'">'+esc(d.status)+'</span></td></tr>';
      });
      tb.innerHTML=h;
    });
  };

  // ===== Inbox (tab Perlu Diterima) =====
  window.pgMuatInbox<%=rpg%>=function(){
    var tujuan=document.getElementById('pgLokasiSaya<%=rpg%>').value, status=document.getElementById('pgFilterStatus<%=rpg%>').value;
    var tb=document.getElementById('pgInboxBody<%=rpg%>');
    document.getElementById('pgPanelTerima<%=rpg%>').classList.add('d-none');
    if(!tujuan){ tb.innerHTML='<tr><td colspan="5" class="text-center text-muted py-3">Pilih lokasi tujuan terlebih dahulu.</td></tr>'; return; }
    fetch(SVC+'&aksi=daftarTujuan&lokasiTujuan='+tujuan+(status?('&status='+status):'')).then(function(r){return r.json();}).then(function(j){
      var rows=(j&&j.data)||[];
      var badgeEl=document.getElementById('pgBadgeInbox<%=rpg%>');
      var perluTindak=rows.filter(function(d){return d.status==='DIKIRIM'||d.status==='DITERIMA_SEBAGIAN';}).length;
      if(perluTindak>0){ badgeEl.textContent=perluTindak; badgeEl.classList.remove('d-none'); } else { badgeEl.classList.add('d-none'); }
      if(!rows.length){ tb.innerHTML='<tr><td colspan="5" class="text-center text-muted py-3">Tidak ada dokumen.</td></tr>'; return; }
      var h='';
      rows.forEach(function(d){
        var bisaTerima=(d.status==='DIKIRIM'||d.status==='DITERIMA_SEBAGIAN');
        h+='<tr><td class="small fw-semibold">'+esc(d.kode)+'</td><td class="small">'+esc(d.lokasiAsal)+'</td><td class="small">'+esc(d.tanggalKirim)+'</td>'
          +'<td><span class="badge bg-'+(badgeStatus[d.status]||'secondary')+'-subtle text-'+(badgeStatus[d.status]||'secondary')+'">'+esc(d.status)+'</span></td>'
          +'<td>'+(bisaTerima?('<button type="button" class="btn btn-sm btn-primary" onclick="pgBukaTerima<%=rpg%>('+d.id+')"><i class="fas fa-box-open me-1"></i>Terima</button>'):'')+'</td></tr>';
      });
      tb.innerHTML=h;
    });
  };

  window.pgBukaTerima<%=rpg%>=function(pengirimanId){
    fetch(SVC+'&aksi=detail&pengiriman='+pengirimanId).then(function(r){return r.json();}).then(function(j){
      if(j.status!=='00'){ tampilkanPesanGagalFormal("membuka dokumen pengiriman", j.message||'Gagal memuat detail.', []); return; }
      pengirimanTerimaAktif=pengirimanId;
      document.getElementById('pgTerimaKode<%=rpg%>').textContent=j.header.kode+' ('+j.header.lokasiAsal+' → '+j.header.lokasiTujuan+')';
      var tb=document.getElementById('pgTerimaBarisBody<%=rpg%>'), h='';
      (j.baris||[]).forEach(function(b){
        var sisaBelumDiterima=(b.qtyKirim||0)-(b.qtyTerima||0);
        h+='<tr data-detail-id="'+b.id+'"><td class="small">'+esc(b.kode?(b.kode+' — '):'')+esc(b.produk)+'</td>'
          +'<td class="text-end small">'+fmt(b.qtyKirim)+'</td><td class="text-end small">'+fmt(b.qtyTerima||0)+'</td>'
          +'<td><input type="number" step="any" class="form-control form-control-sm pg-terima-qty" value="'+fmt(sisaBelumDiterima)+'" data-max="'+sisaBelumDiterima+'"></td></tr>';
      });
      tb.innerHTML=h;
      document.getElementById('pgTerimaKet<%=rpg%>').value='';
      document.getElementById('pgPanelTerima<%=rpg%>').classList.remove('d-none');
      document.getElementById('pgPanelTerima<%=rpg%>').scrollIntoView({behavior:'smooth', block:'nearest'});
    });
  };

  window.pgSubmitTerima<%=rpg%>=function(){
    if(!pengirimanTerimaAktif){ return; }
    var rows=document.querySelectorAll('#pgTerimaBarisBody<%=rpg%> tr'), qtyMap={}, adaIsi=false;
    rows.forEach(function(tr){
      var id=tr.getAttribute('data-detail-id'), input=tr.querySelector('.pg-terima-qty');
      var q=parseFloat(input.value||'0');
      if(q>0){ adaIsi=true; }
      qtyMap[id]=q;
    });
    if(!adaIsi){ tampilkanPesanGagalFormal("konfirmasi terima", '<%=Common.getBahasaConfigJS("Isi jumlah diterima minimal satu baris dengan nilai lebih dari 0.")%>', ["Masukkan jumlah yang benar-benar diterima pada minimal satu baris."]); return; }
    var pr=new URLSearchParams();
    pr.append('aksi','terima'); pr.append('pengiriman',pengirimanTerimaAktif);
    pr.append('keterangan',document.getElementById('pgTerimaKet<%=rpg%>').value);
    pr.append('qtyTerima', JSON.stringify(qtyMap));
    fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:pr.toString()})
      .then(function(r){return r.json();}).then(function(res){
        if(res.status==='00'){
          document.getElementById('pgPanelTerima<%=rpg%>').classList.add('d-none');
          pengirimanTerimaAktif=null;
          pgMuatInbox<%=rpg%>();
        } else {
          tampilkanPesanGagalFormal("konfirmasi terima", res.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali jumlah yang dimasukkan.", "Ulangi proses beberapa saat lagi."]);
        }
      });
  };

  // ===== Inisialisasi =====
  fetch(SVC+'&aksi=ref').then(function(r){return r.json();}).then(function(j){
    lokasi=(j&&j.lokasi)||[]; produk=(j&&j.produk)||[];
    if(BOLEH){
      fill(document.getElementById('pgAsal<%=rpg%>'),lokasi,'— pilih —');
      fill(document.getElementById('pgTujuan<%=rpg%>'),lokasi,'— pilih —');
      pgTambahBaris<%=rpg%>();
    }
    fill(document.getElementById('pgLokasiSaya<%=rpg%>'),lokasi,'— pilih —');
  });
})();
</script>
