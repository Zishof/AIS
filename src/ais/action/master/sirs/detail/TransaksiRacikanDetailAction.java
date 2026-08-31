package ais.action.master.sirs.detail;

import java.util.HashSet;
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
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataItemMedisBanyakBerdasarkanStok;
import ais.action.master.sirs.util.CommonTarifItem;
import ais.common.Common;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Diskon;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.PajakMedis;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.RacikanDetail;
import ais.database.model.sirs.TransaksiMedisDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;

/**
 * Controller/action ZK untuk transaksi racikan detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Racikan racikan}, {@code Grid grid},
 * {@code TransaksiMedisDetail transaksiDetail}, {@code KelasPerawatan kelasPerawatan}, {@code Footer totalHrg},
 * {@code Footer totalDiskon}, {@code Footer totalPajak}, {@code EventListener eventListener};
 * pembacaan/pencarian ({@code loadData()}, {@code reloadDokter()}, {@code loadTotal()}); operasi domain lain
 * ({@code display()}); konfigurasi constructor: {@code hasOpen}. Bagian lain dari kontrak tetap mengikuti kelas
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
public class TransaksiRacikanDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Racikan racikan;
	private Grid grid;

	private TransaksiMedisDetail transaksiDetail;

	// private Racikan racikanVariasi;
	private KelasPerawatan kelasPerawatan;

	private Footer totalHrg;
	private Footer totalDiskon;
	private Footer totalPajak;

	private EventListener eventListener;

	private Boolean hasOpen = false;

	public TransaksiRacikanDetailAction(final Racikan racikan, final TransaksiMedisDetail transaksiDetail,
			KelasPerawatan kelasPerawatan, EventListener eventListener) {
		super();

		if (racikan.getResepDetail() == null) {
			this.racikan = racikan.getVariasiDari() == null ? racikan : racikan.getVariasiDari();
		} else {
			this.racikan = racikan;
		}

		this.kelasPerawatan = kelasPerawatan;
		this.transaksiDetail = transaksiDetail;
		this.eventListener = eventListener;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (isOpen() && !hasOpen) {
					display();
					hasOpen = true;
				}
			}
		});

	}

	class RacikanDetailRenderer extends ais.ui.util.MyRowRenderer {

		public RacikanDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final RacikanDetail racikanDetail = (RacikanDetail) data;

			Session session = HibernateUtil.currentSession();

			Double diskonPersen = CommonSirs.getTotalDiskonDalamPersen(racikanDetail.getItem(), null, null,
					racikanDetail.getJumlah().intValue(), transaksiDetail.getTanggal(),
					transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
					transaksiDetail.getTransaksi().getPendaftaran().getKomunitass());
			Double pajakPersen = CommonSirs.getTotalPajakDalamPersen(racikanDetail.getItem(), null, null,
					transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
					transaksiDetail.getTransaksi().getPendaftaran().getKomunitass());

			racikanDetail.setPajaks(new HashSet<PajakMedis>());
			racikanDetail.getPajaks()
					.addAll(CommonSirs.getPajakSekarang(racikanDetail.getItem(), null, null,
							transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
							transaksiDetail.getTransaksi().getPendaftaran().getKomunitass()));
			racikanDetail.setDiskons(new HashSet<Diskon>());
			racikanDetail.getDiskons()
					.addAll(CommonSirs.getDiskonSekarang(racikanDetail.getItem(), null, null,
							racikanDetail.getJumlah().intValue(), transaksiDetail.getTanggal(),
							transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
							transaksiDetail.getTransaksi().getPendaftaran().getKomunitass()));

			racikanDetail.setDiskonPersen(diskonPersen);
			racikanDetail.setPajakPersen(pajakPersen);

			final Label diskon;
			final Label pajak;

			diskon = new Label();
			pajak = new Label();

			RevisiHelper.createNewRevisi(RacikanDetail.class, racikanDetail,
					racikanDetail.getItem() == null ? "" : racikanDetail.getItem().getNama()).setParent(row);

			HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(racikanDetail.getItem(), kelasPerawatan,
					transaksiDetail.getDokter(), transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
					transaksiDetail.getTransaksi().getPendaftaran().getKomunitass(),
					transaksiDetail.getTransaksi().getPendaftaran().getPasien());
			racikanDetail.setHargaTransaksi(hargaJualItem.getHargaJual());
			session.update(racikanDetail);

			diskon.setValue(Common.numberFormat.get().format(racikanDetail.getDiskonPersen()) + "% ("
					+ Common.numberFormat.get().format(racikanDetail.getDiskon() * racikanDetail.getJumlah()) + ")");

			pajak.setValue(Common.numberFormat.get().format(racikanDetail.getPajakPersen()) + "% ("
					+ Common.numberFormat.get().format(racikanDetail.getPajak() * racikanDetail.getJumlah()) + ")");

			final Label harga = new Label(Common.numberFormat.get()
					.format(racikanDetail.getHargaTransaksi() == null ? 0.0 : racikanDetail.getHargaTransaksi()));

			final Label total = new Label();

			final MyDoublebox jumlah;
			(jumlah = new MyDoublebox(racikanDetail.getJumlah() == null ? 0.0 : racikanDetail.getJumlah()))
					.setParent(row);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					Session session = HibernateUtil.currentSession();

					HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(racikanDetail.getItem(),
							kelasPerawatan, transaksiDetail.getDokter(),
							transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
							transaksiDetail.getTransaksi().getPendaftaran().getKomunitass(),
							transaksiDetail.getTransaksi().getPendaftaran().getPasien());

					racikanDetail.setJumlah(jumlah.getValue());

					Double diskonPersen = CommonSirs.getTotalDiskonDalamPersen(racikanDetail.getItem(), null, null,
							racikanDetail.getJumlah().intValue(), transaksiDetail.getTanggal(),
							transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
							transaksiDetail.getTransaksi().getPendaftaran().getKomunitass());
					Double pajakPersen = CommonSirs.getTotalPajakDalamPersen(racikanDetail.getItem(), null, null,
							transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
							transaksiDetail.getTransaksi().getPendaftaran().getKomunitass());

					racikanDetail.setPajaks(new HashSet<PajakMedis>());
					racikanDetail.getPajaks()
							.addAll(CommonSirs.getPajakSekarang(racikanDetail.getItem(), null, null,
									transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
									transaksiDetail.getTransaksi().getPendaftaran().getKomunitass()));
					racikanDetail.setDiskons(new HashSet<Diskon>());
					racikanDetail.getDiskons()
							.addAll(CommonSirs.getDiskonSekarang(racikanDetail.getItem(), null, null,
									racikanDetail.getJumlah().intValue(), transaksiDetail.getTanggal(),
									transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
									transaksiDetail.getTransaksi().getPendaftaran().getKomunitass()));

					racikanDetail.setDiskonPersen(diskonPersen);
					racikanDetail.setPajakPersen(pajakPersen);

					racikanDetail.setHargaTransaksi(hargaJualItem == null ? 0.0 : hargaJualItem.getHargaJual());
					racikanDetail.setJumlah(jumlah.getValue());
					session.update(racikanDetail);

					total.setValue(Common.numberFormat.get().format(racikanDetail.getHasilPenghitunganTotal()));
					harga.setValue(Common.numberFormat.get().format(
							racikanDetail.getHargaTransaksi() == null ? 0.0 : racikanDetail.getHargaTransaksi()));

					diskon.setValue(Common.numberFormat.get().format(racikanDetail.getDiskonPersen()) + "% ("
							+ Common.numberFormat.get().format(racikanDetail.getDiskon() * racikanDetail.getJumlah()) + ")");

					pajak.setValue(Common.numberFormat.get().format(racikanDetail.getPajakPersen()) + "% ("
							+ Common.numberFormat.get().format(racikanDetail.getPajak() * racikanDetail.getJumlah()) + ")");

					loadTotal();

				}
			};
			jumlah.addEventListener(Events.ON_CHANGE, eventListener);

			new Label(racikanDetail.getItem() == null || racikanDetail.getItem().getSatuanItem() == null ? ""
					: racikanDetail.getItem().getSatuanItem().getNama()).setParent(row);

			harga.setParent(row);

			diskon.setParent(row);
			pajak.setParent(row);

			total.setValue(Common.numberFormat.get().format(racikanDetail.getHasilPenghitunganTotal()));
			total.setParent(row);

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui, data yang telah dihapus tidak dapat dikembalikan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(racikanDetail);
											loadData(null);
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/TransaksiRacikanDetailAction.java:257");
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
	public void loadData(Object value) throws Exception {
		// buatSubRacikan();
		Session session = HibernateUtil.currentSession();
		List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("racikan", racikan)).list();

		ListModel strset = new SimpleListModel(racikanDetails);
		grid.setRowRenderer(new RacikanDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();

		loadTotal();
	}

	private void display() throws Exception {

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(this);
		groupbox.appendChild(new Caption("Daftar Item Racikan"));

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Item", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(session.createCriteria(RacikanDetail.class)
						.setProjection(Projections.groupProperty("item.id")).add(Restrictions.eq("racikan", racikan)),
						ItemMedis.class, false);

				AmbilDataItemMedisBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemMedisBanyakBerdasarkanStok(
						items, transaksiDetail.getTransaksi().getLokasi(), true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {

							Double diskon = CommonSirs.getTotalDiskonDalamPersen(item, null, null, 1,
									transaksiDetail.getTanggal(),
									transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
									transaksiDetail.getTransaksi().getPendaftaran().getKomunitass());
							Double pajak = CommonSirs.getTotalPajakDalamPersen(item, null, null,
									transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
									transaksiDetail.getTransaksi().getPendaftaran().getKomunitass());

							RacikanDetail racikanDetail = new RacikanDetail();
							racikanDetail.setDiskonPersen(diskon);
							racikanDetail.setPajakPersen(pajak);
							racikanDetail.getPajaks()
									.addAll(CommonSirs.getPajakSekarang(item, null, null,
											transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
											transaksiDetail.getTransaksi().getPendaftaran().getKomunitass()));
							racikanDetail.getDiskons()
									.addAll(CommonSirs.getDiskonSekarang(item, null, null, 1,
											transaksiDetail.getTanggal(),
											transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
											transaksiDetail.getTransaksi().getPendaftaran().getKomunitass()));

							racikanDetail.setItem(item);
							racikanDetail.setJumlah(1.0);
							racikanDetail.setKeterangan("");
							racikanDetail.setRacikan(racikan);
							session.save(racikanDetail);
						}

						loadData(null);
						Double hargaJual = CommonSirs.hitungHargaJualRacikan(racikan, kelasPerawatan,
								transaksiDetail.getDokter(),
								transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
								transaksiDetail.getTransaksi().getPendaftaran().getKomunitass(),
								transaksiDetail.getTransaksi().getPasien());
						transaksiDetail.setAmount(hargaJual);
						transaksiDetail.setAmountCustom(transaksiDetail.getAmount());

						Common.refreshUpdate(session, (transaksiDetail));
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
		column.setLabel("Nama");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Hrg");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Diskon");
		column.setAlign("right");
		column.setWidth("14%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Pajak");
		column.setAlign("right");
		column.setWidth("14%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ttl");
		column.setAlign("right");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("7%");

		Foot foot = new Foot();
		foot.setParent(grid);

		foot.appendChild(new Footer("Total"));
		foot.appendChild(new Footer());
		foot.appendChild(new Footer());
		foot.appendChild(new Footer());

		totalDiskon = new Footer();
		totalDiskon.setParent(foot);
		totalDiskon.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		totalPajak = new Footer();
		totalPajak.setParent(foot);
		totalPajak.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		totalHrg = new Footer();
		totalHrg.setParent(foot);
		totalHrg.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		foot.appendChild(new Footer());

		loadData(null);
	}

	public void reloadDokter(Dokter dokter) throws Exception {
		transaksiDetail.setDokter(dokter);
		loadData(null);
	}

	@SuppressWarnings("unchecked")
	public void loadTotal() throws Exception {

		Double mydiskon = 0.0;
		Double mypajak = 0.0;

		Double hargaJual = CommonSirs.hitungHargaJualRacikan(racikan, kelasPerawatan, transaksiDetail.getDokter(),
				transaksiDetail.getTransaksi().getPendaftaran().getAsuransi(),
				transaksiDetail.getTransaksi().getPendaftaran().getKomunitass(),
				transaksiDetail.getTransaksi().getPasien());
		transaksiDetail.setAmount(hargaJual);

		totalHrg.setLabel(hargaJual == null ? "0.0" : Common.numberFormat.get().format(hargaJual));

		Session session = HibernateUtil.currentSession();
		List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("racikan", racikan)).list();
		for (RacikanDetail racikanDetail : racikanDetails) {
			mydiskon += racikanDetail.getDiskon() * racikanDetail.getJumlah();
			mypajak += racikanDetail.getPajak() * racikanDetail.getJumlah();
		}

		transaksiDetail.setDiskon(mydiskon);
		transaksiDetail.setPajak(mypajak);
		session.update((transaksiDetail));

		totalDiskon.setLabel(mydiskon == null ? "0.0" : Common.numberFormat.get().format(mydiskon));
		totalPajak.setLabel(mypajak == null ? "0.0" : Common.numberFormat.get().format(mypajak));

		if (eventListener != null) {
			Event event = new Event("event1", null, transaksiDetail);
			eventListener.onEvent(event);
		}
	}

}
