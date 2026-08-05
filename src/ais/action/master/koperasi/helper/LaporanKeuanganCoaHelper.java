package ais.action.master.koperasi.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.akunting.KelompokLaporan;
import ais.database.model.akunting.KelompokLaporanPunyaAkun;
import ais.database.model.akunting.MasterGrupLaporan;
import ais.ui.util.DashboardUiKit;

/**
 * <h2>LaporanKeuanganCoaHelper — Penyusun Laporan Keuangan Koperasi dari Buku Besar (COA)</h2>
 *
 * <p>
 * Utilitas ini menyusun laporan keuangan koperasi (Neraca, Perhitungan Hasil Usaha/Laba Rugi, dan
 * Arus Kas) langsung dari <b>buku besar terposting</b> pada modul akuntansi, bukan dari perkiraan
 * manajerial. Pengelompokan tiap akun ke baris laporan mengikuti konfigurasi yang sudah ada di modul
 * akuntansi — {@link JenisLaporan} (jenis laporan), {@link MasterGrupLaporan} (kelompok besar seperti
 * Aktiva/Kewajiban/Modal/Pendapatan/Beban), {@link KelompokLaporan} (baris laporan), dan
 * {@link KelompokLaporanPunyaAkun} (pemetaan akun ke baris) — sehingga hasilnya konsisten dengan
 * laporan keuangan resmi entitas.
 * </p>
 *
 * <h3>Cara menghitung saldo</h3>
 * <p>
 * Saldo tiap akun dihitung dari tabel {@code akunting.transaksi} yang sudah <b>terposting</b>
 * (memiliki {@code posting_history}, dan bila posting tidak otomatis, {@code posting=true}) memakai
 * rumus baku {@code Σ(debet − kredit)} — mengikuti persis konvensi dasbor/laporan akuntansi yang ada.
 * Untuk laporan posisi (Neraca) saldo bersifat <b>kumulatif</b> sampai tanggal akhir; untuk laporan
 * arus/hasil (Laba Rugi, Arus Kas) saldo dihitung untuk <b>periode</b> tanggal awal–akhir. Tanda tiap
 * akun dinormalkan dengan saldo normalnya ({@link Akun#getDebetCredit()}: 1=Debet, −1=Kredit) sehingga
 * pos yang wajar bernilai positif dan penjumlahan per baris menjadi tepat.
 * </p>
 *
 * <h3>Penyajian</h3>
 * <p>
 * Hasil disajikan bertingkat: kelompok besar → baris laporan → (opsional) rincian akun bila
 * {@link KelompokLaporan#getTampilkanAkunRinci()} aktif, lengkap dengan subtotal per kelompok. Untuk
 * Neraca ditampilkan Total Aktiva vs Total Pasiva (Kewajiban+Modal) beserta penanda seimbang; untuk
 * Laba Rugi ditampilkan Total Pendapatan, Total Beban, dan Sisa Hasil Usaha. Seluruh keluaran berupa
 * HTML/CSS sederhana (tanpa pustaka grafik) yang ramah cetak.
 * </p>
 *
 * <h3>Desain</h3>
 * <p>
 * Kelas final berkonstruktor privat. Memakai {@code Session} yang diberikan pemanggil (tidak menutup
 * sendiri; ikut kaidah {@code currentSession()}). Query saldo memakai SQL native (agregasi cepat di
 * basis data), sedangkan struktur laporan dibaca lewat entity Hibernate agar mengikuti konfigurasi.
 * Kompatibel Java 1.7, aman-null, dan hemat memori (hanya menyimpan peta saldo per akun).
 * </p>
 */
public final class LaporanKeuanganCoaHelper {

	private LaporanKeuanganCoaHelper() {
		// util statis
	}

	/** Kelas tampung baris laporan (satu KelompokLaporan) beserta nilainya dan rincian akunnya. */
	private static final class Baris {
		String label;
		double nilai;
		final List<String[]> rincian = new ArrayList<String[]>(); // {kode+nama, nilaiRp}
	}

	/** Kelas tampung satu kelompok besar (MasterGrupLaporan) dan baris-barisnya. */
	private static final class Grup {
		String nama;
		String keterangan;
		final List<Baris> baris = new ArrayList<Baris>();

		double total() {
			double t = 0.0;
			for (Baris b : baris) {
				t += b.nilai;
			}
			return t;
		}
	}

	/**
	 * Susun dan tempelkan sebuah laporan keuangan ke {@code host}.
	 *
	 * @param host    komponen tempat laporan digambar
	 * @param session sesi Hibernate aktif (tidak ditutup di sini)
	 * @param jl      jenis laporan terpilih (Neraca/Laba Rugi/Arus Kas)
	 * @param awal    tanggal awal periode (dipakai untuk laporan arus/hasil)
	 * @param sampai  tanggal akhir periode
	 */
	public static void render(Component host, Session session, JenisLaporan jl, Date awal, Date sampai) {
		if (host == null || jl == null || jl.getId() == null) {
			return;
		}
		String namaLaporan = jl.getNama() == null ? "Laporan Keuangan" : jl.getNama();
		boolean kumulatif = namaLaporan.toLowerCase().contains("neraca");

		Map<Long, Double> saldoMap = hitungSaldo(session, awal, sampai, kumulatif);

		// Baca struktur laporan (baris) dan pemetaan akun sesuai konfigurasi.
		List<KelompokLaporan> kelompoks = ambilKelompok(session, jl.getId());
		Map<Long, List<KelompokLaporanPunyaAkun>> pemetaan = ambilPemetaan(session, jl.getId());

		// Kelompokkan baris ke dalam grup besar (MasterGrupLaporan), pertahankan urutan urut.
		LinkedHashMap<Long, Grup> grupMap = new LinkedHashMap<Long, Grup>();
		for (KelompokLaporan kl : kelompoks) {
			try {
				MasterGrupLaporan mgl = kl.getMasterGrupLaporan();
				Long gid = mgl == null || mgl.getId() == null ? -1L : mgl.getId();
				Grup g = grupMap.get(gid);
				if (g == null) {
					g = new Grup();
					g.nama = mgl == null || mgl.getNama() == null ? "Lainnya" : mgl.getNama();
					g.keterangan = mgl == null ? "" : (mgl.getKeterangan() == null ? "" : mgl.getKeterangan());
					grupMap.put(gid, g);
				}
				Baris b = new Baris();
				b.label = kl.getKeterangan() == null ? "-" : kl.getKeterangan();
				double totalBaris = 0.0;
				List<KelompokLaporanPunyaAkun> akunBaris = pemetaan.get(kl.getId());
				boolean rinci = Boolean.TRUE.equals(kl.getTampilkanAkunRinci());
				if (akunBaris != null) {
					for (KelompokLaporanPunyaAkun kp : akunBaris) {
						try {
							Akun akun = kp.getAkun();
							if (akun == null || akun.getId() == null) {
								continue;
							}
							Double s = saldoMap.get(akun.getId());
							double nilai = (s == null ? 0.0 : s) * akun.getDebetCredit();
							totalBaris += nilai;
							if (rinci) {
								String namaAkun = (akun.getKode() == null ? "" : akun.getKode() + " ")
										+ (akun.getNama() == null ? "" : akun.getNama());
								b.rincian.add(new String[] { namaAkun, DashboardUiKit.money(nilai) });
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/LaporanKeuanganCoaHelper.java:146");
							// lewati pemetaan bermasalah
						}
					}
				}
				b.nilai = totalBaris;
				g.baris.add(b);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		host.appendChild(DashboardUiKit.html(bangunHtml(namaLaporan, kumulatif, awal, sampai, grupMap)));
	}

	/** Susun tampilan HTML laporan bertingkat + subtotal + ikhtisar (Neraca / Laba Rugi). */
	private static String bangunHtml(String namaLaporan, boolean kumulatif, Date awal, Date sampai,
			LinkedHashMap<Long, Grup> grupMap) {
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy");
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:16px;font-weight:800;color:#0f172a;margin:6px 0 2px;'>")
				.append(DashboardUiKit.esc(namaLaporan)).append("</div>");
		sb.append(DashboardUiKit.descChip(
				"Disusun dari buku besar terposting sesuai pengelompokan akun yang terkonfigurasi. Periode "
						+ (kumulatif ? "s/d " + (sampai == null ? "-" : sdf.format(sampai))
								: (awal == null ? "-" : sdf.format(awal)) + " s/d "
										+ (sampai == null ? "-" : sdf.format(sampai)))
						+ "."));

		sb.append("<table style='border-collapse:collapse;width:100%;font-size:13px;'>");
		double aset = 0.0, pasiva = 0.0, pendapatan = 0.0, beban = 0.0;
		for (Grup g : grupMap.values()) {
			sb.append("<tr style='background:#f1f5f9;'>")
					.append("<td colspan='2' style='padding:6px 8px;border:1px solid #e2e8f0;font-weight:800;color:#0f172a;'>")
					.append(DashboardUiKit.esc(g.nama))
					.append(g.keterangan == null || g.keterangan.isEmpty() ? "" : " — " + DashboardUiKit.esc(g.keterangan))
					.append("</td></tr>");
			for (Baris b : g.baris) {
				if (!b.rincian.isEmpty()) {
					for (String[] r : b.rincian) {
						sb.append("<tr><td style='padding:3px 8px 3px 26px;border:1px solid #e2e8f0;color:#475569;'>")
								.append(DashboardUiKit.esc(r[0])).append("</td>")
								.append("<td style='padding:3px 8px;border:1px solid #e2e8f0;text-align:right;color:#475569;'>Rp ")
								.append(r[1]).append("</td></tr>");
					}
				}
				sb.append("<tr><td style='padding:4px 8px 4px 16px;border:1px solid #e2e8f0;font-weight:600;'>")
						.append(DashboardUiKit.esc(b.label)).append("</td>")
						.append("<td style='padding:4px 8px;border:1px solid #e2e8f0;text-align:right;font-weight:600;'>Rp ")
						.append(DashboardUiKit.money(b.nilai)).append("</td></tr>");
			}
			double gt = g.total();
			sb.append("<tr style='background:#f8fafc;'>")
					.append("<td style='padding:5px 8px;border:1px solid #e2e8f0;text-align:right;font-weight:800;'>Jumlah ")
					.append(DashboardUiKit.esc(g.nama)).append("</td>")
					.append("<td style='padding:5px 8px;border:1px solid #e2e8f0;text-align:right;font-weight:800;color:#2563eb;'>Rp ")
					.append(DashboardUiKit.money(gt)).append("</td></tr>");

			String low = g.nama == null ? "" : g.nama.toLowerCase();
			if (low.contains("aktiva") || low.contains("aset")) {
				aset += gt;
			} else if (low.contains("kewajiban") || low.contains("hutang") || low.contains("modal")
					|| low.contains("ekuitas") || low.contains("pasiva")) {
				pasiva += gt;
			} else if (low.contains("pendapat") || low.contains("penerimaan")) {
				pendapatan += gt;
			} else if (low.contains("beban") || low.contains("biaya")) {
				beban += gt;
			}
		}
		sb.append("</table>");

		// Ikhtisar penyeimbang / hasil usaha.
		if (kumulatif && (aset != 0.0 || pasiva != 0.0)) {
			boolean seimbang = Math.abs(aset - pasiva) < 1.0;
			sb.append("<div style='margin-top:10px;padding:10px 12px;border-radius:10px;border:1px solid #e2e8f0;")
					.append("background:").append(seimbang ? "#f0fdf4" : "#fef2f2").append(";font-size:13px;'>")
					.append("<b>Total Aktiva:</b> Rp ").append(DashboardUiKit.money(aset))
					.append(" &nbsp;•&nbsp; <b>Total Pasiva (Kewajiban+Modal):</b> Rp ")
					.append(DashboardUiKit.money(pasiva)).append("<br><b>Status:</b> ")
					.append(seimbang ? "Neraca seimbang." : "Terdapat selisih Rp " + DashboardUiKit.money(aset - pasiva)
							+ " — periksa jurnal yang belum terposting/penutup.")
					.append("</div>");
		} else if (!kumulatif && (pendapatan != 0.0 || beban != 0.0)) {
			double shu = pendapatan - beban;
			sb.append("<div style='margin-top:10px;padding:10px 12px;border-radius:10px;border:1px solid #e2e8f0;")
					.append("background:#eff6ff;font-size:13px;'>")
					.append("<b>Total Pendapatan:</b> Rp ").append(DashboardUiKit.money(pendapatan))
					.append(" &nbsp;•&nbsp; <b>Total Beban:</b> Rp ").append(DashboardUiKit.money(beban))
					.append("<br><b>Sisa Hasil Usaha (SHU):</b> Rp ").append(DashboardUiKit.money(shu))
					.append("</div>");
		}
		return sb.toString();
	}

	/** Hitung saldo per akun (id → Σ(debet−kredit)) dari transaksi terposting, kumulatif atau per periode. */
	@SuppressWarnings("unchecked")
	private static Map<Long, Double> hitungSaldo(Session session, Date awal, Date sampai, boolean kumulatif) {
		Map<Long, Double> map = new HashMap<Long, Double>();
		try {
			String dateCond = kumulatif ? " and date(gt.tanggal_transaksi) <= date(:sampai) "
					: " and date(gt.tanggal_transaksi) >= date(:awal) and date(gt.tanggal_transaksi) <= date(:sampai) ";
			String postingJoin = ConstantValues.otomatisTerposting ? ""
					: " inner join akunting.posting_history ph on (ph.id = gt.posting_history and ph.posting = true) ";
			String sql = "select t.akun as akun_id, coalesce(sum(t.debet - t.kredit), 0) as saldo "
					+ " from akunting.transaksi t "
					+ " inner join akunting.grup_transaksi gt on gt.id = t.grup_transaksi "
					+ postingJoin
					+ " where gt.posting_history is not null and t.akun is not null "
					+ " and date(:sampai) is not null "
					+ dateCond
					+ " group by t.akun ";
			org.hibernate.SQLQuery q = session.createSQLQuery(sql);
			q.setParameter("sampai", sampai);
			if (!kumulatif) {
				q.setParameter("awal", awal);
			}
			List<Object[]> rows = q.list();
			for (Object[] o : rows) {
				try {
					if (o[0] == null) {
						continue;
					}
					Long id = ((Number) o[0]).longValue();
					double s = o[1] == null ? 0.0 : ((Number) o[1]).doubleValue();
					map.put(id, s);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/LaporanKeuanganCoaHelper.java:272");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return map;
	}

	/** Ambil baris laporan (KelompokLaporan) aktif untuk sebuah jenis laporan, urut menaik. */
	@SuppressWarnings("unchecked")
	private static List<KelompokLaporan> ambilKelompok(Session session, Long jenisLaporanId) {
		try {
			return session.createQuery(
					"select kl from KelompokLaporan kl where kl.jenisLaporan.id = :jid "
							+ "and (kl.aktif is null or kl.aktif = true) order by kl.urut")
					.setParameter("jid", jenisLaporanId).list();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return new ArrayList<KelompokLaporan>();
		}
	}

	/** Ambil pemetaan akun→baris untuk sebuah jenis laporan, dikelompokkan per KelompokLaporan id. */
	@SuppressWarnings("unchecked")
	private static Map<Long, List<KelompokLaporanPunyaAkun>> ambilPemetaan(Session session, Long jenisLaporanId) {
		Map<Long, List<KelompokLaporanPunyaAkun>> peta = new HashMap<Long, List<KelompokLaporanPunyaAkun>>();
		try {
			List<KelompokLaporanPunyaAkun> list = session.createQuery(
					"select kp from KelompokLaporanPunyaAkun kp left join fetch kp.akun "
							+ "where kp.kelompokLaporan.jenisLaporan.id = :jid order by kp.nomorUrut")
					.setParameter("jid", jenisLaporanId).list();
			for (KelompokLaporanPunyaAkun kp : list) {
				try {
					KelompokLaporan kl = kp.getKelompokLaporan();
					if (kl == null || kl.getId() == null) {
						continue;
					}
					List<KelompokLaporanPunyaAkun> bucket = peta.get(kl.getId());
					if (bucket == null) {
						bucket = new ArrayList<KelompokLaporanPunyaAkun>();
						peta.put(kl.getId(), bucket);
					}
					bucket.add(kp);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/LaporanKeuanganCoaHelper.java:316");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return peta;
	}
}
