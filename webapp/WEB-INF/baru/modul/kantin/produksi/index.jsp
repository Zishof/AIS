<%--
  Produksi Kantin (JSP). Form (menu, tanggal, porsi rencana/dibuat/terjual/sisa/waste) + daftar
  dengan ubah/hapus. Hanya admin. Endpoint: kantin/produksi/service.jsp. Laporan di Laporan-Laporan.
--%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser uP = Common.getCurrentUser(request);
if (uP==null || uP.getUserId()==null){ response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); out.print("Unauthorized"); return; }
boolean bolehP = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
String SVC = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fproduksi&s=service";
String R = Common.getGeneratedBarCode(6);
%>
<div class="prd">
  <div class="mb-2">
    <h5 class="fw-bold mb-0"><i class="fas fa-utensils text-primary me-2"></i><%=Common.getBahasaConfig("Produksi Kantin")%></h5>
    <div class="text-muted small"><%=Common.getBahasaConfig("Catat produksi menu harian: porsi rencana, dibuat, terjual, sisa, dan waste. Laporan Realisasi/Waste ada di menu Laporan-Laporan.")%></div>
  </div>
<% if (bolehP) { %>
  <div class="card border-0 shadow-sm rounded-4 mb-3"><div class="card-body p-3">
    <input type="hidden" id="id<%=R%>">
    <div class="row g-2">
      <div class="col-md-4"><label class="small text-muted"><%=Common.getBahasaConfig("Menu")%></label><select id="produk<%=R%>" class="form-select form-select-sm"></select></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Tanggal")%></label><input id="tanggal<%=R%>" type="date" class="form-control form-control-sm"></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Rencana")%></label><input id="rencana<%=R%>" type="number" step="0.01" class="form-control form-control-sm" value="0"></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Dibuat")%></label><input id="dibuat<%=R%>" type="number" step="0.01" class="form-control form-control-sm" value="0"></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Terjual")%></label><input id="terjual<%=R%>" type="number" step="0.01" class="form-control form-control-sm" value="0"></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Sisa")%></label><input id="sisa<%=R%>" type="number" step="0.01" class="form-control form-control-sm" value="0"></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Waste")%></label><input id="waste<%=R%>" type="number" step="0.01" class="form-control form-control-sm" value="0"></div>
      <div class="col-md-4"><label class="small text-muted"><%=Common.getBahasaConfig("Keterangan")%></label><input id="ket<%=R%>" class="form-control form-control-sm"></div>
      <div class="col-md-4 d-flex align-items-end gap-2"><button class="btn btn-primary btn-sm fw-bold" id="save<%=R%>"><i class="fas fa-save me-1"></i><%=Common.getBahasaConfig("Simpan")%></button><button class="btn btn-light btn-sm" id="reset<%=R%>"><%=Common.getBahasaConfig("Reset")%></button></div>
    </div>
  </div></div>
<% } %>
  <div class="card border-0 shadow-sm rounded-4"><div class="card-body p-2">
    <div class="table-responsive"><table class="table table-sm table-hover align-middle mb-0">
      <thead><tr class="small text-muted text-uppercase"><th><%=Common.getBahasaConfig("Tanggal")%></th><th><%=Common.getBahasaConfig("Menu")%></th><th class="text-end"><%=Common.getBahasaConfig("Rencana")%></th><th class="text-end"><%=Common.getBahasaConfig("Dibuat")%></th><th class="text-end"><%=Common.getBahasaConfig("Terjual")%></th><th class="text-end"><%=Common.getBahasaConfig("Sisa")%></th><th class="text-end"><%=Common.getBahasaConfig("Waste")%></th><% if(bolehP){ %><th></th><% } %></tr></thead>
      <tbody id="tbl<%=R%>"></tbody>
    </table></div>
  </div></div>
</div>
<script>
(function(){
  var SVC="<%=SVC%>", BOLEH=<%=bolehP%>, g=function(id){return document.getElementById(id+"<%=R%>");};
  var rp=function(n){ return new Intl.NumberFormat('id-ID',{maximumFractionDigits:2}).format(Number(n)||0); };
  var post=async function(p){ var b=Object.keys(p).map(function(k){return encodeURIComponent(k)+"="+encodeURIComponent(p[k]==null?"":p[k]);}).join("&");
    var r=await fetch(SVC,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:b}); return await r.json(); };
  var loadProduk=async function(){ if(!BOLEH)return; var j=await post({aksi:"produk"}); var s=g("produk"); s.innerHTML="";
    (j.data||[]).forEach(function(t){ var o=document.createElement("option"); o.value=t.id; o.text=t.nama; s.appendChild(o); }); };
  var reset=function(){ if(!BOLEH)return; g("id").value=""; ["rencana","dibuat","terjual","sisa","waste"].forEach(function(k){g(k).value="0";}); g("tanggal").value=""; g("ket").value=""; };
  var loadList=async function(){ var j=await post({aksi:"list"}); var d=j.data||[]; var h="";
    if(!d.length){ h='<tr><td colspan="'+(BOLEH?8:7)+'" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Belum ada catatan produksi.")%></td></tr>'; }
    else d.forEach(function(x){ h+='<tr><td class="small">'+x.tanggal+'</td><td class="fw-semibold">'+x.menu+'</td><td class="text-end">'+rp(x.rencana)+'</td><td class="text-end">'+rp(x.dibuat)+'</td><td class="text-end">'+rp(x.terjual)+'</td><td class="text-end">'+rp(x.sisa)+'</td><td class="text-end fw-bold '+(x.waste>0?"text-danger":"")+'">'+rp(x.waste)+'</td>'
      +(BOLEH?('<td class="text-end">' + aksiBarisMenu([{ ikon: 'fa-pen', label: 'Ubah', onclick: 'ed<%=R%>('+JSON.stringify(x)+')' }, { ikon: 'fa-trash', label: 'Hapus', onclick: 'del<%=R%>('+x.id+')', merusak: true }]) + '</td>'):'')+'</tr>'; });
    g("tbl").innerHTML=h; };
  window["ed<%=R%>"]=function(x){ if(!BOLEH)return; g("id").value=x.id; if(x.produkId)g("produk").value=x.produkId; g("rencana").value=x.rencana||0; g("dibuat").value=x.dibuat||0; g("terjual").value=x.terjual||0; g("sisa").value=x.sisa||0; g("waste").value=x.waste||0; g("ket").value=x.keterangan||""; window.scrollTo(0,0); };
  window["del<%=R%>"]=async function(id){ if(!confirm("<%=Common.getBahasaConfig("Hapus data ini?")%>"))return; var r=await post({aksi:"hapus",id:id}); if(r.status=="00"){ tampilkanPesanSuksesFormal("penghapusan data produksi", r.message||""); } else { tampilkanPesanGagalFormal("penghapusan data produksi", r.message||"Peladen menolak permintaan tanpa keterangan rinci.", ["Ulangi proses beberapa saat lagi."]); } loadList(); };
<% if (bolehP) { %>
  g("reset").onclick=reset;
  g("save").onclick=async function(){ var r=await post({aksi:"simpan",id:g("id").value||"",produkId:g("produk").value||"",tanggal:g("tanggal").value||"",rencana:g("rencana").value||0,dibuat:g("dibuat").value||0,terjual:g("terjual").value||0,sisa:g("sisa").value||0,waste:g("waste").value||0,keterangan:g("ket").value||""}); if(r.status=="00"){ tampilkanPesanSuksesFormal("penyimpanan data produksi", r.message||""); reset(); loadList(); } else { tampilkanPesanGagalFormal("penyimpanan data produksi", r.message||"Peladen menolak permintaan tanpa keterangan rinci.", ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); } };
  loadProduk();
<% } %>
  loadList();
})();
</script>
