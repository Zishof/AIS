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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.LogLoginAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.akademik.LaporanMonitorKknKbm;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.classroom.ClassRoomUtil;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.kkn.KelompokKkn;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Helper ZK yang membangun tampilan "Aktifitas KKN" — dasbor tab berisi seluruh aktivitas satu
 * {@link KelompokKkn} (kelompok Kuliah Kerja Nyata): agenda/timeline pertemuan, anggota, materi/
 * tugas/ujian, tugas kelompok, referensi (buku/bahan ajar/artikel), penilaian, dan laporan KBM.
 * Pola dan strukturnya mengikuti helper aktivitas serupa untuk perkuliahan biasa (lihat referensi
 * silang ke {@code AktifitasPerkuliahanHelper}), diadaptasikan untuk entitas KKN.
 *
 * <p>
 * {@link #initAgendaKelompokKkn} membangun toolbar aksi agenda: penjadwalan pertemuan baru
 * ({@link PenjadwalanKknHelper} lewat {@link PenjadwalanHelper#tampilTombolAmbil}), cetak laporan
 * absensi, ekspor ke DSpace ({@link DspaceHelper#tampilkanButtonExportDiPertemuan}), integrasi
 * kalender dan Google Classroom, tombol Refresh, pemulihan pertemuan terhapus
 * ({@code RecoveryPertemuanHelper}), dan riwayat/urut-ulang pertemuan ({@code
 * RevisiPertemuanKknHelper}) — sebagian besar tombol hanya tampil bila {@link #edit} bernilai
 * {@code true} (dosen/admin, atau mahasiswa/dosen yang diizinkan konfigurasi KKN terkait untuk
 * mengubah agenda).
 * </p>
 *
 * <p>
 * {@link #initDetail} adalah titik masuk utama: menyusun {@link org.zkoss.zul.Tabbox} dengan tab
 * "Agenda KKN" (grid berpaging satu-pertemuan-per-halaman berisi catatan diskusi, absensi, video
 * conference, dan daftar diskusi terkait — dipompa lewat kelas {@code DashboardTimelinePertemuan}
 * dan {@code AbsensiHelper}), serta tab-tab lain yang di-lazy-load saat pertama diklik: Anggota
 * ({@link KelompokKknHelper}), Materi/Tugas/Ujian ({@code TampilanELearningAction}), Tugas
 * Kelompok ({@code TugasKelompokHelper}), Referensi (sub-tab Buku/Bahan Ajar/Artikel via
 * {@link DataPunyaItemHelper} dan sejenisnya), Penilaian ({@link PenilaianKknHelper}), dan
 * Laporan KBM ({@code LaporanMonitorKknKbm}). Bila kelompok belum punya pertemuan sama sekali,
 * satu pertemuan pembukaan otomatis dibuat sebelum tampilan dibangun ulang.
 * </p>
 */
public class AktifitasKknHelper {

	protected PenjadwalanKknHelper penjadwalanHelper = new PenjadwalanKknHelper();

	private Mahasiswa userMahasiswa = null;
	private Tbmuser tbmuser = null;
	private boolean edit = false;

	/** Mengambil user login saat ini beserta data mahasiswa terkait (bila user adalah mahasiswa) sebagai konteks tampilan. */
	public AktifitasKknHelper() {
		tbmuser = Common.getCurrentUser();
		userMahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
	}

	/**
	 * Membangun toolbar aksi untuk tab Agenda KKN: Agenda KKN (buka penjadwalan), Absensi (cetak
	 * laporan), dan — hanya bila {@link #edit} true — tombol tambah pertemuan cepat, ekspor
	 * DSpace, kalender, Google Classroom, Refresh, pemulihan pertemuan, dan riwayat/History
	 * (khusus non-mahasiswa/siswa) yang membuka {@code RevisiPertemuanKknHelper} untuk mengurutkan
	 * ulang pertemuan berdasarkan tanggal dan menyegarkan cache kelompok.
	 *
	 * @param kelompokKkn kelompok KKN yang agendanya dikelola
	 * @param dataLoader  callback untuk memuat ulang tampilan setelah aksi selesai
	 * @return toolbar siap ditempel
	 */
	public Toolbar initAgendaKelompokKkn(final KelompokKkn kelompokKkn, final DataLoader dataLoader) {

		Toolbar hbox = new Toolbar();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Agenda KKN", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				penjadwalanHelper.display(kelompokKkn, dataLoader);
			}

		});
		button.setVisible(edit);
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(kelompokKkn, true);
			}

		});
		button.setParent(hbox);

		// PenjadwalanHelper.tampilTombol(hbox, null, kelompokKkn, null, null,
		// null);
		if (edit) {
			PenjadwalanHelper.tampilTombolAmbil(hbox, null, kelompokKkn, null, null, null, null, null, dataLoader);

			DspaceHelper.tampilkanButtonExportDiPertemuan(hbox, null, kelompokKkn, null, null, null,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(arg0);
							LogLoginAction.tampilDpsaceLog();
						}
					});

			AktifitasPerkuliahanHelper.tampilCalender(hbox, dataLoader, kelompokKkn);

			ClassRoomUtil.createButton(kelompokKkn, dataLoader).setParent(hbox);
		}

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(hbox);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kelompokKkn.belum();
				dataLoader.loadData(null);
			}
		});

		if (edit) {
			
			RecoveryPertemuanHelper.button(kelompokKkn, new EventListener() {
				
				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKkn.belum();
					dataLoader.loadData(null);
				}
			}).setParent(hbox);
			
			button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
			button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					RevisiPertemuanKknHelper revisiHelper = new RevisiPertemuanKknHelper(kelompokKkn,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event arg0) throws Exception {
									kelompokKkn.belum();

									Session session = HibernateUtil.currentNativeSession();
									List<Pertemuan> pertemuansTemp = session.createCriteria(Pertemuan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.addOrder(!kelompokKkn.getUrutkanotomatis() ? Order.asc("pertemuanKe")
													: Order.asc("tanggal"))
											.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
											.add(Restrictions.eq("kelompokKkn", kelompokKkn)).list();
									// session.disconnect();
									if (session.isOpen()) {session.disconnect();session.close();}
									HibernateUtil.closeSession();

									session = HibernateUtil.currentSession();
									kelompokKkn.reInitPertemuan(pertemuansTemp, session);
									pertemuansTemp.clear();
									pertemuansTemp = null;

									kelompokKkn.belum();
									dataLoader.loadData(null);

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											kelompokKkn.belum();
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

	/** Seperti {@link #initDetail(KelompokKkn, DataLoader, MyDiv)} dengan {@code dataLoader} default yang memuat ulang dirinya sendiri. */
	public void initDetail(final KelompokKkn kelompokKkn, final MyDiv groupbox) throws Exception {
		initDetail(kelompokKkn, null, groupbox);
	}

	/**
	 * Titik masuk utama: membangun seluruh dasbor tab Aktifitas KKN ke dalam {@code groupbox}.
	 * Menentukan hak edit ({@link #edit}) berdasarkan kombinasi user login, admin, dan pengaturan
	 * {@code mahasiswaBolehMerubahAgenda}/{@code dosenBolehMerubahAgenda} pada {@link
	 * ais.database.model.Kkn} induk kelompok. Bila kelompok belum memiliki pertemuan, satu
	 * pertemuan pembukaan otomatis dibuat lalu tampilan dijadwalkan untuk dibangun ulang lewat
	 * timer. Tab-tab selain Agenda KKN (Anggota, Materi/Tugas/Ujian, Tugas Kelompok, Referensi,
	 * Penilaian, Laporan KBM) baru dimuat isinya saat pertama kali diklik (lazy loading).
	 *
	 * @param kelompokKkn kelompok KKN yang ditampilkan
	 * @param mydataLoader loader kustom untuk memuat ulang tampilan; bila {@code null}, dipakai loader default yang memanggil {@link #initDetail(KelompokKkn, MyDiv)}
	 * @param groupbox    komponen induk (dibersihkan lebih dulu)
	 */
	public void initDetail(final KelompokKkn kelompokKkn, final DataLoader mydataLoader, final MyDiv groupbox)
			throws Exception {

		edit = (tbmuser != null && ((tbmuser.getMahasiswa() == null && tbmuser.ambilDosen() == null)
				|| (tbmuser.getMahasiswa() != null && kelompokKkn.getKkn() != null
						&& kelompokKkn.getKkn().getMahasiswaBolehMerubahAgenda())
				|| Common.getApakahAdmin() || (tbmuser.ambilDosen() != null && kelompokKkn.getKkn() != null
						&& kelompokKkn.getKkn().getDosenBolehMerubahAgenda())));

		final DataLoader dataLoader = mydataLoader == null ? new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(kelompokKkn, groupbox);
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

		MyTabConfig tab = new MyTabConfig("Agenda KKN");
		tab.setParent(tabs);
		tab.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					initDetail(kelompokKkn, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		final MyTabConfig tabAnggota = new MyTabConfig("Anggota");
		tabAnggota.setParent(tabs);

		final MyTabConfig tabMateri = new MyTabConfig("Materi,Tugas,Ujian");
		tabMateri.setParent(tabs);

		final MyTabConfig tabTugasKelompok = new MyTabConfig("Tugas Kelompok");
		tabTugasKelompok.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		ais.ui.util.MyDiv myGroupbox = new ais.ui.util.MyDiv();
		myGroupbox.setStyle("min-height: 500px;");
		myGroupbox.setParent(tabpanel);
		myGroupbox.appendChild(initAgendaKelompokKkn(kelompokKkn, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(kelompokKkn, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		}));

		TreeMap<String, Long> pertemuans = kelompokKkn.ambilPertemuan();
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && pertemuans.isEmpty()) {

			Pertemuan pertemuan = new Pertemuan();

			pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
			pertemuan.setTanggal(ais.ui.util.WaktuUtil.getDate());
			pertemuan.setKelompokKkn(kelompokKkn);
			pertemuan.setTopik("Topik pembukaan KKN untuk kelompok \"" + kelompokKkn.getNama() + "\"");
			pertemuan.setWaktuMulai(Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));
			pertemuan.setWaktuSelesai(Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));

			try {
				Session session = HibernateUtil.currentSession();
				session.save(pertemuan);
				session.flush();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasKknHelper.java:305");
			}
			pertemuans.put(Common.dateFormat8.get().format(pertemuan.getTanggal()) + "_" + pertemuan.getId(),
					pertemuan.getId());

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKkn.belum();
					initDetail(kelompokKkn, dataLoader, groupbox);
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasKknHelper.java:348");
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
						pertemuan.getTanggal() == null ? "-"
								: Common.dateFormat11.get().format(pertemuan.getTanggal()) + " "
										+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
												: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai()));

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

				AbsensiHelper.createStatusKehadiran(kelompokKkn.populateDosenBuNama(), pertemuan)
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
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasKknHelper.java:438");
				// TODO: handle exception
			}
		}

		Session session = HibernateUtil.currentSession();
		final Tabpanel tabpanelAnggota = new ais.ui.util.MyTabpanel();
		int jumlahAnggota = ((Number) session.createCriteria(MahasiswaDapatKelompokKkn.class)
				.add(Restrictions.eq("diterima", true)).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kelompokKkn", kelompokKkn)).uniqueResult()).intValue();
		tabAnggota.setLabel("Anggota Kelompok " + (jumlahAnggota == 0 ? "" : "(" + jumlahAnggota + ")"));

		tabpanelAnggota.setParent(tabpanels);
		tabpanelAnggota.setHeight("1250px");
		tabAnggota.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelAnggota.getChildren().size() == 0) {
					KelompokKknHelper kelompokKknHelper = new KelompokKknHelper();
					kelompokKknHelper.display(kelompokKkn, tabpanelAnggota);
				}
			}
		});

		final Tabpanel tabpanelMateri = new ais.ui.util.MyTabpanel();
		tabpanelMateri.setParent(tabpanels);
		tabpanelMateri.setHeight("1250px");
		tabMateri.addEventListener("onClick", new EventListener() {

			private boolean materiBol = true;
			private boolean ujianBol = true;
			private boolean tugasBol = true;

			void reload() throws Exception {

				try {
					Common.clear(tabpanelMateri);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasKknHelper.java:476");
					// TODO: handle exception
				}
				onEvent(null);
			}

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelMateri.getChildren().size() == 0) {

					final MyCheckboxConfig materiPil = new MyCheckboxConfig("Materi");
					final MyCheckboxConfig ujianPil = new MyCheckboxConfig("Ujian");
					final MyCheckboxConfig tugasPil = new MyCheckboxConfig("Tugas");
					final Textbox cari = new Textbox();
					EventListener eventListenerReload = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							materiBol = materiPil.isChecked();
							ujianBol = ujianPil.isChecked();
							tugasBol = tugasPil.isChecked();
							reload();
						}
					};

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(tabpanelMateri);

					boolean mobile = Common.isMobile();

					North north = new North();
					north.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(north, true);
					north.setHeight(mobile ? "70px" : "35px");
					Box vbox = mobile ? new Vbox() : new Hbox();
					vbox.setParent(north);

					Hbox hbox = new Hbox();
					hbox.setParent(vbox);

					hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari :")));
					hbox.appendChild(cari);
					cari.setCols(15);

					final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh",
							"/img/Button-Refresh-icon.png");
					hbox.appendChild(button);

					hbox = new Hbox();
					hbox.setParent(vbox);
					hbox.appendChild(materiPil);
					hbox.appendChild(ujianPil);
					hbox.appendChild(tugasPil);

					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					materiPil.addEventListener("onClick", eventListenerReload);
					ujianPil.addEventListener("onClick", eventListenerReload);
					tugasPil.addEventListener("onClick", eventListenerReload);

					materiPil.setChecked(materiBol);
					ujianPil.setChecked(ujianBol);
					tugasPil.setChecked(tugasBol);

					final Rows rows = new Rows();
					final Paging paging = new Paging();

					Borderlayout subBorderlayout = new Borderlayout();
					subBorderlayout.setParent(center);

					Center subcenter = new Center();
					subcenter.setParent(subBorderlayout);
					ais.ui.util.ZkCompat.setFlex(subcenter, true);
					subcenter.setBorder("none");

					Grid grid = new Grid();
					grid.setSclass("dgrid");
					grid.setParent(subcenter);
					grid.appendChild(rows);

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig();
					column.setParent(columns);

					column = new MyColumnConfig();
					column.setParent(columns);
					column.setWidth(Common.isMobile() ? "40%" : "20%");

					EventListener eventListener = new EventListener() {

						@SuppressWarnings("deprecation")
						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.clear(rows);
							Row row = new Row();
							row.setValign("top");
							row.setStyle("border:0px;background: transparent;font-size: x-small;");
							row.setParent(rows);
							ais.ui.util.ZkCompat.setSpans(row, "2");

							Vbox vbox = new Vbox();
							vbox.setParent(row);
							vbox.setWidth("100%");
							final Label label;
							final boolean refresh = arg0 != null && arg0.getTarget() == button;
							vbox.appendChild(label = new Label(ais.common.Common.getBahasaConfig("Ambil data ...")));
							Image img;
							vbox.appendChild(img = new Image("/loading_icon.gif"));
							img.setWidth("90%");

							Common.createDefaultTimerNoBusy(new EventListener() {

								@Override
								public void onEvent(Event a) throws Exception {
									Tbmuser tbmuser = Common.getCurrentUser();
									Common.clear(rows);
									TampilanELearningAction.loadDataMateri(cari, rows, paging, refresh, materiPil,
											ujianPil, tugasPil, label, tbmuser, kelompokKkn.ambilPertemuan(refresh),
											true, false);
								}
							});

						}
					};

					Common.createDefaultTimer(eventListener);
					button.addEventListener("onClick", eventListener);
					cari.addEventListener("onOK", eventListener);

				}
			}
		});

		final Tabpanel tabpanelTugasKelompok = new ais.ui.util.MyTabpanel();
		int jumlahTugasKelompok = ((Number) session.createCriteria(TugasKelompok.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("kelompokKkn", kelompokKkn)).uniqueResult()).intValue();
		tabTugasKelompok
				.setLabel("Tugas Kelompok " + (jumlahTugasKelompok == 0 ? "" : "(" + jumlahTugasKelompok + ")"));

		tabpanelTugasKelompok.setParent(tabpanels);
		tabpanelTugasKelompok.setHeight("1250px");
		tabTugasKelompok.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelTugasKelompok.getChildren().size() == 0) {

					TugasKelompokHelper tugasKelompokHelper = new TugasKelompokHelper(userMahasiswa, null);
					tugasKelompokHelper.display(null, kelompokKkn, null, tabpanelTugasKelompok);
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
					dataPunyaItemHelper.display(null, null, null, kelompokKkn, null, tabpanelReferensi);

					final Tabpanel tabpanelBukuAjar = new ais.ui.util.MyTabpanel();
					tabpanelBukuAjar.setParent(tabpanels);
					tabpanelBukuAjar.setHeight("1250px");
					tabBukuAjar.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelBukuAjar.getChildren().size() == 0) {

								DataPunyaBukuAjarHelper dataPunyaBukuAjarHelper = new DataPunyaBukuAjarHelper();
								dataPunyaBukuAjarHelper.display(null, null, null, kelompokKkn, null, tabpanelBukuAjar);
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
								dataPunyaArtikelHelper.display(null, null, null, kelompokKkn, null, null, null,
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

					PenilaianKknHelper penilaianKknHelper = new PenilaianKknHelper();
					penilaianKknHelper.display(kelompokKkn, tabpanelPenilaian);
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
					LaporanMonitorKknKbm laporanMonitorKknKbm = new LaporanMonitorKknKbm(kelompokKkn);
					laporanMonitorKknKbm.setBorder("none");
					laporanMonitorKknKbm.setHeight("650px");
					laporanMonitorKknKbm.setWidth("100%");
					tabpanelMonitorKbm.appendChild(laporanMonitorKknKbm);
					tabpanelMonitorKbm.setHeight("650px");
				}

			}
		});

	}

}
