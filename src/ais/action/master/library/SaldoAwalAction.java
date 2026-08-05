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
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.helper.SaldoAwalDetailAction;
import ais.action.master.library.helper.SaldoAwalPunyaItemHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.action.report.format1.library.LaporanSaldoAwal;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.SaldoAwalDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.SaldoAwal;
import ais.database.model.library.SaldoAwalDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class SaldoAwalAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchkode;
	private Textbox searchbarcode;
	private Textbox searchjudul;
	private AmbilDataPerpustakaanBanbox searchperpustakaan;

	private MyTextbox kode;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;
	private AmbilDataPerpustakaanBanbox perpustakaan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private Perpustakaan currentPerpustakaan;

	private SaldoAwal saldoAwal;
	private MyToolbarbuttonConfig add;
	private MyGrid gridItem;

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

		// MyToolbarbuttonConfig barcode = new MyToolbarbuttonConfig("Cetak
		// Semua Barcode",
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
	// List<SaldoAwalDetail> saldoAwalDetails =
	// session.createCriteria(SaldoAwalDetail.class)
	// .createAlias("saldoAwal",
	// "saldoAwal").add(Restrictions.isNotNull("saldoAwal.disetujuiOleh"))
	// .add(Restrictions.isNotNull("saldoAwal.tanggalPersetujuan")).addOrder(Order.desc("id")).list();
	// for (SaldoAwalDetail saldoAwalDetail : saldoAwalDetails) {
	// System.out.println("saldoAwalDetail => " + saldoAwalDetail);
	// BatchItemPunyaBarcode batchItemPunyaBarcode = (BatchItemPunyaBarcode)
	// session
	// .createCriteria(BatchItemPunyaBarcode.class)
	// .add(Restrictions.eq("saldoAwal", saldoAwalDetail.getSaldoAwal()))
	// .add(Restrictions.eq("item",
	// saldoAwalDetail.getItem())).setMaxResults(1).uniqueResult();
	// if (batchItemPunyaBarcode == null) {
	// batchItemPunyaBarcode = new BatchItemPunyaBarcode();
	// batchItemPunyaBarcode.setBerasalDari(BatchItemPunyaBarcode.SALDO_AWAL);
	// batchItemPunyaBarcode.setDibuatOleh(dibuatOleh);
	// batchItemPunyaBarcode.setItem(saldoAwalDetail.getItem());
	// batchItemPunyaBarcode.setSaldoAwal(saldoAwalDetail.getSaldoAwal());
	// batchItemPunyaBarcode.setTanggal(saldoAwalDetail.getSaldoAwal().getTanggalPersetujuan());
	//
	// session.getTransaction().begin();
	// Common.refreshSaveOrUpdate(session, batchItemPunyaBarcode);
	// session.getTransaction().commit();
	// }
	//
	// saldoAwalDetail.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
	// session.getTransaction().begin();
	// Common.refreshSaveOrUpdate(session, saldoAwalDetail);
	// session.getTransaction().commit();
	//
	// Item item = saldoAwalDetail.getItem();
	//
	// int qtyTotal = ((Number) session.createCriteria(ItemPunyaBarcode.class)
	// .setProjection(Projections.rowCount())
	// .add(Restrictions.eq("batchItemPunyaBarcode", batchItemPunyaBarcode))
	// .add(Restrictions.eq("item",
	// item)).setMaxResults(1).uniqueResult()).intValue();
	// int jumlahItem = saldoAwalDetail.getJumlah().intValue();
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
	// itemPunyaBarcode.setPerpustakaan(saldoAwalDetail.getSaldoAwal().getPerpustakaan());
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
	// LaporanBarcodeSemuaSaldoAwal laporanBarcodeItem = new
	// LaporanBarcodeSemuaSaldoAwal();
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
		LaporanSaldoAwal laporan = new LaporanSaldoAwal();
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cetak(SaldoAwal saldoAwal) throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();

		Common.insertProperty(SaldoAwal.class, saldoAwal, parameters, "");

		Session session = HibernateUtil.currentSession();
		List<SaldoAwalDetail> saldoAwalDetails = session.createCriteria(SaldoAwalDetail.class)
				.add(Restrictions.eq("saldoAwal", saldoAwal)).list();
		List<Map> maps = new ArrayList<Map>();
		for (SaldoAwalDetail saldoAwalDetail : saldoAwalDetails) {
			Map map = new HashMap();
			Common.insertProperty(SaldoAwalDetail.class, saldoAwalDetail, map, "");
			map.put("isbn", saldoAwalDetail.getItem().getIsbn());
			map.put("isbn10", saldoAwalDetail.getItem().getIsbn10());
			map.put("nama", saldoAwalDetail.getItem().getNama());

			map.put("jumlah", saldoAwalDetail.getJumlah());
			map.put("kode", saldoAwal.getKode());
			map.put("status_persetujuan",
					saldoAwal.getDisetujuiOleh() == null ? "Belum disetujui"
							: "Disetujui oleh " + saldoAwal.getDisetujuiOleh().getUserNama() + " pada "
									+ (saldoAwal.getTanggalPersetujuan() == null ? ""
											: Common.dateFormat1.get().format(saldoAwal.getTanggalPersetujuan())));
			map.put("perpustakaan", saldoAwal.getPerpustakaan() == null ? "" : saldoAwal.getPerpustakaan().getNama());
			map.put("disetujui_oleh",
					saldoAwal.getDisetujuiOleh() == null ? "" : saldoAwal.getDisetujuiOleh().getNama());

			map.put("tanggal_persetujuan", saldoAwal.getTanggalPersetujuan());

			maps.add(map);
		}
		parameters.put("maps", maps);

		parameters.put("id", saldoAwal.getId());
		Report.generatePDFReport(Report.PDF, parameters, "library/saldo_awal", saldoAwal.getTanggalPembuatan());
	}

	class SaldoAwalRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final SaldoAwal saldoAwal = (SaldoAwal) arg1;

			final SaldoAwalDetailAction detail;
			(detail = new SaldoAwalDetailAction(saldoAwal)).setParent(arg0);

			RevisiHelper.createNewRevisi(SaldoAwal.class, saldoAwal, saldoAwal.getKode()).setParent(arg0);

			new Label(saldoAwal.getPerpustakaan() == null ? "" : saldoAwal.getPerpustakaan().getNama()).setParent(arg0);

			new Label(saldoAwal.getDibuatOleh() == null ? "" : saldoAwal.getDibuatOleh().getUserNama()).setParent(arg0);
			new Label(saldoAwal.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(saldoAwal.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(
					saldoAwal.getDisetujuiOleh() == null ? "" : saldoAwal.getDisetujuiOleh().getUserNama()))
					.setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(saldoAwal.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(saldoAwal.getTanggalPersetujuan()))).setParent(arg0);
			new Label(saldoAwal.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Saldo Awal");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(saldoAwal);
				}

			});
			button.setParent(toolbar);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && saldoAwal.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && saldoAwal.getDisetujuiOleh() != null
					&& !saldoAwal.getKode().toLowerCase().contains("saldo_awal"));

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Saldo Awal ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings({ "unchecked" })
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();
										// Integer countItemjumlah = ((Number)
										// session.createCriteria(SaldoAwalDetail.class)
										// .setProjection(Projections.rowCount())
										// .add(Restrictions.eq("saldoAwal", saldoAwal))
										// .add(Restrictions.lt("jumlah",
										// 1.0)).uniqueResult()).intValue();
										//
										// if (!countItemjumlah.equals(0)) {
										// MyMessageboxConfig.show("Lengkapilah jumlah
										// !", "Peringatan", MyMessageboxConfig.OK,
										// MyMessageboxConfig.EXCLAMATION);
										// return;
										// }

										saldoAwal.setDisetujuiOleh(Common.getCurrentUser());
										saldoAwal.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, (saldoAwal));

										List<SaldoAwalDetail> saldoAwalDetails = session
												.createCriteria(SaldoAwalDetail.class)
												.add(Restrictions.eq("saldoAwal", saldoAwal)).list();

										session.createSQLQuery(
												"delete from library.detail_transaksi where saldo_awal_detail in (select id from library.saldo_awal_detail where saldo_awal = "
														+ saldoAwal.getId()
														+ " and (data_per_item is null or data_per_item = false));")
												.executeUpdate();
										for (SaldoAwalDetail saldoAwalDetail : saldoAwalDetails) {
											if (saldoAwalDetail.getDataPerItem() != null
													&& saldoAwalDetail.getDataPerItem()) {
												continue;
											}
											DetailTransaksi detailTransaksi = new DetailTransaksi();
											detailTransaksi.setSaldoAwalDetail(saldoAwalDetail);
											detailTransaksi.setQtyBonus(0.0);

											detailTransaksi.setItem(saldoAwalDetail.getItem());
											detailTransaksi.setKeterangan("Transaksi Saldo Awal");
											detailTransaksi.setKodeTransaksi(LibraryUtil.SALDO_AWAL);
											detailTransaksi.setPerpustakaan(saldoAwal.getPerpustakaan());
											detailTransaksi.setQty(saldoAwalDetail.getJumlah());
											detailTransaksi.setTanggal(saldoAwal.getTanggalPembuatan());
											detailTransaksi.setTanggalDanWaktu(saldoAwal.getTanggalPembuatan());

											session.save(detailTransaksi);
										}

										disetujuiTanggal.setValue(saldoAwal.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(saldoAwal.getTanggalPersetujuan()));
										disetujuiOleh.setValue(saldoAwal.getDisetujuiOleh() == null ? ""
												: saldoAwal.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && saldoAwal.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && saldoAwal.getDisetujuiOleh() != null);
										rubah.setVisible(edit && saldoAwal.getDisetujuiOleh() == null);
										hapus.setVisible(delete && saldoAwal.getDisetujuiOleh() == null
												&& !saldoAwal.getKode().toLowerCase().contains("saldo_awal"));
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										final Timer timer = new Timer(500);
										timer.setParent(page.getFirstRoot());
										timer.addEventListener("onTimer", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												cetak(saldoAwal);
												timer.detach();
											}
										});
										timer.start();
									}
								}
							});
				}

			});
			disetujui.setParent(toolbar);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Saldo Awal ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										saldoAwal.setDisetujuiOleh(null);
										saldoAwal.setTanggalPersetujuan(null);
										Common.refreshUpdate(session, (saldoAwal));

										session.createSQLQuery(
												"delete from library.detail_transaksi where saldo_awal_detail in (select id from library.saldo_awal_detail where saldo_awal = "
														+ saldoAwal.getId()
														+ " and (data_per_item is null or data_per_item = false) );")
												.executeUpdate();

										disetujuiTanggal.setValue(saldoAwal.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(saldoAwal.getTanggalPersetujuan()));
										disetujuiOleh.setValue(saldoAwal.getDisetujuiOleh() == null ? ""
												: saldoAwal.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && saldoAwal.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && saldoAwal.getDisetujuiOleh() != null);
										rubah.setVisible(edit && saldoAwal.getDisetujuiOleh() == null);
										hapus.setVisible(delete && saldoAwal.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			dibatalkan.setParent(toolbar);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && saldoAwal.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(saldoAwal);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(
					delete && saldoAwal.getDisetujuiOleh() == null && !saldoAwal.getKeterangan().equalsIgnoreCase(
							"SALDO_AWAL_OTOTAMIS_IMPORT-Data ini merupakan data yang berisi daftar buku yang otomatis diimport lewat excel"));
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

											SaldoAwalDao saldoAwalDao = DaoFactory.getInstance().getSaldoAwalDao();

											Session session = saldoAwalDao.getCurrentSession();
											List<SaldoAwalDetail> saldoAwalDetails = session
													.createCriteria(SaldoAwalDetail.class)
													.add(Restrictions.eq("saldoAwal", saldoAwal)).list();
											for (SaldoAwalDetail saldoAwalDetail : saldoAwalDetails) {
												session.delete(saldoAwalDetail);
											}

											Common.refreshDelete(saldoAwal);

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
			hapus.setParent(toolbar);
			toolbar.setParent(arg0);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new SaldoAwal());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final SaldoAwal saldoAwal, Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabDipinjam = new MyTabConfig("Item Saldo Awal");
		tabDipinjam.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDipinjam = new ais.ui.util.MyTabpanel();
		tabpanelDipinjam.setParent(tabpanels);
		tabpanelDipinjam.setWidth("100%");

		tabpanelDipinjam.appendChild(new SaldoAwalPunyaItemHelper(gridItem = new MyGrid()).initDetail(saldoAwal));

	}

	private void init(SaldoAwal saldoAwal) throws Exception {
		this.saldoAwal = saldoAwal;
		addWindow.setTitle(saldoAwal.getId() == null ? "Tambah Saldo Awal" : "Ubah Saldo Awal");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("70%");

		initDetail(saldoAwal, east);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Saldo Awal"));
		String mykode = saldoAwal.getKode();

		row.appendChild(kode = new MyTextbox(saldoAwal.getKode() == null ? mykode : saldoAwal.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan"));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				saldoAwal.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: saldoAwal.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());

		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new AmbilDataPerpustakaanBanbox());
		perpustakaan.setAttribute("perpustakaan", saldoAwal.getPerpustakaan());
		perpustakaan.setValue(saldoAwal.getPerpustakaan() == null ? "" : saldoAwal.getPerpustakaan().toString());
		perpustakaan.setWidth("90%");
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				currentPerpustakaan = (Perpustakaan) perpustakaan.getAttribute("perpustakaan");
				String mykode = LibraryUtil.generateCode(SaldoAwal.class, 8, "AW", currentPerpustakaan);
				kode.setValue(mykode);
			}
		};
		perpustakaan.setEventListener(eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox(saldoAwal.getKeterangan() == null ? "" : saldoAwal.getKeterangan()));
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
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Permintaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (perpustakaan.getAttribute("perpustakaan") == null) {
			MyMessageboxConfig.show("Perpustakaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (keterangan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Keterangan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rowsItem = gridItem.getRows().getChildren();
		for (Row row : rowsItem) {
			SaldoAwalDetail saldoAwalDetail = (SaldoAwalDetail) row.getAttribute("saldoAwalDetail");
			if (saldoAwalDetail.getItem() == null) {
				MyMessageboxConfig.show("Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		SaldoAwalDao saldoAwalDao = DaoFactory.getInstance().getSaldoAwalDao();
		if (saldoAwal.getId() != null) {
			saldoAwal = saldoAwalDao.load(saldoAwal.getId());

		}

		saldoAwal.setPerpustakaan((Perpustakaan) perpustakaan.getAttribute("perpustakaan"));
		saldoAwal.setKode(kode.getValue());
		saldoAwal.setKeterangan(keterangan.getValue());
		saldoAwal.setTanggalPembuatan(tanggalPembuatan.getValue());

		if (saldoAwal.getId() != null) {
			saldoAwalDao.update(saldoAwal);
		} else {
			saldoAwal.setDibuatOleh(Common.getCurrentUser());

			currentPerpustakaan = (Perpustakaan) (perpustakaan.getAttribute("perpustakaan"));
			saldoAwal.setIndex(LibraryUtil.generateMaxByPerpustakaan(SaldoAwal.class, currentPerpustakaan) + 1);
			String mykode = LibraryUtil.generateCode(SaldoAwal.class, 8, "AW", currentPerpustakaan);
			kode.setValue(mykode);
			saldoAwal.setKode(mykode);
			saldoAwalDao.save(saldoAwal);
		}

		Session session = saldoAwalDao.getCurrentSession();
		for (Row row : rowsItem) {
			SaldoAwalDetail saldoAwalDetail = (SaldoAwalDetail) row.getAttribute("saldoAwalDetail");
			saldoAwalDetail.setSaldoAwal(saldoAwal);
			session.saveOrUpdate(saldoAwalDetail);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(SaldoAwal.class);

		if ((searchbarcode != null && !searchbarcode.getValue().trim().isEmpty())
				|| (searchjudul != null && !searchjudul.getValue().trim().isEmpty())) {

			criteria = session.createCriteria(ItemPunyaBarcode.class).createAlias("item", "item")
					.add(searchbarcode == null || searchbarcode.getValue().trim().isEmpty()
							? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("barcode", searchbarcode.getValue().trim(), MatchMode.ANYWHERE))
					.add(searchjudul == null || searchjudul.getValue().trim().isEmpty()
							? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("item.isbn10", searchjudul.getValue().trim()),
									Restrictions.or(Restrictions.ilike("item.isbn", searchjudul.getValue().trim()),
											Restrictions.ilike("item.nama", searchjudul.getValue().trim(),
													MatchMode.ANYWHERE))))

					.createAlias("batchItemPunyaBarcode", "batchItemPunyaBarcode")
					.setProjection(Projections.groupProperty("batchItemPunyaBarcode.saldoAwal"))
					.createCriteria("batchItemPunyaBarcode.saldoAwal");
		} else if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.add((searchperpustakaan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchperpustakaan.getAttribute("perpustakaan") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("perpustakaan", searchperpustakaan.getAttribute("perpustakaan"))))
				.add(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<SaldoAwal> saldoAwal = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(saldoAwal);
		grid.setRowRenderer(new SaldoAwalRenderer());
		grid.setModelCheckMobile(strset);

	}

}
