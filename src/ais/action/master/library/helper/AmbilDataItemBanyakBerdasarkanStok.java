package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.JenisItem;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyTextbox;

/**
 * Tipe khusus untuk ambil data item banyak berdasarkan stok. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code EventListener
 * eventListener}, {@code List items}, {@code Perpustakaan perpustakaan}, {@code Set ids}, {@code String
 * orderBy}, {@code Boolean disableStokHabis}, {@code MyTextbox kodeIteman}; pembacaan/pencarian ({@code
 * onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code
 * display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataItemBanyakBerdasarkanStok extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	private EventListener eventListener;
	private List<Item> items;
	private Perpustakaan perpustakaan;

	private Set<Long> ids = new HashSet<Long>();

	private String orderBy = "stok";
	private Boolean disableStokHabis;

	public AmbilDataItemBanyakBerdasarkanStok(List<Item> items, Perpustakaan perpustakaan, Boolean disableStokHabis) {
		this(items, perpustakaan, null, disableStokHabis);
	}

	public AmbilDataItemBanyakBerdasarkanStok(List<Item> items, Perpustakaan perpustakaan, String orderBy,
			Boolean disableStokHabis) {
		super();
		this.disableStokHabis = disableStokHabis;
		this.items = items;
		this.perpustakaan = perpustakaan;
		if (orderBy != null) {
			this.orderBy = orderBy;
		}
		display();

		onSearchDefault(null);
	}

	private MyTextbox kodeIteman;
	private MyTextbox nama;
	private Combobox jenisItem;

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataItemBanyakBerdasarkanStok}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataItemBanyakBerdasarkanStok} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataItemBanyakBerdasarkanStok
	 */
	class ItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			Object[] objects = (Object[]) arg1;
			Long itemId = ((Number) objects[0]).longValue();
			Date tanggalTerakhirPengadaan = (Date) objects[2];
			Number stok = (Number) objects[3];
			Session session = HibernateUtil.currentSession();
			final Item item = (Item) ConstantValues
					.simpleObject(session.createCriteria(Item.class).add(Restrictions.idEq(itemId)), Item.class);
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

			if (stok != null && stok.doubleValue() < 1.0) {
				checkbox.setDisabled(disableStokHabis != null && disableStokHabis);
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

			Image image = LibraryUtil.generateImage(item);
			image.setWidth("100%");
			image.setParent(arg0);

			new Label(item.getIsbn()).setParent(arg0);
			new Label(item.getNama()).setParent(arg0);
			new Label(item.getJenisItem() == null ? "" : item.getJenisItem().getNama()).setParent(arg0);
			new Label(item.getPenerbit() == null ? "" : item.getPenerbit().getNama()).setParent(arg0);
			new Label(item.getPengarangs()).setParent(arg0);
			new Label(tanggalTerakhirPengadaan == null ? "" : Common.dateFormat1.get().format(tanggalTerakhirPengadaan))
					.setParent(arg0);
			new Label(stok == null ? "" : Common.numberFormat.get().format(stok)).setParent(arg0);
		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Item");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.Grid gridUtama = new org.zkoss.zul.Grid();
		gridUtama.setWidth("100%");
		ais.ui.util.ZkCompat.setFlex(gridUtama, true);
		gridUtama.setParent(center);
		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		Row rowUtama = new Row();
		rowUtama.setParent(rowsUtama);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/ISBN"));
		row.appendChild(kodeIteman = new MyTextbox());
		kodeIteman.setWidth("90%");
		kodeIteman.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama/Judul"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		row.appendChild(jenisItem = new Combobox());
		Common.insertCombo(jenisItem, "nama", JenisItem.class);
		jenisItem.setWidth("90%");
		jenisItem.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(new ais.ui.util.MyLabelConfig(perpustakaan == null ? "" : perpustakaan.getNama()));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

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
		column.setLabel("Jenis");
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
		column.setLabel("Tgl Pengadaan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Stok");
		column.setAlign("right");
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
				AmbilDataItemBanyakBerdasarkanStok.this.detach();
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
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataItemBanyakBerdasarkanStok.java:326");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), items);
					eventListener.onEvent(myEvent);
				}
				AmbilDataItemBanyakBerdasarkanStok.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		JenisItem jenisItem = (JenisItem) (this.jenisItem.getSelectedItem() == null ? null
				: this.jenisItem.getSelectedItem().getValue());

		String sql = "select a.item, max(c.nama) as nama_item, " + "max(a.tanggal) as tanggal_terakhir_pengadaan, "
				+ "sum((a.qty+a.qtybonus)*b.jenis) as stok " + "from library.detail_transaksi a "
				+ "inner join library.kode_transaksi b on (a.kode_transaksi = b.id) "
				+ "left join library.item c on (a.item = c.id) where  (c.aktif is null or c.aktif=true) and a.item "
				+ (ids.size() == 0 ? "!=c.id"
						: "in (" + ids.toString().replaceAll("\\[", "").replaceAll("\\]", "") + ")")
				+ "  group by a.item order by " + orderBy + " asc";

		System.out.println(sql);
		List<Object[]> item = session.createSQLQuery(sql).list();

		sql = "select a.item, max(c.nama) as nama_item, " + "max(a.tanggal) as tanggal_terakhir_pengadaan, "
				+ "sum((a.qty+a.qtybonus)*b.jenis) as stok " + "from library.detail_transaksi a "
				+ "inner join library.kode_transaksi b on (a.kode_transaksi = b.id) "
				+ "left join library.item c on (a.item = c.id) where  (c.aktif is null or c.aktif=true) and a.item "
				+ (ids.size() == 0 ? "=c.id"
						: "not in (" + ids.toString().replaceAll("\\[", "").replaceAll("\\]", "") + ")")
				+ " and c.isbn ilike '%" + kodeIteman.getValue().trim() + "%' and c.nama ilike '%"
				+ nama.getValue().trim() + "%' " + "and c.jenis_item = "
				+ (jenisItem == null ? "c.jenis_item" : jenisItem.getId()) + " and a.perpustakaan = "
				+ (perpustakaan == null ? "a.perpustakaan" : perpustakaan.getId()) + " group by a.item order by "
				+ orderBy + " asc limit " + Common.MAX_RESULT_50;
		System.out.println(sql);
		List<Object[]> myItem = session.createSQLQuery(sql).list();

		item.addAll(myItem);

		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new ItemRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
