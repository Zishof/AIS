package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.file.FotoImagePerHalamanItem;
import ais.database.model.library.Item;
import ais.database.model.library.JenisItem;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data item banyak. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code List items},
 * {@code List itemsHanyaDitampilkan}, {@code Boolean ddc}, {@code Boolean udc}, {@code Set ids};
 * inisialisasi/lifecycle ({@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}, {@code
 * onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code
 * display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataItemBanyak extends MyWindow implements DataCriteria {

	/**
	 *  
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<Item> items;
	private List<Item> itemsHanyaDitampilkan;
	private Boolean ddc = false;
	private Boolean udc = false;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataItemBanyak(List<Item> items, Boolean ddc, Boolean udc) {
		super();
		this.ddc = ddc;
		this.udc = udc;
		this.items = items;
		display();
		onSearchDefault(null);
	}

	public AmbilDataItemBanyak(List<Item> items) {
		super();
		this.items = items;
		display();
		onSearchDefault(null);
	}

	public AmbilDataItemBanyak(List<Item> items, JenisItem jenisItem) {
		super();
		this.items = items;
		display();
		onSearchDefault(null);
	}

	public AmbilDataItemBanyak(List<Item> items, List<Item> itemsHanyaDitampilkan) {
		super();
		this.items = items;
		this.itemsHanyaDitampilkan = itemsHanyaDitampilkan;
		display();

		onSearchDefault(null);

	}

	private MyTextbox kodeIteman;
	private MyTextbox nama;
	private MyTextbox pengarang;
	private MyTextbox penerbit;
	private MyTextbox kategori;

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataItemBanyak}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataItemBanyak} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataItemBanyak
	 */
	class ItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Item item = (Item) arg1;
			arg0.setAttribute("item", item);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (Item myItem : items) {
				if (myItem.getId().equals(item.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(item.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(item.getId());
					} else {
						ids.remove(item.getId());
					}
				}
			});

			if (udc && item.getUdcItem() != null) {
				checkbox.setVisible(false);
			}

			if (ddc && item.getDdcItem() != null) {
				checkbox.setVisible(false);
			}

			Image image = LibraryUtil.generateImage(item);
			image.setWidth("100%");
			image.setParent(arg0);

			new Label(item.getIsbn()).setParent(arg0);
			new Label(item.getNama()).setParent(arg0);
			new Label(item.getKategories()).setParent(arg0);
			new Label(item.getPenerbit() == null ? "" : item.getPenerbit().getNama()).setParent(arg0);
			new Label(item.getPengarangs()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Google", "/img/Apps-Google-Play-Books-icon.png");
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
			button.setParent(toolbar);
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataItemBanyak.java:197");

					}
				}

			});
			button.setParent(toolbar);

			Session session = StreamingHibernateUtil.getInstance().currentSession();

			int qty = ((Number) session.createCriteria(FotoImagePerHalamanItem.class)
					.add(Restrictions.eq("item", item.getId())).setProjection(Projections.rowCount()).uniqueResult())
							.intValue();

			StreamingHibernateUtil.getInstance().closeSession();

			button.setVisible(qty > 0);
		}

	}

	public void display() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("130px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("ISBN"));
		row.appendChild(kodeIteman = new MyTextbox());
		kodeIteman.setWidth("90%");
		kodeIteman.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pengarang"));
		row.appendChild(pengarang = new MyTextbox());
		pengarang.setWidth("90%");
		pengarang.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit"));
		row.appendChild(penerbit = new MyTextbox());
		penerbit.setWidth("90%");
		penerbit.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori"));
		row.appendChild(kategori = new MyTextbox());
		kategori.setWidth("90%");
		kategori.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		// Pager tunggal via AmbilDataPagingHelper; pager manual dihapus (double paging).

		grid = new Grid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* setPageSize legacy dihapus: grid bukan mold "paging" sehingga setPageSize melempar IllegalStateException ("Available only the paging mold") dan daftar tidak pernah tampil. Paging ditangani AmbilDataPagingHelper. */
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
		pagingHelper.pasangGridDanPaging(myCenter1, grid);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/Isbn");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kategori");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penerbit");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pengarang");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataItemBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Item> items = new ArrayList<Item>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								Item myItem = (Item) row.getAttribute("item");
								items.add(myItem);
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/library/helper/AmbilDataItemBanyak.java:414");
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), items);
					eventListener.onEvent(myEvent);
				}
				AmbilDataItemBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (itemsHanyaDitampilkan != null) {
			for (Item item : itemsHanyaDitampilkan) {
				values.add(item.getId());
			}
		}

		List<Long> valuesNot = new ArrayList<Long>();
		if (items != null) {
			for (Item item : items) {
				valuesNot.add(item.getId());
			}
		}

		Criteria criteria = session.createCriteria(Item.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNull("defaultSatuanKerja"))

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))

				.add(valuesNot.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", valuesNot)))

				.add(itemsHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.createAlias("penerbit", "penerbit", Criteria.LEFT_JOIN)

				.add(penerbit.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("penerbit.nama", penerbit.getValue().trim(), MatchMode.ANYWHERE))

				.add(kategori.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kategories", kategori.getValue().trim(), MatchMode.ANYWHERE))

				.add(pengarang.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("pengarangs", pengarang.getValue().trim(), MatchMode.ANYWHERE))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(kodeIteman.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("isbn", kodeIteman.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("isbn10", kodeIteman.getValue().trim(), MatchMode.ANYWHERE)));
		if (order) {
			criteria.addOrder(Order.asc("nama"));
		}

		return criteria;
	}

	public void onSearchDefault(Event event) {
		onSearchDefault(event, false);
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event, final Boolean dontLoop) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (itemsHanyaDitampilkan != null) {
			for (Item item : itemsHanyaDitampilkan) {
				values.add(item.getId());
			}
		}

		List<Item> item = pagingHelper.cariDenganCriteria(session.createCriteria(Item.class).addOrder(Order.asc("nama"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)), Item.class);

		final List<Item> myItem = pagingHelper.cariDenganCriteria(initCriteria(true), Item.class);

		item.addAll(myItem);

		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new ItemRenderer());
		grid.setModel(strset);

		if (Common.bolehKonfigurasi("terintegrasi_dengan_google_book_baru", Konfigurasi.TIDAK_AKTIF)) {
			if (!dontLoop) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String isbn = kodeIteman.getValue().trim();
						isbn = org.apache.commons.lang3.StringUtils.replace(isbn, "-", "");
						String judul = nama.getValue().trim();
						String tahun = "_";
						String keyword = "_";
						String catatan = "_";
						String pengarang = AmbilDataItemBanyak.this.pengarang.getValue().trim();
						String penerbit = AmbilDataItemBanyak.this.penerbit.getValue().trim();
						String kategori = AmbilDataItemBanyak.this.kategori.getValue().trim();

						LibraryUtil.cariDiGoogleBook(isbn, judul, keyword, catatan, pengarang, penerbit, kategori,
								tahun, AmbilDataItemBanyak.this, pagingHelper.getPaging(), myItem, null);
					}
				}, "Mencoba mencari buku ke google book, harap menunggu, setelah beberapa saat, klik tombol cari lagi untuk me-load ulang buku pencarian",
						false, 2500);
			}
		}

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
