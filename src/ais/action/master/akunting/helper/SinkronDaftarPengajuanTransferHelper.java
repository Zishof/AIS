package ais.action.master.akunting.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.JenisKasKecil;
import ais.database.model.akunting.KasBesar;
import ais.database.model.akunting.Pajak;
import ais.database.model.akunting.PenggantianKasKecil;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.PertangungjawabanKasBesar;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.PembayaranDpMasterAssetDetail;
import ais.database.model.asset.PembayaranPengadaanMasterAssetDetail;
import ais.database.model.asset.PembayaranTerminMasterAssetDetail;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.payroll.PengajuanTransaksiPegawai;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>SinkronDaftarPengajuanTransferHelper — "Singkronkan" Daftar Transfer (DPC)</h1>
 *
 * <p><b>Untuk apa.</b> Tombol <i>Singkronkan</i> pada layar "Tambah Proses Transfer" dan tab
 * "Daftar Transfer" memeriksa ULANG seluruh tabel (class) yang menjadi <b>referensi</b> Daftar
 * Pengajuan Transfer, lalu menautkan data yang SUDAH SIAP (mis. sudah disetujui) namun BELUM
 * memiliki baris {@link DaftarPengajuanTransfer}. Berguna ketika sinkronisasi otomatis saat simpan
 * terlewat (data hanya muncul di DPC setelah entitasnya dibuka ulang).</p>
 *
 * <p><b>Sumber referensi</b> = 15 tipe entitas yang dirujuk oleh method {@code DaftarPengajuanTransfer.simpanXxx(...)}:
 * Uang Muka, Kas Besar, Dana Talangan, Penggantian Kas Kecil, Jenis Kas Kecil (saldo awal),
 * Pengajuan Transaksi Pegawai, Transaksi Koperasi (pinjaman), Pembayaran DP/Termin/Pengadaan aset,
 * Saldo Awal Master Aset, Pertanggungjawaban &amp; Pertanggungjawaban Kas Besar (LPJ), Pajak (PPh),
 * dan Diskon Pembayaran (Tagihan). Method {@code simpanXxx} bersifat <b>idempoten</b> — bila baris
 * DPC sudah ada, ia tidak membuat ganda.</p>
 *
 * <p><b>Kenapa multi-thread.</b> Memeriksa ratusan/ribuan baris di banyak tabel lambat bila serial.
 * Helper memakai kolam thread (plafon permintaan {@value #MAKS_THREAD}, di-<i>clamp</i> ke
 * {@value #BATAS_KONEKSI_AMAN} demi menjaga pool koneksi c3p0 {@code max_size=80} tetap punya sisa
 * untuk pengguna lain). Tiap worker memakai {@code Session} Hibernate SENDIRI lewat
 * {@code currentNativeSession()} yang bersifat <b>thread-local</b>; method {@code simpanXxx}
 * membuka/menutup session-nya per baris. Progres ditampilkan lewat {@link Progressmeter} yang
 * diperbarui {@link Timer} (persen = baris selesai / total), sementara pemrosesan berjalan di utas
 * latar (pola sama seperti {@code DownloadFotoMassalHelper}).</p>
 *
 * <p><b>Gating.</b> Hanya satu {@code simpanXxx} yang menyaring persetujuan sendiri
 * ({@code simpanSaldoAwalMasterAsset}); sisanya mempercayai pemanggil. Karena itu helper meniru
 * gating para pemanggil interaktif (status {@code "Disetujui"}/{@code disetujuiOleh}/{@code setujui},
 * dsb.) di sisi worker. <b>PENTING:</b> {@code getStatus()}/{@code getDisetujuiOleh()} adalah getter
 * <i>terhitung</i> yang menyembuhkan diri dari graf disposisi SOP, sehingga kolom persisten
 * {@code disetujui_oleh} kerap NULL untuk data yang disetujui via alur SOP. Karena itu query kandidat
 * <b>TIDAK</b> menyaring {@code disetujui_oleh} (agar tidak melewatkan data yang disetujui via SOP) —
 * cukup {@code daftarPengajuanTransfer is null} (belum tertaut DPC); keputusan "sudah disetujui?"
 * diserahkan ke gate <i>terhitung</i> di worker + method {@code simpanXxx} yang idempoten. Dengan begitu
 * SEMUA data yang disetujui namun DPC-nya kosong ikut tertaut, tanpa batas jumlah.</p>
 */
public final class SinkronDaftarPengajuanTransferHelper {

	/** Plafon jumlah thread sesuai permintaan (maksimal 100 berjalan bersama). */
	public static final int MAKS_THREAD = 100;

	/**
	 * Batas aman konkurensi koneksi. Pool c3p0 utama {@code hibernate.c3p0.max_size=80}; kita sisakan
	 * headroom agar pengguna lain tidak kehabisan koneksi saat sinkronisasi berjalan.
	 */
	private static final int BATAS_KONEKSI_AMAN = 40;

	private SinkronDaftarPengajuanTransferHelper() {
	}

	/**
	 * Entry point tanpa komponen ZK untuk New UI. Semua kandidat tetap memakai daftar
	 * sumber dan gating yang sama dengan layar existing, tetapi diproses berurutan agar
	 * satu request HTTP tidak menghabiskan pool koneksi PostgreSQL.
	 */
	public static HasilSinkron sinkronkanHeadless() {
		HasilSinkron hasil = new HasilSinkron();
		List<Object[]> tugas = new ArrayList<Object[]>();
		Session baca = null;
		try {
			baca = HibernateUtil.openSession();
			for (Sumber sumber : daftarSumber()) {
				try {
					List<Long> ids = sumber.kandidat(baca);
					if (ids != null) for (Long id : ids) if (id != null) tugas.add(new Object[] { sumber, id });
				} catch (Exception ex) {
					hasil.gagal++;
					hasil.pesan.add(sumber.nama() + ": " + ex.getMessage());
					try { ais.common.ErrorAuditUtil.record(ex, "SinkronDaftarPengajuanTransferHelper.headless.query"); } catch (Exception ignored) { }
				}
			}
		} finally {
			HibernateUtil.closeSessionQuietly(baca);
		}
		hasil.total = tugas.size();
		for (Object[] tugasSatu : tugas) {
			Sumber sumber = (Sumber) tugasSatu[0];
			Long id = (Long) tugasSatu[1];
			try {
				if (sumber.sinkronSatu(id)) hasil.berhasil++; else hasil.dilewati++;
			} catch (Throwable ex) {
				hasil.gagal++;
				hasil.pesan.add(sumber.nama() + " ID " + id + ": " + ex.getMessage());
				try { ais.common.ErrorAuditUtil.record(ex instanceof Exception ? (Exception) ex : new RuntimeException(ex), "SinkronDaftarPengajuanTransferHelper.headless.item"); } catch (Exception ignored) { }
			} finally {
				try { HibernateUtil.closeSession(); } catch (Exception ignored) { }
			}
		}
		return hasil;
	}

	public static final class HasilSinkron {
		public int total;
		public int berhasil;
		public int dilewati;
		public int gagal;
		public final List<String> pesan = new ArrayList<String>();
	}

	// ── Titik masuk (dipanggil dari tombol) ─────────────────────────────────────────────────────

	/**
	 * Jalankan sinkronisasi dengan bar progres modal. {@code owner} dipakai untuk menemukan halaman
	 * (boleh null → fallback ke halaman aktif). {@code onSelesai} (boleh null) dijalankan di utas event
	 * setelah selesai — dipakai pemanggil untuk me-refresh grid/daftar.
	 */
	public static void sinkronkan(final Component owner, final EventListener onSelesai) {
		final Component root = cariRoot(owner);
		if (root == null) {
			try {
				MyMessageboxConfig.show("Tidak dapat menampilkan progres sinkronisasi (halaman tidak ditemukan).",
						"Kesalahan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/SinkronDaftarPengajuanTransferHelper.java:105");
			}
			return;
		}

		// Jendela progres (modal, tidak bisa ditutup manual).
		final MyWindow win = new MyWindow("Sinkronisasi Daftar Transfer", "normal", false);
		win.setWidth("500px");
		win.setClosable(false);
		win.setParent(root);
		Vbox box = new Vbox();
		box.setWidth("100%");
		box.setStyle("padding:16px 18px;");
		box.setParent(win);
		new Label(ais.common.Common.getBahasaConfig("Memeriksa ulang & menyinkronkan data pengajuan transfer, harap tunggu...")).setParent(box);
		final Progressmeter meter = new Progressmeter();
		meter.setValue(0);
		meter.setWidth("100%");
		meter.setParent(box);
		final Label lbl = new Label(ais.common.Common.getBahasaConfig("Menyiapkan daftar data..."));
		lbl.setStyle("font-weight:800;color:#1e3a5f;font-size:13px;");
		lbl.setParent(box);
		win.doHighlighted();

		final AtomicInteger total = new AtomicInteger(-1); // -1 = daftar kandidat belum siap
		final AtomicInteger diproses = new AtomicInteger(0);
		final AtomicInteger berhasil = new AtomicInteger(0);
		final AtomicInteger gagal = new AtomicInteger(0);
		final AtomicBoolean selesai = new AtomicBoolean(false);
		final AtomicReference<String> pesanError = new AtomicReference<String>();
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Daftar Pengajuan Transfer");

		// Utas latar: kumpulkan kandidat lalu proses paralel.
		Thread worker = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					jalankan(total, diproses, berhasil, gagal, laporan);
				} catch (Throwable t) {
					pesanError.set(String.valueOf(t.getMessage()));
					laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-baris): "
							+ ais.common.LaporanUpload.detailTeknisException(t));
				} finally {
					selesai.set(true);
				}
			}
		});
		worker.setDaemon(true);
		worker.start();

		// Timer: perbarui bar progres; saat selesai → tutup jendela + jalankan onSelesai.
		final Timer timer = new Timer(400);
		timer.setRepeats(true);
		timer.setParent(root);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				int t = total.get();
				int d = diproses.get();
				if (t < 0) {
					lbl.setValue("Menyiapkan daftar data...");
				} else if (t == 0) {
					meter.setValue(100);
					lbl.setValue("Tidak ada data yang perlu disinkronkan.");
				} else {
					int pct = (int) ((long) d * 100L / t);
					if (pct > 100) {
						pct = 100;
					}
					meter.setValue(pct);
					lbl.setValue(pct + "%   (" + d + " / " + t + ")   •   Tertaut: " + berhasil.get()
							+ (gagal.get() > 0 ? "   •   Gagal: " + gagal.get() : ""));
				}
				if (selesai.get()) {
					try {
						timer.stop();
						timer.detach();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/SinkronDaftarPengajuanTransferHelper.java:179");
					}
					try {
						win.detach();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/SinkronDaftarPengajuanTransferHelper.java:183");
					}
					laporan.selesaikan(new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							if (onSelesai != null) {
								try {
									onSelesai.onEvent(new Event("onSinkronSelesai", root));
								} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/SinkronDaftarPengajuanTransferHelper.java:198");
								}
							}
						}
					});
				}
			}
		});
		timer.start();
	}

	// ── Inti pemrosesan (utas latar) ────────────────────────────────────────────────────────────

	/**
	 * Kumpulkan kandidat dari seluruh sumber (satu session khusus), lalu proses paralel. {@code total}
	 * di-set setelah daftar kandidat siap; {@code diproses}/{@code berhasil}/{@code gagal} di-update
	 * per baris untuk bar progres.
	 */
	private static void jalankan(final AtomicInteger total, final AtomicInteger diproses,
			final AtomicInteger berhasil, final AtomicInteger gagal, final ais.common.LaporanUpload laporan) {

		final List<Sumber> daftarSumber = daftarSumber();
		final List<Object[]> tugas = new ArrayList<Object[]>(); // {Sumber, Long id}

		// Fase-0: kumpulkan id kandidat (ringan, hanya id). Satu tipe gagal query tidak menggagalkan
		// keseluruhan.
		Session s = null;
		try {
			s = HibernateUtil.openSession();
			for (Sumber sm : daftarSumber) {
				try {
					List<Long> ids = sm.kandidat(s);
					if (ids != null) {
						for (Long id : ids) {
							if (id != null) {
								tugas.add(new Object[] { sm, id });
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/SinkronDaftarPengajuanTransferHelper.java:236");
					laporan.tambahCatatan("Gagal mengambil daftar kandidat sumber \"" + sm.nama() + "\": "
							+ ais.common.LaporanUpload.detailTeknisException(e));
				}
			}
		} finally {
			HibernateUtil.closeSessionQuietly(s);
		}

		total.set(tugas.size());
		if (tugas.isEmpty()) {
			return;
		}

		// Fase-1: proses paralel. Peak koneksi = jumlah worker (tiap simpanXxx buka/tutup session-nya).
		final int threads = Math.min(Math.min(MAKS_THREAD, BATAS_KONEKSI_AMAN), Math.max(1, tugas.size()));
		final ExecutorService pool = Executors.newFixedThreadPool(threads);
		final AtomicInteger nomorBarisLaporan = new AtomicInteger(0);
		try {
			List<Future<?>> futures = new ArrayList<Future<?>>();
			for (final Object[] t : tugas) {
				final Sumber sm = (Sumber) t[0];
				final Long id = (Long) t[1];
				futures.add(pool.submit(new Runnable() {
					@Override
					public void run() {
						int nomorBaris = nomorBarisLaporan.getAndIncrement();
						String kunci = sm.nama() + " ID:" + id;
						try {
							if (sm.sinkronSatu(id)) {
								berhasil.incrementAndGet();
								laporan.catatBerhasil(nomorBaris, kunci, "Ditautkan ke Daftar Pengajuan Transfer");
							} else {
								laporan.catatDilewati(nomorBaris, kunci,
										"Belum memenuhi syarat gating (mis. belum disetujui) - dilewati, bukan gagal");
							}
						} catch (Throwable ex) {
							gagal.incrementAndGet();
							laporan.catatGagalDetail(nomorBaris, kunci,
									ex instanceof Exception ? (Exception) ex : new RuntimeException(ex));
						} finally {
							// Pastikan session thread-local bersih tiap baris (cegah bloat cache & tahan koneksi).
							try {
								HibernateUtil.closeSession();
							} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/SinkronDaftarPengajuanTransferHelper.java:269");
							}
							diproses.incrementAndGet();
						}
					}
				}));
			}
			for (Future<?> f : futures) {
				try {
					f.get();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/SinkronDaftarPengajuanTransferHelper.java:279");
				}
			}
		} finally {
			pool.shutdown();
		}
	}

	// ── Definisi 15 sumber referensi ────────────────────────────────────────────────────────────

	/**
	 * Deskriptor satu tipe entitas referensi: cara mengambil id kandidat (belum tertaut DPC &amp;
	 * memenuhi pra-saring), dan cara menyinkronkan satu baris (muat → cek gating → {@code simpanXxx}).
	 * {@code sinkronSatu} mengembalikan {@code true} bila baris benar-benar (atau seharusnya) tertaut.
	 */
	private abstract static class Sumber {
		abstract String nama();

		@SuppressWarnings("unchecked")
		protected List<Long> query(Session s, String hql) {
			return s.createQuery(hql).list();
		}

		abstract List<Long> kandidat(Session s);

		abstract boolean sinkronSatu(Long id);
	}

	private static List<Sumber> daftarSumber() {
		List<Sumber> l = new ArrayList<Sumber>();

		// 1. Uang Muka — gating: status "Disetujui" (≡ disetujuiOleh != null).
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Uang Muka";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select u.id from ais.database.model.akunting.UangMuka u "
						+ "where u.daftarPengajuanTransfer is null");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				UangMuka e = (UangMuka) s.get(UangMuka.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				if (!UangMuka.DISETUJU.equals(e.getStatus())) {
					return false;
				}
				DaftarPengajuanTransfer.simpanUangMuka(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 2. Kas Besar — gating: status "Disetujui".
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Kas Besar";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select k.id from ais.database.model.akunting.KasBesar k "
						+ "where k.daftarPengajuanTransfer is null");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				KasBesar e = (KasBesar) s.get(KasBesar.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				if (!KasBesar.DISETUJU.equals(e.getStatus())) {
					return false;
				}
				DaftarPengajuanTransfer.simpanKasBesar(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 3. Dana Talangan — gating: status "Disetujui" + disetujuiOleh != null.
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Dana Talangan";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select d.id from ais.database.model.akunting.DanaTalangan d "
						+ "where d.daftarPengajuanTransfer is null");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				DanaTalangan e = (DanaTalangan) s.get(DanaTalangan.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				if (e.getDisetujuiOleh() == null || !DanaTalangan.DISETUJU.equals(e.getStatus())) {
					return false;
				}
				DaftarPengajuanTransfer.simpanDanaTalangan(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 4. Penggantian Kas Kecil — gating: status "Disetujui" + disetujuiOleh (method skip penutupan).
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Penggantian Kas Kecil";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select p.id from ais.database.model.akunting.PenggantianKasKecil p "
						+ "where p.daftarPengajuanTransfer is null");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				PenggantianKasKecil e = (PenggantianKasKecil) s.get(PenggantianKasKecil.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				if (e.getDisetujuiOleh() == null || !PenggantianKasKecil.DISETUJU.equals(e.getStatus())) {
					return false;
				}
				DaftarPengajuanTransfer.simpanPenggantianKasKecil(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 5. Jenis Kas Kecil (saldo awal) — data master; tidak ada gating persetujuan.
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Jenis Kas Kecil";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select j.id from ais.database.model.akunting.JenisKasKecil j "
						+ "where j.daftarPengajuanTransfer is null");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				JenisKasKecil e = (JenisKasKecil) s.get(JenisKasKecil.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				DaftarPengajuanTransfer.simpanJenisKasKecil(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 6. Pengajuan Transaksi Pegawai — gating: setujui == true.
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Pengajuan Transaksi Pegawai";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select p.id from ais.database.model.payroll.PengajuanTransaksiPegawai p "
						+ "where p.daftarPengajuanTransfer is null and p.setujui = true");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				PengajuanTransaksiPegawai e = (PengajuanTransaksiPegawai) s.get(PengajuanTransaksiPegawai.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				if (!Boolean.TRUE.equals(e.getSetujui())) {
					return false;
				}
				DaftarPengajuanTransfer.simpanPengajuanTransaksiPegawai(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 7. Transaksi Koperasi — gating: status "Disetujui" (method hanya proses produk PINJAMAN).
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Transaksi Koperasi";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select t.id from ais.database.model.koperasi.TransaksiKoperasi t "
						+ "where t.daftarPengajuanTransfer is null");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				TransaksiKoperasi e = (TransaksiKoperasi) s.get(TransaksiKoperasi.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				if (!TransaksiKoperasi.DISETUJU.equals(e.getStatus())) {
					return false;
				}
				DaftarPengajuanTransfer.simpanTransaksiKoperasi(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 8. Pembayaran DP Master Aset (detail) — tanpa gating persetujuan (didorong ke DPC saat simpan).
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Pembayaran DP";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select d.id from ais.database.model.asset.PembayaranDpMasterAssetDetail d "
						+ "where d.daftarPengajuanTransfer is null");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				PembayaranDpMasterAssetDetail e = (PembayaranDpMasterAssetDetail) s
						.get(PembayaranDpMasterAssetDetail.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				DaftarPengajuanTransfer.simpanPembayaranDpMasterAssetDetail(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 9. Pembayaran Termin Master Aset (detail) — gating: header disetujuiOleh != null.
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Pembayaran Termin";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select d.id from ais.database.model.asset.PembayaranTerminMasterAssetDetail d "
						+ "where d.daftarPengajuanTransfer is null");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				PembayaranTerminMasterAssetDetail e = (PembayaranTerminMasterAssetDetail) s
						.get(PembayaranTerminMasterAssetDetail.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				if (e.getPembayaranTerminMasterAsset() == null
						|| e.getPembayaranTerminMasterAsset().getDisetujuiOleh() == null) {
					return false;
				}
				DaftarPengajuanTransfer.simpanPembayaranTerminMasterAssetDetail(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 10. Pembayaran Pengadaan Master Aset (detail) — tanpa gating persetujuan.
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Pembayaran Pengadaan";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select d.id from ais.database.model.asset.PembayaranPengadaanMasterAssetDetail d "
						+ "where d.daftarPengajuanTransfer is null");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				PembayaranPengadaanMasterAssetDetail e = (PembayaranPengadaanMasterAssetDetail) s
						.get(PembayaranPengadaanMasterAssetDetail.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				DaftarPengajuanTransfer.simpanPembayaranPengadaanMasterAssetDetail(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 11. Saldo Awal Master Aset — method sudah menyaring disetujuiOleh sendiri.
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Saldo Awal Master Aset";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select a.id from ais.database.model.asset.SaldoAwalMasterAsset a "
						+ "where a.daftarPengajuanTransfer is null");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				SaldoAwalMasterAsset e = (SaldoAwalMasterAsset) s.get(SaldoAwalMasterAsset.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				DaftarPengajuanTransfer.simpanSaldoAwalMasterAsset(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 12. Pertanggungjawaban (LPJ) — method menyaring disetujuiOleh & dikembalikan>0.1 sendiri.
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Pertanggungjawaban";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select p.id from ais.database.model.akunting.Pertangungjawaban p "
						+ "where p.daftarPengajuanTransfer is null");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				Pertangungjawaban e = (Pertangungjawaban) s.get(Pertangungjawaban.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				DaftarPengajuanTransfer.simpanPertangungjawaban(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 13. Pertanggungjawaban Kas Besar (LPJ) — method menyaring disetujuiOleh & dikembalikan>0.1.
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Pertanggungjawaban Kas Besar";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select p.id from ais.database.model.akunting.PertangungjawabanKasBesar p "
						+ "where p.daftarPengajuanTransfer is null");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				PertangungjawabanKasBesar e = (PertangungjawabanKasBesar) s.get(PertangungjawabanKasBesar.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				DaftarPengajuanTransfer.simpanPertangungjawabanKasBesar(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 14. Pajak (PPh) — dibuat oleh proses lain; simpanPajak idempoten menautkan yang belum tertaut.
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Pajak";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select p.id from ais.database.model.akunting.Pajak p "
						+ "where p.daftarPengajuanTransfer is null and (p.aktif is null or p.aktif = true)");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				Pajak e = (Pajak) s.get(Pajak.class, id);
				if (e == null || e.getDaftarPengajuanTransfer() != null) {
					return false;
				}
				DaftarPengajuanTransfer.simpanPajak(e);
				return e.getDaftarPengajuanTransfer() != null;
			}
		});

		// 15. Diskon Pembayaran (Tagihan) — Tagihan TIDAK punya back-ref; pakai anti-join ke DPC.
		// simpanDiskonPembayaran menyaring sendiri (diskonSiswa, !memotongTagihan, diskonTidakLangsung>0.1).
		l.add(new Sumber() {
			@Override
			String nama() {
				return "Diskon Pembayaran";
			}

			@Override
			List<Long> kandidat(Session s) {
				return query(s, "select t.id from ais.database.model.sekolah.Tagihan t "
						+ "where t.diskonSiswa is not null and t.id not in "
						+ "(select d.diskonTagihan.id from ais.database.model.akunting.DaftarPengajuanTransfer d "
						+ "where d.diskonTagihan is not null)");
			}

			@Override
			boolean sinkronSatu(Long id) {
				Session s = HibernateUtil.currentNativeSession();
				Tagihan e = (Tagihan) s.get(Tagihan.class, id);
				if (e == null) {
					return false;
				}
				// Meniru gating simpanDiskonPembayaran untuk nilai kembalian yang jujur.
				boolean layak = e.getDiskonSiswa() != null
						&& Boolean.FALSE.equals(e.getDiskonSiswa().getMemotongTagihan())
						&& e.getDiskonTidakLangsung() != null && e.getDiskonTidakLangsung() > 0.1;
				DaftarPengajuanTransfer.simpanDiskonPembayaran(e);
				return layak;
			}
		});

		return l;
	}

	// ── Util ────────────────────────────────────────────────────────────────────────────────────

	/** Temukan root halaman dari {@code owner} (atau halaman aktif bila null). */
	private static Component cariRoot(Component owner) {
		try {
			if (owner != null && owner.getPage() != null) {
				return owner.getPage().getFirstRoot();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/SinkronDaftarPengajuanTransferHelper.java:728");
		}
		try {
			return ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
		} catch (Exception e) {
			return null;
		}
	}
}
