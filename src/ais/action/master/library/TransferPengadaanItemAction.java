package ais.action.master.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import ais.action.master.library.helper.TransferPengadaanItemDetailAction;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.action.report.format1.library.LaporanTransfer;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.TransferPengadaanItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.TransferPengadaanItem;
import ais.database.model.library.TransferPengadaanItemDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TransferPengadaanItemAction extends GenericAutowireComposer {

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
	private AmbilDataPerpustakaanBanbox perpustakaan;
	private AmbilDataPerpustakaanBanbox perpustakaanTujuan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private Perpustakaan currentPerpustakaan;

	private TransferPengadaanItem transferPengadaanItem;
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
		LaporanTransfer laporan = new LaporanTransfer();
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cetak(TransferPengadaanItem transferPengadaanItem) throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();

		Common.insertProperty(TransferPengadaanItem.class, transferPengadaanItem, parameters, "");

		Session session = HibernateUtil.currentSession();
		List<TransferPengadaanItemDetail> transferPengadaanItemDetails = session
				.createCriteria(TransferPengadaanItemDetail.class)
				.add(Restrictions.eq("transferPengadaanItem", transferPengadaanItem)).list();
		List<Map> maps = new ArrayList<Map>();
		for (TransferPengadaanItemDetail transferPengadaanItemDetail : transferPengadaanItemDetails) {
			Map map = new HashMap();
			Common.insertProperty(TransferPengadaanItemDetail.class, transferPengadaanItemDetail, map, "");
			map.put("isbn", transferPengadaanItemDetail.getItem().getIsbn());
			map.put("isbn10", transferPengadaanItemDetail.getItem().getIsbn10());
			map.put("nama", transferPengadaanItemDetail.getItem().getNama());
			map.put("perpustakaan_tujuan", transferPengadaanItem.getPerpustakaanTujuan() == null ? ""
					: transferPengadaanItem.getPerpustakaanTujuan().getNama());
			map.put("kode", transferPengadaanItem.getKode());
			map.put("status_persetujuan", transferPengadaanItem.getDisetujuiOleh() == null ? "Belum disetujui"
					: "Disetujui oleh " + transferPengadaanItem.getDisetujuiOleh().getUserNama() + " pada "
							+ (transferPengadaanItem.getTanggalPersetujuan() == null ? ""
									: Common.dateFormat1.get().format(transferPengadaanItem.getTanggalPersetujuan())));
			map.put("perpustakaan", transferPengadaanItem.getPerpustakaan() == null ? ""
					: transferPengadaanItem.getPerpustakaan().getNama());
			map.put("disetujui_oleh", transferPengadaanItem.getDisetujuiOleh() == null ? ""
					: transferPengadaanItem.getDisetujuiOleh().getNama());

			map.put("barcode", transferPengadaanItemDetail.getItemPunyaBarcode() == null ? ""
					: transferPengadaanItemDetail.getItemPunyaBarcode().getBarcode());

			map.put("tanggal_persetujuan", transferPengadaanItem.getTanggalPersetujuan());

			maps.add(map);
		}
		parameters.put("maps", maps);

		parameters.put("id", transferPengadaanItem.getId());
		Report.generatePDFReport(Report.PDF, parameters, "library/transfer_pengadaan",
				transferPengadaanItem.getTanggalPembuatan());
	}

	class TransferPengadaanItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final TransferPengadaanItem transferPengadaanItem = (TransferPengadaanItem) arg1;

			final TransferPengadaanItemDetailAction detail;
			(detail = new TransferPengadaanItemDetailAction(transferPengadaanItem)).setParent(arg0);

			RevisiHelper.createNewRevisi(TransferPengadaanItem.class, transferPengadaanItem,
					transferPengadaanItem.getKode()).setParent(arg0);

			new Label(transferPengadaanItem.getPerpustakaan() == null ? ""
					: transferPengadaanItem.getPerpustakaan().getNama()).setParent(arg0);

			new Label(transferPengadaanItem.getPerpustakaanTujuan() == null ? ""
					: transferPengadaanItem.getPerpustakaanTujuan().getNama()).setParent(arg0);

			new Label(transferPengadaanItem.getTerimaPengadaanItem() == null ? "Belum diterima"
					: "Sudah diterima (" + transferPengadaanItem.getTerimaPengadaanItem().getKode() + ")")
					.setParent(arg0);

			new Label(transferPengadaanItem.getDibuatOleh() == null ? ""
					: transferPengadaanItem.getDibuatOleh().getUserNama()).setParent(arg0);
			new Label(transferPengadaanItem.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(transferPengadaanItem.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(transferPengadaanItem.getDisetujuiOleh() == null ? ""
					: transferPengadaanItem.getDisetujuiOleh().getUserNama())).setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(transferPengadaanItem.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(transferPengadaanItem.getTanggalPersetujuan()))).setParent(arg0);
			new Label(transferPengadaanItem.getKeterangan()).setParent(arg0);

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons = new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Transfer Pengadaan Item");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					cetak(transferPengadaanItem);
				}

			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && transferPengadaanItem.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && transferPengadaanItem.getDisetujuiOleh() != null
					&& transferPengadaanItem.getTerimaPengadaanItem() == null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Transfer Pengadaan Item ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										transferPengadaanItem.setDisetujuiOleh(Common.getCurrentUser());
										transferPengadaanItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, transferPengadaanItem);

										List<TransferPengadaanItemDetail> transferPengadaanItemDetails = session
												.createCriteria(TransferPengadaanItemDetail.class)
												.add(Restrictions.eq("transferPengadaanItem", transferPengadaanItem))
												.list();

										session.createSQLQuery(
												"delete from library.detail_transaksi where transfer_pengadaan_item_detail in (select id from library.transfer_pengadaan_item_detail where transfer_pengadaan_item = "
														+ transferPengadaanItem.getId() + ");")
												.executeUpdate();
										for (TransferPengadaanItemDetail transferPengadaanItemDetail : transferPengadaanItemDetails) {
											DetailTransaksi detailTransaksi = new DetailTransaksi();
											detailTransaksi.setTransferPengadaanItemDetail(transferPengadaanItemDetail);
											detailTransaksi.setQtyBonus(0.0);

											detailTransaksi.setItemPunyaBarcode(
													transferPengadaanItemDetail.getItemPunyaBarcode());
											detailTransaksi.setItem(transferPengadaanItemDetail.getItem());
											detailTransaksi.setKeterangan("Transaksi Transfer Pengadaan");
											detailTransaksi.setKodeTransaksi(LibraryUtil.TRANSFER);
											detailTransaksi.setPerpustakaan(transferPengadaanItem.getPerpustakaan());
											detailTransaksi.setQty(1.0);
											detailTransaksi.setTanggal(transferPengadaanItem.getTanggalPersetujuan());
											detailTransaksi
													.setTanggalDanWaktu(transferPengadaanItem.getTanggalPersetujuan());

											session.save(detailTransaksi);
										}

										disetujuiTanggal
												.setValue(transferPengadaanItem.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(transferPengadaanItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(transferPengadaanItem.getDisetujuiOleh() == null ? ""
												: transferPengadaanItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && transferPengadaanItem.getDisetujuiOleh() == null);
										dibatalkan
												.setVisible(reject && transferPengadaanItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && transferPengadaanItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && transferPengadaanItem.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										final Timer timer = new Timer(500);
										timer.setParent(page.getFirstRoot());
										timer.addEventListener("onTimer", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												cetak(transferPengadaanItem);
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

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Transfer Pengadaan Item ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										transferPengadaanItem.setDisetujuiOleh(null);
										transferPengadaanItem.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, transferPengadaanItem);

										session.createSQLQuery(
												"delete from library.detail_transaksi where transfer_pengadaan_item_detail in (select id from library.transfer_pengadaan_item_detail where transfer_pengadaan_item = "
														+ transferPengadaanItem.getId() + ");")
												.executeUpdate();

										disetujuiTanggal
												.setValue(transferPengadaanItem.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(transferPengadaanItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(transferPengadaanItem.getDisetujuiOleh() == null ? ""
												: transferPengadaanItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && transferPengadaanItem.getDisetujuiOleh() == null);
										dibatalkan
												.setVisible(reject && transferPengadaanItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && transferPengadaanItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && transferPengadaanItem.getDisetujuiOleh() == null);
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
			rubah.setVisible(edit && transferPengadaanItem.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(transferPengadaanItem);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && transferPengadaanItem.getDisetujuiOleh() == null);
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

											TransferPengadaanItemDao transferPengadaanItemDao = DaoFactory.getInstance()
													.getTransferPengadaanItemDao();

											Session session = transferPengadaanItemDao.getCurrentSession();
											List<TransferPengadaanItemDetail> transferPengadaanItemDetails = session
													.createCriteria(TransferPengadaanItemDetail.class).add(Restrictions
															.eq("transferPengadaanItem", transferPengadaanItem))
													.list();
											for (TransferPengadaanItemDetail transferPengadaanItemDetail : transferPengadaanItemDetails) {
												session.delete(transferPengadaanItemDetail);
											}

											Common.refreshDelete(transferPengadaanItem);

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
		init(new TransferPengadaanItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(TransferPengadaanItem transferPengadaanItem) throws Exception {
		this.transferPengadaanItem = transferPengadaanItem;
		addWindow.setTitle(transferPengadaanItem.getId() == null ? "Tambah Transfer Pengadaan Item" : "Ubah Transfer Pengadaan Item");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Transfer Pengadaan Item"));
		String mykode = transferPengadaanItem.getKode();

		row.appendChild(kode = new MyTextbox(
				transferPengadaanItem.getKode() == null ? mykode : transferPengadaanItem.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan"));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				transferPengadaanItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: transferPengadaanItem.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());

		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan Asal"));
		row.appendChild(perpustakaan = new AmbilDataPerpustakaanBanbox());
		perpustakaan.setAttribute("perpustakaan", transferPengadaanItem.getPerpustakaan());
		perpustakaan.setValue(transferPengadaanItem.getPerpustakaan() == null ? ""
				: transferPengadaanItem.getPerpustakaan().toString());

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				currentPerpustakaan = (Perpustakaan) perpustakaan.getAttribute("perpustakaan");
				String mykode = LibraryUtil.generateCode(TransferPengadaanItem.class, 8, "TRF", currentPerpustakaan);
				kode.setValue(mykode);
			}
		};
		perpustakaan.setEventListener(eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan Tujuan"));
		row.appendChild(perpustakaanTujuan = new AmbilDataPerpustakaanBanbox(false, true));
		perpustakaanTujuan.setAttribute("perpustakaan", transferPengadaanItem.getPerpustakaanTujuan());
		perpustakaanTujuan.setValue(transferPengadaanItem.getPerpustakaanTujuan() == null ? ""
				: transferPengadaanItem.getPerpustakaanTujuan().toString());
		perpustakaanTujuan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox(
				transferPengadaanItem.getKeterangan() == null ? "" : transferPengadaanItem.getKeterangan()));
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
			MyMessageboxConfig.show("Kode Transfer harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (perpustakaan.getAttribute("perpustakaan") == null) {
			MyMessageboxConfig.show("Perpustakaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (perpustakaanTujuan.getAttribute("perpustakaan") == null) {
			MyMessageboxConfig.show("Perpustakaan Tujuan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (perpustakaan.getAttribute("perpustakaan").equals(perpustakaanTujuan.getAttribute("perpustakaan"))) {
			MyMessageboxConfig.show("Perpustakaan sumber dengan perpustakaan tujuan tidak boleh sama", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		TransferPengadaanItemDao transferPengadaanItemDao = DaoFactory.getInstance().getTransferPengadaanItemDao();
		if (transferPengadaanItem.getId() != null) {
			transferPengadaanItem = transferPengadaanItemDao.load(transferPengadaanItem.getId());

		}

		transferPengadaanItem.setPerpustakaanTujuan((Perpustakaan) perpustakaanTujuan.getAttribute("perpustakaan"));
		transferPengadaanItem.setPerpustakaan((Perpustakaan) perpustakaan.getAttribute("perpustakaan"));
		transferPengadaanItem.setKode(kode.getValue());
		transferPengadaanItem.setKeterangan(keterangan.getValue());
		transferPengadaanItem.setTanggalPembuatan(tanggalPembuatan.getValue());

		if (transferPengadaanItem.getId() != null) {
			transferPengadaanItemDao.update(transferPengadaanItem);
		} else {
			transferPengadaanItem.setDibuatOleh(Common.getCurrentUser());

			currentPerpustakaan = (Perpustakaan) (perpustakaan.getAttribute("perpustakaan"));
			transferPengadaanItem.setIndex(
					LibraryUtil.generateMaxByPerpustakaan(TransferPengadaanItem.class, currentPerpustakaan) + 1);
			String mykode = LibraryUtil.generateCode(TransferPengadaanItem.class, 8, "TRF", currentPerpustakaan);
			kode.setValue(mykode);
			transferPengadaanItem.setKode(mykode);
			transferPengadaanItemDao.save(transferPengadaanItem);
		}
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TransferPengadaanItem.class)
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
		List<TransferPengadaanItem> transferPengadaanItem = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(transferPengadaanItem);
		grid.setRowRenderer(new TransferPengadaanItemRenderer());
		grid.setModelCheckMobile(strset);

	}

}
