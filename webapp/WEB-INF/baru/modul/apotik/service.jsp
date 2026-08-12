<%@page session="true" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.EbisnisMenuKatalog"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.action.servlet.api.ApotikApiDispatcher"%>
<%!
private boolean boleh(Tbmuser user, String action) {
    if (user == null || user.hakAkses() == null || action == null) return false;
    JSONObject root = EbisnisMenuKatalog.urai(user.hakAkses().getEbisnisMenu());
    JSONObject menu = root.optJSONObject("menu");
    if (menu == null) return false;
    if (action.startsWith("apotik_resep_")) return menu.optBoolean("apotik_resep", false) || menu.optBoolean("apotik_kasir", false);
    if ("apotik_item_profil_simpan".equals(action)) return menu.optBoolean("apotik_formularium", false);
    if (action.startsWith("apotik_item_")) return menu.optBoolean("apotik_kasir", false) || menu.optBoolean("apotik_formularium", false)
            || menu.optBoolean("apotik_stok_opname", false) || menu.optBoolean("apotik_batch", false)
            || menu.optBoolean("apotik_pengadaan", false) || menu.optBoolean("apotik_retur", false);
    if (action.startsWith("apotik_terima_")) return menu.optBoolean("apotik_pengadaan", false);
    if (action.startsWith("apotik_opname_")) return menu.optBoolean("apotik_stok_opname", false);
    if (action.startsWith("apotik_retur_")) return menu.optBoolean("apotik_retur", false);
    if (action.startsWith("apotik_batch_")) return menu.optBoolean("apotik_batch", false);
    if (action.startsWith("apotik_laporan_terkendali")) return menu.optBoolean("apotik_narkotika", false) || menu.optBoolean("apotik_laporan", false);
    if (action.startsWith("apotik_laporan_")) return menu.optBoolean("apotik_laporan", false);
    if (action.startsWith("apotik_bayar")) return menu.optBoolean("apotik_kasir", false) || menu.optBoolean("apotik_resep", false);
    return false;
}
%>
<%
response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
JSONObject hasil = new JSONObject();
try {
    Tbmuser user = Common.getCurrentUser(request);
    if (user == null || user.getUserId() == null || !user.getAktif()) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        hasil.put("status", "error").put("message", "Sesi login tidak valid atau sudah berakhir.");
    } else {
        String expected = (String) session.getAttribute("apotikJspCsrf");
        String supplied = request.getHeader("X-Apotik-CSRF");
        if (expected == null || supplied == null || !expected.equals(supplied)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            hasil.put("status", "error").put("message", "Token keamanan halaman tidak valid. Muat ulang halaman.");
        } else {
            StringBuilder body = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            JSONObject payload = body.length() == 0 ? new JSONObject() : new JSONObject(body.toString());
            String action = payload.optString("action", "");
			if ("apotik_retur_simpan".equals(action)) {
				String jenis = payload.optString("jenis", "");
				if ("MASUK".equalsIgnoreCase(jenis)) payload.put("jenis", "penjualan");
				if ("KELUAR".equalsIgnoreCase(jenis)) payload.put("jenis", "pbf");
			}
            if (!boleh(user, action)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                hasil.put("status", "error").put("message", "Akses menu Apotik tidak diizinkan untuk role ini.");
            } else if (!ApotikApiDispatcher.dispatch(action, user, payload, hasil)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                hasil.put("status", "error").put("message", "Aksi tidak dikenal.");
            }
        }
    }
} catch (Exception e) {
    ais.common.ErrorAuditUtil.record(e, "apotik/service.jsp");
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    hasil = new JSONObject();
    hasil.put("status", "error").put("message", "Permintaan gagal diproses: " + e.getMessage());
}
out.print(hasil.toString());
%>
