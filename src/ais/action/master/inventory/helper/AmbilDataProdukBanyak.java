package ais.action.master.inventory.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.JenisProduk;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataProdukBanyak extends MyWindow implements DataCriteria {

	/**
	 *  
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<Produk> produks;
	private List<Produk> produksHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();
	private Toko toko;

	public AmbilDataProdukBanyak(List<Produk> produks, Toko toko) {
		super();
		this.produks = produks;
		this.toko = toko;
		display();
		onSearchDefault(null);
	}

	public AmbilDataProdukBanyak(List<Produk> produks, JenisProduk jenisProduk, Toko toko) {
		super();
		this.produks = produks;
		this.toko = toko;
		display();
		onSearchDefault(null);
	}

	public AmbilDataProdukBanyak(List<Produk> produks, List<Produk> produksHanyaDitampilkan, Toko toko) {
		super();
		this.produks = produks;
		this.produksHanyaDitampilkan = produksHanyaDitampilkan;
		this.toko = toko;
		display();

		onSearchDefault(null);

	}

	private MyTextbox nama;

	class ProdukRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Produk produk = (Produk) arg1;
			arg0.setAttribute("produk", produk);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (Produk myProduk : produks) {
				if (myProduk.getId().equals(produk.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(produk.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(produk.getId());
					} else {
						ids.remove(produk.getId());
					}
				}
			});

			Component image = ProdukUtil.generateImage(produk);
			image.setParent(arg0);

			new Label(produk.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(Produk.class, produk, produk.getNama()).setParent(arg0);

			new Label(produk.getToko() == null ? "" : produk.getToko().getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(produk.getHargaJual())).setParent(arg0);

		}

	}

	public void display() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("130px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Columns columns = new Columns();
		columns.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		// Pager tunggal via AmbilDataPagingHelper; pager manual dihapus (double paging).
		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
		pagingHelper.pasangGridDanPaging(myCenter1, grid);

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Toko");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Harga");
		column.setWidth("10%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataProdukBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Produk> produks = new ArrayList<Produk>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								Produk myProduk = (Produk) row.getAttribute("produk");
								produks.add(myProduk);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/inventory/helper/AmbilDataProdukBanyak.java:295");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), produks);
					eventListener.onEvent(myEvent);
				}
				AmbilDataProdukBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (produksHanyaDitampilkan != null) {
			for (Produk produk : produksHanyaDitampilkan) {
				values.add(produk.getId());
			}
		}

		List<Long> valuesNot = new ArrayList<Long>();
		if (produks != null) {
			for (Produk produk : produks) {
				valuesNot.add(produk.getId());
			}
		}

		Criteria criteria = session.createCriteria(Produk.class).add(Restrictions.ne("nama", ""))
				.add(Restrictions.isNotNull("nama"))
				.add(Restrictions.or(Restrictions.eq("toko", toko), Restrictions.isNull("toko")))

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))

				.add(valuesNot.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", valuesNot)))

				.add(produksHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("kode", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE)));
		if (order) {
			criteria.addOrder(Order.asc("nama"));
		}

		return criteria;
	}

	public void onSearchDefault(Event event) {
		onSearchDefault(event, false);
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event, final Boolean dontLoop) {

		Session session = HibernateUtil.currentSession();

		List<Produk> produk = ConstantValues.simpleList(
				session.createCriteria(Produk.class).addOrder(Order.asc("nama"))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				Produk.class);

		List<Produk> myProduk = pagingHelper.cariDenganCriteria(initCriteria(true), Produk.class);

		produk.addAll(myProduk);

		ListModel strset = new SimpleListModel(produk);
		grid.setRowRenderer(new ProdukRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
