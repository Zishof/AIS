package ais.action.master.sirs.detail;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataItemMedisBanyak;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.RacikanDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class RacikanDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Racikan racikan;

	private Grid grid;

	private Toolbar toolbar;

	private Boolean aktif;

	public RacikanDetailAction(Racikan racikan, final Boolean aktif) {
		super();
		this.racikan = racikan;
		this.aktif = aktif;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(RacikanDetailAction.this);
				if (isOpen()) {
					display();
					toolbar.setVisible(aktif);
					Common.freeze(RacikanDetailAction.this, !aktif);
				}
			}
		});
	}

	public void reInitRacikan(Racikan racikan, final Boolean aktif) {
		this.racikan = racikan;
		this.aktif = aktif;
		Common.clear(RacikanDetailAction.this);
		display();
		toolbar.setVisible(aktif);
		Common.freeze(RacikanDetailAction.this, !aktif);

	}

	class RacikanDetailRenderer extends ais.ui.util.MyRowRenderer {

		public RacikanDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final RacikanDetail racikanDetail = (RacikanDetail) data;

			new Label(racikanDetail.getItem() == null ? "" : racikanDetail.getItem().getKode()).setParent(row);

			RevisiHelper.createNewRevisi(RacikanDetail.class, racikanDetail,
					racikanDetail.getItem() == null ? "" : racikanDetail.getItem().getNama()).setParent(row);

			final MyDoublebox jumlah;
			(jumlah = new MyDoublebox(racikanDetail.getJumlah() == null ? 0.0 : racikanDetail.getJumlah()))
					.setParent(row);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					racikanDetail.setJumlah(jumlah.getValue());
					session.update((racikanDetail));
				}
			});

			new Label(racikanDetail.getItem() == null || racikanDetail.getItem().getSatuanItem() == null ? ""
					: racikanDetail.getItem().getSatuanItem().getNama()).setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					racikanDetail.getKeterangan() == null ? "" : racikanDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					racikanDetail.setKeterangan(keterangan.getValue());
					session.update((racikanDetail));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui, data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(racikanDetail); 

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/RacikanDetailAction.java:152");
											MyMessageboxConfig.show(Common.pesan(
																"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih terkait dengan data ini; (2) hapus terlebih dahulu seluruh data yang berelasi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.",
																	e.getMessage()));
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("racikan", racikan)).list();

		ListModel strset = new SimpleListModel(racikanDetails);
		grid.setRowRenderer(new RacikanDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	private void display() {

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(this);
		groupbox.appendChild(new Caption("Daftar Item Racikan"));

		toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Item", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = session.createCriteria(RacikanDetail.class)
						.setProjection(Projections.groupProperty("item")).add(Restrictions.eq("racikan", racikan))
						.list();

				AmbilDataItemMedisBanyak ambilDataItemBanyak = new AmbilDataItemMedisBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {
							RacikanDetail racikanDetail = new RacikanDetail();
							racikanDetail.setItem(item);
							racikanDetail.setJumlah(0.0);
							racikanDetail.setKeterangan("");
							racikanDetail.setRacikan(racikan);
							session.save(racikanDetail);
							session.flush();
						}

						loadData(null);
					}
				});
				ambilDataItemBanyak.setWidth("95%");
				ambilDataItemBanyak.setHeight("97%");
				ambilDataItemBanyak.setVisible(true);
				ambilDataItemBanyak.onModal();
			}

		});
		button.setParent(toolbar);

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
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new Column();
		column.setVisible(aktif);
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

}
