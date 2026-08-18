<%@page session="false"%>
<%--
  Endpoint JSON "Cetak Price Tag / POP" (versi JSP) — daftar produk untuk dibuatkan price tag.
  Sesi: currentSession() (dikelola kerangka kerja) -> TIDAK ditutup manual.
  Toko: pedagang dikunci ke tokonya; admin-kantin boleh memakai parameter toko.
--%>
<%@page import="java.util.*"%>
<%@page import="org.json.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.Produk"%>
<%!
    static String s(Object o){ return o==null?"":o.toString(); }
    /** Pedagang/toko dikunci ke tokonya sendiri; hanya admin-kantin boleh memakai parameter toko. */
    private Long resolveToko(HttpServletRequest request) {
        Tbmuser u = Common.getCurrentUser(request);
        if (u != null && u.getPedagang() != null && u.getPedagang().getToko() != null) {
            return u.getPedagang().getToko().getId();
        }
        String p = request.getParameter("toko");
        if (p != null && p.trim().length() > 0) {
            try { return Long.valueOf(p.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/barang/pricetag_service.jsp:28"); /* abaikan */ }
        }
        return null;
    }
%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
try {
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        result.put("status","01"); result.put("message","Sesi berakhir, silakan masuk kembali.");
        out.print(result.toString()); return;
    }
    String aksi = request.getParameter("aksi");
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    Session session = HibernateUtil.currentNativeSession();
    Long tokoId = resolveToko(request);

    if ("listProduk".equals(aksi)) {
        if (tokoId == null) { result.put("status","02"); result.put("message","Toko/kios belum dipilih."); out.print(result.toString()); return; }
        String q = request.getParameter("q");
        JSONArray arr = new JSONArray();
        for (Object o : ais.action.master.inventory.PriceTagUtil.listProduk(session, tokoId, q)) {
            Produk p = (Produk) o; JSONObject j = new JSONObject();
            j.put("id", p.getId());
            j.put("kode", s(p.getKode()));
            j.put("nama", s(p.getNama()));
            j.put("hargaJual", p.getHargaJual()==null?0:p.getHargaJual());
            j.put("hargaBeli", p.getHargaBeli()==null?0:p.getHargaBeli());
            arr.put(j);
        }
        result.put("data", arr); result.put("status","00");

    } else { result.put("status","98"); result.put("message","Aksi tidak dikenal."); }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/barang/pricetag_service.jsp:66");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/barang/pricetag_service.jsp:67");}
}
out.print(result.toString());
%>
