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
import ais.action.master.library.helper.AmbilDataPermintaanPengadaanPerPenyediaBanbox;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.helper.PemesananPengadaanItemDetailAction;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.action.report.format1.library.LaporanPemesanan;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.PemesananPengadaanItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.PemesananPengadaanItem;
import ais.database.model.library.PemesananPengadaanItemDetail;
import ais.database.model.library.Penyedia;
import ais.database.model.library.PermintaanPengadaanItem;
import ais.database.model.library.PermintaanPengadaanItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk pemesanan pengadaan item. Tipe ini merupakan titik masuk UI yang
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
public class PemesananPengadaanItemAction extends GenericAutowireComposer {

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
	private AmbilDataPermintaanPengadaanPerPenyediaBanbox permintaanPengadaanItem;
	private Label penyedia;
	private Label perpustakaan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private Perpustakaan currentPerpustakaan;

	private PemesananPengadaanItem pemesananPengadaanItem;
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
	}

	public void onCetak(Event event) throws Exception {
		LaporanPemesanan laporan = new LaporanPemesanan();
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cetak(PemesananPengadaanItem pemesananPengadaanItem) throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();

		Common.insertProperty(PemesananPengadaanItem.class, pemesananPengadaanItem, parameters, "");

		Session session = HibernateUtil.currentSession();
		List<PemesananPengadaanItemDetail> pemesananPengadaanItemDetails = session
				.createCriteria(PemesananPengadaanItemDetail.class)
				.add(Restrictions.eq("pemesananPengadaanItem", pemesananPengadaanItem)).list();
		List<Map> maps = new ArrayList<Map>();
		for (PemesananPengadaanItemDetail pemesananPengadaanItemDetail : pemesananPengadaanItemDetails) {
			Map map = new HashMap();
			Common.insertProperty(PemesananPengadaanItemDetail.class, pemesananPengadaanItemDetail, map, "");
			map.put("isbn", pemesananPengadaanItemDetail.getItem().getIsbn());
			map.put("isbn10", pemesananPengadaanItemDetail.getItem().getIsbn10());
			map.put("nama", pemesananPengadaanItemDetail.getItem().getNama());
			map.put("penyedia",
					pemesananPengadaanItem.getPenyedia() == null ? "" : pemesananPengadaanItem.getPenyedia().getNama());
			map.put("jumlah", pemesananPengadaanItemDetail.getJumlah());
			map.put("kode", pemesananPengadaanItem.getKode());
			map.put("status_persetujuan", pemesananPengadaanItem.getDisetujuiOleh() == null ? "Belum disetujui"
					: "Disetujui oleh " + pemesananPengadaanItem.getDisetujuiOleh().getUserNama() + " pada "
							+ (pemesananPengadaanItem.getTanggalPersetujuan() == null ? ""
									: Common.dateFormat1.get().format(pemesananPengadaanItem.getTanggalPersetujuan())));
			map.put("perpustakaan", pemesananPengadaanItem.getPerpustakaan() == null ? ""
					: pemesananPengadaanItem.getPerpustakaan().getNama());
			map.put("disetujui_oleh", pemesananPengadaanItem.getDisetujuiOleh() == null ? ""
					: pemesananPengadaanItem.getDisetujuiOleh().getNama());

			map.put("tanggal_persetujuan", pemesananPengadaanItem.getTanggalPersetujuan());

			maps.add(map);
		}
		parameters.put("maps", maps);

		parameters.put("id", pemesananPengadaanItem.getId());

		Report.generatePDFReport(Report.PDF, parameters, "library/pemesanan_pengadaan",
				pemesananPengadaanItem.getTanggalPembuatan());
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PemesananPengadaanItemAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PemesananPengadaanItemAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PemesananPengadaanItemAction
	 */
	class PemesananPengadaanItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PemesananPengadaanItem pemesananPengadaanItem = (PemesananPengadaanItem) arg1;

			final PemesananPengadaanItemDetailAction detail;
			(detail = new PemesananPengadaanItemDetailAction(pemesananPengadaanItem)).setParent(arg0);

			RevisiHelper.createNewRevisi(PemesananPengadaanItem.class, pemesananPengadaanItem,
					pemesananPengadaanItem.getKode()).setParent(arg0);

			new Label(
					pemesananPengadaanItem.getPenyedia() == null ? "" : pemesananPengadaanItem.getPenyedia().getNama())
					.setParent(arg0);

			new Label(pemesananPengadaanItem.getPerpustakaan() == null ? ""
					: pemesananPengadaanItem.getPerpustakaan().getNama()).setParent(arg0);

			new Label(pemesananPengadaanItem.getDibuatOleh() == null ? ""
					: pemesananPengadaanItem.getDibuatOleh().getUserNama()).setParent(arg0);
			new Label(pemesananPengadaanItem.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(pemesananPengadaanItem.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(pemesananPengadaanItem.getDisetujuiOleh() == null ? ""
					: pemesananPengadaanItem.getDisetujuiOleh().getUserNama())).setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(pemesananPengadaanItem.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(pemesananPengadaanItem.getTanggalPersetujuan()))).setParent(arg0);
			new Label(pemesananPengadaanItem.getKeterangan()).setParent(arg0);

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons = new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Pemesanan Pengadaan Item");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					cetak(pemesananPengadaanItem);
				}

			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && pemesananPengadaanItem.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && pemesananPengadaanItem.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Pemesanan Pengadaan Item ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();
										Integer countItemjumlah = ((Number) session
												.createCriteria(PemesananPengadaanItemDetail.class)
												.setProjection(Projections.count("id"))
												.add(Restrictions.eq("pemesananPengadaanItem", pemesananPengadaanItem))
												.add(Restrictions.lt("jumlah", 1.0)).uniqueResult()).intValue();

										if (!countItemjumlah.equals(0)) {
											MyMessageboxConfig.show("Lengkapilah jumlah !", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}

										pemesananPengadaanItem.setDisetujuiOleh(Common.getCurrentUser());
										pemesananPengadaanItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, pemesananPengadaanItem);

										disetujuiTanggal
												.setValue(pemesananPengadaanItem.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																pemesananPengadaanItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pemesananPengadaanItem.getDisetujuiOleh() == null ? ""
												: pemesananPengadaanItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && pemesananPengadaanItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && pemesananPengadaanItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && pemesananPengadaanItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && pemesananPengadaanItem.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										final Timer timer = new Timer(500);
										timer.setParent(page.getFirstRoot());
										timer.addEventListener("onTimer", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												cetak(pemesananPengadaanItem);
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

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Pemesanan Pengadaan Item ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pemesananPengadaanItem.setDisetujuiOleh(null);
										pemesananPengadaanItem.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, pemesananPengadaanItem);

										disetujuiTanggal
												.setValue(pemesananPengadaanItem.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																pemesananPengadaanItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pemesananPengadaanItem.getDisetujuiOleh() == null ? ""
												: pemesananPengadaanItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && pemesananPengadaanItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && pemesananPengadaanItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && pemesananPengadaanItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && pemesananPengadaanItem.getDisetujuiOleh() == null);
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
			rubah.setVisible(edit && pemesananPengadaanItem.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pemesananPengadaanItem);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && pemesananPengadaanItem.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											PemesananPengadaanItemDao pemesananPengadaanItemDao = DaoFactory
													.getInstance().getPemesananPengadaanItemDao();

											Session session = pemesananPengadaanItemDao.getCurrentSession();
											List<PemesananPengadaanItemDetail> pemesananPengadaanItemDetails = session
													.createCriteria(PemesananPengadaanItemDetail.class).add(Restrictions
															.eq("pemesananPengadaanItem", pemesananPengadaanItem))
													.list();
											for (PemesananPengadaanItemDetail pemesananPengadaanItemDetail : pemesananPengadaanItemDetails) {
												session.delete(pemesananPengadaanItemDetail);
											}

											Common.refreshDelete(pemesananPengadaanItem);

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
		init(new PemesananPengadaanItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PemesananPengadaanItem pemesananPengadaanItem) throws Exception {
		this.pemesananPengadaanItem = pemesananPengadaanItem;
		addWindow.setTitle(pemesananPengadaanItem.getId() == null ? "Tambah Pemesanan Pengadaan Item" : "Ubah Pemesanan Pengadaan Item");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pemesanan Pengadaan Item"));
		String mykode = pemesananPengadaanItem.getKode();
		row.appendChild(kode = new MyTextbox(
				pemesananPengadaanItem.getKode() == null ? mykode : pemesananPengadaanItem.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Permintaan Pengadaan"));
		row.appendChild(permintaanPengadaanItem = new AmbilDataPermintaanPengadaanPerPenyediaBanbox());
		permintaanPengadaanItem.setWidth("90%");
		permintaanPengadaanItem.setAttribute("permintaanPengadaanItem",
				pemesananPengadaanItem.getPermintaanPengadaanItem());
		permintaanPengadaanItem.setAttribute("penyedia", pemesananPengadaanItem.getPenyedia());
		permintaanPengadaanItem.setValue(pemesananPengadaanItem.getPermintaanPengadaanItem() == null ? ""
				: pemesananPengadaanItem.getPermintaanPengadaanItem().toString());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyedia"));
		row.appendChild(penyedia = new Label(
				pemesananPengadaanItem.getPenyedia() == null ? "" : pemesananPengadaanItem.getPenyedia().getNama()));

		permintaanPengadaanItem.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				penyedia.setValue(permintaanPengadaanItem.getAttribute("penyedia") == null ? ""
						: ((Penyedia) permintaanPengadaanItem.getAttribute("penyedia")).getNama());

				perpustakaan.setValue(permintaanPengadaanItem.getAttribute("permintaanPengadaanItem") == null
						|| ((PermintaanPengadaanItem) permintaanPengadaanItem.getAttribute("permintaanPengadaanItem"))
								.getPerpustakaan() == null ? ""
										: ((PermintaanPengadaanItem) permintaanPengadaanItem
												.getAttribute("permintaanPengadaanItem")).getPerpustakaan().getNama());

				currentPerpustakaan = ((PermintaanPengadaanItem) permintaanPengadaanItem
						.getAttribute("permintaanPengadaanItem")).getPerpustakaan();
				String mykode = LibraryUtil.generateCode(PemesananPengadaanItem.class, 8, "PO", currentPerpustakaan);
				kode.setValue(mykode);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan"));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				pemesananPengadaanItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: pemesananPengadaanItem.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		;
		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new Label(pemesananPengadaanItem.getPerpustakaan() == null ? ""
				: pemesananPengadaanItem.getPerpustakaan().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox(
				pemesananPengadaanItem.getKeterangan() == null ? "" : pemesananPengadaanItem.getKeterangan()));
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
	public void generateDetail(PemesananPengadaanItem pemesananPengadaanItem) {
		Session session = HibernateUtil.currentSession();
		List<PermintaanPengadaanItemDetail> permintaanPengadaanItemDetails = session
				.createCriteria(PermintaanPengadaanItemDetail.class)
				.add(Restrictions.eq("permintaanPengadaanItem", pemesananPengadaanItem.getPermintaanPengadaanItem()))
				.add(Restrictions.eq("penyedia", pemesananPengadaanItem.getPenyedia())).list();

		for (PermintaanPengadaanItemDetail permintaanPengadaanItemDetail : permintaanPengadaanItemDetails) {
			PemesananPengadaanItemDetail pemesananPengadaanItemDetail = new PemesananPengadaanItemDetail();
			pemesananPengadaanItemDetail.setItem(permintaanPengadaanItemDetail.getItem());
			pemesananPengadaanItemDetail.setJumlah(permintaanPengadaanItemDetail.getJumlah());
			pemesananPengadaanItemDetail.setKeterangan(permintaanPengadaanItemDetail.getKeterangan());
			pemesananPengadaanItemDetail.setPemesananPengadaanItem(pemesananPengadaanItem);
			session.save(pemesananPengadaanItemDetail);
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Pemesanan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (permintaanPengadaanItem.getAttribute("permintaanPengadaanItem") == null) {
			MyMessageboxConfig.show("Permintaan Pengadaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (permintaanPengadaanItem.getAttribute("penyedia") == null) {
			MyMessageboxConfig.show("Penyedia harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (keterangan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Keterangan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		PemesananPengadaanItemDao pemesananPengadaanItemDao = DaoFactory.getInstance().getPemesananPengadaanItemDao();
		if (pemesananPengadaanItem.getId() != null) {
			pemesananPengadaanItem = pemesananPengadaanItemDao.load(pemesananPengadaanItem.getId());

		}

		pemesananPengadaanItem.setKode(kode.getValue());
		pemesananPengadaanItem.setKeterangan(keterangan.getValue());
		pemesananPengadaanItem.setTanggalPembuatan(tanggalPembuatan.getValue());
		pemesananPengadaanItem.setPermintaanPengadaanItem(
				(PermintaanPengadaanItem) permintaanPengadaanItem.getAttribute("permintaanPengadaanItem"));
		pemesananPengadaanItem.setPenyedia((Penyedia) permintaanPengadaanItem.getAttribute("penyedia"));
		pemesananPengadaanItem.setPerpustakaan(pemesananPengadaanItem.getPermintaanPengadaanItem().getPerpustakaan());

		if (pemesananPengadaanItem.getId() != null) {
			pemesananPengadaanItemDao.update(pemesananPengadaanItem);
		} else {
			pemesananPengadaanItem.setDibuatOleh(Common.getCurrentUser());

			currentPerpustakaan = pemesananPengadaanItem.getPerpustakaan();
			pemesananPengadaanItem.setIndex(
					LibraryUtil.generateMaxByPerpustakaan(PemesananPengadaanItem.class, currentPerpustakaan) + 1);
			String mykode = LibraryUtil.generateCode(PemesananPengadaanItem.class, 8, "PO", currentPerpustakaan);
			kode.setValue(mykode);
			pemesananPengadaanItem.setKode(mykode);
			pemesananPengadaanItemDao.save(pemesananPengadaanItem);

			generateDetail(pemesananPengadaanItem);
		}
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PemesananPengadaanItem.class)
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
		List<PemesananPengadaanItem> pemesananPengadaanItem = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pemesananPengadaanItem);
		grid.setRowRenderer(new PemesananPengadaanItemRenderer());
		grid.setModelCheckMobile(strset);

	}

}
