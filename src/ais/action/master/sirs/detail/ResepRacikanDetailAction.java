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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.RacikanDetail;
import ais.database.model.sirs.ResepDetail;
import ais.ui.util.MyDoublebox;

/**
 * Controller/action ZK untuk resep racikan detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Racikan racikan}, {@code Grid grid},
 * {@code KelasPerawatan kelasPerawatan}, {@code EventListener eventListener}; pembacaan/pencarian ({@code
 * loadData()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
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
public class ResepRacikanDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Racikan racikan;
	private Grid grid;

	// private Racikan racikan;
	private KelasPerawatan kelasPerawatan;

	private EventListener eventListener;

	public ResepRacikanDetailAction(final Racikan racikan, final ResepDetail resepDetail, KelasPerawatan kelasPerawatan,
			EventListener eventListener) {
		super();
		this.racikan = racikan.getVariasiDari() == null ? racikan : racikan.getVariasiDari();
		this.kelasPerawatan = kelasPerawatan;
		this.eventListener = eventListener;

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(ResepRacikanDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link ResepRacikanDetailAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ResepRacikanDetailAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see ResepRacikanDetailAction
	 */
	class RacikanDetailRenderer extends ais.ui.util.MyRowRenderer {

		public RacikanDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			Session session = HibernateUtil.currentSession();
			final RacikanDetail racikanDetail = (RacikanDetail) data;

			RevisiHelper.createNewRevisi(RacikanDetail.class, racikanDetail,
					racikanDetail.getItem() == null ? "" : racikanDetail.getItem().getNama()).setParent(row);

			HargaJualItem hargaJualItem = (HargaJualItem) session.createCriteria(HargaJualItem.class)
					.add(Restrictions.eq("item", racikanDetail.getItem()))
					.add(Restrictions.eq("kelasPerawatan", kelasPerawatan)).setMaxResults(1).uniqueResult();
			racikanDetail.setHargaTransaksi(hargaJualItem == null ? 0.0 : hargaJualItem.getHargaJual());
			session.update(racikanDetail);

			final MyDoublebox jumlah;
			(jumlah = new MyDoublebox(racikanDetail.getJumlah() == null ? 0.0 : racikanDetail.getJumlah()))
					.setParent(row);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");

			EventListener eventListener = new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					Session session = HibernateUtil.currentSession();

					HargaJualItem hargaJualItem = (HargaJualItem) session.createCriteria(HargaJualItem.class)
							.add(Restrictions.eq("item", racikanDetail.getItem()))
							.add(Restrictions.eq("kelasPerawatan", kelasPerawatan)).setMaxResults(1).uniqueResult();

					racikanDetail.setHargaTransaksi(hargaJualItem == null ? 0.0 : hargaJualItem.getHargaJual());
					racikanDetail.setJumlah(jumlah.getValue());
					Common.refreshUpdate(session, (racikanDetail));

					List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
							.addOrder(Order.desc("id")).add(Restrictions.eq("racikan", racikan)).list();

					String nama = "";
					for (RacikanDetail racikanDetail : racikanDetails) {
						nama += racikanDetail.getItem() == null ? ""
								: (racikanDetail.getItem().getNama() + " => " + racikanDetail.getJumlah() + ", ");
					}
					racikan.setNama(nama);
					Common.refreshUpdate(session, (racikan));

					ResepRacikanDetailAction.this.eventListener.onEvent(new Event("", null, racikan));

				}
			};
			jumlah.addEventListener(Events.ON_CHANGE, eventListener);

			new Label(racikanDetail.getItem() == null || racikanDetail.getItem().getSatuanItem() == null ? ""
					: racikanDetail.getItem().getSatuanItem().getNama()).setParent(row);

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
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/ResepRacikanDetailAction.java:156");
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
	public void loadData(Object value) throws Exception {
		// buatSubRacikan();
		Session session = HibernateUtil.currentSession();
		List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("racikan", racikan)).list();

		ListModel strset = new SimpleListModel(racikanDetails);
		grid.setRowRenderer(new RacikanDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	// @SuppressWarnings("unchecked")
	// private void buatSubRacikan() {
	// // Session session = HibernateUtil.currentSession();
	// // racikan = (Racikan) session.createCriteria(Racikan.class)
	// // .add(Restrictions.eq("variasiDari", racikan))
	// // .add(Restrictions.eq("resepDetail", resepDetail))
	// // .setMaxResults(1).uniqueResult();
	// // if (racikan == null) {
	// // racikan = new Racikan();
	// // racikan.setJenisRacikan(racikan.getJenisRacikan());
	// // racikan.setKeterangan("Variasi dari racikan "
	// // + racikan.getNama());
	// // racikan.setKode(Common.generateCode(Racikan.class, 8));
	// // racikan.setNama(racikan.getNama());
	// // racikan.setResepDetail(resepDetail);
	// // racikan.setVariasiDari(racikan);
	// // session.save(racikan);
	// //
	// // List<RacikanDetail> racikanDetails = session
	// // .createCriteria(RacikanDetail.class)
	// // .add(Restrictions.eq("racikan", racikan)).list();
	// // for (RacikanDetail racikanDetail : racikanDetails) {
	// // RacikanDetail myRacikanDetail = new RacikanDetail();
	// // myRacikanDetail.setItem(racikanDetail.getItem());
	// // myRacikanDetail.setJumlah(racikanDetail.getJumlah());
	// // myRacikanDetail.setKeterangan(racikanDetail.getKeterangan());
	// // myRacikanDetail.setRacikan(racikan);
	// // session.save(myRacikanDetail);
	// // }
	// //
	// // resepDetail.setRacikan(racikan);
	// // resepDetail.setItem(null);
	// // session.save(session.merge(resepDetail));
	// //
	// // }
	// }

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
							racikanDetail.setJumlah(1.0);
							racikanDetail.setKeterangan("");
							racikanDetail.setRacikan(racikan);
							session.save(racikanDetail);
						}

						List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
								.addOrder(Order.desc("id")).add(Restrictions.eq("racikan", racikan)).list();

						String nama = "";
						for (RacikanDetail racikanDetail : racikanDetails) {
							nama += racikanDetail.getItem() == null ? ""
									: (racikanDetail.getItem().getNama() + " => " + racikanDetail.getJumlah() + ", ");
						}
						racikan.setNama(nama);
						Common.refreshUpdate(session, racikan);

						eventListener.onEvent(new Event("", null, racikan));

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
		column.setLabel("Nama");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setAlign("right");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("7%");

		loadData(null);
	}

}
