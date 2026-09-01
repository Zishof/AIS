package ais.action.report;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.fill.JRSwapFileVirtualizer;
import net.sf.jasperreports.engine.util.JRSwapFile;

/**
 * Pengendali beban pembuatan laporan Jasper (optimasi RAM Fase 6). Dua mekanisme:
 *
 * <ol>
 *   <li><b>Batas concurrency</b> — {@link Semaphore} global membatasi jumlah laporan yang
 *       di-fill/export BERSAMAAN per JVM (konfigurasi {@code report_maks_paralel}, default 4).
 *       Sebelumnya tak terbatas: puluhan request cetak paralel = puluhan {@code JasperPrint}
 *       penuh di heap sekaligus (pemicu utama puncak Old Gen). Penunggu antre adil (FIFO)
 *       hingga {@code report_tunggu_antrian_detik} (default 120). Setelah batas tersebut,
 *       secara bawaan penunggu tetap mengantre agar laporan tidak gagal hanya karena lonjakan
 *       sesaat. Administrator dapat mengaktifkan {@code report_gagalkan_jika_antrian_penuh}
 *       bila instalasi memang menghendaki perilaku gagal-cepat.</li>
 *   <li><b>Virtualizer</b> — {@link JRSwapFileVirtualizer} per job: halaman laporan melebihi
 *       {@code report_virtualizer_max_halaman_memori} (default 200) di-swap ke berkas temp,
 *       bukan ditahan seluruhnya di heap. Nonaktifkan via konfigurasi
 *       {@code report_virtualizer_nonaktif} = aktif. {@code cleanup()} WAJIB di {@code finally}
 *       pemanggil SETELAH export selesai (halaman dibaca ulang dari swap saat export).</li>
 * </ol>
 *
 * Semua kegagalan internal helper ini bersifat best-effort dan tidak boleh menggagalkan
 * pembuatan laporan (fallback = perilaku lama tanpa virtualizer).
 */
public final class ReportThrottle {

	private static volatile Semaphore izin = null;

	private ReportThrottle() {
	}

	private static int bacaKonfigInt(String kunci, int nilaiDefault) {
		try {
			return Integer.parseInt(
					ais.common.Common.getKonfigurasi(kunci, String.valueOf(nilaiDefault)).getNilai().trim());
		} catch (Throwable t) {
			return nilaiDefault;
		}
	}

	/** Semaphore dibuat malas pada pemakaian pertama; jumlah permit dibaca sekali seumur JVM. */
	private static Semaphore ambilSemaphore() {
		Semaphore s = izin;
		if (s == null) {
			synchronized (ReportThrottle.class) {
				if (izin == null) {
					int permit = bacaKonfigInt("report_maks_paralel", 4);
					if (permit < 1) {
						permit = 1;
					}
					izin = new Semaphore(permit, true);
					System.out.println("ReportThrottle: batas laporan paralel per JVM = " + permit);
				}
				s = izin;
			}
		}
		return s;
	}

	/**
	 * Ringkasan pemakaian slot cetak laporan untuk snapshot performa (OPTIMASI FASE 10).
	 * Membuktikan batas concurrency laporan (Fase 6) benar-benar berlaku. Tidak pernah
	 * melempar exception dan TIDAK membuat semaphore bila belum pernah dipakai.
	 */
	public static String statistik() {
		try {
			Semaphore s = izin;
			if (s == null) {
				return "belum ada laporan dijalankan sejak start";
			}
			return "slot bebas=" + s.availablePermits()
					+ ", menunggu=" + s.getQueueLength();
		} catch (Throwable t) {
			return "gagal dibaca: " + t.getClass().getSimpleName();
		}
	}

	/**
	 * Ambil satu slot cetak; menunggu adil (FIFO) hingga batas waktu. Secara bawaan
	 * proses tetap mengantre setelah batas waktu sehingga pembatas RAM tetap berlaku
	 * tanpa menggagalkan laporan. Perilaku lama (gagal-cepat) dapat diaktifkan melalui
	 * konfigurasi {@code report_gagalkan_jika_antrian_penuh}.
	 *
	 * @return true bila slot diperoleh (WAJIB dilepas via {@link #lepasIzin(boolean)})
	 */
	public static boolean ambilIzin() throws Exception {
		int tungguDetik = bacaKonfigInt("report_tunggu_antrian_detik", 120);
		if (tungguDetik < 1) {
			tungguDetik = 1;
		}
		try {
			Semaphore semaphore = ambilSemaphore();
			if (semaphore.tryAcquire(tungguDetik, TimeUnit.SECONDS)) {
				return true;
			}
			if (ais.common.Common.bolehKonfigurasi("report_gagalkan_jika_antrian_penuh",
					ais.database.model.Konfigurasi.TIDAK_AKTIF)) {
				throw new Exception("Server sedang memproses banyak laporan secara bersamaan (menunggu "
						+ tungguDetik + " detik). Silakan coba cetak ulang beberapa saat lagi.");
			}
			System.out.println("ReportThrottle: antrean melewati " + tungguDetik
					+ " detik; proses tetap menunggu slot agar laporan tidak gagal dan batas RAM tetap terjaga.");
			semaphore.acquire();
			return true;
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new Exception("Pembuatan laporan dibatalkan saat menunggu antrean cetak.", ie);
		}
	}

	/** Lepas slot cetak. Aman dipanggil dari finally; no-op bila slot tidak pernah diperoleh. */
	public static void lepasIzin(boolean diperoleh) {
		if (!diperoleh) {
			return;
		}
		try {
			ambilSemaphore().release();
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "ReportThrottle.lepasIzin");
		}
	}

	/**
	 * Pasang virtualizer swap-file ke parameter fill Jasper. Return null (tanpa virtualizer,
	 * perilaku lama) bila dinonaktifkan konfigurasi atau pembuatan gagal.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static JRSwapFileVirtualizer pasangVirtualizer(Map parameters) {
		try {
			if (parameters == null) {
				return null;
			}
			if (ais.common.Common.bolehKonfigurasi("report_virtualizer_nonaktif",
					ais.database.model.Konfigurasi.TIDAK_AKTIF)) {
				return null;
			}
			int maksHalamanMemori = bacaKonfigInt("report_virtualizer_max_halaman_memori", 200);
			if (maksHalamanMemori < 10) {
				maksHalamanMemori = 10;
			}
			File dirSwap = new File(System.getProperty("java.io.tmpdir"), "ais-report-swap");
			if (!dirSwap.exists()) {
				dirSwap.mkdirs();
			}
			JRSwapFile swap = new JRSwapFile(dirSwap.getAbsolutePath(), 4096, 100);
			JRSwapFileVirtualizer virtualizer = new JRSwapFileVirtualizer(maksHalamanMemori, swap, true);
			parameters.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);
			return virtualizer;
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "ReportThrottle.pasangVirtualizer");
			return null;
		}
	}

	/**
	 * Bersihkan virtualizer + lepaskan referensinya dari map parameter. WAJIB dipanggil di
	 * {@code finally} setelah export selesai (sukses/gagal) — tanpa ini berkas swap temp
	 * menumpuk dan halaman ter-pin.
	 */
	@SuppressWarnings("rawtypes")
	public static void bersihkanVirtualizer(JRSwapFileVirtualizer virtualizer, Map parameters) {
		try {
			if (parameters != null) {
				parameters.remove(JRParameter.REPORT_VIRTUALIZER);
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "ReportThrottle.bersihkanVirtualizer.param");
		}
		if (virtualizer != null) {
			try {
				virtualizer.cleanup();
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "ReportThrottle.bersihkanVirtualizer");
			}
		}
	}
}
