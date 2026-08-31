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
 * Tipe khusus untuk ambil data berkas banbox. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tree tree}, {@code EventListener
 * eventListener}, {@code BerkasTreeModel berkasTreeModel}, {@code Boolean bisaDipilihSemua}; pembacaan/pencarian
 * ({@code onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); operasi domain lain
 * ({@code display()}); konfigurasi constructor: {@code count}. Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataBerkasBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected BerkasTreeModel berkasTreeModel;

	private Boolean bisaDipilihSemua = false;

	public AmbilDataBerkasBanbox() {
		this(false);
	}

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
	 * Renderer lokal untuk layar/komponen {@link AmbilDataBerkasBanbox}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataBerkasBanbox} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
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

	public void onSearchDefault(Event event) {
		berkasTreeModel = new BerkasTreeModel();
		tree.setModel(berkasTreeModel);
		tree.setItemRenderer(new BerkasTreeRenderer());
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Tipe implementasi bersarang {@link BerkasSeringDipakai} milik {@link AmbilDataBerkasBanbox}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataBerkasBanbox} dan dapat mengakses
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
	 * @see AmbilDataBerkasBanbox
	 */
	private class BerkasSeringDipakai extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		public BerkasSeringDipakai() {
			super();
			display();
		}

		private Textbox nama;

		/**
		 * Renderer lokal untuk layar/komponen {@link BerkasSeringDipakai}. Kelas ini menerjemahkan satu item data
		 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link BerkasSeringDipakai} dan dapat mengakses state
		 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
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

