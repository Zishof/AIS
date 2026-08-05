<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ErrorAuditUtil"%>
<%@page import="ais.common.ElearningSessionUtil"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.List"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
JSONObject resp = new JSONObject();
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    resp.put("status", "error");
    resp.put("message", "Sesi berakhir.");
    out.print(resp);
    return;
}
String ppuParam = request.getParameter("ppu");
if (ppuParam == null || ppuParam.trim().isEmpty()) {
    resp.put("status", "error");
    resp.put("message", "Parameter ppu wajib.");
    out.print(resp);
    return;
}
Session s = null;
Transaction tx = null;
try {
    s = HibernateUtil.getSessionFactory().openSession();
    tx = s.beginTransaction();

    PertemuanPunyaUjian src = (PertemuanPunyaUjian) s.get(PertemuanPunyaUjian.class, Long.parseLong(ppuParam.trim()));
    if (src == null || src.getUjian() == null) throw new Exception("PPU tidak ditemukan.");
    if (!src.getPertemuan().bolehUbahAbsenSaja(tbmuser)) throw new Exception("Tidak ada hak akses.");

    String suffix = " (salinan " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date()) + ")";

    // 1. Clone Ujian master
    Ujian srcUjian = src.getUjian();
    Ujian newUjian = new Ujian();
    newUjian.setId(null);
    newUjian.setNama(srcUjian.getNama() + suffix);
    newUjian.setJenisKoreksi(srcUjian.getJenisKoreksi());
    newUjian.setTatatertibUjian(srcUjian.getTatatertibUjian());
    s.save(newUjian);

    // 2. Clone UjianPunyaSoal -> BankSoal -> BankSoalDetail
    List soals = s.createCriteria(UjianPunyaSoal.class)
        .add(Restrictions.eq("ujian", srcUjian)).list();
    for (int i = 0; i < soals.size(); i++) {
        UjianPunyaSoal ups = (UjianPunyaSoal) soals.get(i);
        BankSoal srcBs = ups.getBankSoal();
        if (srcBs == null) continue;

        // Clone BankSoal
        BankSoal newBs = new BankSoal();
        newBs.setId(null);
        newBs.setSoal(srcBs.getSoal());
        newBs.setJenisKoreksi(srcBs.getJenisKoreksi());
        newBs.setSkor(srcBs.getSkor());
        newBs.setSkorSalah(srcBs.getSkorSalah());
        newBs.setSkorDefault(srcBs.getSkorDefault());
        newBs.setNomorUrut(srcBs.getNomorUrut());
        s.save(newBs);

        // Clone BankSoalDetail
        List details = s.createCriteria(BankSoalDetail.class)
            .add(Restrictions.eq("bankSoal", srcBs)).list();
        for (int j = 0; j < details.size(); j++) {
            BankSoalDetail d = (BankSoalDetail) details.get(j);
            BankSoalDetail nd = new BankSoalDetail();
            nd.setId(null);
            nd.setBankSoal(newBs);
            nd.setJawaban(d.getJawaban());
            nd.setBetul(d.getBetul());
            nd.setHuruf(d.getHuruf());
            nd.setEssay(d.getEssay());
            nd.setKodeUnik(null); // auto-generated on first access
            s.save(nd);
        }

        // Clone UjianPunyaSoal
        UjianPunyaSoal newUps = new UjianPunyaSoal();
        newUps.setId(null);
        newUps.setUjian(newUjian);
        newUps.setBankSoal(newBs);
        newUps.setNomorUrut(ups.getNomorUrut());
        s.save(newUps);
    }

    // 3. Clone PertemuanPunyaUjian (non-aktif)
    PertemuanPunyaUjian newPpu = new PertemuanPunyaUjian();
    newPpu.setId(null);
    newPpu.setPertemuan(src.getPertemuan());
    newPpu.setUjian(newUjian);
    newPpu.setAktif(Boolean.FALSE);
    newPpu.setKeterangan(src.getKeterangan());
    newPpu.setJmlDitampilkan(src.getJmlDitampilkan());
    newPpu.setJumlahBolehIkut(src.getJumlahBolehIkut());
    newPpu.setLama(src.getLama());
    newPpu.setDibatasiWaktu(src.getDibatasiWaktu());
    newPpu.setRandom(src.getRandom());
    newPpu.setLihatNilaiSetelahUjian(src.getLihatNilaiSetelahUjian());
    newPpu.setLihatJawabanSetelahUjian(src.getLihatJawabanSetelahUjian());
    newPpu.setFormatNilais(src.getFormatNilais());
    s.save(newPpu);

    tx.commit();
    tx = null;
    resp.put("status", "success");
    resp.put("message", "Ujian berhasil digandakan. Ujian baru dibuat dalam status non-aktif.");
} catch (Exception e) {
    if (tx != null) try { tx.rollback(); } catch (Exception ignored) {}
    ErrorAuditUtil.record(e, "_gandakan_ujian.jsp");
    resp.put("status", "error");
    resp.put("message", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
} finally {
    ElearningSessionUtil.closeQuietly(s);
}
out.print(resp.toString());
out.flush();
%>
