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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
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
import ais.ui.util.MyMessageboxConfig;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JenisItemMedis;
import ais.ui.util.MyTextbox;

public class AmbilDataItemMedisBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;
	private EventListener eventListener;

	public AmbilDataItemMedisBanbox() {
		super();
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("item", null);
					setValue("");
					return;
				}

				ItemMedis item = (ItemMedis) HibernateUtil
						.currentSession().createCriteria(ItemMedis.class).add(Restrictions.ilike("kode",
								AmbilDataItemMedisBanbox.this.getValue().trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (item == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data Item Medis dengan kode \"{V1}\" tidak ditemukan. Langkah yang dapat dilakukan: (1) periksa kembali penulisan kode item medis; (2) gunakan tombol pencarian untuk memilih dari daftar yang tersedia; (3) pastikan data item medis telah terdaftar di dalam sistem.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							AmbilDataItemMedisBanbox.this.getValue().trim());
					return;
				}
				AmbilDataItemMedisBanbox.this.setOpen(false);
				AmbilDataItemMedisBanbox.this.setAttribute("item", item);
				AmbilDataItemMedisBanbox.this.setValue(item.getKode() + "-" + item.getNama());
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

	private MyTextbox kodeItemMedisan;
	private MyTextbox nama;
	private Combobox jenisItemMedis;

	class ItemMedisRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final ItemMedis item = (ItemMedis) arg1;

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataItemMedisBanbox.this.setOpen(false);
					AmbilDataItemMedisBanbox.this.setAttribute("item", item);
					AmbilDataItemMedisBanbox.this.setValue(item.getKode() + "-" + item.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(item.getKode()).setParent(arg0);
			new Label(item.getNama()).setParent(arg0);
			new Label(item.getKandungan()).setParent(arg0);
			new Label(item.getJenisItem() == null ? "" : item.getJenisItem().getNama()).setParent(arg0);
			new Label(item.getKelompokItem() == null ? "" : item.getKelompokItem().getNama()).setParent(arg0);
			new Label(item.getKelasItem() == null ? "" : item.getKelasItem().getNama()).setParent(arg0);
			new Label(item.getGenerikItem() == null ? "" : item.getGenerikItem().getNama()).setParent(arg0);
			new Label(item.getSatuanItem() == null ? "" : item.getSatuanItem().getNama()).setParent(arg0);

		}

	}

	public void display() {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		Panel panel = new Panel();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar ItemMedis");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Barang Medis")));
		row.appendChild(kodeItemMedisan = new MyTextbox());
		kodeItemMedisan.setWidth("90%");
		kodeItemMedisan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Barang Medis")));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Barang Medis")));
		row.appendChild(jenisItemMedis = new Combobox());
		Common.insertCombo(jenisItemMedis, "nama", JenisItemMedis.class);
		jenisItemMedis.setWidth("90%");
		jenisItemMedis.addEventListener(Events.ON_CHANGE, new EventListener() {
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
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataItemMedisBanbox.java:230");
					}
				}
				onSearchDefault(event);
			}
		}));

		// final Radiogroup radiogroup = new Radiogroup();
		// radiogroup.setWidth("100%");
		// radiogroup.setHeight("100%");
		// radiogroup.setParent(center);

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
		column.setLabel("Kode");
		column.setWidth("8%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kandungan");
		column.setWidth("8%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kelompok");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kelas");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Generik");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("10%");

		// onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		JenisItemMedis jenisItemMedis = (JenisItemMedis) (this.jenisItemMedis.getSelectedItem() == null ? null
				: this.jenisItemMedis.getSelectedItem().getValue());
		List<ItemMedis> item = ConstantValues
				.simpleList(session.createCriteria(ItemMedis.class).addOrder(Order.asc("nama"))
						.add(jenisItemMedis == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisItemMedis", jenisItemMedis))
						.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
						.add(Restrictions.ilike("kode", kodeItemMedisan.getValue().trim(), MatchMode.ANYWHERE))

						.setMaxResults(Common.MAX_RESULT), ItemMedis.class);

		System.out.println(item);
		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new ItemMedisRenderer());
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
