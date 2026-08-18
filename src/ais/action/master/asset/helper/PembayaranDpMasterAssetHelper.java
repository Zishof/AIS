package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.PembayaranDpMasterAsset;
import ais.database.model.asset.PembayaranDpMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenyediaAsset;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class PembayaranDpMasterAssetHelper {

	private MyGrid gridPenerimaanDpMasterAsset;
	private boolean edit = false;
	private PenyediaAsset penyediaAsset = null;
	private double totalDibayar = 0.0;
	private Footer footerTotalDibayar;
	private PembayaranDpMasterAsset pembayaranDpMasterAsset;

	public PembayaranDpMasterAssetHelper(MyGrid gridPenerimaanDpMasterAsset) {
		this.gridPenerimaanDpMasterAsset = gridPenerimaanDpMasterAsset;

	}

	public Groupbox initDetail(final PembayaranDpMasterAsset pembayaranDpMasterAsset) throws Exception {
		this.pembayaranDpMasterAsset = pembayaranDpMasterAsset;
		MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
		myGroupboxStyled.appendChild(new MyCaptionStyled("Daftar Pembayaran DP Barang/Jasa"));

		edit = pembayaranDpMasterAsset.getDisetujuiOleh() == null;

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(myGroupboxStyled);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.setDisabled(pembayaranDpMasterAsset.getId() != null && pembayaranDpMasterAsset.getDisetujuiOleh() != null);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(pembayaranDpMasterAsset);
			}
		});

		Columns columns = new Columns();
		columns.setParent(gridPenerimaanDpMasterAsset);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/Keterangan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tagihan DP");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("DP Tertagih");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sisa DP");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dibayar");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		gridPenerimaanDpMasterAsset.setParent(myGroupboxStyled);
		gridPenerimaanDpMasterAsset.setWidth("100%");
		gridPenerimaanDpMasterAsset.setHeight("100%");
		gridPenerimaanDpMasterAsset.setStyle("min-height:350px");
		gridPenerimaanDpMasterAsset.setMold("paging");
		gridPenerimaanDpMasterAsset.setPageSize(10);
		gridPenerimaanDpMasterAsset.getPagingChild().setMold("os");

		loadDataDetail(pembayaranDpMasterAsset);

		Foot foot = new Foot();
		foot.setParent(gridPenerimaanDpMasterAsset);

		Footer footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footerTotalDibayar = new Footer(Common.numberFormat.get().format(totalDibayar));
		foot.appendChild(footerTotalDibayar);

		footer = new Footer();
		foot.appendChild(footer);

		return myGroupboxStyled;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final PembayaranDpMasterAsset pembayaranDpMasterAsset) throws Exception {

		if (pembayaranDpMasterAsset.getPenyedia() != null) {
			penyediaAsset = pembayaranDpMasterAsset.getPenyedia();
		}

		Rows rows = gridPenerimaanDpMasterAsset.getRows() == null ? new Rows() : gridPenerimaanDpMasterAsset.getRows();
		rows.setParent(gridPenerimaanDpMasterAsset);
		Common.clear(rows);
		if (penyediaAsset == null) {
			return;
		}
		List<PembayaranDpMasterAssetDetail> pembayaranDpMasterAssetDetails;
		if (pembayaranDpMasterAsset.getId() != null) {
			Session session = HibernateUtil.currentNativeSession();
			pembayaranDpMasterAssetDetails = session.createCriteria(PembayaranDpMasterAssetDetail.class)
					.add(Restrictions.eq("pembayaranDpMasterAsset", pembayaranDpMasterAsset)).list();
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		} else {

			pembayaranDpMasterAssetDetails = new ArrayList<PembayaranDpMasterAssetDetail>();

			Session session = HibernateUtil.currentNativeSession();
			List<PemesananPengadaanMasterAsset> pemesananPengadaanMasterAssets = session
					.createCriteria(PemesananPengadaanMasterAsset.class).add(Restrictions.gt("dp", 0.1))
//					.add(Restrictions.or(Restrictions.isNull("lunas"), Restrictions.eq("lunas", false)))
					.add(Restrictions.eq("penyedia", penyediaAsset)).list();

			totalDibayar = 0.0;
			for (PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset : pemesananPengadaanMasterAssets) {

				PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail = new PembayaranDpMasterAssetDetail();
				pembayaranDpMasterAssetDetail.setDibayar(0.0);
				pembayaranDpMasterAssetDetail.setPembayaranDpMasterAsset(pembayaranDpMasterAsset);
				pembayaranDpMasterAssetDetail.setPemesananPengadaanMasterAsset(pemesananPengadaanMasterAsset);

				Number nilaiTagihan = (Number) session.createCriteria(PembayaranDpMasterAssetDetail.class)
						.add(Restrictions.eq("pemesananPengadaanMasterAsset", pemesananPengadaanMasterAsset))
						.setProjection(Projections.sum("dibayar")).uniqueResult();
				Double d = nilaiTagihan == null ? 0.0 : nilaiTagihan.doubleValue();
				pembayaranDpMasterAssetDetail.setTotalTelahDibayar(d);
				Double dibayar = pemesananPengadaanMasterAsset.hitungDibayar(session);

				if (dibayar.intValue() != pemesananPengadaanMasterAsset.getDibayar().intValue()) {
					pemesananPengadaanMasterAsset.setDibayar(dibayar);

					session.getTransaction().begin();
					Common.refreshUpdate(session, pemesananPengadaanMasterAsset);
					session.getTransaction().commit();
				}

				if (pembayaranDpMasterAsset != null && pembayaranDpMasterAsset.getId() != null) {
					session.getTransaction().begin();
					session.save(pembayaranDpMasterAssetDetail);
					session.getTransaction().commit();
				}

				totalDibayar += pembayaranDpMasterAssetDetail.getDibayar();

				pembayaranDpMasterAssetDetails.add(pembayaranDpMasterAssetDetail);
			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		}

		for (PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail : pembayaranDpMasterAssetDetails) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, pembayaranDpMasterAssetDetail);
		}

		Common.createDefaultTimer(eventListenerHitungUlang);
	}

	private EventListener eventListenerHitungUlang = new EventListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(Event arg0) throws Exception {
			List<Row> rows = gridPenerimaanDpMasterAsset.getRows().getChildren();

			totalDibayar = 0.0;
			for (Row row : rows) {
				if (row.isVisible()) {
					PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail = (PembayaranDpMasterAssetDetail) row
							.getAttribute("pembayaranDpMasterAssetDetail");

					totalDibayar += pembayaranDpMasterAssetDetail.getDibayar();
				}
			}

			footerTotalDibayar.setLabel(Common.numberFormat.get().format(totalDibayar));
		}
	};

	public void initRow(final Row row, final PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail)
			throws Exception {

		boolean persetujuan = pembayaranDpMasterAssetDetail.getPembayaranDpMasterAsset().getDisetujuiOleh() != null
				|| !edit;

		row.setValign("top");
		row.setAttribute("pembayaranDpMasterAssetDetail", pembayaranDpMasterAssetDetail);
		PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = pembayaranDpMasterAssetDetail
				.getPemesananPengadaanMasterAsset();
		PembayaranDpMasterAsset pembayaranDpMasterAsset = pembayaranDpMasterAssetDetail.getPembayaranDpMasterAsset();

		Session session = HibernateUtil.currentSession();
		PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAssetData = (PenerimaanPengadaanMasterAsset) session
				.createCriteria(PenerimaanPengadaanMasterAsset.class)
				.add(Restrictions.eq("pemesananPengadaanMasterAsset", pemesananPengadaanMasterAsset)).setMaxResults(1)
				.uniqueResult();

		if (penerimaanPengadaanMasterAssetData != null) {
			new PenerimaanPengadaanMasterAssetDetailAction(penerimaanPengadaanMasterAssetData, false).setParent(row);
		} else {
			new Label().setParent(row);
		}

		Vbox myvbox = new Vbox();
		myvbox.setParent(row);

		Double dp = pemesananPengadaanMasterAsset == null ? 0.0 : pemesananPengadaanMasterAsset.getDptotal();
		Double telahDibayar = pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset().getDibayar();
		Double sisa = dp - telahDibayar;

		final MyCheckboxConfig pilih;
		pilih = new MyCheckboxConfig(pemesananPengadaanMasterAsset.getKode());
		pilih.setChecked(pembayaranDpMasterAssetDetail.getPilih());
		row.setValign("top");
		row.setAttribute("pilih", pilih);

		if (pembayaranDpMasterAssetDetail.getId() != null || sisa.intValue() == 0) {
			myvbox.appendChild(new Label(pemesananPengadaanMasterAsset.getKode()));
		} else {
			myvbox.appendChild(pilih);
		}

		myvbox.appendChild(new Label(pemesananPengadaanMasterAsset.getKeterangan()));

		Label jumlah = new MyLabelKecil(Common.numberFormat.get().format(dp));

		(jumlah).setParent(row);

		jumlah = new MyLabelKecil(Common.numberFormat.get().format(telahDibayar));

		(jumlah).setParent(row);

		jumlah = new MyLabelKecil(Common.numberFormat.get().format(sisa));

		(jumlah).setParent(row);

		final MyDoublebox dibayar = new MyDoublebox(pembayaranDpMasterAssetDetail.getDibayar());

		if (pembayaranDpMasterAssetDetail.getPembayaranDpMasterAsset().getDisetujuiOleh() != null) {
			jumlah = new MyLabelKecil(Common.numberFormat.get().format(pembayaranDpMasterAssetDetail.getDibayar()));
			(jumlah).setParent(row);
		} else {
			(dibayar).setParent(row);
		}
		dibayar.setDisabled(pembayaranDpMasterAsset.getDisetujuiOleh() != null || !edit);
		dibayar.setStyle("text-align:right");
		dibayar.setWidth("90%");
		dibayar.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Double saldo = Math.abs(dibayar.getValue() == null ? 0.0 : dibayar.getValue());
				dibayar.setValue(saldo);
				pembayaranDpMasterAssetDetail.setPilih(pilih.isChecked());
				pembayaranDpMasterAssetDetail.setDibayar(saldo);
				if (pembayaranDpMasterAssetDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pembayaranDpMasterAssetDetail));
				}
				row.setValign("top");
				row.setAttribute("pembayaranDpMasterAssetDetail", pembayaranDpMasterAssetDetail);

				eventListenerHitungUlang.onEvent(arg0);
			}
		});

		pilih.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				dibayar.setDisabled(!pilih.isChecked());

				Double saldo = Math.abs(dibayar.getValue() == null ? 0.0 : dibayar.getValue());
				dibayar.setValue(saldo);
				pembayaranDpMasterAssetDetail.setPilih(pilih.isChecked());
				pembayaranDpMasterAssetDetail.setDibayar(saldo);
				if (pembayaranDpMasterAssetDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pembayaranDpMasterAssetDetail));
				}
				row.setValign("top");
				row.setAttribute("pembayaranDpMasterAssetDetail", pembayaranDpMasterAssetDetail);

				eventListenerHitungUlang.onEvent(arg0);
			}
		});

		dibayar.setDisabled(!pilih.isChecked());

		final MyTextbox keterangan = new MyTextbox(pembayaranDpMasterAssetDetail.getKeterangan() == null ? ""
				: pembayaranDpMasterAssetDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");
		if (persetujuan) {
			new MyLabelKecil(pembayaranDpMasterAssetDetail.getKeterangan()).setParent(row);
		} else {
			keterangan.setParent(row);
		}
		keterangan.setDisabled(
				pembayaranDpMasterAssetDetail.getPembayaranDpMasterAsset().getDisetujuiOleh() != null || !edit);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				pembayaranDpMasterAssetDetail.setKeterangan(keterangan.getValue());

				row.setValign("top");
				row.setAttribute("pembayaranDpMasterAssetDetail", pembayaranDpMasterAssetDetail);
				if (pembayaranDpMasterAssetDetail.getId() != null) {
					Common.refreshUpdate(session, (pembayaranDpMasterAssetDetail));
				}
			}
		});

	}

	public PenyediaAsset getPenyediaAsset() {
		return penyediaAsset;
	}

	public void setPenyediaAsset(PenyediaAsset penyediaAsset) {
		this.penyediaAsset = penyediaAsset;
		try {
			loadDataDetail(pembayaranDpMasterAsset);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/asset/helper/PembayaranDpMasterAssetHelper.java:399");
		}
	}

}
