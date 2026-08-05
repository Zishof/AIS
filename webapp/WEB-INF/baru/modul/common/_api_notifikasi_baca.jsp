<%@page contentType="application/json; charset=UTF-8" trimDirectiveWhitespaces="true"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.common.NotifikasiCache"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
    /*
     * Menandai notifikasi "sudah dibaca" MELALUI NotifikasiCache (bukan SQL mentah via
     * servlet /Data). Ini penting karena daftar notifikasi dilayani dari cache
     * multi-level (L1/L2/L3): memperbarui basis data secara langsung TIDAK menyegarkan
     * cache, sehingga badge & warna baru berubah setelah TTL berakhir.
     * NotifikasiCache.tandaiSudahDibaca / tandaiSemuaDibaca memperbarui basis data
     * SEKALIGUS membalik status pada snapshot memori -> tampilan langsung konsisten.
     *
     * Parameter:
     *   id=<angka>     -> tandai satu notifikasi
     *   semua=true     -> tandai semua notifikasi milik pengguna yang sedang masuk
     */
    JSONObject res = new JSONObject();
    try {
        Tbmuser tbmuser = Common.getCurrentUser(request);
        if (tbmuser == null || tbmuser.getUserId() == null) {
            res.put("status", "error");
            res.put("message", Common.getBahasaConfig("Akses ditolak. Sesi Anda telah berakhir."));
            out.print(res.toString());
            return;
        }

        String semua = request.getParameter("semua");
        String idStr = request.getParameter("id");

        if (semua != null && semua.equalsIgnoreCase("true")) {
            NotifikasiCache.tandaiSemuaDibaca(tbmuser.getUserId());
            res.put("status", "success");
        } else if (idStr != null && idStr.trim().length() > 0) {
            Long id = null;
            try {
                id = Long.valueOf(idStr.trim());
            } catch (Exception exId) {
                id = null;
            }
            if (id == null) {
                res.put("status", "error");
                res.put("message", "Id notifikasi tidak valid.");
            } else if (!NotifikasiCache.milikPengguna(tbmuser.getUserId(), id)) {
                // Penjaga: hanya penerima notifikasi yang boleh menandainya dibaca.
                res.put("status", "error");
                res.put("message", Common.getBahasaConfig("Akses ditolak."));
            } else {
                NotifikasiCache.tandaiSudahDibaca(id, tbmuser.getUserId());
                res.put("status", "success");
            }
        } else {
            res.put("status", "error");
            res.put("message", "Parameter id atau semua wajib diisi.");
        }
    } catch (Exception e) {
        res.put("status", "error");
        res.put("message", e.getMessage());
    }

    out.print(res.toString());
%>
