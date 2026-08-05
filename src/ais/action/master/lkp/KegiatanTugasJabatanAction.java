package ais.action.master.lkp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.lkp.helper.AmbilDataKegiatanTugasJabatanBanbox;
import ais.action.master.lkp.helper.KegiatanTugasJabatanPunyaIndikatorHelper;
import ais.action.master.lkp.helper.KegiatanTugasJabatanPunyaPredecessorHelper;
import ais.action.master.lkp.helper.KegiatanTugasJabatanPunyaSasaranHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.lkp.KegiatanTugasJabatanPunyaIndikator;
import ais.database.model.lkp.KegiatanTugasJabatanPunyaPredecessor;
import ais.database.model.lkp.KegiatanTugasJabatanPunyaSasaran;
import ais.database.model.lkp.KelompokParameterTambahanKegiatan;
import ais.database.model.lkp.SatuanKegiatanTugasJabatan;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KegiatanTugasJabatanAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchjenis;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private Checkbox searchaktif;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Textbox nama;
	private MyDoublebox angkaKredit;
	private Textbox keterangan;
	private Combobox satuanWaktu;
	private AmbilDataKegiatanTugasJabatanBanbox induk;

	private boolean edit = false;
	private boolean delete = false;

	private KegiatanTugasJabatan kegiatanTugasJabatan;
	private MyToolbarbuttonConfig add;
	private MyDoublebox kuantitasDefault;
	private Combobox satuanKuantitas;
	private MyDoublebox kualitasDefault;
	private MyDoublebox waktuDefault;
	private MyDoublebox biayaDefault;
	private MyDoublebox kuantitasRealisasiDefault;
	protected Rows rowsKelompokParameterTambahanKegiatan;

	private Tabpanel manajemenParameter;
	private MyGrid gridSasaran;
	private MyGrid gridIndikator;
	private MyGrid gridPredecessor;
	private Combobox periode;
	private Combobox userRole;

	public void onManajemenParameter(Event event) {
		if (manajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/lkp/parameter_tambahan_kegiatan.zul");
			iframe.setParent(window);
		}
	}

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

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

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

		Criterion criterion = Restrictions.and(
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.and(Restrictions.not(Restrictions.ilike("roleName", "Ortu", MatchMode.EXACT)),
						Restrictions.and(Restrictions.not(Restrictions.ilike("roleName", "Siswa", MatchMode.EXACT)),
								Restrictions.and(
										Restrictions.not(Restrictions.ilike("roleName", "Peserta", MatchMode.EXACT)),
										Restrictions
												.not(Restrictions.ilike("roleName", "Mahasiswa", MatchMode.EXACT))))));

		Common.insertComboDanSemua(searchjenis, new String[] { "roleName" }, "roleName", Tbmrole.class,
				"Semua Jenis Pengguna", criterion);

		String[] contents = new String[] { "id", "satuanKerja", "userRole", "nama", "angkaKredit", "kuantitasDefault",
				"satuanKuantitas", "kualitasDefault", "waktuDefault", "satuanWaktu", "periode", "biayaDefault",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KegiatanTugasJabatan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class KegiatanTugasJabatanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KegiatanTugasJabatan kegiatanTugasJabatan = (KegiatanTugasJabatan) arg1;

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(kegiatanTugasJabatan.getSatuanKerja() == null ? ""
					: kegiatanTugasJabatan.getSatuanKerja().getNama()).setParent(vbox);
			new Label(
					kegiatanTugasJabatan.getUserRole() == null ? "" : kegiatanTugasJabatan.getUserRole().getRoleName())
					.setParent(vbox);

			vbox = new Vbox();
			vbox.setParent(arg0);
			RevisiHelper
					.createNewRevisi(KegiatanTugasJabatan.class, kegiatanTugasJabatan, kegiatanTugasJabatan.getNama())
					.setParent(vbox);
			int i = 1;
			for (KelompokParameterTambahanKegiatan kelompokParameterTambahanKegiatan : kegiatanTugasJabatan
					.getKelompokParameterTambahanKegiatans()) {
				new Label((i++) + ". " + kelompokParameterTambahanKegiatan.getNama()).setParent(vbox);
			}

			if (kegiatanTugasJabatan.getInduk() != null) {
				new Label("Bagian dari : " + kegiatanTugasJabatan.getInduk().getNama()).setParent(vbox);
			}

			new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getAngkaKredit())).setParent(arg0);
			new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getKuantitasDefault())).setParent(arg0);

			new Label(kegiatanTugasJabatan.getSatuanKuantitas() == null ? ""
					: kegiatanTugasJabatan.getSatuanKuantitas().getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getKualitasDefault()) + " / "
					+ Common.numberFormat.get().format(kegiatanTugasJabatan.getKuantitasRealisasiDefault())).setParent(arg0);

			new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getWaktuDefault())).setParent(arg0);

			new Label(kegiatanTugasJabatan.getSatuanWaktu()).setParent(arg0);
			new Label(kegiatanTugasJabatan.getPeriode()).setParent(arg0);

			new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getBiayaDefault())).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kegiatanTugasJabatan.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kegiatanTugasJabatan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kegiatanTugasJabatan);
				}
			});

			final MyCheckboxConfig checkboxWajib = new MyCheckboxConfig("Wajib");
			checkboxWajib.setChecked(kegiatanTugasJabatan.getWajib());
			checkboxWajib.setParent(arg0);
			checkboxWajib.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kegiatanTugasJabatan.setWajib(checkboxWajib.isChecked());
					Common.refreshSaveOrUpdate(kegiatanTugasJabatan);
				}
			});

			final MyDoublebox noUrut = new MyDoublebox(kegiatanTugasJabatan.getNoUrut());
			noUrut.setParent(arg0);
			noUrut.setWidth("90%");
			noUrut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kegiatanTugasJabatan.setNoUrut(noUrut.getValue());
					Common.refreshSaveOrUpdate(kegiatanTugasJabatan);
				}
			});

			new Label(kegiatanTugasJabatan.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kegiatanTugasJabatan);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(kegiatanTugasJabatan);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KegiatanTugasJabatan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KegiatanTugasJabatan kegiatanTugasJabatan) throws Exception {
		this.kegiatanTugasJabatan = kegiatanTugasJabatan;
		addWindow.setTitle(kegiatanTugasJabatan.getId() == null ? "Tambah Tugas Jabatan (Jobdesk)" : "Ubah Tugas Jabatan (Jobdesk)");
		addWindow.setHeight("98%");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("70%");

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(east);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabSasaran = new MyTabConfig("Sasaran");
		tabSasaran.setParent(tabs);

		final MyTabConfig tabIndikator = new MyTabConfig("Indikator Kinerja");
		tabIndikator.setParent(tabs);

		final MyTabConfig tabPredecessor = new MyTabConfig("Pendahulu / Predecessor");
		tabPredecessor.setParent(tabs);

		final MyTabConfig tabParameter = new MyTabConfig("Parameter");
		tabParameter.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelSasaran = new ais.ui.util.MyTabpanel();
		tabpanelSasaran.setParent(tabpanels);

		tabpanelSasaran.appendChild(new KegiatanTugasJabatanPunyaSasaranHelper(gridSasaran = new MyGrid())
				.initDetail(kegiatanTugasJabatan));

		final Tabpanel tabpanelIndikator = new ais.ui.util.MyTabpanel();
		tabpanelIndikator.setParent(tabpanels);

		tabpanelIndikator.appendChild(new KegiatanTugasJabatanPunyaIndikatorHelper(gridIndikator = new MyGrid())
				.initDetail(kegiatanTugasJabatan));

		final Tabpanel tabpanelPredecessor = new ais.ui.util.MyTabpanel();
		tabpanelPredecessor.setParent(tabpanels);

		tabpanelPredecessor.appendChild(new KegiatanTugasJabatanPunyaPredecessorHelper(gridPredecessor = new MyGrid())
				.initDetail(kegiatanTugasJabatan));

		final Tabpanel tabpanelParameter = new ais.ui.util.MyTabpanel();
		tabpanelParameter.setParent(tabpanels);
		//
		// tabpanelParameter
		// .appendChild(new
		// KegiatanTugasJabatanPunyaJenisParameterHelper(gridParameter = new
		// Grid()).initDetail(kegiatanTugasJabatan));

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan / Unit Kerja (*)"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setAttribute("satuanKerja", kegiatanTugasJabatan.getSatuanKerja());
		satuanKerja.setValue(
				kegiatanTugasJabatan.getSatuanKerja() == null ? "" : kegiatanTugasJabatan.getSatuanKerja().getNama());
		satuanKerja.setWidth("90%");
		satuanKerja.setReadonly(true);

		Criterion criterion = Restrictions.and(
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.and(Restrictions.not(Restrictions.ilike("roleName", "Ortu", MatchMode.EXACT)),
						Restrictions.and(Restrictions.not(Restrictions.ilike("roleName", "Siswa", MatchMode.EXACT)),
								Restrictions.and(
										Restrictions.not(Restrictions.ilike("roleName", "Peserta", MatchMode.EXACT)),
										Restrictions
												.not(Restrictions.ilike("roleName", "Mahasiswa", MatchMode.EXACT))))));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("atau Jenis Pengguna"));
		row.appendChild(userRole = new Combobox());
		Common.insertComboDanSemua(userRole, new String[] { "roleName" }, "roleName", Tbmrole.class,
				"Semua Jenis Pengguna", criterion);
		Common.selectComboItem(true, userRole, kegiatanTugasJabatan.getUserRole());

		userRole.setWidth("90%");
		userRole.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan (*)"));
		row.appendChild(nama = new Textbox(kegiatanTugasJabatan.getNama()));
		nama.setWidth("90%");
		nama.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bagian dari Kegiatan"));
		row.appendChild(induk = new AmbilDataKegiatanTugasJabatanBanbox(true));
		induk.setValue(kegiatanTugasJabatan.getInduk() == null ? "" : kegiatanTugasJabatan.getInduk().getNama());
		induk.setAttribute("myValue", kegiatanTugasJabatan.getInduk());
		induk.setAttribute("kegiatanTugasJabatan", kegiatanTugasJabatan.getInduk());
		induk.setWidth("90%");
		induk.setReadonly(true);

		Common.initKeterangan(rows, "Kosongkan \"Bagian dari Kegiatan\" jika merupakan induk dari suatu kegiatan");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angka Kredit"));
		row.appendChild(angkaKredit = new MyDoublebox(kegiatanTugasJabatan.getAngkaKredit()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kuantitas Default"));
		row.appendChild(kuantitasDefault = new MyDoublebox(kegiatanTugasJabatan.getKuantitasDefault()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kuantitas (*)"));
		row.appendChild(satuanKuantitas = new Combobox());
		Common.insertCombo(satuanKuantitas, "nama", SatuanKegiatanTugasJabatan.class);
		satuanKuantitas.setReadonly(true);
		Common.selectComboItem(satuanKuantitas, kegiatanTugasJabatan.getSatuanKuantitas());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kualitas Default"));
		row.appendChild(kualitasDefault = new MyDoublebox(kegiatanTugasJabatan.getKualitasDefault()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Realisasi Kualitas Default"));
		row.appendChild(
				kuantitasRealisasiDefault = new MyDoublebox(kegiatanTugasJabatan.getKuantitasRealisasiDefault()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Default"));
		row.appendChild(waktuDefault = new MyDoublebox(kegiatanTugasJabatan.getWaktuDefault()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Waktu (*)"));
		row.appendChild(satuanWaktu = new Combobox());
		Comboitem comboitem = new MyComboitemConfig("Menit");
		comboitem.setValue("Menit");
		satuanWaktu.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Jam");
		comboitem.setValue("Jam");
		satuanWaktu.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Hari");
		comboitem.setValue("Hari");
		satuanWaktu.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Minggu");
		comboitem.setValue("Minggu");
		satuanWaktu.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Bulan");
		comboitem.setValue("Bulan");
		satuanWaktu.appendChild(comboitem);

		Common.selectComboItem(satuanWaktu, kegiatanTugasJabatan.getSatuanWaktu());
		satuanWaktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode (*)"));
		row.appendChild(periode = new Combobox());
		comboitem = new MyComboitemConfig(KegiatanTugasJabatan.BULANAN);
		comboitem.setValue(KegiatanTugasJabatan.BULANAN);
		periode.appendChild(comboitem);

		comboitem = new MyComboitemConfig(KegiatanTugasJabatan.TAHUNAN);
		comboitem.setValue(KegiatanTugasJabatan.TAHUNAN);
		periode.appendChild(comboitem);

		Common.selectComboItem(periode, kegiatanTugasJabatan.getPeriode());
		periode.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya Default"));
		row.appendChild(biayaDefault = new MyDoublebox(kegiatanTugasJabatan.getBiayaDefault()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				kegiatanTugasJabatan.getKeterangan() == null ? "" : kegiatanTugasJabatan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		final MyGrid subGrid = new MyGrid();
		subGrid.setParent(tabpanelParameter);

		final List<Long> idsParameter = new ArrayList<Long>();
		for (KelompokParameterTambahanKegiatan kelompokParameterTambahanKegiatan : kegiatanTugasJabatan
				.getKelompokParameterTambahanKegiatans()) {
			idsParameter.add(kelompokParameterTambahanKegiatan.getId());
		}

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Columns columns = new Columns();
				columns.setParent(subGrid);

				MyColumnConfig column = new MyColumnConfig("Pilih");
				column.setParent(columns);
				column.setWidth("50px");

				column = new MyColumnConfig("Kelompok Parameter Tambahan");
				column.setParent(columns);

				Session session = HibernateUtil.currentSession();

				List<KelompokParameterTambahanKegiatan> kelompokParameterTambahanKegiatans = session
						.createCriteria(KelompokParameterTambahanKegiatan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("nomorUrut")).list();

				subGrid.setVisible(!kelompokParameterTambahanKegiatans.isEmpty());

				rowsKelompokParameterTambahanKegiatan = new Rows();
				rowsKelompokParameterTambahanKegiatan.setParent(subGrid);

				for (KelompokParameterTambahanKegiatan kelompokParameterTambahanKegiatan : kelompokParameterTambahanKegiatans) {

					MyFormRow rowKelompokParameterTambahanKegiatan = new MyFormRow();
					rowKelompokParameterTambahanKegiatan.setParent(rowsKelompokParameterTambahanKegiatan);

					MyCheckboxConfig pilih = new MyCheckboxConfig();
					rowKelompokParameterTambahanKegiatan.appendChild(pilih);
					rowKelompokParameterTambahanKegiatan
							.appendChild(new Label(kelompokParameterTambahanKegiatan.getNama()));
					pilih.setChecked(idsParameter.contains(kelompokParameterTambahanKegiatan.getId()));
					pilih.setAttribute("kelompokParameterTambahanKegiatan", kelompokParameterTambahanKegiatan);
				}
			}
		});

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

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Kegiatan Tugas Jabatan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (satuanKuantitas.getSelectedItem() == null) {
			MyMessageboxConfig.show("Satuan Kuantitas harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (satuanWaktu.getSelectedItem() == null) {
			MyMessageboxConfig.show("Satuan Waktu harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		List<Row> rowsSasaran = gridSasaran.getRows().getChildren();
		for (Row row : rowsSasaran) {
			KegiatanTugasJabatanPunyaSasaran kegiatanTugasJabatanPunyaSasaran = (KegiatanTugasJabatanPunyaSasaran) row
					.getAttribute("kegiatanTugasJabatanPunyaSasaran");
			if (kegiatanTugasJabatanPunyaSasaran.getSasaran() == null) {
				MyMessageboxConfig.show("Sasaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsIndikator = gridIndikator.getRows().getChildren();
		for (Row row : rowsIndikator) {
			KegiatanTugasJabatanPunyaIndikator kegiatanTugasJabatanPunyaIndikator = (KegiatanTugasJabatanPunyaIndikator) row
					.getAttribute("kegiatanTugasJabatanPunyaIndikator");
			if (kegiatanTugasJabatanPunyaIndikator.getIndikator() == null) {
				MyMessageboxConfig.show("Indikator harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsPredecessor = gridPredecessor.getRows().getChildren();
		for (Row row : rowsPredecessor) {
			KegiatanTugasJabatanPunyaPredecessor kegiatanTugasJabatanPunyaPredecessor = (KegiatanTugasJabatanPunyaPredecessor) row
					.getAttribute("kegiatanTugasJabatanPunyaPredecessor");
			if (kegiatanTugasJabatanPunyaPredecessor.getKegiatanTugasJabatanPredecessor() == null) {
				MyMessageboxConfig.show("Predecessor harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (kegiatanTugasJabatan.getId() != null) {
			kegiatanTugasJabatan = (KegiatanTugasJabatan) session.load(KegiatanTugasJabatan.class,
					kegiatanTugasJabatan.getId());

		}

		kegiatanTugasJabatan.setInduk((KegiatanTugasJabatan) induk.getAttribute("kegiatanTugasJabatan"));
		kegiatanTugasJabatan.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		kegiatanTugasJabatan.setNama(nama.getValue());
		kegiatanTugasJabatan.setAngkaKredit(angkaKredit.getValue());
		kegiatanTugasJabatan.setKeterangan(keterangan.getValue());
		kegiatanTugasJabatan.setKuantitasDefault(kuantitasDefault.getValue());
		kegiatanTugasJabatan
				.setSatuanKuantitas((SatuanKegiatanTugasJabatan) satuanKuantitas.getSelectedItem().getValue());
		kegiatanTugasJabatan.setKualitasDefault(kualitasDefault.getValue());
		kegiatanTugasJabatan.setWaktuDefault(waktuDefault.getValue());
		kegiatanTugasJabatan.setSatuanWaktu((String) satuanWaktu.getSelectedItem().getValue());
		kegiatanTugasJabatan.setBiayaDefault(biayaDefault.getValue());
		kegiatanTugasJabatan.setKuantitasRealisasiDefault(kuantitasRealisasiDefault.getValue());

		kegiatanTugasJabatan.setPeriode((String) periode.getSelectedItem().getValue());

		kegiatanTugasJabatan.setUserRole(
				userRole.getSelectedItem() == null ? null : (Tbmrole) userRole.getSelectedItem().getValue());

		kegiatanTugasJabatan.setKelompokParameterTambahanKegiatans(new TreeSet<KelompokParameterTambahanKegiatan>());
		List<Row> checkboxs = rowsKelompokParameterTambahanKegiatan.getChildren();
		for (Row checkbox : checkboxs) {
			MyCheckboxConfig myCheckbox = (MyCheckboxConfig) checkbox.getChildren().get(0);
			KelompokParameterTambahanKegiatan kelompokParameterTambahanKegiatan = (KelompokParameterTambahanKegiatan) myCheckbox
					.getAttribute("kelompokParameterTambahanKegiatan");
			if (myCheckbox.isChecked()) {
				kegiatanTugasJabatan.getKelompokParameterTambahanKegiatans().add(kelompokParameterTambahanKegiatan);
			}
		}

		Common.refreshUpdate(session, kegiatanTugasJabatan);

		for (Row row : rowsSasaran) {
			KegiatanTugasJabatanPunyaSasaran kegiatanTugasJabatanPunyaSasaran = (KegiatanTugasJabatanPunyaSasaran) row
					.getAttribute("kegiatanTugasJabatanPunyaSasaran");
			kegiatanTugasJabatanPunyaSasaran.setKegiatanTugasJabatan(kegiatanTugasJabatan);
			session.saveOrUpdate(kegiatanTugasJabatanPunyaSasaran);
		}

		for (Row row : rowsIndikator) {
			KegiatanTugasJabatanPunyaIndikator kegiatanTugasJabatanPunyaIndikator = (KegiatanTugasJabatanPunyaIndikator) row
					.getAttribute("kegiatanTugasJabatanPunyaIndikator");
			kegiatanTugasJabatanPunyaIndikator.setKegiatanTugasJabatan(kegiatanTugasJabatan);
			session.saveOrUpdate(kegiatanTugasJabatanPunyaIndikator);
		}

		for (Row row : rowsPredecessor) {
			KegiatanTugasJabatanPunyaPredecessor kegiatanTugasJabatanPunyaPredecessor = (KegiatanTugasJabatanPunyaPredecessor) row
					.getAttribute("kegiatanTugasJabatanPunyaPredecessor");
			kegiatanTugasJabatanPunyaPredecessor.setKegiatanTugasJabatan(kegiatanTugasJabatan);
			session.saveOrUpdate(kegiatanTugasJabatanPunyaPredecessor);
		}

		return true;
	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KegiatanTugasJabatan.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("userRole", searchjenis.getSelectedItem().getValue()))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas)));

		if (order)
			criteria.addOrder(Order.asc("noUrut")).addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KegiatanTugasJabatan> kegiatanTugasJabatan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kegiatanTugasJabatan);
		grid.setRowRenderer(new KegiatanTugasJabatanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
