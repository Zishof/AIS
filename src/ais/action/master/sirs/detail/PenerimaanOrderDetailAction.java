package ais.action.master.sirs.detail;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
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
import ais.database.model.sirs.PenerimaanOrder;
import ais.database.model.sirs.PenerimaanOrderDetail;
import ais.database.model.sirs.PesananPembelianDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk penerimaan order detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PenerimaanOrder penerimaanOrder},
 * {@code Grid grid}, {@code Footer totalJumlah}, {@code Footer totalBonus}, {@code Footer totalHarga}, {@code
 * Footer totalDiskon}, {@code Footer totalPajak}, {@code Footer total}; pembacaan/pencarian ({@code loadData()},
 * {@code loadTotal()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
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
public class PenerimaanOrderDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PenerimaanOrder penerimaanOrder;

	private Grid grid;

	public PenerimaanOrderDetailAction(PenerimaanOrder penerimaanOrder) {
		super();
		this.penerimaanOrder = penerimaanOrder;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PenerimaanOrderDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PenerimaanOrderDetailAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PenerimaanOrderDetailAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PenerimaanOrderDetailAction
	 */
	class PenerimaanOrderDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PenerimaanOrderDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PenerimaanOrderDetail penerimaanOrderDetail = (PenerimaanOrderDetail) data;
			new Label(penerimaanOrderDetail.getItem() == null ? "" : penerimaanOrderDetail.getItem().getKode())
					.setParent(row);

			RevisiHelper
					.createNewRevisi(PenerimaanOrderDetail.class, penerimaanOrderDetail,
							penerimaanOrderDetail.getItem() == null ? "" : penerimaanOrderDetail.getItem().getNama())
					.setParent(row);

			final Label total = new Label();

			final MyDoublebox hargaSatuan = new MyDoublebox(
					penerimaanOrderDetail == null ? null : penerimaanOrderDetail.getHargaBeli());
			hargaSatuan.setStyle("text-align:right");

			final MyDoublebox diskon = new MyDoublebox(
					penerimaanOrderDetail == null ? null : penerimaanOrderDetail.getHargaDiskon());
			diskon.setStyle("text-align:right");

			final MyDoublebox pajak = new MyDoublebox(
					penerimaanOrderDetail == null ? null : penerimaanOrderDetail.getHargaPajak());
			pajak.setStyle("text-align:right");

			final MyDoublebox jumlah;
			jumlah = new MyDoublebox(
					penerimaanOrderDetail.getJumlah() == null ? 0.0 : penerimaanOrderDetail.getJumlah());

			final MyDoublebox bonus;
			bonus = new MyDoublebox(
					penerimaanOrderDetail.getJumlahBonus() == null ? 0.0 : penerimaanOrderDetail.getJumlahBonus());

			EventListener perubahanEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();

					Double myJumlah = jumlah.getValue() == null ? 0.0 : jumlah.getValue();
					Double myHargaSatuan = hargaSatuan.getValue() == null ? 0.0 : hargaSatuan.getValue();
					Double myHargaPajak = pajak.getValue() == null ? 0.0 : pajak.getValue();
					Double myHargaDiskon = diskon.getValue() == null ? 0.0 : diskon.getValue();

					Double myHargaBeli = (myJumlah * myHargaSatuan) - myHargaDiskon + myHargaPajak;
					total.setValue(Common.numberFormat.get().format(myHargaBeli));

					penerimaanOrderDetail.setJumlahBonus(bonus.getValue());
					penerimaanOrderDetail.setHargaPajak(myHargaPajak);
					penerimaanOrderDetail.setHargaDiskon(myHargaDiskon);
					penerimaanOrderDetail.setJumlah(myJumlah);
					penerimaanOrderDetail.setHargaBeli(myHargaSatuan);
					session.saveOrUpdate(penerimaanOrderDetail);

					Penyedia vendor = penerimaanOrderDetail.getPenerimaanOrder().getPesananPembelian().getPenyedia();
					HargaBeliItem hargaBeliItem = (HargaBeliItem) session.createCriteria(HargaBeliItem.class)
							.add(Restrictions.eq("item", penerimaanOrderDetail.getItem()))
							.add(Restrictions.eq("vendor", vendor)).setMaxResults(1).uniqueResult();

					if (hargaBeliItem == null) {
						hargaBeliItem = new HargaBeliItem();
						hargaBeliItem.setPenyedia(vendor);
						hargaBeliItem.setItem(penerimaanOrderDetail.getItem());
						hargaBeliItem.setKeterangan("");
					}
					hargaBeliItem.setHargaBeli(hargaSatuan.getValue());
					session.saveOrUpdate(hargaBeliItem);

					ItemMedis item = hargaBeliItem.getItem();
					item.setDefaultHargaBeli(hargaBeliItem.getHargaBeli());
					session.saveOrUpdate(session.merge(item));

					loadTotal();
				}
			};

			jumlah.setParent(row);
			jumlah.setDisabled(penerimaanOrderDetail.getPenerimaanOrder().getDisetujuiOleh() != null);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, perubahanEventListener);

			bonus.setParent(row);
			bonus.setDisabled(penerimaanOrderDetail.getPenerimaanOrder().getDisetujuiOleh() != null);
			bonus.setStyle("text-align:right");
			bonus.setWidth("90%");
			bonus.addEventListener(Events.ON_CHANGE, perubahanEventListener);

			new Label(penerimaanOrderDetail.getSatuanItem() == null ? ""
					: penerimaanOrderDetail.getSatuanItem().getNama()).setParent(row);

			final MyDatebox tanggalKadaluarsa = new MyDatebox(
					penerimaanOrderDetail == null ? null : penerimaanOrderDetail.getTanggalKadaluarsa());
			tanggalKadaluarsa.setFormat(Common.dateFormat2.get().toPattern());
			tanggalKadaluarsa.setParent(row);
			tanggalKadaluarsa.setWidth("90%");
			tanggalKadaluarsa.setDisabled(penerimaanOrderDetail.getPenerimaanOrder().getDisetujuiOleh() != null);
			tanggalKadaluarsa.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					penerimaanOrderDetail.setTanggalKadaluarsa(tanggalKadaluarsa.getValue());
					session.saveOrUpdate(penerimaanOrderDetail);

				}
			});

			hargaSatuan.setParent(row);
			hargaSatuan.setWidth("90%");
			hargaSatuan.setDisabled(penerimaanOrderDetail.getPenerimaanOrder().getDisetujuiOleh() != null);
			hargaSatuan.addEventListener("onChange", perubahanEventListener);

			diskon.setParent(row);
			diskon.setWidth("90%");
			diskon.setDisabled(penerimaanOrderDetail.getPenerimaanOrder().getDisetujuiOleh() != null);
			diskon.addEventListener("onChange", perubahanEventListener);

			pajak.setParent(row);
			pajak.setWidth("90%");
			pajak.setDisabled(penerimaanOrderDetail.getPenerimaanOrder().getDisetujuiOleh() != null);
			pajak.addEventListener("onChange", perubahanEventListener);

			Double myJumlah = jumlah.getValue() == null ? 0.0 : jumlah.getValue();
			Double myHargaSatuan = hargaSatuan.getValue() == null ? 0.0 : hargaSatuan.getValue();
			Double myHargaPajak = pajak.getValue() == null ? 0.0 : pajak.getValue();
			Double myHargaDiskon = diskon.getValue() == null ? 0.0 : diskon.getValue();
			Double myHargaBeli = (myJumlah * myHargaSatuan) - myHargaDiskon + myHargaPajak;
			total.setValue(Common.numberFormat.get().format(myHargaBeli));
			total.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					penerimaanOrderDetail.getKeterangan() == null ? "" : penerimaanOrderDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(penerimaanOrderDetail.getPenerimaanOrder().getDisetujuiOleh() != null);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					penerimaanOrderDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (penerimaanOrderDetail));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setDisabled(penerimaanOrderDetail.getPenerimaanOrder().getDisetujuiOleh() != null);
			button.setVisible(penerimaanOrderDetail.getPenerimaanOrder().getPostingHistory() == null);
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
											Common.refreshDelete(penerimaanOrderDetail);

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/PenerimaanOrderDetailAction.java:252");
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
		List<PenerimaanOrderDetail> penerimaanOrderDetails = session.createCriteria(PenerimaanOrderDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("penerimaanOrder", penerimaanOrder)).list();

		ListModel strset = new SimpleListModel(penerimaanOrderDetails);
		grid.setRowRenderer(new PenerimaanOrderDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();

		loadTotal();
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
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Item Banyak", "/img/add_item.png");
		button.setDisabled(penerimaanOrder.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(session.createCriteria(PenerimaanOrderDetail.class)
						.setProjection(Projections.groupProperty("item.id"))
						.add(Restrictions.eq("penerimaanOrder", penerimaanOrder)), ItemMedis.class, false);

				AmbilDataItemMedisBanyak ambilDataItemBanyak = new AmbilDataItemMedisBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {
							Penyedia vendor = penerimaanOrder.getPesananPembelian().getPenyedia();
							HargaBeliItem hargaBeliItem = (HargaBeliItem) session.createCriteria(HargaBeliItem.class)
									.add(Restrictions.eq("item", item)).add(Restrictions.eq("vendor", vendor))
									.setMaxResults(1).uniqueResult();

							PenerimaanOrderDetail penerimaanOrderDetail = new PenerimaanOrderDetail();
							penerimaanOrderDetail
									.setHargaBeli(hargaBeliItem == null || hargaBeliItem.getHargaBeli() == null ? 0.0
											: hargaBeliItem.getHargaBeli());
							penerimaanOrderDetail.setItem(item);
							penerimaanOrderDetail.setJumlah(0.0);
							penerimaanOrderDetail.setKeterangan("");
							penerimaanOrderDetail.setPenerimaanOrder(penerimaanOrder);
							penerimaanOrderDetail.setSatuanItem(item.getSatuanItem());
							session.save(penerimaanOrderDetail);
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

		button = new ais.ui.util.MyToolbarbuttonConfig("Jadikan Default dari Pemesanan Pembelian (Purchase Order)", "/img/check.png");
		button.setDisabled(penerimaanOrder.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show(
						"Apakah Bapak/Ibu yakin ingin menjadikan data ini sebagai data default dari Pemesanan Pembelian (Purchase Order)? Perlu diketahui, tindakan ini akan menggantikan seluruh data detail yang tersimpan saat ini.",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = new Integer(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Session session = HibernateUtil.currentSession();

									session.createSQLQuery(
											"delete from sirs.penerimaan_order_detail where penerimaan_order = "
													+ penerimaanOrder.getId())
											.executeUpdate();
									List<PesananPembelianDetail> pesananPembelianDetails = session
											.createCriteria(PesananPembelianDetail.class).add(Restrictions
													.eq("pesananPembelian", penerimaanOrder.getPesananPembelian()))
											.list();

									for (PesananPembelianDetail pesananPembelianDetail : pesananPembelianDetails) {
										Penyedia vendor = pesananPembelianDetail.getPesananPembelian().getPenyedia();
										HargaBeliItem hargaBeliItem = (HargaBeliItem) session
												.createCriteria(HargaBeliItem.class)
												.add(Restrictions.eq("item", pesananPembelianDetail.getItem()))
												.add(Restrictions.eq("vendor", vendor)).setMaxResults(1).uniqueResult();

										PenerimaanOrderDetail penerimaanOrderDetail = new PenerimaanOrderDetail();
										penerimaanOrderDetail.setHargaBeli(
												hargaBeliItem == null || hargaBeliItem.getHargaBeli() == null ? 0.0
														: hargaBeliItem.getHargaBeli());

										penerimaanOrderDetail.setItem(pesananPembelianDetail.getItem());
										penerimaanOrderDetail.setJumlah(pesananPembelianDetail.getJumlah());
										penerimaanOrderDetail.setKeterangan(pesananPembelianDetail.getKeterangan());
										penerimaanOrderDetail.setPenerimaanOrder(penerimaanOrder);
										penerimaanOrderDetail.setSatuanItem(pesananPembelianDetail.getSatuanItem());
										penerimaanOrderDetail.setPesananPembelianDetail(pesananPembelianDetail);
										session.save(penerimaanOrderDetail);
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
		column.setLabel("Kode");
		column.setWidth("5%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jml");
		column.setAlign("rigth");
		column.setWidth("5%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Bonus");
		column.setAlign("rigth");
		column.setWidth("5%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kadaluarsa");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Harga");
		column.setAlign("rigth");
		column.setWidth("8%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Diskon");
		column.setAlign("rigth");
		column.setVisible(false);
		column.setWidth("8%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Pajak");
		column.setAlign("rigth");
		column.setVisible(false);
		column.setWidth("8%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jmlh");
		column.setAlign("rigth");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("4%");

		Foot foot = new Foot();
		foot.setParent(grid);

		foot.appendChild(new Footer());
		foot.appendChild(new Footer("Total Qty"));

		totalJumlah = new Footer();
		totalJumlah.setParent(foot);
		totalJumlah.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		totalBonus = new Footer();
		totalBonus.setParent(foot);
		totalBonus.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		foot.appendChild(new Footer());
		foot.appendChild(new Footer());

		totalHarga = new Footer();
		totalHarga.setParent(foot);
		totalHarga.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		totalDiskon = new Footer();
		totalDiskon.setParent(foot);
		totalDiskon.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		totalPajak = new Footer();
		totalPajak.setParent(foot);
		totalPajak.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		total = new Footer();
		total.setParent(foot);
		total.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		loadData(null);
	}

	@SuppressWarnings("unchecked")
	public void loadTotal() {
		Session session = HibernateUtil.currentSession();

		ProjectionList projectionList = Projections.projectionList();
		projectionList.add(Projections.property("jumlah"));
		projectionList.add(Projections.property("jumlahBonus"));
		projectionList.add(Projections.property("hargaBeli"));
		projectionList.add(Projections.property("hargaDiskon"));
		projectionList.add(Projections.property("hargaPajak"));

		List<Object[]> penerimaanOrderDetails = session.createCriteria(PenerimaanOrderDetail.class)
				.setProjection(projectionList).add(Restrictions.eq("penerimaanOrder", penerimaanOrder)).list();

		Double jumlah = 0.0;
		Double jumlahBonus = 0.0;
		Double hargaBeli = 0.0;
		Double hargaDiskon = 0.0;
		Double hargaPajak = 0.0;

		Double myTotal = 0.0;

		for (Object[] objects : penerimaanOrderDetails) {
			jumlah += ((Double) (objects[0] == null ? 0.0 : objects[0]));
			jumlahBonus += ((Double) (objects[1] == null ? 0.0 : objects[1]));
			hargaBeli += ((Double) (objects[2] == null ? 0.0 : objects[2]));
			hargaDiskon += ((Double) (objects[3] == null ? 0.0 : objects[3]));
			hargaPajak += ((Double) (objects[4] == null ? 0.0 : objects[4]));

			myTotal += (((Double) (objects[0] == null ? 0.0 : objects[0]))
					* ((Double) (objects[2] == null ? 0.0 : objects[2])))
					- ((Double) (objects[3] == null ? 0.0 : objects[3]))
					+ ((Double) (objects[4] == null ? 0.0 : objects[4]));

			System.out.println("myTotal = " + myTotal);
		}

		totalJumlah.setLabel(Common.numberFormat.get().format(jumlah));
		totalBonus.setLabel(Common.numberFormat.get().format(jumlahBonus));
		totalHarga.setLabel(Common.numberFormat.get().format(hargaBeli));
		totalDiskon.setLabel(Common.numberFormat.get().format(hargaDiskon));
		totalPajak.setLabel(Common.numberFormat.get().format(hargaPajak));
		total.setLabel(Common.numberFormat.get().format(myTotal));
	}

	private Footer totalJumlah;
	private Footer totalBonus;
	private Footer totalHarga;
	private Footer totalDiskon;
	private Footer totalPajak;
	private Footer total;

}
