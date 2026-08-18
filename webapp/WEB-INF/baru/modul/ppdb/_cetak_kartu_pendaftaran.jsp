<%@page import="ais.database.model.bri.BriRequest"%>
<%@page import="ais.action.report.Report"%>
<%@page import="ais.common.EnglishNumberToWords"%>
<%@page import="ais.common.IndonesianNumberToWords"%>
<%@page import="ais.common.BarcodeCommon"%>
<%@page import="ais.common.BniCommon"%>
<%@page import="ais.common.BriCommon"%>
<%@page import="ais.database.model.bni.BniRequest"%>
<%@page import="ais.action.report.CommonReportHelper"%>
<%@page import="ais.common.CommonPSB"%>
<%@page import="java.io.File"%>
<%@page import="java.io.FileOutputStream"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.apache.pdfbox.util.PDFMergerUtility"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.sekolah.CalonSiswa"%>
<%@page import="ais.database.model.sekolah.GelombangPendaftaranPsb"%>
<%@page import="ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB"%>
<%@page import="ais.database.model.sekolah.JenisBiayaSekolah"%>
<%@page import="ais.database.model.sekolah.Tagihan"%>
<%@page import="ais.action.master.sekolah.helper.TagihanUtilCalonSiswa"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.common.CommonEmail"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // =========================================================================
    // SERVICE PPDB: CETAK KARTU PENDAFTARAN & INFO PEMBAYARAN SISWA BARU
    // Output: JSON berisi URL path PDF
    // =========================================================================
    
    response.setContentType("application/json");

    String idSiswa = request.getParameter("id");
    String paramKirimEmail = request.getParameter("kirimEmail");
    boolean kirimEmail = (paramKirimEmail != null && paramKirimEmail.equalsIgnoreCase("true"));

    if (idSiswa == null || idSiswa.trim().isEmpty()) {
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("ID Calon Siswa tidak valid.") + "\"}");
        return;
    }

    Session hibSession = null;
    File finalPdfFile = null;

    try {
        hibSession = HibernateUtil.openSession();
        CalonSiswa calonSiswa = (CalonSiswa) GeneralValueObject.ambilData(CalonSiswa.class, idSiswa, true);
        
        if (calonSiswa == null) {
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Data Calon Siswa tidak ditemukan.") + "\"}");
            return;
        }

        // Cek Konfigurasi Global
        if (!Common.getKonfigurasi("setelah_daftar_psb_langsung_cetak_kartu", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Fitur cetak kartu otomatis sedang tidak diaktifkan oleh sistem.") + "\"}");
            return;
        }

        

        // PERSIAPAN PARAMETER REPORT
        @SuppressWarnings("rawtypes")
        Map parameters = ais.common.HashMapGenerator.getRand();
        GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();
        
        LampiranLain kop = LampiranLain.ambil(gel.getId(), LampiranLain.KOP_GELOMBANG_PSB);
        if (kop != null) {
            try {
                parameters.put("kop_file", kop.ambilFile().getAbsolutePath());
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_pendaftaran.jsp:81");}
        }

        parameters.put("biodata_id", calonSiswa.getId());
        parameters.put("tahun_akademik", calonSiswa.getGelombangPendaftaranPsb().getTahunAjaran());

        Common.insertProperty(CalonSiswa.class, calonSiswa, parameters, "calon");

        // GENERATE BARCODE
        File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + calonSiswa.getNoRegistrasi() + ".png");
        String bText = calonSiswa.getNoRegistrasi() + "\n" + calonSiswa.getNamaSiswa() + "\n"
                     + (calonSiswa.getSekolah() != null ? calonSiswa.getSekolah().getNama() : "") + "\n" 
                     + calonSiswa.getGelombangPendaftaranPsb().getNama();
        BarcodeCommon.generateCRCode(bText, myfilebarcode);
        parameters.put("cr_code", myfilebarcode.getAbsolutePath());

        // LOGIKA PEMBAYARAN & VIRTUAL ACCOUNT
        String info = "";
        JenisBiayaSekolah jenisBiayaSekolah = calonSiswa.getTerverifikasi()
                ? calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahTerverifikasi()
                : !calonSiswa.getTelahDiterima() ? calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolah()
                        : calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahLulus();

        if (jenisBiayaSekolah != null) {
            
            // CEK BRI
            boolean bri = Common.getKonfigurasi("generate_nomor_pembayaran_bri_saat_formulir_siswa_baru", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF);
            if (bri) {
                List<Tagihan> tagihans = TagihanUtilCalonSiswa.getTagihan(jenisBiayaSekolah, null, calonSiswa, null, null, true);
                if (!tagihans.isEmpty()) {
                    Double amn = 0.0;
                    for (Tagihan tagihan : tagihans) {
                        amn += (tagihan.getNominal() + tagihan.getDenda());
                    }

                    BriRequest briRequest = BriCommon.onSaveBri(null, calonSiswa, tagihans, amn, false, 0.0);
                    if (briRequest != null) {
                        Double biayaAdministrasi = 0.0;
                        try { biayaAdministrasi = Double.parseDouble(Common.getKonfigurasi("bri_biaya_administrasi", "0.0").getNilai()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_pendaftaran.jsp:119");}
                        
                        String brivaNo = Common.getKonfigurasi("bri_briva_no", "77777").getNilai();

                        info = "Kode Pembayaran\t\t: " + brivaNo + briRequest.getVa() + "\n";
                        info += "Tagihan \t\t\t: " + Common.numberFormat.get().format(amn) + "\n";
                        if (biayaAdministrasi > 0.1) {
                            info += "Biaya admin \t\t\t: " + Common.numberFormat.get().format(biayaAdministrasi) + "\n";
                            info += "Total tagihan \t\t: " + Common.numberFormat.get().format(amn + biayaAdministrasi) + "\n";
                        }
                        info += "Terbilang \t\t\t: " + IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi)) + "\n";
                        
                        if (briRequest.getSiswa() != null) {
                            info += "NIS  \t\t\t\t: " + briRequest.getSiswa().getNomorInduk() + "\n";
                            info += "Nama \t\t\t\t: " + briRequest.getSiswa().getNama() + "\n";
                        } else if (briRequest.getCalonSiswa() != null) {
                            info += "No. Reg \t\t\t: " + briRequest.getCalonSiswa().getNoRegistrasi() + "\n";
                            if (briRequest.getCalonSiswa().getNoUjian() != null) {
                                info += "No. Ujian \t\t\t: " + briRequest.getCalonSiswa().getNoUjian() + "\n";
                            }
                            info += "Nama \t\t\t\t: " + briRequest.getCalonSiswa().getNamaSiswa() + "\n";
                        }

                        if (briRequest.getBill_expired() != null) {
                            info += "Wkt. Kadaluarsa\t\t: " + Common.dateFormat3.get().format(briRequest.getBill_expired()) + "\n";
                        }

                        info += "\nTata Cara Pembayaran bisa dilihat di menu pengumuman cara pembayaran di Sistem Penerimaan Peserta Didik Baru (PPDB)\n";
                        parameters.put("info_bayar", info);
                    }
                }
            }

            // CEK BNI
            boolean bni = Common.getKonfigurasi("generate_nomor_pembayaran_bni_saat_formulir_siswa_baru", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF);
            if (bni) {
                List<Tagihan> tagihans = TagihanUtilCalonSiswa.getTagihan(jenisBiayaSekolah, null, calonSiswa, null, null, true);
                if (!tagihans.isEmpty()) {
                    Double amn = 0.0;
                    for (Tagihan tagihan : tagihans) {
                        amn += (tagihan.getNominal() + tagihan.getDenda());
                    }

                    BniRequest bniRequest = BniCommon.onSaveBni(null, calonSiswa, tagihans, amn, false, 0.0);
                    if (bniRequest != null) {
                        Double biayaAdministrasi = 0.0;
                        try { biayaAdministrasi = Double.parseDouble(Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_pendaftaran.jsp:165");}

                        info = "Kode Pembayaran\t\t: " + bniRequest.getVa() + "\n";
                        info += "Tagihan \t\t\t: " + Common.numberFormat.get().format(amn) + "\n";
                        if (biayaAdministrasi > 0.1) {
                            info += "Biaya admin \t\t\t: " + Common.numberFormat.get().format(biayaAdministrasi) + "\n";
                            info += "Total tagihan \t\t: " + Common.numberFormat.get().format(amn + biayaAdministrasi) + "\n";
                        }
                        info += "Terbilang \t\t\t: " + IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi)) + "\n";
                        
                        if (bniRequest.getSiswa() != null) {
                            info += "NIS  \t\t\t\t: " + bniRequest.getSiswa().getNomorInduk() + "\n";
                            info += "Nama \t\t\t\t: " + bniRequest.getSiswa().getNama() + "\n";
                        } else if (bniRequest.getCalonSiswa() != null) {
                            info += "No. Reg \t\t\t: " + bniRequest.getCalonSiswa().getNoRegistrasi() + "\n";
                            if (bniRequest.getCalonSiswa().getNoUjian() != null) {
                                info += "No. Ujian \t\t\t: " + bniRequest.getCalonSiswa().getNoUjian() + "\n";
                            }
                            info += "Nama \t\t\t\t: " + bniRequest.getCalonSiswa().getNamaSiswa() + "\n";
                        }

                        if (bniRequest.getBillExpired() != null) {
                            info += "Wkt. Kadaluarsa\t\t: " + Common.dateFormat3.get().format(bniRequest.getBillExpired()) + "\n";
                        }

                        info += "\nTata Cara Pembayaran bisa dilihat di menu pengumuman cara pembayaran di Sistem Penerimaan Peserta Didik Baru (PPDB)\n";
                        parameters.put("info_bayar", info);
                    }
                }
            }
        }

        // MERGE PDF DOCUMENTS
        PDFMergerUtility ut = new PDFMergerUtility();
        
        // 1. Tambahkan PDF Kartu Bayar Mandiri PPDB
        File fileKartuBayar = Report.generateDownloadReport(Report.PDF, parameters, "sekolah/KartuBayarPsbMandiri", null, WaktuUtil.getDate(), Common.locale, false);
        if (fileKartuBayar != null && fileKartuBayar.exists()) {
            ut.addSource(fileKartuBayar);
        }

        // 2. Jika ada informasi pembayaran (Info VA BNI/BRI), gabung halaman Info
        if (info != null && !info.trim().isEmpty()) {
            parameters.put("info_data", info);
            File fileinfo = Report.generateDownloadReport(Report.PDF, parameters, "info", null, WaktuUtil.getDate(), Common.locale, false);
            if(fileinfo != null && fileinfo.exists()) ut.addSource(fileinfo);
        }

        // 3. Tambahkan Cetakan Biodata Calon Siswa (Sudah disesuaikan ke Biodata_Calon_Siswa)
        try {
            File bio = Report.generateDownloadReport(Report.PDF, parameters, "sekolah/Biodata_Calon_Siswa", null, WaktuUtil.getDate(), Common.locale, false);
            if (bio != null && bio.exists()) {
                ut.addSource(bio);
            }
        } catch (Exception x) {
            // Abaikan jika template Biodata_Calon_Siswa belum dibuat oleh developer atau error generate
            x.printStackTrace(); ais.common.ErrorAuditUtil.record(x, "auto-audit webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_pendaftaran.jsp:221");
        }
        
        // 4. Buat Final File Gabungan
        finalPdfFile = new File(Common.ambilREAL_PATH_REPORT() + "/kartu_reg_" + calonSiswa.getNoRegistrasi() + "_" + Common.getGeneratedBarCode() + ".pdf");
        FileOutputStream fos = new FileOutputStream(finalPdfFile);
        ut.setDestinationStream(fos);
        ut.mergeDocuments();
        fos.close();

        // KEMBALIKAN PATH URL PDF AGAR BISA DIDOWNLOAD VIA AJAX
        String URLPDF = Common.ROOT + "/pdf?p=" + URLEncoder.encode(Common.desEncrypter.get().encrypt(finalPdfFile.getName()), "UTF-8");
        out.print("{\"status\":\"success\", \"url\":\"" + URLPDF + "\"}");

        // THREAD KIRIM EMAIL
        if (kirimEmail) {
            final CalonSiswa finalBio = calonSiswa;
            final File finalPdf = finalPdfFile;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        CommonEmail.infoDaftarSiswaBanyakFile(finalBio, new File[] { finalPdf });
                    } catch (Exception e) {
                        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_pendaftaran.jsp:244");
                    }
                }
            }).start();
        }

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_pendaftaran.jsp:251");
        out.print("{\"status\":\"error\", \"message\":\"Terjadi kesalahan server saat mencetak kartu pendaftaran: " + e.getMessage() + "\"}");
    } finally {
        if (hibSession != null) {
            try {
                hibSession.disconnect();
                hibSession.close();
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_pendaftaran.jsp:258");}
        }
        HibernateUtil.closeSessionQuietly(hibSession);
    }
%>