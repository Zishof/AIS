
<!--begin:::Tab content-->
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.KrsMahasiswa"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.database.model.CicilanPembayaran"%>
<%@page import="java.util.List"%>
<%
KrsMahasiswa krsMahasiswa = (KrsMahasiswa) request.getAttribute("krsMahasiswa");
String idsmt = (String)request.getAttribute("idsmt");
Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();

Integer semester = krsMahasiswa.getSemester();
boolean refresh = true;


if (ais.common.CommonHelperClass.jenisKegiatansTanpaDaftarUlang == null) {
	Common.reloadJenisKegiatans();
}
%>
<div class="tab-content" id="myTabContent">
	<!--begin:::Tab pane-->
	<div class="tab-pane fade" id="kt_ecommerce_customer_general"
		role="tabpanel">
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
							<span class="card-label fw-bold text-gray-800">Riwayat
								Pembayaran</span> <span class="text-gray-400 mt-1 fw-semibold fs-6"></span>
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
											<label class="form-label fs-6 fw-semibold"><%= Common.getBahasaConfig("Tahun Akademik") %></label> <select
												class="form-select form-select-solid fw-bold"
												data-kt-select2="true" data-placeholder="Select option"
												data-allow-clear="true"
												data-kt-subscription-table-filter="status"
												data-hide-search="true">
												<option></option>
												<option value="Active">2018/2019 Semester Ganjil</option>
												<option value="Expiring">2018/2019 Semester Ganjil</option>
												<option value="Suspended">2018/2019 Semester Ganjil</option>
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
							</div>
							<!--end::Toolbar-->
						</div>
					</div>
					<!--end::Card header-->
					<!--begin::Card body-->
					<div class="card-body pt-2">
						<div class="table-responsive">
							<!--begin::Table-->
							<table class="table align-middle table-row-dashed fs-6 gy-5"
								id="kt_table_widget_4_table">
								<!--begin::Table head-->
								<thead>
									<!--begin::Table row-->
									<tr
										class="text-start align-middle text-gray-400 fw-bold fs-7 text-uppercase gs-0">
										<th class="min-w-100px"><%= Common.getBahasaConfig("Tanggal Pembayaran") %></th>
										<th class="min-w-50px"><%= Common.getBahasaConfig("Validator") %></th>
										<th class="min-w-100px"><%= Common.getBahasaConfig("Item Tagihan") %></th>
										<th class="min-w-75px"><%= Common.getBahasaConfig("Tahun Akademik") %></th>
										<th class="min-w-50px"><%= Common.getBahasaConfig("Nominal") %></th>
										<th class="min-w-50px"><%= Common.getBahasaConfig("Bukti Pembayaran") %></th>
									</tr>
									<!--end::Table row-->
								</thead>
								<!--end::Table head-->
								<!--begin::Table body-->
								<tbody class="text-gray-600">
									
									<%
									
									List<CicilanPembayaran> cicilanPembayarans = new ArrayList<CicilanPembayaran>();
									BiodataCalonMahasiswa biodataCalonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();
									System.out.println("biodataCalonMahasiswa -> " + biodataCalonMahasiswa);
									if (biodataCalonMahasiswa != null) {
										cicilanPembayarans.addAll(biodataCalonMahasiswa.ambilCicilan());
									}
									cicilanPembayarans.addAll(mahasiswa.ambilCicilan());
									
									
									for(CicilanPembayaran cicilanPembayaran : cicilanPembayarans){
									%>
								
									
									<tr>
										<td class=""><%=Common.dateFormat51.get().format(cicilanPembayaran.getTanggal()) %></td>
										<td class=""><%=cicilanPembayaran.getValidator() %></td>
										<td class=""><%=cicilanPembayaran.getItemBiaya().getNama() %></td>
										<td class=""><%=cicilanPembayaran.getKegiatan().getTahunAkademik() %> Semester <%=cicilanPembayaran.getKegiatan().getSemster() %></td>
										<td class=""><%=Common.numberFormat.get().format(cicilanPembayaran.getNilai()) %></td>
										<td class=""><a href="#"
											class="btn btn-light btn-active-light-primary btn-sm">Lihat</a>
										</td>
									</tr>
									
									<%
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
	<!--end::tab pane-->
</div>
<!--end::tab content-->