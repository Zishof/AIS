package ais.action.master.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.MainAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.classroom.ClassRoomUtil;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GrupPertemuan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaGrupPertemuan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class AktifitasGrupPertemuanHelper {

	protected PenjadwalanGrupPertemuanHelper penjadwalanHelper;

	private Mahasiswa userMahasiswa = null;

	private Textbox nama;

	private String dataNama = "";

	public AktifitasGrupPertemuanHelper() {
		userMahasiswa = Common.getCurrentUser().getMahasiswa();
	}

	public Toolbar initAgendaGrupPertemuan(final GrupPertemuan grupPertemuan, final DataLoader dataLoader) {

		Toolbar hbox = new Toolbar();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Catatan Konsultasi", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				penjadwalanHelper.display(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
					}
				});
			}

		});

		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(grupPertemuan, true);
			}

		});
		button.setParent(hbox);

		hbox.appendChild(AbsensiGrupPertemuanHelper.createTombolAbsen(grupPertemuan, dataLoader));

		AktifitasPerkuliahanHelper.tampilCalender(hbox, dataLoader, grupPertemuan);

		ClassRoomUtil.createButton(grupPertemuan, dataLoader).setParent(hbox);
		RecoveryPertemuanHelper.button(grupPertemuan, new EventListener() {
			
			@Override
			public void onEvent(Event arg0) throws Exception {
				grupPertemuan.belum();
				dataLoader.loadData(null);
			}
		}).setParent(hbox);
		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(hbox);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				grupPertemuan.belum();
				dataLoader.loadData(null);
			}
		});

		nama = new Textbox(dataNama);
		nama.setCols(10);
		nama.setParent(hbox);
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dataLoader.loadData(null);
			}
		});

		cari = new MyToolbarbuttonConfig("", "/img/search.png");
		cari.setParent(hbox);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dataLoader.loadData(null);
			}
		});

		return hbox;
	}

	public void initDetail(final GrupPertemuan grupPertemuan, final Div groupbox) throws Exception {
		initDetail(grupPertemuan, null, groupbox);
	}

	private List<PertemuanPunyaGrupPertemuan> pertemuanPunyaGrupPertemuans = null;

	@SuppressWarnings("unchecked")
	public void initDetail(final GrupPertemuan grupPertemuan, final DataLoader mydataLoader, final Div groupbox)
			throws Exception {
		penjadwalanHelper = new PenjadwalanGrupPertemuanHelper(grupPertemuan);
		final DataLoader dataLoader = mydataLoader == null ? new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(grupPertemuan, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} : mydataLoader;

		groupbox.setStyle("border: none;");
		Common.clear(groupbox);

		final MyButtonTabbox tabbox = MyButtonTabbox.buat(groupbox, "100%", new int[] { 0 });
		Div tabpanel = tabbox.tambahTab(0, "Agenda Konsultasi");
		final Div tabpanelCetak = tabbox.tambahTab(1, "Cetak Agenda Konsultasi");
		tabpanelCetak.setStyle("min-height:25000px;overflow:auto;");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null) {
			Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
			if (desktopHeight != null) {
				tabpanelCetak.setStyle("min-height:" + (desktopHeight * 0.9) + "px;overflow:auto;");
			}
		}

		tabbox.onSetiapPilih(1, new EventListener() {

			@SuppressWarnings({ "rawtypes" })
			private Map parameters = null;
			private Toolbar toolbar;

			@SuppressWarnings("rawtypes")
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelCetak.getChildren().isEmpty()) {

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(tabpanelCetak);
					borderlayout.setWidth("100%");
					borderlayout.setHeight("2000px");

					North north = new North();
					north.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(north, true);

					north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

						@SuppressWarnings({})
						@Override
						public Map<String, Serializable> generateParameters() throws Exception {
							return parameters;
						}
					}, "lembar_konsultasi", null, null));

					final Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					final PDFMergerUtility ut = new PDFMergerUtility();
					final File filePdfBaru = new File(
							Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							try {
								ut.setDestinationStream(new FileOutputStream(filePdfBaru));
								ut.mergeDocuments();
								CommonReport.tampilkanReportPDF(center, filePdfBaru);
							} catch (Exception eMerge) {
								ais.common.ErrorAuditUtil.record(eMerge, "auto-audit src/ais/action/master/helper/AktifitasGrupPertemuanHelper.java:mergeDocuments");
								if (Common.getApakahAdmin()) {
									String msgJs = ("Error cetak agenda: " + (eMerge.getMessage() != null ? eMerge.getMessage() : eMerge.getClass().getSimpleName()))
											.replace("\\", "\\\\").replace("'", "\\'")
											.replace("\r\n", " ").replace("\r", " ").replace("\n", " ");
									org.zkoss.zk.ui.util.Clients.evalJavaScript("tampilkanToast('" + msgJs + "', 'error');");
								}
							}
						}
					});

					// Server push wajib agar label.setValue() dari Thread terdeteksi Timer
					final org.zkoss.zk.ui.Desktop threadDesktop = org.zkoss.zk.ui.Executions.getCurrent().getDesktop();
					if (threadDesktop != null && !threadDesktop.isServerPushEnabled()) {
						threadDesktop.enableServerPush(true);
					}

					new Thread(new Runnable() {

						@SuppressWarnings("rawtypes")
						private void setLabel(String nilai) {
							if (threadDesktop == null) {
								label.setValue(nilai);
								return;
							}
							try {
								org.zkoss.zk.ui.Executions.activate(threadDesktop);
								try {
									label.setValue(nilai);
								} finally {
									org.zkoss.zk.ui.Executions.deactivate(threadDesktop);
								}
							} catch (Exception eAct) {
								ais.common.ErrorAuditUtil.record(eAct, "auto-audit(empty-catch) AktifitasGrupPertemuanHelper:setLabel");
								label.setValue(nilai);
							}
						}

						@SuppressWarnings("rawtypes")
						@Override
						public void run() {
							int index = 0;
							int size = pertemuanPunyaGrupPertemuans.size();

							for (PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan : pertemuanPunyaGrupPertemuans) {
								Mahasiswa mahasiswa = pertemuanPunyaGrupPertemuan.getMahasiswa();
								index++;
								setLabel("Memperoses data " + mahasiswa.getNama() + " ("
										+ Common.numberFormat.get().format(((index * 1.0) / (size * 1.0)) * 100.0) + "%)");
								try {
									KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
									parameters = ais.common.HashMapGenerator.getRand();
									Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs", 1,
											"mahasiswa");
									Common.insertProperty(Mahasiswa.class, krsMahasiswa.getMahasiswa(), parameters, "",
											1);
									parameters.put("perkuliahan", krsMahasiswa == null || krsMahasiswa.getId() == null ? -1L : krsMahasiswa.getId());
									parameters.put("kelas", krsMahasiswa.getKelas());

									// FIX: null guard untuk jurusan (sebelumnya NPE jika jurusan null)
									Fakultas fakMhs = (krsMahasiswa.getMahasiswa() == null
											|| krsMahasiswa.getMahasiswa().getJurusan() == null)
													? null
													: krsMahasiswa.getMahasiswa().getJurusan().getFakultas();
									Common.insertProperty(Fakultas.class, fakMhs, parameters, "fak");

									parameters.put("nidn_kaprodi", krsMahasiswa.getMahasiswa() == null
											|| krsMahasiswa.getMahasiswa().getJurusan() == null
											|| krsMahasiswa.getMahasiswa().getJurusan().getKaprodi() == null ? ""
													: krsMahasiswa.getMahasiswa().getJurusan().getKaprodi().getNidn());
									parameters.put("nama_kaprodi", krsMahasiswa.getMahasiswa() == null
											|| krsMahasiswa.getMahasiswa().getJurusan() == null
											|| krsMahasiswa.getMahasiswa().getJurusan().getKaprodi() == null ? ""
													: krsMahasiswa.getMahasiswa().getJurusan().getKaprodi().getNama());

									List<Map> maps = new ArrayList<Map>();
									for (Pertemuan pertemuan : pertemuanPunyaGrupPertemuan.ambilPertemuanList(true)) {
										Map map = new HashMap();
										Common.insertProperty(Pertemuan.class, pertemuan, map, "", 1);

										map.put("pertemuan_ke", pertemuan.getPertemuanKe());
										map.put("tanggal", pertemuan.getTanggal());
										map.put("waktu",
												pertemuan.getWaktuMulai() + " sd " + pertemuan.getWaktuSelesai());
										map.put("materi", pertemuan.getTopik());
										map.put("catatan", pertemuan.getCatatan());
										map.put("metode", pertemuan.getMetodePembelajaran());
										map.put("paraf", "");

										map.put("jenis_semester",
												krsMahasiswa.getSemester() % 2 == 0 ? Perkuliahan.GENAP
														: Perkuliahan.GANJIL);

										map.put("nama_dosen", krsMahasiswa.getDosenPa() == null ? ""
												: krsMahasiswa.getDosenPa().getNama());
										map.put("code_dosen", krsMahasiswa.getDosenPa() == null ? ""
												: krsMahasiswa.getDosenPa().getCode());

										map.put("mahasiswa", krsMahasiswa.getMahasiswa() == null ? ""
												: krsMahasiswa.getMahasiswa().getNama());

										map.put("buku_rujukan",
												pertemuan.getBukuRujukan1() + " " + pertemuan.getBukuRujukan2());

										map.put("nama_jurusan", (krsMahasiswa.getMahasiswa() == null
												|| krsMahasiswa.getMahasiswa().getJurusan() == null) ? ""
												: krsMahasiswa.getMahasiswa().getJurusan().getNama());
										map.put("fakultas", (krsMahasiswa.getMahasiswa() == null
												|| krsMahasiswa.getMahasiswa().getJurusan() == null
												|| krsMahasiswa.getMahasiswa().getJurusan().getFakultas() == null) ? ""
												: krsMahasiswa.getMahasiswa().getJurusan().getFakultas().getNama());
										map.put("tahun_ajaran", krsMahasiswa.getTahunAkademik());
										map.put("kelas", krsMahasiswa.getKelas());
										map.put("nip_kaprodi",
												krsMahasiswa.getMahasiswa() == null
														|| krsMahasiswa.getMahasiswa().getJurusan() == null
														|| krsMahasiswa.getMahasiswa().getJurusan().getKaprodi() == null
																? ""
																: krsMahasiswa.getMahasiswa().getJurusan().getKaprodi()
																		.getCode());
										map.put("nidn_kaprodi",
												krsMahasiswa.getMahasiswa() == null
														|| krsMahasiswa.getMahasiswa().getJurusan() == null
														|| krsMahasiswa.getMahasiswa().getJurusan().getKaprodi() == null
																? ""
																: krsMahasiswa.getMahasiswa().getJurusan().getKaprodi()
																		.getNidn());
										map.put("nama_kaprodi",
												krsMahasiswa.getMahasiswa() == null
														|| krsMahasiswa.getMahasiswa().getJurusan() == null
														|| krsMahasiswa.getMahasiswa().getJurusan().getKaprodi() == null
																? ""
																: krsMahasiswa.getMahasiswa().getJurusan().getKaprodi()
																		.getNama());

										maps.add(map);
									}
									parameters.put("maps", maps);

									File file = Report.generateFileReport(Report.PDF, parameters, "lembar_konsultasi",
											ais.ui.util.WaktuUtil.getDate(), toolbar);
									ut.addSource(file);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasGrupPertemuanHelper.java:366");
								}
							}

							setLabel("");
						}
					}).start();

				}
			}
		});

		Session session = HibernateUtil.currentSession();

		pertemuanPunyaGrupPertemuans = penjadwalanHelper.initCriteria(dataNama = (nama == null ? "" : nama.getValue()))
				.list();
		for (PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan : pertemuanPunyaGrupPertemuans) {
			Pertemuan pertemuan = pertemuanPunyaGrupPertemuan.getPertemuan();
			if (!pertemuan.getPertemuanKe().equals(grupPertemuan.getPertemuanKe())) {
				pertemuan.setPertemuanKe(grupPertemuan.getPertemuanKe());
				session.update(pertemuan);
			}
		}

		ais.ui.util.MyDiv myGroupbox = new ais.ui.util.MyDiv();
		myGroupbox.setStyle("min-height: 500px;");
		myGroupbox.setParent(tabpanel);
		myGroupbox.appendChild(initAgendaGrupPertemuan(grupPertemuan, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(grupPertemuan, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		}));

		int size = pertemuanPunyaGrupPertemuans.size();
		if (size > 10) {
			size = 10;
		}
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(myGroupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("min-height: 500px;");
//		grid.setHeight("10000px");
		grid.setStyle("overflow:hidden");
		grid.setSclass("fgrid");
		// Lantai 2000px: bila belum ada agenda (size=0), tinggi jangan 0px (kolaps → tab kosong).
		grid.setStyle("height: " + Math.max(size * 300, 2000) + "px;");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setSclass("fgrid");
//		grid.setOddRowSclass("non-odd");

		tabpanel.setStyle("min-height: " + Math.max(size * 300, 2000) + "px;");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Mahasiswa");
		column.setWidth("25%");
		column.setParent(columns);

		column = new MyColumnConfig("Materi Konsultasi");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);
		boolean urut = false;
		try {
			String pil = tbmuser.retreive("urutkan_diskusi_berdasarkan_terlama");
			urut = (pil == null || pil.trim().isEmpty() ? false : Boolean.parseBoolean(pil));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasGrupPertemuanHelper.java:460");
			// TODO: handle exception
		}

		boolean mobile = Common.isMobile();
		for (PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan : pertemuanPunyaGrupPertemuans) {
			Pertemuan pertemuan = pertemuanPunyaGrupPertemuan.getPertemuan();

			if (pertemuan.getPertemuanPunyaGrupPertemuan() == null) {
				pertemuan.setPertemuanPunyaGrupPertemuan(pertemuanPunyaGrupPertemuan);
				Common.refreshUpdate(pertemuan);
			}

			Mahasiswa mahasiswa = pertemuanPunyaGrupPertemuan.getMahasiswa();
			final Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			row.setValign("top");

			Hbox vb = new Hbox();
			vb.setParent(row);
			vb.appendChild(CommonMedia.tampilkanGambarKecil(mahasiswa));
			Vbox a = RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan, mahasiswa.getNim());

			a.appendChild(new Label(mahasiswa.getNama()));
			vb.appendChild(a);

			Vbox vbox = new Vbox();
			vbox.setParent(row);

			DashboardTimelinePertemuan.displayCatatan(vbox, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dataLoader.loadData(null);
				}
			}, pertemuan, tbmuser, mobile);

			Component aa = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, null, true, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dataLoader.loadData(null);
				}
			});

			AktifitasPerkuliahanHelper.createKeterangan(pertemuan, userMahasiswa, null, dataLoader, aa,
					DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuan)).setParent(vbox);

			pertemuan.masukkanData("akses");
			if (Common.bolehKonfigurasi("komentar_tampil_di_halaman_utama_elearning")) {
				Vbox vbox2 = new Vbox();
				vbox2.setParent(vbox);
				if (!pertemuan.udah()) {
					pertemuan.reInitPertemuanPunyaDiskusi(session);
				}

				TreeSet<Long> pertemuanPunyaDiskusisa = pertemuan.ambilPertemuanPunyaDiskusiTotal(urut);
				DashboardTimelinePertemuan.loadKomentarDetail(null, "30px", pertemuanPunyaDiskusisa, pertemuan, vbox2,
						row.getStyle(), 0, 10, false, null);
			}
		}

	}

}
