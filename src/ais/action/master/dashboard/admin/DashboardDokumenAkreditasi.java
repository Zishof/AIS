package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.DokumenAkreditasiAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Akreditasi;
import ais.database.model.DokumenAkreditasi;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyWindow;

public class DashboardDokumenAkreditasi extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Textbox cari;
	private Row rowUtama;
	private Combobox searchjenis;
	private Tabpanel tabpanel1;
	private Tabpanel tabpanel2;
	private Paging paging;

	public DashboardDokumenAkreditasi() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardDokumenAkreditasi(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings({ "deprecation" })
	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Dokumen Akreditasi");

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		Tbmuser tbmuser = Common.getCurrentUser();

		if (tbmuser == null || Common.bolehKonfigurasi("dokumen_tampil_utama", Konfigurasi.TIDAK_AKTIF)) {

			String judul = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi().getNama();
			String image = ais.action.master.helper.util.PerguruanTinggiUtil
					.getPerguruanTinggiMedia("logo_perguruanTinggi_");

			if (Common.isMobile()) {
				North north = new North();
				north.setBorder("none");
				borderlayout.appendChild(north);
				north.setHeight("150px");
				north.setSclass("headerHbox");

				Grid grid = new Grid();
				grid.setSclass("dgrid");
				grid.setHeight("100%");
				grid.setSclass("fgrid");
				grid.setStyle("border:0px;background: transparent;");
				grid.setParent(north);

				Columns columns = new Columns();
				columns.setParent(grid);

				Column column = new Column();
				column.setWidth("100%");
				column.setAlign("center");
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setStyle("border:0px;background: transparent;");
				row.setParent(rows);

				Image imgLogo;
				row.appendChild(imgLogo = new Image(image == null ? "img/logo_pmb.png" : image));
				imgLogo.setHeight("58px");

				row = new MyFormRow();
				row.setStyle("border:0px;background: transparent;");
				row.setParent(rows);

				Label namaSeleksi = new Label(
						judul == null ? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
								: judul);
				row.appendChild(namaSeleksi);

				row = new MyFormRow();
				row.setStyle("border:0px;background: transparent;");
				row.setParent(rows);

				Label namaSekolah = new Label(
						Common.getKonfigurasi("label_dokumen_kampus", "Sistem Informasi Dokumen").getNilai());
				row.appendChild(namaSekolah);

				namaSeleksi.setSclass("title1pmb");
				namaSekolah.setSclass("mottopmb");

			} else {

				borderlayout.setStyle("border-radius:20px;");

				North north = new North();
				north.setHeight("60px");
				north.setBorder("none");
				north.setSclass("headerHbox");
				borderlayout.appendChild(north);

				Hbox hbox = new Hbox();
				hbox.appendChild(new Space());
				hbox.appendChild(new Space());
				Image imgLogo;
				hbox.appendChild(imgLogo = new Image(image == null ? "img/logo_pmb.png" : image));
				hbox.appendChild(new Space());
				hbox.setWidth("100%");
				hbox.setHeight("90px");
				north.appendChild(hbox);

				Vbox vbox = new Vbox();

				vbox.setWidth("100%");
				vbox.setPack("center");
				hbox.appendChild(vbox);

				imgLogo.setHeight("50px");

				Label namaSeleksi = new Label(
						judul == null ? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
								: judul);
				vbox.appendChild(namaSeleksi);

				Label namaSekolah = new Label(
						Common.getKonfigurasi("label_dokumen_kampus", "Sistem Informasi Dokumen").getNilai());
				vbox.appendChild(namaSekolah);

				namaSeleksi.setSclass("title1");
				namaSekolah.setSclass("motto");

			}

		}

		Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setWidth("80px");
		column.setParent(columns);

		column = new Column();
		column.setParent(columns);

		column = new Column();
		column.setWidth("80px");
		column.setParent(columns);

		column = new Column();
		column.setParent(columns);

		column = new Column();
		column.setWidth("80px");
		column.setParent(columns);

		column = new Column();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Dokumen:")));
		row.appendChild(cari = new Textbox());
		cari.setWidth("90%");
		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		});
		searchfakultas = new Combobox();
		searchfakultas.setVisible(false);
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Prodi:")));
		row.appendChild(searchjurusan);
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		});
		searchjurusan.setWidth("90%");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis:")));
		row.appendChild(searchjenis = new Combobox());
		searchjenis.setWidth("90%");
		searchjenis.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		});

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
						: searchjurusan.getSelectedItem().getValue());
				Common.insertComboDanSemua(searchjenis, new String[] { "kode", "nama", "jurusan" }, "keterangan",
						Akreditasi.class,
						Restrictions.and(Restrictions.eq("aktif", true),
								jurusan == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("jurusan", jurusan)));
			}
		};

		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(arg0);
				reload();
			}
		});
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "6");

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyButtonConfig myButtonConfig = new MyButtonConfig("Cari", "/img/search.png");

		myButtonConfig.setParent(hbox);
		myButtonConfig.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		});

		myButtonConfig = new MyButtonConfig("Lihat Rinci", "/img/12123-eyes-icon.png");
		myButtonConfig.setParent(hbox);
		myButtonConfig.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyWindow laporan = new MyWindow();
				laporan.setHeight("99%");
				laporan.setWidth("99%");
				laporan.setTitle("Akreditasi");
				laporan.setClosable(true);
				laporan.setBorder("none");

				Borderlayout borderlayout = new Borderlayout();
				laporan.appendChild(borderlayout);

				Center center = new Center();
				ais.ui.util.ZkCompat.setFlex(center, true);
				center.setParent(borderlayout);

				center.appendChild(new Iframe("/pages/master/akreditasi.zul"));
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
				laporan.onModal();
			}
		});

		MyFormRow rowUtamaLagi = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowUtamaLagi, "6");
		rowUtamaLagi.setParent(rows);

		paging = new Paging();
		rowUtamaLagi.appendChild(paging);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		});

		rowUtama = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowUtama, "6");
		rowUtama.setParent(rows);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(rowUtama);
		tabbox.setStyle("min-height: 1150px;");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		Tab tab1 = new Tab("Daftar Dokumen");
		tab1.setParent(tabs);

		Tab tab2 = new Tab("Struktur Dokumen");
		tab2.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setStyle("min-height: 1150px;");

		tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tabpanel2.setStyle("min-height: 1150px;");

		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().isEmpty()) {
					reload2();
				}
			}
		});

		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				reload1();
			}
		});
	}

	private void reload() {
		if (tabpanel1.getLinkedTab().isSelected()) {
			reload1();
		} else {
			reload2();
		}
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DokumenAkreditasi.class)
//				.add(Restrictions.eq("merupakanDokumenInduk", false))
				.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("kode", cari.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("nama", cari.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("keterangan", cari.getValue().trim(), MatchMode.ANYWHERE))))

				.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("akreditasi", searchjenis.getSelectedItem().getValue()))

				.createAlias("akreditasi", "akreditasi")

				.add(Restrictions.or(Restrictions.isNull("akreditasi.kodeGrupPengguna"),
						Restrictions.eq("akreditasi.kodeGrupPengguna", "")))

				.createAlias("akreditasi.jurusan", "jurusan", Criteria.LEFT_JOIN)
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("akreditasi.jurusan", searchjurusan, false))

				.add(Restrictions.or(Restrictions.isNull("akreditasi.aktif"),
						Restrictions.eq("akreditasi.aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (order)
			criteria.addOrder(Order.desc("akreditasi")).addOrder(Order.asc("nomorUrut"));

		return criteria;
	}

	@SuppressWarnings({ "unchecked" })
	private void reload1() {

		Common.initPaging(initCriteria(false), paging);

		Common.clear(tabpanel1);

		List<DokumenAkreditasi> dokumenAkreditasis = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(tabpanel1);
		grid.setHeight("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.setStyle("min-height: 1150px;");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column("Nama");
		column.setParent(columns);
		column.setWidth("40%");

		column = new Column("Keterangan");
		column.setParent(columns);

		column = new Column("Jenis/Prodi");
		column.setParent(columns);

		column = new Column("Tanggal Dokumen");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		for (DokumenAkreditasi dokumenAkreditasi : dokumenAkreditasis) {

			try {
				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setParent(rows);

				Vbox v;

				(v = RevisiHelper.createNewRevisi(DokumenAkreditasi.class, dokumenAkreditasi,
						(dokumenAkreditasi.getKode().isEmpty() ? "" : dokumenAkreditasi.getKode() + " - ")
								+ dokumenAkreditasi.getNama()))
						.setParent(row);

				Vbox vbox = new Vbox();
				vbox.setParent(v);
				Hbox hbox = new Hbox();

				LampiranLain.createDownloadUploadFileLain(hbox, dokumenAkreditasi.getId(),
						DokumenAkreditasi.class.getName(), "Dokumen", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, null, false, false, false, false);

				hbox.setParent(vbox);

				new MyLabelAgakKecil(dokumenAkreditasi.getKeterangan()).setParent(row);

				new MyLabelAgakKecilBold(dokumenAkreditasi.getAkreditasi().getNama()
						+ (dokumenAkreditasi.getAkreditasi().getJurusan() == null ? ""
								: " / " + dokumenAkreditasi.getAkreditasi().getJurusan().getNama()))
						.setParent(row);

				new MyLabelAgakKecilBold(dokumenAkreditasi.getTanggalDokumen() == null ? ""
						: Common.dateFormat112.get().format(dokumenAkreditasi.getTanggalDokumen())).setParent(row);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardDokumenAkreditasi.java:543");
			}
		}

	}

	public Criteria initCriteria2(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DokumenAkreditasi.class).add(Restrictions.isNull("induk"))
				.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("kode", cari.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("nama", cari.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("keterangan", cari.getValue().trim(), MatchMode.ANYWHERE))))

				.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("akreditasi", searchjenis.getSelectedItem().getValue()))

				.createAlias("akreditasi", "akreditasi")

				.add(Restrictions.or(Restrictions.isNull("akreditasi.kodeGrupPengguna"),
						Restrictions.eq("akreditasi.kodeGrupPengguna", "")))

				.createAlias("akreditasi.jurusan", "jurusan", Criteria.LEFT_JOIN)
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("akreditasi.jurusan", searchjurusan, false))

				// .add(searchfakultas.getSelectedItem() == null ||
				// searchfakultas.getSelectedItem().getValue() == null
				// ? Restrictions.sqlRestriction("true")
				// : Restrictions.eq("jurusan.fakultas",
				// searchfakultas.getSelectedItem().getValue()))

				.add(Restrictions.or(Restrictions.isNull("akreditasi.aktif"),
						Restrictions.eq("akreditasi.aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (order)
			criteria.addOrder(Order.desc("akreditasi")).addOrder(Order.asc("nomorUrut"));

		return criteria;
	}

	@SuppressWarnings({ "unchecked" })
	private void reload2() {

		Common.initPaging(initCriteria2(false), paging);

		Common.clear(tabpanel2);

		List<DokumenAkreditasi> dokumenAkreditasis = initCriteria2(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		// setStyle("min-height:" + (130 + (dokumenAkreditasis.size() * 60)) +
		// "px");

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(tabpanel2);
		grid.setHeight("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.setStyle("min-height: 1150px;");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setWidth("0px");
		column.setParent(columns);

		column = new Column();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Akreditasi temp = null;

		for (DokumenAkreditasi dokumenAkreditasi : dokumenAkreditasis) {

			if (temp == null || !temp.getId().equals(dokumenAkreditasi.getAkreditasi().getId())) {
				Group group = new ais.ui.util.MyGroupConfig(dokumenAkreditasi.getAkreditasi().getNama());
				group.setParent(rows);
				temp = dokumenAkreditasi.getAkreditasi();
			}

			MyFormRow row = new MyFormRow();
		row.setValign("top");
			row.setParent(rows);

//			if (dokumenAkreditasi.getMerupakanDokumenInduk()) {
			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			DokumenAkreditasiAction groupbox = new DokumenAkreditasiAction(dokumenAkreditasi.getAkreditasi(),
					dokumenAkreditasi, true, null);
			groupbox.setParent(detail);
			groupbox.setStyle("min-height: 150px;");

			groupbox.init();
			groupbox.appendChild(new MyCaptionStyled(dokumenAkreditasi.getNama()));
//			} else {
//				new Label().setParent(row);
//			}

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			vbox.appendChild(new MyLabelBoldAja(
					(dokumenAkreditasi.getKode().isEmpty() ? "" : dokumenAkreditasi.getKode() + " - ")
							+ dokumenAkreditasi.getNama()));
			vbox.appendChild(new MyLabelAgakKecil(dokumenAkreditasi.getKeterangan()));

//			if (!dokumenAkreditasi.getMerupakanDokumenInduk()) {
			Hbox hbox = new Hbox();
			
			

			LampiranLain.createDownloadUploadFileLain(hbox, dokumenAkreditasi.getId(),
					DokumenAkreditasi.class.getName(), "Dokumen", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, false);

			hbox.setParent(vbox);
//			}
		}

	}
}
