package ais.action.master.sirkulasisurat.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoGambarSuratMasuk;
import ais.database.model.sirkulasisurat.KembaliSuratItem;
import ais.database.model.sirkulasisurat.KembaliSuratItemDetail;
import ais.database.model.sirkulasisurat.PeminjamanSuratItemDetail;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class KembaliSuratItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private KembaliSuratItem kembaliSuratItem;
	private MyGrid grid;
	private boolean edit = false;

	// private boolean delete = false;

	public KembaliSuratItemDetailAction(KembaliSuratItem kembaliSuratItem) {
		super();
		this.kembaliSuratItem = kembaliSuratItem;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(KembaliSuratItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class KembaliSuratItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public KembaliSuratItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KembaliSuratItemDetail kembaliSuratItemDetail = (KembaliSuratItemDetail) data;

			new Label(kembaliSuratItemDetail.getSuratMasuk() == null ? ""
					: kembaliSuratItemDetail.getSuratMasuk().getNoSurat()).setParent(row);

			new Label(kembaliSuratItemDetail.getSuratMasuk() == null ? ""
					: kembaliSuratItemDetail.getSuratMasuk().getKode()).setParent(row);

			RevisiHelper.createNewRevisi(KembaliSuratItemDetail.class, kembaliSuratItemDetail,
					kembaliSuratItemDetail.getSuratMasuk().getPerihal()).setParent(row);

			final Label perpanjang = new Label(
					kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getJumlahPerpanjangan() + " kali");
			perpanjang.setParent(row);

			new Label(Common.dateFormat4.get()
					.format(kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getBatasWaktupengembalian()) + "  \n"
					+ kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getJumlahHariBatas() + " hari")
					.setParent(row);

			final Label terlambat = new Label(
					kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getJumlahHariTerlambat() + " hari");
			terlambat.setParent(row);

			final ais.ui.util.MyDatebox tanggal = new ais.ui.util.MyDatebox(kembaliSuratItemDetail.getTanggal());
			tanggal.setFormat(Common.dateFormat.get().toPattern());

			tanggal.setDisabled(kembaliSuratItemDetail.getKembaliSuratItem().getDisetujuiOleh() != null || !edit);
			tanggal.setWidth("90%");
			tanggal.setReadonly(true);
			tanggal.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					kembaliSuratItemDetail.getKeterangan() == null ? "" : kembaliSuratItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setRows(3);
			keterangan.setParent(row);
			keterangan.setDisabled(kembaliSuratItemDetail.getKembaliSuratItem().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					kembaliSuratItemDetail.setKeterangan(keterangan.getValue());
					session.update(kembaliSuratItemDetail);
				}
			});

			final Label textDenda = new Label(Common.numberFormat.get().format(kembaliSuratItemDetail.getDenda()));
			textDenda.setParent(row);

			final EventListener tanggalEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (tanggal.getValue() == null) {
						tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
					}

					kembaliSuratItemDetail.setTanggal(tanggal.getValue());

					// int jumlahSelisihHari =
					// Common.getWorkingDaysBetweenTwoDates(kembaliSuratItemDetail
					// .getKembaliSuratItem().getPeminjamanSuratItem().getTanggalPembuatan(),
					// kembaliSuratItemDetail.getTanggal());
					//
					// System.out.println("jumlahSelisihHari = " +
					// jumlahSelisihHari);

					PeminjamanSuratItemDetail peminjamanSuratItemDetail = kembaliSuratItemDetail
							.getPeminjamanSuratItemDetail();

					peminjamanSuratItemDetail.setTanggalKembali(tanggal.getValue());

					peminjamanSuratItemDetail.setKembaliSuratItemDetail(kembaliSuratItemDetail);

					perpanjang.setValue(peminjamanSuratItemDetail.getJumlahPerpanjangan() + " kali");

					terlambat.setValue(peminjamanSuratItemDetail.getJumlahHariTerlambat() + " hari");

					kembaliSuratItemDetail.setPeminjamanSuratItemDetail(peminjamanSuratItemDetail);

					row.setValign("top");
					row.setAttribute("kembaliSuratItemDetail", kembaliSuratItemDetail);
				}
			};

			tanggalEventListener.onEvent(null);
			tanggal.addEventListener("onChange", tanggalEventListener);

			Vbox toolbar = new Vbox();
			toolbar.setParent(row);

			if (kembaliSuratItemDetail.getPeminjamanSuratItemDetail() != null
					&& kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getPeminjamanSuratItem() != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Lihat "
						+ kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getPeminjamanSuratItem().getTipe(),
						"/img/eye-icon.png");
				button.setDisabled(kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getPeminjamanSuratItem()
						.getDisetujuiOleh() == null
						|| kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getPeminjamanSuratItem().getMulai()
								.after(WaktuUtil.getDate())
						|| kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getPeminjamanSuratItem().getSampai()
								.before(WaktuUtil.getDate()));
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
						List<FotoGambarSuratMasuk> fotoGambarSuratMasuks = kembaliSuratItemDetail
								.getPeminjamanSuratItemDetail().getSuratMasuk() == null
								|| kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getSuratMasuk().getId() == null
										? new ArrayList<FotoGambarSuratMasuk>()
										: session.createCriteria(FotoGambarSuratMasuk.class)
												.add(Restrictions.eq("suratMasuk",
														kembaliSuratItemDetail.getPeminjamanSuratItemDetail()
																.getSuratMasuk().getId()))
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
				button.setParent(toolbar);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<KembaliSuratItemDetail> kembaliSuratItemDetails = session.createCriteria(KembaliSuratItemDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("kembaliSuratItem", kembaliSuratItem)).list();

		ListModel strset = new SimpleListModel(kembaliSuratItemDetails);
		grid.setRowRenderer(new KembaliSuratItemDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item Kembali");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No Surat");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No Agenda");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Perihal");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Perpanjang");
		column.setWidth("0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Batas kembali");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Terlambat");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal Kembali");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Denda");
		column.setWidth("0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("0px");

		loadData(null);
	}

}
