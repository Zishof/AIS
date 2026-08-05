<%@page import="org.jsoup.Jsoup"%>
<%@page import="org.jsoup.nodes.Document"%>
<%@page import="ais.database.model.KategoriPengumuman"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.action.master.TampilanPengumumanAkademisAction"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="java.util.HashMap"%>
<%@page import="ais.database.model.PengumumanAkademis"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}

Map<Integer, List<PengumumanAkademis>> kategoriPengumumans = new HashMap<Integer, List<PengumumanAkademis>>();

PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
org.hibernate.Session sessionData = HibernateUtil.currentNativeSession();
Criteria criteria = TampilanPengumumanAkademisAction.initCriteriaStatic(true, tbmuser, selectedPerguruanTinggi, null,
		sessionData);
List<PengumumanAkademis> pengumumanAkademis = ConstantValues.simpleList(criteria, PengumumanAkademis.class);

for (PengumumanAkademis pengumumanAkademi : pengumumanAkademis) {
	Integer idKategori = pengumumanAkademi.getKategoriPengumuman() == null ? 1000000
	: pengumumanAkademi.getKategoriPengumuman().getNomorUrut();
	List<PengumumanAkademis> dataPengumumans = kategoriPengumumans.get(idKategori);
	if (dataPengumumans == null) {
		dataPengumumans = new ArrayList<PengumumanAkademis>();
		kategoriPengumumans.put(idKategori, dataPengumumans);
	}
	dataPengumumans.add(pengumumanAkademi);
}
pengumumanAkademis = null;
%>


<!--begin::List widget 16-->
<div class="card card-flush h-xl-100">
	<!--begin::Header-->
	<div class="card-header pt-7">
		<!--begin::Title-->
		<h3 class="card-title align-items-start flex-column">
			<span class="card-label fw-bold text-gray-800">Informasi</span>
		</h3>
		<!--end::Title-->
	</div>
	<!--end::Header-->
	<!--begin::Body-->
	<div class="card-body pt-4 px-0">
		<!--begin::Nav-->
		<ul
			class="nav nav-pills nav-pills-custom item position-relative mx-9 mb-9">

			<%
			int indexKat = 0;
			for (Integer idKat : kategoriPengumumans.keySet()) {
				
				List<PengumumanAkademis> dataPengumumans = kategoriPengumumans.get(idKat);
				
				KategoriPengumuman kategoriPengumuman = dataPengumumans.isEmpty() ? null
				: dataPengumumans.get(0).getKategoriPengumuman();
			%>

			<!--begin::Item-->
			<li class="nav-item col-2 mx-0 p-0">
				<!--begin::Link--> <a
				class="nav-link  <%=indexKat == 0 ? "active" : ""%> d-flex justify-content-center w-100 border-0 h-100"
				data-bs-toggle="pill" href="#kt_list_widget_16_tab_<%=idKat%>">
					<!--begin::Subtitle--> <span
					class="nav-text text-gray-800 fw-bold fs-6 mb-3"><%=(kategoriPengumuman == null ? "Pengumuman" : kategoriPengumuman.getNama())%></span>
					<!--end::Subtitle--> <!--begin::Bullet--> <span
					class="bullet-custom position-absolute z-index-2 bottom-0 w-100 h-4px bg-primary rounded"></span>
					<!--end::Bullet-->
			</a> <!--end::Link-->
			</li>
			<!--end::Item-->
			<%
			indexKat++;
			}
			%>

			<!--begin::Bullet-->
			<span
				class="position-absolute z-index-1 bottom-0 w-100 h-4px bg-light rounded"></span>
			<!--end::Bullet-->
		</ul>
		<!--end::Nav-->
		<!--begin::Tab Content-->
		<div class="tab-content px-9 hover-scroll-overlay-y pe-7 me-3 mb-2">


			<%
			indexKat = 0;
			for (Integer idKat : kategoriPengumumans.keySet()) {
				List<PengumumanAkademis> dataPengumumans = kategoriPengumumans.get(idKat);
			%>


			<!--begin::Tap pane-->
			<div class="tab-pane fade show <%=indexKat == 0 ? "active" : ""%>"
				id="kt_list_widget_16_tab_<%=idKat%>">


				<%
				for (PengumumanAkademis akademis : dataPengumumans) {
					Document doc = Jsoup.parse(akademis.getCatatan());
					String textCatatan = doc.body().text();
				%>
				<!--begin::Item-->
				<div class="m-0">
					<!--begin::Timeline-->
					<div class="timeline ms-n1">
						<!--begin::Timeline item-->
						<div class="timeline-item align-items-center mb-4">
							<!--begin::Timeline icon-->
							<div class="timeline-icon pt-1" style="margin-left: 0.7px">
								<!--begin::Svg Icon | path: icons/duotune/general/gen015.svg-->
								<span class="icon-dashboard"> <i
									class="fad fa-comment-alt-exclamation"></i>
								</span>
								<!--end::Svg Icon-->
							</div>
							<!--end::Timeline icon-->
							<!--begin::Timeline content-->
							<div class="timeline-content m-0">

								<!--begin::Title-->
								<a
									href="<%=request.getContextPath()%>/pages/ux/pengumuman_rinci.jsp?id=<%=akademis.getId()%>"
									class="fs-6 text-gray-800 fw-bold d-block text-hover-primary text-limit-1-row"><%=akademis.getJudul()%></a>
								<!--end::Title-->
								<%
								if (request.getParameter("rinci") != null) {
								%>
								<!--begin::Title-->
								<span class="fw-semibold text-gray-400 text-limit-3-row">

									<%=textCatatan%>

								</span>
								<!--end::Title-->
								<%
								}
								%>
							</div>
							<!--end::Timeline content-->
						</div>
						<!--end::Timeline item-->
					</div>
					<!--end::Timeline-->
				</div>
				<!--end::Item-->
				<!--begin::Separator-->
				<div class="separator separator-dashed mt-5 mb-4"></div>
				<!--end::Separator-->

				<%
				}
				%>

			</div>
			<!--end::Tap pane-->
			<%
			indexKat++;
			}
			%>
		</div>
		<!--end::Tab Content-->
	</div>
	<!--end: Card Body-->
</div>
<!--end::List widget 16-->