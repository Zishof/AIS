<%@page session="false"%>
<%--
  Endpoint JSON editor Stok Minimum & Expired. Aksi: list (produk + stok/min/batch/expired),
  simpan (set HANYA stokMinimum/batch/tanggalExpired pada produk yg ada — tidak menyentuh field lain).
  Sesi currentSession(). Hanya admin (bukan pedagang) yang boleh mengubah. Toko dipaksa bila pedagang.
--%>
<%@page import="org.json.*"%>
<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.SQLQuery"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.Produk"%>
<%!
    private static boolean ada(String s){ return s!=null && s.trim().length()>0; }
%>
<%
response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd");
try {
    Tbmuser u = Common.getCurrentUser(request);
    if (u==null || u.getUserId()==null){ result.put("status","01"); result.put("message","Sesi berakhir."); out.print(result.toString()); return; }
    boolean boleh = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
    String aksi = request.getParameter("aksi");
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    Session session = HibernateUtil.currentNativeSession();
    Long tokoId=null; boolean lockToko=false;
    if (u.getPedagang()!=null && u.getPedagang().getToko()!=null){ tokoId=u.getPedagang().getToko().getId(); lockToko=true; }

    if ("simpan".equals(aksi)) {
        if (!boleh){ result.put("status","03"); result.put("message","Hanya admin yang boleh mengubah."); out.print(result.toString()); return; }
        String idS=request.getParameter("id");
        if (!ada(idS)){ result.put("status","02"); result.put("message","ID kosong."); out.print(result.toString()); return; }
        Produk p = (Produk) session.get(Produk.class, Long.valueOf(idS.trim()));
        if (p==null || (lockToko && (p.getToko()==null || !p.getToko().getId().equals(tokoId)))){ result.put("status","02"); result.put("message","Produk tidak ditemukan."); out.print(result.toString()); return; }
        double sm = 0; try { sm = Double.parseDouble(request.getParameter("stokMin")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/stok_expired_service.jsp:40");}
        p.setStokMinimum(Double.valueOf(sm));
        p.setBatch(request.getParameter("batch"));
        String ex = request.getParameter("expired");
        if (ada(ex)) { try { p.setTanggalExpired(DF.parse(ex.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/stok_expired_service.jsp:44");} }
        else { p.setTanggalExpired(null); }
        Common.refreshSaveOrUpdate(session, p);
        result.put("status","00"); result.put("message","Tersimpan.");

    } else { // list
        StringBuilder sb = new StringBuilder("select id, kode, nama, coalesce(stok,0), coalesce(stok_minimum,0), coalesce(batch,''), tanggal_expired from koperasi.produk where aktif=true ");
        if (lockToko) sb.append(" and toko=").append(tokoId);
        String q = request.getParameter("q");
        if (ada(q)) { sb.append(" and (lower(kode) like :q or lower(nama) like :q) "); }
        sb.append(" order by nama asc ");
        SQLQuery sq = session.createSQLQuery(sb.toString());
        if (ada(q)) sq.setParameter("q", "%"+q.trim().toLowerCase()+"%");
        sq.setMaxResults(1000);
        JSONArray arr = new JSONArray();
        for (Object o : sq.list()){ Object[] a=(Object[])o; JSONObject j=new JSONObject();
            j.put("id", a[0]); j.put("kode", a[1]==null?"":a[1].toString()); j.put("nama", a[2]==null?"":a[2].toString());
            j.put("stok", ((Number)a[3]).doubleValue()); j.put("stokMin", ((Number)a[4]).doubleValue());
            j.put("batch", a[5]==null?"":a[5].toString()); j.put("expired", a[6]==null?"":DF.format((java.util.Date)a[6]));
            arr.put(j);
        }
        result.put("status","00"); result.put("data",arr);
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/stok_expired_service.jsp:68");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()+" (pastikan sudah RESTART agar kolom stok_minimum/tanggal_expired/batch terbentuk)"); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/stok_expired_service.jsp:69");}
}
out.print(result.toString());
%>
