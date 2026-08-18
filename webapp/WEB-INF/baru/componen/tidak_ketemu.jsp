<%@page import="ais.common.Common"%>
<%
    // VALIDASI INPUT (Mencegah Error Null Pointer)
    String paramPesan = request.getParameter("pesan");
	String paramJudul = request.getParameter("judul");
	String paramJenis = request.getParameter("jenis") == null || request.getParameter("jenis").trim().isEmpty() ? "modal" : request.getParameter("jenis").trim();
    String pesanTampil = (paramPesan == null || paramPesan.trim().isEmpty()) 
                         ? "Mohon maaf, data yang Anda cari tidak tersedia." 
                         : paramPesan;
    String pesanHeader = (paramJudul == null || paramJudul.trim().isEmpty()) 
            ? "Data Tidak Ditemukan"
            : paramJudul;
%>

<div class="alert alert-warning bg-warning-subtle text-warning-emphasis border-0 shadow-sm rounded-4 d-flex align-items-center p-4 mb-0 fade show" role="alert">
    
    <div class="flex-shrink-0 bg-warning bg-gradient bg-opacity-25 rounded-circle p-3 d-flex align-items-center justify-content-center" style="width: 60px; height: 60px;">
        <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" fill="currentColor" class="fas fa-exclamation-triangle" viewBox="0 0 16 16">
            <path d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767L8.982 1.566zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5zm.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2z"/>
        </svg>
    </div>
    
    <div class="ms-3 flex-grow-1">
        <h5 class="alert-heading fw-bold mb-1"><%=Common.getBahasaConfig(pesanHeader) %></h5>
        <p class="mb-0 opacity-75 small"><%=Common.getBahasaConfig(pesanTampil) %></p>
    </div>
    
    <button type="button" class="btn-close ms-2 align-self-start" data-bs-dismiss="<%=paramJenis %>" aria-label="<%=Common.getBahasaConfig("Close") %>"></button>
</div>