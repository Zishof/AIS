<%@page import="ais.action.report.Report"%>
<%@page import="java.util.*"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPSB"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.sekolah.CalonSiswa"%>
<%@page import="ais.database.model.sekolah.GelombangPendaftaranPsb"%>
<%@page import="ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");

    String idParam = request.getParameter("id");
    Tbmuser tbmuser = Common.getCurrentUser(request);

    if (idParam == null || idParam.trim().isEmpty()) {
        out.print("{\"status\":\"error\",\"message\":\"" + Common.getBahasaConfig("ID Calon Siswa tidak valid.") + "\"}");
        return;
    }

    CalonSiswa casis = (CalonSiswa) GeneralValueObject.ambilData(CalonSiswa.class, idParam.trim(), true);
    if (casis == null) {
        out.print("{\"status\":\"error\",\"message\":\"" + Common.getBahasaConfig("Data calon siswa tidak ditemukan di dalam sistem.") + "\"}");
        return;
    }

    Session sessionLocal = null;
    Transaction tx = null;

    try {
        sessionLocal = HibernateUtil.openSession();
        tx = sessionLocal.beginTransaction();

        // Re-attach ke session agar lazy-load relasi berfungsi
        casis = (CalonSiswa) sessionLocal.get(CalonSiswa.class, casis.getId());
        GelombangPendaftaranPsb gelombang = casis == null ? null : casis.getGelombangPendaftaranPsb();
        if (gelombang == null) {
            out.print("{\"status\":\"error\",\"message\":\"" + Common.getBahasaConfig("Gelombang pendaftaran belum terhubung dengan data calon siswa.") + "\"}");
            tx.rollback(); return;
        }

        // Validasi pembayaran jika gelombang mengharuskan bayar dulu
        if (gelombang.getHarusBayarSebelumBisaLogin() != null && gelombang.getHarusBayarSebelumBisaLogin()) {
            Kegiatan kegiatan = casis.getPembayaranRegistrasi();
            if (kegiatan == null || kegiatan.getId() == null || kegiatan.getLunas() == null || !kegiatan.getLunas()) {
                out.print("{\"status\":\"error\",\"message\":\"" + Common.getBahasaConfig("Pembayaran registrasi belum lunas. Kartu ujian belum dapat dicetak.") + "\"}");
                tx.rollback(); return;
            }
        }

        // Ambil atau gunakan nomor ujian yang sudah ada
        String noUjian = casis.getNoUjian();
        if (noUjian == null || noUjian.trim().isEmpty()) {
            // Nomor ujian harus ditetapkan admin melalui antarmuka administrasi
            out.print("{\"status\":\"error\",\"message\":\"" + Common.getBahasaConfig("Nomor ujian Anda belum ditetapkan. Silakan hubungi panitia PPDB untuk mendapatkan nomor ujian.") + "\"}");
            tx.rollback(); return;
        }

        // Validasi alokasi ruang ujian
        RuangGelombangPendaftaranPsbPSB ruangRec =
            (RuangGelombangPendaftaranPsbPSB) sessionLocal
                .createCriteria(RuangGelombangPendaftaranPsbPSB.class)
                .add(Restrictions.eq("calonSiswa", casis))
                .addOrder(Order.desc("id"))
                .setMaxResults(1)
                .uniqueResult();

        if (ruangRec == null) {
            // Coba alokasikan via CommonPSB jika sudah ada nomor ujian
            try {
                ruangRec = CommonPSB.dapatkanRuangUjian(casis);
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_ujian.jsp:84");}
        }

        if (ruangRec == null) {
            out.print("{\"status\":\"error\",\"message\":\"" + Common.getBahasaConfig("Anda belum mendapat alokasi ruang ujian. Silakan hubungi panitia PPDB.") + "\"}");
            tx.rollback(); return;
        }

        final String  fNoUjian = noUjian;
        final Long    fCasisId = casis.getId();

        // Update cetakKartu di latar belakang (non-blocking)
        new Thread(new Runnable() {
            public void run() {
                Session threadSession = null;
                Transaction threadTx  = null;
                try {
                    threadSession = HibernateUtil.getSessionFactory().openSession();
                    threadTx      = threadSession.beginTransaction();
                    threadSession.createQuery(
                        "UPDATE CalonSiswa SET noUjian = :noUjian WHERE id = :id")
                        .setParameter("noUjian", fNoUjian)
                        .setParameter("id", fCasisId)
                        .executeUpdate();
                    threadTx.commit();
                } catch (Exception e) {
                    if (threadTx != null) { try { threadTx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_ujian.jsp:110");} }
                } finally {
                    if (threadSession != null) {
                        try { threadSession.clear(); }      catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_ujian.jsp:113");}
                        try { threadSession.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_ujian.jsp:114");}
                        try { threadSession.close(); }      catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_ujian.jsp:115");}
                    }
                }
            }
        }).start();

        // Susun parameter untuk Jasper Report
        Map parameters = ais.common.HashMapGenerator.getRand();
        try {
            Common.insertProperty(CalonSiswa.class, casis, parameters, "sekolah");
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_ujian.jsp:125");}
        parameters.put("calon_siswa_id", casis.getId());
        parameters.put("nomorUjian", noUjian);

        // Generate PDF
        File filePDF = Report.generateDownloadReport(
            Report.PDF, parameters, "KartuUjianSpsbMandiri", null, ais.ui.util.WaktuUtil.getDate());

        tx.commit();

        String urlPDF = Common.ROOT + "/pdf?p="
            + URLEncoder.encode(Common.desEncrypter.get().encrypt(filePDF.getName()), "UTF-8");

        out.print("{\"status\":\"success\",\"url\":\"" + urlPDF + "\"}");
        return;

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_ujian.jsp:142");
        if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_ujian.jsp:143");} }
        out.print("{\"status\":\"error\",\"message\":\""
            + Common.getBahasaConfig("Terjadi kesalahan peladen saat membuat kartu ujian. Silakan coba lagi atau hubungi panitia.")
            + "\"}");
        return;
    } finally {
        if (sessionLocal != null) {
            try { sessionLocal.clear(); }      catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_ujian.jsp:150");}
            try { sessionLocal.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_ujian.jsp:151");}
            try { sessionLocal.close(); }      catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_cetak_kartu_ujian.jsp:152");}
        }
    }
%>
