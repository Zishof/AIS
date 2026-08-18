package ais.action.master.sirs.helper;

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
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.RacikanDetail;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class BuatRacikanBaruHelper extends Window {

	private MyTextbox kode;
	private MyTextbox keterangan;
	private Racikan racikan;

	private Lokasi myLokasi;

	private EventListener eventListener;
	/**
	 * 
	 */
	private static final long serialVersionUID = 3891561377476699477L;

	public BuatRacikanBaruHelper(EventListener eventListener) {
		super();
		this.eventListener = eventListener;
		myLokasi = Common.getCurrentLokasi();
		init(new Racikan());
	}

	public BuatRacikanBaruHelper(EventListener eventListener, String title, String border, boolean closable) {
		super(title, border, closable);
		this.eventListener = eventListener;
		myLokasi = Common.getCurrentLokasi();
		init(new Racikan());
	}

	@SuppressWarnings("deprecation")
	private void init(Racikan racikan) {
		this.racikan = racikan;
		setTitle("Buat Racikan Baru");
		Common.clear(this);
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Racikan")));
		row.appendChild(kode = new MyTextbox(
				racikan.getKode() == null ? Common.generateCode(Racikan.class, 8) : racikan.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(racikan.getKeterangan() == null ? "" : racikan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new RacikanDetailAction());

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				eventListener.onEvent(new Event("", null, null));
				detach();
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					eventListener.onEvent(new Event("", null, BuatRacikanBaruHelper.this.racikan));
					detach();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(this);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		Session session = HibernateUtil.currentSession();
		if (racikan.getId() != null) {
			racikan = (Racikan) session.load(Racikan.class, racikan.getId());

		}

		racikan.setKeterangan(keterangan.getValue());

		if (racikan.getId() != null) {
			Common.refreshUpdate(session, racikan);
		} else {
			racikan.setKode(Common.generateCode(Racikan.class, 8));
			session.save(racikan);
		}

		List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("racikan", racikan)).list();

		String nama = "";
		for (RacikanDetail racikanDetail : racikanDetails) {
			nama += racikanDetail.getItem() == null ? ""
					: (racikanDetail.getItem().getNama() + " => " + racikanDetail.getJumlah() + ", ");
		}
		racikan.setNama(nama);
		Common.refreshUpdate(session, racikan);

		return true;
	}

	public class RacikanDetailAction extends Div {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5086031585928643232L;

		private Grid grid;
		public List<RacikanDetail> racikanDetails = new ArrayList<RacikanDetail>();

		public RacikanDetailAction() {
			super();
			display();
		}

		class RacikanDetailRenderer extends ais.ui.util.MyRowRenderer {

			public RacikanDetailRenderer() {

			}

			@Override
			public void render(final Row row, Object data) throws Exception {row.setValign("top");
				final RacikanDetail racikanDetail = (RacikanDetail) data;

				new Label(racikanDetail.getItem() == null ? "" : racikanDetail.getItem().getKode()).setParent(row);

				RevisiHelper
						.createNewRevisi(RacikanDetail.class, racikanDetail,
								racikanDetail.getItem() == null ? "" : racikanDetail.getItem().getNama())
						.setParent(row);

				final MyDoublebox jumlah;
				(jumlah = new MyDoublebox(racikanDetail.getJumlah() == null ? 0.0 : racikanDetail.getJumlah()))
						.setParent(row);
				jumlah.setStyle("text-align:right");
				jumlah.setWidth("90%");
				jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						racikanDetail.setJumlah(jumlah.getValue());
						session.update(racikanDetail);
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
						session.update(racikanDetail);
					}
				});

				Hbox toolbar = new Hbox();

				Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data obat ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												Common.refreshDelete(racikanDetail);

												loadData(null);

											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/BuatRacikanBaruHelper.java:264");
												MyMessageboxConfig.show(Common.pesan(
														"Mohon maaf, data obat ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan data tidak sedang digunakan pada transaksi lain; (3) hubungi administrator apabila kendala masih berlanjut.",
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
			racikanDetails = racikan == null || racikan.getId() == null ? new ArrayList<RacikanDetail>()
					: session.createCriteria(RacikanDetail.class).addOrder(Order.desc("id"))
							.add(Restrictions.eq("racikan", racikan)).list();

			ListModel strset = new SimpleListModel(racikanDetails);
			grid.setRowRenderer(new RacikanDetailRenderer());
			grid.setModel(strset);
			grid.renderAll();
		}

		private void display() {

			this.setHeight("450px");

			Groupbox groupbox = new Groupbox();
			groupbox.setParent(this);
			groupbox.appendChild(new Caption("Daftar Obat"));

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("25px");
			toolbar.setParent(groupbox);
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Obat", "/img/add_item.png");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					if (!BuatRacikanBaruHelper.this.onSave(event)) {
						return;
					}

					Session session = HibernateUtil.currentSession();

					List<ItemMedis> items = session.createCriteria(RacikanDetail.class)
							.setProjection(Projections.groupProperty("item")).add(Restrictions.eq("racikan", racikan))
							.list();

					AmbilDataItemMedisBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemMedisBanyakBerdasarkanStok(
							items, myLokasi, "nama_item", true);
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
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("5%");

			loadData(null);
		}

	}

}
