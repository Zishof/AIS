package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.NamaSekolahAsalAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.NamaSekolahAsal;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity
 * {@link ais.database.model.NamaSekolahAsal} — lihat {@link ais.ui.util.GetEventListener} untuk
 * arsitektur kerangka umum (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * {@code NamaSekolahAsal} adalah master data institusi pendidikan asal (mis. SMA/SMK asal calon
 * mahasiswa), dipakai terutama di modul penerimaan mahasiswa baru (PMB). Popup pencarian hanya
 * menyediakan satu field {@code kode}, yang dicocokkan ilike-substring ke DUA kolom sekaligus
 * (kode ATAU nama institusi — digabung OR), sehingga label "Cari" pada form berfungsi sebagai
 * pencarian bebas kode/nama. KHAS kelas ini: {@link #display()} memanggil
 * {@link NamaSekolahAsalAction#initdata()} lebih dulu (menyiapkan/seed data pendukung sebelum
 * form dibangun) dan, bila konfigurasi
 * {@code bisa_membuat_institusi_pendidikan_baru_langsung_dari_pilihan} aktif, menampilkan tautan
 * "Institusi Pendidikan Anda belum terdaftar? Buat baru disini" yang memanggil
 * {@link NamaSekolahAsalAction#onAddExternal} untuk membuat entri baru langsung dari popup picker
 * (umum dipakai calon mahasiswa yang sekolah asalnya belum ada di database). Constructor kedua
 * menerima {@code String nama} yang diteruskan ke {@link Bandbox#Bandbox(String)} sebagai nilai
 * tampilan awal Bandbox (BUKAN filter pencarian). Pemilihan bersifat TUNGGAL (Radiogroup). Field
 * {@code pagingHelper} dideklarasikan tapi TIDAK dipakai — pencarian masih memakai
 * {@code grid.setMold("paging")} client-side lama dibatasi {@link ais.common.Common#MAX_RESULT}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataNamaSekolahBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/**
	 * Konstruktor standar: memasang listener {@code onOpen} yang membangun popup pencarian secara
	 * lazy pada pembukaan pertama. Mengikuti kerangka standar di
	 * {@link ais.ui.util.GetEventListener}, tidak ada logika tambahan khusus entity ini.
	 */
	public AmbilDataNamaSekolahBanbox() {
		super();
		setReadonly(true);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {

					display();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});

	}

	/**
	 * Konstruktor dengan nilai tampilan awal: diteruskan ke {@link Bandbox#Bandbox(String)}
	 * sebagai teks awal Bandbox (BUKAN filter/kriteria pencarian — popup pencarian tetap kosong
	 * sampai pengguna mengisi field {@link #kode}). Selebihnya mengikuti kerangka constructor
	 * standar di {@link ais.ui.util.GetEventListener}.
	 *
	 * @param nama teks tampilan awal Bandbox
	 */
	public AmbilDataNamaSekolahBanbox(String nama) {
		super(nama);
		setReadonly(true);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {

					display();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});

	}

	/** Kriteria pencarian: kode ATAU nama institusi (ilike, substring, digabung OR). */
	private Textbox kode;

	/**
	 * Renderer baris grid hasil pencarian {@link NamaSekolahAsal}: kolom nama, tingkat, dan
	 * keterangan, plus satu radio button pilihan. Mengikuti kerangka renderer standar di
	 * {@link ais.ui.util.GetEventListener} — listener {@code onCheck} menutup popup, menyimpan
	 * entity terpilih ke atribut {@code "namaSekolahAsal"} dan teks tampilan
	 * {@code namaSekolahAsal.getNama()}, lalu meneruskan event ke {@link #eventListener} bila
	 * terpasang.
	 *
	 * @see AmbilDataNamaSekolahBanbox
	 */
	class NamaSekolahAsalRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final NamaSekolahAsal namaSekolahAsal = (NamaSekolahAsal) arg1;
			MyRadioConfig checkbox = new MyRadioConfig(namaSekolahAsal.getKode());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataNamaSekolahBanbox.this.setOpen(false);
					AmbilDataNamaSekolahBanbox.this.setAttribute("namaSekolahAsal", namaSekolahAsal);
					AmbilDataNamaSekolahBanbox.this.setValue(namaSekolahAsal.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(namaSekolahAsal.getNama()).setParent(arg0);
			new Label(namaSekolahAsal.getTingkat()).setParent(arg0);
			new Label(namaSekolahAsal.getKeterangan()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian {@link NamaSekolahAsal} sekali (dipanggil lazy dari listener
	 * {@code onOpen}): memanggil dulu {@link NamaSekolahAsalAction#initdata()} untuk menyiapkan
	 * data pendukung, lalu form dengan satu field {@code kode}, tautan "Buat baru disini" (hanya
	 * bila konfigurasi terkait aktif — lihat Javadoc class), tombol Cari, dan grid hasil dibungkus
	 * {@link org.zkoss.zul.Radiogroup} (pilih tunggal). Mengikuti kerangka {@code display()}
	 * standar selebihnya — lihat {@link ais.ui.util.GetEventListener}. Memanggil
	 * {@link #onSearchDefault(Event)} di akhir agar grid terisi saat popup pertama dibuka.
	 */
	@SuppressWarnings("deprecation")
	public void display() {
		NamaSekolahAsalAction.initdata();
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth(Common.isMobile() ? "100%" : "800px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Institusi Pendidikan");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new Borderlayout();
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

		Grid searchgrid = new Grid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Columns columns = new Columns();
		columns.setParent(searchgrid);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cari : "));
		row.appendChild(kode = new Textbox());
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(
				Common.bolehKonfigurasi("bisa_membuat_institusi_pendidikan_baru_langsung_dari_pilihan"));
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		A abutton = new A("Intitusi Pendidikan Anda belum terdaftar ? Buat baru disini");

		abutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				NamaSekolahAsal namaSekolahAsal = new NamaSekolahAsal();
				NamaSekolahAsalAction.onAddExternal(event, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						NamaSekolahAsal namaSekolahAsal = (NamaSekolahAsal) arg0.getData();
						AmbilDataNamaSekolahBanbox.this.setOpen(false);
						AmbilDataNamaSekolahBanbox.this.setAttribute("namaSekolahAsal", namaSekolahAsal);
						AmbilDataNamaSekolahBanbox.this.setValue(namaSekolahAsal.getNama());

						if (eventListener != null) {
							eventListener.onEvent(arg0);
						}
					}
				}, namaSekolahAsal);
			}
		});
		abutton.setParent(row);

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

		toolbar.appendChild(Common.createCleanButton(this, this));

		grid = new Grid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* setPageSize legacy dihapus: grid bukan mold "paging" sehingga setPageSize melempar IllegalStateException ("Available only the paging mold") dan daftar tidak pernah tampil. Paging ditangani AmbilDataPagingHelper. */
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NPSN");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Intitusi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tingkat");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Alamat");

		onSearchDefault(null);

	}

	/**
	 * Mengeksekusi pencarian {@link NamaSekolahAsal} dengan filter {@code aktif} (baris nonaktif
	 * disembunyikan kecuali kolomnya {@code null}) dan {@code kode} (ilike substring, dicocokkan
	 * ke kolom {@code kode} ATAU {@code nama} sekaligus lewat OR). Diurutkan menaik berdasar nama,
	 * dibatasi {@link ais.common.Common#MAX_RESULT}, lalu memasang {@link NamaSekolahAsalRenderer}
	 * dan model hasil ke {@link #grid}. Mengikuti kerangka {@code onSearchDefault} standar — lihat
	 * {@link ais.ui.util.GetEventListener}.
	 *
	 * @param event event pemicu (klik tombol Cari); boleh {@code null} saat dipanggil dari
	 *              {@link #display()} untuk mengisi grid pertama kali
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<NamaSekolahAsal> namaSekolahAsal = session.createCriteria(NamaSekolahAsal.class)

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.addOrder(Order.asc("nama"))

				.add(kode.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("kode", kode.getText().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", kode.getText().trim(), MatchMode.ANYWHERE)))

				.setMaxResults(Common.MAX_RESULT)

				.list();

		System.out.println(namaSekolahAsal);
		ListModel strset = new SimpleListModel(namaSekolahAsal);
		grid.setRowRenderer(new NamaSekolahAsalRenderer());
		grid.setModel(strset);

	}

	/** {@inheritDoc} Implementasi setter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** {@inheritDoc} Implementasi getter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public EventListener getEventListener() {
		return eventListener;
	}
}

