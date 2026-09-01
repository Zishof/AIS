package ais.action.master.library.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import ais.ui.util.MyRadioConfig;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.library.util.UdcItemTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.UdcItem;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.library.UdcItem} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/onSearchDefault/
 * renderer/callback). {@code UdcItem} adalah kode klasifikasi pustaka berdasarkan skema UDC (Universal Decimal
 * Classification) yang bersifat HIERARKIS lewat relasi {@code parent} (mis. kode induk "5" Ilmu Pengetahuan Alam
 * punya sub-kode "51" Matematika, dst.). Sama seperti {@code AmbilDataKategoriItemBanbox}, kelas ini menampilkan
 * data sebagai POHON ({@link Tree}, model {@link UdcItemTreeModel}) alih-alih grid datar, karena struktur UDC
 * memang berjenjang.
 *
 * <p>
 * Popup terbagi dua tab: tab "Daftar" berisi seluruh pohon kode UDC, dan tab "Sering Dipakai"
 * ({@link UdcItemSeringDipakai}) berisi grid datar (kolom Kode UDC, Nama UDC, Parent) berisi kode
 * {@code defaultItem = true} yang diurutkan menurun berdasarkan {@code jmlDipakai} (jumlah pemakaian), sebagai
 * jalan pintas tanpa perlu menavigasi pohon. Setiap kali sebuah kode UDC dipilih — dari node pohon maupun dari
 * baris grid "Sering Dipakai" — kolom {@code jmlDipakai} baris tersebut di-increment di database.
 * </p>
 * <p>
 * Parameter constructor {@code chooseAll} menentukan apakah SEMUA node pohon boleh dipilih, atau HANYA node daun
 * ({@code udcItemTreeModel.getChildCount(...) == 0}) — sama seperti pada {@code AmbilDataKategoriItemBanbox}.
 * Constructor tanpa argumen default ke {@code chooseAll = true}. BERBEDA dari kebanyakan subclass Bandbox picker
 * lain (yang membangun {@link Bandpopup}/{@link Radiogroup} secara lazy di dalam {@code display()} saat popup
 * pertama dibuka): kelas ini membangun kerangka {@code Bandpopup}/{@code Radiogroup} langsung di constructor,
 * sedangkan {@code display(Radiogroup)} hanya mengisi KONTEN popup, dijaga idempoten oleh flag
 * {@link #hasDisplayed} (bukan {@code getChildren().isEmpty()} seperti pola umum).
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataUdcItemBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected UdcItemTreeModel udcItemTreeModel;

	private Boolean chooseAll = false;
	/** Penjaga agar {@link #display(Radiogroup)} hanya mengisi konten popup satu kali (idempoten). */
	private boolean hasDisplayed = false;

	/**
	 * Constructor default; memilih {@code chooseAll = true} (semua node pohon boleh dipilih). Lihat
	 * {@link #AmbilDataUdcItemBanbox(Boolean)}.
	 */
	public AmbilDataUdcItemBanbox() throws Exception {
		this(true);
	}

	/**
	 * Menyiapkan model pohon UDC serta kerangka popup ({@link Bandpopup} + {@link Radiogroup}) yang langsung
	 * dipasang sebagai child Bandbox ini (BUKAN lazy seperti kebanyakan subclass Bandbox picker lain — lihat
	 * catatan class-level). Konten popup baru diisi belakangan oleh {@link #display(Radiogroup)} saat event
	 * {@code onOpen} pertama kali terpicu.
	 *
	 * @param chooseAll {@code true} untuk mengizinkan pemilihan semua node pohon UDC (termasuk kode induk);
	 *                  {@code false} untuk membatasi pemilihan hanya pada node daun (kode UDC tanpa turunan).
	 */
	public AmbilDataUdcItemBanbox(Boolean chooseAll) throws Exception {
		super();
		udcItemTreeModel = new UdcItemTreeModel(false);
		this.chooseAll = chooseAll;

		final Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(AmbilDataUdcItemBanbox.this);
		bandpopup.setWidth("700px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (hasDisplayed) {
					return;
				}

				display(radiogroup);
			}
		});
	}

	/**
	 * Mengganti flag {@code chooseAll} setelah instance dibuat. Catatan: pemanggilan {@code display()} di sini
	 * dikomentari (tidak aktif), sehingga pohon yang sudah terlanjur dirender TIDAK otomatis di-refresh.
	 *
	 * @param chooseAll nilai baru untuk mode pemilihan node (lihat {@link #AmbilDataUdcItemBanbox(Boolean)}).
	 */
	public void setChooseAll(Boolean chooseAll) throws Exception {
		this.chooseAll = chooseAll;
		// display();
	}

	/**
	 * Renderer satu node pohon kode UDC pada tab "Daftar". Menampilkan label kode UDC pada kolom pertama, dan
	 * komponen pilihan ({@code MyRadioConfig}) pada kolom kedua — hanya DITAMPILKAN bila {@code chooseAll}
	 * bernilai true ATAU node tersebut daun ({@code udcItemTreeModel.getChildCount(udcItem) == 0}). Saat
	 * komponen pilihan dicentang: kolom {@code jmlDipakai} kode UDC tersebut di-increment di database, popup
	 * ditutup, nilai/atribut Bandbox diisi, lalu {@link #eventListener} dipanggil — lihat
	 * {@link GetEventListener} untuk pola callback umum ini.
	 *
	 * @see AmbilDataUdcItemBanbox
	 */
	class UdcItemTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final UdcItem udcItem = (UdcItem) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);

				new Label(udcItem.toString()).setParent(arg0);

				arg0 = new Treecell();
				arg0.setParent(treerow);
				MyRadioConfig checkbox = new MyRadioConfig();
				checkbox.setVisible(chooseAll
						|| udcItemTreeModel.getChildCount(udcItem) == 0);
				checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("udcItem", udcItem);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataUdcItemBanbox.this.setOpen(false);
						AmbilDataUdcItemBanbox.this.setAttribute("udcItem",
								udcItem);
						AmbilDataUdcItemBanbox.this.setValue(udcItem.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session
								.createCriteria(UdcItem.class)
								.setProjection(
										Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(udcItem.getId()))
								.uniqueResult();
						count = count == null ? 0L : count;
						udcItem.setJmlDipakai(++count);
						Common.refreshUpdate(session,(udcItem));

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

		}

	}

	/**
	 * Mengisi konten popup ke dalam {@code radiogroup} yang sudah disiapkan oleh constructor: panel dua-tab —
	 * tab "Daftar" berisi {@link Tree} kode UDC ({@link UdcItemTreeRenderer}) dan tab "Sering Dipakai" berisi
	 * {@link UdcItemSeringDipakai}. Dijaga idempoten oleh {@link #hasDisplayed} agar konten tidak dibangun ulang
	 * pada {@code onOpen} berikutnya (berbeda dari pola umum {@code getChildren().isEmpty()} — lihat catatan
	 * class-level).
	 *
	 * @param radiogroup wadah popup yang sudah dipasang sebagai child {@link Bandpopup} di constructor;
	 *                    dibersihkan ({@code Common.clear(radiogroup)}) sebelum diisi ulang.
	 */
	public void display(Radiogroup radiogroup) throws Exception {
		if (hasDisplayed) {
			return;
		}
		hasDisplayed = true;
		Common.clear(radiogroup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		toolbar.appendChild(Common.createCleanButton(this, this));

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(panelchildren);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabSoal = new MyTabConfig("Daftar");
		tabSoal.setParent(tabs);

		MyTabConfig tabJawaban = new MyTabConfig("Sering Dapakai");
		tabJawaban.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanelUtama);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		tree = new Tree();
		tree.setZclass("z-dottree");
		tree.setParent(center);

		Treecols columns = new Treecols();

		columns.setParent(tree);

		Treecol column = new Treecol();
		column.setParent(columns);
		column.setLabel("Nama Item");

		column = new Treecol();
		column.setParent(columns);
		column.setLabel("Pilih");
		column.setWidth("10%");

		onSearchDefault(null);

		tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);
		tabpanelUtama.appendChild(new UdcItemSeringDipakai());

	}

	/**
	 * Memasang model pohon ({@link #udcItemTreeModel}) dan renderer ({@link UdcItemTreeRenderer}) ke
	 * {@link #tree}. Sama seperti pada {@code AmbilDataKategoriItemBanbox}, tidak ada query Hibernate langsung
	 * di sini — data pohon sudah disiapkan lebih dulu oleh {@link UdcItemTreeModel} saat constructor dipanggil.
	 */
	public void onSearchDefault(Event event) throws Exception {

		tree.setModel(udcItemTreeModel);
		tree.setItemRenderer(new UdcItemTreeRenderer());
	}

	/** @see GetEventListener */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @see GetEventListener */
	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Tab kedua popup {@link AmbilDataUdcItemBanbox}, berisi jalan pintas daftar kode UDC yang paling sering
	 * dipakai — grid datar (kolom Kode UDC, Nama UDC, Parent) berisi kode {@code defaultItem = true}, diurutkan
	 * menurun berdasarkan {@code jmlDipakai}, sehingga pengguna tidak perlu menavigasi pohon UDC penuh untuk kode
	 * yang biasa dipakai berulang. Field {@link #nama} disediakan pada form namun TIDAK dipakai sebagai kriteria
	 * filter oleh {@link #onSearchDefault(Event)} saat ini — query tetap memfilter {@code defaultItem = true}
	 * terlepas dari isi field tersebut.
	 *
	 * @see AmbilDataUdcItemBanbox
	 */
	private class UdcItemSeringDipakai extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		public UdcItemSeringDipakai() throws Exception {
			super();
			display();
		}

		private Textbox nama;

		/**
		 * Renderer satu baris grid "Sering Dipakai": menampilkan nama UDC dan nama UDC induknya
		 * ({@code udcItem.getParent()}, kosong bila kode level teratas). Klik pada baris langsung memilih kode
		 * UDC: kolom {@code jmlDipakai} di-increment, popup ditutup, nilai/atribut Bandbox diisi, lalu
		 * {@link #eventListener} dipanggil — lihat {@link GetEventListener} untuk pola callback umum ini.
		 *
		 * @see UdcItemSeringDipakai
		 */
		class UdcItemRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
				// TODO Auto-generated method stub
				final UdcItem udcItem = (UdcItem) arg1;

				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataUdcItemBanbox.this.setOpen(false);
						AmbilDataUdcItemBanbox.this.setAttribute("udcItem",
								udcItem);
						AmbilDataUdcItemBanbox.this.setValue(udcItem.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session
								.createCriteria(UdcItem.class)
								.setProjection(
										Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(udcItem.getId()))
								.uniqueResult();
						count = count == null ? 0L : count;
						udcItem.setJmlDipakai(++count);
						Common.refreshUpdate(session,(udcItem));

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

				new Label(udcItem.getNama()).setParent(arg0);
				new Label(udcItem.getParent() == null ? "" : udcItem
						.getParent().getNama()).setParent(arg0);

			}

		}

		/**
		 * Membangun UI tab: form pencarian dengan field {@link #nama} (lihat catatan kelas — belum dipakai sebagai
		 * filter), tombol "Cari", dan grid hasil (kolom Kode UDC, Nama UDC, Parent) memakai
		 * {@link UdcItemRenderer} dengan paging mold client-side ({@code grid.setMold("paging")}, 50 baris/halaman).
		 */
		public void display() throws Exception {

			Center center = new Center();
			center.setParent(this);
			ais.ui.util.ZkCompat.setFlex(center, true);

			org.zkoss.zul.Grid gridUtama = new org.zkoss.zul.Grid();
		gridUtama.setWidth("100%");
		ais.ui.util.ZkCompat.setFlex(gridUtama, true);
		gridUtama.setParent(center);
		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		Row rowUtama = new Row();
		rowUtama.setParent(rowsUtama);

			MyGrid searchgrid = new MyGrid();searchgrid.setWidth("100%");
			searchgrid.setParent(rowUtama);

			Rows rows = new Rows();
			rows.setParent(searchgrid);

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
			row.appendChild(nama = new Textbox());
			nama.setWidth("90%");

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			});
			button.setParent(toolbar);

			grid = new MyGrid();//grid.setOddRowSclass("non-odd");grid.setWidth("100%");
			/* setPageSize legacy dihapus: grid bukan mold "paging" sehingga setPageSize melempar IllegalStateException ("Available only the paging mold") dan daftar tidak pernah tampil. Paging ditangani AmbilDataPagingHelper. */
			/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
			 * client-side yang dibatasi MAX_RESULT. */
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

			Columns columns = new Columns();

			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kode UDC");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama UDC");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Parent");
			column.setWidth("25%");

			onSearchDefault(null);

		}

		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) throws Exception {

			Session session = HibernateUtil.currentSession();
			List<UdcItem> udcItems = session.createCriteria(UdcItem.class)
					.add(Restrictions.eq("defaultItem", true))
					.addOrder(Order.desc("jmlDipakai"))
					.setMaxResults(Common.MAX_RESULT).list();
			ListModel strset = new SimpleListModel(udcItems);
			grid.setRowRenderer(new UdcItemRenderer());
			grid.setModelCheckMobile(strset);

			

		}

	}

}
