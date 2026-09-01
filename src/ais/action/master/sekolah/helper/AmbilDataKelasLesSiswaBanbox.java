package ais.action.master.sekolah.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
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

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity
 * {@link ais.database.model.sekolah.KelasLesSiswa} — lihat {@link ais.ui.util.GetEventListener}
 * untuk arsitektur kerangka umum (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * Memilih satu {@link KelasLesSiswa} — kelas les/ekstrakurikuler siswa (kelompok belajar
 * tambahan di luar kelas reguler, mis. les mata pelajaran, beda dari {@code KelasSiswa} yang
 * merupakan rombongan belajar reguler) — dari daftar popup, dengan filter nama, sekolah, dan
 * guru/wali pembina, serta pemilihan tunggal lewat {@link Radiogroup}. Combo sekolah otomatis
 * terisi dan dinonaktifkan bila konteks sekolah aktif sudah diketahui lewat
 * {@link SekolahUtil#getSekolah()}. Field {@code wali} adalah nested
 * {@link AmbilDataGuruBanbox} — picker guru bersarang di dalam picker ini — yang dipasangi
 * listener sehingga memilih guru di situ langsung memicu pencarian ulang kelas les.
 * </p>
 * <p>
 * <b>Logika filter guru pembina di {@link #onSearchDefault(Event)} bergantung dua flag
 * constructor</b> ({@link #AmbilDataKelasLesSiswaBanbox(boolean, boolean)}), bukan
 * kombinasi sederhana:
 * <ul>
 * <li>{@code waliKelasDanGuru == true}: bila user login BUKAN guru, hasil difilter ke guru yang
 * dipilih di picker {@code wali} (atau tampil semua bila belum memilih); bila user login ADALAH
 * guru, hasil dikosongkan sama sekali (mode ini secara desain hanya untuk user non-guru).</li>
 * <li>{@code waliKelasDanGuru == false} dan {@code semuaTampil == true}: sama seperti kasus
 * non-guru di atas — difilter ke guru yang dipilih di picker {@code wali}, atau tampil semua
 * bila belum dipilih (flag {@code waliKelasDanGuru} diabaikan di cabang ini).</li>
 * <li>{@code waliKelasDanGuru == false} dan {@code semuaTampil == false}: bila user login
 * ADALAH guru, hasil dibatasi ke kelas les dengan {@code guruPembina} = guru tsb ATAU flag
 * {@code absensiharusGuruPembina == true}; bila user login BUKAN guru, hasil dikosongkan.</li>
 * </ul>
 * Constructor tanpa argumen memakai {@code semuaTampil=true, waliKelasDanGuru=false}. Field
 * {@code ta} (tahun akademik berjalan) dan field {@code pagingHelper}
 * ({@link ais.ui.util.AmbilDataPagingHelper}) dideklarasikan namun TIDAK dipakai di file ini
 * (paging aktual memakai mold client-side "paging" + {@code Common.MAX_RESULT_1000}) — sisa
 * refactor yang belum tuntas, bukan bug fungsional yang perlu diperbaiki di sini. Semua hasil
 * juga difilter ke kelas les yang {@code aktif} true atau belum diisi.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataKelasLesSiswaBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private boolean semuaTampil;
	private boolean waliKelasDanGuru;

	/** Membuat komponen dengan {@code semuaTampil=true, waliKelasDanGuru=false} (lihat {@link #AmbilDataKelasLesSiswaBanbox(boolean, boolean)}). */
	public AmbilDataKelasLesSiswaBanbox() {
		this(true, false);
	}

	/** Tahun akademik berjalan; dideklarasikan tapi tidak dipakai di file ini. */
	public String ta = Common.getCurrentTahunAkademik();

	/**
	 * @param semuaTampil     bila {@code true} (dan {@code waliKelasDanGuru} juga tidak
	 *                        mengubahnya), hasil difilter ke guru yang dipilih pada picker
	 *                        {@code wali}, atau tampil semua bila belum dipilih
	 * @param waliKelasDanGuru bila {@code true}, sembunyikan seluruh hasil untuk user login yang
	 *                        berperan sebagai guru (mode ini ditujukan untuk user non-guru);
	 *                        lihat penjelasan lengkap kombinasi kedua flag ini di Javadoc kelas
	 */
	public AmbilDataKelasLesSiswaBanbox(boolean semuaTampil, boolean waliKelasDanGuru) {
		super();
		this.semuaTampil = semuaTampil;
		this.waliKelasDanGuru = waliKelasDanGuru;
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

	private Textbox nama;
	private Combobox searchsekolah = new Combobox();
	private AmbilDataGuruBanbox wali;

	/**
	 * Renderer baris hasil pencarian kelas les: radio pilih (memilih kelas les, menutup popup,
	 * dan memicu {@code eventListener}), nama kelas, ruang, tingkat, sekolah (label "Semua" bila
	 * kelas les tidak terikat satu sekolah tertentu), mata pelajaran, dan nama guru pembina.
	 *
	 * @see AmbilDataKelasLesSiswaBanbox
	 */
	class KelasLesSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelasLesSiswa kelas = (KelasLesSiswa) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKelasLesSiswaBanbox.this.setOpen(false);
					AmbilDataKelasLesSiswaBanbox.this.setAttribute("kelasLesSiswa", kelas);
					AmbilDataKelasLesSiswaBanbox.this.setAttribute("kelas", kelas);
					AmbilDataKelasLesSiswaBanbox.this.setValue(kelas.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(kelas.getNama()).setParent(arg0);
			new Label(kelas.getRuang() == null ? "" : kelas.getRuang().getNama()).setParent(arg0);
			new Label(kelas.getTingkat() + "").setParent(arg0);
			new Label(kelas.getSekolah() == null ? "Semua" : kelas.getSekolah().getNama() + "").setParent(arg0);
			new Label(kelas.getMatapelajaran() == null ? "" : kelas.getMatapelajaran().getNama()).setParent(arg0);
			new Label(kelas.getGuruPembina() == null ? "" : kelas.getGuruPembina().getNama()).setParent(arg0);

		}

	}

	/**
	 * Menyusun konten popup bandbox (dipanggil sekali saat pertama dibuka): form pencarian
	 * (nama, sekolah, guru/wali) — label kolom guru berubah menjadi "Wali" bila
	 * {@code semuaTampil} atau "Guru/Wali" sebaliknya — dengan tombol cari/bersihkan, grid hasil
	 * dengan paginasi client-side (mold "paging", 50 baris/halaman), lalu memuat data awal lewat
	 * {@link #onSearchDefault(Event)}.
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("950px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Kelas Les Siswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		Common.insertComboDanSemua(searchsekolah, "nama", Sekolah.class);

		Sekolah sekolah = SekolahUtil.getSekolah();
		if (sekolah != null) {
			Common.selectComboItem(searchsekolah, sekolah);
			searchsekolah.setDisabled(true);
		}

		searchsekolah.setWidth("90%");
		searchsekolah.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig(semuaTampil ? "Wali" : "Guru/Wali"));
		row.appendChild(wali = new AmbilDataGuruBanbox());
		wali.setWidth("90%");
		wali.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
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

		toolbar.appendChild(Common.createCleanButton(this, this));

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
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
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ruang");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tingkat");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sekolah");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Matapelajaran");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Wali Kelas");

		onSearchDefault(null);

	}

	/**
	 * Memuat ulang grid hasil pencarian kelas les sesuai filter formulir dan flag
	 * {@code semuaTampil}/{@code waliKelasDanGuru} constructor saat ini (lihat penjelasan
	 * kombinasi lengkap di Javadoc kelas), memakai sesi Hibernate thread-local. Selalu
	 * menambahkan filter {@code aktif} true/belum diisi, urutan sekolah/tingkat/nama, filter nama
	 * (ilike) dan sekolah dari formulir, dibatasi {@code Common.MAX_RESULT_1000} baris.
	 *
	 * @param event event pemicu (tidak dipakai — pemanggil selalu mengirim {@code null})
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Tbmuser tbmuser = Common.getCurrentUser();
		Guru guru = tbmuser.ambilGuru();

		List<KelasLesSiswa> kelas = session.createCriteria(KelasLesSiswa.class)

				.add(waliKelasDanGuru
						? (guru == null
								? (wali.getAttribute("guru") == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("guruPembina", wali.getAttribute("guru")))
								: Restrictions.sqlRestriction("false"))
						: semuaTampil
								? (wali.getAttribute("guru") == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("guruPembina", wali.getAttribute("guru")))
								: Restrictions.or(
										guru != null
												? Restrictions.or(Restrictions.eq("absensiharusGuruPembina", true),
														Restrictions.eq("guruPembina", guru))
												: Restrictions.sqlRestriction("false"),
										Restrictions.sqlRestriction("false")))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("sekolah")).addOrder(Order.asc("tingkat")).addOrder(Order.asc("nama"))
				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.setMaxResults(Common.MAX_RESULT_1000)

				.list();
		ListModel strset = new SimpleListModel(kelas);
		grid.setRowRenderer(new KelasLesSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @param eventListener dipanggil setiap kali user memilih satu kelas les dari daftar */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan kelas les yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}
}
