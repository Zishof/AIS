<%@page import="ais.database.model.sekolah.CalonSiswa"%>
<%@page import="ais.database.model.JenisSeleksi"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String rnd = Common.getGeneratedBarCode(7);
    String linkGelombang = Common.ROOT + "/ppdb?hanya_tampil_jsp=true&p=ppdb&s=_gelombang_ppdb&baru=true";
    String linkPengumuman = Common.ROOT + "/ppdb?hanya_tampil_jsp=true&p=ppdb&s=_pengumuman_ppdb&baru=true";
    String linkPengumumanRinci = Common.ROOT + "/ppdb?hanya_tampil_jsp=true&p=home&s=pengumuman_rinci&baru=true";
    String linkPendaftaran = Common.ROOT + "/ppdb?hanya_tampil_jsp=true&p=ppdb&s=_pendaftaran_siswa&baru=true";
    
    // Pengambilan Data Konfigurasi Master Sekolah
    String judulNamaSekolah = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
    String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
    String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();
    String Email = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getEmail();
    String logo_Sekolah = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
%>

<style>
    .panel-bg-white-<%=rnd%> { background-color: rgba(255, 255, 255, 0.98) !important; backdrop-filter: blur(10px); }
    .icon-circle-<%=rnd%> { width: 45px; height: 45px; display: flex; align-items: center; justify-content: center; border-radius: 50%; font-size: 1.3rem; }
</style>

<div class="container mb-5 mt-4" style="position: relative; z-index: 1; min-height: 50vh;">
    <div class="row g-4">
        
        <div class="col-lg-8">
            <div class="card shadow-sm rounded-4 mb-4 border-0 panel-bg-white-<%=rnd%>">
                <div class="card-header bg-transparent border-bottom py-3">
                    <h5 class="mb-0 fw-bold text-primary">
                        <i class="fas fa-search me-2 text-warning"></i><%= Common.getBahasaConfig("Pencarian Gelombang Pendaftaran PPDB") %>
                    </h5>
                </div>
                <div class="card-body rounded-bottom-4 bg-light bg-opacity-50">
                    <div class="row g-3">
                        <div class="col-md-4">
                            <label class="form-label fw-semibold small text-muted"><i class="far fa-calendar-alt me-1"></i><%= Common.getBahasaConfig("Tahun Ajaran") %></label>
                            <select class="form-select form-select-sm border-primary rounded-pill shadow-none fw-bold text-dark" onchange="loadGelombangPPDB<%=rnd%>(1)" id="filterTA<%=rnd%>">
                                <option value="" selected><%= Common.getBahasaConfig("Semua Tahun Ajaran") %></option>
                                <% if (Common.tahunAngkatans != null) { 
                                    for (String ta : Common.tahunAngkatans) { %>
                                        <option value="<%= ta %>"><%= ta %></option>
                                <%  } } %>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold small text-muted"><i class="fas fa-list-ul me-1"></i><%= Common.getBahasaConfig("Jalur Seleksi") %></label>
                            <select class="form-select form-select-sm border-primary rounded-pill shadow-none fw-bold text-dark" onchange="loadGelombangPPDB<%=rnd%>(1)" id="filterSeleksi<%=rnd%>">
                                <option value=""><%= Common.getBahasaConfig("Semua Jalur Seleksi") %></option>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold small text-muted"><i class="fas fa-door-open me-1"></i><%= Common.getBahasaConfig("Status Pendaftaran") %></label>
                            <select class="form-select form-select-sm border-primary rounded-pill shadow-none fw-bold text-dark" onchange="loadGelombangPPDB<%=rnd%>(1)" id="filterStatus<%=rnd%>">
                                <option value="semua"><%= Common.getBahasaConfig("Semua Status") %></option>
                                <option value="buka" selected><%= Common.getBahasaConfig("Sedang Dibuka") %></option>
                                <option value="tutup"><%= Common.getBahasaConfig("Telah Ditutup") %></option>
                            </select>
                        </div>
                    </div>
                </div>
            </div>

            <div id="containerGelombang<%=rnd%>" class="row g-4">
                <div class="text-center py-5">
                    <div class="spinner-border text-primary" style="width: 3rem; height: 3rem;" role="status"></div>
                    <div class="mt-3 text-muted fw-bold"><%= Common.getBahasaConfig("Sistem sedang memuat data gelombang...") %></div>
                </div>
            </div>
        </div>

        <div class="col-lg-4">
            <div class="card shadow-sm rounded-4 border-0 mb-4 panel-bg-white-<%=rnd%>">
                <div class="card-header bg-transparent border-bottom py-3">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h5 class="mb-0 fw-bold text-danger">
                            <i class="fas fa-bullhorn me-2"></i><%= Common.getBahasaConfig("Informasi Terkini PPDB") %>
                        </h5>
                        <button class="btn btn-sm btn-outline-danger rounded-circle shadow-sm" onclick="loadPengumumanPPDB<%=rnd%>(1)" title="<%= Common.getBahasaConfig("Perbarui Informasi") %>">
                            <i class="fas fa-sync-alt"></i>
                        </button>
                    </div>
                    <div class="input-group input-group-sm">
                        <input type="text" id="cariPengumuman<%=rnd%>" class="form-control rounded-start-pill border-danger shadow-none" 
                               placeholder="<%= Common.getBahasaConfig("Cari informasi...") %>" 
                               onkeydown="if(event.key === 'Enter') loadPengumumanPPDB<%=rnd%>(1)">
                        <button class="btn btn-danger text-white rounded-end-pill px-3 shadow-none" type="button" onclick="loadPengumumanPPDB<%=rnd%>(1)">
                            <i class="fas fa-search"></i>
                        </button>
                    </div>
                </div>
                <div class="card-body p-0">
                    <div id="listPengumuman<%=rnd%>" class="rounded-bottom-4">
                        <div class="p-4 text-center text-muted fw-bold"><%= Common.getBahasaConfig("Memuat informasi terkini...") %></div>
                    </div>
                </div>
            </div>

            <div class="card shadow-sm rounded-4 border-0 panel-bg-white-<%=rnd%>">
                <div class="card-header bg-transparent border-bottom-0 pt-4 pb-0">
                    <h5 class="mb-0 fw-bold text-success text-center">
                        <i class="fas fa-headset me-2"></i><%= Common.getBahasaConfig("Pusat Layanan Informasi") %>
                    </h5>
                </div>
                <div class="card-body">
                    <div class="text-center mb-4">
                        <% if (logo_Sekolah != null && !logo_Sekolah.trim().isEmpty()) { %>
                            <img src="<%= logo_Sekolah %>" alt="Logo Sekolah" class="img-fluid mb-3" style="max-height: 65px; object-fit: contain;">
                        <% } %>
                        <h6 class="fw-bold text-dark mb-1"><%= judulNamaSekolah != null ? judulNamaSekolah : "-" %></h6>
                        <small class="text-muted"><%= Common.getBahasaConfig("Silakan hubungi kami apabila Anda memiliki pertanyaan seputar pendaftaran.") %></small>
                    </div>
                    <ul class="list-unstyled mb-0 text-secondary small">
                        <li class="d-flex align-items-start mb-3">
                            <div class="icon-circle-<%=rnd%> bg-light shadow-sm text-danger me-3 flex-shrink-0 border border-white"><i class="fas fa-map-marker-alt"></i></div>
                            <div class="mt-1"><strong class="d-block text-dark mb-1"><%= Common.getBahasaConfig("Alamat Resmi") %></strong><%= Alamat1 != null ? Alamat1 : "-" %></div>
                        </li>
                        <li class="d-flex align-items-center mb-3">
                            <div class="icon-circle-<%=rnd%> bg-light shadow-sm text-success me-3 flex-shrink-0 border border-white"><i class="fas fa-phone-alt"></i></div>
                            <div><strong class="d-block text-dark mb-1"><%= Common.getBahasaConfig("Nomor Telepon") %></strong><%= Telepon != null ? Telepon : "-" %></div>
                        </li>
                        <li class="d-flex align-items-center">
                            <div class="icon-circle-<%=rnd%> bg-light shadow-sm text-primary me-3 flex-shrink-0 border border-white"><i class="fas fa-envelope"></i></div>
                            <div><strong class="d-block text-dark mb-1"><%= Common.getBahasaConfig("Surat Elektronik (Email)") %></strong><%= Email != null ? Email : "-" %></div>
                        </li>
                    </ul>
                </div>
            </div>

        </div>
    </div>
</div>

<script>
// LOAD DATA FILTER JENIS SELEKSI
const loadJenisSeleksiPPDB<%=rnd%> = async () => {
    var reqObj = { "action": "daftar", "class": "<%=JenisSeleksi.class.getName()%>", "where1": "aktif = true or aktif is null", "order1": "asc", "sort1": "nama", "tanpaLogin": "true" };
    try {
        const res = await fetch('<%=Common.ROOT%>/Data?baru=true', {
            method: 'POST', headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' }, body: JSON.stringify(reqObj)
        });
        const textResponse = await res.text();
        if (textResponse.trim().startsWith("<")) throw new Error("Terblokir Filter");
        
        const dataResponse = JSON.parse(textResponse);
        const data = dataResponse.data || [];
        let options = '<option value=""><%= Common.getBahasaConfig("Semua Jalur Seleksi") %></option>';
        data.forEach((row) => { options += '<option value="' + row.id + '">' + row.nama + '</option>'; });
        document.getElementById('filterSeleksi<%=rnd%>').innerHTML = options;
    } catch (e) { 
        document.getElementById('filterSeleksi<%=rnd%>').innerHTML = '<option value="">-- <%= Common.getBahasaConfig("Gagal memuat data jalur seleksi") %> --</option>';
    }
};

// FUNGSI PAGING & FILTER PENGUMUMAN
window.loadPengumumanPPDB<%=rnd%> = async (page) => {
    if (!page) page = 1;
    const container = document.getElementById('listPengumuman<%=rnd%>');
    const inputCari = document.getElementById('cariPengumuman<%=rnd%>');
    let keywordParam = '';
    if (inputCari && inputCari.value.trim() !== '') keywordParam = '&keyword=' + encodeURIComponent(inputCari.value.trim());
    
    container.innerHTML = '<div class="p-4 text-center text-muted small"><div class="spinner-border spinner-border-sm text-danger mb-2" role="status"></div><br><%= Common.getBahasaConfig("Mencari informasi PPDB...") %></div>';
    try {
        const url = '<%=linkPengumuman%>&page=' + page + '&rnd=<%=rnd%>' + keywordParam;
        const response = await fetch(url);
        if (response.ok) container.innerHTML = await response.text(); 
        else throw new Error("Gagal");
    } catch (error) { 
        container.innerHTML = '<div class="p-4 text-center text-danger small"><i class="fas fa-exclamation-triangle fa-2x mb-2 d-block"></i> <%= Common.getBahasaConfig("Terjadi kesalahan pada saat memuat pengumuman terbaru.") %></div>';
    }
};

// FUNGSI PAGING & FILTER GELOMBANG
window.loadGelombangPPDB<%=rnd%> = async (page) => {
    if (!page) page = 1;
    const container = document.getElementById('containerGelombang<%=rnd%>');
    const ta = encodeURIComponent(document.getElementById('filterTA<%=rnd%>').value);
    const seleksiId = encodeURIComponent(document.getElementById('filterSeleksi<%=rnd%>').value);
    const statusVal = encodeURIComponent(document.getElementById('filterStatus<%=rnd%>').value);
    
    container.innerHTML = '<div class="col-12 text-center py-5"><div class="spinner-border text-primary mb-3" style="width: 3rem; height: 3rem;"></div><div class="text-muted fw-bold"><%= Common.getBahasaConfig("Sistem sedang memuat data gelombang PPDB...") %></div></div>';
    try {
        const url = '<%=linkGelombang%>&ta=' + ta + '&seleksi=' + seleksiId + '&status=' + statusVal + '&page=' + page + '&rnd=<%=rnd%>';
        const response = await fetch(url);
        if (response.ok) container.innerHTML = await response.text();
        else throw new Error("Gagal");
    } catch (error) { 
        container.innerHTML = '<div class="col-12 text-center text-danger py-5 bg-white shadow-sm rounded-4"><i class="fas fa-exclamation-triangle fa-2x mb-2 d-block"></i><%= Common.getBahasaConfig("Terjadi kesalahan pada saat memuat data gelombang pendaftaran.") %></div>';
    }
};

// FUNGSI MEMBUKA DETAIL PENGUMUMAN
window.bukaPengumumanRinciPPDB<%=rnd%> = function(idPengumuman) {
    window.location.href = '<%=linkPengumumanRinci%>&id=' + idPengumuman;
};

// FUNGSI MEMBUKA MODAL PENDAFTARAN
window.daftarGelombangPPDB<%=rnd%> = async (idGelombang) => {
    if(typeof window.showLoadingPPDB === 'function') window.showLoadingPPDB();
    try {
        const response = await fetch('<%=linkPendaftaran%>&gelombangId=' + idGelombang);
        const content = await response.text();
        
        const modalId = 'modalDaftarPpdb<%=rnd%>';
        if(document.getElementById(modalId)) document.getElementById(modalId).remove();
        
        const modalHtml = 
            '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">' +
                '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                    '<div class="modal-content shadow-lg border-0 rounded-4">' +
                        '<div class="modal-header bg-light border-0 py-3 px-4">' +
                            '<h5 class="modal-title fw-bold text-dark"><i class="fas fa-file-signature text-primary me-2"></i><%= Common.getBahasaConfig("Formulir Pendaftaran Siswa Baru") %></h5>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body p-0 bg-light">' + content + '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const modalElement = document.getElementById(modalId);
        
        // Eksekusi ulang script di dalam form AJAX
        const scriptsArray = Array.from(modalElement.getElementsByTagName('script'));
        for (let i = 0; i < scriptsArray.length; i++) {
            const oldScript = scriptsArray[i];
            if (oldScript.src && oldScript.src.includes('email-decode')) continue; 
            const scriptNode = document.createElement('script');
            var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
            Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
            scriptNode.type = 'text/javascript';
            if (srcEff) { scriptNode.src = srcEff; document.body.appendChild(scriptNode); }
            else { scriptNode.text = oldScript.innerHTML; document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode); }
        }

        if(typeof window.hideLoadingPPDB === 'function') window.hideLoadingPPDB();
        new bootstrap.Modal(modalElement).show();
        modalElement.addEventListener('hidden.bs.modal', function () { this.remove(); });
    } catch (error) {
        if(typeof window.hideLoadingPPDB === 'function') window.hideLoadingPPDB();
        if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Gagal memuat formulir pendaftaran. Silakan coba kembali.") %>', 'bg-danger text-white');
    }
};

// INISIALISASI PEMUATAN DATA PERTAMA KALI
document.addEventListener("DOMContentLoaded", async () => {
    await loadJenisSeleksiPPDB<%=rnd%>(); 
    loadGelombangPPDB<%=rnd%>(1);
    loadPengumumanPPDB<%=rnd%>(1);
});
</script>