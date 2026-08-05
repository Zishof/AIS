<%--
  Editor Stok Minimum & Expired (JSP). Daftar produk dengan input stok minimum, batch, dan tanggal
  kedaluwarsa yang bisa disimpan per baris (hanya 3 field itu — aman, tak menyentuh field produk lain).
  Endpoint: kantin/stok_expired_service.jsp. Hanya admin. Laporan terkait di menu Laporan-Laporan.
--%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser uSE = Common.getCurrentUser(request);
if (uSE==null || uSE.getUserId()==null){ response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); out.print("Unauthorized"); return; }
boolean bolehSE = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
String SVC = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin&s=stok_expired_service";
String R = Common.getGeneratedBarCode(6);
%>
<div class="ste">
  <div class="mb-2">
    <h5 class="fw-bold mb-0"><i class="fas fa-calendar-xmark text-primary me-2"></i><%=Common.getBahasaConfig("Stok Minimum & Kadaluarsa")%></h5>
    <div class="text-muted small"><%=Common.getBahasaConfig("Isi batas stok minimum (untuk peringatan reorder), nomor batch, dan tanggal kedaluwarsa tiap produk. Laporan Reorder/Expired ada di menu Laporan-Laporan.")%></div>
  </div>
  <input type="text" class="form-control shadow-sm mb-2" id="q<%=R%>" placeholder="<%=Common.getBahasaConfig("Cari produk (kode/nama)...")%>">
  <div class="card border-0 shadow-sm rounded-4"><div class="card-body p-2">
    <div class="table-responsive"><table class="table table-sm align-middle mb-0">
      <thead><tr class="small text-muted text-uppercase">
        <th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Produk")%></th><th class="text-end"><%=Common.getBahasaConfig("Stok")%></th>
        <th style="width:120px"><%=Common.getBahasaConfig("Stok Min")%></th><th style="width:140px"><%=Common.getBahasaConfig("Batch")%></th><th style="width:160px"><%=Common.getBahasaConfig("Kadaluarsa")%></th><% if(bolehSE){ %><th style="width:70px"></th><% } %>
      </tr></thead><tbody id="tbl<%=R%>"></tbody>
    </table></div>
  </div></div>
</div>
<script>
(function(){
  var SVC="<%=SVC%>", BOLEH=<%=bolehSE%>;
  var post=async function(p){ var b=Object.keys(p).map(function(k){return encodeURIComponent(k)+"="+encodeURIComponent(p[k]==null?"":p[k]);}).join("&");
    var r=await fetch(SVC,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:b}); return await r.json(); };
  var esc=function(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/"/g,"&quot;"); };
  var render=function(d){ var h="";
    if(!d.length){ h='<tr><td colspan="'+(BOLEH?7:6)+'" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Tidak ada produk.")%></td></tr>'; }
    else d.forEach(function(x){
      h+='<tr data-id="'+x.id+'"><td class="fw-semibold">'+esc(x.kode)+'</td><td>'+esc(x.nama)+'</td><td class="text-end">'+(x.stok)+'</td>'
        +'<td><input type="number" step="0.01" class="form-control form-control-sm sm" value="'+(x.stokMin||0)+'" '+(BOLEH?'':'disabled')+'></td>'
        +'<td><input type="text" class="form-control form-control-sm bt" value="'+esc(x.batch)+'" '+(BOLEH?'':'disabled')+'></td>'
        +'<td><input type="date" class="form-control form-control-sm ex" value="'+esc(x.expired)+'" '+(BOLEH?'':'disabled')+'></td>'
        +(BOLEH?'<td><button class="btn btn-sm btn-primary sv"><i class="fas fa-save"></i></button></td>':'')+'</tr>'; });
    document.getElementById("tbl<%=R%>").innerHTML=h;
    if(BOLEH) document.querySelectorAll("#tbl<%=R%> .sv").forEach(function(b){ b.onclick=async function(){
      var tr=b.closest("tr"); var r=await post({aksi:"simpan",id:tr.getAttribute("data-id"),stokMin:tr.querySelector(".sm").value||0,batch:tr.querySelector(".bt").value||"",expired:tr.querySelector(".ex").value||""});
      b.innerHTML=(r.status=="00")?'<i class="fas fa-check"></i>':'<i class="fas fa-xmark"></i>'; if(r.status!="00") tampilkanPesanGagalFormal("penyimpanan stok kedaluwarsa (expired)", r.message||"Peladen menolak permintaan tanpa keterangan rinci.", ["Periksa kembali stok minimum/batch/tanggal kedaluwarsa yang dimasukkan.", "Ulangi proses beberapa saat lagi."]); setTimeout(function(){b.innerHTML='<i class="fas fa-save"></i>';},1500);
    }; });
  };
  var load=async function(){ var q=document.getElementById("q<%=R%>").value; var j=await post({aksi:"list",q:q}); if(j.status!="00"){ document.getElementById("tbl<%=R%>").innerHTML='<tr><td colspan="7" class="text-danger small py-3">'+(j.message||"Gagal")+'</td></tr>'; return; } render(j.data||[]); };
  var t; document.getElementById("q<%=R%>").addEventListener("input",function(){ clearTimeout(t); t=setTimeout(load,350); });
  load();
})();
</script>
