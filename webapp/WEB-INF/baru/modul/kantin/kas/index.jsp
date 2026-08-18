<%--
  Buka / Tutup Kas Kasir (JSP). Memuat status sesi kas berjalan, form buka (modal awal) atau tutup
  (input uang fisik -> selisih otomatis), dan riwayat sesi. Endpoint: kantin/kas/service.jsp.
--%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser uKas = Common.getCurrentUser(request);
if (uKas == null || uKas.getUserId() == null) { response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); out.print("Unauthorized"); return; }
String SVC = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fkas&s=service";
String R = Common.getGeneratedBarCode(6);
%>
<style>
.kas .card{border:1px solid #e9eef5;border-radius:14px}
.kas .big{font-size:22px;font-weight:800}
.kas .lbl{font-size:11px;color:#64748b;text-transform:uppercase;font-weight:700}
.kas .sel-pos{color:#16a34a}.kas .sel-neg{color:#dc2626}
</style>
<div class="kas">
  <div class="d-flex align-items-center justify-content-between mb-2 flex-wrap gap-2">
    <h5 class="fw-bold mb-0"><i class="fas fa-cash-register text-primary me-2"></i><%=Common.getBahasaConfig("Buka / Tutup Kas Kasir")%></h5>
  </div>
  <div class="text-muted small mb-3"><%=Common.getBahasaConfig("Buka kas dengan modal awal, lalu tutup kas untuk mencocokkan uang fisik dengan penjualan tunai. Selisih dihitung otomatis.")%></div>

  <div id="panel<%=R%>" class="card shadow-sm mb-3"><div class="card-body p-4 text-center text-muted"><%=Common.getBahasaConfig("Memuat…")%></div></div>

  <div class="card shadow-sm"><div class="card-body p-3">
    <h6 class="fw-bold mb-2"><i class="fas fa-clock-rotate-left me-2"></i><%=Common.getBahasaConfig("Riwayat Sesi Kas")%></h6>
    <div class="table-responsive"><table class="table table-sm align-middle">
      <thead><tr class="small text-muted text-uppercase">
        <th><%=Common.getBahasaConfig("Kasir")%></th><th><%=Common.getBahasaConfig("Buka")%></th><th><%=Common.getBahasaConfig("Tutup")%></th>
        <th class="text-end"><%=Common.getBahasaConfig("Modal")%></th><th class="text-end"><%=Common.getBahasaConfig("Tunai")%></th>
        <th class="text-end"><%=Common.getBahasaConfig("Uang Fisik")%></th><th class="text-end"><%=Common.getBahasaConfig("Selisih")%></th><th><%=Common.getBahasaConfig("Status")%></th>
      </tr></thead><tbody id="tbl<%=R%>"></tbody>
    </table></div>
  </div></div>
</div>

<script>
(function(){
  var SVC="<%=SVC%>";
  var rp=function(n){ return new Intl.NumberFormat('id-ID',{maximumFractionDigits:0}).format(Number(n)||0); };
  var post=async function(params){
    var body=Object.keys(params).map(function(k){return encodeURIComponent(k)+"="+encodeURIComponent(params[k]);}).join("&");
    var r=await fetch(SVC,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:body});
    return await r.json();
  };
  var panel=document.getElementById("panel<%=R%>");
  var esc=function(v){return String(v==null?'':v).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');};
  var tampilLaporan=function(l){
    l=l||{}; var metode=(l.metodePembayaran||[]).map(function(m){return '<b>'+esc(m.nama)+'</b><div class="d-flex justify-content-between"><span>'+Number(m.jumlahTransaksi||0)+'x Penerimaan</span><span>Rp '+rp(m.penerimaan)+'</span></div><div class="d-flex justify-content-between"><span>Retur</span><span>Rp '+rp(m.retur)+'</span></div><div class="d-flex justify-content-between fw-bold"><span>Total '+esc(m.nama)+'</span><span>Rp '+rp(m.total)+'</span></div><br>';}).join('');
    var isi='<div class="font-monospace small" style="max-width:390px;margin:auto"><div class="text-center fw-bold fs-5">LAPORAN TUTUP KAS</div><div class="text-center">'+esc(l.namaToko)+'</div><hr><div>Nama Kasir : '+esc(l.namaKasir)+'</div><div>Buka : '+esc(l.waktuBuka)+'</div><div>Tutup : '+esc(l.waktuTutup)+'</div><hr>'+
      '<div class="d-flex justify-content-between"><span>Modal Awal</span><span>Rp '+rp(l.modalAwal)+'</span></div><div class="d-flex justify-content-between"><span>Penjualan Tunai</span><span>Rp '+rp(l.penjualanTunai)+'</span></div><div class="d-flex justify-content-between"><span>Kas Seharusnya</span><span>Rp '+rp(l.kasSeharusnya)+'</span></div><div class="d-flex justify-content-between"><span>Jumlah Kas Tunai</span><span>Rp '+rp(l.jumlahKasTunai)+'</span></div><div class="d-flex justify-content-between fw-bold"><span>Selisih</span><span>Rp '+rp(l.selisih)+'</span></div><hr>'+
      '<div class="d-flex justify-content-between"><span>Retur Penjualan</span><span>Rp '+rp(l.returPenjualan)+'</span></div><div class="d-flex justify-content-between"><span>Biaya</span><span>Rp '+rp(l.biaya)+'</span></div><div class="d-flex justify-content-between"><span>Piutang ('+Number(l.jumlahTransaksiPiutang||0)+'x)</span><span>Rp '+rp(l.piutang)+'</span></div><hr>'+metode+'<hr><div class="d-flex justify-content-between fw-bold"><span>Jumlah Transaksi</span><span>'+Number(l.jumlahTransaksi||0)+'</span></div><div class="d-flex justify-content-between fw-bold fs-6"><span>Total Transaksi</span><span>Rp '+rp(l.totalTransaksi)+'</span></div></div>';
    var el=document.getElementById('lapKas<%=R%>'); if(!el){el=document.createElement('div');el.id='lapKas<%=R%>';el.className='modal fade';document.body.appendChild(el);} el.innerHTML='<div class="modal-dialog modal-dialog-scrollable"><div class="modal-content"><div class="modal-header"><h5>Laporan Tutup Kas</h5><button class="btn-close" data-bs-dismiss="modal"></button></div><div class="modal-body">'+isi+'</div><div class="modal-footer"><button id="printKas<%=R%>" class="btn btn-outline-success">Cetak Laporan</button><button class="btn btn-success" data-bs-dismiss="modal">Selesai</button></div></div></div>';
    document.getElementById('printKas<%=R%>').onclick=function(){var w=window.open('','_blank','width=480,height=720');if(!w)return;w.document.write('<style>@page{size:80mm auto;margin:4mm}body{font:11px monospace}.d-flex{display:flex}.justify-content-between{justify-content:space-between}.text-center{text-align:center}.fw-bold{font-weight:bold}hr{border:0;border-top:1px dashed}</style>'+isi+'<script>onload=function(){print()}<\/script>');w.document.close();}; bootstrap.Modal.getOrCreateInstance(el).show();
  };

  var render=async function(){
    var j=await post({aksi:"status"});
    var s=(j&&j.sesi)||{buka:false};
    if(!s.buka){
      panel.innerHTML='<div class="card-body p-4">'
        +'<div class="lbl mb-2"><%=Common.getBahasaConfig("Kas Tertutup")%></div>'
        +'<div class="row g-2 align-items-end">'
        +'<div class="col-md-4"><label class="lbl"><%=Common.getBahasaConfig("Modal Awal (Rp)")%></label><input id="ma<%=R%>" type="number" class="form-control" value="0"></div>'
        +'<div class="col-md-5"><label class="lbl"><%=Common.getBahasaConfig("Keterangan")%></label><input id="kb<%=R%>" type="text" class="form-control"></div>'
        +'<div class="col-md-3"><button class="btn btn-success w-100 fw-bold" id="btnBuka<%=R%>"><i class="fas fa-unlock me-1"></i><%=Common.getBahasaConfig("Buka Kas")%></button></div>'
        +'</div></div>';
      document.getElementById("btnBuka<%=R%>").onclick=async function(){
        var r=await post({aksi:"buka",modalAwal:document.getElementById("ma<%=R%>").value||0,keterangan:document.getElementById("kb<%=R%>").value||""});
        if(r && r.status==="00"){ tampilkanPesanSuksesFormal("pembukaan kas", r.message||""); } else { tampilkanPesanGagalFormal("pembukaan kas", (r&&r.message)||"Peladen menolak permintaan tanpa keterangan rinci.", ["Periksa kembali nominal modal awal yang dimasukkan.", "Ulangi proses pembukaan kas beberapa saat lagi."]); }
        render(); loadList();
      };
    } else {
      var seharusnya=Number(s.seharusnya)||0;
      panel.innerHTML='<div class="card-body p-4">'
        +'<div class="d-flex justify-content-between flex-wrap"><div class="lbl mb-2 text-success"><i class="fas fa-lock-open me-1"></i><%=Common.getBahasaConfig("Kas Terbuka sejak")%> '+s.waktuBuka+'</div></div>'
        +'<div class="row g-3 mb-3">'
        +'<div class="col-6 col-md-3"><div class="lbl"><%=Common.getBahasaConfig("Modal Awal")%></div><div class="big">'+rp(s.modal)+'</div></div>'
        +'<div class="col-6 col-md-3"><div class="lbl"><%=Common.getBahasaConfig("Penjualan Tunai")%></div><div class="big text-success">'+rp(s.tunai)+'</div></div>'
        +'<div class="col-6 col-md-3"><div class="lbl"><%=Common.getBahasaConfig("Non Tunai")%></div><div class="big text-primary">'+rp(s.nontunai)+'</div></div>'
        +'<div class="col-6 col-md-3"><div class="lbl"><%=Common.getBahasaConfig("Kas Seharusnya")%></div><div class="big">'+rp(seharusnya)+'</div></div>'
        +'</div>'
        +'<div class="row g-2 align-items-end">'
        +'<div class="col-md-3"><label class="lbl"><%=Common.getBahasaConfig("Uang Fisik (Rp)")%></label><input id="uf<%=R%>" type="number" class="form-control" value="'+Math.round(seharusnya)+'"></div>'
        +'<div class="col-md-3"><div class="lbl"><%=Common.getBahasaConfig("Selisih")%></div><div class="big" id="selP<%=R%>">0</div></div>'
        +'<div class="col-md-3"><label class="lbl"><%=Common.getBahasaConfig("Keterangan")%></label><input id="kt<%=R%>" type="text" class="form-control"></div>'
        +'<div class="col-md-3"><button class="btn btn-danger w-100 fw-bold" id="btnTutup<%=R%>"><i class="fas fa-lock me-1"></i><%=Common.getBahasaConfig("Tutup Kas")%></button></div>'
        +'</div></div>';
      var hit=function(){ var uf=Number(document.getElementById("uf<%=R%>").value)||0; var sel=uf-seharusnya; var el=document.getElementById("selP<%=R%>"); el.innerText=rp(sel); el.className="big "+(sel<0?"sel-neg":"sel-pos"); };
      document.getElementById("uf<%=R%>").oninput=hit; hit();
      document.getElementById("btnTutup<%=R%>").onclick=async function(){
        if(!confirm("<%=Common.getBahasaConfig("Tutup kas sekarang?")%>"))return;
        var r=await post({aksi:"tutup",uangFisik:document.getElementById("uf<%=R%>").value||0,keterangan:document.getElementById("kt<%=R%>").value||""});
        if(r && r.status==="00"){ tampilkanPesanSuksesFormal("penutupan kas", r.message||""); tampilLaporan(r.laporanTutupKas); } else { tampilkanPesanGagalFormal("penutupan kas", (r&&r.message)||"Peladen menolak permintaan tanpa keterangan rinci.", ["Periksa kembali nominal uang fisik yang dimasukkan.", "Ulangi proses penutupan kas beberapa saat lagi."]); }
        render(); loadList();
      };
    }
  };
  var loadList=async function(){
    var j=await post({aksi:"list"}); var d=(j&&j.data)||[]; var h="";
    if(!d.length){ h='<tr><td colspan="8" class="text-center text-muted py-3"><%=Common.getBahasaConfig("Belum ada sesi kas.")%></td></tr>'; }
    else d.forEach(function(x){ var sc=x.selisih<0?"sel-neg":(x.selisih>0?"sel-pos":"");
      h+='<tr><td class="fw-semibold">'+x.kasir+'</td><td class="small">'+x.buka+'</td><td class="small">'+(x.tutup||'-')+'</td>'
        +'<td class="text-end">'+rp(x.modal)+'</td><td class="text-end">'+rp(x.tunai)+'</td><td class="text-end">'+rp(x.fisik)+'</td>'
        +'<td class="text-end fw-bold '+sc+'">'+rp(x.selisih)+'</td><td><span class="badge '+(x.stat=='TUTUP'?'bg-secondary':'bg-success')+'">'+x.stat+'</span></td></tr>'; });
    document.getElementById("tbl<%=R%>").innerHTML=h;
  };
  render(); loadList();
})();
</script>
