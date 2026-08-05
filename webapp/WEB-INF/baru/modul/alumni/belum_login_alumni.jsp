<%@page import="ais.database.model.PengumumanAkademis"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<jsp:include page="/WEB-INF/baru/modul/alumni/header_alumni.jsp"></jsp:include>

<%
    String rnd = Common.getGeneratedBarCode(7);
    String linkPengumumanAlumni = Common.ROOT + "/alumni?hanya_tampil_jsp=true&p=alumni&s=_pengumuman_alumni&baru=true";
    String linkPengumumanRinci = Common.ROOT + "/alumni?hanya_tampil_jsp=true&p=home&s=pengumuman_rinci&baru=true";
    String linkLoginAlumni = Common.ROOT + "/alumni?hanya_tampil_jsp=true&p=alumni&s=tracer_study&action=proses_login_alumnii&baru=true";
    
    // Variabel khusus Modal Login
    java.util.Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
    int currentYear = cal.get(java.util.Calendar.YEAR);
    
    StringBuilder optTgl = new StringBuilder();
    for (int i = 1; i <= 31; i++) {
        String val = String.format("%02d", i);
        optTgl.append("<option value=\"").append(val).append("\">").append(val).append("</option>");
    }
    
    StringBuilder optBln = new StringBuilder();
    String[] namaBulan = { Common.getBahasaConfig("Januari"), Common.getBahasaConfig("Februari"), Common.getBahasaConfig("Maret"), Common.getBahasaConfig("April"), Common.getBahasaConfig("Mei"), Common.getBahasaConfig("Juni"), Common.getBahasaConfig("Juli"), Common.getBahasaConfig("Agustus"), Common.getBahasaConfig("September"), Common.getBahasaConfig("Oktober"), Common.getBahasaConfig("November"), Common.getBahasaConfig("Desember") };
    for (int i = 0; i < 12; i++) {
        String val = String.format("%02d", i + 1);
        optBln.append("<option value=\"").append(val).append("\">").append(namaBulan[i]).append("</option>");
    }
    
    StringBuilder optThn = new StringBuilder();
    for (int i = currentYear; i >= currentYear - 100; i--) {
        optThn.append("<option value=\"").append(i).append("\">").append(i).append("</option>");
    }
%>

<style>
    .nav-tabs .nav-link { color: #6c757d; font-weight: 600; background-color: #f8f9fa; border: 1px solid #dee2e6; margin-right: 2px; transition: all 0.3s ease; }
    .nav-tabs .nav-link:hover { background-color: #e9ecef; }
    .nav-tabs .nav-link.active { color: #0d6efd; background-color: #fff; border-bottom-color: transparent; }
</style>

<div class="container mb-5" style="position: relative; z-index: 1; min-height: 55vh;">
    <div class="row g-4">
        <div class="col-lg-12">
            
            <ul class="nav nav-tabs border-bottom-0" id="alumniTabs<%=rnd%>" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link active rounded-top-3 px-4 py-3" id="pengumuman-tab<%=rnd%>" data-bs-toggle="tab" data-bs-target="#pengumuman<%=rnd%>" type="button" role="tab" aria-selected="true" onclick="if(!window.pengumumanLoaded<%=rnd%>) { loadPengumuman<%=rnd%>(1); window.pengumumanLoaded<%=rnd%> = true; }">
                        <i class="fas fa-bullhorn me-2 text-primary"></i><%= Common.getBahasaConfig("Pengumuman") %>
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link rounded-top-3 px-4 py-3" id="galeri-tab<%=rnd%>" data-bs-toggle="tab" data-bs-target="#galeri<%=rnd%>" type="button" role="tab" aria-selected="false" onclick="if(!window.galeriLoaded<%=rnd%>) { loadGaleri<%=rnd%>(); window.galeriLoaded<%=rnd%> = true; }">
                        <i class="fas fa-images me-2 text-success"></i><%= Common.getBahasaConfig("Galeri Kegiatan") %>
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link rounded-top-3 px-4 py-3" id="tracer-tab<%=rnd%>" data-bs-toggle="tab" data-bs-target="#tracer<%=rnd%>" type="button" role="tab" aria-selected="false">
                        <i class="fas fa-clipboard-check me-2 text-warning"></i><%= Common.getBahasaConfig("Pengisian Tracer Study") %>
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link rounded-top-3 px-4 py-3" id="daftar-tab<%=rnd%>" data-bs-toggle="tab" data-bs-target="#daftar<%=rnd%>" type="button" role="tab" aria-selected="false" onclick="if(!window.daftarLoaded<%=rnd%>) { loadDaftarAlumni<%=rnd%>(1); window.daftarLoaded<%=rnd%> = true; }">
                        <i class="fas fa-users me-2 text-info"></i><%= Common.getBahasaConfig("Direktori Lulusan") %>
                    </button>
                </li>
                <li class="nav-item" role="presentation">
				    <button class="nav-link rounded-top-3 px-4 py-3" id="statistik-tab<%=rnd%>" data-bs-toggle="tab" data-bs-target="#statistik<%=rnd%>" type="button" role="tab" aria-selected="false" onclick="if(!window.statistikLoaded<%=rnd%>) { loadStatistikAlumni<%=rnd%>(); window.statistikLoaded<%=rnd%> = true; }">
				        <i class="fas fa-chart-pie me-2 text-danger"></i><%= Common.getBahasaConfig("Statistik Alumni") %>
				    </button>
				</li>
            </ul>

            <div class="tab-content card shadow rounded-4 rounded-top-0 border-0 bg-white p-4" id="alumniTabsContent<%=rnd%>">
                
                <div class="tab-pane fade show active" id="pengumuman<%=rnd%>" role="tabpanel">
                    <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
                        <h5 class="mb-0 fw-bold text-primary">
                            <i class="fas fa-newspaper me-2"></i><%= Common.getBahasaConfig("Informasi & Lowongan Karir Terkini") %>
                        </h5>
                        <div class="input-group input-group-sm w-25 shadow-sm rounded-pill overflow-hidden">
                            <input type="text" id="cariPengumuman<%=rnd%>" class="form-control border-0 bg-light shadow-none px-3" placeholder="<%= Common.getBahasaConfig("Cari informasi...") %>" onkeydown="if(event.key === 'Enter') loadPengumuman<%=rnd%>(1)">
                            <button class="btn btn-primary text-white px-3 shadow-none border-0" type="button" onclick="loadPengumuman<%=rnd%>(1)">
                                <i class="fas fa-search"></i>
                            </button>
                        </div>
                    </div>
                    <div id="listPengumuman<%=rnd%>" class="min-vh-50">
                        <div class="p-5 text-center text-muted fw-bold"><%= Common.getBahasaConfig("Sistem sedang memuat data informasi...") %></div>
                    </div>
                </div>

                <div class="tab-pane fade" id="galeri<%=rnd%>" role="tabpanel">
                    <div class="p-5 text-center text-muted">
                        <i class="fas fa-images fa-4x mb-3 text-secondary opacity-50"></i>
                        <h5 class="fw-bold"><%= Common.getBahasaConfig("Galeri Dokumen Kegiatan") %></h5>
                        <p><%= Common.getBahasaConfig("Harap tunggu sebentar, sistem sedang mengunduh aset gambar...") %></p>
                    </div>
                </div>

                <div class="tab-pane fade" id="tracer<%=rnd%>" role="tabpanel">
                    <div class="p-5 text-center text-muted py-5 my-3">
                        <i class="fas fa-file-signature fa-5x mb-4 text-primary opacity-75"></i>
                        <h4 class="fw-bold text-dark mb-3"><%= Common.getBahasaConfig("Layanan Kuesioner Tracer Study") %></h4>
                        <p class="mb-4 lead text-secondary mx-auto" style="max-width: 600px;">
                            <%= Common.getBahasaConfig("Pastikan Anda sudah mengautentikasi data diri terlebih dahulu. Kontribusi Anda dalam menjawab kuesioner ini sangat berarti bagi akreditasi dan pengembangan kualitas institusi ke depan.") %>
                        </p>
                        <button class="btn btn-primary btn-lg rounded-pill fw-bold shadow px-5 py-3 mt-2" onclick="bukaModalLoginAlumni<%=rnd%>()">
                            <i class="fas fa-sign-in-alt me-2"></i><%= Common.getBahasaConfig("Mulai Autentikasi Pengguna") %>
                        </button>
                    </div>
                </div>

                <div class="tab-pane fade" id="daftar<%=rnd%>" role="tabpanel">
                    <div class="p-5 text-center text-muted">
                        <i class="fas fa-users fa-4x mb-3 text-secondary opacity-50"></i>
                        <h5 class="fw-bold"><%= Common.getBahasaConfig("Direktori Pencarian Lulusan") %></h5>
                    </div>
                </div>

                <div class="tab-pane fade" id="statistik<%=rnd%>" role="tabpanel">
                    <div class="p-5 text-center text-muted">
                        <i class="fas fa-chart-line fa-4x mb-3 text-secondary opacity-50"></i>
                        <h5 class="fw-bold"><%= Common.getBahasaConfig("Analitik Sebaran Alumni") %></h5>
                    </div>
                </div>

            </div>
        </div>
    </div>
</div>

<div id="globalLoader<%=rnd%>" class="d-none align-items-center justify-content-center" style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.65); z-index: 9999; backdrop-filter: blur(4px);">
    <div class="text-center text-white bg-dark bg-opacity-50 p-5 rounded-4 shadow-lg">
        <div class="spinner-border text-primary mb-3" style="width: 4rem; height: 4rem;" role="status"></div>
        <h5 class="fw-bold"><%= Common.getBahasaConfig("Mohon Menunggu...") %></h5>
        <p class="small text-white-50 mb-0"><%= Common.getBahasaConfig("Sistem sedang memproses permintaan jaringan") %></p>
    </div>
</div>

<jsp:include page="/WEB-INF/baru/modul/alumni/footer_alumni.jsp"></jsp:include>

<script>
const loadPengumuman<%=rnd%> = async (page = 1) => {
    const container = document.getElementById('listPengumuman<%=rnd%>');
    const inputCari = document.getElementById('cariPengumuman<%=rnd%>');
    let keywordParam = '';
    if (inputCari && inputCari.value.trim() !== '') keywordParam = '&keyword=' + encodeURIComponent(inputCari.value.trim());
    container.innerHTML = '<div class="p-5 text-center text-muted small"><div class="spinner-border text-primary mb-3" style="width: 3rem; height: 3rem;" role="status"></div><br><%= Common.getBahasaConfig("Mencari data informasi...") %></div>';
    try {
        const response = await fetch('<%=linkPengumumanAlumni%>&page=' + page + '&rnd=<%=rnd%>' + keywordParam);
        if (response.ok) container.innerHTML = await response.text(); else throw new Error("Server error");
    } catch (error) { container.innerHTML = '<div class="p-5 text-center text-danger bg-danger bg-opacity-10 rounded-4"><i class="fas fa-exclamation-triangle fa-3x mb-3 d-block"></i> <%= Common.getBahasaConfig("Terjadi kendala koneksi pada saat memuat pengumuman.") %></div>';
    }
};

const bukaModalLoginAlumni<%=rnd%> = () => {
    const modalId = 'modalLoginAlumni<%=rnd%>';
    if(document.getElementById(modalId)) document.getElementById(modalId).remove();
    const modalHtml = 
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">' +
            '<div class="modal-dialog modal-dialog-centered">' +
                '<div class="modal-content shadow-lg border-0 rounded-4">' +
                    '<div class="modal-header bg-primary text-white border-0 py-3 rounded-top-4">' +
                        '<h5 class="modal-title fw-bold"><i class="fas fa-user-graduate me-2"></i><%= Common.getBahasaConfig("Autentikasi Pengguna") %></h5>' +
                        '<button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal" aria-label="Close"></button>' +
                    '</div>' +
                    '<div class="modal-body p-4 p-md-5">' +
                        '<p class="text-muted small mb-4 text-center lh-base"><%= Common.getBahasaConfig("Sistem membutuhkan verifikasi data diri Anda. Harap masukkan identitas serta Tanggal Lahir (berfungsi sebagai kata sandi keamanan).") %></p>' +
                        '<form id="formLoginAlumni<%=rnd%>" onsubmit="event.preventDefault(); prosesLoginAlumni<%=rnd%>();">' +
                            '<div class="mb-4">' +
                                '<label class="form-label fw-bold small text-secondary"><i class="fas fa-id-badge me-2 text-primary"></i><%= Common.getBahasaConfig("Nama Lengkap / Email / NIM") %></label>' +
                                '<input type="text" class="form-control form-control-lg shadow-sm border-0 bg-light" id="loginNim<%=rnd%>" placeholder="<%= Common.getBahasaConfig("Ketikkan identitas pencarian Anda di sini...") %>" required autocomplete="off">' +
                            '</div>' +
                            '<div class="mb-4">' +
                                '<label class="form-label fw-bold small text-secondary"><i class="fas fa-calendar-alt me-2 text-primary"></i><%= Common.getBahasaConfig("Tanggal Lahir (Sebagai Kata Sandi)") %></label>' +
                                '<div class="row g-2">' +
                                    '<div class="col-4"><select class="form-select form-select-lg shadow-sm border-0 bg-light fw-bold text-center" id="loginTgl<%=rnd%>" required><option value=""><%= Common.getBahasaConfig("Tgl") %></option><%= optTgl.toString() %></select></div>' +
                                    '<div class="col-4"><select class="form-select form-select-lg shadow-sm border-0 bg-light fw-bold text-center px-1" id="loginBln<%=rnd%>" required><option value=""><%= Common.getBahasaConfig("Bulan") %></option><%= optBln.toString() %></select></div>' +
                                    '<div class="col-4"><select class="form-select form-select-lg shadow-sm border-0 bg-light fw-bold text-center" id="loginThn<%=rnd%>" required><option value=""><%= Common.getBahasaConfig("Tahun") %></option><%= optThn.toString() %></select></div>' +
                                '</div>' +
                            '</div>' +
                            '<div class="d-grid mt-5 pt-2">' +
                                '<button type="submit" class="btn btn-primary btn-lg rounded-pill fw-bold shadow-sm" id="btnProsesLoginAlumni<%=rnd%>"><i class="fas fa-sign-in-alt me-2"></i><%= Common.getBahasaConfig("Jalankan Verifikasi") %></button>' +
                            '</div>' +
                        '</form>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    const modalElement = new bootstrap.Modal(document.getElementById(modalId));
    modalElement.show();
    document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });
};

const prosesLoginAlumni<%=rnd%> = async () => {
    const btn = document.getElementById('btnProsesLoginAlumni<%=rnd%>');
    const oriText = btn.innerHTML;
    const nimAtauNama = document.getElementById('loginNim<%=rnd%>').value.trim();
    const tgl = document.getElementById('loginTgl<%=rnd%>').value;
    const bln = document.getElementById('loginBln<%=rnd%>').value;
    const thn = document.getElementById('loginThn<%=rnd%>').value;
    
    if (!tgl || !bln || !thn) {
        if(typeof tampilkanToast === "function") tampilkanToast('<%= Common.getBahasaConfigJS("Dimohon untuk melengkapi kombinasi Tanggal, Bulan, dan Tahun Lahir Anda secara tepat.") %>', 'bg-warning text-dark');
        return;
    }
    
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span><%= Common.getBahasaConfig("Memeriksa Pangkalan Data...") %>';
    btn.disabled = true;

    const reqData = new URLSearchParams();
    reqData.append("identitas", nimAtauNama);
    reqData.append("tgl", tgl);
    reqData.append("bln", bln);
    reqData.append("thn", thn);
    try {
        const res = await fetch('<%=linkLoginAlumni%>', {
            method: 'POST', 
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, 
            body: reqData.toString()
        });
        const textResp = await res.text();
        let result;
        try { result = JSON.parse(textResp); } catch(err) { throw new Error("JSON Parse Error"); }
        
        if (result && result.status === 'sukses') {
            if(typeof tampilkanToast === "function") tampilkanToast(result.pesan, 'bg-success text-white');
            setTimeout(() => {
                window.location.reload(); 
            }, 700);
        } else {
            if(typeof tampilkanToast === "function") tampilkanToast(result.pesan || '<%= Common.getBahasaConfigJS("Kredensial yang diberikan tidak lolos verifikasi.") %>', 'bg-danger text-white');
            btn.innerHTML = oriText; btn.disabled = false;
        }
    } catch(e) {
        if(typeof tampilkanToast === "function") tampilkanToast('<%= Common.getBahasaConfigJS("Terjadi gagal sambung saat memvalidasi akses. Silakan coba dalam beberapa menit ke depan.") %>', 'bg-danger text-white');
        btn.innerHTML = oriText; btn.disabled = false;
    }
};

const showLoading<%=rnd%> = () => { const l = document.getElementById('globalLoader<%=rnd%>'); if(l) { l.classList.remove('d-none'); l.classList.add('d-flex'); } };
const hideLoading<%=rnd%> = () => { const l = document.getElementById('globalLoader<%=rnd%>'); if(l) { l.classList.remove('d-flex'); l.classList.add('d-none'); } };

const detailPengumuman<%=rnd%> = async (idPengumuman) => {
    showLoading<%=rnd%>();
    try {
        const response = await fetch('<%=linkPengumumanRinci%>&id=' + idPengumuman);
        const content = await response.text();
        const modalId = 'modalDetailPengumuman<%=rnd%>';
        if(document.getElementById(modalId)) document.getElementById(modalId).remove();
        const modalHtml = '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true"><div class="modal-dialog modal-dialog-centered modal-dialog-scrollable" style="min-width: 85vw; max-width: 85vw;"><div class="modal-content shadow border-0 rounded-4"><div class="modal-header bg-light border-bottom py-3 px-4"><h5 class="modal-title fw-bold text-dark"><i class="fas fa-info-circle text-primary me-2"></i><%= Common.getBahasaConfig("Lembar Rincian Informasi") %></h5><button type="button" class="btn-close shadow-none" data-bs-dismiss="modal"></button></div><div class="modal-body p-4 p-md-5">' + content + '</div><div class="modal-footer border-top bg-light p-3"><button type="button" class="btn btn-secondary rounded-pill fw-bold shadow-sm px-4" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Tutup Jendela Lembar") %></button></div></div></div></div>';
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        hideLoading<%=rnd%>();
        new bootstrap.Modal(document.getElementById(modalId)).show();
        document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });
    } catch (error) { 
        hideLoading<%=rnd%>();
        if(typeof tampilkanToast === "function") tampilkanToast('<%= Common.getBahasaConfigJS("Sistem gagal menarik dokumen pengumuman secara lengkap.") %>', 'bg-danger text-white');
    }
};

const loadGaleri<%=rnd%> = async () => {
    const container = document.getElementById('galeri<%=rnd%>');
    container.innerHTML = '<div class="p-5 text-center text-muted"><div class="spinner-border text-success mb-3" style="width: 3rem; height: 3rem;"></div><br><%= Common.getBahasaConfig("Sedang mengumpulkan visual galeri...") %></div>';
    try {
        const response = await fetch('<%=Common.ROOT%>/alumni?hanya_tampil_jsp=true&p=alumni&s=_galeri&baru=true&rnd=<%=rnd%>');
        if (response.ok) container.innerHTML = '<div class="p-2 p-md-4">' + await response.text() + '</div>';
        else throw new Error("Gagal mengambil data galeri");
    } catch (error) { container.innerHTML = '<div class="p-5 text-center text-danger bg-danger bg-opacity-10 rounded-4"><i class="fas fa-exclamation-triangle fa-3x mb-3 d-block"></i><%= Common.getBahasaConfig("Mengalami kendala sinkronisasi pada saat membuka tampilan galeri gambar.") %></div>'; }
};

const loadDaftarAlumni<%=rnd%> = async (page = 1) => {
    const container = document.getElementById('daftar<%=rnd%>');
    let params = '&page=' + page;
    const kw = document.getElementById('filterKeyword<%=rnd%>');
    if (kw && kw.value.trim() !== '') params += '&keyword=' + encodeURIComponent(kw.value.trim());
    const fak = document.getElementById('filterFakultas<%=rnd%>');
    if (fak && fak.value.trim() !== '') params += '&fakultasId=' + encodeURIComponent(fak.value.trim());
    const prod = document.getElementById('filterProdi<%=rnd%>');
    if (prod && prod.value.trim() !== '') params += '&prodiId=' + encodeURIComponent(prod.value.trim());
    const statK = document.getElementById('filterStatusKeluar<%=rnd%>');
    if (statK) params += '&statusKeluarId=' + encodeURIComponent(statK.value);
    const taS = document.getElementById('filterTaStart<%=rnd%>');
    if (taS && taS.value.trim() !== '') params += '&taStart=' + encodeURIComponent(taS.value.trim());
    const taE = document.getElementById('filterTaEnd<%=rnd%>');
    if (taE && taE.value.trim() !== '') params += '&taEnd=' + encodeURIComponent(taE.value.trim());
    const tlS = document.getElementById('filterTlStart<%=rnd%>');
    if (tlS && tlS.value.trim() !== '') params += '&tlStart=' + encodeURIComponent(tlS.value.trim());
    const tlE = document.getElementById('filterTlEnd<%=rnd%>');
    if (tlE && tlE.value.trim() !== '') params += '&tlEnd=' + encodeURIComponent(tlE.value.trim());
    container.innerHTML = '<div class="p-5 text-center text-muted"><div class="spinner-border text-info mb-3" style="width: 3rem; height: 3rem;"></div><br><%= Common.getBahasaConfig("Mempersiapkan filter dan menarik direktori data...") %></div>';
    try {
        const response = await fetch('<%=Common.ROOT%>/alumni?hanya_tampil_jsp=true&p=alumni&s=_daftar_alumni&baru=true&rnd=<%=rnd%>' + params);
        if (response.ok) container.innerHTML = await response.text(); else throw new Error("Gagal mengambil data direktori");
    } catch (error) { container.innerHTML = '<div class="p-5 text-center text-danger bg-danger bg-opacity-10 rounded-4"><i class="fas fa-exclamation-triangle fa-3x mb-3 d-block"></i><%= Common.getBahasaConfig("Komunikasi menuju mesin database direktori terputus. Mohon coba ulangi.") %></div>'; }
};

const loadStatistikAlumni<%=rnd%> = async () => {
    const container = document.getElementById('statistik<%=rnd%>');
    container.innerHTML = '<div class="p-5 text-center text-muted"><div class="spinner-border text-danger mb-3" style="width: 3rem; height: 3rem;"></div><br><%= Common.getBahasaConfig("Mesin sedang mengeksekusi analitik statistik masif...") %></div>';
    try {
        const response = await fetch('<%=Common.ROOT%>/alumni?hanya_tampil_jsp=true&p=alumni&s=_statistik_alumni&baru=true&rnd=<%=rnd%>');
        if (response.ok) {
            container.innerHTML = await response.text();
            const scriptsArray = Array.from(container.getElementsByTagName('script'));
            for (let i = 0; i < scriptsArray.length; i++) {
                const oldScript = scriptsArray[i];
                const scriptNode = document.createElement('script');
                var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
                Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
                scriptNode.type = 'text/javascript';
                if (srcEff) { scriptNode.src = srcEff; document.body.appendChild(scriptNode); }
                else { scriptNode.text = oldScript.innerHTML; document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode); }
            }
        } else throw new Error("Gagal mengambil data statistik");
    } catch (error) { container.innerHTML = '<div class="p-5 text-center text-danger bg-danger bg-opacity-10 rounded-4"><i class="fas fa-exclamation-triangle fa-3x mb-3 d-block"></i><%= Common.getBahasaConfig("Sistem operasi tidak merespons pada saat pembacaan laporan diagram statistik.") %></div>'; }
};

document.addEventListener("DOMContentLoaded", async () => {
    loadPengumuman<%=rnd%>(1);
    window.pengumumanLoaded<%=rnd%> = true;
});
</script>