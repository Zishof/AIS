package ais.action.master.helper;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>Laporan &amp; Analisis Kunjungan Pengguna Sistem</h1>
 *
 * <p>Membangun laporan CETAK yang lengkap dan detail atas data {@code public.log_login}
 * untuk tombol <b>Cetak</b> di Dasbor Login Pengguna ({@code log_login.zul} /
 * {@code LogLoginAction}). Laporan berisi statistik, daftar, dan analisis kunjungan pengguna
 * yang dipecah menjadi:</p>
 * <ol>
 *   <li><b>Ringkasan eksekutif</b> — total kunjungan, pengguna unik, sukses/gagal, akses mobile,
 *       hari aktif, rata-rata per hari.</li>
 *   <li><b>Tren harian</b> &amp; <b>distribusi jam</b> (jam sibuk).</li>
 *   <li><b>Per Jenis Pengguna</b> — Mahasiswa/Dosen/Pegawai/Guru/Siswa/Admin/Penduduk.</li>
 *   <li><b>Per Satuan Kerja</b> (join {@code rab.satuan_kerja}) &amp; per Fakultas (unit akademik).</li>
 *   <li><b>Akses Mobile vs Web</b> beserta rincian mobile per jenis pengguna.</li>
 *   <li><b>Per Pengguna</b> — daftar terperinci pengguna paling aktif (Top 300).</li>
 *   <li><b>Analisis &amp; Rekomendasi</b> naratif yang dihitung otomatis dari data.</li>
 * </ol>
 *
 * <p>Laporan dirender sebagai dokumen HTML mandiri (CSS inline + aturan {@code @media print})
 * lalu ditampilkan pada {@link Iframe} di dalam popup {@link MyWindow}; tombol "Cetak" pada popup
 * memanggil {@code print()} pada iframe sehingga hanya isi laporan yang tercetak. Pola HTML/CSS ini
 * mengikuti gaya dasbor aplikasi (tanpa JFreeChart / gambar server).</p>
 *
 * <p>Semua agregasi memakai SQL PostgreSQL via {@link ais.common.Common#ambilSql(String)} — pola
 * yang sama dengan {@code DashboardRekapKunjunganPengguna}. Kolom {@code "login"} di-quote karena
 * kata kunci. Setiap query dibungkus try/catch agar satu query gagal (mis. skema {@code rab})
 * tidak menggagalkan seluruh laporan.</p>
 *
 * <p>Kompatibel ZK 5 (Java 1.6/1.7): hanya memakai komponen inti + {@code AMedia}.</p>
 *
 * @author AIS
 */
public final class LaporanKunjunganPenggunaHelper {

	private LaporanKunjunganPenggunaHelper() {
	}

	/** Ekspresi CASE penentu jenis pengguna berdasarkan FK yang terisi (alias tabel {@code a}). */
	private static final String JENIS =
			"case when a.mahasiswa is not null then 'Mahasiswa' "
			+ "when a.dosen is not null then 'Dosen' "
			+ "when a.pegawai is not null then 'Pegawai' "
			+ "when a.guru is not null then 'Guru' "
			+ "when a.siswa is not null then 'Siswa' "
			+ "when a.tbmuser is not null then 'Admin / Operator' "
			+ "when a.penduduk is not null then 'Penduduk / Umum' "
			+ "else 'Lainnya' end";

	/** Predikat akses mobile berdasar kolom {@code description} (mis. "Login from Android"). */
	private static final String MOBILE =
			"(coalesce(a.description,'') ilike '%android%' or coalesce(a.description,'') ilike '%mobile%' "
			+ "or coalesce(a.description,'') ilike '%ios%' or coalesce(a.description,'') ilike '%iphone%')";

	/** Per Tbmrole (hak akses): log_login → tbmuser(userid) → tbmrole(roleid → rolename via kolom userrole). */
	private static final String SQL_TBMROLE =
			"select coalesce(tr.rolename,'(Tanpa Role)'), count(*), count(distinct a.nama) "
			+ "from log_login a join tbmuser tu on a.tbmuser = tu.userid "
			+ "left join tbmrole tr on tu.userrole = tr.roleid ";
	private static final String SQL_TBMROLE_TAIL = " and a.tbmuser is not null group by 1 order by 2 desc";

	private static final int MAKS_BARIS_PENGGUNA = 300;

	// =====================================================================================
	// ENTRY POINT
	// =====================================================================================

	/**
	 * Membangun laporan lalu menampilkannya pada popup siap-cetak.
	 *
	 * @param owner   komponen pemicu (untuk memperoleh Page/Desktop).
	 * @param mulai   tanggal awal periode (inklusif). Bila null dipakai 30 hari ke belakang.
	 * @param sampai  tanggal akhir periode (inklusif). Bila null dipakai hari ini.
	 * @param fakId   filter fakultas (0/null = semua).
	 * @param jurId   filter jurusan (0/null = semua).
	 * @param yayId   filter yayasan (0/null = semua).
	 * @param sekId   filter sekolah (0/null = semua).
	 */
	public static void tampilkanLaporan(final Component owner, Date mulai, Date sampai,
			Long fakId, Long jurId, Long yayId, Long sekId) {
		if (sampai == null) {
			sampai = ais.ui.util.WaktuUtil.getDate();
		}
		if (mulai == null) {
			java.util.Calendar c = ais.ui.util.WaktuUtil.getCalendar();
			c.setTime(sampai);
			c.add(java.util.Calendar.DATE, -30);
			mulai = c.getTime();
		}

		// Kumpulkan data BERTAHAP via echoEvent agar bar progres bisa dirender di klien antar tahap
		// (kalau semua query dijalankan dalam satu request, klien membeku tanpa umpan balik).
		final Konteks d = new Konteks();
		d.owner = owner;
		d.mulai = mulai;
		d.sampai = sampai;
		d.fakId = fakId;
		d.jurId = jurId;
		d.yayId = yayId;
		d.sekId = sekId;
		d.where = whereClause(mulai, sampai, fakId, jurId, yayId, sekId);

		MyWindow pw = new MyWindow("Menyusun Laporan", "normal", false);
		pw.setWidth("440px");
		pw.setStyle("background:#fff;");
		pw.setContentStyle("background:#fff;");
		org.zkoss.zul.Html bar = new org.zkoss.zul.Html();
		bar.setParent(pw);
		d.progressWin = pw;
		d.bar = bar;
		updateBar(d, 8, "Menyiapkan data & filter");
		pw.setPage(owner.getPage());
		pw.doHighlighted();

		final org.zkoss.zul.Div driver = new org.zkoss.zul.Div();
		driver.setVisible(false);
		driver.setParent(pw);
		d.driver = driver;
		driver.addEventListener("onLaporanStage", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				jalankanStage(d, Integer.parseInt(String.valueOf(e.getData())));
			}
		});
		Events.echoEvent("onLaporanStage", driver, "1");
	}

	/** Menampilkan popup laporan siap-cetak dari HTML yang sudah jadi (dipanggil di tahap akhir progres). */
	private static void bukaModalLaporan(Component owner, String html) {
		MyWindow w = new MyWindow("Laporan & Analisis Kunjungan Pengguna Sistem", "normal", true);
		w.setWidth("96%");
		w.setHeight("96%");
		try { w.setMaximizable(true); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/LaporanKunjunganPenggunaHelper.java:163");}
		try { w.setSizable(true); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/LaporanKunjunganPenggunaHelper.java:164");}
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
			frame.setContent(new AMedia("laporan_kunjungan_pengguna.html", "html", "text/html;charset=UTF-8",
					html.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			frame.setContent(new AMedia("laporan_kunjungan_pengguna.html", "html", "text/html", html));
		}

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak / Simpan PDF", "/img/print.png");
		cetak.setParent(tb);
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				Clients.evalJavaScript(
						"var f=document.getElementById('" + frame.getUuid() + "');"
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
	// PROGRESS (echo-chain) — bar persen bertahap saat menyusun laporan cetak
	// =====================================================================================

	/** Wadah state antar-tahap: query dikumpulkan bertahap, lalu dirender di tahap akhir. */
	private static class Konteks {
		Component owner;
		Date mulai, sampai;
		Long fakId, jurId, yayId, sekId;
		String where;
		long total, unik, sukses, gagal, mobile, hariAktif;
		List<Object[]> harian, perJam, perJenis, perSatker, perFakultas, perTbmrole, perKanal, mobilePerJenis, perPengguna;
		MyWindow progressWin;
		org.zkoss.zul.Html bar;
		Component driver;
	}

	private static final int[] PERSEN = { 0, 18, 30, 40, 52, 64, 74, 84, 92 };
	private static final String[] LABEL = { "",
			"Menghitung ringkasan kunjungan", "Menyusun tren harian", "Menganalisis jam sibuk",
			"Analisis per jenis pengguna", "Analisis per satuan kerja & fakultas",
			"Analisis per tbmrole (hak akses)", "Analisis akses mobile", "Menyusun daftar per pengguna" };

	/** Menjalankan satu tahap, memperbarui bar, lalu meng-echo tahap berikut (echo = klien render progres). */
	private static void jalankanStage(Konteks d, int st) {
		try {
			switch (st) {
				case 1: {
					List<Object[]> q1 = sql("select count(*), count(distinct a.nama), "
							+ "sum(case when a.success_status then 1 else 0 end), "
							+ "sum(case when a.success_status is not true then 1 else 0 end), "
							+ "sum(case when " + MOBILE + " then 1 else 0 end), count(distinct date(a.\"login\")) "
							+ "from log_login a " + d.where);
					if (!q1.isEmpty()) {
						Object[] r = q1.get(0);
						d.total = L(r[0]); d.unik = L(r[1]); d.sukses = L(r[2]); d.gagal = L(r[3]);
						d.mobile = L(r[4]); d.hariAktif = L(r[5]);
					}
					break;
				}
				case 2:
					d.harian = sql("select to_char(a.\"login\",'YYYY-MM-DD'), count(*), "
							+ "sum(case when a.success_status is not true then 1 else 0 end) "
							+ "from log_login a " + d.where + " group by 1 order by 1");
					break;
				case 3:
				d.perJam = sql("select CAST(extract(hour from a.\"login\") AS integer), count(*) "
							+ "from log_login a " + d.where + " group by 1 order by 1");
					break;
				case 4:
					d.perJenis = sql("select " + JENIS + " jenis, count(*), count(distinct a.nama), "
							+ "sum(case when a.success_status then 1 else 0 end), "
							+ "sum(case when a.success_status is not true then 1 else 0 end), "
							+ "sum(case when " + MOBILE + " then 1 else 0 end) "
							+ "from log_login a " + d.where + " group by 1 order by 2 desc");
					break;
				case 5:
					d.perSatker = sql("select coalesce(sk.nama,'(Tanpa Satuan Kerja)'), count(*), count(distinct a.nama) "
							+ "from log_login a left join rab.satuan_kerja sk on a.satuan_kerja = sk.id "
							+ d.where + " group by 1 order by 2 desc");
					d.perFakultas = sql("select coalesce(f.nama,'(Tanpa Fakultas)'), count(*), count(distinct a.nama) "
							+ "from log_login a left join fakultas f on a.fakultas = f.id "
							+ d.where + " group by 1 order by 2 desc");
					break;
				case 6:
					d.perTbmrole = sql(SQL_TBMROLE + d.where + SQL_TBMROLE_TAIL);
					break;
				case 7:
					d.perKanal = sql("select case when " + MOBILE + " then 'Mobile (Android/iOS)' else 'Web / Desktop' end, "
							+ "count(*), count(distinct a.nama) from log_login a " + d.where + " group by 1 order by 2 desc");
					d.mobilePerJenis = sql("select " + JENIS + ", count(*) from log_login a "
							+ d.where + " and " + MOBILE + " group by 1 order by 2 desc");
					break;
				case 8:
					d.perPengguna = sql("select a.nama, max(" + JENIS + "), count(*), "
							+ "sum(case when a.success_status then 1 else 0 end), "
							+ "sum(case when a.success_status is not true then 1 else 0 end), "
							+ "sum(case when " + MOBILE + " then 1 else 0 end), max(a.\"login\"), max(a.ip) "
							+ "from log_login a " + d.where + " group by a.nama order by count(*) desc limit " + MAKS_BARIS_PENGGUNA);
					break;
				case 9:
					updateBar(d, 100, "Menyelesaikan laporan");
					String html = renderHtml(d);
					try { d.progressWin.detach(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/LaporanKunjunganPenggunaHelper.java:291");}
					bukaModalLaporan(d.owner, html);
					return;
			}
		} catch (Exception ex) {
			try { Common.tampilErrorJikaAdmin(ex); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/LaporanKunjunganPenggunaHelper.java:296");}
		}
		updateBar(d, PERSEN[st], LABEL[st]);
		Events.echoEvent("onLaporanStage", d.driver, String.valueOf(st + 1));
	}

	private static void updateBar(Konteks d, int pct, String label) {
		if (d == null || d.bar == null) {
			return;
		}
		String h = "<div style='padding:18px 20px;font-family:Segoe UI,Roboto,Arial,sans-serif;min-width:360px;'>"
				+ "<div style='font-size:13px;font-weight:800;color:#0f172a;'>Menyusun Laporan Kunjungan</div>"
				+ "<div style='font-size:11.5px;color:#475569;margin:4px 0 10px;'>" + esc(label) + " ...</div>"
				+ "<div style='height:14px;background:#e2e8f0;border-radius:999px;overflow:hidden;'>"
				+ "<div style='height:14px;width:" + pct + "%;background:linear-gradient(90deg,#0e7490,#2563eb);border-radius:999px;'></div>"
				+ "</div>"
				+ "<div style='text-align:right;font-size:12px;font-weight:800;color:#0e7490;margin-top:6px;'>" + pct + "%</div>"
				+ "</div>";
		try { d.bar.setContent(h); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/LaporanKunjunganPenggunaHelper.java:314");}
	}

	private static List<Object[]> nn(List<Object[]> l) {
		return l == null ? new ArrayList<Object[]>() : l;
	}

	// =====================================================================================
	// EKSPOR EXCEL (.xlsx) — daftar LENGKAP per pengguna + sheet agregat
	// =====================================================================================

	/**
	 * Mengekspor laporan ke berkas Excel multi-sheet (unduh via {@link Filedownload}):
	 * Ringkasan, Per Jenis Pengguna, Per Satuan Kerja, Per Fakultas, Mobile vs Web, Tren Harian,
	 * dan <b>Per Pengguna (daftar LENGKAP — tanpa batas 300 seperti pada laporan HTML)</b>.
	 * Rentang &amp; filter sama dengan {@link #tampilkanLaporan}.
	 */
	public static void eksporExcel(Date mulai, Date sampai, Long fakId, Long jurId, Long yayId, Long sekId) {
		try {
			Filedownload.save(buatExcel(mulai, sampai, fakId, jurId, yayId, sekId),
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
					namaBerkasExcel());
		} catch (Exception e) {
			try { Common.tampilErrorJikaAdmin(e); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/LaporanKunjunganPenggunaHelper.java:eksporExcel");}
		}
	}

	/**
	 * Menyusun isi workbook yang sama dengan ekspor layar ZK tanpa mengikatnya
	 * pada {@link Filedownload}. Kontrak native memakai method ini agar Android
	 * dan desktop memperoleh byte .xlsx yang identik, bukan implementasi laporan
	 * kedua yang perlahan menyimpang dari layar lama.
	 */
	public static byte[] buatExcel(Date mulai, Date sampai, Long fakId, Long jurId, Long yayId, Long sekId)
			throws Exception {
		if (sampai == null) {
			sampai = ais.ui.util.WaktuUtil.getDate();
		}
		if (mulai == null) {
			java.util.Calendar c = ais.ui.util.WaktuUtil.getCalendar();
			c.setTime(sampai);
			c.add(java.util.Calendar.DATE, -30);
			mulai = c.getTime();
		}
		String where = whereClause(mulai, sampai, fakId, jurId, yayId, sekId);
		SimpleDateFormat tampil = new SimpleDateFormat("dd-MM-yyyy");
		String periode = "Periode " + tampil.format(mulai) + " s.d. " + tampil.format(sampai)
				+ "  |  Dicetak " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(ais.ui.util.WaktuUtil.getDate());

		XSSFWorkbook wb = new XSSFWorkbook();
		CellStyle head = headStyle(wb);
		CellStyle title = titleStyle(wb);

		// --- Sheet: Ringkasan ---
		List<Object[]> q1 = sql("select count(*), count(distinct a.nama), "
				+ "sum(case when a.success_status then 1 else 0 end), "
				+ "sum(case when a.success_status is not true then 1 else 0 end), "
				+ "sum(case when " + MOBILE + " then 1 else 0 end), count(distinct date(a.\"login\")) "
				+ "from log_login a " + where);
		long total = 0, unik = 0, sukses = 0, gagal = 0, mob = 0, hari = 0;
		if (!q1.isEmpty()) {
			Object[] r = q1.get(0);
			total = L(r[0]); unik = L(r[1]); sukses = L(r[2]); gagal = L(r[3]); mob = L(r[4]); hari = L(r[5]);
		}
		Sheet sr = wb.createSheet("Ringkasan");
		titleRow(sr, 0, title, "LAPORAN & ANALISIS KUNJUNGAN PENGGUNA SISTEM");
		titleRow(sr, 1, null, periode);
		headerRow(sr, 3, head, "Metrik", "Nilai");
		int ri = 4;
		ri = kv(sr, ri, "Total Kunjungan", total);
		ri = kv(sr, ri, "Pengguna Unik", unik);
		ri = kv(sr, ri, "Login Berhasil", sukses);
		ri = kv(sr, ri, "Login Gagal", gagal);
		ri = kv(sr, ri, "Akses Mobile", mob);
		ri = kv(sr, ri, "Akses Web/Desktop", total - mob);
		ri = kv(sr, ri, "Hari Aktif", hari);
		ri = kv(sr, ri, "Rata-rata per Hari Aktif", hari > 0 ? Math.round((double) total / hari) : total);
		autoSize(sr, 2);

		// --- Sheet: Per Jenis Pengguna ---
		sheetKategoriRinci(wb, "Per Jenis Pengguna", periode, head, title,
				sql("select " + JENIS + " jenis, count(*), count(distinct a.nama), "
						+ "sum(case when a.success_status then 1 else 0 end), "
						+ "sum(case when a.success_status is not true then 1 else 0 end), "
						+ "sum(case when " + MOBILE + " then 1 else 0 end) "
						+ "from log_login a " + where + " group by 1 order by 2 desc"),
				"Jenis Pengguna");

		// --- Sheet: Per Satuan Kerja ---
		sheetKategori(wb, "Per Satuan Kerja", periode, head, title,
				sql("select coalesce(sk.nama,'(Tanpa Satuan Kerja)'), count(*), count(distinct a.nama) "
						+ "from log_login a left join rab.satuan_kerja sk on a.satuan_kerja = sk.id "
						+ where + " group by 1 order by 2 desc"),
				"Satuan Kerja");

		// --- Sheet: Per Fakultas ---
		sheetKategori(wb, "Per Fakultas", periode, head, title,
				sql("select coalesce(f.nama,'(Tanpa Fakultas)'), count(*), count(distinct a.nama) "
						+ "from log_login a left join fakultas f on a.fakultas = f.id "
						+ where + " group by 1 order by 2 desc"),
				"Fakultas");

		// --- Sheet: Per Tbmrole (Hak Akses) ---
		sheetKategori(wb, "Per Tbmrole (Hak Akses)", periode, head, title,
				sql(SQL_TBMROLE + where + SQL_TBMROLE_TAIL), "Tbmrole / Hak Akses");

		// --- Sheet: Mobile vs Web ---
		sheetKategori(wb, "Mobile vs Web", periode, head, title,
				sql("select case when " + MOBILE + " then 'Mobile (Android/iOS)' else 'Web / Desktop' end, "
						+ "count(*), count(distinct a.nama) from log_login a " + where + " group by 1 order by 2 desc"),
				"Kanal Akses");

		// --- Sheet: Tren Harian ---
		Sheet sh = wb.createSheet("Tren Harian");
		titleRow(sh, 0, title, "TREN KUNJUNGAN HARIAN");
		titleRow(sh, 1, null, periode);
		headerRow(sh, 3, head, "Tanggal", "Total Kunjungan", "Login Gagal");
		int hi = 4;
		for (Object[] r : sql("select to_char(a.\"login\",'YYYY-MM-DD'), count(*), "
				+ "sum(case when a.success_status is not true then 1 else 0 end) "
				+ "from log_login a " + where + " group by 1 order by 1")) {
			Row row = sh.createRow(hi++);
			cellStr(row, 0, S(r[0]));
			cellNum(row, 1, L(r[1]));
			cellNum(row, 2, L(r[2]));
		}
		autoSize(sh, 3);

		// --- Sheet: Per Pengguna (LENGKAP, tanpa limit) ---
		Sheet sp = wb.createSheet("Per Pengguna");
		titleRow(sp, 0, title, "DAFTAR RINCI PER PENGGUNA");
		titleRow(sp, 1, null, periode);
		headerRow(sp, 3, head, "No", "Nama Pengguna", "Jenis", "Total", "Berhasil", "Gagal",
				"Mobile", "Akses Terakhir", "IP Terakhir");
		int pi = 4, no = 1;
		SimpleDateFormat dtf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		for (Object[] r : sql("select a.nama, max(" + JENIS + "), count(*), "
				+ "sum(case when a.success_status then 1 else 0 end), "
				+ "sum(case when a.success_status is not true then 1 else 0 end), "
				+ "sum(case when " + MOBILE + " then 1 else 0 end), max(a.\"login\"), max(a.ip) "
				+ "from log_login a " + where + " group by a.nama order by count(*) desc")) {
			Row row = sp.createRow(pi++);
			cellNum(row, 0, no++);
			cellStr(row, 1, S(r[0]));
			cellStr(row, 2, S(r[1]));
			cellNum(row, 3, L(r[2]));
			cellNum(row, 4, L(r[3]));
			cellNum(row, 5, L(r[4]));
			cellNum(row, 6, L(r[5]));
			cellStr(row, 7, tglStr(r[6], dtf));
			cellStr(row, 8, S(r[7]));
		}
		autoSize(sp, 9);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		wb.write(baos);
		return baos.toByteArray();
	}

	/** Nama aman dan stabil untuk hasil ekspor laporan kunjungan. */
	public static String namaBerkasExcel() {
		return "Laporan_Kunjungan_Pengguna_"
				+ new SimpleDateFormat("yyyyMMdd_HHmm").format(ais.ui.util.WaktuUtil.getDate()) + ".xlsx";
	}

	/**
	 * Memasang tombol <b>"Cetak Laporan Lengkap"</b> + <b>"Ekspor Excel"</b> (memakai laporan kunjungan
	 * yang sama) ke sebuah toolbar/kontainer pada dasbor lain. Rentang &amp; filter dibaca dari komponen
	 * filter dasbor tersebut saat tombol diklik. Lewatkan {@code null} untuk combobox yang tak tersedia.
	 */
	public static void pasangTombolLaporan(Component parent, final Component owner,
			final MyDatebox mulai, final MyDatebox sampai,
			final Combobox fak, final Combobox jur, final Combobox yay, final Combobox sek) {
		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak Laporan Lengkap", "/img/print.png");
		cetak.setTooltiptext("Cetak laporan & analisis kunjungan pengguna: statistik, per pengguna, per jenis, "
				+ "per satuan kerja, per tbmrole (hak akses), dan akses mobile");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				tampilkanLaporan(owner, val(mulai), val(sampai), idDari(fak), idDari(jur), idDari(yay), idDari(sek));
			}
		});
		cetak.setParent(parent);

		final MyToolbarbuttonConfig excel = new MyToolbarbuttonConfig("Ekspor Excel", "/img/excel.png");
		excel.setTooltiptext("Unduh laporan kunjungan ke Excel (.xlsx), termasuk daftar LENGKAP per pengguna");
		// 2 langkah: klik → tampil indikator "sedang memproses" → echo → susun & unduh berkas
		excel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				try { Clients.showBusy("Menyiapkan berkas Excel, mohon tunggu..."); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/LaporanKunjunganPenggunaHelper.java:488");}
				Events.echoEvent("onProsesExcelLaporan", excel, null);
			}
		});
		excel.addEventListener("onProsesExcelLaporan", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				try {
					eksporExcel(val(mulai), val(sampai), idDari(fak), idDari(jur), idDari(yay), idDari(sek));
				} finally {
					try { Clients.clearBusy(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/LaporanKunjunganPenggunaHelper.java:498");}
				}
			}
		});
		excel.setParent(parent);
	}

	private static Date val(MyDatebox d) {
		try {
			return (d == null || d.getValue() == null) ? null : d.getValue();
		} catch (Exception e) {
			return null;
		}
	}

	private static Long idDari(Combobox cb) {
		try {
			if (cb == null || cb.getSelectedItem() == null || cb.getSelectedItem().getValue() == null) {
				return null;
			}
			Object v = cb.getSelectedItem().getValue();
			if (v instanceof ais.database.model.Fakultas) return ((ais.database.model.Fakultas) v).getId();
			if (v instanceof ais.database.model.Jurusan) return ((ais.database.model.Jurusan) v).getId();
			if (v instanceof ais.database.model.sekolah.Yayasan) return ((ais.database.model.sekolah.Yayasan) v).getId();
			if (v instanceof ais.database.model.sekolah.Sekolah) return ((ais.database.model.sekolah.Sekolah) v).getId();
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	private static String whereClause(Date mulai, Date sampai, Long fakId, Long jurId, Long yayId, Long sekId) {
		SimpleDateFormat dbf = Common.databaseDateFormat.get();
		String where = "where date(a.\"login\") between date('" + dbf.format(mulai) + "') and date('"
				+ dbf.format(sampai) + "') ";
		if (fakId != null && fakId > 0) where += " and a.fakultas = " + fakId;
		if (jurId != null && jurId > 0) where += " and a.jurusan = " + jurId;
		if (yayId != null && yayId > 0) where += " and a.yayasan = " + yayId;
		if (sekId != null && sekId > 0) where += " and a.sekolah = " + sekId;
		return where;
	}

	// ---- util POI ----
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
		c.setCellValue(text);
		if (style != null) c.setCellStyle(style);
	}

	private static void headerRow(Sheet sh, int idx, CellStyle head, String... cols) {
		Row r = sh.createRow(idx);
		for (int i = 0; i < cols.length; i++) {
			Cell c = r.createCell(i);
			c.setCellValue(cols[i]);
			c.setCellStyle(head);
		}
	}

	private static int kv(Sheet sh, int idx, String k, long v) {
		Row r = sh.createRow(idx);
		r.createCell(0).setCellValue(k);
		r.createCell(1).setCellValue((double) v);
		return idx + 1;
	}

	private static Cell cellStr(Row r, int c, String v) {
		Cell x = r.createCell(c);
		x.setCellValue(v == null ? "" : v);
		return x;
	}

	private static Cell cellNum(Row r, int c, long v) {
		Cell x = r.createCell(c);
		x.setCellValue((double) v);
		return x;
	}

	private static void autoSize(Sheet sh, int cols) {
		try {
			for (int i = 0; i < cols; i++) sh.autoSizeColumn(i);
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/LaporanKunjunganPenggunaHelper.java:599");
		}
	}

	/** Sheet kategori sederhana: [Label, Kunjungan, Pengguna Unik]. */
	private static void sheetKategori(XSSFWorkbook wb, String name, String periode, CellStyle head,
			CellStyle title, List<Object[]> rows, String labelKolom) {
		Sheet sh = wb.createSheet(name);
		titleRow(sh, 0, title, name.toUpperCase());
		titleRow(sh, 1, null, periode);
		headerRow(sh, 3, head, labelKolom, "Kunjungan", "Pengguna Unik");
		int i = 4;
		for (Object[] r : rows) {
			Row row = sh.createRow(i++);
			cellStr(row, 0, S(r[0]));
			cellNum(row, 1, L(r[1]));
			cellNum(row, 2, r.length > 2 ? L(r[2]) : 0);
		}
		autoSize(sh, 3);
	}

	/** Sheet kategori rinci: [Label, Kunjungan, Pengguna Unik, Berhasil, Gagal, Mobile]. */
	private static void sheetKategoriRinci(XSSFWorkbook wb, String name, String periode, CellStyle head,
			CellStyle title, List<Object[]> rows, String labelKolom) {
		Sheet sh = wb.createSheet(name);
		titleRow(sh, 0, title, name.toUpperCase());
		titleRow(sh, 1, null, periode);
		headerRow(sh, 3, head, labelKolom, "Kunjungan", "Pengguna Unik", "Berhasil", "Gagal", "Mobile");
		int i = 4;
		for (Object[] r : rows) {
			Row row = sh.createRow(i++);
			cellStr(row, 0, S(r[0]));
			cellNum(row, 1, L(r[1]));
			cellNum(row, 2, L(r[2]));
			cellNum(row, 3, L(r[3]));
			cellNum(row, 4, L(r[4]));
			cellNum(row, 5, L(r[5]));
		}
		autoSize(sh, 6);
	}

	// =====================================================================================
	// PEMBANGUN HTML
	// =====================================================================================

	private static String renderHtml(Konteks d) {
		Date mulai = d.mulai, sampai = d.sampai;
		Long fakId = d.fakId, jurId = d.jurId, yayId = d.yayId, sekId = d.sekId;
		long total = d.total, unik = d.unik, sukses = d.sukses, gagal = d.gagal, mobile = d.mobile, hariAktif = d.hariAktif;
		long web = total - mobile;
		long avgHari = hariAktif > 0 ? Math.round((double) total / hariAktif) : total;
		List<Object[]> harian = nn(d.harian), perJam = nn(d.perJam), perJenis = nn(d.perJenis),
				perSatker = nn(d.perSatker), perFakultas = nn(d.perFakultas), perTbmrole = nn(d.perTbmrole),
				perKanal = nn(d.perKanal), mobilePerJenis = nn(d.mobilePerJenis), perPengguna = nn(d.perPengguna);

		// =============================== RENDER ===============================
		StringBuilder h = new StringBuilder(120000);
		SimpleDateFormat tampil = new SimpleDateFormat("dd MMMM yyyy");
		SimpleDateFormat cap = new SimpleDateFormat("dd-MM-yyyy HH:mm");

		h.append("<!DOCTYPE html><html lang='id'><head><meta charset='UTF-8'/>");
		h.append("<title>Laporan Kunjungan Pengguna</title>");
		h.append("<style>").append(css()).append("</style></head><body>");

		// ------- KOP / JUDUL -------
		h.append("<div class='kop'>");
		h.append("<div class='kop-eyebrow'>LAPORAN OPERASIONAL SISTEM</div>");
		h.append("<div class='kop-judul'>Laporan &amp; Analisis Kunjungan Pengguna Sistem</div>");
		h.append("<div class='kop-sub'>Periode <b>").append(tampil.format(mulai)).append("</b> s.d. <b>")
		 .append(tampil.format(sampai)).append("</b>");
		if ((fakId != null && fakId > 0) || (jurId != null && jurId > 0) || (yayId != null && yayId > 0) || (sekId != null && sekId > 0)) {
			h.append(" &middot; <i>dengan filter unit aktif</i>");
		}
		h.append(" &middot; Dicetak ").append(cap.format(ais.ui.util.WaktuUtil.getDate())).append("</div>");
		h.append("</div>");

		if (total <= 0) {
			h.append("<div class='kosong'>Tidak ada data kunjungan pada periode/ filter yang dipilih. "
					+ "Silakan ubah rentang tanggal atau filter, lalu cetak ulang.</div>");
			h.append(footer()).append("</body></html>");
			return h.toString();
		}

		// ------- 1. RINGKASAN EKSEKUTIF (KPI) -------
		h.append(sec("1", "Ringkasan Eksekutif"));
		h.append("<div class='kpi-grid'>");
		h.append(kpi("Total Kunjungan", fmt(total), "seluruh percobaan login pada periode", "#1d4ed8", "#eff6ff"));
		h.append(kpi("Pengguna Unik", fmt(unik), "jumlah akun berbeda yang mengakses", "#7c3aed", "#f5f3ff"));
		h.append(kpi("Login Berhasil", fmt(sukses) + " <span class='mini'>(" + pct(sukses, total) + ")</span>",
				"status sukses", "#047857", "#ecfdf5"));
		h.append(kpi("Login Gagal", fmt(gagal) + " <span class='mini'>(" + pct(gagal, total) + ")</span>",
				"gagal / ditolak", gagal > 0 ? "#b91c1c" : "#64748b", gagal > 0 ? "#fef2f2" : "#f8fafc"));
		h.append(kpi("Akses Mobile", fmt(mobile) + " <span class='mini'>(" + pct(mobile, total) + ")</span>",
				"Android / iOS", "#0e7490", "#ecfeff"));
		h.append(kpi("Akses Web/Desktop", fmt(web) + " <span class='mini'>(" + pct(web, total) + ")</span>",
				"peramban desktop", "#334155", "#f1f5f9"));
		h.append(kpi("Hari Aktif", fmt(hariAktif), "jumlah hari ada kunjungan", "#92400e", "#fffbeb"));
		h.append(kpi("Rata-rata / Hari", fmt(avgHari), "kunjungan per hari aktif", "#0f766e", "#f0fdfa"));
		h.append("</div>");

		// ------- 2. TREN HARIAN -------
		h.append(sec("2", "Tren Kunjungan Harian"));
		if (harian.isEmpty()) {
			h.append(empty());
		} else {
			long maxHarian = 1;
			for (Object[] r : harian) maxHarian = Math.max(maxHarian, L(r[1]));
			h.append("<div class='barchart'>");
			for (Object[] r : harian) {
				long v = L(r[1]);
				long g = L(r[2]);
				h.append("<div class='bc-row'>");
				h.append("<div class='bc-lbl'>").append(esc(S(r[0]))).append("</div>");
				h.append("<div class='bc-track'><div class='bc-fill' style='width:")
				 .append(pctNum(v, maxHarian)).append("%;'></div></div>");
				h.append("<div class='bc-val'>").append(fmt(v));
				if (g > 0) h.append(" <span class='bc-bad'>&#9888; ").append(fmt(g)).append(" gagal</span>");
				h.append("</div></div>");
			}
			h.append("</div>");
		}

		// ------- 3. DISTRIBUSI JAM -------
		h.append(sec("3", "Distribusi Jam Akses (Jam Sibuk)"));
		if (perJam.isEmpty()) {
			h.append(empty());
		} else {
			long[] jamArr = new long[24];
			for (Object[] r : perJam) {
				int jam = (int) L(r[0]);
				if (jam >= 0 && jam < 24) jamArr[jam] = L(r[1]);
			}
			long maxJam = 1;
			int jamPuncak = 0;
			for (int i = 0; i < 24; i++) { if (jamArr[i] > maxJam) { maxJam = jamArr[i]; } if (jamArr[i] > jamArr[jamPuncak]) jamPuncak = i; }
			h.append("<div class='jamgrid'>");
			for (int i = 0; i < 24; i++) {
				int hgt = (int) Math.max(2, pctNum(jamArr[i], maxJam));
				h.append("<div class='jam-col' title='").append(fmt(jamArr[i])).append(" kunjungan'>");
				h.append("<div class='jam-bar-wrap'><div class='jam-bar").append(i == jamPuncak ? " jam-peak" : "")
				 .append("' style='height:").append(hgt).append("%;'></div></div>");
				h.append("<div class='jam-lbl'>").append(i).append("</div></div>");
			}
			h.append("</div>");
			h.append("<div class='note'>Jam paling sibuk: <b>pukul ").append(jamPuncak).append(":00</b> ("
					).append(fmt(jamArr[jamPuncak])).append(" kunjungan).</div>");
		}

		// ------- 4. PER JENIS PENGGUNA -------
		h.append(sec("4", "Analisis Per Jenis Pengguna"));
		appendTabelKategori(h, perJenis, total, true);

		// ------- 5. PER SATUAN KERJA -------
		h.append(sec("5", "Analisis Per Satuan Kerja"));
		appendTabelKategori(h, perSatker, total, false);

		// ------- 5b. PER FAKULTAS -------
		h.append(sec("6", "Analisis Per Unit Akademik (Fakultas)"));
		appendTabelKategori(h, perFakultas, total, false);

		// ------- 6b. PER TBMROLE (HAK AKSES) -------
		h.append(sec("7", "Analisis Per Tbmrole (Hak Akses)"));
		h.append("<div class='note'>Berdasarkan peran / hak akses akun sistem (login yang tertaut ke akun operator/admin). "
				+ "Login yang tidak tertaut akun sistem (mis. mahasiswa/dosen langsung) tidak dihitung di bagian ini.</div>");
		appendTabelKategori(h, perTbmrole, total, false);

		// ------- 7. MOBILE VS WEB -------
		h.append(sec("8", "Analisis Akses Mobile vs Web"));
		h.append("<div class='dua-kolom'>");
		h.append("<div>").append(donutHtml(mobile, total)).append("</div>");
		h.append("<div>");
		appendTabelKategori(h, perKanal, total, false);
		h.append("</div></div>");
		if (!mobilePerJenis.isEmpty()) {
			h.append("<div class='subjudul'>Rincian Akses Mobile per Jenis Pengguna</div>");
			appendTabelKategori(h, mobilePerJenis2col(mobilePerJenis), mobile, false);
		}

		// ------- 7. PER PENGGUNA -------
		h.append(sec("9", "Daftar Rinci Per Pengguna (Top " + MAKS_BARIS_PENGGUNA + " Teraktif)"));
		if (perPengguna.isEmpty()) {
			h.append(empty());
		} else {
			h.append("<table class='tbl'><thead><tr>");
			String[] head = { "#", "Nama Pengguna", "Jenis", "Total", "Berhasil", "Gagal", "Mobile", "Akses Terakhir", "IP Terakhir" };
			for (String hd : head) h.append("<th>").append(hd).append("</th>");
			h.append("</tr></thead><tbody>");
			SimpleDateFormat dtf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
			int no = 1;
			for (Object[] r : perPengguna) {
				long tot = L(r[2]), suk = L(r[3]), gag = L(r[4]), mob = L(r[5]);
				h.append("<tr>");
				h.append("<td class='r'>").append(no++).append("</td>");
				h.append("<td>").append(esc(S(r[0]))).append("</td>");
				h.append("<td>").append(esc(S(r[1]))).append("</td>");
				h.append("<td class='r b'>").append(fmt(tot)).append("</td>");
				h.append("<td class='r'>").append(fmt(suk)).append("</td>");
				h.append("<td class='r'>").append(gag > 0 ? "<span class='bad'>" + fmt(gag) + "</span>" : "0").append("</td>");
				h.append("<td class='r'>").append(fmt(mob)).append("</td>");
				h.append("<td>").append(esc(tglStr(r[6], dtf))).append("</td>");
				h.append("<td>").append(esc(S(r[7]))).append("</td>");
				h.append("</tr>");
			}
			h.append("</tbody></table>");
			if (perPengguna.size() >= MAKS_BARIS_PENGGUNA) {
				h.append("<div class='note'>Menampilkan ").append(MAKS_BARIS_PENGGUNA)
				 .append(" pengguna paling aktif. Untuk daftar penuh gunakan ekspor Excel pada tab <b>Login</b>.</div>");
			}
		}

		// ------- 10. ANALISIS & REKOMENDASI -------
		h.append(sec("10", "Analisis &amp; Rekomendasi"));
		h.append(analisis(total, unik, sukses, gagal, mobile, web, hariAktif, avgHari,
				harian, perJenis, perSatker, perPengguna));

		h.append(footer());
		h.append("</body></html>");
		return h.toString();
	}

	// =====================================================================================
	// SUB-KOMPONEN RENDER
	// =====================================================================================

	/** Tabel kategori generik: kolom [Kategori, Total, %, (opsional Pengguna Unik / Sukses / Gagal / Mobile)] + bar. */
	private static void appendTabelKategori(StringBuilder h, List<Object[]> rows, long grand, boolean rinci) {
		if (rows == null || rows.isEmpty()) { h.append(empty()); return; }
		long maks = 1;
		for (Object[] r : rows) maks = Math.max(maks, L(r[1]));
		h.append("<table class='tbl'><thead><tr>");
		h.append("<th>Kategori</th><th>Kunjungan</th><th>Porsi</th><th style='width:26%;'>Grafik</th>");
		if (rinci) h.append("<th>Pengguna Unik</th><th>Berhasil</th><th>Gagal</th><th>Mobile</th>");
		else if (colCount(rows) >= 3) h.append("<th>Pengguna Unik</th>");
		h.append("</tr></thead><tbody>");
		for (Object[] r : rows) {
			long v = L(r[1]);
			h.append("<tr>");
			h.append("<td>").append(esc(S(r[0]))).append("</td>");
			h.append("<td class='r b'>").append(fmt(v)).append("</td>");
			h.append("<td class='r'>").append(pct(v, grand)).append("</td>");
			h.append("<td><div class='bc-track sm'><div class='bc-fill' style='width:")
			 .append(pctNum(v, maks)).append("%;'></div></div></td>");
			if (rinci) {
				h.append("<td class='r'>").append(fmt(L(r[2]))).append("</td>");
				h.append("<td class='r'>").append(fmt(L(r[3]))).append("</td>");
				h.append("<td class='r'>").append(L(r[4]) > 0 ? "<span class='bad'>" + fmt(L(r[4])) + "</span>" : "0").append("</td>");
				h.append("<td class='r'>").append(fmt(L(r[5]))).append("</td>");
			} else if (colCount(rows) >= 3) {
				h.append("<td class='r'>").append(fmt(L(r[2]))).append("</td>");
			}
			h.append("</tr>");
		}
		h.append("</tbody></table>");
	}

	private static String donutHtml(long part, long total) {
		int p = (int) pctNum(part, total);
		int deg = (int) Math.round(p * 3.6);
		StringBuilder h = new StringBuilder();
		h.append("<div class='donut-wrap'>");
		h.append("<div class='donut' style='background:conic-gradient(#0e7490 0deg ").append(deg)
		 .append("deg,#e2e8f0 ").append(deg).append("deg 360deg);'>");
		h.append("<div class='donut-hole'><div class='donut-pct'>").append(p).append("%</div>")
		 .append("<div class='donut-cap'>MOBILE</div></div></div>");
		h.append("<div class='donut-legend'>");
		h.append("<div><span class='dot' style='background:#0e7490;'></span>Mobile: <b>").append(fmt(part)).append("</b></div>");
		h.append("<div><span class='dot' style='background:#e2e8f0;'></span>Web/Desktop: <b>").append(fmt(total - part)).append("</b></div>");
		h.append("</div></div>");
		return h.toString();
	}

	/** Konversi hasil Q5b (jenis,count) menjadi bentuk 2 kolom untuk appendTabelKategori. */
	private static List<Object[]> mobilePerJenis2col(List<Object[]> in) {
		List<Object[]> out = new ArrayList<Object[]>();
		for (Object[] r : in) out.add(new Object[] { r[0], r[1] });
		return out;
	}

	private static String analisis(long total, long unik, long sukses, long gagal, long mobile, long web,
			long hariAktif, long avgHari, List<Object[]> harian, List<Object[]> perJenis,
			List<Object[]> perSatker, List<Object[]> perPengguna) {
		StringBuilder h = new StringBuilder();
		h.append("<ul class='analisis'>");

		// jenis dominan
		if (!perJenis.isEmpty()) {
			Object[] top = perJenis.get(0);
			h.append(li("Komposisi pengguna didominasi oleh <b>" + esc(S(top[0])) + "</b> dengan "
					+ fmt(L(top[1])) + " kunjungan (" + pct(L(top[1]), total) + " dari total)."));
		}
		// hari puncak
		if (!harian.isEmpty()) {
			Object[] puncak = harian.get(0);
			long mx = 0; String tgl = "";
			for (Object[] r : harian) { if (L(r[1]) > mx) { mx = L(r[1]); tgl = S(r[0]); } }
			h.append(li("Puncak aktivitas terjadi pada <b>" + esc(tgl) + "</b> sebanyak <b>" + fmt(mx)
					+ "</b> kunjungan; rata-rata <b>" + fmt(avgHari) + "</b> kunjungan per hari aktif."));
		}
		// mobile share
		h.append(li("Sebanyak <b>" + pct(mobile, total) + "</b> akses dilakukan melalui perangkat <b>mobile</b> "
				+ "(" + fmt(mobile) + " dari " + fmt(total) + "), sisanya " + pct(web, total) + " via web/desktop."
				+ (mobile > web ? " Kanal mobile menjadi kanal akses utama — pastikan aplikasi mobile prima."
						: " Kanal web/desktop masih dominan.")));
		// kegagalan
		if (gagal > 0) {
			String rate = pct(gagal, total);
			boolean tinggi = total > 0 && ((double) gagal / total) > 0.15;
			h.append(li("Tingkat kegagalan login <b>" + rate + "</b> (" + fmt(gagal) + " percobaan)."
					+ (tinggi ? " <span class='bad'>Relatif tinggi</span> — periksa potensi salah kata sandi massal, "
							+ "brute-force, atau kendala integrasi. Tinjau tab <b>Blacklist</b> dan <b>Kesalahan Sistem</b>."
							: " Dalam batas wajar.")));
		} else {
			h.append(li("Tidak ada login gagal tercatat pada periode ini."));
		}
		// pengguna teraktif
		if (!perPengguna.isEmpty()) {
			Object[] u = perPengguna.get(0);
			h.append(li("Pengguna paling aktif: <b>" + esc(S(u[0])) + "</b> (" + esc(S(u[1])) + ") dengan "
					+ fmt(L(u[2])) + " kunjungan."));
		}
		// satuan kerja teraktif (abaikan bucket kosong)
		if (!perSatker.isEmpty()) {
			for (Object[] r : perSatker) {
				String nm = S(r[0]);
				if (nm.indexOf("Tanpa Satuan Kerja") < 0) {
					h.append(li("Satuan kerja paling aktif: <b>" + esc(nm) + "</b> dengan " + fmt(L(r[1]))
							+ " kunjungan (" + pct(L(r[1]), total) + ")."));
					break;
				}
			}
		}
		// rasio pengguna
		if (unik > 0) {
			long perUser = Math.round((double) total / unik);
			h.append(li("Rata-rata setiap pengguna mengakses <b>" + fmt(perUser) + "</b> kali pada periode ini "
					+ "(" + fmt(total) + " kunjungan / " + fmt(unik) + " pengguna unik)."));
		}
		h.append("</ul>");

		h.append("<div class='rekom'><div class='rekom-judul'>Rekomendasi</div><ul>");
		h.append("<li>Gunakan filter tanggal &amp; unit untuk mempersempit analisis pada satuan kerja tertentu.</li>");
		if (total > 0 && ((double) gagal / total) > 0.15)
			h.append("<li>Audit login gagal: aktifkan pembatasan percobaan dan tinjau daftar Blacklist.</li>");
		if (mobile > web)
			h.append("<li>Prioritaskan kualitas &amp; kompatibilitas aplikasi mobile karena menjadi kanal utama.</li>");
		h.append("<li>Bandingkan tren antar periode secara berkala untuk mendeteksi anomali aktivitas.</li>");
		h.append("</ul></div>");
		return h.toString();
	}

	// =====================================================================================
	// UTIL
	// =====================================================================================

	private static List<Object[]> sql(String q) {
		try {
			List<Object[]> r = Common.ambilSql(q);
			return r == null ? new ArrayList<Object[]>() : r;
		} catch (Exception e) {
			try { Common.tampilErrorJikaAdmin(e); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/LaporanKunjunganPenggunaHelper.java:958");}
			return new ArrayList<Object[]>();
		}
	}

	private static int colCount(List<Object[]> rows) {
		return rows.isEmpty() ? 0 : rows.get(0).length;
	}

	private static long L(Object o) {
		if (o == null) return 0;
		if (o instanceof Number) return ((Number) o).longValue();
		try { return Long.parseLong(o.toString().trim()); } catch (Exception e) { return 0; }
	}

	private static String S(Object o) {
		return o == null ? "" : o.toString();
	}

	private static String tglStr(Object o, SimpleDateFormat f) {
		if (o == null) return "";
		if (o instanceof Date) return f.format((Date) o);
		return o.toString();
	}

	private static String fmt(long v) {
		return String.format("%,d", v);
	}

	private static String pct(long part, long total) {
		if (total <= 0) return "0%";
		return String.format("%.1f%%", (part * 100.0) / total);
	}

	private static double pctNum(long part, long total) {
		if (total <= 0) return 0;
		return Math.min(100.0, (part * 100.0) / total);
	}

	private static String sec(String no, String judul) {
		return "<div class='sec'><span class='sec-no'>" + no + "</span>" + judul + "</div>";
	}

	private static String kpi(String label, String value, String help, String warna, String bg) {
		return "<div class='kpi' style='border-top:3px solid " + warna + ";'>"
				+ "<div class='kpi-label' style='background:" + bg + ";color:" + warna + ";'>" + esc(label) + "</div>"
				+ "<div class='kpi-val'>" + value + "</div>"
				+ "<div class='kpi-help'>" + esc(help) + "</div></div>";
	}

	private static String li(String s) {
		return "<li>" + s + "</li>";
	}

	private static String empty() {
		return "<div class='note'>Tidak ada data pada bagian ini.</div>";
	}

	private static String footer() {
		return "<div class='ftr'>Laporan dihasilkan otomatis oleh Sistem Informasi &middot; "
				+ esc(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(ais.ui.util.WaktuUtil.getDate()))
				+ " &middot; Dokumen ini bersifat internal.</div>";
	}

	private static String esc(String s) {
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

	private static String css() {
		return ""
			+ "*{box-sizing:border-box;} "
			+ "body{font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#0f172a;margin:0;padding:22px 26px;background:#fff;font-size:12px;line-height:1.5;} "
			+ ".kop{border-radius:14px;padding:16px 20px;color:#fff;background:linear-gradient(135deg,#0f766e 0%,#0e7490 55%,#2563eb 100%);margin-bottom:16px;} "
			+ ".kop-eyebrow{font-size:9.5px;letter-spacing:.16em;font-weight:800;opacity:.85;} "
			+ ".kop-judul{font-size:22px;font-weight:900;line-height:1.15;margin-top:3px;} "
			+ ".kop-sub{font-size:11.5px;opacity:.92;margin-top:5px;} "
			+ ".sec{font-size:14px;font-weight:800;color:#0f172a;margin:22px 0 10px;padding-bottom:6px;border-bottom:2px solid #e2e8f0;display:flex;align-items:center;gap:8px;} "
			+ ".sec-no{display:inline-flex;align-items:center;justify-content:center;width:22px;height:22px;border-radius:7px;background:#0e7490;color:#fff;font-size:12px;font-weight:900;} "
			+ ".subjudul{font-weight:700;color:#334155;margin:14px 0 6px;font-size:12.5px;} "
			+ ".kpi-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;} "
			+ ".kpi{background:#fff;border:1px solid #e5e7eb;border-radius:12px;padding:10px 12px;box-shadow:0 1px 4px rgba(15,23,42,.05);} "
			+ ".kpi-label{display:inline-block;border-radius:999px;padding:2px 9px;font-size:9px;font-weight:900;text-transform:uppercase;letter-spacing:.04em;} "
			+ ".kpi-val{font-size:23px;font-weight:900;margin-top:6px;line-height:1;} .kpi-val .mini{font-size:12px;color:#64748b;font-weight:700;} "
			+ ".kpi-help{font-size:10px;color:#64748b;margin-top:4px;} "
			+ ".tbl{width:100%;border-collapse:collapse;margin-top:8px;font-size:11px;} "
			+ ".tbl th{background:#0e7490;color:#fff;text-align:left;padding:7px 8px;font-size:10.5px;font-weight:700;} "
			+ ".tbl td{padding:6px 8px;border-bottom:1px solid #eef2f7;} "
			+ ".tbl tbody tr:nth-child(even){background:#f8fafc;} "
			+ ".tbl .r{text-align:right;} .tbl .b{font-weight:800;} "
			+ ".bad{color:#b91c1c;font-weight:800;} .mini{font-size:11px;font-weight:600;} "
			+ ".bc-track{height:11px;background:#f1f5f9;border-radius:999px;overflow:hidden;} .bc-track.sm{height:9px;} "
			+ ".bc-fill{height:100%;background:linear-gradient(90deg,#0e7490,#2563eb);border-radius:999px;} "
			+ ".barchart{display:flex;flex-direction:column;gap:6px;margin-top:8px;} "
			+ ".bc-row{display:grid;grid-template-columns:110px 1fr 150px;gap:10px;align-items:center;} "
			+ ".bc-lbl{font-size:11px;font-weight:700;color:#334155;} .bc-val{font-size:11px;font-weight:700;text-align:right;} "
			+ ".bc-bad{color:#b91c1c;font-weight:700;font-size:10px;} "
			+ ".jamgrid{display:flex;align-items:flex-end;gap:3px;height:130px;margin-top:8px;padding:6px;border:1px solid #eef2f7;border-radius:10px;} "
			+ ".jam-col{flex:1;display:flex;flex-direction:column;align-items:center;height:100%;} "
			+ ".jam-bar-wrap{flex:1;width:100%;display:flex;align-items:flex-end;} "
			+ ".jam-bar{width:100%;background:#38bdf8;border-radius:3px 3px 0 0;} .jam-peak{background:#2563eb;} "
			+ ".jam-lbl{font-size:8.5px;color:#94a3b8;margin-top:3px;} "
			+ ".dua-kolom{display:grid;grid-template-columns:280px 1fr;gap:16px;align-items:center;} "
			+ ".donut-wrap{display:flex;flex-direction:column;align-items:center;gap:8px;} "
			+ ".donut{width:150px;height:150px;border-radius:50%;display:flex;align-items:center;justify-content:center;} "
			+ ".donut-hole{width:96px;height:96px;border-radius:50%;background:#fff;display:flex;flex-direction:column;align-items:center;justify-content:center;} "
			+ ".donut-pct{font-size:26px;font-weight:900;color:#0e7490;} .donut-cap{font-size:9px;letter-spacing:.1em;color:#64748b;font-weight:700;} "
			+ ".donut-legend{font-size:11px;} .donut-legend div{margin:2px 0;} .dot{display:inline-block;width:10px;height:10px;border-radius:3px;margin-right:6px;vertical-align:middle;} "
			+ ".analisis{margin:6px 0 0 0;padding-left:18px;} .analisis li{margin:5px 0;} "
			+ ".rekom{margin-top:12px;background:#f0fdfa;border:1px solid #99f6e4;border-radius:10px;padding:10px 14px;} "
			+ ".rekom-judul{font-weight:800;color:#0f766e;margin-bottom:4px;} .rekom ul{margin:0;padding-left:18px;} .rekom li{margin:3px 0;} "
			+ ".note{font-size:10.5px;color:#64748b;margin-top:6px;font-style:italic;} "
			+ ".kosong{padding:40px;text-align:center;color:#64748b;font-size:14px;} "
			+ ".ftr{margin-top:26px;padding-top:10px;border-top:1px solid #e2e8f0;font-size:10px;color:#94a3b8;text-align:center;} "
			+ "@media print{body{padding:0;font-size:11px;-webkit-print-color-adjust:exact;print-color-adjust:exact;} "
			+ ".sec{page-break-after:avoid;} .tbl{page-break-inside:auto;} .tbl tr{page-break-inside:avoid;} "
			+ ".kop,.kpi,.rekom,.donut{-webkit-print-color-adjust:exact;print-color-adjust:exact;}} "
			+ "@page{size:A4;margin:12mm;}";
	}
}
