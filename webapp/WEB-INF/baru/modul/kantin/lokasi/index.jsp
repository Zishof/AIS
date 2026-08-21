<%--
  Master "Lokasi (Gudang)" (versi JSP) — daftar + form tambah/ubah/hapus (modal), gate admin.
  Setiap lokasi bisa diberi Jenis (Gudang/Outlet/Kasir/…) & dikaitkan ke Toko/Outlet.
  Reuse lokasi/service.jsp (yang reuse LokasiKantinUtil). Aman di-include berkali-kali (id ber-akhiran rnd).
--%>
<%@page import="ais.common.Common"%>
<%@page import="ais.action.master.koperasi.helper.LokasiKantinUtil"%>
<%
String rlk = ais.common.Common.getGeneratedBarCode(6);
boolean bolehLK = LokasiKantinUtil.bolehKelola(request);
String svcLK = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Flokasi&s=service";
%>
<div class="lk-wrap-<%=rlk%>">
  <style>
    .lk-wrap-<%=rlk%> .lk-badge{display:inline-flex;align-items:center;gap:6px;padding:2px 10px;border-radius:999px;color:#fff;font-weight:600;font-size:.75rem}
    .lk-wrap-<%=rlk%> table td{vertical-align:middle}
  </style>

  <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
    <div>
      <h5 class="fw-bold mb-0"><i class="fas fa-warehouse text-primary me-2"></i><%=Common.getBahasaConfig("Lokasi (Gudang)")%></h5>
      <div class="text-muted small"><%=Common.getBahasaConfig("Daftar tempat fisik: gudang, outlet, kasir, dan lainnya. Tetapkan jenisnya agar rapi di laporan.")%></div>
    </div>
    <div class="d-flex gap-2 flex-wrap">
      <select id="lkFilter<%=rlk%>" class="form-select form-select-sm" style="max-width:180px" onchange="lkRender<%=rlk%>()"><option value="">Semua jenis</option></select>
      <input id="lkCari<%=rlk%>" class="form-control form-control-sm" style="max-width:200px" placeholder="Cari…" oninput="lkRender<%=rlk%>()"/>
      <% if (bolehLK) { %>
      <a class="btn btn-outline-secondary btn-sm" href="<%=Common.ROOT%>/pages/master/sirs/gudang.zul" target="_blank"
         title="<%=Common.getBahasaConfig("Tambah/ubah data Gudang (termasuk Gudang Induk untuk hierarki pusat/cabang) di tab baru")%>">
        <i class="fas fa-warehouse me-1"></i><%=Common.getBahasaConfig("Kelola Gudang")%></a>
      <button class="btn btn-primary btn-sm" onclick="lkForm<%=rlk%>(null)"><i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah")%></button>
      <% } %>
    </div>
  </div>

  <% if (!bolehLK) { %>
  <div class="alert alert-light border small py-2"><i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Anda hanya dapat melihat. Perubahan hanya untuk admin (bukan pedagang/toko).")%></div>
  <% } %>

  <div class="table-responsive">
    <table class="table table-hover table-sm align-middle">
      <thead class="table-light"><tr>
        <th><%=Common.getBahasaConfig("Nama Lokasi")%></th>
        <th style="width:150px"><%=Common.getBahasaConfig("Jenis")%></th>
        <th style="width:170px"><%=Common.getBahasaConfig("Outlet/Toko")%></th>
        <th style="width:170px"><%=Common.getBahasaConfig("Gudang Induk")%></th>
        <th><%=Common.getBahasaConfig("Alamat")%></th>
        <th style="width:80px" class="text-center"><%=Common.getBahasaConfig("Aktif")%></th>
        <% if (bolehLK) { %><th style="width:110px" class="text-center"><%=Common.getBahasaConfig("Aksi")%></th><% } %>
      </tr></thead>
      <tbody id="lkBody<%=rlk%>"><tr><td colspan="7" class="text-center text-muted py-4"><i class="fas fa-spinner fa-spin me-1"></i>Memuat…</td></tr></tbody>
    </table>
  </div>
</div>

<!-- Modal Form -->
<div class="modal fade" id="lkModal<%=rlk%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content border-0 shadow rounded-4">
      <div class="modal-header"><h6 class="modal-title fw-bold" id="lkModalTitle<%=rlk%>">Lokasi</h6>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
      <div class="modal-body">
        <input type="hidden" id="lkId<%=rlk%>"/>
        <div class="mb-2"><label class="form-label small fw-semibold"><%= Common.getBahasaConfig("Nama Lokasi *") %></label>
          <input id="lkNama<%=rlk%>" class="form-control" maxlength="255" placeholder="mis. Gudang Utama"/></div>
        <div class="row g-2">
          <div class="col-6"><label class="form-label small fw-semibold"><%= Common.getBahasaConfig("Jenis") %></label>
            <select id="lkJenis<%=rlk%>" class="form-select"><option value="">— pilih —</option></select></div>
          <div class="col-6"><label class="form-label small fw-semibold"><%= Common.getBahasaConfig("Outlet/Toko") %></label>
            <select id="lkToko<%=rlk%>" class="form-select"><option value="">— (opsional) —</option></select></div>
        </div>
        <div class="mb-2 mt-2"><label class="form-label small fw-semibold"><%= Common.getBahasaConfig("Gudang Induk (opsional, Fase 2 — hierarki pusat/cabang)") %></label>
          <select id="lkGudang<%=rlk%>" class="form-select"><option value="">— (tanpa) —</option></select></div>
        <div class="mb-2 mt-2"><label class="form-label small fw-semibold"><%= Common.getBahasaConfig("Alamat") %></label>
          <textarea id="lkAlamat<%=rlk%>" class="form-control" rows="2"></textarea></div>
        <div class="mb-2"><label class="form-label small fw-semibold"><%= Common.getBahasaConfig("Keterangan") %></label>
          <input id="lkKet<%=rlk%>" class="form-control"/></div>
        <div class="form-check form-switch"><input class="form-check-input" type="checkbox" id="lkAktif<%=rlk%>" checked/>
          <label class="form-check-label small" for="lkAktif<%=rlk%>"><%= Common.getBahasaConfig("Aktif") %></label></div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-light btn-sm" data-bs-dismiss="modal"><%= Common.getBahasaConfig("Batal") %></button>
        <button class="btn btn-primary btn-sm" onclick="lkSimpan<%=rlk%>()"><i class="fas fa-save me-1"></i>Simpan</button>
      </div>
    </div>
  </div>
</div>

<script>
(function(){
  var SVC='<%=svcLK%>', BOLEH=<%=bolehLK%>, data=[], jenis=[], toko=[], gudang=[], modal=null;
  function esc(s){return (s==null?'':String(s)).replace(/[&<>"]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
  function jenisBadge(d){ if(!d.jenisNama) return '<span class="text-muted small">—</span>';
    var w=d.jenisWarna||'#0d6efd', ic=d.jenisIkon||'fas fa-location-dot';
    return '<span class="lk-badge" style="background:'+esc(w)+'"><i class="'+esc(ic)+'"></i>'+esc(d.jenisNama)+'</span>'; }
  window.lkRender<%=rlk%>=function(){
    var q=(document.getElementById('lkCari<%=rlk%>').value||'').toLowerCase();
    var fj=document.getElementById('lkFilter<%=rlk%>').value;
    var b=document.getElementById('lkBody<%=rlk%>'), h='';
    var rows=data.filter(function(d){
      if(fj && String(d.jenisId)!==String(fj)) return false;
      return !q || (d.nama+' '+d.alamat+' '+d.tokoNama+' '+d.jenisNama).toLowerCase().indexOf(q)>=0; });
    if(!rows.length){ b.innerHTML='<tr><td colspan="7" class="text-center text-muted py-4">Belum ada data.</td></tr>'; return; }
    rows.forEach(function(d){
      var gudangTxt=d.gudangNama?(esc(d.gudangNama)+(d.gudangIndukNama?' <span class="text-muted">(induk: '+esc(d.gudangIndukNama)+')</span>':'')):'<span class="text-muted">—</span>';
      h+='<tr>'
        +'<td class="fw-semibold">'+esc(d.nama)+(d.keterangan?'<div class="small text-muted fw-normal">'+esc(d.keterangan)+'</div>':'')+'</td>'
        +'<td>'+jenisBadge(d)+'</td>'
        +'<td class="small">'+(d.tokoNama?esc(d.tokoNama):'<span class="text-muted">—</span>')+'</td>'
        +'<td class="small">'+gudangTxt+'</td>'
        +'<td class="small text-muted">'+esc(d.alamat||'')+'</td>'
        +'<td class="text-center">'+(d.aktif?'<span class="badge bg-success-subtle text-success">Aktif</span>':'<span class="badge bg-secondary-subtle text-secondary">Nonaktif</span>')+'</td>';
      if(BOLEH){ h+='<td class="text-center">'
        + aksiBarisMenu([
            { ikon: 'fa-pen',   label: 'Ubah',  onclick: 'lkForm<%=rlk%>('+JSON.stringify(d)+')' },
            { ikon: 'fa-trash', label: 'Hapus', onclick: 'lkHapus<%=rlk%>('+d.id+')', merusak: true }
        ]) + '</td>'; }
      h+='</tr>';
    });
    b.innerHTML=h;
  };
  function fillSelect(el, list, withEmpty, emptyLabel){
    var h=withEmpty?'<option value="">'+emptyLabel+'</option>':'';
    list.forEach(function(x){ h+='<option value="'+x.id+'">'+esc(x.nama)+'</option>'; }); el.innerHTML=h;
  }
  function load(){ fetch(SVC+'&aksi=list').then(function(r){return r.json();}).then(function(j){
    data=(j&&j.data)||[]; jenis=(j&&j.jenis)||[]; toko=(j&&j.toko)||[]; gudang=(j&&j.gudang)||[];
    fillSelect(document.getElementById('lkFilter<%=rlk%>'), jenis, true, 'Semua jenis');
    window.lkRender<%=rlk%>();
  }); }
  window.lkForm<%=rlk%>=function(d){
    if(!BOLEH) return;
    fillSelect(document.getElementById('lkJenis<%=rlk%>'), jenis, true, '— pilih —');
    fillSelect(document.getElementById('lkToko<%=rlk%>'), toko, true, '— (opsional) —');
    fillSelect(document.getElementById('lkGudang<%=rlk%>'), gudang, true, '— (tanpa) —');
    document.getElementById('lkId<%=rlk%>').value=d?d.id:'';
    document.getElementById('lkNama<%=rlk%>').value=d?d.nama:'';
    document.getElementById('lkJenis<%=rlk%>').value=d&&d.jenisId?d.jenisId:'';
    document.getElementById('lkToko<%=rlk%>').value=d&&d.tokoId?d.tokoId:'';
    document.getElementById('lkGudang<%=rlk%>').value=d&&d.gudangId?d.gudangId:'';
    document.getElementById('lkAlamat<%=rlk%>').value=d?(d.alamat||''):'';
    document.getElementById('lkKet<%=rlk%>').value=d?(d.keterangan||''):'';
    document.getElementById('lkAktif<%=rlk%>').checked=d?!!d.aktif:true;
    document.getElementById('lkModalTitle<%=rlk%>').textContent=d?'Ubah Lokasi':'Tambah Lokasi';
    modal=modal||new bootstrap.Modal(document.getElementById('lkModal<%=rlk%>')); modal.show();
  };
  window.lkSimpan<%=rlk%>=function(){
    var nama=document.getElementById('lkNama<%=rlk%>').value.trim();
    if(!nama){ tampilkanPesanGagalFormal("penyimpanan Lokasi", '<%=Common.getBahasaConfigJS("Kolom Nama belum diisi, padahal wajib diisi.")%>', ["Isi kolom Nama Lokasi terlebih dahulu.", "Setelah terisi, silakan simpan kembali."]); return; }
    var p=new URLSearchParams();
    p.append('aksi','simpan'); p.append('id',document.getElementById('lkId<%=rlk%>').value);
    p.append('nama',nama); p.append('jenisId',document.getElementById('lkJenis<%=rlk%>').value);
    p.append('tokoId',document.getElementById('lkToko<%=rlk%>').value);
    p.append('gudangId',document.getElementById('lkGudang<%=rlk%>').value);
    p.append('alamat',document.getElementById('lkAlamat<%=rlk%>').value); p.append('keterangan',document.getElementById('lkKet<%=rlk%>').value);
    p.append('aktif',document.getElementById('lkAktif<%=rlk%>').checked?'true':'false');
    fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:p.toString()})
      .then(function(r){return r.json();}).then(function(j){ if(j.status==='00'){ modal.hide(); load(); } else tampilkanPesanGagalFormal("pengelolaan data Lokasi", j.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); });
  };
  window.lkHapus<%=rlk%>=function(id){
    if(!confirm('<%= Common.getBahasaConfigJS("Apakah Bapak/Ibu yakin ingin menghapus lokasi ini?") %>')) return;
    var p=new URLSearchParams(); p.append('aksi','hapus'); p.append('id',id);
    fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:p.toString()})
      .then(function(r){return r.json();}).then(function(j){ if(j.status==='00'){ load(); } else tampilkanPesanGagalFormal("pengelolaan data Lokasi", j.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); });
  };
  load();
})();
</script>
