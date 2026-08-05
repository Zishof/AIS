<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.obe.CapaianPembelajaranLulusan"%>
<%@page import="ais.database.model.obe.BahanKajian"%>
<%@page import="ais.database.model.obe.ReferensiLulusan"%>
<%@page import="ais.action.master.helper.ObeAiJspHelper"%>
<%@page import="ais.action.servlet.AiGenerateServlet"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%><%!
	private String esc(String s) {
		if (s == null) return "";
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++) { char c = s.charAt(i);
			if (c == '"') b.append("\\\""); else if (c == '\\') b.append("\\\\");
			else if (c == '\n') b.append("\\n"); else if (c == '\r') b.append("\\r"); else if (c == '\t') b.append("\\t");
			else if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c);
		}
		return b.toString();
	}
	// Bangun map ref (id->{id,nama}) dari array NOMOR 1-based terhadap list [id,nama]
	private JSONObject bangunMapRef(JSONArray noArr, List<String[]> list) {
		JSONObject map = new JSONObject();
		if (noArr == null) return map;
		for (int i = 0; i < noArr.length(); i++) {
			int no = noArr.optInt(i, -1);
			if (no >= 1 && no <= list.size()) {
				String[] it = list.get(no - 1);
				JSONObject v = new JSONObject();
				v.put("id", it[0]); v.put("nama", it[1]);
				map.put(it[0], v);
			}
		}
		return map;
	}
%><%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) { out.print("{\"status\":\"error\",\"message\":\"Sesi berakhir.\"}"); return; }
String idKpm = request.getParameter("kurikulumPunyaMatakuliah");
String jumlahStr = request.getParameter("jumlah");
String catatan = request.getParameter("catatan");
if (idKpm == null || idKpm.trim().isEmpty()) { out.print("{\"status\":\"error\",\"message\":\"Parameter kpm tidak valid.\"}"); return; }

Session s = null; Transaction tx = null;
int dibuat = 0;
try {
	KurikulumPunyaMatakuliah kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpm.trim(), true);
	if (kpm == null || kpm.getMatakuliah() == null) { out.print("{\"status\":\"error\",\"message\":\"Data matakuliah tidak ditemukan.\"}"); return; }
	Matakuliah mk = kpm.getMatakuliah();
	boolean isNilaiCpmk = kpm.getNilaiMenggunakanCpmk() != null && kpm.getNilaiMenggunakanCpmk();

	// Rincian yang sudah ada + banyakMinggu
	JSONObject rincianMap = new JSONObject();
	try { if (kpm.getRincian() != null && !kpm.getRincian().trim().isEmpty()) rincianMap = new JSONObject(kpm.getRincian()); } catch (Exception e) {}
	int banyakMinggu = 0;
	Iterator<String> itk = rincianMap.keys();
	while (itk.hasNext()) {
		JSONObject o = rincianMap.optJSONObject(itk.next());
		if (o != null) banyakMinggu = Math.max(banyakMinggu, o.optInt("sampaiMingguKe", 0));
	}
	int startWeek = banyakMinggu + 1;
	int jumlah = Math.max(1, 16 - banyakMinggu);
	try { if (jumlahStr != null && !jumlahStr.trim().isEmpty()) jumlah = Integer.parseInt(jumlahStr.trim()); } catch (Exception e) {}
	if (jumlah < 1) jumlah = 1; if (jumlah > 24) jumlah = 24;

	// subList (mirror _rincian_pertemuan_form.jsp arrOpsiCpmk)
	List<String[]> subList = new ArrayList<String[]>();
	String cpmkCsv = mk.getCapaianPembelajaranLulusan();
	if (cpmkCsv != null && !cpmkCsv.trim().isEmpty()) {
		for (String id : cpmkCsv.split(",")) {
			if (id == null || id.trim().isEmpty()) continue;
			CapaianPembelajaranLulusan cp = (CapaianPembelajaranLulusan) GeneralValueObject.ambilData(CapaianPembelajaranLulusan.class, id.trim(), true);
			if (cp == null) continue;
			if (isNilaiCpmk) {
				subList.add(new String[]{ String.valueOf(cp.getId()), (cp.getKode() != null ? cp.getKode() : "") + " " + ObeAiJspHelper.bersih(cp.getNama()) });
			} else if (cp.getFormula() != null && !cp.getFormula().trim().isEmpty()) {
				JSONArray fa = new JSONArray(cp.getFormula());
				for (int i = 0; i < fa.length(); i++) {
					JSONObject fd = fa.optJSONObject(i);
					if (fd == null || fd.optString("key", "").isEmpty()) continue;
					subList.add(new String[]{ fd.optString("key", "") + "_" + cp.getId(), fd.optString("kode", "") + " " + fd.optString("nama", "") });
				}
			}
		}
	}
	// bkList, puList, ppList
	List<String[]> bkList = new ArrayList<String[]>();
	if (mk.getBahanKajian() != null) for (String id : mk.getBahanKajian().split(",")) { if (id.trim().isEmpty()) continue; BahanKajian b = (BahanKajian) GeneralValueObject.ambilData(BahanKajian.class, id.trim(), true); if (b != null) bkList.add(new String[]{ String.valueOf(b.getId()), ObeAiJspHelper.bersih(b.getNama()) }); }
	List<String[]> puList = new ArrayList<String[]>();
	if (kpm.getPustaka() != null) for (String id : kpm.getPustaka().split(",")) { if (id.trim().isEmpty()) continue; ReferensiLulusan r = (ReferensiLulusan) GeneralValueObject.ambilData(ReferensiLulusan.class, id.trim(), true); if (r != null) puList.add(new String[]{ String.valueOf(r.getId()), ObeAiJspHelper.bersih(r.getNama()) }); }
	List<String[]> ppList = new ArrayList<String[]>();
	if (kpm.getPustakaPendukung() != null) for (String id : kpm.getPustakaPendukung().split(",")) { if (id.trim().isEmpty()) continue; ReferensiLulusan r = (ReferensiLulusan) GeneralValueObject.ambilData(ReferensiLulusan.class, id.trim(), true); if (r != null) ppList.add(new String[]{ String.valueOf(r.getId()), ObeAiJspHelper.bersih(r.getNama()) }); }

	// Prompt (mirror ZK bukaGenerateRinciAi)
	StringBuilder p = new StringBuilder();
	p.append("Buat rencana ").append(jumlah).append(" pertemuan RPS untuk mata kuliah \"").append(ObeAiJspHelper.bersih(mk.getNama())).append("\", dimulai dari minggu ke-").append(startWeek).append(".\n\n");
	p.append("Sub-CPMK tersedia (rujuk dengan NOMOR):\n");
	if (subList.isEmpty()) p.append("(belum ada Sub-CPMK)\n");
	else for (int i = 0; i < subList.size(); i++) p.append(i + 1).append(". ").append(subList.get(i)[1]).append("\n");
	if (!bkList.isEmpty()) { p.append("Bahan Kajian tersedia (NOMOR):\n"); for (int i = 0; i < bkList.size(); i++) p.append(i + 1).append(". ").append(bkList.get(i)[1]).append("\n"); }
	if (!puList.isEmpty()) { p.append("Pustaka Utama tersedia (NOMOR):\n"); for (int i = 0; i < puList.size(); i++) p.append(i + 1).append(". ").append(puList.get(i)[1]).append("\n"); }
	if (!ppList.isEmpty()) { p.append("Pustaka Pendukung tersedia (NOMOR):\n"); for (int i = 0; i < ppList.size(); i++) p.append(i + 1).append(". ").append(ppList.get(i)[1]).append("\n"); }
	if (catatan != null && !catatan.trim().isEmpty()) p.append("Catatan tambahan dari dosen: ").append(ObeAiJspHelper.bersih(catatan)).append("\n");
	p.append("\nUntuk TIAP pertemuan: pilih Sub-CPMK yang paling cocok (NOMOR), isi indikator, teknik & kriteria, metode pembelajaran, pembelajaran luring, pembelajaran daring, dan pilih bahan kajian/pustaka relevan (NOMOR, boleh kosong []). Bahasa Indonesia akademis.\n");
	p.append("Keluarkan HANYA JSON array valid berisi ").append(jumlah).append(" objek (tanpa teks lain), format persis:\n");
	p.append("[{\"minggu\":").append(startWeek).append(",\"subCpmkNo\":1,\"indikator\":\"...\",\"teknikDanKriteria\":\"- Kriteria: ...\\n- Bentuk: ...\",\"metodePembelajaran\":\"...\",\"pembelajaranLuring\":\"- Kuliah: ...\\n- Diskusi: ...\",\"pembelajaranDaring\":\"e-Learning: ...\",\"bahanKajianNo\":[1],\"pustakaUtamaNo\":[1],\"pustakaPendukungNo\":[]}]");

	String hasil = AiGenerateServlet.generateText(p.toString(), 8000);
	JSONArray arr = ObeAiJspHelper.ekstrakArray(hasil);
	if (arr.length() == 0) { out.print("{\"status\":\"error\",\"message\":\"Tidak ada pertemuan dari respons AI. Coba lagi.\"}"); return; }

	for (int i = 0; i < arr.length(); i++) {
		JSONObject o = arr.optJSONObject(i);
		if (o == null) continue;
		int minggu = o.optInt("minggu", startWeek + i);
		int no = o.optInt("subCpmkNo", -1);
		String subKey = (no >= 1 && no <= subList.size()) ? subList.get(no - 1)[0] : "-1";
		String subDes = (no >= 1 && no <= subList.size()) ? subList.get(no - 1)[1] : "";
		JSONObject r = new JSONObject();
		r.put("mulaiMingguKe", minggu);
		r.put("sampaiMingguKe", minggu);
		r.put("jumlahCpmk", 1);
		r.put("sub_cpmk", subKey);
		r.put("sub_cpmk_des", subDes);
		r.put("cpmk_des", subDes);
		r.put("indikator", o.optString("indikator", ""));
		r.put("teknikDanKriteria", o.optString("teknikDanKriteria", ""));
		r.put("metodePembelajaran", o.optString("metodePembelajaran", ""));
		r.put("pembelajaranLuring", o.optString("pembelajaranLuring", ""));
		r.put("pembelajaranDaring", o.optString("pembelajaranDaring", ""));
		r.put("bahanKajians", bangunMapRef(o.optJSONArray("bahanKajianNo"), bkList));
		r.put("pustakaUtamas", bangunMapRef(o.optJSONArray("pustakaUtamaNo"), puList));
		r.put("pustakaPendukungs", bangunMapRef(o.optJSONArray("pustakaPendukungNo"), ppList));
		rincianMap.put(Common.getGeneratedBarCode(15), r);
		dibuat++;
	}

	s = HibernateUtil.openSession();
	tx = s.beginTransaction();
	KurikulumPunyaMatakuliah kpmDb = (KurikulumPunyaMatakuliah) s.get(KurikulumPunyaMatakuliah.class, kpm.getId());
	kpmDb.setRincian(rincianMap.toString());
	s.update(kpmDb);
	s.flush();
	tx.commit();
	try { Common.refreshUpdate(kpmDb); } catch (Exception e) {}

	out.print("{\"status\":\"success\",\"dibuat\":" + dibuat + ",\"message\":\"" + esc(dibuat + " pertemuan berhasil dibuat via AI.") + "\"}");
} catch (Exception e) {
	if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ex) {} }
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_generate_rincian_ai.jsp");
	out.print("{\"status\":\"error\",\"message\":\"" + esc(e.getMessage() != null ? e.getMessage() : "error") + "\"}");
} finally {
	if (s != null) { try { if (s.isOpen()) s.close(); } catch (Exception ex) {} }
}
%>
