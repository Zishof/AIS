<%@page import="ais.action.report.format1.akademik.LaporanKartuMahasiswa"%>
<%@page import="org.apache.pdfbox.util.PDFMergerUtility"%>
<%@page import="ais.action.report.Report"%>
<%@page import="ais.action.report.CommonReportHelper"%>
<%@page import="ais.action.master.pmb.BiodataCalonMahasiswaAction"%>
<%@page import="ais.action.master.pmb.CetakRegistrasiAction"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.io.File"%>
<%@page import="java.io.FileOutputStream"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonEmail"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="org.zkoss.zul.Label"%>
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
        String kirimParam = request.getParameter("kirimEmail");
        boolean kirim = (kirimParam != null && kirimParam.equalsIgnoreCase("true"));

        if (idCama == null || idCama.trim().isEmpty()) {
            result.put("status", "error");
            result.put("message", Common.getBahasaConfig("ID Calon Mahasiswa tidak ditemukan."));
            out.print(result.toString());
            return;
        }

        BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, idCama, true);

        if (calonMahasiswa == null || calonMahasiswa.getProdiLulus() == null) {
            result.put("status", "error");
            result.put("message", Common.getBahasaConfig("Calon mahasiswa ini belum dinyatakan lulus atau diterima!"));
            out.print(result.toString());
            return;
        }

        // Sinkronisasi Pembayaran
        try {
            CetakRegistrasiAction.singkronkanDenganPembayaran(calonMahasiswa.getId(), new Label(), 0, 1);
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_cetak_bukti_diterima.jsp:53");
        }

        // Persiapan Parameter Report menggunakan CommonReportHelper
        Map parameters = CommonReportHelper.genSklMap(calonMahasiswa);
        try {
            parameters.put("file_laporan", URLEncoder.encode(calonMahasiswa.getNoRegistrasi() + " " + calonMahasiswa.getNama() + " Keterangan_Lulus", "UTF-8"));
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_bukti_diterima.jsp:60");}

        File finalFile = null;

        // Logika Penggabungan dengan KTM (Kartu Tanda Mahasiswa)
        if (Common.getKonfigurasi("cetak_ktm_di_surat_keterangan_lulus", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF) && calonMahasiswa.getMahasiswa() != null) {
            
            if (!kirim) {
                if (Common.getKonfigurasi("cetak_ktm_di_surat_keterangan_lulus_harus_mendapatkan_nim", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF) && calonMahasiswa.getMahasiswa() == null) {
                    result.put("status", "error");
                    result.put("message", Common.getBahasaConfig("Calon mahasiswa ini belum mendapatkan NIM!"));
                    out.print(result.toString());
                    return;
                }
            }

            File fileSkl = Report.generateDownloadReport(Report.PDF, parameters, "Keterangan_Lulus", null, ais.ui.util.WaktuUtil.getDate(), Common.locale, false);

            PDFMergerUtility ut = new PDFMergerUtility();
            ut.addSource(fileSkl);

            int masaKartuMahasiswa = 4;
            try {
                masaKartuMahasiswa = Integer.parseInt(Common.getKonfigurasi("masa_berlaku_kartu_mahasiswa", masaKartuMahasiswa + "").getNilai());
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_bukti_diterima.jsp:84");}

            Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
            try {
                if (calonMahasiswa.getTanggalDiterima() != null) calendar.setTime(calonMahasiswa.getTanggalDiterima());
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_bukti_diterima.jsp:89");}
            
            calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + masaKartuMahasiswa);
            Date masa_berlaku_kartu = calendar.getTime();

            List list = new ArrayList();
            list.add(LaporanKartuMahasiswa.siapkanParemeter(calonMahasiswa.getMahasiswa()));

            Map parametersKartu = ais.common.HashMapGenerator.getRand();
            parametersKartu = LaporanKartuMahasiswa.siapkanParemeterGambar(parametersKartu, null);
            parametersKartu.put("tanggal_kartu", calonMahasiswa.getTanggalDiterima() != null ? calonMahasiswa.getTanggalDiterima() : ais.ui.util.WaktuUtil.getDate());
            parametersKartu.put("masa_berlaku_kartu", masa_berlaku_kartu);

            parametersKartu.put("belakang", true);
            parametersKartu.put("depan", true);
            parametersKartu.put("maps", list);

            File fileKtm = Report.generateDownloadReport(Report.PDF, parametersKartu, "format1/kartu_mahasiswa", null, ais.ui.util.WaktuUtil.getDate(), Common.locale, false);
            ut.addSource(fileKtm);

            File filePdfBaru = new File(fileSkl.getParentFile().getAbsolutePath() + "/" + Common.getGeneratedBarCode() + ".pdf");
            ut.setDestinationStream(new FileOutputStream(filePdfBaru));
            ut.mergeDocuments();

            parameters.putAll(parametersKartu);
            finalFile = filePdfBaru;

            if (kirim) {
                try { CommonEmail.infoDaftarMahasiswaDinyatakanDIterima(calonMahasiswa, filePdfBaru); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_bukti_diterima.jsp:117");}
            }
            
        } else {
            // Logika Cetak Tanpa KTM
            finalFile = Report.generateDownloadReport(Report.PDF, parameters, "Keterangan_Lulus", null, ais.ui.util.WaktuUtil.getDate(), Common.locale, false);
            
            if (kirim) {
                try { CommonEmail.infoDaftarMahasiswaDinyatakanDIterima(calonMahasiswa, finalFile); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_bukti_diterima.jsp:125");}
            }
        }

        // Cek jika File Berhasil Digenerate, kembalikan URL terenkripsi ke UI
        if (finalFile != null && finalFile.exists()) {
            result.put("status", "success");
            
            // Menggunakan desEncrypter untuk mengamankan nama file pada parameter URL
            String urlPDF = Common.ROOT + "/pdf?p=" + URLEncoder.encode(Common.desEncrypter.get().encrypt(finalFile.getName()), "UTF-8");
            result.put("url", urlPDF);
        } else {
            result.put("status", "error");
            result.put("message", Common.getBahasaConfig("Gagal men-generate file PDF Keterangan Lulus."));
        }

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_cetak_bukti_diterima.jsp:142");
        result.put("status", "error");
        result.put("message", Common.getBahasaConfig("Terjadi kesalahan sistem: ") + e.getMessage());
    }
    
    // Output JSON ke response
    out.print(result.toString());
%>