<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="java.util.HashMap"%>
<%@page import="ais.database.model.KategoriPengumuman"%>
<%@page import="java.util.Map"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.PengumumanAkademis"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.TampilanPengumumanAkademisAction"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Detailperkuliahan"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.KrsMahasiswa"%>

<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}

String ta = Common.getCurrentTahunAkademik();
boolean ganjil = Common.isNowSemensterGanjil();
String idsmt = request.getParameter("idsmt");

if (idsmt != null) {
	ta = (Integer.parseInt(idsmt.substring(0, 4)) - 1) + "/" + (idsmt.substring(0, 4));
	ganjil = Integer.parseInt(idsmt.substring(4, 5)) == 1;
}

Integer semester = Common.getSemester(tbmuser.getMahasiswa().getTahunangkatan(), ta,
		ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP, tbmuser.getMahasiswa().getPindahKeKampusIniMasukSemester(),
		tbmuser.getMahasiswa().getSemesterMulai());

KrsMahasiswa krsMahasiswa = tbmuser.getMahasiswa() == null ? null
		: Common.singkronkanKrsMahasiswa(tbmuser.getMahasiswa(), semester, null, null);

List<Long> detailperkuliahansData = tbmuser.getMahasiswa() == null ? new ArrayList<Long>()
		: Common.getDetailperkuliahans(tbmuser.getMahasiswa(), krsMahasiswa.getSemester(), krsMahasiswa.getTahapan(),
		null, krsMahasiswa.getSemesterPendek(), false, false, false, false);
%>


<!--begin::Col-->
<div class="col-xl-7 mb-5 mb-xl-10">
	<!--begin::Tables widget 14-->
	<div class="card card-flush h-md-100">
		<!--begin::Header-->
		<div class="card-header pt-7">
			<!--begin::Title-->
			<h3 class="card-title align-items-start flex-column">
				<span class="card-label fw-bold text-gray-800">e-Learning</span> <span
					class="text-gray-400 mt-1 fw-semibold fs-6"><%=detailperkuliahansData.size()%>
					Mata Kuliah Aktif</span>
			</h3>
			<!--end::Title-->
			<jsp:include page="/WEB-INF/o/ux/content/index/filter_tahun_akademik.jsp"></jsp:include>
		</div>
		<!--end::Header-->
		<!--begin::Body-->
		<div class="card-body pt-6">
			<!--begin::Table container-->
			<div class="table-responsive">
				<!--begin::Table-->
				<table class="table table-row-dashed align-middle gs-0 gy-3 my-0">
					<!--begin::Table head-->
					<thead>
						<tr class="fs-7 fw-bold text-gray-400 border-bottom-0">
							<th class="p-0 pb-3 min-w-225px text-start"><%= Common.getBahasaConfig("Mata Kuliah") %></th>
							<th class="p-0 pb-3 min-w-100px text-end"><%= Common.getBahasaConfig("Detail") %></th>
						</tr>
					</thead>
					<!--end::Table head-->
					<!--begin::Table body-->
					<tbody>
						<%
						for (Long detailperkuliahanid : detailperkuliahansData) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
							detailperkuliahanid.toString());
							if (detailperkuliahan != null) {
								Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
								if (perkuliahan != null) {

							String dosen = "";
							List<Dosen> dosens = perkuliahan.populateDosenBuNama();
							for (Dosen dd : dosens) {
								dosen += dosen.isEmpty() ? dd.getNama() : ", " + dd.getNama();
							}

							String fotoDosen = "<i class=\"fad fa-user\"></i>";
							if (!dosens.isEmpty()) {
								fotoDosen = "<img alt=\"Foto dosen\" src=\""
										+ CommonMedia.getUrlFotoPengguna(new Tbmuser(dosens.get(0)), 152, 114) + "\" />";
							}
						%>
						<tr>
							<td>
								<div class="d-flex align-items-center">
									<div class="symbol me-3 icon-elearning">
										<%=fotoDosen%>
									</div>
									<div class="d-flex justify-content-start flex-column">
										<a href="<%=request.getContextPath()%>/pages/ux/elearning_detail.jsp?id=<%=perkuliahan.getId()%>"
											class="text-gray-800 fw-bold text-hover-primary mb-1 fs-6"><%=perkuliahan.getMatakuliah().getKode()%>
											| <%=perkuliahan.getMatakuliah().getNama()%> | <%=perkuliahan.getIdSmt()%></a>
										<span class="text-gray-400 fw-semibold d-block fs-7"><%=(perkuliahan.getHari() == null ? "" : perkuliahan.getHari())%>
											<%=(perkuliahan.getWaktuMulai() == null ? "" : "(" + perkuliahan.getWaktuMulai())%>
											<%=(perkuliahan.getWaktuSelesai() == null ? "" : "s.d " + perkuliahan.getWaktuSelesai() + ")")%></span>
										<span class="text-gray-400 fw-semibold d-block fs-7"><%=dosen%></span>
									</div>
								</div>
							</td>


							<td class="text-end"><a
								href="<%=request.getContextPath()%>/pages/ux/elearning_detail.jsp?id=<%=perkuliahan.getId()%>"
								class="btn btn-sm btn-icon btn-bg-light btn-active-color-primary w-30px h-30px">
									<!--begin::Svg Icon | path: icons/duotune/arrows/arr001.svg-->
									<span class="svg-icon svg-icon-5 svg-icon-gray-700"> <svg
											width="24" height="24" viewBox="0 0 24 24" fill="none"
											xmlns="http://www.w3.org/2000/svg">
																					<path
												d="M14.4 11H3C2.4 11 2 11.4 2 12C2 12.6 2.4 13 3 13H14.4V11Z"
												fill="currentColor" />
																					<path opacity="0.3"
												d="M14.4 20V4L21.7 11.3C22.1 11.7 22.1 12.3 21.7 12.7L14.4 20Z"
												fill="currentColor" />
																				</svg>
								</span> <!--end::Svg Icon-->
							</a></td>
						</tr>
						<%
						}
						}
						}
						%>
					</tbody>
					<!--end::Table body-->
				</table>
			</div>
			<!--end::Table-->
		</div>
		<!--end: Card Body-->
	</div>
	<!--end::Tables widget 14-->
</div>
<!--end::Col-->