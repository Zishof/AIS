package ais.action.master.rab;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import ais.action.master.akunting.util.AkunTreeModel;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataSumberDanaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyMessageboxConfig;

/**
 * Halaman <b>Realisasi Anggaran per Akun</b> (kode rekening).
 *
 * <p>Menampilkan pohon akun beserta nilai realisasi/penyerapan anggaran untuk Tahun, Satuan Kerja,
 * Sumber Dana, dan Revisi terpilih - dihitung melalui {@link WorkspaceTreeModel}. Berguna untuk
 * melihat seberapa besar anggaran pada tiap akun sudah terpakai dibanding pagunya.</p>
 *
 * <h3>Higiene session basis data</h3>
 * Membaca lewat session ThreadLocal ({@code currentSession()}) yang ditutup OTOMATIS; tidak ada
 * {@code openSession()}/{@code currentNativeSession()} yang perlu ditutup manual di kelas ini.
 *
 * <h3>Catatan teknis</h3>
 * Kompatibel Java 1.7 dan ZK 5.5 ({@code try/catch} gaya Java 1.6). Memaksimalkan pemakaian ulang
 * {@link WorkspaceTreeModel} (perhitungan realisasi terpusat) agar konsisten dan mudah dirawat.
 */
public class RealisasiAkunAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Tree tree;

	private Combobox tahunAkun;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataSumberDanaBanbox sumberDana;
	private Label sumberDanaLabel;
	private Set<SatuanKerja> satuanKerjas;

	private AkunTreeModel akunTreeModel;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
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

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

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
		Common.insertComboItems(tahunAkun, "", tahuns);
		Common.selectComboItem(tahunAkun, tahun);

		initTree();

		onSearchDefault(null);

	}

	private void initTree() throws Exception {

		if (tahunAkun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Item Akun");
		treecol.setWidth("75%");
		treecol.setParent(treecols);

		treecol = new Treecol("Total Realisasi");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	public void onReloadTree(Event event) throws Exception {

		if (tahunAkun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			return;
		}

		sumberDana.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"),
				(Integer) (tahunAkun.getSelectedItem() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
						: tahunAkun.getSelectedItem().getValue()));

		sumberDanaLabel.setVisible(sumberDana.isVisible());

		SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
		SumberDana sumberDana = (SumberDana) this.sumberDana.getAttribute("sumberDana");

		this.satuanKerjas = new HashSet<SatuanKerja>();
		this.satuanKerjas.add(satuanKerja);
		satuanKerjaTreeModel.generateAllChildren(satuanKerja, satuanKerjas);

		revisi = (Integer) HibernateUtil.currentSession().createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", tahunAkun.getSelectedItem().getValue()))
				.setProjection(Projections.max("revisi")).uniqueResult();
		revisi = revisi == null ? -1 : revisi;

		akunTreeModel = new AkunTreeModel();

		tree.setModel(akunTreeModel);

		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final Akun akun = (Akun) arg1;

				try {
					final Treerow treerow = treeitem.getTreerow() == null ? new Treerow() : treeitem.getTreerow();
					treerow.setParent(treeitem);
					Common.clear(treerow);

					noChildNotEnabled(treerow, akun);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});
	}

	private void noChildNotEnabled(Treerow treerow, final Akun akun) {
		Treecell treecell = new Treecell(akun.toString());
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		SumberDana sumberDana = (SumberDana) this.sumberDana.getAttribute("sumberDana");

		Integer tahunAkun = (Integer) this.tahunAkun.getSelectedItem().getValue();
		Set<Akun> akuns = new HashSet<Akun>();
		akuns.add(akun);
		akunTreeModel.generateAllChildren(akun, akuns);
		Double realisasi = WorkspaceTreeModel.getRealisasi(tahunAkun, satuanKerjas, sumberDana, revisi, akuns, akun);
		treecell = new Treecell(realisasi == null ? "0" : Common.numberFormat.get().format(realisasi));
		treecell.setStyle("font-size:xx-small;text-align: right;");
		treecell.setParent(treerow);
		akuns = null;
	}

	public void onSearchDefault(Event event) throws Exception {

		if (tahunAkun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		onReloadTree(event);
	}

}
