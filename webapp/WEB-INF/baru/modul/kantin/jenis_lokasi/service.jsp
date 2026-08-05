<%@page session="false"%>
<%--
  Endpoint JSON master "Jenis Lokasi" (aksi: list | simpan | hapus).
  Reuse LokasiKantinUtil (query/simpan/hapus + gate admin). Sesi native request-scoped (TAK ditutup di JSP; FilterJSP yang menutup).
  Tulis/hapus HANYA bila LokasiKantinUtil.bolehKelola(request) (admin & bukan pedagang/toko).
--%>
<%@page import="org.json.*"%>
<%@page import="java.util.List"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.asset.JenisLokasi"%>
<%@page import="ais.action.master.koperasi.helper.LokasiKantinUtil"%>
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
	boolean boleh = LokasiKantinUtil.bolehKelola(request);
	String aksi = request.getParameter("aksi");
	// Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
	// JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
	Session session = HibernateUtil.currentNativeSession();

	if ("simpan".equals(aksi) || "hapus".equals(aksi)) {
		if (!boleh) {
			result.put("status", "03");
			result.put("message", "Hanya admin (bukan pedagang/toko) yang boleh mengubah data ini.");
			out.print(result.toString()); return;
		}
	}

	if ("list".equals(aksi) || aksi == null) {
		JSONArray arr = new JSONArray();
		List<JenisLokasi> daftar = LokasiKantinUtil.daftarJenisLokasi(session, false);
		for (JenisLokasi j : daftar) {
			JSONObject o = new JSONObject();
			o.put("id", j.getId());
			o.put("nama", j.getNama() == null ? "" : j.getNama());
			o.put("keterangan", j.getKeterangan() == null ? "" : j.getKeterangan());
			o.put("aktif", j.getAktif() != null && j.getAktif().booleanValue());
			o.put("warna", j.getWarna() == null ? "" : j.getWarna());
			o.put("ikon", j.getIkon() == null ? "" : j.getIkon());
			o.put("urutan", j.getUrutan() == null ? 0 : j.getUrutan().intValue());
			arr.put(o);
		}
		result.put("status", "00"); result.put("boleh", boleh); result.put("data", arr);

	} else if ("simpan".equals(aksi)) {
		Long id = null;
		try { String s = request.getParameter("id"); if (s != null && s.trim().length() > 0) id = Long.valueOf(s.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/jenis_lokasi/service.jsp:57");}
		Integer urutan = null;
		try { String s = request.getParameter("urutan"); if (s != null && s.trim().length() > 0) urutan = Integer.valueOf(s.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/jenis_lokasi/service.jsp:59");}
		boolean aktif = !"false".equalsIgnoreCase(request.getParameter("aktif"));
		String oleh = tbmuser.getUserNama() != null ? tbmuser.getUserNama() : String.valueOf(tbmuser.getUserId());
		try {
			LokasiKantinUtil.simpanJenisLokasi(session, id, request.getParameter("nama"), request.getParameter("keterangan"),
					aktif, request.getParameter("warna"), request.getParameter("ikon"), urutan, oleh, String.valueOf(tbmuser.getUserId()));
			result.put("status", "00"); result.put("message", "Jenis lokasi tersimpan.");
		} catch (IllegalArgumentException iae) {
			result.put("status", "02"); result.put("message", iae.getMessage());
		}

	} else if ("hapus".equals(aksi)) {
		Long id = null;
		try { id = Long.valueOf(request.getParameter("id").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/jenis_lokasi/service.jsp:72");}
		if (id == null) { result.put("status", "02"); result.put("message", "ID kosong."); out.print(result.toString()); return; }
		try {
			LokasiKantinUtil.hapusJenisLokasi(session, id);
			result.put("status", "00"); result.put("message", "Data dihapus.");
		} catch (Exception e) {
			result.put("status", "04"); result.put("message", "Tidak bisa dihapus (mungkin sedang dipakai lokasi lain).");
		}

	} else {
		result.put("status", "98"); result.put("message", "Aksi tidak dikenal.");
	}
} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/jenis_lokasi/service.jsp:85");
	try { result.put("status", "99"); result.put("message", "Error: " + e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/jenis_lokasi/service.jsp:86");}
}
out.print(result.toString());
%>
