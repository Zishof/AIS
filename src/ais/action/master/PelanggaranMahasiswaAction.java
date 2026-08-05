package ais.action.master;

import ais.action.master.pelanggaran.DasbordPelanggaran;
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

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.PelanggaranMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Hukuman;
import ais.database.model.sekolah.Pelanggaran;
import ais.database.model.sekolah.PelanggaranDanHukuman;
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

public class PelanggaranMahasiswaAction extends GenericAutowireComposer
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
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private PelanggaranMahasiswa pelanggaranMahasiswa;
	private MyToolbarbuttonConfig add;
	private Set<Pelanggaran> selectedPelanggaran;
	private Set<Hukuman> selectedHukuman;
	private Combobox pelanggaranDanHukuman;
	private AmbilDataMahasiswaBanbox mahasiswa;
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
		if (tbmuser != null && (tbmuser.ambilDosen() != null || tbmuser.getMahasiswa() != null)) {
			jenis.setVisible(false);
			jenis.getLinkedPanel().setVisible(false);
			tab1.setVisible(false);
			tab1.getLinkedPanel().setVisible(false);
			tab2.setVisible(false);
			tab2.getLinkedPanel().setVisible(false);
		}

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

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

		String[] contents = new String[] { "id", "nama", "ta", "mahasiswa", "keterangan",
				"tampilkanInfoIniSaatMahasiswaLogin", "batasWaktuDitampilkan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PelanggaranMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
		onDasbor(null);
	}

	@SuppressWarnings("unchecked")
	public static void checkDanTampil(Mahasiswa mahasiswa) {
		Session session = HibernateUtil.currentSession();
		List<PelanggaranMahasiswa> pelanggaranMahasiswas = session.createCriteria(PelanggaranMahasiswa.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("tampilkanInfoIniSaatMahasiswaLogin", true))
				.add(Restrictions.or(Restrictions.isNull("batasWaktuDitampilkan"),
						Restrictions.le("batasWaktuDitampilkan", WaktuUtil.getDate())))
				.list();

		for (PelanggaranMahasiswa pelanggaranMahasiswa : pelanggaranMahasiswas) {
			info(pelanggaranMahasiswa);
		}
	}

	@SuppressWarnings({ "deprecation" })
	private static void info(final PelanggaranMahasiswa pelanggaranMahasiswa) {
		final MyWindow addWindow = new MyWindow();
		addWindow.setHeight("80%");
		addWindow.setWidth("600px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Info Kedisiplinan Mahasiswa");

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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(new Label(
				pelanggaranMahasiswa.getMahasiswa() == null ? "" : pelanggaranMahasiswa.getMahasiswa().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Pelanggaran *"));
		row.appendChild(new Label(Common.dateFormat3.get().format(pelanggaranMahasiswa.getWaktu())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(new Label(pelanggaranMahasiswa.getKeterangan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pelanggaran"));
		row.appendChild(new Label(pelanggaranMahasiswa.getPelanggaranDanHukuman() == null ? ""
				: pelanggaranMahasiswa.getPelanggaranDanHukuman().getNama()));

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

				PelanggaranDanHukuman pelanggaranDanHukuman = pelanggaranMahasiswa.getPelanggaranDanHukuman();
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

				if (pelanggaranMahasiswa.getId() != null) {
					HibernateUtil.currentSession().refresh(pelanggaranMahasiswa);
				}
				Set<Pelanggaran> pelanggarans = pelanggaranMahasiswa.getPelanggarans();
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

				Set<Hukuman> hukumans = pelanggaranMahasiswa.getHukumans();
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

	public void onDasbor(org.zkoss.zk.ui.event.Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordPelanggaran dasbord = new DasbordPelanggaran(DasbordPelanggaran.Lingkup.MAHASISWA);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Pelanggaran Mahasiswa",
				"Ringkasan dan tren pelanggaran yang dilakukan mahasiswa.");
		}
	}

	class PelanggaranMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PelanggaranMahasiswa pelanggaranMahasiswa = (PelanggaranMahasiswa) arg1;

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(pelanggaranMahasiswa.getMahasiswa()).setParent(hbox);
			Vbox vbox = new Vbox();
			vbox.setParent(hbox);
			vbox.appendChild(new Label(pelanggaranMahasiswa.getMahasiswa().getNim()));

			RevisiHelper.createNewRevisi(PelanggaranMahasiswa.class, pelanggaranMahasiswa,
					pelanggaranMahasiswa.getPelanggaranDanHukuman().getNama()).setParent(arg0);
			new Label(Common.dateFormat5.get().format(pelanggaranMahasiswa.getWaktu())).setParent(arg0);

			new Label(pelanggaranMahasiswa.getTa()).setParent(arg0);
			new Label(pelanggaranMahasiswa.getKeterangan()).setParent(arg0);

			vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			for (Pelanggaran pelanggaran : new TreeSet<Pelanggaran>(pelanggaranMahasiswa.getPelanggarans())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + pelanggaran.getNama()));
				i++;
			}

			vbox = new Vbox();
			vbox.setParent(arg0);
			i = 1;
			for (Hukuman hukuman : new TreeSet<Hukuman>(pelanggaranMahasiswa.getHukumans())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + hukuman.getNama()));
				i++;
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(pelanggaranMahasiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pelanggaranMahasiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pelanggaranMahasiswa);
				}
			});

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			final MyCheckboxConfig tampilkanInfoIniSaatMahasiswaLogin = new MyCheckboxConfig("Tampil Saat Login");
			tampilkanInfoIniSaatMahasiswaLogin.setDisabled(!edit);
			tampilkanInfoIniSaatMahasiswaLogin.setChecked(pelanggaranMahasiswa.getTampilkanInfoIniSaatMahasiswaLogin());
			tampilkanInfoIniSaatMahasiswaLogin.setParent(vbox2);
			tampilkanInfoIniSaatMahasiswaLogin.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pelanggaranMahasiswa
							.setTampilkanInfoIniSaatMahasiswaLogin(tampilkanInfoIniSaatMahasiswaLogin.isChecked());
					Common.refreshSaveOrUpdate(pelanggaranMahasiswa);
				}
			});

			vbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("Tampil sd tanggal")));

			final MyDatebox batasWaktuDitampilkan = new MyDatebox(pelanggaranMahasiswa.getBatasWaktuDitampilkan());
			batasWaktuDitampilkan.setDisabled(!edit);
			batasWaktuDitampilkan.setParent(vbox2);
			batasWaktuDitampilkan.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pelanggaranMahasiswa.setBatasWaktuDitampilkan(batasWaktuDitampilkan.getValue());
					Common.refreshSaveOrUpdate(pelanggaranMahasiswa);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, pelanggaranMahasiswa, PelanggaranMahasiswaAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PelanggaranMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pelanggaranMahasiswa = (PelanggaranMahasiswa) obj;
		init(pelanggaranMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "deprecation" })
	private void init(PelanggaranMahasiswa pelanggaranMahasiswa) {
		this.pelanggaranMahasiswa = pelanggaranMahasiswa;
		addWindow.setTitle(pelanggaranMahasiswa.getId() == null ? "Tambah Pelanggaran Mahasiswa" : "Ubah Pelanggaran Mahasiswa");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Mahasiswa *"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setAttribute("mahasiswa", pelanggaranMahasiswa.getMahasiswa());
		mahasiswa.setValue(
				pelanggaranMahasiswa.getMahasiswa() == null ? "" : pelanggaranMahasiswa.getMahasiswa().getNama());
		mahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Pelanggaran *"));
		row.appendChild(waktu = new MyDatebox(pelanggaranMahasiswa.getWaktu()));
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
		Common.selectComboItem(ta, pelanggaranMahasiswa.getTa());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pelanggaranMahasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pelanggaran *"));
		row.appendChild(pelanggaranDanHukuman = new Combobox());
		Common.insertCombo(pelanggaranDanHukuman, "nama", PelanggaranDanHukuman.class);
		Common.selectComboItem(pelanggaranDanHukuman, pelanggaranMahasiswa.getPelanggaranDanHukuman());
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

		if (pelanggaranMahasiswa.getId() != null) {
			HibernateUtil.currentSession().refresh(this.pelanggaranMahasiswa);
		}
		selectedHukuman = this.pelanggaranMahasiswa.getHukumans();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final Hukuman hukuman : hukumans) {
			final Checkbox checkbox = new Checkbox(hukuman.getNama()
					+ (hukuman.getPoin() > 0.1 ? ", pengurangan poin : " + Common.numberFormat.get().format(hukuman.getPoin())
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

		if (pelanggaranMahasiswa.getId() != null) {
			HibernateUtil.currentSession().refresh(this.pelanggaranMahasiswa);
		}
		selectedPelanggaran = this.pelanggaranMahasiswa.getPelanggarans();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final Pelanggaran pelanggaran : pelanggarans) {
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
		}

	}

	public boolean onSave(Event event) throws Exception {
		if (mahasiswa.getAttribute("mahasiswa") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Mahasiswa",
					"Kolom Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (pelanggaranDanHukuman.getSelectedItem() == null
				|| pelanggaranDanHukuman.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis pelanggaran",
					"Kolom Jenis pelanggaran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis pelanggaran.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pelanggaranMahasiswa.getId() != null) {
			pelanggaranMahasiswa = (PelanggaranMahasiswa) session.load(PelanggaranMahasiswa.class,
					pelanggaranMahasiswa.getId());

		}

		pelanggaranMahasiswa.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		pelanggaranMahasiswa
				.setPelanggaranDanHukuman((PelanggaranDanHukuman) pelanggaranDanHukuman.getSelectedItem().getValue());
		pelanggaranMahasiswa.setKeterangan(keterangan.getValue());
		pelanggaranMahasiswa.setPelanggarans(selectedPelanggaran);
		pelanggaranMahasiswa.setHukumans(selectedHukuman);

		pelanggaranMahasiswa.setTa((String) ta.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, pelanggaranMahasiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PelanggaranMahasiswa.class).createAlias("mahasiswa", "mahasiswa")
				.createAlias("mahasiswa.jurusan", "jurusan");

		if (order)
			criteria.addOrder(Order.desc("waktu"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("mahasiswa.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PelanggaranMahasiswa> pelanggaranMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pelanggaranMahasiswa);
		grid.setRowRenderer(new PelanggaranMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
