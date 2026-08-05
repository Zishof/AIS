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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.action.master.library.PenerbitAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Penerbit;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataPenerbitBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataPenerbitBanbox() throws Exception {
		super();
		// this.addEventListener(Events.ON_OK, new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		//
		// if (getValue().trim().equals("")) {
		// setAttribute("penerbit", null);
		// setValue("");
		// return;
		// }
		//
		// Penerbit penerbit = (Penerbit) HibernateUtil
		// .currentSession()
		// .createCriteria(Penerbit.class)
		// .add(Restrictions.ilike("isbn",
		// AmbilDataPenerbitBanbox.this.getValue().trim(),
		// MatchMode.EXACT)).setMaxResults(1)
		// .uniqueResult();
		// if (penerbit == null) {
		// MyMessageboxConfig.show("Penerbit dengan kode = "
		// + AmbilDataPenerbitBanbox.this.getValue().trim()
		// + " tidak dpenerbitukan", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		// return;
		// }
		// AmbilDataPenerbitBanbox.this.setOpen(false);
		// AmbilDataPenerbitBanbox.this.setAttribute("penerbit", penerbit);
		// AmbilDataPenerbitBanbox.this.setValue(penerbit.getNama());
		// if (eventListener != null) {
		// eventListener.onEvent(arg0);
		// }
		// }
		// });

		final Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("700px");
		bandpopup.setHeight("600px");

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (hasDisplayed) {
					return;
				}

				display(bandpopup);
				onSearchDefault(arg0);
			}
		});
	}

	private MyTextbox kodePenerbitan;
	private MyTextbox nama;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private boolean hasDisplayed = false;

	class PenerbitRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Penerbit penerbit = (Penerbit) arg1;

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPenerbitBanbox.this.setOpen(false);
					AmbilDataPenerbitBanbox.this.setAttribute("penerbit", penerbit);
					AmbilDataPenerbitBanbox.this.setValue(penerbit.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			// new Label(penerbit.getKode()).setParent(arg0);
			new Label(penerbit.getNama()).setParent(arg0);
			new Label(penerbit.getAlamat()).setParent(arg0);
			// new Label(penerbit.getTelp()).setParent(arg0);
			// new Label(penerbit.getFax()).setParent(arg0);
			new Label(penerbit.getSatuanKerja() == null ? "" : penerbit.getSatuanKerja().toString()).setParent(arg0);
			// new Label(penerbit.getEmail()).setParent(arg0);

		}

	}

	public void display(Bandpopup bandpopup) throws Exception {

		if (hasDisplayed) {
			return;
		}
		hasDisplayed = true;
		Common.clear(bandpopup);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(bandpopup);
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kodePenerbitan = new MyTextbox());
		kodePenerbitan.setWidth("90%");
		kodePenerbitan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setWidth("90%");
		satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});
		satuanKerja.setValue("");
		satuanKerja.setAttribute("satuanKerja", null);
		satuanKerja.setAttribute("myValue", null);
		satuanKerja.setDisabled(false);

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

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Penerbit Baru", "/img/new.gif");
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				PenerbitAction.onAddExternal(event, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Penerbit penerbit = (Penerbit) arg0.getData();
						AmbilDataPenerbitBanbox.this.setOpen(false);
						AmbilDataPenerbitBanbox.this.setAttribute("penerbit", penerbit);
						AmbilDataPenerbitBanbox.this.setValue(penerbit.getNama());
						if (eventListener != null) {
							eventListener.onEvent(arg0);
						}
					}
				}, new Penerbit());

			}
		});

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
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

		// MyColumnConfig column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setLabel("Kode");
		// column.setWidth("10%");

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Alamat");
		column.setWidth("20%");

		// column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setLabel("Telp.");
		// column.setWidth("10%");
		//
		// column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setLabel("Fax.");
		// column.setWidth("10%");
		//
		// column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setLabel("Kontak");
		// column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Satuan Kerja");
		// column.setWidth("10%");

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Penerbit> penerbit = session.createCriteria(Penerbit.class).addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				// Entitas Penerbit TIDAK memiliki properti "kode" (hanya nama/alamat/kodePos/telp/dll),
				// sehingga ilike("kode", ...) memicu QueryException "could not resolve property: kode".
				// Kotak pencarian ini dialihkan ke properti nyata "nama" agar tetap berfungsi menyaring
				// penerbit tanpa menimbulkan error.
				.add(kodePenerbitan.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", kodePenerbitan.getValue().trim(), MatchMode.ANYWHERE))

				.add(satuanKerja.getAttribute("satuanKerja") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("satuanKerja", satuanKerja.getAttribute("satuanKerja")))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(penerbit);
		ListModel strset = new SimpleListModel(penerbit);
		grid.setRowRenderer(new PenerbitRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setSatuanKerja(SatuanKerja satuanKerja) {
		if (satuanKerja != null) {
			try {
				setValue("");
				setAttribute("penerbit", null);
				this.satuanKerja.setAttribute("satuanKerja", satuanKerja);
				this.satuanKerja.setValue(satuanKerja == null ? "" : satuanKerja.toString());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
			// onSearchDefault(null);
		}
		try {
			this.satuanKerja.setDisabled(satuanKerja != null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

}

