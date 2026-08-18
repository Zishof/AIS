<%@ page import="ais.common.Common" %>
<!-- Button trigger modal -->

<a href=""
	class="small-box-footer link-light link-underline-opacity-0 link-underline-opacity-50-hover" data-bs-toggle="modal" data-bs-target="#exampleModal">
	selengkapnya<i class="bi bi-link-45deg"></i>
</a>

<!-- Modal -->
<div class="modal fade" id="exampleModal" tabindex="-1"
	aria-labelledby="exampleModalLabel" aria-hidden="true">
	<div class="modal-dialog modal-xl">
		<div class="modal-content">
			<div class="modal-header">
				<h1 class="modal-title fs-5" id="exampleModalLabel">Modal title</h1>
				<button type="button" class="btn-close" data-bs-dismiss="modal"
					aria-label="Close"></button>
			</div>
			<div class="modal-body">
			<!-- content modal -->
			
			</div>
			
			<div class="modal-footer">
				<button type="button" class="btn btn-secondary"
					data-bs-dismiss="modal"><%= Common.getBahasaConfig("Close") %></button>
				<button type="button" class="btn btn-primary"><%= Common.getBahasaConfig("Save changes") %></button>
			</div>
		</div>
	</div>
</div>