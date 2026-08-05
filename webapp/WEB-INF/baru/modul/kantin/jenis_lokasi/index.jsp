<%--
  Master "Jenis Lokasi" (versi JSP) — daftar + form tambah/ubah/hapus (modal), gate admin.
  Reuse endpoint jenis_lokasi/service.jsp (yang reuse LokasiKantinUtil). Aman di-include berkali-kali
  (semua id diberi akhiran rnd unik). Tombol ubah/hapus hanya tampil bila boleh mengelola.
--%>
<%@page import="ais.common.Common"%>
<%@page import="ais.action.master.koperasi.helper.LokasiKantinUtil"%>
<%
String rjl = ais.common.Common.getGeneratedBarCode(6);
boolean bolehJL = LokasiKantinUtil.bolehKelola(request);
String svcJL = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fjenis_lokasi&s=service";
%>
<div class="jl-wrap-<%=rjl%>">
  <style>
    .jl-wrap-<%=rjl%> .jl-badge{display:inline-flex;align-items:center;gap:6px;padding:2px 10px;border-radius:999px;color:#fff;font-weight:600;font-size:.8rem}
    .jl-wrap-<%=rjl%> .jl-off{opacity:.5}
    .jl-wrap-<%=rjl%> table td{vertical-align:middle}
  </style>

  <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
    <div>
      <h5 class="fw-bold mb-0"><i class="fas fa-tags text-primary me-2"></i><%=Common.getBahasaConfig("Jenis Lokasi")%></h5>
      <div class="text-muted small"><%=Common.getBahasaConfig("Daftar macam tempat: Gudang, Outlet, Kasir, Entry Data, dan lainnya. Dipakai untuk menggolongkan setiap lokasi.")%></div>
    </div>
    <div class="d-flex gap-2">
      <input id="jlCari<%=rjl%>" class="form-control form-control-sm" style="max-width:220px" placeholder="Cari…" oninput="jlRender<%=rjl%>()"/>
      <% if (bolehJL) { %>
      <button class="btn btn-primary btn-sm" onclick="jlForm<%=rjl%>(null)"><i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah")%></button>
      <% } %>
    </div>
  </div>

  <% if (!bolehJL) { %>
  <div class="alert alert-light border small py-2"><i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Anda hanya dapat melihat. Perubahan hanya untuk admin (bukan pedagang/toko).")%></div>
  <% } %>

  <div class="table-responsive">
    <table class="table table-hover table-sm align-middle">
      <thead class="table-light"><tr>
        <th style="width:60px" class="text-center">#</th>
        <th><%=Common.getBahasaConfig("Nama")%></th>
        <th><%=Common.getBahasaConfig("Keterangan")%></th>
        <th style="width:90px" class="text-center"><%=Common.getBahasaConfig("Aktif")%></th>
        <% if (bolehJL) { %><th style="width:110px" class="text-center"><%=Common.getBahasaConfig("Aksi")%></th><% } %>
      </tr></thead>
      <tbody id="jlBody<%=rjl%>"><tr><td colspan="5" class="text-center text-muted py-4"><i class="fas fa-spinner fa-spin me-1"></i>Memuat…</td></tr></tbody>
    </table>
  </div>
</div>

<!-- Modal Form -->
<div class="modal fade" id="jlModal<%=rjl%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content border-0 shadow rounded-4">
      <div class="modal-header"><h6 class="modal-title fw-bold" id="jlModalTitle<%=rjl%>">Jenis Lokasi</h6>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
      <div class="modal-body">
        <input type="hidden" id="jlId<%=rjl%>"/>
        <div class="mb-2"><label class="form-label small fw-semibold"><%= Common.getBahasaConfig("Nama *") %></label>
          <input id="jlNama<%=rjl%>" class="form-control" maxlength="255" placeholder="mis. Gudang"/></div>
        <div class="mb-2"><label class="form-label small fw-semibold"><%= Common.getBahasaConfig("Keterangan") %></label>
          <textarea id="jlKet<%=rjl%>" class="form-control" rows="2"></textarea></div>
        <div class="row g-2">
          <div class="col-6"><label class="form-label small fw-semibold"><%= Common.getBahasaConfig("Warna") %></label>
            <input type="color" id="jlWarna<%=rjl%>" class="form-control form-control-color w-100" value="#0d6efd"/></div>
          <div class="col-6"><label class="form-label small fw-semibold"><%= Common.getBahasaConfig("Urutan") %></label>
            <input type="number" id="jlUrut<%=rjl%>" class="form-control" value="0"/></div>
        </div>
        <div class="mb-2 mt-2"><label class="form-label small fw-semibold">Ikon <span class="text-muted">(FontAwesome, mis. fas fa-warehouse)</span></label>
          <input id="jlIkon<%=rjl%>" class="form-control" placeholder="fas fa-warehouse"/></div>
        <div class="form-check form-switch"><input class="form-check-input" type="checkbox" id="jlAktif<%=rjl%>" checked/>
          <label class="form-check-label small" for="jlAktif<%=rjl%>"><%= Common.getBahasaConfig("Aktif") %></label></div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-light btn-sm" data-bs-dismiss="modal"><%= Common.getBahasaConfig("Batal") %></button>
        <button class="btn btn-primary btn-sm" onclick="jlSimpan<%=rjl%>()"><i class="fas fa-save me-1"></i>Simpan</button>
      </div>
    </div>
  </div>
</div>

<script>
(function(){
  var SVC='<%=svcJL%>', BOLEH=<%=bolehJL%>, data=[], modal=null;
  function esc(s){return (s==null?'':String(s)).replace(/[&<>"]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
  window.jlRender<%=rjl%>=function(){
    var q=(document.getElementById('jlCari<%=rjl%>').value||'').toLowerCase();
    var b=document.getElementById('jlBody<%=rjl%>'), h='';
    var rows=data.filter(function(d){return !q || (d.nama+' '+d.keterangan).toLowerCase().indexOf(q)>=0;});
    if(!rows.length){ b.innerHTML='<tr><td colspan="5" class="text-center text-muted py-4">Belum ada data.</td></tr>'; return; }
    rows.forEach(function(d){
      var warna=d.warna||'#0d6efd', ikon=d.ikon||'fas fa-location-dot';
      h+='<tr>'
        +'<td class="text-center text-muted">'+(d.urutan||0)+'</td>'
        +'<td><span class="jl-badge '+(d.aktif?'':'jl-off')+'" style="background:'+esc(warna)+'"><i class="'+esc(ikon)+'"></i>'+esc(d.nama)+'</span></td>'
        +'<td class="small text-muted">'+esc(d.keterangan||'')+'</td>'
        +'<td class="text-center">'+(d.aktif?'<span class="badge bg-success-subtle text-success">Aktif</span>':'<span class="badge bg-secondary-subtle text-secondary">Nonaktif</span>')+'</td>';
      if(BOLEH){ h+='<td class="text-center">'
        +'<button class="btn btn-sm btn-outline-secondary border-0" title="Ubah" onclick=\'jlForm<%=rjl%>('+JSON.stringify(d)+')\'><i class="fas fa-pen"></i></button> '
        +'<button class="btn btn-sm btn-outline-danger border-0" title="Hapus" onclick="jlHapus<%=rjl%>('+d.id+")\"><i class=\"fas fa-trash\"></i></button></td>"; }
      h+='</tr>';
    });
    b.innerHTML=h;
  };
  function load(){ fetch(SVC+'&aksi=list').then(function(r){return r.json();}).then(function(j){ data=(j&&j.data)||[]; window.jlRender<%=rjl%>(); }); }
  window.jlForm<%=rjl%>=function(d){
    if(!BOLEH) return;
    document.getElementById('jlId<%=rjl%>').value=d?d.id:'';
    document.getElementById('jlNama<%=rjl%>').value=d?d.nama:'';
    document.getElementById('jlKet<%=rjl%>').value=d?(d.keterangan||''):'';
    document.getElementById('jlWarna<%=rjl%>').value=d&&d.warna?d.warna:'#0d6efd';
    document.getElementById('jlIkon<%=rjl%>').value=d&&d.ikon?d.ikon:'';
    document.getElementById('jlUrut<%=rjl%>').value=d?(d.urutan||0):0;
    document.getElementById('jlAktif<%=rjl%>').checked=d?!!d.aktif:true;
    document.getElementById('jlModalTitle<%=rjl%>').textContent=d?'Ubah Jenis Lokasi':'Tambah Jenis Lokasi';
    modal=modal||new bootstrap.Modal(document.getElementById('jlModal<%=rjl%>')); modal.show();
  };
  window.jlSimpan<%=rjl%>=function(){
    var nama=document.getElementById('jlNama<%=rjl%>').value.trim();
    if(!nama){ tampilkanPesanGagalFormal("penyimpanan Jenis Lokasi", '<%=Common.getBahasaConfigJS("Kolom Nama belum diisi, padahal wajib diisi.")%>', ["Isi kolom Nama Jenis Lokasi terlebih dahulu.", "Setelah terisi, silakan simpan kembali."]); return; }
    var p=new URLSearchParams();
    p.append('aksi','simpan'); p.append('id',document.getElementById('jlId<%=rjl%>').value);
    p.append('nama',nama); p.append('keterangan',document.getElementById('jlKet<%=rjl%>').value);
    p.append('warna',document.getElementById('jlWarna<%=rjl%>').value); p.append('ikon',document.getElementById('jlIkon<%=rjl%>').value);
    p.append('urutan',document.getElementById('jlUrut<%=rjl%>').value); p.append('aktif',document.getElementById('jlAktif<%=rjl%>').checked?'true':'false');
    fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:p.toString()})
      .then(function(r){return r.json();}).then(function(j){ if(j.status==='00'){ modal.hide(); load(); } else tampilkanPesanGagalFormal("pengelolaan data Jenis Lokasi", j.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); });
  };
  window.jlHapus<%=rjl%>=function(id){
    if(!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus jenis lokasi ini?")%>')) return;
    var p=new URLSearchParams(); p.append('aksi','hapus'); p.append('id',id);
    fetch(SVC,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:p.toString()})
      .then(function(r){return r.json();}).then(function(j){ if(j.status==='00'){ load(); } else tampilkanPesanGagalFormal("pengelolaan data Jenis Lokasi", j.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); });
  };
  load();
})();
</script>
