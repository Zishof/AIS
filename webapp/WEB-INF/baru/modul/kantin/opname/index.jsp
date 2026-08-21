<%--
  Jadwal Stock Opname (JSP). Form (toko, kode, tgl rencana, kategori, petugas, status) + daftar
  dengan ubah/hapus. Hanya admin yang boleh mengubah. Endpoint: kantin/opname/service.jsp.
  Hasil & Berita Acara ada di menu Laporan-Laporan (kategori Stock Opname).
--%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser uO = Common.getCurrentUser(request);
if (uO==null || uO.getUserId()==null){ response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); out.print("Unauthorized"); return; }
boolean bolehO = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
String SVC = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fopname&s=service";
String R = Common.getGeneratedBarCode(6);
%>
<style>.opn .st-RENCANA{background:#e0e7ff;color:#3730a3}.opn .st-BERJALAN{background:#fef3c7;color:#92400e}.opn .st-SELESAI{background:#dcfce7;color:#166534}.opn .b2{padding:2px 8px;border-radius:999px;font-size:11px;font-weight:700}</style>
<div class="opn">
  <div class="d-flex align-items-center justify-content-between mb-2 flex-wrap gap-2">
    <div>
      <h5 class="fw-bold mb-0"><i class="fas fa-clipboard-list text-primary me-2"></i><%=Common.getBahasaConfig("Jadwal Stock Opname")%></h5>
      <div class="text-muted small"><%=Common.getBahasaConfig("Rencanakan kegiatan opname. Hasil & Berita Acara ada di menu Laporan-Laporan (Stock Opname).")%></div>
    </div>
  </div>
<% if (bolehO) { %>
  <div class="card border-0 shadow-sm rounded-4 mb-3"><div class="card-body p-3">
    <input type="hidden" id="id<%=R%>">
    <div class="row g-2">
      <div class="col-md-3"><label class="small text-muted"><%=Common.getBahasaConfig("Toko/Lokasi")%></label><select id="toko<%=R%>" class="form-select form-select-sm"></select></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Kode")%></label><input id="kode<%=R%>" class="form-control form-control-sm" placeholder="OPN-2026-07"></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Tgl Rencana")%></label><input id="rencana<%=R%>" type="date" class="form-control form-control-sm"></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Kategori")%></label><input id="kategori<%=R%>" class="form-control form-control-sm"></div>
      <div class="col-md-3"><label class="small text-muted"><%=Common.getBahasaConfig("Petugas")%></label><input id="petugas<%=R%>" class="form-control form-control-sm"></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Status")%></label>
        <select id="status<%=R%>" class="form-select form-select-sm"><option>RENCANA</option><option>BERJALAN</option><option>SELESAI</option></select></div>
      <div class="col-md-7"><label class="small text-muted"><%=Common.getBahasaConfig("Keterangan")%></label><input id="ket<%=R%>" class="form-control form-control-sm"></div>
      <div class="col-md-3 d-flex align-items-end gap-2"><button class="btn btn-primary btn-sm fw-bold w-100" id="save<%=R%>"><i class="fas fa-save me-1"></i><%=Common.getBahasaConfig("Simpan")%></button>
        <button class="btn btn-light btn-sm" id="reset<%=R%>"><%=Common.getBahasaConfig("Reset")%></button></div>
    </div>
  </div></div>
<% } %>
  <div class="card border-0 shadow-sm rounded-4"><div class="card-body p-2">
    <div class="table-responsive"><table class="table table-sm table-hover align-middle mb-0">
      <thead><tr class="small text-muted text-uppercase"><th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Toko")%></th><th><%=Common.getBahasaConfig("Rencana")%></th><th><%=Common.getBahasaConfig("Kategori")%></th><th><%=Common.getBahasaConfig("Petugas")%></th><th><%=Common.getBahasaConfig("Status")%></th><% if(bolehO){ %><th></th><% } %></tr></thead>
      <tbody id="tbl<%=R%>"></tbody>
    </table></div>
  </div></div>
</div>
<script>
(function(){
  var SVC="<%=SVC%>", BOLEH=<%=bolehO%>, g=function(id){return document.getElementById(id+"<%=R%>");};
  var post=async function(p){ var b=Object.keys(p).map(function(k){return encodeURIComponent(k)+"="+encodeURIComponent(p[k]==null?"":p[k]);}).join("&");
    var r=await fetch(SVC,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:b}); return await r.json(); };
  var loadToko=async function(){ if(!BOLEH)return; var j=await post({aksi:"toko"}); var s=g("toko"); s.innerHTML="";
    (j.data||[]).forEach(function(t){ var o=document.createElement("option"); o.value=t.id; o.text=t.nama; s.appendChild(o); }); };
  var reset=function(){ if(!BOLEH)return; g("id").value=""; ["kode","kategori","petugas","ket"].forEach(function(k){g(k).value="";}); g("rencana").value=""; g("status").value="RENCANA"; };
  var loadList=async function(){ var j=await post({aksi:"list"}); var d=j.data||[]; var h="";
    if(!d.length){ h='<tr><td colspan="'+(BOLEH?7:6)+'" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Belum ada jadwal opname.")%></td></tr>'; }
    else d.forEach(function(x){ h+='<tr><td class="fw-semibold">'+(x.kode||'-')+'</td><td>'+x.toko+'</td><td class="small">'+x.rencana+'</td><td>'+(x.kategori||'-')+'</td><td>'+(x.petugas||'-')+'</td><td><span class="b2 st-'+x.status+'">'+x.status+'</span></td>'
      +(BOLEH?('<td class="text-end">' + aksiBarisMenu([{ ikon: 'fa-pen', label: 'Ubah', onclick: 'ed<%=R%>('+JSON.stringify(x)+')' }, { ikon: 'fa-trash', label: 'Hapus', onclick: 'del<%=R%>('+x.id+')', merusak: true }]) + '</td>'):'')+'</tr>'; });
    g("tbl").innerHTML=h; };
  window["ed<%=R%>"]=function(x){ if(!BOLEH)return; g("id").value=x.id; if(x.tokoId)g("toko").value=x.tokoId; g("kode").value=x.kode||""; g("kategori").value=x.kategori||""; g("petugas").value=x.petugas||""; g("status").value=x.status||"RENCANA"; g("ket").value=x.keterangan||""; window.scrollTo(0,0); };
  window["del<%=R%>"]=async function(id){ if(!confirm("<%=Common.getBahasaConfig("Hapus jadwal ini?")%>"))return; var r=await post({aksi:"hapus",id:id}); if(r.status=="00"){ tampilkanPesanSuksesFormal("penghapusan jadwal opname", r.message||""); } else { tampilkanPesanGagalFormal("penghapusan jadwal opname", r.message||"Peladen menolak permintaan tanpa keterangan rinci.", ["Ulangi proses beberapa saat lagi."]); } loadList(); };
<% if (bolehO) { %>
  g("reset").onclick=reset;
  g("save").onclick=async function(){ var r=await post({aksi:"simpan",id:g("id").value||"",tokoId:g("toko").value||"",kode:g("kode").value||"",rencana:g("rencana").value||"",kategori:g("kategori").value||"",petugas:g("petugas").value||"",status:g("status").value||"RENCANA",keterangan:g("ket").value||""}); if(r.status=="00"){ tampilkanPesanSuksesFormal("penyimpanan jadwal opname", r.message||""); reset(); loadList(); } else { tampilkanPesanGagalFormal("penyimpanan jadwal opname", r.message||"Peladen menolak permintaan tanpa keterangan rinci.", ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); } };
  loadToko();
<% } %>
  loadList();
})();
</script>
