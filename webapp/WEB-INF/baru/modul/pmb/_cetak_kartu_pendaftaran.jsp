<%-- BUILD 2026-06-29: dipaksa rekompilasi untuk cegah IncompatibleClassChangeError dari method PembayaranUtil yang berubah static->instance pada JSP ter-compile lama. WAJIB bersihkan work dir Tomcat saat deploy lalu restart. --%>
<%@page import="ais.action.report.Report"%>
<%@page import="ais.action.master.helper.virtualaccount.DownloadNoRegistrasiCalonMahasiswaBankNtt"%>
<%@page import="ais.database.model.VirtualAccountBank"%>
<%@page import="ais.action.master.helper.virtualaccount.DownloadNoRegistrasiCalonMahasiswaBankOnline"%>
<%@page import="ais.database.model.BankHost"%>
<%@page import="ais.action.ws.util.PembayaranUtil"%>
<%@page import="ais.common.EnglishNumberToWords"%>
<%@page import="ais.common.IndonesianNumberToWords"%>
<%@page import="ais.common.BarcodeCommon"%>
<%@page import="ais.common.BniCommon"%>
<%@page import="ais.database.model.bni.BniRequest"%>
<%@page import="ais.action.report.CommonReportHelper"%>
<%@page import="ais.action.master.pmb.TampilanPaymentGateway"%>
<%@page import="ais.common.CommonPMB"%>
<%@page import="java.io.File"%>
<%@page import="java.io.FileOutputStream"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="java.io.OutputStream"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.apache.pdfbox.util.PDFMergerUtility"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.DetailBiaya"%>
<%@page import="ais.database.model.JadwalPembayaran"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.CommonEmail"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // =========================================================================
    // SERVICE: CETAK KARTU PENDAFTARAN & INFO PEMBAYARAN (REUSABLE JSP)
    // Output: Mengembalikan URL path PDF agar bisa di-download via AJAX/JavaScript
    // =========================================================================
    
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");

    String idCama = request.getParameter("id");
    String paramKirimEmail = request.getParameter("kirimEmail");
    boolean kirimEmail = (paramKirimEmail != null && paramKirimEmail.equalsIgnoreCase("true"));

    if (idCama == null || idCama.trim().isEmpty()) {
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("ID Calon Mahasiswa tidak valid.") + "\"}");
        return;
    }

    Session hibSession = null;
    File finalPdfFile = null;

    try {
        hibSession = HibernateUtil.openSession();
        BiodataCalonMahasiswa biodataCalonMahasiswa = idCama == null || idCama.trim().isEmpty() ? null : (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, idCama, true);
        

        if (biodataCalonMahasiswa == null) {
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Data Calon Mahasiswa tidak ditemukan.") + "\"}");
            return;
        }

        // Cek Konfigurasi Global
        if (!Common.getKonfigurasi("setelah_daftar_pmb_langsung_cetak_kartu", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Fitur cetak kartu otomatis sedang tidak diaktifkan oleh sistem.") + "\"}");
            return;
        }

        // Generate No. Registrasi jika belum ada
        if (biodataCalonMahasiswa.getNoRegistrasi() == null || biodataCalonMahasiswa.getNoRegistrasi().trim().isEmpty()) {
            biodataCalonMahasiswa.setNoRegistrasi(CommonPMB.generateNoRegistrasi(biodataCalonMahasiswa));
            Common.refreshUpdate(hibSession, biodataCalonMahasiswa);
        }

        // KONFIGURASI BANK & PAYMENT
        boolean bni = Common.getKonfigurasi("generate_nomor_pembayaran_saat_formulir_mahasiswa_baru", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF);
        boolean online = Common.getKonfigurasi("aktifkan_pembayaran_via_bank_online", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF);
        boolean aktifkan_pembayaran_via_bank_ntt = Common.getKonfigurasi("aktifkan_pembayaran_via_bank_ntt", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF);

        String info = null;
        

        // PERSIAPAN PARAMETER REPORT
        @SuppressWarnings("rawtypes")
        Map parameters = CommonReportHelper.genSklMap(biodataCalonMahasiswa);

        Common.insertProperty(BiodataCalonMahasiswa.class, biodataCalonMahasiswa, parameters, "calon");
        parameters.put("biodata_id", biodataCalonMahasiswa.getId());
        parameters.put("tahun_akademik", Common.getTahunAkademik(1, WaktuUtil.getCalendar().get(Calendar.YEAR), biodataCalonMahasiswa.getSemesterMulai()) + "");
        parameters.put("tagihan", "Rp. " + Common.numberFormat.get().format(CommonPMB.getTotalTagihan(biodataCalonMahasiswa, ConstantValues.PENDAFTARAN_CALON_MAHASISWA)));
        parameters.put("pilihan", CommonPMB.getProdiPilihan(biodataCalonMahasiswa));

        // GENERATE BARCODE
        File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + biodataCalonMahasiswa.getNoRegistrasi() + ".png");
        BarcodeCommon.generateCRCode(biodataCalonMahasiswa.getNoRegistrasi() + "\n" + biodataCalonMahasiswa.getNama() + "\n" + (biodataCalonMahasiswa.getGelombangPendaftaran() != null ? biodataCalonMahasiswa.getGelombangPendaftaran().getNama() : ""), myfilebarcode);
        parameters.put("cr_code", myfilebarcode.getAbsolutePath());

        // LOGIKA PEMBAYARAN & VIRTUAL ACCOUNT (Sesuai source asli)
        if (Common.getKonfigurasi("generate_va_langsung_saat_daftar", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
            if (biodataCalonMahasiswa.getPembayaranRegistrasi() == null || !biodataCalonMahasiswa.getPembayaranRegistrasi().getLunas()) {
                JenisKegiatan jenisKegiatan = ConstantValues.PENDAFTARAN_CALON_MAHASISWA;

                if (bni) {
                    BniRequest bniRequest = BniCommon.bayarCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, false);
                    if (bniRequest != null) {
                        Double amn = bniRequest.getAmount();
                        Double biayaAdministrasi = 0.0;
                        try { biayaAdministrasi = Double.parseDouble(Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_pendaftaran.jsp:124");}

                        info = "Kode Pembayaran\t\t: " + bniRequest.getVa() + "\n";
                        info += "Kode invoice\t\t\t: " + bniRequest.getBillNo() + "\n";
                        info += "Tagihan \t\t\t: " + Common.numberFormat.get().format(amn) + "\n";
                        if (biayaAdministrasi > 0.1) {
                            info += "Biaya admin \t\t\t: " + Common.numberFormat.get().format(biayaAdministrasi) + "\n";
                            info += "Total tagihan\t\t\t: " + Common.numberFormat.get().format(amn + biayaAdministrasi) + "\n";
                        }

                        String terbilang = IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi));
                        String terbilangEn = EnglishNumberToWords.convert((long) (amn + biayaAdministrasi));

                        info += "Terbilang \t\t\t: " + terbilang + "\n";
                        if (bniRequest.getMahasiswa() != null) {
                            info += "NIM \t\t\t\t: " + bniRequest.getMahasiswa().getNim() + "\n";
                            info += "Nama \t\t\t\t: " + bniRequest.getMahasiswa().getNama() + "\n";
                        } else if (bniRequest.getBiodataCalonMahasiswa() != null) {
                            info += "No. Reg \t\t\t: " + bniRequest.getBiodataCalonMahasiswa().getNoRegistrasi() + "\n";
                            if (bniRequest.getBiodataCalonMahasiswa().getNoUjian() != null) {
                                info += "No. Ujian \t\t\t: " + bniRequest.getBiodataCalonMahasiswa().getNoUjian() + "\n";
                            }
                            info += "Nama\t\t\t\t: " + bniRequest.getBiodataCalonMahasiswa().getNama() + "\n";
                        }

                        if (bniRequest.getBillExpired() != null) {
                            info += "Waktu Kadaluarsa \t\t: " + Common.dateFormat5.get().format(bniRequest.getBillExpired()) + "\n";
                        }

                        parameters.put("kode_va", bniRequest.getVa());
                        parameters.put("biaya_va", amn);
                        parameters.put("admin_va", biayaAdministrasi);
                        parameters.put("terbilang_va", terbilang);
                        parameters.put("total_va", amn + biayaAdministrasi);
                        parameters.put("kadaluarsa", bniRequest.getBillExpired());
                        parameters.put("terbilang_en_va", terbilangEn);
                        parameters.put("kode_pembayaran", bniRequest.getVa());
                        parameters.put("kode_invoice", bniRequest.getBillNo());
                        parameters.put("tagihan", Common.numberFormat.get().format(amn));
                        parameters.put("tagihan_format", Common.numberFormat.get().format(amn));
                        parameters.put("biaya_administrasi_format", Common.numberFormat.get().format(biayaAdministrasi));
                        parameters.put("biaya_administrasi", biayaAdministrasi);
                        parameters.put("total_tagihan", amn + biayaAdministrasi);
                        parameters.put("total_tagihan_format", Common.numberFormat.get().format(amn + biayaAdministrasi));
                    }
                } else if (online) {
                    Jurusan prodiLulus = biodataCalonMahasiswa.getProdiLulus();
                    final List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
                    if (prodiLulus == null || prodiLulus.getId() == null) {
                        Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2() : biodataCalonMahasiswa.getProdi1();
                        java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance().getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1, true);
                        detailBiayas.addAll(detailBiayas1);
                    } else {
                        java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance().getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, prodiLulus, true);
                        detailBiayas.addAll(detailBiayas1);
                    }

                    Double nilaiBiayaHarusDiBayars = 0.0;
                    for (DetailBiaya detailBiaya : detailBiayas) {
                        nilaiBiayaHarusDiBayars += (detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya() : detailBiaya.getNilaiBiayaBaru());
                    }

                    if (nilaiBiayaHarusDiBayars > 0.1) {
                        java.io.Serializable[] serializables = PembayaranUtil.getInstance().getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
                                biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan, biodataCalonMahasiswa.getJenjang(),
                                biodataCalonMahasiswa.getTahunAkademik(),
                                (biodataCalonMahasiswa.getGelombangPendaftaran() != null && biodataCalonMahasiswa.getGelombangPendaftaran().getJenisSemester() != null) ? biodataCalonMahasiswa.getGelombangPendaftaran().getJenisSemester().equalsIgnoreCase(Perkuliahan.GANJIL) : true,
                                biodataCalonMahasiswa.getJenisSeleksi(), biodataCalonMahasiswa.getProgram(),
                                biodataCalonMahasiswa.getNoRegistrasi(), biodataCalonMahasiswa.getGelombangPendaftaran());
                        
                        JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];

                        if (jadwalPembayaran != null) {
                            Double biayaAdmin = 0.0;
                            try { biayaAdmin = Double.parseDouble(Common.getKonfigurasi("online_biaya_administrasi", "0.0").getNilai()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_pendaftaran.jsp:198");}

                            BankHost bankHost = PembayaranUtil.getInstance().getBankHost(Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");
                            Map param = new HashMap();
                            VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankOnline.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas, param, biayaAdmin, bankHost);

                            if (virtualAccountBank != null) {
                                Double amn = virtualAccountBank.getTotal();
                                Double biayaAdministrasi = virtualAccountBank.getBiayaAdmin();

                                info = "Kode Pembayaran\t\t: " + virtualAccountBank.getKode() + "\n";
                                info += "Tagihan \t\t\t: " + Common.numberFormat.get().format(amn) + "\n";
                                if (biayaAdministrasi > 0.1) {
                                    info += "Biaya admin \t\t\t: " + Common.numberFormat.get().format(biayaAdministrasi) + "\n";
                                    info += "Total tagihan \t\t\t: " + Common.numberFormat.get().format(amn + biayaAdministrasi) + "\n";
                                }

                                String terbilang = IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi));
                                String terbilangEn = EnglishNumberToWords.convert((long) (amn + biayaAdministrasi));

                                info += "Terbilang \t\t\t: " + terbilang + "\n";
                                if (virtualAccountBank.getMahasiswa() != null) {
                                    info += "NIM \t\t\t\t: " + virtualAccountBank.getMahasiswa().getNim() + "\n";
                                    info += "Nama \t\t\t\t: " + virtualAccountBank.getMahasiswa().getNama() + "\n";
                                } else if (virtualAccountBank.getBiodataCalonMahasiswa() != null) {
                                    info += "No. Reg \t\t\t: " + virtualAccountBank.getBiodataCalonMahasiswa().getNoRegistrasi() + "\n";
                                    if (virtualAccountBank.getBiodataCalonMahasiswa().getNoUjian() != null) {
                                        info += "No. Ujian \t\t\t: " + virtualAccountBank.getBiodataCalonMahasiswa().getNoUjian() + "\n";
                                    }
                                    info += "Nama \t\t\t\t: " + virtualAccountBank.getBiodataCalonMahasiswa().getNama() + "\n";
                                }

                                if (virtualAccountBank.getKadaluarsaWaktu() != null) {
                                    info += "Tgl Kadaluarsa \t\t\t: " + Common.dateFormat51.get().format(virtualAccountBank.getKadaluarsaWaktu()) + "\n";
                                }

                                parameters.put("kode_va", virtualAccountBank.getKode());
                                parameters.put("biaya_va", virtualAccountBank.getTotal());
                                parameters.put("admin_va", biayaAdministrasi);
                                parameters.put("terbilang_va", terbilang);
                                parameters.put("total_va", virtualAccountBank.getTotal() + biayaAdministrasi);
                                parameters.put("terbilang_en_va", terbilangEn);
                                parameters.put("kode_pembayaran", virtualAccountBank.getKode());
                                parameters.put("kode_invoice", virtualAccountBank.getKode());
                                parameters.put("tagihan", Common.numberFormat.get().format(amn));
                                parameters.put("tagihan_format", Common.numberFormat.get().format(amn));
                                parameters.put("biaya_administrasi_format", Common.numberFormat.get().format(biayaAdministrasi));
                                parameters.put("biaya_administrasi", biayaAdministrasi);
                                parameters.put("total_tagihan", amn + biayaAdministrasi);
                                parameters.put("total_tagihan_format", Common.numberFormat.get().format(amn + biayaAdministrasi));
                            }
                        }
                    }
                } else if (aktifkan_pembayaran_via_bank_ntt) {
                    Kegiatan kegiatan = biodataCalonMahasiswa.ambilKegiatans(null, ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
                    if (kegiatan == null || kegiatan.getId() == null) {
                        java.io.Serializable[] serializables = PembayaranUtil.getInstance().getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
                                biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan, biodataCalonMahasiswa.getJenjang(),
                                biodataCalonMahasiswa.getTahunAkademik(),
                                (biodataCalonMahasiswa.getGelombangPendaftaran() != null && biodataCalonMahasiswa.getGelombangPendaftaran().getJenisSemester() != null) ? biodataCalonMahasiswa.getGelombangPendaftaran().getJenisSemester().equalsIgnoreCase(Perkuliahan.GANJIL) : true,
                                biodataCalonMahasiswa.getJenisSeleksi(), biodataCalonMahasiswa.getProgram(),
                                biodataCalonMahasiswa.getNoRegistrasi(), biodataCalonMahasiswa.getGelombangPendaftaran());
                        
                        JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
                        if (jadwalPembayaran != null) {
                            Jurusan prodiLulus = biodataCalonMahasiswa.getProdiLulus();
                            ArrayList<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
                            if (prodiLulus == null || prodiLulus.getId() == null) {
                                Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2() : biodataCalonMahasiswa.getProdi1();
                                java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance().getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1, false);
                                detailBiayas.addAll(detailBiayas1);
                            } else {
                                java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance().getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, prodiLulus, false);
                                detailBiayas.addAll(detailBiayas1);
                            }
                            
                            if (!detailBiayas.isEmpty()) {
                                VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankNtt.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas);
                                if (virtualAccountBank != null) {
                                    String code = virtualAccountBank.getKode();
                                    File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + virtualAccountBank.getId() + ".png");
                                    BarcodeCommon.generateCRCode(code, myfilebarcode1);

                                    Double biayaAdministrasi = 0.0;

                                    info = "Pembayaran dapat dilakukan di Bank NTT dengan informasi sbb :\nKode Pembayaran\t\t: " + virtualAccountBank.getKode() + "\n";
                                    info += "Tagihan \t\t\t: " + Common.numberFormat.get().format(virtualAccountBank.getTotal()) + "\n";
                                    if (biayaAdministrasi > 0.1) {
                                        info += "Biaya admin \t\t\t: " + Common.numberFormat.get().format(biayaAdministrasi) + "\n";
                                        info += "Total tagihan \t\t: " + Common.numberFormat.get().format(virtualAccountBank.getTotal() + biayaAdministrasi) + "\n";
                                    }

                                    String terbilang = IndonesianNumberToWords.convert((long) (virtualAccountBank.getTotal() + biayaAdministrasi));
                                    String terbilangEn = EnglishNumberToWords.convert((long) (virtualAccountBank.getTotal() + biayaAdministrasi));

                                    info += "Terbilang \t\t\t: " + terbilang + "\n";
                                    if (virtualAccountBank.getMahasiswa() != null) {
                                        info += "NIM \t\t\t\t: " + virtualAccountBank.getMahasiswa().getNim() + "\n";
                                        info += "Nama \t\t\t\t: " + virtualAccountBank.getMahasiswa().getNama() + "\n";
                                    } else if (virtualAccountBank.getBiodataCalonMahasiswa() != null) {
                                        info += "No. Reg \t\t\t: " + virtualAccountBank.getBiodataCalonMahasiswa().getNoRegistrasi() + "\n";
                                        if (virtualAccountBank.getBiodataCalonMahasiswa().getNoUjian() != null) {
                                            info += "No. Ujian \t\t\t: " + virtualAccountBank.getBiodataCalonMahasiswa().getNoUjian() + "\n";
                                        }
                                        info += "Nama \t\t\t: " + virtualAccountBank.getBiodataCalonMahasiswa().getNama() + "\n";
                                    }

                                    parameters.put("barcode_file", myfilebarcode1.getAbsolutePath());
                                    parameters.put("kode_va", virtualAccountBank.getKode());
                                    parameters.put("biaya_va", virtualAccountBank.getTotal());
                                    parameters.put("admin_va", biayaAdministrasi);
                                    parameters.put("terbilang_va", terbilang);
                                    parameters.put("total_va", virtualAccountBank.getTotal() + biayaAdministrasi);
                                    parameters.put("terbilang_en_va", terbilangEn);
                                    parameters.put("kode_pembayaran", virtualAccountBank.getKode());
                                    parameters.put("kode_invoice", virtualAccountBank.getKode());
                                    parameters.put("tagihan", Common.numberFormat.get().format(virtualAccountBank.getTotal()));
                                    parameters.put("tagihan_format", Common.numberFormat.get().format(virtualAccountBank.getTotal()));
                                    parameters.put("biaya_administrasi_format", Common.numberFormat.get().format(biayaAdministrasi));
                                    parameters.put("biaya_administrasi", biayaAdministrasi);
                                    parameters.put("total_tagihan", virtualAccountBank.getTotal() + biayaAdministrasi);
                                    parameters.put("total_tagihan_format", Common.numberFormat.get().format(virtualAccountBank.getTotal() + biayaAdministrasi));
                                }
                            }
                        }
                    }
                }

                if (info != null && !info.trim().isEmpty()) {
                    info = "INFORMASI VIRTUAL ACCOUNT PEMBAYARAN " + (ConstantValues.PENDAFTARAN_CALON_MAHASISWA == null ? "" : ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getNamaKegiatan().toUpperCase()) + "\n\n" + info;
                }
            }
        }

        parameters.put("info_data", info);
        Common.insertProperty(BiodataCalonMahasiswa.class, biodataCalonMahasiswa, parameters, "pmb");

        // MERGE PDF DOCUMENTS
        PDFMergerUtility ut = new PDFMergerUtility();
        
        File fileKartuBayar = Report.generateDownloadReport(Report.PDF, parameters, "KartuBayarSpmbMandiri", null, WaktuUtil.getDate(), Common.locale, false);
        ut.addSource(fileKartuBayar);

        if (info != null && !info.trim().isEmpty()) {
            File fileinfo = Report.generateDownloadReport(Report.PDF, parameters, "info", null, WaktuUtil.getDate(), Common.locale, false);
            ut.addSource(fileinfo);
        }

        if (biodataCalonMahasiswa.getNoUjian() != null) {
            File bio = CommonReportHelper.onCetakBiodataCalonMahasiswa(biodataCalonMahasiswa, false);
            ut.addSource(bio);
        }
        
        finalPdfFile = new File(Common.ambilREAL_PATH_REPORT() + "/kartu_reg_" + biodataCalonMahasiswa.getNoRegistrasi() + "_" + Common.getGeneratedBarCode() + ".pdf");
        FileOutputStream fos = new FileOutputStream(finalPdfFile);
        ut.setDestinationStream(fos);
        ut.mergeDocuments();
        fos.close();

        // JIKA MINTA DIKIRIM KE EMAIL (ASYNCHRONOUS THREAD)
        if (kirimEmail) {
            final BiodataCalonMahasiswa finalBio = biodataCalonMahasiswa;
            final File finalPdf = finalPdfFile;
            
            new Thread(new Runnable() {
                public void run() {
                    try {
                        CommonEmail.infoDaftarMahasiswaBanyakFile(finalBio, new File[] { finalPdf });
                    } catch (Exception e) {
                        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_pendaftaran.jsp:367");
                    }
                }
            }).start();
        }

        // KEMBALIKAN PATH URL PDF AGAR BISA DIDOWNLOAD VIA AJAX ATAU DI BUKA LANGSUNG DENGAN ENKRIPSI AMAN
        String URLPDF = Common.ROOT + "/pdf?p=" + URLEncoder.encode(Common.desEncrypter.get().encrypt(finalPdfFile.getName()), "UTF-8");
        out.print("{\"status\":\"success\", \"url\":\"" + URLPDF + "\"}");

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_pendaftaran.jsp:378");
        out.print("{\"status\":\"error\", \"message\":\"Terjadi kesalahan server saat mencetak kartu: " + e.getMessage() + "\"}");
    } finally {
        if (hibSession != null) {
            try { hibSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_pendaftaran.jsp:382");}
            try { hibSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_pendaftaran.jsp:383");}
            try { hibSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_pendaftaran.jsp:384");}
        }
    }
%>
