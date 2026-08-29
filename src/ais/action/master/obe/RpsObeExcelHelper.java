package ais.action.master.obe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.obe.BahanKajian;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.database.model.obe.ProfilLulusan;
import ais.database.model.obe.ReferensiLulusan;

/**
 * Workbook RPS OBE per mata kuliah. Delapan sheet data mengikuti delapan tab pada
 * layar RPS OBE. Seluruh file divalidasi sebelum transaksi dimulai dan seluruh
 * perubahan disimpan atomik agar upload yang gagal tidak meninggalkan data parsial.
 */
public final class RpsObeExcelHelper {

	public static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	public static final String FORMAT_VERSION = "AIS-RPS-OBE-2026-V1";
	private static final String GUIDE = "PETUNJUK";
	private static final String MK = "MATA_KULIAH";
	private static final String AUTH = "OTORITAS";
	private static final String PL = "PROFIL_LULUSAN";
	private static final String CPL = "CPL";
	private static final String CPMK = "CPMK_SUB_CPMK";
	private static final String DESC = "DESKRIPSI_PUSTAKA";
	private static final String AGENDA = "RINCIAN_AGENDA";
	private static final String NOTE = "CATATAN_OBE";
	private static final String META = "_RPS_OBE_META";
	private static final int HEADER = 3;
	private static final int FIRST = 4;
	private static final int MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

	private RpsObeExcelHelper() {
	}

	@SuppressWarnings("unchecked")
	public static byte[] exportWorkbook(Long kpmId, Long perkuliahanId) throws Exception {
		if (kpmId == null) throw new IllegalArgumentException("Pilih mata kuliah dan kurikulum terlebih dahulu.");
		Session session = null;
		XSSFWorkbook workbook = new XSSFWorkbook();
		try {
			session = HibernateUtil.openSession();
			KurikulumPunyaMatakuliah kpm = (KurikulumPunyaMatakuliah) session.get(KurikulumPunyaMatakuliah.class, kpmId);
			if (kpm == null || kpm.getMatakuliah() == null) throw new IllegalArgumentException("RPS OBE tidak ditemukan.");
			Perkuliahan perkuliahan = perkuliahanId == null ? null : (Perkuliahan) session.get(Perkuliahan.class, perkuliahanId);
			Matakuliah matakuliah = kpm.getMatakuliah();
			Styles styles = new Styles(workbook);
			writeGuide(workbook, styles);
			writeMk(workbook, styles, kpm);
			writeAuthority(workbook, styles, kpm);

			List<ProfilLulusan> profil = selected(session, ProfilLulusan.class, matakuliah.getProfilLulusan());
			List<CapaianLulusan> cpl = selected(session, CapaianLulusan.class, matakuliah.getCapaianLulusan());
			List<CapaianPembelajaranLulusan> cpmk = selected(session, CapaianPembelajaranLulusan.class,
					matakuliah.getCapaianPembelajaranLulusan());
			Map<Long, String> plCodes = codes(profil);
			Map<Long, String> cpmkCodes = codes(cpmk);
			writePl(workbook, styles, profil, cpl, kpm.getId());
			writeCpl(workbook, styles, cpl, plCodes, cpmkCodes);
			writeCpmk(workbook, styles, cpmk);
			writeDescription(workbook, styles, session, kpm);
			writeAgenda(workbook, styles, session, kpm, cpmk);
			writeNotes(workbook, styles, kpm, perkuliahan);
			writeMeta(workbook, kpm, perkuliahan);
			workbook.setSheetHidden(workbook.getSheetIndex(META), true);
			workbook.setActiveSheet(workbook.getSheetIndex(MK));
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try {
				workbook.write(output);
				return output.toByteArray();
			} finally {
				output.close();
			}
		} finally {
			try { workbook.getPackage().close(); } catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "close workbook export RPS OBE");
			}
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static ImportResult importWorkbook(byte[] bytes, Long expectedKpmId, Long perkuliahanId) throws Exception {
		validateBytes(bytes);
		XSSFWorkbook workbook;
		try {
			workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));
		} catch (Exception e) {
			throw new IllegalArgumentException("File tidak dapat dibaca sebagai XLSX yang valid.", e);
		}
		try {
			validateStructure(workbook, expectedKpmId);
			ImportData data = readAll(workbook);
			data.validate();
			return saveAll(data, expectedKpmId, perkuliahanId);
		} finally {
			try { workbook.getPackage().close(); } catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "close workbook import RPS OBE");
			}
		}
	}

	public static String fileName(KurikulumPunyaMatakuliah kpm) {
		String kode = kpm == null || kpm.getMatakuliah() == null ? "MATAKULIAH" : safeFile(kpm.getMatakuliah().getKode());
		return "RPS_OBE_" + kode + "_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx";
	}

	private static void validateBytes(byte[] bytes) {
		if (bytes == null || bytes.length < 4) throw new IllegalArgumentException("File upload kosong atau rusak.");
		if (bytes.length > MAX_UPLOAD_BYTES) throw new IllegalArgumentException("Ukuran file melebihi 10 MB.");
		if (bytes[0] != 'P' || bytes[1] != 'K') throw new IllegalArgumentException("File harus berformat XLSX.");
	}

	private static void validateStructure(XSSFWorkbook wb, Long expectedKpmId) {
		String[] required = { MK, AUTH, PL, CPL, CPMK, DESC, AGENDA, NOTE, META };
		for (int i = 0; i < required.length; i++) if (wb.getSheet(required[i]) == null)
			throw new IllegalArgumentException("Sheet wajib '" + required[i] + "' tidak ditemukan.");
		String version = value(wb.getSheet(META), 0, 1);
		if (!FORMAT_VERSION.equals(version)) throw new IllegalArgumentException("Versi format RPS OBE tidak didukung: " + version);
		Long fileKpmId = parseLong(value(wb.getSheet(META), 1, 1), "Metadata KPM_ID");
		if (expectedKpmId == null || !expectedKpmId.equals(fileKpmId))
			throw new IllegalArgumentException("File ini berasal dari RPS mata kuliah lain. Download format dari RPS yang sedang dibuka.");
	}

	private static ImportData readAll(XSSFWorkbook wb) {
		ImportData d = new ImportData();
		d.mk = readKeyValues(wb.getSheet(MK), MK);
		d.authority = readKeyValues(wb.getSheet(AUTH), AUTH);
		d.profiles = readMasterRows(wb.getSheet(PL), PL, 7);
		d.cpls = readMasterRows(wb.getSheet(CPL), CPL, 8);
		d.cpmks = readCpmkRows(wb.getSheet(CPMK));
		d.descriptions = readDescriptionRows(wb.getSheet(DESC));
		d.agendas = readAgendaRows(wb.getSheet(AGENDA));
		d.notes = readNoteRows(wb.getSheet(NOTE));
		return d;
	}

	private static ImportResult saveAll(ImportData data, Long kpmId, Long perkuliahanId) throws Exception {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.openSession();
			tx = session.beginTransaction();
			KurikulumPunyaMatakuliah kpm = (KurikulumPunyaMatakuliah) session.get(KurikulumPunyaMatakuliah.class, kpmId);
			if (kpm == null || kpm.getMatakuliah() == null) throw new IllegalArgumentException("RPS OBE tidak ditemukan.");
			if (kpm.getDikunci() != null) throw new IllegalArgumentException("RPS OBE sudah dikunci dan tidak dapat di-upload.");
			Matakuliah mk = kpm.getMatakuliah();
			Jurusan jurusan = mk.getJurusan() != null ? mk.getJurusan()
					: (kpm.getKurikulum() == null ? null : kpm.getKurikulum().getJurusan());
			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
			Counter counter = new Counter();

			applyBase(data, kpm);
			Map<String, ProfilLulusan> pl = saveProfiles(session, data.profiles, jurusan, pt, counter);
			Map<String, CapaianLulusan> cpl = saveCpls(session, data.cpls, jurusan, pt, mk, counter);
			Map<String, CapaianPembelajaranLulusan> cpmk = saveCpmks(session, data.cpmks, jurusan, pt, mk, counter);
			mk.setProfilLulusan(csvIds(pl.values()));
			mk.setCapaianLulusan(csvIds(cpl.values()));
			mk.setCapaianPembelajaranLulusan(csvIds(cpmk.values()));
			applyRelations(data, pl, cpl, cpmk, kpm.getId());
			applyDescription(session, data.descriptions, kpm, mk, jurusan, pt, counter);
			kpm.setRincian(buildAgendaJson(data.agendas, cpmk, session));
			applyNotes(data.notes, kpm);
			Perkuliahan perkuliahan = perkuliahanId == null ? null : (Perkuliahan) session.get(Perkuliahan.class, perkuliahanId);
			if (perkuliahan != null) perkuliahan.setCqiData(buildCqiJson(data.notes));

			session.update(mk);
			session.update(kpm);
			if (perkuliahan != null) session.update(perkuliahan);
			session.flush();
			tx.commit();
			return new ImportResult(data.profiles.size(), data.cpls.size(), data.cpmks.size(), data.agendas.size(),
					counter.inserted, counter.updated);
		} catch (Exception e) {
			if (tx != null) try { tx.rollback(); } catch (Exception rollback) {
				ais.common.ErrorAuditUtil.record(rollback, "rollback upload RPS OBE");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static void applyBase(ImportData d, KurikulumPunyaMatakuliah kpm) throws Exception {
		kpm.setTanggalPenyusunan(parseDate(d.mk.get("TANGGAL_PENYUSUNAN"), "TANGGAL_PENYUSUNAN"));
		kpm.setMinimalKetercapaian(parseDoubleNullable(d.mk.get("MINIMAL_KETERCAPAIAN"), "MINIMAL_KETERCAPAIAN"));
		kpm.setNilaiMenggunakanCpmk(parseBoolean(d.mk.get("NILAI_MENGGUNAKAN_CPMK"), "NILAI_MENGGUNAKAN_CPMK"));
		kpm.setPengembangRps(trim(d.authority.get("PENGEMBANG_RPS")));
		kpm.setKoordinator(trim(d.authority.get("KOORDINATOR")));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, ProfilLulusan> saveProfiles(Session s, List<MasterRow> rows, Jurusan jur, PerguruanTinggi pt, Counter c) {
		List<ProfilLulusan> all = ConstantValues.simpleList(s.createCriteria(ProfilLulusan.class), ProfilLulusan.class);
		Map<String, ProfilLulusan> result = new LinkedHashMap<String, ProfilLulusan>();
		for (MasterRow r : rows) {
			ProfilLulusan x = r.id == null ? findProfile(all, jur, r.code) : (ProfilLulusan) s.get(ProfilLulusan.class, r.id);
			boolean insert = x == null;
			if (insert) { x = new ProfilLulusan(); x.setJurusan(jur); x.setPerguruanTinggi(pt); }
			x.setKode(r.code); x.setNama(r.name); x.setKeterangan(nullIfEmpty(r.description));
			x.setReferensi(nullIfEmpty(r.reference)); x.setAktif(r.active);
			if (insert) { s.save(x); all.add(x); c.inserted++; } else { s.update(x); c.updated++; }
			result.put(key(r.code), x);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, CapaianLulusan> saveCpls(Session s, List<MasterRow> rows, Jurusan jur,
			PerguruanTinggi pt, Matakuliah mk, Counter c) {
		List<CapaianLulusan> all = ConstantValues.simpleList(s.createCriteria(CapaianLulusan.class), CapaianLulusan.class);
		Map<String, CapaianLulusan> result = new LinkedHashMap<String, CapaianLulusan>();
		for (MasterRow r : rows) {
			CapaianLulusan x = r.id == null ? findCpl(all, jur, r.code) : (CapaianLulusan) s.get(CapaianLulusan.class, r.id);
			boolean insert = x == null;
			if (insert) { x = new CapaianLulusan(); x.setJurusan(jur); x.setPerguruanTinggi(pt); x.setKhususBuatMk(mk); }
			x.setKode(r.code); x.setNama(r.name); x.setKeterangan(nullIfEmpty(r.description));
			x.setKategori(nullIfEmpty(r.category)); x.setReferensi(nullIfEmpty(r.reference)); x.setAktif(r.active);
			if (insert) { s.save(x); all.add(x); c.inserted++; } else { s.update(x); c.updated++; }
			result.put(key(r.code), x);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, CapaianPembelajaranLulusan> saveCpmks(Session s, List<CpmkRow> rows,
			Jurusan jur, PerguruanTinggi pt, Matakuliah mk, Counter c) throws Exception {
		List<CapaianPembelajaranLulusan> all = ConstantValues.simpleList(
				s.createCriteria(CapaianPembelajaranLulusan.class), CapaianPembelajaranLulusan.class);
		Map<String, CapaianPembelajaranLulusan> result = new LinkedHashMap<String, CapaianPembelajaranLulusan>();
		Map<String, List<CpmkRow>> groups = groupCpmk(rows);
		for (Map.Entry<String, List<CpmkRow>> e : groups.entrySet()) {
			CpmkRow first = e.getValue().get(0);
			CapaianPembelajaranLulusan x = first.id == null ? findCpmk(all, jur, first.cpmkCode)
					: (CapaianPembelajaranLulusan) s.get(CapaianPembelajaranLulusan.class, first.id);
			boolean insert = x == null;
			if (insert) { x = new CapaianPembelajaranLulusan(); x.setJurusan(jur); x.setPerguruanTinggi(pt); x.setKhususBuatMk(mk); }
			x.setKode(first.cpmkCode); x.setNama(first.cpmkName); x.setAktif(Boolean.TRUE);
			x.setBobot(first.cpmkWeight); x.setMinimal(first.cpmkMinimum);
			JSONArray formula = new JSONArray();
			int n = 0;
			for (CpmkRow row : e.getValue()) if (notEmpty(row.subCode) || notEmpty(row.subName)) {
				JSONObject sub = new JSONObject(); sub.put("key", ++n); sub.put("kode", row.subCode);
				sub.put("nama", row.subName); sub.put("bobot", zero(row.subWeight)); sub.put("minimal", zero(row.subMinimum));
				Map<String, Double> mappings = parseWeights(row.mapping, row.location);
				int idx = 0;
				for (String code : groups.keySet()) sub.put("bobot_index_" + idx++, zero(mappings.get(key(code))));
				formula.put(sub);
			}
			x.setFormula(formula.toString());
			if (insert) { s.save(x); all.add(x); c.inserted++; } else { s.update(x); c.updated++; }
			result.put(key(first.cpmkCode), x);
		}
		return result;
	}

	private static void applyRelations(ImportData d, Map<String, ProfilLulusan> profiles,
			Map<String, CapaianLulusan> cpls, Map<String, CapaianPembelajaranLulusan> cpmks, Long kpmId) {
		Map<String, MasterRow> plRows = byCode(d.profiles);
		for (MasterRow cr : d.cpls) {
			CapaianLulusan cpl = cpls.get(key(cr.code));
			String profileCsv = removeKpmTokens(cpl.getProfil(), kpmId);
			for (MasterRow pr : d.profiles) if (containsCode(pr.relations, cr.code)) {
				ProfilLulusan pl = profiles.get(key(pr.code));
				profileCsv = appendCsv(profileCsv, pl.getId() + "_" + kpmId);
			}
			cpl.setProfil(profileCsv);
			String cpmkCsv = "";
			for (String code : splitCodes(cr.relations)) {
				CapaianPembelajaranLulusan cp = cpmks.get(key(code));
				if (cp != null) cpmkCsv = appendCsv(cpmkCsv, String.valueOf(cp.getId()));
			}
			cpl.setCapaianPembelajaranLulusan(cpmkCsv);
		}
	}

	@SuppressWarnings("unchecked")
	private static void applyDescription(Session s, List<DescriptionRow> rows, KurikulumPunyaMatakuliah kpm,
			Matakuliah mk, Jurusan jur, PerguruanTinggi pt, Counter counter) {
		Map<String, String> fields = new HashMap<String, String>();
		Map<String, BahanKajian> bks = new LinkedHashMap<String, BahanKajian>();
		Map<String, ReferensiLulusan> utama = new LinkedHashMap<String, ReferensiLulusan>();
		Map<String, ReferensiLulusan> pendukung = new LinkedHashMap<String, ReferensiLulusan>();
		List<BahanKajian> allBk = ConstantValues.simpleList(s.createCriteria(BahanKajian.class), BahanKajian.class);
		List<ReferensiLulusan> allRef = ConstantValues.simpleList(s.createCriteria(ReferensiLulusan.class), ReferensiLulusan.class);
		for (DescriptionRow r : rows) {
			if ("FIELD".equals(r.type)) { fields.put(r.code, r.name); continue; }
			if ("BAHAN_KAJIAN".equals(r.type)) {
				BahanKajian x = findBk(allBk, jur, r.code, r.name); boolean insert = x == null;
				if (insert) { x = new BahanKajian(); x.setKode(r.code); x.setJurusan(jur); x.setPerguruanTinggi(pt); x.setKhususBuatMk(mk); }
				x.setNama(r.name); x.setKeterangan(nullIfEmpty(r.description)); x.setAktif(r.active);
				if (insert) { s.save(x); allBk.add(x); counter.inserted++; } else { s.update(x); counter.updated++; }
				bks.put(key(r.code), x);
			} else if ("PUSTAKA_UTAMA".equals(r.type) || "PUSTAKA_PENDUKUNG".equals(r.type)) {
				ReferensiLulusan x = findRef(allRef, r.code, r.name); boolean insert = x == null;
				if (insert) { x = new ReferensiLulusan(); x.setKode(r.code); x.setPerguruanTinggi(pt); }
				x.setNama(r.name); x.setKeterangan(nullIfEmpty(r.description)); x.setAktif(r.active);
				if (insert) { s.save(x); allRef.add(x); counter.inserted++; } else { s.update(x); counter.updated++; }
				("PUSTAKA_UTAMA".equals(r.type) ? utama : pendukung).put(key(r.code), x);
			}
		}
		kpm.setDeskripsiPembelajaran(trim(fields.get("DESKRIPSI_PEMBELAJARAN")));
		kpm.setMitraPengembang(trim(fields.get("MITRA_PENGEMBANG")));
		kpm.setMkPrasyarat(findCourseIds(s, fields.get("MK_PRASYARAT_KODE")));
		kpm.setDosen(findLecturerIds(s, fields.get("DOSEN_ID")));
		mk.setBahanKajian(csvIds(bks.values()));
		kpm.setPustaka(csvIds(utama.values()));
		kpm.setPustakaPendukung(csvIds(pendukung.values()));
	}

	private static String buildAgendaJson(List<AgendaRow> rows,
			Map<String, CapaianPembelajaranLulusan> cpmks, Session session) throws Exception {
		JSONObject root = new JSONObject(); int n = 0;
		Map<String, JSONObject> subByCode = subCpmkLookup(cpmks);
		for (AgendaRow r : rows) {
			JSONObject o = new JSONObject(); o.put("keyData", "excel_" + (++n)); o.put("mulaiMingguKe", r.start);
			o.put("sampaiMingguKe", r.end); int count = 0;
			for (int i = 0; i < r.subCodes.length; i++) if (notEmpty(r.subCodes[i])) {
				JSONObject sub = subByCode.get(key(r.subCodes[i]));
				if (sub == null) throw new IllegalArgumentException(r.location + ": Sub-CPMK '" + r.subCodes[i] + "' tidak ditemukan.");
				count++; String suffix = count == 1 ? "" : String.valueOf(count);
				o.put("sub_cpmk" + suffix, sub.get("value")); o.put("sub_cpmk_des" + suffix, sub.getString("label"));
				o.put("cpmk_des" + suffix, sub.getString("label"));
			}
			o.put("jumlahCpmk", Math.max(1, count)); o.put("indikator", r.indicator); o.put("teknikDanKriteria", r.criteria);
			o.put("metodePembelajaran", r.method); o.put("pembelajaranLuring", r.offline);
			o.put("pembelajaranDaring", r.online); o.put("pengalamanBelajar", r.experience);
			o.put("bahanKajians", referenceJson(session, BahanKajian.class, r.bahanKajian));
			o.put("pustakaUtamas", referenceJson(session, ReferensiLulusan.class, r.pustakaUtama));
			o.put("pustakaPendukungs", referenceJson(session, ReferensiLulusan.class, r.pustakaPendukung));
			root.put("excel_" + n, o);
		}
		return root.toString();
	}

	private static void applyNotes(List<NoteRow> rows, KurikulumPunyaMatakuliah kpm) {
		Map<String, String> m = new HashMap<String, String>();
		for (NoteRow r : rows) if ("FIELD".equals(r.type)) m.put(r.key, r.value);
		kpm.setCatatan(trim(m.get("CATATAN"))); kpm.setCplBobot(trim(m.get("BOBOT_CPL")));
		kpm.setKomponenPenilaian(trim(m.get("KOMPONEN_PENILAIAN"))); kpm.setTeknikPerCpmk(trim(m.get("TEKNIK_PER_CPMK")));
		kpm.setRubrikPenilaian(trim(m.get("RUBRIK_PENILAIAN"))); kpm.setPemetaanSoalUts(trim(m.get("PEMETAAN_SOAL_UTS")));
		kpm.setPemetaanSoalUas(trim(m.get("PEMETAAN_SOAL_UAS")));
	}

	private static String buildCqiJson(List<NoteRow> rows) throws Exception {
		JSONArray result = new JSONArray();
		for (NoteRow r : rows) if ("CQI".equals(r.type)) {
			JSONObject o = new JSONObject(); o.put("cpmk", r.key); o.put("masalah", r.problem);
			o.put("analisis", r.analysis); o.put("rencana", r.plan); o.put("pj", r.owner);
			o.put("targetWaktu", r.target); o.put("status", notEmpty(r.status) ? r.status : "Planned");
			o.put("adminKomentar", r.adminComment); result.put(o);
		}
		return result.toString();
	}

	/* ---------------------------- workbook writer ---------------------------- */

	private static void writeGuide(XSSFWorkbook wb, Styles st) {
		XSSFSheet sh = wb.createSheet(GUIDE); sh.setDisplayGridlines(false);
		title(sh, st, "FORMAT DOWNLOAD / UPLOAD RPS OBE", 8);
		String[] notes = {
			"Workbook ini mewakili seluruh delapan tab RPS OBE pada satu mata kuliah.",
			"Isi sel kuning. Jangan mengganti nama sheet, kode field, atau baris metadata.",
			"Kode PL/CPL/CPMK/Sub-CPMK harus unik. Pisahkan daftar kode relasi dengan koma.",
			"Tanggal memakai format dd-MM-yyyy. Nilai bobot/minimal berupa angka tanpa tanda persen.",
			"Upload melakukan tambah/perbarui tanpa menghapus master OBE lain yang tidak ada pada file.",
			"Seluruh sheet diperiksa lebih dahulu lalu disimpan dalam satu transaksi. Satu kesalahan membatalkan semuanya.",
			"File hanya dapat di-upload kembali pada RPS asalnya dan RPS harus belum dikunci.",
			"Simpan sebagai XLSX dengan ukuran maksimum 10 MB."
		};
		for (int i = 0; i < notes.length; i++) { sh.addMergedRegion(new CellRangeAddress(2 + i, 2 + i, 0, 7)); cell(sh, 2 + i, 0, (i + 1) + ". " + notes[i]).setCellStyle(st.note); }
		widths(sh, new int[] { 22,22,22,22,22,22,22,22 });
	}

	private static void writeMk(XSSFWorkbook wb, Styles st, KurikulumPunyaMatakuliah kpm) {
		XSSFSheet sh = keyValueSheet(wb, st, MK, "1. MATA KULIAH"); int r = FIRST;
		r = kv(sh, st, r, "KODE_MATA_KULIAH", kpm.getMatakuliah().getKode(), "Identitas; tidak diubah saat upload", false);
		r = kv(sh, st, r, "NAMA_MATA_KULIAH", kpm.getMatakuliah().getNama(), "Identitas; tidak diubah saat upload", false);
		r = kv(sh, st, r, "TANGGAL_PENYUSUNAN", date(kpm.getTanggalPenyusunan()), "dd-MM-yyyy", true);
		r = kv(sh, st, r, "MINIMAL_KETERCAPAIAN", kpm.getMinimalKetercapaian(), "Angka 0-100", true);
		r = kv(sh, st, r, "NILAI_MENGGUNAKAN_CPMK", yesNo(kpm.getNilaiMenggunakanCpmk()), "Ya/Tidak", true);
		finish(sh, r, 2, new int[] { 32, 60, 55 }); addYesNo(sh, 1, FIRST + 4, FIRST + 4);
	}

	private static void writeAuthority(XSSFWorkbook wb, Styles st, KurikulumPunyaMatakuliah kpm) {
		XSSFSheet sh = keyValueSheet(wb, st, AUTH, "2. OTORITAS"); int r = FIRST;
		r = kv(sh, st, r, "PENGEMBANG_RPS", kpm.getPengembangRps(), "Nama penyusun/pengembang RPS", true);
		r = kv(sh, st, r, "KOORDINATOR", kpm.getKoordinator(), "Nama koordinator mata kuliah", true);
		String kaprodi = kpm.getKurikulum() == null || kpm.getKurikulum().getJurusan() == null
				|| kpm.getKurikulum().getJurusan().getKaprodi() == null ? "" : kpm.getKurikulum().getJurusan().getKaprodi().getNama();
		r = kv(sh, st, r, "KETUA_PROGRAM_STUDI", kaprodi, "Diambil dari master program studi", false);
		finish(sh, r, 2, new int[] { 32, 60, 55 });
	}

	private static void writePl(XSSFWorkbook wb, Styles st, List<ProfilLulusan> rows,
			List<CapaianLulusan> cpls, Long kpmId) {
		XSSFSheet sh = dataSheet(wb, st, PL, "3. PROFIL LULUSAN",
				new String[] { "ID Sistem", "Kode PL", "Nama Profil Lulusan", "Keterangan", "Referensi", "Kode CPL Terkait", "Aktif" });
		int r = FIRST; for (ProfilLulusan p : rows) { XSSFRow row = sh.createRow(r++);
			put(row,0,p.getId(),st.readOnly); put(row,1,p.getKode(),st.input); put(row,2,p.getNama(),st.input);
			put(row,3,p.getKeterangan(),st.input); put(row,4,p.getReferensi(),st.input);
			List<String> related = new ArrayList<String>(); String token = p.getId() + "_" + kpmId;
			for (CapaianLulusan c : cpls) if (containsCsv(c.getProfil(), token) || containsCsv(c.getProfil(), String.valueOf(p.getId()))) related.add(c.getKode());
			put(row,5,join(related),st.input); put(row,6,yesNo(p.getAktif()),st.inputCenter); }
		finish(sh,r,6,new int[]{14,18,45,55,45,35,12}); addYesNo(sh,6,FIRST,Math.max(FIRST+200,r));
	}

	private static void writeCpl(XSSFWorkbook wb, Styles st, List<CapaianLulusan> rows,
			Map<Long,String> plCodes, Map<Long,String> cpmkCodes) {
		XSSFSheet sh = dataSheet(wb, st, CPL, "4. CAPAIAN PEMBELAJARAN LULUSAN (CPL)",
				new String[]{"ID Sistem","Kode CPL","Nama CPL","Keterangan","Kategori","Referensi","Kode CPMK Terkait","Aktif"});
		int r=FIRST; for(CapaianLulusan c:rows){XSSFRow row=sh.createRow(r++); put(row,0,c.getId(),st.readOnly);
			put(row,1,c.getKode(),st.input);put(row,2,c.getNama(),st.input);put(row,3,c.getKeterangan(),st.input);
			put(row,4,c.getKategori(),st.input);put(row,5,c.getReferensi(),st.input);
			put(row,6,codesFromCsv(c.getCapaianPembelajaranLulusan(),cpmkCodes),st.input);put(row,7,yesNo(c.getAktif()),st.inputCenter);}
		finish(sh,r,7,new int[]{14,18,60,55,20,45,35,12}); addYesNo(sh,7,FIRST,Math.max(FIRST+200,r));
	}

	private static void writeCpmk(XSSFWorkbook wb, Styles st, List<CapaianPembelajaranLulusan> rows) {
		XSSFSheet sh=dataSheet(wb,st,CPMK,"5. CPMK DAN SUB-CPMK",new String[]{"ID CPMK","Kode CPMK","Nama CPMK","Bobot CPMK","Minimal CPMK","Kode Sub-CPMK","Nama Sub-CPMK","Bobot Sub-CPMK","Minimal Sub-CPMK","Bobot Pemetaan per CPMK"});
		int r=FIRST; for(CapaianPembelajaranLulusan c:rows){JSONArray a=array(c.getFormula()); if(a.length()==0){XSSFRow row=sh.createRow(r++); writeCpmkBase(row,st,c);}
			for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;XSSFRow row=sh.createRow(r++);writeCpmkBase(row,st,c);
				put(row,5,o.optString("kode",""),st.input);put(row,6,o.optString("nama",""),st.input);put(row,7,o.optDouble("bobot",0),st.inputCenter);put(row,8,o.optDouble("minimal",0),st.inputCenter);
				List<String> maps=new ArrayList<String>();for(int x=0;x<rows.size();x++){double v=o.optDouble("bobot_index_"+x,0);if(v!=0)maps.add(rows.get(x).getKode()+":"+number(v));}put(row,9,join(maps),st.input);}}
		finish(sh,r,9,new int[]{14,18,55,16,16,20,60,18,18,45});
	}

	private static void writeCpmkBase(XSSFRow row,Styles st,CapaianPembelajaranLulusan c){put(row,0,c.getId(),st.readOnly);put(row,1,c.getKode(),st.input);put(row,2,c.getNama(),st.input);put(row,3,c.getBobot(),st.inputCenter);put(row,4,c.getMinimal(),st.inputCenter);}

	private static void writeDescription(XSSFWorkbook wb, Styles st, Session s, KurikulumPunyaMatakuliah kpm) {
		XSSFSheet sh=dataSheet(wb,st,DESC,"6. DESKRIPSI DAN PUSTAKA",new String[]{"Tipe Data","Kode/Field","Nama/Nilai","Keterangan","Aktif"});int r=FIRST;
		r=desc(sh,st,r,"FIELD","DESKRIPSI_PEMBELAJARAN",kpm.getDeskripsiPembelajaran(),"Deskripsi mata kuliah",true);
		r=desc(sh,st,r,"FIELD","MITRA_PENGEMBANG",kpm.getMitraPengembang(),"Mitra pengembang RPS",true);
		r=desc(sh,st,r,"FIELD","MK_PRASYARAT_KODE",courseCodes(s,kpm.getMkPrasyarat()),"Kode mata kuliah dipisah koma",true);
		r=desc(sh,st,r,"FIELD","DOSEN_ID",lecturerIds(kpm.getDosen()),"ID dosen pengembang dipisah koma",true);
		List<BahanKajian> bks=selected(s,BahanKajian.class,kpm.getMatakuliah().getBahanKajian());for(BahanKajian x:bks)r=desc(sh,st,r,"BAHAN_KAJIAN",x.getKode(),x.getNama(),x.getKeterangan(),!Boolean.FALSE.equals(x.getAktif()));
		List<ReferensiLulusan> utama=selected(s,ReferensiLulusan.class,kpm.getPustaka());for(ReferensiLulusan x:utama)r=desc(sh,st,r,"PUSTAKA_UTAMA",x.getKode(),x.getNama(),x.getKeterangan(),!Boolean.FALSE.equals(x.getAktif()));
		List<ReferensiLulusan> pend=selected(s,ReferensiLulusan.class,kpm.getPustakaPendukung());for(ReferensiLulusan x:pend)r=desc(sh,st,r,"PUSTAKA_PENDUKUNG",x.getKode(),x.getNama(),x.getKeterangan(),!Boolean.FALSE.equals(x.getAktif()));
		finish(sh,r,4,new int[]{24,28,75,70,12});addYesNo(sh,4,FIRST,Math.max(FIRST+300,r));
	}

	private static void writeAgenda(XSSFWorkbook wb,Styles st,Session session,KurikulumPunyaMatakuliah kpm,List<CapaianPembelajaranLulusan> cpmks) throws Exception {
		XSSFSheet sh=dataSheet(wb,st,AGENDA,"7. RINCIAN / AGENDA",new String[]{"Kunci","Mulai Minggu","Sampai Minggu","Sub-CPMK 1","Sub-CPMK 2","Sub-CPMK 3","Sub-CPMK 4","Sub-CPMK 5","Indikator","Teknik & Kriteria","Metode Pembelajaran","Pembelajaran Luring","Pembelajaran Daring","Pengalaman Belajar","Kode Bahan Kajian","Kode Pustaka Utama","Kode Pustaka Pendukung"});
		JSONObject root=object(kpm.getRincian());List<JSONObject> items=agendaItems(root);int r=FIRST;for(JSONObject o:items){XSSFRow row=sh.createRow(r++);put(row,0,o.optString("keyData",""),st.readOnly);put(row,1,o.optInt("mulaiMingguKe",0),st.inputCenter);put(row,2,o.optInt("sampaiMingguKe",0),st.inputCenter);
			for(int i=0;i<5;i++){String suf=i==0?"":String.valueOf(i+1);put(row,3+i,codeFromLabel(o.optString("sub_cpmk_des"+suf,"")),st.input);}
			put(row,8,o.optString("indikator",""),st.input);put(row,9,o.optString("teknikDanKriteria",""),st.input);put(row,10,o.optString("metodePembelajaran",""),st.input);put(row,11,o.optString("pembelajaranLuring",""),st.input);put(row,12,o.optString("pembelajaranDaring",""),st.input);put(row,13,o.optString("pengalamanBelajar",""),st.input);put(row,14,jsonCodes(session,BahanKajian.class,o.optJSONObject("bahanKajians")),st.input);put(row,15,jsonCodes(session,ReferensiLulusan.class,o.optJSONObject("pustakaUtamas")),st.input);put(row,16,jsonCodes(session,ReferensiLulusan.class,o.optJSONObject("pustakaPendukungs")),st.input);}
		finish(sh,r,16,new int[]{18,15,15,18,18,18,18,18,55,55,45,45,45,45,35,35,35});
	}

	private static void writeNotes(XSSFWorkbook wb,Styles st,KurikulumPunyaMatakuliah kpm,Perkuliahan perkuliahan){
		XSSFSheet sh=dataSheet(wb,st,NOTE,"8. CATATAN DAN DATA OBE",new String[]{"Tipe Data","Kunci/Kode CPMK","Nilai","Masalah CQI","Analisis","Rencana Tindak Lanjut","Penanggung Jawab","Target Waktu","Status","Evaluasi Admin"});int r=FIRST;
		r=note(sh,st,r,"CATATAN",kpm.getCatatan());r=note(sh,st,r,"BOBOT_CPL",kpm.getCplBobot());r=note(sh,st,r,"KOMPONEN_PENILAIAN",kpm.getKomponenPenilaian());r=note(sh,st,r,"TEKNIK_PER_CPMK",kpm.getTeknikPerCpmk());r=note(sh,st,r,"RUBRIK_PENILAIAN",kpm.getRubrikPenilaian());r=note(sh,st,r,"PEMETAAN_SOAL_UTS",kpm.getPemetaanSoalUts());r=note(sh,st,r,"PEMETAAN_SOAL_UAS",kpm.getPemetaanSoalUas());
		JSONArray cqi=array(perkuliahan==null?null:perkuliahan.getCqiData());for(int i=0;i<cqi.length();i++){JSONObject o=cqi.optJSONObject(i);if(o==null)continue;XSSFRow row=sh.createRow(r++);put(row,0,"CQI",st.readOnly);put(row,1,o.optString("cpmk",""),st.input);put(row,2,"",st.input);put(row,3,o.optString("masalah",""),st.input);put(row,4,o.optString("analisis",""),st.input);put(row,5,o.optString("rencana",""),st.input);put(row,6,o.optString("pj",""),st.input);put(row,7,o.optString("targetWaktu",""),st.input);put(row,8,o.optString("status","Planned"),st.input);put(row,9,o.optString("adminKomentar",""),st.input);}
		finish(sh,r,9,new int[]{18,28,65,45,45,45,28,28,20,45});
	}

	private static void writeMeta(XSSFWorkbook wb,KurikulumPunyaMatakuliah kpm,Perkuliahan p){XSSFSheet sh=wb.createSheet(META);raw(sh,0,"FORMAT_VERSION",FORMAT_VERSION);raw(sh,1,"KPM_ID",kpm.getId());raw(sh,2,"KODE_MATA_KULIAH",kpm.getMatakuliah().getKode());raw(sh,3,"PERKULIAHAN_ID",p==null?null:p.getId());raw(sh,4,"DIBUAT_PADA",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));}

	/* ----------------------------- workbook reader ---------------------------- */

	private static Map<String,String> readKeyValues(XSSFSheet sh,String name){checkHeader(sh,name,"Kode Field");Map<String,String> m=new LinkedHashMap<String,String>();for(int r=FIRST;r<=sh.getLastRowNum();r++){String k=keyUpper(value(sh,r,0));if(!notEmpty(k))continue;if(m.containsKey(k))throw new IllegalArgumentException(name+" baris "+(r+1)+": kode field duplikat '"+k+"'.");m.put(k,value(sh,r,1));}return m;}
	private static List<MasterRow> readMasterRows(XSSFSheet sh,String name,int cols){checkHeader(sh,name,"ID Sistem");List<MasterRow> out=new ArrayList<MasterRow>();Set<String> codes=new HashSet<String>();for(int r=FIRST;r<=sh.getLastRowNum();r++){if(blankRow(sh,r,cols))continue;MasterRow x=new MasterRow();x.location=name+" baris "+(r+1);x.id=parseLongNullable(value(sh,r,0),x.location+" ID");x.code=trim(value(sh,r,1));x.name=trim(value(sh,r,2));x.description=trim(value(sh,r,3));if(PL.equals(name)){x.reference=trim(value(sh,r,4));x.relations=trim(value(sh,r,5));x.active=parseBoolean(value(sh,r,6),x.location+" Aktif");}else{x.category=trim(value(sh,r,4));x.reference=trim(value(sh,r,5));x.relations=trim(value(sh,r,6));x.active=parseBoolean(value(sh,r,7),x.location+" Aktif");}if(!notEmpty(x.code)||!notEmpty(x.name))throw new IllegalArgumentException(x.location+": kode dan nama wajib diisi.");if(!codes.add(key(x.code)))throw new IllegalArgumentException(x.location+": kode duplikat '"+x.code+"'.");out.add(x);}return out;}
	private static List<CpmkRow> readCpmkRows(XSSFSheet sh){checkHeader(sh,CPMK,"ID CPMK");List<CpmkRow> out=new ArrayList<CpmkRow>();for(int r=FIRST;r<=sh.getLastRowNum();r++){if(blankRow(sh,r,10))continue;CpmkRow x=new CpmkRow();x.location=CPMK+" baris "+(r+1);x.id=parseLongNullable(value(sh,r,0),x.location+" ID");x.cpmkCode=trim(value(sh,r,1));x.cpmkName=trim(value(sh,r,2));x.cpmkWeight=parseDoubleNullable(value(sh,r,3),x.location+" Bobot CPMK");x.cpmkMinimum=parseDoubleNullable(value(sh,r,4),x.location+" Minimal CPMK");x.subCode=trim(value(sh,r,5));x.subName=trim(value(sh,r,6));x.subWeight=parseDoubleNullable(value(sh,r,7),x.location+" Bobot Sub-CPMK");x.subMinimum=parseDoubleNullable(value(sh,r,8),x.location+" Minimal Sub-CPMK");x.mapping=trim(value(sh,r,9));if(!notEmpty(x.cpmkCode)||!notEmpty(x.cpmkName))throw new IllegalArgumentException(x.location+": kode dan nama CPMK wajib diisi.");if(notEmpty(x.subCode)!=notEmpty(x.subName))throw new IllegalArgumentException(x.location+": kode dan nama Sub-CPMK harus diisi bersama.");out.add(x);}return out;}
	private static List<DescriptionRow> readDescriptionRows(XSSFSheet sh){checkHeader(sh,DESC,"Tipe Data");List<DescriptionRow> out=new ArrayList<DescriptionRow>();for(int r=FIRST;r<=sh.getLastRowNum();r++){if(blankRow(sh,r,5))continue;DescriptionRow x=new DescriptionRow();x.location=DESC+" baris "+(r+1);x.type=keyUpper(value(sh,r,0));String rawCode=trim(value(sh,r,1));x.code="FIELD".equals(x.type)?keyUpper(rawCode):rawCode;x.name=trim(value(sh,r,2));x.description=trim(value(sh,r,3));x.active=parseBoolean(value(sh,r,4),x.location+" Aktif");if(!("FIELD".equals(x.type)||"BAHAN_KAJIAN".equals(x.type)||"PUSTAKA_UTAMA".equals(x.type)||"PUSTAKA_PENDUKUNG".equals(x.type)))throw new IllegalArgumentException(x.location+": Tipe Data tidak dikenal.");if(!notEmpty(x.code))throw new IllegalArgumentException(x.location+": Kode/Field wajib diisi.");out.add(x);}return out;}
	private static List<AgendaRow> readAgendaRows(XSSFSheet sh){checkHeader(sh,AGENDA,"Kunci");List<AgendaRow> out=new ArrayList<AgendaRow>();for(int r=FIRST;r<=sh.getLastRowNum();r++){if(blankRow(sh,r,17))continue;AgendaRow x=new AgendaRow();x.location=AGENDA+" baris "+(r+1);x.start=parseInteger(value(sh,r,1),x.location+" Mulai Minggu");x.end=parseInteger(value(sh,r,2),x.location+" Sampai Minggu");for(int i=0;i<5;i++)x.subCodes[i]=trim(value(sh,r,3+i));x.indicator=trim(value(sh,r,8));x.criteria=trim(value(sh,r,9));x.method=trim(value(sh,r,10));x.offline=trim(value(sh,r,11));x.online=trim(value(sh,r,12));x.experience=trim(value(sh,r,13));x.bahanKajian=trim(value(sh,r,14));x.pustakaUtama=trim(value(sh,r,15));x.pustakaPendukung=trim(value(sh,r,16));if(x.start<1||x.end<x.start)throw new IllegalArgumentException(x.location+": rentang minggu tidak valid.");out.add(x);}return out;}
	private static List<NoteRow> readNoteRows(XSSFSheet sh){checkHeader(sh,NOTE,"Tipe Data");List<NoteRow> out=new ArrayList<NoteRow>();for(int r=FIRST;r<=sh.getLastRowNum();r++){if(blankRow(sh,r,10))continue;NoteRow x=new NoteRow();x.location=NOTE+" baris "+(r+1);x.type=keyUpper(value(sh,r,0));String rawKey=trim(value(sh,r,1));x.key="FIELD".equals(x.type)?keyUpper(rawKey):rawKey;x.value=value(sh,r,2);x.problem=value(sh,r,3);x.analysis=value(sh,r,4);x.plan=value(sh,r,5);x.owner=value(sh,r,6);x.target=value(sh,r,7);x.status=value(sh,r,8);x.adminComment=value(sh,r,9);if(!("FIELD".equals(x.type)||"CQI".equals(x.type)))throw new IllegalArgumentException(x.location+": Tipe Data harus FIELD atau CQI.");if(!notEmpty(x.key))throw new IllegalArgumentException(x.location+": Kunci/Kode CPMK wajib diisi.");out.add(x);}return out;}

	/* -------------------------------- utilities ------------------------------- */

	private static XSSFSheet keyValueSheet(XSSFWorkbook wb,Styles st,String name,String title){return dataSheet(wb,st,name,title,new String[]{"Kode Field","Nilai","Keterangan"});}
	private static XSSFSheet dataSheet(XSSFWorkbook wb,Styles st,String name,String title,String[] headers){XSSFSheet sh=wb.createSheet(name);sh.setDisplayGridlines(false);sh.createFreezePane(0,FIRST);title(sh,st,title,headers.length);sh.addMergedRegion(new CellRangeAddress(1,1,0,headers.length-1));cell(sh,1,0,"Isi sel berwarna kuning. Jangan mengganti nama sheet dan judul kolom.").setCellStyle(st.note);XSSFRow hr=sh.createRow(HEADER);for(int i=0;i<headers.length;i++)put(hr,i,headers[i],st.header);return sh;}
	private static int kv(XSSFSheet sh,Styles st,int r,String k,Object v,String note,boolean editable){XSSFRow row=sh.createRow(r++);put(row,0,k,st.readOnly);put(row,1,v,editable?st.input:st.readOnly);put(row,2,note,st.body);return r;}
	private static int desc(XSSFSheet sh,Styles st,int r,String type,String code,String name,String description,boolean active){XSSFRow row=sh.createRow(r++);put(row,0,type,st.readOnly);put(row,1,code,st.input);put(row,2,name,st.input);put(row,3,description,st.input);put(row,4,active?"Ya":"Tidak",st.inputCenter);return r;}
	private static int note(XSSFSheet sh,Styles st,int r,String code,String value){XSSFRow row=sh.createRow(r++);put(row,0,"FIELD",st.readOnly);put(row,1,code,st.readOnly);put(row,2,value,st.input);for(int i=3;i<10;i++)put(row,i,"",st.body);return r;}
	private static void finish(XSSFSheet sh,int next,int lastCol,int[] widths){int last=Math.max(FIRST,next-1);sh.setAutoFilter(new CellRangeAddress(HEADER,last,0,lastCol));widths(sh,widths);}
	private static void addYesNo(XSSFSheet sh,int col,int first,int last){XSSFDataValidationHelper h=new XSSFDataValidationHelper(sh);DataValidationConstraint c=h.createExplicitListConstraint(new String[]{"Ya","Tidak"});DataValidation v=h.createValidation(c,new CellRangeAddressList(first,last,col,col));v.setShowErrorBox(true);v.createErrorBox("Nilai tidak valid","Pilih Ya atau Tidak.");sh.addValidationData(v);}
	private static void checkHeader(XSSFSheet sh,String name,String first){if(sh==null||sh.getRow(HEADER)==null||!first.equalsIgnoreCase(value(sh,HEADER,0)))throw new IllegalArgumentException("Header sheet '"+name+"' tidak sesuai format.");}
	private static boolean blankRow(XSSFSheet sh,int r,int cols){for(int i=0;i<cols;i++)if(notEmpty(value(sh,r,i)))return false;return true;}
	private static String value(XSSFSheet sh,int r,int c){if(sh==null||sh.getRow(r)==null)return "";Cell cell=sh.getRow(r).getCell(c);if(cell==null)return "";return new DataFormatter(Locale.US).formatCellValue(cell).trim();}
	private static void title(XSSFSheet sh,Styles st,String value,int cols){sh.addMergedRegion(new CellRangeAddress(0,0,0,cols-1));cell(sh,0,0,value).setCellStyle(st.title);}
	private static Cell cell(XSSFSheet sh,int r,int c,String value){XSSFRow row=sh.getRow(r);if(row==null)row=sh.createRow(r);Cell cell=row.getCell(c);if(cell==null)cell=row.createCell(c);cell.setCellValue(value==null?"":value);return cell;}
	private static void put(Row row,int col,Object value,CellStyle style){Cell cell=row.createCell(col);if(value instanceof Number)cell.setCellValue(((Number)value).doubleValue());else cell.setCellValue(value==null?"":String.valueOf(value));if(style!=null)cell.setCellStyle(style);}
	private static void raw(XSSFSheet sh,int row,String label,Object value){XSSFRow r=sh.createRow(row);put(r,0,label,null);put(r,1,value,null);}
	private static void widths(XSSFSheet sh,int[] widths){for(int i=0;i<widths.length;i++)sh.setColumnWidth(i,Math.min(255,widths[i])*256);}
	private static String date(Date d){return d==null?"":new SimpleDateFormat("dd-MM-yyyy").format(d);}
	private static String number(double d){return d==Math.rint(d)?String.valueOf((long)d):String.valueOf(d);}
	private static String yesNo(Boolean b){return Boolean.FALSE.equals(b)?"Tidak":"Ya";}
	private static Double zero(Double d){return d==null?Double.valueOf(0):d;}
	private static String trim(String s){return s==null?"":s.trim();}
	private static String nullIfEmpty(String s){return notEmpty(s)?s.trim():null;}
	private static boolean notEmpty(String s){return s!=null&&s.trim().length()>0;}
	private static String key(String s){return trim(s).replace('\u00a0',' ').replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
	private static String keyUpper(String s){return trim(s).replace(' ','_').toUpperCase(Locale.ROOT);}
	private static Long parseLong(String s,String where){Long x=parseLongNullable(s,where);if(x==null)throw new IllegalArgumentException(where+" wajib berupa angka.");return x;}
	private static Long parseLongNullable(String s,String where){if(!notEmpty(s))return null;try{return Long.valueOf(s.replaceFirst("\\.0$",""));}catch(Exception e){throw new IllegalArgumentException(where+" harus berupa bilangan bulat.");}}
	private static Integer parseInteger(String s,String where){if(!notEmpty(s))throw new IllegalArgumentException(where+" wajib diisi.");try{return Integer.valueOf(s.replaceFirst("\\.0$",""));}catch(Exception e){throw new IllegalArgumentException(where+" harus berupa bilangan bulat.");}}
	private static Double parseDoubleNullable(String s,String where){if(!notEmpty(s))return null;try{return Double.valueOf(s.replace(',','.'));}catch(Exception e){throw new IllegalArgumentException(where+" harus berupa angka.");}}
	private static Boolean parseBoolean(String s,String where){String k=key(s);if(!notEmpty(k)||"ya".equals(k)||"true".equals(k)||"1".equals(k))return Boolean.TRUE;if("tidak".equals(k)||"false".equals(k)||"0".equals(k))return Boolean.FALSE;throw new IllegalArgumentException(where+" harus Ya atau Tidak.");}
	private static Date parseDate(String s,String where)throws Exception{if(!notEmpty(s))return null;String[] fs={"dd-MM-yyyy","yyyy-MM-dd","dd/MM/yyyy"};for(int i=0;i<fs.length;i++)try{SimpleDateFormat f=new SimpleDateFormat(fs[i]);f.setLenient(false);return f.parse(s);}catch(Exception e){}throw new IllegalArgumentException(where+" harus berformat dd-MM-yyyy.");}
	private static JSONArray array(String raw){try{return notEmpty(raw)?new JSONArray(raw):new JSONArray();}catch(Exception e){return new JSONArray();}}
	private static JSONObject object(String raw){try{return notEmpty(raw)?new JSONObject(raw):new JSONObject();}catch(Exception e){return new JSONObject();}}
	private static String safeFile(String s){String x=notEmpty(s)?s.trim():"MATAKULIAH";return x.replaceAll("[^A-Za-z0-9._-]","_");}
	private static String join(List<String> s){StringBuilder b=new StringBuilder();for(String x:s)if(notEmpty(x)){if(b.length()>0)b.append(',');b.append(x.trim());}return b.toString();}
	private static List<String> splitCodes(String csv){List<String> r=new ArrayList<String>();if(csv!=null)for(String s:csv.split("[,;\\n]"))if(notEmpty(s))r.add(s.trim());return r;}
	private static boolean containsCode(String csv,String code){for(String x:splitCodes(csv))if(key(x).equals(key(code)))return true;return false;}
	private static boolean containsCsv(String csv,String token){if(!notEmpty(csv)||!notEmpty(token))return false;for(String x:csv.split(","))if(trim(x).equals(trim(token)))return true;return false;}
	private static String appendCsv(String csv,String token){if(containsCsv(csv,token))return trim(csv);return !notEmpty(csv)?token:trim(csv)+","+token;}
	private static String removeKpmTokens(String csv,Long kpmId){List<String> out=new ArrayList<String>();String suffix="_"+kpmId;if(csv!=null)for(String x:csv.split(","))if(notEmpty(x)&&!trim(x).endsWith(suffix))out.add(trim(x));return join(out);}
	private static String codeFromLabel(String s){String x=trim(s);int p=x.indexOf(' ');return p>0?x.substring(0,p):x;}

	@SuppressWarnings("unchecked") private static <T> List<T> selected(Session s,Class<T> type,String csv){Set<Long> ids=parseIds(csv);if(ids.isEmpty())return new ArrayList<T>();return ConstantValues.simpleList(s.createCriteria(type).add(Restrictions.in("id",ids)).addOrder(Order.asc("kode")),type);}
	private static Set<Long> parseIds(String csv){Set<Long> ids=new HashSet<Long>();if(csv!=null)for(String x:csv.split(","))try{if(notEmpty(x))ids.add(Long.valueOf(trim(x).split("_")[0]));}catch(Exception e){}return ids;}
	private static Map<Long,String> codes(List<? extends Object> rows){Map<Long,String> m=new HashMap<Long,String>();for(Object x:rows){if(x instanceof ProfilLulusan)m.put(((ProfilLulusan)x).getId(),((ProfilLulusan)x).getKode());else if(x instanceof CapaianPembelajaranLulusan)m.put(((CapaianPembelajaranLulusan)x).getId(),((CapaianPembelajaranLulusan)x).getKode());}return m;}
	private static String codesFromCsv(String csv,Map<Long,String> codes){List<String> out=new ArrayList<String>();for(Long id:parseIds(csv))if(codes.get(id)!=null)out.add(codes.get(id));return join(out);}
	private static String csvIds(Iterable<?> rows){String r="";for(Object x:rows){Long id=null;if(x instanceof ProfilLulusan)id=((ProfilLulusan)x).getId();else if(x instanceof CapaianLulusan)id=((CapaianLulusan)x).getId();else if(x instanceof CapaianPembelajaranLulusan)id=((CapaianPembelajaranLulusan)x).getId();else if(x instanceof BahanKajian)id=((BahanKajian)x).getId();else if(x instanceof ReferensiLulusan)id=((ReferensiLulusan)x).getId();if(id!=null)r=appendCsv(r,String.valueOf(id));}return r;}
	private static Map<String,MasterRow> byCode(List<MasterRow> rows){Map<String,MasterRow> m=new HashMap<String,MasterRow>();for(MasterRow r:rows)m.put(key(r.code),r);return m;}
	private static Map<String,List<CpmkRow>> groupCpmk(List<CpmkRow> rows){Map<String,List<CpmkRow>> m=new LinkedHashMap<String,List<CpmkRow>>();for(CpmkRow r:rows){String k=key(r.cpmkCode);List<CpmkRow> l=m.get(k);if(l==null){l=new ArrayList<CpmkRow>();m.put(k,l);}l.add(r);}return m;}
	private static Map<String,Double> parseWeights(String raw,String where){Map<String,Double> m=new HashMap<String,Double>();for(String p:splitCodes(raw)){String[] a=p.split(":",2);if(a.length!=2)throw new IllegalArgumentException(where+": format bobot pemetaan harus KODE_CPMK:BOBOT.");m.put(key(a[0]),parseDoubleNullable(a[1],where+" bobot pemetaan"));}return m;}
	private static ProfilLulusan findProfile(List<ProfilLulusan> all,Jurusan jur,String code){for(ProfilLulusan x:all)if(sameJur(x.getJurusan(),jur)&&key(x.getKode()).equals(key(code)))return x;return null;}
	private static CapaianLulusan findCpl(List<CapaianLulusan> all,Jurusan jur,String code){for(CapaianLulusan x:all)if(sameJur(x.getJurusan(),jur)&&key(x.getKode()).equals(key(code)))return x;return null;}
	private static CapaianPembelajaranLulusan findCpmk(List<CapaianPembelajaranLulusan> all,Jurusan jur,String code){for(CapaianPembelajaranLulusan x:all)if(sameJur(x.getJurusan(),jur)&&key(x.getKode()).equals(key(code)))return x;return null;}
	private static BahanKajian findBk(List<BahanKajian> all,Jurusan jur,String code,String name){for(BahanKajian x:all)if((sameJur(x.getJurusan(),jur)||x.getJurusan()==null)&&key(x.getKode()).equals(key(code)))return x;for(BahanKajian x:all)if((sameJur(x.getJurusan(),jur)||x.getJurusan()==null)&&key(x.getNama()).equals(key(name)))return x;return null;}
	private static ReferensiLulusan findRef(List<ReferensiLulusan> all,String code,String name){for(ReferensiLulusan x:all)if(notEmpty(code)&&key(x.getKode()).equals(key(code)))return x;for(ReferensiLulusan x:all)if(key(x.getNama()).equals(key(name)))return x;return null;}
	private static boolean sameJur(Jurusan a,Jurusan b){return a==null?b==null:b!=null&&a.getId()!=null&&a.getId().equals(b.getId());}
	@SuppressWarnings("unchecked") private static String findCourseIds(Session s,String codes){String out="";for(String code:splitCodes(codes)){List<Matakuliah> l=s.createCriteria(Matakuliah.class).add(Restrictions.eq("kode",code)).setMaxResults(1).list();if(!l.isEmpty())out=appendCsv(out,String.valueOf(l.get(0).getId()));}return out;}
	@SuppressWarnings("unchecked") private static String findLecturerIds(Session s,String ids){String out="";for(String id:splitCodes(ids)){try{Long x=Long.valueOf(id);if(s.get(Dosen.class,x)!=null)out=appendCsv(out,id);}catch(Exception e){List<Dosen> l=s.createCriteria(Dosen.class).add(Restrictions.eq("kode",id)).setMaxResults(1).list();if(!l.isEmpty())out=appendCsv(out,String.valueOf(l.get(0).getId()));}}return out;}
	private static String lecturerIds(String ids){return trim(ids);}
	private static String courseCodes(Session s,String ids){List<String> out=new ArrayList<String>();for(Long id:parseIds(ids)){Matakuliah m=(Matakuliah)s.get(Matakuliah.class,id);if(m!=null)out.add(m.getKode());}return join(out);}
	private static Map<String,JSONObject> subCpmkLookup(Map<String,CapaianPembelajaranLulusan> cpmks) throws Exception {Map<String,JSONObject> m=new HashMap<String,JSONObject>();for(CapaianPembelajaranLulusan c:cpmks.values()){JSONArray a=array(c.getFormula());for(int i=0;i<a.length();i++){JSONObject s=a.optJSONObject(i);if(s==null)continue;JSONObject x=new JSONObject();x.put("label",s.optString("kode","")+" "+s.optString("nama",""));x.put("value",s.opt("key")+"_"+c.getId());m.put(key(s.optString("kode","")),x);}}return m;}
	@SuppressWarnings("unchecked") private static JSONObject referenceJson(Session s,Class<?> type,String codes) throws Exception {JSONObject out=new JSONObject();for(String code:splitCodes(codes)){List<?> l=s.createCriteria(type).add(Restrictions.eq("kode",code)).setMaxResults(1).list();if(l.isEmpty())continue;Object x=l.get(0);Long id=null;String name="";if(x instanceof BahanKajian){id=((BahanKajian)x).getId();name=((BahanKajian)x).getNama();}else{id=((ReferensiLulusan)x).getId();name=((ReferensiLulusan)x).getNama();}JSONObject o=new JSONObject();o.put("id",id);o.put("nama",name);out.put(String.valueOf(id),o);}return out;}
	private static List<JSONObject> agendaItems(JSONObject root) throws Exception {List<JSONObject> out=new ArrayList<JSONObject>();Iterator<String> it=root.keys();while(it.hasNext()){String k=it.next();JSONObject o=root.optJSONObject(k);if(o!=null){if(!o.has("keyData"))o.put("keyData",k);out.add(o);}}java.util.Collections.sort(out,new java.util.Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){return a.optInt("mulaiMingguKe",0)-b.optInt("mulaiMingguKe",0);}});return out;}
	private static String jsonCodes(Session session,Class<?> type,JSONObject o){List<String> out=new ArrayList<String>();if(o!=null){Iterator<String> it=o.keys();while(it.hasNext()){String id=it.next();String code="";try{Object entity=session.get(type,Long.valueOf(id));if(entity instanceof BahanKajian)code=((BahanKajian)entity).getKode();else if(entity instanceof ReferensiLulusan)code=((ReferensiLulusan)entity).getKode();}catch(Exception ignored){}JSONObject x=o.optJSONObject(id);if(!notEmpty(code)&&x!=null)code=x.optString("kode","");if(!notEmpty(code)&&x!=null)code=codeFromLabel(x.optString("nama",""));if(notEmpty(code))out.add(code);}}return join(out);}

	private static final class ImportData{Map<String,String> mk,authority;List<MasterRow> profiles,cpls;List<CpmkRow> cpmks;List<DescriptionRow> descriptions;List<AgendaRow> agendas;List<NoteRow> notes;void validate(){Set<String> cplCodes=new HashSet<String>();for(MasterRow r:cpls)cplCodes.add(key(r.code));Set<String> cpmkCodes=new HashSet<String>();Set<String> subCodes=new HashSet<String>();for(CpmkRow r:cpmks){cpmkCodes.add(key(r.cpmkCode));if(notEmpty(r.subCode)&&!subCodes.add(key(r.subCode)))throw new IllegalArgumentException(r.location+": Kode Sub-CPMK duplikat '"+r.subCode+"'.");}for(MasterRow r:profiles)for(String c:splitCodes(r.relations))if(!cplCodes.contains(key(c)))throw new IllegalArgumentException(r.location+": CPL terkait '"+c+"' tidak ada di sheet CPL.");for(MasterRow r:cpls)for(String c:splitCodes(r.relations))if(!cpmkCodes.contains(key(c)))throw new IllegalArgumentException(r.location+": CPMK terkait '"+c+"' tidak ada di sheet CPMK_SUB_CPMK.");for(AgendaRow r:agendas)for(String c:r.subCodes)if(notEmpty(c)&&!subCodes.contains(key(c)))throw new IllegalArgumentException(r.location+": Sub-CPMK '"+c+"' tidak ada di sheet CPMK_SUB_CPMK.");}}
	private static final class MasterRow{String location,code,name,description,reference,category,relations;Long id;Boolean active;}
	private static final class CpmkRow{String location,cpmkCode,cpmkName,subCode,subName,mapping;Long id;Double cpmkWeight,cpmkMinimum,subWeight,subMinimum;}
	private static final class DescriptionRow{String location,type,code,name,description;Boolean active;}
	private static final class AgendaRow{String location,indicator,criteria,method,offline,online,experience,bahanKajian,pustakaUtama,pustakaPendukung;Integer start,end;String[] subCodes=new String[5];}
	private static final class NoteRow{String location,type,key,value,problem,analysis,plan,owner,target,status,adminComment;}
	private static final class Counter{int inserted,updated;}

	public static final class ImportResult{private final int pl,cpl,cpmk,agenda,inserted,updated;ImportResult(int pl,int cpl,int cpmk,int agenda,int inserted,int updated){this.pl=pl;this.cpl=cpl;this.cpmk=cpmk;this.agenda=agenda;this.inserted=inserted;this.updated=updated;}public String message(){return "Upload RPS OBE berhasil. Profil lulusan: "+pl+", CPL: "+cpl+", baris CPMK/Sub-CPMK: "+cpmk+", rincian agenda: "+agenda+". Data master baru: "+inserted+", diperbarui: "+updated+".";}}

	private static final class Styles{final XSSFCellStyle title,header,body,input,inputCenter,note,readOnly;Styles(XSSFWorkbook wb){XSSFFont white=wb.createFont();white.setBold(true);white.setColor(IndexedColors.WHITE.getIndex());XSSFFont dark=wb.createFont();dark.setBold(true);dark.setColor(IndexedColors.DARK_BLUE.getIndex());XSSFFont normal=wb.createFont();title=style(wb,white,IndexedColors.DARK_BLUE);title.setAlignment(HorizontalAlignment.CENTER);header=style(wb,white,IndexedColors.TEAL);header.setAlignment(HorizontalAlignment.CENTER);body=style(wb,normal,IndexedColors.WHITE);input=style(wb,normal,IndexedColors.LIGHT_YELLOW);inputCenter=style(wb,normal,IndexedColors.LIGHT_YELLOW);inputCenter.setAlignment(HorizontalAlignment.CENTER);note=style(wb,dark,IndexedColors.LIGHT_CORNFLOWER_BLUE);readOnly=style(wb,normal,IndexedColors.GREY_25_PERCENT);}private static XSSFCellStyle style(XSSFWorkbook wb,Font font,IndexedColors fill){XSSFCellStyle st=wb.createCellStyle();st.setFont(font);st.setFillForegroundColor(fill.getIndex());st.setFillPattern(FillPatternType.SOLID_FOREGROUND);st.setBorderBottom(BorderStyle.THIN);st.setBorderTop(BorderStyle.THIN);st.setBorderLeft(BorderStyle.THIN);st.setBorderRight(BorderStyle.THIN);st.setVerticalAlignment(VerticalAlignment.TOP);st.setWrapText(true);return st;}}
}
