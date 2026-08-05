package ais.action.master.sirs.detail;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiMedisDetail;

public class TransaksiDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private TransaksiMedis transaksi;
	private Footer total;
	private Grid grid;

	public TransaksiDetailAction(TransaksiMedis transaksi) {
		super();
		this.transaksi = transaksi;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(TransaksiDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class TransaksiDetailRenderer extends ais.ui.util.MyRowRenderer {

		public TransaksiDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final TransaksiMedisDetail transaksiDetail = (TransaksiMedisDetail) data;

			if (transaksiDetail.getRacikan() == null) {
				new Label(transaksiDetail.getItem() == null ? "" : transaksiDetail.getItem().getKode()).setParent(row);
				RevisiHelper
						.createNewRevisi(TransaksiMedisDetail.class, transaksiDetail,
								transaksiDetail.getItem() == null ? "" : transaksiDetail.getItem().getNama())
						.setParent(row);
				new Label(transaksiDetail.getItem() == null || transaksiDetail.getItem().getSatuanItem() == null ? ""
						: transaksiDetail.getItem().getSatuanItem().getNama()).setParent(row);
			} else {
				new Label(transaksiDetail.getRacikan() == null ? "" : transaksiDetail.getRacikan().getKode())
						.setParent(row);
				RevisiHelper
						.createNewRevisi(TransaksiMedisDetail.class, transaksiDetail,
								transaksiDetail.getRacikan() == null ? "" : transaksiDetail.getRacikan().getNama())
						.setParent(row);

				new Label(ais.common.Common.getBahasaConfig("Racikan")).setParent(row);

			}

			new Label(transaksiDetail.getQty() == null ? "" : Common.numberFormat.get().format(transaksiDetail.getQty()))
					.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<TransaksiMedisDetail> transaksiDetails = session.createCriteria(TransaksiMedisDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("transaksi", transaksi)).list();

		ListModel strset = new SimpleListModel(transaksiDetails);
		grid.setRowRenderer(new TransaksiDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();

		Double mytotal = 0.0;
		for (TransaksiMedisDetail transaksiDetail : transaksiDetails) {
			mytotal += (transaksiDetail.getQty() == null ? 0.0 : transaksiDetail.getQty());
		}

		total.setLabel(Common.numberFormat.get().format(mytotal));
	}

	private void display() {

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(this);
		groupbox.appendChild(new Caption("Daftar Item Transaksi"));

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Kode Item");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Item");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setWidth("10%");

		Foot foot = new Foot();
		foot.setParent(grid);

		foot.appendChild(new Footer());
		foot.appendChild(new Footer("Total Qty"));
		foot.appendChild(new Footer());

		total = new Footer();
		total.setParent(foot);
		total.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		loadData(null);
	}

}
