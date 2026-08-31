package ais.action.master.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import com.google.api.services.books.model.Volume;

import ais.action.master.MatakuliahPrasyaratAction;
import ais.action.master.library.ItemAction;
import ais.action.master.library.helper.AmbilDataDariGoogleBookBanyak;
import ais.action.master.library.helper.AmbilDataItemBanyak;
import ais.action.master.library.helper.TampilanHasilScanPerHalamanWindow;
import ais.action.master.library.util.LibraryUtil;
import ais.action.servlet.CheckISBN;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Kurikulum;
import ais.database.model.Perkuliahan;
import ais.database.model.PerkuliahanPunyaItem;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoImagePerHalamanItem;
import ais.database.model.library.Item;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper "Daftar Referensi Perpustakaan" untuk satu {@link Perkuliahan} (kelas kuliah),
 * mengelola baris {@link PerkuliahanPunyaItem} yang menautkan buku/{@link Item}
 * perpustakaan sebagai referensi kelas tersebut. Menyediakan pencarian (pengarang/judul/
 * ISBN), pengambilan item dari katalog internal ({@link AmbilDataItemBanyak}) atau dari
 * Google Books ({@link AmbilDataDariGoogleBookBanyak} — item yang belum ada di katalog
 * otomatis dibuat via {@link CheckISBN}), tampilan kutipan, pembacaan buku via Google
 * Play Books atau viewer scan per halaman internal (bila tersedia), catatan per item, dan
 * penghapusan.
 *
 * <p>
 * {@link #display} mendukung tiga mode tata letak yang isinya sebagian besar duplikat:
 * sebagai isi {@link Tabpanel} (groupbox mandiri dengan toolbar+grid+paging), sebagai isi
 * region {@link Center} pada borderlayout (toolbar ke {@code North}, paging ke
 * {@code South} milik parent), atau kontainer generik lainnya (grid+paging langsung
 * dipasang ke {@code component}, tanpa toolbar bawaan). Ukuran halaman grid tetap KECIL
 * (3 baris/halaman) di semua mode karena setiap baris menampilkan detail buku yang cukup
 * besar (gambar sampul, kutipan, dsb). {@link #loadData} menangani putusnya koneksi
 * database ({@link org.hibernate.exception.JDBCConnectionException}/
 * {@link org.hibernate.exception.GenericJDBCException}) secara non-fatal dengan pesan
 * alert, bukan membiarkan UI crash.
 * </p>
 */
public class PerkuliahanPunyaItemHelper implements DataLoader {

	private Grid grid;
	private Perkuliahan perkuliahan;
	private Tbmuser tbmuser;
	private String sqltambahan = "false";
	private Textbox cari;

	private Paging paging;

	/** Membuat helper dan mengambil pengguna yang sedang login ke {@link #tbmuser} (dipakai untuk kontrol visibilitas tombol/field). */
	public PerkuliahanPunyaItemHelper() {
		tbmuser = Common.getCurrentUser();
	}

	/** Perender baris grid: sampul dan detail buku, form katalog (mode "semua perkuliahan"), catatan yang dapat diedit, dan tombol aksi (kutipan, baca via Google/scan internal, hapus). */
	class DetailPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris {@link PerkuliahanPunyaItem}: sampul buku, ISBN/ISSN,
		 * nama/judul dengan riwayat revisi, pengarang, penerbit; bila helper dipakai
		 * dalam mode "lintas perkuliahan" ({@link #perkuliahan} {@code null}), juga
		 * menampilkan info dosen dan detail kelas pemilik baris tersebut; catatan yang
		 * dapat diedit langsung (staf) atau label saja (mahasiswa/siswa); serta tombol
		 * Kutipan, "Google" (buka preview via Google Play Books, hanya bila item punya
		 * {@code googleBookId}), "Baca" (viewer scan per halaman internal, hanya bila
		 * ada halaman ter-scan), dan hapus (staf saja, dengan konfirmasi dan pesan
		 * galat ramah bila gagal karena relasi data).
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PerkuliahanPunyaItem perkuliahanPunyaItem = (PerkuliahanPunyaItem) data;
			final Item item = perkuliahanPunyaItem.getItem();
			Image image = LibraryUtil.generateImage(perkuliahanPunyaItem.getItem());
			image.setWidth("100%");
			image.setParent(row);

			new Label(item.getIsbn() + " " + item.getIssn()).setParent(row);

			RevisiHelper
					.createNewRevisi(PerkuliahanPunyaItem.class, perkuliahanPunyaItem,
							perkuliahanPunyaItem.getItem() == null ? "" : perkuliahanPunyaItem.getItem().getNama())
					.setParent(row);

			new Label(item.getPengarangs()).setParent(row);

			new Label(item.getPenerbit() == null ? "" : item.getPenerbit().getNama()).setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			if (perkuliahan == null) {
				ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(vbox, perkuliahanPunyaItem.getPerkuliahan(), true);
				Kurikulum kurikulum = perkuliahanPunyaItem.getPerkuliahan().getKurikulum();
				Vbox a = RevisiHelper.createNewRevisi(Perkuliahan.class, perkuliahanPunyaItem.getPerkuliahan(),
						perkuliahanPunyaItem.getPerkuliahan().getMatakuliah().getKode() + "-"
								+ perkuliahanPunyaItem.getPerkuliahan().getMatakuliah().getNama() + " "
								+ perkuliahanPunyaItem.getPerkuliahan().getMatakuliah().getSks() + " sks "
								+ (kurikulum == null ? "" : " (Kurikulum:" + kurikulum.getTahun() + ")"));
				a.setParent(vbox);

				MatakuliahPrasyaratAction.tampilPrasyarat(a, perkuliahanPunyaItem.getPerkuliahan().getMatakuliah());
				ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(vbox, perkuliahanPunyaItem.getPerkuliahan());
			}

			if (tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null) {
				vbox.appendChild(new MyLabelBoldAja("Masukkan catatan :"));
				final Textbox keterangan = new Textbox(perkuliahanPunyaItem.getKeterangan());
				keterangan.setRows(2);
				keterangan.setWidth("97%");
				keterangan.setParent(vbox);
				keterangan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						perkuliahanPunyaItem.setKeterangan(keterangan.getValue().trim());
						Common.refreshUpdate(perkuliahanPunyaItem);

					}
				});
			} else {
				new Label(perkuliahanPunyaItem.getKeterangan()).setParent(vbox);
			}

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Kutipan", "/img/eye-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Kutipan Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ItemAction.tampilkanKutipan(item);
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Google", "/img/Apps-Google-Play-Books-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Baca Buku via Google");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					JSONObject jsonObject = new JSONObject(item.getInfoLain()).getJSONObject("volumeInfo");
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(jsonObject.getString("previewLink"), "_blank");
					} else {
						Clients.evalJavaScript("popupCenter({url: '" + jsonObject.getString("previewLink")
								+ "', title: 'Book', w: 1200, h: 600});");
					}

				}

			});
			aksiButtons.add(button);
			button.setVisible(item.getGoogleBookId() != null && !item.getGoogleBookId().trim().isEmpty());

			button = new MyToolbarbuttonConfig("Baca", "/img/Book-icon.png");
			button.setOrient("vertical");
			button.setOrient("vertical");
			button.setTooltiptext("Lihat Isi Buku");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					TampilanHasilScanPerHalamanWindow halamanWindow = new TampilanHasilScanPerHalamanWindow("Isi Buku",
							"none", true);

					halamanWindow.init(item);
					try {
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(halamanWindow);
						halamanWindow.onModal();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanPunyaItemHelper.java:183");

					}
				}

			});
			aksiButtons.add(button);

			Session session = StreamingHibernateUtil.getInstance().currentSession();

			int qty = ((Number) session.createCriteria(FotoImagePerHalamanItem.class)
					.add(Restrictions.eq("item", item.getId())).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();

			StreamingHibernateUtil.getInstance().closeSession();

			button.setVisible(qty > 0);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(Common.getCurrentUser().getMahasiswa() == null);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(perkuliahanPunyaItem);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"menghapus item perkuliahan ini",
													e,
													new String[] {
															"Periksa apakah item perkuliahan ini masih berelasi dengan data lain (misalnya data KRS atau nilai) sehingga tidak dapat dihapus.",
															"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
															"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
										}

									}

								}
							});

				}

			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);

		}

	}

	/**
	 * Membangun kriteria pencarian {@link PerkuliahanPunyaItem}: bila {@link #perkuliahan}
	 * terisi, dibatasi ke perkuliahan tersebut; bila {@code null}, memakai
	 * {@link #sqltambahan} sebagai kondisi SQL bebas (mode lintas perkuliahan). Disaring
	 * opsional oleh kata kunci pencarian pada pengarang/nama/ISBN item.
	 *
	 * @param order tambahkan pengurutan berdasarkan id menaik bila {@code true}
	 * @return kriteria Hibernate atas {@link PerkuliahanPunyaItem}
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteri = session.createCriteria(PerkuliahanPunyaItem.class)

				.createAlias("item", "item")
				.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("item.pengarangs", cari.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("item.nama", cari.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("item.isbn", cari.getValue().trim(), MatchMode.ANYWHERE))))

				.add(perkuliahan == null ? Restrictions.sqlRestriction(sqltambahan)
						: Restrictions.eq("perkuliahan", perkuliahan));

		if (order) {
			criteri.addOrder(Order.asc("id"));
		}
		return criteri;
	}

	/**
	 * Memuat ulang daftar {@link PerkuliahanPunyaItem} sesuai
	 * {@link #initCriteria(boolean)}, dengan paging 3 baris/halaman. Kegagalan koneksi
	 * database ({@link org.hibernate.exception.JDBCConnectionException}/
	 * {@link org.hibernate.exception.GenericJDBCException}) ditangkap dan ditampilkan
	 * sebagai alert non-fatal, bukan dibiarkan menggagalkan seluruh render halaman.
	 *
	 * @param value tidak digunakan (parameter kontrak {@link DataLoader})
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		try {
			Common.initPagingCustom(initCriteria(false), paging, 3);
			List<PerkuliahanPunyaItem> perkuliahanPunyaItem = initCriteria(true).setMaxResults(3)
					.setFirstResult(3 * (paging == null ? 0 : paging.getActivePage())).list();

			ListModel strset = new SimpleListModel(perkuliahanPunyaItem);
			grid.setRowRenderer(new DetailPerkuliahanRenderer());
			grid.setModel(strset);
		} catch (org.hibernate.exception.JDBCConnectionException connEx) {
			// Koneksi DB terputus (biasanya maintenance tengah malam); jangan crash UI
			Common.tampilErrorJikaAdmin(connEx);
			// ZK 5.5 Clients tidak punya showNotification; pakai alert (tetap informatif, non-fatal).
			Clients.alert("Koneksi database terputus. Silakan muat ulang halaman.");
		} catch (org.hibernate.exception.GenericJDBCException jdbcEx) {
			// Koneksi diputus paksa oleh admin PostgreSQL
			Common.tampilErrorJikaAdmin(jdbcEx);
			// ZK 5.5 Clients tidak punya showNotification; pakai alert (tetap informatif, non-fatal).
			Clients.alert("Koneksi database bermasalah. Silakan muat ulang halaman.");
		}
	}

	/**
	 * Menampilkan helper dalam mode "lintas perkuliahan" (tanpa satu {@link Perkuliahan}
	 * spesifik): {@code sqltambahan} menjadi satu-satunya kondisi filter kepemilikan
	 * baris {@link PerkuliahanPunyaItem} yang ditampilkan.
	 *
	 * @param sqltambahan kondisi SQL bebas untuk membatasi baris yang ditampilkan
	 * @param component   kontainer ZK yang akan diisi (dibersihkan lebih dulu)
	 */
	public void display(final String sqltambahan, final Component component) {
		this.sqltambahan = sqltambahan;
		display(perkuliahan, component);
	}

	/**
	 * Membangun tampilan "Daftar Referensi Perpustakaan" untuk {@code perkuliahan} ke
	 * dalam {@code component}. Tata letak menyesuaikan tipe {@code component} — lihat
	 * javadoc kelas untuk ketiga mode ({@link Tabpanel}/{@link Center}/generik). Toolbar
	 * (bila ada) berisi "Ambil Referensi" (staf non-mahasiswa, membuka
	 * {@link AmbilDataItemBanyak}), "Ambil Google Book" (membuka
	 * {@link AmbilDataDariGoogleBookBanyak}, judul mata kuliah dipakai sebagai kata
	 * kunci pencarian awal), dan kotak pencarian.
	 *
	 * @param perkuliahan kelas kuliah yang referensinya dikelola; {@code null} untuk mode lintas perkuliahan (memakai {@link #sqltambahan})
	 * @param component   kontainer ZK yang akan diisi (dibersihkan lebih dulu bila tidak {@code null})
	 */
	public void display(final Perkuliahan perkuliahan, final Component component) {
		this.perkuliahan = perkuliahan;
		if (component != null) {
			Common.clear(component);
		}
		tbmuser = Common.getCurrentUser();
		cari = new Textbox();
		if (component instanceof Tabpanel) {

			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			groupbox.setParent(component);
			groupbox.appendChild(new MyCaptionStyled("Daftar Referensi Perpustakaan"));

			Toolbar toolbar = new Toolbar();

			toolbar.setParent(groupbox);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Referensi", "/img/new.gif");
			button.setVisible(tbmuser != null && perkuliahan != null && tbmuser.getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					List<Item> items = HibernateUtil.currentSession().createCriteria(PerkuliahanPunyaItem.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan))
							.setProjection(Projections.property("item")).list();
					AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
					ambilDataItemBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Item> items = (List<Item>) arg0.getData();
							for (Item item : items) {
								PerkuliahanPunyaItem perkuliahanPunyaItem = new PerkuliahanPunyaItem();
								perkuliahanPunyaItem.setItem(item);
								perkuliahanPunyaItem.setKeterangan("");
								perkuliahanPunyaItem.setPerkuliahan(perkuliahan);
								Common.refreshSaveOrUpdate(perkuliahanPunyaItem);
							}

							loadData(null);
						}
					});
					ambilDataItemBanyak.setWidth("97%");
					ambilDataItemBanyak.setHeight("97%");
					ambilDataItemBanyak.setVisible(true);
					ambilDataItemBanyak.onModal();

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Ambil Google Book", "/img/Apps-Google-Play-Books-icon.png");
			button.setVisible(tbmuser != null && perkuliahan != null && tbmuser.getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					AmbilDataDariGoogleBookBanyak ambilDataDariGoogleBookBanyak = new AmbilDataDariGoogleBookBanyak(
							perkuliahan != null && perkuliahan.getMatakuliah() != null
									? perkuliahan.getMatakuliah().getNama()
									: "");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
							.appendChild(ambilDataDariGoogleBookBanyak);
					ambilDataDariGoogleBookBanyak.setHeight("95%");
					ambilDataDariGoogleBookBanyak.setWidth("90%");

					ambilDataDariGoogleBookBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Object> objects = (List<Object>) arg0.getData();
							for (Object object : objects) {
								Item item = (object instanceof Item) ? (Item) object
										: (object instanceof ais.database.model.library.ItemTemporary)
											? CheckISBN.itemDariItemTemporary((ais.database.model.library.ItemTemporary) object)
											: CheckISBN.simpanVolume((Volume) object, new Item());
								PerkuliahanPunyaItem perkuliahanPunyaItem = new PerkuliahanPunyaItem();
								perkuliahanPunyaItem.setItem(item);
								perkuliahanPunyaItem.setKeterangan("");
								perkuliahanPunyaItem.setPerkuliahan(perkuliahan);
								Common.refreshSaveOrUpdate(perkuliahanPunyaItem);
							}

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(null);
								}
							});
						}
					});

					ambilDataDariGoogleBookBanyak.onModal();

				}

			});
			button.setParent(toolbar);

			toolbar.appendChild(new Space());
			toolbar.appendChild(new MyLabelConfig("Cari : "));
			toolbar.appendChild(cari);
			cari.setCols(15);
			cari.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});
			button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});
			button.setParent(toolbar);

			paging = new Paging();
			Common.initPagingCustom(paging, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			}, 3);
			groupbox.appendChild(paging);

			grid = new Grid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);
			grid.getPagingChild().setMold("os");
			grid.setParent(groupbox);

		} else if (component instanceof Center) {

			North north = new North();
			north.setParent(component.getParent());
			ais.ui.util.ZkCompat.setFlex(north, true);
			north.setHeight("25px");

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(north);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Referensi", "/img/new.gif");
			button.setVisible(tbmuser != null && perkuliahan != null && tbmuser.getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					List<Item> items = HibernateUtil.currentSession().createCriteria(PerkuliahanPunyaItem.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan))
							.setProjection(Projections.property("item")).list();
					AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
					ambilDataItemBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Item> items = (List<Item>) arg0.getData();
							for (Item item : items) {
								PerkuliahanPunyaItem perkuliahanPunyaItem = new PerkuliahanPunyaItem();
								perkuliahanPunyaItem.setItem(item);
								perkuliahanPunyaItem.setKeterangan("");
								perkuliahanPunyaItem.setPerkuliahan(perkuliahan);
								Common.refreshSaveOrUpdate(perkuliahanPunyaItem);
							}

							loadData(null);
						}
					});
					ambilDataItemBanyak.setWidth("97%");
					ambilDataItemBanyak.setHeight("97%");
					ambilDataItemBanyak.setVisible(true);
					ambilDataItemBanyak.onModal();

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Ambil Google Book", "/img/Apps-Google-Play-Books-icon.png");
			button.setVisible(tbmuser != null && perkuliahan != null && tbmuser.getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					AmbilDataDariGoogleBookBanyak ambilDataDariGoogleBookBanyak = new AmbilDataDariGoogleBookBanyak(
							perkuliahan != null && perkuliahan.getMatakuliah() != null
									? perkuliahan.getMatakuliah().getNama()
									: "");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
							.appendChild(ambilDataDariGoogleBookBanyak);
					ambilDataDariGoogleBookBanyak.setHeight("95%");
					ambilDataDariGoogleBookBanyak.setWidth("90%");

					ambilDataDariGoogleBookBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Object> objects = (List<Object>) arg0.getData();
							for (Object object : objects) {
								Item item = (object instanceof Item) ? (Item) object
										: (object instanceof ais.database.model.library.ItemTemporary)
											? CheckISBN.itemDariItemTemporary((ais.database.model.library.ItemTemporary) object)
											: CheckISBN.simpanVolume((Volume) object, new Item());
								PerkuliahanPunyaItem perkuliahanPunyaItem = new PerkuliahanPunyaItem();
								perkuliahanPunyaItem.setItem(item);
								perkuliahanPunyaItem.setKeterangan("");
								perkuliahanPunyaItem.setPerkuliahan(perkuliahan);
								Common.refreshSaveOrUpdate(perkuliahanPunyaItem);
							}

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(null);
								}
							});
						}
					});

					ambilDataDariGoogleBookBanyak.onModal();

				}

			});
			button.setParent(toolbar);

			toolbar.appendChild(new Space());
			toolbar.appendChild(new MyLabelConfig("Cari : "));
			toolbar.appendChild(cari);
			cari.setCols(15);
			cari.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});
			button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});
			button.setParent(toolbar);

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);
			grid.getPagingChild().setMold("os");
			grid.setParent(component);

			paging = new Paging();
			Common.initPagingCustom(paging, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			}, 3);
			South south = ((Borderlayout) component.getParent()).getSouth() == null ? new South()
					: ((Borderlayout) component.getParent()).getSouth();
			south.setParent(component.getParent());
			south.appendChild(paging);

		} else {
			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);
			grid.getPagingChild().setMold("os");
			grid.setParent(component);

			paging = new Paging();
			Common.initPagingCustom(paging, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			}, 3);
			component.appendChild(paging);
		}

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/ISBN/ISSN");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pengarang");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penerbit");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Catatan");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(tbmuser == null ? "0%" : "15%");

		loadData(null);

	}

}
