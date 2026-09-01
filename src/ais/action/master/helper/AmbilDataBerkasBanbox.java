package ais.action.master.helper;

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
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
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
import org.zkoss.zul.Treerow;

import ais.action.master.helper.util.BerkasTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Berkas;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Berkas} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). {@code Berkas} adalah master data
 * jenis berkas/dokumen (mis. "KTP", "Ijazah", "Kartu Keluarga") yang dipakai sebagai referensi
 * kelengkapan dokumen di berbagai alur AIS (PMB, kepegawaian, dsb.), berbentuk hierarki pohon
 * (punya {@code parent}, mis. kategori berkas &rarr; jenis berkas spesifik).
 * <p>
 * <b>Menyimpang dari kerangka standar</b>: popup tidak memakai grid pencarian teks, melainkan dua
 * tab: tab "Daftar Berkas" menampilkan seluruh hierarki sebagai {@link Tree} (model
 * {@link BerkasTreeModel}, renderer {@link BerkasTreeRenderer}), dan tab "Berkas Sering Dapakai"
 * ({@link BerkasSeringDipakai}) menampilkan grid pencarian nama biasa diurutkan berdasarkan
 * {@code jmlDipakai} menurun. Mode pilih: {@link #bisaDipilihSemua} (constructor
 * {@link #AmbilDataBerkasBanbox(Boolean)}) menentukan apakah node non-daun boleh dipilih
 * ({@code true}) atau hanya node daun ({@code false}, default — checkbox radio node non-daun
 * disembunyikan). Selain lewat pohon/grid, komponen juga mendukung entri manual: mengetik nama
 * berkas persis lalu Enter ({@code onOK}) mencari berkas dengan nama sama persis dan langsung
 * memilihnya bila ditemukan (menampilkan peringatan bila tidak). Setiap kali berkas dipilih
 * (lewat cara mana pun), counter {@code jmlDipakai} dinaikkan satu lewat
 * {@code Common.refreshUpdate} sebelum {@code eventListener} dipanggil.
 *
 * @see Bandbox
 */
public class AmbilDataBerkasBanbox extends Bandbox implements GetEventListener {

	/**
	 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected BerkasTreeModel berkasTreeModel;

	private Boolean bisaDipilihSemua = false;

	/** Constructor default: {@link #bisaDipilihSemua} dinonaktifkan (hanya node daun yang boleh dipilih). */
	public AmbilDataBerkasBanbox() {
		this(false);
	}

	/**
	 * Membangun komponen: memasang listener {@code onOK} (Enter setelah mengetik nama berkas
	 * persis) untuk pemilihan manual, dan listener {@code onOpen} yang, pada pembukaan pertama,
	 * membangun popup ({@link #display()}), mengikuti kerangka umum di
	 * {@link ais.ui.util.GetEventListener}.
	 *
	 * @param bisaDipilihSemua {@code true} untuk mengizinkan pemilihan node non-daun pada pohon
	 */
	public AmbilDataBerkasBanbox(Boolean bisaDipilihSemua) {
		super();
		this.bisaDipilihSemua = bisaDipilihSemua;
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Berkas berkas = (Berkas) HibernateUtil.currentSession().createCriteria(Berkas.class)
						.add(Restrictions.ilike("nama", AmbilDataBerkasBanbox.this.getValue().trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (berkas == null) {
					MyMessageboxConfig.show(
							"Berkas dengan nama = " + AmbilDataBerkasBanbox.this.getValue().trim() + " tidak ditemukan",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				AmbilDataBerkasBanbox.this.setOpen(false);
				AmbilDataBerkasBanbox.this.setAttribute("berkas", berkas);
				AmbilDataBerkasBanbox.this.setValue(berkas.toString());

				Session session = HibernateUtil.currentSession();
				Long count = (Long) session.createCriteria(Berkas.class)
						.setProjection(Projections.property("jmlDipakai")).add(Restrictions.idEq(berkas.getId()))
						.uniqueResult();
				count = count == null ? 0L : count;
				berkas.setJmlDipakai(++count);
				Common.refreshUpdate(session, (berkas));

				if (eventListener != null) {
					eventListener.onEvent(arg0);
				}
			}
		});

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
	 * Merender satu node pohon berkas: label nama pada kolom pertama, dan radio pilih pada kolom
	 * ketiga — disembunyikan bila node masih punya anak dan {@link #bisaDipilihSemua} bernilai
	 * {@code false} (aturan "hanya daun"). Memilih radio menutup popup, menyimpan entity
	 * {@link Berkas} terpilih ke attribute {@code "berkas"} pada Bandbox, mengisi teks tampilan
	 * dengan {@code berkas.toString()}, menaikkan counter {@code jmlDipakai}, lalu memicu
	 * {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataBerkasBanbox
	 */
	class BerkasTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final Berkas berkas = (Berkas) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);
				new Label(berkas.getNama()).setParent(arg0);

				arg0 = new Treecell();
				arg0.setParent(treerow);

				arg0 = new Treecell();
				arg0.setParent(treerow);
				MyRadioConfig checkbox = new MyRadioConfig();
				if (!bisaDipilihSemua) {
					checkbox.setVisible(berkasTreeModel.getChildCount(berkas) == 0);
				}
				checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("berkas", berkas);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataBerkasBanbox.this.setOpen(false);
						AmbilDataBerkasBanbox.this.setAttribute("berkas", berkas);
						AmbilDataBerkasBanbox.this.setValue(berkas.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(Berkas.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(berkas.getId())).uniqueResult();
						count = count == null ? 0L : count;
						berkas.setJmlDipakai(++count);
						Common.refreshUpdate(session, (berkas));

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
	 * Membangun popup (dipanggil sekali saat pertama dibuka): tab "Daftar Berkas" berisi
	 * {@link Tree} hierarki penuh (dimuat lewat {@link #onSearchDefault(Event)}), dan tab "Berkas
	 * Sering Dapakai" berisi {@link BerkasSeringDipakai}.
	 */
	public void display() {
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
		panel.setTitle("Daftar Berkas");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(panelchildren);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabSoal = new MyTabConfig("Daftar Berkas");
		tabSoal.setParent(tabs);

		MyTabConfig tabJawaban = new MyTabConfig("Berkas Sering Dapakai");
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
		column.setLabel("Berkas");
		column.setWidth("85%");

		column = new Treecol();
		column.setParent(columns);
		column.setLabel("Pilih");
		column.setWidth("5%");

		onSearchDefault(null);

		tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);
		tabpanelUtama.appendChild(new BerkasSeringDipakai());

	}

	/**
	 * Membuat ulang {@link #berkasTreeModel} dan memasang ulang {@link BerkasTreeRenderer} ke
	 * {@link #tree}.
	 *
	 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
	 */
	public void onSearchDefault(Event event) {
		berkasTreeModel = new BerkasTreeModel();
		tree.setModel(berkasTreeModel);
		tree.setItemRenderer(new BerkasTreeRenderer());
	}

	/** @param eventListener dipanggil setiap kali user memilih satu berkas */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan berkas yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Isi tab "Berkas Sering Dapakai" pada popup {@link AmbilDataBerkasBanbox}: grid pencarian
	 * nama biasa (bukan pohon) atas {@link Berkas} yang pernah dipakai ({@code jmlDipakai} tidak
	 * null), diurutkan menurun berdasarkan {@code jmlDipakai} sehingga berkas paling sering
	 * dipilih tampil di atas — mempercepat pemilihan berkas umum tanpa perlu menelusuri hierarki
	 * pohon.
	 *
	 * @see AmbilDataBerkasBanbox
	 */
	private class BerkasSeringDipakai extends Borderlayout {

		/**
		 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		/** Membuat panel dan langsung menyusun isinya lewat {@link #display()}. */
		public BerkasSeringDipakai() {
			super();
			display();
		}

		private Textbox nama;

		/**
		 * Merender satu baris grid "Berkas Sering Dapakai": nama berkas. Mengklik baris (bukan
		 * radio — seluruh baris klikabel) menutup popup induk, menyimpan entity {@link Berkas}
		 * terpilih ke attribute {@code "berkas"} pada {@link AmbilDataBerkasBanbox}, mengisi teks
		 * tampilan, menaikkan counter {@code jmlDipakai}, lalu memicu {@code eventListener} milik
		 * kelas induk bila terpasang.
		 *
		 * @see BerkasSeringDipakai
		 */
		class BerkasRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
				// TODO Auto-generated method stub
				final Berkas berkas = (Berkas) arg1;

				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataBerkasBanbox.this.setOpen(false);
						AmbilDataBerkasBanbox.this.setAttribute("berkas", berkas);
						AmbilDataBerkasBanbox.this.setValue(berkas.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(Berkas.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(berkas.getId())).uniqueResult();
						count = count == null ? 0L : count;
						berkas.setJmlDipakai(++count);
						Common.refreshUpdate(session, (berkas));

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

				new Label(berkas.getNama()).setParent(arg0);

			}

		}

		/**
		 * Membangun tata letak panel "Berkas Sering Dapakai": form filter Nama Berkas, grid hasil
		 * bermold "paging", lalu memuat data awal lewat {@link #onSearchDefault(Event)}.
		 */
		public void display() {

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
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Berkas"));
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

			toolbar.appendChild(Common.createCleanButton(AmbilDataBerkasBanbox.this, AmbilDataBerkasBanbox.this));

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
			column.setLabel("Nama Berkas");

			onSearchDefault(null);

		}

		/**
		 * Mengambil {@link Berkas} yang pernah dipakai ({@code jmlDipakai} tidak null) dan cocok
		 * filter nama (ILIKE ANYWHERE, kosong berarti semua), diurutkan menurun berdasarkan
		 * {@code jmlDipakai}, dibatasi {@link Common#MAX_RESULT} baris. Mengisi ulang grid dengan
		 * hasilnya beserta {@link BerkasRenderer}.
		 *
		 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
		 */
		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) {

			Session session = HibernateUtil.currentSession();
			List<Berkas> berkas = session.createCriteria(Berkas.class).addOrder(Order.desc("jmlDipakai"))
					.add(Restrictions.isNotNull("jmlDipakai"))
					.add(Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))

			.setMaxResults(Common.MAX_RESULT).list();

			System.out.println(berkas);
			ListModel strset = new SimpleListModel(berkas);
			grid.setRowRenderer(new BerkasRenderer());
			grid.setModelCheckMobile(strset);

			// //grid.setOddRowSclass("non-odd");

		}

	}

}

