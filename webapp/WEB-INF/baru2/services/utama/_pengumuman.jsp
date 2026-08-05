<%@ page contentType="application/json;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="
    ais.action.master.helper.util.PerguruanTinggiUtil,
    ais.database.model.PerguruanTinggi,
    ais.database.hibernate.HibernateUtil,
    org.hibernate.Session,
    java.util.List,
    java.util.Date,
    java.text.SimpleDateFormat,
    java.util.Locale
" %>
<%!
private static String escP(String s) {
    if (s == null) return "";
    return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r","").replace("\t"," ");
}
private static String stripP(String h) {
    if (h == null) return "";
    return h.replaceAll("<[^>]+>"," ").replaceAll("&nbsp;"," ").replaceAll("\\s+"," ").trim();
}
private static String truncP(String s, int n) {
    if (s == null) return "";
    return s.length() > n ? s.substring(0,n) + "…" : s;
}
%>
<%
response.setContentType("application/json;charset=UTF-8");
response.setHeader("Cache-Control","no-cache");

boolean hasError = false;
String errorMsg  = "";
StringBuilder sb = new StringBuilder(4096);
sb.append("{\"ok\":true,\"data\":[");

Session db = null;
try {
    db = HibernateUtil.openSession();
    Date today = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", new Locale("id","ID"));
    Long ptId = null;
    try {
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (pt != null) ptId = pt.getId();
    } catch (Exception ig) {}

    org.hibernate.Query q;
    if (ptId != null) {
        q = db.createQuery(
            "from PengumumanAkademis p " +
            "where (p.aktif = true OR p.aktif IS NULL) " +
            "AND (p.perguruanTinggi IS NULL OR p.perguruanTinggi.id = :ptId) " +
            "AND (p.langsungMunculDiTab IS NULL OR p.langsungMunculDiTab = false) " +
            "AND (p.tetapTampilkanPengumumanMeskipunSudahKelewat = true " +
            "     OR p.sampai IS NULL OR p.sampai >= :today) " +
            "ORDER BY p.tanggal DESC")
            .setLong("ptId", ptId).setDate("today", today).setMaxResults(30);
    } else {
        q = db.createQuery(
            "from PengumumanAkademis p " +
            "where (p.aktif = true OR p.aktif IS NULL) " +
            "AND (p.langsungMunculDiTab IS NULL OR p.langsungMunculDiTab = false) " +
            "AND (p.tetapTampilkanPengumumanMeskipunSudahKelewat = true " +
            "     OR p.sampai IS NULL OR p.sampai >= :today) " +
            "ORDER BY p.tanggal DESC")
            .setDate("today", today).setMaxResults(30);
    }

    List items = q.list();
    boolean first = true;
    for (int i = 0; i < items.size(); i++) {
        Object item = items.get(i);
        try {
            Class cls = item.getClass();
            Long id = (Long) cls.getMethod("getId").invoke(item);
            String judul = ""; try { String v=(String)cls.getMethod("getJudul").invoke(item); if(v!=null)judul=v; } catch(Exception ig){}
            String catatan=""; try { String v=(String)cls.getMethod("getCatatan").invoke(item); if(v!=null)catatan=v; } catch(Exception ig){}
            String diperuntukkan=""; try { String v=(String)cls.getMethod("getDiperuntukkan").invoke(item); if(v!=null)diperuntukkan=v; } catch(Exception ig){}
            String oleh=""; try { String v=(String)cls.getMethod("getOleh").invoke(item); if(v!=null)oleh=v; } catch(Exception ig){}
            Date tgl=null; try { tgl=(Date)cls.getMethod("getTanggal").invoke(item); } catch(Exception ig){}

            String tglStr  = tgl!=null ? sdf.format(tgl) : "";
            long   tglTs   = tgl!=null ? tgl.getTime() : 0L;
            String preview = truncP(stripP(catatan), 200);

            long katId=0; String katNama="Pengumuman"; int katUrut=99; boolean utama=false;
            try {
                Object kat = cls.getMethod("getKategoriPengumuman").invoke(item);
                if (kat != null) {
                    Class kc = kat.getClass();
                    try { Long ki=(Long)kc.getMethod("getId").invoke(kat); if(ki!=null)katId=ki; } catch(Exception ig){}
                    try { String kn=(String)kc.getMethod("getNama").invoke(kat); if(kn!=null)katNama=kn; } catch(Exception ig){}
                    try { Integer ku=(Integer)kc.getMethod("getNomorUrut").invoke(kat); if(ku!=null)katUrut=ku; } catch(Exception ig){}
                    utama = katUrut <= 1;
                }
            } catch (Exception ig) {}

            if (!first) sb.append(",");
            first = false;
            sb.append("{\"id\":").append(id!=null?id:0)
              .append(",\"judul\":\"").append(escP(judul)).append("\"")
              .append(",\"catatan\":\"").append(escP(catatan)).append("\"")
              .append(",\"preview\":\"").append(escP(preview)).append("\"")
              .append(",\"tanggalStr\":\"").append(escP(tglStr)).append("\"")
              .append(",\"tanggalTs\":").append(tglTs)
              .append(",\"diperuntukkan\":\"").append(escP(diperuntukkan)).append("\"")
              .append(",\"oleh\":\"").append(escP(oleh)).append("\"")
              .append(",\"kategoriId\":").append(katId>0?katId:0)
              .append(",\"kategoriNama\":\"").append(escP(katNama)).append("\"")
              .append(",\"kategoriNomorUrut\":").append(katUrut)
              .append(",\"utama\":").append(utama)
              .append("}");
        } catch (Exception ig) {}
    }
} catch (Exception e) {
    hasError = true;
    errorMsg = e.getMessage() != null ? e.getMessage() : "unknown";
} finally {
    if (db != null) try { db.close(); } catch (Exception ig) {}
}

out.clear();
if (hasError) {
    out.print("{\"ok\":false,\"data\":[],\"msg\":\"" + escP(errorMsg) + "\"}");
} else {
    sb.append("]}");
    out.print(sb.toString());
}
%>
