<%@page import="ais.database.model.KegiatanKedosenan"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Pegawai"%>
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

// Pegawai yang juga menjabat sebagai dosen — scoped ke kegiatan kedosenan mereka
Dosen dosenRole = tbmuser.ambilDosen();
Long idDosenRole = (dosenRole != null) ? dosenRole.getId() : null;
Jurusan jurusan = tbmuser.getJurusan();
Fakultas fakultas = tbmuser.getFakultas();

String baseWhere = " WHERE k.id IS NOT NULL ";
if (idDosenRole != null) {
    baseWhere += " AND k.diajukan_oleh = " + idDosenRole;
} else if (jurusan != null) {
    baseWhere += " AND k.jurusan = " + jurusan.getId();
} else if (fakultas != null) {
    baseWhere += " AND k.fakultas = " + fakultas.getId();
}
%>

<div class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-secondary border-4">
                <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                    <h5 class="fw-bold text-dark mb-1">
                        <i class="fas fa-id-badge text-secondary me-2"></i><%=Common.getBahasaConfig("Pusat Kegiatan Pegawai")%>
                    </h5>
                    <small class="text-muted mb-3 d-block"><%=Common.getBahasaConfig("Pantau kegiatan dan pencapaian kedosenan pegawai.")%></small>
                    <ul class="nav nav-tabs border-bottom-0 flex-wrap" role="tablist">
                        <li class="nav-item">
                            <button class="nav-link active fw-bold text-secondary" data-bs-toggle="tab" data-bs-target="#dash-pane-<%=rnd%>" type="button">
                                <i class="fas fa-chart-pie me-2"></i><%=Common.getBahasaConfig("Ringkasan")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-bold text-secondary" data-bs-toggle="tab" data-bs-target="#data-pane-<%=rnd%>" type="button" onclick="loadDataPegawai<%=rnd%>(1)">
                                <i class="fas fa-list-ul me-2"></i><%=Common.getBahasaConfig("Data Kegiatan")%>
                            </button>
                        </li>
                    </ul>
                </div>
                <div class="card-body p-4 bg-light rounded-bottom-4">
                    <div class="tab-content">

                        <!-- Ringkasan -->
                        <div class="tab-pane fade show active" id="dash-pane-<%=rnd%>" role="tabpanel">
                            <div class="row g-3 mb-4" id="kpiRow<%=rnd%>">
                                <div class="col-12 text-center py-3"><div class="spinner-border text-secondary"></div></div>
                            </div>
                            <div class="row g-3">
                                <div class="col-md-6">
                                    <div class="card border-0 shadow-sm rounded-4 h-100">
                                        <div class="card-header bg-white border-0 pt-3 pb-0 px-4">
                                            <h6 class="fw-bold mb-0"><i class="fas fa-chart-pie me-2 text-secondary"></i><%=Common.getBahasaConfig("Distribusi Status")%></h6>
                                        </div>
                                        <div class="card-body px-4 pb-3"><div id="chartStatus<%=rnd%>" style="min-height:150px;"></div></div>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="card border-0 shadow-sm rounded-4 h-100">
                                        <div class="card-header bg-white border-0 pt-3 pb-0 px-4">
                                            <h6 class="fw-bold mb-0"><i class="fas fa-list me-2 text-info"></i><%=Common.getBahasaConfig("Kegiatan Terbaru")%></h6>
                                        </div>
                                        <div class="card-body px-4 pb-3">
                                            <div id="listTerbaru<%=rnd%>" style="max-height:200px;overflow-y:auto;">
                                                <div class="text-center py-3"><div class="spinner-border spinner-border-sm text-info"></div></div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Data Kegiatan -->
                        <div class="tab-pane fade" id="data-pane-<%=rnd%>" role="tabpanel">
                            <div class="card border-0 shadow-sm rounded-4 mb-3">
                                <div class="card-body p-3">
                                    <div class="d-flex flex-wrap gap-2 align-items-center">
                                        <input type="text" id="cariKegiatan<%=rnd%>" class="form-control form-control-sm" style="min-width:180px;flex:1 1 200px;" placeholder="<%=Common.getBahasaConfig("Cari nama kegiatan...")%>" oninput="loadDataPegawai<%=rnd%>(1)">
                                        <select id="filterStatus<%=rnd%>" class="form-select form-select-sm" style="max-width:180px;" onchange="loadDataPegawai<%=rnd%>(1)">
                                            <option value=""><%=Common.getBahasaConfig("-- Semua Status --")%></option>
                                            <option>Belum diproses</option><option>Sedang diproses</option>
                                            <option>Disetujui</option><option>Ditolak</option>
                                        </select>
                                        <button class="btn btn-sm btn-outline-secondary" onclick="document.getElementById('cariKegiatan<%=rnd%>').value='';document.getElementById('filterStatus<%=rnd%>').value='';loadDataPegawai<%=rnd%>(1)">
                                            <i class="fas fa-sync-alt"></i>
                                        </button>
                                    </div>
                                </div>
                            </div>
                            <div id="listDataPegawai<%=rnd%>">
                                <div class="text-center py-4"><div class="spinner-border text-secondary"></div></div>
                            </div>
                            <nav><ul class="pagination pagination-sm justify-content-end mt-3" id="pagPegawai<%=rnd%>"></ul></nav>
                        </div>

                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
(function(){
    var ROOT='<%=Common.ROOT%>';
    var baseWhere='<%=baseWhere.replace("'","\\'")%>';
    var page<%=rnd%>=1, limit<%=rnd%>=10, total<%=rnd%>=0;

    async function fetchSql(q){try{var r=await fetch(ROOT+'/Data',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({action:'sql',sql:q})});return(await r.json()).data||[];}catch(e){return[];}}
    function badge(st){var m={'Disetujui':'bg-success','Ditolak':'bg-danger','Sedang diproses':'bg-info text-dark','Belum diproses':'bg-warning text-dark'};return'<span class="badge '+(m[st]||'bg-secondary')+'">'+(st||'-')+'</span>';}

    async function loadDashboard(){
        var q='FROM public.kegiatan_kedosenan k LEFT JOIN public.dosen d ON k.diajukan_oleh=d.id '+baseWhere;
        var t=await fetchSql('SELECT COUNT(*) as total, SUM(CASE WHEN k.status=\'Disetujui\' THEN 1 ELSE 0 END) as ok, SUM(CASE WHEN k.status=\'Sedang diproses\' THEN 1 ELSE 0 END) as proses '+q);
        if(t.length){var x=t[0],tot=parseInt(x.total||0),ok=parseInt(x.ok||0),pr=parseInt(x.proses||0),pct=tot>0?Math.round(ok*100/tot):0;
            document.getElementById('kpiRow<%=rnd%>').innerHTML=
            '<div class="col-6 col-md-3"><div class="card border-0 shadow-sm rounded-4 text-center p-3 border-top border-secondary border-3"><div style="font-size:2rem;font-weight:900;color:#6c757d;">'+tot+'</div><small class="text-muted fw-semibold"><%=Common.getBahasaConfig("Total")%></small></div></div>'+
            '<div class="col-6 col-md-3"><div class="card border-0 shadow-sm rounded-4 text-center p-3 border-top border-success border-3"><div style="font-size:2rem;font-weight:900;color:#198754;">'+ok+'</div><small class="text-muted fw-semibold"><%=Common.getBahasaConfig("Disetujui")%></small></div></div>'+
            '<div class="col-6 col-md-3"><div class="card border-0 shadow-sm rounded-4 text-center p-3 border-top border-warning border-3"><div style="font-size:2rem;font-weight:900;color:#ffc107;">'+(tot-ok-pr)+'</div><small class="text-muted fw-semibold"><%=Common.getBahasaConfig("Belum/Proses")%></small></div></div>'+
            '<div class="col-6 col-md-3"><div class="card border-0 shadow-sm rounded-4 text-center p-3 border-top border-info border-3"><div style="font-size:2rem;font-weight:900;color:#0dcaf0;">'+pct+'%</div><small class="text-muted fw-semibold"><%=Common.getBahasaConfig("Tingkat Persetujuan")%></small></div></div>';
        }
        var sr=await fetchSql('SELECT k.status, COUNT(*) as jml '+q+' GROUP BY k.status ORDER BY jml DESC');
        var el=document.getElementById('chartStatus<%=rnd%>');
        if(!sr.length){el.innerHTML='<p class="text-muted text-center small py-4"><%=Common.getBahasaConfig("Tidak ada data.")%></p>';}else{
            var tot2=sr.reduce(function(s,r){return s+parseInt(r.jml||0);},0);
            var clr={'Disetujui':'#198754','Ditolak':'#dc3545','Sedang diproses':'#0dcaf0','Belum diproses':'#ffc107'};
            var h='<div class="d-flex flex-column gap-2 mt-2">';
            sr.forEach(function(r){var j=parseInt(r.jml||0),p=tot2>0?Math.round(j*100/tot2):0,st=r.status||'Belum diproses';
                h+='<div><div class="d-flex justify-content-between small fw-semibold mb-1"><span>'+st+'</span><span>'+j+' ('+p+'%)</span></div><div class="progress" style="height:10px;border-radius:5px;"><div class="progress-bar" style="width:'+p+'%;background:'+(clr[st]||'#6c757d')+';border-radius:5px;"></div></div></div>';});
            el.innerHTML=h+'</div>';
        }
        var tr=await fetchSql('SELECT k.nama, k.status, k.tempat, d.nama as nama_dosen FROM public.kegiatan_kedosenan k LEFT JOIN public.dosen d ON k.diajukan_oleh=d.id '+baseWhere+' ORDER BY k.id DESC LIMIT 8');
        var le=document.getElementById('listTerbaru<%=rnd%>');
        if(!tr.length){le.innerHTML='<p class="text-muted text-center small py-3"><%=Common.getBahasaConfig("Belum ada data.")%></p>';}else{
            var h2='<ul class="list-group list-group-flush">';
            tr.forEach(function(r){h2+='<li class="list-group-item px-0 py-2 border-0 border-bottom"><div class="d-flex justify-content-between align-items-start gap-2"><div class="fw-semibold text-dark" style="font-size:.85rem;">'+(r.nama||'-')+'</div>'+badge(r.status)+'</div><small class="text-muted">'+(r.nama_dosen||r.tempat||'')+'</small></li>';});
            le.innerHTML=h2+'</ul>';
        }
    }

    window['loadDataPegawai<%=rnd%>']=function(pg){
        page<%=rnd%>=pg||1;
        var cari=document.getElementById('cariKegiatan<%=rnd%>').value.trim();
        var st=document.getElementById('filterStatus<%=rnd%>').value;
        var w=baseWhere;
        if(cari) w+=" AND k.nama ILIKE '%"+cari.replace(/'/g,"''")+"'%'";
        if(st)   w+=" AND k.status='"+st.replace(/'/g,"''")+"'";
        var off=(page<%=rnd%>-1)*limit<%=rnd%>;
        var el=document.getElementById('listDataPegawai<%=rnd%>');
        el.innerHTML='<div class="text-center py-4"><div class="spinner-border spinner-border-sm text-secondary"></div></div>';
        Promise.all([
            fetchSql('SELECT COUNT(*) as total FROM public.kegiatan_kedosenan k '+w),
            fetchSql('SELECT k.id,k.nama,k.status,k.tempat,k.mulai,k.sampai,d.nama as nama_dosen FROM public.kegiatan_kedosenan k LEFT JOIN public.dosen d ON k.diajukan_oleh=d.id '+w+' ORDER BY k.id DESC LIMIT '+limit<%=rnd%>+' OFFSET '+off)
        ]).then(function(res){
            total<%=rnd%>=parseInt((res[0][0]||{}).total||0);
            var rows=res[1];
            if(!rows.length){el.innerHTML='<p class="text-center text-muted py-4"><%=Common.getBahasaConfig("Tidak ada data.")%></p>';renderPagPegawai<%=rnd%>();return;}
            var h='<div class="row g-3">';
            rows.forEach(function(r){
                h+='<div class="col-md-6"><div class="card border-0 shadow-sm rounded-3 h-100 p-3">'+
                   '<div class="d-flex justify-content-between align-items-start mb-2"><span class="fw-bold text-dark" style="font-size:.9rem;">'+(r.nama||'-')+'</span>'+badge(r.status)+'</div>'+
                   (r.nama_dosen?'<div class="text-muted small"><i class="fas fa-user me-1"></i>'+r.nama_dosen+'</div>':'')+
                   (r.tempat?'<div class="text-muted small"><i class="fas fa-map-marker-alt me-1"></i>'+r.tempat+'</div>':'')+
                   (r.mulai?'<div class="text-muted small"><i class="far fa-calendar me-1"></i>'+r.mulai+(r.sampai?' s/d '+r.sampai:'')+'</div>':'')+
                   '</div></div>';
            });
            el.innerHTML=h+'</div>';
            renderPagPegawai<%=rnd%>();
        });
    };

    function renderPagPegawai<%=rnd%>(){
        var pages=Math.ceil(total<%=rnd%>/limit<%=rnd%>);
        var el=document.getElementById('pagPegawai<%=rnd%>');
        if(pages<=1){el.innerHTML='';return;}
        var h='<li class="page-item'+(page<%=rnd%><=1?' disabled':'')+'"><a class="page-link" href="#" onclick="event.preventDefault();loadDataPegawai<%=rnd%>(page<%=rnd%>-1)">‹</a></li>';
        for(var i=Math.max(1,page<%=rnd%>-2);i<=Math.min(pages,page<%=rnd%>+2);i++)
            h+='<li class="page-item'+(i===page<%=rnd%>?' active':'')+'"><a class="page-link" href="#" onclick="event.preventDefault();loadDataPegawai<%=rnd%>('+i+')">'+i+'</a></li>';
        h+='<li class="page-item'+(page<%=rnd%>>=pages?' disabled':'')+'"><a class="page-link" href="#" onclick="event.preventDefault();loadDataPegawai<%=rnd%>(page<%=rnd%>+1)">›</a></li>';
        el.innerHTML=h;
    }

    document.addEventListener('DOMContentLoaded', loadDashboard);
})();
</script>
