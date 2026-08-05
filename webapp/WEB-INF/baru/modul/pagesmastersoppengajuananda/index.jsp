<%@page import="ais.common.Common"%>
<%--
  "Pengajuan Anda" (Workflow / Dasbor Proses SOP) — versi JSP (konversi dari DasboardSop ZK).
  Pengguna kantin mengajukan dokumen berbasis SOP (PR, PO, BAST, dst.) lewat tombol "Pengajuan Baru",
  dan memantau alurnya. Data via pengajuan_anda_service.jsp (reuse PengajuanAndaSopUtil).
  Tampilan lama (monitor_sop.jsp) tetap ada di folder ini sebagai referensi (tidak dipakai index).
--%>
<%
String rnd = Common.getGeneratedBarCode(7);
String svc = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=pagesmastersoppengajuananda&s=pengajuan_anda_service";
String zkAlur = Common.ROOT + "/pages/master/sop/tampilan_alur_sop.zul";
%>
<style>
.pa-wrap{--pa-ink:#0f172a;--pa-mut:#64748b;}
.pa-banner{background:linear-gradient(120deg,#1e3a8a,#0e7490);color:#fff;border-radius:18px;padding:20px 22px;position:relative;overflow:hidden;}
.pa-banner h3{font-weight:800;margin:0;}
.pa-chip{display:inline-block;background:rgba(255,255,255,.16);border-radius:999px;padding:3px 12px;font-size:.78rem;margin-right:6px;margin-top:6px;}
.pa-bnum{background:rgba(255,255,255,.12);border-radius:14px;padding:10px 16px;text-align:center;min-width:104px;}
.pa-bnum .n{font-size:1.6rem;font-weight:800;line-height:1;}
.pa-bnum .l{font-size:.72rem;opacity:.9;}
.pa-card{border:1px solid #eef2f7;border-radius:16px;background:#fff;box-shadow:0 1px 2px rgba(16,24,40,.04);height:100%;}
.pa-card .ic{width:40px;height:40px;border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:1.1rem;}
.pa-card .n{font-size:1.7rem;font-weight:800;line-height:1;}
.pa-card .t{font-weight:700;color:var(--pa-ink);}
.pa-card .d{font-size:.76rem;color:var(--pa-mut);}
.pa-list-item{border:1px solid #eef2f7;border-radius:12px;padding:10px 12px;margin-bottom:8px;}
.pa-list-item .kode{font-size:.74rem;color:var(--pa-mut);letter-spacing:.02em;}
.pa-list-item .judul{font-weight:700;color:var(--pa-ink);}
.pa-list-item .meta{font-size:.8rem;color:var(--pa-mut);}
@media (max-width:991px){ .pa-bnum{min-width:84px;} }
</style>

<div class="container-fluid px-0 pa-wrap" id="pa<%=rnd%>">
  <div class="mb-2">
    <h6 class="fw-bold mb-0"><i class="fas fa-diagram-project me-2 text-primary"></i><%=Common.getBahasaConfig("Workflow")%></h6>
    <div class="text-muted small"><%=Common.getBahasaConfig("Pantau alur pekerjaan, dokumen yang menunggu tindak lanjut, dan proses yang perlu segera diselesaikan.")%></div>
  </div>

  <!-- Banner -->
  <div class="pa-banner mb-3">
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-3">
      <div>
        <div class="text-uppercase small" style="letter-spacing:.12em;opacity:.85"><%=Common.getBahasaConfig("SOP Control Center")%></div>
        <h3><%=Common.getBahasaConfig("Dasbor Proses SOP")%></h3>
        <div class="small mt-1" style="max-width:560px;opacity:.92"><%=Common.getBahasaConfig("Pantau alur pengajuan, antrian disposisi, penyelesaian, dan risiko batas waktu dalam satu layar.")%></div>
        <div class="mt-2">
          <span class="pa-chip"><i class="fas fa-calendar me-1"></i><%=Common.getBahasaConfig("Periode: Semua Periode")%></span>
          <span class="pa-chip"><i class="fas fa-building me-1"></i><%=Common.getBahasaConfig("Semua Satker")%></span>
        </div>
      </div>
      <div class="d-flex gap-2">
        <div class="pa-bnum"><div class="n" id="paProses<%=rnd%>">-</div><div class="l"><%=Common.getBahasaConfig("Proses Dipantau")%></div></div>
        <div class="pa-bnum"><div class="n" id="paAntri<%=rnd%>">-</div><div class="l"><%=Common.getBahasaConfig("Total Antrian")%></div></div>
      </div>
    </div>
  </div>

  <!-- 6 kartu metrik -->
  <div class="row g-2 mb-3">
    <div class="col-6 col-lg-2"><div class="pa-card p-3"><div class="ic" style="background:#fee2e2;color:#b91c1c">!</div><div class="n mt-2 text-danger" id="paMenungguSaya<%=rnd%>">-</div><div class="t small"><%=Common.getBahasaConfig("Menunggu Saya")%></div><div class="d"><%=Common.getBahasaConfig("Butuh disposisi / tindak lanjut")%></div></div></div>
    <div class="col-6 col-lg-2"><div class="pa-card p-3"><div class="ic" style="background:#dcfce7;color:#166534">✓</div><div class="n mt-2 text-success" id="paSudah<%=rnd%>">-</div><div class="t small"><%=Common.getBahasaConfig("Sudah Disposisi")%></div><div class="d"><%=Common.getBahasaConfig("Riwayat proses oleh Anda")%></div></div></div>
    <div class="col-6 col-lg-2"><div class="pa-card p-3"><div class="ic" style="background:#dbeafe;color:#1e40af">+</div><div class="n mt-2 text-primary" id="paPengajuan<%=rnd%>">-</div><div class="t small"><%=Common.getBahasaConfig("Jumlah Pengajuan Anda")%></div><div class="d"><%=Common.getBahasaConfig("Pengajuan SOP yang Anda lakukan")%></div></div></div>
    <div class="col-6 col-lg-2"><div class="pa-card p-3"><div class="ic" style="background:#fef9c3;color:#854d0e">★</div><div class="n mt-2" style="color:#854d0e" id="paSelesai<%=rnd%>">-</div><div class="t small"><%=Common.getBahasaConfig("Selesai")%></div><div class="d"><%=Common.getBahasaConfig("Pengajuan selesai")%></div></div></div>
    <div class="col-6 col-lg-2"><div class="pa-card p-3"><div class="ic" style="background:#ede9fe;color:#5b21b6">→</div><div class="n mt-2" style="color:#5b21b6" id="paPetugas<%=rnd%>">-</div><div class="t small"><%=Common.getBahasaConfig("Menunggu Petugas")%></div><div class="d"><%=Common.getBahasaConfig("Proses sedang berjalan")%></div></div></div>
    <div class="col-6 col-lg-2"><div class="pa-card p-3"><div class="ic" style="background:#fee2e2;color:#b91c1c"><i class="fas fa-stopwatch"></i></div><div class="n mt-2 text-danger" id="paLewat<%=rnd%>">-</div><div class="t small"><%=Common.getBahasaConfig("Lewat Batas Waktu")%></div><div class="d"><%=Common.getBahasaConfig("Sudah melewati batas waktu")%></div></div></div>
  </div>

  <div class="mb-2">
    <h6 class="fw-bold mb-0"><%=Common.getBahasaConfig("Daftar Operasional SOP")%></h6>
    <div class="text-muted small"><%=Common.getBahasaConfig("Ajukan dokumen baru, dan pantau pengajuan Anda yang sedang berjalan maupun yang baru saja Anda proses.")%></div>
  </div>

  <div class="row g-3">
    <!-- Sedang diproses + Pengajuan Baru -->
    <div class="col-12 col-lg-6">
      <div class="card border-0 shadow-sm rounded-4 h-100">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-1">
            <strong><%=Common.getBahasaConfig("Pengajuan Anda yang sedang diproses")%></strong>
            <button class="btn btn-primary btn-sm" type="button" data-bs-toggle="modal" data-bs-target="#paModalSop<%=rnd%>"><i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Pengajuan Baru")%></button>
          </div>
          <div class="text-muted small mb-2"><%=Common.getBahasaConfig("Pengajuan SOP yang Anda ajukan dan masih berjalan.")%></div>
          <div class="input-group input-group-sm mb-2" style="max-width:280px"><span class="input-group-text"><i class="fas fa-search"></i></span><input type="text" id="paCariProses<%=rnd%>" class="form-control" placeholder="<%=Common.getBahasaConfig("cari...")%>" oninput="paFilterProses<%=rnd%>()"></div>
          <div id="paProsesBody<%=rnd%>"><div class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</div></div>
          <div id="paProsesPg<%=rnd%>" class="d-flex justify-content-center gap-2 mt-2"></div>
        </div>
      </div>
    </div>
    <!-- Baru saja disposisi -->
    <div class="col-12 col-lg-6">
      <div class="card border-0 shadow-sm rounded-4 h-100">
        <div class="card-body">
          <strong><%=Common.getBahasaConfig("Pengajuan yang baru saja Anda disposisi")%></strong>
          <div class="text-muted small mb-2"><%=Common.getBahasaConfig("SOP yang baru saja Anda tindak lanjuti atau disposisikan.")%></div>
          <div class="input-group input-group-sm mb-2" style="max-width:280px"><span class="input-group-text"><i class="fas fa-search"></i></span><input type="text" id="paCariDisp<%=rnd%>" class="form-control" placeholder="<%=Common.getBahasaConfig("cari...")%>" oninput="paFilterDisp<%=rnd%>()"></div>
          <div id="paDispBody<%=rnd%>"><div class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</div></div>
          <div id="paDispPg<%=rnd%>" class="d-flex justify-content-center gap-2 mt-2"></div>
        </div>
      </div>
    </div>
  </div>
</div>

<!-- Modal Pengajuan Baru: pilih SOP -->
<div class="modal fade" id="paModalSop<%=rnd%>" tabindex="-1"><div class="modal-dialog modal-lg modal-dialog-scrollable"><div class="modal-content rounded-4">
  <div class="modal-header"><h6 class="modal-title fw-bold"><i class="fas fa-paper-plane me-2 text-primary"></i><%=Common.getBahasaConfig("Pengajuan Baru — pilih SOP")%></h6><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
  <div class="modal-body">
    <div class="text-muted small mb-2"><%=Common.getBahasaConfig("Pilih SOP yang ingin Anda ajukan (mis. PR, PO, BAST). Anda akan diarahkan ke formulir alur SOP.")%></div>
    <div class="input-group input-group-sm mb-2"><span class="input-group-text"><i class="fas fa-search"></i></span><input type="text" id="paCariSop<%=rnd%>" class="form-control" placeholder="<%=Common.getBahasaConfig("cari SOP...")%>" oninput="paLoadSop<%=rnd%>()"></div>
    <div class="table-responsive"><table class="table table-hover table-sm align-middle">
      <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Jenis")%></th><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Nama SOP")%></th><th class="text-end"><%=Common.getBahasaConfig("Aksi")%></th></tr></thead>
      <tbody id="paSopBody<%=rnd%>"><tr><td colspan="4" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr></tbody>
    </table></div>
  </div>
</div></div></div>

<script>
(function(){
  var SVC='<%=svc%>', ZK='<%=zkAlur%>', rnd='<%=rnd%>';
  var prosesAll=[], dispAll=[];
  function $(id){ return document.getElementById(id+rnd); }
  function esc(s){ return (s==null?'':String(s)).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];}); }
  async function post(p){ var r=await fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},body:new URLSearchParams(p)}); return JSON.parse((await r.text()).trim()); }
  function openAlur(){ window.open(ZK, '_blank'); }
  window['paOpenAlur'+rnd]=openAlur;
  function badge(st){ return st==='selesai'?'<span class="badge bg-success">Selesai</span>':(st==='disetujui'?'<span class="badge bg-info text-dark">Disetujui</span>':'<span class="badge bg-warning text-dark">Menunggu</span>'); }

  async function loadDashboard(){
    try{ var r=await post({aksi:'dashboard'});
      if(r.status!=='00') return;
      $('paProses').textContent=r.prosesDipantau||0; $('paAntri').textContent=r.totalAntrian||0;
      $('paMenungguSaya').textContent=r.menungguSaya||0; $('paSudah').textContent=r.sudahDisposisi||0;
      $('paPengajuan').textContent=r.pengajuanAnda||0; $('paSelesai').textContent=r.selesai||0;
      $('paPetugas').textContent=r.menungguPetugas||0; $('paLewat').textContent=r.lewatBatas||0;
    }catch(e){}
  }

  function renderProses(list){
    var b=$('paProsesBody');
    if(!list.length){ b.innerHTML='<div class="text-center text-muted py-4">Belum ada pengajuan berjalan.</div>'; return; }
    b.innerHTML=list.map(function(p){
      return '<div class="pa-list-item"><div class="d-flex justify-content-between"><div class="kode">'+esc(p.kode)+'</div>'+badge(p.status)+'</div>'
        +'<div class="judul">'+esc(p.sop)+'</div>'
        +'<div class="meta">'+(p.tahap?('Tahap: '+esc(p.tahap)+' • '):'')+esc(p.waktu)+'</div>'
        +(p.keterangan?'<div class="meta">Catatan: '+esc(p.keterangan)+'</div>':'')
        +'<div class="mt-1"><button class="btn btn-sm btn-outline-secondary" onclick="paOpenAlur'+rnd+'()"><i class="fas fa-eye me-1"></i>Lihat / Proses</button></div></div>';
    }).join('');
  }
  function renderDisp(list){
    var b=$('paDispBody');
    if(!list.length){ b.innerHTML='<div class="text-center text-muted py-4">Belum ada yang Anda disposisi.</div>'; return; }
    b.innerHTML=list.map(function(p){
      return '<div class="pa-list-item"><div class="kode">'+esc(p.kode)+'</div>'
        +'<div class="judul">'+esc(p.sop)+'</div>'
        +'<div class="meta">'+(p.tahap?('Tahap: '+esc(p.tahap)+' • '):'')+esc(p.waktu)+'</div>'
        +(p.keterangan?'<div class="meta">Catatan: '+esc(p.keterangan)+'</div>':'')
        +'<div class="mt-1"><button class="btn btn-sm btn-outline-secondary" onclick="paOpenAlur'+rnd+'()"><i class="fas fa-eye me-1"></i>Lihat</button></div></div>';
    }).join('');
  }
  function pager(el, total, totalPage, page, fn){
    if(totalPage<=1){ el.innerHTML=''; return; }
    var h='<button class="btn btn-sm btn-light" '+(page<=0?'disabled':'')+' onclick="'+fn+'('+(page-1)+')">‹</button>'
      +'<span class="small text-muted align-self-center">Hal '+(page+1)+'/'+totalPage+'</span>'
      +'<button class="btn btn-sm btn-light" '+(page+1>=totalPage?'disabled':'')+' onclick="'+fn+'('+(page+1)+')">›</button>';
    el.innerHTML=h;
  }

  window['paLoadProses'+rnd]=async function(page){
    try{ var r=await post({aksi:'listProses', page:page||0});
      if(r.status!=='00'){ $('paProsesBody').innerHTML='<div class="text-danger small py-3">'+esc(r.message||'Gagal')+'</div>'; return; }
      prosesAll=r.data||[]; renderProses(prosesAll);
      pager($('paProsesPg'), r.total, r.totalPage, r.page, "window.paLoadProses"+rnd);
    }catch(e){ $('paProsesBody').innerHTML='<div class="text-danger small py-3">Error: '+esc(e.message)+'</div>'; }
  };
  window['paLoadDisp'+rnd]=async function(page){
    try{ var r=await post({aksi:'listDisposisi', page:page||0});
      if(r.status!=='00'){ $('paDispBody').innerHTML='<div class="text-danger small py-3">'+esc(r.message||'Gagal')+'</div>'; return; }
      dispAll=r.data||[]; renderDisp(dispAll);
      pager($('paDispPg'), r.total, r.totalPage, r.page, "window.paLoadDisp"+rnd);
    }catch(e){ $('paDispBody').innerHTML='<div class="text-danger small py-3">Error: '+esc(e.message)+'</div>'; }
  };
  window['paFilterProses'+rnd]=function(){ var q=$('paCariProses').value.toLowerCase(); renderProses(prosesAll.filter(function(p){return (p.sop+' '+p.kode+' '+p.keterangan).toLowerCase().indexOf(q)>=0;})); };
  window['paFilterDisp'+rnd]=function(){ var q=$('paCariDisp').value.toLowerCase(); renderDisp(dispAll.filter(function(p){return (p.sop+' '+p.kode+' '+p.keterangan).toLowerCase().indexOf(q)>=0;})); };

  window['paLoadSop'+rnd]=async function(){
    var tb=$('paSopBody'); tb.innerHTML='<tr><td colspan="4" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr>';
    try{ var r=await post({aksi:'listSop', q:$('paCariSop').value});
      if(r.status!=='00'||!r.data||!r.data.length){ tb.innerHTML='<tr><td colspan="4" class="text-center text-muted py-4">'+(r.message||'Tidak ada SOP.')+'</td></tr>'; return; }
      tb.innerHTML=r.data.map(function(p){
        return '<tr><td><span class="badge bg-light text-dark border">'+esc(p.jenis)+'</span></td><td class="fw-semibold">'+esc(p.kode)+'</td><td>'+esc(p.nama)+'</td><td class="text-end"><button class="btn btn-sm btn-primary" onclick="paOpenAlur'+rnd+'()"><i class="fas fa-paper-plane me-1"></i>Ajukan</button></td></tr>';
      }).join('');
    }catch(e){ tb.innerHTML='<tr><td colspan="4" class="text-center text-danger py-4">Error: '+esc(e.message)+'</td></tr>'; }
  };

  loadDashboard(); window['paLoadProses'+rnd](0); window['paLoadDisp'+rnd](0);
  var modalEl=document.getElementById('paModalSop'+rnd);
  if(modalEl){ modalEl.addEventListener('shown.bs.modal', function(){ window['paLoadSop'+rnd](); }); }
})();
</script>
