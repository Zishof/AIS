package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranSidang;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.PendaftaranSidang}
 * (dipakai untuk memilih {@link ais.database.model.Mahasiswa}) — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * BERBEDA dari kebanyakan picker mahasiswa lain di AIS: entity ROOT yang dicari lewat
 * {@code Criteria} adalah {@link PendaftaranSidang} (pendaftaran sidang skripsi/tugas akhir),
 * BUKAN {@code Mahasiswa} langsung — sehingga hasilnya otomatis terbatas HANYA mahasiswa yang
 * PERNAH mendaftar sidang (lewat relasi {@code PendaftaranSidang.skripsi.mahasiswa}). Field
 * pencarian {@code nim}, {@code nama}, {@code tahunangkatan}, dan prodi diterapkan pada sub-criteria
 * {@code skripsi.mahasiswa} (dibuka lewat {@code createCriteria("skripsi").createCriteria(
 * "mahasiswa")}), sedangkan filter fakultas diterapkan lewat sub-criteria berikutnya pada
 * {@code jurusan} milik mahasiswa tersebut. Renderer mengekstrak {@link Mahasiswa} dari tiap hasil
 * lewat {@code pendaftaranSidang.getSkripsi().getMahasiswa()}. BERBEDA lagi dari kebanyakan
 * subclass sejenis: constructor memanggil {@link #display()} LANGSUNG (bukan lewat listener
 * {@code onOpen} lazy standar). Pemilihan bersifat TUNGGAL (Radiogroup). Tidak ada constructor
 * dengan parameter tambahan; field {@code pagingHelper} dideklarasikan tapi TIDAK dipakai —
 * pencarian masih memakai {@code grid.setMold("paging")} client-side lama dibatasi
 * {@link ais.common.Common#MAX_RESULT}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataMahasiswaDaftarSidangBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/** {@inheritDoc} Implementasi getter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public EventListener getEventListener() {
		return eventListener;
	}

	/** {@inheritDoc} Implementasi setter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * Konstruktor KHUSUS (menyimpang dari kerangka standar {@link ais.ui.util.GetEventListener}):
	 * mempersiapkan Combobox fakultas/prodi lalu memanggil {@link #display()} langsung saat
	 * instance dibuat, TIDAK memasang listener {@code onOpen} lazy seperti kebanyakan subclass
	 * sejenis.
	 */
	public AmbilDataMahasiswaDaftarSidangBanbox() {
		super();

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		display();
	}

	/** Kriteria pencarian: NIM mahasiswa (ilike, substring, pada sub-criteria skripsi.mahasiswa). */
	private Textbox nim;
	/** Kriteria pencarian: nama mahasiswa (ilike, substring, pada sub-criteria skripsi.mahasiswa). */
	private Textbox nama;
	/** Kriteria pencarian: tahun angkatan mahasiswa (eq). */
	private Decimalbox tahunangkatan;

	/** Kriteria pencarian: fakultas mahasiswa (lewat sub-criteria jurusan). */
	private Combobox searchfakultas = new Combobox();
	/** Kriteria pencarian: prodi mahasiswa. */
	private Combobox searchjurusan = new Combobox();

	/**
	 * Renderer baris grid hasil pencarian {@link PendaftaranSidang}: menampilkan NIM, nama, dan
	 * tahun angkatan {@link Mahasiswa} yang diekstrak dari
	 * {@code pendaftaranSidang.getSkripsi().getMahasiswa()}, plus satu radio button pilihan.
	 * Mengikuti kerangka renderer standar di {@link ais.ui.util.GetEventListener} — listener
	 * {@code onCheck} menutup popup, menyimpan {@link Mahasiswa} (bukan {@link PendaftaranSidang})
	 * terpilih ke atribut {@code "mahasiswa"} dan teks tampilan {@code nim + " - " + nama}, lalu
	 * meneruskan event ke {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataMahasiswaDaftarSidangBanbox
	 */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = ((PendaftaranSidang) arg1).getSkripsi().getMahasiswa();
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataMahasiswaDaftarSidangBanbox.this.setOpen(false);
					AmbilDataMahasiswaDaftarSidangBanbox.this.setAttribute("mahasiswa", mahasiswa);
					AmbilDataMahasiswaDaftarSidangBanbox.this
							.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
					AmbilDataMahasiswaDaftarSidangBanbox.this.setId("mhs_" + mahasiswa.getId());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian {@link PendaftaranSidang} sekali (dipanggil langsung dari
	 * constructor — lihat catatan di Javadoc class): form dengan field NIM, nama, tahun angkatan,
	 * fakultas, dan prodi (NIM/nama memicu pencarian langsung saat Enter ditekan lewat
	 * {@code onOK}), tombol Cari, dan grid hasil dibungkus {@link org.zkoss.zul.Radiogroup} (pilih
	 * tunggal). Mengikuti kerangka {@code display()} standar selebihnya — lihat
	 * {@link ais.ui.util.GetEventListener}. Memanggil {@link #onSearchDefault(Event)} di akhir agar
	 * grid langsung terisi.
	 */
	public void display() {
		setReadonly(true);
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("600px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Mahasiswa");
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

		// Listener pencarian bersama: tombol Cari + Enter (onOK) di tiap field.
		final EventListener listenerCari = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		};

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Textbox());
		nim.setWidth("90%");
		nim.addEventListener("onOK", listenerCari); // Enter di NIM → cari

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");
		nama.addEventListener("onOK", listenerCari); // Enter di Nama → cari

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunangkatan = new Decimalbox());
		tahunangkatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		// Tombol "Cari" SEBARIS agar SELALU terlihat (tak tergantung tinggi North).
		MyToolbarbuttonConfig btnCariSebaris = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		btnCariSebaris.addEventListener("onClick", listenerCari);
		row.appendChild(btnCariSebaris);

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// South south = new South();
		// ais.ui.util.ZkCompat.setFlex(south, true);
		// south.setParent(div);

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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
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
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("55%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");
		column.setWidth("25%");

		onSearchDefault(null);

	}

	/**
	 * Mengeksekusi pencarian {@link PendaftaranSidang}: membuka sub-criteria berjenjang
	 * {@code skripsi} → {@code mahasiswa} lalu menerapkan filter {@code nama}/{@code nim} (ilike
	 * substring) dan tahun angkatan (eq) pada mahasiswa terkait, filter prodi (eq bila dipilih),
	 * lalu sub-criteria {@code jurusan} untuk filter fakultas (eq bila dipilih). Diurutkan menurun
	 * berdasar tahun angkatan lalu menaik berdasar NIM, dibatasi
	 * {@link ais.common.Common#MAX_RESULT}, lalu memasang {@link MahasiswaRenderer} dan model hasil
	 * ke {@link #grid}. Mengikuti kerangka {@code onSearchDefault} standar — lihat
	 * {@link ais.ui.util.GetEventListener}.
	 *
	 * @param event event pemicu (klik tombol Cari, atau tekan Enter di NIM/nama); boleh
	 *              {@code null} saat dipanggil dari {@link #display()}
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<PendaftaranSidang> pendaftarSidang = session.createCriteria(PendaftaranSidang.class)
				.createCriteria("skripsi").createCriteria("mahasiswa").addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
				.add(Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("nim", nim.getText().trim(), MatchMode.ANYWHERE))
				.add(tahunangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))

		.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
				: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

		.createCriteria("jurusan", Criteria.LEFT_JOIN)

		.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
				: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

		.setMaxResults(Common.MAX_RESULT).list();

		// System.out.println(mahasiswa);
		ListModel strset = new SimpleListModel(pendaftarSidang);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}
}
