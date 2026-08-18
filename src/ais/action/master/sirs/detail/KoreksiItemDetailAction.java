package ais.action.master.sirs.detail;

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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataItemMedisBanyak;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.KodeTransaksiMedis;
import ais.database.model.sirs.KoreksiItemMedis;
import ais.database.model.sirs.KoreksiItemMedisDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class KoreksiItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private KoreksiItemMedis koreksiItem;
	private Grid grid;

	private List<KodeTransaksiMedis> kodeTransaksis = new ArrayList<KodeTransaksiMedis>();

	public KoreksiItemDetailAction(KoreksiItemMedis koreksiItem) {
		super();
		this.koreksiItem = koreksiItem;
		kodeTransaksis.add(ConstantValues.adjustmentPenambahan);
		kodeTransaksis.add(ConstantValues.adjustmentPengurangan);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(KoreksiItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class KoreksiItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public KoreksiItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KoreksiItemMedisDetail koreksiItemDetail = (KoreksiItemMedisDetail) data;

			new Label(koreksiItemDetail.getItem() == null ? "" : koreksiItemDetail.getItem().getKode()).setParent(row);

			RevisiHelper
					.createNewRevisi(KoreksiItemMedisDetail.class, koreksiItemDetail,
							koreksiItemDetail.getItem() == null ? "" : koreksiItemDetail.getItem().getNama())
					.setParent(row);

			final Label stok = new Label(Common.numberFormat.get()
					.format((koreksiItemDetail.getStok() == null ? 0.0 : koreksiItemDetail.getStok())));

			final Label stokMenjadi = new Label(Common.numberFormat.get()
					.format((koreksiItemDetail.getStokmenjadi() == null ? 0.0 : koreksiItemDetail.getStokmenjadi())));

			final Label total = new Label(Common.numberFormat.get()
					.format((koreksiItemDetail.getJumlah() == null ? 0.0 : koreksiItemDetail.getJumlah())
							* (koreksiItemDetail.getHarga() == null ? 0.0 : koreksiItemDetail.getHarga())));

			final MyDoublebox jumlah;
			jumlah = new MyDoublebox(koreksiItemDetail.getJumlah() == null ? 0.0 : koreksiItemDetail.getJumlah());

			final Combobox ajdusment = new Combobox();
			Common.insertComboItems(ajdusment, "nama", kodeTransaksis);
			Common.selectComboItem(ajdusment, koreksiItemDetail.getKodeTransaksi());

			final EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					KodeTransaksiMedis kodeTransaksi = (KodeTransaksiMedis) (ajdusment.getSelectedItem() == null ? null
							: ajdusment.getSelectedItem().getValue());
					if (kodeTransaksi == null) {
						MyMessageboxConfig.show("Mohon maaf, Bapak/Ibu wajib memilih salah satu jenis koreksi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih salah satu jenis koreksi pada kolom yang tersedia; (2) kemudian lanjutkan kembali proses Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						jumlah.setValue(0.0);
						return;
					}
					if (jumlah.getValue() == null) {
						jumlah.setValue(0.0);
					}
					Session session = HibernateUtil.currentSession();
					koreksiItemDetail.setKodeTransaksi(kodeTransaksi);
					koreksiItemDetail.setJumlah(Math.abs(jumlah.getValue()) * kodeTransaksi.getJenis().doubleValue());
					jumlah.setValue(koreksiItemDetail.getJumlah());

					Double menjadi = koreksiItemDetail.getJumlah() + koreksiItemDetail.getStok();
					stokMenjadi.setValue(Common.numberFormat.get().format(menjadi));
					koreksiItemDetail.setStokmenjadi(menjadi);

					Common.refreshUpdate(session, (koreksiItemDetail));
					total.setValue(Common.numberFormat.get()
							.format((koreksiItemDetail.getJumlah() == null ? 0.0 : koreksiItemDetail.getJumlah())
									* (koreksiItemDetail.getHarga() == null ? 0.0 : koreksiItemDetail.getHarga())));

				}
			};

			ajdusment.setWidth("90%");
			ajdusment.setDisabled(koreksiItemDetail.getKoreksiItem().getDisetujuiOleh() != null);
			ajdusment.setParent(row);
			ajdusment.addEventListener("onChange", eventListener);

			jumlah.setParent(row);
			jumlah.setDisabled(koreksiItemDetail.getKoreksiItem().getDisetujuiOleh() != null);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, eventListener);

			stok.setParent(row);
			stokMenjadi.setParent(row);

			new Label(koreksiItemDetail.getItem() == null || koreksiItemDetail.getItem().getSatuanItem() == null ? ""
					: koreksiItemDetail.getItem().getSatuanItem().getNama()).setParent(row);

			(new Label(Common.numberFormat.get()
					.format(koreksiItemDetail.getHarga() == null ? 0.0 : koreksiItemDetail.getHarga()))).setParent(row);

			total.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					koreksiItemDetail.getKeterangan() == null ? "" : koreksiItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(koreksiItemDetail.getKoreksiItem().getDisetujuiOleh() != null);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					koreksiItemDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (koreksiItemDetail));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setDisabled(koreksiItemDetail.getKoreksiItem().getDisetujuiOleh() != null);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang sudah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(koreksiItemDetail);

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/KoreksiItemDetailAction.java:202");
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon hubungi administrator sistem. Rincian kesalahan: {V1}"
															, e.getMessage()));
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
		List<KoreksiItemMedisDetail> koreksiItemDetails = session.createCriteria(KoreksiItemMedisDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("koreksiItem", koreksiItem)).list();

		ListModel strset = new SimpleListModel(koreksiItemDetails);
		grid.setRowRenderer(new KoreksiItemDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	public void display() {
		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Koreksi Item");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(panel);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Data Item", "/img/add_item.png");
		button.setDisabled(koreksiItem.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(session.createCriteria(KoreksiItemMedisDetail.class)
						.setProjection(Projections.groupProperty("item.id"))
						.add(Restrictions.eq("koreksiItem", koreksiItem)), ItemMedis.class, false);

				AmbilDataItemMedisBanyak ambilDataItemBanyak = new AmbilDataItemMedisBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {

							String sql = "select sum((a.qty+a.qty_bonus)*b.jenis) as stok from sirs.detail_transaksi_pasien a inner join sirs.kode_transaksi_medis b on (a.kode_transaksi = b.id) where a.item = "
									+ item.getId() + " and a.lokasi = " + koreksiItem.getLokasi().getId() + ";";
							Number number = (Number) session.createSQLQuery(sql).uniqueResult();

							HargaJualItem hargaJualItem = (HargaJualItem) session.createCriteria(HargaJualItem.class)
									.add(Restrictions.eq("item", item))
									.add(Restrictions.eq("kelasPerawatan", ConstantValues.kelasNormal)).setMaxResults(1)
									.uniqueResult();

							KoreksiItemMedisDetail koreksiItemDetail = new KoreksiItemMedisDetail();
							koreksiItemDetail.setItem(item);
							koreksiItemDetail.setJumlah(0.0);
							koreksiItemDetail
									.setHarga(hargaJualItem == null || hargaJualItem.getHargaJual() == null ? 0.0
											: hargaJualItem.getHargaJual());
							koreksiItemDetail.setStok(number == null ? 0.0 : number.doubleValue());
							koreksiItemDetail.setStokmenjadi(number == null ? 0.0 : number.doubleValue());
							koreksiItemDetail.setKeterangan("");
							koreksiItemDetail.setKoreksiItem(koreksiItem);
							session.save(koreksiItemDetail);
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

		// AmbilDataItemBanyak

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis Koreksi");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Stok");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Menjadi");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Hrg. Jual");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nilai Jual");
		column.setAlign("right");
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
