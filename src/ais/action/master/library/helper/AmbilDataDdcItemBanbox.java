package ais.action.master.library.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.library.util.DdcItemTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DdcItem;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Bandbox picker AIS untuk entity {@link ais.database.model.library.DdcItem} — item klasifikasi Dewey
 * Decimal Classification (DDC) yang dipakai modul Perpustakaan AIS untuk mengelompokkan buku secara
 * berjenjang (mis. kelas utama 000-900, lalu divisi, lalu seksi, dst., masing-masing {@code DdcItem}
 * menaut ke {@code parent}-nya sendiri). Kelas ini TIDAK mengikuti kerangka standar grid/renderer yang
 * dijelaskan di {@link ais.ui.util.GetEventListener} — DDC berjenjang lebih cocok dinavigasi sebagai
 * pohon daripada dicari lewat form kriteria flat, sehingga popup-nya berisi {@link Tree} yang datanya
 * dipasok {@link DdcItemTreeModel} (lazy-load anak per level langsung dari DB tiap node di-expand,
 * bukan memuat seluruh hierarki sekaligus).
 *
 * <p>Popup dibangun dalam dua tahap: {@link Radiogroup} dan {@link Bandpopup} dibuat eager di
 * constructor, sedangkan isi (tab + {@code Tree}) baru dibangun lazy saat popup pertama dibuka, dijaga
 * flag {@code hasDisplayed} (bukan {@code getChildren().isEmpty()} seperti pola standar) — dan tidak ada
 * pemanggilan {@code setOpen(true)} lewat timer di constructor. Parameter constructor {@code chooseAll}
 * (default {@code true} bila pakai constructor tanpa argumen) mengatur mode pilih: {@code true}
 * menampilkan tombol pilih (radio) di SETIAP node tree (boleh pilih kategori DDC di level mana pun),
 * {@code false} hanya menampilkan radio pada node daun ({@code ddcItemTreeModel.getChildCount(...) == 0}
 * — tidak punya anak). Memilih node (radio dicentang) menutup popup, menyimpan {@code DdcItem} terpilih
 * ke atribut {@code ddcItem}, DAN menaikkan counter {@code jmlDipakai} milik item tersebut lalu
 * menyimpannya via {@code Common.refreshUpdate(...)} (efek samping tulis, dipakai untuk pelacakan
 * pemakaian). Popup juga berisi tab kedua "Sering Dipakai" berisi {@link DdcItemSeringDipakai}, sub-picker
 * terpisah dengan grid pencarian flat (bukan pohon) berdasar kode/nama, sebagai jalan pintas tanpa perlu
 * menelusuri hierarki tree.</p>
 *
 * @see Bandbox
 */
public class AmbilDataDdcItemBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected DdcItemTreeModel ddcItemTreeModel;

	private Boolean chooseAll = false;
	private boolean hasDisplayed = false;

	/**
	 * Constructor default: sama dengan {@link #AmbilDataDdcItemBanbox(Boolean)} dengan
	 * {@code chooseAll = true} (radio pilih tampil di semua level node, bukan hanya daun).
	 */
	public AmbilDataDdcItemBanbox() throws Exception {
		this(true);
	}

	/**
	 * Membangun Bandbox, {@link DdcItemTreeModel} (root, mode {@code tampilkanSemua = false}), dan
	 * kerangka popup ({@link Bandpopup} + {@link Radiogroup}) secara eager; isi tree baru dibangun lazy
	 * lewat listener {@code onOpen} yang memanggil {@link #display(Radiogroup)} sekali (dijaga
	 * {@code hasDisplayed}).
	 *
	 * @param chooseAll {@code true} agar radio pilih tampil di setiap node tree (level mana pun boleh
	 *            dipilih); {@code false} agar radio pilih hanya tampil pada node daun (tanpa anak)
	 */
	public AmbilDataDdcItemBanbox(Boolean chooseAll) throws Exception {
		super();
		ddcItemTreeModel = new DdcItemTreeModel(false);
		this.chooseAll = chooseAll;

		final Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
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
	 * Mengubah mode pilih setelah instance dibuat (lihat penjelasan parameter {@code chooseAll} di
	 * {@link #AmbilDataDdcItemBanbox(Boolean)}). Catatan: tidak memicu render ulang tree ({@code
	 * display()} dikomentari) — efeknya baru terlihat pada render node berikutnya.
	 *
	 * @param chooseAll mode pilih baru
	 */
	public void setChooseAll(Boolean chooseAll) throws Exception {
		this.chooseAll = chooseAll;
		// display();
	}

	/**
	 * Merender satu {@link Treeitem} node DDC: label {@code ddcItem.toString()} pada sel pertama, dan
	 * radio pilih pada sel kedua yang HANYA tampil bila {@code chooseAll} true atau node ini daun
	 * (tanpa anak menurut {@link DdcItemTreeModel#getChildCount(Object)}). Saat radio dicentang, popup
	 * ditutup, {@code DdcItem} terpilih disimpan sebagai atribut {@code ddcItem}, dan counter
	 * {@code jmlDipakai} milik item dinaikkan lalu disimpan via {@code Common.refreshUpdate(...)}
	 * sebelum {@link #eventListener} (bila terpasang) diberi tahu.
	 *
	 * @see AmbilDataDdcItemBanbox
	 */
	class DdcItemTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final DdcItem ddcItem = (DdcItem) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);

				new Label(ddcItem.toString()).setParent(arg0);

				arg0 = new Treecell();
				arg0.setParent(treerow);
				MyRadioConfig checkbox = new MyRadioConfig();
				checkbox.setVisible(chooseAll || ddcItemTreeModel.getChildCount(ddcItem) == 0);
				checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("ddcItem", ddcItem);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataDdcItemBanbox.this.setOpen(false);
						AmbilDataDdcItemBanbox.this.setAttribute("ddcItem", ddcItem);
						AmbilDataDdcItemBanbox.this.setValue(ddcItem.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(DdcItem.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(ddcItem.getId())).uniqueResult();
						count = count == null ? 0L : count;
						ddcItem.setJmlDipakai(++count);
						Common.refreshUpdate(session, (ddcItem));

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
	 * Membangun isi popup sekali (dijaga {@code hasDisplayed}): panel judul "Daftar" berisi
	 * {@link Tabbox} dua tab — "Daftar" berisi {@link Tree} DDC ({@link #tree}) yang dipasok
	 * {@link #ddcItemTreeModel} dan {@link DdcItemTreeRenderer}, dan "Sering Dipakai" berisi sub-picker
	 * {@link DdcItemSeringDipakai} untuk pencarian flat berdasar kode/nama.
	 *
	 * @param radiogroup kontainer pilih-tunggal yang sudah dibuat di constructor, dibersihkan dulu
	 *            ({@code Common.clear(...)}) sebelum diisi ulang
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
		tabpanelUtama.appendChild(new DdcItemSeringDipakai());

	}

	/**
	 * Memasang {@link #ddcItemTreeModel} dan {@link DdcItemTreeRenderer} baru ke {@link #tree}. Berbeda
	 * dari pola standar {@code onSearchDefault} (yang menjalankan query kriteria), method ini tidak
	 * melakukan pencarian apa pun — pemuatan data per node ditangani lazy oleh {@link DdcItemTreeModel}
	 * sendiri saat node di-expand.
	 *
	 * @param event tidak dipakai; ada agar tetap sesuai konvensi penamaan {@code onSearchDefault} di
	 *            keluarga Bandbox picker
	 */
	public void onSearchDefault(Event event) throws Exception {

		tree.setModel(ddcItemTreeModel);
		tree.setItemRenderer(new DdcItemTreeRenderer());
	}

	/**
	 * Menetapkan listener yang dipanggil setelah node DDC dipilih (dari tab tree maupun tab "Sering
	 * Dipakai").
	 *
	 * @param eventListener listener baru yang akan dipasang
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * @return listener aktif saat ini, atau {@code null} bila belum diset
	 */
	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Sub-picker pada tab "Sering Dipakai" milik {@link AmbilDataDdcItemBanbox}: alternatif pencarian
	 * flat (bukan pohon) untuk {@link DdcItem}, dengan form kriteria Kode dan Nama (ilike, digabung AND
	 * bila diisi) diurutkan ascending berdasar kode dan dibatasi {@code Common.MAX_RESULT} baris —
	 * mengikuti bentuk grid/renderer standar seperti keluarga {@code AmbilData*Banbox} lain (lihat
	 * {@link ais.ui.util.GetEventListener}), kecuali pemilihan dipicu {@code onClick} pada baris
	 * (bukan checkbox/radio terpisah) karena tidak butuh mode pilih-jamak.
	 *
	 * @see AmbilDataDdcItemBanbox
	 */
	private class DdcItemSeringDipakai extends Borderlayout {

		/**
		 *
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();

		/**
		 * Membangun panel pencarian flat DDC dan langsung menampilkannya lewat {@link #display()}
		 * (dipanggil eager dari {@link AmbilDataDdcItemBanbox#display(Radiogroup)} saat tab ini dibuat).
		 */
		public DdcItemSeringDipakai() throws Exception {
			super();
			display();
		}

		private MyTextbox nama;
		private MyTextbox kode;

		/**
		 * Merender satu baris grid hasil pencarian DDC flat: label Nama DDC dan Parent (nama induk, atau
		 * kosong bila tidak ada). Baris ini dipilih via klik ({@code onClick} pada seluruh baris, bukan
		 * checkbox/radio): popup Bandbox induk ditutup, {@code DdcItem} terpilih disimpan sebagai atribut
		 * {@code ddcItem}, counter {@code jmlDipakai} milik item dinaikkan dan disimpan via
		 * {@code Common.refreshUpdate(...)}, lalu {@link AmbilDataDdcItemBanbox#eventListener} (bila
		 * terpasang) diberi tahu.
		 *
		 * @see DdcItemSeringDipakai
		 */
		class DdcItemRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
				// TODO Auto-generated method stub
				final DdcItem ddcItem = (DdcItem) arg1;

				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataDdcItemBanbox.this.setOpen(false);
						AmbilDataDdcItemBanbox.this.setAttribute("ddcItem", ddcItem);
						AmbilDataDdcItemBanbox.this.setValue(ddcItem.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(DdcItem.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(ddcItem.getId())).uniqueResult();
						count = count == null ? 0L : count;
						ddcItem.setJmlDipakai(++count);
						Common.refreshUpdate(session, (ddcItem));

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

				new Label(ddcItem.getNama()).setParent(arg0);
				new Label(ddcItem.getParent() == null ? "" : ddcItem.getParent().getNama()).setParent(arg0);

			}

		}

		/**
		 * Membangun form pencarian ({@code MyTextbox kode}, {@code MyTextbox nama}, keduanya juga
		 * memicu pencarian lewat {@code onOk}/Enter) + tombol Cari, dan grid hasil client-side bermold
		 * "paging" (page size 50). Diakhiri memanggil {@link #onSearchDefault(Event)} dengan {@code null}
		 * agar grid langsung terisi.
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
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
			row.appendChild(kode = new MyTextbox());
			kode.setWidth("90%");
			kode.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
			row.appendChild(nama = new MyTextbox());
			nama.setWidth("90%");
			nama.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

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

			grid = new MyGrid();//grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
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
			column.setLabel("Kode DDC");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama DDC");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Parent");
			column.setWidth("25%");

			onSearchDefault(null);

		}

		/**
		 * Menjalankan pencarian flat {@link DdcItem} (tanpa mempedulikan hierarki parent/child),
		 * difilter opsional lewat {@code Textbox nama} dan {@code Textbox kode} (ilike, digabung AND
		 * bila diisi), diurutkan ascending berdasar kode dan dibatasi {@code Common.MAX_RESULT} baris.
		 * Hasil dipasang ke {@link #grid} lewat {@link DdcItemRenderer} dan {@code SimpleListModel}.
		 *
		 * @param event event pemicu (boleh {@code null}, mis. saat dipanggil dari {@link #display()})
		 */
		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) {

			Session session = HibernateUtil.currentSession();

			List<DdcItem> myDdcItem = session.createCriteria(DdcItem.class)
					// .add(Restrictions.eq("defaultItem", true))
					.addOrder(Order.asc("kode"))
					.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
					.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE))
					.setMaxResults(Common.MAX_RESULT).list();
			ListModel strset = new SimpleListModel(myDdcItem);
			grid.setRowRenderer(new DdcItemRenderer());
			grid.setModelCheckMobile(strset);

			
		}

	}

}
