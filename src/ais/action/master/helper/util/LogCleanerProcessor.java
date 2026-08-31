package ais.action.master.helper.util;

import java.util.TimerTask;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;

/**
 * Tugas terjadwal ({@link TimerTask}) untuk membersihkan tabel log lawas secara berkala:
 * {@code online_users}, {@code log_user_actifity}, dan {@code log_login}. Baris yang lebih tua
 * dari {@link #months} bulan dihapus lewat SQL native langsung (bukan Hibernate criteria), dalam
 * satu transaksi tunggal.
 *
 * <p>
 * Eksekusi hanya berjalan bila konfigurasi {@code log_cleaner_processor} bernilai
 * {@link Konfigurasi#AKTIF} (nilai baku bila konfigurasi belum ada juga AKTIF). Pembersihan
 * {@code detail_log_login} sengaja dinonaktifkan (kode dikomentari) — kemungkinan karena tabel ini
 * masih dipakai/direferensikan tabel lain sehingga penghapusan langsung berisiko.
 * </p>
 */
public class LogCleanerProcessor extends TimerTask {

	/** Ambang usia data (dalam bulan) — baris log yang lebih tua dari ini akan dihapus. */
	private static int months = 3;

	/** Dipanggil oleh scheduler {@link java.util.Timer}; mendelegasikan ke {@link #doProcess()}. */
	@Override
	public void run() {
		doProcess();
	}

	/**
	 * Menghapus baris log lawas pada {@code online_users}, {@code log_user_actifity}, dan
	 * {@code log_login} yang lebih tua dari {@link #months} bulan, dalam satu transaksi Hibernate
	 * native. Tidak melakukan apa pun bila konfigurasi {@code log_cleaner_processor} tidak aktif.
	 */
	private void doProcess() {
		
		
		

		Konfigurasi auto_proses_tunggakan = Common.getKonfigurasi(
				"log_cleaner_processor", Konfigurasi.AKTIF);

		if (auto_proses_tunggakan.getNilai().equals(Konfigurasi.AKTIF)) {

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();

			String sql11 = "delete from online_users where tanggal_dirubah < current_timestamp - interval '"
					+ months + " months';";

			String sql1 = "delete from online_users where login in (select id from log_login where tanggal_dirubah < current_timestamp - interval '"
					+ months + " months');";

			String sql21 = "delete from log_user_actifity where tanggal_dirubah < current_timestamp - interval '"
					+ months + " months';";

			String sql2 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where tanggal_dirubah < current_timestamp - interval '"
					+ months + " months'));";

//			String sql31 = "delete from detail_log_login where tanggal_dirubah < current_timestamp - interval '"
//					+ months + " months';";
//
//			String sql3 = "delete from detail_log_login where log_login in (select id from log_login where tanggal_dirubah < current_timestamp - interval '"
//					+ months + " months');";
			String sql4 = "delete from log_login where tanggal_dirubah < current_timestamp - interval '"
					+ months + " months';";

			System.out.println(sql11);
			session.createSQLQuery(sql11).executeUpdate();

			System.out.println(sql1);
			session.createSQLQuery(sql1).executeUpdate();

			System.out.println(sql21);
			session.createSQLQuery(sql21).executeUpdate();

			System.out.println(sql2);
			session.createSQLQuery(sql2).executeUpdate();

//			System.out.println(sql31);
//			session.createSQLQuery(sql31).executeUpdate();
//
//			System.out.println(sql3);
//			session.createSQLQuery(sql3).executeUpdate();

			System.out.println(sql4);
			session.createSQLQuery(sql4).executeUpdate();

			session.getTransaction().commit();

			
			HibernateUtil.closeSession();

		}
	}

}
