package ais.action.master.akunting.helper;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.Type;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.MoveEvent;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.KegiatanAction;
import ais.action.master.helper.AmbilDataJurusanBanbox;
import ais.action.master.helper.KegiatanProsesHeper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;

import org.zkoss.zul.Html;

/**
 * Memantau tagihan, pembayaran, dan piutang mahasiswa agar tindak lanjut pembayaran lebih tepat sasaran.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardPembayaranPerguruanTinggi extends MyPortallayout {

	private static final long serialVersionUID = -9006490521125337935L;

	public DasboardPembayaranPerguruanTinggi() throws Exception {
		super();
		setWidth("100%");
		setMaximizedMode("whole");
		init();
	}

	private int width = 550;
	private int height = 110;

	/**
	 * PEMBERSIH SESSION TERPUSAT - PENCEGAHAN MEMORY LEAK (OOM)
	 */
	private static void cleanupSession(Session session) {
		ais.ui.util.DashboardModernHtmlUtil.closeOpenedSession(session);
	}

	private void init() throws Exception {

		EventListener reloadPiutangPerProdi = new EventListener() {

			private Jurusan sk = null;
			private String c = Common.getCurrentTahunAkademik();
			private String s = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
			private JenisKegiatan jenisKegiatan = ConstantValues.PENDAFTARAN_MAHASISWA_LAMA;
			private boolean tampilChart = false;

			// State untuk Panel Tren Baru
			private String searchNamaTrend = "";
			private JenisKegiatan filterJkTrend = null;

			// State loading/progress dashboard agar pengguna melihat proses data yang sedang diambil.
			private MyPortalchildren dashboardLoadingPortalchildren;
			private Panelchildren dashboardLoadingPanelchildren;

			private void lanjutkanTahapanDashboard(final int tahap) {
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						jalankanTahapanDashboard(tahap);
					}
				});
			}

			private void jalankanTahapanDashboard(int tahap) throws Exception {
				if (tahap == 0) {
					Common.clear(DasboardPembayaranPerguruanTinggi.this);
					dashboardLoadingPortalchildren = null;
					dashboardLoadingPanelchildren = null;
					tampilkanLoadingDashboardPembayaran("Menyiapkan Dasbor Pembayaran",
							"Menyiapkan ringkasan pembayaran dan tagihan mahasiswa.", 3);
					lanjutkanTahapanDashboard(1);
					return;
				}

				if (tahap == 1) {
					tampilkanLoadingDashboardPembayaran("Mengambil Ringkasan Utama",
							"Menghitung total tagihan, pembayaran, piutang aktif, lunas, belum bayar, dan lebih bayar.", 12);
					pembayaranOverviewModern();
					lanjutkanTahapanDashboard(2);
					return;
				}

				if (tahap == 2) {
					tampilkanLoadingDashboardPembayaran("Mengambil Tren Aktivitas Pembayaran",
							"Mengolah cicilan pembayaran per hari, bulan, dan semester.", 28);
					pembayaranTrendActivity();
					lanjutkanTahapanDashboard(3);
					return;
				}

				if (tahap == 3) {
					tampilkanLoadingDashboardPembayaran("Mengambil Piutang Per-Prodi",
							"Mengelompokkan tagihan, pembayaran, dan sisa piutang berdasarkan program studi.", 44);
					piutangMahasiswa();
					lanjutkanTahapanDashboard(4);
					return;
				}

				if (tahap == 4) {
					tampilkanLoadingDashboardPembayaran("Mengambil Piutang Per-Angkatan",
							"Mengelompokkan tagihan, pembayaran, dan sisa piutang berdasarkan angkatan mahasiswa.", 60);
					piutangMahasiswaPerAngkatan();
					lanjutkanTahapanDashboard(5);
					return;
				}

				if (tahap == 5) {
					tampilkanLoadingDashboardPembayaran("Mengambil Piutang Per-Status",
							"Mengelompokkan data berdasarkan status awal mahasiswa.", 74);
					piutangMahasiswaStatusAwal();
					lanjutkanTahapanDashboard(6);
					return;
				}

				if (tahap == 6) {
					tampilkanLoadingDashboardPembayaran("Mengambil Piutang Per-Program",
							"Mengelompokkan tagihan dan piutang berdasarkan program mahasiswa.", 88);
					piutangMahasiswaProgram();
					lanjutkanTahapanDashboard(7);
					return;
				}

				if (tahap == 7) {
					tampilkanLoadingDashboardPembayaran("Mengambil Piutang Per-Validator",
							"Mengelompokkan pembayaran berdasarkan validator/petugas validasi.", 97);
					piutangMahasiswaPerValidator();
					lanjutkanTahapanDashboard(8);
					return;
				}

				if (tahap == 8) {
					tampilkanLoadingDashboardPembayaran("Dashboard Siap",
							"Seluruh ringkasan dan panel rincian pembayaran berhasil dimuat.", 100);
					lanjutkanTahapanDashboard(9);
					return;
				}

				selesaiLoadingDashboardPembayaran();
			}

			private void tampilkanLoadingDashboardPembayaran(String judul, String keterangan, int persen) {
				if (dashboardLoadingPortalchildren == null || dashboardLoadingPortalchildren.getParent() == null) {
					dashboardLoadingPortalchildren = new MyPortalchildren();
					dashboardLoadingPortalchildren.setWidth("100%");
					dashboardLoadingPortalchildren.setStyle("padding:6px; box-sizing:border-box;");
					dashboardLoadingPortalchildren.setParent(DasboardPembayaranPerguruanTinggi.this);

					Panel panel = new ais.ui.util.MyPanelConfig();
					panel.setTitle("Status Proses Dashboard");
					panel.setBorder("none");
					panel.setCollapsible(false);
					panel.setClosable(false);
					panel.setMaximizable(false);
					panel.setMinimizable(false);
					panel.setStyle("margin-bottom:12px; border:1px solid #dbeafe; border-radius:18px; overflow:hidden;"
							+ "background:#ffffff; box-shadow:0 14px 28px rgba(37,99,235,.12);");
					panel.setParent(dashboardLoadingPortalchildren);

					dashboardLoadingPanelchildren = new Panelchildren();
					dashboardLoadingPanelchildren.setStyle("padding:0; background:#ffffff;");
					dashboardLoadingPanelchildren.setParent(panel);
				}
				if (dashboardLoadingPanelchildren == null) {
					return;
				}
				Common.clear(dashboardLoadingPanelchildren);
				new org.zkoss.zul.Html(buildLoadingDashboardHtml(judul, keterangan, persen, true))
						.setParent(dashboardLoadingPanelchildren);
			}

			private void selesaiLoadingDashboardPembayaran() {
				try {
					if (dashboardLoadingPortalchildren != null && dashboardLoadingPortalchildren.getParent() != null) {
						dashboardLoadingPortalchildren.detach();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPembayaranPerguruanTinggi.java:257");
				}
				dashboardLoadingPortalchildren = null;
				dashboardLoadingPanelchildren = null;
			}

			private void tampilkanLoadingPanel(Panelchildren parent, String judul, String keterangan, int persen) {
				if (parent == null) {
					return;
				}
				try {
					Common.clear(parent);
					new org.zkoss.zul.Html(buildLoadingDashboardHtml(judul, keterangan, persen, false)).setParent(parent);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPembayaranPerguruanTinggi.java:270");
				}
			}

			private String buildLoadingDashboardHtml(String judul, String keterangan, int persen, boolean besar) {
				int p = persen < 0 ? 0 : (persen > 100 ? 100 : persen);
				String titleSize = besar ? "16px" : "13px";
				String padding = besar ? "18px 20px" : "14px 16px";
				return "<div style='padding:" + padding + "; background:linear-gradient(135deg,#eff6ff 0%,#ffffff 60%);"
						+ "border:1px solid #dbeafe; border-radius:16px; box-sizing:border-box; color:#1e3a8a;'>"
						+ "<div style='display:flex; align-items:center; justify-content:space-between; gap:10px; flex-wrap:wrap;'>"
						+ "<div>"
						+ "<div style='font-size:" + titleSize + "; font-weight:800; color:#0f172a;'>"
						+ "<i class=\"fa fa-spinner fa-spin\"></i> " + escapePaymentHtml(judul) + "</div>"
						+ "<div style='font-size:12px; color:#475569; margin-top:5px; line-height:1.45;'>"
						+ escapePaymentHtml(keterangan) + "</div>"
						+ "</div>"
						+ "<div style='font-size:22px; font-weight:900; color:#2563eb;'>" + p + "%</div>"
						+ "</div>"
						+ "<div style='margin-top:12px; width:100%; height:12px; border-radius:999px; background:#e2e8f0; overflow:hidden;'>"
						+ "<div style='width:" + p + "%; height:12px; border-radius:999px;"
						+ "background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4)); transition:width .35s ease;'></div>"
						+ "</div>"
						+ "<div style='font-size:11px; color:#64748b; margin-top:8px;'>Mohon tunggu, data sedang diproses secara bertahap agar halaman tetap informatif.</div>"
						+ "</div>";
			}



			/**
			 * PANEL BARU: TREN AKTIVITAS PEMBAYARAN (CicilanPembayaran) Menampilkan grafik
			 * per hari, minggu, bulan, dan semester.
			 */
			private void pembayaranTrendActivity() throws Exception {
				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardPembayaranPerguruanTinggi.this);
				portalchildren.setWidth("100%"); // Tren dibuat lebar penuh

				Panel panel = new Panel();
				panel.setTitle("Analisis Tren Aktivitas Pembayaran");
				panel.setBorder("normal");
				panel.setCollapsible(true);
				panel.setStyle("margin-bottom:20px; border-radius: 10px; margin-left: 3px; margin-right: 3px;");
				portalchildren.appendChild(panel);

				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				EventListener loadTrendListener = new EventListener() {

					private EventListener getThis() {
						return this;
					}

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.clear(panelchildren);

						Toolbar tb = new Toolbar();
						tb.setParent(panelchildren);

						tb.appendChild(new MyLabelAgakKecil("Nama/NIM:"));
						final MyTextbox txtNama = new MyTextbox(searchNamaTrend);
						txtNama.setWidth("150px");
						txtNama.setParent(tb);

						tb.appendChild(new MyLabelAgakKecil("Jenis Kegiatan:"));
						final Combobox cbJk = new Combobox();
						cbJk.setReadonly(true);
						Common.insertComboDanSemua(cbJk, "namaKegiatan", JenisKegiatan.class,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
						Common.selectComboItem(cbJk, filterJkTrend);
						cbJk.setParent(tb);

						final Combobox cari = Common.generateTahunAjaran(null);
						final Combobox jenissemester = new Combobox();

						cari.setParent(tb);
						jenissemester.setParent(tb);

						MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
						comboitem.setValue(Perkuliahan.GANJIL);
						jenissemester.appendChild(comboitem);

						comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
						comboitem.setValue(Perkuliahan.GENAP);
						jenissemester.appendChild(comboitem);

						comboitem = new MyComboitemConfig("Semua");
						comboitem.setValue(null);
						jenissemester.appendChild(comboitem);
						jenissemester.setSelectedItem(comboitem);
						jenissemester.setReadonly(true);

						Common.selectComboItem(jenissemester, s);

						MyToolbarbuttonConfig btnCari = new MyToolbarbuttonConfig("Filter Tren", "/img/search.png");
						btnCari.setParent(tb);
						btnCari.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event e) throws Exception {
								searchNamaTrend = txtNama.getValue().trim();
								filterJkTrend = (JenisKegiatan) (cbJk.getSelectedItem() == null ? null
										: cbJk.getSelectedItem().getValue());

								c = cari.getValue() != null ? cari.getValue().trim() : "";
								s = (jenissemester.getSelectedItem() == null
										|| jenissemester.getSelectedItem().getValue() == null) ? null
												: jenissemester.getSelectedItem().getValue() + "";

								tampilkanLoadingPanel(panelchildren, "Memuat Ulang Data Dashboard",
										"Filter berubah. Sistem sedang mengambil ulang data pembayaran sesuai parameter terbaru.", 8);
								Common.createDefaultTimer(getThis());
							}
						});

						// Agregasi Data Tren
						TreeMap<String, Double> trendHari = new TreeMap<String, Double>();
						TreeMap<String, Double> trendBulan = new TreeMap<String, Double>();
						TreeMap<Integer, Double> trendSemester = new TreeMap<Integer, Double>();

						SimpleDateFormat sdfHari = new SimpleDateFormat("yyyy-MM-dd");
						// Kunci bulan dibuat "yyyy-MM" agar bisa diurutkan kronologis (bukan alfabetis).
						SimpleDateFormat sdfBulan = new SimpleDateFormat("yyyy-MM");

						Session session = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();
							Criteria crit = session.createCriteria(CicilanPembayaran.class, "cp");
							crit.createAlias("cp.kegiatan", "k");

							// Filter Berdasarkan Tahun Akademik & Semester Dashboard (Opsional, agar
							// sinkron)
							crit.add(Restrictions.eq("k.tahunAkademik", c));

							if (filterJkTrend != null) {
								crit.add(Restrictions.eq("k.jenisKegiatan", filterJkTrend));
							}

							crit.add(s == null ? Restrictions.sqlRestriction("1=1")
									: s.equalsIgnoreCase(Perkuliahan.GENAP) ? Restrictions.in("k.semster", Common.genap)
											: Restrictions.in("k.semster", Common.ganjil));

							if (!searchNamaTrend.isEmpty()) {
								crit.createAlias("k.mahasiswa", "m", Criteria.LEFT_JOIN);
								crit.createAlias("k.calonMahasiswa", "cm", Criteria.LEFT_JOIN);
								crit.add(Restrictions.or(
										Restrictions.ilike("m.nama", searchNamaTrend, MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("m.nim", searchNamaTrend, MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("cm.nama", searchNamaTrend,
																MatchMode.ANYWHERE),
														Restrictions.ilike("cm.noRegistrasi", searchNamaTrend,
																MatchMode.ANYWHERE)))));
							}

							List<CicilanPembayaran> list = crit.list();
							for (CicilanPembayaran cp : list) {
								double nilai = cp.getNilai() != null ? cp.getNilai() : 0.0;
								// Tanggal aman: bila kolom "tanggal" rusak (mis. tahun 0002/2003/2004),
								// otomatis fallback ke tanggalKwitansi agar tren tidak melenceng.
								Date tgl = resolveTanggalTrenEfektif(cp);

								// Agregasi Hari
								String sHari = sdfHari.format(tgl);
								Double nilaiHari = trendHari.get(sHari);
								trendHari.put(sHari, (nilaiHari == null ? 0.0 : nilaiHari.doubleValue()) + nilai);

								// Agregasi Bulan
								String sBulan = sdfBulan.format(tgl);
								Double nilaiBulan = trendBulan.get(sBulan);
								trendBulan.put(sBulan, (nilaiBulan == null ? 0.0 : nilaiBulan.doubleValue()) + nilai);

								// Agregasi Semester
								Integer smt = cp.getKegiatan().getSemster();
								if (smt != null) {
									Double nilaiSemester = trendSemester.get(smt);
									trendSemester.put(smt, (nilaiSemester == null ? 0.0 : nilaiSemester.doubleValue()) + nilai);
								}
							}
						} finally {
							cleanupSession(session);
						}

						// Penjelasan singkat untuk awam.
						panelchildren.appendChild(new Html(DashboardUiKit.descChip(
								"Lihat kapan uang pembayaran paling banyak masuk — per hari, per bulan, dan per semester. "
										+ "Yang paling baru selalu tampil di atas, dan tiap halaman memuat 10 baris.")));

						// Urutkan menurun (terbaru dulu), lalu sajikan sebagai tabel ber-paging 10 baris.
						LinkedHashMap<String, Double> hariDesc = new LinkedHashMap<String, Double>();
						for (Map.Entry<String, Double> entry : trendHari.descendingMap().entrySet()) {
							hariDesc.put(formatTanggalHari(entry.getKey()), entry.getValue());
						}
						LinkedHashMap<String, Double> bulanDesc = new LinkedHashMap<String, Double>();
						for (Map.Entry<String, Double> entry : trendBulan.descendingMap().entrySet()) {
							bulanDesc.put(formatBulan(entry.getKey()), entry.getValue());
						}
						LinkedHashMap<String, Double> smtDesc = new LinkedHashMap<String, Double>();
						for (Map.Entry<Integer, Double> entry : trendSemester.descendingMap().entrySet()) {
							smtDesc.put("Semester " + entry.getKey(), entry.getValue());
						}

						// Susunan responsif: 3 kolom di desktop, menumpuk rapi di HP.
						MyPortallayout trenPortal = new MyPortallayout();
						trenPortal.setWidth("100%");
						trenPortal.setParent(panelchildren);
						String trenWidth = Common.isMobile() ? "100%" : "33%";

						buatPanelTren(trenPortal, trenWidth, "Tren Harian", "Tanggal",
								"Total pembayaran masuk tiap hari; tanggal terbaru di paling atas.", hariDesc,
								DashboardUiKit.PRIMARY);
						buatPanelTren(trenPortal, trenWidth, "Tren Bulanan", "Bulan",
								"Total pembayaran masuk tiap bulan; bulan terbaru di paling atas.", bulanDesc,
								DashboardUiKit.ACCENT);
						buatPanelTren(trenPortal, trenWidth, "Tren Per-Semester", "Semester",
								"Total pembayaran tiap semester; semester terbaru di paling atas.", smtDesc,
								DashboardUiKit.GOOD);
					}
				};
				loadTrendListener.onEvent(null);
			}

			/**
			 * Tanggal efektif untuk agregasi tren pembayaran (harian/bulanan).
			 *
			 * Sebagian data lama menyimpan kolom "tanggal" yang rusak (mis. tahun 0002, 2003,
			 * atau 2004) sehingga tren menampilkan periode yang tidak masuk akal. Bila tanggal
			 * utama tidak wajar, sistem memakai tanggalKwitansi sebagai cadangan. Bila keduanya
			 * tidak wajar, nilai tetap dipertahankan agar total pembayaran tidak hilang dari grafik.
			 */
			private Date resolveTanggalTrenEfektif(CicilanPembayaran cp) {
				if (cp == null) {
					return ais.ui.util.WaktuUtil.getDate();
				}
				Date tgl = cp.getTanggal();
				if (isTahunPembayaranWajar(tgl)) {
					return tgl;
				}
				Date tglKwitansi = cp.getTanggalKwitansi();
				if (isTahunPembayaranWajar(tglKwitansi)) {
					return tglKwitansi;
				}
				return tglKwitansi != null ? tglKwitansi : tgl;
			}

			/**
			 * Tahun dianggap wajar bila >= 2000 dan tidak lebih dari satu tahun ke depan.
			 * Batas atas memberi toleransi pembayaran semester genap yang melewati pergantian
			 * tahun, sekaligus menyaring tanggal rusak yang nilainya terlampau jauh.
			 */
			private boolean isTahunPembayaranWajar(Date tgl) {
				if (tgl == null) {
					return false;
				}
				Calendar cal = Calendar.getInstance();
				cal.setTime(tgl);
				int tahun = cal.get(Calendar.YEAR);
				int tahunSekarang = Calendar.getInstance().get(Calendar.YEAR);
				return tahun >= 2000 && tahun <= tahunSekarang + 1;
			}

			/**
			 * Buat satu sub-panel tren (Harian/Bulanan/Semester) berisi penjelasan singkat + tabel
			 * ber-paging 10 baris dengan batang HTML/CSS. {@code data} sudah terurut terbaru-dulu.
			 */
			private void buatPanelTren(MyPortallayout portal, String width, String judul, String labelKolom,
					String deskripsi, LinkedHashMap<String, Double> data, String warna) {
				MyPortalchildren pc = new MyPortalchildren();
				pc.setWidth(width);
				pc.setStyle("padding:5px; box-sizing:border-box;");
				pc.setParent(portal);

				Panel panel = new ais.ui.util.MyPanelConfig();
				panel.setTitle(judul);
				panel.setStyle("margin-bottom:10px; border:1px solid #e2e8f0; border-radius:12px; overflow:hidden;");
				pc.appendChild(panel);

				Panelchildren body = new Panelchildren();
				body.setStyle("padding:8px;");
				body.setParent(panel);
				body.appendChild(new Html(DashboardUiKit.descChip(deskripsi)));
				DashboardUiKit.trendGrid(labelKolom, "Nilai (Rp)", data, warna, true, 10).setParent(body);
			}

			/** Ubah kunci "yyyy-MM-dd" menjadi label tanggal yang ramah dibaca. */
			private String formatTanggalHari(String key) {
				try {
					Date d = new SimpleDateFormat("yyyy-MM-dd").parse(key);
					return new SimpleDateFormat("dd MMM yyyy").format(d);
				} catch (Exception e) {
					return key;
				}
			}

			/** Ubah kunci "yyyy-MM" menjadi label bulan yang ramah dibaca. */
			private String formatBulan(String key) {
				try {
					Date d = new SimpleDateFormat("yyyy-MM").parse(key);
					return new SimpleDateFormat("MMM yyyy").format(d);
				} catch (Exception e) {
					return key;
				}
			}

			private void piutangMahasiswa() throws Exception {
				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardPembayaranPerguruanTinggi.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Piutang Per-Prodi"));
				panel.setTooltiptext("Sisa tagihan yang belum dibayar, dikelompokkan menurut program studi.");
				panel.setBorder("none");
				panel.setCollapsible(false);
				panel.setClosable(false);
				panel.setMaximizable(false);
				panel.setMinimizable(false);
				panel.addEventListener("onMove", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						MoveEvent moveEvent = (MoveEvent) arg0;
						String left = moveEvent.getLeft();
						String top = moveEvent.getTop();

					}
				});
				panel.setStyle(
						"margin-bottom:10px;border: 1px;border-style: solid;border-color: #dbdbd9;margin-left: 3px;margin-right: 3px;border-radius: 10px;");

				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				EventListener pengajuanBaruEventListener = new EventListener() {

					private List<Object[]> kegiatans;

					public EventListener getThis() {
						return this;
					}

					@SuppressWarnings({ "unchecked", "deprecation" })
					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataJurusanBanbox ambilDataJurusanBanbox = new AmbilDataJurusanBanbox();
						final Combobox cari = Common.generateTahunAjaran(null);
						final Checkbox chartTampil = new Checkbox("Grafik");
						chartTampil.setChecked(tampilChart);
						final Combobox jenissemester = new Combobox();
						final Combobox searchJenisPembayaran = new Combobox();

						// KOREKSI: Tidak Perlu Session Parameter Sesuai Permintaan
						Common.insertComboDanSemua(searchJenisPembayaran, "namaKegiatan", JenisKegiatan.class,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

						Common.selectComboItem(searchJenisPembayaran, jenisKegiatan);

						jenissemester.setCols(5);
						searchJenisPembayaran.setCols(5);

						Common.selectComboItem(cari, c);
						ambilDataJurusanBanbox.setCols(7);
						ambilDataJurusanBanbox.setValue(sk == null ? "Prodi" : sk.getNama());
						ambilDataJurusanBanbox.setAttribute("jurusan", sk);
						ambilDataJurusanBanbox.setAttribute("myValue", sk);
						ambilDataJurusanBanbox.setReadonly(true);
						ambilDataJurusanBanbox.setParent(toolbar);

						searchJenisPembayaran.setParent(toolbar);

						EventListener cariListener = new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue() != null ? cari.getValue().trim() : "";
								tampilChart = chartTampil.isChecked();
								s = (jenissemester.getSelectedItem() == null
										|| jenissemester.getSelectedItem().getValue() == null) ? null
												: jenissemester.getSelectedItem().getValue() + "";
								sk = (Jurusan) ambilDataJurusanBanbox.getAttribute("jurusan");
								jenisKegiatan = (JenisKegiatan) (searchJenisPembayaran.getSelectedItem() == null ? null
										: searchJenisPembayaran.getSelectedItem().getValue());

								tampilkanLoadingPanel(panelchildren, "Memuat Ulang Data Dashboard",
										"Filter berubah. Sistem sedang mengambil ulang data pembayaran sesuai parameter terbaru.", 8);
								Common.createDefaultTimer(getThis());
							}
						};

						ambilDataJurusanBanbox.setEventListener(cariListener);
						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(6);
						cari.setParent(toolbar);
						jenissemester.setParent(toolbar);
						cari.addEventListener("onChange", cariListener);
						chartTampil.addEventListener("onClick", cariListener);
						jenissemester.addEventListener("onChange", cariListener);
						searchJenisPembayaran.addEventListener("onChange", cariListener);

						MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
						comboitem.setValue(Perkuliahan.GANJIL);
						jenissemester.appendChild(comboitem);

						comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
						comboitem.setValue(Perkuliahan.GENAP);
						jenissemester.appendChild(comboitem);

						comboitem = new MyComboitemConfig("Semua");
						comboitem.setValue(null);
						jenissemester.appendChild(comboitem);
						jenissemester.setSelectedItem(comboitem);
						jenissemester.setReadonly(true);

						Common.selectComboItem(jenissemester, s);

						MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
						refresh.setTooltiptext("Refresh");
						refresh.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								Common.clear(DasboardPembayaranPerguruanTinggi.this);
								DasboardPembayaranPerguruanTinggi.this.init();
							}
						});
						refresh.setParent(toolbar);

						MyFormRow rowUtama = new MyFormRow();
						rowUtama.setParent(rowUtamapalingAwal.getParent());

						final Grid grid = new Grid();
						MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
						toolbarbutton.setParent(toolbar);
						toolbarbutton.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								UIUtil.downloadGrid(grid);
							}
						});

						chartTampil.setParent(toolbar);

						grid.setSclass("dgrid fgrid");
						grid.setParent(rowUtama);
						grid.setStyle("min-height:100px;border:0px;background: transparent;");
						grid.setMold("paging");
						grid.setPageSize(1000);
						grid.getPagingChild().setMold("os");

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig("Prodi");
						column.setWidth("18%");
						columns.appendChild(column);

						column = new MyColumnConfig("Jenis Pembayaran");
						columns.appendChild(column);
						column.setWidth("18%");

						column = new MyColumnConfig("Tagihan");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Dibayar");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Piutang");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("%");
						column.setAlign("right");
						column.setWidth("8%");
						columns.appendChild(column);

						Rows rows = new Rows();
						rows.setParent(grid);

						int size = 0;
						HtmlCategoryModel categoryModel = new HtmlCategoryModel();
						HtmlCategoryModel categoryModelLunas = new HtmlCategoryModel();

						Double totaltagihan = 0.0;
						Double totaldibayar = 0.0;
						Double totalpiutang = 0.0;
						Double totalJumlahTagihan = 0.0;
						Double totalJumlahDibayar = 0.0;
						Double totalJumlahPiutang = 0.0;

						Session sessionData = null;

						try {
							sessionData = HibernateUtil.getSessionFactory().openSession();
							String sql = "sum(case when this_.tagihan>0.1 then 1 else 0 end) as jumlahTagihan,"
									+ "sum(case when this_.dibayar>0.1 then 1 else 0 end) as jumlahDibayar,sum(case when (this_.tagihan-this_.dibayar)>0.1 then 1 else 0 end) as jumlahPiutang";

							kegiatans = sessionData.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true))
									.setProjection(Projections.projectionList()
											.add(Projections.groupProperty("jurusan"))
											.add(Projections.groupProperty("jenisKegiatan"))
											.add(Projections.sum("tagihan")).add(Projections.sum("dibayar"))
											.add(Projections.sqlProjection(sql,
													new String[] { "jumlahTagihan", "jumlahDibayar", "jumlahPiutang" },
													new Type[] { org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE,
															org.hibernate.type.StandardBasicTypes.DOUBLE })))
									.add(s == null ? Restrictions.sqlRestriction("1=1")
											: s.equalsIgnoreCase(Perkuliahan.GENAP)
													? Restrictions.in("semster", Common.genap)
													: Restrictions.in("semster", Common.ganjil))
									.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jurusan", sk))
									.add(jenisKegiatan == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jenisKegiatan", jenisKegiatan))
									.add(c != null && !c.isEmpty() ? Restrictions.eq("tahunAkademik", c)
											: Restrictions.sqlRestriction("1=1"))
									.add(Restrictions.isNotNull("jurusan")).add(Restrictions.isNotNull("jenisKegiatan"))
									.addOrder(Order.asc("jurusan")).addOrder(Order.asc("jenisKegiatan"))
									.setMaxResults(500).list();

							if (kegiatans != null) {
								for (Object[] kegiatan : kegiatans) {
									final Jurusan jurusan = (Jurusan) kegiatan[0];
									final JenisKegiatan jkRow = (JenisKegiatan) kegiatan[1];

									Number tagihan = (Number) (kegiatan[2] == null ? 0.0 : kegiatan[2]);
									Number dibayar = (Number) (kegiatan[3] == null ? 0.0 : kegiatan[3]);
									Double piutang = tagihan.doubleValue() - dibayar.doubleValue();

									Number jumlahTagihan = (Number) (kegiatan[4] == null ? 0.0 : kegiatan[4]);
									Number jumlahDibayar = (Number) (kegiatan[5] == null ? 0.0 : kegiatan[5]);
									Number jumlahPiutang = (Number) (kegiatan[6] == null ? 0.0 : kegiatan[6]);

									if (tagihan.doubleValue() > 0.01) {
										totaltagihan += tagihan.doubleValue();
										totaldibayar += dibayar.doubleValue();
										totalpiutang += piutang.doubleValue();

										totalJumlahTagihan += jumlahTagihan.doubleValue();
										totalJumlahDibayar += jumlahDibayar.doubleValue();
										totalJumlahPiutang += jumlahPiutang.doubleValue();

										size++;
										MyFormRow rowUtamaLagi = new MyFormRow();
										rowUtamaLagi.setParent(rows);
										new MyLabelKecilBold(jurusan.getNama()).setParent(rowUtamaLagi);
										new MyLabelKecil(jkRow.getNama()).setParent(rowUtamaLagi);

										A aTagihan = new A(Common.numberFormat.get().format(tagihan) + "/"
												+ Common.numberFormat.get().format(jumlahTagihan));
										aTagihan.setStyle("font-size:11px;");
										aTagihan.setParent(rowUtamaLagi);
										aTagihan.addEventListener("onClick",
												createPopupListener(jurusan, jkRow, "tagihan", c, s));

										A aDibayar = new A(Common.numberFormat.get().format(dibayar) + "/"
												+ Common.numberFormat.get().format(jumlahDibayar));
										aDibayar.setStyle("font-size:11px;");
										aDibayar.setParent(rowUtamaLagi);
										aDibayar.addEventListener("onClick",
												createPopupListener(jurusan, jkRow, "dibayar", c, s));

										A aPiutang = new A(Common.numberFormat.get().format(piutang) + "/"
												+ Common.numberFormat.get().format(jumlahPiutang));
										aPiutang.setStyle("font-size:11px;");
										aPiutang.setParent(rowUtamaLagi);
										aPiutang.addEventListener("onClick",
												createPopupListener(jurusan, jkRow, "piutang", c, s));

										Double lunasRow = tagihan.doubleValue() > 0
												? (dibayar.doubleValue() * 100.0) / tagihan.doubleValue()
												: 0.0;
										rowUtamaLagi.appendChild(
												new MyLabelAgakKecil(Common.numberFormat.get().format(lunasRow)));

										categoryModel.setValue(jurusan.getNama(), jkRow.getNama(),
												piutang.doubleValue());
										categoryModelLunas.setValue(jurusan.getNama(), jkRow.getNama(), lunasRow);
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/DasboardPembayaranPerguruanTinggi.java:862");
							Common.tampilErrorJikaAdmin(e);
							kegiatans = new ArrayList<Object[]>(); // Prevent NullPointerException
						} finally {
							cleanupSession(sessionData);
						}

						Foot rowUtamaLagi = new Foot();
						rowUtamaLagi.setParent(grid);

						Footer footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecilBold("TOTAL").setParent(footer);

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecil().setParent(footer);

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totaltagihan) + "/"
								+ Common.numberFormat.get().format(totalJumlahTagihan)));

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totaldibayar) + "/"
								+ Common.numberFormat.get().format(totalJumlahDibayar)));

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totalpiutang) + "/"
								+ Common.numberFormat.get().format(totalJumlahPiutang)));

						Double lunasTotal = totaltagihan > 0 ? (totaldibayar * 100.0) / totaltagihan : 0.0;
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(lunasTotal)));

						// PENAMBAHAN TOMBOL DASHBOARD EXCEL DI SAMPING DOWNLOAD GRID
						MyToolbarbuttonConfig excelDashBtn = new MyToolbarbuttonConfig("Dasbor Excel",
								"/img/excel.png");
						excelDashBtn.setParent(toolbar);
						excelDashBtn.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								downloadExcelDashboard(kegiatans, "Rekap Piutang Per-Prodi", "Prodi");
							}
						});

						if (tampilChart) {
							renderChartBar(rowUtamapalingAwal,
									"Piutang Mahasiswa TA " + (c + (s == null ? " Semua Semester" : " Semester " + s)),
									categoryModel, size);
							renderChartBar(rowUtamapalingAwal,
									"Persen Lunas TA " + (c + (s == null ? " Semua Semester" : " Semester " + s)),
									categoryModelLunas, size);
						}
					}
				};

				pengajuanBaruEventListener.onEvent(null);
			}

			private void piutangMahasiswaPerAngkatan() throws Exception {
				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardPembayaranPerguruanTinggi.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Piutang Per-Angkatan"));
				panel.setTooltiptext("Sisa tagihan yang belum dibayar, dikelompokkan menurut tahun angkatan.");
				panel.setBorder("none");
				panel.setCollapsible(false);
				panel.setClosable(false);
				panel.setMaximizable(false);
				panel.setMinimizable(false);
				panel.addEventListener("onMove", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
					}
				});
				panel.setStyle(
						"margin-bottom:10px;border: 1px;border-style: solid;border-color: #dbdbd9;margin-left: 3px;margin-right: 3px;border-radius: 10px;");

				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				EventListener pengajuanBaruEventListener = new EventListener() {

					private List<Object[]> kegiatans;

					public EventListener getThis() {
						return this;
					}

					@SuppressWarnings({ "unchecked", "deprecation" })
					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataJurusanBanbox ambilDataJurusanBanbox = new AmbilDataJurusanBanbox();
						final Combobox cari = Common.generateTahunAjaran(null);
						final Checkbox chartTampil = new Checkbox("Grafik");
						chartTampil.setChecked(tampilChart);
						final Combobox jenissemester = new Combobox();
						final Combobox searchJenisPembayaran = new Combobox();

						Common.insertComboDanSemua(searchJenisPembayaran, "namaKegiatan", JenisKegiatan.class,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

						Common.selectComboItem(searchJenisPembayaran, jenisKegiatan);

						jenissemester.setCols(5);
						searchJenisPembayaran.setCols(5);

						Common.selectComboItem(cari, c);
						ambilDataJurusanBanbox.setCols(7);
						ambilDataJurusanBanbox.setValue(sk == null ? "Prodi" : sk.getNama());
						ambilDataJurusanBanbox.setAttribute("jurusan", sk);
						ambilDataJurusanBanbox.setAttribute("myValue", sk);
						ambilDataJurusanBanbox.setReadonly(true);
						ambilDataJurusanBanbox.setParent(toolbar);

						searchJenisPembayaran.setParent(toolbar);

						EventListener cariListener = new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue() != null ? cari.getValue().trim() : "";
								tampilChart = chartTampil.isChecked();
								s = (jenissemester.getSelectedItem() == null
										|| jenissemester.getSelectedItem().getValue() == null) ? null
												: jenissemester.getSelectedItem().getValue() + "";
								sk = (Jurusan) ambilDataJurusanBanbox.getAttribute("jurusan");
								jenisKegiatan = (JenisKegiatan) (searchJenisPembayaran.getSelectedItem() == null ? null
										: searchJenisPembayaran.getSelectedItem().getValue());

								tampilkanLoadingPanel(panelchildren, "Memuat Ulang Data Dashboard",
										"Filter berubah. Sistem sedang mengambil ulang data pembayaran sesuai parameter terbaru.", 8);
								Common.createDefaultTimer(getThis());
							}
						};

						ambilDataJurusanBanbox.setEventListener(cariListener);
						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(6);
						cari.setParent(toolbar);
						jenissemester.setParent(toolbar);
						cari.addEventListener("onChange", cariListener);
						chartTampil.addEventListener("onClick", cariListener);
						jenissemester.addEventListener("onChange", cariListener);
						searchJenisPembayaran.addEventListener("onChange", cariListener);

						MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
						comboitem.setValue(Perkuliahan.GANJIL);
						jenissemester.appendChild(comboitem);

						comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
						comboitem.setValue(Perkuliahan.GENAP);
						jenissemester.appendChild(comboitem);

						comboitem = new MyComboitemConfig("Semua");
						comboitem.setValue(null);
						jenissemester.appendChild(comboitem);
						jenissemester.setSelectedItem(comboitem);
						jenissemester.setReadonly(true);

						Common.selectComboItem(jenissemester, s);

						MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
						refresh.setTooltiptext("Refresh");
						refresh.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								Common.clear(DasboardPembayaranPerguruanTinggi.this);
								DasboardPembayaranPerguruanTinggi.this.init();
							}
						});
						refresh.setParent(toolbar);

						MyFormRow rowUtama = new MyFormRow();
						rowUtama.setParent(rowUtamapalingAwal.getParent());

						final Grid grid = new Grid();
						MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
						toolbarbutton.setParent(toolbar);
						toolbarbutton.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								UIUtil.downloadGrid(grid);
							}
						});

						chartTampil.setParent(toolbar);

						grid.setSclass("dgrid fgrid");
						grid.setParent(rowUtama);
						grid.setStyle("min-height:100px;border:0px;background: transparent;");
						grid.setMold("paging");
						grid.setPageSize(1000);
						grid.getPagingChild().setMold("os");

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig("Angkatan");
						column.setWidth("10%");
						columns.appendChild(column);

						column = new MyColumnConfig("Jenis Pembayaran");
						columns.appendChild(column);
						column.setWidth("18%");

						column = new MyColumnConfig("Tagihan");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Dibayar");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Piutang");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("%");
						column.setAlign("right");
						column.setWidth("8%");
						columns.appendChild(column);

						Rows rows = new Rows();
						rows.setParent(grid);

						int size = 0;
						HtmlCategoryModel categoryModel = new HtmlCategoryModel();
						HtmlCategoryModel categoryModelLunas = new HtmlCategoryModel();

						Double totaltagihan = 0.0;
						Double totaldibayar = 0.0;
						Double totalpiutang = 0.0;
						Double totalJumlahTagihan = 0.0;
						Double totalJumlahDibayar = 0.0;
						Double totalJumlahPiutang = 0.0;

						Session sessionData = null;
						try {
							sessionData = HibernateUtil.getSessionFactory().openSession();
							String sql = "sum(case when this_.tagihan>0.1 then 1 else 0 end) as jumlahTagihan,"
									+ "sum(case when this_.dibayar>0.1 then 1 else 0 end) as jumlahDibayar,sum(case when (this_.tagihan-this_.dibayar)>0.1 then 1 else 0 end) as jumlahPiutang";

							kegiatans = sessionData.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true))
									.setProjection(Projections.projectionList()
											.add(Projections.groupProperty("tahunAngkatan"))
											.add(Projections.groupProperty("jenisKegiatan"))
											.add(Projections.sum("tagihan")).add(Projections.sum("dibayar"))
											.add(Projections.sqlProjection(sql,
													new String[] { "jumlahTagihan", "jumlahDibayar", "jumlahPiutang" },
													new Type[] { org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE,
															org.hibernate.type.StandardBasicTypes.DOUBLE })))
									.add(s == null ? Restrictions.sqlRestriction("1=1")
											: s.equalsIgnoreCase(Perkuliahan.GENAP)
													? Restrictions.in("semster", Common.genap)
													: Restrictions.in("semster", Common.ganjil))
									.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jurusan", sk))
									.add(jenisKegiatan == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jenisKegiatan", jenisKegiatan))
									.add(c != null && !c.isEmpty() ? Restrictions.eq("tahunAkademik", c)
											: Restrictions.sqlRestriction("1=1"))
									.add(Restrictions.isNotNull("tahunAngkatan"))
									.add(Restrictions.isNotNull("jenisKegiatan")).addOrder(Order.asc("tahunAngkatan"))
									.addOrder(Order.asc("jenisKegiatan")).setMaxResults(500).list();

							if (kegiatans != null) {
								for (Object[] kegiatan : kegiatans) {
									final Number tahunAngkatan = (Number) kegiatan[0];
									final JenisKegiatan jkRow = (JenisKegiatan) kegiatan[1];

									Number tagihan = (Number) (kegiatan[2] == null ? 0.0 : kegiatan[2]);
									Number dibayar = (Number) (kegiatan[3] == null ? 0.0 : kegiatan[3]);
									Double piutang = tagihan.doubleValue() - dibayar.doubleValue();

									Number jumlahTagihan = (Number) (kegiatan[4] == null ? 0.0 : kegiatan[4]);
									Number jumlahDibayar = (Number) (kegiatan[5] == null ? 0.0 : kegiatan[5]);
									Number jumlahPiutang = (Number) (kegiatan[6] == null ? 0.0 : kegiatan[6]);

									if (tagihan.doubleValue() > 0.01) {
										totaltagihan += tagihan.doubleValue();
										totaldibayar += dibayar.doubleValue();
										totalpiutang += piutang.doubleValue();

										totalJumlahTagihan += jumlahTagihan.doubleValue();
										totalJumlahDibayar += jumlahDibayar.doubleValue();
										totalJumlahPiutang += jumlahPiutang.doubleValue();

										size++;
										MyFormRow rowUtamaLagi = new MyFormRow();
										rowUtamaLagi.setParent(rows);
										new MyLabelKecilBold(tahunAngkatan + "").setParent(rowUtamaLagi);
										new MyLabelKecil(jkRow.getNama()).setParent(rowUtamaLagi);

										A aTagihan = new A(Common.numberFormat.get().format(tagihan) + "/"
												+ Common.numberFormat.get().format(jumlahTagihan));
										aTagihan.setStyle("font-size:11px;");
										aTagihan.setParent(rowUtamaLagi);
										aTagihan.addEventListener("onClick",
												createPopupListenerAngkatan(tahunAngkatan, jkRow, "tagihan", c, s, sk));

										A aDibayar = new A(Common.numberFormat.get().format(dibayar) + "/"
												+ Common.numberFormat.get().format(jumlahDibayar));
										aDibayar.setStyle("font-size:11px;");
										aDibayar.setParent(rowUtamaLagi);
										aDibayar.addEventListener("onClick",
												createPopupListenerAngkatan(tahunAngkatan, jkRow, "dibayar", c, s, sk));

										A aPiutang = new A(Common.numberFormat.get().format(piutang) + "/"
												+ Common.numberFormat.get().format(jumlahPiutang));
										aPiutang.setStyle("font-size:11px;");
										aPiutang.setParent(rowUtamaLagi);
										aPiutang.addEventListener("onClick",
												createPopupListenerAngkatan(tahunAngkatan, jkRow, "piutang", c, s, sk));

										Double lunasRow = tagihan.doubleValue() > 0
												? (dibayar.doubleValue() * 100.0) / tagihan.doubleValue()
												: 0.0;
										rowUtamaLagi.appendChild(
												new MyLabelAgakKecil(Common.numberFormat.get().format(lunasRow)));

										categoryModel.setValue(tahunAngkatan + "", jkRow.getNama(),
												piutang.doubleValue());
										categoryModelLunas.setValue(tahunAngkatan + "", jkRow.getNama(), lunasRow);
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/DasboardPembayaranPerguruanTinggi.java:1205");
							Common.tampilErrorJikaAdmin(e);
							kegiatans = new ArrayList<Object[]>();
						} finally {
							cleanupSession(sessionData);
						}

						Foot rowUtamaLagi = new Foot();
						rowUtamaLagi.setParent(grid);

						Footer footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecilBold("TOTAL").setParent(footer);

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecil().setParent(footer);

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totaltagihan) + "/"
								+ Common.numberFormat.get().format(totalJumlahTagihan)));

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totaldibayar) + "/"
								+ Common.numberFormat.get().format(totalJumlahDibayar)));

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totalpiutang) + "/"
								+ Common.numberFormat.get().format(totalJumlahPiutang)));

						Double lunasTotal = totaltagihan > 0 ? (totaldibayar * 100.0) / totaltagihan : 0.0;
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(lunasTotal)));

						// PENAMBAHAN TOMBOL DASHBOARD EXCEL
						MyToolbarbuttonConfig excelDashBtn = new MyToolbarbuttonConfig("Dasbor Excel",
								"/img/excel.png");
						excelDashBtn.setParent(toolbar);
						excelDashBtn.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								downloadExcelDashboard(kegiatans, "Rekap Piutang Per-Angkatan", "Angkatan");
							}
						});

						if (tampilChart) {
							renderChartBar(rowUtamapalingAwal,
									"Piutang Mahasiswa TA " + (c + (s == null ? " Semua Semester" : " Semester " + s)),
									categoryModel, size);
							renderChartBar(rowUtamapalingAwal,
									"Persen Lunas TA " + (c + (s == null ? " Semua Semester" : " Semester " + s)),
									categoryModelLunas, size);
						}
					}
				};

				pengajuanBaruEventListener.onEvent(null);
			}

			private void piutangMahasiswaStatusAwal() throws Exception {
				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardPembayaranPerguruanTinggi.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Piutang Per-Status"));
				panel.setTooltiptext("Sisa tagihan yang belum dibayar, dikelompokkan menurut status awal mahasiswa.");
				panel.setBorder("none");
				panel.setCollapsible(false);
				panel.setClosable(false);
				panel.setMaximizable(false);
				panel.setMinimizable(false);
				panel.addEventListener("onMove", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
					}
				});
				panel.setStyle(
						"margin-bottom:10px;border: 1px;border-style: solid;border-color: #dbdbd9;margin-left: 3px;margin-right: 3px;border-radius: 10px;");

				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				EventListener pengajuanBaruEventListener = new EventListener() {

					private List<Object[]> kegiatans;

					public EventListener getThis() {
						return this;
					}

					@SuppressWarnings({ "unchecked", "deprecation" })
					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataJurusanBanbox ambilDataJurusanBanbox = new AmbilDataJurusanBanbox();
						final Combobox cari = Common.generateTahunAjaran(null);
						final Checkbox chartTampil = new Checkbox("Grafik");
						chartTampil.setChecked(tampilChart);
						final Combobox jenissemester = new Combobox();
						final Combobox searchJenisPembayaran = new Combobox();

						Common.insertComboDanSemua(searchJenisPembayaran, "namaKegiatan", JenisKegiatan.class,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

						Common.selectComboItem(searchJenisPembayaran, jenisKegiatan);

						jenissemester.setCols(5);
						searchJenisPembayaran.setCols(5);

						Common.selectComboItem(cari, c);
						ambilDataJurusanBanbox.setCols(7);
						ambilDataJurusanBanbox.setValue(sk == null ? "Prodi" : sk.getNama());
						ambilDataJurusanBanbox.setAttribute("jurusan", sk);
						ambilDataJurusanBanbox.setAttribute("myValue", sk);
						ambilDataJurusanBanbox.setReadonly(true);
						ambilDataJurusanBanbox.setParent(toolbar);

						searchJenisPembayaran.setParent(toolbar);

						EventListener cariListener = new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue() != null ? cari.getValue().trim() : "";
								tampilChart = chartTampil.isChecked();
								s = (jenissemester.getSelectedItem() == null
										|| jenissemester.getSelectedItem().getValue() == null) ? null
												: jenissemester.getSelectedItem().getValue() + "";
								sk = (Jurusan) ambilDataJurusanBanbox.getAttribute("jurusan");
								jenisKegiatan = (JenisKegiatan) (searchJenisPembayaran.getSelectedItem() == null ? null
										: searchJenisPembayaran.getSelectedItem().getValue());

								tampilkanLoadingPanel(panelchildren, "Memuat Ulang Data Dashboard",
										"Filter berubah. Sistem sedang mengambil ulang data pembayaran sesuai parameter terbaru.", 8);
								Common.createDefaultTimer(getThis());
							}
						};

						ambilDataJurusanBanbox.setEventListener(cariListener);
						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(6);
						cari.setParent(toolbar);
						jenissemester.setParent(toolbar);
						cari.addEventListener("onChange", cariListener);
						chartTampil.addEventListener("onClick", cariListener);
						jenissemester.addEventListener("onChange", cariListener);
						searchJenisPembayaran.addEventListener("onChange", cariListener);

						MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
						comboitem.setValue(Perkuliahan.GANJIL);
						jenissemester.appendChild(comboitem);

						comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
						comboitem.setValue(Perkuliahan.GENAP);
						jenissemester.appendChild(comboitem);

						comboitem = new MyComboitemConfig("Semua");
						comboitem.setValue(null);
						jenissemester.appendChild(comboitem);
						jenissemester.setSelectedItem(comboitem);
						jenissemester.setReadonly(true);

						Common.selectComboItem(jenissemester, s);

						MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
						refresh.setTooltiptext("Refresh");
						refresh.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								Common.clear(DasboardPembayaranPerguruanTinggi.this);
								DasboardPembayaranPerguruanTinggi.this.init();
							}
						});
						refresh.setParent(toolbar);

						MyFormRow rowUtama = new MyFormRow();
						rowUtama.setParent(rowUtamapalingAwal.getParent());

						final Grid grid = new Grid();
						MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
						toolbarbutton.setParent(toolbar);
						toolbarbutton.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								UIUtil.downloadGrid(grid);
							}
						});

						chartTampil.setParent(toolbar);

						grid.setSclass("dgrid fgrid");
						grid.setParent(rowUtama);
						grid.setStyle("min-height:100px;border:0px;background: transparent;");
						grid.setMold("paging");
						grid.setPageSize(1000);
						grid.getPagingChild().setMold("os");

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig("Status");
						column.setWidth("10%");
						columns.appendChild(column);

						column = new MyColumnConfig("Jenis Pembayaran");
						columns.appendChild(column);
						column.setWidth("18%");

						column = new MyColumnConfig("Tagihan");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Dibayar");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Piutang");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("%");
						column.setAlign("right");
						column.setWidth("8%");
						columns.appendChild(column);

						Rows rows = new Rows();
						rows.setParent(grid);

						int size = 0;
						HtmlCategoryModel categoryModel = new HtmlCategoryModel();
						HtmlCategoryModel categoryModelLunas = new HtmlCategoryModel();

						Double totaltagihan = 0.0;
						Double totaldibayar = 0.0;
						Double totalpiutang = 0.0;
						Double totalJumlahTagihan = 0.0;
						Double totalJumlahDibayar = 0.0;
						Double totalJumlahPiutang = 0.0;

						Session sessionData = null;
						try {
							sessionData = HibernateUtil.getSessionFactory().openSession();
							String sql = "sum(case when this_.tagihan>0.1 then 1 else 0 end) as jumlahTagihan,"
									+ "sum(case when this_.dibayar>0.1 then 1 else 0 end) as jumlahDibayar,sum(case when (this_.tagihan-this_.dibayar)>0.1 then 1 else 0 end) as jumlahPiutang";

							kegiatans = sessionData.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true))
									.setProjection(Projections.projectionList()
											.add(Projections.groupProperty("statusAwalMahasiswa"))
											.add(Projections.groupProperty("jenisKegiatan"))
											.add(Projections.sum("tagihan")).add(Projections.sum("dibayar"))
											.add(Projections.sqlProjection(sql,
													new String[] { "jumlahTagihan", "jumlahDibayar", "jumlahPiutang" },
													new Type[] { org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE,
															org.hibernate.type.StandardBasicTypes.DOUBLE })))
									.add(s == null ? Restrictions.sqlRestriction("1=1")
											: s.equalsIgnoreCase(Perkuliahan.GENAP)
													? Restrictions.in("semster", Common.genap)
													: Restrictions.in("semster", Common.ganjil))
									.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jurusan", sk))
									.add(jenisKegiatan == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jenisKegiatan", jenisKegiatan))
									.add(c != null && !c.isEmpty() ? Restrictions.eq("tahunAkademik", c)
											: Restrictions.sqlRestriction("1=1"))
									.add(Restrictions.isNotNull("statusAwalMahasiswa"))
									.add(Restrictions.isNotNull("jenisKegiatan"))
									.addOrder(Order.asc("statusAwalMahasiswa")).addOrder(Order.asc("jenisKegiatan"))
									.setMaxResults(500).list();

							if (kegiatans != null) {
								for (Object[] kegiatan : kegiatans) {
									final StatusAwalMahasiswa statusRow = (StatusAwalMahasiswa) kegiatan[0];
									final JenisKegiatan jkRow = (JenisKegiatan) kegiatan[1];

									Number tagihan = (Number) (kegiatan[2] == null ? 0.0 : kegiatan[2]);
									Number dibayar = (Number) (kegiatan[3] == null ? 0.0 : kegiatan[3]);
									Double piutang = tagihan.doubleValue() - dibayar.doubleValue();

									Number jumlahTagihan = (Number) (kegiatan[4] == null ? 0.0 : kegiatan[4]);
									Number jumlahDibayar = (Number) (kegiatan[5] == null ? 0.0 : kegiatan[5]);
									Number jumlahPiutang = (Number) (kegiatan[6] == null ? 0.0 : kegiatan[6]);

									if (tagihan.doubleValue() > 0.01) {
										totaltagihan += tagihan.doubleValue();
										totaldibayar += dibayar.doubleValue();
										totalpiutang += piutang.doubleValue();

										totalJumlahTagihan += jumlahTagihan.doubleValue();
										totalJumlahDibayar += jumlahDibayar.doubleValue();
										totalJumlahPiutang += jumlahPiutang.doubleValue();

										size++;
										MyFormRow rowUtamaLagi = new MyFormRow();
										rowUtamaLagi.setParent(rows);
										new MyLabelKecilBold(statusRow.getNama()).setParent(rowUtamaLagi);
										new MyLabelKecil(jkRow.getNama()).setParent(rowUtamaLagi);

										A aTagihan = new A(Common.numberFormat.get().format(tagihan) + "/"
												+ Common.numberFormat.get().format(jumlahTagihan));
										aTagihan.setStyle("font-size:11px;");
										aTagihan.setParent(rowUtamaLagi);
										aTagihan.addEventListener("onClick",
												createPopupListenerStatus(statusRow, jkRow, "tagihan", c, s, sk));

										A aDibayar = new A(Common.numberFormat.get().format(dibayar) + "/"
												+ Common.numberFormat.get().format(jumlahDibayar));
										aDibayar.setStyle("font-size:11px;");
										aDibayar.setParent(rowUtamaLagi);
										aDibayar.addEventListener("onClick",
												createPopupListenerStatus(statusRow, jkRow, "dibayar", c, s, sk));

										A aPiutang = new A(Common.numberFormat.get().format(piutang) + "/"
												+ Common.numberFormat.get().format(jumlahPiutang));
										aPiutang.setStyle("font-size:11px;");
										aPiutang.setParent(rowUtamaLagi);
										aPiutang.addEventListener("onClick",
												createPopupListenerStatus(statusRow, jkRow, "piutang", c, s, sk));

										Double lunasRow = tagihan.doubleValue() > 0
												? (dibayar.doubleValue() * 100.0) / tagihan.doubleValue()
												: 0.0;
										rowUtamaLagi.appendChild(
												new MyLabelAgakKecil(Common.numberFormat.get().format(lunasRow)));

										categoryModel.setValue(statusRow.getNama(), jkRow.getNama(),
												piutang.doubleValue());
										categoryModelLunas.setValue(statusRow.getNama(), jkRow.getNama(), lunasRow);
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/DasboardPembayaranPerguruanTinggi.java:1549");
							Common.tampilErrorJikaAdmin(e);
							kegiatans = new ArrayList<Object[]>();
						} finally {
							cleanupSession(sessionData);
						}

						Foot rowUtamaLagi = new Foot();
						rowUtamaLagi.setParent(grid);

						Footer footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecilBold("TOTAL").setParent(footer);

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecil().setParent(footer);

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totaltagihan) + "/"
								+ Common.numberFormat.get().format(totalJumlahTagihan)));

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totaldibayar) + "/"
								+ Common.numberFormat.get().format(totalJumlahDibayar)));

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totalpiutang) + "/"
								+ Common.numberFormat.get().format(totalJumlahPiutang)));

						Double lunasTotal = totaltagihan > 0 ? (totaldibayar * 100.0) / totaltagihan : 0.0;
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(lunasTotal)));

						// PENAMBAHAN TOMBOL DASHBOARD EXCEL
						MyToolbarbuttonConfig excelDashBtn = new MyToolbarbuttonConfig("Dasbor Excel",
								"/img/excel.png");
						excelDashBtn.setParent(toolbar);
						excelDashBtn.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								downloadExcelDashboard(kegiatans, "Rekap Piutang Per-Status", "Status");
							}
						});

						if (tampilChart) {
							renderChartBar(rowUtamapalingAwal,
									"Piutang Mahasiswa TA " + (c + (s == null ? " Semua Semester" : " Semester " + s)),
									categoryModel, size);
							renderChartBar(rowUtamapalingAwal,
									"Persen Lunas TA " + (c + (s == null ? " Semua Semester" : " Semester " + s)),
									categoryModelLunas, size);
						}
					}
				};

				pengajuanBaruEventListener.onEvent(null);
			}

			private void piutangMahasiswaProgram() throws Exception {
				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardPembayaranPerguruanTinggi.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Piutang Per-Program"));
				panel.setTooltiptext("Sisa tagihan yang belum dibayar, dikelompokkan menurut program mahasiswa.");
				panel.setBorder("none");
				panel.setCollapsible(false);
				panel.setClosable(false);
				panel.setMaximizable(false);
				panel.setMinimizable(false);
				panel.addEventListener("onMove", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
					}
				});
				panel.setStyle(
						"margin-bottom:10px;border: 1px;border-style: solid;border-color: #dbdbd9;margin-left: 3px;margin-right: 3px;border-radius: 10px;");

				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				EventListener pengajuanBaruEventListener = new EventListener() {

					private List<Object[]> kegiatans;

					public EventListener getThis() {
						return this;
					}

					@SuppressWarnings({ "unchecked", "deprecation" })
					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataJurusanBanbox ambilDataJurusanBanbox = new AmbilDataJurusanBanbox();
						final Combobox cari = Common.generateTahunAjaran(null);
						final Checkbox chartTampil = new Checkbox("Grafik");
						chartTampil.setChecked(tampilChart);
						final Combobox jenissemester = new Combobox();
						final Combobox searchJenisPembayaran = new Combobox();

						Common.insertComboDanSemua(searchJenisPembayaran, "namaKegiatan", JenisKegiatan.class,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

						Common.selectComboItem(searchJenisPembayaran, jenisKegiatan);

						jenissemester.setCols(5);
						searchJenisPembayaran.setCols(5);

						Common.selectComboItem(cari, c);
						ambilDataJurusanBanbox.setCols(7);
						ambilDataJurusanBanbox.setValue(sk == null ? "Prodi" : sk.getNama());
						ambilDataJurusanBanbox.setAttribute("jurusan", sk);
						ambilDataJurusanBanbox.setAttribute("myValue", sk);
						ambilDataJurusanBanbox.setReadonly(true);
						ambilDataJurusanBanbox.setParent(toolbar);

						searchJenisPembayaran.setParent(toolbar);

						EventListener cariListener = new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue() != null ? cari.getValue().trim() : "";
								tampilChart = chartTampil.isChecked();
								s = (jenissemester.getSelectedItem() == null
										|| jenissemester.getSelectedItem().getValue() == null) ? null
												: jenissemester.getSelectedItem().getValue() + "";
								sk = (Jurusan) ambilDataJurusanBanbox.getAttribute("jurusan");
								jenisKegiatan = (JenisKegiatan) (searchJenisPembayaran.getSelectedItem() == null ? null
										: searchJenisPembayaran.getSelectedItem().getValue());

								tampilkanLoadingPanel(panelchildren, "Memuat Ulang Data Dashboard",
										"Filter berubah. Sistem sedang mengambil ulang data pembayaran sesuai parameter terbaru.", 8);
								Common.createDefaultTimer(getThis());
							}
						};

						ambilDataJurusanBanbox.setEventListener(cariListener);
						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(6);
						cari.setParent(toolbar);
						jenissemester.setParent(toolbar);
						cari.addEventListener("onChange", cariListener);
						chartTampil.addEventListener("onClick", cariListener);
						jenissemester.addEventListener("onChange", cariListener);
						searchJenisPembayaran.addEventListener("onChange", cariListener);

						MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
						comboitem.setValue(Perkuliahan.GANJIL);
						jenissemester.appendChild(comboitem);

						comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
						comboitem.setValue(Perkuliahan.GENAP);
						jenissemester.appendChild(comboitem);

						comboitem = new MyComboitemConfig("Semua");
						comboitem.setValue(null);
						jenissemester.appendChild(comboitem);
						jenissemester.setSelectedItem(comboitem);
						jenissemester.setReadonly(true);

						Common.selectComboItem(jenissemester, s);

						MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
						refresh.setTooltiptext("Refresh");
						refresh.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								Common.clear(DasboardPembayaranPerguruanTinggi.this);
								DasboardPembayaranPerguruanTinggi.this.init();
							}
						});
						refresh.setParent(toolbar);

						MyFormRow rowUtama = new MyFormRow();
						rowUtama.setParent(rowUtamapalingAwal.getParent());

						final Grid grid = new Grid();
						MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
						toolbarbutton.setParent(toolbar);
						toolbarbutton.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								UIUtil.downloadGrid(grid);
							}
						});

						chartTampil.setParent(toolbar);

						grid.setSclass("dgrid fgrid");
						grid.setParent(rowUtama);
						grid.setStyle("min-height:100px;border:0px;background: transparent;");
						grid.setMold("paging");
						grid.setPageSize(1000);
						grid.getPagingChild().setMold("os");

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig("Program");
						column.setWidth("10%");
						columns.appendChild(column);

						column = new MyColumnConfig("Jenis Pembayaran");
						columns.appendChild(column);
						column.setWidth("18%");

						column = new MyColumnConfig("Tagihan");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Dibayar");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Piutang");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("%");
						column.setAlign("right");
						column.setWidth("8%");
						columns.appendChild(column);

						Rows rows = new Rows();
						rows.setParent(grid);

						int size = 0;
						HtmlCategoryModel categoryModel = new HtmlCategoryModel();
						HtmlCategoryModel categoryModelLunas = new HtmlCategoryModel();

						Double totaltagihan = 0.0;
						Double totaldibayar = 0.0;
						Double totalpiutang = 0.0;
						Double totalJumlahTagihan = 0.0;
						Double totalJumlahDibayar = 0.0;
						Double totalJumlahPiutang = 0.0;

						Session sessionData = null;

						try {
							sessionData = HibernateUtil.getSessionFactory().openSession();
							String sql = "sum(case when this_.tagihan>0.1 then 1 else 0 end) as jumlahTagihan,"
									+ "sum(case when this_.dibayar>0.1 then 1 else 0 end) as jumlahDibayar,sum(case when (this_.tagihan-this_.dibayar)>0.1 then 1 else 0 end) as jumlahPiutang";

							kegiatans = sessionData.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true))
									.setProjection(Projections.projectionList()
											.add(Projections.groupProperty("program"))
											.add(Projections.groupProperty("jenisKegiatan"))
											.add(Projections.sum("tagihan")).add(Projections.sum("dibayar"))
											.add(Projections.sqlProjection(sql,
													new String[] { "jumlahTagihan", "jumlahDibayar", "jumlahPiutang" },
													new Type[] { org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE,
															org.hibernate.type.StandardBasicTypes.DOUBLE })))
									.add(s == null ? Restrictions.sqlRestriction("1=1")
											: s.equalsIgnoreCase(Perkuliahan.GENAP)
													? Restrictions.in("semster", Common.genap)
													: Restrictions.in("semster", Common.ganjil))
									.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jurusan", sk))
									.add(jenisKegiatan == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jenisKegiatan", jenisKegiatan))
									.add(c != null && !c.isEmpty() ? Restrictions.eq("tahunAkademik", c)
											: Restrictions.sqlRestriction("1=1"))
									.add(Restrictions.isNotNull("program")).add(Restrictions.isNotNull("jenisKegiatan"))
									.addOrder(Order.asc("program")).addOrder(Order.asc("jenisKegiatan"))
									.setMaxResults(500).list();

							if (kegiatans != null) {
								for (Object[] kegiatan : kegiatans) {
									final String programRow = (String) kegiatan[0];
									final JenisKegiatan jkRow = (JenisKegiatan) kegiatan[1];

									Number tagihan = (Number) (kegiatan[2] == null ? 0.0 : kegiatan[2]);
									Number dibayar = (Number) (kegiatan[3] == null ? 0.0 : kegiatan[3]);
									Double piutang = tagihan.doubleValue() - dibayar.doubleValue();

									Number jumlahTagihan = (Number) (kegiatan[4] == null ? 0.0 : kegiatan[4]);
									Number jumlahDibayar = (Number) (kegiatan[5] == null ? 0.0 : kegiatan[5]);
									Number jumlahPiutang = (Number) (kegiatan[6] == null ? 0.0 : kegiatan[6]);

									if (tagihan.doubleValue() > 0.01) {
										totaltagihan += tagihan.doubleValue();
										totaldibayar += dibayar.doubleValue();
										totalpiutang += piutang.doubleValue();

										totalJumlahTagihan += jumlahTagihan.doubleValue();
										totalJumlahDibayar += jumlahDibayar.doubleValue();
										totalJumlahPiutang += jumlahPiutang.doubleValue();

										size++;
										MyFormRow rowUtamaLagi = new MyFormRow();
										rowUtamaLagi.setParent(rows);
										new MyLabelKecilBold(programRow).setParent(rowUtamaLagi);
										new MyLabelKecil(jkRow.getNama()).setParent(rowUtamaLagi);

										A aTagihan = new A(Common.numberFormat.get().format(tagihan) + "/"
												+ Common.numberFormat.get().format(jumlahTagihan));
										aTagihan.setStyle("font-size:11px;");
										aTagihan.setParent(rowUtamaLagi);
										aTagihan.addEventListener("onClick",
												createPopupListenerProgram(programRow, jkRow, "tagihan", c, s, sk));

										A aDibayar = new A(Common.numberFormat.get().format(dibayar) + "/"
												+ Common.numberFormat.get().format(jumlahDibayar));
										aDibayar.setStyle("font-size:11px;");
										aDibayar.setParent(rowUtamaLagi);
										aDibayar.addEventListener("onClick",
												createPopupListenerProgram(programRow, jkRow, "dibayar", c, s, sk));

										A aPiutang = new A(Common.numberFormat.get().format(piutang) + "/"
												+ Common.numberFormat.get().format(jumlahPiutang));
										aPiutang.setStyle("font-size:11px;");
										aPiutang.setParent(rowUtamaLagi);
										aPiutang.addEventListener("onClick",
												createPopupListenerProgram(programRow, jkRow, "piutang", c, s, sk));

										Double lunasRow = tagihan.doubleValue() > 0
												? (dibayar.doubleValue() * 100.0) / tagihan.doubleValue()
												: 0.0;
										rowUtamaLagi.appendChild(
												new MyLabelAgakKecil(Common.numberFormat.get().format(lunasRow)));

										categoryModel.setValue(programRow, jkRow.getNama(), piutang.doubleValue());
										categoryModelLunas.setValue(programRow, jkRow.getNama(), lunasRow);
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/DasboardPembayaranPerguruanTinggi.java:1892");
							Common.tampilErrorJikaAdmin(e);
							kegiatans = new ArrayList<Object[]>();
						} finally {
							cleanupSession(sessionData);
						}

						Foot rowUtamaLagi = new Foot();
						rowUtamaLagi.setParent(grid);

						Footer footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecilBold("TOTAL").setParent(footer);

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecil().setParent(footer);

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totaltagihan) + "/"
								+ Common.numberFormat.get().format(totalJumlahTagihan)));

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totaldibayar) + "/"
								+ Common.numberFormat.get().format(totalJumlahDibayar)));

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totalpiutang) + "/"
								+ Common.numberFormat.get().format(totalJumlahPiutang)));

						Double lunasTotal = totaltagihan > 0 ? (totaldibayar * 100.0) / totaltagihan : 0.0;
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(lunasTotal)));

						// PENAMBAHAN TOMBOL DASHBOARD EXCEL
						MyToolbarbuttonConfig excelDashBtn = new MyToolbarbuttonConfig("Dasbor Excel",
								"/img/excel.png");
						excelDashBtn.setParent(toolbar);
						excelDashBtn.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								downloadExcelDashboard(kegiatans, "Rekap Piutang Per-Program", "Program");
							}
						});

						if (tampilChart) {
							renderChartBar(rowUtamapalingAwal,
									"Piutang Mahasiswa TA " + (c + (s == null ? " Semua Semester" : " Semester " + s)),
									categoryModel, size);
							renderChartBar(rowUtamapalingAwal,
									"Persen Lunas TA " + (c + (s == null ? " Semua Semester" : " Semester " + s)),
									categoryModelLunas, size);
						}
					}
				};

				pengajuanBaruEventListener.onEvent(null);
			}

			private void piutangMahasiswaPerValidator() throws Exception {
				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardPembayaranPerguruanTinggi.this);
				portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(Common.getBahasaConfig("Piutang Per-Validator"));
				panel.setTooltiptext("Pembayaran yang sudah masuk, dikelompokkan menurut petugas yang memvalidasi.");
				panel.setBorder("none");
				panel.setCollapsible(false);
				panel.setClosable(false);
				panel.setMaximizable(false);
				panel.setMinimizable(false);
				panel.addEventListener("onMove", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
					}
				});
				panel.setStyle(
						"margin-bottom:10px;border: 1px;border-style: solid;border-color: #dbdbd9;margin-left: 3px;margin-right: 3px;border-radius: 10px;");

				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				EventListener pengajuanBaruEventListener = new EventListener() {

					private List<Object[]> kegiatans;

					public EventListener getThis() {
						return this;
					}

					@SuppressWarnings({ "unchecked", "deprecation" })
					@Override
					public void onEvent(Event arg0) throws Exception {

						Common.clear(panelchildren);

						Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
						rowUtamapalingAwal.getGrid().setSclass("dgrid");

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(rowUtamapalingAwal);

						final AmbilDataJurusanBanbox ambilDataJurusanBanbox = new AmbilDataJurusanBanbox();
						final Combobox cari = Common.generateTahunAjaran(null);
						final Checkbox chartTampil = new Checkbox("Grafik");
						chartTampil.setChecked(tampilChart);
						final Combobox jenissemester = new Combobox();
						final Combobox searchJenisPembayaran = new Combobox();

						Common.insertComboDanSemua(searchJenisPembayaran, "namaKegiatan", JenisKegiatan.class,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

						Common.selectComboItem(searchJenisPembayaran, jenisKegiatan);

						jenissemester.setCols(5);
						searchJenisPembayaran.setCols(5);

						Common.selectComboItem(cari, c);
						ambilDataJurusanBanbox.setCols(7);
						ambilDataJurusanBanbox.setValue(sk == null ? "Prodi" : sk.getNama());
						ambilDataJurusanBanbox.setAttribute("jurusan", sk);
						ambilDataJurusanBanbox.setAttribute("myValue", sk);
						ambilDataJurusanBanbox.setReadonly(true);
						ambilDataJurusanBanbox.setParent(toolbar);

						searchJenisPembayaran.setParent(toolbar);

						EventListener cariListener = new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								c = cari.getValue() != null ? cari.getValue().trim() : "";
								tampilChart = chartTampil.isChecked();
								s = (jenissemester.getSelectedItem() == null
										|| jenissemester.getSelectedItem().getValue() == null) ? null
												: jenissemester.getSelectedItem().getValue() + "";
								sk = (Jurusan) ambilDataJurusanBanbox.getAttribute("jurusan");
								jenisKegiatan = (JenisKegiatan) (searchJenisPembayaran.getSelectedItem() == null ? null
										: searchJenisPembayaran.getSelectedItem().getValue());

								tampilkanLoadingPanel(panelchildren, "Memuat Ulang Data Dashboard",
										"Filter berubah. Sistem sedang mengambil ulang data pembayaran sesuai parameter terbaru.", 8);
								Common.createDefaultTimer(getThis());
							}
						};

						ambilDataJurusanBanbox.setEventListener(cariListener);
						new MyLabelAgakKecil("Cari:").setParent(toolbar);
						cari.setCols(6);
						cari.setParent(toolbar);
						jenissemester.setParent(toolbar);
						cari.addEventListener("onChange", cariListener);
						chartTampil.addEventListener("onClick", cariListener);
						jenissemester.addEventListener("onChange", cariListener);
						searchJenisPembayaran.addEventListener("onChange", cariListener);

						MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
						comboitem.setValue(Perkuliahan.GANJIL);
						jenissemester.appendChild(comboitem);

						comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
						comboitem.setValue(Perkuliahan.GENAP);
						jenissemester.appendChild(comboitem);

						comboitem = new MyComboitemConfig("Semua");
						comboitem.setValue(null);
						jenissemester.appendChild(comboitem);
						jenissemester.setSelectedItem(comboitem);
						jenissemester.setReadonly(true);

						Common.selectComboItem(jenissemester, s);

						MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
						refresh.setTooltiptext("Refresh");
						refresh.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								Common.clear(DasboardPembayaranPerguruanTinggi.this);
								DasboardPembayaranPerguruanTinggi.this.init();
							}
						});
						refresh.setParent(toolbar);

						MyFormRow rowUtama = new MyFormRow();
						rowUtama.setParent(rowUtamapalingAwal.getParent());

						final Grid grid = new Grid();
						MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
						toolbarbutton.setParent(toolbar);
						toolbarbutton.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								UIUtil.downloadGrid(grid);
							}
						});

						chartTampil.setParent(toolbar);

						grid.setSclass("dgrid fgrid");
						grid.setParent(rowUtama);
						grid.setStyle("min-height:100px;border:0px;background: transparent;");
						grid.setMold("paging");
						grid.setPageSize(1000);
						grid.getPagingChild().setMold("os");

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig("Validator");
						column.setWidth("10%");
						columns.appendChild(column);

						column = new MyColumnConfig("Jenis Pembayaran");
						columns.appendChild(column);
						column.setWidth("18%");

						column = new MyColumnConfig("Tagihan");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Dibayar");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("Piutang");
						column.setAlign("right");
						columns.appendChild(column);

						column = new MyColumnConfig("%");
						column.setAlign("right");
						column.setWidth("8%");
						columns.appendChild(column);

						Rows rows = new Rows();
						rows.setParent(grid);

						int size = 0;
						HtmlCategoryModel categoryModel = new HtmlCategoryModel();
						HtmlCategoryModel categoryModelLunas = new HtmlCategoryModel();

						Double totaltagihan = 0.0;
						Double totaldibayar = 0.0;
						Double totalpiutang = 0.0;
						Double totalJumlahTagihan = 0.0;
						Double totalJumlahDibayar = 0.0;
						Double totalJumlahPiutang = 0.0;

						Session sessionData = null;
						try {
							sessionData = HibernateUtil.getSessionFactory().openSession();
							String sql = "sum(case when this_.tagihan>0.1 then 1 else 0 end) as jumlahTagihan,"
									+ "sum(case when this_.dibayar>0.1 then 1 else 0 end) as jumlahDibayar,sum(case when (this_.tagihan-this_.dibayar)>0.1 then 1 else 0 end) as jumlahPiutang";

							kegiatans = sessionData.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true))
									.setProjection(Projections.projectionList()
											.add(Projections.groupProperty("validator"))
											.add(Projections.groupProperty("jenisKegiatan"))
											.add(Projections.sum("tagihan")).add(Projections.sum("dibayar"))
											.add(Projections.sqlProjection(sql,
													new String[] { "jumlahTagihan", "jumlahDibayar", "jumlahPiutang" },
													new Type[] { org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE,
															org.hibernate.type.StandardBasicTypes.DOUBLE })))
									.add(s == null ? Restrictions.sqlRestriction("1=1")
											: s.equalsIgnoreCase(Perkuliahan.GENAP)
													? Restrictions.in("semster", Common.genap)
													: Restrictions.in("semster", Common.ganjil))
									.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jurusan", sk))
									.add(jenisKegiatan == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jenisKegiatan", jenisKegiatan))
									.add(c != null && !c.isEmpty() ? Restrictions.eq("tahunAkademik", c)
											: Restrictions.sqlRestriction("1=1"))
									.add(Restrictions.isNotNull("validator"))
									.add(Restrictions.isNotNull("jenisKegiatan")).addOrder(Order.asc("validator"))
									.addOrder(Order.asc("jenisKegiatan")).setMaxResults(500).list();

							if (kegiatans != null) {
								for (Object[] kegiatan : kegiatans) {
									final String validatorRow = (String) kegiatan[0];
									final JenisKegiatan jkRow = (JenisKegiatan) kegiatan[1];

									Number tagihan = (Number) (kegiatan[2] == null ? 0.0 : kegiatan[2]);
									Number dibayar = (Number) (kegiatan[3] == null ? 0.0 : kegiatan[3]);
									Double piutang = tagihan.doubleValue() - dibayar.doubleValue();

									Number jumlahTagihan = (Number) (kegiatan[4] == null ? 0.0 : kegiatan[4]);
									Number jumlahDibayar = (Number) (kegiatan[5] == null ? 0.0 : kegiatan[5]);
									Number jumlahPiutang = (Number) (kegiatan[6] == null ? 0.0 : kegiatan[6]);

									if (tagihan.doubleValue() > 0.01) {
										totaltagihan += tagihan.doubleValue();
										totaldibayar += dibayar.doubleValue();
										totalpiutang += piutang.doubleValue();

										totalJumlahTagihan += jumlahTagihan.doubleValue();
										totalJumlahDibayar += jumlahDibayar.doubleValue();
										totalJumlahPiutang += jumlahPiutang.doubleValue();

										size++;
										MyFormRow rowUtamaLagi = new MyFormRow();
										rowUtamaLagi.setParent(rows);
										new MyLabelKecilBold(validatorRow).setParent(rowUtamaLagi);
										new MyLabelKecil(jkRow.getNama()).setParent(rowUtamaLagi);

										A aTagihan = new A(Common.numberFormat.get().format(tagihan) + "/"
												+ Common.numberFormat.get().format(jumlahTagihan));
										aTagihan.setStyle("font-size:11px;");
										aTagihan.setParent(rowUtamaLagi);
										aTagihan.addEventListener("onClick",
												createPopupListenerValidator(validatorRow, jkRow, "tagihan", c, s, sk));

										A aDibayar = new A(Common.numberFormat.get().format(dibayar) + "/"
												+ Common.numberFormat.get().format(jumlahDibayar));
										aDibayar.setStyle("font-size:11px;");
										aDibayar.setParent(rowUtamaLagi);
										aDibayar.addEventListener("onClick",
												createPopupListenerValidator(validatorRow, jkRow, "dibayar", c, s, sk));

										A aPiutang = new A(Common.numberFormat.get().format(piutang) + "/"
												+ Common.numberFormat.get().format(jumlahPiutang));
										aPiutang.setStyle("font-size:11px;");
										aPiutang.setParent(rowUtamaLagi);
										aPiutang.addEventListener("onClick",
												createPopupListenerValidator(validatorRow, jkRow, "piutang", c, s, sk));

										Double lunasRow = tagihan.doubleValue() > 0
												? (dibayar.doubleValue() * 100.0) / tagihan.doubleValue()
												: 0.0;
										rowUtamaLagi.appendChild(
												new MyLabelAgakKecil(Common.numberFormat.get().format(lunasRow)));

										categoryModel.setValue(validatorRow, jkRow.getNama(), piutang.doubleValue());
										categoryModelLunas.setValue(validatorRow, jkRow.getNama(), lunasRow);
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/DasboardPembayaranPerguruanTinggi.java:2234");
							Common.tampilErrorJikaAdmin(e);
							kegiatans = new ArrayList<Object[]>();
						} finally {
							cleanupSession(sessionData);
						}

						Foot rowUtamaLagi = new Foot();
						rowUtamaLagi.setParent(grid);

						Footer footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecilBold("TOTAL").setParent(footer);

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						new MyLabelKecil().setParent(footer);

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totaltagihan) + "/"
								+ Common.numberFormat.get().format(totalJumlahTagihan)));

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totaldibayar) + "/"
								+ Common.numberFormat.get().format(totalJumlahDibayar)));

						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(totalpiutang) + "/"
								+ Common.numberFormat.get().format(totalJumlahPiutang)));

						Double lunasTotal = totaltagihan > 0 ? (totaldibayar * 100.0) / totaltagihan : 0.0;
						footer = new Footer();
						footer.setParent(rowUtamaLagi);
						footer.appendChild(new MyLabelKecilBold(Common.numberFormat.get().format(lunasTotal)));
						if (tampilChart) {
							renderChartBar(rowUtamapalingAwal,
									"Piutang Mahasiswa TA " + (c + (s == null ? " Semua Semester" : " Semester " + s)),
									categoryModel, size);
							renderChartBar(rowUtamapalingAwal,
									"Persen Lunas TA " + (c + (s == null ? " Semua Semester" : " Semester " + s)),
									categoryModelLunas, size);
						}
					}
				};

				pengajuanBaruEventListener.onEvent(null);
			}


			class PaymentDashboardData {
				double totalTagihan;
				double totalDibayar;
				double totalPiutang;
				double totalLebihBayar;
				int totalRecord;
				int jumlahTagihan;
				int jumlahDibayar;
				int jumlahPiutang;
				int jumlahLunas;
				int jumlahBelumBayar;
				int jumlahParsial;
				int jumlahLebihBayar;
				List<PaymentDashboardRow> byJenis = new ArrayList<PaymentDashboardRow>();
				List<PaymentDashboardRow> byJurusan = new ArrayList<PaymentDashboardRow>();
				List<PaymentDashboardRow> byAngkatan = new ArrayList<PaymentDashboardRow>();
				List<PaymentDashboardRow> byStatus = new ArrayList<PaymentDashboardRow>();
				List<PaymentDashboardRow> byProgram = new ArrayList<PaymentDashboardRow>();
				List<PaymentDashboardRow> byValidator = new ArrayList<PaymentDashboardRow>();
			}

			class PaymentDashboardRow {
				String label;
				Jurusan jurusan;
				JenisKegiatan jenisKegiatan;
				Number angkatan;
				StatusAwalMahasiswa statusAwal;
				String program;
				String validator;
				double tagihan;
				double dibayar;
				double piutang;
				int jumlahTagihan;
				int jumlahDibayar;
				int jumlahPiutang;
				int jumlahLunas;
				int jumlahBelumBayar;
				int jumlahParsial;
				int jumlahLebihBayar;
			}

			private void pembayaranOverviewModern() throws Exception {
				MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardPembayaranPerguruanTinggi.this);
				portalchildren.setWidth("100%");
				portalchildren.setStyle("padding:6px; box-sizing:border-box;");

				Panel panel = new ais.ui.util.MyPanelConfig();
				panel.setTitle("Dasbor Pembayaran & Tagihan Mahasiswa");
				panel.setBorder("none");
				panel.setCollapsible(false);
				panel.setClosable(false);
				panel.setMaximizable(false);
				panel.setMinimizable(false);
				panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:18px; overflow:hidden;"
						+ "background:#ffffff; box-shadow:0 14px 28px rgba(15,23,42,.08);");
				panel.setParent(portalchildren);

				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setStyle("padding:0; background:#f6f8fb;");
				panelchildren.setParent(panel);

				renderPembayaranOverviewContent(panelchildren);
			}

			private void renderPembayaranOverviewContent(final Component parent) throws Exception {
				Common.clear(parent);
				final PaymentDashboardData data = loadPaymentDashboardData();

				org.zkoss.zul.Div shell = new org.zkoss.zul.Div();
				shell.setWidth("100%");
				shell.setStyle("background:#f6f8fb; padding:14px; box-sizing:border-box; overflow:auto;");
				shell.setParent(parent);

				renderPaymentHero(shell, data);
				renderPaymentGlobalFilter(shell, parent);
				renderPaymentMetricCards(shell, data);
				ais.ui.util.DashboardJurnalPembayaranUtil.renderJurnalMahasiswaPanel(shell, "Ringkasan Jurnal Pembayaran Mahasiswa",
						"Menunjukkan akun kas/bank, piutang, pendapatan, denda, dan diskon yang dipakai pada pembayaran mahasiswa. Data ini membantu petugas memastikan akun sudah lengkap sebelum transaksi diposting.");

				MyPortallayout analyticLayout = new MyPortallayout();
				analyticLayout.setParent(shell);
				analyticLayout.setWidth("100%");
				analyticLayout.setMaximizedMode("whole");
				analyticLayout.setStyle("margin-top:10px; padding:0; background:transparent;");

				String pcWidth = Common.isMobile() ? "100%" : "50%";

				MyPortalchildren pcTop = new MyPortalchildren();
				pcTop.setWidth("100%");
				pcTop.setStyle("padding:6px; box-sizing:border-box;");
				pcTop.setParent(analyticLayout);

				MyPortalchildren pcLeft = new MyPortalchildren();
				pcLeft.setWidth(pcWidth);
				pcLeft.setStyle("padding:6px; box-sizing:border-box;");
				pcLeft.setParent(analyticLayout);

				MyPortalchildren pcRight = new MyPortalchildren();
				pcRight.setWidth(pcWidth);
				pcRight.setStyle("padding:6px; box-sizing:border-box;");
				pcRight.setParent(analyticLayout);

				MyPortalchildren pcBottom = new MyPortalchildren();
				pcBottom.setWidth("100%");
				pcBottom.setStyle("padding:6px; box-sizing:border-box;");
				pcBottom.setParent(analyticLayout);

				renderPaymentFunnel(pcTop, data);
				renderPaymentCollectionHealth(pcLeft, data);
				renderPaymentRiskSegmentation(pcRight, data);
				renderPaymentGroupGrid(pcLeft, "Top Piutang per Prodi", data.byJurusan, 8, "Prodi");
				renderPaymentGroupGrid(pcRight, "Rekap per Jenis Pembayaran", data.byJenis, 10, "Jenis Pembayaran");
				renderPaymentGroupGrid(pcBottom, "Dasbor Rinci per Angkatan", data.byAngkatan, 12, "Angkatan");
				renderPaymentGroupGrid(pcBottom, "Dasbor Rinci per Status Awal Mahasiswa", data.byStatus, 12, "Status Awal");
				renderPaymentGroupGrid(pcBottom, "Dasbor Rinci per Program", data.byProgram, 12, "Program");
				renderPaymentGroupGrid(pcBottom, "Dasbor Rinci per Validator", data.byValidator, 12, "Validator");
				renderPaymentOperationalNotes(pcBottom, data);
			}

			private PaymentDashboardData loadPaymentDashboardData() {
				PaymentDashboardData data = new PaymentDashboardData();
				Session session = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();

					Criteria summary = session.createCriteria(Kegiatan.class);
					applyPaymentDashboardCriteria(summary);
					String sql = "sum(case when coalesce(this_.tagihan,0)>0.1 then 1 else 0 end) as jumlahTagihan,"
							+ "sum(case when coalesce(this_.dibayar,0)>0.1 then 1 else 0 end) as jumlahDibayar,"
							+ "sum(case when (coalesce(this_.tagihan,0)-coalesce(this_.dibayar,0))>0.1 then 1 else 0 end) as jumlahPiutang,"
							+ "sum(case when coalesce(this_.tagihan,0)>0.1 and coalesce(this_.dibayar,0)>=coalesce(this_.tagihan,0) then 1 else 0 end) as jumlahLunas,"
							+ "sum(case when coalesce(this_.tagihan,0)>0.1 and coalesce(this_.dibayar,0)<=0.1 then 1 else 0 end) as jumlahBelumBayar,"
							+ "sum(case when coalesce(this_.tagihan,0)>0.1 and coalesce(this_.dibayar,0)>0.1 and coalesce(this_.dibayar,0)<coalesce(this_.tagihan,0) then 1 else 0 end) as jumlahParsial,"
							+ "sum(case when (coalesce(this_.dibayar,0)-coalesce(this_.tagihan,0))>0.1 then 1 else 0 end) as jumlahLebihBayar,"
							+ "sum(case when (coalesce(this_.tagihan,0)-coalesce(this_.dibayar,0))>0.1 then (coalesce(this_.tagihan,0)-coalesce(this_.dibayar,0)) else 0 end) as totalPiutang,"
							+ "sum(case when (coalesce(this_.dibayar,0)-coalesce(this_.tagihan,0))>0.1 then (coalesce(this_.dibayar,0)-coalesce(this_.tagihan,0)) else 0 end) as totalLebihBayar";
					summary.setProjection(Projections.projectionList().add(Projections.sum("tagihan"))
							.add(Projections.sum("dibayar")).add(Projections.rowCount())
							.add(Projections.sqlProjection(sql,
									new String[] { "jumlahTagihan", "jumlahDibayar", "jumlahPiutang", "jumlahLunas",
											"jumlahBelumBayar", "jumlahParsial", "jumlahLebihBayar", "totalPiutang",
											"totalLebihBayar" },
									new Type[] { org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE,
											org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE,
											org.hibernate.type.StandardBasicTypes.DOUBLE })));

					Object[] row = (Object[]) summary.uniqueResult();
					if (row != null) {
						data.totalTagihan = toDouble(row, 0);
						data.totalDibayar = toDouble(row, 1);
						data.totalRecord = toInt(row, 2);
						data.jumlahTagihan = toInt(row, 3);
						data.jumlahDibayar = toInt(row, 4);
						data.jumlahPiutang = toInt(row, 5);
						data.jumlahLunas = toInt(row, 6);
						data.jumlahBelumBayar = toInt(row, 7);
						data.jumlahParsial = toInt(row, 8);
						data.jumlahLebihBayar = toInt(row, 9);
						data.totalPiutang = toDouble(row, 10);
						data.totalLebihBayar = toDouble(row, 11);
					}

					data.byJenis = loadPaymentDashboardGroups(session, "jenis", 0);
					data.byJurusan = loadPaymentDashboardGroups(session, "jurusan", 0);
					data.byAngkatan = loadPaymentDashboardGroups(session, "angkatan", 0);
					data.byStatus = loadPaymentDashboardGroups(session, "status", 0);
					data.byProgram = loadPaymentDashboardGroups(session, "program", 0);
					data.byValidator = loadPaymentDashboardGroups(session, "validator", 0);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/DasboardPembayaranPerguruanTinggi.java:2456");
					Common.tampilErrorJikaAdmin(e);
				} finally {
					cleanupSession(session);
				}
				return data;
			}

			@SuppressWarnings("unchecked")
			private List<PaymentDashboardRow> loadPaymentDashboardGroups(Session session, String mode, int max) {
				List<PaymentDashboardRow> rows = new ArrayList<PaymentDashboardRow>();
				try {
					String property = "jenisKegiatan";
					if ("jurusan".equals(mode)) {
						property = "jurusan";
					} else if ("angkatan".equals(mode)) {
						property = "tahunAngkatan";
					} else if ("status".equals(mode)) {
						property = "statusAwalMahasiswa";
					} else if ("program".equals(mode)) {
						property = "program";
					} else if ("validator".equals(mode)) {
						property = "validator";
					}

					Criteria criteria = session.createCriteria(Kegiatan.class);
					applyPaymentDashboardCriteria(criteria);
					criteria.add(Restrictions.isNotNull(property));
					String sql = "sum(case when coalesce(this_.tagihan,0)>0.1 then 1 else 0 end) as jumlahTagihan,"
							+ "sum(case when coalesce(this_.dibayar,0)>0.1 then 1 else 0 end) as jumlahDibayar,"
							+ "sum(case when (coalesce(this_.tagihan,0)-coalesce(this_.dibayar,0))>0.1 then 1 else 0 end) as jumlahPiutang,"
							+ "sum(case when coalesce(this_.tagihan,0)>0.1 and coalesce(this_.dibayar,0)>=coalesce(this_.tagihan,0) then 1 else 0 end) as jumlahLunas,"
							+ "sum(case when coalesce(this_.tagihan,0)>0.1 and coalesce(this_.dibayar,0)<=0.1 then 1 else 0 end) as jumlahBelumBayar,"
							+ "sum(case when coalesce(this_.tagihan,0)>0.1 and coalesce(this_.dibayar,0)>0.1 and coalesce(this_.dibayar,0)<coalesce(this_.tagihan,0) then 1 else 0 end) as jumlahParsial,"
							+ "sum(case when (coalesce(this_.dibayar,0)-coalesce(this_.tagihan,0))>0.1 then 1 else 0 end) as jumlahLebihBayar,"
							+ "sum(case when (coalesce(this_.tagihan,0)-coalesce(this_.dibayar,0))>0.1 then (coalesce(this_.tagihan,0)-coalesce(this_.dibayar,0)) else 0 end) as totalPiutang";
					criteria.setProjection(Projections.projectionList().add(Projections.groupProperty(property))
							.add(Projections.sum("tagihan")).add(Projections.sum("dibayar"))
							.add(Projections.sqlProjection(sql,
									new String[] { "jumlahTagihan", "jumlahDibayar", "jumlahPiutang", "jumlahLunas",
											"jumlahBelumBayar", "jumlahParsial", "jumlahLebihBayar", "totalPiutang" },
									new Type[] { org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE,
											org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE })));

					List<Object[]> list = criteria.list();
					if (list != null) {
						for (Object[] object : list) {
							if (object == null || object.length == 0 || object[0] == null) {
								continue;
							}
							PaymentDashboardRow row = new PaymentDashboardRow();
							Object key = object[0];
							row.label = getPaymentGroupLabel(key);
							if (key instanceof Jurusan) {
								row.jurusan = (Jurusan) key;
							} else if (key instanceof JenisKegiatan) {
								row.jenisKegiatan = (JenisKegiatan) key;
							} else if (key instanceof StatusAwalMahasiswa) {
								row.statusAwal = (StatusAwalMahasiswa) key;
							} else if (key instanceof Number) {
								row.angkatan = (Number) key;
							} else if ("program".equals(mode)) {
								row.program = String.valueOf(key);
							} else if ("validator".equals(mode)) {
								row.validator = String.valueOf(key);
							}
							row.tagihan = toDouble(object, 1);
							row.dibayar = toDouble(object, 2);
							row.jumlahTagihan = toInt(object, 3);
							row.jumlahDibayar = toInt(object, 4);
							row.jumlahPiutang = toInt(object, 5);
							row.jumlahLunas = toInt(object, 6);
							row.jumlahBelumBayar = toInt(object, 7);
							row.jumlahParsial = toInt(object, 8);
							row.jumlahLebihBayar = toInt(object, 9);
							row.piutang = toDouble(object, 10);
							rows.add(row);
						}
					}
					Collections.sort(rows, new Comparator<PaymentDashboardRow>() {
						@Override
						public int compare(PaymentDashboardRow o1, PaymentDashboardRow o2) {
							return Double.compare(o2.piutang, o1.piutang);
						}
					});
					if (max > 0 && rows.size() > max) {
						return new ArrayList<PaymentDashboardRow>(rows.subList(0, max));
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/DasboardPembayaranPerguruanTinggi.java:2545");
					Common.tampilErrorJikaAdmin(e);
				}
				return rows;
			}

			private void applyPaymentDashboardCriteria(Criteria criteria) {
				criteria.add(Restrictions.eq("aktif", true));
				criteria.add(s == null ? Restrictions.sqlRestriction("1=1")
						: s.equalsIgnoreCase(Perkuliahan.GENAP) ? Restrictions.in("semster", Common.genap)
								: Restrictions.in("semster", Common.ganjil));
				criteria.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jurusan", sk));
				criteria.add(jenisKegiatan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisKegiatan", jenisKegiatan));
				criteria.add(c != null && !c.trim().isEmpty() ? Restrictions.eq("tahunAkademik", c.trim())
						: Restrictions.sqlRestriction("1=1"));
			}

			private void renderPaymentHero(Component parent, PaymentDashboardData d) {
				org.zkoss.zul.Div hero = new org.zkoss.zul.Div();
				hero.setStyle("position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff;"
						+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
						+ "box-shadow:0 14px 30px rgba(15,23,42,.18); box-sizing:border-box;");
				hero.setParent(parent);
				appendPaymentHtml(hero,
						"<div style='position:absolute; right:-70px; top:-80px; width:230px; height:230px; border-radius:999px; background:rgba(255,255,255,.13);'></div>"
								+ "<div style='position:absolute; right:110px; bottom:-70px; width:160px; height:160px; border-radius:999px; background:rgba(255,255,255,.10);'></div>");

				Hbox content = new Hbox();
				content.setWidth("100%");
				content.setPack("justify");
				content.setAlign("center");
				content.setStyle("position:relative; z-index:1; gap:16px; flex-wrap:wrap;");
				content.setParent(hero);

				Vbox titleBox = new Vbox();
				titleBox.setStyle("max-width:760px;");
				titleBox.setParent(content);
				appendPaymentHtml(titleBox,
						"<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>Payment Control Center</div>"
								+ "<div style='font-size:28px; line-height:1.15; font-weight:800; margin-top:6px;'>Pembayaran & Tagihan Mahasiswa</div>"
								+ "<div style='font-size:13px; opacity:.90; margin-top:8px;'>Pantau total tagihan, realisasi pembayaran, piutang aktif, status lunas, pembayaran parsial, dan potensi lebih bayar. Klik angka untuk membuka data detail.</div>");

				String prodiText = sk == null ? "Semua Prodi" : sk.getNama();
				String jenisText = jenisKegiatan == null ? "Semua Jenis Pembayaran" : jenisKegiatan.getNama();
				appendPaymentHtml(titleBox,
						"<div style='margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;'>"
								+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>TA: "
								+ escapePaymentHtml(c == null ? "Semua" : c) + "</span>"
								+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>Semester: "
								+ escapePaymentHtml(s == null ? "Semua" : s) + "</span>"
								+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>"
								+ escapePaymentHtml(prodiText) + "</span>"
								+ "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>"
								+ escapePaymentHtml(jenisText) + "</span>" + "</div>");

				Hbox numberBox = new Hbox();
				numberBox.setStyle("gap:10px; flex-wrap:wrap;");
				numberBox.setParent(content);
				createPaymentHeroNumber(numberBox, "Collection Ratio", percentText(d.totalDibayar, d.totalTagihan),
						createPopupListenerDashboard(null, null, null, null, null, null, "dibayar", c, s, sk,
								jenisKegiatan));
				createPaymentHeroNumber(numberBox, "Piutang Aktif", money(d.totalPiutang),
						createPopupListenerDashboard(null, null, null, null, null, null, "piutang", c, s, sk,
								jenisKegiatan));
			}

			private void renderPaymentGlobalFilter(final Component parent, final Component containerToRefresh) throws Exception {
				final EventListener rootReloadListener = this;
				org.zkoss.zul.Div filterContainer = new org.zkoss.zul.Div();
				filterContainer.setParent(parent);
				filterContainer.setStyle("margin-top:12px; padding:14px; background:#ffffff; border:1px solid #e8eef6; "
						+ "border-radius:16px; box-shadow:0 10px 26px rgba(15,23,42,0.04); box-sizing:border-box;");

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(filterContainer);
				toolbar.setStyle("border:0; background:transparent; padding:0; display:flex; flex-wrap:wrap; align-items:center; gap:8px;");

				new MyLabelAgakKecil("Prodi:").setParent(toolbar);
				final AmbilDataJurusanBanbox ambilDataJurusanBanbox = new AmbilDataJurusanBanbox();
				ambilDataJurusanBanbox.setCols(8);
				ambilDataJurusanBanbox.setReadonly(true);
				ambilDataJurusanBanbox.setValue(sk == null ? "Semua Prodi" : sk.getNama());
				ambilDataJurusanBanbox.setAttribute("jurusan", sk);
				ambilDataJurusanBanbox.setAttribute("myValue", sk);
				ambilDataJurusanBanbox.setParent(toolbar);

				new MyLabelAgakKecil("TA:").setParent(toolbar);
				final Combobox cari = Common.generateTahunAjaran(null);
				Common.selectComboItem(cari, c);
				cari.setCols(6);
				cari.setParent(toolbar);

				new MyLabelAgakKecil("Semester:").setParent(toolbar);
				final Combobox jenissemester = new Combobox();
				jenissemester.setReadonly(true);
				jenissemester.setCols(6);
				MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
				comboitem.setValue(Perkuliahan.GANJIL);
				jenissemester.appendChild(comboitem);
				comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
				comboitem.setValue(Perkuliahan.GENAP);
				jenissemester.appendChild(comboitem);
				comboitem = new MyComboitemConfig("Semua");
				comboitem.setValue(null);
				jenissemester.appendChild(comboitem);
				jenissemester.setSelectedItem(comboitem);
				Common.selectComboItem(jenissemester, s);
				jenissemester.setParent(toolbar);

				new MyLabelAgakKecil("Jenis:").setParent(toolbar);
				final Combobox searchJenisPembayaran = new Combobox();
				searchJenisPembayaran.setReadonly(true);
				searchJenisPembayaran.setCols(8);
				Common.insertComboDanSemua(searchJenisPembayaran, "namaKegiatan", JenisKegiatan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
				Common.selectComboItem(searchJenisPembayaran, jenisKegiatan);
				searchJenisPembayaran.setParent(toolbar);

				final EventListener refreshDashboardListener = new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						c = cari.getValue() != null ? cari.getValue().trim() : "";
						s = (jenissemester.getSelectedItem() == null || jenissemester.getSelectedItem().getValue() == null) ? null
								: jenissemester.getSelectedItem().getValue() + "";
						sk = (Jurusan) ambilDataJurusanBanbox.getAttribute("jurusan");
						jenisKegiatan = (JenisKegiatan) (searchJenisPembayaran.getSelectedItem() == null ? null
								: searchJenisPembayaran.getSelectedItem().getValue());
						rootReloadListener.onEvent(null);
					}
				};

				ambilDataJurusanBanbox.setEventListener(refreshDashboardListener);
				cari.addEventListener("onChange", refreshDashboardListener);
				jenissemester.addEventListener("onChange", refreshDashboardListener);
				searchJenisPembayaran.addEventListener("onChange", refreshDashboardListener);

				MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan Dasbor", "/img/search.png");
				refresh.setTooltiptext("Refresh overview pembayaran berdasarkan filter");
				refresh.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; padding:6px 14px;");
				refresh.setParent(toolbar);
				refresh.addEventListener("onClick", refreshDashboardListener);

				final MyToolbarbuttonConfig prosesTagihan = new MyToolbarbuttonConfig("Proses Tagihan", "/img/save.gif");
				prosesTagihan.setTooltiptext("Proses ulang tagihan sesuai jenis yang dipilih");
				prosesTagihan.setParent(toolbar);
				prosesTagihan.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						JenisKegiatan currentJenis = (searchJenisPembayaran.getSelectedItem() == null) ? null
								: (JenisKegiatan) searchJenisPembayaran.getSelectedItem().getValue();
						MyToolbarbuttonConfig tempBtn = KegiatanProsesHeper.prosesUlangTagihan(
								"Proses Tagihan", "/img/save.gif", currentJenis, currentJenis != null);
						Events.sendEvent(tempBtn, new Event(Events.ON_CLICK, tempBtn));
					}
				});
			}

			private void renderPaymentMetricCards(Component parent, PaymentDashboardData d) {
				org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
				wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
				wrap.setParent(parent);
				createPaymentMetricCard(wrap, "Total Tagihan", money(d.totalTagihan), d.jumlahTagihan + " data tagihan", "#dbeafe",
						"#1e40af", "Rp", createPopupListenerDashboard(null, null, null, null, null, null, "tagihan", c,
								s, sk, jenisKegiatan));
				createPaymentMetricCard(wrap, "Total Dibayar", money(d.totalDibayar), d.jumlahDibayar + " data pembayaran", "#dcfce7",
						"#166534", "✓", createPopupListenerDashboard(null, null, null, null, null, null, "dibayar", c,
								s, sk, jenisKegiatan));
				createPaymentMetricCard(wrap, "Piutang Aktif", money(d.totalPiutang), d.jumlahPiutang + " mahasiswa/tagihan", "#fee2e2",
						"#991b1b", "!", createPopupListenerDashboard(null, null, null, null, null, null, "piutang", c,
								s, sk, jenisKegiatan));
				createPaymentMetricCard(wrap, "Lunas", formatInt(d.jumlahLunas), "Tagihan sudah tertutup", "#fef3c7",
						"#92400e", "★", createPopupListenerDashboard(null, null, null, null, null, null, "lunas", c,
								s, sk, jenisKegiatan));
				createPaymentMetricCard(wrap, "Parsial", formatInt(d.jumlahParsial), "Bayar sebagian", "#fef9c3",
						"#854d0e", "½", createPopupListenerDashboard(null, null, null, null, null, null, "parsial", c,
								s, sk, jenisKegiatan));
				createPaymentMetricCard(wrap, "Belum Bayar", formatInt(d.jumlahBelumBayar), "Tagihan tanpa pembayaran", "#ede9fe",
						"#5b21b6", "0", createPopupListenerDashboard(null, null, null, null, null, null, "belumbayar",
								c, s, sk, jenisKegiatan));
				createPaymentMetricCard(wrap, "Lebih Bayar", money(d.totalLebihBayar), d.jumlahLebihBayar + " data", "#cffafe",
						"#155e75", "+", createPopupListenerDashboard(null, null, null, null, null, null, "lebihbayar",
								c, s, sk, jenisKegiatan));
			}

			private void renderPaymentFunnel(Component parent, PaymentDashboardData d) {
				Panelchildren pch = createPaymentModernPanel(" Status Tagihan & Pembayaran", parent);
				appendPaymentHtml(pch,
						"<div style='font-size:12px; color:#64748b; margin-bottom:12px; line-height:1.55;'> memperlihatkan perjalanan dari total tagihan, pembayaran masuk, piutang tersisa, tagihan parsial, belum bayar, sampai lunas. Setiap angka bisa diklik untuk membuka popup data.</div>");
				double max = Math.max(1, Math.max(d.totalTagihan, Math.max(d.totalDibayar, d.totalPiutang)));
				renderPaymentFunnelRow(pch, "Tagihan", d.totalTagihan, max, "#2563eb", money(d.totalTagihan),
						createPopupListenerDashboard(null, null, null, null, null, null, "tagihan", c, s, sk, jenisKegiatan));
				renderPaymentFunnelRow(pch, "Dibayar", d.totalDibayar, max, "#16a34a", money(d.totalDibayar),
						createPopupListenerDashboard(null, null, null, null, null, null, "dibayar", c, s, sk, jenisKegiatan));
				renderPaymentFunnelRow(pch, "Piutang Aktif", d.totalPiutang, max, "#dc2626", money(d.totalPiutang),
						createPopupListenerDashboard(null, null, null, null, null, null, "piutang", c, s, sk, jenisKegiatan));
				renderPaymentFunnelRow(pch, "Parsial", d.jumlahParsial, Math.max(1, d.jumlahTagihan), "#f59e0b",
						formatInt(d.jumlahParsial), createPopupListenerDashboard(null, null, null, null, null, null, "parsial",
								c, s, sk, jenisKegiatan));
				renderPaymentFunnelRow(pch, "Belum Bayar", d.jumlahBelumBayar, Math.max(1, d.jumlahTagihan), "#7c3aed",
						formatInt(d.jumlahBelumBayar), createPopupListenerDashboard(null, null, null, null, null, null,
								"belumbayar", c, s, sk, jenisKegiatan));
				renderPaymentFunnelRow(pch, "Lunas", d.jumlahLunas, Math.max(1, d.jumlahTagihan), "#0891b2",
						formatInt(d.jumlahLunas), createPopupListenerDashboard(null, null, null, null, null, null, "lunas",
								c, s, sk, jenisKegiatan));
			}

			private void renderPaymentCollectionHealth(Component parent, PaymentDashboardData d) {
				Panelchildren pch = createPaymentModernPanel("Kesehatan Koleksi Pembayaran", parent);
				appendPaymentHtml(pch,
						"<div style='font-size:12px; color:#64748b; line-height:1.55; margin-bottom:12px;'>membantu membaca rasio pembayaran, rasio piutang, rasio lunas, dan tekanan tagihan belum bayar.</div>");
				renderPaymentGauge(pch, "Collection Ratio", percentValue(d.totalDibayar, d.totalTagihan),
						"Total dibayar dibanding total tagihan.", "#16a34a", createPopupListenerDashboard(null, null, null, null,
								null, null, "dibayar", c, s, sk, jenisKegiatan));
				renderPaymentGauge(pch, "Outstanding Ratio", percentValue(d.totalPiutang, d.totalTagihan),
						"Piutang aktif dibanding total tagihan.", "#dc2626", createPopupListenerDashboard(null, null, null, null,
								null, null, "piutang", c, s, sk, jenisKegiatan));
				renderPaymentGauge(pch, "Lunas Count Ratio", percentValue(d.jumlahLunas, d.jumlahTagihan),
						"Jumlah tagihan lunas dibanding jumlah tagihan.", "#0891b2", createPopupListenerDashboard(null, null,
								null, null, null, null, "lunas", c, s, sk, jenisKegiatan));
				renderPaymentGauge(pch, "Belum Bayar Ratio", percentValue(d.jumlahBelumBayar, d.jumlahTagihan),
						"Jumlah tagihan yang belum ada pembayaran.", "#7c3aed", createPopupListenerDashboard(null, null, null,
								null, null, null, "belumbayar", c, s, sk, jenisKegiatan));
			}

			private void renderPaymentRiskSegmentation(Component parent, PaymentDashboardData d) {
				Panelchildren pch = createPaymentModernPanel("Segmentasi Risiko Tagihan", parent);
				appendPaymentHtml(pch,
						"<div style='font-size:12px; color:#64748b; margin-bottom:12px; line-height:1.55;'>Segmentasi ini memisahkan tagihan lunas, parsial, belum bayar, dan lebih bayar agar tim keuangan bisa menentukan prioritas tindak lanjut.</div>");
				renderPaymentSegmentRow(pch, "Lunas", d.jumlahLunas, d.jumlahTagihan, "#16a34a", "lunas");
				renderPaymentSegmentRow(pch, "Parsial", d.jumlahParsial, d.jumlahTagihan, "#f59e0b", "parsial");
				renderPaymentSegmentRow(pch, "Belum Bayar", d.jumlahBelumBayar, d.jumlahTagihan, "#dc2626", "belumbayar");
				renderPaymentSegmentRow(pch, "Lebih Bayar", d.jumlahLebihBayar, Math.max(1, d.jumlahTagihan), "#0891b2", "lebihbayar");
				appendPaymentHtml(pch,
						"<div style='margin-top:12px; padding:10px 12px; border-radius:12px; background:#f8fafc; border:1px solid #e2e8f0; color:#475569; font-size:12px; line-height:1.55;'>Prioritas follow-up: tagihan <b>Belum Bayar</b> dan <b>Parsial</b> dengan piutang tertinggi per prodi/jenis pembayaran. Cek juga <b>Lebih Bayar</b> untuk rekonsiliasi.</div>");
			}

			private void renderPaymentGroupGrid(Component parent, String title, List<PaymentDashboardRow> rowsData, int maxRows,
					String firstColumnTitle) {
				Panelchildren pch = createPaymentModernPanel(title, parent);
				Grid grid = new Grid();
				grid.setSclass("dgrid fgrid");
				grid.setStyle("border:0px; background:transparent; min-height:100px;");
				grid.setMold("paging");
				grid.setPageSize(12);
				grid.setParent(pch);
				try {
					grid.getPagingChild().setMold("os");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPembayaranPerguruanTinggi.java:2794");
				}

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig(firstColumnTitle);
				column.setWidth("24%");
				columns.appendChild(column);
				column = new MyColumnConfig("Tagihan");
				column.setAlign("right");
				columns.appendChild(column);
				column = new MyColumnConfig("Dibayar");
				column.setAlign("right");
				columns.appendChild(column);
				column = new MyColumnConfig("Piutang");
				column.setAlign("right");
				columns.appendChild(column);
				column = new MyColumnConfig("Lunas");
				column.setAlign("right");
				column.setWidth("12%");
				columns.appendChild(column);
				column = new MyColumnConfig("Belum Bayar");
				column.setAlign("right");
				column.setWidth("12%");
				columns.appendChild(column);

				Rows rows = new Rows();
				rows.setParent(grid);
				double totalTagihan = 0;
				double totalDibayar = 0;
				double totalPiutang = 0;
				int totalLunas = 0;
				int totalBelum = 0;
				int rendered = 0;
				if (rowsData != null) {
					for (PaymentDashboardRow dataRow : rowsData) {
						if (dataRow == null) {
							continue;
						}
						totalTagihan += dataRow.tagihan;
						totalDibayar += dataRow.dibayar;
						totalPiutang += dataRow.piutang;
						totalLunas += dataRow.jumlahLunas;
						totalBelum += dataRow.jumlahBelumBayar;
					}

					for (PaymentDashboardRow dataRow : rowsData) {
						if (dataRow == null) {
							continue;
						}
						if (maxRows > 0 && rendered >= maxRows) {
							break;
						}
						rendered++;

						MyFormRow row = new MyFormRow();
						row.setParent(rows);
						new MyLabelKecilBold(dataRow.label).setParent(row);
						createPaymentLink(row, money(dataRow.tagihan) + " / " + formatInt(dataRow.jumlahTagihan),
								"Detail tagihan", createPopupFromDashboardRow(dataRow, "tagihan"),
								"font-size:11px; font-weight:700; color:#1d4ed8; text-decoration:none; display:block; text-align:right;");
						createPaymentLink(row, money(dataRow.dibayar) + " / " + formatInt(dataRow.jumlahDibayar),
								"Detail dibayar", createPopupFromDashboardRow(dataRow, "dibayar"),
								"font-size:11px; font-weight:700; color:#166534; text-decoration:none; display:block; text-align:right;");
						createPaymentLink(row, money(dataRow.piutang) + " / " + formatInt(dataRow.jumlahPiutang),
								"Detail piutang", createPopupFromDashboardRow(dataRow, "piutang"),
								"font-size:11px; font-weight:800; color:#991b1b; text-decoration:none; display:block; text-align:right;");
						createPaymentLink(row, percentText(dataRow.dibayar, dataRow.tagihan) + " / " + formatInt(dataRow.jumlahLunas),
								"Detail lunas", createPopupFromDashboardRow(dataRow, "lunas"),
								"font-size:11px; font-weight:700; color:#0f766e; text-decoration:none; display:block; text-align:right;");
						createPaymentLink(row, formatInt(dataRow.jumlahBelumBayar), "Detail belum bayar",
								createPopupFromDashboardRow(dataRow, "belumbayar"),
								"font-size:11px; font-weight:700; color:#7c2d12; text-decoration:none; display:block; text-align:right;");
					}
				}

				Foot foot = new Foot();
				foot.setParent(grid);
				Footer footer = new Footer();
				footer.setParent(foot);
				new MyLabelKecilBold("TOTAL FILTER").setParent(footer);
				footer = new Footer();
				footer.setParent(foot);
				createPaymentLink(footer, money(totalTagihan), "Detail total tagihan", createPopupListenerDashboard(null, null,
						null, null, null, null, "tagihan", c, s, sk, jenisKegiatan),
						"font-size:11px; font-weight:800; color:#1d4ed8; text-decoration:none;");
				footer = new Footer();
				footer.setParent(foot);
				createPaymentLink(footer, money(totalDibayar), "Detail total dibayar", createPopupListenerDashboard(null, null,
						null, null, null, null, "dibayar", c, s, sk, jenisKegiatan),
						"font-size:11px; font-weight:800; color:#166534; text-decoration:none;");
				footer = new Footer();
				footer.setParent(foot);
				createPaymentLink(footer, money(totalPiutang), "Detail total piutang", createPopupListenerDashboard(null, null,
						null, null, null, null, "piutang", c, s, sk, jenisKegiatan),
						"font-size:11px; font-weight:800; color:#991b1b; text-decoration:none;");
				footer = new Footer();
				footer.setParent(foot);
				createPaymentLink(footer, formatInt(totalLunas), "Detail lunas", createPopupListenerDashboard(null, null, null,
						null, null, null, "lunas", c, s, sk, jenisKegiatan),
						"font-size:11px; font-weight:800; color:#0f766e; text-decoration:none;");
				footer = new Footer();
				footer.setParent(foot);
				createPaymentLink(footer, formatInt(totalBelum), "Detail belum bayar", createPopupListenerDashboard(null, null,
						null, null, null, null, "belumbayar", c, s, sk, jenisKegiatan),
						"font-size:11px; font-weight:800; color:#7c2d12; text-decoration:none;");
			}

			private void renderPaymentOperationalNotes(Component parent, PaymentDashboardData d) {
				Panelchildren pch = createPaymentModernPanel("Insight & Aksi Operasional Keuangan", parent);
				int outstandingRatio = percentValue(d.totalPiutang, d.totalTagihan);
				String level = outstandingRatio >= 50 ? "TINGGI" : outstandingRatio >= 20 ? "SEDANG" : "TERKENDALI";
				appendPaymentHtml(pch,
						"<div style='display:flex; gap:12px; flex-wrap:wrap;'>"
								+ "<div style='flex:1 1 220px; padding:14px; border-radius:14px; background:#f8fafc; border:1px solid #e2e8f0;'>"
								+ "<div style='font-size:12px; color:#64748b;'>Level Outstanding</div>"
								+ "<div style='font-size:24px; font-weight:800; color:#0f172a; margin-top:4px;'>" + level
								+ "</div>"
								+ "<div style='font-size:12px; color:#64748b; margin-top:6px;'>Outstanding ratio "
								+ outstandingRatio + "% dari total tagihan aktif.</div>" + "</div>"
								+ "<div style='flex:2 1 420px; padding:14px; border-radius:14px; background:#ffffff; border:1px solid #e2e8f0;'>"
								+ "<div style='font-size:13px; font-weight:800; color:#0f172a;'>Rencana Tindak Lanjut</div>"
								+ "<ol style='margin:8px 0 0 18px; padding:0; color:#475569; font-size:12px; line-height:1.65;'>"
								+ "<li>Prioritaskan prodi/jenis pembayaran dengan piutang tertinggi.</li>"
								+ "<li>Follow-up mahasiswa dengan status belum bayar dan parsial.</li>"
								+ "<li>Rekonsiliasi data lebih bayar untuk menghindari selisih kas/tagihan.</li>"
								+ "<li>Gunakan panel existing di bawah untuk detail per prodi, angkatan, status, program, dan validator.</li>"
								+ "</ol></div></div>");
			}

			private Panelchildren createPaymentModernPanel(String title, Component parent) {
				Panel panel = new ais.ui.util.MyPanelConfig();
				panel.setTitle(title);
				panel.setBorder("none");
				panel.setCollapsible(false);
				panel.setClosable(false);
				panel.setMaximizable(false);
				panel.setMinimizable(false);
				panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:16px; overflow:hidden;"
						+ "background:#ffffff; box-shadow:0 12px 24px rgba(15,23,42,.07);");
				panel.setParent(parent);
				Panelchildren pch = new Panelchildren();
				pch.setStyle("padding:14px; background:#ffffff;");
				pch.setParent(panel);
				return pch;
			}

			private void createPaymentMetricCard(Component parent, String title, String value, String desc, String bg,
					String color, String icon, EventListener listener) {
				org.zkoss.zul.Div card = new org.zkoss.zul.Div();
				card.setStyle("flex:1 1 150px; min-width:150px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:14px;"
						+ "box-shadow:0 10px 22px rgba(15,23,42,.06); box-sizing:border-box;");
				card.setParent(parent);
				Hbox top = new Hbox();
				top.setWidth("100%");
				top.setPack("justify");
				top.setAlign("center");
				top.setParent(card);
				appendPaymentHtml(top, "<div style='width:38px; height:38px; border-radius:12px; display:flex; align-items:center; justify-content:center; font-weight:800; background:"
						+ bg + "; color:" + color + ";'>" + escapePaymentHtml(icon) + "</div>");
				createPaymentLink(top, value, "Klik untuk detail " + title, listener,
						"font-size:22px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer;");
				appendPaymentHtml(card,
						"<div style='font-size:12px; color:#64748b; margin-top:10px;'>" + escapePaymentHtml(title)
								+ "</div><div style='font-size:11px; color:#94a3b8; margin-top:3px;'>"
								+ escapePaymentHtml(desc) + "</div>");
			}

			private void createPaymentHeroNumber(Component parent, String label, String value, EventListener listener) {
				org.zkoss.zul.Div box = new org.zkoss.zul.Div();
				box.setStyle("min-width:150px; padding:12px 14px; border-radius:16px; background:rgba(255,255,255,.16); border:1px solid rgba(255,255,255,.18);");
				box.setParent(parent);
				createPaymentLink(box, value, "Klik untuk detail " + label, listener,
						"display:block; color:#ffffff; font-size:22px; font-weight:800; text-decoration:none;");
				appendPaymentHtml(box,
						"<div style='font-size:11px; opacity:.86; margin-top:4px;'>" + escapePaymentHtml(label) + "</div>");
			}

			private void renderPaymentFunnelRow(Component parent, String label, double value, double max, String color,
					String displayValue, EventListener listener) {
				int percent = percentValue(value, max);
				org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
				wrap.setStyle("margin-bottom:10px;");
				wrap.setParent(parent);
				Hbox top = new Hbox();
				top.setWidth("100%");
				top.setPack("justify");
				top.setAlign("center");
				top.setParent(wrap);
				appendPaymentHtml(top, "<div style='font-size:12px; color:#475569; font-weight:700;'>" + escapePaymentHtml(label)
						+ "</div>");
				createPaymentLink(top, displayValue, "Klik detail " + label, listener,
						"font-size:12px; font-weight:800; color:#0f172a; text-decoration:none;");
				appendPaymentHtml(wrap,
						"<div style='height:10px; border-radius:999px; background:#e5e7eb; overflow:hidden; margin-top:5px;'>"
								+ "<div style='height:10px; width:" + percent + "%; border-radius:999px; background:" + color
								+ ";'></div></div>");
			}

			private void renderPaymentGauge(Component parent, String title, int percent, String desc, String color,
					EventListener listener) {
				org.zkoss.zul.Div box = new org.zkoss.zul.Div();
				box.setStyle("padding:12px; border-radius:14px; border:1px solid #e2e8f0; margin-bottom:10px; background:#f8fafc;");
				box.setParent(parent);
				Hbox top = new Hbox();
				top.setWidth("100%");
				top.setPack("justify");
				top.setAlign("center");
				top.setParent(box);
				appendPaymentHtml(top, "<div style='font-size:12px; font-weight:800; color:#0f172a;'>" + escapePaymentHtml(title)
						+ "</div>");
				createPaymentLink(top, percent + "%", "Klik detail " + title, listener,
						"font-size:18px; font-weight:800; color:#0f172a; text-decoration:none;");
				appendPaymentHtml(box,
						"<div style='height:9px; border-radius:999px; background:#e2e8f0; overflow:hidden; margin-top:8px;'>"
								+ "<div style='height:9px; width:" + Math.max(0, Math.min(100, percent))
								+ "%; border-radius:999px; background:" + color + ";'></div></div>"
								+ "<div style='font-size:11px; color:#64748b; margin-top:6px;'>" + escapePaymentHtml(desc)
								+ "</div>");
			}

			private void renderPaymentSegmentRow(Component parent, String label, int value, int total, String color,
					String type) {
				int percent = percentValue(value, total);
				org.zkoss.zul.Div row = new org.zkoss.zul.Div();
				row.setStyle("display:flex; align-items:center; gap:10px; margin-bottom:10px;");
				row.setParent(parent);
				appendPaymentHtml(row, "<div style='width:120px; font-size:12px; font-weight:800; color:#334155;'>"
						+ escapePaymentHtml(label) + "</div>");
				appendPaymentHtml(row,
						"<div style='flex:1; height:10px; border-radius:999px; background:#e2e8f0; overflow:hidden;'>"
								+ "<div style='height:10px; width:" + percent + "%; border-radius:999px; background:" + color
								+ ";'></div></div>");
				createPaymentLink(row, formatInt(value) + " (" + percent + "%)", "Klik detail " + label,
						createPopupListenerDashboard(null, null, null, null, null, null, type, c, s, sk, jenisKegiatan),
						"width:110px; text-align:right; font-size:12px; font-weight:800; color:#0f172a; text-decoration:none;");
			}

			private EventListener createPopupFromDashboardRow(PaymentDashboardRow row, String type) {
				return createPopupListenerDashboard(row.jurusan, row.jenisKegiatan, row.angkatan, row.statusAwal, row.program,
						row.validator, type, c, s, sk, jenisKegiatan);
			}

			private A createPaymentLink(Component parent, String text, String tooltip, EventListener listener, String style) {
				A link = new A(text == null ? "-" : text);
				link.setStyle(style == null ? "font-size:11px; font-weight:bold;" : style);
				link.setTooltiptext(tooltip);
				link.setParent(parent);
				if (listener != null) {
					link.addEventListener("onClick", listener);
				}
				return link;
			}

			private String getPaymentGroupLabel(Object key) {
				try {
					if (key instanceof Jurusan) {
						return ((Jurusan) key).getNama();
					}
					if (key instanceof JenisKegiatan) {
						return ((JenisKegiatan) key).getNama();
					}
					if (key instanceof StatusAwalMahasiswa) {
						return ((StatusAwalMahasiswa) key).getNama();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardPembayaranPerguruanTinggi.java:3059");
				}
				return key == null ? "Tidak diketahui" : String.valueOf(key);
			}

			private void appendPaymentHtml(Component parent, String html) {
				org.zkoss.zul.Html h = new org.zkoss.zul.Html(html == null ? "" : html);
				h.setParent(parent);
			}

			private String escapePaymentHtml(String value) {
				if (value == null) {
					return "";
				}
				return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
			}

			private String money(double value) {
				try {
					return Common.numberFormat.get().format(value);
				} catch (Exception e) {
					return String.valueOf(value);
				}
			}

			private String formatInt(double value) {
				try {
					return Common.numberFormat.get().format(value);
				} catch (Exception e) {
					return String.valueOf((int) value);
				}
			}

			private String percentText(double numerator, double denominator) {
				return percentValue(numerator, denominator) + "%";
			}

			private int percentValue(double numerator, double denominator) {
				if (denominator <= 0) {
					return 0;
				}
				return (int) Math.round((numerator * 100.0) / denominator);
			}

			private double toDouble(Object[] row, int index) {
				if (row == null || row.length <= index || row[index] == null) {
					return 0.0;
				}
				if (row[index] instanceof Number) {
					return ((Number) row[index]).doubleValue();
				}
				try {
					return Double.parseDouble(row[index].toString());
				} catch (Exception e) {
					return 0.0;
				}
			}

			private int toInt(Object[] row, int index) {
				return (int) Math.round(toDouble(row, index));
			}

			@Override
			public void onEvent(Event arg0) throws Exception {
				// Urutan render dibuat bertahap supaya indikator loading/progress sempat tampil di UI.
				jalankanTahapanDashboard(0);
			}
		};

		Common.createDefaultTimer(reloadPiutangPerProdi);
	}

	// =========================================================================================
	// HELPER UNTUK MENCEGAH DUPLIKASI KODE SAAT MEMBANGUN CHART & POPUP LISTENER
	// =========================================================================================

	private void renderChartBar(Row anchorRow, String title, HtmlCategoryModel model, int sizeBar) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(anchorRow.getParent());
		row.setAlign("center");
		row.appendChild(new Html(buildModernChartHtml(title, model,
				"menyajikan grafik ringkas berdasarkan data pada tabel di atas. Gunakan grafik ini untuk melihat perbandingan nominal, piutang, atau tingkat pelunasan secara cepat.")));
	}


	private EventListener createPopupListenerDashboard(final Jurusan jurusan, final JenisKegiatan jk,
			final Number angkatan, final StatusAwalMahasiswa statusAwal, final String program, final String validator,
			final String type, final String c, final String s, final Jurusan globalJurusan,
			final JenisKegiatan globalJenisKegiatan) {
		return new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				EventListener eventListener = (EventListener) Common
						.cetakDataCustomButton(Kegiatan.class, new DataCriteriaWithColumn() {
							@Override
							public Object[] initCriteria(boolean order) {
								try {
									Criteria criteria = HibernateUtil.currentSession().createCriteria(Kegiatan.class)
											.add(Restrictions.eq("aktif", true))
											.add(s == null ? Restrictions.sqlRestriction("1=1")
													: s.equalsIgnoreCase(Perkuliahan.GENAP)
															? Restrictions.in("semster", Common.genap)
															: Restrictions.in("semster", Common.ganjil))
											.add(c != null && !c.isEmpty() ? Restrictions.eq("tahunAkademik", c)
													: Restrictions.sqlRestriction("1=1"))
											.addOrder(Order.asc("kode")).addOrder(Order.asc("id"));

									Jurusan effectiveJurusan = jurusan != null ? jurusan : globalJurusan;
									JenisKegiatan effectiveJenis = jk != null ? jk : globalJenisKegiatan;
									if (effectiveJurusan != null && effectiveJurusan.getId() != null) {
										criteria.add(Restrictions.eq("jurusan", effectiveJurusan));
									}
									if (effectiveJenis != null && effectiveJenis.getId() != null) {
										criteria.add(Restrictions.eq("jenisKegiatan", effectiveJenis));
									}
									if (angkatan != null) {
										criteria.add(Restrictions.eq("tahunAngkatan", angkatan.intValue()));
									}
									if (statusAwal != null && statusAwal.getId() != null) {
										criteria.add(Restrictions.eq("statusAwalMahasiswa", statusAwal));
									}
									if (program != null && !program.trim().isEmpty()) {
										criteria.add(Restrictions.eq("program", program));
									}
									if (validator != null && !validator.trim().isEmpty()) {
										criteria.add(Restrictions.eq("validator", validator));
									}

									applyPaymentPopupTypeRestriction(criteria, type);

									String[] headerArr = getHeaderAction(effectiveJenis);
									return new Object[] { criteria, headerArr };
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
								return null;
							}
						}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA PEMBAYARAN",
						getBlankArray(100))
						.getAttribute("eventListener");
				eventListener.onEvent(null);
			}
		};
	}

	private void applyPaymentPopupTypeRestriction(Criteria criteria, String type) {
		if (type == null || "all".equalsIgnoreCase(type)) {
			return;
		}
		if ("tagihan".equals(type)) {
			criteria.add(Restrictions.sqlRestriction("abs(coalesce({alias}.tagihan,0)) > 0.1"));
		} else if ("dibayar".equals(type)) {
			criteria.add(Restrictions.sqlRestriction("abs(coalesce({alias}.dibayar,0)) > 0.1"));
		} else if ("piutang".equals(type)) {
			criteria.add(Restrictions.sqlRestriction("(coalesce({alias}.tagihan,0)-coalesce({alias}.dibayar,0)) > 0.1"));
		} else if ("lunas".equals(type)) {
			criteria.add(Restrictions.sqlRestriction(
					"coalesce({alias}.tagihan,0) > 0.1 and coalesce({alias}.dibayar,0) >= coalesce({alias}.tagihan,0)"));
		} else if ("belumbayar".equals(type)) {
			criteria.add(Restrictions.sqlRestriction(
					"coalesce({alias}.tagihan,0) > 0.1 and coalesce({alias}.dibayar,0) <= 0.1"));
		} else if ("parsial".equals(type)) {
			criteria.add(Restrictions.sqlRestriction(
					"coalesce({alias}.tagihan,0) > 0.1 and coalesce({alias}.dibayar,0) > 0.1 and coalesce({alias}.dibayar,0) < coalesce({alias}.tagihan,0)"));
		} else if ("lebihbayar".equals(type)) {
			criteria.add(Restrictions.sqlRestriction("(coalesce({alias}.dibayar,0)-coalesce({alias}.tagihan,0)) > 0.1"));
		}
	}

	// Helper Pop-up untuk klik data jurusan
	private EventListener createPopupListener(final Jurusan jurusan, final JenisKegiatan jk, final String type,
			final String c, final String s) {
		return new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				EventListener eventListener = (EventListener) Common
						.cetakDataCustomButton(Kegiatan.class, new DataCriteriaWithColumn() {
							@Override
							public Object[] initCriteria(boolean order) {
								try {
									Criteria criteria = HibernateUtil.currentSession().createCriteria(Kegiatan.class)
											.add(Restrictions.eq("aktif", true))
											.add(s == null ? Restrictions.sqlRestriction("1=1")
													: s.equalsIgnoreCase(Perkuliahan.GENAP)
															? Restrictions.in("semster", Common.genap)
															: Restrictions.in("semster", Common.ganjil))
											.add(Restrictions.isNotNull("jurusan"))
											.add(Restrictions.isNotNull("jenisKegiatan"))
											.add(c != null && !c.isEmpty() ? Restrictions.eq("tahunAkademik", c)
													: Restrictions.sqlRestriction("1=1"))
											.add(Restrictions.eq("jurusan", jurusan))
											.add(Restrictions.eq("jenisKegiatan", jk)).addOrder(Order.asc("kode"))
											.addOrder(Order.asc("id"));

									if ("tagihan".equals(type))
										criteria.add(Restrictions.or(Restrictions.gt("tagihan", 0.1),
												Restrictions.lt("tagihan", -0.1)));
									else if ("dibayar".equals(type))
										criteria.add(Restrictions.or(Restrictions.gt("dibayar", 0.1),
												Restrictions.lt("dibayar", -0.1)));
									else if ("piutang".equals(type))
										criteria.add(Restrictions.sqlRestriction(
												"(coalesce({alias}.tagihan,0)-coalesce({alias}.dibayar,0)) > 0.1"));

									String[] headerArr = getHeaderAction(jk);
									return new Object[] { criteria, headerArr };
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
								return null;
							}
						}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
								getBlankArray(100))
						.getAttribute("eventListener");
				eventListener.onEvent(null);
			}
		};
	}

	// Helper Pop-up untuk klik data angkatan
	private EventListener createPopupListenerAngkatan(final Number angkatan, final JenisKegiatan jk, final String type,
			final String c, final String s, final Jurusan sk) {
		return new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				EventListener eventListener = (EventListener) Common
						.cetakDataCustomButton(Kegiatan.class, new DataCriteriaWithColumn() {
							@Override
							public Object[] initCriteria(boolean order) {
								try {
									Criteria criteria = HibernateUtil.currentSession().createCriteria(Kegiatan.class)
											.add(Restrictions.eq("aktif", true))
											.add(s == null ? Restrictions.sqlRestriction("1=1")
													: s.equalsIgnoreCase(Perkuliahan.GENAP)
															? Restrictions.in("semster", Common.genap)
															: Restrictions.in("semster", Common.ganjil))
											.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("jurusan", sk))
											.add(Restrictions.isNotNull("tahunAngkatan"))
											.add(Restrictions.isNotNull("jenisKegiatan"))
											.add(c != null && !c.isEmpty() ? Restrictions.eq("tahunAkademik", c)
													: Restrictions.sqlRestriction("1=1"))
											.add(Restrictions.eq("tahunAngkatan", angkatan.intValue()))
											.add(Restrictions.eq("jenisKegiatan", jk)).addOrder(Order.asc("kode"))
											.addOrder(Order.asc("id"));

									if ("tagihan".equals(type))
										criteria.add(Restrictions.or(Restrictions.gt("tagihan", 0.1),
												Restrictions.lt("tagihan", -0.1)));
									else if ("dibayar".equals(type))
										criteria.add(Restrictions.or(Restrictions.gt("dibayar", 0.1),
												Restrictions.lt("dibayar", -0.1)));
									else if ("piutang".equals(type))
										criteria.add(Restrictions.sqlRestriction(
												"(coalesce({alias}.tagihan,0)-coalesce({alias}.dibayar,0)) > 0.1"));

									String[] headerArr = getHeaderAction(jk);
									return new Object[] { criteria, headerArr };
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
								return null;
							}
						}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
								getBlankArray(100))
						.getAttribute("eventListener");
				eventListener.onEvent(null);
			}
		};
	}

	// Helper Pop-up untuk klik data Status
	private EventListener createPopupListenerStatus(final StatusAwalMahasiswa statusAwal, final JenisKegiatan jk,
			final String type, final String c, final String s, final Jurusan sk) {
		return new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				EventListener eventListener = (EventListener) Common
						.cetakDataCustomButton(Kegiatan.class, new DataCriteriaWithColumn() {
							@Override
							public Object[] initCriteria(boolean order) {
								try {
									Criteria criteria = HibernateUtil.currentSession().createCriteria(Kegiatan.class)
											.add(Restrictions.eq("aktif", true))
											.add(s == null ? Restrictions.sqlRestriction("1=1")
													: s.equalsIgnoreCase(Perkuliahan.GENAP)
															? Restrictions.in("semster", Common.genap)
															: Restrictions.in("semster", Common.ganjil))
											.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("jurusan", sk))
											.add(Restrictions.isNotNull("statusAwalMahasiswa"))
											.add(Restrictions.isNotNull("jenisKegiatan"))
											.add(c != null && !c.isEmpty() ? Restrictions.eq("tahunAkademik", c)
													: Restrictions.sqlRestriction("1=1"))
											.add(Restrictions.eq("statusAwalMahasiswa", statusAwal))
											.add(Restrictions.eq("jenisKegiatan", jk)).addOrder(Order.asc("kode"))
											.addOrder(Order.asc("id"));

									if ("tagihan".equals(type))
										criteria.add(Restrictions.or(Restrictions.gt("tagihan", 0.1),
												Restrictions.lt("tagihan", -0.1)));
									else if ("dibayar".equals(type))
										criteria.add(Restrictions.or(Restrictions.gt("dibayar", 0.1),
												Restrictions.lt("dibayar", -0.1)));
									else if ("piutang".equals(type))
										criteria.add(Restrictions.sqlRestriction(
												"(coalesce({alias}.tagihan,0)-coalesce({alias}.dibayar,0)) > 0.1"));

									String[] headerArr = getHeaderAction(jk);
									return new Object[] { criteria, headerArr };
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
								return null;
							}
						}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
								getBlankArray(100))
						.getAttribute("eventListener");
				eventListener.onEvent(null);
			}
		};
	}

	// Helper Pop-up untuk klik data Program
	private EventListener createPopupListenerProgram(final String program, final JenisKegiatan jk, final String type,
			final String c, final String s, final Jurusan sk) {
		return new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				EventListener eventListener = (EventListener) Common
						.cetakDataCustomButton(Kegiatan.class, new DataCriteriaWithColumn() {
							@Override
							public Object[] initCriteria(boolean order) {
								try {
									Criteria criteria = HibernateUtil.currentSession().createCriteria(Kegiatan.class)
											.add(Restrictions.eq("aktif", true))
											.add(s == null ? Restrictions.sqlRestriction("1=1")
													: s.equalsIgnoreCase(Perkuliahan.GENAP)
															? Restrictions.in("semster", Common.genap)
															: Restrictions.in("semster", Common.ganjil))
											.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("jurusan", sk))
											.add(Restrictions.isNotNull("program"))
											.add(Restrictions.isNotNull("jenisKegiatan"))
											.add(c != null && !c.isEmpty() ? Restrictions.eq("tahunAkademik", c)
													: Restrictions.sqlRestriction("1=1"))
											.add(Restrictions.eq("program", program))
											.add(Restrictions.eq("jenisKegiatan", jk)).addOrder(Order.asc("kode"))
											.addOrder(Order.asc("id"));

									if ("tagihan".equals(type))
										criteria.add(Restrictions.or(Restrictions.gt("tagihan", 0.1),
												Restrictions.lt("tagihan", -0.1)));
									else if ("dibayar".equals(type))
										criteria.add(Restrictions.or(Restrictions.gt("dibayar", 0.1),
												Restrictions.lt("dibayar", -0.1)));
									else if ("piutang".equals(type))
										criteria.add(Restrictions.sqlRestriction(
												"(coalesce({alias}.tagihan,0)-coalesce({alias}.dibayar,0)) > 0.1"));

									String[] headerArr = getHeaderAction(jk);
									return new Object[] { criteria, headerArr };
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
								return null;
							}
						}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
								getBlankArray(100))
						.getAttribute("eventListener");
				eventListener.onEvent(null);
			}
		};
	}

	// Helper Pop-up untuk klik data Validator
	private EventListener createPopupListenerValidator(final String validator, final JenisKegiatan jk,
			final String type, final String c, final String s, final Jurusan sk) {
		return new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				EventListener eventListener = (EventListener) Common
						.cetakDataCustomButton(Kegiatan.class, new DataCriteriaWithColumn() {
							@Override
							public Object[] initCriteria(boolean order) {
								try {
									Criteria criteria = HibernateUtil.currentSession().createCriteria(Kegiatan.class)
											.add(Restrictions.eq("aktif", true))
											.add(s == null ? Restrictions.sqlRestriction("1=1")
													: s.equalsIgnoreCase(Perkuliahan.GENAP)
															? Restrictions.in("semster", Common.genap)
															: Restrictions.in("semster", Common.ganjil))
											.add(sk == null || sk.getId() == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("jurusan", sk))
											.add(Restrictions.isNotNull("validator"))
											.add(Restrictions.isNotNull("jenisKegiatan"))
											.add(c != null && !c.isEmpty() ? Restrictions.eq("tahunAkademik", c)
													: Restrictions.sqlRestriction("1=1"))
											.add(Restrictions.eq("validator", validator))
											.add(Restrictions.eq("jenisKegiatan", jk)).addOrder(Order.asc("kode"))
											.addOrder(Order.asc("id"));

									if ("tagihan".equals(type))
										criteria.add(Restrictions.or(Restrictions.gt("tagihan", 0.1),
												Restrictions.lt("tagihan", -0.1)));
									else if ("dibayar".equals(type))
										criteria.add(Restrictions.or(Restrictions.gt("dibayar", 0.1),
												Restrictions.lt("dibayar", -0.1)));
									else if ("piutang".equals(type))
										criteria.add(Restrictions.sqlRestriction(
												"(coalesce({alias}.tagihan,0)-coalesce({alias}.dibayar,0)) > 0.1"));

									String[] headerArr = getHeaderAction(jk);
									return new Object[] { criteria, headerArr };
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
								return null;
							}
						}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
								getBlankArray(100))
						.getAttribute("eventListener");
				eventListener.onEvent(null);
			}
		};
	}

	private String[] getHeaderAction(JenisKegiatan jenisKegiatan) {
		String[] sArr = KegiatanAction.DATA;
		if (jenisKegiatan != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
				&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
			sArr = KegiatanAction.DATA_CALON;
		} else if (jenisKegiatan != null && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
				&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
			sArr = KegiatanAction.DATA_CALON;
		} else if (jenisKegiatan != null) {
			sArr = KegiatanAction.DATA_MHS;
		}
		return sArr;
	}

	private String[] getBlankArray(int size) {
		String[] arr = new String[size];
		for (int i = 0; i < size; i++)
			arr[i] = "";
		return arr;
	}

	@SuppressWarnings("resource")
	private void downloadExcelDashboard(List<Object[]> kegiatans, String title, String row1Title) {
		if (kegiatans == null || kegiatans.isEmpty())
			return;

		try {
			String filename = Executions.getCurrent().getDesktop().getWebApp()
					.getRealPath("/tmp/dashboard_piutang_" + System.currentTimeMillis() + ".xlsx");
			File file = new File(filename);
			file.createNewFile();

			XSSFWorkbook workbook = new XSSFWorkbook();

			// ---------------- SHEET 1: DATA TABEL ----------------
			XSSFSheet sheet = workbook.createSheet("DATA " + title.toUpperCase());
			sheet.setDefaultColumnWidth(20);

			XSSFRow rowhead = sheet.createRow(0);
			rowhead.createCell(0).setCellValue(row1Title.toUpperCase());
			rowhead.createCell(1).setCellValue("JENIS PEMBAYARAN");
			rowhead.createCell(2).setCellValue("TAGIHAN (Rp)");
			rowhead.createCell(3).setCellValue("DIBAYAR (Rp)");
			rowhead.createCell(4).setCellValue("PIUTANG (Rp)");
			rowhead.createCell(5).setCellValue("PERSENTASE LUNAS (%)");

			int r = 1;
			double tTagihan = 0, tDibayar = 0, tPiutang = 0;
			DecimalFormat df = new DecimalFormat("#.##");

			// Untuk keperluan dashboard analitik
			Map<String, Double> mapTunggakan = new HashMap<String, Double>();

			for (Object[] kegiatan : kegiatans) {
				Object objRow1 = kegiatan[0];
				String valRow1 = objRow1 instanceof Jurusan ? ((Jurusan) objRow1).getNama()
						: (objRow1 instanceof StatusAwalMahasiswa ? ((StatusAwalMahasiswa) objRow1).getNama()
								: String.valueOf(objRow1));
				String valRow2 = ((JenisKegiatan) kegiatan[1]).getNama();

				double tagihan = kegiatan[2] != null ? ((Number) kegiatan[2]).doubleValue() : 0.0;
				double dibayar = kegiatan[3] != null ? ((Number) kegiatan[3]).doubleValue() : 0.0;
				double piutang = tagihan - dibayar;

				if (tagihan > 0.01) {
					XSSFRow row = sheet.createRow(r++);
					row.createCell(0).setCellValue(valRow1);
					row.createCell(1).setCellValue(valRow2);
					row.createCell(2).setCellValue(tagihan);
					row.createCell(3).setCellValue(dibayar);
					row.createCell(4).setCellValue(piutang);
					row.createCell(5).setCellValue(df.format((dibayar * 100.0) / tagihan) + "%");

					tTagihan += tagihan;
					tDibayar += dibayar;
					tPiutang += piutang;

					Double currentTunggakan = mapTunggakan.get(valRow1);
					mapTunggakan.put(valRow1, (currentTunggakan == null ? 0 : currentTunggakan) + piutang);
				}
			}

			XSSFRow rowfoot = sheet.createRow(r);
			rowfoot.createCell(1).setCellValue("TOTAL KESELURUHAN");
			rowfoot.createCell(2).setCellValue(tTagihan);
			rowfoot.createCell(3).setCellValue(tDibayar);
			rowfoot.createCell(4).setCellValue(tPiutang);
			rowfoot.createCell(5).setCellValue(tTagihan > 0 ? df.format((tDibayar * 100.0) / tTagihan) + "%" : "0%");

			// ---------------- SHEET 2: DASHBOARD ANALITIK (TOP 5) ----------------
			XSSFSheet dashSheet = workbook.createSheet("DASHBOARD ANALITIK");
			dashSheet.setDefaultColumnWidth(30);

			XSSFCellStyle headerStyle = workbook.createCellStyle();
			XSSFFont fontBold = workbook.createFont();
			fontBold.setBold(true);
			headerStyle.setFont(fontBold);
			headerStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(200, 220, 240)));

			XSSFRow dHead = dashSheet.createRow(0);
			XSSFCell c0 = dHead.createCell(0);
			c0.setCellValue("REKAPITULASI TOP 5 PIUTANG/TUNGGAKAN TERTINGGI");
			c0.setCellStyle(headerStyle);
			XSSFCell c1 = dHead.createCell(1);
			c1.setCellValue("TOTAL PIUTANG (Rp)");
			c1.setCellStyle(headerStyle);
			XSSFCell c2 = dHead.createCell(2);
			c2.setCellValue("TREN VISUAL");
			c2.setCellStyle(headerStyle);

			List<Map.Entry<String, Double>> listTunggakan = new ArrayList<Map.Entry<String, Double>>(
					mapTunggakan.entrySet());
			Collections.sort(listTunggakan, new Comparator<Map.Entry<String, Double>>() {
				public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
					return Double.compare(o2.getValue(), o1.getValue());
				}
			});

			double maxTunggakan = listTunggakan.isEmpty() ? 0 : listTunggakan.get(0).getValue();
			int topCount = 1;
			for (Map.Entry<String, Double> entry : listTunggakan) {
				if (topCount > 5)
					break;
				if (entry.getValue() <= 0)
					continue;

				XSSFRow dRow = dashSheet.createRow(topCount);
				dRow.createCell(0).setCellValue(entry.getKey());
				dRow.createCell(1).setCellValue(entry.getValue());

				double persenGrafik = maxTunggakan > 0 ? (entry.getValue() * 100.0) / maxTunggakan : 0;
				int barLength = (int) (persenGrafik / 5);
				StringBuilder bar = new StringBuilder();
				for (int i = 0; i < barLength; i++) {
					bar.append("█");
				}
				dRow.createCell(2).setCellValue(bar.toString());

				topCount++;
			}

			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
			fileOut.close();

			Filedownload.save(new FileInputStream(file),
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
					title.replace(" ", "_") + ".xlsx");

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static class HtmlCategoryModel {
		private List<HtmlCategoryRow> rows = new ArrayList<HtmlCategoryRow>();

		public void clear() {
			rows.clear();
		}

		public void setValue(String series, Object category, Object value) {
			HtmlCategoryRow row = new HtmlCategoryRow();
			row.series = series == null ? "" : series;
			row.category = category == null ? "" : String.valueOf(category);
			row.value = toDoubleDashboardValue(value);
			rows.add(row);
		}

		public List<HtmlCategoryRow> getRows() {
			return rows;
		}
	}

	private static class HtmlCategoryRow {
		String series;
		String category;
		double value;
	}

	private String buildModernChartHtml(String title, HtmlCategoryModel model, String description) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='width:100%;box-sizing:border-box;padding:14px;border:1px solid #e2e8f0;border-radius:16px;background:#ffffff;box-shadow:0 8px 18px rgba(15,23,42,.06);'>");
		sb.append("<div style='font-size:14px;font-weight:900;color:#0f172a;margin-bottom:6px;'>").append(escapeDashboardHtml(title)).append("</div>");
		if (description != null && description.trim().length() > 0) {
			sb.append("<div style='font-size:11px;color:#64748b;line-height:1.55;margin-bottom:10px;'>").append(escapeDashboardHtml(description)).append("</div>");
		}
		if (model == null || model.getRows() == null || model.getRows().isEmpty()) {
			sb.append("<div style='padding:12px;border-radius:12px;background:#f8fafc;color:#64748b;font-size:12px;'>Belum ada data yang dapat ditampilkan.</div></div>");
			return sb.toString();
		}
		double max = 0.0d;
		for (int i = 0; i < model.getRows().size(); i++) {
			HtmlCategoryRow r = (HtmlCategoryRow) model.getRows().get(i);
			if (r != null && r.value > max) {
				max = r.value;
			}
		}
		if (max <= 0.0d) {
			max = 1.0d;
		}
		sb.append("<div style='display:flex;flex-direction:column;gap:7px;'>");
		for (int i = 0; i < model.getRows().size(); i++) {
			HtmlCategoryRow r = (HtmlCategoryRow) model.getRows().get(i);
			if (r == null) {
				continue;
			}
			int width = (int) Math.round((r.value * 100.0d) / max);
			if (width < 2 && r.value > 0.0d) {
				width = 2;
			}
			sb.append("<div style='display:grid;grid-template-columns:minmax(95px,210px) 1fr minmax(70px,120px);gap:8px;align-items:center;'>");
			sb.append("<div style='font-size:11px;color:#334155;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;'>").append(escapeDashboardHtml(r.category)).append("</div>");
			sb.append("<div style='height:14px;border-radius:999px;background:#e2e8f0;overflow:hidden;'><div style='height:14px;width:").append(width)
					.append("%;border-radius:999px;background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div></div>");
			sb.append("<div style='font-size:11px;color:#0f172a;font-weight:900;text-align:right;'>").append(formatDashboardNumber(r.value)).append("</div>");
			sb.append("</div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private static double toDoubleDashboardValue(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return value == null ? 0.0d : Double.parseDouble(String.valueOf(value));
		} catch (Exception e) {
			return 0.0d;
		}
	}

	private static String formatDashboardNumber(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(Math.round(value));
		}
	}

	private static String escapeDashboardHtml(Object value) {
		String text = value == null ? "" : String.valueOf(value);
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}


}
