<%@page import="ais.common.Common"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="java.util.Calendar"%>
<%@page isELIgnored="true"%>

<%
String rnd = request.getParameter("rnd");
%>

<div class="row g-3">
    <!-- BARIS 1: IDENTITAS UTAMA -->
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("NIM")%> <span class="text-danger">*</span></label>
        <input type="text" class="form-control bg-light shadow-sm fw-bold text-primary" id="inputNim<%=rnd%>">
    </div>
    <div class="col-md-8">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Lengkap")%> <span class="text-danger">*</span></label>
        <input type="text" class="form-control bg-light shadow-sm fw-bold" id="inputNama<%=rnd%>">
    </div>

    <!-- BARIS 2: NAMA INTERNASIONAL -->
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Arab")%></label>
        <input type="text" class="form-control bg-light shadow-sm" id="inputNamaArab<%=rnd%>">
    </div>
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Tionghoa")%></label>
        <input type="text" class="form-control bg-light shadow-sm" id="inputNamaTionghoa<%=rnd%>">
    </div>

    <!-- BARIS 3: STRUKTUR FAKULTAS & PRODI -->
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Fakultas")%></label>
        <select class="form-select bg-light shadow-sm" id="selectFakultas<%=rnd%>" onchange="loadJurusan<%=rnd%>(this.value)"></select>
    </div>
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jurusan / Program Studi")%> <span class="text-danger">*</span></label>
        <select class="form-select bg-light shadow-sm" id="selectJurusan<%=rnd%>"></select>
    </div>

    <!-- BARIS 4: PERIODE MASUK -->
    <div class="col-md-3">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Angkatan")%></label>
        <input type="number" class="form-control bg-light shadow-sm" id="inputAngkatan<%=rnd%>" value="<%=Calendar.getInstance().get(Calendar.YEAR)%>">
    </div>
    <div class="col-md-3">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Masuk")%></label>
        <input type="date" class="form-control bg-light shadow-sm" id="inputTglMasuk<%=rnd%>">
    </div>
    <div class="col-md-3">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Semester Mulai")%></label>
        <select class="form-select bg-light shadow-sm" id="inputSmtMulai<%=rnd%>">
            <option value="Ganjil">Ganjil</option>
            <option value="Genap">Genap</option>
        </select>
    </div>
    <div class="col-md-3">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Mulai KBM")%></label>
        <input type="date" class="form-control bg-light shadow-sm" id="inputTglMulaiKBM<%=rnd%>">
    </div>

    <!-- BARIS 5: RELASI KELAS & DOSEN (DATA PICKER) -->
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kelas")%></label>
        <div class="input-group shadow-sm">
            <input type="hidden" id="inputKelasId<%=rnd%>">
            <input type="text" class="form-control bg-light fw-bold" id="inputKelasNama<%=rnd%>" readonly placeholder="Pilih Kelas...">
            <button class="btn btn-primary" type="button" onclick="bukaPickerKelas<%=rnd%>()">
                <i class="fas fa-search"></i>
            </button>
        </div>
        <div class="form-check mt-1">
            <input class="form-check-input" type="checkbox" id="checkKelasSelaluSama<%=rnd%>">
            <label class="form-check-label small text-muted" for="checkKelasSelaluSama<%=rnd%>"><%=Common.getBahasaConfig("Selalu sama di tiap semester")%></label>
        </div>
    </div>
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Dosen Pembimbing Akademik (PA)")%></label>
        <div class="input-group shadow-sm">
            <input type="hidden" id="inputDosenPaId<%=rnd%>">
            <input type="text" class="form-control bg-light fw-bold" id="inputDosenPaNama<%=rnd%>" readonly placeholder="Pilih Dosen...">
            <button class="btn btn-primary" type="button" onclick="bukaPickerDosenPa<%=rnd%>()">
                <i class="fas fa-search"></i>
            </button>
        </div>
        <div class="form-check mt-1">
            <input class="form-check-input" type="checkbox" id="checkDosenPaSelaluSama<%=rnd%>">
            <label class="form-check-label small text-muted" for="checkDosenPaSelaluSama<%=rnd%>"><%=Common.getBahasaConfig("Selalu sama di tiap semester")%></label>
        </div>
    </div>

    <!-- BARIS 6: KATEGORI MAHASISWA -->
    <div class="col-md-3">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Program")%></label>
        <select class="form-select bg-light shadow-sm" id="selectProgram<%=rnd%>"></select>
    </div>
    <div class="col-md-3">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Status Awal")%></label>
        <select class="form-select bg-light shadow-sm" id="selectStatusAwal<%=rnd%>"></select>
    </div>
    <div class="col-md-3">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jalur Pendaftaran")%></label>
        <select class="form-select bg-light shadow-sm" id="selectJenisSeleksi<%=rnd%>"></select>
    </div>
    <div class="col-md-3">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jenis Pembiayaan")%></label>
        <select class="form-select bg-light shadow-sm" id="selectJenisPembiayaan<%=rnd%>"></select>
    </div>

    <!-- BARIS 7: DATA SISTEM & INTEGRASI -->
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Username OJS")%></label>
        <input type="text" class="form-control bg-light shadow-sm" id="inputOjs<%=rnd%>">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("ID Fingerprint")%></label>
        <input type="text" class="form-control bg-light shadow-sm" id="inputFinger<%=rnd%>">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Lock ID")%></label>
        <input type="text" class="form-control bg-light shadow-sm" id="inputLockId<%=rnd%>">
    </div>

    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("ID Feeder")%></label>
        <input type="text" class="form-control bg-light shadow-sm" id="inputFeeder<%=rnd%>">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("ID Reg PD (Neo Feeder)")%></label>
        <input type="text" class="form-control bg-light shadow-sm" id="inputIdRegPd<%=rnd%>">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Link Validasi Eksternal")%></label>
        <input type="text" class="form-control bg-light shadow-sm text-primary" id="inputLinkValidasi<%=rnd%>">
    </div>

    <!-- TOMBOL SIMPAN KHUSUS TAB AKADEMIK -->
    <div class="col-12 text-end mt-4 pt-3 border-top">
        <button type="button" class="btn btn-primary px-5 fw-bold shadow-sm rounded-pill" id="btnSimpanAkademik<%=rnd%>" onclick="simpanDataAkademik<%=rnd%>()">
            <i class="fas fa-save me-2"></i>Simpan Data Akademik
        </button>
    </div>
</div>

<script>
    // --- DATA PICKER LOGIC (AKADEMIK) ---
    
    // 1. Picker Kelas (Hanya Aktif)
    const bukaPickerKelas<%=rnd%> = () => {
        const url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=common&s=_data_picker' +
                    '&class=ais.database.model.Kelas' + 
                    '&cols=' + encodeURIComponent('id,nama') + 
                    '&where=' + encodeURIComponent('aktif = true') + 
                    '&order=nama+asc&radio=true&rnd=<%=rnd%>&callback=onKelasTerpilih<%=rnd%>';
        bukaModalPicker<%=rnd%>('modalPickerKelas_<%=rnd%>', 'Pilih Kelas', url);
    };
    
    const onKelasTerpilih<%=rnd%> = (data) => {
        if(data && data.length > 0) {
            document.getElementById('inputKelasId<%=rnd%>').value = data[0].id;
            document.getElementById('inputKelasNama<%=rnd%>').value = data[0].nama;
        }
        tutupModalPicker<%=rnd%>('modalPickerKelas_<%=rnd%>');
    };

    // 2. Picker Dosen PA (Hanya Aktif)
    const bukaPickerDosenPa<%=rnd%> = () => {
        const url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=common&s=_data_picker' +
                    '&class=ais.database.model.Dosen' + 
                    '&cols=' + encodeURIComponent('id,nama') + 
                    '&where=' + encodeURIComponent('aktif = true') + 
                    '&order=nama+asc&radio=true&rnd=<%=rnd%>&callback=onDosenPaTerpilih<%=rnd%>';
        bukaModalPicker<%=rnd%>('modalPickerDosen_<%=rnd%>', 'Pilih Dosen PA', url);
    };
    
    const onDosenPaTerpilih<%=rnd%> = (data) => {
        if(data && data.length > 0) {
            document.getElementById('inputDosenPaId<%=rnd%>').value = data[0].id;
            document.getElementById('inputDosenPaNama<%=rnd%>').value = data[0].nama;
        }
        tutupModalPicker<%=rnd%>('modalPickerDosen_<%=rnd%>');
    };

    // --- MODAL PICKER HELPER ---
    const bukaModalPicker<%=rnd%> = (modalId, title, url) => {
        let modalEl = document.getElementById(modalId);
        if (!modalEl) {
            modalEl = document.createElement('div');
            modalEl.id = modalId;
            modalEl.className = 'modal fade';
            document.body.appendChild(modalEl);
        }
        
        modalEl.innerHTML = 
            '<div class="modal-dialog modal-lg modal-dialog-centered">' +
                '<div class="modal-content shadow-lg border-0 rounded-4">' +
                    '<div class="modal-header bg-primary text-white pt-4 px-4 border-bottom-0">' +
                        '<h5 class="modal-title fw-bold"><i class="fas fa-search me-2"></i>' + title + '</h5>' +
                        '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>' +
                    '</div>' +
                    '<div class="modal-body p-0" id="body_' + modalId + '">' +
                        '<div class="d-flex justify-content-center p-5">' +
                            '<div class="text-center">' +
                                '<div class="spinner-border text-primary" role="status"></div>' +
                                '<div class="mt-2 text-muted fw-bold">Memuat Data...</div>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
        
        new bootstrap.Modal(modalEl).show();
        
        fetch(url).then(res => res.text()).then(html => {
            const targetBody = document.getElementById('body_' + modalId);
            if (!targetBody) return;
            targetBody.innerHTML = html;
            targetBody.querySelectorAll("script").forEach(script => {
                try {
                    if (script.src) {
                        const newScript = document.createElement("script");
                        newScript.src = script.src;
                        document.body.appendChild(newScript);
                    } else window.eval(script.innerHTML);
                } catch (e) { console.error("Gagal eksekusi script:", e); }
            });
        }).catch(err => {
            const tb = document.getElementById('body_' + modalId);
            if(tb) tb.innerHTML = '<div class="alert alert-danger m-4 text-center">Gagal memuat data dari server.</div>';
        });
    };

    const tutupModalPicker<%=rnd%> = (modalId) => {
        const modalEl = document.getElementById(modalId);
        if (modalEl) {
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();
        }
    };
</script>