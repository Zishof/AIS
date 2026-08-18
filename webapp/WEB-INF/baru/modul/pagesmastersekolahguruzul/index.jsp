<%@page import="java.util.Arrays"%>
<%@page import="java.util.List"%>
<%@page import="java.io.InputStream"%>
<%@page import="org.apache.commons.fileupload.FileItem"%>
<%@page import="org.apache.commons.fileupload.disk.DiskFileItemFactory"%>
<%@page import="org.apache.commons.fileupload.servlet.ServletFileUpload"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.common.CommonExcelJspUtil"%>
<%@page import="ais.action.master.sekolah.GuruAction"%>
<%@page import="ais.action.master.KonfigurasiTampilanGuruAction"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>

<%
    // 1. SECURITY CHECK
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
    }
    String rnd = Common.getGeneratedBarCode(7);
    
    // Mengecek apakah user adalah Admin Utama / Super Admin
    boolean isAdmin = Common.getApakahAdminLain(tbmuser);
    
    // Privilege Check (Jika admin, otomatis true untuk semua akses)
    boolean edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser) || isAdmin;
    boolean delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser) || isAdmin;
    boolean add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE, tbmuser) || isAdmin;

    // =========================================================================
    // MICRO-CONTROLLER: IMPORT / EXPORT EXCEL 
    // =========================================================================
    String actionParams = request.getParameter("action");
    if ("export".equals(actionParams)) {
        org.hibernate.Session sessExport = HibernateUtil.openSession();
        try {
            List<Guru> gurus = sessExport.createCriteria(Guru.class).list();
            // Export dilakukan di dalam try agar lazy-load property Guru masih bisa berjalan
            CommonExcelJspUtil.exportDataToExcel(response, gurus, GuruAction.DATA, GuruAction.DATA_DESC, "Data_Guru", Guru.class);
        } finally {
            HibernateUtil.closeSessionQuietly(sessExport);
        }
        return;
    }
    
    if ("import".equals(actionParams) && ServletFileUpload.isMultipartContent(request)) {
        try {
            List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
            for (FileItem item : items) {
                if (!item.isFormField()) {
                    InputStream is = item.getInputStream();
                    JSONObject resImport = CommonExcelJspUtil.importDataFromExcel(is, GuruAction.DATA, Guru.class);
                    out.print(resImport.toString());
                    return;
                }
            }
        } catch (Exception e) {
            out.print("{\"status\":\"99\", \"message\":\"Gagal membaca file.\"}");
            return;
        }
    }

    // =========================================================================
    // PERSIAPAN ARRAY DINAMIS UNTUK GENERATOR FORM
    // =========================================================================
    String[] DATA = GuruAction.DATA;
    String[] DATA_DESC = GuruAction.DATA_DESC;
    
    // Klasifikasi field untuk menentukan jenis input HTML
    List<String> dateFields = Arrays.asList("tanggalLahir", "tglSkCpns", "tmtSkAngkat", "tmtPns", "tmtKerja");
    List<String> booleanFields = Arrays.asList("aktif", "sudahLisensiKepalaSekolah", "pernahDiklatKepengawasan", "keahlianBraille", "keahlianBahasaIsyarat", "milikUniversitas");
    List<String> selectFields = Arrays.asList("agama", "negara", "kecamatan", "statusKepegawaian", "jenisPendidikDanTenagaKependidikan", "statusPegawai", "golonganPegawai", "sumberGaji", "lembagaPengangkat", "pekerjaanAyah", "pekerjaanIbu", "pekerjaanSuamiIstri", "pendidikan", "yayasan", "sekolah", "sekolah1", "sekolah2", "sekolah3", "jenisGuru", "statusNikah", "jenisKelamin", "bahasa", "kewarganegaraan");
    List<String> textareaFields = Arrays.asList("alamatGuru", "tugasTambahan", "mengajar", "jamTugasTambahan", "siswa", "kompetensi", "sertifikasi", "keterangan");
    List<String> hiddenFields = Arrays.asList("id", "oleh", "olehId", "pegawai", "pegawaiId", "tanggal_dirubah");
%>

<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
    <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4 mb-4">
        <div class="card-header bg-white border-0 pt-4 pb-3 px-4">
            <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-2">
                <div>
                    <h5 class="fw-bold text-dark mb-1"><i class="fas fa-chalkboard-teacher text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Data Guru")%></h5>
                </div>
                
                <div class="d-flex align-items-center gap-2 flex-wrap">
                    <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                        <input type="text" class="form-control" id="searchGuru<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Cari Nama atau NIP...")%>" onkeypress="if(event.key === 'Enter') resetAndLoadData<%=rnd%>()">
                        <button class="btn btn-secondary" onclick="resetAndLoadData<%=rnd%>()"><i class="fas fa-search"></i></button>
                    </div>

                    <div class="btn-group shadow-sm">
                        <a href="?action=export" class="btn btn-sm btn-success fw-bold"><i class="fas fa-file-excel me-1"></i> <%=Common.getBahasaConfig("Export")%></a>
                        <% if (add || edit) { %>
                        <button class="btn btn-sm btn-warning fw-bold text-dark" onclick="document.getElementById('importFile<%=rnd%>').click();"><i class="fas fa-file-import me-1"></i> <%=Common.getBahasaConfig("Import")%></button>
                        <input type="file" id="importFile<%=rnd%>" class="d-none" accept=".xlsx" onchange="prosesImportExcel<%=rnd%>(this)">
                        <% } %>
                    </div>
                    
                    <% if (add) { %>
                    <button class="btn btn-sm btn-primary fw-bold" onclick="bukaFormTambah<%=rnd%>()"><i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Tambah Guru")%></button>
                    <% } %>
                </div>
            </div>
        </div>
        <div class="card-body p-4 pt-0">
            <div class="table-responsive border rounded-3 shadow-sm bg-white">
                <table class="table table-hover table-striped align-middle mb-0">
                    <thead class="table-dark text-center align-middle">
                        <tr>
                            <th style="width: 60px;">#</th>
                            <th style="width: 80px;"><%=Common.getBahasaConfig("Foto")%></th>
                            <th class="text-start"><%=Common.getBahasaConfig("Nama / NIP")%></th>
                            <th class="text-start"><%=Common.getBahasaConfig("Jenis PTK / Kelamin")%></th>
                            <th class="text-start"><%=Common.getBahasaConfig("Telepon/HP")%></th>
                            <th><%=Common.getBahasaConfig("Status")%></th>
                            <th style="width: 120px;"><i class="fas fa-cogs"></i></th>
                        </tr>
                    </thead>
                    <tbody id="tabelDataGuru<%=rnd%>">
                        <tr><td colspan="7" class="text-center py-5"><div class="spinner-border text-primary"></div></td></tr>
                    </tbody>
                </table>
            </div>
            <div class="d-flex justify-content-between align-items-center mt-3">
                <div class="small text-muted" id="pagingInfoGuru<%=rnd%>"></div>
                <ul class="pagination pagination-sm mb-0 shadow-sm" id="paginationContainer<%=rnd%>"></ul>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="modalFotoBesar<%=rnd%>" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content bg-transparent border-0 shadow-none">
            <div class="modal-body text-center p-0 position-relative">
                <button type="button" class="btn-close btn-close-white position-absolute top-0 end-0 m-3" data-bs-dismiss="modal"></button>
                <img id="imgPreviewBesar<%=rnd%>" src="" class="img-fluid rounded-4 shadow-lg border border-3 border-white">
            </div>
        </div>
    </div>
</div>

<div id="viewForm<%=rnd%>" class="animate__animated animate__fadeIn" style="display: none;">
    <div class="card border-0 shadow-sm rounded-4 border-top border-success border-4">
        <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
            <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                <i class="fas fa-user-edit text-success me-2"></i><%=Common.getBahasaConfig("Formulir Data Guru")%>
            </h5>
        </div>
        <div class="card-body p-4">
            <form id="formGuru<%=rnd%>" onsubmit="event.preventDefault(); simpanGuru<%=rnd%>();">
                <input type="hidden" id="inputIdGuru<%=rnd%>" value="">
                
                <div class="row g-4 mt-1">
                    <div class="col-md-3 text-center">
                        <h6 class="fw-bold text-muted border-bottom pb-2 mb-3"><%=Common.getBahasaConfig("Foto Profil")%></h6>
                        <div class="bg-light rounded-4 border p-2 shadow-sm mb-3">
                            <jsp:include page="/WEB-INF/baru/modul/common/upload_component.jsp">
                                <jsp:param name="clazz" value="ais.database.model.file.FotoGuru"/>
                                <jsp:param name="jenis" value="ais.database.model.file.FotoGuru.DEFAULT_JENIS"/>
                                <jsp:param name="col" value="foto_profil"/>
                                <jsp:param name="rnd" value="<%=rnd%>"/>
                                <jsp:param name="label" value="Foto"/>
                                <jsp:param name="accept" value="accept='image/*'"/>
                                <jsp:param name="afterSave" value="handleUploadAfterSave(response);"/>
                            </jsp:include>
                        </div>
                    </div>
                    
                    <div class="col-md-9">
                        <ul class="nav nav-tabs mb-4 fw-bold" id="tabGuru<%=rnd%>" role="tablist">
                            <li class="nav-item"><a class="nav-link active" data-bs-toggle="tab" href="#tabBiodata<%=rnd%>"><i class="fas fa-address-card me-1"></i>Biodata & Kepegawaian</a></li>
                        </ul>
                        
                        <div class="tab-content">
                            <div class="tab-pane fade show active" id="tabBiodata<%=rnd%>">
                                <div class="row g-3">
                                    <% 
                                    // GENERATOR FIELD DINAMIS MENGGUNAKAN LOGIKA EKSISTING
                                    for (int i = 0; i < DATA.length; i++) {
                                        String f = DATA[i];
                                        String label = DATA_DESC[i];
                                        
                                        // Skip hidden fields
                                        if (hiddenFields.contains(f)) continue;

                                        // MENGGUNAKAN METHOD ASLI DARI KonfigurasiTampilanGuruAction
                                        String stat = KonfigurasiTampilanGuruAction.statusWajibIsi(f);
                                        
                                        if (stat.equals(Konfigurasi.TIDAK_AKTIF)) continue;

                                        boolean isReadOnly = stat.equals(Konfigurasi.READ_ONLY) || (stat.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) && !isAdmin);
                                        
                                        // Jika status AKTIF berarti Wajib Isi (karena ada AKTIF_TIDAK_WAJIB)
                                        boolean isRequired = stat.equals(Konfigurasi.AKTIF); 
                                        
                                        String reqAttr = isRequired ? "required" : "";
                                        String disAttr = isReadOnly ? "disabled" : "";
                                        String star = isRequired ? "<span class='text-danger'>*</span>" : "";
                                        
                                        out.print("<div class='col-md-6'>");
                                        out.print("<label class='form-label small fw-bold text-secondary'>" + label + " " + star + "</label>");

                                        if (selectFields.contains(f)) {
                                            out.print("<select class='form-select shadow-sm' id='input_" + f + rnd + "' " + reqAttr + " " + disAttr + ">");
                                            out.print("<option value=''>-- Pilih " + label + " --</option>");
                                            out.print("</select>");
                                        } else if (booleanFields.contains(f)) {
                                            out.print("<select class='form-select shadow-sm' id='input_" + f + rnd + "' " + reqAttr + " " + disAttr + ">");
                                            out.print("<option value='true'>Ya / Aktif</option><option value='false'>Tidak / Non-Aktif</option>");
                                            out.print("</select>");
                                        } else if (dateFields.contains(f)) {
                                            out.print("<input type='date' class='form-control shadow-sm' id='input_" + f + rnd + "' " + reqAttr + " " + disAttr + ">");
                                        } else if (textareaFields.contains(f)) {
                                            out.print("<textarea class='form-control shadow-sm' id='input_" + f + rnd + "' rows='2' " + reqAttr + " " + disAttr + "></textarea>");
                                        } else {
                                            out.print("<input type='text' class='form-control shadow-sm' id='input_" + f + rnd + "' " + reqAttr + " " + disAttr + ">");
                                        }
                                        out.print("</div>");
                                    }
                                    %>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="col-12 mt-4 text-end border-top pt-3">
                        <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-semibold border shadow-sm" onclick="tutupForm<%=rnd%>()"><i class="fas fa-times text-danger me-2"></i>Batal</button>
                        <% if (add || edit) { %>
                        <button type="submit" class="btn btn-success px-4 rounded-pill shadow-sm fw-bold" id="btnSimpan<%=rnd%>"><i class="fas fa-save me-2"></i>Simpan Data</button>
                        <% } %>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    // Konfigurasi Field Dinamis dari Java ke JS
    window.masterModelClass<%=rnd%> = "<%=Guru.class.getName()%>";
    window.arrFields<%=rnd%> = <%= new JSONArray(DATA).toString() %>;
    window.arrSelectFields<%=rnd%> = <%= new JSONArray(selectFields).toString() %>;
    window.arrBooleanFields<%=rnd%> = <%= new JSONArray(booleanFields).toString() %>;
    window.arrDateFields<%=rnd%> = <%= new JSONArray(dateFields).toString() %>;
    
    window.pendingUploads<%=rnd%> = [];
    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    
    // IMPORT EXCEL AJAX
    const prosesImportExcel<%=rnd%> = async (inputElement) => {
        if(inputElement.files.length === 0) return;
        let file = inputElement.files[0];
        let formData = new FormData();
        formData.append('file', file);
        tampilkanToast('Sedang mengimpor data...', 'bg-info text-white');
        try {
            const res = await fetch('?action=import', { method: 'POST', body: formData });
            const result = await res.json();
            if(result.status === '00') {
                tampilkanToast(result.message, 'bg-success text-white');
                resetAndLoadData<%=rnd%>();
            } else { tampilkanToast(result.message, 'bg-danger text-white'); }
        } catch (e) { tampilkanToast('Koneksi terputus.', 'bg-danger text-white'); }
        inputElement.value = ''; 
    };

    // UPLOAD PENDING SAVES
    function handleUploadAfterSave(response) {
        let currentId = document.getElementById('inputIdGuru<%=rnd%>').value;
        if(currentId < 0) { 
            window.pendingUploads<%=rnd%>.push({
                id_val: response.id, class_val: 'ais.database.model.file.FotoGuru', jenis_val: 'ais.database.model.file.FotoGuru.DEFAULT_JENIS'
            });
        }
    }

    const showFotoBesar<%=rnd%> = (url) => {
        if(!url || url === '-' || url.trim() === '') return;
        document.getElementById('imgPreviewBesar<%=rnd%>').src = url;
        new bootstrap.Modal(document.getElementById('modalFotoBesar<%=rnd%>')).show();
    };

    // FUNGSI LOAD MASTER SELECT DINAMIS
    const loadMasterData<%=rnd%> = async (className, targetId, idField='id', textField='nama') => {
        const el = document.getElementById(targetId);
        if(!el) return;
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ action: "daftar", class: className, max: 500, order1: "asc", sort1: textField }) });
            const result = await res.json();
            let html = '<option value="">-- Pilih --</option>';
            if(result.data) {
                result.data.forEach(row => { html += `<option value="\${row[idField]}">\${row[textField]}</option>`; });
            }
            el.innerHTML = html;
        } catch(e) {}
    };

    // LOAD DATA UTAMA
    const resetAndLoadData<%=rnd%> = () => { currentPage<%=rnd%> = 1; loadDataGuru<%=rnd%>(); };

    const loadDataGuru<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataGuru<%=rnd%>');
        const keyword = document.getElementById('searchGuru<%=rnd%>').value.trim();
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5"><div class="spinner-border text-primary"></div></td></tr>';
        
        var reqObj = { "action": "daftar", "class": window.masterModelClass<%=rnd%>, "deep": "1", "max": limitPerPage<%=rnd%>, "halaman": (currentPage<%=rnd%> - 1), "order1": "asc", "sort1": "namaGuru", "count": "true" };
        if(keyword) reqObj["where1"] = "namaGuru ILIKE '%" + keyword + "%' OR nip ILIKE '%" + keyword + "%'";

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqObj) });
            const res = await response.json();
            const recordList = res.data || [];
            let totalRecords = parseInt(res.count || 0);
            
            if (recordList.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-5"><i class="fas fa-box-open fa-3x mb-3 d-block"></i>Tidak ada data ditemukan.</td></tr>';
            } else {
                let htmlList = '';
                let no = ((currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>) + 1;
                
                recordList.forEach((row) => {
                    const safeNama = (row.namaGuru || '-').replace(/'/g, "\\'");
                    const fotoKecil = row.foto_kecil || '<%=Common.ROOT%>/img/administrator-icon_default.png';
                    const fotoBesar = row.foto_besar || fotoKecil;
                    const statusBadge = row.aktif ? '<span class="badge bg-success">Aktif</span>' : '<span class="badge bg-danger">Non-Aktif</span>';
                    const jnsPtk = row["jenisPendidikDanTenagaKependidikan.nama"] || '-';

                    let fotoHtml = `<img src="\${fotoKecil}" class="rounded-circle shadow-sm cursor-pointer border" style="width: 45px; height: 45px; object-fit: cover;" onclick="showFotoBesar<%=rnd%>('\${fotoBesar}')">`;

                    let actionBtns = '';
                    <% if (edit) { %> actionBtns += `<button class="btn btn-sm btn-outline-warning shadow-sm px-2 me-1" onclick="editGuru<%=rnd%>(\${row.id})"><i class="fas fa-edit"></i></button>`; <% } %>
                    <% if (delete) { %> actionBtns += `<button class="btn btn-sm btn-outline-danger shadow-sm px-2" onclick="hapusGuru<%=rnd%>(\${row.id})"><i class="fas fa-trash-alt"></i></button>`; <% } %>

                    htmlList += `<tr>
                        <td class="text-center text-muted fw-medium">\${no++}</td>
                        <td class="text-center align-middle">\${fotoHtml}</td>
                        <td class="text-start"><div class="fw-bold text-dark">\${safeNama}</div><small class="text-muted">NIP: \${row.nip || '-'}</small></td>
                        <td class="text-start text-muted small"><div class="fw-bold">\${jnsPtk}</div>\${row.jenisKelamin || '-'}</td>
                        <td class="text-start text-muted small">\${row.teleponGuru || row.hp || '-'}</td>
                        <td class="text-center align-middle">\${statusBadge}</td>
                        <td class="text-center text-nowrap">\${actionBtns}</td>
                    </tr>`;
                });
                tbody.innerHTML = htmlList;
                
                document.getElementById('pagingInfoGuru<%=rnd%>').innerHTML = `Menampilkan \${recordList.length} dari \${totalRecords} data`;
                let totalP = Math.ceil(totalRecords/limitPerPage<%=rnd%>) || 1;
                document.getElementById('paginationContainer<%=rnd%>').innerHTML = `
                    <li class="page-item \${currentPage<%=rnd%> === 1 ? 'disabled' : ''}"><button class="page-link" onclick="currentPage<%=rnd%>--; loadDataGuru<%=rnd%>()"><i class="fas fa-chevron-left"></i></button></li>
                    <li class="page-item disabled"><span class="page-link fw-bold text-dark">\${currentPage<%=rnd%>} / \${totalP}</span></li>
                    <li class="page-item \${currentPage<%=rnd%> === totalP ? 'disabled' : ''}"><button class="page-link" onclick="currentPage<%=rnd%>++; loadDataGuru<%=rnd%>()"><i class="fas fa-chevron-right"></i></button></li>
                `;
            }
        } catch (e) { tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-5">Gagal menarik data.</td></tr>'; }
    };

    // GET DATA DINAMIS FORM 
    const getPayloadBiodata<%=rnd%> = () => {
        let payload = {};
        window.arrFields<%=rnd%>.forEach(f => {
            let el = document.getElementById('input_' + f + '<%=rnd%>');
            if (el) {
                let val = el.value.trim();
                if (window.arrBooleanFields<%=rnd%>.includes(f)) {
                    payload[f] = (val === 'true');
                } else if (window.arrSelectFields<%=rnd%>.includes(f)) {
                    payload[f] = val ? (isNaN(val) ? val : parseInt(val)) : null; 
                } else {
                    payload[f] = val || null;
                }
            }
        });
        return payload;
    };

    const simpanGuru<%=rnd%> = async () => {
        const btn = document.getElementById('btnSimpan<%=rnd%>');
        const originTxt = btn.innerHTML;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Menyimpan...'; btn.disabled = true;

        let curId = document.getElementById('inputIdGuru<%=rnd%>').value;
        const payload = { action: "simpanDataRinci", log: "true", class: window.masterModelClass<%=rnd%>, data: getPayloadBiodata<%=rnd%>() };
        if (curId && curId > 0) payload.id = curId;

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success' || result.id) {
                let savedId = result.id || curId;
                if (window.pendingUploads<%=rnd%> && window.pendingUploads<%=rnd%>.length > 0) {
                    let promises = window.pendingUploads<%=rnd%>.map(item => fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ "action": "update_file", "id": item.id_val, "class": item.class_val, "jenis": item.jenis_val, "ref": savedId }) }));
                    await Promise.all(promises); window.pendingUploads<%=rnd%> = [];
                }
                tampilkanToast('Data Guru Berhasil Disimpan!', 'bg-success text-white'); tutupForm<%=rnd%>();
            } else { tampilkanToast(result.description || 'Gagal menyimpan data!', 'bg-danger text-white'); }
        } catch (error) { tampilkanToast('Koneksi terputus.', 'bg-danger text-white'); } finally { btn.innerHTML = originTxt; btn.disabled = false; }
    };

    const bukaFormTambah<%=rnd%> = () => {
        document.getElementById('formGuru<%=rnd%>').reset();
        document.getElementById('inputIdGuru<%=rnd%>').value = -Math.floor(Math.random() * 9999999);
        window.pendingUploads<%=rnd%> = [];
        try { resetUpload<%=rnd%>(); } catch(e){}
        
        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i>Tambah Guru Baru';
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const editGuru<%=rnd%> = async (id) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "load", class: window.masterModelClass<%=rnd%>, id: id }) });
            const result = await res.json();
            
            if (result.status === '00' || result.data) {
                const data = result.data;
                document.getElementById('inputIdGuru<%=rnd%>').value = data.id;
                
                // MAPPING DATA KE FORM DINAMIS
                window.arrFields<%=rnd%>.forEach(f => {
                    let el = document.getElementById('input_' + f + '<%=rnd%>');
                    if(el) {
                        let val = data[f];
                        if (window.arrSelectFields<%=rnd%>.includes(f) && data[f + '.id']) val = data[f + '.id'];
                        if (window.arrDateFields<%=rnd%>.includes(f) && data[f + '.datetime_format']) val = data[f + '.datetime_format'];
                        
                        if (window.arrBooleanFields<%=rnd%>.includes(f)) { el.value = (val === true || val === 'true') ? 'true' : 'false'; }
                        else { el.value = val || ''; }
                    }
                });
                
                try { checkExistingFile<%=rnd%>(); } catch(e){} 
                document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i>Ubah Data Guru';
                document.getElementById('viewList<%=rnd%>').style.display = 'none';
                document.getElementById('viewForm<%=rnd%>').style.display = 'block';
            }
        } catch (e) { tampilkanToast('Gagal memuat data.', 'bg-danger text-white'); }
    };

    const hapusGuru<%=rnd%> = (id) => {
        if(confirm('Apakah Anda yakin ingin menghapus data ini?')) {
            if(typeof proseshapusDataRinci === 'function') { proseshapusDataRinci(window.masterModelClass<%=rnd%>, id, function(){ loadDataGuru<%=rnd%>(); }); }
        }
    };

    const tutupForm<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataGuru<%=rnd%>();
    };

    // INIT MASTER DATA
    document.addEventListener("DOMContentLoaded", () => {
        // Init Static Selects
        const staticPilihan = {
            "jenisKelamin": [ {id: "Laki-laki", nama: "Laki-laki"}, {id: "Perempuan", nama: "Perempuan"} ],
            "statusNikah": [ {id: "Belum Kawin", nama: "Belum Kawin"}, {id: "Kawin", nama: "Kawin"}, {id: "Janda/Duda", nama: "Janda/Duda"} ],
            "bahasa": [ {id: "Indonesia", nama: "Indonesia"}, {id: "English", nama: "English"}, {id: "Arab", nama: "Arab"} ],
            "kewarganegaraan": [ {id: "WNI", nama: "WNI"}, {id: "WNA", nama: "WNA"} ]
        };
        for(let key in staticPilihan) {
            let el = document.getElementById('input_' + key + '<%=rnd%>');
            if(el) {
                let opts = '<option value="">-- Pilih --</option>';
                staticPilihan[key].forEach(opt => { opts += `<option value="${opt.id}">${opt.nama}</option>`; });
                el.innerHTML = opts;
            }
        }

        // Init Dynamic Master DB Selects
        loadMasterData<%=rnd%>("ais.database.model.Agama", "input_agama<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.sekolah.Yayasan", "input_yayasan<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.sekolah.Sekolah", "input_sekolah<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.sekolah.Sekolah", "input_sekolah1<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.sekolah.Sekolah", "input_sekolah2<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.sekolah.Sekolah", "input_sekolah3<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.Negara", "input_negara<%=rnd%>", "id", "namaNegara");
        loadMasterData<%=rnd%>("ais.database.model.Wilayah", "input_kecamatan<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.StatusKepegawaian", "input_statusKepegawaian<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.JenisPendidikDanTenagaKependidikan", "input_jenisPendidikDanTenagaKependidikan<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.StatusPegawai", "input_statusPegawai<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.GolonganPns", "input_golonganPegawai<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.SumberGaji", "input_sumberGaji<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.LembagaPengangkat", "input_lembagaPengangkat<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.PekerjaanOrangTua", "input_pekerjaanAyah<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.PekerjaanOrangTua", "input_pekerjaanIbu<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.PekerjaanOrangTua", "input_pekerjaanSuamiIstri<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.employ.Pendidikan", "input_pendidikan<%=rnd%>");
        loadMasterData<%=rnd%>("ais.database.model.sekolah.JenisGuru", "input_jenisGuru<%=rnd%>");

        loadDataGuru<%=rnd%>();
    });
</script>