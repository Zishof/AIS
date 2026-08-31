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
 * Tipe khusus untuk ambil data kategori item banbox. Kelas ini memberi nama dan batas tanggung
 * jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tree tree}, {@code EventListener
 * eventListener}, {@code KategoriItemTreeModel kategoriItemTreeModel}, {@code Boolean chooseAll};
 * pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()});
 * mutasi data ({@code setChooseAll()}); operasi domain lain ({@code display()}); konfigurasi constructor: {@code
 * kategoriItemTreeModel}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
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

	public AmbilDataKategoriItemBanbox() throws Exception {
		this(true);
	}

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

	public void setChooseAll(Boolean chooseAll) throws Exception {
		this.chooseAll = chooseAll;
		// display();
	}

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

	public void onSearchDefault(Event event) throws Exception {

		tree.setModel(kategoriItemTreeModel);
		tree.setItemRenderer(new KategoriItemTreeRenderer());
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

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
