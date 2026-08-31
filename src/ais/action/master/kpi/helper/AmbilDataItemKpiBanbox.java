package ais.action.master.kpi.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.kpi.ItemKpi;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Tipe khusus untuk ambil data item kpi banbox. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tree tree}, {@code EventListener
 * eventListener}, {@code ItemKpiTreeModel itemKpiTreeModel}, {@code Boolean chooseAll}, {@code FormatKpiDetail
 * formatKpiDetail}; pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code
 * getEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataItemKpiBanbox extends Bandbox {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected ItemKpiTreeModel itemKpiTreeModel;

	private Boolean chooseAll = false;
	private FormatKpiDetail formatKpiDetail;

	public AmbilDataItemKpiBanbox(FormatKpiDetail formatKpiDetail) throws Exception {
		this(true, formatKpiDetail);
	}

	public AmbilDataItemKpiBanbox(Boolean chooseAll, FormatKpiDetail formatKpiDetail) throws Exception {
		super();
		this.chooseAll = chooseAll;
		display();
		this.formatKpiDetail = formatKpiDetail;
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataItemKpiBanbox}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataItemKpiBanbox} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataItemKpiBanbox
	 */
	class ItemKpiTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final ItemKpi itemKpi = (ItemKpi) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);

				new Label(itemKpi.toString()).setParent(arg0);

				arg0 = new Treecell();
				arg0.setParent(treerow);
				Radio checkbox = new Radio();
				checkbox.setVisible(chooseAll || itemKpiTreeModel.getChildCount(itemKpi) == 0);
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("itemKpi", itemKpi);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataItemKpiBanbox.this.setOpen(false);
						AmbilDataItemKpiBanbox.this.setAttribute("itemKpi", itemKpi);
						AmbilDataItemKpiBanbox.this.setValue(itemKpi.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(ItemKpi.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(itemKpi.getId())).uniqueResult();
						count = count == null ? 0L : count;
						itemKpi.setJmlDipakai(++count);
						Common.refreshUpdate(session, (itemKpi));

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
		toolbar.appendChild(Common.createCleanButton(this, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		}));

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(panelchildren);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final Tab tabSoal = new Tab("Daftar");
		tabSoal.setParent(tabs);

		Tab tabJababan = new Tab("Sering Dapakai");
		tabJababan.setParent(tabs);

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
		tabpanelUtama.appendChild(new ItemKpiSeringDipakai());

	}

	public void onSearchDefault(Event event) throws Exception {

		if (formatKpiDetail == null) {
			return;
		}

		itemKpiTreeModel = new ItemKpiTreeModel(false, formatKpiDetail);
		tree.setModel(itemKpiTreeModel);
		tree.setItemRenderer(new ItemKpiTreeRenderer());
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Tipe implementasi bersarang {@link ItemKpiSeringDipakai} milik {@link AmbilDataItemKpiBanbox}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataItemKpiBanbox} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
	 * implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code MyGrid grid}, {@code
	 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code Textbox nama}; operasi lokal: {@code display()},
	 * {@code onSearchDefault}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see AmbilDataItemKpiBanbox
	 */
	private class ItemKpiSeringDipakai extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		public ItemKpiSeringDipakai() throws Exception {
			super();
			display();
		}

		private Textbox nama;

		/**
		 * Renderer lokal untuk layar/komponen {@link ItemKpiSeringDipakai}. Kelas ini menerjemahkan satu item data
		 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ItemKpiSeringDipakai} dan dapat mengakses
		 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see ItemKpiSeringDipakai
		 */
		class ItemKpiRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				// TODO Auto-generated method stub
				final ItemKpi itemKpi = (ItemKpi) arg1;

				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataItemKpiBanbox.this.setOpen(false);
						AmbilDataItemKpiBanbox.this.setAttribute("itemKpi", itemKpi);
						AmbilDataItemKpiBanbox.this.setValue(itemKpi.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(ItemKpi.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(itemKpi.getId())).uniqueResult();
						count = count == null ? 0L : count;
						itemKpi.setJmlDipakai(++count);
						Common.refreshUpdate(session, (itemKpi));

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

				new Label(itemKpi.getNama()).setParent(arg0);

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
			searchgrid.setParent(rowUtama);

			Rows rows = new Rows();
			rows.setParent(searchgrid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
			row.appendChild(nama = new Textbox());
			nama.setWidth("90%");

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

			Toolbarbutton button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			});
			button.setParent(toolbar);

			grid = new MyGrid();
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

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("Nama Item KPI");

			onSearchDefault(null);

		}

		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) throws Exception {

			Session session = HibernateUtil.currentSession();
			List<ItemKpi> itemKpis = session.createCriteria(ItemKpi.class)

					.add(Restrictions.or(Restrictions.eq("formatKpiDetail", formatKpiDetail),
							Restrictions.and(Restrictions.eq("formatKpi", formatKpiDetail.getFormatKpi()),
									Restrictions.isNull("formatKpiDetail"))))

					.add(nama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("jmlDipakai"))
					.setMaxResults(Common.MAX_RESULT).list();
			ListModel strset = new SimpleListModel(itemKpis);
			grid.setRowRenderer(new ItemKpiRenderer());
			grid.setModelCheckMobile(strset);

			grid.renderAll();

		}

	}

}
