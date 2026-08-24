package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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

import ais.action.master.TampilanELearningAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.FormulirKegiatan;
import ais.database.model.GeneralValueObject;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class RekapitulasiPerkuliahanHelper {

	public static void display(Component parent, final Tbmuser tbmuser, final boolean tampilStatistik) {

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(parent);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		tabbox.setOrient("vertical");

		Tabs tabs = new Tabs();
		tabs.setWidth("30px");
		tabs.setParent(tabbox);

		MyTabConfig tabPerkuliahan = new MyTabConfig("Perkuliahan", "/img/Teacher-icon.png");
		tabPerkuliahan.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabPerkuliahan.setParent(tabs);

		MyTabConfig tabSidang = new MyTabConfig("Sidang", "/img/Books-icon_1.png");
		tabSidang.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabSidang.setParent(tabs);

		MyTabConfig tabBimbingan = new MyTabConfig("Bimbingan", "/img/book-icon_2.png");
		tabBimbingan.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabBimbingan.setParent(tabs);

		MyTabConfig tabKkn = new MyTabConfig("KKN", "/img/Diploma-Certificate-icon.png");
		tabKkn.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabKkn.setParent(tabs);

		MyTabConfig tabPkl = new MyTabConfig("PKL", "/img/Resume-icon.png");
		tabPkl.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabPkl.setParent(tabs);

		MyTabConfig tabPA = new MyTabConfig("Pembimbing Akademik", "/img/10218-man-teacher-icon.png");
		tabPA.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabPA.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtamaPerkuliahan = new ais.ui.util.MyTabpanel();
		tabpanelUtamaPerkuliahan.setParent(tabpanels);

		tampilPerkuliahan(tabpanelUtamaPerkuliahan, tbmuser, tampilStatistik, TampilanELearningAction.PERKULIAHAN);

		final Tabpanel tabpanelSidang = new ais.ui.util.MyTabpanel();
		tabpanelSidang.setParent(tabpanels);
		tabSidang.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelSidang.getChildren().isEmpty()) {
					tampilPerkuliahan(tabpanelSidang, tbmuser, tampilStatistik, TampilanELearningAction.SKRIPSI);
				}
			}
		});

		final Tabpanel tabpanelBimbingan = new ais.ui.util.MyTabpanel();
		tabpanelBimbingan.setParent(tabpanels);
		tabBimbingan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelBimbingan.getChildren().isEmpty()) {
					tampilPerkuliahan(tabpanelBimbingan, tbmuser, tampilStatistik, TampilanELearningAction.BIMBINGAN);
				}
			}
		});

		final Tabpanel tabpanelKkn = new ais.ui.util.MyTabpanel();
		tabpanelKkn.setParent(tabpanels);
		tabKkn.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelKkn.getChildren().isEmpty()) {
					tampilPerkuliahan(tabpanelKkn, tbmuser, tampilStatistik, TampilanELearningAction.KKN);
				}
			}
		});

		final Tabpanel tabpanelPkl = new ais.ui.util.MyTabpanel();
		tabpanelPkl.setParent(tabpanels);
		tabPkl.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPkl.getChildren().isEmpty()) {
					tampilPerkuliahan(tabpanelPkl, tbmuser, tampilStatistik, TampilanELearningAction.PKL);
				}
			}
		});

		final Tabpanel tabpanelPA = new ais.ui.util.MyTabpanel();
		tabpanelPA.setParent(tabpanels);
		tabPA.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPA.getChildren().isEmpty()) {
					tampilPerkuliahan(tabpanelPA, tbmuser, tampilStatistik, TampilanELearningAction.KRS);
				}
			}
		});
	}

	private static void tampilPerkuliahan(Tabpanel tabpanelUtamaPerkuliahan, final Tbmuser tbmuser,
			final boolean tampilStatistik, final Integer ditampilkanHanya) {
		Borderlayout subBorderlayoutUtama = new Borderlayout();
		subBorderlayoutUtama.setParent(tabpanelUtamaPerkuliahan);

		final Center center = new Center();
		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(subBorderlayoutUtama);

		Toolbar hbox = new Toolbar();
		hbox.setParent(north);

		final Textbox cari = new Textbox();
		hbox.appendChild(new MyLabelConfig("Cari:"));
		hbox.appendChild(cari);
		cari.setCols(10);

		hbox.appendChild(new MyLabelConfig("Tahun Akademik"));
		final Combobox tahunAkademik = Common.generateTahunAjaran(null);
		tahunAkademik.setCols(8);
		hbox.appendChild(tahunAkademik);
		tahunAkademik.setReadonly(true);
		hbox.appendChild(new MyLabelConfig("Semester"));
		final Combobox semester = new Combobox();
		semester.setCols(4);
		hbox.appendChild(semester);
		semester.setReadonly(true);

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		semester.appendChild(comboitem);

		Common.selectComboItem(semester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
		refresh.setTooltiptext("Refresh");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (tahunAkademik.getSelectedItem() == null || semester.getSelectedItem() == null) {
					return;
				}
				String ta = (String) (tahunAkademik.getSelectedItem().getValue());
				String smt = (String) semester.getSelectedItem().getValue();
				reload(tbmuser, center, ta, smt, cari.getValue().trim(), true, -1, tampilStatistik, ditampilkanHanya);
			}
		});
		refresh.setParent(hbox);

		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(subBorderlayoutUtama);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tahunAkademik.getSelectedItem() == null || semester.getSelectedItem() == null) {
					return;
				}
				String ta = (String) (tahunAkademik.getSelectedItem().getValue());
				String smt = (String) semester.getSelectedItem().getValue();
				reload(tbmuser, center, ta, smt, cari.getValue().trim(), false, -1, tampilStatistik, ditampilkanHanya);
			}
		};

		Common.createDefaultTimer(eventListener);
		cari.addEventListener("onOK", eventListener);
		tahunAkademik.addEventListener("onChange", eventListener);
		semester.addEventListener("onChange", eventListener);
	}

	@SuppressWarnings({})
	public static List<VOPembelajaran> ambilPembelajaran(Tbmuser tbmuser, String ta, String smt, String cari,
			boolean refreh, int page, boolean tampilStatistik, Integer ditampilkanHanya, Paging paging,
			EventListener eventListener) {
		Integer jumlahDown = null;
		return ambilPembelajaran(tbmuser, ta, smt, cari, refreh, page, tampilStatistik, ditampilkanHanya, paging,
				eventListener, jumlahDown);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<VOPembelajaran> ambilPembelajaran(final Tbmuser tbmuser, final String ta, final String smt,
			final String cari, boolean refreh, final int page, final boolean tampilStatistik,
			final Integer ditampilkanHanya, Paging paging, EventListener eventListener, Integer jumlahDown) {
		List<VOPembelajaran> perkuliahans = new ArrayList<VOPembelajaran>();
		Mahasiswa mahasiswa = tbmuser.getMahasiswa();
		Dosen dosen = tbmuser.ambilDosen();
		if (mahasiswa != null) {

			if (refreh) {
				try {
					Session session = HibernateUtil.currentNativeSession();

					if (ditampilkanHanya.equals(TampilanELearningAction.PERKULIAHAN)) {
						mahasiswa.reInitDetailperkuliahan(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.SKRIPSI)) {
						mahasiswa.reInitSkripsi(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.BIMBINGAN)) {
						mahasiswa.reInitBimbingan(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.KKN)) {
						mahasiswa.reInitKkn(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.PKL)) {
						mahasiswa.reInitPkl(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.KRS)) {
						mahasiswa.reInitKrs(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.KEGIATAN)) {
						mahasiswa.reInitFormulirKegiatanPeserta(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.KONSULTASI)) {
						mahasiswa.reInitKonsultasi(session);
					}
					// session.disconnect();
					closeHibernateSessionQuietly(session);

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiPerkuliahanHelper.java:277");
				}

				HibernateUtil.closeSession();
			}
			Session session = HibernateUtil.currentNativeSession();
			Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(ta, smt, null, "", "", false, null, true, false,
					false, ditampilkanHanya, 0, 1000);
			perkuliahans = (List<VOPembelajaran>) objects[0];
			// session.disconnect();
			closeHibernateSessionQuietly(session);
			HibernateUtil.closeSession();
		} else if (dosen != null) {

			if (refreh) {
				try {
					Session session = HibernateUtil.currentNativeSession();
					if (ditampilkanHanya.equals(TampilanELearningAction.PERKULIAHAN)) {
						dosen.reInitPerkuliahan(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.SKRIPSI)) {
						dosen.reInitSkripsi(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.BIMBINGAN)) {
						dosen.reInitBimbingan(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.KKN)) {
						dosen.reInitKkn(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.PKL)) {
						dosen.reInitPkl(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.KRS)) {
						dosen.reInitKrs(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.KEGIATAN)) {
						dosen.reInitFormulirKegiatanPeserta(session);
					} else if (ditampilkanHanya.equals(TampilanELearningAction.KONSULTASI)) {
						dosen.reInitKonsultasi(session);
					}
					// session.disconnect();
					closeHibernateSessionQuietly(session);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiPerkuliahanHelper.java:314");
				}
				HibernateUtil.closeSession();
			}

			Session session = HibernateUtil.currentNativeSession();
			Object[] objects = dosen.ambilPerkuliahanDanParalel(session, ta, smt, null, "", "", false, null, true,
					false, false, false, false, false, false, false, false, false, false, false, ditampilkanHanya, 0,
					1000);
			perkuliahans = (List<VOPembelajaran>) objects[0];
			// session.disconnect();
			closeHibernateSessionQuietly(session);
			HibernateUtil.closeSession();
		} else {

			Session session = HibernateUtil.currentNativeSession();
			int size = ((Number) TampilanELearningAction
					.initStaticCriteria(false, ditampilkanHanya, cari, tbmuser.ambilFakultas(), tbmuser.ambilJurusan(),
							tbmuser.ambilProgram() == null ? null : tbmuser.ambilProgram().getNama(),
							tbmuser.ambilYayasan(), tbmuser.ambilSekolah(), ta, smt, null, false, false, false, false,
							true, true, true, true, true, true, true, true, true, "", tbmuser, session)
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			// session.disconnect();
			closeHibernateSessionQuietly(session);
			HibernateUtil.closeSession();

			Integer jumlahDataDalamSatuHalaman = 10;
			Integer halaman = page == -1 ? paging.getActivePage() : page;
			if (halaman == null || halaman < 0) {
				halaman = 0;
			}

			/* Clamp: setActivePage() sebelumnya dipanggil dengan parameter mentah `page`
			 * (bukan `halaman`), padahal `page` adalah sentinel -1 dari pemanggil yang
			 * berarti "pakai halaman aktif sekarang". Akibatnya ZK menerima -1 langsung dan
			 * melempar WrongValueException "Unable to set active page to -1 since only N
			 * pages" begitu hasil pencarian/filter menyusut. Batasi juga batas atas terhadap
			 * jumlah halaman riil supaya aman bila offset lama melebihi total setelah
			 * data/filter berubah. */
			int totalHalaman = Common.ROWS_COUNT_ON_PAGE <= 0 ? 1
					: (int) Math.ceil(size / (double) Common.ROWS_COUNT_ON_PAGE);
			if (totalHalaman < 1) {
				totalHalaman = 1;
			}
			if (halaman >= totalHalaman) {
				halaman = totalHalaman - 1;
			}

			try {
				paging.setAttribute("aktif", true);
				paging.setPageSize(Common.ROWS_COUNT_ON_PAGE);
				paging.setPageIncrement(Common.isMobile() ? 5 : 10);
				paging.setMold("os");
				paging.setTotalSize(size);
				paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE);
				paging.setDetailed(true);
				paging.setActivePage(halaman);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RekapitulasiPerkuliahanHelper.java:351");
				// TODO: handle exception
			}
			try {
				paging.addEventListener("onPaging", eventListener);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RekapitulasiPerkuliahanHelper.java:356");
				// TODO: handle exception
			}

			Class clazz = null;
			if (ditampilkanHanya.equals(TampilanELearningAction.PERKULIAHAN)) {
				clazz = Perkuliahan.class;
			} else if (ditampilkanHanya.equals(TampilanELearningAction.SKRIPSI)) {
				clazz = Skripsi.class;
			} else if (ditampilkanHanya.equals(TampilanELearningAction.BIMBINGAN)) {
				clazz = MahasiswaRequestTugasAkhir.class;
			} else if (ditampilkanHanya.equals(TampilanELearningAction.KKN)) {
				clazz = KelompokKkn.class;
			} else if (ditampilkanHanya.equals(TampilanELearningAction.PKL)) {
				clazz = KelompokPkl.class;
			} else if (ditampilkanHanya.equals(TampilanELearningAction.KEGIATAN)) {
				clazz = FormulirKegiatan.class;
			} else if (ditampilkanHanya.equals(TampilanELearningAction.KRS)) {
				clazz = FormulirKegiatan.class;
			}

			if (clazz != null && ditampilkanHanya.equals(TampilanELearningAction.KRS)) {
				session = HibernateUtil.currentNativeSession();
				List<Long> ids = TampilanELearningAction
						.initStaticCriteria(true, ditampilkanHanya, cari, tbmuser.ambilFakultas(),
								tbmuser.ambilJurusan(),
								tbmuser.ambilProgram() == null ? null : tbmuser.ambilProgram().getNama(),
								tbmuser.ambilYayasan(), tbmuser.ambilSekolah(), ta, smt, null, false, false, false,
								false, true, true, true, true, true, true, true, true, true, "", tbmuser, session)
						.setMaxResults(jumlahDataDalamSatuHalaman)
						.setFirstResult(jumlahDown != null ? jumlahDown : (jumlahDataDalamSatuHalaman * halaman))
						.setProjection(Projections.property("id")).list();

				perkuliahans = new ArrayList<VOPembelajaran>();
				for (Long id : ids) {
					KrsMahasiswa krsMahasiswa = (KrsMahasiswa) GeneralValueObject.ambilData(KrsMahasiswa.class,
							id.toString(), true);
					perkuliahans.add(krsMahasiswa);
				}
				// session.disconnect();
				closeHibernateSessionQuietly(session);
				HibernateUtil.closeSession();
			} else if (clazz != null) {
				session = HibernateUtil.currentNativeSession();
				perkuliahans = ConstantValues
						.simpleList(
								TampilanELearningAction
										.initStaticCriteria(true, ditampilkanHanya, cari, tbmuser.ambilFakultas(),
												tbmuser.ambilJurusan(),
												tbmuser.ambilProgram() == null ? null
														: tbmuser.ambilProgram().getNama(),
												tbmuser.ambilYayasan(), tbmuser.ambilSekolah(), ta, smt, null, false,
												false, false, false, true, true, true, true, true, true, true, true,
												true, "", tbmuser, session)
										.setMaxResults(jumlahDataDalamSatuHalaman)
										.setFirstResult(jumlahDown != null ? jumlahDown
												: (jumlahDataDalamSatuHalaman * halaman)),
								clazz);
				// session.disconnect();
				closeHibernateSessionQuietly(session);
				HibernateUtil.closeSession();
			}
		}
		return perkuliahans;
	}

	@SuppressWarnings({ "deprecation" })
	private static void reload(final Tbmuser tbmuser, final Center center, final String ta, final String smt,
			final String cari, boolean refreh, final int page, final boolean tampilStatistik,
			final Integer ditampilkanHanya) {
		if (center != null) {
			Common.clear(center);
		}

		boolean mobile = Common.isMobile();
		Paging paging = new Paging();
		List<VOPembelajaran> perkuliahans = ambilPembelajaran(tbmuser, ta, smt, cari, refreh, page, tampilStatistik,
				ditampilkanHanya, paging, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Paging paging = (Paging) arg0.getTarget();

						reload(tbmuser, center, ta, smt, cari, false, paging.getActivePage(), tampilStatistik,
								ditampilkanHanya);

						Clients.scrollIntoView(paging.getParent().getParent());
					}
				});

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		if (tampilStatistik) {

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setWidth("25%");
			column.setParent(columns);

			column = new MyColumnConfig();
			column.setWidth("45%");
			column.setParent(columns);

			column = new MyColumnConfig();
			column.setWidth("25%");
			column.setParent(columns);
		} else {

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column;
			if (ditampilkanHanya.equals(TampilanELearningAction.SKRIPSI)
					|| ditampilkanHanya.equals(TampilanELearningAction.BIMBINGAN)) {
				column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("0%");
				column = new MyColumnConfig();
				column.setWidth("55%");
				column.setParent(columns);
			} else {
				column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("20%");
				column = new MyColumnConfig();
				column.setWidth("25%");
				column.setParent(columns);
			}

			column = new MyColumnConfig();
			column.setParent(columns);
		}
		Rows rows = new Rows();
		rows.setParent(grid);

		for (VOPembelajaran vpPembelajaran : perkuliahans) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);

			try {
				Common.getDeskripsiPerkuliahanHbox(vpPembelajaran, tampilStatistik, !mobile, row, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						reload(tbmuser, center, ta, smt, cari, true, page, tampilStatistik, ditampilkanHanya);
					}
				}, refreh);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiPerkuliahanHelper.java:515");
			}
		}
		try {
			if (paging != null && paging.getAttribute("aktif") != null) {
				Row row = new Row();
				row.setValign("top");
				ais.ui.util.ZkCompat.setSpans(row, tampilStatistik ? "4" : "3");
				row.setParent(rows);
				row.appendChild(paging);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiPerkuliahanHelper.java:528");
		}
	}


	private static void closeHibernateSessionQuietly(Session session) {
		ais.common.ElearningSessionUtil.closeQuietly(session);
	}

}
