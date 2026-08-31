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
import ais.database.model.sirs.SaldoAwalMedis;
import ais.database.model.sirs.SaldoAwalMedisDetail;
import ais.database.model.sirs.SatuanItem;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk saldo awal detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code SaldoAwalMedis saldoAwal}, {@code Grid
 * grid}; pembacaan/pencarian ({@code loadData()}); operasi domain lain ({@code display()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class SaldoAwalDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private SaldoAwalMedis saldoAwal;
	private Grid grid;

	public SaldoAwalDetailAction(SaldoAwalMedis saldoAwal) {
		super();
		this.saldoAwal = saldoAwal;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(SaldoAwalDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class SaldoAwalDetailRenderer extends ais.ui.util.MyRowRenderer {

		public SaldoAwalDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final SaldoAwalMedisDetail saldoAwalDetail = (SaldoAwalMedisDetail) data;

			new Label(saldoAwalDetail.getItem() == null ? "" : saldoAwalDetail.getItem().getKode()).setParent(row);

			RevisiHelper
					.createNewRevisi(SaldoAwalMedisDetail.class, saldoAwalDetail,
							saldoAwalDetail.getItem() == null ? "" : saldoAwalDetail.getItem().getNama())
					.setParent(row);

			final Label total = new Label(
					Common.numberFormat.get().format((saldoAwalDetail.getJumlah() == null ? 0.0 : saldoAwalDetail.getJumlah())
							* (saldoAwalDetail.getHarga() == null ? 0.0 : saldoAwalDetail.getHarga())));

			final MyDoublebox jumlah;
			(jumlah = new MyDoublebox(saldoAwalDetail.getJumlah() == null ? 0.0 : saldoAwalDetail.getJumlah()))
					.setParent(row);
			jumlah.setDisabled(saldoAwalDetail.getSaldoAwal().getDisetujuiOleh() != null);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					saldoAwalDetail.setJumlah(jumlah.getValue());
					Common.refreshUpdate(session, (saldoAwalDetail));
					total.setValue(Common.numberFormat.get()
							.format((saldoAwalDetail.getJumlah() == null ? 0.0 : saldoAwalDetail.getJumlah())
									* (saldoAwalDetail.getHarga() == null ? 0.0 : saldoAwalDetail.getHarga())));
				}
			});

			if (saldoAwalDetail.getSaldoAwal().getDisetujuiOleh() != null) {
				new Label(saldoAwalDetail.getSatuanItem() == null ? "" : saldoAwalDetail.getSatuanItem().getNama())
						.setParent(row);
			} else {
				final Combobox satuan = new Combobox();
				satuan.setParent(row);
				satuan.setWidth("90%");
				Common.insertCombo(satuan, "nama", SatuanItem.class);
				Common.selectComboItem(satuan, saldoAwalDetail.getSatuanItem());
				satuan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						SatuanItem satuanItem = (SatuanItem) (satuan.getSelectedItem() == null ? null
								: satuan.getSelectedItem().getValue());
						ItemMedis item = saldoAwalDetail.getItem();
						if (item != null && satuanItem != null) {
							saldoAwalDetail.setSatuanItem(satuanItem);
							item.setSatuanItem(satuanItem);

							Session session = HibernateUtil.currentSession();
							Common.refreshUpdate(session, (saldoAwalDetail));
							Common.refreshUpdate(session, (item));
						}
					}
				});
			}

			final MyDoublebox harga;
			(harga = new MyDoublebox(saldoAwalDetail.getHarga() == null ? 0.0 : saldoAwalDetail.getHarga()))
					.setParent(row);
			harga.setDisabled(saldoAwalDetail.getSaldoAwal().getDisetujuiOleh() != null);
			harga.setStyle("text-align:right");
			harga.setWidth("90%");
			harga.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					saldoAwalDetail.setHarga(harga.getValue());
					Common.refreshUpdate(session, (saldoAwalDetail));
					total.setValue(Common.numberFormat.get()
							.format((saldoAwalDetail.getJumlah() == null ? 0.0 : saldoAwalDetail.getJumlah())
									* (saldoAwalDetail.getHarga() == null ? 0.0 : saldoAwalDetail.getHarga())));
				}
			});

			total.setParent(row);

			final MyDatebox tanggalKadaluarsa = new MyDatebox(saldoAwalDetail.getTanggalKadaluarsa());
			tanggalKadaluarsa.setFormat(Common.dateFormat2.get().toPattern());
			tanggalKadaluarsa.setParent(row);
			tanggalKadaluarsa.setWidth("90%");
			tanggalKadaluarsa.setDisabled(saldoAwalDetail.getSaldoAwal().getDisetujuiOleh() != null);
			tanggalKadaluarsa.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					saldoAwalDetail.setTanggalKadaluarsa(tanggalKadaluarsa.getValue());
					Common.refreshUpdate(session, (saldoAwalDetail));

				}
			});

			final MyTextbox keterangan = new MyTextbox(
					saldoAwalDetail.getKeterangan() == null ? "" : saldoAwalDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(saldoAwalDetail.getSaldoAwal().getDisetujuiOleh() != null);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					saldoAwalDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (saldoAwalDetail));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setDisabled(saldoAwalDetail.getSaldoAwal().getDisetujuiOleh() != null);
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
											Common.refreshDelete(saldoAwalDetail);

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/SaldoAwalDetailAction.java:215");
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
		List<SaldoAwalMedisDetail> saldoAwalDetails = session.createCriteria(SaldoAwalMedisDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("saldoAwal", saldoAwal)).list();

		ListModel strset = new SimpleListModel(saldoAwalDetails);
		grid.setRowRenderer(new SaldoAwalDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	public void display() {
		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item Saldo Awal");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(panel);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Data Item", "/img/add_item.png");
		button.setDisabled(saldoAwal.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(session.createCriteria(SaldoAwalMedisDetail.class)
						.setProjection(Projections.groupProperty("item.id"))
						.add(Restrictions.eq("saldoAwal", saldoAwal)), ItemMedis.class, false);

				AmbilDataItemMedisBanyak ambilDataItemBanyak = new AmbilDataItemMedisBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {

							HargaJualItem hargaJualItem = (HargaJualItem) session.createCriteria(HargaJualItem.class)
									.add(Restrictions.eq("item", item))
									.add(Restrictions.eq("kelasPerawatan", ConstantValues.kelasNormal)).setMaxResults(1)
									.uniqueResult();

							SaldoAwalMedisDetail saldoAwalDetail = new SaldoAwalMedisDetail();
							saldoAwalDetail.setItem(item);
							saldoAwalDetail.setJumlah(0.0);
							saldoAwalDetail.setHarga(hargaJualItem == null ? 0.0 : hargaJualItem.getHargaJual());
							saldoAwalDetail.setKeterangan("");
							saldoAwalDetail.setSaldoAwal(saldoAwal);
							saldoAwalDetail.setSatuanItem(item.getSatuanItem());
							session.save(saldoAwalDetail);
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
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Item");
		column.setWidth("15%");

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
		column.setLabel("Harga");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Total");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Tgl Kadaluarsa");
		column.setWidth("15%");

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
