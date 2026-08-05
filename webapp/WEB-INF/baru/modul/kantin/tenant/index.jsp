<%--
  Setoran & Bagi Hasil Tenant (JSP). Form input (tenant, periode, omzet, %bagi hasil, sewa, biaya,
  setoran) + daftar dengan ubah/hapus. Hanya admin (bukan pedagang) yang boleh mengubah.
  Endpoint: kantin/tenant/service.jsp. Perhitungan kewajiban & status di TenantSetoranUtil.
--%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser uT = Common.getCurrentUser(request);
if (uT==null || uT.getUserId()==null){ response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); out.print("Unauthorized"); return; }
boolean bolehT = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
String SVC = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Ftenant&s=service";
String R = Common.getGeneratedBarCode(6);
%>
<style>
.tnt .card{border:1px solid #e9eef5;border-radius:14px}
.tnt .st-LUNAS{background:#dcfce7;color:#166534}.tnt .st-KURANG{background:#fee2e2;color:#991b1b}
.tnt .badge2{padding:2px 8px;border-radius:999px;font-size:11px;font-weight:700}
</style>
<div class="tnt">
  <div class="d-flex align-items-center justify-content-between mb-2 flex-wrap gap-2">
    <div>
      <h5 class="fw-bold mb-0"><i class="fas fa-store text-primary me-2"></i><%=Common.getBahasaConfig("Setoran & Bagi Hasil Tenant")%></h5>
      <div class="text-muted small"><%=Common.getBahasaConfig("Catat kewajiban (bagi hasil + sewa + biaya) dan setoran tiap tenant/stan per periode.")%></div>
    </div>
  </div>

<% if (bolehT) { %>
  <div class="card shadow-sm mb-3"><div class="card-body p-3">
    <input type="hidden" id="id<%=R%>">
    <div class="row g-2">
      <div class="col-md-3"><label class="small text-muted"><%=Common.getBahasaConfig("Tenant / Stan")%></label><select id="toko<%=R%>" class="form-select form-select-sm"></select></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Periode")%></label><input id="periode<%=R%>" class="form-control form-control-sm" placeholder="2026-07"></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Tanggal")%></label><input id="tanggal<%=R%>" type="date" class="form-control form-control-sm"></div>
      <div class="col-md-3"><label class="small text-muted"><%=Common.getBahasaConfig("Omzet")%></label><input id="omzet<%=R%>" type="number" class="form-control form-control-sm" value="0"></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("% Bagi Hasil")%></label><input id="persen<%=R%>" type="number" class="form-control form-control-sm" value="0"></div>
      <div class="col-md-3"><label class="small text-muted"><%=Common.getBahasaConfig("Bagi Hasil (Rp, opsional)")%></label><input id="bagihasil<%=R%>" type="number" class="form-control form-control-sm" value="0"></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Sewa")%></label><input id="sewa<%=R%>" type="number" class="form-control form-control-sm" value="0"></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Biaya Layanan")%></label><input id="biaya<%=R%>" type="number" class="form-control form-control-sm" value="0"></div>
      <div class="col-md-2"><label class="small text-muted"><%=Common.getBahasaConfig("Setoran")%></label><input id="setoran<%=R%>" type="number" class="form-control form-control-sm" value="0"></div>
      <div class="col-md-3"><label class="small text-muted"><%=Common.getBahasaConfig("Keterangan")%></label><input id="ket<%=R%>" class="form-control form-control-sm"></div>
    </div>
    <div class="mt-2 d-flex gap-2 align-items-center">
      <button class="btn btn-primary btn-sm fw-bold" id="btnSave<%=R%>"><i class="fas fa-save me-1"></i><%=Common.getBahasaConfig("Simpan")%></button>
      <button class="btn btn-light btn-sm" id="btnReset<%=R%>"><%=Common.getBahasaConfig("Reset")%></button>
      <span class="text-muted small ms-2"><%=Common.getBahasaConfig("Kewajiban")%>: <b id="kw<%=R%>">0</b></span>
    </div>
  </div></div>
<% } %>

  <div class="card shadow-sm"><div class="card-body p-2">
    <div class="table-responsive"><table class="table table-sm table-hover align-middle mb-0">
      <thead><tr class="small text-muted text-uppercase">
        <th><%=Common.getBahasaConfig("Tenant")%></th><th><%=Common.getBahasaConfig("Periode")%></th><th><%=Common.getBahasaConfig("Tanggal")%></th>
        <th class="text-end"><%=Common.getBahasaConfig("Omzet")%></th><th class="text-end"><%=Common.getBahasaConfig("Kewajiban")%></th>
        <th class="text-end"><%=Common.getBahasaConfig("Setoran")%></th><th class="text-end"><%=Common.getBahasaConfig("Sisa")%></th><th><%=Common.getBahasaConfig("Status")%></th><% if(bolehT){ %><th></th><% } %>
      </tr></thead><tbody id="tbl<%=R%>"></tbody>
    </table></div>
  </div></div>
</div>

<script>
(function(){
  var SVC="<%=SVC%>", BOLEH=<%=bolehT%>;
  var rp=function(n){ return new Intl.NumberFormat('id-ID',{maximumFractionDigits:0}).format(Number(n)||0); };
  var g=function(id){ return document.getElementById(id+"<%=R%>"); };
  var post=async function(p){ var b=Object.keys(p).map(function(k){return encodeURIComponent(k)+"="+encodeURIComponent(p[k]==null?"":p[k]);}).join("&");
    var r=await fetch(SVC,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:b}); return await r.json(); };

  var loadToko=async function(){ if(!BOLEH) return; var j=await post({aksi:"toko"}); var s=g("toko"); s.innerHTML="";
    (j.data||[]).forEach(function(t){ var o=document.createElement("option"); o.value=t.id; o.text=t.nama; s.appendChild(o); }); };
  var hitKw=function(){ if(!BOLEH) return; var bh=Number(g("bagihasil").value)||0; if(bh<=0){ bh=(Number(g("omzet").value)||0)*(Number(g("persen").value)||0)/100; }
    g("kw").innerText=rp(bh+(Number(g("sewa").value)||0)+(Number(g("biaya").value)||0)); };
  var reset=function(){ if(!BOLEH) return; g("id").value=""; ["periode","omzet","persen","bagihasil","sewa","biaya","setoran","ket"].forEach(function(k){ g(k).value=(k=="periode"||k=="ket")?"":"0"; }); g("periode").value=""; g("ket").value=""; g("tanggal").value=""; hitKw(); };

  var loadList=async function(){ var j=await post({aksi:"list"}); var d=j.data||[]; var h="";
    if(!d.length){ h='<tr><td colspan="'+(BOLEH?9:8)+'" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Belum ada data setoran tenant.")%></td></tr>'; }
    else d.forEach(function(x){ var sisa=(Number(x.kewajiban)||0)-(Number(x.setoran)||0);
      h+='<tr><td class="fw-semibold">'+x.tenant+'</td><td>'+x.periode+'</td><td class="small">'+x.tanggal+'</td>'
        +'<td class="text-end">'+rp(x.omzet)+'</td><td class="text-end">'+rp(x.kewajiban)+'</td><td class="text-end">'+rp(x.setoran)+'</td>'
        +'<td class="text-end fw-bold '+(sisa>0?"text-danger":"text-success")+'">'+rp(sisa)+'</td>'
        +'<td><span class="badge2 st-'+x.status+'">'+x.status+'</span></td>'
        +(BOLEH?('<td class="text-end"><button class="btn btn-sm btn-link p-0 me-2" onclick=\'ed<%=R%>('+JSON.stringify(x)+')\'><i class="fas fa-pen"></i></button><button class="btn btn-sm btn-link text-danger p-0" onclick="del<%=R%>('+x.id+')"><i class="fas fa-trash"></i></button></td>'):'')
        +'</tr>'; });
    g("tbl").innerHTML=h; };

  window["ed<%=R%>"]=function(x){ if(!BOLEH) return; g("id").value=x.id; if(x.tokoId) g("toko").value=x.tokoId; g("periode").value=x.periode||""; g("omzet").value=x.omzet||0; g("persen").value=x.persen||0; g("bagihasil").value=x.bagihasil||0; g("sewa").value=x.sewa||0; g("biaya").value=x.biaya||0; g("setoran").value=x.setoran||0; g("ket").value=x.keterangan||""; hitKw(); window.scrollTo(0,0); };
  window["del<%=R%>"]=async function(id){ if(!confirm("<%=Common.getBahasaConfig("Hapus data ini?")%>"))return; var r=await post({aksi:"hapus",id:id}); alert(r.message||""); loadList(); };

<% if (bolehT) { %>
  ["omzet","persen","bagihasil","sewa","biaya"].forEach(function(k){ g(k).addEventListener("input",hitKw); });
  g("btnReset").onclick=reset;
  g("btnSave").onclick=async function(){
    var p={aksi:"simpan",id:g("id").value||"",tokoId:g("toko").value||"",periode:g("periode").value||"",tanggal:g("tanggal").value||"",
      omzet:g("omzet").value||0,persen:g("persen").value||0,bagihasil:g("bagihasil").value||0,sewa:g("sewa").value||0,biaya:g("biaya").value||0,setoran:g("setoran").value||0,keterangan:g("ket").value||""};
    var r=await post(p); if(r.status=="00"){ tampilkanPesanSuksesFormal("penyimpanan data tenant", r.message||""); reset(); loadList(); } else { tampilkanPesanGagalFormal("penyimpanan data tenant", r.message||"Peladen menolak permintaan tanpa keterangan rinci.", ["Periksa kembali data isian pada formulir.", "Ulangi proses beberapa saat lagi."]); }
  };
  loadToko();
<% } %>
  loadList();
})();
</script>
