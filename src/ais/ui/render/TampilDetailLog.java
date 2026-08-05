package ais.ui.render;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.LogLogin;
import ais.database.model.LogUserActifity;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

public class TampilDetailLog implements EventListener {

	private class TampilDetailUserAccessLog implements EventListener {

		private DetailLogLogin detailLogLogin;
		private MyDetail detail;
		private MyGrid logUserActifityGrid;

		class DetailLogLoginRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(final Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				// TODO Auto-generated method stub
				final LogUserActifity logUserActifity = (LogUserActifity) arg1;

				new Label(logUserActifity.getKeterangan()).setParent(arg0);
				new Label(logUserActifity.getKeterangan12() == null ? "" : logUserActifity.getKeterangan12())
						.setParent(arg0);
				new Label(logUserActifity.getKeterangan1()).setParent(arg0);

				new Label(logUserActifity.getImg()).setParent(arg0);
				new Label(logUserActifity.getEvent()).setParent(arg0);

				new Label(
						logUserActifity.getWaktu() == null ? "" : Common.dateFormat3.get().format(logUserActifity.getWaktu()))
						.setParent(arg0);

			}
		}

		private void createList() {
			Common.clear(detail);
			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			groupbox.setParent(detail);
			groupbox.appendChild(new MyCaptionStyled("Daftar Rincian Akses"));

			logUserActifityGrid = new MyGrid();
			logUserActifityGrid.setMold("paging");
			logUserActifityGrid.setPageSize(15);
			logUserActifityGrid.setParent(groupbox);
			logUserActifityGrid.setWidth("100%");
			logUserActifityGrid.setHeight("100%");

			Columns columns = new Columns();

			columns.setParent(logUserActifityGrid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Keterangan 1");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Keterangan 2");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Keterangan 3");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Image");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Event");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Waktu");
			column.setAlign("center");

			loadDetailLogLogin();
		}

		@SuppressWarnings("unchecked")
		private void loadDetailLogLogin() {
			Session session = HibernateUtil.currentSession();
			List<LogUserActifity> logUserActifity = session.createCriteria(LogUserActifity.class)
					.add(Restrictions.eq("detailLogLogin", detailLogLogin)).addOrder(Order.desc("id")).list();

			ListModel strset = new SimpleListModel(logUserActifity);
			logUserActifityGrid.setRowRenderer(new DetailLogLoginRenderer());
			logUserActifityGrid.setModelCheckMobile(strset);
			logUserActifityGrid.renderAll();
			logUserActifityGrid.setOddRowSclass("non-odd");

		}

		public TampilDetailUserAccessLog(MyDetail detail, DetailLogLogin logLogin) {
			this.detailLogLogin = logLogin;
			this.detail = detail;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {
			Common.clear(detail);
			if (detail.isOpen()) {
				createList();
			}
		}

	}

	private LogLogin logLogin;
	private MyDetail detail;
	private MyGrid detailLogGrid;

	class DetailLogLoginRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DetailLogLogin logLogin = (DetailLogLogin) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new TampilDetailUserAccessLog(detail, logLogin));

			new Label(logLogin.getKeterangan()).setParent(arg0);

			new Label(logLogin.getWaktu() == null ? "" : Common.dateFormat3.get().format(logLogin.getWaktu()))
					.setParent(arg0);

		}
	}

	private void createList() {
		Common.clear(detail);
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(detail);
		groupbox.appendChild(new MyCaptionStyled("Daftar Rincian Kunjungan"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();
				Criteria criteria = session.createCriteria(DetailLogLogin.class)
						.add(Restrictions.eq("logLogin", logLogin)).addOrder(Order.desc("id"));
				return criteria;
			}
		}, "logLogin", "waktu", "halaman", "keterangan");
		toolbar.appendChild(cetakToolbarbutton);

		detailLogGrid = new MyGrid();
		detailLogGrid.setMold("paging");
		detailLogGrid.setPageSize(15);
		detailLogGrid.setParent(groupbox);
		detailLogGrid.setWidth("100%");
		detailLogGrid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(detailLogGrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("35px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Menu yang diakses");
		column.setWidth("75%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");
		column.setAlign("center");
		column.setWidth("25%");

		loadDetailLogLogin();
	}

	@SuppressWarnings("unchecked")
	private void loadDetailLogLogin() {
		Session session = HibernateUtil.currentSession();
		List<DetailLogLogin> logLogins = session.createCriteria(DetailLogLogin.class)
				.add(Restrictions.eq("logLogin", logLogin)).addOrder(Order.desc("id")).list();

		ListModel strset = new SimpleListModel(logLogins);
		detailLogGrid.setRowRenderer(new DetailLogLoginRenderer());
		detailLogGrid.setModelCheckMobile(strset);
		detailLogGrid.renderAll();
		detailLogGrid.setOddRowSclass("non-odd");

	}

	public TampilDetailLog(MyDetail detail, LogLogin logLogin) {
		this.logLogin = logLogin;
		this.detail = detail;
	}

	@Override
	public void onEvent(Event arg0) throws Exception {
		Common.clear(detail);
		if (detail.isOpen()) {
			createList();
		}
	}

}
