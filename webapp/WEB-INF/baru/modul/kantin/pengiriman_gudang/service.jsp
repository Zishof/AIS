<%@page session="false"%>
<%--
  Endpoint JSON "Pengiriman Antar Gudang" (Gudang Pusat <-> Cabang/Outlet, alur kirim -> dalam
  perjalanan -> terima). Reuse PengirimanGudangUtil (yang sendiri hanya memanggil StokLokasiUtil.catatKeluar/
  catatMasuk yang SUDAH ADA -- lihat javadoc PengirimanGudang utk desain lokasi transit virtual).
  Aksi:
    ref           -> data referensi (lokasi + produk) untuk form.
    kirim         -> buat dokumen pengiriman baru (baris produk dikirim sbg JSON).
    daftarAsal    -> riwayat kirim dari satu lokasi asal.
    daftarTujuan  -> inbox "Perlu Diterima" utk satu lokasi tujuan (opsional filter status).
    detail        -> baris detail + info header satu dokumen (utk form terima).
    terima        -> konfirmasi terima (qty per baris sbg JSON, boleh sebagian).
  Sesi currentSession() (TAK ditutup). Menulis (kirim/terima) HANYA bila LokasiKantinUtil.bolehKelola(request).
--%>
<%@page import="org.json.*"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.asset.Lokasi"%>
<%@page import="ais.database.model.asset.PengirimanGudang"%>
<%@page import="ais.database.model.asset.PengirimanGudangDetail"%>
<%@page import="ais.action.master.koperasi.helper.LokasiKantinUtil"%>
<%@page import="ais.action.master.inventory.PengirimanGudangUtil"%>
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
	// Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request.
	// JANGAN closeSession()/session.close() manual di JSP -> lihat COOKBOOK di HibernateUtil.
	Session session = HibernateUtil.currentNativeSession();
	String oleh = tbmuser.getUserNama() != null ? tbmuser.getUserNama() : String.valueOf(tbmuser.getUserId());
	String olehId = String.valueOf(tbmuser.getUserId());

	if (("kirim".equals(aksi) || "terima".equals(aksi)) && !boleh) {
		result.put("status", "03"); result.put("message", "Hanya admin (bukan pedagang/toko) yang boleh mengelola pengiriman antar gudang.");
		out.print(result.toString()); return;
	}

	if ("ref".equals(aksi)) {
		JSONArray lok = new JSONArray();
		List<Lokasi> lokasis = LokasiKantinUtil.daftarLokasi(session, null, null, true);
		for (Lokasi l : lokasis) {
			// Lokasi transit ("Dalam Perjalanan") sengaja aktif=false -> otomatis tak ikut daftarLokasi(aktifSaja=true).
			JSONObject o = new JSONObject();
			o.put("id", l.getId()); o.put("nama", l.getNama() == null ? "" : l.getNama());
			o.put("jenis", l.getJenisLokasi() == null ? "" : (l.getJenisLokasi().getNama() == null ? "" : l.getJenisLokasi().getNama()));
			lok.put(o);
		}
		JSONArray prd = new JSONArray();
		org.hibernate.SQLQuery q = session.createSQLQuery("select id, kode, nama, coalesce(hargabeli,0) from koperasi.produk where aktif=true order by nama asc");
		q.setMaxResults(5000);
		for (Object row : q.list()) {
			Object[] a = (Object[]) row; JSONObject o = new JSONObject();
			o.put("id", a[0]); o.put("kode", a[1] == null ? "" : a[1].toString());
			o.put("nama", a[2] == null ? "" : a[2].toString()); o.put("hargabeli", ((Number) a[3]).doubleValue());
			prd.put(o);
		}
		result.put("status", "00"); result.put("boleh", boleh); result.put("lokasi", lok); result.put("produk", prd);

	} else if ("kirim".equals(aksi)) {
		Long lokAsal = null, lokTujuan = null;
		try { lokAsal = Long.valueOf(request.getParameter("lokasiAsal").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) pengiriman_gudang/service.jsp:kirim:lokasiAsal"); }
		try { lokTujuan = Long.valueOf(request.getParameter("lokasiTujuan").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) pengiriman_gudang/service.jsp:kirim:lokasiTujuan"); }
		String ket = request.getParameter("keterangan");
		java.util.Date tgl = new java.util.Date();
		try { String s = request.getParameter("tanggal"); if (s != null && s.trim().length() > 0) tgl = new SimpleDateFormat("yyyy-MM-dd").parse(s.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) pengiriman_gudang/service.jsp:kirim:tanggal"); }

		List<PengirimanGudangUtil.DetailKirim> baris = new ArrayList<PengirimanGudangUtil.DetailKirim>();
		try {
			JSONArray arr = new JSONArray(request.getParameter("baris"));
			for (int i = 0; i < arr.length(); i++) {
				JSONObject b = arr.getJSONObject(i);
				Long produkId = Long.valueOf(b.getLong("produk"));
				double qty = b.optDouble("qty", 0);
				Double harga = b.has("harga") && !b.isNull("harga") ? Double.valueOf(b.optDouble("harga", 0)) : null;
				baris.add(new PengirimanGudangUtil.DetailKirim(produkId, qty, harga));
			}
		} catch (Exception e) {
			result.put("status", "02"); result.put("message", "Baris produk tidak valid.");
			out.print(result.toString()); return;
		}

		try {
			PengirimanGudang p = PengirimanGudangUtil.kirim(session, lokAsal, lokTujuan, baris, tgl, ket, oleh, olehId);
			result.put("status", "00"); result.put("message", "Pengiriman " + p.getKode() + " tersimpan.");
			result.put("kode", p.getKode());
		} catch (IllegalArgumentException iae) {
			result.put("status", "02"); result.put("message", iae.getMessage());
		}

	} else if ("daftarAsal".equals(aksi)) {
		Long lokAsal = null;
		try { lokAsal = Long.valueOf(request.getParameter("lokasiAsal").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) pengiriman_gudang/service.jsp:daftarAsal"); }
		JSONArray arr = new JSONArray();
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		for (PengirimanGudang p : PengirimanGudangUtil.daftarUntukLokasiAsal(session, lokAsal)) {
			arr.put(pengirimanKeJson(p, df));
		}
		result.put("status", "00"); result.put("data", arr);

	} else if ("daftarTujuan".equals(aksi)) {
		Long lokTujuan = null;
		try { lokTujuan = Long.valueOf(request.getParameter("lokasiTujuan").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) pengiriman_gudang/service.jsp:daftarTujuan"); }
		String status = request.getParameter("status");
		JSONArray arr = new JSONArray();
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		for (PengirimanGudang p : PengirimanGudangUtil.daftarUntukLokasiTujuan(session, lokTujuan, status)) {
			arr.put(pengirimanKeJson(p, df));
		}
		result.put("status", "00"); result.put("data", arr);

	} else if ("detail".equals(aksi)) {
		Long pengirimanId = null;
		try { pengirimanId = Long.valueOf(request.getParameter("pengiriman").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) pengiriman_gudang/service.jsp:detail"); }
		PengirimanGudang p = pengirimanId == null ? null : (PengirimanGudang) session.get(PengirimanGudang.class, pengirimanId);
		if (p == null) {
			result.put("status", "02"); result.put("message", "Dokumen pengiriman tidak ditemukan.");
			out.print(result.toString()); return;
		}
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		result.put("status", "00");
		result.put("header", pengirimanKeJson(p, df));
		JSONArray baris = new JSONArray();
		for (PengirimanGudangDetail d : PengirimanGudangUtil.detailPengiriman(session, pengirimanId)) {
			JSONObject o = new JSONObject();
			o.put("id", d.getId());
			o.put("produkId", d.getProduk().getId());
			o.put("produk", d.getProduk().getNama() == null ? "" : d.getProduk().getNama());
			o.put("kode", d.getProduk().getKode() == null ? "" : d.getProduk().getKode());
			o.put("qtyKirim", d.getQtyKirim());
			o.put("qtyTerima", d.getQtyTerima());
			baris.put(o);
		}
		result.put("baris", baris);

	} else if ("terima".equals(aksi)) {
		Long pengirimanId = null;
		try { pengirimanId = Long.valueOf(request.getParameter("pengiriman").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) pengiriman_gudang/service.jsp:terima:id"); }
		String ket = request.getParameter("keterangan");
		java.util.Map<Long, Double> qtyMap = new java.util.HashMap<Long, Double>();
		try {
			JSONObject obj = new JSONObject(request.getParameter("qtyTerima"));
			java.util.Iterator<String> keys = obj.keys();
			while (keys.hasNext()) {
				String k = keys.next();
				qtyMap.put(Long.valueOf(k), Double.valueOf(obj.optDouble(k, 0)));
			}
		} catch (Exception e) {
			result.put("status", "02"); result.put("message", "Data jumlah diterima tidak valid.");
			out.print(result.toString()); return;
		}
		try {
			PengirimanGudang p = PengirimanGudangUtil.terima(session, pengirimanId, qtyMap, ket, oleh, olehId);
			result.put("status", "00"); result.put("message", "Penerimaan tersimpan (" + p.getStatus() + ").");
			result.put("dokumenStatus", p.getStatus());
		} catch (IllegalArgumentException iae) {
			result.put("status", "02"); result.put("message", iae.getMessage());
		} catch (IllegalStateException ise) {
			result.put("status", "02"); result.put("message", ise.getMessage());
		}

	} else {
		result.put("status", "98"); result.put("message", "Aksi tidak dikenal.");
	}
} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit pengiriman_gudang/service.jsp");
	try { result.put("status", "99"); result.put("message", "Error: " + e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) pengiriman_gudang/service.jsp:catch"); }
}
out.print(result.toString());
%>
<%!
	/** Ringkasan satu {@code PengirimanGudang} sbg JSON, dipakai daftarAsal/daftarTujuan/detail. */
	private org.json.JSONObject pengirimanKeJson(ais.database.model.asset.PengirimanGudang p, java.text.SimpleDateFormat df) throws Exception {
		org.json.JSONObject o = new org.json.JSONObject();
		o.put("id", p.getId());
		o.put("kode", p.getKode());
		o.put("lokasiAsal", p.getLokasiAsal().getNama());
		o.put("lokasiTujuan", p.getLokasiTujuan().getNama());
		o.put("tanggalKirim", p.getTanggalKirim() == null ? "" : df.format(p.getTanggalKirim()));
		o.put("tanggalTerima", p.getTanggalTerima() == null ? "" : df.format(p.getTanggalTerima()));
		o.put("status", p.getStatus());
		o.put("keterangan", p.getKeterangan() == null ? "" : p.getKeterangan());
		return o;
	}
%>
