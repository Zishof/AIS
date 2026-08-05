<%@ page import="ais.common.Common" %>
<!-- Modal -->
<div class="modal fade" id="BuatKelas" data-bs-backdrop="static"
	data-bs-keyboard="false" tabindex="-1"
	aria-labelledby="staticBackdropLabel" aria-hidden="true">
	<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
		<div class="modal-content">
			<div class="modal-header">
				<h1 class="modal-title fs-5" id="staticBackdropLabel">Buat
					Kelas</h1>
				<button type="button" class="btn-close" data-bs-dismiss="modal"
					aria-label="Close"></button>
			</div>
			<div class="modal-body">

				<div class="mb-3">
					<label for="formGroupExampleInput" class="form-label"><%= Common.getBahasaConfig("Kode Kelas") %></label> <input type="text" class="form-control"
						id="formGroupExampleInput" placeholder="">
				</div>
				<div class="mb-3">
					<label for="formGroupExampleInput2" class="form-label"><%= Common.getBahasaConfig("Nama Kelas") %></label> <input type="text" class="form-control"
						id="formGroupExampleInput2" placeholder="">
				</div>
				<div class="mb-3">
					<label for="formGroupExampleInput3" class="form-label"><%= Common.getBahasaConfig("Matapelajaran") %></label> <select class="form-select"
						aria-label="Default select example">
						<option selected>pilih Mapel</option>
						<option value="1">MTK</option>
						<option value="2">IPA</option>
						<option value="3">Biologi</option>
					</select>
				</div>
				<div class="mb-3">
					<label for="formGroupExampleInput4" class="form-label"><%= Common.getBahasaConfig("Tanggal dan Waktu") %></label> 
					
  
</div>
<div class="mb-3">
					<label for="formGroupExampleInput5" class="form-label"><%= Common.getBahasaConfig("Ruangan Kelas") %></label> <select class="form-select"
						aria-label="Default select example">
						<option selected>pilih Ruangan</option>
						<option value="1">001</option>
						<option value="2">002</option>
						<option value="3">003</option>
					</select>
				</div>
				<div class="mb-3">
					<label for="formGroupExampleInput6" class="form-label"><%= Common.getBahasaConfig("Pilih Kelas") %></label> <select class="form-select"
						aria-label="Default select example">
						<option selected>pilih Kelas</option>
						<option value="1">XI A</option>
						<option value="2">XI B</option>
						<option value="3">XI C</option>
					</select>
				</div>
				<div class="mb-3">
					<label for="formGroupExampleInput7" class="form-label"><%= Common.getBahasaConfig("Pilih Guru") %></label> 
						<select class="form-select"
						aria-label="Default select example">
						<option selected>pilih Guru</option>
						<option value="1">Samuji</option>
						<option value="2">Sonhaji</option>
						<option value="3">Jaya Katwang</option>
					</select>
				</div>

			</div>
			 <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-dismiss="modal" aria-label="Close"><%= Common.getBahasaConfig("Close") %></button>
        <button type="button" class="btn btn-primary"><%= Common.getBahasaConfig("Understood") %></button>
      </div>
				</div>
				
			
		</div>
	</div>

<!--  Modal -->