<%@page session="false"%>
<%@page import="org.json.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.SQLQuery"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pembelian"%>
<%@page import="ais.database.model.koperasi.PembelianAnggotaKoperasi"%>
<%@page import="ais.action.master.koperasi.helper.PembatalanTransaksiUtil"%>
<%!
    private static boolean ada(String s) {
        return s != null && s.trim().length() > 0;
    }
%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
Transaction tx = null;
try {
    Tbmuser u = Common.getCurrentUser(request);
    if (u == null || u.getUserId() == null) {
        result.put("status", "01");
        result.put("message", "Sesi berakhir.");
        out.print(result.toString());
        return;
    }

    Pedagang pedagang = u.getPedagang();
    Toko tokoLogin = pedagang == null ? null : pedagang.getToko();
    boolean bolehSupervisor = pedagang == null || Boolean.TRUE.equals(pedagang.getSupervisor());
    if (!bolehSupervisor) {
        result.put("status", "03");
        result.put("message", "Hanya supervisor/admin yang boleh membatalkan transaksi.");
        out.print(result.toString());
        return;
    }

    String aksi = request.getParameter("aksi");
    if (!"batal_transaksi".equals(aksi)) {
        result.put("status", "98");
        result.put("message", "Aksi tidak dikenal.");
        out.print(result.toString());
        return;
    }

    String idS = request.getParameter("id");
    String alasan = request.getParameter("alasan");
    if (!ada(idS)) {
        result.put("status", "02");
        result.put("message", "ID transaksi wajib diisi.");
        out.print(result.toString());
        return;
    }
    if (!ada(alasan)) {
        result.put("status", "02");
        result.put("message", "Alasan pembatalan wajib diisi.");
        out.print(result.toString());
        return;
    }

    Long idTransaksi = Long.valueOf(idS.trim());
    Session session = HibernateUtil.currentNativeSession();
    PembelianAnggotaKoperasi trx = (PembelianAnggotaKoperasi) session.get(PembelianAnggotaKoperasi.class, idTransaksi);

    if (trx == null) {
        Pembelian pembelianLegacy = (Pembelian) session.createCriteria(Pembelian.class)
                .add(Restrictions.idEq(idTransaksi))
                .add(Restrictions.isNull("pembelianAnggotaKoperasi"))
                .uniqueResult();
        if (pembelianLegacy == null) {
            result.put("status", "04");
            result.put("message", "Transaksi tidak ditemukan.");
            out.print(result.toString());
            return;
        }
        if (tokoLogin != null && (pembelianLegacy.getToko() == null || pembelianLegacy.getToko().getId() == null
                || !tokoLogin.getId().equals(pembelianLegacy.getToko().getId()))) {
            result.put("status", "03");
            result.put("message", "Transaksi ini bukan milik toko yang sedang login.");
            out.print(result.toString());
            return;
        }

        tx = session.beginTransaction();
        session.delete(pembelianLegacy);
        tx.commit();
        result.put("status", "00");
        result.put("message", "Transaksi lama tanpa header berhasil dibatalkan.");
        out.print(result.toString());
        return;
    }

    if (tokoLogin != null && (trx.getToko() == null || trx.getToko().getId() == null
            || !tokoLogin.getId().equals(trx.getToko().getId()))) {
        result.put("status", "03");
        result.put("message", "Transaksi ini bukan milik toko yang sedang login.");
        out.print(result.toString());
        return;
    }

    if (trx.getPostingHistory() != null) {
        result.put("status", "03");
        result.put("message", "Transaksi sudah diposting ke jurnal, tidak bisa dibatalkan dari menu ini.");
        out.print(result.toString());
        return;
    }

    tx = session.beginTransaction();
    PembatalanTransaksiUtil.batalkan(session, trx, alasan.trim());
    tx.commit();

    result.put("status", "00");
    result.put("message", "Transaksi berhasil dibatalkan dan tercatat di arsip pembatalan.");
} catch (Exception e) {
    if (tx != null) {
        try { tx.rollback(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) riwayat_transaksi_service.rollback"); }
    }
    e.printStackTrace();
    ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/riwayat_transaksi_service.jsp");
    try {
        result.put("status", "99");
        result.put("message", "Error: " + e.getMessage());
    } catch (Exception ignore) {
        ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) riwayat_transaksi_service.result");
    }
}
out.print(result.toString());
%>
