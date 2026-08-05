<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) return;

boolean edit = false;
if (tbmuser != null && tbmuser.getUserId() != null) {
    boolean isNotStudentTeacher = tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
            && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null && tbmuser.getGuru() == null;
    if (isNotStudentTeacher) edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    if (Common.getApakahAdminLain(tbmuser) || tbmuser.getDosen() != null) edit = true;
}
boolean disableForm = !edit || tbmuser.getMahasiswa() != null || tbmuser.getDosen() != null;
String idKpmStr = request.getParameter("kurikulumPunyaMatakuliah");
if(idKpmStr == null || idKpmStr.trim().isEmpty()) return;
%>

<div class="card border-0 shadow-sm rounded-4 mb-4 animate__animated animate__fadeIn">
    <div class="card-body p-4">
        <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3 flex-wrap gap-3">
            <div>
                <h6 class="fw-bold text-primary mb-1"><i class="fas fa-list-alt me-2"></i><%=Common.getBahasaConfig("Rincian Pertemuan (RPS)")%></h6>
                <small class="text-muted"><%=Common.getBahasaConfig("Konfigurasikan jadwal, materi, metode, serta indikator evaluasi tiap pertemuan.")%></small>
            </div>
            
            <div class="d-flex gap-2">
                <button type="button" class="btn btn-outline-secondary rounded-pill fw-bold px-4 shadow-sm" onclick="window.reloadListRincian<%=rnd%>()" title="<%=Common.getBahasaConfig("Segarkan Data")%>">
                    <i class="fas fa-sync-alt me-2"></i><%=Common.getBahasaConfig("Refresh")%>
                </button>
                
                <% if(!disableForm) { %>
                <button type="button" id="btnGenRincianAi<%=rnd%>" class="btn rounded-pill fw-bold px-4 shadow-sm text-white" style="background-color:#7c3aed;" onclick="window.generateRincianAi<%=rnd%>()">
                    <i class="fas fa-magic me-2"></i><%=Common.getBahasaConfig("Generate Pertemuan via AI")%>
                </button>
                <button type="button" class="btn btn-primary rounded-pill fw-bold px-4 shadow-sm" onclick="window.bukaFormRincian<%=rnd%>('')">
                    <i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Tambah Rincian Baru")%>
                </button>
                <% } %>
            </div>
        </div>

        <div id="containerRincianList<%=rnd%>">
            <div class="text-center py-5"><div class="spinner-border text-primary" role="status"></div></div>
        </div>
    </div>
</div>

<div id="containerRincianForm<%=rnd%>"></div>

<script>
    var rootUrl<%=rnd%> = "<%=Common.ROOT%>";
    var idKpm<%=rnd%> = "<%=idKpmStr%>";

    // FUNGSI 1: MENGAMBIL TABEL LIST DENGAN CACHE BUSTER
    window.reloadListRincian<%=rnd%> = function() {
        var timeStamp = new Date().getTime(); // Mencegah Browser Caching
        var url = rootUrl<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_rincian_pertemuan_list&kurikulumPunyaMatakuliah=' + idKpm<%=rnd%> + '&var=<%=rnd%>&t=' + timeStamp;
        
        // Tampilkan loading spinner saat tombol refresh ditekan
        document.getElementById('containerRincianList<%=rnd%>').innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary" role="status"></div></div>';
        
        fetch(url).then(res => res.text()).then(html => {
            document.getElementById('containerRincianList<%=rnd%>').innerHTML = html;
            var scripts = document.getElementById('containerRincianList<%=rnd%>').querySelectorAll('script');
            scripts.forEach(s => {
                var ns = document.createElement('script');
                if(s.src) ns.src = s.src; else ns.textContent = s.textContent;
                document.body.appendChild(ns); document.body.removeChild(ns);
            });
        }).catch(e => console.error("Gagal memuat list", e));
    };

    // FUNGSI 2: MEMBUKA MODAL FORM (TAMBAH / EDIT) DENGAN CACHE BUSTER
    window.bukaFormRincian<%=rnd%> = function(keyData) {
        document.getElementById('containerRincianForm<%=rnd%>').innerHTML = '<div class="text-center p-3"><div class="spinner-border text-primary"></div></div>';
        var timeStamp = new Date().getTime(); // Mencegah Browser Caching
        var url = rootUrl<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_rincian_pertemuan_form&kurikulumPunyaMatakuliah=' + idKpm<%=rnd%> + '&keyData=' + keyData + '&var=<%=rnd%>&t=' + timeStamp;
        fetch(url).then(res => res.text()).then(html => {
            document.getElementById('containerRincianForm<%=rnd%>').innerHTML = html;
            var scripts = document.getElementById('containerRincianForm<%=rnd%>').querySelectorAll('script');
            scripts.forEach(s => {
                var ns = document.createElement('script');
                if(s.src) ns.src = s.src; else ns.textContent = s.textContent;
                document.body.appendChild(ns); document.body.removeChild(ns);
            });
        }).catch(e => console.error("Gagal memuat form", e));
    };

    // GENERATE Pertemuan via AI
    window.generateRincianAi<%=rnd%> = function() {
        var jml = prompt('<%=Common.getBahasaConfigJS("Berapa pertemuan yang ingin di-generate? (kosongkan untuk melengkapi hingga 16)")%>', '');
        var btn = document.getElementById('btnGenRincianAi<%=rnd%>');
        var asli = btn ? btn.innerHTML : '';
        if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("AI menyusun pertemuan...")%>'; }
        var body = new URLSearchParams();
        body.append('kurikulumPunyaMatakuliah', idKpm<%=rnd%>);
        if (jml !== null && String(jml).trim() !== '') body.append('jumlah', String(jml).trim());
        fetch(rootUrl<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_generate_rincian_ai', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: body.toString() })
        .then(function(r){ return r.json(); })
        .then(function(d){
            if (!d || d.status !== 'success') throw new Error(d && d.message ? d.message : '<%=Common.getBahasaConfigJS("Gagal generate pertemuan.")%>');
            if (typeof tampilkanToast === 'function') tampilkanToast(d.message || '<%=Common.getBahasaConfigJS("Pertemuan berhasil dibuat via AI.")%>', 'bg-success text-white');
            window.reloadListRincian<%=rnd%>();
        })
        .catch(function(err){ console.error(err); if (typeof tampilkanToast === 'function') tampilkanToast('AI: ' + (err && err.message ? err.message : err), 'bg-danger text-white'); })
        .finally(function(){ if (btn) { btn.disabled = false; btn.innerHTML = asli; } });
    };

    // Init Load pertama kali
    window.reloadListRincian<%=rnd%>();
</script>