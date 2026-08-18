package ais.action.master.sirs.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.akunting.helper.AmbilDataAkunKreditBanbox;
import ais.action.master.sirs.helper.AmbilDataJenisBiayaBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.listener.OnSave;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.Biaya;
import ais.database.model.sirs.BiayaAlatMedisPerKelas;
import ais.database.model.sirs.JenisAlatMedis;
import ais.database.model.sirs.JenisBiaya;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.TarifKhusus;
import ais.database.model.sirs.TarifKhususPunyaAlatMedis;
import ais.ui.util.MyDoublebox;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

public class CommonAlatMedis {

	public static void onUploadBiaya(Event event, final TarifKhusus tarifKhusus, final String jenis) throws Exception {

		final Media media;

		if (event instanceof ForwardEvent) {
			ForwardEvent forwardEvent = (ForwardEvent) event;
			media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();
			if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
				return;
		} else {
			media = ((UploadEvent) event).getMedia();
			if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
				return;
		}

		final Label label = new Label(ais.common.Common.getBahasaConfig("memproses data .."));

		try {
			InputStream inputStream = media.getStreamData();
			System.out.println("media = " + media);
			final File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			System.out.println("file = " + file.getAbsolutePath());
			file.getParentFile().mkdirs();

			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();

			if (media.getName().toLowerCase().endsWith("xls")) {

				Runnable runnable = new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {

						try {

							Cell cell = null;
							KelasPerawatan kelasPerawatanUmum = null;
							Map<Long, JenisBiaya> jenisBiayaMap = new HashMap<Long, JenisBiaya>();

							Workbook workbook = Workbook.getWorkbook(file);
							for (Sheet sheet : workbook.getSheets()) {

								String kelas = sheet.getName();
								// FIX bocor: jangan openSession di sini; SELALU di-reassign di kedua cabang sebelum dipakai.
								Session session = null;

								if (kelas.trim().equalsIgnoreCase("1000000-DATA")) {

									for (int i = 1; i <= sheet.getRows(); i++) {

										session = HibernateUtil.getSessionFactory().openSession();
										try {

											try {
												cell = sheet.getCell(0, i);
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:136");
												// session.disconnect();
												if (session.isOpen()) {session.disconnect();session.close();}
												break;
											}
											if (cell == null) {
												// session.disconnect();
												if (session.isOpen()) {session.disconnect();session.close();}
												break;
											}

											String id = sheet.getCell(0, i) == null
													|| sheet.getCell(0, i).getContents() == null ? null
															: sheet.getCell(0, i).getContents().trim();
											String kode = sheet.getCell(1, i) == null
													|| sheet.getCell(1, i).getContents() == null ? null
															: sheet.getCell(1, i).getContents().trim();

											String nama = sheet.getCell(2, i) == null
													|| sheet.getCell(2, i).getContents() == null ? null
															: sheet.getCell(2, i).getContents().trim();

											String aktif = sheet.getCell(3, i) == null
													|| sheet.getCell(3, i).getContents() == null ? null
															: sheet.getCell(3, i).getContents().trim();

											String sama = sheet.getCell(4, i) == null
													|| sheet.getCell(4, i).getContents() == null ? null
															: sheet.getCell(4, i).getContents().trim();

											String jenisAlatMedis = sheet.getCell(5, i) == null
													|| sheet.getCell(5, i).getContents() == null ? null
															: sheet.getCell(5, i).getContents().trim();

											String Per = sheet.getCell(6, i) == null
													|| sheet.getCell(6, i).getContents() == null ? null
															: sheet.getCell(6, i).getContents().trim();

											String Lab = sheet.getCell(7, i) == null
													|| sheet.getCell(7, i).getContents() == null ? null
															: sheet.getCell(7, i).getContents().trim();
											String Operasi = sheet.getCell(8, i) == null
													|| sheet.getCell(8, i).getContents() == null ? null
															: sheet.getCell(8, i).getContents().trim();
											String Radiologi = sheet.getCell(9, i) == null
													|| sheet.getCell(9, i).getContents() == null ? null
															: sheet.getCell(9, i).getContents().trim();
											String Vk = sheet.getCell(10, i) == null
													|| sheet.getCell(10, i).getContents() == null ? null
															: sheet.getCell(10, i).getContents().trim();

											String RenalUnit = sheet.getCell(11, i) == null
													|| sheet.getCell(11, i).getContents() == null ? null
															: sheet.getCell(11, i).getContents().trim();

											String Gizi = sheet.getCell(12, i) == null
													|| sheet.getCell(12, i).getContents() == null ? null
															: sheet.getCell(12, i).getContents().trim();

											AlatMedis alatMedis = null;

											if (id != null && !id.trim().isEmpty()) {
												alatMedis = (AlatMedis) session.createCriteria(AlatMedis.class)
														.setMaxResults(1)
														.add(Restrictions.idEq(Long.parseLong(id.trim())))
														.uniqueResult();
											}

											if (alatMedis == null && kode != null && !kode.trim().isEmpty()) {
												alatMedis = (AlatMedis) session.createCriteria(AlatMedis.class)
														.setMaxResults(1)
														.add(Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT))
														.uniqueResult();
											}

											if (alatMedis == null && nama != null && !nama.trim().isEmpty()) {
												alatMedis = (AlatMedis) session.createCriteria(AlatMedis.class)
														.setMaxResults(1)
														.add(Restrictions.ilike("nama", nama.trim(), MatchMode.EXACT))
														.uniqueResult();
											}

											if (alatMedis == null) {
												alatMedis = new AlatMedis();
												alatMedis.setJenis(jenis);
											}

											alatMedis.setKode(kode);
											alatMedis.setNama(nama);
											alatMedis.setPer(Per);

											JenisAlatMedis myJenisAlatMedis = null;
											try {

												myJenisAlatMedis = (JenisAlatMedis) session
														.createCriteria(JenisAlatMedis.class)
														.add(Restrictions.idEq(Long
																.parseLong(jenisAlatMedis.trim().split("-")[0].trim())))
														.uniqueResult();
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonAlatMedis.java:235");

											}

											if (myJenisAlatMedis == null) {
												try {
													myJenisAlatMedis = (JenisAlatMedis) session
															.createCriteria(JenisAlatMedis.class)
															.add(Restrictions.ilike("nama",
																	jenisAlatMedis.trim().split("-")[1].trim(),
																	MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonAlatMedis.java:247");

												}

											}

											if (myJenisAlatMedis == null) {
												try {
													myJenisAlatMedis = (JenisAlatMedis) session
															.createCriteria(JenisAlatMedis.class)
															.add(Restrictions.ilike("nama", jenisAlatMedis.trim(),
																	MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonAlatMedis.java:260");

												}

											}

											alatMedis.setJenisAlatMedis(myJenisAlatMedis);

											try {
												alatMedis.setAktif(Boolean.parseBoolean(aktif));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:271");
											}

											try {
												alatMedis.setSemuahargasama(Boolean.parseBoolean(sama));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:277");
											}

											try {
												alatMedis.setAlatMedisLab(Boolean.parseBoolean(Lab));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:283");
											}

											try {
												alatMedis.setAlatMedisOperasi(Boolean.parseBoolean(Operasi));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:289");
											}

											try {
												alatMedis.setAlatMedisRadiologi(Boolean.parseBoolean(Radiologi));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:295");
											}

											try {
												alatMedis.setAlatMedisVk(Boolean.parseBoolean(Vk));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:301");
											}

											try {
												alatMedis.setAlatMedisRenalUnit(Boolean.parseBoolean(RenalUnit));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:307");
											}

											try {
												alatMedis.setAlatMedisGizi(Boolean.parseBoolean(Gizi));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:313");
											}

											label.setValue(alatMedis.toString());

											session.getTransaction().begin();
											session.saveOrUpdate(alatMedis);
											session.getTransaction().commit();

											if (tarifKhusus != null) {
												Number jumlah = (Number) session
														.createCriteria(TarifKhususPunyaAlatMedis.class)
														.add(Restrictions.eq("tarifKhusus", tarifKhusus))
														.add(Restrictions.eq("alatMedis", alatMedis))
														.setProjection(Projections.rowCount()).uniqueResult();

												if (jumlah.intValue() == 0) {
													TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis = new TarifKhususPunyaAlatMedis();
													tarifKhususPunyaAlatMedis.setAktif(true);
													tarifKhususPunyaAlatMedis.setTarifKhusus(tarifKhusus);
													tarifKhususPunyaAlatMedis.setAlatMedis(alatMedis);
													session.getTransaction().begin();
													session.save(tarifKhususPunyaAlatMedis);
													session.getTransaction().commit();
												}
											}

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:341");
										}

										// session.disconnect();
										if (session.isOpen()) {session.disconnect();session.close();}
									}

								} else {

									session = HibernateUtil.getSessionFactory().openSession();

									// FIX bocor: tutup session di finally agar tetap ditutup walau query gagal.
									KelasPerawatan kelasPerawatan = null;
									try {
										kelasPerawatan = (KelasPerawatan) session
												.createCriteria(KelasPerawatan.class)
												.add(Restrictions.idEq(Long.parseLong(kelas.split("-")[0].trim())))
												.uniqueResult();
									} finally {
										if (session.isOpen()) {session.disconnect();session.close();}
									}

									if (kelasPerawatan != null) {

										if (kelasPerawatan.getNama().trim().equalsIgnoreCase("umum")) {
											kelasPerawatanUmum = kelasPerawatan;
										}

										for (int i = 1; i <= sheet.getRows(); i++) {

											session = HibernateUtil.getSessionFactory().openSession();
											try {

												String id = sheet.getCell(0, i) == null
														|| sheet.getCell(0, i).getContents() == null ? null
																: sheet.getCell(0, i).getContents().trim();
												String kode = sheet.getCell(1, i) == null
														|| sheet.getCell(1, i).getContents() == null ? null
																: sheet.getCell(1, i).getContents().trim();

												String nama = sheet.getCell(2, i) == null
														|| sheet.getCell(2, i).getContents() == null ? null
																: sheet.getCell(2, i).getContents().trim();

												String tarif = sheet.getCell(3, i) == null
														|| sheet.getCell(3, i).getContents() == null ? null
																: sheet.getCell(3, i).getContents().trim();

												String persen = sheet.getCell(4, i) == null
														|| sheet.getCell(4, i).getContents() == null ? null
																: sheet.getCell(4, i).getContents().trim();

												String berubah = sheet.getCell(5, i) == null
														|| sheet.getCell(5, i).getContents() == null ? null
																: sheet.getCell(5, i).getContents().trim();

												AlatMedis alatMedis = null;

												if (id != null && !id.trim().isEmpty()) {
													alatMedis = (AlatMedis) session.createCriteria(AlatMedis.class)
															.setMaxResults(1)
															.add(Restrictions.idEq(Long.parseLong(id.trim())))
															.uniqueResult();
												}

												if (alatMedis == null && kode != null && !kode.trim().isEmpty()) {
													alatMedis = (AlatMedis) session.createCriteria(AlatMedis.class)
															.setMaxResults(1).add(Restrictions.ilike("kode",
																	kode.trim(), MatchMode.EXACT))
															.uniqueResult();
												}

												if (alatMedis == null && nama != null && !nama.trim().isEmpty()) {
													alatMedis = (AlatMedis) session.createCriteria(AlatMedis.class)
															.setMaxResults(1).add(Restrictions.ilike("nama",
																	nama.trim(), MatchMode.EXACT))
															.uniqueResult();
												}

												if (alatMedis != null) {

													TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis = null;

													if (tarifKhusus != null) {
														tarifKhususPunyaAlatMedis = (TarifKhususPunyaAlatMedis) session
																.createCriteria(TarifKhususPunyaAlatMedis.class)
																.add(Restrictions.eq("tarifKhusus", tarifKhusus))
																.add(Restrictions.eq("alatMedis", alatMedis))
																.setMaxResults(1).uniqueResult();
													}

													BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = CommonTarifAlatMedis
															.getBiayaAlatMedisPerKelas(alatMedis, kelasPerawatan,
																	tarifKhususPunyaAlatMedis, session);

													try {
														biayaAlatMedisPerKelas.setPembagianBiayaDalamPersen(
																Boolean.parseBoolean(persen));
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:440");
													}

													try {
														biayaAlatMedisPerKelas.setHargaBisaDirubahSaatTransaksi(
																Boolean.parseBoolean(berubah));
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:447");
													}

													try {
														biayaAlatMedisPerKelas.setBiaya(Double.parseDouble(tarif));
														session.getTransaction().begin();
														session.update(biayaAlatMedisPerKelas);
														session.getTransaction().commit();
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:456");
													}

													label.setValue(biayaAlatMedisPerKelas.toString());

													for (int index = 6; index < sheet.getColumns(); index++) {

														try {
															String jenis = sheet.getCell(index, 0) == null
																	|| sheet.getCell(index, 0).getContents() == null
																			? null
																			: sheet.getCell(index, 0).getContents()
																					.trim();

															String nilai = sheet.getCell(index, i) == null
																	|| sheet.getCell(index, i).getContents() == null
																			? null
																			: sheet.getCell(index, i).getContents()
																					.trim();

															Double jumlah = 0.0;
															try {
																jumlah = Double.parseDouble(nilai.trim());
															} catch (Exception e) {
																e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:480");
															}

															JenisBiaya jenisBiaya = (JenisBiaya) session
																	.createCriteria(JenisBiaya.class)
																	.add(Restrictions.idEq(
																			Long.parseLong(jenis.split("-")[0].trim())))
																	.uniqueResult();

															if (jenisBiaya != null) {

																jenisBiayaMap.put(jenisBiaya.getId(), jenisBiaya);

																Biaya biaya = ((Biaya) session
																		.createCriteria(Biaya.class)
																		.add(Restrictions.eq("jenisBiaya", jenisBiaya))
																		.add(Restrictions.eq("biayaAlatMedisPerKelas",
																				biayaAlatMedisPerKelas))

																		.add(Restrictions
																				.isNull("detailTransaksiLayanan"))
																		.add(Restrictions.isNull("detailTransaksi"))

																		.setMaxResults(1).uniqueResult());

																if (biaya == null) {
																	biaya = new Biaya();
																}

																biaya.setBiayaAlatMedisPerKelas(biayaAlatMedisPerKelas);
																biaya.setJenisBiaya(jenisBiaya);

																if (biayaAlatMedisPerKelas
																		.getPembagianBiayaDalamPersen()) {
																	biaya.setPersen(jumlah);
																} else {
																	biaya.setJumlah(jumlah);
																}

																session.getTransaction().begin();
																session.saveOrUpdate(biaya);
																session.getTransaction().commit();

															}

														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:526");
														}
													}
												}

											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:532");
												// e.printStackTrace();
											}
										}

										// session.disconnect();
										if (session.isOpen()) {session.disconnect();session.close();}
									}
								}
							}

							if (kelasPerawatanUmum != null) {
								Session session = HibernateUtil.getSessionFactory().openSession();

								// FIX bocor: bungkus body sinkronisasi dengan try/finally agar session ditutup walau gagal.
								try {

								List<AlatMedis> alatMedissSama = session.createCriteria(AlatMedis.class)
										.add(Restrictions.eq("semuahargasama", true)).list();

								List<KelasPerawatan> kelasPerawatans = session.createCriteria(KelasPerawatan.class)
										.add(Restrictions.ne("id", kelasPerawatanUmum.getId()))
										.addOrder(Order.asc("id")).list();

								for (AlatMedis alatMedis : alatMedissSama) {
									label.setValue("Singkronisasi -- " + alatMedis.toString());

									TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis = null;

									if (tarifKhusus != null) {
										tarifKhususPunyaAlatMedis = (TarifKhususPunyaAlatMedis) session
												.createCriteria(TarifKhususPunyaAlatMedis.class)
												.add(Restrictions.eq("tarifKhusus", tarifKhusus))
												.add(Restrictions.eq("alatMedis", alatMedis)).setMaxResults(1)
												.uniqueResult();
									}

									BiayaAlatMedisPerKelas biayaAlatMedisPerKelasUmum = CommonTarifAlatMedis
											.getBiayaAlatMedisPerKelas(alatMedis, kelasPerawatanUmum,
													tarifKhususPunyaAlatMedis, session);

									for (JenisBiaya jenisBiaya : jenisBiayaMap.values()) {

										Biaya biayaUmum = ((Biaya) session.createCriteria(Biaya.class)
												.add(Restrictions.eq("jenisBiaya", jenisBiaya))
												.add(Restrictions.eq("biayaAlatMedisPerKelas",
														biayaAlatMedisPerKelasUmum))

												.add(Restrictions.isNull("detailTransaksiLayanan"))
												.add(Restrictions.isNull("detailTransaksi"))

												.setMaxResults(1).uniqueResult());

										if (biayaUmum != null) {

											for (KelasPerawatan kelasPerawatan : kelasPerawatans) {

												BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = CommonTarifAlatMedis
														.getBiayaAlatMedisPerKelas(alatMedis, kelasPerawatan,
																tarifKhususPunyaAlatMedis, session);

												Biaya biaya = ((Biaya) session.createCriteria(Biaya.class)
														.add(Restrictions.eq("jenisBiaya", jenisBiaya))
														.add(Restrictions.eq("biayaAlatMedisPerKelas",
																biayaAlatMedisPerKelas))

														.add(Restrictions.isNull("detailTransaksiLayanan"))
														.add(Restrictions.isNull("detailTransaksi"))

														.setMaxResults(1).uniqueResult());

												if (biaya == null) {
													biaya = new Biaya();
												}

												biayaAlatMedisPerKelas.setBiaya(biayaAlatMedisPerKelasUmum.getBiaya());
												biayaAlatMedisPerKelas.setPembagianBiayaDalamPersen(
														biayaAlatMedisPerKelasUmum.getPembagianBiayaDalamPersen());
												biayaAlatMedisPerKelas.setHargaBisaDirubahSaatTransaksi(
														biayaAlatMedisPerKelasUmum.getHargaBisaDirubahSaatTransaksi());

												biaya.setBiayaAlatMedisPerKelas(biayaAlatMedisPerKelas);
												biaya.setJenisBiaya(jenisBiaya);

												biaya.setPersen(biayaUmum.getPersen());
												biaya.setJumlah(biayaUmum.getJumlah());
												biaya.setJumlahTotal(biayaUmum.getJumlahTotal());

												session.getTransaction().begin();
												session.update(biayaAlatMedisPerKelas);
												session.saveOrUpdate(biaya);
												session.getTransaction().commit();

											}
										}
									}

								}

								} finally {
									// session.disconnect();
									if (session.isOpen()) {session.disconnect();session.close();}
								}
							}

						} catch (Exception ex) {
							System.out.println(ex);
							ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:639");

						}

						label.setValue("");
					}

				};

				new Thread(runnable).start();

				final Timer timer = new Timer(500);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						System.out.println("process = " + label.getValue());
						Clients.showBusy("Upload data .. " + label.getValue());
						if (label.getValue().isEmpty()) {
							Clients.clearBusy();
							MyMessageboxConfig.showFormat("Alhamdulillah, proses unggah data telah berhasil dilakukan. {V1}", "Error", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION, media);
							timer.detach();
						}

					}
				});
				timer.start();

			}

			else {
				MyMessageboxConfig.showFormat("Mohon maaf, berkas yang Bapak/Ibu unggah harus berformat Excel 2003 (xls). {V1}. Langkah yang dapat dilakukan: (1) simpan berkas dalam format .xls; (2) unggah kembali berkas tersebut.", "Error",
						MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
			}

		} catch (Exception e) {

			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:680");
			// e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static void onDownloadBiaya(Event event, final TarifKhusus tarifKhusus, final String jenis)
			throws Exception {

		Clients.showBusy("memproses data ..");

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/biaya_alat_medis_"
				+ URLEncoder.encode(jenis, "UTF-8") + "_" + (Common.dateFormat6.get().format(new Date())) + ".xls");

		new File(filename).createNewFile();

		final Label label = new Label(ais.common.Common.getBahasaConfig("memproses data .."));

		Runnable runnable = new Runnable() {

			@Override
			public void run() {

				Session session = null;
				try {

					session = HibernateUtil.getSessionFactory().openSession();
					List<KelasPerawatan> kelasPerawatans = session.createCriteria(KelasPerawatan.class)
							.addOrder(Order.asc("id")).list();
					@SuppressWarnings("resource")
					HSSFWorkbook workbook = new HSSFWorkbook();

					List<AlatMedis> alatMediss = null;

					if (tarifKhusus != null) {
						alatMediss = session.createCriteria(TarifKhususPunyaAlatMedis.class)
								.setProjection(Projections.groupProperty("alatMedis"))
								.createAlias("alatMedis", "alatMedis").add(Restrictions.eq("alatMedis.jenis", jenis))
								.list();
					} else {
						alatMediss = session.createCriteria(AlatMedis.class).add(Restrictions.eq("jenis", jenis))
								.addOrder(Order.asc("nama")).list();
					}

					List<JenisBiaya> jenisBiayas = session.createCriteria(Biaya.class)
							.add(Restrictions.isNull("detailTransaksiLayanan"))
							.add(Restrictions.isNull("detailTransaksi"))
							.setProjection(Projections.groupProperty("jenisBiaya"))
							.createAlias("biayaAlatMedisPerKelas", "biayaAlatMedisPerKelas")
							.createAlias("jenisBiaya", "jenisBiaya").add(Restrictions.eq("jenisBiaya.aktif", true))
							.list();

					System.out.println("alatMediss = " + alatMediss.size() + ", jenisBiayas = " + jenisBiayas.size());

					// session.disconnect();
					if (session.isOpen()) {session.disconnect();session.close();}

					if (true) {
						HSSFSheet sheet = workbook.createSheet("1000000-DATA");

						HSSFRow rowhead = sheet.createRow((short) 0);
						rowhead.createCell(0).setCellValue("ID");
						rowhead.createCell(1).setCellValue("Kode");
						rowhead.createCell(2).setCellValue("Nama");
						rowhead.createCell(3).setCellValue("Aktif");
						rowhead.createCell(4).setCellValue("Harga Sama");
						rowhead.createCell(5).setCellValue("Jenis Alat Medis");
						rowhead.createCell(6).setCellValue("Satuan");

						if (jenis.equals(AlatMedis.JENIS_TEMPAT_TIDUR)) {

							rowhead.createCell(7).setCellValue("Bangsal / Ruang");
							rowhead.createCell(8).setCellValue("Kamar");
							rowhead.createCell(9).setCellValue("Bed / Tempat Tidur");

							rowhead.createCell(10).setCellValue("Lab");
							rowhead.createCell(11).setCellValue("Operasi");
							rowhead.createCell(12).setCellValue("Radiologi");
							rowhead.createCell(13).setCellValue("Vk");
							rowhead.createCell(14).setCellValue("Renal Unit");
							rowhead.createCell(15).setCellValue("Gizi");

						} else {
							rowhead.createCell(7).setCellValue("Lab");
							rowhead.createCell(8).setCellValue("Operasi");
							rowhead.createCell(9).setCellValue("Radiologi");
							rowhead.createCell(10).setCellValue("Vk");
							rowhead.createCell(11).setCellValue("Renal Unit");
							rowhead.createCell(12).setCellValue("Gizi");

						}

						int i = 1;
						for (AlatMedis alatMedis : alatMediss) {

							session = HibernateUtil.getSessionFactory().openSession();

							System.out.println("alatMedis.toString() = " + alatMedis.toString());
							label.setValue(alatMedis.toString());

							try {

								HSSFRow row = sheet.createRow(i);
								row.createCell(0).setCellValue(alatMedis.getId());
								row.createCell(1).setCellValue(alatMedis.getKode());
								row.createCell(2).setCellValue(alatMedis.getNama());
								row.createCell(3).setCellValue(alatMedis.getAktif());
								row.createCell(4).setCellValue(alatMedis.getSemuahargasama());

								row.createCell(5)
										.setCellValue(alatMedis.getJenisAlatMedis() == null ? ""
												: (alatMedis.getJenisAlatMedis().getId() + "-"
														+ alatMedis.getJenisAlatMedis().getNama().trim()));

								row.createCell(6).setCellValue(alatMedis.getPer());

								if (jenis.equals(AlatMedis.JENIS_TEMPAT_TIDUR)) {

									row.createCell(7)
											.setCellValue(alatMedis.getRuang() == null ? ""
													: (alatMedis.getRuang().getId() + "-"
															+ alatMedis.getRuang().getNama().trim()));

									row.createCell(8)
											.setCellValue(alatMedis.getKamar() == null ? ""
													: (alatMedis.getKamar().getId() + "-"
															+ alatMedis.getKamar().getNama().trim()));

									row.createCell(9)
											.setCellValue(alatMedis.getTempatTidur() == null ? ""
													: (alatMedis.getTempatTidur().getId() + "-"
															+ alatMedis.getTempatTidur().getNama().trim()));

									row.createCell(10).setCellValue(alatMedis.getAlatMedisLab());
									row.createCell(11).setCellValue(alatMedis.getAlatMedisOperasi());
									row.createCell(12).setCellValue(alatMedis.getAlatMedisRadiologi());
									row.createCell(13).setCellValue(alatMedis.getAlatMedisVk());
									row.createCell(14).setCellValue(alatMedis.getAlatMedisRenalUnit());
									row.createCell(15).setCellValue(alatMedis.getAlatMedisGizi());

								} else {
									row.createCell(7).setCellValue(alatMedis.getAlatMedisLab());
									row.createCell(8).setCellValue(alatMedis.getAlatMedisOperasi());
									row.createCell(9).setCellValue(alatMedis.getAlatMedisRadiologi());
									row.createCell(10).setCellValue(alatMedis.getAlatMedisVk());
									row.createCell(11).setCellValue(alatMedis.getAlatMedisRenalUnit());
									row.createCell(12).setCellValue(alatMedis.getAlatMedisGizi());
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:829");
							}

							// session.disconnect();
							if (session.isOpen()) {session.disconnect();session.close();}

							i++;
						}
					}

					for (KelasPerawatan kelasPerawatan : kelasPerawatans) {

						HSSFSheet sheet = workbook.createSheet(kelasPerawatan.getId() + "-" + kelasPerawatan.getNama());

						HSSFRow rowhead = sheet.createRow((short) 0);
						rowhead.createCell(0).setCellValue("ID");
						rowhead.createCell(1).setCellValue("Kode");
						rowhead.createCell(2).setCellValue("Nama");
						rowhead.createCell(3).setCellValue("Tarif");
						rowhead.createCell(4).setCellValue("Biaya dalam persen");
						rowhead.createCell(5).setCellValue("Biaya bisa dirubah saat transaksi");

						int index = 6;
						for (JenisBiaya jenisBiaya : jenisBiayas) {
							rowhead.createCell(index).setCellValue(jenisBiaya.getId() + "-" + jenisBiaya.getNama());
							index++;
						}

						int i = 1;
						for (AlatMedis alatMedis : alatMediss) {

							session = HibernateUtil.getSessionFactory().openSession();

							BiayaAlatMedisPerKelas biayaAlatMedisPerKelas;
							TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis = null;

							if (tarifKhusus != null) {
								tarifKhususPunyaAlatMedis = (TarifKhususPunyaAlatMedis) session
										.createCriteria(TarifKhususPunyaAlatMedis.class)
										.add(Restrictions.eq("tarifKhusus", tarifKhusus))
										.add(Restrictions.eq("alatMedis", alatMedis)).setMaxResults(1).uniqueResult();
							}

							if (tarifKhususPunyaAlatMedis == null) {
								biayaAlatMedisPerKelas = CommonTarifAlatMedis.getBiayaAlatMedisPerKelas(alatMedis,
										kelasPerawatan, null, session);
							} else {
								biayaAlatMedisPerKelas = CommonTarifAlatMedis.getBiayaAlatMedisPerKelas(alatMedis,
										kelasPerawatan, tarifKhususPunyaAlatMedis, session);
							}

							label.setValue(biayaAlatMedisPerKelas.toString());

							System.out.println(
									"biayaAlatMedisPerKelas.toString() = " + biayaAlatMedisPerKelas.toString());

							try {

								HSSFRow row = sheet.createRow(i);
								row.createCell(0).setCellValue(alatMedis.getId());
								row.createCell(1).setCellValue(alatMedis.getKode());
								row.createCell(2).setCellValue(alatMedis.getNama());
								row.createCell(3).setCellValue(biayaAlatMedisPerKelas.getBiaya());

								row.createCell(4).setCellValue(biayaAlatMedisPerKelas.getPembagianBiayaDalamPersen());
								row.createCell(5)
										.setCellValue(biayaAlatMedisPerKelas.getHargaBisaDirubahSaatTransaksi());

								index = 6;
								for (JenisBiaya jenisBiaya : jenisBiayas) {
									Biaya biaya = ((Biaya) session.createCriteria(Biaya.class)
											.add(Restrictions.eq("jenisBiaya", jenisBiaya))
											.add(Restrictions.eq("biayaAlatMedisPerKelas", biayaAlatMedisPerKelas))

											.add(Restrictions.isNull("detailTransaksiLayanan"))
											.add(Restrictions.isNull("detailTransaksi"))

											.setMaxResults(1).uniqueResult());

									if (biayaAlatMedisPerKelas.getPembagianBiayaDalamPersen()) {
										row.createCell(index).setCellValue(biaya == null ? 0.0 : biaya.getPersen());
									} else {
										row.createCell(index).setCellValue(biaya == null ? 0.0 : biaya.getJumlah());
									}
									index++;
								}

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:917");
							}

							// session.disconnect();
							if (session.isOpen()) {session.disconnect();session.close();}

							i++;
						}
					}

					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
					System.out.println("Your excel file has been generated!");

					label.setValue("Finish");

				} catch (Exception ex) {
					System.out.println(ex);
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:936");
					label.setValue("");
				} finally {
					// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
					// finally menjamin penutupan walau exception (idempoten via isOpen()).
					if (session != null && session.isOpen()) {
						try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonAlatMedis.java:942");}
						try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonAlatMedis.java:943");}
					}
				}
			}
		};

		new Thread(runnable).start();

		final Timer timer = new Timer(500);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				System.out.println("process = " + label.getValue());
				Clients.showBusy("Download data .. " + label.getValue());
				if (label.getValue().trim().equalsIgnoreCase("Finish")) {
					Clients.clearBusy();
					Filedownload.save(new File(filename), "application/vnd.ms-excel");
					timer.detach();
				}

			}
		});
		timer.start();

	}

	public static class InitHarga {

		public Grid gridHargaJual = new Grid();
		public Checkbox semuahargasama = new Checkbox();

		@SuppressWarnings("unchecked")
		public boolean saveDetail(AlatMedis alatMedis, TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis) {
			Session session = HibernateUtil.currentSession();
			List<Row> rows = gridHargaJual.getRows().getChildren();
			for (Row row : rows) {
				KelasPerawatan kelasPerawatan = (KelasPerawatan) row.getAttribute("kelasPerawatan");
				BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = (BiayaAlatMedisPerKelas) row
						.getAttribute("biayaAlatMedisPerKelas");

				if (biayaAlatMedisPerKelas != null) {
					Double nilaiBiaya = ((MyDoublebox) row.getChildren().get(row.getChildren().size() - 1)).getValue();
					nilaiBiaya = nilaiBiaya == null ? 0.0 : nilaiBiaya;

					Checkbox persen = (Checkbox) row.getChildren().get(1);
					Checkbox berubah = (Checkbox) row.getChildren().get(2);

					biayaAlatMedisPerKelas.setHargaBisaDirubahSaatTransaksi(berubah.isChecked());
					biayaAlatMedisPerKelas.setPembagianBiayaDalamPersen(persen.isChecked());

					if (tarifKhususPunyaAlatMedis == null) {
						biayaAlatMedisPerKelas.setAlatMedis(alatMedis);
						biayaAlatMedisPerKelas.setTarifKhususPunyaAlatMedis(null);
					} else {
						biayaAlatMedisPerKelas.setTarifKhususPunyaAlatMedis(tarifKhususPunyaAlatMedis);
						biayaAlatMedisPerKelas.setAlatMedis(null);
					}

					biayaAlatMedisPerKelas.setKelasPerawatan(kelasPerawatan);
					biayaAlatMedisPerKelas.setBiaya(nilaiBiaya);

					session.saveOrUpdate(biayaAlatMedisPerKelas);
					row.setAttribute("biayaAlatMedisPerKelas", biayaAlatMedisPerKelas);

					for (Object oo : row.getChildren()) {

						if (oo instanceof Hbox) {

							MyDoublebox o = (MyDoublebox) ((Hbox) oo).getChildren().get(0);
							MyDoublebox o1 = (MyDoublebox) ((Hbox) oo).getChildren().get(1);

							Biaya biaya = (Biaya) o.getAttribute("biaya");
							biaya.setBiayaAlatMedisPerKelas(biayaAlatMedisPerKelas);
							biaya.setJumlah(o.getValue());
							biaya.setPersen(o1.getValue());

							session.saveOrUpdate(biaya);

						}

					}
				}
			}

			return true;
		}

		public EventListener ubahStatusRubahSemuaEventListener = new EventListener() {

			@SuppressWarnings({ "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {

				List<Row> rows = gridHargaJual.getRows().getChildren();
				if (!rows.isEmpty()) {
					int i = 0;
					Row mainRow = rows.get(0);
					for (Row row : rows) {
						BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = (BiayaAlatMedisPerKelas) row
								.getAttribute("biayaAlatMedisPerKelas");
						if (biayaAlatMedisPerKelas != null) {
							if (i != 0) {
								if (semuahargasama.isChecked()) {

									int index = 0;
									for (Object oo : row.getChildren()) {

										if (oo instanceof Checkbox) {
											Checkbox o1 = ((Checkbox) mainRow.getChildren().get(index));
											((Checkbox) oo).setChecked(o1.isChecked());
											((Checkbox) oo).setDisabled(true);
										}

										if (oo instanceof MyDoublebox) {
											MyDoublebox o1 = ((MyDoublebox) mainRow.getChildren().get(index));
											((MyDoublebox) oo).setValue(o1.getValue());
											((MyDoublebox) oo).setDisabled(true);

											Biaya biaya = (Biaya) ((MyDoublebox) oo).getAttribute("biaya");
											biaya.setJumlahTotal(o1.getValue());
											((MyDoublebox) oo).setAttribute("biaya", biaya);
										}

										if (oo instanceof Hbox) {

											Object o = ((Hbox) oo).getChildren().get(0);
											MyDoublebox o1 = (MyDoublebox) ((Hbox) mainRow.getChildren().get(index))
													.getChildren().get(0);

											if (o instanceof MyDoublebox) {
												((MyDoublebox) o).setValue(o1.getValue());
												((MyDoublebox) o).setDisabled(true);

												Biaya biaya = (Biaya) ((MyDoublebox) o).getAttribute("biaya");
												biaya.setJumlah(o1.getValue());
												((MyDoublebox) o).setAttribute("biaya", biaya);
											}

											o = ((Hbox) oo).getChildren().get(1);
											o1 = (MyDoublebox) ((Hbox) mainRow.getChildren().get(index)).getChildren()
													.get(1);

											if (o instanceof MyDoublebox) {
												((MyDoublebox) o).setValue(o1.getValue());
												((MyDoublebox) o).setDisabled(true);

												Biaya biaya = (Biaya) ((MyDoublebox) o).getAttribute("biaya");
												biaya.setPersen(o1.getValue());
												((MyDoublebox) o).setAttribute("biaya", biaya);
											}
										}
										index++;
									}

								} else {
									for (Object oo : row.getChildren()) {
										if (oo instanceof Hbox) {
											Object o = ((Hbox) oo).getChildren().get(0);
											((MyDoublebox) o).setDisabled(false);
											o = ((Hbox) oo).getChildren().get(1);
											((MyDoublebox) o).setDisabled(false);
										}

										if (oo instanceof MyDoublebox) {
											((MyDoublebox) oo).setDisabled(false);
										}

										if (oo instanceof Checkbox) {
											((Checkbox) oo).setDisabled(false);
										}
									}
								}
							}
							i++;

							Checkbox pembagianBiayaDalamPersen = (Checkbox) row.getChildren().get(1);

							if (!pembagianBiayaDalamPersen.isChecked()) {
								try {
									Double totalNilai = 0.0;
									for (Object oo : row.getChildren()) {
										if (oo instanceof Hbox) {
											MyDoublebox o = (MyDoublebox) ((Hbox) oo).getChildren().get(0);
											totalNilai += o.getValue() == null ? 0.0 : o.getValue();
										}
									}

									System.out.println("totalNilai => " + totalNilai);

									biayaAlatMedisPerKelas.setBiaya(totalNilai);

									MyDoublebox total = (MyDoublebox) row.getChildren()
											.get(row.getChildren().size() - 1);

									total.setValue(biayaAlatMedisPerKelas.getBiaya());
									row.setAttribute("biayaAlatMedisPerKelas", biayaAlatMedisPerKelas);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonAlatMedis.java:1143");
								}
							}
						}
					}
				}
			}
		};

		@SuppressWarnings("unchecked")
		public void initHargaJual(final AlatMedis alatMedis, final Tabpanel tabpanel, final OnSave onSave,
				final TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis) throws Exception {

			final Boolean delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

			final Center c = new Center();

			Common.clear(tabpanel);

			Borderlayout bl = new Borderlayout();
			bl.setParent(tabpanel);

			North n = new North();
			n.setParent(bl);

			c.setParent(bl);
			ais.ui.util.ZkCompat.setFlex(c, true);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("25px");
			toolbar.setParent(n);
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Tambah akun lain-nya", "/img/add_item.png");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					if (!onSave.onSave(event)) {
						return;
					}

					List<JenisBiaya> jenisBiayas = CommonTarifAlatMedis.getJenisBiayas(alatMedis,
							tarifKhususPunyaAlatMedis);

					AmbilDataJenisBiayaBanyak ambilDataJenisBiayaBanyak = new AmbilDataJenisBiayaBanyak(jenisBiayas,
							JenisBiaya.TIPE_ALAT_MEDIS);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
							.appendChild(ambilDataJenisBiayaBanyak);
					ambilDataJenisBiayaBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<JenisBiaya> jenisBiayas = (List<JenisBiaya>) arg0.getData();

							Session session = HibernateUtil.currentSession();
							List<Row> rows = gridHargaJual.getRows().getChildren();
							if (!rows.isEmpty()) {
								Row mainRow = rows.get(0);
								BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = (BiayaAlatMedisPerKelas) mainRow
										.getAttribute("biayaAlatMedisPerKelas");
								for (JenisBiaya jenisBiaya : jenisBiayas) {
									Integer count = ((Number) session.createCriteria(Biaya.class)
											.add(Restrictions.isNull("detailTransaksiLayanan"))
											.add(Restrictions.isNull("detailTransaksi"))
											.add(Restrictions.eq("biayaAlatMedisPerKelas", biayaAlatMedisPerKelas))
											.add(Restrictions.eq("jenisBiaya", jenisBiaya))
											.setProjection(Projections.rowCount()).uniqueResult()).intValue();
									if (count.equals(0)) {
										Biaya biaya = new Biaya();
										biaya.setBiayaAlatMedisPerKelas(biayaAlatMedisPerKelas);
										biaya.setJenisBiaya(jenisBiaya);
										session.save(biaya);
									}
								}
							}

							initHargaJual(alatMedis, tabpanel, onSave, tarifKhususPunyaAlatMedis);

							ubahStatusRubahSemuaEventListener.onEvent(null);
						}
					});
					ambilDataJenisBiayaBanyak.setWidth("750px");
					ambilDataJenisBiayaBanyak.setHeight("97%");
					ambilDataJenisBiayaBanyak.setVisible(true);
					ambilDataJenisBiayaBanyak.onModal();
				}

			});
			button.setParent(toolbar);

			toolbar.appendChild(semuahargasama = new Checkbox("Semua harga sama"));
			semuahargasama.setChecked(alatMedis.getSemuahargasama());

			semuahargasama.addEventListener("onCheck", ubahStatusRubahSemuaEventListener);

			Groupbox groupbox = new Groupbox();
			c.appendChild(groupbox);

			Session session = HibernateUtil.currentSession();

			TreeSet<JenisBiaya> jenisBiayas = new TreeSet<JenisBiaya>(
					CommonTarifAlatMedis.getJenisBiayas(alatMedis, tarifKhususPunyaAlatMedis));

			groupbox.appendChild(new Caption("Biaya Alat Medis"));

			gridHargaJual = new Grid();
			gridHargaJual.setParent(groupbox);
			gridHargaJual.setWidth("100%");
			gridHargaJual.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(gridHargaJual);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("Kelas");
			column.setWidth("20%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("%");
			column.setWidth("3%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Berubah");
			column.setWidth("5%");

			for (final JenisBiaya jenisBiaya : jenisBiayas) {

				column = new Column();
				column.setParent(columns);

				button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
				button.setWidth("100%");
				button.setStyle("font-size:xx-small;text-align: left;");
				Label label = new Label(jenisBiaya.getNama());
				label.setMultiline(true);
				label.setTooltiptext(jenisBiaya.getNama() + "-"
						+ (jenisBiaya.getAkun() == null ? "" : jenisBiaya.getAkun().toString()));
				label.setWidth("90%");
				column.appendChild(new Hbox(new Component[] { button, label }));

				button.setTooltiptext(jenisBiaya.getNama());
				button.setVisible(delete && alatMedis.getId() != null && jenisBiayas.size() > 1);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin menghapus data ini? Perlu diketahui bahwa data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Session session = HibernateUtil.currentSession();

												for (Biaya biaya : CommonTarifAlatMedis.getBiayaPerJenis(alatMedis,
														tarifKhususPunyaAlatMedis, jenisBiaya)) {
													session.delete(biaya);
												}

												initHargaJual(alatMedis, tabpanel, onSave, tarifKhususPunyaAlatMedis);

												ubahStatusRubahSemuaEventListener.onEvent(null);

											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonAlatMedis.java:1313");
												MyMessageboxConfig.show(Common.pesan(
														"Mohon maaf, data ini tidak dapat dihapus karena masih berkaitan dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau pindahkan terlebih dahulu seluruh data yang berkaitan; (2) periksa kembali keterkaitan antar data; (3) hubungi administrator apabila kendala masih berlanjut.",
																e.getMessage()));
											}

										}

									}
								});

					}
				});

			}

			column = new Column();
			column.setParent(columns);
			column.setLabel("Total");
			column.setAlign("right");

			Rows rows = new Rows();
			rows.setParent(gridHargaJual);

			List<KelasPerawatan> kelasPerawatans = session.createCriteria(KelasPerawatan.class)
					.addOrder(Order.asc("id")).list();

			for (KelasPerawatan kelasPerawatan : kelasPerawatans) {

				BiayaAlatMedisPerKelas biayaAlatMedisPerKelas;

				if (tarifKhususPunyaAlatMedis == null) {
					biayaAlatMedisPerKelas = CommonTarifAlatMedis.getBiayaAlatMedisPerKelas(alatMedis, kelasPerawatan);
				} else {
					biayaAlatMedisPerKelas = CommonTarifAlatMedis.getBiayaAlatMedisPerKelas(tarifKhususPunyaAlatMedis,
							kelasPerawatan);
				}

				final Checkbox pembagianBiayaDalamPersen = new Checkbox();
				final Checkbox hargaBisaDirubahSaatTransaksi = new Checkbox();

				pembagianBiayaDalamPersen.setChecked(biayaAlatMedisPerKelas.getPembagianBiayaDalamPersen());
				hargaBisaDirubahSaatTransaksi.setChecked(biayaAlatMedisPerKelas.getHargaBisaDirubahSaatTransaksi());

				final MyDoublebox total = new MyDoublebox(biayaAlatMedisPerKelas.getBiaya());
				total.setReadonly(!pembagianBiayaDalamPersen.isChecked());

				total.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						List<Row> rows = gridHargaJual.getRows().getChildren();
						if (!rows.isEmpty()) {
							for (Row row : rows) {
								for (Object oo : row.getChildren()) {

									if (oo instanceof MyDoublebox && oo == total) {
										Biaya biaya = (Biaya) ((MyDoublebox) oo).getAttribute("biaya");
										biaya.getBiayaAlatMedisPerKelas().setBiaya(total.getValue());

										biaya.getBiayaAlatMedisPerKelas()
												.setPembagianBiayaDalamPersen(pembagianBiayaDalamPersen.isChecked());

										((MyDoublebox) oo).setAttribute("biaya", biaya);

										for (Object ooo : row.getChildren()) {

											if (ooo instanceof Hbox) {

												MyDoublebox harga = (MyDoublebox) ((Hbox) ooo).getChildren().get(0);
												MyDoublebox persen = (MyDoublebox) ((Hbox) ooo).getChildren().get(1);

												Biaya myBiaya = (Biaya) harga.getAttribute("biaya");
												myBiaya.setJumlahTotal(total.getValue());
												myBiaya.getBiayaAlatMedisPerKelas().setBiaya(total.getValue());

												myBiaya.getBiayaAlatMedisPerKelas().setPembagianBiayaDalamPersen(
														pembagianBiayaDalamPersen.isChecked());

												harga.setAttribute("biaya", myBiaya);
												persen.setAttribute("biaya", myBiaya);

												harga.setValue(myBiaya.getJumlah());
												persen.setValue(myBiaya.getPersen());
											}

										}

									}
								}
							}
						}

						ubahStatusRubahSemuaEventListener.onEvent(arg0);
					}
				});

				final Row row = new Row();
				row.setAttribute("kelasPerawatan", kelasPerawatan);
				row.setAttribute("biayaAlatMedisPerKelas", biayaAlatMedisPerKelas);
				row.setStyle("border:0px;background: transparent;");
				row.setParent(rows);
				row.appendChild(new Label(kelasPerawatan.getNama()));

				row.appendChild(pembagianBiayaDalamPersen);

				final EventListener pembagianBiayaDalamPersenEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						for (Object oo : row.getChildren()) {
							if (oo instanceof Hbox) {
								MyDoublebox o1 = (MyDoublebox) ((Hbox) oo).getChildren().get(0);
								MyDoublebox o2 = (MyDoublebox) ((Hbox) oo).getChildren().get(1);

								o1.setReadonly(pembagianBiayaDalamPersen.isChecked());
								o2.setReadonly(!pembagianBiayaDalamPersen.isChecked());
							}

							if (oo instanceof MyDoublebox) {
								MyDoublebox o1 = (MyDoublebox) (oo);
								o1.setReadonly(!pembagianBiayaDalamPersen.isChecked());
							}
						}

						total.setReadonly(!pembagianBiayaDalamPersen.isChecked());

						ubahStatusRubahSemuaEventListener.onEvent(arg0);

					}
				};

				pembagianBiayaDalamPersen.addEventListener("onCheck", pembagianBiayaDalamPersenEventListener);

				row.appendChild(hargaBisaDirubahSaatTransaksi);

				hargaBisaDirubahSaatTransaksi.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (hargaBisaDirubahSaatTransaksi.isChecked()) {
							pembagianBiayaDalamPersen.setChecked(true);
							pembagianBiayaDalamPersenEventListener.onEvent(arg0);
						} else {
							ubahStatusRubahSemuaEventListener.onEvent(arg0);
						}
					}
				});

				final Row rowAkun = new Row();
				rowAkun.setVisible(false);
				rowAkun.setStyle("border:0px;background: transparent;");
				rowAkun.setParent(rows);
				rowAkun.appendChild(new Label());

				for (JenisBiaya jenisBiaya : jenisBiayas) {
					Biaya biaya = ((Biaya) session.createCriteria(Biaya.class)
							.add(Restrictions.eq("jenisBiaya", jenisBiaya))
							.add(biayaAlatMedisPerKelas.getId() == null ? Restrictions.sqlRestriction("false")
									: Restrictions.eq("biayaAlatMedisPerKelas", biayaAlatMedisPerKelas))

							.createAlias("biayaAlatMedisPerKelas", "biayaAlatMedisPerKelas")

							.add(Restrictions.isNull("detailTransaksiLayanan"))
							.add(Restrictions.isNull("detailTransaksi"))

							.setMaxResults(1).uniqueResult());
					if (biaya == null) {
						biaya = new Biaya();
						biaya.setBiayaAlatMedisPerKelas(biayaAlatMedisPerKelas);
						biaya.setJenisBiaya(jenisBiaya);
					}

					final Biaya tempBiaya = biaya;

					final MyDoublebox boxBiaya = new MyDoublebox();
					final MyDoublebox boxBiayaPersen = new MyDoublebox();

					boxBiaya.setReadonly(pembagianBiayaDalamPersen.isChecked());
					boxBiayaPersen.setReadonly(!pembagianBiayaDalamPersen.isChecked());

					final AmbilDataAkunKreditBanbox akun = new AmbilDataAkunKreditBanbox();

					boxBiaya.setAttribute("biaya", tempBiaya);
					boxBiaya.setValue(biaya.getJumlah());

					boxBiayaPersen.setAttribute("biaya", tempBiaya);
					boxBiayaPersen.setValue(biaya.getPersen());

					total.setAttribute("biaya", tempBiaya);

					boxBiaya.setWidth("100%");
					boxBiayaPersen.setWidth("70%");
					Hbox hbox;
					row.appendChild(hbox = new Hbox(new Component[] { boxBiaya, boxBiayaPersen, new Label(" %") }));
					hbox.setWidth("100%");
					hbox.setPack("center");
					hbox.setAlign("center");

					boxBiaya.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							tempBiaya.getBiayaAlatMedisPerKelas()
									.setPembagianBiayaDalamPersen(pembagianBiayaDalamPersen.isChecked());

							tempBiaya.setJumlah(boxBiaya.getValue());
							tempBiaya.setAkun((Akun) akun.getAttribute("akun"));

							Double totalNilai = 0.0;
							for (Object oo : row.getChildren()) {
								if (oo instanceof Hbox) {
									MyDoublebox o = (MyDoublebox) ((Hbox) oo).getChildren().get(0);
									totalNilai += o.getValue() == null ? 0.0 : o.getValue();
								}
							}

							System.out.println("totalNilai => " + totalNilai);

							tempBiaya.getBiayaAlatMedisPerKelas().setBiaya(totalNilai);
							total.setValue(totalNilai);

							tempBiaya.setJumlahTotal(total.getValue());
							boxBiayaPersen.setValue(tempBiaya.getPersen());
							boxBiaya.setAttribute("biaya", tempBiaya);
							total.setAttribute("biaya", tempBiaya);

							ubahStatusRubahSemuaEventListener.onEvent(arg0);
						}
					});

					boxBiayaPersen.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							tempBiaya.getBiayaAlatMedisPerKelas()
									.setPembagianBiayaDalamPersen(pembagianBiayaDalamPersen.isChecked());

							tempBiaya.setPersen(boxBiayaPersen.getValue());
							tempBiaya.setAkun((Akun) akun.getAttribute("akun"));

							tempBiaya.setJumlahTotal(total.getValue());
							boxBiaya.setValue(tempBiaya.getJumlah());
							boxBiayaPersen.setAttribute("biaya", tempBiaya);
							total.setAttribute("biaya", tempBiaya);

							ubahStatusRubahSemuaEventListener.onEvent(arg0);
						}
					});

					akun.setAttribute("biaya", tempBiaya);
					akun.setValue(tempBiaya.getAkun().toString());
					akun.setAttribute("akun", tempBiaya.getAkun());
					akun.setWidth("90%");
					akun.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tempBiaya.setJumlah(boxBiaya.getValue());
							tempBiaya.setAkun((Akun) akun.getAttribute("akun"));
							boxBiaya.setAttribute("biaya", tempBiaya);
						}
					});
					rowAkun.appendChild(akun);

					pembagianBiayaDalamPersenEventListener.onEvent(null);
				}
				row.appendChild(total);
				rowAkun.appendChild(new Label());
			}

			ubahStatusRubahSemuaEventListener.onEvent(null);

		}
	}
}
