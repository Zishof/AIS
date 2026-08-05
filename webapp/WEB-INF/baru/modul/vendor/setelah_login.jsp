<%@page import="ais.database.model.asset.PenyediaAsset"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String va(Object o) {
        String s = o == null ? "" : String.valueOf(o);
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String rnd = Common.getGeneratedBarCode(7);
    PenyediaAsset vendor = (PenyediaAsset) request.getSession().getAttribute("VENDOR_LOGGED_IN");
    String teksDashboard = Common.getKonfigurasi("vendor_dashboard_deskripsi", "Lengkapi profil, legalitas, kualifikasi, dan dokumen agar data vendor siap diverifikasi oleh tim pengadaan.").getNilai();
    String teksStatus = Common.getKonfigurasi("vendor_dashboard_teks_status", "Status menunjukkan apakah akun sudah dapat digunakan dalam proses vendor.").getNilai();
    String teksProfil = Common.getKonfigurasi("vendor_dashboard_teks_profil", "Semakin lengkap data profil, semakin mudah tim pengadaan menilai kesiapan vendor.").getNilai();
    String teksDokumen = Common.getKonfigurasi("vendor_dashboard_teks_dokumen", "Dokumen terverifikasi adalah berkas yang sudah diperiksa dan dinyatakan sesuai.").getNilai();
    String teksRevisi = Common.getKonfigurasi("vendor_dashboard_teks_revisi", "Angka revisi menunjukkan dokumen yang perlu diperbaiki atau diunggah ulang.").getNilai();
    if (vendor == null) {
        response.sendRedirect(Common.ROOT + "/vendor");
        return;
    }
%>
<div class="container py-4 py-lg-5">
    <div class="row g-4 align-items-center mb-4">
        <div class="col-lg-8">
            <span class="badge vendor-badge-soft rounded-pill px-3 py-2 mb-2"><i class="fas fa-building me-1"></i>Dashboard Vendor</span>
            <h1 class="vendor-section-title-<%=rnd%> mb-2">Selamat datang, <%=va(vendor.getNama())%></h1>
            <p class="text-muted mb-0"><%=va(teksDashboard)%></p>
        </div>
        <div class="col-lg-4 text-lg-end">
            <button class="btn vendor-btn-gradient-<%=rnd%> rounded-pill fw-bold px-4" onclick="vendorReloadDashboard()"><i class="fas fa-sync-alt me-2"></i>Refresh Dashboard</button>
        </div>
    </div>

    <div class="row g-3 mb-4" id="vendorDashboardCards<%=rnd%>">
        <div class="col-md-3"><div class="vendor-soft-card-<%=rnd%> p-4 vendor-stat-<%=rnd%>"><div class="text-muted small">Status Vendor</div><h4 id="statStatusVendor" class="fw-bold mb-0">-</h4><div class="vendor-stat-description"><%=va(teksStatus)%></div></div></div>
        <div class="col-md-3"><div class="vendor-soft-card-<%=rnd%> p-4 vendor-stat-<%=rnd%>"><div class="text-muted small">Kelengkapan Profil</div><h4 id="statProfilVendor" class="fw-bold mb-0">0%</h4><div class="vendor-stat-description"><%=va(teksProfil)%></div></div></div>
        <div class="col-md-3"><div class="vendor-soft-card-<%=rnd%> p-4 vendor-stat-<%=rnd%>"><div class="text-muted small">Dokumen Terverifikasi</div><h4 id="statDokumenVendor" class="fw-bold mb-0">0/0</h4><div class="vendor-stat-description"><%=va(teksDokumen)%></div></div></div>
        <div class="col-md-3"><div class="vendor-soft-card-<%=rnd%> p-4 vendor-stat-<%=rnd%>"><div class="text-muted small">Dokumen Perlu Revisi</div><h4 id="statRevisiVendor" class="fw-bold mb-0">0</h4><div class="vendor-stat-description"><%=va(teksRevisi)%></div></div></div>
    </div>



    <div class="row g-4 mb-4">
        <div class="col-lg-4">
            <div class="vendor-soft-card-<%=rnd%> vendor-visual-card p-4 text-center">
                <h5 class="fw-bold mb-2"><i class="fas fa-circle-notch text-primary me-2"></i>Kesiapan Profil</h5>
                <p class="vendor-panel-note mb-3">Angka ini membantu vendor melihat seberapa lengkap data utama sebelum diverifikasi.</p>
                <div id="vendorRingProfil<%=rnd%>" class="vendor-ring" data-label="0%" style="--pct:0"></div>
            </div>
        </div>
        <div class="col-lg-4">
            <div class="vendor-soft-card-<%=rnd%> vendor-visual-card p-4">
                <h5 class="fw-bold mb-2"><i class="fas fa-chart-bar text-primary me-2"></i>Perkembangan Data</h5>
                <p class="vendor-panel-note mb-3">Batang ini memperlihatkan bagian data yang sudah siap, dokumen yang sudah sesuai, dan revisi yang masih perlu dibereskan.</p>
                <div class="vendor-trend-row"><span class="small fw-bold">Profil</span><div class="vendor-trend-track"><div id="vendorTrendProfil<%=rnd%>" class="vendor-trend-fill"></div></div><span id="vendorTrendProfilText<%=rnd%>" class="small fw-bold text-end">0%</span></div>
                <div class="vendor-trend-row"><span class="small fw-bold">Dokumen</span><div class="vendor-trend-track"><div id="vendorTrendDokumen<%=rnd%>" class="vendor-trend-fill"></div></div><span id="vendorTrendDokumenText<%=rnd%>" class="small fw-bold text-end">0%</span></div>
                <div class="vendor-trend-row"><span class="small fw-bold">Revisi</span><div class="vendor-trend-track"><div id="vendorTrendRevisi<%=rnd%>" class="vendor-trend-fill"></div></div><span id="vendorTrendRevisiText<%=rnd%>" class="small fw-bold text-end">0</span></div>
            </div>
        </div>
        <div class="col-lg-4">
            <div class="vendor-soft-card-<%=rnd%> vendor-visual-card p-4 text-center">
                <h5 class="fw-bold mb-2"><i class="fas fa-dharmachakra text-primary me-2"></i>Peta Kesiapan Vendor</h5>
                <p class="vendor-panel-note mb-3">Bentuk radar ini merangkum kesiapan profil, legalitas, kualifikasi, dokumen, dan revisi.</p>
                <div class="vendor-radar">
                    <div id="vendorRadarShape<%=rnd%>" class="vendor-radar-shape"></div>
                    <span class="vendor-radar-label l1">Profil</span><span class="vendor-radar-label l2">Legal</span><span class="vendor-radar-label l3">Kualifikasi</span><span class="vendor-radar-label l4">Dokumen</span><span class="vendor-radar-label l5">Revisi</span>
                </div>
            </div>
        </div>
    </div>

    <div class="vendor-soft-card-<%=rnd%> p-3 p-lg-4">
        <ul class="nav nav-pills gap-2 mb-4" id="vendorTab<%=rnd%>" role="tablist">
            <li class="nav-item"><button class="nav-link active" data-bs-toggle="pill" data-bs-target="#tabRingkasan<%=rnd%>" type="button"><i class="fas fa-chart-pie me-2"></i>Ringkasan</button></li>
            <li class="nav-item"><button class="nav-link" data-bs-toggle="pill" data-bs-target="#tabProfil<%=rnd%>" type="button"><i class="fas fa-id-card me-2"></i>Profil Umum</button></li>
            <li class="nav-item"><button class="nav-link" data-bs-toggle="pill" data-bs-target="#tabLegal<%=rnd%>" type="button"><i class="fas fa-file-signature me-2"></i>Legalitas Umum</button></li>
            <li class="nav-item"><button class="nav-link" data-bs-toggle="pill" data-bs-target="#tabKhusus<%=rnd%>" type="button"><i class="fas fa-briefcase me-2"></i>Kualifikasi Khusus</button></li>
            <li class="nav-item"><button class="nav-link" data-bs-toggle="pill" data-bs-target="#tabDokumen<%=rnd%>" type="button"><i class="fas fa-cloud-upload-alt me-2"></i>Dokumen</button></li>
        </ul>
        <div class="tab-content">
            <div class="tab-pane fade show active" id="tabRingkasan<%=rnd%>">
                <div class="row g-4">
                    <div class="col-lg-7">
                        <div class="card border-0 shadow-sm rounded-4 h-100"><div class="card-body p-4">
                            <h5 class="fw-bold mb-3"><i class="fas fa-tasks text-primary me-2"></i>Checklist Proses Vendor</h5>
                            <div class="list-group list-group-flush">
                                <div class="list-group-item px-0 d-flex gap-3"><i class="fas fa-check-circle text-success mt-1"></i><div><strong>Pendaftaran awal</strong><br><small class="text-muted">Data vendor masuk ke sistem ERP.</small></div></div>
                                <div class="list-group-item px-0 d-flex gap-3"><i class="fas fa-user-check text-primary mt-1"></i><div><strong>Persetujuan admin</strong><br><small class="text-muted">Akun vendor aktif setelah diverifikasi.</small></div></div>
                                <div class="list-group-item px-0 d-flex gap-3"><i class="fas fa-folder-open text-warning mt-1"></i><div><strong>Lengkapi dokumen</strong><br><small class="text-muted">Upload dokumen umum dan khusus sesuai bidang.</small></div></div>
                                <div class="list-group-item px-0 d-flex gap-3"><i class="fas fa-handshake text-info mt-1"></i><div><strong>Siap mengikuti pengadaan</strong><br><small class="text-muted">Vendor siap masuk proses pengadaan setelah dokumen lengkap.</small></div></div>
                            </div>
                        </div></div>
                    </div>
                    <div class="col-lg-5">
                        <div class="card border-0 shadow-sm rounded-4 h-100"><div class="card-body p-4">
                            <h5 class="fw-bold mb-3"><i class="fas fa-bell text-primary me-2"></i>Notifikasi</h5>
                            <div id="vendorNotifBox<%=rnd%>" class="small text-muted">Memuat notifikasi...</div>
                        </div></div>
                    </div>
                </div>
            </div>
            <div class="tab-pane fade" id="tabProfil<%=rnd%>">
                <form id="formProfilVendor<%=rnd%>" class="row g-3">
                    <input type="hidden" name="tipe_update" value="profil">
                    <div class="col-md-6"><label class="form-label vendor-required fw-semibold"><%= Common.getBahasaConfig("Nama Perusahaan") %></label><input id="vNama<%=rnd%>" name="nama" class="form-control rounded-3" required></div>
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Pemilik / Direktur") %></label><input id="vPemilik<%=rnd%>" name="pemilik" class="form-control rounded-3"></div>
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Nama Kontak") %></label><input id="vKontak<%=rnd%>" name="kontak" class="form-control rounded-3"></div>
                    <div class="col-md-6"><label class="form-label vendor-required fw-semibold"><%= Common.getBahasaConfig("Email") %></label><input id="vEmail<%=rnd%>" name="email" type="email" class="form-control rounded-3" required></div>
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Telepon / HP") %></label><input id="vTelp<%=rnd%>" name="telp" class="form-control rounded-3"></div>
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Kode Pos") %></label><input id="vKodePos<%=rnd%>" name="kodePos" class="form-control rounded-3"></div>
                    <div class="col-12"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Alamat") %></label><textarea id="vAlamat<%=rnd%>" name="alamat" rows="3" class="form-control rounded-3"></textarea></div>
                    <div class="col-12 text-end"><button class="btn vendor-btn-gradient-<%=rnd%> rounded-pill px-4 fw-bold"><i class="fas fa-save me-2"></i>Simpan Profil</button></div>
                </form>
            </div>
            <div class="tab-pane fade" id="tabLegal<%=rnd%>">
                <form id="formLegalVendor<%=rnd%>" class="row g-3">
                    <input type="hidden" name="tipe_update" value="legal">
                    <div class="col-md-4"><label class="form-label fw-semibold">NIB</label><input id="vNib<%=rnd%>" name="nib" class="form-control rounded-3"></div>
                    <div class="col-md-4"><label class="form-label fw-semibold">NPWP</label><input id="vNpwp<%=rnd%>" name="npwp" class="form-control rounded-3"></div>
                    <div class="col-md-4"><label class="form-label fw-semibold">PKP</label><select id="vPkp<%=rnd%>" name="pkp" class="form-select rounded-3"><option value="Tidak">Tidak</option><option value="Ya">Ya</option></select></div>
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("No. Akta Pendirian") %></label><input id="vAkta<%=rnd%>" name="noAktaPendirian" class="form-control rounded-3"></div>
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Nama Notaris") %></label><input id="vNotaris<%=rnd%>" name="namaNotaris" class="form-control rounded-3"></div>
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("No. Pengesahan") %></label><input id="vPengesahan<%=rnd%>" name="noPengesahan" class="form-control rounded-3"></div>
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Status Daftar Hitam") %></label><select id="vBlacklist<%=rnd%>" name="blacklist" class="form-select rounded-3"><option value="Tidak termasuk daftar hitam">Tidak termasuk daftar hitam</option><option value="Perlu klarifikasi">Perlu klarifikasi</option></select></div>
                    <div class="col-md-4"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Bank") %></label><input id="vBank<%=rnd%>" name="bankNama" class="form-control rounded-3"></div>
                    <div class="col-md-4"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("No. Rekening") %></label><input id="vNoRek<%=rnd%>" name="noRek" class="form-control rounded-3"></div>
                    <div class="col-md-4"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Atas Nama") %></label><input id="vAtasNama<%=rnd%>" name="atasNama" class="form-control rounded-3"></div>
                    <div class="col-12"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Pernyataan Vendor") %></label><textarea id="vPernyataan<%=rnd%>" name="pernyataan" class="form-control rounded-3" rows="4" placeholder="Pernyataan tidak pailit, tidak dalam pengawasan pengadilan, data benar, dan siap menerima sanksi bila data tidak benar."></textarea></div>
                    <div class="col-12 text-end"><button class="btn vendor-btn-gradient-<%=rnd%> rounded-pill px-4 fw-bold"><i class="fas fa-save me-2"></i>Simpan Legalitas</button></div>
                </form>
            </div>
            <div class="tab-pane fade" id="tabKhusus<%=rnd%>">
                <form id="formKhususVendor<%=rnd%>" class="row g-3">
                    <input type="hidden" name="tipe_update" value="khusus">
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Bidang Kualifikasi") %></label><select id="vBidang<%=rnd%>" name="bidang" class="form-select rounded-3"><option value="Umum">Umum</option><option value="Konstruksi">Konstruksi</option><option value="Pengadaan Barang">Pengadaan Barang</option><option value="Jasa">Jasa</option></select></div>
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Pengalaman Serupa") %></label><input id="vPengalaman<%=rnd%>" name="pengalaman" class="form-control rounded-3" placeholder="Contoh: 3 tahun"></div>
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("IUJK / SBU / Izin Usaha") %></label><input id="vIzinKhusus<%=rnd%>" name="izinKhusus" class="form-control rounded-3"></div>
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Tenaga Ahli / SKA") %></label><input id="vTenagaAhli<%=rnd%>" name="tenagaAhli" class="form-control rounded-3"></div>
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Layanan Purna Jual") %></label><input id="vPurnaJual<%=rnd%>" name="purnaJual" class="form-control rounded-3"></div>
                    <div class="col-md-6"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Dukungan Pabrikan/Distributor") %></label><input id="vDukungan<%=rnd%>" name="dukungan" class="form-control rounded-3"></div>
                    <div class="col-12"><label class="form-label fw-semibold"><%= Common.getBahasaConfig("Produk/Jasa Utama, Merk, Tipe, dan Spesifikasi") %></label><textarea id="vProdukJasa<%=rnd%>" name="produkJasa" rows="4" class="form-control rounded-3"></textarea></div>
                    <div class="col-12 text-end"><button class="btn vendor-btn-gradient-<%=rnd%> rounded-pill px-4 fw-bold"><i class="fas fa-save me-2"></i>Simpan Kualifikasi</button></div>
                </form>
            </div>
            <div class="tab-pane fade" id="tabDokumen<%=rnd%>">
                <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                    <div><h5 class="fw-bold mb-1"><i class="fas fa-folder-open text-primary me-2"></i>Kelengkapan Dokumen Vendor</h5><div class="small text-muted">Unggah berkas yang diminta dan lihat hasil pemeriksaan admin dalam satu tempat.</div></div>
                    <button class="btn btn-outline-primary rounded-pill fw-bold" onclick="vendorLoadDokumen()"><i class="fas fa-sync-alt me-2"></i>Refresh</button>
                </div>
                <div id="vendorDokumenBox<%=rnd%>" class="table-responsive"><div class="text-center py-5"><div class="spinner-border text-primary"></div></div></div>
            </div>
        </div>
    </div>
</div>
<script>
function vendorSetValue(id, value){ var e=document.getElementById(id); if(e) e.value = value || ''; }
function vendorEscapeHtml(s){ return (s || '').replace(/[&<>'"]/g, function(c){ return {'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',"'":'&#39;'}[c]; }); }
function vendorSetPct(id, value){ var e=document.getElementById(id); if(e){ var v=Math.max(0, Math.min(100, parseInt(value || 0, 10))); e.style.setProperty('--pct', v); e.setAttribute('data-label', v + '%'); } }
function vendorSetTrend(id, textId, value, text){ var e=document.getElementById(id), t=document.getElementById(textId); var v=Math.max(0, Math.min(100, parseInt(value || 0, 10))); if(e) e.style.width=v+'%'; if(t) t.innerText=(text || (v+'%')); }
function vendorUpdateVisuals(s){
    var profil=parseInt(s.profil || 0, 10);
    var total=parseInt(s.dokumenTotal || 0, 10), ok=parseInt(s.dokumenTerverifikasi || 0, 10), rev=parseInt(s.dokumenRevisi || 0, 10);
    var dok= total > 0 ? Math.round((ok * 100) / total) : 0;
    var revScore= total > 0 ? Math.max(0, 100 - Math.round((rev * 100) / total)) : 100;
    vendorSetPct('vendorRingProfil<%=rnd%>', profil);
    vendorSetTrend('vendorTrendProfil<%=rnd%>', 'vendorTrendProfilText<%=rnd%>', profil);
    vendorSetTrend('vendorTrendDokumen<%=rnd%>', 'vendorTrendDokumenText<%=rnd%>', dok);
    vendorSetTrend('vendorTrendRevisi<%=rnd%>', 'vendorTrendRevisiText<%=rnd%>', rev > 0 ? Math.max(8, 100-revScore) : 0, rev + ' revisi');
    var legal = profil >= 70 ? 82 : Math.max(38, profil);
    var khusus = profil >= 55 ? 76 : Math.max(32, profil);
    var points = [profil, legal, khusus, dok, revScore].map(function(x){ return Math.max(18, Math.min(96, x || 0)); });
    var poly = '50% '+(50-points[0]/2)+'%, '+(50+points[1]*0.48)+'% '+(50-points[1]*0.15)+'%, '+(50+points[2]*0.30)+'% '+(50+points[2]*0.42)+'%, '+(50-points[3]*0.30)+'% '+(50+points[3]*0.42)+'%, '+(50-points[4]*0.48)+'% '+(50-points[4]*0.15)+'%';
    var radar=document.getElementById('vendorRadarShape<%=rnd%>'); if(radar) radar.style.clipPath='polygon('+poly+')';
}
async function vendorReloadDashboard(){
    vendorShowLoading();
    try{
        var res = await fetch(window.vendorServiceUrl + '&action=get_profile'); var json = await res.json();
        vendorHideLoading();
        if(json.status !== 'success'){ vendorToast(json.message || 'Gagal memuat data vendor.', 'danger'); return; }
        var v = json.vendor || {}, x = json.extra || {}, s = json.stat || {};
        document.getElementById('statStatusVendor').innerHTML = v.aktif ? '<span class="text-success">Aktif</span>' : '<span class="text-warning">Menunggu</span>';
        document.getElementById('statProfilVendor').innerText = (s.profil || 0) + '%';
        document.getElementById('statDokumenVendor').innerText = (s.dokumenTerverifikasi || 0) + '/' + (s.dokumenTotal || 0);
        document.getElementById('statRevisiVendor').innerText = s.dokumenRevisi || 0;
        document.getElementById('vendorNotifBox<%=rnd%>').innerHTML = s.notifikasi || 'Tidak ada notifikasi.';
        vendorUpdateVisuals(s);
        vendorSetValue('vNama<%=rnd%>', v.nama); vendorSetValue('vPemilik<%=rnd%>', v.pemilik); vendorSetValue('vKontak<%=rnd%>', v.kontak); vendorSetValue('vEmail<%=rnd%>', v.email); vendorSetValue('vTelp<%=rnd%>', v.telp); vendorSetValue('vKodePos<%=rnd%>', v.kodePos); vendorSetValue('vAlamat<%=rnd%>', v.alamat);
        vendorSetValue('vNib<%=rnd%>', x.nib); vendorSetValue('vNpwp<%=rnd%>', v.npwp); vendorSetValue('vAkta<%=rnd%>', v.noAktaPendirian); vendorSetValue('vNotaris<%=rnd%>', v.namaNotaris); vendorSetValue('vPengesahan<%=rnd%>', v.noPengesahan); vendorSetValue('vBank<%=rnd%>', x.bankNama); vendorSetValue('vNoRek<%=rnd%>', v.noRek); vendorSetValue('vAtasNama<%=rnd%>', v.atasNama); vendorSetValue('vPernyataan<%=rnd%>', x.pernyataan); vendorSetValue('vPengalaman<%=rnd%>', x.pengalaman); vendorSetValue('vIzinKhusus<%=rnd%>', x.izinKhusus); vendorSetValue('vTenagaAhli<%=rnd%>', x.tenagaAhli); vendorSetValue('vPurnaJual<%=rnd%>', x.purnaJual); vendorSetValue('vDukungan<%=rnd%>', x.dukungan); vendorSetValue('vProdukJasa<%=rnd%>', x.produkJasa);
        if(document.getElementById('vPkp<%=rnd%>')) document.getElementById('vPkp<%=rnd%>').value = x.pkp || 'Tidak';
        if(document.getElementById('vBlacklist<%=rnd%>')) document.getElementById('vBlacklist<%=rnd%>').value = x.blacklist || 'Tidak termasuk daftar hitam';
        if(document.getElementById('vBidang<%=rnd%>')) document.getElementById('vBidang<%=rnd%>').value = x.bidang || 'Umum';
    } catch(e){ vendorHideLoading(); vendorToast('Gagal memuat data vendor.', 'danger'); }
}
async function vendorSaveForm(form){
    vendorShowLoading();
    try{
        var fd = new FormData(form); fd.append('action','update_profile');
        var res = await fetch(window.vendorServiceUrl, {method:'POST', body:new URLSearchParams(fd)}); var json = await res.json();
        vendorHideLoading();
        if(json.status === 'success'){ vendorToast(json.message || 'Data berhasil disimpan.', 'success'); vendorReloadDashboard(); }
        else vendorToast(json.message || 'Gagal menyimpan data.', 'danger');
    }catch(e){ vendorHideLoading(); vendorToast('Terjadi kesalahan jaringan.', 'danger'); }
}
async function vendorLoadDokumen(){
    var box = document.getElementById('vendorDokumenBox<%=rnd%>'); box.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary"></div></div>';
    try{
        var res = await fetch(window.vendorServiceUrl + '&action=list_dokumen'); var json = await res.json();
        if(json.status !== 'success') throw new Error(json.message || 'Gagal');
        var html = '<table class="table vendor-table align-middle"><thead><tr><th><%= Common.getBahasaConfig("Dokumen") %></th><th><%= Common.getBahasaConfig("Wajib") %></th><th><%= Common.getBahasaConfig("Status") %></th><th><%= Common.getBahasaConfig("Masa Berlaku") %></th><th><%= Common.getBahasaConfig("Catatan / File") %></th><th style="width:260px"><%= Common.getBahasaConfig("Upload") %></th></tr></thead><tbody>';
        if(!json.data || json.data.length === 0){ html += '<tr><td colspan="6" class="text-center text-muted py-4">Template dokumen vendor belum dikonfigurasi admin.</td></tr>'; }
        json.data.forEach(function(d){
            var badge = d.status === 'Terverifikasi' ? 'success' : (d.status === 'Harus Direvisi' ? 'danger' : 'warning text-dark');
            var mb;
            if(d.berlakuSampai){
                var mbClass = d.kedaluwarsa ? 'danger' : (d.segeraKedaluwarsa ? 'warning text-dark' : 'light text-dark border');
                var mbNote = d.kedaluwarsa ? ' <span class="badge bg-danger">Kedaluwarsa</span>' : (d.segeraKedaluwarsa ? ' <span class="badge bg-warning text-dark">&le;30 hari</span>' : '');
                mb = '<span class="badge bg-'+mbClass+'">'+vendorEscapeHtml(d.berlakuSampai)+'</span>'+mbNote;
            } else { mb = '<small class="text-muted">-</small>'; }
            html += '<tr><td><strong>'+vendorEscapeHtml(d.nama)+'</strong><br><small class="text-muted">'+vendorEscapeHtml(d.kode)+'</small></td>'
                + '<td>'+(d.wajib ? '<span class="badge bg-danger">Wajib</span>' : '<span class="badge bg-secondary">Opsional</span>')+'</td>'
                + '<td><span class="badge bg-'+badge+'">'+vendorEscapeHtml(d.status)+'</span></td>'
                + '<td>'+mb+'</td>'
                + '<td><small>'+vendorEscapeHtml(d.keterangan)+'</small>'+(d.link ? '<br><a target="_blank" href="'+vendorEscapeHtml(d.link)+'" class="small"><i class="fas fa-link me-1"></i>Lihat File</a>' : '')+'</td>'
                + '<td><form class="vendorDocUploadForm" data-id="'+d.id+'"><input type="file" name="file" class="form-control form-control-sm mb-2"><div class="input-group input-group-sm mb-2"><span class="input-group-text">Berlaku s/d</span><input type="date" name="berlaku_sampai" value="'+(d.berlakuIso||'')+'" class="form-control" title="Masa berlaku dokumen (opsional)"></div><input type="text" name="catatan" class="form-control form-control-sm mb-2" placeholder="Catatan"><button class="btn btn-sm btn-outline-primary rounded-pill fw-bold w-100"><i class="fas fa-upload me-1"></i>Upload</button></form></td></tr>';
        });
        html += '</tbody></table>'; box.innerHTML = html;
        document.querySelectorAll('.vendorDocUploadForm').forEach(function(f){ f.addEventListener('submit', async function(e){ e.preventDefault(); await vendorUploadDokumen(this); }); });
    }catch(e){ box.innerHTML = '<div class="alert alert-danger rounded-4"><i class="fas fa-triangle-exclamation me-2"></i>Gagal memuat dokumen.</div>'; }
}
async function vendorUploadDokumen(form){
    var fd = new FormData(form); fd.append('action','upload_dokumen'); fd.append('id', form.getAttribute('data-id'));
    vendorShowLoading();
    try{ var res = await fetch(window.vendorServiceUrl, {method:'POST', body:fd}); var json = await res.json(); vendorHideLoading(); if(json.status === 'success'){ vendorToast(json.message || 'Dokumen berhasil diproses.', 'success'); vendorLoadDokumen(); vendorReloadDashboard(); } else vendorToast(json.message || 'Upload gagal.', 'danger'); }
    catch(e){ vendorHideLoading(); vendorToast('Upload gagal. Pastikan ukuran file sesuai.', 'danger'); }
}
document.addEventListener('DOMContentLoaded', function(){
    vendorReloadDashboard(); vendorLoadDokumen();
    ['formProfilVendor<%=rnd%>','formLegalVendor<%=rnd%>','formKhususVendor<%=rnd%>'].forEach(function(id){ var f=document.getElementById(id); if(f) f.addEventListener('submit', function(e){ e.preventDefault(); vendorSaveForm(this); }); });
});
</script>
