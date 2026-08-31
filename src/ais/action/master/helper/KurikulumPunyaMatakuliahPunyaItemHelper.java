package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
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
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahPunyaItem;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoImagePerHalamanItem;
import ais.database.model.library.Item;
import ais.database.model.library.SaldoAwalDetail;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper tampilan "Daftar Referensi Perpustakaan" untuk satu mata kuliah dalam kurikulum
 * ({@link KurikulumPunyaMatakuliah}): menampilkan buku/pustaka ({@link Item}) yang dijadikan
 * referensi wajib/anjuran, lengkap dengan cover, ISBN/ISSN, pengarang, penerbit, catatan
 * (editable untuk staf non-mahasiswa), dan aksi per baris (lihat kutipan, baca via Google
 * Books, baca hasil scan per halaman, hapus). Referensi baru dapat ditambahkan lewat dua jalur:
 * memilih {@link Item} yang sudah ada di katalog pustaka ({@link AmbilDataItemBanyak}), atau
 * mengambil data langsung dari Google Books ({@link AmbilDataDariGoogleBookBanyak}).
 *
 * <p>
 * Dapat ditampilkan berdiri sendiri di komponen apa pun, atau khusus sebagai isi
 * {@link Tabpanel} — dalam mode tab, label tab ikut diperbarui dengan jumlah referensi
 * ({@link #loadData}), dan toolbar tambah data hanya tampil untuk pengguna non-mahasiswa dengan
 * hak {@link CommonPrivilages#CREATE}. Filter data dapat pula diperluas dengan klausa SQL
 * tambahan bebas lewat {@link #display(String, Component)} (parameter {@code sqltambahan}).
 * </p>
 *
 * <p>
 * Mengimplementasikan {@link DataLoader} agar {@link #loadData(Object)} dapat dipakai sebagai
 * callback penyegaran setelah aksi tambah/hapus selesai.
 * </p>
 */
public class KurikulumPunyaMatakuliahPunyaItemHelper implements DataLoader {

	private MyGrid grid;
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah;
	private boolean delete = false;
	private boolean add = false;
	private Component component;

	private Tbmuser tbmuser;
	private String sqltambahan = "false";

	/** Menentukan hak hapus/tambah pengguna saat ini lewat {@link CommonPrivilages} dan menyimpan referensi user login. */
	public KurikulumPunyaMatakuliahPunyaItemHelper() {
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Merender satu baris grid referensi: cover buku, ISBN/ISSN, riwayat revisi item (via
	 * {@link RevisiHelper}), pengarang, penerbit, info mata kuliah+prasyarat terkait, catatan
	 * (textbox editable untuk staf, label read-only untuk mahasiswa/siswa), dan tombol aksi
	 * (Kutipan, Google — hanya tampil bila item punya {@code googleBookId}, Baca — hanya tampil
	 * bila ada halaman hasil scan tersimpan, Hapus — hanya untuk staf non-mahasiswa dengan hak
	 * {@link #delete}).
	 */
	class DetailKurikulumPunyaMatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KurikulumPunyaMatakuliahPunyaItem kurikulumPunyaMatakuliahPunyaItem = (KurikulumPunyaMatakuliahPunyaItem) data;
			final Item item = kurikulumPunyaMatakuliahPunyaItem.getItem();
			Image image = LibraryUtil.generateImage(kurikulumPunyaMatakuliahPunyaItem.getItem());
			image.setWidth("100%");
			image.setParent(row);

			new Label(item.getIsbn() + " " + item.getIssn()).setParent(row);

			RevisiHelper.createNewRevisi(SaldoAwalDetail.class, kurikulumPunyaMatakuliahPunyaItem,
					kurikulumPunyaMatakuliahPunyaItem.getItem() == null ? ""
							: kurikulumPunyaMatakuliahPunyaItem.getItem().getNama())
					.setParent(row);

			new Label(item.getPengarangs()).setParent(row);

			new Label(item.getPenerbit() == null ? "" : item.getPenerbit().getNama()).setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliahPunyaItem
					.getKurikulumPunyaMatakuliah();
			if (kurikulumPunyaMatakuliah != null) {
				Kurikulum kurikulum = kurikulumPunyaMatakuliah.getKurikulum();
				Vbox a = RevisiHelper.createNewRevisi(KurikulumPunyaMatakuliah.class, kurikulumPunyaMatakuliah,
						kurikulumPunyaMatakuliah.getMatakuliah().getKode() + "-"
								+ kurikulumPunyaMatakuliah.getMatakuliah().getNama() + " "
								+ kurikulumPunyaMatakuliah.getMatakuliah().getSks() + " sks "
								+ (kurikulum == null ? "" : " (Kurikulum:" + kurikulum.getTahun() + ")"));
				a.setParent(vbox);

				MatakuliahPrasyaratAction.tampilPrasyarat(a, kurikulumPunyaMatakuliah.getMatakuliah());
			}

			if (tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null) {
				final Textbox keterangan = new Textbox(kurikulumPunyaMatakuliahPunyaItem.getKeterangan());
				keterangan.setRows(2);
				keterangan.setWidth("97%");
				keterangan.setParent(vbox);
				keterangan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kurikulumPunyaMatakuliahPunyaItem.setKeterangan(keterangan.getValue().trim());
						Common.refreshUpdate(kurikulumPunyaMatakuliahPunyaItem);

					}
				});
			} else {
				new Label(kurikulumPunyaMatakuliahPunyaItem.getKeterangan()).setParent(vbox);
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KurikulumPunyaMatakuliahPunyaItemHelper.java:176");

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
			button.setVisible(Common.getCurrentUser().getMahasiswa() == null && delete);
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
											Common.refreshDelete(kurikulumPunyaMatakuliahPunyaItem);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
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
	 * Memuat/menyegarkan grid dengan seluruh {@link KurikulumPunyaMatakuliahPunyaItem} milik
	 * {@link #kurikulumPunyaMatakuliah} (atau, bila {@code null}, dengan klausa SQL tambahan
	 * {@link #sqltambahan} sebagai filter — default {@code "false"} yaitu tidak menampilkan apa
	 * pun). Bila komponen tuan rumah adalah {@link Tabpanel}, label tabnya ikut diperbarui
	 * dengan jumlah referensi dalam kurung.
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<KurikulumPunyaMatakuliahPunyaItem> kurikulumPunyaMatakuliahPunyaItem = session
				.createCriteria(KurikulumPunyaMatakuliahPunyaItem.class).addOrder(Order.asc("id"))
				.add(kurikulumPunyaMatakuliah == null ? Restrictions.sqlRestriction(sqltambahan)
						: Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah))
				.list();

		if (component instanceof Tabpanel) {
			((Tabpanel) component).getLinkedTab()
					.setLabel("Buku Referensi " + (kurikulumPunyaMatakuliahPunyaItem.size() == 0 ? ""
							: "(" + kurikulumPunyaMatakuliahPunyaItem.size() + ")"));
		}

		ListModel strset = new SimpleListModel(kurikulumPunyaMatakuliahPunyaItem);
		grid.setRowRenderer(new DetailKurikulumPunyaMatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Varian {@link #display(KurikulumPunyaMatakuliah, Component)} yang menyaring data lewat
	 * klausa SQL tambahan bebas ({@code sqltambahan}) alih-alih relasi
	 * {@link KurikulumPunyaMatakuliah} langsung — dipakai saat kurikulum-matakuliah belum
	 * ditentukan tetapi daftar referensi tetap perlu difilter secara kustom.
	 */
	public void display(final String sqltambahan, final Component component) {
		this.sqltambahan = sqltambahan;
		display(kurikulumPunyaMatakuliah, component);
	}

	/**
	 * Membangun tampilan daftar referensi ke dalam {@code component}: bila {@code component}
	 * berupa {@link Tabpanel}, dibungkus dalam groupbox berjudul "Daftar Referensi
	 * Perpustakaan" lengkap dengan toolbar "Ambil Referensi" (dari katalog pustaka) dan "Ambil
	 * Google Book" (dari Google Books) — keduanya hanya tampil untuk staf non-mahasiswa dengan
	 * hak {@link #add}; selain itu, hanya grid yang dipasang langsung. Kedua alur tambah data
	 * membuka jendela modal pemilihan lalu menyimpan hasil terpilih sebagai baris
	 * {@link KurikulumPunyaMatakuliahPunyaItem} baru dan menyegarkan grid.
	 *
	 * @param kurikulumPunyaMatakuliah mata kuliah dalam kurikulum yang referensinya ditampilkan
	 * @param component                kontainer ZK yang akan diisi (isi sebelumnya dibersihkan)
	 */
	public void display(final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah, final Component component) {
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliah;
		Common.clear(component);
		this.component = component;

		if (component instanceof Tabpanel) {

			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			groupbox.setParent(component);
			groupbox.appendChild(new MyCaptionStyled("Daftar Referensi Perpustakaan"));

			Tbmuser tbmuser = Common.getCurrentUser();

			Toolbar toolbar = new Toolbar();
			toolbar.setVisible(
					tbmuser != null && kurikulumPunyaMatakuliah != null && tbmuser.getMahasiswa() == null && add);
			// toolbar.setHeight("25px");
			toolbar.setParent(groupbox);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Referensi", "/img/new.gif");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					List<Item> items = HibernateUtil.currentSession()
							.createCriteria(KurikulumPunyaMatakuliahPunyaItem.class)
							.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah))
							.setProjection(Projections.property("item")).list();
					AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
					ambilDataItemBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Item> items = (List<Item>) arg0.getData();
							for (Item item : items) {
								KurikulumPunyaMatakuliahPunyaItem kurikulumPunyaMatakuliahPunyaItem = new KurikulumPunyaMatakuliahPunyaItem();
								kurikulumPunyaMatakuliahPunyaItem.setItem(item);
								kurikulumPunyaMatakuliahPunyaItem.setKeterangan("");
								kurikulumPunyaMatakuliahPunyaItem.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
								Common.refreshSaveOrUpdate(kurikulumPunyaMatakuliahPunyaItem);
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
			toolbar.setVisible(
					tbmuser != null && kurikulumPunyaMatakuliah != null && tbmuser.getMahasiswa() == null && add);
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					AmbilDataDariGoogleBookBanyak ambilDataDariGoogleBookBanyak = new AmbilDataDariGoogleBookBanyak(
							kurikulumPunyaMatakuliah != null && kurikulumPunyaMatakuliah.getMatakuliah() != null
									? kurikulumPunyaMatakuliah.getMatakuliah().getNama() : "");
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
								KurikulumPunyaMatakuliahPunyaItem kurikulumPunyaMatakuliahPunyaItem = new KurikulumPunyaMatakuliahPunyaItem();
								kurikulumPunyaMatakuliahPunyaItem.setItem(item);
								kurikulumPunyaMatakuliahPunyaItem.setKeterangan("");
								kurikulumPunyaMatakuliahPunyaItem.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
								Common.refreshSaveOrUpdate(kurikulumPunyaMatakuliahPunyaItem);
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

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);grid.getPagingChild().setMold("os");
			grid.setParent(groupbox);

		} else {
			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);grid.getPagingChild().setMold("os");
			grid.setParent(component);
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
		column.setWidth("15%");

		loadData(null);

	}

}
