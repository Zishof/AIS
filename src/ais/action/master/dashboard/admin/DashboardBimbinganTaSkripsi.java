package ais.action.master.dashboard.admin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.zkoss.zul.Div;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Skripsi;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyHtml;

/**
 * <h1>DashboardBimbinganTaSkripsi &mdash; Ringkasan Bimbingan Tugas Akhir &amp; Skripsi</h1>
 *
 * <p>Menyatukan gambaran besar proses <b>Tugas Akhir</b> (pengajuan judul &rarr; bimbingan &rarr;
 * seminar) dan <b>Skripsi</b> (sidang) dalam satu halaman yang bisa dibaca siapa saja &mdash; pimpinan
 * fakultas, kaprodi, koordinator tugas akhir, hingga staf akademik &mdash; tanpa perlu keahlian IT.
 * Dasbor ini melengkapi tab "Bimbingan" (yang menampilkan daftar per-mahasiswa): di sini pengguna
 * melihat <i>angkanya secara keseluruhan</i>, bukan satu per satu.</p>
 *
 * <h2>Untuk siapa &amp; kenapa</h2>
 * <ul>
 *   <li><b>Kaprodi / Koordinator TA</b> &mdash; tahu berapa mahasiswa sedang bimbingan, berapa sudah
 *       masuk tahap sidang, dan prodi mana yang paling banyak, dari satu layar.</li>
 *   <li><b>Pimpinan fakultas</b> &mdash; memantau tren pengerjaan skripsi dari tahun ke tahun untuk
 *       perencanaan dosen pembimbing/penguji.</li>
 *   <li><b>Staf akademik</b> &mdash; melihat komposisi status pengajuan tugas akhir dengan cepat.</li>
 * </ul>
 *
 * <h2>Isi dasbor (urut dari atas)</h2>
 * <ol>
 *   <li><b>Kartu angka utama (KPI)</b> &mdash; total Tugas Akhir, total Skripsi, yang sedang bimbingan,
 *       yang sudah masuk tahap sidang, dan jumlah program studi yang terlibat. Angka besar yang
 *       langsung menjawab "berapa?".</li>
 *   <li><b>Komposisi Status Tugas Akhir (diagram donat)</b> &mdash; berapa banyak yang masih pengajuan,
 *       disetujui, seminar/bimbingan, sidang, mengulang, atau ditolak. Membantu melihat di tahap mana
 *       mahasiswa paling banyak menumpuk.</li>
 *   <li><b>Tugas Akhir per Program Studi (batang mendatar)</b> &mdash; prodi mana yang paling banyak
 *       mahasiswa tugas akhirnya. Semakin panjang batang, semakin banyak.</li>
 *   <li><b>Tren Skripsi per Tahun Akademik (grafik garis)</b> &mdash; naik-turun jumlah skripsi tiap
 *       tahun ajaran, untuk melihat apakah pengerjaan skripsi bertambah atau berkurang.</li>
 * </ol>
 *
 * <h2>Catatan teknis</h2>
 * <ul>
 *   <li>Semua grafik dibuat oleh {@link HtmlChartHelper} memakai <b>SVG + CSS modern</b> &mdash;
 *       <b>tanpa JFreeChart</b> &mdash; sehingga ringan dan tampil baik di HP maupun desktop.</li>
 *   <li><b>Hemat memori:</b> semua angka diambil lewat <i>agregasi database</i>
 *       ({@code Projections.rowCount} / {@code groupProperty}), <b>bukan</b> memuat seluruh entitas ke
 *       memori lalu menghitung di Java. Jadi seberapapun banyak datanya, yang ditarik hanya baris
 *       ringkasan.</li>
 *   <li><b>Sesi Hibernate:</b> memakai {@code HibernateUtil.currentNativeSession()} dan ditutup di blok
 *       {@code finally} (disconnect + close) agar tidak bocor. (Bila memakai {@code currentSession()}
 *       tidak perlu ditutup manual, tetapi di sini sengaja memakai native session untuk agregasi
 *       terisolasi.)</li>
 *   <li>Tiap kueri dibungkus penanganan aman: bila satu bagian gagal/kosong, panel terkait menampilkan
 *       pesan sederhana tanpa menggagalkan panel lain.</li>
 *   <li>Kompatibel Java 1.7: tanpa lambda, try-with-resources, Stream API, atau diamond operator.</li>
 * </ul>
 *
 * @see HtmlChartHelper Utilitas grafik HTML/CSS modern (donut/bar/line/radar/kpi).
 * @see ais.ui.util.BaseDasbordPortal Pembungkus mount dasbor (judul + deskripsi).
 */
public class DashboardBimbinganTaSkripsi extends Div {

	private static final long serialVersionUID = -5540121991002233114L;

	private static final String BIRU = "#2563eb";
	private static final String HIJAU = "#10b981";
	private static final String ORANYE = "#f59e0b";
	private static final String MERAH = "#ef4444";
	private static final String UNGU = "#8b5cf6";
	private static final String ABU = "#94a3b8";

	/** Membangun seluruh isi dasbor saat komponen dibuat. Aman: kegagalan dilaporkan ke admin saja. */
	public DashboardBimbinganTaSkripsi() {
		super();
		try {
			build();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menyusun panel: intro, KPI, donat status, batang per-prodi, dan garis tren. Semua angka dihitung
	 * sekali di awal (agregasi DB) lalu dipetakan ke grafik. Sesi native ditutup di {@code finally}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void build() {
		setStyle("padding:12px; box-sizing:border-box; background:#f8fafc;");

		MyHtml intro = new MyHtml("<div style='background:linear-gradient(135deg,#1e3a8a,#2563eb);color:#fff;"
				+ "border-radius:14px;padding:16px 18px;margin-bottom:12px;box-shadow:0 8px 20px rgba(37,99,235,.25);'>"
				+ "<div style='font-size:12px;letter-spacing:.06em;opacity:.85;font-weight:600;'>DASBOR BIMBINGAN</div>"
				+ "<div style='font-size:20px;font-weight:800;margin-top:2px;'>Ringkasan Tugas Akhir &amp; Skripsi</div>"
				+ "<div style='font-size:13px;opacity:.9;margin-top:6px;max-width:820px;'>Melihat berapa banyak "
				+ "mahasiswa yang sedang mengerjakan tugas akhir dan skripsi, di tahap mana saja, prodi mana yang "
				+ "terbanyak, serta perkembangannya dari tahun ke tahun.</div></div>");
		intro.setParent(this);

		long totalTa = 0;
		long totalSkripsi = 0;
		Map<String, Long> statusTa = new LinkedHashMap<String, Long>();
		Map<String, Long> perProdi = new LinkedHashMap<String, Long>();
		Map<String, Long> perTaSkripsi = new LinkedHashMap<String, Long>();

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();

			totalTa = hitung(session, MahasiswaRequestTugasAkhir.class);
			totalSkripsi = hitung(session, Skripsi.class);
			statusTa = kelompokkan(session, MahasiswaRequestTugasAkhir.class, "status", null, false);
			perProdi = kelompokkan(session, MahasiswaRequestTugasAkhir.class, "jur.nama", "mahasiswa", true);
			perTaSkripsi = kelompokkan(session, Skripsi.class, "tahunAkademik", null, false);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.disconnect();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardBimbinganTaSkripsi.java:132");
				}
				try {
					session.close();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardBimbinganTaSkripsi.java:136");
				}
			}
			HibernateUtil.closeSession();
		}

		// ---- KPI ----
		long sedangBimbingan = nilai(statusTa, MahasiswaRequestTugasAkhir.SEMINAR_STATUS);
		long tahapSidang = nilai(statusTa, MahasiswaRequestTugasAkhir.LULUS_STATUS);
		String[] kpiLabel = { "Total Tugas Akhir", "Total Skripsi", "Sedang Bimbingan", "Tahap Sidang",
				"Program Studi" };
		String[] kpiNilai = { fmt(totalTa), fmt(totalSkripsi), fmt(sedangBimbingan), fmt(tahapSidang),
				fmt(perProdi.size()) };
		String[] kpiSub = { "pengajuan tugas akhir", "skripsi tercatat", "seminar/proses bimbingan",
				"sudah masuk sidang", "prodi terlibat" };
		String[] kpiWarna = { BIRU, HIJAU, ORANYE, UNGU, ABU };
		try {
			MyHtml kpi = new MyHtml(kartuKpi(kpiLabel, kpiNilai, kpiSub, kpiWarna));
			kpi.setParent(this);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		// ---- Donat: komposisi status Tugas Akhir ----
		if (!statusTa.isEmpty()) {
			String[] labels = statusTa.keySet().toArray(new String[0]);
			double[] values = new double[labels.length];
			for (int i = 0; i < labels.length; i++) {
				values[i] = statusTa.get(labels[i]).doubleValue();
			}
			String[] colors = { BIRU, HIJAU, ORANYE, UNGU, ABU, MERAH };
			tambahPanel(HtmlChartHelper.donut("Komposisi Status Tugas Akhir",
					"Berapa banyak mahasiswa di tiap tahap: pengajuan, disetujui, seminar/bimbingan, sidang, "
							+ "mengulang, atau ditolak.",
					labels, values, colors, fmt(totalTa) + " TA"));
		}

		// ---- Batang: Tugas Akhir per Program Studi (maks 10) ----
		Map<String, Long> prodiTop = ambilTeratas(perProdi, 10);
		if (!prodiTop.isEmpty()) {
			String[] labels = prodiTop.keySet().toArray(new String[0]);
			double[] values = new double[labels.length];
			for (int i = 0; i < labels.length; i++) {
				values[i] = prodiTop.get(labels[i]).doubleValue();
			}
			tambahPanel(HtmlChartHelper.barHorizontal("Tugas Akhir per Program Studi",
					"Prodi dengan mahasiswa tugas akhir terbanyak. Makin panjang batang, makin banyak.", labels,
					values, BIRU));
		}

		// ---- Garis: tren Skripsi per Tahun Akademik ----
		if (!perTaSkripsi.isEmpty()) {
			String[] cats = perTaSkripsi.keySet().toArray(new String[0]);
			double[] vals = new double[cats.length];
			for (int i = 0; i < cats.length; i++) {
				vals[i] = perTaSkripsi.get(cats[i]).doubleValue();
			}
			tambahPanel(HtmlChartHelper.lineMulti("Tren Skripsi per Tahun Akademik",
					"Naik-turun jumlah skripsi tiap tahun ajaran. Tren naik = pengerjaan skripsi bertambah.", cats,
					new String[] { "Skripsi" }, new double[][] { vals }, new String[] { HIJAU }));
		}
	}

	/** Membungkus satu potongan HTML grafik ke kartu putih ber-jarak lalu memasangnya ke dasbor. */
	private void tambahPanel(String htmlGrafik) {
		try {
			Div kartu = new Div();
			kartu.setStyle("background:#ffffff;border:1px solid #e2e8f0;border-radius:14px;padding:8px 10px;"
					+ "margin-bottom:12px;box-shadow:0 4px 6px -1px rgba(0,0,0,.05);");
			kartu.setParent(this);
			MyHtml h = new MyHtml(htmlGrafik);
			h.setParent(kartu);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Menghitung jumlah baris sebuah entitas (agregasi {@code rowCount}), 0 bila gagal. */
	@SuppressWarnings("rawtypes")
	private long hitung(Session session, Class cls) {
		try {
			Number n = (Number) session.createCriteria(cls).setProjection(Projections.rowCount()).uniqueResult();
			return n == null ? 0L : n.longValue();
		} catch (Exception e) {
			return 0L;
		}
	}

	/**
	 * Mengelompokkan jumlah baris menurut satu properti (agregasi {@code groupProperty + rowCount}),
	 * memakai alias opsional bila propertinya lewat relasi (mis. {@code mahasiswa.jurusan.nama}). Hasil
	 * {@link LinkedHashMap} (urut) berisi label &rarr; jumlah; label kosong diberi teks "(lainnya)".
	 *
	 * @param prop  properti yang dikelompokkan; bila memakai alias tulis {@code alias.field}.
	 * @param alias relasi yang perlu di-{@code createAlias} (mis. {@code "mahasiswa"} lalu {@code "m.jurusan"});
	 *              boleh {@code null} bila properti langsung di entitas.
	 * @param prodi {@code true} khusus pengelompokan per program studi (mahasiswa &rarr; jurusan).
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<String, Long> kelompokkan(Session session, Class cls, String prop, String alias, boolean prodi) {
		Map<String, Long> map = new LinkedHashMap<String, Long>();
		try {
			Criteria c = session.createCriteria(cls);
			if (prodi) {
				c.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN).createAlias("mahasiswa.jurusan", "jur",
						Criteria.LEFT_JOIN);
			} else if (alias != null) {
				c.createAlias(alias, alias, Criteria.LEFT_JOIN);
			}
			c.setProjection(Projections.projectionList().add(Projections.groupProperty(prop))
					.add(Projections.rowCount())).addOrder(prodi ? Order.asc(prop) : Order.asc(prop));
			List rows = c.list();
			for (Object o : rows) {
				Object[] r = (Object[]) o;
				String key = r[0] == null ? "(lainnya)" : r[0].toString().trim();
				if (key.length() == 0) {
					key = "(lainnya)";
				}
				long jml = ((Number) r[1]).longValue();
				Long lama = map.get(key);
				map.put(key, Long.valueOf((lama == null ? 0L : lama.longValue()) + jml));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardBimbinganTaSkripsi.java:258");
			// Bila properti/alias tidak cocok, kembalikan map kosong (panel akan dilewati) tanpa mengganggu lain.
		}
		return map;
	}

	/** Mengambil N entri dengan jumlah terbesar dari sebuah map (untuk membatasi batang agar tetap rapi). */
	private Map<String, Long> ambilTeratas(Map<String, Long> map, int n) {
		List<Map.Entry<String, Long>> list = new ArrayList<Map.Entry<String, Long>>(map.entrySet());
		java.util.Collections.sort(list, new java.util.Comparator<Map.Entry<String, Long>>() {
			@Override
			public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
				return b.getValue().compareTo(a.getValue());
			}
		});
		Map<String, Long> hasil = new LinkedHashMap<String, Long>();
		int i = 0;
		for (Map.Entry<String, Long> e : list) {
			if (i++ >= n) {
				break;
			}
			hasil.put(e.getKey(), e.getValue());
		}
		return hasil;
	}

	/** Ambil nilai map dengan aman (0 bila tak ada). */
	private long nilai(Map<String, Long> map, String key) {
		Long v = map.get(key);
		return v == null ? 0L : v.longValue();
	}

	/** Format angka ribuan sederhana. */
	private String fmt(long v) {
		return Common.numberFormat.get().format(v);
	}

	/**
	 * Membuat baris <b>kartu angka utama (KPI)</b> sebagai HTML/CSS murni (tanpa pustaka chart). Setiap
	 * kartu punya garis atas berwarna, angka besar, judul, dan keterangan kecil. Memakai
	 * {@code flex-wrap} sehingga otomatis melipat ke bawah di layar sempit (HP) &mdash; responsif tanpa
	 * media-query. Ditulis sendiri agar tidak bergantung pada varian {@code kpiCards} pustaka yang bisa
	 * berbeda antar-versi.
	 */
	private String kartuKpi(String[] labels, String[] nilai, String[] sub, String[] warna) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:flex;flex-wrap:wrap;gap:12px;margin-bottom:12px;'>");
		for (int i = 0; i < labels.length; i++) {
			String w = warna[i % warna.length];
			sb.append("<div style='flex:1 1 160px;min-width:150px;background:#ffffff;border:1px solid #e2e8f0;")
					.append("border-top:4px solid ").append(w)
					.append(";border-radius:12px;padding:12px 14px;box-shadow:0 4px 6px -1px rgba(0,0,0,.05);'>")
					.append("<div style='font-size:11px;color:#64748b;font-weight:700;text-transform:uppercase;")
					.append("letter-spacing:.04em;'>").append(esc(labels[i])).append("</div>")
					.append("<div style='font-size:28px;font-weight:800;line-height:1.1;margin:4px 0 2px;color:")
					.append(w).append(";'>").append(esc(nilai[i])).append("</div>")
					.append("<div style='font-size:11px;color:#94a3b8;'>").append(esc(sub[i])).append("</div>")
					.append("</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	/** Escape ringan agar teks aman dimasukkan ke HTML (mencegah tag/entity tak sengaja). */
	private String esc(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
