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

import ais.action.master.library.util.KategoriItemTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.KategoriItem;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.library.KategoriItem} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/onSearchDefault/
 * renderer/callback). {@code KategoriItem} adalah kategori/klasifikasi koleksi pustaka (mis. "Buku" &gt;
 * "Fiksi" &gt; "Novel") yang bersifat HIERARKIS lewat relasi {@code parent}; karena itu kelas ini menyimpang
 * dari kebanyakan subclass Bandbox picker lain yang menampilkan grid datar — di sini popup utama menampilkan
 * kategori sebagai POHON ({@link Tree}) memakai model khusus {@link KategoriItemTreeModel}.
 *
 * <p>
 * Popup terbagi dua tab: tab "Daftar" menampilkan seluruh pohon kategori (dapat diperluas/dipersempit sesuai
 * struktur parent-anak), dan tab "Sering Dipakai" ({@link KategoriItemSeringDipakai}) menampilkan grid datar
 * berisi maksimal 20 kategori {@code defaultItem = true} yang diurutkan menurun berdasarkan {@code jmlDipakai}
 * (jumlah pemakaian), sebagai jalan pintas tanpa perlu menavigasi pohon. Setiap kali sebuah kategori dipilih —
 * dari node pohon maupun dari baris grid "Sering Dipakai" — kolom {@code jmlDipakai} baris tersebut di-increment
 * di database, sehingga popularitas kategori terakumulasi dari histori pemakaian.
 * </p>
 * <p>
 * Parameter constructor {@code chooseAll} menentukan apakah SEMUA node pohon boleh dipilih (termasuk node
 * kategori induk/pengelompokan), atau HANYA node daun ({@code kategoriItemTreeModel.getChildCount(...) == 0}) —
 * dipakai saat pemanggil hanya boleh menerima kategori final/spesifik, bukan kategori pengelompokan level atas.
 * Constructor tanpa argumen default ke {@code chooseAll = true}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataKategoriItemBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected KategoriItemTreeModel kategoriItemTreeModel;

	private Boolean chooseAll = false;

	/**
	 * Constructor default; memilih {@code chooseAll = true} (semua node pohon, termasuk kategori induk, boleh
	 * dipilih). Lihat {@link #AmbilDataKategoriItemBanbox(Boolean)}.
	 */
	public AmbilDataKategoriItemBanbox() throws Exception {
		this(true);
	}

	/**
	 * @param chooseAll {@code true} untuk mengizinkan pemilihan semua node pohon kategori (termasuk node yang
	 *                  punya anak); {@code false} untuk membatasi pemilihan hanya pada node daun (kategori tanpa
	 *                  sub-kategori). Lihat {@link GetEventListener} untuk pola constructor {@code onOpen} lazy.
	 */
	public AmbilDataKategoriItemBanbox(Boolean chooseAll) throws Exception {
		super();
		kategoriItemTreeModel = new KategoriItemTreeModel(false);
		this.chooseAll = chooseAll;

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					display();
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});

	}

	/**
	 * Mengganti flag {@code chooseAll} setelah instance dibuat. Catatan: pemanggilan {@code display()} di sini
	 * dikomentari (tidak aktif), sehingga pohon yang sudah terlanjur dirender TIDAK otomatis di-refresh — flag
	 * baru baru berlaku efektif pada render {@link KategoriItemTreeRenderer} berikutnya.
	 *
	 * @param chooseAll nilai baru untuk mode pemilihan node (lihat {@link #AmbilDataKategoriItemBanbox(Boolean)}).
	 */
	public void setChooseAll(Boolean chooseAll) throws Exception {
		this.chooseAll = chooseAll;
		// display();
	}

	/**
	 * Renderer satu node pohon kategori pada tab "Daftar". Menampilkan nama kategori ({@code kategoriItem}) pada
	 * kolom pertama, dan komponen pilihan ({@code MyRadioConfig}, tampil sebagai checkbox/radio) pada kolom kedua
	 * — komponen pilihan hanya DITAMPILKAN bila {@code chooseAll} bernilai true ATAU node tersebut adalah daun
	 * ({@code kategoriItemTreeModel.getChildCount(kategoriItem) == 0}), sesuai kontrak {@code chooseAll} yang
	 * dijelaskan di constructor. Saat komponen pilihan dicentang: kolom {@code jmlDipakai} kategori tersebut
	 * di-increment di database, popup ditutup, nilai/atribut Bandbox diisi, lalu {@link #eventListener} dipanggil
	 * — lihat {@link GetEventListener} untuk pola callback umum ini.
	 *
	 * @see AmbilDataKategoriItemBanbox
	 */
	class KategoriItemTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final KategoriItem kategoriItem = (KategoriItem) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);

				new Label(kategoriItem.toString()).setParent(arg0);

				arg0 = new Treecell();
				arg0.setParent(treerow);
				MyRadioConfig checkbox = new MyRadioConfig();
				checkbox.setVisible(chooseAll || kategoriItemTreeModel.getChildCount(kategoriItem) == 0);
				checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("kategoriItem", kategoriItem);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataKategoriItemBanbox.this.setOpen(false);
						AmbilDataKategoriItemBanbox.this.setAttribute("kategoriItem", kategoriItem);
						AmbilDataKategoriItemBanbox.this.setValue(kategoriItem.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(KategoriItem.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(kategoriItem.getId())).uniqueResult();
						count = count == null ? 0L : count;
						kategoriItem.setJmlDipakai(++count);
						Common.refreshUpdate(session, (kategoriItem));

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
	 * Membangun popup dua-tab: tab "Daftar" berisi {@link Tree} kategori ({@link KategoriItemTreeRenderer}) dan
	 * tab "Sering Dipakai" berisi {@link KategoriItemSeringDipakai}. Lihat {@link GetEventListener} untuk kapan
	 * method ini dipanggil (lazy, sekali, saat popup pertama dibuka).
	 *
	 * @see GetEventListener
	 */
	public void display() throws Exception {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("700px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

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
		tabpanelUtama.appendChild(new KategoriItemSeringDipakai());

	}

	/**
	 * Memasang model pohon ({@link #kategoriItemTreeModel}) dan renderer ({@link KategoriItemTreeRenderer}) ke
	 * {@link #tree}. Berbeda dari kebanyakan subclass Bandbox picker lain, method ini tidak menjalankan query
	 * Hibernate langsung — data pohon sudah disiapkan lebih dulu oleh {@link KategoriItemTreeModel} saat
	 * constructor dipanggil.
	 */
	public void onSearchDefault(Event event) throws Exception {

		tree.setModel(kategoriItemTreeModel);
		tree.setItemRenderer(new KategoriItemTreeRenderer());
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
	 * Tab kedua popup {@link AmbilDataKategoriItemBanbox}, berisi jalan pintas daftar kategori yang paling sering
	 * dipakai — grid datar berisi maksimal {@link Common#MAX_RESULT_20} kategori {@code defaultItem = true},
	 * diurutkan menurun berdasarkan {@code jmlDipakai}, sehingga pengguna tidak perlu menavigasi pohon kategori
	 * penuh untuk kategori yang biasa dipakai berulang. Field {@link #nama} disediakan pada form namun TIDAK
	 * dipakai sebagai kriteria filter oleh {@link #onSearchDefault(Event)} saat ini — query tetap memfilter
	 * {@code defaultItem = true} terlepas dari isi field tersebut.
	 *
	 * @see AmbilDataKategoriItemBanbox
	 */
	private class KategoriItemSeringDipakai extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		public KategoriItemSeringDipakai() throws Exception {
			super();
			display();
		}

		private Textbox nama;

		/**
		 * Renderer satu baris grid "Sering Dipakai": menampilkan nama kategori dan nama kategori induknya
		 * ({@code kategoriItem.getParent()}, kosong bila kategori level teratas). Klik pada baris (bukan
		 * checkbox/radio terpisah seperti tab pohon) langsung memilih kategori: kolom {@code jmlDipakai}
		 * di-increment, popup ditutup, nilai/atribut Bandbox diisi, lalu {@link #eventListener} dipanggil — lihat
		 * {@link GetEventListener} untuk pola callback umum ini.
		 *
		 * @see KategoriItemSeringDipakai
		 */
		class KategoriItemRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
				// TODO Auto-generated method stub
				final KategoriItem kategoriItem = (KategoriItem) arg1;

				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataKategoriItemBanbox.this.setOpen(false);
						AmbilDataKategoriItemBanbox.this.setAttribute("kategoriItem", kategoriItem);
						AmbilDataKategoriItemBanbox.this.setValue(kategoriItem.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(KategoriItem.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(kategoriItem.getId())).uniqueResult();
						count = count == null ? 0L : count;
						kategoriItem.setJmlDipakai(++count);
						Common.refreshUpdate(session, (kategoriItem));

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

				new Label(kategoriItem.getNama()).setParent(arg0);
				new Label(kategoriItem.getParent() == null ? "" : kategoriItem.getParent().getNama()).setParent(arg0);

			}

		}

		/**
		 * Membangun UI tab: form pencarian dengan field {@link #nama} (lihat catatan kelas — belum dipakai sebagai
		 * filter), tombol "Cari", dan grid hasil ({@link KategoriItemRenderer}) dengan paging mold client-side
		 * ({@code grid.setMold("paging")}, 50 baris/halaman).
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

			MyGrid searchgrid = new MyGrid();
			searchgrid.setWidth("100%");
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

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
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
			column.setLabel("Nama");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Parent");
			column.setWidth("25%");

			onSearchDefault(null);

		}

		/**
		 * Mengambil hingga {@link Common#MAX_RESULT_20} kategori dengan {@code defaultItem = true}, diurutkan
		 * menurun berdasarkan {@code jmlDipakai} (kategori paling sering dipilih tampil paling atas). Catatan:
		 * field {@link #nama} pada form TIDAK dipakai sebagai kriteria di sini.
		 */
		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) throws Exception {

			Session session = HibernateUtil.currentSession();
			List<KategoriItem> kategoriItems = session.createCriteria(KategoriItem.class)
					.add(Restrictions.eq("defaultItem", true)).addOrder(Order.desc("jmlDipakai"))
					.setMaxResults(Common.MAX_RESULT_20).list();
			ListModel strset = new SimpleListModel(kategoriItems);
			grid.setRowRenderer(new KategoriItemRenderer());
			grid.setModelCheckMobile(strset);

		}

	}

}
