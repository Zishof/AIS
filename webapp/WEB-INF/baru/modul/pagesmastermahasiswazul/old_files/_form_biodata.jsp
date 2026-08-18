<%
    String rnd = request.getParameter("rnd");
%>

<div class="card border-0 shadow-none bg-transparent mt-2">
    <div class="card-header pb-0 px-0 bg-transparent border-bottom">
        <ul class="nav nav-tabs border-bottom-0" id="subTabBiodata<%=rnd%>" role="tablist">
            <li class="nav-item">
                <a class="nav-link active fw-semi-bold" id="sub-diri-tab-<%=rnd%>" data-bs-toggle="tab" href="#subContentDiri-<%=rnd%>" role="tab">
                    <i class="fas fa-user me-2 text-primary"></i>Biodata
                </a>
            </li>
            <li class="nav-item">
                <a class="nav-link fw-semi-bold" id="sub-alamat-tab-<%=rnd%>" data-bs-toggle="tab" href="#subContentAlamat-<%=rnd%>" role="tab">
                    <i class="fas fa-map-marker-alt me-2 text-primary"></i>Alamat
                </a>
            </li>
            <li class="nav-item">
                <a class="nav-link fw-semi-bold" id="sub-keluarga-tab-<%=rnd%>" data-bs-toggle="tab" href="#subContentKeluarga-<%=rnd%>" role="tab">
                    <i class="fas fa-users me-2 text-primary"></i>Keluarga
                </a>
            </li>
            <li class="nav-item">
                <a class="nav-link fw-semi-bold" id="sub-wali-tab-<%=rnd%>" data-bs-toggle="tab" href="#subContentWali-<%=rnd%>" role="tab">
                    <i class="fas fa-user-tie me-2 text-primary"></i>Wali
                </a>
            </li>
        </ul>
    </div>
    <div class="card-body pt-4 px-0">
        <div class="tab-content" id="subTabBiodataContent<%=rnd%>">
            <div class="tab-pane fade show active" id="subContentDiri-<%=rnd%>" role="tabpanel">
                <jsp:include page="_form_biodata_diri.jsp">
                    <jsp:param name="rnd" value="<%=rnd%>" />
                </jsp:include>
            </div>

            <div class="tab-pane fade" id="subContentAlamat-<%=rnd%>" role="tabpanel">
                <jsp:include page="_form_biodata_alamat.jsp">
                    <jsp:param name="rnd" value="<%=rnd%>" />
                </jsp:include>
            </div>

            <div class="tab-pane fade" id="subContentKeluarga-<%=rnd%>" role="tabpanel">
                <jsp:include page="_form_biodata_keluarga.jsp">
                    <jsp:param name="rnd" value="<%=rnd%>" />
                </jsp:include>
            </div>

            <div class="tab-pane fade" id="subContentWali-<%=rnd%>" role="tabpanel">
                <jsp:include page="_form_biodata_wali.jsp">
                    <jsp:param name="rnd" value="<%=rnd%>" />
                </jsp:include>
            </div>
        </div>
    </div>
</div>