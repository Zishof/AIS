package ais.action.master.rab;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
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
import ais.database.model.rab.JenisWorkspace;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyMessageboxConfig;

/**
 * Halaman <b>Realisasi Anggaran (Umum)</b>.
 *
 * <p>Menampilkan pohon item anggaran (lewat {@link WorkspaceTreeModel}) beserta nilai realisasinya
 * untuk Tahun, Satuan Kerja, Sumber Dana, dan Revisi terpilih - pandangan menyeluruh atas penyerapan
 * anggaran satu satuan kerja.</p>
 *
 * <h3>Higiene session basis data</h3>
 * Memakai session ThreadLocal ({@code currentSession()}) yang ditutup OTOMATIS; tidak ada
 * {@code openSession()}/{@code currentNativeSession()} yang perlu ditutup manual di kelas ini.
 *
 * <h3>Catatan teknis</h3>
 * Kompatibel Java 1.7 dan ZK 5.5 ({@code try/catch} gaya Java 1.6); memakai ulang
 * {@link WorkspaceTreeModel} untuk perhitungan realisasi.
 */
public class RealisasiUmumAction extends GenericAutowireComposer {

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

	/** Wadah visual ringkasan Anggaran vs Realisasi (HTML/CSS), di-autowire dari id "ringkasanRab" di .zul. */
	private org.zkoss.zul.Div ringkasanRab;

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
		Treecol treecol = new Treecol("Item Anggaran");
		treecol.setWidth("55%");
		treecol.setParent(treecols);

		treecol = new Treecol("Anggaran");
		treecol.setParent(treecols);

		treecol = new Treecol("Realisasi");
		treecol.setParent(treecols);

		treecol = new Treecol("Sisa");
		treecol.setParent(treecols);

		treecol = new Treecol("%");
		treecol.setParent(treecols);
		treecol.setWidth("4%");

		// treecol = new Treecol("");
		// treecol.setWidth("10%");
		// treecol.setParent(treecols);

		treecols.setParent(tree);

		// Tampilkan placeholder ringkasan sebelum pencarian pertama agar area tidak kosong.
		ais.action.master.rab.helper.RabRingkasanVisualHelper.render(ringkasanRab, null);
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

		// Visual ringkasan Anggaran vs Realisasi (HTML/CSS murni, dapat dipakai ulang). Dibungkus aman
		// di dalam helper sehingga bila gagal, render pohon utama tetap berjalan.
		ais.action.master.rab.helper.RabRingkasanVisualHelper.render(ringkasanRab, workspaceTreeModel);

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

	private void noChildNotEnabled(Treerow treerow, final Workspace workspace) {
		Treecell treecell = new Treecell(workspace.toString());
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		treecell = new Treecell(
				Common.numberFormat.get().format(workspace.getHargaTotal() == null ? 0.0 : workspace.getHargaTotal()));

		treecell.setStyle("font-size:xx-small;text-align: right;");
		treecell.setParent(treerow);

		Double realisasi = workspace.getRealisasiProses();
		Treecell treecellNilai = new Treecell(Common.numberFormat.get().format(realisasi));
		treecellNilai.setStyle("font-size:xx-small;color:blue;font-weight: bolder;text-align: right;");
		treecellNilai.setParent(treerow);

		Double persen = (realisasi * 100.0) / workspace.getHargaTotal();
		Double sisa = workspace.getHargaTotal() - realisasi;

		Treecell treecellSisa = new Treecell(Common.numberFormat.get().format(sisa));
		treecellSisa.setStyle("font-size:xx-small;color:red;font-weight: bolder;text-align: right;");
		treecellSisa.setParent(treerow);

		Treecell treecellPersen = new Treecell(Common.numberFormat.get().format(persen) + " %");
		treecellPersen.setStyle("font-size:xx-small;color:red;font-weight: normal;text-align: right;");
		treecellPersen.setParent(treerow);

	}

	public void onSearchDefault(Event event) throws Exception {

		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		onReloadTree(event);
	}

}
