<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.FormatNilai"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="org.json.JSONObject"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
JSONObject resp = new JSONObject();
Tbmuser currentUser = Common.getCurrentUser(request);

if (currentUser == null) {
    resp.put("status", "error").put("message", "Sesi berakhir.");
    out.print(resp.toString()); return;
}

Session mySession = null;
Transaction tx = null;

try {
    mySession = HibernateUtil.getSessionFactory().openSession();
    tx = mySession.beginTransaction();
    
    String action = request.getParameter("action");
    Long ppuId = Long.parseLong(request.getParameter("ppu"));
    PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) mySession.get(PertemuanPunyaUjian.class, ppuId);
    if (ppu == null) throw new Exception("Data sesi ujian tidak ditemukan: " + ppuId);

    if ("save".equals(action)) {
        boolean isObe = "true".equals(request.getParameter("isObe"));
        if (isObe) {
            ppu.setFormatNilais(request.getParameter("formatNilaisJson"));
        } else {
            String fnId = request.getParameter("formatNilaiId");
            if (fnId != null && !fnId.isEmpty()) {
                ppu.setFormatNilai((FormatNilai) mySession.get(FormatNilai.class, Long.parseLong(fnId)));
            } else {
                ppu.setFormatNilai(null);
            }
            ppu.setProsentase(Double.parseDouble(request.getParameter("prosentase")));
        }
        mySession.update(ppu);
        tx.commit(); tx = null;
        resp.put("status", "success").put("message", Common.getBahasaConfig("Pengaturan format nilai berhasil disimpan."));

    } else if ("sync".equals(action)) {
        // PANGGIL WEB GRADING HELPER (VERSI TANPA ZKOSS UI)
        boolean isObe = false;
        if (ppu.getPertemuan() != null && ppu.getPertemuan().getPerkuliahan() != null 
            && ppu.getPertemuan().getPerkuliahan().getKurikulum() != null 
            && ppu.getPertemuan().getPerkuliahan().getKurikulum().apakahObe(
                ppu.getPertemuan().getPerkuliahan().getTahunAjaran(), 
                ppu.getPertemuan().getPerkuliahan().getGanjilGenap())) {
            isObe = true;
        }

        if (isObe) {
            ais.common.WebGradingHelper.sinkronObe(ppu.getPertemuan().getPerkuliahan(), ppu.getFormatNilais(), currentUser);
        } else {
            ais.common.WebGradingHelper.sinkronNonObe(ppu.getPertemuan().getPerkuliahan(), ppu.getFormatNilai(), currentUser);
        }
        tx.commit(); tx = null;
        resp.put("status", "success").put("message", Common.getBahasaConfig("Nilai berhasil dikalkulasi dan disinkronkan ke rekap KRS Mahasiswa."));
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_format_nilai_services.jsp:69");
    if (tx != null) tx.rollback();
    resp.put("status", "error").put("message", "Error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}
out.print(resp.toString());
%>