<%@page session="false"%>
<%--
  Endpoint JSON Sesi Kas Kasir (Buka/Tutup Kas). Aksi:
    status : sesi terbuka milik kasir + hitung tunai/non-tunai POS sejak buka.
    buka   : buka kas (modal awal).  tutup : tutup kas (input uang fisik -> selisih).  list : riwayat.
  Tunai/non-tunai dihitung dari koperasi.pembelian_anggota_koperasi (oleh = kasir) dalam rentang sesi.
  Sesi currentSession() (tak ditutup). Toko dipaksa ke toko pedagang bila login pedagang.
--%>
<%@page import="org.json.*"%>
<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.SQLQuery"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.SesiKasKasir"%>
<%!
    private static boolean ada(String s) { return s != null && s.trim().length() > 0; }
    private static double d(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; } }
%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
SimpleDateFormat DF = new SimpleDateFormat("dd-MM-yyyy HH:mm");
try {
    Tbmuser u = Common.getCurrentUser(request);
    if (u == null || u.getUserId() == null) { result.put("status","01"); result.put("message","Sesi berakhir."); out.print(result.toString()); return; }
    // Gerbang CRUD granular per grup pengguna (Tbmrole.ebisnisMenu.crud, ais.common.EbisnisMenuKatalog)
    // khusus menu "kaskasir" -- sebelumnya endpoint ini TIDAK bergerbang sama sekali (tiap kasir login
    // bebas buka/tutup kasnya sendiri); default-allow (lihat EbisnisMenuKatalog.bolehAksi) menjaga
    // perilaku lama tetap sama utk role yang belum pernah menyimpan grid CRUD ini -- hanya role yang
    // eksplisit di-uncheck admin utk create/update "kaskasir" yang mulai ditolak di sini.
    ais.database.model.Tbmrole roleKas = u.hakAkses();
    org.json.JSONObject ebisnisMenuKas = roleKas == null ? null
        : ais.common.EbisnisMenuKatalog.urai(roleKas.getEbisnisMenu());
    String aksi = request.getParameter("aksi");
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    Session session = HibernateUtil.currentNativeSession();

    String oleh = u.getUserNama() != null ? u.getUserNama() : String.valueOf(u.getUserId());
    String olehId = String.valueOf(u.getUserId());

    Long tokoId = null; boolean lockToko = false;
    if (u.getPedagang() != null && u.getPedagang().getToko() != null) { tokoId = u.getPedagang().getToko().getId(); lockToko = true; }
    else { String tp = request.getParameter("tokoId"); if (ada(tp)) { try { tokoId = Long.valueOf(tp.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kas/service.jsp:41");} } }

    // cari sesi terbuka milik kasir ini
    StringBuilder cari = new StringBuilder("select id from koperasi.sesi_kas_kasir where coalesce(status,'BUKA')='BUKA' and (oleh=:o or olehid=:i) ");
    if (tokoId != null) cari.append(" and toko=:t ");
    cari.append(" order by id desc ");
    SQLQuery cq = session.createSQLQuery(cari.toString());
    cq.setParameter("o", oleh); cq.setParameter("i", olehId);
    if (tokoId != null) cq.setParameter("t", tokoId);
    cq.setMaxResults(1);
    Object openIdObj = cq.uniqueResult();
    Long openId = openIdObj == null ? null : Long.valueOf(openIdObj.toString());
    SesiKasKasir sesi = openId == null ? null : (SesiKasKasir) session.get(SesiKasKasir.class, openId);

    if ("buka".equals(aksi)) {
        if (ebisnisMenuKas != null && !ais.common.EbisnisMenuKatalog.bolehAksi(ebisnisMenuKas, "kaskasir", "create")) {
            result.put("status","03"); result.put("message","Anda tidak memiliki izin untuk membuka kas."); out.print(result.toString()); return;
        }
        if (sesi != null) { result.put("status","02"); result.put("message","Kas masih terbuka. Tutup dulu sebelum buka lagi."); out.print(result.toString()); return; }
        Toko toko = null;
        if (lockToko) toko = u.getPedagang().getToko();
        else if (tokoId != null) toko = (Toko) session.get(Toko.class, tokoId);
        SesiKasKasir o = new SesiKasKasir();
        o.setToko(toko); o.setOleh(oleh); o.setOlehId(olehId);
        o.setWaktuBuka(new Date()); o.setModalAwal(Double.valueOf(d(request.getParameter("modalAwal"))));
        o.setStatus(SesiKasKasir.STATUS_BUKA); o.setKeterangan(request.getParameter("keterangan"));
        Common.refreshSaveOrUpdate(session, o);
        result.put("status","00"); result.put("message","Kas dibuka.");

    } else if ("tutup".equals(aksi)) {
        if (ebisnisMenuKas != null && !ais.common.EbisnisMenuKatalog.bolehAksi(ebisnisMenuKas, "kaskasir", "update")) {
            result.put("status","03"); result.put("message","Anda tidak memiliki izin untuk menutup kas."); out.print(result.toString()); return;
        }
        if (sesi == null) { result.put("status","02"); result.put("message","Tidak ada kas terbuka."); out.print(result.toString()); return; }
        double[] jual = hitungPenjualan(session, oleh, olehId, tokoId, sesi.getWaktuBuka(), new Date());
        double uangFisik = d(request.getParameter("uangFisik"));
        double seharusnya = sesi.getModalAwal().doubleValue() + jual[0];
        sesi.setTotalTunai(Double.valueOf(jual[0])); sesi.setTotalNonTunai(Double.valueOf(jual[1]));
        sesi.setUangFisik(Double.valueOf(uangFisik)); sesi.setSelisih(Double.valueOf(uangFisik - seharusnya));
        sesi.setWaktuTutup(new Date()); sesi.setStatus(SesiKasKasir.STATUS_TUTUP);
        String ket = request.getParameter("keterangan"); if (ada(ket)) sesi.setKeterangan(ket);
        Common.refreshSaveOrUpdate(session, sesi);
        result.put("status","00"); result.put("message","Kas ditutup. Selisih: " + (uangFisik - seharusnya));

    } else if ("list".equals(aksi)) {
        StringBuilder w = new StringBuilder(" where 1=1 ");
        if (lockToko) w.append(" and k.toko=").append(tokoId);
        SQLQuery q = session.createSQLQuery("select k.oleh, k.waktubuka, k.waktututup, coalesce(k.modalawal,0), coalesce(k.totaltunai,0), coalesce(k.totalnontunai,0), coalesce(k.uangfisik,0), coalesce(k.selisih,0), coalesce(k.status,'BUKA') "
            + " from koperasi.sesi_kas_kasir k " + w + " order by k.waktubuka desc ");
        q.setMaxResults(100);
        JSONArray arr = new JSONArray();
        for (Object o : q.list()) {
            Object[] a = (Object[]) o; JSONObject j = new JSONObject();
            j.put("kasir", a[0]==null?"-":a[0].toString());
            j.put("buka", a[1]==null?"":DF.format((Date)a[1]));
            j.put("tutup", a[2]==null?"":DF.format((Date)a[2]));
            j.put("modal", ((Number)a[3]).doubleValue()); j.put("tunai", ((Number)a[4]).doubleValue());
            j.put("nontunai", ((Number)a[5]).doubleValue()); j.put("fisik", ((Number)a[6]).doubleValue());
            j.put("selisih", ((Number)a[7]).doubleValue()); j.put("stat", a[8]==null?"BUKA":a[8].toString());
            arr.put(j);
        }
        result.put("status","00"); result.put("data", arr);

    } else { // status
        JSONObject s = new JSONObject();
        if (sesi == null) { s.put("buka", false); }
        else {
            double[] jual = hitungPenjualan(session, oleh, olehId, tokoId, sesi.getWaktuBuka(), new Date());
            s.put("buka", true); s.put("id", sesi.getId());
            s.put("waktuBuka", DF.format(sesi.getWaktuBuka())); s.put("modal", sesi.getModalAwal());
            s.put("tunai", jual[0]); s.put("nontunai", jual[1]);
            s.put("seharusnya", sesi.getModalAwal().doubleValue() + jual[0]);
        }
        result.put("status","00"); result.put("sesi", s);
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/kas/service.jsp:111");
    try { result.put("status","99"); result.put("message","Error: " + e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kas/service.jsp:112");}
}
out.print(result.toString());
%>
<%!
    /** Hitung [tunai, nontunai] penjualan POS oleh kasir dlm rentang waktu. */
    private double[] hitungPenjualan(Session session, String oleh, String olehId, Long tokoId, Date dari, Date sampai) {
        try {
            StringBuilder sb = new StringBuilder("select coalesce(sum(coalesce(bayar_tunai,0)),0), coalesce(sum(coalesce(bayar_non_tunai,0)),0) "
                + " from koperasi.pembelian_anggota_koperasi where (oleh=:o or oleh=:i) and tanggal_pembayaran >= :dari and tanggal_pembayaran <= :sampai ");
            if (tokoId != null) sb.append(" and toko=:t ");
            SQLQuery q = session.createSQLQuery(sb.toString());
            q.setParameter("o", oleh); q.setParameter("i", olehId);
            q.setParameter("dari", dari); q.setParameter("sampai", sampai);
            if (tokoId != null) q.setParameter("t", tokoId);
            Object[] r = (Object[]) q.uniqueResult();
            return new double[]{ ((Number)r[0]).doubleValue(), ((Number)r[1]).doubleValue() };
        } catch (Exception e) { return new double[]{0,0}; }
    }
%>
