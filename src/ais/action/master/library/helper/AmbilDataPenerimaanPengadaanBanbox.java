package ais.action.master.library.helper;

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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.PenerimaanPengadaanItem;
import ais.ui.util.MyTextbox;

public class AmbilDataPenerimaanPengadaanBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataPenerimaanPengadaanBanbox() throws Exception {
		super();

		display();

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (grid == null || grid.getRows() == null
						|| grid.getRows().getChildren() == null
						|| grid.getRows().getChildren().size() == 0) {
					onSearchDefault(null);
				}
			}
		});
	}

	private MyTextbox kodePenerimaanPengadaanIteman;
	private MyTextbox nama;
	private AmbilDataPerpustakaanBanbox searchperpustakaan;

	class PenerimaanPengadaanItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenerimaanPengadaanItem penerimaanPengadaanItem = (PenerimaanPengadaanItem) arg1;

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (penerimaanPengadaanItem.getDisetujuiOleh() == null) {
						MyMessageboxConfig.show(
								"Penerimaan yang anda pilih belum disetujui",
								"Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						return;
					}

					AmbilDataPenerimaanPengadaanBanbox.this.setOpen(false);
					AmbilDataPenerimaanPengadaanBanbox.this.setAttribute(
							"penerimaanPengadaanItem", penerimaanPengadaanItem);
					AmbilDataPenerimaanPengadaanBanbox.this
							.setValue(penerimaanPengadaanItem.toString());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(penerimaanPengadaanItem.getKode()).setParent(arg0);
			new Label(penerimaanPengadaanItem.getKeterangan()).setParent(arg0);
			new Label(penerimaanPengadaanItem.getPenyedia() == null ? ""
					: penerimaanPengadaanItem.getPenyedia().getNama())
					.setParent(arg0);
			new Label(
					penerimaanPengadaanItem.getTanggalPersetujuan() == null ? ""
							: Common.dateFormat.get().format(penerimaanPengadaanItem
									.getTanggalPersetujuan())).setParent(arg0);
			new Label(penerimaanPengadaanItem.getDisetujuiOleh() == null ? ""
					: penerimaanPengadaanItem.getDisetujuiOleh().getUserId())
					.setParent(arg0);

		}

	}

	public void display() throws Exception {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Penerimaan Pengadaan Item");
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

		MyGrid searchgrid = new MyGrid();searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kodePenerimaanPengadaanIteman = new MyTextbox());
		kodePenerimaanPengadaanIteman.setWidth("90%");
		kodePenerimaanPengadaanIteman.addEventListener(Events.ON_OK,
				new EventListener() {
					public void onEvent(Event event) throws Exception {
						onSearchDefault(event);
					}
				});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(searchperpustakaan = new AmbilDataPerpustakaanBanbox());
		searchperpustakaan.setWidth("90%");
		searchperpustakaan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

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

		grid = new MyGrid();//grid.setOddRowSclass("non-odd");grid.setWidth("100%");
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
		column.setLabel("Kode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterengan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penyedia");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Disetujui");
		column.setWidth("10%");

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<PenerimaanPengadaanItem> penerimaanPengadaanItem = session
				.createCriteria(PenerimaanPengadaanItem.class)
				.add(searchperpustakaan.getAttribute("perpustakaan") == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq(
						"perpustakaan",
						searchperpustakaan.getAttribute("perpustakaan")))
				.addOrder(Order.desc("id"))
				.add(Restrictions.ilike("keterangan", nama.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(kodePenerimaanPengadaanIteman.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ilike("kode",
						kodePenerimaanPengadaanIteman.getValue().trim(),
						MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(penerimaanPengadaanItem);
		ListModel strset = new SimpleListModel(penerimaanPengadaanItem);
		grid.setRowRenderer(new PenerimaanPengadaanItemRenderer());
		grid.setModelCheckMobile(strset);

		
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}

