<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.DynamicReport"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// 1. PEMERIKSAAN KEAMANAN & SESI
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
	+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
	return;
}
String rnd = Common.getGeneratedBarCode(7);
%>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                
                <div class="card-header bg-white border-0 pt-4 pb-3 px-4 border-bottom border-light">
                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-2">
                        <div>
                            <h5 class="fw-bold text-dark mb-1">
                                <i class="fas fa-chart-pie text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Dynamic Report")%>
                            </h5>
                            <small class="text-muted"><%=Common.getBahasaConfig("Kelola laporan dinamis, parameter, kondisi visibilitas, dan file JRXML.")%></small>
                        </div>
                        
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                                <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium" id="searchReport<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Kode, Nama, Keterangan...")%>">
                            </div>
                            
                            <button class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                                <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
                            </button>

                            <button class="btn btn-sm btn-outline-success rounded-pill px-3 shadow-sm fw-bold" onclick="downloadExcelReport<%=rnd%>()" title="<%=Common.getBahasaConfig("Unduh Data Excel")%>">
                                <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh Excel")%>
                            </button>

                            <input type="file" id="inputUploadExcel<%=rnd%>" class="d-none" accept=".xls,.xlsx" onchange="uploadExcelReport<%=rnd%>()">
                            <button class="btn btn-sm btn-outline-warning rounded-pill px-3 shadow-sm fw-bold" onclick="document.getElementById('inputUploadExcel<%=rnd%>').click();" title="<%=Common.getBahasaConfig("Unggah Data Excel")%>">
                                <i class="fas fa-upload me-1"></i><%=Common.getBahasaConfig("Unggah Excel")%>
                            </button>
                            
                            <button class="btn btn-sm btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                                <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Laporan")%>
                            </button>
                        </div>
                    </div>
                </div>
                
                <div class="card-body p-4 pt-3">
                    <div class="table-responsive border rounded-3 shadow-sm bg-white">
                        <table class="table table-hover table-striped align-middle mb-0" id="tabelHTMLDataReport<%=rnd%>">
                            <thead class="table-dark text-center align-middle">
                                <tr>
                                    <th class="fw-semibold text-uppercase small px-3 py-3" style="width: 60px;">#</th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-barcode me-1"></i><%=Common.getBahasaConfig("Kode Laporan")%></th>
                                    <th class="text-start fw-semibold text-uppercase small px-3"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Keterangan Laporan")%></th>
                                    <th class="text-center fw-semibold text-uppercase small px-3"><i class="fas fa-toggle-on me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                    <th class="fw-semibold text-uppercase small px-3 text-center no-export" style="width: 120px;"><i class="fas fa-cogs"></i></th>
                                </tr>
                            </thead>
                            <tbody id="tabelDataReport<%=rnd%>">
                                <tr><td colspan="5" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Memuat data laporan...")%></span></td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mt-4">
                        <div class="small fw-medium text-muted bg-light px-3 py-1 rounded-pill border" id="pagingInfoReport<%=rnd%>"></div>
                        <nav>
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item"><button class="page-link text-primary rounded-start-pill px-3 fw-bold" id="btnPrevPage<%=rnd%>" onclick="changePageReport<%=rnd%>(-1)"><i class="fas fa-chevron-left me-1"></i></button></li>
                                <li class="page-item disabled"><span class="page-link bg-light text-dark px-3 fw-bold" id="pageNumberDisplay<%=rnd%>">1</span></li>
                                <li class="page-item"><button class="page-link text-primary rounded-end-pill px-3 fw-bold" id="btnNextPage<%=rnd%>" onclick="changePageReport<%=rnd%>(1)"><i class="fas fa-chevron-right ms-1"></i></button></li>
                            </ul>
                        </nav>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div id="viewForm<%=rnd%>" class="animate__animated animate__fadeIn" style="display: none;">
    <div class="row justify-content-center">
        <div class="col-lg-12"> 
            <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
                <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                    <div class="d-flex justify-content-between align-items-center">
                        <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                            <i class="fas fa-edit text-success me-2"></i><%=Common.getBahasaConfig("Formulir Dynamic Report")%>
                        </h5>
                    </div>
                </div>
                <div class="card-body p-4">
                    <form id="formReport<%=rnd%>" onsubmit="event.preventDefault(); simpanReport<%=rnd%>();">
                        <input type="hidden" id="inputIdReport<%=rnd%>" value="">
                        
                        <div class="row g-4 mt-1">
                            
                            <div class="col-md-12 mb-3 mt-2 text-center border-bottom pb-4" id="uploadFileArea<%=rnd%>" style="display: none;">
                                <div class="p-4 bg-light rounded-3 border border-2 border-danger shadow-sm border-dashed">
                                    <label class="form-label fw-bold text-danger d-block fs-5"><i class="fas fa-file-code me-2"></i><%=Common.getBahasaConfig("Template Laporan (.jrxml)")%></label>
                                    
                                    <div id="fileInfoArea<%=rnd%>" class="mb-3 text-primary fw-bold" style="display: none;">
                                        <i class="fas fa-check-circle text-success me-1"></i> <%=Common.getBahasaConfig("File JRXML telah tersedia.")%><br>
                                        <a href="#" id="linkDownloadJrxml<%=rnd%>" target="_blank" class="btn btn-sm btn-outline-info mt-2 rounded-pill"><i class="fas fa-download me-1"></i> <%=Common.getBahasaConfig("Unduh File Saat Ini")%></a>
                                    </div>
                                    
                                    <div class="d-flex justify-content-center align-items-center gap-2 mt-3">
                                        <input type="file" id="input_dofile_jrxml_<%=rnd%>" class="d-none" accept=".jrxml" onchange="uploadFileJrxml<%=rnd%>()">
                                        <input type="hidden" id="fileNameDisplay<%=rnd%>" value="">
                                        
                                        <button type="button" class="btn btn-danger fw-bold rounded-pill shadow-sm px-4" onclick="document.getElementById('input_dofile_jrxml_<%=rnd%>').click();">
                                            <i class="fas fa-upload me-2"></i><%=Common.getBahasaConfig("Unggah / Ganti File .jrxml")%>
                                        </button>
                                    </div>

                                    <div id="progress-wrapper-<%=rnd%>" class="mt-3" style="display: none; max-width: 400px; margin: 0 auto;">
                                        <div class="progress" style="height: 15px;">
                                            <div id="progress-bar-<%=rnd%>" class="progress-bar bg-danger progress-bar-striped progress-bar-animated fw-bold" role="progressbar" style="width: 0%; font-size: 10px;">0%</div>
                                        </div>
                                    </div>
                                    
                                    <small class="text-muted mt-2 d-block fst-italic"><%=Common.getBahasaConfig("Pastikan file yang diunggah berekstensi .jrxml")%></small>
                                </div>
                            </div>

                            <div class="col-md-12">
                                <h6 class="fw-bold text-primary border-bottom pb-2 mb-0"><i class="fas fa-info-circle me-2"></i>1. <%=Common.getBahasaConfig("Data Utama Laporan")%></h6>
                            </div>
                            
                            <div class="col-md-8">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kode / Nama Laporan")%> <span class="text-danger">*</span></label>
                                <input type="text" class="form-control fw-bold shadow-sm" id="inputKode<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: LAP_MAHASISWA_AKTIF")%>" required>
                            </div>

                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Status Aktif")%> <span class="text-danger">*</span></label>
                                <select class="form-select shadow-sm" id="inputAktif<%=rnd%>" required>
                                    <option value="true"><%=Common.getBahasaConfig("Aktif")%></option>
                                    <option value="false"><%=Common.getBahasaConfig("Tidak Aktif")%></option>
                                </select>
                            </div>

                            <div class="col-md-12">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan Laporan")%></label>
                                <textarea class="form-control shadow-sm bg-light" id="inputKeterangan<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Deskripsi kegunaan laporan ini...")%>"></textarea>
                            </div>

                            <div class="col-md-12 mt-4">
                                <div class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-3">
                                    <h6 class="fw-bold text-primary mb-0"><i class="fas fa-sliders-h me-2"></i>2. <%=Common.getBahasaConfig("Parameter Dinamis Laporan")%></h6>
                                    <button type="button" class="btn btn-sm btn-outline-primary rounded-pill fw-bold px-3 shadow-sm" onclick="addParam<%=rnd%>()">
                                        <i class="fas fa-plus me-1"></i> <%=Common.getBahasaConfig("Tambah Parameter")%>
                                    </button>
                                </div>
                                
                                <div class="table-responsive border rounded-3 shadow-sm">
                                    <table class="table table-hover align-middle mb-0">
                                        <thead class="table-light text-center small">
                                            <tr>
                                                <th style="width: 15%;"><%=Common.getBahasaConfig("Nama Parameter")%></th>
                                                <th style="width: 12%;"><%=Common.getBahasaConfig("Tipe")%></th>
                                                <th style="width: 15%;"><%=Common.getBahasaConfig("Default Value")%></th>
                                                <th style="width: 25%;"><%=Common.getBahasaConfig("SQL Pilihan (Combobox)")%></th>
                                                <th style="width: 25%;"><%=Common.getBahasaConfig("Tampil Jika (Kondisi JS)")%></th>
                                                <th style="width: 8%;"><i class="fas fa-cogs"></i></th>
                                            </tr>
                                        </thead>
                                        <tbody id="tbodyParams<%=rnd%>">
                                            </tbody>
                                    </table>
                                </div>
                                <small class="text-muted mt-2 d-block"><%=Common.getBahasaConfig("Data parameter ini akan disimpan sebagai format JSON. Untuk kolom 'Tampil Jika', gunakan evaluasi properti, misal: toko_id == null")%></small>
                            </div>

                            <div class="col-12 mt-5 text-end border-top pt-3">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border shadow-sm" onclick="tutupForm<%=rnd%>()"><i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%></button>
                                <button type="submit" class="btn btn-success px-4 rounded-pill shadow-sm fw-bold" id="btnSimpan<%=rnd%>">
                                    <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%>
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    const masterModelClass<%=rnd%> = "<%=DynamicReport.class.getName()%>";
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;
    
    // Tracking state Upload
    let activeEditingReportId<%=rnd%> = "";
    let isJrxmlUploaded<%=rnd%> = false;
    
    // State untuk menampung JSON Parameter Laporan
    let reportParams<%=rnd%> = [];

    const showToast<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    // --- JSON Parameter Logic ---
    const renderParams<%=rnd%> = () => {
        let html = '';
        if(reportParams<%=rnd%>.length === 0) {
            html = '<tr><td colspan="6" class="text-center text-muted py-4 fst-italic"><%=Common.getBahasaConfig("Belum ada parameter. Klik \"Tambah Parameter\" untuk membuat.")%></td></tr>';
        } else {
            reportParams<%=rnd%>.forEach((p, i) => {
                let isCombo = p.type === 'combobox';
                let isDate = p.type === 'date';
                
                // Logika Pembuatan Input HTML untuk Default Value
                let defaultValueHtml = '';
                if (isDate) {
                    const dateOptions = [
                        "Tidak Diisi", "Kemarin", "Kemarin Lusa", "1 minggu yang lalu", 
                        "1 bulan yang lalu", "3 bulan yang lalu", "6 bulan yang lalu", 
                        "1 tahun yang lalu", "3 tahun yang lalu", "Besok", "Besok lusa", 
                        "1 minggu yang akan datang", "1 bulan yang akan datang", 
                        "3 bulan yang akan datang", "6 bulan yang akan datang", 
                        "1 tahun yang akan datang", "3 tahun yang akan datang"
                    ];
                    defaultValueHtml = '<select class="form-select form-select-sm" onchange="updateParam<%=rnd%>(' + i + ', \'default_value\', this.value)">';
                    dateOptions.forEach(opt => {
                        let optValue = opt === "Tidak Diisi" ? "" : opt;
                        let selected = (p.default_value === optValue) ? 'selected' : '';
                        defaultValueHtml += '<option value="' + optValue + '" ' + selected + '>' + opt + '</option>';
                    });
                    defaultValueHtml += '</select>';
                } else {
                    defaultValueHtml = '<input type="text" class="form-control form-control-sm" placeholder="<%=Common.getBahasaConfig("Opsional")%>" value="' + (p.default_value || '') + '" onchange="updateParam<%=rnd%>(' + i + ', \'default_value\', this.value)">';
                }

                html += '<tr>' +
                    '<td><input type="text" class="form-control form-control-sm fw-bold" placeholder="{NAMA_PARAMETER}" value="' + (p.parameter || '') + '" onchange="updateParam<%=rnd%>(' + i + ', \'parameter\', this.value)" required></td>' +
                    '<td><select class="form-select form-select-sm" onchange="updateParam<%=rnd%>(' + i + ', \'type\', this.value)">' +
                        '<option value="text" ' + (!isCombo && !isDate && p.type !== 'number' ? 'selected' : '') + '>Text</option>' +
                        '<option value="combobox" ' + (isCombo ? 'selected' : '') + '>Combobox</option>' +
                        '<option value="date" ' + (isDate ? 'selected' : '') + '>Date</option>' +
                        '<option value="number" ' + (p.type === 'number' ? 'selected' : '') + '>Number</option>' +
                    '</select></td>' +
                    '<td>' + defaultValueHtml + '</td>' +
                    '<td><input type="text" class="form-control form-control-sm font-monospace" placeholder="<%=Common.getBahasaConfig("select id, nama from tabel...")%>" value="' + (p.sqlPilihan || '') + '" onchange="updateParam<%=rnd%>(' + i + ', \'sqlPilihan\', this.value)" ' + (isCombo ? '' : 'disabled') + '></td>' +
                    '<td><input type="text" class="form-control form-control-sm font-monospace" placeholder="<%=Common.getBahasaConfig("toko.id == null")%>" value="' + (p.tampil_jika || '') + '" onchange="updateParam<%=rnd%>(' + i + ', \'tampil_jika\', this.value)"></td>' +
                    '<td class="text-center"><button type="button" class="btn btn-sm btn-outline-danger shadow-sm" onclick="removeParam<%=rnd%>(' + i + ')" title="<%=Common.getBahasaConfig("Hapus Parameter")%>"><i class="fas fa-trash"></i></button></td>' +
                '</tr>';
            });
        }
        document.getElementById('tbodyParams<%=rnd%>').innerHTML = html;
    };

    const addParam<%=rnd%> = () => {
        // Menambahkan properti "tampil_jika" ke dalam struktur JSON default
        reportParams<%=rnd%>.push({ parameter: "", type: "text", default_value: "", pilihan: [], sqlPilihan: "", tampil_jika: "" });
        renderParams<%=rnd%>();
    };

    const removeParam<%=rnd%> = (index) => {
        reportParams<%=rnd%>.splice(index, 1);
        renderParams<%=rnd%>();
    };

    const updateParam<%=rnd%> = (index, field, value) => {
        reportParams<%=rnd%>[index][field] = value;
        if (field === 'type') {
            if (value !== 'combobox') reportParams<%=rnd%>[index].sqlPilihan = ""; 
            if (value === 'date') reportParams<%=rnd%>[index].default_value = ""; // Reset default value bila beralih ke date
            renderParams<%=rnd%>(); 
        }
    };

    // --- UPLOAD JRXML FILE LOGIC (AJAX) ---
    const cekFileJrxml<%=rnd%> = async (id) => {
        var reqObj = { 
            "action": "file", 
            "class": "<%=ais.database.model.file.LampiranLain.class.getName()%>", 
            "ref": id.toString(), 
            "jenis": "<%=DynamicReport.class.getName()%>",
            "kondisiTambahan": "",
            "refresh": "false",
            "usingId": false
        };
        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(reqObj)
            });
            const dataResponse = await response.json();
            
            if(dataResponse.status == '00' && dataResponse.data){ 
                isJrxmlUploaded<%=rnd%> = true;
                $('#fileInfoArea<%=rnd%>').show();
                $('#linkDownloadJrxml<%=rnd%>').attr('href', dataResponse.data.url).html('<i class="fas fa-download me-1"></i> <%=Common.getBahasaConfig("Unduh File JRXML")%>');
            } else {
                isJrxmlUploaded<%=rnd%> = false;
                $('#fileInfoArea<%=rnd%>').hide();
            }
        } catch (error) {
            console.error("Gagal cek file:", error);
        }
    };

    const resetUpload<%=rnd%> = () => {
        $('#fileNameDisplay<%=rnd%>').val("");
        $('#progress-bar-<%=rnd%>').css('width', '0%').text('0%').removeClass('bg-success bg-danger').addClass('bg-danger');
        $('#progress-wrapper-<%=rnd%>').hide();
    };

    function uploadFileJrxml<%=rnd%>() {
        var fileInput = $('#input_dofile_jrxml_<%=rnd%>')[0];
        if (fileInput.files.length === 0) return;
        
        if (!activeEditingReportId<%=rnd%>) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Error: Harap simpan data laporan terlebih dahulu sebelum mengunggah file JRXML!")%>', 'bg-danger text-white');
            return;
        }

        var file = fileInput.files[0];
        if (!file.name.toLowerCase().endsWith('.jrxml')) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("File yang dipilih harus berformat .jrxml")%>', 'bg-warning text-dark');
            fileInput.value = '';
            return;
        }

        resetUpload<%=rnd%>();
        $('#fileNameDisplay<%=rnd%>').val(file.name); 
        $('#progress-wrapper-<%=rnd%>').fadeIn();
        $('#progress-bar-<%=rnd%>').css('width', '10%').text('10%').removeClass('bg-success bg-danger').addClass('bg-primary progress-bar-animated');
        
        var formData = new FormData();
        formData.append('nama', file.name);
        formData.append('clazz', '<%=ais.database.model.file.LampiranLain.class.getName()%>');
        formData.append('jenis', '<%=DynamicReport.class.getName()%>');
        formData.append('id', activeEditingReportId<%=rnd%>);
        formData.append('tanpaLogin', 'true');
        formData.append('fileContent', file);

        $.ajax({
            url: '<%=Common.ROOT%>/DoUpload',
            type: 'POST',
            data: formData,
            cache: false,
            contentType: false,
            processData: false,
            xhr: function() {
                var xhr = new window.XMLHttpRequest();
                xhr.upload.addEventListener("progress", function(evt) {
                    if (evt.lengthComputable) {
                        var percentComplete = Math.round((evt.loaded / evt.total) * 100);
                        $('#progress-bar-<%=rnd%>').css('width', percentComplete + '%').text(percentComplete + '%');
                    }
                }, false);
                return xhr;
            },
            success: function(response) {
                $('#progress-bar-<%=rnd%>').removeClass('bg-primary progress-bar-animated').addClass('bg-success');
                $('#progress-bar-<%=rnd%>').css('width', '100%').text('<%=Common.getBahasaConfigJS("Selesai")%>');

                setTimeout(function(){ $('#progress-wrapper-<%=rnd%>').fadeOut(); }, 1500);

                if(response.status === 'Sukses' || response.status === '00' || response.keterangan === 'Sukses'){
                   // Sukses Upload! Panggil simpanReport() untuk otomatis menutup & simpan form
                   isJrxmlUploaded<%=rnd%> = true;
                   showToast<%=rnd%>('<%=Common.getBahasaConfigJS("File JRXML berhasil diunggah!")%>', 'bg-success text-white');
                   simpanReport<%=rnd%>(); 
                } else {
                   showToast<%=rnd%>(response.keterangan || '<%=Common.getBahasaConfigJS("Gagal menyimpan file ke database.")%>', 'bg-danger text-white');
                }
                fileInput.value = '';
            },
            error: function(jqXHR, textStatus, errorThrown) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi bermasalah: ")%> ' + textStatus, 'bg-danger text-white');
                $('#progress-bar-<%=rnd%>').removeClass('bg-primary progress-bar-animated').addClass('bg-danger');
                $('#progress-bar-<%=rnd%>').text('<%=Common.getBahasaConfigJS("Gagal")%>');
            }
        });
    }

    // --- CRUD Excel ---
    const downloadExcelReport<%=rnd%> = () => {
        const table = document.getElementById('tabelHTMLDataReport<%=rnd%>');
        if (!table) return;
        const cloneTable = table.cloneNode(true);
        const elementsToRemove = cloneTable.querySelectorAll('.no-export, button, select, input, a');
        elementsToRemove.forEach(el => el.parentNode ? el.parentNode.removeChild(el) : null);
        
        const rows = cloneTable.rows;
        for (let i = 0; i < rows.length; i++) {
            const row = rows[i];
            for (let j = row.cells.length - 1; j >= 0; j--) {
                if (row.cells[j].classList.contains('no-export')) {
                    row.deleteCell(j);
                }
            }
        }
        const wb = XLSX.utils.table_to_book(cloneTable, {sheet: "Dynamic Report"});
        XLSX.writeFile(wb, "Data_DynamicReport_" + new Date().getTime() + ".xlsx");
    };

    const uploadExcelReport<%=rnd%> = async () => {
        const fileInput = document.getElementById('inputUploadExcel<%=rnd%>');
        if (fileInput.files.length === 0) return;
        const file = fileInput.files[0];
        
        const formData = new FormData();
        formData.append('fileContent', file);
        formData.append('action', 'importExcel');
        formData.append('class', masterModelClass<%=rnd%>);

        try {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Sedang mengunggah Excel...")%>', 'bg-info text-white');
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                body: formData
            });
            const result = await response.json();
            if(result.status === '00' || result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data Excel berhasil diunggah!")%>', 'bg-success text-white');
                resetAndLoadData<%=rnd%>();
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal mengunggah Excel.")%>', 'bg-danger text-white');
            }
        } catch(e) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        }
        fileInput.value = '';
    };

    // --- CRUD Main Logic ---
    const buildFilterSQL<%=rnd%> = () => {
        const keyword = document.getElementById('searchReport<%=rnd%>').value.trim().replace(/'/g, "''");
        let filters = [];
        if (keyword !== '') {
            filters.push("(this_.kode ILIKE '%" + keyword + "%' OR this_.nama ILIKE '%" + keyword + "%' OR this_.keterangan ILIKE '%" + keyword + "%')");
        }
        return filters.length > 0 ? filters.join(' AND ') : '';
    };

    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataReport<%=rnd%>();
    };

    const loadDataReport<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataReport<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';
        
        const start = (currentPage<%=rnd%> - 1);
        const whereClause = buildFilterSQL<%=rnd%>();
        
        var reqObj = {
            "action": "daftar", 
            "class": masterModelClass<%=rnd%>,
            "max": limitPerPage<%=rnd%>, 
            "halaman": start,
            "order1": "desc", 
            "sort1": "id", 
            "count": "true"
        };
        
        if (whereClause) {
            reqObj["where1"] = whereClause;
        }

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(reqObj)
            });
            const dataResponse = await response.json();
            totalRecords<%=rnd%> = parseInt(dataResponse.count || 0);
            const recordList = dataResponse.data || [];
            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="5" class="text-center text-muted py-5"><i class="fas fa-box-open fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada data laporan ditemukan.")%></td></tr>';
            } else {
                let no = (start * limitPerPage<%=rnd%>) + 1;
                recordList.forEach((row) => {
                    const kode = row.kode || '-';
                    const ket = row.keterangan || '-';
                    const isAktif = row.aktif !== false;
                    const statusBadge = isAktif ? '<span class="badge bg-success"><%=Common.getBahasaConfig("Aktif")%></span>' : '<span class="badge bg-danger"><%=Common.getBahasaConfig("Tidak Aktif")%></span>';
                    
                    const shortKet = ket.length > 80 ? ket.substring(0, 80) + '...' : ket;

                    htmlList += '<tr>' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-start fw-bold text-primary">' + kode + '</td>' +
                            '<td class="text-start text-muted small" title="' + ket.replace(/"/g, '&quot;') + '">' + shortKet + '</td>' +
                            '<td class="text-center align-middle">' + statusBadge + '</td>' +
                            '<td class="text-center text-nowrap no-export">' +
                                '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 fw-bold" onclick="editReport<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Edit Laporan / Upload JRXML")%>"><i class="fas fa-edit"></i></button>' +
                                '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 fw-bold" onclick="hapusReport<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data")%>"><i class="fas fa-trash-alt"></i></button>' +
                            '</td>' +
                        '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();
        } catch (error) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        const startIdx = totalRecords<%=rnd%> > 0 ? (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1 : 0;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        document.getElementById('pagingInfoReport<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startIdx + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b>';
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePageReport<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataReport<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataReport<%=rnd%>();
        }
    };

    const bukaFormTambah<%=rnd%> = () => {
        activeEditingReportId<%=rnd%> = "";
        isJrxmlUploaded<%=rnd%> = false;
        
        document.getElementById('inputIdReport<%=rnd%>').value = '';
        document.getElementById('formReport<%=rnd%>').reset();
        document.getElementById('inputAktif<%=rnd%>').value = 'true';
        document.getElementById('uploadFileArea<%=rnd%>').style.display = 'none'; // Sembunyikan saat buat baru
        
        reportParams<%=rnd%> = []; 
        renderParams<%=rnd%>();

        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i><%=Common.getBahasaConfig("Tambah Dynamic Report")%>';
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataReport<%=rnd%>();
    };

    const simpanReport<%=rnd%> = async () => {
        const idReport = document.getElementById('inputIdReport<%=rnd%>').value;
        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');
        
        // VALIDASI: Wajib ada file JRXML sebelum form dapat ditutup sepenuhnya.
        if (idReport !== '' && !isJrxmlUploaded<%=rnd%>) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Penyimpanan ditolak: Anda WAJIB mengunggah file template .jrxml terlebih dahulu!")%>', 'bg-danger text-white');
            return;
        }

        const jsonParameterValue = JSON.stringify(reportParams<%=rnd%>);

        const dataObj = {
            kode: document.getElementById('inputKode<%=rnd%>').value.trim(),
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value.trim(),
            nama: jsonParameterValue,
            aktif: document.getElementById('inputAktif<%=rnd%>').value === 'true'
        };
        
        const payload = { 
            action: "simpanDataRinci", 
            log: "true",
            class: masterModelClass<%=rnd%>, 
            data: dataObj
        };

        if (idReport !== '') { payload.id = idReport; }

        const oriBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btnSimpan.disabled = true;

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success' || result.id) {
                if (!isJrxmlUploaded<%=rnd%>) {
                    // Tahap 1 (Baru Buat Laporan): Tampilkan Area Unggah JRXML
                    let newId = result.id || idReport;
                    activeEditingReportId<%=rnd%> = newId; 
                    document.getElementById('inputIdReport<%=rnd%>').value = newId;
                    
                    document.getElementById('uploadFileArea<%=rnd%>').style.display = 'block';
                    document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-exclamation-triangle text-warning me-2"></i><%=Common.getBahasaConfig("Wajib Unggah JRXML")%>';
                    
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data berhasil disimpan. Sekarang WAJIB unggah file .jrxml!")%>', 'bg-warning text-dark');
                } else {
                    // Tahap 2: Laporan + JRXML berhasil lengkap disimpan, tutup formulir.
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Laporan dinamis beserta file JRXML berhasil diperbarui!")%>', 'bg-success text-white');
                    tutupForm<%=rnd%>();
                }
            } else {
                showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan data ke database.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        } finally {
            btnSimpan.innerHTML = oriBtn;
            btnSimpan.disabled = false;
        }
    };

    const editReport<%=rnd%> = async (id) => {
        const payload = { action: "load", class: masterModelClass<%=rnd%>, id: id };
        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success' || result.id) {
                const data = result.data;
                activeEditingReportId<%=rnd%> = data.id;
                
                document.getElementById('inputIdReport<%=rnd%>').value = data.id;
                document.getElementById('inputKode<%=rnd%>').value = data.kode || '';
                document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
                document.getElementById('inputAktif<%=rnd%>').value = data.aktif !== false ? 'true' : 'false';

                try {
                    reportParams<%=rnd%> = data.nama ? JSON.parse(data.nama) : [];
                } catch(err) {
                    reportParams<%=rnd%> = [];
                }
                renderParams<%=rnd%>(); 

                // Tampilkan Area Upload File JRXML & Cek Ketersediaan
                document.getElementById('uploadFileArea<%=rnd%>').style.display = 'block';
                cekFileJrxml<%=rnd%>(data.id);

                document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Dynamic Report")%>';
                document.getElementById('viewList<%=rnd%>').style.display = 'none';
                document.getElementById('viewForm<%=rnd%>').style.display = 'block';
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal mengambil data dari server.")%>', 'bg-danger text-white');
            }
        } catch (error) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
        }
    };

    const hapusReport<%=rnd%> = async (id) => {
        if (typeof proseshapusDataRinci === 'function') {
            proseshapusDataRinci(masterModelClass<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataReport<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                     currentPage<%=rnd%>--;
                 }
                 loadDataReport<%=rnd%>();
            });
        }
    };

    document.addEventListener("DOMContentLoaded", () => {
        document.getElementById('searchReport<%=rnd%>').addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                resetAndLoadData<%=rnd%>(); 
            }
        });
        loadDataReport<%=rnd%>();
    });
</script>