<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
String rnd = request.getParameter("rnd");
Tbmuser tbmuser = Common.getCurrentUser(request);
%>

<div id="containerPengajuan<%=rnd%>" class="animate__animated animate__fadeIn">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4 bg-light p-3 rounded-4 border">
        <button class="btn btn-primary shadow-sm fw-bold rounded-pill px-4" onclick="bukaFormSuratDashboard('')">
            <i class="fas fa-plus-circle me-2"></i><%=Common.getBahasaConfig("Pengajuan Surat Baru")%>
        </button>

        <div class="d-flex align-items-center gap-2 flex-wrap">
            <div class="input-group input-group-sm shadow-sm" style="width: 280px; border-radius: 20px; overflow: hidden;">
                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                <input type="text" class="form-control border-start-0 ps-0 bg-white" id="cariPengajuan<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari perihal, kode, atau agenda...")%>">
            </div>
            
            <div class="input-group input-group-sm shadow-sm w-auto" style="border-radius: 20px; overflow: hidden;">
                <span class="input-group-text bg-white border-end-0"><i class="fas fa-calendar-alt text-muted"></i></span>
                <input type="date" class="form-control border-start-0 border-end-0" id="tglMulaiPengajuan<%=rnd%>" title="<%=Common.getBahasaConfig("Tanggal Mulai")%>">
                <span class="input-group-text bg-light border-start-0 border-end-0 fw-bold">-</span>
                <input type="date" class="form-control border-start-0" id="tglSampaiPengajuan<%=rnd%>" title="<%=Common.getBahasaConfig("Tanggal Akhir")%>">
            </div>
            
            <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetLoadPengajuan<%=rnd%>()" title="<%=Common.getBahasaConfig("Segarkan Data")%>">
                <i class="fas fa-sync-alt"></i>
            </button>
        </div>
    </div>

    <div class="table-responsive border rounded-3 shadow-sm bg-white">
        <table class="table table-hover table-striped align-middle mb-0">
            <thead class="table-dark text-center align-middle">
                <tr>
                    <th class="fw-semibold small px-3 py-3" style="width: 50px;">#</th>
                    <th class="text-start fw-semibold small px-3"><i class="fas fa-tags me-1"></i><%=Common.getBahasaConfig("Klasifikasi & Kode")%></th>
                    <th class="text-start fw-semibold small px-3"><i class="fas fa-envelope-open-text me-1"></i><%=Common.getBahasaConfig("Perihal & Agenda")%></th>
                    <th class="text-center fw-semibold small px-3" style="width: 150px;"><i class="fas fa-cogs"></i></th>
                </tr>
            </thead>
            <tbody id="tabelDataPengajuan<%=rnd%>">
                <tr><td colspan="4" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="fw-medium text-muted"><%=Common.getBahasaConfig("Sedang memuat data...")%></span></td></tr>
            </tbody>
        </table>
    </div>

    <div class="d-flex justify-content-between align-items-center mt-3">
        <small class="text-muted fw-bold bg-light px-3 py-1 rounded-pill border" id="infoPagingPengajuan<%=rnd%>"></small>
        <div class="btn-group shadow-sm">
            <button class="btn btn-sm btn-light border fw-bold text-primary" id="btnPrevPengajuan<%=rnd%>" onclick="gantiHalamanPengajuan<%=rnd%>(-1)"><i class="fas fa-chevron-left"></i></button>
            <button class="btn btn-sm btn-light border fw-bold text-dark disabled" id="lblHalamanPengajuan<%=rnd%>">1</button>
            <button class="btn btn-sm btn-light border fw-bold text-primary" id="btnNextPengajuan<%=rnd%>" onclick="gantiHalamanPengajuan<%=rnd%>(1)"><i class="fas fa-chevron-right"></i></button>
        </div>
    </div>
</div>

<script>
    const injectModalPengajuan<%=rnd%> = () => {
        const modalId = 'modalAksiPengajuan<%=rnd%>';
        if(!document.getElementById(modalId)) {
            const modalHtml = `
            <div class="modal fade" id="\${modalId}" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog modal-lg modal-dialog-centered">
                    <div class="modal-content border-0 shadow-lg rounded-4">
                        <div class="modal-header bg-light border-bottom-0 rounded-top-4">
                            <h5 class="modal-title fw-bold"><i class="fas fa-file-signature text-primary me-2"></i><%=Common.getBahasaConfig("Detail Pengajuan Surat")%></h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<%=Common.getBahasaConfig("Tutup")%>"></button>
                        </div>
                        <div class="modal-body p-4" id="bodyModalAksiPengajuan<%=rnd%>">
                            <p class="text-center text-muted"><%=Common.getBahasaConfig("Sedang menyiapkan formulir...")%></p>
                        </div>
                    </div>
                </div>
            </div>`;
            document.body.insertAdjacentHTML('beforeend', modalHtml);
        }
    };
    injectModalPengajuan<%=rnd%>();

    const classSuratKeluar<%=rnd%> = "ais.database.model.surat.SuratKeluar";
    let halamanPengajuan<%=rnd%> = 1;
    const batasBarisPengajuan<%=rnd%> = 10;
    let totalDataPengajuan<%=rnd%> = 0;

    // Set Filter Tanggal (1 Tahun Terakhir)
    const hariIniPengajuan = new Date();
    const tahunLaluPengajuan = new Date(hariIniPengajuan.getFullYear() - 1, hariIniPengajuan.getMonth(), hariIniPengajuan.getDate());
    document.getElementById('tglSampaiPengajuan<%=rnd%>').valueAsDate = hariIniPengajuan;
    document.getElementById('tglMulaiPengajuan<%=rnd%>').valueAsDate = tahunLaluPengajuan;
    
    document.getElementById('cariPengajuan<%=rnd%>').addEventListener('keydown', (e) => {
        if(e.key === 'Enter') resetLoadPengajuan<%=rnd%>();
    });

    const susunFilterPengajuan<%=rnd%> = () => {
        const kataKunci = document.getElementById('cariPengajuan<%=rnd%>').value.trim().replace(/'/g, "''");
        const tglMulai = document.getElementById('tglMulaiPengajuan<%=rnd%>').value;
        const tglSampai = document.getElementById('tglSampaiPengajuan<%=rnd%>').value;
        
        let kriteria = [];
        if (tglMulai) kriteria.push("this_.tanggal >= '" + tglMulai + "'");
        if (tglSampai) kriteria.push("this_.tanggal <= '" + tglSampai + "'");

        <% if (tbmuser.getMahasiswa() != null) { %>
            kriteria.push("this_.mahasiswa = <%=tbmuser.getMahasiswa().getId()%>");
        <% } else if (tbmuser.getSiswa() != null) { %>
            kriteria.push("this_.siswa = <%=tbmuser.getSiswa().getId()%>");
        <% } else if (tbmuser.hakAkses() != null && !tbmuser.hakAkses().getMelihatSemuaSurat()) { %>
            kriteria.push("this_.konseptor = <%=tbmuser.getId()%>");
        <% } %>

        if (kataKunci !== '') {
            kriteria.push("(this_.perihal ILIKE '%" + kataKunci + "%' OR this_.agenda ILIKE '%" + kataKunci + "%' OR this_.keterangan ILIKE '%" + kataKunci + "%' OR this_.kode ILIKE '%" + kataKunci + "%')");
        }
        return kriteria.length > 0 ? kriteria.join(' AND ') : '';
    };

    const resetLoadPengajuan<%=rnd%> = () => {
        halamanPengajuan<%=rnd%> = 1;
        loadDataPengajuan<%=rnd%>();
    };

    const loadDataPengajuan<%=rnd%> = async () => {
        const areaTabel = document.getElementById('tabelDataPengajuan<%=rnd%>');
        areaTabel.innerHTML = '<tr><td colspan="4" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="fw-medium text-muted"><%=Common.getBahasaConfig("Mengambil data pengajuan...")%></span></td></tr>';
        
        const indeksAwal = (halamanPengajuan<%=rnd%> - 1);
        const kriteriaPencarian = susunFilterPengajuan<%=rnd%>();

        let objekPermintaan = {
            "action": "daftar", 
            "class": classSuratKeluar<%=rnd%>,
            "deep": "1", 
            "max": batasBarisPengajuan<%=rnd%>, 
            "halaman": indeksAwal,
            "order1": "desc", 
            "sort1": "id", 
            "count": "true"
        };
        if (kriteriaPencarian) objekPermintaan["where1"] = kriteriaPencarian;

        try {
            const respons = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(objekPermintaan)
            });
            const hasilData = await respons.json();
            
            totalDataPengajuan<%=rnd%> = parseInt(hasilData.count || 0);
            const daftarRekam = hasilData.data || [];
            
            let htmlBaris = '';
            if (daftarRekam.length === 0) {
                htmlBaris = '<tr><td colspan="4" class="text-center text-muted py-5"><i class="fas fa-folder-open fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Belum ada data pengajuan surat yang ditemukan.")%></td></tr>';
            } else {
                let nomorUrut = (indeksAwal * batasBarisPengajuan<%=rnd%>) + 1;
                daftarRekam.forEach((baris) => {
                    const namaKlasifikasi = baris["klasifikasiSuratKeluar.nama"] || baris.nama || '-';
                    const kodeSurat = baris.kode || '-';
                    const perihalSurat = baris.perihal || '-';
                    const agendaSurat = baris.agenda || '-';
                    const waktuFormat = baris["waktu.formated2"] || baris["waktu.datetime_format"] || '-';
                    
                    const tombolAksi = '<button class="btn btn-sm btn-outline-primary shadow-sm px-3 fw-bold rounded-pill" onclick="bukaFormSuratDashboard(' + baris.id + ')" title="<%=Common.getBahasaConfig("Ubah / Lihat Data")%>"><i class="fas fa-edit me-1"></i><%=Common.getBahasaConfig("Kelola")%></button>';
                    
                    htmlBaris += '<tr>' +
                            '<td class="text-center text-muted fw-bold">' + (nomorUrut++) + '</td>' +
                            '<td class="text-start"><div class="fw-bold text-dark mb-1">' + namaKlasifikasi + '</div><span class="badge bg-light text-dark border"><i class="fas fa-barcode me-1"></i>' + kodeSurat + '</span></td>' +
                            '<td class="text-start"><div class="fw-bold text-primary mb-1">' + perihalSurat + '</div><small class="text-muted"><i class="far fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Agenda")%>: ' + agendaSurat + ' <br> (<%=Common.getBahasaConfig("Pengajuan")%>: ' + waktuFormat + ')</small></td>' +
                            '<td class="text-center align-middle text-nowrap">' + tombolAksi + '</td>' +
                        '</tr>';
                });
            }
            areaTabel.innerHTML = htmlBaris;
            perbaruiPaginasiPengajuan<%=rnd%>();
        } catch (galat) {
            console.error("Galat Penarikan Data:", galat);
            areaTabel.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data dari peladen.")%></td></tr>';
        }
    };

    const perbaruiPaginasiPengajuan<%=rnd%> = () => {
        const totalHalaman = Math.ceil(totalDataPengajuan<%=rnd%> / batasBarisPengajuan<%=rnd%>) || 1;
        document.getElementById('lblHalamanPengajuan<%=rnd%>').innerText = halamanPengajuan<%=rnd%> + " / " + totalHalaman;
        document.getElementById('infoPagingPengajuan<%=rnd%>').innerText = "<%=Common.getBahasaConfig("Total Data")%>: " + totalDataPengajuan<%=rnd%>;
        document.getElementById('btnPrevPengajuan<%=rnd%>').disabled = (halamanPengajuan<%=rnd%> === 1);
        document.getElementById('btnNextPengajuan<%=rnd%>').disabled = (halamanPengajuan<%=rnd%> === totalHalaman);
    };

    const gantiHalamanPengajuan<%=rnd%> = (arah) => {
        const totalHalaman = Math.ceil(totalDataPengajuan<%=rnd%> / batasBarisPengajuan<%=rnd%>);
        if (arah === -1 && halamanPengajuan<%=rnd%> > 1) {
            halamanPengajuan<%=rnd%>--;
            loadDataPengajuan<%=rnd%>();
        } else if (arah === 1 && halamanPengajuan<%=rnd%> < totalHalaman) {
            halamanPengajuan<%=rnd%>++;
            loadDataPengajuan<%=rnd%>();
        }
    };

    const bukaFormPengajuan<%=rnd%> = () => {
        const myModal = new bootstrap.Modal(document.getElementById('modalAksiPengajuan<%=rnd%>'));
        document.getElementById('bodyModalAksiPengajuan<%=rnd%>').innerHTML = '<div class="alert alert-info"><i class="fas fa-info-circle me-2"></i><%=Common.getBahasaConfig("Fitur form penambahan surat sedang dalam penyesuaian antar-muka.")%></div>';
        myModal.show();
    };

    const lihatDetailPengajuan<%=rnd%> = (id) => {
        const myModal = new bootstrap.Modal(document.getElementById('modalAksiPengajuan<%=rnd%>'));
        document.getElementById('bodyModalAksiPengajuan<%=rnd%>').innerHTML = '<div class="alert alert-warning"><i class="fas fa-tools me-2"></i><%=Common.getBahasaConfig("Memuat detail spesifik untuk ID")%>: ' + id + '</div>';
        myModal.show();
    };

    // Panggil saat script dieksekusi pertama kali
    loadDataPengajuan<%=rnd%>();
</script>