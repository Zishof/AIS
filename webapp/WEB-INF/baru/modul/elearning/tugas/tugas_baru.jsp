<%@page import="ais.common.GenCodeHelper"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.TugasPertemuan"%>
<%@page import="ais.database.model.TugasKelompok"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.Tugas"%>
<%@page import="ais.common.Common"%>
<%
    // --- 1. Validasi User Login ---
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        return;
        // Stop jika tidak ada sesi user
    }
    String itemId = request.getParameter("pertemuan");
    Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, itemId.trim(), true);

    if (pertemuan == null || pertemuan.getId() == null || !pertemuan.getAktif()) {
        return;
    }
    String contentModal = request.getParameter("contentModal");
	String rnd = contentModal==null || contentModal.trim().isEmpty() ?  Common.getGeneratedBarCode(7) : contentModal;

    // --- 2. Inisialisasi Variable ---
    String jenis = request.getParameter("jenis");
    String idStr = request.getParameter("id");
    String afterSave = request.getParameter("afterSave")==null?"":request.getParameter("afterSave").trim();
    
    Session mySession = null;
    Tugas tugas = null;
    Class clazz = Pertemuan.class;
    if (jenis != null && jenis.equalsIgnoreCase(TugasPertemuan.class.getSimpleName())) {
        clazz = TugasPertemuan.class;
    } else if (jenis != null && jenis.equalsIgnoreCase(TugasKelompok.class.getSimpleName())) {
        clazz = TugasKelompok.class;
    }

    try {
        mySession = HibernateUtil.openSession();
        Long id = null;
        if (idStr != null && !idStr.isEmpty()) {
            try { id = Long.parseLong(idStr);
            } catch (NumberFormatException e) { id = null; }
        }

        if (id != null && jenis != null) {
            if (jenis.equalsIgnoreCase(TugasPertemuan.class.getSimpleName())) {
                tugas = (TugasPertemuan) mySession.get(TugasPertemuan.class, id);
            } 
        }
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/tugas/tugas_baru.jsp:58");
    } finally {
        ais.common.ElearningSessionUtil.closeQuietly(mySession);
    }
    
    if (jenis != null && jenis.equalsIgnoreCase(Pertemuan.class.getSimpleName())) {
    	tugas = pertemuan;
    }
    
    String tglMulai = (tugas != null && tugas.getMulai() != null) ? Common.dateFormatInput.get().format(tugas.getMulai()) : "";
    String tglSelesai = (tugas != null && tugas.getSelesai() != null) ? Common.dateFormatInput.get().format(tugas.getSelesai()) : "";
    String placeholder = Common.getBahasaConfig("Tuliskan detail tugas di sini...");
    String isi = tugas==null?"":tugas.getIsitugas();
    String idtugas = tugas != null && tugas.getId() != null ? tugas.getId().toString() : "";
    String afterSaveData = "loadDataTugas"+itemId+"();";
%>

<div class="col-lg-12" id="form_tugas_<%=rnd%>">
	<% if(pertemuan != null && pertemuan.getId() != null){ %>
		<input type="hidden" name="pertemuan" id="pertemuan" value="<%=pertemuan.getId()%>">
	<%} %>
	<% if(tugas != null && tugas.getId() != null){ %>
		<input type="hidden" name="id" id="id" value="<%=tugas.getId()%>">
	<%} %>
	<% if(pertemuan.getPerkuliahan() != null){ %>
		<input type="hidden" name="perkuliahan" id="perkuliahan" value="<%=pertemuan.getPerkuliahan().getId()%>">
	<%} %>
	<% if(pertemuan.getJadwalPelajaran() != null){ %>
		<input type="hidden" name="jadwalPelajaran" id="jadwalPelajaran" value="<%=pertemuan.getJadwalPelajaran().getId()%>">
	<%} %>
	<% if(pertemuan.getSkripsi() != null){ %>
		<input type="hidden" name="skripsi" id="skripsi" value="<%=pertemuan.getSkripsi().getId()%>">
	<%} %>
	<% if(pertemuan.getMahasiswaRequestTugasAkhir() != null){ %>
		<input type="hidden" name="mahasiswaRequestTugasAkhir" id="mahasiswaRequestTugasAkhir" value="<%=pertemuan.getMahasiswaRequestTugasAkhir().getId()%>">
	<%} %>
	<% if(pertemuan.getKrsMahasiswa() != null){ %>
		<input type="hidden" name="krsMahasiswa" id="krsMahasiswa" value="<%=pertemuan.getKrsMahasiswa().getId()%>">
	<%} %>
	<% if(pertemuan.getJadwalUjianPSB() != null){ %>
		<input type="hidden" name="jadwalUjianPSB" id="jadwalUjianPSB" value="<%=pertemuan.getJadwalUjianPSB().getId()%>">
	<%} %>
	<% if(pertemuan.getJadwalUjianPMB() != null){ %>
		<input type="hidden" name="jadwalUjianPMB" id="jadwalUjianPMB" value="<%=pertemuan.getJadwalUjianPMB().getId()%>">
	<%} %>
	<% if(pertemuan.getKelompokKkn() != null){ %>
		<input type="hidden" name="kelompokKkn" id="kelompokKkn" value="<%=pertemuan.getKelompokKkn().getId()%>">
	<%} %>
	<% if(pertemuan.getKelompokPkl() != null){ %>
		<input type="hidden" name="kelompokPkl" id="kelompokPkl" value="<%=pertemuan.getKelompokPkl().getId()%>">
	<%} %>
	<% if(pertemuan.getJadwalUjianPegawai() != null){ %>
		<input type="hidden" name="jadwalUjianPegawai" id="jadwalUjianPegawai" value="<%=pertemuan.getJadwalUjianPegawai().getId()%>">
	<%} %>
	<% if(pertemuan.getKelasLesSiswa() != null){ %>
		<input type="hidden" name="kelasLesSiswa" id="kelasLesSiswa" value="<%=pertemuan.getKelasLesSiswa().getId()%>">
	<%} %>
	<% if(pertemuan.getPertemuanPunyaGrupPertemuan() != null){ %>
		<input type="hidden" name="pertemuanPunyaGrupPertemuan" id="pertemuanPunyaGrupPertemuan" value="<%=pertemuan.getPertemuanPunyaGrupPertemuan().getId()%>">
	<%} %>
	<% if(pertemuan.getFormulirKegiatan() != null){ %>
		<input type="hidden" name="formulirKegiatan" id="formulirKegiatan" value="<%=pertemuan.getFormulirKegiatan().getId()%>">
	<%} %>
	<% if(pertemuan.getKomponenDataProdukKursus() != null){ %>
		<input type="hidden" name="komponenDataProdukKursus" id="komponenDataProdukKursus" value="<%=pertemuan.getKomponenDataProdukKursus().getId()%>">
	<%} %>
	
    <div class="card shadow border-0 rounded-3">
        <div class="card-header bg-white border-bottom p-4 d-flex justify-content-between align-items-center">
            <h4 class="mb-0 fw-bold text-primary">
                <i class="fa fa-clipboard-list me-2"></i><%= Common.getBahasaConfig("Perintah Tugas") %>
            </h4>
        </div>

        <div class="card-body p-4">
            <form>
                <div class="row g-4 mb-4">
                    <div class="col-md-6">
                        <label class="form-label fw-semibold"><%= Common.getBahasaConfig("Tugas Mulai") %></label>
                        <div class="input-group">
                            <span class="input-group-text bg-light"><i class="fa fa-calendar-plus text-success"></i></span> 
                            <input type="datetime-local" class="form-control" id="mulai" name="mulai" value="<%= tglMulai %>">
                        </div>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label fw-semibold"><%= Common.getBahasaConfig("Tugas Selesai") %></label>
                        <div class="input-group">
                            <span class="input-group-text bg-light"><i class="fa fa-calendar-check text-danger"></i></span> 
                            <input type="datetime-local" class="form-control" id="selesai" name="selesai"  value="<%= tglSelesai %>">
                        </div>
                    </div>
                </div>

                <div class="mb-4">
                    <label class="form-label fw-semibold">
                        <%= Common.getBahasaConfig((jenis != null && TugasKelompok.class.getSimpleName().equalsIgnoreCase(jenis)) ? "Judul Tugas Kelompok" : "Judul Tugas Individu") %> <span class="text-danger">*</span>
                    </label>
                    <input type="text" class="form-control form-control-lg" id="judultugas" name="judultugas" placeholder="<%= Common.getBahasaConfig("Masukkan judul tugas di sini") %>" value='<%= tugas != null ? tugas.getJudultugas() : "" %>'>
                </div>

                <div class="mb-4">
                    <label class="form-label fw-bold mb-2"><%= Common.getBahasaConfig("Pengaturan Format Nilai:") %></label>
                    <div class="p-3 border rounded bg-light d-flex flex-wrap justify-content-between align-items-center">
                        <div class="mb-2 mb-md-0">
                            <h6 class="mb-1 fw-bold text-dark"><i class="fa fa-sliders-h text-info me-2"></i><%= Common.getBahasaConfig("Integrasi Penilaian") %></h6>
                            <small class="text-muted"><%= Common.getBahasaConfig("Tentukan format CPMK atau Kategori Penilaian untuk bobot tugas ini.") %></small>
                        </div>
                        <button type="button" class="btn btn-outline-primary bg-white shadow-sm fw-bold px-4 rounded-pill" onclick="bukaFormatNilaiTugas<%=rnd%>()">
                            <i class="fa fa-th-list me-2"></i> <%= Common.getBahasaConfig("Atur Format Nilai") %>
                        </button>
                    </div>
                </div>
                <div class="mb-4">
                	<label class="form-label fw-bold mb-3"><%= Common.getBahasaConfig("Upload Soal:") %></label>
                    <jsp:include page="/WEB-INF/baru/modul/common/upload_component.jsp">
                        <jsp:param name="clazz" value="<%=LampiranLain.class.getName()%>" />
                        <jsp:param name="jenis" value="<%=LampiranLain.TUGAS_MANDIRI_PERKULIAHAN%>" />
                        <jsp:param name="rnd" value="<%= rnd %>" />
                        <jsp:param name="id" value="<%=idtugas %>" />
                        <jsp:param name="col" value="file_soal" />
                        <jsp:param name="label" value="Soal" />
                        <jsp:param name="afterSave" value="<%=afterSaveData %>" />
                    </jsp:include> 
                </div>
				
                <div class="mb-4">
                    <label class="form-label fw-semibold"><%= Common.getBahasaConfig("Tata Cara dan Langkah Pengerjaan:") %> <span class="text-danger">*</span></label>
                    <jsp:include page="/WEB-INF/baru/modul/common/text_area.jsp">
                        <jsp:param name="placeholder" value="<%= placeholder %>" />
                        <jsp:param name="idtextarea" value="isitugas" />
                        <jsp:param name="isi" value="<%= isi %>" />
                        <jsp:param name="namatextarea" value="isitugas" />
                    </jsp:include>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    // Fungsi untuk membuka modal format nilai
    window.bukaFormatNilaiTugas<%=rnd%> = function() {
        var idInput = document.getElementById("id");
        var idPertemuan = document.getElementById("pertemuan") ? document.getElementById("pertemuan").value : "";
        var jenisTugas = '<%=jenis != null ? jenis : ""%>';

        // Validasi agar tugas disimpan terlebih dahulu sebelum bisa mengatur nilai
        if (!idInput || idInput.value === "") {
            alert('<%= Common.getBahasaConfigJS("Silakan lengkapi dan SIMPAN TUGAS terlebih dahulu sebelum mengatur format nilai!") %>');
            return;
        }

        var idTugas = idInput.value;
        var cm = 'cm_' + (++variable);
        
        // --- PERUBAHAN DI SINI ---
        // Mengubah p=elearning/services dan s=_format_nilai_tugas_services
        var url = '<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_format_nilai_tugas_services&idTugas=' + idTugas + '&idPertemuan=' + idPertemuan + '&jenis=' + jenisTugas + '&contentModal=' + cm;
        // -------------------------

        loadModalContentCustomSimpan(
            '<%= Common.getBahasaConfigJS("Format Penilaian Tugas") %>', 
            url, 
            '75%', 
            'hidden', 
            '', 
            cm
        );
    };

	<%
	String judulLabel = (jenis != null && TugasKelompok.class.getSimpleName().equalsIgnoreCase(jenis)) ? Common.getBahasaConfig("Judul Tugas Kelompok") : Common.getBahasaConfig("Judul Tugas Individu");
	String d= GenCodeHelper.simpanDataHelper("form_tugas_"+rnd,clazz, tugas==null||tugas.getId()==null ? "" : tugas.getId().toString(), new Class[]{}, new String[]{}, rnd, afterSave, new String[]{"judultugas","isitugas"}, new String[]{judulLabel,Common.getBahasaConfig("Tata Cara dan Langkah Pengerjaan")});
	out.println(d);
	%>
</script>