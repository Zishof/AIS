<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="java.util.TreeSet"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.ui.util.SmartDateTimeUtil"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.PertemuanPunyaDiskusi"%>
<%@page import="ais.database.model.Pertemuan"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
Pertemuan pertemuan = (Pertemuan) request.getAttribute("pertemuan");
request.setAttribute("pertemuan", pertemuan);
PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) request.getAttribute("pertemuanPunyaDiskusi");
TreeSet<Long> pertemuanPunyaDiskusisa = (TreeSet<Long>) request.getAttribute("pertemuanPunyaDiskusisa");
request.setAttribute("pertemuanPunyaDiskusisa", pertemuanPunyaDiskusisa);
List<Long> pertemuanPunyaDiskusis = pertemuan.ambilPertemuanPunyaDiskusi(pertemuanPunyaDiskusi, pertemuanPunyaDiskusisa,
		0, 1000);

for (Long pertemuanPunyaDiskusiIdSub : pertemuanPunyaDiskusis) {
	if (pertemuanPunyaDiskusiIdSub != null) {
		PertemuanPunyaDiskusi pertemuanPunyaDiskusiSub = (PertemuanPunyaDiskusi) GeneralValueObject
		.ambilData(PertemuanPunyaDiskusi.class, pertemuanPunyaDiskusiIdSub.toString());

		if (pertemuanPunyaDiskusiSub != null) {

	request.setAttribute("pertemuanPunyaDiskusi", pertemuanPunyaDiskusiSub);

	String oleh = pertemuanPunyaDiskusiSub.getBiodataCalonMahasiswa() != null
			? (pertemuanPunyaDiskusiSub.getBiodataCalonMahasiswa().getNama() + " (Calon Mahasiswa)")
			: (pertemuanPunyaDiskusiSub.getMahasiswa() != null
					? pertemuanPunyaDiskusiSub.getMahasiswa().getNama() + " (Mahasiswa)"
					: "");

	try {
		if (oleh.trim().equals("")) {
			oleh = pertemuanPunyaDiskusiSub.getDosen() != null
					? pertemuanPunyaDiskusiSub.getDosen().getNama() + " (Dosen)"
					: "";
		}

		if (oleh.trim().equals("")) {
			oleh = pertemuanPunyaDiskusiSub.getTbmuser() != null
					? pertemuanPunyaDiskusiSub.getTbmuser().getUserNama() + " ("
							+ pertemuanPunyaDiskusiSub.getTbmuser().hakAkses().getRoleName() + ")"
					: "";
		}
	} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/o/ux/content/component/diskusi/sub_diskusi_elearning.jsp:57");
		// TODO: handle exception
	}

	Tbmuser usrkomentar = pertemuanPunyaDiskusiSub.getTbmuser();
	String foto = "https://bootdey.com/img/Content/avatar/avatar1.png";
	if (pertemuanPunyaDiskusiSub.getMahasiswa() != null) {
		foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusiSub.getMahasiswa()), 152, 114);
	} else if (pertemuanPunyaDiskusiSub.getDosen() != null) {
		foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusiSub.getDosen()), 152, 114);
	} else if (pertemuanPunyaDiskusiSub.getBiodataCalonMahasiswa() != null) {
		foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusiSub.getBiodataCalonMahasiswa()),
				152, 114);
	} else if (pertemuanPunyaDiskusiSub.getSiswa() != null) {
		foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusiSub.getSiswa()), 152, 114);
	} else if (pertemuanPunyaDiskusiSub.getCalonSiswa() != null) {
		foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusiSub.getCalonSiswa()), 152, 114);
	} else if (pertemuanPunyaDiskusiSub.getGuru() != null) {
		foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusiSub.getGuru()), 152, 114);
	} else {
		foto = CommonMedia.getUrlFotoPengguna(pertemuanPunyaDiskusiSub.getTbmuser(), 152, 114);
	}

	String waktu = pertemuanPunyaDiskusiSub.getTanggal_dirubah() == null ? ""
			: SmartDateTimeUtil.getDayString(pertemuanPunyaDiskusiSub.getTanggal_dirubah(), null)
					+ Common.dateFormat5.get().format(pertemuanPunyaDiskusiSub.getTanggal_dirubah());
%>
<!-- Comments -->
<div>
	<div class="media-block">
		<a class="media-left" href="#"><img class="img-circle img-sm"
			alt="Profile Picture" src="<%=foto%>"></a>
		<div class="media-body">
			<div class="mar-btm">
				<a href="#" class="btn-link text-semibold media-heading box-inline"><%=oleh%></a>
				<p class="text-muted text-sm">
					<i class="far fa-clock"></i>
					<%=waktu%>
				</p>
			</div>
			<p><%=pertemuanPunyaDiskusiSub.getIsi()%></p>

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
				<jsp:param value="false" name="bolehtanggapi" />
			</jsp:include>
		</div>
	</div>
</div>
<%
}
}
}
%>

