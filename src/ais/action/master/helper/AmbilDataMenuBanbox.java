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
 * Tipe khusus untuk ambil data menu banbox. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code EventListener eventListener}, {@code
 * Center center}, {@code Menu menu}; pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()},
 * {@code getEventListener()}); operasi domain lain ({@code display()}, {@code createTreeMenu()}, {@code
 * hasChild()}, {@code createTreerow()}, {@code createRootSubMenu()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
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

	private Boolean hasChild(Long root, List<Menu> menus) {
		for (Menu menu : menus) {
			if (menu.getRoot().equals(root)) {
				return true;
			}
		}
		return false;
	}

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

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
