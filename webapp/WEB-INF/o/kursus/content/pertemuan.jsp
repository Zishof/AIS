<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Pegawai"%>
<%@page import="java.util.Map"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="java.util.List"%>
<%@page import="ais.ui.util.SmartDateTimeUtil"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<script src="<%=request.getContextPath() %>/js/pesan-formal.js"></script>
<%
Long pertemuanid = request.getParameter("pertemuanid") == null ? null
		: Long.parseLong(request.getParameter("pertemuanid"));
Boolean peserta = (Boolean) request.getAttribute("peserta");
Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
if (pertemuan != null && pertemuan.getKomponenDataProdukKursus() != null) {

	Map<String, Pegawai> map = pertemuan.getKomponenDataProdukKursus().populatePegawai();

	String tgl = pertemuan.getTanggal() == null ? "-"
	: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), pertemuan.getWaktuMulai())
			+ Common.dateFormat6.get().format(pertemuan.getTanggal())) + " "
			+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
					: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai());
	String tpk = pertemuan.getBukuRujukan1();
	List<String> urls = null;
	if (!tpk.trim().isEmpty()) {
		urls = Common.getUrls(tpk);
		tpk = tpk.replaceAll("\n", "<br>");
		for (String url : urls) {
	tpk = StringUtils.replace(tpk, url, "<a href='" + url + "' target='_blank'>" + url + "</a>");
		}
	}

	String met = pertemuan.getMetodePembelajaran();
	urls = null;
	if (!met.trim().isEmpty()) {
		urls = Common.getUrls(met);
		met = met.replaceAll("\n", "<br>");
		for (String url : urls) {
	met = StringUtils.replace(met, url, "<a href='" + url + "' target='_blank'>" + url + "</a>");
		}
	}

	String ref = pertemuan.getBukuRujukan2();
	urls = null;
	if (!ref.trim().isEmpty()) {
		urls = Common.getUrls(ref);
		ref = ref.replaceAll("\n", "<br>");
		for (String url : urls) {
	ref = StringUtils.replace(ref, url, "<a href='" + url + "' target='_blank'>" + url + "</a>");
		}
	}

	String catat = pertemuan.getCatatan();
	urls = null;
	if (!catat.trim().isEmpty()) {
		urls = Common.getUrls(catat);
		catat = catat.replaceAll("\n", "<br>");
		for (String url : urls) {
	catat = StringUtils.replace(catat, url, "<a href='" + url + "' target='_blank'>" + url + "</a>");
		}

	}
%>
<li style="border-bottom: 3px solid #7b87bb;">
	<div class="row">
		<div class="col-8">
			<h6><%="Pertemuan ke " + pertemuan.getPertemuanKe() + ", " + pertemuan.info()%></h6>
			<p class="mb-3" style="font-size: small;"><%=pertemuan.getTopik()%></p>
		</div>
		<div class="col-4">
			<span style="font-size: small;"><%=tgl%></span>
		</div>


		<div class="col-12">
			<h6>Tutor</h6>
			<p class="mb-3">
			<ul>
				<%
				String tutor = "";
				for (Pegawai pegawai : map.values()) {
					tutor += tutor.isEmpty() ? pegawai.getNama() : ", " + pegawai.getNama();
					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(pegawai));
				%>
				<li
					style="width: 25%; display: inline-block; vertical-align: top; border-bottom: 0px solid #7b87bb;"
					title="<%=pegawai.getNama()%>"><img src="<%=url%>"
					alt="<%=pegawai.getNama()%>" style="max-height: 80px;"
					class="rounded mx-auto d-block"></li>
				<%
				}
				%>
			</ul>

			<%=(tutor.trim().isEmpty() ? "<i>Tidak ada tutor</i>" : tutor)%>
			</p>
			<h6>Bahan Kajian</h6>
			<p class="mb-3"><%=(tpk.trim().isEmpty() ? "<i>Tidak ada bahan kajian</i>" : tpk)%></p>
			<h6>Metode Pembelajaran</h6>
			<p class="mb-3"><%=(met.trim().isEmpty() ? "<i>Tidak ada metode pembelajaran</i>" : met)%></p>
			<h6>Referensi</h6>
			<p class="mb-3"><%=(ref.trim().isEmpty() ? "<i>Tidak ada referensi</i>" : ref)%></p>
			<h6>Catatan</h6>
			<p class="mb-3"><%=(catat.trim().isEmpty() ? "<i>Tidak ada catatan</i>" : catat)%></p>
		</div>

		<div class="col-12">
			<div style="font-size: 1rem; font-weight: 500;">

				<%
				if (peserta) {
				%>

				<a class="all_data"
					href="<%=request.getContextPath()%>/pertemuan.zul?id=<%=pertemuanid%>&info=0&tampilSelesai=false"
					class="mr-3"><i class="icon_document_alt"></i> Kehadiran</a> <a
					class="all_data"
					href="<%=request.getContextPath()%>/pertemuan.zul?id=<%=pertemuanid%>&info=2&tampilSelesai=false"
					class="mr-3"><i class="icon_archive_alt"></i> Materi</a> <a
					class="all_data"
					href="<%=request.getContextPath()%>/pertemuan.zul?id=<%=pertemuanid%>&info=4&tampilSelesai=false"
					class="mr-3"><i class="icon_volume-high"></i> Audio</a> <a
					class="all_data"
					href="<%=request.getContextPath()%>/pertemuan.zul?id=<%=pertemuanid%>&info=5&tampilSelesai=false"
					class="mr-3"><i class="ti-video-camera"></i> Video</a> <a
					class="all_data"
					href="<%=request.getContextPath()%>/pertemuan.zul?id=<%=pertemuanid%>&info=7&tampilSelesai=false"
					class="mr-3"><i class="icon_comment_alt"></i> Diskusi</a> <a
					class="all_data"
					href="<%=request.getContextPath()%>/pertemuan.zul?id=<%=pertemuanid%>&info=6&tampilSelesai=false"
					class="mr-3"><i class="icon_pencil-edit"></i> Ujian</a> <a
					class="all_data"
					href="<%=request.getContextPath()%>/pertemuan.zul?id=<%=pertemuanid%>&info=3&tampilSelesai=false"
					class="mr-3"><i class="icon_pencil-edit_alt"></i> Tugas</a>

				<%
				} else {
				%>

				<a class="all_data"
					onclick="return tampilkanPesanGagalFormal('akses ke halaman pertemuan ini', '<%= Common.getBahasaConfig("Anda belum terdaftar sebagai peserta pada kursus ini sehingga tidak diizinkan mengakses konten pertemuan.") %>', ['Lakukan pendaftaran sebagai peserta pada kursus ini terlebih dahulu.', 'Setelah terdaftar sebagai peserta, silakan muat ulang halaman ini.']);"
					class="mr-3"><i class="icon_document_alt"></i> Kehadiran</a> <a
					class="all_data"
					onclick="return tampilkanPesanGagalFormal('akses ke halaman pertemuan ini', '<%= Common.getBahasaConfig("Anda belum terdaftar sebagai peserta pada kursus ini sehingga tidak diizinkan mengakses konten pertemuan.") %>', ['Lakukan pendaftaran sebagai peserta pada kursus ini terlebih dahulu.', 'Setelah terdaftar sebagai peserta, silakan muat ulang halaman ini.']);"
					class="mr-3"><i class="icon_archive_alt"></i> Materi</a> <a
					class="all_data"
					onclick="return tampilkanPesanGagalFormal('akses ke halaman pertemuan ini', '<%= Common.getBahasaConfig("Anda belum terdaftar sebagai peserta pada kursus ini sehingga tidak diizinkan mengakses konten pertemuan.") %>', ['Lakukan pendaftaran sebagai peserta pada kursus ini terlebih dahulu.', 'Setelah terdaftar sebagai peserta, silakan muat ulang halaman ini.']);"
					class="mr-3"><i class="icon_volume-high"></i> Audio</a> <a
					class="all_data"
					onclick="return tampilkanPesanGagalFormal('akses ke halaman pertemuan ini', '<%= Common.getBahasaConfig("Anda belum terdaftar sebagai peserta pada kursus ini sehingga tidak diizinkan mengakses konten pertemuan.") %>', ['Lakukan pendaftaran sebagai peserta pada kursus ini terlebih dahulu.', 'Setelah terdaftar sebagai peserta, silakan muat ulang halaman ini.']);"
					class="mr-3"><i class="ti-video-camera"></i> Video</a> <a
					class="all_data"
					onclick="return tampilkanPesanGagalFormal('akses ke halaman pertemuan ini', '<%= Common.getBahasaConfig("Anda belum terdaftar sebagai peserta pada kursus ini sehingga tidak diizinkan mengakses konten pertemuan.") %>', ['Lakukan pendaftaran sebagai peserta pada kursus ini terlebih dahulu.', 'Setelah terdaftar sebagai peserta, silakan muat ulang halaman ini.']);"
					class="mr-3"><i class="icon_comment_alt"></i> Diskusi</a> <a
					class="all_data"
					onclick="return tampilkanPesanGagalFormal('akses ke halaman pertemuan ini', '<%= Common.getBahasaConfig("Anda belum terdaftar sebagai peserta pada kursus ini sehingga tidak diizinkan mengakses konten pertemuan.") %>', ['Lakukan pendaftaran sebagai peserta pada kursus ini terlebih dahulu.', 'Setelah terdaftar sebagai peserta, silakan muat ulang halaman ini.']);"
					class="mr-3"><i class="icon_pencil-edit"></i> Ujian</a> <a
					class="all_data"
					onclick="return tampilkanPesanGagalFormal('akses ke halaman pertemuan ini', '<%= Common.getBahasaConfig("Anda belum terdaftar sebagai peserta pada kursus ini sehingga tidak diizinkan mengakses konten pertemuan.") %>', ['Lakukan pendaftaran sebagai peserta pada kursus ini terlebih dahulu.', 'Setelah terdaftar sebagai peserta, silakan muat ulang halaman ini.']);"
					class="mr-3"><i class="icon_pencil-edit_alt"></i> Tugas</a>

				<%
				}
				%>

			</div>
		</div>
	</div>
</li>
<%
}
%>