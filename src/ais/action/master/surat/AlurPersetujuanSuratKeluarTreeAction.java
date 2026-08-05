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
import ais.action.master.surat.helper.AlurPersetujuanSuratKeluarTreeModel;
import ais.action.master.surat.helper.AmbilDataAlurPersetujuanSuratMasukBanbox;
import ais.action.master.surat.helper.AmbilDataKlasifikasiSuratMasukBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.surat.AlurPersetujuanSuratKeluarDao;
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
import ais.database.model.surat.KlasifikasiSuratMasuk;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

public class AlurPersetujuanSuratKeluarTreeAction extends GenericAutowireComposer {

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

	private AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar;
	private boolean add = false;
	private AlurPersetujuanSuratKeluarTreeModel alurPersetujuanSuratKeluarTreeModel;

	private TreeMap<AlurPersetujuanSuratKeluar, Treecell[]> treecellMap = new TreeMap<AlurPersetujuanSuratKeluar, Treecell[]>();

	private MyToolbarbuttonConfig addNew;
	// private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataAlurPersetujuanSuratMasukBanbox alurPersetujuanSuratMasuk;
	private AmbilDataKlasifikasiSuratMasukBanbox klasifikasiSuratMasuk;

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
	private MyCheckboxConfig harusMengikutiAlur;

	private String tipe = "surat";
	private MyCheckboxConfig terdapatPilihanSelesai;

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

		AlurPersetujuanSuratKeluar myalurPersetujuanSuratKeluar = new AlurPersetujuanSuratKeluar();
		myalurPersetujuanSuratKeluar.setParent(null);

		init(myalurPersetujuanSuratKeluar, new EventListener() {

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

	private void init(AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar, final EventListener eventListener)
			throws Exception {
		this.alurPersetujuanSuratKeluar = alurPersetujuanSuratKeluar;

		Tbmuser tbmuser = Common.getCurrentUser();
		if (alurPersetujuanSuratKeluar.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			alurPersetujuanSuratKeluar.setFakultas(tbmuser.ambilFakultas());
		}

		if (alurPersetujuanSuratKeluar.getSatuanKerja() == null && tbmuser.ambilSatuanKerja() != null) {
			alurPersetujuanSuratKeluar.setSatuanKerja(tbmuser.ambilSatuanKerja());
		}

		if (alurPersetujuanSuratKeluar.getJurusan() == null && tbmuser.ambilJurusan() != null) {
			alurPersetujuanSuratKeluar.setJurusan(tbmuser.ambilJurusan());
		}

		if (alurPersetujuanSuratKeluar.getYayasan() == null && tbmuser.ambilYayasan() != null) {
			alurPersetujuanSuratKeluar.setYayasan(tbmuser.ambilYayasan());
		}

		if (alurPersetujuanSuratKeluar.getSekolah() == null && tbmuser.ambilSekolah() != null) {
			alurPersetujuanSuratKeluar.setSekolah(tbmuser.ambilSekolah());
		}

		addWindow.setTitle(alurPersetujuanSuratKeluar.getId() == null ? "Tambah Alur Persetujuan" : "Ubah Alur Persetujuan");
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
		Common.selectComboItem(jenisJabatan, alurPersetujuanSuratKeluar.getJenisJabatan());
		jenisJabatan.setWidth("90%");
		jenisJabatan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Alur Persetujuan"));
		row.appendChild(nama = new Textbox(
				alurPersetujuanSuratKeluar.getNama() == null ? "" : alurPersetujuanSuratKeluar.getNama()));
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

		if (alurPersetujuanSuratKeluar.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			alurPersetujuanSuratKeluar.setFakultas(tbmuser.ambilFakultas());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(alurPersetujuanSuratKeluar.getSatuanKerja() == null ? ""
				: alurPersetujuanSuratKeluar.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", alurPersetujuanSuratKeluar.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		MyFormRow rowFakultas = new MyFormRow();
		rowFakultas.setVisible(pt);
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new MyLabelConfig("Fakultas"));
		rowFakultas.appendChild(fakultas);
		Common.selectComboItem(fakultas, alurPersetujuanSuratKeluar.getFakultas());
		fakultas.setWidth("90%");

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", alurPersetujuanSuratKeluar.getFakultas() == null ? tbmuser.ambilFakultas()
						: alurPersetujuanSuratKeluar.getFakultas()));

		MyFormRow rowJurusan = new MyFormRow();
		rowJurusan.setVisible(pt);
		rowJurusan.setStyle("border:0px;background: transparent;");
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new MyLabelConfig("Jurusan"));
		rowJurusan.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, alurPersetujuanSuratKeluar.getJurusan());

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				alurPersetujuanSuratKeluar == null || alurPersetujuanSuratKeluar.getYayasan() == null
						? tbmuser.ambilYayasan()
						: alurPersetujuanSuratKeluar.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				alurPersetujuanSuratKeluar == null || alurPersetujuanSuratKeluar.getSekolah() == null
						? tbmuser.ambilSekolah()
						: alurPersetujuanSuratKeluar.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(defaultItem = new MyCheckboxConfig());
		defaultItem.setChecked(
				alurPersetujuanSuratKeluar.getDefaultItem() != null && alurPersetujuanSuratKeluar.getDefaultItem());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Harus Mengikuti Alur"));
		row.appendChild(harusMengikutiAlur = new MyCheckboxConfig());
		harusMengikutiAlur.setChecked(alurPersetujuanSuratKeluar.getHarusMengikutiAlur());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Terdapat Pilihan Selesai"));
		row.appendChild(terdapatPilihanSelesai = new MyCheckboxConfig());
		terdapatPilihanSelesai.setChecked(alurPersetujuanSuratKeluar.getTerdapatPilihanSelesai());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig tercopy;
		row.appendChild(tercopy = new MyCheckboxConfig("Alur ini ter-copy ke Surat Masuk"));
		tercopy.setChecked(alurPersetujuanSuratKeluar.getAlurPersetujuanSuratMasuk() != null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alur Surat Masuk"));
		row.appendChild(alurPersetujuanSuratMasuk = new AmbilDataAlurPersetujuanSuratMasukBanbox(tipe));
		alurPersetujuanSuratMasuk.setAttribute("alurPersetujuanSuratMasuk",
				alurPersetujuanSuratKeluar.getAlurPersetujuanSuratMasuk());
		alurPersetujuanSuratMasuk.setValue(alurPersetujuanSuratKeluar.getAlurPersetujuanSuratMasuk() == null ? ""
				: alurPersetujuanSuratKeluar.getAlurPersetujuanSuratMasuk().toString());
		alurPersetujuanSuratMasuk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Surat Masuk"));
		row.appendChild(klasifikasiSuratMasuk = new AmbilDataKlasifikasiSuratMasukBanbox(tipe));
		klasifikasiSuratMasuk.setAttribute("klasifikasiSuratMasuk",
				alurPersetujuanSuratKeluar.getKlasifikasiSuratMasuk());
		klasifikasiSuratMasuk.setValue(alurPersetujuanSuratKeluar.getKlasifikasiSuratMasuk() == null ? ""
				: alurPersetujuanSuratKeluar.getKlasifikasiSuratMasuk().getKode()
						+ (alurPersetujuanSuratKeluar.getKlasifikasiSuratMasuk().getNama() == null
								|| alurPersetujuanSuratKeluar.getKlasifikasiSuratMasuk().getNama().trim().isEmpty() ? ""
										: "-" + alurPersetujuanSuratKeluar.getKlasifikasiSuratMasuk().getNama()));

		klasifikasiSuratMasuk.setWidth("90%");
		klasifikasiSuratMasuk.setReadonly(true);

		EventListener tercopyEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (!tercopy.isChecked()) {
					alurPersetujuanSuratMasuk.setAttribute("alurPersetujuanSuratMasuk", null);
					alurPersetujuanSuratMasuk.setValue("");
				}

				alurPersetujuanSuratMasuk.getParent().setVisible(tercopy.isChecked());
				klasifikasiSuratMasuk.getParent().setVisible(tercopy.isChecked());

			}
		};

		tercopyEventListener.onEvent(null);
		tercopy.addEventListener("onClick", tercopyEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				alurPersetujuanSuratKeluar.getKeterangan() == null ? "" : alurPersetujuanSuratKeluar.getKeterangan()));
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
							new Event("", null, AlurPersetujuanSuratKeluarTreeAction.this.alurPersetujuanSuratKeluar));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	private Set<ais.database.model.employ.JenisJabatan> selectedJenisJabatan;

	@SuppressWarnings("deprecation")
	private void initKelengkapanBerkas(Rows rows) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Jabatan Lain"));
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

		if (alurPersetujuanSuratKeluar.getId() != null) {
			alurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) HibernateUtil.currentSession()
					.createCriteria(AlurPersetujuanSuratKeluar.class)
					.add(Restrictions.idEq(alurPersetujuanSuratKeluar.getId())).uniqueResult();
			selectedJenisJabatan = this.alurPersetujuanSuratKeluar.getJenisJabatans();
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

		AlurPersetujuanSuratKeluarDao alurPersetujuanSuratKeluarDao = DaoFactory.getInstance()
				.getAlurPersetujuanSuratKeluarDao();
		if (alurPersetujuanSuratKeluar.getId() != null) {
			alurPersetujuanSuratKeluar = alurPersetujuanSuratKeluarDao.load(alurPersetujuanSuratKeluar.getId());
		}

		// alurPersetujuanSuratKeluar.setSatuanKerja((SatuanKerja) satuanKerja
		// .getAttribute("satuanKerja"));
		alurPersetujuanSuratKeluar.setJenisJabatan((JenisJabatan) jenisJabatan.getSelectedItem().getValue());
		alurPersetujuanSuratKeluar.setDefaultItem(defaultItem.isChecked());
		alurPersetujuanSuratKeluar.setNama(nama.getValue());
		alurPersetujuanSuratKeluar.setKeterangan(keterangan.getValue());
		alurPersetujuanSuratKeluar.setJenisJabatans(selectedJenisJabatan);
		alurPersetujuanSuratKeluar.setAlurPersetujuanSuratMasuk(
				(AlurPersetujuanSuratMasuk) alurPersetujuanSuratMasuk.getAttribute("alurPersetujuanSuratMasuk"));
		alurPersetujuanSuratKeluar.setKlasifikasiSuratMasuk(
				(KlasifikasiSuratMasuk) klasifikasiSuratMasuk.getAttribute("klasifikasiSuratMasuk"));

		alurPersetujuanSuratKeluar.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		alurPersetujuanSuratKeluar.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		alurPersetujuanSuratKeluar.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		alurPersetujuanSuratKeluar.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		alurPersetujuanSuratKeluar.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		alurPersetujuanSuratKeluar.setTerdapatPilihanSelesai(terdapatPilihanSelesai.isChecked());
		alurPersetujuanSuratKeluar.setHarusMengikutiAlur(harusMengikutiAlur.isChecked());

		alurPersetujuanSuratKeluar.setTipe(tipe);

		if (alurPersetujuanSuratKeluar.getId() != null) {
			alurPersetujuanSuratKeluarDao.update(alurPersetujuanSuratKeluar);
		} else {
			alurPersetujuanSuratKeluarDao.save(alurPersetujuanSuratKeluar);
		}

		return true;
	}

	public void onReloadTree(Event event) throws Exception {
		addNew.setVisible(add);
		alurPersetujuanSuratKeluarTreeModel = new AlurPersetujuanSuratKeluarTreeModel(true, searchfakultas,
				searchjurusan, searchyayasan, searchsekolah, searchparent, searchaktif, tipe);
		tree.setModel(alurPersetujuanSuratKeluarTreeModel);
		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) arg1;

				if (alurPersetujuanSuratKeluar == null || alurPersetujuanSuratKeluar.getId() == null) {
					treeitem.setVisible(false);
					return;
				}

				if (alurPersetujuanSuratKeluar.getTipe() == null) {
					alurPersetujuanSuratKeluar.setTipe(tipe);
					Common.refreshUpdate(alurPersetujuanSuratKeluar);
				}

				treeitem.setImage("/img/dir.gif");
				try {
					Common.clear(treeitem);
					final Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					hasSomeChilds(treerow, alurPersetujuanSuratKeluar);

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

							AlurPersetujuanSuratKeluar myalurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) alurPersetujuanSuratKeluar
									.clone();
							myalurPersetujuanSuratKeluar.setParent(alurPersetujuanSuratKeluar);
							myalurPersetujuanSuratKeluar.setId(null);
							init(myalurPersetujuanSuratKeluar, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									render(treeitem, alurPersetujuanSuratKeluar);
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
																.get(alurPersetujuanSuratKeluar)[0].getParent()
																.getParent();

														render(myTreeitem, alurPersetujuanSuratKeluar);

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
																				.get(alurPersetujuanSuratKeluar)[0]
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

							AlurPersetujuanSuratKeluar myalurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) alurPersetujuanSuratKeluar
									.clone();
							myalurPersetujuanSuratKeluar.setParent(alurPersetujuanSuratKeluar.getParent());
							myalurPersetujuanSuratKeluar.setId(null);
							init(myalurPersetujuanSuratKeluar, new EventListener() {

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
							init(alurPersetujuanSuratKeluar, new EventListener() {

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

													alurPersetujuanSuratKeluarTreeModel
															.deleteChilds(alurPersetujuanSuratKeluar);

													Common.refreshDelete((alurPersetujuanSuratKeluar));

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

	private void hasSomeChilds(Treerow treerow, final AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar) {

		if (alurPersetujuanSuratKeluar == null) {
			return;
		}

		Treecell treecell = new Treecell(alurPersetujuanSuratKeluar.toString());
		treecell.setTooltiptext(alurPersetujuanSuratKeluar.toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		Treecell treecellAktif = new Treecell(
				alurPersetujuanSuratKeluar.getDefaultItem() == null || !alurPersetujuanSuratKeluar.getDefaultItem()
						? "Tidak"
						: "Aktif");

		if (edit) {
			treecellAktif.setLabel("");
			final MyCheckboxConfig defaultItem;
			treecellAktif.appendChild(defaultItem = new MyCheckboxConfig());
			defaultItem.setChecked(
					alurPersetujuanSuratKeluar.getDefaultItem() != null && alurPersetujuanSuratKeluar.getDefaultItem());
			defaultItem.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					alurPersetujuanSuratKeluar.setDefaultItem(defaultItem.isChecked());
					session.update(alurPersetujuanSuratKeluar);
				}
			});
		}
		treecellAktif.setStyle("font-size:x-small;text-align: left;");
		treecellAktif.setParent(treerow);

		treecellMap.put(alurPersetujuanSuratKeluar, new Treecell[] { treecellAktif });

	}

}
