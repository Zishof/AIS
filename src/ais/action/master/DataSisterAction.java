package ais.action.master;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.DataSisterApi;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DataSister;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.ui.util.BaseDasbordPortal;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>Layar &amp; Dasbor "Data SISTER"</h2>
 *
 * <p>
 * Komposer ZK untuk halaman <b>Data SISTER</b> (dipetakan pada {@code data_sister.zul}). Halaman ini
 * menampung <i>salinan lokal</i> seluruh <b>data referensi</b> yang ditarik dari layanan web SISTER
 * (Sistem Informasi Sumber Daya Terintegrasi) milik Kemdikbudristek — misalnya daftar agama, bidang
 * studi, jabatan fungsional, wilayah, hingga data SDM/dosen. Salinan ini disimpan pada tabel
 * {@link DataSister} sehingga modul kepegawaian &amp; akademik dapat memakainya secara cepat tanpa harus
 * memanggil SISTER berulang kali.
 * </p>
 *
 * <h3>Apa saja yang bisa dilakukan di halaman ini</h3>
 * <ol>
 *   <li><b>Melihat &amp; mengelola data</b> (Tambah/Ubah/Hapus/Aktifkan) dalam bentuk tabel/grid ZK yang
 *       ringan, dengan pencarian berdasarkan nama tabel &amp; isi, serta ekspor ke Excel (tombol Cetak).</li>
 *   <li><b>Menyinkronkan data dari SISTER</b> lewat tombol "Sinkronkan Data dari SISTER" — memicu
 *       {@link DataSisterApi#synDataSister()} yang menarik puluhan endpoint {@code referensi/*} di latar
 *       belakang, lengkap dengan bilah kemajuan.</li>
 *   <li><b>Menguji akun SISTER</b> lewat tombol "Login ke SISTER" — menyimpan &amp; menguji kredensial
 *       (username, password, id pengguna, alamat server) yang tersimpan sebagai konfigurasi.</li>
 *   <li><b>Membaca ringkasan visual</b> di bagian atas halaman: kartu angka (KPI), grafik batang jenis
 *       referensi dengan data terbanyak, dan grafik donat perbandingan data aktif vs non-aktif — semuanya
 *       digambar memakai HTML/CSS modern (tanpa JFreeChart) sehingga otomatis rapi di layar ponsel maupun
 *       komputer.</li>
 * </ol>
 *
 * <h3>Desain UI/UX &amp; pemakaian ulang komponen</h3>
 * <p>
 * Agar konsisten dengan dasbor lain (mis. dasbor Feeder) dan mudah dirawat, halaman ini memakai ulang
 * pustaka bersama alih-alih menggambar sendiri: {@link HtmlChartHelper} untuk seluruh grafik
 * (kartu KPI, batang, donat) yang sudah responsif; {@link BaseDasbordPortal} untuk membungkus panel
 * dasbor beserta judul dan <i>penjelasan singkat berbahasa awam</i>; serta {@link MyHtml} untuk memasang
 * potongan HTML ke dalam komponen ZK. Setiap panel diberi kalimat penjelasan sederhana agar pengguna yang
 * sama sekali tidak paham teknologi tetap mengerti fungsinya.
 * </p>
 *
 * <h3>Aturan sesi Hibernate</h3>
 * <p>
 * Seluruh operasi baca/tulis pada halaman ini berjalan di atas <b>request ZK</b>, sehingga memakai
 * {@link HibernateUtil#currentSession()} yang <b>tidak boleh ditutup</b> secara manual (kerangka kerja
 * yang menutupnya di akhir request). Proses sinkronisasi yang berjalan di thread latar sudah ditangani
 * secara terpisah &amp; aman di {@link DataSisterApi} (satu {@code openSession()} yang ditutup di
 * {@code finally}). Karena itu tidak ada sesi yang perlu ditutup di kelas ini.
 * </p>
 *
 * <h3>Efisiensi &amp; ketahanan</h3>
 * <p>
 * Ringkasan dasbor dihitung dengan <b>query agregat</b> ({@code GROUP BY} + {@code COUNT}) — bukan memuat
 * seluruh baris ke memori — sehingga tetap ringan meski tabel referensi berisi puluhan ribu baris.
 * Pembuatan dasbor dibungkus {@code try/catch} sehingga bila terjadi kegagalan, layar CRUD utama tetap
 * berfungsi normal (dasbor sekadar tidak tampil). Kode dijaga kompatibel <b>Java 1.7</b> (tanpa
 * lambda/stream/diamond, {@code try/catch} gaya 1.6) agar lolos proses build Ant.
 * </p>
 *
 * @author e-Campus
 */
public class DataSisterAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = -5779730267402400328L;

	// ---- komponen dari zul (di-autowire) ----
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchket;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;
	private Textbox kode;

	private MyToolbarbuttonConfig add;

	// ---- status hak akses ----
	private boolean edit = false;
	private boolean delete = false;

	// ---- objek yang sedang diedit ----
	private DataSister dataSister;

	// ---- wadah dasbor ringkasan (dibuat dinamis di atas kartu filter) ----
	private Div dasborWadah;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi halaman: menyiapkan hak akses, memuat data ke grid, memasang tombol standar
	 * (Cetak/Upload), tombol khusus SISTER (Sinkronkan &amp; Login), lalu membangun dasbor ringkasan.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
			add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		// Tombol Cetak (ekspor Excel data yang tampil) & Upload (impor) — reuse helper standar.
		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(DataSister.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, DataSister.class, contents);
		if (upload != null) {
			upload.setVisible((add != null && add.isVisible()) && edit && delete);
		}
		Common.appendKeToolbar(upload, add, comp);

		// Tombol: Sinkronkan Data dari SISTER (menarik seluruh referensi di latar belakang).
		MyToolbarbuttonConfig btnSinkron = new MyToolbarbuttonConfig("Sinkronkan Data dari SISTER",
				"/img/Actions-view-media-equalizer-icon.png");
		if (add != null && add.getParent() != null) {
			add.getParent().appendChild(btnSinkron);
		}
		btnSinkron.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				DataSisterApi.synDataSister();
			}
		});

		// Tombol: Sinkronkan Data Dosen (SDM & Tridharma) — proses berat & lama, per dosen.
		MyToolbarbuttonConfig btnDosen = new MyToolbarbuttonConfig("Sinkronkan Data Dosen (SDM & Tridharma)",
				"/img/Actions-view-media-equalizer-icon.png");
		Common.appendKeToolbar(btnDosen, add, comp);
		btnDosen.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				bukaDialogPilihDosen();
			}
		});

		// Tombol: Login ke SISTER (simpan & uji kredensial).
		MyToolbarbuttonConfig btnLogin = new MyToolbarbuttonConfig("Login ke SISTER", "/img/svg/key.svg");
		Common.appendKeToolbar(btnLogin, add, comp);
		btnLogin.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				bukaDialogLogin();
			}
		});

		// Dasbor ringkasan (kartu KPI + grafik) di atas kartu filter — tidak boleh mengganggu CRUD.
		try {
			pasangDasbor(comp);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/DataSisterAction.java:224");
		}
	}

	// =====================================================================================
	// DASBOR RINGKASAN (KPI + grafik HTML/CSS) — reuse HtmlChartHelper + BaseDasbordPortal
	// =====================================================================================

	/**
	 * Menempatkan wadah dasbor sebagai anak PERTAMA dari portal CRUD (di atas kartu filter) lalu
	 * mengisinya. Dipisah agar mudah dibangun ulang.
	 */
	private void pasangDasbor(Component comp) {
		Component portal = comp == null ? null : comp.getFirstChild(); // div.ais-crud-portal
		if (portal == null) {
			return;
		}
		dasborWadah = new Div();
		dasborWadah.setWidth("100%");
		dasborWadah.setStyle("margin-bottom:10px;");
		portal.insertBefore(dasborWadah, portal.getFirstChild());
		bangunDasbor();
	}

	/**
	 * Menghitung ringkasan dari basis data lokal dan menggambar: kartu angka (KPI), grafik batang jenis
	 * referensi terbanyak, dan grafik donat aktif vs non-aktif. Memakai {@code currentSession()} milik
	 * request (tidak ditutup). Bila belum ada data, menampilkan ajakan untuk sinkronisasi.
	 */
	private void bangunDasbor() {
		if (dasborWadah == null) {
			return;
		}
		Common.clear(dasborWadah);

		Map<String, Long> perTabel = DataSisterApi.ringkasanPerTabel();
		long total = 0;
		for (Long v : perTabel.values()) {
			total += (v == null ? 0 : v.longValue());
		}
		int jenis = perTabel.size();
		long aktif = hitungAktif(true);
		long nonAktif = hitungAktif(false);

		Component host = BaseDasbordPortal.panelTunggal(dasborWadah, "Ringkasan Data SISTER",
				"Gambaran singkat berapa banyak data dari SISTER yang sudah tersimpan di sistem.");

		if (total == 0) {
			new MyHtml("<div style='padding:18px;text-align:center;color:#65676b;font-size:14px;'>"
					+ "Belum ada data SISTER yang tersimpan. Silakan klik tombol "
					+ "<b>Sinkronkan Data dari SISTER</b> di atas untuk menariknya dari server SISTER.</div>")
					.setParent(host);
			return;
		}

		// --- Kartu angka (KPI) ---
		String kpi = HtmlChartHelper.kpiCards(
				new String[] { "Total Data", "Jenis Referensi", "Data Aktif", "Data Non-aktif" },
				new String[] { fmt(total), fmt(jenis), fmt(aktif), fmt(nonAktif) },
				new String[] { "baris tersimpan dari SISTER", "macam daftar referensi", "sedang dipakai",
						"disembunyikan" },
				(String[]) null, (boolean[]) null, new String[] { "#1877f2", "#00a884", "#f7b928", "#8a8d91" });
		new MyHtml(kpi).setParent(host);

		// --- Grafik batang: 12 jenis referensi dengan data terbanyak ---
		List<Map.Entry<String, Long>> entries = new ArrayList<Map.Entry<String, Long>>(perTabel.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, Long>>() {
			@Override
			public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
				long va = a.getValue() == null ? 0 : a.getValue().longValue();
				long vb = b.getValue() == null ? 0 : b.getValue().longValue();
				return (vb < va) ? -1 : (vb > va ? 1 : 0);
			}
		});
		int topN = Math.min(12, entries.size());
		String[] labels = new String[topN];
		double[] values = new double[topN];
		for (int i = 0; i < topN; i++) {
			labels[i] = pendekTabel(entries.get(i).getKey());
			values[i] = entries.get(i).getValue() == null ? 0 : entries.get(i).getValue().doubleValue();
		}
		String bar = HtmlChartHelper.barHorizontal("Data Terbanyak per Jenis Referensi",
				"Jenis data SISTER yang paling banyak tersimpan di sistem (12 teratas).", labels, values, "#1877f2");
		new MyHtml(bar).setParent(host);

		// --- Grafik donat: aktif vs non-aktif ---
		String donut = HtmlChartHelper.donut("Data Aktif vs Non-aktif",
				"Perbandingan data yang sedang dipakai dengan yang disembunyikan.",
				new String[] { "Aktif", "Non-aktif" }, new double[] { aktif, nonAktif },
				new String[] { "#00a884", "#8a8d91" }, "data");
		new MyHtml(donut).setParent(host);

		// --- Grafik radar/spider: komposisi data per kelompok besar referensi ---
		try {
			bangunRadar(host, perTabel);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/DataSisterAction.java:320");
		}

		// --- Grafik tren: aktivitas pembaruan data 30 hari terakhir ---
		try {
			bangunTren(host);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/DataSisterAction.java:327");
		}
	}

	/**
	 * Menggambar grafik radar (jaring laba-laba) komposisi data SISTER pada lima kelompok besar
	 * (SDM &amp; Mahasiswa, Wilayah &amp; Institusi, Kepegawaian, Kegiatan &amp; Tridharma, Referensi
	 * Akademik). Membantu melihat kelompok mana yang datanya banyak/sedikit dalam satu pandang.
	 */
	private void bangunRadar(Component host, Map<String, Long> perTabel) {
		String[] kategori = { "SDM & Mahasiswa", "Wilayah & Institusi", "Kepegawaian", "Kegiatan & Tridharma",
				"Referensi Akademik" };
		double[] nilai = new double[kategori.length];
		for (Map.Entry<String, Long> e : perTabel.entrySet()) {
			String kat = kategoriTabel(e.getKey());
			long v = e.getValue() == null ? 0 : e.getValue().longValue();
			for (int i = 0; i < kategori.length; i++) {
				if (kategori[i].equals(kat)) {
					nilai[i] += v;
					break;
				}
			}
		}
		double max = 1;
		for (int i = 0; i < nilai.length; i++) {
			if (nilai[i] > max) {
				max = nilai[i];
			}
		}
		String radar = HtmlChartHelper.radar("Komposisi Data per Kelompok",
				"Sebaran banyaknya data pada tiap kelompok besar; makin jauh dari pusat berarti makin banyak.",
				kategori, new String[] { "Jumlah data" }, new double[][] { nilai }, new String[] { "#1877f2" }, max);
		new MyHtml(radar).setParent(host);
	}

	/**
	 * Menggambar grafik garis (tren) berapa banyak data SISTER yang tersimpan/diperbarui tiap hari selama
	 * 30 hari terakhir — memperlihatkan kapan sinkronisasi terakhir dilakukan. Query dibatasi 30 hari agar
	 * tetap ringan. Bila tidak ada aktivitas, grafik dilewati.
	 */
	@SuppressWarnings("unchecked")
	private void bangunTren(Component host) {
		List<Object[]> rows = HibernateUtil.currentSession()
				.createSQLQuery("select to_char(tanggal_dirubah,'YYYY-MM-DD') as d, count(*) as c "
						+ "from public.data_sister where tanggal_dirubah >= now() - interval '30 day' "
						+ "group by 1 order by 1")
				.list();
		if (rows == null || rows.isEmpty()) {
			return;
		}
		int n = rows.size();
		String[] kategori = new String[n];
		double[] nilai = new double[n];
		for (int i = 0; i < n; i++) {
			Object[] r = rows.get(i);
			kategori[i] = r[0] == null ? "" : r[0].toString();
			nilai[i] = r[1] == null ? 0 : ((Number) r[1]).doubleValue();
		}
		String tren = HtmlChartHelper.lineMulti("Aktivitas Pembaruan Data (30 Hari Terakhir)",
				"Berapa banyak data SISTER yang tersimpan atau diperbarui pada tiap hari.", kategori,
				new String[] { "Data diperbarui" }, new double[][] { nilai }, new String[] { "#00a884" });
		new MyHtml(tren).setParent(host);
	}

	/** Memetakan nama endpoint/tabel ke salah satu dari lima kelompok besar untuk grafik radar. */
	private static String kategoriTabel(String nama) {
		String s = pendekTabel(nama);
		if (s == null) {
			return "Referensi Akademik";
		}
		if (s.contains("sdm") || s.contains("mahasiswa")) {
			return "SDM & Mahasiswa";
		}
		if (s.contains("wilayah") || s.equals("negara") || s.contains("perguruan") || s.contains("unit_kerja")
				|| s.contains("profil_pt") || s.contains("dudi") || s.contains("bidang_usaha")) {
			return "Wilayah & Institusi";
		}
		if (s.contains("jabatan") || s.contains("golongan") || s.contains("pangkat") || s.contains("kepegawaian")
				|| s.contains("ikatan_kerja") || s.contains("gaji") || s.contains("tunjangan")
				|| s.contains("pekerjaan") || s.contains("status_kepegawaian")) {
			return "Kepegawaian";
		}
		if (s.contains("kegiatan") || s.contains("publikasi") || s.contains("penghargaan") || s.contains("skim")
				|| s.contains("kepanitiaan") || s.contains("diklat") || s.contains("beasiswa")
				|| s.contains("kesejahteraan") || s.contains("tes")) {
			return "Kegiatan & Tridharma";
		}
		return "Referensi Akademik";
	}

	/** Menghitung jumlah baris {@link DataSister} berdasarkan status aktif (currentSession, tidak ditutup). */
	private long hitungAktif(boolean aktif) {
		try {
			Criteria c = HibernateUtil.currentSession().createCriteria(DataSister.class);
			if (aktif) {
				c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			} else {
				c.add(Restrictions.eq("aktif", false));
			}
			Object o = c.setProjection(Projections.rowCount()).uniqueResult();
			return o == null ? 0 : ((Number) o).longValue();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/DataSisterAction.java:429");
			return 0;
		}
	}

	/** Memperpendek nama endpoint agar enak dibaca pada grafik (buang prefix "referensi/" &amp; query). */
	private static String pendekTabel(String nama) {
		if (nama == null) {
			return "";
		}
		String s = nama;
		int tanya = s.indexOf('?');
		if (tanya >= 0) {
			s = s.substring(0, tanya);
		}
		int garis = s.lastIndexOf('/');
		if (garis >= 0 && garis < s.length() - 1) {
			s = s.substring(garis + 1);
		}
		return s;
	}

	/** Memformat angka dengan pemisah ribuan agar mudah dibaca. */
	private static String fmt(long n) {
		return new DecimalFormat("#,##0").format(n);
	}

	// =====================================================================================
	// DIALOG LOGIN / UJI KREDENSIAL SISTER
	// =====================================================================================

	/**
	 * Membuka dialog untuk mengisi &amp; menguji kredensial SISTER (username, password, id pengguna,
	 * alamat server). Nilai disimpan sebagai konfigurasi, lalu login diuji lewat
	 * {@link DataSisterApi#doLogin(String, String, String, String)}.
	 */
	private void bukaDialogLogin() throws Exception {
		final MyWindow window = new MyWindow("Masukkan Akun SISTER", "none", true);
		window.setParent(page.getFirstRoot());
		window.setHeight("95%");
		window.setWidth("600px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);

		MyGrid gridDialog = new MyGrid();
		gridDialog.setWidth("100%");
		gridDialog.setParent(center);
		gridDialog.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(gridDialog);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(gridDialog);

		String username = Common.getKonfigurasi("sister_username", "knNcb8iOFtKOxY1N8mUfVY5mqArRyecX+RH+pLOndCE=")
				.getNilai();
		String password = Common
				.getKonfigurasi("sister_password", "MycV1kHjaHWJ97zYzg4YiReNBpIj40ZVnxrFXWkmi0zooQDExe6sJ6HLHVoX8BJN")
				.getNilai();
		String idPengguna = Common.getKonfigurasi("sister_id_pengguna", "acecd7e5-330a-48e8-98d0-12cd46500408")
				.getNilai();
		String strURL = Common.getKonfigurasi("sister_host_url", "https://sister-api.kemdikbud.go.id/ws.php/1.0")
				.getNilai();

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Username"));
		final Textbox usernameBox = new Textbox(username);
		row.appendChild(usernameBox);
		usernameBox.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Password"));
		final Textbox passwordBox = new Textbox(password);
		row.appendChild(passwordBox);
		passwordBox.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("ID Pengguna"));
		final Textbox idPenggunaBox = new Textbox(idPengguna);
		row.appendChild(idPenggunaBox);
		idPenggunaBox.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Host URL"));
		final Textbox hostUrlBox = new Textbox(strURL);
		row.appendChild(hostUrlBox);
		hostUrlBox.setWidth("90%");
		hostUrlBox.setRows(2);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Coba Login", "/img/save.gif");
		save.setTooltiptext("Simpan & uji kredensial");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();

				Konfigurasi kUser = Common.getKonfigurasi("sister_username",
						"knNcb8iOFtKOxY1N8mUfVY5mqArRyecX+RH+pLOndCE=");
				kUser.setNilai(usernameBox.getValue());
				Common.refreshUpdate(kUser);

				Konfigurasi kPass = Common.getKonfigurasi("sister_password",
						"MycV1kHjaHWJ97zYzg4YiReNBpIj40ZVnxrFXWkmi0zooQDExe6sJ6HLHVoX8BJN");
				kPass.setNilai(passwordBox.getValue());
				Common.refreshUpdate(kPass);

				Konfigurasi kId = Common.getKonfigurasi("sister_id_pengguna", "acecd7e5-330a-48e8-98d0-12cd46500408");
				kId.setNilai(idPenggunaBox.getValue());
				Common.refreshUpdate(kId);

				Konfigurasi kHost = Common.getKonfigurasi("sister_host_url",
						"https://sister-api.kemdikbud.go.id/ws.php/1.0");
				kHost.setNilai(hostUrlBox.getValue());
				Common.refreshUpdate(kHost);

				String hasil = DataSisterApi.doLogin(kUser.getNilai(), kPass.getNilai(), kId.getNilai(),
						kHost.getNilai() + "/authorize");

				if (DataSisterApi.token == null || DataSisterApi.token.isEmpty()) {
					MyMessageboxConfig.show("Login data SISTER GAGAL. Info rinci:\n\n" + hasil, "Pemberitahuan",
							MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				} else {
					MyMessageboxConfig.show("Login data SISTER BERHASIL. Info rinci:\n\n" + hasil, "Pemberitahuan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
			}
		});
		save.setParent(toolbar);

		window.setVisible(true);
		window.onModal();
	}

	// =====================================================================================
	// DIALOG PEMILIHAN DOSEN (agar sinkron dosen bisa BERTAHAP / per-batch)
	// =====================================================================================

	/**
	 * Membuka dialog daftar dosen (dari referensi SDM tersinkron) dengan centang + penyaring nama, tombol
	 * "Pilih Semua (tampil)"/"Kosongkan", lalu "Sinkronkan Terpilih". Memungkinkan admin menyinkronkan
	 * hanya sebagian dosen dalam satu kali jalan — sangat membantu karena sinkron dosen berat &amp; lama.
	 */
	private void bukaDialogPilihDosen() throws Exception {
		// dosens[i] = [id_sdm, nama, nidn, jenis_sdm, prodi, fakultas]
		final List<String[]> dosens = DataSisterApi.ambilDaftarDosen();
		if (dosens == null || dosens.isEmpty()) {
			MyMessageboxConfig.show(
					"Belum ada data SDM/dosen. Jalankan \"Sinkronkan Data dari SISTER\" lebih dulu (agar referensi SDM terisi), lalu ulangi.",
					"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		final java.util.Set<String> terpilih = new java.util.HashSet<String>();

		// Kumpulkan pilihan Fakultas, peta Fakultas->Prodi, dan Jenis SDM untuk combobox filter.
		final java.util.TreeSet<String> setFak = new java.util.TreeSet<String>();
		final java.util.Map<String, java.util.TreeSet<String>> mapFakProdi = new java.util.HashMap<String, java.util.TreeSet<String>>();
		final java.util.TreeSet<String> setJenis = new java.util.TreeSet<String>();
		for (int i = 0; i < dosens.size(); i++) {
			String[] d = dosens.get(i);
			if (d[3] != null && !d[3].isEmpty()) {
				setJenis.add(d[3]);
			}
			if (d[5] != null && !d[5].isEmpty()) {
				setFak.add(d[5]);
				java.util.TreeSet<String> ps = mapFakProdi.get(d[5]);
				if (ps == null) {
					ps = new java.util.TreeSet<String>();
					mapFakProdi.put(d[5], ps);
				}
				if (d[4] != null && !d[4].isEmpty()) {
					ps.add(d[4]);
				}
			}
		}

		final MyWindow window = new MyWindow("Pilih Dosen untuk Disinkronkan", "normal", true);
		window.setParent(page.getFirstRoot());
		window.setWidth("760px");
		window.setHeight("90%");
		window.setClosable(true);

		Borderlayout bl = new ais.ui.util.MyBorderlayout();
		bl.setParent(window);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(bl);
		org.zkoss.zul.Vbox atas = new org.zkoss.zul.Vbox();
		atas.setWidth("100%");
		atas.setStyle("padding:8px;");
		atas.setParent(north);
		atas.appendChild(new ais.ui.util.MyLabelConfig(
				"Centang dosen yang ingin disinkronkan (boleh sebagian). Saring per Fakultas/Prodi/Jenis atau ketik nama."));

		org.zkoss.zul.Hbox barisFilter = new org.zkoss.zul.Hbox();
		barisFilter.setParent(atas);
		barisFilter.appendChild(new ais.ui.util.MyLabelConfig("Fakultas:"));
		final org.zkoss.zul.Combobox comboFak = new org.zkoss.zul.Combobox();
		comboFak.setReadonly(true);
		comboFak.setWidth("170px");
		isiCombo(comboFak, "= Semua Fakultas =", setFak);
		barisFilter.appendChild(comboFak);

		barisFilter.appendChild(new ais.ui.util.MyLabelConfig("Prodi:"));
		final org.zkoss.zul.Combobox comboProdi = new org.zkoss.zul.Combobox();
		comboProdi.setReadonly(true);
		comboProdi.setWidth("180px");
		final java.util.TreeSet<String> semuaProdi = new java.util.TreeSet<String>();
		for (int i = 0; i < dosens.size(); i++) {
			if (dosens.get(i)[4] != null && !dosens.get(i)[4].isEmpty()) {
				semuaProdi.add(dosens.get(i)[4]);
			}
		}
		isiCombo(comboProdi, "= Semua Prodi =", semuaProdi);
		barisFilter.appendChild(comboProdi);

		barisFilter.appendChild(new ais.ui.util.MyLabelConfig("Jenis:"));
		final org.zkoss.zul.Combobox comboJenis = new org.zkoss.zul.Combobox();
		comboJenis.setReadonly(true);
		comboJenis.setWidth("130px");
		isiCombo(comboJenis, "= Semua =", setJenis);
		barisFilter.appendChild(comboJenis);

		org.zkoss.zul.Hbox barisNama = new org.zkoss.zul.Hbox();
		barisNama.setParent(atas);
		barisNama.appendChild(new ais.ui.util.MyLabelConfig("Cari nama:"));
		final Textbox cari = new Textbox();
		cari.setWidth("240px");
		barisNama.appendChild(cari);
		final Label infoLbl = new Label(ais.common.Common.getBahasaConfig("Terpilih: 0"));
		barisNama.appendChild(infoLbl);

		Center center = new Center();
		center.setParent(bl);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Div scroll = new Div();
		scroll.setWidth("100%");
		scroll.setStyle("overflow:auto;height:100%;");
		scroll.setParent(center);
		MyGrid g = new MyGrid();
		g.setWidth("100%");
		g.setParent(scroll);
		Columns cols = new Columns();
		cols.setParent(g);
		tambahKolom(cols, "", "42px");
		tambahKolom(cols, "Nama Dosen", null);
		tambahKolom(cols, "Prodi", "24%");
		tambahKolom(cols, "Fakultas", "22%");
		tambahKolom(cols, "Jenis", "14%");
		final Rows rows = new Rows();
		rows.setParent(g);

		final Rows rowsF = rows;
		renderDosenRows(rowsF, dosens, "", "", "", "", terpilih, infoLbl);

		// Satu listener untuk semua penyaring: baca nilai combo + kotak nama lalu gambar ulang.
		final EventListener saring = new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				String fNama = cari.getValue() == null ? "" : cari.getValue().trim().toLowerCase();
				String fFak = nilaiCombo(comboFak);
				String fProdi = nilaiCombo(comboProdi);
				String fJenis = nilaiCombo(comboJenis);
				renderDosenRows(rowsF, dosens, fNama, fFak, fProdi, fJenis, terpilih, infoLbl);
			}
		};
		cari.addEventListener("onChanging", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				String v = ((org.zkoss.zk.ui.event.InputEvent) e).getValue();
				String fNama = v == null ? "" : v.trim().toLowerCase();
				renderDosenRows(rowsF, dosens, fNama, nilaiCombo(comboFak), nilaiCombo(comboProdi),
						nilaiCombo(comboJenis), terpilih, infoLbl);
			}
		});
		comboProdi.addEventListener("onChange", saring);
		comboJenis.addEventListener("onChange", saring);
		// Saat Fakultas berubah: susun ulang daftar Prodi (hanya prodi di fakultas itu) lalu gambar ulang.
		comboFak.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				String fFak = nilaiCombo(comboFak);
				java.util.TreeSet<String> ps;
				if (fFak.isEmpty()) {
					ps = semuaProdi;
				} else {
					ps = mapFakProdi.get(fFak);
					if (ps == null) {
						ps = new java.util.TreeSet<String>();
					}
				}
				isiCombo(comboProdi, "= Semua Prodi =", ps);
				saring.onEvent(e);
			}
		});

		South south = new South();
		south.setParent(bl);
		ais.ui.util.ZkCompat.setFlex(south, true);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig pilihSemua = new MyToolbarbuttonConfig("Pilih Semua (sesuai saringan)",
				"/img/svg/check2-circle.svg");
		pilihSemua.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				String fNama = cari.getValue() == null ? "" : cari.getValue().trim().toLowerCase();
				String fFak = nilaiCombo(comboFak);
				String fProdi = nilaiCombo(comboProdi);
				String fJenis = nilaiCombo(comboJenis);
				for (int i = 0; i < dosens.size(); i++) {
					if (cocokFilter(dosens.get(i), fNama, fFak, fProdi, fJenis)) {
						terpilih.add(dosens.get(i)[0]);
					}
				}
				renderDosenRows(rowsF, dosens, fNama, fFak, fProdi, fJenis, terpilih, infoLbl);
			}
		});
		pilihSemua.setParent(toolbar);

		MyToolbarbuttonConfig kosong = new MyToolbarbuttonConfig("Kosongkan", "/img/cancel.gif");
		kosong.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				terpilih.clear();
				saring.onEvent(e);
			}
		});
		kosong.setParent(toolbar);

		MyToolbarbuttonConfig sinkron = new MyToolbarbuttonConfig("Sinkronkan Terpilih", "/img/save.gif");
		sinkron.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (terpilih.isEmpty()) {
					MyMessageboxConfig.show("Belum ada dosen yang dicentang.", "Pemberitahuan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				final List<String> pilihan = new ArrayList<String>(terpilih);
				window.detach();
				MyMessageboxConfig.show(
						"Sinkronkan " + pilihan.size()
								+ " dosen sekarang? Proses berjalan di latar belakang dan bisa lama.",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event ev) throws Exception {
								if (Integer.parseInt(ev.getData().toString()) == MyMessageboxConfig.OK) {
									DataSisterApi.synDataDosen(pilihan);
								}
							}
						});
			}
		});
		sinkron.setParent(toolbar);

		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				window.detach();
			}
		});
		batal.setParent(toolbar);

		window.setVisible(true);
		window.onModal();
	}

	/** Mengisi ulang combobox dengan item "semua" (nilai null) + daftar nilai; memilih item pertama. */
	private static void isiCombo(org.zkoss.zul.Combobox combo, String labelSemua, java.util.Collection<String> nilai) {
		combo.getItems().clear();
		ais.ui.util.MyComboitemConfig semua = new ais.ui.util.MyComboitemConfig(labelSemua);
		semua.setValue(null);
		combo.appendChild(semua);
		if (nilai != null) {
			for (java.util.Iterator<String> it = nilai.iterator(); it.hasNext();) {
				String v = it.next();
				ais.ui.util.MyComboitemConfig ci = new ais.ui.util.MyComboitemConfig(v);
				ci.setValue(v);
				combo.appendChild(ci);
			}
		}
		combo.setSelectedItem(semua);
	}

	/** Membaca nilai combobox filter; string kosong berarti "semua". */
	private static String nilaiCombo(org.zkoss.zul.Combobox combo) {
		if (combo == null || combo.getSelectedItem() == null || combo.getSelectedItem().getValue() == null) {
			return "";
		}
		return combo.getSelectedItem().getValue().toString();
	}

	/** Menambah satu kolom grid dengan label &amp; lebar opsional. */
	private static void tambahKolom(Columns cols, String label, String width) {
		MyColumnConfig c = new MyColumnConfig();
		if (label != null) {
			c.setLabel(label);
		}
		if (width != null) {
			c.setWidth(width);
		}
		c.setParent(cols);
	}

	/**
	 * Uji apakah satu baris dosen {@code [id_sdm, nama, nidn, jenis, prodi, fakultas]} lolos seluruh
	 * penyaring aktif (nama-mengandung, fakultas/prodi/jenis sama-persis). Filter kosong = tidak menyaring.
	 */
	private static boolean cocokFilter(String[] d, String fNama, String fFak, String fProdi, String fJenis) {
		if (fNama != null && !fNama.isEmpty() && (d[1] == null || !d[1].toLowerCase().contains(fNama))) {
			return false;
		}
		if (fFak != null && !fFak.isEmpty() && !fFak.equalsIgnoreCase(d[5] == null ? "" : d[5])) {
			return false;
		}
		if (fProdi != null && !fProdi.isEmpty() && !fProdi.equalsIgnoreCase(d[4] == null ? "" : d[4])) {
			return false;
		}
		if (fJenis != null && !fJenis.isEmpty() && !fJenis.equalsIgnoreCase(d[3] == null ? "" : d[3])) {
			return false;
		}
		return true;
	}

	/**
	 * Menggambar ulang baris daftar dosen sesuai seluruh penyaring aktif; mempertahankan status centang
	 * lewat {@code terpilih}. Render dibatasi {@code MAKS} baris agar ringan (persempit dengan saringan;
	 * "Pilih Semua" tetap mencakup seluruh yang cocok, bukan hanya yang tampil).
	 */
	private void renderDosenRows(final Rows rows, List<String[]> dosens, String fNama, String fFak, String fProdi,
			String fJenis, final java.util.Set<String> terpilih, final Label infoLbl) {
		Common.clear(rows);
		final int MAKS = 500;
		int tampil = 0;
		int cocok = 0;
		for (int i = 0; i < dosens.size(); i++) {
			final String[] d = dosens.get(i);
			if (!cocokFilter(d, fNama, fFak, fProdi, fJenis)) {
				continue;
			}
			cocok++;
			if (tampil >= MAKS) {
				continue;
			}
			tampil++;
			Row row = new Row();
			row.setParent(rows);
			final org.zkoss.zul.Checkbox cb = new org.zkoss.zul.Checkbox();
			cb.setChecked(terpilih.contains(d[0]));
			cb.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					if (cb.isChecked()) {
						terpilih.add(d[0]);
					} else {
						terpilih.remove(d[0]);
					}
					infoLbl.setValue("Terpilih: " + terpilih.size());
				}
			});
			row.appendChild(cb);
			row.appendChild(new Label(d[1]));
			row.appendChild(new Label(d[4] == null ? "" : d[4]));
			row.appendChild(new Label(d[5] == null ? "" : d[5]));
			row.appendChild(new Label(d[3] == null ? "" : d[3]));
		}
		infoLbl.setValue("Terpilih: " + terpilih.size() + " | Cocok: " + cocok
				+ (cocok > MAKS ? " (tampil " + MAKS + ", persempit saringan)" : ""));
	}

	// =====================================================================================
	// GRID / RENDERER
	// =====================================================================================

	/** Penggambar satu baris grid Data SISTER (ID, tabel+revisi, isi, checkbox aktif, tombol salin/ubah/hapus). */
	class DataSisterRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final DataSister item = (DataSister) arg1;

			new Label(item.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(DataSister.class, item, item.getNama()).setParent(arg0);
			new Label(item.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(item.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					item.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(item);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, item, DataSisterAction.this).setParent(arg0);
		}
	}

	// =====================================================================================
	// TAMBAH / UBAH
	// =====================================================================================

	public void onAdd(Event event) throws Exception {
		init(new DataSister());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		dataSister = (DataSister) obj;
		init(dataSister);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/** Membangun form Tambah/Ubah satu Data SISTER (ID Sister, Tabel Sister, Isi). */
	private void init(DataSister dataSister) {
		this.dataSister = dataSister;
		addWindow.setTitle(dataSister.getId() == null ? "Tambah Data Sister" : "Ubah Data Sister");
		Common.clear(addWindow);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid gridForm = new MyGrid();
		gridForm.setWidth("100%");
		gridForm.setParent(center);
		gridForm.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(gridForm);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(gridForm);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("ID Sister"));
		row.appendChild(kode = new Textbox(dataSister.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tabel Sister"));
		row.appendChild(nama = new Textbox(dataSister.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Isi"));
		row.appendChild(keterangan = new Textbox(dataSister.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(5);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	/**
	 * Menyimpan Data SISTER dari form. Memakai {@code currentSession()} (request ZK) yang tidak ditutup
	 * di sini; bila objek sudah ada di basis data, dimuat ulang lebih dulu agar pembaruan konsisten.
	 */
	public boolean onSave(Event event) throws Exception {
		Session session = HibernateUtil.currentSession();
		if (dataSister.getId() != null) {
			dataSister = (DataSister) session.load(DataSister.class, dataSister.getId());
		}
		dataSister.setKode(kode.getValue());
		dataSister.setNama(nama.getValue());
		dataSister.setKeterangan(keterangan.getValue());
		Common.refreshSaveOrUpdate(session, dataSister);
		return true;
	}

	// =====================================================================================
	// PENCARIAN / PEMUATAN GRID
	// =====================================================================================

	/**
	 * Membangun kriteria pencarian dari kotak filter (status aktif, nama tabel, isi). Memakai
	 * {@code currentSession()} (tidak ditutup).
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DataSister.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}

		criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));

		criteria.add(searchket == null || searchket.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("keterangan", searchket.getValue().trim(), MatchMode.ANYWHERE));

		return criteria;
	}

	/** Memuat data ke grid sesuai filter &amp; halaman aktif. */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DataSister> data = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(data);
		grid.setRowRenderer(new DataSisterRenderer());
		grid.setModelCheckMobile(strset);
	}
}
