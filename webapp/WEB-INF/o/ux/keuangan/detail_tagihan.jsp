
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="ais.database.model.PengaturanPembayaranBulanan"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.DetailKegiatan"%>
<%@page import="java.util.Collection"%>
<%@page import="ais.database.model.CicilanPembayaran"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.KrsMahasiswa"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
String idsmt = request.getParameter("idsmt");
Kegiatan kegiatan = (Kegiatan) GeneralValueObject.ambilData(Kegiatan.class, request.getParameter("id"), true);
List<CicilanPembayaran> cicilanPembayarans = kegiatan.ambilCicilan();

Map<Long, Double> nilais = new HashMap<Long, Double>();
for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
	if (cicilanPembayaran != null && cicilanPembayaran.getId() != null && cicilanPembayaran.getItemBiaya() != null) {
		if (nilais.keySet().contains(cicilanPembayaran.getItemBiaya().getId())) {
	Double nilai = nilais.get(cicilanPembayaran.getItemBiaya().getId()) + cicilanPembayaran.getNilai();
	nilais.put(cicilanPembayaran.getItemBiaya().getId(), nilai);
		} else {
	nilais.put(cicilanPembayaran.getItemBiaya().getId(), cicilanPembayaran.getNilai());
		}
	}
}

Collection<DetailKegiatan> detailKegiatans = kegiatan.ambilDetailKegiatan();
Mahasiswa mahasiswa = kegiatan.getMahasiswa();
BiodataCalonMahasiswa biodataCalonMahasiswa = kegiatan.getCalonMahasiswa();
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
							<jsp:param value="Keuangan" name="judul" />
							<jsp:param value="Detail Tagihan" name="subjudul" />
							<jsp:param
								value="<%=kegiatan.getJenisKegiatan().getNamaKegiatan()%>"
								name="subjudul1" />
						</jsp:include>

						<!--begin::Content-->
						<div id="kt_app_content" class="app-content flex-column-fluid">
							<!--begin::Content container-->
							<div id="kt_app_content_container"
								class="app-container container-xxl">
								<!--begin::Layout-->
								<div class="d-flex flex-column flex-xl-row">
									<!--begin::Col-->
									<div class="col-xl-12">
										<!--begin::Table Widget 4-->
										<div class="card card-flush h-xl-100">
											<!--begin::Card header-->
											<div class="card-header pt-7">
												<!--begin::Title-->
												<h3 class="card-title align-items-start flex-column">
													<span class="card-label fw-bold text-gray-800">Detail
														Tagihan</span> <span class="text-gray-400 mt-1 fw-semibold fs-6"></span>
												</h3>
												<!--end::Title-->
											</div>
											<!--end::Card header-->
											<!--begin::Card body-->
											<div class="card-body p-9">
												<!--begin::Row-->
												<div class="row mb-7">
													<!--begin::Label-->
													<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Jenis Pembayaran") %></label>
													<!--end::Label-->
													<!--begin::Col-->
													<div class="col-lg-8">
														<span class="fw-bold fs-6 text-gray-800"><%=kegiatan.getJenisKegiatan().getNamaKegiatan()%></span>
													</div>
													<!--end::Col-->
												</div>
												<!--end::Row-->
												<!--begin::Input group-->
												<div class="row mb-7">
													<!--begin::Label-->
													<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Tahun Akademik") %></label>
													<!--end::Label-->
													<!--begin::Col-->
													<div class="col-lg-8 fv-row">
														<span class="fw-semibold text-gray-800 fs-6"><%=kegiatan.getTahunAkademik()%>
														</span>
													</div>
													<!--end::Col-->
												</div>
												<!--end::Input group-->
												<!--begin::Input group-->
												<div class="row mb-7">
													<!--begin::Label-->
													<label class="col-lg-4 fw-semibold text-muted"><%= Common.getBahasaConfig("Semester") %></label>
													<!--end::Label-->
													<!--begin::Col-->
													<div class="col-lg-8 fv-row">
														<span class="fw-semibold text-gray-800 fs-6"><%=kegiatan.getSemster()%>
														</span>
													</div>
													<!--end::Col-->
												</div>
												<!--end::Input group-->
											</div>
											<!--end::Card body-->
											<div class="card-body pt-0">
												<div class="table-responsive">
													<!--begin::Table-->
													<table
														class="table align-middle table-row-dashed fs-6 gy-7"
														id="kt_table_widget_4_table">
														<!--begin::Table head-->
														<thead>
															<!--begin::Table row-->
															<tr
																class="text-start text-gray-400 fw-bold fs-7 text-uppercase gs-0">
																<th class="min-w-50px"><%= Common.getBahasaConfig("No") %></th>
																<th class="min-w-100px"><%= Common.getBahasaConfig("Item Tagihan") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Tagihan") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Dibayar") %></th>
																<th class="min-w-50px"><%= Common.getBahasaConfig("Sisa") %></th>
															</tr>
															<!--end::Table row-->
														</thead>
														<!--end::Table head-->
														<!--begin::Table body-->
														<tbody class="fw-bold text-gray-600">
															<tr data-kt-table-widget-4="subtable_template">
																<td></td>
															</tr>
															<%
															int index = 1;
															for (DetailKegiatan detailKegiatan : detailKegiatans) {
																if (detailKegiatan.getBiaya().intValue() != 0) {
																	Number sumCicilan = 0.0;
																	try {
																if (detailKegiatan.getPengaturanPembayaranBulanan() != null) {
																	PengaturanPembayaranBulanan pengaturanPembayaranBulanan = detailKegiatan
																			.getPengaturanPembayaranBulanan();
																	sumCicilan = mahasiswa != null && mahasiswa.getId() != null
																			? mahasiswa.hitungTotalCicilan(kegiatan, pengaturanPembayaranBulanan, cicilanPembayarans)
																			: biodataCalonMahasiswa.hitungTotalCicilan(kegiatan, pengaturanPembayaranBulanan,
																					cicilanPembayarans);
																} else {
																	sumCicilan = nilais.get(detailKegiatan.getItemBiaya().getId());
																	if (sumCicilan == null) {
																		sumCicilan = 0.0;
																	}
																}
																	} catch (Exception e) {
																e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/ux/keuangan/detail_tagihan.jsp:196");
																	}
															%>
															<tr>
																<td><%=index%></td>
																<td class=""><%=detailKegiatan.getItemBiaya().getKode() + " - " + detailKegiatan.getItemBiaya().getNama()%></td>
																<td class=""><%=Common.numberFormat.get().format(detailKegiatan.getBiaya())%></td>
																<td class=""><%=Common.numberFormat.get().format(sumCicilan.doubleValue())%></td>
																<td class=""><%=Common.numberFormat.get().format(detailKegiatan.getBiaya() - sumCicilan.doubleValue())%></td>
															</tr>
															<%
															index++;
															}
															}
															%>

														</tbody>
														<!--end::Table body-->
														<tfoot>
															<tr class="border-top fw-semibold fs-6 text-gray-800">
																<th><%= Common.getBahasaConfig("Total") %></th>
																<th></th>
																<th><%=Common.numberFormat.get().format(kegiatan.getAmount() + kegiatan.getAmountTerhutang())%></th>
																<th><%=Common.numberFormat.get().format(kegiatan.getAmount())%></th>
																<th><%=Common.numberFormat.get().format(kegiatan.getAmountTerhutang())%></th>
															</tr>
														</tfoot>
													</table>
													<!--end::Table-->
												</div>
												<!--end::Card body-->
												<div
													class="card-footer d-flex justify-content-end py-6 px-9">
													<%
													if (!kegiatan.getLunas()) {
													%>
													<a
														href="<%=request.getContextPath()%>/pages/ux/keuangan/bayar_tagihan.jsp?id=<%=kegiatan.getId()%>&idsmt=<%=idsmt%>"
														class="btn btn-primary">Proses Pembayaran</a>&nbsp;
													<%
													}
													%>
													<a
														href="<%=request.getContextPath()%>/pages/ux/keuangan/tagihan.jsp?idsmt=<%=idsmt%>"
														class="btn btn-primary">Kembali</a>
												</div>
											</div>
											<!--end::Table Widget 4-->
										</div>
									</div>
									<!--end::Col-->
								</div>
								<!--end::Layout-->
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