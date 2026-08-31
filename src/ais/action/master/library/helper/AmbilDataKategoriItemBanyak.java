package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
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

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.KategoriItem;
import ais.ui.util.MyTextbox;

/**
 * Tipe khusus untuk ambil data kategori item banyak. Kelas ini memberi nama dan batas tanggung
 * jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code List
 * kategoriItems}, {@code List kategoriItemsHanyaDitampilkan}, {@code Set ids}, {@code java.util.Map
 * idKategoriItemMap}, {@code MyTextbox nama}; pembacaan/pencarian ({@code onSearchDefault()}, {@code
 * setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataKategoriItemBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<KategoriItem> kategoriItems;
	private List<KategoriItem> kategoriItemsHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();
	// KE-FIX (picker "pilih banyak -> hanya halaman aktif tersimpan"): grid ini pakai mold
	// "paging" (lihat display()), jadi grid.getRows().getChildren() saat Simpan HANYA berisi
	// baris di halaman yang sedang tampil -- centang di halaman lain hilang diam-diam. Simpan
	// referensi objek yang sudah tercentang di sini (kunci = id, sejalan dgn Set "ids" yang
	// sudah ada) supaya tombol Simpan bisa membaca SELURUH pilihan lintas halaman, bukan cuma
	// yang ada di DOM grid saat ini. Pola sama dgn AmbilDataMasterAssetBanyak.
	private java.util.Map<Long, KategoriItem> idKategoriItemMap = new java.util.HashMap<Long, KategoriItem>();

	public AmbilDataKategoriItemBanyak(List<KategoriItem> kategoriItems) {
		super();
		this.kategoriItems = kategoriItems;
		display();
		onSearchDefault(null);
	}

	public AmbilDataKategoriItemBanyak(List<KategoriItem> kategoriItems,
			List<KategoriItem> kategoriItemsHanyaDitampilkan) {
		super();
		this.kategoriItems = kategoriItems;
		this.kategoriItemsHanyaDitampilkan = kategoriItemsHanyaDitampilkan;
		display();

		onSearchDefault(null);

	}

	private MyTextbox nama;

	class KategoriItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KategoriItem kategoriItem = (KategoriItem) arg1;
			arg0.setAttribute("kategoriItem", kategoriItem);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (KategoriItem myKategoriItem : kategoriItems) {
				if (myKategoriItem != null && kategoriItem != null
						&& myKategoriItem.getId().equals(kategoriItem.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(kategoriItem.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(kategoriItem.getId());
						idKategoriItemMap.put(kategoriItem.getId(), kategoriItem);
					} else {
						ids.remove(kategoriItem.getId());
					}
				}
			});

			new Label(kategoriItem.getNama()).setParent(arg0);
			new Label(kategoriItem.getKeterangan()).setParent(arg0);

		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Kategori Item");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
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
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

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
				AmbilDataKategoriItemBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					// KE-FIX: baca dari Set "ids" (dipelihara onCheck, lintas halaman) bukan
					// grid.getRows().getChildren() (cuma halaman aktif saat grid mold="paging").
					List<KategoriItem> kategoriItems = new ArrayList<KategoriItem>();
					for (Long id : ids) {
						try {
							KategoriItem myKategoriItem = idKategoriItemMap.get(id);
							if (myKategoriItem != null) {
								kategoriItems.add(myKategoriItem);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataKategoriItemBanyak.java:242");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), kategoriItems);
					eventListener.onEvent(myEvent);
				}
				AmbilDataKategoriItemBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (kategoriItemsHanyaDitampilkan != null) {
			for (KategoriItem kategoriItem : kategoriItemsHanyaDitampilkan) {
				values.add(kategoriItem.getId());
			}
		}

		List<KategoriItem> kategoriItem = session.createCriteria(KategoriItem.class).addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)).list();

		List<KategoriItem> myKategoriItem = session.createCriteria(KategoriItem.class)
				.add(Restrictions.eq("defaultItem", true)).addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(kategoriItemsHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();

		kategoriItem.addAll(myKategoriItem);

		ListModel strset = new SimpleListModel(kategoriItem);
		grid.setRowRenderer(new KategoriItemRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
