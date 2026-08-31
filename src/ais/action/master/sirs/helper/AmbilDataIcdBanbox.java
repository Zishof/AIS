package ais.action.master.sirs.helper;

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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.util.IcdTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyMessageboxConfig;
import ais.database.model.sirs.Icd;

/**
 * Tipe khusus untuk ambil data icd banbox. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tree tree}, {@code EventListener
 * eventListener}, {@code IcdTreeModel icdTreeModel}, {@code Boolean telahDibuka}, {@code IcdSeringDipakai
 * icdSeringDipakai}; pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code
 * getEventListener()}); operasi domain lain ({@code display()}); konfigurasi constructor: {@code count}, {@code
 * telahDibuka}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataIcdBanbox extends Bandbox {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected IcdTreeModel icdTreeModel;
	protected Boolean telahDibuka = false;
	private IcdSeringDipakai icdSeringDipakai;

	public AmbilDataIcdBanbox() {
		super();
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Icd icd = (Icd) HibernateUtil.currentSession().createCriteria(Icd.class)
						.add(Restrictions.ilike("kode", AmbilDataIcdBanbox.this.getValue().trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (icd == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data ICD dengan kode \"{V1}\" tidak ditemukan. Langkah yang dapat dilakukan: (1) periksa kembali penulisan kode ICD; (2) gunakan tombol pencarian untuk memilih dari daftar yang tersedia; (3) pastikan data ICD telah terdaftar di dalam sistem.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							AmbilDataIcdBanbox.this.getValue().trim());
					return;
				}
				AmbilDataIcdBanbox.this.setOpen(false);
				AmbilDataIcdBanbox.this.setAttribute("icd", icd);
				AmbilDataIcdBanbox.this.setValue(icd.toString());

				Session session = HibernateUtil.currentSession();
				Long count = (Long) session.createCriteria(Icd.class).setProjection(Projections.property("jmlDipakai"))
						.add(Restrictions.idEq(icd.getId())).uniqueResult();
				count = count == null ? 0L : count;
				icd.setJmlDipakai(++count);
				session.update(icd);

				if (eventListener != null) {
					eventListener.onEvent(arg0);
				}
			}
		});
		display();

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (!telahDibuka) {
					onSearchDefault(null);
					telahDibuka = true;
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataIcdBanbox}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataIcdBanbox} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataIcdBanbox
	 */
	class IcdTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final Icd icd = (Icd) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);
				RevisiHelper.createNewRevisi(Icd.class, icd, icd.toString()).setParent(arg0);

				arg0 = new Treecell();
				arg0.setParent(treerow);
				Radio checkbox = new Radio();
				checkbox.setVisible(icdTreeModel.getChildCount(icd) == 0);
				checkbox.setParent(arg0);
				checkbox.setAttribute("icd", icd);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataIcdBanbox.this.setOpen(false);
						AmbilDataIcdBanbox.this.setAttribute("icd", icd);
						AmbilDataIcdBanbox.this.setValue(icd.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(Icd.class)
								.setProjection(Projections.property("jmlDipakai")).add(Restrictions.idEq(icd.getId()))
								.uniqueResult();
						count = count == null ? 0L : count;
						icd.setJmlDipakai(++count);
						session.update(icd);

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataIcdBanbox.java:153");
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

		Panel panel = new Panel();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Icd");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		// Tabbox tabbox = new Tabbox();
		// tabbox.setParent(panelchildren);
		// tabbox.setHeight("100%");
		// tabbox.setWidth("100%");
		//
		// Tabs tabs = new Tabs();
		// tabs.setParent(tabbox);
		//
		// final Tab tabSoal = new Tab("Daftar Penyakit");
		// tabSoal.setParent(tabs);
		//
		// Tab tabJababan = new Tab("Kode Penyakit Sering Dapakai");
		// tabJababan.setParent(tabs);
		//
		// Tabpanels tabpanels = new Tabpanels();
		// tabpanels.setParent(tabbox);
		//
		// Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		// tabpanelUtama.setParent(tabpanels);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		//
		// tree = new Tree();
		// tree.setZclass("z-vfiletree");
		// tree.setParent(center);
		//
		// Treecols columns = new Treecols();
		//
		// columns.setParent(tree);
		//
		// Treecol column = new Treecol();
		// column.setParent(columns);
		// column.setLabel("Icd");
		// column.setWidth("85%");
		//
		// column = new Treecol();
		// column.setParent(columns);
		// column.setLabel("Pilih");
		//
		// tabpanelUtama = new ais.ui.util.MyTabpanel();
		// tabpanelUtama.setParent(tabpanels);
		center.appendChild(icdSeringDipakai = new IcdSeringDipakai());

	}

	public void onSearchDefault(Event event) {
		// icdTreeModel = new IcdTreeModel();
		// tree.setModel(icdTreeModel);
		// tree.setItemRenderer(new IcdTreeRenderer());
		icdSeringDipakai.onSearchDefault(event);
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Tipe implementasi bersarang {@link IcdSeringDipakai} milik {@link AmbilDataIcdBanbox}. Kelas ini memberi
	 * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataIcdBanbox} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
	 * implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Grid grid}, {@code Textbox
	 * kodeIcdan}, {@code Textbox nama}; operasi lokal: {@code display()}, {@code onSearchDefault}(). Aturan bisnis
	 * bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see AmbilDataIcdBanbox
	 */
	private class IcdSeringDipakai extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private Grid grid;

		public IcdSeringDipakai() {
			super();
			display();
		}

		private Textbox kodeIcdan;
		private Textbox nama;

		/**
		 * Renderer lokal untuk layar/komponen {@link IcdSeringDipakai}. Kelas ini menerjemahkan satu item data menjadi
		 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link IcdSeringDipakai} dan dapat mengakses state
		 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see IcdSeringDipakai
		 */
		class IcdRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				// TODO Auto-generated method stub
				final Icd icd = (Icd) arg1;

				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataIcdBanbox.this.setOpen(false);
						AmbilDataIcdBanbox.this.setAttribute("icd", icd);
						AmbilDataIcdBanbox.this.setValue(icd.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(Icd.class)
								.setProjection(Projections.property("jmlDipakai")).add(Restrictions.idEq(icd.getId()))
								.uniqueResult();
						count = count == null ? 0L : count;
						icd.setJmlDipakai(++count);
						session.update(icd);

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

				new Label(icd.getKode()).setParent(arg0);
				new Label(icd.getNama_english()).setParent(arg0);

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

			Grid searchgrid = new Grid();
			searchgrid.setParent(rowUtama);

			Rows rows = new Rows();
			rows.setParent(searchgrid);

			Row row = new Row();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Icd")));
			row.appendChild(kodeIcdan = new Textbox());
			kodeIcdan.setWidth("90%");

			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Icd")));
			row.appendChild(nama = new Textbox());
			nama.setWidth("90%");

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Cari", "/img/search.gif");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			});
			button.setParent(toolbar);

			grid = new Grid();
			grid.setMold("paging");
			grid.setPageSize(25);
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
			column.setLabel("Kode Icd");
			column.setWidth("15%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Nama Icd");

			// onSearchDefault(null);

		}

		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) {

			Session session = HibernateUtil.currentSession();
			List<Icd> icd = ConstantValues.simpleList(session.createCriteria(Icd.class)
					.addOrder(Order.desc("jmlDipakai")).add(Restrictions.isNotNull("jmlDipakai"))
					.add(Restrictions.ilike("nama_english", nama.getText().trim(), MatchMode.ANYWHERE))
					.add(Restrictions.ilike("kode", kodeIcdan.getText().trim(), MatchMode.ANYWHERE))

					.setMaxResults(Common.MAX_RESULT), Icd.class);

			System.out.println(icd);
			ListModel strset = new SimpleListModel(icd);
			grid.setRowRenderer(new IcdRenderer());
			grid.setModel(strset);

			grid.renderAll();

		}

	}

}
