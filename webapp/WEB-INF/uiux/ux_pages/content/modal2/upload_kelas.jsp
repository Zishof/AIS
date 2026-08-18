
<%@ page import="ais.common.Common" %>
<!-- Modal -->
<div class="modal fade" id="UploadKelas" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="staticBackdropLabel" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
    <div class="modal-content">
      <div class="modal-header">
        <h1 class="modal-title fs-5" id="staticBackdropLabel">Upload Kelas</h1>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
     <div class="input-group mb-3">
  <input type="file" class="form-control" id="inputGroupFile02">
   <button type="button" class="btn btn-primary"><%= Common.getBahasaConfig("Upload") %></button>

</div>
       </div>
     
    </div>
  </div>
</div>
<!--  Modal -->