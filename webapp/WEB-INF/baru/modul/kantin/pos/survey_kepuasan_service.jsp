<%@page session="false"%>
<%--
  Endpoint JSON: simpan rating kepuasan pembeli dari Layar Pelanggan (layar kedua POS).
  Aksi tunggal "simpan": rating (1-5, wajib), catatan (opsional), toko (opsional).
  Sesi currentNativeSession() (dipanggil dari JSP /baru, ditutup terpusat FilterJSP). Jendela layar
  pelanggan berbagi cookie sesi dengan jendela kasir (sama origin) sehingga Common.getCurrentUser
  tetap resolve ke kasir yang sedang login.
--%>
<%@page import="org.json.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.SurveyKepuasanPos"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%!
    private static boolean ada(String s){ return s!=null && s.trim().length()>0; }
%>
<%
response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
try {
    Tbmuser u = Common.getCurrentUser(request);
    if (u==null || u.getUserId()==null){ result.put("status","01"); result.put("message","Sesi berakhir."); out.print(result.toString()); return; }

    int rating = 0;
    try { rating = Integer.parseInt(request.getParameter("rating")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/pos/survey_kepuasan_service.jsp:27");}
    if (rating < 1 || rating > 5) { result.put("status","02"); result.put("message","Rating tidak valid."); out.print(result.toString()); return; }

    Session session = HibernateUtil.currentNativeSession();

    SurveyKepuasanPos sv = new SurveyKepuasanPos();
    sv.setRating(Integer.valueOf(rating));
    String catatan = request.getParameter("catatan");
    if (ada(catatan)) sv.setCatatan(catatan.trim());

    String tokoS = request.getParameter("toko");
    Long tokoId = null;
    if (u.getPedagang()!=null && u.getPedagang().getToko()!=null) { tokoId = u.getPedagang().getToko().getId(); }
    else if (ada(tokoS)) { try { tokoId = Long.valueOf(tokoS.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/pos/survey_kepuasan_service.jsp:40");} }
    if (tokoId != null) { Toko t=(Toko) session.get(Toko.class, tokoId); if (t!=null) sv.setToko(t); }

    sv.setOlehId(u.getUserId()); sv.setOleh(u.getUserId());
    Common.refreshSaveOrUpdate(session, sv);

    result.put("status","00"); result.put("message","Terima kasih atas penilaian Anda.");
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/pos/survey_kepuasan_service.jsp:48");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()+" (pastikan sudah RESTART agar tabel koperasi.survey_kepuasan_pos terbentuk)"); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/pos/survey_kepuasan_service.jsp:49");}
}
out.print(result.toString());
%>
