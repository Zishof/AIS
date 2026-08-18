package ais.action.master.akunting.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.database.dao.DaoFactory;
import ais.database.dao.akunting.KelompokLaporanPunyaAkunDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.KelompokLaporan;
import ais.database.model.akunting.KelompokLaporanPunyaAkun;

public class AmbilDataBanyakListAkun extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private KelompokLaporan kelompokLaporan;

	private EventListener eventListener;

	public AmbilDataBanyakListAkun(KelompokLaporan kelompokLaporan) {
		super();
		this.kelompokLaporan = kelompokLaporan;
		display();
	}

	private Textbox kodeAkunan;
	private Textbox nama;

	class AkunRenderer extends ais.ui.util.MyRowRenderer {

		private Session session = HibernateUtil.currentSession();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Akun akun = (Akun) arg1;
			arg0.setAttribute("akun", akun);

			Integer count = ((Number) session
					.createCriteria(KelompokLaporanPunyaAkun.class)
					.add(Restrictions.eq("akun", akun))
					.setProjection(Projections.rowCount()).uniqueResult())
					.intValue();

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setDisabled(!count.equals(0));
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (checkbox.isChecked()) {
						KelompokLaporanPunyaAkunDao kelompokLaporanPunyaAkunDao = DaoFactory
								.getInstance().getKelompokLaporanPunyaAkunDao();
						KelompokLaporanPunyaAkun kelompokLaporanPunyaAkun = new KelompokLaporanPunyaAkun();

						kelompokLaporanPunyaAkun
								.setKelompokLaporan(kelompokLaporan);
						kelompokLaporanPunyaAkun.setAkun(akun);

						kelompokLaporanPunyaAkunDao
								.save(kelompokLaporanPunyaAkun);
					} else {
						KelompokLaporanPunyaAkunDao kelompokLaporanPunyaAkunDao = DaoFactory
								.getInstance().getKelompokLaporanPunyaAkunDao();
						KelompokLaporanPunyaAkun kelompokLaporanPunyaAkun = (KelompokLaporanPunyaAkun) kelompokLaporanPunyaAkunDao
								.getCurrentSession()
								.createCriteria(KelompokLaporanPunyaAkun.class)
								.add(Restrictions.eq("akun", akun))
								.setMaxResults(1).uniqueResult();
						

						Common.refreshDelete(kelompokLaporanPunyaAkun);
					}

					if (eventListener != null) {
						eventListener.onEvent(arg0);
					}
				}
			});

			new Label(akun.getKode()).setParent(arg0);
			new Label(akun.getNama()).setParent(arg0);
			new Label(akun.getDebetCredit() == null ? "" : akun
					.getDebetCredit().equals(Akun.DEBET) ? "Debet" : "Credit")
					.setParent(arg0);

		}

	}

	public void display() {

		setWidth("700px");
		setHeight("90%");
		setClosable(true);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Akun");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Akun"));
		row.appendChild(kodeAkunan = new Textbox());
		kodeAkunan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Akun"));
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
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode Akun");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Akun");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Debet/Credit");
		column.setWidth("25%");

		onSearchDefault(null);

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Akun> akun = session
				.createCriteria(Akun.class)
				.addOrder(Order.desc("id"))
				.add(Restrictions.ilike("nama", nama.getText().trim(),
						MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodeAkunan.getText().trim(),
						MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(akun);
		ListModel strset = new SimpleListModel(akun);
		grid.setRowRenderer(new AkunRenderer());
		grid.setModelCheckMobile(strset);
		

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
