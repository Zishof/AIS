<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.Ujian"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
JSONObject responseJson = new JSONObject();
Tbmuser tbmuser = Common.getCurrentUser(request);

if (tbmuser == null || tbmuser.getUserId() == null) {
    responseJson.put("status", "error");
    responseJson.put("message", Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali."));
    out.print(responseJson.toString()); return;
}

String ppuParam = request.getParameter("ppu");
String namaUjian = request.getParameter("namaUjian");

if (ppuParam == null || ppuParam.trim().isEmpty() || namaUjian == null || namaUjian.trim().isEmpty()) {
    responseJson.put("status", "error");
    responseJson.put("message", Common.getBahasaConfig("Informasi utama (ID Sesi & Nama Ujian) tidak boleh kosong."));
    out.print(responseJson.toString()); return;
}

Session mySession = null;
Transaction tx = null;

try {
    mySession = HibernateUtil.getSessionFactory().openSession();
    tx = mySession.beginTransaction();

    Long idppu = Long.parseLong(ppuParam.trim());
    PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) mySession.get(PertemuanPunyaUjian.class, idppu);

    if (ppu == null || ppu.getUjian() == null) {
        throw new Exception(Common.getBahasaConfig("Sesi ujian tidak valid."));
    }

    // 1. Ekstraksi Data dari Parameter Formulir
    String keterangan = request.getParameter("keterangan");
    String jenisKoreksi = request.getParameter("jenisKoreksi");
    
    int jmlSoal = request.getParameter("jmlSoal") != null && !request.getParameter("jmlSoal").isEmpty() ? Integer.parseInt(request.getParameter("jmlSoal")) : 0;
    int jmlPercobaan = request.getParameter("jmlPercobaan") != null && !request.getParameter("jmlPercobaan").isEmpty() ? Integer.parseInt(request.getParameter("jmlPercobaan")) : 1;
    
    boolean isDibatasiWaktu = "true".equals(request.getParameter("isDibatasiWaktu"));
    boolean isRandom = "true".equals(request.getParameter("isRandom"));
    boolean lihatNilai = "true".equals(request.getParameter("lihatNilai"));
    boolean lihatJawaban = "true".equals(request.getParameter("lihatJawaban"));

    SimpleDateFormat sdfDatetimeLocal = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
    
    Date tglMulai = null;
    Date tglSelesai = null;
    Date durasi = null;

    try {
        if (request.getParameter("tglMulai") != null && !request.getParameter("tglMulai").isEmpty()) tglMulai = sdfDatetimeLocal.parse(request.getParameter("tglMulai"));
        if (request.getParameter("tglSelesai") != null && !request.getParameter("tglSelesai").isEmpty()) tglSelesai = sdfDatetimeLocal.parse(request.getParameter("tglSelesai"));
        if (request.getParameter("durasi") != null && !request.getParameter("durasi").isEmpty()) durasi = sdfTime.parse(request.getParameter("durasi"));
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_simpan_info_ujian.jsp:69"); /* Format parsing diabaikan jika tidak valid */ }

    // 2. Pembaruan pada objek Ujian (Master)
    Ujian ujian = ppu.getUjian();
    ujian.setNama(namaUjian.trim());
    ujian.setJenisKoreksi(jenisKoreksi);
    mySession.update(ujian);

    // 3. Pembaruan pada objek PertemuanPunyaUjian (Child/Sesi)
    ppu.setKeterangan(keterangan != null ? keterangan.trim() : "");
    ppu.setJmlDitampilkan(jmlSoal);
    ppu.setJumlahBolehIkut(jmlPercobaan);
    ppu.setLama(durasi);
    ppu.setDibatasiWaktu(isDibatasiWaktu);
    ppu.setMulaiUjian(tglMulai);
    ppu.setSampaiUjian(tglSelesai);
    ppu.setRandom(isRandom);
    ppu.setLihatNilaiSetelahUjian(lihatNilai);
    ppu.setLihatJawabanSetelahUjian(lihatJawaban);

    // Anti-kecurangan baru
    boolean blokirTangkapLayar = "true".equals(request.getParameter("blokirTangkapLayar"));
    boolean larangMultiDevice = "true".equals(request.getParameter("larangMultiDevice"));
    ppu.setAntiCurangBlokirTangkapLayar(blokirTangkapLayar);
    ppu.setAntiCurangLarangMultiDevice(larangMultiDevice);

    // Restriksi lokasi
    String latStr = request.getParameter("lokasiLat");
    String lonStr = request.getParameter("lokasiLon");
    String radStr = request.getParameter("lokasiRadius");
    try {
        ppu.setAntiCurangLokasiLatitude((latStr != null && !latStr.trim().isEmpty()) ? Double.parseDouble(latStr.trim()) : null);
        ppu.setAntiCurangLokasiLongitude((lonStr != null && !lonStr.trim().isEmpty()) ? Double.parseDouble(lonStr.trim()) : null);
        ppu.setAntiCurangLokasiRadius((radStr != null && !radStr.trim().isEmpty()) ? Integer.parseInt(radStr.trim()) : null);
    } catch (NumberFormatException nfe) { /* ignore invalid input */ }

    mySession.update(ppu);

    tx.commit(); tx = null;

    responseJson.put("status", "success");
    responseJson.put("message", Common.getBahasaConfig("Pengaturan ujian berhasil disimpan."));

} catch (Exception e) {
    if (tx != null) tx.rollback();
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_simpan_info_ujian.jsp:97");
    responseJson.put("status", "error");
    responseJson.put("message", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

out.print(responseJson.toString());
out.flush();
%>