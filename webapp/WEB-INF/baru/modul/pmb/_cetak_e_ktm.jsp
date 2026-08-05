<%@page import="ais.action.report.format1.akademik.LaporanKartuMahasiswa"%>
<%@page import="ais.action.report.Report"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // Set Header Response JSON
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");
    response.setCharacterEncoding("UTF-8");
    JSONObject result = new JSONObject();

    try {
        String idCama = request.getParameter("id");

        if (idCama == null || idCama.trim().isEmpty()) {
            result.put("status", "error");
            result.put("message", Common.getBahasaConfig("ID Calon Mahasiswa tidak valid."));
            out.print(result.toString());
            return;
        }

        BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, idCama, true);

        if (calonMahasiswa == null || calonMahasiswa.getMahasiswa() == null) {
            result.put("status", "error");
            result.put("message", Common.getBahasaConfig("Calon mahasiswa ini belum diproses atau belum memiliki data Nomor Induk Mahasiswa (NIM)!"));
            out.print(result.toString());
            return;
        }

        Mahasiswa mahasiswa = calonMahasiswa.getMahasiswa();

        // 1. Perhitungan Masa Berlaku Kartu Mahasiswa
        int masaKartuMahasiswa = 4;
        try {
            masaKartuMahasiswa = Integer.parseInt(Common.getKonfigurasi("masa_berlaku_kartu_mahasiswa", masaKartuMahasiswa + "").getNilai());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_e_ktm.jsp:48");}

        Calendar calendar = WaktuUtil.getCalendar();
        try {
            if (calonMahasiswa.getTanggalDiterima() != null) {
                calendar.setTime(calonMahasiswa.getTanggalDiterima());
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_e_ktm.jsp:55");}
        
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + masaKartuMahasiswa);
        Date masaBerlakuKartu = calendar.getTime();

        // 2. Penyiapan Parameter List (Data Mahasiswa)
        List list = new ArrayList();
        list.add(LaporanKartuMahasiswa.siapkanParemeter(mahasiswa));

        // 3. Penyiapan Parameter Gambar (Stempel, TTD, Background) & Konfigurasi Laporan
        Map parametersKartu = ais.common.HashMapGenerator.getRand();
        parametersKartu = LaporanKartuMahasiswa.siapkanParemeterGambar(parametersKartu, null);
        
        parametersKartu.put("tanggal_kartu", calonMahasiswa.getTanggalDiterima() != null ? calonMahasiswa.getTanggalDiterima() : WaktuUtil.getDate());
        parametersKartu.put("masa_berlaku_kartu", masaBerlakuKartu);
        
        // Cetak halaman depan dan belakang sesuai request
        parametersKartu.put("belakang", true);
        parametersKartu.put("depan", true);
        parametersKartu.put("maps", list);

        // 4. Proses Pembuatan PDF
        File fileKtm = Report.generateDownloadReport(Report.PDF, parametersKartu, "format1/kartu_mahasiswa", null, WaktuUtil.getDate(), Common.locale, false);

        // 5. Verifikasi dan Pengembalian URL PDF
        if (fileKtm != null && fileKtm.exists()) {
            result.put("status", "success");
            
            // Buat tautan (URL) terenkripsi untuk keamanan dokumen file PDF
            String urlPDF = Common.ROOT + "/pdf?p=" + URLEncoder.encode(Common.desEncrypter.get().encrypt(fileKtm.getName()), "UTF-8");
            result.put("url", urlPDF);
        } else {
            result.put("status", "error");
            result.put("message", Common.getBahasaConfig("Gagal men-generate file PDF Kartu Tanda Mahasiswa (E-KTM)."));
        }

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_cetak_e_ktm.jsp:92");
        result.put("status", "error");
        result.put("message", Common.getBahasaConfig("Terjadi kesalahan sistem: ") + e.getMessage());
    }
    
    // Output JSON ke response
    out.print(result.toString());
%>