package ais.action.master.rab;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
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

import ais.action.master.helper.generic.AmbilDataPegawaiBanyak;
import ais.action.master.rab.helper.AmbilDataJenisWorkspaceBanbox;
import ais.action.master.rab.helper.WorkspacePunyaIndikatorHelper;
import ais.action.master.rab.helper.WorkspacePunyaJenisParameterHelper;
import ais.action.master.rab.helper.WorkspacePunyaPredecessorHelper;
import ais.action.master.rab.helper.WorkspacePunyaSasaranHelper;
import ais.action.master.rab.util.RabUtil;
import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.action.report.format1.rab.LaporanJadwalRencanaAnggaran;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.rab.WorkspaceDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.rab.JenisWorkspace;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.UnitOrganisasi;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaIndikator;
import ais.database.model.rab.WorkspacePunyaJenisParameter;
import ais.database.model.rab.WorkspacePunyaPegawai;
import ais.database.model.rab.WorkspacePunyaPredecessor;
import ais.database.model.rab.WorkspacePunyaSasaran;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

/**
 * Halaman rincian <b>Revisi Anggaran berbasis Proyek</b> (di-include dari halaman Anggaran Proyek).
 *
 * <p>Menampilkan pohon item anggaran satu revisi (volume, satuan, harga satuan, total, jadwal,
 * pegawai penanggung jawab) dan menyediakan operasi ubah struktur: pindah induk item, salin pegawai
 * antar item, perbarui total/harga, serta pemeliharaan agregat parent (durasi, harga total, mulai,
 * selesai, pegawai) lewat {@link ais.action.master.rab.util.WorkspaceTreeModel}.</p>
 *
 * <h3>Higiene session basis data (PENTING)</h3>
 * Kelas ini banyak melakukan operasi tulis via {@link HibernateUtil#currentNativeSession()} yang
 * <b>wajib</b> ditutup. Sebelumnya 12 blok menutup session secara <i>inline</i> dengan
 * {@code HibernateUtil.closeSession()} sehingga jika operasi di tengah (begin/update/commit/query)
 * melempar exception, penutupan ter-skip dan koneksi menggantung ("idle in transaction").
 * Kini SELURUH 12 blok dibungkus {@code try { ... } finally { HibernateUtil.closeSession(); }}
 * sehingga session SELALU ditutup walau terjadi kesalahan. Nilai hasil baca yang masih dipakai
 * setelah penutupan (mis. {@code parentOriginal}) dideklarasikan sebelum {@code try} agar tetap
 * dalam jangkauan. Pembacaan ringan lain memakai session ThreadLocal yang ditutup otomatis.
 *
 * <h3>Catatan teknis</h3>
 * Kompatibel Java 1.7 dan ZK 5.5 (penanganan {@code try/finally} gaya Java 1.6, tanpa multi-catch
 * maupun try-with-resources). Logika fungsional tidak diubah; hanya higiene session yang diperketat.
 */
public class WorkspaceProjectRevisiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Tree tree;
	private MyWindow addWindow;

	private boolean edit = false;
	private boolean delete = false;

	private Textbox kode;
	private Textbox nama;
	private AmbilDataJenisWorkspaceBanbox parent;
	private Textbox keterangan;

	private WorkspaceTreeModel workspaceTreeModel;
	private TreeMap<Workspace, Treecell[]> treecellMap = new TreeMap<Workspace, Treecell[]>();

	private Integer selectedTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
	private Integer revisi = 1;

	private SatuanKerja satuanKerja;
	private SumberDana sumberDana;
	private Integer maxrevisi;
	private MyGrid gridParameter;
	private Workspace workspace;
	private Combobox unitOrganisasi;
	private MyGrid gridIndikator;
	private MyGrid gridSasaran;

	private MyToolbarbuttonConfig cetak;
	private MyGrid gridPredecessor;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		try {

			selectedTahun = Integer.parseInt(execution.getParameter("selectedTahun"));
			satuanKerja = (SatuanKerja) ConstantValues.ambil(SatuanKerja.class.getName(),
					Long.parseLong(execution.getParameter("satuanKerja")));
			sumberDana = (SumberDana) ConstantValues.ambil(SumberDana.class.getName(),
					Long.parseLong(execution.getParameter("sumberDana")));

			revisi = Integer.parseInt(execution.getParameter("revisi").trim());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		initTree();

		onSearchDefault(null);

	}

	public void onCetak(Event event) throws Exception {
		if (satuanKerja == null) {
			MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		LaporanJadwalRencanaAnggaran laporanPerencanaan = new LaporanJadwalRencanaAnggaran(satuanKerja);
		laporanPerencanaan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporanPerencanaan);
		laporanPerencanaan.setHeight("95%");
		laporanPerencanaan.setWidth("90%");
		laporanPerencanaan.setClosable(true);
		laporanPerencanaan.onModal();
	}

	private void init(Workspace workspace, final EventListener eventListener) throws Exception {
		this.workspace = workspace;
		addWindow.setTitle(workspace.getId() == null ? "Tambah Workspace" : "Ubah Workspace");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(workspace.getKode() == null ? "" : workspace.getKode()));
		kode.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(workspace.getNama() == null ? "" : workspace.getNama()));
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Item"));
		row.appendChild(parent = new AmbilDataJenisWorkspaceBanbox(true));
		parent.setValue(workspace.getJenisWorkspace() == null ? "" : workspace.getJenisWorkspace().toString());
		parent.setAttribute("jenisWorkspace", workspace.getJenisWorkspace());
		parent.setWidth("90%");
		parent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// JenisWorkspace jenisWorkspaceParent = (JenisWorkspace) parent
				// .getAttribute("jenisWorkspace");
				// System.out.println("jenisWorkspaceParent = "
				// + jenisWorkspaceParent);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit Organisasi"));
		row.appendChild(unitOrganisasi = new Combobox());
		Common.insertCombo(unitOrganisasi, "nama", UnitOrganisasi.class);
		Common.selectComboItem(unitOrganisasi, workspace.getUnitOrganisasi());
		unitOrganisasi.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(workspace.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(2);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabSasaran = new MyTabConfig("Sasaran");
		tabSasaran.setParent(tabs);

		final MyTabConfig tabIndikator = new MyTabConfig("Indikator Kinerja");
		tabIndikator.setParent(tabs);

		final MyTabConfig tabPredecessor = new MyTabConfig("Predecessor");
		tabPredecessor.setParent(tabs);

		final MyTabConfig tabParameter = new MyTabConfig("Parameter");
		tabParameter.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelSasaran = new ais.ui.util.MyTabpanel();
		tabpanelSasaran.setParent(tabpanels);

		tabpanelSasaran.appendChild(new WorkspacePunyaSasaranHelper(gridSasaran = new MyGrid()).initDetail(workspace));

		final Tabpanel tabpanelIndikator = new ais.ui.util.MyTabpanel();
		tabpanelIndikator.setParent(tabpanels);

		tabpanelIndikator
				.appendChild(new WorkspacePunyaIndikatorHelper(gridIndikator = new MyGrid()).initDetail(workspace));

		final Tabpanel tabpanelPredecessor = new ais.ui.util.MyTabpanel();
		tabpanelPredecessor.setParent(tabpanels);

		tabpanelPredecessor
				.appendChild(new WorkspacePunyaPredecessorHelper(gridPredecessor = new MyGrid()).initDetail(workspace));

		final Tabpanel tabpanelParameter = new ais.ui.util.MyTabpanel();
		tabpanelParameter.setParent(tabpanels);

		tabpanelParameter.appendChild(
				new WorkspacePunyaJenisParameterHelper(gridParameter = new MyGrid()).initDetail(workspace));

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
					eventListener.onEvent(new Event("", null, WorkspaceProjectRevisiAction.this.workspace));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		List<Row> rows = gridParameter.getRows().getChildren();
		for (Row row : rows) {
			WorkspacePunyaJenisParameter workspacePunyaJenisParameter = (WorkspacePunyaJenisParameter) row
					.getAttribute("workspacePunyaJenisParameter");
			if (workspacePunyaJenisParameter.getJenisParameter() == null) {
				MyMessageboxConfig.show("Parameter harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsSasaran = gridSasaran.getRows().getChildren();
		for (Row row : rowsSasaran) {
			WorkspacePunyaSasaran workspacePunyaSasaran = (WorkspacePunyaSasaran) row
					.getAttribute("workspacePunyaSasaran");
			if (workspacePunyaSasaran.getSasaran() == null) {
				MyMessageboxConfig.show("Sasaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsIndikator = gridIndikator.getRows().getChildren();
		for (Row row : rowsIndikator) {
			WorkspacePunyaIndikator workspacePunyaIndikator = (WorkspacePunyaIndikator) row
					.getAttribute("workspacePunyaIndikator");
			if (workspacePunyaIndikator.getIndikator() == null) {
				MyMessageboxConfig.show("Indikator harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsPredecessor = gridPredecessor.getRows().getChildren();
		for (Row row : rowsPredecessor) {
			WorkspacePunyaPredecessor workspacePunyaPredecessor = (WorkspacePunyaPredecessor) row
					.getAttribute("workspacePunyaPredecessor");
			if (workspacePunyaPredecessor.getWorkspacePredecessor() == null) {
				MyMessageboxConfig.show("Predecessor harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		WorkspaceDao workspaceDao = DaoFactory.getInstance().getWorkspaceDao();
		if (workspace.getId() != null) {
			workspace = workspaceDao.load(workspace.getId());
		}

		workspace.setKode(kode.getText().trim());
		workspace.setNama(nama.getText().trim());
		workspace.setRevisi(revisi);
		workspace.setKeterangan(keterangan.getValue());
		workspace.setJenisWorkspace((JenisWorkspace) parent.getAttribute("jenisWorkspace"));
		workspace.setUnitOrganisasi((UnitOrganisasi) (unitOrganisasi.getSelectedItem() == null ? null
				: unitOrganisasi.getSelectedItem().getValue()));

		if (workspace.getId() != null) {
			workspaceDao.update(workspace);
		} else {
			workspaceDao.save(workspace);
		}

		Session session = workspaceDao.getCurrentSession();
		for (Row row : rows) {
			WorkspacePunyaJenisParameter workspacePunyaJenisParameter = (WorkspacePunyaJenisParameter) row
					.getAttribute("workspacePunyaJenisParameter");
			workspacePunyaJenisParameter.setWorkspace(workspace);
			session.saveOrUpdate(workspacePunyaJenisParameter);
		}

		for (Row row : rowsSasaran) {
			WorkspacePunyaSasaran workspacePunyaSasaran = (WorkspacePunyaSasaran) row
					.getAttribute("workspacePunyaSasaran");
			workspacePunyaSasaran.setWorkspace(workspace);
			session.saveOrUpdate(workspacePunyaSasaran);
		}

		for (Row row : rowsIndikator) {
			WorkspacePunyaIndikator workspacePunyaIndikator = (WorkspacePunyaIndikator) row
					.getAttribute("workspacePunyaIndikator");
			workspacePunyaIndikator.setWorkspace(workspace);
			session.saveOrUpdate(workspacePunyaIndikator);
		}

		for (Row row : rowsPredecessor) {
			WorkspacePunyaPredecessor workspacePunyaPredecessor = (WorkspacePunyaPredecessor) row
					.getAttribute("workspacePunyaPredecessor");
			workspacePunyaPredecessor.setWorkspace(workspace);
			session.saveOrUpdate(workspacePunyaPredecessor);
		}

		RabUtil.checkForChildsCopy(workspace, workspaceTreeModel, workspace.getTahunWorkspace(), revisi, satuanKerja,
				sumberDana, satuanKerja, sumberDana);

		return true;
	}

	private void initTree() throws Exception {

		if (selectedTahun == null) {
			MyMessageboxConfig.show("Tahun Anggaran Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Item Anggaran");
		treecol.setWidth("35%");
		treecol.setParent(treecols);

		treecol = new Treecol("S");
		treecol.setWidth("2%");
		treecol.setParent(treecols);

		treecol = new Treecol("I");
		treecol.setWidth("2%");
		treecol.setParent(treecols);

		treecol = new Treecol("P");
		treecol.setWidth("2%");
		treecol.setParent(treecols);

		treecol = new Treecol("Durasi");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Mulai");
		treecol.setWidth("9%");
		treecol.setParent(treecols);

		treecol = new Treecol("Selesai");
		treecol.setWidth("9%");
		treecol.setParent(treecols);

		treecol = new Treecol("Komplit");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Perulangan");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Dikerjakan Oleh");
		treecol.setWidth("12%");
		treecol.setParent(treecols);

		treecol = new Treecol("Anggaran");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("7%");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	public void onReloadTree(Event event) throws Exception {

		if (selectedTahun == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		maxrevisi = (Integer) HibernateUtil.currentSession().createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", selectedTahun)).setProjection(Projections.max("revisi"))
				.uniqueResult();
		maxrevisi = maxrevisi == null ? 1 : maxrevisi;

		RabUtil.ubahSemuaStatus(selectedTahun, revisi, satuanKerja, sumberDana);
		cetak.setVisible(maxrevisi.equals(revisi));

		workspaceTreeModel = new WorkspaceTreeModel((Integer) selectedTahun, revisi, satuanKerja, sumberDana);

		if (workspaceTreeModel.getSatuanKerjas().size() == 1 && sumberDana == null) {
			MyMessageboxConfig.show("Sumber Dana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (edit) {
			edit = maxrevisi.equals(revisi);
		}
		if (delete) {
			delete = workspaceTreeModel.getSatuanKerjas().size() == 1;
		}

		tree.setModel(workspaceTreeModel);

		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final Workspace workspace = (Workspace) arg1;

				if (workspace == null || !workspace.getAktif()) {
					treeitem.detach();
					return;
				}

				try {

					boolean hasChild = workspaceTreeModel.getChildCount(workspace) != 0;

					treeitem.setAttribute("workspace", workspace);
					treeitem.setDraggable((edit && hasChild) + "");
					treeitem.setDroppable((edit) + "");
					treeitem.addEventListener("onDrop", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							DropEvent dropEvent = (DropEvent) arg0;
							final MyTreeitemConfig dragged = (MyTreeitemConfig) dropEvent.getDragged();
							final MyTreeitemConfig self = (MyTreeitemConfig) dropEvent.getTarget();

							final Workspace parent = (Workspace) self.getAttribute("workspace");
							final Workspace child = (Workspace) dragged.getAttribute("workspace");

							// System.out.println("parent = " + parent);
							// System.out.println("child = " + child);
							// System.out.println("workspace = " + workspace);

							// if (workspace != null && parent != null
							// && workspace.getId().equals(parent.getId())) {
							// return;
							// }

							Session session = HibernateUtil.currentNativeSession();
							Workspace parentOriginal = null;
							try {
								parentOriginal = (Workspace) session.createCriteria(Workspace.class)
										.add(Restrictions.or(Restrictions.eq("carryOver", true),
												Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true))))
										.add(Restrictions.idEq(child.getParentId())).uniqueResult();

								child.setParentId(parent.getId());
								session.getTransaction().begin();
								session.update(child);
								session.getTransaction().commit();

							} finally {
								HibernateUtil.closeSession();
							}

							if (parentOriginal != null) {
								workspaceTreeModel.ubahDurasiParents(parentOriginal);
								workspaceTreeModel.ubahHargaTotalParents(parentOriginal);
								workspaceTreeModel.ubahKomplitParents(parentOriginal);
								workspaceTreeModel.ubahMulaiParents(parentOriginal);
								workspaceTreeModel.ubahPegawaisParents(parentOriginal);
								// workspaceTreeModel
								// .ubahRealisasiParents(parentOriginal);
								workspaceTreeModel.ubahSelesaiParents(parentOriginal);
							}

							workspaceTreeModel.ubahDurasiParents(parent);
							workspaceTreeModel.ubahHargaTotalParents(parent);
							workspaceTreeModel.ubahKomplitParents(parent);
							workspaceTreeModel.ubahMulaiParents(parent);
							workspaceTreeModel.ubahPegawaisParents(parent);
							// workspaceTreeModel.ubahRealisasiParents(parent);
							workspaceTreeModel.ubahSelesaiParents(parent);

							final Timer timer = new Timer(500);
							timer.setParent(page.getFirstRoot());
							timer.addEventListener("onTimer", new EventListener() {

								@SuppressWarnings({})
								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});
							timer.start();

						}
					});

					Common.clear(treeitem);
					final Treerow treerow = new Treerow();
					treerow.setParent(treeitem);
					if (workspace.getJenisWorkspace() != null) {
						JenisWorkspace jenisWorkspace = workspace.getJenisWorkspace();
						treerow.setStyle((jenisWorkspace.getWarna() != null
								? "background-color:" + jenisWorkspace.getWarna() + ";"
								: "")
								+ (jenisWorkspace.getWarnaText() != null
										? "color:" + jenisWorkspace.getWarnaText() + ";"
										: ""));
					}

					if (hasChild) {
						if (workspace.getLeaf() == null || workspace.getLeaf()) {
							workspace.setLeaf(false);
							Session session = HibernateUtil.currentSession();
							Common.refreshUpdate(session, (workspace));
						}
					} else {
						if (workspace.getLeaf() == null || !workspace.getLeaf()) {
							workspace.setLeaf(true);
							Session session = HibernateUtil.currentSession();
							Common.refreshUpdate(session, (workspace));
						}
					}

					if (hasChild || !edit) {
						hasSomeChilds(treerow, workspace);
					} else {
						noChilds(treerow, workspace);
					}

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();
					toolbar.setParent(arg0);
					toolbar.setVisible(maxrevisi.equals(revisi));

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/refresh-cw.svg");
					button.setTooltiptext("Refresh");
					button.setVisible(hasChild);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadTreeitem(treeitem, true, false);
						}
					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
					button.setTooltiptext("Ubah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							init(workspace, new EventListener() {

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

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});
	}

	private void reloadTreeitem(final Treeitem treeitem, final Boolean reloadTotalTanggal, final Boolean loadParent) {
		reloadTreeitem(treeitem, reloadTotalTanggal, loadParent, null);
	}

	private void reloadTreeitem(final Treeitem treeitem, final Boolean reloadTotalTanggal, final Boolean loadParent,
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
					if (reloadTotalTanggal) {
						reloadTotalTanggal();
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

	private void reloadTotalPegawai() {
		for (final Workspace workspace : treecellMap.keySet()) {
			Treecell[] treecell = treecellMap.get(workspace);
			List<WorkspacePunyaPegawai> workspacePunyaPegawais = workspaceTreeModel.getPagawais(workspace);

			Session session = HibernateUtil.currentNativeSession();
			try {
				session.getTransaction().begin();
				session.createSQLQuery("delete from rab.workspace_punya_pegawai where workspace = :workspaceId")
						.setLong("workspaceId", workspace.getId()).executeUpdate();
				session.getTransaction().commit();
				session.flush();

				session.getTransaction().begin();
				for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
					WorkspacePunyaPegawai copyWorkspacePunyaPegawai = (WorkspacePunyaPegawai) workspacePunyaPegawai.clone();
					copyWorkspacePunyaPegawai.setId(null);
					copyWorkspacePunyaPegawai.setWorkspace(workspace);
					session.save(copyWorkspacePunyaPegawai);
				}

				session.getTransaction().commit();

			} finally {
				HibernateUtil.closeSession();
			}

			String pegs = "";
			if (workspacePunyaPegawais != null) {
				int i = 0;
				for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
					if (i == 3) {
						break;
					}
					Pegawai pegawai = workspacePunyaPegawai.getPegawai();
					pegs += (pegs.equals("") ? pegawai.getNama() : ", " + pegawai.getNama());
					i++;
				}
			}

			final MyToolbarbuttonConfig a = new MyToolbarbuttonConfig(
					(workspacePunyaPegawais == null ? "0" : workspacePunyaPegawais.size()) + " peg. " + pegs);
			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					initPegawai(workspace, false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
						}
					});

				}
			});

			Common.clear(treecell[3]);
			treecell[3].appendChild(a);
		}
	}

	private void reloadTotalTanggal() {
		for (Workspace workspace : treecellMap.keySet()) {
			Treecell[] treecell = treecellMap.get(workspace);
			Date mulai = workspaceTreeModel.getMinimalMulai(workspace);
			Date selesai = workspaceTreeModel.getMaksimalSelesai(workspace);
			Integer durasi = null;
			if (mulai != null && selesai != null) {
				durasi = ((int) ((selesai.getTime() - mulai.getTime()) / (1000 * 60 * 60 * 24))) + 1;
			}
			if (durasi != null && durasi < 1) {
				durasi = 1;
			}
			// if (durasi != null && durasi > 1) {
			// durasi++;
			// }

			workspace.setDurasi(durasi);
			workspace.setMulai(mulai);
			workspace.setSelesai(selesai);
			Session session = HibernateUtil.currentNativeSession();
			try {
				session.getTransaction().begin();
				Common.refreshUpdate(session, (workspace));
				session.getTransaction().commit();

			} finally {
				HibernateUtil.closeSession();
			}

			treecell[0].setLabel(workspace.getMulai() == null ? "" : Common.dateFormat4.get().format(workspace.getMulai()));

			treecell[1]
					.setLabel(workspace.getSelesai() == null ? "" : Common.dateFormat4.get().format(workspace.getSelesai()));

			treecell[2].setLabel(
					workspace.getDurasi() == null ? "" : Common.numberFormat.get().format(workspace.getDurasi()) + " hr");
		}
	}

	private void reloadTotalPersen() {
		for (Workspace workspace : treecellMap.keySet()) {
			Treecell[] treecell = treecellMap.get(workspace);
			Double persenKomplit = workspaceTreeModel.getPersenKomplit(workspace);

			workspace.setPersenKomplit(persenKomplit);
			Session session = HibernateUtil.currentNativeSession();
			try {
				session.getTransaction().begin();
				Common.refreshUpdate(session, (workspace));
				session.getTransaction().commit();

			} finally {
				HibernateUtil.closeSession();
			}

			treecell[4].setLabel(workspace.getPersenKomplit() == null ? "0 %"
					: Common.numberFormat.get().format(workspace.getPersenKomplit()) + " %");
		}
	}

	@SuppressWarnings("unchecked")
	private void hasSomeChilds(Treerow treerow, final Workspace workspace) {

		Treecell treecell = new Treecell(workspace.toString());
		treecell.setTooltiptext(workspace.toString());
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		Serializable[] serializables = RabUtil.getDetailWorkspace(WorkspacePunyaSasaran.class, "sasaran",
				new String[] { "a1.nama" }, workspace);
		treecell = new Treecell(Common.numberFormat.get().format(serializables[0]));
		treecell.setTooltiptext(serializables[1] + "");
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		serializables = RabUtil.getDetailWorkspace(WorkspacePunyaIndikator.class, "indikator",
				new String[] { "a1.nama", "nilaiTarget", "satuan", "output" }, workspace);
		treecell = new Treecell(Common.numberFormat.get().format(serializables[0]));
		treecell.setTooltiptext(serializables[1] + "");
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		serializables = RabUtil.getDetailWorkspace(WorkspacePunyaPredecessor.class, "workspacePredecessor",
				new String[] { "a1.nama" }, workspace);
		treecell = new Treecell(Common.numberFormat.get().format(serializables[0]));
		treecell.setTooltiptext(serializables[1] + "");
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		Treecell treecellDurasi = new Treecell(
				workspace.getDurasi() == null ? "" : Common.numberFormat.get().format(workspace.getDurasi()) + " hr");
		treecellDurasi.setStyle("font-size:xx-small;text-align: left;");
		treecellDurasi.setParent(treerow);

		Treecell treecellMulai = new Treecell(
				workspace.getMulai() == null ? "" : Common.dateFormat4.get().format(workspace.getMulai()));
		treecellMulai.setStyle("font-size:xx-small;text-align: left;");
		treecellMulai.setParent(treerow);

		Treecell treecellSelesai = new Treecell(
				workspace.getSelesai() == null ? "" : Common.dateFormat4.get().format(workspace.getSelesai()));
		treecellSelesai.setStyle("font-size:xx-small;text-align: left;");
		treecellSelesai.setParent(treerow);

		Treecell treecellPersen = new Treecell(workspace.getPersenKomplit() == null ? "0 %"
				: Common.numberFormat.get().format(workspace.getPersenKomplit()) + " %");
		treecellPersen.setStyle("font-size:xx-small;text-align: right;");
		treecellPersen.setParent(treerow);

		treecell = new Treecell(workspace.getPerulangan());
		treecell.setStyle("font-size:xx-small;font-weight: bolder;text-align: right;");
		treecell.setParent(treerow);

		Session session = HibernateUtil.currentSession();
		List<WorkspacePunyaPegawai> workspacePunyaPegawais = session.createCriteria(WorkspacePunyaPegawai.class)
				.add(Restrictions.eq("workspace", workspace)).list();

		// System.out.println("Peg = " + workspacePunyaPegawais);
		String pegs = "";
		if (workspacePunyaPegawais != null) {
			int i = 0;
			for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
				if (i == 3) {
					break;
				}
				Pegawai pegawai = workspacePunyaPegawai.getPegawai();
				pegs += (pegs.equals("") ? pegawai.getNama() : ", " + pegawai.getNama());
				i++;
			}
		}

		final MyToolbarbuttonConfig a = new MyToolbarbuttonConfig(
				(workspacePunyaPegawais == null ? "0" : workspacePunyaPegawais.size()) + " peg. " + pegs);
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				initPegawai(workspace, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
					}
				});

			}
		});

		Treecell treecellPeg = new Treecell();
		treecellPeg.appendChild(a);
		treecellPeg.setStyle("font-size:xx-small;text-align: left;");
		treecellPeg.setParent(treerow);

		treecellMap.put(workspace,
				new Treecell[] { treecellMulai, treecellSelesai, treecellDurasi, treecellPeg, treecellPersen });

		Double total = workspaceTreeModel.getHargaTotal(workspace);
		if (workspace.getHargaTotal() == null || !workspace.getHargaTotal().equals(total)) {
			workspace.setHargaTotal(total);
			session = HibernateUtil.currentNativeSession();
			try {
				session.getTransaction().begin();
				Common.refreshUpdate(session, (workspace));
				session.getTransaction().commit();

			} finally {
				HibernateUtil.closeSession();
			}
		}

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getHargaTotal()));
		treecell.setAttribute("workspace", workspace);
		treecell.setStyle("font-size:xx-small;font-weight: bolder;text-align: right;");
		treecell.setParent(treerow);

	}

	@SuppressWarnings("unchecked")
	private void noChilds(Treerow treerow, final Workspace workspace) {

		Treecell treecell = new Treecell(workspace.toString());
		treecell.setTooltiptext(workspace.toString());
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		Serializable[] serializables = RabUtil.getDetailWorkspace(WorkspacePunyaSasaran.class, "sasaran",
				new String[] { "a1.nama" }, workspace);
		treecell = new Treecell(Common.numberFormat.get().format(serializables[0]));
		treecell.setTooltiptext(serializables[1] + "");
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		serializables = RabUtil.getDetailWorkspace(WorkspacePunyaIndikator.class, "indikator",
				new String[] { "a1.nama", "nilaiTarget", "satuan", "output" }, workspace);
		treecell = new Treecell(Common.numberFormat.get().format(serializables[0]));
		treecell.setTooltiptext(serializables[1] + "");
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		serializables = RabUtil.getDetailWorkspace(WorkspacePunyaPredecessor.class, "workspacePredecessor",
				new String[] { "a1.nama" }, workspace);
		treecell = new Treecell(Common.numberFormat.get().format(serializables[0]));
		treecell.setTooltiptext(serializables[1] + "");
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		final Intbox intbox = new Intbox(workspace.getDurasi() == null ? 0 : workspace.getDurasi());
		intbox.setStyle("font-size:xx-small;");
		intbox.setWidth("90%");
		treecell = new Treecell();
		treecell.appendChild(new Hbox(new Component[] { intbox, new Label(ais.common.Common.getBahasaConfig(" hr")) }));
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		final MyDatebox datebox = new MyDatebox(workspace.getMulai());
		datebox.setWidth("90%");
		Treecell treecellMulai = new Treecell();
		treecellMulai.appendChild(datebox);
		datebox.setStyle("font-size:xx-small;");
		treecellMulai.setStyle("font-size:xx-small;text-align: left;");
		treecellMulai.setParent(treerow);

		final MyDatebox datebox1 = new MyDatebox(workspace.getSelesai());
		datebox1.setWidth("90%");
		Treecell treecellSelesai = new Treecell();
		treecellSelesai.appendChild(datebox1);
		datebox1.setStyle("font-size:xx-small;");
		treecellSelesai.setStyle("font-size:xx-small;text-align: left;");
		treecellSelesai.setParent(treerow);

		final MyDoublebox persen = new MyDoublebox(
				workspace.getPersenKomplit() == null ? 0.0 : workspace.getPersenKomplit());
		persen.setStyle("font-size:xx-small;");
		persen.setWidth("90%");
		treecell = new Treecell();
		treecell.appendChild(new Hbox(new Component[] { persen, new Label(" %") }));
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		persen.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (persen.getValue() == null || persen.getValue() < 0 || persen.getValue() > 100) {
					persen.setValue(workspace.getPersenKomplit());
					MyMessageboxConfig.show("Nilai persen harus antara 0 s.d 100", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				workspace.setPersenKomplit(persen.getValue());
				Session session = HibernateUtil.currentNativeSession();
				try {

					session.getTransaction().begin();
					Common.refreshUpdate(session, (workspace));
					session.getTransaction().commit();

				} finally {
					HibernateUtil.closeSession();
				}

				workspaceTreeModel.ubahKomplitParents(workspace);
				reloadTotalPersen();
			}
		});

		datebox1.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (datebox1.getValue() == null) {
					MyMessageboxConfig.show("Tanggal selesai harus terisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (datebox.getValue() == null) {
					datebox1.setValue(workspace.getSelesai());
					MyMessageboxConfig.show(
							"Sebelum tanggal selesai pengerjaan diisi, terlebih dahulu harap mengisi tanggal mulai",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Date tglMulai = datebox.getValue();
				Date tglSelesai = datebox1.getValue();

				WorkspacePunyaPegawai workspaceBentrok = workspaceTreeModel.checkBentrok(workspace, tglMulai,
						tglSelesai);
				if (workspaceBentrok != null) {
					String message = "\n\nPegawai : " + workspaceBentrok.getPegawai().getNama() + ", workspace : "
							+ workspaceBentrok.getWorkspace().getNama() + ", mulai : "
							+ (workspaceBentrok.getWorkspace().getMulai() == null ? ""
									: Common.dateFormat1.get().format(workspaceBentrok.getWorkspace().getMulai()))
							+ ", selesai : " + (workspaceBentrok.getWorkspace().getSelesai() == null ? ""
									: Common.dateFormat1.get().format(workspaceBentrok.getWorkspace().getSelesai()));
					MyMessageboxConfig.show("Workspace Bentrok:" + message, "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					datebox1.setValue(workspace.getSelesai());
					return;
				}

				int durasi = ((int) ((tglSelesai.getTime() - tglMulai.getTime()) / (1000 * 60 * 60 * 24))) + 1;
				if (durasi < 1) {
					datebox1.setValue(workspace.getSelesai());
					MyMessageboxConfig.show("Tanggal selesai tidak boleh lebih awal daripada tanggal mulai",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				// durasi++;

				// if (durasi == 0) {
				// durasi = 1;
				// }

				intbox.setValue(durasi);

				workspace.setSelesai(datebox1.getValue());
				workspace.setDurasi(durasi);
				Session session = HibernateUtil.currentNativeSession();
				try {

					session.getTransaction().begin();
					Common.refreshUpdate(session, (workspace));
					session.getTransaction().commit();

				} finally {
					HibernateUtil.closeSession();
				}

				workspaceTreeModel.ubahSelesaiParents(workspace);
				workspaceTreeModel.ubahDurasiParents(workspace);
				reloadTotalTanggal();
			}
		});

		datebox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (datebox.getValue() == null) {

					MyMessageboxConfig.show("Apakah yakin ingin meng-kosong-kan jadwal data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										workspace.setMulai(null);
										workspace.setSelesai(null);
										workspace.setDurasi(0);

										Session session = HibernateUtil.currentNativeSession();
										try {

											session.getTransaction().begin();
											Common.refreshUpdate(workspace);
											session.getTransaction().commit();

										} finally {
											HibernateUtil.closeSession();
										}

										datebox1.setValue(null);
										intbox.setValue(0);
									} else {
										datebox.setValue(workspace.getMulai());
									}
								}
							});

					return;
				}

				if (intbox.getValue() == null) {
					return;
				}

				Integer durasi = intbox.getValue() == null ? 0 : intbox.getValue();
				if (durasi.equals(0)) {
					intbox.setValue(1);
				}

				Date tglMulai = datebox.getValue();
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(tglMulai);
				calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + ((durasi > 0) ? (durasi - 1) : durasi));
				Date tglSelesai = calendar.getTime();

				WorkspacePunyaPegawai workspaceBentrok = workspaceTreeModel.checkBentrok(workspace, tglMulai,
						tglSelesai);
				if (workspaceBentrok != null) {
					String message = "\n\nPegawai : " + workspaceBentrok.getPegawai().getNama() + ", workspace : "
							+ workspaceBentrok.getWorkspace().getNama() + ", mulai : "
							+ (workspaceBentrok.getWorkspace().getMulai() == null ? ""
									: Common.dateFormat1.get().format(workspaceBentrok.getWorkspace().getMulai()))
							+ ", selesai : " + (workspaceBentrok.getWorkspace().getSelesai() == null ? ""
									: Common.dateFormat1.get().format(workspaceBentrok.getWorkspace().getSelesai()));
					MyMessageboxConfig.show("Workspace Bentrok:" + message, "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					datebox.setValue(workspace.getMulai());
					return;
				}

				workspace.setMulai(tglMulai);
				workspace.setSelesai(tglSelesai);
				workspace.setDurasi(durasi);

				Session session = HibernateUtil.currentNativeSession();
				try {

					session.getTransaction().begin();
					Common.refreshUpdate(session, (workspace));
					session.getTransaction().commit();

				} finally {
					HibernateUtil.closeSession();
				}

				datebox1.setValue(tglSelesai);
				workspaceTreeModel.ubahMulaiParents(workspace);
				workspaceTreeModel.ubahSelesaiParents(workspace);
				workspaceTreeModel.ubahDurasiParents(workspace);
				reloadTotalTanggal();
			}
		});

		intbox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (intbox.getValue() == null) {
					MyMessageboxConfig.show("Durasi harus terisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (intbox.getValue() < 0) {
					MyMessageboxConfig.show("Durasi tidak boleh minus", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (datebox.getValue() == null) {
					intbox.setValue(workspace.getDurasi());
					MyMessageboxConfig.show(
							"Sebelum durasi pengerjaan diisi, terlebih dahulu harap mengisi tanggal mulai",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Integer durasi = intbox.getValue();
				if (durasi < 1) {
					durasi = 1;
					intbox.setValue(1);
				}

				Date tglMulai = datebox.getValue();
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(tglMulai);
				calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + ((durasi > 0) ? (durasi - 1) : durasi));
				Date tglSelesai = calendar.getTime();

				WorkspacePunyaPegawai workspaceBentrok = workspaceTreeModel.checkBentrok(workspace, tglMulai,
						tglSelesai);
				if (workspaceBentrok != null) {
					String message = "\n\nPegawai : " + workspaceBentrok.getPegawai().getNama() + ", workspace : "
							+ workspaceBentrok.getWorkspace().getNama() + ", mulai : "
							+ (workspaceBentrok.getWorkspace().getMulai() == null ? ""
									: Common.dateFormat1.get().format(workspaceBentrok.getWorkspace().getMulai()))
							+ ", selesai : " + (workspaceBentrok.getWorkspace().getSelesai() == null ? ""
									: Common.dateFormat1.get().format(workspaceBentrok.getWorkspace().getSelesai()));
					MyMessageboxConfig.show("Workspace Bentrok:" + message, "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					intbox.setValue(workspace.getDurasi());
					return;
				}

				workspace.setMulai(tglMulai);
				workspace.setSelesai(tglSelesai);
				workspace.setDurasi(durasi);

				Session session = HibernateUtil.currentNativeSession();
				try {

					session.getTransaction().begin();
					Common.refreshUpdate(session, (workspace));
					session.getTransaction().commit();

				} finally {
					HibernateUtil.closeSession();
				}

				datebox1.setValue(tglSelesai);
				workspaceTreeModel.ubahSelesaiParents(workspace);
				workspaceTreeModel.ubahDurasiParents(workspace);
				reloadTotalTanggal();
			}
		});

		final Combobox perulangan = new Combobox();
		perulangan.setWidth("90%");
		perulangan.setStyle("font-size:xx-small;text-align: left;");
		MyComboitemConfig comboitem = new MyComboitemConfig(Workspace.BULANAN);
		comboitem.setValue(Workspace.BULANAN);
		perulangan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Workspace.MINGGUAN);
		comboitem.setValue(Workspace.MINGGUAN);
		perulangan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Workspace.HARIAN);
		comboitem.setValue(Workspace.HARIAN);
		perulangan.appendChild(comboitem);

		Common.selectComboItem(perulangan, workspace.getPerulangan());
		treecell = new Treecell();
		treecell.appendChild(perulangan);
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);
		perulangan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				workspace.setPerulangan((String) (perulangan.getSelectedItem() == null ? null
						: perulangan.getSelectedItem().getValue()));
				Common.refreshUpdate(session, (workspace));
			}
		});

		Session session = HibernateUtil.currentSession();
		List<WorkspacePunyaPegawai> workspacePunyaPegawais = session.createCriteria(WorkspacePunyaPegawai.class)
				.add(Restrictions.eq("workspace", workspace)).list();

		// System.out.println("Peg = " + workspacePunyaPegawais);
		String pegs = "";
		if (workspacePunyaPegawais != null) {
			int i = 0;
			for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
				if (i == 3) {
					break;
				}
				Pegawai pegawai = workspacePunyaPegawai.getPegawai();
				pegs += (pegs.equals("") ? pegawai.getNama() : ", " + pegawai.getNama());
				i++;
			}
		}

		final Treecell totalBiayaTreecell = new Treecell();

		final MyToolbarbuttonConfig a = new MyToolbarbuttonConfig(
				(workspacePunyaPegawais == null ? "0" : workspacePunyaPegawais.size()) + " peg. " + pegs);
		a.setStyle("font-size:xx-small;");
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				initPegawai(workspace, edit, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Session session = HibernateUtil.currentNativeSession();
						try {

							List<WorkspacePunyaPegawai> workspacePunyaPegawais = session
									.createCriteria(WorkspacePunyaPegawai.class)
									.add(Restrictions.eq("workspace", workspace)).list();

							// System.out.println("Peg = " +
							// workspacePunyaPegawais);
							String pegs = "";
							if (workspacePunyaPegawais != null) {
								int i = 0;
								for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
									if (i == 3) {
										break;
									}
									Pegawai pegawai = workspacePunyaPegawai.getPegawai();
									pegs += (pegs.equals("") ? pegawai.getNama() : ", " + pegawai.getNama());
									i++;
								}
							}

							a.setLabel((workspacePunyaPegawais == null ? "0" : workspacePunyaPegawais.size()) + " peg. "
									+ pegs);

						} finally {
							HibernateUtil.closeSession();
						}
						workspaceTreeModel.ubahPegawaisParents(workspace);
						reloadTotalPegawai();
						Double biaya = workspaceTreeModel.getHargaTotal(workspace);
						totalBiayaTreecell.setLabel(Common.numberFormat.get().format(biaya));
					}
				});

			}
		});
		treecell = new Treecell();
		treecell.appendChild(a);
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		Double biaya = workspaceTreeModel.getHargaTotal(workspace);
		totalBiayaTreecell.setLabel(Common.numberFormat.get().format(biaya));
		totalBiayaTreecell.setStyle("font-size:xx-small;font-weight: bolder;text-align: right;");
		totalBiayaTreecell.setParent(treerow);
	}

	public void onSearchDefault(Event event) throws Exception {

		if (selectedTahun == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		onReloadTree(event);
	}

	private class PegawaiDetail {

		private MyGrid grid;
		private Workspace workspace;
		private EventListener eventListener;
		private MyWindow window;
		private Boolean editable = true;

		public PegawaiDetail(Workspace workspace, MyWindow window, Boolean editable,
				final EventListener eventListener) {
			this.workspace = workspace;
			this.window = window;
			this.editable = editable;
			this.eventListener = eventListener;
		}

		@SuppressWarnings("unchecked")
		public Borderlayout init() {

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			South south = new South();
			south.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(south, true);

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("Kode/NIP");
			column.setParent(columns);
			column.setWidth("25%");

			column = new MyColumnConfig("Nama");
			column.setParent(columns);

			column = new MyColumnConfig("");
			column.setParent(columns);
			column.setWidth("5%");

			final Rows rows = new Rows();
			rows.setParent(grid);

			Session session = HibernateUtil.currentSession();
			List<WorkspacePunyaPegawai> workspacePunyaPegawais = session.createCriteria(WorkspacePunyaPegawai.class)
					.add(Restrictions.eq("workspace", workspace)).list();

			if (workspacePunyaPegawais != null) {

				for (final WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {

					Pegawai pegawai = workspacePunyaPegawai.getPegawai();

					final MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setValign("top");
					row.setAttribute("workspacePunyaPegawai", workspacePunyaPegawai);
					row.setParent(rows);

					row.appendChild(new ais.ui.util.MyLabelConfig(pegawai.getCode()));
					row.appendChild(new ais.ui.util.MyLabelConfig(pegawai.getNama()));

					Hbox hbox = new Hbox();
					hbox.setParent(row);

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
					button.setTooltiptext("Hapus Data");
					button.setVisible(delete && editable);
					button.setParent(hbox);

					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							row.setVisible(false);
						}
					});
				}

			}

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("30px");
			toolbar.setParent(north);

			MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Ambil Data Pegawai", "/img/new.gif");
			add.setVisible(edit && editable);
			add.setParent(toolbar);
			add.setTooltiptext("Tambah");
			add.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					List<Row> myrows = grid.getRows().getChildren();

					List<Pegawai> pegawais = new ArrayList<Pegawai>();
					for (Row row : myrows) {
						WorkspacePunyaPegawai workspacePunyaPegawai = (WorkspacePunyaPegawai) row
								.getAttribute("workspacePunyaPegawai");
						Pegawai pegawai = workspacePunyaPegawai.getPegawai();
						pegawais.add(pegawai);
					}

					AmbilDataPegawaiBanyak window = new AmbilDataPegawaiBanyak(pegawais);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
					window.setWidth("700px");
					window.setHeight("90%");

					window.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Pegawai> pegawais = (List<Pegawai>) arg0.getData();
							if (pegawais != null) {

								for (Pegawai pegawai : pegawais) {

									final WorkspacePunyaPegawai workspacePunyaPegawai = new WorkspacePunyaPegawai();
									workspacePunyaPegawai.setPegawai(pegawai);
									workspacePunyaPegawai.setWorkspace(workspace);

									final MyFormRow row = new MyFormRow();
									row.setValign("top");
									row.setValign("top");
									row.setAttribute("workspacePunyaPegawai", workspacePunyaPegawai);
									row.setParent(rows);

									row.appendChild(new ais.ui.util.MyLabelConfig(pegawai.getCode()));
									row.appendChild(new ais.ui.util.MyLabelConfig(pegawai.getNama()));

									Hbox hbox = new Hbox();
									hbox.setParent(row);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
									button.setTooltiptext("Hapus Data");
									button.setVisible(delete && editable);
									button.setParent(hbox);

									button.addEventListener("onClick", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											row.setVisible(false);
											row.detach();
										}
									});
								}

							}
						}
					});

					window.onModal();

				}

			});

			toolbar = new Toolbar();
			toolbar.setHeight("30px");
			toolbar.setParent(south);

			MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			simpan.setVisible(edit && editable);
			simpan.setParent(toolbar);
			simpan.setTooltiptext("Simpan");

			simpan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					List<Row> rows = grid.getRows().getChildren();

					String message = "";
					for (Row row : rows) {
						WorkspacePunyaPegawai workspacePunyaPegawai = (WorkspacePunyaPegawai) row
								.getAttribute("workspacePunyaPegawai");
						WorkspacePunyaPegawai workspaceBentrok = workspaceTreeModel.checkBentrok(workspacePunyaPegawai);
						if (workspaceBentrok != null) {
							message += "\n\nPegawai : " + workspaceBentrok.getPegawai().getNama() + ", workspace : "
									+ workspaceBentrok.getWorkspace().getNama() + ", mulai : "
									+ (workspaceBentrok.getWorkspace().getMulai() == null ? ""
											: Common.dateFormat1.get().format(workspaceBentrok.getWorkspace().getMulai()))
									+ ", selesai : " + (workspaceBentrok.getWorkspace().getSelesai() == null ? ""
											: Common.dateFormat1.get().format(workspaceBentrok.getWorkspace().getSelesai()));
						}
					}

					if (!message.equals("")) {
						MyMessageboxConfig.show("Workspace Bentrok:" + message, "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						return;
					}
					Session session = HibernateUtil.currentNativeSession();
					try {

						session.getTransaction().begin();
						session.createSQLQuery("delete from rab.workspace_punya_pegawai where workspace = :workspaceId")
								.setLong("workspaceId", workspace.getId()).executeUpdate();
						session.getTransaction().commit();
						session.flush();

						session.getTransaction().begin();
						for (Row row : rows) {
							WorkspacePunyaPegawai workspacePunyaPegawai = (WorkspacePunyaPegawai) row
									.getAttribute("workspacePunyaPegawai");
							WorkspacePunyaPegawai newWorkspacePunyaPegawai = (WorkspacePunyaPegawai) workspacePunyaPegawai
									.clone();
							newWorkspacePunyaPegawai.setId(null);
							session.save(newWorkspacePunyaPegawai);
						}
						session.getTransaction().commit();

					} finally {
						HibernateUtil.closeSession();
					}

					eventListener.onEvent(arg0);

					window.detach();
				}
			});

			MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			batal.setParent(toolbar);
			batal.setTooltiptext("Batal");
			batal.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					eventListener.onEvent(arg0);
					window.detach();
				}
			});

			return borderlayout;
		}
	}

	public void initPegawai(Workspace workspace, Boolean editable, final EventListener eventListener) throws Exception {
		MyWindow window = new MyWindow("Data Pegawai", "none", true);
		page.getFirstRoot().appendChild(window);
		window.setHeight("95%");
		window.setWidth("850px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("90%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(new ais.ui.util.MyLabelConfig(workspace.getKode() == null ? "" : workspace.getKode()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(new ais.ui.util.MyLabelConfig(workspace.getNama() == null ? ""
				: workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
						: " - " + workspace.getUnitOrganisasi().getNama())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Anggaran"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				workspace.getHargaTotal() == null ? "" : Common.numberFormat.get().format(workspace.getHargaTotal())));

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		center.appendChild(new PegawaiDetail(workspace, window, editable, eventListener).init());

		window.onModal();
	}

}
