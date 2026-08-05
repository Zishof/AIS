<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.file.LampiranLainBiodataCalonMahasiswa"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String rnd = Common.getGeneratedBarCode(7);
    String idCama = request.getParameter("id");
    
    // Menggunakan GeneralValueObject.ambilData sesuai standar framework
    BiodataCalonMahasiswa cama = idCama == null || idCama.trim().isEmpty() ? null : 
        (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, idCama.trim(), true);

    if (cama == null) {
%>
    <div class="text-center py-5">
        <i class="fas fa-circle-exclamation fa-3x text-danger mb-3"></i>
        <h5 class="text-dark fw-bold"><%= Common.getBahasaConfig("Data Tidak Ditemukan") %></h5>
        <p class="text-muted"><%= Common.getBahasaConfig("Gagal memuat data calon mahasiswa. Silakan tutup jendela ini dan muat ulang halaman.") %></p>
    </div>
<%
        return;
    }
%>

<div class="container-fluid py-4 px-3 bg-light">
    <div class="row g-4 justify-content-center">
        
        <div class="col-md-6 col-lg-6">
            <div class="card h-100 border-0 shadow-sm rounded-4 overflow-hidden">
                <div class="card-header bg-primary text-white text-center py-4 border-0">
                    <i class="fas fa-file-invoice-dollar fa-3x mb-2 opacity-75"></i>
                    <h6 class="fw-bold mb-0 text-uppercase" style="letter-spacing: 1px;"><%= Common.getBahasaConfig("Pembayaran Registrasi") %></h6>
                </div>
                <div class="card-body text-center p-4 d-flex flex-column align-items-center justify-content-center bg-white">
                    <p class="small text-muted mb-4"><%= Common.getBahasaConfig("Silakan unggah bukti pembayaran biaya pendaftaran (registrasi) Anda pada kolom di bawah ini.") %></p>
                    
                    <div class="w-100 p-3 border border-secondary border-opacity-25 rounded-3 bg-light">
                        <jsp:include page="/WEB-INF/baru/modul/common/upload_component.jsp">
                            <jsp:param name="clazz" value="<%=LampiranLainBiodataCalonMahasiswa.class.getName()%>"/>
                            <jsp:param name="jenis" value="<%=LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_PENDAFTARAN%>"/>
                            <jsp:param name="id" value="<%=cama.getId().toString()%>"/>
                            <jsp:param name="accept" value="image/*,application/pdf"/>
                            <jsp:param name="tampilUpload" value="true"/>
                            <jsp:param name="loadPreview" value="true"/>
                        </jsp:include>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-md-6 col-lg-6">
            <div class="card h-100 border-0 shadow-sm rounded-4 overflow-hidden">
                <div class="card-header bg-success text-white text-center py-4 border-0">
                    <i class="fas fa-money-check-dollar fa-3x mb-2 opacity-75"></i>
                    <h6 class="fw-bold mb-0 text-uppercase" style="letter-spacing: 1px;"><%= Common.getBahasaConfig("Pembayaran Daftar Ulang") %></h6>
                </div>
                <div class="card-body text-center p-4 d-flex flex-column align-items-center justify-content-center bg-white">
                    <p class="small text-muted mb-4"><%= Common.getBahasaConfig("Silakan unggah bukti pembayaran daftar ulang (herregistrasi) Anda pada kolom di bawah ini.") %></p>
                    
                    <div class="w-100 p-3 border border-secondary border-opacity-25 rounded-3 bg-light">
                        <jsp:include page="/WEB-INF/baru/modul/common/upload_component.jsp">
                            <jsp:param name="clazz" value="<%=LampiranLainBiodataCalonMahasiswa.class.getName()%>"/>
                            <jsp:param name="jenis" value="<%=LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_DAFTAR_ULANG%>"/>
                            <jsp:param name="id" value="<%=cama.getId().toString()%>"/>
                            <jsp:param name="accept" value="image/*,application/pdf"/>
                            <jsp:param name="tampilUpload" value="true"/>
                            <jsp:param name="loadPreview" value="true"/>
                        </jsp:include>
                    </div>
                </div>
            </div>
        </div>

    </div>
</div>