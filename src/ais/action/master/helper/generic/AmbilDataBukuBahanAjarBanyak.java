package ais.action.master.helper.generic;

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
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
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

import ais.action.master.BukuBahanAjarAction;
import ais.action.master.helper.FileBukuAjarHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BukuBahanAjar;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data buku bahan ajar banyak. Kelas ini memberi nama dan batas tanggung
 * jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code List
 * bukuBahanAjars}, {@code List bukuBahanAjarsHanyaDitampilkan}, {@code Set ids}, {@code MyTextbox
 * kodeBukuBahanAjaran}, {@code MyTextbox nama}; pembacaan/pencarian ({@code onSearchDefault()}, {@code
 * setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataBukuBahanAjarBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<BukuBahanAjar> bukuBahanAjars;
	private List<BukuBahanAjar> bukuBahanAjarsHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataBukuBahanAjarBanyak(List<BukuBahanAjar> bukuBahanAjars) {
		super();
		this.bukuBahanAjars = bukuBahanAjars;
		display();
		onSearchDefault(null);
	}

	public AmbilDataBukuBahanAjarBanyak(List<BukuBahanAjar> bukuBahanAjars,
			List<BukuBahanAjar> bukuBahanAjarsHanyaDitampilkan) {
		super();
		this.bukuBahanAjars = bukuBahanAjars;
		this.bukuBahanAjarsHanyaDitampilkan = bukuBahanAjarsHanyaDitampilkan;

		display();

		onSearchDefault(null);
	}

	private MyTextbox kodeBukuBahanAjaran;
	private MyTextbox nama;

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataBukuBahanAjarBanyak}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataBukuBahanAjarBanyak} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataBukuBahanAjarBanyak
	 */
	class BukuBahanAjarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final BukuBahanAjar bukuBahanAjar = (BukuBahanAjar) arg1;
			arg0.setAttribute("bukuBahanAjar", bukuBahanAjar);

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						FileBukuAjarHelper fileBukuAjarHelper = new FileBukuAjarHelper(false);

						fileBukuAjarHelper.display(bukuBahanAjar, detail);
					}

				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			for (BukuBahanAjar myBukuBahanAjar : bukuBahanAjars) {
				if (myBukuBahanAjar.getId().equals(bukuBahanAjar.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(bukuBahanAjar.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(bukuBahanAjar.getId());
					} else {
						ids.remove(bukuBahanAjar.getId());
					}
				}
			});

			new Label(bukuBahanAjar.getNama()).setParent(arg0);
			if (bukuBahanAjar.getPengarangAdalahDosen()) {
				BukuBahanAjarAction.tampilkanInfoDosen(bukuBahanAjar, false).setParent(arg0);
			} else {
				new Label((bukuBahanAjar.getPengarang1() != null && !bukuBahanAjar.getPengarang1().trim().equals("")
						? bukuBahanAjar.getPengarang1().trim() + ", " : "")
						+ (bukuBahanAjar.getPengarang2() != null && !bukuBahanAjar.getPengarang2().trim().equals("")
								? bukuBahanAjar.getPengarang2().trim() + ", " : "")
						+ (bukuBahanAjar.getPengarang3() != null && !bukuBahanAjar.getPengarang3().trim().equals("")
								? bukuBahanAjar.getPengarang3().trim() + ", " : "")).setParent(arg0);
			}
			new Label(bukuBahanAjar.getIsbn()).setParent(arg0);
			new Label(bukuBahanAjar.getPenerbit()).setParent(arg0);
			new Label(bukuBahanAjar.getTahun() == null ? "" : bukuBahanAjar.getTahun() + "").setParent(arg0);
		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Buku Bahan Ajar");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("ISBN"));
		row.appendChild(kodeBukuBahanAjaran = new MyTextbox());
		kodeBukuBahanAjaran.setWidth("90%");
		kodeBukuBahanAjaran.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Buku"));
		row.appendChild(nama = new MyTextbox());
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

		grid = new Grid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* setPageSize legacy dihapus: grid bukan mold "paging" sehingga setPageSize melempar IllegalStateException ("Available only the paging mold") dan daftar tidak pernah tampil. Paging ditangani AmbilDataPagingHelper. */
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
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Buku");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pengarang");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("ISBN");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penerbit");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun");
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
				AmbilDataBukuBahanAjarBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<BukuBahanAjar> bukuBahanAjars = new ArrayList<BukuBahanAjar>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						if (checkbox.isChecked() && !checkbox.isDisabled()) {
							BukuBahanAjar myBukuBahanAjar = (BukuBahanAjar) row.getAttribute("bukuBahanAjar");
							bukuBahanAjars.add(myBukuBahanAjar);
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), bukuBahanAjars);
					eventListener.onEvent(myEvent);
				}
				AmbilDataBukuBahanAjarBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (bukuBahanAjarsHanyaDitampilkan != null) {
			for (BukuBahanAjar bukuBahanAjar : bukuBahanAjarsHanyaDitampilkan) {
				values.add(bukuBahanAjar.getId());
			}
		}

		List<BukuBahanAjar> bukuBahanAjar = session.createCriteria(BukuBahanAjar.class).addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)).list();

		List<Long> valuesNot = new ArrayList<Long>();
		if (bukuBahanAjars != null) {
			for (BukuBahanAjar a : bukuBahanAjars) {
				valuesNot.add(a.getId());
			}
		}

		List<BukuBahanAjar> myBukuBahanAjar = session.createCriteria(BukuBahanAjar.class).addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))

		.add(valuesNot.size() == 0 ? Restrictions.sqlRestriction("1=1")
				: Restrictions.not(Restrictions.in("id", valuesNot)))

		.add(bukuBahanAjarsHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
				: Restrictions.in("id", values))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("isbn", kodeBukuBahanAjaran.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();

		bukuBahanAjar.addAll(myBukuBahanAjar);

		ListModel strset = new SimpleListModel(bukuBahanAjar);
		grid.setRowRenderer(new BukuBahanAjarRenderer());
		grid.setModel(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
