<%@page import="ais.action.master.helper.AbsensiHelper"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.common.JSPCurdGenerator"%>
<%@page import="ais.database.model.file.TugasFileContent"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.TugasPertemuan"%>
<%@page import="ais.database.model.Tugas"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.database.model.VOPembelajaran"%>
<%@page import="java.util.List"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="ais.database.model.PengajuanIzinTidakMasukPerkuliahan"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.DynamicFormGenerator"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%
try {

	// ==========================================
	// 1. SECURITY & PARAMETER HANDLING
	// ==========================================
	Tbmuser tbmuser = Common.getCurrentUser(request);
	if (tbmuser == null || tbmuser.getUserId() == null) {
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		return;
	}

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
	boolean hasError = false;

	try {

		pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, itemId.trim(), true);

		if (pertemuan == null || pertemuan.getId() == null || !pertemuan.getAktif()) {
	out.print(Common.getBahasaConfig("Data pertemuan tidak valid atau tidak aktif"));
	return;
		}

		if (paramTugas != null && !paramTugas.trim().isEmpty() && paramJenis != null) {
	String tugasIdClean = paramTugas.trim();

	// PERBAIKAN: Mengambil instance dari class yang tepat untuk mencegah ClassCastException
	if ("Pertemuan".equalsIgnoreCase(paramJenis)) {
		tugasdata = (Tugas) GeneralValueObject.ambilData(Tugas.class, tugasIdClean, true);
	} else if ("TugasPertemuan".equalsIgnoreCase(paramJenis)) {
		tugasdata = (Tugas) GeneralValueObject.ambilData(TugasPertemuan.class, tugasIdClean, true);
	}
		}
	} catch (Exception e) {
		e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/tugas/_upload_tugas.jsp:72");
		hasError = true;
	} finally {

	}

	if (tugasdata == null) {
		out.print("<div class='alert alert-warning'>" + Common.getBahasaConfig("Tugas tidak ditemukan") + "</div>");
		return;
	}

	if (hasError) {
		out.print("<div class='alert alert-danger'>" + Common.getBahasaConfig("Terjadi kesalahan saat memuat data")
		+ "</div>");
		return;
	}

	String idtugas = tugasdata.getId() != null ? tugasdata.getId().toString() : "";
	
	
	String randomID = Common.getGeneratedBarCode(7);
	String col = "mahasiswa";
	String label = "Mahasiswa";
	String returnName = Mahasiswa.class.getName();
	String contentModal = request.getParameter("contentModal");
	String rnd = Common.getGeneratedBarCode(7);
	String kodeUpload = Common.getGeneratedBarCode(16);
	
	
	
	
	List<Long> mahasiswas = new ArrayList<Long>();
	for (GeneralValueObject mhs : AbsensiHelper.populateMahasiswaDariPertemuan(pertemuan)) {
		if (!tugasdata.getMhsYgTidakIkut().contains("," + mhs.getId() + ",")) {
			mahasiswas.add(mhs.getId());
		}
	}
	
	String whereMhs;
	if (mahasiswas.isEmpty()) {
		whereMhs = "-111";
	} else {
		StringBuilder sbMhs = new StringBuilder();
		for (Long mhsId : mahasiswas) {
			if (sbMhs.length() > 0) sbMhs.append(",");
			sbMhs.append(mhsId);
		}
		whereMhs = sbMhs.toString();
		if (whereMhs.isEmpty()) whereMhs = "-111";
	}
	String whereTambahan = " and this_.id in ("+whereMhs+")";
	//String afterSaveData = "loadDataTugasData"+itemId+"(true);";
	String afterSaveData = "";
	String beforeSave = "if(chekSubdata"+rnd+"('mahasiswa')==false){tampilkanToast('"+Common.getBahasaConfigJS("Mahasiswa harus dipilih dulu")+"', 'bg-success');return;}";
	
	%>
	
	

	            
	            <div class="card shadow border-0 rounded-4">
	                
	                <div class="card-header bg-white border-bottom pt-4 pb-3 px-4 d-flex justify-content-between align-items-center rounded-top-4">
	                    <h5 class="card-title fw-bold mb-0 text-dark"><%=Common.getBahasaConfig("Upload Tugas") %></h5>
	                </div>
	
	                <div class="card-body p-4">
	                    
	                    <div class="row align-items-center mb-4">
	                    
	                    	
	                    
	                        <label for="selectMahasiswa" class="col-sm-3 col-form-label text-secondary fw-semibold"><%=Common.getBahasaConfig("Mahasiswa") %></label>
	                        
	                        
	                        <div class="col-sm-9">
	                            <%	
	                            String onEvent = "setSubdata"+rnd+"('mahasiswa', prov.id);";
	                        		String s = JSPCurdGenerator.pilihan(randomID, col, label, returnName, true, "id", "nama", whereTambahan, "input", onEvent);
	                        		out.println(s);
	                        	%>
	                        </div>
	                    </div>
	                    
	                    <div class="mb-4">
		                	<label class="form-label fw-bold mb-3"><%= Common.getBahasaConfig("Upload Tugas:") %></label>
		                    <jsp:include page="/WEB-INF/baru/modul/common/upload_component.jsp">
		                        <jsp:param name="clazz" value="<%=TugasFileContent.class.getName()%>" />
                        		<jsp:param name="jenis" value="<%=TugasFileContent.DEFAULT_JENIS%>" />
		                        <jsp:param name="rnd" value="<%= rnd %>" />
		                        <jsp:param name="id" value="<%=idtugas %>" />
		                        <jsp:param name="col" value="file_soal" />
		                        <jsp:param name="label" value="Soal" />
		                        <jsp:param name="afterSave" value="<%=afterSaveData %>" />
		                        <jsp:param name="beforeSave" value="<%=beforeSave %>" />
		                        <jsp:param name="loadPreview" value="false" />
		                    </jsp:include> 
		                </div> 
	
	                    <div class="text-muted small lh-lg">
	                        <%=Common.getBahasaConfig("Jika file yang Anda upload lebih dari satu file, zip / compress / jadikan satu file terlebih dulu, kemudian baru di-upload") %>
	                    </div>
	
	                </div>
	            </div>
	
	        <script>
	        	setTimeout(function(){
	        		setSubdata<%=rnd%>('kodeUpload','<%=kodeUpload%>');
	        		setSubdata<%=rnd%>('pertemuan','<%=tugasdata.getId()%>');
	        		setSubdata<%=rnd%>('classFrom','<%=tugasdata.getClass().getName() %>');
	        		<%if(tbmuser.getMahasiswa() != null){ %>
	        			setSubdata<%=rnd%>('mahasiswa','<%=tbmuser.getMahasiswa().getId() %>');
	        		<%} %>
	        		<%if(tbmuser.getSiswa() != null){ %>
	        			setSubdata<%=rnd%>('siswa','<%=tbmuser.getSiswa().getId() %>');
	        		<%} %>
	        		<%if(tbmuser.getSiswa() != null){ %>
	        			setSubdata<%=rnd%>('calonSiswa','<%=tbmuser.getCalonSiswa().getId() %>');
	        		<%} %>
	        		<%if(tbmuser.getBiodataCalonMahasiswa() != null){ %>
	        			setSubdata<%=rnd%>('biodataCalonMahasiswa','<%=tbmuser.getBiodataCalonMahasiswa().getId() %>');
	        		<%} %>
	        	},2000);
	        </script>
	
	<%

} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/tugas/_upload_tugas.jsp:201");
}
%>



