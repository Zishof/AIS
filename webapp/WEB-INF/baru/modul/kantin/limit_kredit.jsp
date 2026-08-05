<%--
  Editor Limit Kredit Anggota (JSP). Daftar anggota dengan input plafon kredit yang bisa disimpan per
  baris (HANYA field limit_kredit — aman, tak menyentuh field anggota lain). Endpoint:
  kantin/limit_kredit_service.jsp. Hanya admin. Laporan "Limit & Sisa Kredit Pelanggan" ada di
  menu Laporan-Laporan. Kolom limit_kredit terbentuk otomatis saat RESTART (hbm2ddl=update).
--%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser uLK = Common.getCurrentUser(request);
if (uLK==null || uLK.getUserId()==null){ response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); out.print("Unauthorized"); return; }
boolean bolehLK = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
String SVC = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin&s=limit_kredit_service";
String R = Common.getGeneratedBarCode(6);
%>
<div class="lmk">
  <div class="mb-2">
    <h5 class="fw-bold mb-0"><i class="fas fa-hand-holding-dollar text-primary me-2"></i><%=Common.getBahasaConfig("Limit Kredit Anggota")%></h5>
    <div class="text-muted small"><%=Common.getBahasaConfig("Tetapkan plafon kredit (piutang maksimum) tiap anggota untuk belanja tempo. Laporan 'Limit & Sisa Kredit Pelanggan' ada di menu Laporan-Laporan.")%></div>
  </div>
  <input type="text" class="form-control shadow-sm mb-2" id="q<%=R%>" placeholder="<%=Common.getBahasaConfig("Cari anggota (kode / nama)...")%>">
  <div class="card border-0 shadow-sm rounded-4"><div class="card-body p-2">
    <div class="table-responsive"><table class="table table-sm align-middle mb-0">
      <thead><tr class="small text-muted text-uppercase">
        <th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Anggota")%></th>
        <th class="text-end"><%=Common.getBahasaConfig("Piutang Berjalan")%></th>
        <th style="width:180px"><%=Common.getBahasaConfig("Limit Kredit")%></th><% if(bolehLK){ %><th style="width:70px"></th><% } %>
      </tr></thead><tbody id="tbl<%=R%>"></tbody>
    </table></div>
  </div></div>
</div>
<script>
(function(){
  var SVC="<%=SVC%>", BOLEH=<%=bolehLK%>;
  var post=async function(p){ var b=Object.keys(p).map(function(k){return encodeURIComponent(k)+"="+encodeURIComponent(p[k]==null?"":p[k]);}).join("&");
    var r=await fetch(SVC,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:b}); return await r.json(); };
  var esc=function(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/"/g,"&quot;"); };
  var fmt=function(n){ return new Intl.NumberFormat('id-ID').format(Number(n)||0); };
  var render=function(d){ var h="";
    if(!d.length){ h='<tr><td colspan="'+(BOLEH?5:4)+'" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Tidak ada anggota.")%></td></tr>'; }
    else d.forEach(function(x){
      var lim=Number(x.limit)||0, piu=Number(x.piutang)||0; var over = lim>0 && piu>lim;
      h+='<tr data-id="'+x.id+'"><td class="fw-semibold">'+esc(x.kode)+'</td><td>'+esc(x.nama)+'</td>'
        +'<td class="text-end '+(over?'text-danger fw-bold':'')+'">'+fmt(piu)+(over?' <i class="fas fa-triangle-exclamation" title="Melebihi limit"></i>':'')+'</td>'
        +'<td><input type="number" step="1000" min="0" class="form-control form-control-sm lm text-end" value="'+lim+'" '+(BOLEH?'':'disabled')+'></td>'
        +(BOLEH?'<td><button class="btn btn-sm btn-primary sv"><i class="fas fa-save"></i></button></td>':'')+'</tr>'; });
    document.getElementById("tbl<%=R%>").innerHTML=h;
    if(BOLEH) document.querySelectorAll("#tbl<%=R%> .sv").forEach(function(b){ b.onclick=async function(){
      var tr=b.closest("tr"); var r=await post({aksi:"simpan",id:tr.getAttribute("data-id"),limit:tr.querySelector(".lm").value||0});
      b.innerHTML=(r.status=="00")?'<i class="fas fa-check"></i>':'<i class="fas fa-xmark"></i>'; if(r.status!="00") tampilkanPesanGagalFormal("penyimpanan limit kredit anggota", r.message||"Peladen menolak permintaan tanpa keterangan rinci.", ["Periksa kembali nilai limit kredit yang dimasukkan.", "Ulangi proses beberapa saat lagi."]); setTimeout(function(){b.innerHTML='<i class="fas fa-save"></i>';},1500);
    }; });
  };
  var load=async function(){ var q=document.getElementById("q<%=R%>").value; var j=await post({aksi:"list",q:q}); if(j.status!="00"){ document.getElementById("tbl<%=R%>").innerHTML='<tr><td colspan="5" class="text-danger small py-3">'+(j.message||"Gagal")+'</td></tr>'; return; } render(j.data||[]); };
  var t; document.getElementById("q<%=R%>").addEventListener("input",function(){ clearTimeout(t); t=setTimeout(load,350); });
  load();
})();
</script>
