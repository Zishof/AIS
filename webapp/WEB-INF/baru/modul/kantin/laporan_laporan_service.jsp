<%@page session="false"%>
<%--
  ============================================================================
  ENDPOINT DATA (JSON) untuk tampilan layar Laporan-Laporan e-Kantin.
  Logika kueri dipusatkan di ais.action.master.koperasi.helper.LaporanKantinUtil
  (dipakai bersama dengan servlet PDF ais.action.servlet.LaporanKantinPdf).
  Sesi: currentSession() (di dalam util) -> TIDAK ditutup di sini (konteks JSP/Baru).
  Keluaran JSON: { status, judul, kolom:[{l,t}], baris:[[...]], catatan, lockToko, grup }
  ============================================================================
--%>
<%@page import="org.json.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="ais.action.master.koperasi.helper.LaporanKantinUtil"%>
<%@page import="ais.action.master.koperasi.helper.LaporanKantinUtil.Hasil"%>
<%@page import="ais.action.master.koperasi.helper.LaporanKantinUtil.Kolom"%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject hasil = new JSONObject();
try {
    Hasil H = LaporanKantinUtil.build(request);
    if (!"00".equals(H.status)) {
        hasil.put("status", H.status);
        hasil.put("message", H.message);
        out.print(hasil.toString()); return;
    }
    JSONArray kolom = new JSONArray();
    for (Kolom k : H.kolom) { JSONObject o = new JSONObject(); o.put("l", k.label); o.put("t", k.tipe); kolom.put(o); }

    SimpleDateFormat fmtTgl = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    JSONArray baris = new JSONArray();
    for (Object[] row : H.baris) {
        JSONArray b = new JSONArray();
        for (int i = 0; i < H.tipe.length; i++) {
            Object v = i < row.length ? row[i] : null;
            if ("num".equals(H.tipe[i])) {
                if (v instanceof Number) { b.put(((Number) v).doubleValue()); }
                else if (v == null) { b.put(JSONObject.NULL); }
                else { b.put(0.0); }
            } else if ("tgl".equals(H.tipe[i])) {
                b.put(v instanceof java.util.Date ? fmtTgl.format((java.util.Date) v) : "");
            } else {
                b.put(v == null ? "" : v.toString());
            }
        }
        baris.put(b);
    }

    hasil.put("status", "00");
    hasil.put("judul", H.judul);
    hasil.put("kolom", kolom);
    hasil.put("baris", baris);
    hasil.put("catatan", H.catatan);
    hasil.put("lockToko", H.lockToko);
    hasil.put("grup", H.grup);
    hasil.put("grandTotal", H.grandTotal);

} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/laporan_laporan_service.jsp:59");
    try { hasil.put("status", "99"); hasil.put("message", "Terjadi kesalahan: " + e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/laporan_laporan_service.jsp:60");}
}
out.print(hasil.toString());
%>
