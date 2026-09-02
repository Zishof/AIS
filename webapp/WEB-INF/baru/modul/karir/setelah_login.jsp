<%@page import="ais.database.model.recruitment.CalonPegawai"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.KarirConfigUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String lk(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String rnd = Common.getGeneratedBarCode(7);
    CalonPegawai calon = null;
    try { calon = (CalonPegawai) request.getSession().getAttribute("KARIR_LOGGED_IN"); } catch(Exception e) { calon = null; }
    if (calon == null) { try { calon = (CalonPegawai) request.getSession().getAttribute("CalonPegawai"); } catch(Exception e) { calon = null; } }
    if (calon == null) calon = KarirConfigUtil.resolveLoggedCandidate(request);
    String teksDashboard = KarirConfigUtil.text("karir_dashboard_deskripsi", "Pantau dokumen, jadwal, dan hasil seleksi tanpa perlu datang ke kantor.");
    String teksPengumumanPanel = KarirConfigUtil.text("karir_dashboard_pengumuman_deskripsi", "Ringkasan jadwal, lokasi, dan keputusan seleksi ditampilkan di tabel ini.");
    String teksProgresPanel = KarirConfigUtil.text("karir_dashboard_progres_deskripsi", "Tahapan ini membantu Anda melihat posisi proses seleksi saat ini.");
    String teksDokumenPanel = KarirConfigUtil.text("karir_dashboard_dokumen_deskripsi", "Dokumen yang belum disetujui masih dapat dikirim ulang sesuai catatan panitia.");
%>
<section class="container py-5">
    <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
        <div>
            <span class="badge karir-badge-soft rounded-pill px-3 py-2 mb-2"><i class="fas fa-gauge-high me-1"></i>Dashboard Calon Pegawai</span>
            <h1 class="karir-section-title-<%=rnd%> mb-1">Selamat Datang, <%=lk(calon == null ? "Calon Pegawai" : calon.getNama())%></h1>
            <p class="text-muted mb-0 karir-dashboard-welcome"><%=lk(teksDashboard)%></p>
        </div>
        <button class="btn karir-btn-gradient-<%=rnd%> rounded-pill fw-bold px-4" onclick="karirReloadDashboard()"><i class="fas fa-sync-alt me-2"></i>Refresh Dashboard</button>
    </div>

    <div class="row g-3 mb-4">
        <div class="col-md-3"><div class="karir-soft-card-<%=rnd%> p-4 karir-stat-<%=rnd%>"><div class="text-muted small">Status Seleksi</div><h4 id="statStatusKarir" class="fw-bold mb-0">-</h4></div></div>
        <div class="col-md-3"><div class="karir-soft-card-<%=rnd%> p-4 karir-stat-<%=rnd%>"><div class="text-muted small">Lowongan Dipilih</div><h4 id="statLowonganKarir" class="fw-bold mb-0">-</h4></div></div>
        <div class="col-md-3"><div class="karir-soft-card-<%=rnd%> p-4 karir-stat-<%=rnd%>"><div class="text-muted small">Dokumen Terverifikasi</div><h4 id="statDokumenKarir" class="fw-bold mb-0">0/0</h4></div></div>
        <div class="col-md-3"><div class="karir-soft-card-<%=rnd%> p-4 karir-stat-<%=rnd%>"><div class="text-muted small">Dokumen Perlu Revisi</div><h4 id="statRevisiKarir" class="fw-bold mb-0">0</h4></div></div>
    </div>

    <div class="row g-3 mb-4">
        <div class="col-lg-4">
            <div class="karir-css-chart-card h-100 text-center">
                <div class="small text-muted mb-2">Kelengkapan Dokumen</div>
                <div id="karirDokumenDonut<%=rnd%>" class="karir-css-donut" style="--value:0" data-label="0%"></div>
                <div class="small text-muted mt-3">Menunjukkan berapa banyak dokumen yang sudah disetujui.</div>
            </div>
        </div>
        <div class="col-lg-4">
            <div class="karir-css-chart-card h-100">
                <div class="small text-muted mb-2">Arah Proses</div>
                <div id="karirTrendBars<%=rnd%>" class="karir-trend-bars"><span></span><span></span><span></span><span></span><span></span></div>
                <div class="small text-muted mt-3">Semakin tinggi batangnya, semakin dekat data Anda ke tahap akhir.</div>
            </div>
        </div>
        <div class="col-lg-4">
            <div class="karir-css-chart-card h-100">
                <div class="small text-muted mb-2">Peta Kesiapan</div>
                <div id="karirSpider<%=rnd%>" class="karir-css-spider" style="--a:25%;--b:25%;--c:25%;--d:25%;--e:25%;">
                    <div class="web"></div><div class="shape"></div>
                    <span class="spider-label l1">Data</span><span class="spider-label l2">Dokumen</span><span class="spider-label l3">Verifikasi</span><span class="spider-label l4">Jadwal</span><span class="spider-label l5">Hasil</span>
                </div>
            </div>
        </div>
    </div>

    <div class="karir-soft-card-<%=rnd%> p-3 p-lg-4">
        <ul class="nav nav-pills gap-2 mb-4" role="tablist">
            <li class="nav-item"><button class="nav-link active" data-bs-toggle="pill" data-bs-target="#tabRingkasanKarir<%=rnd%>" type="button"><i class="fas fa-bullhorn me-2"></i>Pengumuman</button></li>
            <li class="nav-item"><button class="nav-link" data-bs-toggle="pill" data-bs-target="#tabProfilKarir<%=rnd%>" type="button"><i class="fas fa-user-pen me-2"></i>Data Diri</button></li>
            <li class="nav-item"><button class="nav-link" data-bs-toggle="pill" data-bs-target="#tabDokumenKarir<%=rnd%>" type="button"><i class="fas fa-folder-open me-2"></i>Dokumen</button></li>
        </ul>
        <div class="tab-content">
            <div class="tab-pane fade show active" id="tabRingkasanKarir<%=rnd%>">
                <div class="row g-4">
                    <div class="col-lg-7">
                        <div class="p-4 rounded-4 karir-panel-muted">
                            <h5 class="fw-bold mb-1"><i class="fas fa-calendar-check text-primary me-2"></i>Pengumuman Status Seleksi</h5><p class="small text-muted mb-3"><%=lk(teksPengumumanPanel)%></p>
                            <div id="karirPengumumanBox<%=rnd%>" class="table-responsive"><div class="text-center py-5"><div class="spinner-border text-primary"></div></div></div>
                        </div>
                    </div>
                    <div class="col-lg-5">
                        <div class="p-4 rounded-4 h-100 karir-panel-gradient-soft">
                            <h5 class="fw-bold mb-1"><i class="fas fa-route text-primary me-2"></i>Progres Seleksi</h5><p class="small text-muted mb-3"><%=lk(teksProgresPanel)%></p>
                            <div class="list-group list-group-flush bg-transparent">
                                <div class="list-group-item bg-transparent px-0 d-flex gap-3"><i class="fas fa-check-circle text-success mt-1"></i><div><strong>Pendaftaran Terkirim</strong><br><small class="text-muted">Data pelamar sudah masuk ke ERP.</small></div></div>
                                <div class="list-group-item bg-transparent px-0 d-flex gap-3"><i class="fas fa-file-circle-check text-primary mt-1"></i><div><strong>Verifikasi Berkas</strong><br><small class="text-muted">Admin memeriksa kelengkapan dan kesesuaian dokumen.</small></div></div>
                                <div class="list-group-item bg-transparent px-0 d-flex gap-3"><i class="fas fa-comments text-warning mt-1"></i><div><strong>Interview</strong><br><small class="text-muted">Jadwal muncul di tabel pengumuman bila sudah ditentukan.</small></div></div>
                                <div class="list-group-item bg-transparent px-0 d-flex gap-3"><i class="fas fa-award text-success mt-1"></i><div><strong>Hasil Akhir</strong><br><small class="text-muted">Status diterima atau ditolak ditampilkan setelah proses selesai.</small></div></div>
                            </div>
                            <div id="karirNotifBox<%=rnd%>" class="alert alert-info rounded-4 border-0 small mt-3 mb-0">Memuat notifikasi...</div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="tab-pane fade" id="tabProfilKarir<%=rnd%>">
                <form id="karirProfileForm<%=rnd%>">
                    <input type="hidden" name="action" value="update_profile">
                    <div class="row g-3">
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Nama Lengkap")%></label><input id="kNama<%=rnd%>" name="nama" class="form-control rounded-3" required></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Email")%></label><input id="kEmail<%=rnd%>" name="email" type="email" class="form-control rounded-3" required></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Tempat Lahir")%></label><input id="kTempat<%=rnd%>" name="tempat_lahir" class="form-control rounded-3" required></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Tanggal Lahir")%></label><input id="kTanggal<%=rnd%>" name="tanggal_lahir" type="date" class="form-control rounded-3" required></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Jenis Kelamin")%></label><select id="kJk<%=rnd%>" name="jenis_kelamin" class="form-select rounded-3" required><option value="">Pilih</option><option>Laki-laki</option><option>Perempuan</option></select></div>
                        <div class="col-md-6"><label class="form-label fw-semibold"><%=Common.getBahasaConfig("Agama")%></label><select id="kAgama<%=rnd%>" name="agama" class="form-select rounded-3"><option value="">Pilih agama</option></select></div>
                        <div class="col-md-6"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Telepon / HP")%></label><input id="kTelp<%=rnd%>" name="telp" class="form-control rounded-3" required></div>
                        <div class="col-12"><label class="form-label karir-required fw-semibold"><%=Common.getBahasaConfig("Alamat")%></label><textarea id="kAlamat<%=rnd%>" name="alamat" rows="3" class="form-control rounded-3" required></textarea></div>
                        <div class="col-12 text-end"><button class="btn karir-btn-gradient-<%=rnd%> rounded-pill px-4 fw-bold"><i class="fas fa-save me-2"></i>Simpan Data Diri</button></div>
                    </div>
                </form>
            </div>
            <div class="tab-pane fade" id="tabDokumenKarir<%=rnd%>">
                <div class="d-flex justify-content-between align-items-center gap-3 mb-3">
                    <div><h5 class="fw-bold mb-1"><i class="fas fa-folder-open text-primary me-2"></i>Dokumen Persyaratan</h5><p class="text-muted small mb-0"><%=lk(teksDokumenPanel)%></p></div>
                    <button class="btn btn-outline-primary rounded-pill fw-bold" onclick="karirLoadDokumen()"><i class="fas fa-sync-alt me-2"></i>Refresh</button>
                </div>
                <div id="karirDokumenBox<%=rnd%>" class="table-responsive"><div class="text-center py-5"><div class="spinner-border text-primary"></div></div></div>
            </div>
        </div>
    </div>
</section>

<script>
function karirSetValue(id, value){ var e=document.getElementById(id); if(e) e.value = value || ''; }
function karirSetText(id, value){ var e=document.getElementById(id); if(e) e.innerText = value || '-'; }
async function karirLoadAgamaLogin(selected){
    try { var res = await fetch(window.karirServiceUrl + '&action=list_agama'); var json = await res.json(); var sel = document.getElementById('kAgama<%=rnd%>'); if(!sel) return; sel.innerHTML = '<option value="">Pilih agama</option>'; (json.data || []).forEach(function(a){ var opt=document.createElement('option'); opt.value = a.nama || ''; opt.textContent = a.nama || ''; sel.appendChild(opt); }); sel.value = selected || ''; } catch(e) {}
}
async function karirReloadDashboard(){
    karirShowLoading();
    try { var res = await fetch(window.karirServiceUrl + '&action=get_profile'); var json = await res.json(); karirHideLoading();
        if(json.status !== 'success'){ karirToast(json.message || 'Gagal memuat data calon pegawai.', 'danger'); return; }
        var c = json.calon || {}, s = json.stat || {};
        karirSetText('statStatusKarir', s.status || '-');
        karirSetText('statLowonganKarir', c.lowongan || '-');
        karirSetText('statDokumenKarir', (s.dokumenTerverifikasi || 0) + '/' + (s.dokumenTotal || 0));
        karirSetText('statRevisiKarir', s.dokumenRevisi || 0);
        karirSetText('karirNotifBox<%=rnd%>', s.notifikasi || 'Tidak ada notifikasi baru.');
        karirUpdateCssDashboard(s);
        karirSetValue('kNama<%=rnd%>', c.nama); karirSetValue('kEmail<%=rnd%>', c.email); karirSetValue('kTempat<%=rnd%>', c.tempatLahir); karirSetValue('kTanggal<%=rnd%>', c.tanggalLahirIso); karirSetValue('kJk<%=rnd%>', c.jenisKelamin); karirSetValue('kTelp<%=rnd%>', c.telp); karirSetValue('kAlamat<%=rnd%>', c.alamat); karirLoadAgamaLogin(c.agama);
        karirRenderPengumuman(c, s);
    } catch(e){ karirHideLoading(); karirToast('Gagal memuat dashboard.', 'danger'); }
}

function karirUpdateCssDashboard(s){
    var total = Number(s.dokumenTotal || 0);
    var ok = Number(s.dokumenTerverifikasi || 0);
    var revisi = Number(s.dokumenRevisi || 0);
    var pct = total > 0 ? Math.round((ok * 100) / total) : 0;
    var donut = document.getElementById('karirDokumenDonut<%=rnd%>');
    if(donut){ donut.style.setProperty('--value', pct); donut.setAttribute('data-label', pct + '%'); }
    var bars = document.querySelectorAll('#karirTrendBars<%=rnd%> span');
    var status = String(s.status || '').toLowerCase();
    var stage = status.indexOf('diterima') >= 0 ? 100 : (status.indexOf('ditolak') >= 0 ? 100 : (status.indexOf('interview') >= 0 ? 78 : (pct >= 100 ? 62 : (pct > 0 ? 44 : 25))));
    for(var i=0;i<bars.length;i++){ var h = Math.max(12, Math.min(100, stage - ((bars.length - i - 1) * 10))); bars[i].style.height = h + '%'; }
    var spider = document.getElementById('karirSpider<%=rnd%>');
    if(spider){
        spider.style.setProperty('--a', (pct > 0 ? 82 : 35) + '%');
        spider.style.setProperty('--b', Math.max(25, pct) + '%');
        spider.style.setProperty('--c', Math.max(25, pct - revisi * 8) + '%');
        spider.style.setProperty('--d', (String(s.jadwalInterview || '').indexOf('Menunggu') >= 0 ? 35 : 78) + '%');
        spider.style.setProperty('--e', (status.indexOf('diterima') >= 0 || status.indexOf('ditolak') >= 0 ? 90 : 42) + '%');
    }
}
function karirRenderPengumuman(c, s){
    var box = document.getElementById('karirPengumumanBox<%=rnd%>');
    var badge = s.status === 'Diterima' ? 'success' : (s.status === 'Ditolak' ? 'danger' : (s.status === 'Proses Interview' ? 'warning text-dark' : 'primary'));
    var html = '<table class="table karir-table align-middle"><thead><tr><th>Nama Calon</th><th>Jenis Lowongan</th><th>Waktu/Tanggal/Jam</th><th>Ruang/Lokasi</th><th>Status</th></tr></thead><tbody>';
    html += '<tr><td><strong>'+karirEscapeHtml(c.nama)+'</strong><br><small class="text-muted">No. Registrasi: '+karirEscapeHtml(c.noRegistrasi)+'</small></td><td>'+karirEscapeHtml(c.lowongan || '-')+'</td><td>'+karirEscapeHtml(s.jadwalInterview || 'Menunggu informasi panitia')+'</td><td>'+karirEscapeHtml(s.ruangInterview || 'Menunggu informasi panitia')+'</td><td><span class="badge bg-'+badge+' rounded-pill px-3 py-2">'+karirEscapeHtml(s.status || '-')+'</span></td></tr>';
    html += '</tbody></table>';
    box.innerHTML = html;
}
async function karirSaveProfile(form){
    karirShowLoading();
    try { var res = await fetch(window.karirServiceUrl, {method:'POST', body:new URLSearchParams(new FormData(form))}); var json = await res.json(); karirHideLoading(); if(json.status === 'success'){ karirToast(json.message || 'Data diri berhasil disimpan.', 'success'); karirReloadDashboard(); } else karirToast(json.message || 'Gagal menyimpan data.', 'danger'); }
    catch(e){ karirHideLoading(); karirToast('Terjadi kesalahan jaringan.', 'danger'); }
}
async function karirLoadDokumen(){
    var box = document.getElementById('karirDokumenBox<%=rnd%>'); box.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary"></div></div>';
    try { var res = await fetch(window.karirServiceUrl + '&action=list_dokumen'); var json = await res.json();
        if(json.status !== 'success'){ box.innerHTML = '<div class="alert alert-warning rounded-4 border-0">'+karirEscapeHtml(json.message || 'Dokumen belum dapat dimuat.')+'</div>'; return; }
        var html = '<table class="table karir-table align-middle"><thead><tr><th>Dokumen</th><th>Sifat</th><th>Status</th><th>Catatan / File</th><th class="karir-upload-col">Upload Ulang</th></tr></thead><tbody>';
        var dataDokumen = [];
        var dokKeys = {};
        (json.data || []).forEach(function(d){
            var key = (d.kode || d.nama || '').toString().toLowerCase().replace(/\s+/g, ' ').trim();
            if(!key){ key = String(d.id || ''); }
            if(dokKeys[key]){ return; }
            dokKeys[key] = true;
            dataDokumen.push(d);
        });
        if(dataDokumen.length === 0){ html += '<tr><td colspan="5" class="text-center text-muted py-4">Template dokumen belum tersedia.</td></tr>'; }
        dataDokumen.forEach(function(d){ var verified = d.status === 'Terverifikasi'; var badge = verified ? 'success' : (d.status === 'Harus Direvisi' ? 'warning text-dark' : 'secondary');
            html += '<tr><td><strong>'+karirEscapeHtml(d.nama)+'</strong><br><small class="text-muted">'+karirEscapeHtml(d.kode || '')+'</small></td><td>'+(d.wajib ? '<span class="badge bg-danger">Wajib</span>' : '<span class="badge bg-secondary">Opsional</span>')+'</td><td><span class="badge bg-'+badge+' rounded-pill">'+karirEscapeHtml(d.status)+'</span></td><td><small>'+karirEscapeHtml(d.keterangan)+'</small>'+(d.link ? '<br><a target="_blank" href="'+karirEscapeHtml(d.link)+'" class="small karir-safe-link"><i class="fas fa-link me-1"></i>Lihat File</a>' : '')+'</td><td>'+(verified ? '<span class="text-success small fw-bold"><i class="fas fa-check-circle me-1"></i>Sudah terverifikasi</span>' : '<form class="karirDocUploadForm" data-id="'+d.id+'"><input type="file" name="file" class="form-control form-control-sm mb-2"><input type="text" name="catatan" class="form-control form-control-sm mb-2" placeholder="Catatan"><button class="btn btn-sm btn-outline-primary rounded-pill fw-bold w-100"><i class="fas fa-upload me-1"></i>Upload</button></form>')+'</td></tr>'; });
        html += '</tbody></table>'; box.innerHTML = html;
        document.querySelectorAll('.karirDocUploadForm').forEach(function(f){ f.addEventListener('submit', async function(e){ e.preventDefault(); await karirUploadDokumen(this); }); });
    } catch(e) { box.innerHTML = '<div class="alert alert-danger rounded-4">Gagal memuat dokumen.</div>'; }
}
async function karirUploadDokumen(form){
    var docId = form.getAttribute('data-id');
    var fd = new FormData(form);
    fd.append('action', 'upload_dokumen');
    fd.append('aksi', 'upload_dokumen');
    fd.append('mode', 'upload_dokumen');
    fd.append('id', docId);
    fd.append('dokumen_id', docId);
    var uploadUrl = window.karirServiceUrl + '&action=upload_dokumen&aksi=upload_dokumen&mode=upload_dokumen&id=' + encodeURIComponent(docId || '') + '&dokumen_id=' + encodeURIComponent(docId || '');
    karirShowLoading();
    try{ var res = await fetch(uploadUrl, {method:'POST', body:fd}); var json = await res.json(); karirHideLoading(); if(json.status === 'success'){ karirToast(json.message || 'Dokumen berhasil dikirim.', 'success'); karirLoadDokumen(); karirReloadDashboard(); } else karirToast(json.message || 'Upload gagal.', 'danger'); }
    catch(e){ karirHideLoading(); karirToast('Upload gagal. Pastikan ukuran file sesuai.', 'danger'); }
}
document.addEventListener('DOMContentLoaded', function(){
    karirReloadDashboard(); karirLoadDokumen();
    var f = document.getElementById('karirProfileForm<%=rnd%>'); if(f) f.addEventListener('submit', function(e){ e.preventDefault(); karirSaveProfile(this); });
});
</script>
