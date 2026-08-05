<%@page session="false"%>
<%--
  Endpoint JSON editor Rekening Koran (Rekonsiliasi Bank). Aksi:
    listAkun -> daftar akun bank/kas (untuk pemilih),
    list     -> baris mutasi rekening koran per akun (+ s/d tanggal),
    simpan   -> tambah/ubah 1 baris mutasi,
    hapus    -> hapus 1 baris,
    toggle   -> set/lepas tanda "sudah dicocokkan" (sudahRekon) + tanggalRekon.
  Sesi currentSession(). Hanya admin (LokasiKantinUtil.bolehKelola). Toko dipaksa bila pedagang.
--%>
<%@page import="org.json.*"%>
<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.SQLQuery"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.MutasiRekeningKoran"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%!
    private static boolean ada(String s){ return s!=null && s.trim().length()>0; }
    private static double parseD(String s){ try { return Double.parseDouble(s); } catch(Exception e){ return 0; } }
%>
<%
response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd");
try {
    Tbmuser u = Common.getCurrentUser(request);
    if (u==null || u.getUserId()==null){ result.put("status","01"); result.put("message","Sesi berakhir."); out.print(result.toString()); return; }
    boolean boleh = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
    // Gerbang tambahan (OR, bukan pengganti): role (Tbmrole) juga bisa diberi izin granular per
    // aksi (create/update/delete) khusus menu "mutasirekening". Perilaku lama (bolehKelola) tetap
    // selalu boleh; ini hanya MENAMBAH pintu masuk baru, tidak mempersempit. Aksi "toggle" (tandai
    // sudah/belum dicocokkan) diperlakukan sebagai "update" (mengubah field baris yang sudah ada).
    ais.database.model.Tbmrole roleMR = u.hakAkses();
    org.json.JSONObject ebisnisMenuMR = roleMR == null ? null
        : ais.common.EbisnisMenuKatalog.urai(roleMR.getEbisnisMenu());
    String aksi = request.getParameter("aksi");
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    Session session = HibernateUtil.currentNativeSession();
    Long tokoId=null; boolean lockToko=false;
    if (u.getPedagang()!=null && u.getPedagang().getToko()!=null){ tokoId=u.getPedagang().getToko().getId(); lockToko=true; }

    if ("listAkun".equals(aksi)) {
        SQLQuery sq = session.createSQLQuery(
            "select id, coalesce(kode,'-'), coalesce(nama,'-') from akunting.akun "
          + " where (bank_id is not null or norek is not null or lower(coalesce(nama,'')) like '%kas%' or lower(coalesce(nama,'')) like '%bank%') "
          + " order by kode ");
        sq.setMaxResults(500);
        JSONArray arr = new JSONArray();
        for (Object o : sq.list()){ Object[] a=(Object[])o; JSONObject j=new JSONObject();
            j.put("id", ((Number)a[0]).longValue()); j.put("kode", a[1].toString()); j.put("nama", a[2].toString());
            arr.put(j);
        }
        result.put("status","00"); result.put("data",arr);

    } else if ("simpan".equals(aksi)) {
        boolean adaIdMR = ada(request.getParameter("id"));
        boolean bolehCrudMR = ebisnisMenuMR != null && ais.common.EbisnisMenuKatalog.bolehAksi(
            ebisnisMenuMR, "mutasirekening", adaIdMR ? "update" : "create");
        if (!boleh && !bolehCrudMR){ result.put("status","03"); result.put("message","Hanya admin yang boleh mengubah."); out.print(result.toString()); return; }
        String idS=request.getParameter("id");
        MutasiRekeningKoran m;
        if (ada(idS)) { m = (MutasiRekeningKoran) session.get(MutasiRekeningKoran.class, Long.valueOf(idS.trim())); if (m==null){ result.put("status","02"); result.put("message","Baris tidak ditemukan."); out.print(result.toString()); return; } }
        else { m = new MutasiRekeningKoran(); }
        String akunS=request.getParameter("akun");
        if (!ada(akunS)){ result.put("status","02"); result.put("message","Akun bank wajib dipilih."); out.print(result.toString()); return; }
        m.setAkunBank(Long.valueOf(akunS.trim()));
        m.setNamaAkunBank(request.getParameter("namaAkun"));
        String tg=request.getParameter("tanggal");
        if (ada(tg)) { try { m.setTanggal(DF.parse(tg.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/mutasi_rekening_service.jsp:64");} }
        m.setKeterangan(request.getParameter("keterangan"));
        m.setReferensi(request.getParameter("referensi"));
        m.setMasuk(Double.valueOf(parseD(request.getParameter("masuk"))));
        m.setKeluar(Double.valueOf(parseD(request.getParameter("keluar"))));
        m.setSaldo(Double.valueOf(parseD(request.getParameter("saldo"))));
        if (lockToko) { Toko t=(Toko)session.get(Toko.class, tokoId); m.setToko(t); }
        m.setOlehId(u.getUserId()); m.setOleh(u.getUserId());
        Common.refreshSaveOrUpdate(session, m);
        result.put("status","00"); result.put("message","Tersimpan."); result.put("id", m.getId());

    } else if ("hapus".equals(aksi)) {
        boolean bolehCrudMRHapus = ebisnisMenuMR != null
            && ais.common.EbisnisMenuKatalog.bolehAksi(ebisnisMenuMR, "mutasirekening", "delete");
        if (!boleh && !bolehCrudMRHapus){ result.put("status","03"); result.put("message","Hanya admin yang boleh menghapus."); out.print(result.toString()); return; }
        String idS=request.getParameter("id");
        if (ada(idS)) { MutasiRekeningKoran m=(MutasiRekeningKoran) session.get(MutasiRekeningKoran.class, Long.valueOf(idS.trim())); if (m!=null) Common.refreshDelete(session, m); }
        result.put("status","00"); result.put("message","Dihapus.");

    } else if ("toggle".equals(aksi)) {
        boolean bolehCrudMRToggle = ebisnisMenuMR != null
            && ais.common.EbisnisMenuKatalog.bolehAksi(ebisnisMenuMR, "mutasirekening", "update");
        if (!boleh && !bolehCrudMRToggle){ result.put("status","03"); result.put("message","Hanya admin."); out.print(result.toString()); return; }
        String idS=request.getParameter("id");
        MutasiRekeningKoran m=(MutasiRekeningKoran) session.get(MutasiRekeningKoran.class, Long.valueOf(idS.trim()));
        if (m==null){ result.put("status","02"); result.put("message","Baris tidak ditemukan."); out.print(result.toString()); return; }
        boolean val = "true".equals(request.getParameter("val"));
        m.setSudahRekon(Boolean.valueOf(val));
        m.setTanggalRekon(val ? new java.util.Date() : null);
        Common.refreshSaveOrUpdate(session, m);
        result.put("status","00");

    } else { // list
        StringBuilder sb = new StringBuilder(
            "select id, cast(tanggal as date), coalesce(keterangan,''), coalesce(referensi,''), coalesce(masuk,0), coalesce(keluar,0), coalesce(sudahrekon,false) "
          + " from koperasi.mutasi_rekening_koran where 1=1 ");
        String akunS=request.getParameter("akun");
        if (ada(akunS)) sb.append(" and akun_bank = ").append(Long.valueOf(akunS.trim()));
        if (lockToko) sb.append(" and toko = ").append(tokoId);
        String sampai=request.getParameter("sampai");
        if (ada(sampai)) sb.append(" and cast(tanggal as date) <= cast(:sampai as date) ");
        sb.append(" order by tanggal desc, id desc ");
        SQLQuery sq = session.createSQLQuery(sb.toString());
        if (ada(sampai)) sq.setParameter("sampai", sampai.trim());
        sq.setMaxResults(2000);
        JSONArray arr = new JSONArray();
        for (Object o : sq.list()){ Object[] a=(Object[])o; JSONObject j=new JSONObject();
            j.put("id", ((Number)a[0]).longValue()); j.put("tanggal", a[1]==null?"":DF.format((java.util.Date)a[1]));
            j.put("keterangan", a[2]==null?"":a[2].toString()); j.put("referensi", a[3]==null?"":a[3].toString());
            j.put("masuk", ((Number)a[4]).doubleValue()); j.put("keluar", ((Number)a[5]).doubleValue());
            j.put("rekon", ((Boolean)a[6]).booleanValue());
            arr.put(j);
        }
        result.put("status","00"); result.put("data",arr);
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/mutasi_rekening_service.jsp:116");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()+" (pastikan sudah RESTART agar tabel koperasi.mutasi_rekening_koran terbentuk)"); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/mutasi_rekening_service.jsp:117");}
}
out.print(result.toString());
%>
