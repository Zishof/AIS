package ais.action.master.kpi;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.master.kpi.helper.FormatKpiDetailAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.kpi.FormatKpi;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class FormatKpiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Checkbox searchaktif;
	private Textbox searchpegawai;

	private AmbilDataSatuanKerjaBanbox searchparent;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private FormatKpi formatKpi;
	private MyToolbarbuttonConfig add;
	private Textbox kode;

	private Row hbFakultasLabel;
	private Row hbYayasan;
	private boolean pt = false;
	private boolean ya = false;

	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox fakultas;
	private Combobox jurusan;
	protected LampiranLain lainMahasiswa;

	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Textbox usernamePenggunaTarget;
	private Textbox usernamePenggunaRealisasi;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private Tabpanel manajemenMasaKpi;
	private Textbox jenisPengguna;
	private Textbox jenisPenggunaRealisasi;

	public void onManajemenMasaKpi(Event event) {
		if (manajemenMasaKpi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenMasaKpi);
			MyInclude iframe = new MyInclude("/pages/master/kpi/masa_kpi.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel nilaiDefaultKpi;

	public void onNilaiDefaultKpi(Event event) {
		if (nilaiDefaultKpi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(nilaiDefaultKpi);
			MyInclude iframe = new MyInclude("/pages/master/kpi/nilai_default_kpi.zul");
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

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }
		if (hbYayasan != null) { hbYayasan.setVisible(ya); }

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

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif", "jurusan", "fakultas",
				"yayasan", "sekolah", "usernamePenggunaTarget", "usernamePenggunaRealisasi",
				"realisasiTidakDibatasiWaktu", "satuanKerja" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(FormatKpi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, FormatKpi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class FormatKpiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final FormatKpi formatKpi = (FormatKpi) arg1;

			new FormatKpiDetailAction(formatKpi).setParent(arg0);

			new Label(formatKpi.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(FormatKpi.class, formatKpi, formatKpi.getNama()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			new Label(formatKpi == null || formatKpi.getFakultas() == null ? "" : formatKpi.getFakultas().getNama())
					.setParent(vbox);
			new Label(formatKpi == null || formatKpi.getYayasan() == null ? "" : formatKpi.getYayasan().getNama())
					.setParent(vbox);

			new Label(formatKpi == null || formatKpi.getJurusan() == null ? "" : formatKpi.getJurusan().getNama())
					.setParent(vbox);

			new Label(formatKpi == null || formatKpi.getSekolah() == null ? "" : formatKpi.getSekolah().getNama())
					.setParent(vbox);

			new Label(formatKpi.getSatuanKerja() == null ? "" : formatKpi.getSatuanKerja().toString()).setParent(arg0);
			new Label(formatKpi.getUsernamePenggunaTarget().isEmpty() ? "Tidak ditentukan"
					: formatKpi.getUsernamePenggunaTarget()).setParent(arg0);

			new Label(formatKpi.getUsernamePenggunaRealisasi().isEmpty() ? "Tidak ditentukan"
					: formatKpi.getUsernamePenggunaRealisasi()).setParent(arg0);

			new Label(formatKpi.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(formatKpi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					formatKpi.setAktif(checkbox.isChecked());
					Common.refreshUpdate(formatKpi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, formatKpi, FormatKpiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new FormatKpi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		formatKpi = (FormatKpi) obj;
		init(formatKpi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(FormatKpi formatKpi) throws Exception {
		this.formatKpi = formatKpi;
		addWindow.setTitle(formatKpi.getId() == null ? "Tambah Format KPI" : "Ubah Format KPI");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Format KPI"));
		row.appendChild(kode = new Textbox(formatKpi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Format KPI"));
		row.appendChild(nama = new Textbox(formatKpi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Target dinilai oleh jenis pengguna"));
		row.appendChild(jenisPengguna = new Textbox(formatKpi.getJenisPengguna()));
		jenisPengguna.setWidth("90%");
		jenisPengguna.setRows(2);

		Common.initKeterangan(rows, "Jika lebih dari satu, pisahkan dengan tanda koma (,).");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Target dinilai oleh username pengguna"));
		row.appendChild(usernamePenggunaTarget = new Textbox(formatKpi.getUsernamePenggunaTarget()));
		usernamePenggunaTarget.setWidth("90%");
		usernamePenggunaTarget.setRows(2);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Username Penilai Target",
				"/img/user_male_add.png");

		MyFormRow rowAmbilPengguna = new MyFormRow();
		rowAmbilPengguna.setParent(rows);
		rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
		rowAmbilPengguna.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								usernamePenggunaTarget.setValue(usernamePenggunaTarget.getValue()
										+ (usernamePenggunaTarget.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		Common.initKeterangan(rows,
				"Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila target boleh dinilai oleh semua username pengguna");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Realisasi dinilai oleh jenis pengguna"));
		row.appendChild(jenisPenggunaRealisasi = new Textbox(formatKpi.getJenisPenggunaRealisasi()));
		jenisPenggunaRealisasi.setWidth("90%");
		jenisPenggunaRealisasi.setRows(2);

		Common.initKeterangan(rows, "Jika lebih dari satu, pisahkan dengan tanda koma (,).");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Realisasi dinilai oleh username pengguna"));
		row.appendChild(usernamePenggunaRealisasi = new Textbox(formatKpi.getUsernamePenggunaRealisasi()));
		usernamePenggunaRealisasi.setWidth("90%");
		usernamePenggunaRealisasi.setRows(2);

		toolbarbutton = new MyToolbarbuttonConfig("Ambil Username Penilai Realisasi", "/img/user_male_add.png");

		rowAmbilPengguna = new MyFormRow();
		rowAmbilPengguna.setParent(rows);
		rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
		rowAmbilPengguna.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								usernamePenggunaRealisasi.setValue(usernamePenggunaRealisasi.getValue()
										+ (usernamePenggunaRealisasi.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		Common.initKeterangan(rows,
				"Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila realisasi boleh dinilai oleh semua username pengguna");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(formatKpi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		Tbmuser tbmuser1 = Common.getCurrentUser();

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				formatKpi.getFakultas() == null ? tbmuser1.ambilFakultas() : formatKpi.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				formatKpi.getJurusan() == null ? tbmuser1.ambilJurusan() : formatKpi.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				formatKpi == null || formatKpi.getYayasan() == null ? tbmuser1.ambilYayasan() : formatKpi.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				formatKpi == null || formatKpi.getSekolah() == null ? tbmuser1.ambilSekolah() : formatKpi.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		satuanKerja.setValue(formatKpi.getSatuanKerja() == null
				? (tbmuser1.ambilSatuanKerja() == null ? "" : tbmuser1.ambilSatuanKerja().toString())
				: formatKpi.getSatuanKerja().toString());
		satuanKerja.setAttribute("satuanKerja",
				formatKpi.getSatuanKerja() == null ? tbmuser1.ambilSatuanKerja() : formatKpi.getSatuanKerja());
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Format"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, formatKpi.getId(), FormatKpi.class.getName(), "Lampiran Format",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran lebih dari satu file, zip dulu semua file tersebut");

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

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Format KPI belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Format KPI dengan nama yang sesuai; (2) pastikan kolom tidak kosong; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaFormatKpi();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Nama Format KPI yang dimasukkan sudah ada di database. Langkah yang dapat dilakukan: (1) gunakan nama lain yang belum terdaftar; (2) periksa daftar Format KPI yang sudah ada; (3) ulangi proses penyimpanan dengan nama berbeda. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (formatKpi.getId() != null) {
			formatKpi = (FormatKpi) session.load(FormatKpi.class, formatKpi.getId());

		}

		formatKpi.setKode(kode.getValue());
		formatKpi.setNama(nama.getValue());
		formatKpi.setKeterangan(keterangan.getValue());

		formatKpi.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		formatKpi.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		formatKpi.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		formatKpi.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		formatKpi.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		formatKpi.setUsernamePenggunaTarget(usernamePenggunaTarget.getValue());
		formatKpi.setUsernamePenggunaRealisasi(usernamePenggunaRealisasi.getValue());

		formatKpi.setJenisPengguna(jenisPengguna.getValue());
		formatKpi.setJenisPenggunaRealisasi(jenisPenggunaRealisasi.getValue());

		Common.refreshSaveOrUpdate(session, formatKpi);

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(formatKpi.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			if (satuanKerjaTreeModel == null) {
				satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
			}
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}
		Session session = HibernateUtil.currentSession();
		List<Long> pegs = null;
		if (!searchpegawai.getValue().trim().isEmpty()) {
			pegs = session.createCriteria(FormatKpiDetail.class).createAlias("pegawai", "pegawai")
					.setProjection(Projections.property("formatKpi.id"))
					.add(Restrictions.or(
							Restrictions.ilike("pegawai.code", searchpegawai.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("pegawai.nama", searchpegawai.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("pegawai.mycode", searchpegawai.getValue().trim(),
											MatchMode.ANYWHERE))))
					.list();

		}

		Criteria criteria = session.createCriteria(FormatKpi.class)

				.add(pegs == null || pegs.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", pegs))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<FormatKpi> formatKpi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(formatKpi);
		grid.setRowRenderer(new FormatKpiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaFormatKpi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(FormatKpi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.formatKpi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.formatKpi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
