<%@page import="java.util.List"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.HasilUjianMahasiswaDetail"%>
<%@page import="ais.database.model.BankSoal"%>
<%@page import="ais.database.model.BankSoalDetail"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.obe.CapaianPembelajaranLulusan"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%><%!
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
					if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
					else b.append(c);
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
	private String konteksMk(Matakuliah mk) {
		if (mk == null) return "";
		StringBuilder k = new StringBuilder();
		k.append("Mata Kuliah: ").append(bersih(mk.getNama()));
		if (mk.getKode() != null) k.append(" (").append(mk.getKode()).append(")");
		k.append("\n");
		if (mk.getJurusan() != null) k.append("Program Studi: ").append(bersih(mk.getJurusan().getNama())).append("\n");
		String desk = mk.getDeskripsiPembelajaran();
		if (desk == null || desk.trim().isEmpty()) desk = mk.getKeterangan();
		if (desk != null && !desk.trim().isEmpty()) k.append("Deskripsi MK: ").append(potong(bersih(desk), 500)).append("\n");
		try {
			String cplCsv = mk.getCapaianPembelajaranLulusan();
			if (cplCsv != null && !cplCsv.trim().isEmpty()) {
				StringBuilder cpl = new StringBuilder();
				for (String id : cplCsv.split(",")) {
					if (id == null || id.trim().isEmpty()) continue;
					CapaianPembelajaranLulusan cp = (CapaianPembelajaranLulusan) GeneralValueObject.ambilData(CapaianPembelajaranLulusan.class, id.trim(), true);
					if (cp != null) {
						if (cpl.length() > 0) cpl.append("; ");
						cpl.append(bersih(cp.getKode())).append("=").append(potong(bersih(cp.getNama()), 140));
					}
				}
				if (cpl.length() > 0) k.append("Capaian Pembelajaran (CPL/CPMK): ").append(cpl).append("\n");
			}
		} catch (Exception e) {}
		return k.toString();
	}
%><%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");

Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	out.print("{\"status\":\"error\",\"message\":\"Sesi berakhir.\"}");
	return;
}
String idDetail = request.getParameter("idDetail");
if (idDetail == null || idDetail.trim().isEmpty()) {
	out.print("{\"status\":\"error\",\"message\":\"Parameter idDetail tidak valid.\"}");
	return;
}

try {
	HasilUjianMahasiswaDetail humd = (HasilUjianMahasiswaDetail) GeneralValueObject.ambilData(HasilUjianMahasiswaDetail.class, idDetail.trim(), true);
	if (humd == null || humd.getBankSoal() == null) {
		out.print("{\"status\":\"error\",\"message\":\"Data jawaban tidak ditemukan.\"}");
		return;
	}
	BankSoal bs = humd.getBankSoal();
	boolean isPg = BankSoal.PILIHAN_GANDA.equals(bs.getJenis());
	double maks = bs.getSkor() != null ? bs.getSkor() : 0.0;
	String soal = bersih(bs.getSoal());
	String jawaban = humd.getJawaban();
	if (jawaban == null || jawaban.trim().isEmpty()) jawaban = "(TIDAK DIJAWAB)";
	jawaban = bersih(jawaban);

	// Kunci / opsi benar
	String kunci = "";
	try {
		List<Long> essayIds = bs.ambilBankSoalDetailEssay(true);
		if (essayIds != null) {
			for (Long did : essayIds) {
				BankSoalDetail d = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class, did.toString());
				if (d != null && d.getEssay() != null && !d.getEssay().trim().isEmpty()) { kunci = bersih(d.getEssay()); break; }
			}
		}
	} catch (Exception e) {}

	String ctx = konteksMk(bs.getMatakuliah());
	StringBuilder p = new StringBuilder();
	if (ctx.length() > 0) p.append("=== KONTEKS MATA KULIAH ===\n").append(ctx).append("\n");
	p.append("=== BUTIR SOAL ===\n").append(potong(soal, 900)).append("\n");
	if (kunci.length() > 0) p.append("Kunci/rubrik: ").append(potong(kunci, 700)).append("\n");
	p.append("\n=== JAWABAN PESERTA ===\n").append(potong(jawaban, 1500)).append("\n");

	if (isPg) {
		// umpan balik penjelasan (skor PG tetap otomatis, tidak diubah)
		BankSoalDetail dipilih = humd.getBankSoalDetail();
		if (dipilih != null) {
			p.append("Opsi dipilih peserta: ").append(dipilih.getHuruf() != null ? dipilih.getHuruf() : "-")
					.append(". ").append(potong(bersih(dipilih.getJawaban()), 200))
					.append(Boolean.TRUE.equals(dipilih.getBetul()) ? " (BENAR)" : " (SALAH)").append("\n");
		}
		p.append("\n=== TUGAS ===\n");
		p.append("Berikan umpan balik/penjelasan singkat (2-4 kalimat) mengapa jawaban peserta benar atau salah, dalam Bahasa Indonesia yang membangun.\n");
		p.append("Balas HANYA JSON valid: {\"koreksi\":\"teks umpan balik\"}");
	} else {
		p.append("\n=== TUGAS ===\n");
		p.append("Nilai jawaban uraian peserta secara objektif berdasarkan kunci/rubrik dan konteks di atas. ");
		p.append("Beri skor angka dari 0 sampai ").append(Common.numberFormat.get().format(maks)).append(" (maksimal ").append(Common.numberFormat.get().format(maks)).append("). ");
		p.append("Jika jawaban '(TIDAK DIJAWAB)' beri skor 0. Sertakan koreksi/alasan singkat yang membangun dalam Bahasa Indonesia.\n");
		p.append("Balas HANYA JSON valid: {\"skor\": angka, \"koreksi\":\"teks koreksi\"}");
	}

	out.print("{\"status\":\"success\",\"isPg\":" + isPg + ",\"maks\":" + maks + ",\"prompt\":\"" + esc(p.toString()) + "\"}");

} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_prompt_koreksi_ai.jsp");
	out.print("{\"status\":\"error\",\"message\":\"" + esc(e.getMessage() != null ? e.getMessage() : "error") + "\"}");
}
%>
