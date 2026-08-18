<%@page session="false"%>
<%--
  Endpoint JSON dasbor "Pengajuan Anda" (Workflow / Proses SOP) — versi JSP.
  Memakai PengajuanAndaSopUtil (reuse kriteria AktorSop, identik dgn DasboardSop ZK).
  Sesi: currentSession() (dikelola kerangka kerja) -> TIDAK ditutup manual.

  Aksi:
    - dashboard   : 6 metrik + angka banner (Proses Dipantau, Total Antrian).
    - listProses  : pengajuan Anda yang sedang diproses (DisposisiSop).
    - listDisposisi: pengajuan yang baru saja Anda disposisi (DisposisiAlurSop).
    - listSop     : daftar SOP yang bisa diajukan (Pengajuan Baru), di-scope spt ZK.
--%>
<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.json.*"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.sop.Sop"%>
<%@page import="ais.database.model.sop.DisposisiSop"%>
<%@page import="ais.database.model.sop.DisposisiAlurSop"%>
<%@page import="ais.action.master.sop.helper.PengajuanAndaSopUtil"%>
<%@page import="ais.action.master.sop.helper.PengajuanAndaSopUtil.Ringkasan"%>
<%@page import="ais.action.master.sop.DisposisiSopAction"%>
<%!
    static final int PAGE_SIZE = 10;
    static String s(Object o){ return o==null?"":o.toString(); }
    static int pageOf(HttpServletRequest r){ try { int p=Integer.parseInt(r.getParameter("page")); return p<0?0:p; } catch(Exception e){ return 0; } }
    static String statusDisposisiSop(DisposisiSop d){
        if (d.getDisposisiEnd()!=null) return "selesai";
        if (d.getDisposisiSetuju()!=null) return "disetujui";
        return "menunggu";
    }
%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
try {
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        result.put("status","01"); result.put("message","Sesi berakhir, silakan masuk kembali.");
        out.print(result.toString()); return;
    }
    String aksi = request.getParameter("aksi");
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    org.hibernate.Session session = HibernateUtil.currentNativeSession();

    if ("dashboard".equals(aksi)) {
        Ringkasan r = PengajuanAndaSopUtil.hitungRingkasan(session, tbmuser);
        result.put("menungguSaya", r.menungguSaya);
        result.put("sudahDisposisi", r.sudahDisposisi);
        result.put("pengajuanAnda", r.pengajuanAnda);
        result.put("selesai", r.selesai);
        result.put("menungguPetugas", r.menungguPetugas);
        result.put("lewatBatas", r.lewatBatas);
        result.put("prosesDipantau", r.prosesDipantau);
        result.put("totalAntrian", r.totalAntrian);
        result.put("status","00");

    } else if ("listProses".equals(aksi)) {
        int page = pageOf(request);
        int total = PengajuanAndaSopUtil.hitung(PengajuanAndaSopUtil.criteriaPengajuanAnda(session, tbmuser));
        Criteria c = PengajuanAndaSopUtil.criteriaPengajuanAnda(session, tbmuser)
                .addOrder(Order.desc("id")).setFirstResult(page*PAGE_SIZE).setMaxResults(PAGE_SIZE);
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) {
            DisposisiSop d = (DisposisiSop) o;
            JSONObject j = new JSONObject();
            j.put("id", d.getId());
            j.put("kode", d.getDisposisiStart()!=null ? s(d.getDisposisiStart().getKode()) : ("#"+d.getId()));
            j.put("sop", d.getSop()==null?"":((d.getSop().getKode()==null?"":s(d.getSop().getKode())+" - ")+s(d.getSop().getNama())));
            j.put("waktu", d.getWaktu()==null?"":df.format(d.getWaktu()));
            j.put("keterangan", s(d.getKeterangan()));
            j.put("status", statusDisposisiSop(d));
            j.put("tahap", d.getDisposisiStart()!=null && d.getDisposisiStart().getAlurSop()!=null
                    ? s(d.getDisposisiStart().getAlurSop().getNama()) : "");
            arr.put(j);
        }
        result.put("data", arr); result.put("page", page);
        result.put("total", total); result.put("totalPage", (int)Math.ceil(total/(double)PAGE_SIZE));
        result.put("status","00");

    } else if ("listDisposisi".equals(aksi)) {
        int page = pageOf(request);
        int total = PengajuanAndaSopUtil.hitung(PengajuanAndaSopUtil.criteriaSudahDisposisi(session, tbmuser));
        Criteria c = PengajuanAndaSopUtil.criteriaSudahDisposisi(session, tbmuser)
                .addOrder(Order.desc("id")).setFirstResult(page*PAGE_SIZE).setMaxResults(PAGE_SIZE);
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) {
            DisposisiAlurSop da = (DisposisiAlurSop) o;
            DisposisiSop d = da.getDisposisiSop();
            JSONObject j = new JSONObject();
            j.put("id", da.getId());
            j.put("kode", s(da.getKode()));
            j.put("sop", (d!=null && d.getSop()!=null)?((d.getSop().getKode()==null?"":s(d.getSop().getKode())+" - ")+s(d.getSop().getNama())):"");
            j.put("waktu", da.getWaktu()==null?"":df.format(da.getWaktu()));
            j.put("keterangan", s(da.getKeterangan()));
            j.put("tahap", da.getAlurSop()!=null ? s(da.getAlurSop().getNama()) : "");
            arr.put(j);
        }
        result.put("data", arr); result.put("page", page);
        result.put("total", total); result.put("totalPage", (int)Math.ceil(total/(double)PAGE_SIZE));
        result.put("status","00");

    } else if ("listSop".equals(aksi)) {
        String q = request.getParameter("q");
        Criteria c = session.createCriteria(Sop.class)
                .add(DisposisiSopAction.createCriterionSop(tbmuser)).addOrder(Order.asc("nama")).setMaxResults(500);
        if (q!=null && q.trim().length()>0) c.add(Restrictions.or(
                Restrictions.ilike("nama", q.trim(), MatchMode.ANYWHERE), Restrictions.ilike("kode", q.trim(), MatchMode.ANYWHERE)));
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) {
            Sop sop = (Sop) o; JSONObject j = new JSONObject();
            j.put("id", sop.getId()); j.put("kode", s(sop.getKode())); j.put("nama", s(sop.getNama()));
            j.put("versi", s(sop.getVersi()));
            j.put("jenis", sop.getJenisSop()==null?"Lainnya":s(sop.getJenisSop().getNama()));
            arr.put(j);
        }
        result.put("data", arr); result.put("status","00");

    } else { result.put("status","98"); result.put("message","Aksi tidak dikenal."); }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pagesmastersoppengajuananda/pengajuan_anda_service.jsp:130");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoppengajuananda/pengajuan_anda_service.jsp:131");}
}
out.print(result.toString());
%>
