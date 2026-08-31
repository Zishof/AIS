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
import ais.action.master.library.helper.KoreksiItemDetailAction;
import ais.action.master.library.helper.KoreksiItemPunyaItemHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.action.report.format1.library.LaporanKoreksi;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.KoreksiItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.KoreksiItem;
import ais.database.model.library.KoreksiItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk koreksi item. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code MyGrid
 * grid}, {@code Paging paging}, {@code Textbox searchkode}, {@code AmbilDataPerpustakaanBanbox
 * searchperpustakaan}, {@code MyTextbox kode}, {@code MyTextbox keterangan}, {@code MyDatebox tanggalPembuatan};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initDetail()}, {@code
 * init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code
 * onSave()}); pelaporan/ekspor ({@code onCetak()}, {@code cetak()}); operasi domain lain ({@code onAdd()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class KoreksiItemAction extends GenericAutowireComposer {

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

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private Perpustakaan currentPerpustakaan;

	private KoreksiItem koreksiItem;
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
	}

	public void onCetak(Event event) throws Exception {
		LaporanKoreksi laporan = new LaporanKoreksi();
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cetak(KoreksiItem koreksiItem) throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRand();

		Common.insertProperty(KoreksiItem.class, koreksiItem, parameters, "");

		Session session = HibernateUtil.currentSession();
		List<KoreksiItemDetail> koreksiItemDetails = session.createCriteria(KoreksiItemDetail.class)
				.add(Restrictions.eq("koreksiItem", koreksiItem)).list();
		List<Map> maps = new ArrayList<Map>();
		for (KoreksiItemDetail koreksiItemDetail : koreksiItemDetails) {
			Map map = new HashMap();
			Common.insertProperty(KoreksiItemDetail.class, koreksiItemDetail, map, "");
			map.put("isbn", koreksiItemDetail.getItem().getIsbn());
			map.put("isbn10", koreksiItemDetail.getItem().getIsbn10());
			map.put("nama", koreksiItemDetail.getItem().getNama());

			map.put("kode", koreksiItem.getKode());
			map.put("status_persetujuan",
					koreksiItem.getDisetujuiOleh() == null ? "Belum disetujui"
							: "Disetujui oleh " + koreksiItem.getDisetujuiOleh().getUserNama() + " pada "
									+ (koreksiItem.getTanggalPersetujuan() == null ? ""
											: Common.dateFormat1.get().format(koreksiItem.getTanggalPersetujuan())));
			map.put("perpustakaan",
					koreksiItem.getPerpustakaan() == null ? "" : koreksiItem.getPerpustakaan().getNama());
			map.put("disetujui_oleh",
					koreksiItem.getDisetujuiOleh() == null ? "" : koreksiItem.getDisetujuiOleh().getNama());

			map.put("tanggal_persetujuan", koreksiItem.getTanggalPersetujuan());
			map.put("jumlah", koreksiItemDetail.getJumlah());
			maps.add(map);
		}
		parameters.put("maps", maps);

		parameters.put("id", koreksiItem.getId());
		Report.generatePDFReport(Report.PDF, parameters, "library/koreksi_item", koreksiItem.getTanggalPembuatan());

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KoreksiItemAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KoreksiItemAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KoreksiItemAction
	 */
	class KoreksiItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KoreksiItem koreksiItem = (KoreksiItem) arg1;

			final KoreksiItemDetailAction detail;
			(detail = new KoreksiItemDetailAction(koreksiItem)).setParent(arg0);

			RevisiHelper.createNewRevisi(KoreksiItem.class, koreksiItem, koreksiItem.getKode()).setParent(arg0);

			new Label(koreksiItem.getPerpustakaan() == null ? "" : koreksiItem.getPerpustakaan().getNama())
					.setParent(arg0);

			new Label(koreksiItem.getDibuatOleh() == null ? "" : koreksiItem.getDibuatOleh().getUserNama())
					.setParent(arg0);
			new Label(koreksiItem.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(koreksiItem.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(
					koreksiItem.getDisetujuiOleh() == null ? "" : koreksiItem.getDisetujuiOleh().getUserNama()))
					.setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(koreksiItem.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(koreksiItem.getTanggalPersetujuan()))).setParent(arg0);
			new Label(koreksiItem.getKeterangan()).setParent(arg0);

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons = new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Koreksi");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(koreksiItem);
				}

			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && koreksiItem.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && koreksiItem.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Koreksi ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings({ "unchecked" })
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										koreksiItem.setDisetujuiOleh(Common.getCurrentUser());
										koreksiItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, koreksiItem);

										List<KoreksiItemDetail> koreksiItemDetails = session
												.createCriteria(KoreksiItemDetail.class)
												.add(Restrictions.eq("koreksiItem", koreksiItem)).list();

										session.createSQLQuery(
												"delete from library.detail_transaksi where koreksi_item_detail in (select id from library.koreksi_item_detail where koreksi_item = "
														+ koreksiItem.getId() + " );")
												.executeUpdate();
										for (KoreksiItemDetail koreksiItemDetail : koreksiItemDetails) {
											DetailTransaksi detailTransaksi = new DetailTransaksi();
											detailTransaksi.setKoreksiItemDetail(koreksiItemDetail);
											detailTransaksi.setQtyBonus(0.0);

											detailTransaksi.setItem(koreksiItemDetail.getItem());
											detailTransaksi.setKeterangan(
													"Transaksi " + koreksiItemDetail.getKodeTransaksi().getNama());
											detailTransaksi.setKodeTransaksi(koreksiItemDetail.getKodeTransaksi());
											detailTransaksi.setPerpustakaan(koreksiItem.getPerpustakaan());
											detailTransaksi.setQty(koreksiItemDetail.getJumlah());
											detailTransaksi.setTanggal(koreksiItem.getTanggalPersetujuan());
											detailTransaksi.setTanggalDanWaktu(koreksiItem.getTanggalPersetujuan());

											session.save(detailTransaksi);
										}

										disetujuiTanggal.setValue(koreksiItem.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(koreksiItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(koreksiItem.getDisetujuiOleh() == null ? ""
												: koreksiItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && koreksiItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && koreksiItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && koreksiItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && koreksiItem.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										final Timer timer = new Timer(500);
										timer.setParent(page.getFirstRoot());
										timer.addEventListener("onTimer", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												cetak(koreksiItem);
												timer.detach();
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

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Koreksi ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										koreksiItem.setDisetujuiOleh(null);
										koreksiItem.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, koreksiItem);

										session.createSQLQuery(
												"delete from library.detail_transaksi where koreksi_item_detail in (select id from library.koreksi_item_detail where koreksi_item = "
														+ koreksiItem.getId() + " );")
												.executeUpdate();

										disetujuiTanggal.setValue(koreksiItem.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(koreksiItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(koreksiItem.getDisetujuiOleh() == null ? ""
												: koreksiItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && koreksiItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && koreksiItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && koreksiItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && koreksiItem.getDisetujuiOleh() == null);
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
			rubah.setVisible(edit && koreksiItem.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(koreksiItem);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && koreksiItem.getDisetujuiOleh() == null);
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

											KoreksiItemDao koreksiItemDao = DaoFactory.getInstance()
													.getKoreksiItemDao();

											Session session = koreksiItemDao.getCurrentSession();
											List<KoreksiItemDetail> koreksiItemDetails = session
													.createCriteria(KoreksiItemDetail.class)
													.add(Restrictions.eq("koreksiItem", koreksiItem)).list();
											for (KoreksiItemDetail koreksiItemDetail : koreksiItemDetails) {
												session.delete(koreksiItemDetail);
											}

											Common.refreshDelete(koreksiItem);

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
		init(new KoreksiItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final KoreksiItem koreksiItem, Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabDipinjam = new MyTabConfig("Item Koreksi");
		tabDipinjam.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDipinjam = new ais.ui.util.MyTabpanel();
		tabpanelDipinjam.setParent(tabpanels);
		tabpanelDipinjam.setWidth("100%");

		tabpanelDipinjam.appendChild(
				new KoreksiItemPunyaItemHelper(gridItem = new MyGrid()).initDetail(koreksiItem, perpustakaan));

	}

	private void init(KoreksiItem koreksiItem) throws Exception {
		this.koreksiItem = koreksiItem;
		addWindow.setTitle(koreksiItem.getId() == null ? "Tambah Koreksi" : "Ubah Koreksi");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("70%");

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Koreksi"));
		String mykode = koreksiItem.getKode();

		row.appendChild(kode = new MyTextbox(koreksiItem.getKode() == null ? mykode : koreksiItem.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan"));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				koreksiItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: koreksiItem.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		;
		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new AmbilDataPerpustakaanBanbox());
		perpustakaan.setAttribute("perpustakaan", koreksiItem.getPerpustakaan());
		perpustakaan.setValue(koreksiItem.getPerpustakaan() == null ? "" : koreksiItem.getPerpustakaan().toString());
		perpustakaan.setWidth("90%");
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				currentPerpustakaan = (Perpustakaan) perpustakaan.getAttribute("perpustakaan");
				String mykode = LibraryUtil.generateCode(KoreksiItem.class, 8, "KR", currentPerpustakaan);
				kode.setValue(mykode);
			}
		};
		perpustakaan.setEventListener(eventListener);
		eventListener.onEvent(null);

		initDetail(koreksiItem, east);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new MyTextbox(koreksiItem.getKeterangan() == null ? "" : koreksiItem.getKeterangan()));
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

		List<Row> rowsItem = gridItem.getRows().getChildren();
		for (Row row : rowsItem) {
			KoreksiItemDetail koreksiItemDetail = (KoreksiItemDetail) row.getAttribute("koreksiItemDetail");
			if (koreksiItemDetail.getItem() == null) {
				MyMessageboxConfig.show("Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		KoreksiItemDao koreksiItemDao = DaoFactory.getInstance().getKoreksiItemDao();
		if (koreksiItem.getId() != null) {
			koreksiItem = koreksiItemDao.load(koreksiItem.getId());

		}

		koreksiItem.setPerpustakaan((Perpustakaan) perpustakaan.getAttribute("perpustakaan"));
		koreksiItem.setKode(kode.getValue());
		koreksiItem.setKeterangan(keterangan.getValue());
		koreksiItem.setTanggalPembuatan(tanggalPembuatan.getValue());

		if (koreksiItem.getId() != null) {
			koreksiItemDao.update(koreksiItem);
		} else {
			koreksiItem.setDibuatOleh(Common.getCurrentUser());

			currentPerpustakaan = (Perpustakaan) (perpustakaan.getAttribute("perpustakaan"));
			koreksiItem.setIndex(LibraryUtil.generateMaxByPerpustakaan(KoreksiItem.class, currentPerpustakaan) + 1);
			String mykode = LibraryUtil.generateCode(KoreksiItem.class, 8, "KR", currentPerpustakaan);
			kode.setValue(mykode);
			koreksiItem.setKode(mykode);
			koreksiItemDao.save(koreksiItem);
		}

		Session session = koreksiItemDao.getCurrentSession();
		for (Row row : rowsItem) {
			KoreksiItemDetail koreksiItemDetail = (KoreksiItemDetail) row.getAttribute("koreksiItemDetail");
			koreksiItemDetail.setKoreksiItem(koreksiItem);
			session.saveOrUpdate(koreksiItemDetail);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KoreksiItem.class)
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
		List<KoreksiItem> koreksiItem = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(koreksiItem);
		grid.setRowRenderer(new KoreksiItemRenderer());
		grid.setModelCheckMobile(strset);

	}

}
