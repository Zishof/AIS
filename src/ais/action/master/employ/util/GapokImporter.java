package ais.action.master.employ.util;

import java.io.File;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.Golongan;
import ais.database.model.employ.Peraturan;

public class GapokImporter {

	public static Golongan checkGolongan(String nama) {
		Session session = HibernateUtil.currentNativeSession();
		Golongan golongan = (Golongan) session.createCriteria(Golongan.class)
				.add(Restrictions.ilike("nama", nama.trim(), MatchMode.ANYWHERE)).setMaxResults(1).uniqueResult();
		if (golongan == null) {
			golongan = new Golongan();
			golongan.setKeterangan(nama);
			golongan.setNama(nama);
			session.getTransaction().begin();
			session.save(golongan);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return golongan;
	}

	public static void doImport(File file) throws Exception {

		XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
		XSSFSheet sheet = workbook.getSheetAt(0);

		List<List<String>> objects = Common.getSheetContent(sheet);
		

		int index = 0;
		Peraturan peraturan = null;
		String namaPeraturan = "";
		String tentangPeraturan = "";
		String tanggalPeraturan = "";

		Double gol1A = null;
		Double gol1B = null;
		Double gol1C = null;
		Double gol1D = null;

		Double gol2A = null;
		Double gol2B = null;
		Double gol2C = null;
		Double gol2D = null;

		Double gol3A = null;
		Double gol3B = null;
		Double gol3C = null;
		Double gol3D = null;

		Double gol4A = null;
		Double gol4B = null;
		Double gol4C = null;
		Double gol4D = null;
		Double gol4E = null;

		Golongan golongan1A = checkGolongan("I/a");
		Golongan golongan1B = checkGolongan("I/b");
		Golongan golongan1C = checkGolongan("I/c");
		Golongan golongan1D = checkGolongan("I/d");

		Golongan golongan2A = checkGolongan("II/a");
		Golongan golongan2B = checkGolongan("II/b");
		Golongan golongan2C = checkGolongan("II/c");
		Golongan golongan2D = checkGolongan("II/d");

		Golongan golongan3A = checkGolongan("III/a");
		Golongan golongan3B = checkGolongan("III/b");
		Golongan golongan3C = checkGolongan("III/c");
		Golongan golongan3D = checkGolongan("III/d");

		Golongan golongan4A = checkGolongan("IV/a");
		Golongan golongan4B = checkGolongan("IV/b");
		Golongan golongan4C = checkGolongan("IV/c");
		Golongan golongan4D = checkGolongan("IV/d");
		Golongan golongan4E = checkGolongan("IV/e");

		for (List<String> strings : objects) {
			if (index == 0) {
				namaPeraturan = strings.get(1) == null ? "" : strings.get(1).trim().replaceAll("  ", " ");
				System.out.println("namaPeraturan = " + namaPeraturan);
			} else if (index == 1) {
				tentangPeraturan = strings.get(1) == null ? "" : strings.get(1).trim().replaceAll("  ", " ");
				System.out.println("tentangPeraturan = " + tentangPeraturan);
			} else if (index == 2) {
				tanggalPeraturan = strings.get(1) == null ? "" : strings.get(1).trim().replaceAll("  ", " ");
				System.out.println("tanggalPeraturan = " + tanggalPeraturan);
			} else {

				if (namaPeraturan == null || namaPeraturan.trim().equals("")) {
					throw new Exception("Peraturan harus di-isi");
				}

				if (peraturan == null) {
					Session session = HibernateUtil.currentNativeSession();
					peraturan = (Peraturan) session.createCriteria(Peraturan.class)
							.add(Restrictions.ilike("nama", namaPeraturan)).setMaxResults(1).uniqueResult();

					if (peraturan == null) {
						peraturan = new Peraturan();
					}

					peraturan.setIsi(tentangPeraturan);
					peraturan.setKeterangan(tentangPeraturan);
					peraturan.setKode(namaPeraturan);
					peraturan.setNama(namaPeraturan);
					try {
						peraturan.setTanggalBerlaku(Common.databaseDateFormat.get().parse(tanggalPeraturan));
					} catch (Exception e) {

						HibernateUtil.closeSession();
						throw e;
					}

					session.getTransaction().begin();
					if (peraturan.getId() == null) {
						session.save(peraturan);
					} else {
						Common.refreshUpdate(session, (peraturan));
					}
					session.getTransaction().commit();

					HibernateUtil.closeSession();
				}

				Session session = HibernateUtil.currentNativeSession();
				/*
				 * Golongan I
				 */
				Integer masaKerjaGolonganI = null;
				try {
					masaKerjaGolonganI = Integer.parseInt(strings.get(0).trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:148");
				}
				if (masaKerjaGolonganI != null) {

					try {
						gol1A = Double.parseDouble(strings.get(1).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:154");
					}

					try {
						gol1B = Double.parseDouble(strings.get(2).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:159");
					}

					try {
						gol1C = Double.parseDouble(strings.get(3).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:164");
					}

					try {
						gol1D = Double.parseDouble(strings.get(4).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:169");
					}

					System.out.print("masaKerjaGolonganI = " + masaKerjaGolonganI + ", gol1A " + gol1A + ", gol1B "
							+ gol1B + ", gol1C " + gol1C + ", gol1D " + gol1D + "       ");

					GajiPokok gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganI))
							.add(Restrictions.eq("golongan", golongan1A)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol1A);
					gajiPokok.setGolongan(golongan1A);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganI);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

					gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganI))
							.add(Restrictions.eq("golongan", golongan1B)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol1B);
					gajiPokok.setGolongan(golongan1B);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganI);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

					gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganI))
							.add(Restrictions.eq("golongan", golongan1C)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol1C);
					gajiPokok.setGolongan(golongan1C);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganI);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

					gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganI))
							.add(Restrictions.eq("golongan", golongan1D)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol1D);
					gajiPokok.setGolongan(golongan1D);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganI);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();
				}

				/*
				 * Golongan II
				 */
				Integer masaKerjaGolonganII = null;
				try {
					masaKerjaGolonganII = Integer.parseInt(strings.get(5).trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:262");
				}
				if (masaKerjaGolonganII != null) {

					try {
						gol2A = Double.parseDouble(strings.get(6).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:268");
					}

					try {
						gol2B = Double.parseDouble(strings.get(7).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:273");
					}

					try {
						gol2C = Double.parseDouble(strings.get(8).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:278");
					}

					try {
						gol2D = Double.parseDouble(strings.get(9).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:283");
					}

					System.out.print("masaKerjaGolonganII = " + masaKerjaGolonganII + ", gol2A " + gol2A + ", gol2B "
							+ gol2B + ", gol2C " + gol2C + ", gol2D " + gol2D + "       ");

					GajiPokok gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganII))
							.add(Restrictions.eq("golongan", golongan2A)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol2A);
					gajiPokok.setGolongan(golongan2A);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganII);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

					gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganII))
							.add(Restrictions.eq("golongan", golongan2B)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol2B);
					gajiPokok.setGolongan(golongan2B);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganII);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

					gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganII))
							.add(Restrictions.eq("golongan", golongan2C)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol2C);
					gajiPokok.setGolongan(golongan2C);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganII);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

					gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganII))
							.add(Restrictions.eq("golongan", golongan2D)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol2D);
					gajiPokok.setGolongan(golongan2D);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganII);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

				}

				/*
				 * Golongan III
				 */
				Integer masaKerjaGolonganIII = null;
				try {
					masaKerjaGolonganIII = Integer.parseInt(strings.get(10).trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:377");
				}
				if (masaKerjaGolonganIII != null) {

					try {
						gol3A = Double.parseDouble(strings.get(11).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:383");
					}

					try {
						gol3B = Double.parseDouble(strings.get(12).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:388");
					}

					try {
						gol3C = Double.parseDouble(strings.get(13).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:393");
					}

					try {
						gol3D = Double.parseDouble(strings.get(14).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:398");
					}

					System.out.print("masaKerjaGolonganIII = " + masaKerjaGolonganIII + ", gol3A " + gol3A + ", gol3B "
							+ gol3B + ", gol3C " + gol3C + ", gol3D " + gol3D + "       ");

					GajiPokok gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganIII))
							.add(Restrictions.eq("golongan", golongan3A)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol3A);
					gajiPokok.setGolongan(golongan3A);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganIII);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

					gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganIII))
							.add(Restrictions.eq("golongan", golongan3B)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol3B);
					gajiPokok.setGolongan(golongan3B);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganIII);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

					gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganIII))
							.add(Restrictions.eq("golongan", golongan3C)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol3C);
					gajiPokok.setGolongan(golongan3C);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganIII);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

					gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganIII))
							.add(Restrictions.eq("golongan", golongan3D)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol3D);
					gajiPokok.setGolongan(golongan3D);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganIII);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

				}

				/*
				 * Golongan III
				 */
				Integer masaKerjaGolonganIV = null;
				try {
					masaKerjaGolonganIV = Integer.parseInt(strings.get(15).trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:492");
				}
				if (masaKerjaGolonganIV != null) {

					try {
						gol4A = Double.parseDouble(strings.get(16).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:498");
					}

					try {
						gol4B = Double.parseDouble(strings.get(17).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:503");
					}

					try {
						gol4C = Double.parseDouble(strings.get(18).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:508");
					}

					try {
						gol4D = Double.parseDouble(strings.get(19).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:513");
					}

					try {
						gol4E = Double.parseDouble(strings.get(20).replaceAll(",", "").replaceAll("\\.", "").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/util/GapokImporter.java:518");
					}

					System.out.print("masaKerjaGolonganIV = " + masaKerjaGolonganIV + ", gol4A " + gol4A + ", gol4B "
							+ gol4B + ", gol4C " + gol4C + ", gol4D " + gol4D + ", gol4E " + gol4E + "       ");

					GajiPokok gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganIV))
							.add(Restrictions.eq("golongan", golongan4A)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol4A);
					gajiPokok.setGolongan(golongan4A);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganIV);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

					gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganIV))
							.add(Restrictions.eq("golongan", golongan4B)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol4B);
					gajiPokok.setGolongan(golongan4B);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganIV);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

					gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganIV))
							.add(Restrictions.eq("golongan", golongan4C)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol4C);
					gajiPokok.setGolongan(golongan4C);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganIV);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

					gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganIV))
							.add(Restrictions.eq("golongan", golongan4D)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol4D);
					gajiPokok.setGolongan(golongan4D);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganIV);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();

					gajiPokok = (GajiPokok) session.createCriteria(GajiPokok.class)
							.add(Restrictions.eq("peraturan", peraturan))
							.add(Restrictions.eq("masaKerja", masaKerjaGolonganIV))
							.add(Restrictions.eq("golongan", golongan4E)).setMaxResults(1).uniqueResult();
					if (gajiPokok == null) {
						gajiPokok = new GajiPokok();
					}
					gajiPokok.setGaji(gol4E);
					gajiPokok.setGolongan(golongan4E);
					gajiPokok.setKeterangan("");
					gajiPokok.setMasaKerja(masaKerjaGolonganIV);
					gajiPokok.setPeraturan(peraturan);
					session.getTransaction().begin();
					if (gajiPokok.getId() == null) {
						session.save(gajiPokok);
					} else {
						Common.refreshUpdate(session, (gajiPokok));
					}
					session.getTransaction().commit();
				}

				HibernateUtil.closeSession();
			}

			index++;
		}

		// if (peraturan != null) {
		// Session session = HibernateUtil.currentNativeSession();
		// List<Pegawai> pegawais =
		// session.createCriteria(Pegawai.class).add(Restrictions.or(Restrictions.eq("aktif",
		// true), Restrictions.isNull("aktif")))
		// .list();
		//
		// for (Pegawai pegawai : pegawais) {
		// KenaikanPangkat kenaikanPangkat = (KenaikanPangkat) session
		// .createCriteria(KenaikanPangkat.class)
		// .createAlias("gajiPokok", "gajiPokok")
		// .add(Restrictions.eq("gajiPokok.peraturan", peraturan))
		// .add(Restrictions.eq("pegawai", pegawai))
		// .addOrder(Order.desc("mulai"))
		// .addOrder(Order.desc("id")).setMaxResults(1)
		// .uniqueResult();
		// if (kenaikanPangkat == null && pegawai.getGajiPokok() != null) {
		//
		// GajiPokok gajiPokok = session.createCriteria(GajiPokok)
		//
		// kenaikanPangkat = new KenaikanPangkat();
		// kenaikanPangkat.setGajiPokok(gajiPokok);
		// }
		// kenaikanPangkat = null;
		// }
		//
		// pegawais = null;

		// HibernateUtil.closeSession();
		// }

	}

	public static void main(String[] argv) throws Exception {
		File file = new File(
				"D:/Documents/My Project/Academic Information System/AIS/ais/web/tmp/Contoh_Daftar_Gapok_PNS.xlsx");
		doImport(file);
	}

}
