<%@page session="false"%>
<%--
  Endpoint JSON Jadwal Stock Opname (SesiStokOpname). Aksi: toko, list, simpan, hapus.
  Sesi currentSession() (tak ditutup). Hanya admin (bukan pedagang) yang boleh mengubah.
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
<%@page import="ais.database.model.inventory.SesiStokOpname"%>
<%!
    private static boolean ada(String s){ return s!=null && s.trim().length()>0; }
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
    // aksi (create/update/delete) khusus menu "jadwalopname". Perilaku lama (bolehKelola) tetap
    // selalu boleh; ini hanya MENAMBAH pintu masuk baru, tidak mempersempit.
    ais.database.model.Tbmrole roleOpname = u.hakAkses();
    org.json.JSONObject ebisnisMenuOpname = roleOpname == null ? null
        : ais.common.EbisnisMenuKatalog.urai(roleOpname.getEbisnisMenu());
    String aksi = request.getParameter("aksi");
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    Session session = HibernateUtil.currentNativeSession();
    Long tokoId=null; boolean lockToko=false;
    if (u.getPedagang()!=null && u.getPedagang().getToko()!=null){ tokoId=u.getPedagang().getToko().getId(); lockToko=true; }

    if ("toko".equals(aksi)) {
        SQLQuery q = session.createSQLQuery("select id, nama from koperasi.toko order by nama asc"); q.setMaxResults(2000);
        JSONArray arr = new JSONArray();
        for (Object o : q.list()){ Object[] a=(Object[])o; JSONObject j=new JSONObject(); j.put("id",a[0]); j.put("nama",a[1]==null?"":a[1].toString()); arr.put(j); }
        result.put("status","00"); result.put("data",arr); result.put("lockToko",lockToko);

    } else if ("list".equals(aksi)) {
        Criteria c = session.createCriteria(SesiStokOpname.class);
        if (lockToko) c.add(Restrictions.eq("toko.id", tokoId));
        c.addOrder(Order.desc("tanggalRencana")).addOrder(Order.desc("id")).setMaxResults(300);
        JSONArray arr = new JSONArray();
        for (Object o : c.list()){ SesiStokOpname s=(SesiStokOpname)o; JSONObject j=new JSONObject();
            j.put("id", s.getId()); j.put("kode", s.getKode()==null?"":s.getKode());
            j.put("toko", s.getToko()!=null && s.getToko().getNama()!=null ? s.getToko().getNama() : "-");
            j.put("tokoId", s.getToko()!=null?s.getToko().getId():null);
            j.put("rencana", s.getTanggalRencana()==null?"":DF.format(s.getTanggalRencana()));
            j.put("kategori", s.getKategori()==null?"":s.getKategori()); j.put("petugas", s.getPetugas()==null?"":s.getPetugas());
            j.put("status", s.getStatus()); j.put("keterangan", s.getKeterangan()==null?"":s.getKeterangan());
            arr.put(j);
        }
        result.put("status","00"); result.put("data",arr);

    } else if ("simpan".equals(aksi)) {
        boolean adaIdOpname = ada(request.getParameter("id"));
        boolean bolehCrudOpname = ebisnisMenuOpname != null && ais.common.EbisnisMenuKatalog.bolehAksi(
            ebisnisMenuOpname, "jadwalopname", adaIdOpname ? "update" : "create");
        if (!boleh && !bolehCrudOpname){ result.put("status","03"); result.put("message","Hanya admin yang boleh mengubah."); out.print(result.toString()); return; }
        Long tId = tokoId;
        if (!lockToko){ String tp=request.getParameter("tokoId"); if (ada(tp)){ try { tId=Long.valueOf(tp.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/opname/service.jsp:62");} } }
        String idS=request.getParameter("id"); Long id=ada(idS)?Long.valueOf(idS.trim()):null;
        SesiStokOpname s = (id==null)? new SesiStokOpname() : (SesiStokOpname) session.get(SesiStokOpname.class, id);
        if (s==null) s=new SesiStokOpname();
        if (tId!=null) s.setToko((Toko) session.get(Toko.class, tId));
        s.setKode(request.getParameter("kode"));
        String tg=request.getParameter("rencana"); Date tgl=new Date();
        if (ada(tg)){ try { tgl=new SimpleDateFormat("yyyy-MM-dd").parse(tg.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/opname/service.jsp:69");} }
        s.setTanggalRencana(tgl);
        s.setKategori(request.getParameter("kategori")); s.setPetugas(request.getParameter("petugas"));
        String st=request.getParameter("status"); s.setStatus(ada(st)?st:SesiStokOpname.STATUS_RENCANA);
        s.setKeterangan(request.getParameter("keterangan"));
        if (id==null){ s.setOleh(u.getUserNama()!=null?u.getUserNama():String.valueOf(u.getUserId())); s.setOlehId(String.valueOf(u.getUserId())); }
        Common.refreshSaveOrUpdate(session, s);
        result.put("status","00"); result.put("message","Jadwal tersimpan.");

    } else if ("hapus".equals(aksi)) {
        boolean bolehCrudOpnameHapus = ebisnisMenuOpname != null
            && ais.common.EbisnisMenuKatalog.bolehAksi(ebisnisMenuOpname, "jadwalopname", "delete");
        if (!boleh && !bolehCrudOpnameHapus){ result.put("status","03"); result.put("message","Hanya admin yang boleh menghapus."); out.print(result.toString()); return; }
        String idS=request.getParameter("id");
        if (ada(idS)){ SesiStokOpname s=(SesiStokOpname) session.get(SesiStokOpname.class, Long.valueOf(idS.trim()));
            if (s!=null && (!lockToko || (s.getToko()!=null && s.getToko().getId().equals(tokoId)))) Common.refreshDelete(session, s); }
        result.put("status","00"); result.put("message","Data dihapus.");

    } else { result.put("status","98"); result.put("message","Aksi tidak dikenal."); }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/opname/service.jsp:87");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/opname/service.jsp:88");}
}
out.print(result.toString());
%>
