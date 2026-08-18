<%@page import="java.net.URLEncoder"%>
<%@page import="ais.action.report.format1.akademik.LaporanRekamanNilai"%>
<%@page import="ais.action.report.format1.akademik.LaporanTranskipAkademik"%>
<%@page import="ais.action.report.CommonReportHelper"%>
<%@page import="ais.action.report.format1.akademik.LaporanKHS"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="org.zkoss.zul.Toolbar"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.io.File"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.action.report.Report"%>
<%@page import="ais.action.servlet.api.LaporanApi"%>
<%@page import="ais.action.report.format1.akademik.LaporanKRS"%>

<%
    // =================================================================================
    // 1. INISIALISASI VARIABEL UTAMA
    // =================================================================================
    List<String> warnings = new ArrayList<String>();
    String pdfUrl = "";
    String fileLaporan = request.getParameter("fileLaporan");
    
    // Tarik Data Mahasiswa
    Mahasiswa mahasiswa = (Mahasiswa) GeneralValueObject.ambilData(Mahasiswa.class, request.getParameter("mahasiswa"), true);

    // =================================================================================
    // 2. PENGECEKAN KEAMANAN (FAST-FAIL)
    // =================================================================================
    if (mahasiswa == null) {
        warnings.add(Common.getBahasaConfig("Identitas mahasiswa tidak ditemukan. Silakan pilih ulang mahasiswa."));
    } else if (fileLaporan == null || fileLaporan.trim().isEmpty()) {
        warnings.add(Common.getBahasaConfig("Jenis laporan tidak terdefinisi pada sistem."));
    } else {
        // =================================================================================
        // 3. LOGIKA BISNIS (HANYA DIEKSEKUSI JIKA MAHASISWA VALID)
        // =================================================================================
        String paramSmt = request.getParameter("smt");
        Integer smt = (paramSmt == null || paramSmt.trim().isEmpty())
                ? mahasiswa.currentSemester()
                : Integer.parseInt(paramSmt.trim());

        Date tgl = WaktuUtil.getDate();
        boolean remedial = false;
        Integer semesterPendek = null;
        boolean hitungUlang = false;
        Integer tahapan = null;
        Map map = null;

        // Pengecekan Warning API dan Generate Parameter Map
        if(fileLaporan.equalsIgnoreCase("Cetak_KRS_Mahasiswa")){
            warnings = LaporanApi.warningsKrs(mahasiswa, smt, semesterPendek, remedial, false);
            if(warnings == null) warnings = new ArrayList<String>(); // Safety check
            if(warnings.isEmpty()) {
                map = LaporanKRS.generateParameter(mahasiswa, smt, hitungUlang, semesterPendek, remedial, tgl, tgl, false, false);
            }
        } else if(fileLaporan.equalsIgnoreCase("Kartu_Hasil_Studi")){
            warnings = LaporanApi.warningsKhs(mahasiswa, smt, semesterPendek, remedial, false, true);
            if(warnings == null) warnings = new ArrayList<String>();
            if(warnings.isEmpty()) {
                map = LaporanKHS.generateParameter(mahasiswa, smt, hitungUlang, semesterPendek, remedial, tgl, tgl);
            }
        } else if(fileLaporan.equalsIgnoreCase("Cetak_KUTS_Mahasiswa")){
            warnings = LaporanApi.warningsUts(mahasiswa, smt);
            if(warnings == null) warnings = new ArrayList<String>();
            if(warnings.isEmpty()) {
                map = CommonReportHelper.parameterCetakUTS(mahasiswa, smt, tahapan, semesterPendek, hitungUlang, semesterPendek, remedial, tgl, tgl, null);
            }
        } else if(fileLaporan.equalsIgnoreCase("Cetak_KUAS_Mahasiswa")){
            warnings = LaporanApi.warningsUas(mahasiswa, smt);
            if(warnings == null) warnings = new ArrayList<String>();
            if(warnings.isEmpty()) {
                map = CommonReportHelper.parameterCetakUAS(mahasiswa, smt, tahapan, semesterPendek, hitungUlang, remedial, tgl, tgl, null);
            }
        } else if(fileLaporan.equalsIgnoreCase("Transkrip_Akademik")){
            warnings = LaporanApi.warningsKhs(mahasiswa, smt, semesterPendek, remedial, false, false);
            if(warnings == null) warnings = new ArrayList<String>();
            if(warnings.isEmpty()) {
                map = LaporanTranskipAkademik.generateParameter(mahasiswa, smt, hitungUlang, false, tgl, tgl);
            }
        } else if(fileLaporan.equalsIgnoreCase("Rekaman_Nilai")){
            warnings = LaporanApi.warningsKhs(mahasiswa, smt, semesterPendek, remedial, false, false);
            if(warnings == null) warnings = new ArrayList<String>();
            if(warnings.isEmpty()) {
                map = LaporanRekamanNilai.generateParameter(mahasiswa, smt, false, tgl);
            }
        } else {
            warnings.add(Common.getBahasaConfig("Format laporan tidak didukung oleh sistem."));
        }
        
        // =================================================================================
        // 4. PEMBENTUKAN FILE PDF
        // =================================================================================
        if (warnings.isEmpty() && map != null) {
            File file = Report.generateFileReport(Report.PDF, map, fileLaporan, tgl, new Toolbar());
            pdfUrl = file == null ? "" : Common.CURRENT_URL + "/report/" + URLEncoder.encode(file.getName(),"UTF-8");
        }
    }
%>

<!-- =================================================================================
     5. RENDER OUTPUT HTML / IFRAME PDF KE BROWSER
     ================================================================================= -->
<% if (!warnings.isEmpty()) { %>
    <!-- Tampilan Peringatan Akademik -->
    <div class="d-flex justify-content-center align-items-center h-100 p-4 animate__animated animate__fadeIn">
        <div class="alert alert-warning shadow-sm border-0 d-inline-flex align-items-center p-4" style="border-radius: 16px; background: rgba(255, 251, 235, 0.95); border-left: 5px solid #f59e0b !important;">
            <div class="flex-shrink-0 me-4">
                <i class="fas fa-exclamation-triangle fa-3x text-warning"></i>
            </div>
            <div class="flex-grow-1 text-start">
                <h5 class="fw-bold text-dark mb-1"><%=Common.getBahasaConfig("Informasi Sistem")%></h5>
                <p class="mb-0 text-muted fs-6"><%= String.join("<br>", warnings) %></p>
            </div>
        </div>
    </div>
<% } else if (pdfUrl.isEmpty()) { %>
    <!-- Tampilan Gagal Generate PDF (Error Internal Report) -->
    <div class="d-flex justify-content-center align-items-center h-100 p-4 animate__animated animate__fadeIn">
        <div class="alert alert-danger shadow-sm border-0 d-inline-flex align-items-center p-4" style="border-radius: 16px; border-left: 5px solid #dc3545 !important;">
            <div class="flex-shrink-0 me-4">
                <i class="fas fa-times-circle fa-3x text-danger"></i>
            </div>
            <div class="flex-grow-1 text-start">
                <h5 class="fw-bold text-dark mb-1"><%=Common.getBahasaConfig("Gagal Memproses Dokumen")%></h5>
                <p class="mb-0 text-muted fs-6"><%=Common.getBahasaConfig("Terjadi kesalahan internal saat membuat file PDF. Silakan hubungi Administrator.")%></p>
            </div>
        </div>
    </div>
<% } else { %>
    
    <!-- Render Iframe PDF (Komponen Bawaan Sistem) -->
    <jsp:include page="/WEB-INF/baru/componen/pdf2.jsp">
        <jsp:param name="pdf_url" value="<%=pdfUrl%>" />
    </jsp:include>

<% } %>