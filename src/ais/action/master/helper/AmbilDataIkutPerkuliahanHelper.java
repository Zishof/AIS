package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.MatakuliahPrasyaratAction;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.PerkuliahanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK untuk fitur "Ikut Perkuliahan" — memungkinkan seorang {@link Mahasiswa} mendaftarkan
 * diri mengikuti jadwal {@link Perkuliahan} lain di luar jadwal yang sudah tercatat pada KRS-nya
 * sendiri (dicatat sebagai {@link Detailperkuliahan} dengan field {@code ikutiPerkuliahan} terisi,
 * bukan {@code perkuliahan}). Dipakai misalnya untuk mahasiswa yang ingin numpang hadir/menabung
 * nilai pada kelas paralel lain.
 *
 * <p>
 * Jendela pencarian menampilkan daftar jadwal perkuliahan (bukan jadwal paralel) yang dapat
 * disaring berdasarkan Fakultas, Program Studi, Program, kode/nama Mata Kuliah, dan Semester;
 * setiap baris dapat dibuka ({@link MyDetail}) untuk melihat aktivitas/agenda perkuliahan lewat
 * {@link AktifitasPerkuliahanHelper}, dan diberi kotak centang untuk dipilih. Baris yang mahasiswa
 * sudah ikuti/ambil ditandai "Terpilih" dan kotak centangnya disembunyikan (tidak bisa dipilih ulang).
 * </p>
 *
 * <p>
 * {@link #save()} memvalidasi kapasitas kelas ({@link Perkuliahan#getKapasitasKelas()}, default
 * {@link Ruang#getDefaultKapasitas()}) sebelum menyimpan setiap baris terpilih sebagai
 * {@link Detailperkuliahan} baru berstatus {@link Detailperkuliahan#BELUM_DISETUJUI}; mata kuliah
 * yang sama hanya diproses sekali walau muncul di beberapa baris (dedup per
 * {@code matakuliah.getId()}), dan jadwal yang penuh dilewati dengan peringatan terkumpul yang
 * ditampilkan di akhir.
 * </p>
 */
public class AmbilDataIkutPerkuliahanHelper {

	/** Tahun akademik jadwal perkuliahan yang dicari/ditampilkan (mis. "2026/2027"). */
	private String tahunAjaran;
	/** Nomor semester default untuk filter pencarian, sekaligus nilai yang dicatat pada {@link Detailperkuliahan} baru. */
	private Integer semester;
	/** Mahasiswa yang akan mengikuti perkuliahan; sumber filter Fakultas/Prodi/Program awal dan pemilik {@link Detailperkuliahan} yang disimpan. */
	private Mahasiswa mahasiswa;
	/** Grid hasil pencarian jadwal {@link Perkuliahan}, dirender oleh {@link MatakuliahRenderer}. */
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	/** Helper paging server-side (tidak dipakai aktif pada grid mold "paging" saat ini; dipertahankan untuk kompatibilitas). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	/** Kotak filter kode Mata Kuliah (pencocokan ILIKE, dipicu {@code onChange}). */
	private Textbox kodeMk;
	/** Kotak filter nama Mata Kuliah (pencocokan ILIKE, dipicu {@code onChange}). */
	private Textbox namaMk;
	/** Combobox filter Fakultas; pra-isi dari fakultas {@link #mahasiswa}. */
	private Combobox searchfakultas = new Combobox();
	/** Combobox filter Program Studi ({@link Jurusan}); pra-isi dari jurusan {@link #mahasiswa}, opsi dibatasi ke fakultas terpilih. */
	private Combobox jurusanCombobox = new Combobox();
	/** Combobox filter {@link Program}; pra-isi dari program {@link #mahasiswa}. */
	private Combobox programCombobox = new Combobox();
	/** Combobox filter nomor semester (1..{@code max_semester_pilihan}); pra-isi dari {@link #semester}. */
	private Combobox semesterBox;
	/** Penanda konteks Semester Pendek/Antara ({@code null} untuk reguler); diteruskan dari konstruktor. */
	private Integer semesterPendek;

	/** Helper agenda/aktivitas perkuliahan yang dipakai saat baris grid dibuka ({@link MyDetail} onOpen) untuk menampilkan agenda kelas. */
	protected AktifitasPerkuliahanHelper aktifitasPerkuliahanHelper;

	/** @param semesterPendek penanda konteks Semester Pendek/Antara; {@code null} untuk reguler. */
	public AmbilDataIkutPerkuliahanHelper(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
		aktifitasPerkuliahanHelper = new AktifitasPerkuliahanHelper(Common.getCurrentUser().getMahasiswa(), null, true);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataIkutPerkuliahanHelper}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataIkutPerkuliahanHelper} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code PerkuliahanDao perkuliahanDao},
	 * {@code Session session}; operasi lokal: {@code render}(). Aturan bisnis bersama tetap berada pada kelas
	 * induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataIkutPerkuliahanHelper
	 */
	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		/** DAO {@link Perkuliahan} dipakai hanya untuk mengambil {@link #session} yang sedang berjalan. */
		private PerkuliahanDao perkuliahanDao = DaoFactory.getInstance().getPerkuliahanDao();

		/** Session Hibernate aktif, dipakai menghitung jumlah {@link Detailperkuliahan} mahasiswa pada {@link Perkuliahan} baris ini. */
		private Session session = perkuliahanDao.getCurrentSession();

		/**
		 * Merender satu baris jadwal {@link Perkuliahan}: tautan buka-agenda ({@link MyDetail}), info
		 * mata kuliah/dosen/jadwal/ruang, dan checkbox pilih yang disembunyikan bila mahasiswa sudah
		 * mengikuti/mengambil mata kuliah tersebut (label "Terpilih" vs "Tersedia").
		 *
		 * @param row  baris grid target; diberi atribut {@code myValue} (perkuliahan) dan
		 *             {@code checkbox} (state pilihan) untuk dibaca {@link #save()}
		 * @param arg1 data baris, di-cast ke {@link Perkuliahan}
		 */
		@Override
		public void render(Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Perkuliahan perkuliahan = (Perkuliahan) arg1;
			final Matakuliah matakuliah = perkuliahan.getMatakuliah();
			if (perkuliahan == null || matakuliah == null)
				return;

			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						int banyak = 1;
						try {
							banyak = Integer.parseInt(Common
									.getKonfigurasi("tampilan_jumlah_agenda_perkuliahan", banyak + "").getNilai());
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AmbilDataIkutPerkuliahanHelper.java:114");
						}
						aktifitasPerkuliahanHelper.initDetail(perkuliahan, new DataLoader() {

							@Override
							public void loadData(Object value) {

							}
						}, groupbox, 0, banyak);
						detail.appendChild(groupbox);
					}

				}
			});

			Integer jmlMk = ((Number) session.createCriteria(Detailperkuliahan.class)
					.setProjection(Projections.rowCount())

					.add(Restrictions.eq("mahasiswa", mahasiswa))

					.add(Restrictions.or(Restrictions.eq("ikutiPerkuliahan", perkuliahan),
							Restrictions.eq("perkuliahan", perkuliahan)))

					.uniqueResult()).intValue();

			row.setValign("top");row.setAttribute("myValue", perkuliahan);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setVisible(jmlMk.equals(0));
			checkbox.setParent(row);
			row.setValign("top");row.setAttribute("checkbox", checkbox);

			// checkbox.setChecked(!jmlMk.equals(0));

			final Label label = new Label(!jmlMk.equals(0) ? "Terpilih" : "Tersedia");

			checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (checkbox.isChecked()) {
						label.setValue("Terpilih");
						label.setStyle("font-weight:bold;color:red");
					} else {
						label.setValue("Tersedia");
						label.setStyle("font-weight:bold;color:green");
					}
				}
			});

			new Label(perkuliahan.getMatakuliah().getKode()).setParent(row);
			Vbox vbox = new Vbox();
			vbox.setParent(row);
			new ais.ui.util.MyHtml(perkuliahan.getMatakuliah().getKode() + " - " + perkuliahan.getMatakuliah().getNama()
					+ (perkuliahan.getMerupakan_paralel() != null && perkuliahan.getMerupakan_paralel()
							? " <font style='font-weight:bold;color:blue;'>(Paralel)</font>"
							: "")).setParent(vbox);
			MatakuliahPrasyaratAction.tampilPrasyarat(vbox, perkuliahan.getMatakuliah());
			new Label(perkuliahan.getMatakuliah().getSks() + "").setParent(row);
			new Label(perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama()).setParent(row);
			new Label((perkuliahan.getHari() == null ? "" : perkuliahan.getHari())).setParent(row);

			new Label(perkuliahan.getSemester() == null ? "" : perkuliahan.getSemester() + "").setParent(row);

			new Label(perkuliahan.getKelas()).setParent(row);
			new Label((perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()) + "-"
					+ (perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai())).setParent(row);
			new Label(perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getKodeRuangan()).setParent(row);

			label.setParent(row);

			if (!jmlMk.equals(0)) {
				label.setStyle("font-weight:bold;color:red");
			} else {
				label.setStyle("font-weight:bold;color:green");
			}

		}

	}

	/**
	 * Menyimpan seluruh baris grid yang dicentang pengguna sebagai {@link Detailperkuliahan} baru
	 * (atau memperbarui yang sudah ada) dengan {@code ikutiPerkuliahan} terisi. Setiap mata kuliah
	 * hanya diproses sekali (baris duplikat mata kuliah yang sama dilewati); jadwal yang kapasitas
	 * kelasnya sudah penuh dilewati dan pesannya dikumpulkan lalu ditampilkan sekaligus di akhir.
	 *
	 * @return selalu {@code true} (nilai kembalian dipertahankan untuk kompatibilitas pemanggil;
	 *         kegagalan per baris ditangani sebagai lewat/skip, bukan sebagai kegagalan keseluruhan)
	 * @throws Exception diteruskan dari kegagalan akses database
	 */
	@SuppressWarnings({ "unchecked" })
	public boolean save() throws Exception {

		Rows rows = grid.getRows();
		Session session = HibernateUtil.currentSession();
		rows = grid.getRows();
		List<Row> list = rows.getChildren();
		Tbmuser tbmuser = Common.getCurrentUser();

		Set<Long> matakuliahs = new HashSet<Long>();
		String peringatanKapasitasRuangan = "";
		for (Row row : list) {
			MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");

			if (checkbox.isChecked()) {
				final Long id;
				final Perkuliahan perkuliahan = (Perkuliahan) row.getAttribute("myValue");
				if (matakuliahs.contains(perkuliahan.getMatakuliah().getId())) {
					continue;
				}
				matakuliahs.add(perkuliahan.getMatakuliah().getId());

				try {
					id = (Long) (session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.isNotNull("ikutiPerkuliahan")).setProjection(Projections.property("id"))
							.add(Restrictions.eq("ikutiPerkuliahan", perkuliahan))
							.add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("semester", AmbilDataIkutPerkuliahanHelper.this.semester))

							.createCriteria("ikutiPerkuliahan", Criteria.LEFT_JOIN)
							.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
									: Restrictions.eq("statusSemesterPendek", semesterPendek))

							.uniqueResult());
				} catch (Exception e) {
					continue;
				}

				Detailperkuliahan detailperkuliahan = new Detailperkuliahan(tbmuser,
						AmbilDataIkutPerkuliahanHelper.class);
				if (id != null) {
					detailperkuliahan = (Detailperkuliahan) session.load(Detailperkuliahan.class, id);
				} else {

					Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(session, perkuliahan, false);

					jumlahUdahMasuk++;
					if (jumlahUdahMasuk > (perkuliahan.getKapasitasKelas() == null ? Ruang.getDefaultKapasitas()
							: perkuliahan.getKapasitasKelas())) {
						peringatanKapasitasRuangan += "Kapasitas kelas sudah penuh. Maksimal kapasitas kelas tesebut adalah "
								+ (perkuliahan.getKapasitasKelas() == null ? Ruang.getDefaultKapasitas()
										: perkuliahan.getKapasitasKelas())
								+ ", sedangkan anda mencoba masuk ke perkuliahan ini menjadi berjumlah "
								+ jumlahUdahMasuk + ". Pilihlah jadwal perkuliahan lainnya.\n";
						continue;
					}
				}

				detailperkuliahan.setNilaiHuruf("");
				detailperkuliahan.setTotalNilai(0.0);
				detailperkuliahan.setMahasiswa(mahasiswa);
				detailperkuliahan.setIkutiPerkuliahan(perkuliahan);
				detailperkuliahan.setSemester(AmbilDataIkutPerkuliahanHelper.this.semester);
				detailperkuliahan.setPersetujuan(Detailperkuliahan.BELUM_DISETUJUI);
				session.saveOrUpdate(detailperkuliahan);
			}
		}
		if (!peringatanKapasitasRuangan.trim().equals("")) {
			MyMessageboxConfig.show(peringatanKapasitasRuangan, "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
		}
		return true;
	}

	/**
	 * Membangun jendela pencarian dan pemilihan jadwal perkuliahan yang akan diikuti. Filter awal
	 * (Fakultas, Prodi, Program) dipra-isi dari data {@code mahasiswa} sendiri; hasil pencarian
	 * tampil di grid berpaginasi. Tombol Simpan memanggil {@link #save()} lalu menyegarkan tampilan
	 * pemanggil lewat {@code dataLoader}.
	 *
	 * @param mahasiswa   mahasiswa yang akan mengikuti perkuliahan
	 * @param tahunAjaran tahun akademik jadwal yang dicari
	 * @param semester    nomor semester default untuk filter pencarian
	 * @param dataLoader  dipanggil (dengan {@code value=true}) setelah simpan untuk menyegarkan
	 *                    tampilan KRS pemanggil
	 */
	public void display(final Mahasiswa mahasiswa, final String tahunAjaran, final Integer semester,
			final DataLoader dataLoader) {
		this.mahasiswa = mahasiswa;
		this.tahunAjaran = tahunAjaran;
		this.semester = semester;

		final MyWindow window = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setTitle("Ikut Perkuliahan");
		window.setWidth("90%");
		window.setHeight("90%");

		MyPanel panel = new MyPanel();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Perkuliahan");
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

		Columns columns = new Columns();
		columns.setParent(searchgrid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, jurusanCombobox);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas" + " Studi"));
		Common.selectComboItem(searchfakultas, this.mahasiswa.getJurusan().getFakultas());
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Matakuliah"));
		row.appendChild(kodeMk = new Textbox());
		kodeMk.setWidth("90%");

		kodeMk.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.insertCombo(jurusanCombobox, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", this.mahasiswa.getJurusan().getFakultas()));
		Common.selectComboItem(jurusanCombobox, this.mahasiswa.getJurusan());
		row.appendChild(jurusanCombobox);
		jurusanCombobox.setWidth("90%");

		jurusanCombobox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.insertCombo(programCombobox, "namaBaru", Program.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Session session = HibernateUtil.currentSession();
		Program programselected = (Program) session.createCriteria(Program.class)
				.add(Restrictions.eq("nama", this.mahasiswa.getProgram())).uniqueResult();
		Common.selectComboItem(programCombobox, programselected);
		row.appendChild(programCombobox);
		programCombobox.setWidth("90%");

		programCombobox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Matakuliah"));
		row.appendChild(namaMk = new Textbox());
		namaMk.setWidth("90%");

		namaMk.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterBox = new Combobox());

		int maxSemesterPilihan = 25;
		try {
			maxSemesterPilihan = Integer
					.parseInt(Common.getKonfigurasi("max_semester_pilihan", "25").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataIkutPerkuliahanHelper.java:426");

		}

		for (int i = 1; i < maxSemesterPilihan; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterBox.appendChild(comboitem);
		}
		Common.selectComboItem(semesterBox, this.semester);
		semesterBox.setWidth("90%");

		semesterBox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(new ais.ui.util.MyLabelConfig(this.tahunAjaran));

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
		column.appendChild(new Label());
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.appendChild(new Label(ais.common.Common.getBahasaConfig("Ikut")));
		column.setWidth("60px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode MK");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mata Kuliah");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(Common.getBahasa("label_dosen"));
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Hari");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelas");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ruang");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("5%");

		onSearchDefault(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				save();
				dataLoader.loadData(true);
				window.detach();

			}
		});
		button.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Mengisi ulang grid hasil pencarian jadwal {@link Perkuliahan} (non-paralel) sesuai filter
	 * toolbar saat ini (Fakultas, Prodi, Program, kode/nama Mata Kuliah, Semester, Tahun Akademik,
	 * dan konteks Semester Pendek). Hasil dibatasi {@code Common#MAX_RESULT} baris.
	 *
	 * @param event event ZK pemicu (tidak dipakai isinya, hanya penanda pemicu pencarian)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Program program = (Program) (programCombobox.getSelectedItem() == null ? null
				: programCombobox.getSelectedItem().getValue());
		Session session = HibernateUtil.currentSession();

		List<Perkuliahan> matakuliah = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
						: Restrictions.eq("statusSemesterPendek", semesterPendek))

				.createAlias("jurusan", "jurusan")
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))
				.add(jurusanCombobox.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusanCombobox, false))

				.add(program == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", program.getNama() ))
				
				.add(semesterBox.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("semester", semesterBox.getSelectedItem().getValue()))
				.add(Restrictions.eq("tahunAjaran", tahunAjaran))
				.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
						Restrictions.isNull("merupakan_paralel")))

				.createCriteria("matakuliah", "mk").addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("kode", kodeMk.getText().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("nama", namaMk.getText().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(matakuliah);
		grid.setRowRenderer(new MatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}
}
