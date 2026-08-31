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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
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
import ais.action.master.sirs.helper.AmbilDataItemMedisBanyakBerdasarkanStok;
import ais.common.Common;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.BahanBakuItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Produksi;
import ais.database.model.sirs.ProduksiDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk produksi detail. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Produksi produksi}, {@code Grid grid},
 * {@code Footer totalJumlah}, {@code Footer totalHarga}; pembacaan/pencarian ({@code loadData()}, {@code
 * loadTotal()}); operasi domain lain ({@code display()}, {@code masukkanBiaya()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class ProduksiDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Produksi produksi;
	private Grid grid;

	private Footer totalJumlah;
	private Footer totalHarga;

	public ProduksiDetailAction(Produksi produksi) {
		super();
		this.produksi = produksi;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(ProduksiDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class ProduksiDetailRenderer extends ais.ui.util.MyRowRenderer {

		public ProduksiDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final ProduksiDetail produksiDetail = (ProduksiDetail) data;

			new Label(produksiDetail.getItem() == null ? "" : produksiDetail.getItem().getKode()).setParent(row);

			RevisiHelper.createNewRevisi(ProduksiDetail.class, produksiDetail,
					produksiDetail.getItem() == null ? "" : produksiDetail.getItem().getNama()).setParent(row);

			Double myHargaBeli = produksiDetail.getHargaBeli() == null ? 0.0 : produksiDetail.getHargaBeli();

			Double myJumlah = produksiDetail.getJumlah() == null ? 0.0 : produksiDetail.getJumlah();

			Double total = myHargaBeli * myJumlah;

			final Label hargaBeliLabel;
			hargaBeliLabel = new Label(Common.numberFormat.get().format(myHargaBeli));

			final Label totalLabel;
			totalLabel = new Label(Common.numberFormat.get().format(total));

			final MyDoublebox jumlah;
			(jumlah = new MyDoublebox(myJumlah)).setParent(row);

			final EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();

					ItemMedis item = produksiDetail.getItem();
					Double myHargaBeli = CommonSirs.hitungHPP(item, session);

					Double myJumlah = jumlah.getValue() == null ? 0.0 : jumlah.getValue();

					Double total = myHargaBeli * myJumlah;

					produksiDetail.setJumlah(myJumlah);
					produksiDetail.setHargaBeli(myHargaBeli);
					Common.refreshUpdate(session, (produksiDetail));

					hargaBeliLabel.setValue(Common.numberFormat.get().format(myHargaBeli));
					totalLabel.setValue(Common.numberFormat.get().format(total));

					BahanBakuItem bahanBakuItem = (BahanBakuItem) session.createCriteria(BahanBakuItem.class)
							.add(Restrictions.eq("item", item)).add(Restrictions.eq("itemInduk", produksi.getItem()))
							.setMaxResults(1).uniqueResult();

					if (bahanBakuItem == null) {
						bahanBakuItem = new BahanBakuItem();
						bahanBakuItem.setItem(item);
						bahanBakuItem.setItemInduk(produksi.getItem());
					}
					bahanBakuItem.setQty(myJumlah);
					bahanBakuItem.setKeterangan("Bahan baku untuk produksi "
							+ (produksi.getItem() == null ? "" : produksi.getItem().getNama()));
					Common.refreshUpdate(session, (bahanBakuItem));

					masukkanBiaya();
					loadTotal();
				}
			};

			jumlah.setDisabled(produksiDetail.getProduksi().getDisetujuiOleh() != null);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, eventListener);

			new Label(produksiDetail.getItem() == null || produksiDetail.getItem().getSatuanItem() == null ? ""
					: produksiDetail.getItem().getSatuanItem().getNama()).setParent(row);

			hargaBeliLabel.setParent(row);
			totalLabel.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					produksiDetail.getKeterangan() == null ? "" : produksiDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(produksiDetail.getProduksi().getDisetujuiOleh() != null);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					produksiDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (produksiDetail));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setDisabled(produksiDetail.getProduksi().getDisetujuiOleh() != null);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui, data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();
											List<BahanBakuItem> bahanBakuItems = ConstantValues.simpleList(
													session.createCriteria(BahanBakuItem.class)
															.add(Restrictions.eq("item", produksiDetail.getItem()))
															.add(Restrictions.eq("itemInduk", produksi.getItem())),
													BahanBakuItem.class);

											for (BahanBakuItem bahanBakuItem : bahanBakuItems) {
												Common.refreshDelete(session, bahanBakuItem);
											}

											Common.refreshDelete(session, produksiDetail);

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/ProduksiDetailAction.java:205");
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
		List<ProduksiDetail> produksiDetails = ConstantValues.simpleList(session.createCriteria(ProduksiDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("produksi", produksi)), ProduksiDetail.class);

		ListModel strset = new SimpleListModel(produksiDetails);
		grid.setRowRenderer(new ProduksiDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();
		loadTotal();
	}

	@SuppressWarnings("unchecked")
	private void loadTotal() {
		Session session = HibernateUtil.currentSession();
		List<ProduksiDetail> produksiDetails = ConstantValues.simpleList(session.createCriteria(ProduksiDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("produksi", produksi)), ProduksiDetail.class);
		Double myJumlah = 0.0;
		Double myHarga = 0.0;
		for (ProduksiDetail produksiDetail : produksiDetails) {
			myJumlah += (produksiDetail.getJumlah() == null ? 0.0 : produksiDetail.getJumlah());
			myHarga += (produksiDetail.getHargaBeli() == null ? 0.0 : produksiDetail.getHargaBeli())
					* (produksiDetail.getJumlah() == null ? 0.0 : produksiDetail.getJumlah());
		}

		totalJumlah.setLabel(Common.numberFormat.get().format(myJumlah));
		totalHarga.setLabel(Common.numberFormat.get().format(myHarga));
	}

	public void display() {
		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Bahan Baku");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(panel);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Bahan Baku", "/img/add_item.png");
		button.setDisabled(produksi.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(session.createCriteria(ProduksiDetail.class)
						.setProjection(Projections.groupProperty("item.id")).add(Restrictions.eq("produksi", produksi)),
						ItemMedis.class, false);

				AmbilDataItemMedisBanyak ambilDataItemBanyak = new AmbilDataItemMedisBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {
							Double myHargaBeli = CommonSirs.hitungHPP(item, session);
							ProduksiDetail produksiDetail = new ProduksiDetail();
							produksiDetail.setHargaBeli(myHargaBeli);
							produksiDetail.setItem(item);
							produksiDetail.setJumlah(0.0);
							produksiDetail.setKeterangan("");
							produksiDetail.setProduksi(produksi);
							session.save(produksiDetail);
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

		button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Bahan Baku Berdasarkan Stok", "/img/add_item.png");
		button.setDisabled(produksi.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(session.createCriteria(ProduksiDetail.class)
						.setProjection(Projections.groupProperty("item.id")).add(Restrictions.eq("produksi", produksi)),
						ItemMedis.class, false);

				AmbilDataItemMedisBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemMedisBanyakBerdasarkanStok(
						items, produksi.getLokasi(), true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {
							Double myHargaBeli = CommonSirs.hitungHPP(item, session);
							ProduksiDetail produksiDetail = new ProduksiDetail();
							produksiDetail.setHargaBeli(myHargaBeli);
							produksiDetail.setItem(item);
							produksiDetail.setJumlah(0.0);
							produksiDetail.setKeterangan("");
							produksiDetail.setProduksi(produksi);
							session.save(produksiDetail);
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
		column.setLabel("Kode Item");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Item");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Harga Beli");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Total");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		Foot foot = new Foot();
		foot.setParent(grid);

		foot.appendChild(new Footer());
		foot.appendChild(new Footer("Total Qty"));

		totalJumlah = new Footer();
		totalJumlah.setParent(foot);

		foot.appendChild(new Footer());
		foot.appendChild(new Footer());
		// foot.appendChild(new Footer());

		totalHarga = new Footer();
		totalHarga.setParent(foot);

		loadData(null);
	}

	@SuppressWarnings("unchecked")
	public void masukkanBiaya() {
		if (produksi == null || produksi.getId() == null) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		List<ProduksiDetail> produksiDetails = ConstantValues.simpleList(session.createCriteria(ProduksiDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("produksi", produksi)), ProduksiDetail.class);
		Double biaya = 0.0;
		for (ProduksiDetail produksiDetail : produksiDetails) {
			biaya += ((produksiDetail.getHargaBeli() == null ? 0.0 : produksiDetail.getHargaBeli())
					* (produksiDetail.getJumlah() == null ? 0.0 : produksiDetail.getJumlah()));
		}
		produksi.setBiayaSatuan(biaya);
		produksi.setBiaya(
				(biaya == null ? 0.0 : biaya.doubleValue()) * (produksi.getQty() == null ? 0.0 : produksi.getQty()));
		Common.refreshUpdate(session, produksi);
	}

}
