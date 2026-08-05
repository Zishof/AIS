package ais.action.master.helper;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.ss.usermodel.Font;
import org.zkoss.poi.ss.usermodel.Row;
import org.zkoss.poi.ss.usermodel.Sheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>DashboardReportKit — Mesin Laporan &amp; Ekspor Dasbor yang Dapat Dipakai Ulang</h1>
 *
 * <p><b>Untuk apa kelas ini?</b> Hampir semua dasbor/statistik di aplikasi ini butuh dua hal yang
 * sama: (1) tombol <b>Cetak Laporan Lengkap</b> yang membuka laporan siap-cetak, dan (2) tombol
 * <b>Ekspor Excel</b> yang mengunduh datanya. Sebelumnya setiap dasbor menuliskan sendiri kode
 * pembuatan HTML, grafik, progress bar, dan penulisan Excel-nya — berulang, boros, dan rawan beda
 * perilaku. {@code DashboardReportKit} menyatukan semua itu ke SATU tempat: sebuah dasbor cukup
 * mendeskripsikan <i>apa</i> isinya (judul, penjelasan, dan daftar {@link Bagian}), lalu memanggil
 * {@link #pasangTombol(Component, Component, SumberLaporan)} — dan otomatis mendapat kedua tombol,
 * lengkap dengan <b>progress bar bertahap</b>, <b>grafik HTML/CSS modern</b> (batang, donut,
 * spider/radar, tren garis, dan kartu KPI — TANPA JFreeChart), tata letak <b>responsif</b> untuk
 * layar desktop maupun ponsel, serta berkas Excel multi-sheet.</p>
 *
 * <h2>Filosofi desain</h2>
 * <ol>
 *   <li><b>Data-driven &amp; reuse maksimal.</b> Dasbor tidak menulis HTML/Excel; ia hanya
 *       menyediakan {@link SumberLaporan} — deskripsi terstruktur. Mesin yang merender. Menambah
 *       laporan ke dasbor baru = beberapa baris, bukan ratusan.</li>
 *   <li><b>Hemat memori.</b> Data tiap {@link Bagian} diambil <i>malas</i> (lazy) lewat
 *       {@link PenyediaBaris} tepat pada tahap progress-nya, bukan sekaligus di awal. Untuk Excel,
 *       data diambil ulang per-sheet sehingga tidak ada koleksi raksasa yang ditahan lama di heap.
 *       {@link StringBuilder} dipesan (pre-size) agar tidak realokasi berulang. Tidak ada gambar
 *       server (BufferedImage/JFreeChart) yang memakan heap — semua grafik berupa teks SVG/CSS.</li>
 *   <li><b>Progress yang jujur.</b> Bila semua query dijalankan dalam satu request, peramban membeku
 *       tanpa umpan balik. Mesin memakai pola ZK {@code Events.echoEvent}: tiap tahap = satu putaran
 *       ke server sehingga klien benar-benar merender kenaikan persen di antara langkah — persen
 *       nyata, bukan animasi palsu.</li>
 *   <li><b>Aman terhadap Session.</b> Pengambilan data memakai {@link ais.common.Common#ambilSql(String)}
 *       yang bertumpu pada {@code currentSession()} — sesi ini ditutup otomatis oleh kerangka kerja,
 *       maka <b>JANGAN</b> ditutup manual. Bila sebuah {@link PenyediaBaris} membuka sesinya sendiri
 *       lewat {@code openSession()} atau {@code currentNativeSession()}, sesi itu WAJIB ditutup di
 *       blok {@code finally} milik penyedia tersebut (di luar tanggung jawab kit ini).</li>
 *   <li><b>Ramah pengguna awam.</b> Tiap {@link Bagian} memikul {@code deskripsi} satu-dua kalimat
 *       dengan bahasa paling sederhana (hindari istilah teknis) supaya pengguna non-IT paham fungsi
 *       tiap panel.</li>
 *   <li><b>Kompatibel Java 1.7.</b> Tanpa lambda/stream; memakai kelas anonim. Blok {@code try/catch}
 *       memakai gaya 1.6 (menangkap {@code Exception}). Aman dikompilasi pada rantai lama.</li>
 * </ol>
 *
 * <h2>Cara pakai (adopsi 1 baris di dasbor mana pun)</h2>
 * <pre>
 * DashboardReportKit.pasangTombol(toolbar, this, new DashboardReportKit.SumberLaporan() {
 *     public String judul()     { return "Ringkasan Pemakaian Memori"; }
 *     public String subjudul()  { return "Server " + namaServer; }
 *     public String deskripsi() { return "Melihat seberapa berat kerja server dan kapan paling sibuk."; }
 *     public java.util.List&lt;DashboardReportKit.Bagian&gt; bagian() {
 *         java.util.List&lt;DashboardReportKit.Bagian&gt; b = new java.util.ArrayList&lt;DashboardReportKit.Bagian&gt;();
 *         b.add(DashboardReportKit.kpi("Ringkasan", "Angka penting sekilas.", pengambilKpi));
 *         b.add(DashboardReportKit.garis("Tren Pemakaian", "Naik-turun pemakaian dari waktu ke waktu.",
 *                 new String[]{"Waktu","Terpakai (MB)"}, pengambilTren));
 *         return b;
 *     }
 * });
 * </pre>
 *
 * <h2>Jenis panel/grafik yang didukung</h2>
 * <ul>
 *   <li>{@link JenisChart#KPI} — deretan kartu angka penting (baris = {label, nilai, keterangan}).</li>
 *   <li>{@link JenisChart#TABEL} — tabel rinci (semua kolom ditampilkan).</li>
 *   <li>{@link JenisChart#BATANG} — batang horizontal (baris = {label, nilai}); cocok untuk peringkat.</li>
 *   <li>{@link JenisChart#DONUT} — cincin komposisi (baris = {label, nilai}); cocok untuk proporsi.</li>
 *   <li>{@link JenisChart#RADAR} — jaring laba-laba/spider (baris = {label, nilai}); cocok untuk
 *       membandingkan beberapa aspek sekaligus.</li>
 *   <li>{@link JenisChart#GARIS} — tren garis (baris = {waktu, nilai} berurut); cocok untuk perubahan
 *       antar waktu.</li>
 * </ul>
 *
 * <h2>Panduan memilih grafik (best practice)</h2>
 * <p>Perubahan antar waktu → GARIS. Peringkat/perbandingan kategori → BATANG. Proporsi dari
 * keseluruhan → DONUT. Banyak aspek dinilai serentak (mis. kesiapan data) → RADAR. Angka tunggal
 * penting → KPI. Detail baris demi baris → TABEL. Kombinasikan beberapa Bagian untuk cerita utuh:
 * KPI di atas (ringkas), lalu tren, lalu rincian.</p>
 *
 * @author AIS
 * @see LaporanKunjunganPenggunaHelper Contoh implementasi lengkap dengan data log_login.
 */
public final class DashboardReportKit {

	private DashboardReportKit() {
	}

	// =====================================================================================
	// MODEL — dideskripsikan oleh dasbor, dirender oleh kit
	// =====================================================================================

	/** Jenis visual sebuah {@link Bagian}. */
	public enum JenisChart {
		KPI, TABEL, BATANG, DONUT, RADAR, GARIS
	}

	/** Pemasok baris data secara malas (lazy) — dipanggil kit tepat pada tahap progress-nya. */
	public interface PenyediaBaris {
		/** @return daftar baris; tiap baris {@code Object[]}. Boleh kosong, tak boleh melempar (bungkus sendiri). */
		List<Object[]> ambil();
	}

	/** Deskripsi satu panel laporan (judul + penjelasan sederhana + jenis grafik + data). */
	public static final class Bagian {
		final String judul;
		final String deskripsi;
		final JenisChart jenis;
		final String[] kolom;
		final PenyediaBaris penyedia;

		public Bagian(String judul, String deskripsi, JenisChart jenis, String[] kolom, PenyediaBaris penyedia) {
			this.judul = judul;
			this.deskripsi = deskripsi == null ? "" : deskripsi;
			this.jenis = jenis == null ? JenisChart.TABEL : jenis;
			this.kolom = kolom == null ? new String[0] : kolom;
			this.penyedia = penyedia;
		}
	}

	/** Kontrak yang diimplementasikan tiap dasbor untuk memberi tahu kit isi laporannya. */
	public interface SumberLaporan {
		/** Judul besar laporan (mis. "Ringkasan Pemakaian Memori"). */
		String judul();

		/** Subjudul/konteks singkat (mis. nama server, unit). Boleh kosong. */
		String subjudul();

		/** Satu kalimat sederhana: kegunaan laporan bagi pengguna awam. */
		String deskripsi();

		/** Daftar panel laporan, berurutan tampil. */
		List<Bagian> bagian();
	}

	// ---- pabrik Bagian (ringkas dipakai dasbor) ----

	public static Bagian kpi(String judul, String deskripsi, PenyediaBaris p) {
		return new Bagian(judul, deskripsi, JenisChart.KPI, new String[] { "Metrik", "Nilai" }, p);
	}

	public static Bagian tabel(String judul, String deskripsi, String[] kolom, PenyediaBaris p) {
		return new Bagian(judul, deskripsi, JenisChart.TABEL, kolom, p);
	}

	public static Bagian batang(String judul, String deskripsi, String[] kolom, PenyediaBaris p) {
		return new Bagian(judul, deskripsi, JenisChart.BATANG, kolom, p);
	}

	public static Bagian donut(String judul, String deskripsi, String[] kolom, PenyediaBaris p) {
		return new Bagian(judul, deskripsi, JenisChart.DONUT, kolom, p);
	}

	public static Bagian radar(String judul, String deskripsi, String[] kolom, PenyediaBaris p) {
		return new Bagian(judul, deskripsi, JenisChart.RADAR, kolom, p);
	}

	public static Bagian garis(String judul, String deskripsi, String[] kolom, PenyediaBaris p) {
		return new Bagian(judul, deskripsi, JenisChart.GARIS, kolom, p);
	}

	// =====================================================================================
	// PEMASANGAN TOMBOL
	// =====================================================================================

	/**
	 * Memasang tombol <b>Cetak Laporan Lengkap</b> + <b>Ekspor Excel</b> ke {@code parent}
	 * (biasanya sebuah {@code Toolbar}). Data diambil saat diklik (mengikuti filter terbaru dasbor).
	 *
	 * @param parent komponen wadah tombol (Toolbar/Hbox/Div).
	 * @param owner  komponen mana pun yang terpasang ke Page (untuk menampilkan popup).
	 * @param sumber sumber data laporan milik dasbor.
	 */
	public static void pasangTombol(final Component parent, final Component owner, final SumberLaporan sumber) {
		final MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak Laporan Lengkap", "/img/print.png");
		cetak.setTooltiptext("Buka laporan lengkap yang siap dicetak atau disimpan sebagai PDF");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				mulaiCetak(owner, sumber);
			}
		});
		cetak.setParent(parent);

		final MyToolbarbuttonConfig excel = new MyToolbarbuttonConfig("Ekspor Excel", "/img/excel.png");
		excel.setTooltiptext("Unduh data laporan sebagai berkas Excel (.xlsx)");
		excel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				try { Clients.showBusy("Menyiapkan berkas Excel, mohon tunggu..."); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/DashboardReportKit.java:212");}
				Events.echoEvent("onKitProsesExcel", excel, null);
			}
		});
		excel.addEventListener("onKitProsesExcel", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				try {
					eksporExcel(sumber);
				} finally {
					try { Clients.clearBusy(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/DashboardReportKit.java:222");}
				}
			}
		});
		excel.setParent(parent);
	}

	/**
	 * Buka LANGSUNG laporan lengkap (grafik HTML/CSS + tabel, siap dicetak/disimpan sebagai PDF via
	 * dialog cetak peramban) TANPA memasang tombol. Berguna bila dasbor sudah punya tombolnya sendiri
	 * dan hanya ingin memicu laporan bergrafik dari handler-nya.
	 *
	 * @param owner  komponen mana pun yang terpasang ke Page (untuk menampilkan popup laporan).
	 * @param sumber sumber data laporan.
	 */
	public static void bukaLaporan(final Component owner, final SumberLaporan sumber) {
		mulaiCetak(owner, sumber);
	}

	// =====================================================================================
	// CETAK (progress bertahap via echo-chain)
	// =====================================================================================

	/** Wadah state antar-tahap cetak. */
	private static final class Konteks {
		Component owner;
		SumberLaporan sumber;
		List<Bagian> bagian;
		List<List<Object[]>> hasil;
		MyWindow progressWin;
		org.zkoss.zul.Html bar;
		Component driver;
	}

	private static void mulaiCetak(final Component owner, final SumberLaporan sumber) {
		final Konteks k = new Konteks();
		k.owner = owner;
		k.sumber = sumber;
		k.bagian = amanBagian(sumber);
		k.hasil = new ArrayList<List<Object[]>>();

		MyWindow pw = new MyWindow("Menyusun Laporan", "normal", false);
		pw.setWidth("440px");
		pw.setContentStyle("background:#fff;");
		org.zkoss.zul.Html bar = new org.zkoss.zul.Html();
		bar.setParent(pw);
		k.progressWin = pw;
		k.bar = bar;
		updateBar(k, 6, "Menyiapkan data");
		pw.setPage(owner.getPage());
		pw.doHighlighted();

		final org.zkoss.zul.Div driver = new org.zkoss.zul.Div();
		driver.setVisible(false);
		driver.setParent(pw);
		k.driver = driver;
		driver.addEventListener("onKitStage", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				jalankanStage(k, Integer.parseInt(String.valueOf(e.getData())));
			}
		});
		Events.echoEvent("onKitStage", driver, "0");
	}

	/** Tahap {@code i} (0..N-1) mengambil data Bagian ke-i; tahap N merender &amp; menampilkan. */
	private static void jalankanStage(Konteks k, int i) {
		int n = k.bagian.size();
		if (i < n) {
			Bagian b = k.bagian.get(i);
			List<Object[]> baris = null;
			try {
				baris = b.penyedia == null ? null : b.penyedia.ambil();
			} catch (Exception ex) {
				try { Common.tampilErrorJikaAdmin(ex); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/DashboardReportKit.java:284");}
			}
			k.hasil.add(baris == null ? new ArrayList<Object[]>() : baris);
			int pct = n == 0 ? 95 : (int) Math.min(95, 6 + Math.round((i + 1) * 89.0 / n));
			updateBar(k, pct, "Menyusun: " + b.judul);
			Events.echoEvent("onKitStage", k.driver, String.valueOf(i + 1));
			return;
		}
		// tahap akhir: render + tampilkan
		updateBar(k, 100, "Menyelesaikan laporan");
		String html;
		try {
			html = renderHtml(k.sumber, k.bagian, k.hasil);
		} catch (Exception ex) {
			try { Common.tampilErrorJikaAdmin(ex); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/DashboardReportKit.java:298");}
			html = "<html><body style='font-family:sans-serif;padding:30px;'>Gagal menyusun laporan.</body></html>";
		}
		try { k.progressWin.detach(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/DashboardReportKit.java:301");}
		bukaModal(k.owner, k.sumber.judul(), html);
	}

	private static void updateBar(Konteks k, int pct, String label) {
		if (k == null || k.bar == null) {
			return;
		}
		String h = "<div style='padding:18px 20px;font-family:Segoe UI,Roboto,Arial,sans-serif;min-width:360px;'>"
				+ "<div style='font-size:13px;font-weight:800;color:#0f172a;'>Menyusun Laporan</div>"
				+ "<div style='font-size:11.5px;color:#475569;margin:4px 0 10px;'>" + esc(label) + " ...</div>"
				+ "<div style='height:14px;background:#e2e8f0;border-radius:999px;overflow:hidden;'>"
				+ "<div style='height:14px;width:" + pct + "%;background:linear-gradient(90deg,#0e7490,#2563eb);border-radius:999px;'></div>"
				+ "</div>"
				+ "<div style='text-align:right;font-size:12px;font-weight:800;color:#0e7490;margin-top:6px;'>" + pct + "%</div>"
				+ "</div>";
		try { k.bar.setContent(h); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/DashboardReportKit.java:317");}
	}

	private static void bukaModal(Component owner, String judul, String html) {
		MyWindow w = new MyWindow("Laporan - " + (judul == null ? "" : judul), "normal", true);
		w.setWidth("96%");
		w.setHeight("96%");
		try { w.setMaximizable(true); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/DashboardReportKit.java:324");}
		try { w.setSizable(true); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/DashboardReportKit.java:325");}
		w.setContentStyle("overflow:hidden;background:#fff;");
		w.setPage(owner.getPage());

		Toolbar tb = new Toolbar();
		tb.setStyle("border:0;background:#f8fafc;padding:6px 8px;");
		tb.setParent(w);

		final Iframe frame = new Iframe();
		frame.setWidth("100%");
		frame.setHeight("100%");
		frame.setStyle("border:0;background:#fff;");
		try {
			frame.setContent(new AMedia("laporan.html", "html", "text/html;charset=UTF-8", html.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			frame.setContent(new AMedia("laporan.html", "html", "text/html", html));
		}

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak / Simpan PDF", "/img/print.png");
		cetak.setParent(tb);
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				Clients.evalJavaScript("var f=document.getElementById('" + frame.getUuid() + "');"
						+ "if(f&&f.contentWindow){f.contentWindow.focus();f.contentWindow.print();}");
			}
		});

		MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		tutup.setParent(tb);
		final MyWindow ref = w;
		tutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				ref.detach();
			}
		});

		frame.setParent(w);
		w.doHighlighted();
	}

	// =====================================================================================
	// RENDER HTML (responsif + grafik HTML/CSS modern)
	// =====================================================================================

	private static String renderHtml(SumberLaporan s, List<Bagian> bagian, List<List<Object[]>> hasil) {
		StringBuilder h = new StringBuilder(60000);
		SimpleDateFormat cap = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		h.append("<!DOCTYPE html><html lang='id'><head><meta charset='UTF-8'/>");
		h.append("<meta name='viewport' content='width=device-width, initial-scale=1'/>");
		h.append("<title>").append(esc(s.judul())).append("</title><style>").append(css()).append("</style></head><body>");

		h.append("<div class='kop'>");
		h.append("<div class='kop-eyebrow'>LAPORAN DASBOR</div>");
		h.append("<div class='kop-judul'>").append(esc(s.judul())).append("</div>");
		String sub = s.subjudul();
		if (sub != null && sub.trim().length() > 0) {
			h.append("<div class='kop-sub'>").append(esc(sub)).append("</div>");
		}
		String desk = s.deskripsi();
		if (desk != null && desk.trim().length() > 0) {
			h.append("<div class='kop-desk'>").append(esc(desk)).append("</div>");
		}
		h.append("<div class='kop-cap'>Dicetak ").append(cap.format(ais.ui.util.WaktuUtil.getDate())).append("</div>");
		h.append("</div>");

		int no = 0;
		for (int i = 0; i < bagian.size(); i++) {
			Bagian b = bagian.get(i);
			List<Object[]> rows = i < hasil.size() ? hasil.get(i) : new ArrayList<Object[]>();
			no++;
			h.append("<section class='panel'>");
			h.append("<div class='panel-head'><span class='panel-no'>").append(no).append("</span>")
			 .append("<span class='panel-title'>").append(esc(b.judul)).append("</span></div>");
			if (b.deskripsi.length() > 0) {
				h.append("<div class='panel-desc'>").append(esc(b.deskripsi)).append("</div>");
			}
			if (rows == null || rows.isEmpty()) {
				h.append("<div class='kosong'>Belum ada data untuk ditampilkan.</div>");
			} else {
				renderBagian(h, b, rows);
			}
			h.append("</section>");
		}

		h.append("<div class='ftr'>Dihasilkan otomatis oleh Sistem Informasi &middot; ")
		 .append(esc(cap.format(ais.ui.util.WaktuUtil.getDate()))).append("</div>");
		h.append("</body></html>");
		return h.toString();
	}

	private static void renderBagian(StringBuilder h, Bagian b, List<Object[]> rows) {
		switch (b.jenis) {
			case KPI:    renderKpi(h, rows); break;
			case BATANG: renderBatang(h, rows); break;
			case DONUT:  renderDonut(h, rows); break;
			case RADAR:  renderRadar(h, rows); break;
			case GARIS:  renderGaris(h, rows); break;
			case TABEL:
			default:     renderTabel(h, b.kolom, rows); break;
		}
	}

	private static void renderKpi(StringBuilder h, List<Object[]> rows) {
		h.append("<div class='kpi-grid'>");
		for (Object[] r : rows) {
			String label = r.length > 0 ? S(r[0]) : "";
			String nilai = r.length > 1 ? S(r[1]) : "";
			String help = r.length > 2 ? S(r[2]) : "";
			h.append("<div class='kpi'><div class='kpi-label'>").append(esc(label)).append("</div>")
			 .append("<div class='kpi-val'>").append(esc(nilai)).append("</div>");
			if (help.length() > 0) {
				h.append("<div class='kpi-help'>").append(esc(help)).append("</div>");
			}
			h.append("</div>");
		}
		h.append("</div>");
	}

	private static void renderTabel(StringBuilder h, String[] kolom, List<Object[]> rows) {
		h.append("<div class='tbl-wrap'><table class='tbl'><thead><tr>");
		int cols = kolom.length > 0 ? kolom.length : (rows.isEmpty() ? 0 : rows.get(0).length);
		for (int i = 0; i < cols; i++) {
			h.append("<th>").append(i < kolom.length ? esc(kolom[i]) : "").append("</th>");
		}
		h.append("</tr></thead><tbody>");
		for (Object[] r : rows) {
			h.append("<tr>");
			for (int i = 0; i < cols; i++) {
				Object v = i < r.length ? r[i] : null;
				boolean angka = v instanceof Number;
				h.append("<td class='").append(angka ? "r" : "").append("'>")
				 .append(angka ? fmt(((Number) v).longValue()) : esc(S(v))).append("</td>");
			}
			h.append("</tr>");
		}
		h.append("</tbody></table></div>");
	}

	private static void renderBatang(StringBuilder h, List<Object[]> rows) {
		long maks = 1;
		for (Object[] r : rows) maks = Math.max(maks, r.length > 1 ? L(r[1]) : 0);
		h.append("<div class='barchart'>");
		for (Object[] r : rows) {
			long v = r.length > 1 ? L(r[1]) : 0;
			h.append("<div class='bc-row'><div class='bc-lbl'>").append(esc(r.length > 0 ? S(r[0]) : "")).append("</div>")
			 .append("<div class='bc-track'><div class='bc-fill' style='width:").append(pctNum(v, maks)).append("%;'></div></div>")
			 .append("<div class='bc-val'>").append(fmt(v)).append("</div></div>");
		}
		h.append("</div>");
	}

	private static void renderDonut(StringBuilder h, List<Object[]> rows) {
		long total = 0;
		for (Object[] r : rows) total += r.length > 1 ? L(r[1]) : 0;
		String[] palet = { "#0e7490", "#2563eb", "#7c3aed", "#0891b2", "#059669", "#d97706", "#dc2626", "#64748b" };
		StringBuilder grad = new StringBuilder();
		double akum = 0;
		for (int i = 0; i < rows.size(); i++) {
			long v = rows.get(i).length > 1 ? L(rows.get(i)[1]) : 0;
			double mulai = total <= 0 ? 0 : akum * 360.0 / total;
			akum += v;
			double selesai = total <= 0 ? 0 : akum * 360.0 / total;
			if (grad.length() > 0) grad.append(",");
			grad.append(palet[i % palet.length]).append(" ").append(fmt2(mulai)).append("deg ").append(fmt2(selesai)).append("deg");
		}
		if (grad.length() == 0) grad.append("#e2e8f0 0deg 360deg");
		h.append("<div class='donut-wrap'><div class='donut' style='background:conic-gradient(").append(grad)
		 .append(");'><div class='donut-hole'></div></div><div class='donut-legend'>");
		for (int i = 0; i < rows.size(); i++) {
			Object[] r = rows.get(i);
			long v = r.length > 1 ? L(r[1]) : 0;
			h.append("<div><span class='dot' style='background:").append(palet[i % palet.length]).append(";'></span>")
			 .append(esc(r.length > 0 ? S(r[0]) : "")).append(": <b>").append(fmt(v)).append("</b> (").append(pct(v, total)).append(")</div>");
		}
		h.append("</div></div>");
	}

	private static void renderRadar(StringBuilder h, List<Object[]> rows) {
		int n = rows.size();
		if (n < 3) { renderBatang(h, rows); return; }
		long maks = 1;
		for (Object[] r : rows) maks = Math.max(maks, r.length > 1 ? L(r[1]) : 0);
		int cx = 160, cy = 150, R = 110;
		StringBuilder poly = new StringBuilder();
		StringBuilder axis = new StringBuilder();
		StringBuilder labels = new StringBuilder();
		for (int i = 0; i < n; i++) {
			double ang = -Math.PI / 2 + i * 2 * Math.PI / n;
			long v = rows.get(i).length > 1 ? L(rows.get(i)[1]) : 0;
			double rr = R * Math.min(1.0, (double) v / maks);
			double px = cx + rr * Math.cos(ang), py = cy + rr * Math.sin(ang);
			double ax = cx + R * Math.cos(ang), ay = cy + R * Math.sin(ang);
			double lx = cx + (R + 16) * Math.cos(ang), ly = cy + (R + 16) * Math.sin(ang);
			poly.append(fmt2(px)).append(",").append(fmt2(py)).append(" ");
			axis.append("<line x1='").append(cx).append("' y1='").append(cy).append("' x2='").append(fmt2(ax))
			    .append("' y2='").append(fmt2(ay).trim()).append("' stroke='#e2e8f0' stroke-width='1'/>");
			labels.append("<text x='").append(fmt2(lx)).append("' y='").append(fmt2(ly))
			      .append("' font-size='9' fill='#64748b' text-anchor='middle'>").append(esc(S(rows.get(i)[0]))).append("</text>");
		}
		for (int ring = 1; ring <= 3; ring++) {
			axis.append("<circle cx='").append(cx).append("' cy='").append(cy).append("' r='").append(R * ring / 3)
			    .append("' fill='none' stroke='#eef2f7' stroke-width='1'/>");
		}
		h.append("<div style='text-align:center;'><svg viewBox='0 0 320 300' style='max-width:340px;width:100%;'>")
		 .append(axis).append("<polygon points='").append(poly)
		 .append("' fill='rgba(37,99,235,.18)' stroke='#2563eb' stroke-width='2'/>").append(labels).append("</svg></div>");
	}

	private static void renderGaris(StringBuilder h, List<Object[]> rows) {
		int n = rows.size();
		long maks = 1;
		for (Object[] r : rows) maks = Math.max(maks, r.length > 1 ? L(r[1]) : 0);
		int w = 640, hgt = 180, pad = 8;
		StringBuilder pts = new StringBuilder();
		for (int i = 0; i < n; i++) {
			long v = rows.get(i).length > 1 ? L(rows.get(i)[1]) : 0;
			double x = n <= 1 ? pad : pad + i * (double) (w - 2 * pad) / (n - 1);
			double y = hgt - pad - (hgt - 2 * pad) * ((double) v / maks);
			pts.append(fmt2(x)).append(",").append(fmt2(y)).append(" ");
		}
		h.append("<div class='tbl-wrap'><svg viewBox='0 0 ").append(w).append(" ").append(hgt)
		 .append("' preserveAspectRatio='none' style='width:100%;height:180px;'>")
		 .append("<polyline points='").append(pts).append("' fill='none' stroke='#0e7490' stroke-width='2'/>")
		 .append("</svg></div>");
		// label ringkas awal & akhir
		if (n > 0) {
			h.append("<div class='note'>Dari <b>").append(esc(S(rows.get(0)[0]))).append("</b> sampai <b>")
			 .append(esc(S(rows.get(n - 1)[0]))).append("</b> &middot; nilai tertinggi ").append(fmt(maks)).append("</div>");
		}
	}

	// =====================================================================================
	// EKSPOR EXCEL (multi-sheet, data diambil ulang per-sheet demi hemat memori)
	// =====================================================================================

	private static void eksporExcel(SumberLaporan s) {
		try {
			List<Bagian> bagian = amanBagian(s);
			XSSFWorkbook wb = new XSSFWorkbook();
			CellStyle head = headStyle(wb);
			CellStyle title = titleStyle(wb);
			SimpleDateFormat cap = new SimpleDateFormat("dd-MM-yyyy HH:mm");
			String meta = (s.subjudul() == null ? "" : s.subjudul() + "  |  ") + "Dicetak " + cap.format(ais.ui.util.WaktuUtil.getDate());

			if (bagian.isEmpty()) {
				Sheet sh = wb.createSheet("Laporan");
				titleRow(sh, 0, title, s.judul());
			}
			for (int i = 0; i < bagian.size(); i++) {
				Bagian b = bagian.get(i);
				List<Object[]> rows;
				try {
					rows = b.penyedia == null ? new ArrayList<Object[]>() : b.penyedia.ambil();
				} catch (Exception ex) {
					rows = new ArrayList<Object[]>();
				}
				Sheet sh = wb.createSheet(namaSheetAman(b.judul, i, wb));
				titleRow(sh, 0, title, b.judul);
				titleRow(sh, 1, null, meta);
				int cols = b.kolom.length > 0 ? b.kolom.length : (rows.isEmpty() ? 0 : rows.get(0).length);
				Row hr = sh.createRow(3);
				for (int c = 0; c < cols; c++) {
					Cell cell = hr.createCell(c);
					cell.setCellValue(c < b.kolom.length ? b.kolom[c] : ("Kolom " + (c + 1)));
					cell.setCellStyle(head);
				}
				int ri = 4;
				for (Object[] r : rows) {
					Row row = sh.createRow(ri++);
					for (int c = 0; c < cols; c++) {
						Object v = c < r.length ? r[c] : null;
						Cell cell = row.createCell(c);
						if (v instanceof Number) {
							cell.setCellValue(((Number) v).doubleValue());
						} else {
							cell.setCellValue(v == null ? "" : v.toString());
						}
					}
				}
				try {
					for (int c = 0; c < cols; c++) sh.autoSizeColumn(c);
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/DashboardReportKit.java:608");}
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			wb.write(baos);
			String fn = bersih(s.judul()) + "_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(ais.ui.util.WaktuUtil.getDate()) + ".xlsx";
			Filedownload.save(baos.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fn);
		} catch (Exception e) {
			try { Common.tampilErrorJikaAdmin(e); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/DashboardReportKit.java:616");}
		}
	}

	// =====================================================================================
	// UTIL
	// =====================================================================================

	private static List<Bagian> amanBagian(SumberLaporan s) {
		List<Bagian> b = null;
		try {
			b = s == null ? null : s.bagian();
		} catch (Exception e) {
			try { Common.tampilErrorJikaAdmin(e); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/DashboardReportKit.java:629");}
		}
		return b == null ? new ArrayList<Bagian>() : b;
	}

	/** Menjalankan SQL dengan aman (memakai currentSession yang ditutup otomatis kerangka kerja). */
	public static List<Object[]> sql(String q) {
		try {
			List<Object[]> r = Common.ambilSql(q);
			return r == null ? new ArrayList<Object[]>() : r;
		} catch (Exception e) {
			try { Common.tampilErrorJikaAdmin(e); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/DashboardReportKit.java:640");}
			return new ArrayList<Object[]>();
		}
	}

	public static long L(Object o) {
		if (o == null) return 0;
		if (o instanceof Number) return ((Number) o).longValue();
		try { return Long.parseLong(o.toString().trim()); } catch (Exception e) { return 0; }
	}

	public static String S(Object o) {
		return o == null ? "" : o.toString();
	}

	public static String fmt(long v) {
		return String.format("%,d", v);
	}

	private static String fmt2(double v) {
		return String.format(java.util.Locale.US, "%.1f", v);
	}

	private static String pct(long part, long total) {
		if (total <= 0) return "0%";
		return String.format(java.util.Locale.US, "%.1f%%", part * 100.0 / total);
	}

	private static double pctNum(long part, long total) {
		if (total <= 0) return 0;
		return Math.min(100.0, part * 100.0 / total);
	}

	static String esc(String s) {
		if (s == null) return "";
		StringBuilder b = new StringBuilder(s.length() + 16);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '&': b.append("&amp;"); break;
				case '<': b.append("&lt;"); break;
				case '>': b.append("&gt;"); break;
				case '"': b.append("&quot;"); break;
				case '\'': b.append("&#39;"); break;
				default: b.append(c);
			}
		}
		return b.toString();
	}

	private static String bersih(String s) {
		if (s == null) return "Laporan";
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length() && b.length() < 40; i++) {
			char c = s.charAt(i);
			b.append(Character.isLetterOrDigit(c) ? c : '_');
		}
		return b.length() == 0 ? "Laporan" : b.toString();
	}

	private static String namaSheetAman(String judul, int idx, XSSFWorkbook wb) {
		String base = judul == null ? ("Sheet" + (idx + 1)) : judul.replaceAll("[\\\\/*?:\\[\\]]", " ").trim();
		if (base.length() > 28) base = base.substring(0, 28);
		if (base.length() == 0) base = "Sheet" + (idx + 1);
		String nama = base;
		int seq = 2;
		while (wb.getSheet(nama) != null) {
			nama = base + " " + (seq++);
			if (nama.length() > 31) nama = nama.substring(0, 31);
		}
		return nama;
	}

	private static CellStyle headStyle(XSSFWorkbook wb) {
		Font f = wb.createFont();
		f.setBoldweight(Font.BOLDWEIGHT_BOLD);
		f.setColor(org.zkoss.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
		CellStyle cs = wb.createCellStyle();
		cs.setFont(f);
		cs.setFillForegroundColor(org.zkoss.poi.ss.usermodel.IndexedColors.TEAL.getIndex());
		cs.setFillPattern(CellStyle.SOLID_FOREGROUND);
		return cs;
	}

	private static CellStyle titleStyle(XSSFWorkbook wb) {
		Font f = wb.createFont();
		f.setBoldweight(Font.BOLDWEIGHT_BOLD);
		f.setFontHeightInPoints((short) 12);
		CellStyle cs = wb.createCellStyle();
		cs.setFont(f);
		return cs;
	}

	private static void titleRow(Sheet sh, int idx, CellStyle style, String text) {
		Row r = sh.createRow(idx);
		Cell c = r.createCell(0);
		c.setCellValue(text == null ? "" : text);
		if (style != null) c.setCellStyle(style);
	}

	/** Gaya laporan: responsif (grid fleksibel + media query), modern, siap cetak A4. */
	private static String css() {
		return ""
			+ "*{box-sizing:border-box;}"
			+ "body{font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#0f172a;margin:0;padding:20px 22px;background:#fff;font-size:12px;line-height:1.5;}"
			+ ".kop{border-radius:14px;padding:16px 20px;color:#fff;background:linear-gradient(135deg,#0f766e 0%,#0e7490 55%,#2563eb 100%);margin-bottom:16px;}"
			+ ".kop-eyebrow{font-size:9.5px;letter-spacing:.16em;font-weight:800;opacity:.85;}"
			+ ".kop-judul{font-size:21px;font-weight:900;line-height:1.15;margin-top:3px;}"
			+ ".kop-sub{font-size:12px;opacity:.92;margin-top:3px;}"
			+ ".kop-desk{font-size:11.5px;opacity:.9;margin-top:5px;max-width:820px;}"
			+ ".kop-cap{font-size:10.5px;opacity:.8;margin-top:6px;}"
			+ ".panel{border:1px solid #e5e7eb;border-radius:12px;padding:14px 16px;margin-bottom:14px;box-shadow:0 1px 5px rgba(15,23,42,.04);page-break-inside:avoid;}"
			+ ".panel-head{display:flex;align-items:center;gap:8px;}"
			+ ".panel-no{display:inline-flex;align-items:center;justify-content:center;width:22px;height:22px;border-radius:7px;background:#0e7490;color:#fff;font-size:12px;font-weight:900;}"
			+ ".panel-title{font-size:14px;font-weight:800;}"
			+ ".panel-desc{font-size:11px;color:#64748b;margin:4px 0 10px;}"
			+ ".kpi-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:10px;}"
			+ ".kpi{background:#f8fafc;border:1px solid #e5e7eb;border-radius:10px;padding:10px 12px;border-top:3px solid #0e7490;}"
			+ ".kpi-label{font-size:9px;font-weight:900;text-transform:uppercase;letter-spacing:.04em;color:#0e7490;}"
			+ ".kpi-val{font-size:22px;font-weight:900;margin-top:4px;line-height:1;}"
			+ ".kpi-help{font-size:10px;color:#64748b;margin-top:4px;}"
			+ ".tbl-wrap{overflow-x:auto;}"
			+ ".tbl{width:100%;border-collapse:collapse;font-size:11px;min-width:360px;}"
			+ ".tbl th{background:#0e7490;color:#fff;text-align:left;padding:7px 8px;font-size:10.5px;}"
			+ ".tbl td{padding:6px 8px;border-bottom:1px solid #eef2f7;}"
			+ ".tbl tbody tr:nth-child(even){background:#f8fafc;}"
			+ ".tbl .r{text-align:right;}"
			+ ".barchart{display:flex;flex-direction:column;gap:6px;}"
			+ ".bc-row{display:grid;grid-template-columns:minmax(90px,30%) 1fr auto;gap:10px;align-items:center;}"
			+ ".bc-lbl{font-size:11px;font-weight:700;color:#334155;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}"
			+ ".bc-track{height:11px;background:#f1f5f9;border-radius:999px;overflow:hidden;}"
			+ ".bc-fill{height:11px;background:linear-gradient(90deg,#0e7490,#2563eb);border-radius:999px;}"
			+ ".bc-val{font-size:11px;font-weight:800;}"
			+ ".donut-wrap{display:flex;flex-wrap:wrap;gap:16px;align-items:center;}"
			+ ".donut{width:150px;height:150px;border-radius:50%;display:flex;align-items:center;justify-content:center;flex:0 0 auto;}"
			+ ".donut-hole{width:88px;height:88px;border-radius:50%;background:#fff;}"
			+ ".donut-legend{font-size:11px;flex:1 1 200px;}"
			+ ".donut-legend div{margin:3px 0;}"
			+ ".dot{display:inline-block;width:10px;height:10px;border-radius:3px;margin-right:6px;vertical-align:middle;}"
			+ ".note{font-size:10.5px;color:#64748b;margin-top:6px;font-style:italic;}"
			+ ".kosong{padding:20px;text-align:center;color:#94a3b8;font-size:12px;}"
			+ ".ftr{margin-top:22px;padding-top:10px;border-top:1px solid #e2e8f0;font-size:10px;color:#94a3b8;text-align:center;}"
			+ "@media(max-width:640px){body{padding:12px;}.kop-judul{font-size:18px;}.bc-row{grid-template-columns:1fr;}.bc-lbl{white-space:normal;}}"
			+ "@media print{body{padding:0;-webkit-print-color-adjust:exact;print-color-adjust:exact;}.panel{page-break-inside:avoid;}}"
			+ "@page{size:A4;margin:12mm;}";
	}
}
