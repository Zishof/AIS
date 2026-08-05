<%@page import="ais.common.Common"%>
<%@page import="java.util.Random"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // Deklarasi variabel di luar blok try agar bisa diakses di seluruh halaman
    String clazz = "";
    String jenis = "";
    String kondisiTambahan = "";
    String col = "";
    String tampilUpload = "";
    String afterSave = "";
    String beforeSave = "";
    String rnd = "";
    String accept = "";
    String label = "";
    String tanpaLogin = "";
    String id = "";
    String actionUrl = "";
    String localRand = Common.getGeneratedBarCode(7);
    String loadPreview = "";
    try {
        // 1. Ambil & Validasi Parameter
        clazz = request.getParameter("clazz");
        jenis = request.getParameter("jenis");
        col = request.getParameter("col") == null ? "" : request.getParameter("col");
        tampilUpload = request.getParameter("tampilUpload") == null ? "true" : request.getParameter("tampilUpload");
        loadPreview = request.getParameter("loadPreview") == null ? "true" : request.getParameter("loadPreview");
        
        
        
        afterSave = request.getParameter("afterSave") == null ? "" : request.getParameter("afterSave").trim();
        beforeSave = request.getParameter("beforeSave") == null ? "" : request.getParameter("beforeSave").trim();
        kondisiTambahan = request.getParameter("kondisiTambahan") == null ? "" : request.getParameter("kondisiTambahan").trim();
        rnd = request.getParameter("rnd") == null ? String.valueOf(new Random().nextInt(999999)) : request.getParameter("rnd");

        if (clazz == null || clazz.trim().isEmpty() || jenis == null || jenis.trim().isEmpty()) {
            out.println("<div class=\"alert alert-danger shadow-sm\" role=\"alert\">");
            out.println("<strong>" + Common.getBahasaConfig("Error Developer:") + "</strong> " + Common.getBahasaConfig("Parameter clazz dan jenis wajib disertakan!"));
            out.println("</div>");
            return; 
        }

        // 2. Setup Parameter Lanjutan
        accept = request.getParameter("accept") == null ? "" : request.getParameter("accept");
        label = request.getParameter("label") == null ? "" : request.getParameter("label");
        tanpaLogin = request.getParameter("tanpaLogin") == null ? "false" : request.getParameter("tanpaLogin");
        id = request.getParameter("id"); 
        
        // URL Servlet Upload
        actionUrl = request.getContextPath() + "/DoUpload"; 
        
        
        System.out.println("id -> "+id+", clazz -> "+clazz+", jenis -> "+jenis);
%>

<div id="upload-container-<%=rnd%>" class="form-group mb-4">

	<div id="preview-area-<%=rnd%>" class="d-flex justify-content-center mt-2" style="display: none;"></div>
    
    <div class="input-group shadow-sm rounded flex-nowrap justify-content-center mb-3">
        <%if(tampilUpload.equalsIgnoreCase("true")){ %>
            <input id="input_dofile_<%=col %>_<%=rnd%>" type="file" <%=accept%> style="display: none;" onchange="uploadFile<%=rnd%>();">
        <%} %>
        
        <input type="text" id="fileNameDisplay<%=rnd%>" class="form-control bg-light text-truncate" style="display: none;" readonly value="<%=Common.getBahasaConfig("Belum ada file dipilih") %>...">
        
        <input type="hidden" id="input_<%=col %>_<%=rnd%>" name="input_<%=col %>" value="">
        <input type="hidden" id="clazz_<%=col %>_<%=rnd%>" name="clazz_<%=col %>" value="<%=clazz%>">
        <input type="hidden" id="jenis_<%=col %>_<%=rnd%>" name="jenis_<%=col %>" value="<%=jenis%>">
        
        <div id="action-buttons-<%=rnd%>" class="d-flex"></div>

        <button type="button"
                class="btn btn-outline-secondary btn-sm d-flex align-items-center"
                title="<%=Common.getBahasaConfig("Muat ulang status file") %>"
                onclick="resetUpload<%=rnd%>(); checkExistingFile<%=rnd%>();"
                style="border-radius: 0; padding: 0.375rem 0.5rem; border-left: 1px solid #ced4da;">
            <i class="fas fa-rotate"></i>
        </button>

        <%if(tampilUpload.equalsIgnoreCase("true")){ %>
            <div class="input-group-append d-flex">
                <button type="button" class="btn btn-primary fw-bold px-3" style="border-top-left-radius: 0; border-bottom-left-radius: 0; transition: all 0.3s;" onclick="document.getElementById('input_dofile_<%=col %>_<%=rnd%>').click();">
                    <i class="fas fa-cloud-arrow-up me-1"></i> <span id="lbl-upload-<%=rnd%>"><%=Common.getBahasaConfig("Upload") %> <%=Common.getBahasaConfig(label.isEmpty() ? "File" : label) %></span>
                </button>
            </div>
        <%} %>
    </div>

    

    <div id="progress-wrapper-<%=rnd%>" class="progress mt-2 shadow-sm" style="display:none; height: 15px; border-radius: 10px;">
        <div id="progress-bar-<%=rnd%>" 
             class="progress-bar progress-bar-striped progress-bar-animated bg-primary" 
             role="progressbar" 
             style="width: 0%; transition: width 0.4s ease;">
            0%
        </div>
    </div>

    <div id="status-message-<%=rnd%>" class="mt-2"></div>
</div>

<script type="text/javascript">
    function resetUpload<%=rnd%>() {
        $('#status-message-<%=rnd%>').html('');
        $('#preview-area-<%=rnd%>').hide().html(''); 
        $('#fileNameDisplay<%=rnd%>').val('<%=Common.getBahasaConfigJS("Belum ada file dipilih") %>...');
        $('#input_<%=col %>_<%=rnd%>').val('');
        
        // Bersihkan tombol dinamis dan reset label upload
        $('#action-buttons-<%=rnd%>').html('');
        $('#lbl-upload-<%=rnd%>').text('<%=Common.getBahasaConfigJS("Upload") %> <%=Common.getBahasaConfig(label.isEmpty() ? "File" : label) %>');
    }
    

    const subdata<%=localRand%> = new Map();
    
    function setSubdata<%=rnd%>(property, data) {
    	subdata<%=localRand%>.set(property,data);
    }
    
    function chekSubdata<%=rnd%>(property) {
    	return subdata<%=localRand%>.has(property);
    }
    
    // --- 1. Fungsi Upload File (AJAX) ---
    function uploadFile<%=rnd%>() {
    	
    	<%=beforeSave%>
    	
        var fileInput = $('#input_dofile_<%=col %>_<%=rnd%>')[0];
        if (fileInput.files.length === 0) return;

        var file = fileInput.files[0];
        $('#fileNameDisplay<%=rnd%>').val(file.name);

        resetUpload<%=rnd%>();
        $('#fileNameDisplay<%=rnd%>').val(file.name); // re-set after reset
        $('#progress-wrapper-<%=rnd%>').fadeIn();
        $('#progress-bar-<%=rnd%>').css('width', '0%').text('0%').removeClass('bg-success bg-danger').addClass('bg-primary progress-bar-animated');

        
        const objectdata = JSON.stringify(Object.fromEntries(subdata<%=localRand%>));
        //alert(objectdata);
        var formData = new FormData();
        formData.append('nama', file.name);
        formData.append('clazz', '<%=clazz%>');
        formData.append('subdata', objectdata);
        formData.append('jenis', '<%=jenis%>');
        formData.append('id', '<%=id != null ? id : "" %>');
        formData.append('tanpaLogin', '<%=tanpaLogin%>');
        formData.append('fileContent', file);

        $.ajax({
            url: '<%=actionUrl%>',
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
                $('#progress-bar-<%=rnd%>').css('width', '100%').text('<%=Common.getBahasaConfigJS("Upload Selesai")%>');

                setTimeout(function(){ $('#progress-wrapper-<%=rnd%>').fadeOut(); }, 1500);
				console.log("response -> ", response);
                if(response.status === 'Sukses' || response.status === '00'){
                   $('#input_<%=col %>_<%=rnd%>').val(response.id);
                   generatePreview<%=rnd%>(response);
                   <%=afterSave%>
                } else if(response.status === 'Menunggu') {
                   showPending<%=rnd%>(response.keterangan || '<%=Common.getBahasaConfigJS("File diterima, sedang diproses...")%>');
                } else {
                   showError<%=rnd%>(response.keterangan || '<%=Common.getBahasaConfigJS("Gagal Upload")%>');
                }
            },
            error: function(jqXHR, textStatus, errorThrown) {
                showError<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi:")%> ' + textStatus);
            }
        });
    }

    // --- 2. Fungsi Cek File Existing (API Fetch) ---
    async function checkExistingFile<%=rnd%>() {
        var reqObj = { 
            "action": "file", 
            "class": "<%=clazz%>", 
            "ref": "<%=id != null ? id : "" %>", 
            "jenis": "<%=jenis%>",
            "kondisiTambahan": "<%=kondisiTambahan%>",
            "refresh": "<%=!kondisiTambahan.trim().isEmpty()%>",
            "tanpaLogin": "true", 
            "usingId": false
        };

        const servletUrl = '<%=Common.ROOT%>/Data';
        
        try {
            const response = await fetch(servletUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(reqObj)
            });
            const dataResponse = await response.json();

            if(dataResponse.status == '00' && dataResponse.data){
                var fileData = dataResponse.data;
                $('#fileNameDisplay<%=rnd%>').val(fileData.nama);
                generatePreview<%=rnd%>(fileData);
            } else {
                // File tidak ditemukan di server — bersihkan tampilan yang mungkin stale
                resetUpload<%=rnd%>();
            }
        } catch (error) {
            console.error("<%=Common.getBahasaConfig("Gagal mengambil info file:")%>", error);
        }
    }

    // --- 3. Fungsi Generate Preview HTML & Barisan Tombol ---
    function generatePreview<%=rnd%>(data) {
        var url = data.url;
        var filename = data.nama;
        var ext = filename ? filename.split('.').pop().toLowerCase() : '';
        
        document.getElementById("input_<%=col %>_<%=rnd%>").value = data.id;
        
        // ------------------------------------------------------------------
        // A. Inject Tombol Download & Hapus agar Sejajar dengan tombol Upload
        // ------------------------------------------------------------------
        var btnDownload = '<a href="' + url + '" target="_blank" class="btn btn-success fw-bold px-3 d-flex align-items-center" style="border-radius: 0; border-left: 1px solid rgba(255,255,255,0.4);"><i class="fas fa-download me-1"></i> <span class="d-none d-md-inline"><%=Common.getBahasaConfig("Download")%></span></a>';
        
        var btnDelete = '';
        <%if(id != null && tampilUpload.equalsIgnoreCase("true")){ %>
            btnDelete = '<button type="button" class="btn btn-danger fw-bold px-3 d-flex align-items-center" style="border-radius: 0; border-left: 1px solid rgba(255,255,255,0.4);" onclick="prosesDeleteDataFileById(\'<%=clazz %>\',\'<%=jenis %>\', ' + data.id + ', function(){ resetUpload<%=rnd%>(); <%=afterSave%> });"><i class="fas fa-trash-can me-1"></i> <span class="d-none d-md-inline"><%=Common.getBahasaConfig("Hapus")%></span></button>';
        <%} %>
        
        $('#action-buttons-<%=rnd%>').html(btnDownload + btnDelete);
        if (typeof window.pmbEnsureFontAwesome === 'function') { window.pmbEnsureFontAwesome(document.getElementById('action-buttons-<%=rnd%>')); }
        
        // Ubah teks tombol utama dari "Upload" menjadi "Ganti File"
        $('#lbl-upload-<%=rnd%>').text('<%=Common.getBahasaConfigJS("Ganti") %> <%=Common.getBahasaConfig(label.isEmpty() ? "File" : label) %>');


        // ------------------------------------------------------------------
        // B. Generate Area Preview Media (Bersih, Tanpa Tombol)
        // ------------------------------------------------------------------
        var container = $('#preview-area-<%=rnd%>');
        var html = '';

        if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].indexOf(ext) !== -1) {
            html = '<a href="' + url + '" target="_blank" title="<%=Common.getBahasaConfig("Klik untuk memperbesar")%>">' +
                   '  <img src="' + url + '" class="img-fluid rounded shadow-sm" style="max-height: 250px; object-fit: cover; border: 2px solid #ddd;" alt="' + filename + '">' +
                   '</a>';

        } else if (['mp3', 'wav', 'ogg', 'm4a'].indexOf(ext) !== -1) {
            html = '<audio controls class="w-100 rounded shadow-sm border border-info" style="max-width: 500px;">' +
                   '  <source src="' + url + '" type="audio/' + (ext === 'mp3' ? 'mpeg' : ext) + '">' +
                   '  <%=Common.getBahasaConfig("Browser anda tidak mendukung element audio.")%>' +
                   '</audio>';

        } else if (['mp4', 'webm', 'mov'].indexOf(ext) !== -1) {
            html = '<div class="ratio ratio-16x9 shadow-sm rounded overflow-hidden border border-dark" style="max-width: 500px;">' +
                   '  <video controls class="w-100">' +
                   '    <source src="' + url + '" type="video/' + ext + '">' +
                   '    <%=Common.getBahasaConfig("Browser anda tidak mendukung tag video.")%>' +
                   '  </video>' +
                   '</div>';

        } else {
            // Tampilan Icon Besar untuk tipe Dokumen (karena namanya sudah ada di input text)
            var iconClass = 'fa-file-lines text-secondary';
            if(ext === 'pdf') { iconClass = 'fa-file-pdf text-danger'; }
            else if(['xls','xlsx','csv'].indexOf(ext) !== -1) { iconClass = 'fa-file-excel text-success'; }
            else if(['doc','docx'].indexOf(ext) !== -1) { iconClass = 'fa-file-word text-primary'; }
            else if(['zip','rar','7z'].indexOf(ext) !== -1) { iconClass = 'fa-file-zipper text-warning'; }

            html = '<a href="' + url + '" target="_blank" class="text-decoration-none" title="<%=Common.getBahasaConfig("Buka / Download")%>">' +
                   '  <i class="fas ' + iconClass + ' fa-4x p-3 bg-white rounded shadow-sm border"></i>' +
                   '</a>';
        }

        container.html(html).fadeIn();
        if (typeof window.pmbEnsureFontAwesome === 'function') { window.pmbEnsureFontAwesome(container[0]); }
    }

    // --- 4. Fungsi Show Error ---
    function showError<%=rnd%>(message) {
        $('#progress-bar-<%=rnd%>').removeClass('bg-primary bg-success progress-bar-animated').addClass('bg-danger');
        var msg = '<div class="alert alert-danger alert-dismissible fade show shadow-sm" role="alert">' +
                  '  <i class="fas fa-circle-exclamation me-2"></i><strong><%=Common.getBahasaConfig("Gagal!")%></strong> ' + message +
                  '  <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>' +
                  '</div>';
        $('#status-message-<%=rnd%>').html(msg);
        if (typeof window.pmbEnsureFontAwesome === 'function') { window.pmbEnsureFontAwesome(document.getElementById('status-message-<%=rnd%>')); }
    }

    // --- 5. Fungsi Show Pending (file diterima, DB sedang retry) ---
    function showPending<%=rnd%>(message) {
        $('#progress-bar-<%=rnd%>').removeClass('bg-primary bg-danger progress-bar-animated').addClass('bg-warning');
        var msg = '<div class="alert alert-warning alert-dismissible fade show shadow-sm" role="alert">' +
                  '  <i class="fas fa-hourglass-half me-2"></i><strong><%=Common.getBahasaConfig("Menunggu")%></strong> ' + message +
                  '  <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>' +
                  '</div>';
        $('#status-message-<%=rnd%>').html(msg);
        if (typeof window.pmbEnsureFontAwesome === 'function') { window.pmbEnsureFontAwesome(document.getElementById('status-message-<%=rnd%>')); }
    }

    <%if(loadPreview.equalsIgnoreCase("true")){ %>
    $(document).ready(function() {
        checkExistingFile<%=rnd%>();
    });
    <% } %>
</script>

<%
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/common/upload_component.jsp:331");
        out.println("<div class=\"alert alert-danger\"><strong>" + Common.getBahasaConfig("Terjadi kesalahan sistem") + ":</strong> " + e.getMessage() + "</div>");
    } finally {
        /*
         * Area penutupan resource (database connection, streams, dll).
         * Pastikan semua resource ditutup dengan try-catch bersarang untuk mencegah kebocoran memori.
         */
    }
%>