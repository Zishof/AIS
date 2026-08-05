<%@ page contentType="application/json;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="
    ais.database.hibernate.HibernateUtil,
    org.hibernate.Session,
    java.util.List,
    java.util.Date,
    java.text.SimpleDateFormat,
    java.util.Locale
" %>
<%!
private static String escK(String s) {
    if (s == null) return "";
    return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r","").replace("\t"," ");
}
%>
<%
response.setContentType("application/json;charset=UTF-8");
response.setHeader("Cache-Control","no-cache");

boolean hasError = false;
String errorMsg  = "";
StringBuilder sb = new StringBuilder(2048);
sb.append("{\"ok\":true,\"data\":[");

Session db = null;
try {
    db = HibernateUtil.openSession();
    Date today = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", new Locale("id","ID"));

    List items = db.createQuery(
        "from KalenderAkademik k " +
        "where (k.aktif = true OR k.aktif IS NULL) " +
        "AND k.tanggalSelesai >= :today " +
        "ORDER BY k.tanggalMulai ASC")
        .setDate("today", today).setMaxResults(15).list();

    boolean first = true;
    for (int i = 0; i < items.size(); i++) {
        Object item = items.get(i);
        try {
            Class cls = item.getClass();
            Long id = (Long) cls.getMethod("getId").invoke(item);
            String nama=""; try { String v=(String)cls.getMethod("getNamaKegiatanAkademik").invoke(item); if(v!=null)nama=v; } catch(Exception ig){}
            String desk=""; try { String v=(String)cls.getMethod("getDeskripsiKegiatanAkademik").invoke(item); if(v!=null)desk=v; } catch(Exception ig){}
            Date mulai=null;   try { mulai  =(Date)cls.getMethod("getTanggalMulai").invoke(item); } catch(Exception ig){}
            Date selesai=null; try { selesai=(Date)cls.getMethod("getTanggalSelesai").invoke(item); } catch(Exception ig){}
            Integer hari=null; try { hari=(Integer)cls.getMethod("getJumlahHari").invoke(item); } catch(Exception ig){}
            String status="";  try { String v=(String)cls.getMethod("getStatus").invoke(item); if(v!=null)status=v; } catch(Exception ig){}
            String jenis="";
            try {
                Object jk = cls.getMethod("getJenisKegiatan").invoke(item);
                if (jk != null) { String jn=(String)jk.getClass().getMethod("getNama").invoke(jk); if(jn!=null)jenis=jn; }
            } catch (Exception ig) {}

            String tahunNama=""; String semesterNama="";
            try {
                Object ta=cls.getMethod("getTahunAkademik").invoke(item);
                if(ta!=null){
                    Class tac=ta.getClass();
                    try{ String v=(String)tac.getMethod("getNama").invoke(ta); if(v!=null)tahunNama=v; } catch(Exception ig2){}
                    try{ String v=(String)tac.getMethod("getSemester").invoke(ta); if(v!=null)semesterNama=v; } catch(Exception ig2){}
                }
            } catch(Exception ig){}

            String mulaiStr   = mulai   != null ? sdf.format(mulai)   : "";
            String selesaiStr = selesai != null ? sdf.format(selesai) : "";
            long   mulaiTs    = mulai   != null ? mulai.getTime()     : 0L;
            long   selesaiTs  = selesai != null ? selesai.getTime()   : 0L;

            if (!first) sb.append(",");
            first = false;
            sb.append("{\"id\":").append(id!=null?id:0)
              .append(",\"nama\":\"").append(escK(nama)).append("\"")
              .append(",\"deskripsi\":\"").append(escK(desk)).append("\"")
              .append(",\"tanggalMulaiStr\":\"").append(escK(mulaiStr)).append("\"")
              .append(",\"tanggalSelesaiStr\":\"").append(escK(selesaiStr)).append("\"")
              .append(",\"tanggalMulaiTs\":").append(mulaiTs)
              .append(",\"hari\":").append(hari!=null?hari:1)
              .append(",\"status\":\"").append(escK(status)).append("\"")
              .append(",\"jenis\":\"").append(escK(jenis)).append("\"")
              .append(",\"tahunAkademik\":\"").append(escK(tahunNama)).append("\"")
              .append(",\"semester\":\"").append(escK(semesterNama)).append("\"")
              .append(",\"tanggalSelesaiTs\":").append(selesaiTs)
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
    out.print("{\"ok\":false,\"data\":[],\"msg\":\"" + escK(errorMsg) + "\"}");
} else {
    sb.append("]}");
    out.print(sb.toString());
}
%>
