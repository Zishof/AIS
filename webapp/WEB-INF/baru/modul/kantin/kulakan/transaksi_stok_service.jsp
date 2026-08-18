<%@page session="false"%>
<%--
  Endpoint transaksi BARU (JSON) untuk Kulakan: Pemakaian Bahan Baku & Retur Barang.
  param jenis = pemakaian | retur. Aksi: produk (daftar produk toko), list (riwayat),
  simpan (catat transaksi), hapus. Entity: PemakaianBahanBaku / ReturBarang (skema koperasi).
  Pemakaian mengurangi stok -> recompute via StokKantinUtil.recomputeStokProdukViaSql.
  Sesi currentSession() (tak ditutup). Toko DIPAKSA ke toko pedagang bila login pedagang.
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
<%@page import="ais.database.model.inventory.Produk"%>
<%@page import="ais.database.model.inventory.PemakaianBahanBaku"%>
<%@page import="ais.database.model.inventory.ReturBarang"%>
<%@page import="ais.action.master.inventory.StokKantinUtil"%>
<%!
    private static boolean ada(String s) { return s != null && s.trim().length() > 0; }
%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
try {
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        result.put("status", "01"); result.put("message", "Sesi berakhir, silakan masuk kembali.");
        out.print(result.toString()); return;
    }
    String aksi = request.getParameter("aksi");
    String jenis = request.getParameter("jenis");
    boolean retur = "retur".equals(jenis);
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    Session session = HibernateUtil.currentNativeSession();

    // lingkup toko: pedagang dikunci
    Long tokoId = null; boolean lockToko = false;
    if (tbmuser.getPedagang() != null && tbmuser.getPedagang().getToko() != null) {
        tokoId = tbmuser.getPedagang().getToko().getId(); lockToko = true;
    } else {
        String tp = request.getParameter("tokoId");
        if (ada(tp)) { try { tokoId = Long.valueOf(tp.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/transaksi_stok_service.jsp:48");} }
    }

    if ("produk".equals(aksi)) {
        StringBuilder sb = new StringBuilder("select id, kode, nama, coalesce(hargabeli,0), coalesce(hargajual,0), coalesce(stok,0) from koperasi.produk where aktif=true ");
        if (tokoId != null) sb.append(" and toko = :t ");
        sb.append(" order by nama asc ");
        SQLQuery q = session.createSQLQuery(sb.toString());
        if (tokoId != null) q.setParameter("t", tokoId);
        q.setMaxResults(3000);
        JSONArray arr = new JSONArray();
        for (Object o : q.list()) {
            Object[] a = (Object[]) o; JSONObject j = new JSONObject();
            j.put("id", a[0]); j.put("kode", a[1]==null?"":a[1].toString()); j.put("nama", a[2]==null?"":a[2].toString());
            j.put("hargabeli", ((Number)a[3]).doubleValue()); j.put("hargajual", ((Number)a[4]).doubleValue()); j.put("stok", ((Number)a[5]).doubleValue());
            arr.put(j);
        }
        result.put("status","00"); result.put("data", arr);

    } else if ("list".equals(aksi)) {
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        JSONArray arr = new JSONArray();
        if (retur) {
            StringBuilder sb = new StringBuilder("select x.id, x.waktu, p.kode, p.nama, coalesce(x.qty,0), coalesce(x.hargasatuan,0), coalesce(x.total, x.qty*x.hargasatuan,0), coalesce(x.keterangan,'') "
                + " from koperasi.retur_barang x left join koperasi.produk p on p.id=x.produk where 1=1 ");
            if (tokoId != null) sb.append(" and x.toko = :t ");
            sb.append(" order by x.waktu desc, x.id desc ");
            SQLQuery q = session.createSQLQuery(sb.toString());
            if (tokoId != null) q.setParameter("t", tokoId);
            q.setMaxResults(100);
            for (Object o : q.list()) {
                Object[] a = (Object[]) o; JSONObject j = new JSONObject();
                j.put("id", a[0]); j.put("waktu", a[1]==null?"":df.format((java.util.Date)a[1]));
                j.put("kode", a[2]==null?"":a[2].toString()); j.put("nama", a[3]==null?"":a[3].toString());
                j.put("qty", ((Number)a[4]).doubleValue()); j.put("harga", ((Number)a[5]).doubleValue()); j.put("total", ((Number)a[6]).doubleValue());
                j.put("keterangan", a[7]==null?"":a[7].toString());
                arr.put(j);
            }
        } else {
            StringBuilder sb = new StringBuilder("select x.id, x.waktu, p.kode, p.nama, coalesce(x.qty,0), coalesce(x.keterangan,'') "
                + " from koperasi.pemakaian_bahan_baku x left join koperasi.produk p on p.id=x.produk where 1=1 ");
            if (tokoId != null) sb.append(" and x.toko = :t ");
            sb.append(" order by x.waktu desc, x.id desc ");
            SQLQuery q = session.createSQLQuery(sb.toString());
            if (tokoId != null) q.setParameter("t", tokoId);
            q.setMaxResults(100);
            for (Object o : q.list()) {
                Object[] a = (Object[]) o; JSONObject j = new JSONObject();
                j.put("id", a[0]); j.put("waktu", a[1]==null?"":df.format((java.util.Date)a[1]));
                j.put("kode", a[2]==null?"":a[2].toString()); j.put("nama", a[3]==null?"":a[3].toString());
                j.put("qty", ((Number)a[4]).doubleValue()); j.put("keterangan", a[5]==null?"":a[5].toString());
                arr.put(j);
            }
        }
        result.put("status","00"); result.put("data", arr);

    } else if ("simpan".equals(aksi)) {
        String pid = request.getParameter("produkId");
        if (!ada(pid)) { result.put("status","02"); result.put("message","Produk belum dipilih."); out.print(result.toString()); return; }
        Produk produk = (Produk) session.get(Produk.class, Long.valueOf(pid.trim()));
        if (produk == null) { result.put("status","02"); result.put("message","Produk tidak ditemukan."); out.print(result.toString()); return; }
        Toko toko = null;
        if (lockToko) { toko = tbmuser.getPedagang().getToko(); }
        else if (tokoId != null) { toko = (Toko) session.get(Toko.class, tokoId); }
        if (toko == null) { result.put("status","02"); result.put("message","Toko/kios belum dipilih."); out.print(result.toString()); return; }

        double qty = 0.0; try { qty = Double.parseDouble(request.getParameter("qty")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/transaksi_stok_service.jsp:114");}
        if (qty <= 0) { result.put("status","03"); result.put("message","Jumlah (qty) harus lebih dari 0."); out.print(result.toString()); return; }
        String ket = request.getParameter("keterangan");
        String tgl = request.getParameter("tanggal");
        java.util.Date waktu = new java.util.Date();
        if (ada(tgl)) { try { waktu = new SimpleDateFormat("yyyy-MM-dd").parse(tgl.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/transaksi_stok_service.jsp:119");} }
        String oleh = tbmuser.getUserNama() != null ? tbmuser.getUserNama() : (tbmuser.getUserId()+"");

        if (retur) {
            double harga = 0.0; try { harga = Double.parseDouble(request.getParameter("harga")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/transaksi_stok_service.jsp:123");}
            ReturBarang o = new ReturBarang();
            o.setProduk(produk); o.setToko(toko); o.setQty(qty); o.setHargaSatuan(harga);
            o.setTotal(qty * harga); o.setKeterangan(ket); o.setWaktu(waktu); o.setOleh(oleh);
            Common.refreshSaveOrUpdate(session, o);
        } else {
            PemakaianBahanBaku o = new PemakaianBahanBaku();
            o.setProduk(produk); o.setToko(toko); o.setQty(qty); o.setKeterangan(ket); o.setWaktu(waktu); o.setOleh(oleh);
            Common.refreshSaveOrUpdate(session, o);
            try { StokKantinUtil.recomputeStokProdukViaSql(produk.getId()); } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/kulakan/transaksi_stok_service.jsp:132"); }
        }
        result.put("status","00"); result.put("message","Transaksi tersimpan.");

    } else if ("hapus".equals(aksi)) {
        String id = request.getParameter("id");
        if (!ada(id)) { result.put("status","02"); result.put("message","ID kosong."); out.print(result.toString()); return; }
        Long lid = Long.valueOf(id.trim());
        if (retur) {
            ReturBarang o = (ReturBarang) session.get(ReturBarang.class, lid);
            if (o != null && (!lockToko || (o.getToko()!=null && o.getToko().getId().equals(tokoId)))) {
                Common.refreshDelete(session, o);
            }
        } else {
            PemakaianBahanBaku o = (PemakaianBahanBaku) session.get(PemakaianBahanBaku.class, lid);
            if (o != null && (!lockToko || (o.getToko()!=null && o.getToko().getId().equals(tokoId)))) {
                Long prodId = o.getProduk()!=null ? o.getProduk().getId() : null;
                Common.refreshDelete(session, o);
                if (prodId != null) { try { StokKantinUtil.recomputeStokProdukViaSql(prodId); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/transaksi_stok_service.jsp:150");} }
            }
        }
        result.put("status","00"); result.put("message","Data dihapus.");

    } else {
        result.put("status","98"); result.put("message","Aksi tidak dikenal.");
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/kulakan/transaksi_stok_service.jsp:159");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/kulakan/transaksi_stok_service.jsp:160");}
}
out.print(result.toString());
%>
