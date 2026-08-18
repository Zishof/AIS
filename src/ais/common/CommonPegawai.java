package ais.common;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Label;
import org.zkoss.zul.Timer;

import ais.action.master.recruitment.helper.DefaultNisGenerator;
import ais.action.master.recruitment.helper.DefaultNoRegGeneratorPegawai;
import ais.action.master.recruitment.helper.DefaultNoUjianGeneratorPegawai;
import ais.action.master.recruitment.helper.NisGenerator;
import ais.action.master.recruitment.helper.NoRegGeneratorPegawai;
import ais.action.master.recruitment.helper.NoUjianGeneratorPegawai;
import ais.action.report.Report;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.file.FotoCalonPegawai;
import ais.database.model.file.FotoPegawai;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.recruitment.RuangGelombangPendaftaranPegawaiPegawai;
import ais.database.model.recruitment.RuangPegawai;
import ais.ui.util.MyMessageboxConfig;

public class CommonPegawai {
	public static RuangGelombangPendaftaranPegawaiPegawai dapatkanRuangUjian(CalonPegawai calonPegawai) {
		Session session = HibernateUtil.currentSession();

		Long idmin = (Long) session.createCriteria(RuangPegawai.class).createAlias("ujianPegawai", "ujianPegawai")
				.add(Restrictions.eq("gelombangPendaftaranPegawai", calonPegawai.getGelombangPendaftaranPegawai()))
				.add(Restrictions.eq("penuh", 0))
				.add(Restrictions.eq("ujianPegawai.gelombangPendaftaranPegawai",
						calonPegawai.getGelombangPendaftaranPegawai()))
				.setProjection(Projections.min("id")).uniqueResult();

		if (idmin == null) {
			return null;
		}

		RuangPegawai ruangSelected = (RuangPegawai) session.createCriteria(RuangPegawai.class)
				.add(Restrictions.idEq(idmin)).uniqueResult();

		Number t = ((Number) (session.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
				.createAlias("calonPegawai", "calonPegawai").add(Restrictions.ne("calonPegawai.noUjian", ""))
				.add(Restrictions.isNotNull("calonPegawai.noUjian")).add(Restrictions.eq("ruangPegawai", ruangSelected))
				.setProjection(Projections.rowCount()).uniqueResult()));

		Integer isiRuang = t == null ? 0 : t.intValue();

		RuangGelombangPendaftaranPegawaiPegawai ruangGelombangPendaftaranPegawaiPegawai = (RuangGelombangPendaftaranPegawaiPegawai) session
				.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
				.createAlias("calonPegawai", "calonPegawai").add(Restrictions.ne("calonPegawai.noUjian", ""))
				.add(Restrictions.isNotNull("calonPegawai.noUjian")).add(Restrictions.eq("calonPegawai", calonPegawai))
				.setMaxResults(1).uniqueResult();
		if (ruangGelombangPendaftaranPegawaiPegawai == null) {
			ruangGelombangPendaftaranPegawaiPegawai = new RuangGelombangPendaftaranPegawaiPegawai();
		}
		ruangGelombangPendaftaranPegawaiPegawai.setCalonPegawai(calonPegawai);
		ruangGelombangPendaftaranPegawaiPegawai.setRuangPegawai(ruangSelected);
		Common.refreshSaveOrUpdate(session, ruangGelombangPendaftaranPegawaiPegawai);

		if (ruangSelected.getKapasitasRuangan() < (isiRuang + 2)) {
			ruangSelected.setPenuh(1);
			Common.refreshUpdate(session, ruangSelected);
		}

		return ruangGelombangPendaftaranPegawaiPegawai;
	}

	@SuppressWarnings({})
	public static String generateNoUjian(CalonPegawai calonPegawai) throws Exception {

		try {
			NoUjianGeneratorPegawai noUjianGenerator = (NoUjianGeneratorPegawai) Class
					.forName(Common.getKonfigurasi("class_untuk_generate_no_ujian_pegawai",
							DefaultNoUjianGeneratorPegawai.class.getName()).getNilai())
					.newInstance();
			return noUjianGenerator.generateNoUjian(calonPegawai);
		} catch (Exception e) {
			MyMessageboxConfig.show("Nomor Ujian tidak bisa di generate. Harap segera menghubungi Administrator",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			Common.tampilErrorJikaAdmin(e);
		}

		return "";

	}

	public static String generateNoRegistrasi(CalonPegawai calonPegawai) throws Exception {
		try {

			NoRegGeneratorPegawai noRegGenerator = (NoRegGeneratorPegawai) Class.forName(Common
					.getKonfigurasi("class_untuk_generate_no_reg_pegawai", DefaultNoRegGeneratorPegawai.class.getName())
					.getNilai()).newInstance();
			return noRegGenerator.generateNoReg(calonPegawai);
		} catch (Exception e) {
			MyMessageboxConfig.show("Nomor Registrasi tidak bisa di generate. Harap segera menghubungi Administrator",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			Common.tampilErrorJikaAdmin(e);
		}
		return "";
	}

	public static String onGenerateNis(final CalonPegawai calonPegawai, NisGenerator nimGenerator) throws Exception {

		if (!calonPegawai.getTelahDiterima()) {
			MyMessageboxConfig.show("Calon pegawai ini belum dinyatakan lulus / belum diterima!", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return "";

		} else {
			String nim = "";
			if (calonPegawai.getPegawai() != null) {
				MyMessageboxConfig.show(
						"Pegawai telah mendapatkan Kode Pegawai, yaitu : " + calonPegawai.getPegawai().getCode(),
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Map<String, Serializable> parameters = ais.common.HashMapGenerator
										.getRandStringSerializable();
								parameters.put("nim", calonPegawai.getPegawai().getCode());
								parameters.put("biodata_id", calonPegawai.getId());

								calonPegawai.putPhoto(parameters);

								Report.generatePDFReport(Report.PDF, parameters, "recruitment/Biodata_Nis",
										ais.ui.util.WaktuUtil.getDate());
							}
						});
				return nim;
			} else {
				nim = nimGenerator.generateNis(calonPegawai);
			}
			calonPegawai.setNomorInduk(nim);
			Session session = HibernateUtil.currentSession();
			final Pegawai pegawai = CommonPegawai.savePegawai(session, calonPegawai, nim, false);

			MyMessageboxConfig.show("Nomor Induk Pegawai berhasil di-generate: \"" + nim + "\"", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							CommonPegawai.copyLampiran(calonPegawai, pegawai);

							Map<String, Serializable> parameters = ais.common.HashMapGenerator
									.getRandStringSerializable();
							parameters.put("nim", pegawai.getCode());
							parameters.put("biodata_id", calonPegawai.getId());
							calonPegawai.putPhoto(parameters);

							Report.generatePDFReport(Report.PDF, parameters, "recruitment/Biodata_Nis",
									ais.ui.util.WaktuUtil.getDate());
						}
					});

			return nim;

		}
	}

	public static void uploadKelulusan(final File file, final EventListener eventListener) throws Exception {

		final Label peringatan = new Label("");

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		final Label downloadPath = new Label("");
		// FIX compile "cannot find symbol: report": harus dideklarasikan sebelum
		// timer.addEventListener(...) karena dipakai di dalam closure onTimer di bawah.
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Kelulusan Pegawai");
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					if (!downloadPath.getValue().isEmpty()) {
						try { org.zkoss.zul.Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception eD) { ais.common.ErrorAuditUtil.record(eD, "auto-audit(empty-catch) CommonPegawai download-laporan"); }
					}
					MyMessageboxConfig.show(
							"Upload kelulusan berhasil dilakukan."
									+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue())
									+ "\n\n" + report.getRingkasan(),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {
					NisGenerator nisGenerator = (NisGenerator) Class.forName(
							Common.getKonfigurasi("class_untuk_generate_nis", DefaultNisGenerator.class.getName())
									.getNilai().trim())
							.newInstance();

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);
					Session session = HibernateUtil.currentNativeSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							Long code = -1L;
							try {
								String content = Common.getCellContent(Common.getCell(sheet, 0, i));
								System.out.println("content = " + content);
								code = Long.parseLong(content);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPegawai.java:231");

							}
							if (code.equals(-1L)) {
								try {

									String content = Common.getCellContent(Common.getCell(sheet, 1, i));
									System.out.println("noRegistrasi = " + content);
									code = (Long) session.createCriteria(CalonPegawai.class)
											.add(Restrictions.eq("noRegistrasi", content))
											.setProjection(Projections.property("id")).setMaxResults(1).uniqueResult();
								} catch (Exception e) {
									continue;
								}

							}

							System.out.println("code = " + code);

							if (code == null || code.equals(-1L)) {
								continue;
							}

							String nim = "";
							try {

								nim = Common.getCellContent(Common.getCell(sheet, 3, i));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPegawai.java:258");

							}

							CalonPegawai calonPegawai = (CalonPegawai) session.createCriteria(CalonPegawai.class)
									.add(Restrictions.idEq(code)).uniqueResult();
							if (calonPegawai == null) {
								continue;
							}

							label.setValue("Upload data \"" + calonPegawai.getNama() + "\" ("
									+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

							if (nim.trim().isEmpty()
									&& (calonPegawai.getPegawai() == null || calonPegawai.getPegawai().getNim() == null
											|| calonPegawai.getPegawai().getNim().trim().isEmpty())) {
								nim = nisGenerator.generateNis(calonPegawai);
							} else if (nim.trim().isEmpty() && calonPegawai.getPegawai() != null
									&& calonPegawai.getPegawai().getNim() != null
									&& calonPegawai.getPegawai().getNim().trim().isEmpty()) {
								nim = calonPegawai.getPegawai().getNim();
							}

							int count = ((Number) session.createCriteria(Pegawai.class).add(Restrictions.eq("nim", nim))
									.add(Restrictions.ne("calonPegawai", calonPegawai.getId()))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();
							if (count > 0) {
								System.out.println("nim = " + nim + " sudah ada");
								String my = "Calon pegawai \"" + calonPegawai + "\" nim = " + nim
										+ " sudah ada di database.\n";
								peringatan.setValue(peringatan.getValue() + my);

								continue;
							}

							System.out.println("calonPegawai = " + calonPegawai + ", nim = " + nim);

							Pegawai pegawai = null;
							session.getTransaction().begin();
							if (!nim.trim().isEmpty()) {
								pegawai = CommonPegawai.savePegawai(session, calonPegawai, nim.trim(), false);
							} else {
								Common.refreshUpdate(session, calonPegawai);
							}
							session.getTransaction().commit();

							report.sukses(i, nim + " – " + calonPegawai.getNama(), "");
							if (pegawai != null) {
								CommonPegawai.copyLampiran(calonPegawai, pegawai);
							}

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPegawai.java:308");
							report.gagal(i, String.valueOf(i), e, "Periksa data baris " + i);
						}

					}

					HibernateUtil.closeSession();

				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/common/CommonPegawai.java:318");
				}

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) CommonPegawai laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	public static void copyLampiran(CalonPegawai calonPegawai, Pegawai pegawai) {

		try {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

			FotoCalonPegawai fotocalonPegawai = (FotoCalonPegawai) streamingSession
					.createCriteria(FotoCalonPegawai.class).add(Restrictions.eq("calonPegawai", calonPegawai.getId()))
					.setMaxResults(1).uniqueResult();

			if (fotocalonPegawai != null && fotocalonPegawai.getFoto() != null) {

				FotoPegawai fotoPegawai = (FotoPegawai) streamingSession.createCriteria(FotoPegawai.class)
						.addOrder(Order.desc("id")).add(Restrictions.eq("pegawai", pegawai.getId())).setMaxResults(1)
						.uniqueResult();

				if (fotoPegawai != null) {
					streamingSession.getTransaction().begin();
					streamingSession.delete(fotoPegawai);
					streamingSession.getTransaction().commit();
				}

				fotoPegawai = new FotoPegawai();
				fotoPegawai.setNama(fotocalonPegawai.getNama());
				fotoPegawai.setKeterangan(fotocalonPegawai.getKeterangan());
				fotoPegawai.setPegawai(pegawai.getId());
				fotoPegawai.setFoto(fotocalonPegawai.getFoto());

				streamingSession.getTransaction().begin();
				streamingSession.save(fotoPegawai);
				streamingSession.getTransaction().commit();

			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPegawai.java:362");
			// TODO: handle exception
		}
	}

	public static Pegawai savePegawai(Session session, CalonPegawai calonPegawai, String nim, boolean commitMaual) {

		Pegawai pegawai = calonPegawai.getPegawai();
		if (pegawai == null && calonPegawai != null) {
			pegawai = (Pegawai) session.createCriteria(Pegawai.class)
					.add(Restrictions.ilike("nama", calonPegawai.getNama(), MatchMode.EXACT))
					.add(Restrictions.eq("tanggalLahir", calonPegawai.getTanggalLahir())).setMaxResults(1)
					.uniqueResult();
		}
		if (pegawai == null) {
			pegawai = new Pegawai();
		} else {
			nim = pegawai.getCode();
		}
		pegawai.setCalonPegawai(calonPegawai.getId());

		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(CalonPegawai.class);
		ClassMetadata classMetadataPegawai = HibernateUtil.getClassMetadata(Pegawai.class);
		for (String p : classMetadata.getPropertyNames()) {
			try {
				Object val = classMetadata.getPropertyValue(calonPegawai, p, EntityMode.POJO);
				classMetadataPegawai.setPropertyValue(pegawai, p, val, EntityMode.POJO);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPegawai.java:389");

			}
		}

		pegawai.setCode(nim);

		if (commitMaual) {
			session.getTransaction().begin();
		}
		Common.refreshSaveOrUpdate(session, pegawai);
		calonPegawai.setPegawai(pegawai);
		if (commitMaual) {
			session.getTransaction().commit();
		}

		return pegawai;
	}
}
