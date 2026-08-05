package ais.action.master.sirs.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.ui.util.MyTextbox;

public class AmbilDataLokasiBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;

	private EventListener eventListener;

	public AmbilDataLokasiBanbox() {
		super();
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("lokasi", null);
					setValue("");
					return;
				}

				Lokasi lokasi = (Lokasi) HibernateUtil.currentSession().createCriteria(Lokasi.class)
						.add(Restrictions.ilike("nama", AmbilDataLokasiBanbox.this.getValue().trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (lokasi == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data Lokasi dengan nama \"{V1}\" tidak ditemukan di dalam sistem. Langkah yang dapat Bapak/Ibu lakukan: (1) periksa kembali ketepatan penulisan nama Lokasi yang dimasukkan; (2) pastikan data Lokasi tersebut telah terdaftar pada sistem; (3) gunakan fitur pencarian pada tabel untuk menelusuri data yang tersedia.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							AmbilDataLokasiBanbox.this.getValue().trim());
					return;
				}
				AmbilDataLokasiBanbox.this.setOpen(false);
				AmbilDataLokasiBanbox.this.setAttribute("lokasi", lokasi);
				AmbilDataLokasiBanbox.this.setValue(lokasi.getNama());
				if (eventListener != null) {
					eventListener.onEvent(arg0);
				}
			}
		});

		display();

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (grid == null || grid.getRows() == null || grid.getRows().getChildren() == null
						|| grid.getRows().getChildren().size() == 0) {
					onSearchDefault(null);
				}
			}
		});
	}

	private MyTextbox nama;

	class LokasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Lokasi lokasi = (Lokasi) arg1;
			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataLokasiBanbox.this.setOpen(false);
					AmbilDataLokasiBanbox.this.setAttribute("lokasi", lokasi);
					AmbilDataLokasiBanbox.this.setValue(lokasi.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(lokasi.getNama()).setParent(arg0);
			new Label(lokasi.getKeterangan()).setParent(arg0);

		}

	}

	public void display() {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("95%");
		bandpopup.setHeight("600px");

		Panel panel = new Panel();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Lokasi");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new Borderlayout();
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

		Grid searchgrid = new Grid();
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);
		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Lokasi")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

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
		toolbar.appendChild(Common.createCleanButton(this, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					try {
						eventListener.onEvent(null);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataLokasiBanbox.java:193");
					}
				}
				onSearchDefault(event);
			}
		}));

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
		column.setLabel("Nama Lokasi");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Lokasi> lokasi = ConstantValues.simpleList(session.createCriteria(Lokasi.class).addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setMaxResults(Common.MAX_RESULT), Lokasi.class);

		System.out.println(lokasi);
		ListModel strset = new SimpleListModel(lokasi);
		grid.setRowRenderer(new LokasiRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
