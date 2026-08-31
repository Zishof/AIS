package ais.action.master.helper;

import java.util.TimerTask;

/**
 * {@link TimerTask} terjadwal untuk menghapus seluruh direktori media (upload) aplikasi.
 * Seluruh logika penghapusan pada {@link #check()} saat ini DINONAKTIFKAN (dikomentari) —
 * kelas ini efektif tidak melakukan apa pun ketika dijalankan oleh scheduler, kemungkinan
 * sengaja dimatikan karena berisiko menghapus seluruh folder media (lihat komentar kode
 * yang memanggil {@code FileUtils.deleteDirectory} pada seluruh direktori media).
 */
public class HapusMediaSheduler extends TimerTask {

	/**
	 * Titik masuk pembersihan media terjadwal. Implementasi aktual (hapus seluruh
	 * direktori media via {@code FileUtils.deleteDirectory}) saat ini dinonaktifkan;
	 * pemanggilan method ini tidak berefek apa pun.
	 */
	public static void check() {
//		if (Common.REAL_PATH != null && !Common.REAL_PATH.trim().isEmpty()) {
//			try {
//				File folder = CommonMedia.getMediaDirectory();
//				System.out.println(
//						"sebelum hapus directory " + folder.getAbsolutePath() + ", exist -> " + folder.exists());
//				FileUtils.deleteDirectory(folder);
//				System.out.println(
//						"setelah hapus directory " + folder.getAbsolutePath() + ", exist -> " + folder.exists());
//			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HapusMediaSheduler.java:16");
//				e.printStackTrace();
//			}
//
//		}
	}

	/** Dipanggil oleh {@link java.util.Timer} sesuai jadwal; mendelegasikan ke {@link #check()}. */
	@Override
	public void run() {
		check();
	}

}
