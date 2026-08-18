<%@page import="ais.database.hibernate.StreamingHibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="ais.ui.util.SmartDateTimeUtil"%>
<%@page import="ais.database.model.TugasPertemuan"%>
<%@page import="ais.database.model.Tugas"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // ==========================================
    // 1. SECURITY & PARAMETER HANDLING
    // ==========================================
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return;
    }
    Boolean refreshData = request.getParameter("refresh")==null?false: request.getParameter("refresh").trim().equalsIgnoreCase("true") ;
    String itemId = request.getParameter("pertemuan");
    String paramTugas = request.getParameter("tugas");
    String paramJenis = request.getParameter("jenis");

    if (itemId == null || itemId.trim().isEmpty()) {
        out.print(Common.getBahasaConfig("Parameter pertemuan tidak ditemukan"));
        return;
    }

    // ==========================================
    // 2. DATA RETRIEVAL & SESSION MANAGEMENT
    // ==========================================
    Pertemuan pertemuan = null;
    Tugas tugasdata = null;
    Long idPerkuliahan = null; 
    boolean hasError = false;
    
    try {
        pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, itemId.trim(), true);
        if (pertemuan == null || pertemuan.getId() == null || !pertemuan.getAktif()) {
            out.print(Common.getBahasaConfig("Data pertemuan tidak valid atau tidak aktif"));
            return;
        }
        
        // Ambil ID Perkuliahan
        if(pertemuan.getPerkuliahan() != null) {
            idPerkuliahan = pertemuan.getPerkuliahan().getId();
        }

        if (paramTugas != null && !paramTugas.trim().isEmpty() && paramJenis != null) {
            String tugasIdClean = paramTugas.trim();
            if ("Pertemuan".equalsIgnoreCase(paramJenis)) {
                tugasdata = (Tugas) GeneralValueObject.ambilData(Tugas.class, tugasIdClean, true);
            } else if ("TugasPertemuan".equalsIgnoreCase(paramJenis)) {
                tugasdata = (Tugas) GeneralValueObject.ambilData(TugasPertemuan.class, tugasIdClean, true);
            }
        }
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/tugas/_tugas.jsp:61");
        hasError = true;
    } 
    
    if (tugasdata == null) {
        out.print("<div class='alert alert-warning'>" + Common.getBahasaConfig("Tugas tidak ditemukan") + "</div>");
        return;
    }

    if (hasError) {
        out.print("<div class='alert alert-danger'>" + Common.getBahasaConfig("Terjadi kesalahan saat memuat data") + "</div>");
        return;
    }
    
    if(refreshData){
    	tugasdata.belum("tugas_file_content_" + tugasdata.getClass().getName());	
    	Session sessionStreaming = StreamingHibernateUtil.getInstance().currentSession();
        tugasdata.reInitTugasFileContent(sessionStreaming);
		
    }
    
    String idtugas = tugasdata.getId() != null ? tugasdata.getId().toString() : "";

    // ==========================================
    // 3. LOGIC & HELPERS
    // ==========================================
    boolean bolehUbah = (pertemuan != null) && pertemuan.bolehUbahAbsenSaja(tbmuser);
    boolean hasMulai = (tugasdata.getMulai() != null);
    boolean hasSelesai = (tugasdata.getSelesai() != null);
    
    long waktuSekarang = WaktuUtil.getDate().getTime();
    long waktuMulai = hasMulai ? tugasdata.getMulai().getTime() : 0;
    long waktuSelesai = hasSelesai ? tugasdata.getSelesai().getTime() : 0;
    boolean isBelumMulai = hasMulai && (waktuSekarang < waktuMulai);
    boolean isSelesai = hasSelesai && (waktuSekarang > waktuSelesai);
    boolean isBerlangsung = !isBelumMulai && !isSelesai;

    // DETEKSI LOGIK MAHASISWA ATAU SISWA
    boolean isMahasiswa = tbmuser.getMahasiswa() != null;
    boolean isSiswa = tbmuser.getSiswa() != null;
    boolean isStudentLogin = isMahasiswa || isSiswa;
    String userInduk = "";
    if (isMahasiswa) {
        userInduk = tbmuser.getMahasiswa().getNim();
    } else if (isSiswa) {
        userInduk = tbmuser.getSiswa().getNis();
    }

    String strMulai = !hasMulai ? "-" : (SmartDateTimeUtil.getDayString(tugasdata.getMulai(), null) + " " + Common.dateFormat5.get().format(tugasdata.getMulai()));
    String strSelesai = !hasSelesai ? "-" : (SmartDateTimeUtil.getDayString(tugasdata.getSelesai(), null) + " " + Common.dateFormat5.get().format(tugasdata.getSelesai()));
    String linkTugasBaru = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=tugas_baru&pertemuan=" + pertemuan.getId();
    String linkUpload = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=_upload_tugas&pertemuan=" + pertemuan.getId()+"&tugas="+idtugas+"&jenis="+paramJenis;
    
    // UI Config Vars
    String cardClass = "border-primary";
    String iconStatus = "fa-clock";
    String textStatus = Common.getBahasaConfig("Sedang Berlangsung");
    String textDesc = Common.getBahasaConfig("Silahkan kerjakan tugas ini sebelum batas waktu berakhir");
    String badgeColor = "bg-primary";
    
    if (isBelumMulai) {
        cardClass = "border-secondary";
        iconStatus = "fa-hourglass-start";
        textStatus = Common.getBahasaConfig("Belum Dimulai");
        textDesc = Common.getBahasaConfig("Tugas belum dibuka untuk pengerjaan");
        badgeColor = "bg-secondary";
    } else if (isSelesai) {
        cardClass = "border-danger";
        iconStatus = "fa-lock";
        textStatus = Common.getBahasaConfig("Telah Selesai");
        textDesc = Common.getBahasaConfig("Batas waktu pengerjaan telah habis");
        badgeColor = "bg-danger";
    }
%>

<style>
    @keyframes pulse-soft {
        0% { box-shadow: 0 0 0 0 rgba(13, 110, 253, 0.4); }
        70% { box-shadow: 0 0 0 10px rgba(13, 110, 253, 0); }
        100% { box-shadow: 0 0 0 0 rgba(13, 110, 253, 0); }
    }
    .btn-pulse { animation: pulse-soft 2s infinite; }
</style>

<%-- SECTION 1: HERO HEADER --%>
<div class="card shadow-sm mb-4 <%= cardClass %>" style="border-top-width: 5px;">
    <div class="card-body p-4">
        <div class="row align-items-center h-100">
            <%-- Kiri: Informasi Tugas --%>
            <div class="col-lg-7 col-xl-8">
                <div class="d-flex align-items-center mb-3">
                    <span class="badge <%= badgeColor %> me-2 px-3 py-2 rounded-pill">
                        <i class="fa <%= iconStatus %> me-1"></i> <%= textStatus %>
                    </span>
                    <% if (paramJenis != null) { %>
                    <span class="badge bg-light text-secondary border">
                        <%= paramJenis.toUpperCase() %>
                    </span>
                    <% } %>
                </div>
                
                <h3 class="fw-bold mb-2"><%= tugasdata.getJudultugas() %></h3>
                <p class="text-muted mb-4 fs-6"><%= textDesc %></p>

                <div class="d-inline-flex flex-wrap gap-4 p-3 bg-light rounded border">
                    <div>
                        <small class="text-muted d-block text-uppercase fw-bold mb-1" style="font-size: 0.7rem;">
                            <%= Common.getBahasaConfig("Mulai") %>
                        </small>
                        <span class="fw-bold <%= isBelumMulai ? "text-danger" : "text-dark" %>">
                            <i class="fa fa-calendar-alt me-1 text-muted"></i> <%= strMulai %>
                        </span>
                    </div>
                    <div class="border-start mx-2 d-none d-md-block"></div>
                    <div>
                        <small class="text-muted d-block text-uppercase fw-bold mb-1" style="font-size: 0.7rem;">
                            <%= Common.getBahasaConfig("Batas Akhir") %>
                        </small>
                        <span class="fw-bold <%= isSelesai ? "text-danger" : (isBerlangsung ? "text-primary" : "text-dark") %>">
                            <i class="fa fa-flag-checkered me-1 text-muted"></i> <%= strSelesai %>
                        </span>
                    </div>
                </div>
            </div>
            
            <%-- Kanan: Tombol Aksi --%>
            <div class="col-lg-5 col-xl-4 d-flex align-items-center justify-content-lg-end justify-content-center mt-4 mt-lg-0 border-lg-start h-100">
                <% if (isBerlangsung) { %>
                    <button class="btn btn-primary btn-lg fw-bold shadow-sm px-5 py-3 rounded-pill btn-pulse w-100 w-lg-auto" style="font-size: 1.1rem;" onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Buat Tugas Baru") %>', '<%=linkUpload%>&contentModal='+cm+'&afterSave='+encodeURIComponent('loadDataTugas<%=itemId%>();'), '600px', false, '', cm);">
                        <i class="fa fa-play-circle me-2 fs-4 align-middle"></i> 
                        <span class="align-middle"><%= Common.getBahasaConfig("Kerjakan Sekarang") %></span>
                    </button>
                <% } else if (isBelumMulai) { %>
                    <button class="btn btn-secondary btn-lg fw-bold disabled opacity-75 px-5 py-3 rounded-pill w-100 w-lg-auto" disabled>
                        <i class="fa fa-lock me-2 fs-4 align-middle"></i> 
                        <span class="align-middle"><%= Common.getBahasaConfig("Terkunci") %></span>
                    </button>
                <% } else { %>
                    <button class="btn btn-outline-danger btn-lg fw-bold disabled opacity-75 px-5 py-3 rounded-pill w-100 w-lg-auto" disabled>
                        <i class="fa fa-times-circle me-2 fs-4 align-middle"></i> 
                        <span class="align-middle"><%= Common.getBahasaConfig("Ditutup") %></span>
                    </button>
                <% } %>
            </div>
        </div>
    </div>
</div>

<%-- SECTION 2: INFO PERTEMUAN (Collapsible) --%>
<div class="mb-4">
    <div class="d-flex justify-content-center">
        <button class="btn btn-sm btn-white border text-secondary shadow-sm rounded-pill px-4" type="button" 
                data-bs-toggle="collapse" data-bs-target="#collapseMeetingInfo_<%=itemId%>" 
                aria-expanded="false" aria-controls="collapseMeetingInfo_<%=itemId%>">
            <i class="fa fa-info-circle me-1 text-primary"></i> 
            <%= Common.getBahasaConfig("Lihat Informasi Pertemuan") %> 
            <i class="fa fa-chevron-down ms-2 small"></i>
        </button>
    </div>
    <div class="collapse mt-3" id="collapseMeetingInfo_<%=itemId%>">
        <div class="card card-body bg-light border-0 shadow-sm">
            <div id="info_pertemuan_<%=itemId%>"></div>
        </div>
    </div>
</div>

<%-- SECTION 3: CONTENT GRID --%>
<div class="row g-4">
    
    <%-- Kolom Kiri: INSTRUKSI & ADMIN --%>
    <div class="col-lg-5">
        <div class="card shadow-sm border-0 h-100">
            <div class="card-body">
                
                <%-- Header Instruksi --%>
                <div class="d-flex align-items-center mb-3">
                    <div class="bg-primary bg-opacity-10 text-primary rounded px-2 py-1 me-2">
                        <i class="fa fa-align-left"></i>
                    </div>
                    <h5 class="fw-bold mb-0 text-dark"><%= Common.getBahasaConfig("Instruksi Tugas") %></h5>
                </div>
                
                <%-- Konten Instruksi Dinamis --%>
                <% if (isBerlangsung) { %>
                    <div class="p-4 bg-light bg-gradient rounded border border-start-0 border-end-0 text-dark lh-lg mb-4" 
                         style="font-size: 0.95rem; min-height: 150px;">
                         
                         <jsp:include page="/WEB-INF/baru/modul/common/upload_component.jsp">
                            <jsp:param name="clazz" value="<%=LampiranLain.class.getName()%>" />
                            <jsp:param name="jenis" value="<%=LampiranLain.TUGAS_MANDIRI_PERKULIAHAN%>" />
                            <jsp:param name="rnd" value="<%= itemId %>" />
                            <jsp:param name="id" value="<%=idtugas %>" />
                            <jsp:param name="col" value="file_soal" />
                            <jsp:param name="label" value="<%= Common.getBahasaConfig(\"Soal\") %>" />
                            <jsp:param name="tampilUpload" value="false" />
                        </jsp:include> 
                        
                        <%= tugasdata.getIsitugas() %>
                    </div>
                <% } else { %>
                    <div class="d-flex flex-column align-items-center justify-content-center py-5 bg-light rounded border border-dashed mb-4 opacity-75">
                        <div class="bg-secondary bg-opacity-10 p-3 rounded-circle mb-3">
                            <i class="fa fa-lock fa-2x text-secondary opacity-50"></i>
                        </div>
                        <h6 class="fw-bold text-secondary"><%= Common.getBahasaConfig("Konten Tidak Tersedia") %></h6>
                        <p class="text-muted small mb-0 px-4 text-center">
                            <%= isSelesai ? Common.getBahasaConfig("Instruksi disembunyikan karena tugas telah berakhir.") : Common.getBahasaConfig("Instruksi akan tampil saat waktu mulai tiba.") %>
                        </p>
                    </div>
                <% } %>

                <%-- Catatan Penting --%>
                <div class="alert alert-warning border-0 bg-warning bg-opacity-10 d-flex align-items-start small rounded-3 mb-4">
                    <i class="fa fa-exclamation-triangle text-warning mt-1 me-3 fs-5"></i>
                    <div>
                        <strong class="text-warning-emphasis"><%=Common.getBahasaConfig("Perhatian")%>:</strong><br>
                        <span class="text-muted">
                            <%=Common.getBahasaConfig("Masing-masing mahasiswa hanya bisa memiliki satu file tugas. Upload ulang akan menimpa file sebelumnya.")%>
                        </span>
                    </div>
                </div>

                <%-- Admin / Dosen Buttons --%>
                <% if (bolehUbah && tugasdata != null && tugasdata.getId() != null) { %>
                    <hr class="border-secondary opacity-10 my-4">
                    <p class="small fw-bold text-muted text-uppercase mb-2"><%=Common.getBahasaConfig("Menu Pengajar")%></p>
                    <div class="row g-2">
                        <div class="col-sm-6">
                            <button class="btn btn-outline-dark btn-sm w-100 py-2" onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Buat Tugas Baru") %>', '<%=linkTugasBaru%>&id=<%=tugasdata.getId() %>&jenis=<%=paramJenis %>&contentModal='+cm+'&afterSave='+encodeURIComponent('generatePertemuanHTML(<%=pertemuan.getId()%>);loadDataTugas<%=itemId%>();'), '75%', true, 'simpanDataHelper_'+cm+'();', cm);">
                                <i class="fa fa-edit me-1"></i> <%=Common.getBahasaConfig("Ubah Instruksi")%>
                            </button>
                        </div>
                        <div class="col-sm-6">
                            <button class="btn btn-outline-dark btn-sm w-100 py-2" onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Format Nilai")%>', '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=_format_nilai&idTugas=<%=tugasdata.getId()%>&jenis=<%=paramJenis%>&contentModal='+cm, '85%', 'hidden', '', cm);">
                                <i class="fa fa-table me-1"></i> <%=Common.getBahasaConfig("Format Nilai")%>
                            </button>
                        </div>
                        <div class="col-sm-6">
                            <button class="btn btn-outline-dark btn-sm w-100 py-2" onclick="unduhSemuaTugas<%=itemId%>();">
                                <i class="fa fa-download me-1"></i> <%=Common.getBahasaConfig("Download Semua")%>
                            </button>
                        </div>
                        <div class="col-sm-6">
                            <button class="btn btn-outline-danger btn-sm w-100 py-2" onclick="hapusTugas<%=itemId%>();">
                                <i class="fa fa-trash me-1"></i> <%=Common.getBahasaConfig("Hapus Tugas")%>
                            </button>
                        </div>
                    </div>
                <% } %>
                
            </div>
        </div>
    </div>

    <%-- Kolom Kanan: DAFTAR PESERTA --%>
    <div class="col-lg-7">
        <div class="card shadow-sm border-0 h-100">
            <div class="card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center">
                <h6 class="m-0 fw-bold"><i class="fa fa-users text-primary me-2"></i><%= Common.getBahasaConfig("Peserta") %></h6>
                
                <div>
                    <% if (bolehUbah && tugasdata != null && tugasdata.getId() != null) { %>
                    <button onclick="syncNilaiTugas<%=itemId%>(event);" class="btn btn-sm btn-outline-success me-1">
                        <i class="fa fa-cloud-upload-alt small"></i> <%=Common.getBahasaConfig("Singkronkan Nilai")%>
                    </button>
                    <% } %>
                    <button onclick="loadDataTugasData<%=itemId%>(true);" class="btn btn-sm btn-light border text-secondary">
                        <i class="fa fa-sync small"></i> <%=Common.getBahasaConfig("Refresh")%>
                    </button>
                </div>
            </div>
            
            <div class="p-2 bg-light border-bottom">
                <div class="input-group input-group-sm">
                    <span class="input-group-text bg-white border-end-0"><i class="fa fa-search text-muted"></i></span>
                    <input type="text" class="form-control bg-white border-start-0" placeholder="<%=Common.getBahasaConfig("Cari peserta...")%>">
                </div>
            </div>

            <div class="card-body p-0 position-relative overflow-auto">
                <div id="daftar_peserta_tugas_<%=itemId%>"></div>
            </div>
        </div>
    </div>
</div>
    
<script>
    // Unduh SEMUA berkas pengumpulan tugas sebagai 1 ZIP (server merakit dari DB streaming).
    function unduhSemuaTugas<%=itemId%>(){
        var url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=download_semua&idTugas=<%=tugasdata.getId()%>&jenis=<%=paramJenis%>';
        if (typeof tampilkanToast === 'function') {
            tampilkanToast('<%=Common.getBahasaConfigJS("Menyiapkan arsip ZIP berkas tugas...")%>', 'bg-info');
        }
        window.open(url, '_blank');
    }

    function hapusTugas<%=itemId%>(){
		if('<%=tugasdata.getClass().getName() %>' === '<%=Pertemuan.class.getName() %>'){
			prosesDeleteDataTapiUpdate('<%=tugasdata.getClass().getName() %>', '<%=tugasdata.getId()%>', function(){let dataObj = {};dataObj.judultugas = ``;return dataObj;}, function(){ tutupSemuaModal();generatePertemuanHTMLRefresh(<%=pertemuan.getId()%>, true); } );
		} else {
			prosesDeleteData('<%=tugasdata.getClass().getName() %>', '<%=tugasdata.getId()%>', function(){ tutupSemuaModal();generatePertemuanHTMLRefresh(<%=pertemuan.getId()%>, true); });
		}
	}

    // =================================================================================
    // FUNGSI SINKRONISASI NILAI TUGAS & LAPORAN OTOMATIS
    // =================================================================================
    function syncNilaiTugas<%=itemId%>(e) {
        var btn = e.currentTarget;
        
        // Memanggil modal konfirmasi global
        showConfirmModal('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin mengkalkulasi dan menyinkronkan nilai tugas ini ke rekap akhir mahasiswa?")%>', function() {
            
            var originalHtml = btn.innerHTML;
            btn.innerHTML = '<i class="fa fa-spinner fa-spin small me-1"></i> <%=Common.getBahasaConfig("Proses...")%>';
            btn.disabled = true;

            var formData = new URLSearchParams();
            formData.append('action', 'sync');
            formData.append('idTugas', '<%=tugasdata != null ? tugasdata.getId() : ""%>');
            formData.append('jenisTugas', '<%=paramJenis != null ? paramJenis : ""%>');

            if(typeof tampilkanToast === 'function') {
                tampilkanToast('<%=Common.getBahasaConfigJS("Sedang menyinkronkan nilai...")%>', 'bg-info');
            }

            fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_format_nilai_tugas_services', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData.toString()
            })
            .then(function(response) { return response.json(); })
            .then(function(data) {
                btn.innerHTML = originalHtml;
                btn.disabled = false;
                
                if(data.status === 'success') {
                    if(typeof tampilkanToast === 'function') {
                        tampilkanToast(data.message, 'bg-success');
                    }
                    
                    // PANGGIL GENERATOR PDF APABILA SUKSES
                    <% if (idPerkuliahan != null) { %>
                        cetakLaporanNilai_<%=itemId%>('<%= idPerkuliahan %>');
                    <% } %>
                    
                } else {
                    if(typeof tampilkanToast === 'function') {
                        tampilkanToast(data.message, 'bg-danger');
                    } else {
                        alert(data.message);
                    }
                }
            })
            .catch(function(error) {
                console.error(error);
                btn.innerHTML = originalHtml;
                btn.disabled = false;
                
                if(typeof tampilkanToast === 'function') {
                    tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan saat menyinkronkan nilai.")%>', 'bg-danger');
                } else {
                    alert('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>');
                }
            });
        });
    }

    // =================================================================================
    // FUNGSI PEMANGGIL LAPORAN PDF
    // =================================================================================
    function cetakLaporanNilai_<%=itemId%>(idPerkuliahan) {
        if (typeof tampilkanToast === 'function') {
            tampilkanToast('<%= Common.getBahasaConfigJS("Sedang memproses dan menyusun laporan PDF. Harap tunggu...") %>', 'bg-info');
        }

        const formData = new URLSearchParams();
        formData.append('perkuliahanId', idPerkuliahan);

        fetch('<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_cetak_laporan_nilai', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(response => {
            if (!response.ok) throw new Error('Koneksi server gagal.');
            return response.json();
        })
        .then(data => {
            if (data.status === 'success') {
                tampilkanPdfDiPopup_<%=itemId%>(data.url);
            } else {
                alert('<%= Common.getBahasaConfigJS("Gagal mencetak: ") %>' + data.message);
            }
        })
        .catch(error => {
            console.error(error);
            alert('<%= Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat memproses PDF.") %>');
        });
    }

    // FUNGSI MENAMPILKAN PDF DI MODAL (KUSTOM BOOTSTRAP MODAL)
    function tampilkanPdfDiPopup_<%=itemId%>(pdfUrl) {
        if (!document.getElementById('modalPdfViewer_<%=itemId%>')) {
            const modalHtml = `
            <div class="modal fade" id="modalPdfViewer_<%=itemId%>" tabindex="-1" aria-hidden="true" style="z-index: 1060;">
                <div class="modal-dialog modal-xl modal-dialog-centered" style="height: 95vh;">
                    <div class="modal-content h-100 border-0 shadow-lg">
                        <div class="modal-header bg-dark text-white py-2">
                            <h6 class="modal-title fw-bold"><i class="fas fa-file-pdf text-danger me-2"></i> <%= Common.getBahasaConfig("Pratinjau Dokumen Laporan") %></h6>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body p-0 bg-secondary">
                            <iframe id="iframePdfViewer_<%=itemId%>" src="" style="width: 100%; height: 100%; border: none;"></iframe>
                        </div>
                    </div>
                </div>
            </div>`;
            document.body.insertAdjacentHTML('beforeend', modalHtml);
        }
        document.getElementById('iframePdfViewer_<%=itemId%>').src = pdfUrl;
        const modalInstance = new bootstrap.Modal(document.getElementById('modalPdfViewer_<%=itemId%>'));
        modalInstance.show();
    }


    // =================================================================================
    // FUNGSI PEMUATAN DATA UI E-LEARNING (LAZY LOAD) & PROTEKSI HAK UNDUH
    // =================================================================================
    async function reloadTugas<%=itemId%>(withDelay) {
        if (withDelay) {
            setTimeout(() => loadInfoPertemuan(), 500);
            setTimeout(() => loadPesertaTugas(), 500);
        } else {
            loadInfoPertemuan();
            loadPesertaTugas();
        }
    }

    async function loadInfoPertemuan() {
        const targetEl = document.getElementById("info_pertemuan_<%=itemId%>");
        const url = `<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=_info_kehadiran_pertemuan_horizontal&pertemuan=<%=itemId%>`;
        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error("HTTP " + response.status);
            const html = await response.text();
            targetEl.innerHTML = html;
        } catch (error) {
            targetEl.innerHTML = `<div class="text-center text-danger small py-2"><%=Common.getBahasaConfig("Gagal memuat info pertemuan")%></div>`;
        }
    }

    async function loadPesertaTugas() {
        const targetEl = document.getElementById("daftar_peserta_tugas_<%=itemId%>");
        targetEl.innerHTML = `
            <div class="d-flex align-items-center justify-content-center py-5">
                <div class="spinner-border text-primary spinner-border-sm me-2" role="status"></div>
                <span class="text-muted small"><%=Common.getBahasaConfig("Memuat data...")%></span>
            </div>`;
            
        // Peringatan ke backend via parameter agar membantu proteksi ganda jika API backend mendukungnya.
        const url = `<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=tugas_peserta&pertemuan=<%=itemId%>&id=<%= (tugasdata != null) ? tugasdata.getId() : "" %>&jenis=<%= (paramJenis != null) ? paramJenis : "" %>&restrictOtherStudent=true`;
        
        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error("HTTP " + response.status);
            const html = await response.text();
            targetEl.innerHTML = html;

            // PROTEKSI FRONT-END: Secara paksa menyembunyikan tautan atau tombol unduhan milik peserta lain jika yang login adalah Siswa/Mahasiswa
            <% if (isStudentLogin && !bolehUbah) { %>
                setTimeout(() => {
                    try {
                        let myInduk = '<%= userInduk %>';
                        // Mencari semua baris tabel atau item grid di dalam daftar peserta
                        let rows = targetEl.querySelectorAll('table tbody tr, .row, .list-group-item');
                        
                        rows.forEach(row => {
                            // Jika teks yang terdapat pada baris/elemen tersebut TIDAK mengandung NIS/NIM pengguna yang sedang masuk
                            if (myInduk !== '' && !row.textContent.includes(myInduk)) {
                                
                                // Deteksi segala bentuk file download (elemen 'a' atau 'button' dengan kriteria unduh)
                                let downloadLinks = row.querySelectorAll('a, button');
                                downloadLinks.forEach(link => {
                                    let contentLow = link.innerHTML.toLowerCase();
                                    let hrefLow = link.href ? link.href.toLowerCase() : '';
                                    
                                    // Sembunyikan jika mengandung indikasi akses unduh tugas
                                    if (contentLow.includes('fa-download') || contentLow.includes('fa-file') || hrefLow.includes('download')) {
                                        link.style.display = 'none';
                                        
                                        // Tambahkan label Gembok/Terkunci sebagai penanda untuk tugas milik mahasiswa/siswa lain
                                        if(!link.nextElementSibling || !link.nextElementSibling.classList.contains('locked-badge')) {
                                            link.insertAdjacentHTML('afterend', '<span class="badge bg-secondary opacity-50 px-2 py-1 locked-badge" title="<%=Common.getBahasaConfig("Hanya pemilik tugas yang diizinkan untuk mengunduh berkas ini.")%>"><i class="fa fa-lock me-1"></i> <%=Common.getBahasaConfig("Rahasia")%></span>');
                                        }
                                    }
                                });
                            }
                        });
                    } catch(e) { console.error('Gagal memproteksi tautan unduhan:', e); }
                }, 300); // Penundaan 300ms untuk memastikan DOM termuat sepenuhnya
            <% } %>

        } catch (error) {
            targetEl.innerHTML = `
                <div class="text-center py-4 text-muted">
                    <i class="fa fa-wifi fa-2x mb-2 opacity-25"></i>
                    <p class="small mb-0"><%=Common.getBahasaConfig("Koneksi gagal")%></p>
                </div>`;
        }
    }

    // Init load
    reloadTugas<%=itemId%>(false);
</script>