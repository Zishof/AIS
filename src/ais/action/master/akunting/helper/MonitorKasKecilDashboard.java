package ais.action.master.akunting.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.DashboardReportKit;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.KasKecil;
import ais.ui.util.DashboardCardHelper;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>MonitorKasKecilDashboard — Dashboard Pemantauan &amp; Sirkulasi Kas Kecil (Petty Cash)</h3>
 *
 * <p><b>Untuk apa dashboard ini.</b> Menyediakan satu halaman pemantauan menyeluruh atas seluruh kas
 * kecil (petty cash) organisasi, sehingga pimpinan/bendahara dapat menilai dengan cepat: berapa total
 * dana kas kecil, berapa yang sudah dipakai, berapa sisanya, unit kerja mana yang paling banyak
 * memakai, kas kecil mana yang sudah lama belum diisi ulang (perlu penggantian), serta bagaimana
 * kecenderungan (tren) pemakaian dari bulan ke bulan. Halaman ini dipasang sebagai tab "Monitor" pada
 * menu Pengeluaran Kas Kecil dan bersifat hanya-baca (tidak mengubah data), murni untuk analisis.</p>
 *
 * <p><b>Landasan best practice petty cash.</b> Pengelolaan kas kecil yang sehat umumnya memakai
 * <i>imprest system</i>: setiap unit diberi plafon dana tetap, dana yang terpakai lalu diganti
 * (replenishment) agar saldo kembali penuh. Dari prinsip ini diturunkan indikator-indikator yang
 * ditampilkan dashboard: (1) <i>tingkat penggunaan</i> = dana terpakai dibanding plafon, untuk melihat
 * seberapa cepat dana terserap; (2) <i>umur belum diganti (aging)</i> = berapa lama kas kecil belum
 * diisi ulang, karena keterlambatan penggantian membekukan operasional; (3) <i>sirkulasi per unit</i>
 * = perbandingan terpakai vs sisa antar satuan kerja, untuk memeratakan beban dan mendeteksi unit yang
 * boros; (4) <i>status penggantian</i> = komposisi kas kecil yang belum diganti, sedang diajukan, dan
 * sudah diganti; (5) <i>tren waktu</i> = pola musiman pengeluaran; (6) <i>skor kesehatan</i> multi-sudut
 * per unit dalam bentuk grafik jaring laba-laba (radar) yang merangkum beberapa indikator sekaligus.</p>
 *
 * <p><b>Panel yang ditampilkan.</b> Secara berurutan: (a) deretan kartu angka ringkas (KPI); (b) grafik
 * lingkaran status penggantian dan grafik garis tren pemakaian per bulan; (c) grafik batang bertumpuk
 * sirkulasi per satuan kerja dan grafik radar skor kesehatan tiap unit; (d) grafik batang umur
 * kas-kecil-belum-diganti dan grafik batang pemakaian per jenis kas kecil; (e) tabel rincian per kas
 * kecil. Setiap panel diberi satu-dua kalimat penjelasan berbahasa sangat sederhana sehingga pengguna
 * yang awam teknologi tetap memahami maksud tiap tampilan.</p>
 *
 * <p><b>Sumber &amp; alur data.</b> Data diambil sekali dari entitas {@link KasKecil} (beserta relasi
 * satuan kerja, jenis kas kecil, dan penggantian) melalui satu sesi Hibernate tersendiri yang dibuka
 * dan ditutup di dalam {@link #loadData()}; hasilnya diringkas ke objek ringan {@code Baris} sehingga
 * seluruh perhitungan agregasi (per unit, per bulan, per status, aging) dilakukan di memori tanpa
 * membebani basis data berulang kali. Status "Belum Diganti / Dalam Pengajuan / Sudah Diganti"
 * ditentukan dari ada/tidaknya dokumen penggantian dan status persetujuannya. Filter yang tersedia:
 * kata kunci (judul/kode), status penggantian, dan rentang tanggal.</p>
 *
 * <p><b>Teknologi grafik.</b> Seluruh grafik (lingkaran, batang, batang bertumpuk, garis tren, dan
 * radar/jaring laba-laba) dibuat memakai {@link HtmlChartHelper} yang murni HTML/CSS/SVG modern — tanpa
 * JFreeChart — sehingga ringan, tajam di layar beresolusi tinggi, dan responsif. Tata letak memakai
 * {@link DashboardCardHelper} berbasis CSS flexbox: kartu dan grafik otomatis tersusun berdampingan di
 * layar lebar (desktop) dan menumpuk ke bawah di layar sempit (mobile), tanpa media-query rumit.</p>
 *
 * <p><b>Prinsip reuse &amp; pemeliharaan.</b> Kelas ini sengaja tidak menulis ulang gaya kartu/panel;
 * semuanya didelegasikan ke {@link DashboardCardHelper} dan {@link HtmlChartHelper} yang dipakai
 * bersama dashboard lain. Dengan begitu, penyesuaian tampilan cukup dilakukan di dua perkakas tersebut
 * dan seluruh dashboard ikut konsisten. Untuk menambah panel/indikator baru, tambahkan satu metode
 * {@code renderXxx(...)} dan panggil dari {@link #reload()}. Untuk mengganti sumber data atau filter,
 * cukup ubah {@link #loadData()}. Kompatibel Java 1.7 dan ZK 5.5; seluruh akses lazy Hibernate
 * dibungkus {@code try/catch} agar satu data cacat tidak menggagalkan seluruh halaman.</p>
 *
 * <p><b>Ide pengembangan lanjutan (best practice) yang relevan ditambahkan kemudian:</b> proyeksi
 * "hari menuju habis" (burn-rate), selisih rekonsiliasi (sisa tercatat vs sisa seharusnya), lama waktu
 * persetujuan (turnaround), deteksi anomali (sisa minus / melebihi plafon), dan rincian kategori biaya
 * dari formula pengeluaran. Indikator-indikator ini dapat memakai grafik yang sama (garis, batang,
 * radar) sehingga tetap seragam.</p>
 */
public class MonitorKasKecilDashboard extends Vbox {

	private static final long serialVersionUID = -6631823904517741411L;

	private static final int GRID_PAGE_SIZE = 50;
	private static final int LIMIT = 5000;

	private static final String ST_BELUM = "Belum Diganti";
	private static final String ST_PROSES = "Dalam Pengajuan";
	private static final String ST_SUDAH = "Sudah Diganti";

	private Textbox keyword;
	private Combobox statusFilter;
	private MyDatebox tglMulai;
	private MyDatebox tglSampai;
	private Vbox body;

	/** Satu baris ringkas kas kecil hasil ekstraksi dari {@link KasKecil} (agar aman dari lazy-load). */
	private static class Baris {
		String kode;
		String nama;
		String satker;
		String jenis;
		double saldo;
		double terpakai;
		double sisa;
		Date tanggal;
		String statusGanti;
		boolean disetujui;
		long umurHari;
		boolean dibayarDpc;
		String statusTransfer;
		String bank;
		String kodeTransfer;
	}

	public MonitorKasKecilDashboard() {
		setWidth("100%");
		setHeight("100%");
		setStyle("overflow:auto;background:#f1f5f9;padding:12px;box-sizing:border-box;");
		buildLayout();
		reload();
	}

	// --------------------------------------------------------------------- toolbar & kerangka

	private void buildLayout() {
		Toolbar toolbar = new Toolbar();
		toolbar.setWidth("100%");
		toolbar.setStyle("padding:10px;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;"
				+ "margin-bottom:12px;");
		toolbar.setParent(this);

		new Label("Cari judul / kode: ").setParent(toolbar);
		keyword = new Textbox();
		keyword.setWidth("210px");
		keyword.setParent(toolbar);
		keyword.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload();
			}
		});

		new Label(ais.common.Common.getBahasaConfig(" Status: ")).setParent(toolbar);
		statusFilter = new Combobox();
		statusFilter.setReadonly(true);
		statusFilter.setWidth("170px");
		tambahCombo(statusFilter, "Semua Status", "");
		tambahCombo(statusFilter, ST_BELUM, ST_BELUM);
		tambahCombo(statusFilter, ST_PROSES, ST_PROSES);
		tambahCombo(statusFilter, ST_SUDAH, ST_SUDAH);
		statusFilter.setSelectedIndex(0);
		statusFilter.setParent(toolbar);

		new Label(ais.common.Common.getBahasaConfig(" Tgl: ")).setParent(toolbar);
		tglMulai = new MyDatebox();
		tglMulai.setWidth("120px");
		tglMulai.setParent(toolbar);
		new Label(" s/d ").setParent(toolbar);
		tglSampai = new MyDatebox();
		tglSampai.setWidth("120px");
		tglSampai.setParent(toolbar);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Tampilkan", "/img/search.gif");
		cari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload();
			}
		});
		cari.setParent(toolbar);

		// Tombol Cetak Laporan Lengkap (PDF-ready) + Ekspor Excel + progress bar — mesin reuse.
		DashboardReportKit.pasangTombol(toolbar, this, buatSumberLaporan());

		body = new Vbox();
		body.setWidth("100%");
		body.setParent(this);
	}

	private void tambahCombo(Combobox combo, String label, String value) {
		Comboitem item = new Comboitem(label);
		item.setValue(value);
		item.setParent(combo);
	}

	/**
	 * Mendeskripsikan isi laporan (Cetak PDF + Ekspor Excel) untuk mesin {@link DashboardReportKit}.
	 * Data diambil sekali sesuai filter terbaru saat tombol diklik, lalu tiap panel (KPI, donut
	 * status, tren garis, batang per unit, radar kesehatan, aging, per jenis, dan tabel rincian)
	 * menyediakan barisnya. Grafik pada laporan dibuat mesin secara HTML/CSS modern (bukan JFreeChart)
	 * lengkap dengan progress bar bertahap.
	 */
	private DashboardReportKit.SumberLaporan buatSumberLaporan() {
		return new DashboardReportKit.SumberLaporan() {
			@Override
			public String judul() {
				return "Monitor Kas Kecil";
			}

			@Override
			public String subjudul() {
				return "Pemantauan & Sirkulasi Petty Cash";
			}

			@Override
			public String deskripsi() {
				return "Melihat kondisi, pemakaian, sisa, dan penggantian kas kecil per unit kerja.";
			}

			@Override
			public java.util.List<DashboardReportKit.Bagian> bagian() {
				final List<Baris> data = loadData();
				java.util.List<DashboardReportKit.Bagian> b = new java.util.ArrayList<DashboardReportKit.Bagian>();

				b.add(DashboardReportKit.kpi("Ringkasan Kas Kecil",
					"Kondisi kas kecil sekarang: total dana, yang sudah dipakai, sisanya, dan yang perlu diisi ulang.",
					new DashboardReportKit.PenyediaBaris() {
						@Override
						public java.util.List<Object[]> ambil() {
							int tot = data.size(); int belum = 0; double sal = 0, pk = 0, ss = 0;
							for (Baris x : data) { sal += x.saldo; pk += x.terpakai; ss += x.sisa; if (ST_BELUM.equals(x.statusGanti)) belum++; }
							double pp = sal <= 0 ? 0 : pk / sal * 100.0;
							java.util.List<Object[]> o = new java.util.ArrayList<Object[]>();
							o.add(new Object[] { "Jumlah Kas Kecil", DashboardReportKit.fmt(tot), "dokumen kas kecil" });
							o.add(new Object[] { "Total Dana", money(sal), "plafon seluruh kas kecil" });
							o.add(new Object[] { "Sudah Dipakai", money(pk), fmt(pp) + "% dari dana" });
							o.add(new Object[] { "Sisa Dana", money(ss), "yang masih tersedia" });
							o.add(new Object[] { "Perlu Diganti", DashboardReportKit.fmt(belum), "belum diisi ulang" });
							return o;
						}
					}));

				b.add(DashboardReportKit.donut("Status Penggantian",
					"Perbandingan kas kecil yang belum, sedang, dan sudah diisi ulang.",
					new String[] { "Status", "Jumlah" }, new DashboardReportKit.PenyediaBaris() {
						@Override
						public java.util.List<Object[]> ambil() {
							int belum = 0, proses = 0, sudah = 0;
							for (Baris x : data) { if (ST_SUDAH.equals(x.statusGanti)) sudah++; else if (ST_PROSES.equals(x.statusGanti)) proses++; else belum++; }
							java.util.List<Object[]> o = new java.util.ArrayList<Object[]>();
							o.add(new Object[] { ST_BELUM, Long.valueOf(belum) });
							o.add(new Object[] { ST_PROSES, Long.valueOf(proses) });
							o.add(new Object[] { ST_SUDAH, Long.valueOf(sudah) });
							return o;
						}
					}));

				b.add(DashboardReportKit.garis("Tren Pemakaian per Bulan",
					"Naik-turun pemakaian kas kecil dari bulan ke bulan.",
					new String[] { "Bulan", "Dipakai" }, new DashboardReportKit.PenyediaBaris() {
						@Override
						public java.util.List<Object[]> ambil() {
							java.util.Map<String, Double> m = new java.util.LinkedHashMap<String, Double>();
							java.util.Calendar cal = java.util.Calendar.getInstance();
							for (Baris x : data) { if (x.tanggal == null) continue; cal.setTime(x.tanggal);
								String k = cal.get(java.util.Calendar.YEAR) + "-" + pad2(cal.get(java.util.Calendar.MONTH) + 1);
								Double v = m.get(k); m.put(k, (v == null ? 0 : v) + x.terpakai); }
							java.util.List<String> ks = new java.util.ArrayList<String>(m.keySet()); java.util.Collections.sort(ks);
							int mulai = Math.max(0, ks.size() - 12); ks = ks.subList(mulai, ks.size());
							java.util.List<Object[]> o = new java.util.ArrayList<Object[]>();
							for (String k : ks) o.add(new Object[] { labelBulan(k), Long.valueOf(Math.round(m.get(k))) });
							return o;
						}
					}));

				b.add(DashboardReportKit.batang("Pemakaian per Satuan Kerja",
					"Unit kerja mana yang paling banyak memakai kas kecil.",
					new String[] { "Satuan Kerja", "Dipakai" }, new DashboardReportKit.PenyediaBaris() {
						@Override
						public java.util.List<Object[]> ambil() {
							return barisAgregasi(data, true);
						}
					}));

				b.add(DashboardReportKit.radar("Skor Kesehatan Kas Kecil",
					"Nilai kesehatan pengelolaan kas kecil dilihat dari beberapa sisi (0-100).",
					new String[] { "Metrik", "Nilai" }, new DashboardReportKit.PenyediaBaris() {
						@Override
						public java.util.List<Object[]> ambil() {
							double sal = 0, pk = 0, ss = 0; int sudah = 0, setuju = 0, tot = data.size();
							for (Baris x : data) { sal += x.saldo; pk += x.terpakai; ss += x.sisa; if (ST_SUDAH.equals(x.statusGanti)) sudah++; if (x.disetujui) setuju++; }
							double d = sal <= 0 ? 1 : sal;
							java.util.List<Object[]> o = new java.util.ArrayList<Object[]>();
							o.add(new Object[] { "Pemakaian", Long.valueOf(Math.round(Math.min(100, pk / d * 100))) });
							o.add(new Object[] { "Sisa Sehat", Long.valueOf(Math.round(Math.min(100, ss / d * 100))) });
							o.add(new Object[] { "Ketepatan Ganti", Long.valueOf(tot <= 0 ? 0 : Math.round(sudah * 100.0 / tot)) });
							o.add(new Object[] { "Kepatuhan Setuju", Long.valueOf(tot <= 0 ? 0 : Math.round(setuju * 100.0 / tot)) });
							return o;
						}
					}));

				b.add(DashboardReportKit.batang("Umur Kas Kecil Belum Diganti",
					"Kas kecil yang belum diisi ulang, dikelompokkan menurut sudah berapa lama.",
					new String[] { "Kelompok Umur", "Jumlah" }, new DashboardReportKit.PenyediaBaris() {
						@Override
						public java.util.List<Object[]> ambil() {
							long[] bkt = new long[4];
							for (Baris x : data) { if (!ST_BELUM.equals(x.statusGanti)) continue; long h = x.umurHari;
								if (h <= 7) bkt[0]++; else if (h <= 14) bkt[1]++; else if (h <= 30) bkt[2]++; else bkt[3]++; }
							java.util.List<Object[]> o = new java.util.ArrayList<Object[]>();
							o.add(new Object[] { "0-7 hari", Long.valueOf(bkt[0]) });
							o.add(new Object[] { "8-14 hari", Long.valueOf(bkt[1]) });
							o.add(new Object[] { "15-30 hari", Long.valueOf(bkt[2]) });
							o.add(new Object[] { "lebih dari 30 hari", Long.valueOf(bkt[3]) });
							return o;
						}
					}));

				b.add(DashboardReportKit.batang("Pemakaian per Jenis Kas Kecil",
					"Jenis kas kecil yang paling banyak menyerap dana.",
					new String[] { "Jenis", "Dipakai" }, new DashboardReportKit.PenyediaBaris() {
						@Override
						public java.util.List<Object[]> ambil() {
							return barisAgregasi(data, false);
						}
					}));

				b.add(DashboardReportKit.tabel("Rincian Kas Kecil",
					"Daftar lengkap tiap kas kecil beserta dana, pemakaian, sisa, status, dan umurnya.",
					new String[] { "Kode", "Nama", "Satuan Kerja", "Jenis", "Dana", "Dipakai", "Sisa", "% Pakai", "Status", "Umur (hari)" },
					new DashboardReportKit.PenyediaBaris() {
						@Override
						public java.util.List<Object[]> ambil() {
							java.util.List<Object[]> o = new java.util.ArrayList<Object[]>();
							for (Baris x : data) { double p = x.saldo <= 0 ? 0 : x.terpakai / x.saldo * 100.0;
								o.add(new Object[] { dash(x.kode), dash(x.nama), dash(x.satker), dash(x.jenis), money(x.saldo),
									money(x.terpakai), money(x.sisa), fmt(p) + "%", x.statusGanti,
									ST_BELUM.equals(x.statusGanti) ? String.valueOf(x.umurHari) : "-" }); }
							return o;
						}
					}));

				return b;
			}
		};
	}

	/** Baris agregasi pemakaian: per satuan kerja (perSatker=true) atau per jenis; urut turun, maks 12/10. */
	private java.util.List<Object[]> barisAgregasi(List<Baris> data, boolean perSatker) {
		java.util.Map<String, Double> m = new java.util.LinkedHashMap<String, Double>();
		for (Baris x : data) {
			String key = perSatker ? kosongKe(x.satker, "(Tanpa Satuan Kerja)") : kosongKe(x.jenis, "(Tanpa Jenis)");
			Double v = m.get(key); m.put(key, (v == null ? 0 : v) + x.terpakai);
		}
		java.util.List<java.util.Map.Entry<String, Double>> urut = new java.util.ArrayList<java.util.Map.Entry<String, Double>>(m.entrySet());
		java.util.Collections.sort(urut, new java.util.Comparator<java.util.Map.Entry<String, Double>>() {
			@Override
			public int compare(java.util.Map.Entry<String, Double> a, java.util.Map.Entry<String, Double> b2) {
				return Double.compare(b2.getValue(), a.getValue());
			}
		});
		int n = Math.min(perSatker ? 12 : 10, urut.size());
		java.util.List<Object[]> o = new java.util.ArrayList<Object[]>();
		for (int i = 0; i < n; i++) o.add(new Object[] { urut.get(i).getKey(), Long.valueOf(Math.round(urut.get(i).getValue())) });
		return o;
	}

	private void reload() {
		Common.clear(body);
		List<Baris> data = loadData();
		renderRingkasan(data);
		renderStatusDanTren(data);
		renderDpc(data);
		renderSirkulasiDanRadar(data);
		renderAgingDanJenis(data);
		renderGrid(data);
	}

	// --------------------------------------------------------------------- panel 1: KPI

	private void renderRingkasan(List<Baris> data) {
		int total = data.size();
		int aktifBelum = 0, dibayarDpc = 0;
		double saldo = 0, terpakai = 0, sisa = 0;
		for (Baris b : data) {
			saldo += b.saldo;
			terpakai += b.terpakai;
			sisa += b.sisa;
			if (ST_BELUM.equals(b.statusGanti)) {
				aktifBelum++;
			}
			if (b.dibayarDpc) {
				dibayarDpc++;
			}
		}
		double persenPakai = saldo <= 0 ? 0 : (terpakai / saldo * 100.0);

		Panelchildren isi = DashboardCardHelper.panel(body, "Ringkasan Kas Kecil",
				"Kondisi kas kecil sekarang dalam angka besar: total dana, yang sudah dipakai, sisanya, "
						+ "dan berapa yang perlu segera diisi ulang.");
		String baris = DashboardCardHelper.barisKartu(
				DashboardCardHelper.kartu("Jumlah Kas Kecil", fmtInt(total), "dokumen kas kecil", "#1d4ed8"),
				DashboardCardHelper.kartu("Total Dana", money(saldo), "plafon seluruh kas kecil", "#4338ca"),
				DashboardCardHelper.kartu("Sudah Dipakai", money(terpakai), fmt(persenPakai) + "% dari dana", "#b45309"),
				DashboardCardHelper.kartu("Sisa Dana", money(sisa), "yang masih tersedia", "#15803d"),
				DashboardCardHelper.kartu("Perlu Diganti", fmtInt(aktifBelum), "belum diisi ulang", "#dc2626"),
				DashboardCardHelper.kartu("Sudah Dibayar DPC", fmtInt(dibayarDpc), "dana sudah cair (DPC)", "#0f766e"));
		new MyHtml(baris).setParent(isi);
	}

	// --------------------------------------------------------------------- panel: pembayaran DPC

	/** Donut status pembayaran/transfer (DPC) + batang dana dibayar per bank. */
	private void renderDpc(List<Baris> data) {
		String[] urut = { DpcTransferStatusHelper.BELUM_AJU, DpcTransferStatusHelper.DIAJUKAN,
			DpcTransferStatusHelper.PROSES, DpcTransferStatusHelper.DISETUJUI, DpcTransferStatusHelper.DIBAYAR };
		java.util.Map<String, Integer> perStatus = new java.util.LinkedHashMap<String, Integer>();
		for (String u : urut) {
			perStatus.put(u, 0);
		}
		java.util.Map<String, Double> perBank = new java.util.LinkedHashMap<String, Double>();
		for (Baris b : data) {
			String st = b.statusTransfer == null ? DpcTransferStatusHelper.BELUM_AJU : b.statusTransfer;
			Integer v = perStatus.get(st);
			perStatus.put(st, (v == null ? 0 : v) + 1);
			if (b.dibayarDpc) {
				String bk = b.bank == null || b.bank.trim().isEmpty() ? "(Tanpa Bank)" : b.bank.trim();
				Double bv = perBank.get(bk);
				perBank.put(bk, (bv == null ? 0 : bv) + b.terpakai);
			}
		}
		String[] labels = perStatus.keySet().toArray(new String[perStatus.size()]);
		double[] vals = new double[labels.length];
		for (int i = 0; i < labels.length; i++) {
			vals[i] = perStatus.get(labels[i]);
		}
		String donut = HtmlChartHelper.donut("Status Pembayaran (DPC)",
			"Sudah sampai mana pengajuan transfer dana: dari belum diajukan sampai sudah dibayar (cair).",
			labels, vals, new String[] { "#94a3b8", "#f59e0b", "#3b82f6", "#8b5cf6", "#16a34a" }, "belum diajukan");
		String[] bankLabels = perBank.keySet().toArray(new String[perBank.size()]);
		double[] bankVals = new double[bankLabels.length];
		for (int i = 0; i < bankLabels.length; i++) {
			bankVals[i] = perBank.get(bankLabels[i]);
		}
		String bar = HtmlChartHelper.barHorizontal("Dana Dibayar per Bank",
			"Total dana yang sudah cair (DPC) dikelompokkan menurut bank pembayarnya.",
			bankLabels, bankVals, "#0f766e");
		Panelchildren isi = DashboardCardHelper.panel(body, "Pembayaran & Transfer (DPC)",
			"Memantau apakah dana sudah dibayar lewat DPC, lewat bank apa, dan sudah sampai mana prosesnya.");
		new MyHtml(DashboardCardHelper.barisChart(donut, bar)).setParent(isi);
	}

	// --------------------------------------------------------------------- panel 2: status + tren

	private void renderStatusDanTren(List<Baris> data) {
		// Donut status penggantian.
		int belum = 0, proses = 0, sudah = 0;
		for (Baris b : data) {
			if (ST_SUDAH.equals(b.statusGanti)) {
				sudah++;
			} else if (ST_PROSES.equals(b.statusGanti)) {
				proses++;
			} else {
				belum++;
			}
		}
		String donut = HtmlChartHelper.donut("Status Penggantian",
				"Perbandingan kas kecil yang belum diisi ulang, yang sedang diproses, dan yang sudah diisi ulang.",
				new String[] { ST_BELUM, ST_PROSES, ST_SUDAH }, new double[] { belum, proses, sudah },
				new String[] { "#dc2626", "#f59e0b", "#16a34a" }, "belum diganti");

		// Tren pemakaian per bulan (terpakai vs sisa).
		Map<String, double[]> perBulan = new LinkedHashMap<String, double[]>();
		Calendar cal = Calendar.getInstance();
		for (Baris b : data) {
			if (b.tanggal == null) {
				continue;
			}
			cal.setTime(b.tanggal);
			String key = cal.get(Calendar.YEAR) + "-" + pad2(cal.get(Calendar.MONTH) + 1);
			double[] v = perBulan.get(key);
			if (v == null) {
				v = new double[2];
				perBulan.put(key, v);
			}
			v[0] += b.terpakai;
			v[1] += b.sisa;
		}
		List<String> kunci = new ArrayList<String>(perBulan.keySet());
		java.util.Collections.sort(kunci);
		int mulai = Math.max(0, kunci.size() - 12);
		kunci = kunci.subList(mulai, kunci.size());
		String[] kategori = new String[kunci.size()];
		double[][] nilai = new double[2][kunci.size()];
		for (int i = 0; i < kunci.size(); i++) {
			kategori[i] = labelBulan(kunci.get(i));
			double[] v = perBulan.get(kunci.get(i));
			nilai[0][i] = v[0];
			nilai[1][i] = v[1];
		}
		String tren = HtmlChartHelper.lineMulti("Tren Pemakaian per Bulan",
				"Naik-turun pemakaian dan sisa kas kecil tiap bulan, untuk melihat pola dan bulan dengan "
						+ "pengeluaran tinggi.",
				kategori, new String[] { "Dipakai", "Sisa" }, nilai, new String[] { "#e4496b", "#16a34a" });

		Panelchildren isi = DashboardCardHelper.panel(body, "Status & Tren",
				"Melihat sekilas sudah berapa yang diisi ulang, dan bagaimana pemakaian berubah dari bulan ke bulan.");
		new MyHtml(DashboardCardHelper.barisChart(donut, tren)).setParent(isi);
	}

	// --------------------------------------------------------------------- panel 3: sirkulasi + radar

	private void renderSirkulasiDanRadar(List<Baris> data) {
		// Agregasi per satuan kerja.
		Map<String, double[]> perSatker = new LinkedHashMap<String, double[]>();
		// [0]=saldo [1]=terpakai [2]=sisa [3]=jumlah [4]=sudahGanti [5]=disetujui
		for (Baris b : data) {
			String key = kosongKe(b.satker, "(Tanpa Satuan Kerja)");
			double[] v = perSatker.get(key);
			if (v == null) {
				v = new double[6];
				perSatker.put(key, v);
			}
			v[0] += b.saldo;
			v[1] += b.terpakai;
			v[2] += b.sisa;
			v[3] += 1;
			if (ST_SUDAH.equals(b.statusGanti)) {
				v[4] += 1;
			}
			if (b.disetujui) {
				v[5] += 1;
			}
		}
		// Urutkan satker berdasar saldo terbesar.
		List<Map.Entry<String, double[]>> urut = new ArrayList<Map.Entry<String, double[]>>(perSatker.entrySet());
		java.util.Collections.sort(urut, new java.util.Comparator<Map.Entry<String, double[]>>() {
			@Override
			public int compare(Map.Entry<String, double[]> a, Map.Entry<String, double[]> b) {
				return Double.compare(b.getValue()[0], a.getValue()[0]);
			}
		});

		// Stacked bar: terpakai vs sisa per satker (maksimal 12 teratas).
		int nBar = Math.min(12, urut.size());
		String[] catSatker = new String[nBar];
		double[][] nilaiSatker = new double[nBar][2];
		for (int i = 0; i < nBar; i++) {
			catSatker[i] = urut.get(i).getKey();
			nilaiSatker[i][0] = urut.get(i).getValue()[1];
			nilaiSatker[i][1] = urut.get(i).getValue()[2];
		}
		String sirkulasi = HtmlChartHelper.stackedBar("Sirkulasi per Satuan Kerja",
				"Tiap unit kerja: berapa dana yang sudah dipakai dan berapa sisanya. Batang lebih panjang "
						+ "berarti plafon kas kecilnya lebih besar.",
				catSatker, new String[] { "Dipakai", "Sisa" }, nilaiSatker,
				new String[] { "#e4496b", "#16a34a" });

		// Radar skor kesehatan: 3 satker teratas, 5 sumbu (skala 0-100).
		int nRadar = Math.min(3, urut.size());
		double maxCount = 1;
		for (int i = 0; i < nRadar; i++) {
			maxCount = Math.max(maxCount, urut.get(i).getValue()[3]);
		}
		String[] axes = new String[] { "Pemakaian", "Sisa Sehat", "Ketepatan Ganti", "Aktivitas", "Kepatuhan Setuju" };
		String[] seriesSatker = new String[nRadar];
		double[][] nilaiRadar = new double[nRadar][5];
		for (int i = 0; i < nRadar; i++) {
			double[] v = urut.get(i).getValue();
			seriesSatker[i] = urut.get(i).getKey();
			double sal = v[0] <= 0 ? 1 : v[0];
			nilaiRadar[i][0] = Math.min(100, v[1] / sal * 100.0);
			nilaiRadar[i][1] = Math.min(100, v[2] / sal * 100.0);
			nilaiRadar[i][2] = v[3] <= 0 ? 0 : v[4] / v[3] * 100.0;
			nilaiRadar[i][3] = v[3] / maxCount * 100.0;
			nilaiRadar[i][4] = v[3] <= 0 ? 0 : v[5] / v[3] * 100.0;
		}
		String radar = HtmlChartHelper.radar("Skor Kesehatan Kas Kecil per Unit",
				"Nilai kesehatan 3 unit dengan dana terbesar dilihat dari beberapa sisi sekaligus; makin "
						+ "lebar jaringnya makin baik.",
				axes, seriesSatker, nilaiRadar, new String[] { "#1877f2", "#f59e0b", "#16a34a" }, 100);

		Panelchildren isi = DashboardCardHelper.panel(body, "Sirkulasi Antar Unit",
				"Membandingkan pemakaian kas kecil antar unit kerja, dan menilai kesehatan pengelolaannya.");
		new MyHtml(DashboardCardHelper.barisChart(sirkulasi, radar)).setParent(isi);
	}

	// --------------------------------------------------------------------- panel 4: aging + per jenis

	private void renderAgingDanJenis(List<Baris> data) {
		// Aging kas kecil yang BELUM diganti.
		double[] bucket = new double[4]; // 0-7, 8-14, 15-30, >30
		for (Baris b : data) {
			if (!ST_BELUM.equals(b.statusGanti)) {
				continue;
			}
			long h = b.umurHari;
			if (h <= 7) {
				bucket[0]++;
			} else if (h <= 14) {
				bucket[1]++;
			} else if (h <= 30) {
				bucket[2]++;
			} else {
				bucket[3]++;
			}
		}
		String aging = HtmlChartHelper.barHorizontal("Umur Kas Kecil Belum Diganti",
				"Kas kecil yang belum diisi ulang dikelompokkan menurut sudah berapa lama; kelompok paling "
						+ "lama (di bawah) harus diprioritaskan.",
				new String[] { "0-7 hari", "8-14 hari", "15-30 hari", "lebih dari 30 hari" }, bucket, "#dc2626");

		// Pemakaian per jenis kas kecil.
		Map<String, Double> perJenis = new LinkedHashMap<String, Double>();
		for (Baris b : data) {
			String key = kosongKe(b.jenis, "(Tanpa Jenis)");
			Double v = perJenis.get(key);
			perJenis.put(key, (v == null ? 0 : v) + b.terpakai);
		}
		List<Map.Entry<String, Double>> urutJenis = new ArrayList<Map.Entry<String, Double>>(perJenis.entrySet());
		java.util.Collections.sort(urutJenis, new java.util.Comparator<Map.Entry<String, Double>>() {
			@Override
			public int compare(Map.Entry<String, Double> a, Map.Entry<String, Double> b) {
				return Double.compare(b.getValue(), a.getValue());
			}
		});
		int nJenis = Math.min(10, urutJenis.size());
		String[] labelJenis = new String[nJenis];
		double[] nilaiJenis = new double[nJenis];
		for (int i = 0; i < nJenis; i++) {
			labelJenis[i] = urutJenis.get(i).getKey();
			nilaiJenis[i] = urutJenis.get(i).getValue();
		}
		String jenis = HtmlChartHelper.barHorizontal("Pemakaian per Jenis Kas Kecil",
				"Jenis kas kecil mana yang paling banyak menyerap dana.", labelJenis, nilaiJenis, "#4338ca");

		Panelchildren isi = DashboardCardHelper.panel(body, "Penggantian & Jenis",
				"Menemukan kas kecil yang telat diisi ulang, dan jenis kas kecil yang paling banyak dipakai.");
		new MyHtml(DashboardCardHelper.barisChart(aging, jenis)).setParent(isi);
	}

	// --------------------------------------------------------------------- panel 5: grid rincian

	private void renderGrid(List<Baris> data) {
		Panelchildren isi = DashboardCardHelper.panel(body, "Rincian Kas Kecil",
				"Daftar lengkap tiap kas kecil beserta dana, pemakaian, sisa, status, dan umurnya.");

		MyGrid grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(GRID_PAGE_SIZE);
		grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setParent(isi);

		Columns columns = new Columns();
		columns.setParent(grid);
		kolom(columns, "No", "45px", "center");
		kolom(columns, "Kode / Nama", null, null);
		kolom(columns, "Satuan Kerja", null, null);
		kolom(columns, "Jenis", null, null);
		kolom(columns, "Dana", null, "right");
		kolom(columns, "Dipakai", null, "right");
		kolom(columns, "Sisa", null, "right");
		kolom(columns, "% Pakai", "70px", "right");
		kolom(columns, "Status", null, "center");
		kolom(columns, "Umur (hari)", "80px", "right");
		kolom(columns, "Status Transfer", null, "center");
		kolom(columns, "Via Bank", null, null);
		kolom(columns, "Dibayar DPC", "85px", "center");

		Rows rows = new Rows();
		rows.setParent(grid);
		int no = 0;
		for (Baris b : data) {
			no++;
			double persen = b.saldo <= 0 ? 0 : (b.terpakai / b.saldo * 100.0);
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(String.valueOf(no)));
			row.appendChild(new Label(dash(b.kode) + (b.nama == null || b.nama.trim().isEmpty() ? "" : "\n" + b.nama)));
			row.appendChild(new Label(dash(b.satker)));
			row.appendChild(new Label(dash(b.jenis)));
			row.appendChild(kanan(money(b.saldo)));
			row.appendChild(kanan(money(b.terpakai)));
			row.appendChild(kanan(money(b.sisa)));
			row.appendChild(kanan(fmt(persen) + "%"));
			Label st = new Label(b.statusGanti);
			st.setStyle("font-weight:600;color:" + warnaStatus(b.statusGanti) + ";");
			row.appendChild(st);
			row.appendChild(kanan(ST_BELUM.equals(b.statusGanti) ? String.valueOf(b.umurHari) : "-"));
			Label stT = new Label(dash(b.statusTransfer));
			stT.setStyle("color:" + (b.dibayarDpc ? "#15803d" : "#475569") + ";");
			row.appendChild(stT);
			row.appendChild(new Label(dash(b.bank)));
			Label dpcL = new Label(b.dibayarDpc ? "Ya" : "Belum");
			dpcL.setStyle("font-weight:600;color:" + (b.dibayarDpc ? "#15803d" : "#b45309") + ";");
			row.appendChild(dpcL);
		}
		if (data.isEmpty()) {
			Row row = new Row();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "13");
			row.appendChild(new Label("Tidak ada kas kecil pada rentang/filter ini."));
		}
	}

	// --------------------------------------------------------------------- data

	@SuppressWarnings("unchecked")
	private List<Baris> loadData() {
		List<Baris> hasil = new ArrayList<Baris>();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Criteria c = session.createCriteria(KasKecil.class)
					.createAlias("satuanKerja", "satuanKerja", Criteria.LEFT_JOIN)
					.createAlias("jenisKasKecil", "jenisKasKecil", Criteria.LEFT_JOIN)
					.createAlias("penggantianKasKecil", "penggantianKasKecil", Criteria.LEFT_JOIN);

			String q = keyword == null ? "" : safe(keyword.getValue());
			if (!q.isEmpty()) {
				c.add(Restrictions.or(Restrictions.ilike("nama", q, MatchMode.ANYWHERE),
						Restrictions.ilike("kode", q, MatchMode.ANYWHERE)));
			}
			Date d1 = tglMulai == null ? null : tglMulai.getValue();
			Date d2 = tglSampai == null ? null : tglSampai.getValue();
			if (d1 != null) {
				c.add(Restrictions.ge("tanggalPembuatan", awalHari(d1)));
			}
			if (d2 != null) {
				c.add(Restrictions.le("tanggalPembuatan", akhirHari(d2)));
			}
			c.addOrder(Order.desc("id")).setMaxResults(LIMIT);

			String statusPilih = statusFilter == null || statusFilter.getSelectedItem() == null ? ""
					: String.valueOf(statusFilter.getSelectedItem().getValue());
			long now = System.currentTimeMillis();

			List<KasKecil> list = c.list();
			for (KasKecil k : list) {
				if (k == null) {
					continue;
				}
				Baris b = new Baris();
				try { b.kode = k.getKode(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorKasKecilDashboard.java:754"); }
				try { b.nama = k.getNama(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorKasKecilDashboard.java:755"); }
				try { b.satker = k.getSatuanKerja() == null ? null : k.getSatuanKerja().getNama(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorKasKecilDashboard.java:756"); }
				try { b.jenis = k.getJenisKasKecil() == null ? null : k.getJenisKasKecil().getNama(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorKasKecilDashboard.java:757"); }
				try { b.saldo = nz(k.getSaldo()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorKasKecilDashboard.java:758"); }
				try { b.terpakai = nz(k.getNilai()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorKasKecilDashboard.java:759"); }
				try { b.sisa = nz(k.getSisa()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorKasKecilDashboard.java:760"); }
				try { b.tanggal = k.getTanggalPembuatan() != null ? k.getTanggalPembuatan() : k.getTanggal(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorKasKecilDashboard.java:761"); }
				try { b.disetujui = k.getDisetujuiOleh() != null; } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorKasKecilDashboard.java:762"); }
				b.statusGanti = hitungStatusGanti(k);
				b.umurHari = b.tanggal == null ? 0 : Math.max(0, (now - b.tanggal.getTime()) / 86400000L);

				// Status pembayaran DPC / pengajuan transfer (kas kecil diganti via penggantian atau kas besar).
				DaftarPengajuanTransfer dpt = null;
				try {
					if (k.getPenggantianKasKecil() != null) {
						dpt = k.getPenggantianKasKecil().getDaftarPengajuanTransfer();
					}
					if (dpt == null && k.getKasBesar() != null) {
						dpt = k.getKasBesar().getDaftarPengajuanTransfer();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorKasKecilDashboard.java:775");
				}
				DpcTransferStatusHelper.Info dpc = DpcTransferStatusHelper.dari(dpt);
				b.dibayarDpc = dpc.sudahDibayar;
				b.statusTransfer = dpc.status;
				b.bank = dpc.bank;
				b.kodeTransfer = dpc.kodeTransfer;

				if (statusPilih != null && statusPilih.length() > 0 && !statusPilih.equals(b.statusGanti)) {
					continue;
				}
				hasil.add(b);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorKasKecilDashboard.java:792"); }
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorKasKecilDashboard.java:793"); }
			}
		}
		return hasil;
	}

	/** Tentukan status penggantian: belum ada penggantian, sedang diajukan, atau sudah disetujui. */
	private String hitungStatusGanti(KasKecil k) {
		try {
			if (k.getPenggantianKasKecil() == null) {
				return ST_BELUM;
			}
			return k.getPenggantianKasKecil().getDisetujuiOleh() != null ? ST_SUDAH : ST_PROSES;
		} catch (Exception e) {
			return ST_BELUM;
		}
	}

	// --------------------------------------------------------------------- util kecil

	private void kolom(Columns columns, String label, String width, String align) {
		MyColumnConfig col = new MyColumnConfig(label);
		if (width != null) {
			col.setWidth(width);
		}
		if (align != null) {
			col.setAlign(align);
		}
		col.setParent(columns);
	}

	private Label kanan(String teks) {
		Label l = new Label(teks);
		l.setStyle("text-align:right;display:block;");
		return l;
	}

	private String warnaStatus(String s) {
		if (ST_SUDAH.equals(s)) {
			return "#15803d";
		}
		if (ST_PROSES.equals(s)) {
			return "#b45309";
		}
		return "#dc2626";
	}

	private double nz(Double v) {
		return v == null ? 0.0 : v.doubleValue();
	}

	private String kosongKe(String s, String bila) {
		return s == null || s.trim().length() == 0 ? bila : s.trim();
	}

	private String pad2(int n) {
		return n < 10 ? "0" + n : String.valueOf(n);
	}

	private String labelBulan(String key) {
		String[] namaBulan = { "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des" };
		try {
			String[] p = key.split("-");
			int bln = Integer.parseInt(p[1]);
			return namaBulan[(bln - 1) % 12] + " " + p[0];
		} catch (Exception e) {
			return key;
		}
	}

	private Date awalHari(Date d) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	private Date akhirHari(Date d) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
		return cal.getTime();
	}

	private String number(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String money(double value) {
		return "Rp " + number(value);
	}

	private String fmt(double value) {
		return String.valueOf(Math.round(value));
	}

	private String fmtInt(int value) {
		return number(value);
	}

	private String dash(String v) {
		return v == null || v.trim().length() == 0 ? "-" : v.trim();
	}

	private String safe(String v) {
		return v == null ? "" : v.trim();
	}
}
