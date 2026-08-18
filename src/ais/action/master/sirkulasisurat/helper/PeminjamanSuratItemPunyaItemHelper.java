package ais.action.master.sirkulasisurat.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.surat.helper.AmbilDataSuratMasukBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoGambarSuratMasuk;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sirkulasisurat.PeminjamanSuratItem;
import ais.database.model.sirkulasisurat.PeminjamanSuratItemDetail;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class PeminjamanSuratItemPunyaItemHelper {

	private boolean edit = false;
	private boolean delete = false;

	private String tipe;
	private boolean persetujuan;

	public PeminjamanSuratItemPunyaItemHelper(String tipe, boolean persetujuan) {
		this.tipe = tipe;
		this.persetujuan = persetujuan;
//		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
//		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
//		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Groupbox initDetail(final PeminjamanSuratItem peminjamanSuratItem, final MyGrid gridItem,
			final AmbilDataSatuanKerjaBanbox kepadaSatuanKerja) throws Exception {

		MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
		myGroupboxStyled.appendChild(new MyCaptionStyled("Daftar " + tipe));

		if (!persetujuan) {
			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("30px");
			toolbar.setParent(myGroupboxStyled);

			MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Ambil Data " + tipe, "/img/add_item.png");
//			add.setVisible(PeminjamanSuratItemPunyaItemHelper.this.add);
			add.setParent(toolbar);
			add.setTooltiptext("Tambah");
			add.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					if (kepadaSatuanKerja.getAttribute("satuanKerja") == null) {
						MyMessageboxConfig.show("Mohon maaf, Satuan Kerja Kepada belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Satuan Kerja Kepada pada formulir; (2) pilih satuan kerja tujuan yang sesuai dari daftar; (3) klik tombol untuk mencari item surat kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						return;
					}

					Session session = HibernateUtil.currentSession();

					List<SuratMasuk> suratMasuks = peminjamanSuratItem == null || peminjamanSuratItem.getId() == null
							? new ArrayList<SuratMasuk>()
							: session.createCriteria(PeminjamanSuratItemDetail.class)
									.setProjection(Projections.groupProperty("suratMasuk"))
									.add(Restrictions.eq("peminjamanSuratItem", peminjamanSuratItem)).list();

					AmbilDataSuratMasukBanyak ambilDataItemBanyak = new AmbilDataSuratMasukBanyak(suratMasuks, tipe,
							true, (SatuanKerja) kepadaSatuanKerja.getAttribute("satuanKerja"));
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
					ambilDataItemBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<SuratMasuk> items = (List<SuratMasuk>) arg0.getData();

							System.out.println("items -> " + items + " peminjamanSuratItem " + peminjamanSuratItem);

							if (peminjamanSuratItem != null && peminjamanSuratItem.getId() != null) {

								Session session = HibernateUtil.currentSession();
								for (SuratMasuk suratMasuk : items) {
									PeminjamanSuratItemDetail peminjamanSuratItemDetail = new PeminjamanSuratItemDetail();
									peminjamanSuratItemDetail.setSuratMasuk(suratMasuk);
									peminjamanSuratItemDetail.setJumlah(1.0);
									peminjamanSuratItemDetail.setKeterangan("");
									peminjamanSuratItemDetail.setPeminjamanSuratItem(peminjamanSuratItem);
									session.save(peminjamanSuratItemDetail);
								}

								loadDataDetail(peminjamanSuratItem, gridItem);
							} else {

								Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
								rows.setParent(gridItem);

								for (SuratMasuk suratMasuk : items) {
									PeminjamanSuratItemDetail peminjamanSuratItemDetail = new PeminjamanSuratItemDetail();
									peminjamanSuratItemDetail.setSuratMasuk(suratMasuk);
									peminjamanSuratItemDetail.setJumlah(1.0);
									peminjamanSuratItemDetail.setKeterangan("");
									peminjamanSuratItemDetail.setPeminjamanSuratItem(peminjamanSuratItem);

									Row row = new Row();
									row.setValign("top");
									row.setParent(rows);
									initRow(row, peminjamanSuratItemDetail);
								}
							}
						}
					});
					ambilDataItemBanyak.setWidth("97%");
					ambilDataItemBanyak.setHeight("97%");
					ambilDataItemBanyak.setVisible(true);
					ambilDataItemBanyak.onModal();
				}

			});

		}

		Common.clear(gridItem);
		gridItem.setParent(myGroupboxStyled);
		gridItem.setWidth("100%");
		gridItem.setStyle("min-height:350px");
		gridItem.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridItem);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nomor Surat/Agenda");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Perihal");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadDataDetail(peminjamanSuratItem, gridItem);

		return myGroupboxStyled;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final PeminjamanSuratItem peminjamanSuratItem, final MyGrid gridItem) throws Exception {

		List<PeminjamanSuratItemDetail> peminjamanSuratItemDetails = peminjamanSuratItem == null
				|| peminjamanSuratItem.getId() == null ? new ArrayList<PeminjamanSuratItemDetail>()
						: HibernateUtil.currentSession().createCriteria(PeminjamanSuratItemDetail.class)
								.add(Restrictions.eq("peminjamanSuratItem", peminjamanSuratItem)).list();

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);

		for (PeminjamanSuratItemDetail peminjamanSuratItemDetail : peminjamanSuratItemDetails) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, peminjamanSuratItemDetail);
		}
	}

	public void initRow(final Row row, final PeminjamanSuratItemDetail peminjamanSuratItemDetail) throws Exception {

		row.setValign("top");
		row.setAttribute("peminjamanSuratItemDetail", peminjamanSuratItemDetail);

		Vbox vbox = new Vbox();
		vbox.setParent(row);

		new Label(peminjamanSuratItemDetail.getSuratMasuk() == null ? ""
				: peminjamanSuratItemDetail.getSuratMasuk().getNoSurat()).setParent(vbox);

		new Label(peminjamanSuratItemDetail.getSuratMasuk() == null ? ""
				: peminjamanSuratItemDetail.getSuratMasuk().getKode()).setParent(vbox);

		try {
			RevisiHelper.createNewRevisi(PeminjamanSuratItemDetail.class, peminjamanSuratItemDetail,
					peminjamanSuratItemDetail.getSuratMasuk() == null ? ""
							: peminjamanSuratItemDetail.getSuratMasuk().getPerihal())
					.setParent(row);
		} catch (Exception e) {
			new Label(peminjamanSuratItemDetail.getSuratMasuk() == null ? ""
					: peminjamanSuratItemDetail.getSuratMasuk().getPerihal()).setParent(row);
		}

		if (persetujuan) {
			new Label(peminjamanSuratItemDetail.getKeterangan()).setParent(row);
		} else {

			final MyTextbox keterangan = new MyTextbox(
					peminjamanSuratItemDetail.getKeterangan() == null ? "" : peminjamanSuratItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(
					peminjamanSuratItemDetail.getPeminjamanSuratItem().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					peminjamanSuratItemDetail.setKeterangan(keterangan.getValue());
					row.setValign("top");
					row.setAttribute("peminjamanSuratItemDetail", peminjamanSuratItemDetail);
					if (peminjamanSuratItemDetail.getId() != null) {
						Session session = HibernateUtil.currentSession();
						session.refresh(peminjamanSuratItemDetail);
						session.update(peminjamanSuratItemDetail);
					}
				}
			});
		}

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Lihat " + tipe, "/img/eye-icon.png");
		button.setDisabled(peminjamanSuratItemDetail.getPeminjamanSuratItem().getDisetujuiOleh() == null
				|| peminjamanSuratItemDetail.getPeminjamanSuratItem().getMulai().after(WaktuUtil.getDate())
				|| peminjamanSuratItemDetail.getPeminjamanSuratItem().getSampai().before(WaktuUtil.getDate()));
		button.setAttribute("janganDisabled", true);
		button.setOrient("vertical");
		button.setTooltiptext("Lihat Data");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				final MyWindow window = new MyWindow("Tampilan Surat", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("95%");
				window.setWidth("95%");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Session session = StreamingHibernateUtil.getInstance().currentSession();
				List<FotoGambarSuratMasuk> fotoGambarSuratMasuks = peminjamanSuratItemDetail.getSuratMasuk() == null
						|| peminjamanSuratItemDetail.getSuratMasuk().getId() == null
								? new ArrayList<FotoGambarSuratMasuk>()
								: session.createCriteria(FotoGambarSuratMasuk.class)
										.add(Restrictions.eq("suratMasuk",
												peminjamanSuratItemDetail.getSuratMasuk().getId()))
										.addOrder(Order.desc("id")).list();

				Rows rows = new Rows();
				rows.setParent(grid);

				for (FotoGambarSuratMasuk fotoGambarSuratMasuk : fotoGambarSuratMasuks) {
					Row row = new Row();
					row.setValign("top");
					row.setParent(rows);
					CommonMedia.preview(fotoGambarSuratMasuk, row);
				}
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
				StreamingHibernateUtil.getInstance().closeSession();

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				window.onModal();

			}
		});
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete && peminjamanSuratItemDetail.getPeminjamanSuratItem().getDisetujuiOleh() == null);
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
									if (peminjamanSuratItemDetail.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(peminjamanSuratItemDetail);
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
