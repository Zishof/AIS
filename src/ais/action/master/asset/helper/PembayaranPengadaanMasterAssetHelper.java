package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.PembayaranPengadaanMasterAsset;
import ais.database.model.asset.PembayaranPengadaanMasterAssetDetail;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class PembayaranPengadaanMasterAssetHelper {

	private MyGrid gridPenerimaanPengadaanMasterAsset;
	private boolean edit = false;
	private PenyediaAsset penyediaAsset = null;
	private double totalSemua = 0.0;
	private double totalDibayar = 0.0;
	private Footer footerTotalDibayar;
	private Footer footerTotalSemua;
	private PembayaranPengadaanMasterAsset pembayaranPengadaanMasterAsset;
	private boolean persetujuan = false;

	public PembayaranPengadaanMasterAssetHelper(MyGrid gridPenerimaanPengadaanMasterAsset) {
		this.gridPenerimaanPengadaanMasterAsset = gridPenerimaanPengadaanMasterAsset;

	}

	public Groupbox initDetail(final PembayaranPengadaanMasterAsset pembayaranPengadaanMasterAsset, boolean persetujuan)
			throws Exception {
		this.persetujuan = persetujuan;
		this.pembayaranPengadaanMasterAsset = pembayaranPengadaanMasterAsset;
		MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
		myGroupboxStyled.appendChild(new MyCaptionStyled("Daftar Pembayaran Tagihan Barang/Jasa"));

		edit = pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null;

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(!persetujuan);
		toolbar.setHeight("30px");
		toolbar.setParent(myGroupboxStyled);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.setDisabled(pembayaranPengadaanMasterAsset.getId() != null
				&& pembayaranPengadaanMasterAsset.getDisetujuiOleh() != null);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(pembayaranPengadaanMasterAsset);
			}
		});

		Columns columns = new Columns();
		columns.setParent(gridPenerimaanPengadaanMasterAsset);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/Keterangan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tagihan");
		column.setWidth("12%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tertagih");
		column.setWidth("12%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sisa");
		column.setWidth("12%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dibayar");
		column.setWidth("12%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		gridPenerimaanPengadaanMasterAsset.setParent(myGroupboxStyled);
		gridPenerimaanPengadaanMasterAsset.setWidth("100%");
		gridPenerimaanPengadaanMasterAsset.setHeight("100%");
		gridPenerimaanPengadaanMasterAsset.setStyle("min-height:350px");
		gridPenerimaanPengadaanMasterAsset.setMold("paging");
		gridPenerimaanPengadaanMasterAsset.setPageSize(10);
		gridPenerimaanPengadaanMasterAsset.getPagingChild().setMold("os");
		footerTotalSemua = new Footer(Common.numberFormat.get().format(totalSemua));
		loadDataDetail(pembayaranPengadaanMasterAsset);

		Foot foot = new Foot();
		foot.setParent(gridPenerimaanPengadaanMasterAsset);

		Footer footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		foot.appendChild(footerTotalSemua);

		footerTotalDibayar = new Footer(Common.numberFormat.get().format(totalDibayar));
		foot.appendChild(footerTotalDibayar);

		footer = new Footer();
		foot.appendChild(footer);

		return myGroupboxStyled;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final PembayaranPengadaanMasterAsset pembayaranPengadaanMasterAsset) throws Exception {

		if (pembayaranPengadaanMasterAsset.getPenyedia() != null) {
			penyediaAsset = pembayaranPengadaanMasterAsset.getPenyedia();
		}

		Rows rows = gridPenerimaanPengadaanMasterAsset.getRows() == null ? new Rows()
				: gridPenerimaanPengadaanMasterAsset.getRows();
		rows.setParent(gridPenerimaanPengadaanMasterAsset);
		Common.clear(rows);
		if (penyediaAsset == null) {
			return;
		}
		List<PembayaranPengadaanMasterAssetDetail> pembayaranPengadaanMasterAssetDetails;
		if (pembayaranPengadaanMasterAsset.getId() != null) {
			Session session = HibernateUtil.currentNativeSession();
			pembayaranPengadaanMasterAssetDetails = session.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("pembayaranPengadaanMasterAsset", pembayaranPengadaanMasterAsset)).list();
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		} else {

			pembayaranPengadaanMasterAssetDetails = new ArrayList<PembayaranPengadaanMasterAssetDetail>();

			Session session = HibernateUtil.currentNativeSession();
			List<SaldoAwalMasterAsset> saldoAwalMasterAssets = session.createCriteria(SaldoAwalMasterAsset.class)
					.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.eq("penerimaanPengadaanMasterAsset.penyedia", penyediaAsset),
							Restrictions.eq("penyedia", penyediaAsset)))
					.list();

			totalDibayar = 0.0;
			for (SaldoAwalMasterAsset saldoAwalMasterAsset : saldoAwalMasterAssets) {

				Number nilaiTagihan = (Number) session.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
						.add(Restrictions.eq("saldoAwalMasterAsset", saldoAwalMasterAsset))
						.setProjection(Projections.sum("dibayar")).uniqueResult();
				Double d = nilaiTagihan == null ? 0.0 : nilaiTagihan.doubleValue();
				saldoAwalMasterAsset.setDibayar(d);

				if (!saldoAwalMasterAsset.getLunas()) {
					PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail = new PembayaranPengadaanMasterAssetDetail();
					pembayaranPengadaanMasterAssetDetail.setDibayar(0.0);
					pembayaranPengadaanMasterAssetDetail
							.setPembayaranPengadaanMasterAsset(pembayaranPengadaanMasterAsset);
					pembayaranPengadaanMasterAssetDetail.setPenerimaanPengadaanMasterAsset(
							saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset());
					pembayaranPengadaanMasterAssetDetail.setSaldoAwalMasterAsset(saldoAwalMasterAsset);

					pembayaranPengadaanMasterAssetDetail.setTotalTelahDibayar(d);
					saldoAwalMasterAsset.setDibayar(pembayaranPengadaanMasterAssetDetail.getTotalTelahDibayar());
					session.getTransaction().begin();
					Common.refreshUpdate(session, saldoAwalMasterAsset);
					session.getTransaction().commit();

					if (pembayaranPengadaanMasterAsset != null && pembayaranPengadaanMasterAsset.getId() != null) {
						session.getTransaction().begin();
						session.save(pembayaranPengadaanMasterAssetDetail);
						session.getTransaction().commit();
					}

					totalDibayar += pembayaranPengadaanMasterAssetDetail.getDibayar();

					pembayaranPengadaanMasterAssetDetails.add(pembayaranPengadaanMasterAssetDetail);
				}
			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		}

		totalSemua = 0.0;
		for (PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail : pembayaranPengadaanMasterAssetDetails) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, pembayaranPengadaanMasterAssetDetail);
		}

		Common.createDefaultTimer(eventListenerHitungUlang);
	}

	private EventListener eventListenerHitungUlang = new EventListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(Event arg0) throws Exception {
			List<Row> rows = gridPenerimaanPengadaanMasterAsset.getRows().getChildren();

			totalSemua = 0.0;
			totalDibayar = 0.0;
			for (Row row : rows) {
				if (row.isVisible()) {
					PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail = (PembayaranPengadaanMasterAssetDetail) row
							.getAttribute("pembayaranPengadaanMasterAssetDetail");
					PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset = pembayaranPengadaanMasterAssetDetail
							.getPenerimaanPengadaanMasterAsset();
					Double telahDibayar = 0.0;

					try {
						telahDibayar = pembayaranPengadaanMasterAssetDetail.getSaldoAwalMasterAsset().getDibayar()
								+ (pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() == null
										? 0.0
										: pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
												.getPemesananPengadaanMasterAsset().getDptotal());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/PembayaranPengadaanMasterAssetHelper.java:270");
						// TODO: handle exception
					}

					Double nilaitagihan = 0.0;
					try {
						nilaitagihan = pembayaranPengadaanMasterAssetDetail.getSaldoAwalMasterAsset() == null
								? (penerimaanPengadaanMasterAsset == null ? 0.0
										: penerimaanPengadaanMasterAsset.getNilai())
								: pembayaranPengadaanMasterAssetDetail.getSaldoAwalMasterAsset().getNilai();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/PembayaranPengadaanMasterAssetHelper.java:280");
						// TODO: handle exception
					}
					Double j = nilaitagihan - telahDibayar;

					totalSemua += j;
					totalDibayar += pembayaranPengadaanMasterAssetDetail.getDibayar();
				}
			}

			footerTotalDibayar.setLabel(Common.numberFormat.get().format(totalDibayar));
			footerTotalSemua.setLabel(Common.numberFormat.get().format(totalSemua));
		}
	};

	public void initRow(final Row row, final PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail)
			throws Exception {

		row.setValign("top");
		row.setAttribute("pembayaranPengadaanMasterAssetDetail", pembayaranPengadaanMasterAssetDetail);
		PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAssetData = pembayaranPengadaanMasterAssetDetail
				.getPenerimaanPengadaanMasterAsset();
		SaldoAwalMasterAsset saldoAwalMasterAsset = pembayaranPengadaanMasterAssetDetail.getSaldoAwalMasterAsset();
		PembayaranPengadaanMasterAsset pembayaranPengadaanMasterAsset = pembayaranPengadaanMasterAssetDetail
				.getPembayaranPengadaanMasterAsset();

		if (penerimaanPengadaanMasterAssetData != null) {
			new PenerimaanPengadaanMasterAssetDetailAction(penerimaanPengadaanMasterAssetData, false).setParent(row);

			Session session = HibernateUtil.currentSession();
			Number nilaiTagihan = (Number) session.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
					.createAlias("saldoAwalMasterAsset", "saldoAwalMasterAsset", Criteria.LEFT_JOIN)
					.add(Restrictions.eq("saldoAwalMasterAsset.penerimaanPengadaanMasterAsset",
							penerimaanPengadaanMasterAssetData))
					.setProjection(Projections.sum("dibayar")).uniqueResult();
			System.out.println("penerimaanPengadaanMasterAsset nilaiTagihan -> " + nilaiTagihan);

		} else if (saldoAwalMasterAsset != null) {
			new SaldoAwalMasterAssetDetailAction(saldoAwalMasterAsset, edit).setParent(row);

			Session session = HibernateUtil.currentSession();
			Number nilaiTagihan = (Number) session.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("saldoAwalMasterAsset", saldoAwalMasterAsset))
					.setProjection(Projections.sum("dibayar")).uniqueResult();
			System.out.println("saldoAwalMasterAsset nilaiTagihan -> " + nilaiTagihan);
		}

		Vbox myvbox = new Vbox();
		myvbox.setParent(row);

		final MyCheckboxConfig pilih;
		pilih = new MyCheckboxConfig(
				penerimaanPengadaanMasterAssetData != null ? penerimaanPengadaanMasterAssetData.getKode()
						: saldoAwalMasterAsset != null ? saldoAwalMasterAsset.getKode() : "");
		pilih.setChecked(pembayaranPengadaanMasterAssetDetail.getPilih());
		row.setValign("top");
		row.setAttribute("pilih", pilih);

		if (pembayaranPengadaanMasterAssetDetail.getId() != null) {
			myvbox.appendChild(new MyLabelKecil(
					penerimaanPengadaanMasterAssetData != null ? penerimaanPengadaanMasterAssetData.getKode()
							: saldoAwalMasterAsset != null ? saldoAwalMasterAsset.getKode() : ""));
		} else {
			myvbox.appendChild(pilih);
		}

		myvbox.appendChild(new MyLabelKecil(
				penerimaanPengadaanMasterAssetData != null ? penerimaanPengadaanMasterAssetData.getKodeTagihan() : ""));

		myvbox.appendChild(new MyLabelKecil(
				penerimaanPengadaanMasterAssetData != null ? penerimaanPengadaanMasterAssetData.getKeterangan()
						: saldoAwalMasterAsset != null ? saldoAwalMasterAsset.getKeterangan() : ""));

		if (penerimaanPengadaanMasterAssetData != null
				&& penerimaanPengadaanMasterAssetData.getKeteranganTermin() != null
				&& !penerimaanPengadaanMasterAssetData.getKeteranganTermin().trim().isEmpty()) {
			new MyLabelKecil(penerimaanPengadaanMasterAssetData.getKeteranganTermin()).setParent(myvbox);
		}

		Hbox hbox = new Hbox();
		hbox.setParent(myvbox);
		LampiranLain.createDownloadUploadFileLain(hbox, pembayaranPengadaanMasterAsset.getId(),
				PembayaranPengadaanMasterAsset.class.getName(), "Tagihan", false, null, null, false, false, false,
				false);

		Double nilaitagihan = penerimaanPengadaanMasterAssetData != null
				&& penerimaanPengadaanMasterAssetData.getSaldoAwalMasterAsset() == null
						? penerimaanPengadaanMasterAssetData.getNilai()
						: saldoAwalMasterAsset != null ? saldoAwalMasterAsset.getNilai() : 0.0;

//		nilaitagihan = nilaitagihan - pembayaranPengadaanMasterAssetDetail.hitungPajak();

		if (penerimaanPengadaanMasterAssetData != null) {
			RevisiHelper.createNewRevisi(PenerimaanPengadaanMasterAsset.class, penerimaanPengadaanMasterAssetData,
					Common.numberFormat.get().format(nilaitagihan), "font-size:10px").setParent(row);
		} else if (saldoAwalMasterAsset != null) {
			RevisiHelper.createNewRevisi(SaldoAwalMasterAsset.class, saldoAwalMasterAsset,
					Common.numberFormat.get().format(nilaitagihan), "font-size:10px").setParent(row);
		} else {
			new MyLabelKecil(Common.numberFormat.get().format(nilaitagihan)).setParent(row);
		}
		Double telahDibayar = 0.0;

		try {
			telahDibayar = pembayaranPengadaanMasterAssetDetail.getSaldoAwalMasterAsset().getDibayar()
					+ (penerimaanPengadaanMasterAssetData == null ? 0.0
							: penerimaanPengadaanMasterAssetData.getPemesananPengadaanMasterAsset().getDptotal());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/PembayaranPengadaanMasterAssetHelper.java:387");
			// TODO: handle exception
		}

		Label jumlah = new MyLabelKecil(Common.numberFormat.get().format(telahDibayar));

		(jumlah).setParent(row);

		Double j = nilaitagihan - telahDibayar;

		if (penerimaanPengadaanMasterAssetData != null && penerimaanPengadaanMasterAssetData.getId() == null) {
			row.setVisible(j.intValue() > 0);
		}

		totalSemua += j;

		jumlah = new MyLabelKecil(Common.numberFormat.get().format(j));

		(jumlah).setParent(row);

		final MyDoublebox dibayar = new MyDoublebox(
				pembayaranPengadaanMasterAssetDetail.getDibayar().intValue() == 0 ? j
						: pembayaranPengadaanMasterAssetDetail.getDibayar());

		if (pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset().getDisetujuiOleh() != null
				|| persetujuan) {
			jumlah = new MyLabelKecil(Common.numberFormat.get().format(pembayaranPengadaanMasterAssetDetail.getDibayar()));
			(jumlah).setParent(row);
		} else {
			(dibayar).setParent(row);
		}
		dibayar.setDisabled(pembayaranPengadaanMasterAsset.getDisetujuiOleh() != null || !edit);
		dibayar.setStyle("text-align:right");
		dibayar.setWidth("90%");
		dibayar.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Double saldo = Math.abs(dibayar.getValue() == null ? 0.0 : dibayar.getValue());
				dibayar.setValue(saldo);
				pembayaranPengadaanMasterAssetDetail.setPilih(pilih.isChecked());
				pembayaranPengadaanMasterAssetDetail.setDibayar(saldo);
				if (pembayaranPengadaanMasterAssetDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pembayaranPengadaanMasterAssetDetail));
				}
				row.setValign("top");
				row.setAttribute("pembayaranPengadaanMasterAssetDetail", pembayaranPengadaanMasterAssetDetail);

				eventListenerHitungUlang.onEvent(arg0);
			}
		});

		pilih.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				dibayar.setDisabled(!pilih.isChecked());

				Double saldo = Math.abs(dibayar.getValue() == null ? 0.0 : dibayar.getValue());
				dibayar.setValue(saldo);
				pembayaranPengadaanMasterAssetDetail.setPilih(pilih.isChecked());
				pembayaranPengadaanMasterAssetDetail.setDibayar(saldo);
				if (pembayaranPengadaanMasterAssetDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pembayaranPengadaanMasterAssetDetail));
				}
				row.setValign("top");
				row.setAttribute("pembayaranPengadaanMasterAssetDetail", pembayaranPengadaanMasterAssetDetail);

				eventListenerHitungUlang.onEvent(arg0);
			}
		});

		dibayar.setDisabled(!pilih.isChecked());

		final MyTextbox keterangan = new MyTextbox(pembayaranPengadaanMasterAssetDetail.getKeterangan() == null ? ""
				: pembayaranPengadaanMasterAssetDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");
		if (persetujuan) {
			new MyLabelKecil(pembayaranPengadaanMasterAssetDetail.getKeterangan()).setParent(row);
		} else {
			keterangan.setParent(row);
		}
		keterangan.setDisabled(
				pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset().getDisetujuiOleh() != null
						|| !edit);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				pembayaranPengadaanMasterAssetDetail.setKeterangan(keterangan.getValue());

				row.setValign("top");
				row.setAttribute("pembayaranPengadaanMasterAssetDetail", pembayaranPengadaanMasterAssetDetail);
				if (pembayaranPengadaanMasterAssetDetail.getId() != null) {
					Common.refreshUpdate(session, (pembayaranPengadaanMasterAssetDetail));
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
			loadDataDetail(pembayaranPengadaanMasterAsset);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/asset/helper/PembayaranPengadaanMasterAssetHelper.java:503");
		}
	}

}
