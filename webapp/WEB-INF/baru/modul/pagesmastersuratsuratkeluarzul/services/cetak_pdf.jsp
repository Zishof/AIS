<%@page import="ais.action.master.surat.util.SuratUtilHelper"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="java.io.File"%>
<%@page import="java.io.FileOutputStream"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.List"%>
<%@page import="org.apache.pdfbox.util.PDFMergerUtility"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.metadata.ClassMetadata"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.surat.SuratKeluar"%>
<%@page import="ais.database.model.surat.KlasifikasiSuratKeluar"%>
<%@page import="ais.database.model.surat.KlasifikasiSuratKeluarParemeter"%>
<%@page import="ais.database.model.surat.KlasifikasiSuratKeluarParemeterValue"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.action.report.Report"%>
<%@page import="ais.action.master.surat.KlasifikasiSuratKeluarAction"%>
<%-- Pastikan package SuratUtilHelper di bawah ini sesuai dengan struktur proyek Anda --%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
out.clear();

Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir.") + "\"}");
    return;
}

String rnd = request.getParameter("rnd");
String idSuratParam = request.getParameter("id");
String idKlasifikasiSuratParam = request.getParameter("idKlasifikasi");

if ((idSuratParam == null || idSuratParam.trim().isEmpty() || idSuratParam.equals("undefined")) && 
    (idKlasifikasiSuratParam == null || idKlasifikasiSuratParam.trim().isEmpty() || idKlasifikasiSuratParam.equals("undefined"))) {
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Parameter ID Surat atau Klasifikasi tidak ditemukan.") + "\"}");
    return;
}

SuratKeluar suratKeluar = null;
if (idSuratParam != null && !idSuratParam.trim().isEmpty() && !idSuratParam.equals("undefined")) {
    suratKeluar = (SuratKeluar) GeneralValueObject.ambilData(SuratKeluar.class, idSuratParam, true);
}

KlasifikasiSuratKeluar thisKlasifikasiSuratKeluar = null;
if (idKlasifikasiSuratParam != null && !idKlasifikasiSuratParam.trim().isEmpty() && !idKlasifikasiSuratParam.equals("undefined")) {
    thisKlasifikasiSuratKeluar = (KlasifikasiSuratKeluar) GeneralValueObject.ambilData(KlasifikasiSuratKeluar.class, idKlasifikasiSuratParam, true);
}

if (thisKlasifikasiSuratKeluar == null && suratKeluar != null) {
    thisKlasifikasiSuratKeluar = suratKeluar.getKlasifikasiSuratKeluar();
}

if (thisKlasifikasiSuratKeluar == null) {
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Klasifikasi Surat Keluar tidak ditemukan pada data ini.") + "\"}");
    return;
}

if (suratKeluar == null) {
    suratKeluar = new SuratKeluar();
    suratKeluar.setKlasifikasiSuratKeluar(thisKlasifikasiSuratKeluar);
}

Map<String, Object> parameters = new HashMap<String, Object>();
try {
    // MENGGUNAKAN SuratUtilHelper SESUAI PEMBARUAN ANDA
    parameters = SuratUtilHelper.ubahIsiSuratKeluar(null, thisKlasifikasiSuratKeluar,
            suratKeluar.getMahasiswa(), suratKeluar.getDosen(),
            suratKeluar.getPegawai(), tbmuser, suratKeluar.getKode(),
            suratKeluar, null, parameters);
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/cetak_pdf.jsp:80");
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Terjadi kesalahan saat mengekstraksi parameter surat.") + "\"}");
    return;
}

// INJEKSI PARAMETER DINAMIS DARI DB ATAU DARI SESI HTTP
Session sesDb = HibernateUtil.openSession();
try {
    @SuppressWarnings("unchecked")
    Map<String, String> drafSesi = (Map<String, String>) request.getSession().getAttribute("DRAF_PARAM_SURAT_" + rnd);
    
    @SuppressWarnings("unchecked")
    List<KlasifikasiSuratKeluarParemeter> paramsDef = sesDb.createCriteria(KlasifikasiSuratKeluarParemeter.class)
            .add(Restrictions.eq("klasifikasiSuratKeluar", thisKlasifikasiSuratKeluar))
            .list();

    for (KlasifikasiSuratKeluarParemeter pDef : paramsDef) {
        String valStr = pDef.getNilai();

        if (suratKeluar.getId() != null) {
            KlasifikasiSuratKeluarParemeterValue pValue = (KlasifikasiSuratKeluarParemeterValue) sesDb.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
                    .add(Restrictions.eq("klasifikasiSuratKeluarParemeter", pDef))
                    .add(Restrictions.eq("suratKeluar", suratKeluar))
                    .setMaxResults(1).uniqueResult();
            if (pValue != null && pValue.getNama() != null) valStr = pValue.getNama();
        } else if (drafSesi != null && drafSesi.containsKey(pDef.getId().toString())) {
            valStr = drafSesi.get(pDef.getId().toString()); // Baca dari Draf Sesi jika surat baru
        }

        if (valStr == null) valStr = "";
        String keyParam = pDef.getKey();
        parameters.put(keyParam, valStr);

        String tipe = pDef.getTipe();
        if (KlasifikasiSuratKeluarParemeter.DATA.equals(tipe)) {
            KlasifikasiSuratKeluarAction.masukkanTabelKeParameter(parameters, valStr);
        } else if (KlasifikasiSuratKeluarParemeter.GAMBAR.equals(tipe)) {
            try {
                LampiranLain l = LampiranLain.ambil(pDef.getId(), KlasifikasiSuratKeluarParemeter.class.getName());
                if (l != null) parameters.put(keyParam, l.ambilFile().getAbsolutePath());
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/cetak_pdf.jsp:120");}
        } else if (KlasifikasiSuratKeluarParemeter.DAFTAR_MAHASISWA.equals(tipe)) {
            int i = 1;
            for (String m : valStr.split(",")) {
                Mahasiswa mhs = m.trim().isEmpty() ? null : ConstantValues.ambilByNim(m.trim());
                if (mhs != null) {
                    Common.insertProperty(Mahasiswa.class, mhs, parameters, keyParam + "." + m.trim());
                    Common.insertProperty(Mahasiswa.class, mhs, parameters, keyParam + "." + i++);
                }
            }
        } else if (KlasifikasiSuratKeluarParemeter.DAFTAR_SISWA.equals(tipe)) {
            int i = 1;
            for (String m : valStr.split(",")) {
                Siswa sis = m.trim().isEmpty() ? null : ConstantValues.ambilByNis(m.trim());
                if (sis != null) {
                    Common.insertProperty(Siswa.class, sis, parameters, keyParam + "." + m.trim());
                    Common.insertProperty(Siswa.class, sis, parameters, keyParam + "." + i++);
                }
            }
        }
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/cetak_pdf.jsp:142");
} finally {
    try { sesDb.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/cetak_pdf.jsp:144");}
    try { sesDb.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/cetak_pdf.jsp:145");}
}

// Proses Kompilasi dan Penggabungan PDF
PDFMergerUtility ut = new PDFMergerUtility();
boolean adaPdf = false;

for (int index = 1; index <= 15; index++) {
    try {
        LampiranLain lampiranLain = LampiranLain.ambil(thisKlasifikasiSuratKeluar.getId(),
                LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
        if (lampiranLain != null && lampiranLain.getId() != null) {
            try {
                File file = Report.generateCompileFileReport(Report.PDF, parameters,
                        lampiranLain.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate(), false);
                if (file != null && file.exists()) {
                    ut.addSource(file);
                    adaPdf = true;
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/cetak_pdf.jsp:164");}
        }
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/cetak_pdf.jsp:166");}
}

if (adaPdf) {
    try {
        File filePdfBaru = new File(Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
        ut.setDestinationStream(new FileOutputStream(filePdfBaru));
        ut.mergeDocuments();

        String URLPDF = Common.ROOT + "/pdf?p=" + URLEncoder.encode(Common.desEncrypter.get().encrypt(filePdfBaru.getName()), "UTF-8");
        out.print("{\"status\":\"success\", \"url\":\"" + URLPDF + "\"}");
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/cetak_pdf.jsp:178");
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Terjadi kesalahan saat menggabungkan dokumen PDF.") + "\"}");
    }
} else {
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Tidak ada template rancangan PDF yang ditemukan.") + "\"}");
}
%>