package ais.action.master.rab.helper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyCheckboxConfig;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

import ais.action.master.rab.util.Pemilih;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

/**
 * Helper UI untuk dialog "Pilih Kegiatan": menampilkan pohon {@link Workspace} (kegiatan RAB)
 * satu tahun anggaran sebagai {@link Tree} ZK, memungkinkan pemilihan banyak workspace lewat
 * checkbox per baris (opsional dibatasi hanya pada node daun bila {@code tampilRootSelect=false}),
 * dan opsional menampilkan kolom realisasi anggaran serta pemilih tahun workspace. Hasil pilihan
 * diserahkan ke pemanggil lewat kontrak {@link Pemilih#pilih(String, java.util.List)} saat tombol
 * terapkan diklik. Pohon dibangun secara rekursif dari daftar datar {@link Workspace} berdasarkan
 * relasi {@code parentId} (akar ditandai {@link #ROOT}), dengan cache {@code selectedTahun} agar
 * pohon tidak dibangun ulang bila tahun belum berubah.
 */
public class PilihKegiatanHelper {

	private static Long ROOT = -1L;

	private MyWindow window;
	private Tree workspaceTree;
	private List<Workspace> workspaces;
	private Integer tahun;
	private Integer selectedTahun = null;
	private Pemilih pemilih;
	private Textbox judulKegiatan = new Textbox("Pilihan Kegiatan");
	private Treeitem selectestreeitem;
	private Menupopup menupopup;
	private Boolean tampilRootSelect = true;

	private NumberFormat numberFormat = NumberFormat.getNumberInstance(Common.locale);

	private List<Workspace> selectedWorspaces = new ArrayList<Workspace>();

	private boolean tampilRealisasi = false;
	private boolean tampilTahun = false;
	private Combobox tahunWorkspace;

	/**
	 * @param tampilRealisasi  tampilkan kolom jumlah realisasi dan persentase realisasi terhadap anggaran
	 * @param tampilRootSelect izinkan checkbox pilih pada node non-daun (bila {@code false}, hanya node daun yang dapat dipilih)
	 * @param tampilTahun      tampilkan kombo pemilih tahun workspace (rentang tahun berjalan+5 s.d. tahun berjalan-20)
	 */
	public PilihKegiatanHelper(boolean tampilRealisasi, Boolean tampilRootSelect, Boolean tampilTahun) {
		this.tampilRealisasi = tampilRealisasi;
		this.tampilRootSelect = tampilRootSelect;
		this.tampilTahun = tampilTahun;
		numberFormat.setMaximumFractionDigits(2);

		if (tampilTahun) {
			Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
			List<Integer> tahuns = new ArrayList<Integer>();
			for (int i = tahun + 5; i > (tahun - 20); i--) {
				tahuns.add(i);
			}
			Common.insertComboItems(tahunWorkspace = new Combobox(), "", tahuns);
			Common.selectComboItem(tahunWorkspace, tahun);
		}
	}

	/**
	 * Menampilkan dialog pilih kegiatan sebagai modal pada {@code window} yang diberikan.
	 *
	 * @param window jendela yang akan diisi dan ditampilkan sebagai modal
	 * @param tahun  tahun workspace awal; {@code null} berarti tahun berjalan
	 * @param pemilih callback yang menerima hasil pilihan saat tombol terapkan diklik
	 */
	public void display(MyWindow window, Integer tahun, Pemilih pemilih) {
		this.window = window;
		if (tahun != null) {
			this.tahun = tahun;
		} else {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
			this.tahun = tahun;
			Common.selectComboItem(tahunWorkspace, tahun);
		}
		this.pemilih = pemilih;
		init();
	}

	/** Membuka/menutup ({@code open}) seluruh {@link MyTreeitemConfig} secara rekursif di bawah {@code component}. */
	private void setOpen(boolean open, Component component) {
		if (component instanceof MyTreeitemConfig) {
			MyTreeitemConfig treeitem = (MyTreeitemConfig) component;
			treeitem.setOpen(open);
		}
		for (Object object : component.getChildren()) {
			if (object instanceof MyTreeitemConfig) {
				MyTreeitemConfig treeitem = (MyTreeitemConfig) object;
				treeitem.setOpen(open);
			}

			if (((Component) object).getChildren().size() != 0) {
				setOpen(open, ((Component) object));
			}

		}
	}

	/** Membuka (mengembangkan) seluruh sub-node dari node workspace yang sedang dipilih di pohon. */
	public void onTampilItem(Event event) {
		selectestreeitem = workspaceTree.getSelectedItem();
		if (selectestreeitem != null) {
			setOpen(true, selectestreeitem);
		}
	}

	/** Menutup (melipat) seluruh sub-node dari node workspace yang sedang dipilih di pohon. */
	public void onTutupItem(Event event) {
		selectestreeitem = workspaceTree.getSelectedItem();
		if (selectestreeitem != null) {
			setOpen(false, selectestreeitem);
		}
	}

	/** @return menu konteks pohon dengan aksi "Tampil"/"Tutup" (kembangkan/lipat sub-node terpilih). */
	private Menupopup createMenupopup() {
		Menupopup menupopup = new Menupopup();
		menupopup.setId("myMenuPopup");
		Menuitem menuitem = new Menuitem();
		menuitem.setParent(menupopup);
		menuitem.setImage("/img/tampil.png");
		menuitem.setLabel("Tampil");
		menuitem.setTooltiptext("Tampil Detail Workspace");
		menuitem.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onTampilItem(event);
			}
		});

		menuitem = new Menuitem();
		menuitem.setParent(menupopup);
		menuitem.setImage("/img/tutup.png");
		menuitem.setLabel("Tutup");
		menuitem.setTooltiptext("Tutup Detail Workspace");
		menuitem.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onTutupItem(event);
			}
		});

		return menupopup;
	}

	/**
	 * Menyusun (hanya bila jendela masih kosong atau tahun berubah) seluruh konten dialog: pohon
	 * workspace, filter judul kegiatan dan tahun, serta toolbar terapkan/batal, lalu menampilkan
	 * jendela sebagai modal.
	 */
	private void init() {
		window.setTitle("Pilih Kegiatan");
		window.setWidth("90%");
		window.setHeight("90%");
		if (window.getChildren().size() == 0 || !tahun.equals(selectedTahun)) {
			Common.clear(window);
			selectedWorspaces.clear();
			workspaceTree = new Tree();
			workspaceTree.setWidth("100%");
			workspaceTree.setHeight("100%");
			workspaceTree.setZclass("z-vfiletree");
			menupopup = createMenupopup();
			selectedTahun = tahun;
			initTree();
			Panel panel = new ais.ui.util.MyPanelConfig();
			panel.setWidth("100%");
			panel.setHeight("100%");
			panel.setParent(window);
			panel.setBorder("none");

			Panelchildren panelchildren = new Panelchildren();
			panelchildren.setParent(panel);
			panelchildren.appendChild(menupopup);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panelchildren);
			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(north);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Judul Kegiatan"));
			row.appendChild(judulKegiatan);
			judulKegiatan.setWidth("90%");

			if (tampilTahun) {
				row = new Row();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Workspace"));
				row.appendChild(tahunWorkspace);
				tahunWorkspace.setWidth("90%");
				tahunWorkspace.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tahunWorkspace.getSelectedItem() == null)
							return;
						PilihKegiatanHelper.this.tahun = (Integer) tahunWorkspace.getSelectedItem().getValue();
						PilihKegiatanHelper.this.init();
					}
				});
			}

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.appendChild(workspaceTree);
			workspaceTree.setContext("myMenuPopup");

			South south = new South();
			south.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(south, true);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(south);

			MyToolbarbuttonConfig button;

			button = new MyToolbarbuttonConfig("", "/img/apply.png");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pemilih.pilih(judulKegiatan.getValue().trim(), selectedWorspaces);
					window.setVisible(false);
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/cancel.gif");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					window.setVisible(false);
				}
			});
			button.setParent(toolbar);
		}

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Memuat seluruh {@link Workspace} tahun {@link #tahun} dari basis data, lalu membangun pohonnya lewat {@link #loadWorkspace}. */
	@SuppressWarnings("unchecked")
	private void initTree() {
		try {

			Session session = HibernateUtil.currentSession();
			workspaces = session.createCriteria(Workspace.class)
					.add(Restrictions.or(Restrictions.eq("carryOver", true),
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
					.add(Restrictions.eq("tahunWorkspace", tahun)).list();
			loadWorkspace(tahun, null);

			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Menyusun kolom pohon (judul, jumlah anggaran, opsional realisasi+persen, pilih) dan node akar (workspace dengan {@code parentId=}{@link #ROOT}, atau satu node spesifik bila {@code rootId} diisi). */
	private void loadWorkspace(Integer tahun, Long rootId) {
		Common.clear(workspaceTree);
		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Pilih kegiatan pada perancanaan anggaran tahun " + tahun);
		treecol.setWidth(tampilRealisasi ? "65%" : "80%");
		treecol.setParent(treecols);

		treecol = new Treecol("Jumlah Anggaran");
		treecol.setWidth("15%");
		treecol.setParent(treecols);

		if (tampilRealisasi) {
			treecol = new Treecol("Jumlah Realisasi");
			treecol.setWidth("10%");
			treecol.setParent(treecols);
			treecol = new Treecol("%");
			treecol.setWidth("5%");
			treecol.setParent(treecols);
		}

		treecol = new Treecol("Pilih");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecols.setParent(workspaceTree);

		Treechildren tc1 = new Treechildren();
		for (final Workspace workspace : workspaces) {
			if (rootId == null) {
				if (workspace.getParentId().equals(ROOT)) {
					MyTreeitemConfig treeitem = new MyTreeitemConfig();
					treeitem.setValue(workspace.getId());
					treeitem.setOpen(false);
					// treeitem.setImage("/img/root.png");
					createTreerow(treeitem, workspace);
					treeitem.setParent(tc1);
					createRootSubWorkspace(workspace.getId(), treeitem);
				}
			} else {
				if (workspace.getId().equals(rootId)) {
					MyTreeitemConfig treeitem = new MyTreeitemConfig();
					treeitem.setValue(workspace.getId());
					treeitem.setOpen(false);
					// treeitem.setImage("/img/root.png");
					createTreerow(treeitem, workspace);
					treeitem.setParent(tc1);
					createRootSubWorkspace(workspace.getId(), treeitem);
				}
			}
		}
		tc1.setParent(workspaceTree);

	}

	/** Membuat kontainer anak dan mengisinya secara rekursif dengan sub-workspace dari {@code root}. */
	private void createRootSubWorkspace(Long root, MyTreeitemConfig componen) {
		Treechildren tc1 = new Treechildren();
		createRootSubWorkspace(root, tc1);
		tc1.setParent(componen);
	}

	/** Menambahkan satu node tree-item per workspace anak langsung dari {@code root}, rekursif ke sub-workspace bila node tersebut punya anak ({@link #hasChild}). */
	private void createRootSubWorkspace(Long root, Treechildren tc1) {
		for (final Workspace workspace : workspaces) {
			if (workspace.getParentId().equals(root)) {
				Boolean ada = hasChild(workspace.getId());
				if (ada) {
					final MyTreeitemConfig treeitem = new MyTreeitemConfig();
					treeitem.setParent(tc1);
					treeitem.setValue(workspace.getId());
					treeitem.setOpen(false);
					createTreerow(treeitem, workspace);
					createRootSubWorkspace(workspace.getId(), treeitem);
				} else {
					final MyTreeitemConfig treeitem = new MyTreeitemConfig();
					treeitem.setParent(tc1);
					treeitem.setValue(workspace.getId());
					treeitem.setOpen(false);
					createTreerow(treeitem, workspace);
				}
			}
		}
	}

	/** @return {@code true} bila ada workspace lain dengan {@code parentId} sama dengan {@code root}. */
	private Boolean hasChild(Long root) {
		for (Workspace workspace : workspaces) {
			if (workspace.getParentId().equals(root)) {
				return true;
			}
		}
		return false;
	}

	private void createTreerow(MyTreeitemConfig treeitem, final Workspace workspace) {
		Treerow treerow = new Treerow();
		createTreerow(treerow, workspace);
		treerow.setParent(treeitem);
	}

	private void createTreerow(Treerow treerow, final Workspace workspace) {
		generateTreecell(treerow, workspace);
	}

	/**
	 * Mengisi satu baris pohon dengan kode+nama, jumlah anggaran, opsional realisasi+persentase,
	 * dan checkbox pilih — checkbox hanya ditampilkan bila {@link #tampilRootSelect} atau node
	 * tersebut adalah daun (tidak punya anak); memilih/membatalkan checkbox memperbarui
	 * {@link #selectedWorspaces}.
	 */
	private void generateTreecell(Treerow treerow, final Workspace workspace) {
		new Treecell((workspace.getKode() == null || workspace.getKode().trim().equals("") ? ""
				: "" + workspace.getKode() + " - ") + workspace.getNama()).setParent(treerow);

		Treecell treecell = new Treecell(numberFormat.format(workspace.getHargaTotal()));
		treecell.setStyle("text-align: right;");
		treecell.setParent(treerow);

		if (tampilRealisasi) {
			treecell = new Treecell(numberFormat.format(workspace.getRealisasiProses()));
			treecell.setStyle("text-align: right;");
			treecell.setParent(treerow);

			Double persen = (((workspace.getRealisasiProses() == null || workspace.getRealisasiProses().equals(0.0))
					? 0.0
					: workspace.getRealisasiProses()) * 100)
					/ ((workspace.getHargaTotal() == null || workspace.getHargaTotal().equals(0.0)) ? 0.0
							: workspace.getHargaTotal());

			treecell = new Treecell(numberFormat.format(persen) + "%");
			treecell.setStyle("text-align: right;");
			treecell.setParent(treerow);
		}

		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		checkbox.addEventListener("onCheck", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (checkbox.isChecked()) {
					if (!selectedWorspaces.contains(workspace)) {
						selectedWorspaces.add(workspace);
					}
				} else {
					selectedWorspaces.remove(workspace);
				}
			}
		});
		if (tampilRootSelect) {
			final Treecell pilih = new Treecell();
			pilih.setStyle("text-align: center;");
			pilih.appendChild(checkbox);
			pilih.setParent(treerow);
		} else {
			if (!hasChild(workspace.getId())) {
				final Treecell pilih = new Treecell();
				pilih.setStyle("text-align: center;");
				pilih.appendChild(checkbox);
				pilih.setParent(treerow);
			} else {
				final Treecell pilih = new Treecell("");
				pilih.setParent(treerow);
			}
		}

	}

	/** @return tahun workspace yang pohonnya terakhir dibangun, atau {@code null} bila belum pernah ditampilkan. */
	public Integer getSelectedTahun() {
		return selectedTahun;
	}

}
