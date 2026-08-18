package ais.action.master.payroll.helper;

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
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.payroll.Cabang;
import ais.database.model.payroll.Departemen;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.LevelJabatan;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataFormatItemGajiBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataFormatItemGajiBanbox() {
		super();
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				FormatItemGaji formatItemGaji = (FormatItemGaji) HibernateUtil.currentSession()
						.createCriteria(FormatItemGaji.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))) .add(Restrictions.ilike("nama",
								AmbilDataFormatItemGajiBanbox.this.getValue().trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (formatItemGaji == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Format Item Gaji dengan nama \"{V1}\" tidak ditemukan di dalam basis data. Langkah yang dapat dilakukan: (1) mohon periksa kembali penulisan nama yang Bapak/Ibu masukkan; (2) pastikan data Format Item Gaji telah tersedia; (3) kemudian coba lakukan pencarian kembali.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							AmbilDataFormatItemGajiBanbox.this.getValue().trim());
					return;
				}
				AmbilDataFormatItemGajiBanbox.this.setOpen(false);
				AmbilDataFormatItemGajiBanbox.this.setAttribute("formatItemGaji", formatItemGaji);
				AmbilDataFormatItemGajiBanbox.this.setValue(formatItemGaji.getNama());
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
	private Combobox searchcabang;
	private Combobox searchdepartemen;
	private Combobox searchlevelJabatan;

	public void setCabang(Cabang cabang) {

	}

	public void setDepertemen(Departemen departemen) {

	}

	public void setLevelJabatan(LevelJabatan levelJabatan) {

	}

	public class FormatItemGajiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final FormatItemGaji formatItemGaji = (FormatItemGaji) arg1;
			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataFormatItemGajiBanbox.this.setOpen(false);
					AmbilDataFormatItemGajiBanbox.this.setAttribute("formatItemGaji", formatItemGaji);
					AmbilDataFormatItemGajiBanbox.this.setValue(formatItemGaji.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(formatItemGaji.getNama()).setParent(arg0);
			new Label(formatItemGaji.getCabang() == null ? "" : formatItemGaji.getCabang().getNama()).setParent(arg0);
			new Label(formatItemGaji.getDepartemen() == null ? "" : formatItemGaji.getDepartemen().getNama())
					.setParent(arg0);
			new Label(formatItemGaji.getLevelJabatan() == null ? "" : formatItemGaji.getLevelJabatan().getNama())
					.setParent(arg0);
			new Label(formatItemGaji.getKeterangan()).setParent(arg0);

		}

	}

	public void display() {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("90%");
		bandpopup.setHeight("600px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Format Item Gaji");
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
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Cabang")));
		row.appendChild(searchcabang = new Combobox());
		searchcabang.setWidth("90%");
		searchcabang.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Departemen")));
		row.appendChild(searchdepartemen = new Combobox());
		searchdepartemen.setWidth("90%");
		searchdepartemen.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jabatan")));
		row.appendChild(searchlevelJabatan = new Combobox());
		searchlevelJabatan.setWidth("90%");
		searchlevelJabatan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Common.insertCombo(searchcabang, "nama", "keterangan", Cabang.class);
		Common.insertCombo(searchdepartemen, "nama", "keterangan", Departemen.class);
		Common.insertCombo(searchlevelJabatan, "nama", "keterangan", LevelJabatan.class);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		Toolbarbutton button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
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
						Common.tampilErrorJikaAdmin(e);
					}
				}
				onSearchDefault(event);
			}
		}));

		// final Radiogroup radiogroup = new Radiogroup();

		grid = new MyGrid();
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

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Format");
		column.setWidth("35%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Cabang");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Departemen");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jabatan");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		// onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<FormatItemGaji> formatItemGaji = session.createCriteria(FormatItemGaji.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))) .addOrder(Order.desc("id"))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchcabang.getSelectedItem() == null || searchcabang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("cabang", searchcabang.getSelectedItem().getValue()))
				.add(searchdepartemen.getSelectedItem() == null || searchdepartemen.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("departemen", searchdepartemen.getSelectedItem().getValue()))
				.add(searchlevelJabatan.getSelectedItem() == null
						|| searchlevelJabatan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("levelJabatan", searchlevelJabatan.getSelectedItem().getValue()))
				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(formatItemGaji);
		ListModel strset = new SimpleListModel(formatItemGaji);
		grid.setRowRenderer(new FormatItemGajiRenderer());
		grid.setModelCheckMobile(strset);
		grid.renderAll();

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
