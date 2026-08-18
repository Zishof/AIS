<%@page session="false"%>
<%@page import="java.util.*"%>
<%@page import="org.json.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.asset.PenerimaanPengadaanMasterAsset"%>
<%@page import="ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail"%>
<%@page import="ais.database.model.asset.SaldoAwalMasterAsset"%>
<%@page import="ais.database.model.asset.SaldoAwalMasterAssetDetail"%>
<%@page import="ais.database.model.asset.MasterAsset"%>
<%@page import="ais.action.master.asset.SaldoAwalMasterAssetAction"%>
<%@page import="ais.action.master.asset.helper.SaldoAwalMasterAssetDetailAction"%>
<%@page import="ais.action.master.inventory.KantinAssetSyncUtil"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%!
    static String s(Object o){ return o==null?"":o.toString(); }
%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
try {
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        result.put("status","01"); result.put("message", Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali."));
        out.print(result.toString()); return;
    }
    String aksi = request.getParameter("aksi");
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    Session session = HibernateUtil.currentNativeSession();
    // Pedagang/toko dikunci ke tokonya sendiri; hanya admin-kantin (tanpa pedagang) boleh memilih toko.
    ais.database.model.inventory.Toko scopeToko = (tbmuser.getPedagang() != null) ? tbmuser.getPedagang().getToko() : null;
    boolean lockToko = (scopeToko != null);

    if ("list".equals(aksi)) {
        // Item dari BAST yang SUDAH disetujui -> kandidat "Draft Inventaris".
        String q = request.getParameter("q");
        String fStatus = request.getParameter("status"); // semua/draft/selesai
        Criteria c = session.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
                .createAlias("penerimaanPengadaanMasterAsset", "p")
                .createAlias("masterAsset", "m")
                .add(Restrictions.isNotNull("p.disetujuiOleh"))
                .addOrder(Order.desc("id")).setMaxResults(500);
        if (lockToko) c.add(Restrictions.eq("p.toko", scopeToko));
        if (q!=null && !q.trim().isEmpty()) c.add(Restrictions.or(
                Restrictions.ilike("m.nama", q.trim(), org.hibernate.criterion.MatchMode.ANYWHERE),
                Restrictions.ilike("m.kode", q.trim(), org.hibernate.criterion.MatchMode.ANYWHERE)));
        // Pra-muat penanda persediaan yg SUDAH masuk kulakan (1 query, hindari N+1).
        Set<Long> syncedIds = new HashSet<Long>();
        for (Object mr : session.createSQLQuery("SELECT keterangan FROM koperasi.pengadaan_produk WHERE keterangan LIKE 'AUTO-BAST#%'").list()) {
            String k = mr == null ? "" : mr.toString();
            try { syncedIds.add(Long.valueOf(k.substring(KantinAssetSyncUtil.MARKER_PREFIX.length()).trim())); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/draft_inventaris_service.jsp:59");}
        }
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) {
            PenerimaanPengadaanMasterAssetDetail dt = (PenerimaanPengadaanMasterAssetDetail) o;
            MasterAsset ma = dt.getMasterAsset();
            if (ma == null) continue;
            String tipe = s(ma.getTipe());
            boolean inventaris = MasterAsset.TIPE_TIDAK_HABIS_PAKAI.equalsIgnoreCase(tipe);
            boolean selesai;
            if (inventaris) {
                SaldoAwalMasterAssetDetail sd = dt.getSaldoAwalMasterAssetDetail();
                selesai = sd != null && sd.getAsset() != null;
            } else {
                selesai = syncedIds.contains(dt.getId());
            }
            if ("draft".equals(fStatus) && selesai) continue;
            if ("selesai".equals(fStatus) && !selesai) continue;
            JSONObject j = new JSONObject();
            j.put("id", dt.getId());
            j.put("bast", dt.getPenerimaanPengadaanMasterAsset()==null?"":s(dt.getPenerimaanPengadaanMasterAsset().getKode()));
            j.put("barang", (ma.getKode()==null?"":s(ma.getKode())+" - ") + s(ma.getNama()));
            j.put("qty", dt.getDiterima()==null?0.0:dt.getDiterima());
            j.put("kategori", inventaris ? "inventaris" : "persediaan");
            j.put("tipe", tipe);
            j.put("selesai", selesai);
            arr.put(j);
        }
        result.put("data", arr); result.put("status","00");

    } else if ("jadikanPersediaan".equals(aksi)) {
        // Barang habis pakai -> dorong masuk ke kulakan (pengadaan_produk). REUSE util (plain Hibernate).
        Long id = Long.parseLong(request.getParameter("id").trim());
        Session ns = null; Transaction tx = null;
        try {
            ns = HibernateUtil.openSession();
            tx = ns.beginTransaction();
            PenerimaanPengadaanMasterAssetDetail dt = (PenerimaanPengadaanMasterAssetDetail) ns.get(PenerimaanPengadaanMasterAssetDetail.class, id);
            if (dt == null) { tx.rollback(); result.put("status","02"); result.put("message","Data tidak ditemukan"); out.print(result.toString()); return; }
            KantinAssetSyncUtil.syncPengadaanDariBast(ns, dt);
            tx.commit();
            result.put("status","00"); result.put("message","Barang dijadikan persediaan & masuk ke data kulakan.");
        } catch (Exception ex) {
            if (tx != null) { try { tx.rollback(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/draft_inventaris_service.jsp:102");} }
            throw ex;
        } finally {
            if (ns != null) {
                try { ns.clear(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/draft_inventaris_service.jsp:106");}
                try { ns.disconnect(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/draft_inventaris_service.jsp:107");}
                try { ns.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/draft_inventaris_service.jsp:108");}
            }
        }

    } else if ("jadikanInventaris".equals(aksi)) {
        // Barang tidak habis pakai -> jadikan Inventaris/Sarpras (buat Asset + AssetDetail).
        // Mirror PenerimaanPengadaanMasterAssetDetailAction.tampilInventaris: pastikan SaldoAwal +
        // SaldoAwalDetail, lalu REUSE pindahkanMenjadiBarangInventaris (logika asset asli, tak ditulis ulang).
        Long id = Long.parseLong(request.getParameter("id").trim());
        Session ns = null;
        try {
            ns = HibernateUtil.currentNativeSession();
            PenerimaanPengadaanMasterAssetDetail dt = (PenerimaanPengadaanMasterAssetDetail) ns.get(PenerimaanPengadaanMasterAssetDetail.class, id);
            if (dt == null) { result.put("status","02"); result.put("message","Data tidak ditemukan"); out.print(result.toString()); return; }
            PenerimaanPengadaanMasterAsset pen = dt.getPenerimaanPengadaanMasterAsset();

            Transaction tx = ns.beginTransaction();
            SaldoAwalMasterAsset sa = pen.getSaldoAwalMasterAsset();
            if (sa == null) {
                sa = new SaldoAwalMasterAsset();
                sa.setKode(SaldoAwalMasterAssetAction.generateCode(WaktuUtil.getDate(), true));
                sa.setPenerimaanPengadaanMasterAsset(pen);
                ns.save(sa); ns.flush();
                pen.setSaldoAwalMasterAsset(sa);
                ns.update(pen); ns.flush();
            }
            SaldoAwalMasterAssetDetail sd = dt.getSaldoAwalMasterAssetDetail();
            if (sd == null) {
                sd = (SaldoAwalMasterAssetDetail) ns.createCriteria(SaldoAwalMasterAssetDetail.class)
                        .add(Restrictions.eq("masterAsset", dt.getMasterAsset()))
                        .add(Restrictions.eq("saldoAwal", sa)).setMaxResults(1).uniqueResult();
            }
            if (sd == null) {
                sd = new SaldoAwalMasterAssetDetail();
                sd.setPenerimaanPengadaanMasterAssetDetail(dt);
                sd.setSaldoAwal(sa);
                ns.save(sd); ns.flush();
                dt.setSaldoAwalMasterAssetDetail(sd);
                ns.update(dt); ns.flush();
            }
            tx.commit();

            // REUSE: buat Asset + AssetDetail (mengelola native session-nya sendiri).
            SaldoAwalMasterAssetDetailAction.pindahkanMenjadiBarangInventaris(sd);

            result.put("status","00"); result.put("message","Barang dijadikan Inventaris/Sarpras (Asset dibuat).");
        } catch (Exception ex) {
            throw ex;
        } finally {
            // currentNativeSession sudah ditutup oleh pindahkan; tutup defensif bila masih terbuka.
            if (ns != null && ns.isOpen()) {
                try { ns.clear(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/draft_inventaris_service.jsp:159");}
                try { ns.disconnect(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/draft_inventaris_service.jsp:160");}
                try { ns.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/draft_inventaris_service.jsp:161");}
            }
        }

    } else { result.put("status","98"); result.put("message","Aksi tidak dikenal."); }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/kulakan/draft_inventaris_service.jsp:167");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/draft_inventaris_service.jsp:168");}
}
out.print(result.toString());
%>
