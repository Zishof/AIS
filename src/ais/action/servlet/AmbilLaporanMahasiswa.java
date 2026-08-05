package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.action.report.Report;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormatTemplateSurat;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenjangProgramStudi;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PembatasanNilaiIPKUntukPengambilanKRS;
import ais.database.model.Perkuliahan;
import ais.database.model.Staff;
import ais.database.model.Tbmuser;
import ais.database.model.TemplateSuratParameter;
import ais.database.model.file.FotoMahasiswaLulus;
import ais.database.model.file.SuratJrxmlFile;
import net.sf.jasperreports.engine.JasperCompileManager;

/**
 * Servlet implementation class AmbilLaporanMahasiswa
 */
public class AmbilLaporanMahasiswa extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilLaporanMahasiswa() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {

		Enumeration<String> enumeration = request.getParameterNames();
		while (enumeration.hasMoreElements()) {
			String param = enumeration.nextElement();
			System.out.print("  " + param + " = " + request.getParameter(param));
		}

		String nim = request.getParameter("nim");
		String laporan = request.getParameter("laporan");
		laporan = laporan == null ? "" : laporan.trim();
		String type = request.getParameter("type");

		Integer semester = 1;
		try {
			semester = Integer.parseInt(request.getParameter("semester").trim());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		Mahasiswa mahasiswa = null;
		try {
			mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("nim", nim == null ? null : nim.trim())).setMaxResults(1).uniqueResult();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:122");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:123");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:124");}
			}
		}
		if (mahasiswa == null) {
			ServletContext sc = getServletContext();
			sc.log("NIM Mahasiswa tidak ditemukan");
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		File file = new File(getServletContext().getRealPath("/img/laporan_tidak_ditemukan.png"));
		if (laporan.equals("khs")) {
			file = laporanKHS(mahasiswa, semester, request);
		} else if (laporan.equals("krs")) {
			file = laporanKRS(mahasiswa, request);
		} else if (laporan.equals("transkrip")) {
			file = laporanTranskrip(mahasiswa, request);
		} else if (laporan.equals("ipk")) {
			file = laporanIPK(mahasiswa, request);
		} else if (laporan.equals("struk")) {
			file = laporanStruk(mahasiswa, request);
		} else if (laporan.equals("surat")) {

			Long idSurat = 1L;
			String locale = request.getParameter("locale");
			try {
				idSurat = Long.parseLong(request.getParameter("idSurat").trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			file = laporanSurat(mahasiswa, request, idSurat, locale, type);
		}

		if (file == null) {
			ServletContext sc = getServletContext();
			sc.log("Laporan Mahasiswa tidak ditemukan");
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		if (type.equals("img")) {
			resp.setContentType("image/jpeg");
		} else {
			resp.setContentType("application/pdf");
		}

		ServletOutputStream out = resp.getOutputStream();
		FileInputStream in = new FileInputStream(file);
		int length = (int) file.length();

		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];

		while ((length = in.read(buffer)) != -1) {
			out.write(buffer, 0, length);
		}

		in.close();
		out.flush();

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static File laporanSurat(Mahasiswa mahasiswa, HttpServletRequest request, Long idSurat, String locale,
			String type) throws Exception {
		File file = new File(Common.REAL_PATH + ("/img/laporan_tidak_ditemukan.png"));

		Session session = HibernateUtil.getSessionFactory().openSession();

		try {
			List<TemplateSuratParameter> templateSuratParameters = session.createCriteria(TemplateSuratParameter.class)
					.add(Restrictions.eq("templateSurat.id", idSurat)).list();

			Map parameters = ais.common.HashMapGenerator.getRand();

			Staff staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "dekan"))
					.setMaxResults(1).uniqueResult();

			Staff staffRektor = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "rektor"))
					.setMaxResults(1).uniqueResult();

			Common.initDefaultJudisium();
			Judisium judisium = Common.hitungJudisium(mahasiswa, null);
			parameters.put("judisium", judisium == null ? "" : judisium.getNama());
			parameters.put("judisium_en", judisium == null ? "" : judisium.getNamaen());

			parameters.put("tanggal", ais.ui.util.WaktuUtil.getDate());
			parameters.put("rektor", staffRektor == null ? "" : staffRektor.getNama());
			parameters.put("dekan", staffDekan == null ? "" : staffDekan.getNama());
			parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? 1L : mahasiswa.getId());

			String sem = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
			Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), sem,
					mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
			System.out.println("Cetak Semester IPK, semester < " + semester + "(" + mahasiswa.getNim() + ")");
			parameters.put("semester", semester);

			String myfile = CommonMedia.loadPathFileFotoLangsung(new Tbmuser(mahasiswa));
			parameters.put("foto", myfile);

			parameters.put("foto_lulus", myfile);

			Session streamingSession = StreamingHibernateUtil.getInstance().openSession();
			try {
				FotoMahasiswaLulus fotoMahasiswaLulus = (FotoMahasiswaLulus) streamingSession
						.createCriteria(FotoMahasiswaLulus.class).add(Restrictions.eq("mahasiswa", mahasiswa.getId()))
						.setMaxResults(1).uniqueResult();
				parameters.put("foto_lulus", fotoMahasiswaLulus == null ? myfile : fotoMahasiswaLulus.createLinkUri());
			} catch (Exception e) {
				parameters.put("foto_lulus", myfile);
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (streamingSession != null) {
					try { streamingSession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:237");}
					try { streamingSession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:238");}
					try { streamingSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:239");}
				}
			}

			String subReport = Common.ambilREAL_PATH_REPORT() + "/";
			System.out.println("subReport = " + subReport);
			parameters.put("SUBREPORT_DIR", subReport);

			parameters.put("cr_code",
					BarcodeCommon
							.generateCrCodeMahasiswa(mahasiswa,
									request.getRequestURL().toString() + "?" + request.getQueryString())
							.getAbsolutePath());
			for (TemplateSuratParameter parameter : templateSuratParameters) {
				if (parameter.getTipe().equals(Integer.class.getName())) {
					parameters.put(parameter.getNama(),
							Integer.parseInt(request.getParameter(parameter.getNama().trim())));
				} else if (parameter.getTipe().equals(Long.class.getName())) {
					parameters.put(parameter.getNama(),
							Long.parseLong(request.getParameter(parameter.getNama().trim())));
				} else {
					parameters.put(parameter.getNama(), request.getParameter(parameter.getNama().trim()));
				}
			}

			// System.out.println("parameters = " + parameters);

			FormatTemplateSurat formatTemplateSurat = (FormatTemplateSurat) session
					.createCriteria(FormatTemplateSurat.class).add(Restrictions.eq("templateSurat.id", idSurat))
					.add(Restrictions.eq("bahasa", locale == null || locale.equals("id") ? Common.locale.getLanguage()
							: Common.localeEn.getLanguage()))
					.setMaxResults(1).uniqueResult();

			System.out.println("formatTemplateSurat = " + formatTemplateSurat);

			if (formatTemplateSurat != null) {
				Session mySession = StreamingHibernateUtil.getInstance().openSession();
				try {
				SuratJrxmlFile jrxmlFile = (SuratJrxmlFile) mySession.createCriteria(SuratJrxmlFile.class)
						.add(Restrictions.eq("formatTemplateSurat", formatTemplateSurat.getId()))
						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

				System.out.println("jrxmlFile = " + jrxmlFile);

				if (jrxmlFile != null) {
					System.out.println("jrxmlFile nama = " + jrxmlFile.getNama());
					if (jrxmlFile.getNama().trim().toLowerCase().endsWith("jrxml")) {
						File jrxml = new File(Common.ambilREAL_PATH_REPORT() + "/surat/" + jrxmlFile.getNama());
						jrxml.getParentFile().mkdirs();
						jrxml.delete();
						CommonMedia.getFileFotoDenganFile(jrxmlFile, jrxml);
						// JasperPrint jasperPrint = (JasperPrint) JRLoader
						// .loadObjectFromLocation(file.getAbsolutePath());

						File jasperFile = new File(jrxml.getAbsolutePath().trim().replaceAll(".jrxml", ".jasper"));
						jasperFile.delete();
						jasperFile.createNewFile();

						JasperCompileManager.compileReportToFile(jrxml.getAbsolutePath(), jasperFile.getAbsolutePath());

						if (type.equals("img")) {
							file = Report.generateFileImageReport(Report.PDF, parameters,
									"surat/" + (jasperFile.getName().replaceAll(".jasper", "")),
									ais.ui.util.WaktuUtil.getDate(),
									locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
						} else {
							file = Report.generateFileReport(Report.PDF, parameters,
									"surat/" + (jasperFile.getName().replaceAll(".jasper", "")),
									ais.ui.util.WaktuUtil.getDate(),
									locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
						}

					} else if (jrxmlFile.getNama().trim().toLowerCase().endsWith("jasper")) {
						File jrxml = new File(Common.ambilREAL_PATH_REPORT() + ("/surat/" + jrxmlFile.getNama()));
						jrxml.getParentFile().mkdirs();
						jrxml.delete();
						CommonMedia.getFileFotoDenganFile(jrxmlFile, jrxml);

						if (type.equals("img")) {
							file = Report.generateFileImageReport(Report.PDF, parameters,
									"surat/" + (jrxml.getName().replaceAll(".jasper", "")),
									ais.ui.util.WaktuUtil.getDate(),
									locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
						} else {
							file = Report.generateFileReport(Report.PDF, parameters,
									"surat/" + (jrxml.getName().replaceAll(".jasper", "")),
									ais.ui.util.WaktuUtil.getDate(),
									locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
						}

					}

				}
			
				} finally {
					// FIX bocor: mySession dedikasi (openSession, TIDAK di MAP) tak ditutup finally method. Tutup di sini.
					if (mySession != null) {
						try { mySession.clear(); } catch (Exception eMs) { ais.common.ErrorAuditUtil.record(eMs, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:336");}
						try { mySession.disconnect(); } catch (Exception eMs) { ais.common.ErrorAuditUtil.record(eMs, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:337");}
						try { mySession.close(); } catch (Exception eMs) { ais.common.ErrorAuditUtil.record(eMs, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:338");}
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:346");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:347");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:348");}
			}
		}

		return file;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private File laporanStruk(Mahasiswa mahasiswa, HttpServletRequest request) throws Exception {
		String type = request.getParameter("type");
		String locale = request.getParameter("locale");
		Integer semester = 1;
		try {
			semester = Integer.parseInt(request.getParameter("semester").trim());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		String jenisPembayaran = request.getParameter("jenisPembayaran");

		JenisKegiatan jenisKegiatan = pembayaranUtil.generateJenisKegiatan(jenisPembayaran);

		Session session = HibernateUtil.getSessionFactory().openSession();

		Kegiatan kegiatan = null;
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("cr_code",
				BarcodeCommon.generateCrCodeMahasiswa(mahasiswa, request.getRequestURL().toString()).getAbsolutePath());
		File file = new File(getServletContext().getRealPath("/img/laporan_tidak_ditemukan.png"));

		try {
			if (jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA.getId())
					|| jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_WISUDA.getId())) {

				kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan);

				parameters.put("tanggal", kegiatan.getTanggal());
				parameters.put("kegiatan", kegiatan.getId());

				if (type.equals("img")) {
					file = Report.generateFileImageReport(Report.PDF, parameters, "Bukti_Pembayaran_Mahasiswa",
							ais.ui.util.WaktuUtil.getDate(),
							locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
				} else {
					file = Report.generateFileReport(Report.PDF, parameters, "Bukti_Pembayaran_Mahasiswa",
							ais.ui.util.WaktuUtil.getDate(),
							locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
				}

			} else {
				BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) session
						.createCriteria(BiodataCalonMahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("nim", mahasiswa.getNim())).setMaxResults(1).uniqueResult();

				if (calonMahasiswa != null) {
					kegiatan = calonMahasiswa.ambilKegiatans(semester, jenisKegiatan);
					parameters.put("tanggal", kegiatan.getTanggal());
					parameters.put("kegiatan", kegiatan.getId());

					if (type.equals("img")) {
						file = Report.generateFileImageReport(Report.PDF, parameters,
								"Bukti_Pembayaran_Calon_Mahasiswa", ais.ui.util.WaktuUtil.getDate(),

								locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
					} else {
						file = Report.generateFileReport(Report.PDF, parameters, "Bukti_Pembayaran_Calon_Mahasiswa",
								ais.ui.util.WaktuUtil.getDate(),
								locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
					}

				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:424");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:425");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:426");}
			}
		}

		return file;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private File laporanIPK(Mahasiswa mahasiswa, HttpServletRequest request) throws Exception {
		String type = request.getParameter("type");
		String locale = request.getParameter("locale");
		Session session = HibernateUtil.getSessionFactory().openSession();
		Staff staffDekan = null;
		Staff staffRektor = null;
		try {
			staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "dekan"))
					.setMaxResults(1).uniqueResult();

			staffRektor = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "rektor"))
					.setMaxResults(1).uniqueResult();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:448");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:449");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:450");}
			}
		}

		Date date = ais.ui.util.WaktuUtil.getDate();

		final Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("cr_code",
				BarcodeCommon.generateCrCodeMahasiswa(mahasiswa, request.getRequestURL().toString()).getAbsolutePath());
		parameters.put("tanggal", date);
		parameters.put("rektor", staffRektor == null ? "" : staffRektor.getNama());
		parameters.put("dekan", staffDekan == null ? "" : staffDekan.getNama());
		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? 1L : mahasiswa.getId());
		String sem = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), sem,
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
		System.out.println("Cetak Semester IPK, semester < " + semester + "(" + mahasiswa.getNim() + ")");
		parameters.put("semester", semester);

		if (type.equals("img")) {
			File file = Report.generateFileImageReport(Report.PDF, parameters, "Rekaman_Nilai",
					ais.ui.util.WaktuUtil.getDate(),
					locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
			return file;
		} else {
			File file = Report.generateFileReport(Report.PDF, parameters, "Rekaman_Nilai",
					ais.ui.util.WaktuUtil.getDate(),
					locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
			return file;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private File laporanTranskrip(Mahasiswa mahasiswa, HttpServletRequest request) throws Exception {

		Common.initDefaultJudisium();

		String type = request.getParameter("type");
		String locale = request.getParameter("locale");
		Session session = HibernateUtil.getSessionFactory().openSession();
		Staff staffDekan = null;
		Staff staffRektor = null;
		try {
			staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "dekan"))
					.setMaxResults(1).uniqueResult();

			staffRektor = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "rektor"))
					.setMaxResults(1).uniqueResult();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:501");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:502");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:503");}
			}
		}

		Date date = ais.ui.util.WaktuUtil.getDate();

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("cr_code",
				BarcodeCommon.generateCrCodeMahasiswa(mahasiswa, request.getRequestURL().toString()).getAbsolutePath());
		parameters.put("tanggal", date);
		parameters.put("rektor", staffRektor == null ? "" : staffRektor.getNama());
		parameters.put("dekan", staffDekan == null ? "" : staffDekan.getNama());
		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? 1L : mahasiswa.getId());

		Judisium judisium = Common.hitungJudisium(mahasiswa, null);
		parameters.put("judisium", judisium == null ? "" : judisium.getNama());
		parameters.put("judisium_en", judisium == null ? "" : judisium.getNamaen());

		String sem = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), sem,
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
		System.out.println("Cetak Semester IPK, semester < " + semester + "(" + mahasiswa.getNim() + ")");
		parameters.put("semester", semester);

		String subReport = getServletContext().getRealPath("/report/") + "/";
		System.out.println("subReport = " + subReport);
		parameters.put("SUBREPORT_DIR", subReport);

		String myfile = CommonMedia.loadPathFileFotoLangsung(new Tbmuser(mahasiswa));
		parameters.put("foto", myfile);

		parameters.put("foto_lulus", myfile);

		Session streamingSession = StreamingHibernateUtil.getInstance().openSession();
		try {
			FotoMahasiswaLulus fotoMahasiswaLulus = (FotoMahasiswaLulus) streamingSession
					.createCriteria(FotoMahasiswaLulus.class).add(Restrictions.eq("mahasiswa", mahasiswa.getId()))
					.setMaxResults(1).uniqueResult();
			parameters.put("foto_lulus", fotoMahasiswaLulus == null ? myfile : fotoMahasiswaLulus.createLinkUri());
		} catch (Exception e) {
			parameters.put("foto_lulus", myfile);
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (streamingSession != null) {
				try { streamingSession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:547");}
				try { streamingSession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:548");}
				try { streamingSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:549");}
			}
		}

		if (type.equals("img")) {
			File file = Report.generateFileImageReport(Report.PDF, parameters, "Transkrip_Akademik",
					ais.ui.util.WaktuUtil.getDate(),
					locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
			return file;
		} else {
			File file = Report.generateFileReport(Report.PDF, parameters, "Transkrip_Akademik",
					ais.ui.util.WaktuUtil.getDate(),
					locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
			return file;
		}
	}

	private File laporanKRS(Mahasiswa mahasiswa, HttpServletRequest request) throws Exception {

		String type = request.getParameter("type");
		String locale = request.getParameter("locale");
		Integer semester = 1;
		try {
			semester = Integer.parseInt(request.getParameter("semester").trim());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		Session session = HibernateUtil.getSessionFactory().openSession();

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);

		JenjangProgramStudi jenjangProgramStudi = null;
		try {
			jenjangProgramStudi = (JenjangProgramStudi) session
					.createCriteria(JenjangProgramStudi.class).add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
					.setMaxResults(1).uniqueResult();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:588");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:589");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:590");}
			}
		}

		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		parameters.put("cr_code",
				BarcodeCommon.generateCrCodeMahasiswa(mahasiswa, request.getRequestURL().toString()).getAbsolutePath());
		parameters.put("semester", semester);
		parameters.put("mahasiswa", mahasiswa.getId());
		parameters.put("tanggal", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));

		if (jenjangProgramStudi != null && jenjangProgramStudi.getNmKaPS() != null
				&& !jenjangProgramStudi.getNmKaPS().trim().equals("")) {
			parameters.put("kaprodi", jenjangProgramStudi == null ? "(                                          )"
					: jenjangProgramStudi.getNmKaPS());
			parameters.put("nip", jenjangProgramStudi == null ? "" : jenjangProgramStudi.getNidnKaPS());
		} else {
			Jurusan jurusan = mahasiswa.getJurusan();
			Dosen dosen = jurusan.getKaprodi();
			parameters.put("kaprodi", dosen == null ? "(                                          )" : dosen.getNama());
			parameters.put("nip", dosen == null ? "" : dosen.getCode());
		}
		parameters.put("nuptkosenpa", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNuptk());
		parameters.put("semester_pendek", null);
		parameters.put("namamahasiswa", mahasiswa.getNama());
		parameters.put("namafakultas", mahasiswa.getJurusan().getFakultas().getNama());
		parameters.put("dosenpa", krsMahasiswa == null ? ""
				: krsMahasiswa.getDosenPa() == null ? "......................." : krsMahasiswa.getDosenPa().getNama());
		parameters.put("nipdosenpa",
				krsMahasiswa.getDosenPa() == null ? "......................."
						: (krsMahasiswa.getDosenPa().getCode().isEmpty() ? krsMahasiswa.getDosenPa().getNidn()
								: krsMahasiswa.getDosenPa().getCode()));
		// Report.generatePDFReport("pdf", parameters,
		// "Cetak_KRS_Mahasiswa", ais.ui.util.WaktuUtil.getDate(),
		// .getCurrent().getWebApp());

		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, mahasiswa.getTahunangkatan(),
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

		parameters.put("bar", "1-" + tahunAkademik + "-" + semester + "-" + mahasiswa.getId());

		if (type.equals("img")) {
			File file = Report.generateFileImageReport(Report.PDF, parameters, "Cetak_KRS_Mahasiswa",
					ais.ui.util.WaktuUtil.getDate(),
					locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
			return file;
		} else {
			File file = Report.generateFileReport(Report.PDF, parameters, "Cetak_KRS_Mahasiswa",
					ais.ui.util.WaktuUtil.getDate(),
					locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
			return file;
		}

	}

	private File laporanKHS(Mahasiswa mahasiswa, Integer semester, HttpServletRequest request) throws Exception {

		String type = request.getParameter("type");
		String locale = request.getParameter("locale");

		Session session = HibernateUtil.getSessionFactory().openSession();
		Staff staffDekan = null;
		JenjangProgramStudi jenjangProgramStudi = null;
		try {
			staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "prodi"))
					.setMaxResults(1).uniqueResult();

			jenjangProgramStudi = (JenjangProgramStudi) session
					.createCriteria(JenjangProgramStudi.class).add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
					.setMaxResults(1).uniqueResult();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:664");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:665");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanMahasiswa.java:666");}
			}
		}

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);

		Fakultas fakultas = mahasiswa.getJurusan().getFakultas();
		Jurusan jurusan = mahasiswa.getJurusan();
		PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = Common.getIpkUntukPengambilanKRS(
				mahasiswa, semester, mahasiswa.getTahunangkatan(), fakultas, jurusan, mahasiswa.getProgram(),
				krsMahasiswa.getSemesterPendek());

		PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRSBerikutNya = Common
				.getIpkUntukPengambilanKRS(mahasiswa, semester + 1, mahasiswa.getTahunangkatan(), fakultas, jurusan,
						mahasiswa.getProgram(), krsMahasiswa.getSemesterPendek());

		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		parameters.put("max_sks", pembatasanNilaiIPKUntukPengambilanKRS == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil());

		parameters.put("max_sks_next", pembatasanNilaiIPKUntukPengambilanKRSBerikutNya == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRSBerikutNya.getBatasMaksimumIPKYangBolehDiambil());

		parameters.put("max_sks_berikut", pembatasanNilaiIPKUntukPengambilanKRSBerikutNya == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRSBerikutNya.getBatasMaksimumIPKYangBolehDiambil());

		parameters.put("cr_code",
				BarcodeCommon.generateCrCodeMahasiswa(mahasiswa, request.getRequestURL().toString()).getAbsolutePath());
		parameters.put("semester", semester);
		parameters.put("tahun_ajaran", "");
		parameters.put("pembantu_dekan", staffDekan == null ? "" : staffDekan.getNama());
		parameters.put("mahasiswa", mahasiswa.getId());
		// parameters.put("nip", staffDekan.getNip());
		parameters.put("tanggal", ais.ui.util.WaktuUtil.getDate());

		if (jenjangProgramStudi != null && jenjangProgramStudi.getNmKaPS() != null
				&& !jenjangProgramStudi.getNmKaPS().trim().equals("")) {
			parameters.put("kaprodi", jenjangProgramStudi == null ? "(                                          )"
					: jenjangProgramStudi.getNmKaPS());
			parameters.put("nip", jenjangProgramStudi == null ? "" : jenjangProgramStudi.getNidnKaPS());
		} else {
			Dosen dosen = jurusan.getKaprodi();
			parameters.put("kaprodi", dosen == null ? "(                                          )" : dosen.getNama());
			parameters.put("nip", dosen == null ? "" : dosen.getCode());
		}

		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, mahasiswa.getTahunangkatan(),
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

		parameters.put("bar", "2-" + tahunAkademik + "-" + semester + "-" + mahasiswa.getId());
		parameters.put("nuptkosenpa", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNuptk());
		parameters.put("dosenpa", krsMahasiswa == null ? ""
				: krsMahasiswa.getDosenPa() == null ? "......................." : krsMahasiswa.getDosenPa().getNama());
		parameters.put("nipdosenpa",
				krsMahasiswa.getDosenPa() == null ? "......................."
						: (krsMahasiswa.getDosenPa().getCode().isEmpty() ? krsMahasiswa.getDosenPa().getNidn()
								: krsMahasiswa.getDosenPa().getCode()));
		Double ipmhs = krsMahasiswa.getIps();
		Double ipkmhs = krsMahasiswa.getIpk();

		Integer sksmhss = krsMahasiswa.getSksYangDiambil();
		Integer sksmhs = krsMahasiswa.getSksk();

		if (semester > 1) {
			Double iplast = Common.ipTerakhir(mahasiswa, semester);
			parameters.put("ip_sebelumnya", iplast);
		}
		parameters.put("ipk", ipkmhs);
		parameters.put("ips", ipmhs);
		parameters.put("sksk", sksmhs);
		parameters.put("sks", sksmhss);

		krsMahasiswa = semester <= 1 ? null : Common.singkronkanKrsMahasiswa(mahasiswa, semester - 1, null, null);

		ipmhs = krsMahasiswa == null ? 0.0 : krsMahasiswa.getIps();
		ipkmhs = krsMahasiswa == null ? 0.0 : krsMahasiswa.getIpk();

		sksmhss = krsMahasiswa == null ? 0 : krsMahasiswa.getSksYangDiambil();
		sksmhs = krsMahasiswa == null ? 0 : krsMahasiswa.getSksk();

		parameters.put("ipk_1", ipkmhs);
		parameters.put("ips_1", ipmhs);
		parameters.put("sksk_1", sksmhs);
		parameters.put("sks_1", sksmhss);

		parameters.put("ip_kumulatif_1", ipkmhs);

		parameters.put("ip_semester_1", ipmhs);

		if (Common.bolehKonfigurasi("tampilkan_total_nilai_di_krs", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("total_nilai", mahasiswa.hitungNilaiSampaiSemester(semester, null, null, true));
			if (semester > 1) {
				parameters.put("total_nilai_1", mahasiswa.hitungNilaiSampaiSemester(semester - 1, null, null, true));
			}
		}

		if (Common.bolehKonfigurasi("tampilkan_total_mutu_di_krs", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("total_nilai_mutu", mahasiswa.hitungMutuSampaiSemester(semester, null, null, true));
			parameters.put("total_nilai_ip", mahasiswa.hitungNilaiIpSampaiSemester(semester, null, null, true));
		}

		if (type.equals("img")) {
			File file = Report.generateFileImageReport(Report.PDF, parameters, "Kartu_Hasil_Studi",
					ais.ui.util.WaktuUtil.getDate(),
					locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
			return file;
		} else {
			File file = Report.generateFileReport(Report.PDF, parameters, "Kartu_Hasil_Studi",
					ais.ui.util.WaktuUtil.getDate(),
					locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
			return file;
		}
	}

}
