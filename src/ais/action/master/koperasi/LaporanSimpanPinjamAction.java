package ais.action.master.koperasi;

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

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Div;

import ais.action.master.koperasi.helper.BungaSimpananUtil;
import ais.action.master.koperasi.helper.SimpanPinjamReportService;
import ais.action.master.koperasi.helper.SimpanPinjamUiUtil;
import ais.action.master.koperasi.helper.SuratTeguranHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.PembayaranAnggotaKoperasi;
import ais.database.model.koperasi.ProdukKoperasi;
import ais.database.model.koperasi.ShuAnggota;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyWindow;

/**
 * <h2>LaporanSimpanPinjamAction — Buku Simpan Pinjam &amp; Buku Kas (Sesuai Template Pembukuan)</h2>
 *
 * <p>
 * Composer ZK ini mencetak dua buku pembukuan koperasi klasik di atas data engine simpan pinjam yang
 * sudah ada, persis mengikuti bentuk template pembukuan koperasi:
 * </p>
 * <ol>
 * <li><b>Buku Simpan Pinjam</b> — daftar rinci setiap pinjaman beserta jadwal angsurannya: tanggal
 * pinjam, besar pinjaman, angsuran pokok, angsuran bunga, jumlah, dan sisa pinjaman. Sumber datanya
 * adalah {@link TransaksiKoperasi} (akad, bertipe {@link ConstantValues#PINJAMAN}) dan baris jadwal
 * {@link TransaksiKoperasiDetail} (kolom {@code pokok} = angsuran pokok, {@code margin} = angsuran
 * bunga, {@code sisa} = sisa pinjaman). Sebuah baris dianggap "Lunas" bila angsurannya sudah tertaut
 * ke pembayaran anggota.</li>
 * <li><b>Buku Kas Simpan Pinjam</b> — arus kas masuk dan keluar kegiatan simpan pinjam beserta saldo
 * berjalan. Kas <b>masuk</b> berasal dari setoran/angsuran anggota ({@link PembayaranAnggotaKoperasi}),
 * kas <b>keluar</b> berasal dari pencairan pinjaman ({@link TransaksiKoperasi} bertipe pinjaman yang
 * sudah disetujui). Baris diurutkan berdasarkan tanggal dan saldo dihitung berjalan dari nol.</li>
 * </ol>
 *
 * <p>
 * Sesuai arahan, kedua buku ditampilkan sebagai <b>grid ZK</b> lebih dulu (bukan langsung Excel),
 * lengkap dengan tombol <i>Unduh Excel</i> per tabel yang baru menghasilkan berkas Excel asli saat
 * ditekan — seluruhnya lewat utilitas bersama {@link SimpanPinjamUiUtil} agar konsisten dengan
 * dasbor dan mudah dipelihara. Setiap buku diberi penjelasan singkat yang mudah dipahami awam.
 * </p>
 *
 * <h3>Ketahanan &amp; efisiensi</h3>
 * <p>
 * Session diambil via {@link HibernateUtil#currentSession()} (ditutup otomatis oleh kerangka kerja).
 * Setiap buku dibungkus {@code try/catch} mandiri sehingga kegagalan satu buku tidak menumbangkan
 * halaman. Asosiasi anggota/produk di-<i>fetch</i> sekaligus untuk menekan query N+1. Kompatibel
 * Java 1.7.
 * </p>
 *
 * @see SimpanPinjamUiUtil
 * @see TransaksiKoperasiDetail
 */
public class LaporanSimpanPinjamAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	private Div dashHost;
	/** Popup untuk menampilkan surat teguran siap cetak (auto-wired dari ZUL). */
	private MyWindow suratWindow;

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
		DashboardUiKit.attachIntro(comp, "Laporan / Buku Koperasi",
				"Kumpulan buku catatan koperasi sesuai format pembukuan: Buku Simpan Pinjam, Jurnal Kas "
						+ "Masuk & Keluar, Buku Kas, dan Buku Anggota. Semua bisa diunduh ke Excel.");
		buildLaporan();
	}

	/** Aksi tombol Segarkan pada ZUL. */
	public void onRefresh(Event event) throws Exception {
		buildLaporan();
	}

	/**
	 * Bangun surat teguran (siap cetak) untuk seluruh anggota yang memiliki angsuran menunggak, lalu
	 * tampilkan pada popup. Tiap anggota mendapat satu surat (dipisah per halaman) yang disusun
	 * {@link SuratTeguranHelper}. Pengguna cukup menekan Ctrl+P di peramban untuk mencetak. Memakai
	 * {@link HibernateUtil#currentSession()} (ditutup otomatis) dan agregasi per anggota di memori.
	 */
	public void onSuratTeguran(Event event) throws Exception {
		if (suratWindow == null) {
			return;
		}
		SimpanPinjamReportService.Surat hasil = null;
		try {
			hasil = SimpanPinjamReportService.suratTeguran(
					HibernateUtil.currentSession(), ais.ui.util.WaktuUtil.getDate());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		int jumlahSurat = hasil == null ? 0 : hasil.jumlah;

		Common.clear(suratWindow);
		suratWindow.setTitle("Surat Teguran (" + jumlahSurat + " anggota)");
		Div host = new Div();
		host.setStyle("overflow:auto;height:100%;width:100%;background:#f8fafc;");
		host.setParent(suratWindow);
		host.appendChild(DashboardUiKit.html("<div style='padding:8px 12px;font-size:12px;color:#475569;"
				+ "background:#fff;border-bottom:1px solid #e2e8f0;'>Tekan <b>Ctrl+P</b> pada peramban untuk "
				+ "mencetak. Total <b>" + jumlahSurat + "</b> surat.</div>"));
		if (jumlahSurat == 0) {
			host.appendChild(DashboardUiKit.html(DashboardUiKit
					.descChip("Tidak ada anggota yang menunggak — tidak ada surat teguran yang perlu dibuat.")));
		} else {
			host.appendChild(DashboardUiKit.html(hasil == null ? "" : hasil.html));
		}
		suratWindow.setVisible(true);
		suratWindow.onModal();
	}

	/**
	 * Hitung dan tampilkan <b>bunga (jasa) simpanan bulan berjalan</b> per anggota per produk simpanan.
	 * Untuk tiap kombinasi anggota-produk simpanan sukarela/berjangka, saldo harian sepanjang bulan
	 * disusun ulang dari saldo awal bulan ditambah setoran, lalu bunga dihitung memakai metode yang
	 * ditetapkan pada produk ({@link ProdukKoperasi#getMetodeBungaSimpanan()}) — saldo terendah,
	 * harian, atau rata-rata — dengan suku bunga {@link ProdukKoperasi#getBungaSimpananPersen()}.
	 * Simpanan pokok dan wajib dikecualikan karena termasuk modal (memperoleh SHU, bukan bunga).
	 *
	 * <p>
	 * Hasilnya disajikan sebagai tabel yang bisa diunduh ke Excel, lengkap dengan saldo terendah dan
	 * saldo rata-rata sebagai bahan telusur. Memakai {@link HibernateUtil#currentSession()} (ditutup
	 * otomatis) dan seluruh agregasi dilakukan di memori secara aman-null.
	 * </p>
	 */
	public void onBungaSimpanan(Event event) throws Exception {
		if (dashHost == null) {
			return;
		}
		dashHost.getChildren().clear();
		try {
			renderBagian(SimpanPinjamReportService.bangun(HibernateUtil.currentSession(),
					SimpanPinjamReportService.BUNGA_SIMPANAN, ais.ui.util.WaktuUtil.getDate()));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Rangkai ulang kedua buku ke dalam kontainer {@code dashHost}. */
	private void buildLaporan() {
		if (dashHost == null) {
			return;
		}
		dashHost.getChildren().clear();

		Session session = HibernateUtil.currentSession();
		Date sekarang = ais.ui.util.WaktuUtil.getDate();
		for (SimpanPinjamReportService.Bagian metadata : SimpanPinjamReportService.katalog()) {
			if (SimpanPinjamReportService.BUNGA_SIMPANAN.equals(metadata.kunci)) continue;
			try {
				renderBagian(SimpanPinjamReportService.bangun(session, metadata.kunci, sekarang));
			} catch (Exception e) {
				// Sama dengan ketahanan layar lama: kegagalan satu buku tidak
				// menghilangkan tujuh buku lain yang masih dapat disusun.
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	/** Penyaji ZK untuk struktur yang sama dengan kontrak native. */
	private void renderBagian(SimpanPinjamReportService.Bagian bagian) {
		if (!bagian.grafik.isEmpty()) {
			dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.barList(bagian.judul + " (per Kategori)",
					bagian.deskripsi, bagian.grafik,
					SimpanPinjamReportService.JURNAL_KAS_KELUAR.equals(bagian.kunci)
							? DashboardUiKit.BAD : DashboardUiKit.GOOD,
					"", true, "Belum ada data.")));
		}
		if (SimpanPinjamReportService.PROMOSI_EKONOMI.equals(bagian.kunci)) {
			List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
			kartu.add(new DashboardUiKit.Stat("Anggota Terlayani",
					String.valueOf(bagian.ringkasan.get("anggotaTerlayani")), "menerima manfaat", DashboardUiKit.PRIMARY));
			kartu.add(new DashboardUiKit.Stat("Total Simpanan Anggota", "Rp "
					+ DashboardUiKit.money(angkaRingkasan(bagian, "totalSimpanan")), "dana yang dititipkan", DashboardUiKit.GOOD));
			kartu.add(new DashboardUiKit.Stat("Total Pinjaman Diterima", "Rp "
					+ DashboardUiKit.money(angkaRingkasan(bagian, "totalPinjaman")), "modal yang dipinjamkan", DashboardUiKit.ACCENT));
			kartu.add(new DashboardUiKit.Stat("Total SHU Dikembalikan", "Rp "
					+ DashboardUiKit.money(angkaRingkasan(bagian, "totalShu")), "keuntungan untuk anggota", DashboardUiKit.WARN));
			dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.cards(kartu)));
		} else if (SimpanPinjamReportService.BUNGA_SIMPANAN.equals(bagian.kunci)) {
			dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
					"Total bunga bulan ini: Rp " + DashboardUiKit.money(angkaRingkasan(bagian, "totalBunga")) + ".")));
			if (Boolean.TRUE.equals(bagian.ringkasan.get("produkTanpaBunga"))) {
				dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
						"Sebagian produk simpanan belum diisi suku bunganya sehingga bunganya Rp 0.")));
			}
		}
		if (bagian.catatan != null && bagian.catatan.length() > 0) {
			dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(bagian.catatan)));
		}
		SimpanPinjamUiUtil.appendRekapGrid(dashHost, bagian.judul, bagian.deskripsi,
				bagian.sheet, bagian.namaBerkas, bagian.header, bagian.jenisKolom, bagian.baris);
	}

	private static double angkaRingkasan(SimpanPinjamReportService.Bagian bagian, String kunci) {
		Object nilai = bagian.ringkasan.get(kunci);
		return nilai instanceof Number ? ((Number) nilai).doubleValue() : 0.0;
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Simpanan Berjangka (Deposito) & Jatuh Tempo
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Bangun daftar "Simpanan Berjangka (Deposito)": simpanan bertenor (produk yang namanya memuat
	 * "berjangka"/"deposito") beserta perkiraan tanggal jatuh temponya (tanggal setor + jangka waktu
	 * produk). Membantu pengurus memantau deposito yang akan/sudah jatuh tempo untuk ditindaklanjuti
	 * (dicairkan atau diperpanjang).
	 */
	@SuppressWarnings("unchecked")
	private void buildSimpananBerjangka(Session session, Long tipeSimpanan) {
		final List<Object[]> rows = new ArrayList<Object[]>();
		try {
			if (tipeSimpanan != null) {
				Date now = ais.ui.util.WaktuUtil.getDate();
				List<TransaksiKoperasi> simp = session.createQuery(
						"select distinct t from TransaksiKoperasi t left join fetch t.anggotaKoperasi a "
								+ "left join fetch t.produkKoperasi p where p.tipeProdukKoperasi.id = :tipe")
						.setParameter("tipe", tipeSimpanan).list();
				for (TransaksiKoperasi t : simp) {
					try {
						if (t.getProdukKoperasi() == null) {
							continue;
						}
						String nm = t.getProdukKoperasi().getNama() == null ? ""
								: t.getProdukKoperasi().getNama().toLowerCase();
						if (!(nm.contains("berjangka") || nm.contains("deposito"))) {
							continue;
						}
						double jw = t.getProdukKoperasi().getJangkaWaktuBulan() == null ? 0
								: t.getProdukKoperasi().getJangkaWaktuBulan().doubleValue();
						Date setor = t.getTanggalTransaksi();
						Date jatuhTempo = null;
						if (setor != null && jw > 0) {
							java.util.Calendar c = ais.ui.util.WaktuUtil.getCalendar();
							c.setTime(setor);
							c.add(java.util.Calendar.MONTH, (int) Math.round(jw));
							jatuhTempo = c.getTime();
						}
						String status = jatuhTempo != null && jatuhTempo.before(now) ? "Jatuh Tempo" : "Berjalan";
						String anggota = t.getAnggotaKoperasi() == null ? "-" : t.getAnggotaKoperasi().getNama();
						rows.add(new Object[] { anggota, t.getProdukKoperasi().getNama(), Double.valueOf(t.getNilai()),
								setor, jatuhTempo, status });
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/LaporanSimpanPinjamAction.java:394");
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Simpanan Berjangka (Deposito) & Jatuh Tempo",
				"Daftar simpanan berjangka anggota beserta perkiraan tanggal jatuh temponya.", "Simpanan Berjangka",
				"simpanan_berjangka",
				new String[] { "Anggota", "Produk", "Nominal", "Tgl Setor", "Perkiraan Jatuh Tempo", "Status" },
				new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.RUPIAH,
						SimpanPinjamUiUtil.TANGGAL, SimpanPinjamUiUtil.TANGGAL, SimpanPinjamUiUtil.TEKS },
				rows);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Daftar Tunggakan (bahan pembinaan / surat teguran)
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Bangun "Daftar Tunggakan": angsuran yang sudah melewati tanggal jatuh tempo namun belum dibayar.
	 * Daftar ini menjadi bahan pembinaan anggota dan penerbitan surat teguran sebagaimana diatur SOM
	 * USPK — pengurus dapat menghubungi anggota yang menunggak, diurutkan dari yang paling lama
	 * terlambat.
	 */
	@SuppressWarnings("unchecked")
	private void buildDaftarTunggakan(Session session, Long tipePinjaman) {
		final List<Object[]> rows = new ArrayList<Object[]>();
		try {
			if (tipePinjaman != null) {
				Date now = ais.ui.util.WaktuUtil.getDate();
				List<TransaksiKoperasiDetail> nunggak = session.createQuery(
						"select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
								+ "left join fetch t.anggotaKoperasi a where d.pembayaranAnggotaKoperasiDetail is null "
								+ "and d.tanggal < :now and t.produkKoperasi.tipeProdukKoperasi.id = :tipe order by d.tanggal")
						.setParameter("now", now).setParameter("tipe", tipePinjaman).list();
				for (TransaksiKoperasiDetail d : nunggak) {
					try {
						TransaksiKoperasi t = d.getTransaksiKoperasi();
						String anggota = t == null || t.getAnggotaKoperasi() == null ? "-"
								: t.getAnggotaKoperasi().getNama();
						long hari = d.getTanggal() == null ? 0
								: (now.getTime() - d.getTanggal().getTime()) / (1000L * 60 * 60 * 24);
						rows.add(new Object[] { anggota, Integer.valueOf(d.getKe() == null ? 0 : d.getKe()),
								d.getTanggal(), Double.valueOf(d.getPokok() + d.getMargin()),
								Long.valueOf(hari < 0 ? 0 : hari),
								t == null ? "Lancar" : t.getKolektibilitasLabel() });
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		if (!rows.isEmpty()) {
			dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
					"Anggota berikut memiliki angsuran yang lewat jatuh tempo — bahan untuk pembinaan/surat teguran.")));
		}
		SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Daftar Tunggakan (Perlu Pembinaan)",
				"Angsuran yang sudah lewat tanggal jatuh tempo namun belum dibayar, diurutkan dari yang tertua.",
				"Tunggakan", "daftar_tunggakan",
				new String[] { "Anggota", "Angsuran Ke", "Jatuh Tempo", "Jumlah Tertunggak", "Hari Terlambat",
						"Kolektibilitas" },
				new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.ANGKA, SimpanPinjamUiUtil.TANGGAL,
						SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.ANGKA, SimpanPinjamUiUtil.TEKS },
				rows);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Buku Simpan Pinjam
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Bangun "Buku Simpan Pinjam": satu baris per angsuran, memuat identitas pinjaman dan komponen
	 * pokok/bunga/sisa — persis kolom pada template pembukuan.
	 */
	@SuppressWarnings("unchecked")
	private void buildBukuSimpanPinjam(Session session, Long tipePinjaman) {
		List<Object[]> rows = new ArrayList<Object[]>();
		try {
			if (tipePinjaman != null) {
				List<TransaksiKoperasiDetail> angsuran = session.createQuery(
						"select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
								+ "left join fetch t.anggotaKoperasi a left join fetch t.produkKoperasi p "
								+ "where p.tipeProdukKoperasi.id = :tipe order by t.id, d.ke")
						.setParameter("tipe", tipePinjaman).list();
				for (TransaksiKoperasiDetail d : angsuran) {
					try {
						TransaksiKoperasi t = d.getTransaksiKoperasi();
						if (t == null) {
							continue;
						}
						String anggota = t.getAnggotaKoperasi() == null ? "-" : t.getAnggotaKoperasi().getNama();
						String produk = t.getProdukKoperasi() == null ? "-" : t.getProdukKoperasi().getNama();
						String status = d.getPembayaranAnggotaKoperasiDetail() != null ? "Lunas" : "Belum";
						rows.add(new Object[] { anggota, produk, t.getTanggalTransaksi(), Double.valueOf(t.getNilai()),
								Integer.valueOf(d.getKe() == null ? 0 : d.getKe()), d.getTanggal(),
								Double.valueOf(d.getPokok()), Double.valueOf(d.getMargin()),
								Double.valueOf(d.getPokok() + d.getMargin()), Double.valueOf(d.getSisa() == null ? 0.0 : d.getSisa()),
								status });
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Buku Simpan Pinjam",
				"Rincian tiap pinjaman anggota beserta jadwal cicilannya: pokok, bunga, jumlah, dan sisa.",
				"Buku Simpan Pinjam", "buku_simpan_pinjam",
				new String[] { "Anggota", "Produk", "Tgl Pinjam", "Besar Pinjaman", "Angsuran Ke", "Tgl Angsuran",
						"Angsuran Pokok", "Angsuran Bunga", "Jumlah", "Sisa Pinjaman", "Status" },
				new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.TANGGAL,
						SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.ANGKA, SimpanPinjamUiUtil.TANGGAL,
						SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.RUPIAH,
						SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.TEKS },
				rows);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Buku Kas Simpan Pinjam
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Bangun "Buku Kas Simpan Pinjam": gabungkan kas masuk (setoran/angsuran anggota) dan kas keluar
	 * (pencairan pinjaman), urutkan berdasarkan tanggal, lalu hitung saldo berjalan.
	 */
	@SuppressWarnings("unchecked")
	private void buildBukuKas(Session session, Long tipePinjaman) {
		// Kumpulan kejadian kas: {tanggal, uraian, masuk, keluar}
		final List<Object[]> events = new ArrayList<Object[]>();

		// Kas masuk — pembayaran anggota (setoran simpanan & angsuran pinjaman).
		try {
			List<PembayaranAnggotaKoperasi> bayar = session.createQuery(
					"select distinct p from PembayaranAnggotaKoperasi p left join fetch p.anggotaKoperasi a").list();
			for (PembayaranAnggotaKoperasi p : bayar) {
				try {
					String anggota = p.getAnggotaKoperasi() == null ? "-" : p.getAnggotaKoperasi().getNama();
					events.add(new Object[] { p.getTanggal(), "Setoran/Angsuran - " + anggota,
							Double.valueOf(p.getNominal()), Double.valueOf(0.0) });
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		// Kas keluar — pencairan pinjaman yang sudah disetujui.
		try {
			if (tipePinjaman != null) {
				List<TransaksiKoperasi> pinjaman = session.createQuery(
						"select distinct t from TransaksiKoperasi t left join fetch t.produkKoperasi p "
								+ "left join fetch t.anggotaKoperasi a where p.tipeProdukKoperasi.id = :tipe")
						.setParameter("tipe", tipePinjaman).list();
				for (TransaksiKoperasi t : pinjaman) {
					try {
						if (!t.getAktif() || !TransaksiKoperasi.DISETUJU.equals(t.getStatus())) {
							continue;
						}
						String anggota = t.getAnggotaKoperasi() == null ? "-" : t.getAnggotaKoperasi().getNama();
						events.add(new Object[] { t.getTanggalTransaksi(), "Pencairan Pinjaman - " + anggota,
								Double.valueOf(0.0), Double.valueOf(t.getNilai()) });
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		// Urutkan berdasarkan tanggal (null diletakkan paling awal secara aman).
		Collections.sort(events, new Comparator<Object[]>() {
			@Override
			public int compare(Object[] a, Object[] b) {
				Date da = a[0] instanceof Date ? (Date) a[0] : null;
				Date db = b[0] instanceof Date ? (Date) b[0] : null;
				if (da == null && db == null) {
					return 0;
				}
				if (da == null) {
					return -1;
				}
				if (db == null) {
					return 1;
				}
				return da.compareTo(db);
			}
		});

		// Hitung saldo berjalan.
		List<Object[]> rows = new ArrayList<Object[]>();
		double saldo = 0.0;
		int no = 1;
		for (Object[] ev : events) {
			double masuk = ((Number) ev[2]).doubleValue();
			double keluar = ((Number) ev[3]).doubleValue();
			saldo += masuk - keluar;
			rows.add(new Object[] { Integer.valueOf(no++), ev[0], ev[1], Double.valueOf(masuk), Double.valueOf(keluar),
					Double.valueOf(saldo) });
		}

		SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Buku Kas Simpan Pinjam",
				"Catatan uang masuk (setoran/angsuran) dan keluar (pencairan pinjaman) beserta sisa saldonya.",
				"Buku Kas", "buku_kas_simpan_pinjam",
				new String[] { "No", "Tanggal", "Uraian", "Pemasukan", "Pengeluaran", "Saldo" },
				new int[] { SimpanPinjamUiUtil.ANGKA, SimpanPinjamUiUtil.TANGGAL, SimpanPinjamUiUtil.TEKS,
						SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.RUPIAH },
				rows);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Jurnal Kas Masuk & Keluar (dikelompokkan per kategori — sesuai template)
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Bangun "Jurnal Kas Masuk": kelompokkan seluruh penerimaan kas koperasi per kategori sesuai
	 * template — Simpanan Pokok, Simpanan Wajib, Simpanan Sukarela, Angsuran Pokok, dan Jasa/Bunga
	 * Pinjaman. Simpanan diklasifikasi dari nama produk; angsuran &amp; jasa dari cicilan yang sudah
	 * terbayar ({@code pembayaranAnggotaKoperasiDetail is not null}).
	 */
	@SuppressWarnings("unchecked")
	private void buildJurnalKasMasuk(Session session, Long tipeSimpanan, Long tipePinjaman) {
		double simpananPokok = 0, simpananWajib = 0, simpananSukarela = 0, angsuranPokok = 0, jasaPinjaman = 0;
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/LaporanSimpanPinjamAction.java:642");
					}
				}
			}
			if (tipePinjaman != null) {
				List<TransaksiKoperasiDetail> bayar = session.createQuery(
						"select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
								+ "where d.pembayaranAnggotaKoperasiDetail is not null "
								+ "and t.produkKoperasi.tipeProdukKoperasi.id = :tipe")
						.setParameter("tipe", tipePinjaman).list();
				for (TransaksiKoperasiDetail d : bayar) {
					angsuranPokok += d.getPokok();
					jasaPinjaman += d.getMargin();
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		LinkedHashMap<String, Double> kategori = new LinkedHashMap<String, Double>();
		kategori.put("Simpanan Pokok", Double.valueOf(simpananPokok));
		kategori.put("Simpanan Wajib", Double.valueOf(simpananWajib));
		kategori.put("Simpanan Sukarela", Double.valueOf(simpananSukarela));
		kategori.put("Angsuran Pokok", Double.valueOf(angsuranPokok));
		kategori.put("Jasa/Bunga Pinjaman", Double.valueOf(jasaPinjaman));

		dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.barList("Jurnal Kas Masuk (per Kategori)",
				"Dari mana saja uang kas koperasi masuk, dikelompokkan per jenis penerimaan.", kategori,
				DashboardUiKit.GOOD, "", true, "Belum ada penerimaan kas.")));

		List<Object[]> rows = new ArrayList<Object[]>();
		double total = 0;
		for (java.util.Map.Entry<String, Double> e : kategori.entrySet()) {
			rows.add(new Object[] { e.getKey(), e.getValue() });
			total += e.getValue().doubleValue();
		}
		rows.add(new Object[] { "TOTAL KAS MASUK", Double.valueOf(total) });
		SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Jurnal Kas Masuk",
				"Ringkasan penerimaan kas koperasi per kategori (simpanan, angsuran, jasa).", "Jurnal Kas Masuk",
				"jurnal_kas_masuk", new String[] { "Kategori Penerimaan", "Jumlah" },
				new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.RUPIAH }, rows);
	}

	/**
	 * Bangun "Jurnal Kas Keluar": kelompokkan pengeluaran kas koperasi. Dari engine simpan pinjam,
	 * kategori yang dapat diturunkan adalah Penyaluran Pinjaman (pencairan pinjaman yang disetujui);
	 * biaya operasional (ATK, RAT, inventaris, transportasi) dicatat di modul akunting sehingga
	 * diberi catatan agar pengguna tidak salah paham.
	 */
	@SuppressWarnings("unchecked")
	private void buildJurnalKasKeluar(Session session, Long tipePinjaman) {
		double penyaluran = 0;
		try {
			if (tipePinjaman != null) {
				List<TransaksiKoperasi> pinj = session.createQuery(
						"select distinct t from TransaksiKoperasi t left join fetch t.produkKoperasi p "
								+ "left join fetch t.anggotaKoperasi a where p.tipeProdukKoperasi.id = :tipe")
						.setParameter("tipe", tipePinjaman).list();
				for (TransaksiKoperasi t : pinj) {
					try {
						if (t.getAktif() && TransaksiKoperasi.DISETUJU.equals(t.getStatus())) {
							penyaluran += t.getNilai();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/LaporanSimpanPinjamAction.java:705");
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		LinkedHashMap<String, Double> kategori = new LinkedHashMap<String, Double>();
		kategori.put("Penyaluran Pinjaman", Double.valueOf(penyaluran));

		dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.barList("Jurnal Kas Keluar (per Kategori)",
				"Untuk apa saja uang kas koperasi keluar.", kategori, DashboardUiKit.BAD, "", true,
				"Belum ada pengeluaran kas.")));
		dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
				"Catatan: biaya operasional (ATK, RAT, inventaris, transportasi) dicatat di modul akunting, "
						+ "sehingga belum ikut ditampilkan pada jurnal ini.")));

		List<Object[]> rows = new ArrayList<Object[]>();
		rows.add(new Object[] { "Penyaluran Pinjaman", Double.valueOf(penyaluran) });
		SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Jurnal Kas Keluar",
				"Ringkasan pengeluaran kas koperasi dari kegiatan simpan pinjam.", "Jurnal Kas Keluar",
				"jurnal_kas_keluar", new String[] { "Kategori Pengeluaran", "Jumlah" },
				new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.RUPIAH }, rows);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Buku Anggota (registrasi keanggotaan — sesuai template)
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Bangun "Buku Anggota": daftar seluruh anggota koperasi beserta identitas, tanggal masuk,
	 * status keanggotaan, serta tanggal &amp; alasan berhenti — mengikuti kolom template Buku
	 * Anggota. Anggota dianggap "Berhenti" bila memiliki tanggal berhenti atau tidak lagi aktif.
	 */
	@SuppressWarnings("unchecked")
	private void buildBukuAnggota(Session session) {
		List<Object[]> rows = new ArrayList<Object[]>();
		try {
			List<AnggotaKoperasi> anggota = session.createQuery(
					"select distinct a from AnggotaKoperasi a left join fetch a.jenisAnggotaKoperasi order by a.tanggal")
					.list();
			int no = 1;
			for (AnggotaKoperasi a : anggota) {
				try {
					String jenis = a.getJenisAnggotaKoperasi() == null || a.getJenisAnggotaKoperasi().getNama() == null
							? "-"
							: a.getJenisAnggotaKoperasi().getNama();
					boolean berhenti = a.getTanggalBerhenti() != null || !a.getAktif().booleanValue();
					rows.add(new Object[] { Integer.valueOf(no++), a.getNama(), a.getAlamat(), jenis, a.getTanggal(),
							berhenti ? "Berhenti" : "Aktif", a.getTanggalBerhenti(),
							a.getAlasanBerhenti() == null ? "" : a.getAlasanBerhenti() });
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Buku Anggota Koperasi",
				"Daftar seluruh anggota: identitas, tanggal masuk, status, serta tanggal & alasan berhenti.",
				"Buku Anggota", "buku_anggota",
				new String[] { "No", "Nama Lengkap", "Alamat", "Jenis Anggota", "Tgl Masuk", "Status", "Tgl Berhenti",
						"Alasan Berhenti" },
				new int[] { SimpanPinjamUiUtil.ANGKA, SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.TEKS,
						SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.TANGGAL, SimpanPinjamUiUtil.TEKS,
						SimpanPinjamUiUtil.TANGGAL, SimpanPinjamUiUtil.TEKS },
				rows);
	}

	// ════════════════════════════════════════════════════════════════════════════════════════
	// Laporan Promosi Ekonomi Anggota (manfaat yang diterima anggota)
	// ════════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Bangun "Laporan Promosi Ekonomi Anggota": memperlihatkan manfaat ekonomi yang diterima tiap
	 * anggota dari berkoperasi — total simpanan yang dititipkan, total pinjaman yang diterima, jasa
	 * (bunga) yang dibayar sebagai kontribusi ke koperasi, serta SHU yang diterima kembali. Laporan
	 * ini menegaskan tujuan koperasi: memberi manfaat nyata kepada anggotanya.
	 */
	@SuppressWarnings("unchecked")
	private void buildPromosiEkonomiAnggota(Session session, Long tipeSimpanan, Long tipePinjaman) {
		Map<Long, double[]> agg = new HashMap<Long, double[]>(); // [simpanan, pinjaman, jasaDibayar, shu]
		Map<Long, String> nama = new HashMap<Long, String>();
		try {
			if (tipeSimpanan != null) {
				List<TransaksiKoperasi> simp = session.createQuery(
						"select distinct t from TransaksiKoperasi t left join fetch t.anggotaKoperasi a "
								+ "where t.produkKoperasi.tipeProdukKoperasi.id = :tipe")
						.setParameter("tipe", tipeSimpanan).list();
				for (TransaksiKoperasi t : simp) {
					tambahManfaat(agg, nama, t.getAnggotaKoperasi(), 0, t.getNilai());
				}
			}
			if (tipePinjaman != null) {
				List<TransaksiKoperasi> pinj = session.createQuery(
						"select distinct t from TransaksiKoperasi t left join fetch t.anggotaKoperasi a "
								+ "left join fetch t.produkKoperasi p where p.tipeProdukKoperasi.id = :tipe")
						.setParameter("tipe", tipePinjaman).list();
				for (TransaksiKoperasi t : pinj) {
					try {
						if (t.getAktif()) {
							tambahManfaat(agg, nama, t.getAnggotaKoperasi(), 1, t.getNilai());
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/LaporanSimpanPinjamAction.java:810");
					}
				}
				List<TransaksiKoperasiDetail> bayar = session.createQuery(
						"select distinct d from TransaksiKoperasiDetail d left join fetch d.transaksiKoperasi t "
								+ "left join fetch t.anggotaKoperasi a where d.pembayaranAnggotaKoperasiDetail is not null "
								+ "and t.produkKoperasi.tipeProdukKoperasi.id = :tipe")
						.setParameter("tipe", tipePinjaman).list();
				for (TransaksiKoperasiDetail d : bayar) {
					TransaksiKoperasi t = d.getTransaksiKoperasi();
					if (t != null) {
						tambahManfaat(agg, nama, t.getAnggotaKoperasi(), 2, d.getMargin());
					}
				}
			}
			List<ShuAnggota> shus = session
					.createQuery("select distinct s from ShuAnggota s left join fetch s.anggota a").list();
			for (ShuAnggota s : shus) {
				tambahManfaat(agg, nama, s.getAnggota(), 3, s.getTotalShu());
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		double totSimp = 0, totPinj = 0, totJasa = 0, totShu = 0;
		List<Object[]> rows = new ArrayList<Object[]>();
		for (Map.Entry<Long, double[]> e : agg.entrySet()) {
			double[] v = e.getValue();
			String nm = nama.get(e.getKey()) == null ? "-" : nama.get(e.getKey());
			rows.add(new Object[] { nm, Double.valueOf(v[0]), Double.valueOf(v[1]), Double.valueOf(v[2]),
					Double.valueOf(v[3]) });
			totSimp += v[0];
			totPinj += v[1];
			totJasa += v[2];
			totShu += v[3];
		}

		List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
		kartu.add(new DashboardUiKit.Stat("Anggota Terlayani", DashboardUiKit.money(agg.size()), "menerima manfaat",
				DashboardUiKit.PRIMARY));
		kartu.add(new DashboardUiKit.Stat("Total Simpanan Anggota", "Rp " + DashboardUiKit.money(totSimp),
				"dana yang dititipkan", DashboardUiKit.GOOD));
		kartu.add(new DashboardUiKit.Stat("Total Pinjaman Diterima", "Rp " + DashboardUiKit.money(totPinj),
				"modal yang dipinjamkan", DashboardUiKit.ACCENT));
		kartu.add(new DashboardUiKit.Stat("Total SHU Dikembalikan", "Rp " + DashboardUiKit.money(totShu),
				"keuntungan untuk anggota", DashboardUiKit.WARN));
		dashHost.appendChild(DashboardUiKit.html("<div style='font-size:14px;font-weight:800;color:#0f172a;"
				+ "margin:18px 0 4px;border-left:4px solid " + DashboardUiKit.PRIMARY + ";padding-left:8px;'>"
				+ "Laporan Promosi Ekonomi Anggota</div>"));
		dashHost.appendChild(DashboardUiKit.html(DashboardUiKit
				.descChip("Manfaat nyata yang diterima anggota dari berkoperasi: simpanan, pinjaman, dan SHU.")));
		dashHost.appendChild(DashboardUiKit.html(DashboardUiKit.cards(kartu)));

		SimpanPinjamUiUtil.appendRekapGrid(dashHost, "Rincian Manfaat per Anggota",
				"Simpanan, pinjaman, jasa yang dibayar, dan SHU yang diterima tiap anggota.",
				"Promosi Ekonomi Anggota", "promosi_ekonomi_anggota",
				new String[] { "Anggota", "Total Simpanan", "Total Pinjaman", "Jasa Dibayar", "SHU Diterima" },
				new int[] { SimpanPinjamUiUtil.TEKS, SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.RUPIAH,
						SimpanPinjamUiUtil.RUPIAH, SimpanPinjamUiUtil.RUPIAH },
				rows);
	}

	/** Akumulasikan sebuah nilai manfaat ke slot {@code idx} milik seorang anggota. */
	private void tambahManfaat(Map<Long, double[]> agg, Map<Long, String> nama, AnggotaKoperasi a, int idx,
			double val) {
		try {
			if (a == null || a.getId() == null) {
				return;
			}
			Long id = a.getId();
			double[] v = agg.get(id);
			if (v == null) {
				v = new double[4];
				agg.put(id, v);
			}
			v[idx] += val;
			if (!nama.containsKey(id)) {
				nama.put(id, a.getNama() == null ? "-" : a.getNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/LaporanSimpanPinjamAction.java:889");
		}
	}

	/**
	 * Penampung sementara mutasi simpanan satu kombinasi anggota-produk selama proses hitung bunga:
	 * saldo awal bulan, daftar hari &amp; nominal setoran dalam bulan, serta metode dan suku bunga yang
	 * berlaku pada produk. Dipakai lokal oleh {@link #onBungaSimpanan(Event)} agar pengumpulan data
	 * ringkas dan hemat memori.
	 */
	private static final class AkumSimpanan {
		String namaAnggota = "-";
		String namaProduk = "-";
		String metode;
		double bungaPersen;
		double saldoAwal;
		final List<Integer> hari = new ArrayList<Integer>();
		final List<Double> nominal = new ArrayList<Double>();
	}
}
