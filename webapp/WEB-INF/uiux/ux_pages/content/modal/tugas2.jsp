<%@ page import="ais.common.Common" %>
<!-- Button trigger modal -->

<!-- <a href=""
	class="small-box-footer link-light link-underline-opacity-0 link-underline-opacity-50-hover"
	data-bs-toggle="modal" data-bs-target="#exampleModal"> selengkapnya<i
	class="bi bi-link-45deg"></i>
</a> -->

<a href=""
	class="small-box-footer link-light link-underline-opacity-0 link-underline-opacity-50-hover"
	data-bs-toggle="modal" data-bs-target="#tugasModal2"> Buka Tugas<i
	class="bi bi-link-45deg"></i>
</a>

<!-- Modal -->
<div class="modal fade" id="tugasModal2" tabindex="-1"
	aria-labelledby="tugasModal2Label" aria-hidden="true">
	<div class="modal-dialog modal-fullscreen">
		<div class="modal-content">
			<div class="modal-header">
				<h1 class="modal-title fs-5" id="tugasModal2Label"></h1>
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
								<h3 class="card-title fw-bold">Tugas Kelas</h3>

							</div>
							<!-- /.card-header -->
							
							 <!--begin::App Main-->
							 
<div class="row row-cols-1 row-cols-md-5 g-4 ms-3 me-3 mt-2 mb-5">

  <div class="col">
    <div class="card h-150">
      <img src="<%=request.getContextPath()%>/img/buku-contoh.png" class="card-img-top" alt="...">
      <div class="card-body">
        <h4 class="card-title">Judul Tugas</h4>
      </div>
      <div>
            <p class="card-text ms-3 mb-3">keterangan Tugas</p>
            <jsp:include
	page="/WEB-INF/uiux/ux_pages/content/modal/catatan.jsp"></jsp:include>
            
        <div class="d-grid gap-2 me-2 ms-2 mb-2">
  <button class="btn btn-primary" type="Buka Tugas"><%= Common.getBahasaConfig("Buka Tugas") %></button>
</div>

	
      </div>
  
    </div>
  </div>
  
  <div class="col">
    <div class="card h-150">
      <img src="<%=request.getContextPath()%>/img/buku-contoh.png" class="card-img-top me-3" alt="...">
      <div class="card-body">
        <h4 class="card-title">Judul Tugas</h4>
      
      </div>
      
       <div>
            <p class="card-text ms-3 mb-3">keterangan Tugas</p>
           
                   
        <div class="d-grid gap-2 me-2 ms-2 mb-2">
  <button class="btn btn-primary" type="Buka Tugas"><%= Common.getBahasaConfig("Buka Tugas") %></button>
</div>
         
      </div>
      
    </div>
    
  </div>
  
  <div class="col">
    <div class="card h-150">
      <img src="<%=request.getContextPath()%>/img/buku-contoh.png" class="card-img-top" alt="...">
      <div class="card-body">
        <h4 class="card-title">Judul Tugas</h4>
       
      </div>
      <div>
            <p class="card-text ms-3 mb-3">keterangan Tugas</p>
                    
        <div class="d-grid gap-2 me-2 ms-2 mb-2">
  <button class="btn btn-primary" type="Buka Tugas"><%= Common.getBahasaConfig("Buka Tugas") %></button>
</div>
      </div>
    </div>
  </div>
  <div class="col">
    <div class="card h-150">
      <img src="<%=request.getContextPath()%>/img/buku-contoh.png" class="card-img-top" alt="...">
      <div class="card-body">
        <h4 class="card-title">Judul Tugas</h4>
       
      </div>
      <div>
            <p class="card-text ms-3 mb-3">keterangan Tugas</p>
                    
        <div class="d-grid gap-2 me-2 ms-2 mb-2">
  <button class="btn btn-primary" type="Buka Tugas"><%= Common.getBahasaConfig("Buka Tugas") %></button>
</div>

            
            
      </div>
    </div>
  </div>
  
</div>
   
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