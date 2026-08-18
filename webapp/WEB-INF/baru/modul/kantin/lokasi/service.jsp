<%@page session="false"%>
<%--
  Endpoint JSON master "Lokasi (Gudang)" (aksi: list | simpan | hapus).
  list -> {data:[lokasi], jenis:[jenisLokasi aktif], toko:[toko], boleh}. Reuse LokasiKantinUtil.
  Sesi currentSession() (TAK ditutup). Tulis/hapus HANYA bila bolehKelola (admin & bukan pedagang/toko).
--%>
<%@page import="org.json.*"%>
<%@page import="java.util.List"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.asset.Lokasi"%>
<%@page import="ais.database.model.asset.JenisLokasi"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.sirs.Gudang"%>
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
		Long jenisId = null;
		try { String s = request.getParameter("jenis"); if (s != null && s.trim().length() > 0) jenisId = Long.valueOf(s.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/lokasi/service.jsp:44");}
		String cari = request.getParameter("cari");

		JSONArray arr = new JSONArray();
		List<Lokasi> daftar = LokasiKantinUtil.daftarLokasi(session, jenisId, cari, false);
		for (Lokasi l : daftar) {
			JSONObject o = new JSONObject();
			o.put("id", l.getId());
			o.put("nama", l.getNama() == null ? "" : l.getNama());
			o.put("keterangan", l.getKeterangan() == null ? "" : l.getKeterangan());
			o.put("alamat", l.getAlamat() == null ? "" : l.getAlamat());
			o.put("aktif", l.getAktif() != null && l.getAktif().booleanValue());
			JenisLokasi jl = l.getJenisLokasi();
			o.put("jenisId", jl == null ? "" : jl.getId());
			o.put("jenisNama", jl == null ? "" : jl.getNama());
			o.put("jenisWarna", jl == null || jl.getWarna() == null ? "" : jl.getWarna());
			o.put("jenisIkon", jl == null || jl.getIkon() == null ? "" : jl.getIkon());
			Toko tk = l.getToko();
			o.put("tokoId", tk == null ? "" : tk.getId());
			o.put("tokoNama", tk == null ? "" : (tk.getNama() == null ? "" : tk.getNama()));
			Gudang gd = l.getGudang();
			o.put("gudangId", gd == null ? "" : gd.getId());
			o.put("gudangNama", gd == null ? "" : (gd.getNama() == null ? "" : gd.getNama()));
			o.put("gudangIndukNama", (gd == null || gd.getGudangInduk() == null) ? ""
					: (gd.getGudangInduk().getNama() == null ? "" : gd.getGudangInduk().getNama()));
			arr.put(o);
		}

		JSONArray jarr = new JSONArray();
		List<JenisLokasi> jenis = LokasiKantinUtil.daftarJenisLokasi(session, true);
		for (JenisLokasi j : jenis) {
			JSONObject o = new JSONObject();
			o.put("id", j.getId()); o.put("nama", j.getNama() == null ? "" : j.getNama());
			o.put("warna", j.getWarna() == null ? "" : j.getWarna()); o.put("ikon", j.getIkon() == null ? "" : j.getIkon());
			jarr.put(o);
		}

		JSONArray tarr = new JSONArray();
		List<Toko> tokos = session.createCriteria(Toko.class).addOrder(Order.asc("nama")).setMaxResults(2000).list();
		for (Toko t : tokos) {
			JSONObject o = new JSONObject();
			o.put("id", t.getId()); o.put("nama", t.getNama() == null ? ("Toko #" + t.getId()) : t.getNama());
			tarr.put(o);
		}

		// Fase 2: daftar Gudang (hierarki pusat/cabang via gudangInduk) utk picker "Gudang" pada form --
		// reuse LokasiKantinUtil.daftarGudangUntukPicker (dipakai bersama versi ZK), BUKAN query baru.
		JSONArray garr = new JSONArray();
		List<Object[]> gudangs = LokasiKantinUtil.daftarGudangUntukPicker(session);
		for (Object[] g : gudangs) {
			JSONObject o = new JSONObject();
			o.put("id", g[0]); o.put("nama", g[1]);
			garr.put(o);
		}

		result.put("status", "00"); result.put("boleh", boleh);
		result.put("data", arr); result.put("jenis", jarr); result.put("toko", tarr); result.put("gudang", garr);

	} else if ("simpan".equals(aksi)) {
		Long id = null, jenisId = null, tokoId = null, gudangId = null;
		try { String s = request.getParameter("id"); if (s != null && s.trim().length() > 0) id = Long.valueOf(s.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/lokasi/service.jsp:89");}
		try { String s = request.getParameter("jenisId"); if (s != null && s.trim().length() > 0) jenisId = Long.valueOf(s.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/lokasi/service.jsp:90");}
		try { String s = request.getParameter("tokoId"); if (s != null && s.trim().length() > 0) tokoId = Long.valueOf(s.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/lokasi/service.jsp:91");}
		try { String s = request.getParameter("gudangId"); if (s != null && s.trim().length() > 0) gudangId = Long.valueOf(s.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/lokasi/service.jsp:gudangId");}
		Boolean aktif = Boolean.valueOf(!"false".equalsIgnoreCase(request.getParameter("aktif")));
		String oleh = tbmuser.getUserNama() != null ? tbmuser.getUserNama() : String.valueOf(tbmuser.getUserId());
		try {
			LokasiKantinUtil.simpanLokasi(session, id, request.getParameter("nama"), request.getParameter("keterangan"),
					request.getParameter("alamat"), aktif, jenisId, tokoId, gudangId, oleh, String.valueOf(tbmuser.getUserId()));
			result.put("status", "00"); result.put("message", "Lokasi tersimpan.");
		} catch (IllegalArgumentException iae) {
			result.put("status", "02"); result.put("message", iae.getMessage());
		}

	} else if ("hapus".equals(aksi)) {
		Long id = null;
		try { id = Long.valueOf(request.getParameter("id").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/lokasi/service.jsp:104");}
		if (id == null) { result.put("status", "02"); result.put("message", "ID kosong."); out.print(result.toString()); return; }
		try {
			LokasiKantinUtil.hapusLokasi(session, id);
			result.put("status", "00"); result.put("message", "Data dihapus.");
		} catch (Exception e) {
			result.put("status", "04"); result.put("message", "Tidak bisa dihapus (mungkin masih dipakai transaksi lain).");
		}

	} else {
		result.put("status", "98"); result.put("message", "Aksi tidak dikenal.");
	}
} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/lokasi/service.jsp:117");
	try { result.put("status", "99"); result.put("message", "Error: " + e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/lokasi/service.jsp:118");}
}
out.print(result.toString());
%>
