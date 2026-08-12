package ais.action.master.pmb.statistik;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Jenjang;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;

/**
 * <h1>DashboardStatistikPmb — Pusat Ringkasan Penerimaan Mahasiswa Baru</h1>
 *
 * <p>
 * Kelas ini membangun seluruh tampilan <b>Dasbor Statistik PMB</b> (Penerimaan Mahasiswa Baru)
 * yang ditampilkan sebagai tab pertama di halaman Statistik PMB. Tujuan utamanya adalah memberikan
 * <em>satu pandangan lengkap</em> kepada pimpinan dan staf PMB — tanpa harus berpindah-pindah
 * tab — sehingga keputusan dapat diambil lebih cepat dan lebih tepat berdasarkan data nyata.
 * </p>
 *
 * <h2>Bagian-bagian yang Ditampilkan</h2>
 * <ol>
 *   <li><b>Kartu Ringkasan (KPI).</b> Lima angka paling penting: Total Peminat, Peserta Ujian,
 *       Lulus/Diterima, Dapat NIM, dan Tingkat Keberhasilan. Setiap kartu juga menampilkan
 *       perbandingan dengan tahun sebelumnya sehingga pengguna langsung tahu apakah angka
 *       naik atau turun tanpa harus membandingkan sendiri.</li>
 *   <li><b>Corong Penerimaan.</b> Memperlihatkan "perjalanan" calon mahasiswa mulai dari
 *       mendaftar sampai resmi ber-NIM. Semakin panjang batang, semakin banyak yang lolos
 *       ke tahap tersebut. Batang yang sangat pendek di suatu tahap menandakan banyak calon
 *       yang tidak melanjutkan ke tahap berikutnya.</li>
 *   <li><b>Komposisi Gender.</b> Grafik lingkaran yang menampilkan perbandingan jumlah calon
 *       laki-laki dan perempuan. Berguna untuk perencanaan asrama, fasilitas, dan laporan
 *       kesetaraan gender kepada kementerian.</li>
 *   <li><b>Pendaftar Per Bulan (Dua Tahun).</b> Dua batang berdampingan untuk setiap bulan
 *       membandingkan jumlah pendaftar tahun ini dan tahun lalu. Berguna untuk mengetahui
 *       bulan promosi paling efektif dan merencanakan kapan iklan perlu ditingkatkan.</li>
 *   <li><b>Rasio Keketatan per Program Studi.</b> Menampilkan berapa orang yang bersaing
 *       untuk masuk ke setiap program studi: jumlah yang mendaftar (memilih prodi sebagai
 *       pilihan pertama), berapa yang diterima, dan berapa yang akhirnya ber-NIM. Prodi
 *       dengan rasio peminat/diterima tinggi berarti sangat ketat persaingannya.</li>
 *   <li><b>5 Prodi Paling Diminati.</b> Batang mendatar yang menampilkan lima prodi yang
 *       paling banyak dipilih sebagai pilihan pertama. Membantu perencanaan kapasitas kelas.</li>
 *   <li><b>Radar Perbandingan Tahun.</b> Grafik jaring laba-laba yang membandingkan lima
 *       dimensi penerimaan antara tahun ini dan tahun lalu sekaligus. Area yang lebih luas
 *       berarti hasil yang lebih baik secara keseluruhan.</li>
 *   <li><b>Tren 5 Tahun.</b> Grafik garis yang memperlihatkan perkembangan peminat, yang
 *       diterima, dan yang dapat NIM selama 5 tahun terakhir. Berguna untuk melihat arah
 *       pertumbuhan jangka panjang dan menyusun target tahun depan.</li>
 *   <li><b>Distribusi Per Gelombang.</b> Jumlah pendaftar di setiap gelombang penerimaan.
 *       Berguna untuk evaluasi apakah jadwal dan kuota setiap gelombang sudah optimal.</li>
 *   <li><b>8 Daerah Asal Terbanyak.</b> Delapan daerah/propinsi yang paling banyak mengirim
 *       calon mahasiswa. Berguna untuk merencanakan kegiatan promosi ke daerah yang belum
 *       banyak terwakili.</li>
 *   <li><b>Tabel Ringkasan per Prodi.</b> Data terstruktur yang dapat diunduh ke Excel,
 *       menampilkan statistik lengkap per program studi: peminat, diterima, dapat NIM,
 *       dan tingkat keberhasilan dalam bentuk tabel yang bisa ditelusuri baris per baris.</li>
 * </ol>
 *
 * <h2>Arsitektur Teknis</h2>
 * <p>
 * Dasbor memiliki dua lapis komponen:
 * </p>
 * <ul>
 *   <li><b>Filter bar (komponen ZK).</b> Baris berisi Combobox tahun akademik, Combobox semester,
 *       dan tombol "Muat Ulang". Saat tombol ditekan, metode {@link #refresh()} dipanggil dan
 *       seluruh konten dibangun ulang sesuai filter terpilih.</li>
 *   <li><b>Area grafik (HTML murni).</b> Seluruh grafik dibangun sebagai string HTML oleh
 *       {@link HtmlChartHelper} dan dimasukkan ke komponen {@link MyHtml}. Pendekatan ini
 *       ringan karena tidak ada gambar yang digenerate di server; browser yang menggambar lewat
 *       CSS dan SVG inline. Tampilan secara otomatis responsif di ponsel maupun desktop.</li>
 *   <li><b>Tabel ZK + Unduh Excel.</b> Ringkasan per prodi ditampilkan sebagai {@link MyGrid}
 *       sehingga bisa disortir dan dapat langsung diunduh ke Excel via {@code UIUtil.downloadGrid()}.</li>
 * </ul>
 *
 * <h2>Manajemen Sesi Database</h2>
 * <p>
 * Semua query menggunakan {@link HibernateUtil#currentSession()} yang dikelola oleh Spring
 * (transaction-aware). Sesi <em>tidak</em> ditutup secara manual karena Spring yang mengurus
 * siklus hidupnya. Semua operasi hanya bersifat baca (SELECT) sehingga tidak membutuhkan
 * transaksi write.
 * </p>
 *
 * <h2>Perbandingan Tahun Akademik</h2>
 * <p>
 * Tahun akademik sebelumnya dihitung otomatis dari format "YYYY/YYYY+1", misalnya
 * "2026/2027" menjadi "2025/2026". Jika tahun sebelumnya tidak memiliki data, kartu KPI
 * menampilkan "-" untuk delta agar tidak menyesatkan pengguna.
 * </p>
 *
 * <h2>Optimasi Memori</h2>
 * <ul>
 *   <li>Semua query dibatasi hasil maksimalnya ({@code setMaxResults}) agar tidak menarik
 *       data terlalu banyak ke memori JVM.</li>
 *   <li>Setiap {@link Criteria} dibuat sebagai objek terpisah dan tidak di-reuse antar
 *       query agar tidak ada risiko "kebocoran" kondisi filter.</li>
 *   <li>HTML dibangun menggunakan {@link StringBuilder} tunggal dengan kapasitas awal
 *       yang cukup besar untuk menghindari realokasi array internal yang berulang.</li>
 *   <li>Objek Map sementara untuk penggabungan data antar-query dibersihkan di akhir
 *       method setelah data telah dipindahkan ke List hasil akhir.</li>
 * </ul>
 *
 * <h2>Cara Memperluas Dasbor</h2>
 * <p>
 * Untuk menambah panel baru: (1) Tambahkan method {@code buildXxx()} yang mengembalikan
 * {@code String} HTML menggunakan {@link HtmlChartHelper}; (2) Panggil method tersebut
 * di dalam {@link #buildHtml(Session, String, String)}; (3) Jika perlu data baru, tambahkan
 * method {@code queryXxx()} dengan pola yang sama: {@code currentSession()}, try-catch,
 * dan kembalikan collection kosong jika terjadi error.
 * </p>
 *
 * @see HtmlChartHelper
 * @see RekapPendaftarSpmb
 * @see UIUtil#downloadGrid(org.zkoss.zul.Grid)
 */
public class DashboardStatistikPmb {

	// ═══════════════════════════════════════════════════════════════
	// Inner data classes
	// ═══════════════════════════════════════════════════════════════

	/** Snapshot angka-angka utama (KPI) untuk satu tahun akademik. */
	private static final class KpiData {
		int peminat;
		int pesertaUjian;
		int lulus;
		int dapatNIM;

		KpiData(int p, int pu, int l, int nim) {
			peminat = p;
			pesertaUjian = pu;
			lulus = l;
			dapatNIM = nim;
		}
	}

	/** Jumlah pendaftar di satu bulan tertentu. */
	private static final class BulanData {
		int bulan;  // 1-12
		int jumlah;

		BulanData(int b, int j) {
			bulan = b;
			jumlah = j;
		}
	}

	/**
	 * Data rasio keketatan satu program studi: berapa yang mendaftar (pilihan 1),
	 * berapa yang diterima (prodiLulus), dan berapa yang akhirnya dapat NIM.
	 */
	private static final class ProdiKetatatanData {
		Object  id;
		String  nama;
		int     peminat;
		int     diterima;
		int     nim;

		ProdiKetatatanData(Object id, String nama, int peminat) {
			this.id      = id;
			this.nama    = nama;
			this.peminat = peminat;
		}
	}

	/** Jumlah calon yang mendaftar di satu gelombang penerimaan. */
	private static final class GelombangData {
		String nama;
		int    jumlah;

		GelombangData(String n, int j) {
			nama   = n;
			jumlah = j;
		}
	}

	/** Jumlah calon dari satu propinsi/daerah asal. */
	private static final class PropinsiData {
		String nama;
		int    jumlah;

		PropinsiData(String n, int j) {
			nama   = n;
			jumlah = j;
		}
	}

	/** Jumlah calon laki-laki vs perempuan. */
	private static final class GenderData {
		int lakiLaki;
		int perempuan;

		GenderData(int l, int p) {
			lakiLaki  = l;
			perempuan = p;
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// Konstanta
	// ═══════════════════════════════════════════════════════════════

	private static final String[] NAMA_BULAN = {
			"Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
			"Jul", "Agt", "Sep", "Okt", "Nov", "Des"
	};

	private static final String[] WARNA_KPI = {
			"#1877f2", "#059669", "#d97706", "#7c3aed", "#0891b2"
	};

	private static final int MAX_PRODI    = 6;
	private static final int MAX_PROPINSI = 8;
	private static final int MAX_TAHUN    = 5;
	private static final int MAX_ABBREV   = 20;

	// ═══════════════════════════════════════════════════════════════
	// Instance fields
	// ═══════════════════════════════════════════════════════════════

	private final PerguruanTinggi pt;

	/** Combobox tahun akademik di filter bar. */
	private Combobox cboTA;

	/** Combobox semester di filter bar. */
	private Combobox cboSem;

	/** Combobox jenjang studi di filter bar. */
	private Combobox cboJenjang;

	/** Area HTML charts — dibersihkan saat Muat Ulang. */
	private Div contentHolder;

	/** Area tabel ZK grid — dibersihkan saat Muat Ulang. */
	private Div tableHolder;

	// ═══════════════════════════════════════════════════════════════
	// Konstruktor
	// ═══════════════════════════════════════════════════════════════

	/**
	 * Membuat instance dasbor PMB.
	 *
	 * @param pt perguruan tinggi aktif; {@code null} berarti semua PT.
	 */
	public DashboardStatistikPmb(PerguruanTinggi pt) {
		this.pt = pt;
	}

	// ═══════════════════════════════════════════════════════════════
	// API publik
	// ═══════════════════════════════════════════════════════════════

	/**
	 * Membangun seluruh isi dasbor ke dalam {@code container}.
	 * Memanggil metode ini lebih dari sekali pada container yang sama
	 * akan menghapus isi lama dan membangun ulang dari awal.
	 *
	 * @param container tabpanel tujuan.
	 * @param defaultTA tahun akademik awal yang akan dipilih.
	 * @param defaultSem semester awal ("Ganjil"/"Genap"/null untuk semua).
	 */
	public void build(Tabpanel container, String defaultTA, String defaultSem) {
		Common.clear(container);

		// ── Filter bar ────────────────────────────────────────────────────────
		Div filterBar = new Div();
		filterBar.setStyle(
				"display:flex;align-items:center;gap:8px;flex-wrap:wrap;"
				+ "padding:10px 14px;background:var(--ais-card-bg,#fff);"
				+ "border-radius:10px;box-shadow:0 1px 4px rgba(0,0,0,.07);"
				+ "margin-bottom:14px;");
		filterBar.setParent(container);

		Label lblTa = new Label(ais.common.Common.getBahasaConfig("Tahun Akademik:"));
		lblTa.setStyle("font-size:12px;color:#6b7280;white-space:nowrap;");
		lblTa.setParent(filterBar);

		cboTA = new Combobox();
		cboTA.setWidth("148px");
		cboTA.setReadonly(true);
		Common.generateTahunAjaranDanSemua(cboTA);
		Common.selectComboItem(cboTA, defaultTA != null ? defaultTA
				: Common.getCurrentTahunAkademik());
		cboTA.setParent(filterBar);

		Label lblSem = new Label(ais.common.Common.getBahasaConfig("Semester:"));
		lblSem.setStyle("font-size:12px;color:#6b7280;white-space:nowrap;margin-left:4px;");
		lblSem.setParent(filterBar);

		cboSem = buildSemesterCombo(defaultSem);
		cboSem.setParent(filterBar);

		Label lblJenjang = new Label(Common.getBahasaConfig("Jenjang Studi:"));
		lblJenjang.setStyle("font-size:12px;color:#6b7280;white-space:nowrap;margin-left:4px;");
		lblJenjang.setParent(filterBar);

		cboJenjang = new Combobox();
		cboJenjang.setWidth("120px");
		cboJenjang.setReadonly(true);
		Common.insertComboDanSemua(cboJenjang, "nama", Jenjang.class,
				Restrictions.in("nama", new String[] { "D3", "S1", "S2", "S3", "Profesi" }));
		if (cboJenjang.getItemCount() > 0) {
			cboJenjang.setSelectedIndex(0);
		}
		cboJenjang.setParent(filterBar);

		MyToolbarbuttonConfig btnRefresh = new MyToolbarbuttonConfig("Muat Ulang", "/img/refresh.gif");
		btnRefresh.setStyle("margin-left:6px;");
		btnRefresh.setParent(filterBar);
		btnRefresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				refresh();
			}
		});

		// ── Holder area konten ────────────────────────────────────────────────
		contentHolder = new Div();
		contentHolder.setParent(container);

		tableHolder = new Div();
		tableHolder.setStyle("margin-top:16px;");
		tableHolder.setParent(container);

		// ── Muat data pertama kali ────────────────────────────────────────────
		refresh();
	}

	/**
	 * Membangun ulang seluruh isi grafik dan tabel menggunakan filter yang sedang aktif.
	 * Dipanggil otomatis pada {@link #build} dan setiap kali pengguna menekan "Muat Ulang".
	 */
	public void refresh() {
		if (contentHolder == null) {
			return;
		}
		String ta  = getSelectedTA();
		String sem = getSelectedSem();

		Common.clear(contentHolder);
		Common.clear(tableHolder);

		Session session = HibernateUtil.currentSession();

		// HTML grafik
		String html = buildHtml(session, ta, sem);
		new MyHtml(html).setParent(contentHolder);

		// ZK Grid tabel
		buildTable(session, ta, sem);
	}

	// ═══════════════════════════════════════════════════════════════
	// Filter helpers
	// ═══════════════════════════════════════════════════════════════

	private String getSelectedTA() {
		if (cboTA != null && cboTA.getSelectedItem() != null
				&& cboTA.getSelectedItem().getValue() != null) {
			return cboTA.getSelectedItem().getLabel();
		}
		return Common.getCurrentTahunAkademik();
	}

	private String getSelectedSem() {
		if (cboSem != null && cboSem.getSelectedItem() != null
				&& cboSem.getSelectedItem().getValue() != null) {
			return (String) cboSem.getSelectedItem().getValue();
		}
		return null;
	}

	private Jenjang getSelectedJenjang() {
		if (cboJenjang != null && cboJenjang.getSelectedItem() != null
				&& cboJenjang.getSelectedItem().getValue() instanceof Jenjang) {
			return (Jenjang) cboJenjang.getSelectedItem().getValue();
		}
		return null;
	}

	/** Membangun combobox semester dengan pilihan Semua / Ganjil / Genap. */
	private Combobox buildSemesterCombo(String defaultSem) {
		Combobox cbo = new Combobox();
		cbo.setWidth("110px");
		cbo.setReadonly(true);

		Comboitem ciSemua = new MyComboitemConfig();
		ciSemua.setLabel("Semua");
		ciSemua.setValue(null);
		cbo.appendChild(ciSemua);

		Comboitem ciGanjil = new MyComboitemConfig();
		ciGanjil.setLabel("Ganjil");
		ciGanjil.setValue("Ganjil");
		cbo.appendChild(ciGanjil);

		Comboitem ciGenap = new MyComboitemConfig();
		ciGenap.setLabel("Genap");
		ciGenap.setValue("Genap");
		cbo.appendChild(ciGenap);

		if ("Ganjil".equals(defaultSem)) {
			cbo.setSelectedItem(ciGanjil);
		} else if ("Genap".equals(defaultSem)) {
			cbo.setSelectedItem(ciGenap);
		} else {
			cbo.setSelectedItem(ciSemua);
		}
		return cbo;
	}

	// ═══════════════════════════════════════════════════════════════
	// HTML builder utama
	// ═══════════════════════════════════════════════════════════════

	@SuppressWarnings("unchecked")
	private String buildHtml(Session session, String ta, String sem) {
		String prevTA = getPreviousTA(ta);

		// ── Ambil semua data sekaligus ──────────────────────────────────────
		KpiData kpi      = queryKpi(session, ta, sem);
		KpiData kpiPrev  = prevTA != null ? queryKpi(session, prevTA, sem) : null;
		GenderData gender       = queryGender(session, ta, sem);
		List<BulanData> bulanIni  = queryPerBulan(session, ta, sem);
		List<BulanData> bulanLalu = prevTA != null
				? queryPerBulan(session, prevTA, sem)
				: new ArrayList<BulanData>();
		List<ProdiKetatatanData> prodiList = queryProdiKeketatan(session, ta, sem);
		List<GelombangData> gelombangList  = queryPerGelombang(session, ta, sem);
		List<PropinsiData>  propinsiList   = queryTopPropinsi(session, ta, sem);

		// Tren multi-tahun
		List<String> taList   = buildTaList(ta, MAX_TAHUN);
		int n                 = taList.size();
		int[] trendPeminat    = new int[n];
		int[] trendLulus      = new int[n];
		int[] trendNIM        = new int[n];
		for (int i = 0; i < n; i++) {
			KpiData k      = queryKpi(session, taList.get(i), sem);
			trendPeminat[i] = k.peminat;
			trendLulus[i]   = k.lulus;
			trendNIM[i]     = k.dapatNIM;
		}

		// ── Susun HTML ──────────────────────────────────────────────────────
		StringBuilder sb = new StringBuilder(10240);

		// Header
		sb.append("<div style=\"margin-bottom:14px;\">");
		sb.append("<div style=\"font-size:18px;font-weight:700;color:#1e3a5f;margin-bottom:3px;\">")
		  .append("Dasbor PMB &#8212; ").append(escHtml(ta)).append("</div>");
		sb.append("<div style=\"font-size:12px;color:#9ca3af;\">")
		  .append("Gambaran lengkap penerimaan mahasiswa baru dari pertama mendaftar hingga resmi ber-NIM.")
		  .append("</div></div>");

		// 1. KPI
		sb.append(buildKpiSection(kpi, kpiPrev));

		// 2. Corong + Gender
		sb.append(grid2col(buildCorong(kpi), buildGender(gender)));

		// 3. Pendaftar per bulan
		sb.append(fullWidth(buildTrendBulan(ta, prevTA, bulanIni, bulanLalu)));

		// 4. Rasio Keketatan per Prodi
		if (!prodiList.isEmpty()) {
			sb.append(fullWidth(buildRasioKeketatan(prodiList)));
		}

		// 5. Top Prodi + Radar
		sb.append(grid2col(
				buildTopProdi(prodiList),
				buildRadar(kpi, kpiPrev, ta, prevTA)));

		// 6. Tren multi-tahun
		sb.append(fullWidth(buildTrendMultiTahun(taList, trendPeminat, trendLulus, trendNIM)));

		// 7. Gelombang + Propinsi
		sb.append(grid2col(
				buildGelombang(gelombangList),
				buildPropinsi(propinsiList)));

		return sb.toString();
	}

	/** Bungkus satu blok HTML dalam wrapper {@code margin-top:14px}. */
	private String fullWidth(String html) {
		return "<div style=\"margin-top:14px;\">" + html + "</div>";
	}

	/** Bungkus dua blok HTML dalam grid 2 kolom responsif. */
	private String grid2col(String left, String right) {
		return "<div style=\"display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));"
				+ "gap:14px;margin-top:14px;\">"
				+ left + right + "</div>";
	}

	// ═══════════════════════════════════════════════════════════════
	// Section builders
	// ═══════════════════════════════════════════════════════════════

	private String buildKpiSection(KpiData kpi, KpiData prev) {
		double konvIni  = kpi.peminat > 0 ? (kpi.dapatNIM * 100.0 / kpi.peminat) : 0;
		double konvPrev = (prev != null && prev.peminat > 0)
				? (prev.dapatNIM * 100.0 / prev.peminat) : 0;

		String[] labels = { "Total Peminat", "Peserta Ujian", "Lulus/Diterima",
				"Dapat NIM", "Tingkat Keberhasilan" };
		String[] values = {
				fmtAngka(kpi.peminat),
				fmtAngka(kpi.pesertaUjian),
				fmtAngka(kpi.lulus),
				fmtAngka(kpi.dapatNIM),
				fmt1(konvIni) + "%"
		};

		String[] subtitles = new String[5];
		String[] deltas    = new String[5];
		boolean[] positifs  = new boolean[5];

		if (prev != null) {
			int[] curArr = { kpi.peminat, kpi.pesertaUjian, kpi.lulus, kpi.dapatNIM };
			int[] pvArr  = { prev.peminat, prev.pesertaUjian, prev.lulus, prev.dapatNIM };

			for (int i = 0; i < 4; i++) {
				subtitles[i] = "th. lalu: " + fmtAngka(pvArr[i]);
				if (pvArr[i] > 0) {
					double d = (curArr[i] - pvArr[i]) * 100.0 / pvArr[i];
					positifs[i] = d >= 0;
					deltas[i]   = (d >= 0 ? "+" : "") + fmt1(d) + "% vs th. lalu";
				}
			}
			subtitles[4] = "th. lalu: " + fmt1(konvPrev) + "%";
			double dk    = konvIni - konvPrev;
			positifs[4]  = dk >= 0;
			deltas[4]    = (dk >= 0 ? "+" : "") + fmt1(dk) + "pp vs th. lalu";
		} else {
			for (int i = 0; i < 5; i++) {
				subtitles[i] = "";
				deltas[i]    = "";
			}
		}

		return HtmlChartHelper.kpiCards(labels, values, subtitles, deltas, positifs, WARNA_KPI);
	}

	private String buildCorong(KpiData kpi) {
		return HtmlChartHelper.barHorizontal(
				"Corong Penerimaan",
				"Perjalanan calon mahasiswa dari pertama mendaftar hingga resmi ber-NIM. "
				+ "Batang yang jauh lebih pendek di suatu tahap berarti banyak yang tidak "
				+ "melanjutkan ke tahap tersebut.",
				new String[] { "Peminat", "Peserta Ujian", "Lulus/Diterima", "Dapat NIM" },
				new double[] { kpi.peminat, kpi.pesertaUjian, kpi.lulus, kpi.dapatNIM },
				"#1877f2");
	}

	private String buildGender(GenderData g) {
		int total = g.lakiLaki + g.perempuan;
		String tengah = total > 0
				? fmtAngka(total) + " total"
				: "belum ada data";
		return HtmlChartHelper.donut(
				"Komposisi Jenis Kelamin",
				"Perbandingan calon mahasiswa laki-laki dan perempuan. "
				+ "Berguna untuk perencanaan fasilitas dan laporan kesetaraan gender.",
				new String[] { "Laki-laki", "Perempuan" },
				new double[] { g.lakiLaki, g.perempuan },
				new String[] { "#2563eb", "#ec4899" },
				tengah);
	}

	private String buildTrendBulan(String ta, String prevTA,
			List<BulanData> ini, List<BulanData> lalu) {
		// Tentukan rentang bulan dari data yang ada
		int bulanMin = 13;
		int bulanMax = 0;
		for (int i = 0; i < ini.size(); i++) {
			if (ini.get(i).bulan < bulanMin) { bulanMin = ini.get(i).bulan; }
			if (ini.get(i).bulan > bulanMax) { bulanMax = ini.get(i).bulan; }
		}
		for (int i = 0; i < lalu.size(); i++) {
			if (lalu.get(i).bulan < bulanMin) { bulanMin = lalu.get(i).bulan; }
			if (lalu.get(i).bulan > bulanMax) { bulanMax = lalu.get(i).bulan; }
		}
		if (bulanMin > bulanMax) { bulanMin = 1; bulanMax = 12; }

		int     numCat = bulanMax - bulanMin + 1;
		String[] cats  = new String[numCat];
		double[][] vals = new double[numCat][2];

		for (int b = bulanMin; b <= bulanMax; b++) {
			int idx    = b - bulanMin;
			cats[idx]  = NAMA_BULAN[b - 1];
			vals[idx][0] = findBulan(lalu, b);
			vals[idx][1] = findBulan(ini,  b);
		}

		String lblPrev = prevTA != null
				? "PMB " + extractTahunAwal(prevTA) : "Th. Lalu";
		String lblIni  = "PMB " + extractTahunAwal(ta);

		return HtmlChartHelper.barVerticalGrouped(
				"Pendaftar Per Bulan — " + lblIni + " vs " + lblPrev,
				"Jumlah calon yang mendaftar tiap bulan dibandingkan dengan tahun sebelumnya. "
				+ "Batang yang tinggi di suatu bulan menunjukkan periode promosi paling efektif.",
				cats,
				new String[] { lblPrev, lblIni },
				vals,
				new String[] { "#93c5fd", "#2563eb" });
	}

	private String buildRasioKeketatan(List<ProdiKetatatanData> list) {
		int     n    = Math.min(list.size(), MAX_PRODI);
		String[] cats  = new String[n];
		double[][] vals = new double[n][3];

		for (int i = 0; i < n; i++) {
			ProdiKetatatanData d = list.get(i);
			cats[i]    = abbrev(d.nama, MAX_ABBREV);
			vals[i][0] = d.peminat;
			vals[i][1] = d.diterima;
			vals[i][2] = d.nim;
		}

		return HtmlChartHelper.barVerticalGrouped(
				"Keketatan & Keberhasilan per Program Studi",
				"Untuk setiap prodi: batang biru = yang mendaftar (pilihan 1), "
				+ "hijau = yang diterima, ungu = yang resmi ber-NIM. "
				+ "Selisih yang besar antara mendaftar dan diterima berarti persaingan ketat.",
				cats,
				new String[] { "Peminat (Pil.1)", "Diterima", "Dapat NIM" },
				vals,
				new String[] { "#3b82f6", "#10b981", "#8b5cf6" });
	}

	private String buildTopProdi(List<ProdiKetatatanData> list) {
		if (list.isEmpty()) {
			return emptyCard("Top 5 Program Studi Peminat",
					"Belum ada data pilihan prodi untuk filter ini.");
		}
		int     n    = Math.min(list.size(), 5);
		String[] lbl = new String[n];
		double[] val = new double[n];
		for (int i = 0; i < n; i++) {
			lbl[i] = list.get(i).nama;
			val[i] = list.get(i).peminat;
		}
		return HtmlChartHelper.barHorizontal(
				"5 Program Studi Paling Diminati",
				"Lima prodi yang paling banyak dipilih sebagai pilihan pertama. "
				+ "Batang terpanjang = prodi paling diminati.",
				lbl, val, "#8b5cf6");
	}

	private String buildRadar(KpiData kpi, KpiData prev, String ta, String prevTA) {
		double maks = Math.max(kpi.peminat, prev != null ? prev.peminat : 0);
		if (maks <= 0) { maks = 1; }

		double konvIni  = kpi.peminat > 0
				? (kpi.dapatNIM * 100.0 / kpi.peminat) : 0;
		double konvPrev = (prev != null && prev.peminat > 0)
				? (prev.dapatNIM * 100.0 / prev.peminat) : 0;
		double konvMaks = Math.max(konvIni, konvPrev);
		if (konvMaks <= 0) { konvMaks = 100; }

		String lIni  = "PMB " + extractTahunAwal(ta);
		String lPrev = prevTA != null
				? "PMB " + extractTahunAwal(prevTA) : "Th. Lalu";

		String[] axes = { "Peminat", "Peserta Ujian", "Lulus", "Dapat NIM", "Konversi" };
		double[][] vals;
		String[] seriesLabels;

		if (prev != null) {
			vals = new double[][] {
					{ kpi.peminat,  kpi.pesertaUjian,  kpi.lulus,  kpi.dapatNIM,
						konvIni / konvMaks * maks },
					{ prev.peminat, prev.pesertaUjian, prev.lulus, prev.dapatNIM,
						konvPrev / konvMaks * maks }
			};
			seriesLabels = new String[] { lIni, lPrev };
		} else {
			vals = new double[][] {
					{ kpi.peminat, kpi.pesertaUjian, kpi.lulus, kpi.dapatNIM,
						konvIni / 100.0 * maks }
			};
			seriesLabels = new String[] { lIni };
		}

		return HtmlChartHelper.radar(
				"Perbandingan Lima Dimensi PMB",
				"Jaring laba-laba membandingkan lima ukuran penerimaan tahun ini dan tahun lalu. "
				+ "Bidang yang lebih lebar berarti hasil yang lebih baik secara keseluruhan.",
				axes, seriesLabels, vals,
				new String[] { "#2563eb", "#f97316" }, 0);
	}

	private String buildTrendMultiTahun(List<String> taList,
			int[] peminat, int[] lulus, int[] nim) {
		int     n    = taList.size();
		String[] cats = new String[n];
		for (int i = 0; i < n; i++) {
			cats[i] = extractTahunAwal(taList.get(i));
		}
		double[][] vals = new double[3][n];
		for (int i = 0; i < n; i++) {
			vals[0][i] = peminat[i];
			vals[1][i] = lulus[i];
			vals[2][i] = nim[i];
		}
		return HtmlChartHelper.lineMulti(
				"Tren " + n + " Tahun Terakhir",
				"Perkembangan jumlah peminat, yang diterima, dan yang dapat NIM dari tahun ke tahun. "
				+ "Garis yang terus naik menandakan pertumbuhan yang sehat.",
				cats,
				new String[] { "Peminat", "Lulus/Diterima", "Dapat NIM" },
				vals,
				new String[] { "#1877f2", "#16a34a", "#f59e0b" });
	}

	private String buildGelombang(List<GelombangData> list) {
		if (list.isEmpty()) {
			return emptyCard("Distribusi Per Gelombang", "Belum ada data gelombang.");
		}
		int     n    = list.size();
		String[] lbl = new String[n];
		double[] val = new double[n];
		for (int i = 0; i < n; i++) {
			lbl[i] = list.get(i).nama;
			val[i] = list.get(i).jumlah;
		}
		return HtmlChartHelper.barHorizontal(
				"Distribusi Per Gelombang",
				"Jumlah pendaftar di setiap gelombang penerimaan. "
				+ "Berguna untuk evaluasi jadwal dan kuota tiap gelombang.",
				lbl, val, "#06b6d4");
	}

	private String buildPropinsi(List<PropinsiData> list) {
		if (list.isEmpty()) {
			return emptyCard("8 Daerah Asal Terbanyak", "Belum ada data daerah asal.");
		}
		int     n    = list.size();
		String[] lbl = new String[n];
		double[] val = new double[n];
		for (int i = 0; i < n; i++) {
			lbl[i] = list.get(i).nama;
			val[i] = list.get(i).jumlah;
		}
		return HtmlChartHelper.barHorizontal(
				"8 Daerah Asal Calon Mahasiswa Terbanyak",
				"Delapan daerah/propinsi yang mengirimkan paling banyak calon mahasiswa. "
				+ "Daerah yang tidak masuk daftar ini bisa jadi target promosi berikutnya.",
				lbl, val, "#e4496b");
	}

	/** Panel kosong bila tidak ada data untuk suatu seksi. */
	private String emptyCard(String judul, String pesan) {
		return "<div class=\"ais-chart-card\"><div class=\"ais-chart-title\">"
				+ escHtml(judul) + "</div><div class=\"ais-chart-desc\" "
				+ "style=\"color:#9ca3af;padding:12px 0;\">"
				+ escHtml(pesan) + "</div></div>";
	}

	// ═══════════════════════════════════════════════════════════════
	// ZK Grid: tabel ringkasan per prodi
	// ═══════════════════════════════════════════════════════════════

	private void buildTable(Session session, String ta, String sem) {
		if (tableHolder == null) {
			return;
		}

		List<ProdiKetatatanData> list = queryProdiKeketatan(session, ta, sem);
		if (list.isEmpty()) {
			return;
		}

		// ── Header tabel ─────────────────────────────────────────────────────
		Div headerDiv = new Div();
		headerDiv.setStyle("display:flex;align-items:center;justify-content:space-between;"
				+ "flex-wrap:wrap;gap:8px;margin-bottom:8px;");
		headerDiv.setParent(tableHolder);

		Div titleDiv = new Div();
		titleDiv.setParent(headerDiv);
		MyLabelBoldAja lblJudul = new MyLabelBoldAja("Ringkasan Per Program Studi");
		lblJudul.setParent(titleDiv);
		Label lblSub = new Label(ais.common.Common.getBahasaConfig("Data lengkap per prodi yang bisa diunduh ke Excel."));
		lblSub.setStyle("font-size:11px;color:#6b7280;display:block;margin-top:2px;");
		lblSub.setParent(titleDiv);

		// ── Grid ─────────────────────────────────────────────────────────────
		final MyGrid tableGrid = new MyGrid();
		tableGrid.setWidth("100%");
		tableGrid.setFixedLayout(true);
		tableGrid.setParent(tableHolder);

		// Kolom
		Columns cols = new Columns();
		cols.setParent(tableGrid);
		addCol(cols, "No.",             "45px");
		addCol(cols, "Program Studi",   null);
		addCol(cols, "Peminat (Pil.1)", "110px");
		addCol(cols, "Diterima",        "90px");
		addCol(cols, "Dapat NIM",       "90px");
		addCol(cols, "Konversi (%)",    "100px");

		// Baris data
		Rows rows = new Rows();
		rows.setParent(tableGrid);

		int totalPeminat  = 0;
		int totalDiterima = 0;
		int totalNIM      = 0;

		for (int i = 0; i < list.size(); i++) {
			ProdiKetatatanData d = list.get(i);
			totalPeminat  += d.peminat;
			totalDiterima += d.diterima;
			totalNIM      += d.nim;

			double konv = d.peminat > 0 ? (d.nim * 100.0 / d.peminat) : 0;

			Row row = new Row();
			row.setParent(rows);
			addCell(row, String.valueOf(i + 1));
			addCell(row, d.nama);
			addCell(row, fmtAngka(d.peminat));
			addCell(row, fmtAngka(d.diterima));
			addCell(row, fmtAngka(d.nim));
			addCellStyled(row, fmt1(konv) + "%",
					konv >= 50 ? "#16a34a" : konv >= 20 ? "#d97706" : "#dc2626");
		}

		// Baris total
		double konvTotal = totalPeminat > 0 ? (totalNIM * 100.0 / totalPeminat) : 0;
		Row rowTotal = new Row();
		rowTotal.setStyle("font-weight:bold;background:#f0f4ff;");
		rowTotal.setParent(rows);
		addCell(rowTotal, "");
		addCell(rowTotal, "TOTAL");
		addCell(rowTotal, fmtAngka(totalPeminat));
		addCell(rowTotal, fmtAngka(totalDiterima));
		addCell(rowTotal, fmtAngka(totalNIM));
		addCell(rowTotal, fmt1(konvTotal) + "%");

		// ── Tombol unduh ──────────────────────────────────────────────────────
		MyToolbarbuttonConfig btnDownload = new MyToolbarbuttonConfig(
				"Unduh Excel", "/img/excel.png");
		btnDownload.setStyle("margin-top:8px;");
		btnDownload.setParent(tableHolder);
		btnDownload.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UIUtil.downloadGrid(tableGrid);
			}
		});
	}

	/** Tambah header kolom ke grid. */
	private void addCol(Columns cols, String label, String width) {
		MyColumnConfig col = new MyColumnConfig();
		col.setLabel(label);
		if (width != null) {
			col.setWidth(width);
		}
		col.setParent(cols);
	}

	/** Tambah sel Label ke Row. */
	private void addCell(Row row, String text) {
		new Label(text).setParent(row);
	}

	/** Tambah sel Label berwarna ke Row. */
	private void addCellStyled(Row row, String text, String color) {
		Label lbl = new Label(text);
		lbl.setStyle("color:" + color + ";font-weight:600;");
		lbl.setParent(row);
	}

	// ═══════════════════════════════════════════════════════════════
	// Query methods
	// ═══════════════════════════════════════════════════════════════

	/** Menghitung 4 angka KPI utama untuk satu TA. Mengembalikan nol jika query gagal. */
	@SuppressWarnings("unchecked")
	private KpiData queryKpi(Session session, String ta, String sem) {
		try {
			int peminat, peserta, lulus, nim;

			Criteria cPem = baseCriteria(session, ta, sem);
			cPem.setProjection(Projections.rowCount());
			peminat = ((Number) cPem.uniqueResult()).intValue();

			Criteria cPes = baseCriteria(session, ta, sem);
			cPes.add(Restrictions.isNotNull("noUjian"))
			    .add(Restrictions.ne("noUjian", ""))
			    .setProjection(Projections.rowCount());
			peserta = ((Number) cPes.uniqueResult()).intValue();

			Criteria cLul = baseCriteria(session, ta, sem);
			cLul.add(Restrictions.isNotNull("prodiLulus"))
			    .setProjection(Projections.rowCount());
			lulus = ((Number) cLul.uniqueResult()).intValue();

			Criteria cNIM = baseCriteria(session, ta, sem);
			cNIM.add(Restrictions.isNotNull("mahasiswa"))
			    .setProjection(Projections.rowCount());
			nim = ((Number) cNIM.uniqueResult()).intValue();

			return new KpiData(peminat, peserta, lulus, nim);
		} catch (Exception e) {
			logErr("queryKpi ta=" + ta, e);
			return new KpiData(0, 0, 0, 0);
		}
	}

	/** Menghitung jumlah calon laki-laki dan perempuan. */
	@SuppressWarnings("unchecked")
	private GenderData queryGender(Session session, String ta, String sem) {
		try {
			String hql = "SELECT bcm.jenisKelamin, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm "
					+ "WHERE bcm.tahunAkademik = :ta "
					+ semClause(sem)
					+ jenjangClause()
					+ "AND bcm.jenisKelamin IS NOT NULL "
					+ "GROUP BY bcm.jenisKelamin";
			org.hibernate.Query q = session.createQuery(hql)
					.setParameter("ta", ta);
			if (hasSem(sem)) { q.setParameter("sem", sem); }
			applyJenjangParam(q);

			int laki = 0, perempuan = 0;
			for (Object obj : q.list()) {
				Object[] r   = (Object[]) obj;
				String   jk  = r[0] == null ? "" : r[0].toString();
				int      cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
				if (jk.toLowerCase().startsWith("l")) {
					laki += cnt;
				} else {
					perempuan += cnt;
				}
			}
			return new GenderData(laki, perempuan);
		} catch (Exception e) {
			logErr("queryGender ta=" + ta, e);
			return new GenderData(0, 0);
		}
	}

	/** Menghitung jumlah pendaftar per bulan (1-12). */
	@SuppressWarnings("unchecked")
	private List<BulanData> queryPerBulan(Session session, String ta, String sem) {
		List<BulanData> result = new ArrayList<BulanData>();
		try {
			String hql = "SELECT MONTH(bcm.tanggalDaftar), COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm "
					+ "WHERE bcm.tahunAkademik = :ta "
					+ semClause(sem)
					+ jenjangClause()
					+ "AND bcm.tanggalDaftar IS NOT NULL "
					+ "GROUP BY MONTH(bcm.tanggalDaftar) "
					+ "ORDER BY MONTH(bcm.tanggalDaftar)";
			org.hibernate.Query q = session.createQuery(hql)
					.setParameter("ta", ta);
			if (hasSem(sem)) { q.setParameter("sem", sem); }
			applyJenjangParam(q);

			for (Object obj : q.list()) {
				Object[] r    = (Object[]) obj;
				int      bln  = r[0] == null ? 0 : ((Number) r[0]).intValue();
				int      cnt  = r[1] == null ? 0 : ((Number) r[1]).intValue();
				if (bln >= 1 && bln <= 12) {
					result.add(new BulanData(bln, cnt));
				}
			}
		} catch (Exception e) {
			logErr("queryPerBulan ta=" + ta, e);
		}
		return result;
	}

	/**
	 * Mengambil data keketatan per prodi: peminat (pilihan 1), diterima (prodiLulus),
	 * dan dapat NIM. Menggabungkan tiga query menjadi satu list terurut berdasarkan peminat.
	 */
	@SuppressWarnings("unchecked")
	private List<ProdiKetatatanData> queryProdiKeketatan(Session session, String ta, String sem) {
		List<ProdiKetatatanData> result = new ArrayList<ProdiKetatatanData>();
		try {
			// Query 1: peminat per prodi (pilihan 1) — urut terbanyak, TANPA limit di sini.
			// List hasil query ini dipakai bersama utk (a) tabel "Ringkasan Per Program Studi"
			// yang menjanjikan "Data lengkap per prodi" + unduh Excel (harus mencakup SEMUA
			// prodi, termasuk yang peminatnya sedikit), dan (b) grafik "Top Prodi" yang
			// SUDAH membatasi tampilannya sendiri ke MAX_PRODI teratas (lihat buildRasioKeketatan/
			// buildTopProdi, keduanya pakai Math.min(list.size(), MAX_PRODI)). Membatasi
			// MAX_PRODI di query ini dulu membuat prodi dgn peminat sedikit (mis. prodi baru/
			// sepi) hilang total dari tabel & Excel, bukan cuma dari grafik.
			String hqlPem = "SELECT j.id, j.nama, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.prodi1 j "
					+ "WHERE bcm.tahunAkademik = :ta "
					+ semClause(sem)
					+ jenjangClause()
					+ "GROUP BY j.id, j.nama "
					+ "ORDER BY COUNT(bcm) DESC";
			org.hibernate.Query qPem = session.createQuery(hqlPem)
					.setParameter("ta", ta);
			if (hasSem(sem)) { qPem.setParameter("sem", sem); }
			applyJenjangParam(qPem);

			// Simpan ke LinkedHashMap agar urutan terjaga
			Map<Object, ProdiKetatatanData> map =
					new LinkedHashMap<Object, ProdiKetatatanData>();
			for (Object obj : qPem.list()) {
				Object[] r   = (Object[]) obj;
				Object   id  = r[0];
				String   nm  = r[1] == null ? "-" : r[1].toString();
				int      cnt = r[2] == null ? 0 : ((Number) r[2]).intValue();
				map.put(id, new ProdiKetatatanData(id, nm, cnt));
			}

			if (map.isEmpty()) {
				return result;
			}

			// Query 2: diterima per prodi (prodiLulus)
			String hqlDit = "SELECT j.id, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.prodiLulus j "
					+ "WHERE bcm.tahunAkademik = :ta "
					+ semClause(sem)
					+ jenjangClause()
					+ "GROUP BY j.id";
			org.hibernate.Query qDit = session.createQuery(hqlDit)
					.setParameter("ta", ta);
			if (hasSem(sem)) { qDit.setParameter("sem", sem); }
			applyJenjangParam(qDit);

			for (Object obj : qDit.list()) {
				Object[] r   = (Object[]) obj;
				Object   id  = r[0];
				int      cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
				if (map.containsKey(id)) {
					map.get(id).diterima = cnt;
				}
			}

			// Query 3: dapat NIM per prodiLulus
			String hqlNIM = "SELECT j.id, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.prodiLulus j "
					+ "WHERE bcm.tahunAkademik = :ta "
					+ semClause(sem)
					+ jenjangClause()
					+ "AND bcm.mahasiswa IS NOT NULL "
					+ "GROUP BY j.id";
			org.hibernate.Query qNIM = session.createQuery(hqlNIM)
					.setParameter("ta", ta);
			if (hasSem(sem)) { qNIM.setParameter("sem", sem); }
			applyJenjangParam(qNIM);

			for (Object obj : qNIM.list()) {
				Object[] r   = (Object[]) obj;
				Object   id  = r[0];
				int      cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
				if (map.containsKey(id)) {
					map.get(id).nim = cnt;
				}
			}

			result.addAll(map.values());
		} catch (Exception e) {
			logErr("queryProdiKeketatan ta=" + ta, e);
		}
		return result;
	}

	/** Menghitung jumlah pendaftar per gelombang penerimaan. */
	@SuppressWarnings("unchecked")
	private List<GelombangData> queryPerGelombang(Session session, String ta, String sem) {
		List<GelombangData> result = new ArrayList<GelombangData>();
		try {
			String hql = "SELECT g.nama, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.gelombangPendaftaran g "
					+ "WHERE bcm.tahunAkademik = :ta "
					+ semClause(sem)
					+ jenjangClause()
					+ "GROUP BY g.id, g.nama ORDER BY g.id";
			org.hibernate.Query q = session.createQuery(hql)
					.setParameter("ta", ta);
			if (hasSem(sem)) { q.setParameter("sem", sem); }
			applyJenjangParam(q);

			for (Object obj : q.list()) {
				Object[] r  = (Object[]) obj;
				String nm   = r[0] == null ? "Tanpa Gelombang" : r[0].toString();
				int    cnt  = r[1] == null ? 0 : ((Number) r[1]).intValue();
				result.add(new GelombangData(nm, cnt));
			}
		} catch (Exception e) {
			logErr("queryPerGelombang ta=" + ta, e);
		}
		return result;
	}

	/** Mengambil 8 propinsi asal terbanyak; propinsiCalon adalah entity Propinsi. */
	@SuppressWarnings("unchecked")
	private List<PropinsiData> queryTopPropinsi(Session session, String ta, String sem) {
		List<PropinsiData> result = new ArrayList<PropinsiData>();
		try {
			// JOIN langsung ke entity Propinsi untuk mengambil nama
			String hql = "SELECT p.nama, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.propinsiCalon p "
					+ "WHERE bcm.tahunAkademik = :ta "
					+ semClause(sem)
					+ jenjangClause()
					+ "GROUP BY p.id, p.nama "
					+ "ORDER BY COUNT(bcm) DESC";
			org.hibernate.Query q = session.createQuery(hql)
					.setParameter("ta", ta)
					.setMaxResults(MAX_PROPINSI);
			if (hasSem(sem)) { q.setParameter("sem", sem); }
			applyJenjangParam(q);

			for (Object obj : q.list()) {
				Object[] r  = (Object[]) obj;
				String nm   = r[0] == null ? "-" : r[0].toString();
				int    cnt  = r[1] == null ? 0 : ((Number) r[1]).intValue();
				result.add(new PropinsiData(nm, cnt));
			}
		} catch (Exception e) {
			logErr("queryTopPropinsi ta=" + ta, e);
		}
		return result;
	}

	// ═══════════════════════════════════════════════════════════════
	// Utility & helper
	// ═══════════════════════════════════════════════════════════════

	/**
	 * Membuat Criteria dasar BiodataCalonMahasiswa dengan filter TA dan semester.
	 * Setiap pemanggilan menghasilkan objek Criteria baru sehingga tidak ada
	 * risiko "kebocoran" kondisi antar-query.
	 */
	private Criteria baseCriteria(Session session, String ta, String sem) {
		Criteria c = session.createCriteria(BiodataCalonMahasiswa.class)
				.add(Restrictions.eq("tahunAkademik", ta));
		if (hasSem(sem)) {
			c.add(Restrictions.eq("semesterMulai", sem));
		}
		if (getSelectedJenjang() != null) {
			c.add(Restrictions.eq("jenjang", getSelectedJenjang()));
		}
		return c;
	}

	/** Mengembalikan klausa HQL " AND bcm.semesterMulai = :sem " bila semester ada. */
	private String semClause(String sem) {
		return hasSem(sem) ? "AND bcm.semesterMulai = :sem " : "";
	}

	/** Mengembalikan true jika filter semester terisi (bukan null/kosong). */
	private boolean hasSem(String sem) {
		return sem != null && sem.trim().length() > 0;
	}

	private String jenjangClause() {
		return getSelectedJenjang() == null ? "" : "AND bcm.jenjang = :jenjang ";
	}

	private void applyJenjangParam(org.hibernate.Query q) {
		if (getSelectedJenjang() != null) {
			q.setParameter("jenjang", getSelectedJenjang());
		}
	}

	/**
	 * Menghitung tahun akademik sebelumnya dari format "YYYY/YYYY+1".
	 * Contoh: "2026/2027" → "2025/2026". Mengembalikan {@code null} jika format tidak dikenal.
	 */
	static String getPreviousTA(String ta) {
		if (ta == null) { return null; }
		int idx = ta.indexOf('/');
		if (idx <= 0) { return null; }
		try {
			int y1 = Integer.parseInt(ta.substring(0, idx).trim()) - 1;
			int y2 = Integer.parseInt(ta.substring(idx + 1).trim()) - 1;
			return y1 + "/" + y2;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** Membangun daftar TA dari yang paling lama sampai {@code ta}, maks {@code count} tahun. */
	private List<String> buildTaList(String ta, int count) {
		List<String> list = new ArrayList<String>();
		String cur = ta;
		for (int i = 0; i < count; i++) {
			list.add(0, cur);
			String prev = getPreviousTA(cur);
			if (prev == null) { break; }
			cur = prev;
		}
		return list;
	}

	/** Mencari jumlah suatu bulan dari list BulanData; 0 jika tidak ditemukan. */
	private int findBulan(List<BulanData> list, int bulan) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).bulan == bulan) { return list.get(i).jumlah; }
		}
		return 0;
	}

	/** Mengekstrak tahun awal dari format "YYYY/YYYY+1", mis. "2026/2027" → "2026". */
	private String extractTahunAwal(String ta) {
		if (ta == null) { return ""; }
		int idx = ta.indexOf('/');
		return idx > 0 ? ta.substring(0, idx).trim() : ta;
	}

	/** Format angka bulat dengan pemisah ribuan. */
	private static String fmtAngka(int n) {
		return NumberFormat.getIntegerInstance().format(n);
	}

	/** Format desimal satu angka di belakang koma. */
	private static String fmt1(double d) {
		return new DecimalFormat("0.#").format(d);
	}

	/** Singkat teks ke {@code max} karakter dan tambahkan "…" bila lebih. */
	private static String abbrev(String s, int max) {
		if (s == null || s.length() <= max) { return s == null ? "" : s; }
		return s.substring(0, max - 1) + "…";
	}

	/** Escape karakter HTML berbahaya. */
	private static String escHtml(String s) {
		if (s == null) { return ""; }
		return s.replace("&", "&amp;")
		        .replace("<", "&lt;")
		        .replace(">", "&gt;")
		        .replace("\"", "&quot;");
	}

	/** Catat error ke System.err dengan prefix yang mudah dicari di log. */
	private static void logErr(String ctx, Exception e) {
		System.err.println("[DashboardStatistikPmb] " + ctx + " — " + e.getMessage());
	}
}
