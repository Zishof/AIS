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
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Penyedia;
import ais.database.model.sirs.HargaBeliItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.PermintaanPembelianDetail;
import ais.database.model.sirs.PesananPembelian;
import ais.database.model.sirs.PesananPembelianDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk pesanan pembelian detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PesananPembelian pesananPembelian},
 * {@code Grid grid}, {@code Footer totalJumlah}, {@code Footer totalHarga}; pembacaan/pencarian ({@code
 * loadData()}, {@code loadTotal()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PesananPembelianDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PesananPembelian pesananPembelian;

	private Grid grid;

	private Footer totalJumlah;
	private Footer totalHarga;

	public PesananPembelianDetailAction(PesananPembelian pesananPembelian) {
		super();
		this.pesananPembelian = pesananPembelian;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PesananPembelianDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PesananPembelianDetailAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PesananPembelianDetailAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Session session}; operasi lokal:
	 * {@code render}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PesananPembelianDetailAction
	 */
	class PesananPembelianDetailRenderer extends ais.ui.util.MyRowRenderer {

		private Session session = HibernateUtil.currentSession();

		public PesananPembelianDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PesananPembelianDetail pesananPembelianDetail = (PesananPembelianDetail) data;

			if ((pesananPembelianDetail.getHargaBeli() == null || pesananPembelianDetail.getHargaBeli() < 1.0)) {
				Penyedia vendor = pesananPembelian.getPenyedia();
				Double myHargaBeli = (Double) session.createCriteria(HargaBeliItem.class)
						.add(Restrictions.eq("item", pesananPembelianDetail.getItem()))
						.add(Restrictions.eq("vendor", vendor)).setProjection(Projections.property("hargaBeli"))
						.setMaxResults(1).uniqueResult();

				pesananPembelianDetail.setHargaBeli(myHargaBeli == null ? 0.0 : myHargaBeli);
				Common.refreshUpdate(session, (pesananPembelianDetail));

			}

			new Label(pesananPembelianDetail.getItem() == null ? "" : pesananPembelianDetail.getItem().getKode())
					.setParent(row);

			RevisiHelper
					.createNewRevisi(PesananPembelianDetail.class, pesananPembelianDetail,
							pesananPembelianDetail.getItem() == null ? "" : pesananPembelianDetail.getItem().getNama())
					.setParent(row);

			Double myHargaBeli = pesananPembelianDetail.getHargaBeli() == null ? 0.0
					: pesananPembelianDetail.getHargaBeli();

			Double myJumlah = pesananPembelianDetail.getJumlah() == null ? 0.0 : pesananPembelianDetail.getJumlah();

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
					Penyedia vendor = pesananPembelian.getPenyedia();
					Double myHargaBeli = (Double) (vendor == null ? 0.0
							: session.createCriteria(HargaBeliItem.class)
									.add(Restrictions.eq("item", pesananPembelianDetail.getItem()))
									.add(Restrictions.eq("vendor", vendor))
									.setProjection(Projections.property("hargaBeli")).setMaxResults(1).uniqueResult());

					myHargaBeli = (myHargaBeli == null ? 0.0 : myHargaBeli);

					Double myJumlah = jumlah.getValue() == null ? 0.0 : jumlah.getValue();

					Double total = myHargaBeli * myJumlah;

					pesananPembelianDetail.setJumlah(myJumlah);
					pesananPembelianDetail.setHargaBeli(myHargaBeli);
					Common.refreshUpdate(session, (pesananPembelianDetail));

					hargaBeliLabel.setValue(Common.numberFormat.get().format(myHargaBeli));
					totalLabel.setValue(Common.numberFormat.get().format(total));

					loadTotal();
				}
			};

			jumlah.setDisabled(pesananPembelianDetail.getPesananPembelian().getDisetujuiOleh() != null);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, eventListener);

			new Label(pesananPembelianDetail.getSatuanItem() == null ? ""
					: pesananPembelianDetail.getSatuanItem().getNama()).setParent(row);

			hargaBeliLabel.setParent(row);
			totalLabel.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					pesananPembelianDetail.getKeterangan() == null ? "" : pesananPembelianDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(pesananPembelianDetail.getPesananPembelian().getDisetujuiOleh() != null);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pesananPembelianDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (pesananPembelianDetail));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setDisabled(pesananPembelianDetail.getPesananPembelian().getDisetujuiOleh() != null);
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
											Common.refreshDelete(pesananPembelianDetail);
											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/PesananPembelianDetailAction.java:201");
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
		List<PesananPembelianDetail> pesananPembelianDetails = session.createCriteria(PesananPembelianDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("pesananPembelian", pesananPembelian)).list();

		ListModel strset = new SimpleListModel(pesananPembelianDetails);
		grid.setRowRenderer(new PesananPembelianDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();

		loadTotal();
	}

	@SuppressWarnings("unchecked")
	private void loadTotal() {
		Session session = HibernateUtil.currentSession();
		List<PesananPembelianDetail> pesananPembelianDetails = session.createCriteria(PesananPembelianDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("pesananPembelian", pesananPembelian)).list();
		Double myJumlah = 0.0;
		Double myHarga = 0.0;
		for (PesananPembelianDetail pesananPembelianDetail : pesananPembelianDetails) {
			myJumlah += (pesananPembelianDetail.getJumlah() == null ? 0.0 : pesananPembelianDetail.getJumlah());
			myHarga += (pesananPembelianDetail.getHargaBeli() == null ? 0.0 : pesananPembelianDetail.getHargaBeli())
					* (pesananPembelianDetail.getJumlah() == null ? 0.0 : pesananPembelianDetail.getJumlah());
		}

		totalJumlah.setLabel(Common.numberFormat.get().format(myJumlah));
		totalHarga.setLabel(Common.numberFormat.get().format(myHarga));
	}

	public void display() {
		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item Pesanan Pembelian");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(panel);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Item", "/img/add_item.png");
		button.setDisabled(pesananPembelian.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(session.createCriteria(PesananPembelianDetail.class)
						.setProjection(Projections.groupProperty("item.id"))
						.add(Restrictions.eq("pesananPembelian", pesananPembelian)), ItemMedis.class, false);

				AmbilDataItemMedisBanyak ambilDataItemBanyak = new AmbilDataItemMedisBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {

							PesananPembelianDetail pesananPembelianDetail = new PesananPembelianDetail();
							pesananPembelianDetail.setItem(item);
							pesananPembelianDetail.setJumlah(0.0);
							pesananPembelianDetail.setKeterangan("");
							pesananPembelianDetail.setPesananPembelian(pesananPembelian);
							pesananPembelianDetail.setSatuanItem(item.getSatuanItem());
							session.save(pesananPembelianDetail);
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

		button = new ais.ui.util.MyToolbarbuttonConfig("Jadikan Default dari Permintaan Pembelian (Purchase Request)", "/img/check.png");
		button.setDisabled(pesananPembelian.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show(
						"Apakah Bapak/Ibu yakin ingin menjadikan data ini sebagai data default dari Permintaan Pembelian (Purchase Request)? Perlu diketahui, tindakan ini akan menggantikan seluruh data detail yang tersimpan saat ini.",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = new Integer(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Session session = HibernateUtil.currentSession();

									session.createSQLQuery(
											"delete from sirs.pesanan_pembelian_detail where pesanan_pembelian = "
													+ pesananPembelian.getId())
											.executeUpdate();
									List<PermintaanPembelianDetail> permintaanPembelianDetails = session
											.createCriteria(PermintaanPembelianDetail.class)
											.add(Restrictions.eq("vendor", pesananPembelian.getPenyedia()))
											.add(Restrictions.eq("permintaanPembelian",
													pesananPembelian.getPermintaanPembelian()))
											.list();

									for (PermintaanPembelianDetail permintaanPembelianDetail : permintaanPembelianDetails) {
										PesananPembelianDetail pesananPembelianDetail = new PesananPembelianDetail();
										pesananPembelianDetail.setItem(permintaanPembelianDetail.getItem());
										pesananPembelianDetail.setJumlah(permintaanPembelianDetail.getJumlah());
										pesananPembelianDetail.setKeterangan(permintaanPembelianDetail.getKeterangan());
										pesananPembelianDetail.setPesananPembelian(pesananPembelian);
										pesananPembelianDetail.setSatuanItem(permintaanPembelianDetail.getSatuanItem());
										pesananPembelianDetail.setPermintaanPembelianDetail(permintaanPembelianDetail);
										session.save(pesananPembelianDetail);
									}
									loadData(null);
								}
							}
						});
			}

		});
		button.setParent(toolbar);

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

		totalHarga = new Footer();
		totalHarga.setParent(foot);

		loadData(null);
	}

}
