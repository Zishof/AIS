<%@page import="ais.common.Common"%>
<%
String rndP = Common.getGeneratedBarCode(6);
String svcPR = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fkulakan&s=permintaan_asset_service";
%>

<div class="card border-0 shadow-sm rounded-4 mb-3">
    <div class="card-body">
        <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-1">
            <div>
                <h6 class="fw-bold mb-0"><i class="fas fa-file-signature me-2 text-primary"></i><%=Common.getBahasaConfig("Permintaan Pembelian (PR)")%></h6>
                <small class="text-muted"><%=Common.getBahasaConfig("Ajukan daftar barang yang ingin dibeli. Setelah disetujui, lanjut ke Pemesanan (PO).")%></small>
            </div>
            <button class="btn btn-primary rounded-pill px-3" onclick="prBaru<%=rndP%>()"><i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Buat Permintaan")%></button>
        </div>

        <div class="row g-2 align-items-end mt-1">
            <div class="col-12 col-md-3"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Kode")%></label><input type="text" id="prfKode<%=rndP%>" class="form-control form-control-sm" placeholder="cari kode..."></div>
            <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Status")%></label>
                <select id="prfStatus<%=rndP%>" class="form-select form-select-sm">
                    <option value="semua"><%=Common.getBahasaConfig("Semua")%></option>
                    <option value="pending"><%=Common.getBahasaConfig("Menunggu")%></option>
                    <option value="disetujui"><%=Common.getBahasaConfig("Disetujui")%></option>
                    <option value="ditolak"><%=Common.getBahasaConfig("Ditolak")%></option>
                </select>
            </div>
            <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Dari Tanggal")%></label><input type="date" id="prfTglM<%=rndP%>" class="form-control form-control-sm"></div>
            <div class="col-6 col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Sampai")%></label><input type="date" id="prfTglA<%=rndP%>" class="form-control form-control-sm"></div>
            <div class="col-6 col-md-3"><button class="btn btn-outline-primary btn-sm w-100" onclick="prLoad<%=rndP%>()"><i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Tampilkan")%></button></div>
        </div>
    </div>
</div>

<div class="table-responsive">
    <table class="table table-hover align-middle">
        <thead class="table-light">
            <tr>
                <th>#</th><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Tanggal")%></th>
                <th><%=Common.getBahasaConfig("Satuan Kerja")%></th><th><%=Common.getBahasaConfig("Toko")%></th><th class="text-center"><%=Common.getBahasaConfig("Item")%></th>
                <th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th><th class="text-center"><%=Common.getBahasaConfig("Status")%></th>
                <th class="text-end"><%=Common.getBahasaConfig("Aksi")%></th>
            </tr>
        </thead>
        <tbody id="prBody<%=rndP%>"><tr><td colspan="9" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr></tbody>
    </table>
</div>

<!-- Modal Form PR -->
<div class="modal fade" id="prModal<%=rndP%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-xl modal-dialog-scrollable">
    <div class="modal-content rounded-4">
      <div class="modal-header"><h5 class="modal-title fw-bold" id="prModalTitle<%=rndP%>"><%=Common.getBahasaConfig("Permintaan Pembelian")%></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
      <div class="modal-body">
        <input type="hidden" id="prId<%=rndP%>">
        <div class="row g-3">
          <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Kode")%></label><input type="text" id="prKode<%=rndP%>" class="form-control" placeholder="otomatis bila kosong"></div>
          <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Tanggal")%></label><input type="date" id="prTgl<%=rndP%>" class="form-control"></div>
          <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Satuan Kerja")%></label><select id="prSatker<%=rndP%>" class="form-select"></select></div>
          <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Lokasi")%></label><select id="prLokasi<%=rndP%>" class="form-select"></select></div>
          <div class="col-12 col-md-3"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Toko / Pedagang")%></label><select id="prToko<%=rndP%>" class="form-select"></select></div>
          <input type="hidden" id="prDisposisi<%=rndP%>"><%-- SOP dijalankan via menu "Pengajuan SOP", bukan dipilih di CRUD ini --%>
          <div class="col-12"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Keterangan")%></label><input type="text" id="prKet<%=rndP%>" class="form-control"></div>
          <div class="col-12 col-md-6 position-relative"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Akun Beban")%></label>
            <input type="hidden" id="prAkunId<%=rndP%>"><input type="text" id="prAkun<%=rndP%>" class="form-control" placeholder="<%=Common.getBahasaConfig("cari akun beban (opsional)...")%>" autocomplete="off" oninput="prCariAkun<%=rndP%>()">
            <div id="prAkunHasil<%=rndP%>" class="list-group position-absolute w-100 shadow" style="z-index:1085;max-height:220px;overflow:auto;display:none;"></div>
          </div>
          <div class="col-12 col-md-6 position-relative" id="prWorkspaceWrap<%=rndP%>"><label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Anggaran")%></label>
            <input type="hidden" id="prWorkspaceId<%=rndP%>"><input type="text" id="prWorkspace<%=rndP%>" class="form-control" placeholder="<%=Common.getBahasaConfig("cari anggaran (opsional)...")%>" autocomplete="off" oninput="prCariWorkspace<%=rndP%>()">
            <div id="prWorkspaceHasil<%=rndP%>" class="list-group position-absolute w-100 shadow" style="z-index:1085;max-height:220px;overflow:auto;display:none;"></div>
          </div>
          <div class="col-12" id="prAnggaranInfo<%=rndP%>" style="display:none">
            <div class="border rounded-3 p-2 bg-light small">
              <div class="row g-2">
                <div class="col-6 col-md-3"><span class="text-muted d-block"><%=Common.getBahasaConfig("Unit")%></span><span class="fw-semibold" id="prAiUnit<%=rndP%>">-</span></div>
                <div class="col-6 col-md-3"><span class="text-muted d-block"><%=Common.getBahasaConfig("Nilai Anggaran")%></span><span class="fw-semibold" id="prAiNilai<%=rndP%>">-</span></div>
                <div class="col-6 col-md-3"><span class="text-muted d-block"><%=Common.getBahasaConfig("Dalam Proses")%></span><span class="fw-semibold text-warning" id="prAiProses<%=rndP%>">-</span></div>
                <div class="col-6 col-md-3"><span class="text-muted d-block"><%=Common.getBahasaConfig("Sisa Anggaran")%></span><span class="fw-bold text-success" id="prAiSisa<%=rndP%>">-</span></div>
                <div class="col-12 col-md-6"><span class="text-muted d-block"><%=Common.getBahasaConfig("Periode")%></span><span id="prAiPeriode<%=rndP%>">-</span></div>
                <div class="col-12 col-md-6"><span class="text-muted d-block"><%=Common.getBahasaConfig("Akun Anggaran")%></span><span id="prAiAkun<%=rndP%>">-</span></div>
              </div>
            </div>
          </div>
          <div class="col-12 d-flex flex-wrap gap-3 pt-1">
            <div class="form-check"><input class="form-check-input" type="checkbox" id="prTanpaAnggaran<%=rndP%>" onchange="prToggleAnggaran<%=rndP%>()"><label class="form-check-label small" for="prTanpaAnggaran<%=rndP%>"><%=Common.getBahasaConfig("Tanpa Anggaran")%></label></div>
            <div class="form-check"><input class="form-check-input" type="checkbox" id="prDanaTitipan<%=rndP%>" onchange="prToggleAnggaran<%=rndP%>()"><label class="form-check-label small" for="prDanaTitipan<%=rndP%>"><%=Common.getBahasaConfig("Dana Titipan")%></label></div>
            <div class="form-check"><input class="form-check-input" type="checkbox" id="prWajibPks<%=rndP%>"><label class="form-check-label small" for="prWajibPks<%=rndP%>"><%=Common.getBahasaConfig("Wajib Perjanjian Kerjasama")%></label></div>
          </div>
        </div>

        <hr class="my-3">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <span class="fw-bold"><i class="fas fa-boxes me-2 text-primary"></i><%=Common.getBahasaConfig("Daftar Barang")%></span>
        </div>
        <div class="position-relative mb-2">
          <input type="text" id="prCari<%=rndP%>" class="form-control" placeholder="<%=Common.getBahasaConfig("Ketik nama/kode barang lalu pilih...")%>" autocomplete="off" oninput="prCariBarang<%=rndP%>()">
          <div id="prCariHasil<%=rndP%>" class="list-group position-absolute w-100 shadow" style="z-index:1080;max-height:240px;overflow:auto;display:none;"></div>
        </div>
        <div class="table-responsive">
          <table class="table table-sm align-middle">
            <thead class="table-light"><tr><th><%=Common.getBahasaConfig("Barang")%></th><th style="width:120px" class="text-end"><%=Common.getBahasaConfig("Jumlah")%></th><th style="width:160px" class="text-end"><%=Common.getBahasaConfig("Harga")%></th><th style="width:160px" class="text-end"><%=Common.getBahasaConfig("Subtotal")%></th><th style="width:48px"></th></tr></thead>
            <tbody id="prLines<%=rndP%>"></tbody>
            <tfoot><tr class="table-light fw-bold"><td colspan="3" class="text-end"><%=Common.getBahasaConfig("Total")%></td><td class="text-end" id="prTotal<%=rndP%>">Rp 0</td><td></td></tr></tfoot>
          </table>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-light rounded-pill" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
        <button type="button" class="btn btn-primary rounded-pill px-4" id="prBtnSimpan<%=rndP%>" onclick="prSimpan<%=rndP%>()"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan")%></button>
      </div>
    </div>
  </div>
</div>

<script>
(function(){
    var SVC = '<%=svcPR%>';
    var rnd = '<%=rndP%>';
    var comboLoaded = false, lockToko = false, tokoTerkunci = '';
    function $(id){ return document.getElementById(id + rnd); }
    function rupiah(n){ n = Number(n)||0; return 'Rp ' + n.toLocaleString('id-ID'); }
    function esc(s){ return (s==null?'':String(s)).replace(/[&<>"']/g, function(c){ return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]; }); }
    async function post(params){
        var body = new URLSearchParams(params);
        var res = await fetch(SVC, { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: body });
        var txt = await res.text();
        return JSON.parse(txt.trim());
    }

    window['prLoad'+rnd] = async function(){
        var tb = $('prBody');
        tb.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-4"><div class="spinner-border spinner-border-sm me-2"></div>Memuat...</td></tr>';
        try {
            var r = await post({ aksi:'list', kode:$('prfKode').value, status:$('prfStatus').value, tglMulai:$('prfTglM').value, tglAkhir:$('prfTglA').value });
            if (r.status !== '00'){ tb.innerHTML = '<tr><td colspan="9" class="text-center text-danger py-4">'+esc(r.message||'Gagal memuat')+'</td></tr>'; return; }
            if (!r.data || !r.data.length){ tb.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-4">Belum ada data.</td></tr>'; return; }
            var html = '';
            r.data.forEach(function(p, i){
                var badge = p.status==='disetujui' ? '<span class="badge bg-success">Disetujui</span>' : (p.status==='ditolak' ? '<span class="badge bg-danger">Ditolak</span>' : '<span class="badge bg-warning text-dark">Menunggu</span>');
                var aksi = '<button class="btn btn-sm btn-outline-secondary me-1" title="Lihat/Ubah" onclick="prEdit'+rnd+'('+p.id+')"><i class="fas fa-eye"></i></button>';
                if (p.status==='pending'){
                    aksi += '<button class="btn btn-sm btn-outline-success me-1" title="Setujui" onclick="prSetujui'+rnd+'('+p.id+')"><i class="fas fa-check"></i></button>';
                    aksi += '<button class="btn btn-sm btn-outline-danger" title="Hapus" onclick="prHapus'+rnd+'('+p.id+')"><i class="fas fa-trash"></i></button>';
                }
                html += '<tr><td>'+(i+1)+'</td><td class="fw-semibold">'+esc(p.kode)+'</td><td>'+esc(p.tanggal)+'</td><td>'+esc(p.satuanKerja)+'</td><td>'+esc(p.toko||'')+'</td><td class="text-center">'+p.jumlahItem+'</td><td class="text-end">'+rupiah(p.nilai)+'</td><td class="text-center">'+badge+(p.sop?' <i class="fas fa-route text-info" title="Dijalankan via SOP"></i>':'')+'</td><td class="text-end">'+aksi+'</td></tr>';
            });
            tb.innerHTML = html;
        } catch(e){ tb.innerHTML = '<tr><td colspan="9" class="text-center text-danger py-4">Error: '+esc(e.message)+'</td></tr>'; }
    };

    async function loadCombo(){
        if (comboLoaded) return;
        var r = await post({ aksi:'combo' });
        if (r.status==='00'){
            var sk = '<option value="">- pilih -</option>'; (r.satuanKerja||[]).forEach(function(x){ sk += '<option value="'+x.id+'">'+esc(x.nama)+'</option>'; }); $('prSatker').innerHTML = sk;
            var lk = '<option value="">- pilih -</option>'; (r.lokasi||[]).forEach(function(x){ lk += '<option value="'+x.id+'">'+esc(x.nama)+'</option>'; }); $('prLokasi').innerHTML = lk;
            var tk = '<option value="">- pilih -</option>'; (r.toko||[]).forEach(function(x){ tk += '<option value="'+x.id+'">'+esc(x.nama)+'</option>'; }); $('prToko').innerHTML = tk;
            var ds = '<option value="">- tanpa SOP -</option>'; (r.disposisiSop||[]).forEach(function(x){ ds += '<option value="'+x.id+'">'+esc(x.nama)+'</option>'; }); $('prDisposisi').innerHTML = ds;
            lockToko = !!r.lockToko; tokoTerkunci = r.tokoTerkunci || '';
            comboLoaded = true;
        }
    }

    function modal(){ return bootstrap.Modal.getOrCreateInstance($('prModal')); }

    window['prBaru'+rnd] = async function(){
        await loadCombo();
        $('prId').value=''; $('prKode').value=''; $('prTgl').value=new Date().toISOString().slice(0,10);
        $('prSatker').value=''; $('prLokasi').value=''; $('prKet').value=''; $('prToko').value = lockToko ? tokoTerkunci : ''; $('prDisposisi').value='';
        $('prAkun').value=''; $('prAkunId').value=''; $('prWorkspace').value=''; $('prWorkspaceId').value=''; $('prTanpaAnggaran').checked=false; $('prDanaTitipan').checked=false; $('prWajibPks').checked=false; prToggleAnggaranFn();
        $('prLines').innerHTML=''; recalc(); enableForm(true);
        document.getElementById('prModalTitle'+rnd).textContent='Buat Permintaan';
        modal().show();
    };

    window['prEdit'+rnd] = async function(id){
        await loadCombo();
        var r = await post({ aksi:'detail', id:id });
        if (r.status!=='00'){ tampilkanPesanGagalFormal("pengelolaan Permintaan Barang (PR) kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); return; }
        var d = r.data;
        $('prId').value=d.id; $('prKode').value=d.kode||''; $('prTgl').value=d.tanggal||'';
        $('prSatker').value=d.satuanKerja||''; $('prLokasi').value=d.lokasi||''; $('prKet').value=d.keterangan||''; $('prToko').value = lockToko ? tokoTerkunci : (d.toko||''); $('prDisposisi').value=d.disposisiSop||'';
        $('prAkunId').value=d.akun||''; $('prAkun').value=d.akunNama||''; $('prWorkspaceId').value=d.workspace||''; $('prWorkspace').value=d.workspaceNama||''; $('prTanpaAnggaran').checked=!!d.tanpaAnggaran; $('prDanaTitipan').checked=!!d.danaTitipan; $('prWajibPks').checked=!!d.wajibPerjanjian; prToggleAnggaranFn();
        $('prLines').innerHTML='';
        (d.lines||[]).forEach(function(l){ tambahBaris({ id:l.masterAsset, nama:l.masterAssetNama, harga:l.hargaBeli }, l.jumlah, l.keterangan); });
        recalc();
        var bisaUbah = d.status==='pending';
        enableForm(bisaUbah);
        document.getElementById('prModalTitle'+rnd).textContent = bisaUbah ? 'Ubah Permintaan' : 'Detail Permintaan ('+(d.status==='disetujui'?'Disetujui':'Ditolak')+')';
        modal().show();
    };

    function enableForm(on){
        ['prKode','prTgl','prSatker','prLokasi','prToko','prDisposisi','prKet','prCari','prAkun','prWorkspace','prTanpaAnggaran','prDanaTitipan','prWajibPks'].forEach(function(k){ var el=$(k); if(el) el.disabled=!on; });
        if($('prToko')) $('prToko').disabled = !on || lockToko;
        $('prBtnSimpan').style.display = on ? '' : 'none';
        $('prLines').querySelectorAll('input,button').forEach(function(el){ el.disabled=!on; });
    }

    var cariTimer=null;
    window['prCariBarang'+rnd] = function(){
        clearTimeout(cariTimer);
        cariTimer = setTimeout(async function(){
            var q = $('prCari').value.trim();
            var box = $('prCariHasil');
            if (q.length < 2){ box.style.display='none'; return; }
            var r = await post({ aksi:'cariMasterAsset', q:q });
            if (r.status!=='00' || !r.data || !r.data.length){ box.innerHTML='<div class="list-group-item text-muted small">Tidak ada barang.</div>'; box.style.display='block'; return; }
            box.innerHTML = r.data.map(function(m){ return '<button type="button" class="list-group-item list-group-item-action" data-id="'+m.id+'" data-nama="'+esc(m.kode+' - '+m.nama)+'" data-harga="'+(m.harga||0)+'"><span class="fw-semibold">'+esc(m.nama)+'</span> <small class="text-muted">'+esc(m.kode)+'</small> <span class="float-end small">'+rupiah(m.harga)+'</span></button>'; }).join('');
            box.style.display='block';
            box.querySelectorAll('button').forEach(function(b){ b.onclick=function(){ tambahBaris({ id:b.getAttribute('data-id'), nama:b.getAttribute('data-nama'), harga:Number(b.getAttribute('data-harga')) }, 1, ''); recalc(); box.style.display='none'; $('prCari').value=''; }; });
        }, 250);
    };
    document.addEventListener('click', function(e){ var box=$('prCariHasil'); if(box && !box.contains(e.target) && e.target!==$('prCari')) box.style.display='none'; });

    // Pemilih cari (Akun Beban / Anggaran) ala banbox ZK
    function pickerCari(inKey, hasilKey, hiddenKey, aksi, onSelect){
        clearTimeout(cariTimer);
        cariTimer = setTimeout(async function(){
            $(hiddenKey).value=''; // ketik = batalkan pilihan lama sampai dipilih ulang
            var q = $(inKey).value.trim(); var box = $(hasilKey);
            if (q.length < 2){ box.style.display='none'; return; }
            var r = await post({ aksi:aksi, q:q });
            if (r.status!=='00' || !r.data || !r.data.length){ box.innerHTML='<div class="list-group-item text-muted small">Tidak ada data.</div>'; box.style.display='block'; return; }
            box.innerHTML = r.data.map(function(x){ return '<button type="button" class="list-group-item list-group-item-action" data-id="'+x.id+'" data-nama="'+esc(x.nama)+'">'+esc(x.nama)+'</button>'; }).join('');
            box.style.display='block';
            box.querySelectorAll('button').forEach(function(b){ b.onclick=function(){ $(hiddenKey).value=b.getAttribute('data-id'); $(inKey).value=b.getAttribute('data-nama'); box.style.display='none'; if(onSelect) onSelect(); }; });
        }, 250);
    }
    window['prCariAkun'+rnd] = function(){ pickerCari('prAkun','prAkunHasil','prAkunId','cariAkun'); };
    window['prCariWorkspace'+rnd] = function(){ pickerCari('prWorkspace','prWorkspaceHasil','prWorkspaceId','cariWorkspace', infoAnggaran); };

    // Panel saldo anggaran live (reuse JenisUangMukaAction.hitungSaldo via service)
    async function infoAnggaran(){
        var box=$('prAnggaranInfo'); var wid=$('prWorkspaceId').value;
        if(!wid){ if(box) box.style.display='none'; return; }
        try{
            var r=await post({ aksi:'infoAnggaran', workspace:wid, id:$('prId').value, tanggal:$('prTgl').value });
            if(r.status==='00' && r.ada){
                $('prAiUnit').textContent=r.unit||'-'; $('prAiNilai').textContent=rupiah(r.nilai);
                $('prAiProses').textContent=rupiah(r.dalamProses); $('prAiSisa').textContent=rupiah(r.sisa);
                $('prAiPeriode').textContent=r.periode||'-'; $('prAiAkun').textContent=r.akun||'-';
                box.style.display='';
            } else box.style.display='none';
        }catch(e){ box.style.display='none'; }
    }
    window['prInfoAnggaran'+rnd]=infoAnggaran;
    // Tanpa Anggaran <-> Dana Titipan saling eksklusif + sembunyikan Anggaran bila tanpa anggaran (pola ZK)
    function prToggleAnggaranFn(){
        if($('prTanpaAnggaran').checked) $('prDanaTitipan').checked=false;
        if($('prDanaTitipan').checked) $('prTanpaAnggaran').checked=false;
        var sembunyi=$('prTanpaAnggaran').checked;
        if($('prWorkspaceWrap')) $('prWorkspaceWrap').style.display = sembunyi ? 'none' : '';
        if(sembunyi){ if($('prAnggaranInfo')) $('prAnggaranInfo').style.display='none'; }
        else if($('prWorkspaceId').value){ infoAnggaran(); }
    }
    window['prToggleAnggaran'+rnd]=prToggleAnggaranFn;
    document.addEventListener('click', function(e){ ['prAkunHasil','prWorkspaceHasil'].forEach(function(k){ var box=$(k); var inp=$(k.replace('Hasil','')); if(box && !box.contains(e.target) && e.target!==inp) box.style.display='none'; }); });

    function tambahBaris(asset, jumlah, ket){
        var tr = document.createElement('tr');
        tr.setAttribute('data-id', asset.id);
        tr.innerHTML = '<td>'+esc(asset.nama)+'<input type="hidden" class="bb-ket" value="'+esc(ket||'')+'"></td>'
            + '<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-jml" value="'+(jumlah||1)+'" oninput="prRecalc'+rnd+'()"></td>'
            + '<td><input type="number" min="0" step="any" class="form-control form-control-sm text-end bb-hrg" value="'+(asset.harga||0)+'" oninput="prRecalc'+rnd+'()"></td>'
            + '<td class="text-end bb-sub">Rp 0</td>'
            + '<td class="text-center"><button class="btn btn-sm btn-outline-danger" onclick="this.closest(\'tr\').remove(); prRecalc'+rnd+'()"><i class="fas fa-times"></i></button></td>';
        $('prLines').appendChild(tr);
    }

    function recalc(){
        var total=0;
        $('prLines').querySelectorAll('tr').forEach(function(tr){
            var j=Number(tr.querySelector('.bb-jml').value)||0, h=Number(tr.querySelector('.bb-hrg').value)||0, s=j*h;
            tr.querySelector('.bb-sub').textContent=rupiah(s); total+=s;
        });
        $('prTotal').textContent=rupiah(total);
    }
    window['prRecalc'+rnd]=recalc;

    window['prSimpan'+rnd] = async function(){
        var lines=[];
        $('prLines').querySelectorAll('tr').forEach(function(tr){
            lines.push({ masterAsset:tr.getAttribute('data-id'), jumlah:Number(tr.querySelector('.bb-jml').value)||0, hargaBeli:Number(tr.querySelector('.bb-hrg').value)||0, keterangan:tr.querySelector('.bb-ket').value||'' });
        });
        if (!lines.length){ tampilkanPesanGagalFormal("penyimpanan Permintaan Barang (PR)", '<%=Common.getBahasaConfigJS("Belum ada satu pun barang yang ditambahkan pada rincian permintaan ini.")%>', ["Klik tombol cari/tambah barang untuk memasukkan minimal satu baris barang.", "Setelah rincian terisi, silakan simpan kembali."]); return; }
        var payload = { id:$('prId').value, kode:$('prKode').value, tanggal:$('prTgl').value, satuanKerja:$('prSatker').value, lokasi:$('prLokasi').value, toko:$('prToko').value, disposisiSop:$('prDisposisi').value, akun:$('prAkunId').value, workspace:$('prWorkspaceId').value, tanpaAnggaran:$('prTanpaAnggaran').checked, danaTitipan:$('prDanaTitipan').checked, wajibPerjanjian:$('prWajibPks').checked, keterangan:$('prKet').value, lines:lines };
        var btn=$('prBtnSimpan'); btn.disabled=true; var ori=btn.innerHTML; btn.innerHTML='<span class="spinner-border spinner-border-sm me-2"></span>Menyimpan...';
        try {
            var r = await post({ aksi:'simpan', data: JSON.stringify(payload) });
            if (r.status==='00'){ modal().hide(); window['prLoad'+rnd](); }
            else tampilkanPesanGagalFormal("penyimpanan Permintaan Barang (PR)", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]);
        } catch(e){ tampilkanPesanGagalFormal("penyimpanan Permintaan Barang (PR)", "Rincian teknis: "+e.message, ["Periksa koneksi internet Bapak/Ibu.", "Ulangi proses penyimpanan beberapa saat lagi."]); }
        finally { btn.disabled=false; btn.innerHTML=ori; }
    };

    window['prSetujui'+rnd] = async function(id){
        if (!confirm('<%= Common.getBahasaConfigJS("Apakah Bapak/Ibu yakin ingin menyetujui permintaan ini?") %>')) return;
        var r = await post({ aksi:'setujui', id:id });
        if (r.status==='00') window['prLoad'+rnd](); else tampilkanPesanGagalFormal("pengelolaan Permintaan Barang (PR) kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]);
    };
    window['prHapus'+rnd] = async function(id){
        if (!confirm('<%= Common.getBahasaConfigJS("Apakah Bapak/Ibu yakin ingin menghapus permintaan ini?") %>')) return;
        var r = await post({ aksi:'hapus', id:id });
        if (r.status==='00') window['prLoad'+rnd](); else tampilkanPesanGagalFormal("pengelolaan Permintaan Barang (PR) kantin", r.message||'Peladen menolak permintaan tanpa keterangan rinci.', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]);
    };

    // muat awal
    window['prLoad'+rnd]();
})();
</script>
