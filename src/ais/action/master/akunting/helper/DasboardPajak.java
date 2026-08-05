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
import org.hibernate.criterion.Restrictions;
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
import ais.database.model.akunting.Pajak;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.DashboardUiKit.Stat;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>DasboardPajak — Dasbor Pemantauan Pajak (HTML/CSS, reuse DashboardUiKit)</h3>
 *
 * <p><b>Untuk apa:</b> Satu layar ringkas untuk memantau pajak yang
 * dipungut/dipertanggungjawabkan: berapa total nilainya, berapa yang sudah
 * disetor ke negara, jenis pajak apa yang paling besar, unit kerja mana yang
 * paling banyak, tren tiap bulan, kelengkapan administrasi (NTPN, NPWP, nama
 * wajib pajak), serta daftar pajak yang BELUM disetor (paling perlu
 * ditindaklanjuti). Dipasang sebagai tab "Dasbor" pada modul Pertanggungjawaban
 * Pajak.</p>
 *
 * <p><b>Desain:</b> Semua grafik dirender sebagai HTML/CSS/SVG murni lewat
 * {@link DashboardUiKit} (TANPA JFreeChart). Tersedia filter periode (tanggal
 * mulai/sampai); perhitungan berjalan asinkron lewat {@link Timer} ZK (tampilkan
 * loading dulu, hitung di latar), lalu hasilnya disimpan di cache dua lapis
 * ({@link DashboardCacheUtil} L2/L3) per kombinasi filter agar pembukaan
 * berikutnya instan. Tata letak responsif (grid auto-fit) sehingga rapi di
 * desktop maupun HP.</p>
 *
 * <p>Java 1.6/1.7, tanpa lambda/stream/diamond. Semua getter {@link Pajak}
 * bersifat turunan (computed) sehingga setiap pembacaan dibungkus try-catch agar
 * satu baris bermasalah tidak menggagalkan keseluruhan dasbor. Filter periode
 * diterapkan di sisi Java (bukan SQL) karena tanggal pajak adalah nilai turunan.</p>
 */
public final class DasboardPajak {

	/** Batas sampel agar dasbor tetap ringan pada tabel pajak yang sangat besar. */
	private static final int SAMPLE_LIMIT = 8000;

	private DasboardPajak() {
	}

	// ============================================================
	//  ENTRY — dipanggil Action saat tab Dasbor dirender
	// ============================================================

	/**
	 * Render dasbor pajak ke dalam {@code parent}: pasang filter periode, tampilkan
	 * loading, lalu hitung di latar lewat Timer dan ganti dengan dasbor sesungguhnya.
	 */
	public static void render(final Component parent) {
		if (parent == null) {
			return;
		}
		Common.clear(parent);

		Vbox box = new Vbox();
		box.setWidth("100%");
		box.setStyle("padding:12px; box-sizing:border-box; background:#f6f8fb;");
		box.setParent(parent);

		// ---- Filter periode (tanggal mulai/sampai) ----
		final org.zkoss.zul.Div filterCard = new org.zkoss.zul.Div();
		filterCard.setStyle("margin-bottom:10px; padding:10px 12px; background:#ffffff; border:1px solid #e8eef6;"
				+ "border-radius:14px; box-shadow:0 8px 20px rgba(15,23,42,.04); box-sizing:border-box;");
		filterCard.setParent(box);

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("border:0; background:transparent; padding:0; display:flex; flex-wrap:wrap;"
				+ " align-items:center; gap:8px;");
		toolbar.setParent(filterCard);

		new MyLabelAgakKecil("Periode pajak — Mulai:").setParent(toolbar);
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
		btnTampil.setTooltiptext("Tampilkan dasbor pajak sesuai periode yang dipilih (kosongkan untuk semua data)");
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

		// Tampilan awal: semua data (filter kosong).
		computeAsync(slot, null, null);
	}

	/** Hitung di latar lewat Timer agar UI tidak terblokir, lalu isi slot. */
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
							+ "Dasbor pajak belum dapat dimuat.</div>");
				}
				try {
					timer.stop();
					timer.detach();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:156");
				}
			}
		});
		timer.start();
	}

	private static String loadingHtml() {
		return "<div style='padding:22px;border-radius:16px;background:#ffffff;border:1px solid #e5e7eb;"
				+ "box-shadow:0 12px 26px rgba(15,23,42,.07);color:#0f172a;'>"
				+ "<div style='font-size:11px;letter-spacing:.12em;text-transform:uppercase;color:#2563eb;font-weight:900;'>Dasbor Pajak</div>"
				+ "<div style='font-size:17px;font-weight:900;margin-top:6px;'>Menghitung ringkasan pajak…</div>"
				+ "<div style='font-size:12px;color:#64748b;margin-top:6px;'>Mohon tunggu sebentar, data sedang dirangkum.</div>"
				+ "<div style='height:10px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:14px;'>"
				+ "<div style='height:10px;width:55%;border-radius:999px;background:linear-gradient(90deg,var(--ais-theme-primary,#2563eb),var(--ais-theme-accent,#06b6d4));'></div>"
				+ "</div></div>";
	}

	// ============================================================
	//  CACHE (per kombinasi filter)
	// ============================================================

	private static String htmlBercache(Date mulai, Date sampai) {
		String fp = (mulai == null ? "0" : String.valueOf(mulai.getTime())) + "_"
				+ (sampai == null ? "0" : String.valueOf(sampai.getTime()));
		String key = DashboardCacheUtil.keyWithFilter("DasboardPajak", "ADMIN", null, fp);
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

	private static final class Data {
		int count;
		double totalNilai;
		double totalDpp;
		double setorNilai;
		int adaNtpn;
		int adaNpwp;
		int adaNamaWp;
		int adaSetor;
		final Map<String, Double> perJenis = new HashMap<String, Double>();
		final Map<String, Double> perSatker = new HashMap<String, Double>();
		final TreeMap<String, Double> bulanNilai = new TreeMap<String, Double>();
		final TreeMap<String, Double> bulanDpp = new TreeMap<String, Double>();
		final Map<String, Double> perWp = new HashMap<String, Double>();
		final Map<String, Double> belumSetorPerNama = new HashMap<String, Double>();
		String belumSetorTerbesarNama = "-";
		double belumSetorTerbesarNilai = 0;
		boolean capped;
	}

	@SuppressWarnings("unchecked")
	private static Data load(Date mulai, Date sampai) {
		Data d = new Data();
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Criteria c = session.createCriteria(Pajak.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.desc("id")).setMaxResults(SAMPLE_LIMIT);
			List<Pajak> rows = c.list();
			d.capped = rows != null && rows.size() >= SAMPLE_LIMIT;
			if (rows != null) {
				for (int idx = 0; idx < rows.size(); idx++) {
					Pajak p = rows.get(idx);
					if (p == null) {
						continue;
					}

					Date tgl = null;
					try {
						tgl = p.getTanggal();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:242");
					}
					// Filter periode di sisi Java (tanggal pajak = nilai turunan).
					if (mulai != null && tgl != null && tgl.before(mulai)) {
						continue;
					}
					if (sampai != null && tgl != null && tgl.after(sampai)) {
						continue;
					}

					double nilai = 0;
					double dpp = 0;
					try {
						Double n = p.getNilai();
						nilai = n == null ? 0 : n.doubleValue();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:257");
					}
					try {
						Double dd = p.getDpp();
						dpp = dd == null ? 0 : dd.doubleValue();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:262");
					}
					d.count++;
					d.totalNilai += nilai;
					d.totalDpp += dpp;

					String jenis = "Lainnya";
					try {
						if (p.getJenisPajakBarang() != null && p.getJenisPajakBarang().getNama() != null) {
							jenis = p.getJenisPajakBarang().getNama();
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:273");
					}
					add(d.perJenis, jenis, nilai);

					String satker = "Tanpa Satuan Kerja";
					try {
						if (p.getSatuanKerja() != null && p.getSatuanKerja().getNama() != null) {
							satker = p.getSatuanKerja().getNama();
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:282");
					}
					add(d.perSatker, satker, nilai);

					String bln = "?";
					try {
						if (tgl != null) {
							String f = Common.databaseDateFormat.get().format(tgl);
							if (f != null && f.length() >= 7) {
								bln = f.substring(0, 7);
							}
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:294");
					}
					add(d.bulanNilai, bln, nilai);
					add(d.bulanDpp, bln, dpp);

					boolean ntpn = false;
					boolean npwp = false;
					boolean namaWp = false;
					boolean setor = false;
					String wpName = null;
					try {
						ntpn = p.getNtpn() != null && p.getNtpn().trim().length() > 0;
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:306");
					}
					try {
						npwp = p.getNpwp() != null && p.getNpwp().trim().length() > 0;
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:310");
					}
					try {
						wpName = p.getNamaWp();
						namaWp = wpName != null && wpName.trim().length() > 0;
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:315");
					}
					try {
						setor = p.getTanggalStor() != null || ntpn;
					} catch (Exception ex) {
						setor = ntpn;
					}
					if (ntpn) {
						d.adaNtpn++;
					}
					if (npwp) {
						d.adaNpwp++;
					}
					if (namaWp) {
						d.adaNamaWp++;
						add(d.perWp, wpName.trim(), nilai);
					}
					if (setor) {
						d.adaSetor++;
						d.setorNilai += nilai;
					} else {
						add(d.belumSetorPerNama, safeNama(p), nilai);
						if (nilai > d.belumSetorTerbesarNilai) {
							d.belumSetorTerbesarNilai = nilai;
							d.belumSetorTerbesarNama = safeNama(p);
						}
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

	private static String safeNama(Pajak p) {
		try {
			String n = p.getNama();
			if (n != null && n.trim().length() > 0) {
				return n.trim();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:358");
		}
		try {
			String k = p.getKode();
			if (k != null && k.trim().length() > 0) {
				return k.trim();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:365");
		}
		return "-";
	}

	// ============================================================
	//  BUILD HTML (semua via DashboardUiKit)
	// ============================================================

	private static String buildHtml(Data d, Date mulai, Date sampai) {
		StringBuilder sb = new StringBuilder();

		sb.append(DashboardUiKit.introBanner("Dasbor Pajak",
				"Pantau pajak yang dipungut dalam satu layar: berapa totalnya, sudah disetor ke negara berapa, "
						+ "jenis pajak apa yang paling besar, dan mana yang belum disetor — lengkap dengan periodenya."));

		sb.append(periodeChip(mulai, sampai, d.count));

		if (d.capped) {
			sb.append("<div style='font-size:11px;color:#92400e;background:#fffbeb;border:1px solid #fde68a;"
					+ "border-radius:10px;padding:8px 10px;margin-bottom:10px;'>"
					+ "Data sangat banyak — dasbor menampilkan " + fmtInt(SAMPLE_LIMIT)
					+ " catatan pajak terbaru.</div>");
		}

		double belum = d.totalNilai - d.setorNilai;
		List<Stat> stats = new ArrayList<Stat>();
		stats.add(new Stat("Jumlah Pajak", fmtInt(d.count), "Banyaknya catatan pajak", "#2563eb"));
		stats.add(new Stat("Total Nilai Pajak", rp(d.totalNilai), "Total pajak yang dihitung", "#0ea5e9"));
		stats.add(new Stat("Total DPP", rp(d.totalDpp), "Dasar pengenaan pajak", "#7c3aed"));
		stats.add(new Stat("Sudah Disetor", rp(d.setorNilai) + " (" + pctStr(d.setorNilai, d.totalNilai) + ")",
				"Sudah dibayar ke negara", "#16a34a"));
		stats.add(new Stat("Belum Disetor", rp(belum) + " (" + pctStr(belum, d.totalNilai) + ")",
				"Masih harus disetor", "#dc2626"));
		stats.add(new Stat("Belum Setor Terbesar", rp(d.belumSetorTerbesarNilai), d.belumSetorTerbesarNama, "#ea580c"));
		sb.append(DashboardUiKit.cards(stats));

		// ---- Tren bulanan (nilai vs DPP) ----
		List<String> blnKeys = new ArrayList<String>(d.bulanNilai.keySet());
		List<String> blnLabel = new ArrayList<String>();
		List<Double> seriNilai = new ArrayList<Double>();
		List<Double> seriDpp = new ArrayList<Double>();
		for (int i = 0; i < blnKeys.size(); i++) {
			String k = blnKeys.get(i);
			blnLabel.add(prettyMonth(k));
			seriNilai.add(d.bulanNilai.get(k));
			seriDpp.add(d.bulanDpp.get(k) == null ? Double.valueOf(0) : d.bulanDpp.get(k));
		}

		sb.append(DashboardUiKit.openGrid(320));

		sb.append(DashboardUiKit.dualLineChart("Tren Pajak per Bulan",
				"Naik-turun nilai pajak dan dasar pengenaannya dari bulan ke bulan.",
				blnLabel, seriNilai, "Nilai Pajak", "#2563eb", seriDpp, "DPP", "#94a3b8"));

		sb.append(DashboardUiKit.donut("Komposisi per Jenis Pajak",
				"Sebagian besar pajak berasal dari jenis apa (mis. PPh, PPN).",
				topNMoney(d.perJenis, 8), true, "Belum ada data pajak."));

		sb.append(DashboardUiKit.barList("Pajak Belum Disetor (Perlu Ditindaklanjuti)",
				"Catatan pajak dengan nilai terbesar yang belum disetor ke negara — dahulukan ini.",
				topNMoney(d.belumSetorPerNama, 8), "#dc2626", null, true,
				"Bagus — tidak ada pajak yang belum disetor."));

		sb.append(DashboardUiKit.barList("Pajak per Satuan Kerja",
				"Unit kerja mana yang paling banyak nilai pajaknya.",
				topNMoney(d.perSatker, 8), "#0ea5e9", null, true, "Belum ada data satuan kerja."));

		sb.append(DashboardUiKit.progressLines("Kelengkapan Administrasi Pajak",
				"Seberapa lengkap bukti setor, NTPN, NPWP, dan nama wajib pajak dari seluruh catatan.",
				complianceMap(d)));

		sb.append(DashboardUiKit.spider("Skor Ketertiban Pajak",
				"Makin lebar jaringnya, makin tertib pencatatan dan penyetoran pajaknya.",
				new String[] { "Sudah Disetor", "Ber-NTPN", "Ber-NPWP", "Ber-Nama WP", "Ada Nilai" },
				new int[] { pctInt(d.adaSetor, d.count), pctInt(d.adaNtpn, d.count), pctInt(d.adaNpwp, d.count),
						pctInt(d.adaNamaWp, d.count), pctInt(countPositif(d), d.count) }));

		sb.append(DashboardUiKit.insight("Sorotan Penting",
				"Hal-hal yang sebaiknya diperhatikan dari data pajak.", insightPairs(d)));

		sb.append(DashboardUiKit.closeGrid());
		return sb.toString();
	}

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
				+ "border-radius:999px;padding:5px 11px;'>" + fmtInt(count) + " catatan</span></div>";
	}

	private static int countPositif(Data d) {
		// jumlah catatan dianggap punya nilai bila total nilai > 0; menjaga spider tetap 5 sumbu bermakna.
		return d.totalNilai > 0 ? d.count : 0;
	}

	private static LinkedHashMap<String, Integer> complianceMap(Data d) {
		LinkedHashMap<String, Integer> m = new LinkedHashMap<String, Integer>();
		m.put("Sudah Disetor", Integer.valueOf(pctInt(d.adaSetor, d.count)));
		m.put("Ber-NTPN", Integer.valueOf(pctInt(d.adaNtpn, d.count)));
		m.put("Ber-NPWP", Integer.valueOf(pctInt(d.adaNpwp, d.count)));
		m.put("Ber-Nama WP", Integer.valueOf(pctInt(d.adaNamaWp, d.count)));
		return m;
	}

	private static LinkedHashMap<String, String> insightPairs(Data d) {
		LinkedHashMap<String, String> m = new LinkedHashMap<String, String>();
		Map.Entry<String, Double> topWp = topEntry(d.perWp);
		Map.Entry<String, Double> topJenis = topEntry(d.perJenis);
		m.put("Wajib Pajak Teratas", topWp == null ? "-" : topWp.getKey() + "  (" + rp(topWp.getValue()) + ")");
		m.put("Belum Disetor Terbesar", d.belumSetorTerbesarNama + "  (" + rp(d.belumSetorTerbesarNilai) + ")");
		m.put("Jenis Pajak Dominan", topJenis == null ? "-" : topJenis.getKey() + "  (" + rp(topJenis.getValue()) + ")");
		m.put("Jumlah Wajib Pajak", fmtInt(d.perWp.size()));
		return m;
	}

	// ============================================================
	//  HELPER agregasi & format
	// ============================================================

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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:596");
		}
		return yyyymm == null ? "?" : yyyymm;
	}

	private static void closeSession(Session session) {
		if (session != null) {
			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:605");
			}
			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:609");
			}
		}
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPajak.java:614");
		}
	}
}
