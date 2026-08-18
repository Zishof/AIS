<%@page session="false"%>
<%--
  Endpoint JSON editor Limit Kredit Anggota. Aksi: list (anggota + limit_kredit + piutang berjalan),
  simpan (set HANYA limitKredit pada anggota yg ada — tidak menyentuh field lain). Sesi currentSession().
  Hanya admin (LokasiKantinUtil.bolehKelola) yang boleh mengubah.
--%>
<%@page import="org.json.*"%>
<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.SQLQuery"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%!
    private static boolean ada(String s){ return s!=null && s.trim().length()>0; }
%>
<%
response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
try {
    Tbmuser u = Common.getCurrentUser(request);
    if (u==null || u.getUserId()==null){ result.put("status","01"); result.put("message","Sesi berakhir."); out.print(result.toString()); return; }
    boolean boleh = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
    // Gerbang tambahan (OR, bukan pengganti): role (Tbmrole) juga bisa diberi izin granular
    // "update" khusus menu "limitkredit" tanpa perlu jadi supervisor toko penuh. Perilaku lama
    // (bolehKelola) tetap selalu boleh; ini hanya MENAMBAH pintu masuk baru, tidak mempersempit.
    ais.database.model.Tbmrole roleLK = u.hakAkses();
    boolean bolehCrudLK = roleLK != null && ais.common.EbisnisMenuKatalog.bolehAksi(
        ais.common.EbisnisMenuKatalog.urai(roleLK.getEbisnisMenu()), "limitkredit", "update");
    String aksi = request.getParameter("aksi");
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    Session session = HibernateUtil.currentNativeSession();

    if ("simpan".equals(aksi)) {
        if (!boleh && !bolehCrudLK){ result.put("status","03"); result.put("message","Hanya admin yang boleh mengubah."); out.print(result.toString()); return; }
        String idS=request.getParameter("id");
        if (!ada(idS)){ result.put("status","02"); result.put("message","ID kosong."); out.print(result.toString()); return; }
        AnggotaKoperasi a = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, Long.valueOf(idS.trim()));
        if (a==null){ result.put("status","02"); result.put("message","Anggota tidak ditemukan."); out.print(result.toString()); return; }
        double lim = 0; try { lim = Double.parseDouble(request.getParameter("limit")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/limit_kredit_service.jsp:36");}
        if (lim < 0) lim = 0;
        a.setLimitKredit(Double.valueOf(lim));
        Common.refreshSaveOrUpdate(session, a);
        result.put("status","00"); result.put("message","Tersimpan.");

    } else { // list
        StringBuilder sb = new StringBuilder(
            "select a.id as id, coalesce(a.kode,a.kode_identitas,'-') as kode, coalesce(a.nama,'-') as nama, coalesce(a.limit_kredit,0) as limit_kredit, "
          + " coalesce((select sum(coalesce(h.total_biaya,0)-(coalesce(h.bayar_tunai,0)+coalesce(h.bayar_non_tunai,0))) "
          + "   from koperasi.pembelian_anggota_koperasi h where h.anggota_koperasi=a.id "
          + "   and (coalesce(h.bayar_tunai,0)+coalesce(h.bayar_non_tunai,0)) < coalesce(h.total_biaya,0)),0) as piutang "
          + " from koperasi.anggota_koperasi a where a.aktif=true ");
        String q = request.getParameter("q");
        if (ada(q)) { sb.append(" and (lower(coalesce(a.kode,'')) like :q or lower(coalesce(a.nama,'')) like :q or lower(coalesce(a.kode_identitas,'')) like :q) "); }
        sb.append(" order by a.nama asc ");
        SQLQuery sq = session.createSQLQuery(sb.toString());
        // FIX DataException "Bad value for type double : MEM/1/2026/1": tanpa addScalar(), Hibernate
        // menebak tipe tiap kolom scalar dari metadata JDBC hasil coalesce()/subquery -- kadang salah
        // menganggap kolom teks (kode) sbg numerik saat driver Postgres tak bisa memastikan tipe pasti
        // ekspresi coalesce campuran. Deklarasikan tipe eksplisit sesuai urutan SELECT di atas supaya
        // pemetaan kolom selalu benar, tak bergantung pada tebakan metadata.
        sq.addScalar("id", org.hibernate.Hibernate.LONG);
        sq.addScalar("kode", org.hibernate.Hibernate.STRING);
        sq.addScalar("nama", org.hibernate.Hibernate.STRING);
        sq.addScalar("limit_kredit", org.hibernate.Hibernate.DOUBLE);
        sq.addScalar("piutang", org.hibernate.Hibernate.DOUBLE);
        if (ada(q)) sq.setParameter("q", "%"+q.trim().toLowerCase()+"%");
        sq.setMaxResults(1000);
        JSONArray arr = new JSONArray();
        for (Object o : sq.list()){ Object[] a=(Object[])o; JSONObject j=new JSONObject();
            j.put("id", a[0]); j.put("kode", a[1]==null?"":a[1].toString()); j.put("nama", a[2]==null?"":a[2].toString());
            j.put("limit", ((Number)a[3]).doubleValue()); j.put("piutang", ((Number)a[4]).doubleValue());
            arr.put(j);
        }
        result.put("status","00"); result.put("data",arr);
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/limit_kredit_service.jsp:64");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()+" (pastikan sudah RESTART agar kolom limit_kredit terbentuk)"); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/limit_kredit_service.jsp:65");}
}
out.print(result.toString());
%>
