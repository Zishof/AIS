package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Div;

import ais.action.master.koperasi.helper.KesehatanUtil;
import ais.action.master.koperasi.helper.KolektibilitasUtil;
import ais.action.master.koperasi.helper.SimpanPinjamUiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyMessageboxConfig;

/**
 * <h2>DashboardSimpanPinjamAction — Dasbor Unit Simpan Pinjam (USP) Koperasi</h2>
 *
 * <p>
 * Composer ZK ini menyajikan <b>ringkasan menyeluruh kegiatan simpan pinjam koperasi</b> dalam satu
 * layar yang mudah dibaca oleh pengurus maupun anggota awam: berapa banyak anggota, berapa uang
 * pinjaman yang sedang berjalan, seberapa lancar pengembaliannya, tren penyaluran dari bulan ke
 * bulan, siapa peminjam terbesar, dan bagaimana komposisi simpanan. Seluruh grafik digambar memakai
 * {@link DashboardUiKit} (HTML/CSS/SVG murni — <b>tanpa JFreeChart</b>) sehingga otomatis rapi dan
 * responsif di layar HP maupun desktop. Tabel rincian ditampilkan sebagai grid ZK lebih dulu, dengan
 * tombol <i>Unduh Excel</i> (dibangun lewat {@link SimpanPinjamUiUtil}) yang baru menghasilkan berkas
 * Excel asli ketika ditekan.
 * </p>
 *
 * <h3>Dibangun di atas engine existing (tanpa tabel baru)</h3>
 * <p>
 * Dasbor ini <b>tidak</b> membuat model/tabel baru. Ia membaca langsung entity yang sudah ada:
 * </p>
 * <ul>
 * <li>{@link TransaksiKoperasi} — akad pinjaman/simpanan. Dibedakan pinjaman vs simpanan lewat
 * {@code produkKoperasi.tipeProdukKoperasi} yang dicocokkan dengan {@link ConstantValues#PINJAMAN}
 * dan {@link ConstantValues#SIMPANAN}. Kualitas pinjaman dibaca dari
 * {@link TransaksiKoperasi#getKolektibilitas()}.</li>
 * <li>{@link TransaksiKoperasiDetail} — jadwal angsuran (pokok + margin/bunga). Angsuran yang belum
 * tertaut pembayaran dihitung sebagai sisa/outstanding.</li>
 * <li>{@link AnggotaKoperasi} — data anggota (jumlah &amp; peringkat peminjam).</li>
 * </ul>
 *
 * <h3>Ketahanan &amp; efisiensi</h3>
 * <p>
 * Session diperoleh via {@link HibernateUtil#currentSession()} sehingga ditutup otomatis oleh
 * kerangka kerja (tidak ditutup manual). Tiap seksi dibungkus {@code try/catch} mandiri agar
 * kegagalan satu grafik tidak menumbangkan dasbor. Asosiasi produk/anggota di-<i>fetch</i> sekaligus
 * untuk menekan query N+1. Kompatibel Java 1.7; mengikuti pola {@code DashboardKantinAction}.
 * </p>
 *
 * @see DashboardUiKit
 * @see SimpanPinjamUiUtil
 */
public class DashboardSimpanPinjamAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	// Auto-wired dari ZUL (id harus cocok).
	private Div dashHost;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		DashboardUiKit.attachIntro(comp, "Dasbor Simpan Pinjam",
				"Lihat sekejap kondisi simpanan dan pinjaman koperasi: jumlah anggota, uang pinjaman yang "
						+ "berjalan, kelancaran pengembalian, tren penyaluran, dan peminjam terbesar.");
		buildDashboard();
	}

	/** Aksi tombol Segarkan pada ZUL. */
	public void onRefresh(Event event) throws Exception {
		buildDashboard();
	}

	/**
	 * Perbarui otomatis kualitas (kolektibilitas) seluruh pinjaman aktif berdasarkan lama tunggakan
	 * angsuran tertua — Lancar/Kurang Lancar/Ragu-ragu/Macet, memakai {@link KolektibilitasUtil}.
	 * Penyimpanan memakai sesi khusus {@link HibernateUtil#openSession()} dengan transaksi eksplisit
	 * yang <b>ditutup pada blok {@code finally}</b> (sesuai kaidah). Hanya pinjaman yang benar-benar
	 * berubah kelasnya yang disimpan, agar hemat operasi tulis.
	 */
	@SuppressWarnings("unchecked")
	public void onPerbaruiKolektibilitas(Event event) throws Exception {
		Long tipePinjaman = ConstantValues.PINJAMAN != null ? ConstantValues.PINJAMAN.getId() : null;
		if (tipePinjaman == null) {
			MyMessageboxConfig.show("Mohon maaf, tipe produk pinjaman belum tersedia di sistem. Langkah yang dapat dilakukan: (1) buka menu Master Produk Koperasi; (2) tambahkan minimal satu produk dengan tipe Pinjaman; (3) kembali ke halaman ini dan muat ulang.", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		int diperbarui = 0;
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.openSession();
			tx = session.beginTransaction();
			Date now = ais.ui.util.WaktuUtil.getDate();

			// Angsuran belum dibayar + pinjamannya (di-fetch), dikelompokkan per pinjaman.
			List<TransaksiKoperasiDetail> belum = session.createQuery(
					"select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
							+ "where d.pembayaranAnggotaKoperasiDetail is null "
							+ "and t.produkKoperasi.tipeProdukKoperasi.id = :tipe")
					.setParameter("tipe", tipePinjaman).list();

			Map<Long, List<TransaksiKoperasiDetail>> perPinjaman = new HashMap<Long, List<TransaksiKoperasiDetail>>();
			Map<Long, TransaksiKoperasi> pinjamanMap = new HashMap<Long, TransaksiKoperasi>();
			for (TransaksiKoperasiDetail d : belum) {
				try {
					TransaksiKoperasi t = d.getTransaksiKoperasi();
					if (t == null || t.getId() == null) {
						continue;
					}
					List<TransaksiKoperasiDetail> list = perPinjaman.get(t.getId());
					if (list == null) {
						list = new ArrayList<TransaksiKoperasiDetail>();
						perPinjaman.put(t.getId(), list);
						pinjamanMap.put(t.getId(), t);
					}
					list.add(d);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			for (Map.Entry<Long, TransaksiKoperasi> e : pinjamanMap.entrySet()) {
				try {
					TransaksiKoperasi t = e.getValue();
					if (!t.getAktif()) {
						continue;
					}
					String kolBaru = KolektibilitasUtil.hitung(perPinjaman.get(e.getKey()), now);
					if (!kolBaru.equals(t.getKolektibilitas())) {
						t.setKolektibilitas(kolBaru);
						session.update(t);
						diperbarui++;
					}
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
				}
			}

			tx.commit();
			tx = null;
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:172");
				}
			}
			Common.tampilErrorJikaAdmin(e);
			MyMessageboxConfig.show("Gagal memperbarui kolektibilitas: " + e.getMessage(), "Kesalahan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:183");
				}
			}
		}

		buildDashboard();
		MyMessageboxConfig.show(diperbarui + " pinjaman diperbarui kualitas (kolektibilitas)-nya sesuai lama tunggakan.",
				"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Pembangun dasbor
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Rangkai ulang seluruh isi dasbor ke dalam {@code dashHost}: kartu ringkasan, grafik
	 * kolektibilitas, tren penyaluran, peringkat peminjam, komposisi simpanan, dan tabel rincian
	 * pinjaman ber-Excel.
	 */
	private void buildDashboard() {
		if (dashHost == null) {
			return;
		}
		dashHost.getChildren().clear();

		Session session = HibernateUtil.currentSession();

		Long tipePinjaman = ConstantValues.PINJAMAN != null ? ConstantValues.PINJAMAN.getId() : null;
		Long tipeSimpanan = ConstantValues.SIMPANAN != null ? ConstantValues.SIMPANAN.getId() : null;

		List<TransaksiKoperasi> pinjaman = listAktifByTipe(session, tipePinjaman);
		List<TransaksiKoperasi> simpanan = listAktifByTipe(session, tipeSimpanan);
		List<TransaksiKoperasiDetail> angsuranBelum = listAngsuranBelumTerbayar(session, tipePinjaman);

		// Sisa pokok (outstanding) per akad pinjaman, dari angsuran yang belum terbayar.
		Map<Long, Double> sisaPerPinjaman = new java.util.HashMap<Long, Double>();
		double totalOutstandingPokok = 0.0;
		for (TransaksiKoperasiDetail d : angsuranBelum) {
			try {
				TransaksiKoperasi t = d.getTransaksiKoperasi();
				if (t == null || t.getId() == null) {
					continue;
				}
				Double akum = sisaPerPinjaman.get(t.getId());
				sisaPerPinjaman.put(t.getId(), (akum == null ? 0.0 : akum) + d.getPokok());
				totalOutstandingPokok += d.getPokok();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		double totalPokokTersalur = 0.0;
		for (TransaksiKoperasi t : pinjaman) {
			try {
				totalPokokTersalur += t.getNilai();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:238");
			}
		}
		double totalSimpanan = 0.0;
		for (TransaksiKoperasi t : simpanan) {
			try {
				totalSimpanan += t.getNilai();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:245");
			}
		}

		long jumlahAnggota = hitungAnggota(session);

		// ── Bagian visual (kartu + grafik) sebagai satu blok HTML/CSS ──
		try {
			StringBuilder sb = new StringBuilder();

			List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
			kartu.add(new DashboardUiKit.Stat("Total Anggota", DashboardUiKit.money(jumlahAnggota),
					"anggota terdaftar", DashboardUiKit.PRIMARY));
			kartu.add(new DashboardUiKit.Stat("Pinjaman Berjalan", DashboardUiKit.money(pinjaman.size()),
					"akad pinjaman aktif", DashboardUiKit.ACCENT));
			kartu.add(new DashboardUiKit.Stat("Pokok Tersalur", "Rp " + DashboardUiKit.money(totalPokokTersalur),
					"total pinjaman diberikan", DashboardUiKit.GOOD));
			kartu.add(new DashboardUiKit.Stat("Sisa Pokok Berjalan", "Rp " + DashboardUiKit.money(totalOutstandingPokok),
					"belum dikembalikan", DashboardUiKit.WARN));
			kartu.add(new DashboardUiKit.Stat("Total Simpanan", "Rp " + DashboardUiKit.money(totalSimpanan),
					"setoran simpanan anggota", DashboardUiKit.PRIMARY));
			sb.append(DashboardUiKit.descChip("Angka-angka penting simpan pinjam koperasi dalam sekejap."));
			sb.append(DashboardUiKit.cards(kartu));

			LinkedHashMap<String, Double> kol = hitungKolektibilitas(pinjaman);
			LinkedHashMap<String, Double> simpananPerProduk = jumlahkanPerProduk(simpanan);

			sb.append(DashboardUiKit.openGrid(300));
			sb.append(DashboardUiKit.donut("Kualitas Pinjaman (Kolektibilitas)",
					"Seberapa lancar anggota mengembalikan pinjaman: makin hijau makin sehat.", kol, false,
					"Belum ada pinjaman untuk dinilai."));
			sb.append(DashboardUiKit.donut("Komposisi Simpanan per Jenis",
					"Jenis simpanan mana yang paling banyak disetor anggota.", simpananPerProduk, true,
					"Belum ada simpanan tercatat."));
			sb.append(DashboardUiKit.closeGrid());

			// ── Rapor kesehatan (spider) + rincian angka mentah ──
			double modal = 0.0, tabungan = 0.0;
			for (TransaksiKoperasi t : simpanan) {
				try {
					String nm = t.getProdukKoperasi() == null || t.getProdukKoperasi().getNama() == null ? ""
							: t.getProdukKoperasi().getNama().toLowerCase();
					if (nm.contains("pokok") || nm.contains("wajib")) {
						modal += t.getNilai();
					} else {
						tabungan += t.getNilai();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:292");
				}
			}
			// Modal penyertaan aktif memperkuat modal sendiri sekaligus menambah dana terhimpun.
			double modalPenyertaan = totalModalPenyertaanAktif(session);
			modal += modalPenyertaan;
			double totalAset = totalSimpanan + modalPenyertaan; // estimasi dana terhimpun (kas + piutang pinjaman)
			double kas = Math.max(0.0, totalSimpanan + modalPenyertaan - totalOutstandingPokok);
			double outstandingLancar = 0.0;
			for (TransaksiKoperasiDetail d : angsuranBelum) {
				try {
					TransaksiKoperasi t = d.getTransaksiKoperasi();
					if (t != null && TransaksiKoperasi.KOL_LANCAR.equals(t.getKolektibilitas())) {
						outstandingLancar += d.getPokok();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:307");
				}
			}
			double totalMargin = 0.0;
			for (TransaksiKoperasi t : pinjaman) {
				try {
					totalMargin += t.getMargin();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:314");
				}
			}
			int rPermodalan = pct100(totalAset <= 0 ? 0 : modal / totalAset * 100.0);
			int rKap = totalOutstandingPokok <= 0 ? 100 : pct100(outstandingLancar / totalOutstandingPokok * 100.0);
			int rLikuiditas = tabungan <= 0 ? 100 : pct100(kas / tabungan * 100.0);
			int rSolvabilitas = tabungan <= 0 ? 100 : pct100(modal / tabungan * 100.0);
			int rRentabilitas = totalPokokTersalur <= 0 ? 0
					: pct100((totalMargin / totalPokokTersalur * 100.0) / 24.0 * 100.0);

			sb.append(DashboardUiKit.openGrid(300));
			sb.append(DashboardUiKit.spider("Rapor Kesehatan Simpan Pinjam",
					"Makin lebar jaringnya makin sehat: kecukupan modal, kualitas pinjaman, ketersediaan kas, "
							+ "kemampuan menutup simpanan, dan keuntungan.",
					new String[] { "Permodalan", "Kualitas Pinjaman", "Likuiditas", "Solvabilitas", "Rentabilitas" },
					new int[] { rPermodalan, rKap, rLikuiditas, rSolvabilitas, rRentabilitas }));
			LinkedHashMap<String, String> rincian = new LinkedHashMap<String, String>();
			rincian.put("Modal Sendiri (Pokok+Wajib+Penyertaan)", "Rp " + DashboardUiKit.money(modal));
				rincian.put("— termasuk Modal Penyertaan", "Rp " + DashboardUiKit.money(modalPenyertaan));
			rincian.put("Tabungan Sukarela (Kewajiban)", "Rp " + DashboardUiKit.money(tabungan));
			rincian.put("Piutang Pinjaman Berjalan", "Rp " + DashboardUiKit.money(totalOutstandingPokok));
			rincian.put("Estimasi Kas Tersedia", "Rp " + DashboardUiKit.money(kas));
			rincian.put("Permodalan (Modal / Aset)", rPermodalan + " dari 100");
			rincian.put("Kualitas Pinjaman (% Lancar)", rKap + " dari 100");
			rincian.put("Likuiditas (Kas / Tabungan)", rLikuiditas + " dari 100");
			rincian.put("Solvabilitas (Modal / Kewajiban)", rSolvabilitas + " dari 100");
			rincian.put("Rentabilitas (Imbal Hasil / 24%)", rRentabilitas + " dari 100");
			sb.append(DashboardUiKit.insight("Rincian Angka Kesehatan",
					"Angka mentah di balik rapor di atas, agar bisa ditelusuri (bukan sekadar bentuk grafik).",
					rincian));
			sb.append(DashboardUiKit.closeGrid());

			// ── Penilaian Kesehatan FORMAL: satu skor menyeluruh + predikat + rincian bobot ──
			double skorKes = KesehatanUtil.skor(rPermodalan, rKap, rLikuiditas, rSolvabilitas, rRentabilitas);
			String predikatKes = KesehatanUtil.predikat(skorKes);
			String warnaKes = KesehatanUtil.warna(skorKes);
			sb.append("<div style='font-size:14px;font-weight:800;color:#0f172a;margin:10px 0 2px;"
					+ "border-left:4px solid " + warnaKes + ";padding-left:8px;'>Penilaian Kesehatan (Skor &amp; Predikat)</div>");
			sb.append(DashboardUiKit.descChip("Nilai kesehatan koperasi secara keseluruhan beserta predikatnya, "
					+ "dihitung berbobot dari lima aspek di atas. Makin tinggi skor makin sehat."));
			sb.append("<div style='display:flex;flex-wrap:wrap;gap:16px;align-items:center;background:#ffffff;"
					+ "border:1px solid #e2e8f0;border-left:5px solid " + warnaKes + ";border-radius:12px;"
					+ "padding:14px 16px;margin:4px 0 10px;box-shadow:0 2px 8px rgba(15,23,42,.05);'>");
			sb.append("<div style='text-align:center;min-width:96px;'>"
					+ "<div style='font-size:36px;font-weight:800;color:" + warnaKes + ";line-height:1;'>"
					+ Math.round(skorKes) + "</div>"
					+ "<div style='font-size:10px;color:#64748b;text-transform:uppercase;font-weight:800;letter-spacing:.5px;'>dari 100</div></div>");
			sb.append("<div style='flex:1;min-width:220px;'>"
					+ "<div style='font-size:18px;font-weight:800;color:" + warnaKes + ";'>Predikat: " + predikatKes + "</div>"
					+ "<div style='font-size:12px;color:#475569;'>Perbaiki aspek berskor rendah pada tabel di bawah "
					+ "untuk menaikkan predikat.</div></div></div>");
			sb.append("<table style='border-collapse:collapse;width:100%;font-size:12px;margin-bottom:12px;'>");
			sb.append("<thead><tr style='background:#f1f5f9;'>"
					+ "<th style='padding:5px 8px;border:1px solid #e2e8f0;text-align:left;'>Aspek</th>"
					+ "<th style='padding:5px 8px;border:1px solid #e2e8f0;'>Skor (0&#8211;100)</th>"
					+ "<th style='padding:5px 8px;border:1px solid #e2e8f0;'>Bobot</th>"
					+ "<th style='padding:5px 8px;border:1px solid #e2e8f0;'>Nilai Tertimbang</th></tr></thead><tbody>");
			sb.append(barisKesehatan("Permodalan", rPermodalan, KesehatanUtil.BOBOT_PERMODALAN));
			sb.append(barisKesehatan("Kualitas Pinjaman", rKap, KesehatanUtil.BOBOT_KAP));
			sb.append(barisKesehatan("Likuiditas", rLikuiditas, KesehatanUtil.BOBOT_LIKUIDITAS));
			sb.append(barisKesehatan("Solvabilitas", rSolvabilitas, KesehatanUtil.BOBOT_SOLVABILITAS));
			sb.append(barisKesehatan("Rentabilitas", rRentabilitas, KesehatanUtil.BOBOT_RENTABILITAS));
			sb.append("<tr style='font-weight:800;background:#f8fafc;'>"
					+ "<td colspan='3' style='padding:6px 8px;border:1px solid #e2e8f0;text-align:right;'>SKOR KESEHATAN</td>"
					+ "<td style='padding:6px 8px;border:1px solid #e2e8f0;text-align:right;color:" + warnaKes + ";'>"
					+ Math.round(skorKes) + " (" + predikatKes + ")</td></tr>");
			sb.append("</tbody></table>");

			List<Double> tren = trenPenyaluran12Bulan(pinjaman);
			sb.append(DashboardUiKit.sparkline("Tren Penyaluran Pinjaman (12 Bulan)",
					"Naik-turunnya jumlah pinjaman yang diberikan tiap bulan.", tren, DashboardUiKit.ACCENT,
					"Belum ada penyaluran pinjaman."));

			LinkedHashMap<String, Double> topPeminjam = topPeminjam(angsuranBelum, 10);
			LinkedHashMap<String, Double> pinjamanPerProduk = jumlahkanPerProduk(pinjaman);
			sb.append(DashboardUiKit.openGrid(320));
			sb.append(DashboardUiKit.barList("10 Peminjam dengan Sisa Terbesar",
					"Anggota yang paling besar sisa pinjamannya — perlu perhatian penagihan.", topPeminjam,
					DashboardUiKit.PRIMARY, "", true, "Belum ada sisa pinjaman berjalan."));
			sb.append(DashboardUiKit.barList("Penyaluran per Jenis Pinjaman",
					"Produk pinjaman mana yang paling banyak disalurkan.", pinjamanPerProduk, DashboardUiKit.GOOD, "",
					true, "Belum ada penyaluran pinjaman."));
			sb.append(DashboardUiKit.closeGrid());

			dashHost.appendChild(DashboardUiKit.html(sb.toString()));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			dashHost.appendChild(DashboardUiKit.html(
					DashboardUiKit.descChip("Sebagian grafik gagal ditampilkan. Silakan tekan Segarkan.")));
		}

		// ── Tabel rincian pinjaman + Unduh Excel (reuse util) ──
		try {
			List<Object[]> rows = new ArrayList<Object[]>();
			for (TransaksiKoperasi t : pinjaman) {
				try {
					Double sisa = sisaPerPinjaman.get(t.getId());
					String anggota = t.getAnggotaKoperasi() == null ? "-" : t.getAnggotaKoperasi().getNama();
					String produk = t.getProdukKoperasi() == null ? "-" : t.getProdukKoperasi().getNama();
					rows.add(new Object[] { anggota, produk, Double.valueOf(t.getNilai()), Double.valueOf(t.getMargin()),
							Double.valueOf(t.getTotal()), Double.valueOf(sisa == null ? 0.0 : sisa),
							t.getKolektibilitasLabel(), t.getStatus() });
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
			SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Rincian Pinjaman Anggota",
					"Daftar setiap pinjaman: pokok, bunga, total, sisa yang belum dibayar, dan kualitasnya.",
					"Pinjaman", "rekap_pinjaman",
					new String[] { "Anggota", "Produk", "Pokok", "Bunga", "Total", "Sisa Pokok", "Kolektibilitas",
							"Status" },
					new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.RUPIAH,
							SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.RUPIAH,
							SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.TEKS },
					rows);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Pengambilan data (Hibernate) — memakai property entity yang sudah terverifikasi
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Ambil daftar akad {@link TransaksiKoperasi} untuk satu tipe produk (pinjaman/simpanan), sudah
	 * mem-fetch produk &amp; anggota untuk menekan query N+1, lalu disaring hanya yang aktif memakai
	 * {@link TransaksiKoperasi#getAktif()} (menghormati nilai default {@code true} saat null).
	 */
	@SuppressWarnings("unchecked")
	private List<TransaksiKoperasi> listAktifByTipe(Session session, Long tipeId) {
		List<TransaksiKoperasi> hasil = new ArrayList<TransaksiKoperasi>();
		if (tipeId == null) {
			return hasil;
		}
		try {
			List<TransaksiKoperasi> semua = session.createQuery(
					"select distinct t from TransaksiKoperasi t left join fetch t.produkKoperasi p "
							+ "left join fetch t.anggotaKoperasi a where p.tipeProdukKoperasi.id = :tipe")
					.setParameter("tipe", tipeId).list();
			for (TransaksiKoperasi t : semua) {
				try {
					if (t.getAktif()) {
						hasil.add(t);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:459");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return hasil;
	}

	/**
	 * Ambil baris {@link TransaksiKoperasiDetail} (angsuran) yang <b>belum terbayar</b> untuk akad
	 * bertipe pinjaman — yaitu yang belum tertaut ke sebuah pembayaran anggota.
	 */
	@SuppressWarnings("unchecked")
	private List<TransaksiKoperasiDetail> listAngsuranBelumTerbayar(Session session, Long tipePinjaman) {
		List<TransaksiKoperasiDetail> hasil = new ArrayList<TransaksiKoperasiDetail>();
		if (tipePinjaman == null) {
			return hasil;
		}
		try {
			List<TransaksiKoperasiDetail> semua = session.createQuery(
					"select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
							+ "left join fetch t.anggotaKoperasi a where d.pembayaranAnggotaKoperasiDetail is null "
							+ "and t.produkKoperasi.tipeProdukKoperasi.id = :tipe")
					.setParameter("tipe", tipePinjaman).list();
			// Samakan dengan listAktifByTipe: hanya angsuran dari akad pinjaman yang AKTIF, agar
			// KPI "Sisa Pokok", kualitas pinjaman (KAP), dan estimasi kas dihitung atas populasi
			// pinjaman yang sama (tidak mencampur akad non-aktif/dibatalkan).
			for (TransaksiKoperasiDetail d : semua) {
				try {
					TransaksiKoperasi t = d.getTransaksiKoperasi();
					if (t != null && t.getAktif()) {
						hasil.add(d);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:493");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return hasil;
	}

	/** Hitung jumlah anggota koperasi terdaftar. */
	private long hitungAnggota(Session session) {
		try {
			Number n = (Number) session.createQuery("select count(a.id) from AnggotaKoperasi a").uniqueResult();
			return n == null ? 0L : n.longValue();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0L;
		}
	}

	/**
	 * Jumlah nominal modal penyertaan yang masih berstatus aktif. Dipakai untuk memperkuat
	 * perhitungan Modal Sendiri pada rapor kesehatan. Aman-null; mengembalikan 0 bila belum ada.
	 */
	private double totalModalPenyertaanAktif(Session session) {
		double total = 0.0;
		try {
			List<?> list = session.createCriteria(ais.database.model.koperasi.ModalPenyertaanKoperasi.class)
					.add(org.hibernate.criterion.Restrictions.eq("status",
							ais.database.model.koperasi.ModalPenyertaanKoperasi.STATUS_AKTIF))
					.add(org.hibernate.criterion.Restrictions.or(
							org.hibernate.criterion.Restrictions.isNull("aktif"),
							org.hibernate.criterion.Restrictions.eq("aktif", true)))
					.list();
			for (Object o : list) {
				try {
					total += ((ais.database.model.koperasi.ModalPenyertaanKoperasi) o).getNominal();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:530");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return total;
	}

	/** Batasi sebuah persen ke rentang [0,100] lalu bulatkan ke bilangan bulat (untuk sumbu spider). */
	private static int pct100(double v) {
		if (v < 0) {
			return 0;
		}
		if (v > 100) {
			return 100;
		}
		return (int) Math.round(v);
	}

	/**
	 * Susun satu baris tabel penilaian kesehatan: nama aspek, skornya, bobotnya, dan nilai
	 * tertimbang (skor &times; bobot / 100). Dipakai berulang oleh blok Penilaian Kesehatan agar
	 * ringkas dan seragam.
	 *
	 * @param nama  nama aspek yang ditampilkan
	 * @param skor  skor aspek 0&ndash;100
	 * @param bobot bobot aspek dalam persen
	 * @return potongan HTML satu baris {@code <tr>}
	 */
	private static String barisKesehatan(String nama, int skor, int bobot) {
		double tertimbang = skor * bobot / 100.0;
		StringBuilder r = new StringBuilder();
		r.append("<tr>")
				.append("<td style='padding:5px 8px;border:1px solid #e2e8f0;'>").append(DashboardUiKit.esc(nama)).append("</td>")
				.append("<td style='padding:5px 8px;border:1px solid #e2e8f0;text-align:center;'>").append(skor).append("</td>")
				.append("<td style='padding:5px 8px;border:1px solid #e2e8f0;text-align:center;'>").append(bobot).append("%</td>")
				.append("<td style='padding:5px 8px;border:1px solid #e2e8f0;text-align:right;'>")
				.append(DashboardUiKit.money(Math.round(tertimbang * 10.0) / 10.0)).append("</td>")
				.append("</tr>");
		return r.toString();
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Agregasi (di memori)
	// ════════════════════════════════════════════════════════════════════════════════════════

	/** Peta jumlah pinjaman per kategori kolektibilitas (urut: Lancar → Macet). */
	private LinkedHashMap<String, Double> hitungKolektibilitas(List<TransaksiKoperasi> pinjaman) {
		double lancar = 0, kurang = 0, ragu = 0, macet = 0;
		for (TransaksiKoperasi t : pinjaman) {
			try {
				String k = t.getKolektibilitas();
				if (TransaksiKoperasi.KOL_MACET.equals(k)) {
					macet++;
				} else if (TransaksiKoperasi.KOL_RAGU.equals(k)) {
					ragu++;
				} else if (TransaksiKoperasi.KOL_KURANG_LANCAR.equals(k)) {
					kurang++;
				} else {
					lancar++;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:592");
			}
		}
		LinkedHashMap<String, Double> map = new LinkedHashMap<String, Double>();
		map.put("Lancar", Double.valueOf(lancar));
		map.put("Kurang Lancar", Double.valueOf(kurang));
		map.put("Ragu-ragu", Double.valueOf(ragu));
		map.put("Macet", Double.valueOf(macet));
		return map;
	}

	/** Jumlahkan nilai akad per nama produk (untuk komposisi/penyaluran). */
	private LinkedHashMap<String, Double> jumlahkanPerProduk(List<TransaksiKoperasi> list) {
		LinkedHashMap<String, Double> map = new LinkedHashMap<String, Double>();
		for (TransaksiKoperasi t : list) {
			try {
				String nama = t.getProdukKoperasi() == null ? "Lainnya" : t.getProdukKoperasi().getNama();
				nama = DashboardUiKit.shorten(nama == null ? "Lainnya" : nama, 22);
				Double v = map.get(nama);
				map.put(nama, Double.valueOf((v == null ? 0.0 : v.doubleValue()) + t.getNilai()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:612");
			}
		}
		return map;
	}

	/** Daftar total penyaluran pinjaman 12 bulan terakhir (indeks 0 = 11 bulan lalu). */
	private List<Double> trenPenyaluran12Bulan(List<TransaksiKoperasi> pinjaman) {
		Calendar now = ais.ui.util.WaktuUtil.getCalendar();
		int baseTahun = now.get(Calendar.YEAR);
		int baseBulan = now.get(Calendar.MONTH); // 0-based
		LinkedHashMap<String, Double> bucket = new LinkedHashMap<String, Double>();
		String[] urut = new String[12];
		for (int i = 11; i >= 0; i--) {
			int m = baseBulan - i;
			int y = baseTahun;
			while (m < 0) {
				m += 12;
				y--;
			}
			String key = y + "-" + (m + 1);
			urut[11 - i] = key;
			bucket.put(key, Double.valueOf(0.0));
		}
		for (TransaksiKoperasi t : pinjaman) {
			try {
				java.util.Date tgl = t.getTanggalTransaksi();
				if (tgl == null) {
					continue;
				}
				Calendar c = ais.ui.util.WaktuUtil.getCalendar();
				c.setTime(tgl);
				String key = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1);
				if (bucket.containsKey(key)) {
					bucket.put(key, Double.valueOf(bucket.get(key).doubleValue() + t.getNilai()));
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:648");
			}
		}
		List<Double> hasil = new ArrayList<Double>();
		for (String k : urut) {
			hasil.add(bucket.get(k));
		}
		return hasil;
	}

	/** Peringkat anggota berdasarkan total sisa pokok pinjaman (terbesar dulu), maksimal {@code max}. */
	private LinkedHashMap<String, Double> topPeminjam(List<TransaksiKoperasiDetail> angsuranBelum, int max) {
		final Map<String, Double> agg = new java.util.HashMap<String, Double>();
		for (TransaksiKoperasiDetail d : angsuranBelum) {
			try {
				TransaksiKoperasi t = d.getTransaksiKoperasi();
				AnggotaKoperasi a = t == null ? null : t.getAnggotaKoperasi();
				String nama = a == null || a.getNama() == null ? "-" : a.getNama();
				Double v = agg.get(nama);
				agg.put(nama, Double.valueOf((v == null ? 0.0 : v.doubleValue()) + d.getPokok()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardSimpanPinjamAction.java:668");
			}
		}
		List<Map.Entry<String, Double>> entries = new ArrayList<Map.Entry<String, Double>>(agg.entrySet());
		java.util.Collections.sort(entries, new java.util.Comparator<Map.Entry<String, Double>>() {
			@Override
			public int compare(Map.Entry<String, Double> a, Map.Entry<String, Double> b) {
				return Double.compare(b.getValue().doubleValue(), a.getValue().doubleValue());
			}
		});
		LinkedHashMap<String, Double> map = new LinkedHashMap<String, Double>();
		int n = 0;
		for (Map.Entry<String, Double> e : entries) {
			if (n++ >= max) {
				break;
			}
			map.put(DashboardUiKit.shorten(e.getKey(), 22), e.getValue());
		}
		return map;
	}
}
