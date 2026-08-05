package ais.action.master.obe;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Matakuliah;
import ais.database.model.PerguruanTinggi;
import ais.database.model.obe.BahanKajian;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.database.model.obe.ProfilLulusan;
import ais.database.model.obe.ReferensiLulusan;

/**
 * Utilitas Download/Upload OBE massal dalam format *.xlsx untuk Dasbor OBE.
 *
 * <p><b>Download (buildWorkbookBytes):</b> menghasilkan 7 sheet —
 * (1) Mapping lengkap PRODI→MK→PL→CPL→CPMK→Sub-CPMK + Bobot + Nilai Minimal,
 * (2) Data PL, (3) Data CPL, (4) Data CPMK, (5) Data Sub-CPMK,
 * (6) Data Bahan Kajian, (7) Data Referensi.</p>
 *
 * <p><b>Upload (importWorkbook):</b> meng-upsert data master PL, CPL, CPMK, dan
 * Bahan Kajian berdasarkan KODE PRODI + KODE (membuat bila belum ada,
 * memperbarui nama bila sudah ada). Bersifat ADITIF & transaksional (tidak
 * menghapus apa pun). Pemetaan relasi (Sheet 1) dan Sub-CPMK (Sheet 5) sengaja
 * BELUM diterapkan otomatis demi keamanan data — lihat catatan di laporan.</p>
 *
 * <p>Gaya Java 1.6/1.7: tanpa lambda, try-with-resources, diamond, atau Stream.</p>
 */
public final class ObeMassExcelHelper {

	public static final String SHEET_MAPPING = "Mapping";
	public static final String SHEET_PL = "Data PL";
	public static final String SHEET_CPL = "Data CPL";
	public static final String SHEET_CPMK = "Data CPMK";
	public static final String SHEET_SUBCPMK = "Data Sub-CPMK";
	public static final String SHEET_BK = "Data Bahan Kajian";
	public static final String SHEET_REF = "Data Referensi";

	private ObeMassExcelHelper() {
	}

	// =====================================================================
	// DOWNLOAD / EXPORT
	// =====================================================================

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static byte[] buildWorkbookBytes() throws Exception {
		Session session = null;
		XSSFWorkbook wb = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			List<ProfilLulusan> plList = session.createCriteria(ProfilLulusan.class)
					.addOrder(Order.asc("kode")).list();
			List<CapaianLulusan> cplList = session.createCriteria(CapaianLulusan.class)
					.addOrder(Order.asc("kode")).list();
			List<CapaianPembelajaranLulusan> cpmkList = session.createCriteria(CapaianPembelajaranLulusan.class)
					.addOrder(Order.asc("kode")).list();
			List<BahanKajian> bkList = session.createCriteria(BahanKajian.class)
					.addOrder(Order.asc("kode")).list();
			List<ReferensiLulusan> refList = session.createCriteria(ReferensiLulusan.class)
					.addOrder(Order.asc("kode")).list();
			List<Matakuliah> mkAll = session.createCriteria(Matakuliah.class)
					.add(Restrictions.isNotNull("capaianLulusan")).list();

			Map<Long, ProfilLulusan> plById = new HashMap<Long, ProfilLulusan>();
			for (ProfilLulusan pl : plList) plById.put(pl.getId(), pl);
			Map<Long, CapaianLulusan> cplById = new HashMap<Long, CapaianLulusan>();
			for (CapaianLulusan c : cplList) cplById.put(c.getId(), c);
			Map<Long, CapaianPembelajaranLulusan> cpmkById = new HashMap<Long, CapaianPembelajaranLulusan>();
			for (CapaianPembelajaranLulusan c : cpmkList) cpmkById.put(c.getId(), c);

			wb = new XSSFWorkbook();

			// ---- Sheet 1: Mapping lengkap ----
			XSSFSheet s1 = wb.createSheet(SHEET_MAPPING);
			writeHeader(s1, new String[] { "KODE PRODI", "KODE MATAKULIAH", "KODE PL", "KODE CPL",
					"KODE CPMK", "KODE SUB-CPMK", "BOBOT", "NILAI MINIMAL" });
			int r = 1;
			for (Matakuliah mk : mkAll) {
				String mkCsvCpl = mk.getCapaianLulusan();
				if (mkCsvCpl == null || mkCsvCpl.trim().length() == 0) continue;
				String mkKode = nvl(mk.getKode());
				for (Long cplId : parseIdsLeading(mkCsvCpl)) {
					CapaianLulusan cpl = cplById.get(cplId);
					if (cpl == null) continue;
					String prodi = jurusanKode(cpl.getJurusan());
					String cplKode = nvl(cpl.getKode());
					String plKode = joinKodes(parseIdsLeading(cpl.getProfil()), plById);
					List<Long> cpmkIds = parseIdsLeading(cpl.getCapaianPembelajaranLulusan());
					if (cpmkIds.isEmpty()) {
						r = writeRow(s1, r, new String[] { prodi, mkKode, plKode, cplKode, "", "", "", "" });
						continue;
					}
					for (Long cpmkId : cpmkIds) {
						CapaianPembelajaranLulusan cpmk = cpmkById.get(cpmkId);
						if (cpmk == null) continue;
						String cpmkKode = nvl(cpmk.getKode());
						List<String[]> subs = parseSubCpmk(cpmk.getFormula());
						if (subs.isEmpty()) {
							r = writeRow(s1, r, new String[] { prodi, mkKode, plKode, cplKode, cpmkKode, "",
									numStr(cpmk.getBobot()), numStr(cpmk.getMinimal()) });
						} else {
							for (String[] sub : subs) {
								r = writeRow(s1, r, new String[] { prodi, mkKode, plKode, cplKode, cpmkKode,
										sub[0], sub[1], sub[2] });
							}
						}
					}
				}
			}

			// ---- Sheet 2: Data PL ----
			XSSFSheet s2 = wb.createSheet(SHEET_PL);
			writeHeader(s2, new String[] { "KODE PRODI", "KODE PL", "NAMA PL", "KHUSUS MK (KODE, JIKA ADA)" });
			int r2 = 1;
			for (ProfilLulusan pl : plList) {
				r2 = writeRow(s2, r2, new String[] { jurusanKode(pl.getJurusan()), nvl(pl.getKode()),
						nvl(pl.getNama()), "" });
			}

			// ---- Sheet 3: Data CPL ----
			XSSFSheet s3 = wb.createSheet(SHEET_CPL);
			writeHeader(s3, new String[] { "KODE PRODI", "KODE CPL", "NAMA CPL", "KHUSUS MK (KODE, JIKA ADA)" });
			int r3 = 1;
			for (CapaianLulusan c : cplList) {
				r3 = writeRow(s3, r3, new String[] { jurusanKode(c.getJurusan()), nvl(c.getKode()),
						nvl(c.getNama()), c.getKhususBuatMk() != null ? nvl(c.getKhususBuatMk().getKode()) : "" });
			}

			// ---- Sheet 4: Data CPMK ----
			XSSFSheet s4 = wb.createSheet(SHEET_CPMK);
			writeHeader(s4, new String[] { "KODE PRODI", "KODE CPMK", "NAMA CPMK", "KHUSUS MK (KODE, JIKA ADA)" });
			int r4 = 1;
			for (CapaianPembelajaranLulusan c : cpmkList) {
				r4 = writeRow(s4, r4, new String[] { jurusanKode(c.getJurusan()), nvl(c.getKode()),
						nvl(c.getNama()), c.getKhususBuatMk() != null ? nvl(c.getKhususBuatMk().getKode()) : "" });
			}

			// ---- Sheet 5: Data Sub-CPMK ----
			XSSFSheet s5 = wb.createSheet(SHEET_SUBCPMK);
			writeHeader(s5, new String[] { "KODE PRODI", "KODE SUB-CPMK", "NAMA SUB-CPMK", "KHUSUS MK (KODE, JIKA ADA)" });
			int r5 = 1;
			for (CapaianPembelajaranLulusan c : cpmkList) {
				String prodi = jurusanKode(c.getJurusan());
				String mkKode = c.getKhususBuatMk() != null ? nvl(c.getKhususBuatMk().getKode()) : "";
				for (String[] sub : parseSubCpmk(c.getFormula())) {
					if (sub[0].length() == 0 && sub[3].length() == 0) continue;
					r5 = writeRow(s5, r5, new String[] { prodi, sub[0], sub[3], mkKode });
				}
			}

			// ---- Sheet 6: Data Bahan Kajian ----
			XSSFSheet s6 = wb.createSheet(SHEET_BK);
			writeHeader(s6, new String[] { "KODE BAHAN KAJIAN", "NAMA BAHAN KAJIAN" });
			int r6 = 1;
			for (BahanKajian bk : bkList) {
				r6 = writeRow(s6, r6, new String[] { nvl(bk.getKode()), nvl(bk.getNama()) });
			}

			// ---- Sheet 7: Data Referensi ----
			XSSFSheet s7 = wb.createSheet(SHEET_REF);
			writeHeader(s7, new String[] { "KODE REFERENSI", "NAMA REFERENSI" });
			int r7 = 1;
			for (ReferensiLulusan ref : refList) {
				r7 = writeRow(s7, r7, new String[] { nvl(ref.getKode()), nvl(ref.getNama()) });
			}

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			bos.flush();
			return bos.toByteArray();
		} finally {
			closeQuietly(session);
		}
	}

	// =====================================================================
	// UPLOAD / IMPORT (upsert master: PL, CPL, CPMK, Bahan Kajian)
	// =====================================================================

	public static String importWorkbook(InputStream in) {
		Session session = null;
		XSSFWorkbook wb = null;
		int[] cnt = new int[2]; // [0]=dibuat, [1]=diperbarui
		try {
			wb = new XSSFWorkbook(in);
			session = HibernateUtil.getSessionFactory().openSession();
			Transaction tx = session.beginTransaction();
			try {
				PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
				importPl(session, pt, wb, cnt);
				importCpl(session, pt, wb, cnt);
				importCpmk(session, pt, wb, cnt);
				importBahanKajian(session, pt, wb, cnt);
				tx.commit();
			} catch (Exception e) {
				try { tx.rollback(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/obe/ObeMassExcelHelper.java:218"); }
				throw e;
			}
			return "Impor selesai. Data dibuat baru: " + cnt[0] + ", diperbarui: " + cnt[1]
					+ ". (PL, CPL, CPMK, Bahan Kajian)";
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return "Gagal mengimpor: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
		} finally {
			if (in != null) {
				try { in.close(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/obe/ObeMassExcelHelper.java:228"); }
			}
			closeQuietly(session);
		}
	}

	private static void importPl(Session session, PerguruanTinggi pt, XSSFWorkbook wb, int[] cnt) {
		XSSFSheet sheet = sheet(wb, SHEET_PL, 1);
		if (sheet == null) return;
		for (int i = 1; i <= sheet.getLastRowNum(); i++) {
			String prodi = cell(sheet, 0, i), kode = cell(sheet, 1, i), nama = cell(sheet, 2, i);
			if (kode.length() == 0) continue;
			Jurusan jur = findJurusan(session, prodi);
			ProfilLulusan e = (ProfilLulusan) findByJurusanKode(session, ProfilLulusan.class, jur, kode);
			boolean baru = e == null;
			if (baru) {
				e = new ProfilLulusan();
				e.setKode(kode);
				e.setJurusan(jur);
				e.setPerguruanTinggi(pt);
				e.setAktif(Boolean.TRUE);
			}
			e.setNama(nama);
			Common.refreshSaveOrUpdate(session, e);
			cnt[baru ? 0 : 1]++;
		}
	}

	private static void importCpl(Session session, PerguruanTinggi pt, XSSFWorkbook wb, int[] cnt) {
		XSSFSheet sheet = sheet(wb, SHEET_CPL, 2);
		if (sheet == null) return;
		for (int i = 1; i <= sheet.getLastRowNum(); i++) {
			String prodi = cell(sheet, 0, i), kode = cell(sheet, 1, i), nama = cell(sheet, 2, i),
					mkKode = cell(sheet, 3, i);
			if (kode.length() == 0) continue;
			Jurusan jur = findJurusan(session, prodi);
			CapaianLulusan e = (CapaianLulusan) findByJurusanKode(session, CapaianLulusan.class, jur, kode);
			boolean baru = e == null;
			if (baru) {
				e = new CapaianLulusan();
				e.setKode(kode);
				e.setJurusan(jur);
				e.setPerguruanTinggi(pt);
				e.setAktif(Boolean.TRUE);
			}
			e.setNama(nama);
			e.setKhususBuatMk(findMatakuliah(session, mkKode));
			Common.refreshSaveOrUpdate(session, e);
			cnt[baru ? 0 : 1]++;
		}
	}

	private static void importCpmk(Session session, PerguruanTinggi pt, XSSFWorkbook wb, int[] cnt) {
		XSSFSheet sheet = sheet(wb, SHEET_CPMK, 3);
		if (sheet == null) return;
		for (int i = 1; i <= sheet.getLastRowNum(); i++) {
			String prodi = cell(sheet, 0, i), kode = cell(sheet, 1, i), nama = cell(sheet, 2, i),
					mkKode = cell(sheet, 3, i);
			if (kode.length() == 0) continue;
			Jurusan jur = findJurusan(session, prodi);
			CapaianPembelajaranLulusan e = (CapaianPembelajaranLulusan) findByJurusanKode(session,
					CapaianPembelajaranLulusan.class, jur, kode);
			boolean baru = e == null;
			if (baru) {
				e = new CapaianPembelajaranLulusan();
				e.setKode(kode);
				e.setJurusan(jur);
				e.setPerguruanTinggi(pt);
				e.setAktif(Boolean.TRUE);
				e.setBobot(Double.valueOf(0.0));
				e.setMinimal(Double.valueOf(0.0));
			}
			e.setNama(nama);
			e.setKhususBuatMk(findMatakuliah(session, mkKode));
			Common.refreshSaveOrUpdate(session, e);
			cnt[baru ? 0 : 1]++;
		}
	}

	private static void importBahanKajian(Session session, PerguruanTinggi pt, XSSFWorkbook wb, int[] cnt) {
		XSSFSheet sheet = sheet(wb, SHEET_BK, 5);
		if (sheet == null) return;
		for (int i = 1; i <= sheet.getLastRowNum(); i++) {
			String kode = cell(sheet, 0, i), nama = cell(sheet, 1, i);
			if (kode.length() == 0) continue;
			BahanKajian e = (BahanKajian) findByKode(session, BahanKajian.class, kode);
			boolean baru = e == null;
			if (baru) {
				e = new BahanKajian();
				e.setKode(kode);
				e.setPerguruanTinggi(pt);
				e.setAktif(Boolean.TRUE);
			}
			e.setNama(nama);
			Common.refreshSaveOrUpdate(session, e);
			cnt[baru ? 0 : 1]++;
		}
	}

	// =====================================================================
	// Helper lookup
	// =====================================================================

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Jurusan findJurusan(Session session, String kode) {
		if (kode == null || kode.trim().length() == 0) return null;
		String k = kode.trim();
		List list = session.createCriteria(Jurusan.class)
				.add(Restrictions.or(Restrictions.eq("kode", k), Restrictions.eq("kodeEpsbed", k)))
				.setMaxResults(1).list();
		return list.isEmpty() ? null : (Jurusan) list.get(0);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Matakuliah findMatakuliah(Session session, String kode) {
		if (kode == null || kode.trim().length() == 0) return null;
		List list = session.createCriteria(Matakuliah.class)
				.add(Restrictions.eq("kode", kode.trim())).setMaxResults(1).list();
		return list.isEmpty() ? null : (Matakuliah) list.get(0);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object findByJurusanKode(Session session, Class clazz, Jurusan jur, String kode) {
		org.hibernate.Criteria c = session.createCriteria(clazz).add(Restrictions.eq("kode", kode.trim()));
		if (jur != null) {
			c.add(Restrictions.eq("jurusan", jur));
		} else {
			c.add(Restrictions.isNull("jurusan"));
		}
		List list = c.setMaxResults(1).list();
		return list.isEmpty() ? null : list.get(0);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object findByKode(Session session, Class clazz, String kode) {
		List list = session.createCriteria(clazz).add(Restrictions.eq("kode", kode.trim()))
				.setMaxResults(1).list();
		return list.isEmpty() ? null : list.get(0);
	}

	// =====================================================================
	// Helper umum
	// =====================================================================

	private static XSSFSheet sheet(XSSFWorkbook wb, String name, int fallbackIndex) {
		XSSFSheet s = wb.getSheet(name);
		if (s == null && fallbackIndex >= 0 && fallbackIndex < wb.getNumberOfSheets()) {
			s = wb.getSheetAt(fallbackIndex);
		}
		return s;
	}

	private static String cell(XSSFSheet sheet, int col, int row) {
		String v = Common.getCellContent(Common.getCell(sheet, Integer.valueOf(col), Integer.valueOf(row)));
		return v == null ? "" : v.trim();
	}

	private static void writeHeader(XSSFSheet sheet, String[] cols) {
		sheet.setDefaultColumnWidth(22);
		XSSFRow row = sheet.createRow(0);
		for (int i = 0; i < cols.length; i++) {
			row.createCell(i).setCellValue(cols[i]);
		}
	}

	private static int writeRow(XSSFSheet sheet, int rowIndex, String[] vals) {
		XSSFRow row = sheet.createRow(rowIndex);
		for (int i = 0; i < vals.length; i++) {
			row.createCell(i).setCellValue(vals[i] == null ? "" : vals[i]);
		}
		return rowIndex + 1;
	}

	private static String jurusanKode(Jurusan j) {
		if (j == null) return "";
		if (j.getKode() != null && j.getKode().trim().length() > 0) return j.getKode().trim();
		return nvl(j.getKodeEpsbed());
	}

	/**
	 * Pecah CSV id; token komposit ber-underscore ("plId_kpmId") diambil segmen
	 * PERTAMA (= id sebenarnya), konsisten dengan parsing di seluruh modul OBE.
	 */
	private static List<Long> parseIdsLeading(String csv) {
		List<Long> ids = new ArrayList<Long>();
		if (csv == null || csv.trim().length() == 0) return ids;
		String[] parts = csv.split(",");
		for (int i = 0; i < parts.length; i++) {
			String t = parts[i] == null ? "" : parts[i].trim();
			if (t.length() == 0) continue;
			int us = t.indexOf('_');
			if (us > 0) t = t.substring(0, us).trim();
			try {
				Long id = Long.valueOf(t);
				if (!ids.contains(id)) ids.add(id);
			} catch (NumberFormatException ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/obe/ObeMassExcelHelper.java:423");
			}
		}
		return ids;
	}

	private static String joinKodes(List<Long> ids, Map<Long, ProfilLulusan> plById) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ids.size(); i++) {
			ProfilLulusan pl = plById.get(ids.get(i));
			if (pl != null) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(nvl(pl.getKode()));
			}
		}
		return sb.toString();
	}

	/** [kode, bobot, minimal, nama] tiap Sub-CPMK dari kolom formula CPMK. */
	private static List<String[]> parseSubCpmk(String formula) {
		List<String[]> result = new ArrayList<String[]>();
		if (formula == null || formula.trim().length() == 0) return result;
		try {
			JSONArray arr = new JSONArray(formula);
			for (int i = 0; i < arr.length(); i++) {
				JSONObject o = arr.getJSONObject(i);
				if (o.isNull("key")) continue;
				String kode = o.isNull("kode") ? "" : ("" + o.get("kode")).trim();
				String nama = o.isNull("nama") ? "" : ("" + o.get("nama")).trim();
				String bobot = o.isNull("bobot") ? "" : numStr(toDouble(o.get("bobot")));
				String minimal = o.isNull("minimal") ? "" : numStr(toDouble(o.get("minimal")));
				result.add(new String[] { kode, bobot, minimal, nama });
			}
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/obe/ObeMassExcelHelper.java:456");
		}
		return result;
	}

	private static Double toDouble(Object o) {
		if (o == null) return null;
		try { return Double.valueOf(("" + o).trim().replace("%", "").replace(",", ".")); }
		catch (Exception e) { return null; }
	}

	private static String numStr(Double d) {
		if (d == null) return "";
		if (d.doubleValue() == Math.floor(d.doubleValue()) && !Double.isInfinite(d.doubleValue())) {
			return String.valueOf((long) d.doubleValue());
		}
		return String.valueOf(d.doubleValue());
	}

	private static String nvl(String s) {
		return s == null ? "" : s;
	}

	private static void closeQuietly(Session session) {
		if (session == null) return;
		try { session.clear(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/obe/ObeMassExcelHelper.java:481"); }
		try { session.disconnect(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/obe/ObeMassExcelHelper.java:482"); }
		try { session.close(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/obe/ObeMassExcelHelper.java:483"); }
	}
}
