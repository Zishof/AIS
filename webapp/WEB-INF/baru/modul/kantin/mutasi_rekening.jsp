<%--
  Editor Rekening Koran Bank (untuk Rekonsiliasi Bank). Pilih akun bank, entri baris mutasi rekening
  koran (tanggal, keterangan, masuk, keluar, referensi), lalu centang "cocok" bila sudah direkonsiliasi
  dengan buku. Endpoint: kantin/mutasi_rekening_service.jsp. Hanya admin. Laporan "Ikhtisar Rekonsiliasi
  Bank" & "Mutasi Belum Cocok" ada di menu Laporan-Laporan. Tabel auto-terbentuk saat RESTART.
--%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser uMR = Common.getCurrentUser(request);
if (uMR==null || uMR.getUserId()==null){ response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); out.print("Unauthorized"); return; }
boolean bolehMR = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
String SVC = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin&s=mutasi_rekening_service";
String R = Common.getGeneratedBarCode(6);
%>
<div class="mrk">
  <div class="mb-2">
    <h5 class="fw-bold mb-0"><i class="fas fa-scale-balanced text-primary me-2"></i><%=Common.getBahasaConfig("Rekening Koran & Rekonsiliasi Bank")%></h5>
    <div class="text-muted small"><%=Common.getBahasaConfig("Entri baris rekening koran dari bank, lalu centang bila sudah cocok dengan buku. Laporan Ikhtisar Rekonsiliasi & Mutasi Belum Cocok ada di menu Laporan-Laporan.")%></div>
  </div>
  <div class="row g-2 mb-2">
    <div class="col-md-6">
      <label class="form-label small fw-bold mb-1"><%=Common.getBahasaConfig("Akun Bank/Kas")%></label>
      <select class="form-select" id="akun<%=R%>"></select>
    </div>
    <div class="col-md-3">
      <label class="form-label small fw-bold mb-1"><%=Common.getBahasaConfig("s/d Tanggal")%></label>
      <input type="date" class="form-control" id="sampai<%=R%>">
    </div>
  </div>

  <% if(bolehMR){ %>
  <div class="card border-0 shadow-sm rounded-4 mb-2"><div class="card-body p-2">
    <div class="fw-bold small text-uppercase text-muted mb-2"><i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Baris Rekening Koran")%></div>
    <div class="row g-2 align-items-end">
      <div class="col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Tanggal")%></label><input type="date" class="form-control form-control-sm" id="fTgl<%=R%>"></div>
      <div class="col-md-3"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Keterangan")%></label><input type="text" class="form-control form-control-sm" id="fKet<%=R%>"></div>
      <div class="col-md-2"><label class="form-label small mb-1"><%=Common.getBahasaConfig("Referensi")%></label><input type="text" class="form-control form-control-sm" id="fRef<%=R%>"></div>
      <div class="col-md-2"><label class="form-label small mb-1 text-success"><%=Common.getBahasaConfig("Masuk")%></label><input type="number" step="0.01" class="form-control form-control-sm text-end" id="fMasuk<%=R%>" value="0"></div>
      <div class="col-md-2"><label class="form-label small mb-1 text-danger"><%=Common.getBahasaConfig("Keluar")%></label><input type="number" step="0.01" class="form-control form-control-sm text-end" id="fKeluar<%=R%>" value="0"></div>
      <div class="col-md-1"><button class="btn btn-sm btn-primary w-100" id="btnAdd<%=R%>"><i class="fas fa-plus"></i></button></div>
    </div>
  </div></div>
  <% } %>

  <div class="card border-0 shadow-sm rounded-4"><div class="card-body p-2">
    <div class="table-responsive"><table class="table table-sm align-middle mb-0">
      <thead><tr class="small text-muted text-uppercase">
        <th style="width:60px" class="text-center"><%=Common.getBahasaConfig("Cocok")%></th>
        <th><%=Common.getBahasaConfig("Tanggal")%></th><th><%=Common.getBahasaConfig("Keterangan")%></th><th><%=Common.getBahasaConfig("Referensi")%></th>
        <th class="text-end"><%=Common.getBahasaConfig("Masuk")%></th><th class="text-end"><%=Common.getBahasaConfig("Keluar")%></th><% if(bolehMR){ %><th style="width:50px"></th><% } %>
      </tr></thead><tbody id="tbl<%=R%>"></tbody>
      <tfoot><tr class="fw-bold small"><td colspan="4" class="text-end"><%=Common.getBahasaConfig("Total")%></td><td class="text-end text-success" id="tMasuk<%=R%>">0</td><td class="text-end text-danger" id="tKeluar<%=R%>">0</td><% if(bolehMR){ %><td></td><% } %></tr></tfoot>
    </table></div>
  </div></div>
</div>
<script>
(function(){
  var SVC="<%=SVC%>", BOLEH=<%=bolehMR%>, RID="<%=R%>";
  var post=async function(p){ var b=Object.keys(p).map(function(k){return encodeURIComponent(k)+"="+encodeURIComponent(p[k]==null?"":p[k]);}).join("&");
    var r=await fetch(SVC,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:b}); return await r.json(); };
  var esc=function(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/"/g,"&quot;"); };
  var fmt=function(n){ return new Intl.NumberFormat('id-ID',{minimumFractionDigits:2,maximumFractionDigits:2}).format(Number(n)||0); };
  var g=function(id){ return document.getElementById(id+RID); };
  function akunNama(){ var s=g("akun"); return (s&&s.value)?s.options[s.selectedIndex].text:""; }

  var render=function(d){ var h="", tm=0, tk=0;
    if(!d.length){ h='<tr><td colspan="'+(BOLEH?7:6)+'" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Belum ada baris. Pilih akun & tambah di atas.")%></td></tr>'; }
    else d.forEach(function(x){ tm+=Number(x.masuk)||0; tk+=Number(x.keluar)||0;
      h+='<tr data-id="'+x.id+'">'
        +'<td class="text-center"><input type="checkbox" class="form-check-input ck" '+(x.rekon?'checked':'')+' '+(BOLEH?'':'disabled')+'></td>'
        +'<td>'+esc(x.tanggal)+'</td><td>'+esc(x.keterangan)+'</td><td class="text-muted small">'+esc(x.referensi)+'</td>'
        +'<td class="text-end text-success">'+(Number(x.masuk)?fmt(x.masuk):'')+'</td>'
        +'<td class="text-end text-danger">'+(Number(x.keluar)?fmt(x.keluar):'')+'</td>'
        +(BOLEH?'<td class="text-center"><button class="btn btn-sm btn-link text-danger p-0 del"><i class="fas fa-trash"></i></button></td>':'')
        +'</tr>'; });
    g("tbl").innerHTML=h; g("tMasuk").textContent=fmt(tm); g("tKeluar").textContent=fmt(tk);
    if(BOLEH){
      g("tbl").querySelectorAll(".ck").forEach(function(c){ c.onchange=async function(){ var id=c.closest("tr").getAttribute("data-id"); await post({aksi:"toggle",id:id,val:c.checked}); }; });
      g("tbl").querySelectorAll(".del").forEach(function(b){ b.onclick=async function(){ if(!confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus baris ini?")%>'))return; var id=b.closest("tr").getAttribute("data-id"); var r=await post({aksi:"hapus",id:id}); if(r.status=="00") load(); else alert(r.message||""); }; });
    }
  };
  var load=async function(){ var j=await post({aksi:"list",akun:g("akun").value||"",sampai:g("sampai").value||""}); if(j.status!="00"){ g("tbl").innerHTML='<tr><td colspan="7" class="text-danger small py-3">'+(j.message||"Gagal")+'</td></tr>'; return; } render(j.data||[]); };

  (async function(){
    var ja=await post({aksi:"listAkun"}); var opt='<option value="">-- semua akun --</option>';
    if(ja.status=="00") (ja.data||[]).forEach(function(a){ opt+='<option value="'+a.id+'">'+esc(a.kode)+' - '+esc(a.nama)+'</option>'; });
    g("akun").innerHTML=opt;
    g("akun").addEventListener("change",load); g("sampai").addEventListener("change",load);
    if(BOLEH) g("btnAdd").onclick=async function(){
      if(!g("akun").value){ tampilkanPesanGagalFormal("pencatatan Mutasi Rekening", '<%=Common.getBahasaConfigJS("Akun bank belum dipilih, padahal wajib dipilih.")%>', ["Pilih akun bank terlebih dahulu pada formulir ini."]); return; }
      var r=await post({aksi:"simpan",akun:g("akun").value,namaAkun:akunNama(),tanggal:g("fTgl").value||"",keterangan:g("fKet").value||"",referensi:g("fRef").value||"",masuk:g("fMasuk").value||0,keluar:g("fKeluar").value||0});
      if(r.status=="00"){ g("fKet").value=""; g("fRef").value=""; g("fMasuk").value=0; g("fKeluar").value=0; load(); } else tampilkanPesanGagalFormal("pencatatan Mutasi Rekening", r.message||'<%=Common.getBahasaConfigJS("Peladen menolak permintaan tanpa keterangan rinci.")%>', ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]);
    };
    load();
  })();
})();
</script>
