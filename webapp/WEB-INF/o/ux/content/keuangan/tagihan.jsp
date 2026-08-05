<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="java.util.TreeMap"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.database.model.CicilanPembayaran"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.KrsMahasiswa"%>
<%@page import="ais.action.ws.util.PembayaranUtil"%>
<%@page import="java.util.Collection"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%
KrsMahasiswa krsMahasiswa = (KrsMahasiswa) request.getAttribute("krsMahasiswa");
String idsmt = (String)request.getAttribute("idsmt");
Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();

Integer semester = krsMahasiswa.getSemester();
boolean refresh = true;

TreeMap<String, Object[]> semua = new TreeMap<String, Object[]>();

if (ais.common.CommonHelperClass.jenisKegiatansTanpaDaftarUlang == null) {
	Common.reloadJenisKegiatans();
}
%>

<!--begin:::Tab content-->
<div class="tab-content" id="myTabContent">
	<!--begin:::Tab pane-->
	<div class="tab-pane fade show active"
		id="kt_ecommerce_customer_overview" role="tabpanel">
		<!--begin::Row-->
		<div class="row g-5 g-xl-10">
			<!--begin::Col-->
			<div class="col-xl-12">
				<!--begin::Table Widget 4-->
				<div class="card card-flush h-xl-100">
					<!--begin::Card header-->
					<div class="card-header pt-2">
						<!--begin::Title-->
						<h3 class="card-title align-items-start flex-column">
							<span class="card-label fw-bold text-gray-800">Daftar
								Tagihan</span> <span class="text-gray-400 mt-1 fw-semibold fs-6"></span>
						</h3>
						<!--end::Title-->
						<div class="card-toolbar">
							<!--begin::Toolbar-->
							<div class="d-flex justify-content-end"
								data-kt-subscription-table-toolbar="base">
								<!--begin::Filter-->
								<button type="button" class="btn btn-light-primary me-3"
									data-kt-menu-trigger="click"
									data-kt-menu-placement="bottom-end">
									<!--begin::Svg Icon | path: icons/duotune/general/gen031.svg-->
									<span class="svg-icon svg-icon-2"> <i
										class="fas fa-filter"></i>
									</span>
									<!--end::Svg Icon-->
									Filter
								</button>
								<!--begin::Menu 1-->
								<div class="menu menu-sub menu-sub-dropdown w-300px w-md-325px"
									data-kt-menu="true">
									<!--begin::Header-->
									<div class="px-7 py-5">
										<div class="fs-5 text-dark fw-bold">Filter Options</div>
									</div>
									<!--end::Header-->
									<!--begin::Separator-->
									<div class="separator border-gray-200"></div>
									<!--end::Separator-->
									<!--begin::Content-->
									<div class="px-7 py-5" data-kt-subscription-table-filter="form">



										<!--begin::Input group-->
										<div class="mb-10">
											<label class="form-label fs-6 fw-semibold"><%= Common.getBahasaConfig("Jenis Tagihan") %></label> <select
												class="form-select form-select-solid fw-bold"
												data-kt-select2="true" data-placeholder="Select option"
												data-allow-clear="true"
												data-kt-subscription-table-filter="month"
												data-hide-search="true">
												<option></option>
												<option value="Show All">Semua</option>
												<option value="Daftar Ulang">Daftar Ulang</option>
												<option value="Pembayaran SP">Pembayaran SP</option>
												<option value="Biaya Skripsi">Biaya Skripsi</option>
												<option value="Pendaftaran Wisuda">Pendaftaran
													Wisuda</option>
											</select>
										</div>
										<!--end::Input group-->



										<!--begin::Input group-->
										<div class="mb-10">
											<label class="form-label fs-6 fw-semibold"><%= Common.getBahasaConfig("Status") %></label> <select
												class="form-select form-select-solid fw-bold"
												data-kt-select2="true" data-placeholder="Select option"
												data-allow-clear="true"
												data-kt-subscription-table-filter="product"
												data-hide-search="true">
												<option></option>
												<option value="Basic">Lunas</option>
												<option value="Basic Bundle">Belum Lunas</option>
												<option value="Teams">Verifikasi</option>
											</select>
										</div>
										<!--end::Input group-->
										<!--begin::Actions-->
										<div class="d-flex justify-content-end">
											<button type="submit"
												class="btn btn-primary fw-semibold px-6"
												data-kt-menu-dismiss="true"
												data-kt-subscription-table-filter="filter"><%= Common.getBahasaConfig("Apply") %></button>
										</div>
										<!--end::Actions-->
									</div>
									<!--end::Content-->
								</div>
								<!--end::Menu 1-->
								<!--end::Filter-->
								<!--begin::Export-->
								<button type="button" class="btn btn-light-primary me-3">
									<!--begin::Svg Icon | path: icons/duotune/arrows/arr078.svg-->
									<span class="svg-icon svg-icon-2"> <i
										class="fas fa-file-pdf"></i>
									</span>
									<!--end::Svg Icon-->
									Cetak Tagihan
								</button>
								<!--end::Export-->
								<!--begin::Export-->
								<button type="button" class="btn btn-light-primary me-3">
									<!--begin::Svg Icon | path: icons/duotune/arrows/arr078.svg-->
									<span class="svg-icon svg-icon-2"> <i
										class="fas fa-file-pdf"></i>
									</span>
									<!--end::Svg Icon-->
									Surat Bebas Tunggakan
								</button>
								<!--end::Export-->
							</div>
							<!--end::Toolbar-->
						</div>


					</div>
					<!--end::Card header-->
					<!--begin::Card body-->
					<div class="card-body pt-2">
						<div class="table-responsive">
							<jsp:include
								page="/WEB-INF/o/ux/content/elearning/pilihan_tahun_akademik.jsp"></jsp:include>

							<!--begin::Table-->
							<table class="table align-middle table-row-dashed fs-6 gy-5"
								id="kt_table_widget_4_table">
								<!--begin::Table head-->
								<thead>
									<!--begin::Table row-->
									<tr
										class="text-start align-middle text-gray-400 fw-bold fs-7 text-uppercase gs-0">
										<th class="min-w-50px"><%= Common.getBahasaConfig("Kode") %></th>
										<th class="min-w-100px"><%= Common.getBahasaConfig("Item Tagihan") %></th>
										<th class="min-w-50px"><%= Common.getBahasaConfig("Tagihan") %></th>
										<th class="min-w-50px"><%= Common.getBahasaConfig("Dibayar") %></th>
										<th class="min-w-50px"><%= Common.getBahasaConfig("Sisa") %></th>
										<th class="min-w-50px"><%= Common.getBahasaConfig("Status") %></th>
										<th class="min-w-50px"><%= Common.getBahasaConfig("Action") %></th>
									</tr>
									<!--end::Table row-->
								</thead>
								<!--end::Table head-->
								<!--begin::Table body-->
								<tbody class="text-gray-600">

									<%
									List<Kegiatan> kegiatans = new ArrayList<Kegiatan>();
									BiodataCalonMahasiswa biodataCalonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();
									System.out.println("biodataCalonMahasiswa -> " + biodataCalonMahasiswa);
									if (biodataCalonMahasiswa != null) {
										kegiatans.addAll(biodataCalonMahasiswa.ambilKegiatans(null, refresh));
									}
									kegiatans.addAll(mahasiswa.ambilKegiatans(semester, refresh));

									for (Kegiatan kegiatan : kegiatans) {
										System.out.println("kegiatan -> " + kegiatan);
										if (kegiatan != null && ((int) (kegiatan.getAmount() + kegiatan.getAmountTerhutang())) != 0) {

											String statusLunas = !kegiatan.getLunas()
											? "<span class=\"badge badge-light-danger\">Belum Lunas ("
													+ Common.numberFormat.get().format(kegiatan.getPersentaseLunas()) + "%)</span>"
											: "<span class=\"badge badge-light-success\">Lunas</span>";
									%>

									<tr>
										<td><%=kegiatan.getJenisKegiatan().getKode()%></td>
										<td class=""><%=kegiatan.getJenisKegiatan().getNamaKegiatan()%></td>
										<td class=""><%=Common.numberFormat.get().format(kegiatan.getAmount() + kegiatan.getAmountTerhutang())%></td>
										<td class=""><%=Common.numberFormat.get().format(kegiatan.getAmount())%></td>
										<td class=""><%=Common.numberFormat.get().format(kegiatan.getAmountTerhutang())%></td>
										<td class=""><%=statusLunas%></td>
										<td class=""><a
											href="<%=request.getContextPath()%>/pages/ux/keuangan/detail_tagihan.jsp?id=<%=kegiatan.getId()%>&idsmt=<%=idsmt %>"
											class="btn btn-light btn-active-light-primary btn-sm"><%=kegiatan.getLunas() ? "Lihat" : "Bayar" %></a>
										</td>
									</tr>

									<%
									}
									}
									%>

									
								</tbody>
								<!--end::Table body-->
							</table>
							<!--end::Table-->
						</div>
					</div>
					<!--end::Card body-->
				</div>
				<!--end::Table Widget 4-->
			</div>
			<!--end::Col-->
		</div>
		<!--end::Row-->
	</div>
</div>
<!--end::tab content-->