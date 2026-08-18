<%@page contentType="application/json; charset=UTF-8" trimDirectiveWhitespaces="true"%>
<%@page import="java.util.List"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.common.NotifikasiCache"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="java.text.SimpleDateFormat"%>
<%
    JSONObject responseJson = new JSONObject();
    JSONArray dataArray = new JSONArray();

    try {
        Tbmuser tbmuser = Common.getCurrentUser(request);
        if (tbmuser == null || tbmuser.getUserId() == null) {
            responseJson.put("status", "error");
            responseJson.put("message", Common.getBahasaConfig("Akses ditolak. Sesi Anda telah berakhir."));
            out.print(responseJson.toString());
            return;
        }

        // Dibaca dari cache multi-level (L1/L2/L3) yang dihangatkan saat startup,
        // sehingga pusat notifikasi tampil INSTAN tanpa query basis data per akses.
        List<NotifikasiCache.Item> listNotif = NotifikasiCache.untukUser(tbmuser.getUserId(), 5);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");

        for (NotifikasiCache.Item notif : listNotif) {
            JSONObject obj = new JSONObject();
            obj.put("id", notif.getId());

            // 'keterangan' berformat JSON {subject, body, bukaJsp, ...}. Tampilkan isi
            // yang rapi (subject + body) dan ekstrak URL halaman tujuan klik untuk JSP.
            String ket = notif.getKeterangan();
            String pesan = ket;
            String bukaJsp = "";
            try {
                JSONObject k = new JSONObject(ket);
                String subj = k.optString("subject", "");
                String bdy = k.optString("body", "");
                if (bdy != null && !bdy.isEmpty()) {
                    pesan = (subj.isEmpty() ? "" : "<b>" + subj + "</b><br>") + bdy;
                }
                bukaJsp = k.optString("bukaJsp", "");
            } catch (Exception exParse) { ais.common.ErrorAuditUtil.record(exParse, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_api_notifikasi_sistem.jsp:44");
                // 'keterangan' bukan JSON (data lama) -> tampilkan apa adanya.
            }

            obj.put("message", pesan);
            obj.put("bukaJsp", bukaJsp);
            obj.put("type", notif.getStatusNotif() != null ? notif.getStatusNotif() : "INFO");
            obj.put("time", notif.getWaktu() != null ? sdf.format(notif.getWaktu()) : Common.getBahasaConfig("Baru Saja"));
            // Status baca PER-PENERIMA (tabel notifikasi_dibaca), bukan status per-record.
            obj.put("isRead", NotifikasiCache.sudahDibacaOleh(notif.getId(), tbmuser.getUserId()));
            dataArray.put(obj);
        }
        responseJson.put("status", "success");
        responseJson.put("data", dataArray);

    } catch (Exception e) {
        responseJson.put("status", "error");
        responseJson.put("message", e.getMessage());
    }

    out.print(responseJson.toString());
%>
