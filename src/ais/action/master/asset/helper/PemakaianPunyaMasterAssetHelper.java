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
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PemakaianMasterAsset;
import ais.database.model.asset.PemakaianMasterAssetDetail;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class PemakaianPunyaMasterAssetHelper {

	private boolean add = false;
	private boolean edit = false;
	private boolean delete = false;

	public PemakaianPunyaMasterAssetHelper() {

	}

	public MyGroupboxStyled initDetail(final MyGrid gridMasterAsset, final PemakaianMasterAsset pemakaianMasterAsset,
			final AmbilDataSatuanKerjaBanbox dataSasaranBanbox, boolean persetujuan) throws Exception {

		MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
		myGroupboxStyled.appendChild(new MyCaptionStyled("Daftar Pemakaian Barang"));

		edit = pemakaianMasterAsset.getDisetujuiOleh() == null;
		add = pemakaianMasterAsset.getDisetujuiOleh() == null;
		delete = pemakaianMasterAsset.getDisetujuiOleh() == null;

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(!persetujuan);
		toolbar.setHeight("30px");
		toolbar.setParent(myGroupboxStyled);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Produk", "/img/new.gif");
		add.setVisible(PemakaianPunyaMasterAssetHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				if (dataSasaranBanbox.getAttribute("satuanKerja") == null) {
					MyMessageboxConfig.show("Mohon maaf, Satuan Kerja belum dipilih. Langkah yang dapat dilakukan: (1) Pilih satuan kerja dari field filter Satuan Kerja; (2) Setelah satuan kerja dipilih, daftar barang akan muncul; (3) ulangi proses penambahan data pemakaian. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				SatuanKerja satuanKerja = (SatuanKerja) dataSasaranBanbox.getAttribute("satuanKerja");

				List<MasterAsset> masterAssets = new ArrayList<MasterAsset>();
				List<Row> myrows = gridMasterAsset.getRows().getChildren();
				for (Row row : myrows) {
					masterAssets.add(((PemakaianMasterAssetDetail) row.getAttribute("pemakaianMasterAssetDetail"))
							.getMasterAsset());
				}
				AmbilDataAssetBanyakBerdasarkanStok ambilDataMasterAssetBanyak = new AmbilDataAssetBanyakBerdasarkanStok(
						masterAssets, satuanKerja, true);
				ambilDataMasterAssetBanyak.setHeight("95%");
				ambilDataMasterAssetBanyak.setWidth("90%");
				ambilDataMasterAssetBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataMasterAssetBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<MasterAsset> masterAssets = (List<MasterAsset>) arg0.getData();
						for (MasterAsset masterAsset : masterAssets) {
							PemakaianMasterAssetDetail pemakaianMasterAssetDetail = new PemakaianMasterAssetDetail();
							pemakaianMasterAssetDetail.setMasterAsset(masterAsset);
							pemakaianMasterAssetDetail.setJumlah(1.0);
							pemakaianMasterAssetDetail.setKeterangan("");
							pemakaianMasterAssetDetail.setPemakaianMasterAsset(pemakaianMasterAsset);

							if (pemakaianMasterAsset.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.save(pemakaianMasterAssetDetail);
							}

							Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
							rows.setParent(gridMasterAsset);
							Row row = new Row();
							row.setValign("top");
							row.setParent(rows);
							initRow(row, pemakaianMasterAssetDetail);
						}
					}
				});

				ambilDataMasterAssetBanyak.onModal();

			}
		});

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.setDisabled(pemakaianMasterAsset.getDisetujuiOleh() != null);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(gridMasterAsset, pemakaianMasterAsset);
			}
		});

		Common.clear(gridMasterAsset);
		gridMasterAsset.setParent(myGroupboxStyled);
		gridMasterAsset.setWidth("100%");
		gridMasterAsset.setHeight("100%");
		gridMasterAsset.setStyle("min-height:350px");
		Columns columns = new Columns();
		columns.setParent(gridMasterAsset);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Merk");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadDataDetail(gridMasterAsset, pemakaianMasterAsset);

		return myGroupboxStyled;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(MyGrid gridMasterAsset, final PemakaianMasterAsset pemakaianMasterAsset)
			throws Exception {

		List<PemakaianMasterAssetDetail> pemakaianMasterAssetDetails = pemakaianMasterAsset == null
				|| pemakaianMasterAsset.getId() == null ? new ArrayList<PemakaianMasterAssetDetail>()
						: HibernateUtil.currentSession().createCriteria(PemakaianMasterAssetDetail.class)
								.add(Restrictions.eq("pemakaianMasterAsset", pemakaianMasterAsset)).list();

		Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
		rows.setParent(gridMasterAsset);
		
		Common.clear(rows);

		for (PemakaianMasterAssetDetail pemakaianMasterAssetDetail : pemakaianMasterAssetDetails) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, pemakaianMasterAssetDetail);
		}
	}

	public void initRow(final Row row, final PemakaianMasterAssetDetail pemakaianMasterAssetDetail) throws Exception {
		MasterAsset masterAsset = pemakaianMasterAssetDetail.getMasterAsset();
		row.setValign("top");
		row.setAttribute("pemakaianMasterAssetDetail", pemakaianMasterAssetDetail);

		final MyDoublebox jumlah = new MyDoublebox(
				pemakaianMasterAssetDetail.getJumlah() == null ? 0.0 : pemakaianMasterAssetDetail.getJumlah());

		RevisiHelper.createNewRevisi(PemakaianMasterAssetDetail.class, pemakaianMasterAssetDetail,
				pemakaianMasterAssetDetail.getMasterAsset() == null ? ""
						: pemakaianMasterAssetDetail.getMasterAsset().getNama())
				.setParent(row);

		new Label(masterAsset.getMerk()).setParent(row);

		(jumlah).setParent(row);
		jumlah.setDisabled(pemakaianMasterAssetDetail.getPemakaianMasterAsset().getDisetujuiOleh() != null || !edit);
		jumlah.setStyle("text-align:right");
		jumlah.setWidth("90%");
		jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Double saldo = Math.abs(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
				jumlah.setValue(saldo);
				pemakaianMasterAssetDetail.setJumlah(saldo);
				row.setValign("top");
				row.setAttribute("pemakaianMasterAssetDetail", pemakaianMasterAssetDetail);
				if (pemakaianMasterAssetDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pemakaianMasterAssetDetail));
				}
			}
		});

		final MyTextbox keterangan = new MyTextbox(
				pemakaianMasterAssetDetail.getKeterangan() == null ? "" : pemakaianMasterAssetDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");
		keterangan.setParent(row);
		keterangan
				.setDisabled(pemakaianMasterAssetDetail.getPemakaianMasterAsset().getDisetujuiOleh() != null || !edit);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pemakaianMasterAssetDetail.setKeterangan(keterangan.getValue());
				row.setValign("top");
				row.setAttribute("pemakaianMasterAssetDetail", pemakaianMasterAssetDetail);
				if (pemakaianMasterAssetDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pemakaianMasterAssetDetail));
				}
			}
		});

		Hbox hbox = new Hbox();
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
									if (pemakaianMasterAssetDetail.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(pemakaianMasterAssetDetail);
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
