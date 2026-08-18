package ais.action.master.helper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.LogLoginAction;
import ais.action.master.SkripsiAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.classroom.ClassRoomUtil;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class AktifitasSkripsiHelper {

	protected PenjadwalanSkripsiHelper penjadwalanHelper = new PenjadwalanSkripsiHelper();

	private Mahasiswa userMahasiswa = null;

	public AktifitasSkripsiHelper() {
		try {
			Tbmuser tbmuser = Common.getCurrentUser();
			userMahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasSkripsiHelper.java:69");
			// TODO: handle exception
		}
	}

	public Toolbar initAgendaSkripsi(final Skripsi skripsi, final DataLoader dataLoader) {

		Toolbar hbox = new Toolbar();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Agenda Sidang dan Revisi", "/img/jadwal.png");
		button.setVisible(skripsi.getSetujuiSidang());
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				penjadwalanHelper.display(skripsi, dataLoader);
			}

		});

		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(skripsi, true);
			}

		});
		button.setParent(hbox);

		// PenjadwalanHelper.tampilTombol(hbox, null, null, null, null,
		// skripsi);

		if (skripsi.getSetujuiSidang()) {
			PenjadwalanHelper.tampilTombolAmbil(hbox, null, null, null, null, skripsi, null, null, dataLoader);

			DspaceHelper.tampilkanButtonExportDiPertemuan(hbox, null, null, null, null, skripsi, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dataLoader.loadData(arg0);
					LogLoginAction.tampilDpsaceLog();
				}
			});

			AktifitasPerkuliahanHelper.tampilCalender(hbox, dataLoader, skripsi);

			ClassRoomUtil.createButton(skripsi, dataLoader).setParent(hbox);
		}
		RecoveryPertemuanHelper.button(skripsi, new EventListener() {
			
			@Override
			public void onEvent(Event arg0) throws Exception {
				skripsi.belum();
				dataLoader.loadData(null);
			}
		}).setParent(hbox);
		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(hbox);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				skripsi.belum();
				dataLoader.loadData(null);
			}
		});

		return hbox;
	}

	public void initDetail(final Skripsi skripsi, final MyDiv groupbox) throws Exception {
		initDetail(skripsi, null, groupbox);
	}

	public void initDetail(final Skripsi skripsi, final DataLoader mydataLoader, final MyDiv groupbox)
			throws Exception {

		final DataLoader dataLoader = mydataLoader == null ? new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(skripsi, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} : mydataLoader;

		groupbox.setStyle("border:none;height:72vh;min-height:520px;overflow-y:auto;overflow-x:hidden;");
		Common.clear(groupbox);

		final Tabbox tabbox = new Tabbox();
		tabbox.setSclass("ais-aktifitas-tabbox");
		tabbox.setParent(groupbox);
		tabbox.setWidth("100%");
		tabbox.setHeight("3000px");
		tabbox.setStyle("min-height:3000px;overflow-y:auto;overflow-x:hidden;");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab = new MyTabConfig("Agenda Revisi Skripsi atau Tugas Akhir");
		tab.setParent(tabs);
		tab.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					initDetail(skripsi, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		// FIX konten tab kosong: klik tab hanya memicu onSelect Tabbox, sedangkan mount konten
		// dipasang pada onClick MASING-MASING Tab -> kadang tak ter-trigger sehingga panel kosong.
		// Pasang onSelect di Tabbox yang me-RE-DISPATCH onClick ke tab terpilih (mount andal &
		// idempoten karena tiap handler cek getChildren().size()==0). Tab pertama (Agenda) DIKECUALIKAN
		// karena onClick-nya membangun-ulang seluruh tabbox (mencegah loop tak berujung).
		final org.zkoss.zul.Tab tabPertamaAgenda = tab;
		tabbox.addEventListener(org.zkoss.zk.ui.event.Events.ON_SELECT, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				org.zkoss.zul.Tab terpilih = tabbox.getSelectedTab();
				if (terpilih != null && terpilih != tabPertamaAgenda) {
					org.zkoss.zk.ui.event.Events.sendEvent(new Event("onClick", terpilih));
				}
			}
		});

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);
		tabpanels.setHeight("3000px");
		tabpanels.setStyle("min-height:3000px;overflow-y:auto;overflow-x:hidden;");

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.setStyle("min-height: 500px;");

		ais.ui.util.MyDiv myGroupbox = new ais.ui.util.MyDiv();
		myGroupbox.setStyle("min-height: 500px;");
		myGroupbox.setParent(tabpanel);
		myGroupbox.appendChild(initAgendaSkripsi(skripsi, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(skripsi, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		}));

		TreeMap<String, Long> pertemuans = skripsi.ambilPertemuan();
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && pertemuans.isEmpty() && skripsi.getTanggalSidang() != null) {

			Pertemuan pertemuan = new Pertemuan();

			pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
			pertemuan.setTanggal(skripsi.getTanggalSidang() == null ? WaktuUtil.getDate() : skripsi.getTanggalSidang());
			pertemuan.setSkripsi(skripsi);
			pertemuan.setTopik("Sidang atau persiapan sidang dengan judul \"" + skripsi.getJudul() + "\"");
			pertemuan.setWaktuMulai(
					skripsi.getTanggalSidang() == null ? "" : Common.timeFormat2.get().format(skripsi.getTanggalSidang()));
			pertemuan.setWaktuSelesai(
					skripsi.getTanggalSidang() == null ? "" : Common.timeFormat2.get().format(skripsi.getTanggalSidang()));

			pertemuan.setSkripsi(skripsi);

			try {
				Session session = HibernateUtil.currentSession();
				session.save(pertemuan);
				session.flush();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasSkripsiHelper.java:252");
			}
			pertemuans.put(Common.dateFormat8.get().format(pertemuan.getTanggal()) + "_" + pertemuan.getId(),
					pertemuan.getId());

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					skripsi.belum();
					initDetail(skripsi, dataLoader, groupbox);
				}
			});

			return;
		}
		myGroupbox.setStyle("height:2000px;");

		MyGrid grid = new MyGrid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(myGroupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("height: 2000px;");
		grid.setSclass("fgrid");
		grid.setMold("paging");
		grid.setPageSize(1);
		grid.getPagingChild().setMold("os");
		grid.setPagingPosition("top");

		Rows rows = new Rows();
		rows.setParent(grid);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
		calendar1.set(Calendar.DATE, calendar1.get(Calendar.DATE) + 6);

		boolean urut = false;
		try {
			String pil = tbmuser.retreive("urutkan_diskusi_berdasarkan_terlama");
			urut = (pil == null || pil.trim().isEmpty() ? false : Boolean.parseBoolean(pil));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasSkripsiHelper.java:295");
			// TODO: handle exception
		}

		int selected = 0;
		Date sekarang = WaktuUtil.getDate();

		if (skripsi.getSetujuiSidang()) {
			boolean mobile = Common.isMobile();
			for (Long pertemuanid : pertemuans.values()) {
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null) {
					final Row rowUtama = new Row();
					rowUtama.setParent(rows);
					rowUtama.setValign("top");
					if (pertemuan.getTanggal() != null && pertemuan.getTanggal().before(sekarang)) {
						selected++;
					}
					String tgl = pertemuan.getTanggal() == null ? "-"
							: Common.dateFormat11.get().format(pertemuan.getTanggal()) + " "
									+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
											: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai());

					Groupbox pertemuanBox = new ais.ui.util.MyGroupboxStyled();
					pertemuanBox.setWidth(mobile ? "93%" : "95%");
					rowUtama.appendChild(pertemuanBox);
					MyCaptionStyled c;
					pertemuanBox.appendChild(
							c = new MyCaptionStyled("Pertemuan ke-" + pertemuan.getPertemuanKe() + ", " + tgl));
					c.setStyle("font-size:12px;font-weight: bolder;text-decoration: none;color:"
							+ pertemuan.warna().split(",")[0] + ";border: 1px solid " + pertemuan.warna().split(",")[0]
							+ ";\r\n" + "  padding: 5px;" + "  background-color: rgba(169,169,169,0.4);"
							+ "  border-radius: 5px 15px;");

					Vbox a = RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan,
							pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama());

					Vbox vbox = new Vbox();
					vbox.setParent(pertemuanBox);
					a.setParent(vbox);
					new Label(pertemuan.getTopik()).setParent(vbox);

					DashboardTimelinePertemuan.displayCatatan(vbox, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(null);
						}
					}, pertemuan, tbmuser, mobile);

					Component buttonVicon = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, null, false,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									dataLoader.loadData(null);
								}
							});
					Component buttonAbsen = AbsensiHelper.createTombolAbsen(pertemuan, true, dataLoader);

					AktifitasPerkuliahanHelper.createKeterangan(pertemuan, userMahasiswa, null, dataLoader, buttonVicon,
							buttonAbsen, DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuan)).setParent(vbox);

					if (pertemuan.getSkripsi() != null) {
						List<CommonVO> dataDosen = pertemuan.getSkripsi().dataDosen(true);
						AbsensiHelper.createStatusKehadiranData(dataDosen, pertemuan).setParent(pertemuanBox);
					}

					DashboardTimelinePertemuan.tampilOnline(pertemuan, pertemuanBox, tbmuser, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(null);
						}
					});

					pertemuan.masukkanData("akses");
					if (Common.bolehKonfigurasi("komentar_tampil_di_halaman_utama_elearning")) {
						Vbox vbox2 = new Vbox();
						vbox2.setParent(pertemuanBox);
						if (!pertemuan.udah()) {
							Session session = HibernateUtil.currentSession();
							pertemuan.reInitPertemuanPunyaDiskusi(session);
						}
						TreeSet<Long> pertemuanPunyaDiskusisa = pertemuan.ambilPertemuanPunyaDiskusiTotal(urut);
						DashboardTimelinePertemuan.loadKomentarDetail(null, "42px", pertemuanPunyaDiskusisa, pertemuan,
								vbox2, "background-color: rgba(255,255,255,0.5);", 0, 10, false, null);
					}
				}
			}

		}

		try {
			grid.getPagingChild().setActivePage(selected);
		} catch (Exception e) {
			try {
				grid.getPagingChild().setActivePage(selected - 1);
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasSkripsiHelper.java:393");
				// TODO: handle exception
			}
		}

		final MyTabConfig tabReferensi = new MyTabConfig("Referensi");
		tabReferensi.setParent(tabs);

		final Tabpanel tabpanelReferensi = new ais.ui.util.MyTabpanel();
		setPanelDetailTinggi(tabpanelReferensi);
		tabpanelReferensi.setParent(tabpanels);
		addTabLoadListener(tabReferensi, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelReferensi.getChildren().size() == 0) {

					final Tabbox tabbox = new Tabbox();
					tabbox.setSclass("ais-aktifitas-tabbox");
					tabbox.setParent(tabpanelReferensi);
					tabbox.setWidth("100%");
					tabbox.setHeight("100%");

					Tabs tabs = new Tabs();
					tabs.setParent(tabbox);

					final MyTabConfig tabReferensiDiajukan = new MyTabConfig("Referensi");
					tabReferensiDiajukan.setParent(tabs);

					final MyTabConfig tabReferensi = new MyTabConfig("Ref. Buku");
					tabReferensi.setParent(tabs);

					final MyTabConfig tabBukuAjar = new MyTabConfig("Ref. Bahan Ajar");
					tabBukuAjar.setParent(tabs);

					final MyTabConfig tabArtikel = new MyTabConfig("Ref. Artikel");
					tabArtikel.setParent(tabs);

					Tabpanels tabpanels = new Tabpanels();
					tabpanels.setParent(tabbox);

					Tabpanel tabpanelReferensiDiajukan = new ais.ui.util.MyTabpanel();
					setPanelDetailTinggi(tabpanelReferensiDiajukan);
					tabpanelReferensiDiajukan.setParent(tabpanels);

					Grid gridref = SkripsiAction.initReferensi(skripsi, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					});
					gridref.setParent(Common.tampilanScroll(tabpanelReferensiDiajukan));

					Tabpanel tabpanelReferensi = new ais.ui.util.MyTabpanel();
					setPanelDetailTinggi(tabpanelReferensi);
					tabpanelReferensi.setParent(tabpanels);

					DataPunyaItemHelper dataPunyaItemHelper = new DataPunyaItemHelper();
					dataPunyaItemHelper.display(skripsi, null, null, null, null, tabpanelReferensi);

					final Tabpanel tabpanelBukuAjar = new ais.ui.util.MyTabpanel();
					tabpanelBukuAjar.setParent(tabpanels);
					setPanelDetailTinggi(tabpanelBukuAjar);
					addTabLoadListener(tabBukuAjar, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelBukuAjar.getChildren().size() == 0) {

								DataPunyaBukuAjarHelper dataPunyaBukuAjarHelper = new DataPunyaBukuAjarHelper();
								dataPunyaBukuAjarHelper.display(skripsi, null, null, null, null, tabpanelBukuAjar);
							}
						}
					});

					final Tabpanel tabpanelArtikel = new ais.ui.util.MyTabpanel();
					tabpanelArtikel.setParent(tabpanels);
					setPanelDetailTinggi(tabpanelArtikel);
					addTabLoadListener(tabArtikel, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelArtikel.getChildren().size() == 0) {

								DataPunyaArtikelHelper dataPunyaArtikelHelper = new DataPunyaArtikelHelper();
								dataPunyaArtikelHelper.display(skripsi, null, null, null, null, null, null,
										tabpanelArtikel);
							}
						}
					});
				}
			}
		});

		final MyTabConfig tabPenilaian = new MyTabConfig("Penilaian");
		tabPenilaian.setVisible(skripsi.getSetujuiSidang());
		tabPenilaian.setParent(tabs);

		final Tabpanel tabpanelPenilaian = new ais.ui.util.MyTabpanel();
		tabpanelPenilaian.setVisible(skripsi.getSetujuiSidang());
		tabpanelPenilaian.setParent(tabpanels);
		setPanelDetailTinggi(tabpanelPenilaian);
		addTabLoadListener(tabPenilaian, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPenilaian.getChildren().size() == 0) {

					PenilaianSkripsiHelper penilaianSkripsiHelper = new PenilaianSkripsiHelper();
					penilaianSkripsiHelper.display(skripsi, tabpanelPenilaian, null);
				}
			}
		});

		initCetak(tabbox, skripsi);

		final MyTabConfig tabPengajuanWisuda = new MyTabConfig("Pengajuan Wisuda");
		tabPengajuanWisuda.setVisible(skripsi.getSetujuiSidang());
		tabPengajuanWisuda.setParent(tabs);

		final Tabpanel tabpanelPengajuanWisuda = new ais.ui.util.MyTabpanel();
		tabpanelPengajuanWisuda.setVisible(skripsi.getSetujuiSidang());
		tabpanelPengajuanWisuda.setParent(tabpanels);
		setPanelDetailTinggi(tabpanelPengajuanWisuda);
		addTabLoadListener(tabPengajuanWisuda, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPengajuanWisuda.getChildren().size() == 0) {

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(tabpanelPengajuanWisuda);
					borderlayout.setStyle("min-height:360px");
					Center center = new Center();
					center.setParent(borderlayout);
					center.appendChild(new MyInclude("/pages/master/pendaftaran_wisuda_mahasiswa.zul?mahasiswa="
							+ skripsi.getMahasiswa().getId()));
				}
			}
		});

	}

	private Toolbar toolbar;

	public void initCetak(Tabbox tabbox, final Skripsi skripsi) {
		final MyTabConfig tabMonitor = new MyTabConfig("Laporan");
		tabMonitor.setParent(tabbox.getTabs());
		final Tabpanel tabpanelMonitor = new ais.ui.util.MyTabpanel();
		setPanelDetailTinggi(tabpanelMonitor);
		tabpanelMonitor.setParent(tabbox.getTabpanels());
		addTabLoadListener(tabMonitor, new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(tabpanelMonitor);
				final Map parameters = ais.common.HashMapGenerator.getRand();

				parameters.put("perkuliahan", skripsi == null || skripsi.getId() == null ? -1L : skripsi.getId());

				parameters.put("awal", skripsi == null || skripsi.getAwalBimbingan() == null ? ""
						: Common.dateFormat2.get().format(skripsi.getAwalBimbingan()));

				parameters.put("akhir", skripsi == null || skripsi.getAkhirBimbingan() == null ? ""
						: Common.dateFormat2.get().format(skripsi.getAkhirBimbingan()));

				Common.insertProperty(Skripsi.class, skripsi, parameters, "", 2);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(tabpanelMonitor);
				borderlayout.setHeight("700px");

				final Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						File file = Report.generateFileReport(Report.PDF, parameters, "lembar_konsultasi_revisi",
								ais.ui.util.WaktuUtil.getDate(), toolbar);
						CommonReport.tampilkanReportPDF(center, file);
					}
				};

				North north = new North();
				north.setParent(borderlayout);
				north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

					@SuppressWarnings({})
					@Override
					public Map<String, Serializable> generateParameters() throws Exception {
						return parameters;
					}
				}, "lembar_konsultasi_revisi", null, eventListener));

				eventListener.onEvent(null);
			}
		});
	}

	private void addTabLoadListener(MyTabConfig tab, final EventListener listener) {
		if (tab == null || listener == null) {
			return;
		}
		final long[] lastRun = new long[] { 0L };
		EventListener delegatingListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				long now = System.currentTimeMillis();
				if (now - lastRun[0] < 120L) {
					return;
				}
				lastRun[0] = now;
				listener.onEvent(event);
			}
		};
		tab.addEventListener("onClick", delegatingListener);
		tab.addEventListener(org.zkoss.zk.ui.event.Events.ON_SELECT, delegatingListener);
	}

	private void setPanelDetailTinggi(Tabpanel tabpanel) {
		if (tabpanel == null) {
			return;
		}
		tabpanel.setHeight("3000px");
		tabpanel.setStyle("height:3000px;min-height:3000px;overflow-y:auto;overflow-x:hidden;");
	}

}
