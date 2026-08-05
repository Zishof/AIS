<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%!
private String elearningText(String key, String def) {
    try {
        Konfigurasi k = Common.getKonfigurasi(key, def);
        return k == null || k.getNilai() == null || k.getNilai().trim().length() == 0 ? def : k.getNilai().trim();
    } catch (Throwable t) {
        return def;
    }
}
private boolean elearningAktif(String key, String def) {
    return Konfigurasi.AKTIF.equalsIgnoreCase(elearningText(key, def));
}
%>
