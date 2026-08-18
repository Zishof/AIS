package ais.action.master;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyGrid;

/**
 * Monitor "Pemakaian Flag/Cache".
 *
 * Sumber data: tabel native ais_flags_data_baru (dikelola oleh ais.common.BacaTulisUtil),
 * kolom: flag_key (PK), flag_key_parent_folder, flag_value (TEXT). Tabel ini BUKAN entity
 * Hibernate dan tidak punya kolom id/tanggal, sehingga semua agregasi memakai native SQL
 * (PostgreSQL). Halaman bersifat read-only (pemantauan), tidak mengubah flag operasional.
 *
 * Gaya Java 1.6/1.7 (tanpa lambda/stream/diamond), ZK 5.5. Memakai HibernateUtil.currentSession()
 * untuk baca → TIDAK ditutup manual (sesuai aturan session). Semua query dibungkus try/catch agar
 * tetap tampil walau tabel belum terbentuk (belum ada flag yang ditulis).
 */
public class FlagCacheInfoAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	private Paging paging;
	private MyGrid grid;
	private Html dashboardHtml;
	private Html progressHtml;
	private Textbox searchnama;

	private static final String TABLE = "ais_flags_data_baru";

	/**
	 * TTL cache dasbor flag = 10 menit. Query agregat penuh tabel {@code ais_flags_data_baru}
	 * (count + SUM(octet_length) + COUNT(DISTINCT) + GROUP BY) adalah FULL TABLE SCAN yang berat
	 * (teramati ~8 detik pada tabel besar). Datanya berubah lambat, sehingga cukup dihitung sekali
	 * per 10 menit per host; tombol "Refresh" memaksa hitung ulang (force=true).
	 */
	private static final int TTL_DASBOR_FLAG_MS = 10 * 60 * 1000;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (progressHtml != null) {
			progressHtml.setVisible(false);
		}

		refreshDashboardSafe(false);
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	private String ambilSearch() {
		return (searchnama == null || searchnama.getValue() == null) ? "" : searchnama.getValue().trim();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onSearchDefault(Event event) {
		String search = ambilSearch();

		long total = countFlags(search);
		if (paging != null) {
			paging.setPageSize(Common.ROWS_COUNT_ON_PAGE);
			paging.setTotalSize((int) total);
		}
		int active = paging == null ? 0 : paging.getActivePage();

		List<Object[]> rows = loadFlags(search, Common.ROWS_COUNT_ON_PAGE, Common.ROWS_COUNT_ON_PAGE * active);
		ListModel model = new SimpleListModel(rows);
		grid.setRowRenderer(new FlagRenderer());
		grid.setModelCheckMobile(model);

		refreshDashboardSafe(false);
	}

	class FlagRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			Object[] r = (Object[]) arg1;
			String parent = r.length > 0 && r[0] != null ? r[0].toString() : "";
			String key = r.length > 1 && r[1] != null ? r[1].toString() : "";
			String val = r.length > 2 && r[2] != null ? r[2].toString() : "";

			new Label(parent).setParent(arg0);
			new Label(key).setParent(arg0);

			String preview = val.length() > 160 ? (val.substring(0, 160) + " ...") : val;
			new Label(preview).setParent(arg0);

			new Label(Common.numberFormat.get().format(val.length()) + " B").setParent(arg0);
		}
	}

	// ===================================================================================
	// NATIVE SQL (currentSession — tidak ditutup manual)
	// ===================================================================================

	/**
	 * Cek keberadaan tabel TANPA memicu error PostgreSQL. {@code to_regclass} mengembalikan
	 * NULL bila tabel tidak ada (tidak melempar "relation does not exist"), sehingga aman
	 * dipakai sebagai guard sebelum query agregasi.
	 */
	private boolean tabelFlagAda(Session s) {
		try {
			// WAJIB CAST ke text — to_regclass mengembalikan tipe regclass (JDBC 1111/OTHER)
			// yang tak bisa dipetakan Hibernate. Pakai CAST(... AS text), BUKAN '::text'
			// karena '::' membuat Hibernate menyangka ada named-parameter ":text".
			Object o = s.createSQLQuery("SELECT CAST(to_regclass('" + TABLE + "') AS text)").uniqueResult();
			return o != null;
		} catch (Exception e) {
			return false;
		}
	}

	/** Tutup session dedicated dengan aman (disconnect + close di finally). */
	private void tutupSesi(Session s) {
		if (s == null) {
			return;
		}
		try {
			if (s.isConnected()) {
				s.disconnect();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/FlagCacheInfoAction.java:157");
		}
		try {
			if (s.isOpen()) {
				s.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/FlagCacheInfoAction.java:163");
		}
	}

	private long countFlags(String search) {
		// Session DEDICATED (openSession) — bila tabel belum ada, error PostgreSQL hanya
		// membatalkan transaksi session buang ini, TIDAK meracuni transaksi request bersama.
		Session s = null;
		try {
			s = HibernateUtil.openSession();
			if (!tabelFlagAda(s)) {
				return 0L;
			}
			StringBuilder sql = new StringBuilder("SELECT count(*) FROM " + TABLE);
			boolean adaSearch = search != null && !search.isEmpty();
			if (adaSearch) {
				sql.append(" WHERE flag_key ILIKE :q OR flag_key_parent_folder ILIKE :q");
			}
			SQLQuery q = s.createSQLQuery(sql.toString());
			if (adaSearch) {
				q.setParameter("q", "%" + search + "%");
			}
			Object o = q.uniqueResult();
			return o == null ? 0L : ((Number) o).longValue();
		} catch (Exception e) {
			// Tabel mungkin belum terbentuk (belum ada flag tertulis) → anggap kosong.
			return 0L;
		} finally {
			tutupSesi(s);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<Object[]> loadFlags(String search, int limit, int offset) {
		List<Object[]> out = new ArrayList<Object[]>();
		Session s = null;
		try {
			s = HibernateUtil.openSession();
			if (!tabelFlagAda(s)) {
				return out;
			}
			StringBuilder sql = new StringBuilder(
					"SELECT flag_key_parent_folder, flag_key, flag_value FROM " + TABLE);
			boolean adaSearch = search != null && !search.isEmpty();
			if (adaSearch) {
				sql.append(" WHERE flag_key ILIKE :q OR flag_key_parent_folder ILIKE :q");
			}
			sql.append(" ORDER BY flag_key_parent_folder, flag_key LIMIT :lim OFFSET :off");

			SQLQuery q = s.createSQLQuery(sql.toString());
			if (adaSearch) {
				q.setParameter("q", "%" + search + "%");
			}
			q.setParameter("lim", limit);
			q.setParameter("off", offset);

			List list = q.list();
			if (list != null) {
				for (Object o : list) {
					out.add((Object[]) o);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/FlagCacheInfoAction.java:225");
			// abaikan — tampil kosong bila tabel belum ada
		} finally {
			tutupSesi(s);
		}
		return out;
	}

	// ===================================================================================
	// DASBOR (DashboardUiKit — HTML/CSS murni, tanpa JFreeChart)
	// ===================================================================================

	private void refreshDashboardSafe(boolean force) {
		try {
			if (dashboardHtml != null) {
				dashboardHtml.setContent(buildDashboard(force));
				dashboardHtml.setVisible(true);
			}
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/FlagCacheInfoAction.java:246");
			}
		}
	}

	/**
	 * Handler tombol "Refresh": PAKSA hitung ulang dasbor (abaikan cache L3) lalu segarkan daftar.
	 * Hanya jalur inilah yang menjalankan kembali query agregat berat itu; pemuatan halaman & Cari
	 * biasa cukup membaca hasil dari cache.
	 */
	public void onRefreshDashboard(Event event) {
		refreshDashboardSafe(true);
		onSearchDefault(event);
	}

	/**
	 * Kunci cache dasbor flag — di-scope per HOST (tenant). Tabel {@code ais_flags_data_baru} berbeda
	 * per database/domain, sedangkan cache L3 bersifat app-wide; tanpa scope host, angka satu tenant
	 * bisa bocor ke tenant lain. Aman bila host kosong (fallback "default").
	 */
	private String cacheKeyDasbor() {
		String host = null;
		try {
			host = Common.getRequestHost();
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/FlagCacheInfoAction.java:270");
		}
		if (host == null || host.trim().isEmpty()) {
			host = "default";
		}
		return "FlagCacheDash::" + host;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String buildDashboard(boolean force) {
		final String cacheKey = cacheKeyDasbor();
		if (!force) {
			try {
				Object cached = DashboardCacheUtil.getL3(cacheKey);
				if (cached instanceof String) {
					return (String) cached;
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/FlagCacheInfoAction.java:287");
			}
		}

		long total = 0L;
		long totalSize = 0L;
		long folders = 0L;
		long kosong = 0L;
		boolean computed = false;
		LinkedHashMap<String, Number> perFolderCount = new LinkedHashMap<String, Number>();
		LinkedHashMap<String, Number> perFolderSize = new LinkedHashMap<String, Number>();

		Session s = null;
		try {
			s = HibernateUtil.openSession();
			if (!tabelFlagAda(s)) {
				// Tabel belum dibuat (belum ada flag yg ditulis BacaTulisUtil) →
				// render dasbor kosong (nilai 0), bukan melempar error / meracuni transaksi.
				return buildDashboardHtml(0L, 0L, 0L, 0L, perFolderCount, perFolderSize);
			}

			Object[] agg = (Object[]) s.createSQLQuery(
					"SELECT count(*), COALESCE(SUM(octet_length(flag_value)),0), "
							+ "COUNT(DISTINCT flag_key_parent_folder), "
							+ "COALESCE(SUM(CASE WHEN flag_value IS NULL OR flag_value='' THEN 1 ELSE 0 END),0) "
							+ "FROM " + TABLE).uniqueResult();
			if (agg != null) {
				total = agg[0] == null ? 0L : ((Number) agg[0]).longValue();
				totalSize = agg[1] == null ? 0L : ((Number) agg[1]).longValue();
				folders = agg[2] == null ? 0L : ((Number) agg[2]).longValue();
				kosong = agg[3] == null ? 0L : ((Number) agg[3]).longValue();
			}

			List listC = s.createSQLQuery("SELECT flag_key_parent_folder, count(*) FROM " + TABLE
					+ " GROUP BY flag_key_parent_folder ORDER BY count(*) DESC LIMIT 8").list();
			if (listC != null) {
				for (Object o : listC) {
					Object[] r = (Object[]) o;
					String k = r[0] == null ? "(root)" : r[0].toString();
					perFolderCount.put(k, (Number) r[1]);
				}
			}

			List listS = s.createSQLQuery("SELECT flag_key_parent_folder, COALESCE(SUM(octet_length(flag_value)),0) FROM "
					+ TABLE + " GROUP BY flag_key_parent_folder ORDER BY 2 DESC LIMIT 8").list();
			if (listS != null) {
				for (Object o : listS) {
					Object[] r = (Object[]) o;
					String k = r[0] == null ? "(root)" : r[0].toString();
					perFolderSize.put(k, (Number) r[1]);
				}
			}
			computed = true;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/FlagCacheInfoAction.java:340");
			// Tabel mungkin belum terbentuk → tampilkan dasbor kosong (nilai 0).
		} finally {
			tutupSesi(s);
		}

		String html = buildDashboardHtml(total, totalSize, folders, kosong, perFolderCount, perFolderSize);
		// Hanya cache hasil yang BENAR-BENAR terhitung (jangan cache kondisi error/tabel belum ada).
		if (computed) {
			try {
				DashboardCacheUtil.putL3(cacheKey, html, TTL_DASBOR_FLAG_MS);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/FlagCacheInfoAction.java:351");
			}
		}
		return html;
	}

	/** Bangun HTML dasbor dari angka agregat (dipisah agar bisa dipakai jalur tabel-kosong). */
	private String buildDashboardHtml(long total, long totalSize, long folders, long kosong,
			LinkedHashMap<String, Number> perFolderCount, LinkedHashMap<String, Number> perFolderSize) {
		long terisi = total - kosong;
		if (terisi < 0) {
			terisi = 0;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(DashboardUiKit.introBanner("Dasbor Pemakaian Flag/Cache",
				"Ringkasan flag & cache sistem yang tersimpan di tabel " + TABLE
						+ " agar pemantauan beban penyimpanan flag lebih mudah dipahami."));

		List<DashboardUiKit.Stat> stats = new ArrayList<DashboardUiKit.Stat>();
		stats.add(new DashboardUiKit.Stat("Total Flag", DashboardUiKit.money(total), "Semua baris flag tersimpan",
				DashboardUiKit.PRIMARY));
		stats.add(new DashboardUiKit.Stat("Total Ukuran", formatBytes(totalSize), "Perkiraan ukuran seluruh nilai flag",
				DashboardUiKit.ACCENT));
		stats.add(new DashboardUiKit.Stat("Jumlah Folder", DashboardUiKit.money(folders), "Kelompok parent folder",
				DashboardUiKit.GOOD));
		stats.add(new DashboardUiKit.Stat("Rata-rata Ukuran", total > 0 ? formatBytes(totalSize / total) : "0 B",
				"Per flag", DashboardUiKit.WARN));
		sb.append(DashboardUiKit.cards(stats));

		sb.append(DashboardUiKit.openGrid(320));
		sb.append(DashboardUiKit.barList("Jumlah Flag per Folder", "Folder dengan flag terbanyak (maksimal 8).",
				perFolderCount, DashboardUiKit.PRIMARY, "flag", false, "Belum ada data flag."));
		sb.append(DashboardUiKit.barList("Ukuran Nilai per Folder", "Folder dengan total nilai terbesar (byte, maks 8).",
				perFolderSize, DashboardUiKit.ACCENT, "B", false, "Belum ada data flag."));

		LinkedHashMap<String, Double> donutParts = new LinkedHashMap<String, Double>();
		donutParts.put("Terisi", (double) terisi);
		donutParts.put("Kosong", (double) kosong);
		sb.append(DashboardUiKit.donut("Komposisi Nilai", "Flag yang berisi nilai vs kosong.", donutParts, false,
				"Belum ada data flag."));
		sb.append(DashboardUiKit.closeGrid());

		return sb.toString();
	}

	/** Format byte → B / KB / MB ringkas. */
	private static String formatBytes(long b) {
		if (b < 1024L) {
			return b + " B";
		}
		double kb = b / 1024.0;
		if (kb < 1024.0) {
			return DashboardUiKit.money(Math.round(kb)) + " KB";
		}
		double mb = kb / 1024.0;
		return DashboardUiKit.money(Math.round(mb)) + " MB";
	}
}
