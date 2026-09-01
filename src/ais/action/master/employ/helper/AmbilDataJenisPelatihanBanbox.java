package ais.action.master.employ.helper;

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

import ais.action.master.employ.util.JenisPelatihanTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.employ.JenisPelatihan;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity
 * {@link ais.database.model.employ.JenisPelatihan} — lihat {@link ais.ui.util.GetEventListener}
 * untuk arsitektur kerangka umum (constructor/display/onSearchDefault/renderer/callback). {@code
 * JenisPelatihan} adalah master data jenis/kategori pelatihan pegawai, berbentuk hierarki
 * pohon (punya {@code parent}) — mis. "Pelatihan Teknis" &rarr; "Pelatihan Jaringan".
 * <p>
 * <b>Menyimpang dari kerangka standar</b>: popup tidak memakai grid pencarian teks, melainkan dua
 * tab: tab "Daftar" menampilkan seluruh hierarki sebagai {@link Tree} (model
 * {@link JenisPelatihanTreeModel}, renderer {@link JenisPelatihanTreeRenderer}), dan tab "Sering
 * Dipakai" ({@link JenisPelatihanSeringDipakai}) menampilkan grid pencarian nama biasa diurutkan
 * berdasarkan {@code jmlDipakai} (jumlah pemakaian) menurun. Mode pilih: {@link #chooseAll}
 * (constructor {@link #AmbilDataJenisPelatihanBanbox(Boolean)}) menentukan apakah node non-daun
 * (punya anak) boleh dipilih ({@code true}, default) atau hanya node daun yang boleh dipilih
 * ({@code false}, checkbox radio node non-daun disembunyikan). Setiap kali item dipilih (dari
 * pohon maupun tab "Sering Dipakai"), counter {@code jmlDipakai} dinaikkan satu lewat
 * {@code Common.refreshUpdate} sebelum {@code eventListener} dipanggil.
 *
 * @see Bandbox
 */
public class AmbilDataJenisPelatihanBanbox extends Bandbox implements GetEventListener {

	/**
	 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected JenisPelatihanTreeModel jenisPelatihanTreeModel;

	private Boolean chooseAll = false;

	/** Constructor default: {@link #chooseAll} diaktifkan (node non-daun boleh dipilih). */
	public AmbilDataJenisPelatihanBanbox() throws Exception {
		this(true);
	}

	/**
	 * Membangun komponen dan LANGSUNG memanggil {@link #display()} di constructor (berbeda dari
	 * kerangka standar {@link ais.ui.util.GetEventListener} yang menunda pembangunan popup ke
	 * listener {@code onOpen} pembukaan pertama).
	 *
	 * @param chooseAll {@code true} untuk mengizinkan pemilihan node non-daun pada pohon,
	 *                  {@code false} untuk hanya mengizinkan node daun (tanpa anak)
	 */
	public AmbilDataJenisPelatihanBanbox(Boolean chooseAll) throws Exception {
		super();
		jenisPelatihanTreeModel = new JenisPelatihanTreeModel(false);
		this.chooseAll = chooseAll;
		setReadonly(true);
		display();

	}

	/**
	 * Mengubah mode pilih node non-daun/daun untuk pemakaian berikutnya. Catatan: pohon yang
	 * sudah terlanjur dirender TIDAK dibangun ulang di sini (pemanggilan {@code display()}
	 * sengaja dikomentari) — perubahan baru terlihat efektif bila dipanggil sebelum popup pertama
	 * dibuka.
	 *
	 * @param chooseAll {@code true} untuk mengizinkan pemilihan node non-daun
	 */
	public void setChooseAll(Boolean chooseAll) throws Exception {
		this.chooseAll = chooseAll;
		// display();
	}

	/**
	 * Merender satu node pohon jenis pelatihan: label nama (via {@code toString()}) di kolom
	 * pertama, dan radio pilih di kolom kedua — disembunyikan bila node masih punya anak dan
	 * {@link #chooseAll} bernilai {@code false} (aturan "hanya daun"). Memilih radio menutup
	 * popup, menyimpan entity {@link JenisPelatihan} terpilih ke attribute {@code "jenisPelatihan"}
	 * pada Bandbox, mengisi teks tampilan, menaikkan counter {@code jmlDipakai} lewat
	 * {@code Common.refreshUpdate}, lalu memicu {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataJenisPelatihanBanbox
	 */
	class JenisPelatihanTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final JenisPelatihan jenisPelatihan = (JenisPelatihan) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);

				new Label(jenisPelatihan.toString()).setParent(arg0);

				arg0 = new Treecell();
				arg0.setParent(treerow);
				MyRadioConfig checkbox = new MyRadioConfig();
				checkbox.setVisible(chooseAll || jenisPelatihanTreeModel.getChildCount(jenisPelatihan) == 0);
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("jenisPelatihan", jenisPelatihan);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataJenisPelatihanBanbox.this.setOpen(false);
						AmbilDataJenisPelatihanBanbox.this.setAttribute("jenisPelatihan", jenisPelatihan);
						AmbilDataJenisPelatihanBanbox.this.setValue(jenisPelatihan.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(JenisPelatihan.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(jenisPelatihan.getId())).uniqueResult();
						count = count == null ? 0L : count;
						jenisPelatihan.setJmlDipakai(++count);
						Common.refreshUpdate(session, (jenisPelatihan));

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
	 * Membangun popup (dipanggil langsung dari constructor, bukan lewat {@code onOpen}): tab
	 * "Daftar" berisi {@link Tree} hierarki penuh (dimuat lewat {@link #onSearchDefault(Event)}),
	 * dan tab "Sering Dipakai" berisi {@link JenisPelatihanSeringDipakai}.
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
		tabpanelUtama.appendChild(new JenisPelatihanSeringDipakai());

	}

	/**
	 * Memasang ulang {@link #jenisPelatihanTreeModel} dan {@link JenisPelatihanTreeRenderer} ke
	 * {@link #tree}. Tidak melakukan query database sendiri — seluruh hierarki sudah dimuat oleh
	 * {@link JenisPelatihanTreeModel}.
	 *
	 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
	 */
	public void onSearchDefault(Event event) throws Exception {

		tree.setModel(jenisPelatihanTreeModel);
		tree.setItemRenderer(new JenisPelatihanTreeRenderer());
	}

	/** @param eventListener dipanggil setiap kali user memilih satu jenis pelatihan */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan jenis pelatihan yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Isi tab "Sering Dipakai" pada popup {@link AmbilDataJenisPelatihanBanbox}: grid pencarian
	 * nama biasa (bukan pohon) atas {@link JenisPelatihan} yang ditandai {@code defaultItem=true},
	 * diurutkan menurun berdasarkan {@code jmlDipakai} sehingga item paling sering dipilih tampil
	 * di atas — mempercepat pemilihan jenis pelatihan umum tanpa perlu menelusuri hierarki pohon.
	 *
	 * @see AmbilDataJenisPelatihanBanbox
	 */
	private class JenisPelatihanSeringDipakai extends Borderlayout {

		/**
		 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		/** Membuat panel dan langsung menyusun isinya lewat {@link #display()}. */
		public JenisPelatihanSeringDipakai() throws Exception {
			super();
			display();
		}

		private Textbox nama;

		/**
		 * Merender satu baris grid "Sering Dipakai": nama jenis pelatihan dan nama parent-nya
		 * (kosong bila tanpa parent). Mengklik baris (bukan radio — seluruh baris klikabel)
		 * menutup popup induk, menyimpan entity {@link JenisPelatihan} terpilih ke attribute
		 * {@code "jenisPelatihan"} pada {@link AmbilDataJenisPelatihanBanbox}, mengisi teks
		 * tampilan, menaikkan counter {@code jmlDipakai}, lalu memicu {@code eventListener} milik
		 * kelas induk bila terpasang.
		 *
		 * @see JenisPelatihanSeringDipakai
		 */
		class JenisPelatihanRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				// TODO Auto-generated method stub
				final JenisPelatihan jenisPelatihan = (JenisPelatihan) arg1;

				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataJenisPelatihanBanbox.this.setOpen(false);
						AmbilDataJenisPelatihanBanbox.this.setAttribute("jenisPelatihan", jenisPelatihan);
						AmbilDataJenisPelatihanBanbox.this.setValue(jenisPelatihan.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(JenisPelatihan.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(jenisPelatihan.getId())).uniqueResult();
						count = count == null ? 0L : count;
						jenisPelatihan.setJmlDipakai(++count);
						Common.refreshUpdate(session, (jenisPelatihan));

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

				new Label(jenisPelatihan.getNama()).setParent(arg0);
				new Label(jenisPelatihan.getParent() == null ? "" : jenisPelatihan.getParent().getNama())
						.setParent(arg0);

			}

		}

		/**
		 * Membangun tata letak panel "Sering Dipakai": form filter Nama, grid hasil bermold
		 * "paging" (kolom Nama dan Parent), lalu memuat data awal lewat
		 * {@link #onSearchDefault(Event)}.
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
		 * Mengambil {@link JenisPelatihan} yang ditandai {@code defaultItem=true}, diurutkan
		 * menurun berdasarkan {@code jmlDipakai}, dibatasi {@link Common#MAX_RESULT} baris.
		 * Catatan: isian {@link #nama} TIDAK dipakai sebagai filter di sini — tombol "Cari"
		 * hanya memuat ulang daftar item default yang sama, bukan pencarian bebas teks. Mengisi
		 * ulang grid dengan hasilnya beserta {@link JenisPelatihanRenderer}.
		 *
		 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
		 */
		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) throws Exception {

			Session session = HibernateUtil.currentSession();
			List<JenisPelatihan> jenisPelatihans = session.createCriteria(JenisPelatihan.class)
					.add(Restrictions.eq("defaultItem", true)).addOrder(Order.desc("jmlDipakai"))
					.setMaxResults(Common.MAX_RESULT).list();
			ListModel strset = new SimpleListModel(jenisPelatihans);
			grid.setRowRenderer(new JenisPelatihanRenderer());
			grid.setModelCheckMobile(strset);

		}

	}

}
