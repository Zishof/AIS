package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.AssetDetail;
import ais.database.model.asset.PeminjamanMasterAsset;
import ais.database.model.asset.PeminjamanMasterAssetDetail;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class PeminjamanMasterAssetHelper {

	private MyGrid gridMasterAsset;

	private boolean edit = false;
	private boolean delete = false;

	private Boolean pengembalian = false;

	private boolean persetujuan = false;

	public PeminjamanMasterAssetHelper(MyGrid gridMasterAsset, Boolean pengembalian) {
		this.gridMasterAsset = gridMasterAsset;
		this.pengembalian = pengembalian;
	}

	public MyGroupboxStyled initDetail(final PeminjamanMasterAsset peminjamanMasterAsset, boolean persetujuan)
			throws Exception {
		this.persetujuan = persetujuan;
		MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
		myGroupboxStyled.appendChild(
				new MyCaptionStyled("Daftar " + (pengembalian ? "Pengembalian" : "Peminjaman") + " Alat/Fasilitas"));

		if (peminjamanMasterAsset == null) {
			return myGroupboxStyled;
		}

		edit = peminjamanMasterAsset.getDisetujuiOleh() == null;
		delete = peminjamanMasterAsset.getDisetujuiOleh() == null;

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(!persetujuan);
		toolbar.setHeight("30px");
		toolbar.setParent(myGroupboxStyled);

		if (!pengembalian) {
			MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Ambil Data Alat/Fasilitas", "/img/new.gif");
			add.setParent(toolbar);
			add.setVisible(peminjamanMasterAsset.getDisetujuiOleh() == null);
			add.setTooltiptext("Tambah");
			add.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					List<AssetDetail> masterAssets = new ArrayList<AssetDetail>();
					List<Row> myrows = gridMasterAsset.getRows().getChildren();
					for (Row row : myrows) {

						AssetDetail assetDetail = ((PeminjamanMasterAssetDetail) row
								.getAttribute("peminjamanMasterAssetDetail")).getAssetDetail();
						if (assetDetail != null) {
							masterAssets.add(assetDetail);
						}
					}
					AmbilDataAssetDetailBanyak ambilDataAssetDetailBanyak = new AmbilDataAssetDetailBanyak(masterAssets,
							null, true);
					ambilDataAssetDetailBanyak.setHeight("95%");
					ambilDataAssetDetailBanyak.setWidth("90%");
					ambilDataAssetDetailBanyak
							.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					ambilDataAssetDetailBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<AssetDetail> assetDetails = (List<AssetDetail>) arg0.getData();
							for (AssetDetail assetDetail : assetDetails) {
								PeminjamanMasterAssetDetail peminjamanAssetDetailDetail = new PeminjamanMasterAssetDetail();
								peminjamanAssetDetailDetail.setAssetDetail(assetDetail);
								peminjamanAssetDetailDetail.setKeterangan("");
								peminjamanAssetDetailDetail.setPeminjamanMasterAsset(peminjamanMasterAsset);
								peminjamanAssetDetailDetail.setMasterAsset(assetDetail.getAsset().getMasterAsset());

								if (peminjamanMasterAsset.getId() != null) {
									Session session = HibernateUtil.currentSession();
									session.save(peminjamanAssetDetailDetail);
								}

								Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
								rows.setParent(gridMasterAsset);
								Row row = new Row();row.setValign("top");
								row.setParent(rows);
								initRow(row, peminjamanAssetDetailDetail);
							}

						}
					});

					ambilDataAssetDetailBanyak.onModal();

				}
			});
		}

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.setDisabled(peminjamanMasterAsset.getId() != null && peminjamanMasterAsset.getDisetujuiOleh() != null);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(peminjamanMasterAsset);
			}
		});

		myGroupboxStyled.appendChild(gridMasterAsset);
		gridMasterAsset.setWidth("100%");
		gridMasterAsset.setHeight("100%");
		gridMasterAsset.setStyle("min-height:350px");
		gridMasterAsset.setMold("paging");
		gridMasterAsset.setPageSize(10);
		gridMasterAsset.getPagingChild().setMold("os");
		Columns columns = new Columns();
		columns.setParent(gridMasterAsset);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Barcode");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("25%");

		if (pengembalian) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Dikembalikan");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Tgl. Pengembalian");
			column.setWidth("15%");
		}

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadDataDetail(peminjamanMasterAsset);

		return myGroupboxStyled;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final PeminjamanMasterAsset peminjamanMasterAsset) throws Exception {

		List<PeminjamanMasterAssetDetail> peminjamanMasterAssetDetails = peminjamanMasterAsset == null
				|| peminjamanMasterAsset.getId() == null ? new ArrayList<PeminjamanMasterAssetDetail>()
						: HibernateUtil.currentSession().createCriteria(PeminjamanMasterAssetDetail.class)
								.add(Restrictions.eq("peminjamanMasterAsset", peminjamanMasterAsset)).list();

		Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
		rows.setParent(gridMasterAsset);

		Common.clear(rows);

		for (PeminjamanMasterAssetDetail peminjamanMasterAssetDetail : peminjamanMasterAssetDetails) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, peminjamanMasterAssetDetail);
		}

	}

	public void initRow(final Row row, final PeminjamanMasterAssetDetail peminjamanMasterAssetDetail) throws Exception {

		row.setValign("top");row.setAttribute("peminjamanMasterAssetDetail", peminjamanMasterAssetDetail);

		new Label(peminjamanMasterAssetDetail.getAssetDetail() == null ? ""
				: peminjamanMasterAssetDetail.getAssetDetail().getBarcode()).setParent(row);

		RevisiHelper.createNewRevisi(PeminjamanMasterAssetDetail.class, peminjamanMasterAssetDetail,
				peminjamanMasterAssetDetail.getAssetDetail() == null ? ""
						: peminjamanMasterAssetDetail.getAssetDetail().getNama())
				.setParent(row);

		if (pengembalian) {
			final MyDatebox waktuPengembalian = new MyDatebox(peminjamanMasterAssetDetail.getWaktuPengembalian());

			final MyCheckboxConfig dikembalikan = new MyCheckboxConfig("Dikembalikan");
			dikembalikan.setChecked(peminjamanMasterAssetDetail.getDikembalikan());

			if (persetujuan) {
				new Label("Dikembalikan : " + (peminjamanMasterAssetDetail.getDikembalikan() ? "Ya" : "Tidak"))
						.setParent(row);
			} else {
				dikembalikan.setParent(row);
			}
			dikembalikan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					peminjamanMasterAssetDetail.setDikembalikan(dikembalikan.isChecked());
					waktuPengembalian.setDisabled(dikembalikan.isChecked());
					if (dikembalikan.isChecked() && waktuPengembalian.getValue() == null) {
						waktuPengembalian.setValue(WaktuUtil.getDate());
						peminjamanMasterAssetDetail.setWaktuPengembalian(waktuPengembalian.getValue());
					}

					Common.refreshUpdate(session, (peminjamanMasterAssetDetail));

				}
			});
			waktuPengembalian.setDisabled(dikembalikan.isChecked());
			waktuPengembalian.setFormat(Common.dateFormat.get().toPattern());
			if (persetujuan) {
				new Label(peminjamanMasterAssetDetail.getWaktuPengembalian() == null ? ""
						: Common.dateFormat.get().format(peminjamanMasterAssetDetail.getWaktuPengembalian())).setParent(row);
			} else {
				(waktuPengembalian).setParent(row);
			}
			waktuPengembalian.setWidth("90%");
			waktuPengembalian.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					peminjamanMasterAssetDetail.setWaktuPengembalian(waktuPengembalian.getValue());
					Common.refreshUpdate(session, (peminjamanMasterAssetDetail));

				}
			});

		}
		final MyTextbox keterangan = new MyTextbox(
				peminjamanMasterAssetDetail.getKeterangan() == null ? "" : peminjamanMasterAssetDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");

		if (persetujuan) {
			new Label(peminjamanMasterAssetDetail.getKeterangan()).setParent(row);
		} else {
			keterangan.setParent(row);
		}
		keterangan.setDisabled(
				peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getDisetujuiOleh() != null || !edit);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				peminjamanMasterAssetDetail.setKeterangan(keterangan.getValue());
				row.setValign("top");row.setAttribute("peminjamanMasterAssetDetail", peminjamanMasterAssetDetail);
				if (peminjamanMasterAssetDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (peminjamanMasterAssetDetail));
				}
			}
		});

		Hbox hbox = new Hbox();
		hbox.setParent(row);
		if (!persetujuan) {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.setParent(hbox);

			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										if (peminjamanMasterAssetDetail.getId() != null) {
											Session session = HibernateUtil.currentSession();
											session.delete(peminjamanMasterAssetDetail);
										}
										row.setVisible(false);
										row.detach();
									}

								}
							});

				}
			});
		}
	}

}
