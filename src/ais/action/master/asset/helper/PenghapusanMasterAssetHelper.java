package ais.action.master.asset.helper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.AssetDetail;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PenghapusanMasterAsset;
import ais.database.model.asset.PenghapusanMasterAssetDetail;
import ais.database.model.asset.PenyusutanAsset;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class PenghapusanMasterAssetHelper {

	private MyGrid gridMasterAsset;

	private boolean edit = false;
	private boolean delete = false;

	private Boolean disetujui = false;

	protected double totalSemua;

	private Footer footerTotalSemua;

	public PenghapusanMasterAssetHelper(MyGrid gridMasterAsset, Boolean disetujui) {
		this.gridMasterAsset = gridMasterAsset;
		this.disetujui = disetujui;
	}

	public static void masukkanPenyusutan(Calendar calendar1, PenghapusanMasterAsset penghapusanMasterAsset,
			PenghapusanMasterAssetDetail penghapusanAssetDetailDetail, AssetDetail assetDetail, Session session) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(assetDetail.getTanggalBeli());
		calendar.set(Calendar.MONTH, calendar1.get(Calendar.MONTH));
		calendar.set(Calendar.YEAR, calendar1.get(Calendar.YEAR));
		Date perTanggal = calendar.getTime();

		PenyusutanAsset penyusutanAsset = (PenyusutanAsset) session.createCriteria(PenyusutanAsset.class)
				.add(Restrictions.eq("assetDetail", assetDetail)).add(Restrictions.eq("perTanggal", perTanggal))
				.setMaxResults(1).uniqueResult();

		if (penyusutanAsset == null) {

			long monthsBetween = ChronoUnit.MONTHS.between(
					YearMonth.from(LocalDate.parse(Common.databaseDateFormat.get().format(assetDetail.getTanggalBeli()))),
					YearMonth.from(LocalDate
							.parse(Common.databaseDateFormat.get().format(penghapusanMasterAsset.getTanggalPembuatan()))));

			int selisih = (int) monthsBetween;
			if (selisih >= 0) {
				for (int j = 0; j <= selisih; j++) {

					penyusutanAsset = (PenyusutanAsset) session.createCriteria(PenyusutanAsset.class)
							.add(Restrictions.eq("assetDetail", assetDetail)).add(Restrictions.eq("tahunKe", j))
							.setMaxResults(1).uniqueResult();
					if (penyusutanAsset == null) {
						penyusutanAsset = new PenyusutanAsset();
					}
					penyusutanAsset.setAssetDetail(assetDetail);
					penyusutanAsset.setTahunKe(j);

					session.saveOrUpdate(penyusutanAsset);
					session.flush();
				}
			}

		}

		penghapusanAssetDetailDetail.setPenyusutanAsset(penyusutanAsset);
	}

	public MyGroupboxStyled initDetail(final PenghapusanMasterAsset penghapusanMasterAsset) throws Exception {
		MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
		myGroupboxStyled.appendChild(
				new MyCaptionStyled("Daftar " + (disetujui ? "Disetujui" : "Penghapusan") + " Alat/Fasilitas"));

		if (penghapusanMasterAsset == null) {
			return myGroupboxStyled;
		}

		edit = penghapusanMasterAsset.getDisetujuiOleh() == null;
		delete = penghapusanMasterAsset.getDisetujuiOleh() == null;

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(myGroupboxStyled);

		if (!disetujui) {
			MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Ambil Data Alat/Fasilitas", "/img/new.gif");
			add.setParent(toolbar);
			add.setVisible(penghapusanMasterAsset.getDisetujuiOleh() == null);
			add.setTooltiptext("Tambah");
			add.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					List<AssetDetail> masterAssets = new ArrayList<AssetDetail>();
					List<Row> myrows = gridMasterAsset.getRows().getChildren();
					for (Row row : myrows) {

						AssetDetail assetDetail = ((PenghapusanMasterAssetDetail) row
								.getAttribute("penghapusanMasterAssetDetail")).getAssetDetail();
						if (assetDetail != null) {
							masterAssets.add(assetDetail);
						}
					}
					AmbilDataAssetDetailBanyak ambilDataAssetDetailBanyak = new AmbilDataAssetDetailBanyak(masterAssets,
							null);
					ambilDataAssetDetailBanyak.setHeight("95%");
					ambilDataAssetDetailBanyak.setWidth("90%");
					ambilDataAssetDetailBanyak
							.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					ambilDataAssetDetailBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
							calendar1.setTime(penghapusanMasterAsset.getTanggalPembuatan());

							Session session = HibernateUtil.currentSession();
							List<AssetDetail> assetDetails = (List<AssetDetail>) arg0.getData();
							for (AssetDetail assetDetail : assetDetails) {
								PenghapusanMasterAssetDetail penghapusanAssetDetailDetail = new PenghapusanMasterAssetDetail();
								penghapusanAssetDetailDetail.setAssetDetail(assetDetail);
								penghapusanAssetDetailDetail.setKeterangan("");
								penghapusanAssetDetailDetail.setPenghapusanMasterAsset(penghapusanMasterAsset);
								penghapusanAssetDetailDetail.setMasterAsset(assetDetail.getAsset().getMasterAsset());

								PenghapusanMasterAssetHelper.masukkanPenyusutan(calendar1, penghapusanMasterAsset,
										penghapusanAssetDetailDetail, assetDetail, session);

								if (penghapusanMasterAsset.getId() != null) {
									session.save(penghapusanAssetDetailDetail);
									session.flush();
								}

								Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
								rows.setParent(gridMasterAsset);
								Row row = new Row();row.setValign("top");
								row.setParent(rows);
								initRow(row, penghapusanAssetDetailDetail);
							}

							eventListenerHitungUlang.onEvent(null);
						}
					});

					ambilDataAssetDetailBanyak.onModal();

				}
			});
		}

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.setDisabled(penghapusanMasterAsset.getId() != null && penghapusanMasterAsset.getDisetujuiOleh() != null);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(penghapusanMasterAsset);
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
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai Buku");
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Harga Jual");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		footerTotalSemua = new Footer(Common.numberFormat.get().format(totalSemua));

		loadDataDetail(penghapusanMasterAsset);

		Foot foot = new Foot();
		foot.setParent(gridMasterAsset);

		Footer footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer("Total");
		foot.appendChild(footer);

		foot.appendChild(footerTotalSemua);

		if (disetujui) {
			footer = new Footer();
			foot.appendChild(footer);

			footer = new Footer();
			foot.appendChild(footer);
		}

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		return myGroupboxStyled;
	}

	public EventListener eventListenerHitungUlang = new EventListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(Event arg0) throws Exception {
			List<Row> rows = gridMasterAsset.getRows().getChildren();

			totalSemua = 0.0;
			for (Row row : rows) {
				if (row.isVisible()) {
					PenghapusanMasterAssetDetail penghapusanMasterAssetDetail = (PenghapusanMasterAssetDetail) row
							.getAttribute("penghapusanMasterAssetDetail");

					Double j = penghapusanMasterAssetDetail.getHargaBeli();

					totalSemua += j;
				}
			}

			footerTotalSemua.setLabel(Common.numberFormat.get().format(totalSemua));
		}
	};

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final PenghapusanMasterAsset penghapusanMasterAsset) throws Exception {

		List<PenghapusanMasterAssetDetail> penghapusanMasterAssetDetails = penghapusanMasterAsset == null
				|| penghapusanMasterAsset.getId() == null ? new ArrayList<PenghapusanMasterAssetDetail>()
						: HibernateUtil.currentSession().createCriteria(PenghapusanMasterAssetDetail.class)
								.add(Restrictions.eq("penghapusanMasterAsset", penghapusanMasterAsset)).list();

		Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
		rows.setParent(gridMasterAsset);

		Common.clear(rows);

		for (PenghapusanMasterAssetDetail penghapusanMasterAssetDetail : penghapusanMasterAssetDetails) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, penghapusanMasterAssetDetail);
		}

		eventListenerHitungUlang.onEvent(null);
	}

	public void initRow(final Row row, final PenghapusanMasterAssetDetail penghapusanMasterAssetDetail)
			throws Exception {

		row.setValign("top");row.setAttribute("penghapusanMasterAssetDetail", penghapusanMasterAssetDetail);

		new Label(penghapusanMasterAssetDetail.getAssetDetail() == null ? ""
				: penghapusanMasterAssetDetail.getAssetDetail().getBarcode()).setParent(row);

		Vbox a;
		(a = RevisiHelper.createNewRevisi(PenghapusanMasterAssetDetail.class, penghapusanMasterAssetDetail,
				penghapusanMasterAssetDetail.getAssetDetail() == null ? ""
						: penghapusanMasterAssetDetail.getAssetDetail().getNama()))
				.setParent(row);

		Hbox hbox = new Hbox();
		hbox.setParent(a);

		LampiranLain.createDownloadUploadFileLain(hbox, penghapusanMasterAssetDetail.getId(),
				PenghapusanMasterAssetDetail.class.getName(), "Gambar", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, true, null, false, true);

		new Label(penghapusanMasterAssetDetail.getPenyusutanAsset() == null ? ""
				: Common.numberFormat.get().format(penghapusanMasterAssetDetail.getPenyusutanAsset().getNilaiBuku()))
				.setParent(row);

		final MyDoublebox hargaBeli = new MyDoublebox(penghapusanMasterAssetDetail.getHargaBeli() == null ? 0.0
				: penghapusanMasterAssetDetail.getHargaBeli());

		(hargaBeli).setParent(row);
		hargaBeli.setDisabled(
				penghapusanMasterAssetDetail.getPenghapusanMasterAsset().getDisetujuiOleh() != null || !edit);
		hargaBeli.setStyle("text-align:right");
		hargaBeli.setWidth("90%");
		hargaBeli.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				penghapusanMasterAssetDetail.setHargaBeli(hargaBeli.getValue());
				Common.refreshUpdate(session, (penghapusanMasterAssetDetail));

				MasterAsset masterAsset = penghapusanMasterAssetDetail.getMasterAsset();
				session.refresh(masterAsset);
				masterAsset.setHargaBeliDefault(hargaBeli.getValue());
				Common.refreshUpdate(session, masterAsset);

				row.setValign("top");row.setAttribute("penghapusanMasterAssetDetail", penghapusanMasterAssetDetail);

				eventListenerHitungUlang.onEvent(null);
			}
		});

		final MyTextbox keterangan = new MyTextbox(penghapusanMasterAssetDetail.getKeterangan() == null ? ""
				: penghapusanMasterAssetDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");
		keterangan.setParent(row);
		keterangan.setDisabled(
				penghapusanMasterAssetDetail.getPenghapusanMasterAsset().getDisetujuiOleh() != null || !edit);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				penghapusanMasterAssetDetail.setKeterangan(keterangan.getValue());
				row.setValign("top");row.setAttribute("penghapusanMasterAssetDetail", penghapusanMasterAssetDetail);
				if (penghapusanMasterAssetDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (penghapusanMasterAssetDetail));
				}
			}
		});

		hbox = new Hbox();
		hbox.setParent(row);

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
									if (penghapusanMasterAssetDetail.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(penghapusanMasterAssetDetail);
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
