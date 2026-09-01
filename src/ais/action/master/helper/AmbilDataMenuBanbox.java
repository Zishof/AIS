package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treerow;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Menu} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback), TAPI kelas ini menyimpang CUKUP JAUH
 * dari kerangka standar sehingga layak dibaca detail sebelum dianggap mengikuti pola biasa.
 * <p>
 * {@code Menu} adalah master data menu navigasi aplikasi AIS, berstruktur HIERARKIS
 * self-referencing lewat pasangan kolom {@code root}/{@code child} (menu level atas punya
 * {@code root == 0}). Popup di kelas ini BUKAN form pencarian + grid seperti kebanyakan subclass
 * sejenis, melainkan sebuah {@link org.zkoss.zul.Tree} yang menggambarkan seluruh hierarki menu
 * aktif sekaligus ({@link #createTreeMenu}/{@link #createRootSubMenu} membangunnya secara
 * rekursif) — tidak ada field pencarian teks sama sekali, hanya tombol "Refresh" yang memuat ulang
 * tree dari database (berguna bila menu baru saja diubah admin). Tiap {@link org.zkoss.zul.Treerow}
 * memasang satu {@link ais.ui.util.MyRadioConfig} lewat {@link #createTreerow} yang listener-nya
 * memakai event {@code onClick} (BUKAN {@code onCheck} seperti subclass Radiogroup lain di
 * keluarga ini). Constructor MEWAJIBKAN parameter {@link Menu} (bisa {@code null}) yang dipakai
 * memprapilih/menandai radio button menu yang sedang aktif saat popup dibangun. Filter hanya
 * {@code aktif == true} atau {@code aktif} kosong, diurutkan berdasar nomor urut lalu root/child.
 * Pemilihan bersifat TUNGGAL.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataMenuBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;

	private EventListener eventListener;

	private Center center;

	private Menu menu;

	/**
	 * Konstruktor WAJIB menerima {@link Menu} yang sedang terpilih (boleh {@code null}): dipakai
	 * {@link #createTreerow} untuk menandai radio button menu yang sesuai sebagai sudah tercentang
	 * saat tree pertama kali dibangun. Menetapkan teks tampilan awal dari {@code menu.getLabel()},
	 * lalu memasang listener {@code onOpen} standar yang membangun popup (tree) secara lazy pada
	 * pembukaan pertama — lihat {@link ais.ui.util.GetEventListener}. Tidak ada constructor tanpa
	 * argumen; kelas ini SELALU butuh nilai awal yang eksplisit.
	 *
	 * @param menu menu yang sedang terpilih untuk diprapilih/ditandai di tree, atau {@code null}
	 *             bila belum ada yang terpilih
	 */
	public AmbilDataMenuBanbox(Menu menu) {
		super();
		this.menu = menu;
		setValue(menu == null ? "" : menu.getLabel());
		setReadonly(true);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {

					display();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
							setOpen(true);
						}
					});
				}
			}
		});

	}

	/**
	 * Membangun popup pemilihan {@link Menu} sekali (dipanggil lazy dari listener {@code onOpen}):
	 * BUKAN form pencarian, melainkan area {@link #center} kosong tempat {@link #onSearchDefault}
	 * akan memasang {@link org.zkoss.zul.Tree} hierarki menu, plus toolbar berisi tombol Refresh.
	 * Mengikuti struktur bandpopup umum keluarga {@code AmbilData*Banbox} (lihat
	 * {@link ais.ui.util.GetEventListener}) tapi TANPA field pencarian sama sekali — lihat catatan
	 * di Javadoc class.
	 */
	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Menu");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		// Hanya berisi toolbar (Refresh/Bersihkan) — rapatkan agar tak ada ruang kosong.
		north.setHeight("48px");
		north.setAutoscroll(true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		toolbar.appendChild(Common.createCleanButton(this, this));

	}

	/**
	 * Memuat ULANG seluruh hierarki {@link Menu} aktif dari database dan membangun
	 * {@link org.zkoss.zul.Tree} baru ke {@link #center} — TIDAK menyaring berdasar teks apa pun
	 * (tidak ada field pencarian di kelas ini), hanya filter {@code aktif} dan pengurutan
	 * (nomor urut, lalu root, lalu child). Dipanggil dari tombol "Refresh" dan sekali secara
	 * otomatis saat popup pertama dibuka (dari listener {@code onOpen}). Menyimpang dari kerangka
	 * {@code onSearchDefault} standar di {@link ais.ui.util.GetEventListener} (yang biasanya
	 * memasang model+renderer ke grid) — di sini membangun tree lewat {@link #createTreeMenu}.
	 *
	 * @param event event pemicu (klik tombol Refresh); boleh {@code null} saat dipanggil pertama
	 *              kali dari listener {@code onOpen}
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Menu> menus = ConstantValues.simpleList(session.createCriteria(Menu.class).addOrder(Order.asc("nomorUrut"))
				.addOrder(Order.asc("root")).addOrder(Order.asc("child")).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				Menu.class);
		Tree tree = new Tree();
		createTreeMenu(tree, menus);
		tree.setParent(center);

	}

	/**
	 * Membangun kolom-kolom {@link Tree} lalu mengisi level menu TERATAS ({@code root == 0}) satu
	 * per satu, tiap barisnya memicu {@link #createRootSubMenu} secara rekursif untuk membangun
	 * anak-anaknya. Dipanggil sekali per pemuatan dari {@link #onSearchDefault(Event)}.
	 *
	 * @param tree  komponen tree kosong yang akan diisi
	 * @param menus seluruh baris {@link Menu} aktif hasil query (datar, belum tersusun hierarki)
	 */
	public void createTreeMenu(Tree tree, final List<Menu> menus) {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Menu");
		treecol.setWidth("90%");
		treecol.setParent(treecols);
		treecol = new Treecol("");
		treecol.setWidth("5%");
		treecol.setParent(treecols);
		treecols.setParent(tree);

		Treechildren tc1 = new Treechildren();
		for (Menu menu : menus) {
			if (menu.getRoot().equals(0L)) {
				MyTreeitemConfig treeitem = new MyTreeitemConfig();
				treeitem.setOpen(false);
				treeitem.setValue(menu.getUrl());
				createTreerow(treeitem, menu);
				treeitem.setParent(tc1);
				createRootSubMenu(menu.getChild(), menus, treeitem);
			}
		}
		tc1.setParent(tree);
	}

	/**
	 * Memeriksa apakah ada baris {@link Menu} lain di {@code menus} yang menjadikan {@code root}
	 * sebagai induknya (dipakai {@link #createRootSubMenu} untuk menentukan apakah suatu menu
	 * masih perlu direkursi lebih dalam).
	 *
	 * @param root  id menu yang dicek apakah punya anak
	 * @param menus seluruh baris {@link Menu} aktif hasil query
	 * @return {@code true} bila ditemukan minimal satu menu dengan {@code root} sebagai induknya
	 */
	private Boolean hasChild(Long root, List<Menu> menus) {
		for (Menu menu : menus) {
			if (menu.getRoot().equals(root)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Mengisi satu {@link org.zkoss.zul.Treerow} untuk satu baris {@link Menu}: label menu, dan
	 * satu radio button ({@link ais.ui.util.MyRadioConfig}) — INILAH satu-satunya titik "callback"
	 * kelas ini (padanan renderer/listener {@code onCheck} pada subclass Radiogroup lain), tapi
	 * memakai event {@code onClick} (BUKAN {@code onCheck}). Radio button otomatis tercentang bila
	 * {@code menu} yang dirender sama dengan {@link #menu} yang diberikan lewat constructor.
	 * Listener klik menutup popup, menyimpan {@link Menu} terpilih ke atribut {@code "menu"} dan
	 * teks tampilan {@code menu.getLabel()}, lalu meneruskan event ke {@link #eventListener} bila
	 * terpasang.
	 *
	 * @param treeitem item tree yang akan diisi baris ini
	 * @param menu     baris {@link Menu} yang direpresentasikan
	 */
	private void createTreerow(MyTreeitemConfig treeitem, final Menu menu) {
		Treerow treerow = new Treerow();
		new Treecell(menu.getLabel()).setParent(treerow);

		Treecell treecell = new Treecell();
		final MyRadioConfig checkbox = new MyRadioConfig();
		if (AmbilDataMenuBanbox.this.menu != null && AmbilDataMenuBanbox.this.menu.getId() != null) {
			if (menu.getId().equals(AmbilDataMenuBanbox.this.menu.getId())) {
				checkbox.setChecked(true);
			}
		}

		checkbox.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataMenuBanbox.this.setValue(menu.getLabel());
				AmbilDataMenuBanbox.this.setOpen(false);
				AmbilDataMenuBanbox.this.setAttribute("menu", menu);
				if (eventListener != null) {
					eventListener.onEvent(arg0);
				}
			}
		});

		checkbox.setParent(treecell);
		treecell.setParent(treerow);
		treerow.setParent(treeitem);
	}

	/**
	 * Membangun rekursif anak-anak menu dari {@code root} ke dalam {@code componen} (item tree
	 * induk atau tree itu sendiri): iterasi seluruh {@code menus} yang beririsan {@code root} dan
	 * merekursi lebih dalam via {@link #hasChild} untuk menentukan apakah item itu sendiri punya
	 * anak lagi.
	 *
	 * @param root    id menu induk yang anak-anaknya sedang dibangun
	 * @param menus   seluruh baris {@link Menu} aktif hasil query
	 * @param componen komponen ZK (Tree atau Treeitem) tempat anak-anak menu dipasang
	 */
	private void createRootSubMenu(Long root, List<Menu> menus, Component componen) {
		Treechildren tc1 = new Treechildren();
		for (Menu menu : menus) {
			if (menu.getRoot().equals(root)) {
				Boolean ada = hasChild(menu.getChild(), menus);
				if (ada) {
					MyTreeitemConfig treeitem = new MyTreeitemConfig();
					treeitem.setOpen(false);
					treeitem.setValue(menu.getUrl());
					createTreerow(treeitem, menu);
					treeitem.setParent(tc1);
					createRootSubMenu(menu.getChild(), menus, treeitem);
				} else {
					MyTreeitemConfig treeitem = new MyTreeitemConfig();
					treeitem.setOpen(false);
					treeitem.setValue(menu.getUrl());
					createTreerow(treeitem, menu);
					treeitem.setParent(tc1);
				}
			}
		}

		tc1.setParent(componen);
	}

	/** {@inheritDoc} Implementasi setter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** {@inheritDoc} Implementasi getter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public EventListener getEventListener() {
		return eventListener;
	}
}
