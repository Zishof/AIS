package ais.action.master.surat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Session;
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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Vbox;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.surat.helper.AlurPersetujuanSuratMasukTreeModel;
import ais.action.master.surat.helper.AmbilDataAlurPersetujuanSuratKeluarBanbox;
import ais.action.master.surat.helper.AmbilDataKlasifikasiSuratKeluarBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.surat.AlurPersetujuanSuratMasukDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.surat.AlurPersetujuanSuratKeluar;
import ais.database.model.surat.AlurPersetujuanSuratMasuk;
import ais.database.model.surat.KlasifikasiSuratKeluar;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

public class AlurPersetujuanSuratMasukTreeAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;

	private Tree tree;

	private Combobox jenisJabatan;
	private Textbox nama;
	private Textbox keterangan;
	private MyCheckboxConfig defaultItem;
	private Checkbox searchaktif;

	private boolean edit = false;
	private boolean delete = false;

	private AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk;
	private boolean add = false;
	private AlurPersetujuanSuratMasukTreeModel alurPersetujuanSuratMasukTreeModel;

	private TreeMap<AlurPersetujuanSuratMasuk, Treecell[]> treecellMap = new TreeMap<AlurPersetujuanSuratMasuk, Treecell[]>();

	private MyToolbarbuttonConfig addNew;
	private Set<JenisJabatan> selectedJenisJabatan;
	private AmbilDataAlurPersetujuanSuratKeluarBanbox alurPersetujuanSuratKeluar;
	private AmbilDataKlasifikasiSuratKeluarBanbox klasifikasiSuratKeluar;

	private boolean pt;
	private boolean ya;

	private Row hbFakultasLabel;
	private Row hbYayasan;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox yayasan;
	private Combobox sekolah;

	private String tipe = "surat";

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (execution.getParameter("tipe") != null && !execution.getParameter("tipe").trim().isEmpty()) {
			tipe = execution.getParameter("tipe").trim();
		}

		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(null);
			}
		});

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }
		if (hbYayasan != null) { hbYayasan.setVisible(ya); }

		if (addNew != null) { addNew.setVisible(add); }

		initTree();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(null);
			}
		});

	}

	private void initTree() throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Alur Persetujuan");
		treecol.setParent(treecols);

		treecol = new Treecol("Aktif");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("15%");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	public void onAdd(Event event) throws Exception {

		AlurPersetujuanSuratMasuk myalurPersetujuanSuratMasuk = new AlurPersetujuanSuratMasuk();
		myalurPersetujuanSuratMasuk.setParent(null);

		init(myalurPersetujuanSuratMasuk, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Timer timer = new Timer(500);
				timer.setParent(page.getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						timer.detach();
						onReloadTree(arg0);
					}
				});
				timer.start();
			}
		});
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk, final EventListener eventListener)
			throws Exception {
		this.alurPersetujuanSuratMasuk = alurPersetujuanSuratMasuk;

		Tbmuser tbmuser = Common.getCurrentUser();
		if (alurPersetujuanSuratMasuk.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			alurPersetujuanSuratMasuk.setFakultas(tbmuser.ambilFakultas());
		}

		if (alurPersetujuanSuratMasuk.getSatuanKerja() == null && tbmuser.ambilSatuanKerja() != null) {
			alurPersetujuanSuratMasuk.setSatuanKerja(tbmuser.ambilSatuanKerja());
		}

		if (alurPersetujuanSuratMasuk.getJurusan() == null && tbmuser.ambilJurusan() != null) {
			alurPersetujuanSuratMasuk.setJurusan(tbmuser.ambilJurusan());
		}

		if (alurPersetujuanSuratMasuk.getYayasan() == null && tbmuser.ambilYayasan() != null) {
			alurPersetujuanSuratMasuk.setYayasan(tbmuser.ambilYayasan());
		}

		if (alurPersetujuanSuratMasuk.getSekolah() == null && tbmuser.ambilSekolah() != null) {
			alurPersetujuanSuratMasuk.setSekolah(tbmuser.ambilSekolah());
		}

		addWindow.setTitle(alurPersetujuanSuratMasuk.getId() == null ? "Tambah Alur Persetujuan" : "Ubah Alur Persetujuan");
		Common.clear(addWindow);
		addWindow.setHeight("95%");
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Jabatan"));
		row.appendChild(jenisJabatan = new Combobox());
		Common.insertCombo(jenisJabatan, "nama", JenisJabatan.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisJabatan, alurPersetujuanSuratMasuk.getJenisJabatan());
		jenisJabatan.setWidth("90%");
		jenisJabatan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Alur Persetujuan"));
		row.appendChild(nama = new Textbox(
				alurPersetujuanSuratMasuk.getNama() == null ? "" : alurPersetujuanSuratMasuk.getNama()));
		nama.setWidth("90%");

		jenisJabatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisJabatan myJenisJabatan = (JenisJabatan) (jenisJabatan.getSelectedItem() == null ? null
						: jenisJabatan.getSelectedItem().getValue());
				if (myJenisJabatan != null && nama.getValue().trim().equals("")) {
					nama.setValue("Alur persetujuan ke " + myJenisJabatan.getNama());
				}
			}
		});

		if (alurPersetujuanSuratMasuk.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			alurPersetujuanSuratMasuk.setFakultas(tbmuser.ambilFakultas());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(alurPersetujuanSuratMasuk.getSatuanKerja() == null ? ""
				: alurPersetujuanSuratMasuk.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", alurPersetujuanSuratMasuk.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		MyFormRow rowFakultas = new MyFormRow();
		rowFakultas.setVisible(pt);
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new MyLabelConfig("Fakultas"));
		rowFakultas.appendChild(fakultas);
		Common.selectComboItem(fakultas, alurPersetujuanSuratMasuk.getFakultas());
		fakultas.setWidth("90%");

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", alurPersetujuanSuratMasuk.getFakultas() == null ? tbmuser.ambilFakultas()
						: alurPersetujuanSuratMasuk.getFakultas()));

		MyFormRow rowJurusan = new MyFormRow();
		rowJurusan.setVisible(pt);
		rowJurusan.setStyle("border:0px;background: transparent;");
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new MyLabelConfig("Jurusan"));
		rowJurusan.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, alurPersetujuanSuratMasuk.getJurusan());

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				alurPersetujuanSuratMasuk == null || alurPersetujuanSuratMasuk.getYayasan() == null
						? tbmuser.ambilYayasan()
						: alurPersetujuanSuratMasuk.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				alurPersetujuanSuratMasuk == null || alurPersetujuanSuratMasuk.getSekolah() == null
						? tbmuser.ambilSekolah()
						: alurPersetujuanSuratMasuk.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(defaultItem = new MyCheckboxConfig());
		defaultItem.setChecked(
				alurPersetujuanSuratMasuk.getDefaultItem() != null && alurPersetujuanSuratMasuk.getDefaultItem());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig tercopy;
		row.appendChild(tercopy = new MyCheckboxConfig("Alur ini ter-copy ke Surat Keluar"));
		tercopy.setChecked(alurPersetujuanSuratMasuk.getAlurPersetujuanSuratKeluar() != null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alur Surat Masuk"));
		row.appendChild(alurPersetujuanSuratKeluar = new AmbilDataAlurPersetujuanSuratKeluarBanbox(tipe));
		alurPersetujuanSuratKeluar.setAttribute("alurPersetujuanSuratKeluar",
				alurPersetujuanSuratMasuk.getAlurPersetujuanSuratKeluar());
		alurPersetujuanSuratKeluar.setValue(alurPersetujuanSuratMasuk.getAlurPersetujuanSuratKeluar() == null ? ""
				: alurPersetujuanSuratMasuk.getAlurPersetujuanSuratKeluar().toString());
		alurPersetujuanSuratKeluar.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Surat Keluar"));
		row.appendChild(klasifikasiSuratKeluar = new AmbilDataKlasifikasiSuratKeluarBanbox(tipe));
		klasifikasiSuratKeluar.setAttribute("klasifikasiSuratKeluar",
				alurPersetujuanSuratMasuk.getKlasifikasiSuratKeluar());
		klasifikasiSuratKeluar.setValue(alurPersetujuanSuratMasuk.getKlasifikasiSuratKeluar() == null ? ""
				: alurPersetujuanSuratMasuk.getKlasifikasiSuratKeluar().getKode()
						+ (alurPersetujuanSuratMasuk.getKlasifikasiSuratKeluar().getNama() == null
								|| alurPersetujuanSuratMasuk.getKlasifikasiSuratKeluar().getNama().trim().isEmpty() ? ""
										: "-" + alurPersetujuanSuratMasuk.getKlasifikasiSuratKeluar().getNama()));

		klasifikasiSuratKeluar.setWidth("90%");
		klasifikasiSuratKeluar.setReadonly(true);

		EventListener tercopyEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				
				if(!tercopy.isChecked()) {
					alurPersetujuanSuratKeluar.setAttribute("alurPersetujuanSuratKeluar", null);
					alurPersetujuanSuratKeluar.setValue("");
				}
				
				alurPersetujuanSuratKeluar.getParent().setVisible(tercopy.isChecked());
				klasifikasiSuratKeluar.getParent().setVisible(tercopy.isChecked());

			}
		};

		tercopyEventListener.onEvent(null);
		tercopy.addEventListener("onClick", tercopyEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				alurPersetujuanSuratMasuk.getKeterangan() == null ? "" : alurPersetujuanSuratMasuk.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		initKelengkapanBerkas(rows);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
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
					// onReloadTree(null);
					eventListener.onEvent(
							new Event("", null, AlurPersetujuanSuratMasukTreeAction.this.alurPersetujuanSuratMasuk));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	@SuppressWarnings("deprecation")
	private void initKelengkapanBerkas(Rows rows) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Terdapat Jabatan Lain"));
		final MyCheckboxConfig formulirVerifikasi;
		row.appendChild(formulirVerifikasi = new MyCheckboxConfig());
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Daftar Jabatan Lain");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		Tbmuser tbmuser = Common.getCurrentUser();

		@SuppressWarnings("unchecked")
		List<JenisJabatan> jenisJabatans = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(JenisJabatan.class)

						.add(Restrictions.and(
								Restrictions.or(Restrictions.isNull("usernamePengguna"),
										Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
												MatchMode.ANYWHERE)),
								Restrictions.or(Restrictions.isNull("jenisPengguna"),
										Restrictions.ilike("jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
												MatchMode.ANYWHERE))))

						.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				JenisJabatan.class);

		if (alurPersetujuanSuratMasuk.getId() != null) {
			alurPersetujuanSuratMasuk = (AlurPersetujuanSuratMasuk) HibernateUtil.currentSession()
					.createCriteria(AlurPersetujuanSuratMasuk.class)
					.add(Restrictions.idEq(alurPersetujuanSuratMasuk.getId())).uniqueResult();
			selectedJenisJabatan = this.alurPersetujuanSuratMasuk.getJenisJabatans();
		} else {
			selectedJenisJabatan = new HashSet<JenisJabatan>();
		}

		subGrid.setVisible(!selectedJenisJabatan.isEmpty());
		formulirVerifikasi.setChecked(!selectedJenisJabatan.isEmpty());

		formulirVerifikasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				subGrid.setVisible(formulirVerifikasi.isChecked());
			}
		});

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final JenisJabatan jenisJabatan : jenisJabatans) {

			final Checkbox checkbox = new Checkbox(jenisJabatan.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedJenisJabatan.contains(jenisJabatan));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedJenisJabatan.add(jenisJabatan);
					} else {
						selectedJenisJabatan.remove(jenisJabatan);
					}
				}
			});
		}

	}

	public boolean onSave(Event event) throws Exception {

		if (jenisJabatan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Jabatan belum dipilih. Langkah yang dapat dilakukan: (1) klik pilihan Jenis Jabatan pada formulir; (2) pilih jenis jabatan yang sesuai dari daftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Alur Persetujuan belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama pada formulir; (2) isikan nama alur persetujuan secara lengkap dan jelas; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		// if (satuanKerja.getAttribute("satuanKerja") == null) {
		// MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		AlurPersetujuanSuratMasukDao alurPersetujuanSuratMasukDao = DaoFactory.getInstance()
				.getAlurPersetujuanSuratMasukDao();
		if (alurPersetujuanSuratMasuk.getId() != null) {
			alurPersetujuanSuratMasuk = alurPersetujuanSuratMasukDao.load(alurPersetujuanSuratMasuk.getId());
		}

		// alurPersetujuanSuratMasuk.setSatuanKerja((SatuanKerja) satuanKerja
		// .getAttribute("satuanKerja"));
		alurPersetujuanSuratMasuk.setJenisJabatan((JenisJabatan) jenisJabatan.getSelectedItem().getValue());
		alurPersetujuanSuratMasuk.setDefaultItem(defaultItem.isChecked());
		alurPersetujuanSuratMasuk.setNama(nama.getValue());
		alurPersetujuanSuratMasuk.setKeterangan(keterangan.getValue());
		alurPersetujuanSuratMasuk.setJenisJabatans(selectedJenisJabatan);

		alurPersetujuanSuratMasuk.setAlurPersetujuanSuratKeluar(
				(AlurPersetujuanSuratKeluar) alurPersetujuanSuratKeluar.getAttribute("alurPersetujuanSuratKeluar"));

		alurPersetujuanSuratMasuk.setKlasifikasiSuratKeluar(
				(KlasifikasiSuratKeluar) klasifikasiSuratKeluar.getAttribute("klasifikasiSuratKeluar"));

		alurPersetujuanSuratMasuk.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		alurPersetujuanSuratMasuk.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		alurPersetujuanSuratMasuk.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		alurPersetujuanSuratMasuk.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		alurPersetujuanSuratMasuk.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		alurPersetujuanSuratMasuk.setTipe(tipe);
		
		if (alurPersetujuanSuratMasuk.getId() != null) {
			alurPersetujuanSuratMasukDao.update(alurPersetujuanSuratMasuk);
		} else {
			alurPersetujuanSuratMasukDao.save(alurPersetujuanSuratMasuk);
		}

		return true;
	}

	public void onReloadTree(Event event) throws Exception {
		addNew.setVisible(add);
		alurPersetujuanSuratMasukTreeModel = new AlurPersetujuanSuratMasukTreeModel(true, searchfakultas, searchjurusan,
				searchyayasan, searchsekolah, searchparent, searchaktif, tipe);
		tree.setModel(alurPersetujuanSuratMasukTreeModel);
		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk = (AlurPersetujuanSuratMasuk) arg1;
				
				if (alurPersetujuanSuratMasuk == null || alurPersetujuanSuratMasuk.getId() == null) {
					treeitem.setVisible(false);
					return;
				}

				if (alurPersetujuanSuratMasuk.getTipe() == null) {
					alurPersetujuanSuratMasuk.setTipe(tipe);
					Common.refreshUpdate(alurPersetujuanSuratMasuk);
				}

				

				treeitem.setImage("/img/dir.gif");
				try {
					Common.clear(treeitem);
					final Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					hasSomeChilds(treerow, alurPersetujuanSuratMasuk);

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/refresh-cw.svg");
					button.setTooltiptext("Refresh");
					// button.setVisible(hasChild);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadTreeitem(treeitem, true, false);
						}
					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/addthis.svg");
					button.setTooltiptext("Tambah Data");
					button.setVisible(add);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							AlurPersetujuanSuratMasuk myalurPersetujuanSuratMasuk = (AlurPersetujuanSuratMasuk) alurPersetujuanSuratMasuk
									.clone();
							myalurPersetujuanSuratMasuk.setParent(alurPersetujuanSuratMasuk);
							myalurPersetujuanSuratMasuk.setId(null);
							init(myalurPersetujuanSuratMasuk, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									render(treeitem, alurPersetujuanSuratMasuk);
									reloadTreeitem(treeitem, true, true, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											final Timer timer = new Timer(300);
											timer.setParent(page.getFirstRoot());
											timer.addEventListener("onTimer", new EventListener() {

												@SuppressWarnings({})
												@Override
												public void onEvent(Event arg0) throws Exception {
													System.out.println("======= open tree item =======");

													try {
														Treeitem myTreeitem = (Treeitem) treecellMap
																.get(alurPersetujuanSuratMasuk)[0].getParent()
																.getParent();

														render(myTreeitem, alurPersetujuanSuratMasuk);

														reloadTreeitem(myTreeitem, true, false, new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {

																final Timer timer = new Timer(300);
																timer.setParent(page.getFirstRoot());
																timer.addEventListener("onTimer", new EventListener() {

																	@Override
																	public void onEvent(Event arg0) throws Exception {

																		System.out.println(
																				"========================= RELOAD TOTAL ===========================");

																		Treeitem myTreeitem = (Treeitem) treecellMap
																				.get(alurPersetujuanSuratMasuk)[0]
																				.getParent().getParent();
																		reloadTreeitem(myTreeitem, true, false);

																		timer.detach();
																	}

																});
																timer.start();

															}
														});

													} catch (Exception e) {
														// TODO
														// Auto-generated
														// catch
														// block
														Common.tampilErrorJikaAdmin(e);
													}

													timer.detach();
												}
											});

											timer.start();

										}
									});
								}
							});

							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/edit-copy.svg");
					button.setTooltiptext("Copy Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							AlurPersetujuanSuratMasuk myalurPersetujuanSuratMasuk = (AlurPersetujuanSuratMasuk) alurPersetujuanSuratMasuk
									.clone();
							myalurPersetujuanSuratMasuk.setParent(alurPersetujuanSuratMasuk.getParent());
							myalurPersetujuanSuratMasuk.setId(null);
							init(myalurPersetujuanSuratMasuk, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									reloadTreeitem(treeitem, true, true);
								}
							});
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
					button.setTooltiptext("Ubah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							init(alurPersetujuanSuratMasuk, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									reloadTreeitem(treeitem, false, true);

								}
							});
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
							MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {

													alurPersetujuanSuratMasukTreeModel
															.deleteChilds(alurPersetujuanSuratMasuk);

													Common.refreshDelete((alurPersetujuanSuratMasuk));

													reloadTreeitem(treeitem, true, true);
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
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});
	}

	private void reloadTreeitem(final Treeitem treeitem, final Boolean reloadTotal, final Boolean loadParent) {
		reloadTreeitem(treeitem, reloadTotal, loadParent, null);
	}

	private void reloadTreeitem(final Treeitem treeitem, final Boolean reloadTotal, final Boolean loadParent,
			final EventListener eventListener) {
		final Treeitem treeitemParent = loadParent ? treeitem.getParentItem() : treeitem;
		if (treeitemParent == null) {
			try {
				onReloadTree(null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} else {
			treeitemParent.unload();
			final Timer timer = new Timer(300);
			timer.setParent(page.getFirstRoot());
			timer.addEventListener("onTimer", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event arg0) throws Exception {
					treeitemParent.setOpen(true);
					treeitem.setOpen(true);
					if (reloadTotal) {
						reloadTotal();
					}
					if (eventListener != null) {
						eventListener.onEvent(null);
					}
					timer.detach();
				}

			});

			timer.start();
		}
	}

	private void reloadTotal() {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings({ "unchecked" })
	public void openChilds(final Treeitem treeitemParent, int max, int index) {
		if (max > index) {
			treeitemParent.setOpen(true);
			List<MyTreeitemConfig> treeitems = treeitemParent.getChildren();
			for (Object object : treeitems) {

				if (object instanceof Treechildren) {
					Treechildren treechildren = (Treechildren) object;
					List<MyTreeitemConfig> mytreeitems = treechildren.getChildren();
					for (MyTreeitemConfig treeitem : mytreeitems) {
						openChilds(treeitem, max, (++index));
					}

				}
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void closeChilds(final Treeitem treeitemParent) {
		treeitemParent.setOpen(false);
		List<MyTreeitemConfig> treeitems = treeitemParent.getChildren();
		for (Object object : treeitems) {

			if (object instanceof Treechildren) {
				Treechildren treechildren = (Treechildren) object;
				List<MyTreeitemConfig> mytreeitems = treechildren.getChildren();
				for (MyTreeitemConfig treeitem : mytreeitems) {
					closeChilds(treeitem);
				}

			}
		}
	}

	private void hasSomeChilds(Treerow treerow, final AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk) {

		if (alurPersetujuanSuratMasuk == null) {
			return;
		}

		Treecell treecell = new Treecell(alurPersetujuanSuratMasuk.toString());
		treecell.setTooltiptext(alurPersetujuanSuratMasuk.toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		Treecell treecellAktif = new Treecell(
				alurPersetujuanSuratMasuk.getDefaultItem() == null || !alurPersetujuanSuratMasuk.getDefaultItem()
						? "Tidak"
						: "Aktif");

		if (edit) {
			treecellAktif.setLabel("");
			final MyCheckboxConfig defaultItem;
			treecellAktif.appendChild(defaultItem = new MyCheckboxConfig());
			defaultItem.setChecked(
					alurPersetujuanSuratMasuk.getDefaultItem() != null && alurPersetujuanSuratMasuk.getDefaultItem());
			defaultItem.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					alurPersetujuanSuratMasuk.setDefaultItem(defaultItem.isChecked());
					session.update(alurPersetujuanSuratMasuk);
				}
			});
		}
		treecellAktif.setStyle("font-size:x-small;text-align: left;");
		treecellAktif.setParent(treerow);

		treecellMap.put(alurPersetujuanSuratMasuk, new Treecell[] { treecellAktif });

	}

}
