package ais.action.master.library.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DendaKeterlambatanItem;
import ais.database.model.library.KembaliPengadaanItem;
import ais.database.model.library.KembaliPengadaanItemDetail;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk kembali pengadaan item detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code KembaliPengadaanItem
 * kembaliPengadaanItem}, {@code MyGrid grid}, {@code boolean edit}; pembacaan/pencarian ({@code loadData()});
 * operasi domain lain ({@code display()}); konfigurasi constructor: {@code edit}. Bagian lain dari kontrak tetap
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
public class KembaliPengadaanItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private KembaliPengadaanItem kembaliPengadaanItem;
	private MyGrid grid;
	private boolean edit = false;

	// private boolean delete = false;

	public KembaliPengadaanItemDetailAction(KembaliPengadaanItem kembaliPengadaanItem) {
		super();
		this.kembaliPengadaanItem = kembaliPengadaanItem;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(KembaliPengadaanItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KembaliPengadaanItemDetailAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KembaliPengadaanItemDetailAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KembaliPengadaanItemDetailAction
	 */
	class KembaliPengadaanItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public KembaliPengadaanItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KembaliPengadaanItemDetail kembaliPengadaanItemDetail = (KembaliPengadaanItemDetail) data;

			Image image = LibraryUtil.generateImage(kembaliPengadaanItemDetail.getItem());
			image.setWidth("100%");
			image.setParent(row);

			new Label(kembaliPengadaanItemDetail.getItem() == null ? ""
					: kembaliPengadaanItemDetail.getItem().getIsbn() + " "
							+ kembaliPengadaanItemDetail.getItem().getIssn())
					.setParent(row);

			new Label(kembaliPengadaanItemDetail.getItemPunyaBarcode() == null ? ""
					: kembaliPengadaanItemDetail.getItemPunyaBarcode().getBarcode()).setParent(row);

			RevisiHelper.createNewRevisi(KembaliPengadaanItemDetail.class, kembaliPengadaanItemDetail,
					kembaliPengadaanItemDetail.getItem() == null ? "" : kembaliPengadaanItemDetail.getItem().getNama())
					.setParent(row);

			final Label perpanjang = new Label(
					kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail().getJumlahPerpanjangan() + " kali");
			perpanjang.setParent(row);

			new Label(Common.dateFormat4.get().format(
					kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail().getBatasWaktupengembalian()) + "  \n"
					+ kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail().getJumlahHariBatas() + " hari")
					.setParent(row);

			final Label terlambat = new Label(
					kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail().getJumlahHariTerlambat() + " hari");
			terlambat.setParent(row);

			final ais.ui.util.MyDatebox tanggal = new ais.ui.util.MyDatebox(kembaliPengadaanItemDetail.getTanggal());
			tanggal.setFormat(Common.dateFormat.get().toPattern());

			tanggal.setDisabled(
					kembaliPengadaanItemDetail.getKembaliPengadaanItem().getDisetujuiOleh() != null || !edit);
			tanggal.setWidth("90%");
			tanggal.setReadonly(true);
			tanggal.setParent(row);

			final MyTextbox keterangan = new MyTextbox(kembaliPengadaanItemDetail.getKeterangan() == null ? ""
					: kembaliPengadaanItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setRows(3);
			keterangan.setParent(row);
			keterangan.setDisabled(
					kembaliPengadaanItemDetail.getKembaliPengadaanItem().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					kembaliPengadaanItemDetail.setKeterangan(keterangan.getValue());
					session.update(kembaliPengadaanItemDetail);
				}
			});

			final Label textDenda = new Label(Common.numberFormat.get().format(kembaliPengadaanItemDetail.getDenda()));
			textDenda.setParent(row);

			final EventListener tanggalEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (tanggal.getValue() == null) {
						tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
					}

					kembaliPengadaanItemDetail.setTanggal(tanggal.getValue());

					// int jumlahSelisihHari =
					// Common.getWorkingDaysBetweenTwoDates(kembaliPengadaanItemDetail
					// .getKembaliPengadaanItem().getPeminjamanPengadaanItem().getTanggalPembuatan(),
					// kembaliPengadaanItemDetail.getTanggal());
					//
					// System.out.println("jumlahSelisihHari = " +
					// jumlahSelisihHari);

					PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = kembaliPengadaanItemDetail
							.getPeminjamanPengadaanItemDetail();

					peminjamanPengadaanItemDetail.setTanggalKembali(tanggal.getValue());

					peminjamanPengadaanItemDetail.setKembaliPengadaanItemDetail(kembaliPengadaanItemDetail);

					perpanjang.setValue(peminjamanPengadaanItemDetail.getJumlahPerpanjangan() + " kali");

					terlambat.setValue(peminjamanPengadaanItemDetail.getJumlahHariTerlambat() + " hari");

					DendaKeterlambatanItem dendaPerItem = LibraryUtil.hitungDendaItem(peminjamanPengadaanItemDetail);

					Double denda = (dendaPerItem == null || dendaPerItem.getDenda() == null) ? 0.0 : dendaPerItem.getDenda();
					denda = denda * peminjamanPengadaanItemDetail.getJumlah();

					if (dendaPerItem != null && !dendaPerItem.getKeterangan().isEmpty()) {
						textDenda.setValue(dendaPerItem.getKeterangan());
					} else {
						textDenda.setValue(Common.numberFormat.get().format(denda));
					}
					kembaliPengadaanItemDetail.setKetDenda(dendaPerItem == null ? "" : dendaPerItem.getKeterangan());

					kembaliPengadaanItemDetail.setDenda(denda);
					kembaliPengadaanItemDetail.setPeminjamanPengadaanItemDetail(peminjamanPengadaanItemDetail);

					row.setValign("top");row.setAttribute("kembaliPengadaanItemDetail", kembaliPengadaanItemDetail);
				}
			};

			tanggalEventListener.onEvent(null);
			tanggal.addEventListener("onChange", tanggalEventListener);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("Perpanjang", "/img/corner.gif");
			final MyToolbarbuttonConfig batalPerpanjang = new MyToolbarbuttonConfig("Batal Perpanjang",
					"/img/svg/warning-outline.svg");

			rubah.setOrient("vertical");
			rubah.setTooltiptext("Perpanjang");
//			rubah.setDisabled(kembaliPengadaanItemDetail.getKembaliPengadaanItem().getDisetujuiOleh() != null || !edit);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					LibraryUtil.onPerpanjang(kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail());
					tanggalEventListener.onEvent(event);
					batalPerpanjang.setVisible(
							kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail().getJumlahPerpanjangan() > 0);
				}

			});
			aksiButtons.add(rubah);

			batalPerpanjang.setOrient("vertical");
			batalPerpanjang.setTooltiptext("Batal Perpanjang");
			batalPerpanjang.setDisabled(
					kembaliPengadaanItemDetail.getKembaliPengadaanItem().getDisetujuiOleh() != null || !edit);
			batalPerpanjang.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					LibraryUtil.onBatalPerpanjang(kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail());
					tanggalEventListener.onEvent(event);
					batalPerpanjang.setVisible(
							kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail().getJumlahPerpanjangan() > 0);
				}

			});
			aksiButtons.add(batalPerpanjang);

			batalPerpanjang.setVisible(
					kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail().getJumlahPerpanjangan() > 0);

			ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);
		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<KembaliPengadaanItemDetail> kembaliPengadaanItemDetails = session
				.createCriteria(KembaliPengadaanItemDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("kembaliPengadaanItem", kembaliPengadaanItem)).list();

		ListModel strset = new SimpleListModel(kembaliPengadaanItemDetails);
		grid.setRowRenderer(new KembaliPengadaanItemDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item Kembali");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/ISBN/ISSN");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Barcode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Perpanjang");
		column.setWidth("6%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Batas kembali");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Terlambat");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal Kembali");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Denda");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);
	}

}
