

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


<link href="<%=request.getContextPath()%>/css/diskusi.css"
	rel="stylesheet">
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
Pertemuan pertemuan = (Pertemuan) request.getAttribute("pertemuan");

String posting = request.getParameter("posting");
String parentData = request.getParameter("parent");
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

	if (fieldname.equalsIgnoreCase("parent")) {
		parentData = fieldvalue;
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
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/o/ux/content/component/diskusi/diskusi_elearning.jsp:88");

}

if (hapus != null && hapus.equalsIgnoreCase("true")) {
	PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) (parentData == null
	? null
	: GeneralValueObject.ambilData(PertemuanPunyaDiskusi.class, parentData));
	if (pertemuanPunyaDiskusi != null) {
		Session session2 = HibernateUtil.currentNativeSession();
		session2.getTransaction().begin();
		session2.createSQLQuery(
		"delete from pertemuan_punya_diskusi where parent in (select id from pertemuan_punya_diskusi where parent="
				+ pertemuanPunyaDiskusi.getId() + ")")
		.executeUpdate();

		session2.createSQLQuery("delete from pertemuan_punya_diskusi where parent=" + pertemuanPunyaDiskusi.getId())
		.executeUpdate();

		Common.refreshDelete(session2, pertemuanPunyaDiskusi);
		session2.getTransaction().commit();

		pertemuan.reInitPertemuanPunyaDiskusi(session2);

		session2.disconnect();
		session2.close();
		HibernateUtil.closeSession();
	}
}

else if (posting != null && !posting.isEmpty()) {
	PertemuanPunyaDiskusi parent = (PertemuanPunyaDiskusi) (parentData == null
	? null
	: GeneralValueObject.ambilData(PertemuanPunyaDiskusi.class, parentData));
	PertemuanPunyaDiskusi pertemuanPunyaDiskusi;
	if (parent != null && ubah != null && ubah.equalsIgnoreCase("true")) {
		pertemuanPunyaDiskusi = parent;
	} else {
		pertemuanPunyaDiskusi = new PertemuanPunyaDiskusi();
		pertemuanPunyaDiskusi.setParent(parent);
		pertemuanPunyaDiskusi.setPertemuan(pertemuan);
		pertemuanPunyaDiskusi.setMahasiswa(tbmuser.getMahasiswa());
		pertemuanPunyaDiskusi.setDosen(tbmuser.getDosen());
		pertemuanPunyaDiskusi.setSiswa(tbmuser.getSiswa());
		pertemuanPunyaDiskusi.setGuru(tbmuser.getGuru());
		pertemuanPunyaDiskusi.setBiodataCalonMahasiswa(tbmuser.getBiodataCalonMahasiswa());
		pertemuanPunyaDiskusi.setTbmuser(tbmuser);
	}

	pertemuanPunyaDiskusi.setIsi(posting);
	Session session2 = HibernateUtil.currentNativeSession();
	session2.getTransaction().begin();
	session2.saveOrUpdate(pertemuanPunyaDiskusi);
	session2.getTransaction().commit();

	pertemuan.reInitPertemuanPunyaDiskusi(session2);

	session2.disconnect();
	session2.close();
	HibernateUtil.closeSession();

	if (outputfile != null && outputfile.exists()) {
		String olehId = Common.generateOlehId(tbmuser);
		Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();

		LampiranLain lainMahasiswa = new LampiranLain();
		lainMahasiswa.setRef(pertemuanPunyaDiskusi.getId());
		lainMahasiswa.setNama(outputfile.getName());
		lainMahasiswa.setLink(null);
		String mimeType = URLConnection.guessContentTypeFromName(outputfile.getName());
		lainMahasiswa.setKeterangan(mimeType);
		lainMahasiswa.setJenis(LampiranLain.DISKUSI);
		lainMahasiswa.setOlehId(olehId);
		lainMahasiswa.setOleh(tbmuser == null
		? "external_update"
		: mahasiswa != null
				? mahasiswa.getNama()
				: dosen != null
						? dosen.getNama()
						: pegawai != null ? pegawai.getNama() : (tbmuser.getUserNama()));

		try {

	lainMahasiswa.setFoto(
			Hibernate.createBlob(IOUtils.toByteArray(new FileInputStream(outputfile.getAbsolutePath()))));

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

}
%>

<div class="container bootdey">
	<div class="col-md-12 bootstrap snippets">
		<div class="panel">
			<form action="" method="post" enctype="multipart/form-data">
				<div class="panel-body">
					<textarea class="form-control" rows="2" name="posting"
						required="required" placeholder="Apa yang ingin anda tanyakan?"></textarea>

					<input type="file" style="display: none;"
						onchange="loadFile<%=pertemuan.getId()%>(event)"
						id="file_<%=pertemuan.getId()%>" name="file" />

					<div id="file_label_<%=pertemuan.getId()%>"></div>

					<script>
				var loadFile<%=pertemuan.getId()%> = function(event) {
					var output = document.getElementById('file_label_<%=pertemuan.getId()%>');
					if(event.target.files[0]){
						output.innerHTML = event.target.files[0].name;
					}
					
				};
			</script>

					<div class="mar-top clearfix btn-group">

						<button class="btn btn-sm btn-primary pull-right"
							onclick="document.getElementById('file_<%=pertemuan.getId()%>').click();"
							style="padding-left: 50px" type="button">
							<i class="fa fa-paperclip fa-fw"></i> Lampiran
						</button>

						<button class="btn btn-sm btn-primary pull-right" type="submit">
							<i class="fa fa-pencil fa-fw"></i> Kirim
						</button>
					</div>

				</div>
			</form>
		</div>


		<div class="panel">
			<div class="panel-body">


				<%
				TreeSet<Long> pertemuanPunyaDiskusisa = pertemuan.ambilPertemuanPunyaDiskusiTotal(false);
				request.setAttribute("pertemuanPunyaDiskusisa", pertemuanPunyaDiskusisa);

				for (Long pertemuanPunyaDiskusiId : pertemuanPunyaDiskusisa) {
					if (pertemuanPunyaDiskusiId != null) {
						PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) GeneralValueObject
						.ambilData(PertemuanPunyaDiskusi.class, pertemuanPunyaDiskusiId.toString());

						if (pertemuanPunyaDiskusi != null && pertemuanPunyaDiskusi.getParent() == null) {

					request.setAttribute("pertemuanPunyaDiskusi", pertemuanPunyaDiskusi);

					String oleh = pertemuanPunyaDiskusi.getBiodataCalonMahasiswa() != null
							? (pertemuanPunyaDiskusi.getBiodataCalonMahasiswa().getNama() + " (Calon Mahasiswa)")
							: (pertemuanPunyaDiskusi.getMahasiswa() != null
									? pertemuanPunyaDiskusi.getMahasiswa().getNama() + " (Mahasiswa)"
									: "");

					try {
						if (oleh.trim().equals("")) {
							oleh = pertemuanPunyaDiskusi.getDosen() != null
									? pertemuanPunyaDiskusi.getDosen().getNama() + " (Dosen)"
									: "";
						}

						if (oleh.trim().equals("")) {
							oleh = pertemuanPunyaDiskusi.getTbmuser() != null ? pertemuanPunyaDiskusi.getTbmuser().getUserNama()
									+ " (" + pertemuanPunyaDiskusi.getTbmuser().hakAkses().getRoleName() + ")" : "";
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/o/ux/content/component/diskusi/diskusi_elearning.jsp:268");
						// TODO: handle exception
					}

					Tbmuser usrkomentar = pertemuanPunyaDiskusi.getTbmuser();
					String foto = "https://bootdey.com/img/Content/avatar/avatar1.png";
					if (pertemuanPunyaDiskusi.getMahasiswa() != null) {
						foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusi.getMahasiswa()), 152, 114);
					} else if (pertemuanPunyaDiskusi.getDosen() != null) {
						foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusi.getDosen()), 152, 114);
					} else if (pertemuanPunyaDiskusi.getBiodataCalonMahasiswa() != null) {
						foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusi.getBiodataCalonMahasiswa()),
								152, 114);
					} else if (pertemuanPunyaDiskusi.getSiswa() != null) {
						foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusi.getSiswa()), 152, 114);
					} else if (pertemuanPunyaDiskusi.getCalonSiswa() != null) {
						foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusi.getCalonSiswa()), 152, 114);
					} else if (pertemuanPunyaDiskusi.getGuru() != null) {
						foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusi.getGuru()), 152, 114);
					} else {
						foto = CommonMedia.getUrlFotoPengguna(pertemuanPunyaDiskusi.getTbmuser(), 152, 114);
					}

					String waktu = pertemuanPunyaDiskusi.getTanggal_dirubah() == null ? ""
							: SmartDateTimeUtil.getDayString(pertemuanPunyaDiskusi.getTanggal_dirubah(), null)
									+ Common.dateFormat5.get().format(pertemuanPunyaDiskusi.getTanggal_dirubah());
				%>

				<!-- Newsfeed Content -->
				<!--===================================================-->
				<div class="media-block" id="panel_<%=pertemuanPunyaDiskusiId%>">
					<a class="media-left" href="#"><img class="img-circle img-sm"
						alt="Profile Picture" src="<%=foto%>"></a>
					<div class="media-body">
						<div class="mar-btm">
							<a href="#"
								class="btn-link text-semibold media-heading box-inline"><%=oleh%></a>
							<p class="text-muted text-sm">
								<i class="far fa-clock"></i>
								<%=waktu%>
							</p>
						</div>
						<p><%=pertemuanPunyaDiskusi.getIsi()%></p>

						<jsp:include
							page="/WEB-INF/o/ux/content/component/diskusi/diskusi_controller.jsp">
							<jsp:param value="${pertemuanPunyaDiskusi}"
								name="pertemuanPunyaDiskusi" />
						</jsp:include>

						<hr>



						<jsp:include
							page="/WEB-INF/o/ux/content/component/diskusi/sub_diskusi_elearning.jsp">
							<jsp:param value="${pertemuan}" name="pertemuan" />
							<jsp:param value="${pertemuanPunyaDiskusi}"
								name="pertemuanPunyaDiskusi" />
							<jsp:param value="${pertemuanPunyaDiskusisa}"
								name="pertemuanPunyaDiskusisa" />
						</jsp:include>


					</div>
				</div>
				<!--===================================================-->
				<!-- End Newsfeed Content -->

				<%
				}
				}
				}
				%>

			</div>
		</div>

	</div>
</div>