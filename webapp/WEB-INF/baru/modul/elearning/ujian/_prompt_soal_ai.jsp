<%@page import="java.util.List"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.Ujian"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.BankSoal"%>
<%@page import="ais.database.model.BankSoalDetail"%>
<%@page import="ais.database.model.UjianPunyaSoal"%>
<%@page import="ais.database.model.PenjelasanBankSoal"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.obe.CapaianPembelajaranLulusan"%>
<%@page import="ais.database.model.obe.BahanKajian"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%><%!
	// Escape aman untuk dimasukkan ke dalam string JSON.
	private String esc(String s) {
		if (s == null) return "";
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"': b.append("\\\""); break;
				case '\\': b.append("\\\\"); break;
				case '\n': b.append("\\n"); break;
				case '\r': b.append("\\r"); break;
				case '\t': b.append("\\t"); break;
				default:
					if (c < 0x20) { b.append(String.format("\\u%04x", (int) c)); }
					else { b.append(c); }
			}
		}
		return b.toString();
	}
	private String bersih(String s) {
		if (s == null) return "";
		return s.replaceAll("<[^>]*>", " ").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
	}
	private String potong(String s, int maks) {
		if (s == null) return "";
		s = s.trim();
		return s.length() > maks ? s.substring(0, maks) + "..." : s;
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
String topik = request.getParameter("topik");
String jumlahStr = request.getParameter("jumlah");
String opsiStr = request.getParameter("opsi");
if (ppuStr == null || ppuStr.trim().isEmpty()) {
	out.print("{\"status\":\"error\",\"message\":\"Parameter ppu tidak valid.\"}");
	return;
}
int jumlah = 1;
try { jumlah = Integer.parseInt(jumlahStr.trim()); } catch (Exception e) { jumlah = 1; }
if (jumlah < 1) jumlah = 1;
if (jumlah > 30) jumlah = 30;
int jmlOpsi = 5;
try { jmlOpsi = Integer.parseInt(opsiStr.trim()); } catch (Exception e) { jmlOpsi = 5; }
if (jmlOpsi < 2) jmlOpsi = 2;
if (jmlOpsi > 8) jmlOpsi = 8;

Session mySession = null;
try {
	mySession = HibernateUtil.openSession();
	PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) mySession.get(PertemuanPunyaUjian.class, Long.parseLong(ppuStr.trim()));
	if (ppu == null || ppu.getUjian() == null) {
		out.print("{\"status\":\"error\",\"message\":\"Data ujian tidak ditemukan.\"}");
		return;
	}
	Ujian ujian = ppu.getUjian();
	boolean isPg = !PenjelasanBankSoal.KOREKSI_MANUAL.equals(ujian.getJenisKoreksi());
	Pertemuan pertemuan = ppu.getPertemuan();
	Matakuliah mk = ujian.getMatakuliah();

	StringBuilder k = new StringBuilder();
	k.append("=== KONTEKS UJIAN & MATA KULIAH ===\n");
	k.append("Nama Ujian/Tugas: ").append(bersih(ujian.getNama())).append("\n");
	if (ppu.getKeterangan() != null && !ppu.getKeterangan().trim().isEmpty()) {
		k.append("Keterangan Ujian: ").append(potong(bersih(ppu.getKeterangan()), 400)).append("\n");
	}
	if (mk != null) {
		k.append("Mata Kuliah: ").append(bersih(mk.getNama()));
		if (mk.getKode() != null) k.append(" (").append(mk.getKode()).append(")");
		k.append("\n");
		if (mk.getSks() != null) k.append("SKS: ").append(mk.getSks()).append("\n");
		if (mk.getKelompokMatakuliah() != null) k.append("Rumpun: ").append(bersih(mk.getKelompokMatakuliah().getNama())).append("\n");
		if (mk.getJurusan() != null) k.append("Program Studi: ").append(bersih(mk.getJurusan().getNama())).append("\n");
		String desk = mk.getDeskripsiPembelajaran();
		if (desk == null || desk.trim().isEmpty()) desk = mk.getKeterangan();
		if (desk != null && !desk.trim().isEmpty()) k.append("Deskripsi MK: ").append(potong(bersih(desk), 700)).append("\n");
		// CPL / CPMK (best-effort dari CSV id)
		try {
			String cplCsv = mk.getCapaianPembelajaranLulusan();
			if (cplCsv != null && !cplCsv.trim().isEmpty()) {
				StringBuilder cpl = new StringBuilder();
				for (String id : cplCsv.split(",")) {
					if (id == null || id.trim().isEmpty()) continue;
					CapaianPembelajaranLulusan cp = (CapaianPembelajaranLulusan) GeneralValueObject.ambilData(CapaianPembelajaranLulusan.class, id.trim(), true);
					if (cp != null) {
						if (cpl.length() > 0) cpl.append("; ");
						cpl.append(bersih(cp.getKode())).append("=").append(potong(bersih(cp.getNama()), 160));
					}
				}
				if (cpl.length() > 0) k.append("Capaian Pembelajaran (CPL/CPMK): ").append(cpl).append("\n");
			}
		} catch (Exception eObe) {}
		try {
			String bkCsv = mk.getBahanKajian();
			if (bkCsv != null && !bkCsv.trim().isEmpty()) {
				StringBuilder bk = new StringBuilder();
				for (String id : bkCsv.split(",")) {
					if (id == null || id.trim().isEmpty()) continue;
					BahanKajian b = (BahanKajian) GeneralValueObject.ambilData(BahanKajian.class, id.trim(), true);
					if (b != null) {
						if (bk.length() > 0) bk.append("; ");
						bk.append(bersih(b.getNama()));
					}
				}
				if (bk.length() > 0) k.append("Bahan Kajian: ").append(potong(bk.toString(), 500)).append("\n");
			}
		} catch (Exception eBk) {}
	}
	if (pertemuan != null) {
		if (pertemuan.getTopik() != null && !pertemuan.getTopik().trim().isEmpty())
			k.append("Topik Pertemuan: ").append(potong(bersih(pertemuan.getTopik()), 400)).append("\n");
		if (pertemuan.getBukuRujukan1() != null && !pertemuan.getBukuRujukan1().trim().isEmpty())
			k.append("Rujukan: ").append(potong(bersih(pertemuan.getBukuRujukan1()), 300)).append("\n");
	}

	// Soal yang sudah ada (anti-duplikat)
	StringBuilder ex = new StringBuilder();
	int nEx = 0;
	try {
		List<?> upsList = mySession.createCriteria(UjianPunyaSoal.class)
				.add(Restrictions.eq("ujian", ujian)).list();
		for (Object o : upsList) {
			UjianPunyaSoal u = (UjianPunyaSoal) o;
			BankSoal bs = u.getBankSoal();
			if (bs == null || bs.getSoal() == null) continue;
			if (nEx >= 40) break;
			nEx++;
			ex.append(nEx).append(". ").append(potong(bersih(bs.getSoal()), 200)).append("\n");
			try {
				List<?> ds = mySession.createCriteria(BankSoalDetail.class)
						.add(Restrictions.eq("bankSoal", bs)).list();
				for (Object od : ds) {
					BankSoalDetail d = (BankSoalDetail) od;
					if (d.getJawaban() != null && !d.getJawaban().trim().isEmpty()) {
						ex.append("   ").append(d.getHuruf() != null ? d.getHuruf() : "-").append(". ")
								.append(potong(bersih(d.getJawaban()), 120))
								.append(Boolean.TRUE.equals(d.getBetul()) ? " (benar)" : "").append("\n");
					}
				}
			} catch (Exception eD) {}
		}
	} catch (Exception eEx) {}

	// Bangun prompt akhir
	StringBuilder p = new StringBuilder();
	p.append(k);
	if (topik != null && !topik.trim().isEmpty()) {
		p.append("\nFokus/Topik soal yang diminta: ").append(bersih(topik)).append("\n");
	}
	if (nEx > 0) {
		p.append("\n=== SOAL YANG SUDAH ADA (JANGAN DUPLIKAT, buat soal BARU yang berbeda) ===\n").append(ex);
	}
	p.append("\n=== TUGAS ===\n");
	p.append("Buatkan ").append(jumlah).append(" butir soal ");
	if (isPg) {
		p.append("PILIHAN GANDA yang berkualitas, relevan dengan konteks di atas, dengan ").append(jmlOpsi)
				.append(" opsi jawaban (A, B, C, ...), tepat satu opsi benar.\n");
		p.append("Balas HANYA dengan JSON array valid (tanpa penjelasan, tanpa markdown), format tiap elemen:\n");
		p.append("{\"soal\":\"teks pertanyaan\",\"opsi\":[{\"huruf\":\"A\",\"teks\":\"...\",\"betul\":true},{\"huruf\":\"B\",\"teks\":\"...\",\"betul\":false}],\"skor\":1}\n");
	} else {
		p.append("URAIAN/ESAI yang berkualitas, relevan dengan konteks di atas, disertai kunci jawaban ringkas.\n");
		p.append("Balas HANYA dengan JSON array valid (tanpa penjelasan, tanpa markdown), format tiap elemen:\n");
		p.append("{\"soal\":\"teks pertanyaan\",\"kunci\":\"kunci jawaban/rubrik ringkas\",\"skor\":1}\n");
	}
	p.append("Gunakan Bahasa Indonesia akademik. Pastikan JSON lengkap dan tertutup dengan benar.");

	out.print("{\"status\":\"success\",\"isPg\":" + isPg + ",\"prompt\":\"" + esc(p.toString()) + "\"}");

} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_prompt_soal_ai.jsp");
	out.print("{\"status\":\"error\",\"message\":\"" + esc(e.getMessage() != null ? e.getMessage() : "error") + "\"}");
} finally {
	if (mySession != null) {
		try { if (mySession.isConnected()) mySession.disconnect(); } catch (Exception ex) {}
		try { if (mySession.isOpen()) mySession.close(); } catch (Exception ex) {}
	}
}
%>
