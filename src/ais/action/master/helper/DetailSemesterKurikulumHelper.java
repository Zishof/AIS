package ais.action.master.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.EksporFromFeederAction;
import ais.action.master.KurikulumAction;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.feeder.util.FeederJSONImport;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.PaketPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailSemesterKurikulumHelper {

	private String[] contents = new String[] { "id", "matakuliah", "semester", "tahap",
			"jumlahPertemuanPerkuliahanDefault", "indukMatakuliah", "matakuliah.sks", "aktif" };
	private Kurikulum kurikulum;

	public void display(final Kurikulum kurikulum, final Component component, final MyWindow window) {
		display(kurikulum, null, component, window);
	}

	@SuppressWarnings("unchecked")
	public void display(final Kurikulum kurikulum, final PaketPerkuliahan paketPerkuliahan, final Component component,
			final MyWindow window) {
		this.kurikulum = kurikulum;
		Common.clear(component);

		final ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 2800px;");
		groupbox.setParent(component);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.ambilDosen() == null) {

					Toolbar toolbar = new Toolbar();
					// toolbar.setHeight("25px");
					toolbar.setParent(groupbox);
					MyToolbarbuttonConfig cetakToolbarbutton = Common
							.cetakDataCustomButton(KurikulumPunyaMatakuliah.class, new DataCriteria() {

								@Override
								public Criteria initCriteria(boolean order) {
									Session session = HibernateUtil.currentSession();
									Criteria criteria = session.createCriteria(KurikulumPunyaMatakuliah.class)
											.createAlias("matakuliah", "matakuliah").addOrder(Order.asc("semester"))
											.addOrder(Order.asc("matakuliah.nama"))
											.add(Restrictions.eq("kurikulum", kurikulum));
									return criteria;
								}
							}, "Download Kurikulum", "/img/excel.png", contents);
					toolbar.appendChild(cetakToolbarbutton);

					MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
							"Upload Kurikulum" + Common.ukuranLabelFileUpload(), "/img/excel.png");
					upload.setUpload(Common.ukuranFileUpload());
					upload.addEventListener("onUpload", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							UploadEvent uploadEvent = (UploadEvent) event;
							Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
							if (media.getName().toLowerCase().endsWith("xlsx")) {

								InputStream inputStream = media.getStreamData();
								// System.out.println("media = " + media);
								File file = new File(
										Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
								// System.out.println("file = " +
								// file.getAbsolutePath());
								file.getParentFile().mkdirs();
								FileOutputStream fileOutputStream = new FileOutputStream(file);
								int c;
								while ((c = inputStream.read()) != -1) {
									fileOutputStream.write(c);
								}
								fileOutputStream.close();
								inputStream.close();

								uploadKurikulum(file, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										display(kurikulum, paketPerkuliahan, component, window);
									}
								}, contents);

							} else {
								MyMessageboxConfig.show(
										"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
												+ media,
										"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
							}
						}
					});
					toolbar.appendChild(upload);

					MyToolbarbuttonConfig simkronkanTahap = new MyToolbarbuttonConfig("Singkronkan Silabus",
							"/img/svg/check2.svg");
					toolbar.appendChild(simkronkanTahap);
					simkronkanTahap.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							KurikulumAction.copySilabusDariPerkuliahan(kurikulum, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									display(kurikulum, paketPerkuliahan, component, window);
								}
							});
						}
					});

					simkronkanTahap = new MyToolbarbuttonConfig("Bersihkan Matakuliah Double", "/img/svg/check2.svg");
					toolbar.appendChild(simkronkanTahap);
					simkronkanTahap.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									String sql = "select a.id,a.matakuliah from ( "
											+ "	select max(id) as id,matakuliah from kurikulum_punya_matakuliah where kurikulum = "
											+ kurikulum.getId() + " group by matakuliah,semester having count(*)>1 "
											+ "	union all "
											+ "	select min(id) as id,matakuliah from kurikulum_punya_matakuliah where kurikulum = "
											+ kurikulum.getId() + " group by matakuliah,semester having count(*)>1  "
											+ ") a";

									Session session = HibernateUtil.currentSession();

									Map<Long, Long> idsYangmauDihapusNonAktif = new HashMap<Long, Long>();
									Map<Long, Long> idsYangmauDihapus = new HashMap<Long, Long>();
									List<Object[]> ids = session.createSQLQuery(sql).list();
									for (Object[] a : ids) {
										if (a != null && a[0] != null && a[1] != null) {
											Long id = Long.parseLong(a[0].toString());
											Long matakuliah = Long.parseLong(a[1].toString());
											if (matakuliah != null && !idsYangmauDihapus.containsKey(matakuliah)
													&& !idsYangmauDihapusNonAktif.containsKey(matakuliah)) {
												int count = ((Number) session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("kurikulumPunyaMatakuliah.id", id))
														.setProjection(Projections.rowCount()).uniqueResult())
														.intValue();
												if (count == 0) {
													idsYangmauDihapus.put(matakuliah, id);
												} else {
													idsYangmauDihapusNonAktif.put(matakuliah, id);
												}
											}
										}
									}

									System.out.println("idsYangmauDihapus -> " + idsYangmauDihapus);

									if (!idsYangmauDihapus.isEmpty()) {
										List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = session
												.createCriteria(KurikulumPunyaMatakuliah.class)
												.add(Restrictions.in("id", idsYangmauDihapus.values())).list();
										for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
											session.delete(kurikulumPunyaMatakuliah);
										}
										session.flush();
									}

									System.out.println("idsYangmauDihapusNonAktif -> " + idsYangmauDihapusNonAktif);

									if (!idsYangmauDihapusNonAktif.isEmpty()) {
										List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = session
												.createCriteria(KurikulumPunyaMatakuliah.class)
												.add(Restrictions.in("id", idsYangmauDihapusNonAktif.values())).list();
										for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
											kurikulumPunyaMatakuliah.setAktif(false);
											session.update(kurikulumPunyaMatakuliah);
										}
										session.flush();
									}

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											display(kurikulum, paketPerkuliahan, component, window);
										}
									});

								}
							});

						}
					});

					simkronkanTahap = new MyToolbarbuttonConfig("Singkronkan dengan jadwal", "/img/svg/check2.svg");
					toolbar.appendChild(simkronkanTahap);
					simkronkanTahap.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									Session session = HibernateUtil.currentSession();

									List<Perkuliahan> perkuliahans = ConstantValues
											.simpleList(
													session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
															.add(Restrictions.eq("kurikulum", kurikulum)),
													Perkuliahan.class);

									for (Perkuliahan perkuliahan : perkuliahans) {

										KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) ConstantValues
												.simpleObject(
														session.createCriteria(KurikulumPunyaMatakuliah.class)
																.add(Restrictions.eq("kurikulum", kurikulum))
																.add(Restrictions.eq("matakuliah",
																		perkuliahan.getMatakuliah()))
																.setMaxResults(1),
														KurikulumPunyaMatakuliah.class);

										if (kurikulumPunyaMatakuliah == null) {
											kurikulumPunyaMatakuliah = new KurikulumPunyaMatakuliah();
											kurikulumPunyaMatakuliah.setMatakuliah(perkuliahan.getMatakuliah());
											kurikulumPunyaMatakuliah.setKurikulum(kurikulum);
											kurikulumPunyaMatakuliah.setSemester(perkuliahan.getSemester());
											session.save(kurikulumPunyaMatakuliah);
											session.flush();
										}
										perkuliahan.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
										Common.refreshUpdate(session, perkuliahan);
										session.flush();
									}

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											display(kurikulum, paketPerkuliahan, component, window);
										}
									});

								}
							});

						}
					});

					if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
							&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

						MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim ke Feeder",
								"/img/Finance-Invoice-icon.png");
						buttonTagihan.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								MyMessageboxConfig.show("Apakah yakin ingin mengirim ke feeder ?", "Pertanyaan",
										MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
										new EventListener() {

											@Override
											public void onEvent(Event event) throws Exception {
												int i = Integer.parseInt(event.getData().toString());
												if (i == MyMessageboxConfig.OK) {

													String[] kon = EksporFromFeederAction.koneksi();
													final String ip = kon[0];
													final String port = kon[1];
													final String username = kon[2];
													final String password = kon[3];
													final String url = kon[4];

													if (!EksporFromFeederAction.exists(url)) {

														MyMessageboxConfig.show(
																ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
																"Peringatan", MyMessageboxConfig.OK,
																MyMessageboxConfig.EXCLAMATION);
														return;
													}

													final List<String> errorLog = new ArrayList<String>();
													final Label myLabelProsesDetail = Common
															.displayLoadBar(new EventListener() {

																@Override
																public void onEvent(Event arg0) throws Exception {
																	if (arg0 != null && !arg0.getName().isEmpty()) {
																		EksporFromFeederAction.display();
																		MyMessageboxConfig.show(arg0.getName(), "Info",
																				MyMessageboxConfig.OK,
																				MyMessageboxConfig.EXCLAMATION);
																	}

																	if (!errorLog.isEmpty()) {
																		String err = "";
																		for (String s : errorLog) {
																			err += err.isEmpty() ? s
																					: "\n----------------------------------------------------------------------------------------------------------\n"
																							+ s;
																		}

																		MyMessageboxConfig.show(
																				"Error Terjadi, catatan error akan otomatis ter-download",
																				"Error Terjadi", MyMessageboxConfig.OK,
																				MyMessageboxConfig.EXCLAMATION);

																		File file = new File(
																				Common.REAL_PATH + "/tmp/error_"
																						+ Common.randLong() + ".txt");
																		if (!file.getParentFile().exists()) {
																			file.getParentFile().mkdirs();
																		}
																		FileUtils.writeStringToFile(file, err);
																		Filedownload.save(file, "text/plain");
																	}

																	Common.createDefaultTimer(new EventListener() {

																		@Override
																		public void onEvent(Event arg0)
																				throws Exception {
																			display(kurikulum, paketPerkuliahan,
																					component, window);
																		}
																	});
																}
															});

													new Thread(new Runnable() {

														@Override
														public void run() {
															try {
															try {
																FeederConnector feederConnector = new FeederConnector(
																		ip, Integer.parseInt(port), null);

																String token = feederConnector.getToken(username,
																		password);
																System.out.println("TOKEN => " + token);

																if (token == null || token.trim().isEmpty() || token
																		.trim().toLowerCase().startsWith("error")) {
																	myLabelProsesDetail.setValue(
																			"Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
																	return;
																}

																FeederExporter feederImporter = new FeederExporter(
																		feederConnector, token, null, null,
																		myLabelProsesDetail);

																List<Kurikulum> tbmusers = new ArrayList<Kurikulum>();
																tbmusers.add(kurikulum);
																int size = tbmusers.size();
																int index = 1;
																for (Kurikulum kurikulum : tbmusers) {
																	myLabelProsesDetail.setValue(
																			"Memproses " + kurikulum.getNama() + " ("
																					+ Common.numberFormat.get().format(
																							(index * 100.0) / size)
																					+ "%");
																	index++;
																	feederImporter.kurikulum(kurikulum, errorLog);

																	Session session = HibernateUtil
																			.currentNativeSession();
																	List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = ConstantValues
																			.simpleList(session.createCriteria(
																					KurikulumPunyaMatakuliah.class)
																					.add(Restrictions.eq("kurikulum",
																							kurikulum))
																					.add(Restrictions.eq("aktif",
																							true)),
																					KurikulumPunyaMatakuliah.class);
																	// session.disconnect();
																	if (session.isOpen()) {session.disconnect();session.close();}
																	HibernateUtil.closeSession();

																	for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
																		feederImporter.kurikulumPunyaMatakuliah(
																				kurikulumPunyaMatakuliah, errorLog);
																	}
																}
																tbmusers.clear();
																tbmusers = null;
															} catch (Exception e) {
																e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailSemesterKurikulumHelper.java:448");
															}

															myLabelProsesDetail.setValue("");
																													} finally {
																ais.database.hibernate.HibernateUtil.closeSession();
															}
														}
													}).start();

												}

											}
										});

							}
						});
						toolbar.appendChild(buttonTagihan);

						buttonTagihan = new MyToolbarbuttonConfig("Ambil dari feeder", "/img/Button-Refresh-icon.png");
						buttonTagihan.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								MyMessageboxConfig.show("Apakah yakin ingin mengambil data dari feeder ?", "Pertanyaan",
										MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
										new EventListener() {

											@Override
											public void onEvent(Event event) throws Exception {
												int i = Integer.parseInt(event.getData().toString());
												if (i == MyMessageboxConfig.OK) {

													String[] kon = EksporFromFeederAction.koneksi();
													final String ip = kon[0];
													final String port = kon[1];
													final String username = kon[2];
													final String password = kon[3];
													final String url = kon[4];

													if (!EksporFromFeederAction.exists(url)) {

														MyMessageboxConfig.show(
																ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
																"Peringatan", MyMessageboxConfig.OK,
																MyMessageboxConfig.EXCLAMATION);
														return;
													}

													final Label myLabelProsesDetail = Common
															.displayLoadBar(new EventListener() {

																@Override
																public void onEvent(Event arg0) throws Exception {
																	if (arg0 != null && !arg0.getName().isEmpty()) {
																		EksporFromFeederAction.display();
																		MyMessageboxConfig.show(arg0.getName(), "Info",
																				MyMessageboxConfig.OK,
																				MyMessageboxConfig.EXCLAMATION);
																	}
																	Common.createDefaultTimer(new EventListener() {

																		@Override
																		public void onEvent(Event arg0)
																				throws Exception {
																			display(kurikulum, paketPerkuliahan,
																					component, window);
																		}
																	});
																}
															});

													new Thread(new Runnable() {

														@Override
														public void run() {
															try {
																FeederConnector feederConnector = new FeederConnector(
																		ip, Integer.parseInt(port), null);

																String token = feederConnector.getToken(username,
																		password);
																System.out.println("TOKEN => " + token);

																if (token == null || token.trim().isEmpty() || token
																		.trim().toLowerCase().startsWith("error")) {
																	myLabelProsesDetail.setValue(
																			"Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
																	return;
																}

																JSONArray dataMatkulKurikulum = feederConnector.getData(
																		"GetMatkulKurikulum", token,
																		"id_kurikulum='" + kurikulum.getFeeder() + "'",
																		"", "5000", "0");

																for (int j = 0; j < dataMatkulKurikulum.length(); j++) {

																	JSONObject kurSub = dataMatkulKurikulum
																			.getJSONObject(j);
																	FeederJSONImport.kurikulumPunyaMatakuliah(kurSub);
																}

															} catch (Exception e) {
																e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailSemesterKurikulumHelper.java:554");
															}

															myLabelProsesDetail.setValue("");
														}
													}).start();

												}

											}
										});

							}
						});
						toolbar.appendChild(buttonTagihan);
					}

				}

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setMold("paging");
				grid.setPageSize(50);
				grid.getPagingChild().setMold("os");
				grid.setParent(groupbox);
				grid.setWidth("100%");
				grid.setHeight("1000px");
				grid.setStyle("min-height: 2800px;");

				Columns columns = new Columns();

				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("");
				column.setWidth("50px");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Semester");

				if (ConstantValues.aktifkanTahapanKurikulum) {
					column = new MyColumnConfig();
					column.setParent(columns);
					column.setLabel("Tahapan");
				}

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Jumlah Matakuliah");
				column.setWidth("20%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setLabel("Jumlah Jadwal");
				column.setWidth("20%");

				Rows rows = new Rows();
				rows.setParent(grid);

				for (int i = 1; i <= kurikulum.getJurusan().getJenjang().getJumlahSemester(); i++) {

					final int semester = i;
					Row row = new Row();
					row.setValign("top");
					row.setParent(rows);

					final MyDetail detail1 = new MyDetail();
					row.appendChild(detail1);
					detail1.addEventListener("onOpen", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							MatakuliahKurikulumHelper matakuliahKurikulumHelper = new MatakuliahKurikulumHelper();
							matakuliahKurikulumHelper.display(kurikulum, paketPerkuliahan, detail1, semester, null);
						}
					});

					Label label = new Label(Common.numberFormat.get().format(semester));
					row.appendChild(label);

					if (ConstantValues.aktifkanTahapanKurikulum) {
						List<Object[]> tahaps = HibernateUtil.currentSession()
								.createSQLQuery("select tahap,count(*) as jumlah from kurikulum_punya_matakuliah "
										+ " where kurikulum = " + kurikulum.getId() + " and semester = " + i
										+ " group by tahap order by tahap")
								.list();
						Vbox vbox = new Vbox();
						vbox.setParent(row);
						for (Object[] objects : tahaps) {
							Integer tahap = objects[0] == null ? null : ((Number) objects[0]).intValue();
							Integer jumlah = objects[1] == null ? 0 : ((Number) objects[1]).intValue();
							new Label((tahap == null ? "Tanpa tahap" : "Tahap " + tahap) + " terdapat " + jumlah
									+ " matakuliah").setParent(vbox);
						}
					}

					final Label jumlah = new Label();
					row.appendChild(jumlah);

					final Label jmlJadwal = new Label("..");
					jmlJadwal.setParent(row);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = HibernateUtil.currentSession();
							Integer count = ((Number) session.createCriteria(KurikulumPunyaMatakuliah.class)
									.add(Restrictions.eq("kurikulum", kurikulum))
									.add(Restrictions.eq("semester", semester)).setProjection(Projections.rowCount())
									.uniqueResult()).intValue();
							jumlah.setValue(Common.numberFormat.get().format(count));

							count = ((Number) session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.createAlias("kurikulumPunyaMatakuliah", "kurikulumPunyaMatakuliah")
									.add(Restrictions.eq("kurikulumPunyaMatakuliah.semester", semester))
									.add(Restrictions.eq("kurikulumPunyaMatakuliah.kurikulum", kurikulum))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();
							jmlJadwal.setValue(Common.numberFormat.get().format(count));
						}
					});
				}
			}
		});

	}

	public void uploadKurikulum(File file, EventListener eventListener, String[] contents) throws Exception {

		// Laporan hasil per baris. Sebelumnya baris yang matakuliahnya tidak ditemukan
		// dilewati DIAM-DIAM sementara kotak "berhasil dilakukan" tetap tampil tanpa syarat.
		// Kini setiap baris dicatat, laporan otomatis diunduh, dan ringkasan ditampilkan.
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Kurikulum");

		XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
		XSSFSheet sheet = workbook.getSheetAt(0);

		for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
			String kodeMk = "";
			Session session = null;
			org.hibernate.Transaction transaction = null;
			try {
				// Upload berjalan lintas banyak baris dan dapat melampaui lifecycle request.
				// Pakai session terdedikasi per baris; jangan mewarisi currentSession yang
				// mungkin telah ditutup oleh request/baris sebelumnya.
				session = HibernateUtil.getSessionFactory().openSession();

				if (Common.getSheetContentAsString(sheet, 1, i) == null) {
					break;
				}
				kodeMk = Common.getSheetContentAsString(sheet, 1, i);
				Matakuliah matakuliah = (Matakuliah) Common.getSheetContentAsObject(sheet, 1, i, Matakuliah.class,
						Restrictions.eq("jurusan", kurikulum.getJurusan()));

				Integer semester = Common.getSheetContentAsInteger(sheet, 2, i);
				Integer tahap = Common.getSheetContentAsInteger(sheet, 3, i);
				Integer jumlahPertemuanPerkuliahanDefault = Common.getSheetContentAsInteger(sheet, 4, i);
				KurikulumPunyaMatakuliah indukMatakuliah = (KurikulumPunyaMatakuliah) Common
						.getSheetContentAsObject(sheet, 5, i, KurikulumPunyaMatakuliah.class);

				if (matakuliah != null && semester != null) {
					Long id = Common.getSheetContentAsLong(sheet, 0, i);
					KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = id == null || id.equals(-1L) ? null
							: (KurikulumPunyaMatakuliah) session.createCriteria(KurikulumPunyaMatakuliah.class)
									.add(Restrictions.idEq(id)).uniqueResult();

					if (kurikulumPunyaMatakuliah == null) {
						kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session
								.createCriteria(KurikulumPunyaMatakuliah.class)
								.add(Restrictions.eq("matakuliah", matakuliah)).setMaxResults(1).uniqueResult();
					}

					if (kurikulumPunyaMatakuliah == null) {
						kurikulumPunyaMatakuliah = new KurikulumPunyaMatakuliah();
					}

					kurikulumPunyaMatakuliah.setMatakuliah(matakuliah);
					kurikulumPunyaMatakuliah.setSemester(semester);
					kurikulumPunyaMatakuliah.setKurikulum(kurikulum);
					kurikulumPunyaMatakuliah.setTahap(tahap);
					kurikulumPunyaMatakuliah.setJumlahPertemuanPerkuliahanDefault(jumlahPertemuanPerkuliahanDefault);
					kurikulumPunyaMatakuliah.setIndukMatakuliah(indukMatakuliah);

					transaction = session.beginTransaction();
					session.saveOrUpdate(kurikulumPunyaMatakuliah);
					transaction.commit();

					report.sukses(i, kodeMk, "Semester " + semester);
				} else {
					report.gagal(i, kodeMk,
							"Mata kuliah tidak ditemukan atau kolom semester kosong",
							"Periksa kode mata kuliah (kolom B) dan isi semester (kolom C)");
				}

			} catch (Exception e) {
				if (transaction != null && transaction.isActive()) {
					try { transaction.rollback(); } catch (Exception ignored) { }
				}
				report.gagal(i, kodeMk, e, "Periksa data pada baris ini lalu ulangi upload");
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					try { session.clear(); } catch (Exception ignored) { }
					try { session.disconnect(); } catch (Exception ignored) { }
					try { session.close(); } catch (Exception ignored) { }
				}
			}

		}

		java.io.File reportFile = report.simpanLaporan();
		Filedownload.save(reportFile, "text/plain");
		MyMessageboxConfig.show(
				"Upload data kurikulum selesai.\n" + report.getRingkasan(),
				"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
	}

}
