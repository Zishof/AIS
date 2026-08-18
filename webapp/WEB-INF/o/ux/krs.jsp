<!--begin:::Tab content-->
<script src="<%=request.getContextPath() %>/js/pesan-formal.js"></script>
<%@page
	import="ais.database.model.PembagianKuotaPerkuliahanBerdasarkantahunAngkatan"%>
<%@page import="ais.action.master.helper.KrsUtilHelper"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page
	import="ais.database.model.PembatasanNilaiIPKUntukPengambilanKRS"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.KrsMahasiswa"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.Kurikulum"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Detailperkuliahan"%>

<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getMahasiswa() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
Mahasiswa mahasiswa = tbmuser.getMahasiswa();

String ta = Common.getCurrentTahunAkademik();
boolean ganjil = Common.isNowSemensterGanjil();
String idsmt = request.getParameter("idsmt");

try {
	if (idsmt == null) {
		idsmt = ta.substring(0, 4) + (ganjil ? "1" : "2");
	}
} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/ux/krs.jsp:41");
}

//System.out.println("idsmt -> " + idsmt);

Integer semesterPendek = null;
if (idsmt != null && idsmt.endsWith("3")) {
	semesterPendek = Perkuliahan.SEMESTER_PENDEK;
}
if (idsmt != null) {
	ta = (Integer.parseInt(idsmt.substring(0, 4))) + "/" + (Integer.parseInt(idsmt.substring(0, 4)) + 1);
	ganjil = Integer.parseInt(idsmt.substring(4, 5)) == 1;
}

Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), ta, ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
		mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
KrsMahasiswa krsMahasiswa = mahasiswa == null ? null : Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);

String id_hapus = request.getParameter("id_hapus");
String[] perkuliahan = request.getParameterValues("perkuliahan");

String peringatanKapasitasRuangan = "";

if (id_hapus != null) {
	Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
	id_hapus);
	if (detailperkuliahan != null) {
		Session mySession = HibernateUtil.currentNativeSession();
		mySession.getTransaction().begin();
		mySession.delete(detailperkuliahan);
		mySession.getTransaction().commit();
		// mySession.disconnect();
		if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
		HibernateUtil.closeSession();

		krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null, true);
	}
} else if (perkuliahan != null) {
	for (String p : perkuliahan) {
		Perkuliahan perkuliahanData = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
		Long.parseLong(p));
		System.out.println("perkuliahanData -> " + perkuliahanData);

		Session mySession = HibernateUtil.currentNativeSession();
		try {
	int detailperkuliahanCount = KrsUtilHelper.ambilJumlahDetailperkuliahan(mySession, perkuliahanData,
			mahasiswa, true);
	if (detailperkuliahanCount == 0) {
		Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(mySession, perkuliahanData, false);
		Integer kapasitasKelas = perkuliahanData.getKapasitasKelas();
		PembagianKuotaPerkuliahanBerdasarkantahunAngkatan pembagianKuotaPerkuliahanBerdasarkantahunAngkatan = KrsUtilHelper
				.ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(mySession, perkuliahanData,
						mahasiswa.getTahunangkatan(), false);
		Number kuota = pembagianKuotaPerkuliahanBerdasarkantahunAngkatan == null ? null
				: pembagianKuotaPerkuliahanBerdasarkantahunAngkatan.getKuota();
		if (kuota != null) {
			kapasitasKelas = kuota.intValue();
		}

		jumlahUdahMasuk++;
		if (jumlahUdahMasuk > (kapasitasKelas)) {
			peringatanKapasitasRuangan += "Kapasitas kelas sudah penuh. Maksimal kapasitas kelas tesebut adalah "
					+ (kapasitasKelas) + ", sedangkan anda mencoba masuk ke perkuliahan ini menjadi berjumlah "
					+ jumlahUdahMasuk + ". Pilihlah jadwal perkuliahan lainnya.\n";
			HibernateUtil.closeSession();
			continue;
		}
		Detailperkuliahan detailperkuliahan = new Detailperkuliahan(tbmuser, this.getClass());
		detailperkuliahan.setNilaiHuruf("");
		detailperkuliahan.setTotalNilai(0.0);
		detailperkuliahan.setMahasiswa(mahasiswa);
		detailperkuliahan.setPerkuliahan(perkuliahanData);
		detailperkuliahan.setTahap(null);
		detailperkuliahan.setSemester(semester);
		mySession.getTransaction().begin();
		Common.refreshSaveOrUpdate(mySession, detailperkuliahan);
		mySession.getTransaction().commit();
	}

		} catch (Exception e) {
	Common.tampilErrorJikaAdmin(e);
		}
		HibernateUtil.closeSession();
		krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null, true);
	}
}

Fakultas fakultas = mahasiswa.getJurusan().getFakultas();
Jurusan jurusan = mahasiswa.getJurusan();

PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = semester.equals(1) ? null
		: Common.getIpkUntukPengambilanKRS(mahasiswa, semester, mahasiswa.getTahunangkatan(), fakultas, jurusan,
		mahasiswa.getProgram(), semesterPendek);

Integer maxsks = (Integer) (pembatasanNilaiIPKUntukPengambilanKRS == null
		? PembatasanNilaiIPKUntukPengambilanKRS.getDefaultPembatasanNilaiIpUntukAmbilKRS()
		: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil());

List<Long> detailperkuliahansTemp = mahasiswa.ambilDetailperkuliahan(semester, null, semesterPendek);

request.setAttribute("krsMahasiswa", krsMahasiswa);
%>


<!DOCTYPE html>
<html lang="id">
<jsp:include page="/WEB-INF/o/ux/content/common/header.jsp"></jsp:include>
<!--begin::Body-->
<body data-kt-name="metronic" id="kt_app_body"
	data-kt-app-layout="dark-sidebar" data-kt-app-header-fixed="true"
	data-kt-app-sidebar-enabled="true" data-kt-app-sidebar-fixed="true"
	data-kt-app-sidebar-hoverable="true"
	data-kt-app-sidebar-push-header="true"
	data-kt-app-sidebar-push-toolbar="true"
	data-kt-app-sidebar-push-footer="true"
	data-kt-app-toolbar-enabled="true" class="app-default">



	<jsp:include page="/WEB-INF/o/ux/content/common/mode.jsp"></jsp:include>
	<!--begin::App-->
	<div class="d-flex flex-column flex-root app-root" id="kt_app_root">
		<!--begin::Page-->
		<div class="app-page flex-column flex-column-fluid" id="kt_app_page">


			<jsp:include page="/WEB-INF/o/ux/content/common/header_app.jsp"></jsp:include>


			<!--begin::Wrapper-->
			<div class="app-wrapper flex-column flex-row-fluid"
				id="kt_app_wrapper">


				<jsp:include page="/WEB-INF/o/ux/content/common/sidebar.jsp"></jsp:include>


				<!--begin:::Main-->
				<div class="app-main flex-column flex-row-fluid" id="kt_app_main">
					<!--begin::Content wrapper-->
					<div class="d-flex flex-column flex-column-fluid ">

						<jsp:include page="/WEB-INF/o/ux/content/common/page_title.jsp">
							<jsp:param value="KRS" name="judul" />
							<jsp:param value="Rencana Studi Mahasiswa" name="subjudul" />
						</jsp:include>

						<%
						if (!peringatanKapasitasRuangan.isEmpty()) {
							out.println("<script type=\"text/javascript\">tampilkanPesanGagalFormal('pengambilan KRS (Kartu Rencana Studi)', \""
									+ peringatanKapasitasRuangan.replace("\"", "'").replace("\n", " ")
									+ "\", ['Pilih kelas/jadwal perkuliahan paralel lain yang kapasitasnya masih tersedia.', 'Hubungi bagian akademik atau dosen wali apabila memerlukan penambahan kuota kelas.']);</script>");
						}
						%>


						<!--begin::Content-->
						<div id="kt_app_content" class="app-content flex-column-fluid">
							<!--begin::Content container-->
							<div id="kt_app_content_container"
								class="app-container container-xxl">
								<!--begin::details View-->
								<div class="card mb-5 mb-xl-10" id="kt_profile_details_view">
									<!--begin::Card header-->
									<div class="card-header">
										<!--begin::Card title-->
										<div class="card-title align-items-start flex-column">
											<h3 class="fw-bold">Daftar Rencana Studi Mahasiswa</h3>
										</div>
										<!--end::Card title-->
										<!--begin::Action-->
										<div class="card-toolbar">
											<a
												href="<%=request.getContextPath()%>/pages/ux/content/krs_mhs/tambah.jsp?idsmt=<%=idsmt%>"
												class="btn btn-primary align-self-center btn-sm me-3"> <span
												class="svg-icon svg-icon-2"> <i class="fas fa-plus"></i>
											</span> Tambah
											</a>
											<!--begin::Export dropdown-->
											<button type="button"
												class="btn btn-primary align-self-center btn-sm"
												data-kt-menu-trigger="click"
												data-kt-menu-placement="bottom-end">
												<!--begin::Svg Icon | path: icons/duotune/arrows/arr091.svg-->
												<span class="svg-icon svg-icon-2"> <i
													class="fas fa-cogs"></i>
												</span>
												<!--end::Svg Icon-->
												Action
											</button>
											<!--begin::Menu-->
											<div id="kt_datatable_example_export_menu"
												data-kt-menu="true"
												class="menu menu-sub menu-sub-dropdown menu-column menu-rounded menu-gray-600 menu-state-bg-light-primary fw-semibold fs-7 w-200px py-4">
												<!--begin::Menu item-->
												<div class="menu-item px-3">
													<a href="javascript:void(0)" class="menu-link px-3"
														data-kt-export="pdf">Cetak KRS</a>
												</div>
												<!--end::Menu item-->
												<!--begin::Menu item-->
												<div class="menu-item px-3">
													<a href="javascript:void(0)" class="menu-link px-3">Upload
														KRS disetujui</a>
												</div>
												<!--end::Menu item-->
												<!--begin::Menu item-->
												<div class="menu-item px-3">
													<a href="javascript:void(0)" class="menu-link px-3"
														data-kt-export="pdf">Cetak Kartu UTS</a>
												</div>
												<!--end::Menu item-->
												<!--begin::Menu item-->
												<div class="menu-item px-3">
													<a href="javascript:void(0)" class="menu-link px-3"
														data-kt-export="pdf">Cetak Kartu UAS</a>
												</div>
												<!--end::Menu item-->
											</div>
											<!--end::Menu-->
											<!--end::Export dropdown-->
											<!--begin::Hide default export buttons-->
											<div id="kt_datatable_example_buttons" class="d-none"></div>
											<!--end::Hide default export buttons-->
										</div>
									</div>
									<!--begin::Card header-->

									<jsp:include page="/WEB-INF/o/ux/content/common/info_mhs.jsp">
										<jsp:param value="${krsMahasiswa}" name="krsMahasiswa" />
									</jsp:include>

									<jsp:include
										page="/WEB-INF/o/ux/content/elearning/pilihan_tahun_akademik.jsp"></jsp:include>

									<!--begin:::Tab content-->
									<div class="tab-content" id="myTabContent">
										<!--begin:::Tab pane-->
										<div class="tab-pane fade show active" id="kt_tab_pane_1"
											role="tabpanel">
											<div class="card-body pt-2">
												<div class="table-responsive">
													<!--begin::Table-->
													<table
														class="table align-middle table-rounded table-striped border gy-5 gs-7"
														id="kt_table_widget_4_table">
														<!--begin::Table head-->
														<thead>
															<!--begin::Table row-->
															<tr
																class="fw-semibold fs-6 text-gray-800 border-bottom-2 border-gray-200">
																<th class="min-w-50px"><%= Common.getBahasaConfig("Kode") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Nama Mata Kuliah") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Kelas") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Dosen Pengajar") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Ruang") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Hari") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Waktu") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("SKS") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Persetujuan") %></th>
																<th class="min-w-50px"></th>
															</tr>
															<!--end::Table row-->
														</thead>
														<!--end::Table head-->
														<!--begin::Table body-->
														<tbody class="text-gray-600 ">

															<%
															for (Long detailperkuliahanid : detailperkuliahansTemp) {
																Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
																detailperkuliahanid.toString());

																if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {
																	Matakuliah matakuliah = detailperkuliahan.getPerkuliahan().getMatakuliah();
																	Kurikulum kurikulum = detailperkuliahan.getPerkuliahan().getKurikulum();
																	String dosen = "";
																	List<Dosen> dosens = detailperkuliahan.getPerkuliahan().populateDosenBuNama();
																	for (Dosen dd : dosens) {
																dosen += dosen.isEmpty() ? dd.getNama() : ", " + dd.getNama();
																	}
															%>


															<tr>
																<td class=""><%=matakuliah.getKode()%></td>
																<td class=""><%=matakuliah.getNama()%> (<%=kurikulum == null ? "" : kurikulum.getNama()%>)</td>
																<td class=""><%=detailperkuliahan.getPerkuliahan().getKelas()%></td>
																<td class=""><%=dosen%></td>
																<td class=""><%=detailperkuliahan.getPerkuliahan().getRuang() == null ? ""
		: detailperkuliahan.getPerkuliahan().getRuang().getNama()%></td>
																<td class=""><%=detailperkuliahan.getPerkuliahan().getHari()%></td>
																<td class=""><%=(detailperkuliahan.getPerkuliahan().getWaktuMulai() == null ? ""
		: "(" + detailperkuliahan.getPerkuliahan().getWaktuMulai())%> <%=(detailperkuliahan.getPerkuliahan().getWaktuSelesai() == null ? ""
		: "s.d " + detailperkuliahan.getPerkuliahan().getWaktuSelesai() + ")")%></td>
																<td class=""><%=matakuliah.getSks()%></td>
																<td class=""><%=detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI) ? "Ya" : "Belum"%></td>
																<td class="">
																	<%
																	if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI)) {
																	%>

																	<form method="post"
																		id="hapus_<%=detailperkuliahan.getId()%>"
																		action="<%=request.getRequestURI()%>#kt_table_widget_4_table">
																		<input type="hidden" name="idsmt" value="<%=idsmt%>" />
																		<input type="hidden" name="id_hapus"
																			value="<%=detailperkuliahan.getId()%>" /> <a
																			href="#"
																			onclick="document.getElementById('hapus_<%=detailperkuliahan.getId()%>').submit();"
																			data-bs-toggle="tooltip" data-bs-placement="top"
																			title="Hapus Data"><i
																			class="fas fa-times-circle text-danger fs-3"></i></a>

																	</form> <%
 }
 %>

																</td>
															</tr>


															<%
															}

															}
															%>
														</tbody>
														<!--end::Table body-->
														<tfoot>
															<tr class="border-top fw-semibold fs-6 text-gray-800">
																<th></th>
																<th></th>
																<th colspan="2"><%= Common.getBahasaConfig("Maksimal SKS") %></th>
																<th><%=maxsks%></th>
																<th colspan="2"><%= Common.getBahasaConfig("SKS dipilih") %></th>
																<th><%=krsMahasiswa.getSksYangDiambil()%></th>

															</tr>
														</tfoot>
													</table>
													<!--end::Table-->
												</div>
											</div>
											<!--end::Card body-->
										</div>
										<!--end::tab pane-->
									</div>
									<!--end::tab content-->

								</div>
								<!--end::details View-->
							</div>
							<!--end::Content container-->
						</div>
						<!--end::Content-->



					</div>
					<!--end::Content wrapper-->
					<jsp:include page="/WEB-INF/o/ux/content/common/app_footer.jsp"></jsp:include>
				</div>
				<!--end:::Main-->
			</div>
			<!--end::Wrapper-->
		</div>
		<!--end::Page-->
	</div>
	<!--end::App-->


	<!--end::Drawers-->

	<jsp:include page="/WEB-INF/o/ux/content/common/footer.jsp"></jsp:include>
</body>
<!--end::Body-->
</html>