<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
Tbmuser penggunaPiutang = Common.getCurrentUser(request);
if (penggunaPiutang == null || penggunaPiutang.getUserId() == null) {
    out.print("<div class='alert alert-warning'>" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali.") + "</div>");
    return;
}
String rndPiutang = Common.getGeneratedBarCode(7);
%>
<div class="container-fluid py-4" id="pembantuPiutang<%=rndPiutang%>">
  <div class="d-flex flex-column flex-lg-row justify-content-between gap-3 mb-3">
    <div>
      <h4 class="fw-bolder text-danger mb-1"><i class="fas fa-book me-2"></i><%=Common.getBahasaConfig("Buku Besar Pembantu Piutang")%></h4>
      <div class="small text-muted"><%=Common.getBahasaConfig("Rekap piutang per pelanggan. Saldo akhir = saldo awal + faktur - pembayaran - retur - uang muka + jurnal umum.")%></div>
    </div>
    <div class="d-flex gap-2 flex-wrap">
      <button class="btn btn-outline-success rounded-pill" onclick="excelPembantuPiutang<%=rndPiutang%>()"><i class="fas fa-file-excel me-1"></i>Download Excel</button>
      <button class="btn btn-outline-secondary rounded-pill" onclick="pdfPembantuPiutang<%=rndPiutang%>()"><i class="fas fa-file-pdf me-1"></i>Cetak PDF</button>
    </div>
  </div>

  <div class="card border-0 shadow-sm rounded-4 mb-3">
    <div class="card-body">
      <div class="row g-2 align-items-end">
        <div class="col-lg-4"><label class="form-label small fw-bold">ID / Nama Pelanggan</label><input class="form-control" id="cariPiutang<%=rndPiutang%>" placeholder="Cari pelanggan..."></div>
        <div class="col-lg-3"><label class="form-label small fw-bold">Tanggal Mulai</label><input type="date" class="form-control" id="dariPiutang<%=rndPiutang%>"></div>
        <div class="col-lg-3"><label class="form-label small fw-bold">Tanggal Akhir</label><input type="date" class="form-control" id="sampaiPiutang<%=rndPiutang%>"></div>
        <div class="col-lg-2 d-grid"><button class="btn btn-danger" onclick="muatPembantuPiutang<%=rndPiutang%>(1)"><i class="fas fa-filter me-1"></i>Saring</button></div>
      </div>
    </div>
  </div>

  <div id="errorPiutang<%=rndPiutang%>" class="alert alert-danger d-none"></div>
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
    <div class="table-responsive">
      <table class="table table-hover table-bordered align-middle mb-0 small" id="tabelPiutang<%=rndPiutang%>" style="min-width:1200px">
        <thead class="table-primary"><tr>
          <th>ID Pelanggan</th><th>Nama Pelanggan</th>
          <th class="text-end">Saldo Awal</th><th class="text-end">Faktur</th>
          <th class="text-end">Pembayaran</th><th class="text-end">Retur</th>
          <th class="text-end">Uang Muka</th><th class="text-end">Jurnal Umum</th>
          <th class="text-end table-warning">Saldo Akhir</th>
        </tr></thead>
        <tbody id="bodyPiutang<%=rndPiutang%>"><tr><td colspan="9" class="text-center py-5"><span class="spinner-border text-danger"></span></td></tr></tbody>
        <tfoot id="footPiutang<%=rndPiutang%>" class="table-light fw-bold"></tfoot>
      </table>
    </div>
    <div class="card-footer bg-white d-flex justify-content-between align-items-center flex-wrap gap-2">
      <span class="small text-muted" id="infoPiutang<%=rndPiutang%>"></span>
      <div class="btn-group"><button class="btn btn-sm btn-outline-secondary" id="prevPiutang<%=rndPiutang%>" onclick="pindahPiutang<%=rndPiutang%>(-1)">Sebelumnya</button><button class="btn btn-sm btn-light disabled" id="pagePiutang<%=rndPiutang%>">1 / 1</button><button class="btn btn-sm btn-outline-secondary" id="nextPiutang<%=rndPiutang%>" onclick="pindahPiutang<%=rndPiutang%>(1)">Berikutnya</button></div>
    </div>
  </div>
</div>

<script>
(function(){
  let page = 1, totalPages = 1, semuaEkspor = [];
  const id = s => document.getElementById(s + '<%=rndPiutang%>');
  const rp = n => new Intl.NumberFormat('id-ID',{style:'currency',currency:'IDR',minimumFractionDigits:0}).format(Number(n)||0);
  const esc = v => String(v == null ? '' : v).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const tanggal = d => d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');
  const now = new Date(); id('dariPiutang').value=tanggal(new Date(now.getFullYear(),now.getMonth(),1)); id('sampaiPiutang').value=tanggal(now);

  async function api(pageNo, size){
    const response = await fetch('<%=Common.ROOT%>/Api_eBisnis',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({action:'pembantu_piutang_list',dari:id('dariPiutang').value,sampai:id('sampaiPiutang').value,q:id('cariPiutang').value.trim(),page:pageNo,page_size:size})});
    const json = await response.json();
    if (!response.ok || json.status !== 'success') throw new Error(json.message || json.description || 'Laporan belum dapat dimuat.');
    return json;
  }
  window['muatPembantuPiutang<%=rndPiutang%>'] = async function(p){
    page=p||1; id('errorPiutang').classList.add('d-none'); id('bodyPiutang').innerHTML='<tr><td colspan="9" class="text-center py-5"><span class="spinner-border text-danger"></span></td></tr>';
    try {
      const h=await api(page,15), rows=h.data||[], t=h.total||{}; totalPages=Math.max(1,Number(h.totalPages)||1);
      id('bodyPiutang').innerHTML=rows.length?rows.map(r=>'<tr><td>'+esc(r.kodeAnggota)+'</td><td class="fw-bold">'+esc(r.namaAnggota)+'</td><td class="text-end">'+rp(r.saldoAwal)+'</td><td class="text-end text-danger">'+rp(r.faktur)+'</td><td class="text-end text-success">'+rp(r.pembayaran)+'</td><td class="text-end">'+rp(r.retur)+'</td><td class="text-end">'+rp(r.uangMuka)+'</td><td class="text-end">'+rp(r.jurnalUmum)+'</td><td class="text-end table-warning fw-bold">'+rp(r.saldoAkhir)+'</td></tr>').join(''):'<tr><td colspan="9" class="text-center py-5 text-muted">Tidak ada data piutang pada filter ini.</td></tr>';
      id('footPiutang').innerHTML=rows.length?'<tr><td>TOTAL</td><td>'+Number(h.totalData||0)+' pelanggan</td><td class="text-end">'+rp(t.saldoAwal)+'</td><td class="text-end">'+rp(t.faktur)+'</td><td class="text-end">'+rp(t.pembayaran)+'</td><td class="text-end">'+rp(t.retur)+'</td><td class="text-end">'+rp(t.uangMuka)+'</td><td class="text-end">'+rp(t.jurnalUmum)+'</td><td class="text-end table-warning">'+rp(t.saldoAkhir)+'</td></tr>':'';
      id('infoPiutang').textContent='Menampilkan '+rows.length+' dari '+Number(h.totalData||0)+' pelanggan'; id('pagePiutang').textContent=page+' / '+totalPages; id('prevPiutang').disabled=page<=1; id('nextPiutang').disabled=page>=totalPages;
    } catch(e){ id('errorPiutang').textContent='Laporan belum dapat dimuat. '+e.message+' Silakan periksa periode lalu coba kembali.'; id('errorPiutang').classList.remove('d-none'); id('bodyPiutang').innerHTML=''; }
  };
  window['pindahPiutang<%=rndPiutang%>'] = d => { const p=page+d; if(p>=1&&p<=totalPages) window['muatPembantuPiutang<%=rndPiutang%>'](p); };
  async function ambilEkspor(){ const h=await api(1,5000); semuaEkspor=h.data||[]; return h; }
  window['excelPembantuPiutang<%=rndPiutang%>'] = async function(){
    try { const h=await ambilEkspor(); if(!semuaEkspor.length) throw new Error('Tidak ada data untuk diekspor.'); if(typeof XLSX==='undefined') throw new Error('Komponen XLSX belum tersedia.');
      const data=semuaEkspor.map(r=>({'ID Pelanggan':r.kodeAnggota,'Nama Pelanggan':r.namaAnggota,'Saldo Awal':r.saldoAwal,'Faktur':r.faktur,'Pembayaran':r.pembayaran,'Retur':r.retur,'Uang Muka':r.uangMuka,'Jurnal Umum':r.jurnalUmum,'Saldo Akhir':r.saldoAkhir}));
      data.push({'ID Pelanggan':'TOTAL','Nama Pelanggan':'','Saldo Awal':h.total.saldoAwal,'Faktur':h.total.faktur,'Pembayaran':h.total.pembayaran,'Retur':h.total.retur,'Uang Muka':h.total.uangMuka,'Jurnal Umum':h.total.jurnalUmum,'Saldo Akhir':h.total.saldoAkhir});
      const wb=XLSX.utils.book_new(),ws=XLSX.utils.json_to_sheet(data); XLSX.utils.book_append_sheet(wb,ws,'Pembantu Piutang'); XLSX.writeFile(wb,'Buku_Besar_Pembantu_Piutang_'+id('dariPiutang').value+'_'+id('sampaiPiutang').value+'.xlsx');
    } catch(e){ alert(e.message); }
  };
  window['pdfPembantuPiutang<%=rndPiutang%>'] = async function(){
    try { await ambilEkspor(); if(!semuaEkspor.length) throw new Error('Tidak ada data untuk dicetak.'); const w=window.open('','_blank');
      const rows=semuaEkspor.map(r=>'<tr><td>'+esc(r.kodeAnggota)+'</td><td>'+esc(r.namaAnggota)+'</td><td>'+rp(r.saldoAwal)+'</td><td>'+rp(r.faktur)+'</td><td>'+rp(r.pembayaran)+'</td><td>'+rp(r.retur)+'</td><td>'+rp(r.uangMuka)+'</td><td>'+rp(r.jurnalUmum)+'</td><td>'+rp(r.saldoAkhir)+'</td></tr>').join('');
      w.document.write('<html><head><title>Pembantu Piutang</title><style>@page{size:landscape;margin:10mm}body{font-family:Arial;font-size:9px}h2,p{text-align:center}table{width:100%;border-collapse:collapse}th,td{border:1px solid #555;padding:4px}td:nth-child(n+3){text-align:right}th{background:#cfe5fa}</style></head><body><h2>Buku Besar Pembantu Piutang</h2><p>'+id('dariPiutang').value+' s/d '+id('sampaiPiutang').value+'</p><table><thead><tr><th>ID Pelanggan</th><th>Nama Pelanggan</th><th>Saldo Awal</th><th>Faktur</th><th>Pembayaran</th><th>Retur</th><th>Uang Muka</th><th>Jurnal Umum</th><th>Saldo Akhir</th></tr></thead><tbody>'+rows+'</tbody></table></body></html>'); w.document.close(); w.focus(); setTimeout(()=>w.print(),300);
    } catch(e){ alert(e.message); }
  };
  id('cariPiutang').addEventListener('keydown',e=>{if(e.key==='Enter')window['muatPembantuPiutang<%=rndPiutang%>'](1)});
  window['muatPembantuPiutang<%=rndPiutang%>'](1);
})();
</script>
