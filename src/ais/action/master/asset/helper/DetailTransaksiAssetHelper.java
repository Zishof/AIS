package ais.action.master.asset.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.DetailTransaksiAsset;
import ais.database.model.asset.MasterAsset;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;

public class DetailTransaksiAssetHelper extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	public DetailTransaksiAssetHelper(final MasterAsset masterAsset, final SatuanKerja satuanKerja) {
		super();

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(DetailTransaksiAssetHelper.this);
				if (isOpen()) {
					display(masterAsset, satuanKerja);
				}
			}
		});
	}

	public static String dapatkanInfo(DetailTransaksiAsset detailTransaksiAsset) {
		String info = "";
		if (detailTransaksiAsset.getSaldoAwalMasterAssetDetail() != null) {
			info = detailTransaksiAsset.getSaldoAwalMasterAssetDetail().getSaldoAwal().getKode() + " "
					+ detailTransaksiAsset.getSaldoAwalMasterAssetDetail().getKeterangan() + " "
					+ detailTransaksiAsset.getSaldoAwalMasterAssetDetail().getSaldoAwal().getKeterangan();
		} else if (detailTransaksiAsset.getPenerimaanPengadaanMasterAssetDetail() != null) {
			info = detailTransaksiAsset.getPenerimaanPengadaanMasterAssetDetail().getPenerimaanPengadaanMasterAsset()
					.getKode() + " " + detailTransaksiAsset.getPenerimaanPengadaanMasterAssetDetail().getKeterangan()
					+ " " + detailTransaksiAsset.getPenerimaanPengadaanMasterAssetDetail()
							.getPenerimaanPengadaanMasterAsset().getKeterangan();
		} else if (detailTransaksiAsset.getPemakaianMasterAssetDetail() != null) {
			info = detailTransaksiAsset.getPemakaianMasterAssetDetail().getPemakaianMasterAsset().getKode() + " "
					+ detailTransaksiAsset.getPemakaianMasterAssetDetail().getKeterangan() + " "
					+ detailTransaksiAsset.getPemakaianMasterAssetDetail().getPemakaianMasterAsset().getKeterangan();
		}
		return info;
	}

	public void display(MasterAsset masterAsset, SatuanKerja satuanKerja) {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.appendChild(new MyCaptionStyled("Sejarah Transaksi "
				+ (masterAsset == null ? "" : masterAsset.getKode()) + " " + masterAsset.getNama()));
		groupbox.setParent(this);

		Session session = HibernateUtil.currentSession();
		@SuppressWarnings("unchecked")
		List<DetailTransaksiAsset> detailTransaksiAssets = session.createCriteria(DetailTransaksiAsset.class)
				.add(Restrictions.eq("masterAsset", masterAsset))
				.add(satuanKerja == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("satuanKerja", satuanKerja))
				.addOrder(Order.desc("tanggal")).list();

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(groupbox);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Transaksi");
		column.setParent(columns);

		column = new MyColumnConfig("Tanggal/Waktu");
		column.setParent(columns);

		column = new MyColumnConfig("Masuk");
		column.setParent(columns);
		column.setWidth("5%");
		column.setAlign("right");

		column = new MyColumnConfig("Keluar");
		column.setParent(columns);
		column.setWidth("5%");
		column.setAlign("right");

		column = new MyColumnConfig("Saldo");
		column.setParent(columns);
		column.setWidth("5%");
		column.setAlign("right");

		column = new MyColumnConfig("Informasi");
		column.setParent(columns);
		column.setWidth("60%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Double jml = 0.0;
		for (DetailTransaksiAsset detailTransaksiAsset : detailTransaksiAssets) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(detailTransaksiAsset.getKodeTransaksi() == null ? ""
					: (detailTransaksiAsset.getKodeTransaksi().getKode() + " - "
							+ detailTransaksiAsset.getKodeTransaksi().getNama())));
			row.appendChild(new ais.ui.util.MyLabelConfig(
					Common.dateFormat3.get().format(detailTransaksiAsset.getTanggalDanWaktu())));

			if (detailTransaksiAsset.getKodeTransaksi().getJenis().equals(1)) {
				new MyLabelKecil(Common.numberFormat.get()
						.format((detailTransaksiAsset.getQty() + detailTransaksiAsset.getQtyBonus()))).setParent(row);
				new MyLabelKecil(Common.numberFormat.get().format(0.0)).setParent(row);
			} else {
				new MyLabelKecil(Common.numberFormat.get().format(0.0)).setParent(row);
				new MyLabelKecil(Common.numberFormat.get()
						.format((detailTransaksiAsset.getQty() + detailTransaksiAsset.getQtyBonus()))).setParent(row);
			}

			Double j = (detailTransaksiAsset.getKodeTransaksi().getJenis()
					* (detailTransaksiAsset.getQty() + detailTransaksiAsset.getQtyBonus()));

			jml += j;

			new MyLabelKecil(Common.numberFormat.get().format(jml)).setParent(row);

			new Label(dapatkanInfo(detailTransaksiAsset)).setParent(row);
		}
	}

}
