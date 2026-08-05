package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Div;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Kelas;
import ais.database.model.Mahasiswa;
import ais.database.model.PenjadwalanMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Popup <b>&ldquo;Ambil Data Mahasiswa&rdquo;</b> pada modul <i>Manajemen Penjadwalan Mahasiswa</i>.
 *
 * <h2>Untuk apa layar ini</h2>
 * <p>
 * Layar ini dipakai <b>petugas/admin</b> untuk <b>memasukkan mahasiswa ke dalam sebuah kelas
 * perkuliahan secara langsung</b>, tanpa menunggu mahasiswa mengambil/mengisi KRS sendiri. Admin
 * cukup mencari mahasiswa (berdasarkan NIM, rentang NIM, nama, tahun angkatan, atau status), lalu
 * mencentang siapa saja yang hendak didaftarkan; mahasiswa yang tercentang otomatis terdaftar pada
 * kelas tersebut. Ini menyelesaikan kebutuhan &ldquo;mahasiswa langsung menerima KRS tanpa harus
 * mendaftar sendiri&rdquo;.
 * </p>
 *
 * <h2>Alur kerja (langkah admin)</h2>
 * <ol>
 * <li>Popup dibuka dari panel <i>Daftar Mahasiswa</i> pada layar penjadwalan kelas, membawa konteks
 * kelas yang sedang dikelola: {@code kelas}, {@code jurusan} (prodi + fakultas), {@code tahunAjaran},
 * dan {@code semester}.</li>
 * <li>Admin menyaring kandidat mahasiswa memakai kolom pencarian di bagian atas, lalu menekan
 * <b>Cari</b>.</li>
 * <li>Setiap baris menampilkan kotak centang. Mahasiswa yang <b>sudah terdaftar</b> pada kelas ini
 * ditampilkan tercentang dan terkunci (tidak bisa dicentang ganda), sehingga tidak mungkin membuat
 * pendaftaran rangkap.</li>
 * <li>Admin menekan <b>Tambahkan Terpilih</b> untuk mendaftarkan yang dicentang, atau
 * <b>Tambahkan Semua</b> untuk mendaftarkan seluruh mahasiswa yang cocok dengan filter sekarang
 * (yang belum terdaftar saja).</li>
 * </ol>
 *
 * <h2>Kebijakan basis data &amp; sesi</h2>
 * <p>
 * Seluruh akses basis data memakai {@link HibernateUtil#currentSession()} &mdash; sesi terikat
 * thread ZK yang <b>ditutup otomatis</b> oleh kerangka aplikasi di akhir permintaan. Oleh karena
 * itu kelas ini <b>tidak boleh</b> menutup sesi secara manual, dan memang tidak melakukannya. Kelas
 * ini juga <b>tidak</b> membuka {@code openSession()} maupun {@code currentNativeSession()},
 * sehingga tidak ada sesi yang perlu di-{@code close()} di blok {@code finally}.
 * </p>
 *
 * <h2>Efisiensi (memori &amp; kueri)</h2>
 * <p>
 * Versi sebelumnya menjalankan <b>satu kueri COUNT untuk SETIAP baris</b> hanya demi mengetahui
 * apakah mahasiswa sudah terdaftar &mdash; pola N+1 yang boros kueri saat halaman berisi banyak
 * baris. Versi ini memuat <b>sekali</b> himpunan id mahasiswa yang sudah terdaftar pada
 * (kelas, tahun ajaran, semester) ke dalam {@link Set} ({@link #idMahasiswaTerdaftar}), lalu tiap
 * baris cukup memeriksa keanggotaan himpunan &mdash; dari <i>N</i> kueri menjadi <b>1 kueri</b> per
 * pencarian. Himpunan hanya menyimpan {@link Long} id (bukan objek {@link Mahasiswa} penuh), sehingga
 * jejak memorinya kecil. Daftar mahasiswa sendiri tetap dibatasi per-halaman
 * ({@link Common#ROWS_COUNT_ON_PAGE}) melalui {@link Paging} sehingga tidak pernah memuat seluruh
 * tabel sekaligus.
 * </p>
 *
 * <h2>Kompatibilitas</h2>
 * <p>
 * Ditulis agar tetap kompatibel <b>Java 1.7</b> (tanpa lambda, tanpa <i>diamond operator</i>, tanpa
 * <i>try-with-resources</i>): tipe generik dituliskan lengkap dan kelas anonim dipakai untuk
 * <i>event listener</i>. Semua {@code try/catch} mempertahankan gaya <b>Java 1.6</b> (menangkap
 * {@link Exception}, bukan <i>multi-catch</i>). Antarmuka dirancang <b>responsif</b>: kartu pencarian
 * memakai <i>flex-wrap</i> sehingga kolom filter menyusun ulang diri dan tetap terbaca di layar
 * sempit (mobile) maupun lebar (desktop); tombol aksi utama diberi label yang jelas dalam bahasa
 * awam, bukan istilah teknis.
 * </p>
 *
 * <h2>Cara pakai (contoh)</h2>
 *
 * <pre>
 * AmbilDataMahasiswaForManajemenPenjadwalanMahasiswaHelper picker =
 *         new AmbilDataMahasiswaForManajemenPenjadwalanMahasiswaHelper(
 *                 tahunAjaran, program, jurusan, semester, kelas);
 * picker.display(dataLoader); // dataLoader.loadData(null) dipanggil setelah penyimpanan berhasil
 * </pre>
 *
 * <p>
 * Catatan pemeliharaan: seluruh gaya visual dikumpulkan sebagai konstanta {@code GAYA_*} di bagian
 * atas kelas agar mudah diseragamkan; logika kriteria pencarian dipusatkan di {@link #initCriteria(boolean)}
 * sehingga pencarian, penomoran halaman, dan &ldquo;tambahkan semua&rdquo; selalu memakai penyaringan
 * yang sama persis.
 * </p>
 *
 * @author eCampus
 */
public class AmbilDataMahasiswaForManajemenPenjadwalanMahasiswaHelper {

	// ---- Gaya visual terpusat (ubah sekali, berlaku ke seluruh popup) ----------------------------

	private static final String GAYA_KARTU_CARI = "box-sizing:border-box;width:100%;padding:12px 14px;"
			+ "background:#ffffff;border:1px solid #e6edf5;border-radius:14px;"
			+ "box-shadow:0 8px 22px rgba(15,23,42,0.06);";
	private static final String GAYA_BARIS_FILTER = "display:flex;flex-wrap:wrap;gap:10px 14px;align-items:flex-end;";
	private static final String GAYA_GRUP_FILTER = "display:flex;flex-direction:column;gap:3px;min-width:130px;flex:1 1 150px;";
	private static final String GAYA_LABEL_FILTER = "font-size:11px;font-weight:700;color:#334155;";
	private static final String GAYA_KOTAK_FILTER = "width:100%;box-sizing:border-box;border:1px solid #cbd5e1;"
			+ "border-radius:8px;font-size:12px;padding:1px 2px;";

	// ---- Konteks kelas yang sedang dikelola (dari pemanggil, tidak berubah) -----------------------

	private final Kelas kelas;
	private final String tahunAjaran;
	private final String program;
	private final Integer semester;
	private final Jurusan jurusan;

	// ---- Komponen UI + status runtime -------------------------------------------------------------

	private MyGrid grid;
	private Textbox nim;
	private Textbox nama;
	private Intbox tahunangkatan;
	private Textbox dariNim;
	private Textbox sampaiNim;
	private final Combobox searchstatusmahasiswa = new Combobox();
	private final Paging paging;

	/**
	 * Himpunan id mahasiswa yang SUDAH terdaftar pada (kelas, tahun ajaran, semester) ini. Dimuat
	 * sekali per pencarian di {@link #onSearchDefault(Event)} lalu dibaca oleh renderer untuk
	 * menandai baris yang sudah terdaftar &mdash; menggantikan kueri COUNT per-baris.
	 */
	private Set<Long> idMahasiswaTerdaftar = new HashSet<Long>();

	/**
	 * Menyiapkan popup untuk sebuah kelas tertentu.
	 *
	 * @param tahunAjaran tahun ajaran kelas (mis. {@code "2026/2027"}); dipakai menyaring pendaftaran
	 *                    yang sudah ada dan menebak tahun angkatan awal filter.
	 * @param program     nama program (boleh {@code null} = tanpa penyaringan program).
	 * @param jurusan     prodi kelas (memuat fakultas); dipakai untuk konteks tampilan dan filter.
	 * @param semester    semester kelas.
	 * @param kelas        kelas tujuan tempat mahasiswa akan didaftarkan.
	 */
	public AmbilDataMahasiswaForManajemenPenjadwalanMahasiswaHelper(String tahunAjaran, String program,
			Jurusan jurusan, Integer semester, Kelas kelas) {
		this.kelas = kelas;
		this.tahunAjaran = tahunAjaran;
		this.program = program;
		this.semester = semester;
		this.jurusan = jurusan;

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/**
	 * Penggambar baris grid mahasiswa. Setiap baris berisi kotak centang + NIM + Nama + Tahun
	 * Angkatan. Mahasiswa yang sudah terdaftar (ada di {@link #idMahasiswaTerdaftar}) tampil
	 * tercentang dan terkunci agar tidak terjadi pendaftaran ganda.
	 */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;

			boolean sudahTerdaftar = mahasiswa.getId() != null && idMahasiswaTerdaftar.contains(mahasiswa.getId());

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);
			checkbox.setChecked(sudahTerdaftar);
			checkbox.setDisabled(sudahTerdaftar);
			if (sudahTerdaftar) {
				checkbox.setTooltiptext("Mahasiswa ini sudah terdaftar pada kelas ini");
			}

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(String.valueOf(mahasiswa.getTahunangkatan())).setParent(arg0);
		}
	}

	/**
	 * Mendaftarkan mahasiswa yang <b>dicentang &amp; belum terkunci</b> ke kelas ini.
	 *
	 * <p>
	 * Memakai {@link HibernateUtil#currentSession()} (ditutup otomatis). Baris yang terkunci
	 * ({@code disabled}) dilewati karena berarti mahasiswa sudah terdaftar. Setiap baris dibungkus
	 * {@code try/catch} agar satu baris bermasalah tidak menggagalkan baris lain.
	 * </p>
	 *
	 * @return jumlah pendaftaran baru yang tersimpan (untuk ditampilkan sebagai konfirmasi).
	 */
	public int save() {
		Session session = HibernateUtil.currentSession();
		int tersimpan = 0;

		Rows rows = grid.getRows();
		if (rows == null) {
			return 0;
		}
		List<?> daftarBaris = rows.getChildren();
		for (int i = 0; i < daftarBaris.size(); i++) {
			try {
				Row row = (Row) daftarBaris.get(i);
				MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
				if (checkbox == null || !checkbox.isChecked() || checkbox.isDisabled()) {
					continue;
				}
				Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");
				if (mahasiswa == null || mahasiswa.getId() == null) {
					continue;
				}
				// Pengaman terakhir terhadap pendaftaran ganda: lewati bila ternyata sudah terdaftar.
				if (idMahasiswaTerdaftar.contains(mahasiswa.getId())) {
					continue;
				}

				PenjadwalanMahasiswa penjadwalanMahasiswa = new PenjadwalanMahasiswa();
				penjadwalanMahasiswa.setKelas(kelas);
				penjadwalanMahasiswa.setTahunAjaran(tahunAjaran);
				penjadwalanMahasiswa.setSemester(semester);
				penjadwalanMahasiswa.setMahasiswa(mahasiswa);
				session.save(penjadwalanMahasiswa);
				idMahasiswaTerdaftar.add(mahasiswa.getId());
				tersimpan++;
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaForManajemenPenjadwalanMahasiswaHelper.java:save");
			}
		}
		return tersimpan;
	}

	/**
	 * Mendaftarkan <b>seluruh</b> mahasiswa yang cocok dengan filter pencarian sekarang (bukan hanya
	 * halaman yang tampak), melewati yang sudah terdaftar.
	 *
	 * <p>
	 * Memakai {@link HibernateUtil#currentSession()} (ditutup otomatis). Untuk tiap mahasiswa
	 * dilakukan pemeriksaan cepat apakah pendaftaran sudah ada, sehingga aman dijalankan berulang
	 * tanpa membuat data rangkap.
	 * </p>
	 *
	 * @return jumlah pendaftaran baru yang tersimpan.
	 */
	@SuppressWarnings("unchecked")
	public int saveSemua() {
		List<Mahasiswa> mahasiswas = initCriteria(true).list();
		Session session = HibernateUtil.currentSession();
		int tersimpan = 0;
		for (int i = 0; i < mahasiswas.size(); i++) {
			Mahasiswa mahasiswa = mahasiswas.get(i);
			if (mahasiswa == null) {
				continue;
			}
			try {
				PenjadwalanMahasiswa penjadwalanMahasiswa = (PenjadwalanMahasiswa) session
						.createCriteria(PenjadwalanMahasiswa.class).add(Restrictions.eq("kelas", kelas))
						.add(Restrictions.eq("tahunAjaran", tahunAjaran)).add(Restrictions.eq("semester", semester))
						.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();
				if (penjadwalanMahasiswa == null) {
					penjadwalanMahasiswa = new PenjadwalanMahasiswa();
					penjadwalanMahasiswa.setKelas(kelas);
					penjadwalanMahasiswa.setTahunAjaran(tahunAjaran);
					penjadwalanMahasiswa.setSemester(semester);
					penjadwalanMahasiswa.setMahasiswa(mahasiswa);
					session.save(penjadwalanMahasiswa);
					if (mahasiswa.getId() != null) {
						idMahasiswaTerdaftar.add(mahasiswa.getId());
					}
					tersimpan++;
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaForManajemenPenjadwalanMahasiswaHelper.java:saveSemua");
			}
		}
		return tersimpan;
	}

	/**
	 * Membangun dan menampilkan popup secara modal.
	 *
	 * @param dataLoader dipanggil ({@code loadData(null)}) setelah penyimpanan berhasil agar panel
	 *                   pemanggil menyegarkan daftar mahasiswanya.
	 */
	public void display(final DataLoader dataLoader) {

		final MyWindow window = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setTitle("Ambil Data Mahasiswa");
		window.setWidth("92%");
		window.setHeight("92%");
		window.setMaximizable(true);
		window.setSizable(true);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		// ================= BAGIAN ATAS: penjelasan + konteks kelas + kartu pencarian =================
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setBorder("none");
		north.setAutoscroll(true);

		Div wadahAtas = new Div();
		wadahAtas.setParent(north);
		wadahAtas.setStyle("box-sizing:border-box;width:100%;padding:10px 12px 4px;display:flex;"
				+ "flex-direction:column;gap:10px;background:#f8fafc;");

		// Penjelasan singkat + rangkuman konteks kelas (bahasa awam, tanpa istilah teknis).
		MyHtml info = new MyHtml(bangunHtmlPenjelasan());
		info.setParent(wadahAtas);

		// Kartu pencarian yang responsif (flex-wrap): kolom filter menyusun ulang di layar sempit.
		Div kartuCari = new Div();
		kartuCari.setParent(wadahAtas);
		kartuCari.setStyle(GAYA_KARTU_CARI);

		Div barisFilter = new Div();
		barisFilter.setParent(kartuCari);
		barisFilter.setStyle(GAYA_BARIS_FILTER);

		nim = new Textbox();
		tambahGrupFilter(barisFilter, "NIM mengandung", nim);
		dariNim = new Textbox();
		tambahGrupFilter(barisFilter, "Dari NIM", dariNim);
		sampaiNim = new Textbox();
		tambahGrupFilter(barisFilter, "Sampai NIM", sampaiNim);
		nama = new Textbox();
		tambahGrupFilter(barisFilter, "Nama mahasiswa", nama);

		int tahunAwal = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		if (tahunAjaran != null && !tahunAjaran.trim().isEmpty()) {
			try {
				tahunAwal = Integer.parseInt(StringUtils.split(tahunAjaran, "/")[0].trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		tahunangkatan = new Intbox(tahunAwal);
		tambahGrupFilter(barisFilter, "Tahun angkatan", tahunangkatan);

		Common.insertComboDanSemua(searchstatusmahasiswa, new String[] { "nama", "kodeEpsbed" },
				StatusMahasiswa.class);
		tambahGrupFilter(barisFilter, "Status mahasiswa", searchstatusmahasiswa);

		// Grup tombol Cari (sejajar dengan kolom filter, ikut turun saat wrap).
		Div grupCari = new Div();
		grupCari.setParent(barisFilter);
		grupCari.setStyle("display:flex;align-items:flex-end;flex:0 0 auto;");
		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setStyle("border:1px solid #1d4ed8;border-radius:8px;background:#1d4ed8;color:#ffffff;"
				+ "font-weight:700;padding:4px 14px;cursor:pointer;");
		cari.setTooltiptext("Cari mahasiswa sesuai filter di atas");
		// Satu listener pencarian dipakai ulang oleh tombol Cari dan tombol ENTER di kotak NIM/Nama.
		final EventListener pemicuCari = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		};
		cari.addEventListener("onClick", pemicuCari);
		cari.setParent(grupCari);
		nim.addEventListener("onOK", pemicuCari);
		nama.addEventListener("onOK", pemicuCari);
		dariNim.addEventListener("onOK", pemicuCari);
		sampaiNim.addEventListener("onOK", pemicuCari);

		// ================= BAGIAN TENGAH: grid mahasiswa =================
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setBorder("none");
		center.setAutoscroll(true);

		Borderlayout borderGrid = new ais.ui.util.MyBorderlayout();
		borderGrid.setParent(center);

		Center centerGrid = new Center();
		ais.ui.util.ZkCompat.setFlex(centerGrid, true);
		centerGrid.setBorder("none");
		centerGrid.setParent(borderGrid);

		South southGrid = new South();
		southGrid.setBorder("none");
		southGrid.setParent(borderGrid);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(centerGrid);

		Columns columns = new Columns();
		columns.setParent(grid);

		// Kolom-0: kotak centang "pilih semua" di header + centang per baris.
		MyColumnConfig kolomCheck = new MyColumnConfig();
		kolomCheck.setParent(columns);
		kolomCheck.setWidth("46px");
		final MyCheckboxConfig checkSemua = new MyCheckboxConfig();
		checkSemua.setTooltiptext("Pilih / lepas semua pada halaman ini");
		kolomCheck.appendChild(checkSemua);
		checkSemua.addEventListener(Events.ON_CHECK, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (grid.getRows() == null) {
					return;
				}
				List<?> rows = grid.getRows().getChildren();
				for (int i = 0; i < rows.size(); i++) {
					try {
						Row row = (Row) rows.get(i);
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						if (myCheckbox == null || myCheckbox.isDisabled()) {
							continue;
						}
						myCheckbox.setChecked(checkSemua.isChecked());
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e,
								"auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaForManajemenPenjadwalanMahasiswaHelper.java:checkSemua");
					}
				}
			}
		});

		MyColumnConfig kolomNim = new MyColumnConfig();
		kolomNim.setParent(columns);
		kolomNim.setLabel("NIM");
		kolomNim.setWidth("22%");

		MyColumnConfig kolomNama = new MyColumnConfig();
		kolomNama.setParent(columns);
		kolomNama.setLabel("Nama");

		MyColumnConfig kolomAngkatan = new MyColumnConfig();
		kolomAngkatan.setParent(columns);
		kolomAngkatan.setLabel("Tahun Angkatan");
		kolomAngkatan.setWidth("24%");

		paging.setParent(southGrid);

		onSearchDefault(null);

		// ================= BAGIAN BAWAH: tombol aksi utama =================
		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, false);
		south.setBorder("none");

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("padding:8px 12px;background:#ffffff;border-top:1px solid #e6edf5;"
				+ "display:flex;flex-wrap:wrap;gap:8px;");
		toolbar.setParent(south);

		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Tambahkan Terpilih", "/img/save.gif");
		simpan.setStyle("border:1px solid #16a34a;border-radius:8px;background:#16a34a;color:#ffffff;"
				+ "font-weight:700;padding:4px 14px;cursor:pointer;");
		simpan.setTooltiptext("Daftarkan mahasiswa yang dicentang ke kelas ini");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				int jumlah = save();
				muatUlangSetelahSimpan(dataLoader, window, jumlah, false);
			}
		});
		simpan.setParent(toolbar);

		MyToolbarbuttonConfig ambilSemua = new MyToolbarbuttonConfig("Tambahkan Semua (sesuai filter)",
				"/img/save.gif");
		ambilSemua.setStyle("border:1px solid #2563eb;border-radius:8px;background:#eff6ff;color:#1d4ed8;"
				+ "font-weight:700;padding:4px 14px;cursor:pointer;");
		ambilSemua.setTooltiptext("Daftarkan SEMUA mahasiswa yang cocok dengan filter sekarang");
		ambilSemua.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				int jumlah = saveSemua();
				muatUlangSetelahSimpan(dataLoader, window, jumlah, true);
			}
		});
		ambilSemua.setParent(toolbar);

		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		batal.setStyle("border:1px solid #cbd5e1;border-radius:8px;background:#ffffff;color:#334155;"
				+ "font-weight:600;padding:4px 14px;cursor:pointer;");
		batal.setTooltiptext("Tutup tanpa perubahan lain");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		batal.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menyusun HTML penjelasan singkat + rangkuman konteks kelas (chip). Ditulis dengan bahasa
	 * sederhana untuk pengguna non-teknis.
	 */
	private String bangunHtmlPenjelasan() {
		String fakultas = jurusan != null && jurusan.getFakultas() != null ? aman(jurusan.getFakultas().getNama())
				: "-";
		String prodi = jurusan != null ? aman(jurusan.getNama()) : "-";
		String namaKelas = kelas != null ? aman(kelas.getNama()) : "-";
		String semesterTeks = semester == null ? "-" : String.valueOf(semester);
		String ta = aman(tahunAjaran == null ? "-" : tahunAjaran);

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:12px;color:#334155;line-height:1.5;'>");
		sb.append("<div style='font-weight:800;color:#0f172a;font-size:13px;margin-bottom:2px;'>")
				.append("Masukkan mahasiswa ke kelas ini</div>");
		sb.append("<div>Centang mahasiswa yang ingin didaftarkan, lalu tekan <b>Tambahkan Terpilih</b>. "
				+ "Mahasiswa yang dicentang langsung terdaftar pada kelas ini tanpa perlu mendaftar sendiri. "
				+ "Mahasiswa yang sudah terdaftar tampil tercentang dan terkunci.</div>");
		sb.append("<div style='margin-top:7px;display:flex;flex-wrap:wrap;gap:6px;'>");
		sb.append(chip("Kelas", namaKelas));
		sb.append(chip("Prodi", prodi));
		sb.append(chip("Fakultas", fakultas));
		sb.append(chip("Tahun Ajaran", ta));
		sb.append(chip("Semester", semesterTeks));
		sb.append("</div>");
		sb.append("</div>");
		return sb.toString();
	}

	/** Membuat satu chip label:nilai untuk rangkuman konteks. */
	private static String chip(String label, String nilai) {
		return "<span style='display:inline-flex;align-items:center;gap:4px;padding:2px 9px;border-radius:999px;"
				+ "background:#e0edff;color:#1e3a8a;font-size:11px;font-weight:700;'>"
				+ "<span style='opacity:.7;font-weight:600;'>" + label + ":</span> " + nilai + "</span>";
	}

	/** Menyisipkan satu grup filter (label di atas, kotak isian di bawah) ke baris filter responsif. */
	private void tambahGrupFilter(Div baris, String labelTeks, org.zkoss.zk.ui.HtmlBasedComponent kotak) {
		Div grup = new Div();
		grup.setStyle(GAYA_GRUP_FILTER);
		grup.setParent(baris);

		Label label = new Label(Common.getBahasaConfig(labelTeks));
		label.setStyle(GAYA_LABEL_FILTER);
		label.setParent(grup);

		kotak.setWidth("100%");
		kotak.setStyle(GAYA_KOTAK_FILTER);
		kotak.setParent(grup);
	}

	/**
	 * Merapikan penyelesaian setelah penyimpanan: menyegarkan panel pemanggil, memuat ulang daftar
	 * agar penanda &ldquo;sudah terdaftar&rdquo; akurat, dan memberi konfirmasi jumlah yang berhasil.
	 */
	private void muatUlangSetelahSimpan(DataLoader dataLoader, MyWindow window, int jumlah, boolean tutup) {
		try {
			if (dataLoader != null) {
				dataLoader.loadData(null);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		try {
			// Segarkan penanda terdaftar pada grid picker (tanpa menutup) agar konsisten bila admin
			// ingin menambah lagi.
			onSearchDefault(null);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaForManajemenPenjadwalanMahasiswaHelper.java:muatUlang");
		}
		try {
			String pesan = jumlah > 0 ? (jumlah + " mahasiswa berhasil didaftarkan ke kelas ini.")
					: "Tidak ada mahasiswa baru yang didaftarkan (kemungkinan semua sudah terdaftar atau belum ada yang dicentang).";
			MyMessageboxConfig.show(pesan, "Informasi", MyMessageboxConfig.OK,
					jumlah > 0 ? MyMessageboxConfig.INFORMATION : MyMessageboxConfig.EXCLAMATION);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		if (tutup && window != null) {
			window.detach();
		}
	}

	/** Escape ringan untuk teks yang ditaruh ke dalam HTML. */
	private static String aman(String s) {
		if (s == null) {
			return "-";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/**
	 * Menyusun kriteria pencarian mahasiswa. Dipusatkan di sini agar pencarian, penomoran halaman,
	 * dan &ldquo;tambahkan semua&rdquo; memakai penyaringan yang identik.
	 *
	 * @param order true untuk menambahkan pengurutan (tahun angkatan terbaru dulu, lalu NIM).
	 */
	public Criteria initCriteria(boolean order) {
		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatusmahasiswa.getSelectedItem() == null ? null
				: searchstatusmahasiswa.getSelectedItem().getValue());

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (statusMahasiswa != null) {
			String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ statusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
					+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("kelas", kelas.getNama()));

		if (order) {
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));
		}

		criteria.add(criteriaStatus)
				.add(Restrictions.ilike("nama", teks(nama), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("nim", teks(nim), MatchMode.ANYWHERE))
				.add(tahunangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))
				.add(program == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("program", program))
				.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))
				.add(teks(dariNim).isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ge("nim", teks(dariNim)))
				.add(teks(sampaiNim).isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.le("nim", teks(sampaiNim)));

		return criteria;
	}

	/** Nilai teks kotak isian yang aman dari null, sudah di-trim. */
	private static String teks(Textbox t) {
		return t == null || t.getValue() == null ? "" : t.getValue().trim();
	}

	/**
	 * Menjalankan pencarian: menghitung total untuk penomoran halaman, memuat halaman berjalan,
	 * memuat sekali himpunan mahasiswa yang sudah terdaftar (anti N+1), lalu menyegarkan grid.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Mahasiswa> mahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		idMahasiswaTerdaftar = muatIdMahasiswaTerdaftar(mahasiswa);

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);
	}

	/**
	 * Memuat &mdash; dalam <b>satu</b> kueri &mdash; id mahasiswa (dari halaman {@code pageList})
	 * yang sudah terdaftar pada (kelas, tahun ajaran, semester) ini. Menggantikan kueri COUNT
	 * per-baris.
	 *
	 * @param pageList mahasiswa pada halaman yang sedang ditampilkan.
	 * @return himpunan id mahasiswa yang sudah terdaftar (kosong bila {@code pageList} kosong).
	 */
	@SuppressWarnings("unchecked")
	private Set<Long> muatIdMahasiswaTerdaftar(List<Mahasiswa> pageList) {
		Set<Long> hasil = new HashSet<Long>();
		if (pageList == null || pageList.isEmpty()) {
			return hasil;
		}
		List<Long> idHalaman = new ArrayList<Long>(pageList.size());
		for (int i = 0; i < pageList.size(); i++) {
			Mahasiswa m = pageList.get(i);
			if (m != null && m.getId() != null) {
				idHalaman.add(m.getId());
			}
		}
		if (idHalaman.isEmpty()) {
			return hasil;
		}
		try {
			Session session = HibernateUtil.currentSession();
			List<Mahasiswa> terdaftar = session.createCriteria(PenjadwalanMahasiswa.class)
					.add(Restrictions.eq("kelas", kelas)).add(Restrictions.eq("tahunAjaran", tahunAjaran))
					.add(Restrictions.eq("semester", semester)).createAlias("mahasiswa", "m")
					.add(Restrictions.in("m.id", idHalaman)).setProjection(Projections.property("m.id")).list();
			for (int i = 0; i < terdaftar.size(); i++) {
				Object id = terdaftar.get(i);
				if (id instanceof Number) {
					hasil.add(Long.valueOf(((Number) id).longValue()));
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaForManajemenPenjadwalanMahasiswaHelper.java:muatIdMahasiswaTerdaftar");
		}
		return hasil;
	}
}
