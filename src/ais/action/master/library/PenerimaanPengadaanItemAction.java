package ais.action.master.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataPemesananPengadaanBanbox;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.helper.PenerimaanPengadaanItemDetailAction;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.action.report.format1.library.LaporanPenerimaan;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.PenerimaanPengadaanItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.PemesananPengadaanItem;
import ais.database.model.library.PemesananPengadaanItemDetail;
import ais.database.model.library.PenerimaanPengadaanItem;
import ais.database.model.library.PenerimaanPengadaanItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk penerimaan pengadaan item. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code MyGrid
 * grid}, {@code Paging paging}, {@code Textbox searchkode}, {@code AmbilDataPerpustakaanBanbox
 * searchperpustakaan}, {@code MyTextbox kode}, {@code MyTextbox keterangan}, {@code MyDatebox tanggalPembuatan};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()});
 * pelaporan/ekspor ({@code onCetak()}, {@code cetak()}); operasi domain lain ({@code onAdd()}, {@code
 * generateDetail()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class PenerimaanPengadaanItemAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchkode;
	private AmbilDataPerpustakaanBanbox searchperpustakaan;

	private MyTextbox kode;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;
	private AmbilDataPemesananPengadaanBanbox pemesananPengadaanItem;
	private Label penyedia;
	private Label perpustakaan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private Perpustakaan currentPerpustakaan;

	private PenerimaanPengadaanItem penerimaanPengadaanItem;
	private MyToolbarbuttonConfig add;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchperpustakaan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		// MyToolbarbuttonConfig barcode = new MyToolbarbuttonConfig("Cetak Semua
		// Barcode",
		// "/img/album.png");
		// barcode.setParent(add.getParent());
		// barcode.addEventListener("onClick", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		//
		// generateBarcode();
		//
		// }
		// });
	}

	// protected void generateBarcode() {
	// Common.createDefaultTimer(new EventListener() {
	//
	// @Override
	// public void onEvent(Event arg0) throws Exception {
	// Tbmuser dibuatOleh = Common.getTbmuser();
	// Session session = HibernateUtil.currentNativeSession();
	// @SuppressWarnings("unchecked")
	// List<PenerimaanPengadaanItemDetail> penerimaanPengadaanItemDetails =
	// session
	// .createCriteria(PenerimaanPengadaanItemDetail.class)
	// .createAlias("penerimaanPengadaanItem", "penerimaanPengadaanItem")
	// .add(Restrictions.isNotNull("penerimaanPengadaanItem.disetujuiOleh"))
	// .add(Restrictions.isNotNull("penerimaanPengadaanItem.tanggalPersetujuan"))
	// .addOrder(Order.desc("id")).list();
	// for (PenerimaanPengadaanItemDetail penerimaanPengadaanItemDetail :
	// penerimaanPengadaanItemDetails) {
	// System.out.println("penerimaanPengadaanItemDetail => " +
	// penerimaanPengadaanItemDetail);
	// BatchItemPunyaBarcode batchItemPunyaBarcode = (BatchItemPunyaBarcode)
	// session
	// .createCriteria(BatchItemPunyaBarcode.class)
	// .add(Restrictions.eq("penerimaanPengadaanItem",
	// penerimaanPengadaanItemDetail.getPenerimaanPengadaanItem()))
	// .add(Restrictions.eq("item",
	// penerimaanPengadaanItemDetail.getItem())).setMaxResults(1)
	// .uniqueResult();
	// if (batchItemPunyaBarcode == null) {
	// batchItemPunyaBarcode = new BatchItemPunyaBarcode();
	// batchItemPunyaBarcode.setBerasalDari(BatchItemPunyaBarcode.PEMBELIAN);
	// batchItemPunyaBarcode.setDibuatOleh(dibuatOleh);
	// batchItemPunyaBarcode.setItem(penerimaanPengadaanItemDetail.getItem());
	// batchItemPunyaBarcode
	// .setPenerimaanPengadaanItem(penerimaanPengadaanItemDetail.getPenerimaanPengadaanItem());
	// batchItemPunyaBarcode.setTanggal(
	// penerimaanPengadaanItemDetail.getPenerimaanPengadaanItem().getTanggalPersetujuan());
	//
	// session.getTransaction().begin();
	// Common.refreshSaveOrUpdate(session, batchItemPunyaBarcode);
	// session.getTransaction().commit();
	// }
	//
	// penerimaanPengadaanItemDetail.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
	// session.getTransaction().begin();
	// Common.refreshSaveOrUpdate(session, penerimaanPengadaanItemDetail);
	// session.getTransaction().commit();
	//
	// Item item = penerimaanPengadaanItemDetail.getItem();
	//
	// int qtyTotal = ((Number) session.createCriteria(ItemPunyaBarcode.class)
	// .setProjection(Projections.rowCount())
	// .add(Restrictions.eq("batchItemPunyaBarcode", batchItemPunyaBarcode))
	// .add(Restrictions.eq("item",
	// item)).setMaxResults(1).uniqueResult()).intValue();
	// int jumlahItem = penerimaanPengadaanItemDetail.getJumlah().intValue();
	// boolean generateUlang = qtyTotal < jumlahItem;
	// System.out.println("item => " + item + ", qtyTotal = " + qtyTotal + ",
	// jumlahItem = " + jumlahItem
	// + ", generateUlang = " + generateUlang);
	// if (generateUlang) {
	// for (int i = 0; i < jumlahItem; i++) {
	// int qty = ((Number) session.createCriteria(ItemPunyaBarcode.class)
	// .setProjection(Projections.rowCount())
	// .add(Restrictions.eq("batchItemPunyaBarcode", batchItemPunyaBarcode))
	// .add(Restrictions.eq("item", item)).add(Restrictions.eq("indexke", i))
	// .setMaxResults(1).uniqueResult()).intValue();
	// if (qty == 0) {
	// ItemPunyaBarcode itemPunyaBarcode = new ItemPunyaBarcode();
	// itemPunyaBarcode.setIndexke(i);
	// itemPunyaBarcode.setItem(item);
	// itemPunyaBarcode.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
	// itemPunyaBarcode.setBarcode(BarcodeCommon.generateCode(batchItemPunyaBarcode));
	// itemPunyaBarcode.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
	// itemPunyaBarcode.setPerpustakaan(
	// penerimaanPengadaanItemDetail.getPenerimaanPengadaanItem().getPerpustakaan());
	//
	// session.getTransaction().begin();
	// Common.refreshSaveOrUpdate(session, itemPunyaBarcode);
	// session.getTransaction().commit();
	// }
	// }
	// }
	// }
	//
	// HibernateUtil.closeSession();
	//
	// LaporanBarcodeSemuaPenerimaanPengadaanItem laporanBarcodeItem = new
	// LaporanBarcodeSemuaPenerimaanPengadaanItem();
	// laporanBarcodeItem.setTitle("Cetak Barcode");
	// ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanBarcodeItem);
	// laporanBarcodeItem.setHeight("95%");
	// laporanBarcodeItem.setWidth("90%");
	// laporanBarcodeItem.setClosable(true);
	// laporanBarcodeItem.onModal();
	// }
	// });
	//
	// }

	public void onCetak(Event event) throws Exception {
		LaporanPenerimaan laporan = new LaporanPenerimaan();
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cetak(PenerimaanPengadaanItem penerimaanPengadaanItem) throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();

		Common.insertProperty(PenerimaanPengadaanItem.class, penerimaanPengadaanItem, parameters, "");

		Session session = HibernateUtil.currentSession();
		List<PenerimaanPengadaanItemDetail> penerimaanPengadaanItemDetails = session
				.createCriteria(PenerimaanPengadaanItemDetail.class)
				.add(Restrictions.eq("penerimaanPengadaanItem", penerimaanPengadaanItem)).list();
		List<Map> maps = new ArrayList<Map>();
		for (PenerimaanPengadaanItemDetail penerimaanPengadaanItemDetail : penerimaanPengadaanItemDetails) {
			Map map = new HashMap();
			Common.insertProperty(PenerimaanPengadaanItemDetail.class, penerimaanPengadaanItemDetail, map, "");
			map.put("isbn", penerimaanPengadaanItemDetail.getItem().getIsbn());
			map.put("isbn10", penerimaanPengadaanItemDetail.getItem().getIsbn10());
			map.put("nama", penerimaanPengadaanItemDetail.getItem().getNama());
			map.put("penyedia", penerimaanPengadaanItem.getPenyedia() == null ? ""
					: penerimaanPengadaanItem.getPenyedia().getNama());
			map.put("jumlah", penerimaanPengadaanItemDetail.getJumlah());
			map.put("kode", penerimaanPengadaanItem.getKode());
			map.put("status_persetujuan", penerimaanPengadaanItem.getDisetujuiOleh() == null ? "Belum disetujui"
					: "Disetujui oleh " + penerimaanPengadaanItem.getDisetujuiOleh().getUserNama() + " pada "
							+ (penerimaanPengadaanItem.getTanggalPersetujuan() == null ? ""
									: Common.dateFormat1.get().format(penerimaanPengadaanItem.getTanggalPersetujuan())));
			map.put("perpustakaan", penerimaanPengadaanItem.getPerpustakaan() == null ? ""
					: penerimaanPengadaanItem.getPerpustakaan().getNama());
			map.put("disetujui_oleh", penerimaanPengadaanItem.getDisetujuiOleh() == null ? ""
					: penerimaanPengadaanItem.getDisetujuiOleh().getNama());

			map.put("tanggal_persetujuan", penerimaanPengadaanItem.getTanggalPersetujuan());

			maps.add(map);
		}
		parameters.put("maps", maps);

		parameters.put("id", penerimaanPengadaanItem.getId());
		Report.generatePDFReport(Report.PDF, parameters, "library/penerimaan_pengadaan",
				penerimaanPengadaanItem.getTanggalPembuatan());
	}

	class PenerimaanPengadaanItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenerimaanPengadaanItem penerimaanPengadaanItem = (PenerimaanPengadaanItem) arg1;

			final PenerimaanPengadaanItemDetailAction detail;
			(detail = new PenerimaanPengadaanItemDetailAction(penerimaanPengadaanItem)).setParent(arg0);

			RevisiHelper.createNewRevisi(PenerimaanPengadaanItem.class, penerimaanPengadaanItem,
					penerimaanPengadaanItem.getKode()).setParent(arg0);

			new Label(penerimaanPengadaanItem.getPenyedia() == null ? ""
					: penerimaanPengadaanItem.getPenyedia().getNama()).setParent(arg0);

			new Label(penerimaanPengadaanItem.getPerpustakaan() == null ? ""
					: penerimaanPengadaanItem.getPerpustakaan().getNama()).setParent(arg0);

			new Label(penerimaanPengadaanItem.getDibuatOleh() == null ? ""
					: penerimaanPengadaanItem.getDibuatOleh().getUserNama()).setParent(arg0);
			new Label(penerimaanPengadaanItem.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(penerimaanPengadaanItem.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(penerimaanPengadaanItem.getDisetujuiOleh() == null ? ""
					: penerimaanPengadaanItem.getDisetujuiOleh().getUserNama())).setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(penerimaanPengadaanItem.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(penerimaanPengadaanItem.getTanggalPersetujuan()))).setParent(arg0);
			new Label(penerimaanPengadaanItem.getKeterangan()).setParent(arg0);

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons = new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Penerimaan Pengadaan Item");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(penerimaanPengadaanItem);
				}

			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && penerimaanPengadaanItem.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && penerimaanPengadaanItem.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Penerimaan Pengadaan Item ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();
										Integer countItemjumlah = ((Number) session
												.createCriteria(PenerimaanPengadaanItemDetail.class)
												.setProjection(Projections.count("id"))
												.add(Restrictions.eq("penerimaanPengadaanItem",
														penerimaanPengadaanItem))
												.add(Restrictions.lt("jumlah", 1.0)).uniqueResult()).intValue();

										if (!countItemjumlah.equals(0)) {
											MyMessageboxConfig.show("Lengkapilah jumlah !", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}

										countItemjumlah = ((Number) session
												.createCriteria(PenerimaanPengadaanItemDetail.class)
												.setProjection(Projections.count("id"))
												.add(Restrictions.eq("penerimaanPengadaanItem",
														penerimaanPengadaanItem))
												.add(Restrictions.lt("diterima", 1.0)).uniqueResult()).intValue();

										if (!countItemjumlah.equals(0)) {
											MyMessageboxConfig.show("Lengkapilah data diterima !", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}

										penerimaanPengadaanItem.setDisetujuiOleh(Common.getCurrentUser());
										penerimaanPengadaanItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, penerimaanPengadaanItem);

										List<PenerimaanPengadaanItemDetail> penerimaanPengadaanItemDetails = session
												.createCriteria(PenerimaanPengadaanItemDetail.class).add(Restrictions
														.eq("penerimaanPengadaanItem", penerimaanPengadaanItem))
												.list();

										session.createSQLQuery(
												"delete from library.detail_transaksi where penerimaan_pengadaan_item_detail in (select id from library.penerimaan_pengadaan_item_detail where penerimaan_pengadaan_item = "
														+ penerimaanPengadaanItem.getId() + ");")
												.executeUpdate();
										for (PenerimaanPengadaanItemDetail penerimaanPengadaanItemDetail : penerimaanPengadaanItemDetails) {
											DetailTransaksi detailTransaksi = new DetailTransaksi();
											detailTransaksi
													.setPenerimaanPengadaanItemDetail(penerimaanPengadaanItemDetail);
											detailTransaksi.setQtyBonus(0.0);

											detailTransaksi.setItem(penerimaanPengadaanItemDetail.getItem());
											detailTransaksi.setKeterangan("Transaksi Penerimaan Pengadaan");
											detailTransaksi.setKodeTransaksi(LibraryUtil.BELI_MASUK);
											detailTransaksi.setPerpustakaan(penerimaanPengadaanItem.getPerpustakaan());
											detailTransaksi.setQty(penerimaanPengadaanItemDetail.getDiterima());
											detailTransaksi.setTanggal(penerimaanPengadaanItem.getTanggalPersetujuan());
											detailTransaksi.setTanggalDanWaktu(
													penerimaanPengadaanItem.getTanggalPersetujuan());

											session.save(detailTransaksi);
										}

										disetujuiTanggal
												.setValue(penerimaanPengadaanItem.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																penerimaanPengadaanItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(penerimaanPengadaanItem.getDisetujuiOleh() == null ? ""
												: penerimaanPengadaanItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && penerimaanPengadaanItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && penerimaanPengadaanItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && penerimaanPengadaanItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && penerimaanPengadaanItem.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										final Timer timer = new Timer(500);
										timer.setParent(page.getFirstRoot());
										timer.addEventListener("onTimer", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												cetak(penerimaanPengadaanItem);
											}
										});
										timer.start();
									}
								}
							});
				}

			});
			aksiButtons.add(disetujui);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Penerimaan Pengadaan Item ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										penerimaanPengadaanItem.setDisetujuiOleh(null);
										penerimaanPengadaanItem.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, penerimaanPengadaanItem);

										session.createSQLQuery(
												"delete from library.detail_transaksi where penerimaan_pengadaan_item_detail in (select id from library.penerimaan_pengadaan_item_detail where penerimaan_pengadaan_item = "
														+ penerimaanPengadaanItem.getId() + ");")
												.executeUpdate();

										disetujuiTanggal
												.setValue(penerimaanPengadaanItem.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																penerimaanPengadaanItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(penerimaanPengadaanItem.getDisetujuiOleh() == null ? ""
												: penerimaanPengadaanItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && penerimaanPengadaanItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && penerimaanPengadaanItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && penerimaanPengadaanItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && penerimaanPengadaanItem.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			aksiButtons.add(dibatalkan);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && penerimaanPengadaanItem.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(penerimaanPengadaanItem);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && penerimaanPengadaanItem.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											PenerimaanPengadaanItemDao penerimaanPengadaanItemDao = DaoFactory
													.getInstance().getPenerimaanPengadaanItemDao();

											Session session = penerimaanPengadaanItemDao.getCurrentSession();
											List<PenerimaanPengadaanItemDetail> penerimaanPengadaanItemDetails = session
													.createCriteria(PenerimaanPengadaanItemDetail.class)
													.add(Restrictions.eq("penerimaanPengadaanItem",
															penerimaanPengadaanItem))
													.list();
											for (PenerimaanPengadaanItemDetail penerimaanPengadaanItemDetail : penerimaanPengadaanItemDetails) {
												session.delete(penerimaanPengadaanItemDetail);
											}

											Common.refreshDelete(penerimaanPengadaanItem);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			aksiButtons.add(hapus);
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new PenerimaanPengadaanItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PenerimaanPengadaanItem penerimaanPengadaanItem) throws Exception {
		this.penerimaanPengadaanItem = penerimaanPengadaanItem;
		addWindow.setTitle(penerimaanPengadaanItem.getId() == null ? "Tambah Penerimaan Pengadaan Item" : "Ubah Penerimaan Pengadaan Item");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Penerimaan Pengadaan Item"));
		String mykode = penerimaanPengadaanItem.getKode();
		row.appendChild(kode = new MyTextbox(
				penerimaanPengadaanItem.getKode() == null ? mykode : penerimaanPengadaanItem.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pemesanan Pengadaan"));
		row.appendChild(pemesananPengadaanItem = new AmbilDataPemesananPengadaanBanbox());
		pemesananPengadaanItem.setWidth("90%");
		pemesananPengadaanItem.setAttribute("pemesananPengadaanItem",
				penerimaanPengadaanItem.getPemesananPengadaanItem());
		pemesananPengadaanItem.setValue(penerimaanPengadaanItem.getPemesananPengadaanItem() == null ? ""
				: penerimaanPengadaanItem.getPemesananPengadaanItem().toString());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyedia"));
		row.appendChild(penyedia = new Label(
				penerimaanPengadaanItem.getPenyedia() == null ? "" : penerimaanPengadaanItem.getPenyedia().getNama()));

		pemesananPengadaanItem.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				PemesananPengadaanItem mypemesananPengadaanItem = (PemesananPengadaanItem) pemesananPengadaanItem
						.getAttribute("pemesananPengadaanItem");
				penyedia.setValue(
						mypemesananPengadaanItem == null || mypemesananPengadaanItem.getPenyedia() == null ? ""
								: (mypemesananPengadaanItem).getPenyedia().getNama());
				perpustakaan.setValue(
						mypemesananPengadaanItem == null || mypemesananPengadaanItem.getPerpustakaan() == null ? ""
								: (mypemesananPengadaanItem).getPerpustakaan().getNama());

				currentPerpustakaan = mypemesananPengadaanItem.getPerpustakaan();
				String mykode = LibraryUtil.generateCode(PenerimaanPengadaanItem.class, 8, "OR", currentPerpustakaan);
				kode.setValue(mykode);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan"));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				penerimaanPengadaanItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: penerimaanPengadaanItem.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		;
		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new Label(penerimaanPengadaanItem.getPerpustakaan() == null ? ""
				: penerimaanPengadaanItem.getPerpustakaan().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox(
				penerimaanPengadaanItem.getKeterangan() == null ? "" : penerimaanPengadaanItem.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(4);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public void generateDetail(PenerimaanPengadaanItem penerimaanPengadaanItem) {
		Session session = HibernateUtil.currentSession();
		List<PemesananPengadaanItemDetail> pemesananPengadaanItemDetails = session
				.createCriteria(PemesananPengadaanItemDetail.class)
				.add(Restrictions.eq("pemesananPengadaanItem", penerimaanPengadaanItem.getPemesananPengadaanItem()))
				.list();

		for (PemesananPengadaanItemDetail pemesananPengadaanItemDetail : pemesananPengadaanItemDetails) {
			PenerimaanPengadaanItemDetail penerimaanPengadaanItemDetail = new PenerimaanPengadaanItemDetail();
			penerimaanPengadaanItemDetail.setItem(pemesananPengadaanItemDetail.getItem());
			penerimaanPengadaanItemDetail.setJumlah(pemesananPengadaanItemDetail.getJumlah());
			penerimaanPengadaanItemDetail.setDiterima(pemesananPengadaanItemDetail.getJumlah());
			penerimaanPengadaanItemDetail.setKeterangan(pemesananPengadaanItemDetail.getKeterangan());
			penerimaanPengadaanItemDetail.setPenerimaanPengadaanItem(penerimaanPengadaanItem);
			session.save(penerimaanPengadaanItemDetail);
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Penerimaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (pemesananPengadaanItem.getAttribute("pemesananPengadaanItem") == null) {
			MyMessageboxConfig.show("Pemesanan Pengadaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (keterangan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Keterangan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		PemesananPengadaanItem mypemesananPengadaanItem = (PemesananPengadaanItem) pemesananPengadaanItem
				.getAttribute("pemesananPengadaanItem");

		if (mypemesananPengadaanItem.getPenyedia() == null) {
			MyMessageboxConfig.show("Penyedia harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		PenerimaanPengadaanItemDao penerimaanPengadaanItemDao = DaoFactory.getInstance()
				.getPenerimaanPengadaanItemDao();
		if (penerimaanPengadaanItem.getId() != null) {
			penerimaanPengadaanItem = penerimaanPengadaanItemDao.load(penerimaanPengadaanItem.getId());

		}

		penerimaanPengadaanItem.setKode(kode.getValue());
		penerimaanPengadaanItem.setKeterangan(keterangan.getValue());
		penerimaanPengadaanItem.setTanggalPembuatan(tanggalPembuatan.getValue());
		penerimaanPengadaanItem.setPemesananPengadaanItem(
				(PemesananPengadaanItem) pemesananPengadaanItem.getAttribute("pemesananPengadaanItem"));
		penerimaanPengadaanItem.setPenyedia(mypemesananPengadaanItem.getPenyedia());
		penerimaanPengadaanItem.setPerpustakaan(mypemesananPengadaanItem.getPerpustakaan());

		if (penerimaanPengadaanItem.getId() != null) {
			penerimaanPengadaanItemDao.update(penerimaanPengadaanItem);
		} else {
			penerimaanPengadaanItem.setDibuatOleh(Common.getCurrentUser());

			currentPerpustakaan = penerimaanPengadaanItem.getPerpustakaan();
			penerimaanPengadaanItem.setIndex(
					LibraryUtil.generateMaxByPerpustakaan(PenerimaanPengadaanItem.class, currentPerpustakaan) + 1);
			String mykode = LibraryUtil.generateCode(PenerimaanPengadaanItem.class, 8, "OR", currentPerpustakaan);
			kode.setValue(mykode);
			penerimaanPengadaanItem.setKode(mykode);
			penerimaanPengadaanItemDao.save(penerimaanPengadaanItem);

			generateDetail(penerimaanPengadaanItem);
		}
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenerimaanPengadaanItem.class)
				.add((searchperpustakaan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchperpustakaan.getAttribute("perpustakaan") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("perpustakaan", searchperpustakaan.getAttribute("perpustakaan"))))
				.add(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PenerimaanPengadaanItem> penerimaanPengadaanItem = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penerimaanPengadaanItem);
		grid.setRowRenderer(new PenerimaanPengadaanItemRenderer());
		grid.setModelCheckMobile(strset);

	}

}
