<%@page session="false"%>
<%--
  Endpoint JSON: verifikasi PIN transaksi anggota (dientri PEMBELI di Layar Pelanggan / layar kedua
  POS). Dipanggil LANGSUNG oleh layar_pelanggan.jsp -- nilai PIN anggota TIDAK PERNAH dikirim ke
  browser, hanya hasil ok/tidak. Param: memberId (wajib), pin (wajib, dibandingkan plain thd
  AnggotaKoperasi.pin). Sesi currentNativeSession() (dipanggil dari JSP /baru; TIDAK ditutup manual
  di sini -- FilterJSP yang menutup terpusat di akhir request, lihat COOKBOOK POLA B-JSP pada
  HibernateUtil -- tetapi transaksi yang membawa hitungan gagal/kunci PIN WAJIB di-commit eksplisit
  di sini, sama seperti JSP service lain yang menulis lewat currentNativeSession()). Tidak butuh hak
  admin -- PIN adalah milik anggota sendiri, dientri anggota sendiri.

  Sejak perbaikan pembatasan laju percobaan PIN: AnggotaKoperasi.verifikasiPin() kini mengubah
  hitungan kegagalan/penguncian PIN member sebagai efek samping, jadi endpoint ini WAJIB (a)
  mengecek AnggotaKoperasi.cekPinTerkunci() lebih dulu agar pesan ke pembeli jelas "coba lagi
  nanti" alih-alih "PIN salah" yang menyesatkan selama masa tunda, dan (b) membungkus pemanggilan
  verifikasiPin() dalam transaksi yang di-commit, selain itu perubahan hitungan tidak pernah
  tersimpan.
--%>
<%@page import="org.json.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
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
Transaction tx = null;
try {
    Tbmuser u = Common.getCurrentUser(request);
    if (u==null || u.getUserId()==null){ result.put("status","01"); result.put("message","Sesi berakhir."); out.print(result.toString()); return; }

    String memberIdS = request.getParameter("memberId");
    String pinInput = request.getParameter("pin");
    if (!ada(memberIdS) || !ada(pinInput)) { result.put("status","02"); result.put("message","Data tidak lengkap."); out.print(result.toString()); return; }

    Session session = HibernateUtil.currentNativeSession();
    AnggotaKoperasi a = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, Long.valueOf(memberIdS.trim()));
    if (a == null) { result.put("status","02"); result.put("message","Anggota tidak ditemukan."); out.print(result.toString()); return; }

    String pesanTerkunci = a.cekPinTerkunci();
    if (pesanTerkunci != null) { result.put("status","02"); result.put("ok", false); result.put("message", pesanTerkunci); out.print(result.toString()); return; }

    tx = session.beginTransaction();
    boolean ok = a.verifikasiPin(pinInput.trim());
    session.saveOrUpdate(a);
    tx.commit();

    String subjek = ais.action.servlet.api.BiometricApi.linkedUserIdForMember(session, a);
    if (subjek == null) subjek = "MEMBER:" + a.getId();
    String referenceId = request.getParameter("referenceId");
    JSONObject eventRequest = new JSONObject();
    eventRequest.put("reference_type", "POS_PURCHASE");
    eventRequest.put("reference_id", referenceId == null ? "" : referenceId.trim());
    eventRequest.put("captured_at_epoch", System.currentTimeMillis());
    eventRequest.put("clientMutationId", "pos-pin-web-" + (referenceId == null ? "" : referenceId.trim()));
    Long eventId = ais.action.servlet.api.BiometricApi.recordPosPinVerification(u, subjek, eventRequest, ok);
    result.put("status","00"); result.put("ok", ok);
    result.put("pinVerificationEventId", eventId == null ? JSONObject.NULL : eventId);
} catch (Exception e) {
    if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/pos/verifikasi_pin_service.jsp:rollback"); } }
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/pos/verifikasi_pin_service.jsp:36");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()+" (pastikan sudah RESTART agar kolom pin/wajib_pin terbentuk)"); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/pos/verifikasi_pin_service.jsp:37");}
}
out.print(result.toString());
%>
