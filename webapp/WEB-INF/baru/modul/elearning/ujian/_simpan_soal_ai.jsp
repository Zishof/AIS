<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.Ujian"%>
<%@page import="ais.database.model.BankSoal"%>
<%@page import="ais.database.model.BankSoalDetail"%>
<%@page import="ais.database.model.UjianPunyaSoal"%>
<%@page import="ais.database.model.PenjelasanBankSoal"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%><%!
	/**
	 * Ekstraksi objek soal SANGAT tahan-banting dari respons AI. Menangani JSON array rapi,
	 * respons terbungkus prosa/markdown fence, objek tunggal, dan respons TERPOTONG (max_tokens
	 * habis) — tiap objek {...} lengkap tetap diselamatkan. Ini kunci agar "0 soal" tak terjadi.
	 */
	private List<JSONObject> ekstrakObjekSoal(String resp) {
		List<JSONObject> hasil = new ArrayList<JSONObject>();
		if (resp == null || resp.trim().length() == 0) return hasil;
		String s = resp.trim();
		int fence = s.indexOf("```");
		if (fence >= 0) {
			int nl = s.indexOf('\n', fence);
			int fenceAkhir = s.lastIndexOf("```");
			if (nl >= 0 && fenceAkhir > nl) s = s.substring(nl + 1, fenceAkhir).trim();
			else if (nl >= 0) s = s.substring(nl + 1).trim();
		}
		// Jalur cepat: array utuh
		int a = s.indexOf('[');
		int b = s.lastIndexOf(']');
		if (a >= 0 && b > a) {
			try {
				JSONArray arr = new JSONArray(s.substring(a, b + 1));
				for (int i = 0; i < arr.length(); i++) {
					JSONObject o = arr.optJSONObject(i);
					if (o != null && o.has("soal")) hasil.add(o);
				}
				if (!hasil.isEmpty()) return hasil;
			} catch (Exception abaikan) {}
		}
		// Jalur cadangan: pindai brace-depth sadar-string
		int depth = 0, mulai = -1;
		boolean dalamString = false, escape = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (dalamString) {
				if (escape) escape = false;
				else if (c == '\\') escape = true;
				else if (c == '"') dalamString = false;
				continue;
			}
			if (c == '"') dalamString = true;
			else if (c == '{') { if (depth == 0) mulai = i; depth++; }
			else if (c == '}') {
				if (depth > 0) {
					depth--;
					if (depth == 0 && mulai >= 0) {
						try {
							JSONObject o = new JSONObject(s.substring(mulai, i + 1));
							if (o.has("soal")) hasil.add(o);
						} catch (Exception abaikan) {}
						mulai = -1;
					}
				}
			}
		}
		return hasil;
	}
%><%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");

Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	out.print("{\"status\":\"error\",\"message\":\"Sesi berakhir.\"}");
	return;
}

String ppuStr = request.getParameter("ppu");
String respon = request.getParameter("respon");
if (ppuStr == null || ppuStr.trim().isEmpty() || respon == null || respon.trim().isEmpty()) {
	out.print("{\"status\":\"error\",\"message\":\"Parameter tidak lengkap (ppu/respon).\"}");
	return;
}

Session mySession = null;
Transaction tx = null;
int dibuat = 0;
try {
	PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) GeneralValueObject.ambilData(PertemuanPunyaUjian.class, ppuStr.trim(), true);
	if (ppu == null || ppu.getUjian() == null) {
		out.print("{\"status\":\"error\",\"message\":\"Data ujian tidak ditemukan.\"}");
		return;
	}
	Ujian ujian = ppu.getUjian();
	boolean isPg = !PenjelasanBankSoal.KOREKSI_MANUAL.equals(ujian.getJenisKoreksi());

	List<JSONObject> daftar = ekstrakObjekSoal(respon);
	if (daftar.isEmpty()) {
		out.print("{\"status\":\"error\",\"message\":\"Tidak ada soal yang dapat diambil dari respons AI. Coba lagi.\"}");
		return;
	}

	mySession = HibernateUtil.openSession();
	tx = mySession.beginTransaction();

	for (int i = 0; i < daftar.size(); i++) {
		JSONObject o = daftar.get(i);
		if (o == null) continue;
		String soalTeks = o.optString("soal", "").trim();
		if (soalTeks.length() == 0) continue;
		double skor = o.optDouble("skor", 1.0);

		BankSoal bs = new BankSoal();
		bs.setSoal(soalTeks);
		bs.setSkor(skor);
		bs.setSkorSalah(0.0);
		bs.setSkorDefault(0.0);
		bs.setKeterangan("");
		bs.setJenisKoreksi(isPg ? PenjelasanBankSoal.KOREKSI_OTOMATIS : PenjelasanBankSoal.KOREKSI_MANUAL);
		bs.setFakultas(ujian.getFakultas());
		bs.setJurusan(ujian.getJurusan());
		bs.setDosen(ujian.getDosen());
		bs.setGuru(ujian.getGuru());
		bs.setMatakuliah(ujian.getMatakuliah());
		mySession.save(bs);

		if (isPg) {
			JSONArray opsi = o.optJSONArray("opsi");
			int jmlBetul = 0, jmlOpsi = 0;
			if (opsi != null) {
				for (int j = 0; j < opsi.length(); j++) {
					JSONObject op = opsi.optJSONObject(j);
					if (op == null) continue;
					String teks = op.optString("teks", "").trim();
					if (teks.length() == 0) continue;
					String huruf = op.optString("huruf", String.valueOf((char) ('A' + j))).trim();
					boolean betul = op.optBoolean("betul", false);
					if (betul) jmlBetul++;
					jmlOpsi++;
					BankSoalDetail d = new BankSoalDetail();
					d.setBankSoal(bs);
					d.setBetul(betul);
					d.setEssay("");
					d.setHuruf(huruf);
					d.setJawaban(teks);
					d.setKeterangan("");
					mySession.save(d);
				}
			}
			bs.setJenisPilihanGanda(jmlBetul > 1 ? BankSoal.COMBINATION_CHOICE
					: (jmlOpsi == 2 ? BankSoal.BENAR_SALAH : BankSoal.MULTIPLE_COICE));
			mySession.update(bs);
		} else {
			String kunci = o.optString("kunci", "").trim();
			BankSoalDetail d = new BankSoalDetail();
			d.setBankSoal(bs);
			d.setBetul(true);
			d.setEssay(kunci);
			d.setHuruf("");
			d.setJawaban("");
			d.setKeterangan("");
			mySession.save(d);
		}

		UjianPunyaSoal ups = new UjianPunyaSoal();
		ups.setUjian(ujian);
		ups.setBankSoal(bs);
		mySession.save(ups);
		dibuat++;
	}

	mySession.flush();
	tx.commit();
	out.print("{\"status\":\"success\",\"dibuat\":" + dibuat + ",\"message\":\"" + dibuat + " "
			+ Common.getBahasaConfig("soal berhasil dibuat via AI.") + "\"}");

} catch (Exception e) {
	if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ex) {} }
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_simpan_soal_ai.jsp");
	String em = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"").replace("\n", " ") : "Unknown Error";
	out.print("{\"status\":\"error\",\"message\":\"" + em + "\"}");
} finally {
	if (mySession != null) {
		try { if (mySession.isConnected()) mySession.disconnect(); } catch (Exception ex) {}
		try { if (mySession.isOpen()) mySession.close(); } catch (Exception ex) {}
	}
}
%>
