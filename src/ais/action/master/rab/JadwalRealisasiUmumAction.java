package ais.action.master.rab;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataSumberDanaBanbox;
import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.rab.JenisWorkspace;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaPegawai;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk jadwal realisasi umum. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tree tree}, {@code Combobox
 * tahunWorkspace}, {@code AmbilDataSatuanKerjaBanbox satuanKerja}, {@code AmbilDataSumberDanaBanbox sumberDana},
 * {@code Label sumberDanaLabel}, {@code WorkspaceTreeModel workspaceTreeModel}, {@code Integer revisi};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initTree()}, {@code
 * initPegawai()}); pembacaan/pencarian ({@code onReloadTree()}, {@code onSearchDefault()}); operasi domain lain
 * ({@code noChildNotEnabled()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class JadwalRealisasiUmumAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Tree tree;

	private Combobox tahunWorkspace;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataSumberDanaBanbox sumberDana;
	private Label sumberDanaLabel;

	private WorkspaceTreeModel workspaceTreeModel;

	// private TreeMap<Workspace, Treecell[]> treecellMap = new
	// TreeMap<Workspace, Treecell[]>();

	private Integer revisi = 1;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		// Themes.setTheme(execution, "silvertail");

		this.satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(arg0);
			}
		});

		this.sumberDana.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(arg0);
			}
		});

		Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		List<Integer> tahuns = new ArrayList<Integer>();
		for (int i = tahun + 5; i > (tahun - 20); i--) {
			tahuns.add(i);
		}
		Common.insertComboItems(tahunWorkspace, "", tahuns);
		Common.selectComboItem(tahunWorkspace, tahun);

		initTree();

		onSearchDefault(null);

	}

	private void initTree() throws Exception {

		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Item");
		treecol.setWidth("41%");
		treecol.setParent(treecols);

		treecol = new Treecol("Durasi");
		treecol.setParent(treecols);
		treecol.setWidth("8%");

		treecol = new Treecol("Mulai");
		treecol.setParent(treecols);

		treecol = new Treecol("Selesai");
		treecol.setParent(treecols);

		treecol = new Treecol("Ulang");
		treecol.setParent(treecols);
		treecol.setWidth("8%");

		treecol = new Treecol("Dikerjakan");
		treecol.setParent(treecols);
		treecol.setWidth("15%");

		treecol = new Treecol("Anggaran");
		treecol.setParent(treecols);

		// treecol = new Treecol("");
		// treecol.setWidth("10%");
		// treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	public void onReloadTree(Event event) throws Exception {

		// tree.setModel(new TreeModel() {
		//
		// @Override
		// public void removeTreeDataListener(TreeDataListener arg0) {
		// // TODO Auto-generated method stub
		//
		// }
		//
		// @Override
		// public boolean isLeaf(Object arg0) {
		// // TODO Auto-generated method stub
		// return false;
		// }
		//
		// @Override
		// public Object getRoot() {
		// // TODO Auto-generated method stub
		// return null;
		// }
		//
		// @Override
		// public int[] getPath(Object arg0, Object arg1) {
		// // TODO Auto-generated method stub
		// return null;
		// }
		//
		// @Override
		// public int getIndexOfChild(Object arg0, Object arg1) {
		// // TODO Auto-generated method stub
		// return 0;
		// }
		//
		// @Override
		// public int getChildCount(Object arg0) {
		// // TODO Auto-generated method stub
		// return 0;
		// }
		//
		// @Override
		// public Object getChild(Object arg0, int arg1) {
		// // TODO Auto-generated method stub
		// return null;
		// }
		//
		// @Override
		// public void addTreeDataListener(TreeDataListener arg0) {
		// // TODO Auto-generated method stub
		//
		// }
		// });

		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			return;
		}

		sumberDana.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"),
				(Integer) (tahunWorkspace.getSelectedItem() == null
						? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
						: tahunWorkspace.getSelectedItem().getValue()));

		sumberDanaLabel.setVisible(sumberDana.isVisible());

		SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
		SumberDana sumberDana = (SumberDana) this.sumberDana.getAttribute("sumberDana");

		revisi = (Integer) HibernateUtil.currentSession().createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", tahunWorkspace.getSelectedItem().getValue()))
				.setProjection(Projections.max("revisi")).uniqueResult();
		revisi = revisi == null ? -1 : revisi;

		// System.out.println("revisi = " + revisi);

		workspaceTreeModel = new WorkspaceTreeModel((Integer) tahunWorkspace.getSelectedItem().getValue(), revisi,
				satuanKerja, sumberDana);

		if (workspaceTreeModel.getSatuanKerjas().size() == 1 && sumberDana == null) {
			MyMessageboxConfig.show("Sumber Dana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		tree.setModel(workspaceTreeModel);

		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final Workspace workspace = (Workspace) arg1;

				try {
					final Treerow treerow = treeitem.getTreerow() == null ? new Treerow() : treeitem.getTreerow();
					treerow.setParent(treeitem);
					Common.clear(treerow);

					if (workspace.getJenisWorkspace() != null) {
						JenisWorkspace jenisWorkspace = workspace.getJenisWorkspace();
						treerow.setStyle((jenisWorkspace.getWarna() != null
								? "background-color:" + jenisWorkspace.getWarna() + ";"
								: "")
								+ (jenisWorkspace.getWarnaText() != null
										? "color:" + jenisWorkspace.getWarnaText() + ";"
										: ""));
					}

					noChildNotEnabled(treerow, workspace);

					// Treecell arg0 = new Treecell();
					// arg0.setParent(treerow);
					// Hbox toolbar = new Hbox();
					// toolbar.setParent(arg0);
					//
					// MyToolbarbuttonConfig button = new
					// MyToolbarbuttonConfig("Detail",
					// "/img/svg/search.svg");
					//
					// button.setParent(toolbar);

					// shopping_cart1.png

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});
	}

	@SuppressWarnings("unchecked")
	private void noChildNotEnabled(Treerow treerow, final Workspace workspace) {
		Treecell treecell = new Treecell(workspace.toString());
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

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getHargaTotal()));
		treecell.setAttribute("workspace", workspace);
		treecell.setStyle("font-size:xx-small;font-weight: bolder;text-align: right;");
		treecell.setParent(treerow);

	}

	public void onSearchDefault(Event event) throws Exception {

		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		onReloadTree(event);
	}

	public void initPegawai(Workspace workspace, Boolean editable, final EventListener eventListener) throws Exception {
		MyWindow window = new MyWindow("Data Pegawai", "none", true);
		page.getFirstRoot().appendChild(window);
		window.setHeight("95%");
		window.setWidth("850px");
		window.setClosable(true);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

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

	private class PegawaiDetail {

		private MyGrid grid;
		private Workspace workspace;

		public PegawaiDetail(Workspace workspace, MyWindow window, Boolean editable,
				final EventListener eventListener) {
			this.workspace = workspace;
		}

		@SuppressWarnings("unchecked")
		public Borderlayout init() {

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

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

				}

			}

			return borderlayout;
		}
	}

}
