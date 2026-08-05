<%@page session="false"%>
<%--
  Endpoint JSON "Mutasi Gudang" (stok per-Lokasi). Aksi:
    ref     -> data referensi (lokasi + produk) untuk form.
    stok    -> stok berjalan satu (lokasi, produk).
    list    -> riwayat mutasi terbaru (opsional filter lokasi).
    simpan  -> catat MASUK | KELUAR | TRANSFER | PENYESUAIAN via StokLokasiUtil.
  Reuse StokLokasiUtil + LokasiKantinUtil. Sesi currentSession() (TAK ditutup).
  Menulis HANYA bila LokasiKantinUtil.bolehKelola(request) (admin & bukan pedagang/toko).
--%>
<%@page import="org.json.*"%>
<%@page import="java.util.List"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.SQLQuery"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.asset.Lokasi"%>
<%@page import="ais.action.master.koperasi.helper.LokasiKantinUtil"%>
<%@page import="ais.action.master.inventory.StokLokasiUtil"%>
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

	if ("simpan".equals(aksi)) {
		if (!boleh) {
			result.put("status", "03"); result.put("message", "Hanya admin (bukan pedagang/toko) yang boleh mencatat mutasi.");
			out.print(result.toString()); return;
		}
	}

	if ("ref".equals(aksi)) {
		JSONArray lok = new JSONArray();
		List<Lokasi> lokasis = LokasiKantinUtil.daftarLokasi(session, null, null, true);
		for (Lokasi l : lokasis) {
			JSONObject o = new JSONObject();
			o.put("id", l.getId()); o.put("nama", l.getNama() == null ? "" : l.getNama());
			o.put("jenis", l.getJenisLokasi() == null ? "" : (l.getJenisLokasi().getNama() == null ? "" : l.getJenisLokasi().getNama()));
			lok.put(o);
		}
		JSONArray prd = new JSONArray();
		SQLQuery q = session.createSQLQuery("select id, kode, nama, coalesce(hargabeli,0) from koperasi.produk where aktif=true order by nama asc");
		q.setMaxResults(5000);
		for (Object row : q.list()) {
			Object[] a = (Object[]) row; JSONObject o = new JSONObject();
			o.put("id", a[0]); o.put("kode", a[1] == null ? "" : a[1].toString());
			o.put("nama", a[2] == null ? "" : a[2].toString()); o.put("hargabeli", ((Number) a[3]).doubleValue());
			prd.put(o);
		}
		result.put("status", "00"); result.put("boleh", boleh); result.put("lokasi", lok); result.put("produk", prd);

	} else if ("stok".equals(aksi)) {
		Long lokId = null, prdId = null;
		try { lokId = Long.valueOf(request.getParameter("lokasi").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/mutasi_gudang/service.jsp:67");}
		try { prdId = Long.valueOf(request.getParameter("produk").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/mutasi_gudang/service.jsp:68");}
		result.put("status", "00"); result.put("stok", StokLokasiUtil.qtyStok(session, lokId, prdId));

	} else if ("list".equals(aksi)) {
		Long lokId = null;
		try { String s = request.getParameter("lokasi"); if (s != null && s.trim().length() > 0) lokId = Long.valueOf(s.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/mutasi_gudang/service.jsp:73");}
		StringBuilder sb = new StringBuilder(
			"select m.tanggal, m.jenis, l.nama, p.kode, p.nama, m.qty, lp.nama, coalesce(m.keterangan,'') "
			+ "from asset.mutasi_lokasi m join asset.lokasi l on l.id=m.lokasi "
			+ "join koperasi.produk p on p.id=m.produk left join asset.lokasi lp on lp.id=m.lokasi_pasangan where 1=1 ");
		if (lokId != null) sb.append("and m.lokasi=:l ");
		sb.append("order by m.tanggal desc, m.id desc");
		SQLQuery q = session.createSQLQuery(sb.toString());
		if (lokId != null) q.setParameter("l", lokId);
		q.setMaxResults(200);
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		JSONArray arr = new JSONArray();
		for (Object row : q.list()) {
			Object[] a = (Object[]) row; JSONObject o = new JSONObject();
			o.put("tanggal", a[0] == null ? "" : df.format((java.util.Date) a[0]));
			o.put("jenis", a[1] == null ? "" : a[1].toString());
			o.put("lokasi", a[2] == null ? "" : a[2].toString());
			o.put("kode", a[3] == null ? "" : a[3].toString());
			o.put("produk", a[4] == null ? "" : a[4].toString());
			o.put("qty", ((Number) a[5]).doubleValue());
			o.put("pasangan", a[6] == null ? "" : a[6].toString());
			o.put("keterangan", a[7] == null ? "" : a[7].toString());
			arr.put(o);
		}
		result.put("status", "00"); result.put("data", arr);

	} else if ("simpan".equals(aksi)) {
		String jenis = request.getParameter("jenis");
		Long lokId = null, prdId = null, lokTujuan = null;
		try { lokId = Long.valueOf(request.getParameter("lokasi").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/mutasi_gudang/service.jsp:102");}
		try { prdId = Long.valueOf(request.getParameter("produk").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/mutasi_gudang/service.jsp:103");}
		try { String s = request.getParameter("lokasiTujuan"); if (s != null && s.trim().length() > 0) lokTujuan = Long.valueOf(s.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/mutasi_gudang/service.jsp:104");}
		double qty = 0d; try { qty = Double.parseDouble(request.getParameter("qty")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/mutasi_gudang/service.jsp:105");}
		Double harga = null; try { harga = Double.valueOf(Double.parseDouble(request.getParameter("harga"))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/mutasi_gudang/service.jsp:106");}
		String ket = request.getParameter("keterangan");
		java.util.Date tgl = new java.util.Date();
		try { String s = request.getParameter("tanggal"); if (s != null && s.trim().length() > 0) tgl = new SimpleDateFormat("yyyy-MM-dd").parse(s.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/mutasi_gudang/service.jsp:109");}
		String oleh = tbmuser.getUserNama() != null ? tbmuser.getUserNama() : String.valueOf(tbmuser.getUserId());
		String olehId = String.valueOf(tbmuser.getUserId());
		try {
			if (MutasiLokasiJenisTransfer(jenis)) {
				StokLokasiUtil.catatTransfer(session, lokId, lokTujuan, prdId, qty, harga, tgl, ket, oleh, olehId);
			} else if ("KELUAR".equals(jenis)) {
				StokLokasiUtil.catatKeluar(session, lokId, prdId, qty, harga, tgl, ket, null, oleh, olehId);
			} else if ("PENYESUAIAN".equals(jenis)) {
				StokLokasiUtil.catatPenyesuaian(session, lokId, prdId, qty, harga, tgl, ket, oleh, olehId);
			} else {
				StokLokasiUtil.catatMasuk(session, lokId, prdId, qty, harga, tgl, ket, null, oleh, olehId);
			}
			result.put("status", "00"); result.put("message", "Mutasi tersimpan.");
		} catch (IllegalArgumentException iae) {
			result.put("status", "02"); result.put("message", iae.getMessage());
		}

	} else {
		result.put("status", "98"); result.put("message", "Aksi tidak dikenal.");
	}
} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/mutasi_gudang/service.jsp:131");
	try { result.put("status", "99"); result.put("message", "Error: " + e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/mutasi_gudang/service.jsp:132");}
}
out.print(result.toString());
%>
<%!
	/** Transfer bila jenis = TRANSFER. */
	private boolean MutasiLokasiJenisTransfer(String jenis) { return "TRANSFER".equals(jenis); }
%>
