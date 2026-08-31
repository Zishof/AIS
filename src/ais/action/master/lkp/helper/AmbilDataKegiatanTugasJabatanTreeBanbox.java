package ais.action.master.lkp.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.lkp.util.KegiatanTugasJabatanTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Tipe khusus untuk ambil data kegiatan tugas jabatan tree banbox. Kelas ini memberi nama dan
 * batas tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tree tree}, {@code EventListener
 * eventListener}, {@code KegiatanTugasJabatanTreeModel kegiatanTugasJabatanTreeModel}, {@code Boolean
 * chooseAll}, {@code boolean hasDisplayed}, {@code SatuanKerja satuanKerja}, {@code String periode}, {@code List
 * tbmroles}; pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code
 * getEventListener()}); mutasi data ({@code setSatuanKerja()}, {@code setChooseAll()}); operasi domain lain
 * ({@code display()}); konfigurasi constructor: {@code kegiatanTugasJabatanTreeModel}. Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataKegiatanTugasJabatanTreeBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected KegiatanTugasJabatanTreeModel kegiatanTugasJabatanTreeModel;

	private Boolean chooseAll = false;
	private boolean hasDisplayed = false;
	private SatuanKerja satuanKerja;
	private String periode = KegiatanTugasJabatan.BULANAN;
	private List<Tbmrole> tbmroles = null;

	public void setSatuanKerja(SatuanKerja satuanKerja, List<Tbmrole> tbmroles) throws Exception {
		this.satuanKerja = satuanKerja;
		this.tbmroles = tbmroles;
		kegiatanTugasJabatanTreeModel.setSatuanKerja(satuanKerja, this.tbmroles);
		if (hasDisplayed) {
			onSearchDefault(null);
		}
	}

	public AmbilDataKegiatanTugasJabatanTreeBanbox() throws Exception {
		this(true, null);
	}

	public AmbilDataKegiatanTugasJabatanTreeBanbox(String periode) throws Exception {
		this(true, null);
		this.periode = periode;
	}

	public AmbilDataKegiatanTugasJabatanTreeBanbox(SatuanKerja satuanKerja) throws Exception {
		this(true, null);
		this.satuanKerja = satuanKerja;
	}

	public AmbilDataKegiatanTugasJabatanTreeBanbox(KegiatanTugasJabatan induk) throws Exception {
		this(true, induk);

	}

	public AmbilDataKegiatanTugasJabatanTreeBanbox(Boolean chooseAll, KegiatanTugasJabatan induk) throws Exception {
		super();
		kegiatanTugasJabatanTreeModel = new KegiatanTugasJabatanTreeModel(induk, false, periode);
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

	public void setChooseAll(Boolean chooseAll) throws Exception {
		this.chooseAll = chooseAll;
		// display();
	}

	class KegiatanTugasJabatanTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final KegiatanTugasJabatan kegiatanTugasJabatan = (KegiatanTugasJabatan) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);

				new Label(kegiatanTugasJabatan.getNama()).setParent(arg0);

				arg0 = new Treecell();
				arg0.setParent(treerow);
				MyRadioConfig checkbox = new MyRadioConfig();
				checkbox.setVisible(
						chooseAll || kegiatanTugasJabatanTreeModel.getChildCount(kegiatanTugasJabatan) == 0);
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("kegiatanTugasJabatan", kegiatanTugasJabatan);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setOpen(false);
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setAttribute("kegiatanTugasJabatan",
								kegiatanTugasJabatan);
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setValue(kegiatanTugasJabatan.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(KegiatanTugasJabatan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(kegiatanTugasJabatan.getId())).uniqueResult();
						count = count == null ? 0L : count;
						kegiatanTugasJabatan.setJmlDipakai(++count);
						Common.refreshUpdate(session, (kegiatanTugasJabatan));

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
		column.setLabel("Nama Kegiatan");

		column = new Treecol();
		column.setParent(columns);
		column.setLabel("Pilih");
		column.setWidth("10%");

		onSearchDefault(null);

		tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);
		tabpanelUtama.appendChild(new KegiatanTugasJabatanSeringDipakai());

	}

	public void onSearchDefault(Event event) throws Exception {

		tree.setModel(kegiatanTugasJabatanTreeModel);
		tree.setItemRenderer(new KegiatanTugasJabatanTreeRenderer());
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	private class KegiatanTugasJabatanSeringDipakai extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		public KegiatanTugasJabatanSeringDipakai() throws Exception {
			super();
			display();
		}

		private Textbox nama;

		class KegiatanTugasJabatanRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				// TODO Auto-generated method stub
				final KegiatanTugasJabatan kegiatanTugasJabatan = (KegiatanTugasJabatan) arg1;

				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setOpen(false);
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setAttribute("kegiatanTugasJabatan",
								kegiatanTugasJabatan);
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setAttribute("myValue", kegiatanTugasJabatan);
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setValue((kegiatanTugasJabatan.getNama()));
						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

				new Label(kegiatanTugasJabatan.getSatuanKerja() == null ? ""
						: kegiatanTugasJabatan.getSatuanKerja().getNama()).setParent(arg0);

				RevisiHelper.createNewRevisi(KegiatanTugasJabatan.class, kegiatanTugasJabatan,
						kegiatanTugasJabatan.getNama()).setParent(arg0);
				new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getAngkaKredit())).setParent(arg0);

				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getKuantitasDefault())).setParent(hbox);

				new Label(kegiatanTugasJabatan.getSatuanKuantitas() == null ? ""
						: kegiatanTugasJabatan.getSatuanKuantitas().getNama()).setParent(hbox);

				new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getKualitasDefault())).setParent(arg0);

				hbox = new Hbox();
				hbox.setParent(arg0);
				new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getWaktuDefault())).setParent(hbox);

				new Label(kegiatanTugasJabatan.getSatuanWaktu()).setParent(hbox);

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
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan"));
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
			 * client-side yang dibatasi MAX_RESULT_100. */
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
			column.setLabel("Satuan/Unit Kerja");
			column.setWidth("35%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama Kegiatan");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Angka Kredit");
			column.setWidth("8%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kuantitas");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kualitas");
			column.setWidth("8%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Waktu");
			column.setWidth("10%");

			onSearchDefault(null);

		}

		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) throws Exception {

			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser.ambilPegawai() != null) {
				if (tbmroles == null || tbmroles.isEmpty()) {
					tbmroles = new ArrayList<Tbmrole>();
					tbmroles.add(tbmuser.hakAkses());
				}
			}

			Criterion criterion = satuanKerja == null ? Restrictions.sqlRestriction("false")
					: Restrictions.eq("satuanKerja", satuanKerja);

			Session session = HibernateUtil.currentSession();
			List<KegiatanTugasJabatan> kegiatanTugasJabatans =

					
									session.createCriteria(KegiatanTugasJabatan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))

											.add(periode.equals(KegiatanTugasJabatan.BULANAN)
													? Restrictions.or(Restrictions.isNull("periode"),
															Restrictions.eq("periode", periode))
													: Restrictions.eq("periode", periode))

											.add(Restrictions.or(criterion,
													tbmroles == null || tbmroles.isEmpty()
															? Restrictions.sqlRestriction("false")
															: Restrictions.or(
																	Restrictions.and(criterion,
																			Restrictions.isNull("userRole")),
																	Restrictions.in("userRole", tbmroles))))

											.add(nama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
													: Restrictions.ilike("nama", nama.getValue().trim(),
															MatchMode.ANYWHERE))
											.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("jmlDipakai"))
											.setMaxResults(Common.MAX_RESULT).list();
			ListModel strset = new SimpleListModel(kegiatanTugasJabatans);
			grid.setRowRenderer(new KegiatanTugasJabatanRenderer());
			grid.setModelCheckMobile(strset);

		}

	}

}
