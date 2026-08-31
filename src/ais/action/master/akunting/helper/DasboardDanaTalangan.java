package ais.action.master.akunting.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Html;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DanaTalangan;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.DashboardUiKit.Stat;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>DasboardDanaTalangan — Dasbor Pemantauan Dana Talangan (HTML/CSS, reuse DashboardUiKit)</h3>
 *
 * <p><b>Untuk apa:</b> Satu layar ringkas untuk memantau dana talangan (dana yang
 * ditalangi lebih dulu dan menunggu proses persetujuan/realisasi): berapa total
 * nilainya, berapa yang sudah disetujui, berapa yang MASIH MENUNGGU persetujuan
 * (paling perlu ditindaklanjuti), berapa yang ditolak, rata-rata berapa lama
 * persetujuannya, sebaran per unit kerja, dan trennya tiap bulan. Dipasang
 * sebagai tab "Dasbor" pada modul Dana Talangan.</p>
 *
 * <p><b>Desain:</b> Sama dengan {@link DasboardPajak} — semua grafik HTML/CSS/SVG
 * lewat {@link DashboardUiKit} (TANPA JFreeChart), filter periode, perhitungan
 * asinkron ({@link Timer}), cache dua lapis ({@link DashboardCacheUtil}), dan
 * tata letak responsif. Java 1.6/1.7. Banyak getter {@link DanaTalangan} bersifat
 * turunan, jadi setiap pembacaan dibungkus try-catch.</p>
 */
public final class DasboardDanaTalangan {

	private static final int SAMPLE_LIMIT = 8000;

	private DasboardDanaTalangan() {
	}

	// ============================================================
	//  ENTRY
	// ============================================================

	public static void render(final Component parent) {
		if (parent == null) {
			return;
		}
		Common.clear(parent);

		Vbox box = new Vbox();
		box.setWidth("100%");
		box.setStyle("padding:12px; box-sizing:border-box; background:#f6f8fb;");
		box.setParent(parent);

		final org.zkoss.zul.Div filterCard = new org.zkoss.zul.Div();
		filterCard.setStyle("margin-bottom:10px; padding:10px 12px; background:#ffffff; border:1px solid #e8eef6;"
				+ "border-radius:14px; box-shadow:0 8px 20px rgba(15,23,42,.04); box-sizing:border-box;");
		filterCard.setParent(box);

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("border:0; background:transparent; padding:0; display:flex; flex-wrap:wrap;"
				+ " align-items:center; gap:8px;");
		toolbar.setParent(filterCard);

		new MyLabelAgakKecil("Periode pengajuan — Mulai:").setParent(toolbar);
		final MyDatebox dbMulai = new MyDatebox();
		dbMulai.setReadonly(true);
		dbMulai.setCols(6);
		dbMulai.setParent(toolbar);

		new MyLabelAgakKecil("Sampai:").setParent(toolbar);
		final MyDatebox dbSampai = new MyDatebox();
		dbSampai.setReadonly(true);
		dbSampai.setCols(6);
		dbSampai.setParent(toolbar);

		MyToolbarbuttonConfig btnTampil = new MyToolbarbuttonConfig("Tampilkan", "/img/svg/search.svg");
		btnTampil.setTooltiptext("Tampilkan dasbor dana talangan sesuai periode (kosongkan untuk semua data)");
		btnTampil.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px;"
				+ " padding:6px 14px; margin-left:4px;");
		btnTampil.setParent(toolbar);

		final Html slot = new Html(loadingHtml());
		slot.setParent(box);

		EventListener tampilListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				computeAsync(slot, dbMulai.getValue(), dbSampai.getValue());
			}
		};
		btnTampil.addEventListener("onClick", tampilListener);
		dbMulai.addEventListener("onChange", tampilListener);
		dbSampai.addEventListener("onChange", tampilListener);

		computeAsync(slot, null, null);
	}

	private static void computeAsync(final Html slot, final Date mulai, final Date sampai) {
		if (slot == null) {
			return;
		}
		slot.setContent(loadingHtml());
		final Timer timer = new Timer();
		timer.setDelay(150);
		timer.setRepeats(false);
		timer.setParent(slot.getParent());
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					slot.setContent(htmlBercache(mulai, sampai));
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
					slot.setContent("<div style='padding:18px;border-radius:14px;background:#fff7ed;"
							+ "border:1px solid #fed7aa;color:#9a3412;font-size:12px;'>"
							+ "Dasbor dana talangan belum dapat dimuat.</div>");
				}
				try {
					timer.stop();
					timer.detach();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:139");
				}
			}
		});
		timer.start();
	}

	private static String loadingHtml() {
		return "<div style='padding:22px;border-radius:16px;background:#ffffff;border:1px solid #e5e7eb;"
				+ "box-shadow:0 12px 26px rgba(15,23,42,.07);color:#0f172a;'>"
				+ "<div style='font-size:11px;letter-spacing:.12em;text-transform:uppercase;color:#2563eb;font-weight:900;'>Dasbor Dana Talangan</div>"
				+ "<div style='font-size:17px;font-weight:900;margin-top:6px;'>Menghitung ringkasan dana talangan…</div>"
				+ "<div style='font-size:12px;color:#64748b;margin-top:6px;'>Mohon tunggu sebentar, data sedang dirangkum.</div>"
				+ "<div style='height:10px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:14px;'>"
				+ "<div style='height:10px;width:55%;border-radius:999px;background:linear-gradient(90deg,var(--ais-theme-primary,#2563eb),var(--ais-theme-accent,#06b6d4));'></div>"
				+ "</div></div>";
	}

	// ============================================================
	//  CACHE
	// ============================================================

	private static String htmlBercache(Date mulai, Date sampai) {
		String fp = (mulai == null ? "0" : String.valueOf(mulai.getTime())) + "_"
				+ (sampai == null ? "0" : String.valueOf(sampai.getTime()));
		String key = DashboardCacheUtil.keyWithFilter("DasboardDanaTalangan", "ADMIN", null, fp);
		Object l2 = DashboardCacheUtil.getL2(key);
		if (l2 instanceof String) {
			return (String) l2;
		}
		Object l3 = DashboardCacheUtil.getL3(key);
		if (l3 instanceof String) {
			DashboardCacheUtil.putL2(key, l3);
			return (String) l3;
		}
		String html = buildHtml(load(mulai, sampai), mulai, sampai);
		DashboardCacheUtil.putL2(key, html);
		DashboardCacheUtil.putL3(key, html);
		return html;
	}

	// ============================================================
	//  DATA
	// ============================================================

	/**
	 * Tipe implementasi bersarang {@link Data} milik {@link DasboardDanaTalangan}. Kelas ini memberi nama pada
	 * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardDanaTalangan}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int count}, {@code double
	 * totalNilai}, {@code double disetujuiNilai}, {@code double menungguNilai}, {@code double ditolakNilai},
	 * {@code int jmlDisetujui}, {@code int jmlMenunggu}, {@code int jmlDitolak}. Aturan bisnis bersama tetap
	 * berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see DasboardDanaTalangan
	 */
	private static final class Data {
		int count;
		double totalNilai;
		double disetujuiNilai;
		double menungguNilai;
		double ditolakNilai;
		int jmlDisetujui;
		int jmlMenunggu;
		int jmlDitolak;
		int jmlPosting;
		long sumHariSetujui;
		int cntHariSetujui;
		final Map<String, Double> perStatus = new HashMap<String, Double>();
		final Map<String, Double> perSatker = new HashMap<String, Double>();
		final Map<String, Double> perJenis = new HashMap<String, Double>();
		final Map<String, Double> perPemohon = new HashMap<String, Double>();
		final Map<String, Double> menungguPerNama = new HashMap<String, Double>();
		final TreeMap<String, Double> bulanNilai = new TreeMap<String, Double>();
		boolean capped;
	}

	@SuppressWarnings("unchecked")
	private static Data load(Date mulai, Date sampai) {
		Data d = new Data();
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Criteria c = session.createCriteria(DanaTalangan.class).addOrder(Order.desc("id"))
					.setMaxResults(SAMPLE_LIMIT);
			List<DanaTalangan> rows = c.list();
			d.capped = rows != null && rows.size() >= SAMPLE_LIMIT;
			if (rows != null) {
				for (int idx = 0; idx < rows.size(); idx++) {
					DanaTalangan p = rows.get(idx);
					if (p == null) {
						continue;
					}

					Date tgl = null;
					try {
						tgl = p.getTanggalPembuatan();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:225");
					}
					if (mulai != null && tgl != null && tgl.before(mulai)) {
						continue;
					}
					if (sampai != null && tgl != null && tgl.after(sampai)) {
						continue;
					}

					double nilai = 0;
					try {
						Double n = p.getNilai();
						nilai = n == null ? 0 : n.doubleValue();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:238");
					}
					d.count++;
					d.totalNilai += nilai;

					String status = DanaTalangan.PENGAJUAN;
					try {
						if (p.getStatus() != null) {
							status = p.getStatus();
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:248");
					}
					add(d.perStatus, status, nilai);
					if (DanaTalangan.DISETUJU.equals(status)) {
						d.jmlDisetujui++;
						d.disetujuiNilai += nilai;
					} else if (DanaTalangan.DITOLAK.equals(status)) {
						d.jmlDitolak++;
						d.ditolakNilai += nilai;
					} else {
						d.jmlMenunggu++;
						d.menungguNilai += nilai;
						add(d.menungguPerNama, safeNama(p), nilai);
					}

					String satker = "Tanpa Satuan Kerja";
					try {
						if (p.getSatuanKerja() != null && p.getSatuanKerja().getNama() != null) {
							satker = p.getSatuanKerja().getNama();
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:268");
					}
					add(d.perSatker, satker, nilai);

					String jenis = "Lainnya";
					try {
						if (p.getJenisUangMuka() != null && p.getJenisUangMuka().getNama() != null) {
							jenis = p.getJenisUangMuka().getNama();
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:277");
					}
					add(d.perJenis, jenis, nilai);

					String pemohon = "Tidak diketahui";
					try {
						if (p.getDibuatOleh() != null && p.getDibuatOleh().getUserNama() != null) {
							pemohon = p.getDibuatOleh().getUserNama();
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:286");
					}
					add(d.perPemohon, pemohon, nilai);

					String bln = "?";
					try {
						if (tgl != null) {
							String f = Common.databaseDateFormat.get().format(tgl);
							if (f != null && f.length() >= 7) {
								bln = f.substring(0, 7);
							}
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:298");
					}
					add(d.bulanNilai, bln, nilai);

					try {
						if (p.getPostingHistory() != null) {
							d.jmlPosting++;
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:306");
					}

					try {
						if (DanaTalangan.DISETUJU.equals(status) && tgl != null && p.getTanggalPersetujuan() != null) {
							long ms = p.getTanggalPersetujuan().getTime() - tgl.getTime();
							if (ms >= 0) {
								d.sumHariSetujui += Math.round(ms / 86400000.0);
								d.cntHariSetujui++;
							}
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:317");
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(session);
		}
		return d;
	}

	private static String safeNama(DanaTalangan p) {
		try {
			String n = p.getNama();
			if (n != null && n.trim().length() > 0) {
				return n.trim();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:335");
		}
		try {
			String k = p.getKode();
			if (k != null && k.trim().length() > 0) {
				return k.trim();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:342");
		}
		return "-";
	}

	// ============================================================
	//  BUILD HTML
	// ============================================================

	private static String buildHtml(Data d, Date mulai, Date sampai) {
		StringBuilder sb = new StringBuilder();

		sb.append(DashboardUiKit.introBanner("Dasbor Dana Talangan",
				"Pantau dana yang ditalangi lebih dulu: berapa totalnya, sudah disetujui berapa, mana yang masih "
						+ "menunggu persetujuan, dan berapa lama biasanya proses persetujuannya."));

		sb.append(periodeChip(mulai, sampai, d.count));

		if (d.capped) {
			sb.append("<div style='font-size:11px;color:#92400e;background:#fffbeb;border:1px solid #fde68a;"
					+ "border-radius:10px;padding:8px 10px;margin-bottom:10px;'>"
					+ "Data sangat banyak — dasbor menampilkan " + fmtInt(SAMPLE_LIMIT) + " pengajuan terbaru.</div>");
		}

		List<Stat> stats = new ArrayList<Stat>();
		stats.add(new Stat("Jumlah Pengajuan", fmtInt(d.count), "Banyaknya pengajuan dana talangan", "#2563eb"));
		stats.add(new Stat("Total Nilai", rp(d.totalNilai), "Total semua dana talangan", "#0ea5e9"));
		stats.add(new Stat("Sudah Disetujui", rp(d.disetujuiNilai) + " (" + pctStr(d.disetujuiNilai, d.totalNilai) + ")",
				fmtInt(d.jmlDisetujui) + " pengajuan", "#16a34a"));
		stats.add(new Stat("Menunggu Persetujuan",
				rp(d.menungguNilai) + " (" + pctStr(d.menungguNilai, d.totalNilai) + ")",
				fmtInt(d.jmlMenunggu) + " pengajuan — perlu tindak lanjut", "#dc2626"));
		stats.add(new Stat("Ditolak", fmtInt(d.jmlDitolak), rp(d.ditolakNilai), "#9333ea"));
		stats.add(new Stat("Rata-rata Lama Disetujui",
				d.cntHariSetujui > 0 ? (Math.round(d.sumHariSetujui * 1.0 / d.cntHariSetujui) + " hari") : "-",
				"Dari diajukan sampai disetujui", "#ea580c"));
		sb.append(DashboardUiKit.cards(stats));

		List<String> blnKeys = new ArrayList<String>(d.bulanNilai.keySet());
		List<String> blnLabel = new ArrayList<String>();
		List<Double> seriNilai = new ArrayList<Double>();
		for (int i = 0; i < blnKeys.size(); i++) {
			String k = blnKeys.get(i);
			blnLabel.add(prettyMonth(k));
			seriNilai.add(d.bulanNilai.get(k));
		}

		sb.append(DashboardUiKit.openGrid(320));

		sb.append(DashboardUiKit.sparkline("Tren Nilai Dana Talangan per Bulan",
				"Naik-turun nilai pengajuan dana talangan dari bulan ke bulan.", seriNilai, "#2563eb",
				"Belum ada data untuk ditampilkan."));

		sb.append(DashboardUiKit.donut("Komposisi per Status",
				"Bagian mana yang sudah disetujui, masih menunggu, atau ditolak.", statusDonut(d), true,
				"Belum ada data dana talangan."));

		sb.append(DashboardUiKit.barList("Menunggu Persetujuan (Perlu Ditindaklanjuti)",
				"Pengajuan bernilai terbesar yang masih menunggu persetujuan — dahulukan ini.",
				topNMoney(d.menungguPerNama, 8), "#dc2626", null, true,
				"Bagus — tidak ada pengajuan yang menggantung."));

		sb.append(DashboardUiKit.barList("Dana Talangan per Satuan Kerja",
				"Unit kerja mana yang paling banyak nilai dana talangannya.", topNMoney(d.perSatker, 8), "#0ea5e9",
				null, true, "Belum ada data satuan kerja."));

		sb.append(DashboardUiKit.progressLines("Ringkasan Proses",
				"Seberapa besar bagian yang disetujui, ditolak, dan sudah dibukukan (posting).", prosesMap(d)));

		sb.append(DashboardUiKit.insight("Sorotan Penting", "Hal-hal yang sebaiknya diperhatikan.", insightPairs(d)));

		sb.append(DashboardUiKit.closeGrid());
		return sb.toString();
	}

	private static LinkedHashMap<String, Double> statusDonut(Data d) {
		LinkedHashMap<String, Double> m = new LinkedHashMap<String, Double>();
		if (d.disetujuiNilai > 0) {
			m.put("Disetujui", Double.valueOf(d.disetujuiNilai));
		}
		if (d.menungguNilai > 0) {
			m.put("Menunggu", Double.valueOf(d.menungguNilai));
		}
		if (d.ditolakNilai > 0) {
			m.put("Ditolak", Double.valueOf(d.ditolakNilai));
		}
		return m;
	}

	private static LinkedHashMap<String, Integer> prosesMap(Data d) {
		LinkedHashMap<String, Integer> m = new LinkedHashMap<String, Integer>();
		m.put("Disetujui", Integer.valueOf(pctInt(d.jmlDisetujui, d.count)));
		m.put("Menunggu", Integer.valueOf(pctInt(d.jmlMenunggu, d.count)));
		m.put("Ditolak", Integer.valueOf(pctInt(d.jmlDitolak, d.count)));
		m.put("Sudah Dibukukan", Integer.valueOf(pctInt(d.jmlPosting, d.count)));
		return m;
	}

	private static LinkedHashMap<String, String> insightPairs(Data d) {
		LinkedHashMap<String, String> m = new LinkedHashMap<String, String>();
		Map.Entry<String, Double> topPemohon = topEntry(d.perPemohon);
		Map.Entry<String, Double> topSatker = topEntry(d.perSatker);
		Map.Entry<String, Double> topJenis = topEntry(d.perJenis);
		m.put("Pemohon Teratas", topPemohon == null ? "-" : topPemohon.getKey() + "  (" + rp(topPemohon.getValue()) + ")");
		m.put("Satuan Kerja Teratas", topSatker == null ? "-" : topSatker.getKey() + "  (" + rp(topSatker.getValue()) + ")");
		m.put("Jenis Dominan", topJenis == null ? "-" : topJenis.getKey() + "  (" + rp(topJenis.getValue()) + ")");
		m.put("Menunggu Persetujuan", fmtInt(d.jmlMenunggu) + " pengajuan (" + rp(d.menungguNilai) + ")");
		return m;
	}

	// ============================================================
	//  HELPER
	// ============================================================

	private static String periodeChip(Date mulai, Date sampai, int count) {
		String teks;
		if (mulai == null && sampai == null) {
			teks = "Semua periode";
		} else {
			teks = (mulai == null ? "Awal" : tgl(mulai)) + " s.d. " + (sampai == null ? "Sekarang" : tgl(sampai));
		}
		return "<div style='display:flex;flex-wrap:wrap;gap:8px;margin-bottom:10px;'>"
				+ "<span style='font-size:11px;font-weight:800;color:#1d4ed8;background:#eff6ff;border:1px solid #bfdbfe;"
				+ "border-radius:999px;padding:5px 11px;'>Periode: " + esc(teks) + "</span>"
				+ "<span style='font-size:11px;font-weight:800;color:#166534;background:#ecfdf5;border:1px solid #bbf7d0;"
				+ "border-radius:999px;padding:5px 11px;'>" + fmtInt(count) + " pengajuan</span></div>";
	}

	private static void add(Map<String, Double> map, String key, double val) {
		if (key == null || key.trim().length() == 0) {
			key = "Lainnya";
		}
		Double cur = map.get(key);
		map.put(key, Double.valueOf((cur == null ? 0 : cur.doubleValue()) + val));
	}

	private static LinkedHashMap<String, Double> topNMoney(Map<String, Double> map, int n) {
		List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			@Override
			public int compare(Map.Entry<String, Double> a, Map.Entry<String, Double> b) {
				return Double.compare(b.getValue() == null ? 0 : b.getValue(), a.getValue() == null ? 0 : a.getValue());
			}
		});
		LinkedHashMap<String, Double> out = new LinkedHashMap<String, Double>();
		double lainnya = 0;
		for (int i = 0; i < list.size(); i++) {
			if (i < n) {
				out.put(list.get(i).getKey(), list.get(i).getValue());
			} else {
				lainnya += list.get(i).getValue() == null ? 0 : list.get(i).getValue();
			}
		}
		if (lainnya > 0) {
			out.put("Lainnya", Double.valueOf(lainnya));
		}
		return out;
	}

	private static Map.Entry<String, Double> topEntry(Map<String, Double> map) {
		Map.Entry<String, Double> top = null;
		for (Map.Entry<String, Double> e : map.entrySet()) {
			if (top == null || (e.getValue() != null && e.getValue() > (top.getValue() == null ? 0 : top.getValue()))) {
				top = e;
			}
		}
		return top;
	}

	private static int pctInt(int part, int total) {
		if (total <= 0) {
			return 0;
		}
		int v = (int) Math.round(part * 100.0 / total);
		return v < 0 ? 0 : (v > 100 ? 100 : v);
	}

	private static String pctStr(double part, double total) {
		if (total <= 0) {
			return "0%";
		}
		long v = Math.round(part * 100.0 / total);
		if (v < 0) {
			v = 0;
		}
		return v + "%";
	}

	private static String rp(double v) {
		try {
			return "Rp " + Common.numberFormat.get().format(Math.round(v));
		} catch (Exception e) {
			return "Rp " + (long) v;
		}
	}

	private static String fmtInt(int v) {
		try {
			return Common.numberFormat.get().format(v);
		} catch (Exception e) {
			return String.valueOf(v);
		}
	}

	private static String tgl(Date d) {
		try {
			return Common.dateFormat1.get().format(d);
		} catch (Exception e) {
			return String.valueOf(d);
		}
	}

	private static String esc(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static final String[] BULAN = { "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt",
			"Nov", "Des" };

	private static String prettyMonth(String yyyymm) {
		try {
			if (yyyymm != null && yyyymm.length() >= 7 && yyyymm.charAt(4) == '-') {
				int m = Integer.parseInt(yyyymm.substring(5, 7));
				String yy = yyyymm.substring(2, 4);
				if (m >= 1 && m <= 12) {
					return BULAN[m - 1] + " " + yy;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:573");
		}
		return yyyymm == null ? "?" : yyyymm;
	}

	private static void closeSession(Session session) {
		if (session != null) {
			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:582");
			}
			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:586");
			}
		}
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardDanaTalangan.java:591");
		}
	}
}
