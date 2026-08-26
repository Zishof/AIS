package ais.action.master.pmb.statistik;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jenjang;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Kelas dasar untuk semua dasbor PMB.
 *
 * <p>Menyediakan:</p>
 * <ul>
 *   <li>Filter bar standar (Tahun Akademik, Semester, Muat Ulang)</li>
 *   <li>Siklus hidup {@code build → refresh → doRefresh}</li>
 *   <li>Metode utilitas bersama (format angka, HTML escape, layout helper)</li>
 * </ul>
 *
 * <p>Subclass hanya perlu mengimplementasikan {@link #doRefresh(Session, String, String)}
 * yang dipanggil setiap kali pengguna menekan "Muat Ulang" atau pertama kali tab dibuka.
 * Di dalam {@code doRefresh}, subclass menambahkan komponen ke {@link #contentHolder}
 * (untuk grafik HTML) dan/atau {@link #tableHolder} (untuk ZK Grid).</p>
 *
 * <p>Semua query menggunakan {@code HibernateUtil.currentSession()} yang dikelola Spring;
 * tidak perlu dan tidak boleh menutup session secara manual.</p>
 *
 * @see DashboardStatistikPmb
 * @see RekapPendaftarSpmb
 */
public abstract class DashboardPmbBase {

	// ═══════════════════════════════════════════════════════════════
	// Fields yang bisa diakses subclass
	// ═══════════════════════════════════════════════════════════════

	/** Perguruan tinggi konteks saat ini (bisa null = semua PT). */
	protected final PerguruanTinggi pt;

	/** Combobox Tahun Akademik di filter bar. */
	protected Combobox cboTA;

	/** Combobox Semester di filter bar. */
	protected Combobox cboSem;

	/** Combobox Jenjang Studi di filter bar seluruh dasbor PMB. */
	protected Combobox cboJenjang;

	/** Div tempat HTML chart ditempatkan; dibersihkan setiap Muat Ulang. */
	protected Div contentHolder;

	/** Div tempat ZK Grid / tabel ditempatkan; dibersihkan setiap Muat Ulang. */
	protected Div tableHolder;

	// ═══════════════════════════════════════════════════════════════
	// Konstruktor
	// ═══════════════════════════════════════════════════════════════

	protected DashboardPmbBase(PerguruanTinggi pt) {
		this.pt = pt;
	}

	// ═══════════════════════════════════════════════════════════════
	// API publik — siklus hidup
	// ═══════════════════════════════════════════════════════════════

	/**
	 * Membangun seluruh UI dasbor di dalam {@code container}.
	 * Memanggil metode ini lebih dari sekali menghapus isi lama dan membangun ulang.
	 *
	 * @param container  tabpanel tujuan.
	 * @param defaultTA  tahun akademik awal.
	 * @param defaultSem semester awal (null = semua).
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
		// Default seluruh dasbor turunan harus benar-benar "Semua". Item null
		// ditambahkan oleh insertComboDanSemua dan tidak selalu berada di index 0.
		Common.selectComboItem(cboJenjang, null);
		cboJenjang.setParent(filterBar);

		buildExtraFilter(filterBar);

		MyToolbarbuttonConfig btnRefresh = new MyToolbarbuttonConfig("Muat Ulang", "/img/refresh.gif");
		btnRefresh.setStyle("margin-left:6px;");
		btnRefresh.setParent(filterBar);
		btnRefresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				refresh();
			}
		});

		contentHolder = new Div();
		contentHolder.setParent(container);

		tableHolder = new Div();
		tableHolder.setStyle("margin-top:16px;");
		tableHolder.setParent(container);

		refresh();
	}

	/**
	 * Hook untuk subclass yang ingin menambahkan filter tambahan di filter bar.
	 * Default implementasi tidak melakukan apa-apa.
	 *
	 * @param filterBar Div filter bar yang sudah berisi TA + Sem.
	 */
	protected void buildExtraFilter(Div filterBar) {
		// default: tidak ada filter tambahan
	}

	/**
	 * Membangun ulang seluruh konten berdasarkan filter aktif.
	 * Dipanggil otomatis saat {@link #build} dan setiap klik "Muat Ulang".
	 */
	public void refresh() {
		if (contentHolder == null) {
			return;
		}
		Common.clear(contentHolder);
		Common.clear(tableHolder);

		String ta  = getSelectedTA();
		String sem = getSelectedSem();

		Session session = HibernateUtil.currentSession();
		doRefresh(session, ta, sem);
	}

	/**
	 * Metode utama yang harus diimplementasikan oleh setiap dasbor.
	 * Dipanggil dari {@link #refresh()} dengan session aktif dan filter terpilih.
	 * Subclass harus menambahkan komponen ke {@link #contentHolder} dan/atau
	 * {@link #tableHolder}.
	 *
	 * @param session sesi Hibernate aktif (Spring-managed, jangan ditutup).
	 * @param ta      tahun akademik terpilih.
	 * @param sem     semester terpilih (bisa null).
	 */
	protected abstract void doRefresh(Session session, String ta, String sem);

	// ═══════════════════════════════════════════════════════════════
	// Filter helpers
	// ═══════════════════════════════════════════════════════════════

	/** Mengembalikan label TA yang dipilih di combobox. */
	protected String getSelectedTA() {
		if (cboTA != null && cboTA.getSelectedItem() != null
				&& cboTA.getSelectedItem().getValue() != null) {
			return cboTA.getSelectedItem().getLabel();
		}
		return Common.getCurrentTahunAkademik();
	}

	/** Mengembalikan nilai semester yang dipilih, atau null jika "Semua". */
	protected String getSelectedSem() {
		if (cboSem != null && cboSem.getSelectedItem() != null
				&& cboSem.getSelectedItem().getValue() != null) {
			return (String) cboSem.getSelectedItem().getValue();
		}
		return null;
	}

	/** Membangun combobox Semester dengan pilihan Semua / Ganjil / Genap. */
	protected Combobox buildSemesterCombo(String defaultSem) {
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
	// HQL helpers
	// ═══════════════════════════════════════════════════════════════

	/** Menghasilkan klausa HQL "AND bcm.semesterMulai = :sem " bila semester dipilih. */
	protected String semClause(String sem) {
		return hasSem(sem) ? "AND bcm.semesterMulai = :sem " : "";
	}

	/** True jika filter semester terisi. */
	protected boolean hasSem(String sem) {
		return sem != null && sem.trim().length() > 0;
	}

	/** Menetapkan parameter :sem pada query HQL bila diperlukan. */
	protected void applySemParam(org.hibernate.Query q, String sem) {
		if (hasSem(sem)) {
			q.setParameter("sem", sem);
		}
	}

	/** Jenjang terpilih, atau null untuk seluruh jenjang. */
	protected Jenjang getSelectedJenjang() {
		if (cboJenjang != null && cboJenjang.getSelectedItem() != null
				&& cboJenjang.getSelectedItem().getValue() instanceof Jenjang) {
			return (Jenjang) cboJenjang.getSelectedItem().getValue();
		}
		return null;
	}

	/** Klausa filter jenjang untuk query dengan alias BiodataCalonMahasiswa {@code bcm}. */
	protected String jenjangClause() {
		return jenjangClause(getSelectedJenjang());
	}

	protected String jenjangClause(Jenjang jenjang) {
		return jenjang == null ? "" : "AND bcm.jenjang = :jenjang ";
	}

	protected void applyJenjangParam(org.hibernate.Query q) {
		applyJenjangParam(q, getSelectedJenjang());
	}

	protected void applyJenjangParam(org.hibernate.Query q, Jenjang jenjang) {
		if (jenjang != null) {
			q.setParameter("jenjang", jenjang);
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// Layout builders (HTML helper)
	// ═══════════════════════════════════════════════════════════════

	/** Bungkus HTML dalam wrapper dengan margin atas. */
	protected static String fullWidth(String html) {
		return "<div style=\"margin-top:14px;\">" + html + "</div>";
	}

	/** Susun dua blok HTML dalam grid 2 kolom responsif. */
	protected static String grid2col(String left, String right) {
		return "<div style=\"display:grid;grid-template-columns:"
				+ "repeat(auto-fit,minmax(280px,1fr));gap:14px;margin-top:14px;\">"
				+ left + right + "</div>";
	}

	/** Susun tiga blok HTML dalam grid 3 kolom responsif. */
	protected static String grid3col(String a, String b, String c) {
		return "<div style=\"display:grid;grid-template-columns:"
				+ "repeat(auto-fit,minmax(240px,1fr));gap:14px;margin-top:14px;\">"
				+ a + b + c + "</div>";
	}

	/** Kartu kosong dengan pesan bila tidak ada data. */
	protected static String emptyCard(String judul, String pesan) {
		return "<div class=\"ais-chart-card\"><div class=\"ais-chart-title\">"
				+ escHtml(judul) + "</div><div style=\"color:#9ca3af;padding:12px 0;font-size:13px;\">"
				+ escHtml(pesan) + "</div></div>";
	}

	// ═══════════════════════════════════════════════════════════════
	// Format & string utilities
	// ═══════════════════════════════════════════════════════════════

	/** Format bilangan bulat dengan pemisah ribuan. */
	protected static String fmtAngka(int n) {
		return NumberFormat.getIntegerInstance().format(n);
	}

	/** Format desimal satu angka di belakang koma. */
	protected static String fmt1(double d) {
		return new DecimalFormat("0.#").format(d);
	}

	/** Menghitung persentase num/denom dalam format "12.5%". */
	protected static String persen(int num, int denom) {
		if (denom <= 0) { return "0%"; }
		return fmt1(num * 100.0 / denom) + "%";
	}

	/** Mengekstrak tahun awal dari format "YYYY/YYYY+1". */
	protected static String extractTahunAwal(String ta) {
		if (ta == null) { return ""; }
		int idx = ta.indexOf('/');
		return idx > 0 ? ta.substring(0, idx).trim() : ta;
	}

	/** Menghitung tahun akademik sebelumnya. "2026/2027" → "2025/2026". */
	protected static String getPreviousTA(String ta) {
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

	/** Singkat teks ke maks karakter dengan "…". */
	protected static String abbrev(String s, int max) {
		if (s == null || s.length() <= max) { return s == null ? "" : s; }
		return s.substring(0, max - 1) + "…";
	}

	/** HTML-encode karakter berbahaya. */
	protected static String escHtml(String s) {
		if (s == null) { return ""; }
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
		        .replace("\"", "&quot;");
	}

	/** Catat error ke stderr dengan prefix class name. */
	protected void logErr(String ctx, Exception e) {
		System.err.println("[" + getClass().getSimpleName() + "] " + ctx
				+ " — " + e.getMessage());
	}
}
