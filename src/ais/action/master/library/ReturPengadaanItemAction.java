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
import ais.action.master.library.helper.AmbilDataPenerimaanPengadaanBanbox;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.helper.ReturPengadaanItemDetailAction;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.action.report.format1.library.LaporanRetur;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.ReturPengadaanItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.PenerimaanPengadaanItem;
import ais.database.model.library.PenerimaanPengadaanItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.ReturPengadaanItem;
import ais.database.model.library.ReturPengadaanItemDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk retur pengadaan item. Tipe ini merupakan titik masuk UI yang
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
public class ReturPengadaanItemAction extends GenericAutowireComposer {

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
	private AmbilDataPenerimaanPengadaanBanbox penerimaanPengadaanItem;
	private Label penyedia;
	private Label perpustakaan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private Perpustakaan currentPerpustakaan;

	private ReturPengadaanItem returPengadaanItem;
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
		LaporanRetur laporan = new LaporanRetur();
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cetak(ReturPengadaanItem returPengadaanItem) throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();

		Session session = HibernateUtil.currentSession();
		List<ReturPengadaanItemDetail> returPengadaanItemDetails = session
				.createCriteria(ReturPengadaanItemDetail.class)
				.add(Restrictions.eq("returPengadaanItem", returPengadaanItem)).list();
		List<Map> maps = new ArrayList<Map>();
		for (ReturPengadaanItemDetail returPengadaanItemDetail : returPengadaanItemDetails) {
			Map map = new HashMap();
			Common.insertProperty(ReturPengadaanItemDetail.class, returPengadaanItemDetail, map, "");
			map.put("isbn", returPengadaanItemDetail.getItem().getIsbn());
			map.put("isbn10", returPengadaanItemDetail.getItem().getIsbn10());
			map.put("nama", returPengadaanItemDetail.getItem().getNama());
			map.put("penyedia",
					returPengadaanItem.getPenyedia() == null ? "" : returPengadaanItem.getPenyedia().getNama());
			map.put("jumlah", returPengadaanItemDetail.getJumlah());
			map.put("kode", returPengadaanItem.getKode());
			map.put("status_persetujuan",
					returPengadaanItem.getDisetujuiOleh() == null ? "Belum disetujui"
							: "Disetujui oleh " + returPengadaanItem.getDisetujuiOleh().getUserNama() + " pada "
									+ (returPengadaanItem.getTanggalPersetujuan() == null ? ""
											: Common.dateFormat1.get().format(returPengadaanItem.getTanggalPersetujuan())));
			map.put("perpustakaan",
					returPengadaanItem.getPerpustakaan() == null ? "" : returPengadaanItem.getPerpustakaan().getNama());
			map.put("disetujui_oleh", returPengadaanItem.getDisetujuiOleh() == null ? ""
					: returPengadaanItem.getDisetujuiOleh().getNama());

			map.put("tanggal_persetujuan", returPengadaanItem.getTanggalPersetujuan());

			maps.add(map);
		}
		parameters.put("maps", maps);

		Common.insertProperty(ReturPengadaanItem.class, returPengadaanItem, parameters, "");
		parameters.put("id", returPengadaanItem.getId());
		Report.generatePDFReport(Report.PDF, parameters, "library/retur_pengadaan",
				returPengadaanItem.getTanggalPembuatan());
	}

	class ReturPengadaanItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ReturPengadaanItem returPengadaanItem = (ReturPengadaanItem) arg1;

			final ReturPengadaanItemDetailAction detail;
			(detail = new ReturPengadaanItemDetailAction(returPengadaanItem)).setParent(arg0);

			RevisiHelper.createNewRevisi(ReturPengadaanItem.class, returPengadaanItem, returPengadaanItem.getKode())
					.setParent(arg0);

			new Label(returPengadaanItem.getPenyedia() == null ? "" : returPengadaanItem.getPenyedia().getNama())
					.setParent(arg0);

			new Label(
					returPengadaanItem.getPerpustakaan() == null ? "" : returPengadaanItem.getPerpustakaan().getNama())
					.setParent(arg0);

			new Label(
					returPengadaanItem.getDibuatOleh() == null ? "" : returPengadaanItem.getDibuatOleh().getUserNama())
					.setParent(arg0);
			new Label(returPengadaanItem.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(returPengadaanItem.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(returPengadaanItem.getDisetujuiOleh() == null ? ""
					: returPengadaanItem.getDisetujuiOleh().getUserNama())).setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(returPengadaanItem.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(returPengadaanItem.getTanggalPersetujuan()))).setParent(arg0);
			new Label(returPengadaanItem.getKeterangan()).setParent(arg0);

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons = new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Retur Pengadaan Item");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					cetak(returPengadaanItem);

				}

			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && returPengadaanItem.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && returPengadaanItem.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Retur Pengadaan Item ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										returPengadaanItem.setDisetujuiOleh(Common.getCurrentUser());
										returPengadaanItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, returPengadaanItem);

										List<ReturPengadaanItemDetail> returPengadaanItemDetails = session
												.createCriteria(ReturPengadaanItemDetail.class)
												.add(Restrictions.eq("returPengadaanItem", returPengadaanItem)).list();

										session.createSQLQuery(
												"delete from library.detail_transaksi where retur_pengadaan_item_detail in (select id from library.retur_pengadaan_item_detail where retur_pengadaan_item = "
														+ returPengadaanItem.getId() + ");")
												.executeUpdate();
										for (ReturPengadaanItemDetail returPengadaanItemDetail : returPengadaanItemDetails) {
											DetailTransaksi detailTransaksi = new DetailTransaksi();
											detailTransaksi.setReturPengadaanItemDetail(returPengadaanItemDetail);
											detailTransaksi.setQtyBonus(0.0);

											detailTransaksi.setItem(returPengadaanItemDetail.getItem());
											detailTransaksi.setKeterangan("Transaksi Retur Pengadaan");
											detailTransaksi.setKodeTransaksi(LibraryUtil.RETUR_BELI);
											detailTransaksi.setPerpustakaan(returPengadaanItem.getPerpustakaan());
											detailTransaksi.setQty(returPengadaanItemDetail.getDikembalikan());
											detailTransaksi.setTanggal(returPengadaanItem.getTanggalPersetujuan());
											detailTransaksi
													.setTanggalDanWaktu(returPengadaanItem.getTanggalPersetujuan());

											session.save(detailTransaksi);
										}

										disetujuiTanggal
												.setValue(returPengadaanItem.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(returPengadaanItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(returPengadaanItem.getDisetujuiOleh() == null ? ""
												: returPengadaanItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && returPengadaanItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && returPengadaanItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && returPengadaanItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && returPengadaanItem.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										final Timer timer = new Timer(500);
										timer.setParent(page.getFirstRoot());
										timer.addEventListener("onTimer", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												cetak(returPengadaanItem);
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

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Retur Pengadaan Item ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										returPengadaanItem.setDisetujuiOleh(null);
										returPengadaanItem.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, returPengadaanItem);

										session.createSQLQuery(
												"delete from library.detail_transaksi where retur_pengadaan_item_detail in (select id from library.retur_pengadaan_item_detail where retur_pengadaan_item = "
														+ returPengadaanItem.getId() + ");")
												.executeUpdate();

										disetujuiTanggal
												.setValue(returPengadaanItem.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(returPengadaanItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(returPengadaanItem.getDisetujuiOleh() == null ? ""
												: returPengadaanItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && returPengadaanItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && returPengadaanItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && returPengadaanItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && returPengadaanItem.getDisetujuiOleh() == null);
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
			rubah.setVisible(edit && returPengadaanItem.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(returPengadaanItem);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && returPengadaanItem.getDisetujuiOleh() == null);
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

											ReturPengadaanItemDao returPengadaanItemDao = DaoFactory.getInstance()
													.getReturPengadaanItemDao();

											Session session = returPengadaanItemDao.getCurrentSession();
											List<ReturPengadaanItemDetail> returPengadaanItemDetails = session
													.createCriteria(ReturPengadaanItemDetail.class)
													.add(Restrictions.eq("returPengadaanItem", returPengadaanItem))
													.list();
											for (ReturPengadaanItemDetail returPengadaanItemDetail : returPengadaanItemDetails) {
												session.delete(returPengadaanItemDetail);
											}

											Common.refreshDelete(returPengadaanItem);

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
		init(new ReturPengadaanItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(ReturPengadaanItem returPengadaanItem) throws Exception {
		this.returPengadaanItem = returPengadaanItem;
		addWindow.setTitle(returPengadaanItem.getId() == null ? "Tambah Retur Pengadaan Item" : "Ubah Retur Pengadaan Item");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Retur Pengadaan Item"));
		String mykode = returPengadaanItem.getKode();
		row.appendChild(
				kode = new MyTextbox(returPengadaanItem.getKode() == null ? mykode : returPengadaanItem.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerimaan Pengadaan"));
		row.appendChild(penerimaanPengadaanItem = new AmbilDataPenerimaanPengadaanBanbox());
		penerimaanPengadaanItem.setWidth("90%");
		penerimaanPengadaanItem.setAttribute("penerimaanPengadaanItem",
				returPengadaanItem.getPenerimaanPengadaanItem());
		penerimaanPengadaanItem.setValue(returPengadaanItem.getPenerimaanPengadaanItem() == null ? ""
				: returPengadaanItem.getPenerimaanPengadaanItem().toString());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyedia"));
		row.appendChild(penyedia = new Label(
				returPengadaanItem.getPenyedia() == null ? "" : returPengadaanItem.getPenyedia().getNama()));

		penerimaanPengadaanItem.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				PenerimaanPengadaanItem mypenerimaanPengadaanItem = (PenerimaanPengadaanItem) penerimaanPengadaanItem
						.getAttribute("penerimaanPengadaanItem");
				penyedia.setValue(
						mypenerimaanPengadaanItem == null || mypenerimaanPengadaanItem.getPenyedia() == null ? ""
								: (mypenerimaanPengadaanItem).getPenyedia().getNama());
				perpustakaan.setValue(
						mypenerimaanPengadaanItem == null || mypenerimaanPengadaanItem.getPerpustakaan() == null ? ""
								: (mypenerimaanPengadaanItem).getPerpustakaan().getNama());

				currentPerpustakaan = mypenerimaanPengadaanItem.getPerpustakaan();
				String mykode = LibraryUtil.generateCode(ReturPengadaanItem.class, 8, "RTR", currentPerpustakaan);
				kode.setValue(mykode);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan"));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				returPengadaanItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: returPengadaanItem.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		;
		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new Label(
				returPengadaanItem.getPerpustakaan() == null ? "" : returPengadaanItem.getPerpustakaan().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox(
				returPengadaanItem.getKeterangan() == null ? "" : returPengadaanItem.getKeterangan()));
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
	public void generateDetail(ReturPengadaanItem returPengadaanItem) {
		Session session = HibernateUtil.currentSession();
		List<PenerimaanPengadaanItemDetail> penerimaanPengadaanItemDetails = session
				.createCriteria(PenerimaanPengadaanItemDetail.class)
				.add(Restrictions.eq("penerimaanPengadaanItem", returPengadaanItem.getPenerimaanPengadaanItem()))
				.list();

		for (PenerimaanPengadaanItemDetail penerimaanPengadaanItemDetail : penerimaanPengadaanItemDetails) {
			ReturPengadaanItemDetail returPengadaanItemDetail = new ReturPengadaanItemDetail();
			returPengadaanItemDetail.setItem(penerimaanPengadaanItemDetail.getItem());
			returPengadaanItemDetail.setJumlah(penerimaanPengadaanItemDetail.getDiterima());
			returPengadaanItemDetail.setDikembalikan(0.0);
			returPengadaanItemDetail.setKeterangan(penerimaanPengadaanItemDetail.getKeterangan());
			returPengadaanItemDetail.setReturPengadaanItem(returPengadaanItem);
			session.save(returPengadaanItemDetail);
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Retur harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (penerimaanPengadaanItem.getAttribute("penerimaanPengadaanItem") == null) {
			MyMessageboxConfig.show("Penerimaan Pengadaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (keterangan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Keterangan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		PenerimaanPengadaanItem mypenerimaanPengadaanItem = (PenerimaanPengadaanItem) penerimaanPengadaanItem
				.getAttribute("penerimaanPengadaanItem");

		if (mypenerimaanPengadaanItem.getPenyedia() == null) {
			MyMessageboxConfig.show("Penyedia harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		ReturPengadaanItemDao returPengadaanItemDao = DaoFactory.getInstance().getReturPengadaanItemDao();
		if (returPengadaanItem.getId() != null) {
			returPengadaanItem = returPengadaanItemDao.load(returPengadaanItem.getId());

		}

		returPengadaanItem.setKode(kode.getValue());
		returPengadaanItem.setKeterangan(keterangan.getValue());
		returPengadaanItem.setTanggalPembuatan(tanggalPembuatan.getValue());
		returPengadaanItem.setPenerimaanPengadaanItem(
				(PenerimaanPengadaanItem) penerimaanPengadaanItem.getAttribute("penerimaanPengadaanItem"));
		returPengadaanItem.setPenyedia(mypenerimaanPengadaanItem.getPenyedia());
		returPengadaanItem.setPerpustakaan(mypenerimaanPengadaanItem.getPerpustakaan());

		if (returPengadaanItem.getId() != null) {
			returPengadaanItemDao.update(returPengadaanItem);
		} else {
			returPengadaanItem.setDibuatOleh(Common.getCurrentUser());

			currentPerpustakaan = returPengadaanItem.getPerpustakaan();
			returPengadaanItem
					.setIndex(LibraryUtil.generateMaxByPerpustakaan(ReturPengadaanItem.class, currentPerpustakaan) + 1);
			String mykode = LibraryUtil.generateCode(ReturPengadaanItem.class, 8, "RTR", currentPerpustakaan);
			kode.setValue(mykode);
			returPengadaanItem.setKode(mykode);
			returPengadaanItemDao.save(returPengadaanItem);

			generateDetail(returPengadaanItem);
		}
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ReturPengadaanItem.class)
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
		List<ReturPengadaanItem> returPengadaanItem = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(returPengadaanItem);
		grid.setRowRenderer(new ReturPengadaanItemRenderer());
		grid.setModelCheckMobile(strset);

	}

}
