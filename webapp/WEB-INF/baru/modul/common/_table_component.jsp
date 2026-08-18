<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
// Menangkap parameter yang dilempar dari parent page
String rnd = request.getParameter("rnd");
String title = request.getParameter("title");
String searchPlaceholder = request.getParameter("searchPlaceholder");
boolean canAdd = Boolean.parseBoolean(request.getParameter("canAdd"));
boolean canExport = Boolean.parseBoolean(request.getParameter("canExport"));

// Format headers: "NamaKolom|Alignment|Width, NamaKolom|Alignment|Width"
String headersParam = request.getParameter("headers");
String searchColsConfig = request.getParameter("searchColsConfig");

String[] headers = headersParam != null ? headersParam.split(",") : new String[0];
%>


<div id="viewList<%=rnd%>" class="animate__animated animate__fadeInUp">
	<div class="row gx-3">
		<div class="col-12">
			<div class="card" id="ticketsTable<%=rnd%>">
				<div class="card-header border-bottom border-200 px-0">
					<div class="d-lg-flex justify-content-between">
						<div class="row flex-between-center gy-2 px-x1">
							<div class="col-auto pe-0">
								<h6 class="mb-0"><%=title != null ? title : "Data"%></h6>
							</div>
							<div class="col-auto">
								<div class="input-group input-search-width">
									<input class="form-control form-control-sm shadow-none search"
										id="searchData<%=rnd%>" type="search"
										placeholder="<%=searchPlaceholder != null ? searchPlaceholder : "Cari..."%>"
										aria-label="search" />
									<button
										class="btn btn-sm btn-outline-secondary border-300 hover-border-secondary"
										type="button" onclick="resetAndLoadData<%=rnd%>()">
										<span class="fa fa-search fs--1"></span>
									</button>
								</div>
							</div>
						</div>
						<div class="border-bottom border-200 my-3"></div>
						<div
							class="d-flex align-items-center justify-content-between justify-content-lg-end px-x1">
							<button class="btn btn-falcon-default btn-sm mx-2" type="button"
								onclick="toggleSearchPanel<%=rnd%>()" title="Advanced Search">
								<span class="fas fa-filter" data-fa-transform="shrink-3"></span>
								<span class="d-none d-sm-inline-block ms-1">Filter</span>
							</button>

							<button class="btn btn-falcon-default btn-sm mx-2" type="button"
								onclick="resetAndLoadData<%=rnd%>()">
								<span class="fas fa-sync-alt" data-fa-transform="shrink-3"></span>
								<span class="d-none d-sm-inline-block ms-1">Segarkan</span>
							</button>

							<%
							if (canExport) {
							%>
							<button class="btn btn-falcon-default btn-sm" type="button"
								onclick="downloadExcelData<%=rnd%>()">
								<span class="fas fa-file-excel" data-fa-transform="shrink-3"></span>
								<span class="d-none d-sm-inline-block ms-1">Export</span>
							</button>
							<%
							}
							%>

							<%
							if (canAdd) {
							%>
							<button class="btn btn-primary btn-sm ms-2" type="button"
								onclick="bukaFormData<%=rnd%>()">
								<span class="fas fa-plus" data-fa-transform="shrink-3"></span> <span
									class="d-none d-sm-inline-block ms-1">New</span>
							</button>
							<%
							}
							%>
						</div>
					</div>
				</div>

				<div class="card-body p-0">
					<jsp:include
						page="/WEB-INF/baru/modul/common/_search_component.jsp">
						<jsp:param name="rnd" value="<%=rnd%>" />
					</jsp:include>
					<div class="table-responsive scrollbar">
						<table class="table table-sm mb-0 fs--1 table-view-tickets"
							id="tabelHTML<%=rnd%>">
							<thead class="text-800 bg-light">
                                <tr>
                                    <th class="py-2 fs-0 pe-2 text-center" style="width: 50px;">#</th>
                                    
                                    <% 
                                    // Generate Header Dinamis dengan Fitur Sort
                                    // Format: "Label | Alignment | Width | DB_Column_Name"
                                    for(String h : headers) { 
                                        String[] prop = h.split("\\|");
                                        String colName = prop[0].trim();
                                        String align = (prop.length > 1 && !prop[1].trim().isEmpty()) ? prop[1].trim() : "text-start";
                                        String width = (prop.length > 2 && !prop[2].trim().isEmpty()) ? "width: " + prop[2].trim() + ";" : "";
                                        String sortDbCol = (prop.length > 3 && !prop[3].trim().isEmpty()) ? prop[3].trim() : "";
                                        
                                        if (!sortDbCol.isEmpty()) {
                                            // Jika memiliki kolom database, jadikan header bisa diklik (Sortable)
                                    %>
                                            <th class="sort align-middle <%=align%>" style="<%=width%> cursor: pointer; user-select: none;" onclick="changeSort<%=rnd%>('<%=sortDbCol%>')" title="Klik untuk mengurutkan">
                                                <%=colName%> 
                                                <span id="sortIcon_<%=sortDbCol%>_<%=rnd%>" class="ms-1" style="font-size: 0.85em;">
                                                    <i class="fas fa-sort text-muted opacity-50"></i>
                                                </span>
                                            </th>
                                    <% 
                                        } else {
                                            // Jika tidak ada kolom database (misal: Foto), tidak bisa diklik
                                    %>
                                            <th class="align-middle <%=align%>" style="<%=width%>"><%=colName%></th>
                                    <% 
                                        }
                                    } 
                                    %>

                                    <th class="align-middle text-end pe-4 no-export" style="width: 200px;"><i class="fas fa-ellipsis-h"></i></th>
                                </tr>
                            </thead>
							<tbody class="list" id="tabelData<%=rnd%>">
								<tr>
									<td colspan="<%=headers.length + 2%>" class="text-center py-5"><div
											class="spinner-border text-primary"></div></td>
								</tr>
							</tbody>
						</table>
					</div>
				</div>

				<div class="card-footer bg-light py-3">
					<div
						class="d-flex justify-content-between align-items-center flex-wrap">
						<div class="small text-muted fw-semi-bold mb-2 mb-sm-0"
							id="pagingInfo<%=rnd%>"></div>

						<div class="d-flex align-items-center">
							<button class="btn btn-sm btn-falcon-default me-2" type="button"
								title="Previous" id="btnPrev<%=rnd%>"
								onclick="changePage<%=rnd%>(-1)">
								<span class="fas fa-chevron-left"></span>
							</button>
							<ul class="pagination mb-0" id="paginationUl<%=rnd%>"></ul>
							<button class="btn btn-sm btn-falcon-default ms-2" type="button"
								title="Next" id="btnNext<%=rnd%>"
								onclick="changePage<%=rnd%>(1)">
								<span class="fas fa-chevron-right"></span>
							</button>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>

<div class="modal fade" id="modalPreviewFoto<%=rnd%>" tabindex="-1"
	aria-hidden="true">
	<div class="modal-dialog modal-dialog-centered modal-md">
		<div class="modal-content border-0">
			<div class="modal-header bg-light">
				<h5 class="modal-title fw-bold text-dark" id="previewTitle<%=rnd%>">Preview
					Foto</h5>
				<button class="btn-close" type="button" data-bs-dismiss="modal"
					aria-label="Close"></button>
			</div>
			<div class="modal-body p-4 text-center bg-light">
				<img src="" id="imgPreviewTarget<%=rnd%>"
					class="img-fluid rounded-bottom" style="max-height: 80vh;"
					alt="Preview">
			</div>
		</div>
	</div>
</div>

<script>
    // Fungsi universal untuk membuka preview foto
    const bukaPreviewFoto<%=rnd%> = (url, title) => {
        const modalEl = document.getElementById('modalPreviewFoto<%=rnd%>');
        const imgTarget = document.getElementById('imgPreviewTarget<%=rnd%>');
        const titleTarget = document.getElementById('previewTitle<%=rnd%>');
        
        if(imgTarget) imgTarget.src = url;
        if(titleTarget) titleTarget.innerText = title || 'Preview Foto';
        
        const modalInstance = new bootstrap.Modal(modalEl);
        modalInstance.show();
    };
</script>