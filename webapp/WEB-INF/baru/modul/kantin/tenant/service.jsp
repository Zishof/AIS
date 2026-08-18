<%@page session="false"%>
<%--
  Endpoint JSON Setoran & Bagi Hasil Tenant. Aksi: toko (daftar tenant/toko), list (riwayat setoran),
  simpan (tambah/ubah), hapus. Logika lewat TenantSetoranUtil (dipakai bersama versi ZK).
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
<%@page import="ais.database.model.inventory.SetoranTenant"%>
<%@page import="ais.action.master.koperasi.helper.TenantSetoranUtil"%>
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
    // aksi (create/update/delete) khusus menu "setorantenant". Perilaku lama (bolehKelola) tetap
    // selalu boleh; ini hanya MENAMBAH pintu masuk baru, tidak mempersempit.
    ais.database.model.Tbmrole roleTenant = u.hakAkses();
    org.json.JSONObject ebisnisMenuTenant = roleTenant == null ? null
        : ais.common.EbisnisMenuKatalog.urai(roleTenant.getEbisnisMenu());
    String aksi = request.getParameter("aksi");
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    Session session = HibernateUtil.currentNativeSession();

    Long tokoId = null; boolean lockToko=false;
    if (u.getPedagang()!=null && u.getPedagang().getToko()!=null){ tokoId=u.getPedagang().getToko().getId(); lockToko=true; }

    if ("toko".equals(aksi)) {
        SQLQuery q = session.createSQLQuery("select id, nama from koperasi.toko order by nama asc");
        q.setMaxResults(2000);
        JSONArray arr = new JSONArray();
        for (Object o : q.list()){ Object[] a=(Object[])o; JSONObject j=new JSONObject(); j.put("id",a[0]); j.put("nama",a[1]==null?"":a[1].toString()); arr.put(j); }
        result.put("status","00"); result.put("data",arr); result.put("lockToko",lockToko);

    } else if ("list".equals(aksi)) {
        List<SetoranTenant> ls = TenantSetoranUtil.daftar(session, lockToko?tokoId:null, 300);
        JSONArray arr = new JSONArray();
        for (SetoranTenant s : ls){
            JSONObject j = new JSONObject();
            j.put("id", s.getId());
            j.put("tenant", s.getToko()!=null && s.getToko().getNama()!=null ? s.getToko().getNama() : "-");
            j.put("tokoId", s.getToko()!=null ? s.getToko().getId() : null);
            j.put("tanggal", s.getTanggal()==null?"":DF.format(s.getTanggal()));
            j.put("periode", s.getPeriode()==null?"":s.getPeriode());
            j.put("omzet", s.getOmzet()); j.put("persen", s.getPersenBagiHasil());
            j.put("bagihasil", s.getNilaiBagiHasil()); j.put("sewa", s.getSewa()); j.put("biaya", s.getBiayaLayanan());
            j.put("kewajiban", TenantSetoranUtil.kewajiban(s)); j.put("setoran", s.getSetoran()); j.put("status", s.getStatus());
            j.put("keterangan", s.getKeterangan()==null?"":s.getKeterangan());
            arr.put(j);
        }
        result.put("status","00"); result.put("data",arr);

    } else if ("simpan".equals(aksi)) {
        boolean adaIdTenant = ada(request.getParameter("id"));
        boolean bolehCrudTenant = ebisnisMenuTenant != null && ais.common.EbisnisMenuKatalog.bolehAksi(
            ebisnisMenuTenant, "setorantenant", adaIdTenant ? "update" : "create");
        if (!boleh && !bolehCrudTenant){ result.put("status","03"); result.put("message","Hanya admin yang boleh mengubah data ini."); out.print(result.toString()); return; }
        Long tId = tokoId;
        if (!lockToko){ String tp=request.getParameter("tokoId"); if (ada(tp)){ try { tId=Long.valueOf(tp.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/tenant/service.jsp:66");} } }
        if (tId==null){ result.put("status","02"); result.put("message","Tenant/Toko wajib dipilih."); out.print(result.toString()); return; }
        Toko toko = (Toko) session.get(Toko.class, tId);
        String idS = request.getParameter("id"); Long id = ada(idS)?Long.valueOf(idS.trim()):null;
        Date tgl = new Date(); String tg=request.getParameter("tanggal");
        if (ada(tg)){ try { tgl=new SimpleDateFormat("yyyy-MM-dd").parse(tg.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/tenant/service.jsp:71");} }
        String oleh = u.getUserNama()!=null?u.getUserNama():String.valueOf(u.getUserId());
        TenantSetoranUtil.simpan(session, id, toko, tgl, request.getParameter("periode"),
            d(request.getParameter("omzet")), d(request.getParameter("persen")), d(request.getParameter("bagihasil")),
            d(request.getParameter("sewa")), d(request.getParameter("biaya")), d(request.getParameter("setoran")),
            request.getParameter("keterangan"), oleh, String.valueOf(u.getUserId()));
        result.put("status","00"); result.put("message","Data tersimpan.");

    } else if ("hapus".equals(aksi)) {
        boolean bolehCrudTenantHapus = ebisnisMenuTenant != null
            && ais.common.EbisnisMenuKatalog.bolehAksi(ebisnisMenuTenant, "setorantenant", "delete");
        if (!boleh && !bolehCrudTenantHapus){ result.put("status","03"); result.put("message","Hanya admin yang boleh menghapus."); out.print(result.toString()); return; }
        String idS = request.getParameter("id");
        if (ada(idS)){
            SetoranTenant s = (SetoranTenant) session.get(SetoranTenant.class, Long.valueOf(idS.trim()));
            if (s!=null && (!lockToko || (s.getToko()!=null && s.getToko().getId().equals(tokoId)))) TenantSetoranUtil.hapus(session, s.getId());
        }
        result.put("status","00"); result.put("message","Data dihapus.");

    } else { result.put("status","98"); result.put("message","Aksi tidak dikenal."); }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/tenant/service.jsp:90");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/tenant/service.jsp:91");}
}
out.print(result.toString());
%>
