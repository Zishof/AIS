<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); return;
}
String rnd = Common.getGeneratedBarCode(7);

Mahasiswa mhs = tbmuser.getMahasiswa();
Long idMhsLogin = (mhs != null) ? mhs.getId() : null;
Jurusan jurusan = tbmuser.getJurusan();
Fakultas fakultas = tbmuser.getFakultas();

String joinClause = " LEFT JOIN public.kegiatan_kemahasiswaan_punya_mahasiswa p ON p.kegiatankemahasiswaan = k.id ";
String baseWhere = " WHERE k.id IS NOT NULL ";
if (idMhsLogin != null) {
    baseWhere += " AND p.mahasiswa = " + idMhsLogin;
} else if (jurusan != null) {
    baseWhere += " AND k.jurusan = " + jurusan.getId();
} else if (fakultas != null) {
    baseWhere += " AND k.fakultas = " + fakultas.getId();
}
%>

<div class="animate__animated animate__fadeInUp" id="dashboardKegiatanMhs<%=rnd%>">
    <div class="row g-3 mb-4" id="kpiRow<%=rnd%>">
        <div class="col-12 text-center py-4">
            <div class="spinner-border text-primary"></div>
            <p class="text-muted small mt-2"><%=Common.getBahasaConfig("Memuat ringkasan...")%></p>
        </div>
    </div>
    <div class="row g-3">
        <div class="col-md-6">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-header bg-white border-0 pt-3 pb-0 px-4">
                    <h6 class="fw-bold text-dark mb-0"><i class="fas fa-chart-pie me-2 text-primary"></i><%=Common.getBahasaConfig("Distribusi Status")%></h6>
                </div>
                <div class="card-body px-4 pb-3">
                    <div id="chartStatus<%=rnd%>" style="min-height:160px;"></div>
                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-header bg-white border-0 pt-3 pb-0 px-4">
                    <h6 class="fw-bold text-dark mb-0"><i class="fas fa-list me-2 text-info"></i><%=Common.getBahasaConfig("Kegiatan Terbaru")%></h6>
                </div>
                <div class="card-body px-4 pb-3">
                    <div id="listTerbaru<%=rnd%>" style="max-height:220px;overflow-y:auto;">
                        <div class="text-center text-muted small py-3"><div class="spinner-border spinner-border-sm text-info me-2"></div></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
(function(){
    var ROOT = '<%=Common.ROOT%>';
    var join = '<%=joinClause.replace("'","\\'"  )%>';
    var wh   = '<%=baseWhere.replace("'",  "\\'")%>';

    async function sql(q) {
        try {
            var r = await fetch(ROOT+'/Data',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({action:'sql',sql:q})});
            return (await r.json()).data||[];
        } catch(e){return[];}
    }

    function badge(st){
        var m={'Disetujui':'bg-success','Ditolak':'bg-danger','Sedang diproses':'bg-info text-dark','Belum diproses':'bg-warning text-dark'};
        return '<span class="badge '+(m[st]||'bg-secondary')+'">'+( st||'-')+'</span>';
    }

    function renderKpi(total,ok,proses){
        var pend=total-ok-proses, pct=total>0?Math.round(ok*100/total):0;
        document.getElementById('kpiRow<%=rnd%>').innerHTML=
            '<div class="col-6 col-md-3"><div class="card border-0 shadow-sm rounded-4 text-center p-3 border-top border-primary border-3">'+
            '<div style="font-size:2rem;font-weight:900;color:#0d6efd;">'+total+'</div><small class="text-muted fw-semibold"><%=Common.getBahasaConfig("Total Kegiatan")%></small></div></div>'+
            '<div class="col-6 col-md-3"><div class="card border-0 shadow-sm rounded-4 text-center p-3 border-top border-success border-3">'+
            '<div style="font-size:2rem;font-weight:900;color:#198754;">'+ok+'</div><small class="text-muted fw-semibold"><%=Common.getBahasaConfig("Disetujui")%></small></div></div>'+
            '<div class="col-6 col-md-3"><div class="card border-0 shadow-sm rounded-4 text-center p-3 border-top border-warning border-3">'+
            '<div style="font-size:2rem;font-weight:900;color:#ffc107;">'+pend+'</div><small class="text-muted fw-semibold"><%=Common.getBahasaConfig("Belum/Proses")%></small></div></div>'+
            '<div class="col-6 col-md-3"><div class="card border-0 shadow-sm rounded-4 text-center p-3 border-top border-info border-3">'+
            '<div style="font-size:2rem;font-weight:900;color:#0dcaf0;">'+pct+'%</div><small class="text-muted fw-semibold"><%=Common.getBahasaConfig("Tingkat Persetujuan")%></small></div></div>';
    }

    function renderStatus(rows){
        var el=document.getElementById('chartStatus<%=rnd%>');
        if(!rows||!rows.length){el.innerHTML='<p class="text-muted text-center small py-4"><%=Common.getBahasaConfig("Tidak ada data.")%></p>';return;}
        var tot=rows.reduce(function(s,r){return s+parseInt(r.jml||0);},0);
        var clr={'Disetujui':'#198754','Ditolak':'#dc3545','Sedang diproses':'#0dcaf0','Belum diproses':'#ffc107'};
        var h='<div class="d-flex flex-column gap-2 mt-2">';
        rows.forEach(function(r){
            var j=parseInt(r.jml||0),p=tot>0?Math.round(j*100/tot):0,st=r.status||'Belum diproses';
            h+='<div><div class="d-flex justify-content-between small fw-semibold mb-1"><span>'+st+'</span><span>'+j+' ('+p+'%)</span></div>'+
               '<div class="progress" style="height:10px;border-radius:5px;"><div class="progress-bar" style="width:'+p+'%;background:'+(clr[st]||'#6c757d')+';border-radius:5px;"></div></div></div>';
        });
        el.innerHTML=h+'</div>';
    }

    function renderTerbaru(rows){
        var el=document.getElementById('listTerbaru<%=rnd%>');
        if(!rows||!rows.length){el.innerHTML='<p class="text-muted text-center small py-3"><%=Common.getBahasaConfig("Belum ada data.")%></p>';return;}
        var h='<ul class="list-group list-group-flush">';
        rows.forEach(function(r){
            h+='<li class="list-group-item px-0 py-2 border-0 border-bottom">'+
               '<div class="d-flex justify-content-between align-items-start gap-2">'+
               '<div class="fw-semibold text-dark" style="font-size:.85rem;line-height:1.3;">'+(r.nama||'-')+'</div>'+badge(r.status)+'</div>'+
               '<small class="text-muted">'+(r.tempat||'')+(r.nama_mahasiswa?' — '+r.nama_mahasiswa:'')+'</small></li>';
        });
        el.innerHTML=h+'</ul>';
    }

    async function load(){
        var base='FROM public.kegiatan_kemahasiswaan k '+join+wh;
        var totals=await sql('SELECT COUNT(DISTINCT k.id) as total, SUM(CASE WHEN k.status=\'Disetujui\' THEN 1 ELSE 0 END) as disetujui, SUM(CASE WHEN k.status=\'Sedang diproses\' THEN 1 ELSE 0 END) as proses '+base);
        if(totals.length){var t=totals[0];renderKpi(parseInt(t.total||0),parseInt(t.disetujui||0),parseInt(t.proses||0));}
        renderStatus(await sql('SELECT k.status, COUNT(*) as jml '+base+' GROUP BY k.status ORDER BY jml DESC'));
        renderTerbaru(await sql('SELECT k.nama, k.status, k.tempat, m.nama as nama_mahasiswa FROM public.kegiatan_kemahasiswaan k '+join+' LEFT JOIN public.mahasiswa m ON p.mahasiswa=m.id '+wh+' ORDER BY k.id DESC LIMIT 10'));
    }

    document.addEventListener('DOMContentLoaded',load);
})();
</script>
