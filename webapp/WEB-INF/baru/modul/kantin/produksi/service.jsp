<%@page session="false"%>
<%--
  Endpoint JSON Produksi Kantin (ProduksiKantin). Aksi: produk (menu), list, simpan, hapus.
  Sesi currentSession(). Hanya admin (bukan pedagang). Toko dipaksa bila pedagang.
--%>
<%@page import="org.json.*"%>
<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.SQLQuery"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Produk"%>
<%@page import="ais.database.model.inventory.ProduksiKantin"%>
<%!
    private static boolean ada(String s){ return s!=null && s.trim().length()>0; }
    private static double d(String s){ try { return Double.parseDouble(s.trim()); } catch(Exception e){ return 0.0; } }
%>
<%
response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
SimpleDateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
try {
    Tbmuser u = Common.getCurrentUser(request);
    if (u==null || u.getUserId()==null){ result.put("status","01"); result.put("message","Sesi berakhir."); out.print(result.toString()); return; }
    boolean boleh = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
    // Gerbang tambahan (OR, bukan pengganti): role (Tbmrole) juga bisa diberi izin granular per
    // aksi (create/update/delete) khusus menu "produksi". Perilaku lama (bolehKelola) tetap selalu
    // boleh; ini hanya MENAMBAH pintu masuk baru, tidak mempersempit.
    ais.database.model.Tbmrole roleProduksi = u.hakAkses();
    org.json.JSONObject ebisnisMenuProduksi = roleProduksi == null ? null
        : ais.common.EbisnisMenuKatalog.urai(roleProduksi.getEbisnisMenu());
    String aksi = request.getParameter("aksi");
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    Session session = HibernateUtil.currentNativeSession();
    Long tokoId=null; boolean lockToko=false;
    if (u.getPedagang()!=null && u.getPedagang().getToko()!=null){ tokoId=u.getPedagang().getToko().getId(); lockToko=true; }

    if ("produk".equals(aksi)) {
        StringBuilder sb=new StringBuilder("select id, kode, nama from koperasi.produk where aktif=true ");
        if (lockToko) sb.append(" and toko=").append(tokoId);
        sb.append(" order by nama asc ");
        SQLQuery q=session.createSQLQuery(sb.toString()); q.setMaxResults(5000);
        JSONArray arr=new JSONArray();
        for (Object o : q.list()){ Object[] a=(Object[])o; JSONObject j=new JSONObject(); j.put("id",a[0]); j.put("nama",(a[1]==null?"":a[1].toString()+" - ")+(a[2]==null?"":a[2].toString())); arr.put(j); }
        result.put("status","00"); result.put("data",arr);

    } else if ("list".equals(aksi)) {
        Criteria c=session.createCriteria(ProduksiKantin.class);
        if (lockToko) c.add(Restrictions.eq("toko.id", tokoId));
        c.addOrder(Order.desc("tanggal")).addOrder(Order.desc("id")).setMaxResults(300);
        JSONArray arr=new JSONArray();
        for (Object o : c.list()){ ProduksiKantin s=(ProduksiKantin)o; JSONObject j=new JSONObject();
            j.put("id", s.getId());
            j.put("menu", s.getProduk()!=null && s.getProduk().getNama()!=null ? s.getProduk().getNama() : "-");
            j.put("produkId", s.getProduk()!=null?s.getProduk().getId():null);
            j.put("tanggal", s.getTanggal()==null?"":DF.format(s.getTanggal()));
            j.put("rencana", s.getPorsiRencana()); j.put("dibuat", s.getPorsiDibuat()); j.put("terjual", s.getPorsiTerjual());
            j.put("sisa", s.getPorsiSisa()); j.put("waste", s.getPorsiWaste()); j.put("keterangan", s.getKeterangan()==null?"":s.getKeterangan());
            arr.put(j);
        }
        result.put("status","00"); result.put("data",arr);

    } else if ("simpan".equals(aksi)) {
        boolean adaIdProduksi = ada(request.getParameter("id"));
        boolean bolehCrudProduksi = ebisnisMenuProduksi != null && ais.common.EbisnisMenuKatalog.bolehAksi(
            ebisnisMenuProduksi, "produksi", adaIdProduksi ? "update" : "create");
        if (!boleh && !bolehCrudProduksi){ result.put("status","03"); result.put("message","Hanya admin yang boleh mengubah."); out.print(result.toString()); return; }
        String pid=request.getParameter("produkId");
        if (!ada(pid)){ result.put("status","02"); result.put("message","Menu wajib dipilih."); out.print(result.toString()); return; }
        String idS=request.getParameter("id"); Long id=ada(idS)?Long.valueOf(idS.trim()):null;
        ProduksiKantin s=(id==null)? new ProduksiKantin() : (ProduksiKantin) session.get(ProduksiKantin.class, id);
        if (s==null) s=new ProduksiKantin();
        s.setProduk((Produk) session.get(Produk.class, Long.valueOf(pid.trim())));
        Long tId = lockToko ? tokoId : null;
        if (!lockToko){ String tp=request.getParameter("tokoId"); if (ada(tp)){ try { tId=Long.valueOf(tp.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/produksi/service.jsp:73");} } }
        if (tId!=null) s.setToko((Toko) session.get(Toko.class, tId));
        String tg=request.getParameter("tanggal"); Date tgl=new Date();
        if (ada(tg)){ try { tgl=new SimpleDateFormat("yyyy-MM-dd").parse(tg.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/produksi/service.jsp:76");} }
        s.setTanggal(tgl);
        s.setPorsiRencana(Double.valueOf(d(request.getParameter("rencana"))));
        s.setPorsiDibuat(Double.valueOf(d(request.getParameter("dibuat"))));
        s.setPorsiTerjual(Double.valueOf(d(request.getParameter("terjual"))));
        s.setPorsiSisa(Double.valueOf(d(request.getParameter("sisa"))));
        s.setPorsiWaste(Double.valueOf(d(request.getParameter("waste"))));
        s.setKeterangan(request.getParameter("keterangan"));
        if (id==null){ s.setOleh(u.getUserNama()!=null?u.getUserNama():String.valueOf(u.getUserId())); s.setOlehId(String.valueOf(u.getUserId())); }
        Common.refreshSaveOrUpdate(session, s);
        result.put("status","00"); result.put("message","Data tersimpan.");

    } else if ("hapus".equals(aksi)) {
        boolean bolehCrudProduksiHapus = ebisnisMenuProduksi != null
            && ais.common.EbisnisMenuKatalog.bolehAksi(ebisnisMenuProduksi, "produksi", "delete");
        if (!boleh && !bolehCrudProduksiHapus){ result.put("status","03"); result.put("message","Hanya admin yang boleh menghapus."); out.print(result.toString()); return; }
        String idS=request.getParameter("id");
        if (ada(idS)){ ProduksiKantin s=(ProduksiKantin) session.get(ProduksiKantin.class, Long.valueOf(idS.trim()));
            if (s!=null && (!lockToko || (s.getToko()!=null && s.getToko().getId().equals(tokoId)))) Common.refreshDelete(session, s); }
        result.put("status","00"); result.put("message","Data dihapus.");

    } else { result.put("status","98"); result.put("message","Aksi tidak dikenal."); }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/produksi/service.jsp:97");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/produksi/service.jsp:98");}
}
out.print(result.toString());
%>
