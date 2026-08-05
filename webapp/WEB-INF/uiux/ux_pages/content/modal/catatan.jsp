<%@ page import="ais.common.Common" %>
<!-- Modal -->
<div class="modal fade" id="exampleModal" tabindex="-1"
	aria-labelledby="exampleModalLabel" aria-hidden="true" >
	<div class="modal-dialog modal-fullscreen" >
		<div class="modal-content" > 
			<div class="modal-header">
				<h1 class="modal-title fs-5" id="exampleModalLabel"></h1>
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
								<h3 class="card-title fw-bold">Catatan Kelas</h3>
							
							</div>
							<!-- /.card-header -->
							<div class="card-body">
								<!--begin::Row-->
								<div class="row">
									<div class="col-md-8">
										<p class="text-start">
											
											
											<div class="mb-3">
  <label for="exampleFormControlTextarea1" class="form-label"> <strong>Bahan Kajian :</strong></label>
  <textarea class="form-control" id="exampleFormControlTextarea1" rows="2" placeholder="masukan bahan kajian..."></textarea>
</div>
										</p>
										
										<p class="text-start">
											
											<div class="mb-3">
  <label for="exampleFormControlTextarea1" class="form-label"> <strong>Metode Pembelajaran :</strong></label>
  <textarea class="form-control" id="exampleFormControlTextarea1" rows="2" placeholder="masukan metode pembelajaran..."></textarea>
</div>
										</p>
										
										<p class="text-start">
											
											<div class="mb-3">
  <label for="exampleFormControlTextarea1" class="form-label"><strong>Refrensi :</strong></label>
  <textarea class="form-control" id="exampleFormControlTextarea1" rows="2" placeholder="masukan refrensi..."></textarea>
</div>
										</p>
										
										<p class="text-start">
											
											<div class="mb-3">
  <label for="exampleFormControlTextarea1" class="form-label"><strong>Catatan :</strong></label>
  <textarea class="form-control" id="exampleFormControlTextarea1" rows="2" placeholder="masukan catatan..."></textarea>
</div>
										</p>
										
										<div id="sales-chart"></div>
									</div>
									<!-- /.col -->
									<div class="col-md-4">
					
										
										<!-- /.info-box -->
							<div class="card mb-4">
								<div class="card-header">
									<h5 class="card-title">Rangkuman</h5>
		
								</div>
								<!-- /.card-header -->
								
								<!-- /.card-body -->
								<div class="card-footer p-0">
									<ul class="nav nav-pills flex-column">
										<li class="nav-item"><a href="#" class="nav-link">
												Tugas <span class="float-end text-danger"> 3
											</span>
										</a></li>
										<li class="nav-item"><a href="#" class="nav-link">
												Ujian <span class="float-end text-success">  1
											</span>
										</a></li>
										<li class="nav-item"><a href="#" class="nav-link">
												Materi <span class="float-end text-info">  3
											</span>
										</a></li>
									</ul>
								</div>
								<!-- /.footer -->
							</div>
							<!-- /.card -->
							
										
									</div>
									<!-- /.col -->
								</div>
								<!--end::Row-->
							</div>
							<!-- ./card-body -->
							

							<div class="modal-footer">
								<button type="button" class="btn btn-secondary"
									data-bs-dismiss="modal"><%= Common.getBahasaConfig("Tutup") %></button>
								<button type="button" class="btn btn-primary"><%= Common.getBahasaConfig("Simpan") %></button>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
		</div>
		</div>