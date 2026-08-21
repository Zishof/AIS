<%@page import="java.util.Enumeration"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@ page import="ais.common.Common" %>
<%@ page import="java.util.Random" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.ArrayList" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
    private String escapeTextAreaAiJs(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("</script>", "<\\/script>");
    }
%>
<%
    String rnd = Common.getGeneratedBarCode(7);
    
    // --- Konfigurasi Text Editor ---
    String placeholder = request.getParameter("placeholder") == null ? "Tuliskan detail di sini..." : request.getParameter("placeholder").trim();
    String idtextarea = request.getParameter("idtextarea") == null ? "id_text_" + rnd : request.getParameter("idtextarea").trim();
    String namatextarea = request.getParameter("namatextarea") == null ? "nama_text_" + rnd : request.getParameter("namatextarea").trim();
    String isi = request.getParameter("isi") == null ? "" : request.getParameter("isi").trim();
    Boolean isRequired = request.getParameter("isRequired") == null ? false : Boolean.parseBoolean(request.getParameter("isRequired").trim());
    Boolean isGenerate = request.getParameter("isGenerate") == null ? true : Boolean.parseBoolean(request.getParameter("isGenerate").trim());

    // --- Konfigurasi AI via Servlet Lokal ---
    // Frontend sekarang memanggil servlet /Ai agar tidak terkena CORS dari browser.
    // Servlet AiGenerateServlet yang akan meneruskan request ke OpenAI-compatible endpoint/Ollama.
    String aiServletUrl = request.getParameter("aiServletUrl") == null ? (request.getContextPath() + "/Ai") : request.getParameter("aiServletUrl").trim();
    String aiModel = request.getParameter("aiModel") == null ? "qwen2.5:7b" : request.getParameter("aiModel").trim();
    String aiDefaultInstruction = request.getParameter("aiDefaultInstruction") == null ? "Buatkan teks yang rapi, formal, mudah dipahami, dan siap ditempel ke editor." : request.getParameter("aiDefaultInstruction").trim();
    // --- Konfigurasi Upload ---
    String uploadClazz = request.getParameter("clazz") == null ? LampiranLain.class.getName() : request.getParameter("clazz");
    String uploadJenis = request.getParameter("jenis") == null ? "FILES" : request.getParameter("jenis");
    String uploadId    = request.getParameter("id") == null ? "" : request.getParameter("id");
    String actionUrl   = request.getContextPath() + "/DoUpload";
    
    String onchange = request.getParameter("onchange") == null ? "" : request.getParameter("onchange").trim();
    
%>

<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" crossorigin="" />

<script data-cfasync="false" src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" crossorigin=""></script>

<style>
    /* Map Picker Styling */
	    #map_canvas_<%=rnd%> { height: 300px; width: 100%; border: 1px solid #ccc; border-radius: 4px; }
	
	    /* Progress bar */
	    .upload-progress-container { display: none; height: 5px; width: 100%; background-color: #f0f0f0; margin-top: -5px; margin-bottom: 5px; }
	    .upload-progress-bar { height: 100%; width: 0%; background-color: #28a745; transition: width 0.3s; }
	    
	    /* Screen/Camera Preview */
	    #screen_preview_container_<%=rnd%> video, #camera_preview_container_<%=rnd%> video { width: 100%; max-height: 400px; background: #000; border: 1px solid #ccc; transform: scaleX(-1); }
	    #screen_preview_container_<%=rnd%> video { transform: none; }

        /* Modern Text Area Editor */
        .textarea-action-bar-<%=rnd%> {
            padding: 10px;
            border: 1px solid #e5e7eb;
            border-radius: 14px;
            background: linear-gradient(135deg, #f8fafc 0%, #eef6ff 100%);
            box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
        }
        .editor-action-btn-<%=rnd%> {
            border-radius: 999px !important;
            padding: 6px 11px !important;
            transition: all .18s ease-in-out;
            background: #ffffff !important;
            color: #334155 !important;
        }
        .editor-action-btn-<%=rnd%>:hover {
            transform: translateY(-1px);
            box-shadow: 0 6px 14px rgba(15, 23, 42, .10);
            background: #f8fafc !important;
        }
        .text-editor-shell-<%=rnd%> {
            border: 1px solid #e5e7eb !important;
            border-radius: 16px !important;
            overflow: hidden;
            box-shadow: 0 14px 34px rgba(15, 23, 42, 0.08) !important;
        }
        .editor-toolbar-<%=rnd%> {
            background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%) !important;
            border-bottom: 1px solid #e5e7eb !important;
            gap: 6px;
        }
        .editor-toolbar-<%=rnd%> .btn-group,
        .editor-toolbar-<%=rnd%> .input-group {
            border: 1px solid #eef2f7;
            border-radius: 10px;
            overflow: hidden;
        }
        .editor-toolbar-<%=rnd%> .btn:hover {
            background: #eef6ff !important;
            color: #0d6efd !important;
        }
        #visual_<%= idtextarea %>_<%=rnd %>.editor-container {
            min-height: 220px;
            max-height: 620px;
            overflow-y: auto;
            line-height: 1.65;
            color: #1f2937;
            background: #ffffff;
        }
        #visual_<%= idtextarea %>_<%=rnd %>.editor-container:focus {
            outline: none;
            box-shadow: inset 0 0 0 2px rgba(13, 110, 253, .12);
        }
        .editor-image-selected { outline: 3px solid #0d6efd; outline-offset: 2px; border-radius: 8px; }

        /* AI Text Generator Modern UI */
        .ai-generator-card-<%=rnd%> {
            border: 1px solid #dbe7ff;
            border-radius: 18px;
            background: radial-gradient(circle at top left, rgba(13,110,253,.10), transparent 30%), linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
            box-shadow: 0 12px 28px rgba(37, 99, 235, .08);
        }
        .ai-generator-help-<%=rnd%> { font-size: 12px; color: #64748b; }
        .ai-result-box-<%=rnd%> {
            min-height: 190px;
            font-family: inherit;
            line-height: 1.6;
            border-radius: 14px;
            border-color: #dbeafe;
            background: #fbfdff;
        }
        .ai-status-<%=rnd%> { display: none; border-radius: 12px; padding: 10px 12px; font-size: 13px; border: 1px solid transparent; }
        .ai-status-<%=rnd%>.show { display: block; }
        .ai-modal-header-<%=rnd%> {
            background: linear-gradient(135deg, #0d6efd 0%, #20c997 100%);
            color: #ffffff;
            border-bottom: 0;
        }
        .ai-modal-header-<%=rnd%> small { color: rgba(255,255,255,.85) !important; }
        .ai-quick-btn-<%=rnd%> {
            border-radius: 999px;
            border: 1px solid #dbeafe;
            background: #ffffff;
            color: #0f172a;
            padding: 5px 10px;
            font-size: 12px;
            margin: 0 4px 6px 0;
        }
        .ai-quick-btn-<%=rnd%>:hover { background: #eef6ff; color: #0d6efd; }
        .ai-loading-overlay-<%=rnd%> {
            display: none;
            position: absolute;
            inset: 0;
            background: rgba(255,255,255,.72);
            z-index: 20;
            align-items: center;
            justify-content: center;
            backdrop-filter: blur(2px);
        }
        .ai-loading-overlay-<%=rnd%>.show { display: flex; }
        .link-guide-item { border: 1px solid #edf2f7; border-radius: 12px; padding: 10px; margin-bottom: 10px; background: #fff; }
        .link-guide-title { display: block; font-weight: 700; margin-bottom: 4px; }
        .link-guide-code { display: block; font-family: monospace; font-size: 11px; color: #64748b; word-break: break-all; }
</style>

<input type="file" id="input_file_<%=rnd%>" style="display: none;" onchange="handleFileInputChange<%=rnd%>()">

<div class="textarea-action-bar-<%=rnd%> d-flex flex-wrap gap-2 mb-3">
    <button type="button" class="btn btn-sm btn-light border editor-action-btn-<%=rnd%>" onclick="triggerUpload<%=rnd %>('image')">
        <i class="fa fa-image text-success me-1"></i> <%= Common.getBahasaConfig("Gambar") %>
    </button>
    <button type="button" class="btn btn-sm btn-light border editor-action-btn-<%=rnd%>" onclick="textAreaToast<%=rnd%>('<%= Common.getBahasaConfigJS("Fitur SCORM belum tersedia") %>', 'bg-warning')">
        <i class="fa fa-cube text-primary me-1"></i> <%= Common.getBahasaConfig("SCORM") %>
    </button>
    <button type="button" class="btn btn-sm btn-light border editor-action-btn-<%=rnd%>" onclick="openCameraModal<%=rnd %>()">
        <i class="fa fa-camera text-dark me-1"></i> <%= Common.getBahasaConfig("Kamera") %>
    </button>
    <button type="button" class="btn btn-sm btn-light border editor-action-btn-<%=rnd%>" onclick="openScreenModal<%=rnd %>()">
        <i class="fa fa-desktop text-info me-1"></i> <%= Common.getBahasaConfig("Layar") %>
    </button>
    <button type="button" class="btn btn-sm btn-light border editor-action-btn-<%=rnd%>" onclick="triggerUpload<%=rnd %>('audio')">
        <i class="fa fa-microphone text-warning me-1"></i> <%= Common.getBahasaConfig("Audio") %>
    </button>
    <button type="button" class="btn btn-sm btn-light border editor-action-btn-<%=rnd%>" onclick="triggerUpload<%=rnd %>('video')">
        <i class="fa fa-film text-danger me-1"></i> <%= Common.getBahasaConfig("Video") %>
    </button>
    <button type="button" class="btn btn-sm btn-light border editor-action-btn-<%=rnd%>" onclick="openLinkModal<%=rnd %>()">
        <i class="fa fa-link text-secondary me-1"></i> <%= Common.getBahasaConfig("Link") %>
    </button>
    <button type="button" class="btn btn-sm btn-light border editor-action-btn-<%=rnd%>" onclick="openMapModal<%=rnd %>()">
        <i class="fa fa-map-marker-alt text-danger me-1"></i> <%= Common.getBahasaConfig("Map") %>
    </button>
    <%
    if(isGenerate){
    %>
    <button type="button" class="btn btn-sm btn-light border editor-action-btn-<%=rnd%>" onclick="bukaModalAi<%=rnd %>()">
	    <i class="fa fa-magic text-info me-1"></i>
	    <%= Common.getBahasaConfig("Generate") %>
	</button>
	<%
    }
	%>
</div>

<div class="text-editor-shell-<%=rnd%> border rounded shadow-sm bg-white position-relative">
    <div class="editor-toolbar-<%=rnd%> bg-light border-bottom p-2 d-flex flex-wrap gap-1 align-items-center">
        <div class="btn-group btn-group-sm bg-white shadow-sm me-2">
            <button type="button" class="btn btn-outline-secondary border-0" onclick="toggleSource<%=rnd %>()" title="Source"><i class="fa fa-code"></i> <%= Common.getBahasaConfig("Source") %></button>
        </div>
        <div class="btn-group btn-group-sm bg-white shadow-sm me-2">
            <button type="button" class="btn btn-outline-secondary border-0" onclick="saveContent<%=rnd %>()"><i class="fa fa-save"></i></button>
            <button type="button" class="btn btn-outline-secondary border-0" onclick="printContent<%=rnd %>()"><i class="fa fa-print"></i></button>
        </div>
        <div class="btn-group btn-group-sm bg-white shadow-sm me-2">
            <button type="button" class="btn btn-outline-secondary border-0" onclick="formatDoc<%=rnd %>('bold')"><i class="fa fa-bold"></i></button>
            <button type="button" class="btn btn-outline-secondary border-0" onclick="formatDoc<%=rnd %>('italic')"><i class="fa fa-italic"></i></button>
            <button type="button" class="btn btn-outline-secondary border-0" onclick="formatDoc<%=rnd %>('underline')"><i class="fa fa-underline"></i></button>
            <button type="button" class="btn btn-outline-secondary border-0" onclick="formatDoc<%=rnd %>('strikeThrough')"><i class="fa fa-strikethrough"></i></button>
        </div>
        <div class="input-group input-group-sm shadow-sm me-2" style="width: 120px;">
            <select onchange="formatDoc<%=rnd %>('fontName', this.value); this.value='';" class="form-select border-0">
                <option value="" selected><%= Common.getBahasaConfig("Font") %></option>
                <option value="Arial">Arial</option>
                <option value="Courier">Courier</option>
                <option value="Times New Roman">Times New Roman</option>
            </select>
        </div>
        <div class="btn-group btn-group-sm bg-white shadow-sm me-2">
            <button type="button" class="btn btn-outline-secondary border-0" onclick="formatDoc<%=rnd %>('insertUnorderedList')"><i class="fa fa-list-ul"></i></button>
            <button type="button" class="btn btn-outline-secondary border-0" onclick="formatDoc<%=rnd %>('insertOrderedList')"><i class="fa fa-list-ol"></i></button>
        </div>
        <div class="btn-group btn-group-sm bg-white shadow-sm">
            <button type="button" class="btn btn-outline-secondary border-0" onclick="formatDoc<%=rnd %>('justifyLeft')"><i class="fa fa-align-left"></i></button>
            <button type="button" class="btn btn-outline-secondary border-0" onclick="formatDoc<%=rnd %>('justifyCenter')"><i class="fa fa-align-center"></i></button>
            <button type="button" class="btn btn-outline-secondary border-0" onclick="formatDoc<%=rnd %>('justifyRight')"><i class="fa fa-align-right"></i></button>
        </div>
    </div>
    
    <div id="progress-wrapper-<%=rnd%>" class="upload-progress-container">
        <div id="progress-bar-<%=rnd%>" class="upload-progress-bar"></div>
    </div>
    
    <div id="visual_<%= idtextarea %>_<%=rnd %>" onblur="handleOnChange<%=rnd %>()" class="form-control border-0 p-3 editor-container" contenteditable="true"
         oninput="syncTextarea<%=rnd %>()" onmouseup="saveSelection<%=rnd%>()" onkeyup="saveSelection<%=rnd%>()" onfocus="saveSelection<%=rnd%>()">
         <% if (isi == null || isi.trim().isEmpty()) { %><p class="text-muted"><%= Common.getBahasaConfig(placeholder) %></p><% } else { %><%= isi %><% } %>
    </div>
    <textarea id="<%= idtextarea %>" name="<%= namatextarea %>" 
    <%
    Enumeration<String> dataP =  request.getParameterNames();
    while(dataP.hasMoreElements()){String pa = dataP.nextElement();if(pa.startsWith("data-")){String val = request.getParameter(pa);out.println("  "+pa+"=\""+val+"\" ");}}%>
    class="d-none" rows="8" <%=isRequired ? "required" : "" %>><%=isi %></textarea>
</div>

<script>
    const editorID<%=rnd %> = "visual_<%= idtextarea %>_<%=rnd %>";
    const textareaID<%=rnd %> = "<%= idtextarea %>";
    let savedRange<%=rnd%> = null;
    let activeUploadType<%=rnd%> = 'image'; 
    let currentEditingImage<%=rnd%> = null;

    const aiServletUrl<%=rnd%> = '<%= escapeTextAreaAiJs(aiServletUrl) %>';
    const aiModel<%=rnd%> = '<%= escapeTextAreaAiJs(aiModel) %>';
    const aiDefaultInstruction<%=rnd%> = '<%= escapeTextAreaAiJs(aiDefaultInstruction) %>';
    
    // Global Vars
    let screenStream<%=rnd%> = null, mediaRecorder<%=rnd%> = null, recordedChunks<%=rnd%> = [];
    let cameraStream<%=rnd%> = null;
    let leafletMap<%=rnd%> = null;
    let leafletMarker<%=rnd%> = null;

    function textAreaToast<%=rnd%>(message, cssClass) {
        try {
            if (typeof window.tampilkanToast === 'function') {
                window.tampilkanToast(message, cssClass || 'bg-info');
            } else if (window.toastr && window.toastr.info) {
                window.toastr.info(message);
            } else {
                console.log('[TextArea]', message);
            }
        } catch (e) {
            console.log('[TextArea]', message);
        }
    }

    function generateRandomTextTextArea<%=rnd%>(length) {
        var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        var result = '';
        for (var i = 0; i < length; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return result;
    }

    function isiPromptCepatAi<%=rnd%>(teks) {
        var prompt = $('#promptAi<%=rnd%>');
        var lama = prompt.val();
        if (lama && lama.trim().length > 0) {
            prompt.val(lama + '\n\n' + teks);
        } else {
            prompt.val(teks);
        }
        prompt.focus();
    }

    // --- Helper Elevate Backdrop (Untuk mencegah background gelap modal menimpa modal) ---
    function elevateBackdrop<%=rnd%>() {
        setTimeout(() => {
            $('.modal-backdrop').last().css('z-index', '104999');
        }, 105);
    }

    // --- Core Functions ---
    function saveSelection<%=rnd%>() {
        const editor = document.getElementById(editorID<%=rnd %>);
        const sel = window.getSelection();
        if (sel.getRangeAt && sel.rangeCount) {
            const range = sel.getRangeAt(0);
            if (editor.contains(range.commonAncestorContainer)) savedRange<%=rnd%> = range;
        }
    }
    function restoreSelection<%=rnd%>() {
        const editor = document.getElementById(editorID<%=rnd %>);
        editor.focus();
        if (savedRange<%=rnd%>) {
            const sel = window.getSelection();
            sel.removeAllRanges();
            sel.addRange(savedRange<%=rnd%>);
        }
    }
    
	function handleOnChange<%=rnd %>() {
        syncTextarea<%=rnd %>();
          <% if (onchange != null && !onchange.isEmpty()) { %>
	         try {
	             <%= onchange %>;
	         } catch (e) {
	             console.error("Error pada eksekusi onchange:", e);
	         }
	     <% } %>
    }
    
    function bukaModalAi<%=rnd%>() {
        saveSelection<%=rnd%>();

        if ($('#modalAiGenerate<%=rnd%>').length === 0) {
            const modalHtml = `
            <div class="modal fade" id="modalAiGenerate<%=rnd%>" tabindex="-1" aria-hidden="true" style="z-index: 105000;">
                <div class="modal-dialog modal-lg modal-dialog-scrollable">
                    <div class="modal-content">
                        <div class="modal-header ai-modal-header-<%=rnd%>">
                            <div>
                                <h5 class="modal-title mb-0"><i class="fa fa-robot me-2"></i>AI Text Generator</h5>
                                <small class="text-muted">Model: <strong>` + aiModel<%=rnd%> + `</strong> &nbsp; | &nbsp; Servlet: <span class="text-monospace">` + aiServletUrl<%=rnd%> + `</span></small>
                            </div>
                            <button type="button" class="close btn-close text-white" onclick="$('#modalAiGenerate<%=rnd%>').modal('hide')" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                        </div>
                        <div class="modal-body position-relative">
                            <div id="loadingAiOverlay<%=rnd%>" class="ai-loading-overlay-<%=rnd%>"><div class="text-center"><i class="fa fa-spinner fa-spin fa-2x text-primary"></i><div class="mt-2 font-weight-bold fw-bold">AI sedang memproses...</div><small class="text-muted">Mohon tunggu, model lokal bisa membutuhkan beberapa detik.</small></div></div>
                            <div class="ai-generator-card-<%=rnd%> p-3 mb-3">
                                <div class="row">
                                    <div class="col-md-5 mb-3">
                                        <label class="form-label font-weight-bold fw-bold"><%= Common.getBahasaConfig("Jenis Bantuan AI") %></label>
                                        <select id="jenisAi<%=rnd%>" class="form-control form-select">
                                            <option value="buat" selected>Buat konten baru</option>
                                            <option value="rapikan">Rapikan/perbaiki isi editor</option>
                                            <option value="ringkas">Ringkas isi editor</option>
                                            <option value="akademik">Konten akademik sekolah/kampus</option>
                                            <option value="soal">Buat soal dan jawaban</option>
                                        </select>
                                    </div>
                                    <div class="col-md-4 mb-3">
                                        <label class="form-label font-weight-bold fw-bold"><%= Common.getBahasaConfig("Kreativitas") %></label>
                                        <select id="temperaturAi<%=rnd%>" class="form-control form-select">
                                            <option value="0.2">Formal / konsisten</option>
                                            <option value="0.4" selected>Seimbang</option>
                                            <option value="0.7">Lebih kreatif</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3 mb-3 d-flex align-items-end">
                                        <div class="form-check">
                                            <input class="form-check-input" type="checkbox" id="autoInsertAi<%=rnd%>" checked>
                                            <label class="form-check-label small" for="autoInsertAi<%=rnd%>"><%= Common.getBahasaConfig("Sisipkan otomatis") %></label>
                                        </div>
                                    </div>
                                </div>
                                <div class="mb-2">
                                    <label class="form-label font-weight-bold fw-bold"><%= Common.getBahasaConfig("Masukkan Instruksi/Prompt") %></label>
                                    <textarea id="promptAi<%=rnd%>" class="form-control" rows="4" placeholder="Contoh: Buatkan ringkasan materi pertemuan tentang basis data untuk mahasiswa semester 3..."></textarea>
                                    <div class="ai-generator-help-<%=rnd%> mt-1">
                                        Tips: tuliskan tujuan, jenjang, mata pelajaran/mata kuliah, jumlah poin, gaya bahasa, dan format output yang diinginkan.
                                    </div>
                                    <div class="mt-2">
                                        <button type="button" class="ai-quick-btn-<%=rnd%>" onclick="isiPromptCepatAi<%=rnd%>('Buatkan RPS ringkas untuk mata kuliah berikut, lengkap dengan deskripsi, CPMK, materi per pertemuan, metode pembelajaran, dan penilaian.')"><i class="fa fa-graduation-cap"></i> RPS</button>
                                        <button type="button" class="ai-quick-btn-<%=rnd%>" onclick="isiPromptCepatAi<%=rnd%>('Buatkan modul ajar sekolah yang rapi, berisi tujuan pembelajaran, materi inti, aktivitas pembelajaran, asesmen, dan refleksi.')"><i class="fa fa-book"></i> Modul Ajar</button>
                                        <button type="button" class="ai-quick-btn-<%=rnd%>" onclick="isiPromptCepatAi<%=rnd%>('Buatkan 10 soal HOTS lengkap dengan kunci jawaban dan pembahasan.')"><i class="fa fa-question-circle"></i> Soal HOTS</button>
                                        <button type="button" class="ai-quick-btn-<%=rnd%>" onclick="isiPromptCepatAi<%=rnd%>('Rapikan teks ini menjadi bahasa formal, jelas, dan enak dibaca.')"><i class="fa fa-magic"></i> Rapikan</button>
                                    </div>
                                </div>
                            </div>

                            <div id="statusAi<%=rnd%>" class="ai-status-<%=rnd%> alert-info mb-3"></div>

                            <div class="mb-2 d-flex justify-content-between align-items-center">
                                <label class="form-label font-weight-bold fw-bold mb-0"><%= Common.getBahasaConfig("Hasil AI") %></label>
                                <div>
                                    <button type="button" class="btn btn-sm btn-outline-secondary" onclick="copyHasilAi<%=rnd%>()"><i class="fa fa-copy"></i> Copy</button>
                                    <button type="button" class="btn btn-sm btn-outline-warning" onclick="gantiIsiEditorDenganAi<%=rnd%>()"><i class="fa fa-sync"></i> Ganti Isi</button>
                                    <button type="button" class="btn btn-sm btn-outline-primary" onclick="sisipkanHasilAi<%=rnd%>()"><i class="fa fa-plus"></i> Sisipkan</button>
                                </div>
                            </div>
                            <textarea id="hasilAi<%=rnd%>" class="form-control ai-result-box-<%=rnd%>" rows="7" placeholder="Hasil generate akan tampil di sini..."></textarea>
                        </div>
                        <div class="modal-footer bg-light">
                            <button type="button" class="btn btn-secondary" onclick="$('#modalAiGenerate<%=rnd%>').modal('hide')"><%= Common.getBahasaConfig("Tutup") %></button>
                            <button type="button" id="btnProsesAi<%=rnd%>" class="btn btn-primary" onclick="prosesGenerateAi<%=rnd%>()">
                                <i class="fa fa-paper-plane me-1"></i> Generate Sekarang
                            </button>
                        </div>
                    </div>
                </div>
            </div>`;
            $('body').append(modalHtml);
        }

        $('#statusAi<%=rnd%>').removeClass('show alert-danger alert-warning alert-success').addClass('alert-info').html('');
        $('#modalAiGenerate<%=rnd%>').modal('show');
        elevateBackdrop<%=rnd%>();
        setTimeout(function(){ $('#promptAi<%=rnd%>').focus(); }, 300);
    }

    function setStatusAi<%=rnd%>(message, type) {
        const status = $('#statusAi<%=rnd%>');
        status.removeClass('alert-info alert-danger alert-warning alert-success');
        status.addClass(type || 'alert-info');
        status.html(message || '');
        if (message) status.addClass('show'); else status.removeClass('show');
    }

    function getEditorPlainTextAi<%=rnd%>() {
        const editor = document.getElementById(editorID<%=rnd%>);
        if (!editor) return '';
        return (editor.innerText || editor.textContent || '').replace(/\s+$/g, '').trim();
    }

    function escapeHtmlAi<%=rnd%>(text) {
        return String(text || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function aiPlainTextToEditorHtml<%=rnd%>(text) {
        let safe = escapeHtmlAi<%=rnd%>(text || '').replace(/\r\n/g, '\n').replace(/\r/g, '\n').trim();
        if (!safe) return '';

        // Konversi markdown sederhana agar hasil lebih nyaman dibaca di editor.
        safe = safe.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        safe = safe.replace(/__(.*?)__/g, '<strong>$1</strong>');
        safe = safe.replace(/\*(.*?)\*/g, '<em>$1</em>');

        const blocks = safe.split(/\n{2,}/);
        let html = '';
        for (let i = 0; i < blocks.length; i++) {
            let block = blocks[i].replace(/\n/g, '<br>');
            if (/^(###|##|#)\s+/.test(block)) {
                block = block.replace(/^(###|##|#)\s+/, '');
                html += '<h4>' + block + '</h4>';
            } else {
                html += '<p>' + block + '</p>';
            }
        }
        return html + '<p><br></p>';
    }

    function buildPromptAi<%=rnd%>(promptUser, jenis, editorText) {
        let instruksiMode = '';
        if (jenis === 'rapikan') {
            instruksiMode = 'Rapikan dan perbaiki teks editor berikut. Pertahankan makna asli, gunakan bahasa Indonesia formal, susun paragraf yang rapi, dan jangan menambahkan data yang tidak ada.';
        } else if (jenis === 'ringkas') {
            instruksiMode = 'Ringkas isi editor berikut menjadi versi yang lebih padat, jelas, dan formal. Sertakan poin-poin penting.';
        } else if (jenis === 'akademik') {
            instruksiMode = 'Buat konten akademik untuk kebutuhan sekolah atau perguruan tinggi. Gunakan bahasa Indonesia formal, struktur rapi, dan istilah pendidikan yang tepat.';
        } else if (jenis === 'soal') {
            instruksiMode = 'Buat soal, kunci jawaban, dan pembahasan. Jika tidak disebutkan jumlah soal, buat 5 soal. Gunakan bahasa Indonesia formal.';
        } else {
            instruksiMode = aiDefaultInstruction<%=rnd%>;
        }

        let finalPrompt = 'Anda adalah asisten akademik dan penulis profesional untuk sekolah/perguruan tinggi di Indonesia.\n';
        finalPrompt += 'Jawab dalam Bahasa Indonesia yang rapi, formal, dan siap ditempel ke editor.\n';
        finalPrompt += 'Jangan menampilkan catatan proses berpikir.\n\n';
        finalPrompt += 'Instruksi mode:\n' + instruksiMode + '\n\n';
        finalPrompt += 'Instruksi pengguna:\n' + promptUser + '\n\n';

        if ((jenis === 'rapikan' || jenis === 'ringkas') && editorText) {
            finalPrompt += 'Isi editor saat ini:\n' + editorText + '\n\n';
        } else if (editorText) {
            finalPrompt += 'Konteks tambahan dari editor saat ini, gunakan hanya jika relevan:\n' + editorText.substring(0, 8000) + '\n\n';
        }

        finalPrompt += 'Formatkan jawaban dengan paragraf/poin yang jelas. Jangan gunakan HTML.';
        return finalPrompt;
    }

    function prosesGenerateAi<%=rnd%>() {
        const prompt = $('#promptAi<%=rnd%>').val().trim();
        const jenis = $('#jenisAi<%=rnd%>').val();
        const temperature = parseFloat($('#temperaturAi<%=rnd%>').val() || '0.4');
        const btn = $('#btnProsesAi<%=rnd%>');
        const editorText = getEditorPlainTextAi<%=rnd%>();

        if (!prompt && jenis !== 'rapikan' && jenis !== 'ringkas') {
            textAreaToast<%=rnd%>('Silakan masukkan instruksi terlebih dahulu.', 'bg-warning');
            return;
        }
        if ((jenis === 'rapikan' || jenis === 'ringkas') && !editorText) {
            textAreaToast<%=rnd%>('Isi editor masih kosong, tidak ada teks yang dapat diproses.', 'bg-warning');
            return;
        }

        const fullPrompt = buildPromptAi<%=rnd%>(prompt, jenis, editorText);
        const payload = {
            instruksi: fullPrompt,
            prompt: fullPrompt,
            model: aiModel<%=rnd%>,
            temperature: temperature,
            jenis: jenis,
            source: 'text_area.jsp'
        };

        btn.prop('disabled', true).html('<i class="fa fa-spinner fa-spin"></i> Menghubungi AI...');
        $('#loadingAiOverlay<%=rnd%>').addClass('show');
        setStatusAi<%=rnd%>('<i class="fa fa-spinner fa-spin"></i> Menghubungi servlet AI <strong>' + aiServletUrl<%=rnd%> + '</strong> dengan model <strong>' + aiModel<%=rnd%> + '</strong>...', 'alert-info');

        $.ajax({
            url: aiServletUrl<%=rnd%>,
            type: 'POST',
            data: JSON.stringify(payload),
            contentType: 'application/json; charset=UTF-8',
            dataType: 'json',
            timeout: 240000,
            success: function(data) {
                const generated = (data && (data.text || data.response || data.message)) ? (data.text || data.response || data.message) : '';

                if (!data || data.success === false) {
                    const err = data && (data.error || data.raw) ? (data.error || data.raw) : 'Response AI gagal atau tidak dikenali.';
                    throw new Error(err);
                }

                if (!generated) {
                    throw new Error('Response AI kosong atau format response tidak dikenali.');
                }

                $('#hasilAi<%=rnd%>').val(generated);
                setStatusAi<%=rnd%>('<i class="fa fa-check-circle"></i> Generate berhasil. Hasil sudah diterima dari AI lokal melalui servlet.', 'alert-success');

                if ($('#autoInsertAi<%=rnd%>').is(':checked')) {
                    sisipkanHasilAi<%=rnd%>();
                }
            },
            error: function(xhr, textStatus, errorThrown) {
                console.error('AI Generate Error:', xhr, textStatus, errorThrown);

                let detail = '';
                try {
                    if (xhr && xhr.responseJSON) {
                        detail = xhr.responseJSON.error || xhr.responseJSON.raw || JSON.stringify(xhr.responseJSON);
                    } else if (xhr && xhr.responseText) {
                        detail = xhr.responseText;
                    } else {
                        detail = String(errorThrown || textStatus || 'Unknown error');
                    }
                } catch (e) {
                    detail = String(errorThrown || textStatus || 'Unknown error');
                }

                let pesan = 'Gagal menghubungi servlet AI lokal. Pastikan mapping /Ai aktif, AiGenerateServlet berhasil compile, model ' + aiModel<%=rnd%> + ' tersedia, dan servlet bisa mengakses endpoint OpenAI-compatible/Ollama.';
                setStatusAi<%=rnd%>('<i class="fa fa-exclamation-triangle"></i> ' + escapeHtmlAi<%=rnd%>(pesan) + '<br><small>' + escapeHtmlAi<%=rnd%>(detail) + '</small>', 'alert-danger');
                textAreaToast<%=rnd%>('Terjadi kesalahan saat menghubungi servlet AI.', 'bg-warning');
            },
            complete: function() {
                btn.prop('disabled', false).html('<i class="fa fa-paper-plane me-1"></i> Generate Sekarang');
                $('#loadingAiOverlay<%=rnd%>').removeClass('show');
            }
        });
    }

    function sisipkanHasilAi<%=rnd%>() {
        const hasil = $('#hasilAi<%=rnd%>').val();
        if (!hasil || !hasil.trim()) {
            textAreaToast<%=rnd%>('Belum ada hasil AI yang dapat disisipkan.', 'bg-warning');
            return;
        }

        restoreSelection<%=rnd%>();
        document.execCommand('insertHTML', false, aiPlainTextToEditorHtml<%=rnd%>(hasil));
        handleOnChange<%=rnd %>();
        textAreaToast<%=rnd%>('Hasil AI berhasil disisipkan ke editor.', 'bg-success');
    }

    function gantiIsiEditorDenganAi<%=rnd%>() {
        const hasil = $('#hasilAi<%=rnd%>').val();
        if (!hasil || !hasil.trim()) {
            textAreaToast<%=rnd%>('Belum ada hasil AI untuk mengganti isi editor.', 'bg-warning');
            return;
        }
        if (!confirm('Ganti seluruh isi editor dengan hasil AI?')) {
            return;
        }
        document.getElementById(editorID<%=rnd%>).innerHTML = aiPlainTextToEditorHtml<%=rnd%>(hasil);
        handleOnChange<%=rnd %>();
        textAreaToast<%=rnd%>('Isi editor berhasil diganti dengan hasil AI.', 'bg-success');
    }

    function copyHasilAi<%=rnd%>() {
        const hasil = $('#hasilAi<%=rnd%>').val();
        if (!hasil || !hasil.trim()) {
            textAreaToast<%=rnd%>('Belum ada hasil AI untuk disalin.', 'bg-warning');
            return;
        }
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(hasil).then(function(){
                textAreaToast<%=rnd%>('Hasil AI berhasil disalin.', 'bg-success');
            }).catch(function(){
                $('#hasilAi<%=rnd%>').select();
                document.execCommand('copy');
                textAreaToast<%=rnd%>('Hasil AI berhasil disalin.', 'bg-success');
            });
        } else {
            $('#hasilAi<%=rnd%>').select();
            document.execCommand('copy');
            textAreaToast<%=rnd%>('Hasil AI berhasil disalin.', 'bg-success');
        }
    }
    
    // --- FITUR MAP (LEAFLET PICKER -> GOOGLE EMBED) ---
    function openMapModal<%=rnd %>() {
        saveSelection<%=rnd%>();
        if ($("#modalMap_<%=rnd%>").length === 0) {
            var modalHTML = `
                <div class="modal fade" id="modalMap_<%=rnd%>" tabindex="-1" role="dialog" aria-hidden="true" style="z-index: 105000;">
                    <div class="modal-dialog modal-lg" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title"><%= Common.getBahasaConfig("Pilih Lokasi & Embed Map") %></h5>
                                <button type="button" class="close btn-close" onclick="$('#modalMap_<%=rnd%>').modal('hide')" aria-label="Close"><span aria-hidden="true">×</span></button>
                            </div>
                            <div class="modal-body">
                                <div class="alert alert-info py-2 small">
                                    <i class="fa fa-info-circle"></i> Geser marker (penanda) biru di peta untuk menentukan lokasi.
                                </div>
                                <div id="map_canvas_<%=rnd%>" class="mb-3"></div>
                                <div class="row">
                                    <div class="col-md-6">
                                        <div class="form-group">
                                            <label class="small fw-bold"><%= Common.getBahasaConfig("Latitude") %></label>
                                            <input type="text" id="lat_<%=rnd%>" class="form-control form-control-sm" readonly>
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="form-group">
                                            <label class="small fw-bold"><%= Common.getBahasaConfig("Longitude") %></label>
                                            <input type="text" id="lng_<%=rnd%>" class="form-control form-control-sm" readonly>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="modal-footer bg-light">
                                <button type="button" class="btn btn-secondary" onclick="$('#modalMap_<%=rnd%>').modal('hide')"><%= Common.getBahasaConfig("Batal") %></button>
                                <button type="button" class="btn btn-primary" onclick="insertMap<%=rnd%>()">
                                    <i class="fa fa-map"></i> <%= Common.getBahasaConfig("Sisipkan Map ke Editor") %>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>`;
            $('body').append(modalHTML);
            
            $('#modalMap_<%=rnd%>').on('shown.bs.modal', function() {
                initMap<%=rnd%>();
            });
        }
        $('#modalMap_<%=rnd%>').modal('show');
        elevateBackdrop<%=rnd%>();
    }

    function initMap<%=rnd%>() {
        const defaultLat = -6.2088;
        const defaultLng = 106.8456;

        if (!leafletMap<%=rnd%>) {
            leafletMap<%=rnd%> = L.map('map_canvas_<%=rnd%>').setView([defaultLat, defaultLng], 13);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© OpenStreetMap contributors'
            }).addTo(leafletMap<%=rnd%>);

            leafletMarker<%=rnd%> = L.marker([defaultLat, defaultLng], {draggable: true}).addTo(leafletMap<%=rnd%>);

            leafletMarker<%=rnd%>.on('dragend', function(e) {
                var coord = e.target.getLatLng();
                updateInputCoords<%=rnd%>(coord.lat, coord.lng);
            });
            
            leafletMap<%=rnd%>.on('click', function(e) {
                leafletMarker<%=rnd%>.setLatLng(e.latlng);
                updateInputCoords<%=rnd%>(e.latlng.lat, e.latlng.lng);
            });

            updateInputCoords<%=rnd%>(defaultLat, defaultLng);
        } else {
            setTimeout(function(){ leafletMap<%=rnd%>.invalidateSize(); }, 200);
        }
    }

    function updateInputCoords<%=rnd%>(lat, lng) {
        $('#lat_<%=rnd%>').val(lat);
        $('#lng_<%=rnd%>').val(lng);
    }

    function insertMap<%=rnd%>() {
        const lat = $('#lat_<%=rnd%>').val();
        const lng = $('#lng_<%=rnd%>').val();
        
        if(!lat || !lng) {
            textAreaToast<%=rnd%>('Koordinat belum dipilih', 'bg-warning');
            return;
        }

        $('#modalMap_<%=rnd%>').modal('hide');
        restoreSelection<%=rnd%>();

        const embedUrl = "https://maps.google.com/maps?q=" + lat + "," + lng + "&hl=id&z=15&output=embed";
        
        const htmlMap = `
            <div class="ratio ratio-16x9 my-2" style="max-width: 600px; border: 1px solid #ccc;">
                <iframe src="`+embedUrl+`" width="100%" height="350" frameborder="0" style="border:0;" allowfullscreen="" loading="lazy" referrerpolicy="no-referrer-when-downgrade"></iframe>
            </div>
            <p><small class="text-muted"><i class="fa fa-map-marker-alt"></i> Lokasi: `+lat+`, `+lng+`</small></p>
            <p><br></p>
        `;

        document.execCommand('insertHTML', false, htmlMap);
        handleOnChange<%=rnd %>();
    }

    // --- FITUR IMAGE EDIT & DELETE ---
    document.getElementById(editorID<%=rnd %>).addEventListener('click', function(e) {
        if (e.target.tagName === 'IMG') { e.stopPropagation(); openImageEditModal<%=rnd%>(e.target); }
    });

    function openImageEditModal<%=rnd%>(imgElement) {
        currentEditingImage<%=rnd%> = imgElement;
        $('#' + editorID<%=rnd %>).find('img').removeClass('editor-image-selected');
        $(imgElement).addClass('editor-image-selected');

        if ($("#modalEditImage_<%=rnd%>").length === 0) {
            var modalHTML = `<div class="modal fade" id="modalEditImage_<%=rnd%>" tabindex="-1" style="z-index: 105000;"><div class="modal-dialog"><div class="modal-content"><div class="modal-header"><h5 class="modal-title"><%= Common.getBahasaConfig("Edit Gambar") %></h5><button type="button" class="close btn-close" onclick="$('#modalEditImage_<%=rnd%>').modal('hide')"><span aria-hidden="true">&times;</span></button></div><div class="modal-body"><div class="mb-3"><label>URL</label><input type="text" class="form-control" id="editImgSrc_<%=rnd%>"></div><div class="row"><div class="col-6"><label>Lebar</label><input type="text" class="form-control" id="editImgWidth_<%=rnd%>"></div><div class="col-6"><label>Tinggi</label><input type="text" class="form-control" id="editImgHeight_<%=rnd%>"></div></div></div><div class="modal-footer"><button type="button" class="btn btn-danger me-auto" onclick="deleteImage<%=rnd%>()">Hapus</button><button type="button" class="btn btn-primary" onclick="saveImageEdits<%=rnd%>()">Simpan</button></div></div></div></div>`;
            $('body').append(modalHTML);
            $('#modalEditImage_<%=rnd%>').on('hidden.bs.modal', function () {
                if(currentEditingImage<%=rnd%>) $(currentEditingImage<%=rnd%>).removeClass('editor-image-selected');
                currentEditingImage<%=rnd%> = null;
            });
        }
        $('#editImgSrc_<%=rnd%>').val(imgElement.getAttribute('src'));
        $('#editImgWidth_<%=rnd%>').val(imgElement.style.width || imgElement.getAttribute('width') || '');
        $('#editImgHeight_<%=rnd%>').val(imgElement.style.height || imgElement.getAttribute('height') || '');
        $('#modalEditImage_<%=rnd%>').modal('show');
        elevateBackdrop<%=rnd%>();
    }
    
    function saveImageEdits<%=rnd%>() {
        if (currentEditingImage<%=rnd%>) {
            let newSrc = $('#editImgSrc_<%=rnd%>').val().trim();
            if(newSrc) currentEditingImage<%=rnd%>.setAttribute('src', newSrc);
            const formatSize = (val) => { val=val.trim(); if(!val)return ""; if(/^[0-9]+$/.test(val)) return val+"px"; return val; };
            let w = formatSize($('#editImgWidth_<%=rnd%>').val()), h = formatSize($('#editImgHeight_<%=rnd%>').val());
            currentEditingImage<%=rnd%>.style.width = w; currentEditingImage<%=rnd%>.style.height = h;
            handleOnChange<%=rnd%>();
        }
        $('#modalEditImage_<%=rnd%>').modal('hide');
    }
    
    function deleteImage<%=rnd%>() {
        if(confirm('Hapus?')) { if(currentEditingImage<%=rnd%>) currentEditingImage<%=rnd%>.remove(); handleOnChange<%=rnd%>(); $('#modalEditImage_<%=rnd%>').modal('hide'); }
    }

    // --- FITUR CAMERA ---
    function openCameraModal<%=rnd %>() {
        saveSelection<%=rnd%>();
        if ($("#modalCamera_<%=rnd%>").length === 0) {
            var modalHTML = `<div class="modal fade" id="modalCamera_<%=rnd%>" tabindex="-1" data-bs-backdrop="static" style="z-index: 105000;"><div class="modal-dialog modal-lg"><div class="modal-content"><div class="modal-header"><h5 class="modal-title">Kamera</h5><button type="button" class="close btn-close" onclick="closeCameraModal<%=rnd%>()"><span aria-hidden="true">&times;</span></button></div><div class="modal-body text-center"><div id="camera_setup_<%=rnd%>" class="my-3"><button type="button" class="btn btn-primary" onclick="startCamera<%=rnd%>()">Aktifkan Kamera</button></div><div id="camera_preview_container_<%=rnd%>" style="display:none;"><video id="camera_video_<%=rnd%>" autoplay playsinline muted></video><canvas id="camera_canvas_<%=rnd%>" style="display:none;"></canvas><div class="mt-3"><button type="button" class="btn btn-success" onclick="takePhoto<%=rnd%>()">Jepret Foto</button></div></div></div></div></div></div>`;
            $('body').append(modalHTML);
        }
        $('#modalCamera_<%=rnd%>').modal('show');
        elevateBackdrop<%=rnd%>();
    }
    function closeCameraModal<%=rnd%>() { stopCameraStream<%=rnd%>(); $('#modalCamera_<%=rnd%>').modal('hide'); }
    async function startCamera<%=rnd%>() {
        try { cameraStream<%=rnd%> = await navigator.mediaDevices.getUserMedia({video:true,audio:false}); document.getElementById('camera_video_<%=rnd%>').srcObject = cameraStream<%=rnd%>; $('#camera_setup_<%=rnd%>').hide(); $('#camera_preview_container_<%=rnd%>').fadeIn(); } catch(e) { textAreaToast<%=rnd%>('Gagal akses kamera','bg-danger'); }
    }
    function stopCameraStream<%=rnd%>() { if(cameraStream<%=rnd%>) { cameraStream<%=rnd%>.getTracks().forEach(t=>t.stop()); cameraStream<%=rnd%>=null; } $('#camera_setup_<%=rnd%>').show(); $('#camera_preview_container_<%=rnd%>').hide(); }
    function takePhoto<%=rnd%>() {
        const video = document.getElementById('camera_video_<%=rnd%>'), canvas = document.getElementById('camera_canvas_<%=rnd%>');
        canvas.width = video.videoWidth; canvas.height = video.videoHeight;
        const ctx = canvas.getContext('2d'); ctx.translate(canvas.width, 0); ctx.scale(-1, 1); ctx.drawImage(video, 0, 0);
        canvas.toBlob(function(b) { activeUploadType<%=rnd%>='image'; processFileUpload<%=rnd%>(new File([b],"cam_"+Date.now()+".jpg",{type:"image/jpeg"})); closeCameraModal<%=rnd%>(); }, 'image/jpeg', 0.95);
    }

    // --- FITUR SCREEN CAPTURE & UPLOAD ---
    function openScreenModal<%=rnd %>() { 
        saveSelection<%=rnd%>(); 
        if ($("#modalScreen_<%=rnd%>").length===0) { 
            var h=`<div class="modal fade" id="modalScreen_<%=rnd%>" data-bs-backdrop="static" style="z-index: 105000;"><div class="modal-dialog modal-lg"><div class="modal-content"><div class="modal-header"><h5 class="modal-title">Screen Capture</h5><button type="button" class="close btn-close" onclick="closeScreenModal<%=rnd%>()"><span aria-hidden="true">&times;</span></button></div><div class="modal-body text-center"><button type="button" class="btn btn-primary my-3" id="btn_start_share_<%=rnd%>" onclick="startScreenShare<%=rnd%>()">Mulai Share</button><div id="screen_preview_container_<%=rnd%>" style="display:none;"><video id="screen_video_<%=rnd%>" autoplay muted playsinline></video><div class="mt-3"><button type="button" class="btn btn-success" onclick="takeScreenshot<%=rnd%>()">Screenshot</button> <button type="button" id="btn_rec_<%=rnd%>" class="btn btn-danger" onclick="toggleRecording<%=rnd%>()">Rekam</button></div></div></div></div></div></div>`; 
            $('body').append(h); 
        } 
        $('#modalScreen_<%=rnd%>').modal('show'); 
        elevateBackdrop<%=rnd%>();
    }
    
    function closeScreenModal<%=rnd%>() { stopScreenStream<%=rnd%>(); $('#modalScreen_<%=rnd%>').modal('hide'); }
    async function startScreenShare<%=rnd%>() { try{ screenStream<%=rnd%> = await navigator.mediaDevices.getDisplayMedia({video:{cursor:"always"},audio:true}); document.getElementById('screen_video_<%=rnd%>').srcObject=screenStream<%=rnd%>; $('#btn_start_share_<%=rnd%>').hide(); $('#screen_preview_container_<%=rnd%>').show(); }catch(e){} }
    function stopScreenStream<%=rnd%>() { if(screenStream<%=rnd%>) { screenStream<%=rnd%>.getTracks().forEach(t=>t.stop()); screenStream<%=rnd%>=null; } if(mediaRecorder<%=rnd%>) mediaRecorder<%=rnd%>.stop(); $('#btn_start_share_<%=rnd%>').show(); $('#screen_preview_container_<%=rnd%>').hide(); }
    function takeScreenshot<%=rnd%>() { var v=document.getElementById('screen_video_<%=rnd%>'),c=document.createElement('canvas'); c.width=v.videoWidth; c.height=v.videoHeight; c.getContext('2d').drawImage(v,0,0); c.toBlob(function(b){activeUploadType<%=rnd%>='image';processFileUpload<%=rnd%>(new File([b],"ss_"+Date.now()+".png",{type:"image/png"}));closeScreenModal<%=rnd%>();}); }
    function toggleRecording<%=rnd%>() { if(!mediaRecorder<%=rnd%>||mediaRecorder<%=rnd%>.state==='inactive'){ recordedChunks<%=rnd%>=[]; mediaRecorder<%=rnd%>=new MediaRecorder(screenStream<%=rnd%>); mediaRecorder<%=rnd%>.ondataavailable=e=>{if(e.data.size>0)recordedChunks<%=rnd%>.push(e.data)}; mediaRecorder<%=rnd%>.onstop=()=>{ activeUploadType<%=rnd%>='video'; processFileUpload<%=rnd%>(new File([new Blob(recordedChunks<%=rnd%>,{type:"video/webm"})],"rec_"+Date.now()+".webm",{type:"video/webm"})); closeScreenModal<%=rnd%>(); }; mediaRecorder<%=rnd%>.start(); $('#btn_rec_<%=rnd%>').text("Stop"); } else { mediaRecorder<%=rnd%>.stop(); } }

 // --- MODAL LINK LANJUTAN ---
    function openLinkModal<%=rnd %>() {
        saveSelection<%=rnd%>(); 
        
        if ($("#modalLink_<%=rnd%>").length === 0) {
            var html = `
            <div class="modal fade" id="modalLink_<%=rnd%>" tabindex="-1" style="z-index: 105000;">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title"><i class="fa fa-link"></i> Sisipkan Link / Embed</h5>
                            <button type="button" class="btn-close" onclick="$('#modalLink_<%=rnd%>').modal('hide')"></button>
                        </div>
                        <div class="modal-body">
                            <div class="form-group mb-3">
                                <label class="fw-bold"><%= Common.getBahasaConfig("URL / Tautan") %></label>
                                <div class="input-group">
                                    <span class="input-group-text"><i class="fa fa-globe"></i></span>
                                    <input id="urlLink_<%=rnd%>" class="form-control" placeholder="https://..." onkeydown="if(event.keyCode==13) applyLink<%=rnd%>()">
                                </div>
                                <small class="text-muted">Masukkan link lengkap (dimulai dengan http:// atau https://)</small>
                            </div>

                            <label class="fw-bold small text-uppercase text-muted mb-2"><%= Common.getBahasaConfig("Panduan Format Link") %></label>
                            <div class="link-guide-wrapper">
                                <div class="link-guide-item">
                                    <span class="link-guide-title"><i class="fab fa-youtube text-danger"></i> YouTube</span>
                                    <span class="small">Otomatis menjadi Video Player.</span>
                                    <span class="link-guide-code">https://www.youtube.com/watch?v=dQw4w9WgXcQ</span>
                                    <span class="link-guide-code">https://youtu.be/dQw4w9WgXcQ</span>
                                </div>
                                <div class="link-guide-item">
                                    <span class="link-guide-title"><i class="fab fa-google-drive text-success"></i> Google Drive & Docs</span>
                                    <span class="small">Otomatis menjadi Preview Frame (Dokumen/Video). Pastikan akses file sudah "Public" atau "Anyone with link".</span>
                                    <span class="link-guide-code">https://drive.google.com/file/d/123xyz.../view</span>
                                    <span class="link-guide-code">https://docs.google.com/document/d/1abc.../edit</span>
                                </div>
                                <div class="link-guide-item">
                                    <span class="link-guide-title"><i class="fab fa-twitter text-info"></i> Twitter / X</span>
                                    <span class="small">Otomatis menjadi Kartu Tautan Twitter.</span>
                                    <span class="link-guide-code">https://twitter.com/username/status/123456...</span>
                                    <span class="link-guide-code">https://x.com/username/status/123456...</span>
                                </div>
                                <div class="link-guide-item">
                                    <span class="link-guide-title"><i class="fab fa-facebook text-primary"></i> Facebook & Instagram</span>
                                    <span class="small">Otomatis menjadi Kartu Tautan Sosial Media.</span>
                                    <span class="link-guide-code">https://www.facebook.com/zuck/posts/101...</span>
                                    <span class="link-guide-code">https://www.instagram.com/p/Cxyz.../</span>
                                </div>
                                <div class="link-guide-item">
                                    <span class="link-guide-title"><i class="fab fa-dropbox text-primary"></i> Dropbox</span>
                                    <span class="small">Link Download/Preview.</span>
                                    <span class="link-guide-code">https://www.dropbox.com/s/abcd.../file.pdf</span>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer bg-light">
                            <button type="button" class="btn btn-secondary" onclick="$('#modalLink_<%=rnd%>').modal('hide')"><%= Common.getBahasaConfig("Batal") %></button>
                            <button type="button" class="btn btn-primary" onclick="applyLink<%=rnd%>()"><i class="fa fa-save"></i> Simpan & Proses</button>
                        </div>
                    </div>
                </div>
            </div>`;
            $('body').append(html);
        }
        
        $('#urlLink_<%=rnd%>').val(''); 
        $('#modalLink_<%=rnd%>').modal('show');
        elevateBackdrop<%=rnd%>();
        setTimeout(() => $('#urlLink_<%=rnd%>').focus(), 500);
    }

    function applyLink<%=rnd%>() {
        const urlInput = $('#urlLink_<%=rnd%>').val().trim();
        
        if(!urlInput) {
            textAreaToast<%=rnd%>('Mohon masukkan URL terlebih dahulu.', 'bg-warning');
            return;
        }

        $('#modalLink_<%=rnd%>').modal('hide'); 
        restoreSelection<%=rnd%>(); 
        
        const embedHTML = detectAndEmbed<%=rnd%>(urlInput);
        
        document.execCommand('insertHTML', false, embedHTML); 
        handleOnChange<%=rnd%>(); 
    }

    function detectAndEmbed<%=rnd%>(url) {
        let ytId = null;
        
        try {
            let urlObj = new URL(url);
            if (urlObj.hostname.includes('youtube.com')) {
                if (urlObj.searchParams.has('v')) {
                    ytId = urlObj.searchParams.get('v');
                } else if (urlObj.pathname.includes('/embed/') || urlObj.pathname.includes('/shorts/') || urlObj.pathname.includes('/v/')) {
                    let parts = urlObj.pathname.split('/');
                    ytId = parts[parts.length - 1];
                }
            } else if (urlObj.hostname.includes('youtu.be')) {
                ytId = urlObj.pathname.substring(1); 
            }
        } catch(e) {}

        if (!ytId) {
            const match = url.match(/^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/);
            if (match && match[2].length === 11) ytId = match[2];
        }
        
        if (ytId) {
            return `
                <div class="ratio ratio-16x9 my-3 shadow-sm" style="max-width: 650px; margin: 0 auto; border-radius: 8px; overflow: hidden; border: 1px solid #ddd;">
                    <iframe src="https://www.youtube.com/embed/` + ytId + `" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
                </div>
                <p><br></p>`;
        }

        if (url.match(/(drive|docs)\.google\.com/)) {
             let embedUrl = url;
            if (url.includes('/view')) embedUrl = url.replace(/\/view.*/, '/preview');
            else if (url.includes('/edit')) embedUrl = url.replace(/\/edit.*/, '/preview');
            else if (!embedUrl.endsWith('/preview')) embedUrl += '/preview';

            let icon = 'fa-google-drive';
            if(url.includes('docs.google.com')) icon = 'fa-file-alt';
            if(url.includes('spreadsheets')) icon = 'fa-file-excel';
            if(url.includes('presentation')) icon = 'fa-file-powerpoint';

            return `
                <div class="my-3 shadow-sm" style="max-width: 650px; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;">
                    <div class="bg-light px-2 py-1 small border-bottom text-muted"><i class="fab ` + icon + `"></i> Google Preview</div>
                    <div class="ratio ratio-4x3">
                        <iframe src="` + embedUrl + `" style="border:0"></iframe>
                    </div>
                </div>
                <p><br></p>`;
        }
        
        if (url.match(/(twitter\.com|x\.com|facebook\.com|fb\.watch|instagram\.com|dropbox\.com)/)) {
             let icon = 'fa-link';
             let title = 'Tautan Eksternal';
             if(url.includes('twitter') || url.includes('x.com')) { icon='fa-twitter text-info'; title='Twitter / X'; }
             else if(url.includes('facebook') || url.includes('fb.watch')) { icon='fa-facebook text-primary'; title='Facebook'; }
             else if(url.includes('instagram')) { icon='fa-instagram text-danger'; title='Instagram'; }
             else if(url.includes('dropbox')) { icon='fa-dropbox text-primary'; title='Dropbox'; }

             return `
                <a href="` + url + `" target="_blank" class="embed-card" contenteditable="false" style="text-decoration:none; color:#333; border:1px solid #ddd; padding:10px; display:flex; align-items:center; border-radius:5px; margin:10px 0; background:#fff;">
                    <div class="embed-icon me-3" style="font-size:24px; width:30px; text-align:center;"><i class="fab ` + icon + `"></i></div>
                    <div class="embed-info" style="overflow:hidden;">
                        <span class="d-block fw-bold" style="font-size:14px;">` + title + `</span>
                        <span class="d-block text-muted small text-truncate" style="max-width:400px;">` + url + `</span>
                    </div>
                    <i class="fa fa-external-link-alt text-muted ms-auto small"></i>
                </a><p><br></p>`;
        }

        return ` <a href="` + url + `" target="_blank" class="text-primary fw-bold" style="text-decoration:underline;">` + url + `</a> `;
    }

    // --- UPLOAD HANDLERS ---
    function triggerUpload<%=rnd %>(t) { saveSelection<%=rnd%>(); activeUploadType<%=rnd%>=t; let f=$('#input_file_<%=rnd%>'); f.val(''); f.attr('accept', t=='image'?'image/*':(t=='audio'?'.mp3':'video/*')); f.click(); }
    function handleFileInputChange<%=rnd%>() { let f=$('#input_file_<%=rnd%>')[0]; if(f.files.length) processFileUpload<%=rnd%>(f.files[0]); }
    function processFileUpload<%=rnd%>(file) {
        if(file.size > 10*1024*1024) { textAreaToast<%=rnd%>('File > 10MB', 'bg-danger'); return; }
        $('#progress-wrapper-<%=rnd%>').show(); $('#progress-bar-<%=rnd%>').css('width','0%');
        var fd = new FormData(); fd.append('nama',file.name); fd.append('clazz','<%=uploadClazz%>'); fd.append('jenis','<%=uploadJenis%>_'+generateRandomTextTextArea<%=rnd%>(10)); fd.append('id','<%=uploadId%>'); fd.append('fileContent',file);
        $.ajax({
            url: '<%=actionUrl%>', type: 'POST', data: fd, cache:false, contentType:false, processData:false,
            xhr: function(){var x=new window.XMLHttpRequest();x.upload.addEventListener("progress",function(e){if(e.lengthComputable)$('#progress-bar-<%=rnd%>').css('width',Math.round((e.loaded/e.total)*100)+'%');});return x;},
            success: function(res){
                setTimeout(()=>{ $('#progress-wrapper-<%=rnd%>').fadeOut(); },800);
                if(res.status==='Sukses'||res.status==='00') {
                    restoreSelection<%=rnd%>(); let u=res.url||res.data.url, h='';
                    if(activeUploadType<%=rnd%>=='image') h=`<img src="` + u + `" class="img-fluid">`;
                    else if(activeUploadType<%=rnd%>=='audio') h=`<div class="my-2"><audio controls src="` + u + `"></audio></div><p><br></p>`;
                    else h=`<div class="my-2"><video controls style="max-width:100%" src="` + u + `"></video></div><p><br></p>`;
                    document.execCommand('insertHTML',false,h); handleOnChange<%=rnd%>();
                } else textAreaToast<%=rnd%>('Gagal: '+res.keterangan,'bg-danger');
            },
            error: function(){ $('#progress-wrapper-<%=rnd%>').hide(); textAreaToast<%=rnd%>('Error koneksi','bg-danger'); }
        });
    }

    // --- Utilities ---
    function formatDoc<%=rnd%>(c,v){ document.execCommand(c,false,v); document.getElementById(editorID<%=rnd%>).focus(); handleOnChange<%=rnd%>(); saveSelection<%=rnd%>(); }
    function toggleSource<%=rnd%>(){ var v=document.getElementById(editorID<%=rnd%>), t=document.getElementById(textareaID<%=rnd%>); if(t.classList.contains('d-none')){ syncTextarea<%=rnd%>(); v.classList.add('d-none'); t.classList.remove('d-none'); t.classList.add('form-control','p-3','border-0'); t.style.minHeight='220px'; t.focus(); textAreaToast<%=rnd%>('Mode source HTML aktif.', 'bg-info'); }else{ v.innerHTML=t.value; t.classList.add('d-none'); v.classList.remove('d-none'); v.focus(); handleOnChange<%=rnd%>(); textAreaToast<%=rnd%>('Mode visual editor aktif.', 'bg-info'); } }
    
	 function syncTextarea<%=rnd%>(){ document.getElementById(textareaID<%=rnd%>).value = document.getElementById(editorID<%=rnd%>).innerHTML; }
    
    function saveContent<%=rnd%>(){ handleOnChange<%=rnd%>(); textAreaToast<%=rnd%>('Konten berhasil disinkronkan ke textarea.', 'bg-success'); }
    function printContent<%=rnd%>(){ var w=window.open('','','height=650,width=850'); if(!w){ textAreaToast<%=rnd%>('Popup print diblokir browser.', 'bg-warning'); return; } w.document.write('<html><head><title>Print</title><style>body{font-family:Arial,sans-serif;line-height:1.6;padding:20px;} img,video,iframe{max-width:100%;}</style></head><body>'+document.getElementById(editorID<%=rnd%>).innerHTML+'</body></html>'); w.document.close(); w.focus(); w.print(); }
    document.getElementById(editorID<%=rnd%>).addEventListener('focus', function(){ if(this.innerText.trim()==="<%= Common.getBahasaConfig(placeholder) %>"){this.innerHTML="";this.classList.remove("text-muted");} saveSelection<%=rnd%>(); });
    document.addEventListener('DOMContentLoaded', function(){ try { syncTextarea<%=rnd%>(); } catch(e) {} });
</script>