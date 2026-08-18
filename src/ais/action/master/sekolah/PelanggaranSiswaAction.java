package ais.action.master.sekolah;


import ais.action.master.pelanggaran.DasbordPelanggaran;
import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.report.Report;
import ais.action.report.format1.sekolah.LaporanPelanggaranSiswa;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Hukuman;
import ais.database.model.sekolah.Pelanggaran;
import ais.database.model.sekolah.PelanggaranDanHukuman;
import ais.database.model.sekolah.PelanggaranSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class PelanggaranSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Tabpanel tabDasbor;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private PelanggaranSiswa pelanggaranSiswa;
	private MyToolbarbuttonConfig add;
	private Set<Pelanggaran> selectedPelanggaran;
	private Set<Hukuman> selectedHukuman;
	private Combobox pelanggaranDanHukuman;
	private AmbilDataSiswaBanbox siswa;
	private MyDatebox waktu;
	private Combobox ta;

	private MyTabConfig jenis;
	private MyTabConfig tab1;
	private MyTabConfig tab2;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && (tbmuser.ambilGuru() != null || tbmuser.getSiswa() != null)) {
			jenis.setVisible(false);
			jenis.getLinkedPanel().setVisible(false);
			tab1.setVisible(false);
			tab1.getLinkedPanel().setVisible(false);
			tab2.setVisible(false);
			tab2.getLinkedPanel().setVisible(false);
		}

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "ta", "sekolah", "keterangan",
				"tampilkanInfoIniSaatMahasiswaLogin", "batasWaktuDitampilkan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PelanggaranSiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
		onDasbor(null);
	}

	@SuppressWarnings("unchecked")
	public static void checkDanTampil(Siswa siswa) {
		Session session = HibernateUtil.currentSession();
		List<PelanggaranSiswa> pelanggaranSiswas = session.createCriteria(PelanggaranSiswa.class)
				.add(Restrictions.eq("siswa", siswa)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("tampilkanInfoIniSaatSiswaLogin", true))
				.add(Restrictions.or(Restrictions.isNull("batasWaktuDitampilkan"),
						Restrictions.le("batasWaktuDitampilkan", WaktuUtil.getDate())))
				.list();

		for (PelanggaranSiswa pelanggaranSiswa : pelanggaranSiswas) {
			info(pelanggaranSiswa);
		}
	}

	public void onDasbor(org.zkoss.zk.ui.event.Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordPelanggaran dasbord = new DasbordPelanggaran(DasbordPelanggaran.Lingkup.SISWA);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Pelanggaran Siswa",
				"Tren dan distribusi pelanggaran yang dilakukan siswa.");
		}
	}

	class PelanggaranSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PelanggaranSiswa pelanggaranSiswa = (PelanggaranSiswa) arg1;

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(pelanggaranSiswa.getSiswa()).setParent(hbox);
			Vbox vbox = new Vbox();
			vbox.setParent(hbox);
			vbox.appendChild(new Label(pelanggaranSiswa.getSiswa().getNomorInduk()));
			vbox.appendChild(new Label(pelanggaranSiswa.getSiswa().getNamaSiswa()));
			vbox.appendChild(new Label(pelanggaranSiswa.getSiswa().getSekolah().getNama()));

			RevisiHelper.createNewRevisi(PelanggaranSiswa.class, pelanggaranSiswa,
					pelanggaranSiswa.getPelanggaranDanHukuman().getNama()).setParent(arg0);
			new Label(Common.dateFormat5.get().format(pelanggaranSiswa.getWaktu())).setParent(arg0);

			new Label(pelanggaranSiswa.getTa()).setParent(arg0);
			new Label(pelanggaranSiswa.getKeterangan()).setParent(arg0);

			vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			for (Pelanggaran pelanggaran : new TreeSet<Pelanggaran>(pelanggaranSiswa.getPelanggarans())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + pelanggaran.getNama()));
				i++;
			}

			vbox = new Vbox();
			vbox.setParent(arg0);
			i = 1;
			for (Hukuman hukuman : new TreeSet<Hukuman>(pelanggaranSiswa.getHukumans())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + hukuman.getNama()));
				i++;
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(pelanggaranSiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pelanggaranSiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pelanggaranSiswa);
				}
			});

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			final MyCheckboxConfig tampilkanInfoIniSaatSiswaLogin = new MyCheckboxConfig("Tampil Saat Login");
			tampilkanInfoIniSaatSiswaLogin.setDisabled(!edit);
			tampilkanInfoIniSaatSiswaLogin.setChecked(pelanggaranSiswa.getTampilkanInfoIniSaatSiswaLogin());
			tampilkanInfoIniSaatSiswaLogin.setParent(vbox2);
			tampilkanInfoIniSaatSiswaLogin.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pelanggaranSiswa.setTampilkanInfoIniSaatSiswaLogin(tampilkanInfoIniSaatSiswaLogin.isChecked());
					Common.refreshSaveOrUpdate(pelanggaranSiswa);
				}
			});

			vbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("Tampil sd tanggal")));

			final MyDatebox batasWaktuDitampilkan = new MyDatebox(pelanggaranSiswa.getBatasWaktuDitampilkan());
			batasWaktuDitampilkan.setDisabled(!edit);
			batasWaktuDitampilkan.setParent(vbox2);
			batasWaktuDitampilkan.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pelanggaranSiswa.setBatasWaktuDitampilkan(batasWaktuDitampilkan.getValue());
					Common.refreshSaveOrUpdate(pelanggaranSiswa);
				}
			});

			Hbox aa;
			(aa = Common.copyEditDeleteButtons(edit, delete, pelanggaranSiswa, PelanggaranSiswaAction.this))
					.setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Pelanggaran Siswa");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					cetak(pelanggaranSiswa);
				}

			});
			button.setParent(aa);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PelanggaranSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pelanggaranSiswa = (PelanggaranSiswa) obj;
		init(pelanggaranSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({})
	public static void cetak(PelanggaranSiswa pelanggaranSiswa) throws Exception {
		Report.generatePDFReport(Report.PDF, LaporanPelanggaranSiswa.generateParameter(pelanggaranSiswa),
				"sekolah/kartu_pelanggaran", pelanggaranSiswa.getWaktu());
	}

	@SuppressWarnings({ "deprecation" })
	private static void info(final PelanggaranSiswa pelanggaranSiswa) {
		final MyWindow addWindow = new MyWindow();
		addWindow.setHeight("80%");
		addWindow.setWidth("600px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Info Kedisiplinan Siswa");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Siswa"));
		row.appendChild(new Label(pelanggaranSiswa.getSiswa() == null ? "" : pelanggaranSiswa.getSiswa().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Pelanggaran *"));
		row.appendChild(new Label(Common.dateFormat3.get().format(pelanggaranSiswa.getWaktu())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(new Label(pelanggaranSiswa.getKeterangan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pelanggaran"));
		row.appendChild(new Label(pelanggaranSiswa.getPelanggaranDanHukuman() == null ? ""
				: pelanggaranSiswa.getPelanggaranDanHukuman().getNama()));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		final MyGrid subGridH = new MyGrid();
		row.appendChild(subGridH);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				PelanggaranDanHukuman pelanggaranDanHukuman = pelanggaranSiswa.getPelanggaranDanHukuman();
				HibernateUtil.currentSession().refresh(pelanggaranDanHukuman);

				Common.clear(subGrid);

				Columns subColumns = new Columns();
				subColumns.setParent(subGrid);
				subColumns.appendChild(new Column("Pelanggaran"));

				Rows subRows = new Rows();
				subRows.setParent(subGrid);

				MyFormRow subRow = new MyFormRow();
				subRow.setStyle("border:0px;background: transparent;");
				subRow.setParent(subRows);
				subRow.setValign("top");

				if (pelanggaranSiswa.getId() != null) {
					HibernateUtil.currentSession().refresh(pelanggaranSiswa);
				}

				Set<Pelanggaran> pelanggarans = pelanggaranSiswa.getPelanggarans();
				Vbox vboxSkala = new Vbox();
				vboxSkala.setPack("top");
				vboxSkala.setParent(subRow);
				for (Pelanggaran pelanggaran : pelanggarans) {
					new Label(pelanggaran.getNama()).setParent(vboxSkala);
				}

				subColumns = new Columns();
				subColumns.setParent(subGridH);
				subColumns.appendChild(new Column("Hukuman"));

				subRows = new Rows();
				subRows.setParent(subGridH);

				subRow = new MyFormRow();
				subRow.setStyle("border:0px;background: transparent;");
				subRow.setParent(subRows);
				subRow.setValign("top");

				Set<Hukuman> hukumans = pelanggaranSiswa.getHukumans();
				vboxSkala = new Vbox();
				vboxSkala.setPack("top");
				vboxSkala.setParent(subRow);
				for (Hukuman hukuman : hukumans) {
					new Label(hukuman.getNama()).setParent(vboxSkala);
				}

			}
		};

		Common.createDefaultTimer(eventListener);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);

		borderlayout.setParent(addWindow);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings({ "deprecation" })
	private void init(PelanggaranSiswa pelanggaranSiswa) {
		this.pelanggaranSiswa = pelanggaranSiswa;
		addWindow.setTitle(pelanggaranSiswa.getId() == null ? "Tambah Pelanggaran Siswa" : "Ubah Pelanggaran Siswa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Siswa *"));
		row.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setAttribute("siswa", pelanggaranSiswa.getSiswa());
		siswa.setValue(pelanggaranSiswa.getSiswa() == null ? "" : pelanggaranSiswa.getSiswa().getNamaSiswa());
		siswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Pelanggaran *"));
		row.appendChild(waktu = new MyDatebox(pelanggaranSiswa.getWaktu()));
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setWidth("90%");
		waktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		row.appendChild(ta = new Combobox());
		ta.setWidth("90%");
		ta.setReadonly(true);
		Common.generateTahunAjaran(ta);
		Common.selectComboItem(ta, pelanggaranSiswa.getTa());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pelanggaranSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pelanggaran *"));
		row.appendChild(pelanggaranDanHukuman = new Combobox());
		Common.insertCombo(pelanggaranDanHukuman, "nama", PelanggaranDanHukuman.class);
		Common.selectComboItem(pelanggaranDanHukuman, pelanggaranSiswa.getPelanggaranDanHukuman());
		pelanggaranDanHukuman.setWidth("90%");
		pelanggaranDanHukuman.setReadonly(true);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		final MyGrid subGridH = new MyGrid();
		row.appendChild(subGridH);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadPelanggaran(subGrid);
				loadHukuman(subGridH);
			}
		};

		pelanggaranDanHukuman.addEventListener("onChange", eventListener);

		Common.createDefaultTimer(eventListener);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	private void loadHukuman(MyGrid subGrid) {

		Common.clear(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Hukuman"));

		PelanggaranDanHukuman pelanggaranDanHukuman = (PelanggaranDanHukuman) (this.pelanggaranDanHukuman
				.getSelectedItem() == null ? null : this.pelanggaranDanHukuman.getSelectedItem().getValue());

		if (pelanggaranDanHukuman == null) {

			Rows subRows = new Rows();
			subRows.setParent(subGrid);

			Common.initKeteranganSatuKolom(subRows, "* Jenis pelanggaran harus dipilih");

			return;
		}

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		HibernateUtil.currentSession().refresh(pelanggaranDanHukuman);

		Set<Hukuman> hukumans = pelanggaranDanHukuman.getHukumans();

		if (pelanggaranSiswa.getId() != null) {
			HibernateUtil.currentSession().refresh(this.pelanggaranSiswa);
		}
		selectedHukuman = this.pelanggaranSiswa.getHukumans();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final Hukuman hukuman : hukumans) {
			if (edit) {
				final Checkbox checkbox = new Checkbox(hukuman.getNama() + (hukuman.getPoin() > 0.1
						? ", pengurangan poin : " + Common.numberFormat.get().format(hukuman.getPoin())
						: ""));
				checkbox.setParent(vboxSkala);
				checkbox.setChecked(selectedHukuman.contains(hukuman));
				checkbox.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (checkbox.isChecked()) {
							selectedHukuman.add(hukuman);
						} else {
							selectedHukuman.remove(hukuman);
						}
					}
				});
			} else {
				new Label(hukuman.getNama()).setParent(vboxSkala);
			}
		}

	}

	private void loadPelanggaran(MyGrid subGrid) {
		Common.clear(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Pelanggaran"));

		PelanggaranDanHukuman pelanggaranDanHukuman = (PelanggaranDanHukuman) (this.pelanggaranDanHukuman
				.getSelectedItem() == null ? null : this.pelanggaranDanHukuman.getSelectedItem().getValue());

		if (pelanggaranDanHukuman == null) {

			Rows subRows = new Rows();
			subRows.setParent(subGrid);

			Common.initKeteranganSatuKolom(subRows, "* Jenis pelanggaran harus dipilih");

			return;
		}

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		HibernateUtil.currentSession().refresh(pelanggaranDanHukuman);

		Set<Pelanggaran> pelanggarans = pelanggaranDanHukuman.getPelanggarans();

		if (pelanggaranSiswa.getId() != null) {
			HibernateUtil.currentSession().refresh(this.pelanggaranSiswa);
		}
		selectedPelanggaran = this.pelanggaranSiswa.getPelanggarans();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final Pelanggaran pelanggaran : pelanggarans) {
			if (edit) {
				final Checkbox checkbox = new Checkbox(pelanggaran.getNama());
				checkbox.setParent(vboxSkala);
				checkbox.setChecked(selectedPelanggaran.contains(pelanggaran));
				checkbox.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (checkbox.isChecked()) {
							selectedPelanggaran.add(pelanggaran);
						} else {
							selectedPelanggaran.remove(pelanggaran);
						}
					}
				});
			} else {
				new Label(pelanggaran.getNama()).setParent(vboxSkala);
			}
		}

	}

	public boolean onSave(Event event) throws Exception {
		if (siswa.getAttribute("siswa") == null) {
			MyMessageboxConfig.show("Siswa harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (pelanggaranDanHukuman.getSelectedItem() == null
				|| pelanggaranDanHukuman.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Jenis pelanggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pelanggaranSiswa.getId() != null) {
			pelanggaranSiswa = (PelanggaranSiswa) session.load(PelanggaranSiswa.class, pelanggaranSiswa.getId());

		}

		pelanggaranSiswa.setSiswa((Siswa) siswa.getAttribute("siswa"));
		pelanggaranSiswa
				.setPelanggaranDanHukuman((PelanggaranDanHukuman) pelanggaranDanHukuman.getSelectedItem().getValue());
		pelanggaranSiswa.setKeterangan(keterangan.getValue());
		pelanggaranSiswa.setPelanggarans(selectedPelanggaran);
		pelanggaranSiswa.setHukumans(selectedHukuman);

		pelanggaranSiswa.setTa((String) ta.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, pelanggaranSiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PelanggaranSiswa.class).createAlias("siswa", "siswa");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}
		if (order)
			criteria.addOrder(Order.desc("waktu"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("siswa.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PelanggaranSiswa> pelanggaranSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pelanggaranSiswa);
		grid.setRowRenderer(new PelanggaranSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
