package ais.action.master.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.LogLoginAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.akademik.LaporanMonitorPklKbm;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.classroom.ClassRoomUtil;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.pkl.KelompokPkl;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Helper composer ZK utama untuk tampilan "aktivitas" (gaya e-learning) satu {@link KelompokPkl}
 * (kelompok PKL/Praktik Kerja Lapangan): agenda pertemuan berbentuk timeline mirip
 * {@link AktifitasPerkuliahanHelper} tapi khusus PKL, ditambah tab anggota, tugas kelompok,
 * referensi (buku/bahan ajar/artikel), penilaian, dan laporan KBM.
 *
 * <p>
 * Hak edit ({@link #edit}) ditentukan dari peran user login: staf/admin non-mahasiswa-non-dosen,
 * admin, atau mahasiswa/dosen anggota kelompok yang diberi izin eksplisit lewat
 * {@code Pkl#getMahasiswaBolehMerubahAgenda()}/{@code getDosenBolehMerubahAgenda()}. Toolbar agenda
 * ({@link #initAgendaKelompokPkl}) menyediakan aksi jadwal (lewat {@link PenjadwalanPklHelper}),
 * cetak absensi, ambil jadwal, ekspor ke DSpace, integrasi kalender/Google Classroom, refresh, dan
 * riwayat perubahan pertemuan ({@link RevisiPertemuanPklHelper}) — hanya yang punya hak edit.
 * {@link #initDetail} membangun seluruh tabbox: bila kelompok belum punya pertemuan, satu pertemuan
 * pembuka otomatis dibuat; tab agenda menampilkan grid ber-paging satu-pertemuan-per-halaman dengan
 * halaman aktif otomatis mengarah ke pertemuan pertama yang sudah lewat.
 * </p>
 */
public class AktifitasPklHelper {

	/** Helper penjadwalan pertemuan PKL, dipakai tombol "Agenda PKL" pada {@link #initAgendaKelompokPkl}. */
	protected PenjadwalanPklHelper penjadwalanHelper = new PenjadwalanPklHelper();

	/** Data mahasiswa dari user login saat ini, atau {@code null} bila user bukan mahasiswa (atau belum login). */
	private Mahasiswa userMahasiswa = null;
	/** User login saat ini (hasil {@link Common#getCurrentUser()}), atau {@code null} bila belum login. */
	private Tbmuser tbmuser = null;

	/** Menentukan apakah user login boleh mengubah agenda/pertemuan kelompok PKL ini — dihitung ulang di {@link #initDetail}. */
	private boolean edit = false;

	/** Mengambil user login saat ini beserta data mahasiswa terkait (bila user adalah mahasiswa) sebagai konteks tampilan. */
	public AktifitasPklHelper() {
		tbmuser = Common.getCurrentUser();
		userMahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();

	}

	/**
	 * Membangun toolbar aksi agenda untuk {@code kelompokPkl}: tombol Agenda PKL (jadwal, hanya
	 * bila {@link #edit}), Absensi (cetak), Ambil jadwal/ekspor DSpace/kalender/Google Classroom
	 * (hanya bila {@link #edit}), Refresh, dan History revisi pertemuan (hanya bila {@link #edit}
	 * dan user bukan mahasiswa/siswa).
	 *
	 * @param kelompokPkl kelompok PKL target aksi
	 * @param dataLoader  callback muat-ulang tampilan pemanggil setelah suatu aksi selesai
	 * @return toolbar siap dipasang ke parent
	 */
	public Toolbar initAgendaKelompokPkl(final KelompokPkl kelompokPkl, final DataLoader dataLoader) {

		Toolbar hbox = new Toolbar();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Agenda PKL", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				penjadwalanHelper.display(kelompokPkl, dataLoader);
			}

		});
		button.setVisible(edit);
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(kelompokPkl, true);
			}

		});
		button.setParent(hbox);

		// PenjadwalanHelper.tampilTombol(hbox, null, null, kelompokPkl, null,
		// null);

		if (edit) {
			PenjadwalanHelper.tampilTombolAmbil(hbox, null, null, kelompokPkl, null, null, null, null, dataLoader);

			DspaceHelper.tampilkanButtonExportDiPertemuan(hbox, null, null, kelompokPkl, null, null,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(arg0);
							LogLoginAction.tampilDpsaceLog();
						}
					});

			AktifitasPerkuliahanHelper.tampilCalender(hbox, dataLoader, kelompokPkl);

			ClassRoomUtil.createButton(kelompokPkl, dataLoader).setParent(hbox);
		}

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(hbox);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kelompokPkl.belum();
				dataLoader.loadData(null);
			}
		});

		if (edit) {
			RecoveryPertemuanHelper.button(kelompokPkl, new EventListener() {
				
				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokPkl.belum();
					dataLoader.loadData(null);
				}
			}).setParent(hbox);
			button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
			button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					RevisiPertemuanPklHelper revisiHelper = new RevisiPertemuanPklHelper(kelompokPkl,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event arg0) throws Exception {
									kelompokPkl.belum();

									Session session = HibernateUtil.currentNativeSession();
									List<Pertemuan> pertemuansTemp = session.createCriteria(Pertemuan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.addOrder(!kelompokPkl.getUrutkanotomatis() ? Order.asc("pertemuanKe")
													: Order.asc("tanggal"))
											.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
											.add(Restrictions.eq("kelompokPkl", kelompokPkl)).list();
									// session.disconnect();
									if (session.isOpen()) {session.disconnect();session.close();}
									HibernateUtil.closeSession();

									session = HibernateUtil.currentSession();
									kelompokPkl.reInitPertemuan(pertemuansTemp, session);
									pertemuansTemp.clear();
									pertemuansTemp = null;

									kelompokPkl.belum();
									dataLoader.loadData(null);

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											kelompokPkl.belum();
											dataLoader.loadData(null);
										}
									});
								}
							});
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
					revisiHelper.setVisible(true);
					revisiHelper.onModal();

				}

			});
			button.setParent(hbox);
		}

		return hbox;
	}

	/** Seperti {@link #initDetail(KelompokPkl, DataLoader, MyDiv)} dengan {@code mydataLoader=null} (memakai callback muat-ulang default yang membangun ulang seluruh detail). */
	public void initDetail(final KelompokPkl kelompokPkl, final MyDiv groupbox) throws Exception {
		initDetail(kelompokPkl, null, groupbox);
	}

	/**
	 * Membangun seluruh tabbox detail aktivitas PKL kelompok di dalam {@code groupbox}: tab Agenda
	 * PKL (timeline pertemuan ber-paging, satu pertemuan per halaman, dengan komentar/absensi/video
	 * conference per pertemuan), Anggota (dimuat lewat {@link KelompokPklHelper}), Tugas Kelompok
	 * (lewat {@link TugasKelompokHelper}), Referensi (buku/bahan ajar/artikel, masing-masing dimuat
	 * malas saat tab dibuka), Penilaian (lewat {@link PenilaianPklHelper}), dan Laporan KBM (lewat
	 * {@link LaporanMonitorPklKbm}). Menghitung ulang hak edit ({@link #edit}) di awal method.
	 *
	 * <p>
	 * Bila {@code kelompokPkl} belum punya pertemuan sama sekali, satu pertemuan pembuka otomatis
	 * dibuat (topik "Topik pembukaan PKL untuk kelompok ...") sebelum tab dibangun, lalu method
	 * memanggil dirinya sendiri lewat timer dan langsung {@code return}.
	 * </p>
	 *
	 * @param kelompokPkl kelompok PKL yang detail aktivitasnya ditampilkan
	 * @param mydataLoader callback muat-ulang eksternal; bila {@code null}, dipakai callback default
	 *                     yang memanggil ulang {@code initDetail} untuk kelompok dan groupbox yang
	 *                     sama
	 * @param groupbox    komponen induk ZK; isinya dibersihkan lebih dulu
	 * @throws Exception diteruskan dari kegagalan pembangunan UI atau akses data
	 */
	public void initDetail(final KelompokPkl kelompokPkl, final DataLoader mydataLoader, final MyDiv groupbox)
			throws Exception {

		edit = (tbmuser != null && ((tbmuser.getMahasiswa() == null && tbmuser.ambilDosen() == null)
				|| (tbmuser.getMahasiswa() != null && kelompokPkl.getPkl() != null
						&& kelompokPkl.getPkl().getMahasiswaBolehMerubahAgenda())
				|| Common.getApakahAdmin() || (tbmuser.ambilDosen() != null && kelompokPkl.getPkl() != null
						&& kelompokPkl.getPkl().getDosenBolehMerubahAgenda())));

		final DataLoader dataLoader = mydataLoader == null ? new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(kelompokPkl, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} : mydataLoader;

		groupbox.setStyle("border: none;");
		Common.clear(groupbox);

		final Tabbox tabbox = new Tabbox();
		tabbox.setSclass("ais-aktifitas-tabbox");
		tabbox.setParent(groupbox);
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab = new MyTabConfig("Agenda PKL");
		tab.setParent(tabs);
		tab.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					initDetail(kelompokPkl, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		final MyTabConfig tabAnggota = new MyTabConfig("Anggota");
		tabAnggota.setParent(tabs);

		final MyTabConfig tabTugasKelompok = new MyTabConfig("Tugas Kelompok");
		tabTugasKelompok.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		ais.ui.util.MyDiv myGroupbox = new ais.ui.util.MyDiv();
		myGroupbox.setStyle("min-height: 500px;");
		myGroupbox.setParent(tabpanel);
		myGroupbox.appendChild(initAgendaKelompokPkl(kelompokPkl, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(kelompokPkl, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		}));

		TreeMap<String, Long> pertemuans = kelompokPkl.ambilPertemuan();
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && pertemuans.isEmpty()) {

			Pertemuan pertemuan = new Pertemuan();

			pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
			pertemuan.setTanggal(ais.ui.util.WaktuUtil.getDate());
			pertemuan.setKelompokPkl(kelompokPkl);
			pertemuan.setTopik("Topik pembukaan PKL untuk kelompok \"" + kelompokPkl.getNama() + "\"");
			pertemuan.setWaktuMulai(Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));
			pertemuan.setWaktuSelesai(Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));

			try {
				Session session = HibernateUtil.currentSession();
				session.save(pertemuan);
				session.flush();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasPklHelper.java:290");
			}

			pertemuans.put(Common.dateFormat8.get().format(pertemuan.getTanggal()) + "_" + pertemuan.getId(),
					pertemuan.getId());

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokPkl.belum();
					initDetail(kelompokPkl, dataLoader, groupbox);
				}
			});

			return;
		}
		myGroupbox.setStyle("height:6000px;");

		MyGrid grid = new MyGrid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(myGroupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("height: 6000px;");
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasPklHelper.java:334");
			// TODO: handle exception
		}
		boolean mobile = Common.isMobile();
		int selected = 0;
		Date sekarang = WaktuUtil.getDate();
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

				Component aa = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, null, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
					}
				});

				Component bb = AbsensiHelper.createTombolAbsen(pertemuan, true, dataLoader);

				AktifitasPerkuliahanHelper.createKeterangan(pertemuan, userMahasiswa, null, dataLoader, aa, bb,
						DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuan)).setParent(vbox);

				AbsensiHelper.createStatusKehadiran(kelompokPkl.populateDosenBuNama(), pertemuan)
						.setParent(pertemuanBox);

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

		try {
			grid.getPagingChild().setActivePage(selected);
		} catch (Exception e) {
			try {
				grid.getPagingChild().setActivePage(selected - 1);
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasPklHelper.java:420");
				// TODO: handle exception
			}
		}

		Session session = HibernateUtil.currentSession();
		final Tabpanel tabpanelAnggota = new ais.ui.util.MyTabpanel();
		int jumlahAnggota = ((Number) session.createCriteria(MahasiswaDapatKelompokPkl.class)
				.add(Restrictions.eq("diterima", true)).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kelompokPkl", kelompokPkl)).uniqueResult()).intValue();
		tabAnggota.setLabel("Anggota Kelompok " + (jumlahAnggota == 0 ? "" : "(" + jumlahAnggota + ")"));

		tabpanelAnggota.setParent(tabpanels);
		tabpanelAnggota.setHeight("1250px");
		tabAnggota.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelAnggota.getChildren().size() == 0) {
					KelompokPklHelper kelompokPklHelper = new KelompokPklHelper();
					kelompokPklHelper.display(kelompokPkl, tabpanelAnggota);
				}
			}
		});

		final Tabpanel tabpanelTugasKelompok = new ais.ui.util.MyTabpanel();
		int jumlahTugasKelompok = ((Number) session.createCriteria(TugasKelompok.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("kelompokPkl", kelompokPkl)).uniqueResult()).intValue();
		tabTugasKelompok
				.setLabel("Tugas Kelompok " + (jumlahTugasKelompok == 0 ? "" : "(" + jumlahTugasKelompok + ")"));

		tabpanelTugasKelompok.setParent(tabpanels);
		tabpanelTugasKelompok.setHeight("1250px");
		tabTugasKelompok.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelTugasKelompok.getChildren().size() == 0) {

					TugasKelompokHelper tugasKelompokHelper = new TugasKelompokHelper(userMahasiswa, null);
					tugasKelompokHelper.display(null, null, kelompokPkl, tabpanelTugasKelompok);
				}
			}
		});

		final MyTabConfig tabReferensi = new MyTabConfig("Referensi");
		tabReferensi.setParent(tabs);

		final Tabpanel tabpanelReferensi = new ais.ui.util.MyTabpanel();
		tabpanelReferensi.setHeight("1250px");
		tabpanelReferensi.setParent(tabpanels);
		tabReferensi.addEventListener("onClick", new EventListener() {

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

					final MyTabConfig tabReferensi = new MyTabConfig("Buku");
					tabReferensi.setParent(tabs);

					final MyTabConfig tabBukuAjar = new MyTabConfig("Bahan Ajar");
					tabBukuAjar.setParent(tabs);

					final MyTabConfig tabArtikel = new MyTabConfig("Artikel");
					tabArtikel.setParent(tabs);

					Tabpanels tabpanels = new Tabpanels();
					tabpanels.setParent(tabbox);

					Tabpanel tabpanelReferensi = new ais.ui.util.MyTabpanel();
					tabpanelReferensi.setHeight("1250px");
					tabpanelReferensi.setParent(tabpanels);

					DataPunyaItemHelper dataPunyaItemHelper = new DataPunyaItemHelper();
					dataPunyaItemHelper.display(null, null, null, null, kelompokPkl, tabpanelReferensi);

					final Tabpanel tabpanelBukuAjar = new ais.ui.util.MyTabpanel();
					tabpanelBukuAjar.setParent(tabpanels);
					tabpanelBukuAjar.setHeight("1250px");
					tabBukuAjar.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelBukuAjar.getChildren().size() == 0) {

								DataPunyaBukuAjarHelper dataPunyaBukuAjarHelper = new DataPunyaBukuAjarHelper();
								dataPunyaBukuAjarHelper.display(null, null, null, null, kelompokPkl, tabpanelBukuAjar);
							}
						}
					});

					final Tabpanel tabpanelArtikel = new ais.ui.util.MyTabpanel();
					tabpanelArtikel.setParent(tabpanels);
					tabpanelArtikel.setHeight("1250px");
					tabArtikel.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelArtikel.getChildren().size() == 0) {

								DataPunyaArtikelHelper dataPunyaArtikelHelper = new DataPunyaArtikelHelper();
								dataPunyaArtikelHelper.display(null, null, null, null, kelompokPkl, null, null,
										tabpanelArtikel);
							}
						}
					});
				}
			}
		});

		final MyTabConfig tabPenilaian = new MyTabConfig("Penilaian");
		tabPenilaian.setParent(tabs);

		final Tabpanel tabpanelPenilaian = new ais.ui.util.MyTabpanel();

		tabpanelPenilaian.setParent(tabpanels);
		tabpanelPenilaian.setHeight("1250px");
		tabPenilaian.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPenilaian.getChildren().size() == 0) {

					PenilaianPklHelper penilaianPklHelper = new PenilaianPklHelper();
					penilaianPklHelper.display(kelompokPkl, tabpanelPenilaian);
				}
			}
		});

		final MyTabConfig tabMonitorKbm = new MyTabConfig("Laporan KBM");
		tabMonitorKbm.setParent(tabs);

		final Tabpanel tabpanelMonitorKbm = new ais.ui.util.MyTabpanel();
		tabpanelMonitorKbm.setParent(tabpanels);
		tabMonitorKbm.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelMonitorKbm.getChildren().isEmpty()) {
					LaporanMonitorPklKbm laporanMonitorPklKbm = new LaporanMonitorPklKbm(kelompokPkl);
					laporanMonitorPklKbm.setBorder("none");
					laporanMonitorPklKbm.setHeight("650px");
					laporanMonitorPklKbm.setWidth("100%");
					tabpanelMonitorKbm.appendChild(laporanMonitorPklKbm);
					tabpanelMonitorKbm.setHeight("650px");
				}

			}
		});

	}

}
