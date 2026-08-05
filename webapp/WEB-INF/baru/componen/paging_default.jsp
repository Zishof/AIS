<%@page import="ais.common.Common"%>
<%
int size = request.getParameter("size") == null ? 10 : Integer.parseInt(request.getParameter("size").trim());
%>
<div class="card-footer bg-white border-top-0 py-3">
    <div class="row align-items-center <%=(size <= 5) ? "d-none" : "" %>">
        <div class="col">
            <p class="mb-0 small text-muted">
                <span class="d-none d-sm-inline-block" data-list-info="data-list-info"></span>
                <span class="d-none d-sm-inline-block"> &mdash; </span>
                <a class="fw-bold text-decoration-none" href="#!" data-list-view="*">
                    <%=Common.getBahasaConfig("Lihat Semua") %>
                    <span class="fas fa-angle-right ms-1"></span>
                </a>
                <a class="fw-bold text-decoration-none d-none" href="#!" data-list-view="less">
                    <%=Common.getBahasaConfig("Lihat Ringkas") %>
                    <span class="fas fa-angle-right ms-1"></span>
                </a>
            </p>
        </div>

        <div class="col-auto">
            <ul class="pagination d-none mb-0">
                <li class="page-item">
                    <button class="page-link" type="button" data-list-pagination="prev" onclick="event.preventDefault();">
                        <i class="fas fa-chevron-left me-1 small"></i> <%=Common.getBahasaConfig("Sebelumnya") %>
                    </button>
                </li>
                <li class="page-item">
                    <button class="page-link" type="button" data-list-pagination="next" onclick="event.preventDefault();">
                        <%=Common.getBahasaConfig("Setelahnya") %> <i class="fas fa-chevron-right ms-1 small"></i>
                    </button>
                </li>
            </ul>
            
            <div class="pagination d-none"></div>
        </div>
    </div>
</div>