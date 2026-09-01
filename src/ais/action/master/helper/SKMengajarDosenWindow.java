package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Jendela ZK ({@code MyWindow}) akademik untuk menu <b>"SK Mengajar Dosen"</b>: alat bantu admin
 * mengunggah satu berkas Surat Keputusan (SK) penugasan mengajar gabungan lalu menyalinnya menjadi
 * SK per dosen untuk sekelompok {@link Perkuliahan} (mata kuliah yang diampu) pada satu Tahun
 * Akademik &amp; Jenis Semester. Bukan mesin approval/workflow SK &mdash; murni utilitas
 * salin-lampiran + rekap SKS mengajar per dosen.
 *
 * <p><b>Alur:</b> {@link #init()} membangun toolbar filter (Fakultas, Prodi/Jurusan, Tahun
 * Akademik, Jenis Semester, Program, nama Dosen) yang tiap perubahannya memicu
 * {@link #initSpreadsheet()} lewat {@code Common.createDefaultTimer}; kotak upload/download SK
 * gabungan ({@link LampiranLain#createDownloadUploadFileLain}, jenis lampiran
 * {@code "sk_penugasan_pengajaran_dosen_gabungan"}); dan tombol "Masukkan ke SK Dosen" yang, untuk
 * setiap dosen tercentang di grid ({@link #dipilihs}), menghitung kode {@link LampiranLain} unik
 * (gabungan id dosen + tahun ajaran + kode jenis semester) lalu men-<i>copy</i> SK gabungan yang
 * baru diupload ({@link #suratsk}) menjadi lampiran milik dosen tersebut
 * ({@code LampiranLain.setCopyDari}), masing-masing dalam transaksi Hibernate tersendiri (kegagalan
 * satu dosen tidak menggagalkan dosen lain &mdash; hanya dicatat lewat {@code ErrorAuditUtil}).</p>
 *
 * <p><b>{@link #initSpreadsheet()}</b> (dipanggil ulang tiap filter berubah): query
 * {@link Perkuliahan} aktif (bukan perkuliahan paralel) yang salah satu dari 10 slot dosennya
 * ({@code dosen1}..{@code dosen10}) cocok filter Dosen/Fakultas/Prodi/Tahun Ajaran/Jenis Semester/
 * Program; hasil dikelompokkan per {@link Dosen} ({@code perkuliahan.populateDosen()}) ke dalam
 * {@link java.util.TreeMap} (terurut alami by Dosen) sehingga tiap baris grid = satu dosen dengan
 * daftar mata kuliah yang diampu, total SKS (dibagi rata sesuai {@code jumlahDosen} pengampu
 * matakuliah tsb.), checkbox pilih, dan tombol lihat/upload SK individu miliknya.</p>
 *
 * <p><b>Constructor:</b> constructor kosong membungkus {@link #init()} dengan penanganan error
 * ramah pengguna ({@link PesanFormalHelper#tampilkanGagalException}); constructor
 * {@code (title, border, closable)} dan
 * {@code (title, border, closable, tahunAjaran, jenisSemester, jurusanDosen, fakultas, program, dosen)}
 * membiarkan exception {@link #init()} menjalar ke pemanggil. Varian terakhir dipakai saat jendela
 * dibuka dari konteks yang SUDAH tahu filter awal (mis. dari halaman detail dosen/jurusan) &mdash;
 * combo terkait langsung diisi &amp; DINONAKTIFKAN (tak bisa diubah) di {@link #init()}.</p>
 *
 * <p><b>Kuirk:</b> pencarian nama dosen ({@link #searchdosen}) membuat LEFT JOIN alias ke SEMUA 10
 * slot {@code dosen1}..{@code dosen10} sekaligus (bukan hanya slot yang terisi), lalu OR
 * {@code ilike} nama pada tiap alias &mdash; pola query yang sama dipakai berulang di beberapa
 * class helper AIS lain yang berurusan dengan model co-dosen 10-slot {@link Perkuliahan}.</p>
 *
 * @see MyWindow
 */
public class SKMengajarDosenWindow extends MyWindow {

	/** UID serialisasi standar (komponen ZK bisa dipasivasi antar-request); tidak dipakai untuk logika versi. */
	private static final long serialVersionUID = 790038368339375113L;

	/** Filter Fakultas; dikunci nonaktif bila jendela dibuka dengan {@link #fakultasDosen} sudah ditentukan. */
	private Combobox fakultas;
	/** Filter Prodi/Jurusan; dikunci nonaktif bila jendela dibuka dengan {@link #jurusanDosen} sudah ditentukan. */
	private Combobox jurusan;
	/** Filter Tahun Akademik; dikunci nonaktif bila jendela dibuka dengan {@link #tahunAjaran} sudah ditentukan. */
	private Combobox searchTahunAjaran;
	/** Filter Jenis Semester (Ganjil/Genap/SP); default mengikuti semester berjalan, dikunci bila {@link #jenisSemester} sudah ditentukan. */
	private Combobox jenis_semester;

	/** Area tengah borderlayout tempat grid rekap dosen dirender ulang tiap {@link #initSpreadsheet()}. */
	private Center center = new Center();

	/** Tahun ajaran awal (bila diisi lewat constructor filter-tetap) yang mengunci {@link #searchTahunAjaran}. */
	private String tahunAjaran;

	/** Jenis semester awal (bila diisi lewat constructor filter-tetap) yang mengunci {@link #jenis_semester}. */
	private String jenisSemester;

	/** Prodi/Jurusan awal (bila diisi lewat constructor filter-tetap) yang mengunci {@link #jurusan}. */
	private Jurusan jurusanDosen;

	/** Program awal (bila diisi lewat constructor filter-tetap) yang mengunci {@link #searchprogram}. */
	private String program;

	/** Filter Program (mis. S1/S2); dikunci nonaktif bila jendela dibuka dengan {@link #program} sudah ditentukan. */
	private Combobox searchprogram;

	/** Dosen awal (bila diisi lewat constructor filter-tetap) dipakai sebagai kriteria pencarian perkuliahan; tidak mengunci field UI (tidak ada combo dosen). */
	private Dosen dosen;

	/** Fakultas awal (bila diisi lewat constructor filter-tetap) yang mengunci {@link #fakultas}. */
	private Fakultas fakultasDosen;

	/** ID dosen yang checkbox-nya sedang tercentang di grid; sumber daftar penerima saat tombol "Masukkan ke SK Dosen" diklik. */
	private Set<Long> dipilihs = new HashSet<Long>();
	/** Peta id dosen -> checkbox baris grid terkait; dipakai tombol "pilih semua" untuk menyinkronkan status semua checkbox sekaligus. */
	private Map<Long, MyCheckboxConfig> semuas = new HashMap<Long, MyCheckboxConfig>();

	/** SK gabungan yang baru diupload lewat kotak upload di {@link #init()}; sumber salinan untuk tombol "Masukkan ke SK Dosen". {@code null} berarti belum ada berkas diupload pada sesi jendela ini. */
	protected LampiranLain suratsk = null;

	/** Kotak pencarian nama dosen (ilike, dicocokkan ke ke-10 slot {@code dosen1}..{@code dosen10}); lihat {@link #initSpreadsheet()}. */
	private Textbox searchdosen;

	/**
	 * Constructor default: panggil {@link #init()} tanpa filter awal apa pun (semua combo kosong,
	 * grid baru terisi setelah pengguna memilih filter). Kegagalan {@link #init()} ditangkap dan
	 * ditampilkan sebagai pesan error ramah pengguna ({@link PesanFormalHelper}), bukan dilempar ke
	 * pemanggil.
	 */
	public SKMengajarDosenWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"menampilkan jendela SK Mengajar Dosen",
					e, new String[] {
							"Muat ulang (refresh) halaman ini lalu coba buka jendela kembali.",
							"Periksa koneksi jaringan Anda ke server aplikasi.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	/** Constructor dasar {@link MyWindow} tanpa filter awal; {@link #init()} dijalankan langsung (exception diteruskan ke pemanggil, tidak ditangkap seperti pada constructor default). */
	public SKMengajarDosenWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	/**
	 * Constructor dengan filter awal terkunci: semua parameter non-null yang diisi akan mengunci
	 * (disabled) combo terkaitnya di {@link #init()} sehingga pengguna tidak bisa mengubahnya —
	 * dipakai saat jendela dibuka dari konteks yang sudah menentukan konteksnya sendiri (mis. dari
	 * halaman profil dosen/jurusan tertentu).
	 *
	 * @param tahunAjaran   tahun ajaran yang dikunci pada {@link #searchTahunAjaran}; {@code null} = bebas dipilih.
	 * @param jenisSemester jenis semester yang dikunci pada {@link #jenis_semester}; {@code null} = bebas dipilih.
	 * @param jurusanDosen  prodi/jurusan yang dikunci pada {@link #jurusan}; {@code null} = bebas dipilih.
	 * @param fakultas      fakultas yang dikunci pada {@link #fakultas}; {@code null} = bebas dipilih.
	 * @param program       program yang dikunci pada {@link #searchprogram}; {@code null} = bebas dipilih.
	 * @param dosen         dosen tunggal sebagai kriteria filter perkuliahan (tidak mengunci field UI apa pun).
	 */
	public SKMengajarDosenWindow(String title, String border, boolean closable, String tahunAjaran,
			String jenisSemester, Jurusan jurusanDosen, Fakultas fakultas, String program, Dosen dosen) {
		super(title, border, closable);
		this.tahunAjaran = tahunAjaran;
		this.jenisSemester = jenisSemester;
		this.jurusanDosen = jurusanDosen;
		this.fakultasDosen = fakultas;
		this.program = program;
		this.dosen = dosen;
		init();
	}

	/**
	 * Bangun seluruh UI jendela: borderlayout dengan toolbar filter di North (Fakultas, Prodi,
	 * Tahun Akademik, Jenis Semester, Program, nama Dosen — tiap {@code onChange} memicu
	 * {@link #initSpreadsheet()} via timer default) dan kotak upload SK gabungan + tombol
	 * "Masukkan ke SK Dosen" (menyalin {@link #suratsk} ke tiap dosen tercentang di
	 * {@link #dipilihs}, kode lampiran dibentuk dari id dosen + tahun ajaran + kode jenis semester,
	 * masing-masing dalam transaksi Hibernate tersendiri). Combo yang filter awalnya sudah diisi
	 * lewat constructor filter-tetap langsung dipilih &amp; dikunci ({@code setDisabled(true)}).
	 * Area {@link #center} (grid rekap) dipasang dan langsung dipicu render pertama via timer.
	 * Tombol "Tutup" men-detach jendela.
	 */
	@SuppressWarnings("deprecation")
	private void init() {

		searchTahunAjaran = Common.generateTahunAjaran(searchTahunAjaran = new Combobox());
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		jenis_semester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenis_semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenis_semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		jenis_semester.appendChild(comboitem);

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		// setTitle("Rekap Pembayaran Host to Host");
		// setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(this.searchTahunAjaran);
		searchTahunAjaran.setWidth("90%");
		searchTahunAjaran.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});

		if (tahunAjaran != null) {
			Common.selectComboItem(true, searchTahunAjaran, tahunAjaran);
			searchTahunAjaran.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(this.jenis_semester);
		jenis_semester.setWidth("90%");
		Common.selectComboItem(jenis_semester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		jenis_semester.setReadonly(true);

		if (jenisSemester != null) {
			Common.selectComboItem(true, jenis_semester, jenisSemester);
			jenis_semester.setDisabled(true);
		}

		jenis_semester.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});

		searchprogram = Common.initPrograms(null);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(this.searchprogram);
		searchprogram.setWidth("90%");

		if (program != null) {
			Common.selectComboItem(true, searchprogram, program);
			searchprogram.setDisabled(true);
		}

		searchprogram.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(this.searchdosen = new Textbox());
		searchdosen.setWidth("90%");

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "6");
		row.setParent(rows);

		Hbox hboxa = new Hbox();
		hboxa.setParent(row);

		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, -Common.randLong(), "sk_penugasan_pengajaran_dosen_gabungan",
				"SK", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						suratsk = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);
		hbox.setParent(hboxa);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Masukkan ke SK Dosen",
				"/img/stock_data_edit_table.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (suratsk == null) {
					MyMessageboxConfig.show("File SK harus diupload terlebih dulu", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (dipilihs.isEmpty()) {
					MyMessageboxConfig.show("Pilihlah minimal satu dosen", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				String tahun = (String) searchTahunAjaran.getSelectedItem().getValue();
				String jenisSemesterNumber = jenis_semester.getSelectedItem().getValue().equals(Perkuliahan.SP) ? "3"
						: jenis_semester.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "2";

				Session session = StreamingHibernateUtil.getInstance().currentSession();
				for (Long idDosen : dipilihs) {

					try {
						String kode = Common.maxPanjangAkhir("000000000000000000000" + idDosen + "00"
								+ StringUtils.split(tahun, "/")[0] + jenisSemesterNumber, 14);
						kode = "1" + kode;
						Long l = Long.parseLong(kode);

						LampiranLain lampiranLain = LampiranLain.ambil(l, "sk_penugasan_pengajaran_dosen_gabungan");
						if (lampiranLain == null) {
							lampiranLain = new LampiranLain();
						}
						lampiranLain.setRef(l);
						lampiranLain.setJenis("sk_penugasan_pengajaran_dosen_gabungan");
						lampiranLain.setCopyDari(suratsk);
						session.getTransaction().begin();
						session.saveOrUpdate(lampiranLain);
						session.getTransaction().commit();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/SKMengajarDosenWindow.java:325");
					}
				}
				StreamingHibernateUtil.getInstance().closeSession();

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});
		print.setParent(hboxa);

		if (jurusanDosen != null) {
			Common.selectComboItem(true, jurusan, jurusanDosen);
			jurusan.setDisabled(true);

		}
		if (fakultasDosen != null) {
			Common.selectComboItem(true, fakultas, fakultasDosen);
			fakultas.setDisabled(true);
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				SKMengajarDosenWindow.this.detach();
			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
	}

	// private void

	/**
	 * Bangun ulang grid rekap dosen di {@link #center} sesuai filter aktif. Query
	 * {@link Perkuliahan} aktif, bukan perkuliahan paralel, dengan salah satu slot
	 * {@code dosen1}..{@code dosen10} cocok filter {@link #dosen}/{@link #searchdosen} (ilike
	 * nama, LEFT JOIN ke semua 10 alias sekaligus), serta filter Tahun Ajaran, Jenis Semester
	 * (SP dicek via {@code statusSemesterPendek}; Ganjil/Genap via kombinasi
	 * {@code statusSemesterPendek} null + {@code ganjilGenap}), Program, dan Prodi/Fakultas.
	 * Hasil dikelompokkan per {@link Dosen} ({@code perkuliahan.populateDosen()}) ke
	 * {@link java.util.TreeMap} terurut, lalu dirender sebagai grid dengan: checkbox pilih per
	 * dosen (disinkronkan ke {@link #dipilihs}/{@link #semuas}), nama dosen, total SKS (SKS
	 * matakuliah dibagi {@code jumlahDosen} pengampu, dijumlah semua matakuliah dosen tsb.),
	 * rincian tekstual matakuliah yang diampu, dan kotak unduh/unggah SK individu dosen
	 * (kode lampiran dari id dosen + tahun ajaran + kode jenis semester). Baris ringkasan "SKS
	 * Total" ditambahkan di akhir. No-op (grid tidak dibangun) bila hasil query kosong.
	 */
	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {
		Common.clear(center);

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Criterion criterion = dosen == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("dosen1", dosen), Restrictions.eq("dosen2", dosen));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

		if (!searchdosen.getValue().trim().isEmpty()) {
			criteria.createAlias("dosen1", "dosen1", Criteria.LEFT_JOIN)
					.createAlias("dosen2", "dosen2", Criteria.LEFT_JOIN)
					.createAlias("dosen3", "dosen3", Criteria.LEFT_JOIN)
					.createAlias("dosen4", "dosen4", Criteria.LEFT_JOIN)
					.createAlias("dosen5", "dosen5", Criteria.LEFT_JOIN)
					.createAlias("dosen6", "dosen6", Criteria.LEFT_JOIN)
					.createAlias("dosen7", "dosen7", Criteria.LEFT_JOIN)
					.createAlias("dosen8", "dosen8", Criteria.LEFT_JOIN)
					.createAlias("dosen9", "dosen9", Criteria.LEFT_JOIN)
					.createAlias("dosen10", "dosen10", Criteria.LEFT_JOIN);

			Criterion criterionNamaDosn = Restrictions.sqlRestriction("1=1");
			criterionNamaDosn = Restrictions.ilike("dosen1.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE);

			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen2.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen3.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen4.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen5.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen6.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen7.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen8.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen9.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen10.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));

			criteria.add(criterionNamaDosn);
		}

		List<Perkuliahan> perkuliahans = criteria

				.add(Restrictions.isNull("perkuliahan_paralel"))

				.add(criterion)

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))

				.add(jenis_semester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: jenis_semester.getSelectedItem().getValue().equals(Perkuliahan.SP)
								? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
								: Restrictions.and(Restrictions.isNull("statusSemesterPendek"),
										Restrictions.eq("ganjilGenap", jenis_semester.getSelectedItem().getValue())))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

				.createAlias("jurusan", "jurusan")

				.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", fakultas, false))

				.setMaxResults(1048576).addOrder(Order.desc("id")).list();

		if (perkuliahans.size() == 0) {
			return;
		}

		MyGrid grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(5);
		grid.setParent(center);
		grid.getPagingChild().setMold("os");
		grid.setPagingPosition("top");

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		final MyCheckboxConfig semuaPilih = new MyCheckboxConfig();
		semuaPilih.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dipilihs.clear();
				if (semuaPilih.isChecked()) {
					dipilihs.addAll(semuas.keySet());
				}

				for (MyCheckboxConfig c : semuas.values()) {
					c.setChecked(semuaPilih.isChecked());
				}
			}
		});
		semuaPilih.setParent(column);
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dosen");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pengajaran");

		TreeMap<Dosen, List<Perkuliahan>> dosensMap = new TreeMap<Dosen, List<Perkuliahan>>();
		for (Perkuliahan perkuliahan : perkuliahans) {

			Map<String, Dosen> map = perkuliahan.populateDosen();
			for (Dosen d : map.values()) {
				if (dosensMap.containsKey(d)) {
					dosensMap.get(d).add(perkuliahan);
				} else {
					List<Perkuliahan> itemDetails = new ArrayList<Perkuliahan>();
					itemDetails.add(perkuliahan);
					dosensMap.put(d, itemDetails);
				}
			}

		}

		Rows rows = new Rows();
		rows.setParent(grid);

		String tahun = (String) searchTahunAjaran.getSelectedItem().getValue();
		String jenisSemesterNumber = jenis_semester.getSelectedItem().getValue().equals(Perkuliahan.SP) ? "3"
				: jenis_semester.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "2";
		semuas.clear();
		Double sksTotal = 0.0;
		for (final Dosen dosen : dosensMap.keySet()) {

			List<Perkuliahan> perkulishsnasDosen = dosensMap.get(dosen);

			String itemYangDipinjam = "";
			Double sks = 0.0;
			for (Perkuliahan perkul : perkulishsnasDosen) {
				Double sksDibagi = perkul.getMatakuliah().getSks().doubleValue()
						/ perkul.getJumlahDosen().doubleValue();

				sks += sksDibagi;
				String s = perkul.getMatakuliah().getKode() + "-" + perkul.getMatakuliah() + "=> jml dosen: "
						+ perkul.getJumlahDosen() + ", sks mk:" + perkul.getMatakuliah().getSks() + " sks, total: "
						+ Common.numberFormat.get().format(sksDibagi) + "sks";
				itemYangDipinjam += itemYangDipinjam.isEmpty() ? s : " ,\n" + s;
			}

			sksTotal += sks;

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);

			final MyCheckboxConfig myCheckboxConfig = new MyCheckboxConfig();
			myCheckboxConfig.setChecked(dipilihs.contains(dosen.getId()));
			myCheckboxConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (myCheckboxConfig.isChecked()) {
						dipilihs.add(dosen.getId());
					} else {
						dipilihs.remove(dosen.getId());
					}
				}
			});
			semuas.put(dosen.getId(), myCheckboxConfig);

			row.appendChild(myCheckboxConfig);

			row.appendChild(new Label(dosen.getNama()));
			row.appendChild(new Label(Common.numberFormat.get().format(sks)));
			Vbox vbox = new Vbox();
			row.appendChild(vbox);
			vbox.appendChild(new MyLabelKecil(itemYangDipinjam));

			String kode = Common.maxPanjangAkhir("000000000000000000000" + dosen.getId() + "00"
					+ StringUtils.split(tahun, "/")[0] + jenisSemesterNumber, 14);
			kode = "1" + kode;
			Long l = Long.parseLong(kode);

			System.out.println("l -> " + l);

			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, l, "sk_penugasan_pengajaran_dosen_gabungan", "SK Gabungan",
					false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true);
			hbox.setParent(vbox);

		}

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		row.appendChild(new Label());
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("SKS Total")));
		row.appendChild(new Label(Common.numberFormat.get().format(sksTotal)));
		row.appendChild(new Label());

	}
}
