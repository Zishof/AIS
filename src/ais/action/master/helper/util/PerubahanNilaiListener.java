package ais.action.master.helper.util;

import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatNilai;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;

public class PerubahanNilaiListener implements EventListener {

	private Detailperkuliahan detailperkuliahan;
	private EventListener onPerubahanNilai;
	private Label label;
	private FormatNilai formatNilai;
	private MyDoublebox doublebox;

	public MyDoublebox getDoublebox() {
		return doublebox;
	}

	public void setDoublebox(MyDoublebox doublebox) {
		this.doublebox = doublebox;
	}

	private List<FormatNilai> formatNilais;
	private MyCheckboxConfig verify;

	public MyCheckboxConfig getVerify() {
		return verify;
	}

	public void setVerify(MyCheckboxConfig verify) {
		this.verify = verify;
	}

	public PerubahanNilaiListener(Detailperkuliahan detailperkuliahan, FormatNilai formatNilai,
			List<FormatNilai> formatNilais, EventListener onPerubahanNilai, Label label, MyDoublebox doublebox,
			MyCheckboxConfig verify) {
		this.formatNilais = formatNilais;
		this.detailperkuliahan = detailperkuliahan;
		this.formatNilai = formatNilai;
		this.onPerubahanNilai = onPerubahanNilai;
		this.label = label;
		this.doublebox = doublebox;
		this.verify = verify;
	}

	// Pool TERBATAS (daemon) untuk sinkronisasi KRS di LATAR setelah simpan nilai. singkronkanKrsMahasiswa
	// itu BERAT (rekomputasi status + akses cache MapDB terkunci) dan TIDAK perlu memblok UI dosen —
	// nilai sudah tersimpan sebelum ini. Ukuran kecil + antrean + CallerRunsPolicy (backpressure), bukan
	// new Thread liar. Tiap tugas MENUTUP native session di finally (thread pool dipakai-ulang). Ukuran
	// via -Dnilai.sync.pool.size (default 4).
	private static final int SYNC_POOL_SIZE;
	static {
		int n = 4;
		try {
			n = Integer.parseInt(System.getProperty("nilai.sync.pool.size", "4").trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerubahanNilaiListener.java:70");
		}
		SYNC_POOL_SIZE = n < 1 ? 1 : n;
	}

	private static final java.util.concurrent.ExecutorService SYNC_POOL = new java.util.concurrent.ThreadPoolExecutor(
			SYNC_POOL_SIZE, SYNC_POOL_SIZE, 60L, java.util.concurrent.TimeUnit.SECONDS,
			new java.util.concurrent.ArrayBlockingQueue<Runnable>(2000), new java.util.concurrent.ThreadFactory() {
				private final java.util.concurrent.atomic.AtomicInteger seq = new java.util.concurrent.atomic.AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "nilai-sync-" + seq.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			}, new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

	public boolean process() throws Exception {
		return process(null);
	}

	/**
	 * Memproses perubahan nilai.
	 *
	 * @param nilaiMentah nilai hasil baca-aman dari teks yang BENAR-BENAR diketik user
	 *                    (lihat {@link #onEvent}). Bila {@code null} (mis. dipanggil dari
	 *                    timer verifikasi atau {@code process()} tanpa argumen), nilai
	 *                    diambil dari {@code doublebox.getValue()}.
	 */
	public boolean process(Double nilaiMentah) throws Exception {

		/*
		 * AKAR MASALAH "input 80 muncul peringatan > 100":
		 * ZK Doublebox memparse teks memakai LOCALE aktif. Pada locale Indonesia desimal
		 * adalah "," dan pemisah ribuan adalah ".". Saat memparse, ZK MEMBUANG karakter
		 * ribuan "." — sehingga teks seperti "80.0" (titik dianggap ribuan) berubah menjadi
		 * 800 (> 100) dan memicu peringatan PALSU walau user bermaksud mengetik 80. Selain itu
		 * kondisi lama juga memunculkan peringatan ketika nilai kosong (null), padahal kosong
		 * itu wajar (= belum dinilai / 0).
		 *
		 * Solusi: bila tersedia teks mentah dari event ({@code nilaiMentah}, sudah di-parse
		 * aman di {@link #bacaNilaiAman} dengan titik & koma sama-sama dianggap desimal),
		 * itulah yang dipakai. Bila tidak ada, barulah pakai getValue().
		 */
		Double nilai = nilaiMentah;
		if (nilai == null) {
			try {
				nilai = doublebox.getValue();
			} catch (Exception e) {
				nilai = null;
			}
		}

		if (nilai == null) {
			/* Kosong dianggap 0 — TANPA popup (mengetik lalu menghapus itu hal wajar). */
			nilai = 0.0;
			doublebox.setValue(0.0);
		} else if (nilai < 0 || nilai > 100) {
			/*
			 * Hanya nilai yang BENAR-BENAR di luar 0-100 yang diperingatkan, lalu di-clamp
			 * ke batas terdekat (bukan dipaksa ke 0) agar input user tidak hilang total, dan
			 * dihentikan agar nilai tak valid tidak ikut tersimpan.
			 */
			MyMessageboxConfig.show("Nilai yang anda dapat dimasukkan adalah antara 0-100,"
					+ " silahkan perbaiki kembali. Nilai yang lebih dari 100 akan mengganggu akurasi perhitungan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			nilai = nilai < 0 ? 0.0 : 100.0;
			doublebox.setValue(nilai);
			return false;
		} else {
			/* Selaraskan tampilan kotak dengan nilai bersih hasil baca-aman. */
			doublebox.setValue(nilai);
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		// GATE SP (semester pendek): tolak entry nilai bila pembayaran SP mahasiswa belum lunas.
		String alasanSpNilai = ais.action.master.helper.util.GateBayarSpUtil.alasanBlokir(detailperkuliahan);
		if (alasanSpNilai != null) {
			try {
				ais.ui.util.MyMessageboxConfig.show(alasanSpNilai, "Peringatan", ais.ui.util.MyMessageboxConfig.OK,
						ais.ui.util.MyMessageboxConfig.EXCLAMATION);
			} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerubahanNilaiListener.java:153");
			}
			return false;
		}
		Session session = HibernateUtil.currentNativeSession();
		session.refresh(detailperkuliahan);
		detailperkuliahan.populateDetailNilai(formatNilai, null,
				nilai, verify == null ? false : verify.isChecked(),
				tbmuser);

		// FIX "total tidak bisa jadi 0": bila dosen SENGAJA mengosongkan SEMUA komponen (semua 0),
		// total HARUS 0. Tanpa ini, refreshNilaiKeDefault() (dipanggil di dalam hitungTotalNilai)
		// menyangka desync Feeder dan MEMULIHKAN kotak dari totalNilai LAMA yang basi (mis. 11,2)
		// -> total tak pernah 0 & "Hitung Ulang" mengisi ulang komponen ke nilai lama. Menolkan
		// totalNilai lebih dulu membuat guard restore (totalNilai>1.0) gagal, sehingga total dihitung
		// apa adanya dari komponen (=0). Kasus tampilan Feeder (impor total-only) TIDAK terpengaruh
		// karena lewat jalur render yang berbeda, bukan PerubahanNilaiListener.
		if (detailperkuliahan.semuaKomponenDetailNilaiNol()) {
			detailperkuliahan.setTotalNilai(0.0);
			detailperkuliahan.setTotalNilaiSementara(0.0);
		}

		Double total = detailperkuliahan.hitungTotalNilai(true, formatNilais);

		boolean berubah = total.intValue() != detailperkuliahan.getTotalNilai().intValue();

		Matakuliah matakuliah = detailperkuliahan == null ? null
				: detailperkuliahan.getPerkuliahan() != null ? detailperkuliahan.getPerkuliahan().getMatakuliah()
						: detailperkuliahan.getMatakuliahKonversi();

		NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total, detailperkuliahan.getMahasiswa().getTahunangkatan(),
				detailperkuliahan.getMahasiswa().getJurusan(),
				detailperkuliahan.getMahasiswa().getJurusan().getFakultas(), detailperkuliahan.getTahunAkademik(),
				detailperkuliahan.getPerkuliahan() == null ? null : detailperkuliahan.getPerkuliahan().getGanjilGenap(),
				matakuliah == null ? "" : matakuliah.getKode(),
				matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

		if (nilaiHuruf == null) {
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
			MyMessageboxConfig.show(
					"Nilai huruf untuk angka " + total
							+ " belum di konfigurasi secara sesuai, harap menghubungi bagian admin atau puskom",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		detailperkuliahan.setTotalIP(nilaiHuruf.getNilaiDiIPK());
		detailperkuliahan.setTotalNilai(total);
		detailperkuliahan.setNilaiHuruf(nilaiHuruf.getNilaiHuruf());
		detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());

		Double totalSementara = detailperkuliahan.hitungTotalNilaiSementara(true, formatNilais);
		nilaiHuruf = Common.getNilaiHuruf(totalSementara, detailperkuliahan.getMahasiswa().getTahunangkatan(),
				detailperkuliahan.getMahasiswa().getJurusan(),
				detailperkuliahan.getMahasiswa().getJurusan().getFakultas(), detailperkuliahan.getTahunAkademik(),
				detailperkuliahan.getPerkuliahan() == null ? null : detailperkuliahan.getPerkuliahan().getGanjilGenap(),
				matakuliah == null ? "" : matakuliah.getKode(),
				matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

		detailperkuliahan.setTotalNilaiSementara(totalSementara);
		detailperkuliahan.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
		detailperkuliahan.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

		// TAMPILKAN TOTAL LEBIH DULU (instan), SEBELUM simpan DB + sinkronisasi berat — agar dosen
		// langsung melihat total baru tanpa menunggu I/O. Nilai komponen sudah di-set di entity di atas.
		if (detailperkuliahan.getPerkuliahan() != null
				&& detailperkuliahan.getPerkuliahan().getSembunyikanNilaiJikaBelumDiverifikasi()
				&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
			label.setValue(Common.numberFormat.get().format(detailperkuliahan.getTotalNilaiSementara()) + " ("
					+ detailperkuliahan.getNilaiHurufSementara() + ")");
		} else {
			label.setValue(Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()) + " ("
					+ detailperkuliahan.getNilaiHuruf() + ")");
		}

		// Buka session BARU (bukan currentNativeSession) karena helper di atas dapat menutup
		// ThreadLocal native session sehingga commit berikutnya melempar "Session is closed!".
		// Session dedicated ini ditutup di finally agar tidak bocor ke pool.
		Session updateSession = HibernateUtil.getSessionFactory().openSession();
		org.hibernate.Transaction updateTx = null;
		try {
			updateTx = updateSession.beginTransaction();
			updateSession.update(detailperkuliahan);
			updateTx.commit();
		} catch (Exception eUpdate) {
			if (updateTx != null && updateTx.isActive()) {
				try { updateTx.rollback(); } catch (Exception re) { ais.common.ErrorAuditUtil.record(re, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerubahanNilaiListener.java:241");}
			}
			throw eUpdate;
		} finally {
			try { updateSession.clear(); updateSession.close(); } catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerubahanNilaiListener.java:245");}
		}
		HibernateUtil.closeSession();

		System.out.println("total -> " + total + ", totalSementara -> " + totalSementara);

		if (onPerubahanNilai != null) {
			onPerubahanNilai.onEvent(null);
		}

		if (detailperkuliahan.getPerkuliahan() != null
				&& detailperkuliahan.getPerkuliahan().getSembunyikanNilaiJikaBelumDiverifikasi()
				&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
			label.setValue(Common.numberFormat.get().format(detailperkuliahan.getTotalNilaiSementara()) + " ("
					+ detailperkuliahan.getNilaiHurufSementara() + ")");
		} else {
			label.setValue(Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()) + " ("
					+ detailperkuliahan.getNilaiHuruf() + ")");
		}

		// SINKRONISASI KRS DI LATAR: singkronkanKrsMahasiswa BERAT (rekomputasi status + akses cache
		// MapDB terkunci) dan TIDAK perlu memblok UI — nilai SUDAH tersimpan & total SUDAH tampil di atas.
		// Argumen di-capture SELAGI session ZK terbuka (Integer + ref Mahasiswa) agar task latar tak kena
		// LazyInitializationException. Sama seperti batch dashboard yang sudah memanggilnya di thread latar.
		// Bila sync gagal, NILAI TETAP TERSIMPAN.
		if (berubah) {
			final ais.database.model.Mahasiswa mhsSync = detailperkuliahan.getMahasiswa();
			final Integer smtSync = detailperkuliahan.getSemester();
			final Integer tahapSync = detailperkuliahan.getTahap();
			final Integer statusPendekSync = detailperkuliahan.getPerkuliahan() == null ? null
					: detailperkuliahan.getPerkuliahan().getStatusSemesterPendek();
			SYNC_POOL.execute(new Runnable() {
				@Override
				public void run() {
					try {
						Common.singkronkanKrsMahasiswa(mhsSync, smtSync, tahapSync, statusPendekSync, true);
					} catch (Throwable t) {
						System.err.println("nilai-sync KRS gagal (nilai TETAP tersimpan): "
								+ (t == null ? "-" : t.getMessage()));
					} finally {
						try {
							HibernateUtil.closeSession();
						} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerubahanNilaiListener.java:287");
						}
					}
				}
			});
		}

		return true;
	}

	@Override
	public void onEvent(Event arg0) throws Exception {
		/*
		 * Ambil teks yang BENAR-BENAR diketik user dari event onChange (InputEvent), lalu
		 * parse tahan-locale. Ini menghindari kesalahan parse ZK (titik dianggap ribuan →
		 * 80.0 menjadi 800). Bila event bukan InputEvent (mis. dari timer verifikasi),
		 * nilaiMentah = null dan process() memakai doublebox.getValue().
		 */
		Double nilaiMentah = null;
		if (arg0 instanceof org.zkoss.zk.ui.event.InputEvent) {
			nilaiMentah = bacaNilaiAman(((org.zkoss.zk.ui.event.InputEvent) arg0).getValue());
		}
		process(nilaiMentah);
	}

	/**
	 * Mem-parse nilai (0-100) dari teks mentah secara TAHAN-LOCALE: titik maupun koma
	 * sama-sama bisa berperan sebagai pemisah desimal. Pemisah yang muncul TERAKHIR
	 * dianggap pemisah desimal, sedangkan pemisah lain dianggap pemisah ribuan dan dibuang.
	 * Contoh: "80" → 80, "80,5" → 80.5, "80.5" → 80.5, "80.0" → 80.0 (bukan 800),
	 * "1.234,5" → 1234.5. Mengembalikan {@code null} bila teks kosong/tak terbaca.
	 *
	 * @param teks teks mentah dari input
	 * @return nilai {@code Double} hasil parse, atau {@code null}
	 */
	public static Double bacaNilaiAman(String teks) {
		if (teks == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < teks.length(); i++) {
			char c = teks.charAt(i);
			if (Character.isDigit(c) || c == '.' || c == ',' || c == '-') {
				sb.append(c);
			}
		}
		String t = sb.toString().trim();
		if (t.isEmpty() || t.equals("-")) {
			return null;
		}
		int dec = Math.max(t.lastIndexOf('.'), t.lastIndexOf(','));
		if (dec >= 0) {
			String intPart = t.substring(0, dec).replace(".", "").replace(",", "");
			String fracPart = t.substring(dec + 1).replace(".", "").replace(",", "");
			t = intPart + "." + fracPart;
		}
		try {
			return Double.valueOf(Double.parseDouble(t));
		} catch (Exception e) {
			return null;
		}
	}

}
