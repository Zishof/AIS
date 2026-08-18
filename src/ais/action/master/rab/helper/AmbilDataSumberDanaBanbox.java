package ais.action.master.rab.helper;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataSumberDanaBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;

	private EventListener eventListener;
	private SatuanKerja satuanKerja;
	private Integer tahun;
//	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	public AmbilDataSumberDanaBanbox() {
		super();
//		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		display();
	}

	private Textbox kode;
	private Textbox nama;

	class SumberDanaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final SumberDana sumberDana = (SumberDana) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataSumberDanaBanbox.this.setOpen(false);
					AmbilDataSumberDanaBanbox.this.setAttribute("sumberDana", sumberDana);
					AmbilDataSumberDanaBanbox.this.setAttribute("myValue", sumberDana);
					AmbilDataSumberDanaBanbox.this.setValue(sumberDana.getKode() + " - " + sumberDana.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(sumberDana.getKode()).setParent(arg0);
			new Label(sumberDana.getNama()).setParent(arg0);
			new Label(sumberDana.getTahun() == null ? "" : sumberDana.getTahun() + "").setParent(arg0);
			new Label(sumberDana.getTanggal() == null ? "" : Common.dateFormat2.get().format(sumberDana.getTanggal()))
					.setParent(arg0);
			new Label(sumberDana.getSatuanKerja() == null ? "" : sumberDana.getSatuanKerja().toString())
					.setParent(arg0);
			new Label(sumberDana.getPagu() == null ? "" : Common.numberFormat.get().format(sumberDana.getPagu()))
					.setParent(arg0);

		}

	}

	public void display() {
		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("600px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Sumber Dana");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode / Nomor"));
		row.appendChild(kode = new Textbox());
		kode.setWidth("90%");

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

		toolbar.appendChild(Common.createCleanButton(this, this));

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
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
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode / Nomor");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Satuan Kerja");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pagu");
		column.setWidth("10%");

		setSatuanKerja(Common.getCurrentUser() == null ? null : Common.getCurrentUser().ambilSatuanKerja(),
				ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(SumberDana.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		criteria.addOrder(Order.asc("nama"))
				.add(tahun == null ? Restrictions.sqlRestriction("1!=1") : Restrictions.eq("tahun", tahun))
				.add(Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kode.getText().trim(), MatchMode.ANYWHERE))
				.add(satuanKerja == null ? Restrictions.sqlRestriction("1!=1")
						: Restrictions.eq("satuanKerja", satuanKerja));
		List<SumberDana> sumberDana = criteria.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(sumberDana);
		grid.setRowRenderer(new SumberDanaRenderer());
		grid.setModelCheckMobile(strset);

		if (sumberDana.size() == 1) {
			setValue(sumberDana.get(0).getKode() + " - " + sumberDana.get(0).getNama());
			setAttribute("sumberDana", sumberDana.get(0));
			setAttribute("myValue", sumberDana.get(0));
			setDisabled(true);
		} else {
			setValue("");
			setAttribute("sumberDana", null);
			setAttribute("myValue", null);
			setDisabled(false);
		}

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public SatuanKerja getSatuanKerja() {
		return satuanKerja;
	}

	public void setSatuanKerja(SatuanKerja satuanKerja, Integer tahun) {
		this.satuanKerja = satuanKerja;
		this.tahun = tahun;
		onSearchDefault(null);
//		Boolean leaf = false;
//		try {
//			leaf = (satuanKerja != null && satuanKerjaTreeModel
//					.isLeaf(satuanKerja))
//					|| (satuanKerja != null && Common.getCurrentUser() != null
//							&& Common.getCurrentUser() == null ? null : Common
//							.getCurrentUser().getSatuanKerja() != null
//							&& satuanKerja.getId().equals(
//									Common.getCurrentUser() == null ? null
//											: Common.getCurrentUser()
//													.getSatuanKerja().getId()));
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/helper/AmbilDataSumberDanaBanbox.java:292");
//		}
//		setVisible(leaf);
//
//		try {
//			if (getParent() != null && getParent().getParent() != null) {
//				List<Component> components = new ArrayList<Component>();
//				getChilds(getParent().getParent(), components);
//				for (Component component : components) {
//					if (component instanceof Label) {
//						Label label = (Label) component;
//						if (label.getValue().equalsIgnoreCase("Sumber Dana")) {
//							label.setVisible(leaf);
//						}
//					}
//				}
//			}
//		} catch (Exception e) {
//		}
	}

//	@SuppressWarnings("unchecked")
//	private void getChilds(Component parent, List<Component> components) {
//		List<Component> myComponents = parent.getChildren();
//		for (Component component : myComponents) {
//			components.add(component);
//			getChilds(component, components);
//		}
//	}
}
