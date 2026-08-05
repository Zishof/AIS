package ais.action.master.sirs.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
import ais.database.model.sirs.Biaya;
import ais.database.model.sirs.BiayaTindakanPerKelas;
import ais.database.model.sirs.JenisBiaya;
import ais.database.model.sirs.JenisTindakan;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.TarifKhusus;
import ais.database.model.sirs.TarifKhususPunyaTindakan;
import ais.database.model.sirs.Tindakan;
import ais.ui.util.MyDoublebox;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

public class CommonTindakan {

	public static void onUploadBiaya(Event event, final TarifKhusus tarifKhusus) throws Exception {

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
								// FIX bocor: JANGAN buka sesi per-sheet di sini; SELALU di-reassign
								// di kedua cabang (loop baris / else) sebelum dipakai -> dulu orphan.
								Session session = null;

								if (kelas.trim().equalsIgnoreCase("1000000-DATA")) {

									for (int i = 1; i <= sheet.getRows(); i++) {

										session = HibernateUtil.getSessionFactory().openSession();
										try {

											try {
												cell = sheet.getCell(0, i);
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:136");
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

											String paket = sheet.getCell(5, i) == null
													|| sheet.getCell(5, i).getContents() == null ? null
															: sheet.getCell(5, i).getContents().trim();

											String jenisTindakan = sheet.getCell(6, i) == null
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

											Tindakan tindakan = null;

											if (id != null && !id.trim().isEmpty()) {
												tindakan = (Tindakan) session.createCriteria(Tindakan.class)
														.setMaxResults(1)
														.add(Restrictions.idEq(Long.parseLong(id.trim())))
														.uniqueResult();
											}

											if (tindakan == null && kode != null && !kode.trim().isEmpty()) {
												tindakan = (Tindakan) session.createCriteria(Tindakan.class)
														.setMaxResults(1)
														.add(Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT))
														.uniqueResult();
											}

											if (tindakan == null && nama != null && !nama.trim().isEmpty()) {
												tindakan = (Tindakan) session.createCriteria(Tindakan.class)
														.setMaxResults(1)
														.add(Restrictions.ilike("nama", nama.trim(), MatchMode.EXACT))
														.uniqueResult();
											}

											if (tindakan == null) {
												tindakan = new Tindakan();
											}

											tindakan.setKode(kode);
											tindakan.setNama(nama);
											tindakan.setJenisPaket(paket);

											JenisTindakan myJenisTindakan = null;
											try {

												myJenisTindakan = (JenisTindakan) session
														.createCriteria(JenisTindakan.class)
														.add(Restrictions.idEq(Long
																.parseLong(jenisTindakan.trim().split("-")[0].trim())))
														.uniqueResult();
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonTindakan.java:234");

											}

											if (myJenisTindakan == null) {
												try {
													myJenisTindakan = (JenisTindakan) session
															.createCriteria(JenisTindakan.class)
															.add(Restrictions.ilike("nama",
																	jenisTindakan.trim().split("-")[1].trim(),
																	MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonTindakan.java:246");

												}

											}

											if (myJenisTindakan == null) {
												try {
													myJenisTindakan = (JenisTindakan) session
															.createCriteria(JenisTindakan.class)
															.add(Restrictions.ilike("nama", jenisTindakan.trim(),
																	MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonTindakan.java:259");

												}

											}

											tindakan.setJenisTindakan(myJenisTindakan);

											try {
												tindakan.setAktif(Boolean.parseBoolean(aktif));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:270");
											}

											try {
												tindakan.setSemuahargasama(Boolean.parseBoolean(sama));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:276");
											}

											try {
												tindakan.setTindakanLab(Boolean.parseBoolean(Lab));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:282");
											}

											try {
												tindakan.setTindakanOperasi(Boolean.parseBoolean(Operasi));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:288");
											}

											try {
												tindakan.setTindakanRadiologi(Boolean.parseBoolean(Radiologi));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:294");
											}

											try {
												tindakan.setTindakanVk(Boolean.parseBoolean(Vk));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:300");
											}

											try {
												tindakan.setTindakanRenalUnit(Boolean.parseBoolean(RenalUnit));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:306");
											}

											try {
												tindakan.setTindakanGizi(Boolean.parseBoolean(Gizi));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:312");
											}

											label.setValue(tindakan.toString());

											session.getTransaction().begin();
											session.saveOrUpdate(tindakan);
											session.getTransaction().commit();

											if (tarifKhusus != null) {
												Number jumlah = (Number) session
														.createCriteria(TarifKhususPunyaTindakan.class)
														.add(Restrictions.eq("tarifKhusus", tarifKhusus))
														.add(Restrictions.eq("tindakan", tindakan))
														.setProjection(Projections.rowCount()).uniqueResult();

												if (jumlah.intValue() == 0) {
													TarifKhususPunyaTindakan tarifKhususPunyaTindakan = new TarifKhususPunyaTindakan();
													tarifKhususPunyaTindakan.setAktif(true);
													tarifKhususPunyaTindakan.setTarifKhusus(tarifKhusus);
													tarifKhususPunyaTindakan.setTindakan(tindakan);
													session.getTransaction().begin();
													session.save(tarifKhususPunyaTindakan);
													session.getTransaction().commit();
												}
											}

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:340");
										}

										// session.disconnect();
										if (session.isOpen()) {session.disconnect();session.close();}
									}

								} else {

									session = HibernateUtil.getSessionFactory().openSession();

									// FIX bocor: penutupan sesi dijamin di finally
									KelasPerawatan kelasPerawatan = null;
									try {
										kelasPerawatan = (KelasPerawatan) session
												.createCriteria(KelasPerawatan.class)
												.add(Restrictions.idEq(Long.parseLong(kelas.split("-")[0].trim())))
												.uniqueResult();
									} finally {
										// session.disconnect();
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

												Tindakan tindakan = null;

												if (id != null && !id.trim().isEmpty()) {
													tindakan = (Tindakan) session.createCriteria(Tindakan.class)
															.setMaxResults(1)
															.add(Restrictions.idEq(Long.parseLong(id.trim())))
															.uniqueResult();
												}

												if (tindakan == null && kode != null && !kode.trim().isEmpty()) {
													tindakan = (Tindakan) session.createCriteria(Tindakan.class)
															.setMaxResults(1).add(Restrictions.ilike("kode",
																	kode.trim(), MatchMode.EXACT))
															.uniqueResult();
												}

												if (tindakan == null && nama != null && !nama.trim().isEmpty()) {
													tindakan = (Tindakan) session.createCriteria(Tindakan.class)
															.setMaxResults(1).add(Restrictions.ilike("nama",
																	nama.trim(), MatchMode.EXACT))
															.uniqueResult();
												}

												if (tindakan != null) {

													TarifKhususPunyaTindakan tarifKhususPunyaTindakan = null;

													if (tarifKhusus != null) {
														tarifKhususPunyaTindakan = (TarifKhususPunyaTindakan) session
																.createCriteria(TarifKhususPunyaTindakan.class)
																.add(Restrictions.eq("tarifKhusus", tarifKhusus))
																.add(Restrictions.eq("tindakan", tindakan))
																.setMaxResults(1).uniqueResult();
													}

													BiayaTindakanPerKelas biayaTindakanPerKelas = CommonTarifTindakan
															.getBiayaTindakanPerKelas(tindakan, kelasPerawatan,
																	tarifKhususPunyaTindakan, session);

													try {
														biayaTindakanPerKelas.setPembagianBiayaDalamPersen(
																Boolean.parseBoolean(persen));
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:440");
													}

													try {
														biayaTindakanPerKelas.setHargaBisaDirubahSaatTransaksi(
																Boolean.parseBoolean(berubah));
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:447");
													}

													try {
														biayaTindakanPerKelas.setBiaya(Double.parseDouble(tarif));
														session.getTransaction().begin();
														session.update(biayaTindakanPerKelas);
														session.getTransaction().commit();
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:456");
													}

													label.setValue(biayaTindakanPerKelas.toString());

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
																e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:480");
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
																		.add(Restrictions.eq("biayaTindakanPerKelas",
																				biayaTindakanPerKelas))

																		.add(Restrictions
																				.isNull("detailTransaksiLayanan"))
																		.add(Restrictions.isNull("detailTransaksi"))

																		.setMaxResults(1).uniqueResult());

																if (biaya == null) {
																	biaya = new Biaya();
																}

																biaya.setBiayaTindakanPerKelas(biayaTindakanPerKelas);
																biaya.setJenisBiaya(jenisBiaya);

																if (biayaTindakanPerKelas
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
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:526");
														}
													}
												}

											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:532");
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
									try { // FIX bocor: penutupan sesi dijamin di finally

								List<Tindakan> tindakansSama = session.createCriteria(Tindakan.class)
										.add(Restrictions.eq("semuahargasama", true)).list();

								List<KelasPerawatan> kelasPerawatans = session.createCriteria(KelasPerawatan.class)
										.add(Restrictions.ne("id", kelasPerawatanUmum.getId()))
										.addOrder(Order.asc("id")).list();

								for (Tindakan tindakan : tindakansSama) {
									label.setValue("Singkronisasi -- " + tindakan.toString());

									TarifKhususPunyaTindakan tarifKhususPunyaTindakan = null;

									if (tarifKhusus != null) {
										tarifKhususPunyaTindakan = (TarifKhususPunyaTindakan) session
												.createCriteria(TarifKhususPunyaTindakan.class)
												.add(Restrictions.eq("tarifKhusus", tarifKhusus))
												.add(Restrictions.eq("tindakan", tindakan)).setMaxResults(1)
												.uniqueResult();
									}

									BiayaTindakanPerKelas biayaTindakanPerKelasUmum = CommonTarifTindakan
											.getBiayaTindakanPerKelas(tindakan, kelasPerawatanUmum,
													tarifKhususPunyaTindakan, session);

									for (JenisBiaya jenisBiaya : jenisBiayaMap.values()) {

										Biaya biayaUmum = ((Biaya) session.createCriteria(Biaya.class)
												.add(Restrictions.eq("jenisBiaya", jenisBiaya))
												.add(Restrictions.eq("biayaTindakanPerKelas",
														biayaTindakanPerKelasUmum))

												.add(Restrictions.isNull("detailTransaksiLayanan"))
												.add(Restrictions.isNull("detailTransaksi"))

												.setMaxResults(1).uniqueResult());

										if (biayaUmum != null) {

											for (KelasPerawatan kelasPerawatan : kelasPerawatans) {

												BiayaTindakanPerKelas biayaTindakanPerKelas = CommonTarifTindakan
														.getBiayaTindakanPerKelas(tindakan, kelasPerawatan,
																tarifKhususPunyaTindakan, session);

												Biaya biaya = ((Biaya) session.createCriteria(Biaya.class)
														.add(Restrictions.eq("jenisBiaya", jenisBiaya))
														.add(Restrictions.eq("biayaTindakanPerKelas",
																biayaTindakanPerKelas))

														.add(Restrictions.isNull("detailTransaksiLayanan"))
														.add(Restrictions.isNull("detailTransaksi"))

														.setMaxResults(1).uniqueResult());

												if (biaya == null) {
													biaya = new Biaya();
												}

												biayaTindakanPerKelas.setBiaya(biayaTindakanPerKelasUmum.getBiaya());
												biayaTindakanPerKelas.setPembagianBiayaDalamPersen(
														biayaTindakanPerKelasUmum.getPembagianBiayaDalamPersen());
												biayaTindakanPerKelas.setHargaBisaDirubahSaatTransaksi(
														biayaTindakanPerKelasUmum.getHargaBisaDirubahSaatTransaksi());

												biaya.setBiayaTindakanPerKelas(biayaTindakanPerKelas);
												biaya.setJenisBiaya(jenisBiaya);

												biaya.setPersen(biayaUmum.getPersen());
												biaya.setJumlah(biayaUmum.getJumlah());
												biaya.setJumlahTotal(biayaUmum.getJumlahTotal());

												session.getTransaction().begin();
												session.update(biayaTindakanPerKelas);
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
							ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:637");

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

			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:678");
			// e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static void onDownloadBiaya(Event event, final TarifKhusus tarifKhusus) throws Exception {

		Clients.showBusy("memproses data ..");

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/biaya_tindakan_" + (Common.dateFormat6.get().format(new Date())) + ".xls");

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

					List<Tindakan> tindakans = null;

					if (tarifKhusus != null) {
						tindakans = session.createCriteria(TarifKhususPunyaTindakan.class)
								.setProjection(Projections.groupProperty("tindakan")).list();
					} else {
						tindakans = session.createCriteria(Tindakan.class).addOrder(Order.asc("nama")).list();
					}

					List<JenisBiaya> jenisBiayas = session.createCriteria(Biaya.class)
							.add(Restrictions.isNull("detailTransaksiLayanan"))
							.add(Restrictions.isNull("detailTransaksi"))
							.setProjection(Projections.groupProperty("jenisBiaya"))
							.createAlias("biayaTindakanPerKelas", "biayaTindakanPerKelas")
							.createAlias("jenisBiaya", "jenisBiaya").add(Restrictions.eq("jenisBiaya.aktif", true))
							.list();

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
						rowhead.createCell(5).setCellValue("Paket");
						rowhead.createCell(6).setCellValue("Jenis Tindakan");

						rowhead.createCell(7).setCellValue("Lab");
						rowhead.createCell(8).setCellValue("Operasi");
						rowhead.createCell(9).setCellValue("Radiologi");
						rowhead.createCell(10).setCellValue("Vk");
						rowhead.createCell(11).setCellValue("Renal Unit");
						rowhead.createCell(12).setCellValue("Gizi");

						int i = 1;
						for (Tindakan tindakan : tindakans) {

							session = HibernateUtil.getSessionFactory().openSession();

							label.setValue(tindakan.toString());

							try {

								HSSFRow row = sheet.createRow(i);
								row.createCell(0).setCellValue(tindakan.getId());
								row.createCell(1).setCellValue(tindakan.getKode());
								row.createCell(2).setCellValue(tindakan.getNama());
								row.createCell(3).setCellValue(tindakan.getAktif());
								row.createCell(4).setCellValue(tindakan.getSemuahargasama());

								row.createCell(5).setCellValue(tindakan.getJenisPaket());

								row.createCell(6)
										.setCellValue(tindakan.getJenisTindakan() == null ? ""
												: (tindakan.getJenisTindakan().getId() + "-"
														+ tindakan.getJenisTindakan().getNama().trim()));

								row.createCell(7).setCellValue(tindakan.getTindakanLab());
								row.createCell(8).setCellValue(tindakan.getTindakanOperasi());
								row.createCell(9).setCellValue(tindakan.getTindakanRadiologi());
								row.createCell(10).setCellValue(tindakan.getTindakanVk());
								row.createCell(11).setCellValue(tindakan.getTindakanRenalUnit());
								row.createCell(12).setCellValue(tindakan.getTindakanGizi());
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:778");
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
						for (Tindakan tindakan : tindakans) {

							session = HibernateUtil.getSessionFactory().openSession();

							BiayaTindakanPerKelas biayaTindakanPerKelas;
							TarifKhususPunyaTindakan tarifKhususPunyaTindakan = null;

							if (tarifKhusus != null) {
								tarifKhususPunyaTindakan = (TarifKhususPunyaTindakan) session
										.createCriteria(TarifKhususPunyaTindakan.class)
										.add(Restrictions.eq("tarifKhusus", tarifKhusus))
										.add(Restrictions.eq("tindakan", tindakan)).setMaxResults(1).uniqueResult();
							}

							if (tarifKhususPunyaTindakan == null) {
								biayaTindakanPerKelas = CommonTarifTindakan.getBiayaTindakanPerKelas(tindakan,
										kelasPerawatan, null, session);
							} else {
								biayaTindakanPerKelas = CommonTarifTindakan.getBiayaTindakanPerKelas(tindakan,
										kelasPerawatan, tarifKhususPunyaTindakan, session);
							}

							label.setValue(biayaTindakanPerKelas.toString());

							try {

								HSSFRow row = sheet.createRow(i);
								row.createCell(0).setCellValue(tindakan.getId());
								row.createCell(1).setCellValue(tindakan.getKode());
								row.createCell(2).setCellValue(tindakan.getNama());
								row.createCell(3).setCellValue(biayaTindakanPerKelas.getBiaya());

								row.createCell(4).setCellValue(biayaTindakanPerKelas.getPembagianBiayaDalamPersen());
								row.createCell(5)
										.setCellValue(biayaTindakanPerKelas.getHargaBisaDirubahSaatTransaksi());

								index = 6;
								for (JenisBiaya jenisBiaya : jenisBiayas) {
									Biaya biaya = ((Biaya) session.createCriteria(Biaya.class)
											.add(Restrictions.eq("jenisBiaya", jenisBiaya))
											.add(Restrictions.eq("biayaTindakanPerKelas", biayaTindakanPerKelas))

											.add(Restrictions.isNull("detailTransaksiLayanan"))
											.add(Restrictions.isNull("detailTransaksi"))

											.setMaxResults(1).uniqueResult());

									if (biayaTindakanPerKelas.getPembagianBiayaDalamPersen()) {
										row.createCell(index).setCellValue(biaya == null ? 0.0 : biaya.getPersen());
									} else {
										row.createCell(index).setCellValue(biaya == null ? 0.0 : biaya.getJumlah());
									}
									index++;
								}

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:863");
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

					label.setValue("");

				} catch (Exception ex) {
					System.out.println(ex);
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:882");
					label.setValue("");
				} finally {
					// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
					// finally menjamin penutupan walau exception (idempoten via isOpen()).
					if (session != null && session.isOpen()) {
						try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonTindakan.java:888");}
						try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonTindakan.java:889");}
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
				if (label.getValue().isEmpty()) {
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
		public boolean saveDetail(Tindakan tindakan, TarifKhususPunyaTindakan tarifKhususPunyaTindakan) {
			Session session = HibernateUtil.currentSession();
			List<Row> rows = gridHargaJual.getRows().getChildren();
			for (Row row : rows) {
				KelasPerawatan kelasPerawatan = (KelasPerawatan) row.getAttribute("kelasPerawatan");
				BiayaTindakanPerKelas biayaTindakanPerKelas = (BiayaTindakanPerKelas) row
						.getAttribute("biayaTindakanPerKelas");

				if (biayaTindakanPerKelas != null) {
					Double nilaiBiaya = ((MyDoublebox) row.getChildren().get(row.getChildren().size() - 1)).getValue();
					nilaiBiaya = nilaiBiaya == null ? 0.0 : nilaiBiaya;

					Checkbox persen = (Checkbox) row.getChildren().get(1);
					Checkbox berubah = (Checkbox) row.getChildren().get(2);

					biayaTindakanPerKelas.setHargaBisaDirubahSaatTransaksi(berubah.isChecked());
					biayaTindakanPerKelas.setPembagianBiayaDalamPersen(persen.isChecked());

					if (tarifKhususPunyaTindakan == null) {
						biayaTindakanPerKelas.setTindakan(tindakan);
						biayaTindakanPerKelas.setTarifKhususPunyaTindakan(null);
					} else {
						biayaTindakanPerKelas.setTarifKhususPunyaTindakan(tarifKhususPunyaTindakan);
						biayaTindakanPerKelas.setTindakan(null);
					}

					biayaTindakanPerKelas.setKelasPerawatan(kelasPerawatan);
					biayaTindakanPerKelas.setBiaya(nilaiBiaya);

					session.saveOrUpdate(biayaTindakanPerKelas);
					row.setAttribute("biayaTindakanPerKelas", biayaTindakanPerKelas);

					for (Object oo : row.getChildren()) {

						if (oo instanceof Hbox) {

							MyDoublebox o = (MyDoublebox) ((Hbox) oo).getChildren().get(0);
							MyDoublebox o1 = (MyDoublebox) ((Hbox) oo).getChildren().get(1);

							Biaya biaya = (Biaya) o.getAttribute("biaya");
							biaya.setBiayaTindakanPerKelas(biayaTindakanPerKelas);
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
						BiayaTindakanPerKelas biayaTindakanPerKelas = (BiayaTindakanPerKelas) row
								.getAttribute("biayaTindakanPerKelas");
						if (biayaTindakanPerKelas != null) {
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

									biayaTindakanPerKelas.setBiaya(totalNilai);

									MyDoublebox total = (MyDoublebox) row.getChildren()
											.get(row.getChildren().size() - 1);

									total.setValue(biayaTindakanPerKelas.getBiaya());
									row.setAttribute("biayaTindakanPerKelas", biayaTindakanPerKelas);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonTindakan.java:1089");
								}
							}
						}
					}
				}
			}
		};

		@SuppressWarnings("unchecked")
		public void initHargaJual(final Tindakan tindakan, final Tabpanel tabpanel, final String jenisPaket,
				final OnSave onSave, final TarifKhususPunyaTindakan tarifKhususPunyaTindakan) throws Exception {

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

					List<JenisBiaya> jenisBiayas = CommonTarifTindakan.getJenisBiayas(tindakan, jenisPaket,
							tarifKhususPunyaTindakan);

					AmbilDataJenisBiayaBanyak ambilDataJenisBiayaBanyak = new AmbilDataJenisBiayaBanyak(jenisBiayas,
							jenisPaket.equals(Tindakan.JENIS_PERAWATAN_PAKET) ? JenisBiaya.TIPE_PAKET
									: JenisBiaya.TIPE_TINDAKAAN);
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
								BiayaTindakanPerKelas biayaTindakanPerKelas = (BiayaTindakanPerKelas) mainRow
										.getAttribute("biayaTindakanPerKelas");
								for (JenisBiaya jenisBiaya : jenisBiayas) {
									Integer count = ((Number) session.createCriteria(Biaya.class)
											.add(Restrictions.isNull("detailTransaksiLayanan"))
											.add(Restrictions.isNull("detailTransaksi"))
											.add(Restrictions.eq("biayaTindakanPerKelas", biayaTindakanPerKelas))
											.add(Restrictions.eq("jenisBiaya", jenisBiaya))
											.setProjection(Projections.rowCount()).uniqueResult()).intValue();
									if (count.equals(0)) {
										Biaya biaya = new Biaya();
										biaya.setBiayaTindakanPerKelas(biayaTindakanPerKelas);
										biaya.setJenisBiaya(jenisBiaya);
										session.save(biaya);
									}
								}
							}

							initHargaJual(tindakan, tabpanel, jenisPaket, onSave, tarifKhususPunyaTindakan);

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
			semuahargasama.setChecked(tindakan.getSemuahargasama());

			semuahargasama.addEventListener("onCheck", ubahStatusRubahSemuaEventListener);

			Groupbox groupbox = new Groupbox();
			c.appendChild(groupbox);

			Session session = HibernateUtil.currentSession();

			TreeSet<JenisBiaya> jenisBiayas = new TreeSet<JenisBiaya>(
					CommonTarifTindakan.getJenisBiayas(tindakan, jenisPaket, tarifKhususPunyaTindakan));

			groupbox.appendChild(new Caption("Biaya Tindakan"));

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
				button.setVisible(delete && tindakan.getId() != null && jenisBiayas.size() > 1);
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
												for (Biaya biaya : CommonTarifTindakan.getBiayaPerJenis(tindakan,
														tarifKhususPunyaTindakan, jenisBiaya)) {
													session.delete(biaya);
												}

												initHargaJual(tindakan, tabpanel, jenisPaket, onSave,
														tarifKhususPunyaTindakan);

												ubahStatusRubahSemuaEventListener.onEvent(null);

											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonTindakan.java:1260");
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

				BiayaTindakanPerKelas biayaTindakanPerKelas;

				if (tarifKhususPunyaTindakan == null) {
					biayaTindakanPerKelas = CommonTarifTindakan.getBiayaTindakanPerKelas(tindakan, kelasPerawatan);
				} else {
					biayaTindakanPerKelas = CommonTarifTindakan.getBiayaTindakanPerKelas(tarifKhususPunyaTindakan,
							kelasPerawatan);
				}

				final Checkbox pembagianBiayaDalamPersen = new Checkbox();
				final Checkbox hargaBisaDirubahSaatTransaksi = new Checkbox();

				pembagianBiayaDalamPersen.setChecked(biayaTindakanPerKelas.getPembagianBiayaDalamPersen());
				hargaBisaDirubahSaatTransaksi.setChecked(biayaTindakanPerKelas.getHargaBisaDirubahSaatTransaksi());

				final MyDoublebox total = new MyDoublebox(biayaTindakanPerKelas.getBiaya());
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
										biaya.getBiayaTindakanPerKelas().setBiaya(total.getValue());

										biaya.getBiayaTindakanPerKelas()
												.setPembagianBiayaDalamPersen(pembagianBiayaDalamPersen.isChecked());

										((MyDoublebox) oo).setAttribute("biaya", biaya);

										for (Object ooo : row.getChildren()) {

											if (ooo instanceof Hbox) {

												MyDoublebox harga = (MyDoublebox) ((Hbox) ooo).getChildren().get(0);
												MyDoublebox persen = (MyDoublebox) ((Hbox) ooo).getChildren().get(1);

												Biaya myBiaya = (Biaya) harga.getAttribute("biaya");
												myBiaya.setJumlahTotal(total.getValue());
												myBiaya.getBiayaTindakanPerKelas().setBiaya(total.getValue());

												myBiaya.getBiayaTindakanPerKelas().setPembagianBiayaDalamPersen(
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
				row.setAttribute("biayaTindakanPerKelas", biayaTindakanPerKelas);
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
							.add(biayaTindakanPerKelas.getId() == null ? Restrictions.sqlRestriction("false")
									: Restrictions.eq("biayaTindakanPerKelas", biayaTindakanPerKelas))

							.createAlias("biayaTindakanPerKelas", "biayaTindakanPerKelas")

							.add(Restrictions.isNull("detailTransaksiLayanan"))
							.add(Restrictions.isNull("detailTransaksi"))

							.setMaxResults(1).uniqueResult());
					if (biaya == null) {
						biaya = new Biaya();
						biaya.setBiayaTindakanPerKelas(biayaTindakanPerKelas);
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

							tempBiaya.getBiayaTindakanPerKelas()
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

							tempBiaya.getBiayaTindakanPerKelas().setBiaya(totalNilai);
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

							tempBiaya.getBiayaTindakanPerKelas()
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
