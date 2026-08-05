package ais.action.master.akunting.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
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

import ais.action.master.akunting.util.AkunTreeModel;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataBanyakAkun extends MyWindow {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected AkunTreeModel akunTreeModel;

	protected Integer debetCredit = null;
	private List<Akun> akuns;
	private Map<Long, Checkbox> checkes = new HashMap<Long, Checkbox>();

	private boolean bolehPilihLihatSemuaAkun = Common.bolehKonfigurasi("bolehPilihLihatSemuaAkun", Konfigurasi.TIDAK_AKTIF);

	public AmbilDataBanyakAkun(List<Akun> akuns, EventListener eventListener) {
		super();
		this.eventListener = eventListener;
		this.akuns = akuns;
		display();
	}

	class AkunTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final Akun akun = (Akun) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				final Checkbox checkbox = new Checkbox(akun.getKode() + " - " + akun.getNama());
				checkbox.setChecked(akuns.contains(akun));
				checkes.put(akun.getId(), checkbox);
				checkbox.setAttribute("akun", akun);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						boolean boleh = akunTreeModel.getChildCount(akun) != 0;

						if (boleh) {
							Set<Akun> akuns = new HashSet<Akun>();
							akuns.add(akun);
							akunTreeModel.generateAllChildren(akun, akuns);

							List<Long> pilih = new ArrayList<Long>();
							List<Akun> akuns2 = new ArrayList<Akun>();
							for (Akun a : akuns) {
								boolean tidakada = akunTreeModel.getChildCount(a) == 0;
								if (tidakada) {
									akuns2.add(a);
									pilih.add(a.getId());
								}
							}

							if (checkbox.isChecked()) {
								AmbilDataBanyakAkun.this.akuns.addAll(akuns2);

								for (Long a : checkes.keySet()) {
									if (pilih.contains(a)) {
										checkes.get(a).setChecked(true);
									}
								}

							} else if (!checkbox.isChecked()) {

								for (Long a : checkes.keySet()) {
									if (pilih.contains(a)) {
										checkes.get(a).setChecked(false);
									}
								}

								AmbilDataBanyakAkun.this.akuns.removeAll(akuns2);
							}

						} else {

							if (checkbox.isChecked() && !akuns.contains(akun)) {
								AmbilDataBanyakAkun.this.akuns.add(akun);
							} else if (!checkbox.isChecked()) {
								AmbilDataBanyakAkun.this.akuns.remove(akun);
							}
						}
					}
				});

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.setParent(arg0);

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

		}

	}

	public void display() {

		setWidth("900px");
		setHeight("95%");

		Borderlayout borderlayoutMaster = new ais.ui.util.MyBorderlayout();
		borderlayoutMaster.setParent(this);

		Center centerMaster = new Center();
		centerMaster.setParent(borderlayoutMaster);
		ais.ui.util.ZkCompat.setFlex(centerMaster, true);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(centerMaster);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Akun");
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

		final MyTabConfig tabSoal = new MyTabConfig("Daftar Akun");
		tabSoal.setParent(tabs);

		MyTabConfig tabJawaban = new MyTabConfig("Akun Sering Dapakai");
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
		column.setLabel("Akun");

		onSearchDefault(null);

		final Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
		tabpanelUtama1.setHeight("450px");
		tabpanelUtama1.setWidth("100%");
		tabpanelUtama1.setParent(tabpanels);
		tabJawaban.addEventListener("onClick", new EventListener() {
			
			@Override
			public void onEvent(Event arg0) throws Exception {
				if(tabpanelUtama1.getChildren().isEmpty()) {
					tabpanelUtama1.appendChild(new AkunSeringDipakai());
				}
				
			}
		});

		South south = new South();
		south.setParent(borderlayoutMaster);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				detach();
			}
		});
		button.setParent(toolbar);

		final MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				eventListener.onEvent(new Event("", save, akuns));
				detach();
			}
		});
		save.setParent(toolbar);

	}

	public void onSearchDefault(Event event) {
		akunTreeModel = new AkunTreeModel(debetCredit);
		tree.setModel(akunTreeModel);
		tree.setItemRenderer(new AkunTreeRenderer());
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	private class AkunSeringDipakai extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		public AkunSeringDipakai() {
			super();
			try {
				display();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}

		private Textbox kodeAkunan;
		private Textbox nama;
		private AmbilDataSatuanKerjaBanbox satuanKerja;

		class AkunRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				// TODO Auto-generated method stub
				final Akun akun = (Akun) arg1;

				final MyCheckboxConfig checkbox = new MyCheckboxConfig();
				checkbox.setVisible(bolehPilihLihatSemuaAkun || akunTreeModel.getChildCount(akun) == 0);
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.setChecked(akuns.contains(akun));
				checkbox.setAttribute("akun", akun);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (checkbox.isChecked() && !akuns.contains(akun)) {
							akuns.add(akun);
						} else if (!checkbox.isChecked()) {
							akuns.remove(akun);
						}
					}
				});

				new Label(akun.getKode()).setParent(arg0);
				new Label(akun.getNama()).setParent(arg0);
				new Label(akun.getDebetCredit() == null ? ""
						: akun.getDebetCredit().equals(Akun.DEBET) ? "Debet" : "Credit").setParent(arg0);
				new Label(akun.getSatuanKerja() == null ? "" : akun.getSatuanKerja().getNama()).setParent(arg0);
				new Label(akun.getBank() == null ? "" : akun.getBank().getNama()).setParent(arg0);

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
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode Akun"));
			row.appendChild(kodeAkunan = new Textbox());
			kodeAkunan.setWidth("90%");

			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Akun"));
			row.appendChild(nama = new Textbox());
			nama.setWidth("90%");

			row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja Akun"));
			row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
			satuanKerja.setWidth("90%");

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

			grid = new MyGrid();
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
			column.setLabel("");
			column.setWidth("40px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kode Akun");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama Akun");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Debet/Kredit");
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Satuan Kerja");
			column.setWidth("20%");
			
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Bank");
			column.setWidth("15%");

			onSearchDefault(null);

		}

		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) {

			SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");

			Session session = HibernateUtil.currentSession();
			List<Akun> akun = session.createCriteria(Akun.class)
					.add(debetCredit == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("debetCredit", debetCredit))
					.addOrder(Order.asc("kode"))
					.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
					.add(kodeAkunan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("kode", kodeAkunan.getValue().trim(), MatchMode.ANYWHERE))

					.add(satuanKerja == null ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.isNull("satuanKerja"),
									Restrictions.eq("satuanKerja", satuanKerja)))

					.setMaxResults(Common.MAX_RESULT).list();

			System.out.println(akun);
			ListModel strset = new SimpleListModel(akun);
			grid.setRowRenderer(new AkunRenderer());
			grid.setModelCheckMobile(strset);

			// //grid.setOddRowSclass("non-odd");

		}

	}

}
