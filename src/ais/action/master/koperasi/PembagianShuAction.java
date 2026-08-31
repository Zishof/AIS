package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Intbox;

import ais.action.master.koperasi.helper.SimpanPinjamUiUtil;
import ais.action.master.koperasi.util.PembagianShuHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.PembagianShu;
import ais.database.model.koperasi.ShuAnggota;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyMessageboxConfig;

/**
 * <h2>PembagianShuAction — Pembagian Sisa Hasil Usaha (SHU) Koperasi</h2>
 *
 * <p>
 * Composer ZK ini membantu pengurus <b>membagi Sisa Hasil Usaha (SHU) koperasi kepada seluruh
 * anggota secara adil</b> untuk satu tahun buku. Pengurus cukup memasukkan total SHU hasil Rapat
 * Anggota Tahunan (RAT) beserta persentase alokasi tiap pos (dana cadangan, jasa modal, jasa usaha,
 * pendidikan, dan seterusnya); sistem lalu menghitung bagian tiap anggota secara otomatis dan
 * proporsional, menampilkannya sebagai grafik dan tabel, serta menyediakan tombol Unduh Excel.
 * </p>
 *
 * <h3>Cara kerja perhitungan (adil &amp; transparan)</h3>
 * <p>
 * Total SHU <b>tidak dikira-kira oleh sistem</b> melainkan diisi pengurus sesuai keputusan RAT.
 * Dari total tersebut, dua pos dibagikan langsung ke anggota:
 * </p>
 * <ul>
 * <li><b>Jasa Modal</b> — dibagi <i>sebanding besarnya simpanan</i> tiap anggota. Anggota dengan
 * simpanan lebih besar menerima jasa modal lebih besar.</li>
 * <li><b>Jasa Usaha</b> — dibagi <i>sebanding partisipasi</i> tiap anggota, yang di sini diukur dari
 * jasa/bunga pinjaman yang menjadi kontribusi anggota terhadap pendapatan koperasi.</li>
 * </ul>
 * <p>
 * Pos lain (cadangan, pendidikan, insentif pengurus, sosial) tidak dibagikan per anggota melainkan
 * dialokasikan ke koperasi; nilainya tetap ditampilkan pada grafik komposisi agar pembagian
 * transparan. Basis perhitungan tiap anggota (total simpanan dan total partisipasi) disimpan pada
 * {@link ShuAnggota} sehingga hasilnya dapat ditelusuri/diaudit ulang kapan pun.
 * </p>
 *
 * <h3>Alur data</h3>
 * <p>
 * Data bersumber dari engine simpan pinjam existing: simpanan &amp; pinjaman anggota adalah
 * {@link TransaksiKoperasi} yang dibedakan lewat {@code produkKoperasi.tipeProdukKoperasi}
 * ({@link ConstantValues#SIMPANAN} vs {@link ConstantValues#PINJAMAN}). Hasil pembagian disimpan
 * pada {@link PembagianShu} (kepala per tahun) dan {@link ShuAnggota} (rincian per anggota).
 * </p>
 *
 * <h3>Ketahanan &amp; efisiensi</h3>
 * <p>
 * Seluruh operasi baca/tulis memakai {@link HibernateUtil#currentSession()} sehingga ditutup
 * otomatis (tidak ditutup manual). Perhitungan proporsional dilakukan sekali jalan di memori dari
 * daftar yang sudah diambil (asosiasi anggota di-<i>fetch</i> untuk menekan query N+1). Penyimpanan
 * ulang menghapus baris {@link ShuAnggota} lama untuk tahun yang sama agar tidak menumpuk. Grafik
 * dan tabel memakai {@link DashboardUiKit} dan {@link SimpanPinjamUiUtil} (HTML/CSS, tanpa
 * JFreeChart) supaya konsisten dengan dasbor lain dan mudah dipelihara. Kompatibel Java 1.7.
 * </p>
 *
 * @see PembagianShu
 * @see ShuAnggota
 * @see SimpanPinjamUiUtil
 */
public class PembagianShuAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	// Auto-wired dari ZUL.
	private Intbox tahun;
	private Doublebox totalShu;
	private Doublebox persenCadangan;
	private Doublebox persenJasaModal;
	private Doublebox persenJasaUsaha;
	private Doublebox persenPendidikan;
	private Doublebox persenPengurus;
	private Doublebox persenSosial;
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
		DashboardUiKit.attachIntro(comp, "Pembagian SHU",
				"Bagikan Sisa Hasil Usaha koperasi ke seluruh anggota secara adil: isi total SHU dan "
						+ "persentasenya, sistem menghitung bagian tiap anggota otomatis.");
		int thnSekarang = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		if (tahun != null && (tahun.getValue() == null || tahun.getValue().intValue() == 0)) {
			tahun.setValue(Integer.valueOf(thnSekarang));
		}
		prefillDariTersimpan();
		buildHasil(HibernateUtil.currentSession());
	}

	/** Aksi tombol Segarkan. */
	public void onRefresh(Event event) throws Exception {
		buildHasil(HibernateUtil.currentSession());
	}

	/** Isi ulang form dari data pembagian SHU tahun terpilih (bila sudah pernah disimpan). */
	private void prefillDariTersimpan() {
		try {
			PembagianShu p = cariPembagian(HibernateUtil.currentSession(), tahunTerpilih());
			if (p != null) {
				if (totalShu != null) {
					totalShu.setValue(p.getTotalShu());
				}
				setNilai(persenCadangan, p.getPersenCadangan());
				setNilai(persenJasaModal, p.getPersenJasaModal());
				setNilai(persenJasaUsaha, p.getPersenJasaUsaha());
				setNilai(persenPendidikan, p.getPersenPendidikan());
				setNilai(persenPengurus, p.getPersenPengurus());
				setNilai(persenSosial, p.getPersenSosial());
			} else {
				// Default alokasi yang lazim dipakai koperasi (bisa diubah pengurus).
				setNilai(persenCadangan, 25.0);
				setNilai(persenJasaModal, 25.0);
				setNilai(persenJasaUsaha, 30.0);
				setNilai(persenPendidikan, 10.0);
				setNilai(persenPengurus, 5.0);
				setNilai(persenSosial, 5.0);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Aksi tombol "Hitung &amp; Bagikan": simpan parameter SHU tahun ini lalu hitung dan simpan
	 * bagian tiap anggota, kemudian tampilkan hasilnya.
	 */
	public void onHitung(Event event) throws Exception {
		if (totalShu == null || totalShu.getValue() == null || totalShu.getValue().doubleValue() <= 0) {
			MyMessageboxConfig.show("Mohon maaf, total SHU harus lebih dari nol. Langkah yang dapat dilakukan: (1) isi kolom Total SHU dengan angka lebih dari 0; (2) pastikan data simpanan anggota telah tercatat; (3) ulangi proses pembagian SHU.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		try {
			hitungDanSimpan(HibernateUtil.currentSession());
			buildHasil(HibernateUtil.currentSession());
			MyMessageboxConfig.show("Pembagian SHU berhasil dihitung dan disimpan.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			MyMessageboxConfig.show("Mohon maaf, gagal menghitung SHU. Langkah yang dapat dilakukan: (1) periksa kembali data simpanan dan pinjaman anggota; (2) pastikan tidak ada transaksi yang belum diselesaikan; (3) coba lagi atau hubungi Administrator jika masalah berlanjut.", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		}
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Perhitungan & penyimpanan
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Hitung bagian SHU tiap anggota dan simpan. Membuat/memperbarui {@link PembagianShu} untuk tahun
	 * terpilih, menghapus {@link ShuAnggota} lama-nya, lalu menyimpan hasil pembagian yang baru.
	 */
	/**
	 * Hitung dan simpan pembagian SHU tahun terpilih.
	 *
	 * <p>Perhitungannya milik {@link PembagianShuHelper} agar layar native
	 * memakai rumus yang persis sama; di sini hanya nilai formulir yang
	 * dikumpulkan.</p>
	 */
	private void hitungDanSimpan(Session session) {
		PembagianShuHelper.Parameter p = new PembagianShuHelper.Parameter();
		p.totalShu = nilai(totalShu);
		p.persenCadangan = nilai(persenCadangan);
		p.persenJasaModal = nilai(persenJasaModal);
		p.persenJasaUsaha = nilai(persenJasaUsaha);
		p.persenPendidikan = nilai(persenPendidikan);
		p.persenPengurus = nilai(persenPengurus);
		p.persenSosial = nilai(persenSosial);
		PembagianShuHelper.hitungDanSimpan(session, tahunTerpilih(), p);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Tampilan hasil
	// ════════════════════════════════════════════════════════════════════════════════════════

	/** Bangun tampilan hasil pembagian SHU tahun terpilih: kartu, komposisi, peringkat, dan tabel. */
	@SuppressWarnings("unchecked")
	private void buildHasil(Session session) {
		if (dashHost == null) {
			return;
		}
		dashHost.getChildren().clear();

		PembagianShu pembagian = cariPembagian(session, tahunTerpilih());
		if (pembagian == null) {
			dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
					"Belum ada pembagian SHU untuk tahun ini. Isi total SHU dan persentasenya, lalu tekan "
							+ "\"Hitung & Bagikan\".")));
			return;
		}

		List<ShuAnggota> rincian = new ArrayList<ShuAnggota>();
		try {
			rincian = session.createQuery(
					"select distinct s from ShuAnggota s left join fetch s.anggota a where s.pembagianShu.id = :id "
							+ "order by s.totalShu desc").setParameter("id", pembagian.getId()).list();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		double totalDibagikan = 0.0;
		for (ShuAnggota s : rincian) {
			totalDibagikan += s.getTotalShu();
		}

		try {
			StringBuilder sb = new StringBuilder();
			List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
			kartu.add(new DashboardUiKit.Stat("Total SHU " + pembagian.getTahun(),
					"Rp " + DashboardUiKit.money(pembagian.getTotalShu()), "hasil RAT", DashboardUiKit.PRIMARY));
			kartu.add(new DashboardUiKit.Stat("Jasa Modal (Simpanan)",
					"Rp " + DashboardUiKit.money(pembagian.getNominalJasaModal()), "dibagi sebanding simpanan",
					DashboardUiKit.GOOD));
			kartu.add(new DashboardUiKit.Stat("Jasa Usaha (Partisipasi)",
					"Rp " + DashboardUiKit.money(pembagian.getNominalJasaUsaha()), "dibagi sebanding partisipasi",
					DashboardUiKit.ACCENT));
			kartu.add(new DashboardUiKit.Stat("Diterima Anggota", "Rp " + DashboardUiKit.money(totalDibagikan),
					rincian.size() + " anggota", DashboardUiKit.WARN));
			sb.append(DashboardUiKit.descChip("Ringkasan pembagian SHU tahun " + pembagian.getTahun() + "."));
			sb.append(DashboardUiKit.cards(kartu));

			// Komposisi alokasi SHU (semua pos) — donut.
			LinkedHashMap<String, Double> alokasi = new LinkedHashMap<String, Double>();
			double t = pembagian.getTotalShu();
			alokasi.put("Dana Cadangan", Double.valueOf(t * pembagian.getPersenCadangan() / 100.0));
			alokasi.put("Jasa Modal", Double.valueOf(pembagian.getNominalJasaModal()));
			alokasi.put("Jasa Usaha", Double.valueOf(pembagian.getNominalJasaUsaha()));
			alokasi.put("Pendidikan", Double.valueOf(t * pembagian.getPersenPendidikan() / 100.0));
			alokasi.put("Insentif Pengurus", Double.valueOf(t * pembagian.getPersenPengurus() / 100.0));
			alokasi.put("Dana Sosial", Double.valueOf(t * pembagian.getPersenSosial() / 100.0));

			LinkedHashMap<String, Double> topPenerima = new LinkedHashMap<String, Double>();
			int n = 0;
			for (ShuAnggota s : rincian) {
				if (n++ >= 10) {
					break;
				}
				String nm = s.getAnggota() == null || s.getAnggota().getNama() == null ? "-" : s.getAnggota().getNama();
				topPenerima.put(DashboardUiKit.shorten(nm, 22), Double.valueOf(s.getTotalShu()));
			}

			sb.append(DashboardUiKit.openGrid(320));
			sb.append(DashboardUiKit.donut("Kemana SHU Dibagikan",
					"Pembagian total SHU ke tiap pos sesuai keputusan RAT.", alokasi, true,
					"Belum ada nilai SHU."));
			sb.append(DashboardUiKit.barList("10 Anggota Penerima SHU Terbesar",
					"Anggota yang menerima SHU paling besar tahun ini.", topPenerima, DashboardUiKit.GOOD, "", true,
					"Belum ada penerima SHU."));
			sb.append(DashboardUiKit.closeGrid());
			dashHost.appendChild(DashboardUiKit.html(sb.toString()));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		// Tabel rincian SHU per anggota + Unduh Excel.
		try {
			List<Object[]> rows = new ArrayList<Object[]>();
			for (ShuAnggota s : rincian) {
				String nm = s.getAnggota() == null || s.getAnggota().getNama() == null ? "-" : s.getAnggota().getNama();
				rows.add(new Object[] { nm, Double.valueOf(s.getTotalSimpanan()), Double.valueOf(s.getTotalTransaksi()),
						Double.valueOf(s.getJasaModal()), Double.valueOf(s.getJasaUsaha()),
						Double.valueOf(s.getTotalShu()), s.getSudahDibayar().booleanValue() ? "Sudah" : "Belum" });
			}
			SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Rincian SHU per Anggota",
					"Bagian SHU tiap anggota beserta dasar perhitungannya (simpanan & partisipasi).",
					"SHU " + pembagian.getTahun(), "rincian_shu",
					new String[] { "Anggota", "Total Simpanan", "Partisipasi (Jasa)", "Jasa Modal", "Jasa Usaha",
							"Total SHU", "Dibayar" },
					new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.RUPIAH,
							SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.RUPIAH,
							SimpanPinjamUiUtil.TEKS },
					rows);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Bantuan
	// ════════════════════════════════════════════════════════════════════════════════════════

	/** Cari pembagian SHU untuk satu tahun (baris terbaru bila kebetulan ada lebih dari satu). */
	private PembagianShu cariPembagian(Session session, int thn) {
		return PembagianShuHelper.cari(session, thn);
	}

	private int tahunTerpilih() {
		if (tahun != null && tahun.getValue() != null && tahun.getValue().intValue() > 0) {
			return tahun.getValue().intValue();
		}
		return ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
	}

	private static void setNilai(Doublebox box, double v) {
		if (box != null) {
			box.setValue(Double.valueOf(v));
		}
	}

	private static double nilai(Doublebox box) {
		return box == null || box.getValue() == null ? 0.0 : box.getValue().doubleValue();
	}

}
