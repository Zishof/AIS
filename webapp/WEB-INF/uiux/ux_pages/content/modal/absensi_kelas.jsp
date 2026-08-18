<%@ page import="ais.common.Common" %>
<!-- Button trigger modal -->

<!-- <a href=""
	class="small-box-footer link-light link-underline-opacity-0 link-underline-opacity-50-hover"
	data-bs-toggle="modal" data-bs-target="#exampleModal"> selengkapnya<i
	class="bi bi-link-45deg"></i>
</a> -->

<a href=""
	class="small-box-footer link-light link-underline-opacity-0 link-underline-opacity-50-hover"
	data-bs-toggle="modal" data-bs-target="#AbsenModal"> </a>

<!-- Modal -->
<div class="modal fade" id="AbsenModal" tabindex="-1"
	aria-labelledby="AbsenModalLabel" aria-hidden="true">
	<div class="modal-dialog modal-xl">
		<div class="modal-content">
			<div class="modal-header">
				<h1 class="modal-title fs-5" id="AbsenModalLabel"></h1>
				<!-- <button type="button " class="btn-close bg-danger"  data-bs-dismiss="modal"
					aria-label="Close"></button> -->
			</div>
			<div class="modal-body">
				<!-- content modal -->

				<!--begin::Row-->
				<div class="row">
					<div class="col-md-12">
						<div class="card mb-4">
							<div class="card-header">
								<h3 class="card-title fw-bold">Absen Kelas XI</h3>

							</div>
							<!-- /.card-header -->

							<!--begin::App Main-->

							<div class="row row-cols-3 row-cols-md-4 g-2 ms-2 me-2 mt-2 mb-3">

								<div class="col">
									<div class="card h-70">
										<img src="<%=request.getContextPath()%>/img/USER.png"
											class="card-img-top" alt="...">
										<div class="card-body">
											<h4 class="card-title fw-bold">Nama Siswa</h4>
										</div>

										<div>


											<div class="form-check form-check-inline ms-2">
												<input class="form-check-input" type="radio"
													name="inlineRadioOptions" id="inlineRadio1" value="option1">
												<label class="form-check-label" for="inlineRadio1"><%= Common.getBahasaConfig("Hadir") %></label>
											</div>
											<div class="form-check form-check-inline">
												<input class="form-check-input" type="radio"
													name="inlineRadioOptions" id="inlineRadio2" value="option2">
												<label class="form-check-label" for="inlineRadio2"><%= Common.getBahasaConfig("Izin") %></label>
											</div>
											<div class="form-check form-check-inline">
												<input class="form-check-input" type="radio"
													name="inlineRadioOptions" id="inlineRadio2" value="option2">
												<label class="form-check-label" for="inlineRadio2"><%= Common.getBahasaConfig("Alpha") %></label>
											</div>

											<p class="card-text ms-3 mb-3 me-3">
												<textarea class="form-control"
													id="exampleFormControlTextarea1" rows="2"
													placeholder="masukan keterangan..."></textarea>

											</p>



										</div>

									</div>
								</div>

								<div class="col">
									<div class="card h-70">
										<img src="<%=request.getContextPath()%>/img/USER.png"
											class="card-img-top me-3" alt="...">
										<div class="card-body">
											<h4 class="card-title fw-bold">Nama Siswa</h4>

										</div>

										<div>


											<div class="form-check form-check-inline ms-2">
												<input class="form-check-input" type="radio"
													name="inlineRadioOptions" id="inlineRadio1" value="option1">
												<label class="form-check-label" for="inlineRadio1"><%= Common.getBahasaConfig("Hadir") %></label>
											</div>
											<div class="form-check form-check-inline">
												<input class="form-check-input" type="radio"
													name="inlineRadioOptions" id="inlineRadio2" value="option2">
												<label class="form-check-label" for="inlineRadio2"><%= Common.getBahasaConfig("Izin") %></label>
											</div>
											<div class="form-check form-check-inline">
												<input class="form-check-input" type="radio"
													name="inlineRadioOptions" id="inlineRadio2" value="option2">
												<label class="form-check-label" for="inlineRadio2"><%= Common.getBahasaConfig("Alpha") %></label>
											</div>

											<p class="card-text ms-3 mb-3 me-3">
												<textarea class="form-control"
													id="exampleFormControlTextarea1" rows="2"
													placeholder="masukan keterangan..."></textarea>

											</p>



										</div>

									</div>

								</div>

								<div class="col">
									<div class="card h-100">
										<img src="<%=request.getContextPath()%>/img/USER.png"
											class="card-img-top" alt="...">
										<div class="card-body">
											<h4 class="card-title fw-bold">Nama Siswa</h4>

										</div>

										<div>


											<div class="form-check form-check-inline ms-2">
												<input class="form-check-input" type="radio"
													name="inlineRadioOptions" id="inlineRadio1" value="option1">
												<label class="form-check-label" for="inlineRadio1"><%= Common.getBahasaConfig("Hadir") %></label>
											</div>
											<div class="form-check form-check-inline">
												<input class="form-check-input" type="radio"
													name="inlineRadioOptions" id="inlineRadio2" value="option2">
												<label class="form-check-label" for="inlineRadio2"><%= Common.getBahasaConfig("Izin") %></label>
											</div>
											<div class="form-check form-check-inline">
												<input class="form-check-input" type="radio"
													name="inlineRadioOptions" id="inlineRadio2" value="option2">
												<label class="form-check-label" for="inlineRadio2"><%= Common.getBahasaConfig("Alpha") %></label>
											</div>

											<p class="card-text ms-3 mb-3 me-3">
												<textarea class="form-control"
													id="exampleFormControlTextarea1" rows="2"
													placeholder="masukan keterangan..."></textarea>

											</p>



										</div>

									</div>
								</div>
								<div class="col">
									<div class="card h-70">
										<img src="<%=request.getContextPath()%>/img/USER.png"
											class="card-img-top" alt="...">
										<div class="card-body">
											<h4 class="card-title fw-bold">Ilham Ramdhani</h4>

										</div>
										<div>


											<div class="form-check form-check-inline ms-2">
												<input class="form-check-input" type="radio"
													name="inlineRadioOptions" id="inlineRadio1" value="option1">
												<label class="form-check-label" for="inlineRadio1"><%= Common.getBahasaConfig("Hadir") %></label>
											</div>
											<div class="form-check form-check-inline">
												<input class="form-check-input" type="radio"
													name="inlineRadioOptions" id="inlineRadio2" value="option2">
												<label class="form-check-label" for="inlineRadio2"><%= Common.getBahasaConfig("Izin") %></label>
											</div>
											<div class="form-check form-check-inline">
												<input class="form-check-input" type="radio"
													name="inlineRadioOptions" id="inlineRadio2" value="option2">
												<label class="form-check-label" for="inlineRadio2"><%= Common.getBahasaConfig("Alpha") %></label>
											</div>

											<p class="card-text ms-3 mb-3 me-3">
												<textarea class="form-control"
													id="exampleFormControlTextarea1" rows="2"
													placeholder="masukan keterangan..."></textarea>

											</p>



										</div>
									</div>
								</div>

							</div>
							
						<div class="modal-footer">
							<button type="button" class="btn btn-secondary"
								data-bs-dismiss="modal"><%= Common.getBahasaConfig("Close") %></button>
							<button type="button" class="btn btn-primary"><%= Common.getBahasaConfig("Save changes") %></button>
						</div>
						</div>
						<!-- ./card-body -->

						


					</div>
				</div>
			</div>
		</div>
	</div>
</div>