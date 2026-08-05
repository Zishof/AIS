<%@page import="ais.database.model.Kegiatan"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.action.report.CommonReportHelper"%>
<%@page import="java.io.File"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // Mengatur header agar frontend langsung mengenalinya sebagai JSON
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");

    try {
        // Mengambil parameter kegiatanId dari request frontend
        String kegiatanIdStr = request.getParameter("kegiatanId");
        
        if (kegiatanIdStr == null || kegiatanIdStr.trim().isEmpty()) {
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("ID Kegiatan/Tagihan tidak dikirimkan oleh sistem.") + "\"}");
            return;
        }

        long kegiatanId = Long.parseLong(kegiatanIdStr.trim());
        
        // Mengambil data Kegiatan secara langsung berdasarkan ID
        Kegiatan kCetak = (Kegiatan) ConstantValues.ambil(Kegiatan.class.getName(), kegiatanId, true);

        if (kCetak != null) {
            File fileStruk = null;
            
            // Pengecekan cerdas: Apakah ini tagihan Calon Mahasiswa atau Mahasiswa Aktif?
            if (kCetak.getCalonMahasiswa() != null) {
                fileStruk = CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kCetak, true);
            } else if (kCetak.getMahasiswa() != null) {
                fileStruk = CommonReportHelper.cetakBuktipembayaranMahasiswa(kCetak, true);
            } else {
                out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Data pemilik tagihan tidak teridentifikasi.") + "\"}");
                return;
            }
            
            // Evaluasi keberhasilan pembuatan File PDF
            if (fileStruk != null && fileStruk.exists()) {
                String pathPdf = Common.ROOT + "/pdf?p=" + URLEncoder.encode(Common.desEncrypter.get().encrypt(fileStruk.getName()), "UTF-8");
                out.print("{\"status\":\"success\", \"url\":\"" + pathPdf + "\"}");
            } else {
                out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sistem gagal membuat dokumen PDF struk pembayaran.") + "\"}");
            }
            
        } else {
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Data kegiatan pembayaran tidak ditemukan di database.") + "\"}");
        }
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_cetak_bukti_pembayaran_mhs.jsp:56");
        // Membersihkan karakter kutip ganda dari pesan error agar JSON tidak pecah (SyntaxError)
        String safeErrorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "'").replace("\n", " ") : "Unknown Error";
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Galat internal peladen: ") + safeErrorMsg + "\"}");
    }
%>