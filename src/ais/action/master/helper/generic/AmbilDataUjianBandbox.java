package ais.action.master.helper.generic;

import java.util.List;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.Ujian;
import ais.database.model.UjianPunyaSoal;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataUjianBandbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	private MyTextbox nama;
	private Combobox searchjenis;
	private String jenis;

	private Combobox searchlevel;
	private MyTextbox keterangan;


	public AmbilDataUjianBandbox() {
		super();

		setReadonly(true);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					display();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
							
							
							onSearchDefault(null);
						}
					});
				}

			}
		});

	}

	class UjianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Ujian ujian = (Ujian) arg1;
			arg0.setAttribute("ujian", ujian);

			final Radio checkbox = new Radio(ujian.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataUjianBandbox.this.setOpen(false);
					AmbilDataUjianBandbox.this.setAttribute("ujian", ujian);
					AmbilDataUjianBandbox.this.setAttribute("myValue", ujian);
					AmbilDataUjianBandbox.this.setValue(ujian.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(Common.getBahasaConfig(ujian.getJenis()) + " / " + Common.getBahasaConfig(ujian.getLevel())
					+ " / " + Common.numberFormat.get().format(ujian.getNilaiLulus())).setParent(arg0);

			new Label(ujian.getSertifikat() == null ? "" : ujian.getSertifikat().getNama()).setParent(arg0);
			new Label(ujian.getSyaratUjian() == null ? "" : ujian.getSyaratUjian().getNama()).setParent(arg0);

			new Label(ujian.getTanggal_dirubah() == null ? "" : Common.dateFormat3.get().format(ujian.getTanggal_dirubah()))
					.setParent(arg0);

			int count = ((Number) HibernateUtil.currentSession().createCriteria(UjianPunyaSoal.class)
					.add(Restrictions.eq("ujian", ujian)).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
			new Label(Common.numberFormat.get().format(count)).setParent(arg0);
		}

	}

	public void display() {


		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("900px");
		bandpopup.setHeight("400px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Ujian");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		// Pager TUNGGAL: AmbilDataPagingHelper.pasangGridDanPaging(myCenter1, grid) sudah menampilkan
		// grid + pager server-side sendiri. Pager manual South dihapus (double paging).

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Level"));
		row.appendChild(searchlevel = new Combobox());
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Semua Level");
		comboitem.setValue(null);
		searchlevel.appendChild(comboitem);

		for (int i = 1; i <= 10; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Level " + i);
			comboitem.setValue("Level " + i);
			searchlevel.appendChild(comboitem);
		}

		searchlevel.setSelectedIndex(0);
		searchlevel.setWidth("90%");
		searchlevel.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		row.appendChild(searchjenis = new Combobox());
		comboitem = new MyComboitemConfig(BankSoal.PILIHAN_GANDA);
		comboitem.setValue(BankSoal.PILIHAN_GANDA);
		searchjenis.appendChild(comboitem);
		comboitem = new MyComboitemConfig(BankSoal.ESAY);
		comboitem.setValue(BankSoal.ESAY);
		searchjenis.appendChild(comboitem);
		searchjenis.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		if (jenis != null) {
			Common.selectComboItem(searchjenis, jenis);
			searchjenis.setDisabled(true);
		}
		searchjenis.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ujian"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox());
		keterangan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

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
		pagingHelper.pasangOnPaging(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(myCenter1, grid);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Ujian");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sertifikat");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Syarat");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Wkt. Dibuat");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah Soal");
		column.setWidth("10%");

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Ujian.class)

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(keterangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", keterangan.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchlevel.getSelectedItem() == null || searchlevel.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("level", searchlevel.getSelectedItem().getValue()))

				.add(searchjenis.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenis", searchjenis.getSelectedItem().getValue()));

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		// AmbilDataPagingHelper mengelola total + offset sendiri via pager server-side-nya.
		List<Ujian> myUjian = pagingHelper.cariDenganCriteria(initCriteria(true), Ujian.class);

		ListModel strset = new SimpleListModel(myUjian);
		grid.setRowRenderer(new UjianRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
