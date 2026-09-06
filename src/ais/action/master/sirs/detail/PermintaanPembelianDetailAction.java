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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Penyedia;
import ais.database.model.sirs.HargaBeliItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.PermintaanPembelian;
import ais.database.model.sirs.PermintaanPembelianDetail;
import ais.database.model.sirs.SatuanItem;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk permintaan pembelian detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PermintaanPembelian
 * permintaanPembelian}, {@code Grid grid}, {@code Footer totalJumlah}, {@code Footer totalHarga};
 * pembacaan/pencarian ({@code loadData()}, {@code loadTotal()}); operasi domain lain ({@code display()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PermintaanPembelianDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PermintaanPembelian permintaanPembelian;
	private Grid grid;

	private Footer totalJumlah;
	private Footer totalHarga;

	public PermintaanPembelianDetailAction(PermintaanPembelian permintaanPembelian) {
		super();
		this.permintaanPembelian = permintaanPembelian;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PermintaanPembelianDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PermintaanPembelianDetailAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PermintaanPembelianDetailAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code List vendors}; operasi lokal: {@code
	 * render}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PermintaanPembelianDetailAction
	 */
	class PermintaanPembelianDetailRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		private List<Penyedia> vendors = ConstantValues
				.simpleList(HibernateUtil.currentSession().createCriteria(Penyedia.class), Penyedia.class);

		public PermintaanPembelianDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PermintaanPembelianDetail permintaanPembelianDetail = (PermintaanPembelianDetail) data;

			new Label(permintaanPembelianDetail.getItem() == null ? "" : permintaanPembelianDetail.getItem().getKode())
					.setParent(row);

			if (permintaanPembelianDetail.getItem().getDefaultPenyedia() != null
					&& (permintaanPembelianDetail.getHargaBeli() == null
							|| permintaanPembelianDetail.getHargaBeli() < 1.0)) {
				Session session = HibernateUtil.currentSession();
				Double myHargaBeli = (Double) session.createCriteria(HargaBeliItem.class)
						.add(Restrictions.eq("item", permintaanPembelianDetail.getItem()))
						.add(Restrictions.eq("vendor", permintaanPembelianDetail.getItem().getDefaultPenyedia()))
						.setProjection(Projections.property("hargaBeli")).setMaxResults(1).uniqueResult();

				permintaanPembelianDetail
						.setJumlah(permintaanPembelianDetail.getItem().getDefaultPermintaan() == null ? 0.0
								: permintaanPembelianDetail.getItem().getDefaultPermintaan());
				permintaanPembelianDetail.setHargaBeli(myHargaBeli == null ? 0.0 : myHargaBeli);
				permintaanPembelianDetail.setPenyedia(permintaanPembelianDetail.getItem().getDefaultPenyedia());
				Common.refreshUpdate(permintaanPembelianDetail);

			}

			RevisiHelper.createNewRevisi(PermintaanPembelianDetail.class, permintaanPembelianDetail,
					permintaanPembelianDetail.getItem() == null ? "" : permintaanPembelianDetail.getItem().getNama())
					.setParent(row);

			Double myHargaBeli = permintaanPembelianDetail.getHargaBeli() == null ? 0.0
					: permintaanPembelianDetail.getHargaBeli();

			Double myJumlah = permintaanPembelianDetail.getJumlah() == null ? 0.0
					: permintaanPembelianDetail.getJumlah();

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
					Penyedia vendor = permintaanPembelianDetail.getPenyedia();
					Double myHargaBeli = (Double) (vendor == null ? 0.0
							: session.createCriteria(HargaBeliItem.class)
									.add(Restrictions.eq("item", permintaanPembelianDetail.getItem()))
									.add(Restrictions.eq("vendor", vendor))
									.setProjection(Projections.property("hargaBeli")).setMaxResults(1).uniqueResult());

					myHargaBeli = (myHargaBeli == null ? 0.0 : myHargaBeli);

					Double myJumlah = jumlah.getValue() == null ? 0.0 : jumlah.getValue();

					ItemMedis item = permintaanPembelianDetail.getItem();
					item.setDefaultPermintaan(myJumlah);

					Double total = myHargaBeli * myJumlah;

					permintaanPembelianDetail.setJumlah(myJumlah);
					permintaanPembelianDetail.setHargaBeli(myHargaBeli);
					Common.refreshUpdate(session, (permintaanPembelianDetail));
					Common.refreshUpdate(session, (item));

					hargaBeliLabel.setValue(Common.numberFormat.get().format(myHargaBeli));
					totalLabel.setValue(Common.numberFormat.get().format(total));

					loadTotal();
				}
			};

			jumlah.setDisabled(permintaanPembelianDetail.getPermintaanPembelian().getDisetujuiOleh() != null);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, eventListener);

			// new Label(permintaanPembelianDetail.getSatuanItem() == null ? ""
			// : permintaanPembelianDetail.getSatuanItem().getNama())
			// .setParent(row);

			if (permintaanPembelianDetail.getPermintaanPembelian().getDisetujuiOleh() != null) {
				new Label(permintaanPembelianDetail.getSatuanItem() == null ? ""
						: permintaanPembelianDetail.getSatuanItem().getNama()).setParent(row);
			} else {
				final Combobox satuan = new Combobox();
				satuan.setParent(row);
				satuan.setWidth("90%");
				Common.insertCombo(satuan, "nama", SatuanItem.class);
				Common.selectComboItem(satuan, permintaanPembelianDetail.getSatuanItem());
				satuan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						SatuanItem satuanItem = (SatuanItem) (satuan.getSelectedItem() == null ? null
								: satuan.getSelectedItem().getValue());
						ItemMedis item = permintaanPembelianDetail.getItem();
						if (item != null && satuanItem != null) {
							permintaanPembelianDetail.setSatuanItem(satuanItem);
							item.setSatuanItem(satuanItem);

							Session session = HibernateUtil.currentSession();
							Common.refreshUpdate(session, (permintaanPembelianDetail));
							Common.refreshUpdate(session, (item));
						}
					}
				});
			}

			final Combobox supplier = new Combobox();
			supplier.setDisabled(permintaanPembelianDetail.getPermintaanPembelian().getDisetujuiOleh() != null);
			Session session = HibernateUtil.currentSession();
			for (Penyedia vendor : vendors) {
				Comboitem comboitem = new Comboitem(vendor.getNama());
				comboitem.setValue(vendor);

				Double hargaBeli = (Double) session.createCriteria(HargaBeliItem.class)
						.add(Restrictions.eq("item", permintaanPembelianDetail.getItem()))
						.add(Restrictions.eq("vendor", vendor)).setProjection(Projections.property("hargaBeli"))
						.setMaxResults(1).uniqueResult();
				comboitem.setDescription("Harga beli "
						+ (hargaBeli == null ? "belum ditentukan" : ": " + Common.numberFormat.get().format(hargaBeli)));
				supplier.appendChild(comboitem);
			}

			Common.selectComboItem(supplier, permintaanPembelianDetail.getPenyedia());
			supplier.setWidth("90%");
			supplier.setParent(row);
			supplier.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Penyedia vendor = (Penyedia) (supplier.getSelectedItem() == null ? null
							: supplier.getSelectedItem().getValue());
					ItemMedis item = permintaanPembelianDetail.getItem();
					item.setDefaultPenyedia(vendor);
					Session session = HibernateUtil.currentSession();
					permintaanPembelianDetail.setPenyedia(vendor);
					Common.refreshUpdate(session, (permintaanPembelianDetail));
					Common.refreshUpdate(session, (item));
					eventListener.onEvent(arg0);
				}
			});

			hargaBeliLabel.setParent(row);
			totalLabel.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					permintaanPembelianDetail.getKeterangan() == null ? "" : permintaanPembelianDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(permintaanPembelianDetail.getPermintaanPembelian().getDisetujuiOleh() != null);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					permintaanPembelianDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (permintaanPembelianDetail));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setDisabled(permintaanPembelianDetail.getPermintaanPembelian().getDisetujuiOleh() != null);
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
											Common.refreshDelete(permintaanPembelianDetail);

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/PermintaanPembelianDetailAction.java:280");
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
			ais.ui.util.MenuAksiBaris.pasangSelalu(toolbar);
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<PermintaanPembelianDetail> permintaanPembelianDetails = session
				.createCriteria(PermintaanPembelianDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("permintaanPembelian", permintaanPembelian)).list();

		ListModel strset = new SimpleListModel(permintaanPembelianDetails);
		grid.setRowRenderer(new PermintaanPembelianDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();
		loadTotal();
	}

	@SuppressWarnings("unchecked")
	private void loadTotal() {
		Session session = HibernateUtil.currentSession();
		List<PermintaanPembelianDetail> permintaanPembelianDetails = session
				.createCriteria(PermintaanPembelianDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("permintaanPembelian", permintaanPembelian)).list();
		Double myJumlah = 0.0;
		Double myHarga = 0.0;
		for (PermintaanPembelianDetail permintaanPembelianDetail : permintaanPembelianDetails) {
			myJumlah += (permintaanPembelianDetail.getJumlah() == null ? 0.0 : permintaanPembelianDetail.getJumlah());
			myHarga += (permintaanPembelianDetail.getHargaBeli() == null ? 0.0
					: permintaanPembelianDetail.getHargaBeli())
					* (permintaanPembelianDetail.getJumlah() == null ? 0.0 : permintaanPembelianDetail.getJumlah());
		}

		totalJumlah.setLabel(Common.numberFormat.get().format(myJumlah));
		totalHarga.setLabel(Common.numberFormat.get().format(myHarga));
	}

	public void display() {
		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item Permintaan Pembelian");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(panel);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Item", "/img/add_item.png");
		button.setDisabled(permintaanPembelian.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(
						session.createCriteria(PermintaanPembelianDetail.class)
								.setProjection(Projections.groupProperty("item.id"))
								.add(Restrictions.eq("permintaanPembelian", permintaanPembelian)),
						ItemMedis.class, false);

				AmbilDataItemMedisBanyak ambilDataItemBanyak = new AmbilDataItemMedisBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {
							PermintaanPembelianDetail permintaanPembelianDetail = new PermintaanPembelianDetail();
							permintaanPembelianDetail.setItem(item);
							permintaanPembelianDetail.setJumlah(0.0);
							permintaanPembelianDetail.setKeterangan("");
							permintaanPembelianDetail.setPermintaanPembelian(permintaanPembelian);
							permintaanPembelianDetail.setSatuanItem(item.getSatuanItem());
							permintaanPembelianDetail.setPenyedia(item.getDefaultPenyedia());
							session.save(permintaanPembelianDetail);
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

		button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Item Berdasarkan Stok", "/img/add_item.png");
		button.setDisabled(permintaanPembelian.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(
						session.createCriteria(PermintaanPembelianDetail.class)
								.setProjection(Projections.groupProperty("item.id"))
								.add(Restrictions.eq("permintaanPembelian", permintaanPembelian)),
						ItemMedis.class, false);

				AmbilDataItemMedisBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemMedisBanyakBerdasarkanStok(
						items, permintaanPembelian.getLokasi(), false);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {
							PermintaanPembelianDetail permintaanPembelianDetail = new PermintaanPembelianDetail();
							permintaanPembelianDetail.setItem(item);
							permintaanPembelianDetail.setJumlah(0.0);
							permintaanPembelianDetail.setKeterangan("");
							permintaanPembelianDetail.setPermintaanPembelian(permintaanPembelian);
							permintaanPembelianDetail.setSatuanItem(item.getSatuanItem());
							permintaanPembelianDetail.setPenyedia(item.getDefaultPenyedia());
							session.save(permintaanPembelianDetail);
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
		column.setLabel("Supplier");
		column.setWidth("20%");

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
		foot.appendChild(new Footer());

		totalHarga = new Footer();
		totalHarga.setParent(foot);

		loadData(null);
	}

}
