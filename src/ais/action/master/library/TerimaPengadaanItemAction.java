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
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.helper.AmbilDataTransferPengadaanItemBanbox;
import ais.action.master.library.helper.TerimaPengadaanItemDetailAction;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.action.report.format1.library.LaporanTerima;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.TerimaPengadaanItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.TerimaPengadaanItem;
import ais.database.model.library.TerimaPengadaanItemDetail;
import ais.database.model.library.TransferPengadaanItem;
import ais.database.model.library.TransferPengadaanItemDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk terima pengadaan item. Tipe ini merupakan titik masuk UI yang
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
public class TerimaPengadaanItemAction extends GenericAutowireComposer {

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
	private AmbilDataTransferPengadaanItemBanbox transferPengadaanItem;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private Perpustakaan currentPerpustakaan;

	private TerimaPengadaanItem terimaPengadaanItem;
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

		searchperpustakaan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

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
	}

	public void onCetak(Event event) throws Exception {
		LaporanTerima laporan = new LaporanTerima();
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cetak(TerimaPengadaanItem terimaPengadaanItem) throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();

		Common.insertProperty(TerimaPengadaanItem.class, terimaPengadaanItem, parameters, "");

		Session session = HibernateUtil.currentSession();
		List<TerimaPengadaanItemDetail> terimaPengadaanItemDetails = session
				.createCriteria(TerimaPengadaanItemDetail.class)
				.add(Restrictions.eq("terimaPengadaanItem", terimaPengadaanItem)).list();
		List<Map> maps = new ArrayList<Map>();
		for (TerimaPengadaanItemDetail terimaPengadaanItemDetail : terimaPengadaanItemDetails) {
			Map map = new HashMap();
			Common.insertProperty(TerimaPengadaanItemDetail.class, terimaPengadaanItemDetail, map, "");
			map.put("isbn", terimaPengadaanItemDetail.getItem().getIsbn());
			map.put("isbn10", terimaPengadaanItemDetail.getItem().getIsbn10());
			map.put("nama", terimaPengadaanItemDetail.getItem().getNama());
			map.put("perpustakaan_asal",
					terimaPengadaanItem.getTransferPengadaanItem() == null
							|| terimaPengadaanItem.getTransferPengadaanItem().getPerpustakaan() == null ? ""
									: terimaPengadaanItem.getTransferPengadaanItem().getPerpustakaan().getNama());
			map.put("kode", terimaPengadaanItem.getKode());
			map.put("status_persetujuan",
					terimaPengadaanItem.getDisetujuiOleh() == null ? "Belum disetujui"
							: "Disetujui oleh " + terimaPengadaanItem.getDisetujuiOleh().getUserNama() + " pada "
									+ (terimaPengadaanItem.getTanggalPersetujuan() == null ? ""
											: Common.dateFormat1.get().format(terimaPengadaanItem.getTanggalPersetujuan())));
			map.put("perpustakaan", terimaPengadaanItem.getPerpustakaan() == null ? ""
					: terimaPengadaanItem.getPerpustakaan().getNama());
			map.put("disetujui_oleh", terimaPengadaanItem.getDisetujuiOleh() == null ? ""
					: terimaPengadaanItem.getDisetujuiOleh().getNama());

			map.put("barcode", terimaPengadaanItemDetail.getItemPunyaBarcode() == null ? ""
					: terimaPengadaanItemDetail.getItemPunyaBarcode().getBarcode());

			map.put("tanggal_persetujuan", terimaPengadaanItem.getTanggalPersetujuan());

			maps.add(map);
		}
		parameters.put("maps", maps);

		parameters.put("id", terimaPengadaanItem.getId());

		Report.generatePDFReport(Report.PDF, parameters, "library/terima_pengadaan",
				terimaPengadaanItem.getTanggalPembuatan());
	}

	class TerimaPengadaanItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final TerimaPengadaanItem terimaPengadaanItem = (TerimaPengadaanItem) arg1;

			final TerimaPengadaanItemDetailAction detail;
			(detail = new TerimaPengadaanItemDetailAction(terimaPengadaanItem)).setParent(arg0);

			RevisiHelper.createNewRevisi(TerimaPengadaanItem.class, terimaPengadaanItem, terimaPengadaanItem.getKode())
					.setParent(arg0);

			new Label(terimaPengadaanItem.getTransferPengadaanItem().getPerpustakaan() == null ? ""
					: terimaPengadaanItem.getTransferPengadaanItem().getPerpustakaan().getNama()).setParent(arg0);

			new Label(terimaPengadaanItem.getTransferPengadaanItem().getPerpustakaanTujuan() == null ? ""
					: terimaPengadaanItem.getTransferPengadaanItem().getPerpustakaanTujuan().getNama()).setParent(arg0);

			new Label(terimaPengadaanItem.getDibuatOleh() == null ? ""
					: terimaPengadaanItem.getDibuatOleh().getUserNama()).setParent(arg0);
			new Label(terimaPengadaanItem.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(terimaPengadaanItem.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(terimaPengadaanItem.getDisetujuiOleh() == null ? ""
					: terimaPengadaanItem.getDisetujuiOleh().getUserNama())).setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(terimaPengadaanItem.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(terimaPengadaanItem.getTanggalPersetujuan()))).setParent(arg0);
			new Label(terimaPengadaanItem.getKeterangan()).setParent(arg0);

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons = new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Terima Pengadaan Item");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					cetak(terimaPengadaanItem);
				}

			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && terimaPengadaanItem.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && terimaPengadaanItem.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Terima Pengadaan Item ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();
										Integer countItemjumlah = ((Number) session
												.createCriteria(TerimaPengadaanItemDetail.class)
												.setProjection(Projections.count("id"))
												.add(Restrictions.eq("terimaPengadaanItem", terimaPengadaanItem))
												.add(Restrictions.lt("diterima", 1.0)).uniqueResult()).intValue();

										if (!countItemjumlah.equals(0)) {
											MyMessageboxConfig.show("Lengkapilah diterima !", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}

										terimaPengadaanItem.setDisetujuiOleh(Common.getCurrentUser());
										terimaPengadaanItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, terimaPengadaanItem);

										TransferPengadaanItem transferPengadaanItem = terimaPengadaanItem
												.getTransferPengadaanItem();
										session.refresh(transferPengadaanItem);
										transferPengadaanItem.setTerimaPengadaanItem(terimaPengadaanItem);
										Common.refreshUpdate(session, transferPengadaanItem);

										List<TerimaPengadaanItemDetail> terimaPengadaanItemDetails = session
												.createCriteria(TerimaPengadaanItemDetail.class)
												.add(Restrictions.eq("terimaPengadaanItem", terimaPengadaanItem))
												.list();

										session.createSQLQuery(
												"delete from library.detail_transaksi where terima_pengadaan_item_detail in (select id from library.terima_pengadaan_item_detail where terima_pengadaan_item = "
														+ terimaPengadaanItem.getId() + ");")
												.executeUpdate();
										for (TerimaPengadaanItemDetail terimaPengadaanItemDetail : terimaPengadaanItemDetails) {
											DetailTransaksi detailTransaksi = new DetailTransaksi();
											detailTransaksi.setTerimaPengadaanItemDetail(terimaPengadaanItemDetail);
											detailTransaksi.setQtyBonus(0.0);

											detailTransaksi.setItemPunyaBarcode(
													terimaPengadaanItemDetail.getItemPunyaBarcode());
											detailTransaksi.setItem(terimaPengadaanItemDetail.getItem());
											detailTransaksi.setKeterangan("Transaksi Terima Pengadaan");
											detailTransaksi.setKodeTransaksi(LibraryUtil.TERIMA);
											detailTransaksi.setPerpustakaan(terimaPengadaanItem
													.getTransferPengadaanItem().getPerpustakaanTujuan());
											detailTransaksi.setQty(terimaPengadaanItemDetail.getDiterima());
											detailTransaksi.setTanggal(terimaPengadaanItem.getTanggalPembuatan());

											detailTransaksi
													.setTanggalDanWaktu(terimaPengadaanItem.getTanggalPembuatan());

											session.save(detailTransaksi);

											ItemPunyaBarcode itemPunyaBarcode = terimaPengadaanItemDetail
													.getItemPunyaBarcode();
											itemPunyaBarcode.setPerpustakaan(terimaPengadaanItem.getPerpustakaan());
											Common.refreshUpdate(session, itemPunyaBarcode);
										}

										disetujuiTanggal
												.setValue(terimaPengadaanItem.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(terimaPengadaanItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(terimaPengadaanItem.getDisetujuiOleh() == null ? ""
												: terimaPengadaanItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && terimaPengadaanItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && terimaPengadaanItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && terimaPengadaanItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && terimaPengadaanItem.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										final Timer timer = new Timer(500);
										timer.setParent(page.getFirstRoot());
										timer.addEventListener("onTimer", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												cetak(terimaPengadaanItem);
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

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Terima Pengadaan Item ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										terimaPengadaanItem.setDisetujuiOleh(null);
										terimaPengadaanItem.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, terimaPengadaanItem);

										TransferPengadaanItem transferPengadaanItem = terimaPengadaanItem
												.getTransferPengadaanItem();
										session.refresh(transferPengadaanItem);
										transferPengadaanItem.setTerimaPengadaanItem(null);
										Common.refreshUpdate(session, transferPengadaanItem);

										session.createSQLQuery(
												"delete from library.detail_transaksi where terima_pengadaan_item_detail in (select id from library.terima_pengadaan_item_detail where terima_pengadaan_item = "
														+ terimaPengadaanItem.getId() + ");")
												.executeUpdate();

										disetujuiTanggal
												.setValue(terimaPengadaanItem.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(terimaPengadaanItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(terimaPengadaanItem.getDisetujuiOleh() == null ? ""
												: terimaPengadaanItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && terimaPengadaanItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && terimaPengadaanItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && terimaPengadaanItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && terimaPengadaanItem.getDisetujuiOleh() == null);
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
			rubah.setVisible(edit && terimaPengadaanItem.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(terimaPengadaanItem);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && terimaPengadaanItem.getDisetujuiOleh() == null);
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

											TerimaPengadaanItemDao terimaPengadaanItemDao = DaoFactory.getInstance()
													.getTerimaPengadaanItemDao();

											Session session = terimaPengadaanItemDao.getCurrentSession();
											List<TerimaPengadaanItemDetail> terimaPengadaanItemDetails = session
													.createCriteria(TerimaPengadaanItemDetail.class)
													.add(Restrictions.eq("terimaPengadaanItem", terimaPengadaanItem))
													.list();
											for (TerimaPengadaanItemDetail terimaPengadaanItemDetail : terimaPengadaanItemDetails) {
												session.delete(terimaPengadaanItemDetail);
											}

											Common.refreshDelete(terimaPengadaanItem);

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
		init(new TerimaPengadaanItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	public void generateDetail(TerimaPengadaanItem terimaPengadaanItem) {
		Session session = HibernateUtil.currentSession();
		List<TransferPengadaanItemDetail> transferPengadaanItemDetails = session
				.createCriteria(TransferPengadaanItemDetail.class)
				.add(Restrictions.eq("transferPengadaanItem", terimaPengadaanItem.getTransferPengadaanItem())).list();

		for (TransferPengadaanItemDetail transferPengadaanItemDetail : transferPengadaanItemDetails) {
			TerimaPengadaanItemDetail terimaPengadaanItemDetail = new TerimaPengadaanItemDetail();
			terimaPengadaanItemDetail.setItemPunyaBarcode(transferPengadaanItemDetail.getItemPunyaBarcode());
			terimaPengadaanItemDetail.setItem(transferPengadaanItemDetail.getItem());
			terimaPengadaanItemDetail.setTransferPengadaanItemDetail(transferPengadaanItemDetail);
			terimaPengadaanItemDetail.setDiterima(1.0);
			terimaPengadaanItemDetail.setKeterangan(transferPengadaanItemDetail.getKeterangan());
			terimaPengadaanItemDetail.setTerimaPengadaanItem(terimaPengadaanItem);
			session.save(terimaPengadaanItemDetail);
		}
	}

	private void init(TerimaPengadaanItem terimaPengadaanItem) throws Exception {
		this.terimaPengadaanItem = terimaPengadaanItem;
		addWindow.setTitle(terimaPengadaanItem.getId() == null ? "Tambah Terima Pengadaan Item" : "Ubah Terima Pengadaan Item");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Terima Pengadaan Item"));
		String mykode = terimaPengadaanItem.getKode();

		row.appendChild(
				kode = new MyTextbox(terimaPengadaanItem.getKode() == null ? mykode : terimaPengadaanItem.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan"));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				terimaPengadaanItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: terimaPengadaanItem.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());

		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Data Transfer"));
		row.appendChild(transferPengadaanItem = new AmbilDataTransferPengadaanItemBanbox());
		transferPengadaanItem.setWidth("90%");
		transferPengadaanItem.setAttribute("transferPengadaanItem", terimaPengadaanItem.getTransferPengadaanItem());
		transferPengadaanItem.setValue(terimaPengadaanItem.getTransferPengadaanItem() == null ? ""
				: terimaPengadaanItem.getTransferPengadaanItem().toString());
		transferPengadaanItem.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				TransferPengadaanItem mytransferPengadaanItem = (TransferPengadaanItem) transferPengadaanItem
						.getAttribute("transferPengadaanItem");
				currentPerpustakaan = mytransferPengadaanItem.getPerpustakaanTujuan();
				String mykode = LibraryUtil.generateCode(TerimaPengadaanItem.class, 8, "TRM", currentPerpustakaan);
				kode.setValue(mykode);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox(
				terimaPengadaanItem.getKeterangan() == null ? "" : terimaPengadaanItem.getKeterangan()));
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

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Terima harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (transferPengadaanItem.getAttribute("transferPengadaanItem") == null) {
			MyMessageboxConfig.show("Data transfer harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		TerimaPengadaanItemDao terimaPengadaanItemDao = DaoFactory.getInstance().getTerimaPengadaanItemDao();
		if (terimaPengadaanItem.getId() != null) {
			terimaPengadaanItem = terimaPengadaanItemDao.load(terimaPengadaanItem.getId());

		}

		terimaPengadaanItem.setTransferPengadaanItem(
				(TransferPengadaanItem) transferPengadaanItem.getAttribute("transferPengadaanItem"));
		terimaPengadaanItem.setPerpustakaan(terimaPengadaanItem.getTransferPengadaanItem().getPerpustakaanTujuan());
		terimaPengadaanItem.setKode(kode.getValue());
		terimaPengadaanItem.setKeterangan(keterangan.getValue());
		terimaPengadaanItem.setTanggalPembuatan(tanggalPembuatan.getValue());

		if (terimaPengadaanItem.getId() != null) {
			terimaPengadaanItemDao.update(terimaPengadaanItem);
		} else {
			terimaPengadaanItem.setDibuatOleh(Common.getCurrentUser());

			currentPerpustakaan = terimaPengadaanItem.getTransferPengadaanItem().getPerpustakaanTujuan();
			terimaPengadaanItem.setIndex(
					LibraryUtil.generateMaxByPerpustakaan(TerimaPengadaanItem.class, currentPerpustakaan) + 1);
			String mykode = LibraryUtil.generateCode(TerimaPengadaanItem.class, 8, "TRM", currentPerpustakaan);
			kode.setValue(mykode);
			terimaPengadaanItem.setKode(mykode);
			terimaPengadaanItemDao.save(terimaPengadaanItem);

			generateDetail(terimaPengadaanItem);
		}
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TerimaPengadaanItem.class)
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
		List<TerimaPengadaanItem> terimaPengadaanItem = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(terimaPengadaanItem);
		grid.setRowRenderer(new TerimaPengadaanItemRenderer());
		grid.setModelCheckMobile(strset);

	}

}
