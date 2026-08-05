

<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.common.YouTubeHelper"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="ais.database.model.file.PertemuanFileContent"%>
<%@page import="java.util.TreeMap"%>
<%@page import="ais.database.hibernate.StreamingHibernateUtil"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="org.hibernate.Hibernate"%>
<%@page import="ais.database.model.Pegawai"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="java.net.URLConnection"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="org.apache.commons.io.IOUtils"%>
<%@page import="java.nio.file.StandardCopyOption"%>
<%@page import="java.io.File"%>
<%@page import="org.apache.commons.io.FilenameUtils"%>
<%@page import="java.io.InputStream"%>
<%@page import="org.apache.commons.fileupload.disk.DiskFileItemFactory"%>
<%@page import="org.apache.commons.fileupload.servlet.ServletFileUpload"%>
<%@page import="org.apache.commons.fileupload.FileItem"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="java.util.List"%>
<%@page import="java.util.TreeSet"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.ui.util.SmartDateTimeUtil"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.PertemuanPunyaDiskusi"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>


<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
Pertemuan pertemuan = (Pertemuan) request.getAttribute("pertemuan");

String posting = request.getParameter("posting");
String materi_id = request.getParameter("materi_id");
String hapus = request.getParameter("hapus");
String ubah = request.getParameter("ubah");
File outputfile = null;
try {
	List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
	for (FileItem item : items) {
		if (item.isFormField()) {
	// Process regular form field (input type="text|radio|checkbox|etc", select, etc).
	String fieldname = item.getFieldName();
	String fieldvalue = item.getString();

	if (fieldname.equalsIgnoreCase("posting")) {
		posting = fieldvalue;
	}

	if (fieldname.equalsIgnoreCase("materi_id")) {
		materi_id = fieldvalue;
	}

	if (fieldname.equalsIgnoreCase("hapus")) {
		hapus = fieldvalue;
	}
	if (fieldname.equalsIgnoreCase("ubah")) {
		ubah = fieldvalue;
	}
	// ... (do your job here)
		} else {
	// Process form file field (input type="file").
	String fieldname = item.getFieldName();
	String filename = FilenameUtils.getName(item.getName());
	InputStream fileContent = item.getInputStream();

	outputfile = new File(Common.ambilREAL_PATH_REPORT() + "/" + filename);

	if (!outputfile.isDirectory()) {
		System.out.println("outputfile video -> " + outputfile.getAbsolutePath());
		java.nio.file.Files.copy(fileContent, outputfile.toPath(), StandardCopyOption.REPLACE_EXISTING);

		IOUtils.closeQuietly(fileContent);
	}
		}
	}
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/o/ux/content/component/materi/materi_elearning.jsp:91");

}

if (hapus != null && hapus.equalsIgnoreCase("true") && materi_id != null) {

	try {

		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
		streamingSession.getTransaction().begin();
		streamingSession.createSQLQuery("delete from pertemuan_file_content where id=" + materi_id).executeUpdate();
		streamingSession.getTransaction().commit();

	} catch (Exception e) {
		StreamingHibernateUtil.getInstance().rollbackTransaction();
		Common.tampilErrorJikaAdmin(e);
	}

	StreamingHibernateUtil.getInstance().closeSession();

	pertemuan.belum("pertemuan_file_content");

}

else if (posting != null && !posting.isEmpty() && materi_id != null) {
	pertemuan.belum("pertemuan_file_content");
	try {

		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
		
		PertemuanFileContent lainMahasiswa = (PertemuanFileContent)streamingSession.createCriteria(PertemuanFileContent.class)
				.add(Restrictions.idEq(Long.parseLong(materi_id))).uniqueResult();
		lainMahasiswa.setKeterangan(posting);
		
		streamingSession.getTransaction().begin();
		streamingSession.update(lainMahasiswa);
		
		streamingSession.getTransaction().commit();

	} catch (Exception e) {
		StreamingHibernateUtil.getInstance().rollbackTransaction();
		Common.tampilErrorJikaAdmin(e);
	}

	StreamingHibernateUtil.getInstance().closeSession();

	

}

else if (outputfile != null && outputfile.exists()) {

	pertemuan.belum("pertemuan_file_content");

	String olehId = Common.generateOlehId(tbmuser);
	Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
	Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
	Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();

	PertemuanFileContent lainMahasiswa = new PertemuanFileContent();
	lainMahasiswa.setPertemuan(pertemuan.getId());
	lainMahasiswa.setNama(outputfile.getName());
	lainMahasiswa.setLink(null);
	//String mimeType = URLConnection.guessContentTypeFromName(outputfile.getName());
	//lainMahasiswa.setKeterangan(mimeType);
	lainMahasiswa.setJenis(PertemuanFileContent.DEFAULT_JENIS);
	lainMahasiswa.setOlehId(olehId);
	lainMahasiswa.setOleh(tbmuser == null ? "external_update"
	: mahasiswa != null ? mahasiswa.getNama()
			: dosen != null ? dosen.getNama() : pegawai != null ? pegawai.getNama() : (tbmuser.getUserNama()));

	try {

		lainMahasiswa
		.setFoto(Hibernate.createBlob(IOUtils.toByteArray(new FileInputStream(outputfile.getAbsolutePath()))));

		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
		streamingSession.getTransaction().begin();
		streamingSession.save(lainMahasiswa);
		streamingSession.getTransaction().commit();

	} catch (Exception e) {
		StreamingHibernateUtil.getInstance().rollbackTransaction();
		Common.tampilErrorJikaAdmin(e);
	}

	StreamingHibernateUtil.getInstance().closeSession();
}

boolean boleh = tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null;
%>

<link href="<%=request.getContextPath()%>/css/materi.css"
	rel="stylesheet">
<div class="container">
	<div class="row">
		<div class="col-md-3">
			<div class="ibox float-e-margins">
				<div class="ibox-content">
					<div class="file-manager">

						<form method="post" enctype="multipart/form-data">

							<input type="file" style="display: none;" onchange="submit();"
								id="file_<%=pertemuan.getId()%>" name="file" />

						</form>

						<button class="btn btn-primary btn-block"
							onclick="document.getElementById('file_<%=pertemuan.getId()%>').click();">
							<i class="fad fa-upload"></i> Upload Materi, Audio, Video
						</button>

						<div class="hr-line-dashed"></div>
						<h5>File Materi</h5>
						<ul class="folder-list" style="padding: 0">
							<li><a href=""><i class="fad fa-folder"></i> Materi</a></li>
							<li><a href=""><i class="fad fa-microphone-alt"></i>
									Audio</a></li>
							<li><a href=""><i class="fad fa-video"></i> Video</a></li>

						</ul>

						<div class="clearfix"></div>
					</div>
				</div>
			</div>
		</div>
		<div class="col-md-9 animated fadeInRight">
			<div class="row">
				<div class="col-lg-12">

					<%
					TreeMap<Long, PertemuanFileContent> pertemuanFileContentsa = pertemuan.ambilPertemuanFileContentTotal();
					int indexKat = 0;
					for (Long idKat : pertemuanFileContentsa.keySet()) {

						try {
							PertemuanFileContent pertemuanFileContent = pertemuanFileContentsa.get(idKat);
							String textCatatan = pertemuanFileContent.getKeterangan();
							String link = pertemuanFileContent.createLinkUri();

							String waktu = pertemuanFileContent.getTanggal_dirubah() == null ? ""
							: SmartDateTimeUtil.getDayString(pertemuanFileContent.getTanggal_dirubah(), null)
									+ Common.dateFormat5.get().format(pertemuanFileContent.getTanggal_dirubah());
					%>


					<div class="file-box">
						<div class="file">
							<a href="<%=link%>" target="_blank"> <span class="corner"></span>
								<%
								String n = pertemuanFileContent.getNama() != null && pertemuanFileContent.getNama().trim().equalsIgnoreCase("link")
										? pertemuanFileContent.getLink()
										: pertemuanFileContent.getNama();

								if (n == null) {
									n = pertemuanFileContent.getNama();
								}
								n = n.length() > 50 ? n.substring(0, 50) + "..." : n;
								if (!pertemuanFileContent.getGoogleBook().isEmpty()) {
									n = pertemuanFileContent.getNama();
								}

								String iconAwesome = "fa " + PertemuanFileContent.iconAwesome(n);
								if (iconAwesome.contains("youtube")) {
									iconAwesome = "fab " + PertemuanFileContent.iconAwesome(n);
								}

								String nama = n;
								if (pertemuanFileContent.getNama().toLowerCase().endsWith("png") || pertemuanFileContent.getNama().toLowerCase().endsWith("jpg") || pertemuanFileContent.getNama().toLowerCase().endsWith("jpeg")
										|| pertemuanFileContent.getNama().toLowerCase().endsWith("gif") || pertemuanFileContent.getNama().toLowerCase().endsWith("svg")
										|| pertemuanFileContent.getNama().toLowerCase().endsWith("tiff")) {
								%>
								<div class="image" style="text-align: center;">
									<img alt="image" class="img-responsive" height="100px"
										src="<%=link%>" />
								</div> <%
 } else if (iconAwesome.contains("youtube")) {
 String contentVideo = YouTubeHelper.convertoToEmbed(pertemuanFileContent.getLink());
 %>
								<div class="image" style="text-align: center;">
									<%=contentVideo%>
								</div> <%
 } else {
 %>
								<div class="icon">
									<i class="<%=iconAwesome%>"></i>
								</div> <%
 }
 %>


								<div class="file-name">
									<%=nama%>
									<%
									if (textCatatan != null && !textCatatan.isEmpty()) {
										out.print("<br><small>" + textCatatan + "</small>");
									}
									%>
									<br> <small>Diupload: <%=waktu%></small><br>

									<form action="" method="post">
										<input type="hidden" name="materi_id" value="<%=idKat%>" />
										<div class="panel-body">
											<div class="mar-top clearfix btn-group">
												<a class="btn btn-sm btn-default btn-hover-primary"
													id="balas_button_<%=pertemuanFileContent.getId()%>"
													onclick="
											document.getElementById('balas_<%=pertemuanFileContent.getId()%>').style.display='';
											document.getElementById('balas_button_<%=pertemuanFileContent.getId()%>').style.display='none';
											">
													<i class="fad fa-edit"></i>
												</a>
												<button class="btn btn-sm btn-default btn-hover-primary"
													type="submit" value="true" name="hapus"
													onclick="return confirm('Apakah Anda ingin menghapus materi ini ?');">
													<i class="fad fa-trash"></i>
												</button>
											</div>
										</div>
									</form>

									<form id="balas_<%=pertemuanFileContent.getId()%>"
										method="post" enctype="multipart/form-data"
										style="display: none">
										<input type="hidden" name="materi_id" value="<%=idKat%>" />
										<div class="panel-body">
											<textarea class="form-control" rows="5" name="posting"
												required="required" placeholder="Keteranga materi ?"><%=pertemuanFileContent.getKeterangan()%></textarea>
											<div class="mar-top clearfix btn-group">

												<button class="btn btn-sm btn-default btn-hover-primary"
													onclick="
											document.getElementById('balas_<%=pertemuanFileContent.getId()%>').style.display='none';
											document.getElementById('balas_button_<%=pertemuanFileContent.getId()%>').style.display='';
											"
													style="padding-left: 50px" type="button">
													<i class="fa fa-ban fa-fw"></i> Batal
												</button>

												<button class="btn btn-sm btn-default btn-hover-primary"
													type="submit">
													<i class="fa fa-pencil fa-fw"></i> Kirim
												</button>
											</div>

										</div>
									</form>
								</div>
							</a>
						</div>

					</div>

					<%
					} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/ux/content/component/materi/materi_elearning.jsp:348");
					}
					}
					%>

				</div>
			</div>
		</div>
	</div>
</div>