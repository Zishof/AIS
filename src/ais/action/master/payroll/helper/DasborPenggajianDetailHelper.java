package ais.action.master.payroll.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.CommonDashboardHtmlHelper;
import ais.database.model.KehadiranPegawaiBulanan;
import ais.database.model.Pegawai;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.payroll.JenisTransaksiPegawai;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.database.model.payroll.PembayaranItemGajiPegawai;
import ais.database.model.payroll.TransaksiPegawai;

/**
 * <h3>DasborPenggajianDetailHelper — panel rincian penggajian (gaji per kategori,
 * tunjangan &amp; honor per jenis, daftar cuti, daftar perjalanan dinas)</h3>
 *
 * <p>Membangun potongan HTML panel untuk dipasang di {@code DasborAnalisisPenggajian}.
 * SEMUA tunjangan/asuransi/perjalanan dinas/honor dosen direkam sebagai
 * {@link TransaksiPegawai} (dikelompokkan per {@link JenisTransaksiPegawai}); gaji pokok
 * per kategori pegawai berasal dari {@link PembayaranGajiPunyaPegawai}; cuti dari
 * {@link CutiDanIzin}. Tiap angka dapat dibuka rinciannya (HTML &lt;details&gt; bawaan
 * browser) — tanpa komponen ZK tambahan agar ringan.</p>
 *
 * <p>Gaya Java 1.6/1.7 (tanpa lambda/stream/diamond). Tiap query dibatasi tahun (dan
 * rentang tanggal untuk cuti &amp; perjalanan). Baris rincian dibatasi {@link #MAKS_RINCIAN}
 * agar tetap ringan.</p>
 */
public final class DasborPenggajianDetailHelper {

	/** Batas baris rincian yang ditampilkan per kelompok. */
	public static final int MAKS_RINCIAN = 300;

	private DasborPenggajianDetailHelper() {
	}

	// ════════════════════════════════════════════════════════════════════════
	// Util
	// ════════════════════════════════════════════════════════════════════════

	private static double dbl(Object o) {
		return (o instanceof Number) ? ((Number) o).doubleValue() : 0.0;
	}

	private static String rp(double v) {
		return "Rp " + Common.numberFormat.get().format(Math.round(v));
	}

	private static String esc(String v) {
		return CommonDashboardHtmlHelper.escape(v);
	}

	/** Bungkus tabel rincian dalam &lt;details&gt; (collapsible). */
	private static String detailBox(String judul, long jumlah, String isiBaris, String headerKolom) {
		if (isiBaris == null || isiBaris.length() == 0) {
			return "<div style='font-size:11px;color:#94a3b8;padding:4px 0;'>Tidak ada rincian.</div>";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<details style='margin-top:6px;'>");
		sb.append("<summary style='cursor:pointer;font-size:11px;font-weight:700;color:var(--ais-theme-primary,#2563eb);'>")
				.append("Lihat rincian (").append(jumlah).append(")</summary>");
		sb.append("<div style='max-height:280px;overflow:auto;margin-top:6px;'>");
		sb.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>");
		sb.append("<thead><tr style='background:#f1f5f9;'>").append(headerKolom).append("</tr></thead><tbody>");
		sb.append(isiBaris);
		sb.append("</tbody></table></div></details>");
		return sb.toString();
	}

	/** Kartu metrik + rincian collapsible. */
	private static String kartu(String judul, double total, long jumlah, String detailHtml) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#fff;border:1px solid #e2e8f0;border-radius:10px;padding:12px 14px;"
				+ "box-shadow:0 4px 12px rgba(15,23,42,.05);'>");
		sb.append("<div style='font-size:12px;font-weight:800;color:#475569;'>").append(esc(judul)).append("</div>");
		sb.append("<div style='font-size:18px;font-weight:800;color:#0f172a;margin-top:2px;'>").append(rp(total))
				.append("</div>");
		sb.append("<div style='font-size:11px;color:#94a3b8;'>").append(jumlah).append(" transaksi/slip</div>");
		sb.append(detailHtml);
		sb.append("</div>");
		return sb.toString();
	}

	private static String gridWrap(String kartuHtml) {
		return "<div style='display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:12px;'>"
				+ kartuHtml + "</div>";
	}

	// ════════════════════════════════════════════════════════════════════════
	// 1) Gaji per kategori pegawai (pegawai/guru/dosen × tetap/honorer)
	// ════════════════════════════════════════════════════════════════════════

	private static final String[] LABEL_KATEGORI = { "Gaji Pegawai Tetap", "Gaji Pegawai Tidak Tetap / Honorer",
			"Gaji Guru Tetap", "Gaji Guru Tidak Tetap / Honorer", "Gaji Dosen Tetap",
			"Gaji Dosen Tidak Tetap / Honorer" };

	private static int indexKategori(Pegawai p) {
		boolean tetap = p.getTetap() != null && p.getTetap().intValue() == 1;
		boolean dosen = false, guru = false;
		try {
			dosen = p.getDosen() != null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DasborPenggajianDetailHelper.java:113");
		}
		try {
			guru = p.getGuru() != null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DasborPenggajianDetailHelper.java:117");
		}
		if (dosen) {
			return tetap ? 4 : 5;
		}
		if (guru) {
			return tetap ? 2 : 3;
		}
		return tetap ? 0 : 1;
	}

	@SuppressWarnings("unchecked")
	public static String panelGajiKategori(Session s, int tahun, int bulanMulai, int bulanSampai) {
		try {
			double[] total = new double[6];
			long[] jumlah = new long[6];
			StringBuilder[] det = new StringBuilder[6];
			for (int i = 0; i < 6; i++) {
				det[i] = new StringBuilder();
			}

			// Jumlahkan gaji per pegawai (group by pegawai) -> kurangi N+1 saat cek role.
			// Filter rentang bulan (1-12) mengikuti "Rentang Tgl" dashboard.
			List<Object[]> rows = s.createCriteria(PembayaranGajiPunyaPegawai.class, "x")
					.createAlias("x.pembayaranGaji", "pg").add(Restrictions.eq("pg.tahun", Integer.valueOf(tahun)))
					.add(Restrictions.between("pg.bulan", Integer.valueOf(bulanMulai), Integer.valueOf(bulanSampai)))
					.setProjection(Projections.projectionList().add(Projections.groupProperty("x.pegawai"))
							.add(Projections.sum("x.nilai")))
					.list();

			for (Object[] r : rows) {
				if (!(r[0] instanceof Pegawai)) {
					continue;
				}
				Pegawai p = (Pegawai) r[0];
				double nilai = dbl(r[1]);
				int idx = indexKategori(p);
				total[idx] += nilai;
				jumlah[idx]++;
				if (jumlah[idx] <= MAKS_RINCIAN) {
					det[idx].append("<tr><td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>")
							.append(esc(p.getNama())).append("</td><td style='padding:3px 6px;text-align:right;"
									+ "border-bottom:1px solid #f1f5f9;'>")
							.append(rp(nilai)).append("</td></tr>");
				}
			}

			StringBuilder kartu = new StringBuilder();
			String header = "<th style='text-align:left;padding:3px 6px;'>Nama</th>"
					+ "<th style='text-align:right;padding:3px 6px;'>Gaji</th>";
			for (int i = 0; i < 6; i++) {
				kartu.append(kartu(LABEL_KATEGORI[i], total[i], jumlah[i],
						detailBox(LABEL_KATEGORI[i], jumlah[i], det[i].toString(), header)));
			}
			return CommonDashboardHtmlHelper.panel("Gaji per Kategori Pegawai",
					"Total gaji yang dibayarkan, dipecah per status (tetap/honorer) dan jenis "
							+ "(pegawai/guru/dosen). Klik \"Lihat rincian\" untuk detail per orang.",
					gridWrap(kartu.toString()));
		} catch (Exception e) {
			return CommonDashboardHtmlHelper.errorState("Gaji per Kategori Pegawai", e);
		}
	}

	// ════════════════════════════════════════════════════════════════════════
	// 1b) Rincian Komponen Gaji (HASIL FORMULA slip) — dari PembayaranItemGajiPegawai
	// ════════════════════════════════════════════════════════════════════════

	/**
	 * Rincian komponen gaji sesuai RUMUS penghitungan slip (ItemGajiPegawaiTreeModel).
	 *
	 * <p>Rumus di {@code ItemGajiPegawaiTreeModel.hitungItemGajiPegawai} dihitung saat
	 * proses penggajian (substitusi variabel GAPOK/MAKAN/TRANSPORT/INSENTIF/transaksi/KPI/
	 * dll lalu dievaluasi exp4j) dan HASILNYA per item disimpan ke
	 * {@link PembayaranItemGajiPegawai}. Dashboard cukup MENJUMLAHKAN hasil tersimpan itu
	 * (tidak menjalankan ulang mesin formula yang berat). Dikelompokkan per NAMA komponen
	 * (Gaji Pokok, Tunjangan Jabatan, Tunjangan Lembur, Asuransi, potongan, honor, dst —
	 * sesuai item slip institusi), dengan rincian per pegawai.</p>
	 *
	 * <p>Catatan: tiap komponen dijumlah sendiri-sendiri (per nama) sehingga TIDAK ada
	 * double-count antar pegawai untuk komponen yang sama. Baris sub-total/induk slip bila
	 * ada akan muncul sebagai nama tersendiri (mis. "Total Penghasilan").</p>
	 */
	@SuppressWarnings("unchecked")
	public static String panelKomponenGajiFormula(Session s, int tahun, int bulanMulai, int bulanSampai) {
		try {
			List<Object[]> rows = s.createCriteria(PembayaranItemGajiPegawai.class, "x")
					.createAlias("x.pembayaranGajiPunyaPegawai", "pgp")
					.createAlias("pgp.pembayaranGaji", "pg")
					.add(Restrictions.eq("pg.tahun", Integer.valueOf(tahun)))
					.add(Restrictions.between("pg.bulan", Integer.valueOf(bulanMulai), Integer.valueOf(bulanSampai)))
					.setProjection(Projections.projectionList().add(Projections.groupProperty("x.nama"))
							.add(Projections.groupProperty("x.pegawai")).add(Projections.sum("x.nilai")))
					.list();

			// nama komponen -> [total, jumlahPegawai]; + rincian per pegawai.
			Map<String, double[]> agg = new LinkedHashMap<String, double[]>();
			Map<String, StringBuilder> det = new LinkedHashMap<String, StringBuilder>();
			for (Object[] r : rows) {
				double nilai = dbl(r[2]);
				if (nilai == 0.0) {
					continue; // lewati spacer / komponen bernilai 0
				}
				String nama = r[0] == null ? "(Tanpa nama)" : String.valueOf(r[0]);
				Pegawai p = (r[1] instanceof Pegawai) ? (Pegawai) r[1] : null;
				double[] a = agg.get(nama);
				if (a == null) {
					a = new double[2];
					agg.put(nama, a);
				}
				a[0] += nilai;
				a[1]++;
				StringBuilder d = det.get(nama);
				if (d == null) {
					d = new StringBuilder();
					det.put(nama, d);
				}
				if (a[1] <= MAKS_RINCIAN) {
					d.append("<tr><td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>")
							.append(esc(p == null ? "-" : p.getNama()))
							.append("</td><td style='padding:3px 6px;text-align:right;border-bottom:1px solid #f1f5f9;'>")
							.append(rp(nilai)).append("</td></tr>");
				}
			}

			if (agg.isEmpty()) {
				return CommonDashboardHtmlHelper.panel("Rincian Komponen Gaji (Hasil Formula Slip)",
						"Komponen gaji hasil perhitungan rumus slip.",
						CommonDashboardHtmlHelper.emptyState(
								"Belum ada data komponen gaji (PembayaranItemGajiPegawai) untuk tahun ini."));
			}

			// urut total desc
			List<Map.Entry<String, double[]>> entries = new ArrayList<Map.Entry<String, double[]>>(agg.entrySet());
			java.util.Collections.sort(entries, new java.util.Comparator<Map.Entry<String, double[]>>() {
				public int compare(Map.Entry<String, double[]> a, Map.Entry<String, double[]> b) {
					return Double.compare(b.getValue()[0], a.getValue()[0]);
				}
			});

			String header = "<th style='text-align:left;padding:3px 6px;'>Nama</th>"
					+ "<th style='text-align:right;padding:3px 6px;'>Nilai</th>";
			StringBuilder kartu = new StringBuilder();
			for (Map.Entry<String, double[]> e : entries) {
				kartu.append(kartu(e.getKey(), e.getValue()[0], (long) e.getValue()[1],
						detailBox(e.getKey(), (long) e.getValue()[1], det.get(e.getKey()).toString(), header)));
			}
			return CommonDashboardHtmlHelper.panel("Rincian Komponen Gaji (Hasil Formula Slip)",
					"Komponen gaji per item sesuai RUMUS slip (ItemGajiPegawaiTreeModel) — nilai hasil formula "
							+ "yang tersimpan saat penggajian. Klik \"Lihat rincian\" untuk detail per pegawai.",
					gridWrap(kartu.toString()));
		} catch (Exception e) {
			return CommonDashboardHtmlHelper.errorState("Rincian Komponen Gaji (Hasil Formula Slip)", e);
		}
	}

	// ════════════════════════════════════════════════════════════════════════
	// 2) Tunjangan & Honor per jenis (semua dari TransaksiPegawai)
	// ════════════════════════════════════════════════════════════════════════

	/** Daftar kategori yang diminta + kata kunci pencocokan nama JenisTransaksiPegawai. */
	private static final String[][] KATEGORI_TRANSAKSI = {
			{ "Tunjangan Jabatan", "jabatan" },
			{ "Tunjangan Lembur", "lembur" },
			{ "Asuransi", "asuransi" },
			{ "Biaya Perjalanan Dinas Luar", "perjalanan dinas" },
			{ "Honor Mengajar", "mengajar" },
			{ "Honor Koreksi Soal", "koreksi" },
			{ "Honor Pembuatan Soal", "pembuatan soal|buat soal" },
			{ "Honor Pengawas Ujian", "pengawas" },
			{ "Honor Pembimbing (S1/S2)", "pembimbing" },
			{ "Honor Penguji Sidang", "penguji" },
			{ "Honor Pra Sidang", "pra sidang|prasidang|pra-sidang" } };

	private static int cocokKategori(String namaJenis) {
		if (namaJenis == null) {
			return -1;
		}
		String n = namaJenis.toLowerCase();
		for (int i = 0; i < KATEGORI_TRANSAKSI.length; i++) {
			String[] kunciList = KATEGORI_TRANSAKSI[i][1].split("\\|");
			for (int k = 0; k < kunciList.length; k++) {
				if (n.indexOf(kunciList[k]) >= 0) {
					return i;
				}
			}
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	public static String panelTunjanganHonor(Session s, int tahun) {
		try {
			int nK = KATEGORI_TRANSAKSI.length;
			double[] total = new double[nK];
			long[] jumlah = new long[nK];
			StringBuilder[] det = new StringBuilder[nK];
			for (int i = 0; i < nK; i++) {
				det[i] = new StringBuilder();
			}
			double totalLain = 0.0;
			long jumlahLain = 0;

			List<TransaksiPegawai> rows = s.createCriteria(TransaksiPegawai.class)
					.add(Restrictions.eq("thn", Integer.valueOf(tahun))).addOrder(Order.desc("tanggal")).list();

			for (TransaksiPegawai t : rows) {
				JenisTransaksiPegawai j = t.getJenisTransaksiPegawai();
				// hanya Debet (penambah/tunjangan/honor); 2 = Kredit (potongan)
				if (j != null && j.getJenisTransaksi() != null && j.getJenisTransaksi().intValue() == 2) {
					continue;
				}
				String namaJenis = j == null ? "" : j.getNama();
				double nilai = t.getNilai() == null ? 0.0 : t.getNilai().doubleValue();
				int idx = cocokKategori(namaJenis);
				if (idx < 0) {
					totalLain += nilai;
					jumlahLain++;
					continue;
				}
				total[idx] += nilai;
				jumlah[idx]++;
				if (jumlah[idx] <= MAKS_RINCIAN) {
					Pegawai p = t.getPegawai();
					det[idx].append("<tr><td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>")
							.append(esc(p == null ? "-" : p.getNama()))
							.append("</td><td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>")
							.append(t.getTanggal() == null ? "" : Common.dateFormat.get().format(t.getTanggal()))
							.append("</td><td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>")
							.append(esc(namaJenis))
							.append("</td><td style='padding:3px 6px;text-align:right;border-bottom:1px solid #f1f5f9;'>")
							.append(rp(nilai)).append("</td></tr>");
				}
			}

			String header = "<th style='text-align:left;padding:3px 6px;'>Nama</th>"
					+ "<th style='text-align:left;padding:3px 6px;'>Tanggal</th>"
					+ "<th style='text-align:left;padding:3px 6px;'>Jenis</th>"
					+ "<th style='text-align:right;padding:3px 6px;'>Nilai</th>";
			StringBuilder kartu = new StringBuilder();
			for (int i = 0; i < nK; i++) {
				kartu.append(kartu(KATEGORI_TRANSAKSI[i][0], total[i], jumlah[i],
						detailBox(KATEGORI_TRANSAKSI[i][0], jumlah[i], det[i].toString(), header)));
			}
			if (jumlahLain > 0) {
				kartu.append(kartu("Tunjangan/Honor Lainnya", totalLain, jumlahLain, ""));
			}
			return CommonDashboardHtmlHelper.panel("Tunjangan, Asuransi, Perjalanan Dinas & Honor",
					"Total per jenis transaksi pegawai (tunjangan jabatan/lembur, asuransi, perjalanan dinas, "
							+ "serta honor dosen). Klik \"Lihat rincian\" untuk detail penerima.",
					gridWrap(kartu.toString()));
		} catch (Exception e) {
			return CommonDashboardHtmlHelper.errorState("Tunjangan & Honor", e);
		}
	}

	// ════════════════════════════════════════════════════════════════════════
	// 3) Daftar Cuti (filter tanggal)
	// ════════════════════════════════════════════════════════════════════════

	@SuppressWarnings("unchecked")
	public static String panelDaftarCuti(Session s, Date mulai, Date sampai) {
		try {
			Criteria c = s.createCriteria(CutiDanIzin.class);

			// HAK AKSES: filter bawahan sama persis dengan CutiDanIzinAction
			// (atasan langsung + jabatan struktural + Pejabat) via PegawaiBawahanHelper.
			try {
				ais.database.model.Tbmuser tbmuserAkses = Common.getCurrentUser();
				PegawaiBawahanHelper.BawahanSet bs = PegawaiBawahanHelper.ambilBawahan(s, tbmuserAkses);
				c.createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN);
				PegawaiBawahanHelper.applyFilterCuti(c, bs, tbmuserAkses);
			} catch (Exception eAkses) {
				// Gagal menentukan hak akses → fail-closed.
				c.add(Restrictions.sqlRestriction("1=0"));
			}

			if (mulai != null) {
				c.add(Restrictions.ge("mulai", mulai));
			}
			if (sampai != null) {
				c.add(Restrictions.le("mulai", sampai));
			}
			List<CutiDanIzin> rows = c.addOrder(Order.desc("mulai")).setMaxResults(MAKS_RINCIAN).list();

			if (rows.isEmpty()) {
				return CommonDashboardHtmlHelper.panel("Daftar Cuti & Izin",
						"Pengajuan cuti/izin sesuai rentang tanggal.",
						CommonDashboardHtmlHelper.emptyState("Tidak ada data cuti/izin pada rentang ini."));
			}

			StringBuilder b = new StringBuilder();
			b.append("<div style='max-height:420px;overflow:auto;'>");
			b.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>");
			b.append("<thead><tr style='background:#f1f5f9;'>"
					+ "<th style='text-align:left;padding:4px 6px;'>Nama</th>"
					+ "<th style='text-align:left;padding:4px 6px;'>Jenis</th>"
					+ "<th style='text-align:left;padding:4px 6px;'>Mulai</th>"
					+ "<th style='text-align:left;padding:4px 6px;'>Sampai</th>"
					+ "<th style='text-align:right;padding:4px 6px;'>Hari</th>"
					+ "<th style='text-align:left;padding:4px 6px;'>Status</th>"
					+ "<th style='text-align:left;padding:4px 6px;'>Keterangan</th></tr></thead><tbody>");
			for (CutiDanIzin x : rows) {
				Pegawai p = x.getPegawai();
				String jenis = "";
				try {
					jenis = x.getJenisCutiDanIzin() == null ? "" : x.getJenisCutiDanIzin().getNama();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DasborPenggajianDetailHelper.java:423");
				}
				String status = Boolean.TRUE.equals(x.getSetujui()) ? "Disetujui" : "Belum disetujui";
				b.append("<tr>")
						.append("<td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>")
						.append(esc(p == null ? "-" : p.getNama())).append("</td>")
						.append("<td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>").append(esc(jenis))
						.append("</td>")
						.append("<td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>")
						.append(x.getMulai() == null ? "" : Common.dateFormat.get().format(x.getMulai())).append("</td>")
						.append("<td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>")
						.append(x.getSampai() == null ? "" : Common.dateFormat.get().format(x.getSampai()))
						.append("</td>")
						.append("<td style='padding:3px 6px;text-align:right;border-bottom:1px solid #f1f5f9;'>")
						.append(x.getJumlahHariCuti() == null ? "" : x.getJumlahHariCuti().toString()).append("</td>")
						.append("<td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>").append(esc(status))
						.append("</td>")
						.append("<td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>")
						.append(esc(x.getKeterangan())).append("</td></tr>");
			}
			b.append("</tbody></table></div>");
			return CommonDashboardHtmlHelper.panel("Daftar Cuti & Izin",
					"Pengajuan cuti/izin pegawai/guru/dosen sesuai rentang tanggal (maks " + MAKS_RINCIAN + " baris).",
					b.toString());
		} catch (Exception e) {
			return CommonDashboardHtmlHelper.errorState("Daftar Cuti & Izin", e);
		}
	}

	// ════════════════════════════════════════════════════════════════════════
	// 4) Daftar Perjalanan Dinas Luar (filter tanggal)
	// ════════════════════════════════════════════════════════════════════════

	@SuppressWarnings("unchecked")
	public static String panelDaftarPerjalananDinas(Session s, Date mulai, Date sampai) {
		try {
			Criteria c = s.createCriteria(TransaksiPegawai.class, "t")
					.createAlias("t.jenisTransaksiPegawai", "j", Criteria.LEFT_JOIN)
					.add(Restrictions.ilike("j.nama", "perjalanan dinas", org.hibernate.criterion.MatchMode.ANYWHERE));
			if (mulai != null) {
				c.add(Restrictions.ge("t.tanggal", mulai));
			}
			if (sampai != null) {
				c.add(Restrictions.le("t.tanggal", sampai));
			}
			List<TransaksiPegawai> rows = c.addOrder(Order.desc("t.tanggal")).setMaxResults(MAKS_RINCIAN).list();

			if (rows.isEmpty()) {
				return CommonDashboardHtmlHelper.panel("Daftar Perjalanan Dinas Luar",
						"Transaksi perjalanan dinas luar sesuai rentang tanggal.",
						CommonDashboardHtmlHelper.emptyState("Tidak ada data perjalanan dinas pada rentang ini."));
			}

			double total = 0.0;
			StringBuilder b = new StringBuilder();
			b.append("<div style='max-height:420px;overflow:auto;'>");
			b.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>");
			b.append("<thead><tr style='background:#f1f5f9;'>"
					+ "<th style='text-align:left;padding:4px 6px;'>Nama</th>"
					+ "<th style='text-align:left;padding:4px 6px;'>Tanggal</th>"
					+ "<th style='text-align:left;padding:4px 6px;'>Keterangan</th>"
					+ "<th style='text-align:right;padding:4px 6px;'>Nilai</th></tr></thead><tbody>");
			for (TransaksiPegawai t : rows) {
				Pegawai p = t.getPegawai();
				double nilai = t.getNilai() == null ? 0.0 : t.getNilai().doubleValue();
				total += nilai;
				b.append("<tr>")
						.append("<td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>")
						.append(esc(p == null ? "-" : p.getNama())).append("</td>")
						.append("<td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>")
						.append(t.getTanggal() == null ? "" : Common.dateFormat.get().format(t.getTanggal()))
						.append("</td>")
						.append("<td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>")
						.append(esc(t.getKeterangan()))
						.append("</td>")
						.append("<td style='padding:3px 6px;text-align:right;border-bottom:1px solid #f1f5f9;'>")
						.append(rp(nilai)).append("</td></tr>");
			}
			b.append("</tbody></table></div>");
			b.append("<div style='text-align:right;font-weight:800;color:#0f172a;margin-top:6px;'>Total: ")
					.append(rp(total)).append("</div>");
			return CommonDashboardHtmlHelper.panel("Daftar Perjalanan Dinas Luar",
					"Pegawai/guru/dosen yang melakukan perjalanan dinas luar sesuai rentang tanggal (maks "
							+ MAKS_RINCIAN + " baris).",
					b.toString());
		} catch (Exception e) {
			return CommonDashboardHtmlHelper.errorState("Daftar Perjalanan Dinas Luar", e);
		}
	}

	// ════════════════════════════════════════════════════════════════════════
	// 5) Rekap Kehadiran Pegawai per Bulan — VERSI TABEL (KehadiranPegawaiBulanan)
	// ════════════════════════════════════════════════════════════════════════

	private static final String[] NAMA_BULAN = { "", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep",
			"Okt", "Nov", "Des" };

	private static String namaBulan(Integer b) {
		if (b == null || b.intValue() < 1 || b.intValue() > 12) {
			return b == null ? "-" : String.valueOf(b);
		}
		return NAMA_BULAN[b.intValue()];
	}

	/** Angka bulat ringkas (ribuan) tanpa "Rp". */
	private static String num(double v) {
		return Common.numberFormat.get().format(Math.round(v));
	}

	private static int intval(Integer v) {
		return v == null ? 0 : v.intValue();
	}

	/** Sel angka (rata kanan) dengan warna penanda bila &gt; 0. */
	private static String selAngka(int v, String warna) {
		String style = "padding:3px 6px;text-align:right;border-bottom:1px solid #f1f5f9;";
		if (v > 0 && warna != null) {
			style += "color:" + warna + ";font-weight:700;";
		}
		return "<td style='" + style + "'>" + (v == 0 ? "-" : String.valueOf(v)) + "</td>";
	}

	/**
	 * Rekap kehadiran pegawai per bulan dalam bentuk TABEL ({@link KehadiranPegawaiBulanan}).
	 *
	 * <p>Sumber data = rekap kehadiran bulanan yang sudah tersimpan (bukan absen detail
	 * harian). Mengikuti filter dashboard: TAHUN dari combo penggajian, BULAN dari rentang
	 * tanggal ("Rentang Tgl") yang dipetakan ke bulan 1-12 — sehingga bila rentang 1/3/6
	 * bulan, hanya bulan-bulan itu yang muncul; default Jan-Des = setahun penuh. Satu baris
	 * = satu pegawai pada satu bulan. Di bawah tabel ditampilkan baris TOTAL (penjumlahan
	 * seluruh kolom angka). Dibatasi {@link #MAKS_RINCIAN} baris agar ringan.</p>
	 */
	@SuppressWarnings("unchecked")
	public static String panelKehadiranBulanan(Session s, int tahun, int bulanMulai, int bulanSampai) {
		try {
			List<KehadiranPegawaiBulanan> rows = s.createCriteria(KehadiranPegawaiBulanan.class)
					.add(Restrictions.eq("tahun", Integer.valueOf(tahun)))
					.add(Restrictions.between("bulan", Integer.valueOf(bulanMulai), Integer.valueOf(bulanSampai)))
					.addOrder(Order.asc("nama")).addOrder(Order.asc("bulan")).setMaxResults(MAKS_RINCIAN).list();

			if (rows.isEmpty()) {
				return CommonDashboardHtmlHelper.panel("Rekap Kehadiran Pegawai (per Bulan)",
						"Rekap kehadiran bulanan sesuai tahun &amp; rentang bulan.",
						CommonDashboardHtmlHelper.emptyState(
								"Belum ada data rekap kehadiran (KehadiranPegawaiBulanan) untuk periode ini."));
			}

			// akumulasi total kolom
			long tMasuk = 0, tTepat = 0, tTerlambat = 0, tPulangCepat = 0, tAlpa = 0, tSakit = 0, tIzin = 0, tCuti = 0;
			double tLembur = 0.0;

			StringBuilder b = new StringBuilder();
			b.append("<div style='max-height:460px;overflow:auto;'>");
			b.append("<table style='width:100%;border-collapse:collapse;font-size:11px;white-space:nowrap;'>");
			b.append("<thead><tr style='background:#f1f5f9;'>"
					+ "<th style='text-align:left;padding:4px 6px;'>No</th>"
					+ "<th style='text-align:left;padding:4px 6px;'>Nama</th>"
					+ "<th style='text-align:left;padding:4px 6px;'>Periode</th>"
					+ "<th style='text-align:right;padding:4px 6px;'>Masuk</th>"
					+ "<th style='text-align:right;padding:4px 6px;'>Tepat Waktu</th>"
					+ "<th style='text-align:right;padding:4px 6px;'>Terlambat</th>"
					+ "<th style='text-align:right;padding:4px 6px;'>Pulang Cepat</th>"
					+ "<th style='text-align:right;padding:4px 6px;'>Alpa</th>"
					+ "<th style='text-align:right;padding:4px 6px;'>Sakit</th>"
					+ "<th style='text-align:right;padding:4px 6px;'>Izin</th>"
					+ "<th style='text-align:right;padding:4px 6px;'>Cuti</th>"
					+ "<th style='text-align:right;padding:4px 6px;'>Lembur (jam)</th></tr></thead><tbody>");

			int no = 0;
			for (KehadiranPegawaiBulanan x : rows) {
				no++;
				String nama = x.getNama();
				if (nama == null || nama.trim().length() == 0) {
					Pegawai p = x.getPegawai();
					nama = (p == null) ? "-" : p.getNama();
				}
				int masuk = intval(x.getMasuk());
				int tepat = intval(x.getTepatWaktu());
				int terlambat = intval(x.getTerlambat());
				int pulangCepat = intval(x.getPulangcepat());
				int alpa = intval(x.getAlpa());
				int sakit = intval(x.getSakit());
				int izin = intval(x.getIzin());
				int cuti = intval(x.getCuti());
				double lembur = x.getLembur() == null ? 0.0 : x.getLembur().doubleValue();

				tMasuk += masuk;
				tTepat += tepat;
				tTerlambat += terlambat;
				tPulangCepat += pulangCepat;
				tAlpa += alpa;
				tSakit += sakit;
				tIzin += izin;
				tCuti += cuti;
				tLembur += lembur;

				b.append("<tr>")
						.append("<td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>").append(no).append("</td>")
						.append("<td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>").append(esc(nama))
						.append("</td>")
						.append("<td style='padding:3px 6px;border-bottom:1px solid #f1f5f9;'>")
						.append(namaBulan(x.getBulan())).append(" ").append(intval(x.getTahun())).append("</td>")
						.append(selAngka(masuk, null))
						.append(selAngka(tepat, "#16a34a"))
						.append(selAngka(terlambat, "#d97706"))
						.append(selAngka(pulangCepat, "#d97706"))
						.append(selAngka(alpa, "#dc2626"))
						.append(selAngka(sakit, null))
						.append(selAngka(izin, null))
						.append(selAngka(cuti, null))
						.append("<td style='padding:3px 6px;text-align:right;border-bottom:1px solid #f1f5f9;'>")
						.append(lembur == 0.0 ? "-" : num(lembur)).append("</td>")
						.append("</tr>");
			}

			// baris TOTAL
			b.append("<tr style='background:#eef2ff;font-weight:800;color:var(--ais-theme-primary,#1e3a8a);'>")
					.append("<td style='padding:5px 6px;' colspan='3'>TOTAL (")
					.append(rows.size()).append(" baris)</td>")
					.append("<td style='padding:5px 6px;text-align:right;'>").append(num(tMasuk)).append("</td>")
					.append("<td style='padding:5px 6px;text-align:right;'>").append(num(tTepat)).append("</td>")
					.append("<td style='padding:5px 6px;text-align:right;'>").append(num(tTerlambat)).append("</td>")
					.append("<td style='padding:5px 6px;text-align:right;'>").append(num(tPulangCepat)).append("</td>")
					.append("<td style='padding:5px 6px;text-align:right;'>").append(num(tAlpa)).append("</td>")
					.append("<td style='padding:5px 6px;text-align:right;'>").append(num(tSakit)).append("</td>")
					.append("<td style='padding:5px 6px;text-align:right;'>").append(num(tIzin)).append("</td>")
					.append("<td style='padding:5px 6px;text-align:right;'>").append(num(tCuti)).append("</td>")
					.append("<td style='padding:5px 6px;text-align:right;'>").append(num(tLembur)).append("</td>")
					.append("</tr>");

			b.append("</tbody></table></div>");
			return CommonDashboardHtmlHelper.panel("Rekap Kehadiran Pegawai (per Bulan)",
					"Rekap kehadiran bulanan (masuk, tepat waktu, terlambat, pulang cepat, alpa, sakit, izin, cuti, "
							+ "lembur) sesuai tahun &amp; rentang bulan. Satu baris = satu pegawai per bulan (maks "
							+ MAKS_RINCIAN + " baris).",
					b.toString());
		} catch (Exception e) {
			return CommonDashboardHtmlHelper.errorState("Rekap Kehadiran Pegawai (per Bulan)", e);
		}
	}
}
