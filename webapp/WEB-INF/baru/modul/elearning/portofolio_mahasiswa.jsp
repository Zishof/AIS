<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
String rnd = Common.getGeneratedBarCode(7);
Tbmuser tbmuser = Common.getCurrentUser(request);

if (tbmuser == null || tbmuser.getUserId() == null) {
    out.print("<div class='alert alert-danger m-4 shadow-sm'>" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "</div>");
    return;
}

boolean isMahasiswa = tbmuser.getMahasiswa() != null;
String initialMahasiswaId = isMahasiswa ? tbmuser.getMahasiswa().getId().toString() : "";
String initialMahasiswaNama = isMahasiswa ? tbmuser.getMahasiswa().getNama() + " (" + tbmuser.getMahasiswa().getNim() + ")" : "";
%>

<style>
    @media print {
        body * { visibility: hidden; }
        .modal, .modal-backdrop, .no-print { display: none !important; }
        #printAreaPortofolio<%=rnd%>, #printAreaPortofolio<%=rnd%> * { visibility: visible; }
        #printAreaPortofolio<%=rnd%> { position: absolute; left: 0; top: 0; width: 100%; background-color: white !important; padding: 0 !important; margin: 0 !important; }
        .table-portofolio { width: 100% !important; border-collapse: collapse !important; font-family: 'Times New Roman', Times, serif; font-size: 10pt; color: #000; margin-bottom: 20px;}
        .table-portofolio th, .table-portofolio td { border: 1px solid #000 !important; padding: 6px !important; }
        .table-portofolio th { background-color: #f2f2f2 !important; -webkit-print-color-adjust: exact; color-adjust: exact; font-weight: bold; text-align: center; }
        @page { size: A4 landscape; margin: 15mm; }
    }

    .table-portofolio { width: 100%; border-collapse: collapse; font-family: 'Times New Roman', Times, serif; font-size: 11pt; color: #000; margin-bottom: 20px; }
    .table-portofolio th, .table-portofolio td { border: 1px solid #dee2e6; padding: 10px; vertical-align: middle; }
    .table-portofolio th { text-align: center; font-weight: bold; background-color: #f8f9fa; }
    .status-tercapai { color: #198754; font-weight: bold; }
    .status-gagal { color: #dc3545; font-weight: bold; }
</style>

<div class="card border-0 shadow-sm rounded-4 mb-4 animate__animated animate__fadeIn">
    <div class="card-body p-4 bg-light rounded-4 no-print">
        <h6 class="fw-bold text-primary mb-3"><i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Pencarian Mahasiswa")%></h6>
        <div class="input-group shadow-sm">
            <span class="input-group-text bg-white border-end-0 text-primary"><i class="fas fa-user-graduate"></i></span>
            <input type="text" class="form-control border-start-0" id="namaMhsPorto<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Silakan cari dan pilih mahasiswa berdasarkan NIM atau Nama...")%>" readonly value="<%=initialMahasiswaNama%>">
            
            <% if (!isMahasiswa) { %>
            <button class="btn btn-primary fw-bold px-4" type="button" onclick="bukaModalCariMahasiswa<%=rnd%>()">
                <i class="fas fa-list me-2"></i><%=Common.getBahasaConfig("Pilih Data")%>
            </button>
            <% } %>
        </div>
        <% if (!isMahasiswa) { %>
            <div class="mt-3 text-muted small"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Pilih mahasiswa terlebih dahulu untuk melihat daftar mata kuliah dan rekapitulasi portofolionya.")%></div>
        <% } %>
    </div>
</div>

<div id="containerHasilPortofolio<%=rnd%>" class="animate__animated animate__fadeIn"></div>

<script>
    var rootUrlPorto<%=rnd%> = "<%=Common.ROOT%>";
    var serviceUrlPorto<%=rnd%> = rootUrlPorto<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_portofolio_mahasiswa_service&var=<%=rnd%>";
    var serviceGabunganUrlPorto<%=rnd%> = rootUrlPorto<%=rnd%> + "/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_portofolio_mahasiswa_gabungan_service&var=<%=rnd%>";

    window.bukaModalCariMahasiswa<%=rnd%> = function() {
        var htmlContent = `
            <div class="p-2">
                <h6 class="fw-bold text-primary mb-3"><i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Cari Data Mahasiswa")%></h6>
                <div class="input-group mb-3 shadow-sm">
                    <input type="text" id="keywordCariMhs<%=rnd%>" class="form-control" placeholder="<%=Common.getBahasaConfig("Masukkan NIM atau Nama...")%>" onkeypress="if(event.keyCode==13) cariMahasiswaAjax<%=rnd%>()">
                    <button class="btn btn-primary fw-bold px-4" type="button" onclick="cariMahasiswaAjax<%=rnd%>()"><i class="fas fa-search"></i></button>
                </div>
                <div id="hasilPencarianMhs<%=rnd%>" class="list-group shadow-sm" style="max-height: 350px; overflow-y: auto;">
                    <div class="text-center text-muted small py-4"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Ketik minimal 3 karakter untuk mulai mencari.")%></div>
                </div>
            </div>
        `;
        
        if (typeof showConfirmModal === 'function') {
            showConfirmModal(htmlContent, function(){});
        } else {
            if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Fungsi modal global tidak ditemukan.")%>', 'bg-danger text-white');
        }
    };

    window.cariMahasiswaAjax<%=rnd%> = async function() {
        var keyword = document.getElementById('keywordCariMhs<%=rnd%>').value.trim();
        if(keyword.length < 3) {
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Mohon ketik minimal 3 karakter.")%>', 'bg-warning text-dark');
            return;
        }
        
        var container = document.getElementById('hasilPencarianMhs<%=rnd%>');
        container.innerHTML = '<div class="text-center py-4"><div class="spinner-border text-primary spinner-border-sm"></div></div>';
        
        try {
            var url = serviceUrlPorto<%=rnd%> + "&type=search_mhs&keyword=" + encodeURIComponent(keyword);
            var response = await fetch(url);
            var htmlResult = await response.text();
            container.innerHTML = htmlResult;
        } catch(e) {
            container.innerHTML = '<div class="text-danger text-center py-4"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Gagal menghubungi peladen.")%></div>';
        }
    };

    window.pilihMahasiswaDariPencarian<%=rnd%> = function(id, nama, nim) {
        document.getElementById('namaMhsPorto<%=rnd%>').value = nama + " (" + nim + ")";
        window.setMahasiswaPorto<%=rnd%>(id);
        
        var modalEl = document.querySelector('.modal.show');
        if(modalEl) {
            var modalInstance = bootstrap.Modal.getInstance(modalEl);
            if(modalInstance) modalInstance.hide();
        }
    };

    window.setMahasiswaPorto<%=rnd%> = function(idMhs) {
        if (!idMhs) return;
        var container = document.getElementById('containerHasilPortofolio<%=rnd%>');
        container.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary" role="status"></div><div class="mt-2 text-muted fw-bold"><%=Common.getBahasaConfig("Memuat daftar mata kuliah...")%></div></div>';
        
        var url = serviceUrlPorto<%=rnd%> + "&type=list&mahasiswa=" + idMhs;
        if(typeof loadContentIntoContainer === 'function') {
            loadContentIntoContainer(url, 'containerHasilPortofolio<%=rnd%>');
        }
    };

    window.lihatPortofolioKpm<%=rnd%> = function(idMhs, idKpm, idPerkuliahan) {
        var container = document.getElementById('containerHasilPortofolio<%=rnd%>');
        container.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-success" role="status"></div><div class="mt-2 text-muted fw-bold"><%=Common.getBahasaConfig("Mengkalkulasi capaian OBE mahasiswa...")%></div></div>';
        
        var url = serviceUrlPorto<%=rnd%> + "&type=detail&mahasiswa=" + idMhs + "&kurikulumPunyaMatakuliah=" + idKpm + "&perkuliahan=" + idPerkuliahan;
        if(typeof loadContentIntoContainer === 'function') {
            loadContentIntoContainer(url, 'containerHasilPortofolio<%=rnd%>');
        }
    };

    // FUNGSI UNTUK MEMANGGIL PORTOFOLIO GABUNGAN
    window.lihatPortofolioGabungan<%=rnd%> = function(idMhs) {
        var container = document.getElementById('containerHasilPortofolio<%=rnd%>');
        container.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-info" role="status"></div><div class="mt-2 text-muted fw-bold"><%=Common.getBahasaConfig("Menggabungkan seluruh portofolio mahasiswa...")%></div></div>';
        
        var url = serviceGabunganUrlPorto<%=rnd%> + "&mahasiswa=" + idMhs;
        if(typeof loadContentIntoContainer === 'function') {
            loadContentIntoContainer(url, 'containerHasilPortofolio<%=rnd%>');
        }
    };

    <% if (isMahasiswa) { %>
        setTimeout(function() { window.setMahasiswaPorto<%=rnd%>('<%=initialMahasiswaId%>'); }, 500);
    <% } %>
</script>