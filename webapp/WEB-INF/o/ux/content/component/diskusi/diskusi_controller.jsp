<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.database.model.PertemuanPunyaDiskusi"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.Mahasiswa"%>

<%@page import="ais.database.model.Dosen"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
String bolehtanggapi = request.getParameter("bolehtanggapi") == null ? "true" : request.getParameter("bolehtanggapi");

PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) request.getAttribute("pertemuanPunyaDiskusi");
Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();
BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa();
Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
Guru guru = tbmuser == null ? null : tbmuser.getGuru();
boolean boleh = (mahasiswa != null && mahasiswa.getId() != null && pertemuanPunyaDiskusi.getMahasiswa() != null
		&& pertemuanPunyaDiskusi.getMahasiswa().getId() != null
		&& mahasiswa.getId().equals(pertemuanPunyaDiskusi.getMahasiswa().getId()));

boleh = boleh || (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null
		&& pertemuanPunyaDiskusi.getBiodataCalonMahasiswa() != null
		&& pertemuanPunyaDiskusi.getBiodataCalonMahasiswa().getId() != null
		&& biodataCalonMahasiswa.getId().equals(pertemuanPunyaDiskusi.getBiodataCalonMahasiswa().getId()));

boleh = boleh || (siswa != null && siswa.getId() != null && pertemuanPunyaDiskusi.getSiswa() != null
		&& pertemuanPunyaDiskusi.getSiswa().getId() != null
		&& siswa.getId().equals(pertemuanPunyaDiskusi.getSiswa().getId()));

boleh = boleh || (dosen != null && dosen.getId() != null && pertemuanPunyaDiskusi.getDosen() != null
		&& pertemuanPunyaDiskusi.getDosen().getId() != null
		&& dosen.getId().equals(pertemuanPunyaDiskusi.getDosen().getId()));

boleh = boleh || (guru != null && guru.getId() != null && pertemuanPunyaDiskusi.getGuru() != null
		&& pertemuanPunyaDiskusi.getGuru().getId() != null
		&& guru.getId().equals(pertemuanPunyaDiskusi.getGuru().getId()));

boleh = boleh || (tbmuser != null && pertemuanPunyaDiskusi.getTbmuser() != null
		&& pertemuanPunyaDiskusi.getTbmuser().getUserId() != null && tbmuser.getUserId() != null
		&& tbmuser.getUserId().equals(pertemuanPunyaDiskusi.getTbmuser().getUserId()));

boolean bolehHapus = boleh || Common.getApakahAdmin() || (dosen != null && dosen.getId() != null);
Long pertemuanPunyaDiskusiId = pertemuanPunyaDiskusi.getId();
%>

<%
LampiranLain lain = LampiranLain.ambil(pertemuanPunyaDiskusi.getId(), LampiranLain.DISKUSI);
if (lain != null && lain.getId() != null) {
%>

<a href="<%=lain.createLinkUri()%>" target="_blank"><i
	class="far fa-paperclip fa-fw"></i></i> <%=lain.getNama()%></a>

<%
}
%>


<div class="pad-ver">
	<%
	if (boleh || bolehHapus) {
	%>
	<div class="btn-group">
		<form action="#panel_<%=pertemuanPunyaDiskusiId%>" method="post"
			id="action_button_<%=pertemuanPunyaDiskusiId%>">

			<input type="hidden" name="parent"
				value="<%=pertemuanPunyaDiskusiId%>" />
			<%
			if (boleh) {
			%>
			<button class="btn btn-sm btn-default btn-hover-primary"
				onclick="document.getElementById('balas_<%=pertemuanPunyaDiskusiId%>').style.display='';
									document.getElementById('balas_button_<%=pertemuanPunyaDiskusiId%>').style.display='none';
									document.getElementById('ubah_<%=pertemuanPunyaDiskusiId%>').value='true';
									document.getElementById('posting_<%=pertemuanPunyaDiskusiId%>').innerHTML='<%=pertemuanPunyaDiskusi.getIsi()%>';
									"
				name="ubah" value="true" type="button">
				<i class="fa fa-edit fa-fw"></i> Ubah
			</button>
			<%
			}
			%>

			<%
			if (bolehHapus) {
			%>
			<button class="btn btn-sm btn-default btn-hover-primary"
				onclick="return confirm('Apakah Anda ingin menghapus komentar ini ?');"
				name="hapus" value="true" type="submit">
				<i class="fa fa-trash fa-fw"></i> Hapus
			</button>
			<%
			}
			%>

		</form>
	</div>
	<%
	}
	%>

	<form action="#panel_<%=pertemuanPunyaDiskusiId%>" method="post"
		enctype="multipart/form-data" style="display: none"
		id="balas_<%=pertemuanPunyaDiskusiId%>">
		<input type="hidden" name="parent"
			value="<%=pertemuanPunyaDiskusiId%>" /> <input type="hidden"
			id="ubah_<%=pertemuanPunyaDiskusiId%>" name="ubah" value="false" />
		<div class="panel-body">
			<textarea class="form-control" rows="2" name="posting"
				id="posting_<%=pertemuanPunyaDiskusiId%>" required="required"
				placeholder="Apa yang ingin anda tanyakan?"></textarea>

			<input type="file" style="display: none;"
				onchange="loadFile<%=pertemuanPunyaDiskusiId%>(event)"
				id="file_<%=pertemuanPunyaDiskusiId%>" name="file" />

			<div id="file_label_<%=pertemuanPunyaDiskusiId%>"></div>

			<script>
				var loadFile<%=pertemuanPunyaDiskusiId%> = function(event) {
					var output = document.getElementById('file_label_<%=pertemuanPunyaDiskusiId%>');
					if(event.target.files[0]){
						output.innerHTML = event.target.files[0].name;
					}
				};
			</script>

			<div class="mar-top clearfix btn-group">

				<button class="btn btn-sm btn-primary pull-right"
					onclick="
											document.getElementById('balas_<%=pertemuanPunyaDiskusiId%>').style.display='none';
											document.getElementById('balas_button_<%=pertemuanPunyaDiskusiId%>').style.display='';
											document.getElementById('ubah_<%=pertemuanPunyaDiskusiId%>').value='false';
											"
					style="padding-left: 50px" type="button">
					<i class="fa fa-ban fa-fw"></i> Batal
				</button>

				<button class="btn btn-sm btn-primary pull-right"
					onclick="document.getElementById('file_<%=pertemuanPunyaDiskusiId%>').click();"
					style="padding-left: 50px" type="button">
					<i class="fa fa-paperclip fa-fw"></i> Lampiran
				</button>

				<button class="btn btn-sm btn-primary pull-right" type="submit">
					<i class="fa fa-pencil fa-fw"></i> Kirim
				</button>
			</div>

		</div>
	</form>
	<%
	if (bolehtanggapi.equalsIgnoreCase("true")) {
	%>
	<a class="btn btn-sm btn-default btn-hover-primary"
		id="balas_button_<%=pertemuanPunyaDiskusiId%>"
		onclick="
		document.getElementById('balas_<%=pertemuanPunyaDiskusiId%>').style.display='';
		document.getElementById('balas_button_<%=pertemuanPunyaDiskusiId%>').style.display='none';
		document.getElementById('posting_<%=pertemuanPunyaDiskusiId%>').innerHTML='';
		"><i
		class="fad fa-comment"></i> Balas</a>

	<%
	}
	%>
</div>