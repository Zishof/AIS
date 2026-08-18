<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="java.io.File"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.DynamicReport"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="java.util.Map"%>
<%@page import="com.google.gson.Gson"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// ==========================================
// 1. PEMERIKSAAN KEAMANAN & INISIALISASI
// ==========================================
Tbmuser tbmuser = null;
boolean isAdmin = true; 
String rnd = Common.getGeneratedBarCode(7);
Map<String, Object> parameters = ais.common.HashMapGenerator.getRand();

DynamicReport dynamicReport = null;
LampiranLain lampiranLain = null;
File fileLaporan = null;

String jsonConfigParams = "[]";
String reportTitle = Common.getBahasaConfig("Laporan Sistem");
String reportDesc = Common.getBahasaConfig("Halaman ini menyajikan data laporan secara dinamis.");
String baseParamsJson = "{}";
String filePath = "";

try {
    tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
        return;
    }

    if (tbmuser != null && tbmuser.getUserId() != null) {
        Common.insertProperty(Tbmuser.class, tbmuser, parameters, "tbmuser", 2);
    }
    
    if (tbmuser != null && tbmuser.getPedagang() != null && tbmuser.getPedagang().getToko() != null) {
        Common.insertProperty(Toko.class, tbmuser.getPedagang().getToko(), parameters, "toko", 1);
        isAdmin = false;
    }

    // Penarikan Metadata Laporan
    String reportId = request.getParameter("reportId");
    if (reportId != null && !reportId.trim().isEmpty()) {
        dynamicReport = (DynamicReport) GeneralValueObject.ambilData(DynamicReport.class, reportId);
        if (dynamicReport != null) {
            lampiranLain = LampiranLain.ambil(dynamicReport.getId(), DynamicReport.class.getName());
            if (lampiranLain != null) {
                fileLaporan = lampiranLain.ambilFile();
            }
            
            jsonConfigParams = (dynamicReport.getNama() != null && !dynamicReport.getNama().trim().isEmpty()) ? dynamicReport.getNama() : "[]";
            reportTitle = dynamicReport.getKode() != null ? dynamicReport.getKode() : reportTitle;
            reportDesc = dynamicReport.getKeterangan() != null ? dynamicReport.getKeterangan() : reportDesc;
        }
    }

    // Konversi Map ke JSON String menggunakan Gson
    Gson gson = new Gson();
    baseParamsJson = gson.toJson(parameters);
    filePath = fileLaporan != null ? fileLaporan.getAbsolutePath().replace("\\", "\\\\") : "";

} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/common/laporan.jsp:70");
} finally {
    // Memastikan koneksi / session hibernate tertutup agar memori efisien
    HibernateUtil.closeSession();
}
%>

<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

<div class="row mb-4 animate__animated animate__fadeInUp" id="viewLaporan<%=rnd%>">
    <div class="col-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
            
            <div class="card-header bg-white border-0 pt-4 pb-3 px-4 border-bottom border-light">
                <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-2">
                    <div>
                        <h5 class="fw-bold text-dark mb-1">
                            <i class="fas fa-file-invoice text-primary me-2"></i><%=Common.getBahasaConfig("Dokumen Laporan")%> : <span class="text-primary"><%=reportTitle%></span>
                        </h5>
                        <small class="text-muted"><%=reportDesc%></small>
                    </div>
                </div>
            </div>

            <div class="card-body p-4 pt-3">
                <% if (dynamicReport == null) { %>
                    <div class="alert alert-warning shadow-sm border-0 rounded-3 d-flex align-items-center">
                        <i class="fas fa-exclamation-triangle fa-2x me-3 text-warning"></i>
                        <span class="fw-bold text-dark"><%=Common.getBahasaConfig("Laporan tidak ditemukan. Silakan periksa kembali referensi tautan laporan Anda.")%></span>
                    </div>
                <% } else if (fileLaporan == null || !fileLaporan.exists()) { %>
                    <div class="alert alert-danger shadow-sm border-0 rounded-3 d-flex align-items-center">
                        <i class="fas fa-ban fa-2x me-3 text-danger"></i>
                        <span class="fw-bold text-dark"><%=Common.getBahasaConfig("Format templat laporan (.jrxml) belum tersedia atau tidak ditemukan di dalam peladen.")%></span>
                    </div>
                <% } else { %>
                    <div class="alert alert-info border-0 rounded-3 shadow-sm mb-4 d-flex align-items-center">
                        <i class="fas fa-info-circle fa-2x me-3 text-info"></i>
                        <span class="fw-medium text-dark"><%=Common.getBahasaConfig("Silakan lengkapi parameter di bawah ini untuk memulai proses penyusunan laporan.")%></span>
                    </div>

                    <div id="paramArea<%=rnd%>" class="row g-4 mb-4 pb-3">
                        <!-- Area Parameter (Akan Diisi Melalui JS) -->
                    </div>

                    <div class="d-flex justify-content-end gap-3 border-top pt-4 flex-wrap">
                        <% if (isAdmin) { %>
                            <button class="btn btn-outline-secondary fw-bold shadow-sm px-4 rounded-pill d-flex align-items-center me-auto" onclick="showParameterModal<%=rnd%>()" id="btnParameter<%=rnd%>" title="<%=Common.getBahasaConfig("Tampilkan rincian parameter gabungan (Sistem & Input)")%>">
                                <i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Parameter Sistem")%>
                            </button>
                        <% } %>
                        
                        <button class="btn btn-primary fw-bold shadow-sm px-4 rounded-pill d-flex align-items-center" onclick="generateReport<%=rnd%>('view')" id="btnView<%=rnd%>">
                            <i class="fas fa-eye me-2"></i><%=Common.getBahasaConfig("Tampilkan Laporan")%>
                        </button>
                        
                        <button class="btn btn-danger fw-bold shadow-sm px-4 rounded-pill d-flex align-items-center" onclick="generateReport<%=rnd%>('pdf')" id="btnDownloadPdf<%=rnd%>">
                            <i class="fas fa-file-pdf me-2"></i><%=Common.getBahasaConfig("Unduh PDF")%>
                        </button>

                        <button class="btn btn-success fw-bold shadow-sm px-4 rounded-pill d-flex align-items-center" onclick="generateReport<%=rnd%>('excel')" id="btnDownloadExcel<%=rnd%>">
                            <i class="fas fa-file-excel me-2"></i><%=Common.getBahasaConfig("Unduh Excel")%>
                        </button>
                    </div>
                <% } %>
            </div>
        </div>
    </div>
</div>

<div class="row animate__animated animate__fadeIn" id="reportResultContainer<%=rnd%>" style="display: none;">
    <div class="col-12">
        <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
            <div class="card-header bg-white border-0 pt-3 pb-2 px-4 border-bottom border-light d-flex justify-content-between align-items-center">
                <h6 class="fw-bold text-dark mb-0">
                    <i class="fas fa-file-invoice text-success me-2"></i><%=Common.getBahasaConfig("Pratinjau Dokumen Laporan")%>
                </h6>
                <button type="button" class="btn btn-sm btn-outline-danger rounded-circle shadow-sm" aria-label="Close" onclick="tutupPratinjau<%=rnd%>()" title="<%=Common.getBahasaConfig("Tutup Jendela Pratinjau")%>">
                    <i class="fas fa-times"></i>
                </button>
            </div>
            <div class="card-body p-0">
                <div class="ratio ratio-16x9">
                    <iframe id="iframeReport<%=rnd%>" src="" class="w-100 h-100 border-0" style="min-height: 600px; background-color: #f8f9fa;"></iframe>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // 1. DATA & KONFIGURASI GLOBAL
    // ==========================================
    const paramsConfig<%=rnd%> = <%=jsonConfigParams%>;
    const baseParams<%=rnd%> = <%=baseParamsJson%>;
    const filePath<%=rnd%> = "<%=filePath%>";

    // Fungsi Menampilkan Notifikasi
    const showToast<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    // Fungsi Logika Penentuan Waktu (Tanggal Default)
    const mapDateDefault<%=rnd%> = (defaultStr) => {
        if (!defaultStr || defaultStr === 'Tidak Diisi' || defaultStr === '') return "";
        let d = new Date();
        const str = defaultStr.toLowerCase().trim();
        
        if (str === "kemarin") d.setDate(d.getDate() - 1);
        else if (str === "kemarin lusa") d.setDate(d.getDate() - 2);
        else if (str === "1 minggu yang lalu") d.setDate(d.getDate() - 7);
        else if (str === "1 bulan yang lalu") d.setMonth(d.getMonth() - 1);
        else if (str === "3 bulan yang lalu") d.setMonth(d.getMonth() - 3);
        else if (str === "6 bulan yang lalu") d.setMonth(d.getMonth() - 6);
        else if (str === "1 tahun yang lalu") d.setFullYear(d.getFullYear() - 1);
        else if (str === "3 tahun yang lalu") d.setFullYear(d.getFullYear() - 3);
        else if (str === "besok") d.setDate(d.getDate() + 1);
        else if (str === "besok lusa") d.setDate(d.getDate() + 2);
        else if (str === "1 minggu yang akan datang") d.setDate(d.getDate() + 7);
        else if (str === "1 bulan yang akan datang") d.setMonth(d.getMonth() + 1);
        else if (str === "3 bulan yang akan datang") d.setMonth(d.getMonth() + 3);
        else if (str === "6 bulan yang akan datang") d.setMonth(d.getMonth() + 6);
        else if (str === "1 tahun yang akan datang") d.setFullYear(d.getFullYear() + 1);
        else if (str === "3 tahun yang akan datang") d.setFullYear(d.getFullYear() + 3);
        else return ""; 

        let month = '' + (d.getMonth() + 1);
        let day = '' + d.getDate();
        let year = d.getFullYear();
        if (month.length < 2) month = '0' + month;
        if (day.length < 2) day = '0' + day;
        
        return [year, month, day].join('-');
    };

    // ==========================================
    // 2. PEMBENTUKAN UI & PENARIKAN DATA
    // ==========================================
    const fetchComboboxOptions<%=rnd%> = async (sql) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sql, action: "sql" })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) {
            console.error("<%=Common.getBahasaConfig("Kesalahan penarikan data SQL:")%>", e);
            return [];
        }
    };

    const renderParamsForm<%=rnd%> = async () => {
        const container = document.getElementById('paramArea<%=rnd%>');
        if (!container) return;
        
        if (paramsConfig<%=rnd%>.length === 0) {
            container.innerHTML = '<div class="col-12 text-center text-muted fst-italic py-3"><i class="fas fa-check-circle me-2 text-success"></i><%=Common.getBahasaConfig("Laporan ini siap disajikan dan tidak memerlukan parameter tambahan dari pengguna.")%></div>';
            return;
        }

        let html = '';
        
        for (let i = 0; i < paramsConfig<%=rnd%>.length; i++) {
            const p = paramsConfig<%=rnd%>[i];
            
            // -----------------------------------------------------------------
            // LOGIKA TAMPIL_JIKA (Evaluasi kondisional visibilitas form input)
            // -----------------------------------------------------------------
            let isVisible = true;
            if (p.tampil_jika && p.tampil_jika.trim() !== "") {
                try {
                    let conditionStr = p.tampil_jika.trim();
                    const regexVar = /(['"]).*?\1|([a-zA-Z_][a-zA-Z0-9_\.]*)/g;
                    
                    conditionStr = conditionStr.replace(regexVar, function(match, quote, varName) {
                        if (quote) return match;
                        if (varName === 'null' || varName === 'true' || varName === 'false' || varName === 'undefined') {
                            return varName;
                        }
                        return 'baseParams<%=rnd%>["' + varName + '"]';
                    });

                    const evalCondition = new Function("baseParams<%=rnd%>", "try { return (" + conditionStr + "); } catch(e) { return false; }");
                    isVisible = evalCondition(baseParams<%=rnd%>);
                    
                } catch (err) {
                    console.warn("<%=Common.getBahasaConfig("Gagal mengevaluasi kondisi parameter:")%> " + p.tampil_jika, err);
                    isVisible = false; 
                }
            }

            if (!isVisible) continue; 
            
            // Render Komponen Parameter
            const paramNameRaw = p.parameter || '';
            const cleanName = paramNameRaw.replace(/[{}]/g, ''); 
            const labelText = cleanName.replace(/_/g, ' ').toUpperCase(); 
            const type = p.type || 'text';
            const defVal = p.default_value || '';
            
            let inputHtml = '';
            
            if (type === 'combobox') {
                inputHtml = '<select class="form-select border-primary shadow-sm fw-medium param-input-<%=rnd%>" data-param="' + cleanName + '" id="input_' + i + '_<%=rnd%>">';
                inputHtml += '<option value="">-- <%=Common.getBahasaConfig("Pilih")%> ' + labelText + ' --</option>';
                inputHtml += '</select>';
            } else if (type === 'date') {
                const dateVal = mapDateDefault<%=rnd%>(defVal);
                inputHtml = '<input type="date" class="form-control border-primary shadow-sm fw-medium param-input-<%=rnd%>" data-param="' + cleanName + '" id="input_' + i + '_<%=rnd%>" value="' + dateVal + '">';
            } else if (type === 'number') {
                inputHtml = '<input type="number" class="form-control border-primary shadow-sm fw-medium param-input-<%=rnd%>" data-param="' + cleanName + '" id="input_' + i + '_<%=rnd%>" value="' + defVal + '">';
            } else {
                inputHtml = '<input type="text" class="form-control border-primary shadow-sm fw-medium param-input-<%=rnd%>" data-param="' + cleanName + '" id="input_' + i + '_<%=rnd%>" value="' + defVal + '">';
            }

            html += '<div class="col-md-4 col-sm-6">';
            html += '<label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-angle-right text-primary me-1"></i>' + labelText + '</label>';
            html += inputHtml;
            html += '</div>';
        }
        
        container.innerHTML = html;

        // Memuat opsi data SQL untuk Combobox
        for (let i = 0; i < paramsConfig<%=rnd%>.length; i++) {
            const p = paramsConfig<%=rnd%>[i];
            if (p.type === 'combobox' && p.sqlPilihan) {
                const selectEl = document.getElementById('input_' + i + '_<%=rnd%>');
                if (selectEl) {
                    const options = await fetchComboboxOptions<%=rnd%>(p.sqlPilihan);
                    options.forEach(opt => {
                        const keys = Object.keys(opt);
                        const val = opt.id !== undefined ? opt.id : opt[keys[0]];
                        const label = opt.nama !== undefined ? opt.nama : opt[keys[1] || keys[0]];
                        const selected = (p.default_value == val) ? 'selected' : '';
                        selectEl.innerHTML += '<option value="' + val + '" ' + selected + '>' + label + '</option>';
                    });
                }
            }
        }
    };

    const tutupPratinjau<%=rnd%> = () => {
        document.getElementById('reportResultContainer<%=rnd%>').style.display = 'none';
        document.getElementById('iframeReport<%=rnd%>').src = "";
    };

    const getMergedParams<%=rnd%> = () => {
        const inputs = document.querySelectorAll('.param-input-<%=rnd%>');
        let userParams = {};
        
        inputs.forEach(input => {
            const pName = input.getAttribute('data-param');
            userParams[pName] = input.value;
        });

        return Object.assign({}, baseParams<%=rnd%>, userParams);
    };

    // ==========================================
    // 3. LOGIKA EKSEKUSI PEMBUATAN LAPORAN (API)
    // ==========================================
    const generateReport<%=rnd%> = async (mode) => {
        const mergedParams = getMergedParams<%=rnd%>();

        // Mengatur format laporan yang dikirim ke API
        let formatLap = "<%=ais.action.report.Report.PDF%>";
        if (mode === 'excel') {
            formatLap = "<%=ais.action.report.Report.XLS%>";
        }

        const payload = {
            action: "laporan",
            formatLaporan: formatLap,
            params: mergedParams,
            file: filePath<%=rnd%>
        };

        const btnView = document.getElementById('btnView<%=rnd%>');
        const btnPdf = document.getElementById('btnDownloadPdf<%=rnd%>');
        const btnExcel = document.getElementById('btnDownloadExcel<%=rnd%>');
        
        // Simpan state konten tombol
        const oriViewHtml = btnView ? btnView.innerHTML : "";
        const oriPdfHtml = btnPdf ? btnPdf.innerHTML : "";
        const oriExcelHtml = btnExcel ? btnExcel.innerHTML : "";

        // Rubah tombol menjadi memuat (Loading)
        if (mode === 'view' && btnView) {
            btnView.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Memproses...")%>';
            btnView.disabled = true;
        } else if (mode === 'pdf' && btnPdf) {
            btnPdf.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyiapkan PDF...")%>';
            btnPdf.disabled = true;
        } else if (mode === 'excel' && btnExcel) {
            btnExcel.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyiapkan Excel...")%>';
            btnExcel.disabled = true;
        }

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await response.json();

            if (result.status === '00' || result.status === 'success' || result.data) {
                const urlResult = result.data; 
                
                if (mode === 'view') {
                    document.getElementById('reportResultContainer<%=rnd%>').style.display = 'block';
                    document.getElementById('iframeReport<%=rnd%>').src = urlResult;
                    document.getElementById('reportResultContainer<%=rnd%>').scrollIntoView({ behavior: 'smooth', block: 'start' });
                } else {
                    // Untuk pengunduhan (Excel/PDF)
                    window.open(urlResult, '_blank');
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Dokumen laporan telah berhasil disiapkan dan diunduh!")%>', 'bg-success text-white');
                }
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal menyusun laporan: ")%>' + (result.description || result.message || ''), 'bg-danger text-white');
            }
        } catch (error) {
            console.error(error);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat memproses dokumen laporan.")%>', 'bg-danger text-white');
        } finally {
            // Mengembalikan state tombol seperti semula
            if (btnView) { btnView.innerHTML = oriViewHtml; btnView.disabled = false; }
            if (btnPdf) { btnPdf.innerHTML = oriPdfHtml; btnPdf.disabled = false; }
            if (btnExcel) { btnExcel.innerHTML = oriExcelHtml; btnExcel.disabled = false; }
        }
    };


    // ==========================================
    // 4. JENDELA PARAMETER SISTEM (KHUSUS ADMIN)
    // ==========================================
    const showParameterModal<%=rnd%> = () => {
        let modalEl = document.getElementById('modalParamMap<%=rnd%>');
        
        if (!modalEl) {
            const modalHtml = '<div class="modal fade" id="modalParamMap<%=rnd%>" tabindex="-1" aria-hidden="true">' +
                '<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">' +
                    '<div class="modal-content border-0 rounded-4 shadow-lg">' +
                        '<div class="modal-header bg-light border-bottom-0 pb-3">' +
                            '<h5 class="modal-title fw-bold text-dark">' +
                                '<i class="fas fa-cogs text-secondary me-2"></i>' + '<%=Common.getBahasaConfigJS("Rincian Parameter Sistem")%>' +
                            '</h5>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="' + '<%=Common.getBahasaConfigJS("Tutup")%>' + '"></button>' +
                        '</div>' +
                        '<div class="modal-body p-4 pt-2">' +
                            '<div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">' +
                                '<div class="input-group input-group-sm shadow-sm" style="width: 400px;">' +
                                    '<span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>' +
                                    '<input type="text" class="form-control border-start-0 border-end-0 ps-0 bg-white fw-medium" id="searchParamTbl<%=rnd%>" placeholder="' + '<%=Common.getBahasaConfigJS("Cari Kunci atau Nilai Parameter...")%>' + '">' +
                                    '<button class="btn btn-primary fw-bold px-3" type="button" onclick="filterTableParam<%=rnd%>()">' +
                                        '<i class="fas fa-search me-1"></i>' + '<%=Common.getBahasaConfigJS("Cari")%>' +
                                    '</button>' +
                                '</div>' +
                                '<button class="btn btn-sm btn-success rounded-pill px-4 shadow-sm fw-bold d-flex align-items-center" onclick="downloadExcelParam<%=rnd%>()" title="' + '<%=Common.getBahasaConfigJS("Unduh tabel ini sebagai berkas Excel")%>' + '">' +
                                    '<i class="fas fa-file-excel me-2"></i>' + '<%=Common.getBahasaConfigJS("Ekspor Excel")%>' +
                                '</button>' +
                            '</div>' +
                            
                            '<div id="loadingParamTbl<%=rnd%>" class="text-center py-4 text-primary" style="display: none;">' +
                                '<div class="spinner-border me-2 align-middle" role="status"></div>' +
                                '<span class="fw-bold align-middle">' + '<%=Common.getBahasaConfigJS("Sedang mencari data, mohon tunggu...")%>' + '</span>' +
                            '</div>' +
                            
                            '<div class="table-responsive border rounded-3 shadow-sm" id="tableParamContainer<%=rnd%>">' +
                                '<table class="table table-hover table-striped align-middle mb-0" id="tableMapParam<%=rnd%>">' +
                                    '<thead class="table-dark text-center align-middle">' +
                                        '<tr>' +
                                            '<th class="fw-semibold text-uppercase small py-3" style="width: 60px;">#</th>' +
                                            '<th class="fw-semibold text-uppercase small text-start px-3" style="width: 35%;"><i class="fas fa-key me-1"></i>' + '<%=Common.getBahasaConfigJS("Kunci Parameter")%>' + '</th>' +
                                            '<th class="fw-semibold text-uppercase small text-start px-3"><i class="fas fa-database me-1"></i>' + '<%=Common.getBahasaConfigJS("Nilai Parameter")%>' + '</th>' +
                                        '</tr>' +
                                    '</thead>' +
                                    '<tbody id="tbodyMapParam<%=rnd%>">' +
                                    '</tbody>' +
                                '</table>' +
                            '</div>' +
                        '</div>' +
                        '<div class="modal-footer border-top-0 bg-light">' +
                            '<button type="button" class="btn btn-secondary px-4 rounded-pill fw-semibold shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i>' + '<%=Common.getBahasaConfigJS("Tutup Jendela")%>' + '</button>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            
            document.body.insertAdjacentHTML('beforeend', modalHtml);
            modalEl = document.getElementById('modalParamMap<%=rnd%>');
            
            document.getElementById('searchParamTbl<%=rnd%>').addEventListener('keydown', function(event) {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    filterTableParam<%=rnd%>();
                }
            });
        }
        
        renderTableParam<%=rnd%>(getMergedParams<%=rnd%>());
        const modalInstance = new bootstrap.Modal(modalEl);
        modalInstance.show();
    };

    const renderTableParam<%=rnd%> = (currentParams) => {
        const tbody = document.getElementById('tbodyMapParam<%=rnd%>');
        let html = '';
        let index = 1;
        
        for (const key in currentParams) {
            if (currentParams.hasOwnProperty(key)) {
                let rawVal = currentParams[key];
                let displayVal = (typeof rawVal === 'object' && rawVal !== null) ? JSON.stringify(rawVal) : String(rawVal);
                
                const safeKey = String(key).replace(/</g, "&lt;").replace(/>/g, "&gt;");
                const safeVal = displayVal.replace(/</g, "&lt;").replace(/>/g, "&gt;");
                
                html += '<tr>' +
                    '<td class="text-center text-muted fw-medium">' + (index++) + '</td>' +
                    '<td class="text-start fw-bold text-primary font-monospace small px-3">' + safeKey + '</td>' +
                    '<td class="text-start text-dark small px-3" style="word-break: break-all;">' + safeVal + '</td>' +
                '</tr>';
            }
        }
        
        if (html === '') {
            html = '<tr><td colspan="3" class="text-center text-muted py-5 fst-italic"><i class="fas fa-box-open fa-2x mb-2 d-block opacity-50"></i>' + '<%=Common.getBahasaConfigJS("Data parameter sistem saat ini kosong.")%>' + '</td></tr>';
        }
        
        tbody.innerHTML = html;
    };

    const filterTableParam<%=rnd%> = () => {
        const keyword = document.getElementById('searchParamTbl<%=rnd%>').value.toLowerCase();
        const rows = document.querySelectorAll('#tbodyMapParam<%=rnd%> tr');
        const loadingEl = document.getElementById('loadingParamTbl<%=rnd%>');
        const tableContainer = document.getElementById('tableParamContainer<%=rnd%>');
        
        if (loadingEl) loadingEl.style.display = 'block';
        if (tableContainer) tableContainer.style.opacity = '0.4';
        
        setTimeout(() => {
            let adaData = false;
            rows.forEach(row => {
                const textContent = row.innerText.toLowerCase();
                if (textContent.includes(keyword)) {
                    row.style.display = '';
                    adaData = true;
                } else {
                    row.style.display = 'none';
                }
            });
            
            if (loadingEl) loadingEl.style.display = 'none';
            if (tableContainer) tableContainer.style.opacity = '1';
        }, 150);
    };

    const downloadExcelParam<%=rnd%> = () => {
        const table = document.getElementById('tableMapParam<%=rnd%>');
        if (!table) return;
        
        try {
            const wb = XLSX.utils.table_to_book(table, {sheet: "Data Parameter"});
            XLSX.writeFile(wb, "Data_Parameter_Sistem_" + new Date().getTime() + ".xlsx");
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Dokumen parameter sistem berhasil diunduh!")%>', 'bg-success text-white');
        } catch (e) {
            console.error(e);
            alert('<%=Common.getBahasaConfigJS("Gagal mengunduh dokumen. Pastikan pustaka pendukung (SheetJS) telah termuat dengan benar.")%>');
        }
    };

    // 5. Inisialisasi awal saat halaman selesai dimuat
    document.addEventListener("DOMContentLoaded", () => {
        renderParamsForm<%=rnd%>();
    });
</script>