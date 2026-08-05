package ais.action.master.helper;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Decimalbox;
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
import ais.database.dao.QuotaWisudaUntukFakultasDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.QuotaWisudaUntukFakultas;
import ais.database.model.Wisuda;

public class QuotaWisudaUntukFakultasHelper {

	private MyGrid grid;

	// private Decimalbox quota;
	// private Textbox keterangan;
	private Textbox searchfakultas;
	private Wisuda wisuda;

	public void tampil(Wisuda wisuda, final MyWindow window) {
		this.wisuda = wisuda;
		Common.clear(window);
		window.setTitle("Data Quota Fakultas untuk Wisuda ke -- "
				+ wisuda.getWisudaKe());
		window.setWidth("750px");
		window.setHeight("540px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Quota " + "Fakultas");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(searchfakultas = new Textbox());
		searchfakultas.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// South south = new South();
		// ais.ui.util.ZkCompat.setFlex(south, true);
		// south.setParent(div);

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

		grid = new MyGrid();//grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Quota");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("40%");

		onSearchDefault(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	class QuotaWisudaUntukFakultasRenderer extends ais.ui.util.MyRowRenderer {
		// Session session = HibernateUtil.currentSession()
		// ;

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Fakultas fakultas = (Fakultas) arg1;

			QuotaWisudaUntukFakultas quotaWisudaUntukFakultas = getQuotaWisudaUntukFakultas(fakultas);

			new Label(fakultas.getNama()).setParent(arg0);

			final Decimalbox quota = new Decimalbox(
					quotaWisudaUntukFakultas.getQuota() == null ? new BigDecimal(
							0) : new BigDecimal(quotaWisudaUntukFakultas
							.getQuota()));
			quota.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onChangeQuota(fakultas, quota.getValue());
				}
			});
			quota.setParent(arg0);

			final Textbox keterangan = new Textbox(
					quotaWisudaUntukFakultas.getKeterangan() == null ? ""
							: quotaWisudaUntukFakultas.getKeterangan());
			keterangan.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onChangeKeterangan(fakultas, keterangan.getValue());
				}
			});
			keterangan.setParent(arg0);

		}

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Fakultas> fakultas = session
				.createCriteria(Fakultas.class)
				.addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("nama", searchfakultas.getValue(),
						MatchMode.ANYWHERE)).setMaxResults(Common.MAX_RESULT)
				.list();

		ListModel strset = new SimpleListModel(fakultas);
		grid.setRowRenderer(new QuotaWisudaUntukFakultasRenderer());
		grid.setModelCheckMobile(strset);

		

	}

	public void onChangeQuota(Fakultas fakultas, BigDecimal quota) {
		// TODO Auto-generated method stub
		QuotaWisudaUntukFakultas quotaWisudaUntukFakultas = getQuotaWisudaUntukFakultas(fakultas);
		quotaWisudaUntukFakultas.setQuota(quota.intValue());

		QuotaWisudaUntukFakultasDao quotaWisudaUntukFakultasDao = DaoFactory
				.getInstance().getQuotaWisudaUntukFakultasDao();
		// quotaWisudaUntukFakultasDao.beginTransaction();
		if (quotaWisudaUntukFakultas.getId() != null) {
			quotaWisudaUntukFakultasDao.update(quotaWisudaUntukFakultas);
		} else {
			quotaWisudaUntukFakultasDao.save(quotaWisudaUntukFakultas);
		}
		// quotaWisudaUntukFakultasDao.commitTransaction();

	}

	public void onChangeKeterangan(Fakultas fakultas, String keterangan) {
		// TODO Auto-generated method stub
		QuotaWisudaUntukFakultas quotaWisudaUntukFakultas = getQuotaWisudaUntukFakultas(fakultas);
		quotaWisudaUntukFakultas.setKeterangan(keterangan);

		QuotaWisudaUntukFakultasDao quotaWisudaUntukFakultasDao = DaoFactory
				.getInstance().getQuotaWisudaUntukFakultasDao();
		// quotaWisudaUntukFakultasDao.beginTransaction();
		if (quotaWisudaUntukFakultas.getId() != null) {
			quotaWisudaUntukFakultasDao.update(quotaWisudaUntukFakultas);
		} else {
			quotaWisudaUntukFakultasDao.save(quotaWisudaUntukFakultas);
		}
		// quotaWisudaUntukFakultasDao.commitTransaction();
	}

	public QuotaWisudaUntukFakultas getQuotaWisudaUntukFakultas(
			Fakultas fakultas) {
		// TODO Auto-generated method stub
		Session session = HibernateUtil.currentSession();

		QuotaWisudaUntukFakultas quotaWisudaUntukFakultas = (QuotaWisudaUntukFakultas) session
				.createCriteria(QuotaWisudaUntukFakultas.class)
				.add(Restrictions.eq("fakultas", fakultas))
				.add(Restrictions.eq("wisuda", wisuda)).uniqueResult();

		if (quotaWisudaUntukFakultas == null) {
			quotaWisudaUntukFakultas = new QuotaWisudaUntukFakultas();
			quotaWisudaUntukFakultas.setFakultas(fakultas);
			quotaWisudaUntukFakultas.setWisuda(wisuda);
		}

		return quotaWisudaUntukFakultas;
	}

}
