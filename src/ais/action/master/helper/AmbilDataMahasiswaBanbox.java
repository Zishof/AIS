package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.AmbilDataPagingHelper;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Mahasiswa} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * {@code Mahasiswa} adalah entity mahasiswa — salah satu entity yang paling sering dicari di
 * seluruh AIS, sehingga picker ini termasuk yang paling kaya fitur di antara keluarga
 * {@code AmbilData*Banbox}. Popup pencarian menyediakan: {@code nim}, {@code nama} (keduanya ilike
 * substring), {@code searchprogram} (Combobox program), {@code tahunangkatan} (Decimalbox, eq),
 * {@code searchstatus} (status mahasiswa saat ini — DICEK lewat sub-query SQL langsung ke tabel
 * {@code history_status_mahasiswa} untuk tahun akademik dan semester (genap/ganjil) BERJALAN, bukan
 * kolom biasa pada {@code Mahasiswa}), {@code searchstatusawal} (status awal masuk), {@code kelas}
 * (Bandbox nested {@link AmbilDataKelasBanbox}, dicocokkan exact ke kolom denormalisasi
 * {@code kelas} milik {@code Mahasiswa}), {@code searchdosen} (Bandbox nested
 * {@link AmbilDataDosenBanbox} untuk dosen pembimbing akademik/PA), serta Combobox fakultas/prodi.
 * Hasil di-scope otomatis ke {@link PerguruanTinggi} milik pengguna login
 * ({@link ais.action.master.helper.util.PerguruanTinggiUtil#getPerguruanTinggi()}) bila terdeteksi
 * — mendukung setup multi-tenant/multi-kampus dalam satu database. Bila pengguna yang sedang login
 * ADALAH mahasiswa, Bandbox otomatis dikunci ({@code setDisabled(true)}) ke data dirinya sendiri
 * sejak konstruksi. Empat constructor overload menyediakan kombinasi filter siap pakai:
 * batasan daftar ID mahasiswa tertentu ({@code indsMhsPerkuliahan}, mis. peserta suatu
 * perkuliahan), filter hanya status aktif ({@code hanyaYangAktif} — dicek murah lewat flag
 * {@code aktif} pada renderer, BUKAN query {@code HistoryStatusMahasiswa} yang lambat), dan filter
 * hanya alumni ({@code hanyaAlumni}, mengunci Combobox status dan menambah kondisi
 * {@code statusKeluar.id == 1}). Pencarian memakai paging SERVER-SIDE
 * {@link AmbilDataPagingHelper} dengan callback inisialisasi yang memaksa lazy-load beberapa
 * relasi sebelum entity ter-detach dari sesi Hibernate. Pemilihan bersifat TUNGGAL (Radio dalam
 * Radiogroup).
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataMahasiswaBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final AmbilDataPagingHelper pagingHelper = new AmbilDataPagingHelper();

	private EventListener eventListener;
	private Boolean hanyaYangAktif = false;
	private List<Long> indsMhsPerkuliahan;
	private PerguruanTinggi perguruanTinggi;
	private Boolean hanyaAlumni = false;

	protected boolean tidakMelihatDosen = false;

	/**
	 * Konstruktor default: mendelegasikan ke {@link #AmbilDataMahasiswaBanbox(Boolean)} dengan
	 * {@code hanyaYangAktif = false} — pencarian tidak dibatasi status aktif.
	 */
	public AmbilDataMahasiswaBanbox() {

		this(false);
	}

	/**
	 * Konstruktor dengan filter tambahan khusus kelas ini: membatasi hasil HANYA ke mahasiswa yang
	 * id-nya ada dalam {@code indsMhsPerkuliahan} (mis. daftar peserta suatu perkuliahan/kelas
	 * tertentu). Mendelegasikan ke {@link #AmbilDataMahasiswaBanbox(Boolean)} untuk sisa
	 * inisialisasi standar.
	 *
	 * @param indsMhsPerkuliahan daftar id {@link Mahasiswa} yang menjadi satu-satunya hasil yang
	 *                           mungkin muncul; {@code null}/kosong berarti tidak membatasi
	 */
	public AmbilDataMahasiswaBanbox(List<Long> indsMhsPerkuliahan) {
		this(false);
		this.indsMhsPerkuliahan = indsMhsPerkuliahan;
	}

	/**
	 * Konstruktor dengan filter tambahan khusus kelas ini: bila {@code true}, hasil dibatasi hanya
	 * mahasiswa berstatus aktif (dicek murah lewat flag {@code aktif}, bukan query riwayat status
	 * yang lambat — lihat {@link MahasiswaRenderer}). Mendelegasikan ke
	 * {@link #AmbilDataMahasiswaBanbox(Boolean, Boolean)} dengan {@code hanyaAlumni = false}.
	 * CATATAN: parameter {@code hanyaYangAktif} di sini TIDAK diteruskan ke constructor tujuan
	 * (selalu memanggil {@code this(false, false)}) — lihat detail di badan constructor.
	 *
	 * @param hanyaYangAktif diterima untuk kompatibilitas signature; TIDAK memengaruhi hasil pada
	 *                       constructor ini (lihat catatan di atas)
	 */
	public AmbilDataMahasiswaBanbox(Boolean hanyaYangAktif) {
		this(false, false);
	}

	/**
	 * Konstruktor utama dengan dua filter tambahan khusus kelas ini: {@code hanyaYangAktif}
	 * (batasi ke mahasiswa aktif) dan {@code hanyaAlumni} (batasi ke alumni, sekaligus mengunci
	 * Combobox status di {@link #display()}). Menentukan {@link #perguruanTinggi} scoping dari
	 * {@link ais.action.master.helper.util.PerguruanTinggiUtil#getPerguruanTinggi()}, mengunci
	 * Bandbox ke data mahasiswa yang sedang login (bila pengguna adalah mahasiswa), lalu memasang
	 * listener {@code onOpen} standar yang membangun popup pencarian secara lazy pada pembukaan
	 * pertama.
	 *
	 * @param hanyaYangAktif hanya tampilkan mahasiswa berstatus aktif
	 * @param hanyaAlumni    hanya tampilkan alumni (mengunci Combobox status di popup)
	 */
	public AmbilDataMahasiswaBanbox(Boolean hanyaYangAktif, Boolean hanyaAlumni) {
		super();
		this.hanyaAlumni = hanyaAlumni;
		this.hanyaYangAktif = hanyaYangAktif;
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			setAttribute("mahasiswa", tbmuser.getMahasiswa());
			setAttribute("myValue", tbmuser.getMahasiswa());
			setValue(tbmuser.getMahasiswa().getNim() + "-" + tbmuser.getMahasiswa().getNama());
			setDisabled(true);
		}

		Common.insertCombo(searchstatus, "nama", "kodeEpsbed", StatusMahasiswa.class);

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

	/** Kriteria pencarian: NIM (ilike, substring). */
	private Textbox nim;
	/** Kriteria pencarian: nama mahasiswa (ilike, substring). */
	private Textbox nama;
	/** Kriteria pencarian: kelas — Bandbox nested, dicocokkan exact ke kolom {@code kelas}. */
	private AmbilDataKelasBanbox kelas;
	/** Kriteria pencarian: tahun angkatan (eq). */
	private Decimalbox tahunangkatan;
	/** Kriteria pencarian: status mahasiswa berjalan (via sub-query {@code history_status_mahasiswa}). */
	private Combobox searchstatus = new Combobox();
	/** Kriteria pencarian: program studi/jenjang (eq). */
	private Combobox searchprogram = new Combobox();
	/** Kriteria pencarian: status awal masuk mahasiswa (eq). */
	private Combobox searchstatusawal = new Combobox();
	/** Kriteria pencarian: dosen pembimbing akademik (PA) — Bandbox nested. */
	private AmbilDataDosenBanbox searchdosen;
	/** Kriteria pencarian: fakultas (lewat join ke jurusan). */
	private Combobox searchfakultas = new Combobox();
	/** Kriteria pencarian: prodi. */
	private Combobox searchjurusan = new Combobox();

	/**
	 * Renderer baris grid hasil pencarian {@link Mahasiswa}: foto kecil
	 * ({@link ais.common.CommonMedia#tampilkanGambarKecil}), NIM sebagai label radio, nama, tahun
	 * angkatan, jurusan, dan program. KHAS renderer ini: bila {@link #hanyaYangAktif} bernilai
	 * true, radio button dinonaktifkan untuk mahasiswa dengan flag {@code aktif == false} — sengaja
	 * memakai flag murah ini alih-alih query {@code HistoryStatusMahasiswa} yang lebih lambat.
	 * Selebihnya mengikuti kerangka renderer standar di {@link ais.ui.util.GetEventListener} —
	 * listener {@code onCheck} menutup popup, menyimpan entity terpilih ke atribut
	 * {@code "mahasiswa"}/{@code "myValue"} dan teks tampilan {@code nim + " - " + nama}, lalu
	 * meneruskan event ke {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataMahasiswaBanbox
	 */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(arg0);
			Radio checkbox = new Radio(mahasiswa.getNim());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);
			if (hanyaYangAktif) {
				// Hindari query HistoryStatusMahasiswa (lambat): pakai flag aktif (murah).
				checkbox.setDisabled(Boolean.FALSE.equals(mahasiswa.getAktif()));
			}

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					AmbilDataMahasiswaBanbox.this.setOpen(false);
					AmbilDataMahasiswaBanbox.this.setAttribute("mahasiswa", mahasiswa);
					AmbilDataMahasiswaBanbox.this.setAttribute("myValue", mahasiswa);
					AmbilDataMahasiswaBanbox.this.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
					// AmbilDataMahasiswaBanbox.this.setId("mhs_"
					// + mahasiswa.getId());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);
			new Label(mahasiswa.getProgram()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian {@link Mahasiswa} sekali (dipanggil lazy dari listener
	 * {@code onOpen}): form dengan field NIM, nama, program, tahun angkatan, status, status awal,
	 * kelas (Bandbox nested), dosen PA (Bandbox nested), fakultas, dan prodi — hampir semua field
	 * memanggil ulang {@link #onSearchDefault(Event)} langsung saat berubah (bukan menunggu tombol
	 * Cari), plus filter toggle {@link ais.ui.util.BanboxFilterToggle}. Bila
	 * {@link #hanyaAlumni} true, Combobox status dikunci ({@code setDisabled}). Grid hasil
	 * dibungkus {@link org.zkoss.zul.Radiogroup} (pilih tunggal) dengan paging server-side.
	 * Mengikuti kerangka {@code display()} standar — lihat {@link ais.ui.util.GetEventListener}.
	 * Memanggil {@link #onSearchDefault(Event)} di akhir agar grid terisi saat popup pertama
	 * dibuka.
	 */
	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("1200px");
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

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		// Banyak field filter (NIM/Nama/Program/Angkatan/Status/Kelas/Dosen PA/Fakultas/Prodi) →
		// beri tinggi cukup + autoscroll agar filter & tombol Cari tampil rapi, tak terpotong.
		north.setHeight("130px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Textbox());
		nim.setWidth("90%");
		nim.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunangkatan = new Decimalbox());
		tahunangkatan.setWidth("90%");
		tahunangkatan.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(searchstatus);
		searchstatus.setWidth("90%");
		searchstatus.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		if (hanyaAlumni) {
			searchstatus.setDisabled(true);
		}

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		row.appendChild(searchstatusawal);
		searchstatusawal.setWidth("90%");
		searchstatusawal.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initPrograms(searchprogram);
		Common.insertCombo(searchstatusawal, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new AmbilDataKelasBanbox());
		kelas.setWidth("90%");
		kelas.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen PA"));
		searchdosen = new AmbilDataDosenBanbox(tidakMelihatDosen, true, false);
		row.appendChild(searchdosen);
		searchdosen.setWidth("90%");
		searchdosen.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		if (indsMhsPerkuliahan != null && !indsMhsPerkuliahan.isEmpty()) {
			searchdosen.setAttribute("myValue", null);
			searchdosen.setAttribute("dosen", null);
			searchdosen.setValue("");
			searchdosen.setDisabled(false);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		// Tombol "Cari" SEBARIS pada baris filter agar SELALU terlihat (tak tergantung tinggi North).
		MyToolbarbuttonConfig btnCariSebaris = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		btnCariSebaris.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		row.appendChild(btnCariSebaris);

		Toolbar toolbar = new Toolbar();
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		toolbar.appendChild(Common.createCleanButton(this, this));

		/* Paging server-side: grid di Center, kontrol paging di South
		 * (pola seragam AmbilDataPagingHelper, menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100). */
		grid = new MyGrid();
		pagingHelper.pasangOnPaging(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(center, grid);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Program");


		onSearchDefault(null);

	}

	/**
	 * Membangun {@link Criteria} pencarian {@link Mahasiswa}: hanya baris {@code aktif} (atau
	 * {@code null}), filter dosen PA (eq bila dipilih di {@link #searchdosen}), filter daftar id
	 * {@link #indsMhsPerkuliahan} (bila diisi, ditambahkan sebagai {@code Restrictions.in("id",
	 * ...)}, digabung AND dengan seluruh filter lain — bukan menggantikannya), filter kelas (exact
	 * match ke kolom denormalisasi
	 * {@code kelas} milik Mahasiswa, BUKAN join relasi), filter status BERJALAN (dibangun sebagai
	 * SQL restriction sub-query ke {@code history_status_mahasiswa} untuk tahun akademik dan
	 * semester genap/ganjil saat ini — TIDAK sama dengan kolom status biasa), filter status awal,
	 * nama, NIM, tahun angkatan, program, prodi, dan fakultas (join alias {@code jurusan}). Bila
	 * {@link #hanyaAlumni} true, ditambah kondisi {@code statusKeluar.id == 1}; bila
	 * {@link #perguruanTinggi} terisi (scoping multi-tenant), ditambah join ke
	 * {@code jurusan.fakultas.perguruanTinggi}. Dipanggil oleh {@link AmbilDataPagingHelper} baik
	 * untuk menghitung total baris maupun mengambil satu halaman data — parameter {@code isOrder}
	 * mengontrol apakah pengurutan (tahun angkatan menurun, NIM menaik) ikut dipasang.
	 *
	 * @param session  sesi Hibernate aktif
	 * @param isOrder  {@code true} untuk memasang {@code ORDER BY}, {@code false} bila criteria
	 *                 hanya dipakai menghitung jumlah baris
	 * @return criteria siap dieksekusi oleh {@link AmbilDataPagingHelper}
	 */
	public Criteria initCriteria(Session session, boolean isOrder) {
		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");

		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (statusMahasiswa != null) {
			String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ statusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
					+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ")";

			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		String kel = kelas.getValue();

		Criteria criteria = session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(dosen != null ? CommonSearchFilterHelper.eqIdValue("dosenPa", dosen.getId(), false) : Restrictions.sqlRestriction("1=1"))

				.add(indsMhsPerkuliahan == null || indsMhsPerkuliahan.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.in("id", indsMhsPerkuliahan))

				.add(kel != null && !kel.trim().isEmpty() ? Restrictions.ilike("kelas", kel.trim(), MatchMode.EXACT)
						: Restrictions.sqlRestriction("true"))

				.add(criteriaStatus)

				.add(CommonSearchFilterHelper.eqSelectedWithId("statusAwalMahasiswa", searchstatusawal, false))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE))

				.add(tahunangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));

		if (isOrder) {
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));
		}

		if (hanyaAlumni) {
			criteria.add(Restrictions.eq("statusKeluar.id", 1L));
		}

		if (perguruanTinggi != null) {
			criteria.createAlias("jurusan.fakultas", "fakultas", Criteria.LEFT_JOIN)
					.add(CommonSearchFilterHelper.eqValue("fakultas.perguruanTinggi", perguruanTinggi, false));
		}

		return criteria;
	}

	/**
	 * Mengeksekusi pencarian {@link Mahasiswa} lewat {@link AmbilDataPagingHelper#cari}, yang
	 * memanggil balik {@link #initCriteria(Session, boolean)} untuk membangun query per halaman.
	 * Callback {@code Inisialisasi} memaksa lazy-load {@code jurusan}, {@code statusKeluar}, dan
	 * {@code statusAwalMahasiswa} tiap baris hasil (dengan try-catch audit terpisah dari inisiatif
	 * Javadoc ini — lihat marker {@code auto-audit(empty-catch)}, JANGAN disentuh di sini) agar
	 * aman diakses {@link MahasiswaRenderer} di luar sesi Hibernate saat itu, lalu memasang
	 * renderer dan model hasil ke {@link #grid}. Mengikuti kerangka {@code onSearchDefault} standar
	 * — lihat {@link ais.ui.util.GetEventListener}.
	 *
	 * @param event event pemicu (klik tombol Cari, tekan Enter, atau ganti salah satu field);
	 *              boleh {@code null} saat dipanggil dari {@link #display()}
	 */
	public void onSearchDefault(Event event) {
		List<Mahasiswa> mahasiswa = pagingHelper.cari(new AmbilDataPagingHelper.CriteriaFactory() {
			@Override
			public Criteria initCriteria(Session session, boolean isOrder) {
				return AmbilDataMahasiswaBanbox.this.initCriteria(session, isOrder);
			}
		}, Mahasiswa.class, new AmbilDataPagingHelper.Inisialisasi<Mahasiswa>() {
			@Override
			public void init(Mahasiswa m) {
				/* Relasi yang dipakai MahasiswaRenderer disiapkan selagi
				 * session masih terbuka (entity detached setelah ini). */
				try {
					Hibernate.initialize(m.getJurusan());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaBanbox.java:517");
				}
				try {
					Hibernate.initialize(m.getStatusKeluar());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaBanbox.java:521");
				}
				try {
					Hibernate.initialize(m.getStatusAwalMahasiswa());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaBanbox.java:525");
				}
			}
		});

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
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
}
