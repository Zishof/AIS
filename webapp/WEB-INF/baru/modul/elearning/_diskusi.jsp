<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    return; 
}

String random = Common.getGeneratedBarCode(7);
String idPertemuanParam = request.getParameter("idPertemuan");

if (idPertemuanParam == null || idPertemuanParam.trim().isEmpty()) {
    out.println("<div class='alert alert-danger text-center shadow-sm fw-bold'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Identitas pertemuan tidak ditemukan di dalam sistem.") + "</div>");
    return;
}

Pertemuan pertemuan = null;
boolean isKomentarDitutup = false;
Session mySession = null;
try {
    mySession = HibernateUtil.getSessionFactory().openSession();
    pertemuan = (Pertemuan) mySession.get(Pertemuan.class, Long.parseLong(idPertemuanParam.trim()));
    if (pertemuan != null) {
        isKomentarDitutup = pertemuan.getKomentarDitutup() != null && pertemuan.getKomentarDitutup();
    }
} catch(Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/_diskusi.jsp:35");
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

if(pertemuan == null) return;
%>

<style>
    .avatar-bulat-<%=random%> { min-width: 60px; max-width: 70px; min-height: 70px; object-fit: cover; border-radius: 8px; border: 1px solid #ced4da; cursor: pointer; }
    .avatar-bulat-kecil-<%=random%> { min-width: 40px; max-width: 50px; min-height: 50px; object-fit: cover; border-radius: 8px; border: 1px solid #ced4da; cursor: pointer; }
    
    .gelembung-komentar-<%=random%> {
        font-size: 12.5px;
        padding: 12px 16px;
        background-color: rgba(169, 169, 169, 0.15); 
        border-radius: 20px;
        border: 1px solid #d1cdcd;
        margin-bottom: 8px;
        line-height: 1.6;
        color: #333;
    }
    
    .header-gelembung-<%=random%> { color: blue; font-weight: 600; margin-bottom: 6px; }
    .garis-balasan-<%=random%> { border-left: 2px dashed #dee2e6; margin-left: 28px; padding-left: 1.5rem; }
    .btn-aksi-diskusi-<%=random%> { font-size: 10px; color: #0000EE; text-decoration: underline; background: none; border: none; padding: 0; cursor: pointer; }
    .btn-aksi-diskusi-<%=random%>:hover { color: #0000AA; text-decoration: none; }
</style>

<div class="container-fluid py-3" id="areaDiskusiUtama<%=random%>">

    <div class="d-flex justify-content-between align-items-center border-bottom pb-3 mb-4">
        <div>
            <h5 class="fw-bold text-dark mb-1"><i class="fas fa-comments text-primary me-2"></i><%= Common.getBahasaConfig("Ruang Diskusi & Interaksi") %></h5>
            <small class="text-muted fw-bold">
                <%= Common.getBahasaConfig("Untuk memulai diskusi, tulislah komentar, pertanyaan, atau suatu link yang mengarah ke website / file tertentu, bisa juga berupa postingan facebook, twitter, instagram, atau juga link youtube, google drive, dropbox, dll.") %>
            </small>
        </div>
        <div>
            <% if (isKomentarDitutup) { %>
                <span class="badge bg-danger bg-opacity-10 text-danger border border-danger px-3 py-2 rounded-pill shadow-sm"><i class="fas fa-lock me-1"></i> <%= Common.getBahasaConfig("Sesi Diskusi Telah Ditutup") %></span>
            <% } else { %>
                <span class="badge bg-success bg-opacity-10 text-success border border-success px-3 py-2 rounded-pill shadow-sm"><i class="fas fa-lock-open me-1"></i> <%= Common.getBahasaConfig("Sesi Diskusi Terbuka") %></span>
            <% } %>
        </div>
    </div>

    <% if (isKomentarDitutup) { %>
        <div class="text-center py-3 mb-4">
            <span style="font-size: 12px; font-weight: bolder; color: red;"><%= Common.getBahasaConfig("Untuk pertemuan ini, diskusi telah ditutup") %></span>
        </div>
    <% } else { %>
        <div class="card border-0 shadow-sm rounded-4 mb-5">
            <div class="card-body p-4 bg-light bg-opacity-50">
                <form id="formKomentarUtama<%=random%>">
                    <div class="d-flex gap-3 align-items-start">
                        <img src="<%= CommonMedia.getUrlFotoPengguna(tbmuser) %>" class="avatar-bulat-<%=random%> shadow-sm" alt="User" onclick="tampilGambar(this)">
                        <div class="flex-grow-1">
                            <textarea class="form-control shadow-sm" id="teksKomentarUtama<%=random%>" rows="2" placeholder="<%= Common.getBahasaConfig("Tuliskan gagasan atau pertanyaan Anda secara terperinci di sini...") %>" style="border: 1px solid #9fb8bf; border-radius: 5px; resize: none;"></textarea>
                            
                            <div class="d-flex justify-content-end align-items-center mt-2 gap-3">
                                <button type="button" class="btn btn-sm btn-outline-primary fw-bold shadow-sm d-flex flex-column align-items-center" onclick="simpanKomentar<%=random%>(this, null)">
                                    <i class="fas fa-paper-plane mb-1"></i> <span style="font-size: 10px;"><%= Common.getBahasaConfig("Kirim") %></span>
                                </button>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    <% } %>

    <div id="wadahDaftarKomentar<%=random%>" class="d-flex flex-column gap-3"></div>

</div>

<script>
    // ==========================================
    // 1. AJAX FETCH DAFTAR KOMENTAR (SERVICE)
    // ==========================================
    function loadDaftarDiskusi<%=random%>() {
        const wadah = document.getElementById('wadahDaftarKomentar<%=random%>');
        if (!wadah) return;
        
        wadah.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary" role="status"></div><div class="mt-2 text-muted fw-bold"><%= Common.getBahasaConfig("Sedang memuat data diskusi...") %></div></div>';
        
        fetch('<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_load_diskusi&idPertemuan=<%= idPertemuanParam %>&rnd=<%= random %>')
        .then(response => {
            if(!response.ok) throw new Error("Network response was not ok");
            return response.text();
        })
        .then(html => {
            wadah.innerHTML = html;
        })
        .catch(err => {
            console.error(err);
            wadah.innerHTML = '<div class="text-center py-5 text-danger"><i class="fas fa-exclamation-triangle fa-3x mb-3"></i><h6><%= Common.getBahasaConfig("Gagal terhubung ke server untuk memuat diskusi.") %></h6></div>';
        });
    }

    // Panggil saat modal pertama kali terbuka
    loadDaftarDiskusi<%=random%>();

    // ==========================================
    // 2. MANAJEMEN UI DISKUSI (TOGGLE BALASAN)
    // ==========================================
    function tampilFormBalasan<%=random%>(idParent) {
        const formWadah = document.getElementById('formBalasan-' + idParent);
        if (formWadah) {
            if (formWadah.classList.contains('d-none')) {
                document.querySelectorAll('.form-balasan-<%=random%>').forEach(el => el.classList.add('d-none'));
                formWadah.classList.remove('d-none');
                formWadah.querySelector('.input-balasan-<%=random%>').focus();
            } else {
                formWadah.classList.add('d-none');
                formWadah.querySelector('.input-balasan-<%=random%>').value = '';
            }
        }
    }

    // ==========================================
    // 3. FUNGSI PENYIMPANAN KOMENTAR (AJAX)
    // ==========================================
    function simpanKomentar<%=random%>(btnElement, idParent) {
        const idPertemuan = '<%= idPertemuanParam %>';
        let isiKomentar = '';
        
        if (idParent == null) {
            isiKomentar = document.getElementById('teksKomentarUtama<%=random%>').value.trim();
        } else {
            const formWadah = document.getElementById('formBalasan-' + idParent);
            if (formWadah) isiKomentar = formWadah.querySelector('.input-balasan-<%=random%>').value.trim();
        }
        
        if (isiKomentar === '') {
            if (typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Masukkan isi diskusi Anda.") %>', 'bg-danger');
            else alert('<%= Common.getBahasaConfigJS("Masukkan isi diskusi Anda.") %>');
            return;
        }

        const teksAsliBtn = btnElement.innerHTML;
        btnElement.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>`;
        btnElement.disabled = true;

        const formData = new URLSearchParams();
        formData.append('idPertemuan', idPertemuan);
        formData.append('idParent', idParent || '');
        formData.append('isiKomentar', isiKomentar);

        fetch('<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_simpan_diskusi', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(response => {
            if (!response.ok) throw new Error('<%= Common.getBahasaConfigJS("Kegagalan pada jaringan server.") %>');
            return response.json(); 
        })
        .then(data => {
            if (data.status === 'success') {
                if (typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Diskusi berhasil dikirim.") %>', 'bg-success text-white');
                
                // Bersihkan field yang diketik
                if (idParent == null) {
                    document.getElementById('teksKomentarUtama<%=random%>').value = '';
                } else {
                    const formWadah = document.getElementById('formBalasan-' + idParent);
                    if (formWadah) {
                        formWadah.querySelector('.input-balasan-<%=random%>').value = '';
                        formWadah.classList.add('d-none');
                    }
                }
                
                // REFRESH DATA SECARA SILENT TANPA MENUTUP POPUP
                loadDaftarDiskusi<%=random%>();
                
            } else {
                alert(data.message || '<%= Common.getBahasaConfigJS("Gagal mengirim.") %>');
            }
        })
        .catch(error => {
            console.error(error);
            alert('<%= Common.getBahasaConfigJS("Gagal menghubungi server.") %>');
        })
        .finally(() => {
            btnElement.innerHTML = teksAsliBtn;
            btnElement.disabled = false;
        });
    }

    // ==========================================
    // 4. FUNGSI PENGHAPUSAN KOMENTAR (AJAX) 
    // ==========================================
    function hapusKomentar<%=random%>(idKomentar) {
        const pesanKonfirmasi = '<%= Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus diskusi ini secara permanen? Perhatian: Menghapus diskusi utama akan turut menghapus seluruh balasan di dalamnya.") %>';
        const eksekusiHapus = function() {
            const formData = new URLSearchParams();
            formData.append('idKomentar', idKomentar);
            formData.append('action', 'delete');

            fetch('<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_hapus_diskusi', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData.toString()
            })
            .then(response => {
                if (!response.ok) throw new Error('<%= Common.getBahasaConfigJS("Kegagalan jaringan.") %>');
                return response.json(); 
            })
            .then(data => {
                if (data.status === 'success') {
                    if (typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Diskusi berhasil dihapus.") %>', 'bg-success text-white');
                    
                    const elemenKomentar = document.getElementById('komentar-' + idKomentar);
                    if (elemenKomentar) {
                        elemenKomentar.style.opacity = '0';
                        // Memberi waktu CSS memudar, lalu load ulang datanya
                        setTimeout(() => loadDaftarDiskusi<%=random%>(), 400); 
                    } else {
                        loadDaftarDiskusi<%=random%>();
                    }
                } else {
                    alert(data.message || '<%= Common.getBahasaConfigJS("Data ini tidak dapat dihapus.") %>');
                }
            })
            .catch(error => {
                console.error(error);
                alert('<%= Common.getBahasaConfigJS("Data ini tidak dapat dihapus.") %>');
            });
        };

        if (typeof showConfirmModal === 'function') {
            showConfirmModal(pesanKonfirmasi, eksekusiHapus);
        } else {
            if (confirm(pesanKonfirmasi)) {
                eksekusiHapus();
            }
        }
    }

    // ==========================================
    // 5. AUTO REFRESH PERTEMUAN SAAT POPUP DITUTUP
    // ==========================================
    setTimeout(function() {
        const areaDiskusi = document.getElementById('areaDiskusiUtama<%=random%>');
        if (areaDiskusi) {
            // Mencari container modal (popup) terdekat tempat file ini di-load
            const parentModal = areaDiskusi.closest('.modal');
            
            // Pasang event listener jika belum pernah dipasang sebelumnya
            if (parentModal && !parentModal.dataset.listenerTutupDitambahkan) {
                parentModal.addEventListener('hidden.bs.modal', function (event) {
                    if (typeof window.generatePertemuanHTML === 'function') {
                        // Memanggil global script Pertemuan
                        window.generatePertemuanHTML(<%=idPertemuanParam%>);
                    }
                });
                parentModal.dataset.listenerTutupDitambahkan = 'true';
            }
        }
    }, 300);
</script>