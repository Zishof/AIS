package ais.common;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.util.DesktopCleanup;
import org.zkoss.zk.ui.util.DesktopInit;

/**
 * Penghitung ZK Desktop untuk snapshot performa (OPTIMASI FASE 10).
 *
 * <p><b>Mengapa perlu.</b> Fase 5 membatasi {@code max-desktops-per-session} menjadi 15 dan
 * {@code max-pushes-per-session} menjadi 15. Tanpa pengukuran, tidak ada cara membuktikan
 * apakah batas itu terlalu ketat (pengguna terganggu) atau terlalu longgar (memori terbuang).
 * Setiap desktop menahan seluruh pohon komponen ZK-nya di memori sesi, sehingga jumlah
 * desktop hidup adalah indikator langsung pemakaian RAM dari sisi UI.</p>
 *
 * <p><b>Keamanan memori.</b> Kelas ini HANYA menyimpan angka; tidak pernah menyimpan referensi
 * ke {@link Desktop}, sesi, komponen, atau entitas. Jadi tidak mungkin menjadi sumber kebocoran
 * baru. Rincian per-sesi disimpan sebagai satu {@code Integer} pada atribut ZK Session sehingga
 * ikut mati bersama sesinya.</p>
 *
 * <p><b>Privasi.</b> Tidak ada ID pengguna, ID sesi, atau parameter URL yang dicatat -- sesuai
 * aturan observability (jangan membuat label ber-cardinality tinggi / PII).</p>
 *
 * <p>Didaftarkan di {@code zk.xml} sebagai {@code <listener>}; ZK memanggil {@link #init} saat
 * desktop dibuat dan {@link #cleanup} saat desktop dilepas (tab ditutup / timeout).</p>
 */
public class DesktopCounterListener implements DesktopInit, DesktopCleanup {

	/** Nama atribut penghitung desktop pada ZK Session (mati bersama sesinya). */
	private static final String ATTR_JUMLAH_SESI = "aisDesktopCounter.jumlah";

	private static final AtomicInteger aktif = new AtomicInteger(0);
	private static final AtomicInteger puncak = new AtomicInteger(0);
	private static final AtomicLong totalDibuat = new AtomicLong(0L);
	/** Desktop terbanyak yang pernah dipegang SATU sesi (menguji ambang max-desktops-per-session). */
	private static final AtomicInteger puncakPerSesi = new AtomicInteger(0);

	@Override
	public void init(Desktop desktop, Object request) throws Exception {
		try {
			totalDibuat.incrementAndGet();
			int sekarang = aktif.incrementAndGet();
			naikkanPuncak(puncak, sekarang);
			naikkanPuncak(puncakPerSesi, ubahJumlahSesi(desktop, 1));
		} catch (Throwable t) {
			// Observability TIDAK boleh menggagalkan pembuatan desktop.
			ErrorAuditUtil.record(t, "DesktopCounterListener.init");
		}
	}

	@Override
	public void cleanup(Desktop desktop) throws Exception {
		try {
			if (aktif.decrementAndGet() < 0) {
				aktif.set(0);
			}
			ubahJumlahSesi(desktop, -1);
		} catch (Throwable t) {
			ErrorAuditUtil.record(t, "DesktopCounterListener.cleanup");
		}
	}

	/** Naikkan nilai puncak secara atomik tanpa mengunci. */
	private static void naikkanPuncak(AtomicInteger target, int kandidat) {
		while (true) {
			int lama = target.get();
			if (kandidat <= lama || target.compareAndSet(lama, kandidat)) {
				return;
			}
		}
	}

	/**
	 * Ubah penghitung desktop milik satu ZK Session dan kembalikan nilai barunya.
	 * Nilai disimpan pada atribut sesi sehingga otomatis hilang saat sesi berakhir.
	 */
	private static int ubahJumlahSesi(Desktop desktop, int delta) {
		try {
			if (desktop == null || desktop.getSession() == null) {
				return 0;
			}
			Object lama = desktop.getSession().getAttribute(ATTR_JUMLAH_SESI);
			int nilai = (lama instanceof Integer ? ((Integer) lama).intValue() : 0) + delta;
			if (nilai < 0) {
				nilai = 0;
			}
			desktop.getSession().setAttribute(ATTR_JUMLAH_SESI, Integer.valueOf(nilai));
			return nilai;
		} catch (Throwable t) {
			return 0;
		}
	}

	/** Ringkasan untuk snapshot performa. Tidak pernah melempar exception. */
	public static String statistik() {
		try {
			return "aktif=" + aktif.get()
					+ ", puncak=" + puncak.get()
					+ ", puncak/sesi=" + puncakPerSesi.get()
					+ ", total dibuat=" + totalDibuat.get();
		} catch (Throwable t) {
			return "gagal dibaca: " + t.getClass().getSimpleName();
		}
	}

	/** Jumlah desktop yang sedang hidup (dipakai pemantauan lain bila perlu). */
	public static int jumlahAktif() {
		return aktif.get();
	}
}
