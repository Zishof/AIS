package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Matakuliah} —
 * lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * {@code Matakuliah} adalah master data mata kuliah akademik. Kelas ini punya DUA MODE PENCARIAN
 * yang SANGAT BERBEDA tergantung constructor yang dipakai:
 * </p>
 * <ol>
 * <li><b>Mode master biasa</b> (constructor tanpa argumen atau dengan {@link Jurusan}): mencari
 * langsung ke tabel {@link Matakuliah} dengan field {@code kodeMatakuliahan}, {@code nama} (ilike
 * substring), dan Combobox fakultas/prodi (dikunci bila {@link Jurusan} diberikan dan punya id).</li>
 * <li><b>Mode "mata kuliah milik mahasiswa"</b> (constructor dengan {@link Mahasiswa}, atau
 * dipasang belakangan via {@link #setSelectedMahasiswa(Mahasiswa)}): TIDAK mencari tabel
 * {@code Matakuliah} langsung, melainkan mencari {@code Detailperkuliahan} (baris KRS) milik
 * mahasiswa tersebut yang berstatus {@code persetujuan == DISETUJUI}, lalu memproyeksikan id mata
 * kuliahnya — hasilnya digabung dari DUA sumber: mata kuliah asli yang diambil
 * ({@code perkuliahan.matakuliah}) DAN mata kuliah hasil konversi kredit
 * ({@code matakuliahKonversi}, dari mahasiswa pindahan/RPL). Dalam mode ini, field fakultas/prodi
 * TIDAK ditampilkan di popup (tidak relevan — daftar sudah otomatis terbatas ke mahasiswa
 * tersebut), hanya {@code kodeMatakuliahan}/{@code nama} yang tersisa sebagai filter tambahan.</li>
 * </ol>
 * <p>
 * Mode mana yang aktif ditentukan oleh field {@link #selectedMahasiswa}: {@code null} berarti mode
 * master biasa. Popup mode master biasa hanya menampilkan baris {@code aktif == true} atau
 * {@code aktif} kosong; mode mahasiswa tidak menerapkan filter aktif (mengikuti apa pun yang
 * tercatat di riwayat KRS). Pemilihan bersifat TUNGGAL (Radiogroup).
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataMatakuliahBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	private EventListener eventListener;
	private Mahasiswa selectedMahasiswa = null;

	/**
	 * Konstruktor mode "mata kuliah milik mahasiswa" (lihat Javadoc class): mengaktifkan mode
	 * pencarian riwayat KRS mahasiswa. CATATAN: hanya memanggil {@code super()} implisit (constructor
	 * {@link Bandbox} tanpa argumen) — TIDAK menjalankan inisialisasi tambahan milik AIS seperti
	 * {@code setReadonly}/listener {@code onOpen} lazy-build yang dipasang constructor lain di
	 * kelas ini; pemanggil yang memakai constructor ini harus memastikan {@link #display()}
	 * dipanggil sendiri atau instance dipakai lewat jalur yang memasang listener setelahnya.
	 *
	 * @param mahasiswa mahasiswa yang riwayat KRS-nya (mata kuliah diambil + hasil konversi)
	 *                  menjadi satu-satunya sumber hasil pencarian
	 */
	public AmbilDataMatakuliahBanbox(Mahasiswa mahasiswa) {
		this.selectedMahasiswa = mahasiswa;
	}

	/**
	 * Konstruktor mode master biasa, mendelegasikan ke {@link #AmbilDataMatakuliahBanbox(Jurusan)}
	 * dengan instance {@link Jurusan} kosong (tanpa id) — sehingga fakultas/prodi TIDAK dikunci.
	 */
	public AmbilDataMatakuliahBanbox() {
		this(new Jurusan());
	}

	/**
	 * Konstruktor mode master biasa dengan filter opsional dari entity induk {@link Jurusan}: bila
	 * {@code jurusan} punya id (bukan instance kosong dari {@link #AmbilDataMatakuliahBanbox()}),
	 * Combobox fakultas dan prodi di popup pencarian diprapilih dan dikunci ke fakultas/prodi
	 * jurusan tersebut. Memasang listener {@code onOpen} standar untuk lazy-build popup — lihat
	 * {@link ais.ui.util.GetEventListener}.
	 *
	 * @param jurusan prodi induk untuk mengunci pencarian (harus punya id agar aktif), atau
	 *                instance kosong/{@code null} untuk pencarian bebas
	 */
	public AmbilDataMatakuliahBanbox(final Jurusan jurusan) {
		super();
		setReadonly(true);
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		if (jurusan != null && jurusan.getId() != null) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.selectComboItem(true, searchfakultas, jurusan.getFakultas());
					searchfakultas.setDisabled(true);

					Common.selectComboItem(true, searchjurusan, jurusan);
					searchjurusan.setDisabled(true);
				}
			});
		}

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

	/** Kriteria pencarian: kode mata kuliah (ilike, substring), dipakai di kedua mode. */
	private Textbox kodeMatakuliahan;
	/** Kriteria pencarian: nama mata kuliah (ilike, substring), dipakai di kedua mode. */
	private Textbox nama;
	/** Kriteria pencarian: fakultas — hanya relevan/ditampilkan pada mode master biasa. */
	private Combobox searchfakultas = new Combobox();
	/** Kriteria pencarian: prodi — hanya relevan/ditampilkan pada mode master biasa. */
	private Combobox searchjurusan = new Combobox();

	/**
	 * Renderer baris grid hasil pencarian {@link Matakuliah} (baik dari mode master biasa maupun
	 * mode riwayat KRS mahasiswa — lihat Javadoc class): kolom kode+id, nama, SKS, jurusan, dan
	 * jenis mata kuliah, plus satu radio button pilihan. Mengikuti kerangka renderer standar di
	 * {@link ais.ui.util.GetEventListener} — listener {@code onCheck} menutup popup, menyimpan
	 * entity terpilih ke atribut {@code "matakuliah"} dan teks tampilan {@code kode + " - " + nama},
	 * lalu meneruskan event ke {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataMatakuliahBanbox
	 */
	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Matakuliah matakuliah = (Matakuliah) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(matakuliah.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataMatakuliahBanbox.this.setOpen(false);
					AmbilDataMatakuliahBanbox.this.setAttribute("matakuliah", matakuliah);
					AmbilDataMatakuliahBanbox.this.setValue(matakuliah.getKode() + " - " + matakuliah.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(matakuliah.getKode() + " (" + matakuliah.getId() + ") ").setParent(arg0);
			new Label(matakuliah.getNama()).setParent(arg0);
			new Label(matakuliah.getSks() + "").setParent(arg0);
			new Label(matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getNama()).setParent(arg0);
			new Label(matakuliah.getJenisMatakuliah()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian {@link Matakuliah} sekali (dipanggil lazy dari listener
	 * {@code onOpen}): form dengan field kode dan nama (Enter di keduanya langsung memicu
	 * pencarian lewat {@code onOK}, plus tombol Cari sebaris), ditambah Combobox fakultas/prodi
	 * HANYA bila {@link #selectedMahasiswa} {@code null} (mode master biasa — lihat Javadoc
	 * class), dan grid hasil dibungkus {@link org.zkoss.zul.Radiogroup} (pilih tunggal). Mengikuti
	 * kerangka {@code display()} standar — lihat {@link ais.ui.util.GetEventListener}. Memanggil
	 * {@link #onSearchDefault(Event)} di akhir agar grid terisi saat popup pertama dibuka.
	 */
	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Matakuliah");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kodeMatakuliahan = new Textbox());
		kodeMatakuliahan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		if (selectedMahasiswa == null) {
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
			row.appendChild(searchfakultas);
			searchfakultas.setWidth("90%");
			searchfakultas.setWidth("90%");

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
			row.appendChild(searchjurusan);
			searchjurusan.setWidth("90%");
			searchjurusan.setWidth("90%");
		}
					// Enter di kolom Kode/Nama langsung mencari, + tombol Cari sebaris (selalu terlihat).
			final EventListener listenerCariBanbox = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			};
			kodeMatakuliahan.addEventListener("onOK", listenerCariBanbox);
			nama.addEventListener("onOK", listenerCariBanbox);
			MyToolbarbuttonConfig cariSebaris = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			cariSebaris.setStyle("font-weight:bold;");
			cariSebaris.addEventListener("onClick", listenerCariBanbox);
			row.appendChild(cariSebaris);

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
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keberadaan");

		onSearchDefault(null);

	}

	/**
	 * Mengeksekusi pencarian {@link Matakuliah} — cabang total tergantung {@link #selectedMahasiswa}
	 * (lihat Javadoc class untuk gambaran umum kedua mode):
	 * <ul>
	 * <li>{@code null} (mode master biasa): query langsung ke {@link Matakuliah}, hanya baris
	 * {@code aktif} (atau {@code null}), filter {@code nama}/{@code kodeMatakuliahan} (ilike
	 * substring), prodi (eq bila dipilih), dan fakultas (eq bila dipilih, lewat join ke
	 * {@code jurusan}). Diurutkan menaik berdasar nama, dibatasi
	 * {@link ais.common.Common#MAX_RESULT}.</li>
	 * <li>terisi (mode riwayat KRS): DUA query terpisah ke {@code Detailperkuliahan} berstatus
	 * {@code persetujuan == DISETUJUI} milik {@link #selectedMahasiswa} — satu memproyeksikan id
	 * {@code perkuliahan.matakuliah} (mata kuliah asli yang diambil), satu lagi memproyeksikan id
	 * {@code matakuliahKonversi} (hasil konversi kredit) — hasil keduanya DIGABUNG jadi satu daftar.
	 * Filter {@code nama}/{@code kodeMatakuliahan} tetap berlaku pada masing-masing query, tanpa
	 * filter aktif.</li>
	 * </ul>
	 * Kedua cabang memasang {@link MatakuliahRenderer} dan model hasil ke {@link #grid} di akhir.
	 * Mengikuti kerangka {@code onSearchDefault} standar — lihat {@link ais.ui.util.GetEventListener}.
	 *
	 * @param event event pemicu (klik tombol Cari, atau tekan Enter di kode/nama); boleh
	 *              {@code null} saat dipanggil dari {@link #display()}
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<Matakuliah> matakuliah;
		if (selectedMahasiswa == null) {

			matakuliah = ConstantValues.simpleList(session.createCriteria(Matakuliah.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.asc("nama"))
					.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
					.add(kodeMatakuliahan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("kode", kodeMatakuliahan.getValue().trim(), MatchMode.ANYWHERE))
					.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
							|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

					.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))
					.setMaxResults(Common.MAX_RESULT), Matakuliah.class);

		} else {
			matakuliah = ConstantValues
					.simpleList(
							session.createCriteria(Detailperkuliahan.class)
									.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
									.createAlias("perkuliahan", "perkuliahan")
									.setProjection(Projections.property("perkuliahan.matakuliah.id"))
									.createAlias("perkuliahan.matakuliah", "matakuliah")

									.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
											: Restrictions.ilike("matakuliah.nama", nama.getValue().trim(),
													MatchMode.ANYWHERE))
									.add(kodeMatakuliahan.getValue().trim().isEmpty()
											? Restrictions.sqlRestriction("true")
											: Restrictions.ilike("matakuliah.kode", kodeMatakuliahan.getValue().trim(),
													MatchMode.ANYWHERE))

									.add(Restrictions.eq("mahasiswa", selectedMahasiswa))
									.addOrder(Order.asc("semester")).addOrder(Order.asc("matakuliah.nama")),
							Matakuliah.class, false);

			List<Matakuliah> konversis = ConstantValues
					.simpleList(
							session.createCriteria(Detailperkuliahan.class)
									.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
									.setProjection(Projections.property("matakuliahKonversi.id"))
									.createAlias("matakuliahKonversi", "matakuliahKonversi")

									.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
											: Restrictions.ilike("matakuliahKonversi.nama", nama.getValue().trim(),
													MatchMode.ANYWHERE))
									.add(kodeMatakuliahan.getValue().trim().isEmpty()
											? Restrictions.sqlRestriction("true")
											: Restrictions.ilike("matakuliahKonversi.kode",
													kodeMatakuliahan.getValue().trim(), MatchMode.ANYWHERE))

									.add(Restrictions.eq("mahasiswa", selectedMahasiswa))
									.addOrder(Order.asc("semester")).addOrder(Order.asc("matakuliahKonversi.nama")),
							Matakuliah.class, false);
			matakuliah.addAll(konversis);
		}

		ListModel strset = new SimpleListModel(matakuliah);
		grid.setRowRenderer(new MatakuliahRenderer());
		grid.setModelCheckMobile(strset);
	}

	/** {@inheritDoc} Implementasi setter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** {@inheritDoc} Implementasi getter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Mengganti (atau memasang pertama kali) mahasiswa yang membatasi pencarian ke mode riwayat
	 * KRS-nya (lihat Javadoc class) setelah instance dibuat, lalu membersihkan state Bandbox
	 * (nilai/atribut) via {@link Common#clear(org.zkoss.zul.Bandbox)}. Mengaktifkan mode "mata
	 * kuliah milik mahasiswa" pada pencarian berikutnya bila sebelumnya berada di mode master
	 * biasa.
	 *
	 * @param mahasiswa mahasiswa baru yang riwayat KRS-nya menjadi sumber hasil pencarian, atau
	 *                  {@code null} untuk kembali ke mode master biasa
	 */
	public void setSelectedMahasiswa(Mahasiswa mahasiswa) {
		selectedMahasiswa = mahasiswa;
		Common.clear(AmbilDataMatakuliahBanbox.this);
	}
}
