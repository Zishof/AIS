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
import ais.database.model.akunting.UangMuka;
import ais.ui.util.DashboardCardHelper;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>MonitorUangMukaDashboard — Dashboard Pemantauan Uang Muka &amp; Pembayaran (DPC)</h3>
 *
 * <p><b>Untuk apa dashboard ini.</b> Memantau seluruh pengeluaran uang muka organisasi dalam satu
 * halaman: berapa banyak dokumen dan total nilainya, sudah disetujui atau belum, <b>sudah dibayar
 * lewat DPC atau belum</b> (beserta <b>lewat bank apa</b> dan <b>status pengajuan transfernya sudah
 * sampai mana</b>), serta sudah dipertanggungjawabkan atau belum. Dipasang sebagai tab "Monitor" pada
 * menu Uang Muka, bersifat hanya-baca untuk analisis.</p>
 *
 * <p><b>Panel:</b> (1) kartu angka ringkas (KPI); (2) status pembayaran DPC (donut tangga proses
 * transfer) + dana dibayar per bank (batang); (3) sebaran nilai per satuan kerja (batang); (4) status
 * pertanggungjawaban (donut sudah/belum); (5) tabel rincian. Tiap panel diberi penjelasan bahasa
 * sederhana. Semua grafik HTML/CSS/SVG ({@link HtmlChartHelper}), tanpa JFreeChart, responsif.</p>
 *
 * <p><b>Sumber &amp; reuse.</b> Data {@link KasBesar} diambil sekali (openSession sendiri) lalu
 * diringkas ke {@code Baris}. Status pembayaran DPC dihitung memakai perkakas bersama
 * {@link DpcTransferStatusHelper} dari {@code KasBesar.getDaftarPengajuanTransfer()}; status
 * pertanggungjawaban dari {@code getPertangungjawabanKasBesar()}. Tata letak &amp; kartu memakai
 * {@link DashboardCardHelper}; tombol Cetak PDF + Ekspor Excel + progress memakai mesin bersama
 * {@link DashboardReportKit}. Berbagi pola dengan {@code MonitorKasKecilDashboard} agar konsisten dan
 * mudah dirawat.</p>
 */
public class MonitorUangMukaDashboard extends Vbox {

	private static final long serialVersionUID = 7710254419883006612L;

	private static final int GRID_PAGE_SIZE = 50;
	private static final int LIMIT = 5000;

	private Textbox keyword;
	private Combobox dpcFilter;
	private MyDatebox tglMulai;
	private MyDatebox tglSampai;
	private Vbox body;

	/**
	 * Tipe implementasi bersarang {@link Baris} milik {@link MonitorUangMukaDashboard}. Kelas ini memberi nama
	 * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * MonitorUangMukaDashboard}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
	 * dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String kode}, {@code String nama},
	 * {@code String satker}, {@code String jenis}, {@code double nilai}, {@code Date tanggal}, {@code boolean
	 * disetujui}, {@code boolean dibayarDpc}. Aturan bisnis bersama tetap berada pada kelas induk atau service
	 * yang dipanggilnya.</p>
	 *
	 * @see MonitorUangMukaDashboard
	 */
	private static class Baris {
		String kode;
		String nama;
		String satker;
		String jenis;
		double nilai;
		Date tanggal;
		boolean disetujui;
		boolean dibayarDpc;
		String statusTransfer;
		String bank;
		boolean sudahPtj;
		String ptjStatus;
	}

	public MonitorUangMukaDashboard() {
		setWidth("100%");
		setHeight("100%");
		setStyle("overflow:auto;background:#f1f5f9;padding:12px;box-sizing:border-box;");
		buildLayout();
		reload();
	}

	private void buildLayout() {
		Toolbar toolbar = new Toolbar();
		toolbar.setWidth("100%");
		toolbar.setStyle("padding:10px;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;margin-bottom:12px;");
		toolbar.setParent(this);

		new Label("Cari judul / kode: ").setParent(toolbar);
		keyword = new Textbox();
		keyword.setWidth("200px");
		keyword.setParent(toolbar);
		keyword.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload();
			}
		});

		new Label(ais.common.Common.getBahasaConfig(" Pembayaran: ")).setParent(toolbar);
		dpcFilter = new Combobox();
		dpcFilter.setReadonly(true);
		dpcFilter.setWidth("160px");
		tambahCombo(dpcFilter, "Semua", "");
		tambahCombo(dpcFilter, "Sudah Dibayar DPC", "DIBAYAR");
		tambahCombo(dpcFilter, "Belum Dibayar", "BELUM");
		dpcFilter.setSelectedIndex(0);
		dpcFilter.setParent(toolbar);

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

	private void reload() {
		Common.clear(body);
		List<Baris> data = loadData();
		renderRingkasan(data);
		renderDpc(data);
		renderSatkerDanPtj(data);
		renderGrid(data);
	}

	private void renderRingkasan(List<Baris> data) {
		int total = data.size(), setuju = 0, dibayar = 0, ptj = 0;
		double nilai = 0, nilaiDibayar = 0;
		for (Baris b : data) {
			nilai += b.nilai;
			if (b.disetujui) {
				setuju++;
			}
			if (b.dibayarDpc) {
				dibayar++;
				nilaiDibayar += b.nilai;
			}
			if (b.sudahPtj) {
				ptj++;
			}
		}
		Panelchildren isi = DashboardCardHelper.panel(body, "Ringkasan Uang Muka",
				"Kondisi uang muka sekarang dalam angka besar: jumlah dokumen, total nilai, yang sudah "
						+ "dibayar lewat DPC, dan yang sudah dipertanggungjawabkan.");
		String baris = DashboardCardHelper.barisKartu(
				DashboardCardHelper.kartu("Jumlah Uang Muka", fmtInt(total), "dokumen uang muka", "#1d4ed8"),
				DashboardCardHelper.kartu("Total Nilai", money(nilai), "seluruh pengeluaran", "#4338ca"),
				DashboardCardHelper.kartu("Sudah Disetujui", fmtInt(setuju), "dari " + total + " dokumen", "#0369a1"),
				DashboardCardHelper.kartu("Sudah Dibayar DPC", fmtInt(dibayar), money(nilaiDibayar) + " cair", "#15803d"),
				DashboardCardHelper.kartu("Dipertanggungjawabkan", fmtInt(ptj), "sudah ada SPJ", "#b45309"));
		new MyHtml(baris).setParent(isi);
	}

	private void renderDpc(List<Baris> data) {
		String[] urut = { DpcTransferStatusHelper.BELUM_AJU, DpcTransferStatusHelper.DIAJUKAN,
				DpcTransferStatusHelper.PROSES, DpcTransferStatusHelper.DISETUJUI, DpcTransferStatusHelper.DIBAYAR };
		Map<String, Integer> perStatus = new LinkedHashMap<String, Integer>();
		for (String u : urut) {
			perStatus.put(u, 0);
		}
		Map<String, Double> perBank = new LinkedHashMap<String, Double>();
		for (Baris b : data) {
			String st = b.statusTransfer == null ? DpcTransferStatusHelper.BELUM_AJU : b.statusTransfer;
			Integer v = perStatus.get(st);
			perStatus.put(st, (v == null ? 0 : v) + 1);
			if (b.dibayarDpc) {
				String bk = b.bank == null || b.bank.trim().isEmpty() ? "(Tanpa Bank)" : b.bank.trim();
				Double bv = perBank.get(bk);
				perBank.put(bk, (bv == null ? 0 : bv) + b.nilai);
			}
		}
		String[] labels = perStatus.keySet().toArray(new String[perStatus.size()]);
		double[] vals = new double[labels.length];
		for (int i = 0; i < labels.length; i++) {
			vals[i] = perStatus.get(labels[i]);
		}
		String donut = HtmlChartHelper.donut("Status Pembayaran (DPC)",
				"Sudah sampai mana pengajuan transfer dana uang muka: dari belum diajukan sampai sudah dibayar (cair).",
				labels, vals, new String[] { "#94a3b8", "#f59e0b", "#3b82f6", "#8b5cf6", "#16a34a" }, "belum diajukan");
		String[] bankLabels = perBank.keySet().toArray(new String[perBank.size()]);
		double[] bankVals = new double[bankLabels.length];
		for (int i = 0; i < bankLabels.length; i++) {
			bankVals[i] = perBank.get(bankLabels[i]);
		}
		String bar = HtmlChartHelper.barHorizontal("Dana Dibayar per Bank",
				"Total dana uang muka yang sudah cair (DPC) dikelompokkan menurut bank pembayarnya.",
				bankLabels, bankVals, "#0f766e");
		Panelchildren isi = DashboardCardHelper.panel(body, "Pembayaran & Transfer (DPC)",
				"Memantau apakah dana uang muka sudah dibayar lewat DPC, lewat bank apa, dan sudah sampai mana prosesnya.");
		new MyHtml(DashboardCardHelper.barisChart(donut, bar)).setParent(isi);
	}

	private void renderSatkerDanPtj(List<Baris> data) {
		Map<String, Double> perSatker = new LinkedHashMap<String, Double>();
		int ptj = 0, belumPtj = 0;
		for (Baris b : data) {
			String key = b.satker == null || b.satker.trim().isEmpty() ? "(Tanpa Satuan Kerja)" : b.satker.trim();
			Double v = perSatker.get(key);
			perSatker.put(key, (v == null ? 0 : v) + b.nilai);
			if (b.sudahPtj) {
				ptj++;
			} else {
				belumPtj++;
			}
		}
		List<Map.Entry<String, Double>> urut = new ArrayList<Map.Entry<String, Double>>(perSatker.entrySet());
		java.util.Collections.sort(urut, new java.util.Comparator<Map.Entry<String, Double>>() {
			@Override
			public int compare(Map.Entry<String, Double> a, Map.Entry<String, Double> b2) {
				return Double.compare(b2.getValue(), a.getValue());
			}
		});
		int n = Math.min(12, urut.size());
		String[] labels = new String[n];
		double[] vals = new double[n];
		for (int i = 0; i < n; i++) {
			labels[i] = urut.get(i).getKey();
			vals[i] = urut.get(i).getValue();
		}
		String bar = HtmlChartHelper.barHorizontal("Nilai per Satuan Kerja",
				"Unit kerja mana yang paling besar pengeluaran uang mukanya.", labels, vals, "#4338ca");
		String donut = HtmlChartHelper.donut("Status Pertanggungjawaban",
				"Perbandingan uang muka yang sudah dan belum dibuat laporan pertanggungjawabannya (SPJ).",
				new String[] { "Sudah SPJ", "Belum SPJ" }, new double[] { ptj, belumPtj },
				new String[] { "#16a34a", "#f59e0b" }, "sudah SPJ");
		Panelchildren isi = DashboardCardHelper.panel(body, "Sebaran & Pertanggungjawaban",
				"Melihat pengeluaran per unit kerja dan seberapa banyak yang sudah dibuat SPJ-nya.");
		new MyHtml(DashboardCardHelper.barisChart(bar, donut)).setParent(isi);
	}

	private void renderGrid(List<Baris> data) {
		Panelchildren isi = DashboardCardHelper.panel(body, "Rincian Uang Muka",
				"Daftar lengkap tiap uang muka beserta nilai, status transfer, bank, pembayaran DPC, dan SPJ.");
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
		kolom(columns, "Anggaran", null, null);
		kolom(columns, "Nilai", null, "right");
		kolom(columns, "Status Transfer", null, "center");
		kolom(columns, "Via Bank", null, null);
		kolom(columns, "Dibayar DPC", "85px", "center");
		kolom(columns, "SPJ", "70px", "center");
		kolom(columns, "Tanggal", null, null);

		Rows rows = new Rows();
		rows.setParent(grid);
		int no = 0;
		for (Baris b : data) {
			no++;
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(String.valueOf(no)));
			row.appendChild(new Label(dash(b.kode) + (b.nama == null || b.nama.trim().isEmpty() ? "" : "\n" + b.nama)));
			row.appendChild(new Label(dash(b.satker)));
			row.appendChild(new Label(dash(b.jenis)));
			row.appendChild(kanan(money(b.nilai)));
			Label stT = new Label(dash(b.statusTransfer));
			stT.setStyle("color:" + (b.dibayarDpc ? "#15803d" : "#475569") + ";");
			row.appendChild(stT);
			row.appendChild(new Label(dash(b.bank)));
			Label dpcL = new Label(b.dibayarDpc ? "Ya" : "Belum");
			dpcL.setStyle("font-weight:600;color:" + (b.dibayarDpc ? "#15803d" : "#b45309") + ";");
			row.appendChild(dpcL);
			Label ptjL = new Label(b.sudahPtj ? "Ya" : "Belum");
			ptjL.setStyle("font-weight:600;color:" + (b.sudahPtj ? "#15803d" : "#b45309") + ";");
			row.appendChild(ptjL);
			row.appendChild(new Label(b.tanggal == null ? "-" : Common.dateFormat3.get().format(b.tanggal)));
		}
		if (data.isEmpty()) {
			Row row = new Row();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "10");
			row.appendChild(new Label("Tidak ada uang muka pada rentang/filter ini."));
		}
	}

	@SuppressWarnings("unchecked")
	private List<Baris> loadData() {
		List<Baris> hasil = new ArrayList<Baris>();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Criteria c = session.createCriteria(UangMuka.class)
					.createAlias("satuanKerja", "satuanKerja", Criteria.LEFT_JOIN);
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

			String dpcPilih = dpcFilter == null || dpcFilter.getSelectedItem() == null ? ""
					: String.valueOf(dpcFilter.getSelectedItem().getValue());

			List<UangMuka> list = c.list();
			for (UangMuka k : list) {
				if (k == null) {
					continue;
				}
				Baris b = new Baris();
				try { b.kode = k.getKode(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorUangMukaDashboard.java:359"); }
				try { b.nama = k.getNama(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorUangMukaDashboard.java:360"); }
				try { b.satker = k.getSatuanKerja() == null ? null : k.getSatuanKerja().getNama(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorUangMukaDashboard.java:361"); }
				try { b.jenis = k.getWorkspace() == null ? null : k.getWorkspace().getNama(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorUangMukaDashboard.java:362"); }
				try { b.nilai = k.getNilai() == null ? 0.0 : k.getNilai().doubleValue(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorUangMukaDashboard.java:363"); }
				try { b.tanggal = k.getTanggalPembuatan(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorUangMukaDashboard.java:364"); }
				try { b.disetujui = k.getDisetujuiOleh() != null; } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorUangMukaDashboard.java:365"); }
				try {
					b.sudahPtj = k.getPertangungjawaban() != null;
					if (k.getPertangungjawaban() != null) {
						b.ptjStatus = k.getPertangungjawaban().getDisetujuiOleh() != null ? "SPJ Disetujui" : "SPJ Diajukan";
					} else {
						b.ptjStatus = "Belum SPJ";
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorUangMukaDashboard.java:373"); }
				DaftarPengajuanTransfer dpt = null;
				try { dpt = k.getDaftarPengajuanTransfer(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorUangMukaDashboard.java:375"); }
				DpcTransferStatusHelper.Info dpc = DpcTransferStatusHelper.dari(dpt);
				b.dibayarDpc = dpc.sudahDibayar;
				b.statusTransfer = dpc.status;
				b.bank = dpc.bank;

				if ("DIBAYAR".equals(dpcPilih) && !b.dibayarDpc) {
					continue;
				}
				if ("BELUM".equals(dpcPilih) && b.dibayarDpc) {
					continue;
				}
				hasil.add(b);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorUangMukaDashboard.java:393"); }
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/MonitorUangMukaDashboard.java:394"); }
			}
		}
		return hasil;
	}

	private DashboardReportKit.SumberLaporan buatSumberLaporan() {
		return new DashboardReportKit.SumberLaporan() {
			@Override
			public String judul() {
				return "Monitor Uang Muka";
			}

			@Override
			public String subjudul() {
				return "Pemantauan Uang Muka & Pembayaran DPC";
			}

			@Override
			public String deskripsi() {
				return "Melihat nilai, status transfer/pembayaran DPC, bank, dan pertanggungjawaban uang muka.";
			}

			@Override
			public List<DashboardReportKit.Bagian> bagian() {
				final List<Baris> data = loadData();
				List<DashboardReportKit.Bagian> b = new ArrayList<DashboardReportKit.Bagian>();
				b.add(DashboardReportKit.kpi("Ringkasan",
						"Jumlah uang muka, total nilai, yang sudah dibayar DPC, dan yang sudah dipertanggungjawabkan.",
						new DashboardReportKit.PenyediaBaris() {
							@Override
							public List<Object[]> ambil() {
								int tot = data.size(), setuju = 0, bayar = 0, ptj = 0;
								double nil = 0, nilB = 0;
								for (Baris x : data) {
									nil += x.nilai;
									if (x.disetujui) setuju++;
									if (x.dibayarDpc) { bayar++; nilB += x.nilai; }
									if (x.sudahPtj) ptj++;
								}
								List<Object[]> o = new ArrayList<Object[]>();
								o.add(new Object[] { "Jumlah Uang Muka", DashboardReportKit.fmt(tot), "dokumen" });
								o.add(new Object[] { "Total Nilai", money(nil), "seluruh" });
								o.add(new Object[] { "Sudah Disetujui", DashboardReportKit.fmt(setuju), "" });
								o.add(new Object[] { "Sudah Dibayar DPC", DashboardReportKit.fmt(bayar), money(nilB) });
								o.add(new Object[] { "Dipertanggungjawabkan", DashboardReportKit.fmt(ptj), "sudah SPJ" });
								return o;
							}
						}));
				b.add(DashboardReportKit.donut("Status Pembayaran (DPC)",
						"Sudah sampai mana pengajuan transfer dana uang muka.", new String[] { "Status", "Jumlah" },
						new DashboardReportKit.PenyediaBaris() {
							@Override
							public List<Object[]> ambil() {
								String[] urut = { DpcTransferStatusHelper.BELUM_AJU, DpcTransferStatusHelper.DIAJUKAN,
										DpcTransferStatusHelper.PROSES, DpcTransferStatusHelper.DISETUJUI,
										DpcTransferStatusHelper.DIBAYAR };
								Map<String, Integer> m = new LinkedHashMap<String, Integer>();
								for (String u : urut) m.put(u, 0);
								for (Baris x : data) {
									String st = x.statusTransfer == null ? DpcTransferStatusHelper.BELUM_AJU : x.statusTransfer;
									Integer v = m.get(st); m.put(st, (v == null ? 0 : v) + 1);
								}
								List<Object[]> o = new ArrayList<Object[]>();
								for (Map.Entry<String, Integer> e : m.entrySet()) o.add(new Object[] { e.getKey(), Long.valueOf(e.getValue()) });
								return o;
							}
						}));
				b.add(DashboardReportKit.tabel("Rincian Uang Muka", "Daftar uang muka & status pembayaran.",
						new String[] { "Kode", "Nama", "Satuan Kerja", "Anggaran", "Nilai", "Status Transfer", "Via Bank",
								"Dibayar DPC", "SPJ", "Tanggal" },
						new DashboardReportKit.PenyediaBaris() {
							@Override
							public List<Object[]> ambil() {
								List<Object[]> o = new ArrayList<Object[]>();
								for (Baris x : data) {
									o.add(new Object[] { dash(x.kode), dash(x.nama), dash(x.satker), dash(x.jenis),
											money(x.nilai), dash(x.statusTransfer), dash(x.bank),
											x.dibayarDpc ? "Ya" : "Belum", x.sudahPtj ? "Ya" : "Belum",
											x.tanggal == null ? "-" : Common.dateFormat3.get().format(x.tanggal) });
								}
								return o;
							}
						}));
				return b;
			}
		};
	}

	// ---------------------------------------------------------------- util
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
