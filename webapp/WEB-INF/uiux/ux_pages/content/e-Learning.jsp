\
<%@page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.VOPembelajaran"%>
<%@page import="ais.database.model.sekolah.JadwalPelajaran"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.action.servlet.Api"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Perkuliahan"%>

<%
/*
jenis 
PERKULIAHAN = 1;
SKRIPSI = 2;
 BIMBINGAN = 3;
 KKN = 4;
 PKL = 5;
 KRS = 6;
 KONSULTASI = 7;
 PELAJARAN = 8;
 KEGIATAN = 9;
*/
JSONObject jsonObject = new JSONObject();
jsonObject.put("action", "agenda");
jsonObject.put("jenis", "1");
//jsonObject.put("tahunAkademik", Common.getCurrentTahunAkademik() );
jsonObject.put("hari", "");
//jsonObject.put("jenisSemester", Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP );
jsonObject.put("keyword", "");
jsonObject.put("refresh", "true");
jsonObject.put("jumlahDataDalamSatuHalaman", "10");
jsonObject.put("halaman", "0");

JSONObject kembalian = Api.ambil(request, jsonObject);
JSONArray data = kembalian.getJSONArray("data");

//out.println(data);
%>


<main class="app-main">

	<!--begin::App Content Header-->

	<div class="app-content-header">


		<div class="container-fluid">

			<!--begin::Row-->

			<div class="row">


				<!-- judul -->

				<!--begin::Header-->
				<div class="card-header p-2">
					<div class="row d-flex justify-content-between">
						<div class="card-title col-md-3 fw-bold">
							<h3>Ruang Kelas</h3>
						</div>
						<!--  search bar-->
						<div class="col-md-3">
							<form class="d-flex me-2" role="search">
								<input class="form-control me-1 " type="search"
									placeholder="Cari Kelas" aria-label="Cari">
								<button class="btn btn-outline-success" type="submit"><%= Common.getBahasaConfig("Cari") %></button>
							</form>
						</div>
						<!--  search bar-->

					</div>
				</div>


				<!--end::Header-->

				<div class="col-sm-12">
					<div class="me-5 d-flex justify-content-end">

						<jsp:include
							page="/WEB-INF/uiux/ux_pages/content/modal2/buat_kelas.jsp"></jsp:include>
						<jsp:include
							page="/WEB-INF/uiux/ux_pages/content/modal2/upload_kelas.jsp"></jsp:include>
						<a type="button" class="btn btn-primary btn-sm mt-3 me-3 "
							data-bs-toggle="modal" data-bs-target="#BuatKelas""> Buat
							Kelas</a> <a type="button"
							class="btn btn-danger btn-sm mt-3 me-3 ms-3"
							data-bs-toggle="modal" data-bs-target="#UploadKelas"> Upload
							Kelas</a> <a
							href="<%=request.getContextPath()%>/main2?page=ruang_kelas"
							type="button" class="btn btn-success btn-sm mt-3 ms-3">
							Download Kelas</a>
					</div>
				</div>

			</div>

			<!--end::Row-->

		</div>


	</div>


	<div class="app-content">

		<!--begin::Container-->

		<div class="container-fluid">

			<!--begin::Row-->

			<div class="row">

				<!--begin::Col-->
				<div class="col-md-12">

					<%
					for (int i = 0; i < data.length(); i++) {

						try {

							JSONObject dataKu = data.getJSONObject(i);
							Long idData = ais.common.CommonJSONUtil.ambilLong(dataKu,"id");
							VOPembelajaran pembelajaran = (VOPembelajaran) ConstantValues.ambil(dataKu.getString("class"), idData);

							String namaKelas = pembelajaran.ambilKelas();
							String info = pembelajaran.infoSangatSimple();
							String infoJadwal = "";
							if (pembelajaran instanceof JadwalPelajaran) {
						JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) pembelajaran;
						info = jadwalPelajaran.getMatapelajaran().getNama();

						infoJadwal = Common.displayHariJamRuanganJadwalPelajaran(jadwalPelajaran).getContent();

							} else if (pembelajaran instanceof Perkuliahan) {
						Perkuliahan perkuliahan = (Perkuliahan) pembelajaran;
						info = perkuliahan.getMatakuliah().getNama();

						if (perkuliahan.getWaktuMulai() != null && perkuliahan.getWaktuSelesai() != null) {
							infoJadwal = perkuliahan.getHari() + ", " + perkuliahan.getWaktuMulai() + " sd "
									+ perkuliahan.getWaktuSelesai();
						}
							}
					%>
					<!--begin::Accordion-->
					<div class="accordion mb-3" id="accordionExample">
						<div class="accordion-item ">
							<div class="accordion-header ">
							
									<button class="accordion-button collapsed bg-info-50"
										type="button" data-bs-toggle="collapse"
										data-bs-target="#collapse<%=idData%>" aria-expanded="true"
										aria-controls="collapse<%=idData%>">
										<span class="info-box-icon"> <i class="icon-custom">
												<svg class="mt-3" xmlns="http://www.w3.org/2000/svg"
													width="2em" height="2em" viewBox="0 0 20 20 "
													aria-hidden="true">
																<path fill="currentColor" fill-rule="evenodd"
														d="M12.402 8.976H7.259a2.278 2.278 0 0 0-.193-4.547h-1.68A3.095 3.095 0 0 0 4.609 0h7.793a1.35 1.35 0 0 1 1.348 1.35v6.279c0 .744-.604 1.348-1.348 1.348ZM2.898 4.431a1.848 1.848 0 1 0 0-3.695a1.848 1.848 0 0 0 0 3.695m5.195 2.276c0-.568-.46-1.028-1.027-1.028H2.899a2.65 2.65 0 0 0-2.65 2.65v1.205c0 .532.432.963.964.963h.172l.282 2.61A1 1 0 0 0 2.66 14h.502a1 1 0 0 0 .99-.862l.753-5.404h2.16c.567 0 1.027-.46 1.027-1.027Z"
														clip-rule="evenodd" />
														</svg>
										</i>

										</span>
											<h5 class="fw-bold"><%=pembelajaran.infoSangatSimple()%></h5></button>
								
							</div>
							<div id="collapse<%=idData%>" class="accordion-collapse collapse"
								data-bs-parent="#accordionExample">
								<div class="accordion-body">

									<!--  info box -->

									<div class="row">

										<div class="col-lg-8 ">

											<div class="mb-auto p-1">



												<div class="info-box-content2 mt-5">
													<strong> <%=info%></strong>

													<div class="info-box-text">kelas XI A</div>
													<div class="info-box-text">Pertemuan ke-10</div>
													<div class="info-box-text"><%=infoJadwal%></div>
													<div class="info-box-text">pukul 08:00 - 09:30 WIB</div>
													<div class="info-box-text">40 Siswa</div>
													<div class="mt-3">
														<a
															href="<%=request.getContextPath()%>/main2?page=ruang_kelas"
															type="button" class="btn btn-danger btn-lg mt-3">
															Masuk Kelas <svg xmlns="http://www.w3.org/2000/svg"
																width="16" height="16" fill="currentColor"
																class="bi bi-door-open-fill" viewBox="0 0 16 16">
  <path
																	d="M1.5 15a.5.5 0 0 0 0 1h13a.5.5 0 0 0 0-1H13V2.5A1.5 1.5 0 0 0 11.5 1H11V.5a.5.5 0 0 0-.57-.495l-7 1A.5.5 0 0 0 3 1.5V15zM11 2h.5a.5.5 0 0 1 .5.5V15h-1zm-2.5 8c-.276 0-.5-.448-.5-1s.224-1 .5-1 .5.448.5 1-.224 1-.5 1" />
</svg>
														</a>

													</div>
												</div>


												<!-- /.info-box-content -->

											</div>
										</div>



										<div class="col-lg-4">
											<div class="list-group">

												<a href="#" class="list-group-item active"
													aria-current="false">
													<div class="d-flex w-100 justify-content-between">
														<h4 class="mb-1">Ringkasan Kelas</h4>

													</div>

												</a> <a href="#" class="list-group-item list-group-item-action "
													aria-current="false">
													<div class="d-flex w-100 justify-content-between">
														<h5 class="mb-1">Pertemuan</h5>
														<span class="badge text-bg-primary rounded-pill">14</span>
													</div>

												</a> <a href="#" class="list-group-item list-group-item-action">
													<div class="d-flex w-100 justify-content-between">
														<h5 class="mb-1">Tugas Individu</h5>
														<span class="badge text-bg-primary rounded-pill">14</span>
													</div>

												</a> <a href="#" class="list-group-item list-group-item-action">
													<div class="d-flex w-100 justify-content-between">
														<h5 class="mb-1">Tugas Kelompok</h5>
														<span class="badge text-bg-primary rounded-pill">14</span>
													</div>

												</a> <a href="#" class="list-group-item list-group-item-action">
													<div class="d-flex w-100 justify-content-between">
														<h5 class="mb-1">Ujian</h5>
														<span class="badge text-bg-primary rounded-pill">14</span>
													</div>

												</a> <a href="#" class="list-group-item list-group-item-action">
													<div class="d-flex w-100 justify-content-between">
														<h5 class="mb-1">Materi</h5>
														<span class="badge text-bg-primary rounded-pill">14</span>
													</div>

												</a>
											</div>
										</div>








									</div>
									<!-- info box -->

								</div>
							</div>
						</div>


					</div>
					<%
					} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/uiux/ux_pages/content/e-Learning.jsp:285");

					}

					}
					%>
					<!--end::Body-->
					<!--end::Accordion-->



				</div>


			</div>
		</div>

		<!--end::Row-->

	</div>

	<!--end::Container-->


</main>

<!--end::App Main-->