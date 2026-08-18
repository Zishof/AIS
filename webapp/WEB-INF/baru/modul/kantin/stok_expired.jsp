<%-- Manajemen kedaluwarsa POS berbasis batch/lot. API bersama: KantinHelper. --%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser userKdl = Common.getCurrentUser(request);
if (userKdl == null || userKdl.getUserId() == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); out.print("Unauthorized"); return;
}
boolean bolehKdl = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
String rkdl = Common.getGeneratedBarCode(7);
%>
<div id="kdlRoot<%=rkdl%>">
  <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
    <div>
      <h5 class="fw-bold mb-1"><i class="fas fa-calendar-xmark text-danger me-2"></i><%=Common.getBahasaConfig("Manajemen Kedaluwarsa")%></h5>
      <div class="small text-muted"><%=Common.getBahasaConfig("Kontrol stok per batch/lot, FEFO, karantina, dan pemusnahan barang.")%></div>
    </div>
    <% if (bolehKdl) { %>
    <button class="btn btn-primary" id="kdlTambah<%=rkdl%>"><i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Batch")%></button>
    <% } %>
  </div>

  <div class="row g-2 mb-3">
    <div class="col-6 col-md"><div class="card border-danger h-100"><div class="card-body py-2"><div class="small text-muted">Sudah kedaluwarsa</div><div class="fs-4 fw-bold text-danger" id="kdlExpired<%=rkdl%>">0</div></div></div></div>
    <div class="col-6 col-md"><div class="card border-warning h-100"><div class="card-body py-2"><div class="small text-muted">&le; 30 hari</div><div class="fs-4 fw-bold text-warning" id="kdl30<%=rkdl%>">0</div></div></div></div>
    <div class="col-6 col-md"><div class="card border-primary h-100"><div class="card-body py-2"><div class="small text-muted">&le; 90 hari</div><div class="fs-4 fw-bold text-primary" id="kdl90<%=rkdl%>">0</div></div></div></div>
    <div class="col-6 col-md"><div class="card border-secondary h-100"><div class="card-body py-2"><div class="small text-muted">Belum didata</div><div class="fs-4 fw-bold text-secondary" id="kdlKosong<%=rkdl%>">0</div></div></div></div>
    <div class="col-6 col-md"><div class="card border-dark h-100"><div class="card-body py-2"><div class="small text-muted">Stok karantina</div><div class="fs-4 fw-bold" id="kdlKarantina<%=rkdl%>">0</div></div></div></div>
  </div>

  <div class="card border-0 shadow-sm rounded-4">
    <div class="card-body">
      <div class="row g-2 mb-3">
        <div class="col-md-7"><div class="input-group"><span class="input-group-text"><i class="fas fa-search"></i></span><input class="form-control" id="kdlCari<%=rkdl%>" placeholder="Cari produk, kode, atau nomor batch..."></div></div>
        <div class="col-md-4"><select class="form-select" id="kdlFilter<%=rkdl%>">
          <option value="KADALUWARSA">Sudah kedaluwarsa</option><option value="7_HARI">Maksimal 7 hari</option>
          <option value="30_HARI">Maksimal 30 hari</option><option value="90_HARI" selected>Maksimal 90 hari</option>
          <option value="KARANTINA">Karantina</option><option value="TANPA_TANGGAL">Belum didata</option><option value="SEMUA">Semua batch</option>
        </select></div>
        <div class="col-md-1 d-grid"><button class="btn btn-outline-primary" id="kdlRefresh<%=rkdl%>" title="Muat ulang"><i class="fas fa-rotate"></i></button></div>
      </div>
      <div class="table-responsive"><table class="table table-hover align-middle mb-0" style="min-width:900px">
        <thead class="table-light"><tr><th>Produk</th><th>Batch / Lot</th><th>Kedaluwarsa</th><th class="text-end">Sisa hari</th><th class="text-end">Stok batch</th><th>Status</th></tr></thead>
        <tbody id="kdlTbody<%=rkdl%>"><tr><td colspan="6" class="text-center py-4"><span class="spinner-border spinner-border-sm"></span></td></tr></tbody>
      </table></div>
      <div class="d-flex justify-content-between align-items-center mt-3"><small class="text-muted" id="kdlInfo<%=rkdl%>"></small><div class="btn-group"><button class="btn btn-sm btn-outline-secondary" id="kdlPrev<%=rkdl%>">Sebelumnya</button><button class="btn btn-sm btn-outline-secondary" id="kdlNext<%=rkdl%>">Berikutnya</button></div></div>
    </div>
  </div>
</div>

<div class="modal fade" id="kdlModal<%=rkdl%>" tabindex="-1" aria-hidden="true"><div class="modal-dialog modal-lg"><div class="modal-content">
  <div class="modal-header"><h5 class="modal-title">Kelola Batch Produk</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
  <div class="modal-body">
    <input type="hidden" id="kdlBatchId<%=rkdl%>">
    <div class="mb-3"><label class="form-label">Produk *</label><select class="form-select" id="kdlProduk<%=rkdl%>"></select></div>
    <div class="row g-3">
      <div class="col-md-6"><label class="form-label">Nomor batch / lot *</label><input class="form-control" id="kdlNomor<%=rkdl%>" maxlength="100"></div>
      <div class="col-md-3"><label class="form-label">Tanggal produksi</label><input type="date" class="form-control" id="kdlProduksi<%=rkdl%>"></div>
      <div class="col-md-3"><label class="form-label">Tanggal kedaluwarsa *</label><input type="date" class="form-control" id="kdlTanggal<%=rkdl%>"></div>
      <div class="col-md-4"><label class="form-label">Stok fisik batch *</label><input type="number" min="0" step="0.01" class="form-control" id="kdlStok<%=rkdl%>"></div>
      <div class="col-md-4"><label class="form-label">Status *</label><select class="form-select" id="kdlStatus<%=rkdl%>"><option value="AKTIF">Aktif / boleh dijual</option><option value="KARANTINA">Karantina</option><option value="DIMUSNAHKAN">Dimusnahkan</option></select></div>
      <div class="col-md-12"><label class="form-label">Catatan</label><textarea class="form-control" id="kdlCatatan<%=rkdl%>" rows="2"></textarea></div>
    </div>
    <div class="alert alert-danger d-none mt-3" id="kdlError<%=rkdl%>"></div>
  </div>
  <div class="modal-footer"><button class="btn btn-light" data-bs-dismiss="modal">Batal</button><button class="btn btn-primary" id="kdlSimpan<%=rkdl%>"><i class="fas fa-save me-1"></i>Simpan</button></div>
</div></div></div>

<script>
(function(){
  const ID='<%=rkdl%>', BOLEH=<%=bolehKdl%>, ROOT='<%=Common.ROOT%>', PAGE_SIZE=30;
  let page=1,total=0,rows=[],produk=[],modal=null,timer=null;
  const el=n=>document.getElementById(n+ID);
  const esc=s=>String(s==null?'':s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/"/g,'&quot;');
  const angka=n=>new Intl.NumberFormat('id-ID',{maximumFractionDigits:2}).format(Number(n)||0);
  const api=async payload=>{
    const res=await fetch(ROOT+'/Data',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
    const text=await res.text(); let json; try{json=JSON.parse(text);}catch(e){throw new Error('Respons server tidak valid.');}
    if(!res.ok || (json.status && json.status!=='00')) throw new Error(json.description||json.message||'Permintaan gagal.');
    return json;
  };
  const render=()=>{
    if(!rows.length){el('kdlTbody').innerHTML='<tr><td colspan="6" class="text-center text-muted py-4">Tidak ada batch pada filter ini.</td></tr>';}
    else el('kdlTbody').innerHTML=rows.map((r,i)=>{
      const s=r.sisaHari==null?null:Number(r.sisaHari), warna=s==null?'text-secondary':s<0?'text-danger':s<=30?'text-warning':'text-success';
      const sisa=s==null?'-':s<0?'Lewat '+Math.abs(s)+' hari':s+' hari';
      const status=r.legacy?'Belum per batch':r.status;
      return '<tr data-i="'+i+'" style="cursor:'+(BOLEH?'pointer':'default')+'"><td><div class="fw-semibold">'+esc(r.nama)+'</div><small class="text-muted">'+esc(r.kode)+'</small></td><td>'+esc(r.nomorBatch||'-')+'</td><td>'+esc(r.tanggalExpired||'-')+'</td><td class="text-end fw-semibold '+warna+'">'+sisa+'</td><td class="text-end">'+angka(r.stok)+'</td><td><span class="badge '+(status==='AKTIF'?'bg-success':status==='Belum per batch'?'bg-secondary':'bg-warning text-dark')+'">'+esc(status)+'</span></td></tr>';
    }).join('');
    if(BOLEH) el('kdlTbody').querySelectorAll('tr[data-i]').forEach(tr=>tr.onclick=()=>buka(rows[Number(tr.dataset.i)]));
    const pages=Math.max(1,Math.ceil(total/PAGE_SIZE)); el('kdlInfo').textContent='Halaman '+page+' dari '+pages+' · '+total+' batch';
    el('kdlPrev').disabled=page<=1; el('kdlNext').disabled=page>=pages;
  };
  const load=async()=>{
    el('kdlTbody').innerHTML='<tr><td colspan="6" class="text-center py-4"><span class="spinner-border spinner-border-sm"></span></td></tr>';
    try{
      const j=await api({action:'kedaluwarsa_list',filter:el('kdlFilter').value,keyword:el('kdlCari').value.trim(),page:page,page_size:PAGE_SIZE});
      rows=j.data||[]; total=Number(j.total)||0; const s=j.summary||{};
      el('kdlExpired').textContent=s.kedaluwarsa||0;el('kdl30').textContent=s.dalam30Hari||0;el('kdl90').textContent=s.dalam90Hari||0;el('kdlKosong').textContent=s.tanpaTanggal||0;el('kdlKarantina').textContent=angka(s.stokKarantina);render();
    }catch(e){el('kdlTbody').innerHTML='<tr><td colspan="6" class="text-danger text-center py-4">'+esc(e.message)+'</td></tr>';}
  };
  const loadProduk=async pilih=>{
    if(!produk.length){const j=await api({action:'produk_batch_produk_list'});produk=j.data||[];}
    el('kdlProduk').innerHTML='<option value="">-- Pilih produk --</option>'+produk.map(p=>'<option value="'+p.id+'">'+esc(p.kode)+' · '+esc(p.nama)+' (stok '+angka(p.stok)+')</option>').join('');
    if(pilih!=null)el('kdlProduk').value=String(pilih);
  };
  const buka=async r=>{
    el('kdlError').classList.add('d-none'); el('kdlBatchId').value=r&&r.batchId!=null?r.batchId:'';
    await loadProduk(r?r.produkId:null); el('kdlProduk').disabled=!!(r&&r.batchId!=null);
    el('kdlNomor').value=r&&r.nomorBatch?r.nomorBatch:'';el('kdlProduksi').value=r&&r.tanggalProduksi?r.tanggalProduksi:'';el('kdlTanggal').value=r&&r.tanggalExpired?r.tanggalExpired:'';
    el('kdlStok').value=r?Number(r.stok||0):'';el('kdlStatus').value=r&&r.status?r.status:'AKTIF';el('kdlCatatan').value=r&&r.keterangan?r.keterangan:'';
    if(!modal)modal=new bootstrap.Modal(el('kdlModal'));modal.show();
  };
  el('kdlSimpan').onclick=async()=>{
    const err=el('kdlError'),payload={action:'produk_batch_simpan',produk_id:el('kdlProduk').value,nomor_batch:el('kdlNomor').value.trim(),tanggal_produksi:el('kdlProduksi').value,tanggal_expired:el('kdlTanggal').value,stok_fisik:Number(el('kdlStok').value),status:el('kdlStatus').value,keterangan:el('kdlCatatan').value.trim()};
    if(el('kdlBatchId').value)payload.batch_id=el('kdlBatchId').value;
    if(!payload.produk_id||!payload.nomor_batch||!payload.tanggal_expired||!Number.isFinite(payload.stok_fisik)||payload.stok_fisik<0){err.textContent='Produk, nomor batch, tanggal kedaluwarsa, dan stok fisik wajib valid.';err.classList.remove('d-none');return;}
    el('kdlSimpan').disabled=true;try{await api(payload);modal.hide();await load();}catch(e){err.textContent=e.message;err.classList.remove('d-none');}finally{el('kdlSimpan').disabled=false;}
  };
  if(el('kdlTambah'))el('kdlTambah').onclick=()=>buka(null);
  el('kdlRefresh').onclick=load;el('kdlFilter').onchange=()=>{page=1;load();};el('kdlCari').oninput=()=>{clearTimeout(timer);timer=setTimeout(()=>{page=1;load();},350);};
  el('kdlPrev').onclick=()=>{if(page>1){page--;load();}};el('kdlNext').onclick=()=>{if(page*PAGE_SIZE<total){page++;load();}};load();
})();
</script>
