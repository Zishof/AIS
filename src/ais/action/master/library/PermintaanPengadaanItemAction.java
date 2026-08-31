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
import ais.action.master.library.helper.PermintaanPengadaanItemDetailAction;
import ais.action.master.library.helper.PermintaanPengadaanItemPunyaItemHelper;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.PermintaanPengadaanItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.PermintaanPengadaanItem;
import ais.database.model.library.PermintaanPengadaanItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk permintaan pengadaan item. Tipe ini merupakan titik masuk UI yang
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
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initDetail()}, {@code
 * init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code
 * onSave()}); pelaporan/ekspor ({@code cetak()}); operasi domain lain ({@code onAdd()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PermintaanPengadaanItemAction extends GenericAutowireComposer {

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

	private PermintaanPengadaanItem permintaanPengadaanItem;
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cetak(PermintaanPengadaanItem permintaanPengadaanItem) throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();

		Common.insertProperty(PermintaanPengadaanItem.class, permintaanPengadaanItem, parameters, "");

		Session session = HibernateUtil.currentSession();
		List<PermintaanPengadaanItemDetail> permintaanPengadaanItemDetails = session
				.createCriteria(PermintaanPengadaanItemDetail.class)
				.add(Restrictions.eq("permintaanPengadaanItem", permintaanPengadaanItem)).list();
		List<Map> maps = new ArrayList<Map>();
		for (PermintaanPengadaanItemDetail permintaanPengadaanItemDetail : permintaanPengadaanItemDetails) {
			Map map = new HashMap();
			Common.insertProperty(PermintaanPengadaanItemDetail.class, permintaanPengadaanItemDetail, map, "");
			map.put("isbn", permintaanPengadaanItemDetail.getItem().getIsbn());
			map.put("isbn10", permintaanPengadaanItemDetail.getItem().getIsbn10());
			map.put("nama", permintaanPengadaanItemDetail.getItem().getNama());
			map.put("penyedia", permintaanPengadaanItemDetail.getPenyedia() == null ? ""
					: permintaanPengadaanItemDetail.getPenyedia().getNama());
			map.put("jumlah", permintaanPengadaanItemDetail.getJumlah());
			map.put("kode", permintaanPengadaanItem.getKode());
			map.put("status_persetujuan", permintaanPengadaanItem.getDisetujuiOleh() == null ? "Belum disetujui"
					: "Disetujui oleh " + permintaanPengadaanItem.getDisetujuiOleh().getUserNama() + " pada "
							+ (permintaanPengadaanItem.getTanggalPersetujuan() == null ? ""
									: Common.dateFormat1.get().format(permintaanPengadaanItem.getTanggalPersetujuan())));
			map.put("perpustakaan", permintaanPengadaanItem.getPerpustakaan() == null ? ""
					: permintaanPengadaanItem.getPerpustakaan().getNama());
			map.put("disetujui_oleh", permintaanPengadaanItem.getDisetujuiOleh() == null ? ""
					: permintaanPengadaanItem.getDisetujuiOleh().getNama());

			map.put("tanggal_persetujuan", permintaanPengadaanItem.getTanggalPersetujuan());

			maps.add(map);
		}
		parameters.put("maps", maps);

		parameters.put("id", permintaanPengadaanItem.getId());

		Report.generatePDFReport(Report.PDF, parameters, "library/permintaan_pengadaan",
				permintaanPengadaanItem.getTanggalPembuatan());
	}

	class PermintaanPengadaanItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PermintaanPengadaanItem permintaanPengadaanItem = (PermintaanPengadaanItem) arg1;

			final PermintaanPengadaanItemDetailAction detail;
			(detail = new PermintaanPengadaanItemDetailAction(permintaanPengadaanItem)).setParent(arg0);

			RevisiHelper.createNewRevisi(PermintaanPengadaanItem.class, permintaanPengadaanItem,
					permintaanPengadaanItem.getKode()).setParent(arg0);

			new Label(permintaanPengadaanItem.getPerpustakaan() == null ? ""
					: permintaanPengadaanItem.getPerpustakaan().getNama()).setParent(arg0);

			new Label(permintaanPengadaanItem.getDibuatOleh() == null ? ""
					: permintaanPengadaanItem.getDibuatOleh().getUserNama()).setParent(arg0);
			new Label(permintaanPengadaanItem.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(permintaanPengadaanItem.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(permintaanPengadaanItem.getDisetujuiOleh() == null ? ""
					: permintaanPengadaanItem.getDisetujuiOleh().getUserNama())).setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(permintaanPengadaanItem.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(permintaanPengadaanItem.getTanggalPersetujuan()))).setParent(arg0);
			new Label(permintaanPengadaanItem.getKeterangan()).setParent(arg0);

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons = new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Permintaan Pengadaan Item");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(permintaanPengadaanItem);
				}

			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && permintaanPengadaanItem.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && permintaanPengadaanItem.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Permintaan Pengadaan Item ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();
										Integer countItemjumlah = ((Number) session
												.createCriteria(PermintaanPengadaanItemDetail.class)
												.setProjection(Projections.count("id"))
												.add(Restrictions.eq("permintaanPengadaanItem",
														permintaanPengadaanItem))
												.add(Restrictions.lt("jumlah", 1.0)).uniqueResult()).intValue();

										if (!countItemjumlah.equals(0)) {
											MyMessageboxConfig.show("Lengkapilah jumlah !", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}

										// Integer count = ((Number)
										// session
										// .createCriteria(
										// PermintaanPengadaanItemDetail.class)
										// .setProjection(
										// Projections
										// .count("id"))
										// .add(Restrictions
										// .isNull("penyedia"))
										// .uniqueResult())
										// .intValue();
										//
										// if (!count.equals(0)) {
										// MyMessageboxConfig
										// .show("Lengkapilah data penyedia !",
										// "Peringatan",
										// MyMessageboxConfig.OK,
										// MyMessageboxConfig.EXCLAMATION);
										// return;
										// }

										permintaanPengadaanItem.setDisetujuiOleh(Common.getCurrentUser());
										permintaanPengadaanItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, permintaanPengadaanItem);

										disetujuiTanggal
												.setValue(permintaanPengadaanItem.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																permintaanPengadaanItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(permintaanPengadaanItem.getDisetujuiOleh() == null ? ""
												: permintaanPengadaanItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && permintaanPengadaanItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && permintaanPengadaanItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && permintaanPengadaanItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && permintaanPengadaanItem.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										final Timer timer = new Timer(500);
										timer.setParent(page.getFirstRoot());
										timer.addEventListener("onTimer", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												cetak(permintaanPengadaanItem);
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

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Permintaan Pengadaan Item ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										permintaanPengadaanItem.setDisetujuiOleh(null);
										permintaanPengadaanItem.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, permintaanPengadaanItem);

										disetujuiTanggal
												.setValue(permintaanPengadaanItem.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																permintaanPengadaanItem.getTanggalPersetujuan()));
										disetujuiOleh.setValue(permintaanPengadaanItem.getDisetujuiOleh() == null ? ""
												: permintaanPengadaanItem.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && permintaanPengadaanItem.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && permintaanPengadaanItem.getDisetujuiOleh() != null);
										rubah.setVisible(edit && permintaanPengadaanItem.getDisetujuiOleh() == null);
										hapus.setVisible(delete && permintaanPengadaanItem.getDisetujuiOleh() == null);
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
			rubah.setVisible(edit && permintaanPengadaanItem.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(permintaanPengadaanItem);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && permintaanPengadaanItem.getDisetujuiOleh() == null);
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

											PermintaanPengadaanItemDao permintaanPengadaanItemDao = DaoFactory
													.getInstance().getPermintaanPengadaanItemDao();

											Session session = permintaanPengadaanItemDao.getCurrentSession();
											List<PermintaanPengadaanItemDetail> permintaanPengadaanItemDetails = session
													.createCriteria(PermintaanPengadaanItemDetail.class)
													.add(Restrictions.eq("permintaanPengadaanItem",
															permintaanPengadaanItem))
													.list();
											for (PermintaanPengadaanItemDetail permintaanPengadaanItemDetail : permintaanPengadaanItemDetails) {
												session.delete(permintaanPengadaanItemDetail);
											}

											Common.refreshDelete(permintaanPengadaanItem);

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
		init(new PermintaanPengadaanItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final PermintaanPengadaanItem permintaanPengadaanItem, Component component)
			throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabDipinjam = new MyTabConfig("Item Permintaan Pengadaan");
		tabDipinjam.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDipinjam = new ais.ui.util.MyTabpanel();
		tabpanelDipinjam.setParent(tabpanels);
		tabpanelDipinjam.setWidth("100%");

		tabpanelDipinjam.appendChild(new PermintaanPengadaanItemPunyaItemHelper(gridItem = new MyGrid())
				.initDetail(permintaanPengadaanItem));

	}

	private void init(PermintaanPengadaanItem permintaanPengadaanItem) throws Exception {
		this.permintaanPengadaanItem = permintaanPengadaanItem;
		addWindow.setTitle(permintaanPengadaanItem.getId() == null ? "Tambah Permintaan Pengadaan Item" : "Ubah Permintaan Pengadaan Item");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("70%");

		initDetail(permintaanPengadaanItem, east);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Permintaan Pengadaan Item"));
		String mykode = permintaanPengadaanItem.getKode();

		row.appendChild(kode = new MyTextbox(
				permintaanPengadaanItem.getKode() == null ? mykode : permintaanPengadaanItem.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan"));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				permintaanPengadaanItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: permintaanPengadaanItem.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		;
		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new AmbilDataPerpustakaanBanbox());
		perpustakaan.setAttribute("perpustakaan", permintaanPengadaanItem.getPerpustakaan());
		perpustakaan.setValue(permintaanPengadaanItem.getPerpustakaan() == null ? ""
				: permintaanPengadaanItem.getPerpustakaan().toString());
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				currentPerpustakaan = (Perpustakaan) perpustakaan.getAttribute("perpustakaan");
				String mykode = LibraryUtil.generateCode(PermintaanPengadaanItem.class, 8, "PR", currentPerpustakaan);
				kode.setValue(mykode);
			}
		};
		perpustakaan.setEventListener(eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox(
				permintaanPengadaanItem.getKeterangan() == null ? "" : permintaanPengadaanItem.getKeterangan()));
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
			PermintaanPengadaanItemDetail permintaanPengadaanItemDetail = (PermintaanPengadaanItemDetail) row
					.getAttribute("permintaanPengadaanItemDetail");
			if (permintaanPengadaanItemDetail.getPenyedia() == null) {
				MyMessageboxConfig.show("Penyedia harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		PermintaanPengadaanItemDao permintaanPengadaanItemDao = DaoFactory.getInstance()
				.getPermintaanPengadaanItemDao();
		if (permintaanPengadaanItem.getId() != null) {
			permintaanPengadaanItem = permintaanPengadaanItemDao.load(permintaanPengadaanItem.getId());

		}

		permintaanPengadaanItem.setPerpustakaan((Perpustakaan) perpustakaan.getAttribute("perpustakaan"));
		permintaanPengadaanItem.setKode(kode.getValue());
		permintaanPengadaanItem.setKeterangan(keterangan.getValue());
		permintaanPengadaanItem.setTanggalPembuatan(tanggalPembuatan.getValue());

		if (permintaanPengadaanItem.getId() != null) {
			permintaanPengadaanItemDao.update(permintaanPengadaanItem);
		} else {
			permintaanPengadaanItem.setDibuatOleh(Common.getCurrentUser());

			currentPerpustakaan = (Perpustakaan) perpustakaan.getAttribute("perpustakaan");
			permintaanPengadaanItem.setIndex(
					LibraryUtil.generateMaxByPerpustakaan(PermintaanPengadaanItem.class, currentPerpustakaan) + 1);
			String mykode = LibraryUtil.generateCode(PermintaanPengadaanItem.class, 8, "PR", currentPerpustakaan);
			kode.setValue(mykode);
			permintaanPengadaanItem.setKode(mykode);
			permintaanPengadaanItemDao.save(permintaanPengadaanItem);
		}

		Session session = permintaanPengadaanItemDao.getCurrentSession();
		for (Row row : rowsItem) {
			PermintaanPengadaanItemDetail permintaanPengadaanItemDetail = (PermintaanPengadaanItemDetail) row
					.getAttribute("permintaanPengadaanItemDetail");
			permintaanPengadaanItemDetail.setPermintaanPengadaanItem(permintaanPengadaanItem);
			session.saveOrUpdate(permintaanPengadaanItemDetail);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PermintaanPengadaanItem.class)
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
		List<PermintaanPengadaanItem> permintaanPengadaanItem = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(permintaanPengadaanItem);
		grid.setRowRenderer(new PermintaanPengadaanItemRenderer());
		grid.setModelCheckMobile(strset);

	}

}
