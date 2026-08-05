package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Html;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.NilaiHurufSekolah;
import ais.database.model.sekolah.Siswa;
import ais.action.master.sekolah.util.GrupPenilaianUtil;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * "Dasbor Studi Siswa" — versi siswa (sekolah) dari {@code TampilStudiMahasiswaHelper}. Dibuka
 * dari tombol per-baris pada daftar Siswa. Jendela bertab: <b>Dasbor</b> (ringkasan + grafik
 * Distribusi Nilai &amp; Rekap Kehadiran + Riwayat Nilai per Semester + Cetak Laporan grafik &amp;
 * Export Excel), <b>Absensi</b>, dan satu tab per-<b>semester</b> (Ganjil/Genap; TIDAK ada Semester
 * Pendek untuk siswa).
 *
 * <p><b>Nilai siswa dihitung, bukan tersimpan.</b> Tidak ada baris "nilai akhir per mapel"; nilai
 * dihitung dari {@link KelasSiswaPunyaSiswa#retreiveTotalNilaiTotal} per mata pelajaran per grup
 * penilaian per semester (pola sama dengan LaporanRaporSiswa). Semua query memakai session dedikasi
 * dan dibungkus try/catch agar kegagalan satu mapel tidak merusak dasbor.
 */
public final class TampilStudiSiswaHelper {

	private TampilStudiSiswaHelper() {
	}

	// ============================ JENDELA + TAB ============================

	public static void tampil(final Siswa siswa, final DataLoader dataLoader) throws Exception {
		if (siswa == null) {
			return;
		}
		final MyWindow window = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		Common.clear(window);
		window.setWidth("98%");
		window.setHeight("98%");
		window.setTitle("Dasbor Studi Siswa - " + (siswa.getNama() == null ? "" : siswa.getNama()));

		Tabbox tabbox = new Tabbox();
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		tabbox.setParent(Common.tampilanScrollTabbox(window));

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		// Tab Dasbor
		MyTabConfig tabDasbor = new MyTabConfig("Dasbor");
		tabDasbor.setParent(tabs);
		final Tabpanel panelDasbor = new ais.ui.util.MyTabpanel();
		panelDasbor.setParent(tabpanels);

		// Tab Absensi
		MyTabConfig tabAbsensi = new MyTabConfig("Absensi");
		tabAbsensi.setParent(tabs);
		final Tabpanel panelAbsensi = new ais.ui.util.MyTabpanel();
		panelAbsensi.setParent(tabpanels);
		tabAbsensi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (panelAbsensi.getChildren().isEmpty()) {
					new Html(buildKehadiranHtml(siswa)).setParent(panelAbsensi);
				}
			}
		});

		// Tab per-semester (Ganjil/Genap tiap tahun ajaran) — TANPA SP.
		for (final String[] smt : enumSemesterSiswa(siswa)) {
			// smt = {tahunAjaran, semesterLabel}
			MyTabConfig tabSmt = new MyTabConfig(smt[0] + " " + smt[1]);
			tabSmt.setParent(tabs);
			final Tabpanel panelSmt = new ais.ui.util.MyTabpanel();
			panelSmt.setParent(tabpanels);
			tabSmt.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (panelSmt.getChildren().isEmpty()) {
						new Html(buildRiwayatNilaiHtml(siswa, smt[0], smt[1])).setParent(panelSmt);
					}
				}
			});
		}

		// Muat Dasbor default (tab pertama).
		initDashboardSiswa(panelDasbor, siswa, window, dataLoader);

		window.setVisible(true);
		window.onModal();
	}

	// ============================ TAB DASBOR ============================

	public static void initDashboardSiswa(final Tabpanel parent, final Siswa siswa, final MyWindow window,
			final DataLoader dataLoader) {
		try {
			Common.clear(parent);
			org.zkoss.zul.Borderlayout bl = new ais.ui.util.MyBorderlayout();
			bl.setParent(parent);
			bl.setHeight("100%");
			org.zkoss.zul.Center center = new org.zkoss.zul.Center();
			center.setParent(bl);
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.setAutoscroll(true);

			Vbox isi = new Vbox();
			isi.setWidth("100%");
			isi.setStyle("padding:10px 14px;box-sizing:border-box;gap:12px;");
			isi.setParent(center);

			List<String[]> nilai = dataNilaiSiswa(siswa);
			int[] hadir = dataKehadiranSiswa(siswa);

			// Header ringkas
			new Html(buildHeaderSiswaHtml(siswa, nilai)).setParent(isi);

			// Ringkasan + grafik
			panel(isi, "Distribusi Nilai (Predikat)", buildDistribusiNilaiHtml(nilai));
			panel(isi, "Rekap Kehadiran", buildKehadiranHtmlFromData(hadir));
			panel(isi, "Riwayat Nilai per Semester", buildRiwayatNilaiTabelHtml(nilai));

			// Toolbar bawah: Cetak Laporan (grafik) + Export Excel + Tutup
			org.zkoss.zul.South south = new org.zkoss.zul.South();
			south.setParent(bl);
			Toolbar toolbar = new Toolbar();
			toolbar.setStyle("background:#ffffff;border-top:1px solid #e2e8f0;padding:10px 16px;text-align:right;");
			toolbar.setParent(south);

			final Component owner = isi;
			MyToolbarbuttonConfig btnPdf = new MyToolbarbuttonConfig("Cetak Laporan (Grafik & Tabel)", "/img/print.png");
			btnPdf.setTooltiptext("Buka laporan lengkap ber-grafik + tabel yang siap dicetak / disimpan sebagai PDF.");
			btnPdf.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ais.action.master.helper.DashboardReportKit.bukaLaporan(owner, buildSumberLaporanSiswa(siswa));
				}
			});
			btnPdf.setParent(toolbar);

			MyToolbarbuttonConfig btnExcel = new MyToolbarbuttonConfig("Export Excel", "/img/excel.png");
			btnExcel.setTooltiptext("Unduh data studi siswa (Ringkasan, Nilai per Semester, Kehadiran) ke Excel (.xlsx, sheet terpisah).");
			btnExcel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					exportExcel(siswa);
				}
			});
			btnExcel.setParent(toolbar);

			if (window != null) {
				MyToolbarbuttonConfig btnTutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
				btnTutup.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (dataLoader != null) {
							dataLoader.loadData(event);
						}
						window.detach();
					}
				});
				btnTutup.setParent(toolbar);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit TampilStudiSiswaHelper.initDashboardSiswa");
			new Html("<div style='padding:16px;color:#b91c1c;'>Gagal memuat dasbor siswa: " + esc(e.getMessage())
					+ "</div>").setParent(parent);
		}
	}

	private static void panel(Component parent, String judul, String isiHtml) {
		Vbox p = new Vbox();
		p.setWidth("100%");
		p.setStyle("border:1px solid #e2e8f0;border-radius:14px;background:#fff;box-shadow:0 6px 16px rgba(15,23,42,.05);padding:12px 14px;");
		p.setParent(parent);
		new Html("<div style='font-size:14px;font-weight:800;color:#0f172a;margin-bottom:6px;'>" + esc(judul)
				+ "</div>").setParent(p);
		new Html(isiHtml).setParent(p);
	}

	// ============================ DATA: NILAI ============================

	/**
	 * Hitung nilai per mata pelajaran per semester (Ganjil=1, Genap=2) untuk siswa. Baris:
	 * {tahunAjaran, semesterLabel, mataPelajaran, nilai, kkm, predikat, tuntas}.
	 */
	private static List<String[]> dataNilaiSiswa(Siswa siswa) {
		List<String[]> res = new ArrayList<String[]>();
		if (siswa == null) {
			return res;
		}
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			java.util.Date sekarang = WaktuUtil.getDate();

			@SuppressWarnings("unchecked")
			List<KelasSiswaPunyaSiswa> enrolments = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.createAlias("siswa", "siswa").createAlias("kelasSiswa", "kelasSiswa")
					.add(Restrictions.eq("siswa.id", siswa.getId()))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
					.addOrder(Order.asc("kelasSiswa.tingkat")).addOrder(Order.asc("kelasSiswa.tahunAjaran")).list();

			for (KelasSiswaPunyaSiswa ksp : enrolments) {
				try {
					KelasSiswa kelas = ksp.getKelasSiswa();
					if (kelas == null || kelas.getKurikulumSekolah() == null) {
						continue;
					}
					String ta = kelas.getTahunAjaran();
					List<Long> excludeMk = null;
					try {
						excludeMk = ksp.ambilMk();
					} catch (Exception ig) {
						excludeMk = null;
					}

					@SuppressWarnings("unchecked")
					List<KurikulumPunyaMatapelajaran> kpms = session.createCriteria(KurikulumPunyaMatapelajaran.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
							.add(Restrictions.eq("kurikulumSekolah", kelas.getKurikulumSekolah()))
							.createAlias("matapelajaran", "matapelajaran")
							.add(excludeMk == null || excludeMk.isEmpty() ? Restrictions.sqlRestriction("1=1")
									: Restrictions.not(Restrictions.in("matapelajaran.id", excludeMk)))
							.addOrder(Order.asc("matapelajaran.urutan")).list();

					for (KurikulumPunyaMatapelajaran kpm : kpms) {
						Matapelajaran mp = kpm.getMatapelajaran();
						if (mp == null) {
							continue;
						}
						JenisPenilaian jp = mp.getJenisPenilaian();
						if (kpm.getKurikulumSekolah() != null && kpm.getKurikulumSekolah().getJenisPenilaian() != null) {
							jp = kpm.getKurikulumSekolah().getJenisPenilaian();
						}
						if (jp == null) {
							continue;
						}
						@SuppressWarnings("unchecked")
						List<GrupPenilaian> grups = ConstantValues.simpleList(
								session.createCriteria(DetailJenisPenilaian.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", Boolean.TRUE)))
										.add(Restrictions.eq("jenisPenilaian", jp))
										.setProjection(Projections.groupProperty("grupPenilaian.id")),
								GrupPenilaian.class, false);

						for (int smt = 1; smt <= 2; smt++) {
							try {
								double totalSemua = 0.0, jumlahSemua = 0.0;
								// jenisNilaiHuruf grup dipakai untuk resolusi predikat (SAMA seperti
								// LaporanRaporSiswa @1381 yang memakai gp.getJenisNilaiHuruf(); memakai null
								// dulu keliru karena rentang huruf didefinisikan per JenisNilaiHuruf).
								ais.database.model.sekolah.JenisNilaiHuruf jnh = null;
								for (GrupPenilaian gp : grups) {
									if (gp == null) {
										continue;
									}
									if (jnh == null) {
										try {
											jnh = gp.getJenisNilaiHuruf();
										} catch (Exception igJ) {
											jnh = null;
										}
									}
									@SuppressWarnings("unchecked")
									List<GrupKategoriItemPenilaianSiswa> gKats = ConstantValues.simpleList(
											session.createCriteria(DetailGrupPenilaian.class)
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", Boolean.TRUE)))
													.add(Restrictions.isNotNull("grupKategoriItemPenilaianSiswa"))
													.add(Restrictions.eq("grupPenilaian", gp))
													.setProjection(Projections.groupProperty(
															"grupKategoriItemPenilaianSiswa.id")),
											GrupKategoriItemPenilaianSiswa.class, false);
									double t = 0.0;
									try {
										String target = GrupPenilaianUtil.ambilTarget(gp.getFormula(), sekarang);
										Double d = ksp.retreiveTotalNilaiTotal(target, mp, gp, Integer.valueOf(smt), gKats);
										t = d == null ? 0.0 : d.doubleValue();
									} catch (Exception exG) {
										t = 0.0;
									}
									totalSemua += t;
									jumlahSemua += 1.0;
								}
								double nilaiAngka = jumlahSemua > 0 ? totalSemua / jumlahSemua : 0.0;
								// Lewati baris yang benar-benar kosong (tak ada nilai sama sekali).
								if (nilaiAngka <= 0) {
									continue;
								}
								Double kkm = mp.getKkm();
								String semLabel = smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
								String predikat = "-";
								try {
									NilaiHurufSekolah nhs = NilaiHurufSekolah.getNilaiHurufSekolah(
											Double.valueOf(nilaiAngka), siswa.getTahunMasuk(), siswa.getSekolah(),
											siswa.getYayasan(), ta, semLabel, jnh);
									if (nhs != null && nhs.getNilaiHuruf() != null) {
										predikat = nhs.getNilaiHuruf();
									}
								} catch (Exception exH) {
									predikat = "-";
								}
								boolean tuntas = kkm != null && nilaiAngka >= kkm.doubleValue();
								res.add(new String[] { ta == null ? "-" : ta, semLabel,
										mp.getNama() == null ? "-" : mp.getNama(), fmt2(nilaiAngka),
										kkm == null ? "-" : fmt2(kkm.doubleValue()), predikat, tuntas ? "Tuntas" : "Belum" });
							} catch (Exception exSmt) {
								ais.common.ErrorAuditUtil.record(exSmt, "auto-audit(smt) TampilStudiSiswaHelper.dataNilaiSiswa");
							}
						}
					}
				} catch (Exception exKsp) {
					ais.common.ErrorAuditUtil.record(exKsp, "auto-audit(ksp) TampilStudiSiswaHelper.dataNilaiSiswa");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit TampilStudiSiswaHelper.dataNilaiSiswa");
		} finally {
			tutup(session);
		}
		return res;
	}

	/** Daftar (tahunAjaran, semesterLabel) unik yang punya nilai — untuk tab per-semester. */
	private static List<String[]> enumSemesterSiswa(Siswa siswa) {
		List<String[]> res = new ArrayList<String[]>();
		java.util.LinkedHashMap<String, String[]> uniq = new java.util.LinkedHashMap<String, String[]>();
		for (String[] r : dataNilaiSiswa(siswa)) {
			String key = r[0] + "|" + r[1];
			if (!uniq.containsKey(key)) {
				uniq.put(key, new String[] { r[0], r[1] });
			}
		}
		res.addAll(uniq.values());
		return res;
	}

	// ============================ DATA: KEHADIRAN ============================

	/** Rekap kehadiran siswa: {hadir, izin, sakit, alpa, total}. */
	private static int[] dataKehadiranSiswa(Siswa siswa) {
		int[] r = new int[] { 0, 0, 0, 0, 0 };
		if (siswa == null) {
			return r;
		}
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			List<Long> kelasIds = null;
			try {
				kelasIds = siswa.ambilkelas();
			} catch (Exception ig) {
				kelasIds = null;
			}
			if (kelasIds != null && !kelasIds.isEmpty()) {
				@SuppressWarnings("unchecked")
				List<String> abs = session.createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
						.createAlias("jadwalPelajaran", "jp")
						.createAlias("jp.kelas", "kl", Criteria.LEFT_JOIN).add(Restrictions.in("kl.id", kelasIds))
						.add(Restrictions.isNotNull("absensi")).setProjection(Projections.property("absensi")).list();
				Map<String, Integer> c = Perkuliahan.hitungStatus(abs, siswa.getId());
				r[0] = c.get("M") == null ? 0 : c.get("M").intValue();
				r[1] = c.get("I") == null ? 0 : c.get("I").intValue();
				r[2] = c.get("S") == null ? 0 : c.get("S").intValue();
				r[3] = c.get("A") == null ? 0 : c.get("A").intValue();
				r[4] = c.get("T") == null ? 0 : c.get("T").intValue();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit TampilStudiSiswaHelper.dataKehadiranSiswa");
		} finally {
			tutup(session);
		}
		return r;
	}

	// ============================ HTML BUILDERS ============================

	private static String buildHeaderSiswaHtml(Siswa siswa, List<String[]> nilai) {
		double total = 0;
		int n = 0, tuntas = 0;
		for (String[] r : nilai) {
			double v = parseD(r[3]);
			total += v;
			n++;
			if ("Tuntas".equals(r[6])) {
				tuntas++;
			}
		}
		String rata = n > 0 ? fmt2(total / n) : "-";
		StringBuilder h = new StringBuilder();
		h.append("<div style='background:linear-gradient(135deg,#0f766e,#0e7490);color:#fff;border-radius:16px;padding:16px 18px;'>");
		h.append("<div style='font-size:12px;opacity:.85;'>DASBOR STUDI SISWA</div>");
		h.append("<div style='font-size:20px;font-weight:900;'>").append(esc(siswa.getNama())).append("</div>");
		h.append("<div style='font-size:12px;opacity:.9;margin-top:2px;'>NIS: ")
				.append(esc(siswa.getNomorInduk() == null ? "-" : siswa.getNomorInduk())).append(" &nbsp;|&nbsp; NISN: ")
				.append(esc(siswa.getNomorIndukNasional() == null ? "-" : siswa.getNomorIndukNasional()))
				.append(" &nbsp;|&nbsp; Sekolah: ")
				.append(esc(siswa.getSekolah() == null || siswa.getSekolah().getNama() == null ? "-"
						: siswa.getSekolah().getNama()))
				.append("</div>");
		h.append("<div style='margin-top:8px;display:flex;gap:10px;flex-wrap:wrap;'>");
		h.append(kotak("Total Nilai Mapel", String.valueOf(n)));
		h.append(kotak("Rata-rata Nilai", rata));
		h.append(kotak("Mapel Tuntas", tuntas + " / " + n));
		h.append("</div></div>");
		return h.toString();
	}

	private static String kotak(String label, String nilai) {
		return "<div style='background:rgba(255,255,255,.15);border-radius:10px;padding:6px 12px;'>"
				+ "<div style='font-size:10px;opacity:.85;'>" + esc(label) + "</div>"
				+ "<div style='font-size:16px;font-weight:800;'>" + esc(nilai) + "</div></div>";
	}

	private static String buildDistribusiNilaiHtml(List<String[]> nilai) {
		LinkedHashMap<String, Integer> peta = new LinkedHashMap<String, Integer>();
		for (String[] r : nilai) {
			String p = r[5] == null || r[5].trim().isEmpty() ? "-" : r[5].trim();
			peta.put(p, Integer.valueOf((peta.get(p) == null ? 0 : peta.get(p).intValue()) + 1));
		}
		if (peta.isEmpty()) {
			return "<div style='padding:10px;color:#64748b;'>Belum ada nilai yang tercatat.</div>";
		}
		String[] palet = { "#16a34a", "#22c55e", "#84cc16", "#eab308", "#f59e0b", "#f97316", "#ef4444", "#64748b" };
		List<String> labels = new ArrayList<String>();
		List<Double> vals = new ArrayList<Double>();
		List<String> colors = new ArrayList<String>();
		int idx = 0, tot = 0;
		for (Map.Entry<String, Integer> e : peta.entrySet()) {
			labels.add(e.getKey());
			vals.add(Double.valueOf(e.getValue().intValue()));
			colors.add(palet[Math.min(idx, palet.length - 1)]);
			tot += e.getValue().intValue();
			idx++;
		}
		double[] v = new double[vals.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = vals.get(i).doubleValue();
		}
		return HtmlChartHelper.donut("Distribusi Predikat Nilai",
				"Sebaran predikat/huruf nilai dari seluruh mata pelajaran yang tercatat.",
				labels.toArray(new String[labels.size()]), v, colors.toArray(new String[colors.size()]), tot + " nilai");
	}

	private static String buildKehadiranHtml(Siswa siswa) {
		return buildKehadiranHtmlFromData(dataKehadiranSiswa(siswa));
	}

	private static String buildKehadiranHtmlFromData(int[] k) {
		int total = k[4] > 0 ? k[4] : (k[0] + k[1] + k[2] + k[3]);
		if (total == 0) {
			return "<div style='padding:10px;color:#64748b;'>Belum ada rekaman kehadiran untuk siswa ini.</div>";
		}
		double persen = k[0] * 100.0 / total;
		StringBuilder h = new StringBuilder();
		h.append(HtmlChartHelper.donut("Rekap Kehadiran",
				"Proporsi kehadiran siswa: Hadir, Izin, Sakit, dan Alpa (tanpa keterangan) dari seluruh pertemuan tercatat.",
				new String[] { "Hadir", "Izin", "Sakit", "Alpa" }, new double[] { k[0], k[1], k[2], k[3] },
				new String[] { "#16a34a", "#0ea5e9", "#f59e0b", "#dc2626" }, fmt2(persen) + "%"));
		h.append("<div style='margin-top:8px;font-size:12px;color:#334155;'>Total pertemuan: <b>").append(total)
				.append("</b> — Hadir <b style='color:#16a34a'>").append(k[0]).append("</b>, Izin <b>").append(k[1])
				.append("</b>, Sakit <b>").append(k[2]).append("</b>, Alpa <b style='color:#dc2626'>").append(k[3])
				.append("</b>.</div>");
		return h.toString();
	}

	private static String buildRiwayatNilaiTabelHtml(List<String[]> nilai) {
		if (nilai.isEmpty()) {
			return "<div style='padding:10px;color:#64748b;'>Belum ada nilai yang tercatat untuk siswa ini.</div>";
		}
		StringBuilder h = new StringBuilder();
		h.append("<div style='overflow-x:auto;'><table style='width:100%;border-collapse:collapse;font-size:12px;'>");
		h.append("<thead><tr style='background:#f8fafc;color:#334155;'>").append(th("No")).append(th("Tahun Ajaran"))
				.append(th("Semester")).append(th("Mata Pelajaran")).append(th("Nilai")).append(th("KKM"))
				.append(th("Predikat")).append(th("Status")).append("</tr></thead><tbody>");
		int no = 0;
		for (String[] r : nilai) {
			no++;
			String warna = "Tuntas".equals(r[6]) ? "#16a34a" : "#dc2626";
			h.append("<tr>").append(td(String.valueOf(no), true)).append(td(r[0], true)).append(td(r[1], true))
					.append(td(r[2], false)).append(tdColor(r[3], "#0f172a")).append(td(r[4], true))
					.append(tdColor(r[5], "#1e40af")).append(tdColor(r[6], warna)).append("</tr>");
		}
		h.append("</tbody></table></div>");
		return h.toString();
	}

	/** Tabel nilai untuk satu (tahunAjaran, semester) — dipakai tab per-semester. */
	private static String buildRiwayatNilaiHtml(Siswa siswa, String ta, String semLabel) {
		List<String[]> semua = dataNilaiSiswa(siswa);
		List<String[]> filter = new ArrayList<String[]>();
		for (String[] r : semua) {
			if (r[0].equals(ta) && r[1].equals(semLabel)) {
				filter.add(r);
			}
		}
		StringBuilder h = new StringBuilder();
		h.append("<div style='padding:12px 14px;'>");
		h.append("<div style='font-size:15px;font-weight:800;color:#0f172a;margin-bottom:8px;'>Nilai ")
				.append(esc(ta)).append(" — Semester ").append(esc(semLabel)).append("</div>");
		if (filter.isEmpty()) {
			h.append("<div style='padding:10px;color:#64748b;'>Belum ada nilai pada semester ini.</div>");
		} else {
			h.append("<div style='overflow-x:auto;'><table style='width:100%;border-collapse:collapse;font-size:12px;'>");
			h.append("<thead><tr style='background:#f8fafc;color:#334155;'>").append(th("No")).append(th("Mata Pelajaran"))
					.append(th("Nilai")).append(th("KKM")).append(th("Predikat")).append(th("Status"))
					.append("</tr></thead><tbody>");
			int no = 0;
			for (String[] r : filter) {
				no++;
				String warna = "Tuntas".equals(r[6]) ? "#16a34a" : "#dc2626";
				h.append("<tr>").append(td(String.valueOf(no), true)).append(td(r[2], false))
						.append(tdColor(r[3], "#0f172a")).append(td(r[4], true)).append(tdColor(r[5], "#1e40af"))
						.append(tdColor(r[6], warna)).append("</tr>");
			}
			h.append("</tbody></table></div>");
		}
		h.append("</div>");
		return h.toString();
	}

	// ============================ CETAK LAPORAN (grafik + tabel) ============================

	private static ais.action.master.helper.DashboardReportKit.SumberLaporan buildSumberLaporanSiswa(final Siswa siswa) {
		final String nama = siswa == null || siswa.getNama() == null ? "-" : siswa.getNama();
		final String nis = siswa == null || siswa.getNomorInduk() == null ? "-" : siswa.getNomorInduk();
		return new ais.action.master.helper.DashboardReportKit.SumberLaporan() {
			public String judul() {
				return "Dasbor Studi Siswa";
			}

			public String subjudul() {
				return nis + " - " + nama;
			}

			public String deskripsi() {
				return "Ringkasan riwayat pembelajaran siswa: distribusi nilai, kehadiran, dan nilai per semester.";
			}

			public List<ais.action.master.helper.DashboardReportKit.Bagian> bagian() {
				List<ais.action.master.helper.DashboardReportKit.Bagian> list = new ArrayList<ais.action.master.helper.DashboardReportKit.Bagian>();
				list.add(ais.action.master.helper.DashboardReportKit.donut("Distribusi Predikat Nilai",
						"Sebaran predikat nilai seluruh mapel.", new String[] { "Predikat", "Jumlah" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							public List<Object[]> ambil() {
								LinkedHashMap<String, Integer> peta = new LinkedHashMap<String, Integer>();
								for (String[] r : dataNilaiSiswa(siswa)) {
									String p = r[5] == null || r[5].trim().isEmpty() ? "-" : r[5].trim();
									peta.put(p, Integer.valueOf((peta.get(p) == null ? 0 : peta.get(p).intValue()) + 1));
								}
								List<Object[]> o = new ArrayList<Object[]>();
								for (Map.Entry<String, Integer> e : peta.entrySet()) {
									o.add(new Object[] { e.getKey(), e.getValue() });
								}
								return o;
							}
						}));
				list.add(ais.action.master.helper.DashboardReportKit.donut("Rekap Kehadiran",
						"Hadir / Izin / Sakit / Alpa.", new String[] { "Kategori", "Jumlah" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							public List<Object[]> ambil() {
								int[] k = dataKehadiranSiswa(siswa);
								List<Object[]> o = new ArrayList<Object[]>();
								o.add(new Object[] { "Hadir", Integer.valueOf(k[0]) });
								o.add(new Object[] { "Izin", Integer.valueOf(k[1]) });
								o.add(new Object[] { "Sakit", Integer.valueOf(k[2]) });
								o.add(new Object[] { "Alpa", Integer.valueOf(k[3]) });
								return o;
							}
						}));
				list.add(ais.action.master.helper.DashboardReportKit.tabel("Riwayat Nilai per Semester",
						"Nilai per mata pelajaran per semester.",
						new String[] { "Tahun Ajaran", "Semester", "Mata Pelajaran", "Nilai", "KKM", "Predikat", "Status" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							public List<Object[]> ambil() {
								List<Object[]> o = new ArrayList<Object[]>();
								for (String[] r : dataNilaiSiswa(siswa)) {
									o.add(r);
								}
								return o;
							}
						}));
				return list;
			}
		};
	}

	// ============================ EXPORT EXCEL ============================

	private static void exportExcel(Siswa siswa) {
		try {
			org.zkoss.poi.xssf.usermodel.XSSFWorkbook wb = new org.zkoss.poi.xssf.usermodel.XSSFWorkbook();

			org.zkoss.poi.xssf.usermodel.XSSFSheet s0 = wb.createSheet("Ringkasan");
			xlsBaris(s0, 0, "Nama", siswa == null ? "" : nz(siswa.getNama()));
			xlsBaris(s0, 1, "NIS", siswa == null ? "" : nz(siswa.getNomorInduk()));
			xlsBaris(s0, 2, "NISN", siswa == null ? "" : nz(siswa.getNomorIndukNasional()));
			xlsBaris(s0, 3, "Sekolah",
					siswa == null || siswa.getSekolah() == null ? "" : nz(siswa.getSekolah().getNama()));

			org.zkoss.poi.xssf.usermodel.XSSFSheet s1 = wb.createSheet("Nilai per Semester");
			xlsHeader(s1, new String[] { "Tahun Ajaran", "Semester", "Mata Pelajaran", "Nilai", "KKM", "Predikat",
					"Status" });
			int r1 = 1;
			for (String[] r : dataNilaiSiswa(siswa)) {
				org.zkoss.poi.xssf.usermodel.XSSFRow row = s1.createRow(r1++);
				for (int i = 0; i < r.length; i++) {
					row.createCell(i).setCellValue(r[i] == null ? "" : r[i]);
				}
			}

			int[] k = dataKehadiranSiswa(siswa);
			org.zkoss.poi.xssf.usermodel.XSSFSheet s2 = wb.createSheet("Rekap Kehadiran");
			xlsHeader(s2, new String[] { "Kategori", "Jumlah" });
			String[] kl = { "Hadir", "Izin", "Sakit", "Alpa", "Total Pertemuan" };
			for (int i = 0; i < kl.length; i++) {
				org.zkoss.poi.xssf.usermodel.XSSFRow row = s2.createRow(i + 1);
				row.createCell(0).setCellValue(kl[i]);
				row.createCell(1).setCellValue(String.valueOf(k[i]));
			}

			java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
			wb.write(bout);
			bout.close();
			String nis = siswa != null && siswa.getNomorInduk() != null && !siswa.getNomorInduk().trim().isEmpty()
					? siswa.getNomorInduk().trim() : "siswa";
			org.zkoss.zul.Filedownload.save(bout.toByteArray(),
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Studi_Siswa_" + nis + ".xlsx");
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit TampilStudiSiswaHelper.exportExcel");
			try {
				ais.ui.util.MyMessageboxConfig.show("Gagal mengekspor Excel: " + e.getMessage(), "Error",
						ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.ERROR);
			} catch (Exception ig) {
			}
		}
	}

	private static void xlsHeader(org.zkoss.poi.xssf.usermodel.XSSFSheet sheet, String[] cols) {
		org.zkoss.poi.xssf.usermodel.XSSFRow row = sheet.createRow(0);
		for (int i = 0; i < cols.length; i++) {
			row.createCell(i).setCellValue(cols[i]);
		}
	}

	private static void xlsBaris(org.zkoss.poi.xssf.usermodel.XSSFSheet sheet, int r, String label, String val) {
		org.zkoss.poi.xssf.usermodel.XSSFRow row = sheet.createRow(r);
		row.createCell(0).setCellValue(label);
		row.createCell(1).setCellValue(val == null ? "" : val);
	}

	// ============================ UTIL ============================

	private static String th(String s) {
		return "<th style='padding:6px 7px;border-bottom:1px solid #e2e8f0;text-align:left;'>" + esc(s) + "</th>";
	}

	private static String td(String s, boolean center) {
		return "<td style='padding:6px 7px;border-bottom:1px solid #f1f5f9;" + (center ? "text-align:center;" : "")
				+ "'>" + esc(s) + "</td>";
	}

	private static String tdColor(String s, String color) {
		return "<td style='padding:6px 7px;border-bottom:1px solid #f1f5f9;text-align:center;font-weight:700;color:"
				+ color + ";'>" + esc(s) + "</td>";
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}

	private static double parseD(String s) {
		try {
			return s == null || s.trim().isEmpty() || s.trim().equals("-") ? 0
					: Double.parseDouble(s.trim().replace(",", "."));
		} catch (Exception e) {
			return 0;
		}
	}

	private static String fmt2(double d) {
		try {
			return Common.numberFormat.get().format(d);
		} catch (Exception e) {
			return String.valueOf(Math.round(d * 100) / 100.0);
		}
	}

	private static String esc(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private static void tutup(Session session) {
		if (session != null) {
			try {
				session.close();
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TampilStudiSiswaHelper.tutup");
			}
		}
	}
}
