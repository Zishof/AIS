<%@page import="ais.common.Common"%>
<%
String rndPs = Common.getGeneratedBarCode(6);
String svcPs = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=pagesmastersoptampilanalursopzul&s=pengajuan_sop_service";
String uplPs = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=pagesmastersoptampilanalursopzul&s=upload_dokumen_service";
String zkAlur = Common.ROOT + "/pages/master/sop/tampilan_alur_sop.zul";
String rootPs = Common.ROOT;
%>

<div class="container-fluid px-0">
  <h5 class="fw-bold mb-1"><i class="fas fa-diagram-project me-2 text-primary"></i><%=Common.getBahasaConfig("Pengajuan SOP")%></h5>
  <p class="text-muted small mb-3"><%=Common.getBahasaConfig("Ajukan dokumen melalui alur SOP (Disposisi). Pilih SOP untuk membuat pengajuan baru, atau pantau pengajuan Anda di bawah.")%></p>

  <div class="row g-2 mb-3" id="psCards<%=rndPs%>">
    <div class="col-6 col-md-3"><div class="card border-0 shadow-sm rounded-4"><div class="card-body py-3"><div class="text-muted small"><%=Common.getBahasaConfig("Total Pengajuan")%></div><div class="fs-4 fw-bold" id="psCTotal<%=rndPs%>">-</div></div></div></div>
    <div class="col-6 col-md-3"><div class="card border-0 shadow-sm rounded-4"><div class="card-body py-3"><div class="text-muted small"><%=Common.getBahasaConfig("Menunggu")%></div><div class="fs-4 fw-bold text-warning" id="psCMenunggu<%=rndPs%>">-</div></div></div></div>
    <div class="col-6 col-md-3"><div class="card border-0 shadow-sm rounded-4"><div class="card-body py-3"><div class="text-muted small"><%=Common.getBahasaConfig("Disetujui")%></div><div class="fs-4 fw-bold text-info" id="psCDisetujui<%=rndPs%>">-</div></div></div></div>
    <div class="col-6 col-md-3"><div class="card border-0 shadow-sm rounded-4"><div class="card-body py-3"><div class="text-muted small"><%=Common.getBahasaConfig("Selesai")%></div><div class="fs-4 fw-bold text-success" id="psCSelesai<%=rndPs%>">-</div></div></div></div>
  </div>

  <ul class="nav nav-pills bg-white p-2 rounded-4 shadow-sm border mb-3 flex-wrap gap-1" role="tablist">
    <li class="nav-item"><button class="nav-link active rounded-pill fw-bold" data-bs-toggle="pill" data-bs-target="#psPaneBaru<%=rndPs%>" type="button"><i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Pengajuan Baru")%></button></li>
    <li class="nav-item"><button class="nav-link rounded-pill fw-bold" data-bs-toggle="pill" data-bs-target="#psPaneSaya<%=rndPs%>" type="button"><i class="fas fa-folder-open me-2"></i><%=Common.getBahasaConfig("Pengajuan Saya")%></button></li>
    <li class="nav-item"><button class="nav-link rounded-pill fw-bold" data-bs-toggle="pill" data-bs-target="#psPaneMenunggu<%=rndPs%>" type="button" onclick="setTimeout(function(){window['psLoadMenunggu<%=rndPs%>']&&window['psLoadMenunggu<%=rndPs%>']();},50)"><i class="fas fa-check-double me-2"></i><%=Common.getBahasaConfig("Setujui Massal")%></button></li>
  </ul>

  <div class="tab-content">
    <!-- Pengajuan Baru -->
    <div class="tab-pane fade show active" id="psPaneBaru<%=rndPs%>" role="tabpanel">
      <div class="input-group input-group-sm mb-2" style="max-width:420px"><span class="input-group-text"><i class="fas fa-search"></i></span><input type="text" id="psCariSop<%=rndPs%>" class="form-control" placeholder="<%=Common.getBahasaConfig("cari SOP...")%>" oninput="psLoadSop<%=rndPs%>()"></div>
      <div class="table-responsive">
        <table class="table table-hover align-middle">
          <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Jenis SOP")%></th><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Nama SOP")%></th><th><%=Common.getBahasaConfig("Versi")%></th><th class="text-end"><%=Common.getBahasaConfig("Aksi")%></th></tr></thead>
          <tbody id="psSopBody<%=rndPs%>"><tr><td colspan="5" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr></tbody>
        </table>
      </div>
    </div>
    <!-- Pengajuan Saya -->
    <div class="tab-pane fade" id="psPaneSaya<%=rndPs%>" role="tabpanel">
      <div class="d-flex gap-2 mb-2" style="max-width:320px">
        <select id="psFilterStatus<%=rndPs%>" class="form-select form-select-sm" onchange="psLoadPengajuan<%=rndPs%>()">
          <option value="semua"><%=Common.getBahasaConfig("Semua Status")%></option>
          <option value="menunggu"><%=Common.getBahasaConfig("Menunggu")%></option>
          <option value="disetujui"><%=Common.getBahasaConfig("Disetujui")%></option>
          <option value="selesai"><%=Common.getBahasaConfig("Selesai")%></option>
        </select>
      </div>
      <div class="table-responsive">
        <table class="table table-hover align-middle">
          <thead class="table-light"><tr><th>#</th><th><%=Common.getBahasaConfig("SOP")%></th><th><%=Common.getBahasaConfig("Jenis")%></th><th><%=Common.getBahasaConfig("Waktu")%></th><th class="text-center"><%=Common.getBahasaConfig("Status")%></th><th class="text-end"><%=Common.getBahasaConfig("Aksi")%></th></tr></thead>
          <tbody id="psSayaBody<%=rndPs%>"><tr><td colspan="6" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr></tbody>
        </table>
      </div>
    </div>
    <!-- Setujui Massal -->
    <div class="tab-pane fade" id="psPaneMenunggu<%=rndPs%>" role="tabpanel">
      <div class="d-flex flex-wrap gap-2 mb-2 align-items-center">
        <button class="btn btn-sm btn-outline-secondary" onclick="window['psLoadMenunggu<%=rndPs%>']()"><i class="fas fa-sync me-1"></i><%=Common.getBahasaConfig("Muat Ulang")%></button>
        <input type="text" id="psKetBulk<%=rndPs%>" class="form-control form-control-sm" style="max-width:320px" placeholder="<%=Common.getBahasaConfig("Catatan (opsional, dipakai untuk semua)")%>">
        <button class="btn btn-sm btn-success" onclick="window['psSetujuiTerpilih<%=rndPs%>']()"><i class="fas fa-check me-1"></i><%=Common.getBahasaConfig("Setujui Terpilih")%></button>
        <button class="btn btn-sm btn-primary" onclick="window['psSetujuiSemua<%=rndPs%>']()"><i class="fas fa-check-double me-1"></i><%=Common.getBahasaConfig("Setujui Semua")%></button>
      </div>
      <div class="alert alert-info py-2 small mb-2"><%=Common.getBahasaConfig("Menyetujui pengajuan yang menunggu disposisi Anda sekaligus. Item bertanda \"perlu manual\" (harus pilih rute lanjutan / catatan wajib) tidak ikut disetujui massal, silakan proses satu per satu.")%></div>
      <div class="table-responsive">
        <table class="table table-hover align-middle">
          <thead class="table-light"><tr><th style="width:38px" class="text-center"><input type="checkbox" id="psChkAll<%=rndPs%>" onclick="window['psToggleAll<%=rndPs%>'](this)"></th><th><%=Common.getBahasaConfig("SOP")%></th><th><%=Common.getBahasaConfig("Langkah")%></th><th><%=Common.getBahasaConfig("Catatan")%></th><th><%=Common.getBahasaConfig("Waktu")%></th></tr></thead>
          <tbody id="psMenungguBody<%=rndPs%>"><tr><td colspan="5" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Klik tab ini untuk memuat.")%></td></tr></tbody>
        </table>
      </div>
    </div>
  </div>
</div>

<style>
  /* Timeline alur disposisi (native JSP) */
  .ps-tl<%=rndPs%>{ position:relative; padding-left:34px; }
  .ps-tl<%=rndPs%>:before{ content:''; position:absolute; left:14px; top:4px; bottom:4px; width:2px; background:#e3e7ef; }
  .ps-tl<%=rndPs%> .ps-step{ position:relative; margin-bottom:14px; }
  .ps-tl<%=rndPs%> .ps-dot{ position:absolute; left:-27px; top:6px; width:18px; height:18px; border-radius:50%; background:#fff; border:3px solid #adb5bd; }
  .ps-tl<%=rndPs%> .ps-step.done .ps-dot{ border-color:#198754; background:#198754; }
  .ps-tl<%=rndPs%> .ps-step.now .ps-dot{ border-color:#fd7e14; background:#fff; box-shadow:0 0 0 4px rgba(253,126,20,.18); }
  .ps-tl<%=rndPs%> .ps-card{ border:1px solid #e6e9f0; border-radius:14px; padding:10px 14px; background:#fff; }
  .ps-tl<%=rndPs%> .ps-step.now .ps-card{ border-color:#fd7e14; background:#fff8f1; }
  .ps-tl<%=rndPs%> .ps-card .ps-judul{ font-weight:700; }
  .ps-flag-open{ color:#0a7d33; }
  .ps-flag-lock{ color:#6c757d; }
</style>

<!-- Modal: Alur Disposisi SOP (tampilan native JSP) -->
<div class="modal fade" id="psAlurModal<%=rndPs%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-dialog-scrollable modal-dialog-centered">
    <div class="modal-content rounded-4 border-0 shadow">
      <div class="modal-header bg-primary text-white">
        <h6 class="modal-title fw-bold"><i class="fas fa-diagram-project me-2"></i><%=Common.getBahasaConfig("Alur Disposisi SOP")%></h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body" id="psAlurBody<%=rndPs%>"></div>
      <div class="modal-footer">
        <button type="button" class="btn btn-light rounded-pill" data-bs-dismiss="modal"><i class="fas fa-times me-1"></i><%=Common.getBahasaConfig("Tutup")%></button>
      </div>
    </div>
  </div>
</div>

<!-- Modal: Form Proses Disposisi (input native JSP) -->
<div class="modal fade" id="psFormModal<%=rndPs%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
    <div class="modal-content rounded-4 border-0 shadow">
      <div class="modal-header bg-warning">
        <h6 class="modal-title fw-bold"><i class="fas fa-pen me-2"></i><%=Common.getBahasaConfig("Proses Disposisi")%></h6>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body" id="psFormBody<%=rndPs%>"></div>
      <div class="modal-footer">
        <button type="button" class="btn btn-light rounded-pill" data-bs-dismiss="modal"><i class="fas fa-times me-1"></i><%=Common.getBahasaConfig("Batal")%></button>
        <button type="button" class="btn btn-primary rounded-pill" id="psFormSimpan<%=rndPs%>" onclick="psSimpan<%=rndPs%>()"><i class="fas fa-save me-1"></i><%=Common.getBahasaConfig("Simpan")%></button>
      </div>
    </div>
  </div>
</div>

<script>
(function(){
  var SVC='<%=svcPs%>', UPL='<%=uplPs%>', ZK='<%=zkAlur%>', ROOT='<%=rootPs%>', rnd='<%=rndPs%>';
  var curDsId=null;   // id pengajuan yg sedang dibuka (untuk reload timeline setelah proses)
  var curForm={};     // konteks form proses yg sedang dibuka
  function $(id){ return document.getElementById(id+rnd); }
  function esc(s){ return (s==null?'':String(s)).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];}); }
  async function post(params){ var res=await fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},body:new URLSearchParams(params)}); return JSON.parse((await res.text()).trim()); }
  function bukaAlur(){ window.open(ZK, '_blank'); }
  window['psBukaAlur'+rnd]=bukaAlur;

  // ---- Modal timeline alur disposisi (native JSP) ----
  function showModal(){ var m=document.getElementById('psAlurModal'+rnd); if(window.bootstrap&&bootstrap.Modal){ bootstrap.Modal.getOrCreateInstance(m).show(); } else { m.style.display='block'; m.classList.add('show'); } }
  function badgeStatus(st){
    if(st==='selesai') return '<span class="badge bg-success">Selesai</span>';
    if(st==='disetujui') return '<span class="badge bg-info text-dark">Disetujui</span>';
    return '<span class="badge bg-warning text-dark">Menunggu</span>';
  }
  function stepCard(s){
    var cls = s.selesai ? 'done' : (s.isUjung ? 'now' : '');
    var statusKecil = s.selesai
        ? '<span class="badge bg-success-subtle text-success border border-success-subtle">Selesai</span>'
        : (s.isUjung ? '<span class="badge bg-warning text-dark">Menunggu disposisi</span>' : '<span class="badge bg-light text-muted border">Diteruskan</span>');
    // Flag aturan Proses SOP (rute berikutnya)
    var flagProses = s.bolehEditProses
        ? '<div class="small mt-1 ps-flag-open"><i class="fas fa-unlock me-1"></i>Proses SOP (rute berikutnya) dapat ditentukan pada langkah ini.</div>'
        : '<div class="small mt-1 ps-flag-lock"><i class="fas fa-lock me-1"></i>Rute sudah diteruskan ke langkah berikutnya (Proses SOP terkunci).</div>';
    var info='';
    if(s.oleh) info += '<span class="me-3"><i class="fas fa-user me-1 text-muted"></i>'+esc(s.oleh)+'</span>';
    if(s.waktu) info += '<span class="me-3"><i class="fas fa-clock me-1 text-muted"></i>'+esc(s.waktu)+'</span>';
    if(s.batasWaktu) info += '<span class="me-3 text-danger"><i class="fas fa-hourglass-half me-1"></i>Batas: '+esc(s.batasWaktu)+'</span>';
    var catatan = s.catatan ? '<div class="small mt-1"><span class="text-muted">Catatan:</span> '+esc(s.catatan)+'</div>' : '';
    // Dokumen lampiran langkah (tampil + tombol unduh)
    var dokHtml='';
    var docs = s.dokumen||[];
    if(docs.length){
      dokHtml += '<div class="small mt-2"><span class="text-muted">Dokumen:</span><ul class="list-unstyled mb-0 mt-1 ms-1">';
      docs.forEach(function(d){
        var inner;
        if(d.ada){
          var url=ROOT+'/AmbilLampiranLain?ref='+d.ref+'&jenis='+encodeURIComponent(d.jenis);
          inner='<i class="fas fa-file-alt text-primary me-1"></i>'+esc(d.nama)+' <a href="'+url+'" target="_blank" class="ms-1 text-decoration-none"><i class="fas fa-download me-1"></i>Unduh</a>';
        } else {
          inner='<i class="fas fa-file me-1 text-muted"></i>'+esc(d.nama)+(d.wajib?' <span class="badge bg-light text-danger border border-danger-subtle">wajib</span>':'')+' <span class="fst-italic text-muted">— belum diunggah</span>';
        }
        if(s.bolehUpload){
          inner += ' <label class="ms-1" style="cursor:pointer;font-size:.78rem;color:#0d6efd"><i class="fas fa-upload me-1"></i>'+(d.ada?'Ganti':'Unggah')+'<input type="file" hidden onchange="psUploadDok'+rnd+'('+d.ref+',\''+esc(d.jenis)+'\',this)"></label>';
        }
        dokHtml += '<li class="mb-1">'+inner+'</li>';
      });
      dokHtml += '</ul></div>';
    }
    // Tombol aksi (Fase-1: membuka form ZK; Fase-2 akan menjadi form native)
    var aksi='';
    if(s.bolehUbah){
      var label = s.bolehEditProses ? 'Proses / Disposisi' : 'Ubah Catatan';
      aksi += '<button class="btn btn-sm '+(s.bolehEditProses?'btn-warning':'btn-outline-secondary')+' rounded-pill me-1" onclick="psProsesStep'+rnd+'('+s.id+')"><i class="fas fa-pen me-1"></i>'+label+'</button>';
    }
    return '<div class="ps-step '+cls+'">'
      + '<span class="ps-dot"></span>'
      + '<div class="ps-card">'
      +   '<div class="d-flex justify-content-between align-items-start gap-2">'
      +     '<div class="ps-judul"><span class="badge bg-secondary me-1">'+s.urut+'</span>'+esc(s.namaAlur)+(s.aktor?' <span class="text-muted fw-normal small">('+esc(s.aktor)+')</span>':'')+'</div>'
      +     '<div>'+statusKecil+'</div>'
      +   '</div>'
      +   (info?'<div class="small text-muted mt-1">'+info+'</div>':'')
      +   catatan
      +   dokHtml
      +   flagProses
      +   (aksi?'<div class="mt-2">'+aksi+'</div>':'')
      + '</div></div>';
  }
  function renderTimeline(r){
    var h=r.head||{}, steps=r.steps||[];
    var html='';
    html += '<div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-2">';
    html += '  <div><div class="fw-bold fs-6">'+esc(h.sop||'-')+'</div>';
    html += '    <div class="small text-muted">'+(h.kode?'<span class="me-2">Kode: '+esc(h.kode)+'</span>':'')+(h.jenis?'<span class="badge bg-light text-dark border">'+esc(h.jenis)+'</span>':'')+'</div>';
    html += '    <div class="small text-muted mt-1"><i class="fas fa-user me-1"></i>Pengaju: '+esc(h.diajukanOleh||'-')+(h.waktu?' &middot; '+esc(h.waktu):'')+'</div>';
    html += '  </div>';
    html += '  <div>'+badgeStatus(h.status)+'</div>';
    html += '</div>';
    if(h.keterangan) html += '<div class="small mb-2"><span class="text-muted">Keterangan:</span> '+esc(h.keterangan)+'</div>';
    if(h.isAdmin) html += '<div class="alert alert-info py-2 px-3 small mb-3"><i class="fas fa-user-shield me-1"></i><b>Mode Admin.</b> Anda dapat mengubah dokumen &amp; keterangan di semua langkah. <b>Proses SOP</b> (rute berikutnya) hanya dapat diubah pada langkah yang rutenya belum diteruskan.</div>';
    if(!steps.length){ html += '<div class="text-center text-muted py-4">Belum ada langkah disposisi.</div>'; return html; }
    html += '<div class="ps-tl'+rnd+'">';
    steps.forEach(function(s){ html += stepCard(s); });
    html += '</div>';
    return html;
  }
  window['psLihatAlur'+rnd]=async function(id){
    curDsId=id;
    var body=document.getElementById('psAlurBody'+rnd);
    body.innerHTML='<div class="text-center text-muted py-5"><div class="spinner-border"></div><div class="mt-2">Memuat alur disposisi...</div></div>';
    showModal();
    try{ var r=await post({aksi:'timeline', id:id});
      if(r.status!=='00'){ body.innerHTML='<div class="alert alert-danger">'+esc(r.message||'Gagal memuat alur')+'</div>'; return; }
      body.innerHTML=renderTimeline(r);
    }catch(e){ body.innerHTML='<div class="alert alert-danger">Error: '+esc(e.message)+'</div>'; }
  };

  // ---- Upload dokumen lampiran (native multipart) ----
  window['psUploadDok'+rnd]=function(ref, jenis, inputEl){
    if(!inputEl.files || !inputEl.files.length) return;
    var li=inputEl.closest('li'); if(li) li.style.opacity='.5';
    var fd=new FormData(); fd.append('ref',ref); fd.append('jenis',jenis); fd.append('file', inputEl.files[0]);
    fetch(UPL, {method:'POST', body:fd}).then(function(res){return res.text();}).then(function(t){
      var r; try{ r=JSON.parse(t.trim()); }catch(e){ alert('<%= Common.getBahasaConfigJS("Mohon maaf, respons dari peladen tidak valid.") %>'); if(li) li.style.opacity=''; return; }
      if(r.status!=='00'){ alert(r.message||'<%= Common.getBahasaConfigJS("Mohon maaf, dokumen gagal diunggah. Silakan coba lagi.") %>'); if(li) li.style.opacity=''; return; }
      if(curDsId) window['psLihatAlur'+rnd](curDsId);
    }).catch(function(e){ alert('Error: '+e.message); if(li) li.style.opacity=''; });
  };

  // ---- Form proses disposisi (input native JSP) ----
  function showFormModal(){ var m=document.getElementById('psFormModal'+rnd); if(window.bootstrap&&bootstrap.Modal){ bootstrap.Modal.getOrCreateInstance(m).show(); } else { m.style.display='block'; m.classList.add('show'); } }
  function hideFormModal(){ var m=document.getElementById('psFormModal'+rnd); if(window.bootstrap&&bootstrap.Modal){ var b=bootstrap.Modal.getInstance(m); if(b) b.hide(); } else { m.style.display='none'; m.classList.remove('show'); } }

  window['psProsesStep'+rnd]=async function(stepId){
    var body=document.getElementById('psFormBody'+rnd);
    body.innerHTML='<div class="text-center text-muted py-4"><div class="spinner-border"></div><div class="mt-2">Memuat...</div></div>';
    showFormModal();
    try{ var r=await post({aksi:'opsiProses', stepId:stepId});
      if(r.status!=='00'){ body.innerHTML='<div class="alert alert-danger">'+esc(r.message||'Gagal')+'</div>'; return; }
      curForm=r;
      body.innerHTML=renderForm(r);
    }catch(e){ body.innerHTML='<div class="alert alert-danger">Error: '+esc(e.message)+'</div>'; }
  };

  // ---- Pengajuan Baru (native utk SOP tanpa form data; SOP ber-form -> ZK) ----
  window['psAjukan'+rnd]=async function(sopId){
    var body=document.getElementById('psFormBody'+rnd);
    body.innerHTML='<div class="text-center text-muted py-4"><div class="spinner-border"></div><div class="mt-2">Memuat...</div></div>';
    showFormModal();
    try{ var r=await post({aksi:'opsiPengajuanBaru', sopId:sopId});
      if(r.status!=='00'){ body.innerHTML='<div class="alert alert-danger">'+esc(r.message||'Gagal')+'</div>'; return; }
      if(r.adaForm){ hideFormModal(); window.open(ZK,'_blank'); return; } // SOP ber-form data -> form ZK bespoke
      r.modeBaru=true; curForm=r;
      body.innerHTML=renderFormBaru(r);
    }catch(e){ body.innerHTML='<div class="alert alert-danger">Error: '+esc(e.message)+'</div>'; }
  };
  function renderFormBaru(r){
    var html='';
    html+='<div class="mb-2"><span class="badge bg-primary">Pengajuan Baru</span> <b>'+esc(r.sopNama)+'</b></div>';
    html+='<div class="mb-3"><label class="form-label small fw-semibold">Keterangan'+(r.catatanWajib?' <span class="text-danger">*</span>':'')+'</label>';
    html+='<textarea class="form-control" id="psFK'+rnd+'" rows="3"></textarea></div>';
    var opsi=r.opsiRute||[];
    if(opsi.length){
      var tipe=r.berupaPilihan?'checkbox':'radio';
      html+='<div class="mb-3"><label class="form-label small fw-semibold">Proses SOP — Alur Disposisi Berikutnya'+(r.ruteOpsional?'':' <span class="text-danger">*</span>')+'</label><div class="border rounded-3 p-2">';
      opsi.forEach(function(o){
        var idc='psR'+rnd+'_'+o.id;
        html+='<div class="form-check"><input class="form-check-input ps-rute'+rnd+'" type="'+tipe+'" name="psRute'+rnd+'" value="'+o.id+'" id="'+idc+'"><label class="form-check-label" for="'+idc+'">'+esc(o.nama)+'</label></div>';
      });
      html+='</div></div>';
    }
    return html;
  }

  function renderForm(r){
    var html='';
    html+='<div class="mb-2"><span class="badge bg-secondary">Langkah</span> <b>'+esc(r.namaAlur)+'</b></div>';
    html+='<div class="mb-3"><label class="form-label small fw-semibold">Catatan / Keterangan'+(r.catatanWajib?' <span class="text-danger">*</span>':'')+'</label>';
    html+='<textarea class="form-control" id="psFK'+rnd+'" rows="3">'+esc(r.keterangan||'')+'</textarea></div>';
    if(r.bolehEditProses){
      var opsi=r.opsiRute||[];
      if(opsi.length){
        var pre=r.preselected||[];
        var isPre=function(id){ for(var i=0;i<pre.length;i++){ if(pre[i]==id) return true; } return false; };
        var tipe=r.berupaPilihan?'checkbox':'radio';
        html+='<div class="mb-3"><label class="form-label small fw-semibold">Proses SOP — Alur Disposisi Berikutnya'+(r.ruteOpsional?'':' <span class="text-danger">*</span>')+'</label>';
        html+='<div class="border rounded-3 p-2">';
        opsi.forEach(function(o){
          var idc='psR'+rnd+'_'+o.id;
          html+='<div class="form-check"><input class="form-check-input ps-rute'+rnd+'" type="'+tipe+'" name="psRute'+rnd+'" value="'+o.id+'" id="'+idc+'"'+(isPre(o.id)?' checked':'')+'><label class="form-check-label" for="'+idc+'">'+esc(o.nama)+'</label></div>';
        });
        html+='</div></div>';
      }
      html+='<div class="form-check form-switch mb-2"><input class="form-check-input" type="checkbox" id="psFsetujui'+rnd+'"><label class="form-check-label" for="psFsetujui'+rnd+'">Tandai disetujui / selesai pada langkah ini</label></div>';
      if(r.bisaKembali){
        html+='<div class="form-check form-switch mb-2"><input class="form-check-input" type="checkbox" id="psFkembali'+rnd+'"><label class="form-check-label" for="psFkembali'+rnd+'">Kembalikan ke langkah sebelumnya (revisi)</label></div>';
      }
    } else {
      html+='<div class="alert alert-secondary py-2 small mb-0"><i class="fas fa-lock me-1"></i>Rute sudah diteruskan ke langkah berikutnya. '+(r.isAdmin?'Sebagai admin, Anda hanya dapat menyunting catatan pada langkah ini (rute tidak berubah).':'Proses SOP terkunci.')+'</div>';
    }
    return html;
  }

  window['psSimpan'+rnd]=async function(){
    var r=curForm; if(!r) return;
    var btn=document.getElementById('psFormSimpan'+rnd); var old=btn.innerHTML;
    var ket=document.getElementById('psFK'+rnd).value;
    var sel, payload;
    if(r.modeBaru){
      // Pengajuan Baru native
      if(r.catatanWajib && (!ket||!ket.trim())){ alert('<%= Common.getBahasaConfigJS("Mohon lengkapi catatan terlebih dahulu.") %>'); return; }
      sel=[]; var nb=document.querySelectorAll('.ps-rute'+rnd); for(var k=0;k<nb.length;k++){ if(nb[k].checked) sel.push(nb[k].value); }
      if(!r.ruteOpsional && r.opsiRute && r.opsiRute.length && sel.length===0){ alert('<%= Common.getBahasaConfigJS("Mohon pilih disposisi selanjutnya terlebih dahulu.") %>'); return; }
      payload={aksi:'simpanPengajuanBaru', sopId:r.sopId, keterangan:ket, selanjutnya:sel.join(',')};
    } else if(r.stepId){
      if(r.bolehEditProses && r.catatanWajib && (!ket||!ket.trim())){ alert('<%= Common.getBahasaConfigJS("Mohon lengkapi catatan terlebih dahulu.") %>'); return; }
      if(r.bolehEditProses){
        sel=[]; var nodes=document.querySelectorAll('.ps-rute'+rnd); for(var i=0;i<nodes.length;i++){ if(nodes[i].checked) sel.push(nodes[i].value); }
        if(!r.ruteOpsional && r.opsiRute && r.opsiRute.length && sel.length===0){ alert('<%= Common.getBahasaConfigJS("Mohon pilih disposisi selanjutnya terlebih dahulu.") %>'); return; }
        var setujui=document.getElementById('psFsetujui'+rnd)?document.getElementById('psFsetujui'+rnd).checked:false;
        var kembali=document.getElementById('psFkembali'+rnd)?document.getElementById('psFkembali'+rnd).checked:false;
        payload={aksi:'simpanDisposisi', disposisiSopId:r.disposisiSopId, disposisiAlurSopId:r.stepId, alurSopId:r.alurSopId, keterangan:ket, setujui:setujui, kembali:kembali, selanjutnya:sel.join(',')};
      } else {
        payload={aksi:'editKeterangan', disposisiAlurSopId:r.stepId, keterangan:ket};
      }
    } else { return; }
    btn.disabled=true; btn.innerHTML='<span class="spinner-border spinner-border-sm me-1"></span>Menyimpan...';
    try{
      var res=await post(payload);
      if(res.status!=='00'){ alert(res.message||'<%= Common.getBahasaConfigJS("Mohon maaf, data gagal disimpan. Silakan coba lagi.") %>'); btn.disabled=false; btn.innerHTML=old; return; }
      hideFormModal();
      window['psLoadPengajuan'+rnd]();
      if(r.modeBaru){ if(res.disposisiSopId) window['psLihatAlur'+rnd](res.disposisiSopId); }
      else if(curDsId){ window['psLihatAlur'+rnd](curDsId); }
    }catch(e){ alert('Error: '+e.message); btn.disabled=false; btn.innerHTML=old; }
  };

  window['psLoadSop'+rnd]=async function(){
    var tb=$('psSopBody'); tb.innerHTML='<tr><td colspan="5" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr>';
    try{ var r=await post({aksi:'listSop', q:$('psCariSop').value});
      if(r.status!=='00'){ tb.innerHTML='<tr><td colspan="5" class="text-center text-danger py-4">'+esc(r.message||'Gagal')+'</td></tr>'; return; }
      if(!r.data||!r.data.length){ tb.innerHTML='<tr><td colspan="5" class="text-center text-muted py-4">Tidak ada SOP.</td></tr>'; return; }
      tb.innerHTML=r.data.map(function(p){
        return '<tr><td><span class="badge bg-light text-dark border">'+esc(p.jenis)+'</span></td><td class="fw-semibold">'+esc(p.kode)+'</td><td>'+esc(p.nama)+'</td><td>'+esc(p.versi)+'</td><td class="text-end"><button class="btn btn-sm btn-primary" onclick="psAjukan'+rnd+'('+p.id+')"><i class="fas fa-paper-plane me-1"></i>Ajukan</button></td></tr>';
      }).join('');
    }catch(e){ tb.innerHTML='<tr><td colspan="5" class="text-center text-danger py-4">Error: '+esc(e.message)+'</td></tr>'; }
  };

  window['psLoadPengajuan'+rnd]=async function(){
    var tb=$('psSayaBody'); tb.innerHTML='<tr><td colspan="6" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr>';
    try{ var r=await post({aksi:'listPengajuan', status:$('psFilterStatus').value});
      if(r.status!=='00'){ tb.innerHTML='<tr><td colspan="6" class="text-center text-danger py-4">'+esc(r.message||'Gagal')+'</td></tr>'; return; }
      $('psCTotal').textContent=r.countTotal||0; $('psCMenunggu').textContent=r.countMenunggu||0; $('psCDisetujui').textContent=r.countDisetujui||0; $('psCSelesai').textContent=r.countSelesai||0;
      if(!r.data||!r.data.length){ tb.innerHTML='<tr><td colspan="6" class="text-center text-muted py-4">Belum ada pengajuan.</td></tr>'; return; }
      tb.innerHTML=r.data.map(function(p,i){
        var badge=p.status==='selesai'?'<span class="badge bg-success">Selesai</span>':(p.status==='disetujui'?'<span class="badge bg-info text-dark">Disetujui</span>':'<span class="badge bg-warning text-dark">Menunggu</span>');
        return '<tr><td>'+(i+1)+'</td><td class="fw-semibold">'+esc(p.sop)+'</td><td>'+esc(p.jenis)+'</td><td>'+esc(p.waktu)+'</td><td class="text-center">'+badge+'</td><td class="text-end"><button class="btn btn-sm btn-primary" onclick="psLihatAlur'+rnd+'('+p.id+')"><i class="fas fa-diagram-project me-1"></i>Lihat / Proses</button></td></tr>';
      }).join('');
    }catch(e){ tb.innerHTML='<tr><td colspan="6" class="text-center text-danger py-4">Error: '+esc(e.message)+'</td></tr>'; }
  };

  // ---- Setujui Massal (bulk approve) ----
  window['psLoadMenunggu'+rnd]=async function(){
    var tb=$('psMenungguBody'); if(!tb) return;
    tb.innerHTML='<tr><td colspan="5" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr>';
    var ck=$('psChkAll'); if(ck) ck.checked=false;
    try{ var r=await post({aksi:'listMenungguSaya'});
      if(r.status!=='00'){ tb.innerHTML='<tr><td colspan="5" class="text-center text-danger py-4">'+esc(r.message||'Gagal')+'</td></tr>'; return; }
      if(!r.data||!r.data.length){ tb.innerHTML='<tr><td colspan="5" class="text-center text-muted py-4">Tidak ada pengajuan yang menunggu disposisi Anda.</td></tr>'; return; }
      tb.innerHTML=r.data.map(function(p){
        var manual=p.manual?' <span class="badge bg-warning text-dark">perlu manual</span>':'';
        var chk=p.manual?'<span class="text-muted" title="Perlu pilih rute lanjutan / catatan wajib"><i class="fas fa-hand-pointer"></i></span>':'<input type="checkbox" class="psMenungguChk'+rnd+'" value="'+p.id+'">';
        return '<tr><td class="text-center">'+chk+'</td><td class="fw-semibold">'+esc(p.sop)+manual+'</td><td>'+esc(p.langkah)+'</td><td class="small text-muted">'+esc(p.catatan)+'</td><td class="small">'+esc(p.waktu)+'</td></tr>';
      }).join('');
    }catch(e){ tb.innerHTML='<tr><td colspan="5" class="text-center text-danger py-4">Error: '+esc(e.message)+'</td></tr>'; }
  };
  window['psToggleAll'+rnd]=function(cb){ var xs=document.querySelectorAll('.psMenungguChk'+rnd); for(var i=0;i<xs.length;i++) xs[i].checked=cb.checked; };
  async function psKirimBulk(ids){
    var kb=$('psKetBulk'); var ket=kb?kb.value:'';
    try{ var r=await post({aksi:'setujuiBulk', ids:ids, keterangan:ket});
      alert(r.message||(r.status==='00'?'<%= Common.getBahasaConfigJS("Proses telah selesai.") %>':'<%= Common.getBahasaConfigJS("Proses gagal diselesaikan.") %>'));
      window['psLoadMenunggu'+rnd](); if(window['psLoadPengajuan'+rnd]) window['psLoadPengajuan'+rnd]();
    }catch(e){ alert('Error: '+e.message); }
  }
  window['psSetujuiTerpilih'+rnd]=function(){
    var ids=[], xs=document.querySelectorAll('.psMenungguChk'+rnd+':checked'); for(var i=0;i<xs.length;i++) ids.push(xs[i].value);
    if(!ids.length){ alert('<%= Common.getBahasaConfigJS("Mohon pilih minimal satu pengajuan.") %>'); return; }
    if(!confirm('Setujui '+ids.length+' pengajuan terpilih?')) return;
    psKirimBulk(ids.join(','));
  };
  window['psSetujuiSemua'+rnd]=function(){
    if(!confirm('<%= Common.getBahasaConfigJS("Apakah Bapak/Ibu yakin ingin menyetujui SEMUA pengajuan yang menunggu disposisi, kecuali yang perlu diproses manual?") %>')) return;
    psKirimBulk('');
  };

  window['psLoadSop'+rnd]();
  window['psLoadPengajuan'+rnd]();
})();
</script>
