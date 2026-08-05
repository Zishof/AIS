package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;

import ais.action.master.koperasi.helper.LaporanKeuanganCoaHelper;
import ais.action.master.koperasi.helper.SimpanPinjamUiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.koperasi.ModalPenyertaanKoperasi;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyDatebox;

/**
 * <h2>LaporanKeuanganKoperasiAction — Laporan Keuangan Ringkas (Neraca, Hasil Usaha, Arus Kas)</h2>
 *
 * <p>
 * Composer ZK ini menyusun <b>gambaran posisi keuangan koperasi secara ringkas</b> — Neraca
 * (posisi harta, kewajiban, dan modal), Perhitungan Hasil Usaha/PHU (pendapatan dan hasil usaha),
 * serta Arus Kas ringkas (uang masuk dan keluar) — langsung dari data kegiatan simpan pinjam yang
 * sudah ada. Tujuannya membantu pengurus dan anggota membaca kondisi keuangan koperasi dengan cepat
 * dan visual, sebelum laporan keuangan resmi yang lengkap disusun bersama modul akuntansi.
 * </p>
 *
 * <h3>Kejujuran penyajian (managerial, bukan neraca audited)</h3>
 * <p>
 * Angka pada laporan ini <b>diturunkan dari transaksi simpan pinjam</b>, bukan dari buku besar
 * akuntansi lengkap. Karena itu:
 * </p>
 * <ul>
 * <li><b>Neraca</b> dibentuk dari: Aset = Kas (estimasi) + Piutang Pinjaman (sisa pinjaman berjalan);
 * Kewajiban = Simpanan Sukarela anggota; Modal = Simpanan Pokok + Simpanan Wajib. Secara konstruksi,
 * Total Aset = Kewajiban + Modal (seimbang).</li>
 * <li><b>Perhitungan Hasil Usaha</b> menampilkan pendapatan yang terukur (jasa/bunga pinjaman yang
 * telah dibayar). Biaya operasional (gaji, ATK, dsb.) dan bunga simpanan yang dibayarkan ke anggota
 * dicatat di modul akuntansi, sehingga diberi catatan agar tidak salah tafsir.</li>
 * <li><b>Arus Kas</b> meringkas uang masuk (setoran simpanan, angsuran, jasa) dan keluar (penyaluran
 * pinjaman) dari kegiatan simpan pinjam.</li>
 * </ul>
 * <p>
 * Seluruh tabel dapat diunduh ke Excel dan seluruh grafik memakai {@link DashboardUiKit} (HTML/CSS,
 * tanpa JFreeChart), konsisten dengan dasbor lain dan mudah dipelihara.
 * </p>
 *
 * <h3>Sumber data</h3>
 * <p>
 * Simpanan &amp; pinjaman adalah {@link TransaksiKoperasi} yang dibedakan lewat tipe produk
 * ({@link ConstantValues#SIMPANAN} vs {@link ConstantValues#PINJAMAN}); sisa pinjaman &amp; jasa yang
 * dibayar dihitung dari {@link TransaksiKoperasiDetail}. Session memakai
 * {@link HibernateUtil#currentSession()} (ditutup otomatis). Setiap bagian dibungkus {@code try/catch}
 * mandiri. Kompatibel Java 1.7.
 * </p>
 *
 * @see SimpanPinjamUiUtil
 * @see DashboardUiKit
 */
public class LaporanKeuanganKoperasiAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	private Div dashHost;
	private Combobox jenisLaporan;
	private MyDatebox tglAwal;
	private MyDatebox tglSampai;

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
		DashboardUiKit.attachIntro(comp, "Laporan Keuangan Koperasi",
				"Gambaran cepat kondisi keuangan koperasi (ringkas) plus laporan resmi dari buku besar "
						+ "(Neraca / Laba Rugi / Arus Kas) dan Catatan atas Laporan Keuangan.");
		muatPilihanLaporan();
		buildLaporan();
	}

	/**
	 * Isi combo Jenis Laporan (Neraca/Laba Rugi/Arus Kas) dari jenis laporan yang sudah dikonfigurasi
	 * pada modul akuntansi (yang memiliki pemetaan KelompokLaporan), serta beri tanggal awal-akhir
	 * bawaan (awal tahun s/d hari ini). Aman bila komponen tidak ada pada ZUL lama.
	 */
	@SuppressWarnings("unchecked")
	private void muatPilihanLaporan() {
		try {
			if (jenisLaporan != null) {
				List<JenisLaporan> jenis = HibernateUtil.currentSession().createQuery(
						"select distinct kl.jenisLaporan from KelompokLaporan kl where kl.jenisLaporan is not null")
						.list();
				// PostgreSQL menolak "SELECT DISTINCT ... ORDER BY <kolom di luar select list>"
				// (nama lewat join tidak ada di select). ORDER BY dipindah ke Java: urut nama
				// (case-insensitive, null aman). Hasil tetap distinct JenisLaporan terurut nama.
				java.util.Collections.sort(jenis, new java.util.Comparator<JenisLaporan>() {
					@Override
					public int compare(JenisLaporan a, JenisLaporan b) {
						String na = (a == null || a.getNama() == null) ? "" : a.getNama();
						String nb = (b == null || b.getNama() == null) ? "" : b.getNama();
						return na.compareToIgnoreCase(nb);
					}
				});
				jenisLaporan.getChildren().clear();
				for (JenisLaporan jl : jenis) {
					Comboitem ci = new Comboitem(jl.getNama() == null ? "(tanpa nama)" : jl.getNama());
					ci.setValue(jl);
					jenisLaporan.appendChild(ci);
				}
				if (!jenis.isEmpty()) {
					jenisLaporan.setSelectedIndex(0);
				}
				jenisLaporan.setReadonly(true);
			}
			Calendar c = ais.ui.util.WaktuUtil.getCalendar();
			Date sampai = c.getTime();
			c.set(Calendar.MONTH, Calendar.JANUARY);
			c.set(Calendar.DAY_OF_MONTH, 1);
			Date awal = c.getTime();
			if (tglAwal != null && tglAwal.getValue() == null) {
				tglAwal.setValue(awal);
			}
			if (tglSampai != null && tglSampai.getValue() == null) {
				tglSampai.setValue(sampai);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Tampilkan laporan keuangan resmi (Neraca/Laba Rugi/Arus Kas) dari buku besar terposting sesuai
	 * jenis laporan &amp; periode terpilih, memakai pemetaan akun yang terkonfigurasi
	 * ({@link LaporanKeuanganCoaHelper}). Read-only, {@code currentSession()}.
	 */
	public void onTampilkanCoa(Event event) throws Exception {
		if (dashHost == null) {
			return;
		}
		JenisLaporan jl = (JenisLaporan) (jenisLaporan == null || jenisLaporan.getSelectedItem() == null ? null
				: jenisLaporan.getSelectedItem().getValue());
		if (jl == null) {
			ais.ui.util.MyMessageboxConfig.show(
					"Belum ada jenis laporan keuangan yang terkonfigurasi di modul akuntansi (KelompokLaporan).",
					"Informasi", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
			return;
		}
		dashHost.getChildren().clear();
		Date awal = tglAwal == null ? null : tglAwal.getValue();
		Date sampai = tglSampai == null ? null : tglSampai.getValue();
		LaporanKeuanganCoaHelper.render(dashHost, HibernateUtil.currentSession(), jl, awal, sampai);
	}

	/** Aksi tombol CALK. */
	public void onCalk(Event event) throws Exception {
		buildCalk();
	}

	/** Aksi tombol Segarkan. */
	public void onRefresh(Event event) throws Exception {
		buildLaporan();
	}

	@SuppressWarnings("unchecked")
	private void buildLaporan() {
		if (dashHost == null) {
			return;
		}
		dashHost.getChildren().clear();

		Session session = HibernateUtil.currentSession();
		Long tipeSimpanan = ConstantValues.SIMPANAN != null ? ConstantValues.SIMPANAN.getId() : null;
		Long tipePinjaman = ConstantValues.PINJAMAN != null ? ConstantValues.PINJAMAN.getId() : null;

		// ── Kumpulkan angka dasar ──
		double simpananPokok = 0, simpananWajib = 0, simpananSukarela = 0, totalPokokTersalur = 0;
		double outstandingPokok = 0, jasaDiterima = 0, angsuranPokokDiterima = 0;
		double outLancar = 0, outKurang = 0, outRagu = 0, outMacet = 0;

		try {
			if (tipeSimpanan != null) {
				List<TransaksiKoperasi> simp = session.createQuery(
						"select distinct t from TransaksiKoperasi t left join fetch t.produkKoperasi p "
								+ "where p.tipeProdukKoperasi.id = :tipe").setParameter("tipe", tipeSimpanan).list();
				for (TransaksiKoperasi t : simp) {
					try {
						String nm = t.getProdukKoperasi() == null || t.getProdukKoperasi().getNama() == null ? ""
								: t.getProdukKoperasi().getNama().toLowerCase();
						if (nm.contains("pokok")) {
							simpananPokok += t.getNilai();
						} else if (nm.contains("wajib")) {
							simpananWajib += t.getNilai();
						} else {
							simpananSukarela += t.getNilai();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/LaporanKeuanganKoperasiAction.java:214");
					}
				}
			}
			if (tipePinjaman != null) {
				List<TransaksiKoperasi> pinj = session.createQuery(
						"select distinct t from TransaksiKoperasi t left join fetch t.produkKoperasi p "
								+ "where p.tipeProdukKoperasi.id = :tipe").setParameter("tipe", tipePinjaman).list();
				for (TransaksiKoperasi t : pinj) {
					try {
						if (t.getAktif()) {
							totalPokokTersalur += t.getNilai();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/LaporanKeuanganKoperasiAction.java:227");
					}
				}
				List<TransaksiKoperasiDetail> belum = session.createQuery(
						"select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
								+ "where d.pembayaranAnggotaKoperasiDetail is null "
								+ "and t.produkKoperasi.tipeProdukKoperasi.id = :tipe")
						.setParameter("tipe", tipePinjaman).list();
				for (TransaksiKoperasiDetail d : belum) {
					outstandingPokok += d.getPokok();
					try {
						TransaksiKoperasi tk = d.getTransaksiKoperasi();
						String kol = tk == null ? TransaksiKoperasi.KOL_LANCAR : tk.getKolektibilitas();
						if (TransaksiKoperasi.KOL_MACET.equals(kol)) {
							outMacet += d.getPokok();
						} else if (TransaksiKoperasi.KOL_RAGU.equals(kol)) {
							outRagu += d.getPokok();
						} else if (TransaksiKoperasi.KOL_KURANG_LANCAR.equals(kol)) {
							outKurang += d.getPokok();
						} else {
							outLancar += d.getPokok();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/LaporanKeuanganKoperasiAction.java:249");
					}
				}
				List<TransaksiKoperasiDetail> lunas = session.createQuery(
						"select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
								+ "where d.pembayaranAnggotaKoperasiDetail is not null "
								+ "and t.produkKoperasi.tipeProdukKoperasi.id = :tipe")
						.setParameter("tipe", tipePinjaman).list();
				for (TransaksiKoperasiDetail d : lunas) {
					jasaDiterima += d.getMargin();
					angsuranPokokDiterima += d.getPokok();
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		double totalSimpanan = simpananPokok + simpananWajib + simpananSukarela;
		double modal = simpananPokok + simpananWajib;
		double kewajiban = simpananSukarela;
		double kas = Math.max(0.0, totalSimpanan - outstandingPokok);
		double piutang = outstandingPokok;
		double totalAset = kas + piutang;

		buildNeraca(kas, piutang, totalAset, kewajiban, simpananPokok, simpananWajib, modal);
		buildHasilUsaha(jasaDiterima);
		buildArusKas(totalSimpanan, angsuranPokokDiterima, jasaDiterima, totalPokokTersalur);
		buildPpap(outLancar, outKurang, outRagu, outMacet);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// PPAP — Penyisihan Penghapusan Aktiva Produktif (cadangan risiko pinjaman)
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Tampilkan estimasi <b>PPAP (Penyisihan Penghapusan Aktiva Produktif)</b>: dana cadangan yang
	 * sebaiknya disiapkan koperasi untuk mengantisipasi pinjaman yang berpotensi tak tertagih. Nilai
	 * cadangan dihitung dari sisa pinjaman berjalan pada tiap tingkat kolektibilitas dikali persentase
	 * risikonya (makin buruk kolektibilitas, makin besar cadangan): Lancar 0,5%, Kurang Lancar 10%,
	 * Ragu-ragu 50%, Macet 100%.
	 *
	 * @param outLancar sisa pokok pinjaman berkolektibilitas Lancar
	 * @param outKurang sisa pokok pinjaman Kurang Lancar
	 * @param outRagu   sisa pokok pinjaman Ragu-ragu
	 * @param outMacet  sisa pokok pinjaman Macet
	 */
	private void buildPpap(double outLancar, double outKurang, double outRagu, double outMacet) {
		double pLancar = outLancar * 0.005;
		double pKurang = outKurang * 0.10;
		double pRagu = outRagu * 0.50;
		double pMacet = outMacet * 1.00;
		double totalPpap = pLancar + pKurang + pRagu + pMacet;

		try {
			List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
			kartu.add(new DashboardUiKit.Stat("Cadangan Risiko (PPAP)", "Rp " + DashboardUiKit.money(totalPpap),
					"dana antisipasi gagal bayar", DashboardUiKit.BAD));
			kartu.add(new DashboardUiKit.Stat("Pinjaman Berisiko (Non-Lancar)",
					"Rp " + DashboardUiKit.money(outKurang + outRagu + outMacet), "kurang lancar s/d macet",
					DashboardUiKit.WARN));
			dashHost.appendChild(DashboardUiKit.html("<div style='font-size:14px;font-weight:800;color:#0f172a;"
					+ "margin:18px 0 4px;border-left:4px solid " + DashboardUiKit.PRIMARY + ";padding-left:8px;'>"
					+ "Penyisihan Penghapusan Pinjaman (PPAP)</div>"));
			dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
					"Perkiraan dana cadangan yang sebaiknya disiapkan untuk berjaga-jaga terhadap pinjaman "
							+ "yang berpotensi tidak tertagih.")));

			LinkedHashMap<String, Double> komposisi = new LinkedHashMap<String, Double>();
			komposisi.put("Lancar (0,5%)", Double.valueOf(pLancar));
			komposisi.put("Kurang Lancar (10%)", Double.valueOf(pKurang));
			komposisi.put("Ragu-ragu (50%)", Double.valueOf(pRagu));
			komposisi.put("Macet (100%)", Double.valueOf(pMacet));
			dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.cards(kartu)));
			dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.barList("Cadangan Risiko per Tingkat Kolektibilitas",
					"Semakin buruk kualitas pinjaman, semakin besar cadangan yang perlu disiapkan.", komposisi,
					DashboardUiKit.BAD, "", true, "Belum ada pinjaman berjalan.")));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		List<Object[]> rows = new ArrayList<Object[]>();
		rows.add(new Object[] { "Lancar", Double.valueOf(outLancar), "0,5%", Double.valueOf(pLancar) });
		rows.add(new Object[] { "Kurang Lancar", Double.valueOf(outKurang), "10%", Double.valueOf(pKurang) });
		rows.add(new Object[] { "Ragu-ragu", Double.valueOf(outRagu), "50%", Double.valueOf(pRagu) });
		rows.add(new Object[] { "Macet", Double.valueOf(outMacet), "100%", Double.valueOf(pMacet) });
		rows.add(new Object[] { "TOTAL PPAP", Double.valueOf(outLancar + outKurang + outRagu + outMacet), "",
				Double.valueOf(totalPpap) });
		SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Rincian PPAP",
				"Cadangan risiko untuk tiap tingkat kualitas pinjaman.", "PPAP", "ppap_koperasi",
				new String[] { "Kolektibilitas", "Sisa Pinjaman", "Persentase", "Cadangan (PPAP)" },
				new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.TEKS,
						SimpanPinjamUiUtil.RUPIAH },
				rows);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Neraca (posisi keuangan)
	// ════════════════════════════════════════════════════════════════════════════════════════

	/** Tampilkan Neraca ringkas: kartu, donut komposisi aset vs pasiva, dan tabel + Excel. */
	private void buildNeraca(double kas, double piutang, double totalAset, double kewajiban, double simpananPokok,
			double simpananWajib, double modal) {
		try {
			StringBuilder sb = new StringBuilder();
			List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
			kartu.add(new DashboardUiKit.Stat("Total Aset", "Rp " + DashboardUiKit.money(totalAset),
					"harta koperasi", DashboardUiKit.PRIMARY));
			kartu.add(new DashboardUiKit.Stat("Piutang Pinjaman", "Rp " + DashboardUiKit.money(piutang),
					"sisa pinjaman berjalan", DashboardUiKit.ACCENT));
			kartu.add(new DashboardUiKit.Stat("Kewajiban (Tabungan)", "Rp " + DashboardUiKit.money(kewajiban),
					"simpanan sukarela anggota", DashboardUiKit.WARN));
			kartu.add(new DashboardUiKit.Stat("Modal Sendiri", "Rp " + DashboardUiKit.money(modal),
					"simpanan pokok + wajib", DashboardUiKit.GOOD));
			sb.append("<div style='font-size:14px;font-weight:800;color:#0f172a;"
					+ "margin:6px 0 4px;border-left:4px solid " + DashboardUiKit.PRIMARY + ";padding-left:8px;'>"
					+ "Neraca (Posisi Keuangan Ringkas)</div>");
			sb.append(DashboardUiKit.descChip("Perbandingan harta koperasi dengan kewajiban dan modalnya."));
			sb.append(DashboardUiKit.cards(kartu));

			LinkedHashMap<String, Double> aset = new LinkedHashMap<String, Double>();
			aset.put("Kas & Setara", Double.valueOf(kas));
			aset.put("Piutang Pinjaman", Double.valueOf(piutang));
			LinkedHashMap<String, Double> pasiva = new LinkedHashMap<String, Double>();
			pasiva.put("Simpanan Pokok", Double.valueOf(simpananPokok));
			pasiva.put("Simpanan Wajib", Double.valueOf(simpananWajib));
			pasiva.put("Simpanan Sukarela", Double.valueOf(kewajiban));

			sb.append(DashboardUiKit.openGrid(300));
			sb.append(DashboardUiKit.donut("Susunan Aset", "Harta koperasi berupa kas dan piutang pinjaman.", aset,
					true, "Belum ada aset."));
			sb.append(DashboardUiKit.donut("Susunan Kewajiban & Modal", "Dari mana dana koperasi berasal.", pasiva,
					true, "Belum ada simpanan."));
			sb.append(DashboardUiKit.closeGrid());
			dashHost.appendChild(DashboardUiKit.html(sb.toString()));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		List<Object[]> rows = new ArrayList<Object[]>();
		rows.add(new Object[] { "ASET", null });
		rows.add(new Object[] { "  Kas & Setara Kas", Double.valueOf(kas) });
		rows.add(new Object[] { "  Piutang Pinjaman", Double.valueOf(piutang) });
		rows.add(new Object[] { "  TOTAL ASET", Double.valueOf(totalAset) });
		rows.add(new Object[] { "KEWAJIBAN", null });
		rows.add(new Object[] { "  Simpanan Sukarela (Tabungan)", Double.valueOf(kewajiban) });
		rows.add(new Object[] { "MODAL", null });
		rows.add(new Object[] { "  Simpanan Pokok", Double.valueOf(simpananPokok) });
		rows.add(new Object[] { "  Simpanan Wajib", Double.valueOf(simpananWajib) });
		rows.add(new Object[] { "  TOTAL KEWAJIBAN + MODAL", Double.valueOf(kewajiban + modal) });
		SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Neraca Ringkas",
				"Posisi harta (aset), kewajiban, dan modal koperasi pada saat ini.", "Neraca", "neraca_koperasi",
				new String[] { "Akun", "Nilai (Rp)" }, new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.RUPIAH },
				rows);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Perhitungan Hasil Usaha (PHU)
	// ════════════════════════════════════════════════════════════════════════════════════════

	/** Tampilkan Perhitungan Hasil Usaha ringkas (pendapatan jasa pinjaman). */
	private void buildHasilUsaha(double jasaDiterima) {
		try {
			List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
			kartu.add(new DashboardUiKit.Stat("Pendapatan Jasa Pinjaman", "Rp " + DashboardUiKit.money(jasaDiterima),
					"bunga/jasa yang diterima", DashboardUiKit.GOOD));
			kartu.add(new DashboardUiKit.Stat("Hasil Usaha (sebelum biaya)", "Rp " + DashboardUiKit.money(jasaDiterima),
					"sebelum dikurangi biaya", DashboardUiKit.PRIMARY));
			dashHost.appendChild(DashboardUiKit.html("<div style='font-size:14px;font-weight:800;color:#0f172a;"
					+ "margin:18px 0 4px;border-left:4px solid " + DashboardUiKit.PRIMARY + ";padding-left:8px;'>"
					+ "Perhitungan Hasil Usaha (Ringkas)</div>"));
			dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
					"Pendapatan koperasi dari jasa pinjaman. Biaya operasional dan bunga simpanan dicatat di "
							+ "modul akuntansi, sehingga hasil usaha akhir dihitung di sana.")));
			dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.cards(kartu)));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		List<Object[]> rows = new ArrayList<Object[]>();
		rows.add(new Object[] { "PENDAPATAN", null });
		rows.add(new Object[] { "  Jasa/Bunga Pinjaman", Double.valueOf(jasaDiterima) });
		rows.add(new Object[] { "  TOTAL PENDAPATAN", Double.valueOf(jasaDiterima) });
		rows.add(new Object[] { "BIAYA (dicatat di modul akuntansi)", Double.valueOf(0.0) });
		rows.add(new Object[] { "HASIL USAHA (sebelum biaya)", Double.valueOf(jasaDiterima) });
		SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Perhitungan Hasil Usaha",
				"Pendapatan koperasi dari kegiatan simpan pinjam.", "Hasil Usaha", "hasil_usaha_koperasi",
				new String[] { "Uraian", "Nilai (Rp)" }, new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.RUPIAH },
				rows);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Arus Kas ringkas
	// ════════════════════════════════════════════════════════════════════════════════════════

	/** Tampilkan Arus Kas ringkas (masuk vs keluar) dari kegiatan simpan pinjam. */
	private void buildArusKas(double setoranSimpanan, double angsuranPokokDiterima, double jasaDiterima,
			double penyaluranPinjaman) {
		double masuk = setoranSimpanan + angsuranPokokDiterima + jasaDiterima;
		double keluar = penyaluranPinjaman;
		double net = masuk - keluar;
		try {
			List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
			kartu.add(new DashboardUiKit.Stat("Kas Masuk", "Rp " + DashboardUiKit.money(masuk),
					"setoran + angsuran + jasa", DashboardUiKit.GOOD));
			kartu.add(new DashboardUiKit.Stat("Kas Keluar", "Rp " + DashboardUiKit.money(keluar),
					"penyaluran pinjaman", DashboardUiKit.BAD));
			kartu.add(new DashboardUiKit.Stat("Arus Kas Bersih", "Rp " + DashboardUiKit.money(net),
					net >= 0 ? "surplus kas" : "defisit kas", net >= 0 ? DashboardUiKit.PRIMARY : DashboardUiKit.WARN));
			dashHost.appendChild(DashboardUiKit.html("<div style='font-size:14px;font-weight:800;color:#0f172a;"
					+ "margin:18px 0 4px;border-left:4px solid " + DashboardUiKit.PRIMARY + ";padding-left:8px;'>"
					+ "Arus Kas Ringkas</div>"));
			dashHost.appendChild(DashboardUiKit.html(
					DashboardUiKit.descChip("Ringkasan uang yang masuk dan keluar dari kegiatan simpan pinjam.")));
			dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.cards(kartu)));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		List<Object[]> rows = new ArrayList<Object[]>();
		rows.add(new Object[] { "Setoran Simpanan", Double.valueOf(setoranSimpanan) });
		rows.add(new Object[] { "Angsuran Pokok Diterima", Double.valueOf(angsuranPokokDiterima) });
		rows.add(new Object[] { "Jasa/Bunga Diterima", Double.valueOf(jasaDiterima) });
		rows.add(new Object[] { "TOTAL KAS MASUK", Double.valueOf(masuk) });
		rows.add(new Object[] { "Penyaluran Pinjaman", Double.valueOf(keluar) });
		rows.add(new Object[] { "TOTAL KAS KELUAR", Double.valueOf(keluar) });
		rows.add(new Object[] { "ARUS KAS BERSIH", Double.valueOf(net) });
		SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Arus Kas Ringkas",
				"Uang masuk dan keluar koperasi dari kegiatan simpan pinjam.", "Arus Kas", "arus_kas_koperasi",
				new String[] { "Uraian", "Nilai (Rp)" }, new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.RUPIAH },
				rows);
	}

	/**
	 * Susun <b>Catatan atas Laporan Keuangan (CALK)</b>: penjelasan naratif (gambaran umum &amp;
	 * kebijakan akuntansi) dan rincian pos-pos penting koperasi (simpanan per jenis, piutang pinjaman
	 * berjalan, modal sendiri, dan modal penyertaan) yang dihitung dari data simpan pinjam. CALK
	 * melengkapi Neraca/Laba Rugi/Arus Kas agar pembaca memahami dasar penyusunan dan rincian angka.
	 * Read-only, memakai {@link HibernateUtil#currentSession()}.
	 */
	@SuppressWarnings("unchecked")
	private void buildCalk() {
		if (dashHost == null) {
			return;
		}
		dashHost.getChildren().clear();

		Session session = HibernateUtil.currentSession();
		Long tipeSimpanan = ConstantValues.SIMPANAN != null ? ConstantValues.SIMPANAN.getId() : null;
		Long tipePinjaman = ConstantValues.PINJAMAN != null ? ConstantValues.PINJAMAN.getId() : null;

		double sPokok = 0.0, sWajib = 0.0, sSukarela = 0.0, outstanding = 0.0, penyertaan = 0.0;
		try {
			if (tipeSimpanan != null) {
				List<TransaksiKoperasi> simp = session.createQuery(
						"select t from TransaksiKoperasi t left join fetch t.produkKoperasi p "
								+ "where p.tipeProdukKoperasi.id = :tipe").setParameter("tipe", tipeSimpanan).list();
				for (TransaksiKoperasi t : simp) {
					try {
						String nm = t.getProdukKoperasi() == null || t.getProdukKoperasi().getNama() == null ? ""
								: t.getProdukKoperasi().getNama().toLowerCase();
						double n = t.getNilai() == null ? 0.0 : t.getNilai();
						if (nm.contains("pokok")) {
							sPokok += n;
						} else if (nm.contains("wajib")) {
							sWajib += n;
						} else {
							sSukarela += n;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/LaporanKeuanganKoperasiAction.java:517");
					}
				}
			}
			if (tipePinjaman != null) {
				List<TransaksiKoperasiDetail> belum = session.createQuery(
						"select d from TransaksiKoperasiDetail d where d.pembayaranAnggotaKoperasiDetail is null "
								+ "and d.transaksiKoperasi.produkKoperasi.tipeProdukKoperasi.id = :tipe")
						.setParameter("tipe", tipePinjaman).list();
				for (TransaksiKoperasiDetail d : belum) {
					try {
						outstanding += d.getPokok();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/LaporanKeuanganKoperasiAction.java:529");
					}
				}
			}
			List<ModalPenyertaanKoperasi> mp = session.createQuery(
					"from ModalPenyertaanKoperasi m where m.status = :st and (m.aktif is null or m.aktif = true)")
					.setParameter("st", ModalPenyertaanKoperasi.STATUS_AKTIF).list();
			for (ModalPenyertaanKoperasi m : mp) {
				try {
					penyertaan += m.getNominal();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/LaporanKeuanganKoperasiAction.java:539");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		double modalSendiri = sPokok + sWajib + penyertaan;

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:16px;font-weight:800;color:#0f172a;margin:6px 0 2px;'>")
				.append("Catatan atas Laporan Keuangan (CALK)</div>");
		sb.append(DashboardUiKit.descChip(
				"Penjelasan naratif yang menyertai Neraca, Perhitungan Hasil Usaha, dan Arus Kas."));
		sb.append("<div style='font-size:13px;line-height:1.7;color:#334155;'>");
		sb.append("<p><b>1. Gambaran Umum.</b> Koperasi menyelenggarakan Unit Simpan Pinjam (USP) yang "
				+ "menghimpun simpanan anggota dan menyalurkannya kembali dalam bentuk pinjaman. Laporan "
				+ "keuangan disusun untuk kepentingan Rapat Anggota dan pembinaan.</p>");
		sb.append("<p><b>2. Dasar Penyusunan.</b> Neraca, Laba Rugi, dan Arus Kas disusun dari buku besar "
				+ "akuntansi yang telah diposting, mengikuti pengelompokan akun yang terkonfigurasi. Catatan "
				+ "pos-pos di bawah dirinci dari data kegiatan simpan pinjam.</p>");
		sb.append("<p><b>3. Kebijakan Akuntansi.</b> (a) Pendapatan jasa/bunga pinjaman diakui saat diterima; "
				+ "(b) Simpanan pokok dan wajib diperlakukan sebagai modal sendiri, simpanan sukarela sebagai "
				+ "kewajiban kepada anggota; (c) Piutang pinjaman disajikan sebesar sisa pokok yang belum "
				+ "dikembalikan dan diklasifikasikan menurut kolektibilitas, dengan penyisihan (PPAP) sesuai "
				+ "tingkat kolektibilitas; (d) Modal penyertaan diakui sebagai penguat modal sendiri.</p>");
		sb.append("</div>");
		dashHost.appendChild(DashboardUiKit.html(sb.toString()));

		List<Object[]> rows = new ArrayList<Object[]>();
		rows.add(new Object[] { "Simpanan Pokok", Double.valueOf(sPokok) });
		rows.add(new Object[] { "Simpanan Wajib", Double.valueOf(sWajib) });
		rows.add(new Object[] { "Simpanan Sukarela (Kewajiban)", Double.valueOf(sSukarela) });
		rows.add(new Object[] { "Modal Penyertaan (Aktif)", Double.valueOf(penyertaan) });
		rows.add(new Object[] { "Modal Sendiri (Pokok+Wajib+Penyertaan)", Double.valueOf(modalSendiri) });
		rows.add(new Object[] { "Piutang Pinjaman Berjalan (Sisa Pokok)", Double.valueOf(outstanding) });
		SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Catatan Pos-Pos Penting",
				"Rincian angka utama sebagai bagian dari Catatan atas Laporan Keuangan.", "CALK", "calk_koperasi",
				new String[] { "Pos", "Nilai (Rp)" }, new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.RUPIAH },
				rows);
	}
}
