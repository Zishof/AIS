<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir.") + "\"}");
    return;
}
String rnd = Common.getGeneratedBarCode(7);
%>

<div class="container-fluid animate__animated animate__fadeIn" id="wadahDaftarSuratKeluar<%=rnd%>">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h4 class="fw-bold text-dark mb-0">
            <i class="fas fa-paper-plane text-primary me-2"></i><%=Common.getBahasaConfig("Daftar Surat Keluar")%>
        </h4>
        <small class="text-muted"><%=Common.getBahasaConfig("Kelola semua data pengajuan dan draf surat keluar di sini.")%></small>
    </div>

    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4 bg-light p-3 rounded-4 border">
        <button class="btn btn-primary shadow-sm fw-bold rounded-pill px-4" onclick="bukaModalFormSuratKeluar<%=rnd%>('')">
            <i class="fas fa-plus-circle me-2"></i><%=Common.getBahasaConfig("Tambah Surat Keluar")%>
        </button>

        <div class="d-flex align-items-center gap-2 flex-wrap">
            <div class="input-group input-group-sm shadow-sm" style="width: 280px; border-radius: 20px; overflow: hidden;">
                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                <input type="text" class="form-control border-start-0 ps-0 bg-white" id="cariSuratKeluar<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari perihal, nomor, atau agenda...")%>">
            </div>
            
            <div class="input-group input-group-sm shadow-sm w-auto" style="border-radius: 20px; overflow: hidden;">
                <span class="input-group-text bg-white border-end-0"><i class="fas fa-calendar-alt text-muted"></i></span>
                <input type="date" class="form-control border-start-0 border-end-0" id="tglMulaiSuratKeluar<%=rnd%>">
                <span class="input-group-text bg-light border-start-0 border-end-0 fw-bold">-</span>
                <input type="date" class="form-control border-start-0" id="tglSampaiSuratKeluar<%=rnd%>">
            </div>
            
            <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetLoadSuratKeluar<%=rnd%>()">
                <i class="fas fa-sync-alt"></i>
            </button>
        </div>
    </div>

    <div class="table-responsive border rounded-3 shadow-sm bg-white">
        <table class="table table-hover table-striped align-middle mb-0">
            <thead class="table-dark text-center align-middle">
                <tr>
                    <th class="fw-semibold small px-3 py-3" style="width: 60px;">#</th>
                    <th class="text-start fw-semibold small px-3"><i class="fas fa-barcode me-1"></i><%=Common.getBahasaConfig("Nomor & Tanggal")%></th>
                    <th class="text-start fw-semibold small px-3"><i class="fas fa-tags me-1"></i><%=Common.getBahasaConfig("Klasifikasi & Satuan Kerja")%></th>
                    <th class="text-start fw-semibold small px-3"><i class="fas fa-envelope-open-text me-1"></i><%=Common.getBahasaConfig("Perihal Surat")%></th>
                    <th class="text-center fw-semibold small px-3" style="width: 130px;"><i class="fas fa-cogs"></i></th>
                </tr>
            </thead>
            <tbody id="tabelDataSuratKeluar<%=rnd%>">
                <tr><td colspan="5" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br>Memuat data...</td></tr>
            </tbody>
        </table>
    </div>

    <div class="d-flex justify-content-between align-items-center mt-3">
        <small class="text-muted fw-bold bg-light px-3 py-1 rounded-pill border" id="infoPagingSuratKeluar<%=rnd%>"></small>
        <div class="btn-group shadow-sm">
            <button class="btn btn-sm btn-light border fw-bold text-primary" id="btnPrevSuratKeluar<%=rnd%>" onclick="gantiHalamanSuratKeluar<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button>
            <button class="btn btn-sm btn-light border fw-bold text-dark disabled" id="lblHalamanSuratKeluar<%=rnd%>">1</button>
            <button class="btn btn-sm btn-light border fw-bold text-primary" id="btnNextSuratKeluar<%=rnd%>" onclick="gantiHalamanSuratKeluar<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button>
        </div>
    </div>
</div>

<script>
    var injeksiModalSuratKeluar<%=rnd%> = () => {
        var idModal = 'modalFormSuratKeluar<%=rnd%>';
        if(!document.getElementById(idModal)) {
            var htmlModal = `
            <style>
                .modal-lebar-<%=rnd%> { max-width: 100%; width: 100%; margin: 0; }
                .modal-lebar-<%=rnd%> .modal-content { min-height: 100vh; border-radius: 0 !important; }
                @media (min-width: 992px) {
                    .modal-lebar-<%=rnd%> { max-width: 90%; width: 90%; margin: 1.75rem auto; }
                    .modal-lebar-<%=rnd%> .modal-content { min-height: auto; border-radius: 1rem !important; }
                }
            </style>
            <div class="modal fade" id="\${idModal}" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                <div class="modal-dialog modal-lebar-<%=rnd%> modal-dialog-centered modal-dialog-scrollable">
                    <div class="modal-content border-0 shadow-lg">
                        <div class="modal-header bg-primary text-white border-bottom-0">
                            <h5 class="modal-title fw-bold text-white"><i class="fas fa-pen-square me-2"></i><%=Common.getBahasaConfig("Formulir Surat Keluar")%></h5>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body p-0" id="bodiModalFormSuratKeluar<%=rnd%>"></div>
                    </div>
                </div>
            </div>`;
            document.body.insertAdjacentHTML('beforeend', htmlModal);
        }
    };
    injeksiModalSuratKeluar<%=rnd%>();

    var classModelSuratKeluar<%=rnd%> = "ais.database.model.surat.SuratKeluar";
    var halamanSuratKeluar<%=rnd%> = 1;
    var limitSuratKeluar<%=rnd%> = 10;
    var totalDataSuratKeluar<%=rnd%> = 0;

    document.addEventListener("DOMContentLoaded", () => {
        var hariIni = new Date();
        var tahunLalu = new Date(hariIni.getFullYear() - 1, hariIni.getMonth(), hariIni.getDate());
        document.getElementById('tglSampaiSuratKeluar<%=rnd%>').valueAsDate = hariIni;
        document.getElementById('tglMulaiSuratKeluar<%=rnd%>').valueAsDate = tahunLalu;
        
        document.getElementById('cariSuratKeluar<%=rnd%>').addEventListener('keydown', (e) => {
            if(e.key === 'Enter') resetLoadSuratKeluar<%=rnd%>();
        });
        loadDataSuratKeluar<%=rnd%>();
    });

    var susunFilterSuratKeluar<%=rnd%> = () => {
        var kataKunci = document.getElementById('cariSuratKeluar<%=rnd%>').value.trim().replace(/'/g, "''");
        var tglMulai = document.getElementById('tglMulaiSuratKeluar<%=rnd%>').value;
        var tglSampai = document.getElementById('tglSampaiSuratKeluar<%=rnd%>').value;
        
        var kriteria = [];
        if (tglMulai) kriteria.push("this_.tanggal >= '" + tglMulai + "'");
        if (tglSampai) kriteria.push("this_.tanggal <= '" + tglSampai + "'");

        <% if (tbmuser.getMahasiswa() != null) { %> kriteria.push("this_.mahasiswa = <%=tbmuser.getMahasiswa().getId()%>");
        <% } else if (tbmuser.getSiswa() != null) { %> kriteria.push("this_.siswa = <%=tbmuser.getSiswa().getId()%>");
        <% } else if (tbmuser.hakAkses() != null && !tbmuser.hakAkses().getMelihatSemuaSurat()) { %> kriteria.push("this_.konseptor = <%=tbmuser.getId()%>"); <% } %>

        if (kataKunci !== '') {
            kriteria.push("(this_.perihal ILIKE '%" + kataKunci + "%' OR this_.keterangan ILIKE '%" + kataKunci + "%' OR this_.kode ILIKE '%" + kataKunci + "%' OR this_.agenda ILIKE '%" + kataKunci + "%')");
        }
        return kriteria.length > 0 ? kriteria.join(' AND ') : '';
    };

    var resetLoadSuratKeluar<%=rnd%> = () => { halamanSuratKeluar<%=rnd%> = 1; loadDataSuratKeluar<%=rnd%>(); };

    var loadDataSuratKeluar<%=rnd%> = async () => {
        var bodiTabel = document.getElementById('tabelDataSuratKeluar<%=rnd%>');
        bodiTabel.innerHTML = '<tr><td colspan="5" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="fw-medium text-muted"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';
        
        var objekReq = { "action": "daftar", "class": classModelSuratKeluar<%=rnd%>, "deep": "1", "max": limitSuratKeluar<%=rnd%>, "halaman": (halamanSuratKeluar<%=rnd%> - 1), "order1": "desc", "sort1": "id", "count": "true" };
        var filter = susunFilterSuratKeluar<%=rnd%>();
        if (filter) objekReq["where1"] = filter;

        try {
            var respons = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(objekReq) });
            var hasil = await respons.json();
            totalDataSuratKeluar<%=rnd%> = parseInt(hasil.count || 0);
            var dataRekam = hasil.data || [];
            
            var barisHtml = '';
            if (dataRekam.length === 0) {
                barisHtml = '<tr><td colspan="5" class="text-center text-muted py-5"><i class="fas fa-folder-open fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Data tidak ditemukan.")%></td></tr>';
            } else {
                var nomorUrut = ((halamanSuratKeluar<%=rnd%> - 1) * limitSuratKeluar<%=rnd%>) + 1;
                dataRekam.forEach((baris) => {
                    var kode = baris.kode || '-';
                    var agenda = baris.agenda || '-';
                    var tanggalFormat = baris["tanggal.formated2"] || baris.tanggal || '-';
                    var klasifikasi = baris["klasifikasiSuratKeluar.nama"] || '-';
                    var satuanKerja = baris["satuanKerja.nama"] || '-';
                    var perihal = baris.perihal || '-';
                    var keterangan = baris.keterangan || '';
                    var htmlKeterangan = keterangan ? `<br><small class="text-muted"><i class="fas fa-info-circle me-1"></i>\${keterangan}</small>` : '';

                    var tombolAksi = `
                        <button class="btn btn-sm btn-outline-primary shadow-sm px-2 me-1 mb-1 fw-bold" onclick="bukaModalFormSuratKeluar<%=rnd%>('\${baris.id}')" title="<%=Common.getBahasaConfig("Ubah Data")%>"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-sm btn-outline-danger shadow-sm px-2 mb-1 fw-bold" onclick="hapusSuratKeluar<%=rnd%>('\${baris.id}')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>
                    `;
                    barisHtml += `<tr>
                            <td class="text-center text-muted fw-bold">\${nomorUrut++}</td>
                            <td class="text-start">
                                <div class="fw-bold text-dark mb-1"><i class="fas fa-hashtag me-1 text-muted"></i>\${kode}</div>
                                <small class="text-muted"><i class="far fa-calendar-alt me-1"></i>\${tanggalFormat} | <%=Common.getBahasaConfig("Agenda:")%> \${agenda}</small>
                            </td>
                            <td class="text-start">
                                <div class="fw-bold text-secondary mb-1">\${klasifikasi}</div>
                                <small class="text-muted"><i class="fas fa-building me-1"></i>\${satuanKerja}</small>
                            </td>
                            <td class="text-start"><div class="fw-bold text-primary mb-1">\${perihal}</div>\${htmlKeterangan}</td>
                            <td class="text-center align-middle">\${tombolAksi}</td>
                        </tr>`;
                });
            }
            bodiTabel.innerHTML = barisHtml;
            perbaruiPaginasiSuratKeluar<%=rnd%>();
        } catch (galat) {
            bodiTabel.innerHTML = '<tr><td colspan="5" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data.")%></td></tr>';
        }
    };

    var perbaruiPaginasiSuratKeluar<%=rnd%> = () => {
        var totalHalaman = Math.ceil(totalDataSuratKeluar<%=rnd%> / limitSuratKeluar<%=rnd%>) || 1;
        document.getElementById('lblHalamanSuratKeluar<%=rnd%>').innerText = halamanSuratKeluar<%=rnd%> + " / " + totalHalaman;
        document.getElementById('infoPagingSuratKeluar<%=rnd%>').innerText = "<%=Common.getBahasaConfig("Total Data")%>: " + totalDataSuratKeluar<%=rnd%>;
        document.getElementById('btnPrevSuratKeluar<%=rnd%>').disabled = (halamanSuratKeluar<%=rnd%> === 1);
        document.getElementById('btnNextSuratKeluar<%=rnd%>').disabled = (halamanSuratKeluar<%=rnd%> === totalHalaman);
    };

    var gantiHalamanSuratKeluar<%=rnd%> = (arah) => {
        var totalHalaman = Math.ceil(totalDataSuratKeluar<%=rnd%> / limitSuratKeluar<%=rnd%>);
        if (arah === -1 && halamanSuratKeluar<%=rnd%> > 1) { halamanSuratKeluar<%=rnd%>--; loadDataSuratKeluar<%=rnd%>(); } 
        else if (arah === 1 && halamanSuratKeluar<%=rnd%> < totalHalaman) { halamanSuratKeluar<%=rnd%>++; loadDataSuratKeluar<%=rnd%>(); }
    };

    var bukaModalFormSuratKeluar<%=rnd%> = async (idSurat) => {
        var modalEl = document.getElementById('modalFormSuratKeluar<%=rnd%>');
        var bodiModal = document.getElementById('bodiModalFormSuratKeluar<%=rnd%>');
        var myModal = new bootstrap.Modal(modalEl);
        
        bodiModal.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="fw-bold text-muted"><%=Common.getBahasaConfig("Sedang menyiapkan formulir...")%></span></div>';
        myModal.show();
        
        try {
            var urlForm = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pagesmastersuratsuratkeluarzul&s=edit&id=' + idSurat + '&rnd=<%=rnd%>';
            var respons = await fetch(urlForm);
            bodiModal.innerHTML = await respons.text();
            
            var daftarSkrip = bodiModal.querySelectorAll('script');
            daftarSkrip.forEach((skripLama) => {
                var skripBaru = document.createElement('script');
                Array.from(skripLama.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') skripBaru.setAttribute(attr.name, attr.value); });
                skripBaru.type = 'text/javascript';
                skripBaru.appendChild(document.createTextNode(skripLama.innerHTML));
                skripLama.parentNode.replaceChild(skripBaru, skripLama);
            });
        } catch (galat) {
            bodiModal.innerHTML = '<div class="alert alert-danger m-4 text-center"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Gagal memuat formulir.")%></div>';
        }
    };

    var hapusSuratKeluar<%=rnd%> = async (id) => {
        if (!confirm("<%=Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini?")%>")) return;
        try {
            var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "delete", class: classModelSuratKeluar<%=rnd%>, id: id }) });
            var result = await res.json();
            if (result.status === "success" || result.data === "success") {
                if(typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Data berhasil dihapus.")%>', 'bg-success text-white');
                loadDataSuratKeluar<%=rnd%>();
            } else {
                if(typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menghapus data.")%>', 'bg-danger text-white');
            }
        } catch (e) { console.error(e); }
    };

    window["berhasilSimpanSuratKeluar<%=rnd%>"] = () => {
        var modalInstance = bootstrap.Modal.getInstance(document.getElementById('modalFormSuratKeluar<%=rnd%>'));
        if (modalInstance) modalInstance.hide();
        if(typeof tampilkanToast === "function") tampilkanToast('<%=Common.getBahasaConfigJS("Data berhasil disimpan.")%>', 'bg-success text-white');
        loadDataSuratKeluar<%=rnd%>();
    };
</script>